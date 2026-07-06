package com.pettrip.harness;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.pettrip", importOptions = ImportOption.DoNotIncludeTests.class)
public class AgentHarnessArchitectureTest {

  @ArchTest
  static final ArchRule layer_dependencies_are_respected =
      layeredArchitecture()
          .consideringAllDependencies()
          .withOptionalLayers(true)
          .layer("Controller")
          .definedBy("..controller..")
          .layer("Service")
          .definedBy("..service..")
          .layer("Repository")
          .definedBy("..repository..")
          .layer("Model")
          .definedBy("..model..")
          .whereLayer("Controller")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Service")
          .mayOnlyBeAccessedByLayers("Controller")
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Service")
          .whereLayer("Model")
          .mayOnlyBeAccessedByLayers("Controller", "Service", "Repository")
          .as("X [센서 감지] 계층형 아키텍처 위반!")
          .because("상위 계층은 하위 계층을 건너뛰어 호출할 수 없으며, 하위 계층은 상위 계층을 알 수 없습니다.");

  @ArchTest
  static final ArchRule no_cycles_between_domains =
      slices()
          .matching("com.pettrip.(*)..") // 도메인별 슬라이스 (user, place, trip, ...)
          .should()
          .beFreeOfCycles()
          .allowEmptyShould(true)
          .as("X [센서 감지] 도메인 간 순환 참조 발생!")
          .because("도메인 모듈 간에 서로를 참조하는 스파게티 의존성이 감지되었습니다.");

  @ArchTest
  static final ArchRule model_should_not_depend_on_outside =
      noClasses()
          .that()
          .resideInAPackage("..model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "..service..", "..repository..")
          .allowEmptyShould(true)
          .as("X [센서 감지] 도메인 모델 오염!")
          .because("핵심 비즈니스 모델(Entity) 내부에 외부 인프라나 API 요청 객체가 섞여 있습니다.");
}
