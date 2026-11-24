// ---------- 0.48.0 (17:42 to 17:53): Seafoam Islands and Rock Tunnel (tools/caves48.py), and the round's new rooms ----------
(function () {
  const PAL = {
    seafoam: { floor: "area_sf_floor", sand: "area_sf_sand", step: "area_sf_step", wall: "area_sf_snow", wallTop: "area_sf_snow_top", ice: "area_sf_ice", iceTop: "area_sf_ice_top",
      boulder: "area_ice_shard", pre: "iboulder", haze: 0x9ec8ea, night: false },
    rock_tunnel: { floor: "area_rt_floor", sand: "area_rt_floor", step: "area_rt_floor", wall: "area_rt_rock", wallTop: "area_rt_rock_top", ice: "area_rt_rock", iceTop: "area_rt_rock_top",
      boulder: "area_rt_boulder", pre: "rboulder", haze: 0x463a30, night: true },
    // (0.60) Cerulean Cave and Victory Road (John's 16:36; tools/looks60.py's textures).
    cerulean_cave: { floor: "area_cc_floor", sand: "area_cc_floor", step: "area_cc_step", wall: "area_cc_cliff", wallTop: "area_cc_moss_top", ice: "area_cc_cliff", iceTop: "area_cc_moss_top",
      boulder: "area_cc_boulder", pre: "cboulder", haze: 0x76909a, night: false },
    victory_road: { floor: "area_vr_floor", sand: "area_vr_sand", step: "area_vr_step", wall: "area_vr_cliff", wallTop: "area_vr_top", ice: "area_vr_cliff", iceTop: "area_vr_top",
      boulder: "area_vr_boulder", pre: "vboulder", haze: 0xb08c68, night: false },
  };
  const ABOUT = {
    seafoam: "John's floors stitched as FireRed's: ladders and holes down to B4F and ARTICUNO; its rock ice, its boulders ice shards; the deep water you can swim (SURF comes later).",
    rock_tunnel: "John's two floors stitched as FireRed's: in by the north mouth at 1F's ladder, four ladders between 1F and B1F, out at 1F's south doorway. Dark; craggy rocks; FireRed's trainers.",
    cerulean_cave: "John's three floors stitched as FireRed's: in from the river's north bank onto 1F, its water swum (SURF comes later), ladders up to 2F's maze of boulders and down to B1F and MEWTWO.",
    victory_road: "John's three floors stitched as FireRed's: in by Route 23's west mouth onto 1F, up to 2F and 3F and back down to 2F's way out by the east mouth. FireRed's trainers; MOLTRES on 2F, where Red and Blue had it; the rock barriers cleared.",
  };
  function caveRoom48(id) {
    const C = CAVES48[id], K = PAL[C.cave];
    const R = new Room(id, { name: C.name, floor: C.floor, W: C.W, D: C.D, H: 9, outside: true, night: K.night, haze: K.haze });
    const G = R.geo, P = 1 / 16;
    const at = (x, z) => { const r = C.rows[z]; return { k: r[2 * x], h: r.charCodeAt(2 * x + 1) - 56 }; };
    const cut = southCut(C.W, C.D, (x, z) => at(x, z).k !== "w" && at(x, z).k !== "i", (x, z) => { const c = at(x, z); return c.k === "q" ? 0 : Math.max(0, c.h / 2); });
    for (let z = 0; z < C.D; z++) {
      for (let x = 0; x < C.W;) {
        const c = at(x, z); let x1 = x + 1;
        while (x1 < C.W && at(x1, z).k === c.k && at(x1, z).h === c.h) x1++;
        const h = c.h / 2;
        if (c.k === "w" || c.k === "i") {
          const top = Math.min(h, 6);
          cutRock(G, x, x1, z, -1, top, cut, { top: c.k === "i" ? K.iceTop : K.wallTop, all: c.k === "i" ? K.ice : K.wall, bottom: null });
          R.solid(x, z, x1, z + 1);
        } else if (c.k === "G" || c.k === "g") {
          G.box(x, -1, z, x1, h, z + 1, { top: K.floor, all: "soil" });
          for (let bx = x; bx < x1; bx++) {
            const m = BOULDERS[(c.k === "g" ? K.pre + "_small" : ((bx * 31 + z * 17 + 3 * 7) % 2 + 2) % 2 === 0 ? K.pre + "_a" : K.pre + "_b")] || BOULDERS[c.k === "g" ? "boulder_small" : "boulder_a"];
            for (const q of m) G.box(bx + q[0] * P, h + q[1] * P, z + q[2] * P, bx + q[3] * P, h + q[4] * P, z + q[5] * P, K.boulder);
          }
          R.solid(x, z, x1, z + 1);
        } else if (c.k === "K" || c.k === "L") {
          G.box(x, h - 0.2, z, x1, h, z + 1, { top: "darkTop", all: null });
          if (c.k === "L") G.box(x, h, z + 0.02, x1, 0.4, z + 0.08, "walnut");
        } else if (c.k === "q") G.box(x, -1.2, z, x1, -0.15, z + 1, { top: "water", all: "soil" });
        else {
          const mat = c.k === "s" ? K.sand : c.k === "^" ? K.step : c.k === "S" ? "area_cave_mat" : K.floor;
          G.box(x, -1, z, x1, h, z + 1, { top: mat, all: h > 0.6 ? K.ice : "soil" });
          if (c.k === "L") G.box(x + 0.1, h, z + 0.02, x1 - 0.1, h + 2.4, z + 0.1, "walnut");
        }
        x = x1;
      }
    }
    for (const w of C.warps) {
      if (w.kind === "mat") R.warps.push({ test: (p, input) => input.down && p.z > w.z && p.x > w.x0 && p.x < w.x1, to: "town", fx: "fadeOut" });
      if (w.kind === "down") R.warps.push({ test: p => p.x > w.x0 && p.x < w.x1 && p.z > w.z0 && p.z < w.z1, to: w.to, spawn: { x: w.spawn.x, z: w.spawn.z, yaw: FACE.s }, fx: "fade" });
      if (w.kind === "up") R.warps.push({ test: p => p.x > w.x0 - 0.4 && p.x < w.x1 + 0.4 && p.z > w.z0 - 0.5 && p.z < w.z1 + 0.6, to: w.to || "town", spawn: w.spawn ? { x: w.spawn.x, z: w.spawn.z, yaw: FACE.s } : null, fx: "fade" });
    }
    for (const it of C.items) {
      const y = (it.y || 0);
      G.box(it.x - 0.2, y, it.z - 0.2, it.x + 0.2, y + 0.4, it.z + 0.2, { all: "ball", top: "ballTop" });
      R.act([it.x - 0.6, it.z - 0.6, it.x + 0.6, it.z + 0.6], { label: "Look", lines: [it.item.replace(/_/g, " ").toUpperCase()] });
    }
    // (The POKéMON standing here, ARTICUNO, MEWTWO, MOLTRES: the game has the creature; the viewer marks its spot.)
    for (const m of C.mons) {
      const y = m.y || 0;
      G.box(m.x - 0.5, y, m.z - 0.5, m.x + 0.5, y + 0.15, m.z + 0.5, { all: K.iceTop });
      G.box(m.x - 0.25, y + 0.15, m.z - 0.25, m.x + 0.25, y + 1.6, m.z + 0.25, { all: K.boulder });
      R.solid(m.x - 0.5, m.z - 0.5, m.x + 0.5, m.z + 0.5);
      R.act([m.x - 1, m.z - 1, m.x + 1, m.z + 1.2], { label: "Look", lines: [m.species.toUpperCase() + " is here.", "(In the game: talk to it and it attacks. Catch it if you can!)"] });
    }
    for (const a of C.acts) R.act(a.rect, { label: a.label, lines: a.lines });
    for (const p of C.people) R.npcs.push({ name: p.name.split(" ").map(w => w[0] + w.slice(1).toLowerCase()).join(" "), key: p.key, x: p.x, z: p.z, y: p.y || 0,
      yaw: (180 - p.yaw) * Math.PI / 180, look: { skin: p.skin }, role: p.role || null, talk: () => [{ text: "(A trainer: walk into their sight and they'll battle you.)" }] });
    R.spawn = { x: C.spawn.x, z: C.spawn.z, yaw: C.spawn.yaw === 180 ? FACE.n : FACE.s };
    R.item(C.name, ABOUT[C.cave]);
    return R;
  }
  for (const id of Object.keys(CAVES48)) {
    const C = CAVES48[id];
    PLACES.push({ id, name: C.name, floor: C.floor, group: C.name });
    BUILD[id] = () => caveRoom48(id);
  }
})();

