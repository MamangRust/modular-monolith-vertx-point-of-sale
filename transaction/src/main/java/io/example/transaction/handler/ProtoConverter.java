package io.example.transaction.handler;

import com.google.protobuf.StringValue;

import io.example.transaction.domain.response.TransactionMonthlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionMonthlyMethodResponse;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyMethodResponse;

public class ProtoConverter {

    public static pb.transaction.TransactionResponse fromTransactionResponse(TransactionResponse r) {
        if (r == null)
            return pb.transaction.TransactionResponse.getDefaultInstance();
        return pb.transaction.TransactionResponse.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setOrderId(r.getOrderId() != null ? r.getOrderId() : 0)
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setAmount(r.getAmount() != null ? r.getAmount() : 0)
                .setChangeAmount(r.getChangeAmount() != null ? r.getChangeAmount() : 0)
                .setPaymentStatus(r.getPaymentStatus() != null ? r.getPaymentStatus() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    public static pb.transaction.TransactionResponseDeleteAt fromTransactionResponseDeleteAt(
            TransactionResponseDeleteAt r) {
        if (r == null)
            return pb.transaction.TransactionResponseDeleteAt.getDefaultInstance();
        pb.transaction.TransactionResponseDeleteAt.Builder builder = pb.transaction.TransactionResponseDeleteAt
                .newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setOrderId(r.getOrderId() != null ? r.getOrderId() : 0)
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setAmount(r.getAmount() != null ? r.getAmount() : 0)
                .setChangeAmount(r.getChangeAmount() != null ? r.getChangeAmount() : 0)
                .setPaymentStatus(r.getPaymentStatus() != null ? r.getPaymentStatus() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");

        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    public static pb.transaction.stats.TransactionMonthStatusSuccessResponse toMonthStatusSuccessResponse(
            TransactionMonthlyAmountSuccessResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionMonthStatusSuccessResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionMonthStatusSuccessResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalSuccess(r.getTotalSuccess() != null ? r.getTotalSuccess() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionYearStatusSuccessResponse toYearStatusSuccessResponse(
            TransactionYearlyAmountSuccessResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionYearStatusSuccessResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionYearStatusSuccessResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalSuccess(r.getTotalSuccess() != null ? r.getTotalSuccess() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionMonthStatusFailedResponse toMonthStatusFailedResponse(
            TransactionMonthlyAmountFailedResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionMonthStatusFailedResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionMonthStatusFailedResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalFailed(r.getTotalFailed() != null ? r.getTotalFailed() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionYearStatusFailedResponse toYearStatusFailedResponse(
            TransactionYearlyAmountFailedResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionYearStatusFailedResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionYearStatusFailedResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalFailed(r.getTotalFailed() != null ? r.getTotalFailed() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionMonthMethodResponse toMonthMethodResponse(
            TransactionMonthlyMethodResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionMonthMethodResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionMonthMethodResponse.newBuilder()
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setTotalTransactions(r.getTotalTransactions() != null ? r.getTotalTransactions() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionYearMethodResponse toYearMethodResponse(
            TransactionYearlyMethodResponse r) {
        if (r == null)
            return pb.transaction.stats.TransactionYearMethodResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionYearMethodResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setTotalTransactions(r.getTotalTransactions() != null ? r.getTotalTransactions() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }
}