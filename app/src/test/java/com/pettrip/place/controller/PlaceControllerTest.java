package com.pettrip.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.place.model.Place;
import com.pettrip.place.service.PlaceNotFoundException;
import com.pettrip.place.service.PlaceService;
import java.math.BigDecimal;
import java.util.List;
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
@WebMvcTest(PlaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PlaceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlaceService placeService;

  private Place samplePlace() {
    return new Place(
        "12345678",
        null,
        "한강공원",
        "https://example.com/image.jpg",
        "서울시 영등포구",
        new BigDecimal("37.5263"),
        new BigDecimal("126.9342"),
        "09:00-22:00",
        "02-1234-5678",
        (short) 4);
  }

  @Test
  void 장소_상세를_조회한다() throws Exception {
    when(placeService.getPlace("12345678")).thenReturn(samplePlace());

    mockMvc
        .perform(get("/places/{externalPlaceId}", "12345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.externalPlaceId").value("12345678"))
        .andExpect(jsonPath("$.placeName").value("한강공원"))
        .andDo(
            document(
                "place-get",
                pathParameters(
                    parameterWithName("externalPlaceId").description("한국관광공사 contentId")),
                responseFields(
                    fieldWithPath("externalPlaceId").description("한국관광공사 contentId"),
                    fieldWithPath("themeId").description("테마 ID").optional(),
                    fieldWithPath("placeName").description("장소명"),
                    fieldWithPath("placeImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("address").description("주소"),
                    fieldWithPath("latitude").description("위도"),
                    fieldWithPath("longitude").description("경도"),
                    fieldWithPath("businessHours").description("영업시간"),
                    fieldWithPath("phoneNumber").description("전화번호"),
                    fieldWithPath("rating").description("평균 평점"),
                    fieldWithPath("reviewNum").description("리뷰 수"),
                    fieldWithPath("visitNum").description("방문 인증 수"),
                    fieldWithPath("petPolicy").description("반려동물 정책 (없으면 null)").optional(),
                    fieldWithPath("createdAt").description("등록일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 존재하지_않는_장소_조회시_404를_반환한다() throws Exception {
    when(placeService.getPlace("unknown")).thenThrow(new PlaceNotFoundException());

    mockMvc.perform(get("/places/{externalPlaceId}", "unknown")).andExpect(status().isNotFound());
  }

  @Test
  void 주변_장소를_검색한다() throws Exception {
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of(samplePlace()));

    mockMvc
        .perform(
            get("/places/nearby")
                .param("lat", "37.5263")
                .param("lng", "126.9342")
                .param("radiusMeters", "3000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].placeName").value("한강공원"))
        .andDo(
            document(
                "place-nearby",
                queryParameters(
                    parameterWithName("lat").description("위도"),
                    parameterWithName("lng").description("경도"),
                    parameterWithName("radiusMeters")
                        .description("검색 반경 (미터, 기본값 5000)")
                        .optional()),
                responseFields(
                    fieldWithPath("[].externalPlaceId").description("한국관광공사 contentId"),
                    fieldWithPath("[].themeId").description("테마 ID").optional(),
                    fieldWithPath("[].placeName").description("장소명"),
                    fieldWithPath("[].placeImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("[].address").description("주소"),
                    fieldWithPath("[].latitude").description("위도"),
                    fieldWithPath("[].longitude").description("경도"),
                    fieldWithPath("[].businessHours").description("영업시간"),
                    fieldWithPath("[].phoneNumber").description("전화번호"),
                    fieldWithPath("[].rating").description("평균 평점"),
                    fieldWithPath("[].reviewNum").description("리뷰 수"),
                    fieldWithPath("[].visitNum").description("방문 인증 수"),
                    fieldWithPath("[].petPolicy").description("반려동물 정책").optional(),
                    fieldWithPath("[].createdAt").description("등록일시"),
                    fieldWithPath("[].updatedAt").description("수정일시"))));
  }
}
