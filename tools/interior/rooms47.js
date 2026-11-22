// ---------- 0.47.0 (16:17 and 16:20): the Underground Paths, Cerulean City's insides, the Day Care, Bill's Sea Cottage ----------
// (Loaded after rooms.js: its PLACES and BUILD get these rooms.)
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };

  // ----- the Underground Path's entrances (FireRed's: warm gold tiles, cream walls, a bracket of counter either side) -----
  reg("ugFloor", 16, 16, (P, r) => {
    P.fill(hx("#E2BE72"), 0.035, r);
    P.rect(0, 0, 16, 1, hx("#C9A158")); P.rect(0, 0, 1, 16, hx("#C9A158"));
    for (let i = 0; i < 4; i++) { P.set(7 - i, 4 + i, hx("#F0D594")); P.set(8 + i, 4 + i, hx("#F0D594")); P.set(7 - i, 11 - i, hx("#F0D594")); P.set(8 + i, 11 - i, hx("#F0D594")); }
  });
  reg("ugWall", 16, 16, (P, r) => { P.fill(hx("#F3EAD3"), 0.02, r); P.rect(0, 0, 1, 16, hx("#E4D8BA")); P.rect(8, 0, 1, 16, hx("#EADFC3")); });
  reg("ugPanel", 16, 16, (P, r) => { P.fill(hx("#5C7694"), 0.03, r); P.rect(0, 0, 16, 1, hx("#86A0BC")); P.rect(0, 15, 16, 1, hx("#43586F")); P.rect(7, 1, 1, 14, hx("#506985")); });
  reg("ugTrim", 16, 16, (P, r) => { P.fill(hx("#3E4C5E"), 0.03, r); P.rect(0, 0, 16, 2, hx("#5D6E84")); });
  reg("ugCounter", 16, 16, (P, r) => { P.fill(hx("#EFE1BC"), 0.025, r); P.rect(0, 0, 16, 2, hx("#FBF3DC")); P.rect(0, 13, 16, 3, hx("#9C8A62")); });
  reg("ugTop", 16, 16, (P, r) => { P.fill(hx("#D7DADF"), 0.03, r); P.rect(0, 0, 16, 1, hx("#F0F2F5")); P.rect(0, 15, 16, 1, hx("#AEB3BA")); });
  reg("ugSign", 64, 16, (P, r) => {
    P.fill(hx("#2F4A6E"), 0.02, r); P.rect(0, 0, 64, 1, hx("#6E8DB4")); P.rect(0, 15, 64, 1, hx("#1E3048"));
    P.text("UNDERGROUND PATH", 2, 5, hx("#F6F1E2"));
  }, { fit: true });
  // ----- the tunnels: a long floor in bands (FireRed's: tan, then blue, then red, then tan again), grey tiled walls, lamps -----
  const lane = (base, lt, dk) => (P, r) => { P.fill(hx(base), 0.03, r); for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx(lt)); P.rect(0, y + 3, 16, 1, hx(dk)); } };
  reg("ugLane", 16, 16, lane("#B7A274", "#CBB98A", "#9C8960"));
  reg("ugLaneBlue", 16, 16, lane("#7FA3C6", "#9DBCDA", "#62849F"));
  reg("ugLaneRed", 16, 16, lane("#C7826E", "#DB9C88", "#A6685A"));
  const laneX = (base, lt, dk) => (P, r) => { P.fill(hx(base), 0.03, r); for (let x = 0; x < 16; x += 4) { P.rect(x, 0, 1, 16, hx(lt)); P.rect(x + 3, 0, 1, 16, hx(dk)); } };
  reg("ugLaneX", 16, 16, laneX("#B7A274", "#CBB98A", "#9C8960"));
  reg("ugLaneBlueX", 16, 16, laneX("#7FA3C6", "#9DBCDA", "#62849F"));
  reg("ugLaneRedX", 16, 16, laneX("#C7826E", "#DB9C88", "#A6685A"));
  reg("ugCorWall", 16, 16, (P, r) => { P.fill(hx("#B9BBB6"), 0.04, r); P.rect(0, 0, 16, 1, hx("#9EA09B")); P.rect(0, 8, 16, 1, hx("#9EA09B")); P.rect(0, 0, 1, 8, hx("#9EA09B")); P.rect(8, 8, 1, 8, hx("#9EA09B")); });
  reg("ugLamp", 16, 16, (P, r) => { P.fill(hx("#FFF4D2"), 0.02, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); }, { glow: 0.95 });
  reg("ugTread", 16, 16, (P, r) => { P.fill(hx("#8E98A4"), 0.03, r); for (let y = 2; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#6E7782")); P.rect(0, 0, 16, 1, hx("#C4CBD3")); });

  // A flight of stairs up along x (8 steps of half a block over 4 blocks), climbing east or west, its banister along
  // one side (north or south; the other side against a wall), taking you up to o.to. (The mod's stairs run along x.)
  function ugFlight(R, x0, z0, depth, climb, railSide, o) {
    const G = R.geo, n = 8, sw = 0.5;
    for (let i = 0; i < n; i++) {
      const xa = climb === "e" ? x0 + i * sw : x0 + 4 - (i + 1) * sw, xb = xa + sw, h = (i + 1) * 0.5;
      G.box(xa, 0, z0, xb, h, z0 + depth, { top: "ugTread", all: "ugTrim", bottom: null });
    }
    const rz = railSide === "n" ? z0 : z0 + depth;
    for (let i = 0; i < n; i++) {
      const xa = climb === "e" ? x0 + i * sw : x0 + 4 - (i + 1) * sw, y = (i + 1) * 0.5;
      G.box(xa + 0.2, y, rz - 0.04, xa + 0.28, y + 0.95, rz + 0.04, "steel");
      G.box(xa, y + 0.9, rz - 0.06, xa + sw, y + 1.02, rz + 0.06, "steel");
    }
    R.rail(x0 + 0.1, rz - 0.15, x0 + 4, rz + 0.15, R.H + 1);
    R.ramps.push({ rect: [x0, z0, x0 + 4, z0 + depth], h: (x) => {
      const k = climb === "e" ? x - x0 : x0 + 4 - x;
      return Math.max(0, Math.min(4, Math.ceil(k / sw - 0.35) * 0.5));
    } });
    R.warps.push({ test: p => p.y >= 1.45 && p.x > x0 - 0.2 && p.x < x0 + 4.2 && p.z > z0 - 0.5 && p.z < z0 + depth + 0.5, to: o.to, spawn: o.spawn, fx: "fade", why: "up" });
    (R._stairs || (R._stairs = [])).push({ dir: "up", x0, z0, depth, to: o.to, spawn: o.spawn });
    R.item("Stairs up", "Steel treads up to the entrance, a steel rail along the open side.");
  }

  // A low counter in a bracket, as FireRed's (a back and two arms), open toward the middle of the room.
  function ugBracket(R, x0, z0, x1, z1, open) {
    const G = R.geo, t = 0.7, h = 0.95;
    const back = open === "e" ? [x0, z0, x0 + t, z1] : [x1 - t, z0, x1, z1];
    const pieces = [back, [x0, z0, x1, z0 + t], [x0, z1 - t, x1, z1]];
    for (const [a, b, c, d] of pieces) { G.box(a, 0, b, c, h, d, { all: "ugCounter", top: "ugTop", bottom: null }); R.solid(a, b, c, d); }
    R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A low counter, its top cool grey stone."] });
  }

  // An Underground Path entrance (all four the same): the mat at the south, a bracket of counter either side, the
  // stairs down in the middle, a plant in every corner, the sign over the stairs, someone waiting.
  const UG_UP = { x: 9.6, z: 3.8, yaw: FACE.s };
  function ugEntrance(R, o) {
    shell(R, { paper: "ugWall", floor: "ugFloor", door: [6, 8], entry: true, skirt: "ugTrim", ledge: "ugPanel", ledgeTop: "ugTop" });
    band(R, 0.24, "ugPanel", 1.0); band(R, 1.24, "ugTrim", 0.08);
    ugBracket(R, 1.2, 2.4, 3.6, 6.8, "e");
    ugBracket(R, 10.4, 2.4, 12.8, 6.8, "w");
    stairsDown(R, 5.0, 2.2, 2.2, { to: o.down, spawn: o.downSpawn, paper: "ugCorWall" });
    framed(R, 5.0, 2.3, 4.0, 1.0, "ugSign", "n", { act: { label: "Read", lines: ["UNDERGROUND PATH", o.sign] } });
    for (const [x, z] of [[0.8, 0.8], [13.2, 0.8], [0.8, 9.2], [13.2, 9.2]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    const G = R.geo;
    for (const x of [3.0, 11.0]) G.box(x - 0.6, R.H - 0.06, 4.4, x + 0.6, R.H, 5.0, { bottom: "ugLamp", all: null });
    doormat(R, 6, 8, { to: "town", mat: "matGate" });
    R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
    if (o.person) R.npcs.push(o.person);
    R.item("Underground Path entrance", "FireRed's: gold tiles, cream walls on slate-blue panels, a bracket of counter either side, the stairs down between them.");
  }

  // A tunnel: its floor in FireRed's bands, lamps along both walls, the stairs up at each end.
  function ugTunnel(R, along) {
    const { W, D } = R;
    const ns = along === "z";
    const L = ns ? D : W, k = L / 4;
    const bands = ns ? [[0, k, "ugLane"], [k, 2 * k, "ugLaneBlue"], [2 * k, 3 * k, "ugLaneRed"], [3 * k, L, "ugLane"]]
      : [[0, k, "ugLaneX"], [k, 2 * k, "ugLaneBlueX"], [2 * k, 3 * k, "ugLaneRedX"], [3 * k, L, "ugLaneX"]];
    const zones = bands.map(([a, b, m]) => ns ? [0, a, W, b, m] : [a, 0, b, D, m]);
    shell(R, { paper: "ugCorWall", floor: "ugLane", zones, skirt: "ugTrim", rail: false });
    band(R, 0.24, "ugTrim", 0.5);
    const G = R.geo;
    if (ns) for (let z = 4; z < D - 2; z += 8) { G.box(0, 2.6, z, 0.08, 3.0, z + 1.2, { e: "ugLamp", all: "ugTrim" }); G.box(W - 0.08, 2.6, z + 4, W, 3.0, z + 5.2, { w: "ugLamp", all: "ugTrim" }); }
    else for (let x = 4; x < W - 2; x += 8) G.box(x, 2.6, 0, x + 1.2, 3.0, 0.08, { s: "ugLamp", all: "ugTrim" });
    R.item("Tunnel", "FireRed's long floor in bands (tan, blue, red, tan), grey tiled walls, a lamp every few steps.");
  }

  const place = (id, name, floor, group) => PLACES.push({ id, name, floor, group });
  place("ug_r5", "Underground Path", "Route 5 entrance", "Underground Path");
  place("ug_ns", "Underground Path", "North to south", "Underground Path");
  place("ug_r6", "Underground Path", "Route 6 entrance", "Underground Path");
  place("ug_r7", "Underground Path", "Route 7 entrance", "Underground Path");
  place("ug_ew", "Underground Path", "West to east", "Underground Path");
  place("ug_r8", "Underground Path", "Route 8 entrance", "Underground Path");

  const NS_D = 58, EW_W = 62;
  Object.assign(BUILD, {
    ug_r5() {
      // Route 5's (north of SAFFRON): FireRed's TRADER waits by the stairs, after a NIDORAN♂.
      const R = new Room("ug_r5", { name: "Underground Path", floor: "Route 5 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ns", downSpawn: { x: 0.8, z: 3.0, yaw: FACE.s }, sign: "This way to ROUTE 6 (VERMILION CITY).",
        person: { name: "Trader", key: "underground_path_trader", x: 7.0, z: 6.2, yaw: FACE.s, look: { skin: "middle_aged_man" },
          talk: () => [{ text: "I'm looking for NIDORAN♂! Wanna trade one for NIDORAN♀?" }, { text: "(In the game: trade him a NIDORAN♂ and you get MS. NIDO.)" }] } });
      return R;
    },
    ug_r6() {
      const R = new Room("ug_r6", { name: "Underground Path", floor: "Route 6 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ns", downSpawn: { x: 0.8, z: NS_D - 3.0, yaw: FACE.n }, sign: "This way to ROUTE 5 (CERULEAN CITY).",
        person: { name: "Girl", key: "underground_path_route_6_p0", x: 4.6, z: 8.0, yaw: FACE.e, look: { skin: "girl" },
          talk: () => [{ text: "People often lose things in that UNDERGROUND PATH." }] } });
      return R;
    },
    ug_r7() {
      const R = new Room("ug_r7", { name: "Underground Path", floor: "Route 7 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ew", downSpawn: { x: 6.0, z: 1.0, yaw: FACE.e }, sign: "This way to ROUTE 8 (LAVENDER TOWN).",
        person: { name: "Man", key: "underground_path_route_7_p0", x: 9.4, z: 8.0, yaw: FACE.w, look: { skin: "middle_aged_man" },
          talk: () => [{ text: "I heard a sleepy POKéMON appeared near CELADON CITY." }] } });
      return R;
    },
    ug_r8() {
      const R = new Room("ug_r8", { name: "Underground Path", floor: "Route 8 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ew", downSpawn: { x: EW_W - 6.0, z: 1.0, yaw: FACE.w }, sign: "This way to ROUTE 7 (CELADON CITY).",
        person: { name: "Girl", key: "underground_path_route_8_p0", x: 4.6, z: 8.0, yaw: FACE.e, look: { skin: "girl" },
          talk: () => [{ text: "The dept. store in CELADON has a great selection!" }] } });
      return R;
    },
    ug_ns() {
      // ROUTE 5 to ROUTE 6, under SAFFRON CITY: stairs up at the north (to Route 5's entrance) and the south (Route 6's).
      const R = new Room("ug_ns", { name: "Underground Path", floor: "North to south", W: 7, D: NS_D, H: 4 });
      ugTunnel(R, "z");
      ugFlight(R, 1.6, 0, 2, "e", "s", { to: "ug_r5", spawn: UG_UP });
      ugFlight(R, 1.6, NS_D - 2, 2, "e", "n", { to: "ug_r6", spawn: UG_UP });
      R.spawn = { x: 0.8, z: 3.0, yaw: FACE.s };
      return R;
    },
    ug_ew() {
      // ROUTE 7 to ROUTE 8: stairs up at the west (to Route 7's entrance) and the east (Route 8's), along the north wall.
      const R = new Room("ug_ew", { name: "Underground Path", floor: "West to east", W: EW_W, D: 7, H: 4 });
      ugTunnel(R, "x");
      ugFlight(R, 1, 0, 2, "w", "s", { to: "ug_r7", spawn: UG_UP });
      ugFlight(R, EW_W - 5, 0, 2, "e", "s", { to: "ug_r8", spawn: UG_UP });
      R.spawn = { x: 6.0, z: 1.0, yaw: FACE.e };
      return R;
    },
  });
  window.UG = { NS_D, EW_W };
})();

// ---------- Cerulean City (16:17): the Gym, the Bike Shop, its four houses; Route 5's Day Care; Route 25's Sea Cottage ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // The Gym: pale blue tiles, the pool's water, white edges, the stands' seats.
  reg("ceruleanTile", 16, 16, (P, r) => { P.fill(hx("#D3E8F6"), 0.025, r); P.rect(0, 0, 16, 1, hx("#B6D2E8")); P.rect(0, 0, 1, 16, hx("#B6D2E8")); P.rect(8, 8, 1, 1, hx("#EAF5FC")); });
  reg("ceruleanWall", 16, 16, (P, r) => { P.fill(hx("#EEF6FB"), 0.02, r); for (let y = 0; y < 16; y += 8) P.rect(0, y, 16, 1, hx("#D6E6F1")); });
  reg("ceruleanTrim", 16, 16, (P, r) => { P.fill(hx("#3F86C8"), 0.03, r); P.rect(0, 0, 16, 2, hx("#78B0E2")); P.rect(0, 14, 16, 2, hx("#2C5F94")); });
  reg("poolWater", 32, 32, (P, r) => {
    P.fill(hx("#3E92D8"), 0.03, r);
    for (let i = 0; i < 14; i++) { const x = Math.floor(r() * 28), y = Math.floor(r() * 30); P.rect(x, y, 3 + Math.floor(r() * 3), 1, hx("#7DBDF0")); }
    for (let i = 0; i < 8; i++) { const x = Math.floor(r() * 30), y = Math.floor(r() * 30); P.rect(x, y, 2, 1, hx("#2C74B8")); }
  }, { per: 2 });
  reg("poolEdge", 16, 16, (P, r) => { P.fill(hx("#F5F8FB"), 0.02, r); P.rect(0, 15, 16, 1, hx("#C9D6E2")); });
  reg("standSeat", 16, 16, (P, r) => { P.fill(hx("#E35B66"), 0.03, r); P.rect(0, 0, 16, 2, hx("#F08A93")); });
  reg("standSeatBlue", 16, 16, (P, r) => { P.fill(hx("#4C8FDB"), 0.03, r); P.rect(0, 0, 16, 2, hx("#82B5EE")); });
  reg("gymStage", 16, 16, (P, r) => { P.fill(hx("#F4F0E2"), 0.02, r); P.rect(0, 0, 16, 1, hx("#3F86C8")); P.rect(0, 15, 16, 1, hx("#3F86C8")); P.rect(0, 0, 1, 16, hx("#3F86C8")); P.rect(15, 0, 1, 16, hx("#3F86C8")); });
  // The Bike Shop: white walls with a red rail, a light checker floor, the bikes' red frames and black tyres.
  reg("bikeFloor", 32, 32, (P, r) => mcCheck(P, r, hx("#F2F2EF"), hx("#D9DCDF")), { per: 2 });
  reg("bikePaper", 16, 16, (P, r) => { P.fill(hx("#FBF8EE"), 0.02, r); });
  reg("bikeRed", 16, 16, (P, r) => { P.fill(hx("#D8343E"), 0.03, r); P.rect(0, 0, 16, 2, hx("#F0626A")); P.rect(0, 14, 16, 2, hx("#A8232C")); });
  reg("tyre", 16, 16, (P, r) => { P.fill([0, 0, 0], 0, r); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.hypot(x - 7.5, y - 7.5); P.set(x, y, d > 7.6 ? [0, 0, 0] : d > 5.6 ? hx("#26282C") : d > 4.8 ? hx("#B8BEC6") : d > 1.2 ? hx("#E8ECF0") : hx("#8D949C"), d > 7.6 ? 0 : 1); } }, { fit: true, cut: true });
  reg("bikePoster", 64, 32, (P, r) => {
    P.fill(hx("#FFF6E0"), 0.02, r); P.rect(0, 0, 64, 3, hx("#D8343E")); P.rect(0, 29, 64, 3, hx("#D8343E"));
    P.text("BIKE SHOP", 15, 7, hx("#A8232C")); P.text("BICYCLE", 18, 15, hx("#3A3A3A")); P.text("1000000 YEN", 9, 22, hx("#3A3A3A"));
  }, { fit: true });
  reg("cyclingPoster", 64, 32, (P, r) => {
    P.fill(hx("#E9F4FF"), 0.02, r); P.rect(0, 0, 64, 3, hx("#3F86C8")); P.rect(0, 29, 64, 3, hx("#3F86C8"));
    P.text("CYCLING ROAD", 9, 9, hx("#2C5F94")); P.text("ROUTE 17", 17, 18, hx("#3A3A3A"));
  }, { fit: true });
  // The houses: Cerulean's soft blue paper, a badge case on the badge man's wall.
  reg("paperCerulean", 16, 16, (P, r) => { P.fill(hx("#EAF1F6"), 0.02, r); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#DCE7F0")); });
  reg("badgeCase", 64, 16, (P, r) => {
    P.fill(hx("#5B3A22"), 0.03, r); P.rect(1, 1, 62, 14, hx("#2A2F3A"));
    const cols = ["#9AA0A8", "#4AA3E0", "#F2C84B", "#7CC56B", "#C77DD8", "#E2665A", "#F2A23A", "#3ED0C8"];
    for (let i = 0; i < 8; i++) { const x = 4 + i * 7.5; P.rect(Math.round(x), 5, 5, 5, hx(cols[i])); P.set(Math.round(x) + 1, 6, hx("#FFFFFF")); }
  }, { fit: true });
  // The Day Care's play mat; the Sea Cottage's machine.
  reg("playMat", 32, 32, (P, r) => { const c = ["#F2C84B", "#7CC56B", "#4AA3E0", "#E2665A"]; for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) P.rect(tx * 8, ty * 8, 8, 8, hx(c[(tx + ty) % 4])); }, { fit: true });
  reg("cellMetal", 16, 16, (P, r) => { P.fill(hx("#A9B3BE"), 0.03, r); P.rect(0, 0, 16, 1, hx("#DCE3EA")); P.rect(0, 7, 16, 1, hx("#8C96A1")); for (const x of [2, 13]) P.rect(x, 2, 1, 1, hx("#6B7580")); });
  reg("cellPanel", 32, 16, (P, r) => { P.fill(hx("#2E3A50"), 0.02, r); for (let i = 0; i < 6; i++) P.rect(3 + i * 5, 4, 3, 2, hx(i % 2 ? "#7CF5B0" : "#F5D76E")); P.rect(3, 10, 26, 2, hx("#4F6A8E")); }, { fit: true, glow: 0.5 });
  reg("cottageBoards", 16, 16, (P, r) => planks(P, r, hx("#A9774A"), hx("#C08C5C"), hx("#83592F"), hx("#6B4726")));

  // Cut rect b out of each of rects (what's left of the water once the walkways are laid).
  function cut(rects, b) {
    const out = [];
    for (const a of rects) {
      if (b[0] >= a[2] || b[2] <= a[0] || b[1] >= a[3] || b[3] <= a[1]) { out.push(a); continue; }
      if (b[0] > a[0]) out.push([a[0], a[1], b[0], a[3]]);
      if (b[2] < a[2]) out.push([b[2], a[1], a[2], a[3]]);
      const x0 = Math.max(a[0], b[0]), x1 = Math.min(a[2], b[2]);
      if (b[1] > a[1]) out.push([x0, a[1], x1, b[1]]);
      if (b[3] < a[3]) out.push([x0, b[3], x1, a[3]]);
    }
    return out;
  }
  // A display bike: two wheels, a frame in its colour, a saddle and handlebars, side on along x.
  function bike(R, x, z, frame) {
    const G = R.geo;
    for (const dx of [-0.42, 0.42]) G.box(x + dx - 0.34, 0, z - 0.03, x + dx + 0.34, 0.68, z + 0.03, { n: "tyre", s: "tyre", all: null });
    G.box(x - 0.42, 0.32, z - 0.04, x + 0.42, 0.38, z + 0.04, frame);
    G.box(x - 0.1, 0.32, z - 0.04, x - 0.04, 0.78, z + 0.04, frame);
    G.box(x + 0.36, 0.32, z - 0.04, x + 0.42, 0.9, z + 0.04, frame);
    G.box(x - 0.42, 0.56, z - 0.04, x + 0.4, 0.62, z + 0.04, frame);
    G.box(x - 0.22, 0.78, z - 0.08, x + 0.06, 0.84, z + 0.08, "leather");
    G.box(x + 0.36, 0.9, z - 0.22, x + 0.42, 0.95, z + 0.22, "steel");
    R.solid(x - 0.8, z - 0.25, x + 0.8, z + 0.25);
  }
  const CERULEAN_WINDOW = ["Outside: CERULEAN CITY's blue paving, and the river beyond."];

  const place = (id, name, floor, group) => PLACES.push({ id, name, floor, group });
  place("cerulean_gym", "Cerulean Gym", "Cerulean City", "Cerulean City");
  place("bike_shop", "Bike Shop", "Cerulean City", "Cerulean City");
  place("cerulean_house1", "Cerulean house", "The badge house", "Cerulean City");
  place("cerulean_house2", "Cerulean house", "The robbed house", "Cerulean City");
  place("cerulean_house3", "Cerulean house", "The trade house", "Cerulean City");
  place("cerulean_house4", "Cerulean house", "By the Mart", "Cerulean City");
  place("daycare", "Pokémon Day Care", "Route 5", "Route 5");
  place("bills_house", "Sea Cottage", "Route 25", "Route 25");

  Object.assign(BUILD, {
    cerulean_gym() {
      // CERULEAN GYM, laid out as FireRed's (14 by 18 of its steps, a step a block): stands at the back, the pool
      // filling the hall, a white-edged walkway winding through it from the door to MISTY at the top. You can't swim
      // (not yet), so the water's where you can't go. PICNICKER DIANA on the left arm, SWIMMER LUIS in the water.
      const W = 14, D = 18;
      const R = new Room("cerulean_gym", { name: "Cerulean Gym", floor: "Cerulean City", W, D, H: 5 });
      const walks = [[5.9, 4.6, 11.6, 6.4], [2.8, 5.6, 5.9, 6.6], [9.8, 4.6, 11.6, 10.4], [2.8, 9.0, 11.6, 10.4], [2.8, 9.0, 4.0, 12.8], [2.8, 11.8, 8.6, 12.8], [6.4, 9.0, 8.6, 14.6]];
      const stands = [0, 0, W, 2.2], deck = [0, 14.6, W, D - 2], float = [8.7, 11.7, 9.7, 12.7];
      shell(R, { paper: "ceruleanWall", floor: "ceruleanTile", door: [6, 8], entry: true, skirt: "ceruleanTrim", ledge: "ceruleanTrim", ledgeTop: "poolEdge",
        floorRects: [stands, deck, [5, D - 2, 9, D], float, ...walks] });
      band(R, 0.3, "ceruleanTrim", 0.9); band(R, 1.2, "poolEdge", 0.1);
      const G = R.geo;
      // The pool: its water a little below the walkways, a white lip round every walkway and the deck.
      let water = [[0, 2.2, W, 14.6]];
      for (const w of [...walks, float]) water = cut(water, w);
      G.box(0, -1.2, 2.2, W, -0.12, 14.6, { top: "poolWater", all: null });
      for (const [x0, z0, x1, z1] of water) R.solid(x0, z0, x1, z1);
      for (const [x0, z0, x1, z1] of walks) G.box(x0, -0.12, z0, x1, 0.02, z1, { all: "poolEdge", top: null, bottom: null });
      G.box(float[0], -0.12, float[1], float[2], 0.08, float[3], { all: "standSeat", top: "poolEdge", bottom: null });
      G.box(0, -0.12, 14.6, W, 0.02, 14.7, { n: "poolEdge", all: null }); G.box(0, -0.12, 2.1, W, 0.02, 2.2, { s: "poolEdge", all: null });
      R.item("Pool", "The pool fills the hall; a white-edged walkway winds through it. No swimming (no SURF yet): the water stops you.");
      // The stands at the back: two tiers, red and blue seats.
      G.box(0.6, 0, 0, W - 0.6, 0.5, 2.1, { all: "ceruleanTrim", top: "poolEdge" }); G.box(0.6, 0.5, 0, W - 0.6, 1.0, 1.0, { all: "ceruleanTrim", top: "poolEdge" });
      for (let x = 1.4, i = 0; x < W - 1; x += 1.6, i++) { G.box(x, 0.5, 1.3, x + 0.8, 0.75, 1.9, i % 2 ? "standSeatBlue" : "standSeat"); G.box(x, 1.0, 0.2, x + 0.8, 1.25, 0.8, i % 2 ? "standSeat" : "standSeatBlue"); }
      R.solid(0, 0, W, 2.1);
      // MISTY's spot at the top: a stage tile on the walkway.
      G.box(6.2, 0, 4.8, 7.8, 0.06, 6.2, { top: "gymStage", all: "poolEdge", bottom: null });
      // Pool ladders at the corners.
      for (const [x, z] of [[1.2, 14.4], [12.8, 14.4], [1.2, 2.4], [12.8, 2.4]]) { G.box(x - 0.3, -0.12, z - 0.04, x - 0.24, 0.9, z + 0.04, "steel"); G.box(x + 0.24, -0.12, z - 0.04, x + 0.3, 0.9, z + 0.04, "steel"); G.box(x - 0.3, 0.84, z - 0.04, x + 0.3, 0.9, z + 0.04, "steel"); }
      gymStatue(R, 4.4, 15.6, ["CERULEAN CITY POKéMON GYM", "LEADER: MISTY", "WINNING TRAINERS: {rival}"]);
      gymStatue(R, 9.6, 15.6, ["CERULEAN CITY POKéMON GYM", "LEADER: MISTY", "WINNING TRAINERS: {rival}"]);
      for (const [x, z] of [[0.8, 15.2], [13.2, 15.2]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Leader Misty", key: "cerulean_gym_0", role: "leader", x: 7.0, z: 5.5, y: 0.06, yaw: FACE.s, look: { skin: "leader_misty" },
        talk: () => [{ text: "MISTY: Hi, you're a new face! My policy is an all-out offensive with WATER-type POKéMON!" }, { text: "(In the game: you battle her and win the CASCADEBADGE.)" }] });
      R.npcs.push({ name: "Picnicker Diana", key: "cerulean_gym_1", role: "trainer", x: 3.4, z: 6.1, yaw: FACE.e, look: { skin: "cooltrainer_f" },
        talk: () => [{ text: "I'm more than good enough for you! MISTY can wait!" }] });
      R.npcs.push({ name: "Swimmer Luis", key: "cerulean_gym_2", role: "trainer", x: 9.2, z: 12.2, y: 0.08, yaw: FACE.w, look: { skin: "swimmer" },
        talk: () => [{ text: "Splash! I'm first up! Let's do it!" }] });
      R.npcs.push({ name: "Gym guide", key: "cerulean_gym_p0", x: 10.8, z: 15.4, yaw: FACE.w, look: { skin: "gym_guide" },
        talk: () => [{ text: "Yo! Champ in making!" }, { text: "MISTY is a trainer who's going to keep improving." }, { text: "She won't lose to someone like you!" }] });
      doormat(R, 6, 8, { to: "town", mat: "matGate" });
      R.spawn = { x: 7.0, z: D - 1.5, yaw: FACE.n };
      return R;
    },

    bike_shop() {
      // The BIKE SHOP (FireRed's, by the south gate): the clerk at his counter, six bikes on show, the price on the wall.
      const R = new Room("bike_shop", { name: "Bike Shop", floor: "Cerulean City", W: 14, D: 11 });
      shell(R, { paper: "bikePaper", floor: "bikeFloor", door: [6, 8], entry: true, skirt: "bikeRed", ledge: "bikeRed", ledgeTop: "steel" });
      band(R, 0.24, "bikeRed", 0.3); band(R, 1.3, "bikeRed", 0.08);
      const G = R.geo;
      G.box(0.6, 0, 2.6, 4.2, 1.0, 3.5, { all: "bikeRed", top: "ugTop", bottom: null }); R.solid(0.6, 2.6, 4.2, 3.5);
      till(R, 3.2, 3.05, 1.0, "s");
      for (const [i, x] of [6.2, 8.4, 10.6, 12.6].entries()) bike(R, x, 3.4, ["bikeRed", "ceruleanTrim", "standSeat", "steel"][i]);
      for (const [i, x] of [6.2, 8.4, 10.6].entries()) bike(R, x, 6.2, ["ceruleanTrim", "bikeRed", "steel"][i]);
      framed(R, 5.6, 1.9, 3.2, 1.5, "bikePoster", "n", { act: { label: "Read", lines: ["BIKE SHOP: the BICYCLE, ¥1,000,000.", "A BIKE VOUCHER buys one too."] } });
      framed(R, 9.8, 1.9, 3.2, 1.5, "cyclingPoster", "n", { act: { label: "Read", lines: ["CYCLING ROAD: ROUTE 17, from CELADON CITY down to FUCHSIA CITY."] } });
      plant(R, 0.8, 8.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); plant(R, 13.2, 8.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Clerk", key: "bike_shop_p0", x: 2.2, z: 1.8, yaw: FACE.s, look: { skin: "bike_shop_clerk" },
        talk: () => [{ text: "Hi! Welcome to our BIKE SHOP. Have we got just the BIKE for you! It's a cool BIKE! Do you want it?" }, { text: "(In the game: it's ¥1,000,000.)" }] });
      R.npcs.push({ name: "Woman", key: "bike_shop_p1", x: 9.4, z: 8.0, yaw: FACE.n, look: { skin: "middle_aged_woman" }, talk: () => [{ text: "A plain city BIKE is good enough for me!" }] });
      R.npcs.push({ name: "Youngster", key: "bike_shop_p2", x: 11.6, z: 4.8, yaw: FACE.n, look: { skin: "youngster" }, talk: () => [{ text: "These BIKEs are cool, but they're way expensive!" }] });
      doormat(R, 6, 8, { to: "town", mat: "matGate" });
      R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },

    cerulean_house1() {
      // The badge house (FireRed's top-left house): the man who knows what every badge does, his badge case on the wall.
      const R = new Room("cerulean_house1", { name: "Cerulean house", floor: "The badge house", W: 12, D: 11 });
      plainHouse(R, { paper: "paperCerulean", rug: "rugBlue", window: CERULEAN_WINDOW, cupboard: ["A cupboard. Inside: a polishing cloth and a little tin of badge polish."] });
      framed(R, 8.4, 1.8, 3.0, 0.75, "badgeCase", "n", { act: { label: "Look", lines: ["A badge case: eight places, each badge drawn in its colour."] } });
      R.npcs.push({ name: "Man", key: "cerulean_badge_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "middle_aged_man" },
        talk: () => [{ text: "POKéMON BADGEs are owned only by skilled trainers." }, { text: "I see you have at least one. Those BADGEs have amazing secrets!" }] });
      return R;
    },
    cerulean_house2() {
      // The robbed house (FireRed's top-right house): TEAM ROCKET broke in through the back wall and took a TM. The hole's
      // there still, daylight through it; the chairs knocked over, papers everywhere.
      const R = new Room("cerulean_house2", { name: "Cerulean house", floor: "The robbed house", W: 12, D: 11 });
      plainHouse(R, { paper: "paperCerulean", rug: "rugSalmon", window: CERULEAN_WINDOW, cupboard: ["A cupboard, its doors hanging open. Whatever was inside is gone."] });
      const G = R.geo;
      G.box(9.4, 0, -0.02, 11.2, 1.9, 0.02, { s: "deep", all: null });
      G.box(9.6, 0, 0.0, 11.0, 1.7, 0.03, { s: "windowExt", all: null });
      for (const [x, z, w] of [[2.4, 7.6, 0.5], [5.2, 8.2, 0.4], [9.2, 6.8, 0.6], [7.6, 2.0, 0.45]]) G.box(x, 0, z, x + w, 0.02, z + w * 0.7, { top: "paperSchool", all: null });
      R.act([9.2, 0, 11.4, 1.2], { label: "Look", lines: ["TEAM ROCKET left through here. There's a hole in the wall!"] });
      R.npcs.push({ name: "Fishing guru", key: "cerulean_trashed_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "fishing_guru" },
        talk: () => [{ text: "Those miserable ROCKETs! Look what they did here!" }, { text: "They stole a TM for teaching POKéMON how to DIG holes! That cost me a bundle, it did!" }] });
      R.npcs.push({ name: "Girl", key: "cerulean_trashed_house_p1", x: 2.6, z: 7.6, yaw: FACE.e, look: { skin: "girl" }, talk: () => [{ text: "TEAM ROCKET must be trying to DIG their way into no good!" }] });
      return R;
    },
    cerulean_house3() {
      // The trade house (FireRed's): a POLIWHIRL wanted, a JYNX in return (ZYNX).
      const R = new Room("cerulean_house3", { name: "Cerulean house", floor: "The trade house", W: 12, D: 11 });
      plainHouse(R, { paper: "paperCerulean", rug: "rugGreen", window: CERULEAN_WINDOW, cupboard: ["A cupboard. Inside: a box of trade forms."] });
      R.npcs.push({ name: "Trader", key: "cerulean_trader", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "old_man" },
        talk: () => [{ text: "I'm looking for POLIWHIRL! Wanna trade one for JYNX?" }, { text: "(In the game: trade her a POLIWHIRL and you get ZYNX.)" }] });
      return R;
    },
    cerulean_house4() {
      // The house by the Mart (FireRed's WONDER NEWS man): he's mad about berries, and gives you one.
      const R = new Room("cerulean_house4", { name: "Cerulean house", floor: "By the Mart", W: 12, D: 11 });
      plainHouse(R, { paper: "paperCerulean", rug: "rugBlue", window: CERULEAN_WINDOW, cupboard: ["A cupboard. Inside: jars of berries, each one labelled."] });
      fruitBowl(R, 6.0, 4.4, 0.86);
      R.npcs.push({ name: "Old man", key: "cerulean_berry_man", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "old_man" },
        talk: () => [{ text: "I love hearing news! And berries!" }, { text: "Here, have a BERRY. (In the game: he gives you one.)" }] });
      return R;
    },

    daycare() {
      // The POKéMON DAY CARE on ROUTE 5: the old man at his counter, a play mat and toys, the yard through the window.
      const R = new Room("daycare", { name: "Pokémon Day Care", floor: "Route 5", W: 14, D: 11 });
      shell(R, { paper: "paperSchool", floor: "midBoards", door: [6, 8], entry: true, skirt: "beam", ledge: "walnut" });
      band(R, 1.0, "beam", 0.12);
      const G = R.geo;
      G.box(0.6, 0, 2.6, 4.6, 1.0, 3.5, { all: "counterOrange", top: "lab", bottom: null }); R.solid(0.6, 2.6, 4.6, 3.5);
      G.box(6.0, 0, 3.0, 12.0, 0.03, 7.0, { top: "playMat", all: "cap", bottom: null });
      toyBox(R, 11.8, 0.6, 13.4, 1.6); R.solid(11.8, 0.6, 13.4, 1.6);
      for (const [x, z, c] of [[7.2, 4.0, "standSeat"], [10.6, 5.6, "standSeatBlue"], [8.8, 6.2, "ceruleanTrim"]]) roundBall(G, x, 0.03, z, 0.36, { all: c, top: c });
      sofa(R, 1.0, 6.2, "e", 2.4);
      crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, ["Outside: the DAY CARE's yard. POKéMON are playing in the grass."]);
      plant(R, 0.8, 8.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Day Care man", key: "daycare_p0", x: 2.4, z: 1.8, yaw: FACE.s, look: { skin: "gentleman" },
        talk: () => [{ text: "I run a DAY CARE. Would you like me to raise one of your POKéMON?" }, { text: "(In the game: not yet.)" }] });
      doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },

    bills_house() {
      // BILL's SEA COTTAGE on ROUTE 25: warm wood, his PC, and the CELL SEPARATOR (two glass pods joined by pipes).
      const R = new Room("bills_house", { name: "Sea Cottage", floor: "Route 25", W: 14, D: 11 });
      shell(R, { paper: "cottageBoards", floor: "midBoards", door: [6, 8], entry: true, skirt: "beam", ledge: "walnut" });
      band(R, 1.0, "beam", 0.12);
      const G = R.geo;
      deskPC(R, 0.6, 0, "s", R.screen("billPC", 40, 24, drawPC, 0.9), {});
      R.act([0.4, 0, 3.0, 1.4], { label: "Use", pc: true, lines: ["BILL's PC: his rare POKéMON, filed away."] });
      bookshelf(R, 3.4, 0, "s", 2, 3, { lines: ["BILL's books: POKéMON storage, teleporters, transmission theory."] });
      for (const x of [8.4, 11.6]) {
        G.box(x - 0.9, 0, 0.4, x + 0.9, 0.5, 2.2, { all: "cellMetal", top: "cellMetal" });
        G.box(x - 0.7, 0.5, 0.6, x + 0.7, 2.6, 2.0, { all: "tankGlass", bottom: null, top: null });
        G.box(x - 0.9, 2.6, 0.4, x + 0.9, 3.0, 2.2, "cellMetal");
        R.solid(x - 0.9, 0.4, x + 0.9, 2.2);
      }
      G.box(9.3, 2.2, 1.1, 10.7, 2.4, 1.3, "steel"); G.box(9.3, 0.3, 1.1, 10.7, 0.45, 1.3, "steel");
      G.box(9.4, 0, 2.4, 10.6, 1.0, 3.0, { all: "cellMetal", front: "cellPanel" }); R.solid(9.4, 2.4, 10.6, 3.0);
      R.act([7.2, 0, 12.8, 3.4], { label: "Look", lines: ["The CELL SEPARATOR: two glass pods joined by pipes, a panel of lights between them."] });
      R.item("Cell Separator", "BILL's machine: two glass pods on steel bases, pipes between, its panel blinking.");
      sofa(R, 1.0, 6.0, "e", 2.4);
      crossWindow(R, "e", 5.0, 1.6, 1.8, 1.2, ["Outside: the sea, right up to ROUTE 25's edge."]);
      plant(R, 13.2, 8.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Bill", key: "bills_house_p0", x: 6.4, z: 3.4, yaw: FACE.s, look: { skin: "super_nerd" },
        talk: () => [{ text: "BILL: Look, bud, just check out some of my rare POKéMON on my PC!" }] });
      doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },
  });
})();

