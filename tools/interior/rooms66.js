// ---------- (0.61, John's 19:22) The fit-out's second pass: "just have another pass, another overhaul, please" ----------
// Everything here only with FIT on (the viewer's "Fit-out: New"), for his yes.

// A box left out, while a room's built, when it matches (a room's own tweak: the LEAGUE gate's lintel).
let FIT_SKIP = null;
{
  const gb = Geo.prototype.box;
  Geo.prototype.box = function (x0, y0, z0, x1, y1, z1, spec) {
    if (FIT && FIT_SKIP && FIT_SKIP(x0, y0, z0, x1, y1, z1, spec)) return;
    return gb.call(this, x0, y0, z0, x1, y1, z1, spec);
  };
}
function fitTweak(id, skip, after) {
  const orig = BUILD[id];
  if (!orig) return;
  BUILD[id] = function () {
    if (!FIT) return orig.call(this);
    FIT_SKIP = skip || null;
    let R;
    try { R = orig.call(this); } finally { FIT_SKIP = null; }
    if (after) after(R);
    return R;
  };
}

// ----- the LEAGUE gate: no lintel over the way on ("like ... a football goalpost without a net") -----
fitTweak("gate_league", (x0, y0, z0, x1, y1, z1, spec) => spec === "leagueGold" && y0 > 2 && x1 - x0 > 2);

// ----- hanging baskets: none ("Clearly, we're not going to want to do that"); plants stand on the floor instead -----
{
  const hp = hangingPlant;
  hangingPlant = function (R, ...a) { if (!FIT) return hp(R, ...a); };
}

// ----- pictures, to give the walls some life -----
const FIT_PICS = ["picSunset", "picStars", "paintingHills", "painting", "photos", "picSnap"];
function fitPic(R, wall, at, y = 1.5, k = 0, w = 1.2, h = 0.9) {
  framed(R, at, y, w, h, FIT_PICS[k % FIT_PICS.length], wall, { act: { label: "Look", lines: ["A picture in a wooden frame."] } });
}

// ----- greenery: potted plants where they're out of everyone's way (19:22: "a little bit of greenery which doesn't
// impact any of the pathways or any of the stuff that's linear to the game") -----
// The room's floor as blocks: a block is open if it's floor and nothing solid stands in it. A plant goes on an open
// block against a wall (a corner first) only if every other open block can still be walked to from the way in, it's
// clear of the ways in and out, of anyone (and of any TRAINER's line of sight), and of anything you look at or use.
const FIT_GREEN_SKIP = /^ug_(ns|ew)$|mansion_[0-9b]|hall_of_fame|gym|champion|loreleis|brunos|agathas|lances|_room$|tower|cave|arena|elevator|lift|mt_|tunnel|seafoam|victory|forest|underground|_ug_|ug_path|silver|well|ruins|sprout|burned|tin_|bell|lighthouse|whirl|ice_path|dragons|kitchen|captains|ss_anne_\d?f?$|museum|power_plant|safari/;
function fitGreen(R) {
  if (!R || R.outside || R.dark || R.haze || R.night || FIT_GREEN_SKIP.test(R.id)) return 0;
  const W = Math.floor(R.W), D = Math.floor(R.D);
  if (W < 4 || D < 4) return 0;
  const floorOK = (x, z) => !R._floors ? x >= (R.Lx || 0) && x + 1 <= (R.Rx || R.W) && z >= (R.Nm || 0) && z + 1 <= (R.Dm || R.D) : R._floors.some(f => x >= f[0] - 1e-6 && x + 1 <= f[2] + 1e-6 && z >= f[1] - 1e-6 && z + 1 <= f[3] + 1e-6);
  const hit = (r, x, z, m = 0) => r[0] < x + 1 + m - 1e-6 && r[2] > x - m + 1e-6 && r[1] < z + 1 + m - 1e-6 && r[3] > z - m + 1e-6;
  // The middle of the block, a little in (a solid that only skims its edge doesn't count).
  const solidIn = (x, z) => R.solids.some(s => s[0] < x + 0.8 && s[2] > x + 0.2 && s[1] < z + 0.8 && s[3] > z + 0.2);
  const open = [];
  for (let z = 0; z < D; z++) { open.push([]); for (let x = 0; x < W; x++) open[z].push(floorOK(x, z) && !solidIn(x, z)); }
  const flat = (x, z) => [[0.2, 0.2], [0.8, 0.2], [0.2, 0.8], [0.8, 0.8]].every(([u, v]) => Math.abs(R.groundAt(x + u, z + v)) < 0.01) && !R.ramps.some(r => hit(r.rect, x, z, 1));
  const isOpen = (x, z) => x >= 0 && z >= 0 && x < W && z < D && open[z][x];
  // Where you come in: the spawn, else the way-out arrow, else the first open block.
  let st = R.spawn ? [Math.floor(R.spawn.x), Math.floor(R.spawn.z)] : R.exitArrow ? [Math.floor(R.exitArrow.x), Math.floor(R.exitArrow.z) - 1] : null;
  if (!st || !isOpen(st[0], st[1])) { st = null; for (let z = D - 1; z >= 0 && !st; z--) for (let x = 0; x < W && !st; x++) if (isOpen(x, z)) st = [x, z]; }
  if (!st) return 0;
  const reach = (bx, bz) => {
    const seen = new Set([st[0] + "," + st[1]]), q = [st];
    while (q.length) { const [x, z] = q.pop(); for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const a = x + dx, b = z + dz, k = a + "," + b; if ((a === bx && b === bz) || seen.has(k) || !isOpen(a, b)) continue; seen.add(k); q.push([a, b]); } }
    return seen.size;
  };
  const all = reach(-9, -9);
  // In a TRAINER's (or anyone's) line of sight: the blocks straight out in front of them, 6 deep.
  const sight = new Set();
  for (const n of R.npcs) {
    const yaw = n.yaw || 0, dx = Math.round(-Math.sin(yaw)), dz = Math.round(-Math.cos(yaw));
    for (let i = 0; i <= 6; i++) sight.add(Math.floor(n.x + dx * i) + "," + Math.floor(n.z + dz * i));
  }
  const ways = [...(R._boxes || []).map(b => [b.x0, b.z0, b.x1, b.z1]), ...R.warps.map(w => w.rect).filter(Boolean)];
  const ok = (x, z) => {
    if (!isOpen(x, z) || !flat(x, z) || sight.has(x + "," + z)) return false;
    if ((x === st[0] && z === st[1])) return false;
    if (R.acts.some(a => hit(a.rect, x, z, 0.35))) return false;
    if (R.npcs.some(n => Math.hypot(n.x - x - 0.5, n.z - z - 0.5) < 1.9)) return false;
    if ((R.pets || []).some(n => Math.hypot(n.x - x - 0.5, n.z - z - 0.5) < 2.5)) return false;
    if (R.exitArrow && Math.hypot(R.exitArrow.x - x - 0.5, R.exitArrow.z - z - 0.5) < 3.2) return false;
    if (R.spawn && Math.hypot(R.spawn.x - x - 0.5, R.spawn.z - z - 0.5) < 2.5) return false;
    if (ways.some(b => hit(b, x, z, 1.2))) return false;
    for (const nk of [R.westNook, R.eastNook]) if (nk && z + 1 > nk[0] - 1 && z < nk[1] + 1) return false;
    if (R.northNook && x + 1 > R.northNook[0] - 1 && x < R.northNook[1] + 1 && z < (R.Nm || 0) + 3) return false;
    if (R.entry && x + 1 > R.entry[0] - 1 && x < R.entry[1] + 1 && z >= (R.Dm || R.D) - 3) return false;
    // Against a wall: how many sides are shut (2 is a corner).
    const shut = [[1, 0], [-1, 0], [0, 1], [0, -1]].filter(([dx, dz]) => !isOpen(x + dx, z + dz)).length;
    if (shut < 1 || shut > 2) return false;
    return reach(x, z) === all - 1;
  };
  const cand = [];
  for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) if (ok(x, z)) {
    const shut = [[1, 0], [-1, 0], [0, 1], [0, -1]].filter(([dx, dz]) => !isOpen(x + dx, z + dz)).length;
    cand.push({ x, z, s: shut * 10 + (hashStr(R.id + x + "," + z) % 7) });
  }
  cand.sort((a, b) => b.s - a.s);
  const max = Math.max(1, Math.min(4, Math.floor(all / 30)));
  const put = [];
  for (const c of cand) {
    if (put.length >= max) break;
    if (put.some(p => Math.hypot(p.x - c.x, p.z - c.z) < 4)) continue;
    // Taken: shut it, so the next is checked with this one in place.
    open[c.z][c.x] = false;
    if (reach(-9, -9) !== all - 1 - put.length) { open[c.z][c.x] = true; continue; }
    put.push(c);
  }
  put.forEach((c, i) => (i % 2 ? slimPlant : plant)(R, c.x + 0.5, c.z + 0.5, 0.95, { pot: "potWhite", kind: ["leafy", "fern", "bush"][i % 3], lines: ["A leafy plant in a white pot."] }));
  if (put.length) R.item("Plants", "Potted plants on blocks against the walls, where nothing has to get past.");
  return put.length;
}
{
  // Every room built with FIT on gets the greenery pass after its own furniture (getRoom calls finish after).
  const fin = Room.prototype.finish;
  Room.prototype.finish = function () {
    if (FIT && !this._greened) { this._greened = true; try { this._green = fitGreen(this); } catch (e) { this._greenErr = String(e); } }
    return fin.call(this);
  };
}

// ----- the DAY CARE: pictures on the walls ("there's no reason why there can't be some photos on the wall") -----
fitTweak("daycare", null, R => {
  fitPic(R, "n", 1.0, 1.5, 4, 1.4, 1.0);
  fitPic(R, "n", 7.2, 1.7, 0, 1.4, 0.9);
  fitPic(R, "n", 10.2, 1.7, 2, 1.2, 0.9);
  fitPic(R, "w", 4.4, 1.5, 3, 1.4, 1.0);
  R.item("Pictures", "On the walls: the POKéMON he's raised, and the yard in summer.");
});

// ----- the gates' upstairs: a rug, pictures, more plants ("there's no reason why there can't be more decor upstairs") -----
for (const id of ["gate_r11_2f", "gate_r12_2f", "gate_r15_2f", "gate_r16_2f", "gate_r18_2f"]) fitTweak(id, null, R => {
  rug(R, 3, 3, 8, 7, "rugGateBlue");
  fitPic(R, "w", 2.0, 1.5, (id.length) % 6, 1.4, 1.0);
  fitPic(R, "n", 6.4, 1.6, (id.length + 2) % 6, 1.2, 0.9);
  sofa(R, 9, 8, "n", 2);
  R.item("The lounge", "A blue rug, pictures of the routes round here, a sofa under the south window.");
});
RUGS.rugGateBlue = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#2F5FA8" : "#8DB4E0";

