package com.pettrip.wishlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.service.WishlistService;
import java.util.List;
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
@WebMvcTest(WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class WishlistControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WishlistService wishlistService;

  @Test
  void 위시리스트_목록을_조회한다() throws Exception {
    when(wishlistService.listMyWishlist(any()))
        .thenReturn(List.of(new Wishlist(UUID.randomUUID(), "place-123")));

    mockMvc
        .perform(get("/users/me/wishlist"))
        .andExpect(status().isOk())
        .andDo(document("wishlist-list"));
  }

  @Test
  void 위시리스트에서_장소를_제거한다() throws Exception {
    mockMvc
        .perform(delete("/users/me/wishlist/{placeId}", "place-123"))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "wishlist-delete",
                pathParameters(
                    parameterWithName("placeId").description("제거할 장소의 external place id"))));

    verify(wishlistService).removeFromWishlist(any(), eq("place-123"));
  }
}
