package com.pettrip.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.review.model.Review;
import com.pettrip.review.service.ReviewService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(MyReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class MyReviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReviewService reviewService;

  @Test
  void 작성한_리뷰_목록을_조회한다() throws Exception {
    Review review =
        new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "정말 좋은 곳이었어요");
    when(reviewService.listMyReviews(any())).thenReturn(List.of(review));

    mockMvc
        .perform(get("/users/me/reviews"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "my-review-list",
                responseFields(
                    fieldWithPath("[].id").description("리뷰 ID"),
                    fieldWithPath("[].placeId").description("장소 외부 ID"),
                    fieldWithPath("[].petId").description("리뷰 작성 시 동행한 반려견 ID"),
                    fieldWithPath("[].rating").description("별점").type(JsonFieldType.NUMBER),
                    fieldWithPath("[].contents").description("리뷰 내용"),
                    fieldWithPath("[].recommendationCount").description("추천 수"),
                    fieldWithPath("[].createdAt").description("작성일시"))));
  }
}