// ----- the soft materials for what this pass redraws -----
Object.assign(FIT_TEX, {
  // The museum's floor: big pale stone flags, a block each, as the outside's paving (not tiles with lines).
  museumFloor: [64, 64, (P, r) => fitTiles(P, r, hx("#D9CDB5"), hx("#CFC2A6"), 2), 4],
  // The daycare's and the cabins' boards: the block planks.
  midBoards: [64, 64, (P, r) => fitBoards(P, r, hx("#B98350")), 4],
});

// ----- greenery textured as the outside's (John: "what does a hedge look like in game? Bring that into there") -----
// The look's own leaves (resourcepacks/retromon_look: azalea, flowering azalea), made solid as a clipped hedge is (the
// gaps the leaves' own deep green).
function fitLeaves(P, name, deep) {
  if (typeof AREATEX === "undefined" || !AREATEX[name]) { fitPaint(P, rng(1), hx("#4E8A3E"), { drift: 0.06 }); return; }
  P.area(name, c => c[3] < 128 ? [deep[0], deep[1], deep[2], 255] : [c[0], c[1], c[2], 255]);
}
// The leaves' own tufts standing out of a hedge's top (as the game's leafy hedges have), cut out.
{
  TEXDEF.fitLeafTuft = [16, 16, P => { if (typeof AREATEX !== "undefined" && AREATEX.look_azalea_leaves) P.area("look_azalea_leaves", (c, i, j) => Math.hypot(i - 7.5, j - 8) > 7.6 ? [0, 0, 0, 0] : c); }];
  MATDEF.fitLeafTuft = { tex: "fitLeafTuft", fit: true, cut: true };
  TEXDEF.fitLeafTuftF = [16, 16, P => { if (typeof AREATEX !== "undefined" && AREATEX.look_flowering_azalea_leaves) P.area("look_flowering_azalea_leaves", (c, i, j) => Math.hypot(i - 7.5, j - 8) > 7.6 ? [0, 0, 0, 0] : c); }];
  MATDEF.fitLeafTuftF = { tex: "fitLeafTuftF", fit: true, cut: true };
  MATDEF.cgHedgeFlower = { tex: "cgHedgeFlower", per: 1 };
  TEXDEF.cgHedgeFlower = TEXDEF.cgHedge;
}
// A hedge block as the game has it: a whole block of leaves, a couple of tufts standing out of its top.
function fitHedge(G, a, b, h, flower, seed) {
  const k = flower ? "cgHedgeFlower" : "cgHedge";
  G.box(a, 0, b, a + 1, h, b + 1, { all: k, top: k });
  const t = 0.004, tuft = flower ? "fitLeafTuftF" : "fitLeafTuft";
  for (let i = 0; i < 2; i++) {
    const cx = a + 0.3 + ((seed * 7 + i * 5) % 5) * 0.1, cz = b + 0.3 + ((seed * 3 + i * 7) % 5) * 0.1, w = 0.22, y = h - 0.08;
    G.box(cx - w, y, cz - t, cx + w, y + 0.36, cz + t, { s: tuft, n: tuft, all: null });
    G.box(cx - t, y, cz - w, cx + t, y + 0.36, cz + w, { e: tuft, w: tuft, all: null });
  }
}
Object.assign(FIT_TEX, {
  cgHedge: [16, 16, P => fitLeaves(P, "look_azalea_leaves", [38, 84, 34]), 1],
  cgHedgeTop: [16, 16, P => fitLeaves(P, "look_azalea_leaves", [38, 84, 34]), 1],
  cgHedgeFlower: [16, 16, P => fitLeaves(P, "look_flowering_azalea_leaves", [38, 84, 34]), 1],
  treeLeaf: [16, 16, P => fitLeaves(P, "look_azalea_leaves", [34, 78, 32]), 1],
});

