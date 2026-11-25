// ---------- (0.66, John's 13:36) Each room's light, worked out once, and drawn the same here and in the game ----------
// "the lighting just doesn't exist, which is one of the main things that makes the room look amazing ... you said
// what I see in the Retromon interior viewer UI is what I'm going to see in game, and it's just not."
// The viewer used to light its rooms itself as it drew them (a sun with its shadows, the sky's light and the
// floor's); the game drew the same blocks evenly lit. Now each room's light is worked out once, here, at the corners
// of every piece the game draws (each face cut at the blocks and the half blocks, as tools/interior/export/tomod.py
// cuts it): how much of the sun reaches it past everything in the room, softly (S), and how open it is to the room's
// light round about, so corners and the ground under things are darker (AO). The game keeps those two numbers for
// each corner (retromon/interiors/light/<room>.bin) and colours its pieces with them (client/RoomShade.java); the
// viewer's In game view draws the very same colours on the very same pieces. The sums for the colour from S and AO,
// by the time of day, are the same in both (shadeLightAt here, RoomShade.light there).
const SHADE = {
  Q: 64,            // corners are found to a 64th of a block
  OFF: 4096,        // (so a room's corners from -64 to 192 blocks are all positive)
  CONE: 0.08,       // the sun's light comes from a little round patch of sky (radians): its shadows' soft edges
  SUN_RAYS: 8,      // rays to the sun from each corner
  AO_RAYS: 12,      // rays round each corner, for how open it is
  AO_REACH: 1.25,   // how far round a corner things darken it (blocks)
  AO_K: 0.6,        // how much the room's light round about is lost in a closed-in corner
  SPREAD: 0.4,      // how far round a corner its rays start (blocks): its light is the patch round it's
  THIN: 0.13,       // faces narrower than this (blocks) cast no shadow
  SCALE: 2.5,       // the game's shader takes a corner's colour times this (so the light can be brighter than the picture)
  // Not in the game (tomod's SKIP), so no part of its light either.
  SKIP: new Set(["aoN", "aoW", "aoE", "deep", "shadowBlob", "poolWaterView", "poolBottomView", "poolQuartzView", "surgeBarrierView", "cinGateView", "manGateView"]),
  LAMPS: new Set(["lampShade", "lampWhite", "ugLamp", "seaLantern"]),
};
// The rooms the game builds from the mod's own blocks (the ones the export sends to the game; not the battle halls,
// which go up in the open world and are lit by its sun).
function shadeRooms() {
  return ["house1", "house2", "rival1", "rival2", "lab", "center", "mart", "school", "nickhouse", "gate_s", "gate_n", "gate_league", "gate_johto", "route2_gate", "route2_house", "pewter_gym", "museum_1f", "museum_2f", "pewter_house1", "pewter_house2", "fuchsia_gym", "safari_gate", "fuchsia_office", "fuchsia_house1", "fuchsia_house2", "fuchsia_house3", "fuchsia_house4", "safari_rest_center", "safari_rest_east", "safari_rest_north", "safari_rest_west", "safari_secret_house", "ug_r5", "ug_r6", "ug_r7", "ug_r8", "ug_ns", "ug_ew", "cerulean_gym", "bike_shop", "cerulean_house1", "cerulean_house2", "cerulean_house3", "cerulean_house4", "daycare", "bills_house", "power_plant", "vermilion_gym", "pokemon_fan_club", "vermilion_old_rod_house", "vermilion_trade_house", "vermilion_pidgey_house", "lavender_volunteer_house", "lavender_cubone_house", "lavender_name_rater", "gate_r5", "gate_r6", "gate_r7", "gate_r8", "gate_r11", "gate_r12", "gate_r15", "gate_r16n", "gate_r16s", "gate_r18", "route12_house", "route16_house", "gate_r11_2f", "gate_r12_2f", "gate_r15_2f", "gate_r16_2f", "gate_r18_2f", "fighting_dojo", "saffron_gym", "copycats_house_1f", "copycats_house_2f", "mr_psychics_house", "silph_co_1f", "silph_co_2f", "silph_co_3f", "silph_co_4f", "silph_co_5f", "silph_co_6f", "silph_co_7f", "silph_co_8f", "silph_co_9f", "silph_co_10f", "silph_co_11f", "silph_co_elevator", "game_corner", "game_corner_prize_room", "celadon_mart_1f", "celadon_mart_2f", "celadon_mart_3f", "celadon_mart_4f", "celadon_mart_5f", "celadon_mart_roof", "celadon_mart_elevator", "cinnabar_gym", "cinnabar_lab", "cinnabar_lab_trade_room", "cinnabar_lab_metronome_room", "cinnabar_lab_fossil_room", "pokemon_mansion_1f", "pokemon_mansion_2f", "pokemon_mansion_3f", "pokemon_mansion_b1f", "sprout_tower_1f", "sprout_tower_2f", "sprout_tower_3f", "earls_pokemon_academy", "route_35_national_park_gate", "route_36_national_park_gate", "jgate_r29", "jgate_r43n", "jgate_r43s", "jgate_r31", "jgate_r36", "jgate_r32", "jgate_alph", "jgate_r42", "jgate_ecruteak", "jgate_r35", "jgate_r38", ...Object.keys(K60)];
}
let SHADE_SET = null;
// Whether a room is lit this way: one of the game's rooms, in the fit-out (In game), indoors and plain.
function shadeOn(room) {
  if (!FIT || typeof window !== "undefined" && window.SHADE_OFF || !room || room.outside || room.night || room.haze || room.dark) return false;
  if (!SHADE_SET) SHADE_SET = new Set(shadeRooms());
  return SHADE_SET.has(room.id);
}

