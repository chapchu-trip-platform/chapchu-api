package com.pettrip.mypage.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.post.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPostServiceTest {

  @Mock private PostRepository postRepository;

  private MyPostService myPostService;

  @BeforeEach
  void setUp() {
    myPostService = new MyPostService(postRepository);
  }

  @Test
  void listMyPosts는_레포지토리에_위임한다() {
    UUID userId = UUID.randomUUID();
    when(postRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

    myPostService.listMyPosts(userId);

    verify(postRepository).findByUserIdOrderByCreatedAtDesc(userId);
  }
}
