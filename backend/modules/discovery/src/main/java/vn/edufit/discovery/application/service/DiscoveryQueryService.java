package vn.edufit.discovery.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSearchCriteria;
import vn.edufit.shared.exception.EntityNotFoundException;

@Service
public class DiscoveryQueryService {

  private final ProfileFacade profileFacade;

  public DiscoveryQueryService(ProfileFacade profileFacade) {
    this.profileFacade = profileFacade;
  }

  @Transactional(readOnly = true)
  public Page<TutorDiscoveryProfileDto> searchTutors(TutorSearchCriteria criteria, Pageable pageable) {
    TutorSearchCriteria safeCriteria = criteria != null
        ? criteria
        : new TutorSearchCriteria(null, null, null, null, null, null, null);
    var tutors = profileFacade.searchTutors(safeCriteria, pageable);
    Map<UUID, TutorDiscoveryProfileDto> details = profileFacade
        .findVerifiedTutorDetails(tutors.getContent().stream().map(item -> item.tutorId()).toList())
        .stream()
        .collect(Collectors.toMap(profile -> profile.tutor().tutorId(), Function.identity()));
    return tutors.map(summary -> {
      var detail = details.get(summary.tutorId());
      return detail != null
          ? detail
          : new TutorDiscoveryProfileDto(summary, List.of(), List.of(), null);
    });
  }

  @Transactional(readOnly = true)
  public TutorDiscoveryProfileDto getTutorDetail(UUID tutorId) {
    return profileFacade.findVerifiedTutorDetail(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
  }
}
