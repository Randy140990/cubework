// ---------- a screen's controls: whatever's on top gets the keys; text boxes and choices in turn ----------
let GEN = 0;
function ctl() {
  const stack = [], gen = ++GEN;
  const C = {
    alive: () => gen === GEN,
    push(w) { stack.push(w); return w; }, pop(w) { const i = stack.lastIndexOf(w); if (i >= 0) stack.splice(i, 1); },
    key(k) { const w = stack[stack.length - 1]; return w && w.key ? w.key(k) : false; },
    say(lines, o = {}) { const tb = textBox(lines, Object.assign({}, C.sayAt || {}, o)); C.push(tb); return tb.done.then(v => { C.pop(tb); return v; }); },
    choose(items, o = {}) { return new Promise(res => { const cb = choiceBox(items, Object.assign({}, o, { onPick: i => { C.pop(cb); cb.el.remove(); res(i); }, onBack: o.back === false ? null : () => { C.pop(cb); cb.el.remove(); res(-1); } })); C.push(cb); }); },
    wait: ms => new Promise(r => later(ms, r)),
  };
  return C;
}
function header(icon, text, right) {
  const e = box(28, 22, null, null, "g-panel soft", `<div style="display:flex;align-items:center;gap:12px;padding:8px 22px 8px 12px"><span style="width:42px;height:42px;display:grid;place-items:center">${icon || ""}</span><span class="g-title" style="font-size:28px">${esc(text)}</span>${right ? `<span style="margin-left:18px" class="g-label">${right}</span>` : ""}</div>`);
  e.style.borderRadius = "999px"; e.querySelector("svg") && (e.querySelector("svg").style.width = "40px");
  return e;
}
const SKY = "radial-gradient(ellipse at 30% 20%, #F6FFFB 0%, #BFEFE0 38%, #7FD3C2 70%, #3F9C8A 100%)";
const DREAM = "radial-gradient(circle at 50% 42%, #FFFFFF 0%, #E3F8FA 20%, #9EDCEB 48%, #4C9CC7 78%, #2E5F96 100%)";
const NIGHT = "radial-gradient(ellipse at 50% 40%, #1D5A52 0%, #123C38 45%, #071816 100%)";