// Every face is kept as it's made (the export sends these to the game; the light is worked out over them).
{
  const oface = Geo.prototype.face;
  Geo.prototype.face = function (k, BL, R, U, w, h, n, sub) {
    if (k && w > 0 && h > 0) (this._faces || (this._faces = [])).push({ k, bl: [BL.x, BL.y, BL.z], r: [R.x, R.y, R.z], u: [U.x, U.y, U.z], w, h, n: n ? [n.x, n.y, n.z] : null, sub: sub || null });
    return oface.call(this, k, BL, R, U, w, h, n, sub);
  };
}
// A room's faces that are in the game (the export's), and each one's material as the game has it.
function shadeFaces(R) {
  const out = [];
  const take = fs => { for (const f of fs || []) { const d = MATDEF[f.k]; if (!d || d.overlay || SHADE.SKIP.has(f.k)) continue; out.push(f); } };
  take(R.geo._faces);
  for (const p of R.parts) take(p.geo._faces);
  return out;
}
// How a material's texture repeats in the game (the export's fitPer), as [across, up].
function shadePer(k) {
  const d = MATDEF[k] || {};
  const per = typeof fitPer === "function" && FIT ? fitPer(k, d) : d.per;
  const p = per || 1;
  return Array.isArray(p) ? p : [p, p];
}

// ---------- the pieces: each face cut just as tomod.py cuts it (the same sums, in the same order) ----------
const axisOf = v => { for (let i = 0; i < 3; i++) if (Math.abs(v[i]) > 0.5) return [i, v[i] > 0 ? 1 : -1]; throw new Error("no axis"); };
const cross3 = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
function shadeBlockCuts(start, sign, length) {
  const out = [], a = Math.min(start, start + sign * length), b = Math.max(start, start + sign * length);
  for (let i = Math.floor(a) + 1; i < Math.ceil(b); i++) out.push((i - start) * sign);
  return out;
}
// (0.66) And at the half blocks, so the light has a corner every half block across a floor or a wall.
function shadeHalfCuts(start, sign, length) {
  const out = [], a = Math.min(start, start + sign * length), b = Math.max(start, start + sign * length);
  for (let k = Math.floor(a * 2) + 1; k < Math.ceil(b * 2); k++) if (k % 2 !== 0) out.push((k * 0.5 - start) * sign);
  return out;
}
function shadeCuts(a, b, extra) {
  const EPS = 1e-6, pts = new Set([a, b]);
  for (let i = Math.floor(a) + 1; i < Math.ceil(b); i++) if (a + EPS < i && i < b - EPS) pts.add(i);
  for (const e of extra) if (a + EPS < e && e < b - EPS) pts.add(e);
  return [...pts].sort((x, y) => x - y);
}
// A face's cuts along it (tr) and up it (tu), its axes and which way it faces.
function shadeSlice(f) {
  const bl = f.bl, R = f.r, U = f.u, w = f.w, h = f.h, d = MATDEF[f.k] || {};
  const n = cross3(R, U), [an, sn] = axisOf(n), [ar, sr] = axisOf(R), [au, su] = axisOf(U);
  const [pu, pv] = shadePer(f.k);
  const u0 = (bl[0] * R[0] + bl[1] * R[1] + bl[2] * R[2]) / pu, v0 = (bl[0] * U[0] + bl[1] * U[1] + bl[2] * U[2]) / pv;
  let er = shadeBlockCuts(bl[ar], sr, w).concat(shadeHalfCuts(bl[ar], sr, w)), eu = shadeBlockCuts(bl[au], su, h).concat(shadeHalfCuts(bl[au], su, h));
  if (!d.fit) {
    for (let j = Math.floor(u0) + 1; j < Math.ceil(u0 + w / pu); j++) er.push((j - u0) * pu);
    for (let j = Math.floor(v0) + 1; j < Math.ceil(v0 + h / pv); j++) eu.push((j - v0) * pv);
  }
  return { tr: shadeCuts(0, w, er), tu: shadeCuts(0, h, eu), an, sn, ar, sr, au, su, dir: an * 2 + (sn > 0 ? 0 : 1) };
}
// A corner's key: where it is (to a 64th) and which way its face faces (0 east, 1 west, 2 up, 3 down, 4 south, 5 north).
function shadeKey(x, y, z, dir) {
  const q = v => Math.round(v * SHADE.Q) + SHADE.OFF;
  return ((dir * 16384 + q(x)) * 16384 + q(y)) * 16384 + q(z);
}
const SHADE_N = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]];
// Where round a corner each of its rays starts (a little disc, its radius SPREAD): a sunflower's seeds.
const SHADE_SPREAD = Array.from({ length: 16 }, (_, i) => { const r = Math.sqrt((i + 0.5) / 16), a = i * 2.399963229728653; return [r * Math.cos(a), r * Math.sin(a)]; });

