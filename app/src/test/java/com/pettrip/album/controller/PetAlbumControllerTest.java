package com.pettrip.album.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.album.service.AlbumService;
import com.pettrip.config.SecurityConfig;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PetAlbumController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PetAlbumControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AlbumService albumService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private AlbumPhotoItem photo(String petName) {
    return new AlbumPhotoItem(
        UUID.randomUUID(),
        "review/user-1/x.jpg",
        UUID.randomUUID(),
        petName,
        LocalDate.of(2026, 5, 1));
  }

  @Test
  void 앨범을_조회하면_isDie로_두_묶음이_나뉜다() throws Exception {
    AlbumPhotoItem alive = photo("초코");
    AlbumPhotoItem gone = photo("두부");
    when(albumService.getMyAlbum(USER_ID))
        .thenReturn(
            new AlbumResponse(
                UUID.randomUUID(),
                "내 앨범",
                new AlbumGroupResponse(1, List.of(alive)),
                new AlbumGroupResponse(1, List.of(gone))));

    mockMvc
        .perform(get("/albums").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.album.photos[0].petName").value("초코"))
        .andExpect(jsonPath("$.memorialAlbum.photos[0].petName").value("두부"))
        .andDo(
            document(
                "album-get",
                responseFields(
                    fieldWithPath("albumId").description("앨범 ID. 유저당 하나"),
                    fieldWithPath("albumName").description("앨범 이름"),
                    fieldWithPath("album").description("isDie=false인 아이들의 사진 묶음"),
                    fieldWithPath("album.photoCount").description("사진 수"),
                    fieldWithPath("album.photos[].photoId").description("사진 ID"),
                    fieldWithPath("album.photos[].photoKey").description("S3 키"),
                    fieldWithPath("album.photos[].petId").description("사진 속 반려동물 ID"),
                    fieldWithPath("album.photos[].petName").description("반려동물 이름"),
                    fieldWithPath("album.photos[].takenAt")
                        .description("촬영 날짜 (null 가능)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("memorialAlbum").description("isDie=true인 아이들의 사진 묶음"),
                    fieldWithPath("memorialAlbum.photoCount").description("사진 수"),
                    fieldWithPath("memorialAlbum.photos[].photoId").description("사진 ID"),
                    fieldWithPath("memorialAlbum.photos[].photoKey").description("S3 키"),
                    fieldWithPath("memorialAlbum.photos[].petId").description("사진 속 반려동물 ID"),
                    fieldWithPath("memorialAlbum.photos[].petName").description("반려동물 이름"),
                    fieldWithPath("memorialAlbum.photos[].takenAt")
                        .description("촬영 날짜 (null 가능)")
                        .type(JsonFieldType.STRING)
                        .optional())));
  }

  @Test
  void 앨범에_사진을_담는다() throws Exception {
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    String body =
        objectMapper.writeValueAsString(new AlbumPhotoAddRequest(petId, List.of(photoId)));

    mockMvc
        .perform(
            post("/albums/photos")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "album-photo-add",
                requestFields(
                    fieldWithPath("petId").description("사진 속 반려동물. 이 아이의 isDie로 앨범/추억앨범이 갈린다. 필수"),
                    fieldWithPath("photoIds")
                        .description("담을 사진 ID 목록. POST /photos로 먼저 등록한 뒤 그 id를 보낸다"))));

    verify(albumService).addPhotos(USER_ID, petId, List.of(photoId));
  }

  @Test
  void 앨범에서_사진을_뺀다() throws Exception {
    UUID photoId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/albums/photos/{photoId}", photoId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "album-photo-remove",
                pathParameters(parameterWithName("photoId").description("뺄 사진 ID"))));

    verify(albumService).removePhoto(USER_ID, photoId);
  }

  @Test
  void 반려동물을_지정하지_않으면_400() throws Exception {
    String body =
        objectMapper.writeValueAsString(new AlbumPhotoAddRequest(null, List.of(UUID.randomUUID())));

    mockMvc
        .perform(
            post("/albums/photos")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest());

    verify(albumService, never()).addPhotos(any(), any(), any());
  }
}
