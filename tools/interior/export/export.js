// Dumps the Interior Viewer's rooms (every face, its material and texture, the solids, acts, people, stairs and
// mats) to JSON, for tools/interior/export/tomod.py to turn into the mod's blocks.
//   node tools/interior/export/export.js <interior-viewer.html url> <three.min.js> <out.json>
const { chromium } = require('playwright'); const fs = require('fs');
(async () => {
  const [url, three, out] = process.argv.slice(2);
  const b = await chromium.launch(); const p = await b.newPage();
  p.on('pageerror', e => console.log('ERR', e.message));
  await p.route('**/three@0.128.0/build/three.min.js', r => r.fulfill({ body: fs.readFileSync(three), contentType: 'application/javascript' }));
  await p.route('https://fonts.googleapis.com/**', r => r.fulfill({ body: '', contentType: 'text/css' }));
  await p.goto(url); await p.waitForTimeout(2500);
  const data = await p.evaluate(() => {
    const v = o => [o.x, o.y, o.z];
    const oface = Geo.prototype.face;
    Geo.prototype.face = function (k, BL, R, U, w, h, n) { if (k && w > 0 && h > 0) (this._faces || (this._faces = [])).push({ k, bl: v(BL), r: v(R), u: v(U), w, h }); return oface.call(this, k, BL, R, U, w, h, n); };
    const oscreen = Room.prototype.screen;
    Room.prototype.screen = function (key, w, h, draw, glow) { (this._screens || (this._screens = [])).push({ key, w, h, draw: draw.name }); return oscreen.call(this, key, w, h, draw, glow); };
    const oshell = window.shell; window.shell = function (room, o = {}) { room._floors = o.floorRects || [[0, 0, room.W, room.D]]; room._door = o.door || null; room._paper = o.paper; return oshell(room, o); };
    const omat = window.doormat; window.doormat = function (room, x0, x1, o = {}) { (room._mats || (room._mats = [])).push({ x0, x1, to: o.to, spawn: o.spawn }); return omat(room, x0, x1, o); };
    const opass = window.passageN; window.passageN = function (room, x0, x1, o = {}) { (room._passes || (room._passes = [])).push({ x0, x1, to: o.to, spawn: o.spawn }); return opass(room, x0, x1, o); };
    const oup = window.stairsUp; window.stairsUp = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "up", x0, z0, depth, to: o.to, spawn: o.spawn }); return oup(room, x0, z0, depth, o); };
    const odown = window.stairsDown; window.stairsDown = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "down", x0, z0, depth, to: o.to, spawn: o.spawn }); return odown(room, x0, z0, depth, o); };
    const rooms = {}, used = {};
    for (const id of ["house1", "house2", "rival1", "rival2", "lab", "center", "mart", "trainer1", "trainer2", "gate_s", "forest", "gate_n"]) {
      const R = BUILD[id]();
      const faces = (R.geo._faces || []).slice();
      for (const pt of R.parts) for (const f of (pt.geo._faces || [])) faces.push(f);
      for (const f of faces) used[f.k] = true;
      const ramps = R.ramps.map(rp => { const [x0, z0, x1, z1] = rp.rect, xs = []; for (let x = x0; x <= x1 + 1e-6; x += 0.125) xs.push([x, rp.h(x, (z0 + z1) / 2)]); return { rect: rp.rect, h: xs }; });
      rooms[id] = {
        id, name: R.name, floor: R.floor, W: R.W, D: R.D, H: R.H, faces, solids: R.solids, ramps,
        acts: R.acts.map(a => ({ rect: a.rect, label: a.label || "Look", lines: a.lines || null, ask: !!a.ask, pc: !!a.pc, hit: !!a.hit, map: !!a.map })),
        npcs: R.npcs.map(n => ({ name: n.name, x: n.x, z: n.z, yaw: n.yaw, seated: !!n.seated, skin: n.look && n.look.skin, role: n.role || null, key: n.key || null, lines: n.talk ? n.talk(null).map(l => l.text) : [] })),
        spawn: R.spawn, floors: R._floors, door: R._door, mats: R._mats || [], passes: R._passes || [], grass: R.grass || [], stairs: R._stairs || [], screens: R._screens || [],
      };
    }
    // Textures: each material's picture (screens as a strip of frames).
    const mats = {};
    for (const k of Object.keys(used)) {
      const d = MATDEF[k]; if (!d) continue;
      const m = { fit: !!d.fit, per: d.per || 1, alpha: d.alpha || 0, cut: !!d.cut, overlay: !!d.overlay, glow: d.glow || 0 };
      if (d.dyn) {
        let sc = null; for (const r of Object.values(rooms)) for (const s of r.screens) if (s.key === k) sc = s;
        const fn = window[sc.draw], frames = [];
        for (let f = 0; f < 16; f++) { const P = new Pix(sc.w, sc.h); fn(P, f); frames.push(P.canvas().toDataURL()); }
        m.frames = frames; m.fit = true;
      } else m.png = pixTex(d.tex).image.toDataURL();
      mats[k] = m;
    }
    return { rooms, mats };
  });
  fs.writeFileSync(out, JSON.stringify(data));
  console.log(Object.entries(data.rooms).map(([k, r]) => `${k}: ${r.faces.length} faces, ${r.acts.length} acts, ${r.npcs.length} people`).join('\n'), '\nmaterials', Object.keys(data.mats).length);
  await b.close();
})();