// ---------- Vermilion City (17:51): FireRed's insides, HeartGold's colours ----------
// The FISHING GURU's house (the OLD ROD), the POKéMON FAN CLUB (its CHAIRMAN and the BIKE VOUCHER), the trade house
// (ELYSSA: a SPEAROW for her FARFETCH'D, CH'DING), the PIDGEY letter house, and LT. SURGE's GYM: fifteen trash cans,
// two switches under them (the second next to the first), and the electric door to him.
TEXDEF.vermWall = [16, 16, (P, r) => { P.fill(hx("#F1E4CC"), 0.025, r); for (let x = 0; x < 16; x += 8) P.rect(x, 0, 1, 16, hx("#E2D0B0")); }];
MATDEF.vermWall = { tex: "vermWall" };
TEXDEF.vermBoards = [16, 16, (P, r) => { P.fill(hx("#C98F5A"), 0.05, r); for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx("#DDA672")); P.rect(0, y + 3, 16, 1, hx("#9C6A3E")); } }];
MATDEF.vermBoards = { tex: "vermBoards" };
TEXDEF.clubCarpet = [16, 16, (P, r) => { P.fill(hx("#B4384A"), 0.03, r); P.rect(0, 0, 16, 1, hx("#D8B04A")); P.rect(0, 15, 16, 1, hx("#D8B04A")); P.rect(0, 0, 1, 16, hx("#D8B04A")); P.rect(15, 0, 1, 16, hx("#D8B04A"));
  for (let i = 3; i < 13; i += 4) for (let j = 3; j < 13; j += 4) P.rect(i, j, 2, 2, hx("#C8566A")); }];
