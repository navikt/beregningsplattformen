# Beslutningsgrunnlag for mellomlagring i beregningsplattformen

## Status og formål

Dette er et foreløpig beslutningsgrunnlag, ikke en vedtatt arkitektur. Grunnlaget er
intervjunotater fra samtale med Steinar og en kapasitetsanalyse av tallene i notatene.
Responstider og tilgjengelig samtidighet er anslag som må etterprøves.

Formålet er å vurdere hvordan beregningsplattformen kan håndtere store
beregningskjøringer under normal drift, og hvilken rolle mellomlagring (caching)
kan ha. Mellomlagring er et mulig virkemiddel, ikke et mål i seg selv.

## Behov og foreslått mål

Den største kjøringen er beregning av pensjon ved den årlige G-reguleringen i juni.
Notatene beskriver omtrent 1,5 millioner personer med skattekort og beregninger for
både mai og juni: til sammen 3 millioner beregninger.

I dag er tidsvinduet fra fredag kveld til mandag morgen. Helgekjøring medfører
kostnader til blant annet vakt. Ønsket er i stedet en bakgrunnskjøring på dagtid,
der løpende beregninger («onlineberegninger») får prioritet.

Foreslått mål er å beregne skatt for disse 3 millioner beregningene på fire timer.
Det gir i prinsippet mulighet til å kjøre beregningen på nytt samme arbeidsdag.
Oppstart, klargjøring, kontroll av resultatene og eventuell feilretting trenger
også tid; to firetimerskjøringer alene fyller åtte timer.

Målet skal nås uten vesentlig oppskalering av infrastrukturen. En ny database
eller annen infrastruktur må ikke spise opp besparelsen ved å avvikle dagens
database og stormaskin. Hva «vesentlig» betyr i kroner og ressursbruk, må avklares.

Oppdrag Z, systemet vi skal erstatte, bruker mellomlagring i stort omfang for å
nå sine ytelsesmål. Dette er relevant erfaring fra dagens løsning, men ikke i
seg selv et bevis på at beregningsplattformen trenger samme løsning. Vi bør
kartlegge hvilke data Oppdrag Z mellomlagrer, hvordan endringer håndteres, og
hvilken ytelsesgevinst dette gir.

## Arbeidsoppgaver i én beregning

Intervjunotatene beskriver følgende overordnede oppgaver:

1. Ta lås.
2. Hent beregningsordre.
3. Hent oppdrag.
4. Hent skattekort.
5. Hent trekkvedtak.
6. Korriger tidligere beregninger hvis nødvendig.
7. Hent tabell i faggruppe hvis skattekortet er et tabellskattekort.
8. Hent relevante tidligere beregninger: frikort, trekk og allerede utbetalte ytelser.
9. Gjør beregningen.
10. Lagre beregningen, inkludert brukt frikort og trekk.
11. Frigi låsen.

Listen beskriver ansvar, ikke et krav om ett REST-kall eller én SQL-spørring per
punkt. Noen oppgaver er betingede, og enkelte oppslag kan kanskje samles.
Én beregning er heller ikke nødvendigvis én databasetransaksjon. Transaksjons- og
låsgrensene må bestemmes ut fra kravene til korrekte beregninger.

## Kapasitetsbudsjett

Fire timer er 14 400 sekunder:

```text
3 000 000 beregninger / 14 400 sekunder = 208,33 beregninger per sekund
1 / 208,33 = 0,0048 sekunder = 4,8 ms
```

**4,8 ms er intervallet mellom ferdigstilte beregninger for hele systemet ved
jevn gjennomstrømning, ikke maksimal varighet for hver beregning.**
Samtidighet gjør at hver beregning kan ta lengre tid.

Ved stabil belastning er gjennomsnittlig antall pågående beregninger omtrent lik
gjennomstrømningen multiplisert med gjennomsnittlig gjennomløpstid. Med hele
kapasiteten tilgjengelig for bakgrunnskjøringen gir det følgende teoretiske budsjett:

