---
name: review-local
description: Review local branch changes (vs main) for code quality, architecture compliance, and KMP best practices. Use when asked to review local/uncommitted changes before opening a PR.
argument-hint: "[base-branch (default: main)]"
disable-model-invocation: true
allowed-tools: Bash(git *), Read, Grep, Glob
---

# Local Branch Review

Review all changes on the current branch compared to the base branch against the project's coding standards and clean architecture principles.

**Base branch**: Use `$ARGUMENTS` if provided, otherwise `main`.

## Step 1: Gather Context

Run these commands to understand the scope of changes:

```bash
git branch --show-current
git log <base>...HEAD --oneline
git diff --stat <base>...HEAD | tail -1
git diff --name-only <base>...HEAD
```

(Replace `<base>` with `$ARGUMENTS` if provided, or `main` if not.)

## Step 2: Fetch and Analyze the Full Diff

```bash
git diff <base>...HEAD
```

For each changed file, also use the Read tool when needed to understand surrounding context.

## Step 3: Review Checklist

Read the shared review checklist at `.claude/skills/review-checklist.md`. It maps every check to its rule file and severity level.

Evaluate the changes against each category in the checklist. Only flag genuine issues — skip categories not relevant to the changes. For any violation found, read the full rule file referenced in the checklist to confirm severity and check the examples.

## Step 4: Output Format

```
## Local Review: <branch name>

**Branch:** <current> ← <base>
**Scope:** +<additions> -<deletions> across <N> files
**Commits:** <list from git log --oneline>

### Summary
<1-2 sentence summary of what these changes do>

### Verdict: <APPROVE | REQUEST_CHANGES | COMMENT>

### Issues Found
<List issues grouped by severity — omit sections with no findings>

#### Blocking (must fix before PR)
- [ ] <issue description> — `file:line`

#### Change Requested (should fix before PR)
- [ ] <issue description> — `file:line`

#### Suggestions (recommended improvements)
- [ ] <suggestion> — `file:line`

#### Nits (optional, minor style)
- [ ] <nit> — `file:line`

### What Looks Good
<Brief positive feedback on well-done aspects>
```

If there are no issues, keep the review concise and approve.
