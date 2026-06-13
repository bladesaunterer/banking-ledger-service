package com.carson.ledger.domain;

import java.util.Currency;
import java.util.UUID;

public class Account {
    private final Currency currency;
    private final UUID id;
    private final UUID ownerId;

    public Account (UUID ownerId, Currency currency) {
        this.ownerId = ownerId;
        this.currency = currency;
        this.id = UUID.randomUUID();
    }

    private Account (UUID id, UUID ownerId, Currency currency) {
        this.ownerId = ownerId;
        this.currency = currency;
        this.id = id;
    }

    public static Account reconstruct(UUID id, UUID ownerId, Currency currency) {
        return new Account(id, ownerId, currency);
    }

    public Currency getCurrency() {
        return currency;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getId() {
        return id;
    }
}
