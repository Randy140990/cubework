// ---------- 0.48.0 (17:42 to 17:53): Seafoam Islands and Rock Tunnel (tools/caves48.py), and the round's new rooms ----------
(function () {
  const PAL = {
    seafoam: { floor: "area_sf_floor", sand: "area_sf_sand", step: "area_sf_step", wall: "area_sf_snow", wallTop: "area_sf_snow_top", ice: "area_sf_ice", iceTop: "area_sf_ice_top",
      boulder: "area_ice_shard", pre: "iboulder", haze: 0x9ec8ea, night: false },
    rock_tunnel: { floor: "area_rt_floor", sand: "area_rt_floor", step: "area_rt_floor", wall: "area_rt_rock", wallTop: "area_rt_rock_top", ice: "area_rt_rock", iceTop: "area_rt_rock_top",
      boulder: "area_rt_boulder", pre: "rboulder", haze: 0x463a30, night: true },
  };
  function caveRoom48(id) {
    const C = CAVES48[id], K = PAL[C.cave];
    const R = new Room(id, { name: C.name, floor: C.floor, W: C.W, D: C.D, H: 9, outside: true, night: K.night, haze: K.haze });
    const G = R.geo, P = 1 / 16;
    const at = (x, z) => { const r = C.rows[z]; return { k: r[2 * x], h: r.charCodeAt(2 * x + 1) - 56 }; };
    for (let z = 0; z < C.D; z++) {
      for (let x = 0; x < C.W;) {
        const c = at(x, z); let x1 = x + 1;
        while (x1 < C.W && at(x1, z).k === c.k && at(x1, z).h === c.h) x1++;
        const h = c.h / 2;
        if (c.k === "w" || c.k === "i") {
          const top = Math.min(h, 6);
          G.box(x, -1, z, x1, top, z + 1, { top: c.k === "i" ? K.iceTop : K.wallTop, all: c.k === "i" ? K.ice : K.wall, bottom: null });
          R.solid(x, z, x1, z + 1);
        } else if (c.k === "G" || c.k === "g") {
          G.box(x, -1, z, x1, h, z + 1, { top: K.floor, all: "soil" });
          for (let bx = x; bx < x1; bx++) {
            const m = BOULDERS[(c.k === "g" ? K.pre + "_small" : ((bx * 31 + z * 17 + 3 * 7) % 2 + 2) % 2 === 0 ? K.pre + "_a" : K.pre + "_b")] || BOULDERS[c.k === "g" ? "boulder_small" : "boulder_a"];
            for (const q of m.slice(1)) G.box(bx + q[0] * P, h + q[1] * P, z + q[2] * P, bx + q[3] * P, h + q[4] * P, z + q[5] * P, K.boulder);
          }
          R.solid(x, z, x1, z + 1);
        } else if (c.k === "K" || c.k === "L") {
          G.box(x, h - 0.2, z, x1, h, z + 1, { top: "darkTop", all: null });
          if (c.k === "L") G.box(x, h, z + 0.02, x1, 0.4, z + 0.08, "walnut");
        } else if (c.k === "q") G.box(x, -1.2, z, x1, -0.15, z + 1, { top: "water", all: "soil" });
        else {
          const mat = c.k === "s" ? K.sand : c.k === "^" ? K.step : c.k === "S" ? "area_cave_mat" : K.floor;
          G.box(x, -1, z, x1, h, z + 1, { top: mat, all: h > 0.6 ? K.ice : "soil" });
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
      const y = (it.y || 0);
      G.box(it.x - 0.2, y, it.z - 0.2, it.x + 0.2, y + 0.4, it.z + 0.2, { all: "ball", top: "ballTop" });
      R.act([it.x - 0.6, it.z - 0.6, it.x + 0.6, it.z + 0.6], { label: "Look", lines: [it.item.replace(/_/g, " ").toUpperCase()] });
    }
    // (The POKéMON standing here, ARTICUNO: the game has the creature; the viewer marks its spot.)
    for (const m of C.mons) {
      const y = m.y || 0;
      G.box(m.x - 0.5, y, m.z - 0.5, m.x + 0.5, y + 0.15, m.z + 0.5, { all: K.iceTop });
      G.box(m.x - 0.25, y + 0.15, m.z - 0.25, m.x + 0.25, y + 1.6, m.z + 0.25, { all: K.boulder });
      R.solid(m.x - 0.5, m.z - 0.5, m.x + 0.5, m.z + 0.5);
      R.act([m.x - 1, m.z - 1, m.x + 1, m.z + 1.2], { label: "Look", lines: [m.species.toUpperCase() + " is here.", "(In the game: talk to it and it attacks. Catch it if you can!)"] });
    }
    for (const a of C.acts) R.act(a.rect, { label: a.label, lines: a.lines });
    for (const p of C.people) R.npcs.push({ name: p.name.split(" ").map(w => w[0] + w.slice(1).toLowerCase()).join(" "), key: p.key, x: p.x, z: p.z, y: p.y || 0,
      yaw: (180 - p.yaw) * Math.PI / 180, look: { skin: p.skin }, role: p.role || null, talk: () => [{ text: "(A trainer: walk into their sight and they'll battle you.)" }] });
    R.spawn = { x: C.spawn.x, z: C.spawn.z, yaw: C.spawn.yaw === 180 ? FACE.n : FACE.s };
    R.item(C.name, C.cave === "seafoam" ? "John's floors stitched as FireRed's: ladders and holes down to B4F and ARTICUNO; its rock ice, its boulders ice shards; the deep water you can swim (SURF comes later)."
      : "John's two floors stitched as FireRed's: in by the north mouth at 1F's ladder, four ladders between 1F and B1F, out at 1F's south doorway. Dark; craggy rocks; FireRed's trainers.");
    return R;
  }
  for (const id of Object.keys(CAVES48)) {
    const C = CAVES48[id];
    PLACES.push({ id, name: C.name, floor: C.floor, group: C.name });
    BUILD[id] = () => caveRoom48(id);
  }
})();
