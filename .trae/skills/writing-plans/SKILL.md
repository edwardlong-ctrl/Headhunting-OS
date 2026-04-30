---
name: writing-plans
description: Use when a scoped feature or change is approved and the agent must break it into 5-10 concrete implementation tasks with file paths, impact areas, validation steps, and delivery order
---

# Writing Plans

## Description

Use this skill after the problem is clarified and before editing code. The goal is to convert an approved request into a buildable plan with explicit scope, sequencing, and verification.

In this project, plans must stay aligned with the production-first spec and must not treat AI output as product truth.

## When To Use

Use this skill when:

- A feature has been clarified and is ready for execution.
- A bug fix spans multiple layers or files.
- A workflow, permission boundary, or data model change needs ordered tasks.
- The user asks for an implementation plan, checklist, or rollout sequence.

## Instructions

1. Re-read the approved problem statement and keep the scope tight.
2. Anchor the plan to the current product spec and existing architecture, especially:
   - `v2.1` as source of truth
   - `v2.0` UI preservation
   - modular monolith structure
   - PostgreSQL and backend as truth owners
3. Break the work into `5-10` tasks whenever practical.
4. For each task, write:
   - purpose
   - exact files or directories likely affected
   - expected impact
   - verification method
5. Sequence tasks so that contracts, guards, and domain rules come before UI polish.
6. Explicitly call out cross-cutting constraints when relevant:
   - `WorkflowEvent` creation
   - claim vs fact boundary
   - client-safe redaction
   - consent / disclosure gates
   - provenance-weighted matching
7. Keep tasks small enough to execute safely and review independently.
8. Avoid placeholder tasks such as "update backend" or "fix frontend as needed".
9. Prefer tasks that map to a coherent unit of change:
   - schema or contract
   - domain model or service
   - API boundary
   - UI state and presentation
   - tests or verification
10. End with explicit validation coverage for behavior, not just compilation.

## Task Quality Standard

Each task should answer all of these:

- What is being changed?
- Where will it change?
- Why is it needed?
- How will we know it worked?
- What project rule must not be broken?

## Output Format

Produce:

- a brief `范围摘要`
- `前置约束`
- numbered tasks
- a final `验证清单`

For each numbered task, use this structure:

`任务标题`
`目标`
`涉及文件`
`影响范围`
`验证方式`

## Project-Specific Planning Rules

- Put service-layer gates before any UI that depends on them.
- If candidate-visible or client-visible content changes, include anonymity and disclosure checks.
- If a state transition changes, include `WorkflowEvent` verification.
- If AI extraction or write-back changes, include claim-ledger and canonical write-gate review.
- If matching changes, include evidence coverage, score confidence, and provenance checks.
- If the scope is too large for 10 tasks, split into phases instead of producing vague mega-tasks.
