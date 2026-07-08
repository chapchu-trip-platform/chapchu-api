package com.pettrip.photo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.service.PhotoService;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
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
@WebMvcTest(PhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PhotoControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PhotoService photoService;

  @Test
  void 사진_업로드_URL을_발급한다() throws Exception {
    String photoKey = "photos/user-1/uuid-초코.jpg";
    when(photoService.buildPhotoKey(any(), eq("초코.jpg"))).thenReturn(photoKey);
    when(photoService.issueUploadUrl(photoKey))
        .thenReturn(
            URI.create("https://bucket.s3.ap-northeast-2.amazonaws.com/" + photoKey).toURL());

    String body = objectMapper.writeValueAsString(new PhotoUploadUrlRequest("초코.jpg"));

    mockMvc
        .perform(post("/photos/upload-url").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "photo-upload-url",
                requestFields(fieldWithPath("fileName").description("업로드할 원본 파일명")),
                responseFields(
                    fieldWithPath("uploadUrl").description("S3 Presigned PUT URL (10분 유효)"),
                    fieldWithPath("photoKey").description("사진 저장 시 참조할 S3 경로"))));
  }

  @Test
  void 사진을_저장한다() throws Exception {
    UUID coursePlaceId = UUID.randomUUID();
    String photoKey = "photos/user-1/uuid-초코.jpg";
    LocalDate takenAt = LocalDate.of(2026, 7, 1);
    Photo photo = new Photo(UUID.randomUUID(), coursePlaceId, photoKey, takenAt);
    when(photoService.savePhoto(any(), eq(coursePlaceId), eq(photoKey), eq(takenAt)))
        .thenReturn(photo);

    String body =
        objectMapper.writeValueAsString(new PhotoCreateRequest(coursePlaceId, photoKey, takenAt));

    mockMvc
        .perform(post("/photos").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "photo-create",
                requestFields(
                    fieldWithPath("coursePlaceId").description("방문 장소 ID (course_place_id)"),
                    fieldWithPath("photoKey").description("업로드 URL 발급 시 받은 S3 경로"),
                    fieldWithPath("takenAt").description("촬영일 (선택)")),
                responseFields(
                    fieldWithPath("id").description("사진 ID"),
                    fieldWithPath("coursePlaceId").description("방문 장소 ID"),
                    fieldWithPath("photoKey").description("S3 경로"),
                    fieldWithPath("takenAt").description("촬영일"),
                    fieldWithPath("createdAt").description("생성일시"))));
  }
}
