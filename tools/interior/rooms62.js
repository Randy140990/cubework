// ---------- 0.58.0: CINNABAR's insides (FireRed's), the SPROUT TOWER, EARL's ACADEMY and the NATIONAL PARK's gates
// (Crystal's), and the ILEX FOREST (John's build) ----------
// Each room is its game's own map, a tile (FireRed's) or a step (Crystal's) a block: tools/cinnabar.py and
// tools/johto_rooms.py read the layouts, warps, signs and people into cinnabar_data.js and johto_data.js; the forest is
// tools/ilex.py's (ilex_data.js). The gates (the CINNABAR GYM's quiz gates, the MANSION's barred gates) are drawn here
// as they are when you first come in; in the game they're real blocks, put in or taken out for you (game/Game.java).
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const C = CINNABAR, J = JOHTO;
  for (const [id, F] of Object.entries(C)) PLACES.push({ id, name: F.name, floor: F.floor, group: "Cinnabar Island" });
  for (const [id, F] of Object.entries(J)) PLACES.push({ id, name: F.name, floor: F.floor, group: id.startsWith("sprout") || id.startsWith("earls") ? "Violet City" : "National Park" });
  PLACES.push({ id: "ilex_forest", name: "Ilex Forest", floor: "Azalea Town to Route 34", group: "Azalea Town" });

  // ----- Materials -----
  const noise = (P, r, c, k) => P.fill(hx(c), k, r);
  // BLAINE's GYM: orange floor tiles, its green walls with a light grey face, the quiz machines, the lavender pipe.
  reg("cgFloor", 16, 16, (P, r) => { noise(P, r, "#E0904A", 0.02); P.rect(0, 0, 16, 1, hx("#B8662E")); P.rect(0, 0, 1, 16, hx("#B8662E")); P.rect(1, 1, 14, 1, hx("#F0AA66")); });
  reg("cgRug", 16, 16, (P, r) => { noise(P, r, "#9C5AC4", 0.02); for (let i = 0; i < 16; i += 4) { P.rect(i, 0, 1, 16, hx("#B47AD8")); P.rect(0, i, 16, 1, hx("#B47AD8")); } });
  reg("cgMat", 16, 16, (P, r) => { noise(P, r, "#5AA0E0", 0.02); P.rect(0, 0, 16, 2, hx("#3C78B8")); P.rect(0, 14, 16, 2, hx("#3C78B8")); });
  reg("cgWall", 16, 16, (P, r) => { noise(P, r, "#D8DCE0", 0.01); P.rect(0, 15, 16, 1, hx("#9AA0A8")); for (const y of [5, 10]) P.rect(0, y, 16, 1, hx("#C0C6CC")); });
  reg("cgWallTop", 16, 16, (P, r) => { noise(P, r, "#8CD24A", 0.02); P.rect(0, 0, 16, 1, hx("#B6EA7A")); P.rect(0, 15, 16, 1, hx("#5C9A2A")); });
  reg("cgSkirt", 16, 16, (P, r) => { noise(P, r, "#5C9A2A", 0.02); P.rect(0, 0, 16, 2, hx("#8CD24A")); });
  reg("cgQuiz", 16, 32, (P, r) => {
    noise(P, r, "#E8E4F0", 0.01); P.rect(0, 0, 16, 2, hx("#9C8BAC"));
    P.rect(2, 4, 12, 9, hx("#3A3A50")); P.rect(3, 5, 10, 7, hx("#6FD0F0")); P.rect(5, 7, 6, 1, hx("#FFFFFF")); P.rect(5, 9, 4, 1, hx("#FFFFFF"));
    P.rect(3, 16, 4, 3, hx("#E2484A")); P.rect(9, 16, 4, 3, hx("#4C8FD6")); P.rect(2, 24, 12, 5, hx("#9C8BAC"));
  }, { fit: true, glow: 0.35 });
  reg("cgPipe", 16, 16, (P, r) => { noise(P, r, "#C8B4F0", 0.01); P.rect(0, 0, 16, 3, hx("#F0E8FF")); P.rect(0, 13, 16, 3, hx("#9A84C8")); for (let x = 2; x < 16; x += 6) P.rect(x, 3, 1, 10, hx("#E4D8FF")); }, { glow: 0.15 });
  reg("cgPipeEnd", 16, 16, (P, r) => { noise(P, r, "#9A84C8", 0.01); P.rect(3, 3, 10, 10, hx("#C8B4F0")); P.rect(6, 6, 4, 4, hx("#F0E8FF")); });
  reg("cgChannel", 16, 16, (P, r) => { noise(P, r, "#5C9A2A", 0.02); P.rect(0, 0, 16, 1, hx("#8CD24A")); });
  reg("cinGateView", 16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, ((x + y) >> 2) % 2 ? hx("#F08A3C") : hx("#FFE0A0")); }, { glow: 0.3 });
  reg("statueStone", 16, 16, (P, r) => { noise(P, r, "#A8A8B4", 0.03); P.rect(0, 0, 16, 1, hx("#C8C8D4")); });
  reg("statueBase", 16, 16, (P, r) => { noise(P, r, "#787884", 0.02); P.rect(0, 0, 16, 2, hx("#9898A4")); });
  // The LAB: white tiles, cream walls with a gold band, the blue rug, the machines.
  reg("labTile", 16, 16, (P, r) => { noise(P, r, "#F2F2F6", 0.006); P.rect(0, 0, 16, 1, hx("#D8D8E4")); P.rect(0, 0, 1, 16, hx("#D8D8E4")); });
  reg("labRug", 16, 16, (P, r) => { noise(P, r, "#5A7CC8", 0.01); P.rect(0, 0, 16, 1, hx("#7898DC")); });
  reg("labMat", 16, 16, (P, r) => { noise(P, r, "#D85048", 0.02); P.rect(1, 1, 14, 14, hx("#E86A60")); P.rect(3, 3, 10, 10, hx("#D85048")); });
  reg("labWall", 16, 16, (P, r) => { noise(P, r, "#F0EAD0", 0.008); P.rect(0, 15, 16, 1, hx("#C8BE98")); });
  reg("labBand", 16, 16, (P, r) => { noise(P, r, "#B8A050", 0.01); P.rect(0, 0, 16, 2, hx("#D8C070")); P.rect(0, 14, 16, 2, hx("#8A7430")); });
  reg("labCap", 16, 16, (P, r) => { noise(P, r, "#5E6A78", 0.01); P.rect(0, 0, 16, 1, hx("#7A8898")); });
  reg("labWindow", 16, 16, (P, r) => { noise(P, r, "#BFE6FF", 0.01); for (let i = 0; i < 16; i++) if (i % 6 < 2) P.line(i, 0, i - 5, 5, hx("#E8F6FF")); P.rect(0, 0, 16, 1, hx("#8A9CB0")); P.rect(7, 0, 2, 16, hx("#8A9CB0")); P.rect(0, 15, 16, 1, hx("#8A9CB0")); }, { glow: 0.2 });
  reg("labDoor", 16, 32, (P, r) => { noise(P, r, "#5C6A7C", 0.01); P.rect(1, 1, 14, 31, hx("#2C3340")); P.rect(2, 2, 12, 12, hx("#3A4456")); P.rect(11, 18, 2, 3, hx("#C8C8D0")); }, { fit: true });
  reg("labPlate", 16, 8, (P, r) => { P.fill(hx("#E8C860"), 0, r); P.rect(1, 1, 14, 6, hx("#FFF4C8")); for (const y of [3, 5]) P.rect(3, y, 10, 1, hx("#8A7430")); }, { fit: true });
  reg("labCounter", 16, 16, (P, r) => { noise(P, r, "#7AB4E8", 0.01); P.rect(0, 0, 16, 2, hx("#A8D0F4")); P.rect(0, 14, 16, 2, hx("#4C80B8")); });
  reg("labCounterTop", 16, 16, (P, r) => { noise(P, r, "#F6FAFF", 0.004); P.rect(0, 0, 16, 1, hx("#D0E0F0")); });
  reg("labDesk", 16, 16, (P, r) => { noise(P, r, "#F0D060", 0.01); P.rect(0, 0, 16, 1, hx("#FFE890")); P.rect(0, 15, 16, 1, hx("#B89830")); });
  reg("labDeskTop", 16, 16, (P, r) => { noise(P, r, "#F8E080", 0.008); P.rect(0, 0, 16, 1, hx("#FFF0B0")); P.rect(0, 15, 16, 1, hx("#D8B850")); });
  reg("labMachine", 16, 32, (P, r) => {
    noise(P, r, "#E87830", 0.01); P.rect(0, 0, 16, 2, hx("#F8A060")); P.rect(2, 4, 12, 10, hx("#2C3A4C")); P.rect(3, 5, 10, 8, hx("#5AC0E8"));
    for (let x = 3; x < 13; x += 3) P.rect(x, 17, 2, 2, hx(["#FFE060", "#60E080", "#FF6060", "#FFFFFF"][((x / 3) | 0) % 4]));
    P.rect(2, 22, 12, 7, hx("#B85A20"));
  }, { fit: true, glow: 0.35 });
  reg("labMachineSide", 16, 16, (P, r) => { noise(P, r, "#C86424", 0.01); P.rect(0, 0, 16, 1, hx("#E88040")); });
  reg("labPC", 16, 16, (P, r) => { noise(P, r, "#E0E0E8", 0.006); P.rect(2, 2, 12, 9, hx("#2C3A4C")); P.rect(3, 3, 10, 7, hx("#78D0A0")); P.rect(4, 12, 8, 2, hx("#A0A0AC")); }, { fit: true, glow: 0.4 });
  reg("labCrate", 16, 16, (P, r) => { noise(P, r, "#E8C040", 0.02); P.rect(0, 0, 16, 1, hx("#FFE070")); P.rect(0, 15, 16, 1, hx("#A88020")); P.line(0, 0, 15, 15, hx("#C8A030")); P.line(15, 0, 0, 15, hx("#C8A030")); });
  reg("fossilShell", 16, 16, (P, r) => { noise(P, r, "#F4F4F8", 0.006); P.rect(0, 13, 16, 3, hx("#B8B8C8")); P.rect(4, 4, 8, 6, hx("#3A3A50")); P.rect(5, 5, 6, 4, hx("#80E0A0")); }, { glow: 0.3 });
  reg("fossilDome", 16, 16, (P, r) => { noise(P, r, "#E04848", 0.02); P.rect(0, 0, 16, 2, hx("#FF8A8A")); for (let i = 3; i < 14; i += 4) P.rect(i, 3, 2, 2, hx("#FFC0C0")); }, { glow: 0.25 });
  reg("labShelf", 16, 16, (P, r) => {
    noise(P, r, "#8A6A48", 0.01); for (const y of [0, 7, 15]) P.rect(0, y, 16, 1, hx("#5A4028"));
    const cs = ["#E2484A", "#4C8FD6", "#F2C230", "#5DBB63", "#F08A3C", "#9C6ADE"];
    for (const y0 of [1, 8]) for (let x = 1; x < 15; x += 2) P.rect(x, y0, 2, 6 - (x % 3 === 0 ? 1 : 0), hx(cs[(x * 7 + y0) % cs.length]));
  });
  reg("labSofa", 16, 16, (P, r) => { noise(P, r, "#5A8CD8", 0.02); P.rect(0, 0, 16, 2, hx("#8CB4F0")); P.rect(0, 13, 16, 3, hx("#3C68B0")); });
  // The MANSION: stone tiles, old planks, pink brick walls with a cream face, white pillars, rubble.
  reg("manStone", 16, 16, (P, r) => {
    noise(P, r, "#D8C8A0", 0.03);
    for (const [x, y, w, h] of [[0, 0, 8, 8], [8, 0, 8, 8], [0, 8, 8, 8], [8, 8, 8, 8]]) { P.rect(x, y, w, 1, hx("#B8A47C")); P.rect(x, y, 1, h, hx("#B8A47C")); P.rect(x + 1, y + 1, w - 2, 1, hx("#E8DCC0")); }
  });
  reg("manPlank", 16, 16, (P, r) => { noise(P, r, "#A47848", 0.04); for (const y of [0, 4, 8, 12]) P.rect(0, y, 16, 1, hx("#7A5430")); P.set(5, 2, hx("#5A3C20")); P.set(12, 10, hx("#5A3C20")); });
  reg("manChecker", 16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx(((x >> 3) + (y >> 3)) % 2 ? "#D8D8E0" : "#F4F4F8"), (r() - 0.5) * 0.02)); });
  reg("manCarpet", 16, 16, (P, r) => { noise(P, r, "#C03838", 0.02); P.rect(0, 0, 1, 16, hx("#E8C060")); P.rect(15, 0, 1, 16, hx("#E8C060")); });
  reg("manRunner", 16, 16, (P, r) => { noise(P, r, "#E8A0A8", 0.02); for (let y = 2; y < 16; y += 4) for (let x = 2; x < 16; x += 4) P.set(x, y, hx("#C87880")); });
  reg("manRubble", 16, 16, (P, r) => { noise(P, r, "#8A6A48", 0.05); for (let i = 0; i < 9; i++) { const x = (i * 7 + 3) % 15, y = (i * 11 + 5) % 15; P.rect(x, y, 2, 1, hx("#B8A080")); P.set(x, y + 1, hx("#5A4430")); } });
  reg("manBrick", 16, 16, (P, r) => {
    noise(P, r, "#D88878", 0.02);
    for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx("#B86858")); for (let x = (y % 8 === 0 ? 0 : 4); x < 16; x += 8) P.rect(x, y, 1, 4, hx("#B86858")); }
  });
  reg("manRender", 16, 16, (P, r) => { noise(P, r, "#F4E8C8", 0.015); P.rect(0, 15, 16, 1, hx("#C8B890")); });
  reg("manCap", 16, 16, (P, r) => { noise(P, r, "#7A4A3A", 0.02); P.rect(0, 0, 16, 1, hx("#9A6A5A")); });
  reg("manWindow", 16, 32, (P, r) => {
    noise(P, r, "#F4E8C8", 0.01); P.rect(2, 4, 12, 24, hx("#6A4A3A"));
    for (let y = 6; y < 27; y++) for (let x = 3; x < 13; x++) { const top = y < 10 && Math.hypot(x - 7.5, y - 10) > 4.6; if (!top) P.set(x, y, hx(x === 7 || x === 8 || y === 16 ? "#6A4A3A" : "#9CD4F0")); }
  }, { fit: true, glow: 0.15 });
  reg("manPillar", 16, 16, (P, r) => { noise(P, r, "#F6F2EA", 0.01); P.rect(0, 0, 2, 16, hx("#FFFFFF")); P.rect(13, 0, 3, 16, hx("#C8C0B0")); for (let x = 4; x < 12; x += 3) P.rect(x, 0, 1, 16, hx("#E0D8C8")); });
  reg("manRock", 16, 16, (P, r) => { noise(P, r, "#8C8078", 0.06); P.rect(2, 2, 6, 5, hx("#A89C94")); P.rect(9, 8, 5, 5, hx("#A89C94")); P.rect(0, 15, 16, 1, hx("#5A5048")); });
  reg("manTable", 16, 16, (P, r) => { noise(P, r, "#E8C860", 0.01); P.rect(0, 0, 16, 1, hx("#FFE890")); });
  reg("manShelf", 16, 16, (P, r) => {
    noise(P, r, "#7A4A3A", 0.01); for (const y of [0, 7, 15]) P.rect(0, y, 16, 1, hx("#4A2A20"));
    for (const y0 of [1, 8]) for (let x = 1; x < 15; x += 2) P.rect(x, y0, 2, 6, hx(["#C84848", "#486CC8", "#C8A848", "#48A858"][(x + y0) % 4]));
  });
  reg("manGateView", 16, 16, (P, r) => { P.fill(hx("#3A3A44"), 0, r); for (let x = 1; x < 16; x += 3) P.rect(x, 0, 1, 16, hx("#9A9AA8")); P.rect(0, 1, 16, 1, hx("#B0B0BC")); P.rect(0, 14, 16, 1, hx("#B0B0BC")); });
  reg("manHole", 16, 16, (P, r) => { noise(P, r, "#1A1410", 0.02); P.rect(0, 0, 16, 2, hx("#5A4430")); });
  reg("manStep", 16, 16, (P, r) => { noise(P, r, "#E8A0A8", 0.02); P.rect(0, 0, 16, 2, hx("#F8C8CC")); P.rect(0, 14, 16, 2, hx("#B87078")); });
  reg("manWell", 16, 16, (P, r) => { noise(P, r, "#3A2A24", 0.02); });
  // The SPROUT TOWER: wide boards, log rails, dark timber walls, the pillar.
  reg("stBoards", 16, 16, (P, r) => { noise(P, r, "#C8A070", 0.03); for (const y of [0, 8]) P.rect(0, y, 16, 1, hx("#9A7448")); P.rect(5, 1, 1, 7, hx("#B08A5C")); P.rect(11, 9, 1, 7, hx("#B08A5C")); });
  reg("stLog", 16, 16, (P, r) => { noise(P, r, "#8A6440", 0.03); for (const y of [3, 9, 13]) P.rect(0, y, 16, 1, hx("#6A4828")); P.rect(0, 0, 16, 1, hx("#A88058")); });
  reg("stLogEnd", 16, 16, (P, r) => { noise(P, r, "#A88058", 0.02); for (let k = 0; k < 3; k++) { const s = 2 + k * 2; P.rect(s, s, 16 - 2 * s, 1, hx("#7A5430")); P.rect(s, 15 - s, 16 - 2 * s, 1, hx("#7A5430")); } });
  reg("stDark", 16, 16, (P, r) => { noise(P, r, "#3A2A20", 0.02); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#2A1C14")); P.rect(0, 0, 16, 1, hx("#5A4030")); });
  reg("stPillar", 16, 16, (P, r) => { noise(P, r, "#6A4A30", 0.03); for (let x = 2; x < 16; x += 5) P.rect(x, 0, 1, 16, hx("#4A3020")); });
  reg("stPaper", 16, 16, (P, r) => { noise(P, r, "#F4ECD8", 0.01); P.rect(0, 0, 16, 1, hx("#6A4A30")); P.rect(0, 15, 16, 1, hx("#6A4A30")); P.rect(7, 0, 2, 16, hx("#6A4A30")); });
  reg("stPainting", 32, 16, (P, r) => {
    P.fill(hx("#6A4A30"), 0, r); P.rect(1, 1, 30, 14, hx("#F4ECD8"));
    for (let x = 3; x < 29; x++) { const y = 11 - Math.round(Math.sin(x / 4) * 2); P.rect(x, y, 1, 13 - y, hx("#9AC07A")); }
    P.rect(14, 3, 3, 8, hx("#5A8A3A")); P.rect(12, 4, 2, 2, hx("#5A8A3A")); P.rect(17, 5, 2, 2, hx("#5A8A3A"));
  }, { fit: true });
  // EARL's ACADEMY: a warm floor, the blackboard.
  reg("acFloor", 16, 16, (P, r) => { noise(P, r, "#D8B888", 0.03); for (const y of [0, 5, 10]) P.rect(0, y, 16, 1, hx("#B89868")); });
  reg("acBoard", 32, 16, (P, r) => {
    P.fill(hx("#8A6440"), 0, r); P.rect(1, 1, 30, 13, hx("#2E5A3E"));
    for (const [x, y, w] of [[4, 4, 10], [4, 7, 14], [4, 10, 8], [18, 4, 9], [18, 8, 6]]) P.rect(x, y, w, 1, hx("#E8F0E8"));
  }, { fit: true });
  // The NATIONAL PARK's gates (Crystal's gate tiles): cream walls with a blue band, a checked floor, the green counter.
  reg("ngFloor", 16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx(((x >> 2) + (y >> 2)) % 2 ? "#E0D8C8" : "#F4F0E4"), (r() - 0.5) * 0.02)); });
  reg("ngWall", 16, 16, (P, r) => { noise(P, r, "#F0ECDC", 0.008); P.rect(0, 9, 16, 3, hx("#5A78C8")); P.rect(0, 15, 16, 1, hx("#B8B0A0")); });
  reg("ngCounter", 16, 16, (P, r) => { noise(P, r, "#78B858", 0.02); P.rect(0, 0, 16, 2, hx("#A0D880")); P.rect(0, 14, 16, 2, hx("#4A8830")); });
  reg("ngMat", 16, 16, (P, r) => { noise(P, r, "#C86A4A", 0.02); P.rect(1, 1, 14, 14, hx("#E08A60")); });

  const FACEOF = { FACE_UP: "n", FACE_DOWN: "s", FACE_LEFT: "w", FACE_RIGHT: "e", WANDER_AROUND: "s", LOOK_AROUND: "s", WANDER_LEFT_AND_RIGHT: "e", n: "n", s: "s", e: "e", w: "w" };

  function build(id, F, kind) {
    const W = F.W, D = F.D;
    const R = new Room(id, { name: F.name, floor: F.floor, W, D, H: kind === "mansion" ? 3.6 : 3.2 });
    const G = R.geo;
    R._floors = []; R._boxes = []; R.balls = [];
    const at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "v" : F.rows[z][x];
    const idAt = (x, z) => F.ids && x >= 0 && z >= 0 && x < W && z < D ? F.ids[z][x] : -1;
    const flAt = (x, z) => F.fl[z][x];
    const warpAt = {}; for (const w of F.warps) warpAt[w.x + "," + w.z] = w;
    const pillarId = t => [0x2bd, 0x2c5, 0x2cd, 0x2c7].includes(t);
    const WALK = ".XuHD";
    const walkable = (x, z) => WALK.includes(at(x, z));
    const TALL = kind === "mansion" ? 2.8 : 2.4;

    // The floor, each tile's kind (and the stairwells going down, and 3F's holes, sunk).
    const FLOOR = {
      gym: { g: "cgFloor", r: "cgRug", m: "cgMat" }, lab: { l: "labTile", b: "labRug", t: "labMat" },
      mansion: { f: "manStone", n: "manPlank", q: "manChecker", r: "manCarpet", R: "manRunner", d: "manRubble" },
      sprout: { t: "stBoards" }, academy: { a: "acFloor" }, gate: { g: "ngFloor" } }[kind];
    const sunk = new Set();
    const stairs = [];
    for (const w of F.warps.filter(w => w.kind === "stairs")) {
      const up = floorNo(w.to) > floorNo(id);
      const cells = stairCells(w.x, w.z);
      // The side you walk on from: the floor beside the stairs (the south first).
      let from = [0, 1];
      for (const d of [[0, 1], [-1, 0], [1, 0], [0, -1]]) if (at(w.x + d[0], w.z + d[1]) === "." && !cells.some(c => c[0] === w.x + d[0] && c[1] === w.z + d[1])) { from = d; break; }
      stairs.push({ w, up, cells, from });
      if (!up && kind !== "sprout") for (const [a, b] of cells) sunk.add(a + "," + b);
    }
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
      if (at(x, z) === "v") continue;
      R._floors.push([x, z, x + 1, z + 1]);
      if (sunk.has(x + "," + z)) continue;
      if (at(x, z) === "H") { G.box(x, -1.2, z, x + 1, -0.9, z + 1, { top: "manHole", all: "manWell", bottom: null }); continue; }
      G.box(x, -1.2, z, x + 1, 0, z + 1, { top: FLOOR[flAt(x, z)] || Object.values(FLOOR)[0], all: "dsFloorEdge", bottom: null });
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

    // The walls: in runs along each row.
    const wallKind = c => c === "w" ? "w" : c === "b" ? "b" : c === "r" ? "r" : null;
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        const k = wallKind(at(x, z));
        if (!k) { x++; continue; }
        let x1 = x; while (x1 < W && wallKind(at(x1, z)) === k) x1++;
        if (k === "r") {
          // The SPROUT TOWER's log rails: a post at each end and every other block, two rails between.
          for (let a = x; a < x1; a++) G.box(a + 0.38, 0, z + 0.38, a + 0.62, 1.15, z + 0.62, { all: "stLog", top: "stLogEnd" });
          G.box(x + 0.3, 0.5, z + 0.42, x1 - 0.3, 0.62, z + 0.58, "stLog"); G.box(x + 0.3, 0.92, z + 0.4, x1 - 0.3, 1.06, z + 0.6, "stLog");
        } else if (kind === "gym") {
          const h = k === "b" ? 1.9 : TALL;
          G.box(x, 0, z, x1, 0.25, z + 1, { all: "cgSkirt", top: null, bottom: null });
          G.box(x, 0.25, z, x1, h - 0.2, z + 1, { all: "cgWall", top: null, bottom: null });
          G.box(x, h - 0.2, z, x1, h, z + 1, { all: "cgWallTop", top: "cgWallTop", bottom: null });
        } else if (kind === "lab") {
          G.box(x, 0, z, x1, 0.15, z + 1, { all: "labCap", top: null, bottom: null });
          G.box(x, 0.15, z, x1, 1.0, z + 1, { all: "labWall", top: null, bottom: null });
          G.box(x, 1.0, z, x1, 1.14, z + 1, { all: "labBand", top: null, bottom: null });
          G.box(x, 1.14, z, x1, TALL, z + 1, { all: "labWall", top: "labCap", bottom: null });
        } else if (kind === "mansion") {
          G.box(x, 0, z, x1, 1.0, z + 1, { all: "manBrick", top: null, bottom: null });
          G.box(x, 1.0, z, x1, TALL, z + 1, { all: "manRender", top: "manCap", bottom: null });
        } else if (kind === "sprout") {
          G.box(x, 0, z, x1, TALL, z + 1, { all: "stDark", top: "stLogEnd", bottom: null });
        } else if (kind === "academy") {
          G.box(x, 0, z, x1, TALL, z + 1, { all: "labWall", top: "labCap", bottom: null });
        } else {
          G.box(x, 0, z, x1, TALL, z + 1, { all: "ngWall", top: "labCap", bottom: null });
        }
        x = x1;
      }
    }
    // (The front of the back wall: what's on it, by the tile there.)
    const faceBelow = (x, z) => { let zz = z + 1; while (zz < D && at(x, zz) === "w") zz++; return zz; };
    if (kind === "lab" || kind === "mansion") {
      for (let x = 0; x < W; x++) {
        const t = idAt(x, 0), t1 = idAt(x, 1);
        const zf = faceBelow(x, 0);
        if (kind === "lab" && [0x314, 0x315, 0x2f9, 0x2fa, 0x6b, 0x6c].includes(t)) G.box(x + 0.1, 1.35, zf - 0.02, x + 0.9, 2.05, zf, { s: "labWindow", all: null });
        if (kind === "mansion" && [0x378, 0x379].includes(t1) && t1 === 0x378) G.box(x + 0.2, 1.05, zf - 0.02, x + 1.8, 2.5, zf, { s: "manWindow", all: null });
      }
    }

    // The furniture, piece by piece.
    for (const p of pieces(F, "PSTQpkdmFhcosBA")) {
      const { x0, x1, z0, z1, cells } = p, near = [x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3];
      if (p.c === "P") {
        // The SPROUT TOWER's pillar: one great post up through every floor (it sways in the game).
        G.box(x0 + 0.1, 0, z0 + 0.1, x1 - 0.1, TALL + 0.8, z1 - 0.1, { all: "stPillar", top: "stLogEnd" });
        R.act(near, { label: "Look", lines: ["The tower's great pillar. It's said to be a BELLSPROUT over 100 feet tall: it sways, and the tower sways with it."] });
        R.item("The pillar", "Up the middle of every floor: the great swaying pillar.");
      } else if (p.c === "S") {
        for (const [a, b] of cells) {
          G.box(a + 0.1, 0, b + 0.1, a + 0.9, 0.45, b + 0.9, { all: "statueBase", top: "statueStone" });
          G.box(a + 0.22, 0.45, b + 0.25, a + 0.78, 1.45, b + 0.8, { all: "statueStone" });
          G.box(a + 0.3, 1.45, b + 0.32, a + 0.7, 1.75, b + 0.72, { all: "statueStone" });
        }
        const sw = (F.switches || []).find(s => cells.some(([a, b]) => Math.abs(a - s.x) <= 0 && Math.abs(b - s.z) <= 1));
        if (sw) R.act(near, { label: "Look", use: "mansion_switch", lines: ["A POKéMON statue... | Oh, what's this? A secret switch! | Press it?"],
          run: async say => { const pick = await say([{ text: "A POKéMON statue... Oh, what's this? A secret switch! Press it?", ask: ["YES", "NO"] }]);
            if (pick === 0) await say([{ text: "Who wouldn't?" }, { text: "(In the game the barred gates on every floor swap: the shut ones open, the open ones shut.)" }]); else await say([{ text: "Not quite yet!" }]); } });
        else if (kind === "gym") R.act(near, { label: "Read", lines: ["CINNABAR POKéMON GYM", "LEADER: BLAINE", "WINNING TRAINERS: {rival}"] });
      } else if (p.c === "T") {
        // BLAINE's GYM's pipe: a long glass tube lying the length of the middle, in its green channel.
        G.box(x0, 0, z0, x1, 0.35, z1, { all: "cgChannel", top: "cgChannel" });
        G.box(x0 + 0.85, 0.35, z0 + 0.4, x1 - 0.85, 1.45, z1 - 0.2, { all: "cgPipe", n: "cgPipeEnd", s: "cgPipeEnd" });
        R.item("The pipe", "A long lavender glass pipe down the middle of the GYM, in a green channel.");
      } else if (p.c === "Q") {
        for (const [a, b] of cells) G.box(a + 0.06, 0, b + 0.06, a + 0.94, 1.7, b + 0.94, { all: "labMachineSide", s: "cgQuiz", top: "labCap" });
      } else if (p.c === "p") {
        const big = cells.length >= 2;
        for (const [a, b] of cells) if (!big || at(a, b + 1) !== "p") plant(R, a + 0.5, b + 0.5, big ? 1.15 : 1.0, { pot: "potWhite", kind: "leafy", lines: ["A potted plant."] });
      } else if (p.c === "k") {
        if (kind === "gate") for (const [a, b] of cells) G.box(a, 0, b, a + 1, 1.0, b + 1, { all: "ngCounter", top: "labCounterTop" });
        else if (kind === "academy") for (const [a, b] of cells) { G.box(a + 0.08, 0.7, b + 0.1, a + 0.92, 0.78, b + 0.9, { all: "labDesk", top: "labDeskTop" }); G.box(a + 0.15, 0, b + 0.2, a + 0.25, 0.7, b + 0.3, "steel"); G.box(a + 0.75, 0, b + 0.7, a + 0.85, 0.7, b + 0.8, "steel"); }
        else if (kind === "lab" && cells.some(([a, b]) => idAt(a, b) >= 0x2d3 && idAt(a, b) <= 0x2d7)) for (const [a, b] of cells) G.box(a, 0, b, a + 1, 1.0, b + 1, { all: "labCounter", top: "labCounterTop" });
        else table(R, x0 + 0.08, z0 + 0.08, x1 - 0.08, z1 - 0.08, { top: kind === "mansion" ? "manTable" : "labDeskTop", side: kind === "mansion" ? "manTable" : "labDesk", leg: "steel", h: 0.85, name: "Table" });
        if (kind === "gate") R.act(near, { label: "Look", lines: ["The gate's counter."] });
      } else if (p.c === "d") {
        G.box(x0 + 0.05, 0, z0 + 0.1, x1 - 0.05, 1.0, z1 - 0.1, { all: "walnut", top: "oak" });
        R.act(near, { label: "Look", lines: ["EARL's desk, at the front of the class."] });
      } else if (p.c === "B") {
        G.box(x0 + 0.1, 0.9, z1 - 0.06, x1 - 0.1, 2.1, z1, { s: "acBoard", all: "walnut" });
        R.act([x0, z1, x1, z1 + 1.2], { label: "Read", lines: ["The blackboard describes POKéMON status changes during battle."] });
      } else if (p.c === "A") {
        G.box(x0 + 0.05, 1.0, z1 - 0.05, x1 - 0.05, 2.1, z1, { s: "stPainting", all: "stPillar" });
      } else if (p.c === "m") {
        for (const [a, b] of cells) {
          const tall = kind === "lab" && (idAt(a, b) >= 0x318 && idAt(a, b) <= 0x323);
          if (tall) G.box(a + 0.04, 0, b + 0.04, a + 0.96, 2.1, b + 0.96, { all: "labMachineSide", s: "labMachine", top: "labCap" });
          else { G.box(a + 0.08, 0, b + 0.1, a + 0.92, 0.85, b + 0.9, { all: "labDesk", top: "labDeskTop" }); G.box(a + 0.15, 0.85, b + 0.3, a + 0.85, 1.45, b + 0.45, { all: "greyPlastic", s: "labPC" }); }
        }
        R.act(near, { label: "Look", lines: [kind === "mansion" ? "An old computer. Its screen is dead." : "A machine, humming. Its screen is full of readings."] });
      } else if (p.c === "F") {
        // The TESTING ROOM's revival machine: a red dome on a white stand.
        G.box(x0 + 0.05, 0, z0 + 0.1, x1 - 0.05, 1.0, z1 - 0.05, { all: "fossilShell", top: "labCap" });
        G.box(x0 + 0.25, 1.0, z0 + 0.3, x1 - 0.25, 2.0, z1 - 0.25, { all: "fossilDome" });
        R.act(near, { label: "Look", lines: ["A machine for bringing fossils back to life."] });
        R.item("Revival machine", "A red dome on a white stand, by the TESTING ROOM's back wall.");
      } else if (p.c === "h") {
        G.box(x0 + 0.04, 0, z0 + 0.06, x1 - 0.04, 1.9, z1 - 0.06, { all: "walnut", s: kind === "mansion" ? "manShelf" : "labShelf", top: "walnut" });
        R.act(near, { label: "Look", lines: [kind === "academy" ? "The bookshelves are full of books on POKéMON." : "Shelves of books and files."] });
      } else if (p.c === "c") {
        for (const [a, b] of cells) {
          if (kind === "lab") G.box(a + 0.08, 0, b + 0.08, a + 0.92, 0.84, b + 0.92, { all: "labCrate" });
          else if (pillarId(idAt(a, b))) G.box(a + 0.18, 0, b + 0.18, a + 0.82, TALL, b + 0.82, { all: "manPillar", top: "manCap" });
          else { G.box(a + 0.05, 0, b + 0.1, a + 0.95, 0.6, b + 0.95, { all: "manRock" }); G.box(a + 0.25, 0.6, b + 0.3, a + 0.8, 0.95, b + 0.8, { all: "manRock" }); }
        }
      } else if (p.c === "o") {
        for (const [a, b] of cells) {
          if (kind === "lab") stool(R, a + 0.5, b + 0.5);
          else chair(R, a + 0.5, b + 0.5, "s", { wood: "walnut", seat: "blueFabric" });
        }
      } else if (p.c === "s") {
        G.box(x0 + 0.05, 0, z0 + 0.05, x1 - 0.05, 0.45, z1 - 0.05, { all: "labSofa" });
        const back = x0 === 0 ? [x0 + 0.05, x0 + 0.35] : [x1 - 0.35, x1 - 0.05];
        G.box(back[0], 0.45, z0 + 0.05, back[1], 1.0, z1 - 0.05, { all: "labSofa" });
        R.act(near, { label: "Sit", lines: ["A soft blue sofa."] });
      }
    }

    // The gates, shut as you first find them (in the game: real blocks, put in or taken out for you).
    for (const g of F.gates || []) {
      const shut = g.shut === "quiz" || g.shut === "off";
      if (!shut) continue;
      for (const [a, b] of g.cells) G.box(a + 0.04, 0, b + 0.3, a + 0.96, 1.9, b + 0.7, { all: kind === "gym" ? "cinGateView" : "manGateView", top: null, bottom: null });
    }
    if (kind === "gym") {
      R.item("Quiz gates", "Six gates, each shut till you answer its machine's question right (or beat the trainer in its room). Shown shut here.");
      for (const g of F.gates) for (const [qx, qz] of g.quiz.slice(0, 1)) {
        R.act([qx - 0.3, qz - 0.2, qx + 2.3, qz + 1.6], { label: "Answer", use: "quiz:" + g.n, lines: ["POKéMON Quiz! Get it right, and the door opens to the next room.", g.question],
          run: async say => { const pick = await say([{ text: "POKéMON Quiz! Get it right, and the door opens to the next room!" }, { text: g.question, ask: ["YES", "NO"] }]);
            if (pick != null && (pick === 0) === g.yes) await say([{ text: "You're absolutely correct! Go on through!" }]); else if (pick != null) await say([{ text: "Sorry! Bad call!" }]); } });
      }
    }
    if (kind === "mansion") R.item("Barred gates", "The grey barred gates. The statue's secret switch swaps them: shown here as they are when you first come in.");

    // The signs and things on the walls.
    for (const s of F.signs) {
      if (at(s.x, s.z) === "w" || at(s.x, s.z) === "B") {
        const pz = faceBelow(s.x, s.z);
        if (kind !== "academy") G.box(s.x + 0.2, 1.25, pz, s.x + 0.8, 1.6, pz + 0.03, { s: "labPlate", all: null });
        R.act([s.x - 0.2, pz - 0.2, s.x + 1.2, pz + 1.2], { label: "Read", lines: s.lines });
      } else R.act([s.x - 0.25, s.z - 0.25, s.x + 1.25, s.z + 1.25], { label: "Look", lines: s.lines });
    }
    for (const h of F.hidden || []) R.act([h.x - 0.15, h.z - 0.15, h.x + 1.15, h.z + 1.15], { label: "Look", lines: ["There's something here!"], use: "hidden:" + h.item + ":" + h.key });

    // The stairs (and the SPROUT TOWER's ladders): steps up away from the floor you walk on from, or a well going down.
    for (const { w, up, cells, from } of stairs) {
      const away = [-from[0], -from[1]];
      for (const [a, b] of cells) {
        if (kind === "sprout") { G.box(a + 0.2, 0, b + 0.1, a + 0.8, 2.2, b + 0.25, "stLog"); for (let y = 0.3; y < 2.2; y += 0.4) G.box(a + 0.2, y, b + 0.1, a + 0.8, y + 0.06, b + 0.35, "stLogEnd"); continue; }
        const n = Math.max(1, away[0] ? Math.abs(a - w.x) + 1 : Math.abs(b - w.z) + 1);
        if (up) G.box(a, 0, b, a + 1, Math.min(n * 0.35, 1.4), b + 1, { top: kind === "mansion" ? "manStep" : "dsStep", all: "manBrick", bottom: null });
        else G.box(a, -1.2, b, a + 1, -Math.min(n * 0.35, 1.0), b + 1, { top: "manStep", all: "manWell", bottom: null });
      }
      const T = (C[w.to] || J[w.to]);
      const tw = T && T.warps.find(t => t.x === w.tx && t.z === w.tz);
      const sp = T ? landing(T, w.tx, w.tz) : { x: 1, z: 1, yaw: FACE.s };
      const xs = cells.map(c => c[0]), zs = cells.map(c => c[1]);
      const bx0 = w.x + 0.15, bx1 = w.x + 0.85, bz0 = w.z + 0.15, bz1 = w.z + 0.85;
      R._boxes.push({ x0: bx0, z0: bz0, x1: bx1, z1: bz1, to: w.to, spawn: sp });
      R.warps.push({ test: p => p.x > bx0 && p.x < bx1 && p.z > bz0 && p.z < bz1, to: w.to, spawn: sp, fx: "fade" });
      R.item(kind === "sprout" ? (up ? "Ladder up" : "Ladder down") : up ? "Stairs up" : "Stairs down", "Walk onto them to go " + (up ? "up." : "down."));
    }
    // 3F's holes: step in and you fall to the floor below.
    for (const w of F.warps.filter(w => w.kind === "hole")) {
      const T = C[w.to], sp = { x: w.tx + 0.5, z: w.tz + 0.5, yaw: FACE.s };
      R._boxes.push({ x0: w.x + 0.1, z0: w.z + 0.1, x1: w.x + 0.9, z1: w.z + 0.9, to: w.to, spawn: sp });
      R.warps.push({ test: p => p.x > w.x + 0.1 && p.x < w.x + 0.9 && p.z > w.z + 0.1 && p.z < w.z + 0.9, to: w.to, spawn: sp, fx: "fade" });
      R.item("Holes", "Holes in 3F's floor: step in and you drop to " + (T ? T.floor : "the floor below") + ".");
    }
    // The LAB's doors along its hall: in to each room, out onto its mat.
    for (const w of F.warps.filter(w => w.kind === "door")) {
      G.box(w.x, 0, w.z + 0.9, w.x + 1, 2.0, w.z + 1.0, { s: "labDoor", all: "labCap" });
      const T = C[w.to], back = T.warps.find(t => t.kind === "back"), sp = { x: back.x + 0.5, z: back.z - 0.6, yaw: FACE.n };
      R._boxes.push({ x0: w.x + 0.1, z0: w.z, x1: w.x + 0.9, z1: w.z + 1.2, to: w.to, spawn: sp });
      R.warps.push({ test: p => p.x > w.x + 0.1 && p.x < w.x + 0.9 && p.z < w.z + 1.2, to: w.to, spawn: sp, fx: "fade" });
    }

    // The people, trainers and item balls.
    for (const o of F.objs) {
      const x = o.x + 0.5, z = o.z + 0.5;
      if (o.kind === "item") { R.balls.push({ key: o.key, x, z, y: 0 }); roundBall(G, x, 0, z, 0.42); R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Look", lines: ["An item ball!"] }); continue; }
      if (o.kind === "notebook") { G.box(x - 0.2, 0.78, z - 0.15, x + 0.2, 0.8, z + 0.15, { all: "linen", top: "bookOpen" }); R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Read", lines: o.lines }); continue; }
      const f = FACEOF[o.face] || FACEOF[o.dir] || "s";
      if (o.kind === "trainer") R.npcs.push({ name: o.name, key: o.key, role: "trainer", x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => (o.lines || ["..."]).map(text => ({ text })) });
      else R.npcs.push({ name: o.name, key: o.key, x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => o.lines.map(text => ({ text })) });
    }

    // The ways out: the mats at the front (the MANSION's two), a gate's north and side ways.
    const back = F.warps.find(w => w.kind === "back");
    if (back) {
      doormat(R, back.x - 1, back.x + 2, { to: "cinnabar_lab", spawn: labSpawn(id), mat: "matHome" });
      R.spawn = { x: back.x + 0.5, z: D - 1.6, yaw: FACE.n };
    }
    const exits = F.warps.filter(w => w.kind === "exit" || w.kind === "park");
    const south = exits.filter(w => w.side === "s" || (!w.side && w.z >= D - 2)), north = exits.filter(w => w.side === "n"), west = exits.filter(w => w.side === "w"), east = exits.filter(w => w.side === "e");
    const runs = list => { const xs = [...new Set(list.map(w => w.x))].sort((a, b) => a - b), out = []; for (const x of xs) { const r = out[out.length - 1]; if (r && x <= r[1] + 1) r[1] = x; else out.push([x, x]); } return out; };
    for (const [a, b] of runs(south)) doormat(R, a, b + 1, { to: "town", mat: kind === "gate" ? "matGate" : "matHome" });
    for (const [a, b] of runs(north)) passageN(R, a, b + 1, { to: "town", mat: "matGate" });
    if (west.length) passageSide(R, "w", Math.min(...west.map(w => w.z)), Math.max(...west.map(w => w.z)) + 1, { to: "town", mat: "matGateWest" });
    if (east.length) passageSide(R, "e", Math.min(...east.map(w => w.z)), Math.max(...east.map(w => w.z)) + 1, { to: "town", mat: "matGateEast" });
    if (!R.spawn) {
      const e = south[0] || east[0] || north[0];
      R.spawn = e ? (e === east[0] ? { x: W - 1.6, z: e.z + 0.5, yaw: FACE.w } : e === north[0] ? { x: e.x + 1, z: 1.6, yaw: FACE.s } : { x: (Math.min(...south.map(w => w.x)) + Math.max(...south.map(w => w.x)) + 1) / 2, z: D - 1.6, yaw: FACE.n })
        : { x: W / 2, z: D / 2, yaw: FACE.s };
    }
    R.item(F.name, kind === "gate" || kind === "sprout" || kind === "academy" ? "Crystal's own room, a step a block." : "FireRed's own room, a tile a block.");
    return R;

    function stairCells(wx, wz) {
      const seen = new Set([wx + "," + wz]), st = [[wx, wz]], out = [];
      while (st.length) {
        const [a, b] = st.pop(); out.push([a, b]);
        for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
          const nx = a + dx, nz = b + dz, k = nx + "," + nz;
          if (seen.has(k) || at(nx, nz) !== "u" || warpAt[k] && warpAt[k].to !== warpAt[wx + "," + wz].to) continue;
          seen.add(k); st.push([nx, nz]);
        }
      }
      return out;
    }
  }

  // Where you come out on a floor: beside its stairs (or ladder) at that warp, on the floor, facing away from them.
  function landing(T, tx, tz) {
    const at = (x, z) => x < 0 || z < 0 || x >= T.W || z >= T.D ? "v" : T.rows[z][x];
    for (const [dx, dz, yaw] of [[0, 1, FACE.s], [1, 0, FACE.e], [-1, 0, FACE.w], [0, -1, FACE.n]]) if (at(tx + dx, tz + dz) === ".") return { x: tx + dx + 0.5, z: tz + dz + 0.5, yaw };
    return { x: tx + 0.5, z: tz + 1.5, yaw: FACE.s };
  }
  function labSpawn(id) {
    const d = CINNABAR.cinnabar_lab.warps.find(w => w.kind === "door" && w.to === id);
    return { x: d.x + 0.5, z: d.z + 1.6, yaw: FACE.s };
  }
  function floorNo(id) {
    const m = /_(b?)(\d)f$/.exec(id || ""); if (!m) return 0;
    return m[1] ? -(+m[2]) : +m[2];
  }
  function pieces(F, codes) {
    const seen = new Set(), out = [];
    for (let z = 0; z < F.D; z++) for (let x = 0; x < F.W; x++) {
      const c = F.rows[z][x];
      if (!codes.includes(c) || seen.has(x + "," + z)) continue;
      const cells = [], st = [[x, z]]; seen.add(x + "," + z);
      while (st.length) {
        const [a, b] = st.pop(); cells.push([a, b]);
        for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
          const nx = a + dx, nz = b + dz, k = nx + "," + nz;
          if (nx < 0 || nz < 0 || nx >= F.W || nz >= F.D || seen.has(k) || F.rows[nz][nx] !== c) continue;
          seen.add(k); st.push([nx, nz]);
        }
      }
      const xs = cells.map(c => c[0]), zs = cells.map(c => c[1]);
      out.push({ c, cells, x0: Math.min(...xs), x1: Math.max(...xs) + 1, z0: Math.min(...zs), z1: Math.max(...zs) + 1 });
    }
    return out;
  }

  const KIND = id => id === "cinnabar_gym" ? "gym" : id.startsWith("cinnabar_lab") ? "lab" : id.startsWith("pokemon_mansion") ? "mansion" : id.startsWith("sprout") ? "sprout" : id.startsWith("earls") ? "academy" : "gate";
  for (const [id, F] of Object.entries(C)) BUILD[id] = () => build(id, F, KIND(id));
  for (const [id, F] of Object.entries(J)) BUILD[id] = () => build(id, F, KIND(id));

  // ----- The ILEX FOREST (tools/ilex.py, from John's schematic), as Viridian Forest is shown -----
  BUILD.ilex_forest = function () {
    const F = ILEX, P16 = 1 / 16;
    const R = new Room("ilex_forest", { name: "Ilex Forest", floor: "Azalea Town to Route 34", W: F.W, D: F.D, H: 10, outside: true, night: true });
    const G = R.geo;
    const at = (x, z) => { const r = F.rows[z]; return { g: r[3 * x], w: r[3 * x + 1], h: r.charCodeAt(3 * x + 2) - 48 }; };
    const sd = F.southDoor, nd = F.northDoor;
    const doorStep = (x, z) => (x >= sd.x0 && x < sd.x1 && z >= sd.s0 && z <= sd.s1) || (x >= nd.x0 && x < nd.x1 && z >= nd.s0 && z <= nd.s1);
    for (let z = 0; z < F.D; z++) {
      for (let x = 0; x < F.W;) {
        const g = at(x, z).g; let x1 = x + 1;
        while (x1 < F.W && at(x1, z).g === g) x1++;
        if (g === "~") G.box(x, -1, z, x1, -0.15, z + 1, { top: "water", all: "soil" });
        else G.box(x, -1, z, x1, 0, z + 1, { top: g === "m" ? "grassMown" : g === "d" ? "area_dripstone" : "grass", all: "soil" });
        if (g === "~") R.solid(x, z, x1, z + 1);
        x = x1;
      }
      for (let x = 0; x < F.W;) {
        const c = at(x, z);
        if (!"TWUHS".includes(c.w) || doorStep(x, z)) { x++; continue; }
        let x1 = x + 1;
        while (x1 < F.W && at(x1, z).w === c.w && at(x1, z).h === c.h && !doorStep(x1, z)) x1++;
        if (c.w === "W") { G.box(x, 0, z, x1, 2, z + 1, "bark"); G.box(x, 2, z, x1, c.h, z + 1, "treeLeaf"); }
        else if (c.w === "T") G.box(x, 0, z, x1, c.h, z + 1, "treeLeaf");
        else if (c.w === "H") G.box(x, 0, z, x1, Math.max(1, c.h), z + 1, { all: "stPillar", top: "manCap" });
        else if (c.w === "S") G.box(x, 0, z + 0.3, x1, 1.6, z + 0.7, { all: "area_sign_front_l" });
        else G.box(x, 0, z, x1, c.h, z + 1, { top: "area_gate_roof", all: "area_gate_wall_birch" });
        R.solid(x, z, x1, z + 1);
        x = x1;
      }
    }
    // Its tall grass (the light green patches): wild POKéMON in the game.
    for (const [d, z0, z1, face] of [[sd, sd.s0, sd.s1 + 1, "n"], [nd, nd.s0, nd.s1 + 1, "s"]]) {
      G.box(d.x0, 0, z0, d.x1, 0.5, z1, "area_gate_stone");
      const zf = face === "n" ? z1 : z0;
      G.box(d.x0, 0.5, zf - 0.02, d.x1, 2.8, zf + 0.02, { [face]: "doorDark", all: null });
      G.box(d.x0 - 1, 3, z0, d.x1 + 1, 16, z1, { top: "area_gate_roof", all: "area_gate_wall_birch" });
    }
    R.warps.push({ test: (p, input) => input.down && p.z > sd.z && p.x > sd.x0 && p.x < sd.x1, to: "town", fx: "fade" });
    R.warps.push({ test: (p, input) => input.up && p.z < nd.z && p.x > nd.x0 && p.x < nd.x1, to: "town", fx: "fade" });
    for (const a of F.acts) R.act(a.rect, { label: a.label, lines: a.lines || ["There's something here!"], use: a.use });
    for (const it of F.items) { roundBall(G, it.x, 0, it.z, 0.42); R.act([it.x - 0.7, it.z - 0.7, it.x + 0.7, it.z + 0.7], { label: "Pick up", lines: ["An item ball! (" + it.item.toUpperCase().replace(/_/g, " ") + ")"] }); }
    for (const p of F.people) R.npcs.push({ name: p.name.split(" ").map(w => w[0] + w.slice(1).toLowerCase()).join(" "), key: p.key, x: p.x, z: p.z, yaw: (180 - p.yaw) * Math.PI / 180, look: { skin: p.skin }, talk: () => p.lines.map(text => ({ text })) });
    for (const m of F.mons) { G.box(m.x - 0.3, 0, m.z - 0.3, m.x + 0.3, 0.7, m.z + 0.3, { all: "statueStone" }); R.act([m.x - 0.7, m.z - 0.7, m.x + 0.7, m.z + 0.7], { label: "Look", lines: ["FARFETCH'D: Kwaa!"] }); }
    R.spawn = { x: F.fromSouth.x, z: F.fromSouth.z, yaw: FACE.n };
    R.item("Ilex Forest", "John's forest (152 by 224 blocks: HeartGold's, two blocks a tile). In from Azalea Town's gate at the bottom; out of the north gatehouse to Route 34.");
    R.item("People", "The charcoal maker's apprentice and his lost FARFETCH'D, the charcoal maker by the way in, the HEADBUTT man.");
    R.item("Items", "Four item balls and nine hidden items (look at the spot), where HeartGold has them.");
    return R;
  };
})();
