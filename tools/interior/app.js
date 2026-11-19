// ---------- the stage ----------
const viewerEl = $("#viewer"), canvas = $("#c");
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
const scene = new THREE.Scene(); scene.background = new THREE.Color(0x000000);
const camera = new THREE.PerspectiveCamera(32, 16 / 10, 0.1, 400);
const hemi = new THREE.HemisphereLight(0xfff6e8, 0x8a7f74, 0.62); scene.add(hemi);
const ambient = new THREE.AmbientLight(0xffffff, 0.12); scene.add(ambient);
const sun = new THREE.DirectionalLight(0xffffff, 0.6); sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048);
sun.shadow.bias = -0.0006; sun.shadow.normalBias = 0.02; sun.shadow.radius = 3; scene.add(sun); scene.add(sun.target);
const actors = new THREE.Group(); scene.add(actors);

// ---------- the follower: any Retromon creature (or its Cobblemon model), at its true size ----------
const fol = { sid: null, look: "ours", model: null, size: null, ride: "walk", x: 0, z: 0, y: 0, yaw: 0, spd: 0, t: 0, want: "pikachu" };
const trail = [];
async function setFollower(sid) {
  fol.want = sid;
  if (fol.model) { actors.remove(fol.model.holder); actors.remove(fol.shadow); fol.model = null; }
  fol.sid = null; renderPanel();
  if (!sid) return;
  $("#loadNote").hidden = false; $("#loadNote").textContent = "Fetching " + nameOf(sid) + "...";
  try { await loadSpecies(sid); } catch (e) { $("#loadNote").textContent = "Couldn't fetch that creature. Try again."; return; }
  if (fol.want !== sid) return;
  $("#loadNote").hidden = true;
  const d = DATA.species[sid], entry = fol.look === "theirs" ? d.theirs : d.ours;
  const model = buildModel(entry);
  model.holder.traverse(o => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = false; } });
  fol.size = sizeOf(model); fol.model = model; fol.sid = sid;
  const sh = new THREE.Mesh(new THREE.PlaneGeometry(1, 1), matFor("shadowBlob")); sh.rotation.x = -Math.PI / 2; sh.renderOrder = 3;
  fol.shadow = sh; model.shadowMesh = sh;
  actors.add(model.holder); actors.add(sh);
  if (fol.ride === "shoulder" && !canRide()) setRide("walk");
  placeFollowerBehind();
  renderRide(); renderPanel();
}
function canRide() { return fol.size && fol.size.h <= 1.05; }
function gapFor() { return fol.size ? 0.45 + 0.28 + Math.max(fol.size.l, fol.size.w) * 0.5 : 1; }
function placeFollowerBehind() {
  const g = gapFor();
  fol.x = me.x + Math.sin(me.yaw) * g; fol.z = me.z + Math.cos(me.yaw) * g; fol.yaw = me.yaw;
  if (room) { const m = 0.35 + (fol.size ? Math.min(2, Math.max(fol.size.l, fol.size.w) * 0.3) : 0); fol.x = Math.max(m, Math.min(room.W - m, fol.x)); fol.z = Math.max(m, Math.min(room.D - m, fol.z)); }
  fol.y = room ? room.groundAt(fol.x, fol.z) : 0;
  trail.length = 0; trail.push({ x: fol.x, z: fol.z }, { x: me.x, z: me.z });
}
function nameOf(sid) { const d = DATA.species[sid]; return d ? title(d.ours.info.name || sid) : sid; }
function title(s) { return String(s).toLowerCase().replace(/(^|[\s-])\w/g, c => c.toUpperCase()); }

// ---------- you ----------
const me = { x: 4, z: 9, y: 0, yaw: Math.PI, fig: makeFigure(null), walk: 0 };
actors.add(me.fig.root);

