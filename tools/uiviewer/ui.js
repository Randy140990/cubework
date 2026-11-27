// ---------- the game screen: built at 1280 by 720, scaled to whatever the stage is ----------
const stageEl = $("#stage"), uiRoot = $("#ui"), bgEl = $("#bg");
// What's drawn on the backdrop itself (glows, swirls, drifting embers and bubbles): behind the 3D scene, never over it.
const bgArt = document.createElement("div");
Object.assign(bgArt.style, { position: "absolute", left: "0", top: "0", width: "1280px", height: "720px", transformOrigin: "0 0", overflow: "hidden", pointerEvents: "none" });
bgEl.appendChild(bgArt);
function fitUI() { const w = stageEl.clientWidth || 1280; uiRoot.style.transform = `scale(${w / 1280})`; bgArt.style.transform = `scale(${w / 1280})`; }
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
  opts: { textSpeed: 1, music: 3, sound: 3, voice: 1, nights: 1, wild: 1, effects: 0, style: 0, follower: 1, look: 1, shaders: 1, cycle: 0, battles: 0, travel: 0, photo: 0 },
  party: [], place: "PALLET TOWN",
  bag: {
    "ITEMS": [["POTION", 5, "Restores 20 HP to one POKéMON."], ["SUPER POTION", 2, "Restores 50 HP to one POKéMON."], ["ANTIDOTE", 3, "Cures a POKéMON of poison."], ["PARLYZ HEAL", 1, "Cures a POKéMON of paralysis."], ["AWAKENING", 1, "Wakes a sleeping POKéMON."], ["ESCAPE ROPE", 1, "Takes you back out of a cave in a flash."], ["REPEL", 4, "Keeps weak wild POKéMON away for 100 steps."]],
    "BALLS": [["POKé BALL", 12, "A ball for catching wild POKéMON."], ["GREAT BALL", 3, "A better ball, with a higher chance of a catch."], ["ULTRA BALL", 2, "A very good ball: better than a GREAT BALL."],
      ["MASTER BALL", 1, "The best ball there is. It catches any wild POKéMON without fail."], ["SAFARI BALL", 30, "For the SAFARI ZONE only."], ["PARK BALL", 20, "For the Bug-Catching Contest in the NATIONAL PARK."],
      ["LEVEL BALL", 1, "Better the higher your POKéMON's level is than theirs. Made by KURT from an apricorn."], ["LURE BALL", 1, "Good for POKéMON hooked on a rod. Made by KURT."], ["MOON BALL", 1, "Good for POKéMON that evolve with a MOON STONE. Made by KURT."],
      ["FRIEND BALL", 1, "A POKéMON caught in it is friendly to you at once. Made by KURT."], ["LOVE BALL", 1, "Good for a POKéMON of the other gender to yours. Made by KURT."], ["HEAVY BALL", 1, "Better the heavier the POKéMON is. Made by KURT."], ["FAST BALL", 1, "Good for POKéMON that run away. Made by KURT."]],
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
// The game's type colours (Ui.typeColor), for pages in a creature's own colour.
const TYPEHEX = { NORMAL: "#9DA0AA", FIRE: "#F08436", WATER: "#4D90D5", ELECTRIC: "#E9C72E", GRASS: "#5DB850", ICE: "#6FCFD0", FIGHTING: "#CE4069", POISON: "#AB6AC8", GROUND: "#D97746",
  FLYING: "#8FA8DD", PSYCHIC: "#F66F71", BUG: "#90C12C", ROCK: "#C5B78C", GHOST: "#5269AC", DRAGON: "#0A6DC4", DARK: "#5A5366", STEEL: "#5A8EA1" };
const typeHex = t => TYPEHEX[String(t).toUpperCase()] || "#8A8FA3";
// A page's backdrop in its creature's colour (Scenes.typeBackdrop): lit from the top left, shaded to the bottom right.
function typeBackdrop(t) { const c = typeHex(t); backdrop(`linear-gradient(160deg, ${lighterHex(c, 0.62)}, ${c} 55%, ${darkerHex(c, 0.38)})`); }
// A band in the creature's colour across the top of its page, for its name (Scenes.typeBand).
function typeBand(t, x, y, w, hh) { const c = typeHex(t); return `<div style="position:absolute;left:${x}px;top:${y}px;width:${w}px;height:${hh}px;border-radius:22px;background:linear-gradient(180deg, ${lighterHex(c, 0.12)}, ${darkerHex(c, 0.14)} 55%, ${darkerHex(c, 0.32)});box-shadow:0 0 0 2px rgba(255,255,255,.4), 0 4px 0 rgba(0,0,0,.2)"></div>`; }
const TYPECOL = t => `var(--t-${String(t).toLowerCase()})`;
// Each type's own little mark, white (ours, drawn plain): for the move bars, as Let's Go marks each move with its type.
const TYPE_GLYPH = {
  NORMAL: `<circle cx="32" cy="32" r="17" fill="none" stroke="#fff" stroke-width="7"/>`,
  FIRE: `<path d="M32 6c4 10 15 15 15 29 0 10-7 19-15 19s-15-9-15-19c0-7 4-11 6-16 2 5 4 7 6 8 0-7 0-14 3-21z" fill="#fff"/>`,
  WATER: `<path d="M32 6C38 17 48 27 48 38a16 16 0 0 1 -32 0C16 27 26 17 32 6z" fill="#fff"/>`,
  GRASS: `<path d="M32 6C46 14 52 28 48 41 44 51 38 56 32 58 26 56 20 51 16 41 12 28 18 14 32 6z" fill="#fff"/><path d="M32 14V54" stroke="currentColor" stroke-width="3" opacity=".5"/>`,
  ELECTRIC: `<path d="M37 5L17 35h12l-5 24 23-33H35z" fill="#fff"/>`,
  ICE: `<g stroke="#fff" stroke-width="5" stroke-linecap="round"><path d="M32 7v50M10 19.5l44 25M10 44.5l44-25"/><path d="M26 11l6 6 6-6M26 53l6-6 6 6"/></g>`,
  FIGHTING: `<path d="M16 26c0-5 4-8 8-8h18c6 0 10 4 10 10v10c0 9-7 16-16 16h-6c-8 0-14-6-14-14z" fill="#fff"/><path d="M24 18v10M33 18v10M42 18v10" stroke="currentColor" stroke-width="3" opacity=".45"/>`,
  POISON: `<circle cx="32" cy="38" r="15" fill="#fff"/><circle cx="22" cy="16" r="6" fill="#fff"/><circle cx="41" cy="12" r="4" fill="#fff"/>`,
  GROUND: `<path d="M6 46L22 20l9 13 7-9 20 22z" fill="#fff"/><path d="M8 52h48" stroke="#fff" stroke-width="5" stroke-linecap="round"/>`,
  FLYING: `<path d="M8 40c10-22 28-30 48-30-6 6-9 12-10 18 4 0 7-1 10-3-4 10-12 16-22 17 3 1 5 2 8 2-10 6-24 4-34-4z" fill="#fff"/>`,
  PSYCHIC: `<path d="M6 32c8-12 17-18 26-18s18 6 26 18c-8 12-17 18-26 18S14 44 6 32z" fill="#fff"/><circle cx="32" cy="32" r="8" fill="currentColor" opacity=".55"/>`,
  BUG: `<ellipse cx="32" cy="37" rx="14" ry="18" fill="#fff"/><path d="M26 18q-6-10-12-10M38 18q6-10 12-10" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round"/><path d="M32 22v32" stroke="currentColor" stroke-width="3" opacity=".45"/>`,
  ROCK: `<path d="M12 42L10 28 19 15 34 10 48 16 55 31 50 47 34 55 19 52z" fill="#fff"/><path d="M19 15L27 30 48 16M27 30L34 55" stroke="currentColor" stroke-width="2.5" opacity=".4" fill="none"/>`,
  GHOST: `<path d="M14 56V28a18 18 0 0 1 36 0v28l-6-5-6 5-6-5-6 5-6-5z" fill="#fff"/><circle cx="25" cy="29" r="4" fill="currentColor" opacity=".6"/><circle cx="39" cy="29" r="4" fill="currentColor" opacity=".6"/>`,
  DRAGON: `<path d="M14 54C14 30 26 12 50 8 40 18 38 26 40 34 46 30 50 30 54 32 44 40 34 46 14 54z" fill="#fff"/>`,
  DARK: `<path d="M40 8a24 24 0 1 0 16 40 20 20 0 1 1 -16 -40z" fill="#fff"/>`,
  STEEL: `<path d="M32 6l22 13v26L32 58 10 45V19z" fill="#fff"/><circle cx="32" cy="32" r="9" fill="currentColor" opacity=".5"/>`,
  FAIRY: `<path d="M32 4l6 20 20 2-15 13 5 21-16-11-16 11 5-21L6 26l20-2z" fill="#fff"/>`,
};
function typeGlyph(t, size, extra = "") { const g = TYPE_GLYPH[String(t).toUpperCase()] || TYPE_GLYPH.NORMAL; return `<svg viewBox="0 0 64 64" width="${size}" height="${size}" style="display:block;color:${TYPECOL(t)};${extra}">${g}</svg>`; }
function typeChip(t) { return `<span class="g-type" style="background:${TYPECOL(t)}">${esc(t)}</span>`; }
const STATUS = { PSN: "#AB6AC8", BRN: "#F08436", PAR: "#D9B42C", SLP: "#8A93A6", FRZ: "#6FCFD0", FNT: "#B0475A" };
function statusChip(s) { return s ? `<span class="g-status" style="background:${STATUS[s] || "#666"}">${s}</span>` : ""; }
function hpCol(f) { return f > 0.5 ? "var(--hp-green)" : f > 0.2 ? "var(--hp-yellow)" : "var(--hp-red)"; }
function hpBar(f, w) { return `<div class="g-hp" style="width:${w}px"><i style="width:${Math.max(0, f) * 100}%;background:${hpCol(f)}"></i></div>`; }
function gender(g) { return g === "F" ? `<span style="color:#FF8FB1">&#9792;</span>` : g === "M" ? `<span style="color:#7FB8FF">&#9794;</span>` : ""; }
function darkerHex(c, k) { const v = mixc(hx(c), [0, 0, 0], k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
function mixHex(a, b, k) { const v = mixc(hx(a), hx(b), k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
function lighterHex(c, k) { const v = mixc(hx(c), [255, 255, 255], k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
function money(n) { return "&#8381;" + n.toLocaleString("en-GB"); }

// ---------- the backdrop's own light: soft glows and swirls, behind everything (never lines over the creatures) ----------
function bgAdd(html, style) { const e = h("div", "", html, Object.assign({ position: "absolute", inset: "0" }, style || {})); bgArt.appendChild(e); return e; }
// A soft bloom of light from a point (x, y in %), with a wide pale ring round it, as Let's Go's backdrops glow.
function rays(x = 20, y = 10, a = 0.16) {
  return bgAdd("", { background: `radial-gradient(circle at ${x}% ${y}%, rgba(255,255,255,${Math.min(0.85, a * 4.2)}) 0%, rgba(255,255,255,${a * 1.6}) 22%, transparent 48%),
    radial-gradient(circle at ${x}% ${y}%, transparent 52%, rgba(255,255,255,${a}) 60%, transparent 70%)` });
}
// Each creature's own backdrop (as Let's Go's STATS pages): its type's colours (both types if it has two), a great
// soft swirl of light, blurred dots of light, and its own drifting things: embers for fire, bubbles for water, leaves
// for grass, sparks for electric, snow for ice... Placed by its name, so each creature's is its own.
const MONBG = {
  FIRE: [["#FFE2A0", "#F48A5A", "#B4466E"], "ember"], WATER: [["#DDF7FF", "#5FC4EE", "#2C66B4"], "bubble"], GRASS: [["#F3FFC0", "#71D18C", "#1D8A78"], "leaf"],
  ELECTRIC: [["#FFF8BE", "#FFD43E", "#E3841C"], "spark"], NORMAL: [["#FFF6EA", "#F5C8B6", "#C687A6"], "mote"], POISON: [["#F5DEFF", "#B97BE2", "#64358F"], "bubble"],
  GROUND: [["#FFF2D2", "#E1AE6C", "#966338"], "dust"], ROCK: [["#F5EFDD", "#C9B487", "#7A6646"], "dust"], FLYING: [["#F6FBFF", "#A9C9F5", "#6684D6"], "mote"],
  PSYCHIC: [["#FFE8F2", "#F08AB6", "#934897"], "star"], BUG: [["#F7FFCA", "#B6DA4C", "#5A882A"], "firefly"], ICE: [["#F4FFFF", "#A9E9F2", "#57A4C6"], "snow"],
  FIGHTING: [["#FFE3D3", "#E27C5C", "#963838"], "ember"], GHOST: [["#ECE4FF", "#8C7CD2", "#382C66"], "wisp"], DRAGON: [["#E2EDFF", "#6C8CEA", "#37379A"], "star"],
  STEEL: [["#F2F6FA", "#ACBACA", "#5B6B80"], "glint"], DARK: [["#ECE4EC", "#8C7C8C", "#382E3C"], "mote"], FAIRY: [["#FFEAF4", "#F6A6CC", "#B65C9C"], "star"] };
function hashStr(s) { let x = 2166136261; for (const c of String(s)) { x ^= c.charCodeAt(0); x = Math.imul(x, 16777619); } return x >>> 0; }
function monBackdrop(sid, o = {}) {
  const d = sp(sid), types = d.stats.types.map(t => String(t).toUpperCase()), [cols, kind] = MONBG[types[0]] || MONBG.NORMAL, two = types[1] && MONBG[types[1]] ? MONBG[types[1]][0] : null;
  let seed = hashStr(sid); const rnd = () => (seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296;
  // The swirl's middle sits behind the creature (cx, cy), its arc sweeping round it.
  const cx = o.cx ?? 32, cy = o.cy ?? 62, sx = cx + 18 + rnd() * 14, sy = cy - 22 + rnd() * 16;
  backdrop(`radial-gradient(ellipse 60% 70% at ${cx}% ${cy}%, rgba(255,255,255,.62) 0%, rgba(255,255,255,.22) 30%, transparent 58%),
    radial-gradient(circle at ${sx}% ${sy}%, transparent 30%, rgba(255,255,255,.34) 37%, rgba(255,255,255,.12) 44%, transparent 52%),
    radial-gradient(circle at ${sx - 6}% ${sy + 8}%, transparent 46%, rgba(255,255,255,.2) 52%, transparent 60%),
    ${two ? `radial-gradient(ellipse at 100% 0%, ${two[1]}cc 0%, transparent 55%),` : ""}
    linear-gradient(${120 + Math.round(rnd() * 40)}deg, ${cols[0]} 0%, ${cols[1]} 46%, ${cols[2]} 100%)`);
  // Blurred dots of light.
  let dots = ""; for (let i = 0; i < 14; i++) { const r = 18 + rnd() * 46; dots += `<span style="position:absolute;left:${rnd() * 100}%;top:${rnd() * 100}%;width:${r}px;height:${r}px;border-radius:50%;background:rgba(255,255,255,${0.12 + rnd() * 0.22});filter:blur(${2 + rnd() * 6}px)"></span>`; }
  bgAdd(dots);
  // Its own drifting things.
  const P = { ember: (c) => `width:7px;height:7px;border-radius:50%;background:radial-gradient(circle,#FFF6C0,${cols[1]} 60%,transparent 72%);box-shadow:0 0 10px ${cols[1]}`,
    bubble: () => `width:18px;height:18px;border-radius:50%;border:2px solid rgba(255,255,255,.75);background:radial-gradient(circle at 35% 30%,rgba(255,255,255,.7) 0 18%,transparent 30%)`,
    leaf: () => `width:20px;height:10px;border-radius:0 100% 0 100%;background:linear-gradient(135deg,#C8F28A,#4FAE5C)`,
    spark: () => `width:12px;height:12px;background:#FFFBE0;clip-path:polygon(50% 0,62% 38%,100% 50%,62% 62%,50% 100%,38% 62%,0 50%,38% 38%);filter:drop-shadow(0 0 4px #FFE45A)`,
    star: () => `width:12px;height:12px;background:#fff;clip-path:polygon(50% 0,62% 38%,100% 50%,62% 62%,50% 100%,38% 62%,0 50%,38% 38%);filter:drop-shadow(0 0 4px #fff)`,
    snow: () => `width:9px;height:9px;border-radius:50%;background:#fff;box-shadow:0 0 6px #fff`, firefly: () => `width:7px;height:7px;border-radius:50%;background:#F8FFB0;box-shadow:0 0 12px #EFFF7A`,
    dust: () => `width:6px;height:6px;border-radius:50%;background:rgba(255,246,220,.8)`, mote: () => `width:7px;height:7px;border-radius:50%;background:rgba(255,255,255,.85);box-shadow:0 0 8px #fff`,
    wisp: () => `width:16px;height:16px;border-radius:50%;background:radial-gradient(circle,rgba(220,210,255,.9),transparent 70%)`, glint: () => `width:10px;height:10px;background:#fff;clip-path:polygon(50% 0,58% 42%,100% 50%,58% 58%,50% 100%,42% 58%,0 50%,42% 42%)` };
  const rise = { ember: 1, bubble: 1, spark: 0, star: 0, wisp: 1, firefly: 0, glint: 0, mote: 0, dust: 0 }[kind], fall = kind === "leaf" || kind === "snow";
  let bits = ""; for (let i = 0; i < 22; i++) { const x = rnd() * 100, y = rnd() * 100, t = 5 + rnd() * 7, dl = -rnd() * t, sc = 0.6 + rnd() * 0.9;
    const an = rise ? `bgRise ${t}s linear ${dl}s infinite` : fall ? `bgFall ${t * 1.3}s linear ${dl}s infinite` : `bgTwinkle ${2 + rnd() * 3}s ease-in-out ${dl}s infinite`;
    bits += `<span style="position:absolute;left:${x}%;top:${y}%;transform:scale(${sc});${P[kind]()};animation:${an}"></span>`; }
  bgAdd(bits + `<style>@keyframes bgRise{from{transform:translate(0,120px);opacity:0}15%{opacity:1}to{transform:translate(30px,-260px);opacity:0}}@keyframes bgFall{from{transform:translate(0,-140px) rotate(0);opacity:0}15%{opacity:1}to{transform:translate(-60px,260px) rotate(320deg);opacity:0}}@keyframes bgTwinkle{0%,100%{opacity:.15;transform:scale(.5)}50%{opacity:1;transform:scale(1.1)}}</style>`);
  return { cols, kind };
}

// The bright pages (saves, options, the maker, the bag, the card...) get the same kind of backdrop as the creatures, to
// a degree: their colour as a sweep, a great soft swirl of light, blurred dots, a few motes twinkling, and the bloom of
// light from a corner. Placed by the screen, so each page's swirl sits in its own place.
const BRIGHT = { sky: ["#F6FCFF", "#93D2F6", "#4A8EDD"], lime: ["#FCFFE6", "#CDEB72", "#73B53C"], peach: ["#FFF7F0", "#FFC9A6", "#E8826A"], mint: ["#F2FFF8", "#9EE9C8", "#40AE8C"], dream: ["#F6FEFF", "#A9E2EF", "#4182C0"] };
function swirlBackdrop(name, o = {}) {
  const cols = BRIGHT[name] || BRIGHT.sky;
  let seed = hashStr((o.seed || "") + name + (typeof curId !== "undefined" ? curId : "")); const rnd = () => (seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296;
  const cx = o.cx ?? 30 + rnd() * 40, cy = o.cy ?? 40 + rnd() * 25, sx = cx + (rnd() - 0.5) * 30, sy = cy + (rnd() - 0.5) * 20, lx = o.lx ?? 85, ly = o.ly ?? 5;
  backdrop(`radial-gradient(circle at ${lx}% ${ly}%, rgba(255,255,255,.7) 0%, rgba(255,255,255,.25) 18%, transparent 40%),
    radial-gradient(ellipse 55% 65% at ${cx}% ${cy}%, rgba(255,255,255,.42) 0%, rgba(255,255,255,.14) 32%, transparent 60%),
    radial-gradient(circle at ${sx}% ${sy}%, transparent 32%, rgba(255,255,255,.26) 39%, rgba(255,255,255,.09) 46%, transparent 54%),
    radial-gradient(circle at ${sx - 7}% ${sy + 9}%, transparent 48%, rgba(255,255,255,.16) 54%, transparent 62%),
    linear-gradient(${125 + Math.round(rnd() * 30)}deg, ${cols[0]} 0%, ${cols[1]} 48%, ${cols[2]} 100%)`);
  let dots = ""; for (let i = 0; i < 10; i++) { const r = 18 + rnd() * 50; dots += `<span style="position:absolute;left:${rnd() * 100}%;top:${rnd() * 100}%;width:${r}px;height:${r}px;border-radius:50%;background:rgba(255,255,255,${0.1 + rnd() * 0.18});filter:blur(${2 + rnd() * 6}px)"></span>`; }
  for (let i = 0; i < 14; i++) { const dl = -rnd() * 5; dots += `<span style="position:absolute;left:${rnd() * 100}%;top:${rnd() * 100}%;width:7px;height:7px;border-radius:50%;background:rgba(255,255,255,.9);box-shadow:0 0 8px #fff;animation:bgTwinkle ${2 + rnd() * 3}s ease-in-out ${dl}s infinite"></span>`; }
  bgAdd(dots + `<style>@keyframes bgTwinkle{0%,100%{opacity:.15;transform:scale(.5)}50%{opacity:1;transform:scale(1.1)}}</style>`);
}

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
  // MINECRAFT's picture: a little grass block (green on top, earth under it).
  grass: `<svg viewBox="0 0 26 26"><rect width="26" height="26" rx="5.7" fill="#8A5A36"/><rect x="5.2" y="14.3" width="3.6" height="3.6" rx="1.3" fill="#6B4226"/><rect x="16.1" y="18.7" width="3.6" height="3.6" rx="1.3" fill="#6B4226"/><rect width="26" height="10.9" rx="5.2" fill="#62B84A"/><rect x="3.1" y="7.8" width="4.2" height="5.2" rx="1.6" fill="#62B84A"/><rect x="14.3" y="7.8" width="5.2" height="4.4" rx="1.6" fill="#62B84A"/></svg>`,
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
  parcel: `<svg viewBox="0 0 64 64"><path d="M8 22l24-10 24 10v26L32 58 8 48z" fill="#D9A867"/><path d="M8 22l24 10 24-10" fill="none" stroke="#B07D3E" stroke-width="2.5"/><path d="M32 32v26" stroke="#B07D3E" stroke-width="2.5"/><path d="M8 22l24 10 24-10L32 12z" fill="#ECC489"/><path d="M20 17l24 10v10" stroke="#E5525C" stroke-width="4" fill="none"/><path d="M44 17L20 27" stroke="#E5525C" stroke-width="4"/><rect x="36" y="38" width="14" height="9" rx="2" fill="#FFF6E0" transform="skewY(-22)"/></svg>`,
  sparkle: `<svg viewBox="0 0 40 40"><path d="M20 2l4 14 14 4-14 4-4 14-4-14-14-4 14-4z" fill="#fff"/></svg>`,
  badge: c => `<svg viewBox="0 0 64 64"><path d="M32 6l8 14 16 2-12 11 3 16-15-8-15 8 3-16L8 22l16-2z" fill="${c}"/><circle cx="32" cy="28" r="6" fill="#fff" opacity=".5"/></svg>`,
};
// Every ball its own: the colours and marks it's known by, on the same round shape with its band and button.
const BALL_LOOK = {
  "POKé BALL": ["#E5525C", ""], "GREAT BALL": ["#3C8DE4", `<path d="M14 22q6-8 12-8M50 22q-6-8-12-8" stroke="#E5525C" stroke-width="5" fill="none" stroke-linecap="round"/>`],
  "ULTRA BALL": ["#2E2E36", `<path d="M20 12v14M44 12v14" stroke="#F2C12E" stroke-width="6" stroke-linecap="round"/><path d="M20 26q12 4 24 0" stroke="#F2C12E" stroke-width="5" fill="none"/>`],
  "MASTER BALL": ["#8E52C8", `<circle cx="20" cy="19" r="4.5" fill="#F07AB0"/><circle cx="44" cy="19" r="4.5" fill="#F07AB0"/><path d="M26 24l3-8 3 5 3-5 3 8" stroke="#fff" stroke-width="2.4" fill="none" stroke-linejoin="round"/>`],
  "SAFARI BALL": ["#8DA44A", `<path d="M14 24q5-6 10-2M40 14q6 2 8 8M28 12q4 4 2 9" stroke="#5E6E2C" stroke-width="3.5" fill="none" stroke-linecap="round"/>`, "#D9CB8C"],
  "PARK BALL": ["#F2C12E", `<path d="M12 24h40" stroke="#3C8DE4" stroke-width="3.5"/><path d="M22 12v12M32 9v15M42 12v12" stroke="#3C8DE4" stroke-width="3.2"/>`, "#3C8DE4"],
  "LEVEL BALL": ["#E5525C", `<rect x="10" y="20" width="44" height="7" rx="2" fill="#F2C12E"/>`, "#2E2E36"],
  "LURE BALL": ["#3C8DE4", `<path d="M10 22h44" stroke="#E5525C" stroke-width="4"/><path d="M10 15h44" stroke="#3FA34D" stroke-width="3"/>`],
  "MOON BALL": ["#3A5AA8", `<path d="M40 11a10 10 0 1 0 6 12 7 7 0 1 1 -6 -12z" fill="#F2E27A"/><circle cx="20" cy="20" r="2" fill="#F2E27A"/><circle cx="27" cy="13" r="1.6" fill="#F2E27A"/>`],
  "FRIEND BALL": ["#3FA34D", `<path d="M14 22q5-8 12-3 1 5-5 7-5 0-7-4zM50 22q-5-8-12-3-1 5 5 7 5 0 7-4z" fill="#E5525C"/>`],
  "LOVE BALL": ["#F08FB6", `<path d="M32 26c-6-4-10-7-10-11 0-3 2-5 5-5 2 0 4 1 5 3 1-2 3-3 5-3 3 0 5 2 5 5 0 4-4 7-10 11z" fill="#E5525C"/>`],
  "HEAVY BALL": ["#6E7480", `<rect x="13" y="13" width="38" height="11" rx="3" fill="#3A3F4A"/><rect x="18" y="16" width="5" height="5" rx="1" fill="#B8BEC8"/><rect x="41" y="16" width="5" height="5" rx="1" fill="#B8BEC8"/>`],
  "FAST BALL": ["#F2C12E", `<path d="M10 24l9-11h8l-9 11zM26 24l9-11h8l-9 11z" fill="#E5525C"/>`],
};
function ballIcon(n) {
  const [top, mark, bot] = BALL_LOOK[n] || BALL_LOOK["POKé BALL"];
  return `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="${bot || "#F4F6F5"}"/><path d="M8 32a24 24 0 0 1 48 0z" fill="${top}"/>${mark}<path d="M16 15q6-5 13-6" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity=".45"/><rect x="8" y="29" width="48" height="6" fill="#26304A"/><circle cx="32" cy="32" r="8" fill="#26304A"/><circle cx="32" cy="32" r="4.5" fill="#fff"/><circle cx="32" cy="32" r="24" fill="none" stroke="#26304A" stroke-width="2.5"/></svg>`;
}
const ITEM_ICON = n => /PARCEL/.test(n) ? ICON.parcel : typeof ITEM_ART !== "undefined" && ITEM_ART[itemSlug(n)] ? ITEM_ART[itemSlug(n)] : /BALL/.test(n) ? ballIcon(n) : /POTION|HEAL|ANTIDOTE|AWAKENING/.test(n) ? ICON.potion : /^TM|^HM/.test(n) ? ICON.disc : /MAP/.test(n) ? ICON.map : /ROPE|REPEL/.test(n) ? ICON.bag : ICON.key;
// Each sort of thing's colours in a list (light, middle, deep), as the menu's big buttons; the game's V.GOODS.
const GOODS = { ball: ["#F27682", "#D94452", "#AE2E3C"], med: ["#6BDCA2", "#39B27A", "#22855A"], tm: ["#8EC4F6", "#4A8FE0", "#2F64B0"], key: ["#FFD27A", "#F0A030", "#C87818"], field: ["#C6AEF4", "#8A62D8", "#6440B0"] };
const ITEM_KIND = n => /BALL/.test(n) ? "ball" : /POTION|HEAL|ANTIDOTE|AWAKENING|REVIVE|ETHER|ELIXIR/.test(n) ? "med" : /^TM|^HM/.test(n) ? "tm" : /ROPE|REPEL/.test(n) ? "field" : "key";
const BALLMARK = `<svg viewBox="0 0 128 128"><circle cx="64" cy="64" r="48.5" fill="none" stroke="#fff" stroke-width="11"/><path d="M10 64h33M85 64h33" stroke="#fff" stroke-width="11"/><circle cx="64" cy="64" r="12" fill="none" stroke="#fff" stroke-width="10"/></svg>`;
// A thing in a list (the bag, the shop) as a coloured bar, as V.goods draws it: its sort's colours, its picture in a
// white circle, a Poké Ball mark at the right end; the one you're on cream.
function goodsRow(name, right, on, hh = 46) {
  const c = GOODS[ITEM_KIND(name)];
  return `<div class="g-goods${on ? " on" : ""}" style="--c0:${c[0]};--c1:${c[1]};--c2:${c[2]};height:${hh}px"><span class="ic" style="width:${hh - 10}px;height:${hh - 10}px">${ITEM_ICON(name)}</span><span class="nm">${esc(name)}</span><span class="r">${right == null ? "" : right}</span><span class="mark" style="width:${hh - 16}px;height:${hh - 16}px">${BALLMARK}</span></div>`;
}

// ---------- screens, and moving between them ----------
const SCREENS = [], BY = {};
const SKYFOR = { battle: "linear-gradient(180deg,#7EC6EC 0%,#BFE6F7 55%,#E8F7FB 70%)", town: "#0A1715", counter: "#0A1715" };
function screen(id, group, name, o) { const s = Object.assign({ id, group, name, scene: "none" }, o); SCREENS.push(s); BY[id] = s; }
let cur = null, curDef = null, timers = [], curId = null, curArg;
function later(ms, fn) { const t = setTimeout(fn, ms); timers.push(t); return t; }
function clearTimers() { for (const t of timers) { clearTimeout(t); clearInterval(t); } timers = []; }
function go(id, arg) {
  if (cur && cur.leave) cur.leave();
  clearTimers(); uiRoot.innerHTML = ""; bgArt.innerHTML = ""; backdrop(null);
  curDef = BY[id]; curId = id; curArg = arg;
  if (SKYFOR[curDef.scene]) backdrop(SKYFOR[curDef.scene]);
  setScene(curDef.scene, curDef.sceneOpts && curDef.sceneOpts(arg));
  setPixel(typeof curDef.pixel === "function" ? curDef.pixel() : curDef.pixel || 0);
  cur = curDef.make(arg) || {};
  renderFlows(); renderNotes();
  try { history.replaceState(null, "", "#" + id); } catch (e) {}
}
function step(d) { const i = SCREENS.indexOf(curDef); go(SCREENS[(i + d + SCREENS.length) % SCREENS.length].id); }
// What each key does, bottom right: buttons you can click as well as press (each sends its key).
const HINTKEY = { A: "a", SPACE: "a", B: "b", ESC: "b", TAB: "tab", X: "x", Y: "y", E: "e" };
function pressE() { if (cur && cur.raw && cur.raw({ code: "KeyE" })) return true; return false; }
function pressKey(k) { if (k === "e") { pressE(); return; } if (cur && cur.key) cur.key(k); }
function hints(list, o = {}) {
  // A and B are the keyboard's SPACE and ESC (the letter A on a keyboard is "left"), so say so.
  const KB = { A: "SPACE", B: "ESC" };
  const e = h("div", "g-hints");
  for (const [k0, l] of list) {
    const k = KB[k0] || k0, cls = { SPACE: "k-a", ESC: "k-b", TAB: "k-tab", X: "k-x" }[k] || "";
    const b = h("span", "g-btn", `<span class="g-key ${cls}${k.length === 1 ? " round" : ""}">${esc(k)}</span>${esc(T(l))}`);
    const send = HINTKEY[k0] || HINTKEY[k];
    if (send) b.addEventListener("click", ev => { ev.stopPropagation(); pressKey(send); });
    e.appendChild(b);
  }
  if (o.left) { e.style.right = "auto"; e.style.left = "22px"; }
  return add(e);
}
// The big buttons on the screens you step through: [B Back] and [A Continue] (or whatever they're called), centred
// at the foot of the screen, clicked or pressed.
function actions(list, o = {}) {
  const e = h("div", "g-acts");
  if (o.bottom != null) e.style.bottom = o.bottom + "px";
  if (o.right) { e.style.justifyContent = "flex-end"; e.style.right = "28px"; e.style.left = "auto"; }
  for (const it of list) {
    const go = it.key === "a", b = h("span", "g-act" + (go ? " go" : ""), `<span class="g-key ${go ? "" : "k-b"}">${go ? "A" : "B"}</span><span>${esc(T(it.label))}</span><span class="ar">${go ? "&#9654;" : "&#9664;"}</span>`);
    b.addEventListener("click", ev => { ev.stopPropagation(); pressKey(it.key); });
    e.appendChild(b);
  }
  return add(e);
}

// ---------- the text box: letters come in at the chosen text speed; A goes on ----------
function speedCps() { return [28, 52, 110][G.opts.textSpeed]; }
// Typing a line out letter by letter with its names already in gold: the ** marks never show, and a gold word is gold
// from its first letter.
function plainLen(s) { return String(s).replace(/\*\*/g, "").length; }
function richUpTo(s, n) {
  let out = "", bold = false, left = n;
  for (const part of String(s).split("**")) { if (left <= 0) break; const t = part.slice(0, left); left -= t.length; out += bold ? `<b>${esc(t)}</b>` : esc(t); bold = !bold; }
  return out;
}
// Our own "more to read" mark: a little cube turning, in the text's colour (not anyone's ball).
const MORE_MARK = `<svg viewBox="0 0 24 24" width="26" height="26"><path d="M12 2.5l8.5 4.9v9.2L12 21.5l-8.5-4.9V7.4z" fill="currentColor" opacity=".2"/><path d="M12 2.5l8.5 4.9L12 12.3 3.5 7.4z" fill="currentColor"/><path d="M12 12.3v9.2l-8.5-4.9V7.4z" fill="currentColor" opacity=".7"/><path d="M12 12.3l8.5-4.9v9.2L12 21.5z" fill="currentColor" opacity=".45"/></svg>`;
// A click anywhere on the picture moves the words on, as SPACE does (not just a click on the box).
const LIVE_TEXT = [];
stageEl.addEventListener("click", ev => {
  if (ev.target.closest("button, a, select, input, [data-i], [data-p], [data-r], [data-t], [data-m], .g-btn, .g-act, .g-item, .g-choice, .m-tile, .m-pill, .mk-face, .mk-glass, .g-hints, canvas.mapcv")) return;
  for (let i = LIVE_TEXT.length - 1; i >= 0; i--) { const w = LIVE_TEXT[i]; if (!w.el.isConnected) { LIVE_TEXT.splice(i, 1); continue; } w.tap(); return; }
});
function textBox(lines, o = {}) {
  lines = lines.map(l => typeof l === "string" ? { text: l } : l);
  const el = box(o.x ?? 140, null, o.w ?? 1000, null, "g-panel g-text g-fadein" + (o.film ? " film" : ""));
  el.style.top = "auto"; el.style.left = (o.x ?? 140) + "px"; el.style.right = "auto"; el.style.bottom = (o.bottom ?? 26) + "px";
  if (o.film) { el.style.left = "0"; el.style.width = "1280px"; el.style.bottom = "0"; }
  const who = h("span", "who"), body = h("span", ""), more = h("span", "more", MORE_MARK);
  el.append(who, body, more);
  let i = 0, shown = 0, t0 = 0, full = "", raf = 0, choice = null, dead = false;
  let res; const done = new Promise(r => res = r);
  function show() {
    const L = lines[i]; who.hidden = !L.who; who.textContent = L.who ? T(L.who) : ""; full = T(L.text); shown = 0; t0 = performance.now(); more.hidden = true;
    if (L.on) L.on();
    if (choice) { choice.el.remove(); choice = null; }
    tick();
  }
  function tick() {
    if (dead) return;
    const n = Math.min(plainLen(full), Math.floor((performance.now() - t0) / 1000 * speedCps()));
    if (n !== shown) { shown = n; body.innerHTML = richUpTo(full, shown); }
    if (shown < plainLen(full)) raf = requestAnimationFrame(tick); else finished();
  }
  function finished() {
    const L = lines[i];
    if (L.ask) { if (!choice) choice = choiceBox(L.ask, { x: 1280 - 140 - 300, y: null, bottom: (o.bottom ?? 26) + 150, onPick: k => { const c = choice; choice = null; c.el.remove(); end(k); }, onBack: () => { const c = choice; choice = null; c.el.remove(); end(-1); } }); }
    else if (!(o.keep && i === lines.length - 1)) more.hidden = false;
  }
  function end(v) { if (i < lines.length - 1) { i++; show(); } else { if (!o.keep) { dead = true; el.remove(); } res(v); if (o.onDone) o.onDone(v); } }
  function key(k) {
    if (choice) return choice.key(k);
    if (k !== "a") return false;
    if (shown < plainLen(full)) { shown = plainLen(full); body.innerHTML = rich(full); cancelAnimationFrame(raf); finished(); return true; }
    if (lines[i].ask) return true;
    if (o.keep && i === lines.length - 1) { res(); if (o.onDone) o.onDone(); return true; }
    end(); return true;
  }
  el.addEventListener("click", ev => { ev.stopPropagation(); key("a"); });
  show();
  const w = { el, key, done, tap() { if (!choice) key("a"); }, kill() { dead = true; cancelAnimationFrame(raf); el.remove(); if (choice) choice.el.remove(); } };
  LIVE_TEXT.push(w);
  return w;
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
    const r = t.goods ? h("div", "g-goodswrap", goodsRow(t.goods, t.right)) : h("div", "g-item" + (t.off ? " off" : ""), `${t.pic ? pic(t.pic, 36, "margin:-6px 0 -6px -8px") : ""}<span>${rich(T(t.label))}</span>${t.right != null ? `<span class="r">${t.right}</span>` : ""}`);
    r.addEventListener("mouseenter", () => { sel = i; paint(); if (o.onMove) o.onMove(sel); });
    r.addEventListener("click", e => { e.stopPropagation(); sel = i; paint(); pick(); });
    list.appendChild(r); return r;
  });
  function paint() { rows.forEach((r, i) => { r.classList.toggle("on", i === sel); const gd = r.querySelector(".g-goods"); if (gd) gd.classList.toggle("on", i === sel); }); }
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
const ESC_TO = { splash: "title", prof: "saves", rival: "name", ready: "rival", talk: "hud", itemget: "hud", badgeget: "hud", wait: "menu", mart: "hud", center: "hud", trade: "hud", gift: "hud", starter: "hud", levelup: "hud", evolve: "hud", victory: "hud", whiteout: "center" };
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
  let handled = cur && cur.key ? cur.key(k) : false;
  // ESC always gets you out: where a screen doesn't use B itself (a conversation, a gift, the intro), it goes back a step.
  if (handled === false && k === "b" && ESC_TO[curId]) { go(ESC_TO[curId]); handled = true; }
  if (handled !== false || k === "tab" || k === "a" || k.length < 6) e.preventDefault();
});

// ---------- the list of screens beside the stage, and the notes for each ----------
// (0.61, John's 17:58) The screens narrowed to what matches the search (its name, its group, its notes).
let flowQuery = "";
function renderFlows() {
  const nav = $("#flows"); nav.innerHTML = "";
  let g = null, n = 0;
  const sel = $("#flowSel"); sel.innerHTML = "";
  let og = null;
  const box = $("#flowSearch");
  if (box && !box.oninput) {
    box.oninput = () => { flowQuery = box.value; renderFlows(); };
    box.onkeydown = e => { if (e.key === "Escape") { box.value = ""; flowQuery = ""; renderFlows(); } if (e.key === "Enter") { const b = nav.querySelector("button"); if (b) b.click(); } };
  }
  const q = flowQuery.trim().toLowerCase().split(/\s+/).filter(Boolean);
  for (const s of SCREENS) {
    n++;
    const text = (s.name + " " + s.group + " " + s.id + " " + (s.note || "") + " " + (s.notes || "")).toLowerCase();
    if (q.length && !q.every(w => text.includes(w)) && s !== curDef) continue;
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
