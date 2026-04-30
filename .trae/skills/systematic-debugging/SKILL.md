---
name: systematic-debugging
description: Use when a bug, regression, integration failure, or unclear runtime issue must be diagnosed through evidence, reproduction, root-cause isolation, and verification instead of guesswork
---

# Systematic Debugging

## Description

Use this skill for production bugs, local regressions, API integration failures, data inconsistencies, and any issue where the cause is not yet proven.

The goal is to replace intuition-driven patching with evidence-driven debugging.

For this project, debugging must preserve the truth model:

- backend owns truth
- AI outputs claims, not facts
- state transitions require auditability
- client-safe boundaries must remain intact

## When To Use

Use this skill when:

- The behavior is wrong but the cause is unknown.
- The bug appears in API, UI, database, workflow, or AI integration layers.
- A change caused an unexpected regression.
- There is a mismatch between displayed state and backend state.
- A privacy, consent, disclosure, or authorization boundary may be broken.

## Instructions

1. State the observed symptom precisely.
2. Define expected behavior from spec, contract, or existing approved behavior.
3. Reproduce the issue in the smallest reliable way possible.
4. Gather evidence before editing code:
   - logs
   - failing requests or responses
   - screenshots if relevant
   - stack traces
   - schema or contract mismatches
   - workflow state before and after
5. Identify which layer owns the problem:
   - UI rendering
   - API contract
   - domain service
   - persistence
   - permissions
   - AI orchestration or transformation
6. Form hypotheses and test them one by one. Do not patch multiple possible causes at once.
7. For each hypothesis, record:
   - why it is plausible
   - how it will be verified or falsified
   - result
8. Check project-specific failure classes early:
   - canonical write gate bypass
   - missing `WorkflowEvent`
   - wrong verification status
   - client-safe projection leak
   - state machine transition bug
   - evidence or provenance downgrade
   - stale or conflicting candidate field handling
9. Fix only after the root cause is demonstrated.
10. Verify the fix with the same reproduction path and one regression-oriented check nearby.
11. If you cannot prove the root cause, do not claim the issue is fixed.

## Debugging Output Format

Produce a compact report with:

- `现象`
- `预期行为`
- `复现方式`
- `证据`
- `假设与排查结果`
- `根因`
- `修复方案`
- `验证结果`
- `剩余风险`

## Hard Rules

- Never say "probably fixed" without verification.
- Never convert an inferred explanation into a claimed root cause without evidence.
- Never patch around a symptom if the real issue could affect consent, disclosure, privacy, or workflow truth.
- Never treat client-visible projection bugs as cosmetic until access boundaries are verified.

## Project-Specific Checks

- If candidate data appears in the wrong portal, verify role and field-level access policy first.
- If shortlist behavior is wrong, verify anonymization and re-identification controls.
- If workflow status is wrong, verify `before_state`, `after_state`, and `WorkflowEvent`.
- If AI-generated data looks wrong, verify whether the bug is in claim generation, review status, or canonical write-back gating.