// ---------- places ----------
const built = {};
let room = null;
function getRoom(id) { if (!built[id]) { const r = BUILD[id](); r.finish(); prepareRoom(r); built[id] = r; } return built[id]; }
function prepareRoom(r) {
  r.npcFigs = r.npcs.map(n => {
    const f = makeFigure(n.look); f.root.position.set(n.x, 0, n.z); f.root.rotation.y = n.yaw; r.group.add(f.root);
    poseFigure(f, { seated: n.seated });
    r.solid(n.x - 0.3, n.z - 0.3, n.x + 0.3, n.z + 0.3);
    r.act([n.x - 0.45, n.z - 0.45, n.x + 0.45, n.z + 0.45], { label: "Talk", npc: n });
    n.fig = f; return f;
  });
  if (r.exitArrow) {
    const a = new THREE.Mesh(new THREE.PlaneGeometry(0.7, 0.7), new THREE.MeshBasicMaterial({ map: pixTex("exitArrow"), transparent: true, depthWrite: false }));
    a.rotation.x = -Math.PI / 2; a.position.set(r.exitArrow.x, 0.06, r.exitArrow.z); a.renderOrder = 4; r.group.add(a); r.arrowMesh = a;
  }
  if (r.steam) {
    const dot = new THREE.CanvasTexture((() => { const P = new Pix(8, 8); for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) { const d = Math.hypot(x - 3.5, y - 3.5) / 4; P.set(x, y, [255, 255, 255], Math.max(0, 0.7 * (1 - d))); } return P.canvas(); })());
    r.puffs = [];
    for (let i = 0; i < 7; i++) { const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: dot, transparent: true, depthWrite: false })); s.scale.setScalar(0.25); r.group.add(s); r.puffs.push({ s, k: i / 7 }); }
  }
}

// ---------- the camera: always facing north, looking down; the whole room, or following you ----------
const PITCH = 55 * D2R;
const cam = { mode: "room", tx: 8, tz: 7, ty: 0, dist: 30, fit: null };
const scratch = new THREE.PerspectiveCamera(32, 1.6, 0.1, 400), ndc = new THREE.Vector3();
function placeCam(c, tx, ty, tz, d) { c.position.set(tx, ty + Math.sin(PITCH) * d, tz + Math.cos(PITCH) * d); c.lookAt(tx, ty, tz); c.updateMatrixWorld(true); }
function fitRoom(r) {
  const H = r.outside ? 5.5 : r.H, pts = [[0, 0, 0], [r.W, 0, 0], [0, 0, r.D], [r.W, 0, r.D], [0, H, 0], [r.W, H, 0], [0, r.outside ? 0 : H, r.D], [r.W, r.outside ? 0 : H, r.D]].map(p => V(...p));
  scratch.aspect = camera.aspect; scratch.updateProjectionMatrix();
  const fits = (tz, d) => { placeCam(scratch, r.W / 2, 0, tz, d); for (const p of pts) { ndc.copy(p).project(scratch); if (Math.abs(ndc.x) > 0.95 || ndc.y > 0.86 || ndc.y < -0.94 || ndc.z > 1) return false; } return true; };
  let best = null;
  for (let tz = r.D * 0.25; tz <= r.D * 0.85; tz += 0.25) {
    let lo = 3, hi = 200; for (let i = 0; i < 24; i++) { const mid = (lo + hi) / 2; if (fits(tz, mid)) hi = mid; else lo = mid; }
    if (!best || hi < best.d) best = { tz, d: hi };
  }
  return best;
}
function camGoal() {
  if (cam.mode === "room" && cam.fit) return { x: room.W / 2, y: 0, z: cam.fit.tz, d: cam.fit.d };
  const d = room.outside ? 15 : 11.5, m = room.outside ? 6 : 4;
  return { x: Math.max(Math.min(m, room.W / 2), Math.min(room.W - Math.min(m, room.W / 2), me.x)), y: me.y * 0.6, z: Math.max(2, Math.min(room.D - 2.5, me.z - 0.6)), d };
}