// ===================== Start =====================
screen("splash", "Start", "Opening", {
  what: "The first thing you see: just the studio's name, then the title.",
  mod: "IntroScreen: 'INSID3 TH3 CUB3, presents', no star.",
  make() {
    backdrop("#000");
    const e = box(0, 0, 1280, 720, "", `<div style="position:absolute;inset:0;display:grid;place-content:center;text-align:center;gap:10px">
      <div class="g-title" style="font-size:64px;letter-spacing:10px;color:#F4F6FF">INSID3 TH3 CUB3</div><div style="font-size:24px;letter-spacing:6px;color:#A9D4C6;font-weight:700">presents</div></div>`);
    e.style.opacity = 0; e.style.transition = "opacity .9s ease";
    later(80, () => e.style.opacity = 1); later(2300, () => e.style.opacity = 0); later(3300, () => go("title"));
    return { key: k => { if (k === "a") go("title"); return true; } };
  }
});
screen("title", "Start", "Title screen", {
  scene: "model", sceneOpts: () => ({ sid: "pikachu", ox: -1.6, stand: false, zoom: 1.15 }),
  what: "The title: Retromon's own logo, a creature turning beside it, and PRESS START.",
  tags: ["New look"], mod: "IntroScreen shows RETROMON and PRESS START over the world.",
  make() {
    backdrop(SKY);
    add(h("div", "", `<div style="position:absolute;inset:0;background:repeating-conic-gradient(from 0deg at 72% 46%, rgba(255,255,255,.18) 0 6deg, transparent 6deg 18deg)"></div>`));
    box(70, 150, 660, null, "", `<div class="g-title" style="font-size:132px;line-height:.95;letter-spacing:4px;color:#FFD45A;-webkit-text-stroke:6px #26304A;paint-order:stroke fill;text-shadow:0 10px 0 rgba(0,0,0,.25);transform:rotate(-4deg)">RETRO<br>MON</div>
      <div style="display:inline-block;margin:18px 0 0 40px;padding:6px 26px;border-radius:999px;background:#E5525C;color:#fff;font-family:Fredoka;font-weight:600;font-size:34px;letter-spacing:10px;box-shadow:0 6px 0 #9E3433">KANTO</div>`);
    box(0, 600, 1280, null, "", `<div style="text-align:center"><span class="g-title g-blink" style="display:inline-block;padding:8px 34px;border-radius:999px;background:#FFF4D8;color:#26304A;font-size:30px;letter-spacing:6px;box-shadow:0 6px 0 rgba(0,0,0,.18)">PRESS START</span></div>`);
    box(28, 676, 400, null, "", `<div style="font-weight:800;font-size:15px;color:#123C38;opacity:.8">INSID3 TH3 CUB3</div>`);
    box(1080, 676, 180, null, "", `<div style="text-align:right;font-weight:800;font-size:15px;color:#123C38;opacity:.8">Version 0.21</div>`);
    return { key: k => { if (k === "a") go("language"); return true; } };
  }
});
const LANGS = [["English", "English"], ["Español", "Spanish"], ["Français", "French"], ["Deutsch", "German"], ["Italiano", "Italian"], ["日本語", "Japanese"], ["한국어", "Korean"], ["简体中文", "Chinese, simplified"], ["繁體中文", "Chinese, traditional"]];
screen("language", "Start", "Language", {
  what: "Pick a language the first time you play. You can change it later in OPTIONS.",
  tags: ["New"], try: ["Up and down to pick, A to choose."],
  make() {
    backdrop(SKY); const C = ctl();
    box(90, 170, 560, 330, "g-panel g-fadein", `<div style="padding:40px 44px;display:grid;gap:18px"><div class="g-title" style="font-size:38px">Which language would you like to play in?</div>
      <div style="font-size:21px;font-weight:700;color:#CFEDE3;line-height:1.5">All the words in the game will be shown in it. You can change it later in OPTIONS.</div>
      <div class="g-label" style="margin-top:6px">Chosen: <span id="lname" style="color:#FFD45A">${esc(G.lang)}</span></div></div>`);
    const cb = choiceBox(LANGS.map(([n, e]) => ({ label: n, right: `<span style="font-size:15px;opacity:.65">${esc(e)}</span>` })), { x: 760, y: 70, w: 420, sel: Math.max(0, LANGS.findIndex(l => l[0] === G.lang)),
      onMove: i => { $("#lname").textContent = LANGS[i][0]; }, onPick: i => { G.lang = LANGS[i][0]; go("saves"); }, onBack: () => go("title") });
    C.push(cb); hints([["A", "Choose"], ["B", "Back"]]);
    return { key: k => C.key(k) };
  }
});
screen("saves", "Start", "Continue or new game", {
  scene: "creator", sceneOpts: () => ({ side: -1.6 }),
  what: "Your saves as cards: who, where, how far. Pick one to carry on, or an empty slot to start again.",
  mod: "IntroScreen's save slots: rounded cards with a picture, a SLOT pill and DELETE.",
  try: ["Up and down between slots, A to load or start, B for the title."],
  make() {
    backdrop(SKY); const C = ctl();
    const slots = [{ used: true }, {}, {}];
    let sel = 0;
    const wrap = box(70, 90, 640, null, "", "");
    const draw = () => {
      wrap.innerHTML = slots.map((s, i) => s.used ? `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:18px 22px;display:grid;grid-template-columns:150px 1fr;gap:20px;${i === sel ? "border-color:#FFF4D8;box-shadow:0 0 0 4px rgba(255,244,216,.4),0 14px 36px rgba(0,0,0,.4)" : ""}">
          <div style="border-radius:16px;background:linear-gradient(180deg,#9ED8F2,#BFEFE0 55%,#6DB257 56%,#5FA24C);position:relative;overflow:hidden">
            <div style="position:absolute;left:18px;bottom:22px;width:56px;height:40px;background:#F2E8D2;border-radius:4px"></div><div style="position:absolute;left:12px;bottom:58px;width:68px;height:22px;background:#C9493F;border-radius:6px 6px 0 0"></div>
            <div style="position:absolute;right:18px;bottom:16px;width:22px;height:44px;border-radius:10px;background:${G.look.top}"></div><div style="position:absolute;right:20px;bottom:58px;width:18px;height:18px;border-radius:6px;background:${G.look.skin}"></div></div>
          <div style="display:grid;gap:6px"><div style="display:flex;align-items:center;gap:12px"><span style="padding:2px 12px;border-radius:999px;background:#FFF4D8;color:#26304A;font-weight:900;font-size:14px;letter-spacing:1px">SLOT ${i + 1}</span><span class="g-title" style="font-size:30px">${esc(G.name)}</span></div>
            <div style="display:grid;grid-template-columns:auto 1fr;gap:2px 18px;font-size:19px;font-weight:800"><span class="g-label">Place</span><span>${esc(G.place)}</span><span class="g-label">Pokédex</span><span>${G.caught}</span><span class="g-label">Badges</span><span>${G.badges}</span><span class="g-label">Play time</span><span>${G.time}</span></div></div>
          <span style="position:absolute;right:16px;top:14px;padding:2px 12px;border-radius:999px;border:2px solid rgba(229,82,92,.7);color:#FFB3B8;font-weight:900;font-size:13px">DELETE</span></div>` : `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:22px 24px;display:flex;align-items:center;gap:16px;${i === sel ? "border-color:#FFF4D8;box-shadow:0 0 0 4px rgba(255,244,216,.4),0 14px 36px rgba(0,0,0,.4)" : "opacity:.85"}">
          <span style="padding:2px 12px;border-radius:999px;background:rgba(255,244,216,.2);font-weight:900;font-size:14px;letter-spacing:1px">SLOT ${i + 1}</span><span class="g-title" style="font-size:28px">NEW GAME</span><span style="margin-left:auto;font-size:17px;color:#A9D4C6;font-weight:800">Empty</span></div>`).join("") +
        `<div class="g-item${sel === 3 ? " on" : ""}" style="display:inline-flex;margin-top:4px">${ICON.cog.replace("<svg", '<svg style="width:26px;height:26px"')}<span>OPTIONS</span></div>`;
      [...wrap.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); pick(); });
    };
    const pick = () => { if (sel === 0) go("loading"); else if (sel === 3) go("options"); else go("prof"); };
    draw();
    hints([["A", "Choose"], ["B", "Back"]]);
    return { key: k => { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : 3)) % 4; draw(); return true; } if (k === "a") { pick(); return true; } if (k === "b") { go("title"); return true; } return false; } };
  }
});
const OPTS = [
  ["TEXT SPEED", "textSpeed", ["SLOW", "MID", "FAST"], "How quickly words appear in text boxes."],
  ["MUSIC", "music", ["OFF", "LOW", "MID", "HIGH", "MAX"], "How loud the music plays."],
  ["SOUND", "sound", ["OFF", "LOW", "MID", "HIGH", "MAX"], "How loud cries, steps and menu sounds are."],
  ["POKéDEX VOICE", "voice", ["OFF", "ON"], "Whether the POKéDEX reads its entries out loud."],
  ["NIGHTS", "nights", ["DARK", "SOFT", "BRIGHT"], "How dark nights get outside."],
  ["WILD POKéMON", "wild", ["HIDDEN", "SEEN"], "Whether wild POKéMON show in the grass, or jump out at you."],
  ["BATTLE EFFECTS", "effects", ["ON", "OFF"], "Whether moves play their animations in battle."],
  ["BATTLE STYLE", "style", ["SWITCH", "SET"], "SWITCH asks if you want to change POKéMON when a trainer's sends out the next one."],
];
screen("options", "Start", "Options", {
  what: "Settings, a row each: left and right to change. What the row does is written at the top.",
  mod: "MenuScreen's OPTION: TEXT SPEED, MUSIC, POKéDEX VOICE, NIGHTS, WILD POKéMON.",
  tags: ["New: sound, battle effects, battle style, language, controls"],
  make(arg) {
    backdrop(SKY); let sel = 0;
    const desc = box(170, 40, 940, 96, "g-panel", ""); desc.style.padding = "22px 30px"; desc.style.fontSize = "22px"; desc.style.fontWeight = "800";
    const P = box(170, 160, 940, 470, "g-panel", ""); P.style.padding = "18px";
    const rows = OPTS.concat([["LANGUAGE", null, null, "The language the game's words are shown in."], ["CONTROLS", null, null, "See every key: what it does in game, and what's turned off."]]);
    const draw = () => {
      desc.innerHTML = esc(rows[sel][3]);
      P.innerHTML = rows.map(([l, k, vals], i) => {
        const v = k ? vals[G.opts[k]] : l === "LANGUAGE" ? G.lang : "";
        return `<div class="g-item${i === sel ? " on" : ""}" style="min-height:41px;padding-top:5px;padding-bottom:5px"><span style="width:280px">${esc(l)}</span><span class="r" style="display:flex;align-items:center;gap:14px">${k ? `<span style="opacity:.6">&#9664;</span><span style="min-width:110px;text-align:center">${esc(v)}</span><span style="opacity:.6">&#9654;</span>` : `<span>${esc(v)}</span><span style="opacity:.6">&#9654;</span>`}</span></div>`;
      }).join("");
      [...P.children].forEach((c, i) => c.onclick = () => { sel = i; act("a"); });
    };
    const act = k => {
      const [l, key, vals] = rows[sel];
      if (key && (k === "left" || k === "right" || k === "a")) { G.opts[key] = (G.opts[key] + (k === "left" ? vals.length - 1 : 1)) % vals.length; draw(); return true; }
      if (l === "LANGUAGE" && k === "a") { go("language"); return true; }
      if (l === "CONTROLS" && k === "a") { go("keymap", arg); return true; }
      return false;
    };
    draw(); hints([["A", "Change"], ["B", "Back"]]);
    const back = () => go(arg && arg.from === "menu" ? "menu" : "saves", { sel: 8 });
    return { key: k => { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : rows.length - 1)) % rows.length; draw(); return true; } if (k === "b") { back(); return true; } return act(k); } };
  }
});
screen("keys", "Start", "Keys check", {
  what: "Before the first game: shows the keys you'll use, with a way to see the full key map.",
  tags: ["New"],
  make() {
    backdrop(SKY); const C = ctl();
    box(180, 110, 640, 420, "g-panel g-fadein", `<div style="padding:34px 40px;display:grid;gap:20px"><div class="g-title" style="font-size:36px">These are your keys</div>
      <div style="display:grid;grid-template-columns:auto 1fr;gap:14px 22px;align-items:center;font-size:22px;font-weight:800">
        <span><span class="g-key">W</span> <span class="g-key">A</span> <span class="g-key">S</span> <span class="g-key">D</span></span><span>Walk</span>
        <span><span class="g-key" style="min-width:120px">SPACE</span></span><span>Talk, look, choose</span>
        <span><span class="g-key" style="min-width:80px">ENTER</span></span><span>Open the menu</span>
        <span><span class="g-key" style="min-width:64px">TAB</span></span><span>Speed up</span>
        <span><span class="g-key">Q</span> <span class="g-key">X</span></span><span>Back</span></div></div>`);
    C.choose(["Use these keys", "Show the key map", "Back"], { x: 860, y: 300, w: 300 }).then(i => go(i === 1 ? "keymap" : i === 2 || i < 0 ? "saves" : "prof"));
    return { key: k => C.key(k) };
  }
});

