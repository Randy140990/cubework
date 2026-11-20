// ---------- the places: your house (downstairs and your room), the rival's house (the same space), and Pallet Town outside ----------
// Inside, a room is 16 blocks wide (west to east) and 14 deep (north to south), its walls 4 high: twice the old games' 8 by 7
// tiles of floor. Outside, the houses are 8 by 6, so they're bigger on the inside, as the old games were.
const FACE = { n: 0, w: Math.PI / 2, s: Math.PI, e: -Math.PI / 2 };
const DOOR = [4.5, 7.5];
const PLACES = [
  { id: "house2", name: "Your room", floor: "Upstairs", group: "Your house" },
  { id: "house1", name: "Your house", floor: "Downstairs", group: "Your house" },
  { id: "rival1", name: "Rival's house", floor: "Downstairs", group: "Rival's house" },
  { id: "rival2", name: "Rival's room", floor: "Upstairs", group: "Rival's house" },
  { id: "lab", name: "Prof. Oak's lab", floor: "Ground floor", group: "Prof. Oak's lab" },
  { id: "center", name: "Pokémon Center", floor: "Viridian City", group: "Viridian City" },
  { id: "mart", name: "Poké Mart", floor: "Viridian City", group: "Viridian City" },
  { id: "town", name: "Pallet Town", floor: "Outside", group: "Outside" },
];
const FROM_TOWN = { x: 6.0, z: 12.9, yaw: FACE.n };
const LAB_DOOR = [7.5, 10.5], LAB_IN = { x: 9.0, z: 18.9, yaw: FACE.n }, FROM_LAB = { x: 25.0, z: 23.1, yaw: FACE.s };
const STAIRS_FOOT = { x: 11.1, z: 1.1, yaw: FACE.s };
const STAIRS_TOP = { x: 15.5, z: 2.9, yaw: FACE.s };

// The Pokémon Center's and the Poké Mart's ways in (2 wide, in the middle of the front), and where you stand coming in.
const CENTER_DOOR = [9, 11], CENTER_IN = { x: 10.0, z: 14.9, yaw: FACE.n };
const MART_DOOR = [7, 9], MART_IN = { x: 8.0, z: 12.9, yaw: FACE.n };

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
  const mat = (base, edge, motif) => (P, r) => {
    P.fill(hx(base), 0.05, r);
    P.rect(0, 0, 32, 2, hx(edge)); P.rect(0, 18, 32, 2, hx(edge)); P.rect(0, 0, 2, 20, hx(edge)); P.rect(30, 0, 2, 20, hx(edge));
    P.rect(3, 3, 26, 1, hx("#F6F1EA")); P.rect(3, 16, 26, 1, hx("#F6F1EA")); P.rect(3, 3, 1, 14, hx("#F6F1EA")); P.rect(28, 3, 1, 14, hx("#F6F1EA"));
    if (motif === "ball") { P.disc(15.5, 9.5, 4.6, hx("#F6F1EA")); P.rect(11, 9, 10, 1, hx(edge)); P.disc(15.5, 9.5, 1.6, hx(edge)); P.disc(15.5, 9.5, 0.8, hx("#F6F1EA")); }
    else for (let x = 6; x < 27; x += 4) P.rect(x, 6, 2, 8, hx(edge));
  };
  reg("matHome", 32, 20, mat("#C9433F", "#8E2A2A", "ball"), { fit: true });
  reg("matRival", 32, 20, mat("#33477A", "#1E2A4A", "stripes"), { fit: true });
  reg("matLab", 32, 20, mat("#2F9C8A", "#1D6B5E", "ball"), { fit: true });
  reg("matCenter", 32, 20, mat("#F08A3C", "#C7502E", "ball"), { fit: true });
  reg("matMart", 32, 20, mat("#3A6FD0", "#24479A", "stripes"), { fit: true });
  // The Center's runner, door to counter: red, cream edges. The Mart's: blue, white edges.
  RUGS.centerRun = (i, j, w, d) => (i === 0 || i === w - 1) ? "#F6F1EA" : (j % 3 === 1 ? "#E26A64" : "#D9524E");
  RUGS.martRun = (i, j, w, d) => (i === 0 || i === w - 1) ? "#F2F6FA" : (j % 3 === 1 ? "#5A8EE0" : "#3A6FD0");
  RUGS.center = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#D9524E" : (i === 1 || j === 1 || i === w - 2 || j === d - 2) ? "#F6F1EA" : ((i + j) % 2 ? "#E87A72" : "#D9524E");
  RUGS.mart = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#3A6FD0" : ((i + j) % 2 ? "#E8F2F8" : "#9CC8E6");
})();

