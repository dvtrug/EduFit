package vn.edufit.profile.api.dto;

/** Catalog ordering metadata for matching; IDs do not encode education level order. */
public record EducationLevelOrderDto(Integer levelId, Integer sortOrder) {}
