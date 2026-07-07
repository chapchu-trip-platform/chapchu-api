package com.pettrip.user.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "regions")
@AttributeOverride(name = "id", column = @Column(name = "region_id"))
public class Region extends BaseEntity {

  @Column(name = "region_name", nullable = false, length = 100)
  private String regionName;

  protected Region() {}

  public Region(String regionName) {
    this.regionName = regionName;
  }

  public String getRegionName() {
    return regionName;
  }
}