// ---------- the sun and the room's light round about: the same sums as the game's RoomShade ----------
// The sun's way (towards it), from high over the open front, a little west: as the viewer has always lit a room.
function shadeSunDir(W, D) {
  const x = -0.2 * W, y = 18, z = D / 2 + 9, l = Math.hypot(x, y, z);
  return [x / l, y / l, z / l];
}
const hexRGB = h => [(h >> 16 & 255) / 255, (h >> 8 & 255) / 255, (h & 255) / 255];
// By the time of day (k: how low the sun is, side: -1 the morning's, 1 the evening's, m: how far it's night), as the
// fit-out's times had them: the sun's and the sky's light, colour times strength (morning, day, evening, night).
const SHADE_TOD = {
  day: { sun: 0.64, sunC: 0xffffff, hemi: 0.72, hemiC: 0xf4f7fb },
  morning: { sun: 0.6, sunC: 0xfff0d8, hemi: 0.72, hemiC: 0xfff4e6 },
  evening: { sun: 0.42, sunC: 0xffd6aa, hemi: 0.84, hemiC: 0xffecd0 },
  night: { sun: 0.38, sunC: 0xffe2b8, hemi: 0.88, hemiC: 0xffefd6 },
};
const SHADE_GROUND = 0xa89e92, SHADE_AMB = 0.1;
function shadeLightAt(k, side, m, windows) {
  const lerp = (a, b, t) => a.map((v, i) => v + (b[i] - v) * t);
  const of = t => ({ sun: hexRGB(t.sunC).map(v => v * t.sun), sky: hexRGB(t.hemiC).map(v => v * t.hemi), hemi: t.hemi });
  const D0 = of(SHADE_TOD.day), Lo = of(side < 0 ? SHADE_TOD.morning : SHADE_TOD.evening), N = of(SHADE_TOD.night);
  const sun = lerp(lerp(D0.sun, Lo.sun, k), N.sun, m), sky = lerp(lerp(D0.sky, Lo.sky, k), N.sky, m);
  const hemi = D0.hemi + (Lo.hemi - D0.hemi) * k, hemiI = hemi + (N.hemi - hemi) * m;
  const ground = hexRGB(SHADE_GROUND).map(v => v * hemiI);
  // The lamps: off by day, on in the evening and at night; always on in a room with no windows (RoomLightRenderer's).
  const lamp = windows ? Math.max(m, side > 0 ? k : 0) : 1;
  return { sun, sky, ground, amb: SHADE_AMB, lamp };
}
// A corner's colour (r, g, b, each 0 up): from its S and AO (0 to 255), which way it faces, its material.
function shadeColour(Lt, dirV, sunDir, S, AO, mat) {
  const n = SHADE_N[dirV], w = 0.5 * n[1] + 0.5;
  const nl = Math.max(0, n[0] * sunDir[0] + n[1] * sunDir[1] + n[2] * sunDir[2]) * (S / 255);
  const ao = 1 - SHADE.AO_K * (1 - AO / 255);
  const out = [0, 0, 0];
  for (let c = 0; c < 3; c++) {
    const ind = (Lt.amb + Lt.ground[c] + (Lt.sky[c] - Lt.ground[c]) * w) * ao;
    out[c] = mat.mul * (ind + Lt.sun[c] * nl) + (mat.lamp ? 0.04 + (mat.glow * 1.15 - 0.04) * Lt.lamp : mat.glow);
  }
  return out;
}
// A material as the light has it: mul (its colour: the windows' glass and an open way out are their own light, so
// dark), glow (the light it gives off itself), lamp (a lamp's, by the time of day).
function shadeMatOf(k) {
  const d = MATDEF[k] || {};
  return { mul: k === "window" || k === "doorLight" ? 0x1a / 255 : 1, glow: d.glow || 0, lamp: SHADE.LAMPS.has(k) };
}

