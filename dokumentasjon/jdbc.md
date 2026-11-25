# JDBC-libben og migreringer

`libs/jdbc` samler oppsett av tilkoblingspool med HikariCP, databasesjekk og
migreringer med Flyway. Appen oppretter poolen, kjører migreringene ved oppstart og
lukker poolen når appen stopper. Kotliquery, spørringer og transaksjoner ligger
utenfor libben.

## Ta libben i bruk

Legg til avhengigheten i appens `build.gradle.kts`:

```kotlin
implementation(project(":libs:jdbc"))
```

Libben gir tilgang til HikariCP og Flyway. PostgreSQL-driveren og Flyways
PostgreSQL-støtte følger med ved kjøring. Appen legger selv til Kotliquery og
eventuell metrikk.

Hovedappen og Trekk oppretter poolen i `Application.initializeDatabase()`:

```kotlin
val dataSource = Jdbc.initialize(
    config = postgresProperties.toJdbcConfig(),
) {
    poolName = postgresProperties.name
    metricsTrackerFactory = MicrometerMetricsTrackerFactory(Metrics.prometheusMeterRegistry)
}

monitor.subscribe(ApplicationStopped) {
    dataSource.close()
}
```

`toJdbcConfig()` ligger i appens `PostgresProperties`, ikke i libben. Metoden bruker
eksplisitt JDBC-URL hvis den finnes, ellers bygger den URL fra host, port og
databasenavn. Brukernavn og passord kommer fra appens konfigurasjon.

Send samme `DataSource` videre til tjenestene som trenger den. Ikke opprett en
egen pool per modul. Databasesjekken `dataSource.isHealthy()` låner en forbindelse,
validerer den og leverer den tilbake til poolen.

## Standardverdier

| Innstilling | Standard i libben |
|---|---|
| Maksimalt antall forbindelser | 3 |
| Minste antall ledige forbindelser | 1 |
| Levetid for ledige forbindelser (`idleTimeout`) | 5 minutter |
| Maksimal levetid for forbindelser (`maxLifetime`) | 30 minutter |
| Ventetid på forbindelse (`connectionTimeout`) | 2 sekunder |
| Tidsgrense for validering (`validationTimeout`) | 1 sekund |
| Tidsgrense ved pooloppstart (`initializationFailTimeout`) | 30 sekunder |
| Migreringsskjema | `public` |
| Migreringsmappe | `classpath:db/migration` |
| Manglende migreringsmappe | Gir feil |
| Ugyldige migreringsfilnavn | Gir feil |

Poolinnstillinger kan overstyres i blokken til `Jdbc.initialize`. Migreringsvalg
kan overstyres i blokken til `migrate`. Libben bruker ellers Flyways
standardverdier, blant annet at eldre versjoner ikke kjøres etter nyere versjoner
(`outOfOrder = false`).

## Trekk bruker standardmappen og public

Trekk har migreringsfilene under:

```text
apps/trekk/src/main/resources/db/migration/
```

Appen trenger derfor bare:

```kotlin
dataSource.migrate()
```

Flyway registrerer migreringene i `public.flyway_schema_history`.

## Skattekort bruker egen mappe og eget skjema

Migreringsfilene ligger i skattekort-modulen:

```text
apps/beregningsplattform/skattekort/
  src/main/resources/db/migration/skattekort/
    V1__init.sql
```

Hovedappen har avhengighet til modulen og finner ressursene i modulens jar-fil.
`Application` i hovedappen kjører migreringen før MQ-konsumenten starter:

```kotlin
dataSource.migrate(schema = "skattekort") {
    locations("classpath:db/migration/skattekort")
}
```

Flyway oppretter skjemaet hvis det mangler og databasebrukeren har nødvendige
rettigheter. Historikken ligger i `skattekort.flyway_schema_history`.
Eksempelfilen `V1__init.sql` kjører bare `SELECT 1;`. Den oppretter ingen
forretningstabell.

## Hvorfor har hver modul sin egen mappe?

Mappen trenger ikke hete akkurat `db/migration/skattekort`. Det er en navnekonvensjon
vi bruker for å skille modulenes migreringsfiler.

Flyway søker på appens samlede classpath, også i jar-filer fra modulene, og søker
rekursivt under valgt mappe. Det finnes ingen automatisk kobling mellom
Gradle-modulen `skattekort` og migreringsfilene Flyway velger.

Hvis både skattekort og en modul `abc` legger hver sin `V1` under
`db/migration`, vil et søk på rotmappen finne begge. Da får Flyway to migreringer
med samme versjon i samme migreringskjøring.

Bruk derfor en unik mappe og et eksplisitt kall for hver modul:

```text
skattekort/src/main/resources/db/migration/skattekort/V1__init.sql
abc/src/main/resources/db/migration/abc/V1__init.sql
```

```kotlin
dataSource.migrate(schema = "skattekort") {
    locations("classpath:db/migration/skattekort")
}
dataSource.migrate(schema = "abc") {
    locations("classpath:db/migration/abc")
}
```

`abc` er et eksempel, ikke en modul som er satt opp her.

| Valg | Bestemmer |
|---|---|
| `locations(...)` | Hvilke SQL-filer Flyway leser |
| `schema = ...` | Standardskjema for migreringen og plassering av Flyway-historikken |

Skjema og mappe trenger ikke ha samme navn. Å angi bare `schema = "skattekort"`
endrer ikke standardmappen. Et skjema filtrerer heller ikke hvilke filer Flyway
leser. Libben binder migreringen til datakilden og skjemaet etter at
konfigurasjonsblokken har kjørt.

Separate mapper og skjemaer lar modulene ha hver sin `V1` og egen historikk.
De deler fortsatt database og tilkoblingspool. Skjemaoppsettet erstatter ikke
tilgangskontroll.

## Filnavn og kjøring

Versjonerte migreringer følger Flyways format:

```text
V1__opprett_tabell.sql
V2__legg_til_kolonne.sql
```

Bruk stor `V`, en unik versjon i det aktuelle migreringsoppsettet, to understreker,
en beskrivelse og filendelsen `.sql`. Datobaserte versjoner som
`V1__trekk_vedtak.sql` er også gyldige. Flyway støtter dessuten
`R__beskrivelse.sql` for repeterbare migreringer som kjøres på nytt når innholdet
endres.

Ikke endre innhold, navn eller versjon på en migrering som allerede er kjørt i et
felles miljø. Legg til en ny versjon. Flyway bruker historikken og sjekksummen til
å kontrollere tidligere kjørte migreringer.

Hvis to appinstanser migrerer samme skjema samtidig, bruker Flyway låsing slik at de
ikke utfører samme versjonerte migrering parallelt. Ved feil kaster libben
unntaket videre, og oppstartskallet fullføres ikke. PostgreSQL ruller tilbake den
feilende SQL-migreringen når den kjører i en transaksjon. Tidligere fullførte
migreringer blir stående. Migrering av flere moduler er ikke én felles
transaksjon.

## Kode

- [JDBC-libben](../libs/jdbc/src/main/kotlin/no/nav/utbetaling/beregning/jdbc/Jdbc.kt)
- [Oppstart i hovedappen](../apps/beregningsplattform/src/main/kotlin/no/nav/utbetaling/beregning/beregningsplattform/Application.kt)
- [Oppstart i Trekk](../apps/trekk/src/main/kotlin/no/nav/utbetaling/beregning/trekk/Application.kt)
- [Skattekort-migreringen](../apps/beregningsplattform/skattekort/src/main/resources/db/migration/skattekort/V1__init.sql)
