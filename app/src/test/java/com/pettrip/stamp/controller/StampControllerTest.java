package com.pettrip.stamp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.stamp.service.StampService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(StampController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class StampControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StampService stampService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 스탬프_도감을_조회한다() throws Exception {
    StampCollectionResponse response =
        new StampCollectionResponse(
            1,
            2,
            List.of(
                new StampResponse(
                    UUID.randomUUID(),
                    "강원",
                    "강원 스탬프",
                    "https://example.com/mascot-gangwon.png",
                    true,
                    2,
                    LocalDateTime.of(2026, 9, 1, 10, 0)),
                new StampResponse(UUID.randomUUID(), "경기", "경기 스탬프", null, false, 0, null)));
    when(stampService.listMyStamps(USER_ID)).thenReturn(response);

    mockMvc
        .perform(get("/users/me/stamps").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acquiredCount").value(1))
        .andExpect(jsonPath("$.stamps[0].acquired").value(true))
        .andExpect(jsonPath("$.stamps[1].acquired").value(false))
        .andExpect(jsonPath("$.stamps[1].firstAcquiredAt").doesNotExist())
        .andDo(
            document(
                "stamp-collection",
                responseFields(
                    fieldWithPath("acquiredCount").description("획득한 스탬프 수"),
                    fieldWithPath("totalCount").description("전체 스탬프 수 (시·도 개수)"),
                    fieldWithPath("stamps[].stampId").description("스탬프 ID"),
                    fieldWithPath("stamps[].regionName").description("시·도 이름"),
                    fieldWithPath("stamps[].stampName").description("스탬프 이름"),
                    fieldWithPath("stamps[].imageUrl")
                        .description("지자체 마스코트 이미지 URL. 아직 등록 전이면 null")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("stamps[].acquired").description("획득 여부. false면 회색 처리"),
                    fieldWithPath("stamps[].stampCount").description("방문 횟수. 미획득이면 0"),
                    fieldWithPath("stamps[].firstAcquiredAt")
                        .description("처음 획득한 시각. 미획득이면 null")
                        .type(JsonFieldType.STRING)
                        .optional())));
  }
}
