package vn.edufit.profile.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edufit.profile.api.dto.LearningGoalView;
import vn.edufit.profile.application.service.LearningGoalService;
import vn.edufit.profile.web.request.SaveLearningGoalRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/profiles/students/me/goals")
public class LearningGoalController {
  private final LearningGoalService goals;
  private final ObjectProvider<CurrentUser> actor;

  public LearningGoalController(LearningGoalService goals, ObjectProvider<CurrentUser> actor) {
    this.goals = goals;
    this.actor = actor;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<LearningGoalView>> create(
      @Valid @RequestBody SaveLearningGoalRequest input) {
    var goal = goals.create(actor.getIfAvailable(), input);
    return ResponseEntity.created(URI.create("/api/v1/profiles/students/me/goals"))
        .body(ApiResponse.success(goal, "Tạo mục tiêu học tập thành công"));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<LearningGoalView>>> list() {
    return ResponseEntity.ok(
        ApiResponse.success(goals.list(actor.getIfAvailable()), "Danh sách mục tiêu học tập"));
  }

  @PutMapping("/{goalId}")
  public ResponseEntity<ApiResponse<LearningGoalView>> update(
      @PathVariable UUID goalId, @Valid @RequestBody SaveLearningGoalRequest input) {
    return ResponseEntity.ok(
        ApiResponse.success(
            goals.update(actor.getIfAvailable(), goalId, input), "Cập nhật mục tiêu thành công"));
  }

  @PostMapping("/{goalId}/archive")
  public ResponseEntity<ApiResponse<LearningGoalView>> archive(@PathVariable UUID goalId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            goals.archive(actor.getIfAvailable(), goalId), "Lưu trữ mục tiêu thành công"));
  }
}
