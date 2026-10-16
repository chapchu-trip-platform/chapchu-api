package com.pettrip.user.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 여행 테마. TourAPI의 대분류({@code contentTypeId})와 1:1로 맞춰 둔다.
 *
 * <p>유저 선호와 장소 분류가 같은 축을 쓰게 하기 위해서다. {@code places.theme_id}를 채울 때 TourAPI가 준 {@code
 * contentTypeId}로 이 테이블을 찾으면 된다.
 */
@Entity
@Table(name = "themes")
@AttributeOverride(name = "id", column = @Column(name = "theme_id"))
public class Theme extends BaseEntity {

  @Column(name = "theme_name", nullable = false, length = 50, unique = true)
  private String themeName;

  /** TourAPI {@code contentTypeId}. 12 관광지, 14 문화시설, 32 숙박 … */
  @Column(name = "content_type_id", nullable = false, unique = true)
  private Integer contentTypeId;

  protected Theme() {}

  public Theme(String themeName, Integer contentTypeId) {
    this.themeName = themeName;
    this.contentTypeId = contentTypeId;
  }

  public String getThemeName() {
    return themeName;
  }

  public Integer getContentTypeId() {
    return contentTypeId;
  }
}
