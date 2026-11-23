// ---------- 0.52.0 (09:13): Saffron City's two houses you go in, as FireRed's (pret/pokefirered) ----------
// The COPYCAT's house (the top one, its double door under the striped awnings): her mum and dad and their CHANSEY
// downstairs; upstairs, her room, her dolls, her DODUO, her PC ("My Secrets!"), the game on her TV, and the NUGGET
// FireRed hides by her PC. She teaches MIMIC for a POKé DOLL. MR. PSYCHIC's house (south of SILPH CO.): he knows you
// want TM29 PSYCHIC before you say a word.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const place = (id, name, floor) => PLACES.push({ id, name, floor, group: "Saffron City" });
  place("copycats_house_1f", "Copycat's house", "Downstairs");
  place("copycats_house_2f", "Copycat's house", "Upstairs");
  place("mr_psychics_house", "Mr. Psychic's house", "Saffron City");
  const WINDOW = ["Outside: SAFFRON CITY's yellow buildings, and SILPH CO.'s tower over them."];

  // Saffron's homes, as Let's Go's: warm cream walls with a soft yellow stripe, honey boards.
  reg("paperSaffron", 16, 16, (P, r) => wallpaper(P, r, hx("#F6EBCB"), hx("#EFD9A0"), hx("#D9A94E")));
  reg("paperCopycat", 16, 16, (P, r) => wallpaper(P, r, hx("#FBE3EC"), hx("#F4C9DA"), hx("#E58FB1")));
  reg("rugPink", 16, 16, (P, r) => {
    P.fill(hx("#EE9DBE"), 0.03, r); P.rect(0, 0, 16, 1, hx("#FFD6E6")); P.rect(0, 15, 16, 1, hx("#FFD6E6")); P.rect(0, 0, 1, 16, hx("#FFD6E6")); P.rect(15, 0, 1, 16, hx("#FFD6E6"));
    for (let i = 4; i < 13; i += 4) P.rect(i, i, 1, 1, hx("#FFF0F6"));
  });
  reg("quiltPink", 16, 16, (P, r) => { P.fill(hx("#F4A9C6"), 0.03, r); for (let y = 0; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#FFD3E3")); for (let x = 0; x < 16; x += 4) P.rect(x, 0, 1, 16, hx("#E487AD")); });
  // The dolls' plush: FireRed's PIKACHU and JIGGLYPUFF dolls, and her CLEFAIRY (her favourite).
  reg("plushYellow", 16, 16, (P, r) => P.fill(hx("#F7D44A"), 0.03, r));
  reg("plushPink", 16, 16, (P, r) => P.fill(hx("#F7B7CF"), 0.03, r));
  reg("plushPale", 16, 16, (P, r) => P.fill(hx("#F9C9D6"), 0.03, r));
  reg("plushDark", 16, 16, (P, r) => P.fill(hx("#2A2626"), 0.02, r));
  reg("plushRed", 16, 16, (P, r) => P.fill(hx("#E0484A"), 0.02, r));
  reg("plushEye", 16, 16, (P, r) => { P.fill(hx("#F7B7CF"), 0.02, r); P.rect(3, 6, 3, 4, hx("#2A2626")); P.rect(10, 6, 3, 4, hx("#2A2626")); P.rect(4, 6, 1, 1, hx("#FFFFFF")); P.rect(11, 6, 1, 1, hx("#FFFFFF")); });
  reg("plushFaceY", 16, 16, (P, r) => { P.fill(hx("#F7D44A"), 0.02, r); P.rect(3, 5, 2, 2, hx("#2A2626")); P.rect(11, 5, 2, 2, hx("#2A2626")); P.rect(1, 9, 3, 3, hx("#E0484A")); P.rect(12, 9, 3, 3, hx("#E0484A")); P.rect(7, 9, 2, 1, hx("#2A2626")); });

  // A doll on the floor or a shelf: a soft body, a head, and its own ears or tuft.
  function doll(R, x, z, kind, y = 0, lines) {
    const G = R.geo, s = 0.5;
    if (kind === "pikachu") {
      G.box(x - 0.18, y, z - 0.14, x + 0.18, y + 0.3, z + 0.14, { all: "plushYellow" });
      G.box(x - 0.2, y + 0.3, z - 0.17, x + 0.2, y + 0.62, z + 0.17, { all: "plushYellow", s: "plushFaceY" });
      for (const dx of [-0.13, 0.13]) { G.box(x + dx - 0.04, y + 0.62, z - 0.03, x + dx + 0.04, y + 0.86, z + 0.03, "plushYellow"); G.box(x + dx - 0.04, y + 0.86, z - 0.03, x + dx + 0.04, y + 0.94, z + 0.03, "plushDark"); }
      G.box(x + 0.16, y + 0.12, z + 0.12, x + 0.22, y + 0.5, z + 0.18, "plushYellow");
    } else if (kind === "jigglypuff") {
      G.box(x - 0.24, y, z - 0.22, x + 0.24, y + 0.46, z + 0.22, { all: "plushPink", s: "plushEye" });
      G.box(x - 0.06, y + 0.46, z - 0.08, x + 0.06, y + 0.56, z + 0.04, "plushPink");
      for (const dx of [-0.17, 0.17]) G.box(x + dx - 0.05, y + 0.46, z - 0.03, x + dx + 0.05, y + 0.58, z + 0.03, "plushPink");
    } else {
      G.box(x - 0.18, y, z - 0.15, x + 0.18, y + 0.34, z + 0.15, { all: "plushPale" });
      G.box(x - 0.2, y + 0.34, z - 0.17, x + 0.2, y + 0.64, z + 0.17, { all: "plushPale", s: "plushEye" });
      for (const dx of [-0.13, 0.13]) G.box(x + dx - 0.04, y + 0.64, z - 0.03, x + dx + 0.04, y + 0.8, z + 0.03, "plushDark");
    }
    R.solid(x - 0.3, z - 0.3, x + 0.3, z + 0.3);
    R.act([x - 0.7, z - 0.7, x + 0.7, z + 0.7], { label: "Look", lines: lines || ["This is a rare POKéMON!", "Huh? It's only a doll!"] });
  }

  Object.assign(BUILD, {
    copycats_house_1f() {
      // FireRed's 1F: her mum on the left, her dad on the right, their CHANSEY by the table, the stairs up at the
      // back on the right, the door at the front on the left.
      const R = new Room("copycats_house_1f", { name: "Copycat's house", floor: "Downstairs", W: 14, D: 11 });
      shell(R, { paper: "paperSaffron", floor: "midBoards", door: [4, 6], entry: true, skirt: "beam", ledge: "walnut" });
      band(R, 1.0, "beam", 0.12);
      kitchen(R, 0, "e", { z: 5.8 });
      sideboard(R, 1.5, 0, "s", 1.6, { lines: ["A cupboard. Inside: a sewing kit and a pile of little doll's clothes."] });
      tvUnit(R, 4.0, 3.0, R.screen("tvcopy", 40, 24, drawFilm, 0.9), { lines: ["Someone on TV is doing impressions. The audience can't tell which is which."] });
      clock(R, 8.3, 2.3);
      stairsUp(R, 10, 0, 2, { to: "copycats_house_2f", spawn: { x: 13.5, z: 2.9, yaw: FACE.s } });
      R.geo.box(3, 0, 3, 9, 0.03, 7, { top: "rugGreen", all: "cap", bottom: null });
      table(R, 4.6, 4.0, 7.4, 6.0, { top: "lab", leg: "walnut", h: 0.86, name: "Table", note: "A white top in a walnut trim, a vase of flowers." });
      for (const z of [4.55, 5.45]) { chair(R, 3.95, z, "e", { seat: "cushion" }); chair(R, 8.05, z, "w", { seat: "cushion" }); }
      plant(R, 12.8, 7.6, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      crossWindow(R, "e", 5.0, 1.6, 1.8, 1.2, WINDOW);
      R.npcs.push({ name: "Copycat's mum", key: "copycats_house_1f_p0", x: 2.6, z: 3.4, yaw: FACE.s, look: { skin: "middle_aged_woman" },
        talk: () => [{ text: "My daughter is so self-centered. She has but a few friends." }] });
      R.npcs.push({ name: "Copycat's dad", key: "copycats_house_1f_p1", x: 9.6, z: 4.6, yaw: FACE.w, look: { skin: "middle_aged_man" },
        talk: () => [{ text: "My daughter likes to mimic people. It's rather vexing." }, { text: "Her mimicry has earned her the nickname COPYCAT around here." }] });
      R.pets = [{ sid: "chansey", x: 4.0, z: 7.4, yaw: 200 }];
      R.act([3.0, 6.4, 5.0, 8.4], { label: "Look", lines: ["CHANSEY: Chaan! Sii!"] });
      doormat(R, 4, 6, { to: "town", mat: "matHouseV" });
      R.spawn = { x: 5.0, z: R.D - 1.5, yaw: FACE.n };
      return R;
    },
    copycats_house_2f() {
      // Her room (FireRed's 2F): her PC in the corner by the window, her dolls, the game on her TV, her bed, her
      // DODUO strutting at the mirror, and her by the window.
      const R = new Room("copycats_house_2f", { name: "Copycat's room", floor: "Upstairs", W: 14, D: 11 });
      shell(R, { paper: "paperCopycat", floor: "sqLight", floorRects: [[0, 0, 10, 11], [10, 2, 14, 11]], skirt: "mcStripped" });
      stairsDown(R, 10, 0, 2, { to: "copycats_house_1f", spawn: { x: 9.1, z: 1.1, yaw: FACE.s }, paper: "paperCopycat" });
      deskPC(R, 0.4, 0, "s", R.screen("pcCopy", 32, 24, drawPC, 1), { lines: ["…", "My Secrets!", "Skill: Mimicry! Hobby: Collecting dolls!", "Favorite POKéMON: CLEFAIRY!"] });
      officeChair(R, 1.55, 1.6, "n", "pinkFabric" in MATDEF ? "pinkFabric" : "blueFabric");
      // (FireRed hides a NUGGET just right of her PC.)
      R.act([2.6, 0.0, 3.8, 1.3], { label: "Look", lines: ["Her doll's clothes, folded in a box."], use: "hidden:nugget:copycats_house_2f_nugget" });
      windowN(R, 4.4, 1.5, 2.2, 2.0, { curtains: "curtain", curtainIn: 0.35 });
      bed(R, 0, 6.6, "e", { w: 1.8, l: 2.9, quilt: "quiltPink" });
      R.geo.box(2.4, 0, 3.6, 7.4, 0.03, 8.2, { top: "rugPink", all: "cap", bottom: null });
      tvUnit(R, 6.6, 3.0, R.screen("tvgame", 40, 24, drawFilm, 0.9), { lines: ["A game with a plumber wearing a bucket on his head!"] });
      doll(R, 4.4, 6.4, "pikachu");
      doll(R, 7.4, 2.4, "jigglypuff");
      doll(R, 3.2, 4.2, "clefairy", 0, ["A CLEFAIRY doll. It's her favourite."]);
      plant(R, 13.0, 9.6, 0.9, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
      R.npcs.push({ name: "Copycat", key: "copycats_house_2f_p0", x: 9.6, z: 6.4, yaw: FACE.w, look: { skin: "brunette_girl" },
        talk: () => [{ text: "{PLAYER}: Hi! Do you like POKéMON?" }, { text: "{PLAYER}: Uh, no, I just asked you." }, { text: "{PLAYER}: Huh? You're strange!" },
          { text: "COPYCAT: Hmm? Quit mimicking? But, that's my favorite hobby!" }, { text: "(Bring her a POKé DOLL, and she teaches you MIMIC.)" }] });
      R.pets = [{ sid: "doduo", x: 6.6, z: 8.8, yaw: 90 }];
      R.act([5.6, 7.8, 7.6, 9.8], { label: "Look", lines: ["DODUO: Giiih!", "PRETTY MIRROR LOOK AT ME, SEE HOW PRETTY I CAN BE?"] });
      R.spawn = { x: 13.5, z: 2.9, yaw: FACE.s };
      return R;
    },
    mr_psychics_house() {
      const R = new Room("mr_psychics_house", { name: "Mr. Psychic's house", floor: "Saffron City", W: 12, D: 11 });
      plainHouse(R, { paper: "paperSaffron", floor: "midBoards", rug: "rugGreen", window: WINDOW, cupboard: ["A cupboard. Inside: a crystal ball wrapped in a cloth."] });
      R.npcs.push({ name: "Mr. Psychic", key: "mr_psychics_house_p0", x: 9.2, z: 4.4, yaw: FACE.w, look: { skin: "fishing_guru" },
        talk: () => [{ text: "...Wait! Don't say a word!" }, { text: "You wanted this!" }, { text: "(He gives you TM29: PSYCHIC.)" },
          { text: "You already know, don't you? TM29 is PSYCHIC. It's an attack that may also lower the target's special stats." }] });
      return R;
    },
  });
})();
