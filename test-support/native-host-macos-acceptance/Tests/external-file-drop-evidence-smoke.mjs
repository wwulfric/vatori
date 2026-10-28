import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { existsSync, linkSync, lstatSync, mkdtempSync, readFileSync, realpathSync, renameSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { prepareEvidence, verifyEvidence } from './external-file-drop-evidence.mjs';

const script = join(dirname(fileURLToPath(import.meta.url)), 'external-file-drop-evidence.mjs');
const work = realpathSync(mkdtempSync(join(tmpdir(), 'vatori-external-drop-evidence-smoke-')));
const digest = bytes => createHash('sha256').update(bytes).digest('hex');
let sequence = 0;
let rejectedCases = 0;
const save = (path, object) => writeFileSync(path, `${JSON.stringify(object, null, 2)}\n`, { mode: 0o600 });

function fixture() {
  const prepared = prepareEvidence(join(work, `run-${sequence++}`));
  const root = dirname(prepared.manifestPath);
  const manifest = JSON.parse(readFileSync(prepared.manifestPath, 'utf8'));
  const frame = { runId: manifest.runId, expectedFileName: manifest.fileName,
    expectedFileBytes: manifest.fileBytes, expectedFileSha256: manifest.fileSha256,
    hostProcessIdentifier: 501, processIdentifier: 502, recoveryGeneration: 0,
    windowId: 'main', sessionId: 'flow-ipc-session', uiId: 'main',
    sourceApplicationVerified: false, externalProvenanceRequired: true, generatedInput: false };
  const final = { ...frame, complete: true, errorFree: true, javaExactBytesAndHashObserved: true,
    dropFileName: manifest.fileName, dropFileBytes: manifest.fileBytes, dropFileType: '',
    formDataFileName: manifest.fileName, formDataFileBytes: manifest.fileBytes, formDataFileType: '',
    requestFileName: manifest.fileName, requestFileBytes: manifest.fileBytes, requestFileType: '',
    remainingFileName: manifest.fileName, remainingFileBytes: manifest.fileBytes,
    uploadStatus: `External file drop upload: handlerCalls=1, handlerFailures=0, fileName=${manifest.fileName}, declaredBytes=${manifest.fileBytes}, bytesRead=${manifest.fileBytes}, sha256=${manifest.fileSha256}, contentType=application/octet-stream, requestContentType=multipart/form-data, requestMethod=POST, multipart=true`,
    responseStatus: `External file drop response: responseCalls=1, successfulResponses=1, failedResponses=0, success=true, acceptedFiles=1, acceptedFile=${manifest.fileName}, rejectedFiles=0, exception=false`,
    listenerStatus: 'External file drop listeners: allFinished=1, allFinishedClient=false, rejected=0, removed=0' };
  const records = ['readyForExternalDrop', 'dropObserved', 'uploadObserved', 'frameworkAcceptance']
    .map(type => ({ direction: 'recv', type: `externalFileDrop.${type}`, at: 1,
      payload: type === 'frameworkAcceptance' ? final : { ...frame } }));
  const tracePath = join(root, 'host.out.log');
  const receiptPath = join(root, 'operator-receipt.json');
  const evidencePath = join(root, 'synthetic-operator-record.txt');
  // Synthetic input for validator tests only. The production CLI has no command
  // that creates or claims to observe an external operator receipt.
  writeFileSync(evidencePath, 'Synthetic validator test, not a real Finder run.\n', { mode: 0o600 });
  writeFileSync(tracePath, traceText(records), { mode: 0o600 });
  const receipt = { schemaVersion: 1, evidenceKind: 'external-operator-computer-use',
    sourceApplication: 'com.apple.finder', runId: manifest.runId,
    manifestSha256: digest(readFileSync(prepared.manifestPath)), traceSha256: digest(readFileSync(tracePath)),
    hostPid: 501, sidecarPid: 502, observedFileName: manifest.fileName,
    evidenceRefs: [{ path: evidencePath, sha256: digest(readFileSync(evidencePath)) }],
    sourceApplicationVerified: false, claim: 'external operator evidence present and bound' };
  save(receiptPath, receipt);
  return { ...prepared, root, manifest, records, final, tracePath, receiptPath, evidencePath, receipt };
}

function traceText(records) {
  return 'Unrelated launcher diagnostic\n' + records.map(record => `HOST_PROTOCOL ${JSON.stringify(record)}\n`).join('');
}

function rewriteTrace(fixture) {
  writeFileSync(fixture.tracePath, traceText(fixture.records));
  fixture.receipt.traceSha256 = digest(readFileSync(fixture.tracePath));
  save(fixture.receiptPath, fixture.receipt);
}

function rewriteManifest(fixture) {
  save(fixture.manifestPath, fixture.manifest);
  fixture.receipt.manifestSha256 = digest(readFileSync(fixture.manifestPath));
  save(fixture.receiptPath, fixture.receipt);
}

function rejects(label, mutate) {
  const value = fixture();
  const argumentsOverride = mutate(value);
  assert.throws(() => verifyEvidence(...(argumentsOverride
    ?? [value.manifestPath, value.tracePath, value.receiptPath])), undefined, label);
  rejectedCases++;
}

function runPrepare(path) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [script, 'prepare', path]);
    let stdout = '', stderr = '';
    child.stdout.on('data', chunk => { stdout += chunk; });
    child.stderr.on('data', chunk => { stderr += chunk; });
    child.on('error', reject);
    child.on('close', code => resolve({ code, stdout, stderr }));
  });
}

