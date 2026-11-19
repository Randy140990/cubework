// The figure's soft shadow (the only material it borrows from the interiors).
let shadowMat = null;
function matFor(k) {
  if (shadowMat) return shadowMat;
  const cv = document.createElement("canvas"); cv.width = cv.height = 64; const x = cv.getContext("2d"), g = x.createRadialGradient(32, 32, 2, 32, 32, 31);
  g.addColorStop(0, "rgba(0,0,0,.38)"); g.addColorStop(1, "rgba(0,0,0,0)"); x.fillStyle = g; x.fillRect(0, 0, 64, 64);
  return shadowMat = new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(cv), transparent: true, depthWrite: false });
}
// ---------- the stage ----------
const viewerEl = $("#viewer"), canvas = $("#c");
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1)); renderer.setClearColor(0, 0);
renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(28, 16 / 10, 0.1, 100);
scene.add(new THREE.HemisphereLight(0xffffff, 0x9ab0a8, 0.78));
const key = new THREE.DirectionalLight(0xffffff, 0.62); key.position.set(-3, 6, 5); key.castShadow = true; key.shadow.mapSize.set(1024, 1024);
Object.assign(key.shadow.camera, { left: -3, right: 3, top: 3, bottom: -3 }); key.shadow.bias = -0.0008; scene.add(key);
const rim = new THREE.DirectionalLight(0xcff5e6, 0.25); rim.position.set(4, 3, -4); scene.add(rim);
// A low eight-sided plinth, as a Minecraft build would round it.
const plinthMat = c => new THREE.MeshLambertMaterial({ color: c });
const plinth = new THREE.Group();
{ const a = new THREE.Mesh(new THREE.CylinderGeometry(1.0, 1.06, 0.14, 8), plinthMat(0xF4F6F3)); a.position.y = -0.07; a.rotation.y = Math.PI / 8; a.receiveShadow = true; plinth.add(a);
  const b = new THREE.Mesh(new THREE.CylinderGeometry(1.08, 1.08, 0.04, 8), plinthMat(0x4FB896)); b.position.y = -0.16; b.rotation.y = Math.PI / 8; plinth.add(b); }
scene.add(plinth);
const holder = new THREE.Group(); scene.add(holder);
// The size check: you beside them, and one block.
const extras = new THREE.Group(); extras.visible = false; scene.add(extras);
{ const cv = document.createElement("canvas"); cv.width = cv.height = 16; const x = cv.getContext("2d");
  for (let y = 0; y < 16; y++) for (let i = 0; i < 16; i++) { const g = y < 3 + ((i * 7) % 3 === 0 ? 1 : 0); const n = ((i * 13 + y * 7) % 5) * 4; x.fillStyle = g ? `rgb(${96 + n},${168 + n},${72 + n})` : `rgb(${134 + n},${96 + n},${66 + n})`; x.fillRect(i, y, 1, 1); }
  const side = new THREE.CanvasTexture(cv); side.magFilter = THREE.NearestFilter; side.minFilter = THREE.NearestFilter;
  const tcv = document.createElement("canvas"); tcv.width = tcv.height = 16; const tx = tcv.getContext("2d");
  for (let y = 0; y < 16; y++) for (let i = 0; i < 16; i++) { const n = ((i * 11 + y * 5) % 6) * 4; tx.fillStyle = `rgb(${100 + n},${172 + n},${76 + n})`; tx.fillRect(i, y, 1, 1); }
  const top = new THREE.CanvasTexture(tcv); top.magFilter = THREE.NearestFilter; top.minFilter = THREE.NearestFilter;
  const sm = new THREE.MeshLambertMaterial({ map: side }), tm = new THREE.MeshLambertMaterial({ map: top });
  const blk = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), [sm, sm, tm, sm, sm, sm]); blk.position.set(1.45, 0.5, 0.1); blk.castShadow = blk.receiveShadow = true; extras.add(blk); }
let youFig = null;

