import { appendFileSync, closeSync, constants, fstatSync, lstatSync, openSync, readFileSync, readdirSync, realpathSync } from 'node:fs';
import { basename, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const prefix = 'flowHostShutdownTermination.';
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const integer = value => Number.isSafeInteger(value) && value > 0;
const keys = (value, expected) => value && typeof value === 'object' && !Array.isArray(value)
  && Object.keys(value).sort().join(',') === [...expected].sort().join(',');
function require(value, message) { if (!value) throw new Error(message); }

function readOwnedFile(path, privateFile, limit) {
  const fd = openSync(path, constants.O_RDONLY | constants.O_NOFOLLOW);
  try {
    const stat = fstatSync(fd);
    require(stat.isFile() && stat.nlink === 1 && stat.uid === process.getuid()
      && stat.size > 0 && stat.size <= limit, 'Expected bounded owned regular file');
    require(!privateFile || (stat.mode & 0o777) === 0o600, 'Marker must be mode 0600');
    return readFileSync(fd, 'utf8');
  } finally { closeSync(fd); }
}

export function verifyExitEvidence(directory, tracePath, mode) {
  require(['cooperative', 'blocked'].includes(mode), 'Invalid shutdown mode');
  require(resolve(directory) === realpathSync(directory)
    && /^vatori-host-shutdown\.[a-zA-Z0-9]{6}$/.test(basename(directory)), 'Noncanonical private marker directory');
  const stat = lstatSync(directory);
  require(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o700, 'Marker directory must be owned mode 0700');
  const names = ['startup.json', 'launcher-returned.json', 'hook-entered.json'];
  require(readdirSync(directory).sort().join(',') === [...names].sort().join(','), 'Exactly three markers required');
  const markers = names.map(name => JSON.parse(readOwnedFile(join(directory, name), true, 2048)));
  const first = markers[0];
  for (const marker of markers) {
    require(keys(marker, ['schemaVersion', 'runId', 'processIdentifier', 'mode'])
      && marker.schemaVersion === 1 && uuid.test(marker.runId) && integer(marker.processIdentifier)
      && marker.mode === mode && marker.runId === first.runId
      && marker.processIdentifier === first.processIdentifier, 'Marker identity/schema mismatch');
  }
  const text = readOwnedFile(tracePath, false, 64 * 1024 * 1024);
  const events = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
    .map(line => JSON.parse(line.slice('HOST_PROTOCOL '.length)));
  const one = (type, direction = 'recv') => {
    const matches = events.filter(event => event.type === type);
    require(matches.length === 1, `Expected exactly one ${type}`);
    const event = matches[0];
    require(keys(event, ['at', 'direction', 'type', 'payload']) && event.direction === direction
      && integer(event.at) && event.payload && typeof event.payload === 'object', `Invalid ${type} trace`);
    return event;
  };
  require(!events.some(e => e.type === prefix + 'externalExitObserved'), 'External receipt already exists');
  const started = one('sidecar.started'), exit = one('sidecar.exit');
  const final = one(prefix + 'applicationWillTerminate'), completed = one('host.sidecarShutdownCompleted');
  const frame = final.payload;
  require(frame.runId === first.runId && frame.mode === mode
    && frame.processIdentifier === first.processIdentifier && integer(frame.hostProcessIdentifier)
    && frame.hostProcessIdentifier !== frame.processIdentifier
    && frame.windowId === 'main' && frame.sessionId === 'flow-ipc-session'
    && frame.uiId === 'main' && frame.recoveryGeneration === 0, 'Final receipt identity mismatch');
  require(frame.complete === true && frame.preparedObserved === true && frame.finalPointerDispatched === true
    && frame.terminationSource === 'applicationWillTerminate' && frame.currentProcessIdentifier === 0
    && frame.originalSidecarAbsent === true && frame.originalWindowRetained === false
    && frame.launcherReturnedMarkerMatched === true && frame.hookEnteredMarkerMatched === true,
  'Incomplete in-host termination evidence');
  require(started.payload.generation === 1 && started.payload.processIdentifier === first.processIdentifier
    && exit.payload.generation === 1 && exit.payload.processIdentifier === first.processIdentifier
    && exit.payload.expected === true && exit.payload.status === (mode === 'blocked' ? 9 : 0)
    && exit.payload.reason === (mode === 'blocked' ? 2 : 1)
    && completed.payload.succeeded === true && events.indexOf(started) < events.indexOf(exit)
    && events.indexOf(exit) < events.indexOf(completed) && events.indexOf(completed) < events.indexOf(final),
  'Actual process exit must precede AppKit termination');
  for (const pid of [frame.processIdentifier, frame.hostProcessIdentifier]) {
    let absent = false;
    try { process.kill(pid, 0); } catch (error) {
      require(error.code === 'ESRCH', `Cannot verify PID ${pid} absence: ${error.code}`);
      absent = true;
    }
    require(absent, `PID ${pid} is still alive; cleanup must not rescue acceptance`);
  }
  return { direction: 'recv', type: prefix + 'externalExitObserved', at: Date.now(), payload: {
    windowId: 'main', sessionId: 'flow-ipc-session', uiId: 'main', runId: first.runId, mode,
    processIdentifier: first.processIdentifier, hostProcessIdentifier: frame.hostProcessIdentifier,
    recoveryGeneration: 0, schemaVersion: 1, hostAbsent: true, sidecarAbsent: true,
    markersMatched: true, cleanupPerformed: false,
  } };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    require(process.argv.length === 5, 'Usage: host-shutdown-exit-evidence.mjs <private-dir> <trace> cooperative|blocked');
    const [, , directory, tracePath, mode] = process.argv;
    const receipt = verifyExitEvidence(directory, tracePath, mode);
    // Independent observation after LaunchServices waited for host exit, before
    // any runner cleanup. This is focused process evidence, not physical input.
    appendFileSync(tracePath, `HOST_PROTOCOL ${JSON.stringify(receipt)}\n`, { flag: constants.O_WRONLY | constants.O_APPEND | constants.O_NOFOLLOW });
    console.log(`PASS: independent ${mode} host/JVM absence and three matching markers (before cleanup)`);
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
