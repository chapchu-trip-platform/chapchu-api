package com.pettrip.post.repository;

import com.pettrip.post.model.PostBookmark;
import com.pettrip.post.model.PostBookmarkId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {

  boolean existsByUserIdAndPostId(UUID userId, UUID postId);

  void deleteByUserIdAndPostId(UUID userId, UUID postId);

  List<PostBookmark> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
