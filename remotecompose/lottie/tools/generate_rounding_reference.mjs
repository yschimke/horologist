// Offline fixtures from pinned lottie-web rounding. No production Kotlin is used.
import { writeFileSync } from 'node:fs';
const root = 'https://raw.githubusercontent.com/airbnb/lottie-web/v5.12.2/player/js/utils/';
async function source(path) {
  const response = await fetch(root + path);
  if (!response.ok) throw new Error(path + ': ' + response.status);
  return response.text();
}
const common = await source('common.js');
const roundCorner = Number(common.match(/roundCorner\s*=\s*([\d.]+)/)[1]);
const modifier = (await source('shapes/RoundCornersModifier.js'))
  .replace(/^import[\s\S]*?from [^;]*;\s*/gm, '')
  .replace('export default RoundCornersModifier;', '');
const newPath = () => ({
  c: false, v: [], i: [], o: [],
  get _length() { return this.v.length; },
  setTripleAt(x,y,ox,oy,ix,iy,n) { this.v[n]=[x,y]; this.o[n]=[ox,oy]; this.i[n]=[ix,iy]; },
});
const Round = new Function('extendPrototype','ShapeModifier','shapePool','roundCorner',
  modifier + '; return RoundCornersModifier;')(() => {}, function(){}, {newElement:newPath}, roundCorner);
