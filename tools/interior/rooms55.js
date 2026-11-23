// ---------- 0.52.0 (10:41): the POKéMON LEAGUE gate's floor and carpet in the outside's style ----------
// John: the gate's pillars, plants and walls are fine; its floor (the red lattice) and its carpet should look as the
// outside does: flat Let's Go colours, each paving stone lit along its top and left, a soft shadow along its foot.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  // The floor: terracotta pavers a block each, two reds in a checker, a fine gold joint between.
  reg("leagueFloor", 32, 32, (P, r) => {
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const base = (tx + ty) % 2 ? hx("#B5584A") : hx("#C2685A");
      for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        let k = (r() - 0.5) * 0.03;
        if (x === 0 || y === 0) k += 0.09;
        if (x === 15 || y === 15) k -= 0.13;
        let c = shadec(base, k);
        if (x === 15 || y === 15) c = mixc(c, hx("#D9AE58"), 0.35);
        P.set(tx * 16 + x, ty * 16 + y, c);
      }
    }
  }, { per: 2 });
  // The runner: crimson with a fine weave along it, a gold border, flat.
  reg("leagueRun", 64, 160, (P, r) => {
    const w = P.w, h = P.h, B = hx("#A8242E"), G = hx("#E2B652");
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const edge = Math.min(x, y, w - 1 - x, h - 1 - y);
      let c;
      if (edge < 4) c = edge === 0 ? shadec(G, -0.2) : edge === 3 ? shadec(G, 0.12) : G;
      else if (edge === 4) c = shadec(B, -0.22);
      else c = shadec(B, (x % 4 < 2 ? 0.04 : -0.04) + (r() - 0.5) * 0.02);
      P.set(x, y, c);
    }
  }, { fit: true });
})();
