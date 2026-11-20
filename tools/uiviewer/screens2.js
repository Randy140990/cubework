// ===================== In the world =====================
// Where you are: a white pill, a sunny dot, the time on a coral tab.
function placePill(right) {
  return box(24, 20, null, null, "", `<div style="display:flex;align-items:center;gap:12px;padding:6px 8px 6px 14px;border-radius:999px;background:#fff;box-shadow:0 0 0 3px rgba(255,255,255,.55),0 6px 16px rgba(30,40,84,.25)"><span style="width:16px;height:16px;border-radius:50%;background:radial-gradient(circle at 35% 35%,#FFF3A6,#FFC93C);box-shadow:0 0 0 3px #FFE9A0"></span><span class="g-title" style="font-size:24px;color:#26304A">${esc(G.place)}</span><span style="margin-left:6px;padding:3px 12px;border-radius:999px;background:linear-gradient(180deg,#FF9473,#E5525C);color:#fff;font-weight:900;font-size:13px;letter-spacing:1px">${right || "9:15 AM"}</span></div>`);
}
function blurOver() { add(h("div", "g-blur")); }
screen("hud", "In the world", "The HUD", {
  scene: "town", sceneOpts: () => ({}),
  what: "What's over the world while you walk: where you are and the time, what to do next, the keys, and a word when something happens.",
  mod: "Hud: the clock, and the keys for Talk, Menu and Speed.",
  tags: ["New: the place banner, the next thing to do, notices"],
  try: ["A replays the place banner and a notice.", "Tab changes the speed shown.", "B opens the menu."],
  make() {
    let spd = 1;
    const pill = placePill();
    // What to do next: a white card with a blue NEXT tab and a pin.
    const goal = box(1280 - 24 - 390, 20, 390, null, "", `<div style="position:relative;padding:22px 18px 12px;border-radius:22px;background:#fff;box-shadow:0 0 0 3px rgba(255,255,255,.55),0 6px 16px rgba(30,40,84,.25)">
      <span style="position:absolute;left:16px;top:-11px;padding:2px 14px;border-radius:999px;background:linear-gradient(180deg,#7CC2FA,#3C8DE4);color:#fff;font-weight:900;font-size:13px;letter-spacing:1.4px;box-shadow:0 0 0 3px #fff">${esc(T("NEXT"))}</span>
      <span style="display:flex;align-items:center;gap:10px;font-size:21px;font-weight:900;color:#26304A"><span style="width:30px;height:30px">${PICTO.map("#3C8DE4").replace('fill="#fff"', 'fill="#E3F0FC"')}</span>${esc(T("Go to PROF. OAK's lab"))}<span style="margin-left:auto;color:var(--g-muted);font-size:16px">32 m</span></span></div>`);
    const keys = () => { const e = $("#hkeys"); if (e) e.remove(); const k = hints([["SPACE", "Talk"], ["ENTER", "Menu"], ["TAB", `Speed x${spd}`]]); k.id = "hkeys"; };
    keys();
    const banner = () => {
      // The place's name sliding in: a white band fading out to the right, a sunny stripe along its top.
      const b = box(0, 210, 780, null, "g-slide", `<div style="background:linear-gradient(90deg,#fff 58%,rgba(255,255,255,0));padding:20px 130px 22px 60px;border-top:6px solid #FFC93C;box-shadow:0 10px 24px rgba(30,40,84,.12)">
        <div class="g-title" style="font-size:54px;letter-spacing:4px;color:#26304A">${esc(G.place)}</div><div style="font-size:21px;font-weight:800;color:#E5525C">A quiet start to a long road.</div></div>`);
      later(3200, () => { b.style.transition = "opacity .6s, transform .6s"; b.style.opacity = 0; b.style.transform = "translateX(-60px)"; });
      later(4000, () => b.remove());
    };
    const toast = (icon, text) => {
      const t = box(0, 96, null, null, "g-panel g-pop", `<div style="display:flex;align-items:center;gap:12px;padding:10px 22px 10px 12px;font-size:20px;font-weight:900"><span style="width:34px;height:34px">${icon}</span>${esc(T(text))}</div>`);
      t.style.left = "50%"; t.style.transform = "translateX(-50%)"; t.style.borderRadius = "999px";
      later(2600, () => { t.style.transition = "opacity .5s"; t.style.opacity = 0; }); later(3200, () => t.remove());
    };
    const emote = () => {
      const e = box(0, 0, 64, 64, "g-pop", `<img src="${EMOTE.heart}" style="width:64px;height:64px">`);
      const p = V(8.1, 1.35, 11.4).project(cam3); e.style.left = ((p.x + 1) / 2 * 1280 - 32) + "px"; e.style.top = ((1 - p.y) / 2 * 720 - 62) + "px";
      later(1800, () => e.remove());
    };
    later(500, banner); later(4200, () => toast(ICON.save, "Game saved")); later(2000, emote);
    return { key: k => { if (k === "a") { banner(); later(800, () => toast(ICON.potion, "You found a POTION!")); later(300, emote); return true; } if (k === "tab") { spd = spd % 3 + 1; keys(); return true; } if (k === "b") { go("menu"); return true; } return false; } };
  }
});
screen("door", "In the world", "Going through a door", {
  scene: "town", sceneOpts: () => ({}),
  what: "Going in or out of a door: a circle closes in on you until it's black, the place changes, and the circle opens out again round you (the games' iris).",
  tags: ["New"], try: ["A to go through again."],
  make() {
    const ring = box(0, 0, 1280, 720, "", "");
    ring.style.pointerEvents = "none";
    let run = 0;
    const go1 = () => {
      const me = ++run, t0 = performance.now(), Rmax = 760, cx = 640, cy = 360;
      const f = () => { if (me !== run || !uiRoot.contains(ring)) return; const t = (performance.now() - t0) / 1000;
        // 0 to .55 closing, .55 to .9 black, .9 to 1.45 opening.
        const k = t < 0.55 ? 1 - (t / 0.55) ** 1.6 : t < 0.9 ? 0 : Math.min(1, ((t - 0.9) / 0.55) ** 0.8);
        ring.style.background = `radial-gradient(circle at ${cx}px ${cy}px, transparent ${k * Rmax}px, #000 ${k * Rmax + 1}px)`;
        if (t < 1.5) requestAnimationFrame(f); else ring.style.background = "none"; };
      f();
    };
    later(500, go1);
    return { key: k => { if (k === "a") { go1(); return true; } if (k === "b") { go("hud"); return true; } return false; } };
  }
});
screen("talk", "In the world", "Talking to someone", {
  scene: "town", sceneOpts: () => ({ mum: true }),
  what: "Talking: the text box with who's speaking on a cream tag, and choices when they ask you something.",
  mod: "DialogueScreen: the camera closes in on the two of you; the text box and choice lists.",
  make() {
    const C = ctl(), S = SC.town;
    (async () => {
      S.mumTalk = true;
      await C.say([{ who: "MUM", text: "Oh! Are you off already, love?" }, { who: "MUM", text: "PROF. OAK's lab is just down the road. Have you got everything?", ask: ["Yes!", "Not yet"] }]).then(async v => {
        await C.say([{ who: "MUM", text: v === 0 ? "That's my girl... or boy! Off you go, then." : "Here, you'll want this. It's a map of the whole region." }]);
        S.mumTalk = false; if (C.alive()) go("itemget");
      });
    })();
    return { key: k => C.key(k) };
  }
});
const GETS = [{ item: "TOWN MAP", from: "MUM", pocket: "KEY ITEMS", n: 1 }, { item: "OAK's PARCEL", from: "the CLERK", pocket: "KEY ITEMS", n: 1 }, { item: "POKé BALL", from: "PROF. OAK", pocket: "POKé BALLS", n: 5 }];
screen("itemget", "In the world", "Getting an item", {
  scene: "town", sceneOpts: () => ({ mum: true }),
  what: "Getting something, as Let's Go does it: the thing itself (its own picture: a map, the parcel, a ball) pops up in a round pearly medallion with sparkles; more than one counts in, x1 to x5; then it drops into your bag and the words say which pocket.",
  tags: ["New look", "No box, no notes"], try: ["Left and right for the parcel and the five POKé BALLS.", "A to go on."],
  make(arg) {
    const C = ctl(), g = GETS[(arg && arg.k) || 0];
    const card = box(490, 70, 300, 340, "", `<style>@keyframes igpop{0%{transform:scale(.2);opacity:0}60%{transform:scale(1.12);opacity:1}100%{transform:scale(1)}}@keyframes igspin{to{transform:rotate(360deg)}}
      @keyframes igtw{0%,100%{opacity:0;transform:scale(.4)}50%{opacity:1;transform:scale(1)}}@keyframes igdrop{to{transform:translate(40px,300px) scale(.2);opacity:0}}@keyframes igin{from{transform:translateY(-30px) scale(.4);opacity:0}}</style>
      <div id="igm" style="position:absolute;left:30px;top:20px;width:240px;height:240px;animation:igpop .5s cubic-bezier(.2,1.4,.4,1) both">
        <div style="position:absolute;inset:-40px;border-radius:50%;background:repeating-conic-gradient(rgba(255,255,255,.55) 0 8deg,transparent 8deg 22deg);animation:igspin 12s linear infinite;-webkit-mask:radial-gradient(circle,#000 35%,transparent 70%);mask:radial-gradient(circle,#000 35%,transparent 70%)"></div>
        <div style="position:absolute;inset:0;border-radius:50%;padding:6px;background:var(--g-holo);box-shadow:0 14px 30px rgba(30,40,84,.3)"><div style="width:100%;height:100%;border-radius:50%;background:radial-gradient(circle at 40% 30%,#fff,#F3F6FD);display:grid;place-items:center"><div id="igpic" style="width:140px;height:140px;position:relative">${ITEM_ICON(g.item)}</div></div></div>
        ${[[-6, 20, 34, 0], [210, 10, 26, .5], [222, 170, 22, 1], [-14, 180, 18, 1.4]].map(([x, y, w, d]) => `<span style="position:absolute;left:${x}px;top:${y}px;width:${w}px;height:${w}px;animation:igtw 1.6s ${d}s ease-in-out infinite;filter:drop-shadow(0 0 6px #FFE27A)">${ICON.sparkle}</span>`).join("")}
      </div>
      <div id="igc" style="position:absolute;left:0;right:0;top:272px;text-align:center"></div>`);
    // More than one: they count in, a little ball at a time, x1 up to x5.
    if (g.n > 1) { const c = $("#igc"); for (let k = 1; k <= g.n; k++) later(350 + k * 260, () => { c.innerHTML = `<span style="display:inline-flex;gap:6px;align-items:center;padding:6px 14px;border-radius:999px;background:#fff;box-shadow:0 6px 16px rgba(30,40,84,.25)">${Array.from({ length: k }, (_, q) => `<span style="width:30px;height:30px;${q === k - 1 ? "animation:igin .3s ease-out" : ""}">${ITEM_ICON(g.item)}</span>`).join("")}<span class="g-title" style="font-size:24px;color:#E5525C;margin-left:4px">x${k}</span></span>`; }); }
    C.say([{ text: `**{N}** got ${g.n > 1 ? g.n + " " : "a "}**${g.item}${g.n > 1 ? "S" : ""}** from ${g.from}!` },
      { text: `**{N}** put the **${g.item}${g.n > 1 ? "S" : ""}** in the **${g.pocket}** pocket.`, on: () => { const m = $("#igm"); if (m) m.style.animation = "igdrop .6s ease-in forwards"; const c = $("#igc"); if (c) c.style.opacity = 0; } }])
      .then(() => { if (C.alive()) go("menu"); });
    const at = (arg && arg.k) || 0;
    return { key: k => { if (k === "left" || k === "right") { go("itemget", { k: (at + (k === "right" ? 1 : 2)) % 3 }); return true; } return C.key(k); } };
  }
});
screen("badgeget", "In the world", "Getting a badge", {
  scene: "town", sceneOpts: () => ({}),
  what: "Winning a badge: the badge spins in over a burst of light, the leader's words, then it flies up into a notice on the HUD that shows where it sits among the eight. Every badge is our own design.",
  tags: ["New: badges of our own", "New: the badge notice"],
  try: ["Left and right to see each badge.", "A to move the words on."],
  make(arg) {
    const C = ctl(); let i = (arg && arg.badge) || 0, run = 0;
    const gym = i => Object.entries(GYMS).find(([, g]) => g[2] === i);
    const show = () => {
      const me = ++run; uiRoot.querySelectorAll(".bg-get").forEach(e => e.remove());
      const [town, [lead]] = gym(i), [name, col] = BADGES[i];
      const dim = box(0, 0, 1280, 720, "bg-get", ""); dim.style.background = "radial-gradient(circle at 50% 42%, rgba(255,244,216,.0) 0, rgba(10,24,30,.55) 60%)"; dim.style.animation = "gfade .4s";
      const big = box(490, 70, 300, 300, "bg-get", `<style>@keyframes bgspin{0%{transform:scale(.2) rotateY(540deg);opacity:0}70%{transform:scale(1.08) rotateY(-12deg);opacity:1}100%{transform:scale(1) rotateY(0)}}
        @keyframes bgrays{to{transform:rotate(360deg)}}@keyframes bgshine{0%,60%{transform:translateX(-140%) skewX(-20deg)}100%{transform:translateX(160%) skewX(-20deg)}}
        @keyframes bgfly{to{left:1018px;top:100px;width:56px;height:56px;opacity:.0}}@keyframes bgtoast{from{opacity:0;transform:translateX(40px)}}</style>
        <div style="position:absolute;inset:-120px;border-radius:50%;background:repeating-conic-gradient(${col}55 0 10deg,transparent 10deg 20deg);animation:bgrays 14s linear infinite;-webkit-mask:radial-gradient(circle,#000 30%,transparent 68%);mask:radial-gradient(circle,#000 30%,transparent 68%)"></div>
        <div style="position:absolute;inset:-30px;border-radius:50%;background:radial-gradient(circle,rgba(255,244,216,.9),rgba(255,244,216,0) 65%)"></div>
        <div style="position:absolute;inset:30px;perspective:600px"><div style="width:100%;height:100%;animation:bgspin 1.1s cubic-bezier(.2,.8,.3,1) both;filter:drop-shadow(0 14px 18px rgba(0,0,0,.35));position:relative;overflow:hidden;clip-path:polygon(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%)">${badgeSvg(i)}
        <div style="position:absolute;top:0;bottom:0;width:40%;background:linear-gradient(90deg,transparent,rgba(255,255,255,.65),transparent);animation:bgshine 2.6s 1s ease-in-out infinite"></div></div></div>`);
      C.say([{ text: `**${G.name}** received the **${name}** from **${lead}**!` }, { text: `It's pinned to your **TRAINER CARD** now. That's **${i + 1} of 8**.` }]).then(() => {
        if (!C.alive() || me !== run) return;
        big.style.animation = "bgfly .7s ease-in forwards"; later(700, () => big.remove()); dim.style.transition = "opacity .5s"; dim.style.opacity = 0;
        // The notice on the HUD: the badge, its name, and the eight slots with this one lit.
        const t = box(870, 96, 386, null, "g-panel bg-get", `<div style="display:flex;align-items:center;gap:14px;padding:12px 18px 12px 12px;animation:bgtoast .45s ease-out">
          <span style="width:58px;height:58px;flex:none">${badgeSvg(i)}</span><span style="display:grid;gap:6px"><span class="g-label">Badge</span><span style="font-size:21px;font-weight:900">${esc(name)}</span>
          <span style="display:flex;gap:5px">${BADGES.map((b, j) => `<span style="width:22px;height:22px;${j <= i ? "" : "filter:grayscale(1) brightness(.55);opacity:.5"}">${badgeSvg(j)}</span>`).join("")}</span></span></div>`);
        t.style.borderRadius = "22px";
        later(3800, () => { t.style.transition = "opacity .5s"; t.style.opacity = 0; });
      });
    };
    show();
    hints([["A", "Next"], ["◀▶", "Other badges"]]);
    return { key: k => { if (k === "left" || k === "right") { go("badgeget", { badge: (i + (k === "right" ? 1 : 7)) % 8 }); return true; } return C.key(k); } };
  }
});
const MENU = [["POKéDEX", "dex", "dex", "Every POKéMON you've seen and caught."], ["POKéMON", "ball", "party", "Your team: check them, switch them, pick who follows you."], ["BAG", "bag", "bag", "Your items, in four pockets."],
  ["MAP", "map", "map", "The whole region, and where you are in it."], ["CLOTHES", "cap", "clothes", "Change your clothes, hat and bag."], ["CARD", "card", "card", "Your trainer card: your name, money and badges."],
  ["WAIT", "clock", "wait", "Let the time pass: morning, midday, evening or night."], ["SAVE", "save", "save", "Save your adventure so far."], ["OPTION", "cog", "options", "Text speed, music, nights and more."], ["EXIT", "close", "hud", "Close the menu."]];
