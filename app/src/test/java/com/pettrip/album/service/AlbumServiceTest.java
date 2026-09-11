package com.pettrip.album.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.album.controller.AlbumResponse;
import com.pettrip.album.model.Album;
import com.pettrip.album.model.AlbumPhoto;
import com.pettrip.album.repository.AlbumPhotoRepository;
import com.pettrip.album.repository.AlbumRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlbumServiceTest {

  @Mock private AlbumRepository albumRepository;
  @Mock private AlbumPhotoRepository albumPhotoRepository;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private AlbumService albumService;

  @BeforeEach
  void setUp() {
    albumService = new AlbumService(albumRepository, albumPhotoRepository, jdbcTemplate);
  }

  private Album stubAlbum(UUID userId) {
    Album album = new Album(userId, "내 앨범");
    when(albumRepository.findByUserId(userId)).thenReturn(Optional.of(album));
    return album;
  }

  @Test
  void getMyAlbum은_앨범이_없으면_만들어_준다() {
    UUID userId = UUID.randomUUID();
    when(albumRepository.findByUserId(userId)).thenReturn(Optional.empty());
    when(albumRepository.save(any(Album.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    AlbumResponse result = albumService.getMyAlbum(userId);

    assertThat(result.albumName()).isEqualTo("내 앨범");
    verify(albumRepository).save(any(Album.class));
  }

  @Test
  void getMyAlbum은_isDie로_앨범과_추억앨범을_가른다() throws Exception {
    UUID userId = UUID.randomUUID();
    stubAlbum(userId);
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(2);
              return List.of(
                  mapper.mapRow(photoRow("초코", false), 0),
                  mapper.mapRow(photoRow("두부", true), 1),
                  mapper.mapRow(photoRow("두부", true), 2));
            });

    AlbumResponse result = albumService.getMyAlbum(userId);

    assertThat(result.album().photoCount()).isEqualTo(1);
    assertThat(result.album().photos().get(0).petName()).isEqualTo("초코");
    assertThat(result.memorialAlbum().photoCount()).isEqualTo(2);
    assertThat(result.memorialAlbum().photos().get(0).petName()).isEqualTo("두부");
  }

  private java.sql.ResultSet photoRow(String petName, boolean isDie) throws Exception {
    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
    when(rs.getObject("photo_id", UUID.class)).thenReturn(UUID.randomUUID());
    when(rs.getString("photo_url")).thenReturn("review/user-1/x.jpg");
    when(rs.getObject("pet_id", UUID.class)).thenReturn(UUID.randomUUID());
    when(rs.getString("pet_name")).thenReturn(petName);
    when(rs.getBoolean("is_die")).thenReturn(isDie);
    when(rs.getDate("taken_at")).thenReturn(null);
    return rs;
  }

  @Test
  void addPhotos는_남의_반려동물이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    stubAlbum(userId);
    when(jdbcTemplate.queryForObject(
            any(String.class), any(SqlParameterSource.class), any(Class.class)))
        .thenReturn(false);

    assertThatThrownBy(
            () -> albumService.addPhotos(userId, UUID.randomUUID(), List.of(UUID.randomUUID())))
        .isInstanceOf(PetNotOwnedException.class);

    verify(albumPhotoRepository, never()).save(any());
  }

  @Test
  void addPhotos는_남의_사진이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    stubAlbum(userId);
    when(jdbcTemplate.queryForObject(
            any(String.class), any(SqlParameterSource.class), any(Class.class)))
        .thenReturn(true);
    when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class), any()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () -> albumService.addPhotos(userId, UUID.randomUUID(), List.of(UUID.randomUUID())))
        .isInstanceOf(PhotoNotOwnedException.class);

    verify(albumPhotoRepository, never()).save(any());
  }

  @Test
  void addPhotos는_이미_담긴_사진은_건너뛴다() {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Album album = stubAlbum(userId);
    when(jdbcTemplate.queryForObject(
            any(String.class), any(SqlParameterSource.class), any(Class.class)))
        .thenReturn(true);
    when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class), any()))
        .thenReturn(List.of(photoId));
    when(albumPhotoRepository.existsByAlbumIdAndPhotoId(album.getId(), photoId)).thenReturn(true);

    albumService.addPhotos(userId, UUID.randomUUID(), List.of(photoId));

    verify(albumPhotoRepository, never()).save(any(AlbumPhoto.class));
  }

  @Test
  void removePhoto는_앨범에_없는_사진이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Album album = stubAlbum(userId);
    when(albumPhotoRepository.existsByAlbumIdAndPhotoId(album.getId(), photoId)).thenReturn(false);

    assertThatThrownBy(() -> albumService.removePhoto(userId, photoId))
        .isInstanceOf(AlbumPhotoNotFoundException.class);
  }
}