MATDEF.clubCarpet = { tex: "clubCarpet" };
TEXDEF.clubWall = [16, 16, (P, r) => { P.fill(hx("#E9D9B4"), 0.02, r); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#D9C394")); }];
MATDEF.clubWall = { tex: "clubWall" };
TEXDEF.monPortrait = [16, 16, (P, r) => { P.fill(hx("#8E6A2E"), 0, r); P.rect(1, 1, 14, 14, hx("#BFE0F2")); P.rect(1, 10, 14, 5, hx("#7DBF5A"));
  P.rect(5, 5, 6, 6, hx("#F2A6B8")); P.rect(6, 3, 1, 2, hx("#F2A6B8")); P.rect(9, 3, 1, 2, hx("#F2A6B8")); P.rect(6, 7, 1, 1, hx("#2A2A2A")); P.rect(9, 7, 1, 1, hx("#2A2A2A")); }];
MATDEF.monPortrait = { tex: "monPortrait" };
// LT. SURGE's GYM: dark blue steel plates with yellow hazard lines, grey steel walls, the cans in brushed metal.
TEXDEF.surgeFloor = [16, 16, (P, r) => { P.fill(hx("#3C4C6A"), 0.04, r); P.rect(0, 0, 16, 1, hx("#56698C")); P.rect(0, 0, 1, 16, hx("#56698C")); P.rect(15, 0, 1, 16, hx("#28344A")); P.rect(0, 15, 16, 1, hx("#28344A"));
  for (const [x, y] of [[2, 2], [13, 2], [2, 13], [13, 13]]) P.rect(x, y, 1, 1, hx("#8A9CC0")); }];
MATDEF.surgeFloor = { tex: "surgeFloor" };
TEXDEF.surgeHazard = [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, ((x + y) >> 2) % 2 ? hx("#2A2A2E") : hx("#F2C230")); }];
MATDEF.surgeHazard = { tex: "surgeHazard" };
TEXDEF.surgeWall = [16, 16, (P, r) => { P.fill(hx("#8F99A6"), 0.03, r); P.rect(0, 7, 16, 1, hx("#6C7582")); P.rect(0, 8, 16, 1, hx("#B4BDC8")); for (const x of [3, 12]) { P.rect(x, 2, 1, 1, hx("#D5DCE4")); P.rect(x, 12, 1, 1, hx("#D5DCE4")); } }];
MATDEF.surgeWall = { tex: "surgeWall" };
TEXDEF.trashCan = [16, 16, (P, r) => { P.fill(hx("#A7B1B8"), 0.03, r); for (let x = 1; x < 16; x += 3) P.rect(x, 0, 1, 16, hx("#7E878E")); P.rect(0, 0, 16, 2, hx("#C9D1D6")); }];
MATDEF.trashCan = { tex: "trashCan" };
TEXDEF.trashLid = [16, 16, (P, r) => { P.fill(hx("#8C969D"), 0.03, r); P.rect(6, 6, 4, 4, hx("#C9D1D6")); P.rect(0, 0, 16, 1, hx("#B4BDC4")); P.rect(0, 15, 16, 1, hx("#636B71")); }];
MATDEF.trashLid = { tex: "trashLid" };
// (The electric door is real blocks in the game, put there or taken away for you: in the viewer, its bolts.)
TEXDEF.surgeBarrierView = [16, 16, (P, r) => { P.fill(hx("#223A5A"), 0, r); const zz = [3, 6, 4, 8, 6, 10, 8, 12, 10, 13];
  for (let i = 0; i < 10; i++) { P.rect(zz[i] - 1, i + 3, 3, 1, hx("#FFE36A")); } for (let y = 0; y < 16; y += 5) P.rect(0, y, 16, 1, hx("#6FD8FF")); }];
