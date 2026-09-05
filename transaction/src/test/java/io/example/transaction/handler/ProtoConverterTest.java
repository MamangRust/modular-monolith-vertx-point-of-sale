package io.example.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.example.transaction.domain.response.TransactionMonthlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionMonthlyMethodResponse;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyMethodResponse;

class ProtoConverterTest {

  @Test
  void fromTransactionResponse_shouldMapAllFields() {
    TransactionResponse r = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
        "SUCCESS", "2024-01-01", "2024-06-01");

    pb.transaction.TransactionResponse result = ProtoConverter.fromTransactionResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getOrderId()).isEqualTo(1);
    assertThat(result.getMerchantId()).isEqualTo(1);
    assertThat(result.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(result.getAmount()).isEqualTo(50000);
    assertThat(result.getChangeAmount()).isEqualTo(0);
    assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
    assertThat(result.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(result.getUpdatedAt()).isEqualTo("2024-06-01");
  }

  @Test
  void fromTransactionResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.TransactionResponse result = ProtoConverter.fromTransactionResponse(null);

    assertThat(result).isSameAs(pb.transaction.TransactionResponse.getDefaultInstance());
  }

  @Test
  void fromTransactionResponse_shouldHandleNullFields() {
    TransactionResponse r = new TransactionResponse(null, null, null, null, null, null, null, null, null);

    pb.transaction.TransactionResponse result = ProtoConverter.fromTransactionResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(0);
    assertThat(result.getOrderId()).isEqualTo(0);
    assertThat(result.getMerchantId()).isEqualTo(0);
    assertThat(result.getPaymentMethod()).isEmpty();
    assertThat(result.getAmount()).isEqualTo(0);
    assertThat(result.getChangeAmount()).isEqualTo(0);
    assertThat(result.getPaymentStatus()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  @Test
  void fromTransactionResponseDeleteAt_shouldMapAllFields() {
    TransactionResponseDeleteAt r = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
        "SUCCESS", "2024-01-01", "2024-06-01", "2024-07-01");

    pb.transaction.TransactionResponseDeleteAt result = ProtoConverter.fromTransactionResponseDeleteAt(r);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getOrderId()).isEqualTo(1);
    assertThat(result.getMerchantId()).isEqualTo(1);
    assertThat(result.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(result.getAmount()).isEqualTo(50000);
    assertThat(result.getChangeAmount()).isEqualTo(0);
    assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
    assertThat(result.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(result.getUpdatedAt()).isEqualTo("2024-06-01");
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2024-07-01");
  }

  @Test
  void fromTransactionResponseDeleteAt_shouldReturnDefaultForNullInput() {
    pb.transaction.TransactionResponseDeleteAt result = ProtoConverter.fromTransactionResponseDeleteAt(null);

    assertThat(result).isSameAs(pb.transaction.TransactionResponseDeleteAt.getDefaultInstance());
  }

  @Test
  void fromTransactionResponseDeleteAt_shouldHandleNullFields() {
    TransactionResponseDeleteAt r = new TransactionResponseDeleteAt(null, null, null, null, null, null, null, null, null, null);

    pb.transaction.TransactionResponseDeleteAt result = ProtoConverter.fromTransactionResponseDeleteAt(r);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(0);
    assertThat(result.getOrderId()).isEqualTo(0);
    assertThat(result.getMerchantId()).isEqualTo(0);
    assertThat(result.getPaymentMethod()).isEmpty();
    assertThat(result.getAmount()).isEqualTo(0);
    assertThat(result.getChangeAmount()).isEqualTo(0);
    assertThat(result.getPaymentStatus()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
    assertThat(result.hasDeletedAt()).isFalse();
  }

  @Test
  void fromTransactionResponseDeleteAt_shouldHandleNullDeletedAt() {
    TransactionResponseDeleteAt r = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
        "SUCCESS", "2024-01-01", "2024-06-01", null);

    pb.transaction.TransactionResponseDeleteAt result = ProtoConverter.fromTransactionResponseDeleteAt(r);

    assertThat(result.hasDeletedAt()).isFalse();
  }

  @Test
  void toMonthStatusSuccessResponse_shouldMapAllFields() {
    TransactionMonthlyAmountSuccessResponse r = new TransactionMonthlyAmountSuccessResponse("2024", "6", 100, 500000L);

    pb.transaction.stats.TransactionMonthStatusSuccessResponse result = ProtoConverter.toMonthStatusSuccessResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo("2024");
    assertThat(result.getMonth()).isEqualTo("6");
    assertThat(result.getTotalSuccess()).isEqualTo(100);
    assertThat(result.getTotalAmount()).isEqualTo(500000);
  }

  @Test
  void toMonthStatusSuccessResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionMonthStatusSuccessResponse result = ProtoConverter
        .toMonthStatusSuccessResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionMonthStatusSuccessResponse.getDefaultInstance());
  }

  @Test
  void toMonthStatusSuccessResponse_shouldHandleNullFields() {
    TransactionMonthlyAmountSuccessResponse r = new TransactionMonthlyAmountSuccessResponse(null, null, null, null);

    pb.transaction.stats.TransactionMonthStatusSuccessResponse result = ProtoConverter.toMonthStatusSuccessResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTotalSuccess()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }

  @Test
  void toYearStatusSuccessResponse_shouldMapAllFields() {
    TransactionYearlyAmountSuccessResponse r = new TransactionYearlyAmountSuccessResponse("2024", 1000, 5000000L);

    pb.transaction.stats.TransactionYearStatusSuccessResponse result = ProtoConverter.toYearStatusSuccessResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo("2024");
    assertThat(result.getTotalSuccess()).isEqualTo(1000);
    assertThat(result.getTotalAmount()).isEqualTo(5000000);
  }

  @Test
  void toYearStatusSuccessResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionYearStatusSuccessResponse result = ProtoConverter
        .toYearStatusSuccessResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionYearStatusSuccessResponse.getDefaultInstance());
  }

  @Test
  void toYearStatusSuccessResponse_shouldHandleNullFields() {
    TransactionYearlyAmountSuccessResponse r = new TransactionYearlyAmountSuccessResponse(null, null, null);

    pb.transaction.stats.TransactionYearStatusSuccessResponse result = ProtoConverter.toYearStatusSuccessResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalSuccess()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }

  @Test
  void toMonthStatusFailedResponse_shouldMapAllFields() {
    TransactionMonthlyAmountFailedResponse r = new TransactionMonthlyAmountFailedResponse("2024", "6", 5, 50000L);

    pb.transaction.stats.TransactionMonthStatusFailedResponse result = ProtoConverter.toMonthStatusFailedResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo("2024");
    assertThat(result.getMonth()).isEqualTo("6");
    assertThat(result.getTotalFailed()).isEqualTo(5);
    assertThat(result.getTotalAmount()).isEqualTo(50000);
  }

  @Test
  void toMonthStatusFailedResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionMonthStatusFailedResponse result = ProtoConverter
        .toMonthStatusFailedResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionMonthStatusFailedResponse.getDefaultInstance());
  }

  @Test
  void toMonthStatusFailedResponse_shouldHandleNullFields() {
    TransactionMonthlyAmountFailedResponse r = new TransactionMonthlyAmountFailedResponse(null, null, null, null);

    pb.transaction.stats.TransactionMonthStatusFailedResponse result = ProtoConverter.toMonthStatusFailedResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTotalFailed()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }

  @Test
  void toYearStatusFailedResponse_shouldMapAllFields() {
    TransactionYearlyAmountFailedResponse r = new TransactionYearlyAmountFailedResponse("2024", 50, 250000L);

    pb.transaction.stats.TransactionYearStatusFailedResponse result = ProtoConverter.toYearStatusFailedResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo("2024");
    assertThat(result.getTotalFailed()).isEqualTo(50);
    assertThat(result.getTotalAmount()).isEqualTo(250000);
  }

  @Test
  void toYearStatusFailedResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionYearStatusFailedResponse result = ProtoConverter
        .toYearStatusFailedResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionYearStatusFailedResponse.getDefaultInstance());
  }

  @Test
  void toYearStatusFailedResponse_shouldHandleNullFields() {
    TransactionYearlyAmountFailedResponse r = new TransactionYearlyAmountFailedResponse(null, null, null);

    pb.transaction.stats.TransactionYearStatusFailedResponse result = ProtoConverter.toYearStatusFailedResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalFailed()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }

  @Test
  void toMonthMethodResponse_shouldMapAllFields() {
    TransactionMonthlyMethodResponse r = new TransactionMonthlyMethodResponse("2024-06", "credit_card", 50, 250000L);

    pb.transaction.stats.TransactionMonthMethodResponse result = ProtoConverter.toMonthMethodResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getMonth()).isEqualTo("2024-06");
    assertThat(result.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(result.getTotalTransactions()).isEqualTo(50);
    assertThat(result.getTotalAmount()).isEqualTo(250000);
  }

  @Test
  void toMonthMethodResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionMonthMethodResponse result = ProtoConverter.toMonthMethodResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionMonthMethodResponse.getDefaultInstance());
  }

  @Test
  void toMonthMethodResponse_shouldHandleNullFields() {
    TransactionMonthlyMethodResponse r = new TransactionMonthlyMethodResponse(null, null, null, null);

    pb.transaction.stats.TransactionMonthMethodResponse result = ProtoConverter.toMonthMethodResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getPaymentMethod()).isEmpty();
    assertThat(result.getTotalTransactions()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }

  @Test
  void toYearMethodResponse_shouldMapAllFields() {
    TransactionYearlyMethodResponse r = new TransactionYearlyMethodResponse("2024", "credit_card", 500, 2500000L);

    pb.transaction.stats.TransactionYearMethodResponse result = ProtoConverter.toYearMethodResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo("2024");
    assertThat(result.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(result.getTotalTransactions()).isEqualTo(500);
    assertThat(result.getTotalAmount()).isEqualTo(2500000);
  }

  @Test
  void toYearMethodResponse_shouldReturnDefaultForNullInput() {
    pb.transaction.stats.TransactionYearMethodResponse result = ProtoConverter.toYearMethodResponse(null);

    assertThat(result).isSameAs(pb.transaction.stats.TransactionYearMethodResponse.getDefaultInstance());
  }

  @Test
  void toYearMethodResponse_shouldHandleNullFields() {
    TransactionYearlyMethodResponse r = new TransactionYearlyMethodResponse(null, null, null, null);

    pb.transaction.stats.TransactionYearMethodResponse result = ProtoConverter.toYearMethodResponse(r);

    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getPaymentMethod()).isEmpty();
    assertThat(result.getTotalTransactions()).isEqualTo(0);
    assertThat(result.getTotalAmount()).isEqualTo(0);
  }
}
