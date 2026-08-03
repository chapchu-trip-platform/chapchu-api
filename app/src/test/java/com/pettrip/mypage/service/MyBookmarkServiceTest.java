package com.pettrip.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pettrip.post.model.Post;
import com.pettrip.post.model.PostBookmark;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBookmarkServiceTest {

  @Mock private PostBookmarkRepository postBookmarkRepository;
  @Mock private PostRepository postRepository;

  private MyBookmarkService myBookmarkService;

  @BeforeEach
  void setUp() {
    myBookmarkService = new MyBookmarkService(postBookmarkRepository, postRepository);
  }

  @Test
  void listMyBookmarks는_북마크한_게시글을_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    PostBookmark bookmark = new PostBookmark(userId, postId);
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(bookmark));
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));

    List<Post> result = myBookmarkService.listMyBookmarks(userId);

    assertThat(result).containsExactly(post);
  }
}
