package com.pettrip.config;

import com.pettrip.user.service.UserService;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * docs/decisions/026 참고: {@code @CurrentUserId} 파라미터 리졸버를 등록한다.
 *
 * <p>리졸버가 계정 상태까지 확인하므로(docs/decisions/049) {@code UserService}가 필요하다. 그래서 {@code @WebMvcTest} 슬라이스
 * 테스트는 {@code UserService} 목과 {@code isActive} 스텁이 있어야 한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final UserService userService;

  public WebMvcConfig(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new CurrentUserIdArgumentResolver(userService));
  }
}
