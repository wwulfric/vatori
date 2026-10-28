import { closeSync, constants, fstatSync, lstatSync, openSync, readSync, readdirSync,
  realpathSync, writeSync } from 'node:fs';
import { basename, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';

const receiptType = 'flowFileReadGrant.fatalExitObserved';
const positive = value => Number.isSafeInteger(value) && value > 0;
function require(value, message) { if (!value) throw new Error(message); }
const sameFile = (a, b) => a.dev === b.dev && a.ino === b.ino;

// Bounded descriptor reads; never follow the final path component or read a FIFO.
function readOwned(fd, limit) {
  const stat = fstatSync(fd);
  require(stat.isFile() && stat.nlink === 1 && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o600 && stat.size <= limit, 'Expected bounded owned mode-0600 file');
  const bytes = Buffer.alloc(limit + 1);
  let size = 0, count;
  while (size <= limit && (count = readSync(fd, bytes, size, limit + 1 - size, size)) > 0) size += count;
  require(size <= limit, 'File grew beyond the evidence limit');
  return bytes.subarray(0, size).toString('utf8');
}
const readFlags = constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK;
function state(directory, triggered) {
  require(resolve(directory) === realpathSync(directory)
    && /^vatori-fatal-protocol\.[A-Za-z0-9]{6}$/.test(basename(directory)), 'Noncanonical private directory');
  const stat = lstatSync(directory);
  require(stat.isDirectory() && stat.uid === process.getuid() && (stat.mode & 0o777) === 0o700,
    'Expected owned mode-0700 private directory');
  const names = readdirSync(directory).sort().join(',');
  require(names === (triggered ? 'startup.pid,trigger' : 'startup.pid'), 'Unexpected private state entries');
  const fd = openSync(join(directory, 'startup.pid'), readFlags);
  let text, marker;
  try { text = readOwned(fd, 32); marker = fstatSync(fd); } finally { closeSync(fd); }
  require(/^[1-9][0-9]*\n$/.test(text) && positive(Number(text.trim())), 'Invalid startup PID');
  if (triggered) {
    const trigger = openSync(join(directory, 'trigger'), readFlags);
    try { require(readOwned(trigger, 0) === '', 'Trigger must be empty'); } finally { closeSync(trigger); }
  }
  return { stat, marker, pid: Number(text.trim()) };
}
function trace(fd, final) {
  const text = readOwned(fd, 16 * 1024 * 1024);
  if (final) require(text.endsWith('\n'), 'Incomplete final trace');
  const lines = text.split('\n');
  lines.pop(); // Ignore an in-progress final line only while watching.
  return lines.filter(line => line.startsWith('HOST_PROTOCOL ')).map(line => {
    const event = JSON.parse(line.slice(14));
    require(event && typeof event.type === 'string' && positive(event.at)
      && ['send', 'recv'].includes(event.direction) && event.payload
      && typeof event.payload === 'object' && !Array.isArray(event.payload), 'Invalid trace event');
    return event;
  });
}
function one(events, type, direction = 'recv') {
  const matches = events.filter(e => e.type === type);
  require(matches.length === 1 && matches[0].direction === direction, `Expected one ${direction} ${type}`);
  return matches[0];
}
function preparation(events, pid) {
  const started = one(events, 'sidecar.started');
  require(started.payload.generation === 1 && started.payload.processIdentifier === pid, 'Startup identity mismatch');
  const prepared = one(events, 'flowFileReadGrant.hostShutdownPrepared');
  require(prepared.payload.initialProcessIdentifier === pid && prepared.payload.recoveryGeneration === 0
    && prepared.payload.firstSelectionOutcome === 'SELECTED', 'Prepared identity/selection mismatch');
  for (const field of ['complete', 'standardBundleMode', 'flowClientOwned', 'externalPanelSelection',
    'ownerWindowsOpen', 'operationsPending', 'windowRetained', 'webViewRetained', 'secondaryWindowRetained',
    'secondaryWebViewRetained', 'balancedDrainObserved', 'errorFree'])
    require(prepared.payload[field] === true, `Preparation incomplete: ${field}`);
  const created = one(events, 'fileReadGrant.created');
  require(created.payload.diagnosticId === 1 && created.payload.sidecarGeneration === 1
    && created.payload.windowId === 'file-read-grant-secondary'
    && typeof created.payload.securityScoped === 'boolean', 'Expected original live grant');
  require(events.indexOf(started) < events.indexOf(created) && events.indexOf(created) < events.indexOf(prepared),
    'Preparation order mismatch');
  return prepared;
}
function noRecovery(events) {
  require(!events.some(e => /^sidecar\.(recovery|replacement|rollback)/.test(e.type)), 'Unexpected sidecar transition');
}
export function requireAbsent(pid, probe = process.kill) {
  require(positive(pid), 'Invalid process identity');
  try { probe(pid, 0); } catch (error) {
    require(error.code === 'ESRCH', `Cannot confirm process absence: ${error.code}`);
    return;
  }
  throw new Error('Process remains alive; cleanup cannot rescue acceptance');
}

export function armFatalTrigger(directory, tracePath) {
  const initial = state(directory, false);
  const fd = openSync(tracePath, readFlags);
  let events;
  try { events = trace(fd, false); } finally { closeSync(fd); }
  preparation(events, initial.pid);
  noRecovery(events);
  require(!events.some(e => ['error', 'shutdown', 'sidecar.exit', 'fileReadGrant.invalidated',
    'fileReadGrant.closed', 'fileReadGrant.read', 'host.failureShutdownStarted', receiptType].includes(e.type)),
  'Grant was not live before fault trigger');
  process.kill(initial.pid, 0); // Signal 0 only: the privately identified JVM must still exist.
  const current = state(directory, false);
  require(sameFile(initial.stat, current.stat) && sameFile(initial.marker, current.marker)
    && initial.pid === current.pid, 'Private state changed before trigger');
  const trigger = openSync(join(directory, 'trigger'), constants.O_WRONLY | constants.O_CREAT
    | constants.O_EXCL | constants.O_NOFOLLOW, 0o600);
  closeSync(trigger);
}

export function verifyFatalExit(directory, tracePath, append = false) {
  const initial = state(directory, true);
  const fd = openSync(tracePath, (append ? constants.O_RDWR | constants.O_APPEND : constants.O_RDONLY)
    | constants.O_NOFOLLOW | constants.O_NONBLOCK);
  try {
    const events = trace(fd, true), pid = initial.pid;
    preparation(events, pid);
    noRecovery(events);
    require(!events.some(e => e.type === receiptType), 'External receipt already exists');
    const rejected = one(events, 'sidecar.protocolRejected', 'send');
    const error = one(events, 'error', 'send');
    require(rejected.payload.version === 2 && rejected.payload.cause === 'unsupported-sidecar-version'
      && error.payload.code === 'unsupportedSidecarVersion' && error.payload.cause === 'unsupported-sidecar-version',
    'Wrong fatal cause');
    const started = one(events, 'host.failureShutdownStarted'), hostPid = started.payload.hostProcessIdentifier;
    require(started.payload.processIdentifier === pid && positive(hostPid) && hostPid !== pid, 'Fatal identity mismatch');
    const exit = one(events, 'sidecar.exit');
    require(exit.payload.generation === 1 && exit.payload.processIdentifier === pid
      && exit.payload.expected === true && exit.payload.status === 0 && exit.payload.reason === 1,
    'Expected actual cooperative JVM exit');
    const stopped = one(events, 'host.sidecarShutdownCompleted');
    const completed = one(events, 'host.failureShutdownCompleted');
    require(stopped.payload.succeeded === true && completed.payload.exitStatus === 1, 'Fatal shutdown incomplete');
    const ordered = [one(events, 'flowFileReadGrant.hostShutdownPrepared'), rejected, error, started,
      one(events, 'fileReadGrant.invalidated'), one(events, 'fileReadGrant.closed'),
      one(events, 'host.nativeResourceDrainCompleted'), one(events, 'shutdown', 'send'), exit,
      one(events, 'shutdown.sidecar'), stopped, completed];
    require(ordered.every((e, i) => i === 0 || events.indexOf(ordered[i - 1]) < events.indexOf(e))
      && events.at(-1) === completed, 'Fatal cleanup/exit order mismatch');
    // Full command/result, scope, privacy and cleanup-field validation belongs to the inspector.
    // This receipt contributes only independently observed pre-cleanup process absence.
    requireAbsent(pid);
    requireAbsent(hostPid);
    const current = state(directory, true);
    require(sameFile(initial.stat, current.stat) && sameFile(initial.marker, current.marker)
      && initial.pid === current.pid && sameFile(fstatSync(fd), lstatSync(tracePath)), 'Evidence identity changed');
    const receipt = { at: Date.now(), direction: 'recv', type: receiptType, payload: {
      schemaVersion: 1, hostProcessIdentifier: hostPid, processIdentifier: pid,
      hostAbsent: true, sidecarAbsent: true, startupMatched: true, cleanupPerformed: false,
    } };
    if (append) writeSync(fd, `HOST_PROTOCOL ${JSON.stringify(receipt)}\n`);
    return receipt;
  } finally { closeSync(fd); }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [mode, directory, tracePath] = process.argv.slice(2);
    require(process.argv.length === 5 && ['watch', 'verify'].includes(mode),
      'Usage: file-read-grant-fatal-evidence.mjs <watch|verify> <private-dir> <trace>');
    if (mode === 'verify') verifyFatalExit(directory, tracePath, true);
    else {
      const deadline = Date.now() + 180_000;
      for (;;) {
        require(Date.now() < deadline, 'Timed out waiting for live file-grant preparation');
        const fd = openSync(tracePath, readFlags);
        let events;
        try { events = trace(fd, false); } finally { closeSync(fd); }
        if (events.some(e => e.type === 'flowFileReadGrant.hostShutdownPrepared')) {
          armFatalTrigger(directory, tracePath);
          break;
        }
        require(!events.some(e => ['error', 'sidecar.exit', 'shutdown', 'host.failureShutdownStarted'].includes(e.type)),
          'Application terminated before grant preparation');
        await delay(100);
      }
    }
    console.log(`PASS: fatal grant ${mode} evidence (no cleanup signals)`);
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
