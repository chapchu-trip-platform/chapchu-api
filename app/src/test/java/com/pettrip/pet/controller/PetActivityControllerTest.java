package com.pettrip.pet.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.service.PetActivityService;
import java.util.List;
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
@WebMvcTest(PetActivityController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PetActivityControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetActivityService petActivityService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 활동_유형_목록을_조회한다() throws Exception {
    Mockito.when(petActivityService.findAll())
        .thenReturn(List.of(new PetActivity("등산"), new PetActivity("산책")));

    mockMvc
        .perform(get("/activities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("등산"))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[1].name").value("산책"))
        .andDo(
            document(
                "pet-activities",
                responseFields(
                    fieldWithPath("[].id").description("활동 ID. 반려동물 등록 시 `activityIds`에 넣는다"),
                    fieldWithPath("[].name").description("활동 이름"))));
  }

  /** 온보딩 화면은 access token을 받기 전에 이 목록이 필요하다. 인증을 걸면 반려동물 등록 자체가 막힌다. */
  @Test
  void 토큰이_없어도_조회할_수_있다() throws Exception {
    Mockito.when(petActivityService.findAll()).thenReturn(List.of(new PetActivity("수영")));

    mockMvc.perform(get("/activities")).andExpect(status().isOk());
  }
}
