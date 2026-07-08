package com.pettrip.mypage.service;

import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MyBookmarkService {

  private final PostBookmarkRepository postBookmarkRepository;
  private final PostRepository postRepository;

  public MyBookmarkService(
      PostBookmarkRepository postBookmarkRepository, PostRepository postRepository) {
    this.postBookmarkRepository = postBookmarkRepository;
    this.postRepository = postRepository;
  }

  public List<Post> listMyBookmarks(UUID userId) {
    return postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(bookmark -> postRepository.findById(bookmark.getPostId()))
        .flatMap(Optional::stream)
        .toList();
  }
}
