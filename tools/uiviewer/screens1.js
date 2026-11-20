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
  const e = box(28, 22, null, null, "g-panel soft", `<div style="display:flex;align-items:center;gap:12px;padding:8px 22px 8px 12px"><span style="width:42px;height:42px;display:grid;place-items:center">${icon || ""}</span><span class="g-title" style="font-size:28px">${esc(T(text))}</span>${right ? `<span style="margin-left:18px" class="g-label">${right}</span>` : ""}</div>`);
  e.style.borderRadius = "999px"; e.querySelector("svg") && (e.querySelector("svg").style.width = "40px");
  return e;
}
// Each screen on its own bright backdrop, as Let's Go's are: sky, lime, peach, with soft rays from a corner.
const SKY = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #E2F4FF 32%, #A6D8F7 66%, #6AAEE6 100%)";
const LIME = "radial-gradient(ellipse at 25% 15%, #FFFFF2 0%, #F4FAB8 35%, #CDEB6E 70%, #96C73A 100%)";
const PEACH = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #FFEDE0 35%, #FFC9A6 70%, #F2946A 100%)";
const MINT = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #E3FAEF 35%, #A9EBCB 70%, #5CC79A 100%)";
const DREAM = "radial-gradient(circle at 50% 42%, #FFFFFF 0%, #E3F8FA 20%, #9EDCEB 48%, #4C9CC7 78%, #2E5F96 100%)";
const NIGHT = "radial-gradient(ellipse at 50% 40%, #5FA8E8 0%, #2F64B0 50%, #1A3470 100%)";

