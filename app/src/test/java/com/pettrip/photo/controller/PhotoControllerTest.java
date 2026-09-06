package com.pettrip.photo.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.model.PhotoType;
import com.pettrip.photo.service.PhotoService;
import java.net.URI;
import java.time.LocalDate;
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
@WebMvcTest(PhotoController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PhotoControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PhotoService photoService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 사진_업로드_URL을_발급한다() throws Exception {
    String photoKey = "visit/user-1/uuid-초코.jpg";
    when(photoService.buildPhotoKey(any(), eq(PhotoType.VISIT), eq("초코.jpg"))).thenReturn(photoKey);
    when(photoService.issueUploadUrl(photoKey))
        .thenReturn(
            URI.create("https://bucket.s3.ap-northeast-2.amazonaws.com/" + photoKey).toURL());

    String body =
        objectMapper.writeValueAsString(new PhotoUploadUrlRequest(PhotoType.VISIT, "초코.jpg"));

    mockMvc
        .perform(
            post("/photos/upload-url")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "photo-upload-url",
                requestFields(
                    fieldWithPath("type").description("사진 용도 (VISIT, POST, ALBUM, PROFILE)"),
                    fieldWithPath("fileName").description("업로드할 원본 파일명")),
                responseFields(
                    fieldWithPath("uploadUrl").description("S3 Presigned PUT URL (10분 유효)"),
                    fieldWithPath("photoKey").description("사진 저장 시 참조할 S3 경로"))));
  }

  @Test
  void 사진을_저장한다() throws Exception {
    UUID coursePlaceId = UUID.randomUUID();
    String photoKey = "visit/user-1/uuid-초코.jpg";
    LocalDate takenAt = LocalDate.of(2026, 7, 1);
    Photo photo = new Photo(UUID.randomUUID(), coursePlaceId, photoKey, takenAt);
    when(photoService.savePhoto(any(), eq(coursePlaceId), eq(photoKey), eq(takenAt)))
        .thenReturn(photo);

    String body =
        objectMapper.writeValueAsString(new PhotoCreateRequest(coursePlaceId, photoKey, takenAt));

    mockMvc
        .perform(
            post("/photos")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "photo-create",
                requestFields(
                    fieldWithPath("coursePlaceId")
                        .description("방문 장소 ID (course_place_id). 방문 인증이 아니면 생략 가능")
                        .optional(),
                    fieldWithPath("photoKey").description("업로드 URL 발급 시 받은 S3 경로"),
                    fieldWithPath("takenAt").description("촬영일 (선택)").optional()),
                responseFields(
                    fieldWithPath("id").description("사진 ID"),
                    fieldWithPath("coursePlaceId").description("방문 장소 ID").optional(),
                    fieldWithPath("photoKey").description("S3 경로"),
                    fieldWithPath("takenAt").description("촬영일").optional(),
                    fieldWithPath("createdAt").description("생성일시").optional())));
  }

  @Test
  void 사진_조회_URL을_발급한다() throws Exception {
    UUID photoId = UUID.randomUUID();
    String photoKey = "visit/user-1/uuid-초코.jpg";
    Photo photo = new Photo(USER_ID, UUID.randomUUID(), photoKey, LocalDate.of(2026, 7, 1));
    when(photoService.getOwnedPhoto(any(), eq(photoId))).thenReturn(photo);
    when(photoService.issueDownloadUrl(photoKey))
        .thenReturn(
            URI.create("https://bucket.s3.ap-northeast-2.amazonaws.com/" + photoKey).toURL());

    mockMvc
        .perform(
            get("/photos/{photoId}", photoId).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "photo-get",
                pathParameters(parameterWithName("photoId").description("조회할 사진 ID")),
                responseFields(
                    fieldWithPath("id").description("사진 ID"),
                    fieldWithPath("downloadUrl").description("S3 Presigned GET URL (10분 유효)"),
                    fieldWithPath("takenAt").description("촬영일").optional())));
  }
}
