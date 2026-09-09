---
description: Committed configuration and docs must never contain machine-local absolute paths (/Users/<name>/, /home/<name>/, C:\Users\) — use repo-relative paths or placeholders
paths:
  - ".claude/**"
  - "docs/**"
---

# No Absolute Paths in Configuration Files

## Rule

Agent definitions, skill files, and all other configuration checked into the repository must never contain absolute paths that reference a specific user's home directory or machine-local filesystem layout. Hardcoded absolute paths break portability: they resolve only on the developer's machine that authored the file and will silently fail for every other team member and in CI.

## What to Check

### MUST BLOCK

- Any absolute path beginning with `/Users/<name>/` (macOS user home) in a file under `.claude/`
  - **Fix**: Replace with a path relative to the repository root (e.g., `.claude/agent-memory/workload-planner/`) or relative to the worktree parent (`../your-repo-TICKET_ID/`)
- Any absolute path beginning with `/home/<name>/` (Linux user home) in a file under `.claude/`
  - **Fix**: Same as above — use a relative path from the repository root or a documented environment variable
- Any absolute path beginning with `C:\Users\<name>\` or `C:/Users/<name>/` (Windows user home) in a file under `.claude/`
  - **Fix**: Replace with a relative path
- Any absolute path beginning with `/Users/`, `/home/`, or `C:\Users\` anywhere in `docs/` configuration documentation
  - **Fix**: Replace with a placeholder notation like `<repo-root>/` or a relative path

### MUST FLAG

- An absolute path rooted at a project-specific directory (e.g., `/Users/gerchoatanasov/repos/agustinmadina/tickr/`) that would work for the author but not for other contributors who check out the repository to a different location
  - **Fix**: Replace with a path relative to the repository root (`.`) — no absolute path should be needed for in-repo references
- A worktree path written as an absolute path (e.g., `/Users/gerchoatanasov/github/example/your-repo-TICKET-145/`)
  - **Fix**: Express as `../your-repo-TICKET_ID/` (relative to the repo root worktree) or document it as a derived value (`<worktrees-parent>/your-repo-TICKET_ID/`) in a clearly marked substitution block
- An absolute path inside an agent memory file (`.claude/agent-memory/**/*.md`) — memory files are committed and shared, so machine-local paths in them create the same portability problem
  - **Fix**: Rewrite any path references using repository-relative notation

## Scope

This rule applies to all files checked into the repository under the `.claude/` directory tree and the `docs/` directory:

- `.claude/agents/*.md` — agent definitions
- `.claude/skills/**/*.md` — skill files
- `.claude/rules/*.md` — rule files (including this file)
- `.claude/agent-memory/**/*.md` — persistent agent memory
- `.claude/templates/*.md` — rule and other templates
- `docs/**/*.md` — project documentation

It does NOT apply to:

- Files listed in `.gitignore` — those are never committed
- Shell scripts or CI configuration that use `$HOME` or environment variables for legitimate platform-specific resolution (these are runtime-resolved, not hardcoded developer paths)
- Inline comments in Kotlin/Gradle source files that mention a path as a hypothetical example

## Common Mistakes

- Copying a working absolute path from a terminal session directly into an agent definition without converting it to a relative path — this is the exact pattern that triggered this rule (a developer-local path committed in an agent definition that does not resolve for other team members or CI)
- Writing worktree paths as absolute because that is how `git worktree add` reports them — the worktree directory itself is outside the repo root, but references to it inside committed files must use `../` relative notation or a documented placeholder
- Storing machine-local paths in agent memory files under `.claude/agent-memory/` — memory files are version-controlled and shared; a path that works in one checkout silently fails in another
- Using an absolute path in a `## File Paths` or `## Instructions` section of a skill or agent definition because it "reads more clearly" — relative paths are equally readable once the convention is established
- Forgetting that `/var/folders/...` (macOS temp) and `/tmp/...` (Linux temp) are also machine-local absolute paths and must not be hardcoded in committed configuration

## Examples

### Good

```markdown
<!-- .claude/agents/workload-planner.md -->
## Memory Location
Agent memory is stored at `.claude/agent-memory/workload-planner/` relative to the repository root.

## Worktree References
Worktrees are created at `../your-repo-TICKET_ID/` (one level above the repository root).
Example: if the repo is checked out at `~/repos/agustinmadina/tickr`, a TICKET-145 worktree
lives at `~/repos/agustinmadina/tickr-TICKET-145/`.
```

```markdown
<!-- .claude/skills/linear-task/SKILL.md -->
## Working Directory
All file operations use paths relative to the repository root (`.`).
Agent memory writes go to `.claude/agent-memory/<agent-name>/`.
```

```markdown
<!-- docs/WORKFLOW.md -->
## Worktree Location
Worktrees are placed at `<worktrees-parent>/your-repo-{TICKET_ID}/` where
`<worktrees-parent>` is the parent directory of your repository checkout.
```

### Bad

```markdown
<!-- BAD: hardcoded developer home directory — fails for every other contributor -->
## Memory Location
Agent memory is stored at `/Users/gerchoatanasov/repos/agustinmadina/tickr/.claude/agent-memory/workload-planner/`.
```

```markdown
<!-- BAD: absolute worktree path — machine-local, not portable -->
## Worktree Setup
Create the worktree at `/Users/gerchoatanasov/github/example/your-repo-TICKET-145/`.
```

```markdown
<!-- BAD: Linux home path — same problem on a different OS -->
Working directory: /home/alice/projects/your-repo/
```

```markdown
<!-- BAD: Windows home path — same problem -->
Output path: C:\Users\alice\projects\your-repo\.claude\
```

```markdown
<!-- BAD: project-rooted absolute path — breaks for anyone who checks out to a different location -->
Read the plan file at /Users/gerchoatanasov/repos/agustinmadina/tickr/.claude/plans/current.md
```

## Severity

- `🚫 Blocking` — Any absolute path beginning with `/Users/<name>/`, `/home/<name>/`, or `C:\Users\<name>\` in any file under `.claude/` or `docs/`
- `⚠️ Change requested` — Any other machine-local absolute path (project-rooted or temp directory) in a committed configuration or documentation file
- `💡 Suggestion` — An absolute path that is clearly a placeholder example (e.g., surrounded by `<` `>` angle brackets) but would be clearer if rewritten using explicit relative path notation
