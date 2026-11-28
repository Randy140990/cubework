// ---------- 0.77.0 (John's 28 Nov 12:53): the gate between ROUTE 35 and the NATIONAL PARK, as HeartGold's ----------
// "So is that the root 35 gate? You've got it like a normal gate when it's a massive diagonal type one where you still
// come in from the bottom and then you go across to the left and then up". HeartGold's ROUTE_35_NATIONAL_PARK
// (_POKEATHLON) GATEHOUSE (pret/pokeheartgold's R35R0201, PokéMaps' picture of it): a wide hall, in at the bottom on
// the right (its (25, 11)), out at the top on the right to the PARK (its (25, 2)) and at the top on the left (its
// (4, 2): HeartGold's way to the POKéATHLON DOME), as John's building has its doors. A big red-brown carpet with its
// diamonds across the middle; the contest's blue desk, a U open to the back wall, its staff in it, a screen on it;
// posters on the back wall; pale stone tiles round the edges; pink-potted plants in the corners, a kiosk by the
// right-hand door. Its people where HeartGold has them, with its words. A tile a block; HeartGold's z 2 is the
// room's z 0.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const fill = (P, r, c, k) => { for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, shadec(hx(c), (r() - 0.5) * k)); };
  reg("npTile", 16, 16, (P, r) => {
    fill(P, r, "#DADDD6", 0.015);
    P.rect(0, 0, 16, 1, hx("#C2C6BE")); P.rect(0, 0, 1, 16, hx("#C2C6BE")); P.rect(1, 1, 15, 1, hx("#ECEEE9")); P.rect(1, 1, 1, 15, hx("#ECEEE9"));
  });
  reg("npDesk", 16, 16, (P, r) => { fill(P, r, "#4F86D8", 0.015); P.rect(0, 0, 16, 2, hx("#78A6EC")); P.rect(0, 14, 16, 2, hx("#3A68B4")); });
  reg("npDeskTop", 16, 16, (P, r) => { fill(P, r, "#5E93E2", 0.012); P.rect(0, 0, 16, 1, hx("#8DB6F0")); P.rect(0, 15, 16, 1, hx("#4374C4")); });
  reg("npTrim", 16, 16, (P, r) => { fill(P, r, "#F2F5F7", 0.006); });
  // The carpet's own weave: red-brown, a small orange diamond in each block (HeartGold's), the edge darker.
  reg("npCarpet", 16, 16, (P, r) => {
    fill(P, r, "#8C3A2B", 0.03);
    for (let i = 0; i < 4; i++) { P.rect(8 - i, 5 + i, 2 * i, 1, hx("#D9783A")); P.rect(8 - i, 11 - i, 2 * i, 1, hx("#D9783A")); }
    P.rect(7, 4, 2, 1, hx("#D9783A"));
  });
  reg("npCarpetEdge", 16, 16, (P, r) => { fill(P, r, "#6A2A20", 0.03); P.rect(0, 7, 16, 2, hx("#B85A30")); });
  reg("npPoster", 16, 24, (P, r) => {
    fill(P, r, "#F29A3A", 0.02); P.rect(1, 1, 14, 22, hx("#FFF3DC"));
    P.rect(3, 3, 10, 9, hx("#7CC05A")); P.rect(5, 6, 6, 4, hx("#E2C040")); P.rect(3, 14, 10, 2, hx("#D2582E")); P.rect(3, 18, 7, 1, hx("#8A6A50"));
  }, { fit: true });
  reg("npKiosk", 16, 16, (P, r) => { fill(P, r, "#2E5A44", 0.02); P.rect(2, 2, 12, 9, hx("#7FE0B0")); P.rect(3, 12, 10, 2, hx("#F2F5F7")); }, { fit: true, glow: 0.3 });
  reg("potPinkNp", 16, 16, (P, r) => { fill(P, r, "#E487B4", 0.02); P.rect(0, 0, 16, 2, hx("#F2A8CB")); P.rect(0, 14, 16, 2, hx("#C46A96")); });

  BUILD.route_35_national_park_gate = function () {
    const W = 30, D = 12;
    const R = new Room("route_35_national_park_gate", { name: "National Park gate", floor: "Route 35", W, D, H: 4, outsideSize: "42 by 7" });
    const G = R.geo;
    shell(R, { paper: "paperGateLG", floor: "npTile", door: [24, 27], entry: true, skirt: "gateWhite", ledge: "gateBlue", ledgeTop: "gateWhite" });
    band(R, 0.24, "gateBlue", 1.1); band(R, 1.34, "gateWhite", 0.1);
    // The two ways out at the back: the PARK's on the right, over the way in; the other on the left.
    passageN(R, 3, 6, { to: "town", mat: "matGateN", frame: "paint", opening: "gateTunnel", tunnel: true });
    passageN(R, 24, 27, { to: "town", mat: "matGateN", frame: "paint", opening: "gateTunnel", tunnel: true });
    // The carpet, round the desk.
    // (Its weave a block at a time, a darker band round its edge.)
    for (const [x0, z0, x1, z1] of [[3, 1, 17, 9], [17, 6, 23, 9], [23, 2, 27, 9]]) G.box(x0, 0, z0, x1, 0.04, z1, { top: "npCarpet", all: "npCarpetEdge", bottom: null });
    for (const [x0, z0, x1, z1] of [[3, 8.8, 27, 9], [3, 1, 3.2, 9], [26.8, 2, 27, 9], [3, 1, 17, 1.2], [23, 2, 27, 2.2]]) G.box(x0, 0.04, z0, x1, 0.045, z1, { top: "npCarpetEdge", all: null });
    R.item("Carpet", "A big red-brown carpet across the hall, small orange diamonds on it, a darker edge.");
    // Posters on the back wall over the carpet (the contest's and the games').
    framed(R, 8.6, 1.5, 1.2, 1.8, "npPoster", "n", { frameMat: "gateWhite", act: { label: "Read", lines: ["A poster: the BUG-CATCHING CONTEST, in the NATIONAL PARK. TUESDAYS, THURSDAYS AND SATURDAYS!"] } });
    framed(R, 12.6, 1.5, 1.2, 1.8, "npPoster", "n", { frameMat: "gateWhite", act: { label: "Read", lines: ["A poster: play with your POKéMON, trust your POKéMON, believe in your POKéMON!"] } });
    // The contest's desk: a U open to the back wall, blue, white-topped, a screen on its right arm, a sign on its front.
    const desk = (x0, z0, x1, z1) => {
      G.box(x0, 0, z0, x1, 0.95, z1, { all: "npDesk", top: "npDeskTop" });
      G.box(x0 - 0.04, 0.95, z0 - 0.04, x1 + 0.04, 1.0, z1 + 0.04, "npTrim");
      G.box(x0 + 0.02, 1.0, z0 + 0.02, x1 - 0.02, 1.02, z1 - 0.02, { top: "npDeskTop", all: "npDesk" });
      R.solid(x0, z0, x1, z1);
    };
    desk(17, 0.3, 18, 4.2); desk(22, 0.3, 23, 4.2); desk(17, 4.2, 23, 5.2);
    G.box(22.15, 1.02, 2.6, 22.85, 1.55, 3.4, { all: "greyPlastic", w: "labPC" }); G.box(22.4, 1.02, 2.9, 22.6, 1.1, 3.1, "greyPlastic");
    G.box(19.6, 1.02, 4.55, 20.4, 1.4, 4.75, { all: "npTrim", s: "sign" });
    R.act([16.8, 4.0, 23.2, 6.2], { label: "Look", lines: ["The contest's desk. A sign on it: BUG-CATCHING CONTEST, SIGN UP HERE."] });
    R.item("Contest desk", "A blue U-shaped desk with a white top, open to the back wall, a screen on its right arm, a sign on its front.");
    // The kiosk by the right-hand way out, and the plants in pink pots.
    G.box(28.0, 0, 2.6, 29.4, 1.1, 3.6, { all: "gateWhite", n: "npKiosk", s: "npKiosk" }); R.solid(28.0, 2.6, 29.4, 3.6);
    R.act([27.6, 2.2, 29.8, 4.0], { label: "Look", lines: ["A screen with the NATIONAL PARK's map on it. The PARK is through the door on the right."] });
    for (const [x, z] of [[1.4, 1.4], [1.4, 9.4], [28.6, 9.4]]) plant(R, x, z, 1.0, { pot: "potPinkNp", kind: "leafy", lines: ["A leafy plant in a pink pot."] });
    doormat(R, 24, 27, { to: "town", spawn: { x: 25.5, z: D - 2.0, yaw: FACE.s }, mat: "matGate" });
    // HeartGold's people (its x, z less 2): the contest's staff at the desk, and the trainers waiting about.
    const npc = (name, key, x, z, yaw, skin, lines) => R.npcs.push({ name, key, x, z, yaw, look: { skin }, talk: () => lines.map(text => ({ text })) });
    npc("Contest Staff", "route_35_national_park_gate_p0", 20.0, 2.2, FACE.s, "link_receptionist",
      ["The BUG-CATCHING CONTEST is held on Tuesday, Thursday and Saturday.", "Not only do you earn a prize just for taking part, you also get to keep the BUG-type POKéMON you catch."]);
    npc("Staff", "route_35_national_park_gate_p1", 18.9, 3.1, FACE.s, "clerk",
      ["The NATIONAL PARK is through the door on the right. Enjoy your walk!"]);
    npc("Black Belt", "route_35_national_park_gate_p2", 9.0, 2.2, FACE.s, "black_belt",
      ["Arrgh! I can't just sit here and wait!", "I'll go on an around-JOHTO run!"]);
    npc("Lass", "route_35_national_park_gate_p3", 6.0, 6.0, FACE.e, "lass",
      ["I wonder if I can join in too. It's only running with POKéMON, right?"]);
    npc("Gentleman", "route_35_national_park_gate_p4", 10.0, 7.0, FACE.n, "middle_aged_man",
      ["I heard about the BUG-CATCHING CONTEST, where BUG-type lovers gather from all over the region.", "...Me? I cannot take BUG types..."]);
    npc("Builder", "route_35_national_park_gate_p5", 19.0, 7.0, FACE.w, "builder",
      ["Hey, hey! Watch yourself!", "Tap tap, ding ding! We're in a big hurry here."]);
    npc("Youngster", "route_35_national_park_gate_p6", 27.6, 5.6, FACE.w, "youngster",
      ["I've been waiting for this moment! Behold my friendship with my POKéMON!"]);
    R.spawn = { x: 25.5, z: D - 1.5, yaw: FACE.n };
    return R;
  };
})();
