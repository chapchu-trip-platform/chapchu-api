package com.pettrip.wishlist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.wishlist.model.Wishlist;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class WishlistRepositoryTest {

  @Autowired private WishlistRepository wishlistRepository;

  @Test
  void findByUserId는_해당_유저의_위시리스트만_조회한다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    wishlistRepository.save(new Wishlist(ownerId, "place-a"));
    wishlistRepository.save(new Wishlist(ownerId, "place-b"));
    wishlistRepository.save(new Wishlist(otherId, "place-c"));

    List<Wishlist> result = wishlistRepository.findByUserIdOrderByCreatedAtDesc(ownerId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(w -> w.getUserId().equals(ownerId));
  }

  @Test
  void existsByUserIdAndPlaceId는_존재_여부를_반환한다() {
    UUID userId = UUID.randomUUID();
    wishlistRepository.save(new Wishlist(userId, "place-a"));

    assertThat(wishlistRepository.existsByUserIdAndPlaceId(userId, "place-a")).isTrue();
    assertThat(wishlistRepository.existsByUserIdAndPlaceId(userId, "place-z")).isFalse();
  }

  @Test
  void deleteByUserIdAndPlaceId는_해당_항목을_제거한다() {
    UUID userId = UUID.randomUUID();
    wishlistRepository.save(new Wishlist(userId, "place-a"));

    wishlistRepository.deleteByUserIdAndPlaceId(userId, "place-a");

    assertThat(wishlistRepository.existsByUserIdAndPlaceId(userId, "place-a")).isFalse();
  }
}
