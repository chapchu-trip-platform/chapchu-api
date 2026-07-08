package com.pettrip.review.repository;

import com.pettrip.review.model.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
