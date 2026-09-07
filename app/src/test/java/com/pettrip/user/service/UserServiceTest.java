package com.pettrip.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pettrip.photo.model.Photo;
import com.pettrip.photo.repository.PhotoRepository;
import com.pettrip.photo.service.PhotoNotFoundException;
import com.pettrip.photo.service.PhotoService;
import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import com.pettrip.user.repository.UserRepository;
import java.net.URI;
import java.time.LocalDate;
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

  private static final String DEFAULT_IMG = "https://cdn.example.com/default-profile.png";

  @Mock private UserRepository userRepository;
  @Mock private RegionRepository regionRepository;
  @Mock private ThemeRepository themeRepository;
  @Mock private TransportMethodRepository transportMethodRepository;
  @Mock private PhotoRepository photoRepository;
  @Mock private PhotoService photoService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(
            userRepository,
            regionRepository,
            themeRepository,
            transportMethodRepository,
            photoRepository,
            photoService,
            DEFAULT_IMG);
  }

  @Test
  void getMe는_유저가_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getMe(userId)).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void getMe는_프사가_없으면_기본이미지_URL을_내려준다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    MeDetail result = userService.getMe(userId);

    assertThat(result.profilePhoto().photoId()).isNull();
    assertThat(result.profilePhoto().downloadUrl()).isEqualTo(DEFAULT_IMG);
  }

  @Test
  void getMe는_프사가_있으면_presigned_URL을_내려준다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    user.updateProfilePhoto(photoId);
    Photo photo = new Photo(userId, null, "profile/u/1.jpg", LocalDate.of(2026, 7, 1));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
    when(photoService.issueDownloadUrl("profile/u/1.jpg"))
        .thenReturn(URI.create("https://bucket/profile/u/1.jpg?sig=x").toURL());

    MeDetail result = userService.getMe(userId);

    assertThat(result.profilePhoto().photoId()).isEqualTo(photo.getId());
    assertThat(result.profilePhoto().downloadUrl()).contains("sig=x");
  }

  @Test
  void updateProfilePhoto는_소유_사진이면_설정한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    Photo photo = new Photo(userId, null, "profile/u/1.jpg", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(photoService.getOwnedPhoto(userId, photoId)).thenReturn(photo);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
    when(photoService.issueDownloadUrl("profile/u/1.jpg"))
        .thenReturn(URI.create("https://bucket/profile/u/1.jpg?sig=x").toURL());

    MeDetail result = userService.updateProfilePhoto(userId, photoId);

    assertThat(user.getProfilePhotoId()).isEqualTo(photoId);
    assertThat(result.profilePhoto().photoId()).isEqualTo(photo.getId());
  }

  @Test
  void updateProfilePhoto는_타인_사진이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID foreignPhoto = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(photoService.getOwnedPhoto(userId, foreignPhoto)).thenThrow(new PhotoNotFoundException());

    assertThatThrownBy(() -> userService.updateProfilePhoto(userId, foreignPhoto))
        .isInstanceOf(PhotoNotFoundException.class);
  }

  @Test
  void updateProfilePhoto는_null이면_프사를_지우고_기본이미지로_되돌린다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    user.updateProfilePhoto(UUID.randomUUID());
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    MeDetail result = userService.updateProfilePhoto(userId, null);

    assertThat(user.getProfilePhotoId()).isNull();
    assertThat(result.profilePhoto().downloadUrl()).isEqualTo(DEFAULT_IMG);
  }

  @Test
  void updateMe는_탈퇴_상태로_변경할_수_있다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    MeDetail result = userService.updateMe(userId, null, AccountStatus.WITHDRAWN);

    assertThat(result.user().getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
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
    Theme theme = new Theme("관광지", 12);
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

  @Test
  void isNicknameAvailable은_사용_중이면_false를_반환한다() {
    when(userRepository.existsByNickname("초롱이")).thenReturn(true);

    assertThat(userService.isNicknameAvailable("초롱이")).isFalse();
  }

  @Test
  void isNicknameAvailable은_사용_중이_아니면_true를_반환한다() {
    when(userRepository.existsByNickname("초롱이")).thenReturn(false);

    assertThat(userService.isNicknameAvailable("초롱이")).isTrue();
  }

  @Test
  void updateMe는_이미_사용_중인_닉네임이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.existsByNickname("초롱이")).thenReturn(true);

    assertThatThrownBy(() -> userService.updateMe(userId, "초롱이", null))
        .isInstanceOf(NicknameAlreadyInUseException.class);
  }

  @Test
  void updateMe는_다른_유저가_쓰는_닉네임이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    user.registerNickname("내닉네임");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.existsByNickname("남의닉네임")).thenReturn(true);

    assertThatThrownBy(() -> userService.updateMe(userId, "남의닉네임", null))
        .isInstanceOf(NicknameAlreadyInUseException.class);
  }

  @Test
  void updateMe는_자기_닉네임을_그대로_보내면_예외를_던지지_않는다() {
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "google-1");
    user.registerNickname("내닉네임");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    MeDetail result = userService.updateMe(userId, "내닉네임", null);

    assertThat(result.user().getNickname()).isEqualTo("내닉네임");
  }
}
