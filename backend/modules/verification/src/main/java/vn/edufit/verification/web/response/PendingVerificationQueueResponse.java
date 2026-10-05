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

  public static PendingVerificationQueueResponse from(Page<VerificationRequest> pendingPage) {
    return new PendingVerificationQueueResponse(
        pendingPage.getContent().stream().map(Item::from).toList(),
        pendingPage.getNumber(),
        pendingPage.getSize(),
        pendingPage.getTotalElements(),
        pendingPage.getTotalPages()
    );
  }

  public record Item(
      UUID requestId,
      UUID tutorId,
      String status,
      Instant submittedAt
  ) {

    static Item from(VerificationRequest request) {
      return new Item(
          request.getRequestId(),
          request.getTutorId(),
          request.getStatus().name(),
          request.getSubmittedAt()
      );
    }
  }
}
