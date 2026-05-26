package com.carson.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LedgerEntry {
    private final BigDecimal amount;
    private final Instant timestamp;
    private final UUID accountId;
    private final UUID id;

    public LedgerEntry(UUID accountId, BigDecimal amount) {
        if(amount == null) {
            throw new IllegalArgumentException("Ledger Entry must have amount");
        }
        if(accountId == null) {
            throw new IllegalArgumentException("Ledger Entry must have accountId");
        }
        this.amount = amount;
        this.timestamp = Instant.now();
        this.accountId = accountId;
        this.id = UUID.randomUUID();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getId() {
        return id;
    }
}
