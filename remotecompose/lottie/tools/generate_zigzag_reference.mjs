// Generate independent geometry fixtures using the pinned lottie-web implementation.
// Run with Node 18+: node remotecompose/lottie/tools/generate_zigzag_reference.mjs
// Prints JSON; no production Kotlin code is imported or executed.
const root = 'https://raw.githubusercontent.com/airbnb/lottie-web/v5.12.2/player/js/utils/';
async function source(path) {
  const response = await fetch(root + path);
  if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`);
  return response.text();
}
const polynomial = (await source('PolynomialBezier.js')).replace(/export\s*\{[^}]*\};/s, '');
const PolynomialBezier = new Function(polynomial + '; return PolynomialBezier;')();
const modifier = (await source('shapes/ZigZagModifier.js'))
  .replace(/^import[\s\S]*?from [^;]*;\s*/gm, '')
  .replace('export default ZigZagModifier;', '');
const newPath = () => ({
  c: false, v: [], i: [], o: [],
  get _length() { return this.v.length; },
  length() { return this.v.length; },
  setTripleAt(x, y, ox, oy, ix, iy, index) {
    this.v[index] = [x, y]; this.o[index] = [ox, oy]; this.i[index] = [ix, iy];
  },
});
const ZigZag = new Function('extendPrototype', 'ShapeModifier', 'shapePool', 'PolynomialBezier',
  modifier + '; return ZigZagModifier;')(() => {}, function() {}, { newElement: newPath }, PolynomialBezier);
function square(dx = 0) {
  return {c: true, v: [[16+dx,16],[48+dx,16],[48+dx,48],[16+dx,48]],
    i: [[0,0],[0,0],[0,0],[0,0]], o: [[0,0],[0,0],[0,0],[0,0]]};
}
function curve(bend = 0) {
  return {c: false, v: [[8,36],[32,20],[56,36]],
    i: [[0,0],[-8,12+bend],[-8,-12]], o: [[8,-12-bend],[8,12],[0,0]]};
}
function expected(input, amplitude, frequency, pointType) {
  if (amplitude === 0) return input;
  const path = newPath(); path.c = input.c;
  input.v.forEach(([x,y], n) => path.setTripleAt(x,y,
    x+input.o[n][0],y+input.o[n][1],x+input.i[n][0],y+input.i[n][1],n));
  const output = new ZigZag().processPath(path, amplitude, Math.max(0, Math.round(frequency)), pointType);
  return {c: output.c, v: output.v,
    i: output.i.map((p,n) => p.map((x,j) => x-output.v[n][j])),
    o: output.o.map((p,n) => p.map((x,j) => x-output.v[n][j]))};
}
const cases = [];
for (const pt of [1,2]) for (const geometry of ['square','curve']) {
  for (const [size,ridges] of [[4,0],[4,0.49],[4,0.5],[4,2.4],[-4,2.5],[0,3]]) {
    const input = geometry === 'square' ? square() : curve();
    cases.push({name: `${geometry}-${pt}-${size}-${ridges}`, input, size, ridges, pt,
      expected: expected(input, size, ridges, pt)});
  }
}
for (const pt of [1,2]) for (const closed of [true,false]) for (const count of [1,2]) {
  const input = {c: closed, v: Array.from({length:count}, () => [32,32]),
    i: Array.from({length:count}, () => [0,0]), o: Array.from({length:count}, () => [0,0])};
  cases.push({name: `degenerate-${pt}-${closed}-${count}`, input, size: 4, ridges: 2, pt,
    expected: expected(input, 4, 2, pt)});
}
const motion = [];
for (const geometry of ['curve','square']) for (const pt of [1,2]) for (const kind of ['size','ridges','geometry']) {
  const frames = Array.from({length: 11}, (_,frame) => {
    const input = (geometry === 'square' ? square : curve)(kind === 'geometry' ? frame * 0.8 : 0);
    const size = kind === 'size' ? -4 + frame : 4;
    const ridges = kind === 'ridges' ? frame * 0.4 : 3;
    return {frame, expected: expected(input, size, ridges, pt)};
  });
  motion.push({name: `${geometry === 'square' ? 'square-' : ''}${kind}-${pt}`, kind, pt,
    start: (geometry === 'square' ? square : curve)(0),
    end: (geometry === 'square' ? square : curve)(kind === 'geometry' ? 8 : 0), frames});
}
for (const geometry of ['curve', 'square']) for (const kind of ['type-static', 'type-hold', 'type-linear', 'type-reverse']) {
  const start = (geometry === 'square' ? square : curve)(0);
  const frames = Array.from({length: 11}, (_, frame) => {
    const pt = kind === 'type-static' ? 2 : kind === 'type-hold' ? (frame < 5 ? 1 : frame < 8 ? 2 : 1)
      : kind === 'type-linear' ? 1 + frame / 5 : 3 - frame / 5;
    return {frame, expected: expected(start, 4, 3, pt)};
  });
  motion.push({name: `${geometry}-${kind}`, kind, pt: 2, start, end: start, frames});
}
console.log(JSON.stringify({source: root+'shapes/ZigZagModifier.js', cases, motion},
  (_, value) => typeof value === 'number' ? Math.round(value * 1e6) / 1e6 : value));
