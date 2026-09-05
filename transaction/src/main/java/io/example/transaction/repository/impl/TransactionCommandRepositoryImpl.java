package io.example.transaction.repository.impl;

import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.UpdateTransactionRequest;
import io.example.transaction.enums.PaymentStatus;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionCommandRepositoryImpl implements TransactionCommandRepository {
        private final Pool client;

        public Future<Transaction> createTransaction(CreateTransactionRequest req) {
                return client
                                .preparedQuery("""
                                                INSERT INTO transactions (order_id, merchant_id, payment_method, amount, payment_status)
                                                VALUES ($1, $2, $3, $4, $5)
                                                RETURNING *;
                                                """)
                                .execute(Tuple.of(req.getOrderID(), req.getMerchantId(), req.getPaymentMethod(),
                                                req.getAmount(),
                                                req.getPaymentStatus() != null ? req.getPaymentStatus()
                                                                : PaymentStatus.PENDING.name()))
                                .map(rows -> Transaction.fromRow(rows.iterator().next()));
        }

        public Future<Transaction> updateTransaction(UpdateTransactionRequest req) {
                return client
                                .preparedQuery(
                                                """
                                                                UPDATE transactions
                                                                SET
                                                                        merchant_id   = COALESCE(NULLIF($2::INT, 0), merchant_id),
                                                                        payment_method = COALESCE(NULLIF($3, ''), payment_method),
                                                                        amount        = COALESCE(NULLIF($4::INT, 0), amount),
                                                                        payment_status = COALESCE(NULLIF($5, ''), payment_status),
                                                                        order_id      = COALESCE(NULLIF($6::INT, 0), order_id),
                                                                        updated_at    = CURRENT_TIMESTAMP
                                                                WHERE transaction_id = $1 AND deleted_at IS NULL
                                                                RETURNING *;
                                                                """)
                                .execute(Tuple.of(req.getTransactionID(),
                                                req.getMerchantId() != null ? req.getMerchantId() : 0,
                                                req.getPaymentMethod() != null ? req.getPaymentMethod() : "",
                                                req.getAmount() != null ? req.getAmount() : 0,
                                                req.getPaymentStatus() != null ? req.getPaymentStatus() : "",
                                                req.getOrderID() != null ? req.getOrderID() : 0))
                                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next())
                                                : null);
        }

        public Future<Transaction> trashTransaction(Long transactionId) {
                return client
                                .preparedQuery(
                                                "UPDATE transactions SET deleted_at = current_timestamp WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *")
                                .execute(Tuple.of(transactionId))
                                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next())
                                                : null);
        }

        public Future<Transaction> restoreTransaction(Long transactionId) {
                return client
                                .preparedQuery(
                                                "UPDATE transactions SET deleted_at = NULL WHERE transaction_id = $1 AND deleted_at IS NOT NULL RETURNING *")
                                .execute(Tuple.of(transactionId))
                                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next())
                                                : null);
        }

        public Future<Boolean> deleteTransactionPermanently(Long transactionId) {
                return client
                                .preparedQuery("DELETE FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL")
                                .execute(Tuple.of(transactionId))
                                .map(rowSet -> rowSet.rowCount() > 0);
        }

        public Future<Integer> restoreAllTransactions() {
                return client
                                .preparedQuery("UPDATE transactions SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                                .execute()
                                .map(RowSet::rowCount);
        }

        public Future<Integer> deleteAllPermanentTransactions() {
                return client
                                .preparedQuery("DELETE FROM transactions WHERE deleted_at IS NOT NULL")
                                .execute()
                                .map(RowSet::rowCount);
        }
}
