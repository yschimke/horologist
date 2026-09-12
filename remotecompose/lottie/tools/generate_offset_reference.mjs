// Independent offset-path fixtures from pinned lottie-web; stdout JSON, no Kotlin involved.
import { writeFileSync } from 'node:fs';
const root = 'https://raw.githubusercontent.com/airbnb/lottie-web/v5.12.2/player/js/utils/';
async function source(path) {
  const response = await fetch(root + path);
  if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`);
  return response.text();
}
const polynomial = (await source('PolynomialBezier.js')).replace(/export\s*\{[^}]*\};/s, '');
const helpers = new Function(polynomial + '; return {PolynomialBezier, polarOffset, lineIntersection, pointDistance, pointEqual, floatEqual};')();
// The shipping reference clips at a coarse 2-coordinate-unit intersection tolerance.
// Keep that audit unchanged by default; a separate fixture uses the same upstream
// solver at higher precision to test geometry, without copying its coarse clipping error.
const precise = process.argv.includes('--precise-intersections');
const ordered = process.argv.includes('--ordered-intersections');
if (ordered && (!precise || !process.argv.includes('--logical-topology'))) {
  throw new Error('--ordered-intersections requires --precise-intersections --logical-topology');
}
let orderIntersections = ordered;
if (precise) {
  const intersections = helpers.PolynomialBezier.prototype.intersections;
  helpers.PolynomialBezier.prototype.intersections = function(other) {
    const roots = intersections.call(this, other, 0.00001, 20);
    // Upstream's traversal does not return roots in contour order. Its offset modifier
    // takes the first result, which can be a near-endpoint candidate before a real crossing.
    // This opt-in oracle correction is explicit; topology frames retain webExpected below.
    return orderIntersections ? roots.slice().sort((a,b)=>a[0]-b[0]) : roots;
  };
}
const modifier = (await source('shapes/OffsetPathModifier.js'))
  .replace(/^import[\s\S]*?from [^;]*;\s*/gm, '')
  .replace('export default OffsetPathModifier;', '');
const newPath = () => ({
  c: false, v: [], i: [], o: [],
  length() { return this.v.length; },
  setTripleAt(x,y,ox,oy,ix,iy,n) { this.v[n]=[x,y]; this.o[n]=[ox,oy]; this.i[n]=[ix,iy]; },
  setXYAt(x,y,kind,n) { this[kind][n]=[x,y]; },
});
const common = await source('common.js');
const roundCorner = Number(common.match(/roundCorner\s*=\s*([\d.]+)/)[1]);
const Offset = new Function('extendPrototype','ShapeModifier','shapePool','roundCorner', ...Object.keys(helpers),
  modifier + '; return OffsetPathModifier;')(() => {}, function() {}, {newElement:newPath}, roundCorner, ...Object.values(helpers));
function shape(name, change=0) {
  let v, i, o, c=true;
  if (name === 'square' || name === 'reverse') {
    v=[[16+change,16],[48+change,16],[48+change,48],[16+change,48]];
    if (name === 'reverse') v.reverse();
  } else if (name === 'concave') {
    v=[[8,8],[56,8],[56,56],[32,28+change],[8,56]];
  } else if (name === 'curve') {
    c=false; v=[[8,36],[32,20],[56,36]];
    i=[[0,0],[-8,12+change],[-8,-12]]; o=[[8,-12-change],[8,12],[0,0]];
  } else if (name === 'inflection') {
    c=false; v=[[8,32],[56,32]]; i=[[0,0],[-16,-20-change]]; o=[[16,20+change],[0,0]];
  } else if (name === 'split') {
    c=false; v=[[8,32],[56,32]]; i=[[0,0],[-8,-28]]; o=[[12,28+change],[0,0]];
  } else if (name === 'double') {
    c=false; v=[[8,32],[56,32]]; i=[[0,0],[-52,8-change]]; o=[[36,24+change],[0,0]];
  } else {
    c=false; v=[[8,40],[32,16+change],[56,40]];
  }
  return {c,v,i:i??v.map(()=>[0,0]),o:o??v.map(()=>[0,0])};
}
function expected(input, amount, join, limit, orderedRoots = ordered) {
  if (amount===0) return input;
  const path=newPath(); path.c=input.c;
  input.v.forEach(([x,y],n)=>path.setTripleAt(x,y,x+input.o[n][0],y+input.o[n][1],x+input.i[n][0],y+input.i[n][1],n));
  const previousOrder = orderIntersections;
  let out;
  try {
    orderIntersections = orderedRoots;
    out=new Offset().processPath(path,amount,join,limit);
  } finally {
    orderIntersections = previousOrder;
  }
  return {c:out.c,v:out.v,i:out.i.map((p,n)=>p.map((x,j)=>x-out.v[n][j])),o:out.o.map((p,n)=>p.map((x,j)=>x-out.v[n][j]))};
}
const cases=[];
for (const geometry of ['square','reverse','concave','line','curve','inflection',...(precise?['split','double']:[])]) {
  for (const join of [1,2,3]) for (const amount of [-4,4]) {
    const input=shape(geometry), limit=8;
    cases.push({name:`${geometry}-${join}-${amount}`,input,amount,join,limit,expected:expected(input,amount,join,limit)});
  }
}
const motion=[];
for (const geometry of ['square','line','curve','inflection',...(precise?['split','double']:[])]) for (const join of [1,2,3]) {
  for (const kind of ['amount','geometry','limit']) {
    const frames=Array.from({length:11},(_,frame)=>{
      const amount=kind==='amount' ? frame-4 : 4;
      const limit=kind==='limit' ? frame : 8;
      return {frame,expected:expected(shape(geometry,kind==='geometry'?frame*0.4:0),amount,join,limit)};
    });
    motion.push({name:`${geometry}-${join}-${kind}`,kind,geometry,join,start:shape(geometry),
      end:shape(geometry,kind==='geometry'?4:0),frames});
  }
}
const curvesOnly = item => /^(curve|inflection|split|double)-/.test(item.name);
async function topologyCases() {
  const roundSource = (await source('shapes/RoundCornersModifier.js'))
    .replace(/^import[\s\S]*?from [^;]*;\s*/gm, '')
    .replace('export default RoundCornersModifier;', '');
  const Round = new Function('extendPrototype','ShapeModifier','shapePool','roundCorner',
    roundSource + '; return RoundCornersModifier;')(() => {}, function(){},
      {newElement:newPath}, roundCorner);
  function rounded(input, radius) {
    if (radius === 0) return input;
    const path = newPath(); path.c = input.c; path._length = input.v.length;
    input.v.forEach(([x,y],n)=>path.setTripleAt(x,y,x+input.o[n][0],y+input.o[n][1],
      x+input.i[n][0],y+input.i[n][1],n));
    const out = new Round().processPath(path, radius);
    return {c:out.c,v:out.v,i:out.i.map((p,n)=>p.map((x,j)=>x-out.v[n][j])),
      o:out.o.map((p,n)=>p.map((x,j)=>x-out.v[n][j]))};
  }
  const fixed = k=>({a:0,k});
  const animated = (a,b)=>({a:1,k:[{t:0,s:a,o:{x:0,y:0},i:{x:1,y:1}},{t:10,s:b}]});
  const output = [];
  for (const [name, polygon, rounding] of [
    ['polygon',true,0],['rounded-polygon',true,60],['rounded-star',false,60],
  ]) {
    const shape = {ty:'sr',sy:polygon?2:1,d:1,pt:animated([3],[7]),p:fixed([32,32]),
      r:fixed(0),or:fixed(20),ir:fixed(10),os:fixed(rounding),is:fixed(rounding)};
    function geometry(points) {
      const count = polygon?Math.floor(points):Math.ceil(points)*2;
      const fraction = points-Math.floor(points), step=2*Math.PI/(polygon?count:points);
      let angle = -Math.PI/2 + (!polygon && fraction?step/2*(1-fraction):0);
      const v=[],i=[],o=[];
      for(let j=0;j<count;j++) {
        const radius=polygon?20:j===0&&fraction?10+10*fraction:j%2?10:20;
        const controlRadius=polygon||j%2===0?20:10;
        const length=controlRadius*rounding/100*(polygon?0.25:0.47829)*
          (!polygon&&j===0&&fraction?fraction:1);
        v.push([32+radius*Math.cos(angle),32+radius*Math.sin(angle)]);
        i.push([length*Math.sin(angle),-length*Math.cos(angle)]);
        o.push(i[i.length-1].map(x=>-x));
        angle+=polygon?step:j===0&&fraction?step*fraction/2:step/2;
      }
      return {c:true,v,i,o};
    }
    output.push({name,input:[shape],frames:Array.from({length:11},(_,frame)=>{
      const sourcePath = geometry(3+frame*0.4);
      const webExpected = expected(sourcePath,2,1,100,false);
      if (polygon && rounding === 0) {
        // A regular polygon's parallel-edge offset has circumradius r + a/cos(pi/n).
        // Keep both equivalent representations: web inserts collinear cubic/join vertices,
        // whose Android antialiasing differs from the minimal analytic straight contour.
        const scale = (20+2/Math.cos(Math.PI/sourcePath.v.length))/20;
        return {frame,webExpected,expected:{...sourcePath,
          v:sourcePath.v.map(p=>p.map(x=>32+(x-32)*scale))}};
      }
      return ordered ? {frame,webExpected,expected:expected(sourcePath,2,1,100,true)} :
        {frame,expected:webExpected};
    })});
  }
  output.push({name:'rounded-corners',input:[{ty:'sh',ks:fixed(shape('square'))},
    {ty:'rd',r:animated([0],[12])}],frames:Array.from({length:11},(_,frame)=>{
      const sourcePath = rounded(shape('square'),frame*1.2);
      const webExpected = expected(sourcePath,2,1,100,false);
      return ordered ? {frame,webExpected,expected:expected(sourcePath,2,1,100,true)} :
        {frame,expected:webExpected};
    })});
  // Deterministic curved contour exercising the first-left/last-right split-group crossing.
  const splitPrune = {c:true,v:[[16,16],[48,16],[48,48],[16,48]],
    i:[[20,-24],[-20,12],[8,20],[-12,16]],
    o:[[-12,-16],[-20,-20],[16,-8],[-8,-24]]};
  const translated = x=>({...splitPrune,v:splitPrune.v.map(p=>[p[0]+x,p[1]])});
  output.push({name:'split-prune',input:[{ty:'sh',ks:animated([translated(0)],[translated(4)])}],
    frames:Array.from({length:11},(_,frame)=>{
      const source = translated(frame*.4);
      const webExpected = expected(source,4,1,100,false);
      return ordered ? {frame,source,webExpected,expected:expected(source,4,1,100,true)} :
        {frame,source,expected:webExpected};
    })});
  for (const [name,start,end] of [['split-prune-amount',0,8],['split-prune-signed',-4,4]]) {
    output.push({name,input:[{ty:'sh',ks:fixed(splitPrune)}],
      amount:animated([start],[end]),frames:Array.from({length:11},(_,frame)=>{
        const amount=start+(end-start)*frame/10;
        const webExpected=expected(splitPrune,amount,1,100,false);
        return ordered ? {frame,amount,webExpected,expected:expected(splitPrune,amount,1,100,true)} :
          {frame,amount,expected:webExpected};
      })});
  }
  return output;
}
const result = JSON.stringify({source:root+'shapes/OffsetPathModifier.js',roundCorner,
  ...(precise ? {intersectionTolerance:0.00001, intersectionMaxRecursion:20} : {}),
  ...(ordered ? {intersectionOrder:'ascending-t',originalOrderRetained:'webExpected'} : {}),
  ...(process.argv.includes('--logical-topology') ? {cases:await topologyCases()} : {
    cases:precise ? cases.filter(curvesOnly) : cases,
    motion:precise ? motion.filter(curvesOnly) : motion})},
  (_,v)=>typeof v==='number'?Math.round(v*1e6)/1e6:v);
const destination = process.argv.indexOf('--output');
if (destination >= 0) writeFileSync(process.argv[destination+1],result+'\n');
else console.log(result);
