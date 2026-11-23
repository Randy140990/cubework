// ---------- 0.56.0's looks on show (John's 13:53 and 13:55): Johto's first towns cleaned up ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // John's light grey concrete (Blackthorn's walls) and white terracotta (Cherrygrove's), as the game draws them.
  reg("l56Grey", 16, 16, P => wallCrisp(P, hx("#B4B7B9"), { stripes: 0.02 }));
  reg("l56WhiteTerra", 16, 16, P => wallCrisp(P, hx("#E3D7CD"), { stripes: 0.02 }));
  PLACES.push({ id: "looks56", name: "Johto cleaned up (0.56)", floor: "Outside (the mod's blocks)", group: "Outside" });
  BUILD.looks56 = function () {
    // On John's blocks as the mod draws them now: a Blackthorn house (its brown tiled roof, the wooden band between
    // its floors, its dark timber posts, its dark windows, its step and planters); a Cherrygrove house's gable (the
    // wooden trim under its top window, its chimney and the chimney's stone cap) and a Cherrygrove lamp's foot; a
    // Mart's front (white pillars, a white step, the white disc with the blue symbol); Route 45's bridge (one run of
    // boards, its rails to the ends); Route 27's steel posts and chain; Johto's path as Kanto's, its steps; the
    // Goldenrod DEPT. STORE's facing.
    const R = new Room("looks56", { name: "Johto cleaned up (0.56)", floor: "Outside (the mod's blocks)", W: 38, D: 26, H: 10, outside: true });
    ground(R, "grass");
    const G = R.geo, B = (x, y, z, m, o = {}) => G.box(x, y, z, x + 1, y + 1, z + 1, Object.assign({ all: m }, o));

    // A Blackthorn house: the front wall (x 1..9, z 3), two floors and the band between, the roof above.
    for (let x = 1; x < 9; x++) for (let y = 0; y < 6; y++) {
      if (y === 3) { B(x, y, 3, "area_band_shelf"); continue; }
      const win = (y === 1 || y === 4) && (x === 2 || x === 3 || x === 6 || x === 7);
      const low = (y === 0 || y === 3) && (x === 2 || x === 3 || x === 6 || x === 7);
      if (x === 4 || x === 5) { if (y < 2) { G.box(x, y, 3.05, x + 1, y + 1, 3.1, { s: "area_door_bt_" + (y ? "top" : "bottom"), all: null }); continue; } }
      B(x, y, 3, win ? "area_glass_blue_0" : low && y === 0 ? "area_window_low" : "l56Grey");
    }
    G.box(0.6, 2.85, 2.6, 9.4, 3.0, 4.4, { all: "area_trim_jungle" });
    for (const x of [0.5, 8.6]) G.box(x, 0, 3.9, x + 0.9, 3, 4.8, { all: "area_post_bt", top: "area_post_bt_top" });
    G.box(4, 0, 4.0, 6, 0.5, 5.0, { all: "area_house_stone" });
    for (const x of [1, 6.6]) G.box(x, 0, 4.2, x + 2.4, 0.6, 4.9, { all: "area_shelf_wood" });
    for (let i = 0; i < 3; i++) G.box(0.4 + i * 0.9, 6 + i * 0.6, 2.4 + i * 0.3, 9.6 - i * 0.9, 6.6 + i * 0.6, 4.6 - i * 0.3, { top: "area_roof_bt_tiles", all: "area_roof_bt_edge", bottom: "area_roof_bt_under" });
    R.solid(0.5, 2.4, 9.5, 5);
    R.item("Blackthorn's houses", "Their roofs in tiles of John's warm brown (no rock), the band between the floors clean wood, the dark windows Pallet's, its door a crisp painted door in his teal, the dark timber posts either side, the stone step, the wooden planters.");

    // A Cherrygrove gable: white walls, the wooden trim round its top window, the chimney with its stone cap.
    for (let x = 12; x < 17; x++) for (let y = 0; y < 4; y++) B(x, y, 3, y === 2 && (x === 14) ? "area_glass_blue_0" : "l56WhiteTerra");
    G.box(13.6, 1.8, 2.7, 15.4, 2.0, 4.1, { all: "area_trim_jungle" });
    G.box(13.6, 2.95, 2.7, 15.4, 3.1, 4.1, { all: "area_trim_jungle" });
    for (let i = 0; i < 2; i++) G.box(11.6 + i * 0.8, 4 + i * 0.6, 2.4, 17.4 - i * 0.8, 4.6 + i * 0.6, 4.6, { top: "area_roof_tiles", all: "area_roof_edge", bottom: "area_roof_under" });
    for (let y = 4; y < 7; y++) B(15.5, y, 2.8, "area_chimney_brick");
    G.box(15.35, 7, 2.65, 16.65, 7.35, 3.95, { all: "area_cap_shelf" });
    G.box(15.6, 7.35, 2.9, 16.4, 7.6, 3.7, { all: "area_chimney_stone" });
    R.solid(11.6, 2.4, 17.4, 4.6);
    // Its lamp: the post, its foot (no chisel left).
    G.box(18.3, 0, 4.3, 18.9, 0.8, 4.9, { all: "area_lamp_cg_chisel" });
    for (let y = 0.8; y < 3.6; y += 0.7) G.box(18.45, y, 4.45, 18.75, y + 0.7, 4.75, { all: "area_lamp_cg_post" });
    G.box(18.0, 3.6, 4.0, 19.2, 3.9, 5.2, { all: "area_lamp_cg_cap" });
    R.solid(18.3, 4.3, 18.9, 4.9);
    R.item("Cherrygrove's houses", "Pallet's houses, John's jungle wood round their windows a clean wooden trim (not the cliffs' rock), their chimneys brick with a stone cap; the lamps' feet the lamp's own red metal.");

    // A Mart's front: white pillars and step, the white disc with the Mart's blue symbol in front of it.
    for (let x = 21; x < 29; x++) for (let y = 0; y < 4; y++) if (!(x >= 24 && x < 26 && y < 2)) B(x, y, 3, "area_mart_blue_concrete");
    for (const x of [23, 26]) for (let y = 0; y < 3; y++) G.box(x + 0.1, y, 3.6, x + 0.9, y + 1, 4.4, { all: "area_quartz_clean" });
    G.box(24, 0, 3.4, 26, 2, 3.6, { all: "area_door_glass_bottom" });
    G.box(23.6, 0, 4, 26.4, 0.25, 5, { all: "area_quartz_clean" });
    G.box(23.4, 2.15, 4.0, 26.6, 3.95, 4.3, { all: "area_quartz_clean" });
    G.box(24.3, 2.45, 4.3, 25.7, 3.65, 4.5, { all: "area_mart_sign_face" });
    R.solid(21, 3, 29, 4.4);
    R.item("Every Mart (and Center)", "The pillars by the door and the step pure white; a Mart's sign as a Center's: the white disc, the symbol in front of it the Mart's blue (the Center's is its red).");

    // Goldenrod's DEPT. STORE: its blue facing, its red corners, the white ledges round each floor, its glass, its roof.
    for (let x = 31; x < 37; x++) for (let y = 0; y < 7; y++) {
      const ledge = y % 3 === 0, corner = x === 31 || x === 36;
      B(x, y, 3, corner ? "area_ds_red" : ledge ? "area_quartz_clean" : y % 3 === 1 && (x === 33 || x === 34) ? "area_ds_blue" : y % 3 === 2 ? "area_ds_blue_light" : "area_glass_blue_0");
    }
    G.box(30.8, 7, 2.8, 37.2, 7.5, 4.2, { all: "area_ds_roof" });
    G.box(32.5, 7.5, 3.2, 35.5, 8.4, 3.8, { all: "area_ds_steel" });
    R.solid(30.8, 2.8, 37.2, 4.2);
    R.item("Goldenrod's DEPT. STORE", "Its blue facing and paler band crisp blue panels, its red corners and roof crisp red, the ledges round every floor clean white stone, the steel on its top clean.");

    // Route 45's bridge: one run of boards (its stairs, slabs and the andesite steps on, the same), the rails to
    // their ends, the bolts, the posts under it.
    for (let x = 1; x < 12; x++) G.box(x, 0.5, 12, x + 1, 0.7, 15, { top: "area_bridge_boards", all: "area_bridge_boards" });
    for (const z of [11.8, 14.9]) G.box(1, 0.7, z, 12, 1.6, z + 0.3, { all: "area_bridge_beam" });
    for (const x of [3, 9]) for (const z of [12.1, 14.5]) G.box(x, -0.4, z, x + 0.4, 0.5, z + 0.4, { all: "area_bridge_beam" });
    R.solid(1, 11.8, 12, 15.2);
    R.item("Route 45's bridges", "One run of boards end to end (no stripes of stairs and beams), the andesite steps on as boards, the rails to the very ends, the bolts, the posts under it beams.");

    // Route 27's barrier: steel posts, the chain hung between them.
    for (let i = 0; i < 5; i++) {
      const x = 14 + i * 2;
      G.box(x - 0.12, 0, 13.38, x + 0.12, 1.0, 13.62, { all: "area_post_steel" });
      if (i < 4) G.box(x + 0.12, 0.62, 13.47, x + 1.88, 0.8, 13.53, { s: "area_chain_steel", n: "area_chain_steel", all: null });
    }
    R.solid(13.8, 13.3, 22.2, 13.7);
    R.item("Route 27's cliff top", "Its posts steel, the chain between them steel too: one barrier.");

    // Johto's path as Kanto's, the steps up a ledge.
    for (let x = 24; x < 36; x++) for (let z = 10; z < 16; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: "area_path_kanto", all: null });
    for (let i = 0; i < 3; i++) G.box(27, 0, 16 + i, 31, 0.5 + i * 0.5, 17 + i, { top: "area_step_boards_d", all: "area_step_boards_d" });
    R.solid(27, 16, 31, 19);
    R.item("Johto's paths and steps", "Its dirt paths and the brown powder at their edges Kanto's path (no squares), the grass onto them as Kanto's; John's mud brick steps Kanto's steps.");
    R.spawn = { x: 18, z: 21, yaw: FACE.n };
    return R;
  };
})();
