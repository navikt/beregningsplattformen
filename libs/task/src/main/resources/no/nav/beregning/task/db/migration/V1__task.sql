-- Task-kø etter motor-mønster: status-kolonne på JOBB for rask plukking,
-- append-only JOBB_HISTORIKK for full sporbarhet på statusoverganger.

CREATE TABLE JOBB (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type            TEXT        NOT NULL,
    status          TEXT        NOT NULL DEFAULT 'KLAR',
    neste_kjoring   TIMESTAMPTZ NOT NULL DEFAULT now(),
    parameters      TEXT,
    payload         TEXT,
    prioritet       INT         NOT NULL DEFAULT 30,
    antall_forsok   INT         NOT NULL DEFAULT 0,
    opprettet_tid   TIMESTAMPTZ NOT NULL DEFAULT now(),
    sist_endret     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE JOBB_HISTORIKK (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jobb_id       BIGINT      NOT NULL REFERENCES JOBB (id),
    status        TEXT        NOT NULL,
    feilmelding   TEXT,
    opprettet_tid TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Dekker plukkespørringen: modne, klare/feilede tasks sortert etter prioritet og tid.
-- Partial index holder seg liten ved at FERDIG/PLUKKET/AVBRUTT ikke indekseres.
CREATE INDEX idx_jobb_plukk ON JOBB (status, neste_kjoring, prioritet, id)
    WHERE status IN ('KLAR', 'FEILET');

CREATE INDEX idx_historikk_jobb ON JOBB_HISTORIKK (jobb_id);
