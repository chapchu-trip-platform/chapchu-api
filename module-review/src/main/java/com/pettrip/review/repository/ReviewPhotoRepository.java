package com.pettrip.review.repository;

import com.pettrip.review.model.ReviewPhoto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, UUID> {

  List<ReviewPhoto> findByReviewIdOrderByPhotoOrderAsc(UUID reviewId);

  List<ReviewPhoto> findByReviewIdInOrderByPhotoOrderAsc(Collection<UUID> reviewIds);
}
