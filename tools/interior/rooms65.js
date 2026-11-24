// ---------- (0.61, John's 18:10 to 18:17) The new fit-out of the insides, shown here first for his yes ----------
// "everything needs to honour still the Minecraft like one block dimension ... within that one block might be a plant
// pot, a computer"; "textures we've used on the outside, and specifically how it reacts with the light"; "the wood
// floors ... it's just all got to be improved"; "update the ... interior viewer first, completely as it will appear
// in-game when you fit it out ... and then I'll say yes to which ones".
//
// FIT on (the viewer's "Fit-out: New"):
//  - surfaces drawn as the outside's are: one soft colour, a gentle light and shade drifting across several blocks
//    (as the grass and the paths have), a few small specks, the lines between boards and tiles soft, not drawn in ink;
//    each face still 16 pixels to a block. The wood floors redrawn as boards; the walls as soft paint; the kitchen's
//    hob, oven, sink and cupboards in flat colour.
//  - every piece of furniture on the block grid: a small thing (a plant, a chair, a bin) in the middle of its block,
//    a bigger one (a table, a sofa, a bookcase, a bed) filling whole blocks; the kitchen a block a unit.
//  - the light brighter and even, as Pallet's in the daytime, the shadows soft.
// FIT off: the insides as they are in the game now (0.60).
// FIT_GAME: the rooms John has said yes to; only these go into the game (export.js) with the new fit-out.
let FIT = true;
const FIT_GAME = new Set([]);

// A soft field that tiles (period w by h), smooth from cell to cell: -1..1.
function softField(seed, w, h, cx, cy) {
  const r = rng(seed), g = [];
  for (let j = 0; j < cy; j++) { g.push([]); for (let i = 0; i < cx; i++) g[j].push(r() * 2 - 1); }
  const out = new Float32Array(w * h), sm = t => t * t * (3 - 2 * t);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const fx = x / w * cx, fy = y / h * cy, i0 = Math.floor(fx), j0 = Math.floor(fy), tx = sm(fx - i0), ty = sm(fy - j0);
    const a = g[j0 % cy][i0 % cx], b = g[j0 % cy][(i0 + 1) % cx], c = g[(j0 + 1) % cy][i0 % cx], d = g[(j0 + 1) % cy][(i0 + 1) % cx];
    out[y * w + x] = (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * ty;
  }
  return out;
}
// Boards, as a floor of them would be laid: four to a block, running east and west, each board a length of one soft
// tone with a gentle light along it, its ends butted (a soft joint), a lit edge along its top and a soft shadow at
// its foot; a little grain; light and shade drifting over the whole floor. 64 pixels: four blocks before it repeats.
function fitBoards(P, r, base, o = {}) {
  // (Pass 2, 19:22: "if we're going to do things like laminate and that, it's just really tough") Planks a block at a
  // time, as Minecraft's: each block four boards across, every board its own soft tone, the joints where the block
  // ends and one more along each board (staggered board to board), the seams soft; a gentle drift of light over a
  // few blocks; no grain lines.
  const W = P.w, H = P.h, B = 16, field = softField(hashStr("fb" + base.join()), W, H, 4, 4);
  const tone = {}, at = k => tone[k] ?? (tone[k] = (r() - 0.5) * (o.tone ?? 0.09));
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const bx = Math.floor(x / B), by = Math.floor(y / B), row = Math.floor((y % B) / 4), v = y % 4, lx = x % B;
    const j = [10, 4, 13, 7][(row + bx * 3 + by * 2) % 4], seg = lx < j ? 0 : 1;
    let s = at(bx + "," + by + "," + row + "," + seg) + field[y * W + x] * 0.03;
    if (v === 0) s += 0.025; else if (v === 3) s -= 0.06;
    if (lx === 0 || lx === j) s -= 0.055; else if (lx === 1 || lx === j + 1) s += 0.015;
    P.set(x, y, shadec(base, s));
  }
  for (let i = 0; i < W * H / 60; i++) { const x = (r() * W) | 0, y = (r() * H) | 0; if ((y & 3) === 1 || (y & 3) === 2) P.set(x, y, shadec(P.get(x, y), (r() - 0.55) * 0.05)); }
}
// Paint on a wall, as the outside's walls: one colour, a soft drift of light over a few blocks, a few faint specks.
function fitPaint(P, r, base, o = {}) {
  const W = P.w, H = P.h, field = softField(hashStr("fp" + base.join()), W, H, 3, 3);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) P.set(x, y, shadec(base, field[y * W + x] * (o.drift ?? 0.022)));
  for (let i = 0; i < W * H / 70; i++) { const x = (r() * W) | 0, y = (r() * H) | 0; P.set(x, y, shadec(P.get(x, y), (r() - 0.5) * 0.05)); }
  if (o.stripes) for (let y = 0; y < H; y++) for (let x = 0; x < W; x += 8) P.set(x, y, shadec(P.get(x, y), o.stripes));
}
// Tiles as the outside's paving: each tile one soft colour, lit a little at its top and left, a soft joint.
function fitTiles(P, r, a, b, n = 2) {
  const W = P.w, H = P.h, t = W / (n * 2), field = softField(hashStr("ft" + a.join()), W, H, 4, 4);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const tx = Math.floor(x / t), ty = Math.floor(y / t), c = (tx + ty) % 2 ? b : a, u = x % t, v = y % t;
    let s = field[y * W + x] * 0.03;
    if (u === 0 || v === 0) s -= 0.07; else if (u === 1 || v === 1) s += 0.03;
    P.set(x, y, shadec(c, s));
  }
}
// The kitchen in flat colour (18:10: "the hob and stuff ... Even if that means it's more solid colours").
function fitHob(P) {
  const top = hx("#E9E7E1"), ring = hx("#8E9399"), ringIn = hx("#B4B8BC");
  P.rect(0, 0, 16, 16, top);
  for (const [cx, cy, big] of [[4.5, 4.5, 1], [11.5, 4.5, 0], [4.5, 11.5, 0], [11.5, 11.5, 1]]) {
    P.disc(cx, cy, big ? 3.1 : 2.6, ring); P.disc(cx, cy, big ? 1.9 : 1.5, ringIn); P.disc(cx, cy, 0.7, ring);
  }
  P.rect(0, 15, 16, 1, shadec(top, -0.06));
}
function fitOven(P) {
  const c = hx("#ECEBE6");
  P.rect(0, 0, 16, 16, c); P.rect(0, 0, 16, 1, shadec(c, 0.05)); P.rect(0, 15, 16, 1, shadec(c, -0.08));
  for (const kx of [3, 6, 10, 13]) P.disc(kx, 2, 0.9, hx("#7C8187"));
  P.rect(2, 5, 12, 1, hx("#A3A8AD"));
  P.rect(3, 7, 10, 7, hx("#3B4148")); P.rect(3, 7, 10, 1, hx("#55606A")); P.rect(4, 8, 3, 1, hx("#6E7A84"));
}
function fitSink(P) {
  const top = hx("#EEECE6");
  P.rect(0, 0, 16, 16, top);
  P.rect(2, 3, 12, 10, hx("#B9C2C8")); P.rect(3, 4, 10, 8, hx("#CCD4D9")); P.rect(3, 4, 10, 1, hx("#A7B1B8")); P.disc(8, 8, 0.9, hx("#8C979F"));
}
function fitCupboard(P, c, o = {}) {
  P.rect(0, 0, 16, 16, c);
  if (!o.door) { P.rect(0, 3, 16, 1, shadec(c, -0.1)); P.rect(6, 1, 4, 1, hx("#9AA1A6")); }
  const y0 = o.door ? 1 : 5;
  P.rect(1, y0, 14, 15 - y0 - 1, shadec(c, -0.03)); P.rect(1, y0, 14, 1, shadec(c, 0.05)); P.rect(1, 14, 14, 1, shadec(c, -0.08));
  P.rect(12, y0 + 3, 1, 3, hx("#9AA1A6"));
  P.rect(0, 15, 16, 1, shadec(c, -0.12));
}
function fitFridge(P) {
  const c = hx("#F2F3F1");
  P.rect(0, 0, 16, 48, c);
  P.rect(0, 15, 16, 1, hx("#C3C8C8")); P.rect(0, 47, 16, 1, shadec(c, -0.1));
  P.rect(12, 9, 1, 6, hx("#A3A9AE")); P.rect(12, 18, 1, 10, hx("#A3A9AE"));
  P.rect(3, 22, 6, 7, hx("#FFFFFF")); P.line(4, 27, 6, 24, C.blue); P.line(6, 24, 8, 27, C.blue); P.disc(7, 24, 1, C.yellow);
  P.rect(5, 21, 2, 1, C.red); P.disc(3, 33, 1, C.green); P.disc(9, 35, 1, C.orange); P.rect(4, 4, 3, 2, C.purple);
}
function fitCounter(P, r) { fitPaint(P, r, hx("#EEECE5"), { drift: 0.015 }); }

