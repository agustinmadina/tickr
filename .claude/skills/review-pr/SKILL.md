---
name: review-pr
description: Review a GitHub pull request for code quality, architecture compliance, and KMP best practices. Use when asked to review a PR.
argument-hint: "[pr-number-or-url]"
disable-model-invocation: true
allowed-tools: Bash(gh *), Read, Grep, Glob
---

# Pull Request Review

Review PR $ARGUMENTS thoroughly against the project's coding standards and clean architecture principles.

## PR Context

- **PR Details:** !`gh pr view $0 --json title,body,author,baseRefName,headRefName,additions,deletions,changedFiles`
- **Changed Files:** !`gh pr diff $0 --name-only`

## Review Process

### 1. Understand the PR
- Read the PR title, description, and linked issues
- Understand the intent and scope of changes

### 2. Fetch and analyze the full diff
Run `gh pr diff $0` and review every changed file carefully.

### 3. Review Checklist

Read the shared review checklist at `.claude/skills/review-checklist.md`. It maps every check to its rule file and severity level.

Evaluate the PR against each category in the checklist. Only flag genuine issues — skip categories not relevant to the changes. For any violation found, read the full rule file referenced in the checklist to confirm severity and check the examples.

### 4. Output Format

Structure your review as follows:

```
## PR Review: <PR title>

**PR:** #$0 | **Author:** <author> | **Base:** <base> <- <head>
**Scope:** +<additions> -<deletions> across <changedFiles> files

### Summary
<1-2 sentence summary of what this PR does>

### Verdict: <APPROVE | REQUEST_CHANGES | COMMENT>

### Issues Found
<List issues grouped by severity>

#### Critical (must fix before merge)
- [ ] <issue description> — `file:line`

#### Suggestions (recommended improvements)
- [ ] <suggestion> — `file:line`

#### Nits (optional, minor style)
- [ ] <nit> — `file:line`

### What Looks Good
<Brief positive feedback on well-done aspects>
```

If there are no issues, keep the review concise and approve.
