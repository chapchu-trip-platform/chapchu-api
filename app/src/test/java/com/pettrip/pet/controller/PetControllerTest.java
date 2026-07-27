package com.pettrip.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PetControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PetService petService;

  @Test
  void 반려견_목록을_조회한다() throws Exception {
    Breed breed = new Breed("골든리트리버");
    Pet pet = new Pet(UUID.randomUUID(), breed, "초코", PetSize.MEDIUM, 3);
    when(petService.listPets(any())).thenReturn(List.of(pet));

    mockMvc.perform(get("/pets")).andExpect(status().isOk()).andDo(document("pet-list"));
  }

  @Test
  void 반려견을_등록한다() throws Exception {
    UUID breedId = UUID.randomUUID();
    Breed breed = new Breed("골든리트리버");
    Pet pet = new Pet(UUID.randomUUID(), breed, "초코", PetSize.MEDIUM, 3);
    when(petService.createPet(any(), eq(breedId), eq("초코"), eq(PetSize.MEDIUM), eq(3)))
        .thenReturn(pet);

    String body =
        objectMapper.writeValueAsString(new PetCreateRequest("초코", breedId, PetSize.MEDIUM, 3));

    mockMvc
        .perform(post("/pets").contentType("application/json").content(body))
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
        .perform(patch("/pets/{petId}", petId).contentType("application/json").content(body))
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
        .perform(delete("/pets/{petId}", petId))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "pet-delete", pathParameters(parameterWithName("petId").description("반려견 ID"))));
  }
}
