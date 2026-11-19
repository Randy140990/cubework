// ---------- the world behind the screens: one renderer, a scene for each kind of backdrop ----------
const renderer = new THREE.WebGLRenderer({ canvas: $("#c"), antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1)); renderer.setClearColor(0x000000, 0);
renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
const scene3 = new THREE.Scene(), cam3 = new THREE.PerspectiveCamera(34, 16 / 9, 0.1, 400);
const hemi3 = new THREE.HemisphereLight(0xfff6e8, 0x8a7f74, 0.66); scene3.add(hemi3); scene3.add(new THREE.AmbientLight(0xffffff, 0.12));
const sun3 = new THREE.DirectionalLight(0xffffff, 0.62); sun3.castShadow = true; sun3.shadow.mapSize.set(2048, 2048); sun3.shadow.bias = -0.0006; sun3.shadow.normalBias = 0.02; sun3.shadow.radius = 3;
scene3.add(sun3); scene3.add(sun3.target);
function lightFor(cx, cz, S, hi = 18) { sun3.position.set(cx - S * 0.3, hi, cz + S * 0.8); sun3.target.position.set(cx, 0, cz); Object.assign(sun3.shadow.camera, { left: -S, right: S, top: S, bottom: -S, near: 1, far: 90 }); sun3.shadow.camera.updateProjectionMatrix(); }
function look3(px, py, pz, tx, ty, tz) { cam3.position.set(px, py, pz); cam3.lookAt(tx, ty, tz); }
const SC = {}; let scNow = null;
function setScene(name, opts) {
  for (const k in SC) SC[k].group.visible = false;
  scNow = null;
  if (!name || name === "none") return;
  if (!SC[name]) { SC[name] = SCENES[name](); scene3.add(SC[name].group); }
  scNow = SC[name]; scNow.group.visible = true;
  if (scNow.enter) scNow.enter(opts || {});
}
// Swap every creature's look: drop the built scenes (they're made again with the other models) and show the screen again.
function setLook(v) {
  if (v === LOOK) return;
  LOOK = v; try { localStorage.setItem("rm-ui-look", v); } catch (e) {}
  for (const k in SC) { scene3.remove(SC[k].group); delete SC[k]; }
  scNow = null; renderLook();
  if (curId) go(curId, curArg);
}
function renderLook() { document.querySelectorAll("#lookSeg [data-look]").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.look === LOOK))); }
function setPixel(n) { const dpr = Math.min(2, window.devicePixelRatio || 1); renderer.setPixelRatio(n ? 1 / n : dpr); $("#c").style.imageRendering = n ? "pixelated" : "auto"; resize3(); }
function resize3() { const w = stageEl.clientWidth, hh = stageEl.clientHeight; if (!w || !hh) return; renderer.setSize(w, hh, false); cam3.aspect = w / hh; cam3.updateProjectionMatrix(); }
new ResizeObserver(resize3).observe(stageEl);

// ---------- creatures in scenes ----------
async function mon(sid) {
  await loadSpecies(sid);
  const m = buildModel(lookEntry(sid)); m.size = sizeOf(m);
  m.holder.traverse(o => { if (o.isMesh) o.castShadow = true; });
  return { m, sid, base: "idle", t: Math.random() * 3, once: null, ot: 0, x: 0, z: 0, y: 0, yaw: 0 };
}
function placeMon(st, x, z, yaw, y = 0) {
  st.x = x; st.z = z; st.yaw = yaw; st.y = y;
  const c = Math.cos(yaw), s = Math.sin(yaw), ox = -st.m.size.cx, oz = -st.m.size.cz;
  st.m.holder.position.set(x + (ox * c + oz * s), y, z + (-ox * s + oz * c)); st.m.holder.rotation.y = yaw;
}
function playOnce(st, key) { const an = animFor(st.m, key); if (an) { st.once = an; st.ot = 0; } return an ? (an.len || 1) : 0; }
function tickMon(st, dt) {
  if (!st) return;
  st.t += dt;
  const mv = st.base === "walk" ? moveAnim(st.m) : st.base === "fly" ? flyAnim(st.m) : st.base === "hover" ? flyAnim(st.m, ["hover", "fly", "walk"]) : null;
  const base = mv ? mv.an : st.base === "ground" ? groundIdle(st.m) : (animFor(st.m, "idle") || groundIdle(st.m));
  let list = [[base, st.t]];
  if (st.once) { st.ot += dt; if (st.ot > (st.once.len || 1) + 0.05) { st.once = null; } else list = [[st.once, st.ot]]; }
  setParts(st.m, mv ? mv.key : null); pose(st.m, list);
}
function flyAnim(model, order = ["fly", "hover", "walk"]) { const p = model.info.poses || {}; for (const k of order) if (p[k] && model.anims[p[k].anim]) return { an: model.anims[p[k].anim], key: k }; return null; }
function disc(r, color, y = 0.01, op = 1) { const m = new THREE.Mesh(new THREE.CircleGeometry(r, 48), new THREE.MeshLambertMaterial({ color, transparent: op < 1, opacity: op })); m.rotation.x = -Math.PI / 2; m.position.y = y; m.receiveShadow = true; return m; }
function blob(r) { const m = new THREE.Mesh(new THREE.PlaneGeometry(r, r), matFor("shadowBlob")); m.rotation.x = -Math.PI / 2; m.position.y = 0.02; m.renderOrder = 3; return m; }
function figureOf(look) { const f = makeFigure(look); poseFigure(f, {}); return f; }

