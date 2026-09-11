package com.pettrip.album.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * 앨범에 담긴 사진 1장.
 *
 * <p>{@code petId}가 분류의 열쇠다. 이 아이의 {@code is_die}가 앨범/추억앨범을 가른다.
 */
@Entity
@Table(name = "album_photos")
@AttributeOverride(name = "id", column = @Column(name = "album_photo_id"))
public class AlbumPhoto extends BaseEntity {

  @Column(name = "album_id", nullable = false)
  private UUID albumId;

  @Column(name = "photo_id", nullable = false)
  private UUID photoId;

  @Column(name = "pet_id")
  private UUID petId;

  @Column(name = "photo_order", nullable = false)
  private short photoOrder;

  protected AlbumPhoto() {}

  public AlbumPhoto(UUID albumId, UUID photoId, UUID petId, short photoOrder) {
    this.albumId = albumId;
    this.photoId = photoId;
    this.petId = petId;
    this.photoOrder = photoOrder;
  }

  public UUID getAlbumId() {
    return albumId;
  }

  public UUID getPhotoId() {
    return photoId;
  }

  public UUID getPetId() {
    return petId;
  }
}