try {
  const valid = fixture();
  assert.equal(verifyEvidence(valid.manifestPath, valid.tracePath, valid.receiptPath).verified, true);
  assert.equal(lstatSync(valid.root).mode & 0o777, 0o700);
  assert.equal(lstatSync(join(valid.root, 'source')).mode & 0o777, 0o700);
  assert.equal(lstatSync(valid.sourcePath).mode & 0o777, 0o600);
  assert.equal(lstatSync(valid.manifestPath).mode & 0o777, 0o600);
  assert.equal(readFileSync(valid.sourcePath).length, 65573);
  assert.ok(new Set(readFileSync(valid.sourcePath)).size > 1);
  assert.equal(existsSync(join(valid.root, 'operatorReceipt.json')), false);
  const original = readFileSync(valid.sourcePath);
  assert.throws(() => prepareEvidence(valid.root), undefined, 'prepare cannot overwrite an existing run');
  assert.deepEqual(readFileSync(valid.sourcePath), original);
  assert.throws(() => prepareEvidence(join(work, 'absent-parent', 'run')), undefined, 'prepare cannot recursively create parents');
  assert.equal(existsSync(join(work, 'absent-parent')), false);
  assert.throws(() => prepareEvidence('relative-run'), undefined, 'relative paths are not evidence roots');
  assert.throws(() => prepareEvidence(`${work}/parent/../run`), undefined, 'dot segments are rejected');
  const parentLink = join(work, 'parent-link');
  symlinkSync(work, parentLink);
  assert.throws(() => prepareEvidence(join(parentLink, 'run')), undefined, 'symlink parents are rejected');

  const concurrentRoot = join(work, 'concurrent-run');
  const concurrent = await Promise.all([runPrepare(concurrentRoot), runPrepare(concurrentRoot)]);
  assert.deepEqual(concurrent.map(result => result.code).sort(), [0, 1], 'concurrent creation has exactly one winner');
  const winner = JSON.parse(concurrent.find(result => result.code === 0).stdout);
  assert.equal(winner.manifestPath, join(concurrentRoot, 'manifest.json'));
  assert.equal(winner.fileSha256, digest(readFileSync(winner.sourcePath)));
  assert.equal(winner.manifestSha256, digest(readFileSync(winner.manifestPath)));
  assert.equal(existsSync(join(concurrentRoot, 'operator-receipt.json')), false, 'prepare cannot manufacture provenance');

  const cli = spawnSync(process.execPath, [script, 'verify', valid.manifestPath, valid.tracePath, valid.receiptPath], { encoding: 'utf8' });
  assert.equal(cli.status, 0, cli.stderr);
  assert.equal(JSON.parse(cli.stdout).sourceApplicationVerified, false);
  assert.equal(JSON.parse(cli.stdout).claim, 'external operator evidence present and bound');
  assert.equal(spawnSync(process.execPath, [script, 'receipt'], { encoding: 'utf8' }).status, 1);

  const manual = fixture();
  manual.receipt.evidenceKind = 'external-operator-manual';
  manual.receipt.operatorStatement = 'Synthetic validator case: an operator statement would describe the actual Finder drag.';
  save(manual.receiptPath, manual.receipt);
  const manualResult = verifyEvidence(manual.manifestPath, manual.tracePath, manual.receiptPath);
  assert.equal(manualResult.verified, true);
  assert.equal(manualResult.sourceApplicationVerified, false);
  assert.equal(manualResult.claim, 'external operator evidence present and bound');
  for (const statement of [undefined, '', ' \n\t ', 1, null]) {
    rejects(`manual operator statement ${JSON.stringify(statement)}`, f => {
      f.receipt.evidenceKind = 'external-operator-manual';
      f.receipt.operatorStatement = statement;
      save(f.receiptPath, f.receipt);
    });
  }
  rejects('unknown external observation mode with statement', f => {
    f.receipt.evidenceKind = 'external-operator-other';
    f.receipt.operatorStatement = 'A statement cannot authorize an unknown evidence mode.';
    save(f.receiptPath, f.receipt);
  });

  rejects('missing manifest', f => { unlinkSync(f.manifestPath); });
  rejects('missing source', f => { unlinkSync(f.sourcePath); });
  rejects('changed source contents', f => {
    const bytes = readFileSync(f.sourcePath); bytes[10] ^= 255; writeFileSync(f.sourcePath, bytes);
  });
  rejects('source size changed', f => { writeFileSync(f.sourcePath, Buffer.alloc(65572)); });
  rejects('source symlink', f => {
    const moved = join(f.root, 'moved-source'); renameSync(f.sourcePath, moved); symlinkSync(moved, f.sourcePath);
  });
  rejects('source hard link', f => { linkSync(f.sourcePath, join(f.root, 'source-hard-link')); });
  for (const [field, value] of [['schemaVersion', 2], ['runId', 'not-a-uuid'], ['createdAt', 'yesterday'],
    ['fileBytes', 65572], ['fileSha256', '0'.repeat(64)], ['fileName', '../outside.bin'], ['sourcePath', valid.sourcePath]]) {
    rejects(`manifest ${field}`, f => { f.manifest[field] = value; rewriteManifest(f); });
  }
  rejects('missing manifest digest', f => { delete f.manifest.fileSha256; rewriteManifest(f); });
  rejects('unsafe manifest argument', f => [`${f.root}/source/../manifest.json`, f.tracePath, f.receiptPath]);
  rejects('missing trace', f => { unlinkSync(f.tracePath); });
  rejects('changed trace binding', f => { writeFileSync(f.tracePath, readFileSync(f.tracePath, 'utf8') + 'changed\n'); });
  rejects('malformed protocol JSON', f => {
    writeFileSync(f.tracePath, 'HOST_PROTOCOL {invalid}\n');
    f.receipt.traceSha256 = digest(readFileSync(f.tracePath)); save(f.receiptPath, f.receipt);
  });
  for (const type of ['readyForExternalDrop', 'frameworkAcceptance']) {
    rejects(`missing ${type}`, f => { f.records = f.records.filter(record => record.type !== `externalFileDrop.${type}`); rewriteTrace(f); });
    rejects(`duplicate ${type}`, f => { f.records.push(f.records.find(record => record.type === `externalFileDrop.${type}`)); rewriteTrace(f); });
  }
  rejects('reversed ready and final', f => { f.records.reverse(); rewriteTrace(f); });
  for (const [field, value] of [['hostProcessIdentifier', 503], ['processIdentifier', 503], ['recoveryGeneration', 1],
    ['expectedFileName', 'different.bin'], ['expectedFileBytes', 65572], ['expectedFileSha256', '0'.repeat(64)],
    ['windowId', 'secondary'], ['sourceApplicationVerified', true], ['generatedInput', true]]) {
    rejects(`trace ${field}`, f => { f.final[field] = value; rewriteTrace(f); });
  }
  rejects('intermediate receipt PID changed', f => { f.records[1].payload.processIdentifier++; rewriteTrace(f); });
  rejects('wrong receipt direction', f => { f.records[0].direction = 'send'; rewriteTrace(f); });
  rejects('boolean-only Java proof cannot replace actual Java text', f => { delete f.final.uploadStatus; rewriteTrace(f); });
  for (const [from, to] of [['handlerCalls=1', 'handlerCalls=2'], ['handlerFailures=0', 'handlerFailures=1'],
    ['bytesRead=65573', 'bytesRead=65572'], ['declaredBytes=65573', 'declaredBytes=65572'],
    ['multipart=true', 'multipart=false'], ['requestMethod=POST', 'requestMethod=GET']]) {
    rejects(`actual Java ${from}`, f => { f.final.uploadStatus = f.final.uploadStatus.replace(from, to); rewriteTrace(f); });
  }
  rejects('Java digest differs', f => { f.final.uploadStatus = f.final.uploadStatus.replace(f.fileSha256, '0'.repeat(64)); rewriteTrace(f); });
  rejects('Java failed response', f => { f.final.responseStatus = f.final.responseStatus.replace('success=true', 'success=false'); rewriteTrace(f); });
  rejects('extra allFinished listener', f => { f.final.listenerStatus = f.final.listenerStatus.replace('allFinished=1', 'allFinished=2'); rewriteTrace(f); });
  for (const prefix of ['dropFile', 'formDataFile', 'requestFile', 'remainingFile']) {
    rejects(`actual ${prefix} mismatch`, f => { f.final[`${prefix}Bytes`]--; rewriteTrace(f); });
  }
  rejects('missing external receipt', f => { unlinkSync(f.receiptPath); });
  for (const [field, value] of [['schemaVersion', 2], ['evidenceKind', 'generated-appkit'],
    ['sourceApplication', 'com.apple.TextEdit'], ['runId', valid.runId], ['manifestSha256', '0'.repeat(64)],
    ['traceSha256', '0'.repeat(64)], ['hostPid', 999], ['sidecarPid', 999], ['observedFileName', 'different.bin'],
    ['sourceApplicationVerified', true], ['claim', 'machine verified Finder']]) {
    rejects(`operator ${field}`, f => { f.receipt[field] = value; save(f.receiptPath, f.receipt); });
  }
  rejects('missing refs', f => { delete f.receipt.evidenceRefs; save(f.receiptPath, f.receipt); });
  rejects('empty refs', f => { f.receipt.evidenceRefs = []; save(f.receiptPath, f.receipt); });
  rejects('missing ref hash', f => { delete f.receipt.evidenceRefs[0].sha256; save(f.receiptPath, f.receipt); });
  rejects('changed external evidence', f => { writeFileSync(f.evidencePath, 'changed'); });
  rejects('missing external evidence', f => { unlinkSync(f.evidencePath); });
  rejects('empty external evidence', f => {
    writeFileSync(f.evidencePath, ''); f.receipt.evidenceRefs[0].sha256 = digest(Buffer.alloc(0)); save(f.receiptPath, f.receipt);
  });
  rejects('duplicate evidence reference', f => { f.receipt.evidenceRefs.push(f.receipt.evidenceRefs[0]); save(f.receiptPath, f.receipt); });
  rejects('trace is not external evidence', f => {
    f.receipt.evidenceRefs = [{ path: f.tracePath, sha256: f.receipt.traceSha256 }]; save(f.receiptPath, f.receipt);
  });
  rejects('unsafe evidence reference', f => { f.receipt.evidenceRefs[0].path = `${f.root}/source/../synthetic-operator-record.txt`; save(f.receiptPath, f.receipt); });
  rejects('evidence symlink', f => {
    const moved = join(f.root, 'moved-evidence'); renameSync(f.evidencePath, moved); symlinkSync(moved, f.evidencePath);
  });
  rejects('evidence hard link', f => { linkSync(f.evidencePath, join(f.root, 'linked-evidence')); });
  rejects('trace symlink', f => {
    const moved = join(f.root, 'moved-trace'); renameSync(f.tracePath, moved); symlinkSync(moved, f.tracePath);
  });
  rejects('receipt symlink', f => {
    const moved = join(f.root, 'moved-receipt'); renameSync(f.receiptPath, moved); symlinkSync(moved, f.receiptPath);
  });
  const typed = fixture();
  for (const key of ['dropFileType', 'formDataFileType', 'requestFileType']) typed.final[key] = 'application/octet-stream';
  rewriteTrace(typed);
  assert.equal(verifyEvidence(typed.manifestPath, typed.tracePath, typed.receiptPath).verified, true,
    'explicit native MIME and empty native MIME fallback are both valid');
  console.log(`External file drop evidence smoke passed (${rejectedCases} rejection cases plus CLI/concurrent creation)`);
} finally {
  // This exact directory was created by this smoke; it never contains user input.
  rmSync(work, { recursive: true, force: true });
}
