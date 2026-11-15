import assert from 'node:assert/strict';
import {copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync,
  rmSync, statSync, symlinkSync, readlinkSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {spawnSync} from 'node:child_process';

const work = mkdtempSync(join(tmpdir(), 'vatori-artifact-storage-'));
const bin = join(work, 'test-support/native-host-macos-acceptance/bin');
const build = join(work, '.build/native-hosts/macos');
const invoke = (command, args, env = {}) => {
  const result = spawnSync(command, args, {
    encoding: 'utf8', timeout: 15000, env: {...process.env, ...env},
  });
  assert.ifError(result.error);
  return result;
};
try {
  mkdirSync(bin, {recursive: true});
  mkdirSync(build, {recursive: true});
  const lock = join(bin, 'with-package-lock');
  copyFileSync(new URL('../bin/with-package-lock', import.meta.url), lock);
  const marker = join(work, 'child-ran');
  const run = limit => invoke('bash', [lock, '--', 'touch', marker],
    {VATORI_ACCEPTANCE_ARTIFACT_LIMIT_MIB: limit});
  assert.equal(run('1').status, 0);
  rmSync(marker);
  const evidence = join(build, 'previous-evidence.fixture');
  mkdirSync(evidence);
  writeFileSync(join(evidence, 'trace.log'), Buffer.alloc(2 * 1024 * 1024, 65));
  const blocked = run('1');
  assert.equal(blocked.status, 73, blocked.stderr);
  assert.match(blocked.stderr, /previous-evidence.fixture/);
  assert.equal(existsSync(marker), false);
  assert.equal(statSync(join(evidence, 'trace.log')).size, 2 * 1024 * 1024);
  assert.equal(existsSync(join(build, '.acceptance-package.lock')), false);
  assert.equal(run('4').status, 0);
  rmSync(marker);
  assert.equal(run('0').status, 0);
  rmSync(marker);
  for (const invalid of ['-1', '01', 'abc', '1048577']) {
    assert.equal(run(invalid).status, 2);
    assert.equal(existsSync(marker), false);
    assert.equal(existsSync(join(build, '.acceptance-package.lock')), false);
  }
  if (process.platform === 'darwin') {
    const original = join(work, 'original.app');
    const clone = join(work, 'clone.app');
    mkdirSync(original);
    writeFileSync(join(original, 'host'), 'original', {mode: 0o755});
    symlinkSync('host', join(original, 'link'));
    const copied = invoke('/bin/cp', ['-cRp', original, clone]);
    assert.equal(copied.status, 0, copied.stderr);
    assert.equal(readlinkSync(join(clone, 'link')), 'host');
    assert.equal(statSync(join(clone, 'host')).mode & 0o777, 0o755);
    writeFileSync(join(clone, 'host'), 'modified');
    assert.equal(readFileSync(join(original, 'host'), 'utf8'), 'original');
    rmSync(original, {recursive: true});
    assert.equal(readFileSync(join(clone, 'host'), 'utf8'), 'modified');
  }
  console.log('PASS: artifact budget, override, lock cleanup and macOS copy isolation');
} finally {
  rmSync(work, {recursive: true, force: true});
}
