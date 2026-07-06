package com.pettrip.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository);
  }

  @Test
  void getMe는_유저가_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getMe(userId)).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void registerNickname은_이미_등록되어_있으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    user.registerNickname("기존닉네임");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> userService.registerNickname(userId, "새닉네임"))
        .isInstanceOf(NicknameAlreadyRegisteredException.class);
  }

  @Test
  void registerNickname은_닉네임이_없으면_등록한다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.registerNickname(userId, "초코사랑");

    assertThat(result.getNickname()).isEqualTo("초코사랑");
  }

  @Test
  void updateMe는_탈퇴_상태로_변경할_수_있다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.updateMe(userId, null, AccountStatus.WITHDRAWN);

    assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
  }
}