// ---------- the Center's and the Mart's metal (16:17): brushed steel, chrome edges, anodised blue posts ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  reg("brushedSteel", 16, 16, (P, r) => {
    P.fill(hx("#B9C1CA"), 0.02, r);
    for (let y = 0; y < 16; y++) { const k = (r() - 0.5) * 0.12; for (let x = 0; x < 16; x++) if (r() < 0.55) P.set(x, y, shadec(hx("#B9C1CA"), k + (r() - 0.5) * 0.05)); }
    P.rect(0, 0, 16, 1, hx("#E4E9EE")); P.rect(0, 15, 16, 1, hx("#8E96A0"));
  });
  reg("chromeTrim", 16, 16, (P, r) => { P.fill(hx("#D7DDE3"), 0.01, r); P.rect(0, 0, 16, 4, hx("#F7F9FB")); P.rect(0, 10, 16, 2, hx("#A9B1BA")); P.rect(0, 14, 16, 2, hx("#7F8893")); });
  reg("postBlueMetal", 16, 16, (P, r) => {
    P.fill(hx("#3E7FCF"), 0.02, r);
    P.rect(2, 0, 2, 16, hx("#7DB0EC")); P.rect(4, 0, 1, 16, hx("#5D98E0")); P.rect(12, 0, 2, 16, hx("#2D62A8")); P.rect(15, 0, 1, 16, hx("#244F88"));
  });
})();
