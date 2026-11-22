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
    Geo.prototype.face = function (k, BL, R, U, w, h, n, sub) { if (k && w > 0 && h > 0) (this._faces || (this._faces = [])).push({ k, bl: v(BL), r: v(R), u: v(U), w, h, n: n ? v(n) : null, sub: sub || null }); return oface.call(this, k, BL, R, U, w, h, n, sub); };
    const oscreen = Room.prototype.screen;
    Room.prototype.screen = function (key, w, h, draw, glow) { (this._screens || (this._screens = [])).push({ key, w, h, draw: draw.name }); return oscreen.call(this, key, w, h, draw, glow); };
    const oshell = window.shell; window.shell = function (room, o = {}) { room._floors = o.floorRects || [[0, 0, room.W, room.D]]; room._door = o.door || null; room._paper = o.paper; return oshell(room, o); };
    const omat = window.doormat; window.doormat = function (room, x0, x1, o = {}) { if (!o.none) (room._mats || (room._mats = [])).push(room.entry ? { x0: room.entry[0], x1: room.entry[1], to: o.to, spawn: o.spawn } : { x0, x1, to: o.to, spawn: o.spawn }); return omat(room, x0, x1, o); };
    const opass = window.passageN; window.passageN = function (room, x0, x1, o = {}) { if (!o.none) (room._passes || (room._passes = [])).push(room.northNook ? { x0: room.northNook[0], x1: room.northNook[1], to: o.to, spawn: o.spawn, needs: o.needs || null } : { x0, x1, to: o.to, spawn: o.spawn, needs: o.needs || null }); return opass(room, x0, x1, o); };
    const oside = window.passageSide; window.passageSide = function (room, wall, z0, z1, o = {}) { if (!o.none) (room._sides || (room._sides = [])).push({ wall, z0, z1, to: o.to, spawn: o.spawn, choice: o.choice || null }); return oside(room, wall, z0, z1, o); };
    const oup = window.stairsUp; window.stairsUp = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "up", x0, z0, depth, to: o.to, spawn: o.spawn }); return oup(room, x0, z0, depth, o); };
    const odown = window.stairsDown; window.stairsDown = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "down", x0, z0, depth, to: o.to, spawn: o.spawn }); return odown(room, x0, z0, depth, o); };
    const rooms = {}, used = {};
    for (const id of ["house1", "house2", "rival1", "rival2", "lab", "center", "mart", "school", "nickhouse", "gate_s", "gate_n", "gate_league", "gate_johto", "route2_gate", "route2_house", "pewter_gym", "museum_1f", "museum_2f", "pewter_house1", "pewter_house2", "fuchsia_gym", "safari_gate", "fuchsia_office", "fuchsia_house1", "fuchsia_house2", "fuchsia_house3", "fuchsia_house4", "safari_rest_center", "safari_rest_east", "safari_rest_north", "safari_rest_west", "safari_secret_house", "ug_r5", "ug_r6", "ug_r7", "ug_r8", "ug_ns", "ug_ew", "cerulean_gym", "bike_shop", "cerulean_house1", "cerulean_house2", "cerulean_house3", "cerulean_house4", "daycare", "bills_house", "power_plant", "arena_lab", "arena_pewter_gym"]) {
      const R = BUILD[id]();
      const faces = (R.geo._faces || []).slice();
      for (const pt of R.parts) for (const f of (pt.geo._faces || [])) faces.push(f);
      for (const f of faces) used[f.k] = true;
      const ramps = R.ramps.map(rp => { const [x0, z0, x1, z1] = rp.rect, xs = []; for (let x = x0; x <= x1 + 1e-6; x += 0.125) xs.push([x, rp.h(x, (z0 + z1) / 2)]); return { rect: rp.rect, h: xs }; });
      rooms[id] = {
        id, name: R.name, floor: R.floor, W: R.W, D: R.D, H: R.H, faces, solids: R.solids, ramps,
        acts: R.acts.map(a => ({ rect: a.rect, label: a.label || "Look", lines: a.lines || null, ask: !!a.ask, pc: !!a.pc, hit: !!a.hit, map: !!a.map })),
        npcs: R.npcs.map(n => ({ name: n.name, x: n.x, z: n.z, y: n.y || 0, yaw: n.yaw, seated: !!n.seated, skin: n.look && n.look.skin, role: n.role || null, key: n.key || null, group: n.group || null, hours: n.hours || null, lines: n.talk ? n.talk(null).map(l => l.text) : [] })),
        spawn: R.spawn, floors: R._floors, door: R._door, pools: R.pools || [], Dm: R.Dm || R.D, mats: R._mats || [], passes: R._passes || [], sides: R._sides || [], grass: R.grass || [], stairs: R._stairs || [], screens: R._screens || [], clocks: R.clocks || [],
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
    // The windows' pictures at dusk and at night (the light over the floor is worked out in the mod).
    const light = {};
    for (const k of ["windowDusk", "windowNight", "doorDusk", "doorNight"]) light[k] = pixTex(k).image.toDataURL();
    return { rooms, mats, light };
  });
  fs.writeFileSync(out, JSON.stringify(data));
  console.log(Object.entries(data.rooms).map(([k, r]) => `${k}: ${r.faces.length} faces, ${r.acts.length} acts, ${r.npcs.length} people`).join('\n'), '\nmaterials', Object.keys(data.mats).length);
  await b.close();
})();
