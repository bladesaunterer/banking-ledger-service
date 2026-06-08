CREATE TABLE ledger_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount   NUMERIC(19,4) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    transaction_id UUID NOT NULL REFERENCES transactions(id)
);

CREATE INDEX idx_account_id on ledger_entries(account_id);

CREATE INDEX idx_transaction_id on ledger_entries(transaction_id);
