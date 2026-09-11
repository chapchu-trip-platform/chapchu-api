package com.pettrip.album.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * 유저당 앨범 하나.
 *
 * <p>앨범인지 추억앨범인지는 여기에 두지 않는다. 사진마다 달린 반려동물의 {@code pets.is_die}로 불러올 때 갈린다. 타입 컬럼을 따로 두면 "추억앨범인데 그
 * 아이는 살아있음" 같은 어긋난 상태를 막을 방법이 없다.
 */
@Entity
@Table(name = "albums")
@AttributeOverride(name = "id", column = @Column(name = "album_id"))
public class Album extends BaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "album_name", length = 30)
  private String albumName;

  protected Album() {}

  public Album(UUID userId, String albumName) {
    this.userId = userId;
    this.albumName = albumName;
  }

  public void rename(String newName) {
    if (newName == null) {
      return;
    }
    this.albumName = newName;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getAlbumName() {
    return albumName;
  }
}
