package com.pettrip.review.service;

import com.pettrip.review.model.Review;
import java.util.List;

/** 리뷰 + 그 리뷰에 붙은 사진들(다운로드 URL 포함). 컨트롤러가 응답 DTO로 매핑한다. */
public record ReviewDetail(Review review, List<ReviewPhotoView> photos) {}
