/*
 fnr:23519035766, aktoer_id:1, navn: "Bart E Sam": skattekort tabell 8010 for 2025, brukt til faggruppe "ARBYT", ingen prosentsats satt på tabell-del, kun tabelldel eksisterer
 */

INSERT INTO aktoer (id) VALUES (1);
INSERT INTO aktoer_navn (aktoer_id, fornavn, mellomnavn, etternavn, ordinal, kilde) VALUES (1, 'Bart', 'E', 'SAM', 0, 'TEST');

INSERT INTO skattekort_allokering (id, aktoer_id, type_forskuddstrekk, trekkode, brukes_til_faggruppe)
VALUES (1, 1, 'tabell', 'loennFraNAV', 'ARBYT');

INSERT INTO aktoer_identiteter (id, aktoer_id, gjelder_fom, aktoer_ident)
VALUES (1, 1,'2025-01-01', '23519035766');

INSERT INTO aktoer_audit (id, aktoer_id, bruker, tidspunkt_endret, informasjon)
VALUES (1, 1, 'bruker', '2025-01-01T01:01:01.000Z', 'SKATTEKORT_MOTTATT');

INSERT INTO skattekort (id, aktoer_id, inntektsaar, kilde, identifikator, utstedt_dato, tilleggsopplysning)
VALUES (1,1, 2025, 'skattetaten', 123456,'2025-01-01', null);

INSERT INTO forskuddstrekk (id, skattekort_id, type_forskuddstrekk, trekkode, trekktabell, prosentsats, antall_mnd_for_trekk, frikort_belop, bruker, tidspkt_reg)
VALUES (1, 1, 'tabell', 'loennFraNAV','8010', NULL, 10.5, NULL, 'skatt', '2025-01-01T01:01:01.000Z');






