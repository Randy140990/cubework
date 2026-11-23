// ---------- 0.51.0 (09:13, 09:26): Saffron City's FIGHTING DOJO and its GYM, laid out as FireRed's ----------
// Both from pret/pokefirered's own maps (data/layouts, data/maps): a FireRed tile a block, every wall, trainer, pad and
// sign where the game has it, and drawn as Let's Go's.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  PLACES.push({ id: "fighting_dojo", name: "Fighting Dojo", floor: "Saffron City", group: "Saffron City" });
  PLACES.push({ id: "saffron_gym", name: "Saffron Gym", floor: "Saffron City", group: "Saffron City" });

  // The Dojo's: its scrolls (FIGHTING DOJO's two, in brush strokes), its red pillars, its warm boards.
  const scroll = (seed) => (P, r) => {
    P.fill(hx("#F4ECD6"), 0.01, r);
    P.rect(0, 0, P.w, 2, hx("#7A4A2A")); P.rect(0, P.h - 2, P.w, 2, hx("#7A4A2A"));
    const rr = (() => { let s = seed; return () => (s = (s * 9301 + 49297) % 233280) / 233280; })();
    for (let k = 0; k < 4; k++) {
      const y0 = 4 + k * 6, cx = P.w / 2;
      P.rect(cx - 3, y0, 6, 1, hx("#1E1A18")); P.rect(cx - 0.5, y0, 1, 4, hx("#1E1A18"));
      P.rect(cx - 3 + Math.floor(rr() * 3), y0 + 2, 3, 1, hx("#1E1A18")); P.rect(cx + Math.floor(rr() * 2), y0 + 3, 2, 1, hx("#1E1A18"));
    }
    P.rect(P.w / 2 - 1, P.h - 6, 2, 2, hx("#C0302E"));
  };
  reg("dojoScrollL", 16, 32, scroll(7), { fit: true });
  reg("dojoScrollR", 16, 32, scroll(31), { fit: true });
  reg("dojoPillar", 16, 16, (P, r) => {
    P.fill(hx("#B23A30"), 0.008, r); P.rect(0, 0, 1, 16, hx("#D85A4C")); P.rect(15, 0, 1, 16, hx("#7E2420")); P.rect(0, 14, 16, 2, hx("#5E1C18"));
  });
  reg("dojoPillarTop", 16, 16, (P, r) => { P.fill(hx("#F2E2C2"), 0.008, r); P.rect(0, 0, 16, 1, hx("#FFF6E0")); P.rect(0, 15, 16, 1, hx("#C9B38E")); });

  // The Gym's: Let's Go's SAFFRON GYM, a cool lavender hall: a pale blue tiled floor (a block a tile, lit top and left),
  // lavender walls with an orange and white band, and the warp pads, glowing rings on the floor.
  reg("psyFloor", 16, 16, (P, r) => {
    const b = hx("#9DB6E8");
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      let k = (r() - 0.5) * 0.02;
      if (x === 0 || y === 0) k += 0.1; if (x === 15 || y === 15) k -= 0.14;
      if (x === 8 || y === 8) k -= 0.05;
      P.set(x, y, shadec(b, k));
    }
  });
  reg("psyWall", 16, 16, (P, r) => {
    P.fill(hx("#CDB8EA"), 0.006, r); P.rect(7, 0, 1, 16, hx("#BBA4DC")); P.rect(8, 0, 1, 16, hx("#DDCCF4"));
  });
  reg("psyBand", 16, 16, (P, r) => {
    P.fill(hx("#F2F0F6"), 0.004, r);
    for (let x = 0; x < 16; x += 4) P.rect(x, 5, 2, 6, hx("#F08A4A"));
    P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#B9AFCB"));
  });
  reg("psyCap", 16, 16, (P, r) => { P.fill(hx("#8E78B6"), 0.006, r); P.rect(0, 0, 16, 1, hx("#A892CC")); });
  reg("psyPad", 16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
      let c = null;
      if (d < 7.6) c = hx("#2C5FA8");
      if (d < 6.6) c = hx("#7FE3F4");
      if (d < 5.2) c = hx("#E8FCFF");
      if (d < 3.6) c = hx("#9BEAF6");
      if (d < 2.0) c = hx("#FFFFFF");
      if (c) P.set(x, y, c); else P.set(x, y, hx("#9DB6E8"));
    }
  }, { glow: 0.6 });
  reg("psyPadRim", 8, 8, (P, r) => P.fill(hx("#5A84C8"), 0.01, r));

  // A warp pad (FireRed's: step on it and you're on its partner). The arrival pad stays quiet till you step off it.
  window.PAD_LOCK = null;
  function pad(R, x, z, to, key, partner) {
    R.geo.box(x + 0.08, 0, z + 0.08, x + 0.92, 0.06, z + 0.92, { top: "psyPad", all: "psyPadRim", bottom: null });
    (R.pads || (R.pads = [])).push({ x0: x + 0.2, z0: z + 0.2, x1: x + 0.8, z1: z + 0.8, to: { x: to[0] + 0.5, z: to[1] + 0.5 } });
    R.warps.push({
      test: p => {
        const on = p.x > x + 0.2 && p.x < x + 0.8 && p.z > z + 0.2 && p.z < z + 0.8;
        if (!on) { if (window.PAD_LOCK === key) window.PAD_LOCK = null; return false; }
        if (window.PAD_LOCK === key) return false;
        window.PAD_LOCK = partner;
        return true;
      }, to: R.id, spawn: { x: to[0] + 0.5, z: to[1] + 0.5, yaw: FACE.s }, fx: "fade", why: "pad"
    });
  }
  window.warpPad = pad;

  Object.assign(BUILD, {
    fighting_dojo() {
      // FireRed's FIGHTING DOJO (13 by 16): a wooden hall, a long green tatami down its middle, the four BLACK BELTs
      // facing each other in pairs, the KARATE MASTER at the head of the hall between two red pillars, the two POKe BALLs
      // behind him (HITMONLEE and HITMONCHAN: you may take one once you've beaten him), the Dojo's two scrolls on the
      // back wall, two statues by the door.
      const R = new Room("fighting_dojo", { name: "Fighting Dojo", floor: "Saffron City", W: 13, D: 15, H: 4.4 });
      shell(R, { paper: "shoji", floor: "gymBoards", door: [5, 8], skirt: "gymBoardsDark", floorRects: [[1, 3, 12, 15]] });
      band(R, 0.5, "gymBoardsDark", 0.5);
      const G = R.geo;
      // The back of the hall (FireRed's three alcoves): the wall rows 0 to 2, and the side walls a block in.
      G.box(0, 0, 0, 13, 4.4, 3, { s: "shoji", top: "cap", all: null }); R.solid(-5, -5, 18, 3);
      G.box(0, 0, 3, 1, 4.4, 15, { e: "shoji", top: "cap", all: null }); R.solid(-5, 3, 1, 15);
      G.box(12, 0, 3, 13, 4.4, 15, { w: "shoji", top: "cap", all: null }); R.solid(12, 3, 18, 15);
      // The two red pillars either side of the master's alcove (FireRed's x 4 and 8, rows 3 to 6).
      for (const x of [4, 8]) { G.box(x + 0.1, 0, 3, x + 0.9, 4.0, 6.9, { all: "dojoPillar", top: "dojoPillarTop" }); R.solid(x + 0.1, 3, x + 0.9, 6.9); }
      // The scrolls on the back wall (FireRed's signs at 5,2 and 7,2).
      G.box(5.2, 1.2, 3, 5.8, 3.4, 3.02, { s: "dojoScrollL", all: null });
      G.box(7.2, 1.2, 3, 7.8, 3.4, 3.02, { s: "dojoScrollR", all: null });
      R.act([5, 3, 6, 4.2], { label: "Read", lines: ["ENEMIES ON EVERY SIDE!"] });
      R.act([7, 3, 8, 4.2], { label: "Read", lines: ["GOALS LEAD TO SUCCESS!"] });
      // The tatami down the middle of the hall (FireRed's green squares, x 4 to 8, rows 7 to 12).
      G.box(4, 0, 7, 9, 0.04, 13, { top: "tatamiEdge", all: null });
      G.box(4.15, 0.04, 7.15, 8.85, 0.05, 12.85, { top: "tatami", all: null });
      // The statues by the door (FireRed's 3,13 and 9,13).
      for (const x of [3.5, 9.5]) gymStatue(R, x, 13.5, ["FIGHTING DOJO", "The place where the toughest of fighters train."]);
      for (const [x, z] of [[1.6, 3.6], [11.4, 3.6]]) plant(R, x, z, 0.9, { pot: "potWhite", lines: ["A bonsai in a white pot."] });
      R.item("Fighting Dojo", "FireRed's: a wooden hall, a green tatami down its middle, the master's alcove between two red pillars.");
      const T = (name, key, x, z, f, line) => R.npcs.push({ name, key, role: "trainer", x: x + 0.5, z: z + 0.5, yaw: FACE[f], look: { skin: "black_belt" }, talk: () => [{ text: line }] });
      T("Black Belt Hitoshi", "fighting_dojo_4", 11, 10, "w", "Hwa! Arrgh! Beaten!");
      T("Black Belt Hideki", "fighting_dojo_2", 1, 10, "e", "Nothing tough frightens me! I break boulders for training!");
      T("Black Belt Aaron", "fighting_dojo_3", 11, 8, "w", "Hoohah! You're trespassing in our FIGHTING DOJO!");
      T("Black Belt Mike", "fighting_dojo_1", 1, 8, "e", "I hear you're good! Show me!");
      R.npcs.push({ name: "Karate Master Koichi", key: "fighting_dojo_0", role: "trainer", x: 6.5, z: 5.5, yaw: FACE.s, look: { skin: "black_belt" },
        talk: () => [{ text: "Grunt! I am the KARATE MASTER! I am the LEADER here!" }, { text: "You wish to challenge us? Expect no mercy! Fwaaa!" }] });
      // The two POKe BALLs behind him (FireRed's 5,3 and 7,3).
      R.pets = [{ sid: "poke_ball", x: 5.5, z: 3.5, yaw: 0 }, { sid: "poke_ball", x: 7.5, z: 3.5, yaw: 0 }];
      R.act([5, 3, 6, 4.4], { label: "Look", lines: ["It's HITMONLEE, the kicking fiend!", "(Once you've beaten the KARATE MASTER: take it, and the other one stays.)"] });
      R.act([7, 3, 8, 4.4], { label: "Look", lines: ["It's HITMONCHAN, the piercing puncher!", "(Once you've beaten the KARATE MASTER: take it, and the other one stays.)"] });
      doormat(R, 5, 8, { to: "town", mat: "matSchool" });
      R.spawn = { x: 6.5, z: 13.6, yaw: FACE.n };
      return R;
    },

    saffron_gym() {
      // FireRed's SAFFRON GYM (29 by 24): nine rooms in three rows of three, no doors between them, only the warp pads,
      // each one sending you to its partner (FireRed's own pairs, from its map). SABRINA in the middle room; her seven
      // PSYCHICs and CHANNELERs in the others; the way in at the bottom middle, the GYM GUIDE there and two statues.
      const R = new Room("saffron_gym", { name: "Saffron Gym", floor: "Saffron City", W: 29, D: 24, H: 3.2 });
      const rooms = [];
      for (const [z0, z1] of [[2, 8], [10, 16], [18, 24]]) for (const [x0, x1] of [[0, 9], [10, 19], [20, 29]]) rooms.push([x0, z0, x1, z1]);
      shell(R, { paper: "psyWall", floor: "psyFloor", door: [13, 16], skirt: "psyCap", floorRects: rooms, rail: false });
      const G = R.geo;
      // The walls between the rooms (FireRed's rows 0 to 1, 8 to 9 and 16 to 17, and the columns 9 and 19): lavender,
      // an orange and white band, their tops capped.
      const wall = (x0, z0, x1, z1) => {
        G.box(x0, 0, z0, x1, 2.4, z1, { all: "psyWall", top: "psyCap", bottom: null });
        G.box(x0 - 0.01, 0.9, z0 - 0.01, x1 + 0.01, 1.2, z1 + 0.01, { all: "psyBand", top: null, bottom: null });
        R.solid(x0, z0, x1, z1);
      };
      wall(0, 0, 29, 2); wall(0, 8, 29, 10); wall(0, 16, 29, 18);
      for (const [z0, z1] of [[2, 8], [10, 16], [18, 24]]) { wall(9, z0, 10, z1); wall(19, z0, 20, z1); }
      // The entrance room's statues (FireRed's 12,20 and 16,20).
      for (const x of [12.5, 16.5]) gymStatue(R, x, 20.5, ["SAFFRON CITY POKéMON GYM", "LEADER: SABRINA", "WINNING TRAINERS: {rival}"]);
      // The warp pads: FireRed's, each with its partner (index: x, y, the pad it sends you to).
      const P = [null, null, null, [18, 20, 32], [0, 4, 22], [0, 7, 20], [0, 12, 30], [0, 15, 11], [0, 20, 28], [0, 23, 19], [8, 4, 18], [8, 7, 7], [8, 12, 17],
        [8, 15, 15], [8, 20, 26], [8, 23, 13], [12, 4, 24], [12, 7, 12], [16, 4, 10], [16, 7, 9], [18, 15, 5], [20, 4, 23], [20, 7, 4], [20, 12, 21], [20, 15, 16],
        [20, 20, 27], [20, 23, 14], [28, 4, 25], [28, 7, 8], [28, 12, 31], [28, 15, 6], [28, 20, 29], [28, 23, 3]];
      for (let i = 3; i < P.length; i++) {
        const [x, z, to] = P[i], d = P[to];
        pad(R, x, z, [d[0], d[1]], "sg" + i, "sg" + to);
      }
      R.item("Warp pads", "FireRed's: step on one and you're on its partner, in another room. Find the way to SABRINA in the middle.");
      const T = (name, key, x, z, skin, line) => R.npcs.push({ name, key, role: "trainer", x: x + 0.5, z: z + 0.5, yaw: FACE.s, look: { skin }, talk: () => [{ text: line }] });
      T("Psychic Cameron", "saffron_gym_2", 24, 18, "psychic_m", "SAFFRON POKéMON GYM is famous for its psychics! You want to see SABRINA! I can tell!");
      T("Psychic Johan", "saffron_gym_4", 24, 2, "psychic_m", "POKéMON take on the appearance of their trainers. Your POKéMON must be tough, then!");
      T("Psychic Preston", "saffron_gym_6", 4, 2, "psychic_m", "You know that power alone isn't enough!");
      T("Channeler Amanda", "saffron_gym_1", 14, 2, "channeler", "SABRINA is young, but she's also our LEADER! You won't reach her easily!");
      T("Channeler Stacy", "saffron_gym_3", 4, 10, "channeler", "I see a frightening ghostly vision!");
      T("Channeler Tasha", "saffron_gym_5", 4, 18, "channeler", "There used to be two POKéMON GYMs in SAFFRON.");
      T("Psychic Tyron", "saffron_gym_7", 24, 10, "psychic_m", "I simply love it when someone tries to read my mind!");
      R.npcs.push({ name: "Leader Sabrina", key: "saffron_gym_0", role: "leader", x: 14.5, z: 11.5, yaw: FACE.s, look: { skin: "leader_sabrina" },
        talk: () => [{ text: "SABRINA: I had a vision of your arrival!" }, { text: "(In the game: you battle her, and win the MARSHBADGE.)" }] });
      R.npcs.push({ name: "Gym guide", key: "saffron_gym_p0", x: 15.5, z: 20.5, yaw: FACE.s, look: { skin: "gym_guide" },
        talk: () => [{ text: "Yo! Champ in making!" }, { text: "SABRINA's POKéMON use psychic power instead of force." },
          { text: "The GYM is full of warp pads. Step on one and you're whisked off to another room. Find the way to SABRINA!" }] });
      doormat(R, 13, 16, { to: "town", mat: "matLabPurple" });
      R.spawn = { x: 14.5, z: 22.5, yaw: FACE.n };
      return R;
    },
  });
})();
