---
name: requesting-code-review
description: Use when implementation work is complete or a meaningful checkpoint is reached and the agent must review for regressions, permission mistakes, data consistency issues, missing tests, and violations of project rules before handoff or merge
---

# Requesting Code Review

## Description

Use this skill before declaring work done. The review should focus on correctness, risk, regressions, and compliance with the AI Headhunting Transaction OS rules, not on superficial praise.

This skill is especially important in this project because many changes can silently break privacy, workflow integrity, or truth ownership even when code looks clean.

## When To Use

Use this skill when:

- A feature is complete.
- A bug fix is ready for handoff.
- A risky refactor or schema change is finished.
- Work touched permissions, workflow, AI write-back, shortlist generation, matching, consent, or disclosure.
- The user asks for a review.

## Instructions

1. Review the actual diff or changed files, not just the summary.
2. Evaluate the change against:
   - requested behavior
   - current product spec
   - nearby architecture and contracts
   - project hard rules
3. Prioritize findings by risk and user impact.
4. Look for these issue classes first:
   - broken permission boundary
   - client-safe data leakage
   - AI write-back bypassing service gates
   - missing `WorkflowEvent` on key transition
   - incorrect verification status escalation
   - candidate identity or consent leakage
   - evidence-free matching claims
   - missing regression coverage
   - inconsistent API or schema behavior
5. Call out gaps in tests when risk justifies them.
6. If no defects are found, say so explicitly, but still mention residual risk or verification gaps.
7. Keep the review concrete and reference the relevant file or symbol.

## Review Checklist

- Does the change preserve `backend owns truth`?
- Does AI remain within `claim` rather than `fact` authority?
- Are high-risk actions still gated by service-layer approval?
- Are client-visible candidate views still anonymized correctly?
- Are `WorkflowEvent` and audit obligations preserved?
- Does the change preserve the unified Consultant portal model?
- Are matching outputs still evidence-backed and properly bounded?
- Are tests or validation steps sufficient for the change risk?

## Output Format

Return results in this order:

1. `发现`
2. `开放问题或假设`
3. `变更总结`

Under `发现`, list issues ordered by severity:

- `严重` for release-blocking or trust-breaking defects
- `重要` for correctness or regression risks that should be fixed before merge
- `一般` for lower-risk improvements

If there are no findings, write:

- `未发现明确缺陷`

Then add any residual risk.

## Review Standard For This Project

- A clean diff is not enough if anonymity, consent, or workflow guarantees weakened.
- UI-only changes still require data-boundary scrutiny if client or candidate views changed.
- A passing build is not enough if canonical write rules, review states, or audit events were bypassed.
- Prefer one precise finding over many vague comments.
