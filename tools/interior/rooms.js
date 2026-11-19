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
  { id: "town", name: "Pallet Town", floor: "Outside", group: "Outside" },
];
const FROM_TOWN = { x: 6.0, z: 12.9, yaw: FACE.n };
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
function bunting(room, x0, x1, y) { room.geo.box(x0, y, 0.04, x1, y + 0.45, 0.05, { s: "bunting", all: null }); room.item("Bunting", "Pennants strung along the wall."); }

const BUILD = {
  house1() {
    const R = new Room("house1", { name: "Your house", floor: "Downstairs", paper: "paperMum", outsideSize: "8 by 6" });
    shell(R, { paper: "paperMum", floor: "oakFloor", zones: [[0, 0, 7.4, 5.2, "checker"]], door: DOOR });
    kitchen(R, 0.2, "s");
    clock(R, 6.85, 2.55);
    tvSet(R, 7.75, 0, "s", R.screen("tvFilm", 40, 24, drawFilm, 0.9), { w: 3.2, speakers: true,
      lines: ["There's a film on. Four kids are setting off down a railway line.", "Maybe it's time you set off too."] });
    R.item("Television", "On a walnut stand with speakers; the film on it moves.");
    stairsUp(R, 12, 0, 2, { to: "house2", spawn: STAIRS_TOP });
    rug(R, 7.6, 3.1, 12.4, 8.7, "rugMum");
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
      look: { skin: "#E9B48C", hair: "#7A4A2E", hairStyle: "long", shirt: "#E77D6E", legs: "#4C6FA8", shoes: "#5A3D27" },
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
    shell(R, { paper: "paperRoom", floor: "honeyFloor", floorRects: [[0, 0, 12, 14], [12, 2, 16, 14]] });
    stairsDown(R, 12, 0, 2, { to: "house1", spawn: STAIRS_FOOT, paper: "paperRoom" });
    deskPC(R, 0.4, 0, "s", R.screen("pcScreen", 32, 24, drawPC, 1), { pc: true });
    officeChair(R, 1.55, 1.6, "n");
    whiteboard(R, 3.2, 1.45, 2.4, 1.2, "whiteboard", ["The whiteboard says:", "TO DO. 1, wake up (crossed out). 2, see the Prof. 3, become champion!"]);
    windowN(R, 6.5, 1.5, 2.2, 2.0, { curtains: "curtainBlue" });
    bookshelf(R, 9.3, 0, "s", 2, 2.6, { wood: "pale", trinkets: { 0: "ball", 2: "plant" }, lines: ["Comics, a field guide to Kanto's creatures, and a very old ball."], name: "Shelves", note: "Pale-wood shelves with books, a ball and a little plant." });
    bed(R, 0, 10.6, "e", { w: 1.8, l: 2.9 });
    nightstand(R, 0, 8.6, "e");
    rug(R, 5.0, 6.1, 9.4, 9.8, "rugRoom");
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
    shell(R, { paper: "paperRival", floor: "paleFloor", zones: [[0, 0, 5.4, 4.6, "tileBlue"]], door: DOOR });
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
    rug(R, 4.6, 5.0, 10.6, 9.4, "rugRival");
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
      look: { skin: "#F0C29C", hair: "#B77A45", hairStyle: "long", shirt: "#8CC6B5", legs: "#5A4A3E", shoes: "#3B2A1C" },
      talk: () => [{ text: "Hi! My brother's already gone to Grandpa's lab." }, { text: "He always has to be first. You'd better catch him up!" }] });
    R.spawn = FROM_TOWN;
    return R;
  },

  rival2() {
    const R = new Room("rival2", { name: "Rival's room", floor: "Upstairs", paper: "paperRivalRoom" });
    shell(R, { paper: "paperRivalRoom", floor: "walnutFloor", floorRects: [[0, 0, 12, 14], [12, 2, 16, 14]], skirt: "walnut" });
    stairsDown(R, 12, 0, 2, { to: "rival1", spawn: STAIRS_FOOT, paper: "paperRivalRoom" });
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
    rug(R, 5.0, 6.0, 10.0, 10.2, "rugRival");
    bagOnFloor(R, 9.0, 12.4, ["A sports bag, half unpacked. Running shoes, a towel, more trophies."]);
    plant(R, 1.0, 12.8, 1.0, { lines: ["A cactus. It looks like it gets on with him."] });
    beanBag(R, 12.6, 6.2, "redFabric"); dumbbells(R, 3.4, 12.6); bunting(R, 0.3, 6.2, 3.0);

    R.spawn = STAIRS_TOP;
    return R;
  },

  town() {
    const R = new Room("town", { name: "Pallet Town", floor: "Outside", W: 34, D: 24, H: 6, outside: true });
    ground(R, "grass");
    patch(R, 15, 0.4, 19, 23.6, "path"); patch(R, 4, 11, 31, 13, "path");
    patch(R, 6.0, 9.2, 7.8, 11, "path"); patch(R, 24.0, 9.2, 25.8, 11, "path"); patch(R, 23.5, 13, 26.5, 15.6, "path");
    house(R, 4, 3, { roof: "roofRed", shutter: "redFabric", to: "house1", spawn: FROM_TOWN, signKey: "signYou", signText: "YOUR HOUSE", signRead: "YOUR HOUSE.", mail: "A letter for Mum, and a flyer about a new shop in Viridian.", name: "Your house (outside)" });
    house(R, 22, 3, { roof: "roofBlue", shutter: "blueFabric", to: "rival1", spawn: FROM_TOWN, signKey: "signRival", signText: "RIVAL'S HOUSE", signRead: "RIVAL'S HOUSE.", mail: "A parcel addressed to Daisy, and a fan letter for her brother.", name: "Rival's house (outside)" });
    lab(R, 19, 15.6);
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
