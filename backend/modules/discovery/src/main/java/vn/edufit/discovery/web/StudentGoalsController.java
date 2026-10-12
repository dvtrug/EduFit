package vn.edufit.discovery.web;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edufit.discovery.application.service.StudentGoalQueryService;
import vn.edufit.profile.api.dto.LearningGoalView;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/discovery/students")
public class StudentGoalsController {
  private final StudentGoalQueryService goals;
  private final ObjectProvider<CurrentUser> actors;

  public StudentGoalsController(StudentGoalQueryService goals, ObjectProvider<CurrentUser> actors) {
    this.goals = goals;
    this.actors = actors;
  }

  @GetMapping("/{studentId}/goals")
  public ResponseEntity<ApiResponse<List<LearningGoalView>>> list(@PathVariable UUID studentId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            goals.list(actors.getIfAvailable(), studentId), "Danh sách mục tiêu học tập"));
  }
}
