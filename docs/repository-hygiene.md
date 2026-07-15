# Repository Hygiene

This repository intentionally separates stable project docs from process logs and
generated evidence.

## Guard

Run:

```bash
bash bin/repository-hygiene-check
```

The guard checks for:

- archived goal/log directories that should not be committed.
- generated traces under docs or source directories.
- legacy numbered Flow IPC gate aliases in current docs.
- unstable process notes in top-level entry files.
- accidental cache directories and generated artifacts.
- framework/test-support dependency direction, typed-runtime ownership and
  acceptance-fixture leakage.
- retired custom-shell/test-support paths and the minimal devtools source
  allowlist.

Keep the guard when changing docs, agent instructions, acceptance gate names or
generated-resource fixtures.

## Top-Level Docs

`AGENTS.md`, `README.md` and `README.zh-CN.md` are stable entry points. They should
not contain:

- single-run results.
- historical goal notes.
- old numbered acceptance aliases.
- long progress logs.
- unrelated project instructions.

Use focused docs under `docs/`, final answers, git history, CI artifacts or
release assets for evidence that needs to be preserved.

## Generated Artifacts

Generated traces, screenshots, downloaded files, build output, Maven local repo
contents, uv caches and Python bytecode do not belong in version control.

Useful generated locations:

- `.build/`
- `target/`
- `.m2/repository/`

When an experimental prototype is superseded by compiled runtime code and living
docs, keep only the durable conclusion in `docs/` and remove the stale prototype
tree from the repository map.

## Acceptance Commands

Prefer semantic gate names:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-framework-app .build/vaadin-desktop-host-swift/host.out.log
```

Use `bash bin/flow-ipc-trace-inspect --help` for the current list.
