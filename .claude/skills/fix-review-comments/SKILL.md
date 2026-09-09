---
name: fix-review-comments
description: Fetch unresolved review comments from a GitHub PR and apply fixes locally. Marks fixed threads as resolved.
argument-hint: "[pr-number]"
disable-model-invocation: true
allowed-tools: Bash(gh *), Bash(git *), Bash(./gradlew *), Task, Read, Write, Glob, Grep, Edit
---

# Fix Review Comments

Fetch unresolved PR review comments for PR #$ARGUMENTS, apply fixes locally, and mark resolved threads on GitHub.

## Step 1: Validate Input and Gather PR Context

If `$ARGUMENTS` is empty or not a valid PR number, ask the user for the PR number.

Run these in parallel:

1. `gh pr view $0 --json title,body,author,baseRefName,headRefName,headRefOid,number` — PR metadata
2. `gh repo view --json nameWithOwner -q .nameWithOwner` — Get `{owner}/{repo}`
3. `git branch --show-current` — Current local branch

Verify the local branch matches the PR's `headRefName`. If not, warn the user:

> You are on branch `<local>` but PR #$0 is on branch `<remote>`. Switch to the PR branch first:
> ```
> git checkout <headRefName> && git pull
> ```

If the branch matches, ensure it's up to date:
```bash
git pull --ff-only
```

Extract `{owner}/{repo}` from the `nameWithOwner` field (e.g., `example/your-repo` -> owner=`example`, repo=`your-repo`).

## Step 2: Fetch Unresolved Review Comments

Fetch all review comments:

```bash
# Inline review comments (attached to diff lines)
gh api repos/{owner}/{repo}/pulls/$0/comments --paginate

# Top-level PR conversation comments
gh api repos/{owner}/{repo}/issues/$0/comments --paginate
```

Filter out comments that should be skipped:

1. **Already processed** — Comments with a `eyes` (👀) reaction (check the `reactions` field or fetch reactions per comment)
2. **Self-comments** — Comments authored by the current user (`gh api user -q .login`)
3. **Bot comments** — Comments from known bots (type `Bot` in the author field)

If **zero** unresolved comments remain after filtering:

> No unresolved review comments on PR #$0. Nothing to fix.

Stop here.

## Step 3: Synthesize into Actionable Report

Launch the `pr-review-enforcer` agent in **synthesis mode** via the Task tool:

```
Prompt: "Run in Review Synthesis Mode for PR #$0. Collect all outstanding review comments, classify them by resolution status and actionability, and produce the structured Review Synthesis report. The repo is {owner}/{repo}."
```

The synthesis report will include:
- **Unresolved - Blocking**: Must-fix code changes
- **Unresolved - Changes Requested**: Should-fix code changes
- **Unresolved - Questions**: Need human response (not code fixes)
- **Unresolved - Suggestions**: Optional improvements
- Comment IDs for each item (needed for tracking in Step 8)

## Step 4: Present Summary to User

Display the synthesis report to the user. Highlight:

1. **Actionable fixes** (Blocking + Changes Requested) — these will be auto-fixed
2. **Questions** — present these to the user for manual response
3. **Suggestions** — mention count but note they won't be auto-fixed unless requested

Example output:

```
## Review Comments Summary for PR #$0

**Actionable fixes**: 5 (2 blocking, 3 change-requested)
**Questions for you**: 2
**Suggestions**: 1

### Questions (need your response — not auto-fixable)
1. `path/to/file.kt:42` — @reviewer: "Should this use sealed class or enum?"
2. `path/to/file.kt:88` — @reviewer: "Is this the intended error behavior?"

Proceeding to fix 5 actionable comments...
```

If there are **zero actionable** comments (only questions/suggestions), stop here and tell the user.

If there are questions, show them but proceed with fixing the actionable items.

## Step 5: Apply Fixes

Reformat the synthesis report into a **Local Review Report** structure that the `local-fix` agent expects:

```
## Local Review Report

**Branch**: <headRefName>
**Files changed**: <count of files with actionable comments>
**Verdict**: NEEDS_FIXES

### Blocking Issues (must fix before PR)
<Map "Unresolved - Blocking" items here, preserving file:line, description, and fix instructions>

### Changes Requested (should fix before PR)
<Map "Unresolved - Changes Requested" items here>

### Suggestions (nice to have)
<Map "Unresolved - Suggestions" items here — local-fix will skip these by default>
```

