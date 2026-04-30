#!/bin/bash
# PreToolUse hook — blocks dangerous operations before execution
# Exit code 0 = allow, 2 = block (stderr shown to AI as feedback)

INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // ""')
TOOL=$(echo "$INPUT" | jq -r '.tool_name // ""')

# Only check Bash commands
if [ "$TOOL" != "Bash" ] || [ -z "$CMD" ]; then
  exit 0
fi

# Block: force push (the git subcommand, not text in commit messages)
if echo "$CMD" | grep -qiE '(^|[;&|]\s*)git\s+push\s+.*(--force|-f)'; then
  echo "BLOCKED: Force push is not allowed." >&2
  exit 2
fi

# Block: push directly to main/master
if echo "$CMD" | grep -qiE '(^|[;&|]\s*)git\s+push\s+.*\b(main|master)\b'; then
  if ! echo "$CMD" | grep -qiE 'pull.request|pr'; then
    echo "BLOCKED: Do not push directly to main/master. Create a PR." >&2
    exit 2
  fi
fi

# Block: merge into main/master
if echo "$CMD" | grep -qiE '(^|[;&|]\s*)git\s+merge\s+.*\b(main|master)\b'; then
  echo "BLOCKED: Do not merge to main. Merge is done via separate PR review." >&2
  exit 2
fi

# Block: git reset --hard (the git subcommand, not text in commit messages)
if echo "$CMD" | grep -qiE '(^|[;&|]\s*)git\s+reset\s+--hard'; then
  echo "BLOCKED: git reset --hard is destructive." >&2
  exit 2
fi

# Block: broad rm -rf (allow safe cleanup targets)
if echo "$CMD" | grep -qiE '(^|[;&|]\s*)\brm\b\s+.*-rf' && ! echo "$CMD" | grep -qiE 'node_modules|target|\.npm-cache|dist|\.next|coverage'; then
  echo "BLOCKED: rm -rf may be destructive. If intentional, explain why." >&2
  exit 2
fi

exit 0
