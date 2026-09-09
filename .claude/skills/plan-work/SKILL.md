---
name: plan-work
description: Plan a specific chunk of work interactively — clarify requirements, produce a structured plan, and optionally create a Linear ticket.
argument-hint: "<description of the work to plan>"
---

# Plan Work

Plan a specific chunk of work through an interactive process: clarify requirements, produce a structured implementation plan, and optionally create a Linear ticket.

## Input

`$ARGUMENTS` is a free-text description of the work to plan. It can be a feature request, bug description, refactoring goal, or any engineering task.

If `$ARGUMENTS` is empty, use the `AskUserQuestion` tool to ask the developer what they want to plan.

## Phase 1: Understand the Work

Read the provided description carefully. Before doing any planning, identify anything that is unclear, ambiguous, or missing. Specifically check for:

- **Scope**: Is it clear what is included and what is NOT included?
- **Acceptance criteria**: Is there a clear definition of "done"?
- **Affected modules**: Can you determine which modules/layers are impacted?
- **Dependencies**: Are there prerequisites, blocked-by items, or related work?
- **Edge cases**: Are there obvious edge cases that the description doesn't address?
- **Platform considerations**: Does this affect Android, iOS, Desktop, or all?

**If ANYTHING is unclear, use the `AskUserQuestion` tool to ask the developer for clarification before proceeding.** Do not assume or guess. Ask targeted, specific questions — not vague "can you clarify?" requests.

Use `AskUserQuestion` to present choices when questions have a finite set of answers. Use free-text output only when the question is truly open-ended and cannot be structured as options.

Wait for the developer's answers before moving to Phase 2.

## Phase 2: Explore the Codebase

Once requirements are clear, explore the codebase to understand the current state:

- Identify existing modules, files, and patterns relevant to the work
- Check for existing code that can be reused or extended
- Understand the dependency graph for affected modules
- Look at similar features already implemented for pattern reference

## Phase 3: Produce the Plan

Create a structured implementation plan with the following sections:

### Plan Output Format

```
## Work Plan: <concise title>

### Summary
<1-2 sentence description of what this work accomplishes>

### Scope
- **In scope**: <bulleted list>
- **Out of scope**: <bulleted list>

### Affected Modules
| Module | Change Type | Description |
|---|---|---|
| `features/feature-foo/domain` | New | Add use case and repository interface |
| `features/feature-foo/data` | New | Implement repository, add API client |
| ... | ... | ... |

### Implementation Steps
Ordered list of implementation steps. Each step should be:
- Small enough to be a single commit
- Clear about which files are created/modified
- Specific about what code changes are needed

1. **Step title** — Description of what to do and why
   - Files: `path/to/File.kt` (create/modify)
   - Details: Specific implementation notes

2. **Step title** — ...

### Dependencies & Prerequisites
- <any work that must be done first>
- <any external dependencies (backend APIs, design specs, etc.)>

### Risks & Considerations
- <anything that could go wrong or needs special attention>
- <platform-specific concerns>
- <performance considerations>

### Estimated Complexity
<Small / Medium / Large> — <brief justification>
```

## Phase 4: Developer Approval

Present the plan to the developer, then use the `AskUserQuestion` tool to ask for approval:

- Options: **Approve** (proceed as-is), **Adjust** (provide changes), **Reject** (start over)
- If the developer selects "Adjust", read their instructions, update the plan, and present the revised version for re-approval using `AskUserQuestion` again
- Repeat until approved or rejected

## Phase 5: Linear Ticket (Optional)

Once the plan is approved, use `AskUserQuestion` to ask the developer whether they want to create a Linear ticket:

- Options: **Yes, create ticket** / **No, skip**

If **yes**, use `AskUserQuestion` to gather ticket details in a single prompt. Structure each detail as a separate question with suggested defaults based on the plan:

- **Team**: Which team key? Offer options based on known teams (e.g., `CORE`, `MOBILE`)
- **Priority**: 1=Urgent, 2=High, 3=Medium, 4=Low — pre-select the recommended priority
- **Assignee**: Offer known team members or "Unassigned"
- **Estimate**: Suggest a story point estimate based on complexity

For open-ended fields (parent ticket, labels), ask via text output or include as an "Other" option.

Then use the `/create-linear-ticket` skill to create the ticket. Build the JSON payload from the plan:

- **title**: Derive from the plan title (action-oriented, concise)
- **description**: Convert the plan into a well-formatted Markdown ticket description including:
  - Summary
  - Requirements (from scope)
  - Implementation steps
  - Acceptance criteria (derived from scope + steps)
  - Risks/considerations

If the developer says **no** to the Linear ticket, end the skill and confirm the plan is saved in the conversation for reference.

## Rules

- **Use `AskUserQuestion` for all developer interactions** — clarifications, approvals, and ticket details. Use structured options whenever the question has a finite set of answers. Fall back to text output only for truly open-ended questions.
- **Always ask questions when unclear** — never assume requirements
- **Always wait for developer approval** before offering to create a ticket
- **Keep the plan actionable** — every step should be something a developer can execute
- **Follow project architecture** — plans must respect the 4-layer module structure, clean architecture rules, and all project conventions from CLAUDE.md
- **Be specific about files** — reference actual file paths, not abstract descriptions
- **Don't over-plan** — if the work is small (1-3 files, single concern), keep the plan proportionally brief
