package vn.edufit.discovery.application.service;

import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.discovery.web.request.TutorSearchRequest;
import vn.edufit.discovery.web.response.TutorDiscoveryCardResponse;
import vn.edufit.discovery.web.response.TutorDetailResponse;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.exception.EntityNotFoundException;

@Service
public class DiscoveryQueryService {

  private final ProfileFacade profileFacade;

  public DiscoveryQueryService(ProfileFacade profileFacade) {
    this.profileFacade = profileFacade;
  }

  @Transactional(readOnly = true)
  public Page<TutorDiscoveryCardResponse> searchTutors(TutorSearchRequest request, Pageable pageable) {
    TutorSearchRequest safeRequest = request != null
        ? request
        : new TutorSearchRequest(null, null, null, null, null, null, null);
    var tutors = profileFacade.searchTutors(safeRequest.toCriteria(), pageable);
    Map<UUID, vn.edufit.profile.api.dto.TutorDiscoveryProfileDto> details = profileFacade
        .findVerifiedTutorDetails(tutors.getContent().stream().map(item -> item.tutorId()).toList())
        .stream()
        .collect(Collectors.toMap(profile -> profile.tutor().tutorId(), Function.identity()));
    return tutors.map(summary -> {
      var detail = details.get(summary.tutorId());
      return detail != null
          ? TutorDiscoveryCardResponse.from(detail)
          : TutorDiscoveryCardResponse.from(summary);
    });
  }

  @Transactional(readOnly = true)
  public TutorDetailResponse getTutorDetail(UUID tutorId) {
    return profileFacade.findVerifiedTutorDetail(tutorId)
        .map(TutorDetailResponse::from)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
  }
}
