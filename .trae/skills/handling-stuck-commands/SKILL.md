---
name: handling-stuck-commands
description: Use when starting a web server, watch process, or long-lived command, or when a previously started command appears silent, stalled, unhealthy, or slower than expected and the agent must inspect status, diagnose the reason, and decide whether to continue, retry, restart, or switch to debugging
---

# Handling Stuck Commands

## Description

Use this skill to avoid passive waiting when command execution health is unclear.

This skill is for command supervision, not root-cause debugging of the application itself. Its job is to detect whether a command is healthy, stalled, failed, waiting on external input, or needs escalation into `systematic-debugging`.

## When To Use

Use this skill when:

- launching a dev server, preview server, watch process, or daemon
- starting a non-blocking command and needing to verify it actually came up
- a command that should emit startup output stays silent longer than expected
- a previously started command may have crashed, hung, or become unhealthy
- the user reports that "the command has been running forever", "there is no response", or "Trae is just waiting"

Do not use this skill as a replacement for deeper debugging after the command health has already been diagnosed.

## Command Categories

Classify the command before acting:

- `web_server`: dev server, preview server, API server
- `watch_process`: file watcher, background build watcher
- `daemon_or_service`: database, queue, other persistent service
- `blocking_task`: install, test, build, migration, script expected to finish

## Instructions

1. Before launching a command, state the expected health signal:
   - first output window
   - expected ready marker
   - expected port or success condition
2. For `web_server`, `watch_process`, and `daemon_or_service`, launch non-blocking and perform staged checks.
3. Use short incremental checks instead of one long wait:
   - initial check after `2-5s`
   - second check after another `5-10s` if still unclear
   - final check before deciding the process is stalled
4. On each status check, classify the result into one of:
   - `healthy and ready`
   - `still booting`
   - `crashed with error`
   - `blocked on dependency or environment`
   - `waiting for interactive input`
   - `silent / unhealthy / indeterminate`
5. If the process is healthy, continue normally and expose the preview URL when relevant.
6. If it crashed with a concrete error, summarize the error and switch to `systematic-debugging`.
7. If it is blocked on a likely recoverable issue, choose one safe next action:
   - inspect more logs
   - stop and restart once
   - fix an obvious precondition, then restart
8. Never restart more than once without new evidence.
9. If a command appears to require interactive input, stop and replace it with a non-interactive equivalent whenever possible.
10. For `blocking_task`, do not wait blindly at planning time:
   - prefer narrow validation first
   - verify prerequisites before running expensive commands
   - after completion, inspect the result immediately instead of assuming success
11. If command health cannot be proven after the staged checks, escalate to `systematic-debugging`.

## Failure Strategy

- `No output yet but process still starting`: wait briefly and re-check
- `Known error in logs`: stop passive waiting and debug the actual error
- `Port already in use`: stop or reconfigure and restart once
- `Missing dependency / env var / service`: report the missing prerequisite and debug or fix it
- `Interactive prompt detected`: stop and rerun with explicit non-interactive flags
- `No clear signal after repeated checks`: treat as unhealthy and escalate

## Output Format

Return a compact operational report:

- `命令`
- `预期信号`
- `当前状态`
- `证据`
- `判断`
- `下一步动作`

## Hard Rules

- Never wait indefinitely on a non-blocking command without checking status.
- Never call a command healthy just because time passed.
- Never restart repeatedly without identifying a likely cause.
- Never keep waiting after logs show a concrete failure.
- Never confuse command supervision with application debugging; escalate when needed.

## Examples

- Dev server launched but no URL appears after a few seconds: check status, inspect logs, then decide whether it is still booting or failed.
- Watch process started and then went silent: verify whether it is actually watching or exited early.
- User says "this command has been running forever": inspect status first, then classify before taking action.
