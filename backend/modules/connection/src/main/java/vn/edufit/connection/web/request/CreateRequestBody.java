package vn.edufit.connection.web.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateRequestBody(@NotNull UUID goalId, @NotNull UUID tutorId, @NotNull List<String> desiredSlots, String message) {}
