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
  { id: "town", name: "Pallet Town", floor: "Outside", group: "Outside" },
];
const FROM_TOWN = { x: 6.0, z: 12.9, yaw: FACE.n };
const LAB_DOOR = [7.5, 10.5], LAB_IN = { x: 9.0, z: 18.9, yaw: FACE.n }, FROM_LAB = { x: 25.0, z: 23.1, yaw: FACE.s };
const STAIRS_FOOT = { x: 11.1, z: 1.1, yaw: FACE.s };
const STAIRS_TOP = { x: 15.5, z: 2.9, yaw: FACE.s };

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
function healer(room, X, w) {
  const F = room.F(X, 0, "s");
  F.box(0, 0, 0, w, 1.1, 1.0, { all: "cupboardWhite", top: "lab", back: null, bottom: null });
  F.box(0.2, 1.1, 0.15, w - 0.2, 1.2, 0.85, "greyPlastic");
  for (let i = 0; i < 6; i++) { const lx = 0.45 + (i % 3) * ((w - 0.9) / 2), lz = 0.35 + Math.floor(i / 3) * 0.3; F.box(lx - 0.1, 1.2, lz - 0.1, lx + 0.1, 1.38, lz + 0.1, { all: "ball", top: "ballTop" }); }
  F.box(0, 1.1, 0, w, 2.4, 0.15, { front: "labPanel", all: "greyPlastic" });
  F.solid(0, 0, w, 1.0);
  room.act(F.rect(0, 0.5, w, 1.5), { label: "Look", lines: ["A machine for resting creatures, six slots on its top.", "It's warm. Someone was using it this morning."] });
  room.item("Resting machine", "Six slots for balls on its top, a panel of lights behind.");
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
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 6.9, z: 10.0, yaw: FACE.s } });
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
    R.geo.box(6.6, 0.86, 6.6, 7.8, 0.89, 7.5, { top: "map", all: "linen" });
    R.act([6.4, 6.2, 8.0, 8.2], { label: "Look", lines: ["It's a Town Map. Daisy's keeping it safe for you."] });
    chair(R, 5.2, 7.2, "e", { wood: "walnut" }); chair(R, 7.6, 5.65, "s", { wood: "walnut" }); chair(R, 7.6, 8.75, "n", { wood: "walnut" });
    plates(R, [[8.6, 7.2]], 0.86);
    sofa(R, 16, 4.6, "w", 3, { fabric: "blueFabric", cushion: "linen", pillow: "cushion", pillow2: "cushion", lines: ["A blue sofa. There's a game controller down the side of the cushion."] });
    framed(R, 6.0, 1.6, 2.4, 1.7, "paintingHills", "w", { act: { label: "Look", lines: ["A painting of the hills north of town, with a little red-roofed house."] } });
    plant(R, 1.0, 12.9, 1.15); plant(R, 15.0, 12.9, 1.15);
    floorLamp(R, 15.2, 8.4);
    doormat(R, DOOR[0], DOOR[1], { to: "town", spawn: { x: 24.9, z: 10.0, yaw: FACE.s } });
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
    whiteboard(R, 10.2, 1.4, 3.4, 1.9, "boardMap", ["A map of Kanto, the towns marked in red.", "Underneath, in the professor's writing: THREE NEW TRAINERS, THREE NEW PARTNERS."]);
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
    doormat(R, LAB_DOOR[0], LAB_DOOR[1], { to: "town", spawn: FROM_LAB });
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
