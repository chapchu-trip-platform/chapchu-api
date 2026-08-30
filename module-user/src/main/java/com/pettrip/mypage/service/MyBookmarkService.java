package com.pettrip.mypage.service;

import com.pettrip.post.controller.PostResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MyBookmarkService {

  private static final String MY_BOOKMARKS_SQL =
      """
      SELECT p.post_id, p.user_id, p.pet_id, p.photo_id, p.course_id,
             p.title, p.content, p.view_count, p.recommendation_count, p.comment_count, p.created_at,
             COALESCE(u.nickname, '(탈퇴한 사용자)') AS nickname,
             ph.photo_url
      FROM post_bookmarks pb
      JOIN posts p ON pb.post_id = p.post_id
      LEFT JOIN users u ON p.user_id = u.user_id
      LEFT JOIN photos ph ON p.photo_id = ph.photo_id
      WHERE pb.user_id = :userId
      ORDER BY pb.created_at DESC
      """;

  private static final RowMapper<PostResponse> POST_ROW_MAPPER =
      (rs, rowNum) ->
          new PostResponse(
              rs.getObject("post_id", UUID.class),
              rs.getObject("pet_id", UUID.class),
              rs.getObject("photo_id", UUID.class),
              rs.getObject("course_id", UUID.class),
              rs.getString("title"),
              rs.getString("content"),
              rs.getInt("view_count"),
              rs.getInt("recommendation_count"),
              rs.getInt("comment_count"),
              rs.getString("nickname"),
              rs.getString("photo_url"),
              rs.getTimestamp("created_at").toLocalDateTime());

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MyBookmarkService(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<PostResponse> listMyBookmarks(UUID userId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return jdbcTemplate.query(MY_BOOKMARKS_SQL, params, POST_ROW_MAPPER);
  }
}