// ===================== A new game =====================
screen("prof", "A new game", "The professor", {
  scene: "prof", sceneOpts: () => ({ mode: "intro" }),
  what: "The professor welcomes you, with Retromon creatures running round him in the light.",
  tags: ["Retromon creatures"], mod: "IntroScreen: Prof. Oak talks the whole time he's speaking.",
  try: ["A to go on."],
  make() {
    backdrop(DREAM);
    add(h("div", "", `<div style="position:absolute;inset:0;background:radial-gradient(circle at 20% 30%,rgba(255,255,255,.6) 0 2px,transparent 3px),radial-gradient(circle at 80% 20%,rgba(255,255,255,.7) 0 2px,transparent 3px),radial-gradient(circle at 70% 70%,rgba(255,255,255,.5) 0 2px,transparent 3px),radial-gradient(circle at 30% 80%,rgba(255,255,255,.6) 0 2px,transparent 3px);background-size:240px 200px;animation:gbob 4s ease-in-out infinite"></div>`));
    const C = ctl(), S = SC.prof;
    S.talking = true;
    C.say([
      { who: "PROF. OAK", text: "Ah, there you are! Welcome. I'm OAK. Most people round here call me the POKéMON PROF." },
      { who: "PROF. OAK", text: "This world is full of creatures we call **POKéMON**. You'll find them in the grass, the sea, the caves, even in town." },
      { who: "PROF. OAK", text: "Some people keep them as friends. Some raise them for battles. Me? I study them." },
      { who: "PROF. OAK", text: "But that's enough about me. First, tell me a little about yourself." },
    ]).then(() => { S.talking = false; if (C.alive()) go("creator"); });
    return { key: k => C.key(k) };
  }
});
const SKINS = ["#F6D7C3", "#EBC3A0", "#E0AC86", "#C98B62", "#A86F4C", "#8A5638", "#6A3F28", "#4A2B1C"];
const HAIRS = ["#1E1A18", "#3B2618", "#6B4426", "#8E3B22", "#C8622E", "#E3B65C", "#EDE2C4", "#A8ACB4", "#3F6FB0", "#E07AA8"];
const EYES = ["#3B2A1C", "#6B4426", "#8A7A3C", "#3E8A4E", "#3F7FD0", "#7A8A99", "#C98B22", "#7A5AB8"];
const CLOTH = ["#D9524E", "#E9B43C", "#5DAE52", "#3F7FD0", "#6E59B8", "#E07AA8", "#2B2522", "#F4F6F5", "#2F3F66", "#8E5B36"];
const STYLES = [["short", "Short"], ["spiky", "Spiky"], ["swept", "Swept"], ["long", "Long"], ["bob", "Bob"], ["pony", "Ponytail"], ["bun", "Bun"], ["buzz", "Buzz"]];
const BROWS = [["fine", "Fine"], ["thick", "Thick"], ["arched", "Arched"], ["straight", "Straight"], ["bushy", "Bushy"], ["none", "None"]];
// Rows of the character maker, shared with CLOTHES.
function makerRows(full) {
  const R = [];
  if (full) {
    R.push({ label: "BODY", chips: [["boy", "Boy"], ["girl", "Girl"]], get: () => G.look.body, set: v => { G.look.body = v; } });
    R.push({ label: "SKIN", swatches: SKINS, get: () => G.look.skin, set: v => G.look.skin = v });
    R.push({ label: "HAIR", chips: STYLES, get: () => G.look.hairStyle, set: v => G.look.hairStyle = v });
    R.push({ label: "HAIR COLOUR", swatches: HAIRS, get: () => G.look.hair, set: v => G.look.hair = v });
    R.push({ label: "EYES", swatches: EYES, get: () => G.look.eyes, set: v => G.look.eyes = v });
    R.push({ label: "BROWS", chips: BROWS, get: () => G.look.brows, set: v => G.look.brows = v });
  }
  R.push({ label: "CAP", swatches: [null].concat(CLOTH), get: () => G.look.cap, set: v => G.look.cap = v });
  R.push({ label: "TOP", swatches: CLOTH, get: () => G.look.top, set: v => G.look.top = v });
  R.push({ label: "BOTTOMS", swatches: CLOTH, get: () => G.look.legs, set: v => G.look.legs = v, extra: { label: "Skirt", get: () => G.look.skirt, set: v => G.look.skirt = v } });
  R.push({ label: "SHOES", swatches: CLOTH, get: () => G.look.shoes, set: v => G.look.shoes = v });
  R.push({ label: "BAG", swatches: [null].concat(CLOTH), get: () => G.look.bag, set: v => G.look.bag = v });
  return R;
}
function maker(C, rows, o) {
  let sel = 0;
  const P = box(30, 24, 700, null, "g-panel", ""); P.style.padding = "10px 14px";
  const opts = r => r.swatches ? r.swatches.map(v => [v, v]) : r.chips;
  const idxOf = r => Math.max(0, opts(r).findIndex(([v]) => v === r.get()));
  const draw = () => {
    P.innerHTML = rows.map((r, i) => `<div style="display:grid;grid-template-columns:132px 1fr;align-items:center;gap:10px;padding:4px 10px;border-radius:16px;${i === sel ? "background:rgba(255,244,216,.12);box-shadow:inset 0 0 0 2px rgba(255,244,216,.5)" : ""}">
      <span class="g-label" style="font-size:12px;${i === sel ? "color:#FFF4D8" : ""}">${r.label}</span>
      <span style="display:flex;flex-wrap:wrap;gap:${r.swatches ? 8 : 6}px;align-items:center">${opts(r).map(([v, l], j) => r.swatches
        ? `<span class="g-swatch${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="width:31px;height:31px;border-width:2.5px;background:${v || "repeating-linear-gradient(45deg,#3a5a55 0 5px,#2a4743 5px 10px)"}"></span>`
        : `<span class="g-chip${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="font-size:13.5px;padding:2px 9px">${esc(l)}</span>`).join("")}
        ${r.extra ? `<span class="g-chip${r.extra.get() ? " on" : ""}" data-x="${i}" style="font-size:13.5px;padding:2px 9px;margin-left:4px">${esc(r.extra.label)}</span>` : ""}</span></div>`).join("") +
      `<div style="display:flex;gap:12px;margin-top:6px;padding:0 10px"><span class="g-item${sel === rows.length ? " on" : ""}" data-done="1">${esc(o.done || "DONE")}</span><span class="g-item${sel === rows.length + 1 ? " on" : ""}" data-rand="1">MIX IT UP</span></div>`;
    P.querySelectorAll("[data-r]").forEach(e => e.onclick = () => { const r = rows[+e.dataset.r]; sel = +e.dataset.r; r.set(opts(r)[+e.dataset.j][0]); changed(); });
    P.querySelectorAll("[data-x]").forEach(e => e.onclick = () => { const r = rows[+e.dataset.x]; r.extra.set(!r.extra.get()); changed(); });
    P.querySelector("[data-done]").onclick = () => o.onDone(); P.querySelector("[data-rand]").onclick = () => { mix(); };
  };
  const changed = () => { draw(); if (SC.creator) SC.creator.refresh(); };
  const mix = () => { for (const r of rows) { const os = opts(r); r.set(os[Math.floor(Math.random() * os.length)][0]); } changed(); };
  draw();
  const w = { key(k) {
    const n = rows.length + 2;
    if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : n - 1)) % n; draw(); return true; }
    if (sel >= rows.length) { if (k === "a") { if (sel === rows.length) o.onDone(); else mix(); return true; } if (k === "left" || k === "right") { sel = sel === rows.length ? rows.length + 1 : rows.length; draw(); return true; } }
    const r = rows[sel];
    if (r && (k === "left" || k === "right")) { const os = opts(r); r.set(os[(idxOf(r) + (k === "right" ? 1 : os.length - 1)) % os.length][0]); changed(); return true; }
    if (r && k === "a" && r.extra) { r.extra.set(!r.extra.get()); changed(); return true; }
    if (k === "b" && o.onBack) { o.onBack(); return true; }
    return false;
  } };
  C.push(w);
  return w;
}
screen("creator", "A new game", "Character maker", {
  scene: "creator", sceneOpts: () => ({ side: -1.15 }),
  what: "Make yourself: boy or girl, skin tone, hair style and colour, eyes, brows, then clothes. Every part is a rounded block, as Shards of Arcadia's maker lets you choose hair and brows.",
  tags: ["New", "Rounded figure"], mod: "Today: the boy or girl and a few clothes (CLOTHES on the menu).",
  try: ["Up and down between rows, left and right to change, A on DONE.", "MIX IT UP picks at random."],
  make() {
    backdrop("radial-gradient(ellipse at 70% 45%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)");
    const C = ctl();
    box(760, 640, 480, null, "", `<div class="g-title" style="text-align:center;font-size:24px;color:#123C38">What do you look like?</div>`);
    maker(C, makerRows(true), { done: "THAT'S ME", onDone: () => go("name", { who: "you" }), onBack: () => go("prof") });
    hints([["A", "Choose"], ["B", "Back"]]);
    return { key: k => C.key(k) };
  }
});
const KEYS_AZ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").concat(["-", "'", ".", " ", "0", "1", "2", "3", "4", "5"]);
function nameEntry(C, o) {
  let name = o.start || "", sel = 0, presetsOn = !!(o.presets && o.presets.length);
  const P = box(240, 150, 800, 430, "g-panel g-pop", ""); P.style.padding = "24px 30px";
  const all = KEYS_AZ.length + 2;
  const draw = () => {
    P.innerHTML = `<div style="display:flex;align-items:center;gap:18px;margin-bottom:18px"><span class="g-label">${esc(o.label)}</span>
      <span style="flex:1;display:flex;gap:8px">${Array.from({ length: 7 }, (_, i) => `<span style="flex:1;max-width:56px;height:56px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:34px;background:${i < name.length ? "#FFF4D8" : "rgba(255,255,255,.08)"};color:#26304A;${i === name.length ? "box-shadow:inset 0 0 0 3px #8FF5C0" : ""}">${esc(name[i] || "")}</span>`).join("")}</span></div>
      <div style="display:grid;grid-template-columns:repeat(9,1fr);gap:8px">${KEYS_AZ.map((c, i) => `<span data-i="${i}" style="height:50px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:26px;cursor:pointer;${i === sel ? "background:#FFF4D8;color:#26304A" : "background:rgba(255,255,255,.07)"}">${c === " " ? "&#9251;" : esc(c)}</span>`).join("")}
      <span data-i="${KEYS_AZ.length}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length ? "background:#FFF4D8;color:#26304A" : "background:rgba(255,255,255,.07)"}">DELETE</span>
      <span data-i="${KEYS_AZ.length + 1}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length + 1 ? "background:#41BF84;color:#fff" : "background:rgba(65,191,132,.25)"}">OK</span></div>`;
    P.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { sel = +e.dataset.i; press(); });
  };
  const press = () => { if (sel < KEYS_AZ.length) { if (name.length < 7) name += KEYS_AZ[sel]; } else if (sel === KEYS_AZ.length) name = name.slice(0, -1); else if (name.trim()) { o.onDone(name.trim()); return; } draw(); };
  draw();
  const w = {
    key(k) { if (k === "a") { press(); return true; } if (k === "b") { if (name) { name = name.slice(0, -1); draw(); } else if (o.onBack) o.onBack(); return true; }
      const n = sel; sel = gridMove(sel, all, 9, k); if (sel !== n) draw(); return true; },
    raw(e) { if (/^Key[A-Z]$/.test(e.code) && !["KeyW", "KeyA", "KeyS", "KeyD", "KeyE", "KeyQ", "KeyX", "KeyZ"].includes(e.code) || (e.shiftKey && /^Key[A-Z]$/.test(e.code))) { if (name.length < 7) name += e.code.slice(3); draw(); return true; }
      if (e.code === "Backspace") { name = name.slice(0, -1); draw(); return true; } if (e.code === "Enter" && name.trim()) { o.onDone(name.trim()); return true; } return false; }
  };
  C.push(w);
  return w;
}
screen("name", "A new game", "Your name", {
  scene: "creator", sceneOpts: () => ({ side: 0 }),
  what: "Type your name, up to 7 letters, or pick one of the ready names.",
  mod: "IntroScreen: YOUR NAME, with ready names and NEW NAME.",
  try: ["Type on your keyboard (Shift for W, A, S, D, E, Q, X and Z), or move round the letters and press A.", "OK when you're done."],
  make(arg) {
    backdrop("radial-gradient(ellipse at 50% 45%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)");
    const C = ctl(), girl = G.look.body === "girl";
    const presets = girl ? ["LEAF", "LYRA", "KRIS"] : ["RED", "KRIS", "ETHAN"];
    C.choose(["NEW NAME"].concat(presets), { x: 980, y: 190, w: 250, back: false }).then(i => {
      if (i > 0) { G.name = presets[i - 1]; go("rival"); return; }
      const w = nameEntry(C, { label: "YOUR NAME", start: "", onDone: n => { G.name = n.toUpperCase(); go("rival"); }, onBack: () => go("creator") });
      cur.raw = e => w.raw(e);
    });
    box(0, 70, 1280, null, "", `<div class="g-title" style="text-align:center;font-size:30px;color:#123C38">And what's your name?</div>`);
    hints([["A", "Choose"], ["B", "Back"]]);
    return { key: k => C.key(k) };
  }
});
screen("rival", "A new game", "Your rival's name", {
  scene: "prof", sceneOpts: () => ({ mode: "rival" }),
  what: "The professor's grandson, your rival since you were small. Pick or type his name.",
  mod: "IntroScreen: YOUR RIVAL'S NAME.",
  make() {
    backdrop(DREAM); const C = ctl(), S = SC.prof;
    (async () => {
      S.talking = true;
      await C.say([{ who: "PROF. OAK", text: `So you're **${G.name}**! A fine name.` }, { who: "PROF. OAK", text: "And this is my grandson. You two have been racing each other since you could walk." }, { who: "PROF. OAK", text: "Now, what was his name again...?" }], { keep: true });
      S.talking = false;
      const names = ["BLUE", "GARY", "JOHN"];
      const i = await C.choose(["NEW NAME"].concat(names), { x: 980, y: 140, w: 250, back: false });
      if (!C.alive()) return;
      const done = async n => { G.rival = n; uiRoot.querySelectorAll(".g-text").forEach(e => e.remove()); S.talking = true; await C.say([{ who: "PROF. OAK", text: `That's right! I remember now. His name is **${G.rival}**!` }]); S.talking = false; if (C.alive()) go("ready"); };
      if (i > 0) done(names[i - 1]);
      else { uiRoot.querySelectorAll(".g-text").forEach(e => e.remove()); const w = nameEntry(C, { label: "YOUR RIVAL'S NAME", onDone: n => { C.pop(w); uiRoot.querySelectorAll(".g-panel.g-pop").forEach(e => e.remove()); done(n.toUpperCase()); } }); cur.raw = e => w.raw(e); }
    })();
    return { key: k => C.key(k) };
  }
});
screen("ready", "A new game", "Off you go", {
  scene: "prof", sceneOpts: () => ({ mode: "you" }),
  what: "You, as you made yourself, beside the professor, before the game loads.",
  make() {
    backdrop(DREAM); const C = ctl(), S = SC.prof;
    S.talking = true; S.youWave = 0;
    C.say([{ who: "PROF. OAK", text: `**${G.name}**, your own adventure starts today.` }, { who: "PROF. OAK", text: "There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab." }, { who: "PROF. OAK", text: "Off you go, then!" }])
      .then(() => { S.talking = false; S.youWave = 1; return C.wait(900); }).then(() => { if (C.alive()) go("loading"); });
    return { key: k => C.key(k) };
  }
});
const TIPS = ["Press TAB to speed the game up. Press it again to go back.", "Creatures 1 block tall or less can ride on your shoulder.", "Talk to people with SPACE. Some have things to give you.",
  "Your POKéMON follow you around. Talk to them too.", "In the bag, the KEY ITEMS pocket is its lid.", "Nights are darker outside. Change it under OPTIONS, NIGHTS."];
