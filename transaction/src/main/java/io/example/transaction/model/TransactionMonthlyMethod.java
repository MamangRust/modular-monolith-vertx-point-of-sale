package io.example.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMonthlyMethod {
    private String month;
    private String paymentMethod;
    private Integer totalTransactions;
    private Long totalAmount;
}
