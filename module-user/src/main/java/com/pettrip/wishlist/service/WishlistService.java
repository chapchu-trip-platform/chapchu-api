package com.pettrip.wishlist.service;

import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceNotFoundException;
import com.pettrip.wishlist.controller.WishlistResponse;
import com.pettrip.wishlist.model.Wishlist;
import com.pettrip.wishlist.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

  /**
   * 찜 목록에 장소 정보를 붙여 한 번에 가져온다.
   *
   * <p>{@code place_id}는 {@code places.external_place_id}를 가리키는 FK라 항상 대응하는 장소가 있다. 그래서 INNER JOIN
   * 이어도 행이 사라지지 않는다.
   */
  private static final String LIST_SQL =
      """
      SELECT pw.place_id, pw.created_at,
             p.place_name, p.place_image_url, p.address,
             p.latitude, p.longitude, p.rating, p.review_num
      FROM place_wishlists pw
      JOIN places p ON pw.place_id = p.external_place_id
      WHERE pw.user_id = :userId
      ORDER BY pw.created_at DESC
      """;

  private static final RowMapper<WishlistResponse> ROW_MAPPER =
      (rs, rowNum) ->
          new WishlistResponse(
              rs.getString("place_id"),
              rs.getString("place_name"),
              rs.getString("place_image_url"),
              rs.getString("address"),
              rs.getBigDecimal("latitude"),
              rs.getBigDecimal("longitude"),
              (Short) rs.getObject("rating"),
              (Integer) rs.getObject("review_num"),
              rs.getTimestamp("created_at").toLocalDateTime());

  private final WishlistRepository wishlistRepository;
  private final PlaceRepository placeRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public WishlistService(
      WishlistRepository wishlistRepository,
      PlaceRepository placeRepository,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.wishlistRepository = wishlistRepository;
    this.placeRepository = placeRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<WishlistResponse> listMyWishlist(UUID userId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return jdbcTemplate.query(LIST_SQL, params, ROW_MAPPER);
  }

  /**
   * 장소를 찜한다.
   *
   * <p>{@code place_wishlists.place_id}가 {@code places}를 참조하는 FK라, 아직 동기화되지 않은 장소를 찜하면 제약 위반이 난다.
   * 어느 값이 문제인지 알 수 있게 먼저 확인하고 404를 돌려준다.
   */
  @Transactional
  public void addToWishlist(UUID userId, String placeId) {
    if (!placeRepository.existsById(placeId)) {
      throw new PlaceNotFoundException();
    }
    if (wishlistRepository.existsByUserIdAndPlaceId(userId, placeId)) {
      throw new PlaceAlreadyWishlistedException();
    }
    wishlistRepository.save(new Wishlist(userId, placeId));
  }

  @Transactional
  public void removeFromWishlist(UUID userId, String placeId) {
    if (!wishlistRepository.existsByUserIdAndPlaceId(userId, placeId)) {
      throw new WishlistItemNotFoundException();
    }
    wishlistRepository.deleteByUserIdAndPlaceId(userId, placeId);
  }
}
