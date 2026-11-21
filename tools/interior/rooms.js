// ---------- the places: your house (downstairs and your room), the rival's house (the same space), and Pallet Town outside ----------
// Inside, a room is 16 blocks wide (west to east) and 14 deep (north to south), its walls 4 high: twice the old games' 8 by 7
// tiles of floor. Outside, the houses are 8 by 6, so they're bigger on the inside, as the old games were.
const FACE = { n: 0, w: Math.PI / 2, s: Math.PI, e: -Math.PI / 2 };
const DOOR = [6, 8];
const PLACES = [
  { id: "house2", name: "Your room", floor: "Upstairs", group: "Your house" },
  { id: "house1", name: "Your house", floor: "Downstairs", group: "Your house" },
  { id: "rival1", name: "Rival's house", floor: "Downstairs", group: "Rival's house" },
  { id: "rival2", name: "Rival's room", floor: "Upstairs", group: "Rival's house" },
  { id: "lab", name: "Prof. Oak's lab", floor: "Ground floor", group: "Prof. Oak's lab" },
  { id: "center", name: "Pokémon Center", floor: "Viridian City", group: "Viridian City" },
  { id: "mart", name: "Poké Mart", floor: "Viridian City", group: "Viridian City" },
  { id: "school", name: "Trainer School", floor: "1F", group: "Viridian City" },
  { id: "nickhouse", name: "Viridian house", floor: "Ground floor", group: "Viridian City" },
  { id: "gate_s", name: "Route 2 gate", floor: "South", group: "Route 2" },
  { id: "gate_n", name: "Route 2 gate", floor: "North", group: "Route 2" },
  { id: "arena_lab", name: "Lab battle", floor: "Battle arena", group: "Battle arenas" },
  { id: "town", name: "Pallet Town", floor: "Outside", group: "Outside" },
];
// Your house and the rival's (14 by 13, the mat in a nook at the door), the stairs at the back on the right.
const FROM_TOWN = { x: 7.0, z: 11.5, yaw: FACE.n };
const LAB_DOOR = [7, 9], LAB_IN = { x: 8.0, z: 14.5, yaw: FACE.n }, FROM_LAB = { x: 25.0, z: 23.1, yaw: FACE.s };
const STAIRS_FOOT = { x: 9.1, z: 1.1, yaw: FACE.s };
const STAIRS_TOP = { x: 13.5, z: 2.9, yaw: FACE.s };

// The Pokémon Center's and the Poké Mart's ways in (2 wide, in the middle of the front), and where you stand coming in.
const CENTER_DOOR = [8, 10], CENTER_IN = { x: 9.0, z: 12.5, yaw: FACE.n };
const MART_DOOR = [6, 8], MART_IN = { x: 7.0, z: 10.5, yaw: FACE.n };
// The Trainer House (Viridian City): its way in, and the stairs between its floors (as your house's are).
// Viridian Forest and its two gatehouses (Route 2): the south gate's way in, its door through to the forest, the
// forest's ways out (south at the bottom right, north at the top left), the north gate's.
const GATE_DOOR = [6, 8], FOREST_W = 36, FOREST_D = 52, FOREST_IN = [28, 30], FOREST_OUT = [5, 7];
const GS_IN = { x: 7.0, z: 8.5, yaw: FACE.n }, GS_FROM_FOREST = { x: 7.0, z: 2.6, yaw: FACE.s };
const FOREST_FROM_S = { x: 29.0, z: FOREST_D - 2.6, yaw: FACE.n }, FOREST_FROM_N = { x: 6.0, z: 2.6, yaw: FACE.s };
const GN_FROM_FOREST = { x: 7.0, z: 8.5, yaw: FACE.n }, GN_IN = { x: 7.0, z: 2.6, yaw: FACE.s };
const TH_DOOR = [7, 9], TH_IN = { x: 8.0, z: 12.5, yaw: FACE.n }, TH_FOOT = { x: 11.1, z: 1.1, yaw: FACE.s }, TH_TOP = { x: 15.5, z: 2.9, yaw: FACE.s };
// Viridian's two green-roofed buildings: the Trainer School above the Center (the Trainer House's battle room under
// it), and the house above that. Both small, as their outsides are (10 by 6).
const SCHOOL_DOOR = [5, 7], SCHOOL_IN = { x: 6.0, z: 9.5, yaw: FACE.n }, SCHOOL_TOP = { x: 13.5, z: 2.9, yaw: FACE.s };
const VHOUSE_DOOR = [5, 7], VHOUSE_IN = { x: 6.0, z: 9.5, yaw: FACE.n };
// The Route 2 gates, as Let's Go has them (one design for every gate, its people its own): in from the route at the
// south, a long blue runner to the way on at the north.
const LG_GATE_DOOR = [6, 8], LG_GATE_IN = { x: 7.0, z: 10.5, yaw: FACE.n }, LG_GATE_FROM_N = { x: 7.0, z: 2.6, yaw: FACE.s };

// The Trainer House's big screen: the battle downstairs, two trainers and a ball flying between, VS flashing.
function drawTrainerTV(P, f) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, mixc(hx("#5A8EE0"), hx("#1E2A4A"), y / P.h));
  P.rect(0, P.h - 7, P.w, 7, hx("#6DB55A")); P.rect(Math.floor(P.w / 2), P.h - 7, 1, 7, hx("#F6F1EA"));
  P.rect(5, P.h - 13, 3, 6, hx("#E5525C")); P.rect(5, P.h - 15, 3, 2, hx("#F2C79A"));
  P.rect(P.w - 8, P.h - 13, 3, 6, hx("#3A6FD0")); P.rect(P.w - 8, P.h - 15, 3, 2, hx("#F2C79A"));
  const t = (f % 16) / 16, bx = 9 + t * (P.w - 18), by = P.h - 12 - Math.sin(t * Math.PI) * 6;
  P.disc(bx, by, 1.6, hx("#E5525C")); P.rect(Math.round(bx) - 1, Math.round(by), 3, 1, hx("#F4F4F4"));
  if (f % 4 < 2) P.text("VS", Math.floor(P.w / 2) - 5, 2, hx("#FFE04A"));
}
// Downstairs, the scoreboard: TODAY on top, a row of lights going round.
function drawScoreboard(P, f) {
  P.fill(hx("#1A2130"));
  P.text("TODAY", Math.floor(P.w / 2) - 12, 3, hx("#FFE04A"));
  for (let i = 0; i < 8; i++) P.rect(4 + i * 5, P.h - 6, 3, 3, (i === f % 8) ? hx("#8FF5C0") : hx("#2E3A50"));
}