// ===================== Start =====================
screen("splash", "Start", "Opening", {
  what: "The first thing you see: just the studio's name, then the title.",
  mod: "IntroScreen: 'INSID3 TH3 CUB3, presents', no star.",
  make() {
    backdrop("#000");
    const e = box(0, 0, 1280, 720, "", `<div style="position:absolute;inset:0;display:grid;place-content:center;text-align:center;gap:10px">
      <div class="g-title" style="font-size:64px;letter-spacing:10px;color:#F4F6FF">INSID3 TH3 CUB3</div><div style="font-size:24px;letter-spacing:6px;color:var(--g-muted);font-weight:700">presents</div></div>`);
    e.style.opacity = 0; e.style.transition = "opacity .9s ease";
    later(80, () => e.style.opacity = 1); later(2300, () => e.style.opacity = 0); later(3300, () => go("title"));
    return { key: k => { if (k === "a" || k === "b") go("title"); return true; } };
  }
});
screen("title", "Start", "Title screen", {
  scene: "title", pixel: () => LOOK === "theirs" ? 5 : 0,
  what: "The title, as Let's Go's, smooth (no pixels): RETROMON big at the top left with its own tag, A FAN-MADE ADVENTURE, over a painted meadow: blue sky with the sun's glare top right, hazy trees far off, a big tree on the left and leafy canopy on the right, a path, a rock, long grass swaying and flowers, blurred leaves right at the front. Your partner stands big on the right, looking out at you. Press SPACE, in white words. COBBLEMON look: the old title, the pixel Charizard against the night sky, reading POKéMON.",
  tags: ["New look: Let's Go's title"], mod: "IntroScreen: the pixel Charizard against the night sky, RETROMON, PRESS START. To come: this painted title.",
  make() {
    const night = LOOK === "theirs";
    if (night) {
      backdrop("radial-gradient(ellipse at 50% 30%, #2A3F7A 0%, #142454 45%, #070D26 100%)");
      add(h("div", "", `<div style="position:absolute;inset:0;background:radial-gradient(circle at 12% 18%,#fff 0 1.5px,transparent 2.5px),radial-gradient(circle at 78% 12%,#fff 0 2px,transparent 3px),radial-gradient(circle at 64% 34%,#cfe 0 1.5px,transparent 2.5px),radial-gradient(circle at 30% 42%,#fff 0 1px,transparent 2px),radial-gradient(circle at 88% 46%,#fff 0 1.5px,transparent 2.5px);background-size:320px 260px"></div><div style="position:absolute;right:120px;top:70px;width:90px;height:90px;border-radius:50%;background:radial-gradient(circle at 40% 40%,#FFF8DC,#E8DDB0);box-shadow:0 0 40px rgba(255,248,220,.5)"></div>`));
    } else {
      // As Let's Go's title: a painted meadow in soft light (no pixels): sky and the sun's glare top right, hazy trees far
      // off, a big tree on the left and leafy canopy behind on the right, a sunlit meadow with a path and a rock, long
      // grass and flowers, and blurred leaves right at the front, out of focus. Your partner stands big on the right.
      // Like a film's opening shot: the Minecraft meadow behind, out of focus (its own canvas); the sun in the gap
      // between the two big oaks, its glow spilling down; your partner sharp in front; grass blades close up across the
      // foot of the frame; a soft vignette at the corners.
      backdrop("linear-gradient(180deg,#3E9FE6 0%,#7CC6F2 30%,#CBEBFA 52%,#E9F6E2 60%,#8CC867 70%,#5FA846 100%)");
      if (SC.title && SC.title.back) bgArt.appendChild(SC.title.back.canvas);
      bgAdd("", { background: "radial-gradient(ellipse 34% 46% at 50% -4%, rgba(255,252,236,1) 0%, rgba(255,246,214,.75) 24%, rgba(255,238,196,.25) 52%, transparent 74%)" });
      const blades = (hh, texel, blur, dark, seedN, opac) => { const cv = document.createElement("canvas"); cv.width = 1280 / texel | 0; cv.height = hh / texel | 0; const x = cv.getContext("2d");
        let sd = seedN; const r = () => (sd = (Math.imul(sd, 1103515245) + 12345) >>> 0) / 4294967296;
        const GR = dark ? ["#2F6E2A", "#3A7F31", "#2A5F27", "#46913A"] : ["#4E9E3A", "#5DB345", "#79C653", "#3F8A34", "#8ED65E"];
        for (let i = 0; i < cv.width; i++) { if (r() < 0.12) continue; const bh = (0.25 + Math.pow(r(), 1.4) * 0.75) * cv.height; const c = GR[Math.floor(r() * GR.length)];
          x.fillStyle = c; x.fillRect(i, cv.height - bh, 1, bh); x.fillStyle = "rgba(255,255,220,.35)"; x.fillRect(i, cv.height - bh, 1, 1); }
        Object.assign(cv.style, { position: "absolute", left: "0", top: (720 - hh) + "px", width: "1280px", height: hh + "px", imageRendering: "pixelated", filter: `blur(${blur}px)`, opacity: opac, transformOrigin: "50% 100%" });
        return cv; };
      const g1 = blades(150, 9, 0.6, false, 7, 1), g2 = blades(230, 22, 7, true, 19, 0.95);
      g1.style.animation = "tsway 4.2s ease-in-out infinite alternate"; g2.style.animation = "tsway 5.5s ease-in-out -2s infinite alternate";
      add(h("style", "", "@keyframes tsway{from{transform:skewX(-1.2deg)}to{transform:skewX(1.6deg)}}")); add(g1); add(g2);
      add(h("div", "", "", { position: "absolute", inset: "0", pointerEvents: "none", mixBlendMode: "screen", background: "radial-gradient(ellipse 30% 40% at 52% -6%, rgba(255,240,200,.55), transparent 70%)" }));
      add(h("div", "", "", { position: "absolute", inset: "0", pointerEvents: "none", background: "radial-gradient(ellipse 75% 70% at 50% 42%, transparent 58%, rgba(8,26,24,.42) 100%)" }));
    }
    if (night) box(0, 34, 1280, null, "", `<div style="text-align:center"><img alt="POKéMON" src="${wordmark("POKéMON", { top: [255, 236, 120], bottom: [240, 170, 40], side: [40, 60, 140], line: [12, 18, 48] })}" style="max-width:1080px;image-rendering:pixelated;filter:drop-shadow(0 10px 0 rgba(0,0,0,.18))"></div>`);
    // RETROMON in round, heavy letters on a gentle arch (tools/wordmark.py), with its own edge and shadow.
    // The title as a big game's logo: RETROMON in heavy, leaning letters, lit from above (warm white into gold into
    // ember), on its own soft shadow and glow; A FAN-MADE ADVENTURE in small spaced capitals between two fine rules
    // under it. It rises in slowly.
    else box(78, 64, 760, null, "", `<style>@keyframes tlogo{from{transform:translateY(18px);opacity:0;filter:blur(6px)}to{transform:none;opacity:1;filter:blur(0)}}
      .t-word{font-family:"Archivo Black","Arial Black",Impact,system-ui,sans-serif;font-weight:900;font-size:128px;line-height:.9;letter-spacing:-2px;font-style:italic;transform:skewX(-6deg);transform-origin:0 100%;
        background:linear-gradient(180deg,#FFFDF4 0%,#FFF0C2 30%,#FFC85A 58%,#F2843A 82%,#C9472C 100%);-webkit-background-clip:text;background-clip:text;color:transparent;
        filter:drop-shadow(0 3px 0 rgba(120,44,20,.55)) drop-shadow(0 14px 22px rgba(10,30,30,.45)) drop-shadow(0 0 28px rgba(255,196,110,.35))}
      .t-sub{display:flex;align-items:center;gap:18px;margin:16px 0 0 12px;font-family:Archivo,system-ui,sans-serif;font-weight:700;font-size:19px;letter-spacing:9px;color:#fff;text-shadow:0 2px 10px rgba(0,30,30,.55)}
      .t-sub i{display:block;height:2px;width:70px;background:linear-gradient(90deg,transparent,rgba(255,236,190,.95))}.t-sub i:last-child{background:linear-gradient(90deg,rgba(255,236,190,.95),transparent)}</style>
      <div style="animation:tlogo 1.4s cubic-bezier(.2,.8,.2,1) both"><div class="t-word">RETROMON</div><div class="t-sub"><i></i>${esc(T("A FAN-MADE ADVENTURE"))}<i></i></div></div>`);
    // Which key, said plainly (Let's Go's "Press the A Button"): white words with a soft edge, SPACE in its own key cap.
    if (night) box(0, 612, 1280, null, "", `<div style="text-align:center"><span class="g-title g-blink" style="display:inline-block;padding:8px 34px;border-radius:999px;background:#fff;color:#26304A;font-size:30px;letter-spacing:6px">${esc(T("PRESS START"))}</span></div>`);
    else box(340, 640, 600, null, "", `<style>@keyframes tpulse{0%,100%{opacity:.95}50%{opacity:.4}}</style><div style="text-align:center;animation:tpulse 2.4s ease-in-out infinite;font-family:Archivo,system-ui,sans-serif;font-weight:700;font-size:17px;letter-spacing:7px;color:#fff;text-shadow:0 2px 10px rgba(0,20,20,.7)">${T("Press {0}").toUpperCase().split("{0}").map(esc).join(`<span style="display:inline-grid;place-items:center;vertical-align:middle;height:30px;margin:0 10px;padding:0 14px;border:2px solid rgba(255,255,255,.85);border-radius:8px;font-size:13px;letter-spacing:4px">SPACE</span>`)}</div>`);
    box(28, 684, 400, null, "", `<div style="font-family:Archivo,system-ui,sans-serif;font-weight:700;font-size:12px;letter-spacing:4px;color:#fff;opacity:.8;text-shadow:0 1px 6px rgba(0,0,0,.5)">INSID3 TH3 CUB3</div>`);
    box(1060, 684, 200, null, "", `<div style="text-align:right;font-family:Archivo,system-ui,sans-serif;font-weight:700;font-size:12px;letter-spacing:4px;color:#fff;opacity:.8;text-shadow:0 1px 6px rgba(0,0,0,.5)">VERSION 0.28</div>`);
    return { key: k => { if (k === "a") go("saves"); return true; } };
  }
});
const LANGS = [["English", "English"], ["Español", "Spanish"], ["Français", "French"], ["Deutsch", "German"], ["Italiano", "Italian"], ["日本語", "Japanese"], ["한국어", "Korean"], ["简体中文", "Chinese, simplified"], ["繁體中文", "Chinese, traditional"]];
screen("saves", "Start", "Continue or new game", {
  scene: "creator", sceneOpts: () => ({ side: -1.6 }),
  what: "Your saves as cards: who, where, how far. Pick one to carry on, or an empty slot to start again.",
  mod: "IntroScreen's save slots: rounded cards with a picture, a SLOT pill and DELETE.",
  try: ["Up and down between slots, A to load or start, B for the title."],
  make() {
    swirlBackdrop("sky", { lx: 85, ly: 10 }); const C = ctl();
    const slots = [{ used: true }, {}, {}];
    let sel = 0;
    const wrap = box(70, 90, 640, null, "", "");
    const draw = () => {
      wrap.innerHTML = slots.map((s, i) => s.used ? `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:18px 22px;display:grid;grid-template-columns:150px 1fr;gap:20px;${i === sel ? "box-shadow:0 0 0 5px #FFC93C,0 14px 36px rgba(30,40,84,.3)" : ""}">
          <div style="border-radius:16px;background:linear-gradient(180deg,#9ED8F2,#BFEFE0 55%,#6DB257 56%,#5FA24C);position:relative;overflow:hidden">
            <div style="position:absolute;left:18px;bottom:22px;width:56px;height:40px;background:#F2E8D2;border-radius:4px"></div><div style="position:absolute;left:12px;bottom:58px;width:68px;height:22px;background:#C9493F;border-radius:6px 6px 0 0"></div>
            <div style="position:absolute;right:18px;bottom:16px;width:22px;height:44px;border-radius:4px;background:${hex(colourOf(G.outfit.topc))}"></div><div style="position:absolute;right:20px;bottom:58px;width:18px;height:18px;border-radius:3px;background:${hex(skinRGB(G.outfit.skin))}"></div></div>
          <div style="display:grid;gap:6px"><div style="display:flex;align-items:center;gap:12px"><span style="padding:2px 12px;border-radius:999px;background:linear-gradient(180deg,#FF9473,#E5525C);color:#fff;font-weight:900;font-size:14px;letter-spacing:1px">${esc(T("SLOT {0}", i + 1))}</span><span class="g-title" style="font-size:30px">${esc(G.name)}</span></div>
            <div style="display:grid;grid-template-columns:auto 1fr;gap:2px 18px;font-size:19px;font-weight:800"><span class="g-label">${esc(T("Place"))}</span><span>${esc(G.place)}</span><span class="g-label">${esc(T("Pokédex"))}</span><span>${G.caught}</span><span class="g-label">${esc(T("Badges"))}</span><span>${G.badges}</span><span class="g-label">${esc(T("Play time"))}</span><span>${G.time}</span></div></div>
          <span style="position:absolute;right:16px;top:14px;padding:2px 12px;border-radius:999px;border:2px solid #F3A6AC;color:#D9404A;font-weight:900;font-size:13px">${esc(T("DELETE"))}</span></div>` : `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:22px 24px;display:flex;align-items:center;gap:16px;${i === sel ? "box-shadow:0 0 0 5px #FFC93C,0 14px 36px rgba(30,40,84,.3)" : "opacity:.92"}">
          <span style="padding:2px 12px;border-radius:999px;background:#EEF2FB;color:var(--g-muted);font-weight:900;font-size:14px;letter-spacing:1px">${esc(T("SLOT {0}", i + 1))}</span><span class="g-title" style="font-size:28px">${esc(T("NEW GAME"))}</span><span style="margin-left:auto;font-size:17px;color:var(--g-muted);font-weight:800">${esc(T("Empty"))}</span></div>`).join("") +
        `<div class="g-item${sel === 3 ? " on" : ""}" style="display:inline-flex;margin-top:4px;background:${sel === 3 ? "" : "rgba(255,255,255,.85)"}">${ICON.cog.replace("<svg", '<svg style="width:26px;height:26px"')}<span>${esc(T("OPTIONS"))}</span></div>`;
      [...wrap.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); pick(); });
    };
    const pick = () => { if (sel === 0) go("loading"); else if (sel === 3) go("options"); else go("prof"); };
    draw();
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
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
  tags: ["New: sound, battle effects, battle style, controls"],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 }); let sel = 0;
    const desc = box(170, 40, 940, 96, "g-panel", ""); desc.style.padding = "22px 30px"; desc.style.fontSize = "22px"; desc.style.fontWeight = "800";
    const P = box(170, 160, 940, 470, "g-panel", ""); P.style.padding = "18px";
    const rows = OPTS.concat([["CONTROLS", null, null, "See every key: what it does in game, and what's turned off."]]);
    const draw = () => {
      desc.innerHTML = esc(T(rows[sel][3]));
      P.innerHTML = rows.map(([l, k, vals], i) => {
        const v = k ? T(vals[G.opts[k]]) : "";
        return `<div class="g-item${i === sel ? " on" : ""}" style="min-height:44px;padding-top:5px;padding-bottom:5px;${i === sel ? "" : "border-bottom:1.5px solid #EEF1F8;border-radius:0"}"><span style="width:340px">${esc(T(l))}</span><span class="r" style="display:flex;align-items:center;gap:14px">${k ? `<span style="color:var(--g-red)">&#9664;</span><span style="min-width:120px;text-align:center">${esc(v)}</span><span style="color:var(--g-red)">&#9654;</span>` : `<span style="color:var(--g-red)">&#9654;</span>`}</span></div>`;
      }).join("");
      [...P.children].forEach((c, i) => c.onclick = () => { sel = i; act("a"); });
    };
    const act = k => {
      const [l, key, vals] = rows[sel];
      if (key && (k === "left" || k === "right" || k === "a")) { G.opts[key] = (G.opts[key] + (k === "left" ? vals.length - 1 : 1)) % vals.length; draw(); return true; }
      if (l === "CONTROLS" && k === "a") { go("keymap", arg); return true; }
      return false;
    };
    draw(); hints([["A", "Change"], ["B", "Back"]]);
    const back = () => go(arg && arg.from === "menu" ? "menu" : "saves", { sel: 8 });
    return { key: k => { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : rows.length - 1)) % rows.length; draw(); return true; } if (k === "b") { back(); return true; } return act(k); } };
  }
});
screen("keys", "Start", "Keys check", {
  what: "Before the first game: shows the keys you'll use, with a way to see the full key map. E opens the menu, as Minecraft's inventory key does, and every other mod's keys are turned off while you play.",
  tags: ["New"],
  make() {
    swirlBackdrop("peach", { lx: 80, ly: 0 }); const C = ctl();
    box(180, 100, 640, 470, "g-panel g-fadein", `<div style="padding:34px 40px;display:grid;gap:20px"><div class="g-title" style="font-size:36px">These are your keys</div>
      <div style="display:grid;grid-template-columns:auto 1fr;gap:14px 22px;align-items:center;font-size:22px;font-weight:800">
        <span><span class="g-key">W</span> <span class="g-key">A</span> <span class="g-key">S</span> <span class="g-key">D</span></span><span>Walk</span>
        <span><span class="g-key" style="min-width:120px">SPACE</span></span><span>Talk, look, choose</span>
        <span><span class="g-key">E</span></span><span>Open the menu (as Minecraft's inventory)</span>
        <span><span class="g-key" style="min-width:64px">TAB</span></span><span>Speed up</span>
        <span><span class="g-key">Q</span> <span class="g-key">X</span></span><span>Back</span></div>
      <div style="font-size:16px;font-weight:800;color:var(--g-muted);line-height:1.4">Other mods' keys are turned off while you play, so nothing else opens on these (a map mod on M, say).</div></div>`);
    C.choose(["Use these keys", "Show the key map", "Back"], { x: 860, y: 300, w: 300 }).then(i => go(i === 1 ? "keymap" : i === 2 || i < 0 ? "saves" : "prof"));
    return { key: k => C.key(k) };
  }
});