| Samtidige beregninger | Gjennomsnittlig gjennomløpstid ved 208,33 beregninger/s |
|-----------------------|----------------------------------------------------------------|
| 1                     | 4,8 ms                                                         |
| 10                    | 48 ms                                                          |
| 15                    | 72 ms                                                          |

Notatene antar at databasen kan håndtere ti samtidige beregninger uten merkbar
ytelsesreduksjon. Geografisk segmentering og tidsoppdelte indekser nevnes som
mulige tiltak for å øke dette til femten. Dette er hypoteser, ikke dokumentert
kapasitet eller en anbefaling om partisjonering. Effekten avhenger blant annet av
datamodell, spørringer, indekser, låskonflikter og fordeling av belastningen.

Tallene over gir ingen reserve for løpende beregninger, variasjon i behandlingstid,
feil eller nye forsøk. Gjennomsnittlig gjennomløpstid omfatter også eventuell
køventing. Antall aktive databaseforbindelser er ikke det samme som antall
pågående beregninger, særlig når beregningene venter på eksterne tjenester.

## Kostnaden ved fjernkall

Intervjunotatene bruker følgende anslag:

| Operasjon | Anslått tid |
|-----------|-------------|
| Ett REST-kall | 30 ms |
| Ett databasekall | 50–500 mikrosekunder (0,05–0,5 ms) |

Det er ikke avklart om databaseanslaget omfatter nettverk, venting på forbindelse,
resultatoverføring og bekreftelse av transaksjonen. Tallene må måles fra
beregningsapplikasjonen under representativ belastning før de sammenlignes.

Som regneeksempel gir ti sekvensielle REST-kall à 30 ms omtrent 300 ms per
beregning, før lokal beregningslogikk og eventuell behandling utover det som
allerede inngår i kalltiden. Elleve slike kall ville gitt 330 ms. Ikke alle
oppgavene i listen krever et fjernkall, så dette er et scenario, ikke en måling.

Med 300 ms gjennomløpstid og femten samtidige beregninger er teoretisk
gjennomstrømning bare 50 beregninger/s. Tre millioner beregninger ville da ta
omtrent 16 timer og 40 minutter. Firetimersmålet krever minst 63 samtidige
beregninger ved denne gjennomløpstiden, uten reserve til annen trafikk.

Til sammenligning gir ti databasekall med de oppgitte anslagene 0,5–5 ms samlet
kalltid. Det er heller ikke hele beregningstiden: låser, beregningslogikk,
transaksjoner og øvrig venting kommer i tillegg dersom de ikke inngår i anslaget.

Dette taler for å begrense sekvensielle fjernkall i den hyppigst brukte
beregningsflyten. Det beviser ikke at alle data bør ligge i én database, eller at
REST-grensesnitt er feil. Slike grensesnitt kan ivareta tjenesteansvar og
tilgangskontroll. Alternativer er blant annet samlede oppslag, lokale
lesemodeller og mellomlagring. Parallelle kall kan redusere ventetid når oppslagene
er uavhengige, men øker belastningen på tjenestene som mottar kallene.

## To arkitektoniske ytterpunkter

### Én database med separate skjemaer og lokale tjenestekall

Alle data som beregningen trenger, lagres eller mellomlagres i én database,
fordelt på separate skjemaer for komponentene. Beregningen gjør oppslag gjennom
komponentens tjenestelag, som kaller komponentens repository og leser fra dens
skjema. Tjenestekallet skjer lokalt i samme prosess, ikke via REST.

Komponentene beholder dermed ansvar for egne data og oppslag, selv om de deler
database. Beregningskoden går ikke direkte inn i andre komponenters tabeller.
Mellomlagring betyr her at nødvendige data finnes i databasen nær beregningen,
ikke at alt lastes inn i applikasjonens minne.

Dette ytterpunktet unngår nettverkskall mellom komponentene i beregningsflyten.
Med anslaget på 50–500 mikrosekunder per databasekall gir ti oppslag omtrent
0,5–5 ms samlet kalltid. Hele beregningen må fortsatt måles, og vi må avklare
hvordan mellomlagrede data oppdateres og hvordan komponentene deler
databasekapasitet og håndterer transaksjoner og låser.

### Mikrotjenester der alle komponentkall går over nettverket