// ---------- the Center's and the Mart's own materials ----------
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // (The sign font has no accents: POKéMON's é is drawn as an E.)
  if (!FONT3["É"]) FONT3["É"] = FONT3["E"];
  reg("mcCenterCheck", 32, 32, (P, r) => mcCheck(P, r, hx("#F7F2EA"), hx("#F2D6CF")), { per: 2 });
  reg("paperCenter", 16, 16, (P, r) => mcStone(P, r, hx("#F8F5F1"), 0.03, 2));
  reg("paperMart", 16, 16, (P, r) => mcStone(P, r, hx("#F2F6FA"), 0.03, 2));
  reg("blueTrim", 16, 16, (P, r) => { P.fill(hx("#3A6FD0"), 0.04, r); P.rect(0, 0, 16, 2, hx("#6E9AE8")); });
  // A counter's front: its colour, a white stripe near the top, a darker kick at the foot.
  const front = c => (P, r) => { P.fill(hx(c), 0.035, r); P.rect(0, 2, 16, 2, hx("#F6F4EE")); P.rect(0, 14, 16, 2, shadec(hx(c), -0.25)); };
  reg("counterRed", 16, 16, front("#E5525C"));
  reg("counterBlue", 16, 16, front("#3A6FD0"));
  // Goods on a shelf: bottles and boxes and balls, row on row (two blocks across, half a block high).
  const goods = (seed) => (P, r) => {
    P.fill(hx("#E9EEF2"), 0.02, r);
    let x = 0, i = seed;
    const cols = ["#8E6BC8", "#5AA9E6", "#F2C744", "#5DAE52", "#E5525C", "#F08A3C", "#F08AA8", "#4C86C8"];
    while (x < 32) {
      const kind = (i * 7 + seed) % 4, c = hx(cols[(i * 3 + seed) % cols.length]);
      if (kind === 0) { // a bottle
        P.rect(x + 1, 3, 3, 5, c); P.rect(x + 2, 1, 1, 2, hx("#D7DEE3")); P.set(x + 1, 4, shadec(c, 0.35)); x += 4;
      } else if (kind === 1) { // a box
        P.rect(x, 2, 5, 6, c); P.rect(x, 2, 5, 1, shadec(c, 0.25)); P.rect(x + 1, 4, 3, 2, hx("#F6F4EE")); x += 6;
      } else if (kind === 2) { // a ball
        P.rect(x + 1, 3, 4, 2, hx("#E5525C")); P.rect(x + 1, 5, 4, 2, hx("#F4F4F4")); P.rect(x + 1, 5, 4, 1, hx("#2B2522")); P.set(x + 3, 5, hx("#F4F4F4")); x += 6;
      } else { // a tall spray can
        P.rect(x + 1, 2, 3, 6, c); P.rect(x + 1, 1, 3, 1, hx("#B9C3C9")); P.rect(x + 1, 5, 3, 1, hx("#F6F4EE")); x += 4;
      }
      i++;
    }
    P.rect(0, 0, 32, 1, hx("#C9D3D8"));
  };
  reg("goodsA", 32, 8, goods(1), { per: [2, 0.5] });
  reg("goodsB", 32, 8, goods(3), { per: [2, 0.5] });
  reg("goodsC", 32, 8, goods(6), { per: [2, 0.5] });
  // A drinks cooler's glass door: bottles in bright rows behind it, lit.
  reg("cooler", 16, 32, (P, r) => {
    P.fill(hx("#DDEFF8"), 0.02, r);
    const cols = ["#E5525C", "#5AA9E6", "#F2C744", "#5DAE52", "#F08A3C"];
    for (let row = 0; row < 4; row++) { const y = 3 + row * 7; P.rect(1, y + 5, 14, 1, hx("#9FB4C0")); for (let k = 0; k < 4; k++) { const c = hx(cols[(row + k) % cols.length]); P.rect(2 + k * 3.4, y, 2, 5, c); P.set(2 + k * 3.4, y + 1, shadec(c, 0.4)); } }
    P.rect(0, 0, 16, 1, hx("#7D8990")); P.rect(0, 31, 16, 1, hx("#7D8990")); P.rect(0, 0, 1, 32, hx("#7D8990")); P.rect(15, 0, 1, 32, hx("#7D8990")); P.rect(13, 12, 1, 8, hx("#B9C3C9"));
  }, { fit: true, glow: 0.55 });
  // Let's Go's Mart floor: pale mint and white check.
  reg("mcMintCheck", 32, 32, (P, r) => mcCheck(P, r, hx("#EEF8F1"), hx("#C5E8D3")), { per: 2 });
  reg("yellowFabric", 16, 16, (P, r) => fabric(P, r, hx("#F2C744")));
  reg("mintFabric", 16, 16, (P, r) => fabric(P, r, hx("#5BC2A8")));
  // A dark way through (the top of the Center's stairs).
  reg("doorDark", 8, 8, (P, r) => { P.fill(hx("#1A1E2A"), 0.04, r); P.rect(0, 7, 8, 1, hx("#262C3B")); });
  // The till's little screen: green figures.
  reg("tillLcd", 16, 8, (P, r) => { P.fill(hx("#1F3B2E")); P.text("120", 2, 2, hx("#8FF5C0")); });
  // A chest freezer seen through its glass lid: tubs and lollies in bright rows on frost.
  reg("freezerTop", 32, 16, (P, r) => {
    P.fill(hx("#E3F3FA"), 0.03, r);
    const cols = ["#F08AA8", "#F2C744", "#8E6BC8", "#5AA9E6", "#5DAE52", "#F6F4EE", "#E5525C"];
    for (let row = 0; row < 3; row++) for (let k = 0; k < 7; k++) { const c = hx(cols[(row * 3 + k) % cols.length]), x = 1 + k * 4.4 + (row % 2) * 1.5, y = 1 + row * 5;
      if ((row + k) % 3 === 0) { P.disc(x + 1.5, y + 2, 1.8, c); P.set(x + 1, y + 1, shadec(c, 0.35)); } else { P.rect(Math.round(x), y, 3, 4, c); P.rect(Math.round(x), y, 3, 1, shadec(c, 0.3)); } }
  }, { fit: true, glow: 0.3 });
  // Goods under a display case's glass, from above.
  reg("goodsTop", 32, 16, (P, r) => {
    P.fill(hx("#F6F4EE"), 0.02, r);
    const cols = ["#E5525C", "#5AA9E6", "#F2C744", "#8E6BC8", "#5DAE52", "#F08A3C"];
    for (let k = 0; k < 6; k++) { const x = 1 + k * 5, c = hx(cols[k]);
      if (k % 2 === 0) { P.disc(x + 2, 5, 2, hx("#E5525C")); P.rect(x, 5, 5, 1, hx("#2B2522")); P.disc(x + 2, 11, 2, hx("#3A6FD0")); P.rect(x, 11, 5, 1, hx("#2B2522")); }
      else { P.rect(x, 2, 4, 5, c); P.rect(x + 1, 3, 2, 1, hx("#F6F4EE")); P.rect(x, 9, 4, 5, shadec(c, -0.15)); } }
  }, { fit: true });
  // The world map on the walls: John's Kanto from above (tools/worldmap.py, the Town Map's own painting), in a
  // dark frame with a cream mount.
  reg("worldMap", WORLDMAP.w + 6, WORLDMAP.h + 6, (P) => {
    const b = atob(WORLDMAP.d);
    P.fill(hx("#22344F")); P.rect(1, 1, WORLDMAP.w + 4, WORLDMAP.h + 4, hx("#F6EBD2"));
    for (let y = 0; y < WORLDMAP.h; y++) for (let x = 0; x < WORLDMAP.w; x++) { const i = (y * WORLDMAP.w + x) * 3; P.set(x + 3, y + 3, [b.charCodeAt(i), b.charCodeAt(i + 1), b.charCodeAt(i + 2)]); }
  }, { fit: true });
  // Each room's own mat at the door, as carpet: its colour, a border, and a ball (or stripes) in the middle.
  const mat = (base, edge, north) => (P, r) => {
    const cream = hx("#F6F1EA"), w = P.w, cx = w / 2 - 16;
    P.fill(hx(base), 0.04, r);
    P.rect(0, 0, w, 2, hx(edge)); P.rect(0, 30, w, 2, hx(edge)); P.rect(0, 0, 2, 32, hx(edge)); P.rect(w - 2, 0, 2, 32, hx(edge));
    P.rect(4, 4, w - 8, 1, cream); P.rect(4, 27, w - 8, 1, cream); P.rect(4, 4, 1, 24, cream); P.rect(w - 5, 4, 1, 24, cream);
    // Two chevrons in the middle, pointing south (out).
    // (Pointing north for a way through the north wall.)
    for (const y0 of [8, 16]) for (let k = 0; k < 7; k++) { const y = north ? 30 - (y0 + k) - 2 : y0 + k; P.rect(cx + 8 + k, y, 2, 2, cream); P.rect(cx + 22 - k, y, 2, 2, cream); }
  };
  // Each mat, and a wide one (4 across, for the nooks at the doors).
  const matReg = (k, base, edge, north) => { reg(k, 32, 32, mat(base, edge, north), { fit: true }); reg(k + "W", 64, 32, mat(base, edge, north), { fit: true }); };
  matReg("matHome", "#C9433F", "#8E2A2A");
  matReg("matRival", "#33477A", "#1E2A4A");
  matReg("matLab", "#2F9C8A", "#1D6B5E");
  matReg("matCenter", "#F08A3C", "#C7502E");
  matReg("matMart", "#3A6FD0", "#24479A");
  matReg("matTrainer", "#E8823A", "#A9531F");
  matReg("matForest", "#4E9A4A", "#2F6B33");
  matReg("matForestN", "#4E9A4A", "#2F6B33", true);
  // The forest's own: the ground under the trees, a wall of leaves round it, the gates' boards and paper.
  reg("forestGround", 16, 16, (P, r) => { P.fill(hx("#7DB65A"), 0.08, r); for (let i = 0; i < 10; i++) P.set(r() * 16, r() * 16, hx("#6AA24C")); });
  reg("forestPath", 16, 16, (P, r) => mcStone(P, r, hx("#D8BE88"), 0.05, 4));
  reg("leafWall", 16, 16, (P, r) => leaves(P, r, hx("#3F8A44"), hx("#6DBA5A"), hx("#255C2D"), 0));
  reg("paperGate", 16, 16, (P, r) => mcStone(P, r, hx("#EEF1E6"), 0.03, 2));
  reg("gateBoards", 16, 16, (P, r) => { P.fill(hx("#C9A36B"), 0.05, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#B08A55")); });
  reg("greenTrim", 16, 16, (P, r) => { P.fill(hx("#4E9A4A"), 0.04, r); P.rect(0, 0, 16, 2, hx("#78C06E")); });
  // The Trainer House's own: warm cream walls (and cool grey downstairs), an orange trim, a peach check floor,
  // its counter, the battle room's wooden court floor.
  reg("paperTrainer", 16, 16, (P, r) => mcStone(P, r, hx("#FBF3E8"), 0.03, 2));
  reg("paperTrainerB", 16, 16, (P, r) => mcStone(P, r, hx("#E8EDF5"), 0.03, 2));
  reg("orangeTrim", 16, 16, (P, r) => { P.fill(hx("#E8823A"), 0.04, r); P.rect(0, 0, 16, 2, hx("#F4A866")); });
  reg("counterOrange", 16, 16, front("#E8823A"));
  reg("mcPeachCheck", 32, 32, (P, r) => mcCheck(P, r, hx("#FBF1E4"), hx("#F2D4B8")), { per: 2 });
  reg("courtWood", 16, 16, (P, r) => { P.fill(hx("#D9A86A"), 0.05, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#C79358")); });
  // The Trainer School's: a chalkboard, warm wood, yellow desks. The Let's Go gate's: a cream and yellow check, the
  // blue lower walls, round brown stools, a red bin. The Viridian house's mat.
  reg("chalkboard", 64, 32, (P, r) => {
    P.fill(hx("#2F5D46"), 0.05, r);
    const chalk = hx("#E8F0E4");
    P.text("STATUS", 20, 3, hx("#FFE9A8"));
    P.text("SLP PSN PAR", 9, 12, chalk); P.text("BRN FRZ", 17, 20, chalk);
    P.rect(4, 28, 10, 1, chalk); P.rect(48, 27, 8, 2, hx("#F2F2F2"));
  }, { fit: true });
  reg("bookOpen", 16, 16, (P, r) => { P.fill(hx("#F8F4EA"), 0.02, r); P.rect(7, 0, 2, 16, hx("#C9BFA8")); for (let y = 3; y < 14; y += 3) { P.rect(1, y, 5, 1, hx("#8A8F9E")); P.rect(10, y, 5, 1, hx("#8A8F9E")); } });
  reg("schoolWood", 16, 16, (P, r) => { P.fill(hx("#C98F55"), 0.05, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#B37B45")); });
  reg("deskYellow", 16, 16, (P, r) => { P.fill(hx("#F2C14E"), 0.04, r); P.rect(0, 0, 16, 1, hx("#F8D77E")); });
  reg("paperSchool", 16, 16, (P, r) => mcStone(P, r, hx("#F6EEDC"), 0.03, 2));
  reg("schoolTrim", 16, 16, (P, r) => { P.fill(hx("#9C6A3C"), 0.04, r); P.rect(0, 0, 16, 2, hx("#BE8A57")); });
  reg("gateCheck", 32, 32, (P, r) => mcCheck(P, r, hx("#EED690"), hx("#DDB662")), { per: 2 });
  reg("houseBoards", 32, 32, (P, r) => planks(P, r, hx("#9A6438"), hx("#B27A4A"), hx("#7C4E2A"), hx("#4E2E16"), 8, 32), { per: 2 });
  reg("beam", 16, 16, (P, r) => { P.fill(hx("#8A5A34"), 0.05, r); P.rect(0, 0, 2, 16, hx("#A8744A")); P.rect(14, 0, 2, 16, hx("#6A4224")); });
  reg("rugGreen", 32, 32, (P, r) => { P.fill(hx("#6FB55A"), 0.04, r); P.rect(0, 0, 32, 2, hx("#4E8E40")); P.rect(0, 30, 32, 2, hx("#4E8E40")); P.rect(0, 0, 2, 32, hx("#4E8E40")); P.rect(30, 0, 2, 32, hx("#4E8E40"));
    P.rect(4, 4, 24, 1, hx("#A8D88E")); P.rect(4, 27, 24, 1, hx("#A8D88E")); P.rect(4, 4, 1, 24, hx("#A8D88E")); P.rect(27, 4, 1, 24, hx("#A8D88E")); }, { fit: true });
  reg("paperGateLG", 16, 16, (P, r) => mcStone(P, r, hx("#F7F1E2"), 0.03, 2));
  reg("gateBlue", 16, 16, (P, r) => { P.fill(hx("#5C8FB8"), 0.04, r); P.rect(0, 0, 16, 1, hx("#7FAAD0")); });
  reg("gateWhite", 16, 16, (P, r) => P.fill(hx("#F4F6F4"), 0.02, r));
  reg("stoolTop", 16, 16, (P, r) => { P.fill(hx("#9A5B34"), 0.05, r); P.rect(0, 0, 16, 1, hx("#B8774A")); });
  reg("binRed", 16, 16, (P, r) => { P.fill(hx("#E0603A"), 0.04, r); P.rect(0, 0, 16, 2, hx("#F08A62")); });
  matReg("matSchool", "#9C6A3C", "#6B4524");
  // The redesign from Let's Go: the lab's purple mat and its pale oak boards laid on the diagonal; the Mart's blue and
  // white diamonds; dark wide boards (the school and the Viridian house); blush pink and blue corner posts; green
  // cushions and quilt, a green felt top, a white lamp shade, cardboard, a DVD player's front.
  matReg("matLabPurple", "#8F60DE", "#5E3AA6");
  reg("oakDiagonal", 32, 32, (P, r) => {
    const base = hx("#F3E8D3"), gap = hx("#CDBB9C"), tones = [];
    for (let k = 0; k < 4; k++) tones.push((r() - 0.5) * 0.1);
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const d = (x + y) % 32, row = Math.floor(d / 8), along = (x - y + 64) % 32;
      let c = shadec(base, tones[row] + (r() - 0.5) * 0.04 + Math.sin(along * 0.6 + row * 2) * 0.02);
      if (d % 8 === 0) c = gap; else if ((along + row * 11) % 32 === 0) c = shadec(base, -0.2);
      P.set(x, y, c);
    }
  }, { per: 2 });
  reg("mcDiamondBlue", 32, 32, (P, r) => {
    const a = hx("#F4F8FB"), b = hx("#8EC0E6"), line = hx("#C8DCEA");
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const u = (x + y) % 32, v = (x - y + 32) % 32;
      let c = shadec((u < 16) === (v < 16) ? a : b, (r() - 0.5) * 0.04);
      if (u === 0 || u === 16 || v === 0 || v === 16) c = mixc(c, line, 0.6);
      P.set(x, y, c);
    }
  }, { per: 2 });
  reg("midBoards", 32, 32, (P, r) => planks(P, r, hx("#A9764A"), hx("#C28E5E"), hx("#8C5C36"), hx("#5C3B1F"), 8, 32), { per: 2 });
  reg("darkBoards", 32, 32, (P, r) => planks(P, r, hx("#6E4426"), hx("#84522E"), hx("#583418"), hx("#3A220F"), 8, 32), { per: 2 });
  const post = (c, l, d) => (P, r) => { P.fill(hx(c), 0.04, r); P.rect(0, 0, 2, 16, hx(l)); P.rect(14, 0, 2, 16, hx(d)); };
  reg("postPink", 16, 16, post("#E9B7BC", "#F6D2D6", "#C9939A"));
  reg("postBlue", 16, 16, post("#4A86D8", "#7CAAF0", "#2F62AE"));
  reg("greenFabric", 16, 16, (P, r) => fabric(P, r, hx("#6FB55A")));
  reg("quiltGreen", 16, 16, (P, r) => {
    const cols = [hx("#5DAE52"), hx("#8ACB6E"), hx("#3F8A44"), hx("#F4EEE2")];
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) { const c = cols[(tx * 3 + ty) % 4]; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(c, ((x + y) % 2 ? 0.03 : -0.03) + (x === 0 || y === 0 ? -0.12 : 0))); }
  });
  reg("feltGreen", 16, 16, (P, r) => fabric(P, r, hx("#86B86E"), 0.04));
  reg("lampWhite", 16, 16, (P, r) => P.fill(hx("#F7F5EE"), 0.02, r), { glow: 0.6 });
  reg("cardboard", 16, 16, (P, r) => { P.fill(hx("#C9A06A"), 0.05, r); P.rect(0, 7, 16, 2, hx("#E0C48E")); });
  reg("dvdFront", 16, 8, (P, r) => { P.fill(hx("#2A2D33"), 0.03, r); P.rect(2, 3, 8, 1, hx("#111318")); P.set(13, 3, hx("#58D3A0")); }, { fit: true });
  // Rugs: Mum's salmon one under the table, green and blue kitchen mats, your big red rug, the Center's ring at the door.
  RUGS.salmon = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#E07A6A" : ((i + j) % 2 ? "#F4A08E" : "#F6AE9C");
  RUGS.kitchenGreen = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#3F8A44" : "#5DAE52";
  RUGS.kitchenBlue = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#2E4A7A" : "#4A6FA8";
  RUGS.redRoom = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#9A2C34" : ((i + j) % 2 ? "#D9424A" : "#CF3A42");
  RUGS.centerRing = (i, j, w, d) => { const t = Math.hypot((i + 0.5 - w / 2) / (w / 2), (j + 0.5 - d / 2) / (d / 2)); return t > 1.05 ? "#F4A866" : (t > 0.66 && t < 0.92) || t < 0.22 ? "#F6F1EA" : "#E8823A"; };
  matReg("matHouseV", "#3E8A6E", "#2A5E4B");
  matReg("matGate", "#3A63B8", "#25418A");
  matReg("matGateN", "#3A63B8", "#25418A", true);
  // The gate's runner: blue, a yellow line round it.
  RUGS.gateRun = (i, j, w, d) => (i === 0 || i === w - 1 || j === 0 || j === d - 1) ? "#E9D27A" : (j % 3 === 1 ? "#5A8EE0" : "#4A7FD4");
  RUGS.thRun = (i, j, w, d) => (i === 0 || i === w - 1) ? "#FBF1E4" : (j % 3 === 1 ? "#F09A56" : "#E8823A");
  // The battle court: green, white lines round it and across its middle.
  RUGS.court = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1 || j === Math.floor(d / 2)) ? "#F6F1EA" : ((i + j) % 2 ? "#7FC36A" : "#6DB55A");
  // The Center's runner, door to counter: red, cream edges. The Mart's: blue, white edges.
  RUGS.centerRun = (i, j, w, d) => (i === 0 || i === w - 1) ? "#F6F1EA" : (j % 3 === 1 ? "#E26A64" : "#D9524E");
  RUGS.martRun = (i, j, w, d) => (i === 0 || i === w - 1) ? "#F2F6FA" : (j % 3 === 1 ? "#5A8EE0" : "#3A6FD0");
  RUGS.center = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#D9524E" : (i === 1 || j === 1 || i === w - 2 || j === d - 2) ? "#F6F1EA" : ((i + j) % 2 ? "#E87A72" : "#D9524E");
  RUGS.mart = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#3A6FD0" : ((i + j) % 2 ? "#E8F2F8" : "#9CC8E6");
})();

// A band of colour round the three walls you can see, at a height.
function band(room, y, mat, h = 0.22) {
  const G = room.geo, W = room.W, D = room.Dm || room.D;
  G.box(0, y, 0, W, y + h, 0.04, { s: mat, all: null }); G.box(0, y, 0, 0.04, y + h, D, { e: mat, all: null }); G.box(W - 0.04, y, 0, W, y + h, D, { w: mat, all: null });
}
// Shelves of goods (the Mart's): white uprights, a coloured kick, goods on every shelf.
function martShelf(room, X, Z, f, w = 4, h = 2.2, o = {}) {
  const F = room.F(X, Z, f), d = o.depth || 0.8, wood = "shelfWhite", rows = Math.max(2, Math.floor((h - 0.2) / 0.6)), fill = ["goodsA", "goodsB", "goodsC"];
  F.box(0, 0, 0, 0.08, h, d, wood); F.box(w - 0.08, 0, 0, w, h, d, wood); F.box(0, h - 0.06, 0, w, h, d, wood);
  F.box(0.08, 0, 0, w - 0.08, 0.18, d, { all: wood, front: o.kick || "blueTrim" });
  if (!o.open) F.box(0.08, 0.18, 0, w - 0.08, h - 0.06, 0.05, wood);
  for (let i = 0; i < rows; i++) {
    const y = 0.18 + i * 0.6, m = fill[(i + (o.seed || 0)) % fill.length];
    F.box(0.08, y, 0.05, w - 0.08, y + 0.04, d, wood);
    F.box(0.12, y + 0.04, 0.1, w - 0.12, y + 0.5, d - 0.06, { front: m, all: "linen", top: "linen" });
  }
  F.solid(0, 0, w, d);
  room.act(F.rect(0, 0.2, w, d + 0.6), { label: "Look", lines: o.lines || ["Shelves of POTIONS, ANTIDOTES and POKé BALLS. The clerk at the counter sells them."] });
  room.item("Mart shelves", "White shelving with a coloured kick, goods packed on every shelf: bottles, boxes, sprays and balls.");
}
function cooler(room, X, Z, f, w = 2) {
  const F = room.F(X, Z, f), d = 0.9;
  F.box(0, 0, 0, w, 2.5, d, { all: "cupboardWhite", front: null });
  for (let i = 0; i < Math.round(w); i++) F.box(i + 0.05, 0.15, d - 0.02, i + 0.95, 2.35, d, { front: "cooler", all: null });
  F.box(0, 2.5, 0, w, 2.7, d, { all: "blueTrim" });
  F.solid(0, 0, w, d);
  room.act(F.rect(0, 0, w, d + 0.6), { label: "Look", lines: ["A cooler of drinks: FRESH WATER, SODA POP and LEMONADE, all nicely cold."] });
  room.item("Drinks cooler", "Glass doors, bright bottles in rows, lit from inside.");
}

function floorLamp(room, x, z) {
  const G = room.geo;
  G.box(x - 0.25, 0, z - 0.25, x + 0.25, 0.08, z + 0.25, "walnut"); G.box(x - 0.04, 0.08, z - 0.04, x + 0.04, 1.75, z + 0.04, "steel");
  G.box(x - 0.32, 1.6, z - 0.32, x + 0.32, 2.05, z + 0.32, "lampShade");
  room.solid(x - 0.25, z - 0.25, x + 0.25, z + 0.25);
  room.item("Floor lamp", "Walnut foot, steel stem, a cream shade that glows.");
}
function fruitBowl(room, x, z, y = 0.86) {
  const G = room.geo;
  G.box(x - 0.3, y, z - 0.3, x + 0.3, y + 0.14, z + 0.3, "potWhite");
  for (const [dx, dz, m] of [[-0.12, -0.08, "redFabric"], [0.1, -0.1, "cushion"], [0, 0.12, "leafSolid"], [0.13, 0.1, "redFabric"]]) G.box(x + dx - 0.09, y + 0.12, z + dz - 0.09, x + dx + 0.09, y + 0.3, z + dz + 0.09, m);
}
function plates(room, pts, y = 0.86) { for (const [x, z] of pts) room.geo.box(x - 0.22, y, z - 0.22, x + 0.22, y + 0.03, z + 0.22, "potWhite"); }
function bagOnFloor(room, x, z, lines) {
  const G = room.geo;
  G.box(x - 0.4, 0, z - 0.25, x + 0.4, 0.55, z + 0.25, { all: "bag", top: "redFabric" }); G.box(x - 0.25, 0.55, z - 0.05, x + 0.25, 0.7, z + 0.05, "walnut");
  room.solid(x - 0.4, z - 0.25, x + 0.4, z + 0.25);
  room.act([x - 0.4, z - 0.25, x + 0.4, z + 0.25], { label: "Look", lines });
  room.item("Bag", "Red bag with yellow straps, packed for the road.");
}

function beanBag(room, x, z, mat) {
  const G = room.geo;
  G.box(x - 0.55, 0, z - 0.55, x + 0.55, 0.35, z + 0.55, mat); G.box(x - 0.45, 0.35, z - 0.45, x + 0.45, 0.55, z + 0.45, mat); G.box(x - 0.5, 0.35, z - 0.55, x + 0.5, 0.95, z - 0.25, mat);
  room.solid(x - 0.55, z - 0.55, x + 0.55, z + 0.55);
  room.act([x - 0.55, z - 0.55, x + 0.55, z + 0.55], { label: "Sit", lines: ["A beanbag, squashed flat from use."] });
  room.item("Beanbag", "A slumped red beanbag.");
}
function dumbbells(room, x, z) {
  const G = room.geo;
  G.box(x - 0.6, 0, z - 0.25, x + 0.6, 0.5, z + 0.25, { all: "steel", top: "rubber" });
  for (const dx of [-0.35, 0.05]) { G.box(x + dx, 0.5, z - 0.12, x + dx + 0.06, 0.66, z + 0.12, "rubber"); G.box(x + dx + 0.06, 0.55, z - 0.03, x + dx + 0.24, 0.61, z + 0.03, "steel"); G.box(x + dx + 0.24, 0.5, z - 0.12, x + dx + 0.3, 0.66, z + 0.12, "rubber"); }
  room.solid(x - 0.6, z - 0.25, x + 0.6, z + 0.25);
  room.act([x - 0.6, z - 0.3, x + 0.6, z + 0.3], { label: "Lift", lines: ["Dumbbells. You can barely lift one. He's labelled them LIGHT."] });
  room.item("Dumbbells", "Two dumbbells on a little steel rack.");
}
// ---------- the lab's own pieces ----------
function labMachine(room, X, w, screenKey, o = {}) {
  const F = room.F(X, 0, "s");
  F.box(0, 0, 0, w, 2.7, 1.0, { all: "greyPlastic", front: "labPanel", back: null, bottom: null });
  F.box(0.1, 2.7, 0.1, w - 0.1, 2.95, 0.9, { all: "vent" });
  F.box(0.35, 1.45, 1.0, w - 0.35, 2.4, 1.02, { front: screenKey, all: null });
  F.box(0.3, 0.95, 1.0, w - 0.3, 1.05, 1.25, "steel");
  F.solid(0, 0, w, 1.25);
  room.act(F.rect(0, 0.6, w, 1.8), { label: "Look", lines: o.lines });
  room.item(o.name || "Machine", o.note || "A grey cabinet of dials and lights, its screen running.");
}
// A ball as the design has it, rounded: three crossed boxes (a bevelled cube, as a Minecraft build rounds a small thing),
// not a sharp-cornered block.
function roundBall(G, cx, y, cz, s) {
  const h = s / 2, r = s * 0.2, m = { all: "ball", top: "ballTop" };
  G.box(cx - h, y + r, cz - h + r, cx + h, y + s - r, cz + h - r, m);
  G.box(cx - h + r, y, cz - h + r, cx + h - r, y + s, cz + h - r, m);
  G.box(cx - h + r, y + r, cz - h, cx + h - r, y + s - r, cz + h, m);
}
function ballTable(room, x0, z0, x1, z1, o = {}) {
  table(room, x0, z0, x1, z1, { top: o.top || "lab", leg: o.leg || "steel", h: 0.95, name: "The table of balls", note: o.note || "A white-topped steel table with three balls in a row." });
  const n = 3, s = 0.36, y = 0.95, cz = (z0 + z1) / 2;
  for (let i = 0; i < n; i++) { const cx = x0 + (x1 - x0) * (i + 1) / (n + 1); roundBall(room.geo, cx, y, cz, s); }
  room.act([x0, z0 - 0.3, x1, z1 + 0.3], { label: "Look", lines: ["Three balls, each with a creature inside.", "The professor will let you choose one when he's ready."] });
}
// White, modern shelves, packed full: books, binders and boxes, row on row.
function labShelf(room, X, Z, f, w = 2, h = 3.2, o = {}) {
  const F = room.F(X, Z, f), wood = "shelfWhite", d = 0.7, rows = Math.floor((h - 0.3) / 0.72);
  F.box(0, 0, 0, 0.08, h, d, wood); F.box(w - 0.08, 0, 0, w, h, d, wood); F.box(0, h - 0.08, 0, w, h, d, wood);
  F.box(0.08, 0, 0, w - 0.08, 0.16, d, wood); F.box(0.08, 0.16, 0, w - 0.08, h - 0.08, 0.04, { front: wood, all: null });
  const fill = o.rows || ["storeBoxes", "booksFull", "binders", "booksFull"];
  for (let i = 0; i < rows; i++) {
    const y = 0.16 + i * 0.72, m = fill[i % fill.length];
    F.box(0.08, y, 0.04, w - 0.08, y + 0.05, d, wood);
    F.box(0.1, y + 0.05, 0.06, w - 0.1, y + 0.66, d - 0.06, { front: m, all: m === "storeBoxes" ? "cupboardWhite" : "linen", top: m === "storeBoxes" ? "paint" : "linen" });
  }
  F.solid(0, 0, w, d);
  room.act(F.rect(0, 0.3, w, 1.2), { label: "Look", lines: o.lines || ["Shelves packed with books, binders and boxes of notes."] });
  room.item("White shelves", "Modern white shelving, packed: books shoulder to shoulder, rows of binders, white storage boxes.");
}
// The ball incubator: a round machine (eight-sided, as a Minecraft build would round it): a white base, balls on its rim,
// a glass tank of warm water with more balls inside, and a red top.
function incubator(room, x, z) {
  // Round as a Minecraft build rounds things: each disc is two crossed slabs (a blocky circle), so it's all boxes.
  const G = room.geo;
  // (The second slab sits a hair inside the first's top and bottom, so their faces never lie in one plane and flicker.)
  const disc = (r, y0, y1, mat) => { const k = 0.62 * r, e = Math.min(0.01, (y1 - y0) / 4); G.box(x - r, y0, z - k, x + r, y1, z + k, mat); G.box(x - k, y0 + e, z - r, x + k, y1 - e, z + r, mat); };
  disc(1.08, 0, 0.82, "paint"); disc(1.12, 0.82, 0.94, "redPlastic"); disc(1.06, 0.94, 1.0, "paint");
  disc(0.62, 1.0, 1.12, "greyPlastic");
  const ball = (bx, by, bz, s = 0.3) => roundBall(G, x + bx, by, z + bz, s);
  for (const [bx, bz] of [[-0.25, -0.15], [0.22, -0.2], [0.0, 0.2], [-0.3, 0.25], [0.3, 0.18]]) ball(bx, 1.12, bz, 0.26);
  for (const [bx, bz] of [[-0.86, 0.3], [-0.55, 0.78], [0.55, 0.78], [0.86, 0.3]]) ball(bx, 1.0, bz, 0.24);
  disc(0.8, 1.0, 2.35, "tankGlass");
  disc(0.86, 2.35, 2.62, "redPlastic"); disc(0.55, 2.62, 2.86, "paint"); disc(0.3, 2.86, 3.0, "redPlastic");
  // Pipes back to the wall.
  G.box(0.0, 2.0, z - 0.1, x - 0.6, 2.2, z + 0.1, "steel"); G.box(0.0, 0.3, z + 0.35, x - 0.9, 0.5, z + 0.55, "steel");
  room.solid(x - 1.12, z - 1.12, x + 1.12, z + 1.12);
  room.act([x - 1.2, z - 1.2, x + 1.2, z + 1.4], { label: "Look", lines: ["An incubator. Balls rest in its warm water, waiting for new trainers.", "There are more on the rim, ready to hand out."] });
  room.item("Ball incubator", "A round machine (a blocky circle, as Minecraft builds round things): white base, balls on its rim, a glass tank with more inside, a red top.");
}
// A whiteboard on its own stand, on little wheels.
function boardStand(room, x0, z, w, f = "s", lines, mat = "boardLab") {
  const F = room.F(x0, z, f), hgt = 1.6;
  F.box(-0.05, 0, 0.1, 0.07, 2.6, 0.22, "steel"); F.box(w - 0.07, 0, 0.1, w + 0.05, 2.6, 0.22, "steel");
  F.box(-0.25, 0, -0.1, 0.27, 0.08, 0.42, "rubber"); F.box(w - 0.27, 0, -0.1, w + 0.25, 0.08, 0.42, "rubber");
  F.box(0.07, 0.95, 0.12, w - 0.07, 0.95 + hgt, 0.2, { all: "silver", front: mat });
  F.box(0.25, 0.88, 0.12, w - 0.25, 0.95, 0.34, "silver");
  F.solid(-0.25, -0.1, w + 0.25, 0.42);
  room.act(F.rect(0, -0.2, w, 1.0), { label: "Read", lines });
  room.item("Whiteboard on a stand", "Steel frame on wheels; notes in marker, a pen tray.");
}
function healer(room, X, w, o = {}) {
  const F = room.F(X, 0, "s");
  F.box(0, 0, 0, w, 1.1, 1.0, { all: "cupboardWhite", top: "lab", back: null, bottom: null });
  F.box(0.2, 1.1, 0.15, w - 0.2, 1.2, 0.85, "greyPlastic");
  // Six slots on its top: balls resting in them (the lab's), or empty cups waiting for yours (the Center's).
  for (let i = 0; i < 6; i++) { const lx = 0.45 + (i % 3) * ((w - 0.9) / 2), lz = 0.35 + Math.floor(i / 3) * 0.3;
    if (o.empty) F.box(lx - 0.13, 1.2, lz - 0.13, lx + 0.13, 1.24, lz + 0.13, { all: "steel", top: "rubber" });
    else roundBall(F, lx, 1.2, lz, 0.2); }
  F.box(0, 1.1, 0, w, 2.4, 0.15, { front: "labPanel", all: "greyPlastic" });
  F.solid(0, 0, w, 1.0);
  room.act(F.rect(0, 0.5, w, 1.5), { label: "Look", lines: ["A machine for resting creatures, six slots on its top.", "It's warm. Someone was using it this morning."] });
  room.item("Resting machine", "Six slots for balls on its top, a panel of lights behind.");
}

// The back corners cut off on the slant (as the buildings outside have theirs), stepped half a block at a time as a
// Minecraft build would cut them: n blocks along each wall. The paper, skirting, band and rail carry round the steps.
function chamfer(room, n, o = {}) {
  const G = room.geo, { W, H } = room, paper = o.paper, sk = o.skirt || "paint", s = 0.5, rows = Math.round(n / s);
  for (const east of [false, true]) {
    for (let j = 0; j < rows; j++) {
      const z0 = j * s, z1 = z0 + s, len = n - j * s, inner = len - s;
      // The step itself (its two faces into the room), and what's behind it (only its top, dark).
      const [sx0, sx1] = east ? [W - len, W - inner] : [inner, len];
      const [bx0, bx1] = east ? [W - inner, W] : [0, inner];
      G.box(sx0, 0, z0, sx1, H, z1, { s: paper, [east ? "w" : "e"]: paper, top: "cap", n: null, [east ? "e" : "w"]: null, bottom: null });
      if (inner > 1e-6) G.box(bx0, 0, z0, bx1, H, z1, { top: "cap", all: null });
      room.solid(Math.min(sx0, bx0), z0, Math.max(sx1, bx1), z1);
      // Skirting, the band and the rail along both faces.
      const fx = east ? sx0 : sx1, d = east ? -0.07 : 0.07;
      const strip = (y0, y1, m) => { G.box(sx0, y0, z1, sx1, y1, z1 + 0.07, { all: m, n: null, bottom: null }); G.box(Math.min(fx, fx + d), y0, z0, Math.max(fx, fx + d), y1, z1, { all: m, [east ? "e" : "w"]: null, bottom: null }); };
      strip(0, 0.24, sk);
      if (o.band) strip(o.band[0], o.band[0] + (o.band[2] || 0.22), o.band[1]);
      strip(H - 0.55, H - 0.45, sk);
    }
  }
  room.item("Slanted corners", "The back corners cut off on the slant, half a block at a time, as the building is outside.");
}
// Stairs up the side wall (w or e), climbing north from zFoot to a landing and a way through at the top. The upstairs
// isn't built: they're there to look at, as in the games' Centers.
function sideStairs(room, side, zFoot, o = {}) {
  const G = room.geo, W = room.W, w = o.w || 2, n = o.steps || 4, rise = 0.5, land = o.land || 1.8, west = side === "w";
  const x0 = west ? 0 : W - w, x1 = x0 + w, inX = west ? x1 : x0, inF = west ? "e" : "w";
  for (let i = 0; i < n; i++) { const zb = zFoot - i - 1; G.box(x0, 0, zb, x1, (i + 1) * rise, zb + 1, { top: "tread", s: "paint", [inF]: "paint", n: null, [side]: null, bottom: null }); }
  const top = n * rise, zl = zFoot - n - land;
  G.box(x0, 0, zl, x1, top, zFoot - n, { top: "tread", [inF]: "paint", n: "paint", s: null, [side]: null, bottom: null });
  // The way through at the top, in the wall: a dark doorway in a red frame, a rope across it.
  const wx = west ? 0 : W, fd = west ? 0.12 : -0.12, dz0 = zl + 0.25, dz1 = zFoot - n - 0.25, dy = top, dh = 2.2;
  G.box(Math.min(wx, wx + fd), dy, dz0 - 0.15, Math.max(wx, wx + fd), dy + dh + 0.15, dz1 + 0.15, { all: o.frame || "redPlastic", [side]: null });
  G.box(Math.min(wx, wx + fd * 1.1), dy, dz0, Math.max(wx, wx + fd * 1.1), dy + dh, dz1, { [inF]: "doorDark", all: null });
  const rx = west ? 0.7 : W - 0.7;
  for (const z of [dz0 + 0.05, dz1 - 0.15]) G.box(rx - 0.05, top, z, rx + 0.05, top + 0.95, z + 0.1, "gold");
  G.box(rx - 0.03, top + 0.72, dz0 + 0.1, rx + 0.03, top + 0.8, dz1 - 0.1, "redFabric");
  // The banister on the open side: a newel at the foot, a rail climbing with the steps, then along the landing.
  const bx = west ? inX - 0.12 : inX, b0 = bx, b1 = bx + 0.12;
  G.box(b0 - 0.03, 0, zFoot - 0.15, b1 + 0.03, 1.35, zFoot, "walnut");
  for (let i = 0; i < n; i++) { const zb = zFoot - i - 1, h = (i + 1) * rise; G.box(b0, h + 0.86, zb, b1, h + 0.98, zb + 1, "walnut"); G.box(b0 + 0.03, h, zb + 0.45, b1 - 0.03, h + 0.86, zb + 0.55, "paint"); }
  G.box(b0, top + 0.86, zl, b1, top + 0.98, zFoot - n, "walnut");
  for (let z = zl + 0.3; z < zFoot - n; z += 0.5) G.box(b0 + 0.03, top, z, b1 - 0.03, top + 0.86, z + 0.08, "paint");
  room.solid(Math.min(x0, b0), zl, Math.max(x1, b1), zFoot);
  room.act([Math.min(x0, b0) - 0.2, zFoot - 1.2, Math.max(x1, b1) + 0.2, zFoot + 0.3], { label: "Look", lines: o.lines || ["The stairs go up to the 2F: the CABLE CLUB, for trading and battling with friends.", "It isn't open yet."] });
  room.item("Stairs to the 2F", "Up the side wall to a landing and a dark way through, roped off: the upstairs isn't open.");
}
// A low table with a glass top on chrome legs; a magazine and a little vase of flowers on it.
function glassTable(room, x0, z0, x1, z1, o = {}) {
  const G = room.geo, y = o.h || 0.5, cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
  for (const [x, z] of [[x0 + 0.05, z0 + 0.05], [x1 - 0.13, z0 + 0.05], [x0 + 0.05, z1 - 0.13], [x1 - 0.13, z1 - 0.13]]) G.box(x, 0, z, x + 0.08, y - 0.05, z + 0.08, "steel");
  G.box(x0 + 0.1, 0.12, z0 + 0.1, x1 - 0.1, 0.16, z1 - 0.1, "steel");
  G.box(x0 + 0.3, 0.16, z0 + 0.25, x0 + 0.85, 0.18, z0 + 0.65, "poster");
  G.box(x0, y - 0.05, z0, x1, y, z1, "glass");
  G.box(x0 - 0.02, y - 0.07, z0 - 0.02, x1 + 0.02, y - 0.05, z1 + 0.02, { all: "steel", top: null });
  G.box(cx - 0.11, y, cz - 0.11, cx + 0.11, y + 0.24, cz + 0.11, "potWhite"); G.box(cx - 0.16, y + 0.24, cz - 0.16, cx + 0.16, y + 0.36, cz + 0.16, { all: "leafSolid", top: "flowerTop" });
  room.solid(x0, z0, x1, z1);
  room.act([x0 - 0.2, z0 - 0.2, x1 + 0.2, z1 + 0.2], { label: "Look", lines: o.lines || ["A glass table. There's a magazine about famous trainers on the shelf underneath."] });
  room.item("Glass table", "Chrome legs, a glass top, a magazine on the shelf below and a little vase of flowers.");
}
// Plants as Let's Go's gates and school have them: a palm in a round white pot, and a slim one in a tall white pot.
function palm(room, x, z, s = 1, o = {}) {
  const G = room.geo, p = 0.3 * s;
  G.box(x - p, 0, z - p, x + p, 0.5 * s, z + p, { all: "potWhite", top: "soil" });
  G.box(x - p * 1.15, 0.42 * s, z - p * 1.15, x + p * 1.15, 0.5 * s, z + p * 1.15, "potWhite");
  G.box(x - 0.05, 0.5 * s, z - 0.05, x + 0.05, 1.45 * s, z + 0.05, "walnut");
  G.box(x - 0.8 * s, 1.2 * s, z - 0.02, x + 0.8 * s, 1.75 * s, z + 0.02, "leaf");
  G.box(x - 0.02, 1.2 * s, z - 0.8 * s, x + 0.02, 1.75 * s, z + 0.8 * s, "leaf");
  G.box(x - 0.28 * s, 1.35 * s, z - 0.28 * s, x + 0.28 * s, 1.62 * s, z + 0.28 * s, "leafSolid");
  room.solid(x - p, z - p, x + p, z + p);
  room.act([x - p, z - p, x + p, z + p], { label: "Look", lines: o.lines || ["A palm in a round white pot."] });
  room.item("Palm", "Fronds on a slim trunk, in a round white pot.");
}
function slimPlant(room, x, z, s = 1, o = {}) {
  const G = room.geo, p = 0.17 * s;
  G.box(x - p, 0, z - p, x + p, 0.85 * s, z + p, { all: "potWhite", top: "soil" });
  G.box(x - 0.3 * s, 0.85 * s, z - 0.3 * s, x + 0.3 * s, 1.3 * s, z + 0.3 * s, "leafSolid");
  G.box(x - 0.2 * s, 1.3 * s, z - 0.2 * s, x + 0.2 * s, 1.55 * s, z + 0.2 * s, "leafSolid");
  G.box(x - 0.42 * s, 0.95 * s, z - 0.02, x + 0.42 * s, 1.35 * s, z + 0.02, "leaf");
  room.solid(x - p, z - p, x + p, z + p);
  room.act([x - p, z - p, x + p, z + p], { label: "Look", lines: o.lines || ["A slim plant in a tall white pot."] });
  room.item("Slim plant", "A small leafy bush in a tall, narrow white pot.");
}
// A round stool: a brown cushion (rounded, as two crossed boxes) on a steel stem and foot.
function stool(room, x, z) {
  const G = room.geo;
  G.box(x - 0.22, 0, z - 0.22, x + 0.22, 0.05, z + 0.22, "steel");
  G.box(x - 0.05, 0.05, z - 0.05, x + 0.05, 0.4, z + 0.05, "steel");
  G.box(x - 0.3, 0.4, z - 0.21, x + 0.3, 0.52, z + 0.21, "stoolTop");
  G.box(x - 0.21, 0.4, z - 0.3, x + 0.21, 0.52, z + 0.3, "stoolTop");
  room.solid(x - 0.26, z - 0.26, x + 0.26, z + 0.26);
  room.item("Stools", "Round stools, brown cushions on a steel stem.");
}
function bin(room, x, z) {
  const G = room.geo;
  G.box(x - 0.25, 0, z - 0.25, x + 0.25, 0.7, z + 0.25, { all: "binRed", top: "cap" });
  room.solid(x - 0.25, z - 0.25, x + 0.25, z + 0.25);
  room.act([x - 0.3, z - 0.3, x + 0.3, z + 0.3], { label: "Look", lines: ["A bin. Someone's thrown away a used-up REPEL."] });
}
// A pupil's desk (yellow top on steel legs) with its chair behind, facing the board (north).
function schoolDesk(room, x, z, o = {}) {
  table(room, x - 0.55, z - 0.35, x + 0.55, z + 0.35, { top: "deskYellow", leg: "steel", h: 0.74, name: "Desks", note: "Yellow-topped desks on steel legs, a chair at each." });
  chair(room, x, z + 0.78, "n", { wood: "pale" });
  if (o.book) room.geo.box(x - 0.25, 0.74, z - 0.15, x + 0.15, 0.78, z + 0.15, { all: "linen", top: "bookOpen" });
  if (o.lines) room.act([x - 0.6, z - 0.5, x + 0.6, z + 0.45], { label: "Read", lines: o.lines });
}
// A window on a side wall ("w" or "e"), its panes crossed (a white bar each way), a sill under it. z: where it starts
// along the wall, w its width; y its foot, h its height.
function crossWindow(R, wall, z, y, w, h, lines) {
  framed(R, z, y, w, h, "window", wall, { frameMat: "paint", act: { label: "Look", lines } });
  const G = R.geo, x0 = wall === "w" ? 0.09 : R.W - 0.15, x1 = x0 + 0.06, zc = z + w / 2, yc = y + h / 2;
  G.box(x0, y, zc - 0.05, x1, y + h, zc + 0.05, "paint");
  G.box(x0, yc - 0.05, z, x1, yc + 0.05, z + w, "paint");
  const sx0 = wall === "w" ? 0 : R.W - 0.26, sx1 = sx0 + 0.26;
  G.box(sx0, y - 0.2, z - 0.15, sx1, y - 0.1, z + w + 0.15, "paint");
  // Its light on the floor, in from the wall.
  if (wall === "w") sunPool(R, 1.2, z - 0.2, 1.2 + h * 1.4, z + w + 0.2, 2, null, [0.1, z, z + w, y, y + h]);
  else sunPool(R, R.W - 1.2 - h * 1.4, z - 0.2, R.W - 1.2, z + w + 0.2, 1, null, [R.W - 0.1, z, z + w, y, y + h]);
}
// A wooden post in a corner of the room (Let's Go's rooms have them), floor to ceiling.
function cornerPost(R, x, z, s = 0.42, mat = "beam") {
  R.geo.box(x, 0, z, x + s, R.H, z + s, { all: mat, top: "cap", bottom: null });
  R.solid(x, z, x + s, z + s);
}
// Let's Go's gate: the room both Route 2 gates (and later ones) are built from. o.south / o.north: the ways out
// ({ to } for a real one, { none, lines } for one that goes nowhere yet).
function lgGate(R, o) {
  const D = R.D;
  shell(R, { paper: "paperGateLG", floor: "gateCheck", door: LG_GATE_DOOR, entry: true, skirt: "gateWhite", ledge: "gateBlue", ledgeTop: "gateWhite" });
  // The blue lower walls, a white rail over them.
  band(R, 0.24, "gateBlue", 1.1); band(R, 1.34, "gateWhite", 0.1);
  // Windows down both sides.
  for (const z of [2.6, 6.2]) for (const wall of ["w", "e"]) crossWindow(R, wall, z, 1.6, 2.0, 1.4, ["Outside: the trees along ROUTE 2."]);
  // The way at the north (dark, the forest's green beyond), palms either side.
  const north = o.north || {};
  passageN(R, LG_GATE_DOOR[0], LG_GATE_DOOR[1], north.none ? { none: true, mat: "matGateN", frame: "paint" } : { to: north.to, spawn: north.spawn, mat: "matGateN", frame: "paint" });
  if (north.none) R.act([LG_GATE_DOOR[0], 0, LG_GATE_DOOR[1], 1.2], { label: "Look", lines: north.lines });
  palm(R, 4.6, 0.9, 1.0); palm(R, 9.4, 0.9, 1.0);
  // The runner, from the way on at the north to the door at the south.
  rug(R, 5, 2, 9, R.Dm, "gateRun");
  // Stools round two glass tables on the east side, a few along the west wall; plants; a bin.
  glassTable(R, 10.4, 3.0, 11.9, 4.4, { lines: ["A glass table. There's a leaflet: VIRIDIAN FOREST, A WALK FOR ALL AGES!"] });
  stool(R, 12.6, 3.7); stool(R, 11.15, 2.3); stool(R, 9.7, 3.7);
  glassTable(R, 10.4, 6.2, 11.9, 7.6, { lines: ["A glass table. Someone's left a map of ROUTE 2 on it."] });
  stool(R, 12.6, 6.9); stool(R, 11.15, 8.3); stool(R, 9.7, 6.9);
  stool(R, 1.0, 6.6); stool(R, 1.0, 7.8);
  slimPlant(R, 0.8, 3.4, 1.0); plant(R, 1.0, R.Dm - 1.0, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); slimPlant(R, 13.2, R.Dm - 1.0, 1.0);
  bin(R, 13.3, 1.0);
  const south = o.south || {};
  doormat(R, LG_GATE_DOOR[0], LG_GATE_DOOR[1], south.none ? { none: true, mat: "matGate" } : { to: south.to, spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matGate" });
  if (south.none) R.act([LG_GATE_DOOR[0], D - 1.4, LG_GATE_DOOR[1], D], { label: "Look", lines: south.lines });
}
// A soft armchair (as the Centers' waiting seats): x, z its middle, face the way you'd look sitting in it.
function armchair(room, x, z, face, fab = "yellowFabric") {
  const F = centred(room, x, z, face, 0.95, 0.95);
  F.box(0.08, 0, 0.08, 0.87, 0.08, 0.87, "steel");
  F.box(0, 0.08, 0, 0.95, 0.44, 0.95, fab);
  F.box(0.14, 0.44, 0.2, 0.81, 0.54, 0.92, "cushion");
  F.box(0, 0.44, 0, 0.95, 1.05, 0.2, fab);
  F.box(0, 0.44, 0.2, 0.14, 0.72, 0.95, fab); F.box(0.81, 0.44, 0.2, 0.95, 0.72, 0.95, fab);
  F.solid(0, 0, 0.95, 0.95);
  room.item("Armchairs", "Soft square armchairs on chrome feet.");
}
// A till on a counter top at height y (its keys toward f's back, its little screen toward f).
function till(room, x, z, y, f = "s") {
  const F = centred(room, x, z, f, 0.72, 0.6);
  F.box(0, y, 0, 0.72, y + 0.2, 0.6, "greyPlastic");
  F.box(0.06, y + 0.2, 0.04, 0.66, y + 0.28, 0.42, { top: "keyboard", all: "greyPlastic" });
  F.box(0.18, y + 0.2, 0.44, 0.54, y + 0.5, 0.56, { all: "greyPlastic", front: "tillLcd" });
  room.item("Till", "A grey till: a keypad, a drawer, a little green screen facing you.");
}
// A screen on the north wall, framed, its picture moving.
function wallScreen(room, x, y, w, h, screenKey, lines) {
  const G = room.geo;
  G.box(x - 0.1, y - 0.1, 0, x + w + 0.1, y + h + 0.1, 0.12, { all: "plastic", n: null });
  G.box(x, y, 0.12, x + w, y + h, 0.13, { s: screenKey, all: null });
  room.act([x, 0, x + w, 0.6], { label: "Watch", lines });
  room.item("Wall screen", "A big screen on the wall, its picture moving.");
}
// A chest freezer (the Mart's): white, a blue rim, a glass lid over tubs and lollies.
function chestFreezer(room, X, Z, f, w = 2) {
  const F = room.F(X, Z, f), d = 1.0, h = 0.95;
  F.box(0, 0, 0, w, h - 0.1, d, { all: "shelfWhite", front: "cupboardWhite", top: null });
  F.box(0.1, 0, 0.06, w - 0.1, 0.12, d - 0.06, "blueTrim");
  F.box(0.08, h - 0.16, 0.08, w - 0.08, h - 0.14, d - 0.08, { top: "freezerTop", all: null });
  F.box(0, h - 0.1, 0, w, h, d, { all: "blueTrim", top: null });
  F.box(0.06, h - 0.04, 0.06, w - 0.06, h - 0.02, d - 0.06, { top: "glass", all: null });
  F.solid(0, 0, w, d);
  room.act(F.rect(0, 0, w, d + 0.6), { label: "Look", lines: ["A chest freezer: ice creams, lollies, and a tub of something called FROZEN BERRIES."] });
  room.item("Chest freezer", "White, a blue rim, a glass lid over bright tubs and lollies.");
}
// A low glass display case of goods (as the Marts' middle cases).
function displayCase(room, x0, z0, x1, z1, lines) {
  const G = room.geo, y = 0.62;
  G.box(x0, 0, z0, x1, y, z1, { all: "shelfWhite", top: null });
  G.box(x0 + 0.06, 0, z0 - 0.01, x1 - 0.06, 0.14, z0, { n: "blueTrim", all: null }); G.box(x0 + 0.06, 0, z1, x1 - 0.06, 0.14, z1 + 0.01, { s: "blueTrim", all: null });
  G.box(x0 + 0.05, y - 0.02, z0 + 0.05, x1 - 0.05, y, z1 - 0.05, { top: "goodsTop", all: null });
  G.box(x0, y, z0, x1, y + 0.42, z1, { all: "glass", bottom: null });
  for (const [x, z] of [[x0, z0], [x1 - 0.06, z0], [x0, z1 - 0.06], [x1 - 0.06, z1 - 0.06]]) G.box(x, y, z, x + 0.06, y + 0.44, z + 0.06, "steel");
  room.solid(x0, z0, x1, z1);
  room.act([x0 - 0.1, z0 - 0.6, x1 + 0.1, z1 + 0.6], { label: "Look", lines });
  room.item("Display case", "A low white case with a glass top: balls and boxes laid out under it.");
}
// The world map on a wall (n, w or e; a is where it starts along that wall): John's Kanto, as the Town Map shows it.
// SPACE opens the Town Map (in game too).
function wallMap(room, wall, a, y, w, o = {}) {
  const h = w * (WORLDMAP.h + 6) / (WORLDMAP.w + 6);
  framed(room, a, y, w, h, "worldMap", wall, { frameMat: o.frameMat || "walnut", act: { label: "Look", map: true, lines: o.lines || ["A map of the whole region.", "In game, this opens the TOWN MAP."] } });
  room.item("World map", "Kanto from above (the world itself, as the Town Map paints it), framed; it opens the Town Map.");
}
// The Center's screen: a heart beating over a ball, little sparkles drifting up.
function drawCenterAd(P, f) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, mixc(hx("#F7B6B0"), hx("#E5525C"), y / P.h));
  const cx = P.w / 2, cy = P.h / 2 + 1, k = 1 + 0.18 * Math.max(0, Math.sin(f * 0.8));
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) { const u = (x - cx) / (6 * k), v = -(y - cy) / (6 * k) + 0.25; if ((u * u + v * v - 1) ** 3 - u * u * v * v * v <= 0) P.set(x, y, hx("#FFFFFF")); }
  P.disc(cx, cy, 2.2, hx("#E5525C")); P.rect(Math.round(cx - 2), Math.round(cy), 5, 1, hx("#2B2522")); P.set(Math.round(cx), Math.round(cy), hx("#FFFFFF"));
  for (let i = 0; i < 5; i++) { const x = (i * 9 + 3) % P.w, y = P.h - 1 - ((f * 1.5 + i * 7) % P.h); P.set(x, y, hx("#FFF4D8")); }
}
// The Mart's screen: a ball bouncing past a potion and a can of repel, on blue.
function drawMartAd(P, f) {
  for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, mixc(hx("#9CC8E6"), hx("#3A6FD0"), y / P.h));
  P.rect(0, P.h - 4, P.w, 4, hx("#2B4F9A"));
  const bx = (f * 2) % (P.w + 8) - 4, by = P.h - 8 - Math.round(Math.abs(Math.sin(f * 0.5)) * 9);
  P.disc(bx, by, 3, hx("#E5525C")); P.rect(Math.round(bx) - 3, by, 7, 3, hx("#F4F4F4")); P.rect(Math.round(bx) - 3, by, 7, 1, hx("#2B2522")); P.set(Math.round(bx), by, hx("#F4F4F4"));
  P.rect(8, P.h - 12, 4, 8, hx("#8E6BC8")); P.rect(9, P.h - 14, 2, 2, hx("#D7DEE3")); P.rect(9, P.h - 10, 2, 2, hx("#F6F4EE"));
  P.rect(P.w - 12, P.h - 13, 4, 9, hx("#F2C744")); P.rect(P.w - 12, P.h - 14, 4, 1, hx("#B9C3C9"));
  if (f % 8 < 4) for (let i = 0; i < 3; i++) P.set(4 + i * 15, 3, hx("#FFFFFF"));
}