// The five big ones in Let's Go's order (POKéDEX, BAG, POKéMON, MAP, SAVE), each its own colour; then the small ones.
const MENU_MAIN = [0, 2, 1, 3, 7], MENU_SIDE = [4, 5, 6, 8, 9];
const MENU_COLS = [["#FF8C8C", "#E5484F", "#B92F3A"], ["#FFC566", "#F59A2A", "#D2731A"], ["#7FE3A6", "#3DB873", "#228A52"], ["#7CC2FA", "#3C8DE4", "#2763B8"], ["#C9A8F7", "#8F60DE", "#6740B6"]];
screen("menu", "In the world", "The menu", {
  scene: "town", sceneOpts: () => ({}),
  what: "The menu as Let's Go's player menu: your partner's face at the top (ours, drawn flat, where Let's Go has its Pikachu) with PLAY WITH under it; a row of five rounded tiles, each its own colour with a white picture, the one you're on lifted with a red arrow and a green line under its name; the smaller things in white pills underneath.",
  mod: "MenuScreen: POKéDEX, BAG, POKéMON, MAP and SAVE as the five tiles; CLOTHES, CARD, WAIT, OPTION and EXIT as pills; the partner's face opens its summary.",
  tags: ["New look: Let's Go's player menu"], try: ["Left and right along a row; up to your partner, down to the small ones; A to open, B to close."],
  make(arg) {
    blurOver();
    add(h("div", "", "", { position: "absolute", inset: "0", background: "linear-gradient(180deg, rgba(255,255,255,0) 30%, rgba(255,255,255,.55) 62%, rgba(255,255,255,.8))" }));
    const lead = G.party[0], els = {};
    // row 0 is your partner, 1 the tiles, 2 the pills; col along it.
    let row = 1, col = 0;
    if (arg && arg.sel != null) { const m = MENU_MAIN.indexOf(arg.sel), q = MENU_SIDE.indexOf(arg.sel); if (m >= 0) col = m; else if (q >= 0) { row = 2; col = q; } }
    const face = h("div", "", `<div style="display:grid;justify-items:center;gap:6px;cursor:pointer">${faceOf(lead.sid, 210)}
      <div class="g-title" style="font-size:19px;letter-spacing:2px;color:#26304A;padding:4px 18px;border-radius:999px;white-space:nowrap;background:rgba(255,255,255,.88);box-shadow:0 3px 10px rgba(30,40,84,.18)">${esc(T("PLAY WITH {0}", ourName(lead.sid)))}</div></div>`, { position: "absolute", left: "535px", top: "44px", width: "210px", transition: "transform .15s" });
    face.onmouseenter = () => { row = 0; draw(); }; face.onclick = () => { row = 0; open(); };
    add(face);
    MENU_MAIN.forEach((i, c) => {
      const [n, ic] = MENU[i], col0 = MENU_COLS[c], cx = 640 + (c - 2) * 172;
      const t = h("div", "m-tile", `<span class="arr">&#9654;</span><div class="sq" style="background:linear-gradient(180deg, ${col0[0]}, ${col0[1]} 58%, ${col0[2]})"><div class="gloss"></div>${PICTO[ic](col0[1])}</div><div class="nm">${esc(T(n))}</div><div class="ul"></div>`);
      t.style.left = (cx - 70) + "px"; t.style.top = "352px";
      t.onmouseenter = () => { row = 1; col = c; draw(); }; t.onclick = () => { row = 1; col = c; open(); }; els["1:" + c] = add(t);
    });
    MENU_SIDE.forEach((i, c) => {
      const [n, ic] = MENU[i], w = 190, x = 640 - (5 * w + 4 * 14) / 2 + c * (w + 14);
      const t = h("div", "m-pill", `<span class="ic">${ICON[ic]}</span><span>${esc(T(n))}</span>`);
      t.style.left = x + "px"; t.style.top = "560px";
      t.onmouseenter = () => { row = 2; col = c; draw(); }; t.onclick = () => { row = 2; col = c; open(); }; els["2:" + c] = add(t);
    });
    add(h("style", "", `.m-tile{position:absolute;width:140px;display:grid;justify-items:center;gap:10px;cursor:pointer}
      .m-tile .sq{position:relative;width:124px;height:124px;border-radius:34px;display:grid;place-items:center;border:4px solid #fff;box-shadow:0 7px 0 rgba(30,40,84,.18),0 12px 24px rgba(30,40,84,.25);transition:transform .14s;overflow:hidden}
      .m-tile .sq svg{width:74px;height:74px;position:relative;filter:drop-shadow(0 3px 0 rgba(0,0,0,.14))}
      .m-tile .gloss{position:absolute;left:8%;right:8%;top:5%;height:40%;border-radius:26px 26px 50% 50%;background:linear-gradient(180deg,rgba(255,255,255,.45),rgba(255,255,255,0))}
      .m-tile .nm{font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:21px;letter-spacing:1px;color:#26304A;-webkit-text-stroke:5px #fff;paint-order:stroke fill;transition:transform .14s}
      .m-tile .ul{width:0;height:6px;border-radius:999px;background:#41BF84;margin-top:-6px;transition:width .14s;box-shadow:0 2px 0 rgba(0,0,0,.12)}
      .m-tile .arr{position:absolute;left:-14px;top:46px;font-size:30px;color:#E5525C;opacity:0;transition:opacity .12s;-webkit-text-stroke:3px #fff;paint-order:stroke fill}
      .m-tile.on .sq{transform:translateY(-10px) scale(1.08)} .m-tile.on .nm{transform:translateY(-4px)} .m-tile.on .ul{width:96px} .m-tile.on .arr{opacity:1}
      .m-pill{position:absolute;width:190px;height:54px;border-radius:999px;display:flex;align-items:center;gap:12px;padding-left:7px;cursor:pointer;background:#fff;color:#26304A;font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:19px;
        box-shadow:0 0 0 2px rgba(255,255,255,.7),0 5px 14px rgba(30,40,84,.22);transition:transform .12s}
      .m-pill .ic{width:40px;height:40px;border-radius:50%;background:#F1F4FB;display:grid;place-items:center}.m-pill .ic svg{width:30px;height:30px}
      .m-pill.on{background:var(--g-sel);transform:translateY(-3px);box-shadow:0 0 0 3px #fff,0 0 0 6px #41BF84,0 8px 18px rgba(30,40,84,.25)} .m-pill.on .ic{background:#fff}`));
    const line = box(340, 640, 600, null, "", ""); line.style.textAlign = "center"; line.style.fontSize = "19px"; line.style.fontWeight = "800"; line.style.color = "#26304A";
    const item = () => row === 0 ? null : (row === 1 ? MENU_MAIN : MENU_SIDE)[col];
    const draw = () => {
      for (const k in els) els[k].classList.toggle("on", k === row + ":" + col);
      face.style.transform = row === 0 ? "translateY(-6px) scale(1.05)" : "none";
      const i = item();
      line.innerHTML = i == null ? esc(T("PLAY WITH {0}", ourName(lead.sid))) : esc(T(MENU[i][3]));
    };
    const open = () => { const i = item(); if (i == null) go("summary", { i: 0 }); else go(MENU[i][2], { from: "menu" }); };
    draw(); hints([["A", "Open"], ["B", "Close"]]);
    return { key: k => {
      if (k === "a") { open(); return true; } if (k === "b") { go("hud"); return true; }
      if (row > 0 && (k === "left" || k === "right")) col = (col + (k === "right" ? 1 : 4)) % 5;
      else if (k === "up") row = Math.max(0, row - 1);
      else if (k === "down") row = Math.min(2, row + 1);
      if (row === 0) col = 2;
      draw(); return true; } };
  }
});

