# Task 38 Pilot Seed Data Scenario

This document describes the deterministic `semiconductor-pilot-v1` seed used for controlled local pilot walkthroughs.

## Commands

Run from the repository root:

```sh
npm run pilot:data:rebuild
npm run pilot:data:validate
npm run pilot:data:export
RTO_PILOT_DATA_ALLOW_RESET=true npm run pilot:data:reset
```

The CLI uses local Docker Compose PostgreSQL defaults unless overridden:

- `RTO_PILOT_DATA_JDBC_URL`
- `RTO_PILOT_DATA_DB_USER`
- `RTO_PILOT_DATA_DB_PASSWORD`

Default local connection:

```text
jdbc:postgresql://localhost:5432/recruiting_os
user: recruiting_os
password: recruiting_os_local_password
```

## Seeded Accounts

All accounts use password `PilotData38!` and reserved `.example.test` email addresses.

| Portal | Email |
| --- | --- |
| Owner | `owner@pilot.example.test` |
| Consultant | `consultant@pilot.example.test` |
| Client | `client@pilot.example.test` |
| Candidate | `candidate@pilot.example.test` |
| Admin | `admin@pilot.example.test` |

## Seed Shape

- 1 deterministic pilot organization.
- 5 active semiconductor jobs.
- 3 under-review semiconductor jobs.
- 4 fictional client companies.
- 75 synthetic talent-pool candidate records.
- At least 83 source documents: one synthetic resume per candidate and one synthetic job intake per job.

The dataset intentionally creates no pre-seeded shortlists, consent/disclosure records, or canonical-write attempts. Those states must be produced through normal product workflows during pilot exercises.

## Walkthrough

1. Start PostgreSQL and run `npm run pilot:data:rebuild`.
2. Log in as `consultant@pilot.example.test`.
3. Review the consultant candidate list and job list.
4. Open an active semiconductor job and generate/review matches using the normal matching surface.
5. Build a shortlist from seeded candidate records through the normal consultant shortlist builder.
6. Use client/candidate/consultant flows for unlock and feedback rather than editing seeded database state.
7. Run `npm run pilot:data:validate` before and after the walkthrough to confirm the seed baseline remains privacy-safe.

## Boundaries

- No real personal data is present.
- No public profile URLs are present.
- No real high-signal employer names are present.
- Reset is destructive and requires `RTO_PILOT_DATA_ALLOW_RESET=true`.
- The seed is a pilot baseline, not a pilot-readiness claim; Task 42 remains the end-to-end pilot acceptance gate.
