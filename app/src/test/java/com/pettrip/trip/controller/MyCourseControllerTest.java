package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.trip.model.StartCourse;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.service.CourseService;
import java.time.LocalDate;
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
@WebMvcTest(MyCourseController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class MyCourseControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CourseService courseService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 내_코스_목록을_조회한다() throws Exception {
    StartCourse start = new StartCourse("강남구", LocalDateTime.now());
    TravelCourse course = new TravelCourse(USER_ID, start, LocalDate.of(2026, 8, 30));
    when(courseService.listMyCourses(any())).thenReturn(List.of(course));

    mockMvc
        .perform(get("/users/me/courses").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "my-course-list",
                responseFields(
                    fieldWithPath("[].courseId").description("코스 ID"),
                    fieldWithPath("[].travelDate").description("여행 날짜"),
                    fieldWithPath("[].startLocation").description("출발 위치"),
                    fieldWithPath("[].isCompleted").description("코스 완료 여부"),
                    fieldWithPath("[].placeCount")
                        .description("방문 장소 수")
                        .type(JsonFieldType.NUMBER))));
  }
}