MATDEF.surgeBarrierView = { tex: "surgeBarrierView", emissive: true };

(() => {
  const place = (id, name, floor) => PLACES.push({ id, name, floor, group: "Vermilion City" });
  place("vermilion_gym", "Vermilion Gym", "Vermilion City");
  place("pokemon_fan_club", "Pokémon Fan Club", "Vermilion City");
  place("vermilion_old_rod_house", "Vermilion house", "The Fishing Guru");
  place("vermilion_trade_house", "Vermilion house", "The trade house");
  place("vermilion_pidgey_house", "Vermilion house", "The Pidgey letter");
  const WINDOW = ["Outside: VERMILION CITY's harbor, gulls over the water."];

  Object.assign(BUILD, {
    vermilion_old_rod_house() {
      const R = new Room("vermilion_old_rod_house", { name: "Vermilion house", floor: "The Fishing Guru", W: 12, D: 11 });
      plainHouse(R, { paper: "vermWall", rug: "rugBlue", window: WINDOW, cupboard: ["A cupboard. Inside: fishing line, floats and a box of lures."] });
      const G = R.geo;
      // His rods on the wall, a fish in a case.
      for (const x of [1.0, 1.5, 2.0]) G.box(x, 0.2, 0.12, x + 0.06, 2.4, 0.2, "walnut");
      R.act([0.6, 0, 2.6, 1.0], { label: "Look", lines: ["Fishing rods, from his first to his best."] });
      G.box(9.4, 1.4, 0.1, 11.4, 2.2, 0.3, { all: "walnut", s: "tankGlass" });
      R.act([9.2, 0, 11.6, 1.0], { label: "Look", lines: ["A MAGIKARP in a case: \"My first catch!\""] });
      R.npcs.push({ name: "Fishing Guru", key: "vermilion_old_rod_house_p0", x: 2.6, z: 4.4, yaw: FACE.e, look: { skin: "fishing_guru" },
        talk: () => [{ text: "I'm the FISHING GURU! I simply Looove fishing! Do you like to fish?" }, { text: "(In the game: YES, and he gives you the OLD ROD.)" }] });
      return R;
    },
    vermilion_trade_house() {
      const R = new Room("vermilion_trade_house", { name: "Vermilion house", floor: "The trade house", W: 12, D: 11 });
      plainHouse(R, { paper: "vermWall", rug: "rugSalmon", window: WINDOW, cupboard: ["A cupboard. Inside: a bundle of leeks, for FARFETCH'D."] });
      R.npcs.push({ name: "Elyssa", key: "vermilion_trader", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "cooltrainer_f" },
        talk: () => [{ text: "Hi! Do you have a SPEAROW? Want to trade it for my FARFETCH'D?" }, { text: "(In the game: trade a SPEAROW and her FARFETCH'D comes named CH'DING.)" }] });
      return R;
    },
    vermilion_pidgey_house() {
      const R = new Room("vermilion_pidgey_house", { name: "Vermilion house", floor: "The Pidgey letter", W: 12, D: 11 });
      plainHouse(R, { paper: "vermWall", rug: "rugGreen", window: WINDOW, cupboard: ["A cupboard. Inside: writing paper and envelopes."] });
      const G = R.geo;
      G.box(5.6, 0.86, 4.0, 6.2, 0.88, 4.5, { top: "paperSchool", all: null });
      R.act([5.0, 3.6, 6.8, 4.9], { label: "Read", lines: ["Dear PIPPI, I hope to see you soon.", "I heard SAFFRON has problems with TEAM ROCKET.", "VERMILION appears to be safe."] });
      R.npcs.push({ name: "Boy", key: "vermilion_pidgey_house_p0", x: 9.4, z: 3.6, yaw: FACE.w, look: { skin: "youngster" },
        talk: () => [{ text: "I'm getting my PIDGEY to fly a letter to SAFFRON in the north!" }] });
      R.pets = [{ sid: "pidgey", x: 8.2, z: 5.6, yaw: 270 }];
      R.act([7.2, 4.6, 9.2, 6.6], { label: "Look", lines: ["PIDGEY: Kurukkoo!"] });
      return R;
    },
    pokemon_fan_club() {
      // The POKéMON FAN CLUB: a warm club room, a red carpet with a gold border, portraits of the members' POKéMON,
      // armchairs, the CHAIRMAN at the top and his receptionist, CLEFAIRY and SEEL with their trainers.
      const R = new Room("pokemon_fan_club", { name: "Pokémon Fan Club", floor: "Vermilion City", W: 14, D: 12 });
      shell(R, { paper: "clubWall", floor: "vermBoards", door: [6, 8], entry: true, skirt: "walnut", ledge: "walnut" });
      band(R, 1.0, "walnut", 0.12);
      const G = R.geo;
      G.box(2.0, 0, 2.6, 12.0, 0.03, 9.4, { top: "clubCarpet", all: "cap", bottom: null });
      for (const x of [1.4, 4.4, 9.0, 12.0]) { G.box(x, 1.5, 0.1, x + 0.9, 2.4, 0.16, "monPortrait"); }
      R.act([1.0, 0, 13.0, 0.9], { label: "Look", lines: ["Portraits of the club's POKéMON: CLEFAIRY, SEEL, PIKACHU, a RAPIDASH..."] });
      for (const [x, z, f] of [[3.0, 4.6, "e"], [3.0, 6.8, "e"], [11.0, 4.6, "w"], [11.0, 6.8, "w"]]) armchair(R, x, z, f, "redFabric");
      table(R, 6.0, 5.0, 8.0, 6.6, { top: "walnut", leg: "walnut", h: 0.6, name: "Table", note: "A low walnut table with a club newsletter on it." });
      plant(R, 0.8, 10.6, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      plant(R, 13.2, 10.6, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Chairman", key: "pokemon_fan_club_p4", x: 7.0, z: 1.8, yaw: FACE.s, look: { skin: "gentleman" },
        talk: () => [{ text: "I chair the POKéMON FAN CLUB! ... Did you come visit to hear about my POKéMON?" }, { text: "(In the game: YES, hear him out, and he gives you the BIKE VOUCHER.)" }] });
      R.npcs.push({ name: "Receptionist", key: "pokemon_fan_club_p5", x: 10.4, z: 1.8, yaw: FACE.s, look: { skin: "link_receptionist" },
        talk: () => [{ text: "Our CHAIRMAN is very vocal about POKéMON." }] });
      R.npcs.push({ name: "Fan", key: "pokemon_fan_club_p0", x: 11.6, z: 8.6, yaw: FACE.w, look: { skin: "fisher" },
        talk: () => [{ text: "Won't you admire my CLEFAIRY's adorable tail?" }] });
      R.npcs.push({ name: "Girl", key: "pokemon_fan_club_p1", x: 2.4, z: 8.6, yaw: FACE.e, look: { skin: "girl" },
        talk: () => [{ text: "I just love my SEEL! It squeals when I hug it!" }] });
      R.pets = [{ sid: "clefairy", x: 10.4, z: 8.6, yaw: 90 }, { sid: "seel", x: 3.6, z: 8.6, yaw: 270 }];
      doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },
    vermilion_gym() {
      // LT. SURGE's GYM (FireRed's): his stage at the top behind a steel wall, its electric door shut till you've
      // found both switches; fifteen trash cans in three rows, the trainers about them.
      const W = 14, D = 22;
      const R = new Room("vermilion_gym", { name: "Vermilion Gym", floor: "Vermilion City", W, D, H: 5 });
      shell(R, { paper: "surgeWall", floor: "surgeFloor", door: [6, 8], entry: true, skirt: "surgeHazard", ledge: "steel" });
      band(R, 0.3, "surgeHazard", 0.2); band(R, 1.4, "steel", 0.08);
      const G = R.geo;
      // The wall across, its electric door in the middle (x 6 to 8).
      for (const [x0, x1] of [[0, 6], [8, W]]) { G.box(x0, 0, 6.0, x1, 2.4, 6.8, { all: "surgeWall", top: "steel" }); R.solid(x0, 6.0, x1, 6.8); }
      G.box(5.7, 0, 5.9, 6.0, 2.6, 6.9, "surgeHazard"); G.box(8.0, 0, 5.9, 8.3, 2.6, 6.9, "surgeHazard");
      G.box(6.0, 0, 6.3, 8.0, 2.0, 6.5, { all: "surgeBarrierView", top: null, bottom: null });
      R.act([5.6, 6.6, 8.4, 7.6], { label: "Look", lines: ["An electric door. It's locked: two switches open it."] });
      // His stage.
      G.box(5.0, 0, 1.4, 9.0, 0.12, 3.6, { top: "surgeHazard", all: "steel", bottom: null });
      // Generators along the top wall.
      for (const x of [0.6, 11.4]) { G.box(x, 0, 0.3, x + 2.0, 2.2, 1.6, { all: "steel", s: "surgeHazard" }); R.solid(x, 0.3, x + 2.0, 1.6); }
      R.act([0.4, 0.2, 2.8, 2.0], { label: "Look", lines: ["A generator, humming."] }); R.act([11.2, 0.2, 13.6, 2.0], { label: "Look", lines: ["A generator, humming."] });
      // The cans: 5 across, 3 rows (in the game, each one's looked in; the switches are under two of them).
      let i = 0;
      for (const z of [9.0, 12.0, 15.0]) for (const x of [1.6, 4.3, 7.0, 9.7, 12.4]) {
        G.box(x - 0.42, 0, z - 0.42, x + 0.42, 0.95, z + 0.42, { all: "trashCan", top: "trashLid" });
        G.box(x - 0.46, 0.95, z - 0.46, x + 0.46, 1.05, z + 0.46, { all: "trashLid" });
        R.solid(x - 0.45, z - 0.45, x + 0.45, z + 0.45);
        R.act([x - 0.8, z - 0.8, x + 0.8, z + 0.8], { label: "Look", use: "can:" + i, lines: ["Nope! There's only trash here.", "(In the game: two of the cans hide LT.SURGE's switches, the second right next to the first.)"] });
        i++;
      }
      gymStatue(R, 3.6, 19.4, ["VERMILION CITY POKéMON GYM", "LEADER: LT.SURGE", "WINNING TRAINERS: {rival}"]);
      gymStatue(R, 10.4, 19.4, ["VERMILION CITY POKéMON GYM", "LEADER: LT.SURGE", "WINNING TRAINERS: {rival}"]);
      R.npcs.push({ name: "Leader Lt. Surge", key: "vermilion_gym_0", role: "leader", x: 7.0, z: 2.4, y: 0.12, yaw: FACE.s, look: { skin: "leader_lt_surge" },
        talk: () => [{ text: "Hey, kid! What do you think you're doing here?" }] });
      R.npcs.push({ name: "Gentleman Tucker", key: "vermilion_gym_1", role: "trainer", x: 12.6, z: 10.5, yaw: FACE.w, look: { skin: "gentleman" },
        talk: () => [{ text: "I'm a lightweight, but I'm good with electricity!" }] });
      R.npcs.push({ name: "Engineer Baily", key: "vermilion_gym_2", role: "trainer", x: 1.4, z: 13.5, yaw: FACE.e, look: { skin: "super_nerd" },
        talk: () => [{ text: "This is no place for kids!" }] });
      R.npcs.push({ name: "Sailor Dwayne", key: "vermilion_gym_3", role: "trainer", x: 7.0, z: 8.0, yaw: FACE.s, look: { skin: "sailor" },
        talk: () => [{ text: "This is no place for kids!" }] });
      R.npcs.push({ name: "Gym Guide", key: "vermilion_gym_p0", x: 9.6, z: 19.6, yaw: FACE.s, look: { skin: "gym_guide" },
        talk: () => [{ text: "Yo! Champ in making! LT.SURGE has a nickname. People refer to him as the Lightning American!" }] });
      doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 7.0, z: D - 1.5, yaw: FACE.n };
      R.item("Electric door", "In the game it's real blocks, put there or taken away for you: look in the cans for the two switches and it opens.");
      return R;
    },
  });
})();

