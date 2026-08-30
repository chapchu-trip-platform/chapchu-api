package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.trip.service.CourseService;
import com.pettrip.trip.service.TooFarFromPlaceException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(CoursePlaceController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class CoursePlaceControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CourseService courseService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 코스_장소를_방문_체크인한다() throws Exception {
    UUID coursePlaceId = UUID.fromString("0198f3a0-9999-7000-8000-000000000003");
    doNothing().when(courseService).visitPlace(any(), any(), anyDouble(), anyDouble());

    String body = objectMapper.writeValueAsString(new VisitRequest(37.5665, 126.9780));

    mockMvc
        .perform(
            patch("/course-places/{coursePlaceId}/visit", coursePlaceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "course-place-visit",
                pathParameters(parameterWithName("coursePlaceId").description("방문 체크인할 코스 장소 ID")),
                requestFields(
                    fieldWithPath("lat").description("현재 위치 위도"),
                    fieldWithPath("lng").description("현재 위치 경도"))));
  }

  @Test
  void 장소에서_500m_초과_시_400을_반환한다() throws Exception {
    UUID coursePlaceId = UUID.fromString("0198f3a0-9999-7000-8000-000000000003");
    doThrow(new TooFarFromPlaceException())
        .when(courseService)
        .visitPlace(any(), any(), anyDouble(), anyDouble());

    String body = objectMapper.writeValueAsString(new VisitRequest(37.0, 127.0));

    mockMvc
        .perform(
            patch("/course-places/{coursePlaceId}/visit", coursePlaceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void lat_lng_없이_요청_시_400을_반환한다() throws Exception {
    UUID coursePlaceId = UUID.fromString("0198f3a0-9999-7000-8000-000000000003");

    mockMvc
        .perform(
            patch("/course-places/{coursePlaceId}/visit", coursePlaceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());
  }
}