screen("loading", "A new game", "Loading", {
  what: "While the world loads: a ball spinning, the place you're going, a tip.",
  tags: ["New"],
  make() {
    backdrop(NIGHT);
    const tip = TIPS[Math.floor(Math.random() * TIPS.length)];
    box(0, 0, 1280, 720, "", `<div style="position:absolute;left:50%;top:44%;transform:translate(-50%,-50%);display:grid;justify-items:center;gap:22px">
      <div style="width:110px;height:110px;animation:spin 1.1s linear infinite">${ICON.ball}</div><div class="g-title" style="font-size:40px;letter-spacing:4px">${esc(G.place)}</div>
      <div class="g-hp" style="width:520px;height:18px"><i id="ld" style="width:0%;background:linear-gradient(90deg,#4FB896,#8FF5C0);transition:width .2s"></i></div></div>
      <div class="g-panel soft" style="position:absolute;left:50%;bottom:60px;transform:translateX(-50%);padding:16px 30px;font-size:20px;font-weight:800;white-space:nowrap"><span class="g-label" style="margin-right:12px">Tip</span>${esc(tip)}</div>
      <style>@keyframes spin{to{transform:rotate(360deg)}}</style>`);
    let p = 0; const t = setInterval(() => { p = Math.min(100, p + 7 + Math.random() * 9); const e = $("#ld"); if (e) e.style.width = p + "%"; if (p >= 100) { clearInterval(t); later(350, () => go("hud")); } }, 160); timers.push(t);
    return { key: k => true };
  }
});
