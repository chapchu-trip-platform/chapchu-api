package com.pettrip.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Role;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.model.User;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;
  @Autowired private RegionRepository regionRepository;
  @Autowired private ThemeRepository themeRepository;
  @Autowired private TransportMethodRepository transportMethodRepository;

  @Test
  void 유저를_저장하고_조회한다() {
    User saved = userRepository.save(new User("test@example.com", "google-1"));

    User found = userRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getEmail()).isEqualTo("test@example.com");
    assertThat(found.getRole()).isEqualTo(Role.USER);
    assertThat(found.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void 유저의_선호_지역_테마_이동수단을_저장하고_조회한다() {
    Region region = regionRepository.save(new Region("서울"));
    Theme theme = themeRepository.save(new Theme("카페"));
    TransportMethod transportMethod = transportMethodRepository.save(new TransportMethod("자가용"));
    User user = new User("test2@example.com", "google-2");
    user.replacePreferredRegions(Set.of(region));
    user.replacePreferredThemes(Set.of(theme));
    user.replacePreferredTransportMethods(Set.of(transportMethod));
    User saved = userRepository.save(user);

    User found = userRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getPreferredRegions()).extracting(Region::getRegionName).containsExactly("서울");
    assertThat(found.getPreferredThemes()).extracting(Theme::getThemeName).containsExactly("카페");
    assertThat(found.getPreferredTransportMethods())
        .extracting(TransportMethod::getTransportMethodName)
        .containsExactly("자가용");
  }
}
