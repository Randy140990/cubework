// ---------- (0.65, John's 09:28) The gyms' battles on a battlefield from the TV show ----------
// "Can we tailor so all gym battles have the [halls] but look to be on a battle field from the tv show. I.e. symbol on
// the ground etc. but must be surrounded in the theme of that gym - I.e pewter would be rocky etc"
// Each gym's hall is now a long room with the show's battlefield laid down the middle: its own surface (PEWTER's
// rocky earth, ERIKA's grass, SURGE's steel plates...), white lines round it and across its middle, the big circle in
// the middle with the POKé BALL in it, and a trainer's box at each end where the trainers stand. Round it, the gym's
// own theme. Everyone stands where they did (the game's halls put you, them and the camera in the same places), so
// the battle's framed as before: you look up the field from behind yours.
//
// The field: 11 blocks across (x 7 to 18) by 15 long (z 4 to 19), its middle at (12.5, 11.5). Their POKéMON stands
// at (13.6, 8.6), their trainer at (15.6, 6.9); yours at (11.4, 14.6), you (unseen in a hall) at (9.4, 16.2). The
// camera is at (10.8, 2.3 up, 21.2), looking up the field, so nothing stands between there and the field.
const TVF = { x0: 7, z0: 4, W: 11, D: 15, cx: 12.5, cz: 11.5, foe: [15.6, 6.9], you: [9.4, 16.2] };

