#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
backend_dir="$script_dir/../backend"
fixture_dir="$backend_dir/src/test/java/com/edufit/backend/lintfixture"
fixture_target="$fixture_dir/BadConvention.java"

mkdir -p "$fixture_dir"
cp "$backend_dir/src/test/resources/lint-fixtures/BadConvention.java.fixture" "$fixture_target"
trap 'rm -f "$fixture_target"; rmdir "$fixture_dir" 2>/dev/null || true' EXIT

if lint_output=$(cd "$backend_dir" && sh ./mvnw -B -ntp -DskipTests spotless:check checkstyle:check 2>&1); then
  printf '%s\n' "$lint_output"
  echo "Expected lint to fail for the intentionally invalid fixture, but it passed." >&2
  exit 1
fi

printf '%s\n' "$lint_output"
case "$lint_output" in
  *BadConvention.java*) ;;
  *)
    echo "The command failed for an unrelated reason; the invalid fixture was not reported." >&2
    exit 1
    ;;
esac

echo "PASS: lint rejected the intentionally invalid Java fixture."
