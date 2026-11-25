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
  // (0.63) In batches: the page builds a few rooms at a time and hands them over, so it never holds them all.
  await p.evaluate(() => {
    // (0.62, John's 20:37: "Please implement") The game gets the fit-out, as the viewer shows it.
    if (typeof fitOn === 'function') fitOn(true);
    const v = o => [o.x, o.y, o.z];
    const oface = Geo.prototype.face;
    Geo.prototype.face = function (k, BL, R, U, w, h, n, sub) { if (k && w > 0 && h > 0) (this._faces || (this._faces = [])).push({ k, bl: v(BL), r: v(R), u: v(U), w, h, n: n ? v(n) : null, sub: sub || null }); return oface.call(this, k, BL, R, U, w, h, n, sub); };
    const oscreen = Room.prototype.screen;
    Room.prototype.screen = function (key, w, h, draw, glow) { (this._screens || (this._screens = [])).push({ key, w, h, draw: draw.name }); return oscreen.call(this, key, w, h, draw, glow); };
    const oshell = window.shell; window.shell = function (room, o = {}) { room._floors = o.floorRects || [[0, 0, room.W, room.D]]; room._door = o.door || null; room._paper = o.paper; return oshell(room, o); };
    const omat = window.doormat; window.doormat = function (room, x0, x1, o = {}) { if (!o.none) (room._mats || (room._mats = [])).push(room.entry ? { x0: room.entry[0], x1: room.entry[1], to: o.to, spawn: o.spawn, needs: o.needs || null } : { x0, x1, to: o.to, spawn: o.spawn, needs: o.needs || null }); return omat(room, x0, x1, o); };
    const opass = window.passageN; window.passageN = function (room, x0, x1, o = {}) { if (!o.none) (room._passes || (room._passes = [])).push(room.northNook ? { x0: room.northNook[0], x1: room.northNook[1], to: o.to, spawn: o.spawn, needs: o.needs || null } : { x0, x1, to: o.to, spawn: o.spawn, needs: o.needs || null }); return opass(room, x0, x1, o); };
    const oside = window.passageSide; window.passageSide = function (room, wall, z0, z1, o = {}) { if (!o.none) (room._sides || (room._sides = [])).push({ wall, z0, z1, to: o.to, spawn: o.spawn, choice: o.choice || null, needs: o.needs || null }); return oside(room, wall, z0, z1, o); };
    const oup = window.stairsUp; window.stairsUp = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "up", x0, z0, depth, to: o.to, spawn: o.spawn }); return oup(room, x0, z0, depth, o); };
    const odown = window.stairsDown; window.stairsDown = function (room, x0, z0 = 0, depth = 2, o = {}) { (room._stairs || (room._stairs = [])).push({ dir: "down", x0, z0, depth, to: o.to, spawn: o.spawn }); return odown(room, x0, z0, depth, o); };
    window.__used = {};
    window.__ids = () => ["house1", "house2", "rival1", "rival2", "lab", "center", "mart", "school", "nickhouse", "gate_s", "gate_n", "gate_league", "gate_johto", "route2_gate", "route2_house", "pewter_gym", "museum_1f", "museum_2f", "pewter_house1", "pewter_house2", "fuchsia_gym", "safari_gate", "fuchsia_office", "fuchsia_house1", "fuchsia_house2", "fuchsia_house3", "fuchsia_house4", "safari_rest_center", "safari_rest_east", "safari_rest_north", "safari_rest_west", "safari_secret_house", "ug_r5", "ug_r6", "ug_r7", "ug_r8", "ug_ns", "ug_ew", "cerulean_gym", "bike_shop", "cerulean_house1", "cerulean_house2", "cerulean_house3", "cerulean_house4", "daycare", "bills_house", "power_plant", "vermilion_gym", "pokemon_fan_club", "vermilion_old_rod_house", "vermilion_trade_house", "vermilion_pidgey_house", "lavender_volunteer_house", "lavender_cubone_house", "lavender_name_rater", "gate_r5", "gate_r6", "gate_r7", "gate_r8", "gate_r11", "gate_r12", "gate_r15", "gate_r16n", "gate_r16s", "gate_r18", "route12_house", "route16_house", "gate_r11_2f", "gate_r12_2f", "gate_r15_2f", "gate_r16_2f", "gate_r18_2f", "fighting_dojo", "saffron_gym", "copycats_house_1f", "copycats_house_2f", "mr_psychics_house", "silph_co_1f", "silph_co_2f", "silph_co_3f", "silph_co_4f", "silph_co_5f", "silph_co_6f", "silph_co_7f", "silph_co_8f", "silph_co_9f", "silph_co_10f", "silph_co_11f", "silph_co_elevator", "game_corner", "game_corner_prize_room", "celadon_mart_1f", "celadon_mart_2f", "celadon_mart_3f", "celadon_mart_4f", "celadon_mart_5f", "celadon_mart_roof", "celadon_mart_elevator", "cinnabar_gym", "cinnabar_lab", "cinnabar_lab_trade_room", "cinnabar_lab_metronome_room", "cinnabar_lab_fossil_room", "pokemon_mansion_1f", "pokemon_mansion_2f", "pokemon_mansion_3f", "pokemon_mansion_b1f", "sprout_tower_1f", "sprout_tower_2f", "sprout_tower_3f", "earls_pokemon_academy", "route_35_national_park_gate", "route_36_national_park_gate", "jgate_r29", "jgate_r43n", "jgate_r43s", "jgate_r31", "jgate_r36", "jgate_r32", "jgate_alph", "jgate_r42", "jgate_ecruteak", "jgate_r35", "jgate_r38", ...Object.keys(K60), "arena_lab", "arena_pewter_gym", ...(typeof ARENA_HALLS !== "undefined" ? ARENA_HALLS : [])];
    window.__room = id => {
      const rooms = {}, used = window.__used;

      const R = BUILD[id]();
      const faces = (R.geo._faces || []).slice();
      for (const pt of R.parts) for (const f of (pt.geo._faces || [])) faces.push(f);
      for (const f of faces) used[f.k] = true;
      const ramps = R.ramps.map(rp => { const [x0, z0, x1, z1] = rp.rect, xs = []; for (let x = x0; x <= x1 + 1e-6; x += 0.125) xs.push([x, rp.h(x, (z0 + z1) / 2)]); return { rect: rp.rect, h: xs }; });
      rooms[id] = {
        id, name: R.name, floor: R.floor, W: R.W, D: R.D, H: R.H, faces, solids: R.solids, ramps,
        acts: R.acts.map(a => ({ rect: a.rect, label: a.label || "Look", lines: a.lines || null, ask: !!a.ask, pc: !!a.pc, hit: !!a.hit, map: !!a.map, use: a.use || null })),
        npcs: R.npcs.map(n => ({ name: n.name, x: n.x, z: n.z, y: n.y || 0, yaw: n.yaw, seated: !!n.seated, skin: n.look && n.look.skin, role: n.role || null, key: n.key || null, group: n.group || null, hours: n.hours || null, lines: n.talk ? n.talk(null).map(l => l.text) : [] })),
        spawn: R.spawn, floors: R._floors, door: R._door, pools: R.pools || [], Dm: R.Dm || R.D, mats: R._mats || [], passes: R._passes || [], sides: R._sides || [], grass: R.grass || [], pads: R.pads || [], stairs: R._stairs || [], boxes: R._boxes || [], keys: R._keys || [], balls: R.balls || [], screens: R._screens || [], clocks: R.clocks || [], spin: R._spin || [], mons: R._mons || [],
      };
          return rooms[id];
    };
  });
  const ids = await p.evaluate(() => __ids());
  const rooms = {};
  for (let i = 0; i < ids.length; i += 12) {
    const got = await p.evaluate(batch => batch.map(id => __room(id)), ids.slice(i, i + 12));
    for (const r of got) rooms[r.id] = r;
  }
  const data = await p.evaluate(rooms => {
    const used = window.__used;
    // Textures: each material's picture (screens as a strip of frames).
    const mats = {};
    for (const k of Object.keys(used)) {
      const d = MATDEF[k]; if (!d) continue;
      // (Laid over the faces in the fit-out's own sizes: fitPer. Its water's surface opaque in the game, over the dark.)
      const per = typeof fitPer === 'function' && FIT ? fitPer(k, d) : d.per;
      const m = { fit: !!d.fit, per: per || 1, alpha: k === 'fitWater' ? 0 : d.alpha || 0, cut: !!d.cut, overlay: !!d.overlay, glow: d.glow || 0 };
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
    // (0.63) Which rooms each battle hall serves (arena key: the rooms' ids it's for, by their start).
    const arenaFor = typeof ARENA_SERVES !== 'undefined' ? ARENA_SERVES : {};
    return { mats, light, arenaFor };
  }, Object.fromEntries(Object.entries(rooms).map(([k, r]) => [k, { screens: r.screens }])));
  data.rooms = rooms;
  fs.writeFileSync(out, JSON.stringify(data));
  console.log(Object.entries(data.rooms).map(([k, r]) => `${k}: ${r.faces.length} faces, ${r.acts.length} acts, ${r.npcs.length} people`).join('\n'), '\nmaterials', Object.keys(data.mats).length);
  await b.close();
})();
