package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
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
import vn.edufit.connection.application.service.ConnectionRequestService;
import vn.edufit.connection.application.service.ConnectionQueryService;
import vn.edufit.connection.application.service.LinkInvitationService;
import vn.edufit.connection.application.service.ParentStudentLinkService;
import vn.edufit.connection.application.dto.*;
import vn.edufit.connection.web.ConnectionController;
import vn.edufit.shared.auth.CurrentUser;

class ConnectionControllerTest {
  private final ConnectionRequestService service = mock(ConnectionRequestService.class);
  private final LinkInvitationService invitationService = mock(LinkInvitationService.class);
  private final ParentStudentLinkService linkService = mock(ParentStudentLinkService.class);
  private final ConnectionQueryService queries = mock(ConnectionQueryService.class);
  private final CurrentUser actor = mock(CurrentUser.class);
  private MockMvc mvc;
  private static final String ID = "10000000-0000-0000-0000-000000000001";
  private static final String BASE = "/api/v1/connections";

  @BeforeEach
  void setup() {
    @SuppressWarnings("unchecked") ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
    when(users.getObject()).thenReturn(actor);
    mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service, invitationService, linkService, queries, users)).build();
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
    verifyNoInteractions(service, invitationService, linkService, queries);
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
    when(invitationService.invite(actor, "sample@example.test")).thenReturn(true);
    data(mvc.perform(post(BASE + "/parent-student/invite").contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetEmail\":\"sample@example.test\"}")), "null");
  }

  @Test void invitationsKeepExactArrayShape() throws Exception {
    when(queries.invitations(actor)).thenReturn(List.of(new InvitationResult(UUID_ID, UUID_ID, UUID_ID, "PENDING", NOW, EXPIRES)));
    data(mvc.perform(get(BASE + "/parent-student/invitations")),
        "[{\"invitationId\":\"%s\",\"inviterUserId\":\"%s\",\"invitedUserId\":\"%s\",\"status\":\"PENDING\",\"createdAt\":\"2026-10-11T03:00:00Z\",\"expiresAt\":\"2026-10-18T03:00:00Z\"}]".formatted(ID, ID, ID));
  }

  @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void invitationResponseKeepsAcceptAndDeclineShapes(boolean accept) throws Exception {
    when(invitationService.respondInvitation(actor, UUID_ID, accept)).thenReturn(TransitionResult.success(accept ? new ParentStudentLinkResult(UUID_ID, UUID_ID, UUID_ID, "CONFIRMED", NOW) : null));
    data(mvc.perform(post(BASE + "/parent-student/respond").contentType(MediaType.APPLICATION_JSON)
        .content("{\"invitationId\":\"%s\",\"accept\":%s}".formatted(ID, accept))), accept ? LINK : "null");
    verify(invitationService).respondInvitation(actor, UUID_ID, accept);
  }

  @Test void withdrawKeepsNullEnvelope() throws Exception {
    when(invitationService.withdrawInvitation(actor, UUID_ID)).thenReturn(TransitionResult.success(null));
    data(mvc.perform(post(BASE + "/parent-student/invitations/" + ID + "/withdraw")), "null");
    verify(invitationService).withdrawInvitation(actor, UUID_ID);
  }

  @Test void linksKeepExactArrayShape() throws Exception {
    when(queries.links(actor)).thenReturn(List.of(new ParentStudentLinkResult(UUID_ID, UUID_ID, UUID_ID, "CONFIRMED", NOW)));
    data(mvc.perform(get(BASE + "/parent-student/links")), "[" + LINK + "]");
  }

  @Test void revokeKeepsOptionalBodyAndNullEnvelope() throws Exception {
    data(mvc.perform(delete(BASE + "/parent-student/links/" + ID)), "null");
    verify(linkService).revokeLink(actor, UUID_ID, null);
  }

  @Test void createRequestKeepsShapeAndInputs() throws Exception {
    when(service.request(actor, UUID_ID, UUID_ID, List.of("Tue 19:00"), "hello")).thenReturn(TransitionResult.success(requestResult()));
    data(mvc.perform(post(BASE + "/requests").contentType(MediaType.APPLICATION_JSON)
        .content("{\"goalId\":\"%s\",\"tutorId\":\"%s\",\"desiredSlots\":[\"Tue 19:00\"],\"message\":\"hello\"}".formatted(ID, ID))), REQUEST);
  }

  @Test void learnerListDoesNotLeakInternalFields() throws Exception {
    when(queries.requests(actor, UUID_ID)).thenReturn(List.of(requestResult()));
    data(mvc.perform(get(BASE + "/requests").param("studentId", ID)), "[" + REQUEST + "]");
    verify(queries).requests(actor, UUID_ID);
  }

  @Test void tutorInboxKeepsPrivacyWhitelistAndLegacySlots() throws Exception {
    when(actor.hasRole("TUTOR")).thenReturn(true);
    when(queries.tutorInbox(actor)).thenReturn(List.of(new TutorInboxResult(UUID_ID, "EXAM", 1, 4,
        List.of("Tue 19:00"), null, "PARENT", "PENDING", EXPIRES)));
    data(mvc.perform(get(BASE + "/requests")), "[{\"requestId\":\"%s\",\"goalType\":\"EXAM\",\"subjectId\":1,\"educationLevelId\":4,\"desiredSlots\":[\"Tue 19:00\"],\"message\":null,\"senderRole\":\"PARENT\",\"status\":\"PENDING\",\"expiresAt\":\"2026-10-18T03:00:00Z\"}]".formatted(ID));
  }

  @Test void cancelKeepsNullEnvelope() throws Exception {
    when(service.cancelRequest(actor, UUID_ID)).thenReturn(TransitionResult.success(null));
    data(mvc.perform(post(BASE + "/requests/" + ID + "/cancel")), "null");
    verify(service).cancelRequest(actor, UUID_ID);
  }

  @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void tutorResponseKeepsClassOrNull(boolean accept) throws Exception {
    when(service.respondRequest(actor, UUID_ID, accept, "reason")).thenReturn(TransitionResult.success(accept ? new ClassResult(UUID_ID, UUID_ID, UUID_ID, UUID_ID) : null));
    data(mvc.perform(post(BASE + "/requests/" + ID + "/respond").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accept\":%s,\"reason\":\"reason\"}".formatted(accept))), accept ? CLASS : "null");
    verify(service).respondRequest(actor, UUID_ID, accept, "reason");
  }
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings={"empty","null","blank","oversize","message"})
  void createRejectsInvalidInputBeforeService(String kind) throws Exception {
    String slot=kind.equals("oversize")?"x".repeat(101):kind.equals("blank")?" ":"Tue 19:00";
    String slots=kind.equals("empty")?"[]":kind.equals("null")?"null":"[\""+slot+"\"]";
    String message=kind.equals("message")?"x".repeat(501):"";
    String body="{\"goalId\":\""+ID+"\",\"tutorId\":\""+ID+"\",\"desiredSlots\":"+slots+",\"message\":\""+message+"\"}";
    mvc.perform(post(BASE+"/requests").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,result.getResolvedException()));
    verifyNoInteractions(service);
  }

}
