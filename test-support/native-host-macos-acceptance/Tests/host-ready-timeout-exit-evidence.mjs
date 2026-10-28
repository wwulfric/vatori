import { closeSync, constants, fstatSync, lstatSync, openSync, readFileSync, readdirSync, realpathSync, writeSync } from 'node:fs';
import { basename, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const prefix = 'hostReadyTimeout.';
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const integer = value => Number.isSafeInteger(value) && value > 0;
const keys = (value, expected) => value && typeof value === 'object' && !Array.isArray(value)
  && Object.keys(value).sort().join(',') === [...expected].sort().join(',');
function require(value, message) { if (!value) throw new Error(message); }
function readOwnedDescriptor(fd, privateFile, limit) {
  const stat = fstatSync(fd);
  require(stat.isFile() && stat.nlink === 1 && stat.uid === process.getuid()
    && stat.size > 0 && stat.size <= limit, 'Expected bounded owned regular file');
  require(!privateFile || (stat.mode & 0o777) === 0o600, 'Marker must be mode 0600');
  return readFileSync(fd, 'utf8');
}
function readMarker(path) {
  const fd = openSync(path, constants.O_RDONLY | constants.O_NOFOLLOW);
  try { return JSON.parse(readOwnedDescriptor(fd, true, 2048)); } finally { closeSync(fd); }
}

export function verifyExitEvidence(directory, tracePath, mode, hostProcessIdentifier, append = false) {
  require(['cooperative', 'blocked'].includes(mode), 'Invalid ready-timeout mode');
  require(integer(hostProcessIdentifier), 'Invalid independently captured host PID');
  require(resolve(directory) === realpathSync(directory)
    && /^vatori-ready-timeout\.[a-zA-Z0-9]{6}$/.test(basename(directory)), 'Noncanonical private marker directory');
  const stat = lstatSync(directory);
  require(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o700, 'Marker directory must be owned mode 0700');
  const names = ['startup.json', 'hook-entered.json'];
  require(readdirSync(directory).sort().join(',') === [...names].sort().join(','), 'Exactly two markers required');
  const markers = names.map(name => readMarker(join(directory, name)));
  const first = markers[0];
  for (const marker of markers) {
    require(keys(marker, ['schemaVersion', 'runId', 'processIdentifier', 'mode'])
      && marker.schemaVersion === 1 && uuid.test(marker.runId) && integer(marker.processIdentifier)
      && marker.mode === mode && marker.runId === first.runId
      && marker.processIdentifier === first.processIdentifier, 'Marker identity/schema mismatch');
  }
  require(first.processIdentifier !== hostProcessIdentifier, 'Host/JVM PID must differ');
  const flags = (append ? constants.O_RDWR | constants.O_APPEND : constants.O_RDONLY) | constants.O_NOFOLLOW;
  const fd = openSync(tracePath, flags);
  try {
    const text = readOwnedDescriptor(fd, false, 8 * 1024 * 1024);
    const events = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
      .map(line => JSON.parse(line.slice('HOST_PROTOCOL '.length)));
    const one = type => {
      const matches = events.filter(event => event.type === type);
      require(matches.length === 1, `Expected exactly one ${type}`);
      const event = matches[0];
      require(keys(event, ['at', 'direction', 'type', 'payload']) && event.direction === 'recv'
        && integer(event.at) && event.payload && typeof event.payload === 'object', `Invalid ${type} trace`);
      return event;
    };
    require(!events.some(e => e.type === prefix + 'externalExitObserved'), 'External receipt already exists');
    const started = one('sidecar.started'), failure = one('host.failureShutdownStarted');
    const exit = one('sidecar.exit'), receipt = one('shutdown.sidecar');
    const completed = one('host.sidecarShutdownCompleted'), final = one('host.failureShutdownCompleted');
    require(started.payload.generation === 1 && started.payload.processIdentifier === first.processIdentifier
      && started.payload.transport === 'flow-ipc'
      && keys(failure.payload, ['processIdentifier', 'hostProcessIdentifier'])
      && failure.payload.processIdentifier === first.processIdentifier
      && failure.payload.hostProcessIdentifier === hostProcessIdentifier, 'Fatal shutdown identity mismatch');
    require(exit.payload.generation === 1 && exit.payload.processIdentifier === first.processIdentifier
      && exit.payload.expected === true && exit.payload.status === (mode === 'blocked' ? 9 : 0)
      && exit.payload.reason === (mode === 'blocked' ? 2 : 1)
      && keys(receipt.payload, ['mode']) && receipt.payload.mode === (mode === 'blocked' ? 'killed' : 'graceful')
      && keys(completed.payload, ['succeeded']) && completed.payload.succeeded === true
      && keys(final.payload, ['exitStatus']) && final.payload.exitStatus === 1,
    'Incomplete actual fatal-shutdown evidence');
    const ordered = [started, failure, exit, receipt, completed, final];
    require(ordered.every((event, index) => index === 0 || events.indexOf(ordered[index - 1]) < events.indexOf(event)),
      'Actual process exit must precede fatal host completion');
    require(events.at(-1) === final, 'Fatal completion must be the final host event');
    for (const pid of [first.processIdentifier, hostProcessIdentifier]) {
      let absent = false;
      try { process.kill(pid, 0); } catch (error) {
        require(error.code === 'ESRCH', `Cannot verify PID ${pid} absence: ${error.code}`);
        absent = true;
      }
      require(absent, `PID ${pid} is still alive; cleanup must not rescue acceptance`);
    }
    const result = { direction: 'recv', type: prefix + 'externalExitObserved', at: Date.now(), payload: {
      schemaVersion: 1, runId: first.runId, mode, processIdentifier: first.processIdentifier, hostProcessIdentifier,
      hostAbsent: true, sidecarAbsent: true, markersMatched: true, cleanupPerformed: false,
    } };
    // Append on the same validated descriptor, avoiding a read/close/reopen path race.
    if (append) writeSync(fd, `HOST_PROTOCOL ${JSON.stringify(result)}\n`);
    return result;
  } finally { closeSync(fd); }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    require(process.argv.length === 6 && /^[1-9][0-9]*$/.test(process.argv[5]),
      'Usage: host-ready-timeout-exit-evidence.mjs <private-dir> <trace> cooperative|blocked <host-pid>');
    const [, , directory, tracePath, mode, hostPID] = process.argv;
    verifyExitEvidence(directory, tracePath, mode, Number(hostPID), true);
    console.log(`PASS: independent ${mode} host/JVM absence and two matching markers (before cleanup)`);
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