// The markings, painted on the field's own surface (a picture over the whole field, 16 to a block): the outside
// line, the halfway line, the centre circle with the POKé BALL in it, a trainer's box at each end.
function tvMarks(P, o = {}) {
  const S = 16, W = P.w, H = P.h, L = o.line || hx("#F7F5EE"), lw = 2;
  const put = (x, y, c) => { if (x >= 0 && y >= 0 && x < W && y < H) P.set(x, y, c); };
  const box = (x0, y0, x1, y1) => { for (let x = x0; x < x1; x++) for (let k = 0; k < lw; k++) { put(x, y0 + k, L); put(x, y1 - 1 - k, L); } for (let y = y0; y < y1; y++) for (let k = 0; k < lw; k++) { put(x0 + k, y, L); put(x1 - 1 - k, y, L); } };
  box(4, 4, W - 4, H - 4);
  const cy = (TVF.cz - TVF.z0) * S, cx = (TVF.cx - TVF.x0) * S;
  for (let x = 4; x < W - 4; x++) for (let k = -1; k < 1; k++) put(x, Math.round(cy) + k, L);
  // The centre circle (2 blocks round) and the POKé BALL in it (its top half red, towards them; the band across).
  const ring = (r, t, c) => { for (let y = Math.floor(cy - r - 2); y <= cy + r + 2; y++) for (let x = Math.floor(cx - r - 2); x <= cx + r + 2; x++) { const d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy); if (Math.abs(d - r) <= t) put(x, y, c); } };
  ring(2 * S, 1.1, L);
  if (o.ball !== false) {
    const R = 1.35 * S, red = o.red || hx("#D8443E"), white = hx("#F7F5EE"), black = hx("#24262A");
    for (let y = Math.floor(cy - R); y <= cy + R; y++) for (let x = Math.floor(cx - R); x <= cx + R; x++) {
      const dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.hypot(dx, dy);
      if (d > R) continue;
      let c = d > R - 1.6 ? black : Math.abs(dy) < 1.8 ? black : dy < 0 ? red : white;
      if (d < 6.2) c = d < 4.2 ? white : black;
      if (c === red && dx < -2 && dy < -5 && d < R - 5 && d > R - 9) c = shadec(red, 0.2);   // (a little shine)
      put(x, y, c);
    }
  }
  // The trainers' boxes (2 blocks square) round where each stands.
  for (const [tx, tz] of [TVF.foe, TVF.you]) {
    const bx = (tx - TVF.x0) * S, by = (tz - TVF.z0) * S;
    box(Math.round(bx - S), Math.round(by - S), Math.round(bx + S), Math.round(by + S));
  }
}
// A field's surface and its markings, as a material (the picture fitted to the field).
function tvFieldMat(key, base, o) {
  TEXDEF[key] = [TVF.W * 16, TVF.D * 16, (P, r) => { base(P, r); tvMarks(P, o); }];
  MATDEF[key] = { tex: key, fit: true };
  return key;
}
// The field, set into the floor (and its rim, a strip of o.rim round it, if it has one).
function tvField(R, mat, o = {}) {
  const G = R.geo, y = o.y ?? 0;
  G.box(TVF.x0, -0.35, TVF.z0, TVF.x0 + TVF.W, y, TVF.z0 + TVF.D, { top: mat, all: o.side || null, bottom: null });
}
// The floor round the field, a block at a time (top(x, z), or one material).
function tvFloor(R, top, skip) {
  for (let z = 0; z < R.D; z++) for (let x = 0; x < R.W; x++) {
    if (x >= TVF.x0 && x < TVF.x0 + TVF.W && z >= TVF.z0 && z < TVF.z0 + TVF.D) continue;
    if (skip && skip(x, z)) continue;
    R.geo.box(x, -0.35, z, x + 1, 0, z + 1, { top: typeof top === "function" ? top(x, z) : top, all: null });
  }
}
// The walls, a block thick round the edge, in bands (bands(side, i) -> [[y0, y1, mat]]; side n, s, w, e), and the ceiling.
function tvWalls(R, bands, ceil, top) {
  const G = R.geo;
  for (let i = 0; i < 24; i++) {
    for (const [side, x0, z0] of [["n", i, 0], ["s", i, 23], ["w", 0, i], ["e", 23, i]]) {
      if ((side === "w" || side === "e") && (i === 0 || i === 23)) continue;
      const bs = bands(side, i);
      bs.forEach(([a, b, m], k) => { if (b > a) G.box(x0, a, z0, x0 + 1, b, z0 + 1, { all: m, top: b >= 8 || k < bs.length - 1 ? null : (top || m), bottom: null }); });
    }
  }
  for (let z = 0; z < 24; z++) for (let x = 0; x < 24; x++) G.box(x, 8, z, x + 1, 8.2, z + 1, { bottom: ceil, all: null });
}
const tvBands = (low, mid, high) => () => [[0, 1, low], [1, 7, mid], [7, 8, high || low]];
// Lamps overhead, panels in the ceiling down each side of the field (the hall's own light comes from them in the game).
function tvLamps(R, mat = "lampWhite") {
  for (const z of [6, 10, 14, 18]) for (const x of [5, 19]) R.geo.box(x - 0.6, 7.86, z - 0.6, x + 0.6, 8, z + 0.6, { bottom: mat, all: "steel" });
}
// A gym's hall: the floor, the field, the walls, the lamps; then its theme.
function tvGym(key, name, o) {
  BUILD["arena_" + key] = function () {
    const R = new Room("arena_" + key, { name: name + " battle", floor: "Battle arena", W: 24, D: 24, H: 8 });
    if (o.floor) tvFloor(R, o.floor, o.skip);
    if (o.field) tvField(R, o.field, o.fieldOpts);
    tvWalls(R, o.walls, o.ceil, o.wallTop);
    if (o.lamps !== false) tvLamps(R, o.lamps);
    o.theme(R, R.geo);
    // (In the viewer you stand in your trainer's box; in the game you're out of the picture, as in every hall.)
    R.spawn = { x: TVF.you[0], z: TVF.you[1], yaw: FACE.n };
    R.item("The battlefield", "As the TV show's: " + o.about + " White lines round it and across its middle, the centre circle with the POKé BALL in it, a trainer's box at each end.");
    R.item("Where it's used", "Every battle in " + name.toUpperCase() + ".");
    return R;
  };
}

