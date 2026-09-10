package com.pettrip.stamp.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** 시·도 하나에 스탬프 하나. 마이그레이션 V30에서 지역 수만큼 미리 만들어 둔다. */
@Entity
@Table(name = "stamps")
@AttributeOverride(name = "id", column = @Column(name = "stamp_id"))
public class Stamp extends BaseEntity {

  @Column(name = "stamp_name", nullable = false, length = 30)
  private String stampName;

  @Column(name = "region_id")
  private UUID regionId;

  /** 지자체 마스코트 이미지. 준비되는 대로 채운다. */
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  protected Stamp() {}

  public String getStampName() {
    return stampName;
  }

  public UUID getRegionId() {
    return regionId;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
