import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../Sources/VaadinDesktopHostAcceptanceSupport/FlowIpcStandardBundleFrameworkAcceptance.swift',import.meta.url),'utf8');
const literal=name=>{
  const match=source.match(new RegExp('private static let '+name+' = """\\n([\\s\\S]*?)\\n    """'));
  assert(match,name); return match[1];
};
const open=literal('standardFrameworkLoginVariantsAction');
const binding=source.match(/private static let standardFrameworkLoginVariantsSubmitAction =\s*standardFrameworkLoginOverlayActionPostSubmitAction([\s\S]*?)\n    private static let/)?.[1];
assert(binding);
const substitutions=[...binding.matchAll(/\.replacingOccurrences\(of: "([^"]+)",\s*with: "([^"]+)"\)/g)];
assert.equal(substitutions.length,2);
const submit=substitutions.reduce((script,match)=>script.replaceAll(match[1],match[2]),
  literal('standardFrameworkLoginOverlayActionPostSubmitAction'));
const ready=source.match(/loginOverlaySubmitButtonReady: (\(\(\) => \{[\s\S]*?\}\)\(\)),/)?.[1];
assert(ready);
for(const script of [open,submit,ready]) { new vm.Script(script); assert(!script.includes('setTimeout')); }
assert(!submit.includes("CustomEvent('login'")); assert(!submit.includes('opened = false'));
assert(source.includes('next: .waitingForLoginVariantsOpen'));
assert(source.includes('case .waitingForLoginVariantsOpen:'));
class Event { constructor(type){this.type=type;} }
class Input {}
const emitted=[];
let clicks=0;
const node=(tag,children=[])=>({nodeType:1,localName:tag,children,disabled:false,
  getBoundingClientRect:()=>({height:20}),dispatchEvent:e=>emitted.push(e.type)});
const username=node('vaadin-text-field'),password=node('vaadin-password-field');
const button={...node('vaadin-button'),textContent:'Sign in',click:()=>clicks++};
const overlay={...node('vaadin-login-overlay',[username,password,button]),opened:false};
const status=node('span');
const document={querySelector:id=>id.endsWith('-status')?status:overlay};
const findDeepFrom=(root,predicate)=>{
  if(!root) return null;
  if(predicate(root)) return root;
  for(const child of root.children||[]) { const match=findDeepFrom(child,predicate); if(match)return match; }
  return null;
};
const context=vm.createContext({document,Event,HTMLInputElement:Input,findDeepFrom});
assert.equal(vm.runInContext(open,context),true);
assert.deepEqual(emitted,['vatori-login-overlay-submit-button-open']); assert.equal(clicks,0);
assert.equal(vm.runInContext(ready,context),false);
overlay.opened=true; assert.equal(vm.runInContext(ready,context),true);
button.disabled=true; assert.equal(vm.runInContext(ready,context),false); button.disabled=false;
button.getBoundingClientRect=()=>({height:0}); assert.equal(vm.runInContext(ready,context),false);
button.getBoundingClientRect=()=>({height:20});
assert.equal(vm.runInContext(submit,context),true); assert.equal(clicks,1);
assert.equal(username.value,'overlay-button-user'); assert.equal(password.value,'flowpass');
overlay.children=[username,password];
assert.equal(vm.runInContext(ready,context),false);
assert.equal(vm.runInContext(submit,context),false); assert.equal(clicks,1);
assert(!emitted.includes('login'));
console.log('PASS: login open/readiness/real-button action contracts (synthetic DOM, not Flow acceptance)');
