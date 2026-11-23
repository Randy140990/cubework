// ---------- 0.57.0's looks on show (John's review, 15:17 to 16:35): across the map, Cinnabar, Violet, Ecruteak, the coast ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  reg("l57Grey", 16, 16, P => wallCrisp(P, hx("#B4B7B9"), { stripes: 0.02 }));
  reg("l57White", 16, 16, P => wallCrisp(P, hx("#ECEAE4"), { stripes: 0.02 }));
  reg("l57Cobble", 16, 16, P => wallCrisp(P, hx("#8E8A84"), { stripes: 0.06 }));
  reg("l57Moss", 16, 16, P => wallCrisp(P, hx("#5E8F46"), { stripes: 0.06 }));
  PLACES.push({ id: "looks57", name: "Kanto and Johto, the review (0.57)", floor: "Outside (the mod's blocks)", group: "Outside" });
  BUILD.looks57 = function () {
    const R = new Room("looks57", { name: "Kanto and Johto, the review (0.57)", floor: "Outside (the mod's blocks)", W: 56, D: 34, H: 12, outside: true });
    ground(R, "grass");
    const G = R.geo, B = (x, y, z, m, o = {}) => G.box(x, y, z, x + 1, y + 1, z + 1, Object.assign({ all: m }, o));
    const door = (x, z, k) => { G.box(x, 0, z, x + 1, 1, z + 0.18, { all: "area_door_" + k + "_bottom" }); G.box(x, 1, z, x + 1, 2, z + 0.18, { all: "area_door_" + k + "_top" }); };

    // Every door, crisp: a row of them, each wood and metal.
    const DOORS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped", "iron", "copper", "oxidized_copper"];
    DOORS.forEach((k, i) => { B(1 + i * 1.2, 2, 2, "l57White"); door(1 + i * 1.2, 2.82, k); });
    R.solid(1, 2, 19, 3);
    R.item("Every door", "Redrawn crisp, each wood and metal its own colour: a frame, two inset panels, a window above, a brass handle (the metal ones riveted, a small window).");

    // A chimney with its cap of shelves as a plain stone board; the planters' boards; Viridian's green board.
    for (let y = 0; y < 4; y++) B(21, y, 2, "area_chimney_brick");
    G.box(20.7, 4, 1.7, 22.3, 4.35, 3.3, { all: "area_cap_shelf" });
    G.box(21.1, 4.35, 2.1, 21.9, 4.6, 2.9, { all: "area_chimney_stone" });
    G.box(23, 0, 2, 26, 0.6, 2.32, { all: "area_panel_planter_dark" });
    G.box(23, 0, 3.6, 26, 0.6, 3.92, { all: "area_sz_green_shelf" });
    R.solid(20.7, 1.7, 26, 4);
    R.item("Shelves as boards", "John's shelves round the chimneys, under the windows and round the planters drawn as plain flat boards: no lip, no recess, no shelf.");

    // The cobblestone with the moss onto it.
    for (let x = 28; x < 34; x++) for (let z = 1; z < 5; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: x < 31 ? "l57Cobble" : "l57Moss", all: null });
    G.box(30, 0.012, 1, 31, 0.013, 5, { top: "area_moss_on_cobble_8", all: null });
    R.item("Cobblestone and its moss", "Where the mossy cobblestone meets the cobblestone, the moss runs onto it in soft tufts (Route 1, Route 2 and everywhere).");

    // Cinnabar: a pier piece, the Mansion's wall and shutter, the Lab's wall with a solar panel, the steam and lava.
    for (let x = 36; x < 42; x++) G.box(x, -0.2, 2, x + 1, 0.1, 5, { top: "area_cin_deck", all: "area_cin_edge" });
    for (const x of [36, 41.6]) G.box(x, 0.1, 4.7, x + 0.35, 1, 5.05, { all: "area_cin_post", top: "area_cin_post_top" });
    G.box(36.2, 0.85, 4.82, 41.8, 0.9, 4.92, { all: "area_rope_tan" });
    R.solid(36, 2, 42, 5);
    R.item("Cinnabar's piers", "Let's Go's: pale grey boards, a white edge with its orange line, dark posts, a rope between them.");
    for (let x = 44; x < 49; x++) for (let y = 0; y < 3; y++) B(x, y, 2, y === 0 ? "area_mansion_base" : x === 46 && y === 1 ? "area_mansion_shutter" : "area_mansion_wall");
    for (let i = 0; i < 2; i++) G.box(43.6 + i * 0.6, 3 + i * 0.5, 1.6, 49.4 - i * 0.6, 3.5 + i * 0.5, 3.4, { top: "area_roof_mansion_tiles", all: "area_roof_mansion_edge", bottom: "area_roof_mansion_under" });
    R.solid(43.6, 1.6, 49.4, 3.4);
    R.item("The POKéMON MANSION", "A cream render on a grey stone base, dark brown tiles, pale shutters, a white trim.");
    for (let x = 50; x < 55; x++) for (let y = 0; y < 2; y++) B(x, y, 2, "area_lab_cin_wall");
    for (let x = 50; x < 55; x++) B(x, 2, 2, "area_lab_cin_roof");
    G.box(51, 3, 2.1, 52, 3.08, 2.9, { all: "area_solar_cells" });
    G.box(53, 3, 2.1, 54, 3.08, 2.9, { all: "area_solar_cells" });
    R.solid(50, 2, 55, 3);
    R.item("The POKéMON LAB", "White panels, a dark grey roof with solar panels on it, a copper-red trim.");

    // The volcano: lava and steam.
    for (let x = 1; x < 6; x++) for (let z = 8; z < 12; z++) G.box(x, -0.4, z, x + 1, 0.02, z + 1, { top: "area_lava_flow57", all: null });
    for (let i = 0; i < 3; i++) G.box(2 + i * 0.7, 0.4 + i, 9, 3.2 + i * 0.7, 1.4 + i, 10, { all: "area_steam" });
    R.item("Cinnabar's volcano", "Its lava flowing down; John's bone blocks soft steam, about a third see-through.");

    // Violet: a house front, the paving, the bridge's boards, a stone lantern.
    for (let x = 8; x < 14; x++) for (let y = 0; y < 3; y++) B(x, y, 9, y === 1 && (x === 9 || x === 12) ? "area_glass_blue_0" : "l57Grey");
    for (const x of [8, 13]) G.box(x - 0.1, 0, 9.9, x + 0.25, 3, 10.2, { all: "area_vio_timber" });
    G.box(7.6, 1.9, 9.6, 14.4, 2.05, 10.3, { all: "area_band_shelf" });
    for (let i = 0; i < 2; i++) G.box(7.4 + i * 0.7, 3 + i * 0.6, 8.6, 14.6 - i * 0.7, 3.6 + i * 0.6, 10.6, { top: "area_roof_vio_tiles", all: "area_roof_vio_edge", bottom: "area_roof_vio_under" });
    for (let x = 8; x < 14; x++) for (let z = 11; z < 14; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: "area_violet_pave", all: null });
    G.box(15, 0, 10, 16, 0.8, 11, { all: "area_lantern_stone" });
    G.box(15, 0.8, 10, 16, 1.6, 11, { all: "area_lantern_stone_window" });
    G.box(14.8, 1.6, 9.8, 16.2, 1.9, 11.2, { all: "area_lantern_stone_top" });
    R.solid(7.4, 8.6, 16.2, 11.2);
    R.item("Violet City", "Its houses: purple tiles, dark timber either side, the band under the windows wood; its grey setts; HeartGold's stone lanterns by the bridge, lit warm.");

    // A tower's tier: the SPROUT TOWER's and the TIN TOWER's.
    for (const [x0, k] of [[18, "sprout"], [25, "tin"]]) {
      for (let x = x0; x < x0 + 5; x++) for (let y = 0; y < 2; y++) B(x, y, 9, y === 1 && x === x0 + 2 ? "area_" + k + "_screen" : "area_" + k + "_plaster");
      for (const x of [x0, x0 + 4.7]) G.box(x - 0.15, 0, 9.85, x + 0.3, 2.2, 10.3, { all: "area_" + k + "_pillar" });
      G.box(x0 - 0.8, 2.2, 8.4, x0 + 5.8, 2.5, 10.8, { all: "area_" + k + "_timber" });
      G.box(x0 - 0.5, 2.5, 8.6, x0 + 5.5, 2.9, 10.6, { top: "area_" + k + "_roof", all: "area_" + k + "_ridge" });
      R.solid(x0 - 0.8, 8.4, x0 + 5.8, 10.8);
    }
    R.item("SPROUT TOWER and TIN TOWER", "Dark timber, white plaster, paper screens; the SPROUT TOWER's roofs purple, the TIN TOWER's bronze-red with gold ridges, its pillars red lacquer.");

    // Ecruteak: a house in red, the DANCE THEATRE's roof and ridge, the paving and the plots.
    for (let x = 33; x < 38; x++) for (let y = 0; y < 2; y++) B(x, y, 9, "area_ecru_plaster");
    for (const x of [33, 37.7]) G.box(x - 0.1, 0, 9.9, x + 0.25, 2, 10.2, { all: "area_ecru_timber" });
    for (let i = 0; i < 2; i++) G.box(32.4 + i * 0.7, 2 + i * 0.6, 8.6, 38.6 - i * 0.7, 2.6 + i * 0.6, 10.6, { top: "area_roof_ecru_tiles", all: "area_roof_ecru_edge", bottom: "area_roof_ecru_under" });
    for (let x = 33; x < 38; x++) for (let z = 11; z < 13; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: z === 11 ? "area_ecru_plot" : "area_ecru_flag", all: null });
    for (let x = 40; x < 46; x++) for (let y = 0; y < 2; y++) B(x, y, 9, x === 42 || x === 43 ? "area_lacquer_red" : "area_dance_wall");
    for (let i = 0; i < 2; i++) G.box(39.4 + i * 0.7, 2 + i * 0.6, 8.6, 46.6 - i * 0.7, 2.6 + i * 0.6, 10.6, { top: "area_roof_dance_tiles", all: "area_roof_dance_edge", bottom: "area_roof_dance_under" });
    G.box(39.6, 3.2, 9.4, 46.4, 3.45, 9.8, { all: "area_dance_ridge" });
    R.solid(32.4, 8.6, 46.6, 10.6);
    R.item("Ecruteak City", "Its houses as Lavender's in red: red tiles, a white render, dark timber; flagstones and red earth plots; the DANCE THEATRE blue, red and white with a gold ridge.");

    // The NATIONAL PARK's swan in marble and its lamp; the Lake of Rage's GYARADOS scales.
    G.box(48, 0, 9, 50, 1, 11, { all: "area_swan_marble" });
    G.box(48.6, 1, 9.4, 49.2, 2.6, 9.9, { all: "area_swan_marble" });
    G.box(51, 0, 10, 51.3, 2.4, 10.3, { all: "area_lamp_park_post" });
    G.box(50.8, 2.4, 9.8, 51.5, 2.6, 10.5, { all: "area_lamp_park_cap" });
    for (let x = 52; x < 55; x++) for (let y = 0; y < 2; y++) B(x, y, 9, y ? "area_gy_red" : "area_gy_cream");
    R.solid(48, 9, 55, 11);
    R.item("The NATIONAL PARK and the Lake of Rage", "The swan all marble, the park's lamps white with a pale gold cap (no red); the GYARADOS red flowing over its scales, its cream belly.");

    // The coast: the lighthouse's bands, Olivine's pier, a bench, a Cianwood roof with its blue trim; the Day Care, the barn.
    for (let y = 0; y < 4; y++) B(1, y, 18, y % 2 ? "area_lh_teal" : "area_lh_white");
    for (let x = 3; x < 8; x++) G.box(x, -0.2, 17, x + 1, 0.1, 20, { top: "area_pier_ol", all: "area_pier_ol" });
    G.box(9, 0.4, 18, 11, 0.5, 18.5, { all: "area_bench_slats" });
    for (let x = 12; x < 17; x++) for (let y = 0; y < 2; y++) B(x, y, 18, "l57White");
    G.box(11.6, 1.9, 17.6, 17.4, 2.05, 19.3, { all: "area_cian_trim" });
    for (let i = 0; i < 2; i++) G.box(11.4 + i * 0.7, 2.05 + i * 0.6, 17.4, 17.6 - i * 0.7, 2.65 + i * 0.6, 19.4, { top: "area_roof_white_tiles", all: "area_roof_white_edge", bottom: "area_roof_white_under" });
    for (let x = 19; x < 24; x++) for (let y = 0; y < 3; y++) B(x, y, 18, y === 0 ? "area_daycare_foot" : "area_daycare_wall");
    for (let i = 0; i < 2; i++) G.box(18.4 + i * 0.7, 3 + i * 0.6, 17.4, 24.6 - i * 0.7, 3.6 + i * 0.6, 19.4, { top: "area_roof_daycare_tiles", all: "area_roof_daycare_edge", bottom: "area_roof_daycare_under" });
    for (let x = 26; x < 31; x++) for (let y = 0; y < 3; y++) B(x, y, 18, y === 0 ? "area_barn_trim" : "area_barn_red");
    for (let i = 0; i < 2; i++) G.box(25.4 + i * 0.7, 3 + i * 0.6, 17.4, 31.6 - i * 0.7, 3.6 + i * 0.6, 19.4, { top: "area_roof_farm_tiles", all: "area_roof_farm_edge", bottom: "area_roof_farm_under" });
    R.solid(1, 17, 31.6, 20);
    R.item("Olivine, Cianwood, Route 34, the MOOMOO FARM", "The lighthouse in white and teal, the pier's boards, the benches' slats, Cianwood's white roofs with their blue trim, the Day Care's red roof and green foot, the barn red boards on a white trim.");

    // Mr. Berry's house; the Gyms' dark middle column; a plain lamp; a POKé BALL statue.
    for (let x = 33; x < 38; x++) for (let y = 0; y < 2; y++) B(x, y, 18, "area_berry_wall");
    for (let i = 0; i < 2; i++) G.box(32.4 + i * 0.7, 2 + i * 0.6, 17.4, 38.6 - i * 0.7, 2.6 + i * 0.6, 19.4, { top: "area_roof_black_tiles", all: "area_roof_black_edge", bottom: "area_roof_black_under" });
    for (let y = 2; y < 5; y++) B(37, y, 18.2, "area_chimney_dark");
    for (let y = 0; y < 3; y++) B(41, y, 18, "area_gym_divider");
    G.box(44, 0, 18, 44.6, 0.6, 18.6, { all: "area_lamp_chisel" });
    G.box(44.15, 0.6, 18.15, 44.45, 2.5, 18.45, { all: "area_lamp_post" });
    G.box(43.9, 2.5, 17.9, 44.7, 2.7, 18.7, { all: "area_lamp_cap" });
    G.box(47, 0, 18, 48.4, 1.2, 19.4, { all: "area_statue_stone", top: "area_statue_stone_top" });
    G.box(47.2, 1.2, 18.2, 48.2, 1.6, 19.2, { all: "area_statue_ball" });
    G.box(47.15, 1.6, 18.15, 48.25, 1.68, 19.25, { all: "area_statue_band" });
    G.box(47.2, 1.68, 18.2, 48.2, 2.05, 19.2, { all: "area_statue_ball", top: "area_statue_ball_top" });
    R.solid(32.4, 17.4, 48.4, 19.4);
    R.item("And the rest", "Mr. Berry's house (a black roof, a warm render, dark brick chimneys); every Gym's dark middle column at its door; the lamps' caps and feet plain colours; the INDIGO PLATEAU's statues stone POKé BALLs.");

    R.spawn = { x: 28, z: 28, yaw: FACE.n };
    return R;
  };
})();
