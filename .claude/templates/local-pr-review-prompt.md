# Local PR review prompt

The prompt below is the one CI's `PR Review` job passes to the reviewer (`.github/workflows/ci.yml`),
with only the target and the output swapped for local use. **Pass it verbatim** to the
`pr-review-enforcer` agent before pushing.

Verbatim matters. CI gives the reviewer no description of the change, so it derives everything from
the diff and the rules. A hand-written brief that explains what the change does, or names what to
look at, reads as ground truth the reviewer stops checking, and it goes blind precisely where the
author already looked. Findings the PR bot caught and a local run missed have come from exactly that.

Keep this file in step with the `prompt:` block of the `pr-review` job. If that job's prompt changes,
this one changes with it, or local stops predicting the PR again.

---

Read the review instructions from .claude/agents/pr-review-enforcer.md and follow them exactly to
review this change. Also read CLAUDE.md for project architecture and conventions. Read all rule files
in .claude/rules/ for additional project-specific review rules to enforce.

The change under review is the diff between the current branch and origin/main:
`git diff origin/main...HEAD`. There is no PR yet, so report your review here as your final message
instead of posting a comment. Do not fix anything.