// ---------- who's who ----------
const ALL = PEOPLE.map((p, i) => ({ id: "P-" + String(i + 1).padStart(3, "0"), group: p[0], kind: p[1], spec: p[2], name: p[3], who: p[4], where: p[5], team: p[6] }));
const GROUP_NAME = Object.fromEntries(GROUPS.map(g => [g[0], g[1]]));
const specOf = s => typeof s === "string" ? { skin: s } : { outfit: OUTFIT.defaults(s.outfit === "girl") };
const skinFile = s => typeof s === "string" ? "textures/entity/npc/" + s + ".png" : "Made from your clothes (Outfit), as the mod makes it";
let filter = { q: "", group: "all", kind: "all" }, list = ALL.slice(), cur = 0, fig = null;
const PITCH0 = 0.19;   // the usual eye line: a little above them
const st = { pose: "stand", view: "turn", yaw: Math.PI - 0.5, pitch: PITCH0, pitchGoal: PITCH0, drag: null, auto: true, phase: 0 };

function visible() {
  const q = filter.q.trim().toLowerCase();
  return ALL.filter(p => (filter.group === "all" || p.group === filter.group) && (filter.kind === "all" || p.kind === filter.kind)
    && (!q || p.name.toLowerCase().includes(q) || p.id.toLowerCase().includes(q) || (typeof p.spec === "string" && p.spec.includes(q)) || p.where.toLowerCase().includes(q)));
}
function show(i) {
  if (!list.length) { $("#panel").innerHTML = `<p class="note">Nobody matches. Clear the search to see everyone.</p>`; if (fig) holder.remove(fig.root); fig = null; renderList(); return; }
  cur = (i + list.length) % list.length;
  const p = list[cur];
  if (fig) holder.remove(fig.root);
  fig = makeFigure(specOf(p.spec)); holder.add(fig.root);
  fig.root.traverse(o => { if (o.isMesh) o.castShadow = true; });
  $("#badge").innerHTML = `<b>${p.id}</b>${esc(p.name)}`;
  $("#count").textContent = `${cur + 1} of ${list.length}${list.length < ALL.length ? " (" + ALL.length + " in all)" : ""}`;
  renderPanel(p); renderList();
  try { history.replaceState(null, "", "#" + p.id); } catch (e) {}
}
const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
const title = s => String(s).replace(/_/g, " ").toLowerCase().replace(/(^|\s)\w/g, c => c.toUpperCase());
function renderPanel(p) {
  const team = p.team.map(sid => { const n = NAMES[sid]; return n ? `<span>${esc(n[0])} <small>for ${esc(n[1])}</small></span>` : `<span>${esc(title(sid))} <small>(not built yet)</small></span>`; }).join("");
  $("#panel").innerHTML = `
    <div class="chips"><span class="chip">${p.id}</span><span class="chip ${p.kind === "U" ? "u" : ""}">${p.kind === "U" ? "Unique" : "Generic"}</span><span class="chip">${esc(GROUP_NAME[p.group])}</span></div>
    <h2>${esc(p.name)}</h2>
    <div>${esc(p.who)}</div>
    <div><div class="label">Where you meet them</div><div style="font-weight:700">${esc(p.where)}</div></div>
    ${p.team.length ? `<div><div class="label">Their team (from the old games)</div><div class="team" style="margin-top:6px">${team}</div></div>` : ""}
    <div><div class="label">Skin</div><div class="skinwrap" style="margin-top:6px"><canvas class="skin" id="skinCv" width="64" height="64" aria-label="The skin, flat"></canvas>
      <div class="note">${esc(skinFile(p.spec))}<br>64 by 64, the standard Minecraft player layout (classic 4-pixel arms). ${p.kind === "U" ? "One of a kind." : "Used for everyone of this kind."}</div></div></div>
    <div class="note">To give feedback, quote <b>${p.id}</b>.</div>`;
  const cv = $("#skinCv"), x = cv.getContext("2d"); x.imageSmoothingEnabled = false; x.clearRect(0, 0, 64, 64);
  const src = typeof p.spec === "string" ? SKIN_IMG["npc/" + p.spec] : composeOutfit(OUTFIT.defaults(p.spec.outfit === "girl"));
  if (src) x.drawImage(src, 0, 0);
}
// A face for each person in the list: the front of their head, from their own skin (the hat layer over it).
const faceCache = {};
function faceOf(p) {
  const k = typeof p.spec === "string" ? p.spec : "you-" + p.spec.outfit;
  if (faceCache[k]) return faceCache[k];
  const src = typeof p.spec === "string" ? SKIN_IMG["npc/" + p.spec] : composeOutfit(OUTFIT.defaults(p.spec.outfit === "girl"));
  const cv = document.createElement("canvas"); cv.width = cv.height = 8; const x = cv.getContext("2d");
  if (src) { x.drawImage(src, 8, 8, 8, 8, 0, 0, 8, 8); x.drawImage(src, 40, 8, 8, 8, 0, 0, 8, 8); }
  return faceCache[k] = cv;
}
function renderList() {
  const el = $("#list"); el.innerHTML = "";
  const cp = list[cur];
  for (const [gid, gname, gnote] of GROUPS) {
    const ppl = list.filter(p => p.group === gid); if (!ppl.length) continue;
    const u = ppl.filter(p => p.kind === "U").length;
    const h = document.createElement("h3"); h.innerHTML = `${esc(gname)} <small>${ppl.length} ${ppl.length === 1 ? "person" : "people"}${u ? `, ${u} unique` : ""}. ${esc(gnote)}</small>`; el.appendChild(h);
    const g = document.createElement("div"); g.className = "grid"; el.appendChild(g);
    for (const p of ppl) {
      const b = document.createElement("button"); b.className = "person"; b.type = "button"; b.setAttribute("aria-current", String(p === cp));
      const fc = document.createElement("canvas"); fc.width = fc.height = 8; fc.getContext("2d").drawImage(faceOf(p), 0, 0);
      b.appendChild(fc);
      b.insertAdjacentHTML("beforeend", `<span class="n">${p.kind === "U" ? '<i class="u" aria-label="Unique"></i>' : ""}${esc(p.name)}</span><span class="i">${p.id}</span>`);
      b.onclick = () => { show(list.indexOf(p)); viewerEl.scrollIntoView({ behavior: "smooth", block: "nearest" }); };
      g.appendChild(b);
    }
  }
}
function refilter(keep) {
  const was = list[cur]; list = visible();
  const i = keep && was ? list.indexOf(was) : -1;
  show(i >= 0 ? i : 0);
}

