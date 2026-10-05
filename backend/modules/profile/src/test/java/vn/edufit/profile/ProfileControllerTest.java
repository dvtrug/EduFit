package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import vn.edufit.profile.application.service.StudentProfileService;
import vn.edufit.profile.application.service.TutorProfileService;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.web.ProfileController;
import vn.edufit.profile.web.request.AddTutorSubjectRequest;
import vn.edufit.profile.web.request.SetAvailabilitySlotsRequest;
import vn.edufit.profile.web.request.UpdateStudentProfileRequest;
import vn.edufit.profile.web.request.UpdateTutorProfileRequest;
import vn.edufit.profile.web.response.MyProfileResponse;
import vn.edufit.profile.web.response.StudentProfileResponse;
import vn.edufit.profile.web.response.TutorProfileResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.response.ApiResponse;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

  @Mock
  private StudentProfileService studentProfileService;

  @Mock
  private TutorProfileService tutorProfileService;

  @Mock
  private ObjectProvider<CurrentUser> currentUserProvider;

  private ProfileController profileController;

  private UUID testUserId;
  private CurrentUser testCurrentUser;

  @BeforeEach
  void setUp() {
    profileController = new ProfileController(
        studentProfileService,
        tutorProfileService,
        currentUserProvider
    );

    testUserId = UUID.randomUUID();
    testCurrentUser = new CurrentUser() {
      @Override
      public UUID getUserId() {
        return testUserId;
      }

      @Override
      public String getEmail() {
        return "user@edufit.vn";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("STUDENT", "TUTOR");
      }

      @Override
      public boolean hasRole(String role) {
        return getRoles().contains(role);
      }
    };
  }

  @Test
  @DisplayName("Lấy MyProfile trả về thông tin người dùng và các hồ sơ liên quan")
  void shouldGetMyProfile() {
    when(currentUserProvider.getIfAvailable()).thenReturn(testCurrentUser);

    StudentProfileResponse mockStudent = new StudentProfileResponse(
        UUID.randomUUID(), testUserId, 1, "Lớp 12", "Hà Nội", true, Instant.now(), Instant.now()
    );
    TutorProfileResponse mockTutor = new TutorProfileResponse(
        UUID.randomUUID(), testUserId, "Thầy Hải", "Toán", "Bio", "ONLINE", "Hà Nội",
        200_000L, (short) 5, "PP mới", "VERIFIED", Instant.now(), BigDecimal.valueOf(4.8), 10,
        List.of(), List.of(), Instant.now(), Instant.now()
    );

    when(studentProfileService.findProfileResponseByUserId(testUserId)).thenReturn(java.util.Optional.of(mockStudent));
    when(tutorProfileService.findProfileResponseByUserId(testUserId)).thenReturn(java.util.Optional.of(mockTutor));

    ResponseEntity<ApiResponse<MyProfileResponse>> resp = profileController.getMyProfile();
    assertNotNull(resp.getBody());
    assertEquals(testUserId, resp.getBody().data().userId());
    assertNotNull(resp.getBody().data().studentProfile());
    assertNotNull(resp.getBody().data().tutorProfile());
  }

  @Test
  @DisplayName("Cập nhật hồ sơ học sinh qua controller")
  void shouldUpdateStudentProfile() {
    when(currentUserProvider.getIfAvailable()).thenReturn(testCurrentUser);
    UpdateStudentProfileRequest req = new UpdateStudentProfileRequest(2, "Đà Nẵng");
    StudentProfileResponse mockStudent = new StudentProfileResponse(
        UUID.randomUUID(), testUserId, 2, "Lớp 11", "Đà Nẵng", true, Instant.now(), Instant.now()
    );
    when(studentProfileService.updateProfile(testCurrentUser, req)).thenReturn(mockStudent);

    ResponseEntity<ApiResponse<StudentProfileResponse>> resp = profileController.updateMyStudentProfile(req);
    assertNotNull(resp.getBody());
    assertEquals("Đà Nẵng", resp.getBody().data().area());
  }

  @Test
  @DisplayName("Cập nhật hồ sơ gia sư qua controller")
  void shouldUpdateTutorProfile() {
    when(currentUserProvider.getIfAvailable()).thenReturn(testCurrentUser);
    UpdateTutorProfileRequest req = new UpdateTutorProfileRequest(
        "Cô Lan", "Toán 12", "Bio", TeachingMode.ONLINE, null, 250_000L, (short) 3, "PP tích cực"
    );
    TutorProfileResponse mockTutor = new TutorProfileResponse(
        UUID.randomUUID(), testUserId, "Cô Lan", "Toán 12", "Bio", "ONLINE", null,
        250_000L, (short) 3, "PP tích cực", "DRAFT", null, null, 0,
        List.of(), List.of(), Instant.now(), Instant.now()
    );
    when(tutorProfileService.updateProfile(testCurrentUser, req)).thenReturn(mockTutor);

    ResponseEntity<ApiResponse<TutorProfileResponse>> resp = profileController.updateMyTutorProfile(req);
    assertNotNull(resp.getBody());
    assertEquals("Cô Lan", resp.getBody().data().displayName());
  }

  @Test
  @DisplayName("Thêm môn học và xóa môn học cho gia sư")
  void shouldAddAndRemoveTutorSubject() {
    when(currentUserProvider.getIfAvailable()).thenReturn(testCurrentUser);
    AddTutorSubjectRequest req = new AddTutorSubjectRequest(1, 2);

    TutorProfileResponse mockTutor = new TutorProfileResponse(
        UUID.randomUUID(), testUserId, "Gia sư A", null, null, "ONLINE", null,
        150_000L, (short) 1, null, "DRAFT", null, null, 0,
        List.of(), List.of(), Instant.now(), Instant.now()
    );
    when(tutorProfileService.addSubject(testCurrentUser, req)).thenReturn(mockTutor);
    when(tutorProfileService.removeSubject(testCurrentUser, 1, 2)).thenReturn(mockTutor);

    ResponseEntity<ApiResponse<TutorProfileResponse>> addResp = profileController.addTutorSubject(req);
    assertEquals(200, addResp.getStatusCode().value());

    ResponseEntity<ApiResponse<TutorProfileResponse>> delResp = profileController.removeTutorSubject(1, 2);
    assertEquals(200, delResp.getStatusCode().value());
  }

  @Test
  @DisplayName("Cập nhật khung giờ rảnh cho gia sư")
  void shouldSetAvailabilitySlots() {
    when(currentUserProvider.getIfAvailable()).thenReturn(testCurrentUser);
    SetAvailabilitySlotsRequest req = new SetAvailabilitySlotsRequest(List.of(
        new SetAvailabilitySlotsRequest.SlotItem((short) 2, LocalTime.of(8, 0), LocalTime.of(10, 0))
    ));

    TutorProfileResponse mockTutor = new TutorProfileResponse(
        UUID.randomUUID(), testUserId, "Gia sư A", null, null, "ONLINE", null,
        150_000L, (short) 1, null, "DRAFT", null, null, 0,
        List.of(), List.of(), Instant.now(), Instant.now()
    );
    when(tutorProfileService.setAvailabilitySlots(testCurrentUser, req)).thenReturn(mockTutor);

    ResponseEntity<ApiResponse<TutorProfileResponse>> resp = profileController.setAvailabilitySlots(req);
    assertEquals(200, resp.getStatusCode().value());
    verify(tutorProfileService).setAvailabilitySlots(testCurrentUser, req);
  }
}
