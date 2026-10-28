import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';

// Exercise the runner's actual ownership and cleanup functions with inert shell
// substitutes. No host, JVM, GUI or system process is launched or signalled.
const source = readFileSync(new URL('../bin/run-flow-file-read-grant-fatal-shutdown', import.meta.url), 'utf8');
const matcher = source.match(/owned_pids\(\) \{\n[\s\S]*?\n\}\n/)[0];
const cleanup = source.match(/cleanup\(\) \{\n[\s\S]*?\n\}\n/)[0];
const work = mkdtempSync(join(tmpdir(), 'vatori-fatal-runner-smoke-'));
const build = join(work, 'build'), good = join(build, 'file-read-grant-fatal-shutdown.ABC123');
mkdirSync(good, { recursive: true });
let count = 0;
const run = (script, env = {}) => {
  const result = spawnSync('/bin/bash', ['-c', `set -euo pipefail\n${script}`], {
    encoding: 'utf8', timeout: 5000, env: { ...process.env, ...env },
  });
  assert.ifError(result.error); return result;
};
try {
  const host = `${good}/VaadinDesktopHost.app/Contents/MacOS/VaadinDesktopHost`;
  const jar = `${good}/VaadinDesktopHost.app/Contents/Resources/Sidecar/application.jar`;
  const rows = [`101 ${host}`, `102 /test/jdk/bin/java -jar ${jar}`, `103 ${host}.other`,
    `104 /other/jdk/bin/java -jar ${jar}`, `105 /test/jdk/bin/java -jar ${jar} extra`,
    `106 /bin/sh -c ${host}`, `107 /test/jdk/bin/java -jar ${jar}.other`];
  const matched = run(`ps() { printf '%s\\n' "$ROWS"; }\n${matcher}\nowned_pids`, {
    ROWS: rows.join('\n'), HOST_EXE: host, SIDECAR_JAR: jar, JAVA_HOME: '/test/jdk',
  });
  assert.equal(matched.status, 0); assert.equal(matched.stdout, '101\n102\n'); count += rows.length;
  const link = join(build, 'file-read-grant-fatal-shutdown.LINK12'); symlinkSync(good, link);
  for (const [candidate, allowed] of [[good, true], [build, false], [link, false],
    [`${good}.other`, false], [join(work, 'file-read-grant-fatal-shutdown.ABC123'), false]]) {
    if (candidate !== link) mkdirSync(candidate, { recursive: true });
    const r = run(`owned_pids() { :; }\nkill() { exit 97; }\nrm() { printf 'WOULD_REMOVE'; }\n${cleanup}\ncleanup 0`,
      { WORK: candidate, BUILD_ROOT: build, OPEN_WAIT_PID: '' });
    assert.equal(r.status, allowed ? 0 : 1); assert.equal(r.stdout, allowed ? 'WOULD_REMOVE' : ''); count++;
  }
  const refused = run(`owned_pids() { return 23; }\nkill() { exit 97; }\nrm() { exit 98; }\n${cleanup}\ncleanup 0`,
    { WORK: good, BUILD_ROOT: build, OPEN_WAIT_PID: '' });
  assert.equal(refused.status, 1); assert.match(refused.stderr, /retaining clone/); count++;
  const live = run(`owned_pids() { printf '424242\\n'; }\nkill() { return 1; }\nsleep() { :; }\nrm() { exit 98; }\n${cleanup}\ncleanup 0`,
    { WORK: good, BUILD_ROOT: build, OPEN_WAIT_PID: '' });
  assert.equal(live.status, 1); assert.match(live.stderr, /retaining clone/); count++;
  assert(source.indexOf(' watch "$STATE"') < source.indexOf(' verify "$STATE"'));
  assert(source.indexOf(' verify "$STATE"') < source.indexOf('--require-flow-ipc-file-read-grant-fatal-shutdown'));
  assert(source.indexOf('umask 077') < source.indexOf(': > "$HOST_LOG"')); count += 3;
  console.log(`Fatal file-grant runner smoke passed: ${count} ownership/cleanup/order cases (inert substitutes)`);
} finally { rmSync(work, { recursive: true, force: true }); }
