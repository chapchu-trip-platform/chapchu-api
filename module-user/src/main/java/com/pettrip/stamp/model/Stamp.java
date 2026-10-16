package com.pettrip.stamp.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 시·도 하나에 스탬프 하나. 17개 광역자치단체를 V34·V36에서 미리 만들어 둔다.
 *
 * <p>지역 판정은 {@code stamp_area_codes}(TourAPI areaCode → 스탬프)로 한다. 스탬프 자체는 지역을 참조하지 않는다.
 *
 * <p>이미지는 프론트 저장소에서 관리한다. 서버는 {@code stampName}만 준다.
 */
@Entity
@Table(name = "stamps")
@AttributeOverride(name = "id", column = @Column(name = "stamp_id"))
public class Stamp extends BaseEntity {

  @Column(name = "stamp_name", nullable = false, length = 30)
  private String stampName;

  protected Stamp() {}

  public String getStampName() {
    return stampName;
  }
}
