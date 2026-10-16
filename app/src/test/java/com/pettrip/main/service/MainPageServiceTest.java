package com.pettrip.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MainPageServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PetRepository petRepository;

  private MainPageService mainPageService;

  @BeforeEach
  void setUp() {
    mainPageService = new MainPageService(userRepository, petRepository);
  }

  @Test
  void getMainPage는_유저가_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mainPageService.getMainPage(userId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void getMainPage는_닉네임과_반려견_이름_목록을_반환한다() {
    UUID userId = UUID.randomUUID();
    User user = new User("chorong@example.com", "google-1");
    user.registerNickname("초롱");
    Pet pet = new Pet(userId, new Breed("말티즈"), "루이", PetSize.SMALL, 2);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(petRepository.findByUserId(userId)).thenReturn(List.of(pet));

    MainPageSummary summary = mainPageService.getMainPage(userId);

    assertThat(summary.nickname()).isEqualTo("초롱");
    assertThat(summary.petNames()).containsExactly("루이");
  }
}
