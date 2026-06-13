package com.carson.ledger.persistence;

import com.carson.ledger.domain.Account;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAccountRepository implements AccountRepository{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Account save(Account account) {
        String sql = "INSERT INTO accounts (id, owner_id, currency) VALUES (:id, :owner_id, :currency)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", account.getId())
                .addValue("owner_id", account.getOwnerId())
                .addValue("currency", account.getCurrency().getCurrencyCode());
        int rowsAffected = jdbcTemplate.update(sql, params);
        if(rowsAffected == 1) {
            return account;
        }
        throw new PersistenceException("Account creation failed");
    }

    @Override
    public List<Account> findByOwnerId(UUID ownerId) {
        String sql = "SELECT * FROM accounts WHERE owner_id = :owner_id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("owner_id", ownerId);

        return jdbcTemplate.query(sql, params, (ResultSet rs, int rowNum) -> Account.reconstruct(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("owner_id"),
            Currency.getInstance(rs.getString("currency")))
        );
    }

    @Override
    public boolean existsById(UUID id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM accounts WHERE id = :id)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, params, Boolean.class)
        );
    }
}
