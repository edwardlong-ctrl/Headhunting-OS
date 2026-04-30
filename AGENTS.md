# AGENTS.md

## Product Source of Truth

Before any product, architecture, UI, AI, workflow, data model, or governance change, read these files first:

- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md` when checking UI preservation or v2.0 baseline content

v2.1 is the current product source of truth. v2.0 remains available as the historical baseline and UI reference, and its UI and portal definitions must not be deleted, compressed, or replaced.

## Execution Watchdog

When launching a web server, watch process, daemon, or any command whose health is not immediately obvious, do not passively wait without inspection.

- Before launch, state the expected startup signal, such as first output, ready marker, or preview URL.
- For non-blocking commands, use staged status checks and classify the result as healthy, still booting, crashed, blocked, waiting for input, or indeterminate.
- If a command appears silent, stalled, or unhealthy, invoke the `handling-stuck-commands` skill and move into inspect or debug mode instead of continuing to wait.
- Restart at most once without new evidence. After that, switch to evidence-driven debugging.