// ---------- working it out: rays from every corner, through the room's faces ----------
function shadeBake(R) {
  const faces = shadeFaces(R), sunDir = shadeSunDir(R.W, R.D);
  // What's in the way: every face but the ones that give off light or let it through (as the viewer's shadows were);
  // plants (cut out) half.
  const occ = [];
  let lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
  for (const f of faces) {
    const d = MATDEF[f.k] || {}, n = cross3(f.r, f.u), [an, sn] = axisOf(n);
    const a = f.bl, b = [f.bl[0] + f.r[0] * f.w + f.u[0] * f.h, f.bl[1] + f.r[1] * f.w + f.u[1] * f.h, f.bl[2] + f.r[2] * f.w + f.u[2] * f.h];
    const mn = [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])], mx = [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])];
    for (let i = 0; i < 3; i++) { lo[i] = Math.min(lo[i], mn[i]); hi[i] = Math.max(hi[i], mx[i]); }
    // (Nor the thin things, a table's legs and rims, thinner than a pixel or two: at a corner every half block their
    // shadows would only come out as streaks.)
    if (d.glow || d.alpha || Math.min(f.w, f.h) < SHADE.THIN) continue;
    occ.push({ an, sn, c: a[an], mn, mx, t: d.cut ? 0.5 : 0 });
  }
  if (!faces.length) return { map: new Map(), sunDir };
  // A grid of half blocks over the room, each with the faces that pass through it.
  const G = 0.5, g0 = lo.map(v => Math.floor(v / G) * G - G), gn = hi.map((v, i) => Math.ceil((v - g0[i]) / G) + 1);
  const NX = gn[0], NY = gn[1], NZ = gn[2], NC = NX * NY * NZ, NO = occ.length;
  // (Kept in flat arrays: the rays are many.)
  const fan = new Int8Array(NO), fsn = new Int8Array(NO), fc = new Float64Array(NO), fmn = new Float64Array(NO * 3), fmx = new Float64Array(NO * 3), ft = new Float64Array(NO);
  const range = (o, a) => [Math.max(0, Math.floor((o.mn[a] - 1e-4 - g0[a]) / G)), Math.min(gn[a] - 1, Math.floor((o.mx[a] + 1e-4 - g0[a]) / G))];
  const count = new Int32Array(NC + 1);
  occ.forEach((o, i) => {
    fan[i] = o.an; fsn[i] = o.sn; fc[i] = o.c; ft[i] = o.t;
    for (let a = 0; a < 3; a++) { fmn[i * 3 + a] = o.mn[a]; fmx[i * 3 + a] = o.mx[a]; }
    const [i0, i1] = range(o, 0), [j0, j1] = range(o, 1), [k0, k1] = range(o, 2);
    for (let x = i0; x <= i1; x++) for (let y = j0; y <= j1; y++) for (let z = k0; z <= k1; z++) count[(x * NY + y) * NZ + z + 1]++;
  });
  for (let c = 0; c < NC; c++) count[c + 1] += count[c];
  const list = new Int32Array(count[NC]), fill = count.slice(0, NC);
  occ.forEach((o, i) => {
    const [i0, i1] = range(o, 0), [j0, j1] = range(o, 1), [k0, k1] = range(o, 2);
    for (let x = i0; x <= i1; x++) for (let y = j0; y <= j1; y++) for (let z = k0; z <= k1; z++) list[fill[(x * NY + y) * NZ + z]++] = i;
  });
  let stamp = 1;
  const seen = new Int32Array(NO);
  // Along a ray from (ox, oy, oz) (direction dx, dy, dz, unit) up to tmax: what fraction of light gets through (0 to
  // 1), or (nearest) how far to the nearest thing it meets. front: only faces met from the front (as shadows were cast).
  function trace(ox, oy, oz, dx, dy, dz, tmax, front, nearest) {
    stamp++;
    let ix = Math.floor((ox - g0[0]) / G), iy = Math.floor((oy - g0[1]) / G), iz = Math.floor((oz - g0[2]) / G);
    const sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
    let tx = dx === 0 ? Infinity : (g0[0] + (ix + (dx > 0 ? 1 : 0)) * G - ox) / dx;
    let ty = dy === 0 ? Infinity : (g0[1] + (iy + (dy > 0 ? 1 : 0)) * G - oy) / dy;
    let tz = dz === 0 ? Infinity : (g0[2] + (iz + (dz > 0 ? 1 : 0)) * G - oz) / dz;
    const ddx = dx === 0 ? Infinity : G / Math.abs(dx), ddy = dy === 0 ? Infinity : G / Math.abs(dy), ddz = dz === 0 ? Infinity : G / Math.abs(dz);
    let pass = 1, near = Infinity, t = 0;
    while (t <= tmax && ix >= 0 && iy >= 0 && iz >= 0 && ix < NX && iy < NY && iz < NZ) {
      const c = (ix * NY + iy) * NZ + iz;
      for (let q = count[c], qe = count[c + 1]; q < qe; q++) {
        const fi = list[q];
        if (seen[fi] === stamp) continue;
        seen[fi] = stamp;
        const an = fan[fi], dn = an === 0 ? dx : an === 1 ? dy : dz;
        if (dn > -1e-12 && dn < 1e-12) continue;
        if (front && dn * fsn[fi] >= 0) continue;
        const th = (fc[fi] - (an === 0 ? ox : an === 1 ? oy : oz)) / dn;
        if (th < -1e-7 || th > tmax) continue;
        const px = ox + dx * th, py = oy + dy * th, pz = oz + dz * th, b = fi * 3;
        if (an !== 0 && (px < fmn[b] - 1e-6 || px > fmx[b] + 1e-6)) continue;
        if (an !== 1 && (py < fmn[b + 1] - 1e-6 || py > fmx[b + 1] + 1e-6)) continue;
        if (an !== 2 && (pz < fmn[b + 2] - 1e-6 || pz > fmx[b + 2] + 1e-6)) continue;
        if (nearest) { if (th < near) near = th; }
        else { pass *= ft[fi]; if (pass <= 0) return 0; }
      }
      if (tx < ty && tx < tz) { if (nearest && near <= tx) break; t = tx; tx += ddx; ix += sx; }
      else if (ty < tz) { if (nearest && near <= ty) break; t = ty; ty += ddy; iy += sy; }
      else { if (nearest && near <= tz) break; t = tz; tz += ddz; iz += sz; }
    }
    return nearest ? near : pass;
  }
  // The patterns: rays to the sun round its way, and rays round a corner (more towards straight out).
  const perp = v => { const a = Math.abs(v[0]) < 0.9 ? [1, 0, 0] : [0, 1, 0], e1 = cross3(v, a), l1 = Math.hypot(...e1); const u = e1.map(x => x / l1); return [u, cross3(v, u)]; };
  const [e1, e2] = perp(sunDir), sunRays = [];
  for (let i = 0; i < SHADE.SUN_RAYS; i++) {
    const r = Math.sqrt((i + 0.5) / SHADE.SUN_RAYS) * SHADE.CONE, ph = i * 2.399963229728653;
    const v = [0, 1, 2].map(a => sunDir[a] + r * Math.cos(ph) * e1[a] + r * Math.sin(ph) * e2[a]), l = Math.hypot(...v);
    sunRays.push(v.map(x => x / l));
  }
  const aoLocal = [];
  for (let i = 0; i < SHADE.AO_RAYS; i++) {
    const r = Math.sqrt((i + 0.5) / SHADE.AO_RAYS), ph = i * 2.399963229728653;
    aoLocal.push([r * Math.cos(ph), r * Math.sin(ph), Math.sqrt(Math.max(0, 1 - r * r))]);
  }
  const aoRays = SHADE_N.map(n => { const [t1, t2] = perp(n); return aoLocal.map(([x, y, z]) => [0, 1, 2].map(a => x * t1[a] + y * t2[a] + z * n[a])); });
  // Every corner of every piece.
  const map = new Map();
  for (const f of faces) {
    const s = shadeSlice(f);
    // Each ray starts from a little way round the corner, over the face (so a thin thing's shadow, a table leg's,
    // comes out as a faint blur rather than a streak between two corners), never past the face's own edges.
    const at = (a, b, da, db, lift) => {
      const ta = Math.max(0, Math.min(f.w, a + da)), tb = Math.max(0, Math.min(f.h, b + db)), q = [0, 0, 0], n = SHADE_N[s.dir];
      q[s.ar] = f.bl[s.ar] + s.sr * ta; q[s.au] = f.bl[s.au] + s.su * tb; q[s.an] = f.bl[s.an];
      return [q[0] + n[0] * lift, q[1] + n[1] * lift, q[2] + n[2] * lift];
    };
    for (const a of s.tr) for (const b of s.tu) {
      const p = [0, 0, 0]; p[s.ar] = f.bl[s.ar] + s.sr * a; p[s.au] = f.bl[s.au] + s.su * b; p[s.an] = f.bl[s.an];
      const key = shadeKey(p[0], p[1], p[2], s.dir);
      if (map.has(key)) continue;
      const n = SHADE_N[s.dir];
      // To the sun.
      let S = 0;
      if (n[0] * sunDir[0] + n[1] * sunDir[1] + n[2] * sunDir[2] > 0) {
        let sum = 0;
        for (let i = 0; i < sunRays.length; i++) {
          const d = sunRays[i], [da, db] = SHADE_SPREAD[(i * 3) % SHADE_SPREAD.length], o = at(a, b, da * SHADE.SPREAD, db * SHADE.SPREAD, 0.004);
          sum += trace(o[0], o[1], o[2], d[0], d[1], d[2], 64, true, false);
        }
        S = sum / sunRays.length;
      }
      // Round about. (Not for the faces the game's camera never sees, looking down from the south: undersides and
      // the backs of things, facing north.)
      if (s.dir === 3 || s.dir === 5) { map.set(key, [Math.round(S * 255), 255]); continue; }
      let shut = 0;
      const rays = aoRays[s.dir];
      for (let i = 0; i < rays.length; i++) {
        const d = rays[i], [da, db] = SHADE_SPREAD[(i * 5) % SHADE_SPREAD.length], o = at(a, b, da * SHADE.SPREAD * 0.6, db * SHADE.SPREAD * 0.6, 0.002);
        const t = trace(o[0], o[1], o[2], d[0], d[1], d[2], SHADE.AO_REACH, false, true);
        if (t < SHADE.AO_REACH) shut += 1 - t / SHADE.AO_REACH;
      }
      const AO = 1 - shut / SHADE.AO_RAYS;
      map.set(key, [Math.round(S * 255), Math.round(AO * 255)]);
    }
  }
  return { map, sunDir };
}

