package com.pettrip.post.service;

import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private final PostRepository postRepository;

  public PostService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  public List<Post> listPosts() {
    return postRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public Post getPost(UUID postId) {
    Post post = findPost(postId);
    post.increaseViewCount();
    return postRepository.save(post);
  }

  public Post createPost(
      UUID userId, UUID petId, UUID photoId, UUID courseId, String title, String content) {
    return postRepository.save(new Post(userId, petId, photoId, courseId, title, content));
  }

  public Post updatePost(UUID userId, UUID postId, String title, String content) {
    Post post = getOwnedPost(userId, postId);
    post.update(title, content);
    return postRepository.save(post);
  }

  public void deletePost(UUID userId, UUID postId) {
    Post post = getOwnedPost(userId, postId);
    postRepository.delete(post);
  }

  private Post findPost(UUID postId) {
    return postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
  }

  private Post getOwnedPost(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (!post.getUserId().equals(userId)) {
      throw new PostNotFoundException();
    }
    return post;
  }
}