// A wall three blocks tall as one picture (so its rail and panelling come once, at the foot, not every block up: the
// stripes that read as planks): paint above, a rail, panelling below; soft, as the outside's.
function fitWall(P, r, top, o = {}) {
  const W = P.w, H = P.h, field = softField(hashStr("fw" + top.join()), W, H, 4, 3);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const h = (H - y) / 16; // blocks up from the floor
    let c = top, s = field[y * W + x] * 0.022;
    if (o.low && h < o.railY) { c = o.low; if (o.boards && x % 16 === 0) s -= 0.07; if (o.boards && x % 16 === 1) s += 0.03; }
    else if (o.rail && h < o.railY + 0.125) { c = o.rail; if (h > o.railY + 0.08) s += 0.06; }
    else if (o.stripes && x % 8 === 0) s -= o.stripes;
    P.set(x, y, shadec(c, s));
  }
  for (let i = 0; i < W * H / 80; i++) { const x = (r() * W) | 0, y = (r() * H) | 0; P.set(x, y, shadec(P.get(x, y), (r() - 0.5) * 0.04)); }
}
// Carpet, as the rugs: one soft colour, a soft drift, a fine pile of specks.
function fitCarpet(P, r, base, o = {}) {
  fitPaint(P, r, base, { drift: 0.03 });
  if (o.dots) for (let y = 4; y < P.h; y += 8) for (let x = (y % 16 === 4 ? 4 : 0); x < P.w; x += 8) P.rect(x, y, 2, 2, shadec(base, o.dots));
}
// Grass under a roof (ERIKA's GYM): as the outside's grass, soft blotches of a lighter green.
function fitLawn(P, r, base) {
  const W = P.w, H = P.h, field = softField(hashStr("fl" + base.join()), W, H, 6, 6);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) P.set(x, y, shadec(base, field[y * W + x] * 0.06));
  for (let i = 0; i < W * H / 50; i++) { const x = (r() * W) | 0, y = (r() * H) | 0; P.disc(x, y, 1.2, shadec(base, 0.08 + r() * 0.04)); }
}
// Bricks laid as a path (ERIKA's), as the outside's bricks: soft speckled faces, soft mortar.
function fitBricks(P, r, base, mortar) {
  const W = P.w, H = P.h, field = softField(hashStr("fbr" + base.join()), W, H, 4, 4);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const row = y >> 2, off = row % 2 ? 4 : 0, u = (x + off) % 8, v = y % 4;
    let c = base, s = field[y * W + x] * 0.04 + ((((x + off) >> 3) * 7 + row * 3) % 5 - 2) * 0.012;
    if (v === 3 || u === 7) { c = mortar; s *= 0.5; } else if (v === 0) s += 0.03;
    P.set(x, y, shadec(c, s));
  }
  for (let i = 0; i < W * H / 40; i++) { const x = (r() * W) | 0, y = (r() * H) | 0; if (y % 4 !== 3) P.set(x, y, shadec(P.get(x, y), (r() - 0.5) * 0.08)); }
}

