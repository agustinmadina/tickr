---
name: plan-as-product-owner
description: Plan a chunk of work as a product owner — frame the problem, define outcomes and success metrics, cut an MVP, sequence the work into phases, persist the brief as a subpage of the shared Notion home (default), and optionally hand off to Linear or /plan-work. Use BEFORE engineering planning; pair with /plan-work for implementation detail.
argument-hint: "<problem statement, opportunity, or initiative to plan>"
---

# Plan as Product Owner

Plan a chunk of work from a product owner's perspective: frame the problem, name the user and their pain, define what success looks like in measurable terms, cut a scoped MVP, and sequence the work into deliverable phases. Hand off implementation detail to `/plan-work`.

This skill is the **product-discovery** complement to `/plan-work`. `/plan-work` answers *how do we build it?*. This skill answers *what is the problem, who feels it, what does success look like, and what is the smallest valuable cut?* Use this skill first when the work is greenfield, when scope is fuzzy, or when leadership/stakeholders need a brief before engineering plans anything.

## Input

`$ARGUMENTS` is a free-text description of a problem, opportunity, initiative, or feature idea. It can be vague (`"users complain that onboarding is slow"`), a partner ask (`"Acme wants single sign-on"`), or a strategic theme (`"reduce KYC drop-off"`).

If `$ARGUMENTS` is empty, use the `AskUserQuestion` tool to ask the user what they want to plan.

## Constants

- **Notion parent page (home for every plan):** `https://www.notion.so/Shared-35d2f88a1e158023b470fcd5accb505b`
  - Page ID: `35d2f88a-1e15-8023-b470-fcd5accb505b`
  - Every approved brief is persisted as a subpage of this page by default (see Phase 7, Path A). This is the system of record for product briefs — Linear tickets and `/plan-work` handoffs are downstream.

## Phase 1: Frame the problem

**Do not jump to solutions.** The PO's first job is to make sure the problem is real, well-understood, and worth solving. Read the input and identify what you know and what you don't. Use `AskUserQuestion` to clarify anything in this list before continuing:

- **Who** — Which user(s) experience this? Be specific (new users, returning users, partner admins, internal ops). One persona, not "everyone".
- **Pain** — What pain do they feel today? What is the cost of the status quo? What workaround are they using?
- **Evidence** — How do we know this is real? Support tickets, analytics, partner feedback, OKR, gut feel? "Gut feel" is fine, mark it as such.
- **Why now** — Why solve this now and not last quarter or next? Is there a trigger (partner deadline, regulatory, growth target, competitive)?
- **Business context** — Which OKR, top-level goal, or strategic theme does this ladder up to? If none, flag that.
- **Constraints** — Are there fixed constraints (deadline, budget, must-not-break, compliance, platform-only)?

Ask only the questions whose answers you genuinely need. Don't ask everything at once — prioritise the questions that most change the shape of the plan.

Wait for answers before moving to Phase 2.

## Phase 2: Define outcomes and success

A PO plan is judged by outcomes, not output. Before scoping anything, write down what success looks like.

For each of these, write a one-line answer. If the user can't tell you, write `Unknown — needs validation` rather than guessing.

- **Primary outcome** — One sentence: what changes in the world if this ships? Phrase as user behaviour or business state, not feature presence. Bad: *"ship SSO"*. Good: *"partner admins onboard their team in under 10 minutes instead of one business day"*.
- **Success metrics** — 1–3 quantifiable signals. Pick from: conversion rate, time-to-X, error rate, NPS/CSAT, retention, support ticket volume, revenue. Include a target and a measurement window if known.
- **Anti-goals** — What we are explicitly NOT trying to achieve, even if a stakeholder asks. (`"This is not a redesign of the onboarding screen"`, `"This does not block on backend rate-limit work"`.)
- **Counter-metrics** — What we don't want to harm while shipping this. (`"Conversion on existing onboarding must not regress"`, `"Average session start time must stay under 800ms"`.)

If the user has no idea what success looks like, that itself is a finding. Surface it and offer to run a quick brainstorming pass via `superpowers:brainstorming` before continuing.

## Phase 3: Cut an MVP

A good PO names the **smallest cut that delivers the primary outcome** and defends what's out of scope for v1.

Produce three lists:

- **MVP (v1)** — The smallest scope that lets you measure the primary outcome and learn. If you cannot point at the success metric and say "we will move this with v1", v1 is wrong.
- **v1.1+ (next, but not now)** — Things that are valuable but can wait. Sequence them by what unlocks the most learning or value next.
- **Out of scope (probably never)** — Things stakeholders may ask for that this initiative will explicitly not address. Naming these now prevents scope creep and uncomfortable conversations later.

