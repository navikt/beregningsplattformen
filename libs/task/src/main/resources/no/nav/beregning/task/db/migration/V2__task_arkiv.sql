-- Arkivtabeller. FERDIG-tasks og historikken deres flyttes hit på intervall,
-- så JOBB-tabellen og plukkeindeksen holdes små.

CREATE TABLE JOBB_ARKIV (
    id              BIGINT PRIMARY KEY,
    type            TEXT        NOT NULL,
    status          TEXT        NOT NULL,
    neste_kjoring   TIMESTAMPTZ NOT NULL,
    parameters      TEXT,
    payload         TEXT,
    prioritet       INT         NOT NULL,
    antall_forsok   INT         NOT NULL,
    opprettet_tid   TIMESTAMPTZ NOT NULL,
    sist_endret     TIMESTAMPTZ NOT NULL,
    arkivert_tid    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE JOBB_HISTORIKK_ARKIV (
    id            BIGINT PRIMARY KEY,
    jobb_id       BIGINT      NOT NULL,
    status        TEXT        NOT NULL,
    feilmelding   TEXT,
    opprettet_tid TIMESTAMPTZ NOT NULL,
    arkivert_tid  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_historikk_arkiv_jobb ON JOBB_HISTORIKK_ARKIV (jobb_id);
