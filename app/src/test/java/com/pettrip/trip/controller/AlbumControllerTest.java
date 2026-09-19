package com.pettrip.trip.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.trip.service.AlbumService;
import com.pettrip.trip.service.AlbumService.AlbumPhoto;
import com.pettrip.trip.service.AlbumService.CourseAlbum;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(AlbumController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class AlbumControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AlbumService albumService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private CourseAlbum sampleAlbum() {
    return new CourseAlbum(
        UUID.randomUUID(),
        LocalDate.of(2026, 8, 1),
        UUID.randomUUID(),
        List.of(
            new AlbumPhoto(
                UUID.randomUUID(),
                "https://bucket.s3.ap-northeast-2.amazonaws.com/review/u/1.jpg?sig",
                LocalDate.of(2026, 8, 1),
                LocalDateTime.of(2026, 8, 1, 10, 0, 0),
                "ext-1",
                true),
            new AlbumPhoto(
                UUID.randomUUID(),
                "https://bucket.s3.ap-northeast-2.amazonaws.com/review/u/2.jpg?sig",
                null,
                LocalDateTime.of(2026, 8, 1, 14, 30, 0),
                "ext-1",
                false)));
  }

  @Test
  void 내_앨범을_코스_단위로_조회한다() throws Exception {
    when(albumService.getMyAlbum(USER_ID)).thenReturn(List.of(sampleAlbum()));

    mockMvc
        .perform(get("/users/me/album").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "album-my",
                responseFields(
                    fieldWithPath("[].courseId").description("코스 ID"),
                    fieldWithPath("[].travelDate").description("여행일 (null 가능)").optional(),
                    fieldWithPath("[].petId").description("동행 펫 ID (null 가능)").optional(),
                    fieldWithPath("[].photos[]").description("이 코스에서 찍은 사진 목록"),
                    fieldWithPath("[].photos[].photoId").description("사진 ID"),
                    fieldWithPath("[].photos[].downloadUrl").description("presigned GET URL (10분)"),
                    fieldWithPath("[].photos[].takenAt").description("촬영일 (null 가능)").optional(),
                    fieldWithPath("[].photos[].createdAt")
                        .description("업로드 시각 (초 단위, FE 시간순 정밀 정렬용)"),
                    fieldWithPath("[].photos[].externalPlaceId").description("찍은 장소 ID"),
                    fieldWithPath("[].photos[].isPublic")
                        .description("리뷰에 등록돼 공개된 사진인지 (false=개인)"))));
  }

  @Test
  void 펫별_앨범을_조회한다() throws Exception {
    UUID petId = UUID.randomUUID();
    when(albumService.getPetAlbum(eq(USER_ID), eq(petId))).thenReturn(List.of(sampleAlbum()));

    mockMvc
        .perform(
            get("/users/me/pets/{petId}/album", petId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "album-pet",
                pathParameters(parameterWithName("petId").description("펫 ID")),
                responseFields(
                    fieldWithPath("[].courseId").description("코스 ID"),
                    fieldWithPath("[].travelDate").description("여행일 (null 가능)").optional(),
                    fieldWithPath("[].petId").description("동행 펫 ID (null 가능)").optional(),
                    fieldWithPath("[].photos[]").description("이 코스에서 찍은 사진 목록"),
                    fieldWithPath("[].photos[].photoId").description("사진 ID"),
                    fieldWithPath("[].photos[].downloadUrl").description("presigned GET URL (10분)"),
                    fieldWithPath("[].photos[].takenAt").description("촬영일 (null 가능)").optional(),
                    fieldWithPath("[].photos[].createdAt")
                        .description("업로드 시각 (초 단위, FE 시간순 정밀 정렬용)"),
                    fieldWithPath("[].photos[].externalPlaceId").description("찍은 장소 ID"),
                    fieldWithPath("[].photos[].isPublic").description("공개 여부 (false=개인)"))));
  }
}
