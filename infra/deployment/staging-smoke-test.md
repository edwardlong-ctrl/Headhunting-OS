# Task 39 Staging Smoke Test

This smoke test proves only that a deployment baseline exists. It is not a production-ready claim.

## Preconditions

- PostgreSQL migrations have run.
- Task 38 pilot data has been imported and validated.
- The frontend is reachable through the staging URL.
- The backend `/health` endpoint returns `UP`.

## Smoke Path

1. Open the staging frontend.
2. Sign in as `consultant@pilot.example.test` using the Task 38 pilot password.
3. Confirm the Consultant portal opens as one unified workspace.
4. Open the consultant intake workflow.
5. Upload or select pilot source material through the normal intake surface.
6. Confirm the intake review queue shows evidence-backed clean-fact candidates.
7. Confirm no client portal route can read raw Candidate or CandidateProfile records before unlock/disclosure.

## Result Label

Use `deployment baseline exists` when this smoke path passes. Do not label the system `production-ready` until a real managed environment, HTTPS/domain, backup restore, and incident process have all been verified.
