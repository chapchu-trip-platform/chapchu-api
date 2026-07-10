package com.pettrip.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

  @Mock private WishlistRepository wishlistRepository;

  @InjectMocks private WishlistService wishlistService;

  @Test
  void 위시리스트_목록을_최신순으로_조회한다() {
    UUID userId = UUID.randomUUID();
    when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(new Wishlist(userId, "place-1")));

    List<Wishlist> result = wishlistService.listMyWishlist(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlaceId()).isEqualTo("place-1");
  }

  @Test
  void 존재하는_항목을_제거한다() {
    UUID userId = UUID.randomUUID();
    when(wishlistRepository.existsByUserIdAndPlaceId(userId, "place-1")).thenReturn(true);

    wishlistService.removeFromWishlist(userId, "place-1");

    verify(wishlistRepository).deleteByUserIdAndPlaceId(userId, "place-1");
  }

  @Test
  void 존재하지_않는_항목_제거시_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(wishlistRepository.existsByUserIdAndPlaceId(userId, "nope")).thenReturn(false);

    assertThatThrownBy(() -> wishlistService.removeFromWishlist(userId, "nope"))
        .isInstanceOf(WishlistItemNotFoundException.class);

    verify(wishlistRepository, never()).deleteByUserIdAndPlaceId(eq(userId), eq("nope"));
  }
}