// ---------- controls ----------
function setSeg(seg, v) { for (const b of seg.querySelectorAll("button")) b.setAttribute("aria-pressed", String(b.dataset.v === v)); }
$("#prev").onclick = () => show(cur - 1); $("#next").onclick = () => show(cur + 1);
$("#find").addEventListener("input", e => { filter.q = e.target.value; refilter(true); });
{ const g = $("#group"); g.innerHTML = `<option value="all">All groups</option>` + GROUPS.map(([id, n]) => `<option value="${id}">${esc(n)}</option>`).join("");
  g.onchange = () => { filter.group = g.value; refilter(true); }; }
for (const b of $("#kindSeg").querySelectorAll("button")) b.onclick = () => { filter.kind = b.dataset.v; setSeg($("#kindSeg"), filter.kind); refilter(true); };
for (const b of $("#poseSeg").querySelectorAll("button")) b.onclick = () => { st.pose = b.dataset.v; setSeg($("#poseSeg"), st.pose); };
// People face -z at no turn, so their front is half a turn round to the camera.
const VIEWS = { front: Math.PI, side: Math.PI / 2, back: 0, above: Math.PI - 0.6, top: Math.PI };
// How far up the camera looks from: level views keep the usual eye line, From above is three-quarters down, Top is straight down on their head.
const PITCHES = { front: PITCH0, side: PITCH0, back: PITCH0, above: 0.85, top: 1.5 };
for (const b of $("#viewSeg").querySelectorAll("button")) b.onclick = () => {
  st.view = b.dataset.v; setSeg($("#viewSeg"), st.view); st.auto = st.view === "turn";
  if (PITCHES[st.view] != null) st.pitchGoal = PITCHES[st.view]; else if (st.pitchGoal > 1.2) st.pitchGoal = PITCH0;
};
for (const b of $("#sizeSeg").querySelectorAll("button")) b.onclick = () => {
  setSeg($("#sizeSeg"), b.dataset.v); extras.visible = b.dataset.v === "on";
  if (extras.visible && !youFig) { youFig = makeFigure({ outfit: OUTFIT.defaults(false) }); youFig.root.position.set(-1.35, 0, 0.1); youFig.root.rotation.y = Math.PI - 0.35; youFig.root.traverse(o => { if (o.isMesh) o.castShadow = true; }); extras.add(youFig.root); }
};
viewerEl.addEventListener("pointerdown", e => { st.drag = { x: e.clientX, y: e.clientY, yaw: st.yaw, pitch: st.pitchGoal }; viewerEl.setPointerCapture(e.pointerId); viewerEl.style.cursor = "grabbing"; viewerEl.focus({ preventScroll: true }); });
viewerEl.addEventListener("pointermove", e => {
  if (!st.drag) return;
  st.yaw = st.drag.yaw + (e.clientX - st.drag.x) * 0.012;
  st.pitchGoal = st.pitch = Math.max(-0.35, Math.min(1.5, st.drag.pitch + (e.clientY - st.drag.y) * 0.008));
  st.auto = false; st.view = "free"; setSeg($("#viewSeg"), "");
});
const endDrag = () => { st.drag = null; viewerEl.style.cursor = "grab"; };
viewerEl.addEventListener("pointerup", endDrag); viewerEl.addEventListener("pointercancel", endDrag);
window.addEventListener("keydown", e => {
  const t = e.target; if (t && (t.tagName === "INPUT" || t.tagName === "SELECT" || t.tagName === "TEXTAREA")) return;
  if (e.key === "ArrowRight" || e.key === "d" || e.key === "D") { show(cur + 1); e.preventDefault(); }
  else if (e.key === "ArrowLeft" || e.key === "a" || e.key === "A") { show(cur - 1); e.preventDefault(); }
});
function resize() { const w = viewerEl.clientWidth, h = viewerEl.clientHeight; if (!w || !h) return; renderer.setSize(w, h, false); camera.aspect = w / h; camera.updateProjectionMatrix(); }
new ResizeObserver(resize).observe(viewerEl);

