// ---------- pixel textures: 16 pixels to a block, drawn here pixel by pixel ----------
function hx(c) { c = c.replace("#", ""); return [parseInt(c.slice(0, 2), 16), parseInt(c.slice(2, 4), 16), parseInt(c.slice(4, 6), 16)]; }
function mixc(a, b, t) { return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t]; }
function shadec(a, k) { return k >= 0 ? mixc(a, [255, 255, 255], Math.min(1, k)) : mixc(a, [0, 0, 0], Math.min(1, -k)); }
function hashStr(s) { let h = 2166136261; for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619); return h >>> 0; }
function rng(seed) { let s = (seed >>> 0) || 1; return () => { s ^= s << 13; s >>>= 0; s ^= s >>> 17; s ^= s << 5; s >>>= 0; return (s % 100003) / 100003; }; }

class Pix {
  constructor(w, h) { this.w = w; this.h = h; this.d = new Uint8ClampedArray(w * h * 4); }
  set(x, y, c, a = 1) { x |= 0; y |= 0; if (x < 0 || y < 0 || x >= this.w || y >= this.h) return; const i = (y * this.w + x) * 4; this.d[i] = c[0]; this.d[i + 1] = c[1]; this.d[i + 2] = c[2]; this.d[i + 3] = a * 255; }
  get(x, y) { x = Math.max(0, Math.min(this.w - 1, x | 0)); y = Math.max(0, Math.min(this.h - 1, y | 0)); const i = (y * this.w + x) * 4; return [this.d[i], this.d[i + 1], this.d[i + 2]]; }
  alpha(x, y) { const i = ((y | 0) * this.w + (x | 0)) * 4; return this.d[i + 3] / 255; }
  rect(x, y, w, h, c, a = 1) { for (let j = y; j < y + h; j++) for (let i = x; i < x + w; i++) this.set(i, j, c, a); }
  noise(x, y, w, h, c, n, r) { for (let j = y; j < y + h; j++) for (let i = x; i < x + w; i++) this.set(i, j, shadec(c, (r() - 0.5) * n)); }
  fill(c, n = 0, r = Math.random) { this.noise(0, 0, this.w, this.h, c, n, r); }
  // A raised panel: lit along the top and left, shaded along the bottom and right (light from the upper left, as everywhere here).
  bevel(x, y, w, h, c, k = 0.16, n = 0, r = Math.random) {
    for (let j = y; j < y + h; j++) for (let i = x; i < x + w; i++) {
      let v = n ? shadec(c, (r() - 0.5) * n) : c;
      if (j === y || i === x) v = shadec(v, k); else if (j === y + h - 1 || i === x + w - 1) v = shadec(v, -k * 1.3);
      this.set(i, j, v);
    }
  }
  line(x0, y0, x1, y1, c, a = 1) { const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) || 1; for (let i = 0; i <= n; i++) this.set(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), c, a); }
  disc(cx, cy, rad, c, a = 1) { for (let j = Math.floor(cy - rad); j <= cy + rad; j++) for (let i = Math.floor(cx - rad); i <= cx + rad; i++) if ((i - cx) ** 2 + (j - cy) ** 2 <= rad * rad) this.set(i, j, c, a); }
  ring(cx, cy, rad, c, a = 1) { for (let t = 0; t < 64; t++) this.set(Math.round(cx + Math.cos(t / 64 * 6.283) * rad), Math.round(cy + Math.sin(t / 64 * 6.283) * rad), c, a); }
  text(s, x, y, c, a = 1) { let cx = x; for (const ch of s.toUpperCase()) { const g = FONT3[ch]; if (g) for (let j = 0; j < 5; j++) for (let i = 0; i < 3; i++) if (g[j * 3 + i] === "1") this.set(cx + i, y + j, c, a); cx += ch === " " ? 3 : 4; } return cx; }
  canvas() { const cv = document.createElement("canvas"); cv.width = this.w; cv.height = this.h; cv.getContext("2d").putImageData(new ImageData(this.d, this.w, this.h), 0, 0); return cv; }
}
function textWidth(s) { let n = 0; for (const ch of s) n += ch === " " ? 3 : 4; return n - 1; }
// A 3 by 5 pixel font for signs, boards and screens.
const FONT3 = {};
("A010101111101101 B110101110101110 C011100100100011 D110101101101110 E111100110100111 F111100110100100 G011100101101011 H101101111101101 I111010010010111 " +
 "J001001001101010 K101101110101101 L100100100100111 M101111111101101 N110101101101101 O010101101101010 P110101110100100 Q010101101110011 R110101110101101 " +
 "S011100010001110 T111010010010010 U101101101101111 V101101101101010 W101101111111101 X101101010101101 Y101101010010010 Z111001010100111 0111101101101111 " +
 "1010110010010111 2110001010100111 3110001010001110 4101101111001001 5111100110001110 6011100111101111 7111001010010010 8111101111101111 9111101111001110 " +
 ".000000000000010 !010010010000010 ?110001010000010 -000000111000000 :000010000010000 '010010000000000 /001001010100100 +000010111010000 &010101010101011")
  .split(" ").forEach(t => { FONT3[t[0]] = t.slice(1); });
FONT3[" "] = "000000000000000";

const C = {
  oak: hx("#C99A5E"), oakL: hx("#E2B87D"), oakD: hx("#9F6E3C"), gap: hx("#6A4322"),
  walnut: hx("#7C5134"), walnutL: hx("#9A6A46"), walnutD: hx("#56351F"),
  cream: hx("#F3EAD7"), white: hx("#F6F4EE"), mint: hx("#BFE3D2"), teal: hx("#2F8C7A"), tealD: hx("#1E5F54"),
  cap: hx("#2B2522"), steel: hx("#B9C3C9"), steelD: hx("#7D8990"), black: hx("#23262B"), ink: hx("#26304A"),
  red: hx("#D9524E"), redD: hx("#9E3433"), blue: hx("#4C86C8"), blueD: hx("#2F5D96"), yellow: hx("#F2C744"), green: hx("#5DAE52"), greenD: hx("#3B7D3A"),
  leaf: hx("#4E9A48"), leafL: hx("#7CC466"), leafD: hx("#2F6B34"), sky: hx("#8FD0F2"), skyL: hx("#D9F1FB"), terracotta: hx("#C2693F"),
  skin: hx("#E9B48C"), pink: hx("#F08AA8"), purple: hx("#8E6BC8"), orange: hx("#F08A3C"), gold: hx("#E8B93A"), goldL: hx("#FBE38A")
};
const BOOKS = ["#C2483F", "#3F6FB0", "#E2B13C", "#4E9A55", "#8A5AB8", "#E07A3A", "#2F7F86", "#D9D0BD", "#5A4A3E", "#B8476E"].map(hx);

function planks(P, r, base, lt, dk, gap, rows = 4, run = 16) {
  const n = P.h / rows;
  for (let row = 0; row < n; row++) {
    const y0 = row * rows, tone = (r() - 0.5) * 0.14, seam = (r() * P.w) | 0, phase = r() * 9;
    for (let y = y0; y < y0 + rows; y++) for (let x = 0; x < P.w; x++) {
      let c = shadec(base, tone + (r() - 0.5) * 0.05);
      const g = Math.sin(x * 0.55 + phase + Math.sin(x * 0.21 + row * 1.7) * 2.2 + y * 0.9);
      if (g > 0.86 && y !== y0) c = mixc(c, dk, 0.4);
      if (y === y0) c = mixc(c, lt, 0.55);
      if (y === y0 + rows - 1) c = mixc(c, gap, 0.45);
      if (x === seam || (run < P.w && x === (seam + run) % P.w)) c = gap;
      P.set(x, y, c);
    }
  }
}
function fabric(P, r, base, k = 0.07) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, shadec(base, ((x + y) % 2 ? k : -k) * 0.6 + (r() - 0.5) * k));
}
function wallpaper(P, r, base, stripe, motif) {
  P.fill(base, 0.03, r);
  for (let y = 0; y < P.h; y++) { for (const sx of [0, 8]) { P.set(sx, y, mixc(P.get(sx, y), stripe, 0.55)); P.set(sx + 1, y, mixc(P.get(sx + 1, y), stripe, 0.3)); } }
  if (motif) { for (const [mx, my] of [[4, 3], [12, 11]]) { P.set(mx, my, motif); P.set(mx - 1, my + 1, motif, 0.6); P.set(mx + 1, my + 1, motif, 0.6); P.set(mx, my + 2, motif, 0.6); } }
}
function leaves(P, r, base, lt, dk, hole = 0.16) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) {
    const v = r();
    if (v < hole) { P.set(x, y, dk, 0); continue; }
    let c = shadec(base, (r() - 0.5) * 0.18);
    if (v > 0.86) c = lt; else if (v < 0.3) c = dk;
    P.set(x, y, c);
  }
  // A few leaves picked out: a lit edge over a dark one.
  for (let i = 0; i < P.w * P.h / 40; i++) { const x = (r() * P.w) | 0, y = (r() * P.h) | 0; P.set(x, y, shadec(lt, 0.2)); P.set(x + 1, y, lt); P.set(x, y + 1, dk); }
}

