package vn.edufit.connection.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TutorInboxResult(
    UUID requestId,
    String goalType,
    Integer subjectId,
    Integer educationLevelId,
    List<String> desiredSlots,
    String message,
    String senderRole,
    String status,
    Instant expiresAt,
    LocalDate deadline,
    String studentDisplayName,
    Integer currentLevel,
    Integer desiredLevel,
    String currentGrade,
    String targetScore,
    Long budgetMin,
    Long budgetMax,
    String area,
    String district,
    String city,
    String subjectName,
    String educationLevelName,
    String gradeLevelName,
    Boolean hasSlotConflict,
    List<String> conflictingSlots
) {

  public TutorInboxResult(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  public TutorInboxResult(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt, LocalDate deadline) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, deadline,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  public TutorInboxResult(
      UUID requestId,
      String goalType,
      Integer subjectId,
      Integer educationLevelId,
      List<String> desiredSlots,
      String message,
      String senderRole,
      String status,
      Instant expiresAt,
      LocalDate deadline,
      String studentDisplayName,
      Integer currentLevel,
      Integer desiredLevel,
      Long budgetMin,
      Long budgetMax,
      String area,
      String subjectName,
      String educationLevelName,
      Boolean hasSlotConflict,
      List<String> conflictingSlots
  ) {
    this(
        requestId,
        goalType,
        subjectId,
        educationLevelId,
        desiredSlots,
        message,
        senderRole,
        status,
        expiresAt,
        deadline,
        studentDisplayName,
        currentLevel,
        desiredLevel,
        educationLevelName != null ? educationLevelName : (currentLevel != null ? currentLevel.toString() : null),
        desiredLevel != null ? desiredLevel.toString() : null,
        budgetMin,
        budgetMax,
        area,
        parseDistrict(area),
        parseCity(area),
        subjectName,
        educationLevelName,
        educationLevelName,
        hasSlotConflict,
        conflictingSlots
    );
  }

  private static String parseDistrict(String area) {
    if (area == null || area.isBlank()) return null;
    String[] parts = area.split(",", 2);
    return parts.length == 2 ? parts[0].trim() : null;
  }

  private static String parseCity(String area) {
    if (area == null || area.isBlank()) return null;
    String[] parts = area.split(",", 2);
    return parts.length == 2 ? parts[1].trim() : parts[0].trim();
  }
}
