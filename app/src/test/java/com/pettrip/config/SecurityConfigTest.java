package com.pettrip.config;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.pet.controller.PetController;
import com.pettrip.pet.service.PetService;
import jakarta.servlet.DispatcherType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** docs/decisions/025 참고: 공개/인증 필요 엔드포인트 분리와 JWT 검증 적용을 검증한다. */
@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 인증이_필요한_엔드포인트는_토큰이_없으면_401을_반환한다() throws Exception {
    mockMvc.perform(get("/pets")).andExpect(status().isUnauthorized());
  }

  @Test
  void 인증이_필요한_엔드포인트는_유효한_토큰이_있으면_접근할_수_있다() throws Exception {
    when(petService.listPets(any())).thenReturn(List.of());

    mockMvc
        .perform(get("/pets").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk());
  }

  /**
   * 공개 엔드포인트를 담당하는 컨트롤러(place/weather)와 actuator 는 이 @WebMvcTest 슬라이스에 포함되지 않아 404가 반환된다. 401이 아니라
   * 404라는 점이 인증 필터에 막히지 않았음(permitAll)을 증명한다.
   */
  @Test
  void 공개_엔드포인트는_토큰이_없어도_인증에_막히지_않는다() throws Exception {
    mockMvc.perform(get("/places/test-place-id")).andExpect(status().isNotFound());
    mockMvc.perform(get("/weather")).andExpect(status().isNotFound());
    mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
  }

  @Test
  void 공개_대상이_아닌_메서드는_공개_경로여도_인증을_요구한다() throws Exception {
    mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
  }

  /**
   * docs/failures/022 참고: 404가 발생하면 서블릿 컨테이너가 {@code /error}로 forward 하는데, 이 ERROR 디스패치도 시큐리티 필터를
   * 다시 탄다. permitAll 해두지 않으면 {@code anyRequest().authenticated()}에 걸려 모든 404가 401로 둔갑한다.
   *
   * <p>위의 {@code 공개_엔드포인트는_...} 테스트가 404를 확인하며 통과했는데도 운영에서는 401이 나왔다. MockMvc는 실제 컨테이너와 달리 ERROR
   * 디스패치를 일으키지 않아 이 버그를 재현하지 못하기 때문이다. 그래서 여기서는 디스패치 타입을 직접 지정해 검증한다.
   */
  @Test
  void ERROR_디스패치는_인증을_요구하지_않는다() throws Exception {
    mockMvc
        .perform(
            get("/error")
                .with(
                    request -> {
                      request.setDispatcherType(DispatcherType.ERROR);
                      return request;
                    }))
        .andExpect(status().is(not(401)));
  }

  @Test
  void ERROR_디스패치가_아닌_일반_요청은_여전히_인증을_요구한다() throws Exception {
    mockMvc.perform(get("/error")).andExpect(status().isUnauthorized());
  }
}