const PROF_LOOK = { skin: "oak" }, RIVAL_LOOK = { skin: "blue" }, NURSE_LOOK = { skin: "nurse" }, CLERK_LOOK = { skin: "clerk" }, BUGBOY_LOOK = { skin: "youngster" }, MUM_LOOK = { skin: "mum" };

const SCENES = {
  // Pallet Town, from the Interior Viewer: you outside your door, your partner beside you.
  town() {
    const R = BUILD.town(); R.finish();
    const group = new THREE.Group(); group.add(R.group);
    let fig = null, lookKey = "", pal = null;
    const S = { group, room: R, npcs: [] };
    mon("pikachu").then(st => { pal = st; group.add(st.m.holder); const b = blob(1); b.position.set(8.2, 0.03, 11.4); group.add(b); st.base = "ground"; placeMon(st, 8.1, 11.4, Math.PI * 0.85); S.pal = st; });
    const mum = figureOf(MUM_LOOK); mum.root.position.set(5.2, 0, 11.6); mum.root.rotation.y = -1.4; group.add(mum.root); S.mum = mum;
    S.enter = o => {
      const k = JSON.stringify(G.look);
      if (k !== lookKey) { if (fig) group.remove(fig.root); fig = figureOf(G.look); fig.root.position.set(6.9, 0, 11.3); fig.root.rotation.y = Math.PI; group.add(fig.root); lookKey = k; S.fig = fig; }
      mum.root.visible = !!o.mum; if (fig) fig.root.rotation.y = o.mum ? 1.74 : Math.PI;
      lightFor(17, 12, 22, 26); hemi3.color.set(0xe8f4ff); hemi3.groundColor.set(0x6f8a5a);
      const d = o.far ? 22 : 13, p = 52 * D2R, tx = o.far ? 17 : 7.6, tz = o.far ? 12 : 10.6;
      look3(tx, Math.sin(p) * d, tz + Math.cos(p) * d, tx, 0.6, tz);
      S.t = 0;
    };
    S.tick = (dt, t) => {
      if (pal) tickMon(pal, dt);
      if (fig) poseFigure(fig, { wave: S.wave || 0, cheer: S.cheer || 0 });
      if (mum.root.visible) poseFigure(mum, { talk: S.mumTalk, wave: S.mumWave || 0 });
      for (const s of R.screens) s.tick(performance.now());
    };
    return S;
  },
  // The professor, with creatures running round him, on a glowing floor.
  prof() {
    const group = new THREE.Group(), S = { group, runners: [] };
    group.add(disc(4.2, 0xDFF6F4, 0.0, 0.55)); group.add(disc(2.0, 0xFFFFFF, 0.005, 0.45));
    const prof = figureOf(PROF_LOOK); prof.root.position.set(0, 0, 0); prof.root.rotation.y = Math.PI; group.add(prof.root); S.prof = prof;
    const rival = figureOf(RIVAL_LOOK); rival.root.position.set(1.45, 0, 0.45); rival.root.rotation.y = Math.PI - 0.25; group.add(rival.root); S.rival = rival;
    let you = null, lookKey = "";
    // Our creatures beside him: standing about, now and then one calling out.
    const SPOTS = [["pikachu", -0.85, 0.95, 0.35], ["cubone", -1.8, 0.35, 0.55], ["oddish", 0.55, 1.35, -0.1], ["clefairy", -1.3, 1.55, 0.2], ["pidgey", -1.45, -0.55, 0.4, 1.55]];
    SPOTS.forEach(([sid, x, z, turn, fly]) => mon(sid).then(st => { st.base = "ground"; st.home = [x, z, turn, fly || 0]; group.add(st.m.holder); S.runners.push(st); placeMon(st, x, z, Math.PI + turn, fly || 0); }));
    S.nextCry = 2;
    S.hop = () => { if (S.hopT == null) { S.hopT = 0; const r = S.runners.find(q => q.sid === "pikachu"); if (r) playOnce(r, "cry"); } };
    S.enter = o => {
      S.mode = o.mode || "intro"; S.hopT = null;
      rival.root.visible = S.mode === "rival";
      const k = JSON.stringify(G.look);
      if (S.mode === "you") { if (k !== lookKey || !you) { if (you) group.remove(you.root); you = figureOf(G.look); you.root.position.set(1.45, 0, 0.45); you.root.rotation.y = Math.PI - 0.25; group.add(you.root); lookKey = k; } you.root.visible = true; }
      else if (you) you.root.visible = false;
      lightFor(0, 0, 8, 14); hemi3.color.set(0xf2fbff); hemi3.groundColor.set(0x9fb8c0);
      const x = S.mode === "intro" ? -0.3 : 0.2;
      look3(x, 2.35, 6.6, x, 0.62, 0.3);
    };
    S.tick = (dt, t) => {
      poseFigure(prof, { talk: S.talking, point: S.point || 0, wave: S.wave || 0 });
      if (rival.root.visible) poseFigure(rival, { headYaw: Math.sin(t * 0.7) * 0.2 });
      if (you && you.root.visible) poseFigure(you, { wave: S.youWave || 0 });
      S.nextCry -= dt;
      if (S.nextCry < 0 && S.runners.length) { const r = S.runners[Math.floor(Math.random() * S.runners.length)]; playOnce(r, "cry"); S.nextCry = 2.5 + Math.random() * 2.5; }
      for (const r of S.runners) {
        const [x, z, turn, fly] = r.home;
        if (r.sid === "cubone") {
          // Wandering round in a slow loop, facing the way it goes.
          const a = t * 0.45, wx = -1.25 + Math.cos(a) * 0.95, wz = 0.85 + Math.sin(a) * 0.45;
          placeMon(r, wx, wz, Math.atan2(Math.sin(a) * 0.95, -Math.cos(a) * 0.45) + Math.PI); r.base = "walk";
        } else if (r.sid === "pikachu" && S.hopT != null) {
          // Up onto his shoulder in a little arc, then riding there.
          S.hopT = Math.min(1, S.hopT + dt / 0.75); const k = S.hopT, e = k * k * (3 - 2 * k);
          const hx = x + (0.3 - x) * e, hz = z + (0.02 - z) * e, hy = 1.4 * e + Math.sin(Math.PI * k) * 0.9;
          placeMon(r, hx, hz, Math.PI + turn * (1 - e) + Math.sin(t * 0.5) * 0.1 * e, hy); r.base = k < 1 ? "walk" : "ground";
        } else { placeMon(r, x, z, Math.PI + turn + Math.sin(t * 0.4 + x) * 0.15, fly ? fly + Math.sin(t * 2.2) * 0.12 : 0); if (fly) r.base = "fly"; }
        tickMon(r, dt);
      }
    };
    return S;
  },
  // The title: a few of our creatures, drawn as pixel art (the scene is rendered small and blown up).
  title() {
    const group = new THREE.Group(), S = { group, mons: [], clouds: [] }, night = LOOK === "theirs";
    const box = (w, h, d, c, x, y, z) => { const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), new THREE.MeshLambertMaterial({ color: c })); m.position.set(x, y, z); m.castShadow = true; m.receiveShadow = true; group.add(m); return m; };
    if (night) {
      // The old title: Charizard, alone, against the night sky.
      // Flying, a little turned, filling the space under the word (as the old title has it), not crowding it.
      mon("charizard").then(st => { const k = 2.2 / Math.max(0.5, st.m.size.h); st.m.holder.scale.multiplyScalar(k); st.m.size = { ...st.m.size, h: st.m.size.h * k, w: st.m.size.w * k, l: st.m.size.l * k, cx: st.m.size.cx * k, cz: st.m.size.cz * k };
        st.base = "hover"; st.fly = true; group.add(st.m.holder); placeMon(st, 0.15, 0, Math.PI - 0.45, -0.55); S.mons.push(st); S.zard = st; });
      S.enter = () => { lightFor(-2, 3, 4, 10); hemi3.color.set(0x9fb6ff); hemi3.groundColor.set(0x1a2244); look3(0, 1.45, 8.2, 0, 1.15, 0); };
    } else {
      // A blocky hill, a few trees and clouds, and our creatures on top.
      box(7.2, 0.6, 3.6, 0x6DB85B, 0, -0.3, 0); box(5.0, 0.6, 2.6, 0x7CC46A, 0, -0.02, -0.1); box(7.4, 0.12, 3.8, 0x8A6A44, 0, -0.66, 0);
      for (const [x, z, s] of [[-3.0, -1.4, 1.0], [3.1, -1.2, 1.15], [-3.6, 0.6, 0.85], [3.7, 0.4, 0.9]]) {
        box(0.3 * s, 1.2 * s, 0.3 * s, 0x7A5030, x, 0.6 * s - 0.3, z); box(1.3 * s, 1.0 * s, 1.3 * s, 0x3F8E3E, x, 1.4 * s - 0.3, z); box(0.8 * s, 0.6 * s, 0.8 * s, 0x4FA04C, x, 2.05 * s - 0.3, z);
      }
      for (const [x, y, z, w] of [[-3.2, 3.6, -6, 2.2], [1.8, 4.2, -7, 3.0], [4.6, 3.3, -5.5, 1.6], [-0.8, 4.6, -8, 1.8]]) { const c = box(w, 0.5, 0.9, 0xFFFFFF, x, y, z), top = box(w * 0.55, 0.35, 0.8, 0xFFFFFF, 0, 0, 0); group.remove(top); c.add(top); top.position.set(w * 0.1, 0.4, 0); S.clouds.push(c); }
      [["charmander", -1.35, 0.1, 0.45], ["pikachu", 0, 0.75, 0.0], ["bulbasaur", 1.35, 0.1, -0.45], ["squirtle", 0.1, -0.85, -0.1]].forEach(([sid, x, z, turn]) => mon(sid).then(st => { st.base = "ground"; group.add(st.m.holder); placeMon(st, x, z, Math.PI + turn, 0.28); S.mons.push(st); }));
      S.enter = () => { lightFor(0, 0, 6, 14); hemi3.color.set(0xffffff); hemi3.groundColor.set(0x9fc8bc); look3(0, 1.55, 5.9, 0, 1.0, 0); };
    }
    S.tick = (dt, t) => { for (const m of S.mons) tickMon(m, dt); if (S.zard) S.zard.m.holder.position.y = -0.55 + Math.sin(t * 1.6) * 0.12; S.clouds.forEach((c, i) => { c.position.x += dt * 0.12 * (1 + i * 0.3); if (c.position.x > 7) c.position.x = -7; }); };
    return S;
  },
  // Everyone of a kind, side by side on a stage: the one you pick steps forward and waves, with their partner behind.
  cast() {
    const group = new THREE.Group(), S = { group, figs: [], key: "" };
    const floor = new THREE.Mesh(new THREE.CylinderGeometry(9, 9.3, 0.2, 64), new THREE.MeshLambertMaterial({ color: 0xBFEFE0 })); floor.position.y = -0.1; floor.receiveShadow = true; group.add(floor);
    S.show = (list, sel) => {
      const key = list.map(c => c.creature || c.spec.skin || c.spec.player || JSON.stringify(c.spec.outfit)).join(",");
      if (key !== S.key) {
        for (const f of S.figs) group.remove(f.root); S.figs = [];
        const n = list.length, gap = 1.3;
        list.forEach((c, i) => { let f; if (c.creature) { f = { root: new THREE.Group(), creature: true }; mon(c.creature).then(st => { st.base = "ground"; f.st = st; f.root.add(st.m.holder); placeMon(st, 0, 0, 0); }); } else f = makeFigure(c.spec); f.root.position.set((i - (n - 1) / 2) * gap, 0, 0); f.root.rotation.y = Math.PI; group.add(f.root); f.x = f.root.position.x; f.z = 0; S.figs.push(f); });
        S.key = key; const W = (n - 1) * gap + 1.2, d = Math.max(5.2, W / (2 * Math.tan(17 * D2R) * cam3.aspect) * 1.12);
        look3(0, 2.1 + d * 0.06, d, 0, 0.75, 0);
      }
      S.sel = sel; const c = list[sel];
      if (S.ace) { group.remove(S.ace.m.holder); S.ace = null; }
      S.aceWant = c && c.ace;
      if (c && c.ace) mon(c.ace).then(st => { if (S.aceWant !== c.ace) return; if (S.ace) group.remove(S.ace.m.holder); st.base = "ground"; S.ace = st; group.add(st.m.holder); const f = S.figs[sel];
        const big = st.m.size.h > 2.2 ? 2.2 / st.m.size.h : 1; if (big < 1) { st.m.holder.scale.multiplyScalar(big); st.m.size = { ...st.m.size, h: st.m.size.h * big, w: st.m.size.w * big, l: st.m.size.l * big, cx: st.m.size.cx * big, cz: st.m.size.cz * big }; } S.aceScale = big; placeMon(st, f.x + 0.75 + st.m.size.w * 0.4, -0.4 - st.m.size.l * 0.3, Math.PI - 0.3); });
    };
    S.enter = () => { lightFor(0, 0, 9, 14); hemi3.color.set(0xffffff); hemi3.groundColor.set(0x9fc8bc); S.key = ""; };
    S.tick = (dt, t) => {
      S.figs.forEach((f, i) => { const on = i === S.sel; f.z += ((on ? 0.9 : 0) - f.z) * Math.min(1, dt * 6); f.root.position.z = f.z; if (f.creature) { if (f.st) tickMon(f.st, dt); } else poseFigure(f, { wave: on ? 0.8 : 0, headYaw: on ? 0 : Math.sin(t * 0.6 + i) * 0.25 }); });
      if (S.ace) tickMon(S.ace, dt);
    };
    return S;
  },
  // Your figure turning on a little stand, for making yourself and for changing clothes.
  creator() {
    const group = new THREE.Group(), S = { group, fig: null };
    const stand = new THREE.Mesh(new THREE.CylinderGeometry(0.9, 1.0, 0.18, 48), new THREE.MeshLambertMaterial({ color: 0xBFEFE0 })); stand.position.y = -0.09; stand.receiveShadow = true; group.add(stand);
    group.add(disc(0.85, 0xE9FFF6, 0.002));
    const turn = new THREE.Group(); group.add(turn);
    S.refresh = () => { if (S.fig) turn.remove(S.fig.root); S.fig = figureOf(G.look); turn.add(S.fig.root); };
    S.enter = o => { S.refresh(); S.spin = 0; S.side = o.side || 0; lightFor(0, 0, 4, 10); hemi3.color.set(0xffffff); hemi3.groundColor.set(0x9fc8bc); look3(o.side || 0, 1.35, 6.3, o.side || 0, 0.92, 0); };
    S.tick = (dt, t) => { S.spin += dt * (S.paused ? 0 : 0.55); turn.rotation.y = Math.PI + Math.sin(S.spin) * 0.9 + (S.face || 0); if (S.fig) poseFigure(S.fig, { wave: S.wave || 0 }); };
    return S;
  },
  // One creature on a stand: the Pokédex, a catch, an evolution.
  model() {
    const group = new THREE.Group(), S = { group, st: null, want: null };
    const stand = new THREE.Mesh(new THREE.CylinderGeometry(1.4, 1.55, 0.16, 56), new THREE.MeshLambertMaterial({ color: 0xBFEFE0 })); stand.position.y = -0.08; stand.receiveShadow = true; group.add(stand);
    const top = disc(1.3, 0xE9FFF6, 0.002); stand.add(top); top.position.y = 0.082;
    const turn = new THREE.Group(); group.add(turn);
    const glow = new THREE.Mesh(new THREE.SphereGeometry(1, 32, 16), new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0, depthWrite: false })); turn.add(glow); S.glow = glow;
    S.show = sid => {
      S.want = sid;
      return mon(sid).then(st => {
        if (S.want !== sid) return;
        if (S.st) turn.remove(S.st.m.holder);
        S.st = st; turn.add(st.m.holder); placeMon(st, 0, 0, 0);
        const H = Math.max(0.6, st.m.size.h), L = Math.max(st.m.size.l, st.m.size.w, 0.6), d = Math.max(H * 3.1, L * 2.3, 3.8) * (S.zoom || 1);
        S.frame = { d, H }; glow.scale.set(L * 0.7, H * 0.75, L * 0.7); glow.position.y = H / 2;
        look3(S.ox || 0, H * 0.62 + d * 0.16, d, S.ox || 0, H * 0.5, 0);
        stand.scale.setScalar(Math.max(0.5, Math.min(3, L * 0.55)));
      });
    };
    S.enter = o => { S.ox = o.ox || 0; S.zoom = o.zoom || 1; stand.visible = o.stand !== false; S.spin = 0; S.glowOn = 0; S.hide = false; S.turn = true; turn.rotation.y = Math.PI; lightFor(0, 0, 6, 12); hemi3.color.set(0xffffff); hemi3.groundColor.set(0x9fc8bc); if (o.sid) S.show(o.sid); };
    S.tick = (dt, t) => { S.spin += dt * (S.turn === false ? 0 : 0.45); turn.rotation.y = Math.PI + Math.sin(S.spin) * 0.7; glow.material.opacity += ((S.glowOn || 0) * (0.65 + 0.3 * Math.sin(t * 12)) - glow.material.opacity) * Math.min(1, dt * 8); if (S.st) { S.st.m.holder.visible = !S.hide; tickMon(S.st, dt); } };
    return S;
  },
  // A battle: your creature from behind, theirs facing you, each on its patch of ground; you and their trainer behind.
  battle() {
    const group = new THREE.Group(), S = { group };
    const g = new THREE.Mesh(new THREE.PlaneGeometry(80, 60), new THREE.MeshLambertMaterial({ map: (() => { const t = pixTex("grass"); const c = t.clone(); c.needsUpdate = true; c.repeat.set(40, 30); return c; })() }));
    g.rotation.x = -Math.PI / 2; g.receiveShadow = true; group.add(g);
    const pad = (x, z, r) => { const p = new THREE.Mesh(new THREE.CylinderGeometry(r, r * 1.06, 0.12, 56), new THREE.MeshLambertMaterial({ color: 0xCFE7A8 })); p.position.set(x, 0.06, z); p.receiveShadow = true; group.add(p); const q = disc(r * 0.8, 0xE2F1C2, 0.125); q.position.x = x; q.position.z = z; group.add(q); };
    pad(0, 0, 2.0); pad(-1.2, 5.0, 2.1);
    for (const [x, z, s] of [[-9, -6, 1.3], [-6, -9, 1.1], [7, -8, 1.2], [10, -4, 1.0], [-11, 2, 1.1], [12, 3, 1.2]]) { const tr = new Geo(); const R = { geo: tr, solid() {} }; tree(R, x, z, s); group.add(tr.build()); }
    const ball = new THREE.Group(); const top = new THREE.Mesh(new THREE.SphereGeometry(0.16, 24, 12, 0, Math.PI * 2, 0, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0xE5525C })); const bot = new THREE.Mesh(new THREE.SphereGeometry(0.16, 24, 12, 0, Math.PI * 2, Math.PI / 2, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0xF4F6F5 }));
    const band = new THREE.Mesh(new THREE.TorusGeometry(0.16, 0.018, 8, 32), new THREE.MeshLambertMaterial({ color: 0x26304A })); band.rotation.x = Math.PI / 2; const btn = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 0.03, 16), new THREE.MeshLambertMaterial({ color: 0xffffff })); btn.rotation.x = Math.PI / 2; btn.position.z = 0.16;
    ball.add(top, bot, band, btn); ball.visible = false; group.add(ball); S.ball = ball;
    let you = null, lookKey = "", foeFig = figureOf(BUGBOY_LOOK); group.add(foeFig.root); foeFig.root.position.set(2.2, 0, -2.4); foeFig.root.rotation.y = Math.PI - 0.35; S.foeFig = foeFig;
    S.load = async (ours, foe) => {
      S.key = ours + "|" + foe;
      const [a, b] = await Promise.all([mon(ours), mon(foe)]);
      if (S.key !== ours + "|" + foe) return;
      if (S.ours) group.remove(S.ours.m.holder); if (S.foe) group.remove(S.foe.m.holder);
      S.ours = a; S.foe = b; group.add(a.m.holder, b.m.holder);
      placeMon(a, -1.2, 5.0, 0); placeMon(b, 0, 0, Math.PI);
      S.frame();
    };
    S.frame = () => {
      const fh = S.foe ? Math.max(0.8, S.foe.m.size.h) : 1, oh = S.ours ? Math.max(0.8, S.ours.m.size.h) : 1, k = Math.max(1, Math.max(fh, oh) / 1.2);
      look3(1.2 * k, 1.6 + 0.9 * k, 5.2 + 5.6 * k, -1.0, 0.55 + fh * 0.3, 1.4);
    };
    S.enter = o => {
      const k = JSON.stringify(G.look);
      if (k !== lookKey) { if (you) group.remove(you.root); you = figureOf(G.look); group.add(you.root); lookKey = k; S.you = you; }
      you.root.position.set(-2.35, 0, 7.5); you.root.rotation.y = 0.2;
      foeFig.root.visible = !!o.trainer;
      S.load(o.ours || "pikachu", o.foe || "caterpie");
      S.frame(); lightFor(0, 3, 14, 22); hemi3.color.set(0xeaf6ff); hemi3.groundColor.set(0x7a9a5a);
      ball.visible = false; S.foeHidden = 0; S.oursHidden = 0;
    };
    S.throwBall = (onLand) => {
      ball.visible = true; S.throw = { t: 0, onLand, from: new THREE.Vector3(-2.8, 1.6, 6.2), to: new THREE.Vector3(0, 0.95, 0.2) };
    };
    S.tick = (dt, t) => {
      if (S.ours) { tickMon(S.ours, dt); S.ours.m.holder.visible = !S.oursHidden; }
      if (S.foe) { tickMon(S.foe, dt); S.foe.m.holder.scale.setScalar((S.foe.m.info.scale || 1) / 16 * (1 - (S.foeHidden || 0))); }
      if (you) poseFigure(you, { cheer: S.cheer || 0, point: S.point || 0 });
      if (foeFig.root.visible) poseFigure(foeFig, { point: S.foePoint || 0 });
      if (S.throw) {
        const th = S.throw; th.t += dt;
        if (th.t < 0.7) { const k = th.t / 0.7; ball.position.lerpVectors(th.from, th.to, k); ball.position.y += Math.sin(k * Math.PI) * 2.2; ball.rotation.x += dt * 16; }
        else if (!th.landed) { th.landed = true; ball.position.copy(th.to); ball.rotation.set(0, 0, 0); if (th.onLand) th.onLand(); }
      }
      // Three wobbles, a beat apart: the ball tips side to side and rocks on the ground, then settles.
      if (S.shakeT != null) { S.shakeT += dt; const k = S.shakeT, n = Math.floor(k / 1.1), ph = k % 1.1, on = n < 3 && ph < 0.75, env = on ? Math.sin(ph * Math.PI / 0.75) : 0;
        ball.rotation.z = Math.sin(ph * 16) * 0.55 * env; ball.position.x = 0 + Math.sin(ph * 16) * 0.08 * env; ball.position.y = Math.max(0.22, ball.position.y - dt * 3); }
    };
    return S;
  },
  // A shop or the healing desk: a counter, someone behind it, you in front.
  counter() {
    const group = new THREE.Group(), S = { group };
    const build = kind => {
      const R = new Room("ctr" + kind, { W: 12, D: 9, H: 4 });
      shell(R, { paper: kind === "center" ? "paperMum" : "paperRival", floor: kind === "center" ? "checker" : "paleFloor" });
      const F = R.F(2.5, 3.2, "s");
      for (let i = 0; i < 7; i++) counterUnit(R, F, i, 1, kind === "center" ? "cupboardWhite" : "cupboard", "counter", kind === "center" ? "paint" : "mintPaint");
      if (kind === "center") {
        for (const x of [0.6, 4.0]) R.geo.box(x, 0, 0, x + 2.2, 3.0, 0.7, { all: "paint", s: "cupboardWhite" });
        const M = R.F(7.2, 0.0, "s"); M.box(0, 0, 0, 2.4, 1.7, 1.0, { all: "silver", top: "darkTop" }); M.box(0.1, 1.7, 0.1, 2.3, 2.3, 0.9, { all: "glass" });
        windowN(R, 9.8, 1.6, 1.8, 1.6, { pool: false });
      } else {
        bookshelf(R, 0.3, 0, "s", 2.4, 3.2, { trinkets: { 0: "ball", 2: "plant" } }); bookshelf(R, 2.9, 0, "s", 2.4, 3.2, { trinkets: { 1: "ball" } }); bookshelf(R, 9.3, 0, "s", 2.4, 3.2, {});
        R.geo.box(5.6, 0, 0, 9.0, 2.6, 0.8, { all: "paint", s: "fridge" });
      }
      R.finish(); return R;
    };
    S.rooms = { mart: build("mart"), center: build("center") };
    for (const r of Object.values(S.rooms)) group.add(r.group);
    const staff = { mart: figureOf(CLERK_LOOK), center: figureOf(NURSE_LOOK) };
    for (const [k, f] of Object.entries(staff)) { f.root.position.set(6.0, 0, 2.4); f.root.rotation.y = Math.PI; group.add(f.root); }
    let you = null, lookKey = "";
    S.balls = [];
    for (let i = 0; i < 6; i++) { const b = new THREE.Mesh(new THREE.SphereGeometry(0.16, 16, 10), new THREE.MeshLambertMaterial({ color: 0xE5525C, emissive: 0x000000 })); b.position.set(7.55 + (i % 3) * 0.55, 1.88, 0.3 + Math.floor(i / 3) * 0.45); group.add(b); S.balls.push(b); }
    S.enter = o => {
      S.kind = o.kind || "mart";
      for (const [k, r] of Object.entries(S.rooms)) r.group.visible = k === S.kind;
      for (const [k, f] of Object.entries(staff)) f.root.visible = k === S.kind;
      for (const b of S.balls) { b.visible = S.kind === "center"; b.material.emissive.setHex(0); }
      S.staff = staff[S.kind];
      const k = JSON.stringify(G.look);
      if (k !== lookKey) { if (you) group.remove(you.root); you = figureOf(G.look); group.add(you.root); lookKey = k; }
      you.root.position.set(5.0, 0, 5.4); you.root.rotation.y = -0.2;
      lightFor(6, 4, 10, 16); hemi3.color.set(0xfff6e8); hemi3.groundColor.set(0x8a7f74);
      look3(4.6, 4.6, 10.6, 6.0, 1.3, 3.0);
      S.heal = 0;
    };
    S.tick = (dt, t) => {
      if (S.staff) poseFigure(S.staff, { talk: S.talking, nod: S.bow ? 0.35 : 0 });
      if (you) poseFigure(you, {});
      if (S.kind === "center") S.balls.forEach((b, i) => { const on = S.heal > 0 && S.heal * 6 > i; b.material.emissive.setHex(on ? (Math.sin(t * 14 + i) > 0 ? 0x8a2a30 : 0xff9aa0) : 0); });
    };
    return S;
  },
  // Trading: yours on the left, theirs on the right; balls cross between them.
  trade() {
    const group = new THREE.Group(), S = { group };
    const stand = x => { const s = new THREE.Mesh(new THREE.CylinderGeometry(1.2, 1.3, 0.16, 48), new THREE.MeshLambertMaterial({ color: 0xBFEFE0 })); s.position.set(x, -0.08, 0); s.receiveShadow = true; group.add(s); };
    stand(-2.2); stand(2.2);
    const mk = c => { const b = new THREE.Mesh(new THREE.SphereGeometry(0.22, 20, 12), new THREE.MeshLambertMaterial({ color: c })); b.visible = false; group.add(b); return b; };
    S.b1 = mk(0xE5525C); S.b2 = mk(0x4A8FE0);
    S.load = async (a, b) => {
      const [x, y] = await Promise.all([mon(a), mon(b)]);
      if (S.a) group.remove(S.a.m.holder); if (S.b) group.remove(S.b.m.holder);
      S.a = x; S.b = y; group.add(x.m.holder, y.m.holder); placeMon(x, -2.2, 0, Math.PI * 0.82); placeMon(y, 2.2, 0, -Math.PI * 0.82);
    };
    S.enter = o => { S.load(o.a || "sandshrew", o.b || "farfetch_d"); S.swap = null; S.b1.visible = S.b2.visible = false; lightFor(0, 0, 6, 12); hemi3.color.set(0xffffff); hemi3.groundColor.set(0x9fc8bc); look3(0, 2.2, 7.6, 0, 0.8, 0); };
    S.cross = done => { S.swap = { t: 0, done }; };
    S.tick = (dt, t) => {
      if (S.a) tickMon(S.a, dt); if (S.b) tickMon(S.b, dt);
      if (S.swap) {
        const w = S.swap; w.t += dt; const k = Math.min(1, w.t / 2.2);
        const showMons = k < 0.15 || k > 0.85;
        if (S.a) S.a.m.holder.visible = showMons; if (S.b) S.b.m.holder.visible = showMons;
        S.b1.visible = S.b2.visible = !showMons;
        const m = Math.max(0, Math.min(1, (k - 0.15) / 0.7));
        S.b1.position.set(-2.2 + 4.4 * m, 0.8 + Math.sin(m * Math.PI) * 1.6, 0); S.b2.position.set(2.2 - 4.4 * m, 0.8 + Math.sin(m * Math.PI) * 1.2, 0.3);
        if (k > 0.85 && !w.swapped) { w.swapped = true; const ax = S.a; S.a = S.b; S.b = ax; placeMon(S.a, -2.2, 0, Math.PI * 0.82); placeMon(S.b, 2.2, 0, -Math.PI * 0.82); }
        if (k >= 1) { S.swap = null; if (w.done) w.done(); }
      }
    };
    return S;
  },
};
let last3 = performance.now();
function loop3(now) {
  const dt = Math.min(0.05, (now - last3) / 1000); last3 = now;
  if (scNow && scNow.tick) scNow.tick(dt, now / 1000);
  renderer.render(scene3, cam3);
  requestAnimationFrame(loop3);
}
