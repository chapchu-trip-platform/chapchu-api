package com.pettrip.user.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "themes")
@AttributeOverride(name = "id", column = @Column(name = "theme_id"))
public class Theme extends BaseEntity {

  @Column(name = "theme_name", nullable = false, length = 50)
  private String themeName;

  protected Theme() {}

  public Theme(String themeName) {
    this.themeName = themeName;
  }

  public String getThemeName() {
    return themeName;
  }
}
