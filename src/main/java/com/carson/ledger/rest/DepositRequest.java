package com.carson.ledger.rest;

public class DepositRequest extends TransactionRequest {
    public DepositRequest(String amount) {
        super(amount);
    }
}
