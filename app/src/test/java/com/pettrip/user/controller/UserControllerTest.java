package com.pettrip.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.User;
import com.pettrip.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @Test
  void 내_정보를_조회한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.getMe(any())).thenReturn(user);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-get-me",
                responseFields(
                    fieldWithPath("id").description("유저 ID"),
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("role").description("권한"),
                    fieldWithPath("accountStatus").description("계정 상태"),
                    fieldWithPath("createdAt").description("생성일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 닉네임을_등록한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.registerNickname(any(), eq("초코사랑"))).thenReturn(user);

    String body = objectMapper.writeValueAsString(new NicknameRegisterRequest("초코사랑"));

    mockMvc
        .perform(post("/users/me").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "user-register-nickname",
                requestFields(fieldWithPath("nickname").description("등록할 닉네임")),
                responseFields(
                    fieldWithPath("id").description("유저 ID"),
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("role").description("권한"),
                    fieldWithPath("accountStatus").description("계정 상태"),
                    fieldWithPath("createdAt").description("생성일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 내_정보를_수정한다() throws Exception {
    User user = new User("test@example.com", "google-1");
    when(userService.updateMe(any(), eq("새닉네임"), eq(AccountStatus.ACTIVE))).thenReturn(user);

    String body =
        objectMapper.writeValueAsString(new UserUpdateRequest("새닉네임", AccountStatus.ACTIVE));

    mockMvc
        .perform(patch("/users/me").contentType("application/json").content(body))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user-update-me",
                requestFields(
                    fieldWithPath("nickname").description("변경할 닉네임 (선택)"),
                    fieldWithPath("accountStatus").description("계정 상태 (선택, 예: WITHDRAWN=탈퇴)")),
                responseFields(
                    fieldWithPath("id").description("유저 ID"),
                    fieldWithPath("email").description("이메일"),
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("role").description("권한"),
                    fieldWithPath("accountStatus").description("계정 상태"),
                    fieldWithPath("createdAt").description("생성일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }
}