// ----- real shelves (the MART's, the DEPT. STORE's): open shelves with things on them ("not just a friggin' box") -----
{
  const add = (k, c, o = {}) => { TEXDEF[k] = [16, 16, (P, r) => { fitPaint(P, r, hx(c), { drift: 0.03 }); if (o.label) P.rect(2, 6, 12, 4, hx(o.label)); if (o.top) P.rect(0, 0, 16, 3, hx(o.top)); }]; MATDEF[k] = { tex: k, per: 1 }; };
  add("goodRed", "#D9534F", { label: "#F6E8E4" }); add("goodBlue", "#3E7CC9", { label: "#E6EEF8" }); add("goodYellow", "#E8B83A", { label: "#FFF6D8" });
  add("goodGreen", "#4FA65A", { label: "#EAF6EA" }); add("goodPurple", "#8A5AB8", { label: "#F0E8F8" }); add("goodOrange", "#E58A3A", { label: "#FFF0E0" });
  add("goodBottle", "#B8DDF0", { top: "#E8F4FA" }); add("goodCap", "#F4F4F0");
  add("shelfBoard", "#F2F0EA", { top: "#FFFFFF" });
  TEXDEF.shelfBack = [16, 16, (P, r) => { fitPaint(P, r, hx("#D2E0EE"), { drift: 0.03 }); for (let y = 2; y < 16; y += 4) for (let x = 2; x < 16; x += 4) P.set(x, y, hx("#B8CADC")); }];
  MATDEF.shelfBack = { tex: "shelfBack", per: 1 };
}
const GOODS = ["goodRed", "goodBlue", "goodYellow", "goodGreen", "goodPurple", "goodOrange"];
// (20:37: "Same with the shelves, just improve or increase what you've already started") Goods as a Mart has them:
// boxes with their labels, two stacked, POTION sprays (a tall body, a white nozzle), bottles with caps, short tins,
// jars, and POKé BALLS three to a tray; packed close, each shelf's edge a price strip.
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("goodSpray", 8, 16, (P, r) => { fitPaint(P, r, hx("#9A6CD0"), { drift: 0.03 }); P.rect(0, 5, 8, 5, hx("#F4F0FA")); P.rect(2, 6, 4, 1, hx("#9A6CD0")); P.rect(0, 0, 8, 1, hx("#B48CE4")); }, { fit: true });
  add("goodSpray2", 8, 16, (P, r) => { fitPaint(P, r, hx("#E2686E"), { drift: 0.03 }); P.rect(0, 5, 8, 5, hx("#FFF2F0")); P.rect(2, 6, 4, 1, hx("#E2686E")); }, { fit: true });
  add("goodSpray3", 8, 16, (P, r) => { fitPaint(P, r, hx("#4C9AD8"), { drift: 0.03 }); P.rect(0, 5, 8, 5, hx("#EEF6FC")); P.rect(2, 6, 4, 1, hx("#4C9AD8")); }, { fit: true });
  add("goodNozzle", 8, 8, (P, r) => fitPaint(P, r, hx("#F6F6F2"), { drift: 0.01 }));
  add("goodTin", 8, 8, (P, r) => { fitPaint(P, r, hx("#C8CED6"), { drift: 0.02 }); P.rect(0, 3, 8, 3, hx("#E8B43C")); });
  add("goodJar", 8, 8, (P, r) => { fitPaint(P, r, hx("#F2D6A0"), { drift: 0.02 }); P.rect(0, 0, 8, 2, hx("#C8503C")); P.rect(1, 4, 6, 2, hx("#FFFFFF")); });
  add("goodTray", 16, 8, (P, r) => fitPaint(P, r, hx("#5A6A82"), { drift: 0.02 }));
  add("goodBall", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#F4F4F0")); P.rect(0, 0, 8, 3, hx("#E04848")); P.rect(0, 3, 8, 1, hx("#303038")); P.rect(3, 3, 2, 2, hx("#F4F4F0")); P.set(3, 3, hx("#303038")); P.set(4, 4, hx("#303038")); P.set(0, 0, hx("#B83A3A")); P.set(7, 0, hx("#B83A3A")); P.set(0, 7, hx("#D8D8D2")); P.set(7, 7, hx("#D8D8D2")); }, { fit: true });
  add("goodBallTop", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#E04848")); P.rect(2, 2, 4, 4, hx("#EE6A6A")); P.set(0, 0, hx("#B83A3A")); P.set(7, 0, hx("#B83A3A")); P.set(0, 7, hx("#B83A3A")); P.set(7, 7, hx("#B83A3A")); }, { fit: true });
  add("priceStrip", 16, 4, (P, r) => { P.rect(0, 0, 16, 4, hx("#F6E27A")); for (let x = 2; x < 16; x += 5) P.rect(x, 1, 2, 2, hx("#D84848")); }, { fit: true });
  // Box fronts with a label: the box's colour, a white panel, a stripe.
  for (const [k, c] of [["goodRed", "#D9534F"], ["goodBlue", "#3E7CC9"], ["goodYellow", "#E8B83A"], ["goodGreen", "#4FA65A"], ["goodPurple", "#8A5AB8"], ["goodOrange", "#E58A3A"]])
    add(k + "Front", 16, 16, (P, r) => { fitPaint(P, r, hx(c), { drift: 0.03 }); P.rect(2, 4, 12, 7, hx("#F8F6F0")); P.rect(4, 6, 8, 1, hx(c)); P.rect(4, 8, 6, 1, hx(c)); P.rect(0, 0, 16, 2, shadec(hx(c), 0.08)); }, { fit: true });
}
const SPRAYS = ["goodSpray", "goodSpray2", "goodSpray3"];
// One shelf's row of goods along a frame's x from a to b at height y (under the next shelf, room h), depth to d.
function goodsRow(G, F, a, b, y, d, seed, bottles, room = 0.44, strip = true) {
  const r = rng(seed), front = d - 0.05;
  let x = a + 0.03;
  while (x < b - 0.12) {
    const k = bottles ? 0.35 + r() * 0.3 : r(), m = GOODS[Math.floor(r() * GOODS.length)];
    if (k < 0.3) {
      // A box (or two stacked), its label to the front.
      const w = Math.min(b - 0.03 - x, 0.2 + r() * 0.12), h = Math.min(room - 0.04, 0.18 + r() * 0.12);
      F.box(x, y, front - 0.32, x + w, y + h, front, { all: m, front: m + "Front", top: m });
      if (r() < 0.45 && h * 2 < room - 0.04) F.box(x + 0.01, y + h, front - 0.31, x + w - 0.01, y + h * 2, front - 0.01, { all: m, front: m + "Front", top: m });
      x += w + 0.03;
    } else if (k < 0.5) {
      // POTION sprays, two or three side by side.
      const n = 2 + (r() < 0.5 ? 1 : 0), sp = SPRAYS[Math.floor(r() * 3)], h = Math.min(room - 0.1, 0.26 + r() * 0.06);
      for (let i = 0; i < n && x + 0.12 < b; i++, x += 0.12) {
        F.box(x + 0.01, y, front - 0.12, x + 0.1, y + h, front - 0.02, { all: sp, top: "goodNozzle" });
        F.box(x + 0.035, y + h, front - 0.09, x + 0.075, y + h + 0.06, front - 0.05, "goodNozzle");
      }
      x += 0.04;
    } else if (k < 0.66) {
      // Bottles with caps.
      const h = Math.min(room - 0.1, 0.24 + r() * 0.08), bc = r() < 0.6 ? "goodBottle" : m;
      for (let i = 0; i < 3 && x + 0.11 < b; i++, x += 0.11) {
        F.box(x + 0.015, y, front - 0.11, x + 0.095, y + h, front - 0.03, { all: bc });
        F.box(x + 0.035, y + h, front - 0.09, x + 0.075, y + h + 0.05, front - 0.05, "goodCap");
      }
      x += 0.04;
    } else if (k < 0.78) {
      // Short tins, or jars, in a little stack.
      const t = r() < 0.5 ? "goodTin" : "goodJar";
      for (let i = 0; i < 3 && x + 0.14 < b; i++, x += 0.14) F.box(x + 0.01, y, front - 0.14, x + 0.13, y + 0.12, front - 0.02, { all: t });
      if (x - 0.42 > a) F.box(x - 0.35, y + 0.12, front - 0.13, x - 0.23, y + 0.24, front - 0.03, { all: t });
      x += 0.04;
    } else if (!bottles && x + 0.62 < b) {
      // POKé BALLS, three to a tray.
      F.box(x, y, front - 0.26, x + 0.6, y + 0.03, front, "goodTray");
      // (Each ball a single block, its picture a POKé BALL's, so a shelf of them stays light in the game.)
      for (let i = 0; i < 3; i++) F.box(x + 0.02 + i * 0.2, y + 0.03, front - 0.21, x + 0.18 + i * 0.2, y + 0.19, front - 0.05, { all: "goodBall", top: "goodBallTop" });
      x += 0.64;
    } else x += 0.05;
  }
  // The shelf's edge: a price strip.
  if (strip) F.box(a - 0.01, y - 0.05, d - 0.01, b + 0.01, y, d + 0.005, { front: "priceStrip", all: null });
}
// Stock stood all over a top (seen from above): rows of goods from its front to its back.
function topGoods(G, F, a, b, y, d, seed, bottles) {
  let i = 0;
  for (let z = d; z > 0.3; z -= 0.34, i++) goodsRow(G, F, a, b, y, z, seed + i * 31, bottles, 0.44, false);
}
{
  const ms = martShelf;
  martShelf = function (room, X, Z, f, w = 4, h = 2.2, o = {}) {
    if (!FIT) return ms(room, X, Z, f, w, h, o);
    X = gridE(X); Z = gridE(Z); w = gridW(w);
    const F = room.F(X, Z, f), d = o.depth && o.depth < 0.7 ? 0.5 : 1, G = room.geo, levels = Math.max(3, Math.floor(h / 0.55));
    F.box(0, 0, 0, 0.08, h, d, "shelfBoard"); F.box(w - 0.08, 0, 0, w, h, d, "shelfBoard");
    F.box(0.08, 0, 0, w - 0.08, 0.16, d, { all: "shelfBoard", front: o.kick || "blueTrim" });
    if (!o.open) F.box(0.08, 0.16, 0, w - 0.08, h, 0.06, "shelfBack");
    const gap = (h - 0.16) / levels;
    for (let i = 0; i < levels; i++) {
      const y = 0.16 + i * gap;
      F.box(0.08, y, 0.06, w - 0.08, y + 0.04, d, "shelfBoard");
      goodsRow(G, F, 0.1, w - 0.1, y + 0.04, d, hashStr(room.id + X + "," + Z + ":" + i), false, gap - 0.06);
    }
    F.box(0.08, h - 0.04, 0.06, w - 0.08, h, d, "shelfBoard");
    // More stock stood on the top, so from above it's a shelf of goods, not a box.
    if (!o.noTop) topGoods(G, F, 0.1, w - 0.1, h, d, hashStr(room.id + X + "," + Z + ":top"));
    F.solid(0, 0, w, d);
    room.act(F.rect(0, 0.2, w, d + 0.6), { label: "Look", lines: o.lines || ["Shelves of POTIONS, ANTIDOTES and POKé BALLS. The clerk at the counter sells them."] });
    room.item("Mart shelves", "Open white shelving, three or four shelves high, real goods on every one: boxes, bottles with caps, POKé BALLS.");
  };
}
// The south end of a run of shelves (what faces you): its own shelves, the goods facing out.
function endDisplay(R, x0, x1, zEnd, h, seed, bottles) {
  const F = R.F(x0, zEnd - 0.45, "s"), w = x1 - x0, G = R.geo, n = Math.max(3, Math.floor(h / 0.55));
  F.box(0, 0, 0, 0.06, h, 0.45, "shelfBoard"); F.box(w - 0.06, 0, 0, w, h, 0.45, "shelfBoard");
  F.box(0.06, 0, 0, w - 0.06, 0.16, 0.45, { all: "shelfBoard", front: "blueTrim" });
  F.box(0.06, 0.16, 0, w - 0.06, h, 0.04, "shelfBack");
  for (let i = 0; i < n; i++) { const y = 0.16 + i * (h - 0.16) / n; F.box(0.06, y, 0.04, w - 0.06, y + 0.04, 0.45, "shelfBoard"); goodsRow(G, F, 0.08, w - 0.08, y + 0.04, 0.45, seed + i * 13, bottles); }
  F.box(0.06, h - 0.04, 0, w - 0.06, h, 0.45, "shelfBoard");
}
// The DEPT. STORE's racks (rooms58.js, a FireRed rack a piece): the same open shelves, both sides of a free-standing
// rack (its long side facing the aisles), with a board down its middle.
function fitRack(R, x0, z0, x1, z1, seed, bottles) {
  const G = R.geo, alongX = x1 - x0 >= z1 - z0, h = 1.7;
  if (alongX) {
    for (const [f, Z] of [["s", (z0 + z1) / 2], ["n", (z0 + z1) / 2]]) {
      const F = R.F(f === "s" ? x0 : x1, Z, f), w = x1 - x0, d = (z1 - z0) / 2 - 0.04;
      F.box(0, 0, 0, 0.06, h, d, "shelfBoard"); F.box(w - 0.06, 0, 0, w, h, d, "shelfBoard");
      F.box(0.06, 0, 0, w - 0.06, 0.14, d, "shelfBoard");
      for (let i = 0; i < 3; i++) { const y = 0.14 + i * 0.5; F.box(0.06, y, 0.02, w - 0.06, y + 0.04, d, "shelfBoard"); goodsRow(G, F, 0.08, w - 0.08, y + 0.04, d, seed + i * 7 + (f === "s" ? 0 : 99), bottles); }
      F.box(0.06, h - 0.04, 0, w - 0.06, h, d, "shelfBoard");
    }
    G.box(x0 + 0.06, 0, (z0 + z1) / 2 - 0.03, x1 - 0.06, h, (z0 + z1) / 2 + 0.03, "shelfBack");
  } else {
    for (const [f, X] of [["e", (x0 + x1) / 2], ["w", (x0 + x1) / 2]]) {
      const ze = z1 - z0 >= 2 ? z1 - 0.45 : z1, F = R.F(X, f === "e" ? ze : z0, f), w = ze - z0, d = (x1 - x0) / 2 - 0.04;
      F.box(0, 0, 0, 0.06, h, d, "shelfBoard"); F.box(w - 0.06, 0, 0, w, h, d, "shelfBoard");
      F.box(0.06, 0, 0, w - 0.06, 0.14, d, "shelfBoard");
      for (let i = 0; i < 3; i++) { const y = 0.14 + i * 0.5; F.box(0.06, y, 0.02, w - 0.06, y + 0.04, d, "shelfBoard"); goodsRow(G, F, 0.08, w - 0.08, y + 0.04, d, seed + i * 7 + (f === "e" ? 0 : 99), bottles); }
      F.box(0.06, h - 0.04, 0, w - 0.06, h, d, "shelfBoard");
    }
    G.box((x0 + x1) / 2 - 0.03, 0, z0 + 0.06, (x0 + x1) / 2 + 0.03, h, (z1 - z0 >= 2 ? z1 - 0.45 : z1) - 0.06, "shelfBack");
  }
  // Stock on top (seen from above), and at a long rack's south end its own end shelves.
  { const F = R.F(x0, z0, "s"); topGoods(G, F, 0.1, x1 - x0 - 0.1, h, z1 - z0 - 0.05, seed + 555, bottles); }
  if (!alongX && z1 - z0 >= 2) endDisplay(R, x0, x1, z1, h, seed + 777, bottles);
}
// The mart's island shelves on the grid: two blocks across (a block of shelf each side of its spine), flush ends.
{
  const isl = islandShelf;
  islandShelf = function (R, X, Z, len, h = 1.6, o = {}) {
    if (!FIT) return isl(R, X, Z, len, h, o);
    X = gridE(X); Z = gridE(Z); len = gridW(len);
    martShelf(R, X, Z, "w", len - 0.45, h, { seed: o.seed || 0, lines: o.lines, depth: 1 });
    martShelf(R, X, Z + len - 0.45, "e", len - 0.45, h, { seed: (o.seed || 0) + 1, lines: o.lines, depth: 1 });
    R.geo.box(X - 0.04, 0.16, Z, X + 0.04, h, Z + len - 0.45, "shelfBack");
    endDisplay(R, X - 1, X + 1, Z + len, h, hashStr(R.id + X + "," + Z + ":end"));
    R.solid(X - 1, Z, X + 1, Z + len);
  };
}

// ---------- the FireRed rooms (rooms64.js) as the fit-out reads them ----------
let FIT_CUR = null;
const FR_WALK = ".XueNF";
function fitRows(id, F) {
  const W = F.W, D = F.D, rows = F.rows.map(r => r.split(""));
  const at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "v" : rows[z][x];
  const warpCell = new Set(F.warps.map(w => w.x + "," + w.z));
  const walk = (x, z) => FR_WALK.includes(at(x, z)) || warpCell.has(x + "," + z);
  // The S.S. ANNE's kitchen as FireRed draws it (19:22: "the kitchen ... the exact same experience like the main
  // game"): its three steel islands, the bins along the east wall, the long counter across the south; all drawn by
  // fitKitchen (below), so here only marked as solid.
  if (id === "ss_anne_kitchen") {
    for (let z = 4; z <= 8; z++) for (const x of [3, 4, 7, 8, 11, 12]) rows[z][x] = "#";
    for (const z of [4, 6, 8]) rows[z][14] = "#";
    for (const z of [11, 12]) for (let x = 1; x <= 14; x++) rows[z][x] = "#";
  }
  // A cabin's bed against a wall (19:22: "where you've got the beds in the center, maybe put the beds against the
  // wall"): moved sideways the least it can be to stand against one, only onto open floor, never onto anyone or what
  // you pick up, out of every TRAINER's line of sight, and only if everywhere can still be walked to.
  const kind = F.kind;
  if (FIT && kind === "cabin") {
    const objAt = new Set(F.objs.map(o => o.x + "," + o.z));
    const sight = new Set();
    for (const o of F.objs) if (o.kind === "trainer" || o.kind === "rival") {
      const dirs = /DOWN/.test(o.face) ? [[0, 1]] : [];
      if (/UP/.test(o.face)) dirs.push([0, -1]); if (/LEFT/.test(o.face)) dirs.push([-1, 0]); if (/RIGHT/.test(o.face)) dirs.push([1, 0]);
      if (!dirs.length) dirs.push([0, 1], [0, -1], [1, 0], [-1, 0]);
      for (const [dx, dz] of dirs) for (let i = 1; i <= 6; i++) sight.add((o.x + dx * i) + "," + (o.z + dz * i));
    }
    const seeds = [...F.warps.map(w => [w.x, w.z]), ...F.objs.map(o => [o.x, o.z])];
    const reachAll = () => {
      const ok = new Set(), q = [];
      for (const [x, z] of seeds) if (walk(x, z) || objAt.has(x + "," + z)) { ok.add(x + "," + z); q.push([x, z]); }
      while (q.length) { const [x, z] = q.pop(); for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const a = x + dx, b = z + dz, k = a + "," + b; if (ok.has(k) || !walk(a, b)) continue; ok.add(k); q.push([a, b]); } }
      let n = 0; for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) if (walk(x, z) && !ok.has(x + "," + z)) n++;
      return n;
    };
    const beds = [];
    const seen = new Set();
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) if (at(x, z) === "b" && !seen.has(x + "," + z) && z > 1) {
      const cells = [[x, z]]; seen.add(x + "," + z);
      if (at(x, z + 1) === "b") { cells.push([x, z + 1]); seen.add(x + "," + (z + 1)); }
      beds.push(cells);
    }
    for (const cells of beds) {
      const against = dx => cells.every(([x, z]) => !walk(x + dx, z) && at(x + dx, z) !== "b");
      if (against(-1) || against(1)) continue;
      let done = false;
      for (const d of [1, -1, 2, -2, 3, -3]) {
        const to = cells.map(([x, z]) => [x + d, z]);
        if (!to.every(([x, z]) => at(x, z) === "." && !objAt.has(x + "," + z) && !sight.has(x + "," + z) && !warpCell.has(x + "," + z))) continue;
        const sx = Math.sign(d);
        if (!to.every(([x, z]) => !walk(x + sx, z) && at(x + sx, z) !== "b")) continue;
        for (const [x, z] of cells) rows[z][x] = ".";
        for (const [x, z] of to) rows[z][x] = "b";
        if (reachAll() === 0) { done = true; break; }
        for (const [x, z] of to) rows[z][x] = ".";
        for (const [x, z] of cells) rows[z][x] = "b";
      }
    }
  }
  // Nothing shown that you can't get to (19:22, the ship: "do not show anything that doesn't need to show"): floor
  // that can't be walked to from any way in or out, anyone or anything in the room is left out (as the dark past the
  // walls).
  {
    const seeds = [...F.warps.map(w => [w.x, w.z]), ...F.objs.map(o => [o.x, o.z]), ...(F.signs || []).map(s => [s.x, s.z]), ...(F.hidden || []).map(h => [h.x, h.z])];
    const ok = new Set(), q = [];
    for (const [x, z] of seeds) { ok.add(x + "," + z); q.push([x, z]); }
    while (q.length) {
      const [x, z] = q.pop();
      for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const a = x + dx, b = z + dz, k = a + "," + b; if (ok.has(k) || !walk(a, b)) continue; ok.add(k); q.push([a, b]); }
    }
    // (Seeds standing in something solid still reach out of it: a sign on a wall, a TRAINER behind a counter.)
    for (const [x, z] of seeds) for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const a = x + dx, b = z + dz, k = a + "," + b; if (!ok.has(k) && walk(a, b)) { ok.add(k); q.push([a, b]); } }
    while (q.length) {
      const [x, z] = q.pop();
      for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { const a = x + dx, b = z + dz, k = a + "," + b; if (ok.has(k) || !walk(a, b)) continue; ok.add(k); q.push([a, b]); }
    }
    let cut = 0;
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) if (walk(x, z) && !ok.has(x + "," + z)) { rows[z][x] = "v"; cut++; }
  }
  const out = Object.assign({}, F, { rows: rows.map(r => r.join("")) });
  FIT_CUR = { id, F: out };
  return out;
}

