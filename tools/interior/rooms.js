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
  { id: "gate_league", name: "Pokémon League Gate", floor: "Route 22", group: "Route 22" },
  { id: "gate_johto", name: "Johto Gate", floor: "Route 22", group: "Route 22" },
  { id: "arena_lab", name: "Lab battle", floor: "Battle arena", group: "Battle arenas" },
  { id: "arena_route", name: "Route battle", floor: "Battle arena (outside)", group: "Battle arenas" },
  { id: "town", name: "Pallet Town", floor: "Outside", group: "Outside" },
  { id: "pallet_looks", name: "Pallet Town looks", floor: "Outside (the mod's blocks)", group: "Outside" },
  { id: "viridian_looks", name: "Viridian City and Route 22 looks", floor: "Outside (the mod's blocks)", group: "Outside" },
  { id: "league_looks", name: "POKéMON LEAGUE gate looks", floor: "Outside (the mod's blocks)", group: "Outside" },
  { id: "pewter_looks", name: "Pewter City looks", floor: "Outside (the mod's blocks)", group: "Outside" },
  { id: "viridian_forest", name: "Viridian Forest", floor: "Always night", group: "Route 2" },
  { id: "route2_gate", name: "Route 2 gate", floor: "East lane", group: "Route 2" },
  { id: "route2_house", name: "Route 2 trade house", floor: "Ground floor", group: "Route 2" },
  { id: "pewter_gym", name: "Pewter Gym", floor: "Pewter City", group: "Pewter City" },
  { id: "arena_pewter_gym", name: "Pewter Gym battle", floor: "Battle arena", group: "Battle arenas" },
  { id: "museum_1f", name: "Pewter Museum of Science", floor: "1F", group: "Pewter City" },
  { id: "museum_2f", name: "Pewter Museum of Science", floor: "2F", group: "Pewter City" },
  { id: "pewter_house1", name: "Pewter house", floor: "By the museum", group: "Pewter City" },
  { id: "pewter_house2", name: "Pewter house", floor: "By the Center", group: "Pewter City" },
  { id: "digletts_n", name: "Diglett's Cave", floor: "North entrance (Route 2)", group: "Diglett's Cave" },
  { id: "digletts_b1f", name: "Diglett's Cave", floor: "B1F", group: "Diglett's Cave" },
  { id: "digletts_s", name: "Diglett's Cave", floor: "South entrance (Route 11)", group: "Diglett's Cave" },
  { id: "mt_moon_1f", name: "Mt. Moon", floor: "1F", group: "Mt. Moon" },
  { id: "mt_moon_b1f", name: "Mt. Moon", floor: "B1F", group: "Mt. Moon" },
  { id: "mt_moon_b2f", name: "Mt. Moon", floor: "B2F", group: "Mt. Moon" },
  { id: "fuchsia_gym", name: "Fuchsia Gym", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "safari_gate", name: "Safari Zone gate", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "fuchsia_office", name: "Safari Zone office", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "fuchsia_house1", name: "Bill's grandpa's house", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "fuchsia_house2", name: "Move Deleter's house", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "fuchsia_house3", name: "Safari Zone Warden's home", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "fuchsia_house4", name: "Fishing Guru's brother's house", floor: "Fuchsia City", group: "Fuchsia City" },
  { id: "safari_zone", name: "Safari Zone", floor: "The park", group: "Safari Zone" },
  { id: "safari_rest_center", name: "Rest house", floor: "Center area", group: "Safari Zone" },
  { id: "safari_rest_east", name: "Rest house", floor: "East (area 1)", group: "Safari Zone" },
  { id: "safari_rest_north", name: "Rest house", floor: "North (area 2)", group: "Safari Zone" },
  { id: "safari_rest_west", name: "Rest house", floor: "West (area 3)", group: "Safari Zone" },
  { id: "safari_secret_house", name: "Secret House", floor: "West (area 3)", group: "Safari Zone" },
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
const LG_GATE_DOOR = [6, 8], LG_GATE_NOOK = [5, 9], LG_GATE_IN = { x: 7.0, z: 12.5, yaw: FACE.n }, LG_GATE_FROM_N = { x: 7.0, z: 1.4, yaw: FACE.s };

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
  reg("paperCenter", 16, 16, P => wallCrisp(P, hx("#F8F5F1"), { stripes: 0.035 }));
  reg("paperMart", 16, 16, P => wallCrisp(P, hx("#F2F6FA"), { stripes: 0.035 }));
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
  reg("paperGate", 16, 16, P => wallCrisp(P, hx("#EEF1E6"), { stripes: 0.035 }));
  reg("gateBoards", 16, 16, (P, r) => { P.fill(hx("#C9A36B"), 0.05, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#B08A55")); });
  reg("greenTrim", 16, 16, (P, r) => { P.fill(hx("#4E9A4A"), 0.04, r); P.rect(0, 0, 16, 2, hx("#78C06E")); });
  // The Trainer House's own: warm cream walls (and cool grey downstairs), an orange trim, a peach check floor,
  // its counter, the battle room's wooden court floor.
  reg("paperTrainer", 16, 16, P => wallCrisp(P, hx("#FBF3E8"), { stripes: 0.035 }));
  reg("paperTrainerB", 16, 16, P => wallCrisp(P, hx("#E8EDF5"), { stripes: 0.035 }));
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
  reg("paperSchool", 16, 16, P => wallCrisp(P, hx("#F6EEDC"), { stripes: 0.035 }));
  reg("schoolTrim", 16, 16, (P, r) => { P.fill(hx("#9C6A3C"), 0.04, r); P.rect(0, 0, 16, 2, hx("#BE8A57")); });
  reg("gateCheck", 32, 32, (P, r) => mcCheck(P, r, hx("#EED690"), hx("#DDB662")), { per: 2 });
  reg("houseBoards", 32, 32, (P, r) => planks(P, r, hx("#9A6438"), hx("#B27A4A"), hx("#7C4E2A"), hx("#4E2E16"), 8, 32), { per: 2 });
  reg("beam", 16, 16, (P, r) => { P.fill(hx("#8A5A34"), 0.05, r); P.rect(0, 0, 2, 16, hx("#A8744A")); P.rect(14, 0, 2, 16, hx("#6A4224")); });
  reg("rugBlue", 32, 32, (P, r) => { P.fill(hx("#4A6FA8"), 0.04, r); P.rect(0, 0, 32, 2, hx("#2E4A7A")); P.rect(0, 30, 32, 2, hx("#2E4A7A")); P.rect(0, 0, 2, 32, hx("#2E4A7A")); P.rect(30, 0, 2, 32, hx("#2E4A7A"));
    P.rect(4, 4, 24, 1, hx("#8FB0DE")); P.rect(4, 27, 24, 1, hx("#8FB0DE")); P.rect(4, 4, 1, 24, hx("#8FB0DE")); P.rect(27, 4, 1, 24, hx("#8FB0DE")); }, { fit: true });
  reg("rugSalmon", 32, 32, (P, r) => { P.fill(hx("#F4A08E"), 0.04, r); P.rect(0, 0, 32, 2, hx("#E07A6A")); P.rect(0, 30, 32, 2, hx("#E07A6A")); P.rect(0, 0, 2, 32, hx("#E07A6A")); P.rect(30, 0, 2, 32, hx("#E07A6A"));
    P.rect(4, 4, 24, 1, hx("#FBD0C4")); P.rect(4, 27, 24, 1, hx("#FBD0C4")); P.rect(4, 4, 1, 24, hx("#FBD0C4")); P.rect(27, 4, 1, 24, hx("#FBD0C4")); }, { fit: true });
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
  // (0.55) The lab's floor: pale oak boards laid on the slant, crisp: each board one solid tone, lit along one edge,
  // a clean gap along the other, one end joint each.
  reg("oakDiagonal", 32, 32, P => {
    const base = hx("#F0DDB8"), gap = hx("#C4A77A"), lt = hx("#FAEDD2"), tones = [0.03, -0.03, 0.05, -0.01];
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const d = (x + y) % 32, row = Math.floor(d / 8), along = (x - y + 64) % 32;
      let c = shadec(base, tones[row]);
      if (d % 8 === 0) c = gap; else if (d % 8 === 1) c = mixc(c, lt, 0.7);
      else if (along === (row * 11 + 5) % 32) c = mixc(c, gap, 0.8);
      P.set(x, y, c);
    }
  }, { per: 2 });
  reg("mcDiamondBlue", 32, 32, (P, r) => {
    const a = hx("#F4F8FB"), b = hx("#8EC0E6"), line = hx("#C8DCEA");
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const u = (x + y) % 32, v = (x - y + 32) % 32;
      let c = (u < 16) === (v < 16) ? a : b;
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
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) { const c = cols[(tx * 3 + ty) % 4]; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(c, x === 0 || y === 0 ? -0.14 : x === 1 || y === 1 ? 0.07 : 0)); }
  });
  reg("feltGreen", 16, 16, (P, r) => fabric(P, r, hx("#86B86E"), 0.04));
  reg("lampWhite", 16, 16, (P, r) => P.fill(hx("#F7F5EE"), 0.02, r), { glow: 0.6 });
  reg("cardboard", 16, 16, (P, r) => { P.fill(hx("#C9A06A"), 0.05, r); P.rect(0, 7, 16, 2, hx("#E0C48E")); });
  reg("dvdFront", 16, 8, (P, r) => { P.fill(hx("#2A2D33"), 0.03, r); P.rect(2, 3, 8, 1, hx("#111318")); P.set(13, 3, hx("#58D3A0")); }, { fit: true });
  // Rugs: Mum's salmon one under the table, green and blue kitchen mats, your big red rug, the Center's ring at the door.
  RUGS.salmon = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#E07A6A" : ((i + j) % 2 ? "#F4A08E" : "#F6AE9C");
  RUGS.kitchenGreen = (i, j, w, d) => (i + j) % 2 ? "#3F8A44" : "#7CC56B";
  RUGS.kitchenBlue = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#2E4A7A" : "#4A6FA8";
  // (0.55, 13:12: "maybe like 4 or 5 different shades of red there, but just have them very solid") A ring at a time,
  // each one solid red, deepest at the edge, brightest in the middle; the corners of each ring a shade deeper.
  RUGS.redRoom = (i, j, w, d) => {
    const e = Math.min(i, j, w - 1 - i, d - 1 - j), corner = (i === e || i === w - 1 - e) && (j === e || j === d - 1 - e);
    return e === 0 ? (corner ? "#7C1F2A" : "#8E2530") : e === 1 ? (corner ? "#AE2F39" : "#BC343E") : "#DB464E";
  };
  RUGS.centerRing = (i, j, w, d) => { const t = Math.hypot((i + 0.5 - w / 2) / (w / 2), (j + 0.5 - d / 2) / (d / 2)); return t > 1.05 ? "#F4A866" : (t > 0.66 && t < 0.92) || t < 0.22 ? "#F6F1EA" : "#E8823A"; };
  matReg("matHouseV", "#3E8A6E", "#2A5E4B");
  matReg("matGate", "#3A63B8", "#25418A");
  matReg("matGateN", "#3A63B8", "#25418A", true);
  // Mats for ways through the side walls: their arrows pointing east or west (2 across, 4 along the wall).
  const matSide = (base, edge, east) => (P, r) => {
    const cream = hx("#F6F1EA"), w = P.w, h = P.h, cy = h / 2;
    P.fill(hx(base), 0.04, r);
    P.rect(0, 0, w, 2, hx(edge)); P.rect(0, h - 2, w, 2, hx(edge)); P.rect(0, 0, 2, h, hx(edge)); P.rect(w - 2, 0, 2, h, hx(edge));
    P.rect(4, 4, w - 8, 1, cream); P.rect(4, h - 5, w - 8, 1, cream); P.rect(4, 4, 1, h - 8, cream); P.rect(w - 5, 4, 1, h - 8, cream);
    for (const x0 of [8, 16]) for (let k = 0; k < 7; k++) { const x = east ? x0 + k : w - (x0 + k) - 2; P.rect(x, cy - 8 + k, 2, 2, cream); P.rect(x, cy + 6 - k, 2, 2, cream); }
  };
  reg("matGateEast", 32, 64, matSide("#3A63B8", "#25418A", true), { fit: true });
  reg("matGateWest", 32, 64, matSide("#3A63B8", "#25418A", false), { fit: true });
  reg("seedDish", 8, 8, (P, r) => { P.fill(hx("#B98A4E"), 0.12, r); for (let k = 0; k < 9; k++) P.set(Math.floor(r() * 8), Math.floor(r() * 8), hx("#7A5428")); });
  reg("plateSpeary", 16, 8, (P, r) => { P.fill(hx("#F1E6C8"), 0.02, r); P.rect(0, 0, 16, 1, hx("#8A6A3A")); P.rect(0, 7, 16, 1, hx("#8A6A3A")); P.rect(3, 3, 10, 2, hx("#6A4A2A")); }, { fit: true });
  reg("ballStone", 16, 16, (P, r) => { P.fill(hx("#C4C9CF"), 0.05, r); for (let x = 0; x < 16; x++) { P.set(x, 7, hx("#5E646C")); P.set(x, 8, hx("#5E646C")); } for (let y = 9; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#AEB4BB"), (r() - 0.5) * 0.06)); P.rect(6, 6, 4, 4, hx("#5E646C")); P.rect(7, 7, 2, 2, hx("#E4E7EA")); }, { fit: true });
  reg("ballStoneTop", 16, 16, (P, r) => { P.fill(hx("#C9CED4"), 0.05, r); P.rect(3, 3, 3, 2, hx("#E2E5E8")); });
  reg("bushPink", 16, 16, (P, r) => { P.fill(hx("#4E9A44"), 0.12, r); for (let k = 0; k < 9; k++) { const x = Math.floor(r() * 14), y = Math.floor(r() * 14); P.rect(x, y, 2, 2, hx("#F48FB8")); P.set(x, y, hx("#FFD1E3")); } });
  // Let's Go's League gate: its carpet (deep crimson, a gold line down each side), floor (terracotta red with a gold
  // lattice), walls (red panels), trims (maroon, gold), mats (crimson and gold) and the daylight in its way on.
  RUGS.leagueRed = (i, j, w, d) => (i === 0 || i === w - 1) ? "#C9952F" : (j % 3 === 1 ? "#8E222A" : "#7E1C24");
  reg("leagueFloor", 32, 32, (P, r) => {
    const base = hx("#A8433B"), mid = hx("#B5544A"), gold = hx("#D8A64C");
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      const u = (x + y) % 16, v = (x - y + 32) % 16;
      let c = Math.min(u, 16 - u) + Math.min(v, 16 - v) > 9 ? mid : base;
      if (u === 0 || v === 0) c = gold; else if (u === 1 || v === 1) c = shadec(base, -0.12);
      P.set(x, y, c);
    }
  }, { per: 2 });
  // A table's top in one smooth colour (a painted or laminate top, not planks): warm cream, the faintest grain.
  reg("tableTop", 16, 16, (P, r) => { P.fill(hx("#F3EBDD"), 0.012, r); });
  reg("paperLeague", 16, 16, (P, r) => { P.fill(hx("#B23C3C"), 0.03, r); P.rect(0, 0, 1, 16, hx("#952E30")); P.rect(8, 0, 1, 16, hx("#952E30")); P.rect(1, 0, 1, 16, hx("#C25050")); P.rect(9, 0, 1, 16, hx("#C25050")); });
  reg("leagueMaroon", 16, 16, (P, r) => { P.fill(hx("#7A2228"), 0.04, r); P.rect(0, 0, 16, 1, hx("#8E2E34")); });
  reg("leagueGold", 16, 16, (P, r) => { P.fill(hx("#D6A846"), 0.04, r); P.rect(0, 0, 16, 2, hx("#F2D27A")); P.rect(0, 14, 16, 2, hx("#A97E2A")); });
  reg("doorLight", 16, 32, (P, r) => { for (let y = 0; y < 32; y++) for (let x = 0; x < 16; x++) P.set(x, y, y < 24 ? mixc(hx("#FFFFFF"), hx("#FFF4D8"), y / 23) : mixc(hx("#E8F4C8"), hx("#BFE09A"), (y - 24) / 7)); }, { fit: true, glow: 1 });
  matReg("matLeague", "#8C222A", "#D9AE4E");
  matReg("matLeagueN", "#8C222A", "#D9AE4E", true);
  // A runner along a sideways gate (east to west): blue, a yellow line round it.
  RUGS.gateRunX = (i, j, w, d) => (i === 0 || i === w - 1 || j === 0 || j === d - 1) ? "#E9D27A" : (i % 3 === 1 ? "#5A8EE0" : "#4A7FD4");
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
  const G = room.geo, W = room.W, D = room.Dm || room.D, N = room.Nm || 0, nn = room.northNook, L = room.Lx || 0, Rx = room.Rx == null ? W : room.Rx;
  for (const [a, c] of nn ? [[L, nn[0]], [nn[1], Rx]] : [[L, Rx]]) G.box(a, y, N, c, y + h, N + 0.04, { s: mat, all: null });
  const runs = k => k ? [[N, k[0]], [k[1], D]] : [[N, D]];
  for (const [a, c] of runs(room.westNook)) G.box(L, y, a, L + 0.04, y + h, c, { e: mat, all: null });
  for (const [a, c] of runs(room.eastNook)) G.box(Rx - 0.04, y, a, Rx, y + h, c, { w: mat, all: null });
}
// A hanging basket on a wall bracket (a pot on chains, leaves trailing down). wall "n", "w" or "e".
function hangingPlant(R, x, z, wall = "n", top = 2.9) {
  const G = R.geo, out = 0.55, ax = wall === "w" ? x + out : wall === "e" ? x - out : x, az = wall === "n" ? z + out : z;
  if (wall === "n") G.box(x - 0.04, top - 0.08, z, x + 0.04, top, az + 0.04, "steel");
  else G.box(Math.min(x, ax) - 0.02, top - 0.08, z - 0.04, Math.max(x, ax) + 0.02, top, z + 0.04, "steel");
  G.box(ax - 0.015, top - 0.55, az - 0.015, ax + 0.015, top - 0.08, az + 0.015, "steel");
  G.box(ax - 0.22, top - 0.85, az - 0.22, ax + 0.22, top - 0.55, az + 0.22, { all: "potWhite", top: "soil" });
  G.box(ax - 0.3, top - 0.62, az - 0.3, ax + 0.3, top - 0.42, az + 0.3, "leafSolid");
  G.box(ax - 0.34, top - 1.15, az - 0.03, ax + 0.34, top - 0.6, az + 0.03, "leaf");
  G.box(ax - 0.03, top - 1.15, az - 0.34, ax + 0.03, top - 0.6, az + 0.34, "leaf");
  R.item("Hanging baskets", "Leafy plants in white pots, hung from the walls on little brackets.");
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
  F.box(0, 0, 0, w, 2.5, d, { all: "cupboardWhite" });
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
function roundBall(G, cx, y, cz, s, m = { all: "ball", top: "ballTop" }) {
  const h = s / 2, r = s * 0.2;
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
  G.box(x - p * 1.15, 0.42 * s, z - p * 1.15, x + p * 1.15, 0.5 * s, z + p * 1.15, { all: "potWhite", top: "soil" });
  G.box(x - 0.05, 0.5 * s, z - 0.05, x + 0.05, 1.45 * s, z + 0.05, "walnut");
  // Fronds from John's asset pack (its big fern's) out from the top in a star, a little crown of leaves in the middle.
  for (const [w, d, h, y] of [[0.85, 0.05, 0.6, 1.1], [0.05, 0.85, 0.6, 1.1], [0.6, 0.6, 0.75, 0.95]]) cross(G, x, z, w * s, d * s, h * s, "fernTall", y * s - 0.5);
  leafyBall(G, x, 1.35 * s, z, 0.22 * s, 0.22 * s, "leaf", "leafSolid");
  room.solid(x - p, z - p, x + p, z + p);
  room.act([x - p, z - p, x + p, z + p], { label: "Look", lines: o.lines || ["A palm in a round white pot."] });
  room.item("Palm", "Fronds on a slim trunk, in a round white pot.");
}
function slimPlant(room, x, z, s = 1, o = {}) {
  const G = room.geo, p = 0.17 * s;
  G.box(x - p, 0, z - p, x + p, 0.85 * s, z + p, { all: "potWhite", top: "soil" });
  leafyBall(G, x, 0.85 * s, z, 0.3 * s, 0.5 * s, "leaf", "leafSolid");
  cross(G, x, z, 0.36 * s, 0.36 * s, 0.55 * s, "azaleaPlant", 0.45 * s);
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
  // (15:21) Set into the wall, as the back wall's are: the glass at the back of a deep painted reveal, the cross bars
  // halfway in, a deep sill.
  const G = R.geo, W = R.W, e = wall === "e", t = 0.12, dp = 0.36;
  const X = (a, b) => e ? [W - b, W - a] : [a, b];
  let [a0, a1] = X(0, 0.02);
  G.box(a0, y, z, a1, y + h, z + w, e ? { w: "window", all: null } : { e: "window", all: null });
  [a0, a1] = X(0, dp);
  G.box(a0, y - t, z - t, a1, y, z + w + t, "paint"); G.box(a0, y + h, z - t, a1, y + h + t, z + w + t, "paint");
  G.box(a0, y, z - t, a1, y + h, z, "paint"); G.box(a0, y, z + w, a1, y + h, z + w + t, "paint");
  R.act(e ? [W - 0.6, z, W, z + w] : [0, z, 0.6, z + w], { label: "Look", lines });
  const zc = z + w / 2, yc = y + h / 2;
  [a0, a1] = X(0.16, 0.22);
  G.box(a0, y, zc - 0.05, a1, y + h, zc + 0.05, "paint");
  G.box(a0, yc - 0.05, z, a1, yc + 0.05, z + w, "paint");
  [a0, a1] = X(0, 0.48);
  G.box(a0, y - 0.2 - t, z - 0.15, a1, y - t, z + w + 0.15, "paint");
  R.item("Window", "Set into the wall: the glass at the back of a deep reveal, its light on the floor by the time of day.");
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
  shell(R, { paper: "paperGateLG", floor: "gateCheck", door: LG_GATE_DOOR, entry: true, northNook: LG_GATE_NOOK, skirt: "gateWhite", ledge: "gateBlue", ledgeTop: "gateWhite" });
  const N = R.Nm;
  // The blue lower walls, a white rail over them.
  band(R, 0.24, "gateBlue", 1.1); band(R, 1.34, "gateWhite", 0.1);
  // Windows down both sides.
  for (const z of [2.6 + N, 6.2 + N]) for (const wall of ["w", "e"]) if (!(o.stairs && wall === "e" && z < 4 + N)) crossWindow(R, wall, z, 1.6, 2.0, 1.4, [o.view || "Outside: the trees along ROUTE 2."]);
  // The way on at the north: a nook as deep as the one at the door (4 by 2), dark at its back (the forest's green
  // beyond), its mat its whole width with the arrows pointing out; palms either side.
  const north = o.north || {};
  passageN(R, LG_GATE_NOOK[0], LG_GATE_NOOK[1], north.none ? { none: true, mat: "matGateN", frame: "paint" } : { to: north.to, spawn: north.spawn, mat: "matGateN", frame: "paint", needs: north.needs,
    opening: north.opening || "gateTunnel", tunnel: !north.opening,
    lines: north.lines || (north.needs === "safari" ? ["The way into the SAFARI ZONE. (In the game: once you've paid at the counter.)"] : undefined) });
  if (north.none) R.act([LG_GATE_NOOK[0], 0, LG_GATE_NOOK[1], 1.2], { label: "Look", lines: north.lines });
  palm(R, 3.9, N + 0.8, 1.0); palm(R, 10.1, N + 0.8, 1.0);
  // The runner, from the way on at the north to the door at the south.
  runner(R, 5, N, 9, R.Dm, "gateRunN");
  // Stools round two glass tables on the east side, a few along the west wall; plants; a bin.
  // (0.50) A gate with an upstairs: the stairs up along the north wall on the east side, the tables a little further down.
  const up = o.stairs, S = up ? 1.3 : 0;
  if (up) stairsUp(R, 10, N, 2, { to: up.to, spawn: up.spawn, wall: "gateWhite" });
  glassTable(R, 10.4, 3.0 + N + S, 11.9, 4.4 + N + S, { lines: [o.leaflet || "A glass table. There's a leaflet: VIRIDIAN FOREST, A WALK FOR ALL AGES!"] });
  stool(R, 12.6, 3.7 + N + S); stool(R, 11.15, 2.3 + N + S); stool(R, 9.7, 3.7 + N + S);
  if (!up) {
    glassTable(R, 10.4, 6.2 + N, 11.9, 7.6 + N, { lines: [o.map || "A glass table. Someone's left a map of ROUTE 2 on it."] });
    stool(R, 12.6, 6.9 + N); stool(R, 11.15, 8.3 + N); stool(R, 9.7, 6.9 + N);
  }
  stool(R, 1.0, 6.6 + N); stool(R, 1.0, 7.8 + N);
  slimPlant(R, 0.8, 3.4 + N, 1.0); plant(R, 1.0, R.Dm - 1.0, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); slimPlant(R, 13.2, R.Dm - 1.0, 1.0);
  if (!up) bin(R, 13.3, 1.0 + N);
  const south = o.south || {};
  doormat(R, LG_GATE_DOOR[0], LG_GATE_DOOR[1], south.none ? { none: true, mat: "matGate" } : { to: south.to, spawn: south.spawn || { x: 6.9, z: D - 2.0, yaw: FACE.s }, mat: "matGate",
    needs: south.needs, lines: south.lines });
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
  F.box(0, 0.1, 0, w, 1.0, 0.66, { all: body, back: null, bottom: null });
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
  F.box(0, 0, 0, w, h, 0.62, { all: wood });
  for (const [y0, y1] of [[0.06, h / 2 - 0.02], [h / 2 + 0.02, h - 0.06]]) F.box(0.05, y0, 0.62, w - 0.05, y1, 0.63, { front: drawersOf(wood), all: null });
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
  pouf(R, x, z, Math.min(w, d), 0.1, 0.46, fab);
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
// A ball statue as Let's Go's League gate has them: a grey stone ball on a tall grey pedestal (a darker band round it).
function leagueStatue(R, x, z) {
  const G = R.geo;
  G.box(x - 0.5, 0, z - 0.5, x + 0.5, 0.18, z + 0.5, "silver");
  G.box(x - 0.36, 0.18, z - 0.36, x + 0.36, 1.05, z + 0.36, { all: "silver", top: "steel" });
  G.box(x - 0.38, 0.8, z - 0.38, x + 0.38, 0.88, z + 0.38, "steel");
  G.box(x - 0.44, 1.05, z - 0.44, x + 0.44, 1.12, z + 0.44, "silver");
  roundBall(G, x, 1.12, z, 0.82, { all: "ballStone", top: "ballStoneTop" });
  R.solid(x - 0.5, z - 0.5, x + 0.5, z + 0.5);
  R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Look", lines: ["A statue of a POKé BALL on a stone pedestal.", "The plate reads: TO THE TRAINERS WHO CHALLENGE THE POKéMON LEAGUE."] });
  R.item("Ball statues", "Grey stone balls on tall grey pedestals, down both sides of the carpet.");
}
// A round bush with pink flowers (the League gate's, at the statues' feet): flowering azalea, from John's asset pack.
function flowerBush(R, x, z, s = 1) {
  const G = R.geo, a = 0.42 * s;
  leafyBall(G, x, 0, z, a, 0.62 * s, "flowerLeaf", "flowerLeafSolid");
  R.solid(x - a, z - a, x + a, z + a);
  R.item("Bushes", "Round flowering azalea bushes (John's asset pack's leaves), pink flowers among the green.");
}
// The Center's counter, curving round the nurse (stepped round, as a Minecraft build rounds one): its straight front
// from cx-3 to cx+3, then stepping back to the wall either side.
function curvedCounter(R, cx, o = {}) {
  const G = R.geo, front = o.front || "counterOrange", top = o.top || "lab", h = 1.05;
  const segs = [[-3, 3, 4.4, 5.2], [-3.8, -3, 3.9, 4.9], [3, 3.8, 3.9, 4.9], [-4.4, -3.8, 3.1, 4.3], [3.8, 4.4, 3.1, 4.3], [-4.8, -4.3, 0, 3.1], [4.3, 4.8, 0, 3.1]];
  for (const [a, b, z0, z1] of segs) {
    G.box(cx + a, 0, z0, cx + b, h, z1, { all: front, top: null, bottom: null });
    G.box(cx + a, h, z0, cx + b, h + 0.1, z1, top);
    // (16:17) Metal: a brushed steel kick plate at the foot, a chrome edge under the top.
    G.box(cx + a - 0.01, 0, z0 - 0.01, cx + b + 0.01, 0.12, z1 + 0.01, { all: "brushedSteel", top: null, bottom: null });
    G.box(cx + a - 0.01, h - 0.05, z0 - 0.01, cx + b + 0.01, h, z1 + 0.01, { all: "chromeTrim", top: null, bottom: null });
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


// Diglett's Cave as the mod builds it (tools/caves.py, from John's schematics) and draws it (13:07, as FireRed's): its
// rock olive scales on top and darker olive brown faces, as high as it goes; its floor FireRed's patchy brown (paler
// patches where John has packed mud), the entrances' sand a pale sandy floor; its rocks round boulders; the ladders
// and the hole each goes down. Dark and dusty, as Let's Go's cave: a brown haze in the air.
function caveRoom(id, floor) {
  const C = CAVES[id], R = new Room(id, { name: "Diglett's Cave", floor, W: C.W, D: C.D, H: 8, outside: true, night: true, haze: 0x6e5236 });
  const G = R.geo;
  const at = (x, z) => { const r = C.rows[z]; return { k: r[2 * x], h: r.charCodeAt(2 * x + 1) - 48 }; };
  const cut = southCut(C.W, C.D, (x, z) => at(x, z).k !== "w", () => 0);
  for (let z = 0; z < C.D; z++) {
    for (let x = 0; x < C.W;) {
      const c = at(x, z); let x1 = x + 1;
      while (x1 < C.W && at(x1, z).k === c.k && at(x1, z).h === c.h) x1++;
      if (c.k === "w") { cutRock(G, x, x1, z, -1, Math.min(c.h, 6), cut, { top: "area_cave_rock_top", all: "area_cave_rock", bottom: null }); R.solid(x, z, x1, z + 1); }
      else if (c.k === "r") {
        G.box(x, -1, z, x1, 0, z + 1, { top: "area_cave_floor", all: "soil" });
        // A boulder: a round dome of stone (the mod's own boulder models, BOULDERS), one in each block.
        for (let bx = x; bx < x1; bx++) {
          const P = 1 / 16, b = ((bx * 31 + z * 17 + 2 * 7) % 2 + 2) % 2 === 0, m = BOULDERS[c.h <= 1 ? "boulder_small" : b ? "boulder_a" : "boulder_b"];
          for (const q of m.slice(1)) G.box(bx + q[0] * P, q[1] * P, z + q[2] * P, bx + q[3] * P, q[4] * P, z + q[5] * P, "area_cave_boulder");
        }
        R.solid(x, z, x1, z + 1);
      }
      else if (c.k === "K") G.box(x, -2.2, z, x1, -2, z + 1, { top: "darkTop", all: null });
      else if (c.k === "L") { G.box(x, -2.2, z, x1, -2, z + 1, { top: "darkTop", all: null }); G.box(x, -2, z + 0.02, x1, 0.4, z + 0.08, "walnut"); }
      else G.box(x, -1, z, x1, 0, z + 1, { top: c.k === "S" ? "area_cave_mat" : c.k === "s" ? "area_cave_sand" : "area_cave_floor", all: "soil" });
      x = x1;
    }
  }
  for (const w of C.warps) {
    if (w.kind === "mat") R.warps.push({ test: (p, input) => input.down && p.z > w.z && p.x > w.x0 && p.x < w.x1, to: "town", fx: "fadeOut" });
    if (w.kind === "down") R.warps.push({ test: p => p.x > 11.6 && p.x < 14.4 && p.z > 7.6 && p.z < 10.0, to: w.to, spawn: { x: w.spawn.x, z: w.spawn.z, yaw: FACE.s }, fx: "fade" });
    if (w.kind === "up") R.warps.push({ test: (p, input) => input.up && p.x > w.x0 && p.x < w.x1 && p.z < w.z1 + 0.9, to: w.to, spawn: { x: w.spawn.x, z: w.spawn.z, yaw: FACE.s }, fx: "fade" });
  }
  // (The hole's no wall: in the game you drop down the ladder into it.)
  for (const p of C.people) R.npcs.push({ name: p.name[0] + p.name.slice(1).toLowerCase(), key: p.key, x: p.x, z: p.z, yaw: (180 - p.yaw) * Math.PI / 180, look: { skin: p.skin },
    talk: () => p.lines.map(text => ({ text })) });
  R.spawn = { x: C.spawn.x, z: C.spawn.z, yaw: C.spawn.yaw === 180 ? FACE.n : FACE.s };
  R.item("Diglett's Cave", id === "digletts_b1f" ? "The long tunnel DIGLETT dug, from Route 2 to Route 11 by Vermilion City. DIGLETT and DUGTRIO come up anywhere along it." :
    "A cave mouth's room: the ladder down into the tunnel by the east wall, the way out at the south.");
  return R;
}

// Mt. Moon as the mod builds it (tools/mtmoon.py, from John's schematics, 13:56) and draws it: grey brown rock, a pale
// grey floor with darker patches, its sand, the granite rocks as grey boulders, the holes and ladders between its floors,
// B2F's upper level and its wooden steps; FireRed's trainers, people and items where it has them; the way out with
// daylight beyond. Dim, a faint cool haze.
function moonRoom(id, floor) {
  const C = MTMOON[id], R = new Room(id, { name: "Mt. Moon", floor, W: C.W, D: C.D, H: 9, outside: true, night: true, haze: 0x5c6080 });
  const G = R.geo, P = 1 / 16;
  const at = (x, z) => { const r = C.rows[z]; return { k: r[2 * x], h: r.charCodeAt(2 * x + 1) - 48 }; };
  const FLOOR = { f: "area_moon_floor", s: "area_cave_sand", v: "area_moon_gravel", t: "area_moon_gravel", L: "area_moon_floor", "^": "area_moon_steps", S: "area_cave_mat" };
  const cut = southCut(C.W, C.D, (x, z) => at(x, z).k !== "w", (x, z) => { const c = at(x, z); return c.k === "K" || c.k === "q" || (c.k === "L" && c.h === 0) ? 0 : c.h / 2; });
  for (let z = 0; z < C.D; z++) {
    for (let x = 0; x < C.W;) {
      const c = at(x, z); let x1 = x + 1;
      while (x1 < C.W && at(x1, z).k === c.k && at(x1, z).h === c.h) x1++;
      if (c.k === "w") { cutRock(G, x, x1, z, -1, Math.min(c.h, 7), cut, { top: "area_moon_rock_top", all: "area_moon_rock", bottom: null }); R.solid(x, z, x1, z + 1); }
      else if (c.k === "G" || c.k === "g") {
        const base = c.h / 2;
        G.box(x, -1, z, x1, base, z + 1, { top: "area_moon_floor", all: "soil" });
        for (let bx = x; bx < x1; bx++) {
          const m = BOULDERS[c.k === "g" ? "boulder_small" : ((bx * 31 + z * 17 + 3 * 7) % 2 + 2) % 2 === 0 ? "boulder_a" : "boulder_b"];
          for (const q of m.slice(1)) G.box(bx + q[0] * P, base + q[1] * P, z + q[2] * P, bx + q[3] * P, base + q[4] * P, z + q[5] * P, "area_moon_boulder");
        }
        R.solid(x, z, x1, z + 1);
      }
      else if (c.k === "K") G.box(x, -2.2, z, x1, -2, z + 1, { top: "darkTop", all: null });
      else if (c.k === "q") G.box(x, -1, z, x1, -0.15, z + 1, { top: "water", all: "soil" });
      else if (c.k === "L" && c.h === 0) { G.box(x, -2.2, z, x1, -2, z + 1, { top: "darkTop", all: null }); G.box(x, -2, z + 0.02, x1, 0.4, z + 0.08, "walnut"); }
      else {
        const h = c.h / 2;
        G.box(x, -1, z, x1, h, z + 1, { top: FLOOR[c.k] || "area_moon_floor", all: h > 0.6 ? "area_moon_rock" : "soil" });
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
    const y = it.y || 0;
    G.box(it.x - 0.2, y, it.z - 0.2, it.x + 0.2, y + 0.4, it.z + 0.2, { all: "ball", top: "ballTop" });
    R.act([it.x - 0.6, it.z - 0.6, it.x + 0.6, it.z + 0.6], { label: "Look", lines: [it.key.replace(id + "_", "").replace(/_/g, " ").toUpperCase()] });
  }
  for (const a of C.acts) R.act(a.rect, { label: a.label, lines: a.lines });
  for (const p of C.people) R.npcs.push({ name: p.name.split(" ").map(w => w[0] + w.slice(1).toLowerCase()).join(" "), key: p.key, x: p.x, z: p.z, y: p.y || 0,
    yaw: (180 - p.yaw) * Math.PI / 180, look: { skin: p.skin }, role: p.role || null, talk: () => (p.lines.length ? p.lines : ["(A trainer: walk into their sight and they'll battle you.)"]).map(text => ({ text })) });
  R.spawn = { x: C.spawn.x, z: C.spawn.z, yaw: C.spawn.yaw === 180 ? FACE.n : FACE.s };
  R.item("Mt. Moon", { "mt_moon_1f": "In from the mouth by the POKéMON CENTER on Route 4. Three holes down to B1F; FireRed's trainers, the MOON STONE at the top left.",
    "mt_moon_b1f": "Four parts that don't join: each a ladder up or a hole down. Its mushrooms grow back each day. The north-east part's ladder is the way out, onto Route 4.",
    "mt_moon_b2f": "TEAM ROCKET digging for fossils, SUPER NERD MIGUEL with the DOME and HELIX FOSSILs (beat him, choose one), and the ladder up to the way out." }[id]);
  return R;
}

// ---------- Pewter City (13:18): the Gym as FireRed's laid out, as Let's Go's to look at; the Museum of Science ----------
// The Gym's rock: a blue grey cave wall of big rounded slabs, as Let's Go's; its sand; the blue water-pattern tiles by
// the door; tan, angular rocks; grey stone statues on pedestals with gold plaques.
TEXDEF.gymSand = [16, 16, (P, r) => { P.fill(hx("#D9C08D"), 0.07, r); for (let i = 0; i < 7; i++) P.set(r() * 16, r() * 16, hx("#B99E6C")); }];
TEXDEF.gymRock = [32, 32, (P, r) => {
  P.fill(hx("#5E6F82"), 0.05, r);
  for (const [cx, cy, rad] of [[7, 8, 7.5], [22, 6, 7], [14, 22, 8], [29, 22, 6.5], [2, 26, 5]]) {
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
      for (const ox of [-32, 0, 32]) for (const oy of [-32, 0, 32]) {
        const dx = x - cx - ox, dy = y - cy - oy, d = Math.hypot(dx, dy) / rad;
        if (d < 1) P.set(x, y, shadec(hx("#6A7C90"), -(dx + dy) / rad * 0.12 + (d > 0.86 ? -0.22 : 0) + (d < 0.5 && dx + dy < -2 ? 0.06 : 0) + (r() - 0.5) * 0.04));
      }
    }
  }
  // (13:36) Fine cracks across the slabs, and grit.
  for (const [x0, y0, n] of [[4, 5, 7], [20, 3, 6], [11, 19, 8], [26, 18, 6]]) { let y = y0; for (let x = x0; x < x0 + n; x++) { P.set(x, y, hx("#46525F")); if (r() < 0.4) y += r() < 0.5 ? -1 : 1; } }
  for (let i = 0; i < 30; i++) P.set(r() * 32, r() * 32, shadec(hx("#6A7C90"), r() < 0.5 ? -0.2 : 0.14));
}];
TEXDEF.gymRockDark = [16, 16, (P, r) => P.fill(hx("#46525F"), 0.05, r)];
TEXDEF.gymWater = [16, 16, (P, r) => {
  P.fill(hx("#3B92D6"), 0.03, r);
  for (let x = 0; x < 16; x++) { P.set(x, 4 + Math.round(Math.sin(x * 0.8) * 1.2), hx("#79BDF0")); P.set(x, 11 + Math.round(Math.sin(x * 0.8 + 2) * 1.2), hx("#79BDF0")); }
  P.rect(0, 0, 16, 1, hx("#D6ECFA")); P.rect(0, 0, 1, 16, hx("#D6ECFA"));
}];
// The tan rock (13:36, more detail): layers of sandstone, each a shade of its own with a lit top edge and a dark
// underside, the joins between them wavy; cracks running down through them; grit. Its tops lighter, mottled, cracked.
const tanSide = dark => (P, r) => {
  const tones = ["#C6A262", "#D1B072", "#BA9658", "#CBA867", "#B48F52"].map(c => shadec(hx(c), dark ? -0.16 : 0));
  const edge = [];
  for (let x = 0; x < 32; x++) edge.push([0, 7, 13, 20, 26].map((y, i) => y + Math.round(Math.sin(x * 0.4 + i * 1.7) * 1.1)));
  for (let x = 0; x < 32; x++) for (let y = 0; y < 32; y++) {
    let band = 4; for (let i = 0; i < 5; i++) if (y >= edge[x][i]) band = i;
    const top = edge[x][band], next = band < 4 ? edge[x][band + 1] : 32;
    let c = shadec(tones[band], (r() - 0.5) * 0.08);
    if (y === top) c = shadec(c, 0.16);
    else if (y === next - 1) c = shadec(c, -0.2);
    P.set(x, y, c);
  }
  // Cracks: jagged dark lines down through two or three layers.
  for (const [x0, y0, n] of [[6, 2, 14], [19, 9, 16], [27, 1, 10], [12, 20, 11]]) {
    let x = x0;
    for (let y = y0; y < Math.min(32, y0 + n); y++) { P.set(x, y, shadec(tones[2], -0.42)); P.set(x + 1, y, shadec(tones[0], 0.1), 0.5); if (r() < 0.4) x += r() < 0.5 ? -1 : 1; }
  }
  for (let i = 0; i < 26; i++) P.set(r() * 32, r() * 32, shadec(tones[0], r() < 0.5 ? -0.3 : 0.22));
};
TEXDEF.gymTan = [32, 32, tanSide(false)];
TEXDEF.gymTanLow = [32, 32, tanSide(true)];
TEXDEF.gymTanTop = [32, 32, (P, r) => {
  P.fill(hx("#DDBE82"), 0.05, r);
  for (let i = 0; i < 9; i++) P.disc(r() * 32, r() * 32, 1.5 + r() * 3, shadec(hx("#DDBE82"), (r() - 0.5) * 0.16));
  let x = 4, y = 6; for (let k = 0; k < 22; k++) { P.set(x, y, hx("#A9864C")); x += 1; if (r() < 0.45) y += r() < 0.5 ? -1 : 1; }
  for (let i = 0; i < 20; i++) P.set(r() * 32, r() * 32, shadec(hx("#DDBE82"), r() < 0.5 ? -0.25 : 0.15));
}];
TEXDEF.statueStone = [16, 16, (P, r) => P.fill(hx("#B3B3AE"), 0.08, r)];
TEXDEF.plaqueGold = [16, 16, (P, r) => { P.fill(hx("#CDA746"), 0.04, r); for (const y of [4, 7, 10]) P.rect(3, y, 10, 1, hx("#8C6E22")); P.rect(0, 0, 16, 1, hx("#E8C868")); }];
for (const k of ["gymSand", "gymRock", "gymRockDark", "gymWater", "gymTan", "gymTanLow", "gymTanTop", "statueStone", "plaqueGold"]) MATDEF[k] = { tex: k, per: /^gym(Rock|Tan)/.test(k) && k !== "gymRockDark" ? 2 : 1 };
// The Museum: FireRed's brown tiled floor, cream walls with a tan band low down, cream pillars with gold caps, its
// yellow counter, the fossils in glass, the amber, the space shuttle and the moon stone.
TEXDEF.museumFloor = [16, 16, (P, r) => {
  for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
    const x0 = tx * 8, y0 = ty * 8;
    P.rect(x0, y0, 8, 8, hx("#9B8665")); P.noise(x0 + 2, y0 + 2, 4, 4, hx("#A9946F"), 0.04, r);
    P.rect(x0, y0, 8, 1, hx("#6F5E45")); P.rect(x0, y0, 1, 8, hx("#6F5E45"));
    P.rect(x0 + 1, y0 + 1, 6, 1, hx("#B5A07C")); P.rect(x0 + 1, y0 + 1, 1, 6, hx("#B5A07C"));
  }
}];
TEXDEF.museumWall = [16, 16, (P, r) => { P.fill(hx("#EEE3C4"), 0.02, r); for (let x = 2; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#E5D8B4")); }];
TEXDEF.museumBand = [16, 16, (P, r) => { P.fill(hx("#C7A56C"), 0.03, r); P.rect(0, 0, 16, 2, hx("#D9BC86")); P.rect(0, 14, 16, 2, hx("#A9884F")); }];
TEXDEF.museumPillar = [16, 16, (P, r) => { P.fill(hx("#F5ECD4"), 0.02, r); P.rect(0, 0, 2, 16, hx("#E2D4AE")); P.rect(14, 0, 2, 16, hx("#E2D4AE")); P.rect(7, 0, 2, 16, hx("#FBF6E8")); }];
TEXDEF.museumGold = [16, 16, (P, r) => { P.fill(hx("#D6B04C"), 0.04, r); P.rect(0, 0, 16, 2, hx("#EED27A")); }];
TEXDEF.counterYellow = [16, 16, (P, r) => { P.fill(hx("#E7BE45"), 0.03, r); P.rect(0, 0, 16, 2, hx("#F4D774")); P.rect(0, 13, 16, 3, hx("#C49A2C")); }];
TEXDEF.fossilSlab = [32, 16, (P, r) => {
  P.fill(hx("#5B544A"), 0.06, r);
  const b = hx("#E9DFC2");
  P.line(4, 9, 26, 8, b); P.disc(27, 7, 2.2, b); P.line(28, 7, 30, 9, b);
  for (let x = 8; x < 24; x += 3) { P.line(x, 9, x - 1, 13, b); P.line(x, 8, x + 1, 4, b); }
  P.line(4, 9, 1, 12, b); P.line(14, 8, 18, 2, b); P.line(18, 2, 24, 3, b);
}];
TEXDEF.amberGlow = [16, 16, (P, r) => { P.fill(hx("#E79A2C"), 0.06, r); P.disc(6, 6, 3, hx("#F6C060")); P.disc(9, 10, 1.6, hx("#9A5A16")); }];
TEXDEF.shuttleWhite = [16, 16, (P, r) => { P.fill(hx("#F2F1EC"), 0.02, r); P.rect(0, 7, 16, 1, hx("#CFCFC8")); P.rect(7, 0, 1, 16, hx("#CFCFC8")); }];
TEXDEF.shuttleBlack = [16, 16, (P, r) => P.fill(hx("#2A2B30"), 0.03, r)];
TEXDEF.moonRock = [16, 16, (P, r) => { P.fill(hx("#8D8993"), 0.08, r); for (let i = 0; i < 6; i++) P.disc(r() * 16, r() * 16, 1.2, hx("#6C6873")); }];
for (const k of ["museumFloor", "museumWall", "museumBand", "museumPillar", "museumGold", "counterYellow", "amberGlow", "shuttleWhite", "shuttleBlack", "moonRock"]) MATDEF[k] = { tex: k, per: 1 };
MATDEF.fossilSlab = { tex: "fossilSlab", fit: true };
// The cases' glass (14:15: cleaner): nearly clear, a faint cool tint, one soft streak of light.
TEXDEF.caseGlass = [16, 16, (P, r) => { P.fill(hx("#E8F4F8"), 0.0, r); }];
MATDEF.caseGlass = { tex: "caseGlass", per: 1, alpha: 0.16 };

// The Pewter looks' flowers (its beds' poppies and red tulips).
TEXDEF.pewterPoppy = [16, 16, (P, r) => { P.fill(hx("#D8322E"), 0.06, r); P.disc(8, 8, 2, hx("#2B1A12")); }];
TEXDEF.pewterTulip = [16, 16, (P, r) => { P.fill(hx("#E8503A"), 0.05, r); P.rect(0, 0, 16, 3, hx("#F07A5C")); }];
MATDEF.pewterPoppy = { tex: "pewterPoppy", per: 1 }; MATDEF.pewterTulip = { tex: "pewterTulip", per: 1 };
// A tan rock as Let's Go's Pewter Gym has them: angular, stepped in towards its top.
// A round mound of rock, as the cave's boulders are: at each height a cross of two boxes (its corners cut, round from
// above), each level smaller, so its sides read as a curve.
function rockDome(G, cx, cz, r, h, levels, y0, mats, lean = [0, 0]) {
  for (let i = 0; i < levels; i++) {
    const t0 = i / levels, t1 = (i + 1) / levels;
    const rr = Math.max(0.12, r * Math.sqrt(Math.max(0, 1 - (t0 + 0.5 / levels) ** 2)));
    const cut = rr * 0.42, ox = cx + lean[0] * t0, oz = cz + lean[1] * t0;
    const m = i === 0 ? mats.low : mats.mid;
    G.box(ox - rr, y0 + h * t0, oz - rr + cut, ox + rr, y0 + h * t1, oz + rr - cut, { all: m, top: mats.top });
    G.box(ox - rr + cut, y0 + h * t0, oz - rr, ox + rr - cut, y0 + h * t1, oz + rr, { all: m, top: mats.top });
  }
}
function tanRock(R, x, z, s = 1) {
  // (14:15, better again) A craggy outcrop of tan rock, as Let's Go's: a big rounded mound leaning a little, two or three
  // smaller ones against it, a sharp crag up out of the big one, stones at its foot. Solid over the same ground as before.
  const G = R.geo, r = rng(hashStr("tanRock" + x.toFixed(2) + "," + z.toFixed(2)));
  const mats = { low: "gymTanLow", mid: "gymTan", top: "gymTanTop" };
  const lean = [(r() - 0.5) * 0.3 * s, (r() - 0.5) * 0.3 * s];
  rockDome(G, x - 0.1 * s, z - 0.05 * s, 0.72 * s, 1.35 * s, 7, 0, mats, lean);
  const n = 2 + (r() < 0.5 ? 1 : 0);
  for (let i = 0; i < n; i++) {
    const a = r() * Math.PI * 2, d = 0.5 + r() * 0.2;
    rockDome(G, x + Math.cos(a) * d * s, z + Math.sin(a) * d * 0.85 * s, (0.3 + r() * 0.16) * s, (0.45 + r() * 0.35) * s, 4, 0, mats);
  }
  // The crag: a narrow spire up out of the big mound, off its middle.
  const cx = x + lean[0] * 0.6 + (r() - 0.5) * 0.2 * s, cz = z + lean[1] * 0.6 + (r() - 0.5) * 0.2 * s;
  rockDome(G, cx, cz, 0.24 * s, 0.75 * s, 4, 1.05 * s, mats);
  for (let i = 0; i < 5; i++) {
    const a = r() * Math.PI * 2, d = 0.9 + r() * 0.15, q = (0.06 + r() * 0.07) * s;
    const px = x + Math.cos(a) * 0.8 * s * d, pz = z + Math.sin(a) * 0.7 * s * d;
    G.box(px - q, 0, pz - q * 0.8, px + q, q * 1.1, pz + q * 0.8, { all: "gymTan", top: "gymTanTop" });
  }
  R.solid(x - 0.8 * s, z - 0.7 * s, x + 0.8 * s, z + 0.7 * s);
}
// A stone RHYHORN on a pedestal with a gold plaque (the Gym's statues).
function gymStatue(R, x, z, lines) {
  const G = R.geo;
  G.box(x - 0.6, 0, z - 0.6, x + 0.6, 1.3, z + 0.6, { all: "statueStone", top: "statueStone" });
  G.box(x - 0.35, 0.35, z + 0.6, x + 0.35, 1.0, z + 0.62, { s: "plaqueGold", all: null });
  G.box(x - 0.38, 1.3, z - 0.5, x + 0.38, 1.85, z + 0.35, "statueStone");
  G.box(x - 0.24, 1.45, z + 0.35, x + 0.24, 1.85, z + 0.7, "statueStone");
  G.box(x - 0.06, 1.85, z + 0.5, x + 0.06, 2.1, z + 0.62, "statueStone");
  for (const [dx, dz] of [[-0.3, -0.4], [0.18, -0.4], [-0.3, 0.2], [0.18, 0.2]]) G.box(x + dx, 1.3, z + dz, x + dx + 0.12, 1.45, z + dz + 0.14, "statueStone");
  R.solid(x - 0.6, z - 0.6, x + 0.6, z + 0.6);
  R.act([x - 0.8, z - 0.8, x + 0.8, z + 1.0], { label: "Read", lines });
  R.item("Statues", "Stone RHYHORN on pedestals, a gold plaque on each: the Gym's winners.");
}
// A glass case on a wooden base, what's in it on a slab inside (the Museum's).
function glassCase(R, x0, z0, x1, z1, inside, lines) {
  const G = R.geo;
  G.box(x0, 0, z0, x1, 0.75, z1, { all: "walnut", top: "darkTop" });
  inside(G, x0, z0, x1, z1);
  // (14:15: clean glass, a thin bright steel edge at each corner and along the top, nothing else.)
  G.box(x0, 0.75, z0, x1, 1.55, z0 + 0.02, "caseGlass"); G.box(x0, 0.75, z1 - 0.02, x1, 1.55, z1, "caseGlass");
  G.box(x0, 0.75, z0, x0 + 0.02, 1.55, z1, "caseGlass"); G.box(x1 - 0.02, 0.75, z0, x1, 1.55, z1, "caseGlass");
  for (const [px, pz] of [[x0, z0], [x1 - 0.04, z0], [x0, z1 - 0.04], [x1 - 0.04, z1 - 0.04]]) G.box(px, 0.75, pz, px + 0.04, 1.6, pz + 0.04, "steel");
  G.box(x0, 1.55, z0, x1, 1.58, z1, { all: "steel", top: "caseGlass", bottom: "caseGlass" });
  R.solid(x0, z0, x1, z1);
  R.act([x0 - 0.2, z0 - 0.2, x1 + 0.2, z1 + 0.3], { label: "Look", lines });
}
// A cream pillar against the north wall, gold at its top and foot.
function museumPillar(R, x, H) {
  const G = R.geo;
  G.box(x, 0, 0, x + 0.6, H, 0.35, { all: "museumPillar", top: "cap" });
  G.box(x - 0.05, 0, 0, x + 0.65, 0.25, 0.42, "museumGold"); G.box(x - 0.05, H - 0.3, 0, x + 0.65, H - 0.1, 0.42, "museumGold");
}
// A plain house as Viridian's (the Pewter houses): pale boards, a kitchen, a table on a rug, a TV, a sideboard.
function plainHouse(R, o = {}) {
  shell(R, { paper: o.paper || "paperSchool", floor: o.floor || "midBoards", door: VHOUSE_DOOR, entry: true, skirt: "beam", ledge: "walnut" });
  band(R, 1.0, "beam", 0.12);
  const G = R.geo;
  kitchen(R, 0, "e", { z: 6.6 });
  sideboard(R, 1.5, 0, "s", 1.6, { lines: o.cupboard || ["A cupboard. Inside: plates and a tin of biscuits."] });
  tvUnit(R, 3.5, 3.0, R.screen("tv" + R.id, 40, 24, drawFilm, 0.9), { lines: o.tv || ["A show about the MT. MOON fossils."] });
  clock(R, 8.1, 2.3);
  G.box(3.6, 0, 2.4, 8.4, 0.03, 6.4, { top: o.rug || "rugGreen", all: "cap", bottom: null });
  table(R, 4.8, 3.4, 7.2, 5.4, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A white top in a walnut trim." });
  for (const z of [3.95, 4.85]) { chair(R, 4.15, z, "e", { seat: "cushion" }); chair(R, 7.85, z, "w", { seat: "cushion" }); }
  plant(R, 10.8, 7.7, 0.85, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
  crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, o.window || ["Outside: PEWTER CITY's grey stone paths."]);
  doormat(R, VHOUSE_DOOR[0], VHOUSE_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHouseV" });
  R.spawn = VHOUSE_IN;
}


// ---------- 0.46.0 (15:21 to 15:33): railings that stop you, the Museum's plaque, Fuchsia City and the Safari Zone ----------
// A railing along one side of a staircase or stairwell (x0 to x1 along z), its posts and rail in walnut.
function sideRail(R, x0, x1, z, side) {
  const G = R.geo, t = side === "n" ? -0.14 : 0;
  G.box(x0, 0, z + t, x0 + 0.16, 1.35, z + t + 0.16, "walnut"); G.box(x1 - 0.16, 0, z + t, x1, 1.35, z + t + 0.16, "walnut");
  G.box(x0, 1.0, z + t + 0.02, x1, 1.12, z + t + 0.14, "walnut");
  for (let x = x0 + 0.35; x < x1 - 0.2; x += 0.4) G.box(x - 0.03, 0, z + t + 0.05, x + 0.03, 1.0, z + t + 0.11, "paint");
}
TEXDEF.shuttlePlaque = [64, 8, (P, r) => { P.fill(hx("#C9A548"), 0.03, r); P.rect(0, 0, 64, 1, hx("#E6C86E")); P.rect(0, 7, 64, 1, hx("#8E7230"));
  P.text("SPACE SHUTTLE", 6, 2, hx("#4A3A16")); }];
MATDEF.shuttlePlaque = { tex: "shuttlePlaque", fit: true };
// Fuchsia Gym's wood (Let's Go's: a warm wooden hall), its tatami and its paper screens.
TEXDEF.gymBoards = [16, 16, (P, r) => { P.fill(hx("#9C6A3C"), 0.05, r); for (let y = 0; y < 16; y += 4) { P.rect(0, y, 16, 1, hx("#B98552")); P.rect(0, y + 3, 16, 1, hx("#734B29")); }
  P.rect(5, 0, 1, 4, hx("#734B29")); P.rect(12, 4, 1, 4, hx("#734B29")); P.rect(3, 8, 1, 4, hx("#734B29")); P.rect(10, 12, 1, 4, hx("#734B29")); }];
TEXDEF.gymBoardsDark = [16, 16, (P, r) => { P.fill(hx("#5E3F25"), 0.05, r); for (let y = 0; y < 16; y += 4) P.rect(0, y + 3, 16, 1, hx("#3F2A18")); }];
TEXDEF.tatami = [16, 16, (P, r) => { P.fill(hx("#A9BC62"), 0.04, r); for (let x = 0; x < 16; x += 2) P.rect(x, 0, 1, 16, hx("#9AAD55")); }];
TEXDEF.tatamiEdge = [16, 16, (P, r) => { P.fill(hx("#B0303E"), 0.03, r); for (let x = 0; x < 16; x += 4) P.line(x, 2, x + 3, 13, hx("#E05A68")); }];
TEXDEF.shoji = [16, 16, (P, r) => { P.fill(hx("#F2EAD6"), 0.02, r); P.rect(0, 0, 16, 1, hx("#6B4A2C")); P.rect(0, 8, 16, 1, hx("#6B4A2C")); P.rect(0, 0, 1, 16, hx("#6B4A2C")); P.rect(8, 0, 1, 16, hx("#6B4A2C")); }];
TEXDEF.safariCounter = [16, 16, (P, r) => { P.fill(hx("#5FA24E"), 0.03, r); P.rect(0, 0, 16, 2, hx("#86C46A")); P.rect(0, 13, 16, 3, hx("#3F7836")); }];
TEXDEF.safariSign = [64, 32, (P, r) => { P.fill(hx("#F2EAD6"), 0.02, r); P.rect(0, 0, 64, 3, hx("#5FA24E")); P.rect(0, 29, 64, 3, hx("#5FA24E"));
  P.text("SAFARI ZONE", 11, 8, hx("#3F5C2E")); P.text("POKEMON-U-CATCH", 3, 17, hx("#7A5A2A")); }];
for (const k of ["gymBoards", "gymBoardsDark", "tatami", "tatamiEdge", "shoji", "safariCounter"]) MATDEF[k] = { tex: k, per: 1 };
MATDEF.safariSign = { tex: "safariSign", fit: true };

// A plain house as Fuchsia's (dark wood, its own wall colour): the plain house's rooms with their people.
function fuchsiaHouse(R, o = {}) {
  plainHouse(R, { paper: o.paper || "paperSchool", rug: o.rug || "rugGreen", cupboard: o.cupboard, tv: o.tv,
    window: ["Outside: FUCHSIA CITY's red paths and flower beds."] });
}
// A small rest house in the Safari Zone: a bench, a table, a map on the wall; its mat back out into the park.
function restHouse(R, spawn, lines) {
  shell(R, { paper: "paperSchool", floor: "midBoards", door: VHOUSE_DOOR, entry: true, skirt: "beam", ledge: "walnut" });
  band(R, 1.0, "beam", 0.12);
  table(R, 4.6, 3.4, 7.4, 5.4, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A table, a jug of water and cups on it." });
  for (const z of [3.95, 4.85]) { chair(R, 3.95, z, "e", { seat: "greenFabric" }); chair(R, 8.05, z, "w", { seat: "greenFabric" }); }
  framed(R, 4.4, 1.7, 3.2, 1.3, "safariSign", "n", { act: { label: "Read", lines } });
  crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, ["Outside: the SAFARI ZONE's tall grass, and something moving in it."]);
  plant(R, 1.0, 7.8, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
  doormat(R, VHOUSE_DOOR[0], VHOUSE_DOOR[1], { to: "safari_zone", spawn, mat: "matHouseV" });
  R.spawn = VHOUSE_IN;
}
// The Safari Zone's houses' doors (tools/safari.py): where you come out in front of each, facing away from it.
const SZ_OUT = { safari_rest_center: [177, 179], safari_rest_west: [49, 148], safari_rest_north: [195, 27], safari_rest_east: [319, 135], safari_secret_house: [34, 125] };
const szOut = id => ({ x: SZ_OUT[id][0], z: SZ_OUT[id][1], yaw: FACE.s });

const BUILD = {
  house1() {
    // Downstairs, as Let's Go's: the kitchen down the left-hand wall on its green mat, the dresser of plates, a plant and
    // the TV along the back, the stairs up at the back on the right, the table in the middle on a salmon rug with two
    // green-cushioned chairs each side. Mum at the kitchen counter, as Let's Go has her. The mat in its nook at the door.
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
    table(R, 6.4, 5.0, 8.6, 7.6, { top: "tableTop", side: "tableTop", leg: "paint", name: "Dining table", note: "A smooth cream top in one piece on white legs, plates and a fruit bowl." });
    fruitBowl(R, 7.5, 6.3, 0.87); plates(R, [[6.9, 5.6], [8.1, 5.6], [6.9, 7.0], [8.1, 7.0]], 0.87);
    for (const z of [5.7, 6.9]) { chair(R, 5.75, z, "e", { seat: "greenFabric" }); chair(R, 9.25, z, "w", { seat: "greenFabric" }); }
    R.item("Chairs", "Walnut chairs with green cushions, two each side of the table.");
    crossWindow(R, "e", 4.4, 1.5, 2.0, 1.4, ["Out of the window: Pallet Town's flowers, and the lab's roof past the trees."]);
    framed(R, 7.6, 1.8, 1.2, 0.9, "photos", "e", { act: { label: "Look", lines: ["You, Mum and a very small partner on the beach."] } });
    R.item("Photos", "Framed in walnut on the east wall.");
    plant(R, 1.0, 10.0, 1.0, { kind: "fern", lines: ["A big fern. Its fronds tickle as you go by."] }); plant(R, 13.0, 10.0, 1.0, { kind: "leafy", lines: ["A big-leafed plant. Mum wipes the leaves on Sundays."] });
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHome" });
    R.npcs.push({ name: "Mum", x: 1.55, z: 4.4, yaw: FACE.w,
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
    // (0.55) The side walls: Pallet's beach at sunset and a snapshot by the bed (west); the hills under the stars (east).
    framed(R, 2.2, 1.55, 1.3, 1.0, "picSunset", "w", { frameMat: "honey", act: { label: "Look", lines: ["A painting of the beach south of PALLET TOWN at sunset."] } });
    framed(R, 9.3, 1.7, 0.8, 0.65, "picSnap", "w", { frameMat: "walnut", act: { label: "Look", lines: ["A snapshot of you and MOM on your birthday, in front of the house."] } });
    framed(R, 8.3, 1.55, 1.3, 1.0, "picStars", "e", { frameMat: "honey", act: { label: "Look", lines: ["A picture of the hills north of town, under a sky full of stars."] } });
    R.item("Pictures", "Our own: the beach at sunset, the hills under the stars, a snapshot of you and Mom.");
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
    band(R, 1.05, "orangeTrim"); band(R, 1.31, "brushedSteel", 0.05);
    chamfer(R, 2, { paper: "paperCenter", skirt: "redPlastic", band: [1.05, "orangeTrim"] });
    frontChamfer(R, 2);
    for (const west of [true, false]) { wallPost(R, west, 2.0, 2.5, "postBlueMetal"); wallPost(R, west, R.Dm - 2.5, R.Dm - 2.0, "postBlueMetal"); }
    R.item("Blue posts", "Anodised blue metal posts where the slanted corners meet the walls, front and back.");
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
    band(R, 1.05, "blueTrim"); band(R, 1.31, "brushedSteel", 0.05);
    chamfer(R, 2, { paper: "paperMart", skirt: "blueTrim", band: [1.05, "blueTrim"] });
    const G = R.geo, h = 1.05;
    for (const [x0, x1, z0, z1] of [[0, 3.6, 4.0, 4.6], [2.6, 3.6, 4.6, 8.4], [0, 2.6, 7.9, 8.4], [2.7, 3.5, 8.4, 8.65], [2.9, 3.3, 8.65, 8.8]]) {
      G.box(x0, 0, z0, x1, h, z1, { all: "counterBlue", top: null, bottom: null });
      G.box(x0, h, z0, x1, h + 0.1, z1, "lab");
      G.box(x0 - 0.01, 0, z0 - 0.01, x1 + 0.01, 0.12, z1 + 0.01, { all: "brushedSteel", top: null, bottom: null });
      G.box(x0 - 0.01, h - 0.05, z0 - 0.01, x1 + 0.01, h, z1 + 0.01, { all: "chromeTrim", top: null, bottom: null });
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
    // One shopper at a time, a different one each time you come in (the room's shared by every Mart).
    R.npcs.push({ name: "Youngster", group: "shopper", x: 7.6, z: 1.8, yaw: FACE.n, look: { skin: "youngster" },
      talk: () => [{ text: "No! POTIONs are all sold out." }] });
    R.npcs.push({ name: "Lady", group: "shopper", x: 12.0, z: 5.0, yaw: FACE.e, look: { skin: "middle_aged_woman" },
      talk: () => [{ text: "This shop sells many ANTIDOTEs." }] });
    R.npcs.push({ name: "Hiker", group: "shopper", x: 12.4, z: 2.0, yaw: FACE.n, look: { skin: "hiker" },
      talk: () => [{ text: "ESCAPE ROPEs! You can never have too many when you're off into a cave." }] });
    R.npcs.push({ name: "Lass", group: "shopper", x: 4.4, z: 6.2, yaw: FACE.n, look: { skin: "lass" },
      talk: () => [{ text: "A REPEL keeps weak wild POKéMON away for a while." }, { text: "Handy when you just want to get somewhere!" }] });
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
    bookshelf(R, 9.4, 0, "s", 2.1, 2.6, { wood: "darkWood", drawers: true, lines: ["Books on POKéMON: TYPES, MOVES, and HOW TO CARE FOR YOUR FIRST POKéMON.", "The drawers along the bottom are full of chalk."], name: "Bookcase", note: "Solid dark wood, books on its shelves, drawers along the bottom." });
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
    R.npcs.push({ name: "Teacher", key: "viridian_school_teacher", hours: [7, 18], x: 2.4, z: 1.6, yaw: FACE.s, look: { skin: "school_teacher" },
      talk: () => [{ text: "Welcome to the TRAINER SCHOOL! Shall I ask you a question?" }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_kid", hours: [7, 18], x: 3.25, z: 4.18, yaw: FACE.n, seated: true, look: { skin: "school_kid" },
      talk: () => [{ text: "Whew! I'm trying to learn all my notes by heart." }, { text: "The test is about STATUS problems. Look at the board!" }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_girl", hours: [7, 18], x: 8.75, z: 4.18, yaw: FACE.n, seated: true, look: { skin: "school_kid_f" },
      talk: () => [{ text: "The notebook on the desk up front has TRAINER TIPS in it." }, { text: "Have a read! It's really useful." }] });
    R.npcs.push({ name: "Pupil", key: "viridian_school_kid2", hours: [7, 18], x: 7.65, z: 6.58, yaw: FACE.n, seated: true, look: { skin: "schoolboy" },
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
      F.box(0, 0.1, 0, w, 0.92, 0.66, { all: "walnut" });
      for (let i = 0; i < 2; i++) F.box(i * w / 2 + 0.03, 0.12, 0.66, (i + 1) * w / 2 - 0.03, 0.9, 0.67, { front: "drawersWalnut", all: null });
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
    // SPEARY's perch: a stand on a round foot, a T-bar on top with a knob at each end, a seed dish on a little tray
    // half way up, and its name on a plate.
    G.box(10.8, 0, 6.2, 11.6, 0.06, 7.0, "walnut"); G.box(10.9, 0.06, 6.3, 11.5, 0.1, 6.9, "walnut");
    G.box(11.15, 0.1, 6.55, 11.25, 1.35, 6.65, "walnut");
    G.box(10.6, 1.35, 6.54, 11.8, 1.43, 6.66, "walnut");
    G.box(10.55, 1.31, 6.5, 10.68, 1.47, 6.7, "walnut"); G.box(11.72, 1.31, 6.5, 11.85, 1.47, 6.7, "walnut");
    G.box(11.25, 0.72, 6.48, 11.62, 0.76, 6.72, "walnut"); G.box(11.3, 0.76, 6.5, 11.58, 0.86, 6.7, { all: "redPlastic", top: "seedDish" });
    G.box(10.95, 0.42, 6.66, 11.45, 0.6, 6.68, { s: "plateSpeary", all: null });
    R.solid(10.8, 6.2, 11.6, 7.0);
    R.act([10.4, 6.1, 12, 7.1], { label: "Look", lines: ["SPEARY's perch, a seed dish on its tray and SPEARY's name on a little plate.", "SPEARY's out for a fly."] });
    R.item("Perch", "SPEARY the SPEAROW's perch: a T-bar on a stand, a seed dish half way up, a name plate.");
    // The table in the middle on the green rug: a white top in a wooden trim, a fruit bowl, four cushioned chairs.
    G.box(3.6, 0, 2.4, 8.4, 0.03, 6.4, { top: "rugGreen", all: "cap", bottom: null });
    table(R, 4.8, 3.4, 7.2, 5.4, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A white top in a walnut trim, a bowl of fruit in the middle." });
    G.box(4.74, 0.74, 3.34, 7.26, 0.88, 3.44, "walnut"); G.box(4.74, 0.74, 5.36, 7.26, 0.88, 5.46, "walnut");
    G.box(4.74, 0.74, 3.44, 4.84, 0.88, 5.36, "walnut"); G.box(7.16, 0.74, 3.44, 7.26, 0.88, 5.36, "walnut");
    fruitBowl(R, 6.0, 4.4, 0.86);
    for (const z of [3.95, 4.85]) { chair(R, 4.15, z, "e", { seat: "cushion" }); chair(R, 7.85, z, "w", { seat: "cushion" }); }
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

  gate_league() {
    // ROUTE 22's POKéMON LEAGUE gate, as Let's Go's: a red carpet up the middle from the door to the way on at the
    // north (a nook, as at the door), grey ball statues on pedestals down both sides with pink-flowered bushes
    // between them, palms by the way on, and a guard beside it: only TRAINERS with the BOULDERBADGE may go on to
    // ROUTE 23 and VICTORY ROAD.
    const R = new Room("gate_league", { name: "Pokémon League Gate", floor: "Route 22", W: 14, D: 16, outsideSize: "18 by 21" });
    shell(R, { paper: "paperLeague", floor: "leagueFloor", door: LG_GATE_DOOR, entry: true, northNook: LG_GATE_NOOK, skirt: "leagueGold", ledge: "leagueMaroon", ledgeTop: "leagueGold" });
    const N = R.Nm;
    band(R, 0.24, "leagueMaroon", 1.1); band(R, 1.34, "leagueGold", 0.12); band(R, R.H - 0.75, "leagueGold", 0.1);
    passageN(R, LG_GATE_NOOK[0], LG_GATE_NOOK[1], { to: "town", needs: "BOULDERBADGE", mat: "matLeagueN", frame: "leagueGold", opening: "doorLight",
      lines: ["GUARD: Only TRAINERS with the BOULDERBADGE may go on to VICTORY ROAD.", "GUARD: The rules are the rules!"] });
    // The way on's gold frame, thick, all round the nook's mouth; the daylight in through it, a shaft down to the carpet.
    R.geo.box(LG_GATE_NOOK[0] - 0.3, 0, N - 0.12, LG_GATE_NOOK[0], R.H - 0.9, N + 0.06, "leagueGold");
    R.geo.box(LG_GATE_NOOK[1], 0, N - 0.12, LG_GATE_NOOK[1] + 0.3, R.H - 0.9, N + 0.06, "leagueGold");
    R.geo.box(LG_GATE_NOOK[0] - 0.3, R.H - 1.2, N - 0.12, LG_GATE_NOOK[1] + 0.3, R.H - 0.9, N + 0.06, "leagueGold");
    sunPool(R, LG_GATE_NOOK[0] + 0.1, 0.3, LG_GATE_NOOK[1] - 0.1, 4.6, 0, [0.3, 0.15, R.W - 0.3, R.Dm - 0.3], [0.12, LG_GATE_NOOK[0], LG_GATE_NOOK[1], 0.02, 2.4]);
    palm(R, 3.9, N + 0.8, 1.0); palm(R, 10.1, N + 0.8, 1.0);
    // (0.52, 10:41: "carpets and stuff... the same as the outside") A flat crimson runner, not wool.
    runner(R, 5, N, 9, R.Dm, "leagueRun");
    for (const x of [2.5, 11.5]) for (const z of [N + 2.6, N + 5.8, N + 9.0]) leagueStatue(R, x, z);
    for (const x of [2.5, 11.5]) for (const z of [N + 4.2, N + 7.4]) flowerBush(R, x, z);
    for (const x of [0.9, 13.1]) for (const z of [N + 2.6, N + 5.8, N + 9.0]) flowerBush(R, x, z, 0.8);
    plant(R, 1.0, R.Dm - 1.0, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); plant(R, 13.0, R.Dm - 1.0, 1.0, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    doormat(R, LG_GATE_DOOR[0], LG_GATE_DOOR[1], { to: "town", spawn: { x: 6.9, z: R.D - 2.0, yaw: FACE.s }, mat: "matLeague" });
    R.npcs.push({ name: "Guard", key: "league_gate_guard", x: 10.6, z: N + 1.0, yaw: FACE.s, look: { skin: "guard" },
      talk: () => [{ text: "This is the POKéMON LEAGUE's FRONT GATE." }, { text: "Only TRAINERS who have the BOULDERBADGE may go on to VICTORY ROAD." }] });
    R.spawn = { x: 7.0, z: R.D - 1.5, yaw: FACE.n };
    return R;
  },

  gate_johto() {
    // The gate at the west end of ROUTE 22, sideways (its ways through are in its east and west walls): in from
    // ROUTE 22 at the east; at the west the way on under the mountain, to ROUTE 28 (or, for the CHAMPION, round to
    // ROUTE 26 and JOHTO; the game asks which). A guard at his desk on the north wall, a blue runner between the
    // ways through, glass tables and stools along the front.
    const R = new Room("gate_johto", { name: "Johto Gate", floor: "Route 22", W: 16, D: 10, outsideSize: "9 by 12" });
    shell(R, { paper: "paperGateLG", floor: "gateCheck", skirt: "gateWhite", westNook: [3, 7], eastNook: [3, 7], ledge: "gateBlue", ledgeTop: "gateWhite" });
    band(R, 0.24, "gateBlue", 1.1); band(R, 1.34, "gateWhite", 0.1);
    passageSide(R, "e", 3, 7, { to: "town", mat: "matGateEast", frame: "paint", opening: "gateTunnel", tunnel: true });
    passageSide(R, "w", 3, 7, { to: "town", mat: "matGateWest", frame: "paint", choice: "johto", opening: "gateTunnel", tunnel: true });
    runner(R, 2, 3, 14, 7, "gateRunE");
    // Two windows behind the guard (the light falls across his desk), the map of the way west beside them, a notice.
    crossWindowN(R, 4.3, 1.5, 1.8, 1.4, ["Outside: the mountain's foot, and ROUTE 22's grass."]);
    crossWindowN(R, 9.9, 1.5, 1.8, 1.4, ["Outside: the mountain going up and up into the clouds."]);
    framed(R, 2.4, 1.45, 1.5, 1.1, "map", "n", { act: { label: "Look", lines: ["A map: ROUTE 22 here, ROUTE 28 west under the mountain, ROUTE 26 south to JOHTO."] } });
    framed(R, 12.2, 1.6, 1.3, 0.9, "sign", "n", { act: { label: "Read", lines: ["WEST: ROUTE 28 and MT. SILVER. SOUTH: ROUTE 26 and JOHTO (CHAMPIONS ONLY)."] } });
    hangingPlant(R, 3.6, 0, "n"); hangingPlant(R, 12.4, 0, "n");
    hangingPlant(R, 2, 2.2, "w", 2.8); hangingPlant(R, 2, 7.9, "w", 2.8); hangingPlant(R, 14, 2.2, "e", 2.8); hangingPlant(R, 14, 7.9, "e", 2.8);
    table(R, 6.4, 1.3, 9.6, 2.0, { top: "lab", leg: "steel", h: 0.95, name: "Guard's desk", note: "A white-topped desk on steel legs, a logbook open on it." });
    R.geo.box(7.3, 0.95, 1.45, 8.1, 0.99, 1.85, { all: "linen", top: "bookOpen" });
    R.act([6.2, 1.1, 9.8, 2.3], { label: "Read", lines: ["The guard's logbook: who's gone through, and which way.", "ROUTE 28... ROUTE 28... ROUTE 26 (CHAMPION)..."] });
    slimPlant(R, 2.6, 1.2, 1.0); slimPlant(R, 13.4, 1.2, 1.0);
    glassTable(R, 4.0, 7.6, 5.4, 9.0, { lines: ["A glass table. A leaflet: MT. SILVER, FOR THE STRONGEST ONLY."] });
    stool(R, 3.3, 8.3); stool(R, 6.1, 8.3);
    glassTable(R, 10.6, 7.6, 12.0, 9.0, { lines: ["A glass table. A map of JOHTO, folded up small."] });
    stool(R, 9.9, 8.3); stool(R, 12.7, 8.3);
    palm(R, 2.8, 9.1, 1.0); palm(R, 13.2, 9.1, 1.0);
    plant(R, 7.0, 9.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); plant(R, 9.0, 9.2, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    R.npcs.push({ name: "Guard", key: "johto_gate_guard", x: 8.0, z: 0.8, yaw: FACE.s, look: { skin: "officer_m" },
      talk: () => [{ text: "West of here, under the mountain, is ROUTE 28." }, { text: "ROUTE 26 and the JOHTO region are that way too, but only the CHAMPION may go to JOHTO." }] });
    R.spawn = { x: 14.4, z: 5.0, yaw: FACE.w };
    return R;
  },


  fuchsia_gym() {
    // FUCHSIA GYM, laid out as FireRed's (10 by 14 of its steps, a step a block) and drawn as Let's Go's: a warm wooden
    // hall, paper screens on the walls, KOGA on a raised tatami stage in the middle, two stone statues at the door.
    // FireRed's invisible walls: you can't see them, you find the gaps; KOGA looks close but he's shut in, the way to
    // him from the north. His six JUGGLERs and TAMERs each watch their own way (their sight, as FireRed's).
    const R = new Room("fuchsia_gym", { name: "Fuchsia Gym", floor: "Fuchsia City", W: 12, D: 17, H: 5 });
    shell(R, { paper: "shoji", floor: "gymBoards", door: [5, 7], entry: true, skirt: "gymBoardsDark", ledge: "gymBoardsDark", ledgeTop: "gymBoardsDark" });
    band(R, 0.5, "gymBoardsDark", 0.5);
    const G = R.geo;
    // KOGA's stage: tatami on a low wooden step, its edges red.
    G.box(3.8, 0, 7.4, 8.2, 0.25, 11.2, { all: "gymBoardsDark", top: "tatamiEdge" });
    G.box(4.1, 0.25, 7.7, 7.9, 0.27, 10.9, { top: "tatami", all: null });
    R.ramps.push({ rect: [3.8, 7.4, 8.2, 11.2], h: () => 0.25 });
    // The invisible walls (FireRed's), as thin walls you can't see: KOGA's stage shut in but for a gap at its north; the
    // west side shut off below LIAM's... below PHIL's corner but for a gap by the wall; the east side shut off but for a gap
    // at the far east; a wall down the middle of the north.
    const wall = (x0, z0, x1, z1) => R.solid(x0, z0, x1, z1);
    wall(3.6, 11.2, 8.4, 11.4);           // south of the stage
    wall(3.6, 7.2, 3.8, 11.4);            // its west
    wall(8.2, 7.2, 8.4, 11.4);            // its east
    wall(3.6, 7.2, 6.6, 7.4);             // its north, a gap at 6.6 to 8.2
    wall(0, 8.8, 3.6, 9.0);               // the west: shut off below...
    wall(0, 8.8, 0.0, 9.0);
    wall(8.4, 6.0, 10.8, 6.2);            // the east, a gap at the far east (10.8 to 12)
    wall(6.0, 1.0, 6.2, 5.2);             // down the middle of the north
    wall(1.2, 12.4, 3.0, 12.6); wall(9.0, 12.4, 10.8, 12.6);  // either side of the way in
    R.item("Invisible walls", "FireRed's: walls you can't see. KOGA's stage is shut in but for a gap at its north; find the gaps.");
    gymStatue(R, 4.0, 13.8, ["FUCHSIA CITY POKéMON GYM", "LEADER: KOGA", "WINNING TRAINERS: {rival}"]);
    gymStatue(R, 8.0, 13.8, ["FUCHSIA CITY POKéMON GYM", "LEADER: KOGA", "WINNING TRAINERS: {rival}"]);
    for (const [x, z] of [[0.8, 1.0], [11.2, 1.0], [0.8, 15.0], [11.2, 15.0]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A bonsai in a white pot."] });
    R.npcs.push({ name: "Leader Koga", key: "fuchsia_gym_0", role: "leader", x: 6.0, z: 9.2, y: 0.25, yaw: FACE.s, look: { skin: "leader_koga" },
      talk: () => [{ text: "KOGA: Fwahahaha! A mere child like you dares to challenge me?" }, { text: "(In the game: you battle him on his stage, and win the SOULBADGE.)" }] });
    const T = (name, key, x, z, f, skin, line) => R.npcs.push({ name, key, role: "trainer", x, z, yaw: FACE[f], look: { skin }, talk: () => [{ text: line }] });
    T("Juggler Kayden", "fuchsia_gym_1", 9.3, 10.9, "s", "juggler", "I wanted to become a ninja, so I joined this GYM!");
    T("Juggler Kirk", "fuchsia_gym_2", 2.6, 6.3, "e", "juggler", "Let's see you beat my special techniques!");
    T("Juggler Nate", "fuchsia_gym_3", 10.0, 2.9, "s", "juggler", "Stop right there! Our invisible walls have you frustrated?");
    T("Tamer Phil", "fuchsia_gym_4", 2.6, 10.9, "n", "tamer", "I also study the way of the ninja with master KOGA!");
    T("Tamer Edgar", "fuchsia_gym_5", 4.0, 4.2, "s", "tamer", "Master KOGA comes from a long line of ninjas!");
    T("Juggler Shawn", "fuchsia_gym_6", 8.6, 7.6, "w", "juggler", "Wait! You can't get past the invisible walls without battling me!");
    R.npcs.push({ name: "Gym guide", key: "fuchsia_gym_p0", x: 9.0, z: 15.0, yaw: FACE.w, look: { skin: "gym_guide" },
      talk: () => [{ text: "Yo! Champ in making!" }, { text: "FUCHSIA GYM is riddled with invisible walls! KOGA might appear close, but he's blocked off!" },
        { text: "You have to find gaps in the walls to reach him!" }] });
    doormat(R, 5, 7, { to: "town", mat: "matGate" });
    R.spawn = { x: 6.0, z: 15.5, yaw: FACE.n };
    return R;
  },

  safari_gate() {
    // The SAFARI ZONE's gate (FireRed's entrance building): in from FUCHSIA CITY at the south, through to the park at
    // the north. Its attendant at her green counter: ¥500 for 30 SAFARI BALLs and 600 steps, asked as you go through.
    const R = new Room("safari_gate", { name: "Safari Zone gate", floor: "Fuchsia City", W: 14, D: 14, outsideSize: "14 by 13" });
    lgGate(R, { south: { to: "town" }, north: { to: "safari_zone", spawn: { x: 168.0, z: 182.4, yaw: FACE.n }, needs: "safari" } });
    const G = R.geo, N = R.Nm;
    G.box(9.6, 0, 4.4 + N, 13.4, 1.05, 5.2 + N, { all: "safariCounter", top: "lab" }); R.solid(9.6, 4.4 + N, 13.4, 5.2 + N);
    framed(R, 10.0, 1.7, 3.0, 1.4, "safariSign", "e", { act: { label: "Read", lines: ["SAFARI ZONE", "POKéMON-U-CATCH!", "¥500 for 30 SAFARI BALLs and 600 steps."] } });
    R.npcs.push({ name: "Attendant", key: "safari_zone_gate_p0", x: 11.6, z: 3.6 + N, yaw: FACE.s, look: { skin: "safari_zone_worker" },
      talk: () => [{ text: "Welcome to the SAFARI ZONE!" }, { text: "For just ¥500, you can catch all the POKéMON you want in the park! Would you like to join the hunt?" },
        { text: "(In the game: yes, and you pay ¥500 for 30 SAFARI BALLs. The way north takes you into the park.)" }] });
    R.npcs.push({ name: "Attendant", key: "safari_zone_gate_p1", x: 2.2, z: 4.4 + N, yaw: FACE.e, look: { skin: "safari_zone_worker" },
      talk: () => [{ text: "Hi! Is it your first time here?" }, { text: "SAFARI ZONE has 4 zones in it. Each zone has different kinds of POKéMON. Use SAFARI BALLs to catch them!" },
        { text: "When you run out of time or SAFARI BALLs, it's game over for you!" }] });
    R.spawn = LG_GATE_IN;
    return R;
  },

  fuchsia_office() {
    // The SAFARI ZONE's office (FireRed's meeting room, at the top of FUCHSIA CITY): three of its workers.
    const R = new Room("fuchsia_office", { name: "Safari Zone office", floor: "Fuchsia City", W: 14, D: 11, outsideSize: "16 by 15" });
    shell(R, { paper: "paperSchool", floor: "midBoards", door: [6, 8], entry: true, skirt: "beam", ledge: "walnut" });
    band(R, 1.0, "beam", 0.12);
    table(R, 3.6, 3.6, 10.4, 5.6, { top: "lab", leg: "walnut", h: 0.86, name: "Meeting table", note: "Papers on it: the SAFARI ZONE's new campaign, THE SEARCH FOR THE SECRET HOUSE." });
    for (const x of [4.4, 6.0, 7.6, 9.2]) { chair(R, x + 0.4, 3.05, "s", { seat: "greenFabric" }); chair(R, x + 0.4, 6.15, "n", { seat: "greenFabric" }); }
    framed(R, 5.4, 1.7, 3.2, 1.3, "safariSign", "n", { act: { label: "Read", lines: ["SAFARI ZONE", "Staff only beyond this point."] } });
    plant(R, 1.0, 1.0, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] }); plant(R, 13.0, 1.0, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    R.npcs.push({ name: "Worker", key: "fuchsia_meeting_room_p0", x: 6.0, z: 2.2, yaw: FACE.s, look: { skin: "safari_zone_worker" }, talk: () => [{ text: "We nicknamed the WARDEN SLOWPOKE." }] });
    R.npcs.push({ name: "Worker", key: "fuchsia_meeting_room_p1", x: 2.4, z: 4.6, yaw: FACE.e, look: { skin: "safari_zone_worker" }, talk: () => [{ text: "SLOWPOKE is very knowledgeable about POKéMON!" }] });
    R.npcs.push({ name: "Worker", key: "fuchsia_meeting_room_p2", x: 11.6, z: 4.6, yaw: FACE.w, look: { skin: "safari_zone_worker" }, talk: () => [{ text: "SLOWPOKE came in, but I couldn't understand him." }] });
    doormat(R, 6, 8, { to: "town", mat: "matHouseV" });
    R.spawn = { x: 7.0, z: 9.5, yaw: FACE.n };
    return R;
  },

  fuchsia_house1() {
    // BILL's grandpa's house (FireRed's, by the GYM): BILL's grandpa, a woman and a boy.
    const R = new Room("fuchsia_house1", { name: "Bill's grandpa's house", floor: "Fuchsia City", W: 12, D: 11, outsideSize: "10 by 9" });
    fuchsiaHouse(R, { rug: "rugGreen", cupboard: ["A cupboard. Inside: a jar of sweets and a POKéMON picture book."] });
    R.npcs.push({ name: "Bill's grandpa", key: "fuchsia_bills_grandpas_house_p1", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "gambler" }, talk: () => [{ text: "Hmm? You've met BILL? He's my grandson!" }] });
    R.npcs.push({ name: "Woman", key: "fuchsia_bills_grandpas_house_p0", x: 2.4, z: 3.4, yaw: FACE.e, look: { skin: "middle_aged_woman" }, talk: () => [{ text: "SAFARI ZONE's WARDEN is old, but still active! All his teeth are false, though." }] });
    R.npcs.push({ name: "Youngster", key: "fuchsia_bills_grandpas_house_p2", x: 6.0, z: 7.4, yaw: FACE.n, look: { skin: "youngster" }, talk: () => [{ text: "BILL files his own POKéMON data on his PC!" }] });
    return R;
  },

  fuchsia_house2() {
    // The MOVE DELETER's house (FireRed's, beside the POKéMON CENTER).
    const R = new Room("fuchsia_house2", { name: "Move Deleter's house", floor: "Fuchsia City", W: 12, D: 11, outsideSize: "10 by 9" });
    fuchsiaHouse(R, { rug: "rugSalmon", cupboard: ["A cupboard. Inside: notebooks full of moves, each one crossed out."] });
    R.npcs.push({ name: "Move Deleter", key: "fuchsia_move_deleter_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "middle_aged_man" },
      talk: () => [{ text: "Uh... Oh, yes, I'm the MOVE DELETER. I can make POKéMON forget their moves." }, { text: "(In the game: choose a POKéMON, then the move it should forget.)" }] });
    return R;
  },

  fuchsia_house3() {
    // The SAFARI ZONE WARDEN's home (FireRed's): the WARDEN, who can't speak without his GOLD TEETH (bring them, and
    // he gives you HM04 STRENGTH), and a RARE CANDY behind his boulders (STRENGTH and the SOULBADGE move them).
    const R = new Room("fuchsia_house3", { name: "Safari Zone Warden's home", floor: "Fuchsia City", W: 12, D: 11, outsideSize: "10 by 9" });
    fuchsiaHouse(R, { rug: "rugGreen", cupboard: ["A cupboard. Inside: old photos of the SAFARI ZONE, and a box for false teeth (empty)."] });
    for (const [x, z] of [[1.9, 8.4], [0.8, 7.2]]) { tanRock(R, x, z, 0.8); R.solid(x - 0.5, z - 0.5, x + 0.5, z + 0.5); }
    R.npcs.push({ name: "Warden", key: "wardens_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "warden" },
      talk: () => [{ text: "WARDEN: Hif fuff hefifoo! Ha lof ha feef ee hafahi ho. Heff hee fwee!" }, { text: "(In the game: bring him his GOLD TEETH from the SAFARI ZONE, and he gives you HM04.)" }] });
    R.item("The RARE CANDY", "Behind the boulders in the corner: STRENGTH (HM04, with the SOULBADGE) moves them.");
    return R;
  },

  fuchsia_house4() {
    // The FISHING GURU's older brother's house (FireRed's): the GOOD ROD, if you like to fish.
    const R = new Room("fuchsia_house4", { name: "Fishing Guru's brother's house", floor: "Fuchsia City", W: 12, D: 11, outsideSize: "10 by 9" });
    fuchsiaHouse(R, { rug: "rugBlue", cupboard: ["A cupboard. Inside: fishing line, floats and a tin of bait."] });
    R.npcs.push({ name: "Fishing Guru", key: "fuchsia_good_rod_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "fishing_guru" },
      talk: () => [{ text: "I'm the FISHING GURU's older brother! I simply Looove fishing! Do you like to fish?" }, { text: "(In the game: yes, and he gives you a GOOD ROD.)" }] });
    return R;
  },


  safari_zone() {
    // The SAFARI ZONE as the mod builds it (tools/safari.py, from John's schematic): FireRed's four areas at two blocks a
    // step, West, Center and East side by side and North above, joined by paths through the trees. His trees, grass,
    // ponds and raised ground; the entrance building at the bottom (out through its glass doors, back to the gate); the
    // five little houses; FireRed's items and signs. (The viewer's: what stands where, from the mod's own data.)
    const F = SAFARI;
    const R = new Room("safari_zone", { name: "Safari Zone", floor: "The park", W: F.W, D: F.D, H: 10, outside: true });
    const G = R.geo;
    const at = (x, z) => { const r = F.rows[z]; return { g: r[3 * x], w: r[3 * x + 1], h: r.charCodeAt(3 * x + 2) - 48 }; };
    const GROUND = { g: "grass", m: "grass", p: "path", s: "path", d: "lab", r: "soil", w: "water" };
    for (let z = 0; z < F.D; z++) {
      for (let x = 0; x < F.W;) {
        const g = at(x, z).g; let x1 = x + 1;
        while (x1 < F.W && at(x1, z).g === g) x1++;
        G.box(x, g === "w" ? -1.2 : -1, z, x1, g === "w" ? -0.2 : 0, z + 1, { top: GROUND[g] || "grass", all: "soil" });
        if (g === "w") R.solid(x, z, x1, z + 1);
        x = x1;
      }
      for (let x = 0; x < F.W;) {
        const c = at(x, z);
        if (!"TWURF".includes(c.w) || c.h <= 0) { x++; continue; }
        let x1 = x + 1;
        while (x1 < F.W && at(x1, z).w === c.w && at(x1, z).h === c.h) x1++;
        if (c.w === "W") { G.box(x, 0, z, x1, Math.min(2, c.h), z + 1, "bark"); if (c.h > 2) G.box(x, 2, z, x1, c.h, z + 1, "treeLeaf"); }
        else if (c.w === "T") G.box(x, 0, z, x1, c.h, z + 1, "treeLeaf");
        else if (c.w === "R") G.box(x, 0, z, x1, c.h, z + 1, { top: "grass", all: "area_drip_rock" });
        else if (c.w === "F") G.box(x + 0.35, 0, z + 0.35, x1 - 0.35, c.h, z + 0.65, "bark");
        else G.box(x, 0, z, x1, c.h, z + 1, { top: "area_gate_roof", all: "area_gate_wall_birch" });
        if (c.h >= 1) R.solid(x, z, x1, z + 1);
        x = x1;
      }
      for (let x = 0; x < F.W; x++) if (at(x, z).w === "t") { G.box(x, 0, z + 0.5, x + 1, 1.4, z + 0.5, { s: "tallGrass", all: null }); G.box(x + 0.5, 0, z, x + 0.5, 1.4, z + 1, { e: "tallGrass", all: null }); }
    }
    R.grass = F.rows.length ? [] : [];
    for (const a of F.acts) R.act(a.rect, { label: a.label, lines: a.lines });
    for (const it of F.items) {
      G.box(it.x - 0.2, it.y, it.z - 0.2, it.x + 0.2, it.y + 0.4, it.z + 0.2, { all: "ball", top: "ballTop" });
      R.act([it.x - 0.7, it.z - 0.7, it.x + 0.7, it.z + 0.7], { label: "Pick up", lines: ["You found " + it.key.replace(/^safari_zone_[a-z]+_/, "").toUpperCase().replace(/_/g, " ") + "!"] });
    }
    for (const w of F.warps) {
      if (w.kind === "mat") R.warps.push({ test: (p, input) => input.down && p.z > w.z && p.x > w.x0 && p.x < w.x1, to: w.to, spawn: { x: w.spawn.x, z: w.spawn.z, yaw: FACE.s }, fx: "fade" });
      else R.warps.push({ test: (p, input) => input.up && p.z < w.z && p.z > w.z - 1.5 && p.x > w.x0 && p.x < w.x1, to: w.to, spawn: { x: 6.0, z: 9.5, yaw: FACE.n }, fx: "fade" });
    }
    R.item("The park", "FireRed's SAFARI ZONE: CENTER, EAST (area 1), NORTH (area 2) and WEST (area 3). In the game: ¥500 at the gate, 30 SAFARI BALLs and 600 steps.");
    R.spawn = { x: F.spawn.x, z: F.spawn.z, yaw: FACE.n };
    return R;
  },

  safari_rest_center() {
    const R = new Room("safari_rest_center", { name: "Rest house", floor: "Center area", W: 12, D: 11, outsideSize: "8 by 6" });
    restHouse(R, szOut("safari_rest_center"), ["SAFARI ZONE", "CENTER AREA REST HOUSE"]);
    R.npcs.push({ name: "Sara", key: "safari_zone_center_rest_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "girl" }, talk: () => [{ text: "SARA: Where did my boy friend, ERIK, go?" }] });
    R.npcs.push({ name: "Scientist", key: "safari_zone_center_rest_house_p1", x: 2.4, z: 6.4, yaw: FACE.e, look: { skin: "scientist" }, talk: () => [{ text: "I'm catching POKéMON to take home as gifts!" }] });
    return R;
  },
  safari_rest_east() {
    const R = new Room("safari_rest_east", { name: "Rest house", floor: "East (area 1)", W: 12, D: 11, outsideSize: "8 by 6" });
    restHouse(R, szOut("safari_rest_east"), ["SAFARI ZONE", "AREA 1 REST HOUSE"]);
    R.npcs.push({ name: "Scientist", key: "safari_zone_east_rest_house_p0", x: 2.4, z: 6.4, yaw: FACE.e, look: { skin: "scientist" }, talk: () => [{ text: "How many did you catch? I'm bushed from the work!" }] });
    R.npcs.push({ name: "Rocker", key: "safari_zone_east_rest_house_p1", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "rocker" }, talk: () => [{ text: "I caught a CHANSEY! That makes this all worthwhile!" }] });
    R.npcs.push({ name: "Worker", key: "safari_zone_east_rest_house_p2", x: 9.2, z: 7.4, yaw: FACE.w, look: { skin: "silph_worker_m" }, talk: () => [{ text: "Whew! I'm tired from all the fun!" }] });
    return R;
  },
  safari_rest_north() {
    const R = new Room("safari_rest_north", { name: "Rest house", floor: "North (area 2)", W: 12, D: 11, outsideSize: "8 by 6" });
    restHouse(R, szOut("safari_rest_north"), ["SAFARI ZONE", "AREA 2 REST HOUSE"]);
    R.npcs.push({ name: "Scientist", key: "safari_zone_north_rest_house_p0", x: 2.4, z: 6.4, yaw: FACE.e, look: { skin: "scientist" }, talk: () => [{ text: "You can keep any item you find on the ground here." }] });
    R.npcs.push({ name: "Worker", key: "safari_zone_north_rest_house_p1", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "safari_zone_worker" }, talk: () => [{ text: "Go to the deepest part of the SAFARI ZONE. You will win a prize!" }] });
    R.npcs.push({ name: "Gentleman", key: "safari_zone_north_rest_house_p2", x: 9.2, z: 7.4, yaw: FACE.w, look: { skin: "gentleman" }, talk: () => [{ text: "My EEVEE evolved into FLAREON! But, a friend's EEVEE turned into a VAPOREON! I wonder why?" }] });
    return R;
  },
  safari_rest_west() {
    const R = new Room("safari_rest_west", { name: "Rest house", floor: "West (area 3)", W: 12, D: 11, outsideSize: "8 by 6" });
    restHouse(R, szOut("safari_rest_west"), ["SAFARI ZONE", "AREA 3 REST HOUSE"]);
    R.npcs.push({ name: "Scientist", key: "safari_zone_west_rest_house_p0", x: 2.4, z: 6.4, yaw: FACE.e, look: { skin: "scientist" }, talk: () => [{ text: "Tossing ROCKs at POKéMON might make them run, but they'll be easier to catch." }] });
    R.npcs.push({ name: "Cooltrainer", key: "safari_zone_west_rest_house_p1", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "cooltrainer_m" }, talk: () => [{ text: "Using BAIT will make POKéMON easier to catch." }] });
    R.npcs.push({ name: "Worker", key: "safari_zone_west_rest_house_p2", x: 9.2, z: 7.4, yaw: FACE.w, look: { skin: "silph_worker_f" }, talk: () => [{ text: "I hiked a lot, but I didn't see any POKéMON I wanted." }] });
    return R;
  },
  safari_secret_house() {
    // The SECRET HOUSE (FireRed's, deep in the West area): the first to find it wins HM03 SURF.
    const R = new Room("safari_secret_house", { name: "Secret House", floor: "West (area 3)", W: 12, D: 11, outsideSize: "8 by 6" });
    restHouse(R, szOut("safari_secret_house"), ["SAFARI ZONE", "THE SECRET HOUSE"]);
    R.npcs.push({ name: "Fishing Guru", key: "safari_zone_secret_house_p0", x: 6.0, z: 2.4, yaw: FACE.s, look: { skin: "fishing_guru" },
      talk: () => [{ text: "Ah! Finally! You're the first person to reach the SECRET HOUSE!" }, { text: "Congratulations! You have won! (In the game: HM03, SURF.)" }] });
    return R;
  },

  digletts_n() { return caveRoom("digletts_n", "North entrance (Route 2)"); },
  digletts_b1f() { return caveRoom("digletts_b1f", "B1F"); },
  digletts_s() { return caveRoom("digletts_s", "South entrance (Route 11)"); },
  mt_moon_1f() { return moonRoom("mt_moon_1f", "1F"); },
  mt_moon_b1f() { return moonRoom("mt_moon_b1f", "B1F"); },
  mt_moon_b2f() { return moonRoom("mt_moon_b2f", "B2F"); },

  pewter_gym() {
    // PEWTER GYM, laid out as FireRed's (13 by 15 of its steps, a step a block here) and drawn as Let's Go's: the blue
    // grey rock of a cave all round, sand underfoot, the blue water-pattern tiles at the door, tan angular rocks either
    // side of the way up the middle, a stone RHYHORN on a pedestal each side of the door, and BROCK on his stone stage
    // at the top. CAMPER LIAM waits on the left, looking across the way up (he sees 4 steps); the GYM GUIDE by the door.
    const R = new Room("pewter_gym", { name: "Pewter Gym", floor: "Pewter City", W: 14, D: 18, H: 6 });
    shell(R, { paper: "gymRock", floor: "gymSand", door: [6, 8], entry: true, skirt: "gymRockDark", ledge: "gymRockDark", ledgeTop: "gymRockDark" });
    const G = R.geo, Dm = R.Dm;
    // The water tiles by the door, the way up the middle a shade darker.
    G.box(0, 0, 12, R.W, 0.012, Dm, { top: "gymWater", all: null });
    G.box(5, 0, 2, 9, 0.008, 12, { top: "gymSand", all: null });
    // The rock slabs against the back wall, and BROCK's stage: two steps of stone up to it.
    for (const [x0, x1, h] of [[0, 2.6, 5.2], [2.6, 4.6, 4.2], [9.4, 11.4, 4.4], [11.4, 14, 5.5]]) { G.box(x0, 0, 0, x1, h, 1.2, { all: "gymRock", top: "gymRockDark" }); R.solid(x0, 0, x1, 1.2); }
    G.box(4.0, 0, 0.4, 10.0, 0.25, 4.2, { all: "gymRockDark", top: "statueStone" });
    G.box(4.6, 0.25, 0.4, 9.4, 0.5, 3.2, { all: "gymRockDark", top: "statueStone" });
    R.ramps.push({ rect: [4.0, 0.4, 10.0, 4.2], h: (x, z) => (x > 4.6 && x < 9.4 && z < 3.2 ? 0.5 : 0.25) });
    // The tan rocks either side of the way up (a gap on the left where LIAM stands).
    for (const [x, z, k] of [[1.6, 3.4, 1.1], [3.4, 4.2, 0.9], [1.4, 5.8, 1.0], [1.6, 10.6, 1.0], [3.6, 11.0, 0.8],
      [10.6, 3.6, 1.0], [12.4, 4.6, 1.1], [10.8, 6.6, 0.9], [12.6, 8.2, 1.0], [10.6, 9.6, 1.0], [12.4, 11.0, 0.8]]) tanRock(R, x, z, k);
    gymStatue(R, 3.4, 14.0, ["PEWTER CITY POKéMON GYM", "LEADER: BROCK", "WINNING TRAINERS: {rival}"]);
    gymStatue(R, 10.6, 14.0, ["PEWTER CITY POKéMON GYM", "LEADER: BROCK", "WINNING TRAINERS: {rival}"]);
    R.npcs.push({ name: "Leader Brock", key: "pewter_gym_0", role: "leader", x: 7.0, z: 2.2, y: 0.5, yaw: FACE.s, look: { skin: "leader_brock" },
      talk: () => [{ text: "I'm BROCK! I'm PEWTER's GYM LEADER!" }, { text: "(In the game: you battle him in his rocky arena, and win the BOULDERBADGE.)" }] });
    R.npcs.push({ name: "Camper Liam", key: "pewter_gym_1", role: "trainer", x: 3.0, z: 8.2, yaw: FACE.e, look: { skin: "camper" },
      talk: () => [{ text: "Stop right there, kid! You're still light-years from facing BROCK!" }] });
    R.npcs.push({ name: "Gym guide", key: "pewter_gym_p0", x: 9.4, z: 14.6, yaw: FACE.w, look: { skin: "gym_guide" },
      talk: () => [{ text: "Hiya! I can tell you have what it takes to become a POKéMON champ!" }, { text: "BROCK's ROCK-type POKéMON hate WATER and GRASS moves!" }] });
    doormat(R, 6, 8, { to: "town", mat: "matGate" });
    R.spawn = { x: 7.0, z: 16.5, yaw: FACE.n };
    R.item("The way up", "Sand up the middle, between tan rocks, to BROCK's stone stage at the top. CAMPER LIAM looks across it from the left: walk in front of him and he'll battle you.");
    return R;
  },

  arena_pewter_gym() {
    // PEWTER GYM's battle, as Let's Go's: a round hall of blue grey cave rock, big rounded slabs of it standing all
    // round, sand underfoot, tan angular rocks in clumps at the edge, BROCK's stone stage and its steps at the back, a
    // stone RHYHORN on its pedestal to one side. You battle across the middle, facing the back.
    const R = new Room("arena_pewter_gym", { name: "Pewter Gym battle", floor: "Battle arena", W: 24, D: 24, H: 8 });
    const G = R.geo, c = 12, rad = 11.5;
    for (let j = 0; j < R.D; j++) {
      const zc = j + 0.5 - c; if (Math.abs(zc) >= rad + 1) continue;
      const half = Math.sqrt(Math.max(0, (rad + 1) ** 2 - zc * zc)), x0 = Math.max(0, Math.floor(c - half)), x1 = Math.min(R.W, Math.ceil(c + half));
      G.box(x0, -0.35, j, x1, 0, j + 1, { top: "gymSand", all: null });
    }
    for (let i = 0; i < R.W; i++) for (let j = 0; j < R.D; j++) {
      const d = Math.hypot(i + 0.5 - c, j + 0.5 - c);
      if (d < rad || d >= rad + 1) continue;
      const h = 6.5 + ((i * 7 + j * 13) % 5) * 0.4;
      G.box(i, 0, j, i + 1, h, j + 1, { all: "gymRock", top: "gymRockDark", bottom: null });
      R.solid(i, j, i + 1, j + 1);
    }
    // Rounded slabs standing in front of the wall (not on the camera's side, the south).
    for (let a = -150; a <= -30; a += 15) {
      const t = a * D2R, x = c + Math.cos(t) * 10.2, z = c + Math.sin(t) * 10.2, w = 1.4 + ((a + 150) % 30) / 30, h = 3.4 + ((a + 150) % 45) / 15;
      G.box(x - w / 2, 0, z - 0.5, x + w / 2, h, z + 0.5, { all: "gymRock", top: "gymRockDark" });
    }
    // BROCK's stage at the back, its steps down to the floor.
    G.box(8.5, 0, 1.6, 15.5, 1.2, 4.0, { all: "gymRockDark", top: "statueStone" });
    for (let k = 0; k < 3; k++) G.box(10.2, 0, 4.0 + k * 0.5, 13.8, 1.2 - (k + 1) * 0.4, 4.5 + k * 0.5, { all: "gymRockDark", top: "statueStone" });
    // Tan rocks in clumps at the edge, and a statue.
    for (const [x, z, k] of [[4.2, 6.2, 1.3], [3.4, 8.6, 1.0], [5.6, 4.6, 0.9], [19.8, 6.0, 1.3], [20.6, 8.6, 1.0], [18.4, 4.4, 0.9], [3.0, 12.6, 1.1], [21.0, 12.4, 1.1]]) tanRock(R, x, z, k);
    gymStatue(R, 6.6, 3.0, ["A stone RHYHORN."]);
    G.box(0, R.H, 0, R.W, R.H + 0.2, R.D, { bottom: "gymRockDark", all: null });
    R.spawn = { x: 12, z: 14.2, yaw: FACE.n };
    return R;
  },

  museum_1f() {
    // PEWTER MUSEUM OF SCIENCE, 1F, as FireRed's (its 30 by 10 steps, a step a block): a brown tiled floor; cream walls,
    // a tan band low down, cream pillars between the windows (on the left, not behind the counter, 13:36). In at the main door (the left), past the desk where you
    // pay for your ticket: the two fossils in their glass cases (AERODACTYL and KABUTOPS) and the stairs up. The yellow
    // counter runs down the middle and round to the front: past it, in at the side door (the right), the OLD AMBER on
    // its stand with the scientist who studies it, bookshelves, and two scientists at their desks.
    const R = new Room("museum_1f", { name: "Pewter Museum of Science", floor: "1F", W: 30, D: 11, H: 4 });
    shell(R, { paper: "museumWall", floor: "museumFloor", skirt: "museumBand", skirtH: 0.7 });
    const G = R.geo;
    for (const x of [1.0, 6.2, 11.4, 16.6]) museumPillar(R, x, R.H);
    for (const x of [3.0, 8.2, 13.4]) windowN(R, x, 1.6, 1.8, 1.6);
    // The counter: down from the back, round, and down to the front.
    const counter = (x0, z0, x1, z1) => { G.box(x0, 0, z0, x1, 1.0, z1, { all: "counterYellow", top: "museumGold" }); R.solid(x0, z0, x1, z1); };
    counter(16.6, 1.2, 17.4, 7.4); counter(16.6, 6.6, 19.4, 7.4); counter(18.6, 7.4, 19.4, R.D);
    // The fossils.
    const bones = (G2, x0, z0, x1, z1) => G2.box(x0 + 0.2, 0.75, z0 + 0.25, x1 - 0.2, 0.95, z1 - 0.25, { all: "darkTop", top: "fossilSlab" });
    glassCase(R, 2.8, 2.8, 6.2, 4.6, bones, ["AERODACTYL Fossil", "A primitive and rare POKéMON."]);
    glassCase(R, 2.8, 6.0, 6.2, 7.8, bones, ["KABUTOPS Fossil", "A primitive and rare POKéMON."]);
    stairsUp(R, 8, 7, 2, { to: "museum_2f", spawn: { x: 12.5, z: 9.9, yaw: FACE.s }, wall: "museumWall" });
    R.rail(8.0, 9.0, 12.4, 9.25, 3.2); R.rail(8.0, 6.75, 12.4, 7.0, 3.2);
    // The OLD AMBER on its stand, a glass dome over it.
    G.box(21.9, 0, 2.5, 23.1, 0.9, 3.7, { all: "museumGold", top: "darkTop" });
    G.box(22.25, 0.9, 2.85, 22.75, 1.3, 3.35, "amberGlow");
    G.box(22.05, 0.9, 2.65, 22.95, 1.6, 3.55, "glass");
    R.solid(21.9, 2.5, 23.1, 3.7);
    R.act([21.7, 2.3, 23.3, 4.0], { label: "Look", lines: ["The AMBER is clear and gold!"] });
    // Behind the counter (13:36): two double bookshelves along the back wall, no window or pillar there; two chairs
    // by the receptionist; desks by the east wall.
    for (const x of [19.9, 22.0, 25.2, 27.3]) bookshelf(R, x, 0, "s", 2.1, 2.6, { wood: "walnut" });
    R.act([19.9, 0, 24.1, 1.2], { label: "Read", lines: ["Books about fossils, and about the POKéMON of long ago."] });
    R.act([25.2, 0, 29.4, 1.2], { label: "Read", lines: ["POKéMON JOURNAL", "Special Feature: PEWTER GYM LEADER BROCK!", "BROCK rarely laughs, but they say he can't stop once he starts!"] });
    chair(R, 18.1, 2.3, "w", { seat: "blueFabric" }); chair(R, 18.1, 3.3, "w", { seat: "blueFabric" });
    for (const z of [3.2, 6.4]) { table(R, 28.4, z, 29.7, z + 1.6, { top: "lab", leg: "steel", h: 0.85, name: "Desk", note: "A white desk with papers on fossils." }); }
    for (const [x, z] of [[0.8, 9.6], [12.2, 9.6], [17.6, 9.6], [29.2, 9.6], [15.8, 0.9]]) plant(R, x, z, 0.85, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    doormat(R, 13, 16, { to: "town", mat: "matGate" });
    doormat(R, 21, 24, { to: "town", mat: "matGate" });
    R.npcs.push({ name: "Receptionist", key: "museum_1f_desk", x: 18.4, z: 4.6, yaw: FACE.w, look: { skin: "scientist" },
      talk: () => [{ text: "It's ¥50 for a child's ticket. Would you like to come in?" }, { text: "(In the game: yes, and you pay ¥50 the first time you go in.)" }] });
    R.npcs.push({ name: "Old man", key: "museum_1f_oldman", x: 1.8, z: 3.9, yaw: FACE.e, look: { skin: "old_man" },
      talk: () => [{ text: "That is one magnificent fossil!" }] });
    R.npcs.push({ name: "Scientist", key: "museum_1f_amber", x: 21.4, z: 4.3, yaw: FACE.s, look: { skin: "scientist" },
      talk: () => [{ text: "Ssh! I think that this chunk of AMBER contains POKéMON DNA!" }, { text: "Take it to a POKéMON LAB and get it examined!" }, { text: "(In the game: he gives you the OLD AMBER.)" }] });
    R.npcs.push({ name: "Scientist", key: "museum_1f_sci", x: 26.6, z: 4.4, yaw: FACE.e, look: { skin: "scientist" },
      talk: () => [{ text: "We are proud of two fossils of very rare, prehistoric POKéMON!" }] });
    R.npcs.push({ name: "Scientist", key: "museum_1f_sci2", x: 26.6, z: 7.4, yaw: FACE.e, look: { skin: "scientist_f" },
      talk: () => [{ text: "We found these fossils at MT. MOON. Who knows what else is buried there?" }] });
    R.spawn = { x: 14.5, z: 9.4, yaw: FACE.n };
    R.item("Two ways in", "The main door (left of the counter) to the fossils and the stairs; the side door (right of it) to the OLD AMBER.");
    return R;
  },

  museum_2f() {
    // The Museum's 2F, as FireRed's: the space exhibit. The SPACE SHUTTLE on a gold stand, the meteorite from MT.
    // MOON in two glass cases side by side (as big as 1F's, 13:36), the windows between cream pillars; the stairs down; a scientist, a man looking about, an old
    // man remembering the moon landing, a little girl who wants a PIKACHU and her father.
    const R = new Room("museum_2f", { name: "Pewter Museum of Science", floor: "2F", W: 20, D: 11, H: 4 });
    // (13:56: the floor left open over the stairwell, so you see the stairs going down, not floor over them.)
    shell(R, { paper: "museumWall", floor: "museumFloor", skirt: "museumBand", skirtH: 0.7, floorRects: [[0, 0, 20, 7], [0, 9, 20, 11], [0, 7, 9, 9], [13, 7, 20, 9]] });
    const G = R.geo;
    for (const x of [1.0, 6.2, 11.4, 16.6]) museumPillar(R, x, R.H);
    for (const x of [3.0, 8.2, 13.4]) windowN(R, x, 1.6, 1.8, 1.6);
    stairsDown(R, 9, 7, 2, { to: "museum_1f", spawn: { x: 7.1, z: 8.1, yaw: FACE.s }, paper: "museumWall", show: true });
    // (15:21) Its railing both sides of the opening, not only the front: the back as well.
    sideRail(R, 9, 13, 7, "n");
    R.rail(8.7, 6.7, 13.0, 7.05, 1.6);
    // The shuttle on its stand.
    G.box(13.2, 0, 3.4, 17.8, 0.5, 6.0, { all: "museumGold", top: "darkTop" });
    G.box(13.8, 0.5, 4.3, 17.4, 1.2, 5.1, "shuttleWhite");
    G.box(17.4, 0.6, 4.4, 17.9, 1.1, 5.0, "shuttleBlack");
    G.box(14.2, 0.55, 3.6, 16.4, 0.7, 5.8, "shuttleWhite");
    G.box(13.8, 1.2, 4.55, 14.6, 1.8, 4.85, "shuttleWhite");
    G.box(13.5, 0.6, 4.4, 13.8, 1.1, 5.0, "shuttleBlack");
    R.solid(13.2, 3.4, 17.8, 6.0);
    G.box(14.3, 0.08, 6.0, 16.7, 0.42, 6.03, { s: "shuttlePlaque", all: null });
    R.act([13.0, 3.2, 18.0, 6.3], { label: "Look", lines: ["SPACE SHUTTLE COLUMBIA", "Flew into space in 1981, the first of its kind. A model at one twenty-fifth of its size."] });
    // The meteorite from MT. MOON: in two glass cases side by side, each as big as the fossils' downstairs.
    const rock = (G2, x0, z0, x1, z1) => {
      const cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
      G2.box(x0 + 0.3, 0.75, z0 + 0.3, x1 - 0.3, 0.8, z1 - 0.3, { all: "darkTop", top: "statueStone" });
      G2.box(cx - 0.45, 0.8, cz - 0.32, cx + 0.4, 1.12, cz + 0.3, "moonRock"); G2.box(cx - 0.28, 1.12, cz - 0.2, cx + 0.22, 1.3, cz + 0.16, "moonRock");
    };
    glassCase(R, 1.2, 3.4, 4.6, 5.2, rock, ["Meteorite that fell on MT. MOON.", "(MOON STONE?)"]);
    glassCase(R, 5.0, 3.4, 8.4, 5.2, rock, ["Meteorite that fell on MT. MOON.", "(MOON STONE?)"]);
    // (13:56: more greenery up here, by the pillars and either side of the stairs.)
    for (const [x, z] of [[0.8, 9.6], [19.2, 9.6], [19.2, 1.0], [0.6, 1.0], [5.6, 0.9], [10.8, 0.9], [16.0, 0.9], [8.2, 9.7], [13.7, 9.7]])
      plant(R, x, z, 0.85, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    R.npcs.push({ name: "Scientist", key: "museum_2f_p2", x: 10.5, z: 5.5, yaw: FACE.s, look: { skin: "scientist" }, talk: () => [{ text: "We have a space exhibit now." }] });
    R.npcs.push({ name: "Man", key: "museum_2f_p0", x: 4.5, z: 9.3, yaw: FACE.n, look: { skin: "youngster" }, talk: () => [{ text: "MOON STONE? What's so special about it?" }] });
    R.npcs.push({ name: "Old man", key: "museum_2f_p1", x: 2.4, z: 6.4, yaw: FACE.n, look: { skin: "gramps" }, talk: () => [{ text: "July 20, 1969! The 1st lunar landing!" }] });
    R.npcs.push({ name: "Little girl", key: "museum_2f_p3", x: 15.5, z: 8.5, yaw: FACE.e, look: { skin: "brunette_girl" }, talk: () => [{ text: "I want a PIKACHU! It's so cute!" }] });
    R.npcs.push({ name: "Father", key: "museum_2f_p4", x: 16.5, z: 8.5, yaw: FACE.s, look: { skin: "hiker" }, talk: () => [{ text: "Yeah, a PIKACHU soon, I promise!" }] });
    R.spawn = { x: 12.5, z: 9.9, yaw: FACE.s };
    return R;
  },

  pewter_house1() {
    // The house by the Museum (FireRed's PEWTER house 1): the boy whose NIDORAN won't sit, and his father.
    const R = new Room("pewter_house1", { name: "Pewter house", floor: "By the museum", W: 12, D: 11, outsideSize: "11 by 9" });
    plainHouse(R, { rug: "rugGreen", cupboard: ["A cupboard. Inside: a bag of POKéMON food and a squeaky toy."] });
    R.npcs.push({ name: "Little boy", key: "pewter_nidoran_house_p1", x: 9.2, z: 6.2, yaw: FACE.w, look: { skin: "little_boy" }, talk: () => [{ text: "NIDORAN sit!" }] });
    R.npcs.push({ name: "Man", key: "pewter_nidoran_house_p2", x: 2.4, z: 3.4, yaw: FACE.e, look: { skin: "middle_aged_man" },
      talk: () => [{ text: "Our POKéMON's an outsider, so it's hard to handle." }, { text: "An outsider is a POKéMON that you get in a trade." }] });
    return R;
  },

  pewter_house2() {
    // The house by the Center (FireRed's PEWTER house 2): two people with advice about moves and catching.
    const R = new Room("pewter_house2", { name: "Pewter house", floor: "By the Center", W: 12, D: 11, outsideSize: "11 by 9" });
    plainHouse(R, { rug: "rugGreen" });
    R.npcs.push({ name: "Man", key: "pewter_speech_house_p0", x: 2.4, z: 3.4, yaw: FACE.e, look: { skin: "gambler" },
      talk: () => [{ text: "POKéMON learn new techniques as they grow!" }, { text: "But some moves must be taught by the trainer!" }] });
    R.npcs.push({ name: "Youngster", key: "pewter_speech_house_p1", x: 9.4, z: 6.4, yaw: FACE.w, look: { skin: "youngster" },
      talk: () => [{ text: "POKéMON become easier to catch when they are hurt or asleep!" }, { text: "But it's not a sure thing!" }] });
    return R;
  },

  route2_gate() {
    // The gate across Route 2's east lane (the one past the CUT trees, by Diglett's Cave), the same design as the
    // other route gates: in from the south at its door, out to the north through the way at the north. PROF. OAK's
    // AIDE, who gives HM05 FLASH once you've caught 10 kinds of POKéMON, and a boy.
    const R = new Room("route2_gate", { name: "Route 2 gate", floor: "East lane", W: 14, D: 14, outsideSize: "12 by 10" });
    lgGate(R, { south: { to: "town" }, north: { to: "town" } });
    R.npcs.push({ name: "Aide", key: "route_2_gate_p0", x: 12.6, z: 6.4, yaw: FACE.w, look: { skin: "scientist" },
      talk: () => [{ text: "Hi! Remember me? I'm one of PROF. OAK's AIDES." }, { text: "If you've caught 10 kinds of POKéMON, I'm supposed to give you HM05." },
        { text: "(In the game: he counts your POKéDEX. With 10, it's yours.)" }] });
    R.npcs.push({ name: "Youngster", key: "route_2_gate_p1", x: 3.0, z: 8.2, yaw: FACE.e, look: { skin: "youngster" },
      talk: () => [{ text: "Once a POKéMON learns FLASH, you can get through ROCK TUNNEL." }] });
    R.spawn = LG_GATE_IN;
    return R;
  },

  route2_house() {
    // The house in Route 2's east lane, as Let's Go's houses: pale boards, a kitchen along the left wall, a table on a
    // rug, a TV and a sideboard. The TRADER, who'd give his MR. MIME for an ABRA, and a scientist.
    const R = new Room("route2_house", { name: "Route 2 trade house", floor: "Ground floor", W: 12, D: 11, outsideSize: "11 by 7" });
    shell(R, { paper: "paperSchool", floor: "midBoards", door: VHOUSE_DOOR, entry: true, skirt: "beam", ledge: "walnut" });
    band(R, 1.0, "beam", 0.12);
    const G = R.geo;
    kitchen(R, 0, "e", { z: 6.6 });
    sideboard(R, 1.5, 0, "s", 1.6, { lines: ["A cupboard. Inside: a POKé DOLL and a box of trading cards."] });
    tvUnit(R, 3.5, 3.0, R.screen("tvTradeHouse", 40, 24, drawFilm, 0.9), { lines: ["A show about trading POKéMON with friends far away."] });
    clock(R, 8.1, 2.3);
    G.box(3.6, 0, 2.4, 8.4, 0.03, 6.4, { top: "rugGreen", all: "cap", bottom: null });
    table(R, 4.8, 3.4, 7.2, 5.4, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A white top in a walnut trim." });
    for (const z of [3.95, 4.85]) { chair(R, 4.15, z, "e", { seat: "cushion" }); chair(R, 7.85, z, "w", { seat: "cushion" }); }
    plant(R, 10.8, 7.7, 0.85, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    crossWindow(R, "e", 2.6, 1.6, 1.8, 1.2, ["Outside: the trees along ROUTE 2."]);
    doormat(R, VHOUSE_DOOR[0], VHOUSE_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHouseV" });
    R.npcs.push({ name: "Trader", key: "route_2_trader", x: 9.6, z: 4.4, yaw: FACE.w, look: { skin: "gentleman" },
      talk: () => [{ text: "I'm looking for an ABRA. Will you trade one for my MR. MIME?" }, { text: "(In the game: yes, and you trade.)" }] });
    R.npcs.push({ name: "Scientist", key: "route_2_trade_house_p0", x: 2.4, z: 3.4, yaw: FACE.e, look: { skin: "scientist" },
      talk: () => [{ text: "A fainted POKéMON can't fight. But it can still use moves like CUT!" }] });
    R.spawn = VHOUSE_IN;
    return R;
  },

  gate_s() {
    // Route 2's gate south of Viridian Forest, as Let's Go's gates are (one design for every gate; who's in it is its
    // own): a cream and yellow check, blue lower walls, a long blue runner from the door to the way on at the north,
    // palms by the way on, stools round glass tables, people waiting. The way on at the north goes into VIRIDIAN
    // FOREST, out in front of its south gatehouse.
    const R = new Room("gate_s", { name: "Route 2 gate", floor: "South", W: 14, D: 14, outsideSize: "15 by 12" });
    lgGate(R, { south: { to: "town" }, north: { to: "viridian_forest", spawn: { x: FOREST.fromSouth.x, z: FOREST.fromSouth.z, yaw: FACE.n } } });
    R.npcs.push({ name: "Girl", key: "gate_s_girl", x: 3.0, z: 7.6, yaw: FACE.e, look: { skin: "girl" },
      talk: () => [{ text: "Some POKéMON only live in forests and caves." }, { text: "Look everywhere if you want to find them all!" }] });
    R.npcs.push({ name: "Lady", key: "gate_s_lady", x: 12.6, z: 8.9, yaw: FACE.w, seated: true, look: { skin: "lady" },
      talk: () => [{ text: "VIRIDIAN FOREST is a natural maze." }, { text: "Stick to the path, or you'll be wandering in circles!" }] });
    R.spawn = LG_GATE_IN;
    return R;
  },

  gate_n() {
    // The gate on the forest's north side, the same design: in from Route 2 through the way at the north; the door
    // at the south goes back into VIRIDIAN FOREST, out in front of its north gatehouse.
    const R = new Room("gate_n", { name: "Route 2 gate", floor: "North", W: 14, D: 14, outsideSize: "15 by 16" });
    lgGate(R, { north: { to: "town" }, south: { to: "viridian_forest", spawn: { x: FOREST.fromNorth.x, z: FOREST.fromNorth.z, yaw: FACE.s } } });
    R.npcs.push({ name: "Youngster", key: "gate_n_kid", x: 3.0, z: 9.0, yaw: FACE.e, look: { skin: "youngster" },
      talk: () => [{ text: "Off to PEWTER CITY? Its GYM LEADER, BROCK, uses ROCK-type POKéMON." }, { text: "WATER and GRASS moves work well on ROCK!" }] });
    R.npcs.push({ name: "Hiker", key: "gate_n_hiker", x: 12.6, z: 5.7, yaw: FACE.w, seated: true, look: { skin: "hiker" },
      talk: () => [{ text: "Phew! That forest goes on and on." }, { text: "It's easy to lose your way among all those trees." }] });
    R.spawn = LG_GATE_FROM_N;
    return R;
  },

  viridian_forest() {
    // Viridian Forest as the mod builds it (tools/forest.py, from John's schematic: FireRed's forest laid out at 4
    // blocks to 3 of its steps across and 2 down), always night under its trees. His trees, paths and grass; the two
    // gatehouses (Route 2's gate, outside); the five BUG CATCHERs where FireRed has them, who see that many of its
    // steps straight ahead (the ring on the ground); two people; the item balls; the signs, as HeartGold's. In from
    // Route 2's south gate at the bottom; out through the north gatehouse at the top left.
    const F = FOREST, P16 = 1 / 16;
    const R = new Room("viridian_forest", { name: "Viridian Forest", floor: "Always night", W: F.W, D: F.D, H: 9, outside: true, night: true });
    const G = R.geo;
    const at = (x, z) => { const r = F.rows[z]; return { g: r[3 * x], w: r[3 * x + 1], h: r.charCodeAt(3 * x + 2) - 48 }; };
    const sd = F.southDoor, nd = F.northDoor;
    const doorStep = (x, z) => (x >= sd.x0 && x < sd.x1 && z >= sd.s0 && z <= sd.s1) || (x >= nd.x0 && x < nd.x1 && z >= nd.s0 && z <= nd.s1);
    for (let z = 0; z < F.D; z++) {
      // The ground, in runs along the row: grass (the moss along the paths is drawn as the grass, as the Look pack has
      // it), or a dirt path.
      for (let x = 0; x < F.W;) {
        const g = at(x, z).g; let x1 = x + 1;
        while (x1 < F.W && at(x1, z).g === g) x1++;
        G.box(x, -1, z, x1, 0, z + 1, { top: g === "p" ? "path" : "grass", all: "soil" });
        x = x1;
      }
      // The trees (trunks low down, leaves above) and the gatehouses, in runs of the same height.
      for (let x = 0; x < F.W;) {
        const c = at(x, z);
        if (!"TWU".includes(c.w) || doorStep(x, z)) { x++; continue; }
        let x1 = x + 1;
        while (x1 < F.W && at(x1, z).w === c.w && at(x1, z).h === c.h && !doorStep(x1, z)) x1++;
        if (c.w === "W") { G.box(x, 0, z, x1, 2, z + 1, "bark"); G.box(x, 2, z, x1, c.h, z + 1, "treeLeaf"); }
        else if (c.w === "T") G.box(x, 0, z, x1, c.h, z + 1, "treeLeaf");
        else G.box(x, 0, z, x1, c.h, z + 1, { top: "area_gate_roof", all: "area_gate_wall_birch" });
        R.solid(x, z, x1, z + 1);
        x = x1;
      }
      // Tall grass and flowers, as crossed cut-outs.
      for (let x = 0; x < F.W; x++) {
        const c = at(x, z);
        if (c.w === '"') { G.box(x, 0, z + 0.5, x + 1, 1.6, z + 0.5, { s: "tallGrass", all: null }); G.box(x + 0.5, 0, z, x + 0.5, 1.6, z + 1, { e: "tallGrass", all: null }); }
        if (c.w === "*") { G.box(x + 0.1, 0, z + 0.5, x + 0.9, 0.7, z + 0.5, { s: "flower", all: null }); G.box(x + 0.5, 0, z + 0.1, x + 0.5, 0.7, z + 0.9, { e: "flower", all: null }); }
      }
    }
    // The gatehouses' doors: their steps up, a dark doorway, the roof over them.
    for (const [d, z0, z1, face] of [[sd, sd.s0, sd.s1 + 1, "n"], [nd, nd.s0, nd.s1 + 1, "s"]]) {
      G.box(d.x0, 0, z0, d.x1, 0.5, z1, "area_gate_stone");
      const zf = face === "n" ? z1 : z0;
      G.box(d.x0, 0.5, zf - 0.02, d.x1, 2.8, zf + 0.02, { [face]: "doorDark", all: null });
      G.box(d.x0 - 1, 3, z0, d.x1 + 1, 16, z1, { top: "area_gate_roof", all: "area_gate_wall_birch" });
      if (face === "n") R.solid(d.x0, z1 - 0.3, d.x1, z1); else R.solid(d.x0, z0, d.x1, z0 + 0.3);
    }
    // Out: up the south gatehouse's steps to Route 2's south gate; up the north one's to the north gate.
    R.warps.push({ test: (p, input) => input.down && p.z > sd.z && p.x > sd.x0 && p.x < sd.x1, to: "gate_s", spawn: { x: 7.0, z: 1.4, yaw: FACE.s }, fx: "fade" });
    R.warps.push({ test: (p, input) => input.up && p.z < nd.z && p.x > nd.x0 && p.x < nd.x1, to: "gate_n", spawn: { x: 7.0, z: 12.5, yaw: FACE.n }, fx: "fade" });
    // The signs: two shelves wide, the board over the stand, as HeartGold's.
    for (const [sx, sz, lines] of F.signs) {
      for (const [bx, part] of [[sx, "l"], [sx + 1, "r"]]) {
        const outerW = part === "l", outerE = part === "r";
        G.box(bx, 1, sz + 5 * P16, bx + 1, 2, sz + 11 * P16, { s: "area_sign_front_" + part, n: "area_sign_back_" + part, top: "area_sign_edge_h", bottom: "area_sign_edge_h",
          w: outerW ? "area_sign_edge_v" : null, e: outerE ? "area_sign_edge_v" : null });
        G.box(bx + (outerW ? 2 * P16 : 0), 2 * P16, sz + 6 * P16, bx + 1 - (outerE ? 2 * P16 : 0), 1, sz + 10 * P16, { all: "area_sign_stand", top: null, bottom: null,
          w: outerW ? "area_sign_stand" : null, e: outerE ? "area_sign_stand" : null });
        G.box(bx + (outerW ? P16 : 0), 0, sz + 5 * P16, bx + 1 - (outerE ? P16 : 0), 2 * P16, sz + 11 * P16, { all: "area_sign_foot", w: outerW ? "area_sign_foot" : null, e: outerE ? "area_sign_foot" : null });
      }
      R.solid(sx, sz + 0.3, sx + 2, sz + 0.7);
      R.act([sx - 0.1, sz - 0.4, sx + 2.1, sz + 1.4], { label: "Read", lines });
    }
    // The item balls.
    for (const it of F.items) {
      G.box(it.x - 0.2, 0, it.z - 0.2, it.x + 0.2, 0.4, it.z + 0.2, { all: "ball", top: "ballTop" });
      R.act([it.x - 0.7, it.z - 0.7, it.x + 0.7, it.z + 0.7], { label: "Pick up", lines: ["You found " + it.item + "!", "(In the game it goes in your BAG, once.)"] });
    }
    // The people and the trainers: FireRed's, each where it has them, facing its way. A trainer's sight: as many of
    // FireRed's steps straight ahead as theirs (a step here 4/3 of a block across, 2 down), marked on the ground.
    const SIGHT = { viridian_forest_rick: 5, viridian_forest_doug: 4, viridian_forest_sammy: 4, viridian_forest_anthony: 1, viridian_forest_charlie: 1 };
    const TALK = {
      viridian_forest_rick: ["BUG CATCHER RICK: Hey! You have POKéMON! Come on! Let's battle 'em!"],
      viridian_forest_doug: ["BUG CATCHER DOUG: Yo! You can't jam out if you're a POKéMON TRAINER!"],
      viridian_forest_sammy: ["BUG CATCHER SAMMY: Hey, wait up! What's the hurry?"],
      viridian_forest_anthony: ["BUG CATCHER ANTHONY: I'm only little, but don't you go easy on me!"],
      viridian_forest_charlie: ["BUG CATCHER CHARLIE: Did you know that POKéMON evolve?"],
    };
    for (const p of F.people) {
      const yaw = (180 - p.yaw) * Math.PI / 180, lines = p.role === "trainer" ? TALK[p.key] : p.lines;
      R.npcs.push({ name: p.name.split(" ").map(w => w[0] + w.slice(1).toLowerCase()).join(" "), key: p.key, x: p.x, z: p.z, yaw, look: { skin: p.skin },
        talk: () => lines.map(text => ({ text })) });
      if (p.role === "trainer") {
        const fx = -Math.sin(p.yaw * Math.PI / 180), fz = Math.cos(p.yaw * Math.PI / 180), alongX = Math.abs(fx) > Math.abs(fz);
        const len = SIGHT[p.key] * (alongX ? 4 / 3 : 2), half = (alongX ? 2 : 4 / 3) / 2;
        const ax = p.x + fx * 0.5, az = p.z + fz * 0.5, bx = p.x + fx * (len + 0.5), bz = p.z + fz * (len + 0.5);
        const x0 = Math.min(ax, bx) - (alongX ? 0 : half), x1 = Math.max(ax, bx) + (alongX ? 0 : half), z0 = Math.min(az, bz) - (alongX ? half : 0), z1 = Math.max(az, bz) + (alongX ? half : 0);
        G.box(x0, 0.01, z0, x1, 0.02, z1, { top: "sightMark", all: null });
      }
    }
    R.spawn = { x: F.fromSouth.x, z: F.fromSouth.z, yaw: FACE.n };
    R.item("Viridian Forest", "John's forest (120 by 172 blocks: FireRed's maze, 88 by 140, laid out at 4 blocks to 3 of its steps across and 2 down, with 16 blocks of trees all round). Always night in the game.");
    R.item("BUG CATCHERs", "RICK and DOUG on the east side, SAMMY on the west, ANTHONY at the top right, CHARLIE at the top by the north gatehouse. The faint strip in front of each is how far they see.");
    R.item("Items", "A POKé BALL and two POTIONs up the west side, an ANTIDOTE at the top right, a POTION at the bottom right, an ANTIDOTE by the way in.");
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

  arena_route() {
    // A route's battle (a wild one in the grass, or a trainer on the path), as the mod builds it out of Minecraft's own
    // blocks in Retromon Look (client/BattleScene.arena): a mown pitch in stripes of the grass's two greens, a worn
    // line round it, across its middle and round a circle at its centre; beds of flowers either side and a lantern
    // post at each corner; a clipped azalea hedge all round (open where the camera stands, at the bottom); tall grass
    // beyond; two rings of Kanto's trees. (Here for looking at: the mod's is blocks, so it has the Look's grass and
    // flowers, swaying.) Your POKéMON stands just below the middle line, theirs above it.
    const R = new Room("arena_route", { name: "Route battle", floor: "Battle arena (outside)", W: 40, D: 40, H: 8, outside: true });
    ground(R, "grass");
    const cx = 20, cz = 20;
    const pitch = (dx, dz, i) => { const hx2 = 6 - i, z0 = -8 + i, z1 = 7 - i, r = Math.max(0.5, 2.5 - i); if (Math.abs(dx) > hx2 || dz < z0 || dz > z1) return false;
      const ex = Math.max(0, Math.abs(dx) - (hx2 - r)), ez = Math.max(0, Math.max(z0 + r - dz, dz - (z1 - r))); return Math.hypot(ex, ez) <= r + 0.25; };
    for (let dx = -7; dx <= 7; dx++) for (let dz = -9; dz <= 8; dz++) {
      if (!pitch(dx, dz, 0)) continue;
      const line = !pitch(dx, dz, 1) || dz === -1 || Math.abs(Math.hypot(dx, dz + 1) - 2.6) < 0.55;
      patch(R, cx + dx, cz + dz, cx + dx + 1, cz + dz + 1, line ? "path" : ((dz % 4) + 4) % 4 < 2 ? "grass" : "grassMown");
    }
    for (const side of [-1, 1]) {
      flowers(R, side < 0 ? cx - 9 : cx + 8, cz - 7, side < 0 ? cx - 7 : cx + 10, cz + 6);
      for (const dz of [-10, 6]) { const x = cx + side * 8 + 0.5, z = cz + dz + 0.5; R.geo.box(x - 0.12, 0, z - 0.12, x + 0.12, 2.0, z + 0.12, "pale"); R.geo.box(x - 0.22, 2.0, z - 0.22, x + 0.22, 2.5, z + 0.22, { all: "glass", top: "steel" }); }
    }
    // The hedge, round (block by block), open at the bottom where the camera stands.
    for (let dx = -13; dx <= 13; dx++) for (let dz = -13; dz <= 13; dz++) {
      const d = Math.hypot(dx, dz); if (d < 10.6 || d > 11.7 || Math.hypot(dx + 1.4, dz - 9.8) < 4.5) continue;
      R.geo.box(cx + dx, 0, cz + dz, cx + dx + 1, 2, cz + dz + 1, "treeLeaf");
    }
    for (let i = 0; i < 22; i++) { const a = i / 22 * Math.PI * 2, x = cx + Math.sin(a) * 14.2, z = cz + Math.cos(a) * 14.2; if (Math.hypot(x - cx + 1.4, z - cz - 9.8) > 6.5) tree(R, x, z, 1.3); }
    for (let i = 0; i < 28; i++) { const a = (i + 0.5) / 28 * Math.PI * 2, x = cx + Math.sin(a) * 17.4, z = cz + Math.cos(a) * 17.4; if (Math.hypot(x - cx + 1.4, z - cz - 9.8) > 6.5) tree(R, x, z, 1.6); }
    R.item("Pitch", "13 by 16 blocks: stripes of grass and moss, a dirt path line round it, across the middle and round a circle at the centre.");
    R.item("Round it", "Flower beds and lantern posts either side; an azalea hedge two high (some of it in flower); tall grass; birch and azalea trees in two rings.");
    R.spawn = { x: cx - 0.6, z: cz + 2.2, yaw: FACE.n };
    return R;
  },

  pallet_looks() {
    // The looks the mod gives Pallet Town (client/AreaLooks, tools/lookarea.py), on Minecraft's blocks as John's map
    // has them, from the mod's own textures: the pond's dripstone with the grass's rounded tufts on it and its steps
    // in the water in the dripstone's pattern; a white fence; a shelf sign (two by two) as HeartGold's sign; a
    // mailbox; a little roof of stairs in HeartGold's red tiles. Everything faces you (south).
    const R = new Room("pallet_looks", { name: "Pallet Town looks", floor: "Outside (the mod's blocks)", W: 31, D: 18, H: 6, outside: true });
    const G = R.geo, P16 = 1 / 16;
    // The pond: water in a ring of dripstone (rim x 2 to 10, z 3 to 14), sunk a block into the ground.
    const rx0 = 2, rx1 = 10, rz0 = 3, rz1 = 14;
    const rim = (x, z) => x >= rx0 && x <= rx1 && z >= rz0 && z <= rz1 && (x === rx0 || x === rx1 || z === rz0 || z === rz1);
    const wet = (x, z) => x > rx0 && x < rx1 && z > rz0 && z < rz1;
    const grassAt = (x, z) => x >= 0 && z >= 0 && x < R.W && z < R.D && !rim(x, z) && !wet(x, z);
    G.box(0, -1, 0, rx0, 0, R.D, { top: "grass", all: "soil" });
    G.box(rx1 + 1, -1, 0, R.W, 0, R.D, { top: "grass", all: "soil" });
    G.box(rx0, -1, 0, rx1 + 1, 0, rz0, { top: "grass", all: "soil" });
    G.box(rx0, -1, rz1 + 1, rx1 + 1, 0, R.D, { top: "grass", all: "soil" });
    for (let x = rx0; x <= rx1; x++) for (let z = rz0; z <= rz1; z++) if (rim(x, z)) G.box(x, -1, z, x + 1, 0, z + 1, "area_dripstone");
    G.box(rx0 + 1, -1, rz0 + 1, rx1, -0.125, rz1, { top: "water", all: null });
    // The steps in the water (jungle stairs, in the dripstone's pattern now): their backs against the rim.
    const step = (x, z, back) => {
      G.box(x, -1, z, x + 1, -0.5, z + 1, "area_dripstone");
      if (back === "w") G.box(x, -0.5, z, x + 0.5, 0, z + 1, "area_dripstone");
      if (back === "e") G.box(x + 0.5, -0.5, z, x + 1, 0, z + 1, "area_dripstone");
      if (back === "n") G.box(x, -0.5, z, x + 1, 0, z + 0.5, "area_dripstone");
    };
    step(rx0 + 1, 5, "w"); step(rx0 + 1, 9, "w"); step(rx1 - 1, 7, "e"); step(rx1 - 1, 11, "e"); step(5, rz0 + 1, "n");
    R.solid(rx0, rz0, rx1 + 1, rz1 + 1);
    // The grass's tufts onto the rim (the mod's 17 tiles, chosen as AreaLooks chooses them), their shadow under them.
    for (let x = rx0; x <= rx1; x++) for (let z = rz0; z <= rz1; z++) {
      if (!rim(x, z)) continue;
      const n = grassAt(x, z - 1), so = grassAt(x, z + 1), w = grassAt(x - 1, z), e = grassAt(x + 1, z);
      const nw = grassAt(x - 1, z - 1), ne = grassAt(x + 1, z - 1), sw = grassAt(x - 1, z + 1), se = grassAt(x + 1, z + 1);
      const edges = (n ? 1 : 0) | (e ? 2 : 0) | (so ? 4 : 0) | (w ? 8 : 0);
      const tiles = ({ 1: [16], 2: [8], 4: [2], 8: [10], 3: [11], 6: [4], 12: [5], 9: [12], 5: [16, 2], 10: [8, 10], 14: [6], 13: [7], 7: [13], 11: [14], 15: [9] })[edges] || [];
      if (nw && !n && !w) tiles.push(17);
      if (ne && !n && !e) tiles.push(15);
      if (sw && !so && !w) tiles.push(3);
      if (se && !so && !e) tiles.push(1);
      for (const t of tiles) G.box(x, 0.002, z, x + 1, 0.002, z + 1, { top: "area_drip_shade_" + t, all: null });
      for (const t of tiles) G.box(x, 0.004, z, x + 1, 0.004, z + 1, { top: "area_grass_on_drip_" + t, all: null });
    }
    // The white fence (pale oak fence): posts a block apart, two rails between them.
    const fz = 9.5;
    for (let x = 12; x <= 22; x++) {
      G.box(x + 6 * P16, 0, fz - 2 * P16, x + 10 * P16, 1, fz + 2 * P16, "area_white_fence");
      if (x < 22) for (const [y0, y1] of [[6, 9], [12, 15]]) G.box(x + 10 * P16, y0 * P16, fz - P16, x + 22 * P16, y1 * P16, fz + P16, "area_white_fence");
    }
    R.solid(12, fz - 0.2, 23, fz + 0.2);
    // The sign: four pale oak shelves (two by two) facing south. The top two are the board, the bottom two the stand.
    const sx = 13, sz = 12;
    for (const [bx, part] of [[sx, "l"], [sx + 1, "r"]]) {
      const outerW = part === "l", outerE = part === "r";
      G.box(bx, 1, sz + 5 * P16, bx + 1, 2, sz + 11 * P16, { s: "area_sign_front_" + part, n: "area_sign_back_" + part, top: "area_sign_edge_h", bottom: "area_sign_edge_h",
        w: outerW ? "area_sign_edge_v" : null, e: outerE ? "area_sign_edge_v" : null });
      G.box(bx + (outerW ? 2 * P16 : 0), 2 * P16, sz + 6 * P16, bx + 1 - (outerE ? 2 * P16 : 0), 1, sz + 10 * P16, { all: "area_sign_stand", top: null, bottom: null,
        w: outerW ? "area_sign_stand" : null, e: outerE ? "area_sign_stand" : null });
      G.box(bx + (outerW ? P16 : 0), 0, sz + 5 * P16, bx + 1 - (outerE ? P16 : 0), 2 * P16, sz + 11 * P16, { all: "area_sign_foot",
        w: outerW ? "area_sign_foot" : null, e: outerE ? "area_sign_foot" : null });
    }
    R.solid(sx, sz + 0.3, sx + 2, sz + 0.7);
    R.act([sx, sz, sx + 2, sz + 1], { label: "Read", lines: ["PALLET TOWN", "Shades of your journey await!"] });
    // The mailbox (the mod's own block), its slot towards you: a red box on a silver post.
    const mx = 17, mz = 12;
    G.box(mx + 6 * P16, 0, mz + 6 * P16, mx + 10 * P16, 0.5 * P16, mz + 10 * P16, { all: "area_mailbox_plate", bottom: null });
    G.box(mx + 7 * P16, 0.5 * P16, mz + 7 * P16, mx + 9 * P16, 8 * P16, mz + 9 * P16, { all: "area_mailbox_post", top: null, bottom: null });
    G.box(mx + 4.5 * P16, 8 * P16, mz + 2 * P16, mx + 11.5 * P16, 14 * P16, mz + 14 * P16, { all: "area_mailbox_side", s: "area_mailbox_front", top: "area_mailbox_top", bottom: "area_mailbox_under" });
    G.box(mx + 5.5 * P16, 14 * P16, mz + 2 * P16, mx + 10.5 * P16, 15.5 * P16, mz + 14 * P16, { all: "area_mailbox_top", bottom: null });
    R.solid(mx + 0.25, mz + 0.1, mx + 0.75, mz + 0.9);
    R.act([mx, mz, mx + 1, mz + 1], { label: "Look", lines: ["A mailbox: a red box on a silver post, as HeartGold's by each house."] });
    // A little roof of acacia stairs (its ridge running north to south), in HeartGold's red tiles: the rows of tiles
    // along the eaves, the trim at its ends, white wall under it.
    const z0 = 2, z1 = 6, base = 13;
    for (let i = 0; i < 3; i++) {
      for (const side of ["w", "e"]) {
        const x = side === "w" ? base + i : base + 5 - i, y = i;
        const top = side === "w" ? "area_roof_top_w" : "area_roof_top_e";
        G.box(x, y, z0, x + 1, y + 0.5, z1, { top, n: "area_roof_edge", s: "area_roof_edge", w: "area_roof_tiles", e: "area_roof_tiles", bottom: "area_roof_under" });
        const hx0 = side === "w" ? x + 0.5 : x, hx1 = side === "w" ? x + 1 : x + 0.5;
        G.box(hx0, y + 0.5, z0, hx1, y + 1, z1, { top, n: "area_roof_edge", s: "area_roof_edge", w: "area_roof_tiles", e: "area_roof_tiles", bottom: null });
        if (y > 0) G.box(x, 0, z0 + 0.5, x + 1, y, z1 - 0.5, "siding");
      }
    }
    R.solid(base, z0, base + 6, z1);
    // A corner of Prof. Oak's lab: its base of solid stone, its pale yellow brick walls, a light brown pillar at the
    // corner, a window with a red metal awning, the door's teal-green awning, the band under the roof, the painted
    // brick chimney.
    const lx = 25, lz = 3;
    G.box(lx, 0, lz, lx + 5, 1, lz + 1, "area_lab_stone");
    G.box(lx, 1, lz, lx + 5, 4, lz + 1, "area_lab_brick");
    G.box(lx - 1, 0, lz + 0.0, lx, 1, lz + 1, "area_lab_pillar"); G.box(lx - 1 + 0.15, 1, lz + 0.15, lx - 0.15, 2, lz + 0.85, "area_lab_pillar");
    G.box(lx - 1, 2, lz, lx, 3, lz + 1, "area_lab_pillar"); G.box(lx - 1 + 0.15, 3, lz + 0.15, lx - 0.15, 4, lz + 0.85, "area_lab_pillar");
    G.box(lx + 0.5, 1.6, lz + 1, lx + 2.5, 2.6, lz + 1.02, "window");
    G.box(lx + 0.3, 2.75, lz + 1, lx + 2.7, 3.0, lz + 1.5, "area_lab_awning_red");
    G.box(lx + 3.2, 0, lz + 1, lx + 4.6, 2.2, lz + 1.02, "glass");
    G.box(lx + 3.0, 2.3, lz + 1, lx + 4.8, 2.55, lz + 1.5, "area_lab_awning_door");
    G.box(lx + 3.3, 0, lz + 1, lx + 4.5, 0.5, lz + 1.5, "area_lab_stone");
    G.box(lx - 1, 4, lz - 0.5, lx + 5, 4.5, lz + 1.5, "area_lab_trim");
    G.box(lx + 3, 4.5, lz, lx + 4, 6, lz + 1, "area_lab_chimney");
    R.solid(lx - 1, lz, lx + 5, lz + 1.5);
    // A corner of your house: its pillar and steps solid light grey, the dark band at its underside, the trim across
    // its front, the drainpipe in metal.
    const hx = 24, hz = 12;
    G.box(hx, 0, hz, hx + 6, 3, hz + 1, "siding");
    G.box(hx, 0, hz, hx + 1, 3, hz + 1.01, "area_house_stone");
    G.box(hx + 1, 0.5, hz + 1, hx + 6, 1, hz + 1.2, "area_house_dark");
    G.box(hx + 1, 2.5, hz + 1, hx + 6, 3, hz + 1.5, "area_house_trim");
    G.box(hx + 2, 0, hz + 1, hx + 4, 0.5, hz + 2, "area_house_stone");
    G.box(hx + 6 + 4 * P16, 0, hz + 1 + 4 * P16, hx + 6 + 12 * P16, 2.5, hz + 1 + 12 * P16, "area_drainpipe");
    R.solid(hx, hz, hx + 7, hz + 2);
    R.item("Lab", "Prof. Oak's lab as HeartGold's: solid stone base and steps, clean pale yellow brick, light brown pillars, metal awnings (red over the windows, teal-green over the door), a lighter band under the roof, a painted brick chimney. The roof as it is.");
    R.item("Houses", "Your house and Blue's: pillars, steps and the dark band at the underside in solid colours, the trim across the front off-white, the drainpipe metal.");
    R.item("Pond", "Dripstone round the water; the grass's rounded tufts onto the dripstone where they meet (as round the dirt paths); the steps in the water in the dripstone's own pattern.");
    R.item("Fence", "Pale oak fence, solid white with a slight gradient (Pallet Town and Route 1).");
    R.item("Sign", "Four pale oak shelves: the top two HeartGold's white board with its grey panel and writing, the bottom two its grey stand.");
    R.item("Mailbox", "The mod's own block, by each house: a red box with its slot, on a silver post.");
    R.item("Roof", "Acacia stairs in HeartGold's roof red: rows of tiles along the eaves, a darker trim at the ends (your house and Blue's).");
    R.spawn = { x: 12, z: 16.6, yaw: FACE.n };
    return R;
  },

  league_looks() {
    // The looks the mod gives the POKéMON LEAGUE's gate at Route 22's west end (12:44), on John's blocks: its roof
    // (polished deepslate) lime green metal, its rim a shade deeper; its walls (resin bricks) Let's Go's warm brick with
    // lighter and darker ones mixed in; the deepslate brick pillars either side of its door metal; the oak planks and
    // stairs over the door a lime green canopy; its steps the gates' stone; its little trees out front (spruce fence
    // trunks, acacia leaves) a wooden trunk and a round fresh green bush; Route 22's path (mud bricks, and their stairs
    // and slabs) up to it. Everything faces you (south).
    const R = new Room("league_looks", { name: "POKéMON LEAGUE gate looks", floor: "Outside (the mod's blocks)", W: 22, D: 16, H: 9, outside: true });
    const G = R.geo, P16 = 1 / 16;
    G.box(0, -1, 0, 22, 0, 16, { top: "grass", all: "soil" });
    // The path up to the door: mud bricks laid as Route 22's path (the picture over four blocks), a step of its slabs.
    G.box(8, -0.98, 9, 14, 0.01, 16, { top: "area_route_path", all: null });
    G.box(8, 0, 8.5, 14, 0.5, 9, { all: "area_route_path", top: "area_route_path" });
    // The building's front: brick from x 2 to 20, 6 high, a door 4 wide in the middle.
    const x0 = 2, x1 = 20, zf = 4;
    G.box(x0, 0, 0, 9, 6, zf, { s: "area_league_brick", top: null, all: "area_league_brick" });
    G.box(13, 0, 0, x1, 6, zf, { s: "area_league_brick", top: null, all: "area_league_brick" });
    G.box(9, 3.2, 0, 13, 6, zf, { s: "area_league_brick", all: "area_league_brick", top: null });
    G.box(9, 0, zf - 0.6, 13, 3.2, zf - 0.55, { s: "doorDark", all: null });
    R.solid(x0, 0, x1, zf);
    // The metal pillars either side of the door (deepslate brick walls), up to the roof.
    for (const px of [8, 13]) G.box(px + 4 * P16, 0, zf, px + 12 * P16, 6, zf + 0.5, "area_league_pillar");
    // The lime canopy over the door (oak planks and stairs), and the stone steps under it.
    G.box(8, 3.4, zf, 14, 3.9, zf + 1.4, "area_league_arch");
    G.box(8.5, 3.9, zf, 13.5, 4.3, zf + 0.9, "area_league_arch");
    G.box(8.5, 0, zf, 13.5, 0.5, zf + 1.4, "area_gate_stone");
    // The roof: lime green metal, its rim a shade deeper.
    G.box(x0 - 0.5, 6, -0.5, x1 + 0.5, 6.5, zf + 0.6, { all: "area_league_roof_rim", top: "area_league_roof" });
    G.box(x0, 6.5, -0.5, x1, 7.2, zf, { all: "area_league_roof", top: "area_league_roof" });
    // The four little trees out front: a wooden trunk, a round bush on top.
    for (const tx of [3.5, 5.8, 15.2, 17.5]) {
      G.box(tx + 6 * P16, 0, zf + 2.4, tx + 10 * P16, 1.2, zf + 2.4 + 4 * P16, "area_league_trunk");
      G.box(tx - 0.4, 1.2, zf + 1.7, tx + 1.4, 2.8, zf + 3.5, "area_league_bush");
      G.box(tx - 0.1, 2.8, zf + 2.0, tx + 1.1, 3.3, zf + 3.2, "area_league_bush");
      R.solid(tx - 0.4, zf + 1.7, tx + 1.4, zf + 3.5);
    }
    R.act([8, 3.6, 14, 5.6], { label: "Look", lines: ["The POKéMON LEAGUE's gate, as the mod draws John's: a lime green metal roof, warm mixed brick, metal pillars and a lime canopy over the door."] });
    R.item("Roof", "Polished deepslate as lime green metal sheets (FireRed's roof is pale green), the rim a shade deeper.");
    R.item("Walls", "Resin bricks as Let's Go's warm red-orange brick, a lighter and a darker one here and there.");
    R.item("Front", "The deepslate brick pillars metal, the oak over the door a lime green canopy, the steps the gates' stone.");
    R.item("Trees", "Spruce fence trunks as wood, acacia leaves as a round fresh green bush (its own green, not the biome's).");
    R.spawn = { x: 11, z: 13, yaw: FACE.n };
    return R;
  },

  pewter_looks() {
    // The looks the mod gives Pewter City (13:18), on John's blocks: his paving (white concrete, smooth quartz, white
    // terracotta, chiselled quartz, light grey concrete, in his own pattern) each block a paving stone of its own
    // colour; a house front as his (Pewter's two and Route 2's trade house): the pale oak walls solid, the deepslate
    // tile roof metal, the window's stairs white frames, the trapdoors painted panels, the mud brick chimney solid; a
    // flower bed in a white planter (its pale oak trapdoors); the white fence; the town sign (pale oak shelves) as
    // HeartGold's and the TRAINER TIPS sign (oak shelves) as our wooden sign; a corner of the Museum: its purpur roof
    // FireRed's purple tiles, the crimson bands darker, its white terracotta walls cream. Everything faces you (south).
    const R = new Room("pewter_looks", { name: "Pewter City looks", floor: "Outside (the mod's blocks)", W: 28, D: 24, H: 9, outside: true });
    const G = R.geo, P16 = 1 / 16;
    // The paving, as John laid it: his four by four pattern, a border of smooth quartz.
    const MOTIF = ["tgtW", "WtWc", "cWcW", "WtWc"], PAVE = { t: "terracotta", g: "gray", W: "white", c: "chiseled", q: "quartz" };
    const px0 = 1, px1 = 15, pz0 = 11, pz1 = 23;
    for (let x = 0; x < R.W; x++) for (let z = 0; z < R.D; z++) {
      const inPave = x >= px0 && x < px1 && z >= pz0 && z < pz1;
      if (!inPave) { G.box(x, -1, z, x + 1, 0, z + 1, { top: "grass", all: "soil" }); continue; }
      const border = x === px0 || x === px1 - 1 || z === pz0 || z === pz1 - 1;
      const k = border ? "q" : MOTIF[(z - pz0 - 1) % 4][(x - px0 - 1) % 4];
      G.box(x, -1, z, x + 1, 0, z + 1, { top: "area_pave_" + PAVE[k], all: k === "c" ? "area_pave_chiseled_side" : "area_pave_" + PAVE[k] });
    }
    // The house front (x 2 to 12): pale walls three high, a door, two windows in white frames with a painted panel
    // under each, the roof's layers of metal slabs stepping back, the chimney.
    const hx0 = 2, hx1 = 13, hz = 7;
    G.box(hx0, 0, hz - 4, hx1, 3, hz, { all: "area_pewter_wall", top: null });
    G.box(7, 0, hz, 8.95, 2.1, hz + 0.02, { s: "doorDark", all: null });
    for (const wx of [3.5, 10]) {
      G.box(wx, 1.0, hz, wx + 1.6, 2.2, hz + 0.02, { s: "window", all: null });
      G.box(wx - 0.15, 0.85, hz, wx + 1.75, 1.0, hz + 0.25, "area_window_frame"); G.box(wx - 0.15, 2.2, hz, wx + 1.75, 2.35, hz + 0.12, "area_window_frame");
      G.box(wx - 0.15, 1.0, hz, wx, 2.2, hz + 0.12, "area_window_frame"); G.box(wx + 1.6, 1.0, hz, wx + 1.75, 2.2, hz + 0.12, "area_window_frame");
      G.box(wx, 0.15, hz, wx + 1.6, 0.8, hz + 3 * P16, "area_pewter_shutter");
    }
    for (let i = 0; i < 4; i++) G.box(hx0 - 0.5 + i * 0.5, 3 + i * 0.5, hz - 4 + i * 0.6, hx1 + 0.5 - i * 0.5, 3.5 + i * 0.5, hz + 0.5 - i * 0.6, "area_pewter_roof");
    G.box(10.5, 4.5, 4.2, 11.5, 6.0, 5.2, { all: "area_pewter_chimney" }); G.box(10.4, 6.0, 4.1, 11.6, 6.3, 5.3, "area_pewter_chimney");
    R.solid(hx0, hz - 4, hx1, hz);
    R.act([7, hz, 9, hz + 1], { label: "Look", lines: ["A Pewter house as the mod draws John's: solid pale walls, a metal roof, white window frames, painted panels, a solid chimney."] });
    // The flower bed: a white planter (the pale oak trapdoors round it), flowers in it.
    const fx0 = 17, fx1 = 21, fz0 = 12, fz1 = 14;
    G.box(fx0, 0, fz0, fx1, 0.35, fz1, { all: "area_planter_white", top: "soil" });
    for (let x = fx0; x < fx1; x++) for (let z = fz0; z < fz1; z++) {
      const red = (x + z) % 2 === 0;
      G.box(x + 0.4, 0.35, z + 0.4, x + 0.6, 0.75, z + 0.6, "leafSolid");
      G.box(x + 0.32, 0.75, z + 0.32, x + 0.68, 0.95, z + 0.68, red ? "pewterPoppy" : "pewterTulip");
    }
    R.solid(fx0, fz0, fx1, fz1);
    // The white fence (pale oak fence) along the garden.
    const fz = 10.5;
    for (let x = 16; x <= 26; x++) {
      G.box(x + 6 * P16, 0, fz - 2 * P16, x + 10 * P16, 1, fz + 2 * P16, "area_white_fence");
      if (x < 26) for (const [y0, y1] of [[6, 9], [12, 15]]) G.box(x + 10 * P16, y0 * P16, fz - P16, x + 22 * P16, y1 * P16, fz + P16, "area_white_fence");
    }
    R.solid(16, fz - 0.2, 27, fz + 0.2);
    // The two signs, two shelves by two facing south: the town sign (pale oak) as HeartGold's, TRAINER TIPS (oak) in wood.
    const sign = (sx, sz, pre, lines) => {
      for (const [bx, part] of [[sx, "l"], [sx + 1, "r"]]) {
        const outerW = part === "l", outerE = part === "r";
        G.box(bx, 1, sz + 5 * P16, bx + 1, 2, sz + 11 * P16, { s: "area_" + pre + "_front_" + part, n: "area_" + pre + "_back_" + part, top: "area_" + pre + "_edge_h", bottom: "area_" + pre + "_edge_h",
          w: outerW ? "area_" + pre + "_edge_v" : null, e: outerE ? "area_" + pre + "_edge_v" : null });
        G.box(bx + (outerW ? 2 * P16 : 0), 2 * P16, sz + 6 * P16, bx + 1 - (outerE ? 2 * P16 : 0), 1, sz + 10 * P16, { all: "area_" + pre + "_stand", top: null, bottom: null,
          w: outerW ? "area_" + pre + "_stand" : null, e: outerE ? "area_" + pre + "_stand" : null });
        G.box(bx + (outerW ? P16 : 0), 0, sz + 5 * P16, bx + 1 - (outerE ? P16 : 0), 2 * P16, sz + 11 * P16, { all: "area_" + pre + "_foot",
          w: outerW ? "area_" + pre + "_foot" : null, e: outerE ? "area_" + pre + "_foot" : null });
      }
      R.solid(sx, sz + 0.3, sx + 2, sz + 0.7);
      R.act([sx, sz, sx + 2, sz + 1], { label: "Read", lines });
    };
    sign(17, 17, "sign", ["PEWTER CITY", "A Stone Gray City"]);
    sign(22, 17, "wsign", ["TRAINER TIPS", "Any POKéMON that takes part in battle, however short, earns EXP!"]);
    // A corner of the Museum (x 16 to 27): cream walls, the crimson band, the purple tiled roof stepping up to its
    // ridge (running east to west here), the purple flats.
    const mx0 = 16, mx1 = 27, mz0 = 1, mz1 = 6;
    G.box(mx0, 0, mz0, mx1, 3, mz1, { all: "area_museum_wall", top: null });
    G.box(mx0 - 0.3, 3, mz0 - 0.3, mx1 + 0.3, 3.5, mz1 + 0.3, { all: "area_museum_roof_dark", top: "area_museum_roof_flat" });
    for (let i = 0; i < 3; i++) {
      const z0 = mz1 - i, y = 3.5 + i * 0.5;
      G.box(mx0, y, z0 - 1, mx1, y + 0.5, z0, { top: "area_roof_museum_tiles", s: "area_roof_museum_tiles", e: "area_roof_museum_edge", w: "area_roof_museum_edge", bottom: "area_roof_museum_under" });
    }
    G.box(mx0, 5.0, mz0, mx1, 5.5, mz0 + 2, { all: "area_museum_roof_dark", top: "area_museum_roof_flat" });
    G.box(19, 0, mz1, 21, 2.2, mz1 + 0.02, { s: "doorDark", all: null });
    R.solid(mx0, mz0, mx1, mz1);
    R.act([19, mz1, 21, mz1 + 1], { label: "Look", lines: ["The Museum as the mod draws John's: FireRed's purple tiles on its roof, the crimson bands darker, cream walls."] });
    R.item("Paving", "John's paving, each block a paving stone of its own colour, one a block, in his pattern.");
    R.item("Houses", "Pale oak walls solid, the deepslate tile roof metal, white window frames, painted panels, a solid chimney.");
    R.item("Signs", "The town sign (pale oak shelves) as HeartGold's sign; TRAINER TIPS (oak shelves) as our wooden sign.");
    R.item("Museum", "Its purpur roof FireRed's purple tiles, the crimson slabs darker bands, the white terracotta cream.");
    R.spawn = { x: 8, z: 16, yaw: FACE.n };
    return R;
  },

  viridian_looks() {
    // The looks the mod gives Viridian City and Route 22 (client/AreaLooks, tools/lookarea.py), on John's blocks:
    // his path of calcite, diorite and polished diorite (here in his own pattern) as one white path in three
    // patterns, a light grey kerb along the grass with the grass rounded onto it; a lamp (his tuff, in black, the
    // sea lantern kept); his two standing pale oak signs as HeartGold's sign and his spruce sign as its wooden sign,
    // taller; a Pokémon Center's reds and a Mart's blues as Let's Go's; Route 22's floor (mud bricks) as a dirt path
    // with its border (brown concrete powder) a shade deeper. Everything faces you (south).
    const R = new Room("viridian_looks", { name: "Viridian City and Route 22 looks", floor: "Outside (the mod's blocks)", W: 28, D: 30, H: 8, outside: true });
    const G = R.geo, P16 = 1 / 16;
    const ROWS = ["cdcdcd", "cpcpdc", "cdcdcd", "cpcpdc", "cdcddd", "pdpdcc", "cccccd", "cdcdcd"];
    const isPath = (x, z) => (x >= 3 && x < 9 && z >= 2 && z < 20) || (x >= 9 && x < 21 && z >= 8 && z < 13);
    const kind = (x, z) => ({ c: "calcite", d: "diorite", p: "polished_diorite" })[ROWS[((z % 8) + 8) % 8][((x - 3) % 6 + 6) % 6]];
    const grassAt = (x, z) => x >= 0 && z >= 0 && x < R.W && z < R.D && !isPath(x, z) && !(x >= 21 && z >= 15 && z < 22);
    for (let x = 0; x < R.W; x++) for (let z = 0; z < R.D; z++) {
      if (isPath(x, z)) G.box(x, -1, z, x + 1, 0, z + 1, "area_path_" + kind(x, z));
      else if (!(x >= 21 && z >= 15 && z < 22)) G.box(x, -1, z, x + 1, 0, z + 1, { top: "grass", all: "soil" });
    }
    for (let x = 0; x < R.W; x++) for (let z = 0; z < R.D; z++) {
      if (!isPath(x, z)) continue;
      const n = grassAt(x, z - 1), so = grassAt(x, z + 1), w = grassAt(x - 1, z), e = grassAt(x + 1, z);
      const nw = grassAt(x - 1, z - 1), ne = grassAt(x + 1, z - 1), sw = grassAt(x - 1, z + 1), se = grassAt(x + 1, z + 1);
      const edges = (n ? 1 : 0) | (e ? 2 : 0) | (so ? 4 : 0) | (w ? 8 : 0);
      const tiles = ({ 1: [16], 2: [8], 4: [2], 8: [10], 3: [11], 6: [4], 12: [5], 9: [12], 5: [16, 2], 10: [8, 10], 14: [6], 13: [7], 7: [13], 11: [14], 15: [9] })[edges] || [];
      if (nw && !n && !w) tiles.push(17);
      if (ne && !n && !e) tiles.push(15);
      if (sw && !so && !w) tiles.push(3);
      if (se && !so && !e) tiles.push(1);
      for (const t of tiles) G.box(x, 0.002, z, x + 1, 0.002, z + 1, { top: "area_kerb_" + t, all: null });
      for (const t of tiles) G.box(x, 0.004, z, x + 1, 0.004, z + 1, { top: "area_kerb_grass_" + t, all: null });
    }
    // Route 22's floor in the corner: mud bricks as the dirt path, ringed by its brown concrete border.
    for (let x = 21; x < R.W; x++) for (let z = 15; z < 22; z++) {
      const rim = x === 21 || z === 15 || x === R.W - 1 || z === 21;
      G.box(x, -1, z, x + 1, 0, z + 1, rim ? "area_route_path_edge" : "area_route_path");
    }
    // A lamp: chiseled tuff bricks at its foot and under its head, polished tuff wall and tuff walls between (in
    // black), the sea lantern in black glass, the grey carpet cap (black) and the daylight sensor on top.
    const lx = 11, lz = 5;
    G.box(lx, 0, lz, lx + 1, 1, lz + 1, { all: "area_lamp_chiseled_tuff_bricks", top: "area_lamp_chiseled_tuff_bricks_top", bottom: null });
    G.box(lx + 4 * P16, 1, lz + 4 * P16, lx + 12 * P16, 6, lz + 12 * P16, { all: "area_lamp_post", top: null, bottom: null });
    G.box(lx, 6, lz, lx + 1, 7, lz + 1, { all: "area_lamp_chiseled_tuff_bricks", top: null, bottom: "area_lamp_chiseled_tuff_bricks_top" });
    G.box(lx, 7, lz, lx + 1, 8, lz + 1, "seaLantern");
    G.box(lx - 1, 7, lz - 1, lx + 2, 8, lz + 2, { all: "blackGlass", top: null, bottom: null });
    G.box(lx - 1, 8, lz - 1, lx + 2, 8 + P16, lz + 2, "area_lamp_cap");
    G.box(lx, 8 + P16, lz, lx + 1, 8 + 6 * P16, lz + 1, { all: "blackGlass", top: "area_lamp_cap" });
    R.solid(lx, lz, lx + 1, lz + 1);
    // John's two standing pale oak signs, side by side, as HeartGold's sign a block high.
    const sz = 14;
    for (const [bx, part] of [[13, "l"], [14, "r"]]) {
      const outerW = part === "l", outerE = part === "r";
      G.box(bx - (outerW ? 0.25 * P16 : 0), 8.5 * P16, sz + 5 * P16, bx + 1 + (outerE ? 0.25 * P16 : 0), 18.5 * P16, sz + 11 * P16, { s: "area_sign_short_front_" + part, n: "area_sign_short_back_" + part,
        top: "area_sign_edge_h", bottom: "area_sign_edge_h", w: outerW ? "area_sign_edge_v" : null, e: outerE ? "area_sign_edge_v" : null });
      G.box(bx + (outerW ? 2 * P16 : 0), 2 * P16, sz + 6 * P16, bx + 1 - (outerE ? 2 * P16 : 0), 8.5 * P16, sz + 10 * P16, { all: "area_sign_stand", top: null, bottom: null,
        w: outerW ? "area_sign_stand" : null, e: outerE ? "area_sign_stand" : null });
      G.box(bx + (outerW ? P16 : 0), 0, sz + 5 * P16, bx + 1 - (outerE ? P16 : 0), 2 * P16, sz + 11 * P16, { all: "area_sign_foot", w: outerW ? "area_sign_foot" : null, e: outerE ? "area_sign_foot" : null });
    }
    R.solid(13, sz + 0.3, 15, sz + 0.7);
    // The spruce sign as HeartGold's wooden sign, taller: a board on a thick post.
    const wx = 17, wz = 14;
    G.box(wx - 0.5 * P16, 9 * P16, wz + 6.5 * P16, wx + 16.5 * P16, 25 * P16, wz + 9.5 * P16, { s: "area_wood_sign_front", n: "area_wood_sign_back", all: "area_wood_sign_edge" });
    G.box(wx + 6.5 * P16, 0, wz + 6.5 * P16, wx + 9.5 * P16, 9 * P16, wz + 9.5 * P16, { all: "area_wood_post", top: null });
    G.box(wx + 5 * P16, 25 * P16, wz + 6 * P16, wx + 11 * P16, 26 * P16, wz + 10 * P16, "area_wood_post");
    R.solid(wx, wz + 0.3, wx + 1, wz + 0.7);
    // A Pokémon Center's roof (stripped mangrove wood with mangrove stairs along its edge) and a Mart's (blue
    // concrete and wool, a blue carpet on top), on white walls: Let's Go's red and blue.
    G.box(13, 0, 2, 18, 2, 5, "siding");
    G.box(13, 2, 2, 18, 3, 5, "area_center_stripped_mangrove_log");
    G.box(12.5, 2, 5, 18.5, 2.5, 5.5, "area_center_mangrove_planks");
    G.box(13.5, 3, 2.5, 17.5, 3.5, 4.5, "area_center_mangrove_planks");
    G.box(20, 0, 2, 25, 2, 5, "siding");
    G.box(20, 2, 2, 25, 3, 5, { all: "area_mart_blue_concrete", top: "area_mart_blue_wool" });
    G.box(20, 3, 2, 25, 3 + P16, 5, "area_mart_blue_wool");
    G.box(19.5, 1.5, 5, 25.5, 2, 5.5, "area_mart_blue_wool");
    R.solid(13, 2, 18, 5); R.solid(20, 2, 25, 5);
    // One of the green houses' roofs (warped stairs) in green tiles, its side window's frame (pale oak stairs) solid
    // white; and a Gym's roof (jungle stairs, its planks and beams) in tan.
    const gable = (base, z0, z1, set, wall) => {
      for (let i = 0; i < 3; i++) for (const side of ["w", "e"]) {
        const x = side === "w" ? base + i : base + 5 - i, y = i, top = "area_" + set + "_top_" + side;
        G.box(x, y, z0, x + 1, y + 0.5, z1, { top, n: "area_" + set + "_edge", s: "area_" + set + "_edge", w: "area_" + set + "_tiles", e: "area_" + set + "_tiles", bottom: "area_" + set + "_under" });
        const hx0 = side === "w" ? x + 0.5 : x, hx1 = side === "w" ? x + 1 : x + 0.5;
        G.box(hx0, y + 0.5, z0, hx1, y + 1, z1, { top, n: "area_" + set + "_edge", s: "area_" + set + "_edge", w: "area_" + set + "_tiles", e: "area_" + set + "_tiles", bottom: null });
        if (y > 0) G.box(x, 0, z0 + 0.5, x + 1, y, z1 - 0.5, wall);
      }
      R.solid(base, z0, base + 6, z1);
    };
    gable(0.5, -0.2, 1.6, "roof_green", "siding");
    G.box(6.6, 0.9, 0.2, 6.7, 1.9, 1.2, "window");
    G.box(6.5, 0.8, 0.1, 6.8, 0.9, 1.3, "area_window_frame"); G.box(6.5, 1.9, 0.1, 6.8, 2.0, 1.3, "area_window_frame");
    G.box(6.5, 0.8, 0.1, 6.8, 2.0, 0.2, "area_window_frame"); G.box(6.5, 0.8, 1.2, 6.8, 2.0, 1.3, "area_window_frame");
    gable(21.5, 8, 11.5, "roof_gym", "siding");
    G.box(24, 2.5, 8, 25, 3, 11.5, { all: "area_gym_beam", top: "area_gym_beam_top" });
    // A route gate (every one on the map): its pillars (andesite, two high, polished andesite stairs on them) a
    // solid grey, its walls (birch, stripped oak, a stripped jungle beam) solid shades, its roof and the overhang
    // over the way in gunmetal metal, the steps up solid stone.
    const gx = 12, gz = 23;
    G.box(gx, 0, gz, gx + 1, 2, gz + 1, "area_gate_pillar"); G.box(gx + 7, 0, gz, gx + 8, 2, gz + 1, "area_gate_pillar");
    G.box(gx, 2, gz, gx + 1, 2.5, gz + 1, "area_gate_stone"); G.box(gx + 7, 2, gz, gx + 8, 2.5, gz + 1, "area_gate_stone");
    G.box(gx + 1, 0, gz + 0.2, gx + 7, 3.5, gz + 0.8, "area_gate_wall_birch");
    G.box(gx + 1, 1.0, gz + 0.1, gx + 7, 1.4, gz + 0.9, "area_gate_beam");
    for (const x of [gx + 2, gx + 5]) G.box(x, 0, gz + 0.1, x + 1, 3.5, gz + 0.9, { all: "area_gate_wall_oak", top: "area_gate_wall_oak_top" });
    G.box(gx + 3, 0, gz + 0.8, gx + 5, 2.4, gz + 0.82, "blackGlass");
    G.box(gx + 2.5, 2.4, gz + 0.8, gx + 5.5, 2.9, gz + 1.8, "area_gate_roof");
    G.box(gx - 0.5, 3.5, gz - 0.5, gx + 8.5, 4.0, gz + 1.5, "area_gate_roof");
    G.box(gx - 0.5, 4.0, gz - 0.5, gx + 8.5, 4.4, gz - 0.2, "area_gate_roof_rim"); G.box(gx - 0.5, 4.0, gz + 1.2, gx + 8.5, 4.4, gz + 1.5, "area_gate_roof_rim");
    G.box(gx + 3, 0, gz + 1, gx + 5, 0.5, gz + 2, "area_gate_stone");
    R.solid(gx, gz, gx + 8, gz + 1);
    R.item("Gates", "Every route gate on the map: its pillars a solid grey, its walls solid shades (birch, stripped oak, the jungle beams), the roof and the overhang over the way in gunmetal metal, the steps solid stone.");
    R.item("Path", "John's calcite, diorite and polished diorite: one white, each its own pattern of bumps (four, five, three bigger), a light grey kerb along the grass, the grass rounded onto it at the corners.");
    R.item("Lamp", "His tuff lamp as Let's Go's black lampposts: the post solid off-black, the chiselled blocks keeping their pattern, the cap metal, the sea lantern as it is.");
    R.item("Signs", "His standing pale oak signs as HeartGold's sign; his spruce sign as its wooden sign, taller (Minecraft's own sign inside, so it never shows).");
    R.item("Roofs", "Every Pokémon Center's reds and every Mart's blues on the map in Let's Go's red and blue as solid metal sheets, each of John's blocks its own shade (the Center's emblem too); the green houses' roofs in green tiles, their window frames solid white; every Gym's roof in tan tiles, its flats and beams solid tan.");
    R.item("Route 22", "The mud brick floor as the dirt paths, its brown border a shade deeper (the mud brick steps stay as the ledges).");
    R.spawn = { x: 10, z: 19.5, yaw: FACE.n };
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
