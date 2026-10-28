-- task-v2, portet fra familie-prosessering. Optimistisk låsing via versjon-kolonne,
-- egen task_logg for sporbarhet. Statuser og loggtyper lagres som TEXT (enum-navn).

CREATE TABLE task (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payload        TEXT        NOT NULL,
    type           TEXT        NOT NULL,
    status         TEXT        NOT NULL DEFAULT 'UBEHANDLET',
    avvikstype     TEXT,
    metadata       TEXT        NOT NULL,
    opprettet_tid  TIMESTAMPTZ NOT NULL DEFAULT now(),
    trigger_tid    TIMESTAMPTZ NOT NULL DEFAULT now(),
    versjon        BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE task_logg (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id        BIGINT      NOT NULL REFERENCES task (id),
    type           TEXT        NOT NULL,
    node           TEXT        NOT NULL,
    endret_av      TEXT        NOT NULL DEFAULT 'VL',
    melding        TEXT,
    opprettet_tid  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Dekker plukkespørringen: klare, modne tasks sortert på opprettet_tid.
CREATE INDEX idx_task_plukk ON task (status, trigger_tid)
    WHERE status IN ('UBEHANDLET', 'KLAR_TIL_PLUKK');
CREATE INDEX idx_task_logg_task ON task_logg (task_id);
