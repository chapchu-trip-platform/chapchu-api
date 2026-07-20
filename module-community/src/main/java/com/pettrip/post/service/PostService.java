package com.pettrip.post.service;

import com.pettrip.post.model.Post;
import com.pettrip.post.model.PostBookmark;
import com.pettrip.post.model.PostRecommendation;
import com.pettrip.post.model.PostReport;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRecommendationRepository;
import com.pettrip.post.repository.PostReportRepository;
import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private final PostRepository postRepository;
  private final PostRecommendationRepository postRecommendationRepository;
  private final PostBookmarkRepository postBookmarkRepository;
  private final PostReportRepository postReportRepository;

  public PostService(
      PostRepository postRepository,
      PostRecommendationRepository postRecommendationRepository,
      PostBookmarkRepository postBookmarkRepository,
      PostReportRepository postReportRepository) {
    this.postRepository = postRepository;
    this.postRecommendationRepository = postRecommendationRepository;
    this.postBookmarkRepository = postBookmarkRepository;
    this.postReportRepository = postReportRepository;
  }

  public List<Post> listPosts(String sort) {
    if ("popular".equals(sort)) {
      return postRepository.findAllByOrderByRecommendationCountDescCreatedAtDesc();
    }
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

  @Transactional
  public void recommend(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (postRecommendationRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostAlreadyRecommendedException();
    }
    postRecommendationRepository.save(new PostRecommendation(postId, userId));
    post.incrementRecommendationCount();
    postRepository.save(post);
  }

  @Transactional
  public void cancelRecommendation(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (!postRecommendationRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostRecommendationNotFoundException();
    }
    postRecommendationRepository.deleteByPostIdAndUserId(postId, userId);
    post.decrementRecommendationCount();
    postRepository.save(post);
  }

  public void bookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostAlreadyBookmarkedException();
    }
    postBookmarkRepository.save(new PostBookmark(userId, postId));
  }

  public void cancelBookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (!postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostBookmarkNotFoundException();
    }
    postBookmarkRepository.deleteByUserIdAndPostId(userId, postId);
  }

  public void report(UUID userId, UUID postId, String reportReason, String reportDetail) {
    findPost(postId);
    if (postReportRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostAlreadyReportedException();
    }
    postReportRepository.save(new PostReport(postId, userId, reportReason, reportDetail));
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
