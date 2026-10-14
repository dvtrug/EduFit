import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/api";
import { profileService } from "@/services/profile";

const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status });
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe("Backend integration contracts", () => {
  it("refreshes CSRF for every mutation, including after session rotation", async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(json({ success: true, data: { token: "before-login", headerName: "X-XSRF-TOKEN" } }))
      .mockResolvedValueOnce(json({ success: true }))
      .mockResolvedValueOnce(json({ success: true, data: { token: "after-login", headerName: "X-XSRF-TOKEN" } }))
      .mockResolvedValueOnce(json({ success: true }));
    vi.stubGlobal("fetch", fetch);
    await apiClient.post("/api/v1/auth/login", { email: "student@edufit.vn" });
    await apiClient.post("/api/v1/discovery/match", { goalId: "goal" });
    expect(fetch.mock.calls[0][0]).toBe("/api/v1/auth/csrf");
    expect(fetch.mock.calls[1][1].headers.get("X-XSRF-TOKEN")).toBe("before-login");
    expect(fetch.mock.calls[3][1].headers.get("X-XSRF-TOKEN")).toBe("after-login");
    expect(fetch.mock.calls[3][1].credentials).toBe("include");
  });

  it("does not send the mutation if CSRF bootstrap fails", async () => {
    const fetch = vi.fn().mockResolvedValue(json({ success: false, message: "Unavailable" }, 503));
    vi.stubGlobal("fetch", fetch);
    await expect(apiClient.post("/api/v1/connections/requests", {})).rejects.toMatchObject({ status: 503 });
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("saves goal availability through the separate endpoint before returning a ready goal", async () => {
    const post = vi.spyOn(apiClient, "post").mockResolvedValue({ success: true, data: { goalId: "goal" } });
    const put = vi.spyOn(apiClient, "put").mockResolvedValue({ success: true, data: { goalId: "goal", readyForMatching: true } });
    const slots = [{ dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" }];
    const payload = { subjectId: 1, currentLevel: 1, desiredLevel: 4, goalType: "EXAM_PREPARATION" as const,
      deadline: "2030-06-01", mode: "ONLINE" as const, budgetMax: 300000, availabilitySlots: slots };
    const result = await profileService.createLearningGoal(payload);
    expect(post.mock.calls[0][1]).not.toHaveProperty("availabilitySlots");
    expect(put).toHaveBeenCalledWith("/api/v1/profiles/students/me/goals/goal/availability", { slots });
    expect(result.readyForMatching).toBe(true);
    vi.restoreAllMocks();
  });

  it("archives an incomplete goal when saving slots fails and exposes the failure", async () => {
    const post = vi.spyOn(apiClient, "post").mockResolvedValue({ success: true, data: { goalId: "incomplete" } });
    vi.spyOn(apiClient, "put").mockRejectedValue(new Error("Slots overlap"));
    await expect(profileService.createLearningGoal({ subjectId: 1, currentLevel: 1, desiredLevel: 3,
      goalType: "IMPROVE_GRADES", deadline: "2030-06-01", mode: "ONLINE", budgetMax: 300000,
      availabilitySlots: [{ dayOfWeek: 2, startTime: "19:00", endTime: "21:00" }],
    })).rejects.toThrow("Slots overlap");
    expect(post).toHaveBeenLastCalledWith("/api/v1/profiles/students/me/goals/incomplete/archive");
  });
});