For each MVP item, write one line answering: *what would we observe in the metric if this item shipped on its own?* If the answer is "nothing measurable", the item is probably not MVP.

## Phase 4: Risks, dependencies, open questions

Three short lists:

- **Risks** — Things that could go wrong or invalidate the plan. (Adoption risk, technical unknowns, partner dependency, compliance review, platform parity gap.) Mark each `low / medium / high`.
- **Dependencies** — External work this depends on. Backend APIs, design specs, partner integration, legal sign-off, a different team's roadmap item. Name the owner if known.
- **Open questions** — Things we don't know yet that block confident execution. Flag whether each is `must-resolve-before-engineering` or `can-resolve-during-execution`.

## Phase 5: Produce the brief

Compose a PRD-style brief using this format. Keep it tight — every section is one paragraph or a short list, not an essay.

```
## Product Brief: <concise initiative title>

### Problem
**Who:** <persona>
**Pain:** <what they feel today>
**Evidence:** <how we know>
**Why now:** <trigger>
**Ladders up to:** <OKR / theme>

### Outcomes
**Primary outcome:** <one sentence, user-behaviour or business-state framing>
**Success metrics:**
- <metric> — target <number/threshold> within <window>
- ...
**Anti-goals:**
- ...
**Counter-metrics (do not harm):**
- ...

### Scope
**MVP (v1):**
- <item> — <what it moves in the metric>
- ...

**v1.1+ (next):**
- ...

**Out of scope:**
- ...

### Sequencing
| Phase | Goal | Exit criteria |
|---|---|---|
| v1 | <what v1 achieves> | <measurable signal that v1 is done> |
| v1.1 | <next slice> | <signal> |
| v2 | <later> | <signal> |

### Risks
- **<risk>** (low/med/high) — <one-line mitigation or "accept">
- ...

### Dependencies
- **<dep>** — owner: <name or team>, status: <known/blocked/unknown>
- ...

### Open questions
- [ ] **(must resolve before engineering)** <question>
- [ ] **(resolvable during execution)** <question>

### Estimated size
<T-shirt: S / M / L / XL> — <one-line justification (effort, risk, dependencies, not engineering hours)>
```

Notes on writing the brief:

- **Be explicit about uncertainty.** If something is a guess, mark it. A brief that says `evidence: gut feel — needs validation` is more useful than one that asserts a fake number.
- **No file paths, no module names, no implementation detail.** Those belong in `/plan-work`. If you find yourself writing `WidgetContainer.kt`, stop — you've left the PO lane.
- **Keep the brief skimmable.** A stakeholder should be able to read the whole thing in under 90 seconds.

## Phase 6: User approval

Present the brief, then use `AskUserQuestion` to ask for approval. Options:

- **Approve** — proceed to optional Linear handoff
- **Adjust** — user provides changes, you revise, present again, ask again
- **Reject** — start over from Phase 1 with new framing

Loop until approved or rejected. Each `Adjust` round should produce a visibly different brief — if the user says "tighten the metrics" and the metrics don't change shape, you've misunderstood.

## Phase 7: Persist the brief and hand off

Every approved brief lands as a Notion subpage by default. After Notion, the user may optionally create Linear tickets or hand off to engineering planning. The Notion page is the canonical source — Linear and `/plan-work` reference it.

Use `AskUserQuestion` to ask what to do, with multiple selections allowed:

- **(Default, recommended) Save as Notion subpage under Shared** — Path A. Persist the brief as a child page of the parent listed in the Constants section.
- **Create a parent epic + child tickets in Linear** — Path B. Best for initiatives with multiple shippable slices.
- **Create a single Linear ticket** — Path C. Best for one-cut MVPs.
- **Hand off to `/plan-work` for engineering planning** — Path D. Recommend the user runs `/plan-work` on the MVP slice next.
- **Just keep the brief in this conversation** — Path E. Skip Notion only when the user explicitly opts out (e.g. early throwaway exploration).

Default to A. If the user picks Linear paths without explicitly opting out of Notion, do A *and* the Linear path — the Notion page and the Linear ticket should cross-reference each other (Notion URL in the Linear description; Linear URL appended to the Notion page).

### Path A — Save as Notion subpage (default)

Use the Notion MCP server to create the subpage. The server is at `mcp.notion.com/mcp` and exposes tools after authentication.

1. **Check auth.** If Notion MCP page-creation tools (e.g. anything matching `mcp__notion__*` beyond the auth pair) are not yet available, the server is unauthenticated. Call `mcp__notion__authenticate`, share the authorization URL with the user, and wait until they confirm before calling `mcp__notion__complete_authentication`. Then proceed.

