package com.pettrip.album.service;

import com.pettrip.album.controller.AlbumGroupResponse;
import com.pettrip.album.controller.AlbumPhotoItem;
import com.pettrip.album.controller.AlbumResponse;
import com.pettrip.album.model.Album;
import com.pettrip.album.model.AlbumPhoto;
import com.pettrip.album.repository.AlbumPhotoRepository;
import com.pettrip.album.repository.AlbumRepository;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlbumService {

  /**
   * 앨범 사진을 전부 읽는다. 앨범/추억앨범 구분은 {@code p.is_die}로 함께 가져와 자바에서 가른다.
   *
   * <p>쿼리를 두 번 도는 대신 한 번에 읽는다. 한 사람의 앨범이라 규모가 크지 않다.
   */
  private static final String PHOTOS_SQL =
      """
      SELECT ap.photo_id, ph.photo_url, ph.taken_at,
             ap.pet_id, p.pet_name, p.is_die
      FROM album_photos ap
      JOIN photos ph ON ph.photo_id = ap.photo_id
      LEFT JOIN pets p ON p.pet_id = ap.pet_id
      WHERE ap.album_id = :albumId
      ORDER BY ap.photo_order, ap.created_at
      """;

  /** 앨범에 담으려는 사진이 본인 것인지 확인한다. 남의 사진을 붙이지 못하게 막는다. */
  private static final String OWNED_PHOTO_IDS_SQL =
      """
      SELECT photo_id FROM photos
      WHERE photo_id IN (:photoIds) AND user_id = :userId
      """;

  /** 반려동물이 본인 것인지 확인한다. */
  private static final String OWNED_PET_SQL =
      "SELECT EXISTS(SELECT 1 FROM pets WHERE pet_id = :petId AND user_id = :userId)";

  private record PhotoRow(AlbumPhotoItem item, boolean isDie) {}

  private static final RowMapper<PhotoRow> PHOTO_ROW_MAPPER =
      (rs, rowNum) -> {
        Date takenAt = rs.getDate("taken_at");
        AlbumPhotoItem item =
            new AlbumPhotoItem(
                rs.getObject("photo_id", UUID.class),
                rs.getString("photo_url"),
                rs.getObject("pet_id", UUID.class),
                rs.getString("pet_name"),
                takenAt == null ? null : takenAt.toLocalDate());
        return new PhotoRow(item, rs.getBoolean("is_die"));
      };

  private final AlbumRepository albumRepository;
  private final AlbumPhotoRepository albumPhotoRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public AlbumService(
      AlbumRepository albumRepository,
      AlbumPhotoRepository albumPhotoRepository,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.albumRepository = albumRepository;
    this.albumPhotoRepository = albumPhotoRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 유저의 앨범을 돌려준다. 사진은 그 사진 속 반려동물의 {@code is_die}로 두 묶음으로 갈린다.
   *
   * <p>{@code is_die=false} → {@code album}, {@code is_die=true} → {@code memorialAlbum}.
   */
  @Transactional
  public AlbumResponse getMyAlbum(UUID userId) {
    Album album = getOrCreateAlbum(userId);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("albumId", album.getId());
    List<PhotoRow> rows = jdbcTemplate.query(PHOTOS_SQL, params, PHOTO_ROW_MAPPER);

    List<AlbumPhotoItem> alive = new ArrayList<>();
    List<AlbumPhotoItem> memorial = new ArrayList<>();
    for (PhotoRow row : rows) {
      if (row.isDie()) {
        memorial.add(row.item());
        continue;
      }
      alive.add(row.item());
    }
    return new AlbumResponse(
        album.getId(),
        album.getAlbumName(),
        new AlbumGroupResponse(alive.size(), alive),
        new AlbumGroupResponse(memorial.size(), memorial));
  }

  @Transactional
  public void rename(UUID userId, String albumName) {
    Album album = getOrCreateAlbum(userId);
    album.rename(albumName);
    albumRepository.save(album);
  }

  /**
   * 앨범에 사진을 담는다. 사진마다 어느 아이의 것인지 지정해야 분류가 된다.
   *
   * <p>이미 담긴 사진은 건너뛴다. 같은 사진을 두 번 눌러도 중복으로 쌓이지 않는다.
   */
  @Transactional
  public void addPhotos(UUID userId, UUID petId, List<UUID> photoIds) {
    Album album = getOrCreateAlbum(userId);
    verifyPetOwnership(userId, petId);
    verifyPhotoOwnership(userId, photoIds);

    short order = (short) albumPhotoRepository.countByAlbumId(album.getId());
    for (UUID photoId : photoIds) {
      if (albumPhotoRepository.existsByAlbumIdAndPhotoId(album.getId(), photoId)) {
        continue;
      }
      albumPhotoRepository.save(new AlbumPhoto(album.getId(), photoId, petId, order));
      order++;
    }
  }

  /** 앨범에서 사진을 뺀다. {@code photos} 레코드와 S3 객체는 남는다. 다른 곳에서 같은 사진을 쓸 수 있다. */
  @Transactional
  public void removePhoto(UUID userId, UUID photoId) {
    Album album = getOrCreateAlbum(userId);
    if (!albumPhotoRepository.existsByAlbumIdAndPhotoId(album.getId(), photoId)) {
      throw new AlbumPhotoNotFoundException();
    }
    albumPhotoRepository.deleteByAlbumIdAndPhotoId(album.getId(), photoId);
  }

  /**
   * 앨범이 없으면 만들어 준다.
   *
   * <p>회원가입 시점에 만들지 않는 이유: 가입 로직은 module-user에 있고 이 서비스는 module-album에 있다. module-album이 이미
   * module-user를 의존하므로 반대로 호출하면 순환이 된다.
   */
  private Album getOrCreateAlbum(UUID userId) {
    return albumRepository
        .findByUserId(userId)
        .orElseGet(() -> albumRepository.save(new Album(userId, "내 앨범")));
  }

  private void verifyPetOwnership(UUID userId, UUID petId) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("petId", petId).addValue("userId", userId);
    Boolean owned = jdbcTemplate.queryForObject(OWNED_PET_SQL, params, Boolean.class);
    if (!Boolean.TRUE.equals(owned)) {
      throw new PetNotOwnedException();
    }
  }

  private void verifyPhotoOwnership(UUID userId, List<UUID> photoIds) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("photoIds", photoIds);
    List<UUID> owned = jdbcTemplate.queryForList(OWNED_PHOTO_IDS_SQL, params, UUID.class);
    if (owned.size() != photoIds.size()) {
      throw new PhotoNotOwnedException();
    }
  }
}
