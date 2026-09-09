---
name: create-pr
description: Create a GitHub pull request with a standardized template including summary, what to test, changes, test plan, and Linear ticket link.
argument-hint: "[optional base-branch]"
disable-model-invocation: true
allowed-tools: Bash(git *), Bash(gh *)
---

# Create Pull Request

Create a well-structured GitHub PR for the current branch.

## Step 1: Gather context

Run these in parallel:

1. `git status` — check for uncommitted changes (never use `-uall`)
2. `git branch --show-current` — get current branch
3. `git log --oneline main..HEAD` — all commits since diverging from base
4. `git diff main...HEAD --stat` — summary of all changes
5. `git diff main...HEAD` — full diff for analysis
6. `gh pr list --head $(git branch --show-current) --json number` — check if PR already exists

If `$ARGUMENTS` is provided, use it as the base branch instead of `main`.

## Step 2: Pre-flight checks

### On main branch
If the current branch is `main` (or `master`):
1. **Do NOT create a PR from main.** Instead, create a new feature branch first.
2. Derive a branch name from the staged/unstaged changes:
   - If a ticket ID is visible in the context (e.g. from the conversation or commit messages), use `TICKET-ID-slug` format
   - Otherwise, infer a descriptive slug from the changed files (e.g. `agent-workflow-fixes`, `update-build-config`)
3. Create and switch to the new branch:
   ```bash
   git checkout -b <branch-name>
   ```
4. Stage all relevant changed and untracked files (exclude sensitive files, IDE user state like `.xcuserstate`, and files unrelated to the current work). Ask the user which files to include if unclear.
5. Commit with a descriptive message following the project's commit convention.
6. Push the branch:
   ```bash
   git push -u origin <branch-name>
   ```
7. Continue to Step 3 with the new branch.

### Uncommitted changes
If there are uncommitted changes, warn the user and ask if they want to commit first before creating the PR.

### PR already exists
If a PR already exists for this branch, show the URL and ask the user if they want to update it instead.

### Remote branch
Check if the branch is pushed to remote:
```bash
git ls-remote --heads origin $(git branch --show-current)
```
If not pushed, push it:
```bash
git push -u origin $(git branch --show-current)
```

## Step 3: Extract ticket ID

From the branch name, extract the Linear ticket ID:

- Branch `TICKET-145-add-auth-flow` -> `TICKET-145`
- Branch `NO-TASK-fix-something` -> no ticket
- Use this for the PR title prefix and Linear link

## Step 4: Analyze ALL commits

Read EVERY commit in `git log main..HEAD` — not just the latest one. The PR covers the entire branch, not a single commit.

Understand:
- What was added, changed, or removed
- Which modules were affected
- The overall intent of the work

## Step 5: Compose the PR title

Format: `TICKET-ID Short description` (under 70 characters)

Rules:
- Include ticket ID if present
- Imperative mood: "Add auth flow", not "Added auth flow"
- Concise — details go in the body
- Capitalize first word after ticket ID

Examples:
```
TICKET-145 Add authentication flow with email validation
TICKET-160 Fix token refresh race condition in core-network
NO-TASK Update Kotlin to 2.3.0
```

## Step 6: Compose the PR body

Use this exact template:

```markdown
## Summary
<1-3 bullet points describing WHAT this PR does and WHY>

## What to test
<1-3 plain-language bullets for a non-engineer tester. Omit this section entirely for a
change with no user-visible effect (CI, build config, refactor with no behaviour change).>

## Changes
<Grouped by module/area, each with a brief description>

- **module-name**: What changed in this module
- **module-name**: What changed in this module

## Test Plan
- [ ] Step-by-step verification items
- [ ] Specific scenarios to test
- [ ] Edge cases to check

## Linear
<TICKET-ID link, or "N/A" if no ticket>

---
Generated with [Claude Code](https://claude.ai/code)
```

### Summary guidelines
- Focus on the WHY, not the HOW
- Mention user-facing impact if applicable
- Keep each bullet to one line

### What to test guidelines

**This section is not written for the reviewer — it ships to testers verbatim.** On merge to
`main`, `android_firebase_distribute.yml` parses this section out of the PR **description** and
sends it as the Firebase App Distribution release notes and in the Slack build notification. A
tester reads it in an email, with no access to the diff, the ticket, or the codebase.

**It can ship on a build later than yours, next to other people's.** A build carries every commit
since the last delivery tag, so when several merges land close together the APK that reaches testers
contains all of them, and the notes carry **each** change's section, newest first. So your section
may appear in a build whose head commit is somebody else's, and a delivery cancelled before it
shipped leaves its commits in the next successful build's notes. Write it so it still makes sense
read alongside three others — self-contained, naming its own entry point — and do not assume it is
the only thing on the page.

