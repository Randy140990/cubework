// ---------- the game screen: built at 1280 by 720, scaled to whatever the stage is ----------
const stageEl = $("#stage"), uiRoot = $("#ui"), bgEl = $("#bg");
function fitUI() { const w = stageEl.clientWidth || 1280; uiRoot.style.transform = `scale(${w / 1280})`; }
new ResizeObserver(fitUI).observe(stageEl);
function uiScale() { return (stageEl.clientWidth || 1280) / 1280; }
function h(tag, cls, html, style) { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; if (style) Object.assign(e.style, style); return e; }
function box(x, y, w, hh, cls = "g-panel", html = "") { const e = h("div", cls, html); Object.assign(e.style, { left: x + "px", top: y + "px" }); if (w != null) e.style.width = w + "px"; if (hh != null) e.style.height = hh + "px"; e.style.position = "absolute"; uiRoot.appendChild(e); return e; }
function add(e) { uiRoot.appendChild(e); return e; }
function backdrop(css) { bgEl.style.background = css || "none"; }
const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
// **WORD** in game text is picked out in gold, as names and items are in the old games' text.
const rich = s => esc(s).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>");
function title(s) { return String(s).toLowerCase().replace(/(^|[\s-])\w/g, c => c.toUpperCase()); }

// ---------- what you've got: one save, carried from screen to screen ----------
const G = {
  lang: "English", name: "RED", rival: "BLUE", money: 3150, badges: 2, seen: 34, caught: 12, time: "12:34", started: "19/11/2026", id: "40217",
  outfit: Object.assign(OUTFIT.defaults(false), { capp: 0, topc: 1, hairc: 9 }),
  opts: { textSpeed: 1, music: 3, sound: 3, voice: 1, nights: 1, wild: 1, effects: 0, style: 0 },
  party: [], place: "PALLET TOWN",
  bag: {
    "ITEMS": [["POTION", 5, "Restores 20 HP to one POKéMON."], ["SUPER POTION", 2, "Restores 50 HP to one POKéMON."], ["ANTIDOTE", 3, "Cures a POKéMON of poison."], ["PARLYZ HEAL", 1, "Cures a POKéMON of paralysis."], ["AWAKENING", 1, "Wakes a sleeping POKéMON."], ["ESCAPE ROPE", 1, "Takes you back out of a cave in a flash."], ["REPEL", 4, "Keeps weak wild POKéMON away for 100 steps."]],
    "BALLS": [["POKé BALL", 12, "A ball for catching wild POKéMON."], ["GREAT BALL", 3, "A better ball, with a higher chance of a catch."]],
    "KEY ITEMS": [["TOWN MAP", 1, "A map of Kanto. Shows where you are."], ["BICYCLE", 1, "A folding bike, for getting about twice as fast."], ["OLD ROD", 1, "An old fishing rod. Fish in any water."]],
    "TM & HM": [["TM01 MEGA PUNCH", 1, "Teaches MEGA PUNCH."], ["TM28 DIG", 1, "Teaches DIG."], ["HM01 CUT", 1, "Teaches CUT. Clears small trees outside battle."]],
  },
};
const PARTY = [["pikachu", 24, ["THUNDERSHOCK", "QUICK ATTACK", "THUNDER WAVE", "GROWL"], 0.82, ""], ["ivysaur", 22, ["VINE WHIP", "LEECH SEED", "POISONPOWDER", "TACKLE"], 0.64, ""],
  ["pidgeotto", 21, ["GUST", "QUICK ATTACK", "SAND-ATTACK", "WHIRLWIND"], 1, ""], ["sandshrew", 18, ["SCRATCH", "SAND-ATTACK", "SLASH", "DEFENSE CURL"], 0.35, "PSN"],
  ["cubone", 17, ["BONE CLUB", "GROWL", "LEER", "HEADBUTT"], 1, ""], ["clefairy", 16, ["POUND", "SING", "DOUBLESLAP", "MINIMIZE"], 0, "FNT"]];
