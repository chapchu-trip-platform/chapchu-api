package com.pettrip.comment.repository;

import com.pettrip.comment.model.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  long countByPostId(UUID postId);
}
