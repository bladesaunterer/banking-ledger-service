package com.carson.ledger.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Transaction {
    private final List<LedgerEntry> entries;
    private final UUID id;
    private final Instant timestamp;
    private final TransactionEvent eventType;

    private Transaction(Instant timestamp, List<LedgerEntry> entries, TransactionEvent eventType) {
        this.id = UUID.randomUUID();
        this.timestamp = timestamp;
        this.entries = List.copyOf(entries);
        this.eventType = eventType;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public TransactionEvent getEventType() {
        return eventType;
    }

    public static class Builder {
        private final List<LedgerEntry> entries = new ArrayList<>();
        private TransactionEvent event;

        public Builder addEntry(LedgerEntry entry) {
            this.entries.add(entry);
            return this;
        }

        public Builder addEventType(TransactionEvent event) {
            this.event = event;
            return this;
        }

        public Transaction build() {
            if (this.entries.isEmpty()) {
                throw new IllegalArgumentException("Transaction must contain entries");
            }
            if(this.event == null) {
                throw new IllegalArgumentException("Transaction must contain event type");
            }
            return new Transaction(Instant.now(), entries, event);
        }
    }
}
