package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.place.model.Place;
import com.pettrip.trip.service.CourseService;
import com.pettrip.trip.service.CourseService.RecommendedPlaceResult;
import java.math.BigDecimal;
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
@WebMvcTest(RecommendedPlaceController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class RecommendedPlaceControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CourseService courseService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 주변_추천_장소를_조회한다() throws Exception {
    Place place =
        new Place(
            "ext-001",
            null,
            "한강공원",
            "https://example.com/img.jpg",
            "서울시 마포구",
            new BigDecimal("37.5240"),
            new BigDecimal("126.9335"),
            null,
            null,
            null);
    RecommendedPlaceResult result = new RecommendedPlaceResult(place, null, "관광지", "실외");

    when(courseService.recommendPlaces(any(), any(), any(), any(), anyInt(), any(), any(), any()))
        .thenReturn(List.of(result));

    RecommendedPlaceRequest request =
        new RecommendedPlaceRequest(
            UUID.randomUUID(),
            new BigDecimal("37.5240"),
            new BigDecimal("126.9335"),
            5000,
            (short) 25,
            (short) 60,
            "맑음");

    mockMvc
        .perform(
            post("/recommended-places")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "recommended-places",
                requestFields(
                    fieldWithPath("petId").description("반려동물 ID (필수)"),
                    fieldWithPath("lat").description("검색 기준 위도"),
                    fieldWithPath("lng").description("검색 기준 경도"),
                    fieldWithPath("radiusMeters")
                        .description("검색 반경(m). 0 이하면 기본값 5000m 적용")
                        .type(JsonFieldType.NUMBER),
                    fieldWithPath("temperature").description("기온 (선택)").optional(),
                    fieldWithPath("humidity").description("습도 (선택)").optional(),
                    fieldWithPath("weatherStatus").description("날씨 상태 (선택)").optional()),
                responseFields(
                    fieldWithPath("[].externalPlaceId").description("장소 외부 ID"),
                    fieldWithPath("[].placeName").description("장소 이름"),
                    fieldWithPath("[].placeImageUrl").description("장소 이미지 URL").optional(),
                    fieldWithPath("[].latitude").description("위도").optional(),
                    fieldWithPath("[].longitude").description("경도").optional(),
                    fieldWithPath("[].address").description("주소").optional(),
                    fieldWithPath("[].categoryLabel").description("카테고리 (관광지/음식점 등)").optional(),
                    fieldWithPath("[].indoorOutdoorType").description("실내/실외 구분").optional(),
                    fieldWithPath("[].allowedPetSize").description("입장 가능 반려동물 크기").optional(),
                    fieldWithPath("[].leashRequired").description("리드줄 필수 여부").optional(),
                    fieldWithPath("[].carrierRequired").description("이동장 필수 여부").optional(),
                    fieldWithPath("[].placeCaution").description("반려동물 관련 주의사항").optional())));
  }
}
