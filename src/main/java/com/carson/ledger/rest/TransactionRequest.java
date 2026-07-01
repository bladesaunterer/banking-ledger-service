package com.carson.ledger.rest;

import jakarta.validation.constraints.NotNull;

public abstract class TransactionRequest {
    @NotNull
    private final String amount;

    public TransactionRequest(String amount){
        this.amount = amount;
    }

    public String getAmount() {
        return amount;
    }
}
