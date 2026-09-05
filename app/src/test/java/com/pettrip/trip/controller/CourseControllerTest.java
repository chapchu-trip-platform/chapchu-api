package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.place.model.Place;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.service.CourseService;
import com.pettrip.trip.service.CourseService.TravelCourseDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class CourseControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CourseService courseService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private TravelCourseDetail sampleDetail() {
    TravelCourse course =
        new TravelCourse(
            USER_ID,
            "강남구",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "종로구",
            new BigDecimal("37.6"),
            new BigDecimal("126.9"),
            LocalDate.of(2026, 8, 30));
    Place place =
        new Place(
            "ext-001",
            null,
            "한강공원",
            null,
            "서울시",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            null,
            null,
            null);
    CoursePlace cp = new CoursePlace(course, "ext-001", (short) 1, true);
    return new TravelCourseDetail(course, List.of(cp), Map.of("ext-001", place), Map.of());
  }

  @Test
  void 코스를_저장한다() throws Exception {
    TravelCourseDetail detail = sampleDetail();
    when(courseService.createCourse(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(),
            any()))
        .thenReturn(detail.course());
    when(courseService.getCourse(any(), any())).thenReturn(detail);

    CreateCourseRequest request =
        new CreateCourseRequest(
            UUID.randomUUID(),
            LocalDate.of(2026, 8, 30),
            "강남구",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "종로구",
            new BigDecimal("37.6"),
            new BigDecimal("126.9"),
            2,
            (short) 25,
            (short) 60,
            "맑음");

    mockMvc
        .perform(
            post("/courses")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "course-create",
                requestFields(
                    fieldWithPath("petId").description("반려동물 ID (필수)"),
                    fieldWithPath("travelDate").description("여행 날짜"),
                    fieldWithPath("startLocation").description("출발지 이름"),
                    fieldWithPath("startLat").description("출발지 위도"),
                    fieldWithPath("startLng").description("출발지 경도"),
                    fieldWithPath("endLocation").description("도착지 이름"),
                    fieldWithPath("endLat").description("도착지 위도"),
                    fieldWithPath("endLng").description("도착지 경도"),
                    fieldWithPath("intermediateStopCount")
                        .description("출발지·도착지를 제외한 중간 경유 장소 수 (최소 0)")
                        .type(JsonFieldType.NUMBER),
                    fieldWithPath("temperature").description("기온 (선택)").optional(),
                    fieldWithPath("humidity").description("습도 (선택)").optional(),
                    fieldWithPath("weatherStatus").description("날씨 상태 (선택)").optional()),
                responseFields(
                    fieldWithPath("courseId").description("코스 ID"),
                    fieldWithPath("travelDate").description("여행 날짜"),
                    fieldWithPath("startLocation").description("출발지 이름"),
                    fieldWithPath("endLocation").description("도착지 이름"),
                    fieldWithPath("places").description("방문 장소 목록").type(JsonFieldType.ARRAY),
                    fieldWithPath("places[].coursePlaceId").description("코스 장소 ID"),
                    fieldWithPath("places[].externalPlaceId").description("장소 외부 ID"),
                    fieldWithPath("places[].placeName").description("장소 이름"),
                    fieldWithPath("places[].placeImageUrl").description("장소 이미지 URL").optional(),
                    fieldWithPath("places[].latitude").description("위도").optional(),
                    fieldWithPath("places[].longitude").description("경도").optional(),
                    fieldWithPath("places[].visitOrder")
                        .description("방문 순서")
                        .type(JsonFieldType.NUMBER),
                    fieldWithPath("places[].finalPlace").description("마지막 방문 장소 여부"),
                    fieldWithPath("places[].petPolicy").description("반려동물 정책").optional())));
  }

  @Test
  void 내_코스를_조회한다() throws Exception {
    UUID courseId = UUID.fromString("0198f3a0-9999-7000-8000-000000000002");
    TravelCourseDetail detail = sampleDetail();
    when(courseService.getCourse(any(), any())).thenReturn(detail);

    mockMvc
        .perform(
            get("/courses/{courseId}", courseId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "course-get",
                pathParameters(parameterWithName("courseId").description("조회할 코스 ID")),
                responseFields(
                    fieldWithPath("courseId").description("코스 ID"),
                    fieldWithPath("travelDate").description("여행 날짜"),
                    fieldWithPath("startLocation").description("출발지 이름"),
                    fieldWithPath("endLocation").description("도착지 이름"),
                    fieldWithPath("places").description("방문 장소 목록").type(JsonFieldType.ARRAY),
                    fieldWithPath("places[].coursePlaceId").description("코스 장소 ID"),
                    fieldWithPath("places[].externalPlaceId").description("장소 외부 ID"),
                    fieldWithPath("places[].placeName").description("장소 이름"),
                    fieldWithPath("places[].placeImageUrl").description("장소 이미지 URL").optional(),
                    fieldWithPath("places[].latitude").description("위도").optional(),
                    fieldWithPath("places[].longitude").description("경도").optional(),
                    fieldWithPath("places[].visitOrder")
                        .description("방문 순서")
                        .type(JsonFieldType.NUMBER),
                    fieldWithPath("places[].finalPlace").description("마지막 방문 장소 여부"),
                    fieldWithPath("places[].petPolicy").description("반려동물 정책").optional())));
  }

  @Test
  void 코스를_완료한다() throws Exception {
    UUID courseId = UUID.randomUUID();
    mockMvc
        .perform(
            post("/courses/{courseId}/complete", courseId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "course-complete",
                pathParameters(parameterWithName("courseId").description("완료할 코스 ID"))));
  }
}
