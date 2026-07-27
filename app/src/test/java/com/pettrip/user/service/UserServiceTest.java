package com.pettrip.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import com.pettrip.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RegionRepository regionRepository;
  @Mock private ThemeRepository themeRepository;
  @Mock private TransportMethodRepository transportMethodRepository;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(
            userRepository, regionRepository, themeRepository, transportMethodRepository);
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

  @Test
  void updatePreferences는_존재하지_않는_지역이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID regionId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(regionRepository.findAllById(List.of(regionId))).thenReturn(List.of());

    assertThatThrownBy(() -> userService.updatePreferences(userId, List.of(regionId), null, null))
        .isInstanceOf(RegionNotFoundException.class);
  }

  @Test
  void updatePreferences는_존재하는_지역_테마_이동수단으로_교체한다() {
    UUID userId = UUID.randomUUID();
    UUID regionId = UUID.randomUUID();
    UUID themeId = UUID.randomUUID();
    UUID transportMethodId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    Region region = new Region("서울");
    Theme theme = new Theme("카페");
    TransportMethod transportMethod = new TransportMethod("자가용");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(regionRepository.findAllById(List.of(regionId))).thenReturn(List.of(region));
    when(themeRepository.findAllById(List.of(themeId))).thenReturn(List.of(theme));
    when(transportMethodRepository.findAllById(List.of(transportMethodId)))
        .thenReturn(List.of(transportMethod));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result =
        userService.updatePreferences(
            userId, List.of(regionId), List.of(themeId), List.of(transportMethodId));

    assertThat(result.getPreferredRegions()).containsExactly(region);
    assertThat(result.getPreferredThemes()).containsExactly(theme);
    assertThat(result.getPreferredTransportMethods()).containsExactly(transportMethod);
  }

  @Test
  void updatePreferences는_null인_카테고리는_그대로_둔다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    Region existingRegion = new Region("부산");
    user.replacePreferredRegions(Set.of(existingRegion));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.updatePreferences(userId, null, null, null);

    assertThat(result.getPreferredRegions()).containsExactly(existingRegion);
  }
}
