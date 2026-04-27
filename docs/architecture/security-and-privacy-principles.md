# Security and Privacy Principles

## Production Principles

- Backend owns truth. Frontend, AI tools, MCP tools, and external workflow systems cannot write confirmed facts directly.
- PostgreSQL is the target source of truth for canonical records, claims, workflow events, consent, disclosure, audit, and future AI task run records.
- AI output is a claim until reviewed through the future service-layer gates.
- Client surfaces must not receive raw Candidate objects before unlock and disclosure rules exist.
- Every future key state transition must create a WorkflowEvent.
- Consent and disclosure records must be versioned and auditable.

## Access Boundaries

- Owner can inspect business and risk state according to future role policy.
- Consultant is the unified operational portal and may later access full candidate details under policy.
- Client must stay on client-safe, anonymous, redacted candidate information before unlock.
- Candidate must only access their own profile, confirmations, opportunities, and consent surfaces.
- Admin/System owns future governance, policy, audit, retention, and schema configuration.

## Secret Handling

- Secrets must not be committed.
- `.env` files are ignored by default.
- Local Compose defaults are development-only placeholders, not production credentials.
- Provider keys and model configuration are intentionally absent in Task 1.

## Audit and Governance

Future implementation must capture:

- Actor and role
- Entity and entity version
- Action name
- Before and after state
- Reason
- Source
- AI involvement
- Timestamp

Task 1 only records this requirement in documentation. It does not implement audit logic.
