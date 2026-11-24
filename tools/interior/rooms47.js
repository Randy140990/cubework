// ---------- 0.47.0 (16:17 and 16:20): the Underground Paths, Cerulean City's insides, the Day Care, Bill's Sea Cottage ----------
// (Loaded after rooms.js: its PLACES and BUILD get these rooms.)
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };

  // ----- the Underground Path's entrances, as Let's Go's (21:58): the tunnel's own pale grey tiles on the walls with
  // their orange check band, its brown tiled floor, a grey stone counter, slate at the foot -----
  const tiles = (base, grout, lt) => (P, r) => {
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const x0 = tx * 8, y0 = ty * 8, c = shadec(hx(base), (r() - 0.5) * 0.06);
      P.rect(x0, y0, 8, 8, c); P.noise(x0 + 1, y0 + 1, 7, 7, c, 0.03, r);
      P.rect(x0, y0, 8, 1, hx(grout)); P.rect(x0, y0, 1, 8, hx(grout)); P.rect(x0 + 1, y0 + 1, 7, 1, hx(lt));
    }
  };
  reg("ugFloor", 16, 16, tiles("#86694E", "#5E4834", "#9C7F62"));
  reg("ugWall", 16, 16, tiles("#D6D4CD", "#ABA9A2", "#E6E4DE"));
  reg("ugPanel", 16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, ((x >> 2) + (y >> 3)) % 2 ? hx("#E8956A") : hx("#E3DED4"));
    P.rect(0, 0, 16, 1, hx("#B9B4AA")); P.rect(0, 15, 16, 1, hx("#B9B4AA"));
  });
  reg("ugTrim", 16, 16, (P, r) => { P.fill(hx("#7E807C"), 0.03, r); P.rect(0, 0, 16, 2, hx("#9A9C97")); });
  reg("ugCounter", 16, 16, (P, r) => { P.fill(hx("#B9BBB7"), 0.025, r); P.rect(0, 0, 16, 2, hx("#D6D8D4")); P.rect(0, 13, 16, 3, hx("#7E807C")); });
  reg("ugTop", 16, 16, (P, r) => { P.fill(hx("#D7DADF"), 0.03, r); P.rect(0, 0, 16, 1, hx("#F0F2F5")); P.rect(0, 15, 16, 1, hx("#AEB3BA")); });
  reg("ugSign", 64, 16, (P, r) => {
    P.fill(hx("#2F4A6E"), 0.02, r); P.rect(0, 0, 64, 1, hx("#6E8DB4")); P.rect(0, 15, 64, 1, hx("#1E3048"));
    P.text("UNDERGROUND PATH", 1, 5, hx("#F6F1E2"));
  }, { fit: true });
  // ----- the tunnels, as Let's Go's (21:58): a brown tiled floor with a blue lane and a red lane running its length,
  // pale grey tiled walls with an orange check band, lamps on the walls lighting them -----
  reg("ugLane", 16, 16, tiles("#86694E", "#5E4834", "#9C7F62"));
  reg("ugLaneBlue", 16, 16, tiles("#4F86BE", "#3A6896", "#6A9DD0"));
  reg("ugLaneRed", 16, 16, tiles("#D9473D", "#AE3129", "#E8695E"));
  reg("ugLaneX", 16, 16, tiles("#86694E", "#5E4834", "#9C7F62"));
  reg("ugLaneBlueX", 16, 16, tiles("#4F86BE", "#3A6896", "#6A9DD0"));
  reg("ugLaneRedX", 16, 16, tiles("#D9473D", "#AE3129", "#E8695E"));
  reg("ugCorWall", 16, 16, tiles("#D6D4CD", "#ABA9A2", "#E6E4DE"));
  reg("ugLamp", 16, 16, (P, r) => { P.fill(hx("#FFF4D2"), 0.02, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); }, { glow: 0.95 });
  reg("ugLampCase", 16, 16, (P, r) => { P.fill(hx("#3A3E44"), 0.03, r); P.rect(0, 0, 16, 1, hx("#5A5F66")); });
  reg("ugTread", 16, 16, (P, r) => { P.fill(hx("#8E98A4"), 0.03, r); for (let y = 2; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#6E7782")); P.rect(0, 0, 16, 1, hx("#C4CBD3")); });

  // A flight of stairs up along x (8 steps of half a block over 4 blocks), climbing east or west, its banister along
  // one side (north or south; the other side against a wall), taking you up to o.to. (The mod's stairs run along x.)
  function ugFlight(R, x0, z0, depth, climb, railSide, o) {
    // (17:51) The flight runs right up to the end wall (its top step against it, so there's nothing to drop off), its
    // banister along the open side: steel posts at every other step, a steel handrail and a mid rail, tinted glass
    // between, the rail as high as you can't jump over.
    const G = R.geo, n = 8, sw = 0.5;
    for (let i = 0; i < n; i++) {
      const xa = climb === "e" ? x0 + i * sw : x0 + 4 - (i + 1) * sw, xb = xa + sw, h = (i + 1) * 0.5;
      G.box(xa, 0, z0, xb, h, z0 + depth, { top: "ugTread", all: "ugTrim", bottom: null });
      G.box(xa, h - 0.06, z0, xb, h, z0 + 0.08, "steel");
    }
    const rz = railSide === "n" ? z0 : z0 + depth, sgn = railSide === "n" ? 1 : -1;
    // (19:06) Its banisters run diagonally with the steps: steel and tinted glass on the open side, a steel handrail on
    // the wall the other side.
    const kOf = (x) => climb === "e" ? x - x0 : x0 + 4 - x;
    const yAt = (x) => Math.max(0, Math.min(4, kOf(x) / sw * 0.5)), stepAt = (x) => Math.max(0, Math.min(4, Math.ceil(kOf(x) / sw - 1e-3) * 0.5));
    slopeRail(R, x0, x0 + 4, yAt, stepAt, rz, { mat: "steel", glass: true });
    const wz = railSide === "n" ? z0 + depth - 0.08 : z0 + 0.08;
    slopeRail(R, x0 + 0.2, x0 + 3.8, yAt, stepAt, wz, { mat: "steel", wall: railSide === "n" ? "s" : "n" });
    R.rail(x0, rz - 0.15, x0 + 4, rz + 0.15, R.H + 1);
    R.ramps.push({ rect: [x0, z0, x0 + 4, z0 + depth], h: (x) => {
      const k = climb === "e" ? x - x0 : x0 + 4 - x;
      return Math.max(0, Math.min(4, Math.ceil(k / sw - 0.35) * 0.5));
    } });
    R.warps.push({ test: p => p.y >= 1.45 && p.x > x0 - 0.2 && p.x < x0 + 4.2 && p.z > z0 - 0.5 && p.z < z0 + depth + 0.5, to: o.to, spawn: o.spawn, fx: "fade", why: "up" });
    (R._stairs || (R._stairs = [])).push({ dir: "up", x0, z0, depth, to: o.to, spawn: o.spawn });
    R.item("Stairs up", "Steel-edged treads up to the end wall, a banister along the open side: steel posts, a handrail and a mid rail, tinted glass between.");
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
  const UG_UP = { x: 9.8, z: 3.3, yaw: FACE.e };
  function ugEntrance(R, o) {
    // (17:51) The floor left open over the stairwell (x 5 to 9, z 2.2 to 4.4), so the stairs show going down.
    shell(R, { paper: "ugWall", floor: "ugFloor", door: [6, 8], entry: true, skirt: "ugTrim", ledge: "ugPanel", ledgeTop: "ugTop",
      floorRects: [[0, 0, 14, 2.2], [0, 4.4, 14, 10], [0, 2.2, 5, 4.4], [9, 2.2, 14, 4.4], [5, 10, 9, 12]] });
    band(R, 0.24, "ugTrim", 0.2); band(R, 1.0, "ugPanel", 0.5);
    ugBracket(R, 1.2, 2.4, 3.6, 6.8, "e");
    ugBracket(R, 10.4, 2.4, 12.8, 6.8, "w");
    stairsDown(R, 5.0, 2.2, 2.2, { to: o.down, spawn: o.downSpawn, paper: "ugCorWall", show: true });
    sideRail(R, 5.0, 9.0, 2.2, "n");
    R.rail(5.0, 2.0, 9.0, 2.35, 3.2);
    framed(R, 5.0, 2.3, 4.0, 1.0, "ugSign", "n", { act: { label: "Read", lines: ["UNDERGROUND PATH", o.sign] } });
    for (const [x, z] of [[0.8, 0.8], [13.2, 0.8], [0.8, 9.2], [13.2, 9.2]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    const G = R.geo;
    for (const x of [3.0, 11.0]) G.box(x - 0.6, R.H - 0.06, 4.4, x + 0.6, R.H, 5.0, { bottom: "ugLamp", all: null });
    doormat(R, 6, 8, { to: "town", mat: "matGate" });
    R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
    if (o.person) R.npcs.push(o.person);
    R.item("Underground Path entrance", "As the tunnel below (Let's Go's): brown tiles, pale grey tiled walls with an orange check band, a grey stone counter either side, the stairs down between them.");
  }

  // A tunnel: its floor in FireRed's bands, lamps along both walls, the stairs up at each end.
  function ugTunnel(R, along) {
    const { W, D } = R;
    const ns = along === "z";
    const L = ns ? D : W, k = L / 4;
    const bands = ns ? [[0, k, "ugLane"], [k, 2 * k, "ugLaneBlue"], [2 * k, 3 * k, "ugLaneRed"], [3 * k, L, "ugLane"]]
      : [[0, k, "ugLaneX"], [k, 2 * k, "ugLaneBlueX"], [2 * k, 3 * k, "ugLaneRedX"], [3 * k, L, "ugLaneX"]];
    // (21:58) Let's Go's: the blue lane and the red lane run the tunnel's length, brown tiles either side.
    const across = ns ? W : D, lanes = [[0, 1.5, "ugLane"], [1.5, 2.5, "ugLaneBlue"], [2.5, across - 2.5, "ugLane"], [across - 2.5, across - 1.5, "ugLaneRed"], [across - 1.5, across, "ugLane"]];
    const zones = lanes.map(([a, b, m]) => ns ? [a, 0, b, D, m] : [0, a, W, b, m]);
    shell(R, { paper: "ugCorWall", floor: "ugLane", zones, skirt: "ugTrim", rail: false });
    band(R, 0.24, "ugTrim", 0.2); band(R, 1.0, "ugPanel", 0.5);
    const G = R.geo;
    // Lamps on the walls: a dark case, angled down, its light on the wall under it.
    const lamp = (x0, z0, x1, z1, face) => { G.box(x0, 2.45, z0, x1, 2.85, z1, { all: "ugLampCase", bottom: "ugLamp", [face]: "ugLamp" }); };
    if (ns) for (let z = 4; z < D - 2; z += 8) { lamp(0, z, 0.32, z + 1.0, "e"); lamp(W - 0.32, z + 4, W, z + 5.0, "w"); }
    else for (let x = 4; x < W - 2; x += 8) lamp(x, 0, x + 1.0, 0.32, "s");
    R.item("Tunnel", "Let's Go's: a brown tiled floor with a blue lane and a red lane down its length, pale grey tiled walls with an orange check band, lamps in dark cases on the walls.");
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
      ugEntrance(R, { down: "ug_ns", downSpawn: { x: 1.6, z: 2.8, yaw: FACE.s }, sign: "This way to ROUTE 6 (VERMILION CITY).",
        person: { name: "Trader", key: "underground_path_trader", x: 7.0, z: 6.2, yaw: FACE.s, look: { skin: "middle_aged_man" },
          talk: () => [{ text: "I'm looking for NIDORAN♂! Wanna trade one for NIDORAN♀?" }, { text: "(In the game: trade him a NIDORAN♂ and you get MS. NIDO.)" }] } });
      return R;
    },
    ug_r6() {
      const R = new Room("ug_r6", { name: "Underground Path", floor: "Route 6 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ns", downSpawn: { x: 1.6, z: NS_D - 2.8, yaw: FACE.n }, sign: "This way to ROUTE 5 (CERULEAN CITY).",
        person: { name: "Girl", key: "underground_path_route_6_p0", x: 4.6, z: 8.0, yaw: FACE.e, look: { skin: "girl" },
          talk: () => [{ text: "People often lose things in that UNDERGROUND PATH." }] } });
      return R;
    },
    ug_r7() {
      const R = new Room("ug_r7", { name: "Underground Path", floor: "Route 7 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ew", downSpawn: { x: 5.2, z: 1.0, yaw: FACE.e }, sign: "This way to ROUTE 8 (LAVENDER TOWN).",
        person: { name: "Man", key: "underground_path_route_7_p0", x: 9.4, z: 8.0, yaw: FACE.w, look: { skin: "middle_aged_man" },
          talk: () => [{ text: "I heard a sleepy POKéMON appeared near CELADON CITY." }, { text: "The guards on SAFFRON's gates look awfully thirsty." },
            { text: "My gran in CELADON makes TEA. Here, take some!" }] } });
      return R;
    },
    ug_r8() {
      const R = new Room("ug_r8", { name: "Underground Path", floor: "Route 8 entrance", W: 14, D: 12 });
      ugEntrance(R, { down: "ug_ew", downSpawn: { x: EW_W - 5.2, z: 1.0, yaw: FACE.w }, sign: "This way to ROUTE 7 (CELADON CITY).",
        person: { name: "Girl", key: "underground_path_route_8_p0", x: 4.6, z: 8.0, yaw: FACE.e, look: { skin: "girl" },
          talk: () => [{ text: "The dept. store in CELADON has a great selection!" }] } });
      return R;
    },
    ug_ns() {
      // ROUTE 5 to ROUTE 6, under SAFFRON CITY: stairs up at the north (to Route 5's entrance) and the south (Route 6's).
      const R = new Room("ug_ns", { name: "Underground Path", floor: "North to south", W: 7, D: NS_D, H: 4 });
      ugTunnel(R, "z");
      ugFlight(R, 3, 0, 2, "e", "s", { to: "ug_r5", spawn: UG_UP });
      ugFlight(R, 3, NS_D - 2, 2, "e", "n", { to: "ug_r6", spawn: UG_UP });
      R.spawn = { x: 1.6, z: 2.8, yaw: FACE.s };
      return R;
    },
    ug_ew() {
      // ROUTE 7 to ROUTE 8: stairs up at the west (to Route 7's entrance) and the east (Route 8's), along the north wall.
      const R = new Room("ug_ew", { name: "Underground Path", floor: "West to east", W: EW_W, D: 7, H: 4 });
      ugTunnel(R, "x");
      ugFlight(R, 0, 0, 2, "w", "s", { to: "ug_r7", spawn: UG_UP });
      ugFlight(R, EW_W - 4, 0, 2, "e", "s", { to: "ug_r8", spawn: UG_UP });
      R.spawn = { x: 5.2, z: 1.0, yaw: FACE.e };
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
  // (Viewer-only: in the mod the pool is real water and quartz, tools/cerulean_pool.py.) The water as outside's: deep blue,
  // ripples of light, seen into; its dark prismarine bottom; the white quartz.
  reg("poolWaterView", 32, 32, (P, r) => {
    P.fill(hx("#3567C8"), 0.03, r);
    for (let i = 0; i < 16; i++) { const x = Math.floor(r() * 28), y = Math.floor(r() * 30); P.rect(x, y, 3 + Math.floor(r() * 4), 1, hx("#6E9BE8")); }
    for (let i = 0; i < 10; i++) { const x = Math.floor(r() * 30), y = Math.floor(r() * 30); P.rect(x, y, 2, 1, hx("#264E9E")); }
  }, { per: 2, alpha: 0.72 });
  // (0.48: tiled, as the game's look now has it: blue tiles at the bottom, white tiles on the walkways, 17:51.)
  reg("poolBottomView", 16, 16, (P, r) => { P.fill(hx("#3D86C6"), 0.03, r); for (let i = 3; i < 16; i += 4) { P.rect(0, i, 16, 1, hx("#2C6599")); P.rect(i, 0, 1, 16, hx("#2C6599")); } });
  reg("poolQuartzView", 16, 16, (P, r) => { P.fill(hx("#F4F7F8"), 0.015, r); for (let i = 3; i < 16; i += 4) { P.rect(0, i, 16, 1, hx("#B9CCD6")); P.rect(i, 0, 1, 16, hx("#B9CCD6")); } });
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
      // CERULEAN GYM, laid out as FireRed's (a step a block): stands at the back, a deep pool filling the hall (real
      // water in the mod, five deep, as the water outside: tools/cerulean_pool.py), the white walkways its edges
      // winding from the deck at the door to MISTY at the top. You can't swim (not yet): the water stops you.
      // PICNICKER DIANA on the left arm, SWIMMER LUIS on his float.
      const L = CERULEAN_POOL, W = L.W, D = L.D, base = L.base;
      const R = new Room("cerulean_gym", { name: "Cerulean Gym", floor: "Cerulean City", W, D, H: 5 });
      const stands = [0, 0, W, 2], deck = [0, 14, W, D - 2];
      shell(R, { paper: "ceruleanWall", floor: "ceruleanTile", door: [6, 8], entry: true, skirt: "ceruleanTrim", ledge: "ceruleanTrim", ledgeTop: "poolEdge",
        floorRects: [stands, deck, [5, D - 2, 9, D]] });
      band(R, 0.3, "ceruleanTrim", 0.9); band(R, 1.2, "poolEdge", 0.1);
      const G = R.geo;
      // The pool: the water's surface (just under the walkways' tops, as outside), its dark bottom five down; the
      // walkways and the edges white quartz all the way down (in the mod these are real blocks: viewer-only here).
      const P0 = L.pool, walks = [...L.walks, L.float];
      let water = [[P0[0], P0[1], P0[2], P0[3]]];
      for (const w of walks) water = cut(water, w);
      for (const [x0, z0, x1, z1] of water) {
        G.box(x0, base + 1, z0, x1, -0.11, z1, { top: "poolWaterView", all: null });
        G.box(x0, base, z0, x1, base + 1, z1, { top: "poolBottomView", all: null });
        R.solid(x0, z0, x1, z1);
      }
      for (const [x0, z0, x1, z1] of [...walks, ...L.edges.filter(e => e[1] < 14)]) G.box(x0, base, z0, x1, 0, z1, { all: "poolQuartzView", bottom: null });
      G.box(0, base, 14, W, 0, 15, { all: "poolQuartzView", top: null, bottom: null });
      // The edges along the stands and down both sides: you can't walk round on them.
      for (const e of L.edges) if (e[1] < 14) R.solid(e[0], e[1], e[2], e[3]);
      R.item("Pool", "A deep pool filling the hall (real water in the game, five deep, as the water outside); the white walkways are its edges. No swimming yet: the water stops you.");
      // The stands at the back: two tiers, red and blue seats.
      G.box(0.6, 0, 0, W - 0.6, 0.5, 1.9, { all: "ceruleanTrim", top: "poolEdge" }); G.box(0.6, 0.5, 0, W - 0.6, 1.0, 0.9, { all: "ceruleanTrim", top: "poolEdge" });
      for (let x = 1.4, i = 0; x < W - 1; x += 1.6, i++) { G.box(x, 0.5, 1.2, x + 0.8, 0.75, 1.8, i % 2 ? "standSeatBlue" : "standSeat"); G.box(x, 1.0, 0.15, x + 0.8, 1.25, 0.75, i % 2 ? "standSeat" : "standSeatBlue"); }
      R.solid(0, 0, W, 2);
      // MISTY's spot at the top: a stage tile on the walkway; LUIS's float, a ring on it.
      G.box(6.2, 0, 4.15, 7.8, 0.06, 5.85, { top: "gymStage", all: "poolEdge", bottom: null });
      G.box(L.float[0] + 0.08, 0, L.float[1] + 0.08, L.float[2] - 0.08, 0.1, L.float[3] - 0.08, { top: "standSeat", all: "standSeat", bottom: null });
      gymStatue(R, 4.4, 15.6, ["CERULEAN CITY POKéMON GYM", "LEADER: MISTY", "WINNING TRAINERS: {rival}"]);
      gymStatue(R, 9.6, 15.6, ["CERULEAN CITY POKéMON GYM", "LEADER: MISTY", "WINNING TRAINERS: {rival}"]);
      for (const [x, z] of [[0.8, 16.2], [13.2, 16.2]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Leader Misty", key: "cerulean_gym_0", role: "leader", x: 7.0, z: 5.0, y: 0.06, yaw: FACE.s, look: { skin: "leader_misty" },
        talk: () => [{ text: "MISTY: Hi, you're a new face! My policy is an all-out offensive with WATER-type POKéMON!" }, { text: "(In the game: you battle her and win the CASCADEBADGE.)" }] });
      R.npcs.push({ name: "Picnicker Diana", key: "cerulean_gym_1", role: "trainer", x: 2.5, z: 5.5, yaw: FACE.e, look: { skin: "cooltrainer_f" },
        talk: () => [{ text: "I'm more than good enough for you! MISTY can wait!" }] });
      R.npcs.push({ name: "Swimmer Luis", key: "cerulean_gym_2", role: "trainer", x: 11.5, z: 11.5, y: 0.1, yaw: FACE.w, look: { skin: "swimmer" },
        talk: () => [{ text: "Splash! I'm first up! Let's do it!" }] });
      R.npcs.push({ name: "Gym guide", key: "cerulean_gym_p0", x: 11.6, z: 15.8, yaw: FACE.w, look: { skin: "gym_guide" },
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
      framed(R, 2.4, 1.8, 3.0, 0.75, "badgeCase", "w", { act: { label: "Look", lines: ["A badge case: eight places, each badge drawn in its colour."] } });
      // (17:42) FireRed's back door, out to the path behind.
      passageN(R, 9.4, 11.2, { to: "town", mat: "matHouseVN" });
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
      // (17:42) The hole TEAM ROCKET made: a way out the back (FireRed's), broken bricks round it.
      passageN(R, 9.4, 11.2, { to: "town", mat: "matHouseVN", frame: "cap", opening: "deep" });
      for (const [x, z, w] of [[8.9, 0.3, 0.4], [11.4, 0.4, 0.35], [9.0, 2.2, 0.3], [11.0, 2.4, 0.25]]) G.box(x, 0, z, x + w, w * 0.6, z + w * 0.8, "stone");
      for (const [x, z, w] of [[2.4, 7.6, 0.5], [5.2, 8.2, 0.4], [8.2, 6.8, 0.6], [7.6, 3.0, 0.45]]) G.box(x, 0, z, x + w, 0.02, z + w * 0.7, { top: "paperSchool", all: null });
      R.act([8.4, 0, 9.4, 1.2], { label: "Look", lines: ["TEAM ROCKET left through here. There's a hole in the wall!"] });
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
      // (0.48) The pen: a patch of lawn with white pickets round it, where the POKéMON he's raising play (CLEFAIRY and
      // PSYDUCK, and yours when you leave one with him), their toys about.
      G.box(6.4, 0, 2.3, 14.0, 0.04, 7.6, { top: "grassMown", all: "cap", bottom: null });
      picket(R, 6.4, 2.3, 14.0, 2.3); picket(R, 6.4, 7.6, 14.0, 7.6); picket(R, 6.4, 2.3, 6.4, 7.6);
      toyBox(R, 11.8, 0.6, 13.4, 1.6); R.solid(11.8, 0.6, 13.4, 1.6);
      for (const [x, z, c] of [[7.4, 5.8, "standSeat"], [10.2, 3.2, "standSeatBlue"], [12.8, 6.6, "ceruleanTrim"]]) roundBall(G, x, 0.04, z, 0.36, { all: c, top: c });
      R.pets = [{ sid: "clefairy", x: 8.0, z: 4.0, yaw: 210 }, { sid: "psyduck", x: 11.6, z: 4.6, yaw: 150 }];
      for (const pt of R.pets) R.act([pt.x - 1.1, pt.z - 1.1, pt.x + 1.1, pt.z + 1.1], { label: "Look", lines: [pt.sid.toUpperCase() + " is playing in the pen.", "(It's being looked after at the DAY CARE.)"] });
      R.act([8.9, 5.3, 10.9, 7.2], { label: "Look", lines: ["Your POKéMON plays here while you leave it with him."] });
      sofa(R, 1.0, 6.2, "e", 2.4);
      crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, ["Outside: the DAY CARE's yard. POKéMON are playing in the grass."]);
      plant(R, 0.8, 8.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Day Care man", key: "daycare_p0", x: 2.4, z: 1.8, yaw: FACE.s, look: { skin: "gentleman" },
        talk: () => [{ text: "I run a DAY CARE. Would you like me to raise one of your POKéMON?" }, { text: "(In the game: YES, pick one, and it grows a point a step. Back for ¥100, and ¥100 more for each level it's grown.)" }] });
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

// ---------- the Power Plant on Route 10 (16:58): FireRed's hall of generators, ZAPDOS, ELECTRODE posing as items ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  reg("ppFloor", 16, 16, (P, r) => { P.fill(hx("#E7DCC2"), 0.03, r); P.rect(0, 0, 16, 1, hx("#CDBF9F")); P.rect(0, 0, 1, 16, hx("#CDBF9F")); P.rect(7, 7, 2, 2, hx("#D8CBAD")); });
  reg("ppLane", 16, 16, (P, r) => { P.fill(hx("#B9BDC2"), 0.03, r); P.rect(0, 0, 16, 1, hx("#D2D6DA")); P.rect(0, 15, 16, 1, hx("#9A9FA5")); });
  reg("ppWall", 16, 16, (P, r) => { P.fill(hx("#C9CFD6"), 0.02, r); P.rect(0, 0, 16, 1, hx("#E2E6EA")); P.rect(0, 0, 1, 16, hx("#AEB5BE")); for (const y of [5, 11]) P.rect(2, y, 12, 1, hx("#B7BEC7")); });
  reg("ppMachine", 16, 16, (P, r) => {
    P.fill(hx("#5D7895"), 0.03, r); P.rect(0, 0, 16, 2, hx("#8FA8C2")); P.rect(0, 14, 16, 2, hx("#3E5268"));
    for (const y of [4, 6, 8, 10]) P.rect(3, y, 10, 1, hx("#45596F"));
    P.set(13, 3, hx("#F5D76E")); P.set(13, 5, hx("#7CF5B0"));
  });
  reg("ppMachineTop", 16, 16, (P, r) => { P.fill(hx("#7891AB"), 0.03, r); P.rect(0, 0, 16, 1, hx("#A9BED3")); P.rect(2, 2, 12, 12, hx("#6A829C")); P.rect(4, 4, 8, 8, hx("#5D7590")); });
  reg("ppStripe", 16, 16, (P, r) => { P.fill(hx("#2E3136"), 0, r); for (let x = -16; x < 16; x += 6) for (let i = 0; i < 3; i++) P.line(x + i, 15, x + i + 15, 0, hx("#F2C230")); });
  reg("ppCoil", 16, 16, (P, r) => { P.fill(hx("#B87333"), 0.03, r); for (let y = 0; y < 16; y += 2) P.rect(0, y, 16, 1, hx("#D9964F")); });
  reg("ppPanel", 32, 16, (P, r) => { P.fill(hx("#2E3A50"), 0.02, r); for (let i = 0; i < 6; i++) P.rect(3 + i * 5, 4, 3, 2, hx(i % 3 ? "#F5D76E" : "#E2665A")); P.rect(3, 10, 26, 2, hx("#4F6A8E")); }, { fit: true, glow: 0.5 });

  // A row of generators along x (from x0 to x1, at z0 to z1): blue-grey cabinets, a hazard stripe at the foot, copper
  // coils and a pipe along the top, a lit panel on each.
  function generators(R, x0, z0, x1, z1) {
    const G = R.geo, h = 1.7;
    G.box(x0, 0, z0, x1, 0.22, z1, { all: "ppStripe", top: null, bottom: null });
    G.box(x0, 0.22, z0, x1, h, z1, { all: "ppMachine", top: "ppMachineTop", bottom: null });
    for (let x = x0 + 1; x < x1 - 0.5; x += 3) {
      G.box(x, h, z0 + 0.2, x + 1.0, h + 0.5, z1 - 0.2, { all: "ppCoil", top: "steel" });
      G.box(x + 0.15, 0.6, z1, x + 0.85, 1.1, z1 + 0.02, { s: "ppPanel", all: null });
    }
    G.box(x0, h + 0.55, (z0 + z1) / 2 - 0.12, x1, h + 0.8, (z0 + z1) / 2 + 0.12, "steel");
    R.solid(x0, z0, x1, z1);
  }
  PLACES.push({ id: "power_plant", name: "Power Plant", floor: "Route 10", group: "Route 10" });
  BUILD.power_plant = function () {
    // The POWER PLANT (Red and Blue's 40 by 36 steps, a step a block, so everything's where the games have it): its
    // hall of generators in rows, ZAPDOS by the big machine at the back on the left, ELECTRODE and VOLTORB posing as
    // item balls, and its items. The way in at the bottom left.
    const W = 40, D = 36;
    const R = new Room("power_plant", { name: "Power Plant", floor: "Route 10", W, D, H: 5 });
    shell(R, { paper: "ppWall", floor: "ppFloor", door: [4, 6], entry: true, skirt: "ppStripe", ledge: "ppMachine", ledgeTop: "steel" });
    band(R, 0.3, "ppMachine", 0.9); band(R, 1.2, "steel", 0.08);
    const G = R.geo;
    // Lanes of grey floor between the rows.
    for (const [z0, z1] of [[10, 13], [17, 20], [24, 27], [30, 33]]) G.box(0, 0, z0, W, 0.012, z1, { top: "ppLane", all: null });
    // The machines along the back wall, and the rows (their gaps the way through, as FireRed's maze).
    generators(R, 0.4, 0.2, 39.6, 1.4);
    generators(R, 0, 7, 14, 8.6); generators(R, 18, 7, 30, 8.6); generators(R, 34, 7, 40, 8.6);
    generators(R, 4, 14, 22, 15.6); generators(R, 26, 14, 40, 15.6);
    generators(R, 0, 21, 10, 22.6); generators(R, 14, 21, 34, 22.6);
    generators(R, 8, 27.8, 28, 29.4); generators(R, 32, 27.8, 40, 29.4);
    // ZAPDOS's big machine: a tall generator, coils round it.
    G.box(1.0, 0, 2.2, 3.4, 3.2, 4.6, { all: "ppMachine", top: "ppMachineTop" }); G.box(1.3, 3.2, 2.5, 3.1, 3.8, 4.3, "ppCoil"); R.solid(1.0, 2.2, 3.4, 4.6);
    R.act([0.8, 2.0, 3.6, 5.0], { label: "Look", lines: ["A huge generator, humming. The air round it crackles."] });
    for (const [x, z] of [[38.8, 33.0], [9.0, 33.0]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A dusty plant in a white pot."] });
    R.item("Generators", "FireRed's Power Plant: rows of blue-grey generators, hazard stripes at their feet, copper coils and a pipe along the top, lit panels.");
    doormat(R, 4, 6, { to: "town", mat: "matGate" });
    R.spawn = { x: 5.0, z: D - 1.5, yaw: FACE.n };
    return R;
  };
})();

// ---------- the 0.47.0 looks on show (16:17, 16:58): Cerulean City, Nugget Bridge, the hedges, Bill's, the Power Plant ----------
(function () {
  PLACES.push({ id: "cerulean_looks", name: "Cerulean and Power Plant looks", floor: "Outside (the mod's blocks)", group: "Outside" });
  BUILD.cerulean_looks = function () {
    // On John's blocks as the mod draws them: Cerulean's paving (his checker) with its kerb; a hedge (three) and a bush;
    // a house corner (brick foot, pillar, white trim, the teal roof); a Cerulean lamp in off-white; a stretch of Nugget
    // Bridge in gold; Bill's chimney on his bamboo roof; a corner of the Power Plant (its iron, yellow and red metal, a
    // stack, its red tower).
    const R = new Room("cerulean_looks", { name: "Cerulean and Power Plant looks", floor: "Outside (the mod's blocks)", W: 30, D: 22, H: 9, outside: true });
    ground(R, "grass");
    const G = R.geo, B = (x, y, z, m, o = {}) => G.box(x, y, z, x + 1, y + 1, z + 1, Object.assign({ all: m }, o));
    // The paving (4 by 6) inside its kerb.
    const pave = ["area_pave_cer_1", "area_pave_cer_2", "area_pave_cer_3", "area_pave_cer_4"];
    for (let x = 2; x < 10; x++) for (let z = 13; z < 20; z++) {
      const edge = x === 2 || x === 9 || z === 13 || z === 19;
      G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: edge ? "area_kerb_cer" : pave[(x + z) % 4], all: null });
    }
    R.item("Paving", "Cerulean's checker as paving stones in grey-blue tones, inside a light kerb (the smooth stone border), the grass rounded onto it.");
    // A hedge three long and a bush on its own: trimmed, its ends rounded.
    const hedge = (x, z, mask, kind = "az") => B(x, 0, z, "area_hedge_" + kind + "_" + mask);
    hedge(12, 17, 13); hedge(13, 17, 5); hedge(14, 17, 7); hedge(16, 17, 15, "do");
    R.solid(12, 17, 15, 18); R.solid(16, 17, 17, 18);
    R.item("Hedges", "Leaves on the ground as a trimmed hedge: one green along a row, rounded off where it ends; a bush on its own rounded all round.");
    // A house corner.
    for (let x = 2; x < 7; x++) B(x, 0, 6, "area_base_cer");
    for (let y = 1; y < 4; y++) { B(2, y, 6, "area_pillar_cer"); for (let x = 3; x < 7; x++) B(x, y, 6, x === 4 && y === 2 ? "area_trim_cer" : "mcCream"); }
    for (let x = 1; x < 8; x++) G.box(x, 4, 5.5, x + 1, 4.5, 7.2, { top: "area_roof_cer_tiles", all: "area_roof_cer_edge" });
    G.box(3, 0, 7, 5, 0.5, 8, { all: "area_step_cer" });
    R.solid(1, 5.5, 8, 7.2); R.solid(3, 7, 5, 8);
    // A lamp.
    for (let y = 0; y < 5; y++) G.box(9.25, y, 7.25, 9.75, y + 1, 7.75, "area_lamp_white_post");
    G.box(8.5, 5, 6.5, 10.5, 5.5, 8.5, { all: "area_lamp_white_cap" }); G.box(8.9, 5.5, 6.9, 9.4, 5.6, 8.1, { top: "area_lamp_solar", all: "area_lamp_white_cap" });
    R.solid(9.2, 7.2, 9.8, 7.8);
    // Nugget Bridge, in gold.
    for (let z = 1; z < 12; z++) { G.box(19, 0, z, 23, 0.5, z + 1, { top: "area_bridge_gold_boards", all: "area_bridge_gold_beam" }); }
    for (const x of [18.8, 23]) { G.box(x, 0.5, 1, x + 0.2, 1.4, 12, { all: "area_bridge_gold_rail" }); R.solid(x, 1, x + 0.2, 12); }
    // Bill's chimney on his roof.
    for (let x = 12; x < 17; x++) G.box(x, 0, 2, x + 1, 1, 4, { top: "area_bill_roof", all: "area_bill_roof" });
    for (let y = 1; y < 3; y++) B(14, y, 2.5, "area_chimney_stone");
    G.box(13.6, 3, 2.1, 15.4, 3.3, 3.9, { all: "area_bill_shelf" }); R.solid(12, 2, 17, 4);
    // The Power Plant's corner.
    for (let y = 0; y < 3; y++) for (let x = 25; x < 29; x++) B(x, y, 8, y === 0 ? "area_pp_front" : "area_pp_iron");
    for (let x = 25; x < 29; x++) B(x, 3, 8, "area_pp_yellow");
    for (let y = 4; y < 8; y++) B(26, y, 8.2, y % 2 ? "area_pp_white" : "area_pp_red");
    G.box(26, 8, 8.2, 27, 8.1, 9.2, { top: "area_pp_grate", all: "area_pp_white" });
    for (let y = 0; y < 3; y++) B(24, y, 11, "area_pp_red_frame");
    for (let x = 25; x < 29; x++) G.box(x, 4, 9, x + 1, 4.4, 10, { all: "area_pp_roof" });
    R.solid(24, 8, 29, 12);
    R.spawn = { x: 15, z: 20.5, yaw: FACE.n };
    return R;
  };
})();
