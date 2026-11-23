// ---------- 0.54.0's looks on show (John's 12:19, 12:24, 12:26): the Gyms, the Marts, flowers, Johto, Goldenrod ----------
(function () {
  PLACES.push({ id: "looks54", name: "Gyms, Marts, Johto and Goldenrod looks", floor: "Outside (the mod's blocks)", group: "Outside" });
  BUILD.looks54 = function () {
    // On John's blocks as the mod draws them now: a Kanto Gym (tan, its own band colour) and a Johto one (grey, its
    // own); a Mart's front (its pillars, step, sign); the little flowers; Johto's fence, orange sign, clean snow,
    // Cherrygrove's white path and lamp, a bridge; Saffron's wall and band; Goldenrod's station, RADIO TOWER, alley,
    // barrels, bins, railing and the MAGNET TRAIN's track.
    const R = new Room("looks54", { name: "Gyms, Marts, Johto and Goldenrod looks", floor: "Outside (the mod's blocks)", W: 36, D: 25, H: 10, outside: true });
    ground(R, "grass");
    const G = R.geo, B = (x, y, z, m, o = {}) => G.box(x, y, z, x + 1, y + 1, z + 1, Object.assign({ all: m }, o));
    // Two Gyms: the wall, the band (the second row from the bottom, a colour of its own), the beams, the roof.
    const gym = (x0, grey, band) => {
      const g = grey ? "_grey" : "";
      for (let x = x0; x < x0 + 7; x++) for (let y = 0; y < 5; y++) {
        const beam = x === x0 || x === x0 + 6;
        B(x, y, 3, beam ? "area_gym_beam" + g : y === 1 ? "area_gym_band_" + band : y === 0 ? "area_gym_flat" + g : "area_gym_flat_light" + g, beam && y === 4 ? { top: "area_gym_beam_top" + g } : {});
      }
      const rf = grey ? "roof_gymg" : "roof_gym";
      for (let i = 0; i < 3; i++) G.box(x0 - 0.3 + i * 0.6, 5 + i * 0.5, 2.6 + i * 0.25, x0 + 7.3 - i * 0.6, 5.5 + i * 0.5, 4.4 - i * 0.25, { top: "area_" + rf + "_tiles", all: "area_" + rf + "_edge", bottom: "area_" + rf + "_under" });
      R.solid(x0, 3, x0 + 7, 4);
    };
    gym(1, false, "celadon_city");
    gym(10, true, "blackthorn_city");
    R.item("The Gyms", "Each Gym's band (the second row from the bottom) a colour of its own: Kanto's eight on their tan walls, Johto's eight on grey ones (their roofs and beams grey too).");
    // A Mart's front: the blue wall, the pillars by the door, the step in, the sign above.
    for (let x = 19; x < 27; x++) for (let y = 0; y < 4; y++) if (!(x >= 22 && x < 24 && y < 2)) B(x, y, 3, "area_mart_blue_concrete");
    for (const x of [21, 24]) for (let y = 0; y < 3; y++) G.box(x + 0.1, y, 3.6, x + 0.9, y + 1, 4.4, { all: "area_mart_pillar" });
    G.box(22, 0, 3.4, 24, 2, 3.6, { all: "area_door_glass_bottom" });
    G.box(21.6, 0, 4, 24.4, 0.25, 5, { all: "area_mart_step" });
    G.box(21.5, 2.2, 3.7, 24.5, 3.8, 4, { all: "area_mart_sign_ring" }); G.box(21.8, 2.4, 3.65, 24.2, 3.6, 3.7, { all: "area_mart_sign_face" });
    R.solid(19, 3, 27, 4.4);
    R.item("Every Mart", "Its pillars by the door, the step going in and the sign over the door, the same at every Mart.");
    // Saffron's walls: the cream render and its band (no yellow concrete left).
    for (let x = 29; x < 35; x++) for (let y = 0; y < 4; y++) B(x, y, 3, y === 2 ? "area_saf_band" : "area_saf_wall");
    R.solid(29, 3, 35, 4);
    // The little flowers, across the map.
    const flowers = ["flower_wild", "flower_dandelion", "flower_daisy", "flower_bluet", "flower_poppy", "flower_tulip"];
    flowers.forEach((f, i) => {
      for (let k = 0; k < 2; k++) {
        const x = 2 + i * 1.3 + k * 0.5, z = 9 + k * 0.7;
        G.box(x - 0.3, 0, z, x + 0.3, 0.6, z + 0.001, { s: "area_" + f, n: "area_" + f, all: null });
        G.box(x, 0, z - 0.3, x + 0.001, 0.6, z + 0.3, { e: "area_" + f, w: "area_" + f, all: null });
      }
    });
    R.item("Flowers", "The little yellow and white flowers (and the rest), drawn anew, across the map.");
    // Johto: the fence, a sign with its orange face (one sign, slim), clean snow, Cherrygrove's white path and its lamp.
    for (let x = 11; x < 18; x++) G.box(x, 0.5, 9.45, x + 1, 0.7, 9.55, "area_fence_johto");
    for (let x = 11; x <= 18; x++) G.box(x - 0.1, 0, 9.4, x + 0.1, 1.0, 9.6, "area_fence_johto");
    R.solid(11, 9.3, 18, 9.7);
    G.box(19.4, 0, 9.4, 19.6, 0.8, 9.6, "area_fence_johto");
    ["l", "m", "r"].forEach((p, i) => G.box(18.5 + i * 0.6, 0.8, 9.45, 19.1 + i * 0.6, 1.4, 9.55, { s: "area_sign_front_" + p + "_o", n: "area_sign_front_" + p + "_o", all: "area_fence_johto" }));
    for (let x = 22; x < 26; x++) for (let z = 8; z < 11; z++) B(x, -0.6, z, "area_snow_clean");
    for (let x = 27; x < 34; x++) for (let z = 8; z < 12; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: (x + z) % 2 ? "area_pave_cg_a" : "area_pave_cg_b", all: null });
    for (let y = 0; y < 4; y++) G.box(30.3, y, 9.3, 30.7, y + 1, 9.7, { all: "area_lamp_cg_post" });
    G.box(29.9, 4, 8.9, 31.1, 4.5, 10.1, { all: "area_lamp_cg_cap" });
    R.solid(30.3, 9.3, 30.7, 9.7);
    R.item("Johto", "Its fences; its signs single and slim with their orange face; the Ice Path's snow clean; Cherrygrove's white path and its lamps; its bridges, cliffs and trees as Kanto's.");
    // A bridge (Routes 26, 27 and 45): boards, beams, bolts.
    for (let x = 2; x < 10; x++) G.box(x, 0.4, 13, x + 1, 0.6, 15, { top: "area_bridge_boards", all: "area_bridge_beam" });
    for (const z of [13, 14.8]) G.box(2, 0.6, z, 10, 1.4, z + 0.2, { all: "area_bridge_beam" });
    R.solid(2, 13, 10, 15);
    // GOLDENROD: the MAGNET TRAIN station's front, the RADIO TOWER's side, the alley's setts, a barrel, a bin, the
    // dark railing, the train's track, the lawn.
    for (let x = 1; x < 9; x++) for (let y = 0; y < 4; y++) B(x, y, 19, y === 3 ? "area_station_teal" : x % 3 === 0 ? "area_station_mullion" : y === 1 ? "area_station_light" : "area_station_white");
    R.solid(1, 19, 9, 20);
    for (let y = 0; y < 8; y++) for (let x = 10; x < 13; x++) B(x, y, 19, y % 3 === 2 ? "area_radio_white" : x === 11 ? "area_radio_panel" : "area_radio_grey");
    R.solid(10, 19, 13, 20);
    for (let x = 14; x < 20; x++) for (let z = 17; z < 22; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: "area_gold_alley", all: null });
    G.box(15, 0, 18, 15.9, 1, 18.9, { all: "area_crate_barrel_side", top: "area_crate_barrel_top" }); R.solid(15, 18, 15.9, 18.9);
    G.box(17, 0, 18, 17.8, 0.9, 18.8, { all: "area_bin_side", top: "area_bin_top" }); R.solid(17, 18, 17.8, 18.8);
    for (let x = 21; x < 28; x++) G.box(x, 0.8, 17.45, x + 1, 0.95, 17.55, "area_railing_dark");
    for (let x = 21; x <= 28; x++) G.box(x - 0.06, 0, 17.44, x + 0.06, 1.0, 17.56, "area_railing_dark");
    R.solid(21, 17.3, 28, 17.7);
    for (let x = 21; x < 34; x++) { G.box(x, -0.4, 20, x + 1, 0.05, 23, { top: "area_track_bed", all: null }); for (const z of [20.9, 21.9]) G.box(x, 0.05, z, x + 1, 0.2, z + 0.15, "area_track_rail"); }
    for (let x = 29; x < 35; x++) for (let z = 14; z < 18; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: "area_gold_lawn", all: null });
    R.item("GOLDENROD CITY", "The MAGNET TRAIN station its own (white and teal, lit), the RADIO TOWER its panels, the alley's setts, barrels and bins, the dark railings, the train's track.");
    R.spawn = { x: 17.5, z: 7.0, yaw: FACE.n };
    return R;
  };
})();
