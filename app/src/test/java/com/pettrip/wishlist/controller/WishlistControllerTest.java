package com.pettrip.wishlist.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.wishlist.service.WishlistService;
import java.math.BigDecimal;
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
@WebMvcTest(WishlistController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class WishlistControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WishlistService wishlistService;
  @MockitoBean private JwtDecoder jwtDecoder;

  private WishlistResponse sampleResponse() {
    return new WishlistResponse(
        "126508",
        "안목해변",
        "https://example.com/a.jpg",
        "강원 강릉시 창해로14번길 20",
        new BigDecimal("37.7735000"),
        new BigDecimal("128.9470000"),
        (short) 5,
        128,
        LocalDateTime.of(2026, 9, 1, 10, 0, 0));
  }

  @Test
  void 위시리스트_목록을_조회한다() throws Exception {
    when(wishlistService.listMyWishlist(USER_ID)).thenReturn(List.of(sampleResponse()));

    mockMvc
        .perform(get("/users/me/wishlist").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].placeName").value("안목해변"))
        .andDo(
            document(
                "wishlist-list",
                responseFields(
                    fieldWithPath("[].placeId").description("장소 ID (TourAPI contentId)"),
                    fieldWithPath("[].placeName").description("장소 이름"),
                    fieldWithPath("[].placeImageUrl")
                        .description("대표 이미지 URL (null 가능)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("[].address")
                        .description("주소 (null 가능)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("[].latitude").description("위도"),
                    fieldWithPath("[].longitude").description("경도"),
                    fieldWithPath("[].rating")
                        .description("평점 (null 가능)")
                        .type(JsonFieldType.NUMBER)
                        .optional(),
                    fieldWithPath("[].reviewNum")
                        .description("리뷰 수 (null 가능)")
                        .type(JsonFieldType.NUMBER)
                        .optional(),
                    fieldWithPath("[].createdAt").description("찜한 일시"))));
  }

  @Test
  void 장소를_위시리스트에_추가한다() throws Exception {
    mockMvc
        .perform(
            post("/users/me/wishlist/{placeId}", "126508")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "wishlist-create",
                pathParameters(
                    parameterWithName("placeId").description("찜할 장소의 external place id"))));

    verify(wishlistService).addToWishlist(USER_ID, "126508");
  }

  @Test
  void 위시리스트에서_장소를_제거한다() throws Exception {
    mockMvc
        .perform(
            delete("/users/me/wishlist/{placeId}", "126508")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "wishlist-delete",
                pathParameters(
                    parameterWithName("placeId").description("제거할 장소의 external place id"))));

    verify(wishlistService).removeFromWishlist(USER_ID, "126508");
  }
}