// ===================== A new game =====================
screen("prof", "A new game", "The professor", {
  scene: "prof", sceneOpts: () => ({ mode: "intro" }),
  what: "The professor welcomes you, standing with our creatures about him (one wanders round, the bird hovers flapping). Then, as Let's Go's opening, he sets off to the right as he talks, the camera going with him past a whole parade of POKéMON, big ones at the back and little ones near, each turning to watch him go. ZAPGLIDER trots at his side, the bone-helmet one follows behind and the bird flies over his shoulder. At the far end he stops, turns to you, the camera comes in close, and it fades to black before you make yourself.",
  tags: ["New: his walk past them all"], mod: "IntroScreen: Prof. Oak talks the whole time he's speaking. To come: the walk.",
  try: ["A to go on."],
  make() {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 });
    add(h("div", "", `<div style="position:absolute;inset:0;background:radial-gradient(circle at 20% 30%,rgba(255,255,255,.6) 0 2px,transparent 3px),radial-gradient(circle at 80% 20%,rgba(255,255,255,.7) 0 2px,transparent 3px),radial-gradient(circle at 70% 70%,rgba(255,255,255,.5) 0 2px,transparent 3px),radial-gradient(circle at 30% 80%,rgba(255,255,255,.6) 0 2px,transparent 3px);background-size:240px 200px;animation:gbob 4s ease-in-out infinite"></div>`));
    const C = ctl(), S = SC.prof;
    // The professor's words on a dark band at the foot of the screen, no box (as Let's Go's opening).
    C.sayAt = { film: true };
    S.talking = true;
    C.say([
      { who: "PROF. OAK", text: "Ah, there you are! Welcome. I'm **OAK**. Most people round here call me the **POKéMON PROF**." },
      { who: "PROF. OAK", text: "This world is full of creatures we call **POKéMON**. You'll find them in the grass, the sea, the caves, even in town.", on: () => S.startWalk && S.startWalk() },
      { who: "PROF. OAK", text: "Some people keep them as friends. Some raise them for battles. Me? I study them." },
      { who: "PROF. OAK", text: "But that's enough about me. First, tell me a little about yourself.", on: () => S.stopWalk && S.stopWalk() },
    ]).then(async () => {
      S.talking = false; if (!C.alive()) return;
      // Wait for him to reach the end and turn, then fade to black.
      while (C.alive() && !(S.walk && S.walk.done && S.walk.turnT >= 1)) await C.wait(100);
      await C.wait(500); if (!C.alive()) return;
      const fade = box(0, 0, 1280, 720, "", ""); fade.style.background = "#000"; fade.style.opacity = 0; fade.style.transition = "opacity .8s"; later(20, () => fade.style.opacity = 1);
      await C.wait(950); if (C.alive()) go("creator");
    });
    return { key: k => C.key(k) };
  }
});
// The maker's rows: what the mod's CLOTHES already has (Outfit), and what's new here (more skin tones, eye colour).
function makerRows(tab) {
  const O = () => G.outfit, sw = (label, key, list, isNew) => ({ label, key, swatches: list.map(hex), isNew, get: () => O()[key], set: v => O()[key] = v });
  const ch = (label, key, names, off) => ({ label, key, chips: names.map((n, i) => [i, title(n)]), off, get: () => O()[key], set: v => O()[key] = v });
  if (tab === "you") return [
    { label: "BODY", chips: [[0, "Boy"], [1, "Girl"]], get: () => O().girl ? 1 : 0, set: v => { const keep = { skin: O().skin, eyes: O().eyes }; G.outfit = Object.assign(OUTFIT.defaults(!!v), keep); } },
    Object.assign(sw("SKIN", "skin", OUTFIT.SKIN_RGB.concat(OUTFIT.SKIN_RGB_NEW)), { newFrom: 4 }), Object.assign(sw("EYES", "eyes", OUTFIT.EYE_RGB), { newFrom: 0 }),
    ch("HAIR", "hair", OUTFIT.HAIRS), sw("HAIR COLOUR", "hairc", OUTFIT.COLOURS),
  ];
  return [ch("HAT", "hat", OUTFIT.HATS), ch("CAP", "capp", OUTFIT.CAP_NAMES, () => O().hat !== 1), ch("CAP WAY", "capb", ["Forwards", "Backwards"], () => O().hat !== 1), sw("HAT COLOUR", "hatc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)),
    ch("TOP", "top", OUTFIT.TOPS), sw("TOP COLOUR", "topc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)), ch("BOTTOMS", "bot", OUTFIT.BOTTOMS), sw("BOTTOMS COLOUR", "botc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)),
    sw("SHOES", "shoec", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)), ch("BAG", "bag", OUTFIT.BAGS), sw("BAG COLOUR", "bagc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES))];
}
function maker(C, tabs, o) {
  let tab = 0, sel = 0, rows = makerRows(tabs[0]);
  const glass = !!o.glass;
  const P = box(o.x ?? 24, o.y ?? 20, o.w ?? 870, null, glass ? "mk-glass" : "g-panel", ""); P.style.padding = glass ? "16px 16px 14px" : "12px 14px"; P.style.maxHeight = (o.maxH || 650) + "px"; P.style.overflow = "auto";
  if (glass && !document.getElementById("mkGlassCss")) document.head.appendChild(h("style", "", `.mk-glass{border-radius:30px;background:linear-gradient(160deg,rgba(40,80,140,.42),rgba(22,46,92,.52));backdrop-filter:blur(16px);-webkit-backdrop-filter:blur(16px);
    box-shadow:0 0 0 2px rgba(255,255,255,.6),0 0 0 6px rgba(255,255,255,.14),0 20px 44px rgba(16,60,96,.28);color:#fff;scrollbar-width:thin}
    .mk-glass .g-label{color:rgba(255,255,255,.92);text-shadow:0 1px 4px rgba(10,40,70,.35)}.mk-glass .g-title{color:#fff!important;text-shadow:0 2px 8px rgba(10,40,70,.35)}`, { id: "mkGlassCss" }));
  const opts = r => r.swatches ? r.swatches.map((c, i) => [i, c]) : r.chips;
  const idxOf = r => Math.max(0, opts(r).findIndex(([v]) => v === r.get()));
  const draw = () => {
    rows = makerRows(tabs[tab]);
    const tabBar = tabs.length > 1 ? `<div style="display:flex;gap:8px;margin:2px 0 8px 10px">${tabs.map((t, i) => `<span class="g-chip${i === tab ? " on" : ""}" data-t="${i}" style="font-size:14px;padding:3px 14px">${t === "you" ? "YOU" : "CLOTHES"}</span>`).join("")}<span style="margin-left:auto;font-size:12px;font-weight:900;color:${glass ? "rgba(255,255,255,.85)" : "var(--g-muted)"};align-self:center">TAB switches</span></div>` : "";
    // Rows go in cards by what they're for (hat, top, bottoms...), with room to breathe: big swatches and chips.
    // YOU is one card: body, skin, eyes, then hair style and hair colour under them.
    const GROUP = { "BODY": "YOU", "SKIN": "YOU", "EYES": "YOU", "HAIR": "YOU", "HAIR COLOUR": "YOU", "HAT": "HAT", "CAP": "HAT", "CAP WAY": "HAT", "HAT COLOUR": "HAT",
      "TOP": "TOP", "TOP COLOUR": "TOP", "BOTTOMS": "BOTTOMS", "BOTTOMS COLOUR": "BOTTOMS", "SHOES": "SHOES", "BAG": "BAG", "BAG COLOUR": "BAG" };
    const short = (r, g) => r.label === "HAIR" && g === "YOU" ? "HAIR STYLE" : r.label === g ? (g === "SHOES" ? "COLOUR" : "STYLE") : r.label.startsWith(g + " ") ? r.label.slice(g.length + 1) : r.label;
    const one = glass || tabs[tab] === "you";
    const rowHtml = (r, i, g) => { const dim = r.off && r.off(), on = i === sel;
      return `<div style="display:grid;grid-template-columns:${glass ? "1fr" : (one ? 112 : 70) + "px 1fr"};align-items:center;gap:${glass ? 8 : 10}px;padding:${one ? "9px 12px" : "4px 8px"};border-radius:14px;transition:background .15s;${dim ? "opacity:.4;" : ""}${on ? (glass ? "background:rgba(255,236,150,.32);box-shadow:inset 0 0 0 2.5px #FFE07A" : "background:#FFF6CF;box-shadow:inset 0 0 0 2.5px #FFC93C") : ""}">
      <span class="g-label" style="font-size:12.5px;${on ? (glass ? "color:#FFF3B0" : "color:var(--g-red)") : ""}">${on ? "&#9656; " : ""}${short(r, g)}</span>
      <span style="display:flex;flex-wrap:wrap;gap:${one ? 6 : 7}px;align-items:center">${opts(r).map(([v, l], j) => r.swatches
        ? `<span class="g-swatch${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="position:relative;width:${one ? 29 : 30}px;height:${one ? 29 : 30}px;border-width:3px;border-radius:9px;background:linear-gradient(160deg,rgba(255,255,255,.28),rgba(0,0,0,.12)),${l}">${r.newFrom != null && j >= r.newFrom ? `<i style="position:absolute;right:-4px;top:-4px;width:10px;height:10px;border-radius:50%;background:#FFD45A;box-shadow:0 0 0 2px #fff"></i>` : ""}</span>`
        : `<span class="g-chip${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="font-size:14px;padding:3px 11px">${esc(l)}</span>`).join("")}</span></div>`; };
    const groups = []; rows.forEach((r, i) => { const g = GROUP[r.label] || r.label; let G2 = groups[groups.length - 1]; if (!G2 || G2.g !== g) groups.push(G2 = { g, rows: [] }); G2.rows.push([r, i]); });
    // Rows sit apart (a clear gap under the one you're on), the YOU card full width, CLOTHES in two columns.
    P.innerHTML = tabBar + `<div style="${one ? "" : "columns:2;column-gap:10px"}">${groups.map(G2 => `<div style="border-radius:18px;background:${glass ? "rgba(255,255,255,.1);box-shadow:inset 0 0 0 1.5px rgba(255,255,255,.22)" : "#F3F6FC"};padding:${one ? "12px 12px 14px" : "8px 6px"};display:grid;gap:${glass ? 12 : one ? 20 : 8}px;break-inside:avoid;margin-bottom:12px">
      <div class="g-title" style="font-size:17px;letter-spacing:2px;padding:2px 12px 2px;color:var(--g-red)">${G2.g}</div>${G2.rows.map(([r, i]) => rowHtml(r, i, G2.g)).join("")}</div>`).join("")}</div>` +
      `<div style="display:flex;gap:12px;margin-top:0;padding:0 10px;align-items:center"><span class="g-item${sel === rows.length ? " on" : ""}" data-done="1">${esc(T(o.done || "DONE"))}</span><span class="g-item${sel === rows.length + 1 ? " on" : ""}" data-rand="1">${esc(T("MIX IT UP"))}</span>
        ${tabs[tab] === "you" && !glass ? `<span style="margin-left:auto;font-size:12.5px;font-weight:800;color:var(--g-muted)"><i style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#FFD45A;margin-right:5px"></i>New: four more skin tones, eye colour</span>` : ""}</div>`;
    P.querySelectorAll("[data-r]").forEach(e => e.onclick = () => { const r = rows[+e.dataset.r]; sel = +e.dataset.r; r.set(opts(r)[+e.dataset.j][0]); changed(); });
    P.querySelectorAll("[data-t]").forEach(e => e.onclick = () => { tab = +e.dataset.t; sel = 0; draw(); });
    P.querySelector("[data-done]").onclick = () => o.onDone(); P.querySelector("[data-rand]").onclick = () => mix();
  };
  const changed = () => { draw(); if (SC.creator) SC.creator.refresh(); };
  const mix = () => { for (const t of tabs) for (const r of makerRows(t)) { if (r.label === "BODY") continue; const os = opts(r); r.set(os[Math.floor(Math.random() * os.length)][0]); } changed(); };
  draw();
  const w = { el: P, key(k) {
    const n = rows.length + 2;
    if (k === "tab" && tabs.length > 1) { tab = (tab + 1) % tabs.length; sel = 0; draw(); return true; }
    if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : n - 1)) % n; draw(); return true; }
    if (sel >= rows.length) { if (k === "a") { if (sel === rows.length) o.onDone(); else mix(); return true; } if (k === "left" || k === "right") { sel = sel === rows.length ? rows.length + 1 : rows.length; draw(); return true; } }
    if (k === "a") { o.onDone(); return true; }
    const r = rows[sel];
    if (r && (k === "left" || k === "right")) { const os = opts(r); r.set(os[(idxOf(r) + (k === "right" ? 1 : os.length - 1)) % os.length][0]); changed(); return true; }
    if (k === "b" && o.onBack) { o.onBack(); return true; }
    return false;
  } };
  C.push(w);
  return w;
}
// Eight starting looks for the maker: four boys, four girls, light to dark, each their own hair.
const STARTERS_LOOK = [
  { girl: 0, skin: 4, hair: 1, hairc: 14, eyes: 4 }, { girl: 0, skin: 1, hair: 0, hairc: 9, eyes: 1 }, { girl: 0, skin: 5, hair: 1, hairc: 4, eyes: 2 }, { girl: 0, skin: 7, hair: 0, hairc: 4, eyes: 1 },
  { girl: 1, skin: 4, hair: 3, hairc: 15, eyes: 3 }, { girl: 1, skin: 1, hair: 2, hairc: 16, eyes: 4 }, { girl: 1, skin: 5, hair: 4, hairc: 17, eyes: 6 }, { girl: 1, skin: 7, hair: 5, hairc: 4, eyes: 1 }];
function lookOf(i) { const p = STARTERS_LOOK[i]; return Object.assign(OUTFIT.defaults(!!p.girl), { skin: p.skin, hair: p.hair, hairc: p.hairc, eyes: p.eyes }); }
// A Minecraft face, big: the front of the head and the hat layer over it, from the skin the look makes.
function faceCanvas(outfit, px) {
  const src = composeOutfit(outfit), cv = document.createElement("canvas"); cv.width = cv.height = 8; const x = cv.getContext("2d");
  x.drawImage(src, 8, 8, 8, 8, 0, 0, 8, 8); x.drawImage(src, 40, 8, 8, 8, 0, 0, 8, 8);
  Object.assign(cv.style, { position: "relative", left: "auto", top: "auto", width: px + "px", height: px + "px", imageRendering: "pixelated", display: "block", borderRadius: "12px", boxShadow: "0 0 0 3px #fff, 0 6px 12px rgba(20,60,90,.3)" });
  return cv;
}
screen("creator", "A new game", "Character maker", {
  scene: "creator", sceneOpts: () => ({ side: 0 }),
  what: "Make yourself, as Let's Go does it: first a starting look. Four boys' faces down the left and four girls' down the right, each a Minecraft face on a glossy card, light skin to dark, and you in the middle on a pool of light. The one you're on lifts in a white frame. Then THAT'S ME and you move aside for the glass panel, where YOU (body, skin, eyes, hair style, hair colour) and CLOTHES make it your own. No white card: everything floats on the soft blue light.",
  tags: ["New look: Let's Go's", "The mod's wardrobe", "New: 4 skin tones, eye colour"], mod: "Outfit: FAIR, LIGHT, TAN, DEEP skin; 6 hair styles; cap, beanie or sun hat; jacket, tee, hoodie or vest; jeans, shorts or skirt; shoes; backpack or satchel; 14 colours.",
  try: ["Up and down a column, left and right between boys and girls, A to start from that look.", "Then up and down between rows, left and right to change, A to carry on. MIX IT UP picks at random."],
  make(arg) {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 60 });
    bgAdd("", { background: "radial-gradient(ellipse 22% 9% at 50% 86%, rgba(255,255,255,.75), rgba(200,250,255,.25) 55%, transparent 80%)" });
    const C = ctl(), S = SC.creator;
    let stage = arg && arg.stage === 2 ? 2 : 1, pick = 0;
    const cards = box(0, 0, 1280, 720, "", ""); cards.style.pointerEvents = "none";
    add(h("style", "", `.mk-face{position:absolute;width:250px;height:104px;border-radius:24px;display:flex;align-items:center;justify-content:center;gap:18px;cursor:pointer;pointer-events:auto;
        background:linear-gradient(110deg,#FFFFFF 0%,#F2FBEA 45%,#D9F3A8 100%);box-shadow:0 0 0 3px rgba(255,255,255,.85),0 10px 22px rgba(16,60,96,.25);transition:transform .16s, box-shadow .16s}
      .mk-face.on{transform:scale(1.07);box-shadow:0 0 0 3px #fff,0 0 0 9px rgba(255,255,255,.55),0 0 30px 8px rgba(255,255,255,.65),0 14px 26px rgba(16,60,96,.3)}
      .mk-face .gl{position:absolute;inset:6px 10px auto;height:36%;border-radius:20px 20px 40% 40%;background:linear-gradient(180deg,rgba(255,255,255,.75),rgba(255,255,255,0));pointer-events:none}`));
    const faces = STARTERS_LOOK.map((p, i) => { const e = h("div", "mk-face", ""); const girl = i >= 4, row = i % 4;
      e.style.left = (girl ? 960 : 70) + "px"; e.style.top = (70 + row * 122) + "px";
      e.appendChild(faceCanvas(lookOf(i), 80)); e.insertAdjacentHTML("beforeend", `<span class="gl"></span>`);
      e.onmouseenter = () => { if (stage === 1) { pick = i; show(); } }; e.onclick = () => { pick = i; show(); start(); };
      cards.appendChild(e); return e; });
    let tb = null, panel = null, acts = null;
    const say = text => { if (tb) tb.kill(); tb = textBox([{ text }], { x: 190, w: 580, keep: true }); };
    const show = () => { faces.forEach((e, i) => e.classList.toggle("on", i === pick)); G.outfit = lookOf(pick); if (S) S.refresh(); };
    // On to making it your own: you slide left, the glass panel comes in on the right.
    const start = () => {
      if (stage === 2 && panel) return; stage = 2;
      cards.style.transition = "opacity .3s"; cards.style.opacity = 0; later(320, () => cards.style.display = "none");
      if (S) S.sideGoal = 2.1;
      say(T("Make it yours. Change anything you like."));
      panel = maker(C, ["you", "clothes"], { glass: true, x: 650, y: 24, w: 600, maxH: 560, done: "THAT'S ME", onDone: () => go("name", { who: "you" }), onBack: () => back() });
      panel.el.style.animation = "gfade .35s ease-out";
    };
    const back = () => { if (stage === 2) { go("creator"); return; } go("prof"); };
    show(); say(T("What do you look like?"));
    if (stage === 2) start();
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
    return { key: k => {
      if (stage === 2) return C.key(k) || (k === "b" ? (back(), true) : false);
      if (k === "up" || k === "down") { const col = pick >= 4 ? 4 : 0, row = (pick % 4 + (k === "down" ? 1 : 3)) % 4; pick = col + row; show(); return true; }
      if (k === "left" || k === "right") { pick = (pick + 4) % 8; show(); return true; }
      if (k === "a") { start(); return true; } if (k === "b") { back(); return true; } return true; } };
  }
});
const KEYS_AZ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").concat(["-", "'", ".", " ", "0", "1", "2", "3", "4", "5"]);
function nameEntry(C, o) {
  let name = o.start || "", sel = 0, presetsOn = !!(o.presets && o.presets.length);
  const P = box(240, 150, 800, 430, "g-panel g-pop", ""); P.style.padding = "24px 30px";
  const all = KEYS_AZ.length + 2;
  const draw = () => {
    P.innerHTML = `<div style="display:flex;align-items:center;gap:18px;margin-bottom:18px"><span class="g-label">${esc(T(o.label))}</span>
      <span style="flex:1;display:flex;gap:8px">${Array.from({ length: 7 }, (_, i) => `<span style="flex:1;max-width:56px;height:56px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:34px;background:${i < name.length ? "#FFF6CF" : "#EEF2FB"};color:#26304A;${i === name.length ? "box-shadow:inset 0 0 0 3px #E5525C" : ""}">${esc(name[i] || "")}</span>`).join("")}</span></div>
      <div style="display:grid;grid-template-columns:repeat(9,1fr);gap:8px">${KEYS_AZ.map((c, i) => `<span data-i="${i}" style="height:50px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:26px;cursor:pointer;${i === sel ? "background:var(--g-sel);color:#26304A;box-shadow:0 0 0 2px #fff,0 4px 10px rgba(232,150,30,.35)" : "background:#EEF2FB"}">${c === " " ? "&#9251;" : esc(c)}</span>`).join("")}
      <span data-i="${KEYS_AZ.length}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length ? "background:var(--g-sel);color:#26304A" : "background:#EEF2FB"}">${esc(T("DELETE"))}</span>
      <span data-i="${KEYS_AZ.length + 1}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length + 1 ? "background:linear-gradient(180deg,#6BE0A6,#2FA86C);color:#fff" : "background:#D9F5E6;color:#1F8A55"}">OK</span></div>`;
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
    swirlBackdrop("peach", { lx: 80, ly: 10, cx: 78, cy: 62 });
    const C = ctl(), girl = !!G.outfit.girl;
    const presets = girl ? ["LEAF", "LYRA", "KRIS"] : ["RED", "KRIS", "ETHAN"];
    C.choose(["NEW NAME"].concat(presets), { x: 980, y: 190, w: 250, back: false }).then(i => {
      if (i > 0) { G.name = presets[i - 1]; go("rival"); return; }
      const w = nameEntry(C, { label: "YOUR NAME", start: "", onDone: n => { G.name = n.toUpperCase(); go("rival"); }, onBack: () => go("creator", { stage: 2 }) });
      cur.raw = e => w.raw(e);
    });
    box(0, 70, 1280, null, "", `<div class="g-title" style="text-align:center;font-size:30px;color:#26304A">${esc(T("And what's your name?"))}</div>`);
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
    return { key: k => C.key(k) || (k === "b" ? (go("creator", { stage: 2 }), true) : false) };
  }
});
screen("rival", "A new game", "Your rival's name", {
  scene: "prof", sceneOpts: () => ({ mode: "rival" }),
  what: "The professor's grandson, your rival since you were small. Pick or type his name.",
  mod: "IntroScreen: YOUR RIVAL'S NAME.",
  make() {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 }); const C = ctl(), S = SC.prof; C.sayAt = { film: true };
    (async () => {
      S.talking = true;
      await C.say([{ who: "PROF. OAK", text: "So you're **{N}**! A fine name." }, { who: "PROF. OAK", text: "And this is my grandson. You two have been racing each other since you could walk." }, { who: "PROF. OAK", text: "Now, what was his name again...?" }], { keep: true });
      S.talking = false;
      const names = ["BLUE", "GARY", "JOHN"];
      const i = await C.choose(["NEW NAME"].concat(names), { x: 980, y: 140, w: 250, back: false });
      if (!C.alive()) return;
      const done = async n => { G.rival = n; uiRoot.querySelectorAll(".g-text").forEach(e => e.remove()); S.talking = true; await C.say([{ who: "PROF. OAK", text: "That's right! I remember now. His name is **{R}**!" }]); S.talking = false; if (C.alive()) go("ready"); };
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
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 }); const C = ctl(), S = SC.prof; C.sayAt = { film: true };
    // You look up (so your eyes show under the cap), then at "Off you go" you do the floss while he waves.
    S.talking = true; S.youWave = 0; S.youFloss = 0; S.youLookUp = 0.38; S.wave = 0;
    C.say([{ who: "PROF. OAK", text: "**{N}**, your own adventure starts today." }, { who: "PROF. OAK", text: "There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab." },
      { who: "PROF. OAK", text: "Off you go, then!", on: () => { S.youFloss = 1; S.wave = 1; } }])
      .then(() => { S.talking = false; return C.wait(1600); }).then(() => { S.youFloss = 0; S.wave = 0; if (C.alive()) go("loading"); });
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