// Minecraft-style blocks: 16 pixels to a block, four boards to a plank, soft grain; colours as the world's blocks.
function mcPlanks(P, r, base) {
  const lt = shadec(base, 0.16), dk = shadec(base, -0.2), gap = shadec(base, -0.42);
  for (let row = 0; row < 4; row++) {
    const y0 = row * 4, seam = [3, 11, 7, 14][row], tone = [0.02, -0.04, 0.03, -0.02][row];
    for (let y = y0; y < y0 + 4; y++) for (let x = 0; x < 16; x++) {
      let c = shadec(base, tone + (r() - 0.5) * 0.05);
      if (((x * 7 + row * 5 + y * 3) % 11) === 0 && y !== y0 + 3) c = mixc(c, dk, 0.35);
      if (y === y0) c = mixc(c, lt, 0.35);
      if (y === y0 + 3) c = mixc(c, gap, 0.55);
      if (x === seam) c = mixc(c, gap, 0.7);
      P.set(x, y, c);
    }
  }
}
function mcStone(P, r, base, n = 0.07, spots = 0) { P.fill(base, n, r); for (let i = 0; i < spots; i++) { const x = r() * 16, y = r() * 16; P.set(x, y, shadec(base, (r() - 0.5) * 0.4)); P.set(x + 1, y, shadec(base, (r() - 0.5) * 0.25)); } }
function mcWool(P, r, base) { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(base, (((x >> 1) + (y >> 1)) % 2 ? 0.035 : -0.035) + (r() - 0.5) * 0.06)); for (let i = 0; i < 10; i++) P.set(r() * 16, r() * 16, shadec(base, 0.1)); }
function mcCheck(P, r, a, b) { for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) { const c = (tx + ty) % 2 ? b : a; mcStone({ fill: (col, n, rr) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(tx * 16 + x, ty * 16 + y, shadec(col, (rr() - 0.5) * n)); }, set: (x, y, col) => P.set(tx * 16 + (x | 0) % 16, ty * 16 + (y | 0) % 16, col) }, r, c, 0.05, 6); } }
const TEXDEF = {
  mcOak: [16, 16, (P, r) => mcPlanks(P, r, hx("#B8945F"))], mcSpruce: [16, 16, (P, r) => mcPlanks(P, r, hx("#7A5A38"))], mcBirch: [16, 16, (P, r) => mcPlanks(P, r, hx("#D9C48C"))],
  mcPale: [16, 16, (P, r) => mcPlanks(P, r, hx("#E6DAD2"))], mcDarkOak: [16, 16, (P, r) => mcPlanks(P, r, hx("#4E3622"))], mcAcacia: [16, 16, (P, r) => mcPlanks(P, r, hx("#B4623A"))],
  mcCream: [16, 16, (P, r) => mcStone(P, r, hx("#EFE6D2"), 0.05, 5)], mcSky: [16, 16, (P, r) => mcStone(P, r, hx("#CFE2EE"), 0.05, 5)], mcSage: [16, 16, (P, r) => mcStone(P, r, hx("#DDE3D2"), 0.05, 5)],
  mcNavy: [16, 16, (P, r) => mcStone(P, r, hx("#33506F"), 0.06, 6)],
  mcQuartzCheck: [32, 32, (P, r) => mcCheck(P, r, hx("#EEEAE2"), hx("#BFD8CC"))], mcBlueCheck: [32, 32, (P, r) => mcCheck(P, r, hx("#EEF2F4"), hx("#9CC4E0"))],
  mcStripped: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#C9A877"), (x % 5 === 0 ? -0.08 : 0) + (r() - 0.5) * 0.05)); }],
  mcLabTile: [32, 32, (P, r) => mcCheck(P, r, hx("#EEF0EF"), hx("#DCE2E1"))],
  mcLabWall: [16, 16, (P, r) => mcStone(P, r, hx("#F2F4F3"), 0.035, 3)],
  mcCyan: [16, 16, (P, r) => mcStone(P, r, hx("#6FA9B0"), 0.05, 4)],
  labPanel: [16, 16, (P, r) => { P.fill(hx("#D9DEDD"), 0.03, r); P.rect(0, 0, 16, 1, hx("#F4F6F5")); P.rect(0, 15, 16, 1, hx("#A9B2B0")); P.rect(2, 3, 5, 4, hx("#2B3A44")); P.rect(3, 4, 3, 2, hx("#58D3A0"));
    P.rect(9, 3, 2, 2, hx("#E5525C")); P.rect(12, 3, 2, 2, hx("#F2C12E")); P.rect(9, 6, 2, 2, hx("#5AA9E6")); P.rect(12, 6, 2, 2, hx("#58D3A0"));
    for (let i = 0; i < 6; i++) P.rect(2 + i * 2, 10, 1, 3, hx("#7E8886")); P.rect(2, 13, 12, 1, hx("#7E8886")); }],
  vent: [16, 16, (P, r) => { P.fill(hx("#B9C1BF"), 0.03, r); for (let y = 2; y < 16; y += 3) P.rect(1, y, 14, 1, hx("#7E8886")); }],
  mcGlass: [16, 16, (P, r) => { P.fill(hx("#DDEFF5"), 0.02, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#BFD6DD")); P.rect(0, 0, 1, 16, hx("#FFFFFF")); P.rect(15, 0, 1, 16, hx("#BFD6DD")); P.line(3, 6, 6, 3, hx("#FFFFFF")); P.line(3, 9, 9, 3, hx("#FFFFFF")); }],

  oak: [32, 32, (P, r) => planks(P, r, C.oak, C.oakL, C.oakD, C.gap, 4, 16)],
  walnut: [32, 32, (P, r) => planks(P, r, C.walnut, C.walnutL, C.walnutD, hx("#3E2614"), 4, 16)],
  honey: [32, 32, (P, r) => planks(P, r, hx("#D8A866"), hx("#EDC88B"), hx("#B07E45"), hx("#7A5129"), 4, 32)],
  pale: [32, 32, (P, r) => planks(P, r, hx("#E3C79A"), hx("#F1DDB8"), hx("#C4A472"), hx("#9C7C4E"), 4, 16)],
  oakFloor: [32, 32, (P, r) => planks(P, r, hx("#C99A5E"), hx("#E0B57A"), hx("#A5733F"), hx("#7A4E2A"), 8, 32)],
  honeyFloor: [32, 32, (P, r) => planks(P, r, hx("#D9AA68"), hx("#EDC88B"), hx("#B9894F"), hx("#86592F"), 8, 32)],
  paleFloor: [32, 32, (P, r) => planks(P, r, hx("#E2C899"), hx("#F1DDB8"), hx("#C9AB79"), hx("#A08256"), 8, 32)],
  walnutFloor: [32, 32, (P, r) => planks(P, r, hx("#8A5C3B"), hx("#A5734E"), hx("#6A4429"), hx("#4A2E1A"), 8, 32)],
  tread: [16, 16, (P, r) => planks(P, r, hx("#B98348"), hx("#D9A465"), hx("#8F5F2F"), hx("#5E3B1C"), 4, 16)],
  checker: [16, 16, (P, r) => {
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const c = (tx + ty) % 2 ? C.mint : C.cream;
      P.bevel(tx * 8, ty * 8, 8, 8, c, 0.08, 0.035, r);
      P.rect(tx * 8, ty * 8, 8, 1, hx("#D7CCB4")); P.rect(tx * 8, ty * 8, 1, 8, hx("#D7CCB4"));
      P.set(tx * 8 + 2, ty * 8 + 2, shadec(c, 0.35));
    }
  }],
  tileWhite: [16, 16, (P, r) => {
    for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) { P.bevel(tx * 4, ty * 4, 4, 4, hx("#F4F6F4"), 0.06, 0.03, r); P.rect(tx * 4, ty * 4 + 3, 4, 1, hx("#C9D3D1")); P.rect(tx * 4 + 3, ty * 4, 1, 4, hx("#C9D3D1")); }
  }],
  tileBlue: [16, 16, (P, r) => {
    for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) { const c = (tx + ty) % 3 === 0 ? hx("#9CC8E6") : hx("#E8F2F8"); P.bevel(tx * 4, ty * 4, 4, 4, c, 0.06, 0.03, r); P.rect(tx * 4, ty * 4 + 3, 4, 1, hx("#B8CAD6")); P.rect(tx * 4 + 3, ty * 4, 1, 4, hx("#B8CAD6")); }
  }],
  paperMum: [16, 16, (P, r) => wallpaper(P, r, hx("#F3E7CF"), hx("#C9DDB5"), hx("#E3A9A0"))],
  paperRoom: [16, 16, (P, r) => wallpaper(P, r, hx("#DCEBF3"), hx("#B9D3E6"), hx("#F2D17A"))],
  paperRival: [16, 16, (P, r) => wallpaper(P, r, hx("#E6E8DD"), hx("#C6CDB8"), hx("#8DB0C9"))],
  paperRivalRoom: [16, 16, (P, r) => { P.fill(hx("#2D4A66"), 0.04, r); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if ((x + y) % 8 === 0) P.set(x, y, hx("#3A5D7E")); P.set(3, 4, hx("#F2D17A")); P.set(11, 12, hx("#F2D17A")); }],
  cap: [16, 16, (P, r) => P.fill(C.cap, 0.06, r)],
  paint: [16, 16, (P, r) => P.fill(C.white, 0.03, r)],
  mintPaint: [16, 16, (P, r) => P.fill(hx("#A9D8C4"), 0.035, r)],
  counter: [16, 16, (P, r) => { P.fill(hx("#ECEAE4"), 0.04, r); for (let i = 0; i < 14; i++) P.set(r() * 16, r() * 16, hx("#BDB7AD")); }],
  darkTop: [16, 16, (P, r) => { P.fill(hx("#3B3F46"), 0.06, r); for (let i = 0; i < 10; i++) P.set(r() * 16, r() * 16, hx("#5E646D")); }],
  steel: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) { const t = (r() - 0.5) * 0.08; for (let x = 0; x < 16; x++) P.set(x, y, shadec(C.steel, t + (r() - 0.5) * 0.04 + (y < 2 ? 0.12 : 0))); } }],
  plastic: [16, 16, (P, r) => P.fill(C.black, 0.05, r)],
  greyPlastic: [16, 16, (P, r) => P.fill(hx("#C9CCD2"), 0.04, r)],
  sofa: [16, 16, (P, r) => fabric(P, r, hx("#3F9C8A"))],
  sofaL: [16, 16, (P, r) => fabric(P, r, hx("#5CB8A3"))],
  redFabric: [16, 16, (P, r) => fabric(P, r, hx("#C9524E"))],
  blueFabric: [16, 16, (P, r) => fabric(P, r, hx("#4C7FC0"))],
  cushion: [16, 16, (P, r) => fabric(P, r, hx("#F1C76A"))],
  linen: [16, 16, (P, r) => fabric(P, r, hx("#F7F4EC"), 0.04)],
  quilt: [16, 16, (P, r) => {
    const cols = [hx("#E9806B"), hx("#F6D58A"), hx("#8CC6B5"), hx("#F4EEE2")];
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const c = cols[(tx + ty * 2 + (tx === ty ? 1 : 0)) % 4];
      for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(c, ((x + y) % 2 ? 0.03 : -0.03) + (x === 0 || y === 0 ? -0.12 : 0)));
      P.set(tx * 8 + 4, ty * 8 + 4, shadec(c, -0.25));
    }
  }],
  quiltBlue: [16, 16, (P, r) => {
    const cols = [hx("#3F5F9E"), hx("#2E3F66"), hx("#C9524E"), hx("#E8E2D2")];
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) { const c = cols[(tx * 3 + ty) % 4]; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(c, ((x + y) % 2 ? 0.03 : -0.03) + (x === 0 || y === 0 ? -0.12 : 0))); }
  }],
  leaf: [16, 16, (P, r) => leaves(P, r, C.leaf, C.leafL, C.leafD, 0.14)],
  leafSolid: [16, 16, (P, r) => leaves(P, r, C.leaf, C.leafL, C.leafD, 0)],
  treeLeaf: [16, 16, (P, r) => leaves(P, r, hx("#3F8A44"), hx("#6DBA5A"), hx("#255C2D"), 0)],
  pot: [16, 16, (P, r) => { P.fill(C.terracotta, 0.06, r); P.rect(0, 0, 16, 2, shadec(C.terracotta, 0.15)); P.rect(0, 2, 16, 1, shadec(C.terracotta, -0.2)); }],
  potWhite: [16, 16, (P, r) => { P.fill(hx("#EDEFF0"), 0.04, r); P.rect(0, 0, 16, 2, hx("#FFFFFF")); P.rect(0, 5, 16, 1, hx("#7FB9D8")); }],
  soil: [16, 16, (P, r) => P.fill(hx("#5A3D27"), 0.2, r)],
  gold: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, mixc(C.goldL, C.gold, (Math.sin(x * 0.7 + y * 0.3) + 1) / 2 * 0.8 + r() * 0.2)); }],
  silver: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, mixc(hx("#F2F4F6"), hx("#A9B2BA"), (Math.sin(x * 0.7 + y * 0.3) + 1) / 2 * 0.8 + r() * 0.2)); }],
  curtain: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#E77D6E"), Math.sin(x / 16 * 6.283 * 2) * 0.1 + (r() - 0.5) * 0.04)); }],
  curtainBlue: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#5B8FD0"), Math.sin(x / 16 * 6.283 * 2) * 0.1 + (r() - 0.5) * 0.04)); }],
  leather: [16, 16, (P, r) => { P.fill(hx("#B8433F"), 0.1, r); P.rect(0, 0, 16, 1, hx("#D86A60")); }],
  bark: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#7A5435"), ((x % 4 === 0) ? -0.18 : 0) + (r() - 0.5) * 0.14)); }],
  grass: [16, 16, (P, r) => {
    P.fill(hx("#6DB257"), 0.06, r);
    for (let i = 0; i < 10; i++) { const x = (r() * 16) | 0, y = (r() * 16) | 0; P.set(x, y, hx("#86C66A")); P.set(x, y + 1, hx("#5A9A48")); }
    for (let i = 0; i < 3; i++) P.set(r() * 16, r() * 16, hx("#52913F"));
  }],
  path: [16, 16, (P, r) => { P.fill(hx("#D6BC84"), 0.07, r); for (let i = 0; i < 9; i++) { const x = r() * 16, y = r() * 16; P.set(x, y, hx("#B89E66")); P.set(x + 1, y, hx("#E8D3A2")); } }],
  stone: [16, 16, (P, r) => { P.fill(hx("#B9B6AE"), 0.08, r); P.rect(0, 7, 16, 1, hx("#8F8C85")); P.rect(7, 0, 1, 7, hx("#8F8C85")); P.rect(12, 8, 1, 8, hx("#8F8C85")); }],
  siding: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#F2E8D2"), (y % 4 === 3 ? -0.14 : y % 4 === 0 ? 0.06 : 0) + (r() - 0.5) * 0.03)); }],
  labWall: [16, 16, (P, r) => { P.fill(hx("#F1F3F2"), 0.03, r); P.rect(0, 15, 16, 1, hx("#C7CFCC")); P.rect(15, 0, 1, 16, hx("#D3DAD7")); }],
  roofRed: [16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) { const row = y >> 2, off = row % 2 ? 4 : 0; for (let x = 0; x < 16; x++) { let c = shadec(hx("#C9493F"), (y % 4 === 0 ? 0.12 : y % 4 === 3 ? -0.22 : 0) + (r() - 0.5) * 0.06); if ((x + off) % 8 === 0) c = shadec(c, -0.25); P.set(x, y, c); } }
  }],
  roofBlue: [16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) { const row = y >> 2, off = row % 2 ? 4 : 0; for (let x = 0; x < 16; x++) { let c = shadec(hx("#3E6FA8"), (y % 4 === 0 ? 0.12 : y % 4 === 3 ? -0.22 : 0) + (r() - 0.5) * 0.06); if ((x + off) % 8 === 0) c = shadec(c, -0.25); P.set(x, y, c); } }
  }],
  roofGrey: [16, 16, (P, r) => { P.fill(hx("#7E8C93"), 0.06, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#65727A")); }],
  fence: [16, 16, (P, r) => P.fill(hx("#F4F1EA"), 0.05, r)],
  water: [16, 16, (P, r) => { P.fill(hx("#4BA3D9"), 0.06, r); for (let i = 0; i < 6; i++) { const x = r() * 14, y = r() * 16; P.rect(x | 0, y | 0, 3, 1, hx("#BDE6F7")); } }],
  rubber: [16, 16, (P, r) => P.fill(hx("#3A3F47"), 0.08, r)],
  rugMum: [48, 32, (P, r) => {
    P.fill(hx("#C95A4C"), 0.05, r);
    P.rect(2, 2, 44, 28, hx("#F1E2C4")); P.rect(4, 4, 40, 24, hx("#C95A4C"));
    for (let y = 4; y < 28; y++) for (let x = 4; x < 44; x++) { const d = Math.abs(x - 23.5) / 20 + Math.abs(y - 15.5) / 12; if (d < 0.55 && d > 0.42) P.set(x, y, hx("#F1E2C4")); else if (d < 0.25) P.set(x, y, hx("#E9A23B")); }
    for (let x = 6; x < 44; x += 4) { P.set(x, 3, hx("#9C3E34")); P.set(x, 28, hx("#9C3E34")); }
    for (let i = 0; i < P.w * P.h * 0.04; i++) { const x = r() * 48, y = r() * 32; P.set(x, y, shadec(P.get(x, y), -0.12)); }
  }],
  rugRoom: [32, 32, (P, r) => {
    P.fill(hx("#4E7FC4"), 0.05, r); P.rect(2, 2, 28, 28, hx("#E9F0F7")); P.rect(3, 3, 26, 26, hx("#4E7FC4"));
    for (const [sx, sy] of [[9, 9], [22, 11], [12, 22], [24, 23], [16, 16]]) { P.set(sx, sy, C.yellow); P.set(sx - 1, sy, C.goldL); P.set(sx + 1, sy, C.goldL); P.set(sx, sy - 1, C.goldL); P.set(sx, sy + 1, C.goldL); }
  }],
  rugRival: [32, 32, (P, r) => {
    P.fill(hx("#2F3A44"), 0.05, r);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) if ((((x >> 2) + (y >> 2)) % 2) === 0 && x > 2 && y > 2 && x < 29 && y < 29) P.set(x, y, hx("#E8E2D2"));
    P.rect(0, 0, 32, 2, hx("#C9524E")); P.rect(0, 30, 32, 2, hx("#C9524E"));
  }],
  doormat: [32, 16, (P, r) => {
    P.fill(hx("#B48A55"), 0.16, r);
    P.rect(0, 0, 32, 2, hx("#6E4F2C")); P.rect(0, 14, 32, 2, hx("#6E4F2C")); P.rect(0, 0, 2, 16, hx("#6E4F2C")); P.rect(30, 0, 2, 16, hx("#6E4F2C"));
    for (let x = 4; x < 28; x += 3) P.rect(x, 4, 1, 8, hx("#8A6436"));
  }],
  books: [32, 8, (P, r) => {
    let x = 0;
    while (x < 32) {
      const w = 1 + ((r() * 3) | 0), h = 5 + ((r() * 3) | 0), c = BOOKS[(r() * BOOKS.length) | 0];
      if (r() < 0.12) { x += 1; continue; }
      for (let i = 0; i < w && x + i < 32; i++) for (let j = 8 - h; j < 8; j++) P.set(x + i, j, shadec(c, (i === 0 ? 0.14 : i === w - 1 ? -0.16 : 0) + (j === 8 - h ? 0.18 : 0)));
      if (h > 5) P.set(x, 8 - h + 2, shadec(c, 0.45));
      x += w;
    }
  }],
  // The lab's shelves, packed: books shoulder to shoulder, rows of binders, and white storage boxes.
  booksFull: [32, 8, (P, r) => {
    let x = 0;
    while (x < 32) {
      const w = 1 + ((r() * 3) | 0), h = 6 + ((r() * 3) | 0), c = BOOKS[(r() * BOOKS.length) | 0];
      for (let i = 0; i < w && x + i < 32; i++) for (let j = 8 - Math.min(8, h); j < 8; j++) P.set(x + i, j, shadec(c, (i === 0 ? 0.14 : i === w - 1 ? -0.16 : 0) + (j === 8 - h ? 0.18 : 0)));
      if (h > 6) P.set(x, 8 - h + 2, shadec(c, 0.45));
      x += w;
    }
  }],
  binders: [32, 8, (P, r) => {
    const cols = ["#3A6FD0", "#E5525C", "#F2C12E", "#58A64A", "#7A4FB8", "#2FA3B0"];
    for (let b = 0; b < 8; b++) { const c = hx(cols[(b + ((r() * 2) | 0)) % cols.length]);
      for (let i = 0; i < 4; i++) for (let j = 0; j < 8; j++) P.set(b * 4 + i, j, shadec(c, (i === 0 ? 0.14 : i === 3 ? -0.2 : 0)));
      P.rect(b * 4 + 1, 2, 2, 2, hx("#F4F6F5")); P.set(b * 4 + 1, 6, shadec(c, -0.35)); P.set(b * 4 + 2, 6, shadec(c, -0.35)); }
  }],
  storeBoxes: [32, 8, (P, r) => {
    for (let b = 0; b < 4; b++) { const c = hx(b % 2 ? "#E9E4D8" : "#F4F6F5");
      for (let i = 0; i < 8; i++) for (let j = 0; j < 8; j++) P.set(b * 8 + i, j, shadec(c, (i === 0 ? 0.08 : i === 7 ? -0.18 : 0) + (j === 0 ? 0.08 : 0) + (r() - 0.5) * 0.03));
      P.rect(b * 8 + 2, 3, 4, 2, hx(["#9CC4E0", "#F2C12E", "#E99A2E", "#8FD3A8"][b])); P.rect(b * 8 + 3, 6, 2, 1, shadec(c, -0.3)); }
  }],
  shelfWhite: [16, 16, (P, r) => { P.fill(hx("#F4F5F3"), 0.025, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#D9DDDA")); }],
  cupboardTeal: [16, 16, (P, r) => {
    const c = hx("#3FA7A9");
    P.bevel(0, 0, 16, 4, c, 0.14, 0.02, r); P.rect(6, 1, 4, 1, C.steelD); P.rect(6, 2, 4, 1, C.steel);
    P.bevel(0, 4, 16, 12, c, 0.14, 0.02, r); P.bevel(2, 6, 12, 8, shadec(c, -0.05), 0.08, 0, r); P.rect(12, 8, 1, 3, C.steelD); P.rect(11, 8, 1, 3, C.steel);
  }],
  tealPaint: [16, 16, (P, r) => P.fill(hx("#3FA7A9"), 0.03, r)],
  tankGlass: [16, 16, (P, r) => { P.fill(hx("#BFEFF2"), 0.02, r); P.rect(2, 0, 1, 16, hx("#FFFFFF")); P.rect(4, 0, 1, 16, hx("#E6FBFC")); }],
  redPlastic: [16, 16, (P, r) => { P.fill(hx("#E5525C"), 0.03, r); P.rect(0, 0, 16, 2, hx("#F07C84")); }],
  boardLab: [40, 26, (P, r) => {
    P.fill(hx("#F7F8F6")); P.text("NOTES", 2, 2, hx("#3A6FD0"));
    for (let i = 0; i < 3; i++) { P.rect(2, 9 + i * 5, 2, 2, hx("#E5525C")); P.rect(6, 9 + i * 5, 14 - i * 3, 1, hx("#26304A")); P.rect(6, 11 + i * 5, 9, 1, hx("#7E8886")); }
    P.ring(31, 14, 6, hx("#58A64A"));
    P.rect(29, 12, 1, 1, hx("#26304A")); P.rect(33, 12, 1, 1, hx("#26304A")); P.rect(30, 16, 3, 1, hx("#26304A"));
  }],
  boardMap: [48, 28, (P, r) => {
    P.fill(hx("#F7F8F6")); P.rect(0, 0, 48, 1, hx("#DDE1DE"));
    const land = hx("#8FD3A8"), sea = hx("#9CC4E0");
    P.rect(4, 4, 26, 18, sea); P.rect(6, 6, 6, 12, land); P.rect(12, 6, 14, 5, land); P.rect(20, 11, 6, 9, land); P.rect(12, 13, 6, 5, land);
    for (const [x, y] of [[8, 16], [8, 9], [15, 8], [23, 8], [23, 15], [15, 15]]) P.rect(x, y, 2, 2, hx("#E5525C"));
    P.text("KANTO", 5, 23, hx("#26304A"));
    for (let i = 0; i < 4; i++) { P.rect(33, 5 + i * 5, 12, 1, hx("#3A6FD0")); P.rect(33, 7 + i * 5, 8, 1, hx("#7E8886")); }
    P.rect(34, 24, 6, 2, hx("#E5525C"));
  }],
  cupboard: [16, 16, (P, r) => {
    const c = hx("#7CC4AE");
    P.bevel(0, 0, 16, 4, c, 0.14, 0.02, r); P.rect(6, 1, 4, 1, C.steelD); P.rect(6, 2, 4, 1, C.steel);
    P.bevel(0, 4, 16, 12, c, 0.14, 0.02, r); P.bevel(2, 6, 12, 8, shadec(c, -0.04), 0.08, 0, r); P.rect(12, 8, 1, 3, C.steelD); P.rect(11, 8, 1, 3, C.steel);
  }],
  cupboardWhite: [16, 16, (P, r) => {
    const c = hx("#F1F2EF");
    P.bevel(0, 0, 16, 4, c, 0.1, 0.02, r); P.rect(6, 2, 4, 1, C.steelD);
    P.bevel(0, 4, 16, 12, c, 0.1, 0.02, r); P.bevel(2, 6, 12, 8, shadec(c, -0.05), 0.07, 0, r); P.rect(12, 8, 1, 3, C.steelD);
  }],
  wallCupboard: [16, 16, (P, r) => { const c = hx("#7CC4AE"); P.bevel(0, 0, 16, 16, c, 0.14, 0.02, r); P.bevel(2, 2, 12, 12, shadec(c, -0.04), 0.08, 0, r); P.rect(12, 10, 1, 3, C.steelD); }],
  drawers: [16, 16, (P, r) => {
    const c = hx("#C48A52");
    for (let i = 0; i < 4; i++) { P.bevel(0, i * 4, 16, 4, c, 0.16, 0.03, r); P.rect(6, i * 4 + 1, 4, 1, hx("#6E4524")); P.rect(6, i * 4 + 2, 4, 1, C.goldL); }
  }],
  fridge: [16, 48, (P, r) => {
    const c = hx("#F4F6F5");
    P.bevel(0, 0, 16, 16, c, 0.06, 0.015, r); P.bevel(0, 16, 16, 32, c, 0.06, 0.015, r);
    P.rect(0, 16, 16, 1, hx("#AEB6B6"));
    P.rect(13, 9, 1, 6, C.steelD); P.rect(12, 9, 1, 6, C.steel); P.rect(13, 18, 1, 10, C.steelD); P.rect(12, 18, 1, 10, C.steel);
    // A child's drawing and some magnets.
    P.rect(3, 22, 6, 7, hx("#FFFFFF")); P.line(4, 27, 6, 24, C.blue); P.line(6, 24, 8, 27, C.blue); P.disc(7, 24, 1, C.yellow);
    P.rect(5, 21, 2, 1, C.red); P.disc(3, 33, 1, C.green); P.disc(9, 35, 1, C.orange); P.rect(4, 4, 3, 2, C.purple);
  }],
  oven: [16, 16, (P, r) => {
    P.bevel(0, 0, 16, 16, hx("#E6E9EA"), 0.08, 0.02, r);
    for (const kx of [3, 6, 10, 13]) { P.disc(kx, 2, 1, C.black); P.set(kx, 1, C.steel); }
    P.rect(2, 5, 12, 1, C.steelD); P.rect(2, 6, 12, 1, C.steel);
    P.bevel(3, 8, 10, 6, hx("#1D1F24"), 0.12, 0, r); P.rect(4, 9, 3, 1, hx("#4A5058"));
  }],
  hob: [16, 16, (P, r) => {
    P.fill(hx("#1E2126"), 0.04, r);
    P.ring(4.5, 4.5, 2.6, hx("#5A616B")); P.ring(11.5, 4.5, 2.6, hx("#5A616B")); P.ring(4.5, 11.5, 2.6, hx("#E9643C")); P.ring(4.5, 11.5, 1.5, hx("#F0A25A")); P.ring(11.5, 11.5, 2.6, hx("#5A616B"));
    P.set(1, 1, hx("#6B737D"));
  }],
  sink: [16, 16, (P, r) => {
    P.fill(hx("#ECEAE4"), 0.04, r);
    P.bevel(2, 3, 12, 10, hx("#A8B3BA"), -0.2, 0.03, r); P.rect(3, 4, 10, 8, hx("#BCC6CC")); P.disc(8, 8, 1, hx("#6E7880"));
  }],
  splash: [16, 16, (P, r) => { for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) { const c = (tx + ty) % 2 ? hx("#FFFFFF") : hx("#DDF0E8"); P.bevel(tx * 4, ty * 4, 4, 4, c, 0.06, 0.02, r); P.rect(tx * 4, ty * 4 + 3, 4, 1, hx("#BFD2CB")); } }],
  wardrobe: [32, 48, (P, r) => {
    const c = hx("#E2C392");
    P.bevel(0, 0, 32, 4, shadec(c, -0.08), 0.14, 0.02, r);
    P.bevel(0, 4, 16, 36, c, 0.12, 0.025, r); P.bevel(16, 4, 16, 36, c, 0.12, 0.025, r);
    P.bevel(3, 8, 10, 28, shadec(c, -0.05), 0.07, 0, r); P.bevel(19, 8, 10, 28, shadec(c, -0.05), 0.07, 0, r);
    P.rect(14, 20, 1, 5, hx("#6E4524")); P.rect(17, 20, 1, 5, hx("#6E4524"));
    P.bevel(0, 40, 32, 8, shadec(c, -0.03), 0.12, 0.02, r); P.rect(13, 43, 6, 1, hx("#6E4524"));
  }],
  whiteboard: [96, 48, (P, r) => {
    P.fill(hx("#FBFCFB"), 0.015, r);
    for (let i = 0; i < 30; i++) P.set(r() * 96, r() * 48, hx("#E7ECEB"));
    P.text("TO DO", 6, 5, hx("#2F5D96")); P.line(6, 11, 24, 11, hx("#2F5D96"));
    P.text("1 WAKE UP", 6, 15, hx("#2F5D96")); P.line(6, 17, 40, 17, hx("#D9524E"));
    P.text("2 SEE THE PROF", 6, 23, hx("#2F5D96"));
    P.text("3 BE CHAMPION!", 6, 31, hx("#2F5D96"));
    // A doodle of a little creature, and a ball.
    P.ring(74, 22, 9, hx("#D9524E")); P.line(65, 22, 83, 22, hx("#D9524E")); P.disc(74, 22, 2, hx("#D9524E"));
    P.line(58, 38, 62, 34, hx("#3B7D3A")); P.line(62, 34, 66, 38, hx("#3B7D3A")); P.line(58, 38, 66, 38, hx("#3B7D3A")); P.set(61, 36, hx("#3B7D3A")); P.set(63, 36, hx("#3B7D3A"));
    P.text("GO!", 82, 38, hx("#3B7D3A"));
  }],
  boardRival: [96, 48, (P, r) => {
    P.fill(hx("#FBFCFB"), 0.015, r);
    P.text("BATTLE PLAN", 6, 5, hx("#C9524E")); P.line(6, 11, 50, 11, hx("#C9524E"));
    P.text("WIN", 6, 16, hx("#23262B")); P.text("WIN AGAIN", 6, 24, hx("#23262B")); P.text("BEAT HIM", 6, 32, hx("#23262B"));
    P.line(40, 31, 76, 37, hx("#C9524E")); P.line(40, 37, 76, 31, hx("#C9524E"));
    P.ring(80, 18, 7, hx("#2F5D96")); P.line(73, 18, 87, 18, hx("#2F5D96"));
  }],
  window: [32, 32, (P, r) => {
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) P.set(x, y, mixc(hx("#7EC6EC"), hx("#DDF2FB"), y / 31));
    // Distant hills and trees, a cloud, a streak of reflection.
    for (let x = 0; x < 32; x++) { const h = 24 + Math.round(Math.sin(x * 0.35) * 1.5 + Math.sin(x * 0.9) * 0.8); for (let y = h; y < 32; y++) P.set(x, y, y === h ? hx("#7FC07A") : hx("#5FA05E")); }
    for (const [cx, cy] of [[9, 8], [12, 7], [15, 8], [24, 12], [26, 11]]) P.disc(cx, cy, 2.2, hx("#FFFFFF"));
    P.line(20, 3, 28, 11, hx("#FFFFFF"), 0.6); P.line(22, 3, 30, 11, hx("#FFFFFF"), 0.35);
    P.rect(0, 0, 32, 2, hx("#FFFFFF")); P.rect(0, 30, 32, 2, hx("#FFFFFF")); P.rect(0, 0, 2, 32, hx("#FFFFFF")); P.rect(30, 0, 2, 32, hx("#FFFFFF"));
    P.rect(15, 0, 2, 32, hx("#FFFFFF")); P.rect(0, 15, 32, 2, hx("#FFFFFF"));
    P.rect(2, 2, 13, 1, hx("#D3DEE3")); P.rect(17, 2, 13, 1, hx("#D3DEE3")); P.rect(2, 17, 13, 1, hx("#D3DEE3")); P.rect(17, 17, 13, 1, hx("#D3DEE3"));
  }],
  windowExt: [16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, mixc(hx("#5D9CC7"), hx("#B9DDF0"), (x + y) / 30));
    P.line(9, 2, 14, 7, hx("#E6F5FB")); P.line(10, 2, 14, 6, hx("#E6F5FB"));
    P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#FFFFFF")); P.rect(0, 0, 1, 16, hx("#FFFFFF")); P.rect(15, 0, 1, 16, hx("#FFFFFF")); P.rect(7, 0, 2, 16, hx("#FFFFFF")); P.rect(0, 7, 16, 2, hx("#FFFFFF"));
  }],
  doorExt: [16, 32, (P, r) => {
    const c = hx("#8E5B36");
    P.bevel(0, 0, 16, 32, c, 0.14, 0.04, r);
    P.bevel(3, 3, 10, 9, hx("#A9D6EE"), 0.1, 0, r); P.rect(7, 3, 2, 9, c); P.rect(3, 7, 10, 1, c);
    P.bevel(3, 15, 10, 6, shadec(c, -0.06), 0.1, 0, r); P.bevel(3, 23, 10, 6, shadec(c, -0.06), 0.1, 0, r);
    P.disc(12, 19, 1, C.goldL); P.set(12, 18, hx("#FFFFFF"));
  }],
  doorLab: [16, 32, (P, r) => {
    P.bevel(0, 0, 16, 32, hx("#7E8C93"), 0.12, 0.03, r); P.bevel(2, 3, 12, 20, hx("#A9D6EE"), 0.1, 0, r); P.rect(7, 3, 2, 20, hx("#7E8C93")); P.rect(6, 25, 4, 1, hx("#E6E9EA"));
  }],
  painting: [32, 24, (P, r) => {
    for (let y = 0; y < 24; y++) for (let x = 0; x < 32; x++) P.set(x, y, mixc(hx("#F7C58A"), hx("#8CC9EC"), Math.min(1, y / 12)));
    P.disc(22, 9, 3, hx("#FFF1C2"));
    for (let y = 13; y < 24; y++) for (let x = 0; x < 32; x++) P.set(x, y, shadec(hx("#3F86C2"), (y - 13) * -0.03 + ((x + y * 3) % 7 === 0 ? 0.25 : 0)));
    for (let x = 6; x < 13; x++) P.set(x, 13, hx("#FFFFFF")); P.rect(8, 14, 4, 2, hx("#9E3433")); P.line(10, 7, 10, 13, hx("#5A4A3E")); P.rect(10, 8, 3, 4, hx("#FFFFFF"));
  }],
  paintingHills: [32, 24, (P, r) => {
    for (let y = 0; y < 24; y++) for (let x = 0; x < 32; x++) P.set(x, y, mixc(hx("#BFE6F5"), hx("#EAF7FB"), y / 12));
    for (let x = 0; x < 32; x++) { const h = 13 + Math.round(Math.sin(x * 0.25) * 3); for (let y = h; y < 24; y++) P.set(x, y, shadec(hx("#6DB45E"), (y - h) * -0.02 + (y === h ? 0.2 : 0))); }
    for (let x = 0; x < 32; x++) { const h = 18 + Math.round(Math.sin(x * 0.4 + 2) * 2); for (let y = h; y < 24; y++) P.set(x, y, shadec(hx("#4E9A48"), (y - h) * -0.03)); }
    P.rect(20, 14, 5, 4, hx("#F2E8D2")); P.rect(19, 12, 7, 2, hx("#C9493F"));
  }],
  poster: [24, 32, (P, r) => {
    for (let y = 0; y < 32; y++) for (let x = 0; x < 24; x++) P.set(x, y, mixc(hx("#F2C744"), hx("#F08A3C"), y / 31));
    for (let t = 0; t < 12; t++) P.line(12, 13, 12 + Math.cos(t / 12 * 6.283) * 18, 13 + Math.sin(t / 12 * 6.283) * 18, hx("#F6D86A"), 0.6);
    // A trainer mid-throw, in silhouette.
    P.disc(10, 8, 2, C.ink); P.rect(9, 10, 3, 8, C.ink); P.line(11, 11, 16, 7, C.ink); P.line(9, 11, 6, 15, C.ink); P.line(9, 18, 7, 24, C.ink); P.line(11, 18, 14, 24, C.ink);
    P.disc(18, 5, 1.6, hx("#D9524E")); P.set(18, 5, hx("#FFFFFF"));
    P.text("GO!", 7, 26, hx("#FFFFFF"));
  }],
  posterRival: [24, 32, (P, r) => {
    P.fill(hx("#23262B"), 0.04, r);
    P.rect(0, 0, 24, 3, hx("#C9524E")); P.rect(0, 29, 24, 3, hx("#C9524E"));
    P.text("CHAMP", 2, 7, hx("#F2C744")); P.text("OR", 8, 14, hx("#FFFFFF")); P.text("BUST", 4, 21, hx("#F2C744"));
  }],
  posterDragon: [24, 32, (P, r) => {
    for (let y = 0; y < 32; y++) for (let x = 0; x < 24; x++) P.set(x, y, mixc(hx("#2C3E73"), hx("#7C4E9E"), y / 31));
    for (let i = 0; i < 14; i++) P.set(r() * 24, r() * 20, hx("#FFFFFF"));
    P.disc(12, 14, 4, hx("#F08A3C")); P.line(8, 12, 2, 6, hx("#F08A3C")); P.line(16, 12, 22, 6, hx("#F08A3C")); P.line(2, 6, 7, 14, hx("#F08A3C")); P.line(22, 6, 17, 14, hx("#F08A3C"));
    P.line(12, 18, 12, 26, hx("#F08A3C")); P.set(11, 13, hx("#FFFFFF")); P.set(13, 13, hx("#FFFFFF"));
  }],
  clock: [16, 16, (P, r) => {
    P.disc(7.5, 7.5, 7.4, hx("#7C5134")); P.disc(7.5, 7.5, 6, hx("#FBF6EA"));
    for (let t = 0; t < 12; t++) P.set(Math.round(7.5 + Math.cos(t / 12 * 6.283) * 5), Math.round(7.5 + Math.sin(t / 12 * 6.283) * 5), hx("#5A4A3E"));
    P.line(8, 8, 8, 4, C.ink); P.line(8, 8, 11, 8, C.ink); P.set(8, 8, C.red);
  }],
  photos: [16, 16, (P, r) => {
    P.fill(hx("#F4F0E6"), 0.02, r);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, mixc(hx("#A8D8F0"), hx("#7FC07A"), y > 9 ? 1 : 0));
    P.disc(6, 8, 2, C.skin); P.rect(5, 10, 3, 4, C.red); P.disc(11, 9, 1.6, C.skin); P.rect(10, 10, 3, 4, C.blue); P.rect(6, 5, 2, 1, hx("#5A3D27"));
  }],
  map: [32, 24, (P, r) => {
    P.fill(hx("#5BA3D9"), 0.04, r);
    for (let y = 0; y < 24; y++) for (let x = 0; x < 32; x++) { const land = Math.sin(x * 0.3) * 3 + Math.sin(y * 0.4 + 1) * 3 + (x < 22 ? 6 : -2) - Math.abs(y - 11) * 0.4; if (land > 1) P.set(x, y, shadec(hx("#6FBE5E"), land > 5 ? -0.12 : 0)); }
    P.line(5, 18, 5, 6, hx("#F4F0E6")); P.line(5, 6, 18, 6, hx("#F4F0E6")); P.line(18, 6, 18, 14, hx("#F4F0E6"));
    for (const [tx, ty] of [[5, 18], [5, 6], [18, 6], [18, 14]]) { P.rect(tx - 1, ty - 1, 3, 3, C.red); P.set(tx, ty, hx("#FFFFFF")); }
    P.rect(0, 0, 32, 1, hx("#E9E1CF")); P.rect(0, 23, 32, 1, hx("#E9E1CF")); P.rect(0, 0, 1, 24, hx("#E9E1CF")); P.rect(31, 0, 1, 24, hx("#E9E1CF"));
  }],
  sign: [24, 12, (P, r) => { P.bevel(0, 0, 24, 12, hx("#B98348"), 0.16, 0.06, r); P.rect(2, 2, 20, 1, hx("#D9A465")); }],
  dartboard: [16, 16, (P, r) => {
    P.disc(7.5, 7.5, 7.5, C.black);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy); if (d > 6.5) continue; const seg = Math.floor((Math.atan2(dy, dx) + Math.PI) / (Math.PI / 6)) % 2; P.set(x, y, d < 1.2 ? C.red : (d > 4 && d < 4.9) || d > 5.8 ? (seg ? C.red : C.green) : (seg ? hx("#F3E7CF") : C.black)); }
  }],
  bag: [16, 16, (P, r) => { P.fill(hx("#C9524E"), 0.08, r); P.rect(0, 4, 16, 1, hx("#F2C744")); P.rect(0, 11, 16, 1, hx("#F2C744")); }],
  ball: [16, 16, (P, r) => { P.rect(0, 0, 16, 8, hx("#E5525C")); P.rect(0, 8, 16, 8, hx("#F4F6F5")); P.rect(0, 7, 16, 2, C.black); P.rect(6, 6, 4, 4, C.black); P.rect(7, 7, 2, 2, hx("#FFFFFF")); P.set(3, 2, hx("#FF9AA0")); }],
  flower: [16, 16, (P, r) => {
    for (const [cx, cy, c] of [[3, 5, C.red], [11, 3, C.yellow], [7, 9, hx("#F4F6F5")], [13, 10, C.pink]]) {
      P.line(cx, cy + 2, cx + ((cx % 3) - 1), 15, C.greenD); P.set(cx + 1, cy + 5, C.green); P.set(cx - 1, cy + 6, C.green);
      for (const [dx, dy] of [[0, -1], [-1, 0], [1, 0], [0, 1], [-1, -1], [1, -1], [-1, 1], [1, 1]]) P.set(cx + dx, cy + dy, (dx && dy) ? shadec(c, -0.12) : shadec(c, 0.1));
      P.set(cx, cy, C.yellow); P.set(cx - 1, cy - 1, shadec(c, 0.3));
    }
  }],
  flowerTop: [16, 16, (P, r) => {
    P.fill(hx("#5FA24C"), 0.08, r);
    const cols = [C.red, C.yellow, hx("#F4F6F5"), C.pink];
    for (let i = 0; i < 6; i++) { const x = 1 + ((r() * 13) | 0), y = 1 + ((r() * 13) | 0), c = cols[i % 4]; P.set(x, y, c); P.set(x + 1, y, shadec(c, -0.1)); P.set(x, y + 1, shadec(c, -0.15)); P.set(x + 1, y + 1, shadec(c, -0.25)); P.set(x, y - 1 < 0 ? 0 : y - 1, shadec(c, 0.25)); }
    for (let i = 0; i < 8; i++) P.set(r() * 16, r() * 16, hx("#3F7F37"));
  }],
  tallGrass: [16, 16, (P, r) => { for (let x = 0; x < 16; x++) { const h = 6 + ((r() * 9) | 0); for (let y = 16 - h; y < 16; y++) P.set(x, y, shadec(hx("#4E9A48"), (y - 16 + h) * 0.01 + (x % 2 ? 0.06 : -0.04) + (y === 16 - h ? 0.18 : 0))); } }],
  mailbox: [16, 16, (P, r) => { P.bevel(0, 0, 16, 16, hx("#4C86C8"), 0.14, 0.04, r); P.rect(3, 6, 10, 1, hx("#23262B")); P.rect(13, 3, 1, 6, C.red); P.rect(13, 3, 3, 2, C.red); }],
  keyboard: [16, 16, (P, r) => { P.fill(hx("#DADDE2"), 0.03, r); for (let y = 2; y < 14; y += 3) for (let x = 1; x < 15; x += 2) P.set(x, y, hx("#8A9099")); }],
  console: [16, 16, (P, r) => { P.bevel(0, 0, 16, 16, hx("#B8BCC4"), 0.12, 0.02, r); P.rect(2, 3, 12, 3, hx("#7C828C")); P.disc(4, 11, 1.3, C.red); P.disc(8, 11, 1.3, hx("#8E6BC8")); P.rect(11, 10, 3, 1, hx("#7C828C")); }],
  lampShade: [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#FCE9B8"), (x % 4 === 0 ? -0.05 : 0) + (r() - 0.5) * 0.03)); }],
  bunting: [32, 8, (P, r) => { const cols = [C.red, C.yellow, C.blue, C.green]; for (let i = 0; i < 8; i++) for (let j = 0; j < 6; j++) for (let k = j >> 1; k < 4 - (j >> 1); k++) P.set(i * 4 + k, j + 1, cols[i % 4]); P.rect(0, 0, 32, 1, hx("#5A4A3E")); }],
  trophyPlate: [16, 8, (P, r) => { P.bevel(0, 0, 16, 8, hx("#3B2A1C"), 0.12, 0.03, r); P.rect(3, 3, 10, 2, C.goldL); }],
  glass: [16, 16, (P, r) => { P.fill(hx("#CFE9F2"), 0.02, r); P.line(3, 12, 12, 3, hx("#FFFFFF"), 0.7); }],
  lab: [16, 16, (P, r) => { P.fill(hx("#E9EDEC"), 0.03, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); }],
  shadowBlob: [32, 32, (P, r) => { for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) { const d = Math.hypot(x - 15.5, y - 15.5) / 16; P.set(x, y, [0, 0, 0], Math.max(0, 0.55 * (1 - d * d)) ); } }],
  sunPool: [32, 32, (P, r) => { for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) { const ex = Math.min(x, 31 - x) / 11, ey = Math.min(y, 31 - y) / 11; const a = Math.pow(Math.min(1, ex) * Math.min(1, ey), 1.4); const bar = (x === 15 || x === 16 || y === 15 || y === 16) ? 0.55 : 1; P.set(x, y, [255, 240, 200], 0.15 * a * bar); } }],
  aoN: [4, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 4; x++) P.set(x, y, [0, 0, 0], 0.34 * Math.pow(1 - y / 15, 2)); }],
  aoW: [16, 4, (P, r) => { for (let y = 0; y < 4; y++) for (let x = 0; x < 16; x++) P.set(x, y, [0, 0, 0], 0.3 * Math.pow(1 - x / 15, 2)); }],
  aoE: [16, 4, (P, r) => { for (let y = 0; y < 4; y++) for (let x = 0; x < 16; x++) P.set(x, y, [0, 0, 0], 0.3 * Math.pow(x / 15, 2)); }],
  deep: [16, 4, (P, r) => { for (let y = 0; y < 4; y++) for (let x = 0; x < 16; x++) P.set(x, y, [0, 0, 0], 0.92 * Math.pow(1 - x / 15, 1.3)); }],
  exitArrow: [16, 16, (P, r) => { for (let j = 0; j < 6; j++) for (let i = j; i < 11 - j; i++) P.set(i + 2, j + 5, [255, 255, 255], 0.85); }],
};
const texCache = {};
function pixTex(name) {
  if (texCache[name]) return texCache[name];
  const [w, h, draw] = TEXDEF[name];
  const P = new Pix(w, h);
  draw(P, rng(hashStr(name)));
  const t = new THREE.CanvasTexture(P.canvas());
  t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false;
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  return (texCache[name] = t);
}
// Screens that move: drawn again a few times a second.
function screenTex(w, h, draw) {
  const P = new Pix(w, h), cv = document.createElement("canvas"); cv.width = w; cv.height = h;
  const ctx = cv.getContext("2d"), t = new THREE.CanvasTexture(cv);
  t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false;
  let last = -1;
  return { tex: t, tick(now) { const f = Math.floor(now / 125); if (f === last) return; last = f; P.d.fill(0); draw(P, f); ctx.putImageData(new ImageData(P.d, w, h), 0, 0); t.needsUpdate = true; } };
}
// The film on Mum's TV: kids walking off down a railway line.
function drawFilm(P, f) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, mixc(hx("#F6B26B"), hx("#FDE3B0"), y / (P.h * 0.5)));
  const hy = 10;
  for (let y = hy; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, shadec(hx("#7DAA55"), (y - hy) * -0.02));
  for (let y = hy; y < P.h; y++) { const k = (y - hy) / (P.h - hy), half = 1 + k * 9, cx = P.w / 2; P.set(cx - half, y, hx("#6B6B6B")); P.set(cx + half, y, hx("#6B6B6B"));
    if (((y * 3 + f) % 4) === 0) for (let x = Math.round(cx - half); x <= cx + half; x++) P.set(x, y, hx("#8A5B36")); }
  for (let i = 0; i < 4; i++) { const x = P.w / 2 - 4 + i * 2.6, bob = (f + i) % 2; P.rect(Math.round(x), hy - 3 - bob, 1, 3, [[217, 82, 78], [76, 134, 200], [242, 199, 68], [93, 174, 82]][i]); P.set(Math.round(x), hy - 4 - bob, hx("#E9B48C")); }
  for (let i = 0; i < P.w; i += 7) P.set((i + f) % P.w, 2, hx("#FFFFFF"), 0.6);
}
// The racing game on the console upstairs.
function drawRace(P, f) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, y < 9 ? mixc(hx("#5FB7F0"), hx("#BFE6FA"), y / 9) : hx("#58A64A"));
  for (let y = 9; y < P.h; y++) { const k = (y - 9) / (P.h - 9), half = 2 + k * 13, cx = P.w / 2 + Math.sin(f * 0.15) * 3 * (1 - k);
    for (let x = Math.round(cx - half); x <= cx + half; x++) P.set(x, y, ((y + f) % 4 < 2) ? hx("#6B6E75") : hx("#5E6168"));
    P.set(cx - half, y, ((y + f) % 2) ? C.red : hx("#FFFFFF")); P.set(cx + half, y, ((y + f) % 2) ? C.red : hx("#FFFFFF")); }
  const cx = Math.round(P.w / 2); P.rect(cx - 3, P.h - 6, 7, 3, C.red); P.rect(cx - 2, P.h - 8, 5, 2, hx("#F4F6F5")); P.rect(cx - 3, P.h - 3, 2, 2, C.black); P.rect(cx + 2, P.h - 3, 2, 2, C.black);
  P.text("LAP 2", 2, 2, hx("#FFFFFF"));
}
// A PC on its desktop: a window open and a cursor blinking.
function drawPC(P, f) {
  P.fill(hx("#2E6DB4"));
  P.rect(3, 3, P.w - 6, P.h - 8, hx("#F4F6F5")); P.rect(3, 3, P.w - 6, 3, hx("#2F8C7A"));
  P.text("ITEMS", 5, 8, hx("#26304A")); P.text("POTION", 5, 14, hx("#26304A"));
  if (f % 8 < 4) P.rect(29, 14, 1, 5, hx("#26304A"));
  P.rect(0, P.h - 4, P.w, 4, hx("#1E4F86")); P.rect(1, P.h - 3, 5, 2, hx("#5DAE52"));
}
// The professor's machine: a heartbeat line running, a creature's outline and its numbers.
function drawLab(P, f) {
  P.fill(hx("#163A3F"));
  for (let x = 0; x < P.w; x++) { const k = (x + f) % 24, y = 6 + (k === 8 ? -4 : k === 9 ? 3 : k === 10 ? -1 : 0); P.set(x, y, hx("#58D3A0")); }
  P.rect(0, 11, P.w, 1, hx("#24575D"));
  P.text("DATA", 2, 14, hx("#BFF5DC")); P.rect(22, 14, 2 + (f % 6) * 2, 4, hx("#F2C12E"));
  if (f % 6 < 3) P.rect(P.w - 4, 2, 2, 2, hx("#E5525C"));
}
function drawLocked(P, f) {
  P.fill(hx("#23262B"));
  P.text("LOCKED", 8, 6, hx("#C9524E"));
  P.rect(8, 14, 24, 7, hx("#3A3F47")); for (let i = 0; i < 4; i++) P.rect(10 + i * 5, 16, 3, 3, hx("#F4F6F5"));
  if (f % 8 < 4) P.rect(30, 15, 1, 5, hx("#F4F6F5"));
}
