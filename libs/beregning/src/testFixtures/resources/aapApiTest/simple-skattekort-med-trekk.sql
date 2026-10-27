/*
 fnr:23519035766, aktoer_id:1, navn: "Bart E Sam": skattekort tabell 8010 for 2025, brukt til faggruppe "ARBYT", ingen prosentsats satt på tabell-del, kun tabelldel eksisterer
 */

INSERT INTO aktoer (id) VALUES (1);
INSERT INTO aktoer_navn (aktoer_id, fornavn, mellomnavn, etternavn, ordinal, kilde) VALUES (1, 'Bart', 'E', 'SAM', 0, 'TEST');

INSERT INTO skattekort_allokering (id, aktoer_id, type_forskuddstrekk, trekkode, brukes_til_faggruppe)
VALUES (1, 1, 'tabell', 'loennFraNAV', 'ARBYT');

INSERT INTO aktoer_identiteter (id, aktoer_id, gjelder_fom, aktoer_ident)
VALUES (1, 1, '2025-01-01', '23519035766');

INSERT INTO aktoer_audit (id, aktoer_id, bruker, tidspunkt_endret, informasjon)
VALUES (1, 1, 'bruker', '2025-01-01T01:01:01.000Z', 'SKATTEKORT_MOTTATT');

INSERT INTO skattekort (id, aktoer_id, inntektsaar, kilde, identifikator, utstedt_dato, tilleggsopplysning)
VALUES (1,1, 2025, 'skattetaten', 123456,'2025-01-01', null);

INSERT INTO forskuddstrekk (id, skattekort_id, type_forskuddstrekk, trekkode, trekktabell, prosentsats, antall_mnd_for_trekk, frikort_belop, bruker, tidspkt_reg)
VALUES (1, 1, 'tabell', 'loennFraNAV','8010', NULL, 10.5, NULL, 'skatt', '2025-01-01T01:01:01.000Z');

INSERT INTO trekktype (KODE_TREKKTYPE, BESKRIVELSE, PRIORITET, KODE_KLASSE_TREKK, KODE_TREKKATEGORI, REDUSER_SKATTEGR, KODE_OPPGJORSTYPE, KODE_KLASSE_OPPGJ, BELOPSGRENSE, BRUKERID, TIDSPKT_ENDRET,
                       BEREGNINGSTYPE)
VALUES ('FAGP', 'Pensjonsavgift', '01', 'FAGPKRED', 'KRED', true, 'KRED', 'FAGPKREDOPPGJ', 0.00, 'OS16HL4', '2016-11-26 00:17:58.375890', 'YTELPER'),
       ('RKSE', 'Refkrav fra eksterne i kontantstøtte', '01', 'RKSEKRED', 'KRED', false, 'KRED', 'RKSEKREDOPPGJ', 0.00, 'POPPSETT', '2011-02-18 11:55:28.969218', 'YTELPER'),
       ('TRK1', 'Utleggstrekk, ett trekk', '12', 'TRK1', 'KRED', false, 'TRK1', 'TRK1OPPG', 0.00, 'TOB-4521', '2025-03-17 16:21:40.529120', 'UTBDATO');

INSERT INTO trekkvedtak (id, trekktype, kreditor_tss_id, kreditor_trekk_id, kreditor_ref, trekkalt, belop_saldo, ansv_enhet, debitor_id, kilde, trekkstatus, bruker, tidspkt_reg, mottatt)
VALUES (1, 'FAGP', '80000427893', '', '9999-12-31', 'LOP', 0, '4910', 1, 'dare-poc', 'LOPE', 'dare-poc', '2025-07-01 12:11:10.10000', '2025-07-01 12:11:10.10000');
INSERT INTO trekkvedtak (id, trekktype, kreditor_tss_id, kreditor_trekk_id, kreditor_ref, trekkalt, belop_saldo, ansv_enhet, debitor_id, kilde, trekkstatus, bruker, tidspkt_reg, mottatt)
VALUES (2, 'RKSE', '80000427893', '', '9999-12-31', 'LOP', 0, '4910', 1, 'dare-poc', 'LOPE', 'dare-poc', '2025-07-01 12:11:10.10000', '2025-07-01 12:11:10.10000');
INSERT INTO trekkvedtak (id, trekktype, kreditor_tss_id, kreditor_trekk_id, kreditor_ref, trekkalt, belop_saldo, ansv_enhet, debitor_id, kilde, trekkstatus, bruker, tidspkt_reg, mottatt)
VALUES (3, 'TRK1', '80000427893', '', '9999-12-31', 'LOP', 0, '4910', 1, 'dare-poc', 'LOPE', 'dare-poc', '2025-07-01 12:11:10.10000', '2025-07-01 12:11:10.10000');

INSERT INTO trekkperiode (id, trekkvedtak_id, dato_fom, dato_tom, sats, satstype, feil_reg, bruker, tidspkt_reg)
VALUES (1, 1, '2025-03-31', '2025-04-05', 100, 'D', false, 'dare-poc', '2025-07-01 12:11:10.10000');
INSERT INTO trekkperiode (id, trekkvedtak_id, dato_fom, dato_tom, sats, satstype, feil_reg, bruker, tidspkt_reg)
VALUES (2, 2, '2025-03-31', '2025-04-05', 1000, 'M', false, 'dare-poc', '2025-07-01 12:11:10.10000');
INSERT INTO trekkperiode (id, trekkvedtak_id, dato_fom, dato_tom, sats, satstype, feil_reg, bruker, tidspkt_reg)
VALUES (3, 3, '2025-03-31', '2025-04-11', 150, 'D', false, 'dare-poc', '2025-07-01 12:11:10.10000');

INSERT INTO trekk_i_fagomraade(id, trekkvedtak_id, fagomraade_kode, aktiv, tidspkt_reg)
VALUES (1, 1, 'AAP', true, '2025-07-01 12:11:10.10000');
INSERT INTO trekk_i_fagomraade(id, trekkvedtak_id, fagomraade_kode, aktiv, tidspkt_reg)
VALUES (2, 2, 'AAP', true, '2025-07-01 12:11:10.10000');
INSERT INTO trekk_i_fagomraade(id, trekkvedtak_id, fagomraade_kode, aktiv, tidspkt_reg)
VALUES (3, 3, 'AAP', true, '2025-07-01 12:11:10.10000');
