---
name: agent-flows-visualizer
description: Visualize the agent workflow by reading all agent definitions and producing an ASCII flow diagram with routing table.
disable-model-invocation: true
allowed-tools: Read, Glob, Grep
---

# Visualize Agent Workflow

Read all agent definitions and produce a complete ASCII flow diagram of the agent workflow, including routing metadata and a summary table.

## Step 1: Discover All Agents

Use `Glob` to find all agent files:

```
.claude/agents/*.md
```

For each file, read the **frontmatter** (YAML between `---` markers) and extract:
- `name` — the agent identifier
- `model` — the LLM model (haiku, sonnet, opus)
- `color` — the terminal color

## Step 2: Extract Workflow Routing

For each agent file, read the **full content** and search for:

1. **`## Integration with Workflow`** sections — contain ASCII diagrams showing where the agent sits in the pipeline
2. **Lines containing `Output consumed by`** — show the downstream agent(s)
3. **Lines containing `Next step based on verdict`** — show conditional routing (e.g., PASS → X, FAIL → Y)
4. **Delegation tables** — markdown tables with `| Work Type | Specialist Agent |` or `| Work Type | Agent |` headers that show which agents can be delegated to
5. **Mode descriptions** — agents with multiple operating modes (e.g., `pr-review-enforcer` has PR Review, Local Review, and Synthesis modes)

Record for each agent:
- **Role** (one-line summary from the first paragraph of the body)
- **Input from** (which agent or trigger feeds into this one)
- **Output to** (which agent consumes the output)
- **Verdict routing** (conditional next steps, if any)
- **Delegates to** (specialist agents it can invoke)

## Step 3: Produce the Visualization

Output the following sections in order. Use raw text with box-drawing characters.

### Section 1: Agent Registry

Output a table of all discovered agents:

```
## Agent Registry

| # | Agent                     | Model  | Color  | Role                                           |
|---|---------------------------|--------|--------|------------------------------------------------|
| 1 | ticket-clarifier          | haiku  | blue   | Requirements validation gate                   |
| 2 | implementation-planner    | sonnet | purple | Step-by-step implementation plan producer      |
| 3 | dev-pair                  | sonnet | green  | Code generation from plans                     |
| ...                                                                                              |
```

List all agents found in Step 1, numbered, with their extracted role.

### Section 2: Pre-PR Pipeline (Happy Path)

Draw the main linear pipeline from ticket to PR:

```
## Pre-PR Pipeline (Happy Path)

  ┌─────────────────────┐     ┌──────────────────────────┐     ┌──────────────┐
  │  ticket-clarifier   │────▶│  implementation-planner   │────▶│   dev-pair   │
  │  (haiku / blue)     │     │  (sonnet / purple)        │     │  (sonnet /   │
  │                     │     │                            │     │   green)     │
  └─────────┬───────────┘     └────────────┬───────────────┘     └──────┬───────┘
            │                              │                            │
            │ NEEDS CLARIFICATION          │ (if arch decision needed)  │
            │ → Ask Developer              │ ↕ kmp-mobile-architect     │
            │ → loop back                  │   (opus / purple)          │
            │                              │                            │
            ▼                              ▼                            ▼
       [Ask User]                  [Plan produced]              [Code written]
                                                                       │
                                                                       ▼
                                                          ┌────────────────────────┐
                                                          │  pr-review-enforcer    │
                                                          │  LOCAL REVIEW MODE     │
                                                          │  (sonnet / orange)     │
                                                          └───────────┬────────────┘
                                                                      │
                                                          Verdict: PASS │ NEEDS_FIXES
                                                                      │
                                                    ┌─────────────────┴──────────────┐
                                                    │                                │
                                                    ▼                                ▼
                                              ┌──────────┐                   ┌──────────────┐
                                              │  /create  │                   │  Local Fix   │
                                              │   -pr     │                   │  Loop (§3)   │
                                              └──────────┘                   └──────────────┘
```

### Section 3: Local Review Loop

Draw the review-fix-retest loop (max 3 iterations):

```
## Local Review Loop (max 3 iterations)

  ┌────────────────────────┐
  │  pr-review-enforcer    │◀─────────────────────────────────────────┐
  │  LOCAL REVIEW MODE     │                                          │
  │  (sonnet / orange)     │                                          │
  └───────────┬────────────┘                                          │
              │                                                       │
              │ Verdict: NEEDS_FIXES / BLOCKING_ISSUES                │
              ▼                                                       │
  ┌────────────────────────┐                                          │
  │      local-fix         │                                          │
  │  (sonnet / cyan)       │                                          │
  │  Applies minimal fixes │                                          │
  └───────────┬────────────┘                                          │
              │                                                       │
              │ Ready for Re-test: YES                                │
              ▼                                                       │
  ┌────────────────────────┐                                          │
  │  kmp-test-engineer     │     PASS ──▶ /create-pr                  │
  │  VERIFICATION MODE     │────────────────────────────────          │
  │  (sonnet / pink)       │     FAIL + iteration < 3 ───────────────┘
  └───────────┬────────────┘
              │
              │ FAIL + iteration = 3
              ▼
  ┌────────────────────────┐     ┌─────────────────────────────┐
  │   failure-analysis     │────▶│  Ask Developer               │
  │   (sonnet / red)       │     │  → implementation-planner    │
  └────────────────────────┘     │    (retry with revised plan) │
                                 └─────────────────────────────┘
```

### Section 4: Specialist Delegations

Show which agents delegate to which specialists:

