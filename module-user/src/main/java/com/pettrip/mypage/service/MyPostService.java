package com.pettrip.mypage.service;

import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MyPostService {

  private final PostRepository postRepository;

  public MyPostService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  public List<Post> listMyPosts(UUID userId) {
    return postRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }
}
