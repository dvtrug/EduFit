package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import vn.edufit.connection.application.ConnectionService;
import vn.edufit.connection.application.dto.*;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.connection.web.ConnectionController;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.auth.CurrentUser;

class ConnectionControllerTest {
  private final ConnectionService service = mock(ConnectionService.class);
  private final ProfileFacade profile = mock(ProfileFacade.class);
  private final IamFacade iam = mock(IamFacade.class);
  private final CurrentUser actor = mock(CurrentUser.class);
  private MockMvc mvc;
  private static final String ID = "10000000-0000-0000-0000-000000000001";
  private static final String BASE = "/api/v1/connections";

  @BeforeEach
  void setup() {
    @SuppressWarnings("unchecked") ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
    when(users.getObject()).thenReturn(actor);
    mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service, users, profile, iam)).build();
  }

  @ParameterizedTest
  @CsvSource({"invitation,omitted", "invitation,null", "request,omitted", "request,null"})
  void missingAcceptCannotSilentlyDecline(String kind, String value) throws Exception {
    String url = kind.equals("invitation") ? "/parent-student/respond" : "/requests/" + ID + "/respond";
    String body = kind.equals("invitation") ? "{\"invitationId\":\"" + ID + "\"" + (value.equals("null") ? ",\"accept\":null" : "") + "}"
        : (value.equals("null") ? "{\"accept\":null}" : "{}");
    mvc.perform(post(BASE + url).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    verifyNoInteractions(service);
  }

  private static final UUID UUID_ID = UUID.fromString(ID);
  private static final Instant NOW = Instant.parse("2026-10-11T03:00:00Z");
  private static final Instant EXPIRES = Instant.parse("2026-10-18T03:00:00Z");
  private static final String LINK = "{\"linkId\":\"%s\",\"parentUserId\":\"%s\",\"studentId\":\"%s\",\"status\":\"CONFIRMED\",\"confirmedAt\":\"2026-10-11T03:00:00Z\"}".formatted(ID, ID, ID);
  private static final String REQUEST = "{\"requestId\":\"%s\",\"studentId\":\"%s\",\"tutorId\":\"%s\",\"goalId\":\"%s\",\"status\":\"PENDING\",\"createdAt\":\"2026-10-11T03:00:00Z\",\"expiresAt\":\"2026-10-18T03:00:00Z\"}".formatted(ID, ID, ID, ID);
  private static final String CLASS = "{\"classId\":\"%s\",\"studentId\":\"%s\",\"tutorId\":\"%s\",\"goalId\":\"%s\"}".formatted(ID, ID, ID, ID);

  private ConnectionRequestResult requestResult() {
    return new ConnectionRequestResult(UUID_ID, UUID_ID, UUID_ID, UUID_ID, "PENDING", NOW, EXPIRES,
        UUID_ID, List.of("Tue 19:00"), null);
  }

  private ResultActions success(ResultActions result) throws Exception {
    return result.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").isString()).andExpect(jsonPath("$.timestamp").isString());
  }

  private void data(ResultActions result, String expectedJson) throws Exception {
    success(result).andExpect(jsonPath("$.data").value(new ObjectMapper().readValue(expectedJson, Object.class)));
  }

  @Test void inviteKeepsGenericNullEnvelope() throws Exception {
    when(service.invite(actor, "sample@example.test")).thenReturn(true);
    data(mvc.perform(post(BASE + "/parent-student/invite").contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetEmail\":\"sample@example.test\"}")), "null");
  }

  @Test void invitationsKeepExactArrayShape() throws Exception {
    when(service.invitations(actor)).thenReturn(List.of(new InvitationResult(UUID_ID, UUID_ID, UUID_ID, "PENDING", NOW, EXPIRES)));
    data(mvc.perform(get(BASE + "/parent-student/invitations")),
        "[{\"invitationId\":\"%s\",\"inviterUserId\":\"%s\",\"invitedUserId\":\"%s\",\"status\":\"PENDING\",\"createdAt\":\"2026-10-11T03:00:00Z\",\"expiresAt\":\"2026-10-18T03:00:00Z\"}]".formatted(ID, ID, ID));
  }

  @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void invitationResponseKeepsAcceptAndDeclineShapes(boolean accept) throws Exception {
    when(service.respondInvitation(actor, UUID_ID, accept)).thenReturn(accept ? new ParentStudentLinkResult(UUID_ID, UUID_ID, UUID_ID, "CONFIRMED", NOW) : null);
    data(mvc.perform(post(BASE + "/parent-student/respond").contentType(MediaType.APPLICATION_JSON)
        .content("{\"invitationId\":\"%s\",\"accept\":%s}".formatted(ID, accept))), accept ? LINK : "null");
    verify(service).respondInvitation(actor, UUID_ID, accept);
  }

  @Test void withdrawKeepsNullEnvelope() throws Exception {
    data(mvc.perform(post(BASE + "/parent-student/invitations/" + ID + "/withdraw")), "null");
    verify(service).withdrawInvitation(actor, UUID_ID);
  }

  @Test void linksKeepExactArrayShape() throws Exception {
    when(service.links(actor)).thenReturn(List.of(new ParentStudentLinkResult(UUID_ID, UUID_ID, UUID_ID, "CONFIRMED", NOW)));
    data(mvc.perform(get(BASE + "/parent-student/links")), "[" + LINK + "]");
  }

  @Test void revokeKeepsOptionalBodyAndNullEnvelope() throws Exception {
    data(mvc.perform(delete(BASE + "/parent-student/links/" + ID)), "null");
    verify(service).revokeLink(actor, UUID_ID, null);
  }

  @Test void createRequestKeepsShapeAndInputs() throws Exception {
    when(service.request(actor, UUID_ID, UUID_ID, List.of("Tue 19:00"), "hello")).thenReturn(requestResult());
    data(mvc.perform(post(BASE + "/requests").contentType(MediaType.APPLICATION_JSON)
        .content("{\"goalId\":\"%s\",\"tutorId\":\"%s\",\"desiredSlots\":[\"Tue 19:00\"],\"message\":\"hello\"}".formatted(ID, ID))), REQUEST);
  }

  @Test void learnerListDoesNotLeakInternalFields() throws Exception {
    when(service.requests(actor, UUID_ID)).thenReturn(List.of(requestResult()));
    data(mvc.perform(get(BASE + "/requests").param("studentId", ID)), "[" + REQUEST + "]");
    verifyNoInteractions(profile, iam);
  }

  @Test void tutorInboxKeepsPrivacyWhitelistAndLegacySlots() throws Exception {
    when(actor.hasRole("TUTOR")).thenReturn(true);
    when(service.requests(actor, null)).thenReturn(List.of(requestResult()));
    when(profile.findGoalForConnection(UUID_ID)).thenReturn(Optional.of(new ConnectionGoalDto(UUID_ID, UUID_ID, UUID_ID, 1, 4, "EXAM", null, "ACTIVE")));
    when(iam.findUserSummaryById(UUID_ID)).thenReturn(Optional.of(new UserSummaryView(UUID_ID, "hidden@example.test", "Hidden Parent", AccountRole.PARENT, AccountStatus.ACTIVE)));
    data(mvc.perform(get(BASE + "/requests")), "[{\"requestId\":\"%s\",\"goalType\":\"EXAM\",\"subjectId\":1,\"educationLevelId\":4,\"desiredSlots\":[\"Tue 19:00\"],\"message\":null,\"senderRole\":\"PARENT\",\"status\":\"PENDING\",\"expiresAt\":\"2026-10-18T03:00:00Z\"}]".formatted(ID));
  }

  @Test void cancelKeepsNullEnvelope() throws Exception {
    data(mvc.perform(post(BASE + "/requests/" + ID + "/cancel")), "null");
    verify(service).cancelRequest(actor, UUID_ID);
  }

  @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void tutorResponseKeepsClassOrNull(boolean accept) throws Exception {
    when(service.respondRequest(actor, UUID_ID, accept, "reason")).thenReturn(accept ? new ClassResult(UUID_ID, UUID_ID, UUID_ID, UUID_ID) : null);
    data(mvc.perform(post(BASE + "/requests/" + ID + "/respond").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accept\":%s,\"reason\":\"reason\"}".formatted(accept))), accept ? CLASS : "null");
    verify(service).respondRequest(actor, UUID_ID, accept, "reason");
  }
}
