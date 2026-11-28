// ---------- 0.51.0 (09:05, 09:11): the gates' ways through as tunnels, the rooms' floors and mats in the outside's style ----------
// John: the floors and the mats "feel like two separate texturing packs"; they should be drawn as the outside is (the
// area looks: flat Let's Go colours, each paving stone lit along its top and left with a soft shadow along its bottom
// and right, the faintest grain). The black at a gate's far side should feel like a tunnel to go through, with a light
// at the end of it.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };

  // A paving stone a block, as outside (tools/lookarea.py's pave()): lit top and left, shadowed bottom and right.
  const pave = (P, r, x0, y0, n, base) => {
    for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
      let k = (r() - 0.5) * 0.035;
      if (x === 0 || y === 0) k += 0.09;
      if (x === n - 1 || y === n - 1) k -= 0.13;
      P.set(x0 + x, y0 + y, shadec(base, k));
    }
  };
  // The gates' floor: Let's Go's two yellows as paving stones, a block each, in a checker.
  TEXDEF.gateCheck = [32, 32, (P, r) => {
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) pave(P, r, tx * 16, ty * 16, 16, (tx + ty) % 2 ? hx("#E4C072") : hx("#F1DC9C"));
  }];
  // Their walls: flat cream panels (no grain), a fine join down each and a soft line under the picture rail.
  TEXDEF.paperGateLG = [16, 16, (P, r) => {
    P.fill(hx("#F7F1E2"), 0.008, r);
    P.rect(0, 0, 16, 1, hx("#EBE3D0")); P.rect(0, 1, 16, 1, hx("#FFFDF6"));
    P.rect(7, 2, 1, 14, hx("#EDE5D2")); P.rect(8, 2, 1, 14, hx("#FFFDF6"));
  }];
  // The blue lower walls a painted band, lit along its top, shadowed along its foot; the white trim the same.
  TEXDEF.gateBlue = [16, 16, (P, r) => {
    P.fill(hx("#5C8FB8"), 0.008, r);
    P.rect(0, 0, 16, 1, hx("#8DB4D6")); P.rect(0, 1, 16, 1, hx("#6E9DC3")); P.rect(0, 15, 16, 1, hx("#4A7798"));
  }];
  TEXDEF.gateWhite = [16, 16, (P, r) => {
    P.fill(hx("#F4F6F4"), 0.006, r);
    P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#DCE0DC"));
  }];

  // Every mat as the outside would draw it: flat, its rim lit along its top and left and shadowed along its bottom and
  // right, a stitched line in from the rim, the arrows crisp with a little shadow under them.
  const matClean = (base, edge, dir) => (P, r) => {
    const w = P.w, h = P.h, B = hx(base), E = hx(edge), cream = hx("#F6F1EA"), sh = shadec(B, -0.22);
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const rim = x < 2 || y < 2 || x >= w - 2 || y >= h - 2;
      let c = rim ? E : B;
      if (rim) { if (x === 0 || y === 0) c = shadec(E, 0.14); else if (x === w - 1 || y === h - 1) c = shadec(E, -0.14); }
      else {
        if (x === 2 || y === 2) c = shadec(B, 0.09);
        else if (x === w - 3 || y === h - 3) c = shadec(B, -0.11);
        c = shadec(c, (r() - 0.5) * 0.025);
      }
      P.set(x, y, c);
    }
    for (let x = 5; x < w - 5; x++) if ((x - 5) % 4 !== 3) { P.set(x, 5, cream); P.set(x, h - 6, cream); }
    for (let y = 5; y < h - 5; y++) if ((y - 5) % 4 !== 3) { P.set(5, y, cream); P.set(w - 6, y, cream); }
    const dot = (x, y) => { P.rect(x + 1, y + 1, 2, 2, sh); };
    const pen = (x, y) => { P.rect(x, y, 2, 2, cream); };
    if (dir === "s" || dir === "n") {
      const cx = w / 2 - 16;
      for (const pass of [dot, pen]) for (const y0 of [8, 16]) for (let k = 0; k < 7; k++) {
        const y = dir === "n" ? h - 2 - (y0 + k) : y0 + k;
        pass(cx + 8 + k, y); pass(cx + 22 - k, y);
      }
    } else {
      const cy = h / 2;
      for (const pass of [dot, pen]) for (const x0 of [8, 16]) for (let k = 0; k < 7; k++) {
        const x = dir === "e" ? x0 + k : w - (x0 + k) - 2;
        pass(x, cy - 8 + k); pass(x, cy + 6 - k);
      }
    }
  };
  const MATS = [["matHome", "#C9433F", "#8E2A2A"], ["matRival", "#33477A", "#1E2A4A"], ["matLab", "#2F9C8A", "#1D6B5E"], ["matCenter", "#F08A3C", "#C7502E"],
    ["matMart", "#3A6FD0", "#24479A"], ["matTrainer", "#E8823A", "#A9531F"], ["matForest", "#4E9A4A", "#2F6B33"], ["matSchool", "#9C6A3C", "#6B4524"],
    ["matLabPurple", "#8F60DE", "#5E3AA6"], ["matHouseV", "#3E8A6E", "#2A5E4B"], ["matGate", "#3A63B8", "#25418A"], ["matLeague", "#8C222A", "#D9AE4E"]];
  for (const [k, base, edge] of MATS) {
    for (const [key, w, h, dir] of [[k, 32, 32, "s"], [k + "W", 64, 32, "s"], [k + "N", 32, 32, "n"], [k + "NW", 64, 32, "n"]]) {
      if (!TEXDEF[key]) continue;
      TEXDEF[key] = [w, h, matClean(base, edge, dir)];
    }
  }
  TEXDEF.matGateEast = [32, 64, matClean("#3A63B8", "#25418A", "e")];
  TEXDEF.matGateWest = [32, 64, matClean("#3A63B8", "#25418A", "w")];
  // ((0.75) PROF. ELM's HOUSE's way out, through its east wall: a home's red mat, its arrows pointing east.)
  TEXDEF.matHomeEast = [32, 32, matClean("#C9433F", "#8E2A2A", "e")]; MATDEF.matHomeEast = { tex: "matHomeEast", fit: true };

  // A gate's way through (09:05): a tunnel. Its back is a picture of the passage going on, its walls, floor and roof
  // drawn in towards the light at its end (the day outside: the sky and the grass), darker in the middle, the light
  // spilling back along it; rings where its panels join. 4 blocks across, 2.4 high.
  reg("gateTunnel", 64, 40, (P, r) => {
    const W = P.w, H = P.h, vx = W / 2, vy = 22, e = 0.32;
    const wall = hx("#E7DDC6"), floor = hx("#D9B867"), roof = hx("#D6CFBF");
    const sky = hx("#F4FBFF"), sky2 = hx("#FFFFFF"), grass = hx("#C7E6A0"), grass2 = hx("#A9D47C");
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
      const dx = (x + 0.5 - vx) / vx, dy = y + 0.5 < vy ? (y + 0.5 - vy) / vy : (y + 0.5 - vy) / (H - vy);
      const s = Math.max(Math.abs(dx), Math.abs(dy));
      if (s < e) {
        // The day at the end: sky over the grass, brightest in the middle.
        const g = y + 0.5 > vy + 1.5, k = 1 - s / e;
        P.set(x, y, g ? mixc(grass2, grass, Math.min(1, k * 1.4)) : mixc(sky, sky2, Math.min(1, k * 1.2)));
        continue;
      }
      const t = (1 - s) / (1 - e), b = 0.64 - 0.52 * t + 0.72 * Math.pow(t, 4);
      const side = Math.abs(dx) >= Math.abs(dy), base = side ? wall : dy < 0 ? roof : floor;
      let c = shadec(base, -(1 - b) * 0.85);
      // Light from the end lying along the floor's middle.
      if (!side && dy > 0) c = mixc(c, sky2, Math.max(0, 0.35 * Math.pow(t, 3) * (1 - Math.abs(dx) / Math.max(0.01, s))));
      // The corners where the walls meet the floor and the roof, and the rings where the panels join.
      if (Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.025) c = shadec(c, -0.18);
      for (const ring of [0.8, 0.62, 0.48, 0.39]) if (Math.abs(s - ring) < 0.012 + 0.01 * ring) c = shadec(c, -0.16);
      // The glow round the end.
      if (s < e + 0.06) c = mixc(c, sky2, (1 - (s - e) / 0.06) * 0.55);
      P.set(x, y, c);
    }
  }, { fit: true, glow: 0.35 });
  // The tunnel's sides (lining the nook), its roof and the lintel over its mouth.
  reg("tunnelWall", 16, 16, (P, r) => {
    P.fill(hx("#CFC5AE"), 0.008, r);
    P.rect(0, 7, 16, 1, hx("#BDB29A")); P.rect(0, 8, 16, 1, hx("#DCD3BE")); P.rect(0, 15, 16, 1, hx("#B5AA92"));
  });
  reg("tunnelRoof", 16, 16, (P, r) => { P.fill(hx("#A79E8A"), 0.008, r); P.rect(0, 0, 16, 1, hx("#958C78")); });
  reg("tunnelLintel", 16, 16, (P, r) => {
    P.fill(hx("#F4F6F4"), 0.006, r); P.rect(0, 0, 16, 2, hx("#FFFFFF")); P.rect(0, 14, 16, 2, hx("#C9CEC9"));
  });

  // The gates' runner (09:05): flat, not Minecraft's wool: Let's Go's blue with a fine weave along it, a gold border.
  const runnerTex = (alongX) => (P, r) => {
    const w = P.w, h = P.h, B = hx("#4A7FD4"), G = hx("#E9D27A");
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const a = alongX ? y : x, edge = Math.min(x, y, w - 1 - x, h - 1 - y);
      let c;
      if (edge < 3) c = edge === 0 ? shadec(G, -0.18) : edge === 2 ? shadec(G, 0.1) : G;
      else if (edge === 3) c = shadec(B, -0.2);
      else c = shadec(B, (a % 4 < 2 ? 0.035 : -0.035) + (r() - 0.5) * 0.02);
      P.set(x, y, c);
    }
  };
  reg("gateRunN", 64, 160, runnerTex(false), { fit: true });
  reg("gateRunE", 192, 64, runnerTex(true), { fit: true });
  reg("gateRunEdge", 8, 8, (P, r) => P.fill(hx("#C9AE52"), 0.01, r));
})();

// A flat runner (09:05): one carpet over the floor from x0, z0 to x1, z1, its picture the whole runner.
function runner(room, x0, z0, x1, z1, mat) {
  room.geo.box(x0, 0, z0, x1, 1 / 16, z1, { top: mat, all: "gateRunEdge", bottom: null });
  room.item("Runner", "A flat runner over the floor, a fine weave along it and a gold border.");
}