var FIT_GRAVE, FIT_GRAVE_INIT;
// ---------- the POKéMON TOWER's gravestones (19:22: "different colors ... some might have flowers, some could have
// candles, and then they can be different shades ... a couple can be different styles") ----------
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // The stones: grey, sandstone, rose granite, slate blue, dark and pale marble; each its own face, top and side.
  FIT_GRAVE_INIT = [["#9A9AA8", "#B6B6C4", "#76768A"], ["#C6B08A", "#DCCAA6", "#A08A66"], ["#B8949E", "#CEACB4", "#90707A"],
    ["#7E8CA8", "#9AA8C0", "#5E6C88"], ["#6E6E7E", "#8A8A9A", "#50505E"], ["#DADAE2", "#EEEEF2", "#B0B0BE"]];
  FIT_GRAVE = FIT_GRAVE_INIT;
  FIT_GRAVE.forEach(([s, t, d], i) => {
    add("grS" + i, 16, 16, (P, r) => fitPaint(P, r, hx(s), { drift: 0.03 }));
    add("grT" + i, 16, 16, (P, r) => fitPaint(P, r, hx(t), { drift: 0.02 }));
    add("grF" + i, 16, 16, (P, r) => {
      fitPaint(P, r, hx(s), { drift: 0.03 });
      P.disc(7.5, 6.5, 3.2, hx(t)); P.disc(7.5, 6.5, 2.4, hx(d)); P.rect(4, 6, 8, 1, hx(t)); P.disc(7.5, 6.5, 0.9, hx(t));
      P.rect(4, 11, 8, 1, hx(d)); P.rect(5, 13, 6, 1, hx(d));
    }, { fit: true });
  });
  add("grWax", 16, 16, (P, r) => fitPaint(P, r, hx("#F4EEDC"), { drift: 0.02 }));
  add("grFlame", 8, 8, P => { P.rect(0, 0, 8, 8, hx("#FFD866")); P.rect(2, 2, 4, 4, hx("#FFF4C0")); }, { glow: 1 });
  add("grBowl", 16, 16, (P, r) => fitPaint(P, r, hx("#E8E2D6"), { drift: 0.02 }));
  for (const [k, c] of [["grPetR", "#E8506A"], ["grPetY", "#F6C83E"], ["grPetP", "#F49AC0"], ["grPetW", "#F6F4EE"], ["grPetB", "#7AA8F0"], ["grPetL", "#B48CE8"]])
    add(k, 8, 8, (P, r) => fitPaint(P, r, hx(c), { drift: 0.04 }));
  add("grLeaf", 8, 8, (P, r) => fitPaint(P, r, hx("#5AA84A"), { drift: 0.05 }));
  add("grBerry", 8, 8, (P, r) => fitPaint(P, r, hx("#D84848"), { drift: 0.04 }));
  add("grBerry2", 8, 8, (P, r) => fitPaint(P, r, hx("#5A6AD8"), { drift: 0.04 }));
}
const GR_PETALS = ["grPetR", "grPetY", "grPetP", "grPetW", "grPetB", "grPetL"];
function fitGrave(R, G, a, b, id) {
  const h = hashStr(id + ":" + a + "," + b), pal = h % FIT_GRAVE.length, style = (h >> 4) % 3, gift = (h >> 7) % 5;
  const S = "grS" + pal, T = "grT" + pal, Fc = "grF" + pal;
  // A low plinth a block.
  G.box(a + 0.06, 0, b + 0.08, a + 0.94, 0.14, b + 0.92, { all: S, top: T });
  if (style === 0) {
    // Stepped: the stone at the back, its top stepped in twice.
    G.box(a + 0.16, 0.14, b + 0.2, a + 0.84, 0.95, b + 0.46, { all: S, s: Fc, top: T });
    G.box(a + 0.24, 0.95, b + 0.22, a + 0.76, 1.08, b + 0.44, { all: S, top: T });
    G.box(a + 0.36, 1.08, b + 0.24, a + 0.64, 1.15, b + 0.42, { all: S, top: T });
  } else if (style === 1) {
    // Rounded: the stone's top curving over (in blocks narrowing).
    G.box(a + 0.18, 0.14, b + 0.2, a + 0.82, 0.88, b + 0.44, { all: S, s: Fc, top: T });
    for (const [i, y0, y1] of [[0.05, 0.88, 0.98], [0.12, 0.98, 1.05], [0.2, 1.05, 1.1]]) G.box(a + 0.18 + i, y0, b + 0.2, a + 0.82 - i, y1, b + 0.44, { all: S, top: T });
  } else {
    // A low slab laid over the plinth, a small marker standing at its head.
    G.box(a + 0.12, 0.14, b + 0.16, a + 0.88, 0.3, b + 0.86, { all: S, top: T });
    G.box(a + 0.26, 0.3, b + 0.18, a + 0.74, 0.78, b + 0.34, { all: S, s: Fc, top: T });
  }
  const fz = style === 2 ? 0.62 : 0.66, y = style === 2 ? 0.3 : 0.14;
  if (gift === 1 || gift === 2) {
    // Flowers in a little vase.
    G.box(a + 0.42, y, b + fz - 0.06, a + 0.58, y + 0.18, b + fz + 0.1, { all: "potWhite" });
    const p1 = GR_PETALS[(h >> 9) % 6], p2 = GR_PETALS[(h >> 12) % 6];
    for (const [fx, ffz, col] of [[0.44, -0.02, p1], [0.53, 0.05, p2], [0.5, -0.05, p1]]) {
      G.box(a + fx, y + 0.18, b + fz + ffz, a + fx + 0.02, y + 0.33, b + fz + ffz + 0.02, "grLeaf");
      G.box(a + fx - 0.04, y + 0.31, b + fz + ffz - 0.04, a + fx + 0.06, y + 0.39, b + fz + ffz + 0.06, col);
    }
  } else if (gift === 3) {
    // Candles: two (or one) white candles, lit.
    const n = 1 + ((h >> 10) & 1);
    for (let i = 0; i < n; i++) {
      const cx = a + (n === 1 ? 0.5 : 0.36 + i * 0.28), ch = 0.16 + i * 0.05;
      G.box(cx - 0.04, y, b + fz - 0.04, cx + 0.04, y + ch, b + fz + 0.04, "grWax");
      G.box(cx - 0.015, y + ch, b + fz - 0.015, cx + 0.015, y + ch + 0.05, b + fz + 0.015, "grFlame");
    }
  } else if (gift === 4) {
    // An offering: a little bowl of berries.
    G.box(a + 0.38, y, b + fz - 0.08, a + 0.62, y + 0.06, b + fz + 0.12, "grBowl");
    for (const [bx, bz, k] of [[0.42, -0.04, "grBerry"], [0.5, 0.02, "grBerry2"], [0.54, -0.04, "grBerry"]]) G.box(a + bx, y + 0.06, b + fz + bz, a + bx + 0.06, y + 0.11, b + fz + bz + 0.06, k);
  }
}

