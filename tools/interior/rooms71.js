// ---------- (0.69, John's 26 Nov 15:58) The RUINS OF ALPH, between its two gates ----------
// "we've got the ruins of Alf to incorporate within the 2 gates ... you would go into them. Those gates obviously need
// to be open. I think people can go in and just walk around and explore it, but I would say none of those caves or
// anything will have any interaction until I build that."
// John's ruins (tools/alph.py: his schematic, a place of its own in the game's interiors), block for block as the game
// puts them there (alph_data.js, tools/alphview.py), each face in the picture the game gives it (our looks over it, as
// all Johto has them: tools/arealooks69.py), his signs as the game's sign. In from ROUTE 36's gate at the north
// gatehouse, from ROUTE 32's at the east one; out up either one's steps. To look round, as the ships: drag to turn,
// right-drag, two fingers or Shift and drag to move across, the wheel or a pinch to come in and out.
(function () {
  if (typeof ALPH === "undefined" || typeof blockShape === "undefined") return;
  const D = ALPH;
  PLACES.push({ id: "ruins_of_alph", name: "Ruins of Alph", floor: "Between Route 36's gate and Route 32's, as the game has it", group: "Johto's gates" });
  for (const [k, p] of Object.entries(D.pics)) {
    MATDEF["alph_" + k] = { img: "data:image/png;base64," + p.png, per: p.per, ...(p.cut ? { cut: true } : p.alpha ? { alpha: p.alpha } : {}) };
    TEXDEF["alph_" + k] = [16, 16, (P, r) => P.fill([200, 200, 200], 0, r)];
  }
  const M = k => k ? "alph_" + k : null;
  // A kind's own block, its look's number taken off (stone_bricks~3 is stone bricks in a look of ours).
  const plain = s => s.replace(/~\d+/, "");

  BUILD.ruins_of_alph = () => {
    const R = new Room("ruins_of_alph", { name: "Ruins of Alph", floor: "Between Route 36's gate and Route 32's, as the game has it", W: D.W, D: D.D, H: D.H, outside: true });
    R.orbit = { x: D.W / 2, y: 2, z: D.D / 2 + 6, yaw: 0.35, pitch: 0.62, dist: 165 };
    R.spawn = { x: D.fromNorth.x, z: D.fromNorth.z, yaw: FACE.s };
    R.outsideSize = `${D.W} by ${D.D} (${D.count} blocks with a face showing)`;
    const G = R.geo, bytes = atob(D.cells), F = D.floor;
    const looks = D.palette.map(s => { const f = D.faces[s.split("[")[0]]; return f ? { top: M(f.top), bottom: M(f.bottom), side: M(f.side) } : { top: "stone", bottom: "stone", side: "stone" }; });
    const shapes = D.palette.map(s => D.models[s] ? null : blockShape(plain(s)));
    for (let i = 0; i + 5 < bytes.length; i += 6) {
      const x = bytes.charCodeAt(i), y = bytes.charCodeAt(i + 1) - F, z = bytes.charCodeAt(i + 2), p = bytes.charCodeAt(i + 3) | (bytes.charCodeAt(i + 4) << 8), m = bytes.charCodeAt(i + 5);
      const mdl = D.models[D.palette[p]];
      if (mdl) {
        // A sign: the game's own model, its boxes and their pictures.
        for (const b of mdl) {
          const f = b[6];
          G.box(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], { top: M(f.top), bottom: M(f.bottom), n: M(f.n), s: M(f.s), e: M(f.e), w: M(f.w) });
        }
        continue;
      }
      const L = looks[p], s = shapes[p];
      if (!s) {
        G.box(x, y, z, x + 1, y + 1, z + 1, { top: m & 1 ? L.top : null, bottom: m & 2 ? L.bottom : null, s: m & 4 ? L.side : null, n: m & 8 ? L.side : null, e: m & 16 ? L.side : null, w: m & 32 ? L.side : null });
        continue;
      }
      for (const b of s) G.box(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], { top: L.top, bottom: L.bottom, side: L.side });
    }
    R.item("Ruins of Alph", "John's ruins as the game puts them in its interiors, every block of his schematic: the chambers in his cave style, the research centre, the pond and the channel, his signs, his two gatehouses and his trees seven deep all round, each face in the picture the game gives it (Johto's looks over his blocks, as all his Johto has them).");
    R.item("The ways in and out", "In the game: from ROUTE 36's Ruins of Alph gate you come out in front of the north gatehouse; from ROUTE 32's gate, in front of the east one. Up either gatehouse's steps into its doorway (daylight at its end) and you're back in that gate. The chambers' doorways are John's black, with nothing in them yet.");
    R.item("Looking round", "Drag to turn round it; right-drag, two fingers or Shift and drag to move across; the wheel or a pinch to come in and out; W A S D move across too.");
    return R;
  };
})();
