---
name: commit
description: Create a well-formatted git commit following the project's conventional commit format with ticket ID and module scope.
argument-hint: "[optional message override]"
disable-model-invocation: true
allowed-tools: Bash(git *)
---

# Create Commit

Create a conventional commit for the current staged and unstaged changes.

## Step 1: Gather context

Run these in parallel:

1. `git status` — see all changed/untracked files (never use `-uall`)
2. `git diff` and `git diff --staged` — see the actual changes
3. `git log --oneline -5` — see recent commit style
4. `git branch --show-current` — get current branch name

## Step 2: Detect ticket ID

Extract the ticket ID from the branch name:

- Branch `TICKET-145-add-auth-flow` -> ticket ID is `TICKET-145`
- Branch `NO-TASK-fix-something` -> ticket ID is `NO-TASK`
- Branch `main` or no pattern match -> omit ticket ID

## Step 3: Detect scope from changed files

Determine the module scope from the file paths that were changed:

| Changed files path | Scope |
|---|---|
| `core/core-network/...` | `core-network` |
| `core/core-domain/...` | `core-domain` |
| `core/core-ui/...` | `core-ui` |
| `core/core-common/...` | `core-common` |
| `core/core-storage/...` | `core-storage` |

| `features/feature-auth/...` | `auth` |
| `features/feature-<name>/...` | `<name>` |
| `androidApp/...` | `android` |
| `iosApp/...` | `ios` |
| `sharedLib/...` | `shared` |
| `gradle/...` or `build.gradle.kts` | `build` |
| `.claude/...` | `claude` |
| `docs/...` | `docs` |
| Multiple modules | use the most significant one, or omit scope |

## Step 4: Determine commit type

Analyze the diff to pick the right type:

| Type | When to use |
|---|---|
| `feat` | New functionality, new files that add a feature |
| `fix` | Bug fix, error correction |
| `refactor` | Code restructuring without behavior change |
| `chore` | Build config, dependencies, CI, tooling |
| `test` | Adding or updating tests |
| `docs` | Documentation changes |
| `style` | Formatting, whitespace, naming (no logic change) |

## Step 5: Stage files

Stage relevant files by name. Rules:

- **NEVER** use `git add -A` or `git add .`
- Stage specific files: `git add path/to/file1 path/to/file2`
- **NEVER** stage files containing secrets (`.env`, `credentials.json`, `google-services.json`, keystore files)
- Warn the user if any sensitive files are in the changeset
- Group related files together in a single `git add` command

If the user provided `$ARGUMENTS`, they may be specifying which files to include or a message override. Interpret accordingly.

## Step 6: Compose the commit message

### Format

```
TICKET-ID type(scope): short description

Optional longer explanation of what changed and why.
Not what the code does (the diff shows that), but WHY.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

### Examples

```
TICKET-145 feat(auth): add login screen with email validation

TICKET-160 fix(core-network): handle token refresh race condition

NO-TASK chore(build): update Kotlin to 2.3.0

TICKET-103 refactor(shared): extract Koin module registration to base class
```

### Rules for the short description

- Imperative mood: "add", "fix", "update" (not "added", "fixes", "updated")
- Lowercase first letter after the colon
- No period at the end
- Max 60 characters for the short description line
- Focus on WHAT was done, not HOW

### When to add a body

Add a body (blank line after subject) when:
- The change is not obvious from the subject line
- There are important side effects
- There's context the reviewer needs

Skip the body for trivial changes (dependency bumps, single-file fixes, formatting).

## Step 7: Create the commit

Use a HEREDOC to ensure proper formatting:

```bash
git commit -m "$(cat <<'EOF'
TICKET-ID type(scope): short description

Optional body.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

## Step 8: Verify

Run `git status` after committing to confirm success. Show the user the commit hash and message.

## If the user provided a message override

If `$ARGUMENTS` is a commit message (not a file path), use it as the short description but still:
- Prepend the ticket ID from the branch
- Add the type and scope
- Add the co-author line

## If pre-commit hooks fail

- Read the hook output
- Fix the issue
- Re-stage the fixed files
- Create a NEW commit (never amend)