// ---------- Lavender Town (19:06): FireRed's three houses, in the town's own colours (lilac walls, dark boards) ----------
TEXDEF.paperLavender = [16, 16, (P, r) => wallpaper(P, r, hx("#E4DAEE"), hx("#CDBFDD"), hx("#9C84C2"))];
MATDEF.paperLavender = { tex: "paperLavender" };
TEXDEF.lavBoards = [16, 16, (P, r) => { P.fill(hx("#6E5266"), 0.05, r); for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx("#86687C")); P.rect(0, y + 3, 16, 1, hx("#4E3A4A")); } }];
MATDEF.lavBoards = { tex: "lavBoards" };
TEXDEF.lavRug = [16, 16, (P, r) => { P.fill(hx("#6A4E8E"), 0.03, r); P.rect(0, 0, 16, 1, hx("#C9B6E6")); P.rect(0, 15, 16, 1, hx("#C9B6E6")); P.rect(0, 0, 1, 16, hx("#C9B6E6")); P.rect(15, 0, 1, 16, hx("#C9B6E6"));
  for (let i = 4; i < 13; i += 4) P.rect(i, i, 1, 1, hx("#E2D4F5")); }];
MATDEF.lavRug = { tex: "lavRug" };
TEXDEF.petBed = [16, 16, (P, r) => { P.fill(hx("#C9B6E6"), 0.03, r); P.rect(0, 0, 16, 2, hx("#E8DDF5")); P.rect(0, 14, 16, 2, hx("#9C84C2")); }];
MATDEF.petBed = { tex: "petBed" };

