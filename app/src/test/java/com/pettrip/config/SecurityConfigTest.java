package com.pettrip.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.pet.controller.PetController;
import com.pettrip.pet.service.PetService;
import java.util.List;
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

    mockMvc.perform(get("/pets").with(jwt())).andExpect(status().isOk());
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
}
