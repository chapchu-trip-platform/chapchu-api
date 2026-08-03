package com.pettrip.comment.service;

import com.pettrip.comment.model.Comment;
import com.pettrip.comment.repository.CommentRepository;
import com.pettrip.post.repository.PostRepository;
import com.pettrip.post.service.PostNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;

  public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
    this.commentRepository = commentRepository;
    this.postRepository = postRepository;
  }

  public Comment createComment(UUID userId, UUID postId, UUID parentCommentId, String content) {
    if (!postRepository.existsById(postId)) {
      throw new PostNotFoundException();
    }
    int depth = 0;
    if (parentCommentId != null) {
      Comment parent =
          commentRepository.findById(parentCommentId).orElseThrow(CommentNotFoundException::new);
      depth = parent.getDepth() + 1;
    }
    int order = (int) commentRepository.countByPostId(postId) + 1;
    return commentRepository.save(
        new Comment(postId, userId, parentCommentId, depth, order, content));
  }

  public void deleteComment(UUID userId, UUID commentId) {
    Comment comment =
        commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
    if (!comment.getUserId().equals(userId)) {
      throw new CommentNotFoundException();
    }
    commentRepository.delete(comment);
  }
}
