package com.pettrip.signup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.service.BreedNotFoundException;
import com.pettrip.signup.service.AlreadySignedUpException;
import com.pettrip.signup.service.InvalidRegistrationTokenException;
import com.pettrip.signup.service.RegistrationTokenVerificationFailedException;
import com.pettrip.signup.service.SignupService;
import com.pettrip.signup.service.SignupService.SignupResult;
import com.pettrip.user.model.User;
import com.pettrip.user.service.NicknameAlreadyInUseException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(SignupController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class SignupControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private SignupService signupService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private String requestBody() throws Exception {
    return objectMapper.writeValueAsString(
        new SignupRequest(
            "eyJ0b2tlbiI.c2lnbmF0dXJl",
            new SignupRequest.UserPart(
                "밤톨이아빠",
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID())),
            List.of(
                new SignupRequest.PetPart("초코", 7, PetSize.SMALL, 3, List.of(UUID.randomUUID())),
                new SignupRequest.PetPart("보리", 157, PetSize.MEDIUM, 5, List.of()))));
  }

  private SignupResult twoPetResult() {
    User user = new User("bamtol@example.com", "google-signup-001");
    user.registerNickname("밤톨이아빠");
    Pet choco = new Pet(user.getId(), new Breed("푸들"), "초코", PetSize.SMALL, 3);
    Pet bori = new Pet(user.getId(), new Breed("믹스견"), "보리", PetSize.MEDIUM, 5);
    return new SignupResult(user, List.of(choco, bori));
  }

  @Test
  void 유저와_반려동물을_한_번에_등록한다() throws Exception {
    Mockito.when(signupService.signUp(any())).thenReturn(twoPetResult());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nickname").value("밤톨이아빠"))
        .andExpect(jsonPath("$.petIds.length()").value(2))
        .andDo(
            document(
                "auth-signup",
                requestFields(
                    fieldWithPath("registrationToken")
                        .description("온보딩 리다이렉트에서 받은 `registration_token`. 10분 TTL"),
                    fieldWithPath("user.nickname").description("사용할 닉네임. 최대 30자"),
                    fieldWithPath("user.regionIds")
                        .description("선호 지역 ID 목록. `GET /preferences/options`")
                        .optional(),
                    fieldWithPath("user.themeIds").description("선호 테마 ID 목록").optional(),
                    fieldWithPath("user.transportMethodIds")
                        .description("선호 이동수단 ID 목록. 여러 개 선택 가능")
                        .optional(),
                    fieldWithPath("pets").description("등록할 반려동물. 생략하거나 비워도 된다").optional(),
                    fieldWithPath("pets[].petName").description("반려견 이름"),
                    fieldWithPath("pets[].breedId")
                        .description("견종 ID. `GET /breeds`. 순종이 아니면 `믹스견`"),
                    fieldWithPath("pets[].size").description("크기 (SMALL/MEDIUM/LARGE)"),
                    fieldWithPath("pets[].age").description("나이"),
                    fieldWithPath("pets[].activityIds")
                        .description("선호 활동 ID 목록. `GET /activities`")
                        .optional()),
                responseFields(
                    fieldWithPath("userId").description("만들어진 유저 ID. 발급될 access token의 `sub`와 같다"),
                    fieldWithPath("nickname").description("등록된 닉네임"),
                    fieldWithPath("email").description("구글 계정 이메일"),
                    fieldWithPath("petIds").description("만들어진 반려동물 ID. 요청 순서를 지킨다"))));
  }

  /** 이 시점에 사용자가 가진 것은 registration token뿐이다. 인증을 걸면 회원가입 자체가 불가능하다. */
  @Test
  void 토큰_없이도_호출할_수_있다() throws Exception {
    Mockito.when(signupService.signUp(any())).thenReturn(twoPetResult());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("반려동물을 생략해도 가입된다")
  void signsUpWithoutPets() throws Exception {
    User user = new User("nopet@example.com", "google-signup-002");
    user.registerNickname("혼자여행");
    Mockito.when(signupService.signUp(any())).thenReturn(new SignupResult(user, List.of()));

    String body =
        objectMapper.writeValueAsString(
            new SignupRequest(
                "eyJ0b2tlbiI.c2ln", new SignupRequest.UserPart("혼자여행", null, null, null), null));

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.petIds.length()").value(0));
  }

  @Test
  @DisplayName("가입 토큰이 유효하지 않으면 401")
  void invalidTokenReturns401() throws Exception {
    Mockito.when(signupService.signUp(any())).thenThrow(new InvalidRegistrationTokenException());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("이미 가입된 계정이면 409")
  void alreadySignedUpReturns409() throws Exception {
    Mockito.when(signupService.signUp(any())).thenThrow(new AlreadySignedUpException());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  @DisplayName("닉네임이 이미 쓰이고 있으면 409")
  void takenNicknameReturns409() throws Exception {
    Mockito.when(signupService.signUp(any())).thenThrow(new NicknameAlreadyInUseException());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("없는 견종을 고르면 404")
  void unknownBreedReturns404() throws Exception {
    Mockito.when(signupService.signUp(any())).thenThrow(new BreedNotFoundException());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isNotFound());
  }

  /** 인증 서버 장애를 500으로 흘리면 사용자는 자기 입력이 잘못된 줄 안다. */
  @Test
  @DisplayName("인증 서버에 닿지 못하면 502")
  void authServerDownReturns502() throws Exception {
    Mockito.when(signupService.signUp(any()))
        .thenThrow(new RegistrationTokenVerificationFailedException());

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(requestBody()))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"));
  }

  @Test
  @DisplayName("닉네임이 비어 있으면 400")
  void blankNicknameReturns400() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            new SignupRequest(
                "eyJ0b2tlbiI.c2ln", new SignupRequest.UserPart("", null, null, null), null));

    mockMvc
        .perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }
}
