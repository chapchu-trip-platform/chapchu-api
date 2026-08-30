package com.pettrip.post.repository;

import com.pettrip.post.model.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

  List<Post> findAllByOrderByCreatedAtDesc();

  List<Post> findAllByOrderByRecommendationCountDescCreatedAtDesc();

  List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
  int incrementViewCount(@Param("postId") UUID postId);

  @Modifying
  @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
  void incrementCommentCount(@Param("postId") UUID postId);

  @Modifying
  @Query(
      "UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :postId")
  void decrementCommentCount(@Param("postId") UUID postId);
}