function rounded(input, radius) {
  if (radius === 0) return input;
  const path = newPath(); path.c = input.c;
  input.v.forEach(([x,y],n) => path.setTripleAt(x,y,x+input.o[n][0],y+input.o[n][1],
    x+input.i[n][0],y+input.i[n][1],n));
  const output = new Round().processPath(path, radius);
  return {c:output.c,v:output.v,i:output.i.map((p,n)=>p.map((x,j)=>x-output.v[n][j])),
    o:output.o.map((p,n)=>p.map((x,j)=>x-output.v[n][j]))};
}
const fixed = k => ({a:0,k});
const animated = (a,b) => ({a:1,k:[{t:0,s:a,o:{x:0,y:0},i:{x:1,y:1}},{t:10,s:b}]});
function path(name, frame) {
  let v = name === 'open' ? [[8,48],[20,12],[36,44],[56,16]]
    : [[12,12],[52,12],[52,20],[12,52]];
  const i = v.map(()=>[0,0]), o = v.map(()=>[0,0]);
  if (name === 'moving') v[2] = [52-frame,20+frame*2.4];
  if (name === 'controls') o[1] = [frame*0.8,0];
  if (name === 'duplicate') { v.splice(2,0,[52,12]); i.push([0,0]); o.push([0,0]); }
  return {c:name !== 'open',v,i,o};
}
// Analytic unrounded polystar vertices, independently of the Kotlin implementation.
function star(points, polygon, reverse) {
  const v=[], i=[], o=[];
  const count = polygon ? Math.floor(points) : Math.ceil(points)*2;
  const f = points-Math.floor(points);
  const step = Math.PI*2/(polygon ? count : points)*(reverse ? -1 : 1);
  let a = -Math.PI/2 + (!polygon && f ? step/2*(1-f) : 0);
  for (let j=0;j<count;j++) {
    const r = polygon ? 23 : j === 0 && f ? 10+13*f : j%2 ? 10 : 23;
    v.push([32+r*Math.cos(a),32+r*Math.sin(a)]); i.push([0,0]);o.push([0,0]);
    a += polygon ? step : j === 0 && f ? step*f/2 : step/2;
  }
  return {c:true,v,i,o};
}
const cases=[];
for (const name of ['uneven','moving','open','controls','duplicate']) {
  const morph = ['moving','controls'].includes(name);
  const shape = {ty:'sh',ks:morph ? animated([path(name,0)],[path(name,10)]) : fixed(path(name,0))};
  const radius = name === 'controls' ? fixed(8) : animated([0],[24]);
  cases.push({name,shape,radius,frames:Array.from({length:11},(_,frame)=>({
    frame,expected:rounded(path(name,frame),name === 'controls' ? 8 : frame*2.4)}))});
}
for (const name of ['star-radius','star-count','star-reverse','polygon-count','star-trim']) {
  const polygon = name === 'polygon-count', reverse = name === 'star-reverse';
  const moving = name !== 'star-radius';
  const shape={ty:'sr',sy:polygon?2:1,d:reverse?3:1,pt:moving?animated([3],[7]):fixed(5),
    p:fixed([32,32]),r:fixed(0),or:fixed(23),ir:fixed(10),os:fixed(0),is:fixed(0)};
  const radius = moving ? fixed(4) : animated([0],[12]);
  cases.push({name,shape,radius,trim:name==='star-trim',frames:Array.from({length:11},(_,frame)=>({
    frame,expected:rounded(star(moving?3+frame*0.4:5,polygon,reverse),moving?4:frame*1.2)}))});
}
function orderedCases() {
  const output = [];
  const stroke = {ty:'st',c:fixed([1,0,0,1]),o:fixed(100),w:fixed(2),lc:1,lj:1,ml:4};
  const rd = radius => ({ty:'rd',r:fixed(radius)});
  const pb = {ty:'pb',a:animated([-20],[30])};
  const tw = {ty:'tw',a:animated([0],[200]),c:fixed([32,32])};
  const shape = geometry => ({ty:'sh',ks:fixed(geometry)});
  function pucker(p, amount) {
    const center = [0,1].map(k=>p.v.reduce((s,v)=>s+v[k],0)/p.v.length);
    const f = amount / 100;
    const v = p.v.map(q=>q.map((x,k)=>x+(center[k]-x)*f));
    const controls = key => p[key].map((q,j)=>q.map((x,k)=>{
      const absolute = p.v[j][k]+x;
      return absolute+(absolute-center[k])*f-v[j][k];
    }));
    return {...p,v,i:controls('i'),o:controls('o')};
  }
  function twist(p, amount) {
    function map(q) {
      const x=q[0]-32,y=q[1]-32,t=amount*Math.hypot(x,y)*Math.PI/18000;
      return [32+x*Math.cos(t)-y*Math.sin(t),32+x*Math.sin(t)+y*Math.cos(t)];
    }
    const v=p.v.map(map);
    const controls=key=>p[key].map((q,j)=>map(q.map((x,k)=>x+p.v[j][k])).map((x,k)=>x-v[j][k]));
    return {...p,v,i:controls('i'),o:controls('o')};
  }
  const add = (name, input, expected) => output.push({name,input,
    frames:Array.from({length:11},(_,frame)=>({frame,expected:expected(frame)}))});
  const base = path('uneven',0);
  add('pucker-round',[shape(base),pb,rd(8),stroke],f=>rounded(pucker(base,-20+5*f),8));
  add('round-pucker',[shape(base),rd(8),pb,stroke],f=>pucker(rounded(base,8),-20+5*f));
  add('twist-round',[shape(base),tw,rd(8),stroke],f=>rounded(twist(base,20*f),8));
  add('round-twist',[shape(base),rd(8),tw,stroke],f=>twist(rounded(base,8),20*f));
  add('paint-pucker-round',[shape(base),stroke,pb,rd(8)],f=>rounded(pucker(base,-20+5*f),8));
  add('pucker-paint-round',[shape(base),pb,stroke,rd(8)],f=>rounded(pucker(base,-20+5*f),8));
  add('nested-pucker-round',[{ty:'gr',it:[shape(base),pb,stroke]},rd(8)],f=>rounded(pucker(base,-20+5*f),8));
  add('inherited-pucker-round',[{ty:'gr',it:[shape(base),pb]},rd(8),stroke],f=>rounded(pucker(base,-20+5*f),8));
  add('double-round',[shape(base),{ty:'rd',r:animated([0],[12])},rd(4),stroke],f=>rounded(rounded(base,f*1.2),4));
  add('round-pucker-round',[shape(base),rd(8),pb,rd(4),stroke],f=>rounded(pucker(rounded(base,8),-20+5*f),4));
  const rect = {ty:'rc',p:fixed([32,32]),s:fixed([40,32]),r:fixed(0),d:1};
  const rectangle = {c:true,v:[[52,16],[52,48],[12,48],[12,16]],i:Array.from({length:4},()=>[0,0]),o:Array.from({length:4},()=>[0,0])};
  add('rectangle-round',[rect,{ty:'rd',r:animated([0],[12])},stroke],f=>rounded(rectangle,f*1.2));
  add('rectangle-pucker-round',[rect,pb,rd(8),stroke],f=>rounded(pucker(rectangle,-20+5*f),8));
  const square = a=>({c:true,v:[[12-a,12-a],[52+a,12-a],[52+a,52+a],[12-a,52+a]],i:Array.from({length:4},()=>[0,0]),o:Array.from({length:4},()=>[0,0])});
  add('offset-round',[shape(square(0)),{ty:'op',a:animated([0],[6]),lj:1,ml:fixed(100)},rd(8),stroke],f=>rounded(square(f*0.6),8));
  const polystar = {ty:'sr',sy:1,d:1,pt:animated([3],[7]),p:fixed([32,32]),r:fixed(0),or:fixed(23),ir:fixed(10),os:fixed(0),is:fixed(0)};
  add('star-pucker-round',[polystar,pb,rd(4),stroke],f=>rounded(pucker(star(3+f*0.4,false,false),-20+5*f),4));
  return output;
}
const result = JSON.stringify({source:root+'shapes/RoundCornersModifier.js',
  cases:process.argv.includes('--modifier-order') ? orderedCases() : cases},
  (_,v)=>typeof v==='number'?Math.round(v*1e6)/1e6:v);
const destination = process.argv.indexOf('--output');
if (destination >= 0) writeFileSync(process.argv[destination+1],result+'\n');
else console.log(result);
