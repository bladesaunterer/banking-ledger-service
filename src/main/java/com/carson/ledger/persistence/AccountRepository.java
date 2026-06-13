package com.carson.ledger.persistence;

import com.carson.ledger.domain.Account;

import java.util.List;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account account);
    List<Account> findByOwnerId(UUID ownerId);
    boolean existsById(UUID id);
}
