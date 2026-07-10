package com.pettrip.wishlist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "place_wishlists")
@IdClass(WishlistId.class)
@EntityListeners(AuditingEntityListener.class)
public class Wishlist {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Id
  @Column(name = "place_id")
  private String placeId;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected Wishlist() {}

  public Wishlist(UUID userId, String placeId) {
    this.userId = userId;
    this.placeId = placeId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getPlaceId() {
    return placeId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