// ---------- drawing it (the viewer's In game view) ----------
// The room's pieces, each corner coloured, in meshes by material (no lamps of the viewer's own: the colours are the light).
function shadeBuild(R) {
  const t0 = performance.now();
  const B = R._shade = shadeBake(R);
  const faces = shadeFaces(R), groups = {};
  const V3 = (a) => new THREE.Vector3(a[0], a[1], a[2]);
  const geo = new Geo();
  for (const f of faces) {
    const s = shadeSlice(f), d = MATDEF[f.k] || {};
    const g = groups[f.k] || (groups[f.k] = { pos: [], uv: [], col: [], idx: [], meta: [] });
    const R3 = V3(f.r), U3 = V3(f.u), BL0 = V3(f.bl);
    const [s0, t0s, s1, t1s] = f.sub || [0, 0, 1, 1];
    for (let i = 0; i + 1 < s.tr.length; i++) for (let j = 0; j + 1 < s.tu.length; j++) {
      const a0 = s.tr[i], a1 = s.tr[i + 1], b0 = s.tu[j], b1 = s.tu[j + 1];
      if (a1 - a0 < 1e-4 || b1 - b0 < 1e-4) continue;
      const pw = a1 - a0, ph = b1 - b0;
      const BL = BL0.clone().addScaledVector(R3, a0).addScaledVector(U3, b0);
      const sub = d.fit ? [s0 + (s1 - s0) * a0 / f.w, t0s + (t1s - t0s) * b0 / f.h, s0 + (s1 - s0) * a1 / f.w, t0s + (t1s - t0s) * b1 / f.h] : null;
      const uv = geo.faceUV(f.k, BL, R3, U3, pw, ph, sub);
      const base = g.pos.length / 3;
      for (const [ta, tb] of [[a0, b0], [a1, b0], [a1, b1], [a0, b1]]) {
        const p = [0, 0, 0]; p[s.ar] = f.bl[s.ar] + s.sr * ta; p[s.au] = f.bl[s.au] + s.su * tb; p[s.an] = f.bl[s.an];
        g.pos.push(p[0], p[1], p[2]);
        const v = B.map.get(shadeKey(p[0], p[1], p[2], s.dir)) || [255, 255];
        g.meta.push(s.dir, v[0], v[1]);
      }
      g.uv.push(...uv);
      g.idx.push(base + 3, base, base + 1, base + 3, base + 1, base + 2);
    }
  }
  const grp = new THREE.Group();
  R._shadeMeshes = [];
  for (const [k, g] of Object.entries(groups)) {
    if (!g.idx.length) continue;
    const bg = new THREE.BufferGeometry();
    bg.setAttribute("position", new THREE.Float32BufferAttribute(g.pos, 3));
    bg.setAttribute("uv", new THREE.Float32BufferAttribute(g.uv, 2));
    bg.setAttribute("color", new THREE.Float32BufferAttribute(new Array(g.pos.length).fill(1), 3));
    bg.setIndex(g.idx);
    const mesh = new THREE.Mesh(bg, shadeMaterial(k));
    mesh.userData.shade = { meta: g.meta, mat: shadeMatOf(k) };
    grp.add(mesh); R._shadeMeshes.push(mesh);
  }
  R._shadeMs = Math.round(performance.now() - t0);
  return grp;
}
// A material for the lit pieces: its picture times the corners' colours, nothing else (as the game draws it).
function shadeMaterial(k) {
  const key = "shade:" + k;
  if (matCache[key]) return matCache[key];
  const d = MATDEF[k];
  const map = d.dyn ? d.dyn.tex : pixTex(d.tex);
  const m = new THREE.MeshBasicMaterial({ map, vertexColors: true });
  if (d.alpha) { m.transparent = true; m.opacity = d.alpha; m.depthWrite = false; }
  if (d.cut) { m.alphaTest = 0.5; m.side = THREE.DoubleSide; }
  if (d.double) m.side = THREE.DoubleSide;
  return (matCache[key] = m);
}
// The colours at a time of day (TIMES' k, side, m).
function shadeTime(R, t) {
  if (!R || !R._shadeMeshes) return;
  const Lt = shadeLightAt(t.k, t.side, t.m, !!(R.pools && R.pools.length)), sd = R._shade.sunDir;
  for (const mesh of R._shadeMeshes) {
    const { meta, mat } = mesh.userData.shade, col = mesh.geometry.attributes.color;
    for (let v = 0; v < meta.length / 3; v++) {
      const c = shadeColour(Lt, meta[v * 3], sd, meta[v * 3 + 1], meta[v * 3 + 2], mat);
      // (As the game keeps it: a byte each, the shader's SCALE; the plain game without shaders can't go past 1.)
      col.setXYZ(v, ...c.map(x => Math.round(Math.min(SHADE.SCALE, x) / SHADE.SCALE * 255) / 255 * SHADE.SCALE));
    }
    col.needsUpdate = true;
  }
}
// The light's file for the game (retromon/interiors/light/<room>.bin): "RLT1", whether it has windows, the number of
// corners, their keys (sorted, each the step up from the last, 7 bits a byte), then each one's S, then each one's AO.
function shadeFile(R) {
  const B = R._shade || shadeBake(R);
  const keys = [...B.map.keys()].sort((a, b) => a - b), bytes = [82, 76, 84, 49, R.pools && R.pools.length ? 1 : 0];
  const n = keys.length;
  bytes.push(n >>> 24 & 255, n >>> 16 & 255, n >>> 8 & 255, n & 255);
  let last = 0;
  for (const k of keys) {
    let d = k - last; last = k;
    while (d >= 128) { bytes.push(d % 128 | 128); d = Math.floor(d / 128); }
    bytes.push(d);
  }
  for (const k of keys) bytes.push(B.map.get(k)[0]);
  for (const k of keys) bytes.push(B.map.get(k)[1]);
  let s = "";
  for (let i = 0; i < bytes.length; i += 8192) s += String.fromCharCode.apply(null, bytes.slice(i, i + 8192));
  return { b64: btoa(s), n };
}
// The fit-out's potted plants (rooms66's fitGreen), once: put in before the room's light is worked out, and sent to
// the game with the rest (they were only in the viewer before).
function shadeGreen(R) {
  if (FIT && !R._greened && typeof fitGreen === "function") { R._greened = true; try { R._green = fitGreen(R); } catch (e) { R._greenErr = String(e); } }
}
// The game's rooms, in the fit-out: drawn lit (their moving parts too, standing still, as they are in the game).
{
  const ofinish = Room.prototype.finish;
  Room.prototype.finish = function () {
    if (!shadeOn(this)) return ofinish.call(this);
    // (The fit-out's greenery first, as the export puts it in the game too.)
    shadeGreen(this);
    this.group.add(shadeBuild(this));
    for (const p of this.parts) this.group.add(p.grp);
  };
}

