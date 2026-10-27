INSERT INTO aktoer (id) VALUES (2);
INSERT INTO aktoer_navn (aktoer_id, fornavn, mellomnavn, etternavn, ordinal, kilde) VALUES (2, 'Lisa', null, 'ANNEN', 0, 'TEST');
INSERT INTO aktoer_identiteter (id, aktoer_id, gjelder_fom, aktoer_ident)
VALUES (2, 2, '2025-01-01', '12345678910');

INSERT INTO trekkvedtak (id, trekktype, kreditor_tss_id, kreditor_trekk_id, kreditor_ref, trekkalt, belop_saldo, ansv_enhet, debitor_id, kilde, trekkstatus, bruker, tidspkt_reg, mottatt)
VALUES (4, 'FAGP', '80000427893', '', '9999-12-31', 'LOP', 0, '4910', 2, 'dare-poc', 'LOPE', 'dare-poc', '2025-07-01 12:11:10.10000', '2025-08-01 12:11:10.10000');
