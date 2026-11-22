// ---------- 0.51 (20:58, "do it all"): the S.S. ANNE and the POKéMON TOWER ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // The S.S. ANNE as Let's Go's liner: a navy carpet with a gold diamond, cream panelled walls, dark wood rails,
  // brass, portholes.
  reg("ssCarpet", 32, 32, (P, r) => {
    P.fill(hx("#22346B"), 0.03, r);
    for (const [cx, cy] of [[8, 8], [24, 24]]) for (let d = -4; d <= 4; d++) { P.set(cx + d, cy - (4 - Math.abs(d)), hx("#D8B45A")); P.set(cx + d, cy + (4 - Math.abs(d)), hx("#D8B45A")); }
    for (const [cx, cy] of [[24, 8], [8, 24]]) P.rect(cx - 1, cy - 1, 2, 2, hx("#B8923E"));
  }, { per: 2 });
  reg("ssPanel", 16, 16, (P, r) => { P.fill(hx("#F3E9D2"), 0.012, r); P.rect(2, 2, 12, 1, hx("#E2D3B0")); P.rect(2, 13, 12, 1, hx("#FBF5E6")); P.rect(2, 2, 1, 12, hx("#E2D3B0")); P.rect(13, 2, 1, 12, hx("#FBF5E6")); });
  reg("ssRail", 16, 16, (P, r) => { P.fill(hx("#5A3A24"), 0.03, r); P.rect(0, 0, 16, 2, hx("#7A5236")); P.rect(0, 14, 16, 2, hx("#3E2818")); });
  reg("ssPorthole", 16, 16, (P, r) => {
    P.fill(hx("#F3E9D2"), 0.01, r);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.hypot(x - 7.5, y - 7.5); if (d < 6.5) P.set(x, y, d > 5.2 ? hx("#C9A04A") : hx(y < 7 ? "#9BD2F0" : "#4A8CC8")); }
  });
  // The POKéMON TOWER: a deep plum carpet with a lilac border, lavender stone walls, grey headstones.
  reg("towerCarpet", 32, 32, (P, r) => { P.fill(hx("#4A2F5C"), 0.03, r); P.rect(0, 0, 32, 2, hx("#8E72B0")); P.rect(0, 16, 32, 1, hx("#5C3E70")); P.rect(16, 0, 1, 32, hx("#5C3E70")); }, { per: 2 });
  reg("towerStone", 16, 16, (P, r) => { P.fill(hx("#8E83A6"), 0.03, r); P.rect(0, 0, 16, 1, hx("#6E6488")); P.rect(0, 8, 16, 1, hx("#6E6488")); P.rect(7, 0, 1, 8, hx("#6E6488")); P.rect(3, 8, 1, 8, hx("#6E6488")); });
  reg("headstone", 16, 16, (P, r) => { P.fill(hx("#A6A2AE"), 0.05, r); P.rect(0, 0, 16, 1, hx("#C9C6D0")); P.rect(5, 5, 6, 1, hx("#7E7A88")); P.rect(7, 3, 2, 6, hx("#7E7A88")); });

  const T = (R, name, key, x, z, f, skin, line) => R.npcs.push({ name, key, role: "trainer", x, z, yaw: FACE[f], look: { skin }, talk: () => [{ text: line }] });
  const Pn = (R, name, key, x, z, f, skin, line) => R.npcs.push({ name, key, x, z, yaw: FACE[f], look: { skin }, talk: () => [{ text: line }] });
  const wall = (R, x0, z0, x1, z1, mat) => { R.geo.box(x0, 0, z0, x1, R.H, z1, { all: mat || "ssPanel" }); R.solid(x0, z0, x1, z1); };

  PLACES.push({ id: "ss_anne_1f", name: "S.S.ANNE", floor: "1F", group: "S.S.ANNE" });
  PLACES.push({ id: "ss_anne_2f", name: "S.S.ANNE", floor: "2F", group: "S.S.ANNE" });
  PLACES.push({ id: "ss_anne_captain", name: "S.S.ANNE", floor: "The CAPTAIN's office", group: "S.S.ANNE" });
  const SS_1F_TOP = { x: 25.2, z: 8.0, yaw: FACE.w }, SS_2F_ARRIVE = { x: 29.0, z: 12.6, yaw: FACE.w };

  Object.assign(BUILD, {
    ss_anne_1f() {
      // In off the gangway at the south; the corridor along the south, three cabins off it to the north, the stairs
      // up at the east end. The passengers, the waiter, a sailor; trainers in the cabins.
      const R = new Room("ss_anne_1f", { name: "S.S.ANNE", floor: "1F", W: 30, D: 14 });
      shell(R, { paper: "ssPanel", floor: "ssCarpet", door: [14, 16], entry: true, skirt: "ssRail", ledge: "ssRail" });
      band(R, 1.0, "ssRail", 0.1);
      for (const x0 of [0, 10, 20]) {
        wall(R, x0 + 0.0, 6.0, x0 + 3.6, 6.25); wall(R, x0 + 5.4, 6.0, x0 + 10.0, 6.25);
        if (x0 > 0) wall(R, x0 - 0.12, 0, x0 + 0.12, 6.0);
        bed(R, x0 + 0.4, 0.2, "s", { quilt: "quilt" });
        table(R, x0 + 6.0, 1.6, x0 + 8.4, 3.4, { top: "lab", leg: "walnut", name: "Table", note: "A white cloth, a vase of flowers." });
        R.geo.box(x0 + 4.0, 1.4, 0.12, x0 + 5.4, 2.6, 0.14, { s: "ssPorthole", all: null });
      }
      stairsUp(R, 26, 7, 2, { to: "ss_anne_2f", spawn: SS_2F_ARRIVE, wall: "ssPanel" });
      T(R, "Gentleman", "ss_anne_1f_rooms_0", 8.6, 4.6, "w", "gentleman", "Competing against the young keeps me youthful.");
      T(R, "Lass", "ss_anne_1f_rooms_3", 13.4, 4.4, "e", "cooltrainer_f", "I collected these POKéMON from all around the world!");
      Pn(R, "Girl", "ss_anne_1f_rooms_p0", 16.6, 2.6, "w", "girl", "Waiter, I would like a cherry pie please!");
      T(R, "Youngster", "ss_anne_1f_rooms_2", 24.0, 4.6, "n", "youngster", "I love POKéMON! Do you?");
      Pn(R, "Gentleman", "ss_anne_1f_rooms_p5", 28.2, 2.4, "w", "gentleman", "Ssh! I'm a GLOBAL POLICE agent!");
      Pn(R, "Waiter", "ss_anne_1f_p0", 6.0, 9.4, "s", "waiter", "Bonjour! I am le waiter on this ship!");
      Pn(R, "Sailor", "ss_anne_1f_p1", 19.0, 10.6, "w", "sailor", "The passengers are restless! You might be challenged by the more bored ones!");
      T(R, "Gentleman", "ss_anne_1f_rooms_1", 11.0, 9.0, "s", "gentleman", "Which do you like, a strong or a rare POKéMON?");
      doormat(R, 14, 16, { to: "town", spawn: { x: 15.0, z: R.D - 2.0, yaw: FACE.s }, mat: "matGate" });
      R.spawn = { x: 15.0, z: R.D - 1.5, yaw: FACE.n };
      R.item("S.S.ANNE 1F", "A navy carpet with gold diamonds, cream panelled walls, dark wood rails, portholes in the cabins.");
      return R;
    },
    ss_anne_2f() {
      // The upper corridor: the stairs down at the east end, cabins to the north-west, and at the north the way to
      // the CAPTAIN's office, where your rival's waiting.
      const R = new Room("ss_anne_2f", { name: "S.S.ANNE", floor: "2F", W: 30, D: 14 });
      shell(R, { paper: "ssPanel", floor: "ssCarpet", northNook: [22, 26], skirt: "ssRail", floorRects: [[0, 2, 30, 10], [0, 12, 30, 14], [0, 10, 26, 12], [22, 0, 26, 2]] });
      band(R, 1.0, "ssRail", 0.1);
      stairsDown(R, 26, 10, 2, { to: "ss_anne_1f", spawn: SS_1F_TOP, paper: "ssPanel" });
      passageN(R, 22, 26, { to: "ss_anne_captain", spawn: { x: 6.0, z: 7.6, yaw: FACE.n }, mat: "matGateN", frame: "ssRail" });
      for (const x0 of [0, 10]) {
        wall(R, x0, 7.0, x0 + 3.6, 7.25); wall(R, x0 + 5.4, 7.0, x0 + 10.0, 7.25);
        wall(R, x0 + 9.88, 2, x0 + 10.12, 7.0);
        bed(R, x0 + 0.4, 2.2, "s", { quilt: "quilt" });
        R.geo.box(x0 + 4.0, 1.4, 2.12, x0 + 5.4, 2.6, 2.14, { s: "ssPorthole", all: null });
      }
      T(R, "Gentleman", "ss_anne_2f_rooms_0", 7.4, 4.4, "w", "gentleman", "Ah, a young trainer! Let's see what you've got!");
      T(R, "Fisherman", "ss_anne_2f_rooms_1", 13.0, 4.2, "e", "fisher", "Check out what I fished up!");
      Pn(R, "Girl", "ss_anne_2f_rooms_p5", 17.6, 3.0, "w", "brunette_girl", "The CAPTAIN looked really sick and pale!");
      T(R, "Gentleman", "ss_anne_2f_rooms_2", 4.0, 10.6, "e", "gentleman", "Which do you like, a strong or a rare POKéMON?");
      T(R, "Lass", "ss_anne_2f_rooms_3", 18.0, 10.6, "n", "cooltrainer_f", "You look like you could use a battle!");
      Pn(R, "Waiter", "ss_anne_2f_p0", 9.0, 12.2, "n", "waiter", "This ship, she is a luxury liner for trainers!");
      T(R, "Blue", "ss_anne_2f_0", 24.0, 3.6, "s", "blue", "Bonjour! Ha! Are you lost too? Let's see how your POKéMON are doing!");
      R.spawn = SS_2F_ARRIVE;
      return R;
    },
    ss_anne_captain() {
      // The CAPTAIN's office: his desk, a ship's wheel on the wall, a basin. He's seasick; rub his back and he
      // teaches you CUT (HM01).
      const R = new Room("ss_anne_captain", { name: "S.S.ANNE", floor: "The CAPTAIN's office", W: 12, D: 10 });
      shell(R, { paper: "ssPanel", floor: "ssCarpet", door: [5, 7], entry: true, skirt: "ssRail", ledge: "ssRail" });
      band(R, 1.0, "ssRail", 0.1);
      const G = R.geo;
      table(R, 1.0, 1.0, 4.0, 2.4, { top: "walnut", leg: "walnut", name: "Desk", note: "His desk: charts, a sextant, a cup of cold tea." });
      bookshelf(R, 8.0, 0, "s", 3, 2.6);
      for (let a = 0; a < 8; a++) { const t = a * Math.PI / 4; G.box(6.0 + Math.cos(t) * 0.9 - 0.06, 1.9 + Math.sin(t) * 0.9 - 0.06, 0.12, 6.0 + Math.cos(t) * 0.9 + 0.06, 1.9 + Math.sin(t) * 0.9 + 0.06, 0.3, "walnut"); }
      G.box(5.7, 1.6, 0.12, 6.3, 2.2, 0.3, "brushedSteel");
      R.act([5.0, 0, 7.0, 1.0], { label: "Look", lines: ["A ship's wheel on the wall, polished brass at its hub."] });
      G.box(10.0, 0, 6.0, 11.6, 0.9, 7.4, { all: "shelfWhite", top: "linen" });
      R.act([9.8, 5.8, 11.8, 7.6], { label: "Look", lines: ["A basin. The CAPTAIN's been using it a lot."] });
      Pn(R, "Captain", "ss_anne_captains_room_p0", 10.6, 5.2, "s", "captain", "CAPTAIN: Ooargh... I feel hideous... Urrp! Seasick...");
      doormat(R, 5, 7, { to: "ss_anne_2f", spawn: { x: 24.0, z: 3.0, yaw: FACE.s }, mat: "matGate" });
      R.spawn = { x: 6.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },
  });

  // The POKéMON TOWER, seven floors: in at 1F (a counter, the mourners), the stairs up in one corner and down in
  // another, rows of headstones, the CHANNELERS among them; your rival on 2F; MR. FUJI on 7F.
  const FLOORS = [
    { f: 1, people: [["Receptionist", "pokemon_tower_1f_p0", 10.0, 3.2, "s", "link_receptionist"], ["Lady", "pokemon_tower_1f_p1", 4.0, 9.0, "e", "lady"],
      ["Man", "pokemon_tower_1f_p2", 14.0, 8.0, "n", "balding_guy"], ["Girl", "pokemon_tower_1f_p3", 6.0, 12.0, "n", "girl"], ["Channeler", "pokemon_tower_1f_p4", 15.4, 12.0, "w", "channeler"]] },
    { f: 2, people: [["Blue", "pokemon_tower_2f_p0", 10.0, 8.0, "s", "blue"], ["Channeler", "pokemon_tower_2f_p1", 4.0, 12.6, "e", "channeler"]] },
    { f: 3, trainers: [["Channeler", "pokemon_tower_3f_0", 14.6, 6.0, "w"], ["Channeler", "pokemon_tower_3f_1", 6.0, 9.0, "s"], ["Channeler", "pokemon_tower_3f_2", 12.0, 13.0, "s"]] },
    { f: 4, trainers: [["Channeler", "pokemon_tower_4f_0", 5.0, 6.0, "e"], ["Channeler", "pokemon_tower_4f_1", 11.0, 9.0, "s"], ["Channeler", "pokemon_tower_4f_2", 15.0, 13.0, "w"]] },
    { f: 5, trainers: [["Channeler", "pokemon_tower_5f_0", 14.6, 5.0, "w"], ["Channeler", "pokemon_tower_5f_1", 14.6, 11.0, "w"], ["Channeler", "pokemon_tower_5f_2", 4.6, 7.0, "e"], ["Channeler", "pokemon_tower_5f_3", 4.6, 13.0, "e"]],
      people: [["Channeler", "pokemon_tower_5f_p0", 10.0, 9.4, "s", "channeler"]] },
    { f: 6, trainers: [["Channeler", "pokemon_tower_6f_0", 5.0, 6.0, "e"], ["Channeler", "pokemon_tower_6f_1", 10.0, 10.0, "s"], ["Channeler", "pokemon_tower_6f_2", 15.0, 13.0, "w"]] },
    { f: 7, people: [["Mr. Fuji", "pokemon_tower_7f_p0", 10.0, 3.4, "s", "mr_fuji"]] },
  ];
  const TW = 20, TD = 16;
  for (const fl of FLOORS) {
    const id = "pokemon_tower_" + fl.f + "f";
    PLACES.push({ id, name: "Pokémon Tower", floor: fl.f + "F", group: "Lavender Town" });
    BUILD[id] = function () {
      const first = fl.f === 1, last = fl.f === 7;
      // The stairs up along the north wall at the east end, the stairs down at the west end (each floor the same).
      const R = new Room(id, { name: "Pokémon Tower", floor: fl.f + "F", W: TW, D: TD, night: true });
      shell(R, Object.assign({ paper: "towerStone", floor: "towerCarpet", skirt: "ssRail" }, first ? { door: [9, 11], entry: true } : { floorRects: [[0, 2, TW, TD], [4, 0, TW, 2]] }));
      if (!last) stairsUp(R, TW - 4, 0, 2, { to: "pokemon_tower_" + (fl.f + 1) + "f", spawn: { x: 3.5, z: 2.9, yaw: FACE.s }, wall: "towerStone" });
      if (!first) stairsDown(R, 0, 0, 2, { to: "pokemon_tower_" + (fl.f - 1) + "f", spawn: { x: TW - 4.8, z: 1.0, yaw: FACE.w }, paper: "towerStone" });
      // Rows of headstones (not on 1F and 7F), gaps to walk between.
      const G = R.geo;
      if (!first && !last) for (let z = 5; z < TD - 2; z += 3) for (let x = 2; x < TW - 2; x += 3) {
        if ((x * 7 + z * 5 + fl.f) % 4 === 0) continue;
        G.box(x, 0, z, x + 0.9, 0.12, z + 0.5, "towerStone"); G.box(x + 0.15, 0.12, z + 0.1, x + 0.75, 1.0, z + 0.36, "headstone");
        R.solid(x, z, x + 0.9, z + 0.5);
      }
      if (!first && !last) R.act([2, 5, TW - 2, TD - 2], { label: "Look", lines: ["A headstone. Someone's left fresh flowers."] });
      if (first) {
        G.box(8.0, 0, 1.6, 12.0, 1.0, 2.4, { all: "honey", top: "walnut" }); R.solid(8.0, 1.6, 12.0, 2.4);
        R.act([7.8, 1.2, 12.2, 2.8], { label: "Look", lines: ["The TOWER's counter. A book for messages to those at rest."] });
        doormat(R, 9, 11, { to: "town", spawn: { x: 10.0, z: TD - 2.0, yaw: FACE.s }, mat: "matGate" });
      }
      for (const [name, key, x, z, f] of fl.trainers || []) T(R, name, key, x, z, f, "channeler", "Be gone... evil spirit!");
      for (const [name, key, x, z, f, skin] of fl.people || []) Pn(R, name, key, x, z, f, skin, "...");
      R.spawn = first ? { x: 10.0, z: TD - 1.5, yaw: FACE.n } : { x: 3.5, z: 2.9, yaw: FACE.s };
      R.item("POKéMON TOWER " + fl.f + "F", "A deep plum carpet with a lilac border, lavender stone walls" + (!first && !last ? ", rows of grey headstones." : "."));
      return R;
    };
  }
})();