function sp(sid) { return DATA.species[sid]; }
// Which look the creatures wear: ours (Retromon) or theirs (Cobblemon), as the mod's Look toggle swaps them.
let LOOK = "ours";
try { if (localStorage.getItem("rm-ui-look") === "theirs") LOOK = "theirs"; } catch (e) {}
function lookEntry(sid) { return sp(sid)[LOOK] || sp(sid).ours; }
function ourName(sid) { const d = sp(sid); return d ? (LOOK === "theirs" ? d.pname.toUpperCase() : (d.ours.info.name || sid.toUpperCase())) : sid.toUpperCase(); }
function statsAt(sid, lv) {
  const b = sp(sid).stats.stats, o = {};
  for (const [k, v] of Object.entries(b)) o[k] = k === "HP" ? Math.floor((2 * v + 15) * lv / 100) + lv + 10 : Math.floor((2 * v + 15) * lv / 100) + 5;
  return o;
}
Object.defineProperty(G, "look", { get: () => ({ outfit: G.outfit }) });
const hex = n => "#" + (n >>> 0).toString(16).padStart(6, "0").slice(-6);
for (const [sid, lv, moves, hp, status] of PARTY) {
  const st = statsAt(sid, lv);
  G.party.push({ sid, lv, moves, maxhp: st.HP, hp: Math.round(st.HP * hp), status, stats: st, exp: 0.3 + (lv % 5) / 10, gender: sid === "clefairy" ? "F" : (lv % 2 ? "M" : "F") });
}
const MOVE = {}; for (const m of DATA.moves) MOVE[m.name] = m;
const TYPECOL = t => `var(--t-${String(t).toLowerCase()})`;
function typeChip(t) { return `<span class="g-type" style="background:${TYPECOL(t)}">${esc(t)}</span>`; }
const STATUS = { PSN: "#AB6AC8", BRN: "#F08436", PAR: "#D9B42C", SLP: "#8A93A6", FRZ: "#6FCFD0", FNT: "#B0475A" };
function statusChip(s) { return s ? `<span class="g-status" style="background:${STATUS[s] || "#666"}">${s}</span>` : ""; }
function hpCol(f) { return f > 0.5 ? "var(--hp-green)" : f > 0.2 ? "var(--hp-yellow)" : "var(--hp-red)"; }
function hpBar(f, w) { return `<div class="g-hp" style="width:${w}px"><i style="width:${Math.max(0, f) * 100}%;background:${hpCol(f)}"></i></div>`; }
function gender(g) { return g === "F" ? `<span style="color:#FF8FB1">&#9792;</span>` : g === "M" ? `<span style="color:#7FB8FF">&#9794;</span>` : ""; }
function darkerHex(c, k) { const v = mixc(hx(c), [0, 0, 0], k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
function lighterHex(c, k) { const v = mixc(hx(c), [255, 255, 255], k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
function money(n) { return "&#8381;" + n.toLocaleString("en-GB"); }

// ---------- little pictures of our creatures, for lists: each drawn once from its model, three-quarters on ----------
const PORTRAIT = {}, portraitWaits = {};
let pR = null, pScene = null, pCam = null;
function portrait(sid) {
  const key = LOOK + ":" + sid;
  if (PORTRAIT[key]) return Promise.resolve(PORTRAIT[key]);
  if (portraitWaits[key]) return portraitWaits[key];
  return portraitWaits[key] = loadSpecies(sid).then(() => {
    if (!pR) { const c = document.createElement("canvas"); c.width = c.height = 192; pR = new THREE.WebGLRenderer({ canvas: c, antialias: true, alpha: true, preserveDrawingBuffer: true }); pR.setClearColor(0, 0);
      pScene = new THREE.Scene(); pScene.add(new THREE.HemisphereLight(0xffffff, 0x8899aa, 0.85)); const l = new THREE.DirectionalLight(0xffffff, 0.6); l.position.set(-3, 5, 4); pScene.add(l); pCam = new THREE.PerspectiveCamera(30, 1, 0.01, 200); }
    const m = buildModel(lookEntry(sid)), sz = sizeOf(m);
    return new Promise(res => { let n = 0; const wait = () => { const im = m.mat.map && m.mat.map.image; if ((im && im.complete !== false && im.width) || n++ > 60) res(m); else setTimeout(wait, 50); }; wait(); });
  }).then(m => {
    pose(m, [[groundIdle(m), 0.3]]); m.holder.rotation.y = Math.PI * 0.82; m.holder.updateMatrixWorld(true);
    const box = new THREE.Box3().setFromObject(m.holder), c = box.getCenter(new THREE.Vector3()), size = box.getSize(new THREE.Vector3()), R = Math.max(size.x, size.y, size.z) * 0.62;
    pScene.add(m.holder); pCam.position.set(c.x + R * 0.35, c.y + R * 0.55, c.z + R / Math.tan(15 * D2R) * 0.92); pCam.lookAt(c); pR.render(pScene, pCam); pScene.remove(m.holder);
    return PORTRAIT[key] = pR.domElement.toDataURL();
  });
}
// An <img> that fills in when the picture's ready.
function pic(sid, size, extra = "") { const id = "p" + Math.random().toString(36).slice(2); portrait(sid).then(u => { const e = document.getElementById(id); if (e) e.src = u; }); return `<img id="${id}" alt="" style="width:${size}px;height:${size}px;image-rendering:auto;${extra}" src="${PORTRAIT[LOOK + ":" + sid] || "data:image/gif;base64,R0lGODlhAQABAAAAACw="}">`; }

// ---------- the title's wordmark: letters built of little blocks, as Minecraft's own title is ----------
const WM_GLYPHS = {
  R: ["11111.", "11..11", "11..11", "11111.", "11.11.", "11..11", "11..11"], E: ["111111", "11....", "11....", "11111.", "11....", "11....", "111111"],
  T: ["111111", "..11..", "..11..", "..11..", "..11..", "..11..", "..11.."], O: [".1111.", "11..11", "11..11", "11..11", "11..11", "11..11", ".1111."],
  M: ["11...11", "111.111", "1111111", "11.1.11", "11...11", "11...11", "11...11"], N: ["11...11", "111..11", "1111.11", "11.1111", "11..111", "11...11", "11...11"],
  P: ["11111.", "11..11", "11..11", "11111.", "11....", "11....", "11...."], K: ["11..11", "11.11.", "1111..", "111...", "1111..", "11.11.", "11..11"],
  "é": ["....11.", "...11..", "111111", "11....", "11111.", "11....", "111111"],
};
function wordmark(text, o = {}) {
  // Our name has its own drawn wordmark now (round letters, not blocks).
  if (text === "RETROMON" && typeof WORDMARK_OURS !== "undefined") return WORDMARK_OURS;
  const u = o.unit || 14, depth = o.depth || 4, gap = 1, rows = 7, top = o.top || [255, 238, 150], bot = o.bottom || [242, 140, 30];
  const side = o.side || [120, 58, 18], line = o.line || [36, 18, 8];
  const glyphs = [...text].map(c => WM_GLYPHS[c] || WM_GLYPHS[c.toUpperCase()] || ["."]);
  const cols = glyphs.reduce((a, g) => a + Math.max(...g.map(r => r.length)) + gap, -gap);
  const pad = u, W = cols * u + depth * 3 + pad * 2, H = rows * u + depth * 3 + pad * 2 + u;
  const cv = document.createElement("canvas"); cv.width = W; cv.height = H; const x = cv.getContext("2d");
  const cells = []; let cx = 0;
  for (const g of glyphs) { const gw = Math.max(...g.map(r => r.length)); g.forEach((r, ry) => [...r].forEach((ch, rx) => { if (ch === "1") cells.push([cx + rx, ry]); })); cx += gw + gap; }
  const rgb = (c, k = 0) => `rgb(${c.map(v => Math.max(0, Math.min(255, Math.round(v * (1 + k))))).join(",")})`;
  const mix = (a, b, t) => a.map((v, i) => v + (b[i] - v) * t);
  const X = c => pad + c * u, Y = r => pad + u + r * u;
  // The dark outline, then the sides (stacked down and right), then the lit fronts of the blocks.
  x.fillStyle = rgb(line); for (const [c, r] of cells) x.fillRect(X(c) - 3, Y(r) - 3, u + 6 + depth * 3, u + 6 + depth * 3);
  for (let d = depth; d >= 1; d--) for (const [c, r] of cells) { x.fillStyle = rgb(side, -0.12 * (d / depth)); x.fillRect(X(c) + d * 3, Y(r) + d * 3, u, u); }
  let seed = 7; const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  for (const [c, r] of cells) {
    const base = mix(top, bot, r / (rows - 1)), px = X(c), py = Y(r), q = Math.max(2, Math.round(u / 7));
    x.fillStyle = rgb(base); x.fillRect(px, py, u, u);
    for (let i = 0; i < 3; i++) { x.fillStyle = rgb(base, (rnd() - 0.5) * 0.18); x.fillRect(px + Math.floor(rnd() * (u / q)) * q, py + Math.floor(rnd() * (u / q)) * q, q, q); }
    x.fillStyle = rgb(base, 0.18); x.fillRect(px, py, u, q); x.fillRect(px, py, q, u);
    x.fillStyle = rgb(base, -0.2); x.fillRect(px, py + u - q, u, q); x.fillRect(px + u - q, py, q, u);
  }
  return cv.toDataURL();
}

// ---------- icons: drawn here, simple round shapes in the same palette ----------
const ICON = {
  dex: `<svg viewBox="0 0 64 64"><rect x="10" y="6" width="44" height="52" rx="10" fill="#E5525C"/><rect x="16" y="14" width="32" height="20" rx="5" fill="#BFEAF5"/><circle cx="22" cy="45" r="5" fill="#FFD45A"/><rect x="32" y="42" width="14" height="5" rx="2.5" fill="#123C38"/><circle cx="18" cy="10" r="2" fill="#fff"/></svg>`,
  ball: `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="#F4F6F5"/><path d="M8 32a24 24 0 0 1 48 0z" fill="#E5525C"/><rect x="8" y="29" width="48" height="6" fill="#26304A"/><circle cx="32" cy="32" r="8" fill="#26304A"/><circle cx="32" cy="32" r="4.5" fill="#fff"/></svg>`,
  bag: `<svg viewBox="0 0 64 64"><rect x="12" y="18" width="40" height="38" rx="11" fill="#E9B43C"/><path d="M22 18v-4a10 10 0 0 1 20 0v4" stroke="#9A6A1E" stroke-width="5" fill="none"/><rect x="18" y="32" width="28" height="14" rx="5" fill="#C98B22"/><rect x="29" y="30" width="6" height="8" rx="2" fill="#26304A"/></svg>`,
  map: `<svg viewBox="0 0 64 64"><path d="M8 14l16-6 16 6 16-6v42l-16 6-16-6-16 6z" fill="#7CC4AE"/><path d="M24 8v42M40 14v42" stroke="#4FA88E" stroke-width="3"/><path d="M14 40c8-14 18 2 30-16" stroke="#E5525C" stroke-width="3.5" fill="none" stroke-dasharray="4 4"/><circle cx="44" cy="24" r="5" fill="#E5525C"/></svg>`,
  cap: `<svg viewBox="0 0 64 64"><path d="M12 38a20 20 0 0 1 40 0z" fill="#E5525C"/><path d="M8 38h48a4 4 0 0 1 0 8H20z" fill="#B83A43"/><circle cx="32" cy="27" r="6" fill="#fff"/><circle cx="32" cy="27" r="3" fill="#E5525C"/></svg>`,
  card: `<svg viewBox="0 0 64 64"><rect x="6" y="14" width="52" height="36" rx="8" fill="#4A8FE0"/><circle cx="21" cy="30" r="7" fill="#FFE2C4"/><rect x="13" y="38" width="16" height="7" rx="3.5" fill="#FFE2C4"/><rect x="34" y="24" width="18" height="4" rx="2" fill="#fff"/><rect x="34" y="32" width="14" height="4" rx="2" fill="#BFD8F7"/><circle cx="50" cy="44" r="3" fill="#FFD45A"/></svg>`,
  clock: `<svg viewBox="0 0 64 64"><circle cx="32" cy="34" r="22" fill="#F4F6F5"/><circle cx="32" cy="34" r="22" fill="none" stroke="#8E6BC8" stroke-width="5"/><path d="M32 34V20M32 34l10 6" stroke="#26304A" stroke-width="4" stroke-linecap="round"/><rect x="26" y="6" width="12" height="6" rx="3" fill="#8E6BC8"/></svg>`,
  save: `<svg viewBox="0 0 64 64"><rect x="10" y="10" width="44" height="44" rx="10" fill="#41BF84"/><rect x="20" y="10" width="24" height="16" rx="3" fill="#E9FFF4"/><rect x="34" y="13" width="6" height="10" rx="2" fill="#41BF84"/><rect x="18" y="34" width="28" height="14" rx="4" fill="#E9FFF4"/></svg>`,
  cog: `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="13" fill="none" stroke="#A9B4D4" stroke-width="9"/><g stroke="#A9B4D4" stroke-width="8" stroke-linecap="round"><path d="M32 6v8M32 50v8M6 32h8M50 32h8M13 13l6 6M45 45l6 6M13 51l6-6M45 19l6-6"/></g><circle cx="32" cy="32" r="5" fill="#123C38"/></svg>`,
  close: `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="#E5525C"/><path d="M22 22l20 20M42 22L22 42" stroke="#fff" stroke-width="6" stroke-linecap="round"/></svg>`,
  potion: `<svg viewBox="0 0 64 64"><rect x="24" y="6" width="16" height="10" rx="3" fill="#7D8990"/><path d="M20 16h24l4 10v24a8 8 0 0 1-8 8H24a8 8 0 0 1-8-8V26z" fill="#B86AE0"/><path d="M18 34h28v14a6 6 0 0 1-6 6H24a6 6 0 0 1-6-6z" fill="#8E44C0"/><rect x="24" y="22" width="5" height="10" rx="2.5" fill="#fff" opacity=".7"/></svg>`,
  key: `<svg viewBox="0 0 64 64"><circle cx="22" cy="24" r="12" fill="none" stroke="#FFD45A" stroke-width="7"/><path d="M30 32l20 20M42 44l6-6M48 50l5-5" stroke="#FFD45A" stroke-width="7" stroke-linecap="round"/></svg>`,
  disc: `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="#5A8EA1"/><circle cx="32" cy="32" r="24" fill="none" stroke="#9FD3E2" stroke-width="3"/><circle cx="32" cy="32" r="7" fill="#123C38"/><path d="M18 24a16 16 0 0 1 14-8" stroke="#fff" stroke-width="3" fill="none" opacity=".7"/></svg>`,
  heal: `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="#F08AA8"/><rect x="27" y="16" width="10" height="32" rx="3" fill="#fff"/><rect x="16" y="27" width="32" height="10" rx="3" fill="#fff"/></svg>`,
  badge: c => `<svg viewBox="0 0 64 64"><path d="M32 6l8 14 16 2-12 11 3 16-15-8-15 8 3-16L8 22l16-2z" fill="${c}"/><circle cx="32" cy="28" r="6" fill="#fff" opacity=".5"/></svg>`,
};
const ITEM_ICON = n => /BALL/.test(n) ? ICON.ball : /POTION|HEAL|ANTIDOTE|AWAKENING/.test(n) ? ICON.potion : /^TM|^HM/.test(n) ? ICON.disc : /MAP/.test(n) ? ICON.map : /ROPE|REPEL/.test(n) ? ICON.bag : ICON.key;

// ---------- screens, and moving between them ----------
const SCREENS = [], BY = {};
const SKYFOR = { battle: "linear-gradient(180deg,#7EC6EC 0%,#BFE6F7 55%,#E8F7FB 70%)", town: "#0A1715", counter: "#0A1715" };
function screen(id, group, name, o) { const s = Object.assign({ id, group, name, scene: "none" }, o); SCREENS.push(s); BY[id] = s; }
let cur = null, curDef = null, timers = [], curId = null, curArg;
function later(ms, fn) { const t = setTimeout(fn, ms); timers.push(t); return t; }
function clearTimers() { for (const t of timers) { clearTimeout(t); clearInterval(t); } timers = []; }
function go(id, arg) {
  if (cur && cur.leave) cur.leave();
  clearTimers(); uiRoot.innerHTML = ""; backdrop(null);
  curDef = BY[id]; curId = id; curArg = arg;
  if (SKYFOR[curDef.scene]) backdrop(SKYFOR[curDef.scene]);
  setScene(curDef.scene, curDef.sceneOpts && curDef.sceneOpts(arg));
  setPixel(curDef.pixel || 0);
  cur = curDef.make(arg) || {};
  renderFlows(); renderNotes();
  try { history.replaceState(null, "", "#" + id); } catch (e) {}
}
function step(d) { const i = SCREENS.indexOf(curDef); go(SCREENS[(i + d + SCREENS.length) % SCREENS.length].id); }
function hints(list, o = {}) {
  // A and B are the keyboard's SPACE and ESC (the letter A on a keyboard is "left"), so say so.
  const KB = { A: "SPACE", B: "ESC" };
  const e = h("div", "g-hints", list.map(([k, l]) => { k = KB[k] || k; return `<span><span class="g-key${k.length === 1 ? " round" : ""}">${esc(k)}</span>${esc(l)}</span>`; }).join(""));
  if (o.left) { e.style.right = "auto"; e.style.left = "22px"; }
  return add(e);
}

// ---------- the text box: letters come in at the chosen text speed; A goes on ----------
function speedCps() { return [28, 52, 110][G.opts.textSpeed]; }
function textBox(lines, o = {}) {
  lines = lines.map(l => typeof l === "string" ? { text: l } : l);
  const el = box(o.x ?? 140, null, o.w ?? 1000, null, "g-panel g-text g-fadein");
  el.style.top = "auto"; el.style.left = (o.x ?? 140) + "px"; el.style.right = "auto"; el.style.bottom = (o.bottom ?? 26) + "px";
  const who = h("span", "who"), body = h("span", ""), more = h("span", "more");
  el.append(who, body, more);
  let i = 0, shown = 0, t0 = 0, full = "", raf = 0, choice = null, dead = false;
  let res; const done = new Promise(r => res = r);
  function show() {
    const L = lines[i]; who.hidden = !L.who; who.textContent = L.who || ""; full = L.text; shown = 0; t0 = performance.now(); more.hidden = true;
    if (L.on) L.on();
    if (choice) { choice.el.remove(); choice = null; }
    tick();
  }
  function tick() {
    if (dead) return;
    const n = Math.min(full.length, Math.floor((performance.now() - t0) / 1000 * speedCps()));
    if (n !== shown) { shown = n; body.innerHTML = rich(full.slice(0, shown)); }
    if (shown < full.length) raf = requestAnimationFrame(tick); else finished();
  }
  function finished() {
    const L = lines[i];
    if (L.ask) { if (!choice) choice = choiceBox(L.ask, { x: 1280 - 140 - 300, y: null, bottom: (o.bottom ?? 26) + 150, onPick: k => { const c = choice; choice = null; c.el.remove(); end(k); } }); }
    else if (!(o.keep && i === lines.length - 1)) more.hidden = false;
  }
  function end(v) { if (i < lines.length - 1) { i++; show(); } else { if (!o.keep) { dead = true; el.remove(); } res(v); if (o.onDone) o.onDone(v); } }
  function key(k) {
    if (choice) return choice.key(k);
    if (k !== "a") return false;
    if (shown < full.length) { shown = full.length; body.innerHTML = rich(full); cancelAnimationFrame(raf); finished(); return true; }
    if (lines[i].ask) return true;
    if (o.keep && i === lines.length - 1) { res(); if (o.onDone) o.onDone(); return true; }
    end(); return true;
  }
  el.addEventListener("click", () => key("a"));
  show();
  return { el, key, done, kill() { dead = true; cancelAnimationFrame(raf); el.remove(); if (choice) choice.el.remove(); } };
}
// A list in a panel: up and down to pick, A to choose, B to go back.
function choiceBox(items, o = {}) {
  const el = box(o.x ?? 900, o.y ?? null, o.w ?? 300, null, "g-panel g-pop");
  el.style.padding = (o.pad ?? 12) + "px";
  if (o.y == null) { el.style.top = "auto"; el.style.bottom = (o.bottom ?? 180) + "px"; }
  const list = h("div", "g-list"); el.appendChild(list);
  let sel = o.sel || 0;
  const rows = items.map((it, i) => {
    const t = typeof it === "string" ? { label: it } : it;
    const r = h("div", "g-item" + (t.off ? " off" : ""), `<span>${rich(t.label)}</span>${t.right != null ? `<span class="r">${t.right}</span>` : ""}`);
    r.addEventListener("mouseenter", () => { sel = i; paint(); if (o.onMove) o.onMove(sel); });
    r.addEventListener("click", e => { e.stopPropagation(); sel = i; paint(); pick(); });
    list.appendChild(r); return r;
  });
  function paint() { rows.forEach((r, i) => r.classList.toggle("on", i === sel)); }
  function pick() { if (o.onPick) o.onPick(sel); }
  paint();
  return {
    el, get sel() { return sel; }, set sel(v) { sel = v; paint(); },
    key(k) {
      if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : -1) + items.length) % items.length; paint(); if (o.onMove) o.onMove(sel); return true; }
      if (k === "a") { pick(); return true; }
      if (k === "b") { if (o.onBack) { o.onBack(); return true; } if (o.backTo != null) { sel = o.backTo; paint(); pick(); return true; } return false; }
      return false;
    }
  };
}
// Moving a cursor round a grid of n things, cols wide.
function gridMove(i, n, cols, k) {
  if (k === "left") return (i - 1 + n) % n;
  if (k === "right") return (i + 1) % n;
  if (k === "up") return i - cols >= 0 ? i - cols : i;
  if (k === "down") return i + cols < n ? i + cols : i;
  return i;
}

// ---------- input ----------
const KEY = { ArrowUp: "up", KeyW: "up", ArrowDown: "down", KeyS: "down", ArrowLeft: "left", KeyA: "left", ArrowRight: "right", KeyD: "right",
  KeyE: "a", Enter: "a", NumpadEnter: "a", Space: "a", KeyZ: "a", KeyQ: "b", Backspace: "b", Escape: "b", KeyX: "b", Tab: "tab" };
let stageActive = false;
stageEl.addEventListener("pointerdown", () => { stageActive = true; stageEl.focus({ preventScroll: true }); });
document.addEventListener("pointerdown", e => { if (!stageEl.contains(e.target)) stageActive = false; });
window.addEventListener("keydown", e => {
  const t = e.target; if (t && (t.tagName === "SELECT" || t.tagName === "INPUT" || t.tagName === "TEXTAREA")) return;
  if (e.metaKey || e.ctrlKey || e.altKey) return;
  if (!stageActive && document.activeElement !== stageEl) return;
  if (cur && cur.raw && cur.raw(e)) { e.preventDefault(); return; }
  const k = KEY[e.code]; if (!k) return;
  if (e.repeat && (k === "a" || k === "b")) { e.preventDefault(); return; }
  const handled = cur && cur.key ? cur.key(k) : false;
  if (handled !== false || k === "tab" || k === "a" || k.length < 6) e.preventDefault();
});

// ---------- the list of screens beside the stage, and the notes for each ----------
function renderFlows() {
  const nav = $("#flows"); nav.innerHTML = "";
  let g = null, n = 0;
  const sel = $("#flowSel"); sel.innerHTML = "";
  let og = null;
  for (const s of SCREENS) {
    n++;
    if (s.group !== g) { g = s.group; nav.appendChild(h("h2", "", esc(g))); og = document.createElement("optgroup"); og.label = g; sel.appendChild(og); }
    const b = h("button", "", `<small>${n}</small><span>${esc(s.name)}</span>`); b.setAttribute("aria-current", String(s === curDef));
    b.onclick = () => { go(s.id); stageEl.focus({ preventScroll: true }); stageActive = true; };
    nav.appendChild(b);
    const o = document.createElement("option"); o.value = s.id; o.textContent = `${n}. ${s.name}`; og.appendChild(o);
  }
  sel.value = curDef.id;
  sel.onchange = () => { sel.blur(); go(sel.value); stageEl.focus({ preventScroll: true }); stageActive = true; };
  const on = nav.querySelector('[aria-current="true"]'); if (on) { const r = on.getBoundingClientRect(), R = nav.getBoundingClientRect(); if (r.top < R.top || r.bottom > R.bottom) nav.scrollTop += r.top - R.top - 60; }
}
function renderNotes() {
  const d = curDef, n = $("#notes");
  n.innerHTML = `<div><div class="grp">${esc(d.group)}</div><div class="nt">${esc(d.name)}</div></div>
    <p>${d.what || ""}</p>
    <div class="tagrow">${(d.tags || []).map(t => `<span class="tg${t.startsWith("New") ? " new" : ""}">${esc(t)}</span>`).join("")}</div>
    ${d.mod ? `<div><h3>In the mod today</h3><p>${d.mod}</p></div>` : ""}
    ${d.try ? `<div><h3>Try</h3><ul>${d.try.map(t => `<li>${t}</li>`).join("")}</ul></div>` : ""}`;
}
$("#prev").onclick = () => { step(-1); stageEl.focus({ preventScroll: true }); stageActive = true; };
$("#next").onclick = () => { step(1); stageEl.focus({ preventScroll: true }); stageActive = true; };
$("#restart").onclick = () => { go(SCREENS[0].id); stageEl.focus({ preventScroll: true }); stageActive = true; };
for (const b of document.querySelectorAll(".pad [data-k]")) b.addEventListener("click", () => { stageActive = true; if (cur && cur.key) cur.key(b.dataset.k); });
