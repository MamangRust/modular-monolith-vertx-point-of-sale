package io.example.transaction.repository.impl;

import java.util.ArrayList;
import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class TransactionQueryRepositoryImpl implements TransactionQueryRepository {
    private final Pool client;

    public TransactionQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<Transaction>> getTransactions(String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT transaction_id, transaction_no, card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at, deleted_at,
                               COUNT(*) OVER () AS total_count
                        FROM transactions
                        WHERE deleted_at IS NULL
                          AND ($1::TEXT IS NULL OR payment_method ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%' OR card_number ILIKE '%' || $1 || '%')
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedTransactions);
    }

    @Override
    public Future<PagedResult<Transaction>> getTransactionsActive(String search, int page, int pageSize) {
        return getTransactions(search, page, pageSize);
    }

    @Override
    public Future<PagedResult<Transaction>> getTransactionsTrashed(String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT transaction_id, transaction_no, card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at, deleted_at,
                               COUNT(*) OVER () AS total_count
                        FROM transactions
                        WHERE deleted_at IS NOT NULL
                          AND ($1::TEXT IS NULL OR payment_method ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%' OR card_number ILIKE '%' || $1 || '%')
                        ORDER BY deleted_at DESC
                        LIMIT $2 OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedTransactions);
    }

    @Override
    public Future<PagedResult<Transaction>> getTransactionsByCardNumber(String cardNumber, String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT transaction_id, transaction_no, card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at, deleted_at,
                               COUNT(*) OVER () AS total_count
                        FROM transactions
                        WHERE deleted_at IS NULL AND card_number = $1
                          AND ($2::TEXT IS NULL OR payment_method ILIKE '%' || $2 || '%' OR status ILIKE '%' || $2 || '%')
                        ORDER BY created_at DESC
                        LIMIT $3 OFFSET $4;
                        """)
                .execute(Tuple.of(cardNumber, normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedTransactions);
    }

    @Override
    public Future<List<Transaction>> getTransactionsByMerchantId(Integer merchantId) {
        return client
                .preparedQuery("""
                        SELECT transaction_id, transaction_no, card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at, deleted_at
                        FROM transactions
                        WHERE merchant_id = $1 AND deleted_at IS NULL
                        ORDER BY created_at DESC;
                        """)
                .execute(Tuple.of(merchantId))
                .map(rows -> {
                    List<Transaction> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(Transaction.fromRow(row));
                    }
                    return list;
                });
    }

    @Override
    public Future<Transaction> getTransactionById(Long transactionId) {
        return client
                .preparedQuery("""
                        SELECT transaction_id, transaction_no, card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at, deleted_at
                        FROM transactions
                        WHERE transaction_id = $1 AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(transactionId))
                .map(rows -> rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null);
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? null : search;
    }

    private PagedResult<Transaction> mapPagedTransactions(RowSet<Row> rows) {
        List<Transaction> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Transaction.fromRow(row));
            if (total == 0) {
                Integer tc = row.getInteger("total_count");
                if (tc != null) total = tc;
            }
        }
        return new PagedResult<>(list, total);
    }
}
