# Oppdrag-bibliotek

## Feltoversikt for oppdrags-XML

### Oppdrag-felter (`oppdrag-110.*`)

| Felt                                          | Status | Beskrivelse                                                                                                              |
|-----------------------------------------------|:------:|--------------------------------------------------------------------------------------------------------------------------|
| `oppdrag-110.kodeAksjon`                      |   ✅   | Brukes til å avgjøre oppdragstype (simulering/utbetaling).                                                               |
| `oppdrag-110.kodeEndring`                     |   🙈   | Ignoreres. Oppdragstidslinjen vurderer endringer via kjeder/perioder.                                                    |
| `oppdrag-110.kodeStatus`                      |   ✅   | Brukes om vediene er `OPNY`, `OPPH`, `HVIL` eller `SPER`. Andre verdier avvises som verdier som genereres på stormaskin. |
| `oppdrag-110.datoStatusFom`                   |   ✅   | Brukes som fom for oppdragstatus (`EPOCH` brukes dersom den mangler og `kodeStatus` er satt).                            |
| `oppdrag-110.kodeFagomraade`                  |   ✅   | Mappes til `fagområde` og brukes i `OppdragId` & kjeding.                                                                |
| `oppdrag-110.fagsystemId`                     |   ✅   | Mappes til `fagsystemId` og brukes i `OppdragId` & kjeding.                                                              |
| `oppdrag-110.oppdragsId`                      |   ❌   | Avvises som stormaskin-generert felt.                                                                                    |
| `oppdrag-110.utbetFrekvens`                   |   ✅   | Mappes til `utbetalingsfrekvens`.                                                                                        |
| `oppdrag-110.datoForfall`                     |   ✅   | Mappes til `forfall`.                                                                                                    |
| `oppdrag-110.stonadId`                        |   ✅   | Mappes til `stønadId`.                                                                                                   |
| `oppdrag-110.oppdragGjelderId`                |   ✅   | Brukes til `gjelderId`.                                                                                                  |
| `oppdrag-110.datoOppdragGjelderFom`           |   ✅   | Hensyntas for å kun bruke `gjelderId` fra og med denne datoen.                                                           |
| `oppdrag-110.saksbehId`                       |   ✅   | Brukes for alle opplysninger som ligger på oppdragsnivå.                                                                 |
| `oppdrag-110.oppdrags-status-111.*`           |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrag-gjelder-112.*`           |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.bilagstype-113.bilagsType`       |   ✅   | Mappes til `bilagstype`.                                                                                                 |
| `oppdrag-110.avstemming-115.kodeKomponent`    |   ☑️   | Angir hvilket system som leverer data.                                                                                   |
| `oppdrag-110.avstemming-115.nokkelAvstemming` |   ☑️   | Brukes ifm avstemming til å bestemme hvilke data som skal avstemmes.                                                     |
| `oppdrag-110.avstemming-115.tidspktMelding`   |   ☑️   | Når meldingen ble sendt.                                                                                                 |
| `oppdrag-110.ompostering-116.*`               |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.avvent-118.datoAvventFom`        |   ✅   | Brukes i avventingsperiode (påkrevd ved behandling).                                                                     |
| `oppdrag-110.avvent-118.datoAvventTom`        |   ✅   | Brukes i avventingsperiode (påkrevd ved behandling).                                                                     |
| `oppdrag-110.avvent-118.kodeArsak`            |   ✅   | Mappes til avventingsårsak (feiler om det skulle være andre verdier enn `AVAV`/`AVRK`).                                  |
| `oppdrag-110.avvent-118.datoOverfores`        |   ✅   | Hensyntas når det settes avventingsårsak. Må være satt for `AVAV`, optional for `AVRK`                                   |
| `oppdrag-110.avvent-118.feilreg`              |   ✅   | Hensyntas ved å fjerne eksisterende avventing om den er satt til `j`                                                     |
| `oppdrag-110.oppdrags-enhet-120.typeEnhet`    |   ✅   | Håndterer verdiene `BOS` og `BEH` - andre verdier vil feile.                                                             |
| `oppdrag-110.oppdrags-enhet-120.enhet`        |   ✅   | Brukes for å hente ut enhetsnummer.                                                                                      |
| `oppdrag-110.oppdrags-enhet-120.datoEnhetFom` |   ✅   | Hensyntas for å kun bruke enheten fra og med denne datoen.                                                               |
| `oppdrag-110.belops-grense-130.*`             |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.tekst-140.tekstLnr`              |   ☑️   | Rekkefølge ved flere tekstlinjer         .                                                                               |
| `oppdrag-110.tekst-140.kodeTekst`             |   ☑️   | Kode som UR benytter til å legge på ferdig definerte tekster.                                                            |
| `oppdrag-110.tekst-140.tekst`                 |   ☑️   | Utbetalingstekst på betalingsmelding.                                                                                    |
| `oppdrag-110.tekst-140.datoTekstFom`          |   ☑️   | Start på når teksten gjelder (utbetalingsdato).                                                                          |
| `oppdrag-110.tekst-140.datoTekstTom`          |   ☑️   | Slutt på når teksten gjelder (utbetalingsdato).                                                                          |
| `oppdrag-110.tekst-140.feilreg`               |   ☑️   | Settes ved feilregistrering av tekstregistreringen.                                                                      |