// ---------- the S.S. ANNE's cabins: beds of their own, pictures ("Just make sure each has a little bit of character") ----------
{
  const add = (k, c, o = {}) => { TEXDEF[k] = [16, 16, (P, r) => { fitPaint(P, r, hx(c), { drift: 0.03 }); if (o.hem) { P.rect(0, 12, 16, 2, hx(o.hem)); } if (o.check) for (let y = 0; y < 16; y += 4) for (let x = (y / 4) % 2 * 4; x < 16; x += 8) P.rect(x, y, 4, 4, shadec(hx(c), -0.05)); }]; MATDEF[k] = { tex: k, per: 1 }; };
  add("qRose", "#E4899A", { hem: "#F4D2D8" }); add("qSage", "#8EB88A", { hem: "#DCEAD6" }); add("qNavy", "#40609E", { hem: "#D8E0F0" });
  add("qGold", "#E2B04C", { check: true }); add("qCream", "#F0E6CE", { hem: "#C89A62" }); add("qSky", "#86B8E4", { check: true });
  add("padHead", "#7E5A8E"); add("padHead2", "#3E6E86");
}
const CABIN_BEDS = [["qRose", "walnut", 0], ["qSage", "pale", 1], ["qNavy", "honey", 2], ["qGold", "walnut", 1], ["qCream", "oak", 0], ["qSky", "pale", 2]];
function fitCabinBed(R, p, id) {
  // (Each cabin on a deck its own bed: the cabin's number picks it, each deck starting further along.)
  const m = /_(1f|2f|b1f)_room(\d)/.exec(id), pick = m ? (+m[2] + { "1f": 0, "2f": 2, "b1f": 4 }[m[1]]) % CABIN_BEDS.length : hashStr(id) % CABIN_BEDS.length;
  const G = R.geo, { x0, x1, z0, z1 } = p, h = hashStr(id + ":" + x0 + "," + z0), [q, wood, style] = CABIN_BEDS[pick];
  const F = FIT_CUR && FIT_CUR.id === id ? FIT_CUR.F : null;
  const at = (x, z) => !F || x < 0 || z < 0 || x >= F.W || z >= F.D ? "v" : F.rows[z][x];
  // The head at the end with the wall (else the north).
  const headN = !FR_WALK.includes(at(x0, z0 - 1)) || FR_WALK.includes(at(x0, z1));
  const hz0 = headN ? z0 : z1 - 0.16, hz1 = headN ? z0 + 0.16 : z1;
  // The frame, the mattress, the pillow, the quilt over the foot.
  G.box(x0 + 0.04, 0, z0 + 0.04, x1 - 0.04, 0.38, z1 - 0.04, { all: wood });
  G.box(x0 + 0.08, 0.38, z0 + 0.08, x1 - 0.08, 0.62, z1 - 0.08, "linen");
  const pz0 = headN ? z0 + 0.22 : z1 - 0.62, pz1 = pz0 + 0.4;
  G.box(x0 + 0.16, 0.62, pz0, x1 - 0.16, 0.8, pz1, "linen");
  const qz0 = headN ? z0 + 0.72 : z0 + 0.02, qz1 = headN ? z1 - 0.02 : z1 - 0.72;
  G.box(x0 + 0.02, 0.5, qz0, x1 - 0.02, 0.68, qz1, { all: q, top: q });
  // The headboard: a tall panel, a low board and rail, or padded.
  if (style === 0) G.box(x0 + 0.04, 0, hz0, x1 - 0.04, 1.3, hz1, { all: wood });
  else if (style === 1) { G.box(x0 + 0.04, 0, hz0, x1 - 0.04, 0.85, hz1, { all: wood }); G.box(x0 + 0.04, 1.05, hz0 + 0.03, x1 - 0.04, 1.12, hz1 - 0.03, "gold"); for (const x of [x0 + 0.08, x1 - 0.14]) G.box(x, 0.85, hz0 + 0.03, x + 0.06, 1.12, hz1 - 0.03, "gold"); }
  else { G.box(x0 + 0.04, 0, hz0, x1 - 0.04, 0.6, hz1, { all: wood }); G.box(x0 + 0.08, 0.6, hz0 + 0.02, x1 - 0.08, 1.2, hz1 - 0.02, { all: h % 2 ? "padHead" : "padHead2" }); }
  R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: [["A neatly made bed.", "The bed's been turned down for the night.", "A comfy-looking bunk. A ticket stub on the pillow."][h % 3]] });
  R.item("Beds", "Each cabin's bed its own: rose, sage, navy, gold-checked, cream or sky quilts; a tall wooden headboard, a low one with a brass rail, or a padded one. Against the wall.");
}
// The ship's walls: pictures (two a cabin, along the corridors every few blocks), a porthole in each cabin.
function fitShipWalls(R, id) {
  const F = FIT_CUR && FIT_CUR.id === id ? FIT_CUR.F : null;
  if (!F) return;
  const G = R.geo, W = F.W, D = F.D, at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "v" : F.rows[z][x];
  const walk = (x, z) => FR_WALK.includes(at(x, z)) || F.warps.some(w => w.x === x && w.z === z);
  const doors = new Set(F.warps.map(w => w.x + "," + w.z));
  const cabin = /room\d/.test(id), spots = [];
  // Back walls: a wall block with the floor just south of it (no door in it, none under it), the wall's face at z.
  for (let z = 1; z < D; z++) for (let x = 0; x < W; x++) {
    if (at(x, z - 1) !== "w" || !walk(x, z) || doors.has(x + "," + z) || doors.has(x + "," + (z - 1))) continue;
    if (!cabin && x % 4 === 2) continue;
    spots.push([x, z]);
  }
  if (!spots.length) return;
  const h = hashStr(id), pics = ["picSunset", "paintingHills", "picStars", "painting", "photos", "picSnap"];
  const want = cabin ? 2 : Math.max(1, Math.floor(spots.length / 7)), put = [];
  for (let i = 0; i < spots.length && put.length < want; i++) {
    const s = spots[(i * 5 + h) % spots.length];
    if (put.some(p => Math.abs(p[0] - s[0]) < (cabin ? 2 : 5) && p[1] === s[1])) continue;
    put.push(s);
  }
  put.forEach(([x, z], i) => {
    const pic = pics[(h + i * 2) % pics.length];
    G.box(x + 0.12, 1.22, z, x + 0.88, 1.88, z + 0.05, { all: "walnut", s: null });
    G.box(x + 0.18, 1.28, z + 0.05, x + 0.82, 1.82, z + 0.06, { s: pic, all: null });
    R.act([x - 0.2, z, x + 1.2, z + 0.8], { label: "Look", lines: [["A picture of the S.S. ANNE at sea.", "A photo of VERMILION's harbour at sunset.", "A painting of WINGULL over the waves."][i % 3]] });
  });
  if (cabin) {
    // A porthole: on the back wall, a block not used by a picture.
    const free = spots.find(s => !put.some(p => p[0] === s[0] && p[1] === s[1]));
    if (free) G.box(free[0] + 0.15, 1.25, free[1], free[0] + 0.85, 1.95, free[1] + 0.03, { s: "ssPort", all: null });
  }
  R.item("Pictures", cabin ? "Two pictures on each cabin's wall, a porthole by them." : "Pictures along the corridor walls, between the portholes.");
}
for (const id of Object.keys(BUILD)) if (/^ss_anne_/.test(id) && !/kitchen|captain/.test(id)) fitTweak(id, null, R => fitShipWalls(R, id));

