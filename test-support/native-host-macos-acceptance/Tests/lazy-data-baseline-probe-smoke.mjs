import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const sourceFile = readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcLazyDataBaselineAcceptance.swift', import.meta.url), 'utf8');
const source = sourceFile.match(/static let script = """\n([\s\S]*?)\n    """/)?.[1];
assert.ok(source, 'test actual embedded JavaScript');
const script = new vm.Script(source);
let clicks = 0, rootAvailable = false;
const page = vm.createContext({ document: { getElementById: id =>
  id === 'lazy-data-baseline-open' && rootAvailable ? { click: () => clicks++ } : null } });
page.window = page;
assert.deepEqual(JSON.parse(script.runInContext(page)), { receipts: [], done: false, error: '' });
assert.equal(clicks, 0);
rootAvailable = true;
script.runInContext(page);
script.runInContext(page);
assert.equal(clicks, 1, 'navigation waits for public button and dispatches only once');
// Polling an already terminal browser run must not restart actions or mutate its evidence.
const receipts = [{ index: 0, mode: 'exact', stage: 'top' }];
page.document.getElementById = id => id === 'lazy-data-baseline' ? {} : null;
page.VatoriLazyBaselineRun = { receipts, done: true, error: '' };
assert.deepEqual(JSON.parse(script.runInContext(page)), { receipts, done: true, error: '' });
assert.equal(clicks, 1);
console.log('Lazy baseline probe syntax/navigation/terminal-state smoke passed (synthetic DOM; not viewport or IPC acceptance)');