### Linjefelter (`oppdrag-110.oppdrags-linje-150.*`)

| Felt                                                            | Status | Beskrivelse                                                                                                              |
|-----------------------------------------------------------------|:------:|--------------------------------------------------------------------------------------------------------------------------|
| `oppdrag-110.oppdrags-linje-150.kodeEndringLinje`               |   🙈   | Ignoreres. Oppdragstidslinjen vurderer endringer via kjeder/perioder.                                                    |
| `oppdrag-110.oppdrags-linje-150.kodeStatusLinje`                |   ✅   | Brukes om vediene er `REAK`, `OPPH`, `HVIL` eller `SPER`. Andre verdier avvises som verdier som genereres på stormaskin. |
| `oppdrag-110.oppdrags-linje-150.datoStatusFom`                  |   ✅   | Brukes som fom for linjestatus (`EPOCH` dersom mangler).                                                                 |
| `oppdrag-110.oppdrags-linje-150.vedtakId`                       |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.delytelseId`                    |   ✅   | Mappes som `delytelseId` og brukes til kjeding.                                                                          |
| `oppdrag-110.oppdrags-linje-150.linjeId`                        |   ❌   | Avvises som stormaskin-generert felt.                                                                                    |
| `oppdrag-110.oppdrags-linje-150.kodeKlassifik`                  |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.datoKlassifikFom`               |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.datoVedtakFom`                  |   ✅   | Brukes som fra og med-verdi for perioden.                                                                                |
| `oppdrag-110.oppdrags-linje-150.datoVedtakTom`                  |   ✅   | Brukes som til og med-verdi for perioden.                                                                                |
| `oppdrag-110.oppdrags-linje-150.sats`                           |   ✅   | Mappes inn som `sats`                                                                                                    |
| `oppdrag-110.oppdrags-linje-150.fradragTillegg`                 |   ☑️   | Angir om det er fradrag eller utbetaling, behøves ikke.                                                                  |
| `oppdrag-110.oppdrags-linje-150.typeSats`                       |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.skyldnerId`                     |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.datoSkyldnerFom`                |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.kravhaverId`                    |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.oppdrags-linje-150.datoKravhaverFom`               |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.oppdrags-linje-150.kid`                            |   ✅   | Mappes til `kidnummer` i opplysninger.                                                                                   |
| `oppdrag-110.oppdrags-linje-150.datoKidFom`                     |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.brukKjoreplan`                  |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.saksbehId`                      |   ✅   | Brukes for alle opplysninger som ligger på linjenivå.                                                                    |
| `oppdrag-110.oppdrags-linje-150.utbetalesTilId`                 |   ✅   | Mappes til `utbetalesTil`, enten  person, organisasjon eller samhandler.                                                 |
| `oppdrag-110.oppdrags-linje-150.datoUtbetalesTilIdFom`          |   ✅   | Hensyntas for å kun bruke `utbetalesTil` fra og med denne datoen. Om den ikke er satt brukes `datoVedtakFom`.            |
| `oppdrag-110.oppdrags-linje-150.kodeArbeidsgiver`               |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.oppdrags-linje-150.henvisning`                     |   ✅   | Mappes til `henvisning`                                                                                                  |
| `oppdrag-110.oppdrags-linje-150.typeSoknad`                     |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.refFagsystemId`                 |   ☑️   | Brukes ikke til noe, men bør kanskje valderes til å være lik det som står på oppdragsnivå?                               |
| `oppdrag-110.oppdrags-linje-150.refOppdragsId`                  |   ❌   | Avvises som stormaskin-generert felt.                                                                                    |
| `oppdrag-110.oppdrags-linje-150.refDelytelseId`                 |   ✅   | Brukes i forbindelse med kjeding.                                                                                        |
| `oppdrag-110.oppdrags-linje-150.refLinjeId`                     |   ❌   | Avvises som stormaskin-generert felt.                                                                                    |
| `oppdrag-110.oppdrags-linje-150.linje-status-151.*`             |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrags-linje-150.klassifikasjon-152.*`           |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrags-linje-150.skyldner-153.*`                 |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrags-linje-150.kid-154.*`                      |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrags-linje-150.utbetales-til-155.*`            |   ❌   | Avvises som output-felt.                                                                                                 |
| `oppdrag-110.oppdrags-linje-150.refusjonsinfo-156.maksDato`     |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.refusjonsinfo-156.refunderesId` |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.refusjonsinfo-156.datoFom`      |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.vedtakssats-157.vedtakssats`    |   ☑️   | 🧮 Trengs til input for beregning                                                                                        |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.tekstLnr`       |   ☑️   | Rekkefølge ved flere tekstlinjer.                                                                                        |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.tekstKode`      |   ☑️   | Kode som UR benytter til å legge på ferdig definerte tekster.                                                            |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.tekst`          |   ☑️   | Fritekst knyttet til oppdragslinjen.                                                                                     |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.datoTekstFom`   |   ☑️   | Start på når teksten gjelder (utbetalingsdato).                                                                          |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.datoTekstTom`   |   ☑️   | Slutt på når teksten gjelder (utbetalingsdato).                                                                          |
| `oppdrag-110.oppdrags-linje-150.linje-tekst-158.feilreg`        |   ☑️   | Settes ved feilregistrering av tekstregistreringen.                                                                      |
| `oppdrag-110.oppdrags-linje-150.linje-enhet-160.*`              |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
| `oppdrag-110.oppdrags-linje-150.grad-170.typeGrad`              |   ☑️   | Angir type grad (f.eks Uføregrad, Utbetalingsgrad, Yrkesskadegrad).                                                      |
| `oppdrag-110.oppdrags-linje-150.grad-170.grad`                  |   ☑️   | Grad i prosent.                                                                                                          |
| `oppdrag-110.oppdrags-linje-150.attestant-180.attestantId`      |   ☑️   | Ident til saksbehandler eller prosess som har attestert linjen                                                           |
| `oppdrag-110.oppdrags-linje-150.attestant-180.datoUgyldigFom`   |   ☑️   | Dato for når attestasjon ikke er gyldig lenger.                                                                          |
| `oppdrag-110.oppdrags-linje-150.valuta-190.*`                   |   ❌   | Avvises som et felt som ikke skal være i bruk.                                                                           |
