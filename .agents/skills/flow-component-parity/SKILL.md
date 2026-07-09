---
name: flow-component-parity
description: Workflow for advancing vatori Vaadin Flow component product parity one small verified slice at a time. Use when the user asks to推进 Flow 组件 parity, Flow over IPC parity, product-grade component coverage, or to select and complete the next Flow component goal without creating process-log docs.
---

# Flow Component Parity

Use this skill to keep long-running Flow component parity work moving without
turning repository docs into process logs. Each invocation should select and
finish one verifiable slice.

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

Pick one smallest high-value parity gap that can close in the current turn.
Prefer a real developer-shaped Flow component or capability path over a narrow
fixture. Define the expected path before editing:

- bootstrap or resource handoff reaches the WebView;
- WebView event reaches the Java Flow listener;
- Java state change produces a UIDL patch or desktop command/result;
- the WebView shows the expected DOM or behavior;
- the trace inspector, smoke harness or focused test can prove it.

If no product slice can close because validation is missing, choose the smallest
adapter, inspector or smoke-harness improvement that unblocks the next slice.

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

For component parity, prefer evidence that covers all relevant layers: bootstrap
or resource ownership, event transport, server listener execution, UIDL patch or
command/result, and visible WebView behavior.

Do not mark the slice complete when a known class of behavior is only mocked,
fixture-only, or still routed through an unstated compatibility fallback.

## Document

Do not create `docs/goal-*.md`, `docs/archive/goals/`, or other per-run archive
directories. Do not put process logs, trace results, batch numbers or temporary
plans into `AGENTS.md`, `README.md` or `README.zh-CN.md`.

Update existing docs only when durable state changes:

- `docs/flow-over-ipc-runtime.md` for current runtime behavior and limits;
- `docs/acceptance-tools.md` for stable validation contracts and commands;
- `docs/roadmap.md` for sequencing and open product boundaries;
- focused design docs for architecture decisions that must outlive the turn.

Per-turn status belongs in the final response, git history, CI artifacts,
release assets or external storage.

## Stop And Report

Finish one slice per invocation. In the final response, report the chosen slice,
what changed, which validations ran, any remaining blockers and the best next
slice. If the slice cannot close after reasonable investigation, stop with a
clear blocker instead of creating a new goal file.
