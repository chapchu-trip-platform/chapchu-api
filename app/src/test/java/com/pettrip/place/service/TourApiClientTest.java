package com.pettrip.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pettrip.common.service.ExternalApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * 외부 연동 실패가 500 raw로 새지 않는지 검증한다.
 *
 * <p>운영에서 {@code TOUR_API_KEY}가 주입되지 않아 {@code GET /places/nearby}가 Spring 기본 형식의 500을 반환하고 있었다.
 * 무엇이 빠졌는지 응답만 봐서는 알 수 없었다.
 */
class TourApiClientTest {

  private static final BigDecimal LAT = new BigDecimal("37.5665");
  private static final BigDecimal LNG = new BigDecimal("126.9780");

  private TourApiClient clientWithKey(String serviceKey) {
    return new TourApiClient(RestClient.builder(), "https://tour-api.invalid", serviceKey);
  }

  @Test
  void 서비스_키가_없으면_무엇이_빠졌는지_알려준다() {
    TourApiClient client = clientWithKey("");

    assertThatThrownBy(() -> client.fetchNearby(LAT, LNG, 1000))
        .isInstanceOf(ExternalApiException.class)
        .hasMessageContaining("TOUR_API_KEY");
  }

  @Test
  void 서비스_키가_null이어도_같은_예외를_던진다() {
    TourApiClient client = clientWithKey(null);

    assertThatThrownBy(() -> client.fetchPetDetail("123"))
        .isInstanceOf(ExternalApiException.class)
        .hasMessageContaining("TOUR_API_KEY");
  }

  /** 키가 있어도 외부 서버는 언제든 죽는다. 그 실패도 그대로 새어나가면 안 된다. */
  @Test
  void 외부_호출_자체가_실패하면_ExternalApiException으로_감싼다() {
    TourApiClient client = clientWithKey("valid-looking-key");

    assertThatThrownBy(() -> client.fetchNearby(LAT, LNG, 1000))
        .isInstanceOf(ExternalApiException.class)
        .hasMessageContaining("TourAPI 호출에 실패했습니다.")
        .cause()
        .isNotNull();
  }

  @Test
  void 원인_예외를_잃지_않는다() {
    TourApiClient client = clientWithKey("valid-looking-key");

    ExternalApiException thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            ExternalApiException.class, () -> client.fetchNearby(LAT, LNG, 1000));

    assertThat(thrown.getCause()).isNotNull();
  }
}
