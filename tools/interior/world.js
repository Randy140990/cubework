// ---------- materials: each a pixel texture, laid at 16 pixels to a block (or stretched over one face: "fit") ----------
const MATDEF = {
  sqHoney: { tex: "sqHoney", per: 2 }, sqLight: { tex: "sqLight", per: 2 }, sqPale: { tex: "sqPale", per: 2 }, sqDark: { tex: "sqDark", per: 2 },
  mcOak: { tex: "mcOak", per: 1 }, mcSpruce: { tex: "mcSpruce", per: 1 }, mcBirch: { tex: "mcBirch", per: 1 }, mcPale: { tex: "mcPale", per: 1 }, mcDarkOak: { tex: "mcDarkOak", per: 1 }, mcAcacia: { tex: "mcAcacia", per: 1 },
  mcCream: { tex: "mcCream", per: 1 }, mcSky: { tex: "mcSky", per: 1 }, mcSage: { tex: "mcSage", per: 1 }, mcNavy: { tex: "mcNavy", per: 1 }, mcStripped: { tex: "mcStripped", per: 1 },
  mcQuartzCheck: { tex: "mcQuartzCheck", per: 2 }, mcLabTile: { tex: "mcLabTile", per: 2 }, mcLabWall: { tex: "mcLabWall", per: 1 }, mcCyan: { tex: "mcCyan", per: 1 },
  booksFull: { tex: "booksFull", per: [2, 0.5] }, binders: { tex: "binders", per: [2, 0.5] }, storeBoxes: { tex: "storeBoxes", per: [2, 0.5] }, shelfWhite: { tex: "shelfWhite", per: 1 },
  cupboardTeal: { tex: "cupboardTeal", fit: true }, tealPaint: { tex: "tealPaint", per: 1 }, tankGlass: { tex: "tankGlass", fit: true, alpha: 0.38, double: true }, redPlastic: { tex: "redPlastic", per: 1 },
  boardMap: { tex: "boardMap", fit: true }, boardLab: { tex: "boardLab", fit: true },
  labPanel: { tex: "labPanel", per: 1 }, vent: { tex: "vent", per: 1 }, lab: { tex: "lab", per: 1 }, mcBlueCheck: { tex: "mcBlueCheck", per: 2 },
  oak: { tex: "oak", per: 2 }, oakFloor: { tex: "oakFloor", per: 2 }, honeyFloor: { tex: "honeyFloor", per: 2 }, paleFloor: { tex: "paleFloor", per: 2 }, walnutFloor: { tex: "walnutFloor", per: 2 }, walnut: { tex: "walnut", per: 2 }, honey: { tex: "honey", per: 2 }, pale: { tex: "pale", per: 2 }, tread: { tex: "tread", per: 1 },
  checker: { tex: "checker", per: 1 }, tileWhite: { tex: "tileWhite", per: 1 }, tileBlue: { tex: "tileBlue", per: 1 }, splash: { tex: "splash", per: 1 },
  paperMum: { tex: "paperMum", per: 1 }, paperRoom: { tex: "paperRoom", per: 1 }, paperRival: { tex: "paperRival", per: 1 }, paperRivalRoom: { tex: "paperRivalRoom", per: 1 },
  cap: { tex: "cap", per: 1 }, paint: { tex: "paint", per: 1 }, mintPaint: { tex: "mintPaint", per: 1 }, counter: { tex: "counter", per: 1 }, darkTop: { tex: "darkTop", per: 1 },
  steel: { tex: "steel", per: 1 }, plastic: { tex: "plastic", per: 1 }, greyPlastic: { tex: "greyPlastic", per: 1 }, sofa: { tex: "sofa", per: 1 }, sofaL: { tex: "sofaL", per: 1 },
  redFabric: { tex: "redFabric", per: 1 }, blueFabric: { tex: "blueFabric", per: 1 }, cushion: { tex: "cushion", per: 1 }, linen: { tex: "linen", per: 1 },
  quilt: { tex: "quilt", per: 1 }, quiltBlue: { tex: "quiltBlue", per: 1 }, leaf: { tex: "leaf", per: 1, cut: true }, leafSolid: { tex: "leafSolid", per: 1 }, treeLeaf: { tex: "treeLeaf", per: 2 },
  pot: { tex: "pot", per: 1 }, potWhite: { tex: "potWhite", per: 1 }, soil: { tex: "soil", per: 1 }, gold: { tex: "gold", per: 1, shine: true }, silver: { tex: "silver", per: 1, shine: true },
  curtain: { tex: "curtain", per: 1 }, curtainBlue: { tex: "curtainBlue", per: 1 }, leather: { tex: "leather", per: 1 }, bark: { tex: "bark", per: 1 }, grass: { tex: "grass", per: 1 },
  path: { tex: "path", per: 1 }, stone: { tex: "stone", per: 1 }, siding: { tex: "siding", per: 1 }, labWall: { tex: "labWall", per: 1 }, roofRed: { tex: "roofRed", per: 1 },
  roofBlue: { tex: "roofBlue", per: 1 }, roofGrey: { tex: "roofGrey", per: 1 }, fence: { tex: "fence", per: 1 }, water: { tex: "water", per: 2 }, rubber: { tex: "rubber", per: 1 },
  books: { tex: "books", per: [2, 0.5] }, lampShade: { tex: "lampShade", per: 1, glow: 0.7 }, glass: { tex: "glass", per: 1, alpha: 0.3 },
  rugMum: { tex: "rugMum", fit: true }, rugRoom: { tex: "rugRoom", fit: true }, rugRival: { tex: "rugRival", fit: true }, doormat: { tex: "doormat", fit: true },
  cupboard: { tex: "cupboard", fit: true }, cupboardWhite: { tex: "cupboardWhite", fit: true }, wallCupboard: { tex: "wallCupboard", fit: true }, drawers: { tex: "drawers", fit: true },
  fridge: { tex: "fridge", fit: true }, oven: { tex: "oven", fit: true }, hob: { tex: "hob", fit: true }, sink: { tex: "sink", fit: true }, wardrobe: { tex: "wardrobe", fit: true },
  whiteboard: { tex: "whiteboard", fit: true }, boardRival: { tex: "boardRival", fit: true }, window: { tex: "window", fit: true, glow: 1 }, windowExt: { tex: "windowExt", fit: true, glow: 0.35 },
  doorExt: { tex: "doorExt", fit: true }, doorLab: { tex: "doorLab", fit: true }, painting: { tex: "painting", fit: true }, paintingHills: { tex: "paintingHills", fit: true },
  poster: { tex: "poster", fit: true }, posterRival: { tex: "posterRival", fit: true }, posterDragon: { tex: "posterDragon", fit: true }, clock: { tex: "clock", fit: true, cut: true },
  photos: { tex: "photos", fit: true }, map: { tex: "map", fit: true }, sign: { tex: "sign", fit: true }, dartboard: { tex: "dartboard", fit: true, cut: true }, bag: { tex: "bag", per: 1 },
  ball: { tex: "ball", fit: true }, ballTop: { tex: "ballTop", fit: true }, flowerTop: { tex: "flowerTop", per: 1 }, flower: { tex: "flower", fit: true, cut: true, double: true }, tallGrass: { tex: "tallGrass", fit: true, cut: true, double: true },
  mailbox: { tex: "mailbox", fit: true }, keyboard: { tex: "keyboard", fit: true }, console: { tex: "console", fit: true }, bunting: { tex: "bunting", fit: true, cut: true, double: true },
  trophyPlate: { tex: "trophyPlate", fit: true },
  aoN: { tex: "aoN", fit: true, overlay: true }, aoW: { tex: "aoW", fit: true, overlay: true }, aoE: { tex: "aoE", fit: true, overlay: true },
  deep: { tex: "deep", fit: true, overlay: true }, sunPool: { tex: "sunPool", fit: true, overlay: true, add: true }, shadowBlob: { tex: "shadowBlob", fit: true, overlay: true },
};
// Text boards: a wooden board with words painted on, as a sign or a door plate.
function textBoard(key, text, w = 40, ink = "#FFF4D8", board = "#8F5F2F") {
  if (MATDEF[key]) return key;
  w = Math.max(w, textWidth(text) + 8);
  TEXDEF[key] = [w, 11, (P, r) => {
    P.bevel(0, 0, w, 11, hx(board), 0.16, 0.06, r); P.rect(1, 1, w - 2, 1, shadec(hx(board), 0.22));
    P.text(text, Math.round((w - textWidth(text)) / 2), 3, hx(ink));
  }];
  MATDEF[key] = { tex: key, fit: true };
  return key;
}
const matCache = {};
function matFor(key) {
  if (matCache[key]) return matCache[key];
  const d = MATDEF[key];
  if (!d) throw new Error("no material " + key);
  const map = d.dyn ? d.dyn.tex : pixTex(d.tex);
  let m;
  if (d.overlay) m = new THREE.MeshBasicMaterial({ map, transparent: true, depthWrite: false, blending: d.add ? THREE.AdditiveBlending : THREE.NormalBlending, polygonOffset: true, polygonOffsetFactor: -2, polygonOffsetUnits: -2 });
  else if (d.glow) m = new THREE.MeshLambertMaterial({ map, emissive: 0xffffff, emissiveMap: map, emissiveIntensity: d.glow });
  else if (d.alpha) m = new THREE.MeshLambertMaterial({ map, transparent: true, opacity: d.alpha, depthWrite: false });
  else m = new THREE.MeshLambertMaterial({ map });
  if (d.cut) { m.alphaTest = 0.5; m.side = THREE.DoubleSide; }
  if (d.double) m.side = THREE.DoubleSide;
  return (matCache[key] = m);
}

