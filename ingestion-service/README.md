# IngestionServiceApp

## Overview

Parses and cleans `wards-outdated.csv`, a messy legacy export of wards, wings, and specialist departments data, and is the
first stop in the HealthSafe pipeline. Independent Maven module, no parent pom.

Part of the [HealthSafe](../README.md) project.

REST: exposes the cleaned records for `ward-service` (`../ward-service`) to
consume — see [Integration contracts](../README.md#integration-contracts) in the
root README for the endpoint shape.

## Example: one row cleaned

Input (`wards-outdated.csv`, row 6):

```
w-05,east wing ,PAEDIATRICS,five
```

Expected shape after cleaning (exact field names are up to you — this illustrates
the *kind* of transform expected, not a fixed schema to match exactly):

```json
{
  "wardId": "W-05",
  "wing": "East Wing",
  "department": "Paediatrics",
  "bedsAvailable": null,
  "notes": "bedsAvailable was non-numeric ('five') — flagged for follow-up"
}
```

Note this row is also a near-duplicate of `W-05` two rows above it (same real ward,
different ID casing and field values) — deciding how to merge or flag duplicates
like this is part of the exercise.

## Known data issues

`wards-outdated.csv` is deliberately messy — cleaning it is the point of this service. Look
out for (and handle) at least:

- **Inconsistent casing** in IDs, names, and status/category values (`Active` /
  `active` / `ACTIVE`)
- **Padding** — leading/trailing spaces, and the occasional double space, inside
  fields
- **Duplicate records** for the same real-world entity, written with a different ID
  casing/format and/or slightly different field values
- **Inconsistent date formats** (`YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, one- and
  two-digit months/days) and outright invalid dates
- **Missing / placeholder values** — blank fields, `N/A`, `n/a`, `TBD`, `unknown`,
  `-`, `NaN`
- **Invalid or non-numeric values** in numeric columns (negative counts, spelled-out
  numbers, unrealistic values)
- **Inconsistent boolean/flag representations** (`Y`/`N`, `yes`/`no`, `1`/`0`,
  `true`/`FALSE`)
- **Naming/spelling variants** for the same thing (e.g. regional spelling
  differences, synonyms)

## Project structure

```
ingestion-service/
├── pom.xml
└── src/main/
    ├── java/co/wethinkcode/healthsafe/IngestionServiceApp.java
    └── resources/wards-outdated.csv
```

## Build

```
mvn package
```

## Run

```
java -jar target/ingestion-service.jar
```

Listens on port `7030`. Currently just exposes `/health` — the actual CSV
parsing/cleaning logic is a TODO.

## Test

No automated tests yet. Manually verify it's up:

```
curl http://localhost:7030/health   # -> OK
```

To add real tests, add JUnit 5 + the Surefire plugin to `pom.xml`, put tests under
`src/test/java/co/wethinkcode/healthsafe/`, and run `mvn test`.


# IngestionServiceApp

## Overview

Parses and cleans `wards-outdated.csv`, a messy legacy export of wards, wings, and specialist departments data. It is the first stop in the HealthSafe pipeline.

Part of the [HealthSafe](../README.md) project.

REST: exposes the cleaned records via `GET /wards` for `ward-service` to consume.

## What it does

1. Reads `src/main/resources/wards-outdated.csv` line by line
2. Skips the header row
3. Cleans each row:
  - **Trim** leading/trailing spaces and **collapse** double spaces
  - **Uppercase** ward IDs (`w-02` → `W-02`)
  - **Title case** wing names (`east wing` → `East Wing`)
  - **Title case** department names (`paediatrics` → `Paediatrics`)
  - **Parse** beds available — returns `null` + a note for invalid values
4. **Detects duplicates** using a `HashMap` keyed on uppercased ward ID
5. Returns a list of `Ward` objects as JSON

## Known data issues handled

- Inconsistent casing (`Cardiology` / `cardiology` / `CARDIOLOGY`)
- Padding (leading/trailing spaces, double spaces)
- Duplicate records (e.g. `W-05` vs `w-05`)
- Placeholder values (`N/A`, `TBD`, `unknown`, `-`, `NaN`, `full`)
- Non-numeric values in numeric columns (`five`, `full`)
- Out-of-range numeric values (negative counts, unrealistic values > 1000)

## Endpoints

| Endpoint | Method | What It Returns |
|----------|--------|-----------------|
| `/health` | GET | `OK` |
| `/wards` | GET | Cleaned list of `Ward` objects as JSON |

## Ward model

```json
{
  "wardId": "W-01",
  "wing": "East Wing",
  "department": "Cardiology",
  "bedsAvailable": 3,
  "notes": null
}