```
## Specialist Delegations

  dev-pair delegates to:
  ├── kmp-build-engineer     (sonnet / yellow) — Build config, Gradle, publishing
  ├── ios-kmp-integrator     (sonnet / red)    — iOS integration, XCFramework
  ├── android-kmp-sdk-integrator (sonnet / green) — Android SDK integration
  └── kmp-test-engineer      (sonnet / pink)   — Tests (after implementation)

  implementation-planner delegates to:
  ├── kmp-mobile-architect   (opus / purple)   — New module design, SDK architecture
  ├── kmp-build-engineer     (sonnet / yellow) — Build configuration
  ├── ios-kmp-integrator     (sonnet / red)    — iOS integration
  ├── android-kmp-sdk-integrator (sonnet / green) — Android integration
  └── kmp-test-engineer      (sonnet / pink)   — Test planning

  linear-ticket-architect (sonnet / yellow):
  └── Standalone — creates/breaks down Linear tickets (no downstream agent)
```

### Section 5: CI / Post-PR Pipeline

Show the CI and post-PR agents:

```
## CI / Post-PR Pipeline

  ┌──────────────────────┐
  │  PR opened / pushed  │
  └──────────┬───────────┘
             │
             ▼
  ┌────────────────────────┐
  │  pr-review-enforcer    │
  │  PR REVIEW MODE        │    Posts inline comments + commit status via GitHub API
  │  (sonnet / orange)     │    Events: APPROVE | REQUEST_CHANGES | COMMENT
  └───────────┬────────────┘
              │
              │ REQUEST_CHANGES
              ▼
  ┌────────────────────────┐
  │      ci-autofix        │    Reads /tmp/ci-failure-logs.txt or /tmp/review-comments.txt
  │  (sonnet / red)        │    Commits fix, pushes, posts PR comment
  └────────────────────────┘

  ┌──────────────────────┐
  │    CI job fails       │
  └──────────┬───────────┘
             │
             ▼
  ┌────────────────────────┐
  │      ci-autofix        │    Reads /tmp/ci-failure-logs.txt
  │  (sonnet / red)        │    Classifies: Lint | Test | Build failure
  └───────────┬────────────┘    Applies targeted fix, commits, pushes
              │
              │ (if unable to fix)
              ▼
  ┌────────────────────────┐
  │  Posts analysis comment │    Escalates to human review
  │  on PR                 │
  └────────────────────────┘

  Review Synthesis (on re-review):
  ┌────────────────────────┐
  │  pr-review-enforcer    │    Collects all outstanding comments
  │  SYNTHESIS MODE        │    Classifies: Resolved | Unresolved | Outdated
  │  (sonnet / orange)     │    Resolves fixed threads, produces action list
  └────────────────────────┘
```

### Section 6: Routing Table

Output a complete routing table derived from the parsed data:

```
## Routing Table

| Agent                      | Input From                          | Output To                                | Conditional Routing                              |
|----------------------------|-------------------------------------|------------------------------------------|--------------------------------------------------|
| ticket-clarifier           | User / Linear ticket                | implementation-planner (if APPROVED)     | NEEDS CLARIFICATION → Ask User → loop back       |
| implementation-planner     | ticket-clarifier (APPROVED)         | dev-pair                                 | May consult kmp-mobile-architect                  |
| dev-pair                   | implementation-planner (plan)       | pr-review-enforcer (local mode)          | Delegates to specialists per work type            |
| pr-review-enforcer (local) | dev-pair output                     | /create-pr (PASS) or local-fix (FIXES)  | PASS → PR / NEEDS_FIXES → local-fix              |
| local-fix                  | pr-review-enforcer (local report)   | kmp-test-engineer (verification)         | Skips unfixable items → flags for human           |
| kmp-test-engineer (verify) | local-fix output                    | /create-pr or loop or failure-analysis   | PASS → PR / FAIL<3 → loop / FAIL=3 → escalate   |
| failure-analysis           | 3x failed loop                      | Ask Developer → implementation-planner   | Developer guidance → revised plan                 |
| pr-review-enforcer (PR)    | PR opened / pushed                  | ci-autofix (if REQUEST_CHANGES)          | APPROVE / REQUEST_CHANGES / COMMENT               |
| pr-review-enforcer (synth) | Re-review request                   | ci-autofix or human                      | Produces prioritized action list                  |
| ci-autofix                 | CI failure logs / review comments   | PR comment + push                        | Unable to fix → posts analysis comment            |
| kmp-mobile-architect       | implementation-planner (consult)    | implementation-planner (design)          | Standalone consultation                           |
| kmp-build-engineer         | dev-pair / planner (delegate)       | Caller agent                             | Standalone specialist                             |
| ios-kmp-integrator         | dev-pair / planner (delegate)       | Caller agent                             | Standalone specialist                             |
| android-kmp-sdk-integrator | dev-pair / planner (delegate)       | Caller agent                             | Standalone specialist                             |
| kmp-test-engineer (write)  | dev-pair (delegate) / user          | Test files committed                     | Standalone specialist                             |
| linear-ticket-architect    | User request                        | Linear tickets created                   | Standalone — no downstream agent                  |
```

## Step 4: Cross-Check Completeness

After producing the visualization:

1. Count the agents in the Agent Registry — it must match the number of `.md` files found by Glob
2. Verify every agent appears in at least one of: the Pre-PR Pipeline, Local Review Loop, Specialist Delegations, CI/Post-PR Pipeline, or Routing Table
3. If any agent is missing from the visualization, add it to the appropriate section

Output a final line:

```
---
✓ All <N> agents accounted for in visualization.
```

Or if something is missing:

```
---
⚠ <agent-name> not found in any pipeline — verify its Integration with Workflow section.
```
