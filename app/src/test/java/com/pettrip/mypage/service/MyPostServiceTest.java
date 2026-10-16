package com.pettrip.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pettrip.post.controller.PostResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class MyPostServiceTest {

  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private MyPostService myPostService;

  @BeforeEach
  void setUp() {
    myPostService = new MyPostService(jdbcTemplate);
  }

  @Test
  void listMyPosts는_내_게시글을_반환한다() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    List<PostResponse> result = myPostService.listMyPosts(userId);

    assertThat(result).isEmpty();
  }
}
