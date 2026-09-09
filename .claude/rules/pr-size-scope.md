---
description: Enforce PR size limits and single-responsibility scope to keep reviews manageable and merges safe
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "**/*.sq"
---

# PR Size & Scope Enforcement

## Rule

PRs should be small, focused, and address a single concern. Large or mixed-scope PRs are harder to review, more likely to introduce bugs, and more painful to revert.

## Size Guidelines

### File count thresholds
- **< 15 files changed**: Normal — review as-is
- **15–30 files changed**: Flag for attention — verify all changes serve a single purpose
- **> 30 files changed**: Recommend splitting unless it's a well-scoped refactor (e.g., rename across the codebase) or a new module scaffold

### Line count thresholds
- **< 400 lines changed**: Normal
- **400–800 lines changed**: Flag — check if the PR mixes concerns
- **> 800 lines changed**: Strongly recommend splitting

### Exceptions (large PRs that are acceptable)
- Auto-generated code (scaffolding via `/create-feature`, generated SQLDelight code)
- Codebase-wide renames or formatting changes (single mechanical concern)
- New module creation with boilerplate structure (4-layer scaffold)
- Version catalog updates that touch many `build.gradle.kts` files
- Migration PRs with a clear single purpose

## Scope Rules

### Single responsibility (MUST enforce)
A PR should do ONE of the following, not multiple:
- Add a new feature
- Fix a bug
- Refactor existing code
- Update dependencies
- Change build configuration
- Add/update tests
- Update documentation

### Mixed-scope red flags
Flag the PR if it contains:
- A feature AND an unrelated refactor
- A bug fix AND a dependency update in unrelated modules
- Build config changes AND business logic changes
- Code style/formatting changes mixed with functional changes

### How to split
When recommending a split, suggest concrete boundaries:
- "The build.gradle.kts changes could be a separate PR merged first"
- "The refactor in core-network is independent — extract it to its own PR"
- "Tests for the existing behavior could land first, then the behavior change on top"

## Severity

- `⚠️ Change requested` — PR mixes unrelated concerns and should be split
- `💡 Suggestion` — PR is large but single-purpose; consider splitting for easier review