// The new fit-out's pictures: [width, height, draw, blocks per picture (for a surface)].
const FIT_TEX = {
  // Wood floors: boards (four blocks to a picture).
  sqHoney: [64, 64, (P, r) => fitBoards(P, r, hx("#D2A86C")), 4], sqLight: [64, 64, (P, r) => fitBoards(P, r, hx("#DEBF8A")), 4],
  sqPale: [64, 64, (P, r) => fitBoards(P, r, hx("#E6D3B2")), 4], sqDark: [64, 64, (P, r) => fitBoards(P, r, hx("#9C7650")), 4],
  oakFloor: [64, 64, (P, r) => fitBoards(P, r, hx("#C69C66")), 4], honeyFloor: [64, 64, (P, r) => fitBoards(P, r, hx("#D9AE72")), 4],
  paleFloor: [64, 64, (P, r) => fitBoards(P, r, hx("#E4CFA8")), 4], walnutFloor: [64, 64, (P, r) => fitBoards(P, r, hx("#8E6640")), 4],
  oakDiagonal: [64, 64, (P, r) => fitBoards(P, r, hx("#E2C99C")), 4],
  // Walls: soft paint.
  mcCream: [64, 64, (P, r) => fitPaint(P, r, hx("#F2E6C8")), 4], mcSky: [64, 64, (P, r) => fitPaint(P, r, hx("#CFE6F2")), 4],
  mcSage: [64, 64, (P, r) => fitPaint(P, r, hx("#D5E3C6")), 4], mcLabWall: [64, 64, (P, r) => fitPaint(P, r, hx("#F1F2EE")), 4],
  paint: [64, 64, (P, r) => fitPaint(P, r, hx("#F4F3EF")), 4], mintPaint: [64, 64, (P, r) => fitPaint(P, r, hx("#A9DCC7")), 4],
  // Tiled floors: soft tiles.
  mcQuartzCheck: [64, 64, (P, r) => fitTiles(P, r, hx("#EEEAE2"), hx("#C6DDD2")), 4], mcLabTile: [64, 64, (P, r) => fitTiles(P, r, hx("#F0F1F0"), hx("#E1E6E5")), 4],
  mcBlueCheck: [64, 64, (P, r) => fitTiles(P, r, hx("#EEF2F4"), hx("#A9CCE4")), 4],
  counter: [64, 64, (P, r) => fitCounter(P, r), 4],
  // The kitchen.
  hob: [16, 16, P => fitHob(P)], oven: [16, 16, P => fitOven(P)], sink: [16, 16, P => fitSink(P)], fridge: [16, 48, P => fitFridge(P)],
  cupboard: [16, 16, P => fitCupboard(P, hx("#8CCDB7"))], cupboardWhite: [16, 16, P => fitCupboard(P, hx("#F1F2EF"))],
  wallCupboard: [16, 16, P => fitCupboard(P, hx("#8CCDB7"), { door: true })],
  // FireRed's rooms (rooms64.js): the walls as one picture three blocks tall, the floors soft.
  ssWall: [64, 48, (P, r) => fitWall(P, r, hx("#F3EEDF"), { low: hx("#C99A62"), rail: hx("#2F5FA8"), railY: 0.95, boards: true }), [4, 3]],
  vgWall: [64, 48, (P, r) => fitWall(P, r, hx("#EFE6D2"), { low: hx("#DC9A72"), rail: hx("#B87252"), railY: 0.9 }), [4, 3]],
  cgWall: [64, 48, (P, r) => fitWall(P, r, hx("#F4EEDC"), { low: hx("#A8CC88"), rail: hx("#7AA05A"), railY: 0.9 }), [4, 3]],
  cmPaper: [64, 48, (P, r) => fitWall(P, r, hx("#F0E8D0"), { low: hx("#C49466"), rail: hx("#9A6E44"), railY: 0.9, boards: true, stripes: 0.035 }), [4, 3]],
  scWall: [64, 48, (P, r) => fitWall(P, r, hx("#F4ECD4"), { low: hx("#8AB4D8"), rail: hx("#6A94B8"), railY: 0.9 }), [4, 3]],
  rhWall: [64, 48, (P, r) => fitWall(P, r, hx("#8A9C9C"), { low: hx("#667A7A"), rail: hx("#4E6060"), railY: 1.0 }), [4, 3]],
  lgWallHi: [64, 48, (P, r) => fitWall(P, r, hx("#F0E4C8")), [4, 3]],
  ssBoards: [64, 64, (P, r) => fitBoards(P, r, hx("#C99E66")), 4], cmBoards: [64, 64, (P, r) => fitBoards(P, r, hx("#D6AC74")), 4],
  scBoards: [64, 64, (P, r) => fitBoards(P, r, hx("#CBA466")), 4],
  ssCabin: [64, 64, (P, r) => fitCarpet(P, r, hx("#EDB46A"), { dots: -0.05 }), 4], ssGreen: [64, 64, (P, r) => fitCarpet(P, r, hx("#7CBC5C"), { dots: -0.05 }), 4],
  cmCarpetBlue: [64, 64, (P, r) => fitCarpet(P, r, hx("#5C7CC4"), { dots: 0.06 }), 4], cmCarpetRed: [64, 64, (P, r) => fitCarpet(P, r, hx("#C44C4C")), 4],
  fcCarpet: [64, 64, (P, r) => fitCarpet(P, r, hx("#EAA4B4"), { dots: 0.07 }), 4],
  vgFloor: [64, 64, (P, r) => fitTiles(P, r, hx("#96D4C8"), hx("#88CABE"), 4), 4], rhFloor: [64, 64, (P, r) => fitTiles(P, r, hx("#8E9672"), hx("#848C68"), 4), 4],
  ssKitchen: [64, 64, (P, r) => fitTiles(P, r, hx("#ECECEF"), hx("#CDD1D8"), 8), 4],
  // The POKéMON TOWER (18:02): calm checked tiles, pale lilac-grey stone, its gravestones, its arches, its stairs.
  twFloor: [64, 64, (P, r) => fitTiles(P, r, hx("#E8EADB"), hx("#AECDA9"), 2), 4],
  twWall: [64, 48, (P, r) => fitWall(P, r, hx("#DEDAE4"), { low: hx("#C2BBCC"), rail: hx("#A49CB4"), railY: 1.0 }), [4, 3]],
  cgLawn: [64, 64, (P, r) => fitLawn(P, r, hx("#7CC86A")), 4], cgBrick: [64, 64, (P, r) => fitBricks(P, r, hx("#D89A6A"), hx("#BE8458")), 4],
};
const FIT_KEEP = {};
// The fit-out's own new materials (only drawn with FIT on).
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("fitTread", 16, 16, (P, r) => { fitPaint(P, r, hx("#E6E0D2"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#F6F2EA")); P.rect(0, 15, 16, 1, hx("#BEB6A6")); });
  add("fitStepSide", 16, 16, (P, r) => { fitPaint(P, r, hx("#CFC7B8"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#E2DCCF")); });
  add("fitArchDark", 16, 32, P => {
    P.rect(0, 0, 16, 32, [0, 0, 0]); for (let y = 0; y < 32; y++) for (let x = 0; x < 16; x++) P.set(x, y, [0, 0, 0], 0);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 16; x++) { const inArch = y >= 6 || Math.hypot(x - 7.5, y - 7) < 7.5; if (inArch) P.set(x, y, x < 1 || x > 14 ? hx("#8C8496") : hx(y < 8 ? "#2A2434" : "#1E1A26")); }
  }, { fit: true, cut: true });
  add("fitTowerArch", 16, 32, P => {
    for (let y = 0; y < 32; y++) for (let x = 0; x < 16; x++) P.set(x, y, [0, 0, 0], 0);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 16; x++) {
      const d = Math.hypot(x - 7.5, y - 8), inner = y >= 8 ? x >= 3 && x <= 12 : d < 4.8, outer = y >= 8 ? x >= 1 && x <= 14 : d < 6.8;
      if (inner) P.set(x, y, hx(y > 28 ? "#3A3248" : "#4A4060")); else if (outer) P.set(x, y, hx(x < 3 || y < 4 ? "#ECE8F0" : "#C8C0D4"));
    }
    P.rect(7, 9, 2, 20, hx("#5A5070"));
  }, { fit: true, cut: true });
  add("fitGraveStone", 16, 16, (P, r) => fitPaint(P, r, hx("#8E8E9C"), { drift: 0.03 }));
  add("fitGraveTop", 16, 16, (P, r) => fitPaint(P, r, hx("#A8A8B6"), { drift: 0.02 }));
  add("fitGraveFace", 16, 16, (P, r) => {
    fitPaint(P, r, hx("#8E8E9C"), { drift: 0.03 });
    P.disc(7.5, 6.5, 3.2, hx("#B4B4C2")); P.disc(7.5, 6.5, 2.4, hx("#7A7A88")); P.rect(4, 6, 8, 1, hx("#B4B4C2")); P.disc(7.5, 6.5, 0.9, hx("#C8C8D4"));
    P.rect(4, 11, 8, 1, hx("#7A7A88")); P.rect(5, 13, 6, 1, hx("#7A7A88"));
  }, { fit: true });
  // Water as the outside's (Minecraft's own, as the game draws it): deep clear blue, soft ripples of light, seen into;
  // it moves (app.js slides it along).
  add("fitWater", 32, 32, (P, r) => {
    const field = softField(91, 32, 32, 4, 4), f2 = softField(17, 32, 32, 8, 8);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const v = field[y * 32 + x] * 0.6 + f2[y * 32 + x] * 0.4;
      P.set(x, y, v > 0.42 ? hx("#7FB0F0") : v > 0.2 ? hx("#4F86DC") : hx("#3A6CC8"));
    }
  }, { per: 2, alpha: 0.78 });
  add("fitDeskFront", 16, 16, (P, r) => { fitPaint(P, r, hx("#9C86B4"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#B8A4CC")); P.rect(0, 14, 16, 2, hx("#7A6694")); });
}
// Which pictures are laid over surfaces (a "per"), not stretched over one face.
function fitIsSurface(name) { for (const d of Object.values(MATDEF)) if (d && d.tex === name && d.per && !d.fit) return true; return false; }
// The rest of the surfaces, softened as the outside's are: each line and speck eased into its neighbours (a soft
// edge, not ink), the picture laid four times over (so it reads across four blocks, not one), a soft drift of light
// and shade across that, and a few faint specks.
function fitSoften(P, name) {
  const W = P.w, H = P.h, src = P.d.slice(), at = (x, y, c) => src[(((y + H) % H) * W + ((x + W) % W)) * 4 + c];
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = (y * W + x) * 4; if (!src[i + 3]) continue;
    for (let c = 0; c < 3; c++) {
      let m = 0; for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) m += at(x + dx, y + dy, c);
      P.d[i + c] = src[i + c] * 0.55 + (m / 9) * 0.45;
    }
  }
  const Q = new Pix(W * 4 <= 128 ? W * 4 : W, H * 4 <= 128 ? H * 4 : H), field = softField(hashStr("fs" + name), Q.w, Q.h, 4, 4), r = rng(hashStr(name) ^ 77);
  for (let y = 0; y < Q.h; y++) for (let x = 0; x < Q.w; x++) {
    const c = P.get(x % W, y % H), a = P.alpha(x % W, y % H);
    Q.set(x, y, shadec(c, field[y * Q.w + x] * 0.03), a);
  }
  for (let i = 0; i < Q.w * Q.h / 80; i++) { const x = (r() * Q.w) | 0, y = (r() * Q.h) | 0; if (Q.alpha(x, y)) Q.set(x, y, shadec(Q.get(x, y), (r() - 0.5) * 0.05), Q.alpha(x, y)); }
  return Q;
}
// The pictures, drawn as the fit-out has them when FIT is on.
pixTex = function (name) {
  const key = (FIT ? "fit:" : "") + name;
  if (texCache[key]) return texCache[key];
  let P;
  if (FIT && FIT_TEX[name]) { const [w, h, draw] = FIT_TEX[name]; P = new Pix(w, h); draw(P, rng(hashStr(name))); }
  else {
    const [w, h, draw] = TEXDEF[name];
    P = new Pix(w, h); draw(P, rng(hashStr(name)));
    if (!P._raw) crisp(P);
    if (FIT && !P._raw && fitIsSurface(name) && w <= 32 && h <= 32) P = fitSoften(P, name);
  }
  const t = new THREE.CanvasTexture(P.canvas());
  t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false;
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  return (texCache[key] = t);
};
// How many blocks a surface's picture covers, with the fit-out (a softened 16-pixel picture laid four times over is
// four blocks; one of the fit-out's own, as it says).
function fitPer(key, d) {
  if (!d || !d.per || d.fit || d.dyn) return d && d.per;
  const name = d.tex;
  if (FIT_TEX[name]) return FIT_TEX[name][3] || d.per;
  const t = TEXDEF[name];
  if (!t || t[0] > 32 || t[1] > 32) return d.per;
  const k = t[0] * 4 <= 128 ? 4 : 1;
  return Array.isArray(d.per) ? d.per.map(v => v * k) : d.per * k;
}

// Surfaces laid in blocks by the fit-out's own sizes.
{
  const fu = Geo.prototype.faceUV, fp = Geo.prototype.poly;
  Geo.prototype.faceUV = function (k, BL, R, U, w, h, sub) {
    const d = MATDEF[k] || {};
    if (!FIT || d.fit || !d.per) return fu.call(this, k, BL, R, U, w, h, sub);
    const q = fitPer(k, d), per = Array.isArray(q) ? q : [q, q];
    const u0 = BL.dot(R) / per[0], v0 = BL.dot(U) / per[1], u1 = u0 + w / per[0], v1 = v0 + h / per[1];
    return [u0, v0, u1, v0, u1, v1, u0, v1];
  };
  Geo.prototype.poly = function (k, pts) {
    const d = MATDEF[k] || {};
    if (!FIT || !k || d.fit || !d.per) return fp.call(this, k, pts);
    const n = new THREE.Vector3().subVectors(pts[1], pts[0]).cross(new THREE.Vector3().subVectors(pts[2], pts[0])).normalize();
    let U = Math.abs(n.y) < 0.95 ? V(0, 1, 0).addScaledVector(n, -n.y).normalize() : V(0, 0, n.y > 0 ? -1 : 1);
    const R = new THREE.Vector3().crossVectors(U, n);
    const q = fitPer(k, d), per = Array.isArray(q) ? q : [q, q];
    const b = this.buf(k), base = b.pos.length / 3;
    for (const p of pts) { b.pos.push(p.x, p.y, p.z); b.nor.push(n.x, n.y, n.z); b.uv.push(p.dot(R) / per[0], p.dot(U) / per[1]); }
    for (let i = 1; i + 1 < pts.length; i++) b.idx.push(base, base + i, base + i + 1);
  };
}
// Switches the fit-out on or off: the pictures and materials made again, the rooms built again.
function fitOn(on) {
  FIT = !!on;
  for (const k of Object.keys(matCache)) delete matCache[k];
}

// ---------- the block grid ----------
// A small thing in the middle of its block; a bigger one's edges on block edges.
const gridC = v => Math.floor(v) + 0.5;
const gridE = v => Math.round(v);
const gridW = v => Math.max(1, Math.round(v));
function gridRect(x0, z0, x1, z1) { let a = gridE(x0), b = gridE(z0), c = gridE(x1), d = gridE(z1); if (c <= a) c = a + 1; if (d <= b) d = b + 1; return [a, b, c, d]; }
function fitWrap(name, how) {
  const orig = globalThis[name];
  globalThis[name] = function (...a) { if (FIT && a[0] && a[0].geo) a = how(a); return orig.apply(this, a); };
}
// (centre x, z): plants, chairs, stools, bins, lamps and the like.
for (const n of ["plant", "chair", "stool", "bin", "palm", "slimPlant", "armchair", "floorLamp", "beanBag", "officeChair", "cushionSeat", "punchingBag", "flowerBush", "boxes", "incubator", "schoolDesk", "gymStatue"])
  if (typeof globalThis[n] === "function") fitWrap(n, a => { a[1] = gridC(a[1]); a[2] = gridC(a[2]); return a; });
// (x0, z0, x1, z1): tables and cases.
for (const n of ["table", "coffeeTable", "glassTable", "ballTable", "toyBox", "displayCase", "glassCase"])
  if (typeof globalThis[n] === "function") fitWrap(n, a => { const [x0, z0, x1, z1] = gridRect(a[1], a[2], a[3], a[4]); a[1] = x0; a[2] = z0; a[3] = x1; a[4] = z1; return a; });
// (X, Z, facing, width): things along a wall, their corner on a block's corner, their width whole blocks.
for (const n of ["sofa", "bookshelf", "sideboard", "martShelf", "cooler", "chestFreezer", "labShelf", "drawerUnit"])
  if (typeof globalThis[n] === "function") fitWrap(n, a => { a[1] = gridE(a[1]); a[2] = gridE(a[2]); if (typeof a[4] === "number") a[4] = gridW(a[4]); return a; });
for (const n of ["tvSet", "bed", "nightstand", "deskPC", "wardrobe", "trophyCabinet"])
  if (typeof globalThis[n] === "function") fitWrap(n, a => { a[1] = gridE(a[1]); a[2] = gridE(a[2]); return a; });

// The kitchen on the grid: the fridge a block, then five units of a block each.
{
  const kitchen0 = kitchen;
  kitchen = function (room, X, f, o = {}) {
    if (!FIT) return kitchen0(room, X, f, o);
    X = gridE(X); const z = gridE(o.z || 0);
    const F = room.F(X, z, f);
    F.box(0.02, 0, 0, 0.98, 2.7, 0.95, { front: "fridge", all: "paint", back: null, bottom: null });
    F.box(0.15, 2.7, 0.2, 0.75, 3.05, 0.7, { all: "honey" });
    F.solid(0, 0, 1, 1);
    room.act(F.rect(0, 0.5, 1, 1.4), { label: "Open", lines: ["The fridge is full. Lemonade, cheese, and a bowl of berries for your partner."] });
    counterUnit(room, F, 1, 1); counterUnit(room, F, 2, 1, "cupboard", "sink"); counterUnit(room, F, 3, 1); counterUnit(room, F, 4, 1, "oven", "hob"); counterUnit(room, F, 5, 1, "drawers");
    F.box(2.45, 1.0, 0.08, 2.55, 1.45, 0.18, "steel"); F.box(2.45, 1.38, 0.08, 2.55, 1.45, 0.48, "steel");
    F.box(4.25, 1.0, 0.25, 4.75, 1.3, 0.75, { all: "steel", top: "darkTop" });
    F.box(5.3, 1.0, 0.35, 5.7, 1.32, 0.65, { all: "silver" });
    F.box(3.2, 1.0, 0.3, 3.8, 1.04, 0.75, "honey");
    F.box(1, 1.0, 0, 6, 2, 0.03, { front: "splash", all: null });
    for (const i of [1, 5]) F.box(i, 2.2, 0, i + 1, 3.0, 0.6, { front: "wallCupboard", all: "mintPaint", back: null });
    if (f === "s") windowN(room, X + 2.5, 1.95, 2, 1.3, { plant: true, lines: ["Out of the kitchen window: the garden, and the lab's roof past the trees."] });
    F.box(4, 2.15, 0, 5, 2.4, 0.85, { all: "steel", back: null }); F.box(4.3, 2.4, 0, 4.7, H_TOP(room), 0.4, { all: "steel", back: null });
    room.act(F.rect(2, 0.6, 3, 1.5), { label: "Look", lines: ["The washing-up's all done. Mum keeps it tidy."] });
    room.act(F.rect(4, 0.6, 5, 1.5), { label: "Look", lines: ["Something's simmering. It smells like stew."] });
    room.item("Kitchen", "On the block grid: the fridge a block, then five units a block each (a cupboard, the sink, a cupboard, the hob over the oven, drawers), the tiles behind, cupboards and the hood above.");
    const [sx, sz] = F.pt(4.5, 0.5);
    room.steam = { x: sx, y: 1.32, z: sz };
  };
}

// People on the grid too: each in the middle of a block (as anyone stands in Minecraft).
function fitPeople(room) {
  if (!FIT) return;
  for (const n of room.npcs || []) { if (typeof n.x === "number") n.x = gridC(n.x); if (typeof n.z === "number") n.z = gridC(n.z); }
}

// ---------- (0.61, John's 17:58) Kanto's battle arenas by area, as the mod builds them (client/BattleScene) ----------
// "If I'm going to battle somebody on Route 17, there'll need to be a setup for that. Things like Route 1, Route 2 are
// pretty similar. Viridian Forest will need its own. Route 3 ... built up and rocky around ... Route 4 would be the
// same ... Route 24 and 25 ... the bridges ... Route 9 might be the same as Route 3 and Route 4. Same with Route 10 in
// some capacity, but with some water." Which routes get which (game/Battles.KANTO_ARENAS):
//   Routes 1, 2, 5 to 8, 11 to 15, 22: the meadow (Route battle); Viridian Forest: the forest; Routes 3, 4, 9: rocky;
//   Route 10: rocky by the river; Routes 24, 25: the bridge; Routes 16, 18: the Cycling Road; Route 17: the Cycling
//   Road by the sea; Routes 19 to 21: on the water; towns and cities: the town square; caves: the cave; gyms: the gym.
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("arTar", 16, 16, (P, r) => fitPaint(P, r, hx("#6E7278"), { drift: 0.03 }));
  add("arWhite", 16, 16, (P, r) => fitPaint(P, r, hx("#F2F2EE"), { drift: 0.01 }));
  add("arKerb", 16, 16, (P, r) => { fitPaint(P, r, hx("#B8B8B2"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#D2D2CC")); });
  add("arEarth", 32, 32, (P, r) => fitPaint(P, r, hx("#8E6B48"), { drift: 0.05 }), { per: 2 });
  add("arGrit", 16, 16, (P, r) => { fitPaint(P, r, hx("#8C8880"), { drift: 0.04 }); for (let i = 0; i < 14; i++) P.set(r() * 16, r() * 16, hx(i % 2 ? "#6C6860" : "#A8A49C")); });
  add("arRock", 16, 16, (P, r) => { fitPaint(P, r, hx("#9A968E"), { drift: 0.06 }); P.rect(0, 0, 16, 1, hx("#B6B2AA")); });
  add("arRock2", 16, 16, (P, r) => { fitPaint(P, r, hx("#84807A"), { drift: 0.06 }); P.rect(0, 0, 16, 1, hx("#A09C94")); });
  add("arDeck", 16, 16, (P, r) => fitBoards(P, r, hx("#8E6A44")));
  add("arDeckEdge", 16, 16, (P, r) => fitPaint(P, r, hx("#4E3826"), { drift: 0.03 }));
  add("arSand", 32, 32, (P, r) => fitPaint(P, r, hx("#E2CF96"), { drift: 0.04 }), { per: 2 });
  add("arWater", 32, 32, (P, r) => {
    const f = softField(55, 32, 32, 4, 4);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) P.set(x, y, f[y * 32 + x] > 0.35 ? hx("#6FA2E8") : f[y * 32 + x] > 0 ? hx("#4A80D4") : hx("#3C6CC4"));
  }, { per: 2 });

  const stage = (id, name, build) => {
    PLACES.push({ id, name, floor: "Battle arena (outside)", group: "Battle arenas" });
    BUILD[id] = function () {
      const R = new Room(id, { name, floor: "Battle arena (outside)", W: 40, D: 40, H: 8, outside: true });
      ground(R, "grass");
      const r = rng(hashStr(id)), cx = 20, cz = 20;
      const pitch = (dx, dz, i) => { const hx2 = 6 - i, z0 = -8 + i, z1 = 7 - i, rr = Math.max(0.5, 2.5 - i); if (Math.abs(dx) > hx2 || dz < z0 || dz > z1) return false;
        const ex = Math.max(0, Math.abs(dx) - (hx2 - rr)), ez = Math.max(0, Math.max(z0 + rr - dz, dz - (z1 - rr))); return Math.hypot(ex, ez) <= rr + 0.25; };
      const cam = [cx - 1.4, cz + 9.8];
      const camNear = (x, z, d) => Math.hypot(x - cam[0], z - cam[1]) < d;
      const top = (x, z, mat, h = 0) => R.geo.box(cx + x, -0.4, cz + z, cx + x + 1, h + 0.02, cz + z + 1, { top: mat, all: h > 0 ? mat : null, bottom: null });
      const block = (x, z, y, mat) => R.geo.box(cx + x, y, cz + z, cx + x + 1, y + 1, cz + z + 1, mat);
      const post = (x, z, h, mat) => R.geo.box(cx + x + 0.38, 0, cz + z + 0.38, cx + x + 0.62, h, cz + z + 0.62, mat);
      build(R, { r, cx, cz, pitch, camNear, top, block, post });
      R.spawn = { x: cx - 0.6, z: cz + 2.2, yaw: FACE.n };
      return R;
    };
  };
  const trees = (R, n, rad, s, o) => { for (let i = 0; i < n; i++) { const a = i / n * Math.PI * 2, x = o.cx + Math.sin(a) * rad, z = o.cz + Math.cos(a) * rad; if (!o.camNear(x, z, 6.5)) tree(R, x, z, s); } };
  const rocky = river => (R, o) => {
    for (let dx = -7; dx <= 7; dx++) for (let dz = -9; dz <= 8; dz++) {
      if (!o.pitch(dx, dz, 0)) continue;
      o.top(dx, dz, !o.pitch(dx, dz, 1) || dz === -1 ? "path" : o.r() < 0.14 ? "arGrit" : "arEarth");
    }
    for (let dx = -16; dx <= 16; dx++) for (let dz = -16; dz <= 16; dz++) {
      const d = Math.hypot(dx, dz);
      if (d < 9.5 || d > 15.5 || o.camNear(o.cx + dx, o.cz + dz, 6)) continue;
      if (river && dz <= -10 && dz >= -14) continue;
      const bump = Math.sin(dx * 0.55 + 1.3) + Math.cos(dz * 0.5) + Math.sin((dx + dz) * 0.3);
      if (bump < -0.2) continue;
      const h = 1 + Math.round(bump * 1.5) + (dz < -6 ? 2 : 0) + (o.r() < 0.5 ? 1 : 0);
      for (let i = 0; i < h; i++) o.block(dx, dz, i, o.r() < 0.5 ? "arRock" : "arRock2");
    }
    for (let i = 0; i < 8; i++) o.block((o.r() < 0.5 ? 1 : -1) * (8 + Math.floor(o.r() * 2)), Math.floor(o.r() * 14) - 9, 0, "arRock");
    if (river) for (let dx = -19; dx <= 19; dx++) for (let dz = -15; dz <= -9; dz++) {
      if (Math.hypot(dx, dz) > 19.5) continue;
      const bank = dz === -9 || dz === -15;
      o.top(dx, dz, bank ? "arSand" : "arWater");
      if (bank && o.r() < 0.25) R.geo.box(o.cx + dx + 0.3, 0, o.cz + dz + 0.3, o.cx + dx + 0.7, 1.8, o.cz + dz + 0.7, "stem");
    }
    trees(R, 24, 17.4, 1.4, o);
    R.item("The ground", river ? "Route 10: packed earth and grit, rocks all round, the river across the back (sand banks, reeds)." : "Routes 3, 4 and 9: packed earth and grit, a worn line round and across, outcrops of rock all round, higher at the back.");
  };
  stage("arena_rocky", "Rocky route battle (Routes 3, 4, 9)", rocky(false));
  stage("arena_river", "Riverside battle (Route 10)", rocky(true));
  stage("arena_bridge", "Bridge battle (Routes 24, 25)", (R, o) => {
    for (let dx = -19; dx <= 19; dx++) for (let dz = -19; dz <= 19; dz++) {
      if (Math.hypot(dx, dz) > 19.5) continue;
      const deck = Math.abs(dx) <= 7 && (Math.abs(dz) <= 9 || Math.abs(dx) <= 3);
      if (deck) o.top(dx, dz, Math.abs(dx) === 7 || (Math.abs(dx) === 3 && Math.abs(dz) > 9) ? "arDeckEdge" : "arDeck");
      else o.top(dx, dz, Math.abs(dx) >= 16 ? "grass" : "arWater");
    }
    for (let dz = -19; dz <= 19; dz++) for (const side of [-1, 1]) {
      const x = side * (Math.abs(dz) <= 9 ? 8 : 4);
      if (o.camNear(o.cx + x, o.cz + dz, 4.5)) continue;
      R.geo.box(o.cx + x, 0, o.cz + dz + 0.4, o.cx + x + 1, 0.9, o.cz + dz + 0.6, "arDeckEdge");
      if (((dz % 6) + 6) % 6 === 0) { o.post(x, dz, 2.0, "arDeckEdge"); R.geo.box(o.cx + x + 0.3, 2.0, o.cz + dz + 0.3, o.cx + x + 0.7, 2.5, o.cz + dz + 0.7, { all: "glass", top: "steel" }); }
    }
    R.item("The bridge", "Routes 24 and 25: the battle on the bridge's deck (boards across it, a dark board at each edge), rails with lanterns along both sides, the water all round, the bridge running on north and south.");
  });
  const cycling = sea => (R, o) => {
    for (let dx = -19; dx <= 19; dx++) for (let dz = -19; dz <= 19; dz++) {
      if (Math.hypot(dx, dz) > 19.5) continue;
      const ax = Math.abs(dx);
      let m = "grass";
      if (ax <= 7) m = ax === 6 || (ax === 0 && ((dz % 4) + 4) % 4 < 2) ? "arWhite" : "arTar";
      else if (ax === 8) m = "arKerb";
      else if (sea && dx < -11) m = dx < -13 ? "arWater" : "arSand";
      o.top(dx, dz, m);
    }
    for (let dz = -19; dz <= 19; dz++) for (const side of [-1, 1]) {
      const x = side * 9; if (o.camNear(o.cx + x, o.cz + dz, 4.5)) continue;
      o.post(x, dz, 1.0, "arWhite"); R.geo.box(o.cx + x, 0.75, o.cz + dz + 0.45, o.cx + x + 1, 0.9, o.cz + dz + 0.55, "arWhite");
    }
    if (!sea) trees(R, 20, 16, 1.3, o);
    else for (let dz = -16; dz <= 4; dz += 2) o.block(12, dz, 0, "treeLeaf");
    R.item("The road", sea ? "Route 17: the CYCLING ROAD by the sea: grey tarmac, white lines, kerbs and white rails, the sand and the sea along its far side." : "Routes 16 and 18: the CYCLING ROAD: grey tarmac with white lines down its edges and dashes down its middle, kerbs, white rails, grass verges, trees.");
  };
  stage("arena_cycling", "Cycling Road battle (Routes 16, 18)", cycling(false));
  stage("arena_cycling_sea", "Cycling Road by the sea (Route 17)", cycling(true));
}
