package com.carson.ledger.persistence;

import com.carson.ledger.domain.Transaction;
import com.carson.ledger.domain.TransactionEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
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
    public List<TransactionRecordDto> findByAccountId(UUID accountId) {
        String sql = "SELECT DISTINCT t.* FROM transactions t " +
                "INNER JOIN ledger_entries le ON t.id = le.transaction_id " +
                "WHERE le.account_id = :account_id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("account_id", accountId);

        return jdbcTemplate.query(sql, params, (ResultSet rs, int rowNumber) -> new TransactionRecordDto(
                (UUID) rs.getObject("id"),
                rs.getTimestamp("timestamp").toInstant(),
                TransactionEvent.valueOf(rs.getString("event_type"))
        ));
    }
}
