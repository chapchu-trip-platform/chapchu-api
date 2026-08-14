package com.pettrip.pet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 견종 코드.
 *
 * <p>다른 엔티티와 달리 {@code BaseEntity}를 상속하지 않고 {@code Integer} PK를 직접 선언한다. {@code V7}이 {@code
 * breeds.breed_id}를 {@code INT GENERATED ALWAYS AS IDENTITY}로 바꿨는데, {@code BaseEntity}의 PK는 애플리케이션이
 * 만드는 UUID라 타입이 맞지 않는다. 이 불일치 때문에 {@code ddl-auto: validate}에서 기동이 실패했다.
 *
 * <p>DB가 값을 생성하므로 {@link GenerationType#IDENTITY}가 필요하다. 프로젝트 규칙은 {@code @GeneratedValue}를 금지하지만 이
 * 테이블만은 스키마가 그렇게 정해져 있어 예외다. <b>새 엔티티를 만들 때 이 클래스를 본보기로 삼지 마라.</b>
 */
@Entity
@Table(name = "breeds")
@EntityListeners(AuditingEntityListener.class)
public class Breed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "breed_id")
  private Integer id;

  @Column(name = "breed_name", nullable = false, length = 30, unique = true)
  private String breedName;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate private LocalDateTime updatedAt;

  protected Breed() {}

  public Breed(String breedName) {
    this.breedName = breedName;
  }

  public Integer getId() {
    return id;
  }

  public String getBreedName() {
    return breedName;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
