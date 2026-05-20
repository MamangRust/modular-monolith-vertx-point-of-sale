package io.example.transaction.handler;

import com.google.protobuf.StringValue;
import io.example.transaction.model.*;

public class ProtoConverter {

    public static pb.transaction.Transaction.TransactionResponse fromTransactionResponse(TransactionResponse r) {
        if (r == null) return pb.transaction.Transaction.TransactionResponse.getDefaultInstance();
        return pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setCardNumber(r.getCardNumber() != null ? r.getCardNumber() : "")
                .setTransactionNo(r.getTransactionNo() != null ? r.getTransactionNo() : "")
                .setAmount(r.getAmount() != null ? r.getAmount() : 0)
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setTransactionTime(r.getTransactionTime() != null ? r.getTransactionTime() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    public static pb.transaction.Transaction.TransactionResponseDeleteAt fromTransactionResponseDeleteAt(TransactionResponseDeleteAt r) {
        if (r == null) return pb.transaction.Transaction.TransactionResponseDeleteAt.getDefaultInstance();
        pb.transaction.Transaction.TransactionResponseDeleteAt.Builder builder = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setCardNumber(r.getCardNumber() != null ? r.getCardNumber() : "")
                .setTransactionNo(r.getTransactionNo() != null ? r.getTransactionNo() : "")
                .setAmount(r.getAmount() != null ? r.getAmount() : 0)
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setTransactionTime(r.getTransactionTime() != null ? r.getTransactionTime() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");

        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    public static pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse toMonthAmountResponse(TransactionMonthlyAmountSuccess r) {
        if (r == null) return pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse toYearAmountResponse(TransactionYearlyAmountSuccess r) {
        if (r == null) return pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse toMonthMethodResponse(TransactionMonthlyMethod r) {
        if (r == null) return pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setTotalTransactions(r.getTotalTransactions() != null ? r.getTotalTransactions() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse toYearMethodResponse(TransactionYearMethod r) {
        if (r == null) return pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setPaymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "")
                .setTotalTransactions(r.getTotalTransactions() != null ? r.getTotalTransactions() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse toMonthStatusSuccessResponse(TransactionMonthlyAmountSuccess r) {
        if (r == null) return pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalSuccess(r.getTotalSuccess() != null ? r.getTotalSuccess() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse toYearStatusSuccessResponse(TransactionYearlyAmountSuccess r) {
        if (r == null) return pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalSuccess(r.getTotalSuccess() != null ? r.getTotalSuccess() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse toMonthStatusFailedResponse(TransactionMonthlyAmountFailed r) {
        if (r == null) return pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalFailed(r.getTotalFailed() != null ? r.getTotalFailed() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }

    public static pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse toYearStatusFailedResponse(TransactionYearlyAmountFailed r) {
        if (r == null) return pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse.getDefaultInstance();
        return pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalFailed(r.getTotalFailed() != null ? r.getTotalFailed() : 0)
                .setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount().intValue() : 0)
                .build();
    }
}
