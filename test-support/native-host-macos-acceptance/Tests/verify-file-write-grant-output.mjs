import assert from 'node:assert/strict';
import { lstatSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

assert.equal(process.argv.length, 4, 'expected selected and sibling paths');
const [selected, sibling] = process.argv.slice(2).map((path) => resolve(path));
assert.ok(selected && sibling, 'expected selected and sibling paths');
assert.equal(dirname(selected), dirname(sibling));
assert.notEqual(selected, sibling);
for (const path of [selected, sibling]) {
  const stat = lstatSync(path);
  assert.ok(stat.isFile() && !stat.isSymbolicLink());
  assert.equal(stat.nlink, 1);
  assert.equal(stat.uid, process.getuid());
}
const expected = Buffer.alloc(65542);
for (let i = 0; i < 65536; i++) expected[i] = i % 251;
expected.set([0, 255, 1, 254, 42, 10], 65536);
assert.deepEqual(readFileSync(selected), expected);
assert.equal(readFileSync(sibling, 'utf8'),
  'unselected-sandbox-sibling-must-stay-unchanged');
assert.throws(() => lstatSync(join(dirname(selected), 'canceled-write-grant.bin')),
  { code: 'ENOENT' });
console.log('Sandbox file write grant independent binary output and sibling verification passed');
