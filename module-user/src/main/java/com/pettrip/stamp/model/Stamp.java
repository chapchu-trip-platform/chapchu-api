package com.pettrip.stamp.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 도 하나에 스탬프 하나. 9개 도를 V34에서 미리 만들어 둔다.
 *
 * <p>지역 판정은 {@code stamp_area_codes}(TourAPI areaCode → 스탬프)로 한다. 스탬프 자체는 지역을 참조하지 않는다.
 */
@Entity
@Table(name = "stamps")
@AttributeOverride(name = "id", column = @Column(name = "stamp_id"))
public class Stamp extends BaseEntity {

  @Column(name = "stamp_name", nullable = false, length = 30)
  private String stampName;

  /** 지자체 마스코트 이미지. 준비되는 대로 채운다. */
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  protected Stamp() {}

  public String getStampName() {
    return stampName;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
