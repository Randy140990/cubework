// ---------- 0.73.0 (John's 28 Nov 09:22): the MAGNET TRAIN's two stations ----------
// "we'll need to do the interiors, because I know in the interiors in game, I'm pretty sure it'll show you getting on
// it and it going away". GOLDENROD's and SAFFRON's stations (in from the doorway under each one's roof in John's
// world), laid out as Crystal's (pret/pokecrystal's GoldenrodMagnetTrainStation and SaffronMagnetTrainStation): the
// waiting hall at the front, its seats, its planters and its ticket machines; the gate in the middle of the rail
// across the hall, the OFFICER at it; past it the platform and the train waiting at it, its doors open. In the game
// you talk to the OFFICER to ride (game/MagnetTrain.java): the train pulls out along John's line and into the tunnel,
// and you come out of the train at the other station. Each in its own city's colours, as outside: GOLDENROD's teal
// and gold, SAFFRON's green and yellow.
(function () {
  PLACES.push({ id: "goldenrod_magnet_train_station", name: "Magnet Train Station", floor: "Goldenrod City", group: "Goldenrod City" });
  PLACES.push({ id: "saffron_magnet_train_station", name: "Magnet Train Station", floor: "Saffron City", group: "Saffron City" });
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };

  // ----- What the two share: the platform, the line, the train -----
  // The platform: pale grey concrete slabs, a fine join every block.
  reg("mtPlatform", 16, 16, (P, r) => { P.fill(hx("#C9CCCF"), 0.01, r); P.rect(0, 0, 16, 1, hx("#B4B8BC")); P.rect(0, 0, 1, 16, hx("#B4B8BC")); P.rect(1, 1, 15, 1, hx("#D6D9DC")); });
  // Its edge: the yellow line with its raised dots.
  reg("mtEdge", 16, 16, (P, r) => { P.fill(hx("#F2C230"), 0.01, r); for (let y = 2; y < 16; y += 4) for (let x = 2; x < 16; x += 4) { P.rect(x, y, 2, 2, hx("#FFDC5A")); P.set(x + 1, y + 1, hx("#C99A18")); } });
  // The line: dark ballast, two steel rails on it (drawn as their own boxes), a sleeper every half block.
  reg("mtBed", 16, 16, (P, r) => { P.fill(hx("#45474C"), 0.03, r); for (const y of [3, 11]) P.rect(0, y, 16, 2, hx("#5B5E64")); });
  reg("mtRail", 16, 16, (P, r) => { P.fill(hx("#9EA4AC"), 0.01, r); P.rect(0, 0, 16, 2, hx("#D9DEE4")); P.rect(0, 14, 16, 2, hx("#6E747C")); });
  // The train: white sides, the window band dark glass in white frames, a stripe in its line's colour; the roof pale
  // grey; the skirt over the bogies dark; the noses' windscreens deep blue glass; a door's outline in the sides.
  reg("mtBody", 16, 16, (P, r) => { P.fill(hx("#F3F4F2"), 0.006, r); P.rect(0, 15, 16, 1, hx("#DADCD8")); });
  reg("mtWin", 16, 16, (P, r) => { P.fill(hx("#F3F4F2"), 0.006, r); P.rect(0, 3, 16, 10, hx("#28324A")); P.rect(1, 4, 6, 8, hx("#3A4A6E")); P.rect(9, 4, 6, 8, hx("#3A4A6E")); P.rect(2, 5, 3, 1, hx("#7E92BC")); P.rect(10, 5, 3, 1, hx("#7E92BC")); }, { glow: 0.15 });
  reg("mtDoor", 16, 32, (P, r) => {
    P.fill(hx("#F3F4F2"), 0.006, r); P.rect(2, 2, 12, 30, hx("#C8CCCF")); P.rect(3, 3, 10, 29, hx("#2E3650"));
    P.rect(4, 4, 3, 10, hx("#4A5C86")); P.rect(9, 4, 3, 10, hx("#4A5C86")); P.rect(7, 3, 2, 29, hx("#1E2438"));
  }, { fit: true });
  reg("mtRoof", 16, 16, (P, r) => { P.fill(hx("#D7DADD"), 0.01, r); P.rect(0, 0, 16, 1, hx("#E8EBEE")); P.rect(7, 0, 2, 16, hx("#C4C8CC")); });
  reg("mtSkirt", 16, 16, (P, r) => { P.fill(hx("#3C4048"), 0.02, r); P.rect(0, 0, 16, 1, hx("#565B64")); });
  reg("mtScreen", 16, 16, (P, r) => { P.fill(hx("#22304E"), 0.01, r); P.rect(0, 0, 16, 2, hx("#4E6694")); P.rect(3, 3, 5, 1, hx("#8CA6D8")); }, { glow: 0.2 });
  reg("mtLamp", 16, 16, (P, r) => { P.fill(hx("#FFF6C8"), 0, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); }, { glow: 1 });
  // The ticket gate's posts: steel, a green light on top where you'd tap your PASS.
  reg("mtPost", 16, 16, (P, r) => { P.fill(hx("#E6E8EA"), 0.006, r); P.rect(0, 0, 1, 16, hx("#FFFFFF")); P.rect(15, 0, 1, 16, hx("#B8BCC0")); });
  reg("mtPostTop", 16, 16, (P, r) => { P.fill(hx("#4A4E56"), 0.01, r); P.rect(4, 4, 8, 8, hx("#5DDC7A")); P.rect(5, 5, 3, 3, hx("#B8FFC8")); }, { glow: 0.6 });
  // The rail across the hall: steel posts and a rail, glass between.
  reg("mtGlass", 16, 16, (P, r) => { P.fill(hx("#CFE8F4"), 0, r); for (let i = 0; i < 16; i++) if (i % 7 < 2) P.line(i, 0, i - 5, 5, hx("#EAF6FC")); }, { alpha: 0.45 });
  // A ticket machine: its screen, its buttons, the slot for your PASS.
  reg("mtMachine", 16, 32, (P, r) => {
    P.fill(hx("#E8EAEC"), 0.006, r); P.rect(0, 0, 16, 2, hx("#FFFFFF")); P.rect(2, 4, 12, 9, hx("#2A3A5C")); P.rect(3, 5, 10, 7, hx("#6FB8E8"));
    P.rect(4, 7, 6, 1, hx("#FFFFFF")); P.rect(4, 9, 4, 1, hx("#FFFFFF"));
    for (let x = 3; x < 13; x += 3) P.rect(x, 16, 2, 2, hx(["#F2C230", "#5DBB63", "#E2484A", "#4C8FD6"][((x / 3) | 0) % 4]));
    P.rect(4, 21, 8, 1, hx("#3A3E46")); P.rect(3, 26, 10, 4, hx("#C4C8CC"));
  }, { fit: true, glow: 0.3 });

  // ----- Each station's own colours -----
  const LOOK = {
    // GOLDENROD's: pale blue tiles as Crystal's, the walls white over a teal band with gold trim, Johto's teal seats.
    goldenrod: {
      floor: ["#BCD6F0", "#A6C4E6", "#D4E6F8"], paper: ["#F6F7F4", "#E6E9E4"], band: "#2E8D97", trim: "#D8B84A", stripe: "#2E8D97", seat: "mtSeatTeal",
      seatC: ["#3FA2AC", "#2E8D97"], bed: "#7DBA5A", city: "GOLDENROD", other: "SAFFRON", to: "SAFFRON CITY",
      win: ["Outside: GOLDENROD CITY's streets, the RADIO TOWER over the roofs."],
    },
    // SAFFRON's: warm cream tiles, the walls cream over Saffron's green band with yellow trim, green seats.
    saffron: {
      floor: ["#F0E6C8", "#E4D7B0", "#F8F0DA"], paper: ["#FBF6E8", "#EFE6D0"], band: "#62A85D", trim: "#F2C554", stripe: "#62A85D", seat: "mtSeatGreen",
      seatC: ["#74B86C", "#5A9A55"], bed: "#E2484A", city: "SAFFRON", other: "GOLDENROD", to: "GOLDENROD CITY",
      win: ["Outside: SAFFRON CITY's streets, SILPH CO. standing over the roofs."],
    },
  };
  for (const [k, L] of Object.entries(LOOK)) {
    const c = L.floor.map(hx);
    reg("mtFloor_" + k, 32, 32, (P, r) => {
      for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
        const b = c[(tx + ty) % 2 ? 1 : 0];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
          let v = b;
          if (x === 0 || y === 0) v = shadec(b, -0.12); else if (x === 1 || y === 1) v = c[2];
          else if (x >= 4 && x <= 11 && y >= 4 && y <= 11 && (x === 4 || y === 4)) v = shadec(b, 0.1);
          P.set(tx * 16 + x, ty * 16 + y, v);
        }
      }
    });
    reg("mtPaper_" + k, 16, 16, (P, r) => { P.fill(hx(L.paper[0]), 0.006, r); P.rect(7, 0, 1, 16, hx(L.paper[1])); });
    reg("mtBand_" + k, 16, 16, (P, r) => { P.fill(hx(L.band), 0.008, r); P.rect(0, 0, 16, 2, shadec(hx(L.band), 0.25)); P.rect(0, 14, 16, 2, shadec(hx(L.band), -0.2)); });
    reg("mtTrim_" + k, 16, 16, (P, r) => { P.fill(hx(L.trim), 0.006, r); P.rect(0, 0, 16, 1, shadec(hx(L.trim), 0.3)); P.rect(0, 15, 16, 1, shadec(hx(L.trim), -0.25)); });
    // (The same train as the one you ride, tools/train73.py: its deep blue stripe, a gold line under it.)
    reg("mtStripe_" + k, 16, 16, (P, r) => { P.fill(hx("#F3F4F2"), 0.006, r); P.rect(0, 3, 16, 7, hx("#2B4C9B")); P.rect(0, 11, 16, 1, hx("#E0B84A")); });
    reg(L.seat, 16, 16, (P, r) => { P.fill(hx(L.seatC[0]), 0.01, r); P.rect(0, 0, 16, 1, shadec(hx(L.seatC[0]), 0.25)); P.rect(0, 15, 16, 1, hx(L.seatC[1])); });
    // The board over the gate: where the next train's for, its time ticking over.
    reg("mtBoard_" + k, 64, 16, (P, r) => {
      P.fill(hx("#1E2438"), 0, r); P.rect(0, 0, 64, 1, hx("#4A5276")); P.rect(0, 15, 64, 1, hx("#0E1220"));
      P.text("MAGNET TRAIN", 3, 2, hx("#F2C230")); P.text("FOR " + L.other, 3, 9, hx("#FFFFFF")); P.rect(56, 9, 2, 5, hx("#5DDC7A")); P.rect(59, 9, 2, 5, hx("#5DDC7A"));
    }, { fit: true, glow: 0.7 });
    // Flower beds: the city's flowers (GOLDENROD's blue, SAFFRON's red), in a low wooden box.
    reg("mtBed_" + k, 16, 16, (P, r) => {
      P.fill(hx("#4E8A3A"), 0.03, r);
      for (let i = 0; i < 9; i++) { const x = (i * 7 + 2) % 14 + 1, y = (i * 11 + 3) % 14 + 1; P.rect(x, y, 2, 2, hx(L.bed)); P.set(x, y, shadec(hx(L.bed), 0.35)); }
    });
  }

  // The train at the platform (x0 to x1 along the north wall, its middle at z), its doors open where they are.
  function train(R, x0, x1, z, L, k) {
    const G = R.geo, w = 1.1, y0 = 0.18;
    // The skirt over the bogies, the body, the window band, the stripe, the roof.
    G.box(x0 + 0.5, y0, z - w + 0.1, x1 - 0.5, y0 + 0.4, z + w - 0.1, { all: "mtSkirt", top: null });
    G.box(x0 + 1.2, y0 + 0.4, z - w, x1 - 1.2, y0 + 1.15, z + w, { s: "mtStripe_" + k, n: "mtStripe_" + k, all: "mtBody" });
    G.box(x0 + 1.2, y0 + 1.15, z - w, x1 - 1.2, y0 + 2.05, z + w, { s: "mtWin", n: "mtWin", all: "mtBody" });
    G.box(x0 + 1.2, y0 + 2.05, z - w + 0.12, x1 - 1.2, y0 + 2.3, z + w - 0.12, { all: "mtBody", top: "mtRoof" });
    // Its two noses, rounded down to the front in steps, the windscreen over each, a lamp each side.
    for (const [e, s] of [[x0, 1], [x1, -1]]) {
      const a = (u, v) => s > 0 ? [e + u, e + v] : [e - v, e - u];
      let [p, q] = a(0.6, 1.2); G.box(p, y0 + 0.4, z - w + 0.05, q, y0 + 1.9, z + w - 0.05, { all: "mtBody", [s > 0 ? "w" : "e"]: "mtBody" });
      [p, q] = a(0.6, 1.2); G.box(p, y0 + 1.15, z - w + 0.04, q, y0 + 1.75, z + w - 0.04, { s: "mtScreen", n: "mtScreen", top: null, bottom: null, e: null, w: null });
      [p, q] = a(0.15, 0.6); G.box(p, y0 + 0.4, z - w + 0.2, q, y0 + 1.3, z + w - 0.2, { all: "mtBody", [s > 0 ? "w" : "e"]: "mtScreen", top: "mtBody" });
      [p, q] = a(0.0, 0.15); G.box(p, y0 + 0.4, z - w + 0.35, q, y0 + 0.85, z + w - 0.35, { all: "mtBody", [s > 0 ? "w" : "e"]: "mtStripe_" + k });
      for (const dz of [-0.55, 0.55]) { [p, q] = a(-0.01, 0.02); G.box(p, y0 + 0.55, z + dz - 0.12, q, y0 + 0.75, z + dz + 0.12, "mtLamp"); }
      [p, q] = a(0.6, 1.2); G.box(p, y0 + 1.9, z - w + 0.2, q, y0 + 2.15, z + w - 0.2, { all: "mtBody", top: "mtRoof" });
    }
    // Its doors on the platform side (open: dark), two a car, at the train's doors in Crystal (steps 6 and 11).
    for (const dx of [5.6, 10.6, 14.6]) if (dx > x0 + 1.5 && dx < x1 - 1.5) G.box(dx, y0 + 0.42, z + w, dx + 1.0, y0 + 2.0, z + w + 0.01, { s: "mtDoor", all: null });
    R.solid(x0, z - w - 0.2, x1, z + w + 0.2);
    R.act([x0 + 1, z + w, x1 - 1, z + w + 0.6], { label: "Look", lines: ["The MAGNET TRAIN, its doors open, waiting to leave for " + L.other + ".", "Talk to the OFFICER at the gate to ride."] });
    R.item("The MAGNET TRAIN", "A white two-ended train, its windows in a dark band, a deep blue stripe with a gold line along it; its doors open onto the platform.");
  }

  function station(id, k) {
    const L = LOOK[k], W = 20, D = 18;
    const R = new Room(id, { name: "Magnet Train Station", floor: k === "goldenrod" ? "Goldenrod City" : "Saffron City", W, D, H: 4.2, outsideSize: "16 by 20" });
    const G = R.geo;
    shell(R, { paper: "mtPaper_" + k, floor: "mtFloor_" + k, door: [9, 11], entry: true, skirt: "mtTrim_" + k, ledge: "mtBand_" + k, ledgeTop: "mtTrim_" + k });
    // The band round the walls, its trim, and the trim under the ceiling.
    band(R, 0.24, "mtBand_" + k, 0.9); band(R, 1.14, "mtTrim_" + k, 0.1);
    // ----- The platform and the line, along the back -----
    // The line: its bed sunk a little into the floor the width of the room, two rails on it.
    G.box(0, 0, 0.1, W, 0.02, 3.4, { top: "mtBed", all: null });
    for (const z of [1.15, 2.35]) G.box(0, 0.02, z, W, 0.12, z + 0.1, "mtRail");
    // The platform: from the line to the rail across the hall, its yellow edge by the line.
    G.box(0, 0, 3.4, W, 0.05, 6.8, { top: "mtPlatform", all: "mtPlatform" });
    G.box(0, 0.05, 3.4, W, 0.06, 3.8, { top: "mtEdge", all: null });
    R.item("Platform", "Pale concrete slabs, the yellow line along its edge by the train.");
    // The back wall over the line: tall windows on the city between white piers, as Crystal's.
    for (let x = 0.6; x < W - 1; x += 4) {
      G.box(x, 0.8, 0.0, x + 2.8, 3.6, 0.03, { s: "window", all: null });
      G.box(x - 0.2, 0.6, 0, x + 3.0, 0.8, 0.3, "mtTrim_" + k);
    }
    for (let x = 0.4; x < W; x += 4) G.box(x - 0.2, 0, 0, x + 0.2, R.H, 0.35, { all: "mtPost", top: "cap" });
    R.act([0, 0, W, 0.6], { label: "Look", lines: L.win });
    train(R, 1.2, W - 1.2, 1.75, L, k);
    // ----- The rail across the hall and the gate in it -----
    const zr = 6.8, g0 = 8.8, g1 = 11.2;
    for (const [a, b] of [[0, g0], [g1, W]]) {
      G.box(a, 0, zr, b, 0.12, zr + 0.2, "mtPost");
      G.box(a, 0.12, zr + 0.08, b, 1.05, zr + 0.12, { s: "mtGlass", n: "mtGlass", all: null });
      G.box(a, 1.05, zr, b, 1.15, zr + 0.2, "mtPost");
      for (let x = a + 0.05; x < b; x += 2) G.box(x, 0, zr, x + 0.12, 1.15, zr + 0.2, "mtPost");
    }
    // The gate's two posts, a light on each (tap your PASS), and the board over it.
    for (const x of [g0, g1 - 0.5]) G.box(x, 0, zr - 0.3, x + 0.5, 1.2, zr + 0.5, { all: "mtPost", top: "mtPostTop" });
    G.box(g0 - 0.6, 2.7, zr + 0.05, g1 + 0.6, 3.5, zr + 0.15, { s: "mtBoard_" + k, n: "mtBoard_" + k, all: "mtSkirt" });
    for (const x of [g0 - 0.3, g1 + 0.2]) G.box(x, 3.5, zr + 0.07, x + 0.06, R.H, zr + 0.13, "steel");
    R.solid(0, zr - 0.3, W, zr + 0.5);
    R.act([g0, zr + 0.5, g1, zr + 1.1], { label: "Look", lines: ["The ticket gate. The OFFICER will see you through to the train."] });
    R.item("Ticket gate", "Two steel posts, a green light on each where you'd tap your rail PASS; the board over them shows the next train.");
    // ----- The waiting hall -----
    // Two rows of seats facing the platform, on the west side (as Crystal's).
    for (const z of [9.2, 11.2]) for (const x of [2.6, 3.65, 4.7, 5.75]) armchair(R, x, z, "n", L.seat);
    // Flower beds down the west wall by the door, as Crystal's.
    for (const [z0, z1] of [[12.8, 14.4], [14.8, 15.8]]) {
      G.box(0.1, 0, z0, 1.1, 0.5, z1, { all: "walnut", top: "mtBed_" + k });
      R.solid(0.1, z0, 1.1, z1);
    }
    R.item("Flower beds", k === "goldenrod" ? "Low wooden boxes of blue flowers." : "Low wooden boxes of red flowers.");
    // Ticket machines along the east wall, facing in.
    for (const z of [8.0, 9.4, 10.8]) {
      G.box(W - 0.95, 0, z, W - 0.05, 1.8, z + 1.1, { w: "mtMachine", all: "mtPost", top: "mtRoof" });
      R.solid(W - 0.95, z, W - 0.05, z + 1.1);
    }
    R.act([W - 1.6, 8.0, W - 0.9, 11.9], { label: "Look", lines: ["A ticket machine.", "MAGNET TRAIN: " + L.city + " to " + L.other + ". Please show your rail PASS at the gate."] });
    R.item("Ticket machines", "Three machines along the east wall, each a screen, its buttons and a slot.");
    // Windows down the west wall over the seats, plants either side of the door.
    crossWindow(R, "w", 8.4, 1.6, 2.4, 1.2, L.win);
    plant(R, 7.4, R.Dm - 0.9, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    plant(R, 12.6, R.Dm - 0.9, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    slimPlant(R, W - 1.0, R.Dm - 1.0, 1.0);
    bin(R, W - 0.9, 12.6);
    doormat(R, 9, 11, { to: "town", spawn: { x: 10.0, z: D - 2.0, yaw: FACE.s }, mat: "matGate" });
    R.spawn = { x: 10.0, z: D - 1.5, yaw: FACE.n };
    return R;
  }

  BUILD.goldenrod_magnet_train_station = function () {
    const R = station("goldenrod_magnet_train_station", "goldenrod");
    R.npcs.push({ name: "Officer", key: "goldenrod_magnet_train_station_officer", x: 10.0, z: 7.7, yaw: FACE.s, look: { skin: "officer_johto" },
      talk: () => [{ text: "We'll soon depart for SAFFRON." }, { text: "Are you coming aboard?" }] });
    R.npcs.push({ name: "President", key: "goldenrod_magnet_train_station_president", x: 13.5, z: 13.2, yaw: FACE.w, look: { skin: "gentleman" },
      talk: () => [{ text: "I'm the PRESIDENT." }, { text: "My dream was to build a train that is faster than any POKéMON." }, { text: "It really brings JOHTO much closer to KANTO." }] });
    return R;
  };
  BUILD.saffron_magnet_train_station = function () {
    const R = station("saffron_magnet_train_station", "saffron");
    R.npcs.push({ name: "Officer", key: "saffron_magnet_train_station_officer", x: 10.0, z: 7.7, yaw: FACE.s, look: { skin: "officer_m" },
      talk: () => [{ text: "We'll soon depart for GOLDENROD." }, { text: "Are you coming on board?" }] });
    R.npcs.push({ name: "Gym Guide", key: "saffron_magnet_train_station_gym_guide", x: 12.0, z: 13.0, yaw: FACE.w, look: { skin: "gym_guide" },
      talk: () => [{ text: "The MAGNET TRAIN is a super-modern rail liner that uses electricity and magnets to attain incredible speed." }] });
    R.npcs.push({ name: "Lass", key: "saffron_magnet_train_station_lass", x: 6.8, z: 12.6, yaw: FACE.n, look: { skin: "lass" },
      talk: () => [{ text: "Hi. Do you have a rail PASS? I have one." }, { text: "All the people in SAFFRON who ride the MAGNET TRAIN have PASSES." }] });
    return R;
  };
})();
