// ---------- (0.67, John's 16:28) The S.S. ANNE from outside, in our looks: for review only ----------
// "I want you to present me in the viewer ... this will be an exterior kind of, the ship updated, and then obviously
// for me to pan around it ... whatever colours exist now, it's just focusing on those and making them within our
// textural changes. It's purely for review first."
// John's ship, block for block (tools/shipview.py reads it out of his world: ship_data.js), every kind of block in its
// own colour (its own picture's) drawn in the family of our looks it belongs to: painted steel clean and seamless (no
// block lines; the hull's whites, its blues and greys, the funnels' stripes), timber in clean boards, metal brushed
// with a sheen, glass tinted with a soft streak of light, lamps lit. Its shapes as Minecraft has them (slabs, stairs,
// trapdoors, rails and posts, rods). Only what can be seen from outside it. The game's ship isn't changed: this is to
// look at first. Drag to turn round it, right-drag or two fingers (or Shift and drag) to move across, the wheel or a
// pinch to come in and out; W A S D move across too.
(function () {
  if (typeof SHIP === "undefined") return;
  const D = SHIP;
  PLACES.push({ id: "ss_anne_outside", name: "S.S. ANNE (outside)", floor: "In our looks, for review: the game's ship unchanged", group: "Ships (for review)" });

  // ===================================== materials =====================================
  const clamp = v => Math.max(0, Math.min(255, v));
  const lumOf = c => 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2];
  // A kind's own colour, made clean: near-whites whiter and even, the rest a touch richer.
  function clean(c) {
    const l = lumOf(c), mx = Math.max(...c), mn = Math.min(...c), sat = mx ? (mx - mn) / mx : 0;
    if (l > 178 && sat < 0.16) return mixc(c, [238, 238, 233], 0.55);
    const g = l, k = 1.12;
    return [0, 1, 2].map(i => clamp(g + (c[i] - g) * k + 4));
  }
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const STYLE = {
    // Painted steel: one clean colour laid four blocks across, the faintest unevenness, no lines (seamless).
    paint: (k, c) => reg(k, 64, 64, (P, r) => { P.fill(c, 0.012, r); }, { per: 4 }),
    // Timber: clean boards, four to a block, a soft line between each and its lit edge.
    wood: (k, c) => reg(k, 16, 16, (P, r) => {
      P.fill(c, 0.02, r);
      for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, shadec(c, 0.1)); P.rect(0, y + 3, 16, 1, shadec(c, -0.16)); }
    }),
    // Metal: brushed (fine streaks along it), a soft sheen across, laid two blocks across.
    metal: (k, c) => reg(k, 32, 32, (P, r) => {
      for (let y = 0; y < 32; y++) { const s = (r() - 0.5) * 0.05; for (let x = 0; x < 32; x++) { const b = 0.5 + 0.5 * Math.sin((x + y) / 32 * Math.PI * 2); P.set(x, y, shadec(c, s + 0.14 * Math.pow(b, 8) - 0.03)); } }
    }, { per: 2 }),
    // Glass: its colour taken darker for the panes (the light off the sea on it), a soft streak of light.
    glass: (k, c, dark) => reg(k, 16, 16, (P, r) => {
      const g = dark ? mixc(c, [24, 38, 58], 0.55) : mixc(c, [150, 190, 214], 0.35);
      P.fill(g, 0.015, r);
      for (let i = 0; i < 16; i++) { P.set(i, 15 - i, shadec(g, 0.28)); if (i > 2) P.set(i, 18 - i, shadec(g, 0.16)); }
    }, { alpha: dark ? 0.9 : 0.62 }),
    // Lamps: lit.
    glow: (k, c) => reg(k, 16, 16, (P, r) => { P.fill(mixc(c, [255, 248, 230], 0.5), 0.02, r); }, { glow: 0.65 }),
    leaves: (k, c) => reg(k, 16, 16, (P, r) => { P.fill(hx("#5AA548"), 0.06, r); for (let i = 0; i < 14; i++) P.set(r() * 16, r() * 16, hx("#86CC68")); }),
  };
  const KEY = {};
  for (const [b, kd] of Object.entries(D.kinds)) {
    const k = "ship_" + b, c = clean(kd.rgb);
    (STYLE[kd.fam] || STYLE.paint)(k, c, b.startsWith("black_") || b.startsWith("gray_") || b === "glass" || b === "tinted_glass");
    KEY[b] = k;
  }
  reg("shipSea", 32, 32, (P, r) => { P.fill(hx("#3B8FC9"), 0.03, r); for (let i = 0; i < 10; i++) { const x = r() * 28, y = r() * 32; P.rect(x, y, 4, 1, hx("#6DB6E4")); } }, { per: 4, alpha: 0.78 });

  // ===================================== shapes =====================================
  const p16 = v => v / 16;
  const REG = { north: [0, 0, 1, 0.5], south: [0, 0.5, 1, 1], east: [0.5, 0, 1, 1], west: [0, 0, 0.5, 1] };
  const CCW = { north: "west", west: "south", south: "east", east: "north" }, CW = { north: "east", east: "south", south: "west", west: "north" }, OPP = { north: "south", south: "north", east: "west", west: "east" };
  const cut = (a, b) => [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.min(a[2], b[2]), Math.min(a[3], b[3])];
  function parse(state) {
    const i = state.indexOf("["), p = {};
    if (i >= 0) for (const kv of state.slice(i + 1, -1).split(",")) { const [k, v] = kv.split("="); p[k] = v; }
    return [i >= 0 ? state.slice(0, i) : state, p];
  }
  // A state's boxes in the block ([x0, y0, z0, x1, y1, z1]); null: a whole block.
  function shape(state) {
    const [b, p] = parse(state);
    if (b.endsWith("_slab")) return p.type === "double" ? null : p.type === "top" ? [[0, 0.5, 0, 1, 1, 1]] : [[0, 0, 0, 1, 0.5, 1]];
    if (b.endsWith("_stairs")) {
      const top = p.half === "top", f = p.facing, sh = p.shape;
      const base = top ? [0, 0.5, 0, 1, 1, 1] : [0, 0, 0, 1, 0.5, 1], y0 = top ? 0 : 0.5, y1 = top ? 0.5 : 1;
      let rs = [REG[f]];
      if (sh === "outer_left") rs = [cut(REG[f], REG[CCW[f]])];
      else if (sh === "outer_right") rs = [cut(REG[f], REG[CW[f]])];
      else if (sh === "inner_left") rs = [REG[f], cut(REG[OPP[f]], REG[CCW[f]])];
      else if (sh === "inner_right") rs = [REG[f], cut(REG[OPP[f]], REG[CW[f]])];
      return [base, ...rs.map(r => [r[0], y0, r[1], r[2], y1, r[3]])];
    }
    if (b.endsWith("_trapdoor")) {
      const t = p16(3);
      if (p.open === "true") return [{ north: [0, 0, 1 - t, 1, 1, 1], south: [0, 0, 0, 1, 1, t], east: [0, 0, 0, t, 1, 1], west: [1 - t, 0, 0, 1, 1, 1] }[p.facing]];
      return [p.half === "top" ? [0, 1 - t, 0, 1, 1, 1] : [0, 0, 0, 1, t, 1]];
    }
    if (b.endsWith("_door")) {
      const t = p16(3), dirs = ["north", "east", "south", "west"];
      let d = p.facing;
      if (p.open === "true") d = p.hinge === "right" ? CCW[d] : CW[d];
      void dirs;
      return [{ north: [0, 0, 1 - t, 1, 1, 1], south: [0, 0, 0, 1, 1, t], west: [1 - t, 0, 0, 1, 1, 1], east: [0, 0, 0, t, 1, 1] }[d]];
    }
    const arms = (w0, w1, y0, y1, on) => {
      const o = [];
      if (on("north")) o.push([w0, y0, 0, w1, y1, w0]);
      if (on("south")) o.push([w0, y0, w1, w1, y1, 1]);
      if (on("west")) o.push([0, y0, w0, w0, y1, w1]);
      if (on("east")) o.push([w1, y0, w0, 1, y1, w1]);
      return o;
    };
    if (b.endsWith("_fence")) return [[p16(6), 0, p16(6), p16(10), 1, p16(10)], ...arms(p16(7), p16(9), p16(12), p16(15), s => p[s] === "true"), ...arms(p16(7), p16(9), p16(6), p16(9), s => p[s] === "true")];
    if (b.endsWith("_fence_gate")) return p.open === "true" ? [[0, p16(5), p16(7), p16(2), 1, p16(9)], [p16(14), p16(5), p16(7), 1, 1, p16(9)]]
      : [(p.facing === "north" || p.facing === "south") ? [0, p16(5), p16(7), 1, p16(15), p16(9)] : [p16(7), p16(5), 0, p16(9), p16(15), 1]];
    if (b.endsWith("_wall")) {
      const o = [];
      if (p.up !== "false") o.push([p16(4), 0, p16(4), p16(12), 1, p16(12)]);
      for (const s of ["north", "south", "east", "west"]) if (p[s] && p[s] !== "none") o.push(...arms(p16(5), p16(11), 0, p[s] === "tall" ? 1 : p16(14), q => q === s));
      return o.length ? o : [[p16(4), 0, p16(4), p16(12), 1, p16(12)]];
    }
    if (b === "iron_bars" || b.endsWith("_pane")) return [[p16(7), 0, p16(7), p16(9), 1, p16(9)], ...arms(p16(7), p16(9), 0, 1, s => p[s] === "true")];
    if (b.endsWith("_carpet")) return [[0, 0, 0, 1, p16(1), 1]];
    if (b.endsWith("_button")) {
      if (p.face === "floor") return [[p16(5), 0, p16(6), p16(11), p16(2), p16(10)]];
      if (p.face === "ceiling") return [[p16(5), p16(14), p16(6), p16(11), 1, p16(10)]];
      return [{ north: [p16(5), p16(6), p16(14), p16(11), p16(10), 1], south: [p16(5), p16(6), 0, p16(11), p16(10), p16(2)], east: [0, p16(6), p16(5), p16(2), p16(10), p16(11)], west: [p16(14), p16(6), p16(5), 1, p16(10), p16(11)] }[p.facing]];
    }
    if (b === "end_rod") {
      const f = p.facing;
      if (f === "up" || f === "down") return [[p16(7), 0, p16(7), p16(9), 1, p16(9)]];
      if (f === "north" || f === "south") return [[p16(7), p16(7), 0, p16(9), p16(9), 1]];
      return [[0, p16(7), p16(7), 1, p16(9), p16(9)]];
    }
    if (b === "iron_chain" || b === "chain") {
      const a = p.axis || "y";
      return [a === "y" ? [p16(7), 0, p16(7), p16(9), 1, p16(9)] : a === "x" ? [0, p16(7), p16(7), 1, p16(9), p16(9)] : [p16(7), p16(7), 0, p16(9), p16(9), 1]];
    }
    if (b === "ladder") return [{ north: [0, 0, p16(13), 1, 1, 1], south: [0, 0, 0, 1, 1, p16(3)], west: [p16(13), 0, 0, 1, 1, 1], east: [0, 0, 0, p16(3), 1, 1] }[p.facing]];
    if (b === "lantern") return [[p16(5), p.hanging === "true" ? p16(1) : 0, p16(5), p16(11), p.hanging === "true" ? p16(10) : p16(9), p16(11)]];
    if (b === "anvil") return [[p16(2), 0, p16(2), p16(14), 1, p16(14)]];
    if (b === "grindstone") return [[p16(4), p16(4), p16(2), p16(12), p16(14), p16(14)]];
    if (b === "daylight_detector") return [[0, 0, 0, 1, p16(6), 1]];
    if (b === "player_head") return [[p16(4), 0, p16(4), p16(12), p16(8), p16(12)]];
    if (b === "player_wall_head") return [{ north: [p16(4), p16(4), p16(8), p16(12), p16(12), 1], south: [p16(4), p16(4), 0, p16(12), p16(12), p16(8)], west: [p16(8), p16(4), p16(4), 1, p16(12), p16(12)], east: [0, p16(4), p16(4), p16(8), p16(12), p16(12)] }[p.facing] || [p16(4), p16(4), p16(4), p16(12), p16(12), p16(12)]];
    if (b === "snow") return [[0, 0, 0, 1, p16(2 * (+p.layers || 1)), 1]];
    return null;
  }

  // ===================================== the ship =====================================
  BUILD.ss_anne_outside = () => {
    const R = new Room("ss_anne_outside", { name: "S.S. ANNE (outside)", floor: "In our looks, for review: the game's ship unchanged", W: D.W, D: D.D, H: D.H, outside: true });
    R.orbit = { x: D.W / 2, y: 18, z: D.D / 2, yaw: 0.55, pitch: 0.36, dist: 175 };
    R.spawn = { x: D.W / 2, z: D.D / 2, yaw: 0 };
    R.outsideSize = `${D.W} by ${D.D}, ${D.H} high (${D.count} blocks shown)`;
    const G = R.geo, bytes = atob(D.cells), shapes = D.palette.map(shape), kinds = D.palette.map(s => KEY[parse(s)[0]] || "paint");
    for (let i = 0; i + 5 < bytes.length; i += 6) {
      const x = bytes.charCodeAt(i), y = bytes.charCodeAt(i + 1), z = bytes.charCodeAt(i + 2), p = bytes.charCodeAt(i + 3) | (bytes.charCodeAt(i + 4) << 8), m = bytes.charCodeAt(i + 5);
      const k = kinds[p], s = shapes[p];
      if (!s) {
        G.box(x, y, z, x + 1, y + 1, z + 1, { top: m & 1 ? k : null, bottom: m & 2 ? k : null, s: m & 4 ? k : null, n: m & 8 ? k : null, e: m & 16 ? k : null, w: m & 32 ? k : null });
        continue;
      }
      for (const b of s) G.box(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], k);
    }
    // The sea round it.
    const M = 60;
    G.box(-M, D.sea - 0.14, -M, D.W + M, D.sea - 0.12, D.D + M, { top: "shipSea", all: null });
    R.item("Ship", "John's S.S. ANNE as it stands on the map, each kind of block in its own colour in our looks: painted steel clean and seamless, timber boards, brushed metal, tinted glass, lamps lit. For review: the game's ship is as John built it.");
    R.item("Looking round", "Drag to turn round it; right-drag, two fingers or Shift and drag to move across; the wheel or a pinch to come in and out; W A S D move across too.");
    return R;
  };
})();
