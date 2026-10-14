package vn.edufit.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import vn.edufit.ai.infra.client.StubLlmProvider;

class StubLlmProviderTest {
  private final StubLlmProvider stub = new StubLlmProvider();

  @Test
  void matchingPromptContainingTutorDoesNotReturnInventedBio() {
    var result =
        stub.call("Trợ lý EduFit", "Hãy giải thích vì sao gia sư phù hợp. Điểm lịch: 50/100", 0.2);
    assertThat(result.text())
        .startsWith("Giải thích giả lập")
        .doesNotContain("4 năm", "100%", "4.9", "thứ 2");
    assertThat(result.generatedByModel()).isFalse();
    assertThat(result.tokenCount()).isNull();
  }

  @Test
  void bioDraftDoesNotInventQualificationsOrExperience() {
    var result = stub.call("Trợ lý EduFit", "Viết bio giới thiệu gia sư", null);
    assertThat(result.text()).contains("giả lập").doesNotContain("4 năm", "Toán", "cấp 2", "cấp 3");
  }
}