// ---------- each frame ----------
let last = performance.now();
function tick(now) {
  const dt = Math.min(0.05, (now - last) / 1000); last = now;
  if (st.auto) st.yaw += dt * 0.45;
  else if (VIEWS[st.view] != null) { let d = VIEWS[st.view] - st.yaw; d = Math.atan2(Math.sin(d), Math.cos(d)); st.yaw += d * Math.min(1, dt * 8); }
  holder.rotation.y = st.yaw;
  if (fig) {
    const walking = st.pose === "walk";
    if (walking) fig.phase += dt * 7.5;
    poseFigure(fig, { walk: walking ? 1 : 0, wave: st.pose === "wave" ? 1 : 0, talk: st.pose === "talk", cheer: st.pose === "cheer" ? 1 : 0, seated: st.pose === "sit", nod: 0 });
    if (st.pose === "sit") fig.root.position.y = -0.0; else fig.root.position.y = 0;
  }
  if (youFig) poseFigure(youFig, {});
  const w = viewerEl.clientWidth, h = viewerEl.clientHeight, wide = extras.visible;
  st.pitch += (st.pitchGoal - st.pitch) * Math.min(1, dt * 8);
  const dist = (wide ? 7.4 : 5.6) * (w / h < 1 ? 1.45 : 1) * (st.pitch > 1.2 ? 0.62 : 1), ty = wide ? 0.85 : 0.92 + Math.max(0, st.pitch - 0.3) * 0.5;
  camera.position.set(0, ty + Math.sin(st.pitch) * dist, Math.cos(st.pitch) * dist); camera.lookAt(0, ty, 0);
  renderer.render(scene, camera);
  requestAnimationFrame(tick);
}
skinsReady.then(() => {
  resize(); list = visible();
  let start = 0; try { const h = (location.hash || "").slice(1).toUpperCase(); const i = list.findIndex(p => p.id === h); if (i >= 0) start = i; } catch (e) {}
  show(start); requestAnimationFrame(tick);
});
window.addEventListener("hashchange", () => { const h = (location.hash || "").slice(1).toUpperCase(), i = list.findIndex(p => p.id === h); if (i >= 0 && i !== cur) show(i); });
