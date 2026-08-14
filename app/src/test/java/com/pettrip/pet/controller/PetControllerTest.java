package com.pettrip.pet.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.service.PetService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PetControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PetService petService;
  @MockitoBean private JwtDecoder jwtDecoder;

  /**
   * docs/decisions/026 참고: 고정 상수(TempAuthContext) 제거 후, JWT {@code sub} 클레임의 유저 ID가 실제로 서비스까지 전달되는지
   * 검증한다. 다른 테스트들은 userId를 {@code any()}로 느슨하게 검증하므로 이 배선은 여기서만 확인된다.
   */
  @Test
  void JWT_sub_클레임의_유저_ID가_서비스로_전달된다() throws Exception {
    when(petService.listPets(USER_ID)).thenReturn(List.of());

    mockMvc
        .perform(get("/pets").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk());

    verify(petService).listPets(USER_ID);
  }

  @Test
  void 반려견_목록을_조회한다() throws Exception {
    Breed breed = new Breed("골든리트리버");
    Pet pet = new Pet(UUID.randomUUID(), breed, "초코", PetSize.MEDIUM, 3);
    when(petService.listPets(any())).thenReturn(List.of(pet));

    mockMvc
        .perform(get("/pets").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(document("pet-list"));
  }

  @Test
  void 반려견을_등록한다() throws Exception {
    Integer breedId = 7;
    Breed breed = new Breed("골든리트리버");
    Pet pet = new Pet(UUID.randomUUID(), breed, "초코", PetSize.MEDIUM, 3);
    when(petService.createPet(any(), eq(breedId), eq("초코"), eq(PetSize.MEDIUM), eq(3)))
        .thenReturn(pet);

    String body =
        objectMapper.writeValueAsString(new PetCreateRequest("초코", breedId, PetSize.MEDIUM, 3));

    mockMvc
        .perform(
            post("/pets")
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "pet-create",
                requestFields(
                    fieldWithPath("petName").description("반려견 이름"),
                    fieldWithPath("breedId").description("견종 ID"),
                    fieldWithPath("size").description("크기 (SMALL/MEDIUM/LARGE)"),
                    fieldWithPath("age").description("나이")),
                responseFields(
                    fieldWithPath("id").description("반려견 ID"),
                    fieldWithPath("petName").description("반려견 이름"),
                    fieldWithPath("breedId").description("견종 ID"),
                    fieldWithPath("breedName").description("견종 이름"),
                    fieldWithPath("size").description("크기"),
                    fieldWithPath("age").description("나이"),
                    fieldWithPath("createdAt").description("생성일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 반려견_정보를_수정한다() throws Exception {
    UUID petId = UUID.randomUUID();
    Breed breed = new Breed("말티즈");
    Pet pet = new Pet(UUID.randomUUID(), breed, "루이", PetSize.SMALL, 2);
    when(petService.updatePet(any(), eq(petId), eq(null), eq("루이"), eq(null), eq(null)))
        .thenReturn(pet);

    String body = objectMapper.writeValueAsString(new PetUpdateRequest("루이", null, null, null));

    mockMvc
        .perform(
            patch("/pets/{petId}", petId)
                .contentType("application/json")
                .content(body)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "pet-update",
                pathParameters(parameterWithName("petId").description("반려견 ID")),
                requestFields(
                    fieldWithPath("petName").description("반려견 이름 (선택)"),
                    fieldWithPath("breedId").description("견종 ID (선택)"),
                    fieldWithPath("size").description("크기 (선택)"),
                    fieldWithPath("age").description("나이 (선택)")),
                responseFields(
                    fieldWithPath("id").description("반려견 ID"),
                    fieldWithPath("petName").description("반려견 이름"),
                    fieldWithPath("breedId").description("견종 ID"),
                    fieldWithPath("breedName").description("견종 이름"),
                    fieldWithPath("size").description("크기"),
                    fieldWithPath("age").description("나이"),
                    fieldWithPath("createdAt").description("생성일시"),
                    fieldWithPath("updatedAt").description("수정일시"))));
  }

  @Test
  void 반려견을_삭제한다() throws Exception {
    UUID petId = UUID.randomUUID();

    mockMvc
        .perform(delete("/pets/{petId}", petId).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "pet-delete", pathParameters(parameterWithName("petId").description("반려견 ID"))));
  }
}
