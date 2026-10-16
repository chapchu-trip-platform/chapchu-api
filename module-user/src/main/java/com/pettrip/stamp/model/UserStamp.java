package com.pettrip.stamp.model;

import com.pettrip.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/** 유저가 가진 스탬프 하나. 같은 지역을 다시 방문하면 행을 늘리지 않고 {@code stampCount}만 올린다. */
@Entity
@Table(name = "user_stamps")
@AttributeOverride(name = "id", column = @Column(name = "user_stamp_id"))
public class UserStamp extends BaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "stamp_id", nullable = false)
  private UUID stampId;

  @Column(name = "stamp_count")
  private int stampCount;

  /** 처음 획득한 시각. 재방문해도 바뀌지 않는다. */
  @Column(name = "first_acquired_at")
  private LocalDateTime firstAcquiredAt;

  protected UserStamp() {}

  public UserStamp(UUID userId, UUID stampId, LocalDateTime acquiredAt) {
    this.userId = userId;
    this.stampId = stampId;
    this.stampCount = 1;
    this.firstAcquiredAt = acquiredAt;
  }

  public void increaseCount() {
    this.stampCount++;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getStampId() {
    return stampId;
  }

  public int getStampCount() {
    return stampCount;
  }

  public LocalDateTime getFirstAcquiredAt() {
    return firstAcquiredAt;
  }
}
