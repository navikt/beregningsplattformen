# libs:task

Asynkron task-kø for apper i beregningsplattformen. Persisterer tasks i Postgres med JDBI, plukker dem i batch og kjører dem i bakgrunnen. Inspirert av [motor](https://github.com/navikt/aap-kelvin-komponenter/tree/main/motor).

## Kom i gang

Legg modulen til i appen:

```kotlin
dependencies {
    implementation(project(":libs:task"))
}
```

Appen eier datakilden og kjører Flyway. Migreringene ligger på classpath:

```kotlin
Flyway.configure()
    .dataSource(dataSource)
    .locations("classpath:no/nav/beregning/task/db/migration")
    .load()
    .migrate()
```

Registrer task-typene dine og start motoren:

```kotlin
val registry = TaskRegistry(listOf(SendOppdragTask))
val runner = TaskRunner(dataSource, registry).apply { start() }
// ved shutdown: runner.stop()
```

Legg en task i kø, i samme transaksjon som forretningsdataene (utbox):

```kotlin
jdbi.useTransaction<Exception> { handle ->
    handle.attach(VedtakRepository::class.java).lagre(vedtak)
    TaskRepository(handle).leggTil(TaskInput(type = "send-oppdrag", payload = xml))
}
```

## Kontrakter du må kjenne

Disse invariantene er ikke valgfrie. Brytes de, får du dobbeltkjøring, tapte tasks eller feil i produksjon.

**utfor() må være idempotent.** Køen gir at-least-once. En pod kan krasje etter at en task har gjort en ekstern sideeffekt, men før den er markert FERDIG. Da kjøres tasken på nytt. Sender du oppdrag til MQ, må du håndtere duplikater selv, for eksempel med en idempotensnøkkel.

**Enqueue i konsumentens transaksjon.** `leggTil` tar en JDBI `Handle`. Kall den inne i forretningstransaksjonen din, så en rollback der også ruller tilbake tasken. Enqueuer du utenfor, mister du utbox-garantien.

**Reaper-timeout må være lengre enn tregeste task.** En krasjet task blir hengende PLUKKET til reaperen setter den tilbake til KLAR. Er `reaperTimeout` kortere enn en task faktisk bruker, resetter reaperen en task som fortsatt kjører, og den kjøres dobbelt. Default er 30 minutter.

**Ikke logg payload.** Payload er ren tekst biblioteket ikke tolker. `toString` på `TaskInput` og `TaskRecord` utelater den med vilje. Feilmeldinger lagrer kun exception-type og melding, ikke payload.

## Slik virker den

Én poller henter inntil `batchStorrelse` modne tasks i én kort transaksjon med `FOR UPDATE SKIP LOCKED`, committer dem som PLUKKET, og legger dem på en intern kø. En worker-pool kjører hver task i sin egen transaksjon: FERDIG ved suksess, FEILET med backoff ved feil. En reaper resetter hengende PLUKKET-tasks.

Fordi plukkingen committes, er antall samtidige tasks frikoblet fra antall DB-tilkoblinger. Prisen er reaperen: Postgres frigjør ikke lenger en task automatisk ved krasj.

Status ligger både som kolonne på `JOBB` (rask plukking) og som append-only logg i `JOBB_HISTORIKK` (full sporbarhet). Hver statusendring skriver begge i samme transaksjon.

## Konfigurasjon

`TaskRunner` tar disse parameterne:

| Parameter | Default | Beskrivelse |
|-----------|---------|-------------|
| `antallWorkere` | 8 | Tråder som kjører tasks parallelt |
| `batchStorrelse` | 20 | Tasks plukket per runde |
| `pollInterval` | 2 s | Pause mellom runder når det er arbeid |
| `tomKoBackoff` | 10 s | Pause når køen er tom |
| `reaperTimeout` | 30 min | Når en PLUKKET task regnes som hengende |
| `reaperInterval` | 5 min | Hvor ofte reaperen kjører |

Dimensjoner HikariCP-poolen til minst `antallWorkere + 1`.

## Status

Ikke ferdig: metrikker (kølengde, eldste ujobbede, feilrate) er utsatt.

Observerbarhet ellers er på plass: MDC-loggkontekst via `TaskLogInfoProvider`, og manuelle OpenTelemetry-spans rundt hver task-utførelse. Spannene bruker `GlobalOpenTelemetry`, så de kobles opp av OTel-agenten Nais injiserer og er no-op lokalt.

## Team

Eies av beregningsplattformen. {TODO: Slack-kanal}