// ---------- the fields' surfaces ----------
const tvBase = {
  rock(P, r) { // PEWTER: packed rocky earth, pebbles, a few cracks
    fitPaint(P, r, hx("#8E7C62"), { drift: 0.06 });
    for (let i = 0; i < P.w * P.h / 90; i++) P.disc(r() * P.w, r() * P.h, 0.6 + r() * 1.4, hx(r() < 0.5 ? "#A8977A" : "#6E5E4A"));
    for (let i = 0; i < 26; i++) { let x = r() * P.w, y = r() * P.h; for (let k = 0; k < 18; k++) { P.set(x | 0, y | 0, hx("#5E5040")); x += r() * 2 - 1; y += r() * 2 - 1; } }
  },
  earth(P, r) { // VIRIDIAN: red-brown earth, darker clods
    fitPaint(P, r, hx("#94603E"), { drift: 0.06 });
    for (let i = 0; i < P.w * P.h / 70; i++) P.disc(r() * P.w, r() * P.h, 0.5 + r() * 1.6, hx(r() < 0.5 ? "#7E4E30" : "#A6704A"));
  },
  grass(P, r) { // CELADON: mown grass in stripes across
    for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, shadec(hx("#5FA84A"), (Math.floor(y / 32) % 2 ? 0.05 : -0.02) + (r() - 0.5) * 0.08));
    for (let i = 0; i < P.w * P.h / 30; i++) P.set((r() * P.w) | 0, (r() * P.h) | 0, hx(r() < 0.5 ? "#4E9A3E" : "#7CC060"));
  },
  steel(P, r) { // VERMILION: steel plates, two blocks square, riveted
    fitPaint(P, r, hx("#8E98A2"), { drift: 0.03 });
    for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) { const u = x % 32, v = y % 32; if (u === 0 || v === 0) P.set(x, y, hx("#5E6872")); else if (u === 1 || v === 1) P.set(x, y, hx("#AEB8C2")); }
    for (let y = 4; y < P.h; y += 32) for (let x = 4; x < P.w; x += 32) for (const [a, b] of [[0, 0], [24, 0], [0, 24], [24, 24]]) P.rect(x + a, y + b, 2, 2, hx("#C8D0D8"));
  },
  wood(P, r) { // FUCHSIA: polished boards along the field
    for (let x = 0; x < P.w; x++) { const b = Math.floor(x / 8), t = ((b * 37) % 9) / 9 - 0.5; for (let y = 0; y < P.h; y++) { const seam = x % 8 === 0 || (y + b * 23) % 96 === 0; P.set(x, y, shadec(hx("#C49A64"), seam ? -0.14 : t * 0.1 + (r() - 0.5) * 0.03)); } }
  },
  tiles(P, r) { // SAFFRON: dark violet polished tiles, two blocks square, a soft sheen
    for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) { const u = x % 32, v = y % 32; let s = ((Math.floor(x / 32) + Math.floor(y / 32)) % 2 ? 0.04 : -0.02) + (r() - 0.5) * 0.02 + (u + v < 18 ? 0.04 : 0); if (u === 0 || v === 0) s = -0.2; P.set(x, y, shadec(hx("#4E3E72"), s)); }
  },
  volcanic(P, r) { // CINNABAR: dark rock, cracks glowing orange
    fitPaint(P, r, hx("#4E4242"), { drift: 0.05 });
    for (let i = 0; i < P.w * P.h / 120; i++) P.disc(r() * P.w, r() * P.h, 0.6 + r(), hx("#5E5050"));
    for (let i = 0; i < 22; i++) { let x = r() * P.w, y = r() * P.h; for (let k = 0; k < 26; k++) { P.set(x | 0, y | 0, hx(k % 3 ? "#E2682A" : "#FFA24A")); x += r() * 2 - 1; y += r() * 2 - 1; } }
  },
  deck(P, r) { // CERULEAN: the white platform
    fitPaint(P, r, hx("#F2F4F6"), { drift: 0.02 });
  },
};
const tvF = {
  pewter: tvFieldMat("tvFieldPewter", tvBase.rock),
  viridian: tvFieldMat("tvFieldViridian", tvBase.earth),
  celadon: tvFieldMat("tvFieldCeladon", tvBase.grass),
  vermilion: tvFieldMat("tvFieldVermilion", tvBase.steel),
  fuchsia: tvFieldMat("tvFieldFuchsia", tvBase.wood),
  saffron: tvFieldMat("tvFieldSaffron", tvBase.tiles),
  cinnabar: tvFieldMat("tvFieldCinnabar", tvBase.volcanic),
};
// CERULEAN's are on the platforms over the pool: the centre one's the circle and the POKé BALL; the others plain,
// a trainer's box drawn round the trainers'.
{
  const add = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, fit: true }, m || {}); };
  add("tvPoolCentre", 80, 80, (P, r) => {
    fitPaint(P, r, hx("#F2F4F6"), { drift: 0.02 });
    const cx = 40, cy = 40, L = hx("#3A78C8");
    for (let y = 0; y < 80; y++) for (let x = 0; x < 80; x++) { const d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy); if (Math.abs(d - 32) <= 1.1) P.set(x, y, L); }
    const R = 21.6, red = hx("#D8443E"), white = hx("#FFFFFF"), black = hx("#24262A");
    for (let y = 0; y < 80; y++) for (let x = 0; x < 80; x++) { const dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.hypot(dx, dy); if (d > R) continue; let c = d > R - 1.6 ? black : Math.abs(dy) < 1.8 ? black : dy < 0 ? red : white; if (d < 6.2) c = d < 4.2 ? white : black; P.set(x, y, c); }
    for (let i = 0; i < 80; i++) for (const k of [0, 1, 78, 79]) { P.set(i, k, L); P.set(k, i, L); }
  });
  add("tvPoolBox", 32, 32, (P, r) => { fitPaint(P, r, hx("#F2F4F6"), { drift: 0.02 }); const L = hx("#3A78C8"); for (let i = 0; i < 32; i++) for (const k of [0, 1, 30, 31]) { P.set(i, k, L); P.set(k, i, L); } });
  add("tvPoolPad", 32, 32, (P, r) => { fitPaint(P, r, hx("#F2F4F6"), { drift: 0.02 }); for (let i = 0; i < 32; i++) for (const k of [0, 31]) { P.set(i, k, hx("#C8D4DE")); P.set(k, i, hx("#C8D4DE")); } });
  add("tvHazardRim", 16, 4, P => { for (let x = 0; x < 16; x++) for (let y = 0; y < 4; y++) P.set(x, y, hx(((x + y) >> 2) % 2 ? "#2A2A2A" : "#F2C02A")); });
  add("tvGoldRim", 8, 8, (P, r) => fitPaint(P, r, hx("#D8B040"), { drift: 0.03 }), { fit: false, per: 1 });
  add("tvStand", 16, 16, (P, r) => { fitPaint(P, r, hx("#C2C8D0"), { drift: 0.02 }); P.rect(0, 0, 16, 2, hx("#E2E8EE")); P.rect(0, 14, 16, 2, hx("#8E96A0")); }, { fit: false, per: 1 });
}
// A rim round the field: a low strip just proud of the floor.
function tvRim(R, mat, w = 0.3, h = 0.08) {
  const G = R.geo, x0 = TVF.x0, z0 = TVF.z0, x1 = x0 + TVF.W, z1 = z0 + TVF.D;
  for (const [a, b, c, d] of [[x0 - w, z0 - w, x1 + w, z0], [x0 - w, z1, x1 + w, z1 + w], [x0 - w, z0, x0, z1], [x1, z0, x1 + w, z1]]) G.box(a, 0, b, c, h, d, { all: mat, bottom: null });
}
// Low stands down each side (three steps), as the show's gyms have for the crowd.
function tvStands(R, mat, top, x0, x1, z0 = 3, z1 = 19) {
  const G = R.geo, w = (x1 - x0) / 3, inward = x0 < 12 ? 1 : -1;
  for (let k = 0; k < 3; k++) {
    const a = inward > 0 ? x1 - (k + 1) * w : x0 + k * w, b = a + w;
    G.box(a, 0, z0, b, 0.5 * (k + 1), z1, { all: mat, top: top || mat });
  }
}

