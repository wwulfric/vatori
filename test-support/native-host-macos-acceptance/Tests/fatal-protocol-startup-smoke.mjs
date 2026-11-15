import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { chmodSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync,
  realpathSync, renameSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
assert.ok(process.env.JAVA_HOME, 'JAVA_HOME is required');
const work = mkdtempSync(join(realpathSync(tmpdir()), 'vatori-fatal-protocol-smoke-'));
const classes = join(work, 'classes');
mkdirSync(classes);
// Compile a temporary test copy with a pre-publication barrier. No hook is
// added to the test app artifact or framework runtime. This forces mutation
// after the poll's first validation but before it observes the trigger.
const source = readFileSync(join(root, 'test-apps/flow-over-ipc-framework-app/src/main/java/dev/vatori/ipcframework/FlowFatalProtocolStartup.java'), 'utf8');
const marker = 'Path trigger = directory.resolve(TRIGGER);';
assert.equal(source.split(marker).length, 2);
const sourceCopy = join(work, 'FlowFatalProtocolStartup.java');
writeFileSync(sourceCopy, source.replace(marker, `
if ("1".equals(System.getenv("VATORI_FATAL_SMOKE_BARRIER"))) {
    System.out.println("PROBE_PREPUBLICATION"); System.out.flush();
    System.in.read();
}
${marker}`));
const compile = spawnSync(join(process.env.JAVA_HOME, 'bin/javac'), ['-d', classes,
  sourceCopy,
  join(root, 'test-support/native-host-macos-acceptance/Fixtures/FatalProtocolStartupProbe.java')], { encoding: 'utf8', timeout: 30000 });
const variable = 'VAADIN_DESKTOP_TEST_FATAL_PROTOCOL_DIR';
const fault = 'VFIPC {"version":2,"type":"error","payload":{},"error":null}';
let count = 0;

async function run(name, setup, action, expected, extraEnv = {}) {
  const dir = mkdtempSync(join(work, 'vatori-fatal-protocol.'));
  chmodSync(dir, 0o700);
  let environment = setup?.(dir) ?? dir;
  const env = { ...process.env };
  delete env[variable];
  delete env.VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR;
  delete env.VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR;
  delete env.VATORI_FATAL_SMOKE_BARRIER;
  Object.assign(env, extraEnv);
  if (environment !== false) env[variable] = environment;
  const child = spawn(join(process.env.JAVA_HOME, 'bin/java'), ['-cp', classes,
    'dev.vatori.ipcframework.FatalProtocolStartupProbe'], { env, stdio: ['pipe', 'pipe', 'pipe'] });
  let out = '', err = '', acted = false, failure;
  const timer = setTimeout(() => { failure = new Error(`${name}: child exceeded 5s`); child.kill('SIGKILL'); }, 5000);
  child.stdout.on('data', bytes => {
    out += bytes;
    if (!acted && out.includes(extraEnv.VATORI_FATAL_SMOKE_BARRIER === '1' ? 'PROBE_PREPUBLICATION' : 'PROBE_READY')) {
      acted = true;
      try {
        if (environment !== false) assert.equal(readFileSync(join(dir, 'startup.pid'), 'utf8'), `${child.pid}\n`);
        action?.(dir);
        if (extraEnv.VATORI_FATAL_SMOKE_BARRIER === '1') child.stdin.end('\n');
      } catch (error) { failure = error; child.kill('SIGKILL'); }
    }
  });
  child.stderr.on('data', bytes => { err += bytes; });
  const result = await new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('close', (code, signal) => resolve({ code, signal }));
  });
  clearTimeout(timer);
  if (failure) throw failure;
  assert.equal(result.signal, null, name);
  if (expected === 'startup-rejected') {
    assert.notEqual(result.code, 0, name);
    assert.ok(!out.includes('PROBE_READY'), name);
    assert.match(err, /startup configuration is invalid/, name);
  } else {
    assert.equal(result.code, 0, `${name}: ${err}`);
    assert.ok(acted, name);
    if (expected === 'trigger-rejected') assert.match(err, /trigger rejected/, name);
    else assert.equal(err, '', name);
  }
  assert.equal(out.split('\n').filter(line => line === fault).length, expected === 'fault' ? 1 : 0, name);
  assert.ok(!out.includes(dir) && !err.includes(dir), `${name}: path leaked`);
  count++;
}

const trigger = dir => writeFileSync(join(dir, 'trigger'), '', { flag: 'wx', mode: 0o600 });
try {
  assert.equal(compile.status, 0, compile.stderr);
  await run('disabled', () => false, null, 'silent');
  await run('waits for preparation', null, null, 'silent');
  await run('one shot', null, trigger, 'fault');
  await run('stale directory', dir => { writeFileSync(join(dir, 'old'), 'stale'); }, null, 'startup-rejected');
  await run('wide directory', dir => { chmodSync(dir, 0o755); }, null, 'startup-rejected');
  await run('relative directory', () => 'relative-path', null, 'startup-rejected');
  await run('symlink directory', dir => { const p = join(work, 'alias'); symlinkSync(work, p); return join(p, basename(dir)); }, null, 'startup-rejected');
  await run('nonempty trigger', null, dir => writeFileSync(join(dir, 'trigger'), 'x', { mode: 0o600 }), 'trigger-rejected');
  await run('wide trigger', null, dir => writeFileSync(join(dir, 'trigger'), '', { mode: 0o644 }), 'trigger-rejected');
  await run('symlink trigger', null, dir => symlinkSync(join(dir, 'startup.pid'), join(dir, 'trigger')), 'trigger-rejected');
  await run('hardlinked trigger', null, dir => { const p = join(work, 'empty'); writeFileSync(p, '', { mode: 0o600 }); linkSync(p, join(dir, 'trigger')); }, 'trigger-rejected');
  await run('extra entry', null, dir => { writeFileSync(join(dir, 'extra'), 'x'); trigger(dir); }, 'trigger-rejected');
  await run('replaced directory', null, dir => { renameSync(dir, dir + '-old'); mkdirSync(dir, { mode: 0o700 }); trigger(dir); }, 'trigger-rejected');
  await run('changed startup', null, dir => {
    const p = join(dir, 'startup.pid');
    writeFileSync(p, readFileSync(p, 'utf8').replace(/\d/g, '0')); trigger(dir);
  }, 'trigger-rejected');
  await run('startup changed between validation and publication', null, dir => {
    const p = join(dir, 'startup.pid');
    writeFileSync(p, readFileSync(p, 'utf8').replace(/\d/g, '0')); trigger(dir);
  }, 'trigger-rejected', { VATORI_FATAL_SMOKE_BARRIER: '1' });
  await run('extra entry between validation and publication', null, dir => {
    writeFileSync(join(dir, 'extra'), 'x'); trigger(dir);
  }, 'trigger-rejected', { VATORI_FATAL_SMOKE_BARRIER: '1' });
  await run('conflicting shutdown fixture', null, null, 'startup-rejected', { VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR: work });
  await run('conflicting recovery fixture', null, null, 'startup-rejected', { VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR: work });
  assert.ok(existsSync(classes));
  console.log(`Fatal protocol startup smoke passed: ${count} owned-JVM trigger cases; no host/Flow/grant-cleanup evidence`);
} finally {
  rmSync(work, { recursive: true, force: true });
}
