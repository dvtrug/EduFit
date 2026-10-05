package vn.edufit.profile.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.event.ProfileUpdatedEvent;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorAvailabilitySlot;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;
import vn.edufit.profile.infra.persistence.entity.TutorSubject;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.infra.persistence.repository.TutorAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.profile.web.request.AddTutorSubjectRequest;
import vn.edufit.profile.web.request.SetAvailabilitySlotsRequest;
import vn.edufit.profile.web.request.UpdateTutorProfileRequest;
import vn.edufit.profile.web.response.TutorProfileResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;

@Service
public class TutorProfileService {

  private final TutorProfileRepository tutorProfileRepository;
  private final TutorSubjectRepository tutorSubjectRepository;
  private final TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository;
  private final SubjectRepository subjectRepository;
  private final EducationLevelRepository educationLevelRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TutorProfileService(
      TutorProfileRepository tutorProfileRepository,
      TutorSubjectRepository tutorSubjectRepository,
      TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository,
      SubjectRepository subjectRepository,
      EducationLevelRepository educationLevelRepository,
      ApplicationEventPublisher eventPublisher
  ) {
    this.tutorProfileRepository = tutorProfileRepository;
    this.tutorSubjectRepository = tutorSubjectRepository;
    this.tutorAvailabilitySlotRepository = tutorAvailabilitySlotRepository;
    this.subjectRepository = subjectRepository;
    this.educationLevelRepository = educationLevelRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional(readOnly = true)
  public TutorProfileResponse getProfileResponse(UUID userId) {
    TutorProfile profile = tutorProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("Chưa có hồ sơ gia sư cho tài khoản này"));
    return toResponse(profile);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<TutorProfileResponse> findProfileResponseByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public TutorProfileResponse getProfileResponseById(UUID tutorId) {
    TutorProfile profile = tutorProfileRepository.findById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
    return toResponse(profile);
  }

  @Transactional
  public TutorProfileResponse updateProfile(CurrentUser currentUser, UpdateTutorProfileRequest request) {
    ensureAuthenticated(currentUser);
    UUID userId = currentUser.getUserId();

    // Ràng buộc nghiệp vụ BR-07: Hình thức dạy OFFLINE hoặc BOTH bắt buộc phải cung cấp khu vực
    if (request.teachingMode() != TeachingMode.ONLINE) {
      if (request.area() == null || request.area().isBlank()) {
        throw new InvalidOperationException(
            ErrorCode.VALIDATION_FAILED,
            "Khu vực là bắt buộc khi hình thức giảng dạy có dạy trực tiếp (OFFLINE hoặc BOTH)"
        );
      }
    }

    TutorProfile profile = tutorProfileRepository.findByUserId(userId)
        .orElseGet(() -> new TutorProfile(
            userId,
            request.displayName().trim(),
            request.teachingMode(),
            request.pricePerSession()
        ));

    profile.setDisplayName(request.displayName().trim());
    profile.setHeadline(request.headline() != null ? request.headline().trim() : null);
    profile.setBio(request.bio() != null ? request.bio().trim() : null);
    profile.setTeachingMode(request.teachingMode());
    profile.setArea(request.area() != null ? request.area().trim() : null);
    profile.setPricePerSession(request.pricePerSession());
    profile.setExperienceYears(request.experienceYears());
    profile.setTeachingMethod(request.teachingMethod() != null ? request.teachingMethod().trim() : null);

    TutorProfile saved = tutorProfileRepository.save(profile);
    eventPublisher.publishEvent(ProfileUpdatedEvent.of(userId, "TUTOR", saved.getTutorId()));

    return toResponse(saved);
  }

  @Transactional
  public TutorProfileResponse addSubject(CurrentUser currentUser, AddTutorSubjectRequest request) {
    ensureAuthenticated(currentUser);
    TutorProfile profile = getTutorByUserId(currentUser.getUserId());

    if (!subjectRepository.existsById(request.subjectId())) {
      throw EntityNotFoundException.of("Subject", request.subjectId());
    }
    if (!educationLevelRepository.existsById(request.educationLevelId())) {
      throw EntityNotFoundException.of("EducationLevel", request.educationLevelId());
    }

    if (tutorSubjectRepository.existsByTutorIdAndSubjectIdAndEducationLevelId(
        profile.getTutorId(), request.subjectId(), request.educationLevelId())) {
      throw new ResourceConflictException(
          ErrorCode.CONFLICT_DETECTED,
          "Môn học và cấp học này đã được thêm vào hồ sơ gia sư"
      );
    }

    TutorSubject tutorSubject = new TutorSubject(
        profile.getTutorId(),
        request.subjectId(),
        request.educationLevelId()
    );
    tutorSubjectRepository.save(tutorSubject);

    eventPublisher.publishEvent(ProfileUpdatedEvent.of(currentUser.getUserId(), "TUTOR", profile.getTutorId()));
    return toResponse(profile);
  }

  @Transactional
  public TutorProfileResponse removeSubject(CurrentUser currentUser, Integer subjectId, Integer educationLevelId) {
    ensureAuthenticated(currentUser);
    TutorProfile profile = getTutorByUserId(currentUser.getUserId());

    TutorSubject tutorSubject = tutorSubjectRepository.findByTutorIdAndSubjectIdAndEducationLevelId(
        profile.getTutorId(), subjectId, educationLevelId
    ).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy môn học và cấp học trong hồ sơ"));

    tutorSubjectRepository.delete(tutorSubject);
    eventPublisher.publishEvent(ProfileUpdatedEvent.of(currentUser.getUserId(), "TUTOR", profile.getTutorId()));
    return toResponse(profile);
  }

  @Transactional
  public TutorProfileResponse setAvailabilitySlots(CurrentUser currentUser, SetAvailabilitySlotsRequest request) {
    ensureAuthenticated(currentUser);
    TutorProfile profile = getTutorByUserId(currentUser.getUserId());

    List<SetAvailabilitySlotsRequest.SlotItem> rawSlots = request.slots() != null ? request.slots() : List.of();

    // Kiểm tra tính hợp lệ của từng khung giờ (start_time < end_time)
    for (SetAvailabilitySlotsRequest.SlotItem slot : rawSlots) {
      if (!slot.endTime().isAfter(slot.startTime())) {
        throw new InvalidOperationException(
            ErrorCode.VALIDATION_FAILED,
            "Giờ kết thúc phải sau giờ bắt đầu trong mỗi khung giờ"
        );
      }
    }

    // Kiểm tra chống trùng lấn các khung giờ trong cùng ngày (NFR-17 / Overlap check)
    Map<Short, List<SetAvailabilitySlotsRequest.SlotItem>> slotsByDay = rawSlots.stream()
        .collect(Collectors.groupingBy(SetAvailabilitySlotsRequest.SlotItem::dayOfWeek));

    for (Map.Entry<Short, List<SetAvailabilitySlotsRequest.SlotItem>> entry : slotsByDay.entrySet()) {
      List<SetAvailabilitySlotsRequest.SlotItem> daySlots = new ArrayList<>(entry.getValue());
      daySlots.sort(Comparator.comparing(SetAvailabilitySlotsRequest.SlotItem::startTime));

      for (int i = 0; i < daySlots.size() - 1; i++) {
        SetAvailabilitySlotsRequest.SlotItem cur = daySlots.get(i);
        SetAvailabilitySlotsRequest.SlotItem next = daySlots.get(i + 1);
        if (cur.endTime().isAfter(next.startTime())) {
          throw new InvalidOperationException(
              ErrorCode.SCHEDULE_OVERLAP,
              "Các khung giờ rảnh trong cùng ngày (Thứ " + entry.getKey() + ") không được trùng nhau"
          );
        }
      }
    }

    tutorAvailabilitySlotRepository.deleteByTutorId(profile.getTutorId());

    List<TutorAvailabilitySlot> newSlots = rawSlots.stream()
        .map(s -> new TutorAvailabilitySlot(profile.getTutorId(), s.dayOfWeek(), s.startTime(), s.endTime()))
        .toList();
    tutorAvailabilitySlotRepository.saveAll(newSlots);

    eventPublisher.publishEvent(ProfileUpdatedEvent.of(currentUser.getUserId(), "TUTOR", profile.getTutorId()));
    return toResponse(profile);
  }

  public TutorProfileResponse toResponse(TutorProfile profile) {
    List<TutorSubject> tutorSubjects = tutorSubjectRepository.findByTutorId(profile.getTutorId());
    Map<Integer, String> subjectNames = subjectRepository.findAllById(
        tutorSubjects.stream().map(TutorSubject::getSubjectId).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(Subject::getSubjectId, Subject::getName));

    Map<Integer, String> levelNames = educationLevelRepository.findAllById(
        tutorSubjects.stream().map(TutorSubject::getEducationLevelId).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(EducationLevel::getLevelId, EducationLevel::getName));

    List<TutorProfileResponse.TutorSubjectResponse> subjects = tutorSubjects.stream()
        .map(ts -> new TutorProfileResponse.TutorSubjectResponse(
            ts.getTutorSubjectId(),
            ts.getSubjectId(),
            subjectNames.getOrDefault(ts.getSubjectId(), ""),
            ts.getEducationLevelId(),
            levelNames.getOrDefault(ts.getEducationLevelId(), "")
        ))
        .toList();

    List<TutorProfileResponse.TutorSlotResponse> slots = tutorAvailabilitySlotRepository
        .findByTutorIdOrderByDayOfWeekAscStartTimeAsc(profile.getTutorId())
        .stream()
        .map(slot -> new TutorProfileResponse.TutorSlotResponse(
            slot.getSlotId(),
            slot.getDayOfWeek(),
            slot.getStartTime(),
            slot.getEndTime()
        ))
        .toList();

    return new TutorProfileResponse(
        profile.getTutorId(),
        profile.getUserId(),
        profile.getDisplayName(),
        profile.getHeadline(),
        profile.getBio(),
        profile.getTeachingMode() != null ? profile.getTeachingMode().name() : null,
        profile.getArea(),
        profile.getPricePerSession(),
        profile.getExperienceYears(),
        profile.getTeachingMethod(),
        profile.getStatus() != null ? profile.getStatus().name() : null,
        profile.getVerifiedAt(),
        profile.getRatingAvg(),
        profile.getReviewCount(),
        subjects,
        slots,
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }

  private TutorProfile getTutorByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ gia sư"));
  }

  private void ensureAuthenticated(CurrentUser currentUser) {
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new ForbiddenOperationException("Yêu cầu cần được xác thực danh tính người dùng.");
    }
  }
}