// A band of colour round the three walls you can see, at a height.
function band(room, y, mat, h = 0.22) {
  const G = room.geo, W = room.W, D = room.D;
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
function ballTable(room, x0, z0, x1, z1) {
  table(room, x0, z0, x1, z1, { top: "lab", leg: "steel", h: 0.95, name: "The table of balls", note: "A white-topped steel table with three balls in a row." });
  const n = 3, s = 0.36, y = 0.95, cz = (z0 + z1) / 2;
  for (let i = 0; i < n; i++) { const cx = x0 + (x1 - x0) * (i + 1) / (n + 1); room.geo.box(cx - s / 2, y, cz - s / 2, cx + s / 2, y + s, cz + s / 2, { all: "ball", top: "ballTop" }); }
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
  const ball = (bx, by, bz, s = 0.3) => G.box(x + bx - s / 2, by, z + bz - s / 2, x + bx + s / 2, by + s, z + bz + s / 2, { all: "ball", top: "ballTop" });
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
    else F.box(lx - 0.1, 1.2, lz - 0.1, lx + 0.1, 1.38, lz + 0.1, { all: "ball", top: "ballTop" }); }
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

function bunting(room, x0, x1, y) { room.geo.box(x0, y, 0.04, x1, y + 0.45, 0.05, { s: "bunting", all: null }); room.item("Bunting", "Pennants strung along the wall."); }

const BUILD = {
  house1() {
    const R = new Room("house1", { name: "Your house", floor: "Downstairs", paper: "paperMum", outsideSize: "8 by 6" });
    shell(R, { paper: "mcCream", floor: "sqHoney", zones: [[0, 0, 8, 6, "mcQuartzCheck"]], door: DOOR, skirt: "mcStripped" });
    kitchen(R, 0.2, "s");
    clock(R, 6.85, 2.55);
    tvSet(R, 7.75, 0, "s", R.screen("tvFilm", 40, 24, drawFilm, 0.9), { w: 3.2, speakers: true,
      lines: ["There's a film on. Four kids are setting off down a railway line.", "Maybe it's time you set off too."] });
    R.item("Television", "On a walnut stand with speakers; the film on it moves.");
    stairsUp(R, 12, 0, 2, { to: "house2", spawn: STAIRS_TOP });
    rug(R, 8, 3, 13, 9, "rugMum");
    coffeeTable(R, 8.9, 4.6, 10.7, 5.5);
    sofa(R, 11.4, 7.6, "n", 3.2);
    floorLamp(R, 12.15, 7.2);
    table(R, 1.6, 6.4, 4.9, 8.4, { cloth: "linen", name: "Dining table", note: "Oak top, walnut legs, a linen runner, plates and a fruit bowl." });
    fruitBowl(R, 3.25, 7.4, 0.87); plates(R, [[2.4, 6.8], [4.1, 6.8], [2.4, 8.0], [4.1, 8.0]], 0.87);
    chair(R, 2.4, 5.85, "s"); chair(R, 4.1, 5.85, "s"); chair(R, 2.4, 8.95, "n"); chair(R, 4.1, 8.95, "n"); chair(R, 5.45, 7.4, "w");
    R.item("Chairs", "Walnut chairs, four round the table and one at its end for Mum.");
    sideboard(R, 0, 12.2, "e", 2.4);
    framed(R, 10.1, 2.0, 1.2, 0.9, "photos", "w", { act: { label: "Look", lines: ["You, Mum and a very small partner on the beach."] } });
    bookshelf(R, 16, 9.0, "w", 2.2, 3.2, { trinkets: { 1: "plant", 3: "ball" } });
    framed(R, 3.3, 1.5, 2.6, 1.9, "painting", "e", { act: { label: "Look", lines: ["A painting of the sea south of Pallet Town. Mum painted it."] } });
    R.item("Paintings and photos", "Framed in walnut on the side walls.");
    plant(R, 1.0, 12.9, 1.15); plant(R, 15.0, 12.9, 1.15);
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matHome" });
    R.npcs.push({ name: "Mum", x: 5.45, z: 7.4, yaw: FACE.w, seated: true,
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
    const R = new Room("house2", { name: "Your room", floor: "Upstairs", paper: "paperRoom" });
    shell(R, { paper: "mcSky", floor: "sqLight", floorRects: [[0, 0, 12, 14], [12, 2, 16, 14]], skirt: "mcStripped" });
    stairsDown(R, 12, 0, 2, { to: "house1", spawn: STAIRS_FOOT, paper: "mcSky" });
    deskPC(R, 0.4, 0, "s", R.screen("pcScreen", 32, 24, drawPC, 1), { pc: true });
    officeChair(R, 1.55, 1.6, "n");
    whiteboard(R, 3.2, 1.45, 2.4, 1.2, "whiteboard", ["The whiteboard says:", "TO DO. 1, wake up (crossed out). 2, see the Prof. 3, become champion!"]);
    windowN(R, 6.5, 1.5, 2.2, 2.0, { curtains: "curtainBlue" });
    bookshelf(R, 9.3, 0, "s", 2, 2.6, { wood: "pale", trinkets: { 0: "ball", 2: "plant" }, lines: ["Comics, a field guide to Kanto's creatures, and a very old ball."], name: "Shelves", note: "Pale-wood shelves with books, a ball and a little plant." });
    bed(R, 0, 10.6, "e", { w: 1.8, l: 2.9 });
    nightstand(R, 0, 8.6, "e");
    rug(R, 5, 6, 10, 10, "rugRoom");
    tvSet(R, 6.0, 4.9, "s", R.screen("tvRace", 32, 24, drawRace, 0.95), { w: 2.4, tw: 1.7, th: 1.05, lines: ["A racing game, paused on lap 2. It'll keep."] });
    gameConsole(R, 7.2, 6.8);
    cushionSeat(R, 6.5, 8.3, "blueFabric"); cushionSeat(R, 8.0, 8.3, "cushion");
    wardrobe(R, 16, 4.2, "w");
    framed(R, 7.2, 1.6, 1.3, 1.75, "poster", "e", { frameMat: "honey", act: { label: "Look", lines: ["A poster of a famous trainer mid-throw. GO!"] } });
    R.item("Poster", "A trainer mid-throw, drawn in silhouette on a sunburst.");
    bagOnFloor(R, 3.6, 12.6, ["Your bag. Packed and ready for the road."]);
    plant(R, 15.0, 12.8, 1.0, { lines: ["A little fern. You remember to water it, mostly."] });
    R.spawn = { x: 4.2, z: 9.5, yaw: FACE.s };
    return R;
  },

  rival1() {
    const R = new Room("rival1", { name: "Rival's house", floor: "Downstairs", paper: "paperRival", outsideSize: "8 by 6" });
    shell(R, { paper: "mcSage", floor: "sqPale", zones: [[0, 0, 6, 6, "mcBlueCheck"]], door: DOOR, skirt: "mcStripped" });
    const F = R.F(0.2, 0, "s");
    counterUnit(R, F, 0, 1, "cupboardWhite", "counter", "paint"); counterUnit(R, F, 1, 1, "cupboardWhite", "sink", "paint");
    counterUnit(R, F, 2, 1, "oven", "hob", "paint"); counterUnit(R, F, 3, 1, "cupboardWhite", "counter", "paint");
    F.box(1.65, 1.0, 0.08, 1.75, 1.45, 0.18, "steel"); F.box(1.65, 1.38, 0.08, 1.75, 1.45, 0.48, "steel");
    F.box(0, 1.0, 0, 4, 1.8, 0.03, { front: "tileWhite", all: null });
    F.box(0, 2.2, 0, 1.5, 3.0, 0.55, { front: "cupboardWhite", all: "paint", back: null }); F.box(2.5, 2.2, 0, 4, 3.0, 0.55, { front: "cupboardWhite", all: "paint", back: null });
    F.box(3.2, 1.0, 0.25, 3.7, 1.5, 0.7, { all: "plastic", top: "steel" });
    R.act(F.rect(0, 0.5, 4, 1.4), { label: "Look", lines: ["A tidy little kitchen. The coffee machine is still warm."] });
    R.item("Kitchen", "White cupboards, sink and tap, a hob and oven, white tiles and a coffee machine.");
    bookshelf(R, 5.4, 0, "s", 2, 3.0, { lines: ["Books about battling. Most have the corners turned down."] });
    bookshelf(R, 7.4, 0, "s", 2, 3.0, { trinkets: { 2: "trophy" }, lines: ["A row of training manuals, and a trophy shaped like a cup."] });
    trophyCabinet(R, 9.7, 0, "s");
    stairsUp(R, 12, 0, 2, { to: "rival2", spawn: STAIRS_TOP });
    rug(R, 5, 5, 11, 10, "rugRival");
    table(R, 5.8, 6.2, 9.4, 8.2, { top: "walnut", leg: "walnut", name: "Table", note: "Walnut table. The Town Map is lying on it." });
    R.geo.box(6.4, 0.86, 6.62, 8.0, 0.89, 7.42, { top: "worldMap", all: "linen" });
    R.act([6.2, 6.2, 8.2, 8.2], { label: "Look", map: true, lines: ["It's a Town Map. Daisy's keeping it safe for you.", "In game, this opens the TOWN MAP."] });
    chair(R, 5.2, 7.2, "e", { wood: "walnut" }); chair(R, 7.6, 5.65, "s", { wood: "walnut" }); chair(R, 7.6, 8.75, "n", { wood: "walnut" });
    plates(R, [[8.6, 7.2]], 0.86);
    sofa(R, 16, 4.6, "w", 3, { fabric: "blueFabric", cushion: "linen", pillow: "cushion", pillow2: "cushion", lines: ["A blue sofa. There's a game controller down the side of the cushion."] });
    framed(R, 6.0, 1.6, 2.4, 1.7, "paintingHills", "w", { act: { label: "Look", lines: ["A painting of the hills north of town, with a little red-roofed house."] } });
    plant(R, 1.0, 12.9, 1.15); plant(R, 15.0, 12.9, 1.15);
    floorLamp(R, 15.2, 8.4);
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 24.9, z: 10.0, yaw: FACE.s }, mat: "matRival" });
    R.npcs.push({ name: "Daisy", x: 5.2, z: 7.2, yaw: FACE.e, seated: true,
      look: { skin: "brunette_girl" },
      talk: () => [{ text: "Hi! My brother's already gone to Grandpa's lab." }, { text: "He always has to be first. You'd better catch him up!" }] });
    R.spawn = FROM_TOWN;
    return R;
  },

  rival2() {
    const R = new Room("rival2", { name: "Rival's room", floor: "Upstairs", paper: "paperRivalRoom" });
    shell(R, { paper: "mcNavy", floor: "sqDark", floorRects: [[0, 0, 12, 14], [12, 2, 16, 14]], skirt: "mcDarkOak" });
    stairsDown(R, 12, 0, 2, { to: "rival1", spawn: STAIRS_FOOT, paper: "mcNavy" });
    deskPC(R, 0.4, 0, "s", R.screen("pcLocked", 40, 24, drawLocked, 1), { top: "walnut", lines: ["His PC's locked. The password hint just says: ME."] });
    officeChair(R, 1.55, 1.6, "n", "redFabric");
    whiteboard(R, 3.2, 1.45, 2.4, 1.2, "boardRival", ["BATTLE PLAN: win, win again, beat him.", "\"Beat him\" has your name next to it, crossed out three times."]);
    windowN(R, 6.5, 1.5, 2.2, 2.0, { curtains: "curtain", lines: ["From up here you can see right into your own room."] });
    framed(R, 9.4, 1.5, 1.2, 1.6, "posterRival", "n", { frame: false, act: { label: "Look", lines: ["A poster: CHAMP OR BUST."] } });
    framed(R, 6.6, 1.6, 1.3, 1.75, "posterDragon", "e", { frame: false, act: { label: "Look", lines: ["A poster of a dragon breathing fire over the stars."] } });
    R.item("Posters", "CHAMP OR BUST, and a dragon over the stars.");
    bed(R, 16, 9.4, "w", { w: 1.8, l: 2.9, wood: "walnut", quilt: "quiltBlue", ask: false });
    R.acts[R.acts.length - 1].lines = ["An unmade bed, clothes all over it."];
    bookshelf(R, 0, 6.4, "e", 2.2, 2.6, { trinkets: { 0: "trophy", 1: "trophy", 2: "ball" }, lines: ["Trophies, and a ball signed by someone famous."], name: "Trophy shelf", note: "Walnut shelves with gold cups, books and a ball." });
    framed(R, 7.6, 1.6, 0.8, 0.8, "dartboard", "w", { frame: false, act: { label: "Look", lines: ["A dartboard. All the darts are in the bullseye."] } });
    R.item("Dartboard", "On the west wall; every dart in the middle.");
    punchingBag(R, 3.4, 9.4);
    rug(R, 5, 6, 10, 11, "rugRival");
    bagOnFloor(R, 9.0, 12.4, ["A sports bag, half unpacked. Running shoes, a towel, more trophies."]);
    plant(R, 1.0, 12.8, 1.0, { lines: ["A cactus. It looks like it gets on with him."] });
    beanBag(R, 12.6, 6.2, "redFabric"); dumbbells(R, 3.4, 12.6); bunting(R, 0.3, 6.2, 3.0);

    R.spawn = STAIRS_TOP;
    return R;
  },

  lab() {
    // Inside the lab: 18 blocks by 20. Laid out as the newer games' lab: the professor's desk, the ball incubator and his
    // machine along the north wall, the table of three balls, rows of white shelves, an aide's desk and board in the
    // corner, and the door. (Only the inside: in the mod, the outside is your world.)
    const R = new Room("lab", { name: "Prof. Oak's lab", floor: "Ground floor", W: 18, D: 20, outsideSize: "12 by 7" });
    shell(R, { paper: "mcLabWall", floor: "mcLabTile", door: LAB_DOOR, skirt: "mcCyan" });
    // North wall, west to east: a teal desk run with the PC, the incubator, the machine, the board with the map, the resting machine.
    { const F = R.F(0.3, 0, "s"); counterUnit(R, F, 0, 1, "cupboardTeal", "lab", "tealPaint"); counterUnit(R, F, 1, 1, "drawers", "lab", "tealPaint"); counterUnit(R, F, 2, 1, "cupboardTeal", "lab", "tealPaint"); }
    { const G = R.geo, sk = R.screen("pcLab", 40, 26, drawPC, 0.9);
      G.box(1.05, 1.0, 0.18, 1.5, 1.04, 0.45, "greyPlastic"); G.box(1.22, 1.04, 0.25, 1.33, 1.2, 0.35, "greyPlastic"); G.box(0.78, 1.2, 0.18, 1.78, 1.88, 0.32, "greyPlastic"); G.box(0.84, 1.26, 0.32, 1.72, 1.82, 0.33, { s: sk, all: null });
      G.box(0.85, 1.0, 0.5, 1.7, 1.03, 0.74, { top: "keyboard", all: "greyPlastic" });
      G.box(2.35, 1.0, 0.3, 2.6, 1.08, 0.6, "greyPlastic"); G.box(2.44, 1.08, 0.38, 2.52, 1.5, 0.46, "steel"); G.box(2.38, 1.42, 0.36, 2.58, 1.56, 0.62, "greyPlastic");
      R.act([0.3, 0, 2.3, 1.8], { label: "Use", lines: ["The professor's PC. Notes on every creature he's met, and a lot of unread email."] });
      R.item("Teal desk run", "Teal cupboards and drawers, white tops, the PC and a microscope."); }
    officeChair(R, 1.4, 1.75, "n", "blueFabric");
    incubator(R, 1.8, 4.5);
    labMachine(R, 3.9, 3, R.screen("labScreen", 44, 22, drawLab, 0.95), { name: "The professor's machine", lines: ["Numbers scroll by. A creature's heartbeat, its height and its weight.", "It's reading something new."] });
    windowN(R, 7.5, 1.7, 2.2, 1.5, { lines: ["Out of the window: Pallet Town, and the sea past the houses."] });
    wallMap(R, "n", 10.2, 1.45, 3.4, { frameMat: "silver", lines: ["A map of the whole region: every town, every route.", "The professor's pinned a note to it: THREE NEW TRAINERS, THREE NEW PARTNERS."] });
    healer(R, 14.6, 3);
    ballTable(R, 11.6, 4.3, 14.8, 5.6);
    // Two rows of white shelves, back to back across the room, as in the games.
    for (const X of [0.4, 2.4, 4.4]) labShelf(R, X, 9.6, "s", 2, 3.2, { lines: ["Books on creatures: their habits, their homes, how they change as they grow.", "It's all a bit much to take in."] });
    for (const X of [11.6, 13.6, 15.6]) labShelf(R, X, 9.6, "s", 2, 3.2, { rows: ["binders", "booksFull", "storeBoxes", "booksFull"], lines: ["Research papers. One of them is called ON THE KEEPING OF SMALL CREATURES IN BALLS."] });
    // A second row on the right, below the first, with aides either side.
    for (const X of [11.6, 13.6, 15.6]) labShelf(R, X, 14.2, "s", 2, 3.2, { rows: ["booksFull", "storeBoxes", "booksFull", "binders"], lines: ["Field notes, filed by route: ROUTE 1, ROUTE 2, VIRIDIAN FOREST..."] });
    // The aide's corner, bottom left: a desk against the west wall, his chair, and a board on a stand behind him.
    deskPC(R, 0, 15.3, "e", R.screen("pcAide", 40, 26, drawLab, 0.9), { top: "lab", lines: ["The aide's PC: a spreadsheet of heights and weights, half filled in."] });
    officeChair(R, 1.6, 14.15, "w", "blueFabric");
    boardStand(R, 2.9, 12.0, 2.6, "s", ["NOTES, in marker: three lines of questions about sleep, food and growing.", "And a doodle of a very round, very green creature."]);
    plant(R, 1.0, 18.9, 1.2); plant(R, 17.0, 18.9, 1.2); plant(R, 17.1, 8.6, 1.1);
    doormat(R, LAB_DOOR[0], LAB_DOOR[1], { to: "town", spawn: FROM_LAB, mat: "matLab" });
    R.npcs.push({ name: "Prof. Oak", x: 9.6, z: 3.7, yaw: FACE.s, look: { skin: "oak" },
      talk: f => [
        { text: "Ah, there you are! I've been waiting." },
        f ? { text: "And you've brought a friend. Good, good. A creature that trusts you is a fine start." } : { text: "Those three balls on the table: each holds a creature. One of them is yours." },
        { text: "Take your time choosing. It's a partner for life." }
      ] });
    R.npcs.push({ name: "Blue", x: 13.2, z: 6.7, yaw: FACE.n, look: { skin: "blue" },
      talk: () => [{ text: "Gramps said I could pick first. Well, after you. He made me promise." }, { text: "Hurry up, will you? I've got places to be." }] });
    R.npcs.push({ name: "Aide", x: 5.4, z: 1.9, yaw: FACE.n, look: { skin: "scientist" },
      talk: () => [{ text: "I'm the professor's aide. These readings won't record themselves!" }] });
    R.npcs.push({ name: "Aide", x: 1.6, z: 14.15, yaw: FACE.w, seated: true, look: { skin: "scientist" },
      talk: () => [{ text: "The professor's writing a book. Every creature in the region, one page each." }, { text: "He's only up to the second page." }] });
    R.npcs.push({ name: "Aide", x: 15.4, z: 12.5, yaw: FACE.n, look: { skin: "scientist_f" },
      talk: () => [{ text: "Have you got a partner yet? The professor gives every new trainer one of their own." }, { text: "Look after it, and it'll look after you." }] });
    R.npcs.push({ name: "Aide", x: 12.3, z: 12.5, yaw: FACE.n, look: { skin: "scientist" },
      talk: () => [{ text: "Every book on this shelf, I've read twice. The professor's read them three times." }] });
    R.npcs.push({ name: "Aide", x: 14.6, z: 16.4, yaw: FACE.n, look: { skin: "scientist_f" },
      talk: () => [{ text: "I'm filing the notes from ROUTE 1. Bird POKéMON, mostly. And a lot of RATTATA." }] });
    R.spawn = LAB_IN;
    return R;
  },

  center() {
    // A Pokémon Center as the games have one, tightened up: the back corners cut on the slant (as the building is
    // outside); the long red counter with the nurse, her healing machine behind on the left and the PC and a screen
    // on the right, a till on the counter; stairs up either side to the 2F (roped off); a glass-topped table and
    // armchairs each side; the world map and bookshelves in the corners; the storage PC; a red runner to the door.
    // No sign inside.
    const R = new Room("center", { name: "Pokémon Center", floor: "Viridian City", W: 20, D: 16, outsideSize: "13 by 9" });
    shell(R, { paper: "paperCenter", floor: "mcCenterCheck", door: CENTER_DOOR, skirt: "redPlastic" });
    band(R, 1.05, "redPlastic");
    chamfer(R, 2, { paper: "paperCenter", skirt: "redPlastic", band: [1.05, "redPlastic"] });
    const G = R.geo;
    // The counter: its front, its white top, and its ends back to the wall so no one walks round behind.
    G.box(5, 0, 4.4, 15, 1.05, 5.4, { all: "counterRed", top: "lab" });
    G.box(4.85, 1.05, 4.3, 15.15, 1.17, 5.55, "lab");
    G.box(4.85, 0, 0, 5.35, 1.05, 4.4, { all: "counterRed", top: "lab" }); G.box(14.65, 0, 0, 15.15, 1.05, 4.4, { all: "counterRed", top: "lab" });
    R.solid(4.85, 0, 15.15, 5.55);
    G.box(9.6, 1.17, 4.6, 10.4, 1.3, 5.2, "greyPlastic"); G.box(9.9, 1.3, 4.75, 10.1, 1.36, 4.95, "gold");
    till(R, 13.6, 4.95, 1.17, "s");
    R.act([5, 5.4, 15, 6.6], { label: "Talk", lines: ["The counter. The nurse will look after your POKéMON."] });
    R.item("The counter", "Long and red, a white top and stripe; a little bell in the middle and a till at the right.");
    // Behind it: the healing machine on the left, the PC and a big screen on the right.
    healer(R, 6.0, 3, { empty: true });
    deskPC(R, 12.2, 0, "s", R.screen("pcNurse", 40, 26, drawPC, 0.9), { top: "lab", lines: ["The nurse's PC: today's patients, and a note to order more POTIONS."] });
    officeChair(R, 13.35, 1.6, "n", "redFabric");
    wallScreen(R, 11.9, 2.05, 2.9, 1.3, R.screen("centerAd", 40, 18, drawCenterAd, 1), ["A heart beating over a ball. WE HOPE TO SEE YOU AGAIN!"]);
    // The corners: the world map and a bookshelf on the left, the storage PC on the right.
    wallMap(R, "n", 2.2, 1.35, 2.5, { lines: ["A map of the whole region. VIRIDIAN CITY is marked with a red pin.", "In game, this opens the TOWN MAP."] });
    bookshelf(R, 0, 4.6, "e", 2.2, 2.8, { wood: "pale", trinkets: { 1: "plant" }, lines: ["Books on caring for POKéMON: food, rest, and what to do about a cold."] });
    deskPC(R, 15.7, 0, "s", R.screen("pcCenter", 40, 26, drawPC, 0.9), { top: "lab", pc: true, lines: ["The POKéMON STORAGE SYSTEM."] });
    officeChair(R, 16.85, 1.6, "n", "redFabric");
    bookshelf(R, 20, 2.4, "w", 2.2, 2.8, { wood: "pale", trinkets: { 2: "ball" }, lines: ["A shelf of guides: TYPES and how they match up, and one called THE TRAINER'S HANDBOOK."] });
    // Stairs either side, up to the 2F (not open).
    sideStairs(R, "w", 11.4, { frame: "redPlastic" }); sideStairs(R, "e", 11.4, { frame: "redPlastic" });
    // The waiting areas: a glass-topped table and two armchairs each side.
    glassTable(R, 4.3, 8.4, 5.9, 9.9);
    armchair(R, 3.45, 9.15, "e", "yellowFabric"); armchair(R, 6.75, 9.15, "w", "blueFabric");
    glassTable(R, 14.1, 8.4, 15.7, 9.9, { lines: ["A glass table. Someone's left a puzzle book open: a crossword about BUG POKéMON."] });
    armchair(R, 13.25, 9.15, "e", "blueFabric"); armchair(R, 16.55, 9.15, "w", "yellowFabric");
    // Low bookshelves by the door, and plants.
    bookshelf(R, 0, 14.0, "e", 2, 1.6, { wood: "pale", lines: ["Picture books for little trainers waiting with their mums."] });
    bookshelf(R, 20, 12.0, "w", 2, 1.6, { wood: "pale", lines: ["Leaflets: RESTING IS GOOD FOR POKéMON TOO."] });
    plant(R, 1.0, 14.9, 1.1, { lines: ["A big leafy plant. Its leaves are very shiny."] }); plant(R, 19.0, 14.9, 1.1, { lines: ["A big leafy plant."] });
    plant(R, 4.2, 6.2, 0.95, { lines: ["A potted palm."] }); plant(R, 15.8, 6.2, 0.95, { lines: ["A potted palm."] });
    rug(R, 8, 5.6, 12, 14.8, "centerRun");
    doormat(R, CENTER_DOOR[0], CENTER_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matCenter" });
    R.npcs.push({ name: "Nurse", role: "nurse", key: "viridian_nurse", x: 10.0, z: 3.3, yaw: FACE.s, look: { skin: "nurse" },
      talk: () => [{ text: "Hello, and welcome to the POKéMON CENTER!" }, { text: "We heal tired POKéMON back to full health. Shall we heal yours?" }] });
    R.npcs.push({ name: "Gentleman", x: 15.0, z: 11.6, yaw: FACE.n, look: { skin: "gentleman" },
      talk: () => [{ text: "You can use that PC in the corner." }, { text: "The receptionist told me. So kind!" }] });
    R.npcs.push({ name: "Youngster", x: 5.0, z: 11.6, yaw: FACE.n, look: { skin: "youngster" },
      talk: () => [{ text: "There's a POKéMON CENTER in every town ahead." }, { text: "They don't charge any money either!" }] });
    R.spawn = CENTER_IN;
    return R;
  },

  mart() {
    // A Poké Mart as the newer games have one: the back corners cut on the slant; the blue counter on the left as you
    // come in, the clerk behind it with the till and a shelf of stock at his back; drinks coolers and a screen on the
    // back wall; chest freezers down the right; a row of shelves and a glass display case in the middle; a blue runner
    // from the door. No sign inside.
    const R = new Room("mart", { name: "Poké Mart", floor: "Viridian City", W: 16, D: 14, outsideSize: "13 by 9" });
    shell(R, { paper: "paperMart", floor: "mcMintCheck", door: MART_DOOR, skirt: "blueTrim" });
    band(R, 1.05, "blueTrim");
    chamfer(R, 2, { paper: "paperMart", skirt: "blueTrim", band: [1.05, "blueTrim"] });
    const G = R.geo;
    G.box(3.2, 0, 6.6, 4.2, 1.05, 12.2, { all: "counterBlue", top: "lab" });
    G.box(0, 0, 12.2, 4.2, 1.05, 13.0, { all: "counterBlue", top: "lab" });
    G.box(3.1, 1.05, 6.5, 4.3, 1.15, 12.3, "lab");
    R.solid(0, 6.5, 4.3, 13.0); R.solid(0, 6.5, 3.2, 6.6);
    // The till and a basket of balls on the counter.
    till(R, 3.7, 9.45, 1.15, "e");
    for (const [bx, bz] of [[3.55, 7.3], [3.85, 7.5], [3.6, 7.75]]) G.box(bx - 0.13, 1.15, bz - 0.13, bx + 0.13, 1.41, bz + 0.13, { all: "ball", top: "ballTop" });
    R.act([3.2, 6.6, 5.2, 12.2], { label: "Talk", lines: ["The counter. The clerk sells everything on the shelves."] });
    R.item("The counter", "Blue, a white top and stripe, the till and a little basket of balls.");
    // Stock at the clerk's back.
    martShelf(R, 0, 11.8, "e", 4.4, 2.4, { seed: 1, depth: 0.6, lines: ["The stock behind the counter: boxes of POKé BALLS, POTIONS by the dozen."] });
    // The back wall: drinks coolers, the screen, tall shelves.
    cooler(R, 2.3, 0, "s", 2); cooler(R, 4.3, 0, "s", 2);
    wallScreen(R, 6.9, 1.6, 2.6, 1.45, R.screen("martAd", 40, 22, drawMartAd, 1), ["An advert: a ball bouncing past a POTION and a can of REPEL. NEW STOCK IN EVERY WEEK!"]);
    G.box(6.6, 0, 0, 9.8, 0.9, 0.9, { all: "shelfWhite", front: "cupboardWhite" }); G.box(6.6, 0.9, 0, 9.8, 0.98, 0.9, "lab"); R.solid(6.6, 0, 9.8, 0.9);
    for (const [bx, bz] of [[7.2, 0.45], [7.6, 0.4], [8.6, 0.5], [9.1, 0.42]]) G.box(bx - 0.14, 0.98, bz - 0.14, bx + 0.14, 1.26, bz + 0.14, { all: "ball", top: "ballTop" });
    martShelf(R, 10.2, 0, "s", 3.8, 2.6, { seed: 0 });
    // Down the right: two chest freezers, then shelves of things for the road.
    chestFreezer(R, 16, 2.6, "w", 2); chestFreezer(R, 16, 4.6, "w", 2);
    martShelf(R, 16, 7.2, "w", 4, 2.4, { seed: 2, lines: ["REPELS, ESCAPE ROPES and things for camping. Most of them aren't in stock yet."] });
    // The middle: a row of shelves facing the door, and a glass display case in front of it.
    martShelf(R, 6.4, 3.4, "s", 5.4, 2.0, { seed: 1 });
    displayCase(R, 6.8, 7.0, 11.4, 8.0, ["Under the glass: POKé BALLS and GREAT BALLS laid out in rows, and a box of POTIONS."]);
    rug(R, 6, 9, 10, 12.8, "martRun");
    plant(R, 15.0, 12.9, 1.1, { lines: ["A plant by the door. It looks well looked after."] });
    plant(R, 12.6, 12.9, 0.9, { lines: ["A little potted palm."] });
    doormat(R, MART_DOOR[0], MART_DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s }, mat: "matMart" });
    R.npcs.push({ name: "Clerk", role: "clerk", key: "viridian_city", x: 1.7, z: 9.4, yaw: FACE.e, look: { skin: "clerk" },
      talk: () => [{ text: "Hi there! May I help you?" }] });
    R.npcs.push({ name: "Youngster", x: 9.6, z: 6.0, yaw: FACE.n, look: { skin: "youngster" },
      talk: () => [{ text: "No! POTIONs are all sold out." }] });
    R.npcs.push({ name: "Lady", x: 13.8, z: 9.2, yaw: FACE.e, look: { skin: "middle_aged_woman" },
      talk: () => [{ text: "This shop sells many ANTIDOTEs." }] });
    R.spawn = MART_IN;
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