The description is read through the API, not from the merge commit: this repo's
`squash_merge_commit_message` is `COMMIT_MESSAGES`, so a merge body on `main` is the concatenated
commit messages and never contains the description. The section therefore has to live in the PR
body — putting it only in a commit message would reach the release notes solely through the
fallback path, and only on a direct push.

So it must read standalone:

- **1-3 bullets, plain language.** What a person should open and what they should see.
- **No engineering vocabulary** — no module names, no gradle commands, no ticket keys, no PR
  numbers, no type names. Those belong in Test Plan and Changes.
- **Name the entry point.** "Open Settings → Appearance" beats "check the appearance setting",
  because the tester has to find it before they can test it.
- **Say what correct looks like**, not just what to tap: "…and confirm the list still loads from
  the bundled config" rather than "…and check it works".
- **Mention what to watch for if it regresses**, when there is an obvious blast radius.

The heading must be exactly `## What to test`. The parser is case-insensitive and tolerates a
trailing colon, but it keys on that phrase. A near-miss heading — `## Tests`, `## Testing`,
`## How to test`, `## QA` — is shown in the Slack notification with a note naming the convention,
and raises a warning annotation on the run, but it does **not** count as present: the build ships to
testers with release notes carrying only a headline and a version line. The accepted near-miss
headings are listed in the `for heading in …` loop of that workflow's `Resolve build metadata` step,
which is the source of truth for them. Note the warning's limit: it fires only for the **head**
commit's PR. In a multi-change build an older change's near-miss is dropped with a plain log line
and no annotation, because a PR with no user-visible effect is *required* to omit the section and
annotating every absence would fire on rule-compliant PRs. Getting the heading right the first time
is the only reliable catch. `## Test Plan` is not matched at all, by design — it is
reviewer content, and matching it would render something on nearly every build and drown the nudge.

Omit the section for a change with no user-visible effect. An empty or placeholder section is worse
than none: the workflow omits the block when the section is absent, so testers see a clean note
rather than scaffolding text.

```markdown
## What to test
- Open the app with no network connection — the widget list should still load
- Reconnect, pull to refresh on Home, and confirm the list updates
- Check the wallet widget still opens from Home, since that path also changed
```

Not this — every line is reviewer content that will land in a tester's inbox:

```markdown
## What to test
- [ ] ./gradlew :features:feature-widgets:test
- [ ] Verify WidgetRepositoryImpl falls back when /client/v1/widgets 404s
- [ ] TICKET-1584 acceptance criteria met
```

### Changes guidelines
- Group by module: `core-network`, `feature-auth`, `androidApp`, etc.
- Bold the module name
- One line per module describing what changed
- Skip trivially obvious changes (reformatting, imports)

### Test Plan guidelines

Reviewer-facing, and deliberately separate from What to test above: this one may name modules and
run commands, and none of it reaches testers.

- Actionable checklist items a reviewer can follow
- Include build verification: `./gradlew :module:build`
- Include test run if tests were added: `./gradlew :module:test`
- Include manual verification steps for UI changes
- Be specific: "Tap login button with empty email, verify error shown"

### Linear link
If ticket ID exists, format as:
```
[TICKET-145](https://linear.app/your-workspace/issue/TICKET-145)
```

## Step 7: Create the PR

```bash
gh pr create --title "TITLE" --body "$(cat <<'EOF'
BODY_CONTENT
EOF
)"
```

Use `--base main` (or the branch from `$ARGUMENTS`).

Do NOT use `--draft` unless the user asks for it.

## Step 8: Report

Show the user:
- PR URL
- PR number
- Title
- Files changed count
- Commits included count

## Rules

- NEVER create a PR with an empty body
- NEVER push to `main` directly
- A PR that changes user-visible behaviour MUST carry a `## What to test` section — it is the
  source of the tester-facing release notes. Recovering after the merge means re-dispatching the
  distribute workflow with its `release_notes` input, which costs a full rebuild and redistribute,
  and testers who already installed the first build never see the corrected note
- The section must be in the PR **description**, not only in a commit message. The workflow reads
  the description via the API; a commit message reaches the notes only through the fallback path,
  on a direct push with no associated PR
- Always analyze ALL commits on the branch, not just the latest
- If the diff is very large (50+ files), group changes into high-level categories
- Warn if the PR includes potentially sensitive files (.env, credentials, google-services.json)
- Do NOT add reviewers or labels unless the user asks