(() => {
  const place = (id, name, floor) => PLACES.push({ id, name, floor, group: "Lavender Town" });
  place("lavender_volunteer_house", "Volunteer Pokémon House", "Lavender Town");
  place("lavender_cubone_house", "Lavender house", "Cubone's house");
  place("lavender_name_rater", "Name Rater's house", "Lavender Town");
  const WINDOW = ["Outside: LAVENDER TOWN at night, the POKéMON TOWER's lights through the mist."];

  Object.assign(BUILD, {
    lavender_volunteer_house() {
      // MR. FUJI's VOLUNTEER POKéMON HOUSE: soft beds for the POKéMON he looks after, his books, the children
      // waiting for him (he's at the POKéMON TOWER).
      const R = new Room("lavender_volunteer_house", { name: "Volunteer Pokémon House", floor: "Lavender Town", W: 14, D: 11 });
      shell(R, { paper: "paperLavender", floor: "lavBoards", door: [6, 8], entry: true, skirt: "beam", ledge: "walnut" });
      band(R, 1.0, "beam", 0.12);
      const G = R.geo;
      bookshelf(R, 0.6, 0, "s", 2, 3, { lines: ["MR. FUJI's books: caring for POKéMON, from eggs to old age."] });
      bookshelf(R, 2.8, 0, "s", 2, 3, { lines: ["Letters from people whose POKéMON he took in."] });
      G.box(3.0, 0, 3.0, 11.0, 0.03, 7.4, { top: "lavRug", all: "cap", bottom: null });
      for (const [x, z] of [[9.0, 2.0], [11.6, 2.0], [11.6, 4.6]]) { G.box(x - 0.8, 0, z - 0.6, x + 0.8, 0.28, z + 0.6, { all: "petBed", top: "petBed" }); }
      R.act([8.0, 1.2, 12.6, 5.4], { label: "Look", lines: ["Soft beds for the POKéMON MR. FUJI looks after."] });
      table(R, 4.4, 4.2, 6.6, 5.8, { top: "walnut", leg: "walnut", h: 0.86, name: "Table", note: "A walnut table with a bowl of POKéMON food." });
      plant(R, 0.8, 9.4, 0.9, { pot: "potWhite", lines: ["A lavender plant in a white pot."] });
      crossWindow(R, "e", 6.6, 1.6, 1.8, 1.2, WINDOW);
      R.npcs.push({ name: "Boy", key: "mr_fujis_house_p0", x: 3.6, z: 6.6, yaw: FACE.e, look: { skin: "super_nerd" },
        talk: () => [{ text: "That's odd, MR.FUJI isn't here. Where'd he go?" }] });
      R.npcs.push({ name: "Little girl", key: "mr_fujis_house_p1", x: 8.0, z: 6.2, yaw: FACE.s, look: { skin: "little_girl" },
        talk: () => [{ text: "This is really MR.FUJI's house. He's really kind! He looks after abandoned and orphaned POKéMON!" }] });
      R.pets = [{ sid: "psyduck", x: 9.0, z: 2.0, yaw: 180 }, { sid: "nidorino", x: 11.6, z: 4.6, yaw: 90 }];
      doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },
    lavender_cubone_house() {
      const R = new Room("lavender_cubone_house", { name: "Lavender house", floor: "Cubone's house", W: 12, D: 11 });
      plainHouse(R, { paper: "paperLavender", floor: "lavBoards", rug: "lavRug", window: WINDOW, cupboard: ["A cupboard. Inside: candles and a framed photo of a MAROWAK."] });
      R.npcs.push({ name: "Girl", key: "lavender_cubone_house_p1", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "brunette_girl" },
        talk: () => [{ text: "I hate those horrible ROCKETs! That poor CUBONE's mother... It was killed trying to escape from TEAM ROCKET!" }] });
      R.pets = [{ sid: "cubone", x: 8.0, z: 5.6, yaw: 270 }];
      R.act([7.0, 4.6, 9.0, 6.6], { label: "Look", lines: ["CUBONE: Kyarugoo!"] });
      return R;
    },
    lavender_name_rater() {
      const R = new Room("lavender_name_rater", { name: "Name Rater's house", floor: "Lavender Town", W: 12, D: 11 });
      plainHouse(R, { paper: "paperLavender", floor: "lavBoards", rug: "lavRug", window: WINDOW, cupboard: ["A cupboard. Inside: name books, thick with notes."] });
      R.npcs.push({ name: "Name Rater", key: "name_raters_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "silph_president" },
        talk: () => [{ text: "Hello, hello! I am the official NAME RATER! Want me to rate the nicknames of your POKéMON?" }, { text: "(In the game: YES, pick one, and you can give it a new name.)" }] });
      return R;
    },
  });
})();

