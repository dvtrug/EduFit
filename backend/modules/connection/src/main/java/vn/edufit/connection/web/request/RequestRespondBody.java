package vn.edufit.connection.web.request;

import jakarta.validation.constraints.NotNull;

public record RequestRespondBody(@NotNull Boolean accept, String reason) {}
