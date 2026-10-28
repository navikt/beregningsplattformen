-- Reaper trenger å vite når en task ble plukket, for å kunne resette
-- tasks som har stått PLUKKET for lenge (poden som kjørte dem krasjet).
ALTER TABLE JOBB ADD COLUMN plukket_tid TIMESTAMPTZ;

-- Reaper-spørringen leter etter gamle PLUKKET-tasks.
CREATE INDEX idx_jobb_plukket ON JOBB (plukket_tid)
    WHERE status = 'PLUKKET';
