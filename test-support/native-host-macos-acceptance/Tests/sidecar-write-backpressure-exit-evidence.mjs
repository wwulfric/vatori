import { closeSync, constants, fstatSync, lstatSync, openSync, readFileSync, readdirSync, realpathSync, writeSync } from 'node:fs';
import { basename, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const prefix = 'flowSidecarWriteBackpressure.';
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
export function verifyExitEvidence(directory, tracePath, append = false) {
  require(resolve(directory) === realpathSync(directory)
    && /^vatori-host-shutdown\.[a-zA-Z0-9]{6}$/.test(basename(directory)), 'Noncanonical private marker directory');
  const stat = lstatSync(directory);
  require(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o700, 'Marker directory must be owned mode 0700');
  require(readdirSync(directory).join(',') === 'startup.json', 'Stopped JVM must have exactly the startup marker');
  let marker;
  const markerFD = openSync(join(directory, 'startup.json'), constants.O_RDONLY | constants.O_NOFOLLOW);
  try { marker = JSON.parse(readOwnedDescriptor(markerFD, true, 2048)); } finally { closeSync(markerFD); }
  require(keys(marker, ['schemaVersion', 'runId', 'processIdentifier', 'mode']) && marker.schemaVersion === 1
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(marker.runId)
    && integer(marker.processIdentifier) && marker.mode === 'cooperative', 'Startup marker identity/schema mismatch');
  const flags = (append ? constants.O_RDWR | constants.O_APPEND : constants.O_RDONLY) | constants.O_NOFOLLOW;
  const fd = openSync(tracePath, flags);
  try {
    const text = readOwnedDescriptor(fd, false, 64 * 1024 * 1024);
    const events = text.split('\n').filter(line => line.startsWith('HOST_PROTOCOL '))
      .map(line => JSON.parse(line.slice('HOST_PROTOCOL '.length)));
    const validRoot = event => keys(event, ['at', 'direction', 'type', 'payload'])
      && event.direction === 'recv' && integer(event.at) && event.payload && typeof event.payload === 'object';
    const one = type => {
      const matches = events.filter(event => event.type === type);
      require(matches.length === 1 && validRoot(matches[0]), `Expected exactly one valid ${type}`);
      return matches[0];
    };
    require(!events.some(e => e.type === prefix + 'externalExitObserved'), 'External receipt already exists');
    const final = one(prefix + 'applicationWillTerminate'), frame = final.payload;
    const common = { windowId: 'main', sessionId: 'flow-ipc-session', uiId: 'main', runId: marker.runId,
      processIdentifier: marker.processIdentifier, hostProcessIdentifier: frame.hostProcessIdentifier, recoveryGeneration: 0 };
    require(integer(common.hostProcessIdentifier) && common.hostProcessIdentifier !== marker.processIdentifier,
      'Invalid independent host/JVM identities');
    const matchesIdentity = event => Object.entries(common).every(([key, value]) => event.payload[key] === value);
    const prepared = one(prefix + 'prepared'), suspended = one(prefix + 'sidecarSuspended');
    const burst = one(prefix + 'burstRequested'), dispatched = one(prefix + 'browserDispatchCompleted');
    const heartbeat = one(prefix + 'mainLoopHeartbeat'), quit = one(prefix + 'quitDispatched');
    for (const event of [prepared, suspended, burst, dispatched, heartbeat, quit, final]) {
      require(matchesIdentity(event), 'Acceptance identity mismatch');
    }
    require(suspended.payload.signal === 'SIGSTOP' && suspended.payload.processState === 4 && suspended.payload.stopped === true,
      'Exact JVM stop was not observed');
    require(burst.payload.requestCount === 16 && burst.payload.headerBytes === 16384 && burst.payload.browserProgrammatic === true
      && dispatched.payload.requestCount === 16 && heartbeat.payload.mainThread === true
      && heartbeat.payload.sidecarStopped === true && heartbeat.payload.windowRetained === true
      && heartbeat.payload.webViewRetained === true && heartbeat.payload.browserDispatchCompleted === true,
    'Bounded browser dispatch and live main-loop heartbeat required');
    require(quit.payload.source === 'product-application-menu' && quit.payload.action === 'terminate:'
      && quit.payload.keyEquivalent === 'q' && quit.payload.commandModifier === true && quit.payload.menuMatched === true
      && quit.payload.activeApplication === true && quit.payload.keyWindow === true, 'Product native Quit intent missing');
    for (const key of ['complete', 'preparedObserved', 'sidecarSuspendedObserved', 'burstDispatched', 'heartbeatObserved',
      'quitDispatched', 'originalSidecarAbsent', 'originalWindowRetained', 'originalWebViewRetained', 'startupMarkerMatched']) {
      require(frame[key] === true, `Incomplete final receipt: ${key}`);
    }
    require(frame.terminationSource === 'applicationWillTerminate' && frame.currentProcessIdentifier === 0,
      'Actual AppKit termination with no sidecar required');
    const started = one('sidecar.started'), exit = one('sidecar.exit'), receipt = one('shutdown.sidecar');
    const completed = one('host.sidecarShutdownCompleted');
    require(started.payload.generation === 1 && started.payload.processIdentifier === marker.processIdentifier
      && exit.payload.generation === 1 && exit.payload.processIdentifier === marker.processIdentifier
      && exit.payload.expected === true && exit.payload.status === 9 && exit.payload.reason === 2
      && keys(receipt.payload, ['mode']) && receipt.payload.mode === 'killed'
      && keys(completed.payload, ['succeeded']) && completed.payload.succeeded === true,
    'Actual stopped JVM exit must precede host completion');
    const ordered = [started, prepared, suspended, burst, dispatched, heartbeat, quit, exit, receipt, completed, final];
    require(ordered.every((event, index) => index === 0 || events.indexOf(ordered[index - 1]) < events.indexOf(event)),
      'Backpressure lifecycle evidence is out of order');
    const backpressure = events.filter(event => event.type === 'sidecar.writeBackpressure');
    require(backpressure.length > 0 && backpressure.every(event => validRoot(event)
      && keys(event.payload, ['generation', 'processIdentifier', 'pendingBytes', 'pendingFrames', 'partialFrameBytes'])
      && event.payload.generation === 1 && event.payload.processIdentifier === marker.processIdentifier
      && integer(event.payload.pendingBytes) && event.payload.pendingBytes <= 16 * 1024 * 1024
      && integer(event.payload.pendingFrames) && event.payload.pendingFrames <= 256
      && Number.isSafeInteger(event.payload.partialFrameBytes) && event.payload.partialFrameBytes >= 0
      && event.payload.partialFrameBytes < 16 * 1024 * 1024
      && event.payload.partialFrameBytes <= event.payload.pendingBytes
      && events.indexOf(event) < events.indexOf(quit))
      && backpressure.some(event => events.indexOf(burst) < events.indexOf(event)
        && events.indexOf(event) < events.indexOf(heartbeat)),
    'Bounded original-writer backpressure after the stopped-child burst must precede the main-loop heartbeat');
    for (const pid of [marker.processIdentifier, common.hostProcessIdentifier]) {
      let absent = false;
      try { process.kill(pid, 0); } catch (error) {
        require(error.code === 'ESRCH', `Cannot verify PID ${pid} absence: ${error.code}`); absent = true;
      }
      require(absent, `PID ${pid} is still alive; cleanup must not rescue acceptance`);
    }
    const result = { direction: 'recv', type: prefix + 'externalExitObserved', at: Date.now(), payload: {
      ...common, schemaVersion: 1, hostAbsent: true, sidecarAbsent: true, markersMatched: true, cleanupPerformed: false,
    } };
    if (append) writeSync(fd, `HOST_PROTOCOL ${JSON.stringify(result)}\n`);
    return result;
  } finally { closeSync(fd); }
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    require(process.argv.length === 4, 'Usage: sidecar-write-backpressure-exit-evidence.mjs <private-dir> <trace>');
    verifyExitEvidence(process.argv[2], process.argv[3], true);
    console.log('PASS: independent stopped-JVM/host absence and startup-only marker (before cleanup)');
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
