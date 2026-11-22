// ---------- 0.50.0 (20:07 to 20:20): every route gate a way through, Routes 12 and 16's house ----------
(function () {
  // The route gates (John's one gatehouse design, all over the map): Let's Go's gate inside (one design for every gate,
  // who's in it its own), in at one side and out at the other. A gate that runs west to east in the world is the same
  // room: its door at the room's south, its way on at the north, each joined to one of its two sides.
  const GATES = [
    { id: "gate_r5", name: "Route 5 gate", floor: "Saffron City's north gate", group: "Saffron City", needs: "tea",
      people: [["Guard", "route_5_gate_p0", 12.6, 6.0, "w", "guard"]] },
    { id: "gate_r6", name: "Route 6 gate", floor: "Saffron City's south gate", group: "Saffron City", needs: "tea",
      people: [["Guard", "route_6_gate_p0", 12.6, 6.0, "w", "guard"]] },
    { id: "gate_r7", name: "Route 7 gate", floor: "Saffron City's west gate", group: "Saffron City", needs: "tea",
      people: [["Guard", "route_7_gate_p0", 12.6, 6.0, "w", "guard"]] },
    { id: "gate_r8", name: "Route 8 gate", floor: "Saffron City's east gate", group: "Saffron City", needs: "tea",
      people: [["Guard", "route_8_gate_p0", 12.6, 6.0, "w", "guard"]] },
    { id: "gate_r11", name: "Route 11 gate", floor: "Route 11 to Route 12", group: "Routes 11 to 18", up: "gate_r11_2f",
      people: [["Guard", "route_11_gate_1f_p0", 12.6, 7.6, "w", "guard"]] },
    { id: "gate_r12", name: "Route 12 gate", floor: "Lavender Town to Route 12", group: "Routes 11 to 18", up: "gate_r12_2f",
      people: [["Guard", "route_12_gate_1f_p0", 12.6, 7.6, "w", "guard"]] },
    { id: "gate_r15", name: "Route 15 gate", floor: "Fuchsia City to Route 15", group: "Routes 11 to 18", up: "gate_r15_2f",
      people: [["Guard", "route_15_gate_1f_p0", 12.6, 7.6, "w", "guard"]] },
    { id: "gate_r16n", name: "Route 16 gate", floor: "North way (to the house)", group: "Routes 11 to 18", up: "gate_r16_2f",
      people: [["Gambler", "route_16_gate_1f_p1", 2.4, 7.4, "e", "gambler"]] },
    { id: "gate_r16s", name: "Route 16 gate", floor: "South way (the Cycling Road)", group: "Routes 11 to 18", needs: "bicycle",
      people: [["Guard", "route_16_gate_1f_p0", 12.6, 6.0, "w", "guard"]] },
    { id: "gate_r18", name: "Route 18 gate", floor: "Fuchsia City to the Cycling Road", group: "Routes 11 to 18", needs: "bicycle",
      people: [["Guard", "route_18_gate_1f_p0", 12.6, 6.0, "w", "guard"]] },
  ];
  for (const g of GATES) PLACES.push({ id: g.id, name: g.name, floor: g.floor, group: g.group });
  PLACES.push({ id: "route12_house", name: "Route 12 house", floor: "The Fishing Guru's brother", group: "Routes 11 to 18" });
  PLACES.push({ id: "route16_house", name: "Route 16 house", floor: "HM02 FLY", group: "Routes 11 to 18" });
  const facing = { n: FACE.n, s: FACE.s, e: FACE.e, w: FACE.w };
  for (const g of GATES) {
    BUILD[g.id] = function () {
      const R = new Room(g.id, { name: g.name, floor: g.floor, W: 14, D: 14, outsideSize: "16 by 12" });
      lgGate(R, { south: { to: "town" }, north: { to: "town", needs: g.needs }, stairs: g.up ? { to: g.up, spawn: STAIRS_TOP } : null,
        view: "Outside: " + g.floor.toUpperCase().replace(/ TO /, " and ") + ".", leaflet: "A glass table. There's a leaflet: CYCLING ROAD, KANTO'S FASTEST WAY SOUTH!" });
      for (const [name, key, x, z, f, skin] of g.people) {
        R.npcs.push({ name, key, x, z, yaw: facing[f], look: { skin },
          talk: () => [{ text: name === "Guard" && g.needs === "tea" ? "I'm on guard duty. Gee, I'm thirsty, though!" : name === "Guard" && g.needs === "bicycle"
            ? "No pedestrians are allowed on CYCLING ROAD!" : "Welcome! Mind how you go." }] });
      }
      R.spawn = LG_GATE_IN;
      return R;
    };
  }

  // The gates' upstairs (FireRed's): binoculars at the windows north and south, the stairs down in the corner, and
  // who's there (Route 11's AIDE with the ITEMFINDER, Route 12's girl with a TM, Route 15's AIDE with the EXP.SHARE,
  // Route 16's couple out for a ride).
  const UPSTAIRS = [
    { id: "gate_r11_2f", down: "gate_r11", name: "Route 11 gate", north: "DIGLETT's CAVE's mouth, and ROUTE 11's grass", south: "VERMILION CITY's harbor",
      people: [["Aide", "route_11_gate_2f_p0", 4.0, 5.2, "s", "scientist"]] },
    { id: "gate_r12_2f", down: "gate_r12", name: "Route 12 gate", north: "LAVENDER TOWN, the POKéMON TOWER over it", south: "ROUTE 12's piers, and a big POKéMON asleep on one",
      people: [["Girl", "route_12_gate_2f_p0", 5.0, 5.6, "e", "brunette_girl"]] },
    { id: "gate_r15_2f", down: "gate_r15", name: "Route 15 gate", north: "ROUTE 15's grass and fences", south: "the sea, and SEAFOAM ISLANDS far off",
      people: [["Aide", "route_15_gate_2f_p0", 4.0, 5.2, "s", "scientist"]] },
    { id: "gate_r16_2f", down: "gate_r16n", name: "Route 16 gate", north: "CELADON CITY's rooftops", south: "CYCLING ROAD, running all the way down to FUCHSIA",
      people: [["Boy", "route_16_gate_2f_p0", 4.4, 5.0, "e", "youngster"], ["Girl", "route_16_gate_2f_p1", 5.6, 5.0, "w", "girl"]] },
  ];
  for (const u of UPSTAIRS) {
    PLACES.push({ id: u.id, name: u.name, floor: "Upstairs", group: "Routes 11 to 18" });
    BUILD[u.id] = function () {
      const R = new Room(u.id, { name: u.name, floor: "Upstairs", W: 14, D: 10 });
      shell(R, { paper: "paperGateLG", floor: "gateCheck", floorRects: [[0, 0, 10, 10], [10, 2, 14, 10]], skirt: "gateWhite" });
      band(R, 0.24, "gateBlue", 1.1); band(R, 1.34, "gateWhite", 0.1);
      stairsDown(R, 10, 0, 2, { to: u.down, spawn: { x: 9.2, z: 3.0, yaw: FACE.w }, paper: "paperGateLG" });
      const G = R.geo;
      // The binoculars on stands at the windows.
      windowN(R, 3.0, 1.4, 3.0, 1.6);
      for (const [x, z, wall, view] of [[4.5, 1.0, "n", u.north], [7.0, 9.0, "s", u.south]]) {
        G.box(x - 0.08, 0, z - 0.08, x + 0.08, 1.1, z + 0.08, "steel");
        G.box(x - 0.3, 1.1, z - 0.14, x + 0.3, 1.32, z + 0.14, "greyPlastic");
        R.solid(x - 0.3, z - 0.3, x + 0.3, z + 0.3);
        R.act([x - 0.7, z - 0.7, x + 0.7, z + 0.7], { label: "Look", lines: ["You look through the binoculars...", "You can see " + view + "!"] });
      }
      R.item("Binoculars", "Coin-free binoculars on steel stands, at the windows north and south.");
      glassTable(R, 1.0, 6.0, 2.4, 7.4, { lines: ["A glass table, a few leaflets about the routes round here."] });
      stool(R, 1.7, 5.3); stool(R, 1.7, 8.1);
      plant(R, 13.0, 9.0, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      for (const [name, key, x, z, f, skin] of u.people) R.npcs.push({ name, key, x, z, yaw: facing[f], look: { skin }, talk: () => [{ text: "What a view!" }] });
      R.spawn = STAIRS_TOP;
      return R;
    };
  }

  // Routes 12 and 16's house (the same building on both, 20:20; inside, as Vermilion's houses): the FISHING GURU's
  // brother on Route 12 (the SUPER ROD), and on Route 16 the girl with HM02 FLY and her bird POKéMON.
  const ROUTE_WINDOW = ["Outside: the sea, and a long way off, the piers."];
  Object.assign(BUILD, {
    route12_house() {
      const R = new Room("route12_house", { name: "Route 12 house", floor: "The Fishing Guru's brother", W: 12, D: 11 });
      plainHouse(R, { paper: "vermWall", rug: "rugBlue", window: ROUTE_WINDOW, cupboard: ["A cupboard. Inside: reels, line and a box of lures."] });
      const G = R.geo;
      for (const x of [1.0, 1.5, 2.0]) G.box(x, 0.2, 0.12, x + 0.06, 2.4, 0.2, "walnut");
      R.act([0.6, 0, 2.6, 1.0], { label: "Look", lines: ["His rods on the wall: an OLD ROD, a GOOD ROD, and a gap for the best."] });
      R.npcs.push({ name: "Fishing Guru", key: "route_12_super_rod_house_p0", x: 2.6, z: 4.4, yaw: FACE.e, look: { skin: "fishing_guru" },
        talk: () => [{ text: "I'm the FISHING GURU's brother! I simply Looove fishing!" }, { text: "(In the game: he gives you the SUPER ROD.)" }] });
      return R;
    },
    route16_house() {
      const R = new Room("route16_house", { name: "Route 16 house", floor: "HM02 FLY", W: 12, D: 11 });
      plainHouse(R, { paper: "vermWall", rug: "rugGreen", window: ["Outside: ROUTE 16, and the trees along it."], cupboard: ["A cupboard. Inside: bird seed, and feathers in a jar."] });
      R.npcs.push({ name: "Girl", key: "route_16_fly_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "girl" },
        talk: () => [{ text: "Oh, you found my secret retreat! Please don't tell anyone." }, { text: "I'll give you this HM, so keep it a secret!" }] });
      R.pets = [{ sid: "fearow", x: 8.0, z: 6.4, yaw: 270 }];
      R.act([7.0, 5.4, 9.0, 7.4], { label: "Look", lines: ["FEAROW: Kyueen!"] });
      return R;
    },
  });
})();

// ---------- (20:12) the Pokémon Center's inside, restyled as the Museum was: every Center shares this room ----------
// Glazed ceramic tiles underfoot (a fine grout, each tile lit along its top edge, a soft sheen); the cream-peach
// ring of tiles at the door the same; the walls white enamel panels with a fine join and a soft sheen; the orange
// trim a painted rail with a groove along it.
(function () {
  const tiles = (a, b) => (P, r) => {
    for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) {
      const x0 = tx * 8, y0 = ty * 8, base = (Math.floor(tx / 2) + Math.floor(ty / 2)) % 2 ? b : a;
      P.rect(x0, y0, 8, 8, base); P.noise(x0 + 1, y0 + 1, 6, 6, base, 0.02, r);
      P.rect(x0, y0, 8, 1, shadec(base, -0.16)); P.rect(x0, y0, 1, 8, shadec(base, -0.16));
      P.rect(x0 + 1, y0 + 1, 6, 1, shadec(base, 0.1));
      P.rect(x0 + 2, y0 + 2, 2, 1, shadec(base, 0.16));
    }
  };
  TEXDEF.mcCenterCheck = [32, 32, tiles(hx("#FAF6F0"), hx("#F4D9D1"))];
  TEXDEF.mcPeachCheck = [32, 32, tiles(hx("#FCF4E8"), hx("#F4D2B2"))];
  TEXDEF.paperCenter = [16, 16, (P, r) => {
    P.fill(hx("#FAF7F3"), 0.012, r);
    P.rect(0, 0, 16, 1, hx("#E9E2DA")); P.rect(0, 1, 16, 1, hx("#FFFFFF"));
    P.rect(7, 2, 1, 14, hx("#EEE7DF")); P.rect(8, 2, 1, 14, hx("#FFFFFF"));
  }];
  TEXDEF.orangeTrim = [16, 16, (P, r) => {
    P.fill(hx("#EA843C"), 0.02, r);
    P.rect(0, 0, 16, 2, hx("#F6AE70")); P.rect(0, 7, 16, 1, hx("#C9672A")); P.rect(0, 8, 16, 1, hx("#F29A57")); P.rect(0, 15, 16, 1, hx("#B95B22"));
  }];
})();
