package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.pettrip.trip.model.CourseWeatherRecord;
import com.pettrip.trip.service.WeatherService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(WeatherController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class WeatherControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");
  private static final UUID COURSE_ID = UUID.fromString("0198f3a0-5678-7000-8000-000000000002");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private WeatherService weatherService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private CourseWeatherRecord sampleRecord() {
    return new CourseWeatherRecord(
        COURSE_ID, LocalDate.of(2026, 8, 1), (short) 28, (short) 65, "맑음", "자외선 주의");
  }

  @Test
  void 날씨를_기록한다() throws Exception {
    CourseWeatherRecord record = sampleRecord();
    when(weatherService.saveWeather(any(), eq(COURSE_ID), any(), any(), any(), any(), any()))
        .thenReturn(record);

    WeatherRecordRequest request =
        new WeatherRecordRequest(LocalDate.of(2026, 8, 1), (short) 28, (short) 65, "맑음", "자외선 주의");

    mockMvc
        .perform(
            post("/courses/{courseId}/weather", COURSE_ID)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "weather-record-create",
                pathParameters(parameterWithName("courseId").description("코스 ID")),
                requestFields(
                    fieldWithPath("weatherDate").description("날씨 측정 날짜 (yyyy-MM-dd)"),
                    fieldWithPath("temperature").description("기온 (°C)"),
                    fieldWithPath("humidity").description("습도 (%)"),
                    fieldWithPath("weatherStatus").description("날씨 상태 (예: 맑음, 흐림, 비)"),
                    fieldWithPath("weatherCaution")
                        .optional()
                        .description("날씨 주의사항 (예: 자외선 주의). 없으면 null")),
                responseFields(
                    fieldWithPath("id").description("날씨 기록 ID"),
                    fieldWithPath("courseId").description("코스 ID"),
                    fieldWithPath("weatherDate").description("날씨 측정 날짜"),
                    fieldWithPath("temperature").description("기온 (°C)"),
                    fieldWithPath("humidity").description("습도 (%)"),
                    fieldWithPath("weatherStatus").description("날씨 상태"),
                    fieldWithPath("weatherCaution").description("날씨 주의사항"))));
  }

  @Test
  void 날씨_기록_목록을_조회한다() throws Exception {
    when(weatherService.getWeather(any(), eq(COURSE_ID))).thenReturn(List.of(sampleRecord()));

    mockMvc
        .perform(
            get("/courses/{courseId}/weather", COURSE_ID)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "weather-record-list",
                pathParameters(parameterWithName("courseId").description("코스 ID")),
                responseFields(
                    fieldWithPath("[].id").description("날씨 기록 ID"),
                    fieldWithPath("[].courseId").description("코스 ID"),
                    fieldWithPath("[].weatherDate").description("날씨 측정 날짜"),
                    fieldWithPath("[].temperature").description("기온 (°C)"),
                    fieldWithPath("[].humidity").description("습도 (%)"),
                    fieldWithPath("[].weatherStatus").description("날씨 상태"),
                    fieldWithPath("[].weatherCaution").description("날씨 주의사항"))));
  }
}