// ---------- going places ----------
let busy = false;
function switchRoom(id, spawn) {
  if (room) scene.remove(room.group);
  room = getRoom(id);
  scene.add(room.group);
  const s = spawn || room.spawn;
  me.x = s.x; me.z = s.z; me.yaw = s.yaw; me.y = room.groundAt(me.x, me.z);
  if (fol.model) placeFollowerBehind();
  // Light: from high in the south-west, so things cast their shadows back towards the walls.
  const W = room.W, D = room.D;
  sun.position.set(W * 0.3, room.outside ? 26 : 18, D + 9); sun.target.position.set(W / 2, 0, D / 2);
  const S = Math.max(W, D) * 0.85; Object.assign(sun.shadow.camera, { left: -S, right: S, top: S, bottom: -S, near: 1, far: 80 }); sun.shadow.camera.updateProjectionMatrix();
  sun.intensity = room.outside ? 0.78 : 0.6; hemi.intensity = room.outside ? 0.7 : 0.62;
  hemi.color.set(room.outside ? 0xe8f4ff : 0xfff6e8); hemi.groundColor.set(room.outside ? 0x6f8a5a : 0x8a7f74);
  cam.fit = fitRoom(room); snapCam();
  $("#roomPick").value = id;
  $("#placeName").textContent = room.name; $("#placeFloor").textContent = room.floor;
  closeTalk(); renderPanel();
}
function snapCam() { const g = camGoal(); cam.tx = g.x; cam.ty = g.y; cam.tz = g.z; cam.dist = g.d; placeCam(camera, cam.tx, cam.ty, cam.tz, cam.dist); }
const sleep = ms => new Promise(r => setTimeout(r, ms));
function tween(ms, fn) { return new Promise(res => { const t0 = performance.now(); const step = now => { const k = Math.min(1, (now - t0) / ms); fn(k); if (k < 1) requestAnimationFrame(step); else res(); }; requestAnimationFrame(step); }); }
const fadeEl = $("#fade"), irisEl = $("#iris");
async function fadeTo(a, ms) { const a0 = +fadeEl.style.opacity || 0; await tween(ms, k => { fadeEl.style.opacity = String(a0 + (a - a0) * k); }); }
// Going in: black closes in to a circle at your middle, as the newer games do it.
async function irisClose(ms) {
  const w = viewerEl.clientWidth, h = viewerEl.clientHeight, dpr = Math.min(2, window.devicePixelRatio || 1);
  irisEl.width = Math.round(w * dpr); irisEl.height = Math.round(h * dpr); irisEl.style.opacity = "1";
  const ctx = irisEl.getContext("2d"), p = V(me.x, me.y + 0.9, me.z).project(camera);
  const cx = (p.x + 1) / 2 * w, cy = (1 - p.y) / 2 * h, R0 = Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy)) + 4;
  await tween(ms, k => {
    const r = R0 * (1 - (k < 0.85 ? Math.pow(k / 0.85, 1.6) * 0.93 : 0.93 + (k - 0.85) / 0.15 * 0.07));
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.globalCompositeOperation = "source-over"; ctx.fillStyle = "#000"; ctx.fillRect(0, 0, w, h);
    ctx.globalCompositeOperation = "destination-out"; ctx.beginPath(); ctx.arc(cx, cy, Math.max(0, r), 0, Math.PI * 2); ctx.fill();
  });
  fadeEl.style.opacity = "1"; irisEl.style.opacity = "0";
}
async function goTo(id, spawn, fx) {
  if (busy) return; busy = true; closeTalk();
  if (fx === "iris") await irisClose(620); else await fadeTo(1, fx === "fade" ? 260 : 380);
  switchRoom(id, spawn);
  await sleep(fx === "fade" ? 90 : 160);
  await fadeTo(0, fx === "iris" ? 420 : 320);
  busy = false;
}

// ---------- the text box ----------
let talk = null;
const tb = $("#textbox"), tbText = $("#text"), tbWho = $("#who"), tbMore = $("#more"), chEl = $("#choices");
function say(steps) {
  return new Promise(res => { talk = { steps, i: 0, shown: 0, res, sel: 0, result: undefined }; showStep(); });
}
function showStep() {
  const st = talk.steps[talk.i];
  tb.hidden = false; tbWho.hidden = !st.who; tbWho.textContent = st.who || ""; talk.shown = 0; talk.t0 = performance.now(); tbText.textContent = ""; tbMore.hidden = true; chEl.hidden = true;
}
function renderChoices() {
  const st = talk.steps[talk.i]; chEl.innerHTML = "";
  st.ask.forEach((c, i) => { const b = document.createElement("button"); b.textContent = c; b.setAttribute("role", "option"); b.setAttribute("aria-selected", String(i === talk.sel));
    b.onpointerdown = e => { e.preventDefault(); talk.sel = i; renderChoices(); advance(); }; chEl.appendChild(b); });
  chEl.hidden = false;
}
function tickTalk(now) {
  if (!talk) return;
  const st = talk.steps[talk.i], full = st.text.length;
  if (talk.shown < full) { talk.shown = Math.min(full, Math.floor((now - talk.t0) / 1000 * 52)); tbText.textContent = st.text.slice(0, talk.shown); if (talk.shown >= full) { if (st.ask) renderChoices(); else tbMore.hidden = false; } }
}
function advance() {
  if (!talk) return;
  const st = talk.steps[talk.i];
  if (talk.shown < st.text.length) { talk.shown = st.text.length; tbText.textContent = st.text; if (st.ask) renderChoices(); else tbMore.hidden = false; return; }
  if (st.ask) talk.result = talk.sel;
  talk.i++; talk.sel = 0;
  if (talk.i >= talk.steps.length) { const r = talk.res, v = talk.result; closeTalk(); r(v); } else showStep();
}
function closeTalk() { if (talk && talk.res) { const r = talk.res; talk = null; r(undefined); } talk = null; tb.hidden = true; chEl.hidden = true; }

