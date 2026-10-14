"use client";

import type { useTutorMatching } from "../hooks/useTutorMatching";

export function GoalSelector({ matching, parent = false }: {
  matching: ReturnType<typeof useTutorMatching>; parent?: boolean;
}) {
  return <div className="flex flex-wrap gap-4 rounded-2xl border border-stone-200 bg-white p-4 text-sm">
    {parent && <label className="flex flex-col gap-1">Học sinh đã liên kết
      <select className="rounded-lg border p-2" value={matching.studentId}
        onChange={event => matching.setStudentId(event.target.value)} disabled={!matching.links.length}>
        {!matching.links.length && <option value="">Chưa có liên kết xác nhận</option>}
        {matching.links.map((link, index) => <option key={link.linkId} value={link.studentId}>
          Học sinh {index + 1} · {link.studentId.slice(-8)}
        </option>)}
      </select>
    </label>}
    <label className="flex flex-col gap-1">Mục tiêu dùng để ghép và gửi yêu cầu
      <select className="rounded-lg border p-2" value={matching.goalId}
        onChange={event => matching.setGoalId(event.target.value)} disabled={!matching.goals.length}>
        {!matching.goals.length && <option value="">Chưa có mục tiêu học tập</option>}
        {matching.goals.map(goal => <option key={goal.goalId} value={goal.goalId}>
          {goal.subjectName} · {goal.educationLevelName} · {goal.deadline}{goal.readyForMatching === false ? " (chưa sẵn sàng)" : ""}
        </option>)}
      </select>
    </label>
    {matching.error && <p role="alert" className="w-full text-rose-700">{matching.error}</p>}
  </div>;
}
