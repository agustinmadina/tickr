---
name: work-on-pr
description: Switch to (or create) a git worktree for a GitHub PR and start working on it. Accepts a PR URL or number.
argument-hint: "<pr-url-or-number>"
disable-model-invocation: true
allowed-tools: Bash(git *), Bash(gh *), Bash(pwd), Bash(cd *), Bash(ls *), Bash(mkdir *), Read, Glob, Grep
---

# Work on PR

Switch to a local worktree for PR `$ARGUMENTS`. If no worktree exists, create one.

## Step 1: Parse the input

Determine what was provided:

- **Full URL**: `https://github.com/your-org/your-repo/pull/42` → extract PR number `42`
- **PR number**: `42` or `#42` → use directly

PR_INPUT: `$0`

## Step 2: Fetch PR metadata

```bash
gh pr view PR_NUMBER --json number,title,headRefName,baseRefName,state,body,additions,deletions,changedFiles
```

Extract:
- **PR number**
- **Title**
- **Head branch** (the PR's source branch)
- **Base branch** (usually `main`)
- **State** (OPEN, CLOSED, MERGED)

If the PR is MERGED or CLOSED, warn the user and ask if they still want to proceed.

## Step 3: Derive worktree name

Extract a ticket ID from the branch name if present:

- Branch `TICKET-145-add-auth-flow` → TICKET_ID: `TICKET-145`, worktree name: `your-repo-TICKET-145`
- Branch `fix/some-bug` → no ticket, worktree name: `your-repo-pr-PR_NUMBER`
- Branch `add-offline-cache` → no ticket (bare slug, no `xxx/` prefix), worktree name: `your-repo-pr-PR_NUMBER`

Rules:
- If branch starts with a ticket pattern (`XXXX-NNN`), use `your-repo-TICKET_ID`
- Otherwise, use `your-repo-pr-PR_NUMBER`

## Step 4: Check for existing worktree

```bash
git worktree list
```

**Check by branch name** — if any worktree is already on the PR's head branch, use it.

**Also check by directory name** — the derived worktree directory may exist.

### If worktree exists

```bash
PARENT_DIR=$(dirname $(git rev-parse --show-toplevel))
cd "$PARENT_DIR/WORKTREE_NAME"
```

Then pull latest changes:
```bash
git pull --rebase origin HEAD_BRANCH
```

### If worktree does NOT exist

```bash
PARENT_DIR=$(dirname $(git rev-parse --show-toplevel))
REPO_NAME=$(basename $(git rev-parse --show-toplevel))
git fetch origin
git worktree add "$PARENT_DIR/WORKTREE_NAME" "origin/HEAD_BRANCH"
cd "$PARENT_DIR/WORKTREE_NAME"
```

This checks out the PR branch as-is (not creating a new branch).

## Step 5: Verify state

Confirm the worktree is on the correct branch:

```bash
git branch --show-current
git log --oneline -5
```

## Step 6: Report

Output a summary:

```
## Ready to work on PR #NUMBER

**Title:** PR title
**Branch:** head-branch ← base-branch
**Worktree:** /path/to/worktree
**State:** OPEN
**Scope:** +additions -deletions across N files

Recent commits:
- commit1
- commit2
- ...
```

The user is now in the worktree and ready to work. Do NOT start making changes — wait for instructions.
