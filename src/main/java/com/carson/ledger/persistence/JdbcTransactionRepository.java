package com.carson.ledger.persistence;

import com.carson.ledger.domain.LedgerEntry;
import com.carson.ledger.domain.Transaction;
import com.carson.ledger.domain.TransactionEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTransactionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions(id, timestamp, event_type) VALUES (:id, :timestamp, :event_type)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", transaction.getId())
                .addValue("timestamp", transaction.getTimestamp())
                .addValue("event_type", transaction.getEventType());

        int rowsAffected = jdbcTemplate.update(sql, params);
        if (rowsAffected == 1) {
            return transaction;
        }
        throw new PersistenceException("Unable to persist transaction object");
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        String sql =
                "SELECT t.id as transaction_id, " +
                "t.timestamp as transaction_timestamp, " +
                "t.event_type, " +
                "le.id as entry_id, " +
                "le.amount, " +
                "le.account_id, " +
                "le.timestamp as entry_timestamp " +
                "FROM transactions t " +
                "INNER JOIN ledger_entries le ON t.id = le.transaction_id " +
                "WHERE le.account_id = :account_id " +
                "ORDER BY transaction_timestamp ASC";


        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("account_id", accountId);

        Map<UUID, Transaction.Builder> resultMap = jdbcTemplate.query(sql, params, (ResultSet rs) -> {
            Map<UUID, Transaction.Builder> map = new LinkedHashMap<>();

            while(rs.next()) {
                UUID transactionId = (UUID) rs.getObject("transaction_id");
                Instant transactionTimestamp = rs.getTimestamp("transaction_timestamp").toInstant();
                TransactionEvent transactionEvent = TransactionEvent.valueOf(rs.getString("event_type"));
                UUID ledgerEntryId = (UUID) rs.getObject("entry_id");
                BigDecimal amount = (BigDecimal) rs.getObject("amount");
                UUID accountID = (UUID) rs.getObject("account_id");
                Instant ledgerEntryTimestamp =  rs.getTimestamp("entry_timestamp").toInstant();

                Transaction.Builder builder;
                if(!map.containsKey(transactionId)) {
                    builder = new Transaction.Builder()
                            .addTransactionId(transactionId)
                            .addTimestamp(transactionTimestamp)
                            .addEventType(transactionEvent);
                    map.put(transactionId, builder);
                } else {
                    builder = map.get(transactionId);
                }

                builder.addEntry(LedgerEntry.reconstruct(ledgerEntryId, accountID, ledgerEntryTimestamp, amount));
            }

            return map;
        });

        if(resultMap == null) {
            throw new DataRetrievalException("Unable to retrieve transactions");
        }

        return resultMap.values().stream().map(Transaction.Builder::build).toList();

    }
}
