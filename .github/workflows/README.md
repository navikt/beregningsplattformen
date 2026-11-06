# Gradle-workflows

Du trenger ikke lage egne workflows når du lager nye libs/apper i Gradle. Det skal fanges automatisk opp av de etablerte workflowene.

Workflowene tester, deployer og publiserer bare Gradle-modulene som er påvirket av en endring. Gradle-tasken `paavirkedeModuler` i `build-logic` finner modulene.

| Workflow                          | Trigger                | Gjør                                                                                                                                                                                                                                                     |
|-----------------------------------|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `gradle-test-pull-request.yaml`   | pull request           | Kjører `:test` på alle påvirkede moduler                                                                                                                                                                                                                 |
| `gradle-push-main.yaml`           | push til main          | Deployer påvirkede apper, publiserer påvirkede biblioteker og tester interne libs                                                                                                                                                                        |
| `gradle-scheduled-apps-main.yaml` | mandag kl. 05 UTC      | Redeployer alle apper (selv om de ikke er endret), slik at de får nye baseimages og avhengigheter                                                                                                                                                        |
| `gradle-manual-deploy.yaml`       | manuelt, alle brancher | Deployer én app til ett miljø, prod bare fra main (input `app` og `miljo` blir brukt som input til `./gradlew appDeployConfig`). Det du skriver inn av app og miljø må ikke være helt ordrett, men være unikt nok til å identifisere én app i ett miljø. |
| `gradle-deploy-app.yaml`          | `workflow_call`        | Bygger, lager image og deployer én app. Brukes av de tre over                                                                                                                                                                                            |

De gjenbrukbare stegene (bygg, image og Nais-deploy) ligger i [navikt/beregningsplattform-gha-workflows](https://github.com/navikt/beregningsplattform-gha-workflows).

## Påvirkede moduler

Tasken sammenligner `HEAD` med felles stamfar til `--siden` (lokalt mot origin/main om du ikke sender inn noe) og skriver ut JSON:

```json
{"alle": [...], "apper": [...], "publiserteBiblioteker": [...], "resten": [...]}
```

En modul er påvirket når den har en endret fil, eller når den avhenger av en påvirket modul, direkte eller gjennom andre moduler.

Endrer du en global fil, blir alle moduler påvirket. De globale filene er `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` og Gradle-wrapperen. Endringer under `build-logic/` gir bare `:build-logic`, siden pluginene der ikke endrer hvordan modulene bygges.

Finnes det ingen felles stamfar, for eksempel etter en force-push, regnes alle moduler som påvirket.

Med `--alle-apper` kommer alle apper med i `apper`, også de som ikke er endret. Den scheduled-workflowen bruker dette.

## Apper

En modul under `:apps` regnes som en app når `.nais`-katalogen har naiserator-filer som heter `<navn>_<cluster>.yaml`, for eksempel `q1_dev-gcp.yaml`. Moduler uten slike filer testes, men deployes ikke.

Man kan ha flere naiserator-filer for samme app & cluster, for eksempel `q1_dev-gcp.yaml` og `q2_dev-gcp.yaml`. Da deployes appen til begge miljøene.

Hver fil blir ett miljø. Clustere som starter med `prod` er prodmiljøer, de andre er testmiljøer. 

`gradle-deploy-app.yaml` deployer først alle testmiljøene. Prodmiljøene deployes bare hvis ingen testmiljøer feilet, eller hvis appen ikke har noen.

Imaget bygges fra `.nais/Dockerfile` og får navnet til modulen uten `:apps:`, for eksempel `lab-backend`.

## Biblioteker

Moduler med pluginen `publiser-bibliotek` publiseres til GitHub Packages når de er påvirket på main. Versjonen er tidspunktet for kjøringen i formatet `ÅÅÅÅMMDD.TTMM` (norsk tid).

```kotlin
plugins {
    id("publiser-bibliotek")
}
```

## Begrensninger på main

`gradle-push-main.yaml` regner endringer fra `github.event.before`, altså forrige commit på main før pushen. Det gir to begrensninger:

- Feiler en kjøring, tar ikke neste push med seg endringene derfra. Kjør den feilede kjøringen på nytt.
- Push-main og scheduled deler concurrency-gruppen `gradle-main-deploy`. GitHub lar bare én kjøring vente i køen. Kommer det en ny, avbrytes den som venter, og endringene i den blir ikke deployet.

## Kjøre lokalt

```sh
# Moduler påvirket av branchen din
./gradlew -q paavirkedeModuler

# Finne app deploy config
./gradlew -q appDeployConfig --app=mottak --miljo=dev-gcp

# Testene for build-logic
./gradlew :build-logic:test
```