2. **Create the page.** Use whichever Notion MCP tool the authenticated server exposes for creating a page (commonly named like `create-page`, `notion_create_page`, `pages.create`, or similar — the exact name depends on the MCP version). The call must:
   - Set the parent to the page ID from the Constants section (`35d2f88a-1e15-8023-b470-fcd5accb505b`). If the tool accepts a URL instead, pass the full URL from Constants.
   - Set the page title to the brief's initiative title (the `## Product Brief: <title>` line from Phase 5, without the `## Product Brief:` prefix).
   - Set the page body to the full brief from Phase 5, converted from Markdown to the format the tool expects (most Notion MCP tools accept Markdown directly or a block array; prefer Markdown if both are supported).

3. **If the Notion MCP exposes no page-creation tool** (rare but possible), report this to the user with the brief still in the conversation, suggest they paste it manually into the Shared page, and continue to whichever additional handoffs they chose. Do not invent a tool name.

4. **On success**, capture the returned page URL and surface it back to the user: `Saved to Notion: <URL>`.

### Path B — Linear epic + children

Delegate to the `workload-planner` agent (via the `Agent` tool) with this prompt template:

```
Create a Linear epic for the following product brief, then create one child ticket per phase listed in the Sequencing table. Each child ticket should restate its phase exit criteria as acceptance criteria.

Brief source of truth: <Notion page URL from Path A, if it ran; otherwise "in conversation">

Brief:
<paste the brief from Phase 5 verbatim>

Team: <from AskUserQuestion>
Priority: <from AskUserQuestion>
Assignee: <from AskUserQuestion>
```

Surface the resulting ticket URLs back to the user. If Path A also ran, append the Linear epic URL to the Notion page (use the Notion MCP's page-update or append-block tool — again, name varies by MCP version).

### Path C — single Linear ticket

Use `AskUserQuestion` to collect: team, priority, assignee, estimate. Then invoke the `/create-linear-ticket` skill with a JSON payload built from the brief:

- **title**: from the brief title (action-oriented, concise)
- **description**: the full brief from Phase 5, formatted as Markdown, **with a "Source of truth" line at the top linking to the Notion page from Path A** (if Path A ran)
- **team**: from the user
- **priority**: from the user
- **assignee**: from the user
- **estimate**: from the user (if provided)
- **labels**: leave empty unless the user names specific labels

If Path A also ran, append the Linear ticket URL to the Notion page.

### Path D — handoff to /plan-work

End the skill with the suggestion: *"Run `/plan-work <MVP slice description>` next to produce an engineering implementation plan for the v1 cut."* If Path A ran, include the Notion URL in the suggestion so the engineering plan can link back to the brief. Do not invoke `/plan-work` yourself — the user invokes the next skill.

### Path E — keep in conversation only

End the skill with a one-line confirmation that the brief is in the conversation for later reference. Do not save it as a file unless the user explicitly asks. This path skips Notion intentionally — use only when the user explicitly opts out.

## Rules

- **Stay in the PO lane.** Frame problems and outcomes; do not design solutions, name files, or pick architectures. Hand off to `/plan-work` or `workload-planner` for those.
- **Use `AskUserQuestion` for all user interactions** — clarifications, approvals, handoff choice. Use structured options when the question has a finite set of answers.
- **Never invent evidence or metrics.** If the user doesn't know, write `Unknown — needs validation`. A brief that lies about its certainty is worse than a brief that admits uncertainty.
- **Cut MVP ruthlessly.** If every item in the MVP list is "must have", the cut is wrong — push back.
- **Outcomes over output.** If the brief reads like a feature list, rewrite it until each line connects to a user behaviour or business state.
- **Don't over-plan small work.** If the input is `"add a 'forgot password' link"`, the brief is three lines, not three pages. Match depth to ambiguity.
- **No code, no file paths.** If you catch yourself writing `.kt` or a package name, stop.
- **Pair with `/plan-work`, do not replace it.** This skill ends where engineering planning begins.
- **Notion is the system of record.** Every approved brief lands as a subpage under the parent in the Constants section by default. Skipping Notion (Path E) is an explicit opt-out, not a default. When other handoffs run alongside Notion, cross-reference them: Notion URL goes into the Linear/`/plan-work` artifact; their URLs go back onto the Notion page.
- **Never hardcode a different Notion parent.** The parent page ID is fixed in the Constants section. If a user asks for a one-off different destination, do it for that invocation only — do not edit the Constants section as a side effect.
