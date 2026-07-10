package com.pettrip.pet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PetRepositoryTest {

  @Autowired private PetRepository petRepository;
  @Autowired private BreedRepository breedRepository;

  @Test
  void findByUserId는_해당_유저의_반려견만_조회한다() {
    Breed breed = breedRepository.save(new Breed("골든리트리버"));
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    petRepository.save(new Pet(ownerId, breed, "초코", PetSize.MEDIUM, 3));
    petRepository.save(new Pet(otherId, breed, "루이", PetSize.SMALL, 2));

    var result = petRepository.findByUserId(ownerId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPetName()).isEqualTo("초코");
  }

  @Test
  void countByUserId는_해당_유저의_반려견_수를_센다() {
    Breed breed = breedRepository.save(new Breed("말티즈"));
    UUID ownerId = UUID.randomUUID();
    petRepository.save(new Pet(ownerId, breed, "루이", PetSize.SMALL, 2));
    petRepository.save(new Pet(ownerId, breed, "초코", PetSize.MEDIUM, 3));

    long count = petRepository.countByUserId(ownerId);

    assertThat(count).isEqualTo(2);
  }
}