// ---------- geometry: boxes and faces gathered by material, one mesh for each ----------
const V = (x, y, z) => new THREE.Vector3(x, y, z);
class Geo {
  constructor() { this.b = {}; }
  buf(k) { return this.b[k] || (this.b[k] = { pos: [], nor: [], uv: [], idx: [] }); }
  // A flat face given by its four corners seen from outside (bottom left, bottom right, top right, top left).
  quad(k, BL, BR, TR, TL, uv, n) {
    if (!k) return;
    const b = this.buf(k), base = b.pos.length / 3;
    if (!n) n = new THREE.Vector3().subVectors(BR, BL).cross(new THREE.Vector3().subVectors(TL, BL)).normalize();
    for (const p of [BL, BR, TR, TL]) { b.pos.push(p.x, p.y, p.z); b.nor.push(n.x, n.y, n.z); }
    b.uv.push(...uv);
    b.idx.push(base + 3, base, base + 1, base + 3, base + 1, base + 2);
  }
  // A face's texture coordinates: stretched over it ("fit"), or laid in blocks from the world's own axes so neighbours line up.
  faceUV(k, BL, R, U, w, h) {
    const d = MATDEF[k] || {};
    if (d.fit) return [0, 0, 1, 0, 1, 1, 0, 1];
    const per = Array.isArray(d.per) ? d.per : [d.per || 1, d.per || 1];
    const u0 = BL.dot(R) / per[0], v0 = BL.dot(U) / per[1], u1 = u0 + w / per[0], v1 = v0 + h / per[1];
    return [u0, v0, u1, v0, u1, v1, u0, v1];
  }
  face(k, BL, R, U, w, h, n) {
    if (!k || w <= 0 || h <= 0) return;
    const BR = BL.clone().addScaledVector(R, w), TL = BL.clone().addScaledVector(U, h), TR = BR.clone().addScaledVector(U, h);
    this.quad(k, BL, BR, TR, TL, this.faceUV(k, BL, R, U, w, h), n);
  }
  // spec: a material for every face, or { all, side, top, bottom, n, s, e, w } (null leaves a face out).
  box(x0, y0, z0, x1, y1, z1, spec) {
    const S = typeof spec === "string" ? { all: spec } : spec;
    const pick = (f, vert) => (f in S) ? S[f] : (vert && "side" in S) ? S.side : S.all;
    const w = x1 - x0, h = y1 - y0, d = z1 - z0;
    const X = V(1, 0, 0), Y = V(0, 1, 0), Z = V(0, 0, 1);
    this.face(pick("s", 1), V(x0, y0, z1), X, Y, w, h, Z);
    this.face(pick("n", 1), V(x1, y0, z0), X.clone().negate(), Y, w, h, Z.clone().negate());
    this.face(pick("e", 1), V(x1, y0, z1), Z.clone().negate(), Y, d, h, X);
    this.face(pick("w", 1), V(x0, y0, z0), Z, Y, d, h, X.clone().negate());
    this.face(pick("top", 0), V(x0, y1, z1), X, Z.clone().negate(), w, d, Y);
    this.face(pick("bottom", 0), V(x0, y0, z0), X, Z, w, d, Y.clone().negate());
  }
  // Any flat polygon (corners anticlockwise seen from outside), its texture laid in blocks along the face.
  poly(k, pts) {
    if (!k) return;
    const n = new THREE.Vector3().subVectors(pts[1], pts[0]).cross(new THREE.Vector3().subVectors(pts[2], pts[0])).normalize();
    let U = Math.abs(n.y) < 0.95 ? V(0, 1, 0).addScaledVector(n, -n.y).normalize() : V(0, 0, n.y > 0 ? -1 : 1);
    const R = new THREE.Vector3().crossVectors(U, n);
    const d = MATDEF[k] || {}, per = Array.isArray(d.per) ? d.per : [d.per || 1, d.per || 1];
    const b = this.buf(k), base = b.pos.length / 3;
    for (const p of pts) { b.pos.push(p.x, p.y, p.z); b.nor.push(n.x, n.y, n.z); b.uv.push(p.dot(R) / per[0], p.dot(U) / per[1]); }
    for (let i = 1; i + 1 < pts.length; i++) b.idx.push(base, base + i, base + i + 1);
  }
  build(shadows = true) {
    const g = new THREE.Group();
    for (const [k, b] of Object.entries(this.b)) {
      if (!b.idx.length) continue;
      const geo = new THREE.BufferGeometry();
      geo.setAttribute("position", new THREE.Float32BufferAttribute(b.pos, 3));
      geo.setAttribute("normal", new THREE.Float32BufferAttribute(b.nor, 3));
      geo.setAttribute("uv", new THREE.Float32BufferAttribute(b.uv, 2));
      geo.setIndex(b.idx);
      const d = MATDEF[k], mesh = new THREE.Mesh(geo, matFor(k));
      if (d.overlay) mesh.renderOrder = 2;
      else if (shadows) { mesh.castShadow = !d.glow && !d.alpha; mesh.receiveShadow = true; }
      g.add(mesh);
    }
    return g;
  }
}

// A piece of furniture's own frame: x along its front (from its left end as you face it), z from its back (0) out to its front.
// f is the way its front faces: "s" (against the north wall), "n", "w" (against the east wall) or "e" (against the west wall).
const OPP = { s: "n", n: "s", e: "w", w: "e" };
const LEFT = { s: "w", n: "e", w: "n", e: "s" };
function frame(room, X, Z, f, geo) {
  const T = { s: (lx, lz) => [X + lx, Z + lz], n: (lx, lz) => [X - lx, Z - lz], w: (lx, lz) => [X - lz, Z + lx], e: (lx, lz) => [X + lz, Z - lx] }[f];
  const names = { front: f, back: OPP[f], l: LEFT[f], r: OPP[LEFT[f]] };
  const mapSpec = s => { if (typeof s === "string") return s; const o = {}; for (const [k, v] of Object.entries(s)) o[names[k] || k] = v; return o; };
  const G = geo || room.geo;
  const F = {
    f, pt: T,
    rect(lx0, lz0, lx1, lz1) { const [ax, az] = T(lx0, lz0), [bx, bz] = T(lx1, lz1); return [Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)]; },
    box(lx0, y0, lz0, lx1, y1, lz1, spec) { const [x0, z0, x1, z1] = F.rect(lx0, lz0, lx1, lz1); G.box(x0, y0, z0, x1, y1, z1, mapSpec(spec)); },
    solid(lx0, lz0, lx1, lz1) { room.solid(...F.rect(lx0, lz0, lx1, lz1)); },
    act(lx0, lz0, lx1, lz1, a) { room.act(F.rect(lx0, lz0, lx1, lz1), a); },
    // The world angle (about y) for something that should face the way this frame's front faces.
    yaw: { s: 0, w: -Math.PI / 2, n: Math.PI, e: Math.PI / 2 }[f]
  };
  return F;
}

class Room {
  constructor(id, o) {
    Object.assign(this, { id, W: 16, D: 14, H: 4, outside: false }, o);
    this.geo = new Geo(); this.solids = []; this.acts = []; this.warps = []; this.ramps = []; this.npcs = []; this.screens = []; this.ticks = []; this.items = []; this.lights = [];
    this.group = new THREE.Group(); this.parts = []; this.doors = [];
  }
  solid(x0, z0, x1, z1) { this.solids.push([x0, z0, x1, z1]); }
  act(rect, a) { this.acts.push(Object.assign({ rect }, a)); }
  item(name, note) { if (!this.items.some(i => i.name === name)) this.items.push({ name, note }); }
  F(X, Z, f = "s") { return frame(this, X, Z, f); }
  // A moving part (a door, a swinging bag): its own geometry in its own group, pivoted where it turns.
  part(px, py, pz) { const geo = new Geo(), grp = new THREE.Group(); grp.position.set(px, py, pz); const p = { geo, grp, px, py, pz }; this.parts.push(p); return p; }
  screen(key, w, h, draw, glow = 1) {
    const s = screenTex(w, h, draw);
    MATDEF[key] = { dyn: s, fit: true, glow };
    this.screens.push(s);
    return key;
  }
  finish() {
    this.group.add(this.geo.build());
    for (const p of this.parts) { const m = p.geo.build(); m.position.set(-p.px, -p.py, -p.pz); p.grp.add(m); this.group.add(p.grp); }
  }
  groundAt(x, z) { for (const r of this.ramps) if (x >= r.rect[0] && x <= r.rect[2] && z >= r.rect[1] && z <= r.rect[3]) return r.h(x, z); return 0; }
}