// What happens when you look at something, or talk to someone.
async function doAct(a) {
  busy = true;
  if (a.npc) {
    const n = a.npc, lines = n.talk(fol.size);
    n.talking = true;
    await say(lines.map(l => ({ who: n.name, text: l.text })));
    n.talking = false;
  } else if (a.follower) {
    const nm = nameOf(fol.sid), big = fol.size.h > 3.4, tiny = fol.size.h < 0.6;
    const line = big ? `${nm.toUpperCase()} is squeezed in, but it looks happy to be here.` : tiny ? `${nm.toUpperCase()} looks up at you and wiggles.` : `${nm.toUpperCase()} is happy to be following you around.`;
    await say([{ text: line }]);
  } else if (a.ask) {
    const yes = await say([{ text: "Have a rest?", ask: ["Yes", "No"] }]);
    if (yes === 0) { await fadeTo(1, 420); await sleep(380); await fadeTo(0, 420); await say([{ text: "You had a good rest. Your team is full of energy!" }]); }
  } else if (a.pc) {
    const pick = await say([{ text: "You booted up the PC.", ask: ["Item storage", "Log off"] }]);
    if (pick === 0) await say([{ text: "Item storage: one POTION, kept for you." }, { text: "In game, your boxes and stored items open from here." }]);
  } else {
    if (a.hit && room.bag) room.bag.swing = 1;
    await say((a.lines || ["Nothing special."]).map(t => ({ text: t })));
  }
  busy = false;
}

// ---------- input ----------
const keys = { up: false, down: false, left: false, right: false, run: false };
let actQueued = false, viewerActive = false;
const KEYMAP = { KeyW: "up", ArrowUp: "up", KeyS: "down", ArrowDown: "down", KeyA: "left", ArrowLeft: "left", KeyD: "right", ArrowRight: "right", ShiftLeft: "run", ShiftRight: "run" };
const ACTKEYS = new Set(["KeyE", "Space", "Enter", "NumpadEnter", "KeyZ"]);
function typing(e) { const t = e.target; return t && (t.tagName === "SELECT" || t.tagName === "INPUT" || t.tagName === "TEXTAREA" || t.isContentEditable); }
window.addEventListener("keydown", e => {
  if (typing(e) || e.metaKey || e.ctrlKey || e.altKey) return;
  const k = KEYMAP[e.code];
  if (talk && talk.steps[talk.i].ask && talk.shown >= talk.steps[talk.i].text.length && (k === "up" || k === "down")) {
    const n = talk.steps[talk.i].ask.length; talk.sel = (talk.sel + (k === "down" ? 1 : n - 1)) % n; renderChoices(); e.preventDefault(); return;
  }
  if (k) { keys[k] = true; if (viewerActive || k !== "run") if (e.code.startsWith("Arrow")) e.preventDefault(); }
  if (ACTKEYS.has(e.code)) { if (!e.repeat) actQueued = true; if (viewerActive || e.code !== "Enter") e.preventDefault(); }
});
window.addEventListener("keyup", e => { const k = KEYMAP[e.code]; if (k) keys[k] = false; });
window.addEventListener("blur", () => { for (const k in keys) keys[k] = false; });
viewerEl.addEventListener("pointerdown", () => { viewerActive = true; viewerEl.focus({ preventScroll: true }); });
document.addEventListener("pointerdown", e => { if (!viewerEl.contains(e.target)) viewerActive = false; });
viewerEl.addEventListener("click", () => { if (talk) advance(); });
// The touch pad.
if (matchMedia("(pointer: coarse)").matches) viewerEl.classList.add("touch");
for (const b of viewerEl.querySelectorAll("[data-k]")) {
  const k = b.dataset.k;
  b.addEventListener("pointerdown", e => { e.preventDefault(); e.stopPropagation(); b.setPointerCapture(e.pointerId); if (k === "act") actQueued = true; else if (k === "run") keys.run = !keys.run, b.setAttribute("aria-pressed", String(keys.run)); else keys[k] = true; });
  const up = () => { if (k !== "act" && k !== "run") keys[k] = false; };
  b.addEventListener("pointerup", up); b.addEventListener("pointercancel", up); b.addEventListener("lostpointercapture", up);
  b.addEventListener("click", e => e.stopPropagation());
}

