// ---------- 0.63 (John's 05:53: "All locations where a battle can happen, we need to have a setup") ----------
// A battle stage for every place: each gym, the ELITE FOUR's rooms and the CHAMPION's, the FIGHTING DOJO, the ROCKET
// HIDEOUT, the GAME CORNER, SILPH CO., the POKéMON TOWER, the MANSION, the POWER PLANT, the S.S. ANNE, SPROUT TOWER,
// each cave, both forests and the SAFARI ZONE. Each is a round hall 24 by 24 (as the LAB's and PEWTER GYM's), built of
// the place's own materials, the battle fought across its middle facing its back: yours just below the middle line,
// theirs above it, their trainer behind theirs; the camera behind yours, so its south side is kept clear. In the game
// the hall goes up over you for the battle and comes down after (BattleScene.setUpArena), exactly as here.
const ARENA_SERVES = {};
const ARENA_HALLS = [];
const AC = 12;
function arenaDef(key, name, serves, build) {
  const id = "arena_" + key;
  PLACES.push({ id, name: name + " battle", floor: "Battle arena", group: "Battle arenas", also: serves.map(x => x.replace(/_/g, " ")).join(" ") });
  ARENA_SERVES[key] = serves;
  ARENA_HALLS.push(id);
  BUILD[id] = function () {
    const R = new Room(id, { name: name + " battle", floor: "Battle arena", W: 24, D: 24, H: 8 });
    build(R);
    R.spawn = { x: 12, z: 14.2, yaw: FACE.n };
    R.item("Where it's used", "Every battle in " + serves.map(s => s.replace(/_/g, " ")).join(", ") + ".");
    return R;
  };
}
const aDist = (x, z) => Math.hypot(x + 0.5 - AC, z + 0.5 - AC);
// The floor: a block circle of the place's floor (skip: blocks left for something else, a pool, a pit).
function aFloor(R, top, rad = 11.5, skip = null) {
  for (let z = 0; z < R.D; z++) for (let x = 0; x < R.W; x++) {
    if (aDist(x, z) >= rad + 1 || (skip && skip(x, z))) continue;
    R.geo.box(x, -0.35, z, x + 1, 0, z + 1, { top: typeof top === "function" ? top(x, z) : top, all: null });
  }
}
// The wall: a ring of blocks round it, o.bands(post, a, x, z, h) its bands (a: the angle round, post: a post's block).
// Each band a whole block at a time with all its sides (in the game the walls' pictures are laid from where each block
// stands, so a block of wall is the same block wherever it is: the halls stay light).
function aRing(R, o) {
  const rad = o.rad || 11.5, H = o.h || 8;
  for (let z = 0; z < R.D; z++) for (let x = 0; x < R.W; x++) {
    const d = aDist(x, z);
    if (d < rad || d >= rad + 1) continue;
    const a = Math.atan2(z + 0.5 - AC, x + 0.5 - AC), fr = ((a / (Math.PI / 6)) % 1 + 1) % 1, post = o.posts !== false && (fr < 0.09 || fr > 0.91);
    const h = o.height ? o.height(x, z, a) : H, from = o.from ? o.from(x, z) : 0;
    for (const [y0, y1, mat] of o.bands(post, a, x, z, h)) {
      const a0 = Math.max(y0, from), a1 = Math.min(y1, h);
      if (a1 <= a0) continue;
      R.geo.box(x, a0, z, x + 1, a1, z + 1, { all: mat, top: a1 >= h ? (o.top || "cap") : null, bottom: null });
    }
  }
}
// The ceiling, a block at a time (so a picture fitted to a box, CELADON's panes, is one a block, and every block of
// it is the same block).
function aCeil(R, mat, h = 8) { for (let z = 0; z < R.D; z++) for (let x = 0; x < R.W; x++) R.geo.box(x, h, z, x + 1, h + 0.2, z + 1, { bottom: mat, all: null }); }
// The battlefield's lines: its edge (11 by 15), the line across its middle, the circle round its centre.
function aField(R, line, o = {}) {
  const G = R.geo, x0 = o.x0 ?? 6.5, x1 = o.x1 ?? 17.5, z0 = o.z0 ?? 4.5, z1 = o.z1 ?? 19.5, w = 0.14, y = 0.012, zm = o.zm ?? 12;
  for (const [a, b, c, d] of [[x0, z0, x1, z0 + w], [x0, z1 - w, x1, z1], [x0, z0, x0 + w, z1], [x1 - w, z0, x1, z1], [x0, zm - w / 2, x1, zm + w / 2]]) G.box(a, 0, b, c, y, d, { top: line, all: null });
  const r = o.r ?? 2.2;
  for (let i = 0; i < 24; i++) { const t = i / 24 * Math.PI * 2, cx = 12 + Math.cos(t) * r, cz = zm + Math.sin(t) * r; G.box(cx - 0.32, 0, cz - 0.32, cx + 0.32, y, cz + 0.32, { top: line, all: null }); }
  G.box(12 - (r - 0.18), 0, zm - (r - 0.18), 12 + (r - 0.18), y * 0.5, zm + (r - 0.18), { top: o.inner || (typeof o.floor === "string" ? o.floor : line), all: null });
}
// A post (a pillar), square, a cap on it.
function aPillar(R, x, z, h, mat, cap, w = 0.7) { R.geo.box(x - w / 2, 0, z - w / 2, x + w / 2, h, z + w / 2, { all: mat, top: cap || mat }); if (cap) R.geo.box(x - w / 2 - 0.08, h - 0.2, z - w / 2 - 0.08, x + w / 2 + 0.08, h, z + w / 2 + 0.08, cap); }
// Rocks: a rounded clump of the place's rock (rockDome's), as the caves' boulders are drawn.
function aRock(R, x, z, s, mats) {
  const r = rng(hashStr("aRock" + x.toFixed(2) + z.toFixed(2)));
  rockDome(R.geo, x, z, 0.7 * s, 1.1 * s, 6, 0, mats, [(r() - 0.5) * 0.3 * s, (r() - 0.5) * 0.3 * s]);
  for (let i = 0; i < 2; i++) { const a = r() * Math.PI * 2; rockDome(R.geo, x + Math.cos(a) * 0.6 * s, z + Math.sin(a) * 0.5 * s, 0.32 * s, 0.5 * s, 4, 0, mats); }
}
// The north half's spots for things round the edge (angles from the back, clear of the battle and the camera).
function aRound(n, rad, from = -160, to = -20) { const out = []; for (let i = 0; i < n; i++) { const t = (from + (to - from) * (n === 1 ? 0.5 : i / (n - 1))) * D2R; out.push([AC + Math.cos(t) * rad, AC + Math.sin(t) * rad, t]); } return out; }
// A cave's rock wall: rough heights, its own rock and its top, and a ceiling of rock.
function aCave(R, k) {
  // The rock in two rings: the inner a block high at a time, rough (5 to 8 high, a whole block each, so the game's
  // blocks match up); the outer full height behind it, drawn from the inner's top up.
  const hi = (x, z) => 5 + ((x * 7 + z * 13) % 4);
  aRing(R, { rad: 10.5, posts: false, top: k.top, height: hi, bands: (p, a, x, z, h) => [[0, h, k.wall]] });
  const innerTop = (x, z) => { let m = 0; for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const d = aDist(x + dx, z + dz); if (d >= 10.5 && d < 11.5) m = Math.max(m, hi(x + dx, z + dz)); } return m; };
  aRing(R, { rad: 11.5, posts: false, top: k.top, height: () => 8, from: innerTop, bands: () => [[0, 8, k.wall]] });
  aCeil(R, k.ceil || k.top);
}
const aRockMats = (low, mid, top) => ({ low, mid, top });

