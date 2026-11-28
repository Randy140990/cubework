// ---------- 0.75.0 (John's 28 Nov 11:27): NEW BARK TOWN's insides ----------
// "next thing to do is like Johto interiors, obviously Professor Elms. And then there's obviously the equivalency of
// the player house ... You've got the player house, then you've got the rival house, same as always ... this is from I
// guess Heart's Gold ... we've already got loads of interiors created, so you can see about like sizing and making
// them feel more full without trying to go crazy on the space."
// PROF. ELM's LAB laid out as HeartGold's (John's PokéMaps picture) and ELM's HOUSE as HeartGold's, the LAB's upstairs
// (in up the steps outside its east side: John's 11:45). Your house (downstairs and your room) and your rival's house
// (downstairs and their room) laid out as HeartGold's, both with their stairs at the back on the left, and (John's 11:45:
// "should be different from the ones in Palette. So restyle.") built in JOHTO's own looks, nothing like Pallet's. Their
// people, and the words on what you look at, are HeartGold's (pret/pokeheartgold's T20R0101 to T20R0402) or Crystal's.
// In from John's New Bark doors (tools/interior/export/tomod.py).
(function () {
  const G_ = "New Bark Town";
  PLACES.push({ id: "elms_lab", name: "Prof. Elm's Lab", floor: G_, group: G_ });
  PLACES.push({ id: "elms_house", name: "Prof. Elm's House", floor: G_, group: G_ });
  PLACES.push({ id: "nb_house1", name: "Your house", floor: "Downstairs", group: G_ });
  PLACES.push({ id: "nb_house2", name: "Your room", floor: "Upstairs", group: G_ });
  PLACES.push({ id: "nb_rival1", name: "Rival's house", floor: "Downstairs", group: G_ });
  PLACES.push({ id: "nb_rival2", name: "Rival's room", floor: "Upstairs", group: G_ });
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const fill = (P, r, c, k) => { for (let y = 0; y < P.h; y++) for (let x = 0; x < P.w; x++) P.set(x, y, shadec(hx(c), (r() - 0.5) * k)); };

  // ----- The lab's: HeartGold's mint tiles with a fine seam, the darker lines laid across them, the stepping
  // stones down the middle, its walls (mint over a white band), the wooden boards at the front -----
  reg("elmTile", 16, 16, (P, r) => { fill(P, r, "#A8DDCB", 0.02); P.rect(0, 0, 16, 1, hx("#90CBB7")); P.rect(0, 0, 1, 16, hx("#90CBB7")); P.rect(1, 1, 15, 1, hx("#BCE8D8")); });
  reg("elmLine", 16, 16, (P, r) => { fill(P, r, "#6FB79F", 0.02); });
  reg("elmStone", 16, 16, (P, r) => { fill(P, r, "#7FC4AD", 0.03); P.rect(0, 0, 16, 1, hx("#9AD6C1")); P.rect(0, 15, 16, 1, hx("#5E9F89")); });
  reg("elmWall", 16, 16, (P, r) => { fill(P, r, "#CDEBDF", 0.008); P.rect(0, 15, 16, 1, hx("#A9D3C3")); P.rect(0, 9, 16, 1, hx("#F4FBF8")); });
  reg("elmSkirt", 16, 16, (P, r) => { fill(P, r, "#4F9C86", 0.02); P.rect(0, 0, 16, 2, hx("#78C2AC")); });
  reg("elmWood", 16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx(((x >> 3) + (y >> 3)) % 2 ? "#E8C690" : "#F0D3A0"), (r() - 0.5) * 0.03)); });
  // The shelves' blue frames and white faces, the healing machine's panel, the big machine's grey and orange.
  reg("elmBlue", 16, 16, (P, r) => { fill(P, r, "#4C78C8", 0.02); P.rect(0, 0, 16, 1, hx("#6E96DE")); P.rect(0, 15, 16, 1, hx("#3A5FA6")); });
  reg("elmWhite", 16, 16, (P, r) => { fill(P, r, "#F2F5F7", 0.006); P.rect(0, 15, 16, 1, hx("#D5DCE2")); });
  reg("elmHeal", 16, 32, (P, r) => {
    fill(P, r, "#F2F5F7", 0.006); P.rect(2, 3, 12, 20, hx("#3E66B8"));
    for (let y = 5; y < 21; y += 4) for (let x = 3; x < 13; x += 4) P.rect(x, y, 3, 3, hx("#6E9AE6"));
    P.rect(4, 25, 8, 2, hx("#F2A436")); P.rect(6, 1, 4, 1, hx("#F2A436"));
  }, { fit: true, glow: 0.25 });
  reg("elmGear", 16, 16, (P, r) => { fill(P, r, "#4A5058", 0.03); P.rect(0, 0, 16, 1, hx("#6A727C")); });
  reg("elmCore", 16, 16, (P, r) => { fill(P, r, "#E8642E", 0.03); P.rect(4, 4, 8, 8, hx("#F2894E")); }, { glow: 0.2 });
  reg("elmDome", 16, 16, (P, r) => { fill(P, r, "#D9442E", 0.03); P.rect(3, 3, 5, 3, hx("#F27A60")); });
  reg("potPink", 16, 16, (P, r) => { fill(P, r, "#E487B4", 0.02); P.rect(0, 0, 16, 2, hx("#F2A8CB")); P.rect(0, 14, 16, 2, hx("#C46A96")); });
  // ELM's house: pale blue paper with its white diamonds, the blue counter.
  reg("elmPaper", 16, 16, (P, r) => {
    fill(P, r, "#CFE9F4", 0.006);
    for (let i = 0; i < 4; i++) { P.set(8 + i, 4 + i, hx("#EEF8FC")); P.set(8 - i, 4 + i, hx("#EEF8FC")); P.set(8 + i, 12 - i, hx("#EEF8FC")); P.set(8 - i, 12 - i, hx("#EEF8FC")); }
    P.rect(7, 7, 2, 2, hx("#B7DCEB")); P.rect(0, 15, 16, 1, hx("#B7DCEB"));
  });
  reg("elmCounter", 16, 16, (P, r) => { fill(P, r, "#5B8EE6", 0.015); P.rect(0, 0, 16, 2, hx("#82AEF2")); P.rect(0, 14, 16, 2, hx("#4673C2")); });
  // ----- (11:45, John: "your house and rival house in Bark for Johto should be different from the ones in Palette. So
  // restyle.") Your house, as JOHTO's old towns build: plaster between dark timber posts over a cedar wainscot, wide
  // honey boards, a raised tatami corner with a quilted low table, a stone step at the door. Your rival's, a sage
  // cottage over white boards, terracotta tiles, a teal kitchen, a step chest and a hanging scroll. -----
  reg("nbPlaster", 16, 16, (P, r) => { fill(P, r, "#F2E8D6", 0.012); });
  reg("nbPlasterSage", 16, 16, (P, r) => { fill(P, r, "#D5E0C6", 0.012); });
  reg("nbPlasterPeach", 16, 16, (P, r) => { fill(P, r, "#F5DCCB", 0.012); });
  reg("nbCedar", 16, 16, (P, r) => { fill(P, r, "#A9673F", 0.03); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#7E4A2C")); P.rect(0, 0, 16, 1, hx("#C17E52")); });
  reg("nbWainWhite", 16, 16, (P, r) => { fill(P, r, "#F4F1EA", 0.008); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#D9D3C6")); });
  reg("nbBeam", 16, 16, (P, r) => { fill(P, r, "#5C3B24", 0.035); for (let y = 2; y < 16; y += 5) P.rect(0, y, 16, 1, hx("#4C301D")); });
  reg("nbBoards", 32, 32, (P, r) => planks(P, r, hx("#B7814E"), hx("#CF9C68"), hx("#8E5E37"), hx("#5E3C22"), 8, 32), { per: 2 });
  reg("nbBoardsLight", 32, 32, (P, r) => planks(P, r, hx("#D6AE7B"), hx("#E8C796"), hx("#B38A58"), hx("#8A6440"), 8, 32), { per: 2 });
  reg("nbBoardsAsh", 32, 32, (P, r) => planks(P, r, hx("#E3CCA4"), hx("#F1DFBE"), hx("#C3AA80"), hx("#9C8460"), 8, 32), { per: 2 });
  const squares = (P, r, a, b, grout, k) => {
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const base = hx((tx + ty) % 2 ? a : b);
      for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        let c = shadec(base, (r() - 0.5) * k);
        if (x === 0 || y === 0) c = hx(grout); else if (x === 1 || y === 1) c = shadec(base, 0.08);
        P.set(tx * 16 + x, ty * 16 + y, c);
      }
    }
  };
  reg("nbTile", 32, 32, (P, r) => squares(P, r, "#C99272", "#BE8566", "#E8DACB", 0.05), { per: 2 });
  reg("nbGenkan", 32, 32, (P, r) => squares(P, r, "#8E9194", "#81858A", "#5E6266", 0.06), { per: 2 });
  reg("nbTatami", 16, 16, (P, r) => { fill(P, r, "#B9C477", 0.03); for (let x = 0; x < 16; x += 2) P.rect(x, 0, 1, 16, hx("#A9B566")); });
  reg("nbTatamiEdge", 16, 16, (P, r) => { fill(P, r, "#2E3B2C", 0.03); P.rect(7, 0, 2, 16, hx("#C9A24A")); });
  reg("nbKotatsu", 16, 16, (P, r) => {
    fill(P, r, "#2F4A7C", 0.02);
    for (let y = 2; y < 16; y += 6) for (let x = 2; x < 16; x += 6) { P.rect(x, y + 1, 3, 1, hx("#E8E2D2")); P.rect(x + 1, y, 1, 3, hx("#E8E2D2")); }
  });
  reg("nbZabuton", 16, 16, (P, r) => fabric(P, r, hx("#D69A35")));
  reg("nbZabutonRed", 16, 16, (P, r) => fabric(P, r, hx("#C2453F")));
  reg("nbSofaOlive", 16, 16, (P, r) => fabric(P, r, hx("#7E8C4E")));
  reg("nbMikan", 16, 16, (P, r) => { fill(P, r, "#F28A1E", 0.04); P.rect(6, 0, 3, 2, hx("#4E8A35")); });
  const cabDoor = (P, r, body, panel, lit, edge, pull) => {
    fill(P, r, body, 0.02);
    P.rect(0, 0, 16, 1, hx(edge)); P.rect(0, 15, 16, 1, hx(edge)); P.rect(0, 0, 1, 16, hx(edge)); P.rect(15, 0, 1, 16, hx(edge));
    P.rect(3, 4, 10, 9, hx(panel)); P.rect(3, 4, 10, 1, hx(lit)); P.rect(7, 2, 2, 1, hx(pull));
  };
  reg("nbCab", 16, 16, (P, r) => cabDoor(P, r, "#9C5F3A", "#B06E46", "#C9885C", "#6E4227", "#2A2A2A"), { fit: true });
  reg("nbCabTeal", 16, 16, (P, r) => cabDoor(P, r, "#4F8C88", "#5FA29D", "#79B9B3", "#3D706C", "#E2D3A8"), { fit: true });
  reg("nbTop", 16, 16, (P, r) => { fill(P, r, "#E6E1D6", 0.015); });
  const splash = (P, r, base, grout, dot) => {
    fill(P, r, base, 0.01);
    for (let i = 0; i < 16; i += 4) { P.rect(i, 0, 1, 16, hx(grout)); P.rect(0, i, 16, 1, hx(grout)); }
    for (let y = 2; y < 16; y += 8) for (let x = 2; x < 16; x += 8) P.rect(x, y, 2, 2, hx(dot));
  };
  reg("nbSplash", 16, 16, (P, r) => splash(P, r, "#F3F1EA", "#D6D2C8", "#35558A"));
  reg("nbSplashSage", 16, 16, (P, r) => splash(P, r, "#E4EBDB", "#C5CFBA", "#6E8F5E"));
  const fridge = (P, r, body, line, lit) => {
    fill(P, r, body, 0.008); P.rect(0, 0, 16, 1, hx(lit)); P.rect(0, 11, 16, 1, hx(line)); P.rect(0, 31, 16, 1, hx(line));
    P.rect(12, 3, 1, 6, hx("#9AA3AB")); P.rect(12, 14, 1, 10, hx("#9AA3AB"));
    P.rect(3, 16, 3, 3, hx("#E0614F")); P.rect(7, 18, 2, 2, hx("#3E7BC8")); P.rect(3, 20, 5, 4, hx("#FFFFFF")); P.rect(4, 21, 3, 1, hx("#7A8A9A"));
  };
  reg("nbFridge", 16, 32, (P, r) => fridge(P, r, "#F1EDE3", "#C9C3B6", "#FFFFFF"), { fit: true });
  reg("nbFridgeMint", 16, 32, (P, r) => fridge(P, r, "#CFE6DA", "#A9C9B8", "#E6F4EC"), { fit: true });
  // Your house's picture: New Bark's wind turbines on the hill over the sea. Your rival's scroll: the same, in ink.
  const hill = x => 13 + Math.round(2.5 * Math.cos((x - 10) / 7));
  reg("nbPicTurbines", 32, 20, (P, r) => {
    for (let y = 0; y < 20; y++) for (let x = 0; x < 32; x++) P.set(x, y, mixc(hx("#C4E5F6"), hx("#86C1E8"), y / 20));
    for (let x = 0; x < 32; x++) for (let y = hill(x); y < 20; y++) P.set(x, y, shadec(hx("#6FB257"), -(y - hill(x)) * 0.025));
    for (const [tx, ty] of [[8, 5], [17, 4], [25, 6]]) {
      P.rect(tx, ty, 1, hill(tx) - ty, hx("#F4F4F4"));
      P.rect(tx - 3, ty, 3, 1, hx("#FFFFFF")); P.rect(tx, ty - 3, 1, 3, hx("#FFFFFF")); P.rect(tx + 1, ty + 1, 2, 1, hx("#FFFFFF")); P.set(tx + 3, ty + 2, hx("#FFFFFF"));
    }
  }, { fit: true });
  reg("nbScroll", 12, 32, (P, r) => {
    fill(P, r, "#7E9470", 0.02);
    for (let y = 4; y < 28; y++) for (let x = 2; x < 10; x++) P.set(x, y, shadec(hx("#F0E8D2"), (r() - 0.5) * 0.02));
    for (let x = 2; x < 10; x++) { const h = 22 + Math.round(1.5 * Math.sin(x * 0.9)); P.set(x, h, hx("#3C3C3C")); P.set(x, h + 1, hx("#8A8A8A")); }
    for (const tx of [4, 7]) { P.rect(tx, 15, 1, 7, hx("#3C3C3C")); P.set(tx - 1, 14, hx("#3C3C3C")); P.set(tx + 1, 14, hx("#3C3C3C")); P.set(tx, 13, hx("#3C3C3C")); }
    P.rect(8, 25, 1, 1, hx("#C0392B"));
  }, { fit: true });
  reg("nbAquarium", 32, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 32; x++) P.set(x, y, mixc(hx("#8FD3E8"), hx("#3E8FB8"), y / 16));
    for (let x = 0; x < 32; x++) for (let y = 14; y < 16; y++) P.set(x, y, shadec(hx("#D9C79A"), (r() - 0.5) * 0.15));
    for (const wx of [4, 11, 26]) for (let y = 7; y < 14; y++) P.set(wx + (y % 2), y, hx("#3F8A4A"));
    for (const [fx, fy] of [[8, 5], [17, 9], [21, 4]]) { P.rect(fx, fy, 3, 2, hx("#F28A2E")); P.set(fx + 3, fy, hx("#F2B04E")); P.set(fx + 3, fy + 1, hx("#F2B04E")); }
    P.rect(0, 0, 32, 1, hx("#CFEFF8"));
  }, { fit: true, glow: 0.2 });
  reg("nbTansu", 16, 16, (P, r) => {
    fill(P, r, "#3E2A1E", 0.03); P.rect(0, 0, 16, 1, hx("#5A3E2C")); P.rect(0, 15, 16, 1, hx("#2A1C14"));
    P.rect(5, 6, 6, 2, hx("#1C1C1C")); P.rect(7, 5, 2, 1, hx("#1C1C1C"));
    for (const [x, y] of [[0, 0], [13, 0], [0, 13], [13, 13]]) P.rect(x, y, 3, 3, hx("#262626"));
  }, { fit: true });
  reg("nbQuiltMustard", 16, 16, (P, r) => {
    const cols = [hx("#D9A43A"), hx("#2E3B5E"), hx("#EFE4CC"), hx("#B98A2E")];
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) { const c = cols[(tx * 3 + ty) % 4]; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) P.set(tx * 8 + x, ty * 8 + y, shadec(c, x === 0 || y === 0 ? -0.14 : x === 1 || y === 1 ? 0.07 : 0)); }
  });
  reg("nbQuiltRed", 16, 16, (P, r) => { fill(P, r, "#C9403C", 0.02); for (let y = 1; y < 16; y += 4) for (let x = (y % 8 === 1 ? 1 : 3); x < 16; x += 4) P.rect(x, y, 2, 2, hx("#F6EEE6")); });
  const folds = [0.1, 0.06, 0, 0, -0.04, -0.09, -0.12, -0.04];
  reg("nbCurtainMustard", 16, 16, P => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#E0A93E"), folds[x % 8])); });
  reg("nbCurtainRed", 16, 16, P => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#D9534A"), folds[x % 8])); });
  reg("nbPlushTan", 16, 16, (P, r) => { fill(P, r, "#D9A86A", 0.04); });
  reg("nbPlushCream", 16, 16, (P, r) => { fill(P, r, "#F4E3C2", 0.03); });
  reg("nbPlushPink", 16, 16, (P, r) => { fill(P, r, "#F0A6BE", 0.04); });
  reg("nbPlushMint", 16, 16, (P, r) => { fill(P, r, "#9ED9C2", 0.04); });
  RUGS.nbIndigo = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#B8935A" : ((i + j) % 2 ? "#2F4A7C" : "#3A5890");
  RUGS.nbSage = (i, j, w, d) => (i === 0 || j === 0 || i === w - 1 || j === d - 1) ? "#6E8F5E" : ((i * 2 + j) % 3 ? "#EFE8D6" : "#D9CDB0");

  /** A rug of wool with its corners cut (an octagon), a ring of its edge colour round it. */
  function rugOct(R, x0, z0, x1, z1, inner, edge) {
    const w = x1 - x0, d = z1 - z0;
    for (let i = 0; i < w; i++) for (let j = 0; j < d; j++) {
      const cut = (i === 0 || i === w - 1) && (j === 0 || j === d - 1);
      if (cut) continue;
      const rim = i === 0 || j === 0 || i === w - 1 || j === d - 1 || (i === 1 || i === w - 2) && (j === 1 || j === d - 2);
      const k = wool(hexOf(shadec(hx(rim ? edge : inner), rugDrift(R.id + x0, i, j) * 0.028)));
      R.geo.box(x0 + i, 0, z0 + j, x0 + i + 1, 1 / 16, z0 + j + 1, { top: k, all: k, bottom: null });
    }
    R.item("Rug", "Wool, a block at a time, its corners cut.");
  }
  /** A rug with its corners cut and a one-block rim of its edge colour, the field whole inside it. */
  function rugRound(R, x0, z0, x1, z1, inner, edge) {
    const w = x1 - x0, d = z1 - z0;
    for (let i = 0; i < w; i++) for (let j = 0; j < d; j++) {
      if ((i === 0 || i === w - 1) && (j === 0 || j === d - 1)) continue;
      const rim = i === 0 || j === 0 || i === w - 1 || j === d - 1;
      const k = wool(hexOf(shadec(hx(rim ? edge : inner), rugDrift(R.id + x0, i, j) * 0.028)));
      R.geo.box(x0 + i, 0, z0 + j, x0 + i + 1, 1 / 16, z0 + j + 1, { top: k, all: k, bottom: null });
    }
    R.item("Rug", "Wool, a block at a time, its corners cut, a rim round it.");
  }
  /** A line laid in the floor (the lab's). */
  function line(R, x0, z0, x1, z1) { R.geo.box(x0, 0.012, z0, x1, 0.016, z1, { top: "elmLine", all: null }); }
  /** A plant in a pink pot, as HeartGold's corners have. */
  function pinkPlant(R, x, z, lines) { plant(R, x, z, 1.0, { pot: "potPink", kind: "leafy", lines: lines || ["A plant in a pink pot. Its leaves are glossy."] }); }

  // ================= PROF. ELM's LAB =================
  BUILD.elms_lab = function () {
    // HeartGold's: the healing machine, the PC and the window along the back, the big machine in the middle of the back
    // wall with the three POKé BALLs in it and ELM by it, a crate and a terminal; lines laid out across the floor from
    // the machine; two rows of blue bookshelves either side of the way down the middle (its stepping stones); the
    // wooden boards at the front, the mat; plants in pink pots in the corners.
    const W = 16, D = 17;
    const R = new Room("elms_lab", { name: "Prof. Elm's Lab", floor: G_, paper: "elmWall", W, D, H: 3.6, outsideSize: "9 by 8" });
    const G = R.geo;
    shell(R, { paper: "elmWall", floor: "elmTile", door: [7, 9], entry: true, entryFloor: "elmWood", skirt: "elmSkirt", ledge: "elmBlue", ledgeTop: "elmWhite",
      zones: [[0, 12.6, W, R.D - 2, "elmWood"]] });
    // The healing machine (Crystal's, back left).
    G.box(0.4, 0, 0.05, 2.6, 2.55, 0.95, { all: "elmWhite", s: "elmHeal", top: "cap" });
    G.box(0.55, 2.55, 0.2, 2.45, 2.7, 0.8, "elmBlue");
    R.solid(0.4, 0, 2.6, 0.95);
    R.act([0.3, 0, 2.7, 1.6], { label: "Look", lines: ["A machine for healing POKéMON.", "Its lights blink: ready when it's needed."] });
    R.item("Healing machine", "White, with a blue panel of lights and an orange label.");
    // The PC on its desk.
    deskPC(R, 3.2, 0, "s", R.screen("elmPC", 40, 26, drawPC, 0.9), { top: "lab", lines: ["OBSERVATIONS ON POKéMON EVOLUTION", "...It says on the screen..."] });
    officeChair(R, 4.35, 1.6, "n", "blueFabric");
    // The window (Crystal's).
    windowN(R, 6.2, 1.3, 1.6, 1.5, { lines: ["The window's open.", "A pleasant breeze is blowing in."] });
    // The big machine: a grey ring with its teeth, an orange heart, its red dome, the three POKé BALLs in a row on top.
    {
      const cx = 9.8, cz = 1.6;
      const disc = (r, y0, y1, mat) => { const k = 0.62 * r, e = Math.min(0.01, (y1 - y0) / 4); G.box(cx - r, y0, cz - k, cx + r, y1, cz + k, mat); G.box(cx - k, y0 + e, cz - r, cx + k, y1 - e, cz + r, mat); };
      disc(1.35, 0, 0.75, "elmGear");
      for (let i = 0; i < 8; i++) {
        const a = i * Math.PI / 4, tx = cx + Math.cos(a) * 1.42, tz = cz + Math.sin(a) * 1.42;
        G.box(tx - 0.16, 0, tz - 0.16, tx + 0.16, 0.62, tz + 0.16, "elmGear");
      }
      disc(1.05, 0.75, 0.88, "steel"); disc(0.82, 0.88, 1.05, "elmCore"); disc(0.55, 1.05, 1.28, "elmDome");
      for (const dx of [-0.42, 0, 0.42]) roundBall(G, cx + dx, 1.28, cz + 0.05, 0.3);
      R.solid(cx - 1.55, cz - 1.55, cx + 1.55, cz + 1.55);
      R.act([cx - 1.7, cz - 1.7, cx + 1.7, cz + 1.9], { label: "Look", lines: ["Three POKé BALLs, set in PROF. ELM's machine.", "They hold the POKéMON he's been studying."] });
      R.item("PROF. ELM's machine", "A grey ring with teeth, an orange heart, a red dome, three POKé BALLs on top.");
    }
    // A crate, the terminal, the bin (Crystal's, by the machine).
    boxes(R, 12.4, 0.6);
    labMachine(R, 13.3, 1.4, R.screen("elmTerm", 32, 24, drawPC, 0.8));
    bin(R, 15.2, 3.4);
    R.acts[R.acts.length - 1].lines = ["The wrapper from the snack PROF. ELM ate is in there..."];
    pinkPlant(R, 0.9, 2.4); pinkPlant(R, 15.1, 2.0);
    // The lines in the floor, out from the machine either side and on to the walls.
    line(R, 3.0, 4.9, 8.4, 5.15); line(R, 11.2, 4.9, 13.0, 5.15);
    line(R, 2.9, 4.9, 3.15, 7.2); line(R, 12.85, 4.9, 13.1, 7.2);
    line(R, 0, 6.95, 3.15, 7.2); line(R, 12.85, 6.95, W, 7.2);
    line(R, 9.55, 3.2, 9.8, 4.9); line(R, 8.4, 4.9, 11.2, 5.15);
    // The stepping stones down the middle, between the shelves.
    for (let z = 8.0; z < 12.4; z += 1.1) G.box(7.6, 0.012, z, 8.4, 0.018, z + 0.8, { top: "elmStone", all: null });
    R.item("Floor", "HeartGold's mint tiles, darker lines laid across them from the machine, stepping stones down the middle.");
    // The bookshelves, blue-framed, facing the way in: one on the left, three on the right (Crystal's TRAVEL TIPs on the
    // left one, ELM's books on the others).
    bookshelf(R, 0, 9.0, "s", 2.6, 2.2, { wood: "elmBlue", name: "Bookshelf", note: "A blue-framed bookshelf, full.", lines: ["{player} opened a book.", "Travel Tip 1: Press E to open the MENU.", "Travel Tip 2: Record your trip with SAVE!", "Travel Tip 3: Open your BAG and move your items about.", "Travel Tip 4: Check your POKéMON's moves on its SUMMARY."] });
    for (const x of [9.4, 11.6, 13.8]) bookshelf(R, x, 9.0, "s", 2.2, 2.2, { wood: "elmBlue", name: "Bookshelves", note: "Blue-framed bookshelves, full of PROF. ELM's books.", lines: ["Books on how POKéMON evolve, filed by PROF. ELM.", "Some of them are his own."] });
    pinkPlant(R, 0.9, 13.6); pinkPlant(R, 15.1, 13.6);
    pinkPlant(R, 0.9, 6.0); pinkPlant(R, 15.1, 6.0);
    doormat(R, 7, 9, { to: "town", spawn: { x: 8.0, z: R.D - 2.0, yaw: FACE.s }, mat: "matHome" });
    R.npcs.push({ name: "Prof. Elm", key: "elms_lab_elm", x: 7.4, z: 3.9, yaw: FACE.s, look: { skin: "prof_elm" },
      talk: () => [{ text: "ELM: {player}! There you are!" }, { text: "When I announce my findings, I'm sure we'll delve a bit deeper into the many mysteries of POKéMON." },
        { text: "You can count on it!" }] });
    R.npcs.push({ name: "Aide", key: "elms_lab_aide", x: 4.2, z: 11.4, yaw: FACE.e, look: { skin: "elm_aide" },
      talk: () => [{ text: "There are only two of us, so we're always busy." }] });
    R.spawn = { x: 8.0, z: R.D - 1.5, yaw: FACE.n };
    return R;
  };

  // ================= PROF. ELM's HOUSE (the LAB's upstairs) =================
  BUILD.elms_house = function () {
    // (11:45, John: "the Elm house is the top floor ... you go up the right hand side ... the entrance would need to be
    // to the right hand side as you go in instead of from the bottom.") HeartGold's ELMS_LAB_2F: up the steps outside
    // the LAB's east side and in through the east wall, half way along (HeartGold's own way in, its (12, 6)).
    // HeartGold's look: the pale blue paper with its white diamonds, the warm checked boards; the kitchen, the TV and
    // the shelves along the back; the PC on the left wall; the blue counter (an L) on the left; the green rug with its
    // corners cut, the yellow table and a blue sofa either side; plants in pink pots in the corners. ELM's wife, and
    // his son in front of the TV.
    const W = 12, D = 11;
    const R = new Room("elms_house", { name: "Prof. Elm's House", floor: G_, paper: "elmPaper", W, D, H: 3.4, outsideSize: "9 by 8, up the steps" });
    const G = R.geo;
    shell(R, { paper: "elmPaper", floor: "sqHoney", skirt: "mcStripped", ledge: "honey" });
    kitchen(R, 0.2, "s", { z: 0 });
    const k = R.acts.length;
    R.acts[k - 2].lines = ["The washing-up's all done."];
    R.acts[k - 1].lines = ["There's some food here. It must be for the LAB."];
    tvSet(R, 6.8, 0, "s", R.screen("elmTV", 40, 24, drawFilm, 0.9), { w: 2.4, lines: ["A nature programme: a flock of PIDGEY over the fields."] });
    bookshelf(R, 9.5, 0, "s", 2.2, 2.4, { wood: "honey", name: "Shelves", note: "Honey shelves of books and boxes.", lines: ["Books on POKéMON, and a photo of PROF. ELM as a boy."] });
    deskPC(R, 0, 3.5, "e", R.screen("elmHousePC", 40, 26, drawPC, 0.9), { top: "lab", lines: ["\"POKéMON. Where do they come from? Where are they going?",
      "Why has no one ever witnessed a POKéMON's birth?", "I want to know! I will dedicate my life to the study of POKéMON!\"", "...", "It's a part of PROF. ELM's research papers."] });
    // The blue counter: along, then back to the wall's side.
    G.box(0, 0, 4.4, 3.6, 0.95, 5.2, { all: "elmCounter", top: "lab" }); G.box(2.8, 0, 3.6, 3.6, 0.95, 4.4, { all: "elmCounter", top: "lab" });
    G.box(0, 0.95, 4.35, 3.65, 1.02, 5.25, "lab"); G.box(2.75, 0.95, 3.55, 3.65, 1.02, 4.4, "lab");
    R.solid(0, 4.4, 3.6, 5.2); R.solid(2.8, 3.6, 3.6, 4.4);
    fruitBowl(R, 1.4, 4.8, 1.02);
    R.act([0, 3.4, 3.9, 5.5], { label: "Look", lines: ["There's some food here. This must be for POKéMON."] });
    R.item("Counter", "A blue counter with a white top, an L along the left.");
    // The rug, the table and the two sofas.
    rugOct(R, 3, 5, 10, 9, "#7ACB9E", "#5BAF83");
    table(R, 5.6, 6.1, 7.4, 7.9, { top: "deskYellow", side: "deskYellow", leg: "walnut", h: 0.55, name: "Table", note: "A low yellow table." });
    sofa(R, 4.4, 7.95, "e", 1.9, { fabric: "blueFabric", cushion: "blueFabric" });
    sofa(R, 8.6, 6.05, "w", 1.9, { fabric: "blueFabric", cushion: "blueFabric" });
    R.item("Sofas", "Two blue sofas facing each other across the table.");
    pinkPlant(R, 11.1, 2.2); pinkPlant(R, 0.9, 10.1); pinkPlant(R, 11.1, 10.1);
    // The way in and out: the door in the east wall, a mat at its foot (walk east through it, back to the steps).
    passageSide(R, "e", 5, 7, { to: "town", mat: "matHomeEast", frame: "honey" });
    R.npcs.push({ name: "Elm's Wife", key: "elms_house_wife", x: 1.6, z: 6.8, yaw: FACE.e, look: { skin: "teacher" },
      talk: () => [{ text: "Hi, {player}! My husband's always so busy. I hope he's OK." }, { text: "When he's caught up in his POKéMON research, he even forgets to eat." }] });
    R.npcs.push({ name: "Elm's Son", key: "elms_house_son", x: 8.0, z: 2.5, yaw: FACE.n, look: { skin: "bug_catcher" },
      talk: () => [{ text: "When I grow up, I'm going to help my dad!" }, { text: "I'm going to be a great POKéMON professor!" }] });
    R.spawn = { x: W - 1.6, z: 6.0, yaw: FACE.w };
    return R;
  };

  // ----- What New Bark's two homes share (0.75, John's 11:45): their stairs at the back on the LEFT (HeartGold's,
  // both houses: up from its (3, 3)), climbing east along the back wall to a wall; the door at the front on the left,
  // where John's houses have theirs. -----
  const NB_DOOR = [2, 4], NB_FOOT = { x: 0.75, z: 1.1, yaw: FACE.s }, NB_TOP = { x: 4.9, z: 2.9, yaw: FACE.s };
  const NB_UPFLOOR = [[0, 0, 1.4, 12], [1.4, 2, 5.4, 12], [5.4, 0, 14, 12]];
  /** Dark timber posts up the walls (n: x's along the back; w and e: z's down the sides). */
  function posts(R, at, mat) {
    const G = R.geo, H = R.H, W = R.W;
    for (const x of at.n || []) G.box(x - 0.09, 0, 0, x + 0.09, H, 0.07, { all: mat, n: null, bottom: null });
    for (const z of at.w || []) G.box(0, 0, z - 0.09, 0.07, H, z + 0.09, { all: mat, w: null, bottom: null });
    for (const z of at.e || []) G.box(W - 0.07, 0, z - 0.09, W, H, z + 0.09, { all: mat, e: null, bottom: null });
  }
  /** The wainscot (boards to just over a block), a rail along its top, and a beam along the top of the walls. */
  function wainscot(R, boards, beam) { band(R, 0.24, boards, 0.9); band(R, 1.14, beam, 0.08); band(R, R.H - 0.42, beam, 0.26); }
  /** A JOHTO kitchen run along a wall, 5.35 long: the sink, the hob with a pan on it, a rice cooker, a chopping board,
   *  open shelves of bowls and jars over them, a hood, the fridge at the end. */
  function jKitchen(R, X, Z, f, o) {
    const F = R.F(X, Z, f), cab = o.cab, side = o.side || "honey", top = { all: "nbTop", top: "nbTop" };
    counterUnit(R, F, 0, 1, cab, { all: "nbTop", top: "sink" }, side); counterUnit(R, F, 1, 1, "oven", { all: "nbTop", top: "hob" }, side);
    counterUnit(R, F, 2, 1, cab, top, side); counterUnit(R, F, 3, 1, cab, top, side);
    F.box(0.45, 1.0, 0.08, 0.55, 1.45, 0.18, "steel"); F.box(0.45, 1.38, 0.08, 0.55, 1.45, 0.48, "steel"); F.box(0.35, 1.0, 0.08, 0.65, 1.06, 0.22, "steel");
    F.box(1.2, 1.0, 0.3, 1.75, 1.08, 0.8, { all: "darkTop" }); F.box(1.75, 1.04, 0.5, 2.05, 1.07, 0.58, "nbBeam");
    F.box(2.2, 1.0, 0.25, 2.75, 1.36, 0.72, { all: "potWhite", top: "greyPlastic" });
    F.box(3.15, 1.0, 0.3, 3.85, 1.04, 0.78, "honey"); F.box(3.3, 1.04, 0.45, 3.7, 1.12, 0.6, "linen");
    F.box(0, 1.0, 0, 4, 1.8, 0.03, { front: o.splash, all: null });
    for (const [a, b] of [[0, 1.0], [2.1, 4.0]]) {
      F.box(a, 2.05, 0, b, 2.11, 0.4, "nbBeam");
      let i = 0;
      for (let p = a + 0.12; p < b - 0.3; p += 0.38, i++) F.box(p, 2.11, 0.1, p + 0.26, 2.3 + (i % 3) * 0.05, 0.32, i % 2 ? "potWhite" : "pot");
    }
    F.box(1.0, 2.15, 0, 2.1, 2.42, 0.8, { all: o.hood || "steel", back: null }); F.box(1.35, 2.42, 0, 1.75, R.H, 0.4, { all: o.hood || "steel", back: null });
    F.box(4.05, 0, 0, 5.35, 2.5, 0.95, { front: o.fridge, all: "paint", back: null, bottom: null }); F.solid(4.05, 0, 5.35, 0.95);
    R.act(F.rect(0, 0.6, 1, 1.5), { label: "Look", lines: o.sink });
    R.act(F.rect(1, 0.6, 2, 1.5), { label: "Look", lines: o.stove });
    R.act(F.rect(4.05, 0.5, 5.35, 1.4), { label: "Open", lines: o.cold });
    R.item("Kitchen", o.note);
    const [sx, sz] = F.pt(1.45, 0.55); R.steam = { x: sx, y: 1.1, z: sz };
  }
  /** A round table on one leg (its top built up in strips, as a round thing is in blocks). */
  function roundTable(R, cx, cz, rad, o = {}) {
    const G = R.geo, h = o.h || 0.82, top = o.top || "honey", t = 0.08;
    const zs = [-1, -0.84, -0.66, -0.4, 0.4, 0.66, 0.84, 1], ws = [0.5, 0.72, 0.88, 1, 0.88, 0.72, 0.5];
    for (let i = 0; i < ws.length; i++) G.box(cx - ws[i] * rad, h - t, cz + zs[i] * rad, cx + ws[i] * rad, h, cz + zs[i + 1] * rad, { all: o.side || top, top });
    G.box(cx - 0.1, 0, cz - 0.1, cx + 0.1, h - t, cz + 0.1, o.leg || "nbBeam"); G.box(cx - 0.42, 0, cz - 0.42, cx + 0.42, 0.07, cz + 0.42, o.leg || "nbBeam");
    R.solid(cx - rad * 0.85, cz - rad * 0.85, cx + rad * 0.85, cz + rad * 0.85);
    R.item(o.name || "Round table", o.note || "A round table on one leg.");
  }
  /** A plush toy (round, with ears), its size s, its colours. */
  function plush(R, x, y, z, s, body, belly) {
    const G = R.geo;
    G.box(x - 0.3 * s, y, z - 0.27 * s, x + 0.3 * s, y + 0.45 * s, z + 0.27 * s, body);
    G.box(x - 0.2 * s, y + 0.08 * s, z + 0.25 * s, x + 0.2 * s, y + 0.37 * s, z + 0.29 * s, belly);
    G.box(x - 0.22 * s, y + 0.45 * s, z - 0.2 * s, x + 0.22 * s, y + 0.78 * s, z + 0.2 * s, body);
    for (const dx of [-0.16, 0.16]) G.box(x + (dx - 0.06) * s, y + 0.78 * s, z - 0.05 * s, x + (dx + 0.06) * s, y + 0.9 * s, z + 0.04 * s, body);
    for (const dx of [-0.09, 0.09]) G.box(x + (dx - 0.025) * s, y + 0.62 * s, z + 0.2 * s, x + (dx + 0.025) * s, y + 0.66 * s, z + 0.21 * s, "plastic");
  }

  // ================= Your house =================
  BUILD.nb_house1 = function () {
    // Downstairs, JOHTO's way and laid out as HeartGold's: the stairs up at the back on the left, the TV beside them,
    // the kitchen along the back on the right (the sink, the hob, the fridge: HeartGold's three, with its words); the
    // dining table on an indigo rug on the left; a raised tatami corner at the front on the right with a quilted low
    // table and floor cushions round it; a stone step at the door, a shoe cupboard by it. MOM in the middle of the
    // room (HeartGold's (6, 7)).
    const W = 14, D = 13;
    const R = new Room("nb_house1", { name: "Your house", floor: "Downstairs", paper: "nbPlaster", W, D, outsideSize: "10 by 6" });
    const G = R.geo;
    shell(R, { paper: "nbPlaster", floor: "nbBoards", door: NB_DOOR, entry: true, entryFloor: "nbGenkan", skirt: "nbBeam", ledge: "nbBeam", rail: false });
    wainscot(R, "nbCedar", "nbBeam");
    posts(R, { n: [0.09, 7.9, 13.91], w: [4.4, 8.4], e: [3.2, 6.5] }, "nbBeam");
    stairsUp(R, 1.4, 0, 2, { to: "nb_house2", spawn: NB_TOP, wall: "nbPlaster" });
    tvSet(R, 5.75, 0, "s", R.screen("nbTV", 40, 24, drawFilm, 0.9), { w: 2.0, tw: 1.5, th: 0.95,
      lines: ["There's a film on: two boys searching a cave for treasure!", "Better get going..."] });
    clock(R, 6.35, 2.0);
    jKitchen(R, 8.0, 0, "s", { cab: "nbCab", splash: "nbSplash", fridge: "nbFridge", hood: "nbBeam",
      sink: ["A squeaky-clean sink! MOM loves having it clean."], stove: ["CINNABAR VOLCANO BURGER! MOM's specialty!"],
      cold: ["What's in the fridge...?", "Lots of drinking water and sweeeet lemonade!"],
      note: "Cedar cupboards, the sink, the hob with a pan on it, a rice cooker, open shelves of bowls, a cream fridge." });
    // The dining table, on its rug.
    rug(R, 1, 3, 6, 8, "nbIndigo");
    table(R, 2.0, 4.2, 4.8, 6.0, { top: "honey", side: "nbBeam", leg: "nbBeam", h: 0.82, name: "Dining table", note: "A honey-wood table, laid for breakfast." });
    plates(R, [[2.75, 4.65], [4.05, 4.65], [2.75, 5.55], [4.05, 5.55]], 0.82);
    G.box(3.25, 0.82, 4.95, 3.55, 1.02, 5.25, { all: "potWhite", top: "greyPlastic" }); G.box(3.55, 0.92, 5.05, 3.68, 0.96, 5.15, "potWhite");
    for (const x of [2.75, 4.05]) { chair(R, x, 3.6, "s", { wood: "nbBeam", seat: "nbZabuton" }); chair(R, x, 6.6, "n", { wood: "nbBeam", seat: "nbZabuton" }); }
    R.act([1.8, 3.8, 5.0, 6.4], { label: "Look", lines: ["Breakfast's laid out: rice, miso soup and a pot of tea."] });
    // The tatami corner: a step up, mats edged in dark cloth, the quilted low table with a bowl of oranges on it.
    const px0 = 8.4, pz0 = 6.6, px1 = W, pz1 = 10.7, ph = 0.22;
    G.box(px0, 0, pz0, px1, ph, pz1, { all: "nbBeam", top: "nbTatami" });
    for (let i = 0; i < 4; i++) for (let j = 0; j < 2; j++) {
      const a = px0 + 0.05 + i * 1.39, b = a + 1.39, c = pz0 + 0.05 + j * 2.0, d = Math.min(pz1 - 0.05, c + 2.0);
      G.box(a, ph, c, b, ph + 0.015, d, { top: "nbTatami", all: "nbTatami", bottom: null });
      for (const e of [a, b - 0.08]) G.box(e, ph + 0.015, c, e + 0.08, ph + 0.022, d, { top: "nbTatamiEdge", all: "nbTatamiEdge", bottom: null });
    }
    R.ramps.push({ rect: [px0, pz0, px1, pz1], h: () => ph });
    R.item("Tatami corner", "A step up from the boards: tatami mats edged in dark cloth.");
    const cx = 11.2, cz = 8.65;
    G.box(cx - 0.95, ph, cz - 0.95, cx + 0.95, ph + 0.36, cz + 0.95, { all: "nbKotatsu", top: "nbKotatsu" });
    G.box(cx - 0.85, ph + 0.36, cz - 0.85, cx + 0.85, ph + 0.42, cz + 0.85, { all: "nbBeam", top: "honey" });
    G.box(cx - 0.24, ph + 0.42, cz - 0.24, cx + 0.24, ph + 0.5, cz + 0.24, "potWhite");
    for (const [dx, dz, dy] of [[-0.1, -0.08, 0], [0.09, -0.05, 0], [0, 0.1, 0], [0.0, -0.01, 0.07]]) G.box(cx + dx - 0.07, ph + 0.5 + dy, cz + dz - 0.07, cx + dx + 0.07, ph + 0.62 + dy, cz + dz + 0.07, "nbMikan");
    for (const [dx, dz] of [[-0.55, 0.45], [0.5, -0.5]]) G.box(cx + dx - 0.08, ph + 0.42, cz + dz - 0.08, cx + dx + 0.08, ph + 0.56, cz + dz + 0.08, "potWhite");
    R.solid(cx - 0.95, cz - 0.95, cx + 0.95, cz + 0.95);
    R.act([cx - 1.3, cz - 1.3, cx + 1.3, cz + 1.3], { label: "Look", lines: ["A low table with a quilt over it, warm underneath.", "A bowl of oranges on top. MOM's left you one."] });
    R.item("Quilted low table", "A low table with an indigo quilt draped over it, a bowl of oranges and two cups of tea on its top.");
    for (const [x, z, fab] of [[cx - 1.45, cz, "nbZabuton"], [cx + 1.45, cz, "nbZabuton"], [cx, cz + 1.45, "nbZabutonRed"], [cx, cz - 1.45, "nbZabutonRed"]]) pouf(R, x, z, 0.78, ph, ph + 0.2, fab);
    R.item("Floor cushions", "Flat square cushions round the low table, mustard and red.");
    // A vase of flowers on a little stand in the corner, and New Bark's turbines in a picture over the tatami.
    G.box(13.0, ph, 6.85, 13.75, ph + 0.35, 7.6, { all: "nbBeam", top: "honey" });
    G.box(13.22, ph + 0.35, 7.07, 13.53, ph + 0.75, 7.38, "potWhite"); G.box(13.05, ph + 0.75, 6.9, 13.7, ph + 1.2, 7.55, { all: "flower" });
    R.solid(13.0, 6.85, 13.75, 7.6);
    framed(R, 7.6, 1.55, 2.0, 1.2, "nbPicTurbines", "e", { frameMat: "nbBeam", act: { label: "Look", lines: ["A picture of NEW BARK TOWN's wind turbines on the hill, the sea past them."] } });
    // The window on the left, the shoe cupboard by the door.
    crossWindow(R, "w", 5.6, 1.5, 1.8, 1.4, ["Out of the window: NEW BARK TOWN, and the wind turbines turning on the hill."]);
    G.box(0, 0, 8.9, 0.6, 0.9, 10.6, { all: "nbCedar", e: "nbCab", top: "honey" }); R.solid(0, 8.9, 0.6, 10.6);
    G.box(0.75, 0, 9.9, 1.0, 0.12, 10.3, "redFabric"); G.box(1.05, 0, 9.9, 1.3, 0.12, 10.3, "redFabric");
    R.act([0, 8.7, 1.4, 10.7], { label: "Look", lines: ["The shoe cupboard. Your running shoes aren't in it: they're on your feet."] });
    R.item("Shoe cupboard", "A low cedar cupboard by the door, a pair of slippers in front of it.");
    doormat(R, NB_DOOR[0], NB_DOOR[1], { to: "town", spawn: { x: 3.0, z: 10.0, yaw: FACE.s }, mat: "matHome" });
    R.npcs.push({ name: "Mom", key: "nb_house1_mom", x: 6.4, z: 6.0, yaw: FACE.s, look: { skin: "middle_aged_woman" },
      talk: () => [{ text: "Hi, {player}! You're finally awake." }, { text: "Oh, I almost forgot! PROF. ELM was looking for you. He said he had a favor to ask of you." },
        { text: "You know where the LAB is, right? It's right next door to us." }] });
    R.spawn = NB_FOOT;
    return R;
  };

  BUILD.nb_house2 = function () {
    // Your room, upstairs: the stairs down at the back on the left (HeartGold's), the desk and PC beside them, the TV
    // and the games console (HeartGold's PC and console, its (6, 4) and (8, 4)), the window over the corner, the bed
    // down the right with the radio by it; a mustard rug with a navy edge, a low table with JOHTO's map on it, two floor
    // cushions; the wardrobe and three empty trophy stands on the left (HeartGold's trophies come later), a bookcase, a
    // big plush doll in the corner.
    const W = 14, D = 12;
    const R = new Room("nb_house2", { name: "Your room", floor: "Upstairs", paper: "nbPlaster", W, D });
    const G = R.geo;
    shell(R, { paper: "nbPlaster", floor: "nbBoardsLight", floorRects: NB_UPFLOOR, skirt: "nbBeam", rail: false });
    wainscot(R, "nbCedar", "nbBeam");
    posts(R, { n: [0.09, 11.2, 13.91], w: [3.6, 9.9], e: [6.0, 9.4] }, "nbBeam");
    stairsDown(R, 1.4, 0, 2, { to: "nb_house1", spawn: NB_FOOT, paper: "nbPlaster" });
    deskPC(R, 5.8, 0, "s", R.screen("nbPC", 32, 24, drawPC, 1), { pc: true, top: "honey" });
    officeChair(R, 6.95, 1.6, "n");
    tvSet(R, 8.4, 0, "s", R.screen("nbGame", 32, 24, drawRace, 0.95), { w: 2.3, tw: 1.6, th: 1.0, lines: ["A games console! Everyone in JOHTO has one."] });
    gameConsole(R, 9.55, 1.45);
    R.acts[R.acts.length - 1].lines = ["A games console! Everyone in JOHTO has one.", "A racing game, paused on lap 2. It'll keep."];
    cushionSeat(R, 9.0, 2.8, "nbZabuton"); cushionSeat(R, 10.1, 2.8, "blueFabric");
    windowN(R, 11.6, 1.5, 1.9, 1.6, { curtains: "nbCurtainMustard", curtainIn: 0.3, lines: ["From up here you can see the wind turbines on the hill, and the sea."] });
    // The radio by the bed, on a low cupboard; the bed down the right.
    G.box(13.3, 0, 2.3, W, 0.7, 3.1, { all: "honey", w: "drawersHoney" }); R.solid(13.3, 2.3, W, 3.1);
    G.box(13.45, 0.7, 2.45, 13.85, 0.98, 2.95, { all: "greyPlastic", w: "drawersDark" }); G.box(13.8, 0.98, 2.5, 13.83, 1.4, 2.53, "steel");
    R.act([12.8, 2.2, W, 3.2], { label: "Listen", lines: ["The radio's on, very quietly: POKéMON TALK, then a song about the sea."] });
    R.item("Radio", "A grey radio on a low honey cupboard by the bed, its aerial up.");
    bed(R, W, 3.2, "w", { w: 1.8, l: 2.9, wood: "honey", quilt: "nbQuiltMustard" });
    bookshelf(R, W, 6.6, "w", 2.0, 2.2, { wood: "honey", name: "Bookcase", note: "Comics and adventure books.", lines: ["Comics, and a book of maps of JOHTO."] });
    // The rug, the low table with the map, two cushions.
    rugRound(R, 4, 4, 11, 9, "#E3B341", "#2E3B5E");
    table(R, 6.8, 5.8, 8.6, 7.0, { top: "honey", side: "nbBeam", leg: "nbBeam", h: 0.42, name: "Low table", note: "A low honey-wood table, a map of JOHTO spread out on it." });
    G.box(7.0, 0.42, 5.95, 8.4, 0.43, 6.85, { top: "map", all: "linen" });
    R.act([6.6, 5.6, 8.8, 7.2], { label: "Look", lines: ["A map of JOHTO, spread out. NEW BARK TOWN's circled, and an arrow points west."] });
    cushionSeat(R, 6.0, 6.4, "nbZabutonRed"); cushionSeat(R, 9.4, 6.4, "nbZabuton");
    // On the left: the wardrobe, the three trophy stands with a poster over them.
    wardrobe(R, 0, 6.2, "e");
    G.box(0, 0, 7.4, 0.6, 0.75, 9.6, { all: "honey", e: "drawersHoney" }); R.solid(0, 7.4, 0.6, 9.6);
    for (const z of [7.8, 8.5, 9.2]) G.box(0.12, 0.75, z - 0.16, 0.44, 0.83, z + 0.16, "walnut");
    R.act([0, 7.2, 1.2, 9.8], { label: "Look", lines: ["Three empty stands. Room for trophies."] });
    R.item("Trophy stands", "A low honey cupboard on the left wall, three little walnut stands on it, all empty.");
    framed(R, 7.75, 1.6, 1.7, 1.2, "poster", "w", { frameMat: "honey", act: { label: "Look", lines: ["A poster of a trainer mid-throw. GO!"] } });
    // The big plush doll in the corner.
    plush(R, 12.9, 0, 10.6, 2.2, "nbPlushTan", "nbPlushCream");
    R.solid(12.2, 9.9, 13.6, 11.3);
    R.act([12.0, 9.7, 13.8, 11.5], { label: "Look", lines: ["A big plush doll. It's soft and squashy."] }); R.item("Plush doll", "A big round plush with ears and a cream tummy.");
    R.spawn = NB_TOP;
    return R;
  };

  // ================= Your rival's house =================
  BUILD.nb_rival1 = function () {
    // HeartGold's NEW_BARK_RIVAL_HOUSE (John's house south of the street, its door on the left as yours is), its
    // stairs at the back on the left as yours are, and nothing else like yours: a sage cottage over white boards,
    // terracotta tiles; a teal kitchen down the right; a step chest and a hanging scroll on the back wall; a round table
    // with three chairs; a fish tank by the window; the sofa and the TV in the front corner on the right. Your rival's
    // dad (HeartGold's man at its (6, 7)).
    const W = 14, D = 13;
    const R = new Room("nb_rival1", { name: "Rival's house", floor: "Downstairs", paper: "nbPlasterSage", W, D, outsideSize: "10 by 6" });
    const G = R.geo;
    shell(R, { paper: "nbPlasterSage", floor: "nbTile", door: NB_DOOR, entry: true, entryFloor: "nbGenkan", skirt: "paint", ledge: "walnut", rail: false });
    band(R, 0.24, "nbWainWhite", 0.9); band(R, 1.14, "paint", 0.08);
    stairsUp(R, 1.4, 0, 2, { to: "nb_rival2", spawn: NB_TOP, wall: "nbPlasterSage" });
    // The step chest: tall on the left, low on the right, a vase of dried flowers on the step.
    const tz = 0.62;
    for (const [y0, y1] of [[0.05, 0.62], [0.62, 1.2], [1.2, 1.78]]) G.box(6.0, y0, 0, 7.2, y1, tz, { all: "nbBeam", s: "nbTansu" });
    for (const [y0, y1] of [[0.05, 0.5], [0.5, 0.95]]) G.box(7.2, y0, 0, 8.4, y1, tz, { all: "nbBeam", s: "nbTansu" });
    G.box(5.97, 0, 0, 8.43, 0.05, tz + 0.03, "nbBeam"); G.box(5.97, 1.78, 0, 7.23, 1.84, tz + 0.03, "nbBeam"); G.box(7.2, 0.95, 0, 8.43, 1.0, tz + 0.03, "nbBeam");
    G.box(7.6, 1.0, 0.16, 7.9, 1.36, 0.46, "pot"); G.box(7.45, 1.36, 0.05, 8.05, 1.85, 0.58, { all: "flower" });
    R.solid(6.0, 0, 8.4, tz + 0.03);
    R.act([5.9, 0.3, 8.5, 1.2], { label: "Look", lines: ["A step chest, its drawers dark with iron pulls. Dried flowers on the low step."] });
    R.item("Step chest", "A dark wooden chest of drawers, tall on the left and low on the right like a step, iron pulls and corners.");
    // The hanging scroll.
    G.box(8.85, 2.62, 0.02, 9.75, 2.7, 0.1, "nbBeam"); G.box(8.9, 0.98, 0.02, 9.7, 2.62, 0.035, { s: "nbScroll", all: null });
    G.box(8.82, 0.9, 0.02, 9.78, 0.98, 0.12, "nbBeam"); G.box(9.28, 2.7, 0.02, 9.32, 2.95, 0.04, "nbBeam");
    R.act([8.7, 0, 9.9, 0.6], { label: "Look", lines: ["A hanging scroll: wind turbines on a hill, in ink. A little red seal in the corner."] });
    R.item("Hanging scroll", "A scroll on the back wall: a hill and two turbines in ink, a red seal.");
    crossWindowN(R, 10.3, 1.5, 1.8, 1.3, ["Out of the window: the street, and PROF. ELM's LAB over the way."]);
    jKitchen(R, W, 0.4, "w", { cab: "nbCabTeal", side: "paint", splash: "nbSplashSage", fridge: "nbFridgeMint", hood: "steel",
      sink: ["The dishes are stacked up to dry."], stove: ["A pan of pancakes, keeping warm."], cold: ["Milk, eggs and a jar of honey."],
      note: "Teal cupboards down the right, the sink, the hob, a rice cooker, open shelves of bowls, a mint fridge." });
    // The round table and its three chairs.
    roundTable(R, 9.6, 3.6, 0.95, { top: "honey", side: "walnut", leg: "walnut", name: "Round table", note: "A round honey-wood table on one leg, a bowl of fruit on it." });
    fruitBowl(R, 9.6, 3.6, 0.82);
    chair(R, 8.05, 3.6, "e", { seat: "nbSofaOlive" }); chair(R, 11.15, 3.6, "w", { seat: "nbSofaOlive" }); chair(R, 9.6, 5.15, "n", { seat: "nbSofaOlive" });
    // The fish tank, on the left by the window.
    crossWindow(R, "w", 3.2, 1.5, 1.8, 1.3, ["Out of the window: the trees at the edge of town, and ROUTE 29 past them."]);
    G.box(0, 0, 6.4, 0.8, 0.75, 8.8, { all: "walnut", e: "drawersWalnut" });
    G.box(0.05, 0.75, 6.45, 0.75, 1.5, 8.75, { all: "glass", e: "nbAquarium", n: "nbAquarium", s: "nbAquarium", top: null, bottom: null });
    G.box(0.02, 1.5, 6.42, 0.78, 1.57, 8.78, "greyPlastic");
    R.solid(0, 6.4, 0.8, 8.8);
    R.act([0, 6.2, 1.4, 9.0], { label: "Look", lines: ["A fish tank. Little orange fish dart about between the weeds."] });
    R.item("Fish tank", "A tank on a walnut cupboard: blue water, weeds, orange fish, a grey lid.");
    // The sofa and the TV, in the front corner on the right.
    rug(R, 9, 7, 13, 10, "nbSage");
    sofa(R, 9.0, 9.8, "e", 2.6, { fabric: "nbSofaOlive", cushion: "nbSofaOlive", pillow: "cushion", pillow2: "nbZabuton", lines: ["A soft olive sofa. Someone's left a book on it."] });
    coffeeTable(R, 10.9, 7.8, 12.1, 9.2);
    tvSet(R, W, 7.4, "w", R.screen("nbRivalTV", 40, 24, drawFilm, 0.9), { w: 2.2, tw: 1.6, th: 0.95, lines: ["A programme about PROF. ELM's research. They say he's famous."] });
    doormat(R, NB_DOOR[0], NB_DOOR[1], { to: "town", spawn: { x: 3.0, z: 10.0, yaw: FACE.s }, mat: "matRival" });
    R.npcs.push({ name: "Dad", key: "nb_rival1_dad", x: 6.2, z: 6.4, yaw: FACE.s, look: { skin: "middle_aged_man" },
      talk: () => [{ text: "Hi, {player}! If you're looking for my kid, they're out helping PROF. ELM again." }, { text: "Busier than I am!" },
        { text: "MARILL's upstairs, playing games as usual." }] });
    R.spawn = NB_FOOT;
    return R;
  };

  BUILD.nb_rival2 = function () {
    // Your rival's room, nothing like Gary's: peach walls over white boards, pale ash boards; the desk and PC and the
    // TV with the console along the back (HeartGold's, as in your room), the window with red curtains over a chest of
    // drawers; the bed down the right under a red spotted quilt; a cream rug edged in red, a red beanbag; the bookcase
    // and a shelf of plush dolls on the left. Their MARILL (HeartGold's, at its (4, 6)) potters about.
    const W = 14, D = 12;
    const R = new Room("nb_rival2", { name: "Rival's room", floor: "Upstairs", paper: "nbPlasterPeach", W, D });
    const G = R.geo;
    shell(R, { paper: "nbPlasterPeach", floor: "nbBoardsAsh", floorRects: NB_UPFLOOR, skirt: "paint", rail: false });
    band(R, 0.24, "nbWainWhite", 0.9); band(R, 1.14, "paint", 0.08);
    stairsDown(R, 1.4, 0, 2, { to: "nb_rival1", spawn: NB_FOOT, paper: "nbPlasterPeach" });
    deskPC(R, 5.8, 0, "s", R.screen("nbRivalPC", 32, 24, drawPC, 1), { top: "paint", lines: ["The PC. Its screensaver's a MARILL, bouncing about."] });
    officeChair(R, 6.95, 1.6, "n", "redFabric");
    framed(R, 6.3, 2.15, 1.3, 0.85, "photos", "n", { frameMat: "paint", act: { label: "Look", lines: ["Photos: your rival and MARILL, on every one of them."] } });
    tvSet(R, 8.4, 0, "s", R.screen("nbRivalGame", 32, 24, drawRace, 0.95), { w: 2.0, tw: 1.5, th: 0.95, lines: ["A games console. Two controllers, one of them chewed."] });
    gameConsole(R, 9.4, 1.45);
    R.acts[R.acts.length - 1].lines = ["A games console. Two controllers, one of them chewed."];
    cushionSeat(R, 8.9, 2.8, "nbZabutonRed"); cushionSeat(R, 9.95, 2.8, "cushion");
    drawerUnit(R, 11.5, 0, "s", 1.4, 0.9, "pale");
    windowN(R, 11.3, 1.5, 1.9, 1.6, { curtains: "nbCurtainRed", curtainIn: 0.3, lines: ["From up here you can see right across NEW BARK TOWN, to the sea."] });
    bed(R, W, 5.4, "w", { w: 1.8, l: 2.9, wood: "paint", quilt: "nbQuiltRed", ask: false });
    R.acts[R.acts.length - 1].lines = ["A red spotted quilt, rumpled. Somebody left in a hurry."];
    rugRound(R, 3, 4, 10, 9, "#F4ECDD", "#D2504A");
    beanBag(R, 7.2, 6.6, "redFabric");
    bookshelf(R, 0, 7.0, "e", 2.0, 2.2, { wood: "pale", name: "Bookcase", note: "Books on POKéMON, and a diary.", lines: ["Books on POKéMON, and a diary with a lock on it."] });
    // The shelf of plush dolls.
    G.box(0, 0, 8.3, 0.7, 0.8, 10.5, { all: "pale", e: "drawersPale" }); R.solid(0, 8.3, 0.7, 10.5);
    for (const [z, b, c] of [[8.7, "nbPlushPink", "nbPlushCream"], [9.4, "nbPlushMint", "nbPlushCream"], [10.1, "nbPlushTan", "nbPlushCream"]]) plush(R, 0.35, 0.8, z, 0.85, b, c);
    R.act([0, 8.1, 1.3, 10.7], { label: "Look", lines: ["A row of plush dolls. Their eyes are all sewn on a little crooked."] });
    R.item("Plush dolls", "A low pale cupboard on the left wall, three little plush dolls sitting on it.");
    // A dressing table with its mirror on the right, a stool at it.
    G.box(13.35, 0, 8.6, W, 0.8, 10.0, { all: "pale", w: "drawersPale" }); G.box(13.3, 0.8, 8.55, W, 0.86, 10.05, "paint");
    G.box(13.9, 0.86, 8.85, W, 2.0, 9.75, { all: "paint", w: "silver" });
    G.box(13.45, 0.86, 9.0, 13.65, 1.02, 9.2, "redFabric");
    R.solid(13.35, 8.6, W, 10.0);
    R.act([12.8, 8.4, W, 10.2], { label: "Look", lines: ["A dressing table and its mirror. A red ribbon on the corner of it."] });
    R.item("Dressing table", "A pale dressing table against the right wall, a tall mirror over it, a red ribbon.");
    stool(R, 12.75, 9.3);
    floorLamp(R, 13.3, 10.9);
    (R._mons || (R._mons = [])).push({ key: "pet:nb_rival2_marill", species: "marill", x: 4.6, z: 6.6, yaw: FACE.s });
    R.spawn = NB_TOP;
    return R;
  };
})();