// ---------- moving about ----------
const R_ME = 0.28;
function blocked(x, z) { for (const s of room.solids) if (x + R_ME > s[0] && x - R_ME < s[2] && z + R_ME > s[1] && z - R_ME < s[3]) return true; return false; }
function moveMe(dx, dz) {
  // One axis at a time, so you slide along walls and furniture; a step too high (the side of the stairs) stops you.
  const tryMove = (nx, nz) => {
    if (blocked(nx, nz)) return false;
    const g = room.groundAt(nx, nz); if (Math.abs(g - me.y) > 0.8) return false;
    me.x = nx; me.z = nz; return true;
  };
  const a = tryMove(me.x + dx, me.z), b = tryMove(me.x, me.z + dz);
  return a || b;
}
function probe() {
  const px = me.x - Math.sin(me.yaw) * 0.78, pz = me.z - Math.cos(me.yaw) * 0.78;
  if (fol.model && fol.ride === "walk") { const r = Math.max(0.45, Math.max(fol.size.l, fol.size.w) * 0.5); if (Math.hypot(px - fol.x, pz - fol.z) < r + 0.2) return { label: "Talk", follower: true, at: [fol.x, fol.y + fol.size.h + 0.3, fol.z] }; }
  let best = null, bd = 1e9;
  for (const a of room.acts) {
    const [x0, z0, x1, z1] = a.rect, e = 0.18;
    if (px > x0 - e && px < x1 + e && pz > z0 - e && pz < z1 + e) { const d = Math.hypot((x0 + x1) / 2 - px, (z0 + z1) / 2 - pz); if (d < bd) { bd = d; best = a; } }
  }
  return best;
}
function updateMe(dt) {
  let dx = (keys.right ? 1 : 0) - (keys.left ? 1 : 0), dz = (keys.down ? 1 : 0) - (keys.up ? 1 : 0);
  const moving = !busy && !talk && (dx || dz);
  let moved = 0;
  if (moving) {
    const L = Math.hypot(dx, dz); dx /= L; dz /= L;
    const want = Math.atan2(-dx, -dz);
    let dy = want - me.yaw; dy = Math.atan2(Math.sin(dy), Math.cos(dy)); me.yaw += dy * Math.min(1, dt * 16);
    const sp = keys.run ? 5.6 : 4.3, ox = me.x, oz = me.z;
    moveMe(dx * sp * dt, dz * sp * dt);
    moved = Math.hypot(me.x - ox, me.z - oz);
  }
  const g = room.groundAt(me.x, me.z); me.y += (g - me.y) * Math.min(1, dt * 16);
  const sp = dt > 0 ? moved / dt : 0;
  me.walk += ((sp > 0.3 ? Math.min(1.25, sp / 4.3) : 0) - me.walk) * Math.min(1, dt * 10);
  me.fig.phase += moved * 2.6;
  me.fig.root.position.set(me.x, me.y, me.z); me.fig.root.rotation.y = me.yaw;
  me.fig.shadow.position.y = 0.025 + (g - me.y);
  poseFigure(me.fig, { walk: me.walk });
  if (moved > 0) { const last = trail[trail.length - 1]; if (!last || Math.hypot(last.x - me.x, last.z - me.z) > 0.06) { trail.push({ x: me.x, z: me.z }); if (trail.length > 800) trail.shift(); } }
  // Warps: stairs, the door mat, the doors outside.
  if (!busy && !talk) {
    for (const w of room.warps) if (w.test(me, keys)) { goTo(w.to, w.spawn, w.fx); return; }
    for (const d of room.doors) {
      const near = Math.abs(me.x - d.x) < 1.5 && me.z > d.z && me.z < d.z + 2.4;
      d.open += ((near ? 1 : 0) - d.open) * Math.min(1, dt * 7); d.grp.rotation.y = -d.open * 1.45;
      if (keys.up && me.x > d.x0 + 0.1 && me.x < d.x1 - 0.1 && me.z < d.z + 0.5 && d.open > 0.55) { goTo(d.to, d.spawn, "iris"); return; }
    }
  }
  // What's in front of you.
  const a = (!busy && !talk) ? probe() : null;
  const pr = $("#prompt");
  if (a) {
    let at = a.at;
    if (!at) { const [x0, z0, x1, z1] = a.rect, cx = (x0 + x1) / 2, cz = (z0 + z1) / 2; at = [cx, a.npc ? 2.1 : Math.min(2.2, room.groundAt(cx, cz) + 1.6), cz]; }
    const p = V(...at).project(camera);
    pr.hidden = false; pr.style.left = ((p.x + 1) / 2 * 100) + "%"; pr.style.top = ((1 - p.y) / 2 * 100) + "%"; $("#promptText").textContent = a.label || "Look";
  } else pr.hidden = true;
  if (actQueued) { actQueued = false; if (talk) advance(); else if (a && !busy) doAct(a); }
}
function updateFollower(dt, now) {
  const m = fol.model; if (!m) return;
  const sz = fol.size;
  if (fol.ride === "shoulder" && canRide()) {
    // On your left shoulder, facing the way you face.
    const lx = -0.36 - sz.cx * 0, ly = 1.38 + (me.y), lz = 0.02;
    const c = Math.cos(me.yaw), s = Math.sin(me.yaw);
    const ox = -sz.cx, oz = -sz.cz;
    m.holder.position.set(me.x + lx * c + lz * s + (ox * c + oz * s), ly - sz.bottom, me.z - lx * s + lz * c + (-ox * s + oz * c));
    m.holder.rotation.y = me.yaw;
    fol.x = me.x; fol.z = me.z; fol.y = me.y;
    setParts(m, null); pose(m, [[groundIdle(m), (now / 1000)]]);
    fol.shadow.visible = false;
    return;
  }
  // Walking behind you, along the way you came, a little more than its own length back.
  const gap = gapFor();
  let tx = fol.x, tz = fol.z, acc = 0, px = me.x, pz = me.z, found = false;
  for (let i = trail.length - 1; i >= 0; i--) {
    const q = trail[i], d = Math.hypot(q.x - px, q.z - pz);
    if (acc + d >= gap) { const k = (gap - acc) / Math.max(1e-6, d); tx = px + (q.x - px) * k; tz = pz + (q.z - pz) * k; found = true; break; }
    acc += d; px = q.x; pz = q.z;
  }
  if (!found) { tx = fol.x; tz = fol.z; }
  const dx = tx - fol.x, dz = tz - fol.z, dist = Math.hypot(dx, dz);
  const maxStep = 6.5 * dt, step = Math.min(dist, maxStep * (dist > 1.5 ? 1.6 : 1));
  if (dist > 1e-4) { fol.x += dx / dist * step; fol.z += dz / dist * step; }
  const spd = dt > 0 ? step / dt : 0; fol.spd += (spd - fol.spd) * Math.min(1, dt * 8);
  if (dist > 0.02) { const want = Math.atan2(-dx, -dz); let dy = want - fol.yaw; dy = Math.atan2(Math.sin(dy), Math.cos(dy)); fol.yaw += dy * Math.min(1, dt * 9); }
  else { const want = Math.atan2(-(me.x - fol.x), -(me.z - fol.z)); let dy = want - fol.yaw; dy = Math.atan2(Math.sin(dy), Math.cos(dy)); fol.yaw += dy * Math.min(1, dt * 2.5); }
  const g = room.groundAt(fol.x, fol.z); fol.y += (g - fol.y) * Math.min(1, dt * 12);
  const c = Math.cos(fol.yaw), s = Math.sin(fol.yaw), ox = -sz.cx, oz = -sz.cz;
  m.holder.position.set(fol.x + (ox * c + oz * s), fol.y - sz.bottom * 0, fol.z + (-ox * s + oz * c));
  m.holder.rotation.y = fol.yaw;
  const mv = fol.spd > 0.4 ? moveAnim(m) : null;
  setParts(m, mv ? mv.key : null);
  fol.t += dt * (mv ? Math.max(0.6, Math.min(1.8, fol.spd / 3.6)) : 1);
  pose(m, [[mv ? mv.an : groundIdle(m), fol.t]]);
  fol.shadow.visible = true; fol.shadow.position.set(fol.x, fol.y + 0.03, fol.z);
  const sw = Math.max(0.6, Math.min(6, Math.max(sz.w, sz.l) * 0.9)); fol.shadow.scale.set(sw, sw, 1);
}