Launch the `local-fix` agent via the Task tool with this reformatted report.

The local-fix agent will:
- Apply minimal, surgical fixes for each actionable finding
- Run `./gradlew ktlintFormat` if lint issues are involved
- Produce a Fix Report listing what was fixed, skipped, and what needs guidance

## Step 6: Local Review Loop (max 3 iterations)

After the local-fix agent completes, run a full local review with the `pr-review-enforcer` agent in **local mode** to verify the fixes are actually correct and no new issues were introduced:

```
Launch pr-review-enforcer agent:
Prompt: "Run in Local Review Mode. Review the locally modified files on branch <headRefName> against our clean architecture standards and code quality rules. Produce a structured Local Review Report."
```

**If the local review passes (verdict: APPROVED or no blocking/change-requested findings):**
→ Proceed to Step 7 (mark comments on GitHub).

**If the local review finds remaining issues:**
→ Launch the `local-fix` agent with the new findings.
→ Re-run the `pr-review-enforcer` local review.
→ Repeat up to **3 total iterations** (initial fix + 2 re-checks).

After 3 failed iterations, do NOT mark comments as resolved. Instead, report to the user:

```
## ⚠️ Fix loop exhausted (3 iterations)

The following issues could not be resolved automatically and require your attention:
<list remaining findings>

The review comment threads have NOT been marked as resolved on GitHub.
```

Stop here and wait for user input.

## Step 7: Mark Processed Comments on GitHub

Only reach this step after the local review in Step 6 passes.

For each comment from the synthesis report, mark it as processed.

### Get thread IDs (required for resolving)

The `resolveReviewThread` GraphQL mutation requires the **thread** node ID (`PRRT_...`), not the comment node ID (`PRRC_...`). Fetch thread IDs by matching on comment `databaseId`:

```bash
gh api graphql -f query='{
  repository(owner: "{owner}", name: "{repo}") {
    pullRequest(number: $0) {
      reviewThreads(first: 50) {
        nodes {
          id
          isResolved
          comments(first: 1) { nodes { databaseId } }
        }
      }
    }
  }
}'
```

Match each comment ID from the synthesis report to a `PRRT_` thread ID using the `databaseId` field.

### Fixed comments (code change applied)

Add 👀 reaction AND resolve the thread:

```bash
# Add eyes reaction
gh api repos/{owner}/{repo}/pulls/comments/{comment_id}/reactions -f content="eyes"

# Resolve the thread using the PRRT_ thread node ID
gh api graphql -f query='mutation { resolveReviewThread(input: { threadId: "{thread_node_id}" }) { thread { isResolved } } }'
```

### Skipped / needs-guidance comments

Add 👀 reaction only (leave thread open for human attention):

```bash
gh api repos/{owner}/{repo}/pulls/comments/{comment_id}/reactions -f content="eyes"
```

### Top-level issue comments (not inline)

For top-level comments (fetched from `issues/$0/comments`), only add the reaction — they don't have review threads to resolve:

```bash
gh api repos/{owner}/{repo}/issues/comments/{comment_id}/reactions -f content="eyes"
```

**Important**: Only resolve threads where the code fix was actually applied. Do NOT resolve threads for questions, skipped items, or needs-guidance items.

## Step 8: Output Summary

Display a final report:

```
## Fix Review Comments: PR #$0

**Fixed**: <count> comments
**Skipped**: <count> comments (suggestions or not actionable)
**Needs guidance**: <count> comments (ambiguous, requires human decision)
**Questions**: <count> (presented above — need your response)

### Files Changed
- path/to/file1.kt
- path/to/file2.kt

### Next Steps
1. Review the changes: `git diff`
2. Commit: use `/commit`
3. Push: `git push`
```

Do NOT auto-commit or auto-push. The user decides when to commit and push.

## Rules

- NEVER auto-commit or auto-push changes
- NEVER resolve threads for comments that weren't actually fixed in code
- NEVER mark a comment as resolved before the local review (Step 6) passes
- ALWAYS add 👀 reaction to every processed comment (fixed or not) to prevent re-processing
- ALWAYS fetch `PRRT_` thread node IDs via GraphQL reviewThreads query — do NOT use `PRRC_` comment node IDs for `resolveReviewThread`
- If the local-fix agent reports "needs guidance" items, present them clearly to the user
- For top-level issue comments (not inline review comments), use the `issues/comments/{id}/reactions` endpoint, not `pulls/comments/{id}/reactions`
