package com.carson.ledger.rest;

import com.carson.ledger.domain.Account;

import java.util.Currency;
import java.util.UUID;

public class CreateAccountResponse {
    private final UUID id;
    private final String currency;
    private final UUID ownerId;

    public  CreateAccountResponse(Account account) {
        this.currency = account.getCurrency().getCurrencyCode();
        this.id = account.getId();
        this.ownerId = account.getOwnerId();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }
}