// ---------- each frame ----------
const tagsEl = $("#tags");
function updateTags() {
  const want = room.npcs.map(n => ({ text: n.name, at: V(n.x, (n.seated ? 1.95 : 2.2), n.z) }));
  while (tagsEl.children.length < want.length) { const t = document.createElement("div"); t.className = "tag"; tagsEl.appendChild(t); }
  [...tagsEl.children].forEach((el, i) => {
    const w = want[i]; if (!w) { el.hidden = true; return; }
    const p = w.at.project(camera); el.hidden = false; el.textContent = w.text; el.style.left = ((p.x + 1) / 2 * 100) + "%"; el.style.top = ((1 - p.y) / 2 * 100) + "%";
  });
}
function resize() {
  const w = viewerEl.clientWidth, h = viewerEl.clientHeight; if (!w || !h) return;
  renderer.setSize(w, h, false); camera.aspect = w / h; camera.updateProjectionMatrix();
  if (room) { cam.fit = fitRoom(room); }
}
new ResizeObserver(resize).observe(viewerEl);
let last = performance.now();
function tickFrame(now) {
  const dt = Math.min(0.05, (now - last) / 1000); last = now;
  if (room) {
    updateMe(dt); updateFollower(dt, now); tickTalk(now);
    for (const s of room.screens) s.tick(now);
    if (room.arrowMesh) { room.arrowMesh.material.opacity = 0.35 + 0.35 * (0.5 + 0.5 * Math.sin(now / 300)); room.arrowMesh.position.y = 0.06; }
    if (room.puffs) for (const p of room.puffs) { const k = ((now / 2600) + p.k) % 1; p.s.position.set(room.steam.x + Math.sin(k * 7 + p.k * 9) * 0.08, room.steam.y + k * 1.1, room.steam.z); p.s.material.opacity = Math.sin(k * Math.PI) * 0.55; p.s.scale.setScalar(0.18 + k * 0.32); }
    if (room.bag) { room.bag.t += dt; room.bag.swing *= Math.exp(-dt * 1.4); room.bag.grp.rotation.x = -Math.sin(room.bag.t * 5.2) * 0.42 * room.bag.swing; }
    for (const n of room.npcs) { if (!n.fig) continue; const want = n.talking ? Math.atan2(-(me.x - n.x), -(me.z - n.z)) - n.yaw : 0; let d = Math.atan2(Math.sin(want), Math.cos(want)); d = Math.max(-1.1, Math.min(1.1, d)); n.headYaw = (n.headYaw || 0) + (d - (n.headYaw || 0)) * Math.min(1, dt * 6); poseFigure(n.fig, { seated: n.seated, headYaw: n.headYaw, talk: n.talking }); }
    const g = camGoal(), k = Math.min(1, dt * 5);
    cam.tx += (g.x - cam.tx) * k; cam.ty += (g.y - cam.ty) * k; cam.tz += (g.z - cam.tz) * k; cam.dist += (g.d - cam.dist) * k;
    placeCam(camera, cam.tx, cam.ty, cam.tz, cam.dist);
    updateTags();
  }
  renderer.render(scene, camera);
  requestAnimationFrame(tickFrame);
}

