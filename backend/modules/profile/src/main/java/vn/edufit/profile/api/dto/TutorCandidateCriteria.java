package vn.edufit.profile.api.dto;

/** Hard eligibility filters only; level, price, time and rating remain scoring factors. */
public record TutorCandidateCriteria(Integer subjectId, String mode, String area) {}
