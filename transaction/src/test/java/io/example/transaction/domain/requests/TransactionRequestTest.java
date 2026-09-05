package io.example.transaction.domain.requests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;

class TransactionRequestTest {

  @Test
  void createTransactionRequest_shouldBuild() {
    CreateTransactionRequest request = CreateTransactionRequest.builder()
        .orderID(1)
        .merchantId(1)
        .paymentMethod("credit_card")
        .amount(50000)
        .paymentStatus("SUCCESS")
        .build();

    assertThat(request.getOrderID()).isEqualTo(1);
    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(request.getAmount()).isEqualTo(50000);
    assertThat(request.getPaymentStatus()).isEqualTo("SUCCESS");
  }

  @Test
  void createTransactionRequest_shouldUseJsonPropertyNames() {
    CreateTransactionRequest request = CreateTransactionRequest.builder()
        .orderID(2)
        .merchantId(2)
        .paymentMethod("debit_card")
        .amount(30000)
        .paymentStatus("PENDING")
        .build();

    assertThat(request.getOrderID()).isEqualTo(2);
    assertThat(request.getMerchantId()).isEqualTo(2);
    assertThat(request.getPaymentMethod()).isEqualTo("debit_card");
    assertThat(request.getAmount()).isEqualTo(30000);
    assertThat(request.getPaymentStatus()).isEqualTo("PENDING");
  }

  @Test
  void createTransactionRequest_shouldAllowPartialFields() {
    CreateTransactionRequest request = CreateTransactionRequest.builder()
        .orderID(1)
        .merchantId(1)
        .build();

    assertThat(request.getOrderID()).isEqualTo(1);
    assertThat(request.getMerchantId()).isEqualTo(1);
    assertThat(request.getPaymentMethod()).isNull();
    assertThat(request.getAmount()).isNull();
    assertThat(request.getPaymentStatus()).isNull();
  }

  @Test
  void findAllTransactionRequest_shouldUseDefaults() {
    FindAllTransactionRequest request = FindAllTransactionRequest.builder().build();

    assertThat(request.getPage()).isEqualTo(1);
    assertThat(request.getPageSize()).isEqualTo(10);
    assertThat(request.getSearch()).isNull();
  }

  @Test
  void findAllTransactionRequest_shouldSetFields() {
    FindAllTransactionRequest request = FindAllTransactionRequest.builder()
        .page(2)
        .pageSize(20)
        .search("test")
        .build();

    assertThat(request.getPage()).isEqualTo(2);
    assertThat(request.getPageSize()).isEqualTo(20);
    assertThat(request.getSearch()).isEqualTo("test");
  }

  @Test
  void findAllTransactionRequest_shouldAllowNullSearch() {
    FindAllTransactionRequest request = FindAllTransactionRequest.builder()
        .page(3)
        .pageSize(15)
        .build();

    assertThat(request.getPage()).isEqualTo(3);
    assertThat(request.getPageSize()).isEqualTo(15);
    assertThat(request.getSearch()).isNull();
  }

  @Test
  void monthAmountTransactionRequest_shouldBuild() {
    MonthAmountTransactionRequest request = MonthAmountTransactionRequest.builder()
        .year(2024)
        .month(6)
        .build();

    assertThat(request.getYear()).isEqualTo(2024);
    assertThat(request.getMonth()).isEqualTo(6);
  }

  @Test
  void monthAmountTransactionRequest_shouldAllowNullFields() {
    MonthAmountTransactionRequest request = MonthAmountTransactionRequest.builder().build();

    assertThat(request.getYear()).isNull();
    assertThat(request.getMonth()).isNull();
  }
}
