package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.pettrip.photo.service.PhotoService;
import com.pettrip.trip.service.AlbumService.AlbumRow;
import com.pettrip.trip.service.AlbumService.CourseAlbum;
import java.net.URI;
import java.time.LocalDate;
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
    AlbumRow pub =
        new AlbumRow(courseId, date, petId, UUID.randomUUID(), "k1", date, "ext-1", true);
    AlbumRow priv =
        new AlbumRow(courseId, date, petId, UUID.randomUUID(), "k2", null, "ext-1", false);
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
  void getPetAlbum은_결과가_없으면_빈_리스트를_반환한다() {
    when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    assertThat(albumService.getPetAlbum(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }
}
