---
name: brainstorming
description: Use when a new feature, workflow, UI change, AI behavior, data model change, or governance change is still ambiguous and the agent must clarify business goals, actors, boundaries, risks, and acceptance criteria before implementation
---

# Brainstorming

## Description

Use this skill before implementation work begins. The goal is to stop premature coding and force the conversation into a product and workflow clarification pass first.

For this project, brainstorming must respect these standing rules:

- v2.1 is the current product source of truth.
- v2.0 remains the historical UI and portal baseline and must not be deleted, compressed, or replaced.
- Consultant is one unified portal.
- Backend owns truth.
- AI outputs claims, not facts.
- Every key state transition must create `WorkflowEvent`.
- Client must never read raw candidate objects before unlock or disclosure.

## When To Use

Use this skill when:

- The user asks for a new feature or product change.
- The request touches workflow, permissions, AI behavior, data model, UI, or governance.
- The business problem is still vague or underspecified.
- There are multiple possible designs and tradeoffs.
- The request could affect candidate privacy, consent, disclosure, audit, or matching quality.

Do not skip this step just because the request sounds implementation-ready.

## Instructions

1. Restate the requested change in domain language for the AI Headhunting Transaction OS.
2. Identify which portal or actor is involved: `Owner`, `Consultant`, `Client`, `Candidate`, `Admin`.
3. Clarify the business goal before discussing code.
4. Ask focused questions until the following are explicit:
   - target user and portal
   - business objective
   - trigger and entry point
   - data shown, created, or changed
   - workflow states touched
   - permissions and visibility boundaries
   - AI can / must ask / cannot boundaries
   - acceptance criteria
5. Check whether the request conflicts with any hard rules:
   - `backend owns truth`
   - `AI outputs claims, not facts`
   - client anonymity before unlock
   - `WorkflowEvent` requirement for key transitions
   - no deletion or replacement of v2.0 portal and UI definitions
6. Surface risks early, especially:
   - re-identification risk
   - consent or disclosure leakage
   - direct AI write-back into canonical facts
   - matching claims without evidence
   - bulk approval incorrectly becoming verified truth
7. Present the proposed feature shape in a compact structure:
   - problem
   - actors
   - entry points
   - happy path
   - failure path
   - data and state changes
   - guardrails
   - acceptance criteria
8. If the request remains ambiguous, continue asking questions instead of planning implementation.
9. Only move on to planning once the business scope and constraints are concrete enough to build safely.

## Output Format

Prefer a short structured draft with these headings:

- `目标`
- `涉及角色`
- `入口与范围`
- `关键流程`
- `数据与状态变化`
- `风险与边界`
- `验收标准`
- `待确认问题`

## Project-Specific Guardrails

- Treat candidate intent, consent, disclosure, commercial terms, offer, placement, and commission as high-risk topics.
- Never phrase AI inference as confirmed fact.
- Never design client-visible flows that expose raw candidate identity before the proper gate.
- Preserve the unified Consultant portal.
- Prefer workflow-driven operations over free-form chat behavior.
