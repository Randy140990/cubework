// ===================== In the world =====================
function placePill(right) {
  return box(24, 20, null, null, "g-panel soft", `<div style="display:flex;align-items:center;gap:12px;padding:8px 20px 8px 14px"><span style="width:14px;height:14px;border-radius:50%;background:#8FF5C0;box-shadow:0 0 10px #8FF5C0"></span><span class="g-title" style="font-size:24px">${esc(G.place)}</span><span class="g-label" style="margin-left:6px">${right || "9:15 AM, MORNING"}</span></div>`);
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
    const goal = box(1280 - 24 - 380, 20, 380, null, "g-panel soft", `<div style="padding:12px 18px;display:grid;gap:4px"><span class="g-label">Next</span><span style="display:flex;align-items:center;gap:10px;font-size:21px;font-weight:900"><span style="width:28px;height:28px">${ICON.map}</span>Go to PROF. OAK's lab<span style="margin-left:auto;color:#A9D4C6;font-size:16px">32 m</span></span></div>`);
    const keys = () => { const e = $("#hkeys"); if (e) e.remove(); const k = hints([["SPACE", "Talk"], ["ENTER", "Menu"], ["TAB", `Speed x${spd}`]]); k.id = "hkeys"; };
    keys();
    const banner = () => {
      const b = box(0, 210, 760, null, "g-slide", `<div style="background:linear-gradient(90deg,rgba(18,60,56,.95) 60%,rgba(18,60,56,0));padding:22px 120px 22px 60px;border-top:3px solid #8FF5C0;border-bottom:3px solid #8FF5C0">
        <div class="g-title" style="font-size:54px;letter-spacing:4px">${esc(G.place)}</div><div style="font-size:21px;font-weight:800;color:#CFEDE3">A quiet start to a long road.</div></div>`);
      later(3200, () => { b.style.transition = "opacity .6s, transform .6s"; b.style.opacity = 0; b.style.transform = "translateX(-60px)"; });
      later(4000, () => b.remove());
    };
    const toast = (icon, text) => {
      const t = box(0, 96, null, null, "g-panel g-pop", `<div style="display:flex;align-items:center;gap:12px;padding:10px 22px 10px 12px;font-size:20px;font-weight:900"><span style="width:34px;height:34px">${icon}</span>${text}</div>`);
      t.style.left = "50%"; t.style.transform = "translateX(-50%)"; t.style.borderRadius = "999px";
      later(2600, () => { t.style.transition = "opacity .5s"; t.style.opacity = 0; }); later(3200, () => t.remove());
    };
    const emote = () => {
      const e = box(0, 0, 64, 64, "g-pop", `<img src="${EMOTE.note}" style="width:64px;height:64px">`);
      const p = V(8.1, 1.35, 11.4).project(cam3); e.style.left = ((p.x + 1) / 2 * 1280 - 32) + "px"; e.style.top = ((1 - p.y) / 2 * 720 - 62) + "px";
      later(1800, () => e.remove());
    };
    later(500, banner); later(4200, () => toast(ICON.save, "Game saved")); later(2000, emote);
    return { key: k => { if (k === "a") { banner(); later(800, () => toast(ICON.potion, "You found a POTION!")); later(300, emote); return true; } if (k === "tab") { spd = spd % 3 + 1; keys(); return true; } if (k === "b") { go("menu"); return true; } return false; } };
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
screen("itemget", "In the world", "Getting an item", {
  scene: "town", sceneOpts: () => ({ mum: true }),
  what: "Getting something: the item held up in a card with a little tune, then where it went in your bag.",
  tags: ["New card"],
  make() {
    const C = ctl();
    const card = box(490, 120, 300, 300, "g-panel g-pop", `<div style="position:absolute;inset:0;display:grid;place-items:center"><div style="position:absolute;inset:24px;border-radius:50%;background:radial-gradient(circle,rgba(255,244,216,.45),transparent 70%);animation:gbob 1.6s ease-in-out infinite"></div><div style="width:150px;height:150px;position:relative">${ICON.map}</div></div>
      <div style="position:absolute;left:18px;top:14px;font-size:30px;color:#FFD45A">&#9835;</div><div style="position:absolute;right:22px;top:40px;font-size:24px;color:#8FF5C0">&#9834;</div>`);
    C.say([{ text: `**${G.name}** got a **TOWN MAP** from MUM!` }, { text: `**${G.name}** put the **TOWN MAP** in the **KEY ITEMS** pocket.` }]).then(() => { if (C.alive()) go("menu"); });
    return { key: k => C.key(k) };
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
  ["MAP", "map", "map", "The TOWN MAP: where you are, and where everywhere else is."], ["CLOTHES", "cap", "clothes", "Change what you're wearing."], ["CARD", "card", "card", "Your TRAINER CARD and badges."],
  ["WAIT", "clock", "wait", "Let time pass, to morning, midday, evening or night."], ["SAVE", "save", "save", "Save your game."], ["OPTION", "cog", "options", "Text speed, music, nights and more."], ["EXIT", "close", "hud", "Close the menu."]];
const MENU_MAIN = [0, 1, 2, 3, 7], MENU_SIDE = [4, 5, 6, 8, 9];
const MENU_COLS = [["#F58A94", "#D94452", "#AE2E3C"], ["#7EE3AE", "#39B27A", "#22855A"], ["#FFD27A", "#F0A030", "#C87818"], ["#8EC4F6", "#4A8FE0", "#2F64B0"], ["#C6AEF4", "#8A62D8", "#6440B0"]];
screen("menu", "In the world", "The menu", {
  scene: "town", sceneOpts: () => ({}),
  what: "The menu in Let's Go's layout: your partner in a round badge at the top (where Let's Go has its Pikachu), five big round buttons in their own colours with their pictures, five smaller ones under them, and what the one you're on does.",
  mod: "MenuScreen: POKéDEX, POKéMON, BAG, MAP and SAVE as the big ones; CLOTHES, CARD, WAIT, OPTION and EXIT as the small ones.",
  tags: ["New look", "Colour"], try: ["Left and right along a row, up and down between the rows, A to open, B to close."],
  make(arg) {
    blurOver(); let sel = arg && arg.sel || 0;
    const lead = G.party[0], ring = TYPECOL(sp(lead.sid).stats.types[0]);
    add(h("div", "", `<div style="position:absolute;inset:0;border-radius:50%;background:#fff;box-shadow:0 8px 0 rgba(0,0,0,.25)"></div>
      <div style="position:absolute;inset:4px;border-radius:50%;background:${ring}"></div>
      <div style="position:absolute;inset:8px;border-radius:50%;background:radial-gradient(circle at 50% 35%, #FFFBF0, color-mix(in srgb, ${ring} 28%, #fff))"></div>
      <div style="position:absolute;inset:14px;display:grid;place-items:center">${pic(lead.sid, 110, "animation:menuBob 2.9s ease-in-out infinite")}</div>
      <div style="position:absolute;left:50%;top:122px;transform:translateX(-50%);background:${ring};color:#fff;padding:2px 17px;border-radius:999px;font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:17px;white-space:nowrap;box-shadow:0 2px 0 rgba(0,0,0,.2)">${esc(ourName(lead.sid))}</div>
      <style>@keyframes menuBob{0%,100%{transform:translateY(-2px)}50%{transform:translateY(2px)}}</style>`, { position: "absolute", left: (640 - 70) + "px", top: (118 - 70) + "px", width: "140px", height: "140px" }));
    const els = {};
    MENU_MAIN.forEach((i, c) => {
      const [n, ic] = MENU[i], col = MENU_COLS[c], cx = 240 + c * 200, cy = 330;
      const t = h("div", "", `<div class="ring"></div><div class="disc" style="background:linear-gradient(180deg, ${col[0]}, ${col[1]} 55%, ${col[2]})"><div class="face" style="background:linear-gradient(180deg, #fff, color-mix(in srgb, ${col[0]} 26%, #fff))">${ICON[ic]}</div></div><div class="nm">${esc(n)}</div>`);
      t.className = "g-big"; t.style.left = (cx - 75) + "px"; t.style.top = (cy - 75) + "px";
      t.onmouseenter = () => { sel = i; draw(); }; t.onclick = () => { sel = i; open(); }; els[i] = add(t);
    });
    MENU_SIDE.forEach((i, c) => {
      const [n, ic] = MENU[i], w = 186, x = 640 - (5 * w + 4 * 16) / 2 + c * (w + 16);
      const t = h("div", "g-small", `<span class="dot">${ICON[ic]}</span><span>${esc(n)}</span>`);
      t.style.left = x + "px"; t.style.top = "486px";
      t.onmouseenter = () => { sel = i; draw(); }; t.onclick = () => { sel = i; open(); }; els[i] = add(t);
    });
    add(h("style", "", `.g-big{position:absolute;width:150px;display:grid;justify-items:center;gap:10px;cursor:pointer}
      .g-big .disc{position:relative;width:140px;height:140px;border-radius:50%;display:grid;place-items:center;border:4px solid #fff;box-shadow:0 8px 0 rgba(0,0,0,.3);transition:transform .12s}
      .g-big .face{width:100px;height:100px;border-radius:50%;display:grid;place-items:center;box-shadow:inset 0 -3px 0 rgba(0,0,0,.08)}
      .g-big .face svg{width:70px;height:70px}
      .g-big .ring{position:absolute;left:-7px;top:-7px;width:164px;height:164px;border-radius:50%;background:rgba(255,244,216,.4);opacity:0;transition:opacity .12s}
      .g-big .nm{font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:21px;letter-spacing:1px;text-shadow:0 2px 0 rgba(0,0,0,.5);transition:transform .12s}
      .g-big.on .disc{transform:translateY(-10px) scale(1.08);border-color:var(--g-cream)}
      .g-big.on .ring{opacity:1;transform:translateY(-10px)}
      .g-big.on .nm{color:var(--g-cream);transform:translateY(-10px)}
      .g-small{position:absolute;width:186px;height:54px;box-sizing:border-box;border-radius:999px;display:flex;align-items:center;gap:12px;padding-left:7px;cursor:pointer;
        background:linear-gradient(180deg,rgba(29,90,82,.96),rgba(18,60,56,.96));border:2.5px solid var(--g-edge);box-shadow:0 4px 0 rgba(0,0,0,.25);font-family:Fredoka,system-ui,sans-serif;font-weight:600;font-size:19px}
      .g-small .dot{width:40px;height:40px;border-radius:50%;background:rgba(255,255,255,.22);display:grid;place-items:center}
      .g-small .dot svg{width:30px;height:30px}
      .g-small.on{background:var(--g-cream);border-color:#fff;color:#26304A;box-shadow:0 0 0 4px rgba(255,244,216,.4),0 4px 0 rgba(0,0,0,.25)}
      .g-small.on .dot{background:#26304A}`));
    const line = box(240, 576, 800, null, "g-panel soft", ""); line.style.padding = "14px 26px"; line.style.textAlign = "center"; line.style.fontSize = "21px"; line.style.fontWeight = "800";
    const draw = () => { for (const k in els) els[k].classList.toggle("on", +k === sel); line.innerHTML = esc(MENU[sel][3]); };
    const open = () => go(MENU[sel][2], { from: "menu" });
    draw(); hints([["A", "Open"], ["B", "Close"]]);
    return { key: k => {
      if (k === "a") { open(); return true; } if (k === "b") { go("hud"); return true; }
      const top = MENU_MAIN.indexOf(sel) >= 0, row = top ? MENU_MAIN : MENU_SIDE, at = row.indexOf(sel);
      if (k === "left") sel = row[(at + row.length - 1) % row.length];
      else if (k === "right") sel = row[(at + 1) % row.length];
      else if (k === "up" || k === "down") sel = (top ? MENU_SIDE : MENU_MAIN)[at];
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
screen("dex", "Menus", "Pokédex", {
  scene: "model", sceneOpts: () => ({ sid: "pikachu", ox: -1.05 }),
  what: "Every POKéMON in order: those you've caught marked with a ball, those you've only seen in grey, the rest unknown. The one you're on turns on the right.",
  mod: "PokedexScreen: the list, SEEN and CAUGHT, and the entry.",
  try: ["Up and down, left and right a page, A for the entry."],
  make(arg) {
    backdrop("linear-gradient(160deg,#5AC8E8,#2E8FC0 55%,#1E5F96)");
    let sel = Math.max(0, DEXLIST.findIndex(x => x.sid === (arg && arg.sid || "pikachu"))), top = Math.max(0, sel - 4);
    header(ICON.dex, "POKéDEX", `SEEN ${SEEN.size}&nbsp;&nbsp;&nbsp;CAUGHT ${CAUGHT.size}`);
    const L = box(28, 110, 520, 560, "g-panel", ""); L.style.padding = "12px";
    const info = box(800, 560, 440, null, "g-panel soft", ""); info.style.padding = "12px 20px";
    const draw = () => {
      if (sel < top) top = sel; if (sel > top + 8) top = sel - 8;
      L.innerHTML = DEXLIST.slice(top, top + 9).map((x, j) => { const i = top + j, seen = SEEN.has(x.sid), got = CAUGHT.has(x.sid);
        return `<div class="g-item${i === sel ? " on" : ""}" style="min-height:54px;padding-top:2px;padding-bottom:2px"><span style="width:58px;opacity:.75;font-variant-numeric:tabular-nums">${String(x.n).padStart(3, "0")}</span><span style="width:48px;height:48px;display:inline-grid;place-items:center">${seen ? pic(x.sid, 48, got ? "" : "filter:brightness(0) opacity(.45)") : ""}</span><span style="${seen ? "" : "opacity:.45"}">${seen ? esc(ourName(x.sid)) : "?????"}</span><span class="r" style="width:22px;height:22px">${got ? ICON.ball : ""}</span></div>`; }).join("");
      const x = DEXLIST[sel], seen = SEEN.has(x.sid);
      info.innerHTML = seen ? `<div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap"><span class="g-title" style="font-size:26px">${esc(ourName(x.sid))}</span>${x.d.stats.types.map(typeChip).join("")}</div><div style="font-size:15px;font-weight:800;color:#A9D4C6">Stands in for #${String(x.n).padStart(3, "0")} ${esc(x.d.pname)}</div>` : `<span class="g-title" style="font-size:26px">?????</span>`;
      SC.model.hide = !seen; if (seen) SC.model.show(x.sid);
      [...L.children].forEach((c, j) => c.onclick = () => { sel = top + j; draw(); });
    };
    draw(); hints([["A", "Entry"], ["B", "Back"], ["TAB", "Caught only"]]);
    return { key: k => { if (k === "up" || k === "down") { sel = Math.max(0, Math.min(DEXLIST.length - 1, sel + (k === "down" ? 1 : -1))); draw(); return true; } if (k === "left" || k === "right") { sel = Math.max(0, Math.min(DEXLIST.length - 1, sel + (k === "right" ? 9 : -9))); draw(); return true; }
      if (k === "a" && SEEN.has(DEXLIST[sel].sid)) { go("dexentry", { sid: DEXLIST[sel].sid }); return true; } if (k === "b") { go("menu"); return true; } return true; } };
  }
});
screen("dexentry", "Menus", "Pokédex entry", {
  scene: "model", sceneOpts: () => ({ sid: "pikachu", ox: -1.05 }),
  what: "One POKéMON's page: its number and name, its types, how tall it is in blocks, what it's like, and where to find it.",
  mod: "PokedexScreen's entry, with POKéDEX VOICE reading it out (V).",
  make(arg) {
    const sid = arg && arg.sid || "pikachu", d = sp(sid);
    typeBackdrop(d.stats.types[0]);
    SC.model.show(sid).then(() => { const e = $("#hgt"); if (e && SC.model.st) e.textContent = SC.model.st.m.size.h.toFixed(2) + " blocks"; });
    box(28, 28, 600, 640, "g-panel g-fadein", `${typeBand(d.stats.types[0], 12, 12, 576, 96)}<div style="position:relative;padding:26px 30px;display:grid;gap:14px">
      <div class="g-label" style="color:rgba(255,255,255,.85)">No. ${String(d.dex).padStart(3, "0")}</div><div class="g-title" style="font-size:46px;line-height:1;text-shadow:0 2px 0 rgba(0,0,0,.33)">${esc(ourName(sid))}</div>
      <div style="display:flex;gap:8px">${d.stats.types.map(typeChip).join("")}</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px"><div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:16px"><div class="g-label">Height</div><div style="font-size:22px;font-weight:900" id="hgt">...</div></div>
        <div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:16px"><div class="g-label">Stands in for</div><div style="font-size:22px;font-weight:900">${esc(d.pname)}</div></div></div>
      <div style="font-size:22px;font-weight:800;line-height:1.5;color:#E8F4EF">${esc(dexText(sid))}</div>
      <div class="g-label" style="margin-top:4px">Where to find it</div><div style="font-size:19px;font-weight:800">${CAUGHT.has(sid) ? "ROUTE 1, ROUTE 2, VIRIDIAN FOREST" : "Not found yet"}</div></div>`);
    hints([["V", "Read it out"], ["B", "Back"]]);
    return { key: k => { if (k === "b") { go("dex", { sid }); return true; } return true; } };
  }
});
function monCard(p, on, i) {
  const f = p.hp / p.maxhp, d = sp(p.sid);
  return `<div class="g-panel" style="position:relative;height:262px;padding:14px 18px;border-radius:26px;${on ? "background:linear-gradient(180deg,#FFF8E6,#FFF0CC);color:#26304A;border-color:#fff;box-shadow:0 0 0 4px rgba(255,244,216,.4),0 14px 30px rgba(0,0,0,.35)" : ""}${p.status === "FNT" ? ";filter:saturate(.35)" : ""}">
    <div style="position:absolute;left:10px;top:8px;width:156px;height:156px;border-radius:26px;background:radial-gradient(circle at 50% 62%, ${on ? "rgba(255,212,90,.45)" : "rgba(143,245,192,.22)"}, transparent 68%)">${pic(p.sid, 156)}</div>
    <div style="margin-left:160px;display:grid;gap:8px;padding-top:4px">
      <div style="display:flex;align-items:center;gap:6px"><span class="g-title" style="font-size:25px;line-height:1.1">${esc(ourName(p.sid))}</span>${gender(p.gender)}</div>
      <div style="font-weight:900;font-size:20px">Lv. ${p.lv}</div>
      <div style="display:flex;gap:6px;flex-wrap:wrap">${d.stats.types.map(t => typeChip(t).replace("min-width:92px", "")).join("")}</div>
      <div>${statusChip(p.status)}${i === 0 ? `<span style="display:inline-block;margin-left:${p.status ? 6 : 0}px;padding:2px 10px;border-radius:999px;background:${on ? "#26304A" : "#8FF5C0"};color:${on ? "#FFF4D8" : "#123C38"};font-weight:900;font-size:12px;letter-spacing:.8px">FOLLOWING YOU</span>` : ""}</div></div>
    <div style="position:absolute;left:20px;right:20px;bottom:18px;display:flex;align-items:center;gap:10px"><span style="font-weight:900;font-size:14px;letter-spacing:1px;color:${on ? "#B8860B" : "#FFD45A"}">HP</span>
      <div class="g-hp" style="flex:1"><i style="width:${Math.max(0, f) * 100}%;background:${hpCol(f)}"></i></div><span style="font-weight:900;font-size:19px;font-variant-numeric:tabular-nums">${p.hp}/${p.maxhp}</span></div></div>`;
}
screen("party", "Menus", "Your team", {
  what: "Your six as cards, three across: a picture of each, name, level, types, HP and any status, and which one is following you. A on one for SUMMARY, SWITCH, FOLLOW ME.",
  mod: "MenuScreen's POKéMON, with SUMMARY, SWITCH, FOLLOW ME and CANCEL.",
  make() {
    backdrop("linear-gradient(160deg,#9EE3C9,#4FA88E 60%,#2B6E61)"); const C = ctl();
    header(ICON.ball, "POKéMON");
    let sel = 0;
    const grid = box(30, 100, 1220, 560, "", "");
    const draw = () => { grid.innerHTML = `<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:22px 22px">${G.party.map((p, i) => monCard(p, i === sel, i)).join("")}</div>`; [...grid.firstChild.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); menu(); }); };
    const menu = async () => {
      const i = await C.choose(["SUMMARY", "SWITCH", "FOLLOW ME", "CANCEL"], { x: 950, y: null, bottom: 30, w: 290 });
      if (i === 0) go("summary", { i: sel });
      else if (i === 2) { const tb = C.say([{ text: `**${ourName(G.party[sel].sid)}** will follow you now.` }]); }
    };
    draw(); hints([["A", "Choose"], ["B", "Back"]]);
    const nav = { key(k) { if (k === "a") { menu(); return true; } if (k === "b") { go("menu", { sel: 1 }); return true; } const n = sel; sel = gridMove(sel, 6, 3, k); if (n !== sel) draw(); return true; } };
    C.push(nav);
    return { key: k => C.key(k) };
  }
});
screen("summary", "Menus", "Summary", {
  scene: "model", sceneOpts: () => ({ sid: G.party[0].sid, ox: -1.05 }),
  what: "One of your team in full: its stats as bars, its moves with their types and PP, its EXP to the next level.",
  mod: "MenuScreen's SUMMARY: ATTACK, DEFENSE, SPCL.ATK, SPCL.DEF, SPEED.",
  try: ["Tab, or left and right, for the next page. Up and down for the next of your team."],
  make(arg) {
    let i = arg && arg.i || 0, page = 0;
    const P = box(28, 28, 640, 660, "g-panel", ""); P.style.padding = "24px 28px"; P.style.boxSizing = "border-box";
    const draw = () => {
      const p = G.party[i], d = sp(p.sid); SC.model.show(p.sid); typeBackdrop(d.stats.types[0]);
      const tc = typeHex(d.stats.types[0]);
      const tabs = ["INFO", "STATS", "MOVES"].map((t, j) => `<span class="g-chip${j === page ? " on" : ""}" style="font-size:15px">${t}</span>`).join("");
      let body = "";
      if (page === 0) body = `<div style="display:grid;grid-template-columns:auto 1fr;gap:10px 22px;font-size:21px;font-weight:800"><span class="g-label">No.</span><span>${String(d.dex).padStart(3, "0")}</span><span class="g-label">Types</span><span>${d.stats.types.map(typeChip).join(" ")}</span>
        <span class="g-label">Trainer</span><span>${esc(G.name)}</span><span class="g-label">ID No.</span><span>${G.id}</span><span class="g-label">Item</span><span>None</span><span class="g-label">Met</span><span>ROUTE 1, at Lv. ${Math.max(2, p.lv - 18)}</span></div>
        <div style="margin-top:16px"><div class="g-label">EXP to Lv. ${p.lv + 1}</div><div class="g-exp" style="margin-top:6px"><i style="width:${p.exp * 100}%"></i></div></div>`;
      else if (page === 1) { const st = p.stats, names = [["HP", "HP"], ["Attack", "ATTACK"], ["Defense", "DEFENSE"], ["Sp. Atk", "SPCL.ATK"], ["Sp. Def", "SPCL.DEF"], ["Speed", "SPEED"]];
        body = `<div style="display:grid;gap:12px">${names.map(([k, l]) => `<div style="display:grid;grid-template-columns:150px 70px 1fr;align-items:center;gap:12px;font-size:19px;font-weight:900"><span class="g-label">${l}</span><span style="text-align:right">${k === "HP" ? p.hp + "/" + st[k] : st[k]}</span><div class="g-hp" style="height:16px"><i style="width:${Math.min(100, st[k] / (p.lv * 2.6 + 30) * 100)}%;background:linear-gradient(180deg,${lighterHex(tc, 0.45)},${tc})"></i></div></div>`).join("")}</div>`; }
      else body = `<div style="display:grid;gap:10px">${p.moves.map(n => { const m = MOVE[n] || { type: "NORMAL", pp: 20, category: "Physical", power: 40 }; return `<div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:18px;display:flex;align-items:center;gap:14px">${typeChip(m.type)}<span style="font-size:21px;font-weight:900">${esc(n)}</span><span style="margin-left:auto;font-weight:900;font-variant-numeric:tabular-nums">PP ${m.pp}/${m.pp}</span></div>`; }).join("")}</div>`;
      P.innerHTML = `${typeBand(d.stats.types[0], 12, 12, 616, 64)}<div style="position:relative;display:flex;align-items:center;gap:12px;text-shadow:0 2px 0 rgba(0,0,0,.33)"><span class="g-title" style="font-size:36px">${esc(ourName(p.sid))}</span>${gender(p.gender)}${statusChip(p.status)}<span style="margin-left:auto;font-size:24px;font-weight:900">Lv. ${p.lv}</span></div>
        <div style="display:flex;gap:8px;margin:14px 0 18px">${tabs}</div>${body}`;
    };
    draw(); hints([["TAB", "Page"], ["B", "Back"]]);
    return { key: k => { if (k === "tab" || k === "right") { page = (page + 1) % 3; draw(); return true; } if (k === "left") { page = (page + 2) % 3; draw(); return true; } if (k === "up" || k === "down") { i = (i + (k === "down" ? 1 : 5)) % 6; draw(); return true; } if (k === "b") { go("party"); return true; } return true; } };
  }
});
screen("bag", "Menus", "Bag", {
  what: "Your bag with its four pockets: ITEMS (the main body), BALLS (the front), KEY ITEMS (the lid), TM & HM (the side). What's in the picked one is listed, with what it does.",
  mod: "MenuScreen's BAG: the backpack in its colour and its four pockets.",
  try: ["Left and right (or Tab) for a pocket, up and down for an item, A to use."],
  make() {
    backdrop("linear-gradient(160deg,#FFE7B8,#F6C46B 55%,#D99A3A)"); const C = ctl();
    const pockets = Object.keys(G.bag); let pk = 0, sel = 0;
    header(ICON.bag, "BAG");
    const bag = box(48, 96, 400, 540, "", ""); const L = box(480, 110, 740, 440, "g-panel", ""); L.style.padding = "14px";
    const desc = box(480, 570, 740, 110, "g-panel soft", ""); desc.style.padding = "18px 26px"; desc.style.fontSize = "21px"; desc.style.fontWeight = "800";
    add(h("div", "", pic(G.party[0].sid, 680, "position:absolute;right:-90px;bottom:-120px;opacity:.09;filter:saturate(.6)"))).style.cssText = "position:absolute;inset:0;pointer-events:none;overflow:hidden";
    uiRoot.insertBefore(uiRoot.lastChild, uiRoot.firstChild);
    const draw = () => {
      const col = hex(colourOf(G.outfit.bagc)), dk = darkerHex(col, 0.22), dd = darkerHex(col, 0.38), lt = lighterHex(col, 0.18), on = { "ITEMS": "body", "BALLS": "front", "KEY ITEMS": "lid", "TM & HM": "side" }[pockets[pk]];
      const hl = p => p === on ? `stroke="#FFF4D8" stroke-width="6" filter="url(#gl)"` : `stroke="${dd}" stroke-width="3"`;
      const lift = p => p === on ? `transform="translate(0,-6)"` : "";
      bag.innerHTML = `<svg viewBox="0 0 400 500" width="400" height="500"><defs><filter id="gl" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0" dy="0" stdDeviation="7" flood-color="#FFF4D8" flood-opacity=".9"/></filter>
          <pattern id="weave" width="8" height="8" patternUnits="userSpaceOnUse"><rect width="8" height="8" fill="${col}"/><rect width="4" height="4" fill="${lt}" opacity=".25"/><rect x="4" y="4" width="4" height="4" fill="${dk}" opacity=".18"/></pattern></defs>
        <rect x="150" y="18" width="100" height="56" rx="18" fill="none" stroke="${dd}" stroke-width="14"/>
        <rect x="64" y="96" width="44" height="300" rx="10" fill="${dk}"/><rect x="292" y="96" width="44" height="300" rx="10" fill="${dk}"/>
        <g ${lift("body")}><rect x="60" y="70" width="280" height="390" rx="30" fill="url(#weave)" ${hl("body")}/>
          <rect x="76" y="86" width="248" height="358" rx="22" fill="none" stroke="${dd}" stroke-width="2.5" stroke-dasharray="8 7" opacity=".7"/><rect x="60" y="430" width="280" height="30" rx="14" fill="${dd}" opacity=".55"/></g>
        <g ${lift("side")}><rect x="318" y="230" width="62" height="170" rx="14" fill="${dk}" ${hl("side")}/><rect x="326" y="210" width="46" height="34" rx="8" fill="#5A8EA1"/><circle cx="349" cy="227" r="12" fill="#9FD3E2"/><circle cx="349" cy="227" r="4" fill="#123C38"/>
          <rect x="324" y="300" width="50" height="6" rx="3" fill="${dd}"/></g>
        <g ${lift("front")}><rect x="100" y="262" width="200" height="160" rx="26" fill="${lt}" ${hl("front")}/><path d="M118 300 H282" stroke="${dd}" stroke-width="5" stroke-linecap="round"/><rect x="190" y="292" width="20" height="16" rx="4" fill="#F4F6F5" stroke="${dd}" stroke-width="2"/>
          <g transform="translate(200 362)"><circle r="28" fill="#F4F6F5"/><path d="M-28 0 a28 28 0 0 1 56 0z" fill="#E5525C"/><rect x="-28" y="-3" width="56" height="6" fill="#26304A"/><circle r="9" fill="#26304A"/><circle r="5" fill="#fff"/></g></g>
        <g ${lift("lid")}><path d="M60 140 V108 Q60 66 104 64 H296 Q340 66 340 108 V140 Q340 200 290 206 H110 Q60 200 60 140Z" fill="${dk}" ${hl("lid")}/>
          <path d="M80 120 Q200 96 320 120" stroke="${lt}" stroke-width="3" fill="none" opacity=".5"/><rect x="176" y="176" width="48" height="46" rx="9" fill="#FFD45A" stroke="#B8860B" stroke-width="3"/><rect x="190" y="190" width="20" height="18" rx="4" fill="#B8860B"/></g>
      </svg>
      <div style="position:absolute;left:0;right:0;top:500px;display:flex;justify-content:center;gap:6px">${pockets.map((p, i) => `<span class="g-chip${i === pk ? " on" : ""}" style="font-size:13px;padding:3px 10px">${esc(p)}</span>`).join("")}</div>`;
      const items = G.bag[pockets[pk]];
      L.innerHTML = items.map(([n, c], i) => `<div style="margin-bottom:8px">${goodsRow(n, "x" + c, i === sel)}</div>`).join("") || `<div style="padding:20px;font-size:21px;font-weight:800;color:#A9D4C6">Empty</div>`;
      desc.innerHTML = items[sel] ? esc(items[sel][2]) : "";
      [...L.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); use(); });
    };
    const use = async () => {
      const it = G.bag[pockets[pk]][sel]; if (!it) return;
      const i = await C.choose(pockets[pk] === "KEY ITEMS" ? ["USE", "REGISTER", "CANCEL"] : ["USE", "GIVE", "TOSS", "CANCEL"], { x: 940, y: 300, w: 260 });
      if (i === 0) C.say([{ text: pockets[pk] === "BALLS" ? "You can only throw that in a battle with a wild POKéMON!" : `Which POKéMON should get the **${it[0]}**?` }]);
    };
    draw(); hints([["A", "Choose"], ["B", "Back"], ["TAB", "Pocket"]]);
    C.push({ key(k) { if (k === "left" || k === "right" || k === "tab") { pk = (pk + (k === "left" ? pockets.length - 1 : 1)) % pockets.length; sel = 0; draw(); return true; }
      if (k === "up" || k === "down") { const n = G.bag[pockets[pk]].length; sel = (sel + (k === "down" ? 1 : n - 1)) % n; draw(); return true; } if (k === "a") { use(); return true; } if (k === "b") { go("menu", { sel: 2 }); return true; } return false; } });
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
      rr(x - w / 2, y, w, hh, hh / 2); ctx.fillStyle = "rgba(18,60,56,.88)"; ctx.fill(); ctx.strokeStyle = "#4FB896"; ctx.lineWidth = 1.5; ctx.stroke();
      ctx.fillStyle = "#FFF4D8"; ctx.textAlign = "center"; ctx.textBaseline = "middle"; ctx.fillText(text, x, y + hh / 2 + 0.5);
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
        else if (p.kind === "spot") { ctx.save(); ctx.translate(sx, sy); ctx.rotate(Math.PI / 4); ctx.fillStyle = on ? "#FFD45A" : "#8FF5C0"; ctx.fillRect(-5, -5, 10, 10); ctx.strokeStyle = "#123C38"; ctx.lineWidth = 2; ctx.strokeRect(-5, -5, 10, 10); ctx.restore(); }
        else { ctx.beginPath(); ctx.arc(sx, sy, on ? 6 : 4, 0, 7); ctx.fillStyle = on ? "#FFD45A" : "#FFF4D8"; ctx.fill(); ctx.lineWidth = 2; ctx.strokeStyle = "#123C38"; ctx.stroke(); }
        if (on) { ctx.beginPath(); ctx.arc(sx, sy, 15 + pulse * 5, 0, 7); ctx.strokeStyle = `rgba(255,212,90,${0.9 - pulse * 0.5})`; ctx.lineWidth = 3; ctx.stroke(); }
        if (p.kind === "town" || on || zi >= 2) label(p.name, sx, sy + (p.kind === "town" ? 12 : 9), p.kind === "town");
      });
      // You: the arrow, the way you face.
      const [yx, yy] = toScreen(YOU.x, YOU.z);
      ctx.save(); ctx.translate(yx, yy); ctx.rotate(YOU.yaw); ctx.beginPath(); ctx.moveTo(0, -11); ctx.lineTo(8, 8); ctx.lineTo(0, 4); ctx.lineTo(-8, 8); ctx.closePath();
      ctx.fillStyle = "#FFD45A"; ctx.fill(); ctx.lineWidth = 2.5; ctx.strokeStyle = "#26304A"; ctx.stroke(); ctx.restore();
      // Scale bar.
      const blocks = zi === 0 ? 200 : zi === 1 ? 100 : zi === 2 ? 50 : 20, bw = blocks * z;
      ctx.fillStyle = "rgba(18,60,56,.8)"; rr(14, VH - 40, bw + 90, 26, 13); ctx.fill(); ctx.fillStyle = "#FFF4D8"; ctx.fillRect(26, VH - 28, bw, 4);
      ctx.font = "900 12px Nunito, system-ui, sans-serif"; ctx.textAlign = "left"; ctx.textBaseline = "middle"; ctx.fillText(blocks + " blocks", 34 + bw, VH - 26);
      requestAnimationFrame(paint);
    };
    const drawInfo = () => {
      const p = P[sel], been = BEEN.includes(p.key), here = TOWN_HERE[p.name] || p.here || [];
      const wild = p.wild.slice(0, GYMS[p.name] ? 4 : 12);
      info.innerHTML = `<div class="g-label">${p.key === "pallet_town" ? "You are here" : KIND[p.kind] || "Place"}</div>
        <div class="g-title" style="font-size:${p.name.length > 14 ? 27 : 32}px;line-height:1.1;margin:6px 0 10px">${esc(p.name)}</div>
        ${p.tag ? `<div style="font-size:18px;font-weight:800;line-height:1.45;color:#E8F4EF;font-style:italic">"${esc(p.tag)}"</div>` : ""}
        ${here.length ? `<div class="g-label" style="margin-top:16px">Here</div><div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:7px">${here.map(x => `<span style="display:inline-flex;align-items:center;gap:7px;font-size:14.5px;font-weight:900;padding:4px 11px;border-radius:999px;background:rgba(0,0,0,.18)"><span style="width:8px;height:8px;border-radius:3px;background:#8FF5C0"></span>${esc(x)}</span>`).join("")}</div>` : ""}
        ${GYMS[p.name] ? (([lead, type, b, lv]) => `<div class="g-label" style="margin-top:16px">Gym</div><div style="display:grid;grid-template-columns:58px 1fr;gap:4px 12px;align-items:center;margin-top:8px;padding:10px 12px;border-radius:16px;background:rgba(0,0,0,.18)">
          <span style="grid-row:span 3;width:58px;height:58px">${badgeSvg(b)}</span><span style="font-size:19px;font-weight:900">${esc(lead)}</span>
          <span style="font-size:14px;font-weight:900;color:#A9D4C6">${esc(type)} type · ${esc(BADGES[b][0])}</span><span style="font-size:14px;font-weight:800;color:#E8F4EF">Team from level ${esc(lv)}</span></div>`)(GYMS[p.name]) : ""}
        ${wild.length ? `<div class="g-label" style="margin-top:16px">Wild POKéMON</div><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px">${wild.map(sid => DATA.species[sid]
          ? `<div title="${esc(DATA.species[sid].pname)}" style="aspect-ratio:1;border-radius:12px;background:rgba(0,0,0,.18);display:grid;place-items:center">${pic(sid, 56)}</div>`
          : `<div title="${esc(sid)}" style="aspect-ratio:1;border-radius:12px;background:rgba(0,0,0,.18);display:grid;place-items:center;font-size:26px;font-weight:900;color:#5E8E82">?</div>`).join("")}</div>` : ""}
        <div style="margin-top:auto;padding-top:12px;display:grid;gap:8px">
          ${p.kind === "town" ? (been ? `<div class="g-chip on" style="justify-content:center;font-size:17px;padding:8px">A: FLY HERE</div>` : `<div class="g-chip" style="justify-content:center;font-size:15px;padding:8px;opacity:.7">Not been here yet</div>`) : ""}
          <div style="font-size:12.5px;font-weight:800;color:#A9D4C6;line-height:1.35">${p.zone ? `Zone in the mod: ${p.zone[0]}, ${p.zone[1]} to ${p.zone[2]}, ${p.zone[3]}` : `About ${p.x}, ${p.z} on your map`}</div></div>`;
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
    backdrop("linear-gradient(160deg,#BFD8F7,#4A8FE0 60%,#2F5D96)");
    box(470, 60, 770, 600, "g-panel g-fadein", `<div style="padding:28px 34px;display:grid;gap:16px"><div style="display:flex;align-items:center;gap:14px"><span style="width:46px;height:46px">${ICON.card}</span><span class="g-title" style="font-size:34px">TRAINER CARD</span></div>
      <div style="display:grid;grid-template-columns:200px 1fr;gap:12px 20px;font-size:23px;font-weight:900">
      <span class="g-label">Name</span><span>${esc(G.name)}</span><span class="g-label">ID No.</span><span>${G.id}</span><span class="g-label">Money</span><span>${money(G.money)}</span>
      <span class="g-label">Pokédex</span><span>${G.caught}</span><span class="g-label">Play time</span><span>${G.time}</span><span class="g-label">Started</span><span>${G.started}</span></div>
      <div class="g-label" style="margin-top:6px">Badges</div><div style="display:grid;grid-template-columns:repeat(8,1fr);gap:10px">${BADGES.map(([n, c], i) => `<div title="${n}" style="aspect-ratio:1;border-radius:18px;background:rgba(0,0,0,.2);display:grid;place-items:center;${i < G.badges ? "" : "filter:grayscale(1) brightness(.5);opacity:.55"}"><span style="width:66px;height:66px">${badgeSvg(i)}</span></div>`).join("")}</div></div>`);
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
  scene: "town", sceneOpts: () => ({}),
  what: "Saving: your save card, are you sure, then a moment while it writes.",
  mod: "MenuScreen's SAVE, YES or NO.",
  make() {
    blurOver(); const C = ctl();
    box(330, 70, 620, 300, "g-panel g-pop", `<div style="padding:24px 30px;display:grid;grid-template-columns:auto 1fr;gap:10px 24px;font-size:22px;font-weight:900"><span class="g-label">Player</span><span>${esc(G.name)}</span><span class="g-label">Place</span><span>${esc(G.place)}</span><span class="g-label">Badges</span><span>${G.badges}</span><span class="g-label">Pokédex</span><span>${G.caught}</span><span class="g-label">Time</span><span>${G.time}</span></div>`);
    (async () => {
      const v = await C.say([{ text: "Would you like to save the game?", ask: ["YES", "NO"] }]);
      if (v !== 0) { if (C.alive()) go("menu", { sel: 7 }); return; }
      const sp_ = box(600, 400, 80, 80, "", `<div style="width:80px;height:80px;animation:spin 1s linear infinite">${ICON.save}</div><style>@keyframes spin{to{transform:rotate(360deg)}}</style>`);
      await C.say([{ text: "Saving... Don't turn off the power." }], { keep: true });
      await C.wait(300); sp_.remove(); uiRoot.querySelectorAll(".g-text").forEach(e => e.remove());
      await C.say([{ text: `**${G.name}** saved the game.` }]); if (C.alive()) go("hud");
    })();
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