// ---------- the shell: floor, the three walls you can see, skirting, and the shade where wall meets floor ----------
const T_WALL = 0.4;
// o.entry: the door's mat sits in a little nook two blocks deep (Let's Go's rooms have one): the room's floor and
// side walls stop two blocks short of the front, a low wooden ledge runs across the front either side of the nook
// and down both its sides, and the room opens up past it. true: a nook a block wider than the door each side; or
// [x0, x1]. room.Dm is where the room proper ends (D without a nook).
function shell(room, o = {}) {
  const { W, D, H } = room, G = room.geo, T = T_WALL, paper = o.paper || "paperMum";
  const ent = o.entry ? (o.entry === true ? [o.door[0] - 1, o.door[1] + 1] : o.entry) : null, Dm = ent ? D - 2 : D;
  room.Dm = Dm; room.entry = ent;
  const floors = o.floorRects || (ent ? [[0, 0, W, Dm], [ent[0], Dm, ent[1], D]] : [[0, 0, W, D]]);
  // The slab, cut along the front so its edge shows as a section.
  for (const [x0, z0, x1, z1] of floors) {
    const nook = ent && z0 >= Dm - 1e-6;
    G.box(x0, -0.35, z0, x1, 0, z1, { top: (nook && o.entryFloor) || o.floor || "oak", s: z1 >= Dm - 1e-6 ? "cap" : null, n: null, e: nook ? "cap" : null, w: nook ? "cap" : null, bottom: null });
  }
  G.box(-T, -0.35, -T, 0, 0, Dm, { top: null, s: "cap", n: null, e: null, w: null, bottom: null });
  G.box(W, -0.35, -T, W + T, 0, Dm, { top: null, s: "cap", n: null, e: null, w: null, bottom: null });
  for (const z of o.zones || []) G.box(z[0], 0, z[1], z[2], 0.012, z[3], { top: z[4], s: z[3] >= Dm - 1e-6 ? "cap" : null, n: null, e: null, w: null, bottom: null });
  // Walls: north, west and east. Their tops and cut ends are drawn dark, as a section.
  G.box(-T, 0, -T, W + T, H, 0, { s: paper, top: "cap", n: null, e: null, w: null, bottom: null });
  G.box(-T, 0, 0, 0, H, Dm, { e: paper, top: "cap", s: "cap", n: null, w: null, bottom: null });
  G.box(W, 0, 0, W + T, H, Dm, { w: paper, top: "cap", s: "cap", n: null, e: null, bottom: null });
  room.solid(-5, -5, W + 5, 0); room.solid(-5, -5, 0, D + 5); room.solid(W, -5, W + 5, D + 5);
  // The open front: solid but for the door.
  const door = o.door;
  if (ent) {
    // The ledge: across the front either side of the nook, and down its two sides.
    const lm = o.ledge || "walnut", lh = 0.7, lt = 0.3;
    for (const [x0, x1] of [[0, ent[0]], [ent[1], W]]) if (x1 - x0 > 1e-6) {
      G.box(x0, 0, Dm - lt, x1, lh, Dm, { all: lm, top: o.ledgeTop || lm, bottom: null });
      G.box(x0, lh, Dm - lt - 0.03, x1, lh + 0.06, Dm + 0.03, o.ledgeTop || lm);
    }
    for (const x of [ent[0], ent[1] - 0.08]) G.box(x, 0, Dm, x + 0.08, 0.05, D, { all: "cap", bottom: null });
    room.solid(-5, Dm - lt, ent[0], D + 5); room.solid(ent[1], Dm - lt, W + 5, D + 5);
    room.item("Entry", "The mat in a little nook at the door, its whole width; a low wooden ledge across the front either side, a dark edge down the nook's sides; the room opens up past it.");
  } else if (door) { room.solid(-5, D, door[0], D + 5); room.solid(door[1], D, W + 5, D + 5); } else room.solid(-5, D, W + 5, D + 5);
  // Skirting boards and a picture rail.
  const sk = o.skirt || "paint", skh = o.skirtH || 0.24;
  G.box(0, 0, 0, W, skh, 0.07, { all: sk, n: null, bottom: null });
  G.box(0, 0, 0, 0.07, skh, Dm, { all: sk, w: null, bottom: null });
  G.box(W - 0.07, 0, 0, W, skh, Dm, { all: sk, e: null, bottom: null });
  if (o.rail !== false) {
    const ry = H - 0.55;
    G.box(0, ry, 0, W, ry + 0.1, 0.06, { all: sk, n: null });
    G.box(0, ry, 0, 0.06, ry + 0.1, Dm, { all: sk, w: null });
    G.box(W - 0.06, ry, 0, W, ry + 0.1, Dm, { all: sk, e: null });
  }
  // The soft shade where the walls meet the floor.
  G.box(0, 0.02, 0, W, 0.02, 1.1, { top: "aoN", all: null });
  G.box(0, 0.02, 0, 1.1, 0.02, Dm, { top: "aoW", all: null });
  G.box(W - 1.1, 0.02, 0, W, 0.02, Dm, { top: "aoE", all: null });
}
// ---------- light through the windows, by the time of day (the mod's RoomLight does the very same sums) ----------
// A pool of light on the floor: where it falls at midday ([x0, z0, x1, z1]), dir the way the light goes in (0 south
// from a window in the north wall, 1 west from one in the east wall, 2 east from one in the west wall, 3 north in
// at the door), and the rect it's kept inside (the room, short of the walls, or the nook it's in).
// win: the window it comes through ([its wall's place, from and to along the wall, its foot and top]), for the
// shaft of light from it down to the pool (none at the door).
function sunPool(room, x0, z0, x1, z1, dir = 0, bounds, win) {
  const Dm = room.Dm || room.D;
  const b = bounds || (dir === 3 ? [0.3, 0.3, room.W - 0.3, room.D] : [0.3, 0.3, room.W - 0.3, Dm - 0.3]);
  (room.pools || (room.pools = [])).push([x0, z0, x1, z1, dir].concat(b, win || [0, 0, 0, 0, 0]).map(v => Math.round(v * 1000) / 1000));
}
// The times the viewer shows. k: how low and golden the sun is (morning or evening); side: where it is (-1 the east,
// in the morning, 1 the west, in the evening); m: how far it's night (the moon's light, the windows dark).
const TIMES = {
  morning: { k: 1, side: -1, m: 0, name: "Morning", note: "The sun low in the east: golden light, long pools, the east windows brightest" },
  day: { k: 0, side: 1, m: 0, name: "Day", note: "The sun high: warm white pools under the windows" },
  evening: { k: 1, side: 1, m: 0, name: "Evening", note: "The sun low in the west: orange light, long pools, a sunset in the windows" },
  night: { k: 0, side: 1, m: 1, name: "Night", note: "Moonlight: faint blue pools, the windows dark with stars (the lamps keep the room bright)" },
};
let TOD = "day";
// The light's colour and strength: warm white by day, golden low down, a faint cool blue at night.
// [r, g, b, the pool's strength, the shaft's].
function poolLight(k, m) {
  const mix = (a, b, t) => a.map((v, i) => v + (b[i] - v) * t);
  return mix(mix([1, 0.96, 0.82, 0.6, 0.16], [1, 0.74, 0.44, 0.55, 0.2], k), [0.55, 0.7, 1, 0.3, 0.08], m);
}
// Where a pool falls at a time, and how strong it is there: [x0, z0, x1, z1, strength], or null. A low sun throws it
// further in and longer, and to one side (the morning's west of the window, the evening's east); a window facing
// away from a low sun is in shade.
function poolRect(p, k, side, m) {
  let [x0, z0, x1, z1, dir, bx0, bz0, bx1, bz1] = p;
  const shift = 0.6 * k, scale = 1 + 0.7 * k - 0.15 * m, lat = 0.8 * k * side;
  let s = 1;
  if (dir === 0) { const l = (z1 - z0) * scale; z0 += shift; z1 = z0 + l; x0 += lat; x1 += lat; }
  else if (dir === 3) z0 = z1 - (z1 - z0) * scale;
  else if (dir === 1) { const l = (x1 - x0) * scale; x1 -= shift; x0 = x1 - l; if (side > 0) s = 1 - 0.65 * k; }
  else { const l = (x1 - x0) * scale; x0 += shift; x1 = x0 + l; if (side < 0) s = 1 - 0.65 * k; }
  x0 = Math.max(x0, bx0); z0 = Math.max(z0, bz0); x1 = Math.min(x1, bx1); z1 = Math.min(z1, bz1);
  return x1 - x0 > 0.1 && z1 - z0 > 0.1 ? [x0, z0, x1, z1, s] : null;
}
// The shaft from a pool's window down to where it falls (q, from poolRect): four sheets (its top, bottom and sides),
// each [corner, corner, corner, corner] of [x, y, z, u, v] (v 0 at the window, 1 on the floor); none for the door.
function beamQuads(p, q, y = 0.07) {
  const [, , , , dir, , , , , wc, a0, a1, wy0, wy1] = p;
  if (dir === 3 || wy1 <= wy0) return [];
  const [x0, z0, x1, z1] = q;
  // a: along the wall, d: in from it.
  const la0 = dir === 0 ? x0 : z0, la1 = dir === 0 ? x1 : z1;
  const near = dir === 0 ? z0 : dir === 1 ? x1 : x0, far = dir === 0 ? z1 : dir === 1 ? x0 : x1;
  const P = (a, yy, d, u, v) => dir === 0 ? [a, yy, d, u, v] : [d, yy, a, u, v];
  return [
    [P(a0, wy1, wc, 0, 0), P(a1, wy1, wc, 1, 0), P(la1, y, far, 1, 1), P(la0, y, far, 0, 1)],
    [P(a0, wy0, wc, 0, 0), P(a1, wy0, wc, 1, 0), P(la1, y, near, 1, 1), P(la0, y, near, 0, 1)],
    [P(a0, wy0, wc, 0, 0), P(a0, wy1, wc, 1, 0), P(la0, y, far, 1, 1), P(la0, y, near, 0, 1)],
    [P(a1, wy0, wc, 0, 0), P(a1, wy1, wc, 1, 0), P(la1, y, far, 1, 1), P(la1, y, near, 0, 1)],
  ];
}

