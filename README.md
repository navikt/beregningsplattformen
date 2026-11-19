# Beregningsplattformen

![img](beregningsplattformen.png)

## Struktur

```
.
├── apps/
│   ├── beregningsplattform # hovedapp som samler API-er og meldingsbehandling
│   │   └── skattekort     # egen Gradle-modul som kjøres i hovedappen
│   ├── lab                # testverktøy for funksjonelle tester
│   │   ├── backend
│   │   └── frontend
│
├── libs/
│   ├── http              # felles Ktor HTTP-klient med JSON-støtte, tidsavbrudd og nye forsøk
│   ├── kotlinx           # JSON-oppsett og serialisering av datoer, tidspunkter og UUID-er
│   ├── log               # logging, JSON-koding av logghendelser og TCP-sending til sikkerlogg
│   ├── oppdrag           # leser oppdrag fra XML/JSON og bygger utbetalings- og simuleringstidslinjer
│   ├── tid               # datoperioder og tidslinjer med operasjoner for å kombinere dem
│
└── documentasjon/        # intern doc
```

## Henvendelser

Spørsmål om koden eller prosjektet kan stilles som issues her på GitHub.
Interne henvendelser kan sendes i Slack-kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP).