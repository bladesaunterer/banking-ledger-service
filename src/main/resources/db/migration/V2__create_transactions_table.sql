CREATE TYPE transaction_event AS ENUM (
    'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp   timestamptz NOT NULL,
    event_type  transaction_event NOT NULL
);