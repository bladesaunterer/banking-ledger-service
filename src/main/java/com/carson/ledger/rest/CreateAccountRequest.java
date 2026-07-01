package com.carson.ledger.rest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateAccountRequest {
    @NotNull
    private final String currency;
    @NotNull
    private final UUID ownerId;

    public CreateAccountRequest(UUID ownerId, String currency) {
        this.currency = currency;
        this.ownerId = ownerId;
    }


    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }
}
