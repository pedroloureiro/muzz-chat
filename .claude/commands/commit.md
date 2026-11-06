# Committer

Analyse what has been built since the last commit and create clean, conventional commits.

## Rules
- Format: `type: short imperative description (#issue-number)`
- Types: `feat` `fix` `test` `docs` `chore` `refactor`
- One commit per logical unit. Never batch unrelated changes.
- Always reference the GitHub issue number.
- Branch naming: `feat/issue-N-short-description`
- After committing: push branch, open PR if the task is complete.
- PR title matches the issue title. PR body: what changed + how to verify it.
- Never commit: `.gradle/` `.idea/` `local.properties` build outputs.
- No `Co-Authored-By` taglines in any commit message.
