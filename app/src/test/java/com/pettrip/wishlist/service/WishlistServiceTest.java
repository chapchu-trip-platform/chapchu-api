package com.pettrip.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceNotFoundException;
import com.pettrip.wishlist.controller.WishlistResponse;
import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

  @Mock private WishlistRepository wishlistRepository;
  @Mock private PlaceRepository placeRepository;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private WishlistService wishlistService;

  @BeforeEach
  void setUp() {
    wishlistService = new WishlistService(wishlistRepository, placeRepository, jdbcTemplate);
  }

  private WishlistResponse sampleResponse() {
    return new WishlistResponse(
        "126508",
        "안목해변",
        "https://example.com/a.jpg",
        "강원 강릉시 창해로14번길 20",
        new BigDecimal("37.7735000"),
        new BigDecimal("128.9470000"),
        (short) 5,
        128,
        LocalDateTime.of(2026, 9, 1, 10, 0, 0));
  }

  @Test
  void listMyWishlist는_장소_정보를_붙여_돌려준다() {
    UUID userId = UUID.randomUUID();
    WishlistResponse expected = sampleResponse();
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    List<WishlistResponse> result = wishlistService.listMyWishlist(userId);

    assertThat(result).containsExactly(expected);
  }

  @Test
  void addToWishlist는_없는_장소면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(placeRepository.existsById("nope")).thenReturn(false);

    assertThatThrownBy(() -> wishlistService.addToWishlist(userId, "nope"))
        .isInstanceOf(PlaceNotFoundException.class);

    verify(wishlistRepository, never()).save(any());
  }

  @Test
  void addToWishlist는_이미_찜한_장소면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(placeRepository.existsById("126508")).thenReturn(true);
    when(wishlistRepository.existsByUserIdAndPlaceId(userId, "126508")).thenReturn(true);

    assertThatThrownBy(() -> wishlistService.addToWishlist(userId, "126508"))
        .isInstanceOf(PlaceAlreadyWishlistedException.class);

    verify(wishlistRepository, never()).save(any());
  }

  @Test
  void addToWishlist는_찜을_저장한다() {
    UUID userId = UUID.randomUUID();
    when(placeRepository.existsById("126508")).thenReturn(true);
    when(wishlistRepository.existsByUserIdAndPlaceId(userId, "126508")).thenReturn(false);

    wishlistService.addToWishlist(userId, "126508");

    verify(wishlistRepository).save(any(Wishlist.class));
  }

  @Test
  void 존재하는_항목을_제거한다() {
    UUID userId = UUID.randomUUID();
    when(wishlistRepository.existsByUserIdAndPlaceId(userId, "126508")).thenReturn(true);

    wishlistService.removeFromWishlist(userId, "126508");

    verify(wishlistRepository).deleteByUserIdAndPlaceId(userId, "126508");
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
