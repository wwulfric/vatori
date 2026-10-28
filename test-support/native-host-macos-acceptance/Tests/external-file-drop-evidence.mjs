import { constants, closeSync, fstatSync, lstatSync, mkdirSync, openSync, readFileSync, realpathSync, writeFileSync } from 'node:fs';
import { createHash, randomBytes, randomUUID } from 'node:crypto';
import { basename, dirname, isAbsolute, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const FILE_BYTES = 65573;
const SHA256 = /^[0-9a-f]{64}$/;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const CLAIM = 'external operator evidence present and bound';
const READY = 'externalFileDrop.readyForExternalDrop';
const ACCEPTED = 'externalFileDrop.frameworkAcceptance';
const RECEIPTS = new Set([READY, 'externalFileDrop.dropObserved', 'externalFileDrop.uploadObserved', ACCEPTED]);

function requireCondition(condition, message) {
  if (!condition) throw new Error(message);
}

function absolutePath(path, label) {
  requireCondition(typeof path === 'string' && isAbsolute(path)
    && !path.includes('\0') && normalize(path) === path
    && !path.split('/').some(part => part === '.' || part === '..'), `${label} must be an absolute normalized path`);
  return path;
}

function directory(path, label, owned = true) {
  absolutePath(path, label);
  const stat = lstatSync(path);
  requireCondition(stat.isDirectory() && !stat.isSymbolicLink() && realpathSync(path) === path,
    `${label} must be a real canonical directory`);
  requireCondition(!owned || stat.uid === process.getuid(), `${label} must belong to the current user`);
}

function readRegular(path, label, maximum = 1024 * 1024) {
  absolutePath(path, label);
  requireCondition(realpathSync(path) === path, `${label} must not traverse a symbolic link`);
  const before = lstatSync(path);
  requireCondition(before.isFile() && !before.isSymbolicLink() && before.nlink === 1
    && before.uid === process.getuid(), `${label} must be an owned single-link regular file`);
  requireCondition(before.size <= maximum, `${label} exceeds its bounded size`);
  const descriptor = openSync(path, constants.O_RDONLY | constants.O_NOFOLLOW);
  try {
    const opened = fstatSync(descriptor);
    requireCondition(opened.dev === before.dev && opened.ino === before.ino
      && opened.size === before.size && opened.isFile() && opened.nlink === 1,
    `${label} changed before reading`);
    const bytes = readFileSync(descriptor);
    const after = fstatSync(descriptor);
    requireCondition(bytes.length === before.size && after.size === before.size
      && after.mtimeMs === before.mtimeMs && after.ctimeMs === before.ctimeMs,
    `${label} changed while reading`);
    return bytes;
  } finally {
    closeSync(descriptor);
  }
}

const digest = bytes => createHash('sha256').update(bytes).digest('hex');

function json(bytes, label) {
  const value = JSON.parse(bytes.toString('utf8'));
  requireCondition(value !== null && typeof value === 'object' && !Array.isArray(value), `${label} must be an object`);
  return value;
}

export function prepareEvidence(runDirectory) {
  absolutePath(runDirectory, 'run directory');
  directory(dirname(runDirectory), 'run parent', false);
  // No recursive creation or overwrite: concurrent prepares have exactly one
  // winner. A partial failure leaves its new directory available for inspection.
  mkdirSync(runDirectory, { mode: 0o700 });
  const sourceDirectory = join(runDirectory, 'source');
  mkdirSync(sourceDirectory, { mode: 0o700 });
  const runId = randomUUID();
  const fileName = `vatori-finder-${runId}.bin`;
  const sourcePath = join(sourceDirectory, fileName);
  let bytes;
  do { bytes = randomBytes(FILE_BYTES); } while (new Set(bytes).size < 2);
  writeFileSync(sourcePath, bytes, { flag: 'wx', mode: 0o600 });
  const manifest = { schemaVersion: 1, runId, createdAt: new Date().toISOString(),
    sourcePath, fileName, fileBytes: bytes.length, fileSha256: digest(bytes) };
  const manifestPath = join(runDirectory, 'manifest.json');
  const manifestBytes = Buffer.from(`${JSON.stringify(manifest, null, 2)}\n`);
  writeFileSync(manifestPath, manifestBytes, { flag: 'wx', mode: 0o600 });
  return { manifestPath, ...manifest, manifestSha256: digest(manifestBytes) };
}

function validateManifest(manifestPath, manifest) {
  requireCondition(basename(manifestPath) === 'manifest.json', 'manifest must retain its fixed filename');
  directory(dirname(manifestPath), 'run directory');
  requireCondition(manifest.schemaVersion === 1 && UUID.test(manifest.runId), 'invalid manifest schema or run id');
  requireCondition(typeof manifest.createdAt === 'string'
    && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(manifest.createdAt)
    && Number.isFinite(Date.parse(manifest.createdAt))
    && new Date(manifest.createdAt).toISOString() === manifest.createdAt, 'invalid manifest creation time');
  const sourceDirectory = join(dirname(manifestPath), 'source');
  directory(sourceDirectory, 'source directory');
  requireCondition(manifest.fileName === `vatori-finder-${manifest.runId}.bin`
    && manifest.sourcePath === join(sourceDirectory, manifest.fileName), 'source must be the run-owned fixture');
  requireCondition(manifest.fileBytes === FILE_BYTES && SHA256.test(manifest.fileSha256), 'invalid manifest bytes or digest');
  const source = readRegular(manifest.sourcePath, 'source', FILE_BYTES);
  requireCondition(source.length === manifest.fileBytes && digest(source) === manifest.fileSha256
    && new Set(source).size > 1, 'source file no longer matches the independent manifest');
}

function frameMatches(payload, manifest) {
  requireCondition(payload && typeof payload === 'object'
    && payload.runId === manifest.runId && payload.expectedFileName === manifest.fileName
    && payload.expectedFileBytes === manifest.fileBytes && payload.expectedFileSha256 === manifest.fileSha256,
  'trace metadata does not match the independent manifest');
  requireCondition(payload.windowId === 'main' && payload.sessionId === 'flow-ipc-session'
    && payload.uiId === 'main' && payload.recoveryGeneration === 0, 'trace has a different owner or generation');
  requireCondition(Number.isSafeInteger(payload.hostProcessIdentifier) && payload.hostProcessIdentifier > 0
    && Number.isSafeInteger(payload.processIdentifier) && payload.processIdentifier > 0
    && payload.hostProcessIdentifier !== payload.processIdentifier, 'trace has invalid process identities');
  requireCondition(payload.sourceApplicationVerified === false && payload.externalProvenanceRequired === true
    && payload.generatedInput === false, 'trace must not claim machine-authenticated Finder provenance');
}

function traceEvidence(bytes, manifest) {
  const records = [];
  for (const line of bytes.toString('utf8').split(/\r?\n/)) {
    if (!line.startsWith('HOST_PROTOCOL ')) continue;
    const record = json(Buffer.from(line.slice('HOST_PROTOCOL '.length)), 'trace record');
    if (RECEIPTS.has(record.type)) {
      requireCondition(record.direction === 'recv', 'external drop receipt has the wrong direction');
      frameMatches(record.payload, manifest);
      records.push(record);
    }
  }
  const ready = records.filter(record => record.type === READY);
  const accepted = records.filter(record => record.type === ACCEPTED);
  requireCondition(ready.length === 1 && accepted.length === 1
    && records.indexOf(ready[0]) < records.indexOf(accepted[0]), 'trace needs one ready receipt before one acceptance');
  const first = ready[0].payload, final = accepted[0].payload;
  requireCondition(records.every(record => record.payload.hostProcessIdentifier === first.hostProcessIdentifier
    && record.payload.processIdentifier === first.processIdentifier), 'trace process identity changed');
  requireCondition(final.complete === true && final.errorFree === true
    && final.javaExactBytesAndHashObserved === true, 'trace did not complete the actual Java upload proof');
  for (const prefix of ['dropFile', 'formDataFile', 'requestFile', 'remainingFile']) {
    requireCondition(final[`${prefix}Name`] === manifest.fileName && final[`${prefix}Bytes`] === manifest.fileBytes,
      'actual browser file metadata does not match the independent manifest');
  }
  requireCondition(typeof final.dropFileType === 'string'
    && final.formDataFileType === final.dropFileType && final.requestFileType === final.dropFileType,
  'actual browser file media types disagree');
  const partType = final.dropFileType || 'application/octet-stream';
  requireCondition(/^[a-zA-Z0-9!#$&^_.+-]+\/[a-zA-Z0-9!#$&^_.+-]+$/.test(partType), 'invalid actual file media type');
  const upload = `External file drop upload: handlerCalls=1, handlerFailures=0, fileName=${manifest.fileName}, declaredBytes=${manifest.fileBytes}, bytesRead=${manifest.fileBytes}, sha256=${manifest.fileSha256}, contentType=${partType}, requestContentType=multipart/form-data, requestMethod=POST, multipart=true`;
  const response = `External file drop response: responseCalls=1, successfulResponses=1, failedResponses=0, success=true, acceptedFiles=1, acceptedFile=${manifest.fileName}, rejectedFiles=0, exception=false`;
  requireCondition(final.uploadStatus === upload && final.responseStatus === response
    && final.listenerStatus === 'External file drop listeners: allFinished=1, allFinishedClient=false, rejected=0, removed=0',
  'actual Java upload, response or listener status does not match the independent manifest');
  return { hostPid: first.hostProcessIdentifier, sidecarPid: first.processIdentifier };
}

export function verifyEvidence(manifestPath, tracePath, operatorReceiptPath) {
  const manifestBytes = readRegular(manifestPath, 'manifest');
  const manifest = json(manifestBytes, 'manifest');
  validateManifest(manifestPath, manifest);
  const traceBytes = readRegular(tracePath, 'trace', 128 * 1024 * 1024);
  const identities = traceEvidence(traceBytes, manifest);
  const receipt = json(readRegular(operatorReceiptPath, 'operator receipt'), 'operator receipt');
  const manifestSha256 = digest(manifestBytes), traceSha256 = digest(traceBytes);
  requireCondition(['external-operator-computer-use', 'external-operator-manual'].includes(receipt.evidenceKind),
    'operator receipt must identify a supported external observation mode');
  requireCondition(receipt.evidenceKind !== 'external-operator-manual'
    || typeof receipt.operatorStatement === 'string' && receipt.operatorStatement.trim().length > 0,
  'manual evidence requires a nonempty statement from the actual operator');
  requireCondition(receipt.schemaVersion === 1
    && receipt.sourceApplication === 'com.apple.finder' && receipt.runId === manifest.runId
    && receipt.manifestSha256 === manifestSha256 && receipt.traceSha256 === traceSha256
    && receipt.hostPid === identities.hostPid && receipt.sidecarPid === identities.sidecarPid
    && receipt.observedFileName === manifest.fileName, 'operator receipt is not bound to this manifest, trace and process pair');
  requireCondition(receipt.sourceApplicationVerified === false && receipt.claim === CLAIM,
    'operator receipt must state its non-authenticating evidence boundary');
  requireCondition(Array.isArray(receipt.evidenceRefs) && receipt.evidenceRefs.length > 0
    && receipt.evidenceRefs.length <= 32, 'operator receipt requires bounded external evidence references');
  const inputs = new Set([manifestPath, tracePath, operatorReceiptPath, manifest.sourcePath]);
  const seen = new Set();
  for (const reference of receipt.evidenceRefs) {
    requireCondition(reference && typeof reference === 'object' && !Array.isArray(reference)
      && typeof reference.path === 'string' && SHA256.test(reference.sha256)
      && !inputs.has(reference.path) && !seen.has(reference.path), 'invalid or self-referential evidence reference');
    seen.add(reference.path);
    const evidence = readRegular(reference.path, 'operator evidence', 64 * 1024 * 1024);
    requireCondition(evidence.length > 0 && digest(evidence) === reference.sha256, 'operator evidence changed or is empty');
  }
  // This binds an operator's assertion to immutable byte digests. It cannot
  // authenticate which external application initiated a browser drag session.
  return { verified: true, verificationKind: 'external-evidence-binding-only', runId: manifest.runId,
    manifestSha256, traceSha256, ...identities, fileName: manifest.fileName,
    fileBytes: manifest.fileBytes, fileSha256: manifest.fileSha256,
    evidenceRefCount: receipt.evidenceRefs.length, sourceApplicationVerified: false, claim: CLAIM };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [command, ...args] = process.argv.slice(2);
    let result;
    if (command === 'prepare' && args.length === 1) result = prepareEvidence(...args);
    else if (command === 'verify' && args.length === 3) result = verifyEvidence(...args);
    else throw new Error('usage: prepare <new-run-dir> | verify <manifest> <trace> <operatorReceipt>');
    console.log(JSON.stringify(result));
  } catch (error) {
    console.error(`External file drop evidence failed: ${error.message}`);
    process.exitCode = 1;
  }
}
