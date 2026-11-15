import assert from 'node:assert/strict';
import fs from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const runner = fileURLToPath(new URL('../bin/run-product-replacement', import.meta.url));
for (const args of [['invalid'], ['rollback', 'extra']]) {
  const result = spawnSync('bash', [runner, ...args], {encoding: 'utf8', timeout: 5000});
  assert.equal(result.status, 2);
  assert.match(result.stderr, /Usage:/);
  assert.equal(result.stdout, '');
}
const missingJdk = spawnSync('bash', [runner, 'rollback'], {
  encoding: 'utf8', timeout: 5000, env: {...process.env, JAVA_HOME: '/nonexistent-vatori-test-jdk'}
});
assert.equal(missingJdk.status, 2);
assert.equal(missingJdk.stdout, '');
const source = fs.readFileSync(runner, 'utf8');
assert.ok(source.includes('--env VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS=180'));
assert.ok(source.includes('SECONDS - started > 240'));
const start = source.indexOf('owned_pids() {');
const end = source.indexOf('\nfinish() {', start);
assert.ok(start >= 0 && end > start);
// Execute only the exact runner matcher, with an inert ps source. No signals,
// build, app launch or permission manipulation occurs in this test.
const script = `
EXE='/private/test/App.app/Contents/MacOS/Host'
JAR='/private/test/App.app/Contents/Resources/Sidecar/application.jar'
CANDIDATE_JAR='/private/test/App.app/Contents/Resources/RuntimeCandidates/next/application.jar'
JAVA_HOME='/private/test/jdk'
ps() {
  printf '%s\\n' \
    "101 $EXE" "102 $JAVA_HOME/bin/java -jar $JAR" \
    "103 $JAVA_HOME/bin/java -jar $CANDIDATE_JAR" \
    "104 $EXE-other" "105 $JAVA_HOME/bin/java -jar $CANDIDATE_JAR-other" \
    "106 /private/other/java -jar $JAR" "107 $JAVA_HOME/bin/java -jar /private/other/app.jar"
}
${source.slice(start, end)}
owned_pids
`;
const matched = spawnSync('bash', ['-c', script], {encoding: 'utf8', timeout: 5000});
assert.equal(matched.status, 0, matched.stderr);
assert.equal(matched.stdout.trim(), '101\n102\n103');
console.log('Product replacement runner guard/matcher smoke passed (no GUI or process termination)');
