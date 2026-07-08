package com.pettrip.post.repository;

import com.pettrip.post.model.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {

  List<Post> findAllByOrderByCreatedAtDesc();

  List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
