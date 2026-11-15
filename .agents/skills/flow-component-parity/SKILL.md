---
name: flow-component-parity
description: Workflow for advancing vatori Vaadin Flow parity through complete, bounded developer scenarios within the approved scope. Use when the user asks to推进 Flow 组件 parity, Flow over IPC parity, product-grade component coverage, or to select and complete the next Flow component goal without creating process-log docs.
---

# Flow Component Parity

Use this skill to keep long-running Flow component parity work moving without
turning repository docs into process logs. Each invocation should select and
finish one complete, verifiable developer behavior or approved baseline scenario.
The current scope and completion rules in `docs/goal.md` govern selection; the
long-term parity ambition does not automatically expand the current baseline.

## Orient

Read the stable boundaries before choosing work:

- `AGENTS.md`
- `docs/goal.md`
- `docs/roadmap.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/acceptance-tools.md`

Inspect the relevant framework, host, test app and inspector code. If Vaadin
behavior is unclear and local upstream source is available, inspect that source
before using web search.

## Choose One Slice

Pick one high-value gap in the approved scope and state which baseline item it
closes or which actual product defect it removes. Fix the passing conditions
before editing; do not define the task by what helper fits in the current turn.
Prefer a real developer-shaped Flow component or capability path over a narrow
fixture. Define the expected path before editing:

- bootstrap or resource handoff reaches the WebView;
- WebView event reaches the Java Flow listener;
- Java state change produces a UIDL patch or desktop command/result;
- the WebView shows the expected DOM or behavior;
- the trace inspector, smoke harness or focused test can prove it.

When validation is missing, implement only the adapter, fixture, driver, runner
or inspector work directly needed to finish that same scenario. These are
internal steps, not separate deliveries without a concrete blocker. Two
consecutive infrastructure-only turns require a direction review and a plan to
finish the blocked scenario, not another independently selected helper.

## Implement

Keep the framework shape intact:

- Preserve the Vaadin Flow Java programming model for applications.
- Keep local HTTP/Servlet/WebSocket compatibility mode explicit when it is still
  involved.
- Put deterministic hooks and broad assertion surfaces in `test-apps/`, not in
  developer-facing examples.
- Use real Flow APIs and runtime paths wherever possible. Do not claim product
  parity from fixture-only evidence.
- Avoid mixing unrelated component areas in one iteration.

## Validate

Run the narrowest meaningful checks for the slice, then repository hygiene. Add
or update a `DesktopTraceInspector` gate only when the behavior becomes a stable
acceptance contract.

Use deterministic tests for state-machine combinations and real WKWebView runs
for the approved representative end-to-end behavior. Run full regression at
relevant integration points and final baseline verification, not for every
narrow assertion. Prefer replaying retained traces for verifier-only changes;
keep the original run outcome separate from the new verification result. Rerun
when required observations are missing or product behavior, driver or execution
conditions change. Reuse a traceable build where applicable, retaining isolated
evidence, permissions, generation fences and actual process-exit checks.

For component parity, prefer evidence that covers all relevant layers: bootstrap
or resource ownership, event transport, server listener execution, UIDL patch or
command/result, and visible WebView behavior.

Do not mark the slice complete when a known class of behavior is only mocked,
fixture-only, or still routed through an unstated compatibility fallback.
Apply this rule to the fixed scenario and approved baseline, not every possible
combination. Once its conditions pass, close it pending final baseline rerun.
Reopen only for a newly discovered in-scope product defect or relevant behavior
change; extra combinations and scale belong in the backlog.

## Document

Do not create `docs/goal-*.md`, `docs/archive/goals/`, or other per-run archive
directories. Do not put process logs, trace results, batch numbers or temporary
plans into `AGENTS.md`, `README.md` or `README.zh-CN.md`.

Update existing docs only when durable capability state changes, not after each
helper or synthetic test:

- `docs/flow-over-ipc-runtime.md` for current runtime behavior and limits;
- `docs/acceptance-tools.md` for stable validation contracts and commands;
- `docs/roadmap.md` for sequencing and open product boundaries;
- focused design docs for architecture decisions that must outlive the turn.

Per-turn status belongs in the final response, git history, CI artifacts,
release assets or external storage.

## Stop And Report

Finish the complete scenario before reporting unless there is a concrete
blocker. Report the baseline item closed or product defect fixed, the relevant
validation, remaining finite items and the next complete scenario. Distinguish
new capability, defect repair and increased verification confidence; assertion
counts are not the primary progress measure. If blocked, identify the blocker
and completed work without inventing completion or creating a new goal file.
