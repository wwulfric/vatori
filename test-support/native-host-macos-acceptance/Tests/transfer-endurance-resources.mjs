import {execFileSync} from 'node:child_process';
import {appendFileSync} from 'node:fs';

const [host,java,jar,output]=process.argv.slice(2);
if(!output)throw Error('Expected host java jar output');
function sample(){
  const time=Date.now(), processes=[];
  const rows=execFileSync('/bin/ps',['-axo','pid=,rss=,command='],{encoding:'utf8'}).split('\n');
  for(const row of rows){
    const m=row.match(/^\s*(\d+)\s+(\d+)\s+(.+)$/); if(!m)continue;
    const role=m[3]===host?'host':m[3]===`${java} -jar ${jar}`?'sidecar':null;
    if(!role)continue;
    let descriptors=null;
    try { descriptors=execFileSync('/usr/sbin/lsof',['-a','-p',m[1],'-Ff'],{encoding:'utf8',timeout:4000}).split('\n').filter(s=>/^f\d/.test(s)).length; }
    catch { /* Missing samples are explicit, never zero resource use. */ }
    processes.push({role,pid:Number(m[1]),rssKiB:Number(m[2]),descriptors});
  }
  appendFileSync(output,JSON.stringify({time,processes})+'\n');
}
sample();
const timer=setInterval(sample,5000);
process.on('SIGTERM',()=>{clearInterval(timer);sample();process.exit(0);});
