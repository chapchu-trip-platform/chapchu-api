package com.pettrip.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.UserNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PetRepository petRepository;

  private MyPageService myPageService;

  @BeforeEach
  void setUp() {
    myPageService = new MyPageService(userRepository, petRepository);
  }

  @Test
  void getSummary는_유저가_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> myPageService.getSummary(userId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void getSummary는_닉네임_이메일_반려견수를_합쳐_반환한다() {
    UUID userId = UUID.randomUUID();
    User user = new User("chorong@example.com", "google-1");
    user.registerNickname("초롱");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(petRepository.countByUserId(userId)).thenReturn(2L);

    MyPageSummary summary = myPageService.getSummary(userId);

    assertThat(summary.nickname()).isEqualTo("초롱");
    assertThat(summary.email()).isEqualTo("chorong@example.com");
    assertThat(summary.petCount()).isEqualTo(2L);
  }
}