// ===================== Menus =====================
const CAUGHT = new Set(["pikachu", "ivysaur", "pidgeotto", "sandshrew", "cubone", "clefairy", "bulbasaur", "pidgey", "rattata", "caterpie", "weedle", "spearow"]);
const DEXLIST = Object.entries(DATA.species).sort((a, b) => a[1].dex - b[1].dex).map(([sid, d]) => ({ sid, n: d.dex, d }));
const SEEN = new Set(DEXLIST.slice(0, 40).filter((x, i) => i % 6 !== 5).map(x => x.sid).concat([...CAUGHT]));
const DEXTEXT = {
  pikachu: "It glides from tree to tree on charged fur. When it's pleased, little sparks run down its back.",
  bulbasaur: "The seed on its back grows with it. It basks in the morning sun to give the seed strength.",
  ivysaur: "The bud on its back is heavy, so it stands firm on its thick legs. It smells sweet when it's about to bloom.",
  pidgey: "A gentle bird that would rather hide in the grass than fight. It kicks up sand to get away.",
  pidgeotto: "It has a wide patch of land it calls its own, and flies round it all day watching for trouble.",
  cubone: "It wears a hard helmet for its head, and won't take it off for anyone.",
  clefairy: "On full-moon nights it gathers with others of its kind to dance in the light.",
  sandshrew: "It digs deep burrows in dry ground and curls into a ball when it's frightened.",
  caterpie: "Its short feet have suction pads, so it can climb walls and trees without slipping.",
};
function dexText(sid) { const d = sp(sid); return DEXTEXT[sid] || `A ${d.stats.types.map(t => t.toLowerCase()).join(" and ")}-type POKéMON. Its entry is still being written.`; }
// The POKéDEX's own look, as Let's Go's: bright aqua with a fine grid, white type.
const DEXBG = "linear-gradient(0deg, rgba(255,255,255,.07) 1px, transparent 1px) 0 0/28px 28px, linear-gradient(90deg, rgba(255,255,255,.07) 1px, transparent 1px) 0 0/28px 28px, radial-gradient(ellipse at 50% 30%, #6FE3F2 0%, #28B6D6 50%, #1786B8 100%)";
// Reading an entry out (POKéDEX VOICE): the browser's own voice here; the game has its own.
function readOut(text) { try { const u = new SpeechSynthesisUtterance(text.replace(/POKéMON/g, "Pokémon")); u.rate = 0.95; u.pitch = 0.9; speechSynthesis.cancel(); speechSynthesis.speak(u); } catch (e) {} }
function stopReading() { try { speechSynthesis.cancel(); } catch (e) {} }
screen("dex", "Menus", "Pokédex", {
  what: "Every POKéMON as a grid of little pictures on the POKéDEX's aqua, as Let's Go's: the ones you've caught in full colour, the ones you've only seen as shadows, the rest a faint ball. The one you're on gets its number and name on a label.",
  mod: "PokedexScreen: SEEN and CAUGHT, the grid, and the entry.",
  tags: ["New look: Let's Go's"], try: ["Arrows round the grid, A for the entry, B back."],
  make(arg) {
    backdrop(DEXBG);
    const COLS = 7, ROWS = 4;
    let sel = Math.max(0, DEXLIST.findIndex(x => x.sid === (arg && arg.sid || "pikachu"))), top = Math.floor(Math.max(0, sel - COLS) / COLS) * COLS;
    box(0, 0, 1280, 64, "", `<div style="display:flex;justify-content:center;gap:120px;padding-top:16px;font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:22px;color:#fff;text-shadow:0 2px 0 rgba(0,0,0,.18)"><span>POKéMON caught&nbsp;&nbsp;<b style="font-size:28px">${CAUGHT.size}</b></span><span>POKéMON seen&nbsp;&nbsp;<b style="font-size:28px">${SEEN.size}</b></span></div>`);
    const grid = box(150, 86, 980, 540, "", "");
    const draw = () => {
      if (sel < top) top = Math.floor(sel / COLS) * COLS; if (sel >= top + COLS * ROWS) top = (Math.floor(sel / COLS) - ROWS + 1) * COLS;
      grid.innerHTML = `<div style="display:grid;grid-template-columns:repeat(${COLS},1fr);gap:14px 10px">${DEXLIST.slice(top, top + COLS * ROWS).map((x, j) => { const i = top + j, seen = SEEN.has(x.sid), got = CAUGHT.has(x.sid), on = i === sel;
        return `<div data-i="${i}" style="position:relative;height:120px;display:grid;place-items:center;cursor:pointer">
          ${on ? `<div style="position:absolute;inset:4px 10px;border-radius:24px;background:rgba(255,255,255,.28);box-shadow:0 0 0 3px #fff"></div>` : ""}
          <div style="position:relative;transform:${on ? "scale(1.15)" : "none"};transition:transform .12s">${seen ? pic(x.sid, 96, got ? "filter:drop-shadow(0 4px 4px rgba(0,40,80,.3))" : "filter:brightness(0) opacity(.35)") : `<span style="display:block;width:44px;height:44px;opacity:.28">${PICTO.ball("#28B6D6")}</span>`}</div>
          <span style="position:absolute;bottom:2px;font-weight:900;font-size:14px;color:rgba(255,255,255,.85)">${String(x.n).padStart(3, "0")}</span>
          ${on ? `<span style="position:absolute;top:-12px;padding:3px 14px;border-radius:999px;background:#fff;color:#26304A;font-weight:900;font-size:15px;white-space:nowrap;box-shadow:0 4px 10px rgba(0,40,80,.25)">No. ${String(x.n).padStart(3, "0")} ${seen ? esc(ourName(x.sid)) : "?????"}</span>` : ""}</div>`; }).join("")}</div>`;
      grid.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { sel = +e.dataset.i; draw(); if (SEEN.has(DEXLIST[sel].sid)) go("dexentry", { sid: DEXLIST[sel].sid }); });
    };
    draw(); hints([["A", "Entry"], ["B", "Back"], ["TAB", "Caught only"]]);
    return { key: k => {
      if (k === "left" || k === "right" || k === "up" || k === "down") { const d = { left: -1, right: 1, up: -COLS, down: COLS }[k]; sel = Math.max(0, Math.min(DEXLIST.length - 1, sel + d)); draw(); return true; }
      if (k === "a" && SEEN.has(DEXLIST[sel].sid)) { go("dexentry", { sid: DEXLIST[sel].sid }); return true; } if (k === "b") { go("menu", { sel: 0 }); return true; } return true; } };
  }
});
screen("dexentry", "Menus", "Pokédex entry", {
  scene: "model", sceneOpts: () => ({ sid: "pikachu", ox: 0, stand: false, zoom: 1.05 }),
  what: "One POKéMON's page, as Let's Go's: it stands big in the middle on its own colour; its number, name, types and height on a clear card top left; how many you've caught top right; what it's like across the bottom, read out loud (POKéDEX VOICE).",
  mod: "PokedexScreen's entry, with POKéDEX VOICE reading it out.",
  tags: ["New look", "Read out loud"], try: ["V (or the button) to read it again."],
  make(arg) {
    const sid = arg && arg.sid || "pikachu", d = sp(sid), tc = typeHex(d.stats.types[0]);
    monBackdrop(sid, { cx: 50, cy: 58 });
    SC.model.show(sid).then(() => { const e = $("#hgt"); if (e && SC.model.st) e.textContent = SC.model.st.m.size.h.toFixed(2) + " blocks"; });
    const glass = `background:rgba(255,255,255,.86);border:3px solid #fff;border-radius:22px;color:#26304A;box-shadow:0 8px 20px rgba(30,40,84,.2)`;
    box(40, 34, 420, null, "g-fadein", `<div style="${glass};padding:16px 22px;display:grid;gap:6px"><div style="font-weight:900;font-size:16px;letter-spacing:1px;color:${darkerHex(tc, 0.25)}">POKéDEX No. ${String(d.dex).padStart(3, "0")}</div>
      <div class="g-title" style="font-size:40px;line-height:1">${esc(ourName(sid))}</div><div style="display:flex;gap:8px">${d.stats.types.map(typeChip).join("")}</div>
      <div style="display:grid;grid-template-columns:auto 1fr;gap:2px 16px;margin-top:6px;font-size:19px;font-weight:800"><span style="opacity:.8">Height</span><span id="hgt">...</span><span style="opacity:.8">Stands in for</span><span>${esc(d.pname)}</span></div></div>`);
    box(940, 34, 300, null, "g-fadein", `<div style="${glass};padding:16px 22px;display:grid;gap:4px"><div style="font-weight:900;font-size:16px;letter-spacing:1px;opacity:.9">Number caught</div><div class="g-title" style="font-size:40px">${CAUGHT.has(sid) ? 1 : 0}</div>
      <div style="font-weight:900;font-size:16px;letter-spacing:1px;opacity:.9;margin-top:4px">Where to find it</div><div style="font-size:17px;font-weight:800">${CAUGHT.has(sid) ? "ROUTE 1, ROUTE 2, VIRIDIAN FOREST" : "Not found yet"}</div></div>`);
    const text = dexText(sid);
    box(0, 548, 1280, 172, "", `<div style="position:absolute;inset:0;background:linear-gradient(180deg, ${tc}00, ${darkerHex(tc, 0.45)}cc 45%, ${darkerHex(tc, 0.6)}ee)"></div>
      <div style="position:absolute;left:200px;right:200px;top:46px;text-align:center;font-size:27px;font-weight:800;line-height:1.45;color:#fff;text-shadow:0 2px 6px rgba(0,0,0,.35)">${esc(text)}</div>
      <div id="rd" style="position:absolute;left:150px;top:52px;width:36px;height:36px;border-radius:50%;background:#fff;display:grid;place-items:center;box-shadow:0 0 0 0 rgba(255,255,255,.6);animation:rdp 1.2s ease-out infinite"><svg viewBox="0 0 24 24" width="22" height="22"><path d="M4 9h4l5-4v14l-5-4H4z" fill="${tc}"/><path d="M16 8q3 4 0 8M18.5 5.5q5 6.5 0 13" stroke="${tc}" stroke-width="2" fill="none" stroke-linecap="round"/></svg></div>
      <style>@keyframes rdp{to{box-shadow:0 0 0 16px rgba(255,255,255,0)}}</style>`);
    later(600, () => readOut(text));
    hints([["V", "Read it out"], ["B", "Back"]]);
    return { leave: stopReading, key: k => { if (k === "b") { stopReading(); go("dex", { sid }); return true; } if (k === "v" || k === "a") { readOut(text); return true; } return true; } };
  }
});
function monCard(p, on, i) {
  const f = p.hp / p.maxhp, d = sp(p.sid), tc = typeHex(d.stats.types[0]);
  return `<div style="position:relative;height:250px;padding:14px 18px;border-radius:28px;border:4px solid #fff;cursor:pointer;transition:transform .14s;
    background:linear-gradient(170deg, #FFFFFF 0%, ${lighterHex(tc, 0.78)} 55%, ${lighterHex(tc, 0.55)} 100%);color:#26304A;
    box-shadow:${on ? "0 0 0 5px #FFC93C, 0 16px 30px rgba(30,40,84,.32)" : "0 8px 20px rgba(30,40,84,.2)"};${on ? "transform:translateY(-6px)" : ""}${p.status === "FNT" ? ";filter:saturate(.3)" : ""}">
    ${on ? `<span style="position:absolute;left:-26px;top:96px;font-size:30px;color:#E5525C;-webkit-text-stroke:3px #fff;paint-order:stroke fill">&#9654;</span>` : ""}
    <div style="position:absolute;left:8px;top:4px;width:170px;height:170px;display:grid;place-items:center"><div style="animation:${on ? "pbob .5s ease-in-out infinite alternate" : `pidle ${2.4 + i * 0.3}s ease-in-out infinite`}">${pic(p.sid, 160)}</div></div>
    <div style="margin-left:168px;display:grid;gap:8px;padding-top:6px">
      <div style="display:flex;align-items:center;gap:6px"><span class="g-title" style="font-size:25px;line-height:1.1">${esc(ourName(p.sid))}</span>${gender(p.gender)}</div>
      <div style="font-weight:900;font-size:20px">Lv. ${p.lv}</div>
      <div style="display:flex;gap:6px;flex-wrap:wrap">${d.stats.types.map(t => typeChip(t).replace("min-width:92px", "")).join("")}</div>
      <div>${statusChip(p.status)}${i === 0 ? `<span style="display:inline-block;margin-left:${p.status ? 6 : 0}px;padding:2px 10px;border-radius:999px;background:linear-gradient(180deg,#6BE0A6,#2FA86C);color:#fff;font-weight:900;font-size:12px;letter-spacing:.8px">FOLLOWING YOU</span>` : ""}</div></div>
    <div style="position:absolute;left:20px;right:20px;bottom:18px;display:flex;align-items:center;gap:10px;padding:6px 12px;border-radius:999px;background:rgba(255,255,255,.85)"><span style="font-weight:900;font-size:14px;letter-spacing:1px;color:#E5525C">HP</span>
      <div class="g-hp" style="flex:1"><i style="width:${Math.max(0, f) * 100}%;background:${hpCol(f)}"></i></div><span style="font-weight:900;font-size:19px;font-variant-numeric:tabular-nums">${p.hp}/${p.maxhp}</span></div></div>`;
}
screen("party", "Menus", "Your team", {
  what: "Your six as Let's Go's party cards: each in its own type's colour, its picture moving (the one you're on bouncing), name, level, types, HP. A on one for SUMMARY, SWITCH, FOLLOW ME.",
  mod: "MenuScreen's POKéMON, with SUMMARY, SWITCH, FOLLOW ME and CANCEL.",
  tags: ["New look", "Moving pictures"],
  make() {
    backdrop(LIME); rays(20, 0, 0.22); const C = ctl();
    header(PICTO.ball("#3DB873"), "POKéMON");
    add(h("style", "", "@keyframes pbob{from{transform:translateY(2px)}to{transform:translateY(-8px)}}@keyframes pidle{0%,100%{transform:translateY(0) rotate(0)}50%{transform:translateY(-3px) rotate(-2deg)}}"));
    let sel = 0;
    const grid = box(50, 104, 1180, 560, "", "");
    const draw = () => { grid.innerHTML = `<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:24px 28px">${G.party.map((p, i) => monCard(p, i === sel, i)).join("")}</div>`; [...grid.firstChild.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); menu(); }); };
    const menu = async () => {
      const i = await C.choose(["SUMMARY", "SWITCH", "FOLLOW ME", "CANCEL"], { x: 950, y: null, bottom: 30, w: 290 });
      if (i === 0) go("summary", { i: sel });
      else if (i === 2) { C.say([{ text: `**${ourName(G.party[sel].sid)}** will follow you now.` }]); }
    };
    draw(); hints([["A", "Choose"], ["B", "Back"]]);
    const nav = { key(k) { if (k === "a") { menu(); return true; } if (k === "b") { go("menu", { sel: 1 }); return true; } const n = sel; sel = gridMove(sel, 6, 3, k); if (n !== sel) draw(); return true; } };
    C.push(nav);
    return { key: k => C.key(k) };
  }
});
// How good a stat is for its level, in a word (as Let's Go's judge puts it).
function statWord(v, lv) { const f = v / (lv * 2.6 + 30); return f > 0.8 ? "Best" : f > 0.62 ? "Great" : f > 0.45 ? "Good" : f > 0.3 ? "Decent" : "Fair"; }
// The six stats as Let's Go's hexagon: a bright lime rim, a soft aqua inside, spokes; each stat's name and its word
// outside it, the number under; the creature's own share filled in.
function statHex(st, lv, tc) {
  const keys = [["HP", "HP"], ["Attack", "Attack"], ["Defense", "Defense"], ["Speed", "Speed"], ["Sp. Def", "Sp. Def"], ["Sp. Atk", "Sp. Atk"]], R = 118, cx = 190, cy = 178, mx = lv * 2.6 + 30;
  const pt = (k, r) => { const a = -Math.PI / 2 + k * Math.PI / 3; return [cx + Math.cos(a) * r, cy + Math.sin(a) * r]; };
  const ring = f => keys.map((_, k) => pt(k, R * f).join(",")).join(" ");
  const val = keys.map(([k], q) => pt(q, R * Math.max(0.15, Math.min(1, st[k] / mx))).join(",")).join(" ");
  const ink = "#1F3A2C";
  return `<svg viewBox="0 0 380 360" width="380" height="360"><defs><linearGradient id="hxr" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#D6FF6A"/><stop offset="1" stop-color="#3FD06A"/></linearGradient></defs>
    <polygon points="${ring(1.06)}" fill="none" stroke="rgba(0,0,0,.18)" stroke-width="16" transform="translate(0,4)"/>
    <polygon points="${ring(1)}" fill="rgba(110,214,190,.72)" stroke="url(#hxr)" stroke-width="14" stroke-linejoin="round"/>
    ${keys.map((_, q) => `<line x1="${cx}" y1="${cy}" x2="${pt(q, R)[0]}" y2="${pt(q, R)[1]}" stroke="rgba(255,255,255,.5)" stroke-width="1.5"/>`).join("")}
    <polygon points="${ring(0.5)}" fill="none" stroke="rgba(255,255,255,.4)" stroke-width="1.5"/>
    <polygon points="${val}" fill="#E9FF8A" fill-opacity=".85" stroke="#fff" stroke-width="2.5"/>
    ${keys.map(([k, l], q) => { const [x, y] = pt(q, R + 40), w = statWord(st[k], lv);
      return `<text x="${x}" y="${y - 10}" text-anchor="middle" font-family="Fredoka,sans-serif" font-weight="600" font-size="19" fill="#fff" stroke="${ink}" stroke-width="4" paint-order="stroke">${l}</text>
        <text x="${x}" y="${y + 12}" text-anchor="middle" font-family="Fredoka,sans-serif" font-weight="600" font-size="20" fill="#FFE04A" stroke="${ink}" stroke-width="4" paint-order="stroke">${w}</text>
        <text x="${x}" y="${y + 30}" text-anchor="middle" font-family="Nunito Sans,sans-serif" font-weight="900" font-size="13" fill="#fff" stroke="${ink}" stroke-width="3" paint-order="stroke">${st[k]}</text>`; }).join("")}</svg>`;
}
// The judge's stamp, top left: how good its stats are all told.
function statStamp(st, lv) {
  const f = Object.values(st).reduce((a, v) => a + v, 0) / 6 / (lv * 2.6 + 30), word = f > 0.62 ? "AMAZING" : f > 0.48 ? "GREAT" : f > 0.36 ? "GOOD" : "DECENT", stars = f > 0.62 ? 3 : f > 0.48 ? 2 : 1;
  return `<svg viewBox="0 0 260 190" width="260" height="190" style="opacity:.92;transform:rotate(-8deg)"><g fill="none" stroke="#fff" stroke-width="4">
    <path d="M30 40l40-22h120l40 22v70l-40 40H70l-40-40z" fill="rgba(255,255,255,.18)"/><path d="M42 46l34-18h108l34 18v60l-34 34H76l-34-34z" stroke-dasharray="7 5" stroke-width="2.5"/></g>
    <path d="M112 20l8-12 10 10 10-10 8 12z" fill="#fff"/>
    <rect x="8" y="108" width="244" height="34" rx="6" fill="#fff" transform="rotate(-4 130 125)"/>
    <text x="130" y="82" text-anchor="middle" font-family="Fredoka,sans-serif" font-weight="600" font-size="30" fill="#fff">${word}</text>
    <text x="130" y="132" text-anchor="middle" font-family="Fredoka,sans-serif" font-weight="600" font-size="24" fill="#E5525C" transform="rotate(-4 130 125)">STATS!</text>
    ${[0, 1, 2].map(k => `<path d="M${108 + k * 22} 158l3 7 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1z" fill="${k < stars ? "#FFE04A" : "rgba(255,255,255,.4)"}"/>`).join("")}</svg>`;
}
screen("summary", "Menus", "Summary", {
  scene: "model", sceneOpts: () => ({ sid: G.party[0].sid, ox: 1.05, stand: false }),
  what: "One of your team in full, as Let's Go's STATS page: it stands on its own backdrop (its type's colours, a great soft swirl of light, blurred light, and its own drifting things: embers, bubbles, leaves, sparks), the judge's stamp top left, its six stats as a lime hexagon with a word for each, and its name plate bottom right. INFO and MOVES are the other pages.",
  mod: "MenuScreen's SUMMARY: ATTACK, DEFENSE, SPCL.ATK, SPCL.DEF, SPEED.",
  tags: ["New look: Let's Go's STATS", "Each creature's own backdrop"], try: ["Tab, or left and right, for the next page. Up and down for the next of your team."],
  make(arg) {
    let i = arg && arg.i || 0, page = 1;
    const stamp = box(24, 20, 260, 190, "", ""), right = box(720, 40, 540, 470, "", ""), plate = box(700, 512, 560, null, "", ""), dexNo = box(0, 640, 260, null, "", "");
    const tabs = box(0, 10, 1280, null, "", "");
    const glass = "background:rgba(28,36,60,.42);border:2px solid rgba(255,255,255,.55);backdrop-filter:blur(6px);color:#fff";
    const draw = () => {
      const p = G.party[i], d = sp(p.sid), tc = typeHex(d.stats.types[0]), f = p.hp / p.maxhp; SC.model.show(p.sid);
      bgArt.innerHTML = ""; monBackdrop(p.sid, { cx: 33, cy: 64 });
      stamp.innerHTML = page === 1 ? statStamp(p.stats, p.lv) : "";
      tabs.innerHTML = `<div style="display:flex;justify-content:center;gap:10px">${["INFO", "STATS", "MOVES"].map((t, j) => `<span data-p="${j}" style="cursor:pointer;padding:5px 20px;border-radius:999px;font-family:Fredoka,sans-serif;font-weight:600;font-size:17px;letter-spacing:1px;${j === page ? "background:#fff;color:#26304A;box-shadow:0 4px 12px rgba(30,40,84,.25)" : "background:rgba(255,255,255,.28);color:#fff"}">${t}</span>`).join("")}</div>`;
      tabs.querySelectorAll("[data-p]").forEach(e => e.onclick = () => { page = +e.dataset.p; draw(); });
      // The name plate, Let's Go's: dark glass, a ball's outline in it, name, level, types, HP.
      plate.innerHTML = `<div style="position:relative;overflow:hidden;padding:14px 24px 14px 92px;border-radius:60px 24px 24px 60px;${glass};box-shadow:0 10px 24px rgba(20,24,48,.3)">
        <svg viewBox="0 0 100 100" width="150" height="150" style="position:absolute;left:-34px;top:-28px;opacity:.35"><circle cx="50" cy="50" r="44" fill="none" stroke="#fff" stroke-width="8"/><path d="M6 50h88" stroke="#fff" stroke-width="8"/><circle cx="50" cy="50" r="14" fill="none" stroke="#fff" stroke-width="8"/></svg>
        <div style="display:flex;align-items:center;gap:10px"><span class="g-title" style="font-size:32px">${esc(ourName(p.sid))}</span>${gender(p.gender)}${statusChip(p.status)}<span style="margin-left:auto;font-weight:900;font-size:22px">Lv. ${p.lv}</span></div>
        <div style="display:flex;gap:8px;margin:6px 0 10px">${d.stats.types.map(typeChip).join("")}</div>
        <div style="display:flex;align-items:center;gap:10px"><span style="font-weight:900;font-size:14px;color:#FFE04A">HP</span><div class="g-hp" style="flex:1;background:rgba(0,0,0,.3)"><i style="width:${f * 100}%;background:${hpCol(f)}"></i></div><span style="font-weight:900;font-size:19px">${p.hp}/${p.maxhp}</span></div></div>`;
      dexNo.innerHTML = `<div style="padding:10px 22px;border-radius:0 18px 0 0;${glass};border-left:0;border-bottom:0;font-weight:800;font-size:17px;line-height:1.4">${esc(ourName(p.sid))}<br>POKéDEX No. ${String(d.dex).padStart(3, "0")}</div>`;
      const card = `position:relative;border-radius:26px;background:rgba(255,255,255,.9);border:4px solid #fff;box-shadow:0 10px 24px rgba(30,40,84,.25);color:#26304A`;
      if (page === 0) right.innerHTML = `<div style="${card};padding:24px 30px;margin-top:40px"><div style="display:grid;grid-template-columns:auto 1fr;gap:12px 22px;font-size:21px;font-weight:800"><span class="g-label">Trainer</span><span>${esc(G.name)}</span><span class="g-label">ID No.</span><span>${G.id}</span><span class="g-label">Nature</span><span>Brave</span><span class="g-label">Item</span><span>None</span><span class="g-label">Met</span><span>ROUTE 1, at Lv. ${Math.max(2, p.lv - 18)}</span></div>
        <div style="margin-top:20px"><div class="g-label">EXP to Lv. ${p.lv + 1}</div><div class="g-exp" style="margin-top:8px;height:10px;background:#DDE3F0"><i style="width:${p.exp * 100}%;background:linear-gradient(90deg,#7CC2FA,#3C8DE4)"></i></div></div></div>`;
      else if (page === 1) right.innerHTML = `<div style="display:grid;justify-items:center;margin-top:20px">${statHex(p.stats, p.lv, tc)}</div>`;
      else right.innerHTML = `<div style="display:grid;gap:12px;margin-top:40px">${p.moves.map(n => { const m = MOVE[n] || { type: "NORMAL", pp: 20 }, c = typeHex(m.type);
        return `<div style="position:relative;display:flex;align-items:center;gap:14px;height:62px;padding:0 22px 0 8px;border-radius:999px;border:3px solid #fff;color:#fff;text-shadow:0 2px 0 rgba(0,0,0,.2);background:linear-gradient(180deg, ${lighterHex(c, 0.25)}, ${c} 55%, ${darkerHex(c, 0.18)});box-shadow:0 6px 14px rgba(30,40,84,.25)">
          <span style="padding:3px 12px;border-radius:999px;background:rgba(255,255,255,.92);color:${darkerHex(c, 0.25)};font-weight:900;font-size:13px;letter-spacing:1px;text-shadow:none">${esc(m.type)}</span><span class="g-title" style="font-size:24px">${esc(n)}</span><span style="margin-left:auto;font-weight:900;font-size:20px;font-variant-numeric:tabular-nums">${m.pp}/${m.pp}</span></div>`; }).join("")}</div>`;
    };
    draw(); hints([["TAB", "Page"], ["B", "Back"]]);
    return { key: k => { if (k === "tab" || k === "right") { page = (page + 1) % 3; draw(); return true; } if (k === "left") { page = (page + 2) % 3; draw(); return true; } if (k === "up" || k === "down") { i = (i + (k === "down" ? 1 : 5)) % 6; draw(); return true; } if (k === "b") { go("party"); return true; } return true; } };
  }
});
// The bag's pockets: each its own colour and picture.
const POCKETS = { "ITEMS": ["#3DB873", "potion"], "BALLS": ["#E5484F", "ball"], "KEY ITEMS": ["#F59A2A", "key"], "TM & HM": ["#3C8DE4", "disc"] };
screen("bag", "Menus", "Bag", {
  what: "The bag as Let's Go's: one big white page on a sunny backdrop, your money in its corner. The pockets along the top, each a round picture in its colour (the one you're on lifted, a red arrow and a green line under its name); what's in it as tiles below, each with its picture and how many; what it does along the foot.",
  mod: "MenuScreen's BAG: ITEMS, BALLS, KEY ITEMS and TM & HM.",
  tags: ["New look: Let's Go's"], try: ["Tab, or Q and E, for a pocket; arrows round the items; A to use."],
  make() {
    backdrop(LIME); rays(85, 0, 0.25); const C = ctl();
    const pockets = Object.keys(G.bag); let pk = 0, sel = 0;
    const page = box(60, 40, 1160, 610, "g-panel", ""); page.style.borderRadius = "36px";
    box(36, 18, null, null, "", `<div style="padding:6px 18px 6px 10px;border-radius:999px;background:#fff;box-shadow:0 0 0 3px #FFC93C,0 6px 14px rgba(30,40,84,.2);display:flex;align-items:center;gap:8px;font-weight:900;font-size:19px;color:#26304A"><span style="padding:2px 10px;border-radius:999px;background:#FFC93C;font-size:12px;letter-spacing:1px">MONEY</span>${money(G.money)}</div>`);
    const draw = () => {
      const items = G.bag[pockets[pk]], COLS = 4;
      page.innerHTML = `<div style="display:flex;justify-content:center;gap:54px;padding-top:26px">${pockets.map((p, i) => { const [c, ic] = POCKETS[p] || ["#8A93A6", "bag"], on = i === pk;
          return `<div data-p="${i}" style="position:relative;display:grid;justify-items:center;gap:6px;cursor:pointer;transform:${on ? "translateY(-4px)" : "none"};transition:transform .12s">
            ${on ? `<span style="position:absolute;left:-26px;top:22px;font-size:24px;color:#E5525C">&#9654;</span>` : ""}
            <span style="width:76px;height:76px;border-radius:50%;display:grid;place-items:center;background:#fff;box-shadow:0 0 0 4px ${on ? c : "#E4E9F4"},0 6px 14px rgba(30,40,84,.18)"><span style="width:48px;height:48px">${ICON[ic]}</span></span>
            <span style="font-family:Fredoka,sans-serif;font-weight:600;font-size:17px;color:${on ? c : "#8A93A6"}">${esc(p)}</span><span style="width:${on ? 70 : 0}px;height:5px;border-radius:999px;background:#41BF84;transition:width .14s"></span></div>`; }).join("")}</div>
        <div style="margin:22px 44px 0;border-top:2px dashed #E4E9F4"></div>
        <div style="display:grid;grid-template-columns:repeat(${COLS},1fr);gap:14px;padding:22px 50px 0">${items.map(([n, c], i) => { const on = i === sel, kc = GOODS[ITEM_KIND(n)];
          return `<div data-i="${i}" style="position:relative;display:flex;align-items:center;gap:12px;height:76px;padding:0 16px 0 10px;border-radius:22px;cursor:pointer;
            background:${on ? "var(--g-sel)" : "#F5F8FD"};box-shadow:${on ? "0 0 0 3px #fff,0 0 0 6px #FFC93C,0 8px 16px rgba(232,150,30,.3)" : "inset 0 0 0 2px #E8EDF6"}">
            ${on ? `<span style="position:absolute;left:-18px;font-size:20px;color:#E5525C">&#9654;</span>` : ""}
            <span style="width:54px;height:54px;flex:none;border-radius:50%;background:#fff;display:grid;place-items:center;box-shadow:0 0 0 3px ${kc[1]}"><span style="width:36px;height:36px">${ITEM_ICON(n)}</span></span>
            <span style="font-weight:900;font-size:17px;color:#26304A;line-height:1.15">${esc(n)}</span><span style="margin-left:auto;font-weight:900;font-size:18px;color:${kc[2]}">x${c}</span></div>`; }).join("") || `<div style="grid-column:1/-1;padding:30px;text-align:center;font-size:21px;font-weight:800;color:var(--g-muted)">Nothing in this pocket yet.</div>`}</div>
        <div style="position:absolute;left:40px;right:40px;bottom:26px;min-height:62px;padding:16px 26px;border-radius:22px;background:#F5F8FD;font-size:21px;font-weight:800;color:#26304A">${items[sel] ? esc(items[sel][2]) : ""}</div>`;
      page.querySelectorAll("[data-p]").forEach(e => e.onclick = () => { pk = +e.dataset.p; sel = 0; draw(); });
      page.querySelectorAll("[data-i]").forEach(e => { e.onmouseenter = () => { sel = +e.dataset.i; draw(); }; e.onclick = () => { sel = +e.dataset.i; use(); }; });
    };
    const use = async () => {
      const it = G.bag[pockets[pk]][sel]; if (!it) return;
      const i = await C.choose(pockets[pk] === "KEY ITEMS" ? ["USE", "REGISTER", "CANCEL"] : ["USE", "GIVE", "TOSS", "CANCEL"], { x: 940, y: 300, w: 260 });
      if (i === 0) C.say([{ text: pockets[pk] === "BALLS" ? "You can only throw that in a battle with a wild POKéMON!" : `Which POKéMON should get the **${it[0]}**?` }]);
    };
    draw(); hints([["A", "Use"], ["B", "Back"], ["TAB", "Pocket"]]);
    C.push({ key(k) { const items = G.bag[pockets[pk]];
      if (k === "tab") { pk = (pk + 1) % pockets.length; sel = 0; draw(); return true; }
      if (["left", "right", "up", "down"].includes(k) && items.length) { const n = sel; sel = gridMove(sel, items.length, 4, k); if (n !== sel) draw(); return true; }
      if (k === "a") { use(); return true; } if (k === "b") { go("menu", { sel: 2 }); return true; } return false; } });
    return { key: k => C.key(k) };
  }
});
// The town's buildings, for the panel (the places file only has the outside of a town).
const TOWN_HERE = { "PALLET TOWN": ["PROF. OAK's LAB", "Your house", "Rival's house"], "VIRIDIAN CITY": ["POKéMON CENTER", "POKé MART", "GYM"],
  "PEWTER CITY": ["POKéMON CENTER", "POKé MART", "GYM", "MUSEUM"], "CERULEAN CITY": ["POKéMON CENTER", "POKé MART", "GYM", "BIKE SHOP"],
  "SAFFRON CITY": ["POKéMON CENTER", "POKé MART", "GYM", "SILPH CO."], "VERMILION CITY": ["POKéMON CENTER", "POKé MART", "GYM", "FAN CLUB"],
  "LAVENDER TOWN": ["POKéMON CENTER", "POKé MART", "TOWER"], "CELADON CITY": ["POKéMON CENTER", "DEPT. STORE", "GYM", "GAME CORNER"],
  "FUCHSIA CITY": ["POKéMON CENTER", "POKé MART", "GYM", "SAFARI ZONE"], "CINNABAR ISLAND": ["POKéMON CENTER", "POKé MART", "GYM", "LAB"],
  "INDIGO PLATEAU": ["POKéMON CENTER", "POKéMON LEAGUE"] };
const BEEN = ["pallet_town", "route_1", "viridian_city", "route_22", "route_2", "pewter_city"];
let MAPIMG = null;
function mapImage() { if (!MAPIMG) { MAPIMG = new Image(); MAPIMG.src = MAPDATA.img; } return MAPIMG; }
screen("map", "Menus", "Town Map", {
  what: "The real Town Map: Kanto as it is in your world, drawn from above in soft colours from the blocks themselves. Every town and route is marked; point at one to see what's there.",
  mod: "TownMapScreen and TownMap: the same baked map (townmap/r.*.bin), the same colours and shading. Q and E zoom, C comes back to you, a town you've been to can be flown to. In game the places come from the zones you mark.",
  try: ["Arrows to move between places.", "+ and - (or the mouse wheel) to zoom, drag to look around.", "C to come back to you. A on a town you've been to: FLY HERE."],
  make() {
    backdrop("#348CCE");
    const P = MAPDATA.points, [bx0, bz0] = MAPDATA.box, MW = MAPDATA.w, MH = MAPDATA.h, VW = 900, VH = 680;
    const FIT = Math.min(VW / MW, VH / MH), ZOOMS = [FIT, 1.5, 3, 6];
    let zi = 0, cx = bx0 + MW / 2, cz = bz0 + MH / 2, tx = cx, tz = cz, sel = 0, t0 = performance.now(), alive = true;
    const YOU = { x: -797, z: 702, yaw: 0 };
    const M = box(20, 20, VW, VH, "g-panel", ""); M.style.overflow = "hidden"; M.style.padding = "0"; M.style.cursor = "grab";
    const cv = document.createElement("canvas"); cv.width = VW * 2; cv.height = VH * 2; Object.assign(cv.style, { width: VW + "px", height: VH + "px", display: "block" }); M.appendChild(cv);
    const ctx = cv.getContext("2d");
    const info = box(940, 20, 320, 626, "g-panel", ""); info.style.padding = "22px 24px"; info.style.overflow = "hidden"; info.style.display = "flex"; info.style.flexDirection = "column";
    const zoom = () => ZOOMS[zi];
    const toScreen = (x, z) => [(x - cx) * zoom() + VW / 2, (z - cz) * zoom() + VH / 2];
    const clampC = () => { const hw = VW / 2 / zoom(), hh = VH / 2 / zoom();
      tx = MW * zoom() <= VW ? bx0 + MW / 2 : Math.max(bx0 + hw, Math.min(bx0 + MW - hw, tx)); tz = MH * zoom() <= VH ? bz0 + MH / 2 : Math.max(bz0 + hh, Math.min(bz0 + MH - hh, tz)); };
    const follow = () => { const p = P[sel], [sx, sy] = toScreen(p.x, p.z); if (zi > 0 && (sx < 120 || sx > VW - 120 || sy < 90 || sy > VH - 90)) { tx = p.x; tz = p.z; clampC(); } };
    const KIND = { town: "Town", route: "Route", spot: "Place" };
    const rr = (x, y, w, hh, r) => { ctx.beginPath(); ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + hh, r); ctx.arcTo(x + w, y + hh, x, y + hh, r); ctx.arcTo(x, y + hh, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath(); };
    const label = (text, x, y, big) => {
      ctx.font = `900 ${big ? 13 : 11}px Nunito, system-ui, sans-serif`; const w = ctx.measureText(text).width + 14, hh = big ? 22 : 18;
      rr(x - w / 2, y, w, hh, hh / 2); ctx.fillStyle = "rgba(255,255,255,.95)"; ctx.fill(); ctx.strokeStyle = "rgba(30,40,84,.18)"; ctx.lineWidth = 1.5; ctx.stroke();
      ctx.fillStyle = "#26304A"; ctx.textAlign = "center"; ctx.textBaseline = "middle"; ctx.fillText(text, x, y + hh / 2 + 0.5);
    };
    const paint = now => {
      if (!alive) return;
      cx += (tx - cx) * 0.2; cz += (tz - cz) * 0.2;
      ctx.setTransform(2, 0, 0, 2, 0, 0); ctx.fillStyle = "#348CCE"; ctx.fillRect(0, 0, VW, VH);
      const img = mapImage(), z = zoom();
      if (img.complete) { ctx.imageSmoothingEnabled = z < 1; const [ox, oy] = toScreen(bx0, bz0); ctx.drawImage(img, ox, oy, MW * z, MH * z); }
      // The zones the mod knows, faintly, once you're in close.
      if (zi >= 1) for (const p of P) if (p.zone) { const [a, b] = toScreen(p.zone[0], p.zone[1]), [c, d] = toScreen(p.zone[2], p.zone[3]); ctx.strokeStyle = "rgba(255,244,216,.55)"; ctx.setLineDash([6, 5]); ctx.lineWidth = 1.5; ctx.strokeRect(a, b, c - a, d - b); ctx.setLineDash([]); }
      const pulse = (Math.sin((now - t0) / 260) + 1) / 2;
      P.forEach((p, i) => {
        const [sx, sy] = toScreen(p.x, p.z); if (sx < -40 || sy < -40 || sx > VW + 40 || sy > VH + 40) return;
        const on = i === sel, been = BEEN.includes(p.key);
        if (p.kind === "town") { const r = zi >= 2 ? 9 : 7; rr(sx - r, sy - r, r * 2, r * 2, 4); ctx.fillStyle = on ? "#FFD45A" : been ? "#E5525C" : "#B9C4C0"; ctx.fill(); ctx.lineWidth = 2.5; ctx.strokeStyle = "#FFFFFF"; ctx.stroke(); }
        else if (p.kind === "spot") { ctx.save(); ctx.translate(sx, sy); ctx.rotate(Math.PI / 4); ctx.fillStyle = on ? "#FFC93C" : "#fff"; ctx.fillRect(-5, -5, 10, 10); ctx.strokeStyle = "#E5525C"; ctx.lineWidth = 2; ctx.strokeRect(-5, -5, 10, 10); ctx.restore(); }
        else { ctx.beginPath(); ctx.arc(sx, sy, on ? 6 : 4, 0, 7); ctx.fillStyle = on ? "#FFC93C" : "#fff"; ctx.fill(); ctx.lineWidth = 2; ctx.strokeStyle = "#E5525C"; ctx.stroke(); }
        if (on) { ctx.beginPath(); ctx.arc(sx, sy, 15 + pulse * 5, 0, 7); ctx.strokeStyle = `rgba(255,212,90,${0.9 - pulse * 0.5})`; ctx.lineWidth = 3; ctx.stroke(); }
        if (p.kind === "town" || on || zi >= 2) label(p.name, sx, sy + (p.kind === "town" ? 12 : 9), p.kind === "town");
      });
      // You: the arrow, the way you face.
      const [yx, yy] = toScreen(YOU.x, YOU.z);
      ctx.save(); ctx.translate(yx, yy); ctx.rotate(YOU.yaw); ctx.beginPath(); ctx.moveTo(0, -11); ctx.lineTo(8, 8); ctx.lineTo(0, 4); ctx.lineTo(-8, 8); ctx.closePath();
      ctx.fillStyle = "#FFD45A"; ctx.fill(); ctx.lineWidth = 2.5; ctx.strokeStyle = "#26304A"; ctx.stroke(); ctx.restore();
      // Scale bar.
      const blocks = zi === 0 ? 200 : zi === 1 ? 100 : zi === 2 ? 50 : 20, bw = blocks * z;
      ctx.fillStyle = "rgba(255,255,255,.9)"; rr(14, VH - 40, bw + 90, 26, 13); ctx.fill(); ctx.fillStyle = "#26304A"; ctx.fillRect(26, VH - 28, bw, 4);
      ctx.font = "900 12px Nunito, system-ui, sans-serif"; ctx.textAlign = "left"; ctx.textBaseline = "middle"; ctx.fillText(blocks + " blocks", 34 + bw, VH - 26);
      requestAnimationFrame(paint);
    };
    const drawInfo = () => {
      const p = P[sel], been = BEEN.includes(p.key), here = TOWN_HERE[p.name] || p.here || [];
      const wild = p.wild.slice(0, GYMS[p.name] ? 4 : 12);
      info.innerHTML = `<div class="g-label">${p.key === "pallet_town" ? "You are here" : KIND[p.kind] || "Place"}</div>
        <div class="g-title" style="font-size:${p.name.length > 14 ? 27 : 32}px;line-height:1.1;margin:6px 0 10px">${esc(p.name)}</div>
        ${p.tag ? `<div style="font-size:18px;font-weight:800;line-height:1.45;color:var(--g-text);font-style:italic">"${esc(p.tag)}"</div>` : ""}
        ${here.length ? `<div class="g-label" style="margin-top:16px">Here</div><div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:7px">${here.map(x => `<span style="display:inline-flex;align-items:center;gap:7px;font-size:14.5px;font-weight:900;padding:4px 11px;border-radius:999px;background:#F1F4FB"><span style="width:8px;height:8px;border-radius:3px;background:#E5525C"></span>${esc(x)}</span>`).join("")}</div>` : ""}
        ${GYMS[p.name] ? (([lead, type, b, lv]) => `<div class="g-label" style="margin-top:16px">Gym</div><div style="display:grid;grid-template-columns:58px 1fr;gap:4px 12px;align-items:center;margin-top:8px;padding:10px 12px;border-radius:16px;background:#F1F4FB">
          <span style="grid-row:span 3;width:58px;height:58px">${badgeSvg(b)}</span><span style="font-size:19px;font-weight:900">${esc(lead)}</span>
          <span style="font-size:14px;font-weight:900;color:var(--g-muted)">${esc(type)} type · ${esc(BADGES[b][0])}</span><span style="font-size:14px;font-weight:800;color:var(--g-text)">Team from level ${esc(lv)}</span></div>`)(GYMS[p.name]) : ""}
        ${wild.length ? `<div class="g-label" style="margin-top:16px">Wild POKéMON</div><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px">${wild.map(sid => DATA.species[sid]
          ? `<div title="${esc(DATA.species[sid].pname)}" style="aspect-ratio:1;border-radius:12px;background:#F1F4FB;display:grid;place-items:center">${pic(sid, 56)}</div>`
          : `<div title="${esc(sid)}" style="aspect-ratio:1;border-radius:12px;background:#F1F4FB;display:grid;place-items:center;font-size:26px;font-weight:900;color:var(--g-dim)">?</div>`).join("")}</div>` : ""}
        <div style="margin-top:auto;padding-top:12px;display:grid;gap:8px">
          ${p.kind === "town" ? (been ? `<div class="g-chip on" style="justify-content:center;font-size:17px;padding:8px">A: FLY HERE</div>` : `<div class="g-chip" style="justify-content:center;font-size:15px;padding:8px;opacity:.7">Not been here yet</div>`) : ""}
          <div style="font-size:12.5px;font-weight:800;color:var(--g-muted);line-height:1.35">${p.zone ? `Zone in the mod: ${p.zone[0]}, ${p.zone[1]} to ${p.zone[2]}, ${p.zone[3]}` : `About ${p.x}, ${p.z} on your map`}</div></div>`;
    };
    const near = k => { const p = P[sel], dir = { left: [-1, 0], right: [1, 0], up: [0, -1], down: [0, 1] }[k]; let best = sel, bd = 1e9;
      P.forEach((q, i) => { if (i === sel) return; const dx = q.x - p.x, dz = q.z - p.z, along = dx * dir[0] + dz * dir[1]; if (along <= 4) return; const d = along + Math.abs(dx * dir[1] - dz * dir[0]) * 2.2; if (d < bd) { bd = d; best = i; } }); return best; };
    const setZoom = (n, ax, az) => { n = Math.max(0, Math.min(ZOOMS.length - 1, n)); if (n === zi) return; zi = n; if (ax != null) { tx = ax; tz = az; } else { tx = P[sel].x; tz = P[sel].z; } clampC(); cx = tx; cz = tz; };
    const pick = i => { sel = i; drawInfo(); follow(); };
    // Mouse: drag to look around, wheel to zoom, click a place to point at it.
    let drag = null;
    M.addEventListener("pointerdown", e => { drag = { x: e.clientX, y: e.clientY, tx, tz, moved: false }; M.setPointerCapture(e.pointerId); M.style.cursor = "grabbing"; });
    M.addEventListener("pointermove", e => { if (!drag) return; const k = uiScale(), dx = (e.clientX - drag.x) / k, dy = (e.clientY - drag.y) / k; if (Math.hypot(dx, dy) > 4) drag.moved = true; tx = drag.tx - dx / zoom(); tz = drag.tz - dy / zoom(); clampC(); cx = tx; cz = tz; });
    M.addEventListener("pointerup", e => { M.style.cursor = "grab"; const d = drag; drag = null; if (!d || d.moved) return;
      const r = M.getBoundingClientRect(), k = uiScale(), mx = (e.clientX - r.left) / k, my = (e.clientY - r.top) / k; let best = -1, bd = 22;
      P.forEach((p, i) => { const [sx, sy] = toScreen(p.x, p.z), dd = Math.hypot(sx - mx, sy - my); if (dd < bd) { bd = dd; best = i; } }); if (best >= 0) pick(best); });
    M.addEventListener("wheel", e => { e.preventDefault(); const r = M.getBoundingClientRect(), k = uiScale(), mx = (e.clientX - r.left) / k, my = (e.clientY - r.top) / k;
      const wx = cx + (mx - VW / 2) / zoom(), wz = cz + (my - VH / 2) / zoom(); setZoom(zi + (e.deltaY < 0 ? 1 : -1), wx, wz); }, { passive: false });
    pick(0); requestAnimationFrame(paint);
    // A big, plain way out: BACK, top left over the map.
    const back = box(38, 36, null, null, "", `<span class="g-act" style="min-width:0;height:52px;font-size:20px;padding:0 20px 0 8px"><span class="g-key k-b" style="width:36px;height:36px">B</span>${esc(T("Back"))}</span>`);
    back.onclick = () => { alive = false; go("menu", { sel: 3 }); };
    hints([["+", "Zoom in"], ["-", "Zoom out"], ["C", "You"], ["B", "Back"]]);
    return {
      raw(e) { if (e.code === "Equal" || e.code === "NumpadAdd") { setZoom(zi + 1); return true; } if (e.code === "Minus" || e.code === "NumpadSubtract") { setZoom(zi - 1); return true; }
        if (e.code === "KeyC") { pick(0); setZoom(Math.max(zi, 2), YOU.x, YOU.z); return true; } return false; },
      key: k => { if (k === "b") { alive = false; go("menu", { sel: 3 }); return true; }
        if (k === "tab") { let i = sel; do { i = (i + 1) % P.length; } while (P[i].kind !== "town"); pick(i); return true; }
        if (k === "a") { const p = P[sel]; if (p.kind === "town" && BEEN.includes(p.key)) { alive = false; G.place = p.name; go("hud"); } return true; }
        if (["left", "right", "up", "down"].includes(k)) { pick(near(k)); return true; } return true; },
      leave() { alive = false; }
    };
  }
});
// Our own badges: every one a six-sided enamel pin with a silver rim, its town's colour and its own mark in cream.
const BADGES = [["BOULDERBADGE", "#8E8A84"], ["CASCADEBADGE", "#3F86D0"], ["THUNDERBADGE", "#E2B320"], ["RAINBOWBADGE", "#4FA84A"], ["SOULBADGE", "#B0509A"], ["MARSHBADGE", "#D6962A"], ["VOLCANOBADGE", "#D8443C"], ["EARTHBADGE", "#5E8A3A"]];
const BADGE_MARK = [
  // a block: a cube seen from the corner
  `<path d="M32 17l12 6v13l-12 6-12-6V23z" fill="#FFF4D8"/><path d="M32 29l12-6v13l-12 6z" fill="#D9CDB0"/><path d="M32 29L20 23v13l12 6z" fill="#EFE3C6"/><path d="M26 32l3 2" stroke="#8E8A84" stroke-width="1.6"/>`,
  // three waves
  `<path d="M18 24q3.5-4 7 0t7 0 7 0 7 0M18 32q3.5-4 7 0t7 0 7 0 7 0M18 40q3.5-4 7 0t7 0 7 0 7 0" fill="none" stroke="#FFF4D8" stroke-width="3.4" stroke-linecap="round"/>`,
  // a bolt
  `<path d="M35 14L22 34h9l-3 16 14-22h-9z" fill="#FFF4D8"/>`,
  // a bow over a sprout
  `<path d="M18 36a14 14 0 0 1 28 0" fill="none" stroke="#FFF4D8" stroke-width="3"/><path d="M23 36a9 9 0 0 1 18 0" fill="none" stroke="#FFD45A" stroke-width="2.4"/><path d="M32 48V38" stroke="#FFF4D8" stroke-width="2.6"/><path d="M32 41q-6-1-7-6 6 0 7 6zM32 41q6-1 7-6-6 0-7 6z" fill="#FFF4D8"/>`,
  // a crescent and a star
  `<path d="M36 16a16 16 0 1 0 0 32 13 13 0 1 1 0-32z" fill="#FFF4D8"/><path d="M41 27l1.6 3.4 3.7.4-2.8 2.5.8 3.7-3.3-1.9-3.3 1.9.8-3.7-2.8-2.5 3.7-.4z" fill="#FFF4D8"/>`,
  // an eye in a diamond
  `<path d="M32 15l15 17-15 17-15-17z" fill="none" stroke="#FFF4D8" stroke-width="3"/><path d="M22 32q10-9 20 0-10 9-20 0z" fill="#FFF4D8"/><circle cx="32" cy="32" r="3.6" fill="#D6962A"/>`,
  // a peak with a flame
  `<path d="M16 46l11-15 4 4 5-8 12 19z" fill="#FFF4D8"/><path d="M33 26q-5-5 0-12 1 5 4 6 1 4-4 6z" fill="#FFD45A"/>`,
  // a world with a mountain on it
  `<circle cx="32" cy="32" r="14" fill="none" stroke="#FFF4D8" stroke-width="3"/><path d="M19 36l7-8 4 4 5-7 10 11q-2 6-13 9-10-2-13-9z" fill="#FFF4D8"/>`];
function badgeSvg(i) {
  const [, c] = BADGES[i];
  return `<svg viewBox="0 0 64 64" style="display:block;width:100%;height:100%"><defs><linearGradient id="bg${i}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${c}" stop-opacity="1"/><stop offset="1" stop-color="#000" stop-opacity=".25"/></linearGradient>
    <linearGradient id="br${i}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".5" stop-color="#B8C4CC"/><stop offset="1" stop-color="#6E7A84"/></linearGradient></defs>
    <path d="M32 3l25 14.5v29L32 61 7 46.5v-29z" fill="url(#br${i})"/><path d="M32 8l20.7 12v24L32 56 11.3 44V20z" fill="${c}"/><path d="M32 8l20.7 12v24L32 56 11.3 44V20z" fill="url(#bg${i})"/>
    <path d="M32 8l20.7 12v4L32 12 11.3 24v-4z" fill="#fff" opacity=".28"/>${BADGE_MARK[i]}</svg>`;
}
// The gyms, for the Town Map's city panels: leader, type, badge (index into BADGES), and their team's levels.
const GYMS = { "PEWTER CITY": ["BROCK", "ROCK", 0, "12 to 14"], "CERULEAN CITY": ["MISTY", "WATER", 1, "18 to 21"], "VERMILION CITY": ["LT. SURGE", "ELECTRIC", 2, "18 to 24"],
  "CELADON CITY": ["ERIKA", "GRASS", 3, "24 to 29"], "FUCHSIA CITY": ["KOGA", "POISON", 4, "37 to 43"], "SAFFRON CITY": ["SABRINA", "PSYCHIC", 5, "37 to 43"],
  "CINNABAR ISLAND": ["BLAINE", "FIRE", 6, "40 to 47"], "VIRIDIAN CITY": ["GIOVANNI", "GROUND", 7, "42 to 50"] };
screen("card", "Menus", "Trainer card", {
  scene: "creator", sceneOpts: () => ({ side: 2.1 }),
  what: "Your TRAINER CARD: you as you look, your name and ID, money, POKéDEX, time, and the eight badges.",
  mod: "MenuScreen's TRAINER CARD with the eight badges.",
  make() {
    backdrop(SKY); rays(15, 10, 0.2);
    box(470, 60, 770, 600, "g-panel g-fadein", `<div style="padding:28px 34px;display:grid;gap:16px"><div style="display:flex;align-items:center;gap:14px"><span style="width:46px;height:46px">${ICON.card}</span><span class="g-title" style="font-size:34px">TRAINER CARD</span></div>
      <div style="display:grid;grid-template-columns:200px 1fr;gap:12px 20px;font-size:23px;font-weight:900">
      <span class="g-label">Name</span><span>${esc(G.name)}</span><span class="g-label">ID No.</span><span>${G.id}</span><span class="g-label">Money</span><span>${money(G.money)}</span>
      <span class="g-label">Pokédex</span><span>${G.caught}</span><span class="g-label">Play time</span><span>${G.time}</span><span class="g-label">Started</span><span>${G.started}</span></div>
      <div class="g-label" style="margin-top:6px">Badges</div><div style="display:grid;grid-template-columns:repeat(8,1fr);gap:10px">${BADGES.map(([n, c], i) => `<div title="${n}" style="aspect-ratio:1;border-radius:18px;background:#F1F4FB;display:grid;place-items:center;${i < G.badges ? "" : "filter:grayscale(1) brightness(.5);opacity:.55"}"><span style="width:66px;height:66px">${badgeSvg(i)}</span></div>`).join("")}</div></div>`);
    hints([["B", "Back"]]);
    return { key: k => { if (k === "b") go("menu", { sel: 5 }); return true; } };
  }
});
screen("clothes", "Menus", "Clothes", {
  scene: "creator", sceneOpts: () => ({ side: -1.95 }),
  what: "Change what you wear, as the mod's CLOTHES does: hat (cap, beanie, sun hat), cap pattern and which way round, top, bottoms, shoes and bag, each in one of 14 colours. B keeps what you chose, UNDO puts back what you came in with.",
  mod: "ClothesScreen: CAP STYLE, BACKWARDS, colours; B keeps, UNDO restores.",
  make() {
    backdrop("radial-gradient(ellipse at 70% 45%, #FFFFFF 0%, #FFF0E0 35%, #F6C99A 70%, #D98A52 100%)");
    const C = ctl(), before = JSON.stringify(G.outfit);
    maker(C, ["clothes"], { done: "UNDO", onDone: () => { G.outfit = JSON.parse(before); SC.creator.refresh(); go("clothes"); }, onBack: () => go("menu", { sel: 4 }) });
    hints([["A", "Choose"], ["B", "Keep and close"]]);
    return { key: k => C.key(k) };
  }
});
screen("save", "Menus", "Saving", {
  scene: "creator", sceneOpts: () => ({ side: 2.1 }),
  what: "Saving, as Let's Go's: you on the left; your card on the right (name, ID, POKéDEX, money, time, when you started, and your team's faces); two big buttons under it, SAVE YOUR PROGRESS in yellow and BACK TO YOUR ADVENTURE; then a moment while it writes.",
  mod: "MenuScreen's SAVE.",
  tags: ["New look: Let's Go's"],
  make() {
    backdrop(SKY); rays(80, 0, 0.2); const C = ctl();
    box(560, 60, 660, null, "g-panel g-pop", `<div style="padding:22px 30px;display:grid;grid-template-columns:1fr 210px;gap:20px">
      <div style="display:grid;grid-template-columns:auto 1fr;gap:9px 24px;font-size:21px;font-weight:900">${[["Name", esc(G.name)], ["ID No.", G.id], ["Pokédex", G.caught + " POKéMON"], ["Money", money(G.money)], ["Play time", G.time], ["Started", G.started]].map(([l, v]) => `<span class="g-label" style="align-self:center">${esc(T(l))}</span><span style="text-align:right;border-bottom:2px solid #EEF1F8;padding-bottom:4px">${v}</span>`).join("")}</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">${G.party.map(p => `<span style="aspect-ratio:1;border-radius:50%;background:linear-gradient(180deg,#fff,${lighterHex(typeHex(sp(p.sid).stats.types[0]), 0.7)});box-shadow:0 0 0 3px #fff,0 4px 10px rgba(30,40,84,.2);display:grid;place-items:center;overflow:hidden">${pic(p.sid, 84)}</span>`).join("")}</div></div>`);
    const btns = box(600, 430, 580, null, "", "");
    let sel = 0;
    const draw = () => { btns.innerHTML = [["SAVE YOUR PROGRESS", "#FFEC8A", "#FFC93C"], ["BACK TO YOUR ADVENTURE", "#FFFFFF", "#EEF2FB"]].map(([l, a, b], i) => `<div data-i="${i}" style="position:relative;margin-bottom:16px;height:62px;border-radius:999px;display:grid;place-items:center;cursor:pointer;font-family:Fredoka,sans-serif;font-weight:600;font-size:24px;color:#26304A;
      background:linear-gradient(180deg,${a},${b});border:3px solid #fff;box-shadow:${i === sel ? "0 0 0 4px #E5525C," : ""}0 6px 16px rgba(30,40,84,.22)">${i === sel ? `<span style="position:absolute;left:-30px;font-size:26px;color:#E5525C">&#9654;</span>` : ""}${l}</div>`).join("");
      btns.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { sel = +e.dataset.i; pick(); }); };
    let busy = false;
    const pick = async () => {
      if (busy) return; busy = true;
      if (sel === 1) { go("menu", { sel: 7 }); return; }
      btns.style.opacity = .4;
      const sp_ = box(600, 590, 580, null, "", `<div style="display:flex;align-items:center;justify-content:center;gap:14px;font-weight:900;font-size:20px;color:#26304A"><span style="width:44px;height:44px;animation:spin 1s linear infinite">${PICTO.save("#8F60DE").replace('fill="#fff"', 'fill="#8F60DE"')}</span>Saving... don't switch off.</div><style>@keyframes spin{to{transform:rotate(360deg)}}</style>`);
      await C.wait(1400); sp_.remove();
      await C.say([{ text: "**{N}** saved the game." }]); if (C.alive()) go("hud");
    };
    draw(); hints([["A", "Choose"], ["B", "Back"]]);
    C.push({ key(k) { if (k === "up" || k === "down") { sel = 1 - sel; draw(); return true; } if (k === "a") { pick(); return true; } if (k === "b") { go("menu", { sel: 7 }); return true; } return false; } });
    return { key: k => C.key(k) };
  }
});
screen("wait", "Menus", "Letting time pass", {
  scene: "town", sceneOpts: () => ({}),
  what: "Choose a time of day to skip to. The screen dims, 'Time passes...', and it's that time.",
  mod: "MenuScreen's WAIT: MORNING, MIDDAY, EVENING, NIGHT.",
  make() {
    blurOver(); const C = ctl();
    (async () => {
      const i = await C.say([{ text: "Wait until when?", ask: ["MORNING", "MIDDAY", "EVENING", "NIGHT", "CANCEL"] }]);
      if (i == null || i === 4 || i < 0) { if (C.alive()) go("menu", { sel: 6 }); return; }
      const d = box(0, 0, 1280, 720, "", `<div style="position:absolute;inset:0;background:#000;opacity:0;transition:opacity .7s" id="dk"></div><div id="tp" class="g-title" style="position:absolute;left:0;right:0;top:330px;text-align:center;font-size:34px;opacity:0;transition:opacity .5s">Time passes...</div>`);
      later(30, () => { $("#dk").style.opacity = .92; }); await C.wait(900); $("#tp").style.opacity = 1; await C.wait(1400); $("#tp").style.opacity = 0; $("#dk").style.opacity = 0; await C.wait(700); d.remove();
      await C.say([{ text: `It's ${["morning", "midday", "evening", "night"][i]} now.` }]); if (C.alive()) go("hud");
    })();
    return { key: k => C.key(k) };
  }
});
