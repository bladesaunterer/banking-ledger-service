CREATE TYPE currency AS ENUM (
    'USD','EUR','JPY','GBP','CNY','AUD','CAD','CHF','HKD','SGD',
    'SEK','KRW','NOK','NZD','INR','MXN','BRL','RUB','ZAR','TRY'
);

CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_id     UUID NOT NULL ,
    currency    currency NOT NULL
);