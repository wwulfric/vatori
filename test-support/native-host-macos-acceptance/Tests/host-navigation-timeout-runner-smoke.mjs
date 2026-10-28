import assert from 'node:assert/strict';
import { copyFileSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const work = mkdtempSync(join(tmpdir(), 'vatori-navigation-runner-smoke-'));
try {
  const fixtureBin = join(work, 'test-support/native-host-macos-acceptance/bin');
  const build = join(work, '.build/native-hosts/macos');
  mkdirSync(fixtureBin, { recursive: true });
  mkdirSync(build, { recursive: true });
  mkdirSync(join(work, 'bin'));
  copyFileSync(join(root, 'test-support/native-host-macos-acceptance/bin/run-host-navigation-timeout'),
    join(fixtureBin, 'run-host-navigation-timeout'));
  const hostLog = join(build, 'host.out.log');
  const trace = join(build, 'host-navigation-timeout.out.log');
  writeFileSync(hostLog, 'PREVIOUS_VALID_NEGATIVE_TRACE\n');
  writeFileSync(trace, 'PREVIOUS_VALID_NEGATIVE_TRACE\n');
  // Simulate an early package failure, before the normal launcher clears logs.
  writeFileSync(join(fixtureBin, 'run'), '#!/usr/bin/env bash\nexit 1\n');
  // This stub intentionally accepts the old marker, exposing stale-log reuse.
  writeFileSync(join(work, 'bin/flow-ipc-trace-inspect'),
    '#!/usr/bin/env bash\n[[ "$(< "$2")" == "PREVIOUS_VALID_NEGATIVE_TRACE" ]]\n');
  const result = spawnSync('bash', [join(fixtureBin, 'run-host-navigation-timeout')], {
    env: { ...process.env, VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD: '1' },
    encoding: 'utf8', timeout: 10_000,
  });
  assert.ifError(result.error);
  assert.equal(result.signal, null);
  assert.equal(result.status, 1, `early failure reused old trace: ${result.stdout} ${result.stderr}`);
  assert.equal(readFileSync(hostLog, 'utf8'), '');
  assert.equal(readFileSync(trace, 'utf8'), '');
  assert.doesNotMatch(result.stdout, /Acceptance initial-navigation timeout passed/);
  console.log('Host navigation-timeout runner freshness smoke passed');
} finally {
  rmSync(work, { recursive: true, force: true });
}
