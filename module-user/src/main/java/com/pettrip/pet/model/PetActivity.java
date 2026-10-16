package com.pettrip.pet.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 반려견 활동 유형 코드 (산책, 수영, 등산 …). 유저와 무관한 공용 코드값이다. */
@Entity
@Table(name = "pet_activities")
@AttributeOverride(name = "id", column = @Column(name = "activity_id"))
public class PetActivity extends BaseEntity {

  @Column(name = "activity_name", nullable = false, length = 30, unique = true)
  private String activityName;

  protected PetActivity() {}

  public PetActivity(String activityName) {
    this.activityName = activityName;
  }

  public String getActivityName() {
    return activityName;
  }
}
