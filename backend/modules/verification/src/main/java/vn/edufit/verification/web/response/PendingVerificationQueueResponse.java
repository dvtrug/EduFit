package vn.edufit.verification.web.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;

public record PendingVerificationQueueResponse(
    List<Item> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

  public record Item(
      UUID requestId,
      UUID tutorId,
      String status,
      Instant submittedAt,
      int credentialCount
  ) {}

  public static PendingVerificationQueueResponse from(Page<VerificationRequest> page) {
    List<Item> items = page.getContent().stream()
        .map(req -> new Item(
            req.getRequestId(),
            req.getTutorId(),
            req.getStatus().name(),
            req.getSubmittedAt(),
            req.getCredentials().size()
        ))
        .toList();

    return new PendingVerificationQueueResponse(
        items,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
    );
  }
}

