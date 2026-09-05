package io.example.merchant.domain.requests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MerchantRequestTest {

  @Test
  void createMerchantRequest_shouldBuild() {
    CreateMerchantRequest request = CreateMerchantRequest.builder()
        .userId(1)
        .name("Merchant A")
        .description("Description")
        .address("Address")
        .contactEmail("email@test.com")
        .contactPhone("081234")
        .status("ACTIVE")
        .build();

    assertThat(request.getUserId()).isEqualTo(1);
    assertThat(request.getName()).isEqualTo("Merchant A");
    assertThat(request.getDescription()).isEqualTo("Description");
    assertThat(request.getAddress()).isEqualTo("Address");
    assertThat(request.getContactEmail()).isEqualTo("email@test.com");
    assertThat(request.getContactPhone()).isEqualTo("081234");
    assertThat(request.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  void createMerchantRequest_shouldAllowPartialBuild() {
    CreateMerchantRequest request = CreateMerchantRequest.builder()
        .userId(1)
        .name("Merchant B")
        .build();

    assertThat(request.getUserId()).isEqualTo(1);
    assertThat(request.getName()).isEqualTo("Merchant B");
    assertThat(request.getDescription()).isNull();
    assertThat(request.getAddress()).isNull();
    assertThat(request.getContactEmail()).isNull();
    assertThat(request.getContactPhone()).isNull();
    assertThat(request.getStatus()).isNull();
  }

  @Test
  void findAllMerchants_shouldUseDefaults() {
    FindAllMerchants request = FindAllMerchants.builder().build();

    assertThat(request.getPage()).isNull();
    assertThat(request.getPageSize()).isNull();
    assertThat(request.getSearch()).isNull();
  }

  @Test
  void findAllMerchants_shouldSetFields() {
    FindAllMerchants request = FindAllMerchants.builder()
        .page(2)
        .pageSize(20)
        .search("merchant")
        .build();

    assertThat(request.getPage()).isEqualTo(2);
    assertThat(request.getPageSize()).isEqualTo(20);
    assertThat(request.getSearch()).isEqualTo("merchant");
  }

  @Test
  void createMerchantDocumentRequest_shouldBuild() {
    CreateMerchantDocumentRequest request = CreateMerchantDocumentRequest.builder()
        .merchantId(1)
        .documentType("NPWP")
        .documentUrl("https://storage.example.com/doc1.pdf")
        .build();

    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getDocumentType()).isEqualTo("NPWP");
    assertThat(request.getDocumentUrl()).isEqualTo("https://storage.example.com/doc1.pdf");
  }

  @Test
  void createMerchantDocumentRequest_shouldAllowPartialBuild() {
    CreateMerchantDocumentRequest request = CreateMerchantDocumentRequest.builder()
        .merchantId(1)
        .build();

    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getDocumentType()).isNull();
    assertThat(request.getDocumentUrl()).isNull();
  }
}
