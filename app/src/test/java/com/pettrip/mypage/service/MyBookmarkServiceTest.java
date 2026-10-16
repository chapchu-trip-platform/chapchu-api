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
class MyBookmarkServiceTest {

  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private MyBookmarkService myBookmarkService;

  @BeforeEach
  void setUp() {
    myBookmarkService = new MyBookmarkService(jdbcTemplate);
  }

  @Test
  void listMyBookmarks는_북마크한_게시글을_반환한다() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    List<PostResponse> result = myBookmarkService.listMyBookmarks(userId);

    assertThat(result).isEmpty();
  }
}