// ---------- the eight gyms ----------
tvGym("pewter_gym", "Pewter Gym", {
  about: "rocky earth, pebbles and cracks underfoot, boulders round it.",
  floor: "gymSand", field: tvF.pewter, walls: (side, i) => [[0, 6.6 + ((i * 7 + (side === "n" ? 3 : side === "w" ? 5 : 1)) % 5) * 0.3, "gymRock"], [6.6 + ((i * 7 + (side === "n" ? 3 : side === "w" ? 5 : 1)) % 5) * 0.3, 8, "gymRockDark"]],
  ceil: "gymRockDark", wallTop: "gymRockDark",
  theme(R, G) {
    // BROCK's stone stage at the back behind his box, its steps down; big rounded slabs of rock along the walls; tan
    // boulders in clumps down each side; a stone RHYHORN.
    G.box(9, 0, 1.0, 16, 1.0, 3.4, { all: "gymRockDark", top: "statueStone" });
    for (let k = 0; k < 2; k++) G.box(10.5, 0, 3.4 + k * 0.3, 14.5, 0.66 - k * 0.33, 3.7 + k * 0.3, { all: "gymRockDark", top: "statueStone" });
    for (const [x, z, w, h] of [[2.2, 2.4, 2.2, 4.4], [5.2, 1.4, 1.8, 3.4], [19.0, 1.4, 1.8, 3.6], [21.8, 2.6, 2.2, 4.6], [1.6, 7.5, 1.4, 3.8], [22.4, 7.5, 1.4, 3.8], [1.6, 13, 1.4, 3.2], [22.4, 13, 1.4, 3.2]])
      G.box(x - w / 2, 0, z - 0.7, x + w / 2, h, z + 0.7, { all: "gymRock", top: "gymRockDark" });
    for (const [x, z, k] of [[4.4, 6.0, 1.3], [3.6, 9.4, 1.0], [5.0, 12.6, 1.1], [4.0, 16.0, 0.9], [20.4, 6.0, 1.3], [21.0, 9.6, 1.0], [19.8, 12.6, 1.1], [20.6, 16.2, 0.9]]) tanRock(R, x, z, k);
    gymStatue(R, 6.6, 2.6, ["A stone RHYHORN."]);
  },
});
tvGym("viridian_gym", "Viridian Gym", {
  about: "red-brown earth, the ground's own, a gold rim round it.",
  floor: "vgFloor", field: tvF.viridian, walls: tvBands("vgRailFace", "vgWall", "vgRailFace"), ceil: "vgWall",
  theme(R, G) {
    // GIOVANNI's dais at the back behind his box, gold-capped pillars down each side, two statues, plants.
    tvRim(R, "tvGoldRim");
    G.box(9, 0, 1.0, 16, 0.6, 3.2, { all: "vgRailFace", top: "vgMat" });
    for (const z of [4, 9, 14]) for (const x of [4.5, 20.5]) aPillar(R, x, z, 6.2, "vgWall", "gold", 0.9);
    for (const [x, z] of [[2.6, 6.5], [21.4, 6.5]]) gymStatue(R, x, z, ["VIRIDIAN CITY POKéMON GYM", "LEADER: ?"]);
    for (const [x, z] of [[2.4, 1.8], [21.6, 1.8], [2.4, 17], [21.6, 17]]) plant(R, x, z, 1.2, { pot: "potWhite", kind: "leafy" });
  },
});
tvGym("cerulean_gym", "Cerulean Gym", {
  about: "the pool itself, the POKéMON on white platforms floating on it, the big centre platform with the circle and the POKé BALL.",
  floor: "ceruleanTile", walls: tvBands("ceruleanTrim", "ceruleanWall", "ceruleanTrim"), ceil: "ceruleanWall",
  theme(R, G) {
    // The pool where the field is: its floor, the water a little below the deck; the deck's blue-edged rim.
    const x0 = TVF.x0, z0 = TVF.z0, x1 = x0 + TVF.W, z1 = z0 + TVF.D;
    for (let z = z0; z < z1; z++) for (let x = x0; x < x1; x++) { G.box(x, -1.4, z, x + 1, -1.2, z + 1, { top: "ceruleanTile", all: null }); G.box(x, -1.2, z, x + 1, -0.12, z + 1, { top: "fitWater", all: null }); }
    tvRim(R, "arPoolEdge", 0.25, 0.04);
    // The platforms: the centre one, one for each POKéMON, a trainer's box at each end, two more.
    const pad = (x, z, w, mat) => G.box(x - w / 2, -0.12, z - w / 2, x + w / 2, 0.1, z + w / 2, { top: mat, all: "arPoolEdge", bottom: null });
    pad(TVF.cx, TVF.cz, 5, "tvPoolCentre");
    pad(11.4, 14.6, 2.2, "tvPoolPad"); pad(13.6, 8.6, 2.2, "tvPoolPad"); pad(15.4, 14.2, 1.8, "tvPoolPad"); pad(9.6, 8.8, 1.8, "tvPoolPad");
    pad(TVF.foe[0], TVF.foe[1], 2, "tvPoolBox"); pad(TVF.you[0], TVF.you[1], 2, "tvPoolBox");
    // Lane ropes down the pool's sides, a diving board at the back, stands down each side.
    for (const x of [7.4, 17.6]) G.box(x - 0.05, -0.2, z0 + 0.3, x + 0.05, -0.1, z1 - 0.3, "arRope");
    G.box(11.5, 0, 1.2, 13.5, 1.4, 2.2, { all: "steel" }); G.box(11.8, 1.4, 1.2, 13.2, 1.5, 3.9, { all: "arPoolEdge" });
    tvStands(R, "ceruleanTrim", "tvStand", 1, 5.4); tvStands(R, "ceruleanTrim", "tvStand", 18.6, 23);
  },
});
tvGym("vermilion_gym", "Vermilion Gym", {
  about: "riveted steel plates, a yellow and black hazard rim round it.",
  floor: "surgeFloor", field: tvF.vermilion, walls: tvBands("surgeHazard", "surgeWall", "surgeHazard"), ceil: "steel",
  theme(R, G) {
    // Generators along the back, coils humming down each side, floodlights at the corners, the trash cans by the walls.
    tvRim(R, "tvHazardRim", 0.35, 0.06);
    for (const x of [3, 6, 18, 21]) G.box(x - 1.1, 0, 1.0, x + 1.1, 2.4, 2.2, { all: "steel", s: "silphMachine", top: "surgeHazard" });
    for (const z of [5, 10, 15]) for (const x of [3.4, 20.6]) { aPillar(R, x, z, 3.4, "surgeHazard", "steel", 0.5); G.box(x - 0.34, 3.4, z - 0.34, x + 0.34, 4.1, z + 0.34, "arMoonGlow"); }
    for (const [x, z] of [[1.6, 1.6], [22.4, 1.6]]) { aPillar(R, x, z, 6.4, "steel", "steel", 0.3); G.box(x - 0.6, 6.4, z - 0.3, x + 0.6, 7.0, z + 0.3, { all: "steel", s: "arMoonGlow" }); }
    for (const [x, z] of [[5.6, 18.4], [18.4, 18.4], [1.8, 18.4], [22.2, 18.4]]) G.box(x - 0.38, 0, z - 0.38, x + 0.38, 0.95, z + 0.38, { all: "trashCan", top: "trashLid" });
  },
});
tvGym("celadon_gym", "Celadon Gym", {
  about: "a lawn, mown in stripes, under the glass.",
  floor: "cgLawn", field: tvF.celadon, walls: tvBands("cgBrick", "cgWall", "walnut"), ceil: "cgPane",
  theme(R, G) {
    // The gym's hedges down each side, flower beds between them and the field, trees in the back corners, more
    // flowers along the back.
    for (let z = 2; z < 20; z++) for (const x of [1, 22]) fitHedge(G, x, z, 2, false, x * 7 + z);
    for (const z of [4, 8, 12, 16]) { flowers(R, 3, z, 5, z + 2); flowers(R, 19, z, 21, z + 2); }
    for (const [x, z] of [[2.5, 1.5], [21.5, 1.5]]) tree(R, x, z, 1.0);
    for (const x of [8, 11, 14]) flowers(R, x, 1, x + 2, 3);
  },
});
tvGym("fuchsia_gym", "Fuchsia Gym", {
  about: "polished boards running up it, tatami either side.",
  floor: (x, z) => (x < 6 || x >= 19) && z > 2 && z < 20 ? "arTatami" : "gymBoards", field: tvF.fuchsia,
  walls: () => [[0, 0.8, "gymBoardsDark"], [0.8, 6.5, "shoji"], [6.5, 8, "gymBoardsDark"]], ceil: "gymBoardsDark",
  theme(R, G) {
    // Dark posts round the paper screens, red lanterns down each side, scrolls at the back, ferns in the corners.
    for (const z of [4, 9, 14, 19]) for (const x of [1.2, 22.8]) G.box(x - 0.2, 0, z - 0.2, x + 0.2, 8, z + 0.2, "gymBoardsDark");
    for (const z of [5.5, 10.5, 15.5]) for (const x of [4.6, 19.4]) { G.box(x - 0.05, 0, z - 0.05, x + 0.05, 2.0, z + 0.05, "walnut"); G.box(x - 0.3, 2.0, z - 0.3, x + 0.3, 2.7, z + 0.3, "arLantern"); }
    for (const x of [9.5, 12.5, 15.5]) G.box(x - 0.45, 2.0, 0.95, x + 0.45, 4.4, 1.0, { s: "arScroll", all: null });
    for (const [x, z] of [[2.6, 1.8], [21.4, 1.8]]) plant(R, x, z, 0.9, { pot: "potWhite", kind: "fern" });
  },
});
tvGym("saffron_gym", "Saffron Gym", {
  about: "dark violet tiles, polished.",
  floor: "psyFloor", field: tvF.saffron, walls: tvBands("psyBand", "psyWall", "psyCap"), ceil: "psyWall",
  theme(R, G) {
    // The gym's glowing warp pads in a row down each side, crystal-topped pillars at the back, crystals glowing
    // in the air over the sides.
    for (const z of [4.5, 8.5, 12.5, 16.5]) for (const x of [3.6, 20.4]) { G.box(x - 0.8, 0, z - 0.8, x + 0.8, 0.06, z + 0.8, { top: "psyPadRim", all: "psyCap" }); G.box(x - 0.6, 0.06, z - 0.6, x + 0.6, 0.08, z + 0.6, { top: "psyPad", all: null }); }
    for (const x of [6, 19]) { aPillar(R, x, 1.8, 6, "psyWall", "psyCap"); G.box(x - 0.22, 6, 1.58, x + 0.22, 6.7, 2.02, "arCrystal"); }
    for (const z of [6.5, 10.5, 14.5]) for (const x of [2.2, 21.8]) G.box(x - 0.2, 4.2, z - 0.2, x + 0.2, 4.7, z + 0.2, "arCrystal");
  },
});
tvGym("cinnabar_gym", "Cinnabar Gym", {
  about: "dark volcanic rock, its cracks glowing, lava running down either side of it.",
  floor: "arBasaltTop", field: tvF.cinnabar, skip: (x, z) => (x === 5 || x === 19) && z >= 3 && z < 20,
  walls: (side, i) => { const h = 6 + ((i * 5 + (side === "n" ? 2 : 4)) % 4) * 0.5; return [[0, h, "arBasalt"], [h, 8, "arBasalt"]]; }, ceil: "arBasalt", wallTop: "arBasaltTop",
  theme(R, G) {
    // The lava: a channel down each side of the field, a fall of it down the back wall; ember-lit rocks; braziers.
    for (let z = 3; z < 20; z++) for (const x of [5, 19]) G.box(x, -0.5, z, x + 1, -0.2, z + 1, { top: "arLava", all: "arBasalt" });
    for (const x of [10, 15]) G.box(x - 0.6, 0, 1.0, x + 0.6, 6.5, 1.06, { s: "arLava", all: null });
    for (const [x, z] of [[2.8, 3.6], [3.2, 9.4], [2.6, 15], [21.2, 3.6], [20.8, 9.4], [21.4, 15]]) aRock(R, x, z, 1.1, aRockMats("arBasalt", "arBasalt", "arBasaltTop"));
    for (const [x, z] of [[7.5, 2.2], [17.5, 2.2]]) { G.box(x - 0.4, 0, z - 0.4, x + 0.4, 1.0, z + 0.4, "arBasalt"); G.box(x - 0.3, 1.0, z - 0.3, x + 0.3, 1.3, z + 0.3, "arEmber"); }
  },
});