// ---------- windows and things on walls ----------
function windowN(room, x, y, w = 2.2, h = 2, o = {}) {
  const G = room.geo;
  G.box(x, y, 0.0, x + w, y + h, 0.02, { s: "window", all: null });
  const fr = "paint", t = 0.14;
  G.box(x - t, y - t, 0, x + w + t, y, 0.16, fr); G.box(x - t, y + h, 0, x + w + t, y + h + t, 0.16, fr);
  G.box(x - t, y, 0, x, y + h, 0.16, fr); G.box(x + w, y, 0, x + w + t, y + h, 0.16, fr);
  G.box(x - 0.25, y - t - 0.1, 0, x + w + 0.25, y - t, 0.3, fr);
  if (o.curtains) {
    const c = o.curtains;
    G.box(x - 0.45, y + h + 0.3, 0.12, x + w + 0.45, y + h + 0.38, 0.2, "steel");
    const k = o.curtainIn || 0;
    G.box(x - 0.6, y - 0.4, 0.16, x - 0.05 + k, y + h + 0.32, 0.34, c);
    G.box(x + w + 0.05 - k, y - 0.4, 0.16, x + w + 0.6, y + h + 0.32, 0.34, c);
  }
  if (o.plant) { G.box(x + 0.15, y - t, 0.05, x + 0.55, y + 0.3 - t, 0.28, "potWhite"); G.box(x + 0.1, y + 0.3 - t, 0.02, x + 0.6, y + 0.85 - t, 0.32, "leafSolid"); }
  if (o.pool !== false) sunPool(room, x - 0.3, 1.2, x + w + 0.3, 1.2 + h * 1.4, 0, null, [0.03, x, x + w, y, y + h]);
  room.act([x, 0, x + w, 0.6], { label: "Look", lines: o.lines || ["It's a bright morning over Pallet Town."] });
  room.item("Window", "Sky and hills painted on, lit from outside; its light falls on the floor, by the time of day (a sunset at evening, stars at night).");
}
function framed(room, x, y, w, h, mat, wall = "n", o = {}) {
  const G = room.geo, t = o.frame === false ? 0 : 0.1, fm = o.frameMat || "walnut", d = o.depth || 0.08;
  const W = room.W;
  if (wall === "n") { if (t) G.box(x - t, y - t, 0, x + w + t, y + h + t, d, { all: fm, n: null }); G.box(x, y, d, x + w, y + h, d + 0.01, { s: mat, all: null }); room.act([x, 0, x + w, 0.6], o.act || {}); }
  else if (wall === "w") { if (t) G.box(0, y - t, x - t, d, y + h + t, x + w + t, { all: fm, w: null }); G.box(d, y, x, d + 0.01, y + h, x + w, { e: mat, all: null }); room.act([0, x, 0.6, x + w], o.act || {}); }
  else { if (t) G.box(W - d, y - t, x - t, W, y + h + t, x + w + t, { all: fm, e: null }); G.box(W - d - 0.01, y, x, W - d, y + h, x + w, { w: mat, all: null }); room.act([W - 0.6, x, W, x + w], o.act || {}); }
}
function clock(room, x, y) {
  room.geo.box(x, y, 0, x + 0.8, y + 0.8, 0.06, { s: "clock", all: null });
  room.geo.box(x + 0.1, y + 0.1, 0, x + 0.7, y + 0.7, 0.05, { all: "walnut", n: null, s: null });
  room.act([x - 0.2, 0, x + 1.0, 0.6], { label: "Look", lines: ["It's a quarter past nine. Plenty of morning left."] });
  room.item("Wall clock", "Round, wooden rim, hands at a quarter past nine.");
}
function whiteboard(room, x, y, w, h, mat = "whiteboard", lines) {
  const G = room.geo;
  G.box(x - 0.08, y - 0.08, 0, x + w + 0.08, y + h + 0.08, 0.07, { all: "silver", n: null });
  G.box(x, y, 0.07, x + w, y + h, 0.08, { s: mat, all: null });
  G.box(x + 0.2, y - 0.2, 0, x + w - 0.2, y - 0.1, 0.2, "silver");
  [["#2F5D96", 0.5], ["#D9524E", 0.75], ["#3B7D3A", 1.0]].forEach(([c, dx], i) => { const k = "marker" + i; if (!MATDEF[k]) { TEXDEF[k] = [2, 2, P => P.fill(hx(c))]; MATDEF[k] = { tex: k, per: 1 }; } G.box(x + dx, y - 0.1, 0.06, x + dx + 0.18, y - 0.04, 0.12, k); });
  room.act([x, 0, x + w, 0.7], { label: "Read", lines });
  room.item("Whiteboard", "Pixel-drawn marker notes and doodles, a tray with three pens.");
}

// ---------- the kitchen ----------
function counterUnit(room, F, lx, w, front = "cupboard", top = "counter", side = "mintPaint") {
  for (let i = 0; i < w; i++) {
    F.box(lx + i + 0.04, 0, 0.06, lx + i + 0.96, 0.1, 0.9, "cap");
    F.box(lx + i, 0.1, 0, lx + i + 1, 0.92, 0.96, { front, back: null, l: side, r: side, top: null, bottom: null });
  }
  F.box(lx, 0.92, 0, lx + w, 1.0, 1.02, typeof top === "string" ? { all: "counter", top } : top);
  F.solid(lx, 0, lx + w, 1.02);
}
function kitchen(room, X, f, o = {}) {
  // A run along a wall: fridge, cupboards, sink, hob with oven, and cupboards and a hood above.
  const F = room.F(X, o.z || 0, f), G = room.geo;
  F.box(0, 0, 0, 1.2, 2.7, 0.95, { front: "fridge", all: "paint", back: null, bottom: null });
  F.box(0.15, 2.7, 0.2, 0.75, 3.05, 0.7, { all: "honey" });
  F.box(0.8, 2.7, 0.3, 1.1, 2.95, 0.6, { all: "cushion" });
  F.solid(0, 0, 1.2, 0.95);
  room.act(F.rect(0, 0.5, 1.2, 1.4), { label: "Open", lines: ["The fridge is full. Lemonade, cheese, and a bowl of berries for your partner."] });
  counterUnit(room, F, 1.2, 1); counterUnit(room, F, 2.2, 1, "cupboard", "sink"); counterUnit(room, F, 3.2, 1); counterUnit(room, F, 4.2, 1, "oven", "hob"); counterUnit(room, F, 5.2, 1, "drawers");
  // Tap over the sink.
  F.box(2.65, 1.0, 0.08, 2.75, 1.45, 0.18, "steel"); F.box(2.65, 1.38, 0.08, 2.75, 1.45, 0.48, "steel"); F.box(2.55, 1.0, 0.08, 2.85, 1.06, 0.22, "steel");
  // A pot simmering, a kettle, a chopping board with a carrot.
  F.box(4.45, 1.0, 0.25, 4.95, 1.3, 0.75, { all: "steel", top: "darkTop" }); F.box(4.4, 1.22, 0.45, 4.48, 1.27, 0.55, "plastic"); F.box(4.92, 1.22, 0.45, 5.0, 1.27, 0.55, "plastic");
  F.box(5.45, 1.0, 0.35, 5.8, 1.32, 0.65, { all: "silver" }); F.box(5.58, 1.32, 0.45, 5.68, 1.38, 0.55, "plastic");
  F.box(3.35, 1.0, 0.3, 4.0, 1.04, 0.75, "honey"); F.box(3.5, 1.04, 0.45, 3.85, 1.1, 0.55, "cushion");
  // Tiles behind, cupboards above, and the hood.
  F.box(1.2, 1.0, 0, 6.2, 1.85, 0.03, { front: "splash", all: null });
  for (const i of [1.2, 5.2]) F.box(i, 2.2, 0, i + 1, 3.1, 0.6, { front: "wallCupboard", all: "mintPaint", back: null });
  if (f === "s") windowN(room, X + 2.35, 1.95, 1.6, 1.3, { plant: true, lines: ["Out of the kitchen window: the garden, and the lab's roof past the trees."] });
  F.box(4.1, 2.15, 0, 5.3, 2.4, 0.85, { all: "steel", back: null }); F.box(4.45, 2.4, 0, 4.95, H_TOP(room), 0.4, { all: "steel", back: null });
  room.act(F.rect(2.2, 0.6, 3.2, 1.5), { label: "Look", lines: ["The washing-up's all done. Mum keeps it tidy."] });
  room.act(F.rect(4.2, 0.6, 5.2, 1.5), { label: "Look", lines: ["Something's simmering. It smells like stew."] });
  room.item("Kitchen", "Fridge with a child's drawing, mint cupboards, sink and tap, hob with a pot simmering, oven, tiles and a steel hood.");
  // Steam off the pot.
  const [sx, sz] = F.pt(4.7, 0.5);
  room.steam = { x: sx, y: 1.32, z: sz };
}
const H_TOP = room => room.H;

