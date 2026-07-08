package com.pettrip.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.service.PlaceNotFoundException;
import com.pettrip.place.service.PlaceService;
import java.math.BigDecimal;
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
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PlaceService placeService;

  private Place samplePlace() {
    return new Place(
        "kakao-12345",
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
    when(placeService.getPlace("kakao-12345")).thenReturn(samplePlace());

    mockMvc
        .perform(get("/places/{externalPlaceId}", "kakao-12345"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.externalPlaceId").value("kakao-12345"))
        .andExpect(jsonPath("$.placeName").value("한강공원"))
        .andDo(
            document(
                "place-get",
                pathParameters(parameterWithName("externalPlaceId").description("외부 API 장소 ID")),
                responseFields(
                    fieldWithPath("externalPlaceId").description("외부 API 장소 ID"),
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
    when(placeService.getPlace("unknown-id")).thenThrow(new PlaceNotFoundException());

    mockMvc
        .perform(get("/places/{externalPlaceId}", "unknown-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void 장소를_등록한다() throws Exception {
    Place place = samplePlace();
    when(placeService.upsertPlace(
            eq("kakao-12345"),
            any(),
            eq("한강공원"),
            any(),
            eq("서울시 영등포구"),
            any(),
            any(),
            any(),
            any(),
            any()))
        .thenReturn(place);

    PlaceRegisterRequest request =
        new PlaceRegisterRequest(
            "kakao-12345",
            null,
            "한강공원",
            "https://example.com/image.jpg",
            "서울시 영등포구",
            new BigDecimal("37.5263"),
            new BigDecimal("126.9342"),
            "09:00-22:00",
            "02-1234-5678",
            (short) 4);

    mockMvc
        .perform(
            post("/places")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "place-register",
                requestFields(
                    fieldWithPath("externalPlaceId").description("외부 API 장소 ID"),
                    fieldWithPath("themeId").description("테마 ID (선택)").optional(),
                    fieldWithPath("placeName").description("장소명"),
                    fieldWithPath("placeImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("address").description("주소"),
                    fieldWithPath("latitude").description("위도"),
                    fieldWithPath("longitude").description("경도"),
                    fieldWithPath("businessHours").description("영업시간"),
                    fieldWithPath("phoneNumber").description("전화번호"),
                    fieldWithPath("rating").description("평점")),
                responseFields(
                    fieldWithPath("externalPlaceId").description("외부 API 장소 ID"),
                    fieldWithPath("themeId").description("테마 ID").optional(),
                    fieldWithPath("placeName").description("장소명"),
                    fieldWithPath("placeImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("address").description("주소"),
                    fieldWithPath("latitude").description("위도"),
                    fieldWithPath("longitude").description("경도"),
                    fieldWithPath("businessHours").description("영업시간"),
                    fieldWithPath("phoneNumber").description("전화번호"),
                    fieldWithPath("rating").description("평점"),
                    fieldWithPath("reviewNum").description("리뷰 수"),
                    fieldWithPath("visitNum").description("방문 인증 수"),
                    fieldWithPath("petPolicy").description("반려동물 정책").optional(),
                    fieldWithPath("createdAt").description("등록일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 장소_펫_정책을_등록한다() throws Exception {
    Place place = samplePlace();
    PlacePetPolicy policy =
        new PlacePetPolicy(
            place, AllowedPetSize.ALL, true, false, IndoorOutdoorType.OUTDOOR, true, "목줄 필수");
    when(placeService.upsertPetPolicy(eq("kakao-12345"), any(), any(), any(), any(), any(), any()))
        .thenReturn(policy);

    PlacePetPolicyRequest request =
        new PlacePetPolicyRequest(
            AllowedPetSize.ALL, true, false, IndoorOutdoorType.OUTDOOR, true, "목줄 필수");

    mockMvc
        .perform(
            put("/places/{externalPlaceId}/policy", "kakao-12345")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "place-policy-upsert",
                pathParameters(parameterWithName("externalPlaceId").description("외부 API 장소 ID")),
                requestFields(
                    fieldWithPath("allowedPetSize")
                        .description("허용 반려동물 크기 (SMALL/MEDIUM/LARGE/ALL)"),
                    fieldWithPath("leashRequired").description("목줄 필수 여부"),
                    fieldWithPath("carrierRequired").description("케이지 필수 여부"),
                    fieldWithPath("indoorOutdoorType").description("실내외 구분 (INDOOR/OUTDOOR/BOTH)"),
                    fieldWithPath("parking").description("주차 가능 여부"),
                    fieldWithPath("placeCaution").description("주의사항")),
                responseFields(
                    fieldWithPath("allowedPetSize").description("허용 반려동물 크기"),
                    fieldWithPath("leashRequired").description("목줄 필수 여부"),
                    fieldWithPath("carrierRequired").description("케이지 필수 여부"),
                    fieldWithPath("indoorOutdoorType").description("실내외 구분"),
                    fieldWithPath("parking").description("주차 가능 여부"),
                    fieldWithPath("placeCaution").description("주의사항"))));
  }
}