// ---------- the S.S. ANNE's kitchen, as FireRed has it ----------
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("kSteelTop", 16, 16, (P, r) => { fitPaint(P, r, hx("#B4B4C8"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#C8C8D8")); });
  add("kSteel", 16, 16, (P, r) => { fitPaint(P, r, hx("#9092A8"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#A8AABE")); P.rect(0, 14, 16, 2, hx("#6E7086")); for (const x of [0, 8]) P.rect(x, 2, 1, 12, hx("#80829A")); });
  add("kPlate", 16, 16, (P, r) => { P.rect(0, 0, 16, 16, hx("#F6F6F2")); P.disc(7.5, 7.5, 5, hx("#E8E6DE")); P.disc(7.5, 7.5, 2.2, hx("#F2C84A")); }, { fit: true });
  add("kPlateSide", 16, 16, (P, r) => { P.rect(0, 0, 16, 16, hx("#E6E6E0")); for (let y = 0; y < 16; y += 3) P.rect(0, y, 16, 1, hx("#C8C8C0")); });
  add("kBurner", 16, 16, (P, r) => { fitPaint(P, r, hx("#C9CAD8"), { drift: 0.02 }); for (const [cx, cy] of [[4.5, 4.5], [11.5, 4.5], [4.5, 11.5], [11.5, 11.5]]) { P.disc(cx, cy, 2.8, hx("#5E5E70")); P.disc(cx, cy, 1.6, hx("#8E8EA0")); P.disc(cx, cy, 0.6, hx("#5E5E70")); } }, { fit: true });
  add("kBasin", 16, 16, (P, r) => { fitPaint(P, r, hx("#8E90A4"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#6E7084")); P.disc(8, 9, 1.2, hx("#5A5C70")); }, { fit: true });
  add("kCan", 16, 16, (P, r) => { fitPaint(P, r, hx("#9CA4B2"), { drift: 0.03 }); for (let x = 1; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#828A98")); P.rect(0, 0, 16, 2, hx("#BAC2CE")); });
  add("kLid", 16, 16, (P, r) => { fitPaint(P, r, hx("#B6BECA"), { drift: 0.02 }); P.rect(6, 6, 4, 4, hx("#D6DCE4")); });
}
function kStack(G, x, z, n) { for (let i = 0; i < n; i++) G.box(x - 0.3, 1.0 + i * 0.045, z - 0.3, x + 0.3, 1.04 + i * 0.045, z + 0.3, { all: "kPlateSide", top: "kPlate" }); }
fitTweak("ss_anne_kitchen", null, R => {
  const G = R.geo;
  // The three islands, two blocks across, rows 4 to 8: steel, their tops pale; stacks of plates, the sinks.
  for (const x0 of [3, 7, 11]) {
    G.box(x0 + 0.04, 0, 4.04, x0 + 1.96, 1.0, 8.96, { all: "kSteel", top: "kSteelTop" });
    R.act([x0 - 0.3, 3.7, x0 + 2.3, 9.3], { label: "Look", lines: [["Stacks of clean plates.", "The sink's full of dishes waiting to be washed.", "Plates, ready for the dining room."][(x0 - 3) / 4]] });
  }
  for (const [x, z, n] of [[3.5, 5.5, 4], [3.5, 6.5, 3], [7.5, 4.5, 4], [7.5, 5.5, 4], [7.5, 7.5, 2], [11.5, 4.5, 4], [11.5, 5.5, 3]]) kStack(G, x, z, n);
  for (const x0 of [3, 11]) { G.box(x0 + 0.2, 0.96, 8.15, x0 + 1.8, 1.005, 8.85, { top: "kBasin", all: null }); G.box(x0 + 0.95, 1.0, 8.75, x0 + 1.05, 1.3, 8.85, "steel"); G.box(x0 + 0.95, 1.24, 8.5, x0 + 1.05, 1.3, 8.85, "steel"); }
  // The bins down the east wall (in FireRed one hides a GREAT BALL: look in them).
  for (const z of [4, 6, 8]) {
    G.box(14.18, 0, z + 0.18, 14.82, 0.85, z + 0.82, { all: "kCan", top: "kLid" }); G.box(14.14, 0.85, z + 0.14, 14.86, 0.93, z + 0.86, { all: "kLid" });
    R.act([13.7, z - 0.3, 15, z + 1.3], { label: "Look", lines: ["A bin. Nothing in it but scraps."] });
  }
  // The long counter across the south: its burners at either end, plates set out in the middle.
  G.box(1, 0, 11, 15, 1.0, 13, { all: "kSteel", top: "kSteelTop", n: "kSteel" });
  for (const x of [3, 4, 11, 12]) G.box(x + 0.05, 1.0, 11.1, x + 0.95, 1.02, 11.9, { top: "kBurner", all: "steel" });
  for (const x of [6, 7, 8, 9]) kStack(G, x + 0.5, 11.5, 1);
  R.act([2.7, 10.4, 5.3, 11.4], { label: "Look", lines: ["The stove. Something smells good!"] });
  R.act([10.7, 10.4, 13.3, 11.4], { label: "Look", lines: ["The stove. A big pot of stew is bubbling away."] });
  R.act([5.7, 10.4, 10.3, 11.4], { label: "Look", lines: ["Plates set out, ready to go up to the dining room."] });
  R.item("The kitchen", "FireRed's: three steel islands (stacks of plates, two sinks), three bins down the east wall, the long counter across the south with its stoves and the plates set out.");
});

// ---------- the time of day inside (19:22: "during like the daytime, would you have the lights off?") ----------
// By day (morning and day) a room with windows is lit by them, its lamps off; in the evening and at night the lamps
// are on and the room's light is theirs: warm and bright. A room with no windows keeps its lamps on.
const FIT_LAMPS = ["lampShade", "lampWhite", "ugLamp", "seaLantern"];
const FIT_TOD = {
  // By day: the windows' light, a touch cool and crisp, the shadows clear; the lamps off.
  morning: { sun: 0.6, sunC: 0xfff0d8, hemi: 0.72, hemiC: 0xfff4e6, lamps: false },
  day: { sun: 0.64, sunC: 0xffffff, hemi: 0.72, hemiC: 0xf4f7fb, lamps: false },
  // Evening and night: the lamps on, the room's light theirs: warm, bright and even.
  evening: { sun: 0.42, sunC: 0xffd6aa, hemi: 0.84, hemiC: 0xffecd0, lamps: true },
  night: { sun: 0.38, sunC: 0xffe2b8, hemi: 0.88, hemiC: 0xffefd6, lamps: true },
};
function fitTimeLight(room, tod) {
  const plain = FIT && room && !room.outside && !room.night && !room.haze && !room.dark;
  if (!plain) { sun.color.setHex(0xffffff); for (const k of FIT_LAMPS) { const m = matCache[k]; if (m && MATDEF[k]) m.emissiveIntensity = MATDEF[k].glow; } return; }
  const L = FIT_TOD[tod] || FIT_TOD.day, windows = !!(room.pools && room.pools.length);
  sun.intensity = L.sun; sun.color.setHex(L.sunC); hemi.intensity = L.hemi; hemi.color.setHex(L.hemiC);
  const on = L.lamps || !windows;
  for (const k of FIT_LAMPS) { const m = matCache[k]; if (m && MATDEF[k]) m.emissiveIntensity = on ? MATDEF[k].glow * 1.15 : 0.04; }
  room._lampsOn = on;
}
// The top of the S.S. ANNE's block of cabins, as FireRed shows it: plain white (nothing to see there).
TEXDEF.ssTop = [16, 16, (P, r) => fitPaint(P, r, hx("#EEEBE2"), { drift: 0.03 })];
MATDEF.ssTop = { tex: "ssTop", per: 1 };

// ---------- BILL's SEA COTTAGE, fitted out (19:22: "the machinery ... get it aligned. I would say the floor as well
// ... fill it out with a little bit more decor ... like a carpet, maybe plants") ----------
// FireRed's room, a tile a block, everything on the block grid in the fit-out's soft materials: the two pods three
// blocks across, the pipe between them with its panel, his two PCs in the back corners, the crates either side of
// the panel, a bank of machines down the east wall, the table and its computer; a rug, plants and pictures.
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("scMetal", 16, 16, (P, r) => { fitPaint(P, r, hx("#DCE2E6"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#EEF2F4")); P.rect(0, 15, 16, 1, hx("#B8C0C6")); });
  add("scMetalTop", 16, 16, (P, r) => fitPaint(P, r, hx("#C8D0D6"), { drift: 0.02 }));
  add("scGlass", 16, 16, P => { P.rect(0, 0, 16, 16, hx("#A8E4C4")); P.rect(3, 0, 2, 16, hx("#E0F8EA")); P.rect(7, 0, 1, 16, hx("#C8F0DA")); }, { fit: true, alpha: 0.45, double: true });
  add("scPipeFit", 16, 16, (P, r) => { fitPaint(P, r, hx("#8EE0A8"), { drift: 0.02 }); P.rect(0, 0, 16, 3, hx("#C4F6D2")); P.rect(0, 13, 16, 3, hx("#64B67E")); }, { glow: 0.25 });
  add("scPanel", 16, 16, (P, r) => { fitPaint(P, r, hx("#3E4C60"), { drift: 0.02 }); P.rect(2, 3, 12, 5, hx("#7CE0B0")); for (let i = 0; i < 4; i++) P.rect(2 + i * 3, 10, 2, 2, hx(["#F5D76E", "#E2665A", "#7CF5B0", "#8EC8F4"][i])); }, { fit: true, glow: 0.3 });
  add("scMachine", 16, 32, (P, r) => {
    fitPaint(P, r, hx("#E4E8EC"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#F2F4F6")); P.rect(0, 30, 16, 2, hx("#B8C0C6"));
    P.rect(2, 4, 12, 8, hx("#3E4C60")); P.rect(3, 5, 10, 6, hx("#8EE0B8")); for (let i = 0; i < 3; i++) P.rect(3 + i * 4, 15, 2, 2, hx(["#F5D76E", "#7CF5B0", "#E2665A"][i]));
    P.rect(3, 20, 10, 1, hx("#B8C0C6")); P.rect(3, 23, 10, 1, hx("#B8C0C6"));
  }, { fit: true });
  add("scCrateFit", 16, 16, (P, r) => { fitPaint(P, r, hx("#E8C46A"), { drift: 0.03 }); P.rect(0, 0, 16, 1, hx("#F6DA8E")); P.rect(0, 15, 16, 1, hx("#C49E46")); P.rect(0, 0, 1, 16, hx("#D6B05A")); P.rect(15, 0, 1, 16, hx("#D6B05A")); });
  add("scPCFront", 16, 16, (P, r) => { fitPaint(P, r, hx("#E8ECEE"), { drift: 0.01 }); P.rect(2, 2, 12, 9, hx("#2C3A4C")); P.rect(3, 3, 10, 7, hx("#78D0A0")); P.rect(4, 12, 8, 2, hx("#B8C0C6")); }, { fit: true, glow: 0.2 });
  RUGS.rugCottage = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#6E9CC4" : "#B4D2EA";
  FIT_TEX.scBoards = [64, 64, (P, r) => fitTiles(P, r, hx("#E2CFA6"), hx("#D8C396"), 2), 4];
}
{
  const bh = BUILD.bills_house;
  BUILD.bills_house = function () {
    if (!FIT) return bh.call(this);
    const W = 15, D = 11;
    const R = new Room("bills_house", { name: "Sea Cottage", floor: "Route 25", W, D, H: 3.2 });
    const G = R.geo;
    R._floors = [[0, 0, W, D - 1]]; R._boxes = []; R.balls = []; R._spin = []; R._mons = [];
    for (let z = 0; z < D - 1; z++) for (let x = 0; x < W; x++) G.box(x, -1.2, z, x + 1, 0, z + 1, { top: "scBoards", all: "dsFloorEdge", bottom: null });
    // The walls: back and sides, a block thick, three tall, the blue band at their foot.
    G.box(-1, 0, -1, W + 1, 1, 0, { all: "scWall" }); G.box(-1, 0, -1, W + 1, 3, 0, { all: "scWall", top: "cap" });
    for (const [x0, x1] of [[-1, 0], [W, W + 1]]) G.box(x0, 0, 0, x1, 3, D - 1, { all: "scWall", top: "cap" });
    for (const [x0, x1] of [[-1, 6], [9, W + 1]]) G.box(x0, 0, D - 1, x1, 0.7, D, { all: "walnut", top: "honey", bottom: null, n: null });
    R.solid(-2, -2, W + 2, 0); R.solid(-2, 0, 0, D); R.solid(W, 0, W + 2, D); R.solid(-2, D - 1, 6, D + 2); R.solid(9, D - 1, W + 2, D + 2);
    // The pods, three blocks across and three deep: a steel base, green glass, a steel cap.
    for (const x0 of [2, 10]) {
      G.box(x0, 0, 0, x0 + 3, 0.5, 3, { all: "scMetal", top: "scMetalTop" });
      G.box(x0 + 0.2, 0.5, 0.2, x0 + 2.8, 2.5, 2.8, { all: "scGlass", bottom: null, top: null });
      G.box(x0, 2.5, 0, x0 + 3, 2.9, 3, { all: "scMetal", top: "scMetalTop" });
      for (const [px, pz] of [[0, 0], [2.8, 0], [0, 2.8], [2.8, 2.8]]) G.box(x0 + px, 0.5, pz, x0 + px + 0.2, 2.5, pz + 0.2, "scMetal");
      R.solid(x0, 0, x0 + 3, 3);
    }
    // The pipe between them (glowing), its control panel under it, a crate each side.
    G.box(5, 2.1, 1.3, 10, 2.45, 1.7, { all: "scPipeFit" }); G.box(5, 0.25, 1.3, 10, 0.5, 1.7, { all: "scPipeFit" });
    G.box(7, 0, 0, 8, 1.2, 1, { all: "scMetal", s: "scPanel", top: "scMetalTop" });
    for (const x of [5, 9]) G.box(x + 0.05, 0, 0.05, x + 0.95, 0.9, 0.95, { all: "scCrateFit" });
    R.solid(5, 0, 10, 1.8);
    R.act([2.0, 2.8, 13, 4.0], { label: "Look", lines: ["The CELL SEPARATOR: two teleporter pods, a glowing pipe between them."] });
    R.item("Cell Separator", "Two teleporter pods three blocks across, green glass on steel; the glowing pipe between them, the panel under it, a crate each side.");
    // His two PCs, in the back corners: a desk two blocks wide each, the screen on it.
    for (const x0 of [0, 13]) {
      G.box(x0, 0, 0, x0 + 2, 0.85, 1, { all: "scMetal", top: "scMetalTop" });
      G.box(x0 + 0.5, 0.85, 0.15, x0 + 1.5, 1.6, 0.45, { all: "scMetal", s: "scPCFront" });
      R.solid(x0, 0, x0 + 2, 1);
      R.act([x0 - 0.2, 0, x0 + 2.2, 1.6], { label: "Use", pc: true, lines: ["BILL's PC: his rare POKéMON, filed away."] });
    }
    // The machines down the east wall: four, a block each, two high.
    for (let z = 4; z < 8; z++) G.box(W - 1, 0, z, W, 2, z + 1, { all: "scMetal", w: "scMachine", top: "scMetalTop" });
    R.solid(W - 1, 4, W, 8);
    R.act([W - 1.8, 3.8, W, 8.2], { label: "Look", lines: ["Machines, humming. BILL's teleporter controls."] });
    // The rug, the table on it (his computer), plants, pictures.
    rug(R, 4, 4, 11, 8, "rugCottage");
    table(R, 4, 5, 6, 6, { top: "labDeskTop", side: "labDesk", leg: "steel", h: 0.85, name: "Table" });
    G.box(4.6, 0.85, 5.15, 5.4, 1.45, 5.4, { all: "scMetal", s: "scPCFront" });
    plant(R, 0.5, 8.5, 0.95, { pot: "potWhite", kind: "leafy", lines: ["A leafy plant in a white pot."] });
    slimPlant(R, 13.5, 8.5, 0.95, { pot: "potWhite", lines: ["A tall plant in a white pot."] });
    plant(R, 0.5, 3.5, 0.95, { pot: "potWhite", kind: "fern", lines: ["A fern in a white pot."] });
    framed(R, 4.2, 1.3, 1.4, 1.0, "picSunset", "w", { act: { label: "Look", lines: ["A photo of the cape at sunset."] } });
    framed(R, 6.4, 1.4, 1.2, 0.9, "photos", "w", { act: { label: "Look", lines: ["Photos of BILL with POKéMON from all over."] } });
    R.npcs.push({ name: "Bill", key: "bills_house_p0", x: 8.5, z: 5.5, yaw: FACE.s, look: { skin: "super_nerd" },
      talk: () => [{ text: "BILL: Look, bud, just check out some of my rare POKéMON on my PC!" }] });
    R._mons.push({ key: "pet:bills_house_clefairy", species: "clefairy", x: 11.5, z: 6.5, yaw: FACE.s });
    doormat(R, 6, 9, { to: "town", mat: "matHouseV" });
    R.spawn = { x: 7.5, z: D - 1.6, yaw: FACE.n };
    R.item("Sea Cottage", "FireRed's room, a tile a block: pale tiles, cream walls with a blue band; a blue rug, plants, pictures.");
    return R;
  };
}

// ---------- LT. SURGE's GYM on FireRed's own grid (19:22: "people should be placed exactly where they should be") ----------
// FireRed's VermilionCity_Gym a tile a block (its row 2 is the room's first): his stage at the back between two poles,
// the wall across with the electric door in its middle, the fifteen cans in three rows, the statues, everyone where
// FireRed has them. (The game's switches and door follow this when it goes in.)
{
  const vg = BUILD.vermilion_gym;
  BUILD.vermilion_gym = function () {
    if (!FIT) return vg.call(this);
    const W = 11, D = 19;
    const R = new Room("vermilion_gym", { name: "Vermilion Gym", floor: "Vermilion City", W, D, H: 5 });
    shell(R, { paper: "surgeWall", floor: "surgeFloor", door: [4, 7], entry: true, skirt: "surgeHazard", ledge: "steel" });
    band(R, 0.3, "surgeHazard", 0.2); band(R, 1.4, "steel", 0.08);
    const G = R.geo;
    // The north room's side walls (FireRed's x 0 and 10, its rows 2 to 5).
    for (const x of [0, 10]) { G.box(x, 0, 0, x + 1, 3, 4, { all: "surgeWall", top: "steel" }); R.solid(x, 0, x + 1, 4); }
    // The wall across (FireRed's rows 6 and 7), the electric door in it (x 4 to 7) between its two machines.
    for (const [x0, x1] of [[0, 3], [8, W]]) { G.box(x0, 0, 4, x1, 3, 6, { all: "surgeWall", top: "steel" }); R.solid(x0, 4, x1, 6); }
    for (const x of [3, 7]) { G.box(x, 0, 4, x + 1, 3, 6, { all: "steel", s: "surgeHazard", top: "steel" }); R.solid(x, 4, x + 1, 6); }
    // (In the game the door is real blocks at row 4, put there or taken away for you: Game.surgeDoor.)
    G.box(4, 0, 4.4, 7, 2.0, 4.6, { all: "surgeBarrierView", top: null, bottom: null });
    R.act([3.6, 5.6, 7.4, 6.6], { label: "Look", lines: ["An electric door. It's locked: two switches open it."] });
    // His stage (FireRed's x 4 to 6, rows 2 and 3) between the two poles.
    G.box(4, 0, 0, 7, 0.12, 2, { top: "surgeHazard", all: "steel", bottom: null });
    R.ramps.push({ rect: [4, 0, 7, 2], h: () => 0.12 });
    for (const x of [3, 7]) { G.box(x + 0.15, 0, 0.15, x + 0.85, 2.6, 0.85, { all: "surgeHazard", top: "steel" }); R.solid(x, 0, x + 1, 1); }
    // The generators on the back wall either side.
    for (const x of [1, 8]) G.box(x, 0.4, -0.3, x + 2, 2.2, 0.1, { all: "steel", s: "surgeHazard" });
    R.act([0.8, 0, 3.2, 1.0], { label: "Look", lines: ["A generator, humming."] }); R.act([7.8, 0, 10.2, 1.0], { label: "Look", lines: ["A generator, humming."] });
    // The cans: FireRed's x 1, 3, 5, 7 and 9, rows 10, 12 and 14.
    let i = 0;
    for (const z of [8, 10, 12]) for (const x of [1, 3, 5, 7, 9]) {
      const cx = x + 0.5, cz = z + 0.5;
      G.box(cx - 0.38, 0, cz - 0.38, cx + 0.38, 0.95, cz + 0.38, { all: "trashCan", top: "trashLid" });
      G.box(cx - 0.42, 0.95, cz - 0.42, cx + 0.42, 1.05, cz + 0.42, { all: "trashLid" });
      R.solid(x, z, x + 1, z + 1);
      R.act([x - 0.3, z - 0.3, x + 1.3, z + 1.3], { label: "Look", use: "can:" + i, lines: ["Nope! There's only trash here.", "(In the game: two of the cans hide LT.SURGE's switches, the second right next to the first.)"] });
      i++;
    }
    gymStatue(R, 3.5, 15.5, ["VERMILION CITY POKéMON GYM", "LEADER: LT.SURGE", "WINNING TRAINERS: {rival}"]);
    gymStatue(R, 7.5, 15.5, ["VERMILION CITY POKéMON GYM", "LEADER: LT.SURGE", "WINNING TRAINERS: {rival}"]);
    R.npcs.push({ name: "Leader Lt. Surge", key: "vermilion_gym_0", role: "leader", x: 5.5, z: 0.5, y: 0.12, yaw: FACE.s, look: { skin: "leader_lt_surge" },
      talk: () => [{ text: "Hey, kid! What do you think you're doing here?" }] });
    R.npcs.push({ name: "Gentleman Tucker", key: "vermilion_gym_1", role: "trainer", x: 7.5, z: 6.5, yaw: FACE.w, look: { skin: "gentleman" },
      talk: () => [{ text: "I'm a lightweight, but I'm good with electricity!" }] });
    R.npcs.push({ name: "Engineer Baily", key: "vermilion_gym_2", role: "trainer", x: 2.5, z: 9.5, yaw: FACE.e, look: { skin: "super_nerd" },
      talk: () => [{ text: "This is no place for kids!" }] });
    R.npcs.push({ name: "Sailor Dwayne", key: "vermilion_gym_3", role: "trainer", x: 8.5, z: 11.5, yaw: FACE.w, look: { skin: "sailor" },
      talk: () => [{ text: "This is no place for kids!" }] });
    R.npcs.push({ name: "Gym Guide", key: "vermilion_gym_p0", x: 4.5, z: 15.5, yaw: FACE.s, look: { skin: "gym_guide" },
      talk: () => [{ text: "Yo! Champ in making! LT.SURGE has a nickname. People refer to him as the Lightning American!" }] });
    doormat(R, 4, 7, { to: "town", mat: "matHouseV" });
    R.spawn = { x: 5.5, z: D - 1.5, yaw: FACE.n };
    R.item("FireRed's grid", "A tile a block, as FireRed's VermilionCity_Gym: everyone, the cans, the statues and the door where FireRed has them.");
    R.item("Electric door", "In the game it's real blocks, put there or taken away for you: look in the cans for the two switches and it opens.");
    return R;
  };
}

// ---------- (20:37) The MART's drinks coolers and chest freezers: glass you can see into ----------
// ("where you've got the freezers again, I think that could be a glass texture door and actually feel like there's a
// little bit of depth to it") A cooler a block deep: white cabinet, a lit back, four shelves of drinks (bottles and
// cans in their colours) set back inside, the glass doors at the front a block each in steel frames with a handle, a
// blue band and sign along its top. A chest freezer: a white tub, its stock (tubs, boxes, lollies) down inside, a
// glass lid over it in a steel frame.
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  add("coolBack", 16, 16, (P, r) => { fitPaint(P, r, hx("#EEF6FA"), { drift: 0.02 }); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#DCEAF2")); }, { glow: 0.45 });
  add("coolGlass", 16, 16, P => { P.rect(0, 0, 16, 16, hx("#CFEAF6")); for (let i = 0; i < 16; i++) if ((i % 8) < 2) { P.set(i, 15 - i, hx("#FFFFFF")); if (i + 1 < 16) P.set(i + 1, 15 - i, hx("#F2FAFE")); } P.rect(0, 0, 16, 1, hx("#E6F4FA")); }, { fit: true, alpha: 0.22, double: true });
  add("coolFrame", 16, 16, (P, r) => { fitPaint(P, r, hx("#B8C2CC"), { drift: 0.02 }); P.rect(0, 0, 16, 1, hx("#D6DEE6")); });
  add("coolSign", 32, 8, P => { P.rect(0, 0, 32, 8, hx("#2F6FB8")); P.text("DRINKS", 4, 2, hx("#FFFFFF")); }, { fit: true });
  for (const [k, c, lab] of [["canRed", "#D84040", "#F4F4F4"], ["canBlue", "#3C7CD0", "#F4F4F4"], ["canYellow", "#F0C43C", "#3A8A3A"], ["canGreen", "#3FA060", "#F8F0C0"], ["canWater", "#9AD0F0", "#FFFFFF"]])
    add(k, 8, 16, (P, r) => { fitPaint(P, r, hx(c), { drift: 0.03 }); P.rect(0, 6, 8, 4, hx(lab)); P.rect(0, 0, 8, 1, shadec(hx(c), 0.1)); });
  add("iceTub", 16, 16, (P, r) => { fitPaint(P, r, hx("#F4E6C8"), { drift: 0.02 }); P.rect(0, 0, 16, 3, hx("#E88AA8")); });
  add("iceTub2", 16, 16, (P, r) => { fitPaint(P, r, hx("#C8E6F4"), { drift: 0.02 }); P.rect(0, 0, 16, 3, hx("#5AA0D8")); });
  add("iceLolly", 8, 8, (P, r) => fitPaint(P, r, hx("#F08A6A"), { drift: 0.03 }));
  add("iceLolly2", 8, 8, (P, r) => fitPaint(P, r, hx("#9AD86A"), { drift: 0.03 }));
  add("frostIn", 16, 16, (P, r) => { fitPaint(P, r, hx("#E4F2FA"), { drift: 0.03 }); for (let i = 0; i < 10; i++) P.set((i * 7) % 16, (i * 11) % 16, hx("#FFFFFF")); });
}
const CANS = ["canRed", "canBlue", "canYellow", "canGreen", "canWater"];
{
  const co = cooler;
  cooler = function (room, X, Z, f, w = 2) {
    if (!FIT) return co(room, X, Z, f, w);
    X = gridE(X); Z = gridE(Z); w = gridW(w);
    const F = room.F(X, Z, f), G = room.geo, d = 1, h = 2.6, r = rng(hashStr(room.id + X + "," + Z));
    // The cabinet: sides, top, kick, the lit back set in.
    F.box(0, 0, 0, w, 0.18, d, { all: "cupboardWhite", front: "blueTrim" });
    F.box(0, 0, 0, 0.06, h, d, "cupboardWhite"); F.box(w - 0.06, 0, 0, w, h, d, "cupboardWhite");
    F.box(0, h - 0.3, 0, w, h, d, { all: "cupboardWhite", front: "coolSign" });
    F.box(0.06, 0.18, 0, w - 0.06, h - 0.3, 0.06, "coolBack");
    // Four shelves, drinks on each, set back from the glass.
    const top = h - 0.3, n = 4, gap = (top - 0.18) / n;
    for (let i = 0; i < n; i++) {
      const y = 0.18 + i * gap;
      F.box(0.06, y, 0.06, w - 0.06, y + 0.03, d - 0.14, "coolFrame");
      let x = 0.1;
      const can = CANS[Math.floor(r() * CANS.length)], tall = r() < 0.5;
      while (x < w - 0.16) {
        const c = r() < 0.75 ? can : CANS[Math.floor(r() * CANS.length)], bh = tall ? 0.34 : 0.22;
        for (const z0 of [0.18, 0.5]) F.box(x, y + 0.03, z0, x + 0.12, y + 0.03 + bh, z0 + 0.12, { all: c, top: tall ? "goodCap" : c });
        if (tall) F.box(x + 0.035, y + 0.03 + bh, 0.53, x + 0.085, y + 0.08 + bh, 0.59, "goodCap");
        x += 0.16;
      }
    }
    // The doors: a block each, a steel frame round the glass, a handle.
    for (let i = 0; i < w; i++) {
      F.box(i + 0.04, 0.18, d - 0.08, i + 0.1, top, d - 0.02, "coolFrame"); F.box(i + 0.9, 0.18, d - 0.08, i + 0.96, top, d - 0.02, "coolFrame");
      F.box(i + 0.1, 0.18, d - 0.08, i + 0.9, 0.24, d - 0.02, "coolFrame"); F.box(i + 0.1, top - 0.06, d - 0.08, i + 0.9, top, d - 0.02, "coolFrame");
      F.box(i + 0.1, 0.24, d - 0.06, i + 0.9, top - 0.06, d - 0.05, { front: "coolGlass", back: "coolGlass", all: null });
      F.box(i + 0.8, 0.9, d - 0.02, i + 0.84, 1.7, d + 0.04, "steel");
    }
    F.solid(0, 0, w, d);
    room.act(F.rect(0, 0, w, d + 0.6), { label: "Look", lines: ["A cooler of drinks: FRESH WATER, SODA POP and LEMONADE, all nicely cold."] });
    room.item("Drinks cooler", "A block deep: glass doors in steel frames, the lit inside behind them, four shelves of bottles and cans set back from the glass.");
  };
  const cf = chestFreezer;
  chestFreezer = function (room, X, Z, f, w = 2) {
    if (!FIT) return cf(room, X, Z, f, w);
    X = gridE(X); Z = gridE(Z); w = gridW(w);
    const F = room.F(X, Z, f), G = room.geo, d = 1, h = 0.95, r = rng(hashStr(room.id + "cf" + X + "," + Z));
    // The tub: white walls, the frosty inside sunk well down, the blue rim.
    F.box(0, 0, 0, w, 0.12, d, { all: "blueTrim" });
    F.box(0, 0.12, 0, w, h - 0.08, 0.08, "cupboardWhite"); F.box(0, 0.12, d - 0.08, w, h - 0.08, d, "cupboardWhite");
    F.box(0, 0.12, 0, 0.08, h - 0.08, d, "cupboardWhite"); F.box(w - 0.08, 0.12, 0, w, h - 0.08, d, "cupboardWhite");
    F.box(0.08, 0.12, 0.08, w - 0.08, 0.4, d - 0.08, { top: "frostIn", all: null });
    F.box(0.08, 0.4, 0.08, 0.09, h - 0.08, d - 0.08, { e: "frostIn", all: null });
    // The stock: tubs and lollies, in rows.
    for (let x = 0.14; x < w - 0.2; x += 0.24) for (const z0 of [0.16, 0.42, 0.68]) {
      const k = r();
      if (k < 0.6) F.box(x, 0.4, z0, x + 0.2, 0.52 + r() * 0.06, z0 + 0.2, { all: k < 0.3 ? "iceTub" : "iceTub2" });
      else for (let j = 0; j < 3; j++) F.box(x + j * 0.06, 0.4, z0 + 0.02, x + j * 0.06 + 0.04, 0.44, z0 + 0.18, k < 0.8 ? "iceLolly" : "iceLolly2");
    }
    // The rim and the glass lid in its steel frame (two panes, sliding).
    F.box(0, h - 0.08, 0, w, h, 0.08, "blueTrim"); F.box(0, h - 0.08, d - 0.08, w, h, d, "blueTrim");
    F.box(0, h - 0.08, 0, 0.08, h, d, "blueTrim"); F.box(w - 0.08, h - 0.08, 0, w, h, d, "blueTrim");
    F.box(w / 2 - 0.03, h - 0.06, 0.08, w / 2 + 0.03, h - 0.01, d - 0.08, "steel");
    F.box(0.08, h - 0.04, 0.08, w - 0.08, h - 0.03, d - 0.08, { top: "coolGlass", bottom: "coolGlass", all: null });
    F.solid(0, 0, w, d);
    room.act(F.rect(0, 0, w, d + 0.6), { label: "Look", lines: ["A chest freezer: ice creams, lollies, and a tub of something called FROZEN BERRIES."] });
    room.item("Chest freezer", "White, a blue rim, a glass lid in a steel frame over tubs and lollies down inside.");
  };
}
