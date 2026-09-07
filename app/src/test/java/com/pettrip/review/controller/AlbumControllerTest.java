package com.pettrip.review.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.review.service.AlbumItem;
import com.pettrip.review.service.ReviewService;
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
@WebMvcTest(AlbumController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class AlbumControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReviewService reviewService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 내_앨범을_조회한다() throws Exception {
    when(reviewService.getMyAlbum(any()))
        .thenReturn(
            List.of(
                new AlbumItem(
                    UUID.randomUUID(),
                    "https://bucket.s3.ap-northeast-2.amazonaws.com/review/u/1.jpg?sig=x",
                    LocalDate.of(2026, 7, 1),
                    UUID.randomUUID(),
                    "place-A")));

    mockMvc
        .perform(get("/users/me/album").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "album-list",
                responseFields(
                    fieldWithPath("[].photoId").description("사진 ID"),
                    fieldWithPath("[].downloadUrl").description("presigned GET URL (10분 유효)"),
                    fieldWithPath("[].takenAt").description("촬영일").optional(),
                    fieldWithPath("[].reviewId").description("이 사진이 붙은 리뷰 ID"),
                    fieldWithPath("[].placeId").description("리뷰 대상 장소 외부 ID"))));
  }
}
