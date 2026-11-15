import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import vm from 'node:vm';

const swift = readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcRecoveryDocumentIdentity.swift', import.meta.url), 'utf8');
const source = swift.match(/static let source = """\n([\s\S]*?)\n    """/)?.[1];
assert.ok(source, 'exercise actual injected script, not a copied implementation');
const makePage = () => {
  const page = vm.createContext({ document: {}, performance: { timeOrigin: 1000.0002 },
    crypto: { randomUUID } });
  page.window = page;
  vm.runInContext(source, page);
  return page;
};
const page = makePage();
const first = page.VatoriRecoveryDocumentIdentity();
assert.equal(first.retained, true);
page.performance.timeOrigin = 999;
const drifted = page.VatoriRecoveryDocumentIdentity();
assert.equal(drifted.marker, first.marker);
assert.equal(drifted.timeOriginMillis, first.timeOriginMillis);
assert.equal(drifted.observedTimeOriginMillis, 999);
assert.equal(drifted.retained, true);
page.document = {};
assert.equal(page.VatoriRecoveryDocumentIdentity().retained, false);
assert.throws(() => vm.runInContext(source, page), /redefine/,
  'reinjection cannot reset a retained page identity');
const next = makePage().VatoriRecoveryDocumentIdentity();
assert.notEqual(next.marker, first.marker, 'new page changes identity even with identical time origin');
const descriptor = Object.getOwnPropertyDescriptor(page, 'VatoriRecoveryDocumentIdentity');
assert.equal(descriptor.configurable, false);
assert.equal(descriptor.writable, false);
console.log('Recovery document identity smoke passed: stable identity under clock drift, changed document, reinjection, fresh page and immutable hook (synthetic JS contexts)');