// ---------- (0.66, John's 13:36) Metal and glass the same everywhere ----------
// "the glass tables in the poker center, do they match the glass tables now we have everywhere else? And anything
// which is maybe a bit more metallic ... the blue beams on the side ... I think they should react the same ... the
// glass tables ... the white lines, they're squared, and they just probably don't need to be."
// The glass: a clean pale tint, no streak (the streak was a line a pixel wide, laid four blocks over a texture of 16,
// so in the game it stood out as big square steps). A glass table's top: its own picture over the whole top, a soft
// sheen across one corner, no hard lines; every glass table has it (they're all one: glassTable).
// The metal (the Center's and the Mart's blue posts, the brushed steel bands, the chrome trims): each face its whole
// picture (not a corner of one laid over four blocks), so a post shows its round sheen down it, a band its bright top
// edge and its shade beneath, wherever they are.
{
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const mix = (a, b, t) => [0, 1, 2].map(i => Math.round(a[i] + (b[i] - a[i]) * Math.max(0, Math.min(1, t))));
  TEXDEF.glass = [16, 16, (P, r) => P.fill(hx("#CFE9F2"), 0.015, r)];
  reg("glassTop", 32, 32, (P, r) => {
    const base = hx("#CFE9F2"), hi = hx("#FFFFFF"), rim = hx("#E8F6FA");
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      // A soft band of sheen across the top left corner, fading out either side.
      const d = (x + y - 18) / 7, band = Math.exp(-d * d) * 0.55, d2 = (x + y - 30) / 4, band2 = Math.exp(-d2 * d2) * 0.18;
      let c = mix(base, hi, band + band2);
      if (x === 0 || y === 0) c = mix(c, rim, 0.6);
      P.set(x, y, c);
    }
  }, { fit: true, alpha: 0.38 });
  // The metal: a round post's sheen (light down its left third, its edges darker), brushed steel's bright top and
  // shaded foot, chrome's bands.
  const sheen = (P, base, light, dark) => {
    for (let x = 0; x < 16; x++) {
      const u = (x + 0.5) / 16, k = Math.exp(-(((u - 0.32) / 0.13) ** 2)) * 0.75 - Math.max(0, u - 0.55) * 0.9 - Math.max(0, 0.08 - u) * 3;
      const c = k >= 0 ? mix(base, light, k) : mix(base, dark, -k);
      for (let y = 0; y < 16; y++) P.set(x, y, c);
    }
  };
  reg("postBlueMetal", 16, 16, P => sheen(P, hx("#3E7FCF"), hx("#A9CFF6"), hx("#1F4C86")), { fit: true });
  reg("brushedSteel", 16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) { const k = (r() - 0.5) * 0.05; for (let x = 0; x < 16; x++) P.set(x, y, shadec(mix(hx("#E8EDF2"), hx("#9AA3AD"), y / 15), k)); }
    P.rect(0, 0, 16, 2, hx("#F7F9FB")); P.rect(0, 15, 16, 1, hx("#7F8893"));
  }, { fit: true });
  reg("chromeTrim", 16, 16, P => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, mix(hx("#FAFBFC"), hx("#8A939D"), (y / 15) ** 1.4)); }, { fit: true });
  // Every glass table's top in the one picture.
  const gt = glassTable;
  glassTable = function (room, x0, z0, x1, z1, o = {}) {
    const G = room.geo, ob = G.box;
    G.box = function (a, b, c, d, e, f, spec) { return ob.call(this, a, b, c, d, e, f, spec === "glass" ? { all: "glass", top: "glassTop" } : spec); };
    try { return gt(room, x0, z0, x1, z1, o); } finally { G.box = ob; }
  };
}