// ---------- the ELITE FOUR and the CHAMPION: the same battlefield, in the LEAGUE's halls ----------
// (Their rooms as before, gold-capped pillars down each side; the field and the stone round it each their own.)
const tvStone = (base, fleck, vein) => (P, r) => {
  fitPaint(P, r, hx(base), { drift: 0.04 });
  for (let i = 0; i < P.w * P.h / 110; i++) P.disc(r() * P.w, r() * P.h, 0.5 + r(), hx(fleck));
  if (vein) for (let i = 0; i < 16; i++) { let x = r() * P.w, y = r() * P.h; for (let k = 0; k < 30; k++) { P.set(x | 0, y | 0, hx(vein)); x += r() * 2 - 0.8; y += r() * 2 - 1.2; } }
};
const tvLeague = (key, name, o) => tvGym(key, name, Object.assign({
  walls: tvBands("lgWall", "lgWallHi", "gold"), ceil: "lgWallHi",
}, o, {
  theme(R, G) {
    tvRim(R, "gold", 0.25, 0.06);
    for (const z of [4, 9, 14]) for (const x of [4.5, 20.5]) aPillar(R, x, z, 6.4, o.pillar, "gold", 0.9);
    o.extra(R, G);
  },
}));
tvLeague("loreleis_room", "Lorelei's room", {
  about: "ice, frosted and cracked, the snow drifting at its edge.", floor: "lgIce", pillar: "lgIcePillar",
  field: tvFieldMat("tvFieldLorelei", tvStone("#BFE0F0", "#E8F6FC", "#9CC6DC"), { line: hx("#FFFFFF") }),
  extra(R, G) {
    for (const [x, z] of [[2.4, 5], [2.6, 11], [2.2, 17], [21.6, 5], [21.4, 11], [21.8, 17]]) G.box(x - 1, 0, z - 0.8, x + 1, 0.35, z + 0.8, "arSnow");
    for (const x of [3, 6, 9, 15, 18, 21]) G.box(x - 0.12, 6.4, 1.2 - 0.12, x + 0.12, 8, 1.2 + 0.12, "arIcicle");
    for (const x of [10, 15]) G.box(x - 0.7, 0, 1.2, x + 0.7, 1.8, 2.4, "lgIcePillar");
  },
});
tvLeague("brunos_room", "Bruno's room", {
  about: "packed sand, as a fighting ring's.", floor: "lgSand", pillar: "lgRock",
  field: tvFieldMat("tvFieldBruno", tvStone("#C8A870", "#B49460", "#A88A58")),
  extra(R, G) {
    for (const [x, z, k] of [[2.6, 3.2, 1.1], [21.4, 3.2, 1.1], [7.5, 1.8, 0.9], [17.5, 1.8, 0.9]]) aRock(R, x, z, k, aRockMats("gymTanLow", "gymTan", "gymTanTop"));
    for (const [x, z] of [[2.4, 9.5], [21.6, 9.5], [2.4, 15.5], [21.6, 15.5]]) punchingBag(R, x, z);
  },
});
tvLeague("agathas_room", "Agatha's room", {
  about: "dark violet stone, the cracks in it faintly glowing.", floor: "lgViolet", pillar: "lgGilt",
  field: tvFieldMat("tvFieldAgatha", tvStone("#4A3A5E", "#5A4A70", "#8A6AC0")),
  extra(R, G) {
    for (const z of [2, 7, 12, 17]) for (const x of [1.4, 22.6]) G.box(x - 0.4, 1.2, z - 0.9, x + 0.4, 7, z + 0.9, "arDrape");
    let i = 0;
    for (const [x, z] of [[2, 4], [2, 10], [2, 15], [21, 4], [21, 10], [21, 15]]) fitGrave(R, G, x, z, "agatha" + i++);
  },
});
tvLeague("lances_room", "Lance's room", {
  about: "teal stone, hard and polished.", floor: "lgTeal", pillar: "lgHornBase",
  field: tvFieldMat("tvFieldLance", tvStone("#3E7A7A", "#4E8E8C", "#2E6262")),
  extra(R, G) {
    for (const [x, z] of [[2.4, 6.5], [2.4, 12.5], [21.6, 6.5], [21.6, 12.5]]) { G.box(x - 0.4, 0, z - 0.4, x + 0.4, 1.1, z + 0.4, { all: "steel", top: "steel" }); G.box(x - 0.3, 1.1, z - 0.3, x + 0.3, 1.5, z + 0.3, "arEmber"); }
    for (const x of [3, 7, 17, 21]) G.box(x - 0.5, 2.0, 0.95, x + 0.5, 5.4, 1.0, { s: "arBanner", all: null });
  },
});
tvGym("champions_room", "Champion's room", {
  about: "white marble with gold lines, the red runner up to it.", floor: "arMarble",
  field: tvFieldMat("tvFieldChampion", tvStone("#ECEAF2", "#DCDAE4", "#D0CEDA"), { line: hx("#D8B040") }),
  walls: tvBands("lgWall", "lgWallHi", "gold"), ceil: "lgWallHi",
  theme(R, G) {
    tvRim(R, "gold", 0.25, 0.06);
    G.box(11.5, 0, 0.5, 13.5, 0.012, TVF.z0 - 0.3, { top: "arRunner", all: null });
    G.box(11.5, 0, TVF.z0 + TVF.D + 0.3, 13.5, 0.012, 23, { top: "arRunner", all: null });
    for (const z of [4, 9, 14]) for (const x of [4.5, 20.5]) aPillar(R, x, z, 6.6, "arMarble", "gold", 0.9);
    for (const z of [2, 7, 12, 17]) for (const x of [1.4, 22.6]) G.box(x - 0.4, 1.5, z - 0.9, x + 0.4, 7.2, z + 0.9, "arDrapeRed");
    for (const x of [8.5, 16.5]) { G.box(x - 0.5, 0, 1.6, x + 0.5, 1.0, 2.6, { all: "walnut" }); G.box(x - 0.2, 1.0, 1.9, x + 0.2, 1.6, 2.3, "arTrophy"); }
  },
});

// The halls in the viewer get the battle's camera too (where the game's halls put it: behind yours, looking up the field).
for (const id of ["arena_lab", "arena_pewter_gym", ...ARENA_HALLS]) {
  const make = BUILD[id];
  if (!make || make.__cam) continue;
  BUILD[id] = function () { const R = make.apply(this, arguments); if (!R.battleCam) R.battleCam = { pos: { x: 10.8, y: 2.3, z: 21.2 }, look: { x: 12.5, y: 0.9, z: 11.6 } }; return R; };
  BUILD[id].__cam = true;
}
