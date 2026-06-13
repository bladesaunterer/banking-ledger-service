package com.carson.ledger.persistence;

import com.carson.ledger.domain.Account;
import com.carson.ledger.domain.LedgerEntry;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcLedgerEntryRepository implements LedgerEntryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLedgerEntryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry, UUID transactionId) {
        String sql = "INSERT INTO ledger_entries (id, amount, timestamp, account_id, transaction_id) VALUES (:id, :amount, :timestamp, :account_id, :transaction_id)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", entry.getId())
                .addValue("amount", entry.getAmount())
                .addValue("timestamp", entry.getTimestamp())
                .addValue("account_id", entry.getAccountId())
                .addValue("transaction_id", transactionId);

        int rowsAffected = jdbcTemplate.update(sql, params);
        if(rowsAffected == 1) {
            return entry;
        }
        throw new PersistenceException("Ledger Entry creation failed");
    }



    @Override
    public List<LedgerEntry> findByAccountId(UUID accountId) {
        String sql = "SELECT * FROM ledger_entries WHERE account_id = :account_id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("account_id", accountId);

        return jdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> LedgerEntry.reconstruct(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("account_id"),
                rs.getTimestamp("timestamp").toInstant(),
                (BigDecimal) rs.getObject("amount"))
        );
    }

    @Override
    public BigDecimal calculateAccountBalance(UUID accountId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account_id = :account_id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("account_id", accountId);

        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }
}
