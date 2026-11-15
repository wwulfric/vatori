import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,copyFileSync,readdirSync,rmSync,existsSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
// Inert tool doubles: no real app/JVM, signing, OS permission, or user process access.
const root=resolve(fileURLToPath(new URL('../../../',import.meta.url)));
const work=mkdtempSync(join(tmpdir(),'vatori-lazy-runner-'));
const support=join(work,'test-support/native-host-macos-acceptance');
const build=join(work,'.build/native-hosts/macos'),stubs=join(work,'stubs');
const runner=join(support,'bin/run-lazy-data-baseline');
const executable=(path,code)=>writeFileSync(path,'#!/bin/bash\nset -eu\n'+code,{mode:0o700});
try {
  for(const p of ['bin','Tests']) mkdirSync(join(support,p),{recursive:true});
  for(const p of ['bin','jdk/bin','stubs','.build/native-hosts/macos/VaadinDesktopHost.app/Contents/MacOS']) mkdirSync(join(work,p),{recursive:true});
  copyFileSync(join(root,'test-support/native-host-macos-acceptance/bin/run-lazy-data-baseline'),runner);
  const matcher=readFileSync(runner,'utf8').match(/owned_pids\(\) \{\n[\s\S]*?\n\}/)?.[0];
  assert(matcher);
  const matched=spawnSync('bash',['-c',`set -euo pipefail
ps() { printf '%s\\n' '101 /owned/host' '102 /jdk/bin/java -jar /owned/app.jar' '103 /other/host' '104 /jdk/bin/java -jar /other.jar --note -jar /owned/app.jar' '105 /owned/host.neighbor' '106 /jdk/bin/java -jar /owned/app.jar --extra'; }
${matcher}
owned_pids`],{encoding:'utf8',env:{...process.env,EXE:'/owned/host',JAR:'/owned/app.jar',JAVA_HOME:'/jdk'}});
  assert.equal(matched.status,0,matched.stderr); assert.equal(matched.stdout,'101\n102\n');
  for(const name of ['lazy-data-baseline-verify.mjs','lazy-data-baseline-wire-verify.mjs']) writeFileSync(join(support,'Tests',name),'inert verifier');
  writeFileSync(join(build,'VaadinDesktopHost.app/Contents/MacOS/VaadinDesktopHost'),'inert host');
  writeFileSync(join(build,'host.out.log'),'previous shared evidence');
  executable(join(work,'jdk/bin/java'),'exit "${JDK_STATUS:-0}"\n');
  executable(join(work,'bin/vaadin-desktop'),'exit "${PACKAGE_STATUS:-0}"\n');
  executable(join(support,'bin/build-host'),'exit "${BUILD_STATUS:-0}"\n');
  executable(join(stubs,'uname'),'if [[ "$1" == -s ]]; then echo Darwin; else echo arm64; fi\n');
  executable(join(stubs,'sw_vers'),'echo stub-os\n');
  executable(join(stubs,'git'),'echo stub-source\n');
  executable(join(stubs,'ps'),'exit "${PS_STATUS:-0}"\n');
  executable(join(stubs,'ditto'),'cp -R "$1" "$2"\n');
  executable(join(stubs,'plutil'),'exit 0\n');
  executable(join(stubs,'codesign'),'exit "${SIGN_STATUS:-0}"\n');
  executable(join(stubs,'open'),`[[ -z "\${VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES:-}" ]] || exit 97
printf '%s\\n' "$@" > "$LAUNCH_RECEIPT"
exit "\${OPEN_STATUS:-0}"
`);
  executable(join(stubs,'node'),'echo stub-verifier\nexit "${VERIFY_STATUS:-0}"\n');
  const receipt=join(work,'launch.txt');
  const env={...process.env,PATH:stubs+':'+process.env.PATH,JAVA_HOME:join(work,'jdk'),
    VATORI_NATIVE_HOST_MACOS_ACCEPTANCE_PACKAGE_LOCK_HELD:'1',LAUNCH_RECEIPT:receipt,
    VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES:'1'};
  let previous=[];
  for(const [override,expected,launched] of [[{},0,true],[{PACKAGE_STATUS:'17'},17,false],
    [{BUILD_STATUS:'18'},18,false],[{JDK_STATUS:'19'},19,false],[{SIGN_STATUS:'20'},20,false],
    [{OPEN_STATUS:'21'},21,true],[{VERIFY_STATUS:'22'},22,true],[{PS_STATUS:'1'},1,true],[{},0,true]]) {
    if(existsSync(receipt)) rmSync(receipt);
    const before=readdirSync(build);
    const result=spawnSync('bash',[runner],{env:{...env,...override},encoding:'utf8',timeout:15000});
    assert.ifError(result.error); assert.equal(result.status,expected,result.stdout+result.stderr);
    assert.equal(existsSync(receipt),launched);
    const fresh=readdirSync(build).filter(n=>n.startsWith('lazy-data-evidence.')&&!before.includes(n));
    assert.equal(fresh.length,1); const dir=join(build,fresh[0]);
    assert.equal(readFileSync(join(dir,'runner-exit-status.txt'),'utf8').trim(),String(expected));
    assert.equal(readFileSync(join(build,'host.out.log'),'utf8'),'previous shared evidence');
    for(const [path,contents] of previous) assert.equal(readFileSync(path,'utf8'),contents);
    previous.push([join(dir,'runner-exit-status.txt'),String(expected)+'\n']);
    if(launched) {
      const args=readFileSync(receipt,'utf8');
      assert(args.includes('-g\n')); assert(args.includes('VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA_BASELINE=1'));
      assert(args.includes('VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS=600'));
      assert(!args.includes('AUTO_NOTIFICATION')); assert(args.includes(fresh[0]));
    }
  }
  console.log('PASS: lazy-data runner, 9 inert success/failure/retry cases; private evidence and inherited-flag isolation');
} finally { rmSync(work,{recursive:true,force:true}); }