// ---------- pickers and the side panel ----------
function setSeg(el, v) { for (const b of el.querySelectorAll("button")) b.setAttribute("aria-pressed", String(b.dataset.v === v)); }
function renderRide() {
  const seg = $("#rideSeg"), sh = seg.querySelector('[data-v="shoulder"]');
  sh.disabled = !canRide(); setSeg(seg, fol.ride);
  sh.title = fol.size ? (canRide() ? "It's " + fol.size.h.toFixed(2) + " blocks tall: small enough to ride" : "Too big to ride: " + fol.size.h.toFixed(2) + " blocks tall (1 block or less can)") : "For creatures 1 block tall or less";
}
function setRide(v) { fol.ride = v; if (v === "walk" && fol.model) placeFollowerBehind(); renderRide(); renderPanel(); }
function renderPickers() {
  const rp = $("#roomPick"); rp.innerHTML = "";
  let og = null, gname = null;
  for (const p of PLACES) { if (p.group !== gname) { og = document.createElement("optgroup"); og.label = p.group; rp.appendChild(og); gname = p.group; } const o = document.createElement("option"); o.value = p.id; o.textContent = `${p.name} (${p.floor.toLowerCase()})`; og.appendChild(o); }
  rp.onchange = () => { rp.blur(); viewerEl.focus({ preventScroll: true }); viewerActive = true; goTo(rp.value, null, "fade"); };
  const fp = $("#followPick"); fp.innerHTML = "";
  const none = document.createElement("option"); none.value = ""; none.textContent = "No follower"; fp.appendChild(none);
  for (const f of DATA.families) {
    const g = document.createElement("optgroup"); g.label = f.name;
    for (const sid of f.members) { const d = DATA.species[sid]; if (!d) continue; const o = document.createElement("option"); o.value = sid; o.textContent = `${nameOf(sid)} (#${String(d.dex).padStart(3, "0")} ${d.pname})`; g.appendChild(o); }
    fp.appendChild(g);
  }
  fp.value = fol.want || "";
  fp.onchange = () => { fp.blur(); viewerEl.focus({ preventScroll: true }); viewerActive = true; setFollower(fp.value); };
  for (const b of $("#rideSeg").querySelectorAll("button")) b.onclick = () => { if (!b.disabled) setRide(b.dataset.v); };
  for (const b of $("#lookSeg").querySelectorAll("button")) b.onclick = () => { fol.look = b.dataset.v; setSeg($("#lookSeg"), fol.look); if (fol.want) setFollower(fol.want); };
  for (const b of $("#camSeg").querySelectorAll("button")) b.onclick = () => { cam.mode = b.dataset.v; setSeg($("#camSeg"), cam.mode); };
}
function esc(s) { return String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c])); }
function renderPanel() {
  if (!room) return;
  const p = $("#panel"), d = fol.sid && DATA.species[fol.sid];
  const size = room.outside ? `${room.W} by ${room.D} blocks` : `${room.W} by ${room.D} blocks`;
  const facts = [["Floor", size], room.outside ? ["Houses", "8 by 6 outside"] : ["Walls", `${room.H} blocks high`]];
  if (room.outsideSize) facts.push(["From outside", room.outsideSize + " blocks"]);
  facts.push(["Camera", cam.mode === "room" ? "Facing north, whole room" : "Facing north, following"]);
  let follower = `<p class="note">No follower. Pick any creature above.</p>`;
  if (d && fol.size) {
    const h = fol.size.h, ride = canRide();
    follower = `<div class="facts">
      <div class="fact"><small>Following you</small><b>${esc(nameOf(fol.sid))}</b></div>
      <div class="fact"><small>Stands in for</small><b>#${String(d.dex).padStart(3, "0")} ${esc(d.pname)}</b></div>
      <div class="fact"><small>Height</small><b>${h.toFixed(2)} blocks</b></div>
      <div class="fact"><small>Length</small><b>${Math.max(fol.size.l, fol.size.w).toFixed(2)} blocks</b></div></div>
      <p class="note">${ride ? "Small enough to ride on your shoulder (1 block tall or less)." : "Too big for your shoulder, so it walks behind you."}${h > room.H && !room.outside ? " Taller than the walls: it pokes out over the top." : ""}</p>`;
  }
  p.innerHTML = `
    <div class="title"><span class="big">${esc(room.name)}</span><span class="sub">${esc(room.floor)}${room.outside ? "" : ", seen from the south with the near wall left off"}</span></div>
    <div class="facts">${facts.map(([a, b]) => `<div class="fact"><small>${esc(a)}</small><b>${esc(b)}</b></div>`).join("")}</div>
    <div><h2>Follower</h2>${follower}</div>
    <div><h2>Made for this room</h2><ul class="list">${room.items.map(i => `<li><span><b>${esc(i.name)}.</b> <span>${esc(i.note)}</span></span></li>`).join("")}</ul></div>
    <div><h2>Controls</h2><div class="ctrls">
      <kbd>W A S D</kbd><span>Walk (or the arrow keys)</span>
      <kbd>Shift</kbd><span>Run</span>
      <kbd>E</kbd><span>Look, talk, or go on (or Space, Enter)</span>
      <kbd>Stairs</kbd><span>Walk up or down them to change floor</span>
      <kbd>Door mat</kbd><span>Walk off it, south, to go out: a fade to black</span>
      <kbd>Doors</kbd><span>Outside, walk into one: it opens, then the circle closes in</span>
    </div></div>`;
}

// ---------- start ----------
renderPickers();
resize();
switchRoom("house2");
setFollower(fol.want);
requestAnimationFrame(tickFrame);
