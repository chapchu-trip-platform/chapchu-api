package com.pettrip.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.DataIntegrityViolationException;

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

  @Test
  void existsByNickname은_사용_중인_닉네임을_찾는다() {
    User user = new User("nick@example.com", "google-nick");
    user.registerNickname("초롱이");
    userRepository.save(user);

    assertThat(userRepository.existsByNickname("초롱이")).isTrue();
    assertThat(userRepository.existsByNickname("없는닉네임")).isFalse();
  }

  /** docs/decisions/027 참고: 애플리케이션 검사와 별개로 DB UNIQUE 제약이 최종 방어선이다. */
  @Test
  void 같은_닉네임을_두_유저가_가지면_DB_제약에_걸린다() {
    User first = new User("a@example.com", "google-a");
    first.registerNickname("중복닉");
    userRepository.saveAndFlush(first);

    User second = new User("b@example.com", "google-b");
    second.registerNickname("중복닉");

    assertThatThrownBy(() -> userRepository.saveAndFlush(second))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 닉네임이_없는_유저는_여럿_존재할_수_있다() {
    userRepository.saveAndFlush(new User("c@example.com", "google-c"));
    userRepository.saveAndFlush(new User("d@example.com", "google-d"));

    assertThat(userRepository.count()).isGreaterThanOrEqualTo(2);
  }
}
