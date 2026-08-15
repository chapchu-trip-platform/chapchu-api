package com.pettrip.pet.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.pet.model.Breed;
import com.pettrip.pet.service.BreedService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(BreedController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class BreedControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BreedService breedService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private static Breed breed(int id, String name) {
    Breed breed = new Breed(name);
    ReflectionTestUtils.setField(breed, "id", id);
    return breed;
  }

  @Test
  void 견종_목록을_조회한다() throws Exception {
    // breed_id는 DB가 IDENTITY로 만든다(V7). 단위 테스트에는 영속화가 없으므로 값을 직접 넣어준다.
    Mockito.when(breedService.findAll()).thenReturn(List.of(breed(1, "골든리트리버"), breed(157, "믹스견")));

    mockMvc
        .perform(get("/breeds"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("골든리트리버"))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].name").value("믹스견"))
        .andDo(
            document(
                "breeds",
                responseFields(
                    fieldWithPath("[].id").description("견종 ID. 반려동물 등록 시 `breedId`에 넣는다"),
                    fieldWithPath("[].name").description("견종 이름"))));
  }

  /** 온보딩 화면은 access token을 받기 전에 이 목록이 필요하다. 인증을 걸면 반려동물 등록 자체가 막힌다. */
  @Test
  void 토큰이_없어도_조회할_수_있다() throws Exception {
    Mockito.when(breedService.findAll()).thenReturn(List.of(breed(2, "말티즈")));

    mockMvc.perform(get("/breeds")).andExpect(status().isOk());
  }
}
