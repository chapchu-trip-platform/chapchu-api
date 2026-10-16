package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.photo.service.PhotoService;
import com.pettrip.trip.service.AlbumService.AlbumRow;
import com.pettrip.trip.service.AlbumService.CourseAlbum;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

  @Mock private PhotoService photoService;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private AlbumService albumService;

  @BeforeEach
  void setUp() {
    albumService = new AlbumService(photoService, jdbcTemplate);
  }

  @Test
  void getMyAlbum은_같은_코스의_사진을_한_묶음으로_그룹하고_isPublic을_보존한다() throws Exception {
    UUID courseId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 1);
    LocalDateTime uploadedFirst = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
    LocalDateTime uploadedSecond = LocalDateTime.of(2026, 8, 1, 14, 30, 0);
    AlbumRow pub =
        new AlbumRow(
            courseId, date, petId, UUID.randomUUID(), "k1", date, uploadedFirst, "ext-1", true);
    AlbumRow priv =
        new AlbumRow(
            courseId, date, petId, UUID.randomUUID(), "k2", null, uploadedSecond, "ext-1", false);
    when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(pub, priv));
    when(photoService.issueDownloadUrl(any())).thenReturn(URI.create("https://bucket/x").toURL());

    List<CourseAlbum> album = albumService.getMyAlbum(UUID.randomUUID());

    assertThat(album).hasSize(1);
    assertThat(album.get(0).courseId()).isEqualTo(courseId);
    assertThat(album.get(0).petId()).isEqualTo(petId);
    assertThat(album.get(0).photos()).hasSize(2);
    assertThat(album.get(0).photos().get(0).isPublic()).isTrue();
    assertThat(album.get(0).photos().get(1).isPublic()).isFalse();
    assertThat(album.get(0).photos().get(0).downloadUrl()).isEqualTo("https://bucket/x");
  }

  @Test
  void getMyAlbum은_같은_날_사진도_구분할_수_있게_createdAt을_노출한다() throws Exception {
    UUID courseId = UUID.randomUUID();
    LocalDate sameDay = LocalDate.of(2026, 8, 1);
    LocalDateTime morning = LocalDateTime.of(2026, 8, 1, 9, 15, 0);
    LocalDateTime evening = LocalDateTime.of(2026, 8, 1, 18, 40, 0);
    AlbumRow earlier =
        new AlbumRow(
            courseId, sameDay, null, UUID.randomUUID(), "k1", sameDay, morning, "ext-1", false);
    AlbumRow later =
        new AlbumRow(
            courseId, sameDay, null, UUID.randomUUID(), "k2", sameDay, evening, "ext-1", false);
    when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(earlier, later));
    when(photoService.issueDownloadUrl(any())).thenReturn(URI.create("https://bucket/x").toURL());

    List<CourseAlbum> album = albumService.getMyAlbum(UUID.randomUUID());

    assertThat(album.get(0).photos().get(0).takenAt())
        .isEqualTo(album.get(0).photos().get(1).takenAt());
    assertThat(album.get(0).photos().get(0).createdAt()).isEqualTo(morning);
    assertThat(album.get(0).photos().get(1).createdAt()).isEqualTo(evening);
  }

  @Test
  void BASE_SQL은_created_at을_조회하고_그_순서로_정렬한다() {
    when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    albumService.getMyAlbum(UUID.randomUUID());

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));
    assertThat(sql.getValue()).contains("p.created_at");
    assertThat(sql.getValue()).contains("ORDER BY");
    assertThat(sql.getValue()).endsWith("p.created_at");
  }

  @Test
  void getPetAlbum은_결과가_없으면_빈_리스트를_반환한다() {
    when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    assertThat(albumService.getPetAlbum(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }
}