// ---------- new materials for the halls ----------
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("arLine", 8, 8, P => P.rect(0, 0, 8, 8, hx("#F6F4EC")), { fit: true });
  add("arLineGold", 8, 8, P => P.rect(0, 0, 8, 8, hx("#E8C24A")), { fit: true });
  add("arLineBlue", 8, 8, P => P.rect(0, 0, 8, 8, hx("#3A78C8")), { fit: true });
  add("arLineRed", 8, 8, P => P.rect(0, 0, 8, 8, hx("#D84A48")), { fit: true });
  add("arLineViolet", 8, 8, P => P.rect(0, 0, 8, 8, hx("#C08AF0")), { fit: true });
  add("arBasalt", 16, 16, (P, r) => { fitPaint(P, r, hx("#4A3E3E"), { drift: 0.05 }); for (let i = 0; i < 14; i++) P.set((r() * 16) | 0, (r() * 16) | 0, hx("#5E4E4A")); });
  add("arBasaltTop", 16, 16, (P, r) => { fitPaint(P, r, hx("#5A4A46"), { drift: 0.04 }); });
  add("arLava", 16, 16, (P, r) => { fitPaint(P, r, hx("#F2782A"), { drift: 0.06 }); for (let i = 0; i < 10; i++) P.disc(r() * 16, r() * 16, 1.2 + r(), hx("#FFC24A")); }, { glow: 0.9 });
  add("arEmber", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#FF9A3A")); P.rect(2, 2, 4, 4, hx("#FFE08A")); }, { glow: 1 });
  add("arMoonGlow", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#DCE6FF")); P.rect(2, 1, 4, 6, hx("#FFFFFF")); }, { glow: 0.8 });
  add("arCrystal", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#9A7CE8")); P.rect(2, 0, 3, 8, hx("#C8B4FF")); }, { glow: 0.6 });
  add("arIcicle", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#CFEAFA")); P.rect(3, 0, 2, 8, hx("#F4FCFF")); });
  add("arTatami", 16, 16, (P, r) => { fitPaint(P, r, hx("#C9C27E"), { drift: 0.03 }); P.rect(0, 0, 16, 1, hx("#3E6A3A")); P.rect(0, 15, 16, 1, hx("#3E6A3A")); for (let y = 2; y < 15; y += 2) P.rect(0, y, 16, 1, hx("#BDB674")); });
  add("arScroll", 8, 16, P => { P.rect(0, 0, 8, 16, hx("#F2EAD6")); P.rect(0, 0, 8, 1, hx("#6A4A2A")); P.rect(0, 15, 8, 1, hx("#6A4A2A")); for (let y = 3; y < 13; y += 3) P.rect(2, y, 4, 1, hx("#2A2A2A")); }, { fit: true });
  add("arLantern", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#F6E2B0")); P.rect(0, 0, 8, 1, hx("#B84A3A")); P.rect(0, 7, 8, 1, hx("#B84A3A")); }, { glow: 0.7 });
  add("arDrape", 16, 16, (P, r) => { fitPaint(P, r, hx("#6A3A8A"), { drift: 0.03 }); for (let x = 1; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#5A2E78")); });
  add("arDrapeRed", 16, 16, (P, r) => { fitPaint(P, r, hx("#B8343C"), { drift: 0.03 }); for (let x = 1; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#9A2830")); P.rect(0, 14, 16, 2, hx("#E8C24A")); });
  add("arRunner", 16, 16, (P, r) => { fitPaint(P, r, hx("#C23A3A"), { drift: 0.03 }); P.rect(0, 0, 2, 16, hx("#E8C24A")); P.rect(14, 0, 2, 16, hx("#E8C24A")); });
  add("arMarble", 16, 16, (P, r) => { fitPaint(P, r, hx("#ECEAF2"), { drift: 0.02 }); for (let i = 0; i < 4; i++) { let x = r() * 16, y = r() * 16; for (let k = 0; k < 10; k++) { P.set(x | 0, y | 0, hx("#D6D4E0")); x += r() * 2 - 0.6; y += r() * 2 - 1; } } });
  add("arDeck", 16, 16, (P, r) => fitBoards(P, r, hx("#C8915A")), { per: 1 });
  add("arRope", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#F4F4F0")); P.rect(0, 0, 4, 8, hx("#D84848")); }, { fit: true });
  add("arPoolEdge", 16, 16, (P, r) => { fitPaint(P, r, hx("#F2F4F6"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#3A78C8")); });
  add("arPit", 16, 16, P => { P.rect(0, 0, 16, 16, hx("#2A2018")); P.disc(8, 8, 6, hx("#140E0A")); }, { fit: true });
  add("arMound", 16, 16, (P, r) => fitPaint(P, r, hx("#8A6A44"), { drift: 0.05 }));
  add("arMoss", 16, 16, (P, r) => fitPaint(P, r, hx("#5E8A4A"), { drift: 0.05 }));
  add("arSnow", 16, 16, (P, r) => fitPaint(P, r, hx("#F2F6FA"), { drift: 0.02 }));
  add("arSafariGrass", 16, 16, (P, r) => { fitPaint(P, r, hx("#8CB85A"), { drift: 0.05 }); for (let i = 0; i < 12; i++) P.set((r() * 16) | 0, (r() * 16) | 0, hx("#A6CC6E")); });
  add("arSafariSand", 16, 16, (P, r) => fitPaint(P, r, hx("#E2CC8E"), { drift: 0.04 }));
  add("arForestFloor", 16, 16, (P, r) => { fitPaint(P, r, hx("#4E7A3A"), { drift: 0.06 }); for (let i = 0; i < 14; i++) P.set((r() * 16) | 0, (r() * 16) | 0, hx(r() < 0.5 ? "#3E6A30" : "#6A8A44")); });
  add("arCanopy", 16, 16, (P, r) => { fitPaint(P, r, hx("#1E3A22"), { drift: 0.05 }); for (let i = 0; i < 16; i++) P.set((r() * 16) | 0, (r() * 16) | 0, hx("#2E4E2E")); });
  add("arShrine", 16, 16, (P, r) => { fitPaint(P, r, hx("#B8442E"), { drift: 0.03 }); P.rect(0, 0, 16, 2, hx("#6A2A1E")); });
  add("arTrophy", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#E8C24A")); P.rect(2, 2, 4, 4, hx("#FFE68A")); });
  add("arBanner", 8, 16, (P, r) => { fitPaint(P, r, hx("#2F5FA8"), { drift: 0.02 }); P.rect(0, 13, 8, 3, hx("#E8C24A")); P.disc(4, 6, 2, hx("#E8C24A")); }, { fit: true });
  add("arCrate", 16, 16, (P, r) => { fitPaint(P, r, hx("#8A6A42"), { drift: 0.04 }); P.rect(0, 0, 16, 1, hx("#A6845A")); P.rect(0, 15, 16, 1, hx("#6A4E2E")); P.rect(7, 0, 2, 16, hx("#7A5A36")); });
  add("arPanel", 16, 16, (P, r) => { fitPaint(P, r, hx("#3A4250"), { drift: 0.02 }); P.rect(2, 3, 12, 5, hx("#5AE08A")); for (let i = 0; i < 4; i++) P.rect(2 + i * 3, 10, 2, 2, hx(["#F5D76E", "#E2665A", "#7CF5B0", "#8EC8F4"][i])); }, { fit: true, glow: 0.4 });
  add("arPipe", 16, 16, (P, r) => { fitPaint(P, r, hx("#7E8A96"), { drift: 0.02 }); P.rect(0, 0, 16, 3, hx("#A0ACB8")); P.rect(0, 13, 16, 3, hx("#5E6A76")); });
  add("arTable", 16, 16, (P, r) => fitPaint(P, r, hx("#F4F2EC"), { drift: 0.01 }));
  add("arChandelier", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#FFF2C0")); P.rect(3, 3, 2, 2, hx("#FFFFFF")); }, { glow: 0.9 });
}
const AW = (low, mid, top) => post => post ? [[0, 8, top]] : [[0, 1, low], [1, 8, mid]];

// ---------- the gyms ----------
arenaDef("viridian_gym", "Viridian Gym", ["viridian_gym"], R => {
  // GIOVANNI's: his gym's dark floor and walls, gold lines, two gold-capped pillars at the back either side of his dais.
  aFloor(R, "vgFloor"); aField(R, "arLineGold", { floor: "vgFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "vgRailFace"]] : [[0, 0.9, "vgRailFace"], [0.9, 7, "vgWall"], [7, 8, "vgRailFace"]] }); aCeil(R, "vgWall");
  R.geo.box(9, 0, 1.6, 15, 0.5, 3.6, { all: "vgRailFace", top: "vgMat" });
  for (const x of [7.5, 16.5]) aPillar(R, x, 2.6, 5.5, "vgWall", "gold");
  for (const [x, z] of [[3.6, 7], [20.4, 7]]) gymStatue(R, x, z, ["VIRIDIAN CITY POKéMON GYM", "LEADER: ?"]);
  for (const [x, z] of [[4.4, 3.8], [19.6, 3.8]]) plant(R, x, z, 1.2, { pot: "potWhite", kind: "leafy" });
});
arenaDef("cerulean_gym", "Cerulean Gym", ["cerulean_gym"], R => {
  // MISTY's: the battle on a white deck over the pool, the water all round it, lane ropes across, a diving board at
  // the back.
  const deck = (x, z) => x >= 6 && x < 18 && z >= 4 && z < 20;
  aFloor(R, "ceruleanTile", 11.5, (x, z) => !deck(x, z) && aDist(x, z) < 10.5);
  for (let z = 0; z < 24; z++) for (let x = 0; x < 24; x++) if (!deck(x, z) && aDist(x, z) < 10.5) { R.geo.box(x, -1.4, z, x + 1, -1.2, z + 1, { top: "ceruleanTile", all: null }); R.geo.box(x, -1.2, z, x + 1, -0.12, z + 1, { top: "fitWater", all: null }); }
  for (const [a, b, c, d] of [[6, 4, 18, 4.12], [6, 19.88, 18, 20], [6, 4, 6.12, 20], [17.88, 4, 18, 20]]) R.geo.box(a, -0.35, b, c, 0.02, d, { all: "arPoolEdge", top: "arPoolEdge" });
  aField(R, "arLineBlue", { floor: "ceruleanTile" });
  for (const z of [7, 11, 15]) for (const [x0, x1] of [[1.5, 6], [18, 22.5]]) R.geo.box(x0, -0.2, z - 0.05, x1, -0.1, z + 0.05, "arRope");
  aRing(R, { bands: (p) => p ? [[0, 8, "ceruleanTrim"]] : [[0, 1, "ceruleanTrim"], [1, 8, "ceruleanWall"]] }); aCeil(R, "ceruleanWall");
  R.geo.box(11, 0, 1.2, 13, 1.4, 2.2, { all: "steel" }); R.geo.box(11.3, 1.4, 1.2, 12.7, 1.5, 4.4, { all: "arPoolEdge" });
});
arenaDef("vermilion_gym", "Vermilion Gym", ["vermilion_gym"], R => {
  // LT. SURGE's: steel plate floor, hazard stripes round the field, generators and humming coils round the edge.
  aFloor(R, "surgeFloor"); aField(R, "surgeHazard", { floor: "surgeFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "surgeHazard"]] : [[0, 0.8, "surgeHazard"], [0.8, 8, "surgeWall"]] }); aCeil(R, "steel");
  for (const [x, z] of aRound(5, 9)) { R.geo.box(x - 0.9, 0, z - 0.5, x + 0.9, 2.2, z + 0.5, { all: "steel", s: "silphMachine", top: "surgeHazard" }); }
  for (const [x, z] of [[3.5, 12.5], [20.5, 12.5]]) { aPillar(R, x, z, 3.6, "surgeHazard", "steel", 0.5); R.geo.box(x - 0.3, 3.6, z - 0.3, x + 0.3, 4.2, z + 0.3, "arMoonGlow"); }
  for (const [x, z] of [[5, 16.5], [19, 16.5]]) { R.geo.box(x - 0.38, 0, z - 0.38, x + 0.38, 0.95, z + 0.38, { all: "trashCan", top: "trashLid" }); }
});
arenaDef("celadon_gym", "Celadon Gym", ["celadon_gym"], R => {
  // ERIKA's: a lawn under glass, the game's hedges round it, flower beds, trees at the back.
  aFloor(R, "cgLawn"); aField(R, "arLine", { floor: "cgLawn" });
  aRing(R, { bands: (p) => p ? [[0, 8, "walnut"]] : [[0, 1, "cgBrick"], [1, 8, "cgWall"]] }); aCeil(R, "cgPane");
  for (let z = 0; z < 24; z++) for (let x = 0; x < 24; x++) { const d = aDist(x, z); if (d >= 9.6 && d < 10.5 && z < 18) fitHedge(R.geo, x, z, 2, false, x * 7 + z); }
  for (const [x, z] of aRound(4, 7.8, -150, -30)) flowers(R, Math.floor(x) - 1, Math.floor(z), Math.floor(x) + 1, Math.floor(z) + 1);
  for (const [x, z] of [[6, 3], [18, 3], [12, 1.6]]) tree(R, Math.floor(x) + 0.5, Math.floor(z) + 0.5, 1.0);
});
arenaDef("fuchsia_gym", "Fuchsia Gym", ["fuchsia_gym"], R => {
  // KOGA's: a ninja's hall: boards, paper screens round it, tatami either side, red lanterns, scrolls at the back.
  aFloor(R, (x, z) => (x < 6 || x >= 18) && z > 3 && z < 18 ? "arTatami" : "gymBoards"); aField(R, "arLineViolet", { floor: "gymBoards" });
  aRing(R, { bands: (p) => p ? [[0, 8, "gymBoardsDark"]] : [[0, 0.8, "gymBoardsDark"], [0.8, 6.5, "shoji"], [6.5, 8, "gymBoardsDark"]] }); aCeil(R, "gymBoardsDark");
  for (const x of [9, 12, 15]) R.geo.box(x - 0.4, 2.0, 0.9, x + 0.4, 4.2, 0.95, { s: "arScroll", all: null });
  for (const [x, z] of aRound(4, 9.2)) { R.geo.box(x - 0.05, 0, z - 0.05, x + 0.05, 2.0, z + 0.05, "walnut"); R.geo.box(x - 0.3, 2.0, z - 0.3, x + 0.3, 2.7, z + 0.3, "arLantern"); }
  for (const [x, z] of [[4, 6], [20, 6]]) plant(R, x, z, 0.9, { pot: "potWhite", kind: "fern" });
});
arenaDef("saffron_gym", "Saffron Gym", ["saffron_gym"], R => {
  // SABRINA's: the gym's violet floor and walls, its glowing pads set round the edge, violet lines.
  aFloor(R, "psyFloor"); aField(R, "arLineViolet", { floor: "psyFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "psyCap"]] : [[0, 1, "psyBand"], [1, 8, "psyWall"]] }); aCeil(R, "psyWall");
  for (const [x, z] of aRound(6, 8.6, -170, -10)) { R.geo.box(x - 0.7, 0, z - 0.7, x + 0.7, 0.06, z + 0.7, { top: "psyPadRim", all: "psyCap" }); R.geo.box(x - 0.5, 0.06, z - 0.5, x + 0.5, 0.08, z + 0.5, { top: "psyPad", all: null }); }
  for (const x of [8, 16]) { aPillar(R, x, 2.4, 6, "psyWall", "psyCap"); R.geo.box(x - 0.2, 6, 2.2, x + 0.2, 6.6, 2.6, "arCrystal"); }
});
arenaDef("cinnabar_gym", "Cinnabar Gym", ["cinnabar_gym"], R => {
  // BLAINE's: a volcano's floor: dark basalt, pools of lava at the edge, ember-lit rocks.
  const lava = (x, z) => { const d = aDist(x, z); return d > 8.6 && d < 10.4 && z < 17 && ((x * 3 + z * 5) % 7) < 4; };
  aFloor(R, "arBasaltTop", 11.5, lava);
  for (let z = 0; z < 24; z++) for (let x = 0; x < 24; x++) if (lava(x, z) && aDist(x, z) < 11.5) R.geo.box(x, -0.5, z, x + 1, -0.2, z + 1, { top: "arLava", all: "arBasalt" });
  aField(R, "arLineRed", { floor: "arBasaltTop" });
  aRing(R, { posts: false, height: (x, z) => 6 + ((x * 5 + z * 3) % 4) * 0.5, top: "arBasaltTop", bands: (p, a, x, z, h) => [[0, h, "arBasalt"]] }); aCeil(R, "arBasalt");
  for (const [x, z] of aRound(5, 9.6)) aRock(R, x, z, 1.1, aRockMats("arBasalt", "arBasalt", "arBasaltTop"));
  for (const [x, z] of [[4, 9], [20, 9]]) { R.geo.box(x - 0.4, 0, z - 0.4, x + 0.4, 1.0, z + 0.4, "arBasalt"); R.geo.box(x - 0.3, 1.0, z - 0.3, x + 0.3, 1.3, z + 0.3, "arEmber"); }
});

// ---------- the ELITE FOUR and the CHAMPION ----------
const E4 = (floor, low, pillar, extra) => R => {
  aFloor(R, floor); aField(R, "arLine", { floor });
  aRing(R, { bands: (p) => p ? [[0, 8, low]] : [[0, 1, low], [1, 8, "lgWallHi"]] }); aCeil(R, "lgWallHi");
  for (const [x, z] of aRound(4, 9.4, -165, -15)) aPillar(R, x, z, 6, pillar, "gold", 0.9);
  extra(R);
};
arenaDef("loreleis_room", "Lorelei's room", ["loreleis_room"], E4("lgIce", "lgWall", "lgIcePillar", R => {
  // Ice: snow drifts at the edge, icicles hanging from the ceiling, ice blocks at the back.
  for (const [x, z] of aRound(6, 8.2, -170, -10)) R.geo.box(x - 0.9, 0, z - 0.6, x + 0.9, 0.3, z + 0.6, { all: "arSnow" });
  for (const [x, z] of aRound(9, 10.2, -175, -5)) R.geo.box(x - 0.12, 6.4, z - 0.12, x + 0.12, 8, z + 0.12, "arIcicle");
  for (const x of [10, 14]) R.geo.box(x - 0.6, 0, 1.6, x + 0.6, 1.6, 2.6, "lgIcePillar");
}));
arenaDef("brunos_room", "Bruno's room", ["brunos_room"], E4("lgSand", "lgWall", "lgRock", R => {
  // Fighting: boulders, punching bags, weights at the edge.
  for (const [x, z] of aRound(3, 9.6, -150, -30)) aRock(R, x, z, 1.0, aRockMats("gymTanLow", "gymTan", "gymTanTop"));
  for (const [x, z] of [[4.2, 7.4], [19.8, 7.4]]) punchingBag(R, x, z);
}));
arenaDef("agathas_room", "Agatha's room", ["agathas_room"], E4("lgViolet", "lgWall", "lgGilt", R => {
  // Ghost: violet drapes on the wall, gravestones and candles at the edge.
  for (const [x, z] of aRound(5, 10.6, -165, -15)) R.geo.box(x - 0.6, 1.2, z - 0.6, x + 0.6, 7, z + 0.6, { all: "arDrape" });
  let i = 0;
  for (const [x, z] of aRound(6, 8.0, -170, -10)) fitGrave(R, R.geo, Math.floor(x), Math.floor(z), "agatha" + i++);
}));
arenaDef("lances_room", "Lance's room", ["lances_room"], E4("lgTeal", "lgWall", "lgHornBase", R => {
  // Dragon: braziers burning at the edge, banners on the wall.
  for (const [x, z] of aRound(4, 8.2, -160, -20)) { R.geo.box(x - 0.4, 0, z - 0.4, x + 0.4, 1.1, z + 0.4, { all: "steel", top: "steel" }); R.geo.box(x - 0.3, 1.1, z - 0.3, x + 0.3, 1.5, z + 0.3, "arEmber"); }
  for (const [x, z] of aRound(5, 10.6, -165, -15)) R.geo.box(x - 0.45, 2.0, z - 0.45, x + 0.45, 5.0, z + 0.45, { all: "arBanner" });
}));
arenaDef("champions_room", "Champion's room", ["champions_room"], R => {
  // The CHAMPION's: white marble, a red runner up the middle to the back, red and gold drapes, trophies.
  aFloor(R, "arMarble"); R.geo.box(11, 0, 0.5, 13, 0.012, 23, { top: "arRunner", all: null }); aField(R, "arLineGold", { floor: "arMarble", inner: "arRunner" });
  aRing(R, { bands: (p) => p ? [[0, 8, "gold"]] : [[0, 1, "lgWall"], [1, 8, "lgWallHi"]] }); aCeil(R, "lgWallHi");
  for (const [x, z] of aRound(4, 9.4, -165, -15)) aPillar(R, x, z, 6.5, "arMarble", "gold", 0.9);
  for (const [x, z] of aRound(5, 10.6, -165, -15)) R.geo.box(x - 0.5, 1.5, z - 0.5, x + 0.5, 7.2, z + 0.5, { all: "arDrapeRed" });
  for (const x of [8.5, 15.5]) { R.geo.box(x - 0.5, 0, 2.2, x + 0.5, 1.0, 3.2, { all: "walnut" }); R.geo.box(x - 0.2, 1.0, 2.5, x + 0.2, 1.6, 2.9, "arTrophy"); }
});

// ---------- the other places ----------
arenaDef("fighting_dojo", "Fighting Dojo", ["fighting_dojo"], R => {
  aFloor(R, (x, z) => Math.abs(x + 0.5 - AC) < 6 && z > 3 && z < 20 ? "arTatami" : "gymBoards"); aField(R, "arLineRed", { floor: "arTatami" });
  aRing(R, { bands: (p) => p ? [[0, 8, "gymBoardsDark"]] : [[0, 0.8, "gymBoardsDark"], [0.8, 6.5, "shoji"], [6.5, 8, "gymBoardsDark"]] }); aCeil(R, "gymBoardsDark");
  for (const x of [7, 10, 14, 17]) R.geo.box(x - 0.4, 2.0, 1.0, x + 0.4, 4.4, 1.05, { s: "arScroll", all: null });
  for (const [x, z] of [[4.4, 6.8], [19.6, 6.8]]) punchingBag(R, x, z);
  R.geo.box(9.5, 0, 1.4, 14.5, 0.4, 3.0, { all: "gymBoardsDark", top: "arTatami" });
});
arenaDef("rocket_hideout", "Rocket Hideout", ["rocket_hideout", "game_corner"], R => {
  // The hideout's: its dark floor and walls, crates stacked round, machines with their green screens, pipes overhead.
  aFloor(R, "rhFloor"); aField(R, "arLineRed", { floor: "rhFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "rhCap"]] : [[0, 1, "rhCap"], [1, 8, "rhWall"]] }); aCeil(R, "rhWall");
  for (const [x, z] of aRound(4, 9.2, -165, -100).concat(aRound(4, 9.2, -80, -15))) { R.geo.box(x - 0.5, 0, z - 0.5, x + 0.5, 1, z + 0.5, "arCrate"); if ((x | 0) % 2) R.geo.box(x - 0.4, 1, z - 0.4, x + 0.4, 1.8, z + 0.4, "arCrate"); }
  for (const x of [9, 12, 15]) R.geo.box(x - 0.8, 0, 1.2, x + 0.8, 2.0, 2.0, { all: "steel", s: "arPanel", top: "steel" });
  for (const z of [4, 9]) R.geo.box(1, 6.6, z, 23, 7.0, z + 0.4, "arPipe");
});
arenaDef("silph_co", "Silph Co.", ["silph_co"], R => {
  // SILPH CO.'s offices: its floor and walls, desks with their screens, plants, windows over the city.
  aFloor(R, "silphFloor"); aField(R, "arLineBlue", { floor: "silphFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "silphCap"]] : [[0, 1, "silphSkirt"], [1, 3.2, "silphWall"], [3.2, 5.6, "window"], [5.6, 8, "silphWall"]] }); aCeil(R, "silphWall");
  for (const [x, z] of [[5, 5], [19, 5], [4, 10], [20, 10]]) { R.geo.box(x - 0.9, 0, z - 0.45, x + 0.9, 0.8, z + 0.45, { all: "silphDeskSide", top: "silphDesk" }); R.geo.box(x - 0.35, 0.8, z - 0.3, x + 0.35, 1.3, z - 0.2, { s: "silphScreen", all: "greyPlastic" }); }
  for (const [x, z] of [[8, 2.2], [16, 2.2]]) plant(R, x, z, 1.2, { pot: "potWhite", kind: "leafy" });
  R.geo.box(10, 0, 1.0, 14, 2.6, 1.3, { all: "silphLiftWall", s: "silphLiftDoor" });
});
arenaDef("pokemon_tower", "Pokémon Tower", ["pokemon_tower"], R => {
  // The TOWER's: its floor and walls, the gravestones round the edge (as its floors'), candles.
  aFloor(R, "twFloor"); aField(R, "arLineViolet", { floor: "twFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "twWall"]] : [[0, 8, "twWall"]] }); aCeil(R, "twWall");
  let i = 0;
  for (const [x, z] of aRound(9, 9.2, -172, -8)) fitGrave(R, R.geo, Math.floor(x), Math.floor(z), "tower" + i++);
  for (const [x, z] of aRound(4, 7.4, -150, -30)) fitGrave(R, R.geo, Math.floor(x), Math.floor(z), "towerB" + i++);
});
arenaDef("pokemon_mansion", "Pokémon Mansion", ["pokemon_mansion"], R => {
  // The burned MANSION: its checkered floor broken by rubble, scorched brick walls, pillars, a toppled table, a statue.
  aFloor(R, (x, z) => ((x * 7 + z * 11) % 9) === 0 && aDist(x, z) > 7 ? "manRubble" : "manChecker"); aField(R, "arLine", { floor: "manChecker" });
  aRing(R, { bands: (p) => p ? [[0, 8, "manPillar"]] : [[0, 1, "manCap"], [1, 8, "manBrick"]] }); aCeil(R, "manBrick");
  for (const [x, z] of aRound(4, 9.2, -160, -20)) aPillar(R, x, z, 5.5, "manPillar", "manCap", 0.8);
  R.geo.box(4, 0, 8, 6, 0.4, 9, { all: "manTable" }); R.geo.box(18.4, 0, 6, 19.6, 0.9, 7.4, { all: "manTable" });
  gymStatue(R, 12, 2.2, ["A statue of a POKéMON. Its eyes have been pried out."]);
  for (const [x, z] of [[7, 3], [17, 4], [5, 13], [19, 13]]) R.geo.box(x - 0.4, 0, z - 0.3, x + 0.4, 0.25, z + 0.3, { all: "manRock" });
});
arenaDef("power_plant", "Power Plant", ["power_plant"], R => {
  // The POWER PLANT's: its floor and walls, the hazard stripe, generators and coils round the edge, cables.
  aFloor(R, "ppFloor"); aField(R, "ppStripe", { floor: "ppFloor" });
  aRing(R, { bands: (p) => p ? [[0, 8, "ppStripe"]] : [[0, 1, "ppStripe"], [1, 8, "ppWall"]] }); aCeil(R, "ppWall");
  for (const [x, z] of aRound(5, 9.0)) R.geo.box(x - 0.8, 0, z - 0.6, x + 0.8, 2.4, z + 0.6, { all: "ppMachine", top: "ppMachineTop", s: "ppPanel" });
  for (const [x, z] of [[3.6, 12.6], [20.4, 12.6]]) { R.geo.box(x - 0.45, 0, z - 0.45, x + 0.45, 3.2, z + 0.45, { all: "ppCoil" }); R.geo.box(x - 0.3, 3.2, z - 0.3, x + 0.3, 3.6, z + 0.3, "arMoonGlow"); }
  for (const z of [3, 7]) R.geo.box(1, 6.8, z, 23, 7.1, z + 0.3, "arPipe");
});
arenaDef("ss_anne", "S.S. Anne", ["ss_anne"], R => {
  // The S.S. ANNE's: its dining hall: boards, the ship's walls with portholes, round white tables, chandeliers.
  aFloor(R, "ssBoards"); aField(R, "arLineGold", { floor: "ssBoards" });
  aRing(R, { bands: (p, a) => p ? [[0, 8, "walnut"]] : [[0, 1, "walnut"], [1, 8, "ssWall"]] }); aCeil(R, "ssWall");
  for (const [x, z] of aRound(5, 10.9, -165, -15)) R.geo.box(x - 0.4, 2.2, z - 0.4, x + 0.4, 3.0, z + 0.4, { all: "ssPort" });
  for (const [x, z] of [[4.6, 6.4], [19.4, 6.4], [7.6, 2.8], [16.4, 2.8]]) { R.geo.box(x - 0.08, 0, z - 0.08, x + 0.08, 0.75, z + 0.08, "steel"); R.geo.box(x - 0.7, 0.75, z - 0.7, x + 0.7, 0.85, z + 0.7, { all: "arTable", top: "arTable" }); R.geo.box(x - 0.15, 0.85, z - 0.15, x + 0.15, 0.95, z + 0.15, "kPlate"); }
  for (const [x, z] of [[8, 8], [16, 8]]) { R.geo.box(x - 0.03, 6.6, z - 0.03, x + 0.03, 8, z + 0.03, "gold"); R.geo.box(x - 0.6, 6.2, z - 0.6, x + 0.6, 6.6, z + 0.6, "arChandelier"); }
});
arenaDef("sprout_tower", "Sprout Tower", ["sprout_tower"], R => {
  // SPROUT TOWER's: its boards and paper walls, its pillars, the great swaying pillar at the back, a painting.
  aFloor(R, "stBoards"); aField(R, "arLine", { floor: "stBoards" });
  aRing(R, { bands: (p) => p ? [[0, 8, "stPillar"]] : [[0, 0.8, "stDark"], [0.8, 8, "stPaper"]] }); aCeil(R, "stDark");
  R.geo.box(10.6, 0, 0.8, 13.4, 8, 3.6, { all: "stLog", top: "stLogEnd" });
  for (const [x, z] of aRound(4, 9.0, -160, -20)) aPillar(R, x, z, 8, "stPillar", null, 0.8);
  R.geo.box(5.5, 2.4, 2.4, 7.5, 3.8, 2.45, { s: "stPainting", all: null });
});

// ---------- the caves, the forests and the SAFARI ZONE ----------
const caveArena = (key, name, serves, k, extra) => arenaDef(key, name, serves, R => {
  // (No lines on a cave's floor: a battle there is where you met, as Let's Go's are.)
  aFloor(R, k.floor);
  aCave(R, k);
  for (const [x, z] of aRound(6, 9.2, -170, -10)) aRock(R, x, z, 1.1, aRockMats(k.wall, k.wall, k.top));
  if (extra) extra(R, k);
});
caveArena("mt_moon", "Mt. Moon", ["mt_moon"], { floor: "area_moon_floor", wall: "area_moon_rock", top: "area_moon_rock_top" }, R => {
  // Gravel patches, and moon stones glowing among the rocks.
  for (const [x, z] of [[5, 15], [18, 15], [7, 4], [17, 4]]) patch(R, x, z, x + 2, z + 2, "area_moon_gravel");
  for (const [x, z] of aRound(4, 8.2, -150, -30)) R.geo.box(x - 0.15, 0, z - 0.15, x + 0.15, 0.4, z + 0.15, "arMoonGlow");
});
caveArena("rock_tunnel", "Rock Tunnel", ["rock_tunnel"], { floor: "area_rt_floor", wall: "area_rt_rock", top: "area_rt_rock_top" }, R => {
  for (const [x, z] of [[4.5, 8], [19.5, 8]]) { R.geo.box(x - 0.06, 0, z - 0.06, x + 0.06, 1.8, z + 0.06, "walnut"); R.geo.box(x - 0.2, 1.8, z - 0.2, x + 0.2, 2.2, z + 0.2, "arLantern"); }
});
caveArena("digletts_cave", "Diglett's Cave", ["digletts"], { floor: "area_cave_floor", wall: "area_cave_rock", top: "area_cave_rock_top" }, R => {
  // Mounds of earth, each with its hole, round the edge.
  for (const [x, z] of aRound(5, 7.6, -160, -20)) { rockDome(R.geo, x, z, 0.8, 0.5, 6, 0, aRockMats("arMound", "arMound", "arMound")); R.geo.box(x - 0.22, 0.46, z - 0.22, x + 0.22, 0.5, z + 0.22, { top: "arPit", all: null }); }
});
caveArena("seafoam_islands", "Seafoam Islands", ["seafoam"], { floor: "area_sf_floor", wall: "area_sf_ice", top: "area_sf_ice_top" }, R => {
  // Its sand underfoot at the sides, ice shards, a pool of the sea at either side.
  for (const [x, z] of [[2, 9], [19, 9]]) { R.geo.box(x, -0.35, z, x + 3, -0.3, z + 4, { top: "area_sf_sand", all: null }); }
  for (const [x, z] of aRound(4, 8.0, -150, -30)) R.geo.box(x - 0.3, 0, z - 0.3, x + 0.3, 1.4, z + 0.3, "area_ice_shard");
});
caveArena("victory_road", "Victory Road", ["victory_road"], { floor: "area_vr_floor", wall: "area_vr_cliff", top: "area_vr_top" }, R => {
  // Its great boulders (STRENGTH's) and the stone switch.
  for (const [x, z] of [[4.5, 7.5], [19.5, 7.5]]) aRock(R, x, z, 1.4, aRockMats("area_vr_boulder", "area_vr_boulder", "area_vr_boulder"));
  R.geo.box(17.1, 0, 15.1, 17.9, 0.06, 15.9, { all: "steel", top: "steel" });
});
caveArena("cerulean_cave", "Cerulean Cave", ["cerulean_cave"], { floor: "area_cc_floor", wall: "area_cc_cliff", top: "area_cc_moss_top" }, R => {
  // Its mossy tops, and crystals glowing violet in the rock.
  for (const [x, z] of aRound(6, 8.4, -170, -10)) R.geo.box(x - 0.15, 0, z - 0.1, x + 0.15, 0.9, z + 0.1, "arCrystal");
});
caveArena("silver_cave", "Mt. Silver", ["silver_cave", "mt_silver"], { floor: "area_ms_floor", wall: "area_ms_cliff", top: "area_ms_top" }, R => {
  // Snow lying at the edge.
  for (const [x, z] of aRound(6, 8.2, -170, -10)) R.geo.box(x - 0.8, 0, z - 0.6, x + 0.8, 0.2, z + 0.6, "arSnow");
});
const forestArena = (key, name, serves, extra) => arenaDef(key, name, serves, R => {
  // A clearing in the trees: the forest floor, a ring of trees two deep, the canopy closed over it.
  aFloor(R, "arForestFloor");
  // (Each tree on a block, all one size: in the game they share their blocks.)
  const T = (x, z) => tree(R, Math.floor(x) + 0.5, Math.floor(z) + 0.5, 1.0);
  for (const [x, z] of aRound(9, 10.0, -175, -5)) T(x, z);
  for (const [x, z] of aRound(7, 8.0, -165, -15)) T(x, z);
  for (const [x, z] of [[2.5, 15], [21.5, 15]]) T(x, z);
  aRing(R, { posts: false, top: "treeLeaf", bands: () => [[0, 8, "treeLeaf"]] });
  aCeil(R, "arCanopy", 7.6);
  tallGrass(R, 4, 16, 7, 18); tallGrass(R, 17, 16, 20, 18);
  if (extra) extra(R);
});
forestArena("viridian_forest", "Viridian Forest", ["viridian_forest"]);
forestArena("ilex_forest", "Ilex Forest", ["ilex_forest"], R => {
  // Its little shrine at the back.
  R.geo.box(11, 0, 2.2, 13, 0.4, 3.6, { all: "statueStone" }); R.geo.box(11.3, 0.4, 2.4, 12.7, 1.4, 3.4, { all: "arShrine" }); R.geo.box(11.1, 1.4, 2.2, 12.9, 1.7, 3.6, { all: "gymBoardsDark" });
});
arenaDef("safari_zone", "Safari Zone", ["safari_zone"], R => {
  // The SAFARI ZONE's grassland: grass, sandy ground at the edge, a pond, rocks, tall grass, its trees.
  aFloor(R, (x, z) => aDist(x, z) > 9 ? "arSafariSand" : "arSafariGrass");
  for (let z = 2; z < 6; z++) for (let x = 3; x < 7; x++) if (Math.hypot(x - 4.5, z - 3.5) < 2.2) R.geo.box(x, -0.36, z, x + 1, -0.12, z + 1, { top: "fitWater", all: null });
  for (const [x, z] of [[18, 4], [20, 9]]) aRock(R, x, z, 1.0, aRockMats("gymTanLow", "gymTan", "gymTanTop"));
  for (const [x, z] of aRound(8, 10.0, -175, -5)) tree(R, Math.floor(x) + 0.5, Math.floor(z) + 0.5, 1.0);
  tallGrass(R, 3, 8, 6, 11); tallGrass(R, 18, 12, 21, 15);
  aRing(R, { posts: false, top: "treeLeaf", bands: () => [[0, 8, "treeLeaf"]] });
  aCeil(R, "arCanopy", 7.6);
});

// ---------- (0.63) The two outdoor stages the viewer didn't have yet, as the mod builds them (client/BattleScene) ----------
// By the water (a battle on or beside it: Routes 19, 20 and 21 on the sea, fishing, anywhere with water close by) and
// in a town (CERULEAN CITY's). Like the Route battle, the game's is Minecraft's own blocks, put up over you; its
// trees and plants fall a little differently each time (the game places them at random round the rings shown).
{
  const outStage = (id, name, also, build, wet = false) => {
    PLACES.push({ id, name, floor: "Battle arena (outside)", group: "Battle arenas", also });
    BUILD[id] = function () {
      const R = new Room(id, { name, floor: "Battle arena (outside)", W: 40, D: 40, H: 8, outside: true });
      const cx = 20, cz = 20, r = rng(hashStr(id)), cam = [cx + 0.5 - 1.4, cz + 0.5 + 9.8];
      if (!wet) ground(R, "grass");
      else {
        // The water across the back half (a little below the grass), the land from the sand forward.
        R.geo.box(0, -0.4, 0, R.W, -0.12, cz - 1, { top: "arWater", n: "cap", e: "cap", w: "cap", s: null, bottom: null });
        R.geo.box(0, -0.4, cz - 1, R.W, 0, R.D, { top: "grass", s: "cap", n: "cap", e: "cap", w: "cap", bottom: null });
      }
      const away = (x, z, d) => Math.hypot(x - cam[0], z - cam[1]) >= d;
      // A ring of trees as the game's (n of them round a radius, each on a block, none where the camera stands).
      const ring = (rad, n, s, ok = () => true) => {
        for (let i = 0; i < n; i++) {
          const a = (i * 360 / n + Math.floor(r() * 10)) * D2R, bx = cx + Math.round(Math.sin(a) * rad), bz = cz + Math.round(Math.cos(a) * rad);
          if (away(bx, bz, 6.5) && ok(bx - cx, bz - cz)) tree(R, bx + 0.5, bz + 0.5, s);
        }
      };
      // Plants between two radii, not between the camera and the POKéMON.
      const plants = (from, to, chance, put) => {
        for (let dx = -Math.floor(to); dx <= to; dx++) for (let dz = -Math.floor(to); dz <= to; dz++) {
          const d = Math.hypot(dx, dz);
          if (d < from || d > to || r() > chance || (dz > 0 && Math.abs(dx) < 9)) continue;
          put(cx + dx, cz + dz, dx, dz);
        }
      };
      build(R, { cx, cz, r, ring, plants });
      R.spawn = { x: cx + 0.5 - 0.6, z: cz + 0.5 + 2.2, yaw: FACE.n };
      return R;
    };
  };
  const cross = (R, x, z, mat, h) => { R.geo.box(x + 0.15, 0, z + 0.5, x + 0.85, h, z + 0.5, { s: mat, n: mat, all: null }); R.geo.box(x + 0.5, 0, z + 0.15, x + 0.5, h, z + 0.85, { e: mat, w: mat, all: null }); };
  outStage("arena_water", "Battle by the water", "sea surf surfing route 19 route 20 route 21 fishing water beach", (R, o) => {
    // The back half's water, a strip of sand, the grass you stand on; trees round the shore; reeds, cane and lily pads.
    for (let dx = -19; dx <= 19; dx++) for (const dz of [-1, 0]) patch(R, o.cx + dx, o.cz + dz, o.cx + dx + 1, o.cz + dz + 1, "arSand");
    o.ring(12.5, 9, 1.2, (dx, dz) => dz >= -1);
    o.ring(16.5, 22, 1.6, (dx, dz) => dz >= -1);
    o.plants(4, 9, 0.2, (x, z, dx, dz) => {
      const k = Math.floor(o.r() * 3);
      if (dz < -1) { if (k === 2) R.geo.box(x + 0.15, -0.11, z + 0.15, x + 0.85, -0.1, z + 0.85, { top: "treeLeaf", all: null }); return; }
      if (k === 1) R.geo.box(x + 0.4, 0, z + 0.4, x + 0.6, 1.9, z + 0.6, "stem");
      else if (k === 0) cross(R, x, z, "tallGrass", 0.6);
    });
    R.item("The ground", "The water across the back (where theirs is, on it), a strip of sand, the grass you stand on. Trees round the shore (two rings, none out on the water), reeds, sugar cane, lily pads on the water.");
  }, true);
  outStage("arena_town", "Town battle", "town city cerulean square", (R, o) => {
    // Grass with a path across it both ways, two rings of trees, flowers, six lamp posts round the edge (none in front).
    for (let dx = -19; dx <= 19; dx++) for (let dz = -19; dz <= 19; dz++) {
      if (Math.hypot(dx, dz) > 19.5) continue;
      if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) patch(R, o.cx + dx, o.cz + dz, o.cx + dx + 1, o.cz + dz + 1, "path");
    }
    o.ring(12.5, 8, 1.2);
    o.ring(16.5, 24, 1.6);
    o.plants(7, 11, 0.25, (x, z) => { const k = Math.floor(o.r() * 3); cross(R, x, z, k === 2 ? "tallGrass" : "flower", 0.6); });
    for (let i = 0; i < 6; i++) {
      const a = (i * 60 + 30) * D2R, lx = o.cx + Math.round(Math.sin(a) * 9.5), lz = o.cz + Math.round(Math.cos(a) * 9.5);
      if (lz - o.cz > 5) continue;
      R.geo.box(lx + 0.38, 0, lz + 0.38, lx + 0.62, 2, lz + 0.62, "oak");
      R.geo.box(lx + 0.3, 2, lz + 0.3, lx + 0.7, 2.5, lz + 0.7, { all: "glass", top: "steel" });
    }
    R.item("The ground", "A town's square: grass with a dirt path across it both ways, flowers, lamp posts round the back half, two rings of trees.");
  });
}
