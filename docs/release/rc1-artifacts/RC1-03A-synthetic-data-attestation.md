# RC1-03A Synthetic Data Attestation

| Field | Value |
| --- | --- |
| RC1 run id | not-recorded |
| Dataset source | Deterministic pilot dataset from `scripts/pilot-data.sh`, the pilot data CLI, and `docs/pilot/task-38-pilot-scenario.md` |
| Datasource shape | isolated-container |
| Validator command | `npm run pilot:data:validate` |
| Validator result | exit 0; `valid=true`; privacy checks passed; seeded account checks passed; workflow/audit seed checks passed |
| Operator | Codex |
| Statement | No real candidate, client, customer, or production data was intentionally used for this RC1 run. |
| Screenshot/API redaction policy | Do not capture secrets, full connection strings, credential values, raw PII, private keys, or real identity/contact data. Pre-disclosure screenshots must avoid identity/contact fields. |
