// ---------- 0.53.0: SILPH CO. 1F to 11F and its elevator, as FireRed's (pret/pokefirered) ----------
// Every floor is FireRed's own map, a FireRed tile a block (tools/silph.py reads the layouts, warps, card-key doors,
// trainers, people, items and signs into silph_data.js). The walls, desks, machines, sofas, plants, the 1F fountains,
// the boardroom's table, the warp pads (in the floor, or to another floor), the stairs between floors, the elevator
// and its doors on every floor, and FireRed's card-key shutters. Drawn as the building's outside is (Let's Go's
// SILPH CO.): white cladding panels, metal, Silph blue, pale blue-grey tiles.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const ORDER = ["silph_co_1f", "silph_co_2f", "silph_co_3f", "silph_co_4f", "silph_co_5f", "silph_co_6f", "silph_co_7f", "silph_co_8f", "silph_co_9f",
    "silph_co_10f", "silph_co_11f", "silph_co_elevator"];
  for (const id of ORDER) PLACES.push({ id, name: "Silph Co.", floor: SILPH[id].name, group: "Saffron City" });
  const TALL = 2.0, LOW = 0.5;

  // ----- Materials: the outside's white cladding and Silph blue, inside -----
  reg("silphFloor", 16, 16, (P, r) => {
    const b = hx("#A9C0D2");
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      let k = (r() - 0.5) * 0.015;
      if (x === 0 || y === 0) k -= 0.1; if (x === 1 || y === 1) k += 0.05;
      if (x + y > 18 && x + y < 22) k += 0.035;
      P.set(x, y, shadec(b, k));
    }
  });
  reg("silphFloorEdge", 16, 16, (P, r) => { P.fill(hx("#9FB3C2"), 0.01, r); P.rect(0, 0, 16, 1, hx("#C5D4DF")); });
  reg("silphWall", 16, 16, (P, r) => {
    P.fill(hx("#F1F3F5"), 0.006, r);
    P.rect(0, 0, 1, 16, hx("#D6DDE4")); P.rect(15, 0, 1, 16, hx("#FFFFFF")); P.rect(0, 7, 16, 1, hx("#E1E6EB"));
  });
  reg("silphBand", 16, 16, (P, r) => {
    P.fill(hx("#2F6FB6"), 0.006, r); P.rect(0, 0, 16, 2, hx("#FFFFFF")); P.rect(0, 3, 16, 1, hx("#5C93D1")); P.rect(0, 14, 16, 2, hx("#E3E8EE"));
  });
  reg("silphSkirt", 16, 16, (P, r) => { P.fill(hx("#8195A8"), 0.008, r); P.rect(0, 0, 16, 2, hx("#A8B8C7")); P.rect(0, 15, 16, 1, hx("#64788B")); });
  reg("silphCap", 16, 16, (P, r) => { P.fill(hx("#5D6F81"), 0.006, r); P.rect(0, 0, 16, 1, hx("#7D90A2")); P.rect(0, 15, 16, 1, hx("#4A5A6A")); });
  reg("silphDesk", 16, 16, (P, r) => { P.fill(hx("#F5F7F9"), 0.004, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#CCD5DD")); });
  reg("silphDeskSide", 16, 16, (P, r) => { P.fill(hx("#DCE3EA"), 0.006, r); P.rect(0, 0, 16, 2, hx("#2F6FB6")); P.rect(0, 14, 16, 2, hx("#9AA9B7")); });
  reg("silphMachine", 16, 16, (P, r) => {
    P.fill(hx("#8D9FB1"), 0.01, r); P.rect(0, 0, 16, 1, hx("#B5C3D0")); P.rect(0, 15, 16, 1, hx("#62748A"));
    for (const y of [9, 11, 13]) P.rect(3, y, 10, 1, hx("#6E8197"));
  });
  reg("silphMachineTop", 16, 16, (P, r) => { P.fill(hx("#A6B5C3"), 0.008, r); P.rect(1, 1, 14, 14, hx("#97A7B7")); });
  reg("silphScreen", 16, 16, (P, r) => {
    P.fill(hx("#1E3554"), 0.01, r); P.rect(1, 1, 14, 9, hx("#3F86C9"));
    for (const y of [3, 5, 7]) P.rect(3, y, 6 + (y % 3), 1, hx("#BFE3FF"));
    P.set(12, 12, hx("#7CF5B0")); P.set(10, 12, hx("#F5D76E"));
  }, { glow: 0.5 });
  reg("silphCounter", 16, 16, (P, r) => {
    P.fill(hx("#EEF2F6"), 0.005, r); P.rect(0, 5, 16, 3, hx("#2F6FB6")); P.rect(0, 8, 16, 1, hx("#5C93D1")); P.rect(0, 15, 16, 1, hx("#B9C5D0"));
  });
  reg("silphSofa", 16, 16, (P, r) => { P.fill(hx("#4D79B8"), 0.03, r); P.rect(0, 0, 16, 1, hx("#7FA3D6")); P.rect(0, 15, 16, 1, hx("#355A8E")); });
  reg("silphSofaOrange", 16, 16, (P, r) => { P.fill(hx("#D9763A"), 0.03, r); P.rect(0, 0, 16, 1, hx("#F0A06A")); P.rect(0, 15, 16, 1, hx("#A8552A")); });
  reg("silphRim", 16, 16, (P, r) => { P.fill(hx("#F4F6F8"), 0.005, r); P.rect(0, 0, 16, 2, hx("#FFFFFF")); P.rect(0, 14, 16, 2, hx("#C3CDD6")); });
  reg("silphRimTop", 16, 16, (P, r) => { P.fill(hx("#FAFBFC"), 0.004, r); P.rect(0, 0, 16, 1, hx("#DDE4EA")); P.rect(0, 15, 16, 1, hx("#DDE4EA")); });
  reg("silphWater", 16, 16, (P, r) => {
    P.fill(hx("#5FA7DE"), 0.02, r);
    for (let i = 0; i < 5; i++) { const y = 2 + i * 3, x = Math.floor(r() * 9); P.rect(x, y, 5, 1, hx("#A9D8F6")); }
  }, { glow: 0.15 });
  reg("silphStep", 16, 16, (P, r) => { P.fill(hx("#E6EBF0"), 0.006, r); P.rect(0, 13, 16, 3, hx("#2F6FB6")); P.rect(0, 12, 16, 1, hx("#9DB4C9")); });
  reg("silphStepSide", 16, 16, (P, r) => { P.fill(hx("#C3CEDA"), 0.006, r); P.rect(0, 0, 16, 1, hx("#E9EEF2")); });
  reg("silphWell", 16, 16, (P, r) => { P.fill(hx("#55677A"), 0.01, r); P.rect(0, 0, 16, 1, hx("#7B8DA0")); });
  reg("silphLiftDoor", 16, 32, (P, r) => {
    P.fill(hx("#BFC8D1"), 0.01, r);
    for (let y = 0; y < 32; y++) { P.set(7, y, hx("#6F7C89")); P.set(8, y, hx("#E7ECF0")); }
    P.rect(0, 0, 16, 1, hx("#E7ECF0")); P.rect(0, 0, 1, 32, hx("#8C98A5")); P.rect(15, 0, 1, 32, hx("#8C98A5"));
    P.rect(2, 14, 1, 4, hx("#8C98A5")); P.rect(13, 14, 1, 4, hx("#8C98A5"));
  }, { fit: true });
  reg("silphLiftSign", 16, 8, (P, r) => {
    P.fill(hx("#1E2630"), 0.01, r);
    P.rect(3, 2, 1, 4, hx("#FFB04A")); P.rect(2, 3, 3, 1, hx("#FFB04A")); P.set(3, 1, hx("#FFB04A"));
    P.rect(11, 2, 1, 4, hx("#FFB04A")); P.rect(10, 4, 3, 1, hx("#FFB04A")); P.set(11, 6, hx("#FFB04A"));
  }, { fit: true, glow: 0.7 });
  reg("silphLiftWall", 16, 16, (P, r) => {
    P.fill(hx("#B9C3CD"), 0.01, r);
    for (let y = 0; y < 16; y++) if (r() < 0.6) P.rect(0, y, 16, 1, shadec(hx("#B9C3CD"), (r() - 0.5) * 0.05));
    P.rect(0, 0, 1, 16, hx("#E2E8ED")); P.rect(15, 0, 1, 16, hx("#8F9BA7"));
  });
  reg("silphLiftFloor", 16, 16, (P, r) => {
    P.fill(hx("#7D8996"), 0.02, r);
    for (let y = 1; y < 16; y += 4) for (let x = (y % 8 === 1 ? 1 : 3); x < 16; x += 4) P.rect(x, y, 2, 1, hx("#A3AEBA"));
  });
  reg("silphButtons", 16, 24, (P, r) => {
    P.fill(hx("#D7DEE5"), 0.006, r); P.rect(0, 0, 16, 1, hx("#F2F5F8")); P.rect(0, 23, 16, 1, hx("#9CA8B4"));
    for (let i = 0; i < 6; i++) for (let j = 0; j < 2; j++) P.rect(4 + j * 6, 3 + i * 3, 2, 2, hx(i === 0 && j === 0 ? "#FFB04A" : "#4C6E92"));
  }, { fit: true, glow: 0.3 });
  reg("silphShutter", 16, 16, (P, r) => {
    P.fill(hx("#9AA6B2"), 0.01, r);
    for (let y = 0; y < 16; y += 3) { P.rect(0, y, 16, 1, hx("#C4CDD6")); P.rect(0, y + 2, 16, 1, hx("#76828F")); }
  });
  reg("silphHazard", 16, 16, (P, r) => { P.fill(hx("#2E3136"), 0, r); for (let x = -16; x < 16; x += 6) for (let i = 0; i < 3; i++) P.line(x + i, 15, x + i + 15, 0, hx("#F2C230")); });
  reg("silphLock", 8, 8, (P, r) => { P.fill(hx("#2A3542"), 0.01, r); P.rect(2, 2, 4, 2, hx("#E2484A")); P.rect(2, 5, 4, 1, hx("#7CF5B0")); }, { fit: true, glow: 0.6 });
  reg("silphPicture", 16, 12, (P, r) => {
    P.fill(hx("#8A7356"), 0, r); P.rect(1, 1, 14, 10, hx("#9BD3F0")); P.rect(1, 7, 14, 4, hx("#6DBA5C"));
    P.rect(4, 4, 4, 3, hx("#F4F6F8")); P.rect(9, 5, 5, 2, hx("#4E9A48"));
  }, { fit: true });
  reg("silphBoard", 16, 12, (P, r) => {
    P.fill(hx("#B9C5D0"), 0, r); P.rect(1, 1, 14, 10, hx("#FBFCFD"));
    for (const y of [3, 5, 7, 9]) P.rect(3, y, 6 + (y % 4), 1, hx("#7D8EA0")); P.rect(11, 3, 3, 3, hx("#2F6FB6"));
  }, { fit: true });
  reg("silphPlaque", 16, 8, (P, r) => {
    P.fill(hx("#2F6FB6"), 0, r); P.rect(1, 1, 14, 6, hx("#F4F6F8"));
    for (const x of [3, 5, 7, 9, 11]) P.rect(x, 3, 1, 2, hx("#2F6FB6"));
  }, { fit: true });
  reg("silphBust", 16, 16, (P, r) => { P.fill(hx("#ECEFF2"), 0.01, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); });

  const FACEOF = { MOVEMENT_TYPE_FACE_UP: "n", MOVEMENT_TYPE_FACE_DOWN: "s", MOVEMENT_TYPE_FACE_LEFT: "w", MOVEMENT_TYPE_FACE_RIGHT: "e",
    MOVEMENT_TYPE_LOOK_AROUND: null, up: "n", down: "s", left: "w", right: "e" };
  const floorNo = id => { const m = /silph_co_(\d+)f/.exec(id); return m ? +m[1] : 0; };

  // A warp pad to another floor (FireRed's): step on it and you're on its partner there.
  function crossPad(R, x, z, to, tx, tz, key, partner) {
    R.geo.box(x + 0.08, 0, z + 0.08, x + 0.92, 0.06, z + 0.92, { top: "psyPad", all: "psyPadRim", bottom: null });
    (R.pads || (R.pads = [])).push({ x0: x + 0.2, z0: z + 0.2, x1: x + 0.8, z1: z + 0.8, to: { x: tx + 0.5, z: tz + 0.5 }, room: to });
    R.warps.push({
      test: p => {
        const on = p.x > x + 0.2 && p.x < x + 0.8 && p.z > z + 0.2 && p.z < z + 0.8;
        if (!on) { if (window.PAD_LOCK === key) window.PAD_LOCK = null; return false; }
        if (window.PAD_LOCK === key) return false;
        window.PAD_LOCK = partner;
        return true;
      }, to, spawn: { x: tx + 0.5, z: tz + 0.5, yaw: FACE.s }, fx: "fade", why: "pad"
    });
  }

  // Connected pieces of one tile kind (a desk, a sofa, the fountain).
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

  function build(id) {
    const F = SILPH[id], W = F.W, D = F.D, lift = id === "silph_co_elevator";
    const R = new Room(id, { name: "Silph Co.", floor: F.name, W, D, H: 3.2 });
    const G = R.geo;
    const at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "o" : F.rows[z][x];
    const warpAt = {}; for (const w of F.warps) warpAt[w.x + "," + w.z] = w;
    // Furniture inside a wall's band (pictures, phones, the floor signs, the elevator's lights): part of the wall.
    const mounted = new Set();
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) if ("dmc".includes(at(x, z)) && (at(x, z + 1) === "w" || at(x, z + 1) === "e")) mounted.add(x + "," + z);
    const isWall = (x, z) => at(x, z) === "w" || mounted.has(x + "," + z) || (lift && at(x, z) === "m");
    const walkable = (x, z) => { const c = at(x, z); return c === "." || c === "x" || c === "e" || c === "X" || (c === "u" && !!warpAt[x + "," + z]); };
    R._floors = [];
    R._boxes = [];
    R._keys = [];
    R.balls = [];

    // (0.61 fit-out, John's 17:53: "when it comes to water on the inside ... give the water the same effect as the
    // outside") A fountain's water a block down, as the outside's water lies: its basin's floor below, the water's
    // surface just under the floor's, seen into.
    const fountainWater = (a, b) => {
      if (at(a, b) !== "f") return false;
      for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [-1, 1], [1, -1], [-1, -1]]) if (at(a + dx, b + dz) !== "f") return false;
      return true;
    };
    // The floor (a slab, so a stairwell going down shows its sides), and where it's solid.
    const downCells = new Set();
    for (const w of F.warps) if (w.kind === "stairs" && floorNo(w.to) < floorNo(id)) {
      for (const pc of pieces(F, "u")) if (pc.cells.some(([a, b]) => a === w.x && b === w.z)) for (const [a, b] of pc.cells) downCells.add(a + "," + b);
    }
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (at(x, z) === "v") { x++; continue; }
        let x1 = x; while (x1 < W && at(x1, z) !== "v") x1++;
        R._floors.push([x, z, x1, z + 1]);
        for (let a = x; a < x1; a++) if (!downCells.has(a + "," + z) && !(FIT && fountainWater(a, z))) G.box(a, -1.2, z, a + 1, 0, z + 1, { top: lift ? "silphLiftFloor" : "silphFloor", all: "silphFloorEdge", bottom: null });
        x = x1;
      }
      // Solids: everything you can't walk on, in runs.
      x = 0;
      while (x < W) {
        if (walkable(x, z)) { x++; continue; }
        let x1 = x; while (x1 < W && !walkable(x1, z)) x1++;
        R.solid(x, z, x1, z + 1); x = x1;
      }
    }
    R.solid(-1.5, -1, 0, D); R.solid(W, -1, W + 1.5, D); R.solid(-1.5, -1.5, W + 1.5, 0);

    // The walls: tall where you see their face, low at the back of a two-deep one (so a room north of it isn't hidden).
    const wallH = (x, z) => {
      const s = at(x, z + 1), n = at(x, z - 1);
      if (!isWall(x, z + 1) && s !== "v") return TALL;
      if (isWall(x, z + 1) && !isWall(x, z - 1) && n !== "v" && n !== "o") return LOW;
      return TALL;
    };
    const skin = lift ? "silphLiftWall" : "silphWall";
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (!isWall(x, z)) { x++; continue; }
        const h = wallH(x, z);
        let x1 = x; while (x1 < W && isWall(x1, z) && wallH(x1, z) === h) x1++;
        G.box(x, 0, z, x1, 0.18, z + 1, { all: lift ? skin : "silphSkirt", top: null, bottom: null });
        if (h === TALL && !lift) {
          G.box(x, 0.18, z, x1, 0.9, z + 1, { all: skin, top: null, bottom: null });
          G.box(x, 0.9, z, x1, 1.08, z + 1, { all: "silphBand", top: null, bottom: null });
          G.box(x, 1.08, z, x1, h, z + 1, { all: skin, top: "silphCap", bottom: null });
        } else G.box(x, 0.18, z, x1, h, z + 1, { all: skin, top: "silphCap", bottom: null });
        x = x1;
      }
    }
    // The building's sides, where a floor runs to the map's edge.
    for (const [ex, a0, a1, face] of [[0, -0.3, 0, "e"], [W - 1, W, W + 0.3, "w"]]) {
      let z = 0;
      while (z < D) {
        if (at(ex, z) === "v" || isWall(ex, z)) { z++; continue; }
        let z1 = z; while (z1 < D && at(ex, z1) !== "v" && !isWall(ex, z1)) z1++;
        G.box(a0, 0, z, a1, TALL, z1, { [face]: skin, top: "silphCap", all: null });
        z = z1;
      }
    }

    // On the walls: FireRed's pictures, phones and boards, on the face below.
    const faceBelow = (x, z) => { let zz = z + 1; while (zz < D && isWall(x, zz)) zz++; return zz; };
    for (const k of mounted) {
      const [x, z] = k.split(",").map(Number), c = at(x, z), pz = faceBelow(x, z);
      if (at(x, pz) === "e" || wallH(x, pz - 1) !== TALL) continue;
      const mat = c === "m" ? "silphPicture" : "silphBoard", y0 = c === "m" ? 1.25 : 1.3;
      G.box(x + 0.14, y0, pz, x + 0.86, y0 + 0.54, pz + 0.04, { s: mat, all: "silphSkirt", n: null });
    }

    // Plants (FireRed's are two tiles tall: one pot for the pair).
    const used = new Set();
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
      if (at(x, z) !== "p" || used.has(x + "," + z)) continue;
      used.add(x + "," + z);
      if (at(x, z + 1) === "p" && !used.has(x + "," + (z + 1))) { used.add(x + "," + (z + 1)); plant(R, x + 0.5, z + 1.0, 1.15, { pot: "potWhite", lines: ["A tall leafy plant in a white pot."] }); }
      else plant(R, x + 0.5, z + 0.5, 0.95, { pot: "potWhite", lines: ["A leafy plant in a white pot."] });
    }

    // The furniture, piece by piece.
    const ps = [];
    for (const p of pieces(F, "dmkstfc")) {
      const cells = p.cells.filter(([a, b]) => !mounted.has(a + "," + b));
      if (!cells.length || lift && p.c === "m") continue;
      const xs = cells.map(c => c[0]), zs = cells.map(c => c[1]);
      ps.push({ c: p.c, cells, x0: Math.min(...xs), x1: Math.max(...xs) + 1, z0: Math.min(...zs), z1: Math.max(...zs) + 1 });
    }
    const touches = (p, code) => p.cells.some(([a, b]) => [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dz]) => at(a + dx, b + dz) === code));
    for (const p of ps) {
      const { x0, x1, z0, z1, cells } = p;
      if (p.c === "d") {
        G.box(x0 + 0.06, 0, z0 + 0.06, x1 - 0.06, 0.78, z1 - 0.06, { all: "silphDeskSide", top: "silphDesk" });
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A white desk, its papers in neat piles."] });
      } else if (p.c === "m") {
        if (touches(p, "d")) {
          // A PC on a desk.
          G.box(x0 + 0.06, 0, z0 + 0.06, x1 - 0.06, 0.78, z1 - 0.06, { all: "silphDeskSide", top: "silphDesk" });
          for (const [a, b] of cells) {
            G.box(a + 0.2, 0.78, b + 0.3, a + 0.8, 1.34, b + 0.7, { s: "silphScreen", all: "silphMachine", top: "silphMachineTop" });
            G.box(a + 0.42, 0.78, b + 0.7, a + 0.58, 0.84, b + 0.85, "silphMachine");
          }
          R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A SILPH CO. PC. Its screen is full of research figures."] });
        } else {
          // A machine: a cabinet with its lit screen.
          G.box(x0 + 0.05, 0, z0 + 0.08, x1 - 0.05, 1.5, z1 - 0.04, { all: "silphMachine", top: "silphMachineTop" });
          for (let a = x0; a < x1; a++) G.box(a + 0.18, 0.85, z1 - 0.04, a + 0.82, 1.3, z1 - 0.02, { s: "silphScreen", all: null });
          R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A SILPH CO. machine, humming. Its lights blink in a pattern."] });
        }
      } else if (p.c === "k") {
        for (const [a, b] of cells) G.box(a, 0, b, a + 1, 1.0, b + 1, { all: "silphCounter", top: "silphDesk" });
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["The counter. SILPH CO.'s blue band runs along it."] });
      } else if (p.c === "s") {
        const mat = id === "silph_co_11f" ? "silphSofaOrange" : "silphSofa";
        G.box(x0 + 0.06, 0, z0 + 0.06, x1 - 0.06, 0.45, z1 - 0.06, { all: mat });
        G.box(x0 + 0.06, 0.45, z0 + 0.06, x1 - 0.06, 0.95, z0 + 0.4, { all: mat });
        if (x1 - x0 > 1) for (const ax of [x0 + 0.06, x1 - 0.3]) G.box(ax, 0.45, z0 + 0.4, ax + 0.24, 0.66, z1 - 0.06, { all: mat });
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A soft sofa for visitors."] });
      } else if (p.c === "t") {
        table(R, x0 + 0.1, z0 + 0.1, x1 - 0.1, z1 - 0.1, { top: "walnut", leg: "walnut", h: 0.78, name: "The boardroom table", note: "SILPH CO.'s boardroom table, walnut, a white bust in its middle." });
        const cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        if (x1 - x0 > 3) { G.box(cx - 0.18, 0.78, cz - 0.18, cx + 0.18, 0.9, cz + 0.18, "silphBust"); G.box(cx - 0.12, 0.9, cz - 0.1, cx + 0.12, 1.25, cz + 0.1, "silphBust"); G.box(cx - 0.1, 1.25, cz - 0.1, cx + 0.1, 1.45, cz + 0.1, "silphBust"); }
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["A long walnut table. A bust of SILPH's founder stands on it."] });
      } else if (p.c === "c") {
        for (const [a, b] of cells) {
          G.box(a + 0.1, 0, b + 0.12, a + 0.9, 0.62, b + 0.88, "cardboard");
          G.box(a + 0.2, 0.62, b + 0.22, a + 0.78, 1.05, b + 0.8, "cardboard");
        }
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["Boxes of new POKé BALLS, still taped shut."] });
      } else if (p.c === "f") {
        // The fountain: a white rim, the water inside, a spout in each basin's middle.
        const inF = (a, b) => at(a, b) === "f";
        const dist = {};
        for (const [a, b] of cells) {
          let d = 0; while (d < 6 && [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [-1, 1], [1, -1], [-1, -1]].every(([dx, dz]) => inF(a + dx * (d + 1), b + dz * (d + 1)))) d++;
          dist[a + "," + b] = d;
          if (d === 0) G.box(a, 0, b, a + 1, 0.42, b + 1, { all: "silphRim", top: "silphRimTop" });
          else if (FIT) { G.box(a, -1.2, b, a + 1, -1.0, b + 1, { top: "poolBottomView", all: null }); G.box(a, -0.13, b, a + 1, -0.12, b + 1, { top: "fitWater", all: null }); }
          else G.box(a, 0, b, a + 1, 0.26, b + 1, { top: "silphWater", all: "silphRim" });
        }
        const best = Math.max(...Object.values(dist));
        const peaks = cells.filter(([a, b]) => dist[a + "," + b] === best && best > 0), spouts = [];
        for (const [a, b] of peaks) if (!spouts.some(([sa, sb]) => Math.hypot(sa - a, sb - b) < 3)) spouts.push([a, b]);
        for (const [a, b] of spouts) {
          const cx = a + 0.5, cz = b + 0.5;
          G.box(cx - 0.35, 0.26, cz - 0.35, cx + 0.35, 0.55, cz + 0.35, { all: "silphRim", top: "silphRimTop" });
          G.box(cx - 0.12, 0.55, cz - 0.12, cx + 0.12, 1.15, cz + 0.12, "silphRim");
          G.box(cx - 0.22, 1.15, cz - 0.22, cx + 0.22, 1.24, cz + 0.22, { all: "silphRim", top: "silphWater" });
          G.box(cx - 0.06, 1.24, cz - 0.06, cx + 0.06, 1.55, cz + 0.06, { all: "silphWater" });
        }
        R.act([x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3], { label: "Look", lines: ["SILPH CO.'s fountains. Their water sparkles."] });
        R.item("Fountains", "FireRed's two fountains in the lobby: a white rim, the water, a spout in the middle of each.");
      }
    }

    // The stairs between floors (FireRed's), going up or down into their alcove.
    for (const w of F.warps.filter(w => w.kind === "stairs")) {
      const pc = pieces(F, "u").find(p => p.cells.some(([a, b]) => a === w.x && b === w.z));
      const down = floorNo(w.to) < floorNo(id);
      if (pc) for (const [a, b] of pc.cells) {
        const step = (pc.z1 - b) * 0.34;
        if (down) G.box(a, -1.2, b, a + 1, -step, b + 1, { top: "silphStep", all: "silphWell", bottom: null });
        else G.box(a, 0, b, a + 1, step, b + 1, { top: "silphStep", all: "silphStepSide", bottom: null });
      }
      // You come out one tile in front of the far floor's stairs, facing away from them.
      const T = SILPH[w.to], free = (x, z) => x >= 0 && z >= 0 && x < T.W && z < T.D && ".x".includes(T.rows[z][x]);
      let sx = w.tx, sz = w.tz + 1;
      for (const [dx, dz] of [[0, 1], [-1, 1], [1, 1], [0, 2], [-1, 0], [1, 0]]) if (free(w.tx + dx, w.tz + dz)) { sx = w.tx + dx; sz = w.tz + dz; break; }
      const spawn = { x: sx + 0.5, z: sz + 0.5, yaw: FACE.s };
      R._boxes.push({ x0: w.x, z0: w.z, x1: w.x + 1, z1: w.z + 0.85, to: w.to, spawn });
      R.warps.push({ test: p => p.x > w.x && p.x < w.x + 1 && p.z > w.z && p.z < w.z + 0.85, to: w.to, spawn, fx: "fade" });
      R.item(down ? "Stairs down" : "Stairs up", "FireRed's stairs to the next floor, in their alcove: walk onto them to go " + (down ? "down." : "up."));
    }

    // The warp pads: to their partner on this floor, or on another.
    for (const w of F.warps.filter(w => w.kind === "pad")) {
      const key = id + ":" + w.x + "," + w.z, partner = w.to + ":" + w.tx + "," + w.tz;
      if (w.to === id) window.warpPad(R, w.x, w.z, [w.tx, w.tz], key, partner);
      else crossPad(R, w.x, w.z, w.to, w.tx, w.tz, key, partner);
    }
    if (F.warps.some(w => w.kind === "pad")) R.item("Warp pads", "FireRed's: step on one and you're on its partner, on this floor or another.");

    // The elevator's doors (steel, its arrows lit over them): walk in and you're in the elevator.
    for (const w of F.warps.filter(w => w.kind === "elevator")) {
      const x = w.x, z = w.z;
      G.box(x, 0, z, x + 1, 1.9, z + 0.06, { s: "silphLiftDoor", all: "silphSkirt" });
      G.box(x - 0.08, 0, z, x, 2.0, z + 1, { e: "silphCap", all: null }); G.box(x + 1, 0, z, x + 1.08, 2.0, z + 1, { w: "silphCap", all: null });
      G.box(x + 0.15, 1.92, z, x + 0.85, 2.0, z + 0.03, { s: "silphLiftSign", all: null });
      const spawn = { x: 2.5, z: 5.2, yaw: FACE.n };
      R._boxes.push({ x0: x + 0.1, z0: z, x1: x + 0.9, z1: z + 0.8, to: "silph_co_elevator", spawn });
      R.warps.push({ test: p => p.x > x + 0.1 && p.x < x + 0.9 && p.z < z + 0.8 && p.z > z - 0.5, to: "silph_co_elevator", spawn, fx: "fade" });
      const wt = R.warps[R.warps.length - 1], t0 = wt.test; wt.test = p => { const ok = t0(p); if (ok) window.SILPH_LIFT.from = id; return ok; };
      R.item("Elevator", "Steel doors, the arrows lit over them. Walk in, and pick a floor from the buttons inside.");
    }

    // FireRed's card-key shutters: steel slats, hazard stripes at the foot, a lock lit red. With the CARD KEY you're
    // let through (in the game, once you have it; here in the viewer, straight through).
    for (const d of F.doors) {
      const X0 = d.x0, X1 = d.x1 + 1, Z0 = d.z0, Z1 = d.z1 + 1, ns = d.way === "ns";
      if (ns) {
        G.box(X0, 0, Z0 + 0.3, X1, 0.25, Z1 - 0.3, { all: "silphHazard", top: null, bottom: null });
        G.box(X0, 0.25, Z0 + 0.3, X1, 1.85, Z1 - 0.3, { all: "silphShutter", top: "silphCap" });
        for (const px of [X0, X1 - 0.22]) G.box(px, 0, Z0 + 0.15, px + 0.22, 2.0, Z1 - 0.15, { all: "silphMachine", top: "silphCap" });
        G.box((X0 + X1) / 2 - 0.2, 1.4, Z1 - 0.3, (X0 + X1) / 2 + 0.2, 1.62, Z1 - 0.27, { s: "silphLock", all: null });
      } else {
        G.box(X0 + 0.3, 0, Z0, X1 - 0.3, 0.25, Z1, { all: "silphHazard", top: null, bottom: null });
        G.box(X0 + 0.3, 0.25, Z0, X1 - 0.3, 1.85, Z1, { all: "silphShutter", top: "silphCap" });
        for (const pz of [Z0, Z1 - 0.22]) G.box(X0 + 0.15, 0, pz, X1 - 0.15, 2.0, pz + 0.22, { all: "silphMachine", top: "silphCap" });
        G.box(X1 - 0.3, 1.4, (Z0 + Z1) / 2 - 0.2, X1 - 0.27, 1.62, (Z0 + Z1) / 2 + 0.2, { e: "silphLock", all: null });
      }
      const cx = (X0 + X1) / 2, cz = (Z0 + Z1) / 2, did = id + ":" + d.x0 + "," + d.z0;
      const sides = ns
        ? [["s", [X0, Z0 - 0.75, X1, Z0], { x: cx, z: Z1 + 0.5, yaw: FACE.s }], ["n", [X0, Z1, X1, Z1 + 0.75], { x: cx, z: Z0 - 0.5, yaw: FACE.n }]]
        : [["e", [X0 - 0.75, Z0, X0, Z1], { x: X1 + 0.5, z: cz, yaw: FACE.e }], ["w", [X1, Z0, X1 + 0.75, Z1], { x: X0 - 0.5, z: cz, yaw: FACE.w }]];
      for (const [dir, rect, spawn] of sides) {
        R._keys.push({ dir, x0: rect[0], z0: rect[1], x1: rect[2], z1: rect[3], spawn, needs: "card_key", id: did });
        const key = { n: "up", s: "down", e: "right", w: "left" }[dir];
        R.warps.push({ test: (p, input) => input[key] && p.x > rect[0] && p.x < rect[2] && p.z > rect[1] && p.z < rect[3], to: id, spawn, fx: "fade" });
      }
      R.item("Card-key shutters", "FireRed's locked doors: steel slats, hazard stripes, a red lock. With the CARD KEY (found on 5F) they let you through.");
    }

    // FireRed's signs: the floor plaques by the stairs, the POKéMON REPORTs, the elevator's buttons.
    for (const s of F.signs) {
      if (s.elevator) {
        G.box(s.x + 0.2, 0.95, s.z + 1, s.x + 0.8, 1.75, s.z + 1.04, { s: "silphButtons", all: "silphSkirt", n: null });
        R.act([s.x - 0.2, s.z + 0.6, s.x + 1.4, s.z + 2.0], { label: "Use", lines: ["The elevator's floor buttons. Which floor?"], use: "lift", run: async say => {
          const floors = ORDER.slice(0, 11).reverse();
          const pick = await say([{ text: "Which floor?", ask: floors.map(f => SILPH[f].name).concat(["CANCEL"]) }]);
          if (pick != null && pick < floors.length) { window.SILPH_LIFT.to = floors[pick]; await say([{ text: "The elevator hums... It's stopped at " + SILPH[floors[pick]].name + "." }]); }
        } });
        continue;
      }
      R.act([s.x - 0.2, s.z - 0.2, s.x + 1.2, s.z + 1.9], { label: "Read", lines: s.lines });
    }
    // FireRed's hidden items: look at the spot.
    for (const h of F.hidden || []) R.act([h.x - 0.15, h.z - 0.15, h.x + 1.15, h.z + 1.15], { label: "Look", lines: ["There's something here!"], use: "hidden:" + h.item + ":" + h.key });

    // The people, trainers and item balls, where FireRed has them.
    for (const o of F.objs) {
      const x = o.x + 0.5, z = o.z + 0.5;
      if (o.kind === "item") {
        R.balls.push({ key: o.key, x, z, y: 0 });
        roundBall(G, x, 0, z, 0.42);
        R.act([x - 0.6, z - 0.6, x + 0.6, z + 0.6], { label: "Look", lines: ["An item ball!"] });
        continue;
      }
      if (o.skin === "blue") continue; // (your rival comes on 7F in the game's own scene)
      const f = FACEOF[o.face] || FACEOF[o.dir] || "s";
      if (o.kind === "trainer") {
        R.npcs.push({ name: o.name, key: o.key, role: "trainer", x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => [{ text: o.skin === "leader_giovanni" ? "GIOVANNI: Ah, " + "{PLAYER}! So we meet again!" : "Halt! You're not with TEAM ROCKET!" }] });
      } else {
        R.npcs.push({ name: o.name, key: o.key, x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => o.lines.map(text => ({ text })) });
      }
    }

    if (id === "silph_co_1f") {
      doormat(R, 7, 10, { to: "town", mat: "matGate" });
      R.spawn = { x: 8.5, z: D - 1.6, yaw: FACE.n };
    } else if (lift) {
      // Out the elevator's doors: onto the floor you picked (or back where you got in).
      G.box(1, 0, 6.6, 4, 2.0, 6.7, { n: "silphLiftDoor", all: null });
      R._boxes.push({ x0: 1, z0: 6.2, x1: 4, z1: 7.5, lift: true });
      const lw = { test: (p, input) => input.down && p.z > 6.2 && p.x > 1 && p.x < 4, fx: "fade" };
      Object.defineProperty(lw, "to", { get: () => window.SILPH_LIFT.to || window.SILPH_LIFT.from });
      Object.defineProperty(lw, "spawn", { get: () => {
        const f = window.SILPH_LIFT.to || window.SILPH_LIFT.from, e = SILPH[f].warps.find(w => w.kind === "elevator");
        window.SILPH_LIFT.from = f; window.SILPH_LIFT.to = null;
        return { x: e.x + 0.5, z: e.z + 1.5, yaw: FACE.s };
      } });
      R.warps.push(lw);
      R.spawn = { x: 2.5, z: 5.2, yaw: FACE.n };
      R.item("Elevator", "SILPH CO.'s elevator: brushed steel. Press the buttons for a floor, then walk out.");
    } else {
      const st = F.warps.find(w => w.kind === "stairs");
      R.spawn = { x: st.x + 0.5, z: st.z + 1.5, yaw: FACE.s };
    }
    R.item("Silph Co.", "FireRed's floor, tile for tile, as the outside: white cladding panels, SILPH blue, pale blue-grey tiles.");
    return R;
  }
  window.SILPH_LIFT = { from: "silph_co_1f", to: null };
  for (const id of ORDER) BUILD[id] = () => build(id);
})();
