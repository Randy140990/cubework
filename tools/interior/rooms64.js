// ---------- 0.60 (John, 16:14 to 16:28): the rest of KANTO's insides ----------
// "you can do the elite4, you can do the Celadon gym ... Bill's Sea Cottage ... Which will need sorting ... Viridian Gym
// ... the rest of Celadon ... Team Rocket's hideout ... Pokemon Tower, 7 floors ... the S.S. Anne ... interiors only".
// Each room is FireRed's own map, a tile a block (tools/kanto60.py reads its layout, warps, signs and people into
// kanto60_data.js), drawn here in the looks we've agreed: Let's Go's for the TOWER (checked green and cream floors, purple
// mist, rows of gravestones, pale stone pillars and dark arched doors) and the GYMS (ERIKA's hedges and flower beds and
// brick paths; GIOVANNI's maze of low walls on teal tiles, its spinner arrows). The spinners: step on an arrow and you
// slide that way till a yellow stop square or a wall (the mod moves you; the viewer shows them).
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const noise = (P, r, c, k) => P.fill(hx(c), k, r);
  const K = K60;
  const have = new Set(PLACES.map(p => p.id));
  for (const [id, F] of Object.entries(K)) if (!have.has(id)) PLACES.push({ id, name: F.name, floor: F.floor, group: F.group });

  // ===================================== materials =====================================
  const tiles = (k, a, b, line, step = 8) => reg(k, 16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx(((x / step | 0) + (y / step | 0)) % 2 ? a : b), (r() - 0.5) * 0.025));
    if (line) { for (let i = 0; i < 16; i += step) { P.rect(i, 0, 1, 16, hx(line)); P.rect(0, i, 16, 1, hx(line)); } }
  });
  // GIOVANNI's GYM: teal tiles in a fine grid, the maze's low walls (stone tops, a terracotta face), the dais.
  reg("vgFloor", 16, 16, (P, r) => { noise(P, r, "#8FD0C4", 0.015); for (const i of [0, 8]) { P.rect(i, 0, 1, 16, hx("#74B8AC")); P.rect(0, i, 16, 1, hx("#74B8AC")); } P.rect(1, 1, 6, 1, hx("#A8E0D6")); P.rect(9, 9, 6, 1, hx("#A8E0D6")); });
  reg("vgRailTop", 16, 16, (P, r) => { noise(P, r, "#B8B8A8", 0.02); P.rect(0, 0, 16, 2, hx("#D8D8C8")); P.rect(0, 14, 16, 2, hx("#8C8C7C")); });
  reg("vgRailFace", 16, 16, (P, r) => { noise(P, r, "#D88A62", 0.02); P.rect(0, 0, 16, 2, hx("#F0A880")); P.rect(0, 14, 16, 2, hx("#A85E3E")); });
  reg("vgWall", 16, 16, (P, r) => { noise(P, r, "#EADFC8", 0.01); P.rect(0, 12, 16, 4, hx("#D88A62")); P.rect(0, 11, 16, 1, hx("#B87250")); });
  reg("vgDais", 16, 16, (P, r) => { noise(P, r, "#E8E070", 0.02); for (let i = 0; i < 16; i += 4) P.line(i, 0, i + 6, 6, hx("#F8F4A8")); P.rect(0, 0, 16, 1, hx("#B8B040")); });
  reg("vgDaisSide", 16, 16, (P, r) => { noise(P, r, "#8A9AA8", 0.02); P.rect(0, 0, 16, 2, hx("#B8C8D4")); });
  reg("vgMat", 16, 16, (P, r) => { noise(P, r, "#5AA0E0", 0.02); P.rect(0, 0, 16, 2, hx("#3C78B8")); P.rect(0, 14, 16, 2, hx("#3C78B8")); });
  // The spinner tiles: a dark plate with green chevrons (the way you'll slide), and the yellow stop squares.
  for (const [k, dir] of [["spinE", "e"], ["spinW", "w"], ["spinN", "n"], ["spinS", "s"]]) reg(k, 16, 16, (P, r) => {
    noise(P, r, "#3C4654", 0.02); P.rect(0, 0, 16, 1, hx("#E8ECF0")); P.rect(0, 15, 16, 1, hx("#E8ECF0")); P.rect(0, 0, 1, 16, hx("#E8ECF0")); P.rect(15, 0, 1, 16, hx("#E8ECF0"));
    for (const o of [3, 8]) for (let k2 = 0; k2 < 4; k2++) {
      const pts = { e: [[o + k2, 4 + k2], [o + k2, 11 - k2]], w: [[12 - o - k2, 4 + k2], [12 - o - k2, 11 - k2]], s: [[4 + k2, o + k2], [11 - k2, o + k2]], n: [[4 + k2, 12 - o - k2], [11 - k2, 12 - o - k2]] }[dir];
      for (const [x, y] of pts) P.rect(x, y, 2, 2, hx("#5CE07A"));
    }
  }, { glow: 0.25 });
  reg("spinStop", 16, 16, (P, r) => { noise(P, r, "#F4E89A", 0.01); P.rect(0, 0, 16, 1, hx("#C8B850")); P.rect(0, 15, 16, 1, hx("#C8B850")); P.rect(0, 0, 1, 16, hx("#C8B850")); P.rect(15, 0, 1, 16, hx("#C8B850")); for (let y = 3; y < 13; y += 3) for (let x = 3; x < 13; x += 3) P.rect(x, y, 2, 2, hx("#E0CC60")); });
  // ERIKA's GYM: soft lawn, Let's Go's terracotta brick paths, flower beds, clipped hedges with blossom.
  reg("cgLawn", 16, 16, (P, r) => { noise(P, r, "#7CC86A", 0.04); for (let i = 0; i < 10; i++) P.set((i * 7 + 3) % 16, (i * 11 + 5) % 16, hx("#9ADC84")); });
  reg("cgBrick", 16, 16, (P, r) => { noise(P, r, "#D89A6A", 0.03); for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx("#B87A50")); for (let x = (y % 8 ? 4 : 0); x < 16; x += 8) P.rect(x, y, 1, 4, hx("#B87A50")); } });
  reg("cgBed", 16, 16, (P, r) => { noise(P, r, "#6A4A30", 0.05); for (let i = 0; i < 14; i++) { const x = (i * 5 + 2) % 15, y = (i * 9 + 3) % 15; P.rect(x, y, 2, 2, hx(["#F06A8A", "#F8F0F0", "#F8C840", "#E84050"][i % 4])); } });
  reg("cgHedge", 16, 16, (P, r) => { noise(P, r, "#3E8A3A", 0.06); for (let i = 0; i < 22; i++) P.set((i * 7 + 1) % 16, (i * 5 + 3) % 16, hx("#5AAA4E")); for (let i = 0; i < 4; i++) P.rect((i * 9 + 3) % 14, (i * 7 + 2) % 14, 2, 2, hx(i % 2 ? "#F4A0B8" : "#F8F4F4")); });
  reg("cgHedgeTop", 16, 16, (P, r) => { noise(P, r, "#4C9A44", 0.06); for (let i = 0; i < 6; i++) P.rect((i * 5 + 1) % 14, (i * 9 + 4) % 14, 2, 2, hx(["#F06A8A", "#F8F4F4", "#F8C840"][i % 3])); });
  reg("cgWall", 16, 16, (P, r) => { noise(P, r, "#F4EEDC", 0.01); P.rect(0, 13, 16, 3, hx("#9AC07A")); P.rect(0, 12, 16, 1, hx("#7AA05A")); });
  reg("cgPane", 16, 32, (P, r) => { P.fill(hx("#F4EEDC"), 0, r); P.rect(2, 2, 12, 28, hx("#7A9A6A")); for (let y = 3; y < 29; y++) for (let x = 3; x < 13; x++) if (!(y < 8 && Math.hypot(x - 7.5, y - 8) > 4.6)) P.set(x, y, hx(x === 7 || x === 8 || y === 17 ? "#7A9A6A" : "#BFE8F8")); }, { fit: true, glow: 0.2 });
  // The POKéMON LEAGUE: each room's own floor (LORELEI's ice, BRUNO's sandstone, AGATHA's violet, LANCE's teal, the
  // CHAMPION's slate, the HALL's green), its battle court marked out on it, the pillars.
  tiles("lgIce", "#BCE4F4", "#A4D4EC", "#8CC0DC");
  tiles("lgSand", "#E8D49A", "#DCC688", "#C8B074");
  tiles("lgViolet", "#B898E0", "#A884D4", "#9070C0");
  tiles("lgTeal", "#7CC8BC", "#68B8AC", "#54A498");
  tiles("lgSlate", "#B4C0CC", "#A4B0BC", "#8E9AA8");
  tiles("lgGreen", "#8CC894", "#7CBC84", "#68A870");
  reg("lgCourt", 64, 64, (P, r) => {
    for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) P.set(x, y, [0, 0, 0], 0);
    const W = [255, 255, 255];
    for (let i = 2; i < 62; i++) { P.set(i, 2, W); P.set(i, 61, W); P.set(2, i, W); P.set(61, i, W); P.set(i, 32, W); }
    for (let a = 0; a < 360; a += 1) { const t = a * Math.PI / 180; P.set(Math.round(32 + 11 * Math.cos(t)), Math.round(32 + 11 * Math.sin(t)), W); P.set(Math.round(32 + 4 * Math.cos(t)), Math.round(32 + 4 * Math.sin(t)), W); }
  }, { fit: true, cut: true });
  reg("lgIcePillar", 16, 16, (P, r) => { noise(P, r, "#CDEEFA", 0.02); for (let i = 0; i < 16; i += 5) P.line(i, 0, i - 6, 15, hx("#F4FCFF")); P.rect(0, 15, 16, 1, hx("#8CC0DC")); }, { glow: 0.2 });
  reg("lgRock", 16, 16, (P, r) => { noise(P, r, "#9C9488", 0.06); P.rect(2, 3, 6, 4, hx("#B4ACA0")); P.rect(9, 9, 5, 4, hx("#B4ACA0")); P.rect(0, 15, 16, 1, hx("#6C6458")); });
  reg("lgGilt", 16, 16, (P, r) => { noise(P, r, "#7A4AA8", 0.02); P.rect(0, 0, 16, 2, hx("#E8C860")); P.rect(0, 14, 16, 2, hx("#E8C860")); P.rect(6, 2, 4, 12, hx("#9A6AC8")); });
  reg("lgHorn", 16, 16, (P, r) => { noise(P, r, "#E8E4DC", 0.02); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#C8C0B4")); });
  reg("lgHornBase", 16, 16, (P, r) => { noise(P, r, "#C89848", 0.02); P.rect(0, 0, 16, 2, hx("#E8C068")); P.rect(0, 13, 16, 3, hx("#8A6428")); });
  reg("lgWall", 16, 16, (P, r) => { noise(P, r, "#7A3A2A", 0.02); for (let x = 0; x < 16; x += 8) P.rect(x, 0, 1, 16, hx("#5A2A1E")); P.rect(0, 0, 16, 2, hx("#E8C060")); P.rect(7, 7, 2, 2, hx("#F0D078")); });
  reg("lgWallHi", 16, 16, (P, r) => { noise(P, r, "#F0E4C8", 0.01); P.rect(0, 15, 16, 1, hx("#C8B890")); });
  reg("lgDoor", 16, 32, (P, r) => { noise(P, r, "#5A2A1E", 0.02); P.rect(1, 1, 14, 31, hx("#C89848")); P.rect(2, 2, 12, 29, hx("#8A3A2A")); P.rect(7, 2, 2, 29, hx("#C89848")); P.rect(4, 6, 3, 3, hx("#F0D078")); P.rect(9, 6, 3, 3, hx("#F0D078")); }, { fit: true });
  reg("lgDoorOpen", 16, 32, (P, r) => { P.fill(hx("#2A1A18"), 0.02, r); P.rect(0, 0, 16, 2, hx("#C89848")); }, { fit: true });
  reg("lgFame", 32, 32, (P, r) => {
    noise(P, r, "#D8E0E8", 0.01); P.rect(2, 2, 28, 18, hx("#2A3440")); P.rect(4, 4, 24, 14, hx("#5AC8F0"));
    for (let i = 0; i < 6; i++) P.rect(5 + i * 4, 9, 3, 3, hx("#F8F0A0"));
    P.rect(4, 22, 24, 6, hx("#A8B4C0")); for (let x = 6; x < 27; x += 4) P.rect(x, 24, 2, 2, hx(["#E84848", "#48C868", "#F8D040"][(x / 4 | 0) % 3]));
  }, { fit: true, glow: 0.5 });
  // The SEA COTTAGE (FireRed's): honey boards, cream walls, the teleporter pods' glass and steel, the pipe.
  reg("scBoards", 16, 16, (P, r) => { noise(P, r, "#C8A060", 0.03); for (const y of [0, 4, 8, 12]) P.rect(0, y, 16, 1, hx("#A07840")); P.rect(5, 1, 1, 3, hx("#B08A50")); P.rect(11, 9, 1, 3, hx("#B08A50")); });
  reg("scWall", 16, 16, (P, r) => { noise(P, r, "#F4ECD4", 0.01); P.rect(0, 12, 16, 4, hx("#8AB4D8")); P.rect(0, 11, 16, 1, hx("#6A94B8")); });
  reg("scPod", 16, 16, (P, r) => { noise(P, r, "#8CCC9C", 0.02); P.rect(0, 0, 16, 2, hx("#B8E8C4")); for (let x = 3; x < 16; x += 5) P.rect(x, 2, 1, 14, hx("#6AAC7C")); });
  reg("scPipe", 16, 16, (P, r) => { noise(P, r, "#78D890", 0.01); P.rect(0, 0, 16, 3, hx("#B4F4C4")); P.rect(0, 13, 16, 3, hx("#4AA860")); }, { glow: 0.2 });
  reg("scCrate", 16, 16, (P, r) => { noise(P, r, "#F0C838", 0.02); P.rect(0, 0, 16, 1, hx("#FFE070")); P.rect(0, 15, 16, 1, hx("#B89020")); P.rect(7, 0, 2, 16, hx("#D8A828")); });
  // The CELADON MANSION, the hotel, the restaurant, the houses: warm boards, papered walls, carpets.
  reg("cmBoards", 16, 16, (P, r) => { noise(P, r, "#D4A870", 0.03); for (const y of [0, 5, 10]) P.rect(0, y, 16, 1, hx("#B08850")); P.rect(7, 1, 1, 4, hx("#C09860")); P.rect(3, 11, 1, 4, hx("#C09860")); });
  reg("cmPaper", 16, 16, (P, r) => { noise(P, r, "#F0E8D0", 0.01); for (let x = 2; x < 16; x += 6) P.rect(x, 0, 1, 16, hx("#E4D8BC")); P.rect(0, 13, 16, 3, hx("#B88A5A")); });
  reg("cmCarpetBlue", 16, 16, (P, r) => { noise(P, r, "#5878C0", 0.03); for (let y = 2; y < 16; y += 6) for (let x = 2; x < 16; x += 6) P.set(x, y, hx("#7898D8")); });
  reg("cmCarpetRed", 16, 16, (P, r) => { noise(P, r, "#C04848", 0.03); P.rect(0, 0, 16, 1, hx("#E8C060")); P.rect(0, 15, 16, 1, hx("#E8C060")); });
  tiles("cmDinerTile", "#F4F0E8", "#E04848", null);
  // The ROCKET HIDEOUT: dark olive tiles, grey-green panelled walls.
  reg("rhFloor", 16, 16, (P, r) => { noise(P, r, "#8C9470", 0.03); P.rect(0, 0, 16, 1, hx("#6C7454")); P.rect(0, 0, 1, 16, hx("#6C7454")); P.rect(0, 8, 16, 1, hx("#7C8460")); P.rect(8, 0, 1, 16, hx("#7C8460")); });
  reg("rhWall", 16, 16, (P, r) => { noise(P, r, "#7C9090", 0.02); P.rect(0, 0, 16, 1, hx("#A0B4B4")); for (const y of [6, 12]) P.rect(0, y, 16, 1, hx("#5C7070")); P.rect(3, 2, 2, 2, hx("#C04040")); });
  reg("rhCap", 16, 16, (P, r) => { noise(P, r, "#3C4848", 0.02); P.rect(0, 0, 16, 1, hx("#5C6868")); });
  // The POKéMON TOWER (Let's Go's): checked green and cream tiles, a dark tile here and there, pale stone walls with dark
  // arched doors, the gravestones (dark, a POKé BALL mark), flowers, the purple mist.
  reg("twFloor", 32, 32, (P, r) => {
    for (let ty = 0; ty < 4; ty++) for (let tx = 0; tx < 4; tx++) {
      const dark = ((tx * 5 + ty * 3) % 7) === 0, base = dark ? "#5C7A64" : ((tx + ty) % 2 ? "#9CC4A0" : "#E4E6D2");
      for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(hx(base), (x === 0 || y === 0 ? -0.12 : 0) + (r() - 0.5) * 0.03));
    }
  });
  reg("twWall", 16, 16, (P, r) => { noise(P, r, "#D8D4C8", 0.02); for (let y = 0; y < 16; y += 5) P.rect(0, y, 16, 1, hx("#B8B4A8")); P.rect(0, 15, 16, 1, hx("#9A968A")); });
  reg("twArch", 16, 32, (P, r) => {
    P.fill(hx("#D8D4C8"), 0.02, r);
    for (let y = 4; y < 32; y++) for (let x = 2; x < 14; x++) if (!(y < 10 && Math.hypot(x - 7.5, y - 10) > 5.6)) P.set(x, y, hx(x === 7 || x === 8 ? "#2C2A3C" : "#3E3A54"));
    for (let y = 12; y < 30; y += 6) { P.rect(3, y, 4, 1, hx("#55506E")); P.rect(9, y, 4, 1, hx("#55506E")); }
  }, { fit: true });
  reg("twGrave", 16, 16, (P, r) => { noise(P, r, "#54545E", 0.03); P.rect(0, 0, 16, 1, hx("#7A7A86")); P.rect(5, 3, 6, 6, hx("#8A8A96")); P.rect(6, 4, 4, 4, hx("#54545E")); P.rect(5, 6, 6, 1, hx("#8A8A96")); for (const y of [11, 13]) P.rect(3, y, 10, 1, hx("#76767F")); });
  reg("twGraveTop", 16, 16, (P, r) => { noise(P, r, "#62626C", 0.02); });
  reg("twMist", 32, 32, (P, r) => { for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) P.set(x, y, [170, 120, 220], r() < 0.05 + 0.04 * Math.sin((x + y) / 5) ? 1 : 0); }, { cut: true });
  reg("twCarpet", 16, 16, (P, r) => { noise(P, r, "#9C4A48", 0.03); P.rect(0, 0, 16, 1, hx("#B86A60")); });
  reg("twSofa", 16, 16, (P, r) => { noise(P, r, "#6A4A86", 0.02); P.rect(0, 0, 16, 2, hx("#8A6AA6")); P.rect(0, 14, 16, 2, hx("#4A3066")); });
  // The S.S.ANNE: deck boards, cream walls with a blue band and portholes, cabin carpet.
  reg("ssBoards", 16, 16, (P, r) => { noise(P, r, "#C89C64", 0.03); for (const y of [0, 4, 8, 12]) P.rect(0, y, 16, 1, hx("#9C7444")); for (const x of [3, 11]) P.rect(x, 1, 1, 3, hx("#A88050")); });
  reg("ssWall", 16, 16, (P, r) => { noise(P, r, "#F4F0E4", 0.01); P.rect(0, 11, 16, 2, hx("#3A68B8")); P.rect(0, 13, 16, 3, hx("#E8E0CC")); });
  reg("ssPort", 16, 16, (P, r) => { P.fill(hx("#F4F0E4"), 0.01, r); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.hypot(x - 7.5, y - 7.5); if (d < 6.5) P.set(x, y, hx(d > 5 ? "#C8A040" : d > 2.5 ? "#7CC4EC" : "#A8DCF8")); } }, { fit: true, glow: 0.2 });
  reg("ssCabin", 16, 16, (P, r) => { noise(P, r, "#F0B860", 0.02); for (let y = 0; y < 16; y += 4) for (let x = (y % 8 ? 2 : 0); x < 16; x += 4) P.set(x, y, hx("#E0A048")); });
  reg("ssGreen", 16, 16, (P, r) => { noise(P, r, "#78B858", 0.02); for (let y = 0; y < 16; y += 4) for (let x = (y % 8 ? 2 : 0); x < 16; x += 4) P.set(x, y, hx("#68A848")); });
  reg("ssKitchen", 16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx(((x >> 2) + (y >> 2)) % 2 ? "#E8E8EC" : "#C8CCD4"), (r() - 0.5) * 0.02)); });
  reg("ssStove", 16, 16, (P, r) => { noise(P, r, "#A8B0B8", 0.02); P.rect(2, 2, 5, 5, hx("#3A3A40")); P.rect(9, 2, 5, 5, hx("#3A3A40")); P.rect(2, 9, 12, 5, hx("#C8D0D8")); });
  // The fan club and odds and ends.
  reg("fcCarpet", 16, 16, (P, r) => { noise(P, r, "#E8A0B0", 0.02); for (let y = 2; y < 16; y += 5) for (let x = 2; x < 16; x += 5) P.rect(x, y, 2, 2, hx("#F4C8D0")); });

  // ===================================== the looks, by kind =====================================
  const SHIP = { H: 3.0, floor: { a: "ssBoards" }, wall: "ssWall", cap: "cap", skirt: "walnut" };
  const LOOK = {
    vgym: { H: 3.6, floor: { a: "vgFloor", m: "vgMat", F: "vgFloor" }, wall: "vgWall", cap: "vgRailTop", skirt: "vgRailFace", lowWalls: true },
    cgym: { H: 3.6, floor: { a: "cgLawn", s: "cgBrick", f: "cgBed", m: "cgBrick" }, wall: "cgWall", cap: "cap", skirt: "walnut" },
    lobby: { H: 3.4, floor: { a: "mcCenterCheck" }, wall: "paperCenter", cap: "cap", skirt: "blueTrim" },
    lorelei: { H: 3.6, floor: { a: "lgIce" }, wall: "lgWallHi", low: "lgWall", cap: "cap", pillar: "ice", court: true },
    bruno: { H: 3.6, floor: { a: "lgSand" }, wall: "lgWallHi", low: "lgWall", cap: "cap", pillar: "rock", court: true },
    agatha: { H: 3.6, floor: { a: "lgViolet" }, wall: "lgWallHi", low: "lgWall", cap: "cap", pillar: "gilt", court: true },
    lance: { H: 3.6, floor: { a: "lgTeal" }, wall: "lgWallHi", low: "lgWall", cap: "cap", pillar: "horn", court: true },
    champion: { H: 3.6, floor: { a: "lgSlate" }, wall: "lgWallHi", low: "lgWall", cap: "cap", court: true },
    fame: { H: 3.6, floor: { a: "lgGreen" }, wall: "lgWallHi", low: "lgWall", cap: "cap" },
    condo: { H: 3.2, floor: { a: "cmBoards" }, wall: "cmPaper", cap: "cap", skirt: "walnut" },
    roof: { H: 1.2, floor: { a: "roofTile" }, wall: "roofFence", cap: "roofFenceRail", skirt: "roofFence", open: true },
    roofroom: { H: 3.0, floor: { a: "cmCarpetBlue" }, wall: "cmPaper", cap: "cap", skirt: "walnut" },
    hotel: { H: 3.2, floor: { a: "cmCarpetRed" }, wall: "cmPaper", cap: "cap", skirt: "walnut" },
    diner: { H: 3.2, floor: { a: "cmDinerTile" }, wall: "cmPaper", cap: "cap", skirt: "walnut" },
    house: { H: 3.0, floor: { a: "cmBoards" }, wall: "paperMum", cap: "cap", skirt: "paint" },
    fanclub: { H: 3.2, floor: { a: "fcCarpet" }, wall: "cmPaper", cap: "cap", skirt: "walnut" },
    hideout: { H: 3.0, floor: { a: "rhFloor" }, wall: "rhWall", cap: "rhCap", skirt: "rhCap" },
    tower: { H: 3.4, floor: { a: "twFloor" }, wall: "twWall", cap: "twWall", skirt: "twWall", graves: true, mist: true },
    ship: SHIP, cabin: Object.assign({}, SHIP, { floor: { a: "ssCabin" } }),
  };

  const FACEOF = { FACE_UP: "n", FACE_DOWN: "s", FACE_LEFT: "w", FACE_RIGHT: "e", WANDER_AROUND: "s", LOOK_AROUND: "s", WANDER_LEFT_AND_RIGHT: "e", WANDER_UP_AND_DOWN: "s",
    FACE_DOWN_AND_LEFT: "s", FACE_DOWN_AND_RIGHT: "s", FACE_UP_AND_LEFT: "n", FACE_UP_AND_RIGHT: "n", FACE_LEFT_AND_RIGHT: "e", FACE_DOWN_AND_UP: "s", n: "n", s: "s", e: "e", w: "w" };
  const SPIN = { e: "spinE", w: "spinW", n: "spinN", s: "spinS", stop: "spinStop" };
  // The ways back you can't take (the ELITE FOUR's doors shut behind you, as the games' do).
  const SHUT_BACK = new Set(["loreleis_room", "brunos_room", "agathas_room", "lances_room", "champions_room"]);
  // Who you have to beat before a room's way on opens.
  const BEAT = { loreleis_room: "loreleis_room_0", brunos_room: "brunos_room_0", agathas_room: "agathas_room_0", lances_room: "lances_room_0", champions_room: "rival_champion" };
  // People whose words come from shared scripts in FireRed: what they say (and what they do) here.
  const ROLE = {
    indigo_plateau_lobby_fr_nurse: { role: "nurse", name: "Nurse", lines: ["Welcome to the POKéMON CENTER! We heal your POKéMON back to perfect health."] },
    indigo_plateau_lobby_fr_clerk: { role: "clerk", key: "indigo_plateau", name: "Clerk", lines: ["Hi there! May I help you?"] },
    indigo_plateau_lobby_fr_black_belt: { lines: ["The ELITE FOUR are waiting beyond that door.", "Once you go through, there's no coming back till you win or lose!"] },
    indigo_plateau_lobby_fr_cooltrainer_m: { lines: ["I've come all this way and now I'm too nervous to go in!"] },
    bills_house_fr_bill: { name: "Bill", key: "bills_house_p0", skin: "super_nerd", lines: ["BILL: Look, bud, just check out some of my rare POKéMON on my PC!"] },
  };

  function build(id, F) {
    const W = F.W, D = F.D, kind = F.kind, L = LOOK[kind] || LOOK.house;
    const R = new Room(id, { name: F.name, floor: F.floor || F.name, W, D, H: L.H });
    const G = R.geo;
    R._floors = []; R._boxes = []; R.balls = []; R._spin = []; R._mons = [];
    const at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "v" : F.rows[z][x];
    const flAt = (x, z) => F.fl[z][x];
    const warpAt = {}; for (const w of F.warps) warpAt[w.x + "," + w.z] = w;
    const spinAt = {}; for (const [x, z, d] of F.spin) spinAt[x + "," + z] = d;
    const nearVoid = (x, z) => { for (let dz = -1; dz <= 1; dz++) for (let dx = -1; dx <= 1; dx++) if (at(x + dx, z + dz) === "v") return true; return x === 0 || z === 0 || x === W - 1 || z === D - 1; };
    // The kinds of things standing on a tile.
    const WALK = ".XueN";
    let cellKind = (x, z) => at(x, z);
    if (L.graves) cellKind = (x, z) => { const c = at(x, z); return (c === "w" || c === "b") && !nearVoid(x, z) && z > 2 ? "G" : c; };
    const tall = (x, z) => { const c = cellKind(x, z); return c === "w" || (c === "b" && z <= 1); };
    const lowWall = (x, z) => { const c = cellKind(x, z); return L.lowWalls ? (c === "w" || c === "b") && z > 1 && !(x === 0 || x === W - 1) : false; };
    // Doors in a wall you walk through (a room's way on): their tile is floor.
    const doorCells = new Set();
    for (const w of F.warps) if (["door", "pad", "mat", "stairs", "lift", "exit", "lift_out"].includes(w.kind)) doorCells.add(w.x + "," + w.z);
    const walkable = (x, z) => WALK.includes(cellKind(x, z)) || doorCells.has(x + "," + z) || (cellKind(x, z) === "F");

    // ----- the floor -----
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
      if (at(x, z) === "v") continue;
      R._floors.push([x, z, x + 1, z + 1]);
      const s = spinAt[x + "," + z];
      const top = s ? SPIN[s] : (L.floor[flAt(x, z)] || L.floor.a);
      G.box(x, -1.2, z, x + 1, 0, z + 1, { top, all: "dsFloorEdge", bottom: null });
      if (s) R._spin.push([x, z, s]);
    }
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (walkable(x, z)) { x++; continue; }
        let x1 = x; while (x1 < W && !walkable(x1, z)) x1++;
        R.solid(x, z, x1, z + 1); x = x1;
      }
    }
    R.solid(-1.5, -1, 0, D); R.solid(W, -1, W + 1.5, D); R.solid(-1.5, -1.5, W + 1.5, 0); R.solid(-1.5, D, W + 1.5, D + 1.5);
    if (L.court) {
      // The battle court, marked out on the floor in the middle of the room (where the member stands at its head).
      const boss = F.objs.find(o => o.kind === "trainer" || o.kind === "rival");
      if (boss) { const cx = boss.x + 0.5, cz = boss.z + 3.5; G.box(cx - 3.5, 0.004, cz - 3.5, cx + 3.5, 0.006, cz + 3.5, { top: "lgCourt", all: null }); }
    }

    // ----- the walls -----
    const H = L.H, TALL = kind === "roof" ? 1.1 : Math.min(H, 2.6);
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        const c = tall(x, z) && !lowWall(x, z) && !doorCells.has(x + "," + z);
        if (!c) { x++; continue; }
        let x1 = x; while (x1 < W && tall(x1, z) && !lowWall(x1, z) && !doorCells.has(x1 + "," + z)) x1++;
        if (L.low) {
          G.box(x, 0, z, x1, 1.0, z + 1, { all: L.low, top: null, bottom: null });
          G.box(x, 1.0, z, x1, TALL, z + 1, { all: L.wall, top: L.cap, bottom: null });
        } else {
          G.box(x, 0, z, x1, 0.22, z + 1, { all: L.skirt || L.wall, top: null, bottom: null });
          G.box(x, 0.22, z, x1, TALL, z + 1, { all: L.wall, top: L.cap, bottom: null });
        }
        x = x1;
      }
    }
    // GIOVANNI's maze: low walls, stone tops, their faces terracotta.
    if (L.lowWalls) for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (!lowWall(x, z)) { x++; continue; }
        let x1 = x; while (x1 < W && lowWall(x1, z)) x1++;
        G.box(x, 0, z, x1, 0.9, z + 1, { all: "vgRailFace", top: null, bottom: null });
        G.box(x, 0.9, z, x1, 1.15, z + 1, { all: "vgRailTop", top: "vgRailTop", bottom: null });
        x = x1;
      }
      R.item("The maze", "Low walls, stone-topped, their faces terracotta, on teal tiles: GIOVANNI's maze. Spinner arrows in the floor slide you along till a yellow square stops you.");
    }
    // What's on the back wall's face: the TOWER's arched doors, the ship's portholes, the GYM's windows.
    const faceBelow = (x, z) => { let zz = z + 1; while (zz < D && tall(x, zz)) zz++; return zz; };
    for (let x = 0; x < W; x++) {
      let z = 0; while (z < D && at(x, z) === "v") z++;
      if (z >= D || !tall(x, z)) continue;
      const zf = faceBelow(x, z);
      if (zf >= D || !walkable(x, zf)) continue;
      if (kind === "tower" && x % 3 === 1) G.box(x - 0.1, 0.1, zf - 0.03, x + 1.1, 2.4, zf, { s: "twArch", all: null });
      if ((kind === "ship") && x % 4 === 2) G.box(x + 0.15, 1.2, zf - 0.03, x + 0.85, 1.9, zf, { s: "ssPort", all: null });
      if (kind === "cgym" && x % 2 === 1 && x > 1 && x < W - 2) G.box(x + 0.1, 0.9, zf - 0.03, x + 0.9, 2.4, zf, { s: "cgPane", all: null });
    }

    // ----- the furniture, piece by piece -----
    for (const p of pieces("ShmkKsocpTBFxRQG")) {
      const { c, cells, x0, x1, z0, z1 } = p, near = [x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3];
      if (c === "S") {
        // A statue (a GYM's, by its door) or a sign.
        const cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        if (kind === "vgym" || kind === "cgym") gymStatue(R, cx, cz, [F.name.toUpperCase() + "'s statue.", "WINNING TRAINERS: {rival}"]);
        else { G.box(x0 + 0.2, 0, z0 + 0.4, x1 - 0.2, 1.4, z1 - 0.3, { all: "walnut", s: "sign", top: "walnut" }); R.act(near, { label: "Read", lines: ["A sign."] }); }
      } else if (c === "G") {
        // The TOWER's gravestones: a dark stone each, a POKé BALL mark, flowers at its foot.
        for (const [a, b] of cells) {
          G.box(a + 0.12, 0, b + 0.2, a + 0.88, 0.25, b + 0.85, { all: "twGraveTop" });
          G.box(a + 0.2, 0.25, b + 0.35, a + 0.8, 1.05, b + 0.62, { all: "twGraveTop", s: "twGrave", top: "twGraveTop" });
          G.box(a + 0.3, 1.05, b + 0.38, a + 0.7, 1.15, b + 0.59, "twGraveTop");
          if ((a * 7 + b * 3) % 3 !== 0) for (const fx of [0.28, 0.66]) { G.box(a + fx, 0.25, b + 0.66, a + fx + 0.08, 0.5, b + 0.74, "stem"); G.box(a + fx - 0.06, 0.48, b + 0.62, a + fx + 0.14, 0.58, b + 0.78, { all: ["redFabric", "cushion", "gold"][(a + b) % 3] }); }
        }
        R.act(near, { label: "Look", lines: ["A gravestone. Someone has left fresh flowers."] });
        R.item("Gravestones", "Let's Go's: dark stones with a POKé BALL mark, flowers at their feet, in rows.");
      } else if (c === "h") {
        G.box(x0 + 0.04, 0, z0 + 0.06, x1 - 0.04, 1.9, z1 - 0.06, { all: "walnut", s: kind === "hideout" ? "silphMachine" : "labShelf", top: "walnut" });
        R.act(near, { label: "Look", lines: [kind === "hideout" ? "Shelves of TEAM ROCKET's files." : "Shelves of books and files."] });
      } else if (c === "m") {
        for (const [a, b] of cells) {
          G.box(a + 0.08, 0, b + 0.1, a + 0.92, 0.85, b + 0.9, { all: kind === "hideout" ? "silphDeskSide" : "labDesk", top: kind === "hideout" ? "silphDesk" : "labDeskTop" });
          G.box(a + 0.15, 0.85, b + 0.3, a + 0.85, 1.45, b + 0.45, { all: "greyPlastic", s: "labPC" });
        }
        R.act(near, { label: "Look", lines: [kind === "hideout" ? "A computer. Its screen is full of TEAM ROCKET's plans." : "A computer."] });
      } else if (c === "k") {
        if (kind === "lobby" || kind === "hotel") for (const [a, b] of cells) { G.box(a, 0, b, a + 1, 1.0, b + 1, { all: kind === "lobby" ? "counterRed" : "walnut", top: "lab" }); }
        else table(R, x0 + 0.08, z0 + 0.08, x1 - 0.08, z1 - 0.08, { top: kind === "hideout" ? "silphDesk" : "oak", side: "walnut", leg: kind === "hideout" ? "steel" : "walnut", h: 0.85, name: "Table" });
      } else if (c === "K") {
        for (const [a, b] of cells) G.box(a + 0.02, 0, b + 0.05, a + 0.98, 0.95, b + 0.95, { all: "steel", top: "ssStove" });
        R.act(near, { label: "Look", lines: ["The stove. Something smells good!"] });
      } else if (c === "s") {
        G.box(x0 + 0.05, 0, z0 + 0.05, x1 - 0.05, 0.45, z1 - 0.05, { all: kind === "tower" ? "twSofa" : "sofa" });
        G.box(x0 + 0.05, 0.45, z0 + 0.05, x1 - 0.05, 1.0, z0 + 0.35, { all: kind === "tower" ? "twSofa" : "sofa" });
        R.act(near, { label: "Sit", lines: ["A comfy sofa."] });
      } else if (c === "o") {
        for (const [a, b] of cells) chair(R, a + 0.5, b + 0.5, "s", { wood: "walnut", seat: "blueFabric" });
      } else if (c === "c") {
        for (const [a, b] of cells) G.box(a + 0.1, 0, b + 0.1, a + 0.9, 0.8, b + 0.9, { all: kind === "hideout" ? "cardboard" : "labCrate" });
      } else if (c === "p") {
        for (const [a, b] of cells) if (at(a, b + 1) !== "p") plant(R, a + 0.5, b + 0.5, 1.0, { pot: "potWhite", kind: "leafy", lines: ["A potted plant."] });
      } else if (c === "T") {
        // ERIKA's hedges and trees: clipped boxes of leaves dotted with blossom.
        for (const [a, b] of cells) {
          const h = 1.5 + ((a * 3 + b * 5) % 3) * 0.2;
          G.box(a + 0.04, 0, b + 0.04, a + 0.96, h, b + 0.96, { all: "cgHedge", top: "cgHedgeTop" });
        }
        R.act(near, { label: "Look", lines: ["A neatly clipped hedge, dotted with blossom."] });
        R.item("Hedges", "Let's Go's GYM garden: clipped hedges dotted with pink and white blossom.");
      } else if (c === "F") {
        // GIOVANNI's dais: a yellow glass platform, a step up.
        G.box(x0, 0, z0, x1, 0.3, z1, { all: "vgDaisSide", top: "vgDais" });
        R.ramps.push({ rect: [x0, z0, x1, z1], h: () => 0.3 });
        R.item("The dais", "GIOVANNI's: a yellow glass platform at the back corner.");
      }
    }
    // The low blocks left: a counter's back, a table, a pillar, a bed (by what the room is).
    for (const p of pieces("b")) {
      const { cells, x0, x1, z0, z1 } = p, near = [x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3];
      if (L.lowWalls || L.graves) continue;
      if (L.pillar) {
        for (const [a, b] of cells) {
          if (L.pillar === "ice") G.box(a + 0.1, 0, b + 0.1, a + 0.9, 2.6, b + 0.9, { all: "lgIcePillar", top: "lgIcePillar" });
          else if (L.pillar === "rock") { G.box(a + 0.05, 0, b + 0.05, a + 0.95, 2.4, b + 0.95, { all: "lgRock" }); }
          else if (L.pillar === "gilt") { G.box(a + 0.15, 0, b + 0.15, a + 0.85, 2.8, b + 0.85, { all: "lgGilt" }); }
          else {
            // LANCE's horns: great curved tusks rising from gold bases.
            G.box(a + 0.1, 0, b + 0.1, a + 0.9, 0.5, b + 0.9, { all: "lgHornBase" });
            for (let i = 0; i < 6; i++) { const s = 0.38 - i * 0.05, dz = -i * 0.08; G.box(a + 0.5 - s, 0.5 + i * 0.35, b + 0.5 - s + dz, a + 0.5 + s, 0.85 + i * 0.35, b + 0.5 + s + dz, "lgHorn"); }
          }
        }
        R.act(near, { label: "Look", lines: [{ ice: "A pillar of ice. It's freezing to the touch.", rock: "A pillar of rough stone.", gilt: "A violet column banded with gold.", horn: "A great curved horn on a gold base." }[L.pillar]] });
      } else if (kind === "cabin" || kind === "roofroom" || kind === "house") {
        G.box(x0 + 0.05, 0, z0 + 0.05, x1 - 0.05, 0.5, z1 - 0.05, { all: "walnut", top: "quilt" });
        G.box(x0 + 0.1, 0.5, z0 + 0.1, x1 - 0.1, 0.65, z0 + 0.55, { all: "linen" });
        R.act(near, { label: "Look", lines: ["A neatly made bed."] });
      } else if (kind === "roof") {
        G.box(x0 + 0.05, 0, z0 + 0.05, x1 - 0.05, 0.6, z1 - 0.05, { all: "walnut", top: "soil" });
      } else {
        table(R, x0 + 0.08, z0 + 0.08, x1 - 0.08, z1 - 0.08, { top: kind === "hideout" ? "silphDesk" : "oak", side: "walnut", leg: kind === "hideout" ? "steel" : "walnut", h: 0.85, name: "Table" });
      }
    }
    if (L.mist) {
      // Let's Go's purple mist, low over the floor.
      G.box(0.5, 0.3, 0.5, W - 0.5, 0.302, D - 0.5, { top: "twMist", bottom: "twMist", all: null });
      R.item("Mist", "Let's Go's: a lilac haze lying low over the floor.");
    }

    // ----- the signs, hidden items -----
    for (const s of F.signs) {
      const c = at(s.x, s.z);
      if (c === "w" || c === "v") {
        const pz = faceBelow(s.x, s.z);
        G.box(s.x + 0.2, 1.25, pz, s.x + 0.8, 1.6, pz + 0.03, { s: "labPlate", all: null });
        R.act([s.x - 0.2, pz - 0.2, s.x + 1.2, pz + 1.2], { label: "Read", lines: s.lines });
      } else R.act([s.x - 0.25, s.z - 0.25, s.x + 1.25, s.z + 1.25], { label: "Look", lines: s.lines, use: s.kind === "Poster" ? "hideout_switch" : undefined });
    }
    for (const h of F.hidden || []) R.act([h.x - 0.15, h.z - 0.15, h.x + 1.15, h.z + 1.15], { label: "Look", lines: ["There's something here!"], use: "hidden:" + h.item + ":" + h.key });

    // ----- the ways out and through -----
    const runs = list => { const xs = [...new Set(list.map(w => w.x))].sort((a, b) => a - b), out = []; for (const x of xs) { const r = out[out.length - 1]; if (r && x <= r[1] + 1) r[1] = x; else out.push([x, x]); } return out; };
    const exits = F.warps.filter(w => w.kind === "exit" && !/pokemon_center_2f|deck/.test(w.dest));
    const south = exits.filter(w => w.side === "s"), north = exits.filter(w => w.side === "n"), west = exits.filter(w => w.side === "w"), east = exits.filter(w => w.side === "e");
    const matOf = { vgym: "matGate", cgym: "matHome", lobby: "matCenter", hideout: "matGate", tower: "matHouseV", ship: "matGate" }[kind] || "matHome";
    for (const [a, b] of runs(south)) doormat(R, a, b + 1, { to: "town", mat: matOf });
    // (The CELADON MANSION's back door: locked; the S.S.ANNE's way out to the dock, up its gangway.)
    for (const [a, b] of runs(north)) {
      if (id === "celadon_mansion_1f") { G.box(a, 0, 0.9, b + 1, 2.2, 1.0, { s: "labDoor", all: "walnut" }); R.act([a - 0.3, 1, b + 1.3, 2.4], { label: "Look", lines: ["The back door. It's locked."] }); continue; }
      passageN(R, a, b + 1, { to: "town", mat: "matGateNW" });
    }
    for (const w of F.warps.filter(w => w.kind === "exit" && /deck/.test(w.dest)).slice(0, 1)) {
      R.act([w.x - 0.5, w.z - 1.5, w.x + 1.5, w.z + 2.5], { label: "Look", lines: ["The door out to the deck.", "It won't open yet."] });
    }
    if (west.length) passageSide(R, "w", Math.min(...west.map(w => w.z)), Math.max(...west.map(w => w.z)) + 1, { to: "town", mat: "matGateWest" });
    if (east.length) passageSide(R, "e", Math.min(...east.map(w => w.z)), Math.max(...east.map(w => w.z)) + 1, { to: "town", mat: "matGateEast" });
    // Through to the other rooms: stairs, doors, mats, the ELITE FOUR's ways on, the lift's doors.
    const seen = new Set();
    for (const w of F.warps) {
      if (!["stairs", "door", "pad", "mat", "lift"].includes(w.kind) || !w.to) continue;
      const T = K[w.to] || (w.to === "game_corner" ? { rows: null } : null);
      if (!T) continue;
      const back = SHUT_BACK.has(id) && (w.kind === "mat" || (id === "lances_room" && w.to === "agathas_room"));
      const sp = w.to === "game_corner" ? { x: 15.5, z: 3.5, yaw: FACE.s } : landing(T, w.tx, w.tz, w.to);
      const bx0 = w.x + 0.1, bx1 = w.x + 0.9, bz0 = w.z + 0.1, bz1 = w.z + 0.9;
      const key = w.to + ":" + (w.kind === "mat" ? "m" : w.x + "," + w.z);
      // How it looks: steps for stairs, a door in the wall, the lift's doors.
      if (w.kind === "stairs") stairBlock(w);
      else if (w.kind === "door" || w.kind === "pad") {
        const wallN = !walkable(w.x, w.z - 1);
        if (wallN) G.box(w.x, 0, w.z + 0.02, w.x + 1, 2.3, w.z + 0.08, { s: kind.match(/lorelei|bruno|agatha|lance|champion|lobby/) ? (BEAT[id] ? "lgDoor" : "lgDoorOpen") : kind === "hideout" ? "silphLiftDoor" : "labDoor", all: null });
      } else if (w.kind === "lift") G.box(w.x, 0, w.z + 0.85, w.x + 1, 2.2, w.z + 0.95, { s: "silphLiftDoor", all: "silphLiftWall" });
      if (back) { if (!seen.has(key)) R.act([w.x - 0.3, w.z - 0.3, w.x + 1.3, w.z + 1.3], { label: "Look", lines: ["The door's shut behind you."] }); seen.add(key); continue; }
      const needs = BEAT[id] && !(w.kind === "mat") ? "beat:" + BEAT[id] : (id === "rocket_hideout_b4f" || w.kind !== "lift") ? null : null;
      R._boxes.push({ x0: bx0, z0: bz0, x1: bx1, z1: bz1, to: w.to, spawn: sp, needs });
      R.warps.push({ test: p => p.x > bx0 && p.x < bx1 && p.z > bz0 && p.z < bz1, to: w.to, spawn: sp, fx: "fade" });
    }
    // The ROCKET HIDEOUT's lift: out onto the floor you picked.
    if (id === "rocket_hideout_elevator") {
      G.box(1, 0, 6.6, 3, 2.0, 6.7, { n: "silphLiftDoor", all: null });
      R._boxes.push({ x0: 1, z0: 5.2, x1: 3, z1: 6.5, lift: true });
      G.box(3.2, 0.95, 3.0, 3.8, 1.75, 3.04, { s: "silphButtons", all: "silphSkirt", n: null });
      R.act([2.6, 2.6, 4.6, 4.4], { label: "Use", lines: ["The lift's buttons. Which floor?"], use: "lift" });
      R.spawn = { x: 2.0, z: 4.5, yaw: FACE.n };
    }

    // ----- the people, trainers, POKéMON and item balls -----
    for (const o of F.objs) {
      const x = o.x + 0.5, z = o.z + 0.5;
      if (o.kind === "item") { R.balls.push({ key: o.key, x, z, y: 0 }); roundBall(G, x, 0, z, 0.42); R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Look", lines: ["An item ball!"] }); continue; }
      if (o.kind === "notebook") { G.box(x - 0.2, 0.86, z - 0.15, x + 0.2, 0.88, z + 0.15, { all: "linen", top: "bookOpen" }); R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Read", lines: o.lines }); continue; }
      const f = FACEOF[o.face] || "s";
      if (o.kind === "mon") { R._mons.push({ key: "pet:" + id + "_" + o.species + "_" + o.x + "_" + o.z, species: o.species, x, z, yaw: FACE[f] }); continue; }
      if (o.kind === "rival") { R.npcs.push({ name: "Blue", key: o.key, role: "rival", x, z, yaw: FACE[f], look: { skin: "blue" }, talk: () => [{ text: "{rival}: Hey, {player}!" }] }); continue; }
      if (o.kind === "trainer") { R.npcs.push({ name: o.name, key: o.key, role: "trainer", x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => [{ text: o.name + " wants to battle!" }] }); continue; }
      const ro = ROLE[o.key] || {};
      R.npcs.push({ name: ro.name || o.name, key: ro.key || o.key, role: ro.role || null, x, z, yaw: FACE[f], look: { skin: ro.skin || o.skin }, talk: () => (ro.lines || o.lines).map(text => ({ text })) });
    }

    if (!R.spawn) {
      const e = south[0] || north[0] || F.warps.find(w => w.kind !== "exit");
      if (e === south[0] && e) R.spawn = { x: (Math.min(...south.map(w => w.x)) + Math.max(...south.map(w => w.x)) + 1) / 2, z: D - 1.6, yaw: FACE.n };
      else if (e) R.spawn = landing(F, e.x, e.z, id);
      else R.spawn = { x: W / 2, z: D / 2, yaw: FACE.s };
    }
    R.item(F.name + (F.floor ? " " + F.floor : ""), "FireRed's own room, a tile a block.");
    return R;

    function stairBlock(w) {
      const up = floorNo(w.to) > floorNo(id) || /roof/.test(w.to) && !/roof/.test(id);
      const a = w.x, b = w.z;
      if (up) for (let i = 0; i < 3; i++) G.box(a + i * 0.33, 0, b, a + 1, 0.3 + i * 0.3, b + 1, { top: "dsStep", all: "dsStepSide", bottom: null });
      else { G.box(a, -1.2, b, a + 1, -0.5, b + 1, { top: "dsStep", all: "dsWell", bottom: null }); }
      R.item(up ? "Stairs up" : "Stairs down", "Walk onto them to go " + (up ? "up." : "down."));
    }
    function pieces(codes) {
      const seen2 = new Set(), out = [];
      for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
        const c = cellKind(x, z);
        if (!codes.includes(c) || seen2.has(x + "," + z)) continue;
        const cells = [], st = [[x, z]]; seen2.add(x + "," + z);
        while (st.length) {
          const [a, b] = st.pop(); cells.push([a, b]);
          for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
            const nx = a + dx, nz = b + dz, k = nx + "," + nz;
            if (nx < 0 || nz < 0 || nx >= W || nz >= D || seen2.has(k) || cellKind(nx, nz) !== c) continue;
            seen2.add(k); st.push([nx, nz]);
          }
        }
        const xs = cells.map(q => q[0]), zs = cells.map(q => q[1]);
        out.push({ c, cells, x0: Math.min(...xs), x1: Math.max(...xs) + 1, z0: Math.min(...zs), z1: Math.max(...zs) + 1 });
      }
      return out;
    }
  }

  // Where you come out in a room: beside the way in at that tile, on the floor, facing away from it.
  function landing(T, tx, tz, tid) {
    if (tx == null || !T.rows) return { x: 1.5, z: 1.5, yaw: FACE.s };
    const at = (x, z) => x < 0 || z < 0 || x >= T.W || z >= T.D ? "v" : T.rows[z][x];
    const ok = (x, z) => ".".includes(at(x, z)) && !(T.warps || []).some(w => w.x === x && w.z === z);
    for (const [dx, dz, yaw] of [[0, 1, FACE.s], [0, -1, FACE.n], [1, 0, FACE.e], [-1, 0, FACE.w]]) if (ok(tx + dx, tz + dz)) return { x: tx + dx + 0.5, z: tz + dz + 0.5, yaw };
    for (const [dx, dz, yaw] of [[0, 2, FACE.s], [0, -2, FACE.n], [2, 0, FACE.e], [-2, 0, FACE.w]]) if (ok(tx + dx, tz + dz)) return { x: tx + dx + 0.5, z: tz + dz + 0.5, yaw };
    return { x: tx + 0.5, z: tz + 1.5, yaw: FACE.s };
  }
  function floorNo(id) {
    const m = /_(b?)(\d)f/.exec(id || ""); if (!m) return /roof/.test(id || "") ? 9 : 0;
    return m[1] ? -(+m[2]) : +m[2];
  }

  for (const [id, F] of Object.entries(K)) if (id !== "bills_house") BUILD[id] = () => build(id, F);

  // ----- BILL's SEA COTTAGE (John, 16:25: "Bill's Sea Cottage ... Which will need sorting"): FireRed's room, a tile a
  // block. The two teleporter pods on the back wall with the glowing pipe between them, his PCs either end, the crates,
  // the machines down the east wall, the table with a computer on it, BILL and the CLEFAIRY. -----
  BUILD.bills_house = function () {
    const F = K.bills_house, W = 15, D = 11;
    const R = new Room("bills_house", { name: "Sea Cottage", floor: "Route 25", W, D, H: 3.2 });
    const G = R.geo;
    R._floors = [[0, 0, W, D - 1]]; R._boxes = []; R.balls = []; R._spin = []; R._mons = [];
    for (let z = 0; z < D - 1; z++) for (let x = 0; x < W; x++) G.box(x, -1.2, z, x + 1, 0, z + 1, { top: "scBoards", all: "dsFloorEdge", bottom: null });
    // The walls: back (two rows deep: the pods stand in it), sides.
    G.box(0, 0, 0, W, 3.0, 0.6, { all: "scWall", top: "cap" });
    G.box(0, 0, 0, 0.4, 3.0, D - 1, { all: "scWall", top: "cap" }); G.box(W - 0.4, 0, 0, W, 3.0, D - 1, { all: "scWall", top: "cap" });
    R.solid(-2, -2, W + 2, 0.6); R.solid(-2, 0, 0.4, D); R.solid(W - 0.4, 0, W + 2, D); R.solid(-2, D - 1, 6, D + 2); R.solid(9, D - 1, W + 2, D + 2);
    // The pods: steel bases, green glass, a steel cap; the pipe between them glowing.
    for (const cx of [3.5, 11.0]) {
      G.box(cx - 1.2, 0, 0.6, cx + 1.2, 0.5, 3.2, { all: "cellMetal", top: "cellMetal" });
      G.box(cx - 0.95, 0.5, 0.85, cx + 0.95, 2.5, 2.95, { all: "tankGlass", bottom: null, top: null });
      G.box(cx - 1.1, 2.5, 0.7, cx + 1.1, 2.9, 3.1, { all: "scPod", top: "cellMetal" });
      R.solid(cx - 1.2, 0.6, cx + 1.2, 3.2);
    }
    G.box(4.6, 2.1, 1.6, 9.9, 2.45, 1.95, { all: "scPipe" }); G.box(4.6, 0.25, 1.6, 9.9, 0.5, 1.95, { all: "scPipe" });
    G.box(6.6, 0, 1.0, 7.9, 1.2, 2.2, { all: "cellMetal", s: "cellPanel", top: "cellMetal" }); R.solid(4.6, 0.6, 9.9, 2.2);
    R.act([2.0, 2.8, 12.6, 4.0], { label: "Look", lines: ["The CELL SEPARATOR: two teleporter pods, a glowing pipe between them."] });
    R.item("Cell Separator", "FireRed's: two green glass teleporter pods on the back wall, the glowing pipe between them, the control panel.");
    // His PCs either end, the crates, the machines down the east wall, the table with a computer.
    deskPC(R, 0.6, 0.6, "s", R.screen("billPC2", 40, 24, drawPC, 0.9), {});
    R.act([0.4, 0.6, 2.6, 2.2], { label: "Use", pc: true, lines: ["BILL's PC: his rare POKéMON, filed away."] });
    deskPC(R, 12.6, 0.6, "s", R.screen("billPC3", 40, 24, drawPC, 0.9), {});
    for (const [x, z] of [[5.0, 0.7], [9.2, 0.7]]) { G.box(x, 0, z, x + 0.9, 0.8, z + 0.8, { all: "scCrate" }); }
    for (let z = 3.6; z < 9; z += 1.5) { G.box(W - 1.3, 0, z, W - 0.4, 1.6, z + 1.2, { all: "labMachineSide", w: "labMachine", top: "labCap" }); R.solid(W - 1.3, z, W - 0.4, z + 1.2); }
    R.act([W - 2.2, 3.4, W - 0.4, 9.0], { label: "Look", lines: ["Machines, humming. BILL's teleporter controls."] });
    table(R, 4.1, 5.1, 6.0, 6.0, { top: "labDeskTop", side: "labDesk", leg: "steel", h: 0.85, name: "Table" });
    G.box(4.6, 0.85, 5.2, 5.4, 1.45, 5.4, { all: "greyPlastic", s: "labPC" });
    plant(R, 1.0, 8.6, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    R.npcs.push({ name: "Bill", key: "bills_house_p0", x: 8.5, z: 5.2, yaw: FACE.s, look: { skin: "super_nerd" },
      talk: () => [{ text: "BILL: Look, bud, just check out some of my rare POKéMON on my PC!" }] });
    R._mons.push({ key: "pet:bills_house_clefairy", species: "clefairy", x: 11.5, z: 6.5, yaw: FACE.s });
    doormat(R, 6, 9, { to: "town", mat: "matHouseV" });
    R.spawn = { x: 7.5, z: D - 1.6, yaw: FACE.n };
    R.item("Sea Cottage", "FireRed's room, a tile a block: honey boards, cream walls with a blue band.");
    return R;
  };

  // ----- The GAME CORNER's hidden stairs (behind the poster: push the switch), down to the ROCKET HIDEOUT -----
  const gc = BUILD.game_corner;
  BUILD.game_corner = function () {
    const R = gc(), G = R.geo;
    G.box(15, -1.2, 2, 16, -0.5, 3, { top: "dsStep", all: "dsWell", bottom: null });
    R._boxes = R._boxes || [];
    const sp = landing(K.rocket_hideout_b1f, 12, 2, "rocket_hideout_b1f");
    R._boxes.push({ x0: 15.1, z0: 2.1, x1: 15.9, z1: 2.9, to: "rocket_hideout_b1f", spawn: sp, needs: "hideout_open" });
    R.warps.push({ test: p => p.x > 15.1 && p.x < 15.9 && p.z > 2.1 && p.z < 2.9, to: "rocket_hideout_b1f", spawn: sp, fx: "fade" });
    R.item("Hidden stairs", "Behind the poster's switch: stairs down to TEAM ROCKET's hideout.");
    return R;
  };
})();