// ---------- the Let's Go redesign's own pieces ----------
// A dresser (your house's): cupboards below, an open hutch above with plates standing on its shelves, a honey top.
function dresser(R, X, w = 2.4, o = {}) {
  const F = R.F(X, 0, "s"), body = o.body || "paint", top = o.top || "honey";
  F.box(0, 0, 0, w, 0.1, 0.6, "cap");
  F.box(0, 0.1, 0, w, 1.0, 0.66, { all: body, front: null, back: null, bottom: null });
  const n = Math.max(2, Math.round(w / 1.1));
  for (let i = 0; i < n; i++) F.box(i * w / n + 0.03, 0.14, 0.66, (i + 1) * w / n - 0.03, 0.96, 0.67, { front: "cupboardWhite", all: null });
  F.box(-0.04, 1.0, -0.02, w + 0.04, 1.07, 0.71, top);
  F.box(0.04, 1.07, 0, 0.14, 2.62, 0.42, body); F.box(w - 0.14, 1.07, 0, w - 0.04, 2.62, 0.42, body);
  F.box(0.14, 1.07, 0, w - 0.14, 2.62, 0.04, { front: o.back || "mintPaint", all: null });
  for (const y of [1.6, 2.12]) F.box(0.14, y, 0.04, w - 0.14, y + 0.05, 0.42, body);
  for (const y of [1.07, 1.65, 2.17]) for (let p = 0.42; p < w - 0.3; p += 0.46) {
    F.box(p - 0.17, y + 0.06, 0.1, p + 0.17, y + 0.3, 0.14, "potWhite");
    F.box(p - 0.11, y, 0.1, p + 0.11, y + 0.36, 0.14, "potWhite");
  }
  F.box(-0.02, 2.62, -0.02, w + 0.02, 2.74, 0.48, top);
  F.solid(0, 0, w, 0.71);
  R.act(F.rect(0, 0.3, w, 1.3), { label: "Look", lines: o.lines || ["Mum's best plates, lined up on the dresser. Nobody's allowed to use them."] });
  R.item(o.name || "Dresser", o.note || "Cupboards below, an open hutch above with plates standing on its shelves.");
}
// A chest of four drawers (beside your desk).
function drawerUnit(R, X, Z, f, w = 0.9, h = 1.1, wood = "pale") {
  const F = R.F(X, Z, f);
  F.box(0, 0, 0, w, h, 0.62, { all: wood, front: null });
  for (const [y0, y1] of [[0.06, h / 2 - 0.02], [h / 2 + 0.02, h - 0.06]]) F.box(0.05, y0, 0.62, w - 0.05, y1, 0.63, { front: "drawers", all: null });
  F.solid(0, 0, w, 0.63);
  R.item("Chest of drawers", "Four drawers, pale wood, beside the desk.");
}
// A toy box at the foot of the bed, books stacked on it.
function toyBox(R, x0, z0, x1, z1) {
  const G = R.geo;
  G.box(x0, 0, z0, x1, 0.62, z1, { all: "pale", top: "honey" });
  G.box(x0 + 0.08, 0.62, z0 + 0.1, x1 - 0.12, 0.8, z0 + 0.62, { all: "linen", front: "books", s: "books" });
  G.box(x0 + 0.12, 0.8, z0 + 0.2, x1 - 0.2, 0.92, z0 + 0.55, { all: "redPlastic", top: "linen" });
  R.solid(x0, z0, x1, z1);
  R.act([x0 - 0.1, z0 - 0.1, x1 + 0.1, z1 + 0.1], { label: "Look", lines: ["Your toy box. Comics and an old field guide stacked on the lid."] });
  R.item("Toy box", "A pale wooden box, books and comics stacked on its lid.");
}
// A window on the back wall with its panes crossed.
function crossWindowN(R, x, y, w, h, lines) {
  windowN(R, x, y, w, h, { lines });
  const G = R.geo;
  G.box(x + w / 2 - 0.05, y, 0.02, x + w / 2 + 0.05, y + h, 0.08, "paint");
  G.box(x, y + h / 2 - 0.05, 0.02, x + w, y + h / 2 + 0.05, 0.08, "paint");
}
// A padded footstool (the Center's waiting seats, as Let's Go's): rounded at its corners, on little steel feet.
function footstool(R, x, z, w = 0.9, d = 0.9, fab = "blueFabric") {
  const G = R.geo, a = w / 2, b = d / 2;
  for (const [sx, sz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) { const fx = x + sx * (a - 0.14), fz = z + sz * (b - 0.14); G.box(fx - 0.05, 0, fz - 0.05, fx + 0.05, 0.1, fz + 0.05, "steel"); }
  G.box(x - a, 0.1, z - b + 0.07, x + a, 0.42, z + b - 0.07, fab);
  G.box(x - a + 0.07, 0.1, z - b, x + a - 0.07, 0.42, z + b, fab);
  R.solid(x - a, z - b, x + a, z + b);
  R.act([x - a, z - b, x + a, z + b], { label: "Sit", lines: ["A padded footstool. Comfy enough for a short wait."] });
  R.item("Footstools", "Padded blue footstools on little steel feet, round the coffee tables.");
}
// A big ball on a white stand (one at each end of the Center's counter).
function statueBall(R, x, z, y, s = 0.8) {
  const G = R.geo;
  G.box(x - 0.3, y, z - 0.3, x + 0.3, y + 0.1, z + 0.3, "lab");
  G.box(x - 0.18, y + 0.1, z - 0.18, x + 0.18, y + 0.2, z + 0.18, "steel");
  roundBall(G, x, y + 0.2, z, s);
  R.item("Ball statues", "A big ball on a white stand at each end of the counter.");
}
// The Center's counter, curving round the nurse (stepped round, as a Minecraft build rounds one): its straight front
// from cx-3 to cx+3, then stepping back to the wall either side.
function curvedCounter(R, cx, o = {}) {
  const G = R.geo, front = o.front || "counterOrange", top = o.top || "lab", h = 1.05;
  const segs = [[-3, 3, 4.4, 5.2], [-3.8, -3, 3.9, 4.9], [3, 3.8, 3.9, 4.9], [-4.4, -3.8, 3.1, 4.3], [3.8, 4.4, 3.1, 4.3], [-4.8, -4.3, 0, 3.1], [4.3, 4.8, 0, 3.1]];
  for (const [a, b, z0, z1] of segs) {
    G.box(cx + a, 0, z0, cx + b, h, z1, { all: front, top: null, bottom: null });
    G.box(cx + a, h, z0, cx + b, h + 0.1, z1, top);
    R.solid(cx + a, z0, cx + b, z1);
  }
  R.item("The counter", "Curving round the nurse, orange and white, stepped round as a Minecraft build would round it; a big ball at each end.");
}
// The front corners cut on the slant, low (the ledge's height), as the back ones are: n blocks along each wall.
function frontChamfer(R, n, mat = "paint", top = "lab") {
  const G = R.geo, W = R.W, Dm = R.Dm, s = 0.5, rows = Math.round(n / s);
  for (const east of [false, true]) for (let j = 0; j < rows; j++) {
    const z0 = Dm - n + j * s, len = (j + 1) * s, [x0, x1] = east ? [W - len, W] : [0, len];
    G.box(x0, 0, z0, x1, 0.7, z0 + s, { all: mat, top, bottom: null });
    R.solid(x0, z0, x1, z0 + s);
  }
}
// A post at the wall (x 0 or W), z0 to z1, floor to ceiling.
function wallPost(R, west, z0, z1, mat, s = 0.5) {
  const x0 = west ? 0 : R.W - s;
  R.geo.box(x0, 0, z0, x0 + s, R.H, z1, { all: mat, top: "cap", bottom: null });
  R.solid(x0, z0, x0 + s, z1);
}
// The Mart's shelves both sides of one spine (an island), white caps rounding its two ends.
function islandShelf(R, X, Z, len, h = 1.6, o = {}) {
  martShelf(R, X, Z, "w", len, h, { seed: o.seed || 0, lines: o.lines });
  martShelf(R, X, Z + len, "e", len, h, { seed: (o.seed || 0) + 1, lines: o.lines });
  const G = R.geo;
  for (const [z0, z1, k] of [[Z - 0.3, Z, 0.12], [Z - 0.45, Z - 0.3, 0.36], [Z + len, Z + len + 0.3, 0.12], [Z + len + 0.3, Z + len + 0.45, 0.36]])
    G.box(X - 0.8 + k, 0, z0, X + 0.8 - k, h, z1, { all: "shelfWhite", top: "lab" });
  // On top: more goods laid out (seen from above).
  G.box(X - 0.7, h, Z + 0.1, X + 0.7, h + 0.02, Z + len - 0.1, { top: "goodsTop", all: null });
  R.solid(X - 0.8, Z - 0.45, X + 0.8, Z + len + 0.45);
}
// Cardboard boxes, one on another (the lab's).
function boxes(R, x, z) {
  const G = R.geo;
  G.box(x - 0.4, 0, z - 0.35, x + 0.4, 0.6, z + 0.35, "cardboard"); G.box(x - 0.3, 0.6, z - 0.28, x + 0.25, 1.05, z + 0.25, "cardboard");
  R.solid(x - 0.4, z - 0.35, x + 0.4, z + 0.35);
  R.act([x - 0.45, z - 0.4, x + 0.45, z + 0.4], { label: "Look", lines: ["Boxes of new POKé BALLS, still taped shut."] });
  R.item("Boxes", "Cardboard boxes stacked by the shelves.");
}
// A desk lamp with a white shade.
function deskLamp(R, x, z, y) {
  const G = R.geo;
  G.box(x - 0.14, y, z - 0.14, x + 0.14, y + 0.05, z + 0.14, "steel"); G.box(x - 0.03, y + 0.05, z - 0.03, x + 0.03, y + 0.5, z + 0.03, "steel");
  G.box(x - 0.2, y + 0.42, z - 0.2, x + 0.2, y + 0.68, z + 0.2, "lampWhite");
}
// The Viridian house's TV unit: a long low cabinet with a top, a DVD player in one compartment, books in another,
// doors between; the TV on top.
function tvUnit(R, X, w, screenKey, o = {}) {
  const F = R.F(X, 0, "s"), wood = o.wood || "walnut", t = (w - 0.16) / 3;
  F.box(0, 0, 0, w, 0.08, 0.72, "cap");
  F.box(0, 0.08, 0, 0.08, 0.7, 0.75, wood); F.box(w - 0.08, 0.08, 0, w, 0.7, 0.75, wood);
  F.box(0.08, 0.08, 0, w - 0.08, 0.14, 0.75, wood);
  F.box(0.08, 0.14, 0, w - 0.08, 0.7, 0.04, { front: wood, all: null });
  for (const k of [1, 2]) F.box(0.08 + k * t - 0.03, 0.14, 0.04, 0.08 + k * t + 0.03, 0.7, 0.75, wood);
  F.box(-0.03, 0.7, -0.02, w + 0.03, 0.78, 0.8, wood);
  F.box(0.2, 0.14, 0.12, 0.08 + t - 0.12, 0.27, 0.62, { all: "plastic", front: "dvdFront" });
  F.box(0.08 + t + 0.03, 0.14, 0.74, 0.08 + 2 * t - 0.03, 0.7, 0.75, { front: "cupboard", all: null });
  F.box(0.08 + 2 * t + 0.08, 0.14, 0.1, w - 0.14, 0.62, 0.6, { front: "books", all: "linen", top: "linen" });
  const tw = o.tw || 1.9, th = o.th || 1.15, tx = (w - tw) / 2;
  F.box(w / 2 - 0.3, 0.78, 0.2, w / 2 + 0.3, 0.84, 0.55, "plastic"); F.box(w / 2 - 0.08, 0.84, 0.3, w / 2 + 0.08, 0.98, 0.42, "plastic");
  F.box(tx, 0.98, 0.25, tx + tw, 0.98 + th, 0.42, "plastic");
  F.box(tx + 0.08, 1.06, 0.42, tx + tw - 0.08, 0.98 + th - 0.08, 0.43, { front: screenKey, all: null });
  F.solid(0, 0, w, 0.8);
  R.act(F.rect(0, 0.4, w, 1.5), { label: "Watch", lines: o.lines });
  R.item("TV unit", "A long low cabinet with a top: a DVD player in one end, books in the other, doors between; the TV on it.");
}

// ---------- battle arenas ----------
// A battle indoors isn't fought among the room's furniture: as Let's Go has it, it moves to a round hall built from
// that room's own things (the lab's desks, shelves and incubator round its edge, a row of windows high up), with no one
// else about. The mod puts it up over you for the battle, as it does the outdoor stages, and takes it away after.
// o: floor, paper (the wall), post, low (the panelling), beam, ceiling; r the radius, h the height.
function arenaShell(R, o) {
  const G = R.geo, c = R.W / 2, rad = o.r || 11.5, H = o.h || 7, beam = o.beam || "walnut";
  for (let j = 0; j < R.D; j++) {
    const zc = j + 0.5 - c; if (Math.abs(zc) >= rad + 1) continue;
    const half = Math.sqrt(Math.max(0, (rad + 1) ** 2 - zc * zc)), x0 = Math.max(0, Math.floor(c - half)), x1 = Math.min(R.W, Math.ceil(c + half));
    G.box(x0, -0.35, j, x1, 0, j + 1, { top: o.floor, all: null });
  }
  // The wall: a ring of blocks round it (a block circle, as a Minecraft build rounds one): panelling low down, the
  // room's own walls above, a dark beam, a row of windows high up, a post every thirty degrees.
  for (let i = 0; i < R.W; i++) for (let j = 0; j < R.D; j++) {
    const d = Math.hypot(i + 0.5 - c, j + 0.5 - c);
    if (d < rad || d >= rad + 1) continue;
    const a = Math.atan2(j + 0.5 - c, i + 0.5 - c), fr = ((a / (Math.PI / 6)) % 1 + 1) % 1, post = fr < 0.09 || fr > 0.91;
    const pm = o.post || beam;
    G.box(i, 0, j, i + 1, 0.9, j + 1, { all: post ? pm : (o.low || "walnut"), top: null, bottom: null });
    G.box(i, 0.9, j, i + 1, 4.6, j + 1, { all: post ? pm : o.paper, top: null, bottom: null });
    G.box(i, 4.6, j, i + 1, 5.0, j + 1, { all: beam, top: null, bottom: null });
    G.box(i, 5.0, j, i + 1, 6.2, j + 1, { all: post ? pm : "window", top: null, bottom: null });
    G.box(i, 6.2, j, i + 1, H, j + 1, { all: beam, top: "cap", bottom: null });
    R.solid(i, j, i + 1, j + 1);
  }
  G.box(0, H, 0, R.W, H + 0.2, R.D, { bottom: o.ceiling || "paint", all: null });
  R.item("The hall", "Round, a block circle: panelling, the room's walls, a dark beam, windows high up, posts every thirty degrees, a ceiling.");
}

function bunting(room, x0, x1, y) { room.geo.box(x0, y, 0.04, x1, y + 0.45, 0.05, { s: "bunting", all: null }); room.item("Bunting", "Pennants strung along the wall."); }

const BUILD = {
  house1() {
    // Downstairs, as Let's Go's: the kitchen down the left-hand wall on its green mat, the dresser of plates, a plant and
    // the TV along the back, the stairs up at the back on the right, the table in the middle on a salmon rug with two
    // green-cushioned chairs each side, Mum at one of them. The mat in its nook at the door.
    const R = new Room("house1", { name: "Your house", floor: "Downstairs", paper: "paperMum", W: 14, D: 13, outsideSize: "8 by 6" });
    shell(R, { paper: "mcCream", floor: "sqHoney", door: DOOR, entry: true, entryFloor: "mcQuartzCheck", skirt: "mcStripped", ledge: "honey" });
    kitchen(R, 0, "e", { z: 7.4 });
    rug(R, 1, 2, 3, 7, "kitchenGreen");
    bin(R, 0.55, 8.0);
    dresser(R, 1.4, 2.4);
    plant(R, 4.55, 0.8, 1.0);
    tvSet(R, 5.4, 0, "s", R.screen("tvFilm", 40, 24, drawFilm, 0.9), { w: 2.8, speakers: true,
      lines: ["There's a film on. Four kids are setting off down a railway line.", "Maybe it's time you set off too."] });
    R.item("Television", "On a walnut stand with speakers; the film on it moves.");
    clock(R, 8.6, 2.5);
    stairsUp(R, 10, 0, 2, { to: "house2", spawn: STAIRS_TOP });
    rug(R, 4, 4, 11, 9, "salmon");
    table(R, 6.4, 5.0, 8.6, 7.6, { cloth: "linen", name: "Dining table", note: "Oak top, walnut legs, a linen runner, plates and a fruit bowl." });
    fruitBowl(R, 7.5, 6.3, 0.87); plates(R, [[6.9, 5.6], [8.1, 5.6], [6.9, 7.0], [8.1, 7.0]], 0.87);
    for (const z of [5.7, 6.9]) { chair(R, 5.75, z, "e", { seat: "greenFabric" }); chair(R, 9.25, z, "w", { seat: "greenFabric" }); }
    R.item("Chairs", "Walnut chairs with green cushions, two each side of the table.");
    crossWindow(R, "e", 4.4, 1.5, 2.0, 1.4, ["Out of the window: Pallet Town's flowers, and the lab's roof past the trees."]);
    framed(R, 7.6, 1.8, 1.2, 0.9, "photos", "e", { act: { label: "Look", lines: ["You, Mum and a very small partner on the beach."] } });
    R.item("Photos", "Framed in walnut on the east wall.");
    plant(R, 1.0, 10.0, 1.0); plant(R, 13.0, 10.0, 1.0);
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHome" });
    R.npcs.push({ name: "Mum", x: 5.75, z: 6.9, yaw: FACE.e, seated: true,
      look: { skin: "middle_aged_woman" },
      talk: f => f && f.h > 3.4 ? [
        { text: "...Is that coming in too? Mind the ceiling, love!" },
        { text: "Professor Oak was asking for you. Off you go, and take your friend with you." }
      ] : [
        { text: "Morning, sleepyhead! Professor Oak next door was asking for you." },
        { text: "All children leave home one day. Go on, he's waiting at the lab." }
      ] });
    R.spawn = STAIRS_FOOT;
    return R;
  },

  house2() {
    // Your room, as Let's Go's: the desk with its drawers and the PC in the back corner, the window with its red
    // curtains drawn in, a poster and a clock; the green bed down the left-hand wall, the toy box at its foot; the TV on
    // its low stand on a big red rug, the console and two cushions in front of it; the wardrobe; the stairs down.
    const R = new Room("house2", { name: "Your room", floor: "Upstairs", paper: "paperRoom", W: 14, D: 12 });
    shell(R, { paper: "mcSky", floor: "sqLight", floorRects: [[0, 0, 10, 12], [10, 2, 14, 12]], skirt: "mcStripped" });
    stairsDown(R, 10, 0, 2, { to: "house1", spawn: STAIRS_FOOT, paper: "mcSky" });
    deskPC(R, 0.4, 0, "s", R.screen("pcScreen", 32, 24, drawPC, 1), { pc: true });
    drawerUnit(R, 2.8, 0, "s");
    officeChair(R, 1.55, 1.6, "n");
    windowN(R, 4.6, 1.5, 2.2, 2.0, { curtains: "curtain", curtainIn: 0.35 });
    framed(R, 7.4, 1.6, 1.1, 1.5, "poster", "n", { frameMat: "honey", act: { label: "Look", lines: ["A poster of a famous trainer mid-throw. GO!"] } });
    R.item("Poster", "A trainer mid-throw, drawn in silhouette on a sunburst.");
    clock(R, 8.9, 2.6);
    bed(R, 0, 5.4, "e", { w: 1.8, l: 2.9, quilt: "quiltGreen" });
    nightstand(R, 0, 6.3, "e");
    toyBox(R, 3.0, 3.75, 3.7, 5.25);
    rug(R, 4, 4, 10, 10, "redRoom");
    tvSet(R, 5.6, 4.6, "s", R.screen("tvRace", 32, 24, drawRace, 0.95), { w: 2.4, tw: 1.7, th: 1.05, lines: ["A racing game, paused on lap 2. It'll keep."] });
    gameConsole(R, 6.8, 6.5);
    cushionSeat(R, 6.2, 8.0, "blueFabric"); cushionSeat(R, 7.6, 8.0, "cushion");
    wardrobe(R, 14, 5.0, "w");
    bagOnFloor(R, 11.8, 10.6, ["Your bag. Packed and ready for the road."]);
    plant(R, 13.0, 11.0, 1.0, { lines: ["A little fern. You remember to water it, mostly."] });
    R.spawn = { x: 3.6, z: 6.4, yaw: FACE.s };
    return R;
  },

  rival1() {
    // The rival's house, laid out as yours: the kitchen down the left on a blue mat, shelves and the trophy cabinet and
    // the TV along the back, the stairs at the back on the right, the table on his rug with the Town Map on it and Daisy
    // at it, the blue sofa down the right. The mat in its nook at the door.
    const R = new Room("rival1", { name: "Rival's house", floor: "Downstairs", paper: "paperRival", W: 14, D: 13, outsideSize: "8 by 6" });
    shell(R, { paper: "mcSage", floor: "sqPale", door: DOOR, entry: true, entryFloor: "mcBlueCheck", skirt: "mcStripped", ledge: "walnut" });
    kitchen(R, 0, "e", { z: 7.4 });
    rug(R, 1, 2, 3, 7, "kitchenBlue");
    bin(R, 0.55, 8.0);
    bookshelf(R, 1.4, 0, "s", 2, 2.8, { trinkets: { 2: "trophy" }, lines: ["Books about battling. Most have the corners turned down."] });
    trophyCabinet(R, 3.5, 0, "s");
    tvSet(R, 5.8, 0, "s", R.screen("tvRival", 40, 24, drawFilm, 0.9), { w: 2.4, lines: ["A battle on TV. Daisy says her brother watches it every week."] });
    clock(R, 8.6, 2.5);
    stairsUp(R, 10, 0, 2, { to: "rival2", spawn: STAIRS_TOP });
    rug(R, 4, 4, 11, 9, "rugRival");
    table(R, 6.4, 5.0, 8.6, 7.6, { top: "walnut", leg: "walnut", name: "Table", note: "Walnut table. The Town Map is lying on it." });
    R.geo.box(6.7, 0.86, 5.6, 8.3, 0.89, 6.4, { top: "worldMap", all: "linen" });
    R.act([6.4, 5.0, 8.6, 7.6], { label: "Look", map: true, lines: ["It's a Town Map. Daisy's keeping it safe for you.", "In game, this opens the TOWN MAP."] });
    for (const z of [5.7, 6.9]) { chair(R, 5.75, z, "e", { wood: "walnut", seat: "blueFabric" }); chair(R, 9.25, z, "w", { wood: "walnut", seat: "blueFabric" }); }
    plates(R, [[7.5, 7.1]], 0.86);
    sofa(R, 14, 4.4, "w", 3, { fabric: "blueFabric", cushion: "linen", pillow: "cushion", pillow2: "cushion", lines: ["A blue sofa. There's a game controller down the side of the cushion."] });
    framed(R, 4.4, 2.3, 2.4, 1.2, "paintingHills", "e", { act: { label: "Look", lines: ["A painting of the hills north of town, with a little red-roofed house."] } });
    floorLamp(R, 13.2, 8.6);
    plant(R, 1.0, 10.0, 1.0); plant(R, 13.0, 10.0, 1.0);
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 24.9, z: 10.0, yaw: FACE.s }, mat: "matRival" });
    R.npcs.push({ name: "Daisy", x: 5.75, z: 6.9, yaw: FACE.e, seated: true,
      look: { skin: "brunette_girl" },
      talk: () => [{ text: "Hi! My brother's already gone to Grandpa's lab." }, { text: "He always has to be first. You'd better catch him up!" }] });
    R.spawn = FROM_TOWN;
    return R;
  },

  rival2() {
    // His room, laid out as yours: the desk and his locked PC in the back corner, the window, his posters; the bed down
    // the left, his rug, a punching bag, trophies, a beanbag; the stairs down.
    const R = new Room("rival2", { name: "Rival's room", floor: "Upstairs", paper: "paperRivalRoom", W: 14, D: 12 });
    shell(R, { paper: "mcNavy", floor: "sqDark", floorRects: [[0, 0, 10, 12], [10, 2, 14, 12]], skirt: "mcDarkOak" });
    stairsDown(R, 10, 0, 2, { to: "rival1", spawn: STAIRS_FOOT, paper: "mcNavy" });
    deskPC(R, 0.4, 0, "s", R.screen("pcLocked", 40, 24, drawLocked, 1), { top: "walnut", lines: ["His PC's locked. The password hint just says: ME."] });
    drawerUnit(R, 2.8, 0, "s", 0.9, 1.1, "walnut");
    officeChair(R, 1.55, 1.6, "n", "redFabric");
    windowN(R, 4.6, 1.5, 2.2, 2.0, { curtains: "curtain", curtainIn: 0.35, lines: ["From up here you can see right into your own room."] });
    framed(R, 7.4, 1.5, 1.2, 1.6, "posterRival", "n", { frame: false, act: { label: "Look", lines: ["A poster: CHAMP OR BUST."] } });
    framed(R, 2.4, 1.6, 1.3, 1.75, "posterDragon", "e", { frame: false, act: { label: "Look", lines: ["A poster of a dragon breathing fire over the stars."] } });
    R.item("Posters", "CHAMP OR BUST, and a dragon over the stars.");
    bunting(R, 0.3, 4.4, 3.1);
    bed(R, 0, 5.4, "e", { w: 1.8, l: 2.9, wood: "walnut", quilt: "quiltBlue", ask: false });
    R.acts[R.acts.length - 1].lines = ["An unmade bed, clothes all over it."];
    framed(R, 7.4, 1.6, 0.8, 0.8, "dartboard", "w", { frame: false, act: { label: "Look", lines: ["A dartboard. All the darts are in the bullseye."] } });
    R.item("Dartboard", "On the west wall; every dart in the middle.");
    bookshelf(R, 14, 8.6, "w", 2.2, 2.6, { trinkets: { 0: "trophy", 1: "trophy", 2: "ball" }, lines: ["Trophies, and a ball signed by someone famous."], name: "Trophy shelf", note: "Walnut shelves with gold cups, books and a ball." });
    rug(R, 4, 4, 10, 10, "rugRival");
    punchingBag(R, 11.6, 5.4);
    beanBag(R, 8.6, 8.4, "redFabric");
    dumbbells(R, 5.2, 9.4);
    bagOnFloor(R, 2.4, 10.4, ["A sports bag, half unpacked. Running shoes, a towel, more trophies."]);
    plant(R, 1.0, 11.0, 1.0, { lines: ["A cactus. It looks like it gets on with him."] });
    R.spawn = STAIRS_TOP;
    return R;
  },

  lab() {
    // Inside the lab, as Let's Go's, tighter (16 by 14 past the nook): pale oak boards laid on the diagonal, a wooden
    // skirting; the ball incubator in the back corner, the professor's teal desk with his PC and microscope, the board
    // with the map over it, notes pinned up, the resting machine and a white bookshelf; the table of three balls (a
    // green top) in the middle on the right; two rows of white shelves with a way between them; an aide's desk and
    // board in the corner; the purple mat in its nook at the door, low white shelves either side.
    const R = new Room("lab", { name: "Prof. Oak's lab", floor: "Ground floor", W: 16, D: 16, outsideSize: "12 by 7" });
    shell(R, { paper: "mcLabWall", floor: "oakDiagonal", door: LAB_DOOR, entry: true, entryFloor: "mcLabTile", skirt: "walnut", skirtH: 0.5, ledge: "shelfWhite", ledgeTop: "lab" });
    incubator(R, 2.0, 2.6);
    { const F = R.F(4.0, 0, "s");
      counterUnit(R, F, 0, 1, "cupboardTeal", "lab", "tealPaint"); counterUnit(R, F, 1, 1, "drawers", "lab", "tealPaint");
      counterUnit(R, F, 2, 1, "cupboardTeal", "lab", "tealPaint"); counterUnit(R, F, 3, 1, "drawers", "lab", "tealPaint"); }
    { const G = R.geo, sk = R.screen("pcLab", 40, 26, drawPC, 0.9), X = 6.0;
      G.box(X + 0.75, 1.0, 0.18, X + 1.2, 1.04, 0.45, "greyPlastic"); G.box(X + 0.92, 1.04, 0.25, X + 1.03, 1.2, 0.35, "greyPlastic"); G.box(X + 0.48, 1.2, 0.18, X + 1.48, 1.88, 0.32, "greyPlastic"); G.box(X + 0.54, 1.26, 0.32, X + 1.42, 1.82, 0.33, { s: sk, all: null });
      G.box(X + 0.55, 1.0, 0.5, X + 1.4, 1.03, 0.74, { top: "keyboard", all: "greyPlastic" });
      G.box(4.35, 1.0, 0.3, 4.6, 1.08, 0.6, "greyPlastic"); G.box(4.44, 1.08, 0.38, 4.52, 1.5, 0.46, "steel"); G.box(4.38, 1.42, 0.36, 4.58, 1.56, 0.62, "greyPlastic");
      R.act([4.0, 0, 8.0, 1.8], { label: "Use", lines: ["The professor's PC. Notes on every creature he's met, and a lot of unread email.", "Numbers scroll by: a creature's heartbeat, its height and its weight."] });
      R.item("Teal desk", "Teal cupboards and drawers, a white top, the PC and a microscope."); }
    officeChair(R, 6.6, 1.75, "n", "blueFabric");
    wallMap(R, "n", 4.3, 2.0, 3.0, { frameMat: "silver", lines: ["A map of the whole region: every town, every route.", "The professor's pinned a note to it: THREE NEW TRAINERS, THREE NEW PARTNERS."] });
    framed(R, 8.5, 1.9, 0.9, 1.2, "boardLab", "n", { frameMat: "walnut", act: { label: "Read", lines: ["A notice: RESEARCH IN PROGRESS. PLEASE DON'T TOUCH THE BALLS."] } });
    framed(R, 9.7, 1.9, 0.9, 1.2, "boardLab", "n", { frameMat: "walnut", act: { label: "Read", lines: ["A chart of how creatures grow, with a big question mark at the end."] } });
    healer(R, 11.0, 2);
    labShelf(R, 13.4, 0, "s", 2.4, 3.0, { lines: ["Books on creatures: their habits, their homes, how they change as they grow."] });
    boxes(R, 14.2, 1.3);
    ballTable(R, 9.8, 4.2, 13.0, 5.6, { top: "feltGreen", leg: "lab", note: "A white table with a green felt top, three balls in a row." });
    // Two rows of white shelves, a way between them.
    for (const X of [0.4, 2.4]) labShelf(R, X, 8.0, "s", 2, 3.0, { lines: ["Books on creatures: their habits, their homes, how they change as they grow.", "It's all a bit much to take in."] });
    for (const X of [11.6, 13.6]) labShelf(R, X, 8.0, "s", 2, 3.0, { rows: ["binders", "booksFull", "storeBoxes", "booksFull"], lines: ["Research papers. One of them is called ON THE KEEPING OF SMALL CREATURES IN BALLS."] });
    // The aide's corner, bottom left: a desk against the west wall, his chair, a board on a stand.
    deskPC(R, 0, 12.8, "e", R.screen("pcAide", 40, 26, drawLab, 0.9), { top: "lab", lines: ["The aide's PC: a spreadsheet of heights and weights, half filled in."] });
    officeChair(R, 1.6, 11.65, "w", "blueFabric");
    boardStand(R, 2.6, 10.0, 2.4, "s", ["NOTES, in marker: three lines of questions about sleep, food and growing.", "And a doodle of a very round, very green creature."]);
    crossWindow(R, "e", 9.8, 1.5, 2.0, 1.4, ["Out of the window: Pallet Town, and the sea past the houses."]);
    boxes(R, 14.6, 12.6); plant(R, 13.4, 12.8, 1.1); plant(R, 15.0, 6.8, 1.0);
    doormat(R, LAB_DOOR[0], LAB_DOOR[1], { to: "town", spawn: FROM_LAB, mat: "matLabPurple" });
    R.npcs.push({ name: "Prof. Oak", x: 7.6, z: 3.6, yaw: FACE.s, look: { skin: "oak" },
      talk: f => [
        { text: "Ah, there you are! I've been waiting." },
        f ? { text: "And you've brought a friend. Good, good. A creature that trusts you is a fine start." } : { text: "Those three balls on the table: each holds a creature. One of them is yours." },
        { text: "Take your time choosing. It's a partner for life." }
      ] });
    R.npcs.push({ name: "Blue", x: 11.4, z: 6.6, yaw: FACE.n, look: { skin: "blue" },
      talk: () => [{ text: "Gramps said I could pick first. Well, after you. He made me promise." }, { text: "Hurry up, will you? I've got places to be." }] });
    R.npcs.push({ name: "Aide", x: 4.8, z: 1.8, yaw: FACE.n, look: { skin: "scientist" },
      talk: () => [{ text: "I'm the professor's aide. These readings won't record themselves!" }] });
    R.npcs.push({ name: "Aide", x: 1.6, z: 11.65, yaw: FACE.w, seated: true, look: { skin: "scientist" },
      talk: () => [{ text: "The professor's writing a book. Every creature in the region, one page each." }, { text: "He's only up to the second page." }] });
    R.npcs.push({ name: "Aide", x: 12.6, z: 9.6, yaw: FACE.n, look: { skin: "scientist_f" },
      talk: () => [{ text: "Have you got a partner yet? The professor gives every new trainer one of their own." }, { text: "Look after it, and it'll look after you." }] });
    R.npcs.push({ name: "Aide", x: 3.4, z: 9.6, yaw: FACE.n, look: { skin: "scientist_f" },
      talk: () => [{ text: "I'm filing the notes from ROUTE 1. Bird POKéMON, mostly. And a lot of RATTATA." }] });
    R.spawn = LAB_IN;
    return R;
  },

  center() {
    // A Pokémon Center as Let's Go's, compact: the counter curving round the nurse, a big ball at each end of it; her
    // healing machine right behind her under the big screen (it shows your POKéMON while they rest); her PC; the
    // storage PC on the right wall with the world map past it; a magazine rack back left; footstools round two coffee
    // tables each side; blue posts where the slanted corners meet the walls, the front corners slanted low as well; a
    // ring in the floor at the door, the mat in its nook.
    const R = new Room("center", { name: "Pokémon Center", floor: "Viridian City", W: 18, D: 14, outsideSize: "13 by 9" });
    shell(R, { paper: "paperCenter", floor: "mcPeachCheck", door: CENTER_DOOR, entry: true, entryFloor: "mcCenterCheck", skirt: "redPlastic", ledge: "paint", ledgeTop: "lab" });
    band(R, 1.05, "orangeTrim");
    chamfer(R, 2, { paper: "paperCenter", skirt: "redPlastic", band: [1.05, "orangeTrim"] });
    frontChamfer(R, 2);
    for (const west of [true, false]) { wallPost(R, west, 2.0, 2.5, "postBlue"); wallPost(R, west, R.Dm - 2.5, R.Dm - 2.0, "postBlue"); }
    R.item("Blue posts", "Blue posts where the slanted corners meet the walls, front and back.");
    const G = R.geo, cx = 9.0;
    curvedCounter(R, cx);
    statueBall(R, cx - 4.1, 3.7, 1.15); statueBall(R, cx + 4.1, 3.7, 1.15);
    G.box(cx - 0.4, 1.15, 4.5, cx + 0.4, 1.28, 5.1, "greyPlastic"); G.box(cx - 0.1, 1.28, 4.65, cx + 0.1, 1.34, 4.85, "gold");
    till(R, cx + 2.0, 4.8, 1.15, "s");
    R.act([cx - 3, 5.2, cx + 3, 6.4], { label: "Talk", lines: ["The counter. The nurse will look after your POKéMON."] });
    // Behind her: the machine under the big screen, and her PC.
    healer(R, cx - 1.5, 3, { empty: true });
    wallScreen(R, cx - 2.4, 2.55, 4.8, 1.3, R.screen("centerAd", 48, 14, drawCenterAd, 1), ["The big screen. While your POKéMON rest, it shows them, one by one."]);
    deskPC(R, cx + 2.0, 0, "s", R.screen("pcNurse", 40, 26, drawPC, 0.9), { top: "lab", lines: ["The nurse's PC: today's patients, and a note to order more POTIONS."] });
    officeChair(R, cx + 3.15, 1.6, "n", "redFabric");
    // The storage PC on the right wall, the world map past it; the magazine rack back left.
    deskPC(R, 18, 3.2, "w", R.screen("pcCenter", 40, 26, drawPC, 0.9), { top: "lab", pc: true, lines: ["The POKéMON STORAGE SYSTEM."] });
    officeChair(R, 16.4, 4.35, "e", "redFabric");
    wallMap(R, "e", 6.4, 1.4, 2.4, { lines: ["A map of the whole region. VIRIDIAN CITY is marked with a red pin.", "In game, this opens the TOWN MAP."] });
    bookshelf(R, 0, 5.4, "e", 2.2, 1.6, { wood: "pale", lines: ["Magazines: TRAINER WEEKLY, and a guide called RESTING IS GOOD FOR POKéMON TOO."], name: "Magazine rack", note: "A low rack of magazines and leaflets." });
    // Footstools round two coffee tables each side.
    for (const [x0, sx] of [[2.0, [1.0, 4.4]], [14.6, [13.6, 17.0]]]) for (const z0 of [6.6, 9.0]) {
      glassTable(R, x0, z0, x0 + 1.4, z0 + 1.2, { lines: ["A coffee table. Someone's left a puzzle book open: a crossword about BUG POKéMON."] });
      for (const x of sx) footstool(R, x, z0 + 0.6);
    }
    rug(R, 5, 6, 13, 12, "centerRing");
    plant(R, 5.4, 11.0, 0.95, { lines: ["A big leafy plant. Its leaves are very shiny."] }); plant(R, 12.6, 11.0, 0.95, { lines: ["A big leafy plant."] });
    doormat(R, CENTER_DOOR[0], CENTER_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matCenter" });
    R.npcs.push({ name: "Nurse", role: "nurse", key: "viridian_nurse", x: cx, z: 3.0, yaw: FACE.s, look: { skin: "nurse" },
      talk: () => [{ text: "Hello, and welcome to the POKéMON CENTER!" }, { text: "We heal tired POKéMON back to full health. Shall we heal yours?" }] });
    R.npcs.push({ name: "Gentleman", x: 15.6, z: 5.9, yaw: FACE.e, look: { skin: "gentleman" },
      talk: () => [{ text: "You can use that PC in the corner." }, { text: "The receptionist told me. So kind!" }] });
    R.npcs.push({ name: "Youngster", x: 5.2, z: 8.4, yaw: FACE.w, look: { skin: "youngster" },
      talk: () => [{ text: "There's a POKéMON CENTER in every town ahead." }, { text: "They don't charge any money either!" }] });
    R.spawn = CENTER_IN;
    return R;
  },

  mart() {
    // A Poké Mart as Let's Go's: the blue counter at the front on the left, its end rounded, the clerk behind it with
    // his stock at his back; the screen on the back wall and the drinks coolers along it to the right; island shelves
    // on the right; a blue and white diamond floor; a plant by the door. The mat in its nook.
    const R = new Room("mart", { name: "Poké Mart", floor: "Viridian City", W: 14, D: 12, outsideSize: "13 by 9" });
    shell(R, { paper: "paperMart", floor: "mcDiamondBlue", door: MART_DOOR, entry: true, entryFloor: "mcMintCheck", skirt: "blueTrim", ledge: "paint", ledgeTop: "lab" });
    band(R, 1.05, "blueTrim");
    chamfer(R, 2, { paper: "paperMart", skirt: "blueTrim", band: [1.05, "blueTrim"] });
    const G = R.geo, h = 1.05;
    for (const [x0, x1, z0, z1] of [[0, 3.6, 4.0, 4.6], [2.6, 3.6, 4.6, 8.4], [0, 2.6, 7.9, 8.4], [2.7, 3.5, 8.4, 8.65], [2.9, 3.3, 8.65, 8.8]]) {
      G.box(x0, 0, z0, x1, h, z1, { all: "counterBlue", top: null, bottom: null });
      G.box(x0, h, z0, x1, h + 0.1, z1, "lab");
      R.solid(x0, z0, x1, z1);
    }
    till(R, 3.1, 6.4, 1.15, "e");
    for (const [bx, bz] of [[2.95, 5.0], [3.25, 5.2], [3.0, 5.45]]) G.box(bx - 0.13, 1.15, bz - 0.13, bx + 0.13, 1.41, bz + 0.13, { all: "ball", top: "ballTop" });
    R.act([3.6, 4.6, 5.0, 8.4], { label: "Talk", lines: ["The counter. The clerk sells everything on the shelves."] });
    R.item("The counter", "Blue with a white top, its end rounded; the till and a little basket of balls.");
    martShelf(R, 0, 7.8, "e", 3.2, 2.4, { seed: 1, depth: 0.6, lines: ["The stock behind the counter: boxes of POKé BALLS, POTIONS by the dozen."] });
    wallScreen(R, 2.4, 1.7, 2.4, 1.3, R.screen("martAd", 40, 22, drawMartAd, 1), ["An advert: a ball bouncing past a POTION and a can of REPEL. NEW STOCK IN EVERY WEEK!"]);
    cooler(R, 5.2, 0, "s", 2); cooler(R, 7.2, 0, "s", 2); cooler(R, 9.2, 0, "s", 2);
    islandShelf(R, 10.0, 3.2, 4, 1.6, { lines: ["POTIONS, ANTIDOTES and POKé BALLS, both sides. The clerk at the counter sells them."] });
    martShelf(R, 14, 3.0, "w", 4, 2.2, { seed: 2, lines: ["REPELS, ESCAPE ROPES and things for camping. Most of them aren't in stock yet."] });
    // The freezers in the middle, back to back.
    chestFreezer(R, 5.0, 3.4, "s", 2); chestFreezer(R, 7.0, 3.4, "n", 2);
    plant(R, 1.0, 9.3, 1.1, { lines: ["A plant by the door. It looks well looked after."] });
    doormat(R, MART_DOOR[0], MART_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matMart" });
    R.npcs.push({ name: "Clerk", role: "clerk", key: "viridian_city", x: 1.4, z: 6.4, yaw: FACE.e, look: { skin: "clerk" },
      talk: () => [{ text: "Hi there! May I help you?" }] });
    R.npcs.push({ name: "Youngster", x: 7.6, z: 1.8, yaw: FACE.n, look: { skin: "youngster" },
      talk: () => [{ text: "No! POTIONs are all sold out." }] });
    R.npcs.push({ name: "Lady", x: 12.0, z: 5.0, yaw: FACE.e, look: { skin: "middle_aged_woman" },
      talk: () => [{ text: "This shop sells many ANTIDOTEs." }] });
    R.spawn = MART_IN;
    return R;
  },

  school() {
    // Viridian's Trainer School, as Let's Go's: small, dark wide boards, blush pink posts in the corners, no band
    // round the walls; the board on a piece of wall that comes out at the front, the teacher by it with a window
    // behind her; a solid dark bookcase in the right-hand corner (drawers along its bottom); the desks in the middle
    // side by side, pupils at three of them; pictures on the left-hand wall, a window on the right. The mat in its nook.
    const R = new Room("school", { name: "Trainer School", floor: "Ground floor", W: 12, D: 11, outsideSize: "10 by 6" });
    shell(R, { paper: "paperSchool", floor: "darkBoards", door: SCHOOL_DOOR, entry: true, skirt: "schoolTrim", ledge: "walnut" });
    const G = R.geo, Dm = R.Dm;
    for (const [x, z] of [[0, 0], [11.58, 0], [0, Dm - 0.43], [11.58, Dm - 0.43]]) cornerPost(R, x, z, 0.42, "postPink");
    // The front: a piece of wall standing out from the back wall, the board on it, its chalk ledge.
    G.box(4, 0, 0, 8, R.H, 0.5, { s: "paperSchool", e: "paperSchool", w: "paperSchool", top: "cap" });
    G.box(4, 0, 0.5, 8, 0.24, 0.56, "schoolTrim");
    R.solid(4, 0, 8, 0.5);
    G.box(4.25, 1.2, 0.5, 7.75, 3.0, 0.58, "walnut");
    G.box(4.38, 1.33, 0.58, 7.62, 2.87, 0.59, { s: "chalkboard", all: null });
    G.box(4.25, 1.12, 0.5, 7.75, 1.2, 0.72, "walnut");
    const board = ["The BLACKBOARD: what can go wrong for a POKéMON in battle.",
      "SLEEP: it can't attack until it wakes up. That can take a few turns.",
      "POISON: it loses HP every turn, and as you walk about with it too.",
      "PARALYSIS: it's slower, and some turns it can't move at all.",
      "BURN: it loses HP every turn, and its physical attacks are weaker.",
      "FREEZE: it can't move at all until it thaws out.",
      "A POKéMON CENTER puts every one of them right."];
    R.act([4.25, 0.5, 7.75, 1.2], { label: "Read", lines: board });
    crossWindowN(R, 1.0, 1.6, 2.0, 1.4, ["Outside: the POKéMON CENTER's red roof."]);
    bookshelf(R, 9.4, 0, "s", 2.1, 2.6, { wood: "mcDarkOak", drawers: true, lines: ["Books on POKéMON: TYPES, MOVES, and HOW TO CARE FOR YOUR FIRST POKéMON.", "The drawers along the bottom are full of chalk."], name: "Bookcase", note: "Solid dark wood, books on its shelves, drawers along the bottom." });
    framed(R, 2.4, 1.7, 1.2, 0.9, "painting", "w", { act: { label: "Look", lines: ["A painting of the sea south of PALLET TOWN."] } });
    framed(R, 4.6, 1.7, 1.2, 0.9, "paintingHills", "w", { act: { label: "Look", lines: ["A painting of the hills round VIRIDIAN CITY."] } });
    R.item("Pictures", "Two paintings on the left-hand wall.");
    crossWindow(R, "e", 3.6, 1.5, 1.8, 1.4, ["Outside: VIRIDIAN CITY's houses, and the GYM's roof over the trees."]);
    // The desks: side by side in twos, two rows, the way to the board between them, a pupil at three.
    const notebook = ["A notebook, left open. TRAINER TIPS, page 1: weaken a wild POKéMON before you throw a POKé BALL.",
      "Page 2: a POKéMON that's asleep or paralysed is easier to catch.",
      "Page 3: you can carry six POKéMON. Any more you catch go to the PC in a POKéMON CENTER.",
      "Page 4: wild POKéMON live in the tall grass. Watch where you walk!"];
    schoolDesk(R, 3.25, 3.4, { book: true }); schoolDesk(R, 4.35, 3.4, { book: true, lines: notebook });
    schoolDesk(R, 7.65, 3.4); schoolDesk(R, 8.75, 3.4, { book: true });
    schoolDesk(R, 3.25, 5.8); schoolDesk(R, 4.35, 5.8, { book: true });
    schoolDesk(R, 7.65, 5.8, { book: true }); schoolDesk(R, 8.75, 5.8);
    palm(R, 0.9, 7.2, 0.9); palm(R, 11.1, 7.2, 0.9);
    doormat(R, SCHOOL_DOOR[0], SCHOOL_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matSchool" });
    R.npcs.push({ name: "Teacher", key: "viridian_school_teacher", x: 2.4, z: 1.6, yaw: FACE.s, look: { skin: "school_teacher" },
      talk: () => [{ text: "Welcome to the TRAINER SCHOOL! Shall I ask you a question?" }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_kid", x: 3.25, z: 4.18, yaw: FACE.n, seated: true, look: { skin: "school_kid" },
      talk: () => [{ text: "Whew! I'm trying to learn all my notes by heart." }, { text: "The test is about STATUS problems. Look at the board!" }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_girl", x: 8.75, z: 4.18, yaw: FACE.n, seated: true, look: { skin: "school_kid_f" },
      talk: () => [{ text: "The notebook on the desk up front has TRAINER TIPS in it." }, { text: "Have a read! It's really useful." }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_kid2", x: 7.65, z: 6.58, yaw: FACE.n, seated: true, look: { skin: "schoolboy" },
      talk: () => [{ text: "A burnt POKéMON's attacks are weaker? I never knew that!" }, { text: "I'd better write it down." }] });
    R.spawn = SCHOOL_IN;
    return R;
  },

  nickhouse() {
    // The house above the Trainer School, as Let's Go's: darker wide boards, posts in the corners; the kitchen down
    // the left-hand wall with the bin by the fridge; a cupboard, the TV on its unit (a DVD player and books in it) and
    // a sideboard with books on it along the back, a clock over the sideboard; a desk on the right with a white lamp
    // and a red book, under a window; the table in the middle on a green rug (a white top in a wooden trim, a fruit
    // bowl, four cushioned chairs). A father and his little girl, and their SPEAROW's perch. The mat in its nook.
    const R = new Room("nickhouse", { name: "Viridian house", floor: "Ground floor", W: 12, D: 11, outsideSize: "10 by 6" });
    shell(R, { paper: "paperSchool", floor: "midBoards", door: VHOUSE_DOOR, entry: true, skirt: "beam", ledge: "walnut" });
    band(R, 1.0, "beam", 0.12);
    const G = R.geo, Dm = R.Dm;
    for (const [x, z] of [[0, 0], [11.58, 0], [0, Dm - 0.43], [11.58, Dm - 0.43]]) cornerPost(R, x, z);
    kitchen(R, 0, "e", { z: 6.6 });
    bin(R, 0.5, 7.15);
    sideboard(R, 1.5, 0, "s", 1.6, { lines: ["A cupboard. Inside: a bag of bird seed, and SPEARY's spare perch."] });
    tvUnit(R, 3.5, 3.0, R.screen("tvHouseV", 40, 24, drawFilm, 0.9), { lines: ["A nature programme: a flock of SPEAROW over the sea."] });
    { const F = R.F(7.2, 0, "s"), w = 2.6;
      F.box(0, 0, 0, w, 0.1, 0.62, "cap");
      F.box(0, 0.1, 0, w, 0.92, 0.66, { all: "walnut", front: null });
      for (let i = 0; i < 2; i++) F.box(i * w / 2 + 0.03, 0.12, 0.66, (i + 1) * w / 2 - 0.03, 0.9, 0.67, { front: "drawers", all: null });
      F.box(-0.04, 0.92, -0.02, w + 0.04, 0.99, 0.7, "walnut");
      F.box(0.2, 0.99, 0.1, 1.5, 1.44, 0.55, { front: "books", all: "linen", top: "linen" });
      F.box(1.8, 0.99, 0.2, 2.1, 1.3, 0.5, "potWhite"); F.box(1.7, 1.3, 0.1, 2.2, 1.65, 0.6, "leafSolid");
      F.solid(0, 0, w, 0.7);
      R.act(F.rect(0, 0.3, w, 1.3), { label: "Look", lines: ["Books on the sideboard: NAMING YOUR POKéMON, and A THOUSAND NICKNAMES FOR BIRD POKéMON."] });
      R.item("Sideboard", "Walnut, two drawers, a row of books and a plant on top."); }
    clock(R, 8.1, 2.3);
    // The desk on the right, under a window: a white lamp, a red book.
    table(R, 10.9, 2.4, 11.85, 4.4, { top: "walnut", leg: "walnut", h: 0.8, name: "Desk", note: "A small walnut desk under the window: a lamp with a white shade, a red book." });
    deskLamp(R, 11.4, 2.8, 0.8);
    G.box(11.05, 0.8, 3.5, 11.6, 0.9, 4.1, { all: "redPlastic", top: "linen" });
    chair(R, 10.3, 3.4, "e");
    R.act([10.0, 2.4, 11.9, 4.4], { label: "Look", lines: ["A red book on the desk: SPEARY'S FIRST YEAR. Every page has a photo of SPEARY."] });
    crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, ["Outside: VIRIDIAN CITY's rooftops."]);
    crossWindow(R, "e", 5.8, 1.5, 1.6, 1.3, ["Outside: the TRAINER SCHOOL's green roof."]);
    G.box(11.1, 0, 6.5, 11.3, 1.3, 6.7, "walnut"); G.box(10.7, 1.3, 6.55, 11.7, 1.38, 6.65, "walnut");
    R.act([10.4, 6.1, 12, 7.1], { label: "Look", lines: ["A perch with a name carved on it: SPEARY.", "SPEARY's out for a fly."] });
    // The table in the middle on the green rug: a white top in a wooden trim, a fruit bowl, four cushioned chairs.
    G.box(3.6, 0, 3.4, 8.4, 0.03, 7.4, { top: "rugGreen", all: "cap", bottom: null });
    table(R, 4.8, 4.4, 7.2, 6.4, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A white top in a walnut trim, a bowl of fruit in the middle." });
    G.box(4.74, 0.74, 4.34, 7.26, 0.88, 4.44, "walnut"); G.box(4.74, 0.74, 6.36, 7.26, 0.88, 6.46, "walnut");
    G.box(4.74, 0.74, 4.44, 4.84, 0.88, 6.36, "walnut"); G.box(7.16, 0.74, 4.44, 7.26, 0.88, 6.36, "walnut");
    fruitBowl(R, 6.0, 5.4, 0.86);
    for (const z of [4.95, 5.85]) { chair(R, 4.15, z, "e", { seat: "cushion" }); chair(R, 7.85, z, "w", { seat: "cushion" }); }
    R.item("Chairs", "Four walnut chairs with yellow cushions, two each side.");
    plant(R, 10.8, 7.7, 0.85, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    doormat(R, VHOUSE_DOOR[0], VHOUSE_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHouseV" });
    R.npcs.push({ name: "Father", key: "viridian_house_dad", x: 2.4, z: 3.4, yaw: FACE.e, look: { skin: "middle_aged_man" },
      talk: () => [{ text: "Coming up with nicknames is fun, but it's harder than you'd think." }, { text: "Simple names are the easiest to remember." }] });
    R.npcs.push({ name: "Little girl", key: "viridian_house_girl", x: 9.4, z: 6.6, yaw: FACE.w, look: { skin: "little_girl" },
      talk: () => [{ text: "My daddy loves POKéMON too!" }, { text: "Our SPEAROW is called SPEARY. Daddy named it!" }] });
    R.spawn = VHOUSE_IN;
    return R;
  },

  trainer2() {
    // Downstairs: the battle room. A court marked out on the wooden floor, today's challenger waiting at its far
    // end, the scoreboard over them, benches along the sides, the stairs back up.
    const R = new Room("trainer2", { name: "Trainer House", floor: "B1F", W: 16, D: 16 });
    shell(R, { paper: "paperTrainerB", floor: "courtWood", skirt: "orangeTrim" });
    band(R, 1.05, "orangeTrim");
    stairsUp(R, 12, 0, 2, { to: "school", spawn: SCHOOL_TOP });
    rug(R, 3, 4, 13, 15, "court");
    wallScreen(R, 4.4, 1.6, 4.6, 1.9, R.screen("thBoard", 44, 20, drawScoreboard, 1), ["The scoreboard: TODAY'S CHALLENGER, and a tally of everyone they've beaten."]);
    sofa(R, 0, 10.6, "e", 3, { fabric: "blueFabric" }); sofa(R, 16, 7.6, "w", 3, { fabric: "yellowFabric" });
    plant(R, 1.0, 1.0, 1.1); plant(R, 1.0, 15.0, 1.1); plant(R, 15.0, 15.0, 1.1);
    R.npcs.push({ name: "Cooltrainer", role: "trainer", key: "trainer_house", x: 8.0, z: 5.4, yaw: FACE.s, look: { skin: "cooltrainer_m" },
      talk: () => [{ text: "I'm today's challenger. Think you can beat me?" }, { text: "Win, and the prize upstairs is yours!" }] });
    R.spawn = TH_FOOT;
    return R;
  },

  gate_s() {
    // Route 2's gate south of Viridian Forest, as Let's Go's gates are (one design for every gate; who's in it is its
    // own): a cream and yellow check, blue lower walls, a long blue runner from the door to the way on at the north,
    // palms by the way on, stools round glass tables, people waiting. The way on to the forest goes nowhere yet.
    const R = new Room("gate_s", { name: "Route 2 gate", floor: "South", W: 14, D: 12, outsideSize: "15 by 12" });
    lgGate(R, { south: { to: "town" }, north: { none: true, lines: ["The way on to VIRIDIAN FOREST."] } });
    R.npcs.push({ name: "Girl", key: "gate_s_girl", x: 3.0, z: 5.6, yaw: FACE.e, look: { skin: "girl" },
      talk: () => [{ text: "Some POKéMON only live in forests and caves." }, { text: "Look everywhere if you want to find them all!" }] });
    R.npcs.push({ name: "Lady", key: "gate_s_lady", x: 12.6, z: 6.9, yaw: FACE.w, seated: true, look: { skin: "lady" },
      talk: () => [{ text: "VIRIDIAN FOREST is a natural maze." }, { text: "Stick to the path, or you'll be wandering in circles!" }] });
    R.spawn = LG_GATE_IN;
    return R;
  },

  gate_n() {
    // The gate on the forest's north side, the same design: in from Route 2 through the way at the north, the forest
    // to the south (that way goes nowhere yet).
    const R = new Room("gate_n", { name: "Route 2 gate", floor: "North", W: 14, D: 12, outsideSize: "15 by 16" });
    lgGate(R, { north: { to: "town" }, south: { none: true, lines: ["The way back into VIRIDIAN FOREST."] } });
    R.npcs.push({ name: "Youngster", key: "gate_n_kid", x: 3.0, z: 7.0, yaw: FACE.e, look: { skin: "youngster" },
      talk: () => [{ text: "Off to PEWTER CITY? Its GYM LEADER, BROCK, uses ROCK-type POKéMON." }, { text: "WATER and GRASS moves work well on ROCK!" }] });
    R.npcs.push({ name: "Hiker", key: "gate_n_hiker", x: 12.6, z: 3.7, yaw: FACE.w, seated: true, look: { skin: "hiker" },
      talk: () => [{ text: "Phew! That forest goes on and on." }, { text: "It's easy to lose your way among all those trees." }] });
    R.spawn = LG_GATE_FROM_N;
    return R;
  },

  arena_lab() {
    // The lab's battle (your rival's, when you bring Oak's Parcel back), as Let's Go's: a round hall with the lab's
    // pale diagonal boards and walls, its teal desk with the PC and microscope at the back under the map, the
    // incubator to the left, white shelves and boxes to the right. You battle across the middle, facing the back.
    const R = new Room("arena_lab", { name: "Lab battle", floor: "Battle arena", W: 24, D: 24, H: 7 });
    arenaShell(R, { floor: "oakDiagonal", paper: "mcLabWall", low: "walnut", beam: "walnut", ceiling: "mcLabWall" });
    { const F = R.F(9.5, 1.05, "s");
      for (let i = 0; i < 5; i++) counterUnit(R, F, i, 1, i % 2 ? "drawers" : "cupboardTeal", "lab", "tealPaint"); }
    { const G = R.geo, sk = R.screen("pcArena", 40, 26, drawPC, 0.9), X = 11.5, Z = 1.05;
      G.box(X + 0.75, 1.0, Z + 0.18, X + 1.2, 1.04, Z + 0.45, "greyPlastic"); G.box(X + 0.92, 1.04, Z + 0.25, X + 1.03, 1.2, Z + 0.35, "greyPlastic");
      G.box(X + 0.48, 1.2, Z + 0.18, X + 1.48, 1.88, Z + 0.32, "greyPlastic"); G.box(X + 0.54, 1.26, Z + 0.32, X + 1.42, 1.82, Z + 0.33, { s: sk, all: null });
      G.box(X + 0.55, 1.0, Z + 0.5, X + 1.4, 1.03, Z + 0.74, { top: "keyboard", all: "greyPlastic" });
      G.box(10.0, 1.0, Z + 0.3, 10.25, 1.08, Z + 0.6, "greyPlastic"); G.box(10.09, 1.08, Z + 0.38, 10.17, 1.5, Z + 0.46, "steel"); G.box(10.03, 1.42, Z + 0.36, 10.23, 1.56, Z + 0.62, "greyPlastic"); }
    officeChair(R, 12.1, 2.85, "n", "blueFabric");
    // The map over the desk, on the wall behind it.
    { const G = R.geo, w = 3.4, h = w * (WORLDMAP.h + 6) / (WORLDMAP.w + 6), x = 10.3, y = 1.95, z = 1.0;
      G.box(x - 0.1, y - 0.1, z, x + w + 0.1, y + h + 0.1, z + 0.08, "silver"); G.box(x, y, z + 0.08, x + w, y + h, z + 0.09, { s: "worldMap", all: null }); }
    incubator(R, 5.6, 5.4);
    labShelf(R, 15.6, 2.2, "s", 2, 3.0); labShelf(R, 17.8, 3.9, "s", 2, 3.0, { rows: ["binders", "booksFull", "storeBoxes", "booksFull"] });
    boxes(R, 19.8, 6.6); boxes(R, 4.2, 15.6);
    plant(R, 3.6, 8.6, 1.2); plant(R, 20.4, 9.2, 1.1);
    deskPC(R, 1.0, 14.4, "e", R.screen("pcArena2", 40, 26, drawLab, 0.9), { top: "lab" });
    boxes(R, 20.6, 14.0); plant(R, 19.6, 17.8, 1.0);
    R.spawn = { x: 12, z: 14.2, yaw: FACE.n };
    return R;
  },

  town() {
    const R = new Room("town", { name: "Pallet Town", floor: "Outside (a stand-in)", W: 34, D: 24, H: 6, outside: true });
    ground(R, "grass");
    patch(R, 15, 0.4, 19, 23.6, "path"); patch(R, 4, 11, 31, 13, "path");
    patch(R, 6.0, 9.2, 7.8, 11, "path"); patch(R, 24.0, 9.2, 25.8, 11, "path"); patch(R, 23.5, 13, 26.5, 15.6, "path");
    house(R, 4, 3, { roof: "roofRed", shutter: "redFabric", to: "house1", spawn: FROM_TOWN, signKey: "signYou", signText: "YOUR HOUSE", signRead: "YOUR HOUSE.", mail: "A letter for Mum, and a flyer about a new shop in Viridian.", name: "Your house (outside)" });
    house(R, 22, 3, { roof: "roofBlue", shutter: "blueFabric", to: "rival1", spawn: FROM_TOWN, signKey: "signRival", signText: "RIVAL'S HOUSE", signRead: "RIVAL'S HOUSE.", mail: "A parcel addressed to Daisy, and a fan letter for her brother.", name: "Rival's house (outside)" });
    lab(R, 19, 15.6, { to: "lab", spawn: LAB_IN });
    picket(R, 3, 10.4, 5.6, 10.4); picket(R, 8.4, 10.4, 13, 10.4); picket(R, 21, 10.4, 23.6, 10.4); picket(R, 26.4, 10.4, 31, 10.4);
    flowers(R, 9, 13.6, 12.6, 15.6); flowers(R, 4, 13.6, 7, 14.6); flowers(R, 20, 13.6, 22, 14.6);
    for (const x of [1.2, 3.6, 6, 8.4, 10.8, 13.2, 20.8, 23.2, 25.6, 28, 30.4, 32.8]) tree(R, x, 1.2, 0.95);
    for (const z of [3.6, 6.2, 8.8, 11.4, 14, 16.6, 19.2, 21.8]) { tree(R, 1.2, z, 0.95); tree(R, 32.8, z, 0.95); }
    for (const x of [3.6, 6, 8.4, 10.8, 13.2]) tree(R, x, 22.4, 0.95);
    tallGrass(R, 15, 0.4, 19, 1.6);
    R.act([15, 0.3, 19, 2.0], { label: "Look", lines: ["Tall grass: the way to Route 1. Wild creatures live in there.", "In game you can't go in until you have a partner of your own."] });
    R.solid(15, -5, 19, 1.4);
    textBoard("signTown", "PALLET TOWN", 44);
    R.geo.box(13.2, 0, 13.6, 13.3, 1.0, 13.7, "walnut"); R.geo.box(14.6, 0, 13.6, 14.7, 1.0, 13.7, "walnut");
    R.geo.box(12.9, 0.85, 13.62, 15.0, 1.45, 13.72, { s: "signTown", all: "walnut" });
    R.solid(12.9, 13.5, 15.0, 13.8);
    R.act([12.9, 13.4, 15.0, 13.9], { label: "Read", lines: ["PALLET TOWN. A quiet start to a long road."] });
    R.item("Town sign", "A wooden board with the town's name painted on.");
    R.item("Trees and fences", "Round trees in three tiers along the edges, white picket fences, and the tall grass of Route 1 to the north.");
    R.spawn = { x: 6.9, z: 10.0, yaw: FACE.s };
    return R;
  },
};