// ---------- tables, chairs, sofas ----------
function table(room, x0, z0, x1, z1, o = {}) {
  const G = room.geo, top = o.top || "oak", leg = o.leg || "walnut", y = o.h || 0.86;
  G.box(x0, y - 0.1, z0, x1, y, z1, { all: leg, top });
  for (const [lx, lz] of [[x0 + 0.08, z0 + 0.08], [x1 - 0.2, z0 + 0.08], [x0 + 0.08, z1 - 0.2], [x1 - 0.2, z1 - 0.2]]) G.box(lx, 0, lz, lx + 0.12, y - 0.1, lz + 0.12, leg);
  room.solid(x0, z0, x1, z1);
  if (o.cloth) G.box(x0 + (x1 - x0) * 0.3, y, z0 - 0.02, x0 + (x1 - x0) * 0.7, y + 0.01, z1 + 0.02, { all: o.cloth, bottom: null });
  room.item(o.name || "Table", o.note || "Oak top on walnut legs.");
}
// A frame whose middle (of a w by d piece) sits at x, z.
function centred(room, x, z, f, w, d) {
  const O = { s: [x - w / 2, z - d / 2], n: [x + w / 2, z + d / 2], w: [x + d / 2, z - w / 2], e: [x - d / 2, z + w / 2] }[f];
  return frame(room, O[0], O[1], f);
}
function chair(room, x, z, face, o = {}) {
  // x, z: the seat's middle. face: the way the person sitting looks (the back is behind them).
  const w = 0.62, F = centred(room, x, z, face, w, w), wood = o.wood || "walnut";
  F.box(0, 0.48, 0, w, 0.56, w, { all: wood, top: o.seat || wood });
  for (const [lx, lz] of [[0.04, 0.04], [w - 0.12, 0.04], [0.04, w - 0.12], [w - 0.12, w - 0.12]]) F.box(lx, 0, lz, lx + 0.08, 0.48, lz + 0.08, wood);
  F.box(0, 0.56, 0, w, 1.18, 0.08, wood); F.box(0.12, 0.7, 0.08, w - 0.12, 1.08, 0.1, o.back || wood);
  F.solid(0, 0, w, w);
}
function sofa(room, X, Z, f, w = 3, o = {}) {
  const F = room.F(X, Z, f), fab = o.fabric || "sofa", cush = o.cushion || "sofaL";
  F.box(0.05, 0, 0.1, w - 0.05, 0.12, 0.85, "walnut");
  F.box(0, 0.12, 0, w, 0.5, 0.95, fab);
  const n = Math.round((w - 0.5) / 1.1);
  for (let i = 0; i < n; i++) { const a = 0.25 + i * (w - 0.5) / n; F.box(a + 0.02, 0.5, 0.25, a + (w - 0.5) / n - 0.02, 0.66, 0.95, cush); }
  F.box(0, 0.5, 0, w, 1.25, 0.28, fab);
  F.box(0, 0.12, 0, 0.25, 0.85, 0.95, fab); F.box(w - 0.25, 0.12, 0, w, 0.85, 0.95, fab);
  F.box(0.35, 0.66, 0.28, 0.85, 1.0, 0.42, o.pillow || "cushion"); F.box(w - 0.85, 0.66, 0.28, w - 0.35, 1.0, 0.42, o.pillow2 || "redFabric");
  F.solid(0, 0, w, 0.95);
  room.act(F.rect(0, 0, w, 1.4), { label: "Sit", lines: o.lines || ["A comfy sofa. The cushions still have the shape of Mum's afternoon nap."] });
  room.item("Sofa", "Teal fabric, three seat cushions, two throw pillows, walnut feet.");
}
// A rug as Minecraft builders lay one: carpet a block at a time (1 pixel thick), each in its own wool colour.
const RUGS = {
  mum: (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#B8343A" : (i === 1 || j === 1 || i === w - 2 || j === d - 2) ? "#EFE2C4" : ((i + j) % 2 ? "#E99A2E" : "#B8343A"),
  room: (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#F2F2F2" : ((i * 3 + j * 5) % 7 === 0 ? "#F2C12E" : "#3A6FD0"),
  rival: (i, j, w, d) => (j === 0 || j === d - 1) ? "#C9343A" : ((i + j) % 2 ? "#2A2A30" : "#F2F2F2"),
};
function wool(c) { const k = "wool" + c.slice(1); if (!MATDEF[k]) { TEXDEF[k] = [16, 16, (P, r) => mcWool(P, r, hx(c))]; MATDEF[k] = { tex: k, per: 1 }; } return k; }
function rug(room, x0, z0, x1, z1, mat) {
  const pat = RUGS[{ rugMum: "mum", rugRoom: "room", rugRival: "rival" }[mat] || mat];
  x0 = Math.round(x0); z0 = Math.round(z0); x1 = Math.round(x1); z1 = Math.round(z1);
  const w = x1 - x0, d = z1 - z0;
  for (let i = 0; i < w; i++) for (let j = 0; j < d; j++) room.geo.box(x0 + i, 0, z0 + j, x0 + i + 1, 1 / 16, z0 + j + 1, { top: wool(pat(i, j, w, d)), all: wool(pat(i, j, w, d)), bottom: null });
  room.item("Carpet", "Wool carpet, a block at a time, as it would be laid in Minecraft.");
}
function tvSet(room, X, Z, f, screenKey, o = {}) {
  const F = room.F(X, Z, f), w = o.w || 2.8;
  F.box(0, 0, 0, w, 0.62, 0.8, { all: "walnut", front: null, bottom: null });
  for (let i = 0; i < Math.floor(w); i++) F.box(i * w / Math.floor(w), 0.04, 0.8, (i + 1) * w / Math.floor(w), 0.6, 0.81, { front: "drawers", all: null });
  const tw = o.tw || 1.9, th = o.th || 1.15, tx = (w - tw) / 2;
  F.box(w / 2 - 0.3, 0.62, 0.2, w / 2 + 0.3, 0.7, 0.55, "plastic"); F.box(w / 2 - 0.08, 0.7, 0.3, w / 2 + 0.08, 0.85, 0.42, "plastic");
  F.box(tx, 0.85, 0.25, tx + tw, 0.85 + th, 0.42, "plastic");
  F.box(tx + 0.08, 0.93, 0.42, tx + tw - 0.08, 0.85 + th - 0.08, 0.43, { front: screenKey, all: null });
  if (o.speakers) { F.box(0.12, 0.62, 0.25, 0.42, 1.25, 0.6, "plastic"); F.box(w - 0.42, 0.62, 0.25, w - 0.12, 1.25, 0.6, "plastic"); }
  F.solid(0, 0, w, 0.8);
  room.act(F.rect(0, 0.4, w, 1.5), { label: "Watch", lines: o.lines });
}
function coffeeTable(room, x0, z0, x1, z1) {
  const G = room.geo;
  G.box(x0, 0.38, z0, x1, 0.46, z1, "walnut");
  for (const [x, z] of [[x0 + 0.06, z0 + 0.06], [x1 - 0.16, z0 + 0.06], [x0 + 0.06, z1 - 0.16], [x1 - 0.16, z1 - 0.16]]) G.box(x, 0, z, x + 0.1, 0.38, z + 0.1, "walnut");
  G.box(x0 + 0.2, 0.46, z0 + 0.15, x0 + 0.8, 0.48, z0 + 0.55, "poster");
  G.box(x1 - 0.45, 0.46, z0 + 0.25, x1 - 0.25, 0.64, z0 + 0.45, "potWhite");
  room.solid(x0, z0, x1, z1);
  room.item("Coffee table", "Low walnut table with a magazine and a mug.");
}

// ---------- shelves, sideboards, plants ----------
function bookshelf(room, X, Z, f, w = 2, h = 3, o = {}) {
  const F = room.F(X, Z, f), wood = o.wood || "walnut", d = 0.7, rows = Math.floor((h - 0.3) / 0.72);
  F.box(0, 0, 0, 0.1, h, d, wood); F.box(w - 0.1, 0, 0, w, h, d, wood); F.box(0, h - 0.1, 0, w, h, d, wood);
  F.box(0.1, 0, 0, w - 0.1, 0.18, d, wood); F.box(0.1, 0.18, 0, w - 0.1, h - 0.1, 0.05, { front: wood, all: null });
  for (let i = 0; i < rows; i++) {
    const y = 0.18 + i * 0.72;
    F.box(0.1, y, 0.05, w - 0.1, y + 0.06, d, wood);
    const skip = (o.trinkets || {})[i];
    if (o.drawers && i === 0) F.box(0.12, y + 0.06, 0.05, w - 0.12, y + 0.66, d - 0.02, { front: "drawers", all: wood });
    else if (skip === "ball") { F.box(0.15, y + 0.06, 0.05, w * 0.55, y + 0.56, d - 0.12, { front: "books", all: "linen" }); F.box(w * 0.62, y + 0.06, 0.25, w * 0.62 + 0.36, y + 0.42, 0.61, { front: "ball", all: "ball" }); }
    else if (skip === "plant") { F.box(0.15, y + 0.06, 0.05, w * 0.6, y + 0.56, d - 0.12, { front: "books", all: "linen" }); F.box(w * 0.68, y + 0.06, 0.25, w * 0.68 + 0.34, y + 0.32, 0.55, "potWhite"); F.box(w * 0.66, y + 0.32, 0.22, w * 0.66 + 0.4, y + 0.62, 0.58, "leafSolid"); }
    else if (skip === "trophy") { F.box(0.15, y + 0.06, 0.05, w * 0.5, y + 0.56, d - 0.12, { front: "books", all: "linen" }); trophy(F, w * 0.66, y + 0.06, 0.35, 0.6); }
    else F.box(0.15, y + 0.06, 0.05, w - 0.15, y + 0.56, d - 0.12, { front: "books", all: "linen", top: "linen" });
  }
  F.solid(0, 0, w, d);
  room.act(F.rect(0, 0.3, w, 1.2), { label: "Look", lines: o.lines || ["Rows of books. Cookbooks, a photo album, and a well-thumbed guide to raising your first partner."] });
  room.item(o.name || "Bookshelf", o.note || "Walnut shelves; every spine is drawn pixel by pixel, in different colours and heights.");
}
function trophy(F, lx, y, lz, s = 0.5) {
  F.box(lx - 0.14 * s / 0.5, y, lz - 0.1, lx + 0.14 * s / 0.5, y + 0.08, lz + 0.1, "walnut");
  F.box(lx - 0.04, y + 0.08, lz - 0.04, lx + 0.04, y + 0.2 * s / 0.5, lz + 0.04, "gold");
  F.box(lx - 0.12 * s / 0.5, y + 0.2 * s / 0.5, lz - 0.1, lx + 0.12 * s / 0.5, y + 0.42 * s / 0.5, lz + 0.1, "gold");
  F.box(lx - 0.17 * s / 0.5, y + 0.28 * s / 0.5, lz - 0.02, lx - 0.12 * s / 0.5, y + 0.38 * s / 0.5, lz + 0.02, "gold");
  F.box(lx + 0.12 * s / 0.5, y + 0.28 * s / 0.5, lz - 0.02, lx + 0.17 * s / 0.5, y + 0.38 * s / 0.5, lz + 0.02, "gold");
}
function sideboard(room, X, Z, f, w = 2.4, o = {}) {
  const F = room.F(X, Z, f);
  F.box(0, 0, 0, w, 0.1, 0.7, "cap");
  F.box(0, 0.1, 0, w, 0.95, 0.75, { all: "honey", front: null });
  const n = Math.round(w / 1.2);
  for (let i = 0; i < n; i++) F.box(i * w / n, 0.12, 0.75, (i + 1) * w / n, 0.93, 0.76, { front: "drawers", all: null });
  F.box(-0.04, 0.95, -0.02, w + 0.04, 1.02, 0.79, "walnut");
  // Photo frames and a vase of flowers.
  F.box(0.25, 1.02, 0.25, 0.75, 1.5, 0.3, { front: "photos", all: "walnut" }); F.box(0.9, 1.02, 0.3, 1.25, 1.36, 0.34, { front: "photos", all: "gold" });
  F.box(w - 0.6, 1.02, 0.25, w - 0.3, 1.38, 0.55, "potWhite"); F.box(w - 0.75, 1.38, 0.15, w - 0.15, 1.8, 0.65, { all: "flower" });
  F.solid(0, 0, w, 0.79);
  room.act(F.rect(0, 0.4, w, 1.3), { label: "Look", lines: o.lines || ["Family photos. In one of them you're holding a fishing rod the wrong way round."] });
  room.item("Sideboard", "Honey-wood drawers, photo frames, a vase of flowers.");
}
function plant(room, x, z, s = 1, o = {}) {
  const G = room.geo, p = 0.28 * s;
  G.box(x - p, 0, z - p, x + p, 0.55 * s, z + p, { all: o.pot || "pot", top: "soil" });
  G.box(x - p * 1.5, 0.55 * s, z - p * 1.5, x + p * 1.5, 1.05 * s, z + p * 1.5, "leafSolid");
  G.box(x - p * 1.15, 1.05 * s, z - p * 1.15, x + p * 1.15, 1.45 * s, z + p * 1.15, "leafSolid");
  G.box(x - p * 0.7, 1.45 * s, z - p * 0.7, x + p * 0.7, 1.7 * s, z + p * 0.7, "leafSolid");
  G.box(x - p * 1.8, 0.75 * s, z - 0.02, x + p * 1.8, 1.25 * s, z + 0.02, "leaf"); G.box(x - 0.02, 0.75 * s, z - p * 1.8, x + 0.02, 1.25 * s, z + p * 1.8, "leaf");
  room.solid(x - p, z - p, x + p, z + p);
  room.act([x - p, z - p, x + p, z + p], { label: "Look", lines: o.lines || ["A leafy fern. Mum talks to it every morning."] });
  room.item("Potted plant", "Terracotta pot, leaves in layers with cut-out sprigs.");
}

// ---------- stairs ----------
const STEP = 0.5;
function stairsUp(room, x0, z0 = 0, depth = 2, o = {}) {
  const G = room.geo, n = Math.round(room.H / STEP), run = n * STEP / 2;
  // Climbing east along the north wall: each step half a block high and a quarter of a block deep... (8 steps over 4 blocks).
  const sw = 4 / n;
  for (let i = 0; i < n; i++) G.box(x0 + i * sw, 0, z0, x0 + (i + 1) * sw, (i + 1) * STEP, z0 + depth, { top: "tread", s: "paint", w: "paint", n: null, e: null, bottom: null });
  // The banister: a newel post at the foot, spindles, and a rail climbing with the steps.
  const bz = z0 + depth;
  G.box(x0 - 0.05, 0, bz - 0.02, x0 + 0.15, 1.35, bz + 0.18, "walnut");
  for (let i = 0; i < n; i++) { const x = x0 + i * sw + sw * 0.5, y = (i + 1) * STEP; G.box(x - 0.03, y, bz + 0.04, x + 0.03, y + 0.95, bz + 0.1, "paint"); G.box(x0 + i * sw, y + 0.9, bz + 0.02, x0 + (i + 1) * sw, y + 1.02, bz + 0.12, "walnut"); }
  G.box(x0, 0, bz - 0.01, x0 + 4, 0.3, bz + 0.01, { s: "paint", all: null });
  room.solid(x0 + 0.1, bz - 0.02, x0 + 4.5, bz + 0.3);
  room.ramps.push({ rect: [x0, z0, x0 + 4, z0 + depth], h: (x) => Math.max(0, Math.min(room.H, Math.ceil((x - x0) / sw - 0.35) * STEP)) });
  room.warps.push({ test: p => p.y >= 1.45 && p.x > x0, to: o.to, spawn: o.spawn, fx: "fade", why: "up" });
  room.item("Stairs up", "Oak treads, white risers and spindles, a walnut rail and newel post.");
}
function stairsDown(room, x0, z0 = 0, depth = 2, o = {}) {
  const G = room.geo, n = 8, sw = 4 / n, wall = o.paper || "paperRoom";
  for (let i = 0; i < n; i++) {
    const top = (i + 1) * STEP - 4;
    G.box(x0 + i * sw, -4.5, z0, x0 + (i + 1) * sw, top, z0 + depth, { top: "tread", w: "paint", s: null, n: null, e: null, bottom: null });
  }
  // The well's walls below the floor, and the dark it goes down into.
  G.box(x0, -4.6, z0 + depth, x0 + 4, 0, z0 + depth + 0.05, { n: wall, all: null });
  G.box(x0, -4.6, z0 - 0.05, x0 + 4, 0, z0, { s: wall, all: null });
  G.box(x0 - 0.05, -4.6, z0, x0, 0, z0 + depth, { e: wall, all: null });
  G.box(x0 + 4, -4.6, z0, x0 + 4.05, 0, z0 + depth, { w: wall, all: null });
  G.box(x0, -0.36, z0, x0 + 4, -0.36, z0 + depth, { top: "deep", all: null });
  // A railing round the opening, open at the top step to the east.
  const gap = x0 + 4 - 0.95, bz = z0 + depth;
  G.box(x0 - 0.12, 0, bz - 0.02, x0 + 0.08, 1.3, bz + 0.16, "walnut"); G.box(gap - 0.18, 0, bz - 0.02, gap + 0.02, 1.3, bz + 0.16, "walnut");
  G.box(x0, 1.0, bz + 0.0, gap - 0.1, 1.12, bz + 0.12, "walnut");
  for (let x = x0 + 0.35; x < gap - 0.2; x += 0.4) G.box(x - 0.03, 0, bz + 0.03, x + 0.03, 1.0, bz + 0.09, "paint");
  G.box(x0 - 0.12, 0, z0, x0 + 0.04, 1.12, bz, { all: "walnut" });
  room.solid(x0 - 0.3, z0, x0 + 0.06, bz + 0.2); room.solid(x0, bz - 0.04, gap, bz + 0.25);
  room.ramps.push({ rect: [x0, z0, x0 + 4, z0 + depth], h: (x) => Math.min(0, Math.ceil((x - x0) / sw - 0.35) * STEP - 4) });
  room.warps.push({ test: p => p.y <= -1.45, to: o.to, spawn: o.spawn, fx: "fade", why: "down" });
  room.item("Stairs down", "The stairwell, with a railing round it; open at the top step.");
}
function doormat(room, x0, x1, o = {}) {
  const D = room.D, G = room.geo;
  if (room.entry) [x0, x1] = room.entry;
  const m = o.mat || "doormat", mk = x1 - x0 > 3 && MATDEF[m + "W"] ? m + "W" : m;
  G.box(x0, 0, D - 2, x1, 0.03, D, { top: mk, s: "cap", all: "cap", bottom: null });
  // The light in at the door (only as wide as the nook, when there's one).
  if (room.entry) sunPool(room, x0, D - 3.6, x1, D, 3, [x0, 0.3, x1, D]);
  else sunPool(room, x0 - 0.6, D - 3.6, x1 + 0.6, D, 3);
  room.exitArrow = { x: (x0 + x1) / 2, z: D - 0.55 };
  if (!o.none) room.warps.push({ test: (p, input) => p.z > D - 1.0 && p.x > x0 && p.x < x1 && input.down, to: o.to, spawn: o.spawn, fx: "fadeOut", why: "out" });
  room.item("Door mat", o.mat ? "This place's own mat, in its colours. Walk off it, south, to go out." : "Coir mat with a stitched border. Walk off it, south, to go out.");
}

function passageN(room, x0, x1, o = {}) {
  const G = room.geo;
  G.box(x0 - 0.15, 0, 0, x1 + 0.15, 2.6, 0.1, { s: o.frame || "walnut", all: null });
  G.box(x0, 0, 0.1, x1, 2.4, 0.11, { s: "doorDark", all: null });
  G.box(x0, 0, 0.11, x1, 0.03, 2, { top: o.mat || "doormat", s: "cap", all: "cap", bottom: null });
  if (!o.none) room.warps.push({ test: (p, input) => p.z < 1.0 && p.x > x0 && p.x < x1 && input.up, to: o.to, spawn: o.spawn, fx: "fade", why: "north" });
  room.item("Way through", "An open doorway in the north wall, a mat at its foot. Walk north through it.");
}
function wildGrass(room, x0, z0, x1, z1) {
  (room.grass || (room.grass = [])).push([x0, z0, x1, z1]);
  tallGrass(room, x0, z0, x1, z1);
}

// ---------- the bedroom ----------
function bed(room, X, Z, f, o = {}) {
  // Head against the wall, foot out into the room. Local x across the bed, z from head to foot.
  const F = room.F(X, Z, f), w = o.w || 1.7, l = o.l || 2.8, wood = o.wood || "pale", q = o.quilt || "quilt";
  F.box(0, 0, 0, w, 1.35, 0.14, { all: wood });
  F.box(0, 0, 0.14, w, 0.42, l, { all: wood });
  F.box(0.05, 0.42, 0.14, w - 0.05, 0.72, l - 0.05, "linen");
  F.box(0.22, 0.72, 0.25, w - 0.22, 0.95, 0.75, "linen");
  F.box(-0.03, 0.62, 0.95, w + 0.03, 0.8, l + 0.02, { all: q, top: q });
  F.box(0, 0, l - 0.12, w, 0.75, l, wood);
  F.solid(0, 0, w, l);
  room.act(F.rect(0, 0, w, l), { label: "Rest", ask: o.ask !== false });
  room.item("Bed", "Pale-wood frame, patchwork quilt, pillow, headboard against the wall.");
}
function nightstand(room, X, Z, f) {
  const F = room.F(X, Z, f);
  F.box(0, 0, 0, 0.8, 0.7, 0.7, { all: "pale", front: "drawers" });
  F.box(0.15, 0.7, 0.2, 0.35, 0.95, 0.45, "greyPlastic"); F.box(0.16, 0.78, 0.45, 0.34, 0.92, 0.46, { front: "clock", all: null });
  F.box(0.5, 0.7, 0.25, 0.7, 0.75, 0.45, "walnut"); F.box(0.57, 0.75, 0.32, 0.63, 1.05, 0.38, "walnut"); F.box(0.42, 1.05, 0.17, 0.78, 1.3, 0.53, "lampShade");
  F.solid(0, 0, 0.8, 0.7);
  room.item("Bedside table", "A drawer, an alarm clock and a glowing lamp.");
}
function deskPC(room, X, Z, f, screenKey, o = {}) {
  const F = room.F(X, Z, f), top = o.top || "pale";
  F.box(0, 0.8, 0, 2.3, 0.88, 0.95, { all: top });
  F.box(0.05, 0, 0.05, 0.65, 0.8, 0.9, { all: top, front: "drawers" }); F.box(2.15, 0, 0.1, 2.25, 0.8, 0.85, top);
  F.box(1.05, 0.88, 0.2, 1.5, 0.92, 0.45, "greyPlastic"); F.box(1.22, 0.92, 0.25, 1.33, 1.1, 0.35, "greyPlastic");
  F.box(0.78, 1.1, 0.18, 1.78, 1.78, 0.32, "greyPlastic");
  F.box(0.84, 1.16, 0.32, 1.72, 1.72, 0.33, { front: screenKey, all: null });
  F.box(0.85, 0.88, 0.48, 1.7, 0.91, 0.72, { top: "keyboard", all: "greyPlastic" }); F.box(1.85, 0.88, 0.55, 1.98, 0.92, 0.7, "greyPlastic");
  F.box(0.15, 0.88, 0.2, 0.3, 0.93, 0.35, "walnut"); F.box(0.2, 0.93, 0.25, 0.25, 1.4, 0.3, "steel"); F.box(0.08, 1.35, 0.12, 0.42, 1.55, 0.5, "lampShade");
  F.solid(0, 0, 2.3, 0.95);
  room.act(F.rect(0.6, 0.5, 1.9, 1.6), { label: "Use", lines: o.lines, pc: o.pc });
  room.item("Desk and PC", "Pale desk, drawers, a monitor whose screen moves, keyboard, mouse and a lamp.");
}
function officeChair(room, x, z, face, fab = "blueFabric") {
  const F = centred(room, x, z, face, 0.64, 0.64);
  F.box(0.27, 0, 0.27, 0.37, 0.5, 0.37, "steel"); F.box(0.05, 0, 0.05, 0.59, 0.06, 0.59, "rubber");
  F.box(0.02, 0.5, 0.02, 0.62, 0.62, 0.62, fab);
  F.box(0.04, 0.62, 0.0, 0.6, 1.25, 0.1, fab);
  F.solid(0.02, 0.02, 0.62, 0.62);
}
function wardrobe(room, X, Z, f) {
  const F = room.F(X, Z, f);
  F.box(0, 0, 0, 2, 3.0, 0.9, { all: "honey", front: "wardrobe" });
  F.box(-0.05, 3.0, -0.02, 2.05, 3.1, 0.95, "walnut");
  F.box(0.3, 3.1, 0.2, 1.0, 3.45, 0.7, "leather"); F.box(1.2, 3.1, 0.3, 1.7, 3.3, 0.7, "blueFabric");
  F.solid(0, 0, 2, 0.95);
  room.act(F.rect(0, 0.4, 2, 1.4), { label: "Open", lines: ["Your clothes. In game you change them from CLOTHES on the menu."] });
  room.item("Wardrobe", "Two-door honey-wood wardrobe, a bag and a box on top.");
}
function cushionSeat(room, x, z, mat) { room.geo.box(x - 0.4, 0, z - 0.4, x + 0.4, 0.22, z + 0.4, { all: mat, top: mat }); room.item("Floor cushions", "Two square cushions for playing games."); }
function gameConsole(room, x, z) {
  const G = room.geo;
  G.box(x - 0.35, 0, z - 0.25, x + 0.35, 0.18, z + 0.25, { all: "greyPlastic", top: "console" });
  G.box(x - 0.1, 0.0, z + 0.45, x + 0.18, 0.06, z + 0.62, "greyPlastic"); G.box(x + 0.35, 0.0, z + 0.3, x + 0.62, 0.06, z + 0.47, "redFabric");
  room.act([x - 0.4, z - 0.3, x + 0.4, z + 0.3], { label: "Play", lines: ["A racing game, paused on lap 2. It'll keep."] });
  room.item("Games console", "Grey console with two controllers; the TV shows its game.");
}
function trophyCabinet(room, X, Z, f) {
  const F = room.F(X, Z, f);
  F.box(0, 0, 0, 2, 0.15, 0.7, "walnut"); F.box(0, 2.3, 0, 2, 2.4, 0.7, "walnut"); F.box(0, 0.15, 0, 0.08, 2.3, 0.7, "walnut"); F.box(1.92, 0.15, 0, 2, 2.3, 0.7, "walnut");
  F.box(0.08, 0.15, 0, 1.92, 2.3, 0.04, "walnut");
  for (const y of [0.85, 1.55]) F.box(0.08, y, 0.04, 1.92, y + 0.05, 0.66, "glass");
  trophy(F, 0.5, 0.15, 0.35, 0.5); trophy(F, 1.0, 0.15, 0.35, 0.65); trophy(F, 1.5, 0.15, 0.35, 0.5);
  trophy(F, 0.7, 0.9, 0.35, 0.5); F.box(1.15, 0.9, 0.3, 1.55, 1.25, 0.34, { front: "trophyPlate", all: "walnut" });
  trophy(F, 1.0, 1.6, 0.35, 0.55);
  F.box(0.08, 0.15, 0.66, 1.92, 2.3, 0.68, "glass");
  F.solid(0, 0, 2, 0.7);
  room.act(F.rect(0, 0.4, 2, 1.3), { label: "Look", lines: ["Junior trophies, all polished. Every one says FIRST PLACE."] });
  room.item("Trophy cabinet", "Glass-fronted walnut cabinet with gold cups on glass shelves.");
}
function punchingBag(room, x, z) {
  const G = room.geo;
  G.box(x - 0.55, 0, z - 0.55, x + 0.55, 0.12, z + 0.55, "rubber"); G.box(x - 0.06, 0.12, z - 0.06, x + 0.06, 2.9, z + 0.06, "steel");
  G.box(x - 0.06, 2.8, z - 0.06, x + 0.06, 2.9, z + 0.6, "steel");
  const p = room.part(x, 2.82, z + 0.55);
  p.geo.box(x - 0.02, 2.4, z + 0.53, x + 0.02, 2.82, z + 0.57, "steel");
  p.geo.box(x - 0.3, 1.0, z + 0.25, x + 0.3, 2.4, z + 0.85, { all: "leather", top: "rubber" });
  room.bag = { grp: p.grp, swing: 0, t: 0 };
  room.solid(x - 0.55, z - 0.55, x + 0.55, z + 0.9);
  room.act([x - 0.5, z - 0.4, x + 0.5, z + 0.9], { label: "Punch", lines: ["Thwack! It's seen a lot of use."], hit: true });
  room.item("Punching bag", "Red leather bag on a steel stand; it swings when you punch it.");
}

// ---------- outside ----------
function ground(room, mat = "grass") { const { W, D } = room; room.geo.box(0, -0.4, 0, W, 0, D, { top: mat, s: "cap", n: null, e: "cap", w: "cap", bottom: null }); room.solid(-5, -5, W + 5, 0.4); room.solid(-5, -5, 0.4, D + 5); room.solid(W - 0.4, -5, W + 5, D + 5); room.solid(-5, D - 0.4, W + 5, D + 5); }
function patch(room, x0, z0, x1, z1, mat) { room.geo.box(x0, 0, z0, x1, 0.015, z1, { top: mat, all: null }); }
function house(room, x0, z0, o) {
  // Small outside, as in the old games: 8 blocks wide, 6 deep. The door is a third of the way along.
  const G = room.geo, w = 8, d = 6, wh = 3.6, rise = 2.3, ov = 0.45, roof = o.roof;
  G.box(x0, 0, z0, x0 + w, wh, z0 + d, { all: "siding", top: null, bottom: null });
  G.box(x0 - 0.1, 0, z0 + d - 0.1, x0 + 0.15, wh, z0 + d + 0.05, "fence"); G.box(x0 + w - 0.15, 0, z0 + d - 0.1, x0 + w + 0.1, wh, z0 + d + 0.05, "fence");
  G.box(x0 - 0.05, 0, z0 - 0.05, x0 + w + 0.05, 0.35, z0 + d + 0.08, "stone");
  // The roof: a gable running east to west, its slopes seen from the front.
  const zr = z0 + d / 2, y0 = wh - 0.25, y1 = wh + rise, ax = x0 - ov, bx = x0 + w + ov, za = z0 - ov, zb = z0 + d + ov, th = 0.25;
  G.poly(roof, [V(ax, y0, zb), V(bx, y0, zb), V(bx, y1, zr), V(ax, y1, zr)]);
  G.poly(roof, [V(bx, y0, za), V(ax, y0, za), V(ax, y1, zr), V(bx, y1, zr)]);
  G.poly("cap", [V(ax, y0 - th, zb), V(bx, y0 - th, zb), V(bx, y0, zb), V(ax, y0, zb)]);
  G.poly("siding", [V(x0, wh, z0 + d), V(x0, wh, z0), V(x0, y1 - 0.1, zr)].reverse()); G.poly("siding", [V(x0 + w, wh, z0), V(x0 + w, wh, z0 + d), V(x0 + w, y1 - 0.1, zr)].reverse());
  G.poly("cap", [V(ax, y0 - th, zb), V(ax, y0, zb), V(ax, y1, zr), V(ax, y1 - th, zr)]); G.poly("cap", [V(bx, y0, zb), V(bx, y0 - th, zb), V(bx, y1 - th, zr), V(bx, y1, zr)]);
  G.box(ax, y1 - 0.05, zr - 0.18, bx, y1 + 0.12, zr + 0.18, roof);
  G.box(x0 + w - 2.2, y1 - 1.2, zr - 1.4, x0 + w - 1.4, y1 + 0.9, zr - 0.6, { all: "stone", top: "cap" });
  // Windows, a door, a step, a mailbox and a sign.
  const wx = x0 + 5.0;
  G.box(wx, 1.2, z0 + d, wx + 1.6, 2.6, z0 + d + 0.03, { s: "windowExt", all: null });
  G.box(wx - 0.12, 1.08, z0 + d, wx + 1.72, 1.2, z0 + d + 0.2, "fence"); G.box(wx - 0.12, 2.6, z0 + d, wx + 1.72, 2.72, z0 + d + 0.12, "fence");
  G.box(wx - 0.5, 1.15, z0 + d, wx - 0.12, 2.65, z0 + d + 0.08, o.shutter); G.box(wx + 1.72, 1.15, z0 + d, wx + 2.1, 2.65, z0 + d + 0.08, o.shutter);
  G.box(wx - 0.1, 0.35, z0 + d, wx + 1.7, 1.0, z0 + d + 0.5, { all: "walnut", top: "flower" });
  const dx = x0 + 2.2, dw = 1.4, dh = 2.5;
  G.box(dx - 0.15, 0, z0 + d, dx, dh + 0.15, z0 + d + 0.12, "fence"); G.box(dx + dw, 0, z0 + d, dx + dw + 0.15, dh + 0.15, z0 + d + 0.12, "fence"); G.box(dx - 0.15, dh, z0 + d, dx + dw + 0.15, dh + 0.15, z0 + d + 0.12, "fence");
  G.box(dx, 0, z0 + d, dx + dw, dh, z0 + d + 0.006, { s: "cap", all: null });
  G.box(dx - 0.3, 0, z0 + d, dx + dw + 0.3, 0.18, z0 + d + 0.7, "stone");
  const door = room.part(dx, 0, z0 + d + 0.02);
  door.geo.box(dx, 0, z0 + d, dx + dw, dh, z0 + d + 0.12, { s: "doorExt", all: "walnut" });
  room.doors.push({ grp: door.grp, x: dx + dw / 2, z: z0 + d, open: 0, to: o.to, spawn: o.spawn, x0: dx, x1: dx + dw });
  room.solid(x0 - 0.1, z0, x0 + w + 0.1, z0 + d + 0.12);
  textBoard(o.signKey, o.signText, 44);
  const sz = z0 + d + 0.6;
  G.box(dx - 2.2, 0, sz, dx - 2.1, 0.95, sz + 0.1, "walnut"); G.box(dx - 1.0, 0, sz, dx - 0.9, 0.95, sz + 0.1, "walnut");
  G.box(dx - 2.5, 0.8, sz + 0.02, dx - 0.6, 1.35, sz + 0.12, { s: o.signKey, all: "walnut" });
  room.solid(dx - 2.5, sz - 0.1, dx - 0.6, sz + 0.2);
  room.act([dx - 2.5, sz - 0.1, dx - 0.6, sz + 0.25], { label: "Read", lines: [o.signRead] });
  const mz = z0 + d + 0.45;
  G.box(x0 + w - 0.9, 0, mz + 0.2, x0 + w - 0.8, 1.0, mz + 0.3, "walnut"); G.box(x0 + w - 1.15, 1.0, mz, x0 + w - 0.55, 1.4, mz + 0.5, { all: "mailbox", top: "blueFabric" });
  room.solid(x0 + w - 1.15, mz, x0 + w - 0.55, mz + 0.5);
  room.act([x0 + w - 1.15, mz, x0 + w - 0.55, mz + 0.5], { label: "Look", lines: [o.mail] });
  room.item(o.name, "8 by 6 blocks outside (16 by 14 inside), siding walls, tiled gable roof, chimney, window box, a door that opens as you walk up.");
}
function lab(room, x0, z0, o = {}) {
  const G = room.geo, w = 12, d = 7, h = 4.4;
  G.box(x0, 0, z0, x0 + w, h, z0 + d, { all: "labWall", top: "roofGrey", bottom: null });
  G.box(x0 - 0.25, h, z0 - 0.25, x0 + w + 0.25, h + 0.4, z0 + d + 0.25, { all: "fence", top: "roofGrey" });
  G.box(x0 + 2, h + 0.4, z0 + 2, x0 + 5, h + 1.6, z0 + 4.5, { all: "labWall", top: "roofGrey" });
  G.box(x0 + 8.5, h + 0.4, z0 + 3, x0 + 8.7, h + 1.8, z0 + 3.2, "steel"); G.box(x0 + 8.0, h + 1.6, z0 + 2.7, x0 + 9.2, h + 1.7, z0 + 3.5, "silver");
  for (const wx of [x0 + 0.8, x0 + 2.8, x0 + 7.6, x0 + 9.6]) { G.box(wx, 1.4, z0 + d, wx + 1.6, 3.0, z0 + d + 0.03, { s: "windowExt", all: null }); G.box(wx - 0.1, 1.3, z0 + d, wx + 1.7, 1.4, z0 + d + 0.15, "fence"); }
  const dx = x0 + w / 2 - 1, dw = 2;
  G.box(dx, 0, z0 + d, dx + dw, 2.8, z0 + d + 0.04, { s: "doorLab", all: null });
  G.box(dx - 0.2, 2.8, z0 + d, dx + dw + 0.2, 3.0, z0 + d + 0.6, "roofGrey");
  G.box(dx - 0.4, 0, z0 + d, dx + dw + 0.4, 0.15, z0 + d + 1.0, "stone");
  textBoard("signLab", "PROF. LAB", 44, "#26304A", "#E9EDEC");
  G.box(dx - 0.6, 3.15, z0 + d, dx + dw + 0.6, 3.85, z0 + d + 0.06, { s: "signLab", all: "fence" });
  room.solid(x0, z0, x0 + w, z0 + d + 0.1);
  if (o.to) room.warps.push({ test: (p, input) => input.up && p.z < z0 + d + 0.62 && p.x > dx + 0.1 && p.x < dx + dw - 0.1, to: o.to, spawn: o.spawn, fx: "iris" });
  room.item("The professor's lab", "12 by 7 blocks outside, white panels, a roof vent and a dish. Walk into its door to go in.");
}
function tree(room, x, z, s = 1) {
  const G = room.geo;
  G.box(x - 0.25 * s, 0, z - 0.25 * s, x + 0.25 * s, 1.3 * s, z + 0.25 * s, "bark");
  G.box(x - 1.1 * s, 1.1 * s, z - 1.1 * s, x + 1.1 * s, 2.4 * s, z + 1.1 * s, "treeLeaf");
  G.box(x - 0.8 * s, 2.4 * s, z - 0.8 * s, x + 0.8 * s, 3.2 * s, z + 0.8 * s, "treeLeaf");
  G.box(x - 0.4 * s, 3.2 * s, z - 0.4 * s, x + 0.4 * s, 3.6 * s, z + 0.4 * s, "treeLeaf");
  room.solid(x - 0.6 * s, z - 0.6 * s, x + 0.6 * s, z + 0.6 * s);
}
function picket(room, x0, z0, x1, z1) {
  const G = room.geo, hor = Math.abs(x1 - x0) > Math.abs(z1 - z0), L = hor ? x1 - x0 : z1 - z0;
  for (let t = 0; t <= L + 1e-6; t += 0.5) { const x = hor ? x0 + t : x0, z = hor ? z0 : z0 + t; G.box(x - 0.06, 0, z - 0.06, x + 0.06, 0.95, z + 0.06, "fence"); }
  if (hor) { G.box(x0, 0.3, z0 - 0.03, x1, 0.4, z0 + 0.03, "fence"); G.box(x0, 0.7, z0 - 0.03, x1, 0.8, z0 + 0.03, "fence"); room.solid(x0 - 0.1, z0 - 0.15, x1 + 0.1, z0 + 0.15); }
  else { G.box(x0 - 0.03, 0.3, z0, x0 + 0.03, 0.4, z1, "fence"); G.box(x0 - 0.03, 0.7, z0, x0 + 0.03, 0.8, z1, "fence"); room.solid(x0 - 0.15, z0 - 0.1, x0 + 0.15, z1 + 0.1); }
}
function flowers(room, x0, z0, x1, z1) {
  const G = room.geo;
  G.box(x0 - 0.1, 0, z0 - 0.1, x1 + 0.1, 0.14, z1 + 0.1, { all: "soil", top: "flowerTop" });
  for (let x = x0; x < x1 - 0.1; x += 1) for (let z = z0; z < z1 - 0.1; z += 1) { G.box(x + 0.1, 0.14, z + 0.5, x + 0.9, 0.75, z + 0.5, { s: "flower", all: null }); G.box(x + 0.5, 0.14, z + 0.1, x + 0.5, 0.75, z + 0.9, { e: "flower", all: null }); }
  room.item("Flower beds", "Red, yellow, white and pink flowers as crossed cut-outs.");
}
function tallGrass(room, x0, z0, x1, z1) {
  const G = room.geo;
  for (let x = x0; x < x1 - 0.1; x += 1) for (let z = z0; z < z1 - 0.1; z += 1) { G.box(x, 0, z + 0.5, x + 1, 0.8, z + 0.5, { s: "tallGrass", all: null }); G.box(x + 0.5, 0, z, x + 0.5, 0.8, z + 1, { e: "tallGrass", all: null }); }
}
