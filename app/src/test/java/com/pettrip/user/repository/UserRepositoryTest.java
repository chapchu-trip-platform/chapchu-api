package com.pettrip.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Role;
import com.pettrip.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void 유저를_저장하고_조회한다() {
    User saved = userRepository.save(new User("test@example.com", "google-1"));

    User found = userRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getEmail()).isEqualTo("test@example.com");
    assertThat(found.getRole()).isEqualTo(Role.USER);
    assertThat(found.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
  }
}
