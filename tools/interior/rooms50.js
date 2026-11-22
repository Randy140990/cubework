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
    { id: "gate_r11", name: "Route 11 gate", floor: "Route 11 to Route 12", group: "Routes 11 to 18",
      people: [["Guard", "route_11_gate_1f_p0", 12.6, 6.0, "w", "guard"], ["Scientist", "route_11_gate_2f_p0", 2.4, 7.4, "e", "scientist"]] },
    { id: "gate_r12", name: "Route 12 gate", floor: "Lavender Town to Route 12", group: "Routes 11 to 18",
      people: [["Guard", "route_12_gate_1f_p0", 12.6, 6.0, "w", "guard"], ["Girl", "route_12_gate_2f_p0", 2.4, 7.4, "e", "girl"]] },
    { id: "gate_r15", name: "Route 15 gate", floor: "Fuchsia City to Route 15", group: "Routes 11 to 18",
      people: [["Guard", "route_15_gate_1f_p0", 12.6, 6.0, "w", "guard"], ["Scientist", "route_15_gate_2f_p0", 2.4, 7.4, "e", "scientist"]] },
    { id: "gate_r16n", name: "Route 16 gate", floor: "North way (to the house)", group: "Routes 11 to 18",
      people: [["Gambler", "route_16_gate_1f_p1", 2.4, 7.4, "e", "gambler"], ["Boy", "route_16_gate_2f_p0", 11.0, 9.0, "w", "youngster"]] },
    { id: "gate_r16s", name: "Route 16 gate", floor: "South way (the Cycling Road)", group: "Routes 11 to 18", needs: "bicycle",
      people: [["Guard", "route_16_gate_1f_p0", 12.6, 6.0, "w", "guard"], ["Girl", "route_16_gate_2f_p1", 2.4, 7.4, "e", "girl"]] },
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
      lgGate(R, { south: { to: "town" }, north: { to: "town", needs: g.needs } });
      for (const [name, key, x, z, f, skin] of g.people) {
        R.npcs.push({ name, key, x, z, yaw: facing[f], look: { skin },
          talk: () => [{ text: name === "Guard" && g.needs === "tea" ? "I'm on guard duty. Gee, I'm thirsty, though!" : name === "Guard" && g.needs === "bicycle"
            ? "No pedestrians are allowed on CYCLING ROAD!" : "Welcome! Mind how you go." }] });
      }
      R.spawn = LG_GATE_IN;
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
