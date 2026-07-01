package com.carson.ledger.rest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TransferRequest extends TransactionRequest{
    @NotNull
    private final UUID toAccountId;

    public TransferRequest(String amount, UUID toAccountId) {
        super(amount);
        this.toAccountId = toAccountId;
    }

    public UUID getToAccountId() {
        return this.toAccountId;
    }
}
