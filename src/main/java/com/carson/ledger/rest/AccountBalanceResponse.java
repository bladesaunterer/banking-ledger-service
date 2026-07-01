package com.carson.ledger.rest;

import java.math.BigDecimal;

public class AccountBalanceResponse {
    private final BigDecimal accountBalance;

    public AccountBalanceResponse(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }
}
