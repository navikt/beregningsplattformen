# AGENTS.md — beregningsplattformen

Denne applikasjonen mottar og beregner oppdrag for NAVs økonomiplattform. Den består av flere moduler, som sammarbeider om å ta ett oppdrag igjennom en beregningsflyt

## Build & Test Commands

```bash
./gradlew test      # Run tests
./gradlew build     # Build
```

## Project Structure

```text
apps/
build-logic/
buildSrc/
docs/
dokumentasjon/
gradle/
libs/
```

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

- Opprett en branch og send endringer som pull request mot `main`. PR-workflowen kjører tester for påvirkede Gradle-moduler; `paavirkedeModuler` finner dem ut fra endringene mot målbranchen. Workflows alene sier ikke om PR-er krever godkjenning eller hvilken merge-metode teamet bruker.
- Push til `main` tester moduler uten deploy, publiserer påvirkede biblioteker med `publiser-bibliotek` og deployer påvirkede apper. Testmiljøer deployes før prodmiljøer. Ved feilet kjøring på `main`: kjør den feilede workflowen på nytt, siden neste push bare tar med sine egne endringer.
- Alle Gradle-apper redeployes fra `main` hver mandag. Manuell deploy kan kjøres fra alle brancher til testmiljø, men bare fra `main` til prodmiljø. Frontenden i `apps/lab/frontend` har en egen workflow for deploy til `dev-gcp` ved relevante push til `main`, ukentlig og manuelt.
- Dependabot-PR-er for frontend-avhengigheter med labelen `javascript` kan merges automatisk når kravene i den delte workflowen og repoets branch-regler er oppfylt. Endringer i GitHub Actions-workflows utløser en egen zizmor-sjekk.
- Nye Gradle-moduler trenger normalt ikke egne workflows. Se `.github/workflows/README.md` for hvordan påvirkede moduler, deploymiljøer og publisering bestemmes.

## Boundaries

### ✅ Always

- Run tests and ktlint after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input
- Use `secureLogger` when logging national identity numbers (fødselsnummer) or other PII — never the standard logger
- Keep this file in sync with the codebase when making changes that affect the information described here

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema
- Creating new Flyway migrations (check `apps/beregningsplattform/module-xxx/src/main/resources/db/migration/` for the next available version number)

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
- Commit or push changes unless the user explicitly asks you to

## Keeping This File Current

- Update this file whenever changes are made that affect the information described here
- Ensure that the guidelines and instructions remain accurate and relevant
