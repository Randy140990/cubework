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

// The title's own Minecraft blocks: short grass, five flowers, oak log and leaves, and the plain far greens.
function titleBlocks() {
  if (MATDEF.tShort) return;
  const sprite = (key, draw) => { TEXDEF[key] = [16, 16, draw]; MATDEF[key] = { tex: key, fit: true, cut: true, double: true }; };
  sprite("tShort", (P, r) => { for (let x = 0; x < 16; x++) { if (r() < 0.2) continue; const hgt = 5 + ((r() * 10) | 0); for (let y = 16 - hgt; y < 16; y++) P.set(x, y, shadec(hx("#5AAE45"), (y - 16 + hgt) * -0.012 + (x % 2 ? 0.07 : -0.05) + (y === 16 - hgt ? 0.2 : 0))); } });
  const flower = (key, petal, centre, shape) => sprite(key, (P, r) => {
    P.line(8, 6, 8, 15, hx("#3E8A34")); P.set(7, 11, hx("#58A845")); P.set(6, 10, hx("#58A845")); P.set(9, 13, hx("#58A845")); P.set(10, 12, hx("#58A845"));
    for (const [dx, dy] of shape) P.set(8 + dx, 4 + dy, (dy < 0 ? shadec(petal, 0.18) : dy > 0 ? shadec(petal, -0.15) : petal));
    if (centre) P.set(8, 4, centre);
  });
  const ROUND = [[0, -2], [-1, -1], [0, -1], [1, -1], [-2, 0], [-1, 0], [1, 0], [2, 0], [-1, 1], [0, 1], [1, 1], [0, 2]];
  flower("tPoppy", hx("#E0362E"), hx("#2A2A2A"), ROUND);
  flower("tDandelion", hx("#F7D433"), hx("#E8A21E"), [[-1, -1], [0, -1], [1, -1], [-1, 0], [1, 0], [-1, 1], [0, 1], [1, 1]]);
  flower("tDaisy", hx("#F6F6F0"), hx("#F2C12E"), ROUND);
  flower("tCornflower", hx("#4A7BE0"), hx("#2E4FA8"), [[0, -2], [-1, -1], [1, -1], [-2, 0], [2, 0], [-1, 1], [1, 1], [0, 1], [-1, 0], [1, 0]]);
  flower("tAllium", hx("#B66FE0"), null, [[0, -2], [-1, -1], [0, -1], [1, -1], [-1, 0], [1, 0], [0, 1], [-1, 1], [1, 1]]);
  TEXDEF.tLog = [16, 16, (P, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) P.set(x, y, shadec(hx("#6E4F32"), (x % 4 === 0 ? -0.22 : x % 4 === 2 ? 0.06 : 0) + ((y * 7 + x * 3) % 9 === 0 ? -0.12 : 0) + (r() - 0.5) * 0.1)); }];
  TEXDEF.tLogTop = [16, 16, (P, r) => { P.fill(hx("#6E4F32"), 0.05, r); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5)); if (d < 6.5) P.set(x, y, shadec(hx("#B8925C"), (Math.floor(d) % 2 ? -0.1 : 0.04) + (r() - 0.5) * 0.05)); } }];
  TEXDEF.tLeaf = [16, 16, (P, r) => leaves(P, r, hx("#4A9A3C"), hx("#78C45A"), hx("#2E6A2A"), 0.1)];
  MATDEF.tLog = { tex: "tLog", per: 1 }; MATDEF.tLogTop = { tex: "tLogTop", per: 1 }; MATDEF.tLeaf = { tex: "tLeaf", per: 1, cut: true };
  TEXDEF.tFar = [4, 4, (P, r) => P.fill(hx("#7DC25A"), 0.0, r)]; MATDEF.tFar = { tex: "tFar", per: 40 };
  TEXDEF.tFarLeaf1 = [16, 16, (P, r) => leaves(P, r, hx("#5FA858"), hx("#86C478"), hx("#447F44"), 0)]; MATDEF.tFarLeaf1 = { tex: "tFarLeaf1", per: 1 };
  TEXDEF.tFarLeaf2 = [16, 16, (P, r) => leaves(P, r, hx("#86BE88"), hx("#A2D2A2"), hx("#6E9E72"), 0)]; MATDEF.tFarLeaf2 = { tex: "tFarLeaf2", per: 1 };
  TEXDEF.tFarLog = [4, 4, (P, r) => P.fill(hx("#8C7A62"), 0.0, r)]; MATDEF.tFarLog = { tex: "tFarLog", per: 1 };
}

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
      if (you && you.root.visible) poseFigure(you, { wave: S.youWave || 0, floss: S.youFloss || 0, lookUp: S.youLookUp || 0 });
      S.nextCry -= dt;
      if (S.nextCry < 0 && S.runners.length) { const r = S.runners[Math.floor(Math.random() * S.runners.length)]; playOnce(r, "cry"); S.nextCry = 2.5 + Math.random() * 2.5; }
      for (const r of S.runners) {
        const [x, z, turn, fly] = r.home;
        if (r.sid === "cubone") {
          // Wandering round in a slow loop, facing the way it goes.
          // Round the professor in a wide loop, clear of the others, facing the way it walks.
          const a = t * 0.35, wx = Math.cos(a) * 2.55, wz = 0.35 + Math.sin(a) * 2.25;
          placeMon(r, wx, wz, Math.atan2(Math.sin(a) * 2.55, -Math.cos(a) * 2.25)); r.base = "walk";
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
      // As Let's Go's title, built from Minecraft's own blocks: a meadow of grass blocks, short grass and flowers as
      // crossed cut-outs, leaf-block bushes, oak trees (a log of bark, leaves in Minecraft's oak shape) framing it
      // and running back to the horizon, and flat blocky clouds drifting across. The sun is in the sky behind (the
      // page's glare, top right), so the light comes from the right.
      titleBlocks();
      let seed = 7; const rnd = () => (seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296;
      const G = new Geo(), far = new Geo();
      // The ground: grass blocks near, the same green, plain, further out (so it doesn't shimmer at a distance).
      G.box(-14, -1, -14, 14, 0, 8, { top: "grass", all: null });
      far.box(-80, -1.002, -90, 80, -0.002, 8, { top: "tFar", all: null });
      const cross = (x, z, k, hgt = 1) => { G.box(x + 0.15, 0, z + 0.5, x + 0.85, hgt, z + 0.5, { s: k, all: null }); G.box(x + 0.5, 0, z + 0.15, x + 0.5, hgt, z + 0.85, { e: k, all: null }); };
      const clear = (x, z) => Math.abs(x + 0.5) < 2.6 && z > -1.6 && z < 1.8;
      const FL = ["tPoppy", "tDandelion", "tDaisy", "tCornflower", "tAllium"];
      // Grass and flowers kept light: a scatter round the creatures' feet and out to the sides, thinning towards you,
      // so the meadow reads as grass without a wall of it in front.
      for (let x = -14; x < 14; x++) for (let z = -13; z < 4; z++) {
        const v = rnd(), front = z > 0, mid = Math.abs(x + 0.5) < 3.5;
        if (front && mid) continue;
        if (clear(x, z)) { if (v < 0.1) cross(x, z, "tShort", 0.4); continue; }
        const p = front ? 0.1 : z > -4 ? 0.22 : 0.3;
        if (v < p) cross(x, z, "tShort", 0.45 + rnd() * 0.25);
        else if (v < p + 0.06 && !front) cross(x, z, FL[Math.floor(rnd() * FL.length)], 0.6);
      }
      // Bushes behind: a leaf block, or two or three together, scattered across the meadow.
      for (let i = 0; i < 16; i++) { const x = -13 + Math.floor(rnd() * 26), z = -5 - Math.floor(rnd() * 9); if (Math.abs(x) < 2 && z > -7) continue;
        G.box(x, 0, z, x + 1, 1, z + 1, "tLeaf"); if (rnd() < 0.6) G.box(x + 1, 0, z, x + 2, 1, z + 1, "tLeaf"); if (rnd() < 0.35) G.box(x, 1, z, x + 1, 2, z + 1, "tLeaf"); }
      // An oak, as Minecraft grows one: a log 4 to 6 high; two layers of leaves 5 across with the corners mostly gone;
      // a layer 3 across; a plus on top.
      const oak = (Gx, x, z, hgt, leaf = "tLeaf", log = "tLog") => {
        Gx.box(x, 0, z, x + 1, hgt, z + 1, { all: log, top: "tLogTop" });
        for (const [y, r, cut] of [[hgt - 2, 2, 0.6], [hgt - 1, 2, 0.75], [hgt, 1, 0.3], [hgt + 1, 1, 1]]) {
          for (let i = -r; i <= r; i++) for (let k = -r; k <= r; k++) {
            if (i === 0 && k === 0 && y < hgt) continue;
            const corner = Math.abs(i) === r && Math.abs(k) === r;
            if (y === hgt + 1 && corner) continue;
            if (corner && rnd() < cut) continue;
            Gx.box(x + i, y, z + k, x + i + 1, y + 1, z + k + 1, leaf);
          }
        }
      };
      // A big one on the left, framing the creatures; a smaller one further back on the right, below the sun, so the sky
      // and the sun's glare stay open top right. One more far back each side.
      oak(G, -8, -3, 5); oak(G, 9, -9, 4); oak(G, -13, -12, 5); oak(G, 14, -16, 4);
      // The horizon: oaks and bushes a long way off, with gaps between, in plain greens that pale with distance.
      for (let i = 0; i < 26; i++) {
        const z = -42 - Math.floor(rnd() * 20), x = Math.round(-70 + i * 5.6 + (rnd() - 0.5) * 3), hz = Math.min(1, (-z - 40) / 20);
        if (rnd() < 0.8) oak(far, x, z, 4 + Math.floor(rnd() * 2), hz < 0.5 ? "tFarLeaf1" : "tFarLeaf2", "tFarLog");
        far.box(x + 3, 0, z + 2, x + 5 + Math.floor(rnd() * 2), 1 + (rnd() < 0.5 ? 1 : 0), z + 4, hz < 0.5 ? "tFarLeaf1" : "tFarLeaf2");
      }
      const near = G.build(), farG = far.build(); group.add(near); group.add(farG);
      farG.traverse(o => { if (o.isMesh) o.castShadow = false; });
      // Minecraft's clouds: flat white slabs, blocky at the edges, drifting slowly across.
      const cloudMat = new THREE.MeshLambertMaterial({ color: 0xffffff, emissive: 0xffffff, emissiveIntensity: 0.35 });
      for (const [x, y, z, w, d] of [[-7, 5.0, -16, 6, 2], [4, 6.2, -20, 8, 3], [12, 4.6, -18, 4, 2], [-1, 7.4, -26, 7, 3], [-15, 5.6, -22, 5, 2], [20, 6.6, -24, 6, 2]]) {
        const c = new THREE.Group();
        const slab = (bx, bz, bw, bd) => { const m = new THREE.Mesh(new THREE.BoxGeometry(bw, 0.8, bd), cloudMat); m.position.set(bx, 0, bz); c.add(m); };
        slab(0, 0, w, d); slab(-w * 0.2, -d * 0.5 - 0.5, w * 0.5, 1); slab(w * 0.25, d * 0.5 + 0.5, w * 0.4, 1);
        c.position.set(x, y, z); group.add(c); S.clouds.push(c);
      }
      [["charmander", -1.45, 0.05, 0.45], ["pikachu", 0, 0.8, 0.0], ["bulbasaur", 1.45, 0.05, -0.45], ["squirtle", 0.75, -1.05, -0.2]].forEach(([sid, x, z, turn]) => mon(sid).then(st => { st.base = "ground"; group.add(st.m.holder); placeMon(st, x, z, Math.PI + turn, 0); S.mons.push(st); }));
      S.enter = () => {
        lightFor(0, 0, 7, 14); sun3.position.set(6, 11, 1.5); sun3.target.position.set(0, 0, 0);
        hemi3.color.set(0xffffff); hemi3.groundColor.set(0xa8d08c); look3(0, 1.3, 6.4, 0, 1.06, 0);
      };
    }
    S.tick = (dt, t) => { for (const m of S.mons) tickMon(m, dt); if (S.zard) S.zard.m.holder.position.y = -0.55 + Math.sin(t * 1.6) * 0.12; S.clouds.forEach((c, i) => { c.position.x += dt * (S.night ? 0.12 : 0.55) * (1 + (i % 3) * 0.25); const lim = S.night ? 7 : 30; if (c.position.x > lim) c.position.x = -lim; }); };
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
    // A burst of white light where a ball opens.
    const burstTex = (() => { const cv = document.createElement("canvas"); cv.width = cv.height = 128; const x = cv.getContext("2d"), g = x.createRadialGradient(64, 64, 2, 64, 64, 62); g.addColorStop(0, "rgba(255,255,255,1)"); g.addColorStop(0.35, "rgba(255,255,240,.85)"); g.addColorStop(1, "rgba(255,255,255,0)"); x.fillStyle = g; x.fillRect(0, 0, 128, 128); return new THREE.CanvasTexture(cv); })();
    const burst = new THREE.Sprite(new THREE.SpriteMaterial({ map: burstTex, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0 })); burst.visible = false; group.add(burst);
    S.burst = (pos, size = 2.4) => { burst.position.copy(pos); burst.visible = true; S.burstT = 0; S.burstSize = size; };
    // A ball along a path of legs: each from, to, how long, how high it arcs, and what happens when it gets there.
    S.ballPath = (legs, done) => { ball.visible = true; S.path = { legs, i: 0, t: 0, done }; };
    S.frame = () => {
      if (S.holdCam) return;
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
      ball.visible = false; S.foeHidden = 0; S.oursHidden = 0; S.foeRed = 0; S.path = null; S.holdCam = false; S.foeCheer = 0;
    };
    S.throwBall = (onLand) => {
      ball.visible = true; S.throw = { t: 0, onLand, from: new THREE.Vector3(-2.8, 1.6, 6.2), to: new THREE.Vector3(0, 0.95, 0.2) };
    };
    S.tick = (dt, t) => {
      if (S.ours) { tickMon(S.ours, dt); const k = 1 - (+S.oursHidden || 0); S.ours.m.holder.visible = k > 0.001; S.ours.m.holder.scale.setScalar((S.ours.m.info.scale || 1) / 16 * Math.max(0.001, k)); }
      if (S.foe) { tickMon(S.foe, dt); S.foe.m.holder.scale.setScalar((S.foe.m.info.scale || 1) / 16 * Math.max(0.001, 1 - (S.foeHidden || 0)));
        // Turning to red light as the ball draws it in.
        const red = S.foeRed || 0; if (red !== S.foeRedWas) { S.foeRedWas = red; S.foe.m.holder.traverse(o => { if (o.isMesh && o.material && o.material.emissive) o.material.emissive.setRGB(red * 0.95, red * 0.12, red * 0.2); }); } }
      if (S.burstT != null) { S.burstT += dt; const k = S.burstT / 0.5; if (k >= 1) { burst.visible = false; S.burstT = null; } else { burst.scale.setScalar(S.burstSize * (0.3 + k * 0.9)); burst.material.opacity = 1 - k; } }
      if (S.path) { const P = S.path, L = P.legs[P.i]; P.t += dt; const k = Math.min(1, P.t / L.dur), e = L.ease === "out" ? 1 - (1 - k) * (1 - k) : k;
        ball.position.lerpVectors(L.from, L.to, e); ball.position.y += Math.sin(k * Math.PI) * (L.arc || 0); ball.rotation.x += dt * (L.spin ?? 12);
        if (k >= 1) { if (L.at) L.at(); P.i++; P.t = 0; if (P.i >= P.legs.length) { S.path = null; ball.rotation.set(0, 0, 0); if (P.done) P.done(); } } }
      if (you) poseFigure(you, { cheer: S.cheer || 0, point: S.point || 0 });
      if (foeFig.root.visible) poseFigure(foeFig, { point: S.foePoint || 0, cheer: S.foeCheer || 0, wave: S.foeWave || 0 });
      if (S.throw) {
        const th = S.throw; th.t += dt;
        if (th.t < 0.7) { const k = th.t / 0.7; ball.position.lerpVectors(th.from, th.to, k); ball.position.y += Math.sin(k * Math.PI) * 2.2; ball.rotation.x += dt * 16; }
        else if (!th.landed) { th.landed = true; ball.position.copy(th.to); ball.rotation.set(0, 0, 0); if (th.onLand) th.onLand(); }
      }
      // Three wobbles, a beat apart: the ball tips side to side and rocks on the ground, then settles.
      if (S.shakeT != null) { S.shakeT += dt; const k = S.shakeT, n = Math.floor(k / 1.5), ph = k % 1.5, on = n < 3 && ph < 0.9, env = on ? Math.sin(ph * Math.PI / 0.9) : 0;
        ball.rotation.z = Math.sin(ph * 11) * 0.6 * env; ball.position.x = (S.restX || 0) + Math.sin(ph * 11) * 0.09 * env; ball.position.y = Math.max(0.17, ball.position.y - dt * 3); }
    };
    return S;
  },
  // A shop or the healing desk: a counter, someone behind it, you in front.
  // At the counter: the real Pokémon Center and Poké Mart (the Interior Viewer's rooms), the nurse or the clerk behind
  // it and you in front. At the Center your balls go onto her machine one by one, glow, and come back.
  counter() {
    const group = new THREE.Group(), S = { group };
    const build = id => { const R = BUILD[id](); R.finish(); return R; };
    S.rooms = { mart: build("mart"), center: build("center") };
    for (const r of Object.values(S.rooms)) group.add(r.group);
    const staff = { mart: figureOf(CLERK_LOOK), center: figureOf(NURSE_LOOK) };
    staff.center.root.position.set(10.0, 0, 3.3); staff.center.root.rotation.y = Math.PI;   // facing you across the counter
    staff.mart.root.position.set(1.7, 0, 9.4); staff.mart.root.rotation.y = -Math.PI / 2;   // facing you across the counter
    for (const f of Object.values(staff)) group.add(f.root);
    let you = null, lookKey = "";
    // Six balls: from you to the nurse, onto the machine's six cups (x 6.45 to 8.55, z .35 and .65 at y 1.24), lit, back.
    const SLOT = i => new THREE.Vector3(6.45 + (i % 3) * 1.05, 1.36, 0.35 + Math.floor(i / 3) * 0.3);
    const HAND = new THREE.Vector3(10.0, 1.2, 4.0);
    S.balls = [];
    for (let i = 0; i < 6; i++) {
      const b = new THREE.Group(), r = 0.15;
      const top = new THREE.Mesh(new THREE.SphereGeometry(r, 18, 10, 0, Math.PI * 2, 0, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0xE5525C, emissive: 0 }));
      const bot = new THREE.Mesh(new THREE.SphereGeometry(r, 18, 10, 0, Math.PI * 2, Math.PI / 2, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0xF4F6F5, emissive: 0 }));
      const band = new THREE.Mesh(new THREE.CylinderGeometry(r * 1.02, r * 1.02, r * 0.18, 18), new THREE.MeshLambertMaterial({ color: 0x26304A }));
      b.add(top, bot, band); b.visible = false; b.userData.m = [top.material, bot.material]; group.add(b); S.balls.push(b);
    }
    // The machine's glow while it works.
    const glow = new THREE.Mesh(new THREE.PlaneGeometry(3.2, 1.6), new THREE.MeshBasicMaterial({ color: 0xFFB8C8, transparent: true, opacity: 0, depthWrite: false, blending: THREE.AdditiveBlending }));
    glow.position.set(7.5, 1.9, 0.18); group.add(glow);
    S.enter = o => {
      S.kind = o.kind || "mart";
      for (const [k, r] of Object.entries(S.rooms)) r.group.visible = k === S.kind;
      for (const [k, f] of Object.entries(staff)) f.root.visible = k === S.kind;
      for (const b of S.balls) b.visible = false;
      S.staff = staff[S.kind];
      const k = JSON.stringify(G.look);
      if (k !== lookKey) { if (you) group.remove(you.root); you = figureOf(G.look); group.add(you.root); lookKey = k; }
      if (S.kind === "center") { you.root.position.set(10.0, 0, 6.3); you.root.rotation.y = 0; lightFor(10, 6, 12, 16); look3(12.6, 5.6, 13.2, 9.0, 1.4, 3.4); }
      else { you.root.position.set(5.5, 0, 9.4); you.root.rotation.y = Math.PI / 2; lightFor(5, 8, 12, 16); look3(9.4, 5.0, 14.6, 3.6, 1.3, 8.6); }
      hemi3.color.set(0xfffaf2); hemi3.groundColor.set(0x9a9fb0);
      S.heal = 0;
    };
    const ease = x => x < 0 ? 0 : x > 1 ? 1 : x * x * (3 - 2 * x);
    S.tick = (dt, t) => {
      const R = S.rooms[S.kind]; if (R) for (const sc of R.screens) sc.tick(performance.now());
      if (S.staff) poseFigure(S.staff, { talk: S.talking, nod: S.bow ? 0.35 : 0, point: S.kind === "center" && S.heal > 0 && S.heal < 0.42 ? 0.6 : 0 });
      if (you) poseFigure(you, {});
      // Healing: 0 to .4 the balls go on one by one, .4 to .75 the machine works (the balls flashing), .75 to 1 they come back.
      const h = S.heal || 0, n = Math.min(6, G.party.length);
      glow.material.opacity = S.kind === "center" && h > 0.4 && h < 0.78 ? 0.35 + 0.3 * Math.sin(t * 16) : 0;
      S.balls.forEach((b, i) => {
        if (S.kind !== "center" || !h || i >= n) { b.visible = false; return; }
        const on0 = i * 0.06, a = ease((h - on0) / 0.06), back = ease((h - 0.78 - i * 0.02) / 0.12);
        b.visible = h > on0 && back < 1;
        const p = HAND.clone().lerp(SLOT(i), a); p.y += Math.sin(a * Math.PI) * 0.45;
        if (back > 0) { p.copy(SLOT(i).lerp(HAND, back)); p.y += Math.sin(back * Math.PI) * 0.45; }
        b.position.copy(p); b.rotation.y = t * 2;
        const lit = h > 0.4 && h < 0.78 && Math.sin(t * 14 + i * 1.3) > 0;
        for (const m of b.userData.m) m.emissive.setHex(lit ? 0x7A3040 : 0);
      });
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