I det andre ytterpunktet er komponentene separate mikrotjenester. Beregningen
kaller hver komponents tjeneste over nettverket, og tjenesten gjør oppslag
gjennom sitt eget repository.

Med omtrent ti sekvensielle REST-kall à 30 ms blir anslått gjennomsnittlig
gjennomløpstid rundt 300 ms, før tillegg for lokal beregningslogikk og eventuell
køventing. Dette er et anslag for dette ytterpunktet, ikke en målt egenskap ved
alle mikrotjenestearkitekturer.

Komponentene kan driftes og skaleres hver for seg, men beregningen blir avhengig
av responstid og tilgjengelighet i flere tjenester. Ved 300 ms per beregning
krever firetimersmålet minst 63 samtidige beregninger, mot et foreløpig anslag
på ti til femten samtidige beregninger mot databasen.

Ytterpunktene viser avveiningen mellom lokale oppslag med felles
databasekapasitet og selvstendige tjenester med kostnaden ved fjernkall.
Separate skjemaer alene gir ikke uavhengig skalering eller feilisolasjon.

## Foreslått konklusjon

**Med de foreløpige anslagene kan vi ikke legge inn mer enn ett sekvensielt
fjernkall per beregning uten å sette gjennomstrømningsmålet i fare.**

Ett fjernkall på 30 ms bruker allerede en betydelig del av tidsbudsjettet på
48–72 ms per beregning. To slike kall bruker 60 ms: mer enn hele budsjettet ved
ti samtidige beregninger, og med bare 12 ms igjen ved femten. Resten skal dekke
databasearbeid, låser og beregningslogikk, samtidig som løpende beregninger skal
ha prioritet. Dette er en foreløpig dimensjoneringsregel for sekvensielle kall,
ikke en målt kapasitetsgrense.

Undersøk først om én database med separate skjemaer og lokale tjenestekall kan
oppfylle firetimersmålet innen kostnadsrammen. Sammenlign dette med ytterpunktet
der komponentkallene går over nettverket, under samme representative belastning.
Anslagene taler for lokale oppslag i beregningsflyten, men dokumenterer ikke
kapasiteten til noen av løsningene.

Reduser unødvendige sekvensielle fjernkall, men behold avklarte tjenestegrenser.
Prioriter løpende beregninger ved å begrense og kunne bremse bakgrunnskjøringen.
Separate køer er ikke nok hvis begge køene konkurrerer om de samme
databaseforbindelsene, låsene eller eksterne tjenestene.

Anbefalingen gjelder hva vi bør undersøke først. Den er ikke et valg av
bibliotek, mellomlagringsteknologi, database eller partisjoneringsmodell.

## Hva må dokumenteres før en beslutning?

Vi trenger en representativ belastningsprøve med både bakgrunnskjøring og løpende
beregninger, realistiske datamengder og variasjon i beregningsgrunnlaget.
Prøven må omfatte betingede oppslag og korrigeringer, ikke bare den enkleste flyten.

Følgende må måles og avklares:

- Antall korrekt ferdigstilte beregninger per sekund og samlet tid for 3 millioner.
- Gjennomløpstid og køventing, inkludert høye persentiler, for begge trafikktypene.
- Antall REST- og databasekall per beregning, kalltid, låsventing og transaksjonstid.
- Ressursbruk og kostnad: prosessor, minne, databasekapasitet, forbindelser og nettverk.
- Treffandel, minnebehov og belastning ved tom mellomlagring, omstart og samtidige oppslag på samme manglende verdi.
- Korrekthet når grunnlaget endres under kjøring, ved nye forsøk og ved konkurrerende beregninger for samme person.
- Krav til responstid for løpende beregninger og hvilken kapasitet de skal ha reservert.
- Kostnadsramme og hvor mye reserve firetimersmålet skal ha.

En løsning kan anbefales endelig når den gir korrekte resultater innen
firetimersmålet, samtidig som løpende beregninger oppfyller avtalte responstidskrav
og ressursbruken holder seg innen en avtalt kostnadsramme. Inntil dette er målt,
er både kapasiteten og gevinsten ved mellomlagring uavklart.