// ---------- (19:06) the Museum's inside, restyled first in the interiors pass: as its stone outside ----------
// Polished marble underfoot in two warm tones with fine veins; cream plaster walls in panels; dark walnut wainscot in
// raised panels; fluted stone pillars; the counter in honey wood with an inset panel and a brass kick; brushed brass.
TEXDEF.museumFloor = [32, 32, (P, r) => {
  for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) {
    const x0 = tx * 8, y0 = ty * 8, base = (tx + ty) % 2 ? hx("#D9CDB3") : hx("#BFAF91");
    P.rect(x0, y0, 8, 8, base); P.noise(x0 + 1, y0 + 1, 6, 6, base, 0.035, r);
    for (let i = 0; i < 3; i++) { const vx = x0 + 1 + Math.floor(r() * 6), vy = y0 + 1 + Math.floor(r() * 6); P.rect(vx, vy, 2, 1, hx("#A8977A")); }
    P.rect(x0, y0, 8, 1, hx("#8F7F65")); P.rect(x0, y0, 1, 8, hx("#8F7F65"));
    P.rect(x0 + 1, y0 + 1, 7, 1, hx("#EDE3CC"));
  }
}];
MATDEF.museumFloor = { tex: "museumFloor", per: 2 };
TEXDEF.museumWall = [16, 16, (P, r) => { P.fill(hx("#F1E7CD"), 0.015, r); P.rect(2, 2, 12, 1, hx("#E3D5B1")); P.rect(2, 13, 12, 1, hx("#FBF5E5")); P.rect(2, 2, 1, 12, hx("#E3D5B1")); P.rect(13, 2, 1, 12, hx("#FBF5E5")); }];
TEXDEF.museumBand = [16, 16, (P, r) => {
  P.fill(hx("#6A4529"), 0.03, r);
  P.rect(0, 0, 16, 1, hx("#8E6440")); P.rect(0, 15, 16, 1, hx("#432A18"));
  P.rect(2, 3, 12, 1, hx("#4E321D")); P.rect(2, 12, 12, 1, hx("#8A6140")); P.rect(2, 3, 1, 10, hx("#4E321D")); P.rect(13, 3, 1, 10, hx("#8A6140"));
  P.rect(3, 4, 10, 8, hx("#74502F"));
}];
TEXDEF.museumPillar = [16, 16, (P, r) => {
  P.fill(hx("#EDE4CF"), 0.015, r);
  for (let x = 1; x < 16; x += 3) { P.rect(x, 0, 1, 16, hx("#D3C5A3")); P.rect(x + 1, 0, 1, 16, hx("#FAF4E4")); }
}];
TEXDEF.museumGold = [16, 16, (P, r) => { P.fill(hx("#C9A54C"), 0.03, r); for (let y = 0; y < 16; y += 2) P.rect(0, y, 16, 1, hx("#D7B65E")); P.rect(0, 0, 16, 1, hx("#F0D88E")); P.rect(0, 15, 16, 1, hx("#9C7C30")); }];
TEXDEF.counterYellow = [16, 16, (P, r) => {
  P.fill(hx("#D9A85C"), 0.025, r);
  P.rect(0, 0, 16, 2, hx("#EAC27E")); P.rect(0, 13, 16, 3, hx("#B98A3A")); P.rect(0, 14, 16, 1, hx("#D2B060"));
  P.rect(2, 4, 12, 1, hx("#B88A48")); P.rect(2, 10, 12, 1, hx("#EAC88C")); P.rect(2, 4, 1, 7, hx("#B88A48")); P.rect(13, 4, 1, 7, hx("#EAC88C"));
}];
