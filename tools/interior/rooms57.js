// ---------- 0.53.0's looks on show (John's 11:16): the Plateau, Route 23's statues, Saffron and Celadon, Goldenrod ----------
(function () {
  PLACES.push({ id: "looks53", name: "Plateau, Celadon and Goldenrod looks", floor: "Outside (the mod's blocks)", group: "Outside" });
  BUILD.looks53 = function () {
    // On John's blocks as the mod draws them now: the INDIGO PLATEAU's corner pillar and roof walls, a statue's
    // pedestal with its POKé BALL; one of ROUTE 23's statues (John's blocks); a Saffron shop front (a sill under the
    // window, the new glass doors); Celadon's bench, fountain stone, flat trim, the pond in quartz, the GAME CORNER's
    // sign; Goldenrod's street (brick road, golden tiles, kerb), its lamp, its fence and column.
    const R = new Room("looks53", { name: "Plateau, Celadon and Goldenrod looks", floor: "Outside (the mod's blocks)", W: 34, D: 22, H: 9, outside: true });
    ground(R, "grass");
    const G = R.geo, B = (x, y, z, m, o = {}) => G.box(x, y, z, x + 1, y + 1, z + 1, Object.assign({ all: m }, o));
    // The INDIGO PLATEAU: red brick, the deeper red corner pillar, the cream stone walls along the top.
    for (let y = 0; y < 5; y++) { for (let x = 2; x < 7; x++) B(x, y, 3, "area_plateau_brick"); B(1, y, 3, "area_plateau_brick_dark"); B(7, y, 3, "area_plateau_brick_dark"); }
    for (let x = 1; x < 8; x++) B(x, 5, 3, "area_plateau_stone");
    for (const x of [1, 3, 5, 7]) G.box(x + 0.25, 6, 3.25, x + 0.75, 6.8, 3.75, { all: "area_plateau_stone" });
    R.solid(1, 3, 8, 4);
    R.item("INDIGO PLATEAU", "Its corner pillars (John's resin bricks) a deeper red brick; the stone walls along its roof (his diorite walls) its cream stone.");
    // A golem's pedestal (polished teal metal) and the POKé BALL on its gold stand in place of the golem.
    G.box(10, 0, 4, 11, 1, 5, { all: "area_metal_teal" });
    const px = 10.5, pz = 4.5, s = 1 / 16;
    G.box(px - 4 * s, 1, pz - 4 * s, px + 4 * s, 1 + 1.5 * s, pz + 4 * s, "area_statue_gold");
    for (const [lo, hi, m] of [[2.5, 7.5, "area_trophy_white"], [8.5, 13.5, "area_trophy_red"]]) {
      G.box(px - 4.5 * s, 1 + (lo + 1) * s, pz - 3.5 * s, px + 4.5 * s, 1 + (hi - 1) * s, pz + 3.5 * s, m);
      G.box(px - 3.5 * s, 1 + lo * s, pz - 3.5 * s, px + 3.5 * s, 1 + hi * s, pz + 3.5 * s, m);
    }
    G.box(px - 4.7 * s, 1 + 7.5 * s, pz - 4.7 * s, px + 4.7 * s, 1 + 8.5 * s, pz + 4.7 * s, "area_statue_band");
    G.box(px - 1.5 * s, 1 + 6.6 * s, pz + 4.4 * s, px + 1.5 * s, 1 + 9.4 * s, pz + 5.2 * s, "area_statue_gold");
    R.solid(10, 4, 11, 5);
    R.item("The plateau's statues", "The copper golems not drawn: on each pedestal a POKé BALL (red over white, polished) on a gold stand.");
    // ROUTE 23's statue: John's cauldron, hopper and walls, the anvils and the middle top, in statue stone and gold.
    const sx = 14;
    G.box(sx + 1, 0, 4, sx + 2, 1, 5, { all: "area_statue_stone", top: "area_statue_stone_top" });
    G.box(sx + 1.2, 1, 4.2, sx + 1.8, 2, 4.8, { all: "area_statue_stone", top: "area_statue_stone_top" });
    for (const dx of [0, 2]) G.box(sx + dx + 0.25, 1, 4.25, sx + dx + 0.75, 2, 4.75, "area_statue_stone");
    for (const dx of [0, 2]) G.box(sx + dx, 2, 4.2, sx + dx + 1, 3, 4.8, "area_statue_gold");
    B(sx + 1, 2, 4, "area_statue_crown");
    R.solid(sx, 4, sx + 3, 5);
    R.item("ROUTE 23's statues", "John's own blocks again, as Let's Go's: statue stone, a polished gold trim along the top.");
    // A Saffron shop front: the yellow wall, the cream sill under the window (John's oak stairs), the new glass doors.
    for (let x = 19; x < 25; x++) for (let y = 0; y < 3; y++) if (!(x >= 21 && x < 23 && y < 2)) B(x, y, 3, y === 0 && (x < 21 || x >= 23) ? "area_sill_saf" : "area_saf_wall");
    for (const x of [21, 22]) { G.box(x, 0, 3.4, x + 1, 1, 3.6, { all: "area_door_glass_bottom" }); G.box(x, 1, 3.4, x + 1, 2, 3.6, { all: "area_door_glass_top" }); }
    R.solid(19, 3, 25, 4);
    R.item("Saffron and Celadon's doors", "New glass doors in a pale metal frame (John's dark oak doors); the oak stairs under the windows stone sills.");
    // Celadon: a bench by the fountain, a piece of the fountain, the flat trim, the GAME CORNER's sign, the pond.
    G.box(2, 0, 10, 2.2, 0.55, 11, "area_bench_end_cel"); G.box(4.8, 0, 10, 5, 0.55, 11, "area_bench_end_cel");
    G.box(2.2, 0.35, 10.1, 4.8, 0.5, 10.9, { all: "area_bench_seat_cel" });
    R.solid(2, 10, 5, 11);
    for (let y = 0; y < 3; y++) B(7, y, 10, "area_cel_fountain_stone");
    G.box(6.6, 0, 9.6, 8.4, 0.5, 11.4, { all: "area_cel_fountain_stone" });
    R.solid(6.6, 9.6, 8.4, 11.4);
    for (let x = 10; x < 14; x++) { B(x, 0, 10, "area_cel_green"); G.box(x, 1, 10.6, x + 1, 1.3, 11, { all: "area_cel_trim" }); B(x, 1, 10, "area_cel_green"); }
    R.solid(10, 10, 14, 11);
    G.box(15, 0, 10, 17, 1, 11, { all: "area_gc_symbol" }); G.box(15.5, 1, 10, 16.5, 2, 11, { all: "area_gc_symbol" });
    R.solid(15, 10, 17, 11);
    for (let x = 19; x < 25; x++) for (let z = 9; z < 13; z++) {
      const rim = x === 19 || x === 24 || z === 9 || z === 12;
      G.box(x, rim ? 0 : -0.6, z, x + 1, rim ? 0.3 : -0.5, z + 1, { all: "area_quartz_clean" });
      if (!rim) G.box(x, -0.5, z, x + 1, -0.1, z + 1, { top: "water", all: null });
    }
    R.solid(19, 9, 25, 13);
    R.item("Celadon", "The bench: wooden slats on dark green iron ends. The fountain in pale dressed stone. The trims flat green. The GAME CORNER's sign glossy enamel. The pond quartz all through.");
    // GOLDENROD: the brick road, the golden tiles, the kerb, a lamp, the fence and a column.
    for (let x = 2; x < 16; x++) for (let z = 15; z < 21; z++) G.box(x, -0.4, z, x + 1, 0.01, z + 1, { top: z === 17 ? "area_gold_kerb" : z < 17 ? "area_gold_road" : "area_gold_pave", all: null });
    for (let y = 0; y < 6; y++) G.box(17.25, y, 18.25, 17.75, y + 1, 18.75, { all: "area_lamp_gold_post" });
    G.box(16.6, 6, 17.6, 18.4, 6.4, 19.4, { all: "area_lamp_gold_post", top: "area_lamp_gold_cap_top" });
    G.box(16.9, 5.4, 17.9, 18.1, 6.0, 19.1, { all: "area_lamp_globe_amber" });
    R.solid(17.2, 18.2, 17.8, 18.8);
    for (let x = 20; x < 30; x++) G.box(x, 0.35, 18.4, x + 1, 0.55, 18.6, "area_gold_fence");
    for (let x = 20; x <= 30; x += 2) G.box(x - 0.12, 0, 18.38, x + 0.12, 1.0, 18.62, "area_gold_fence");
    R.solid(20, 18.3, 30, 18.7);
    for (let y = 0; y < 3; y++) G.box(31, y, 18, 32, y + 1, 19, { all: "area_gold_pillar", top: "area_gold_pillar_top" });
    G.box(31.4, 3, 18.4, 31.6, 3.7, 18.6, "area_gold_rod");
    R.solid(31, 18, 32, 19);
    R.item("GOLDENROD CITY", "Its streets red-brown brick, its pavements golden tiles, its kerbs pale stone; its lamps dark bronze with gold caps and amber globes; its fences crisp white with gold caps; its columns fluted white with gold rings.");
    R.spawn = { x: 17, z: 13.5, yaw: FACE.n };
    return R;
  };
})();
