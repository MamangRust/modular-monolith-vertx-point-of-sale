package io.example.transaction.repository.impl;

import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class TransactionCommandRepositoryImpl implements TransactionCommandRepository {
    private final Pool client;

    public TransactionCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Transaction> createTransaction(String cardNumber, Integer amount, String paymentMethod, Integer merchantId, String status) {
        return client
                .preparedQuery("""
                        INSERT INTO transactions (card_number, amount, payment_method, merchant_id, status, transaction_time)
                        VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
                        RETURNING *;
                        """)
                .execute(Tuple.of(cardNumber, amount, paymentMethod, merchantId, status != null ? status : "pending"))
                .map(rows -> Transaction.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Transaction> updateTransaction(Long transactionId, String cardNumber, Integer amount, String paymentMethod, Integer merchantId, String status) {
        return client
                .preparedQuery("""
                        UPDATE transactions
                        SET card_number = $2, amount = $3, payment_method = $4, merchant_id = $5, status = $6, updated_at = CURRENT_TIMESTAMP
                        WHERE transaction_id = $1 AND deleted_at IS NULL
                        RETURNING *;
                        """)
                .execute(Tuple.of(transactionId, cardNumber, amount, paymentMethod, merchantId, status))
                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Transaction> trashTransaction(Long transactionId) {
        return client
                .preparedQuery("""
                        UPDATE transactions
                        SET deleted_at = CURRENT_TIMESTAMP
                        WHERE transaction_id = $1 AND deleted_at IS NULL
                        RETURNING *;
                        """)
                .execute(Tuple.of(transactionId))
                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Transaction> restoreTransaction(Long transactionId) {
        return client
                .preparedQuery("""
                        UPDATE transactions
                        SET deleted_at = NULL
                        WHERE transaction_id = $1 AND deleted_at IS NOT NULL
                        RETURNING *;
                        """)
                .execute(Tuple.of(transactionId))
                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Void> deleteTransactionPermanently(Long transactionId) {
        return client
                .preparedQuery("DELETE FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(transactionId))
                .mapEmpty();
    }

    @Override
    public Future<Integer> restoreAllTransactions() {
        return client
                .preparedQuery("UPDATE transactions SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }

    @Override
    public Future<Integer> deleteAllPermanentTransactions() {
        return client
                .preparedQuery("DELETE FROM transactions WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }
}
