package com.pettrip.user.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.User;
import com.pettrip.user.service.MeDetail;
import com.pettrip.user.service.NicknameAlreadyInUseException;
import com.pettrip.user.service.ProfilePhotoView;
import com.pettrip.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class UserControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
  private static final UUID PHOTO_ID = UUID.fromString("0198f3a0-9999-7000-8000-000000000009");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private MeDetail meWithPhoto(User user) {
    return new MeDetail(
        user,
        new ProfilePhotoView(
            PHOTO_ID, "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/u/1.jpg?sig=x"));
  }

  private FieldDescriptor[] userResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").description("유저 ID"),
      fieldWithPath("email").description("이메일"),
      fieldWithPath("nickname").description("닉네임").optional(),
      fieldWithPath("role").description("권한"),
      fieldWithPath("accountStatus").description("계정 상태"),
      fieldWithPath("createdAt").description("생성일시"),
      fieldWithPath("updatedAt").description("수정일시"),
      fieldWithPath("profilePhoto").description("프로필 사진 (미설정 시 기본 이미지)"),
      fieldWithPath("profilePhoto.photoId")
          .description("사진 ID (기본 이미지면 null)")
          .type(JsonFieldType.STRING)
          .optional(),
      fieldWithPath("profilePhoto.downloadUrl").description("사진 URL (presigned GET 또는 기본 이미지)")
    };
  }

  @Test
  void 내_정보를_조회한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.getMe(any())).thenReturn(meWithPhoto(user));

    mockMvc
        .perform(get("/users/me").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(document("user-get-me", responseFields(userResponseFields())));
  }

  @Test
  void 닉네임을_변경한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.updateMe(any(), eq("초코사랑"), eq(null))).thenReturn(meWithPhoto(user));

    String body = objectMapper.writeValueAsString(new NicknameChangeRequest("초코사랑"));

    mockMvc
        .perform(
            patch("/users/me/nickname")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-change-nickname",
                requestFields(fieldWithPath("nickname").description("변경할 닉네임")),
                responseFields(userResponseFields())));
  }

  @Test
  void 내_정보를_수정한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.updateMe(any(), eq("새닉네임"), eq(AccountStatus.ACTIVE)))
        .thenReturn(meWithPhoto(user));

    String body =
        objectMapper.writeValueAsString(new UserUpdateRequest("새닉네임", AccountStatus.ACTIVE));

    mockMvc
        .perform(
            patch("/users/me")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-update-me",
                requestFields(
                    fieldWithPath("nickname").description("변경할 닉네임 (선택)"),
                    fieldWithPath("accountStatus").description("계정 상태 (선택, 예: WITHDRAWN=탈퇴)")),
                responseFields(userResponseFields())));
  }

  @Test
  void 프로필_사진을_설정한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.updateProfilePhoto(any(), eq(PHOTO_ID))).thenReturn(meWithPhoto(user));

    String body = objectMapper.writeValueAsString(new ProfilePhotoUpdateRequest(PHOTO_ID));

    mockMvc
        .perform(
            patch("/users/me/photo")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profilePhoto.photoId").value(PHOTO_ID.toString()))
        .andDo(
            document(
                "user-update-photo",
                requestFields(
                    fieldWithPath("photoId")
                        .description("설정할 사진 ID (upload-url type=PROFILE로 발급). null이면 기본 이미지로 되돌림")
                        .type(JsonFieldType.STRING)
                        .optional()),
                responseFields(userResponseFields())));
  }

  @Test
  void 깨진_JSON이면_400과_INVALID_REQUEST를_반환한다() throws Exception {
    mockMvc
        .perform(
            patch("/users/me/nickname")
                .contentType("application/json")
                .content("{broken json}")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void 잘못된_enum_값이면_400과_INVALID_REQUEST를_반환한다() throws Exception {
    mockMvc
        .perform(
            patch("/users/me")
                .contentType("application/json")
                .content("{\"nickname\": \"테스트\", \"accountStatus\": \"INVALID_VALUE\"}")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void 빈_닉네임으로_변경하면_400을_반환한다() throws Exception {
    String body = objectMapper.writeValueAsString(new NicknameChangeRequest(""));

    mockMvc
        .perform(
            patch("/users/me/nickname")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 최대_길이_초과_닉네임으로_변경하면_400을_반환한다() throws Exception {
    String body = objectMapper.writeValueAsString(new NicknameChangeRequest("a".repeat(31)));

    mockMvc
        .perform(
            patch("/users/me/nickname")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 이미_사용_중인_닉네임으로_변경하면_409를_반환한다() throws Exception {
    when(userService.updateMe(eq(USER_ID), eq("초롱이"), eq(null)))
        .thenThrow(new NicknameAlreadyInUseException());

    String body = objectMapper.writeValueAsString(new NicknameChangeRequest("초롱이"));

    mockMvc
        .perform(
            patch("/users/me/nickname")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void 이미_사용_중인_닉네임으로_내_정보를_수정하면_409를_반환한다() throws Exception {
    when(userService.updateMe(eq(USER_ID), eq("초롱이"), any()))
        .thenThrow(new NicknameAlreadyInUseException());

    String body = objectMapper.writeValueAsString(new UserUpdateRequest("초롱이", null));

    mockMvc
        .perform(
            patch("/users/me")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isConflict());
  }
}
