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
      const e = box(0, 0, 56, 56, "g-pop", `<div style="width:56px;height:56px;border-radius:50%;background:#fff;display:grid;place-items:center;font-size:30px;color:#E5525C;box-shadow:0 6px 14px rgba(0,0,0,.3)">&#9834;</div>`);
      const p = V(8.1, 1.35, 11.4).project(cam3); e.style.left = ((p.x + 1) / 2 * 1280 - 28) + "px"; e.style.top = ((1 - p.y) / 2 * 720 - 70) + "px";
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
const MENU = [["POKéDEX", "dex", "dex", "Every POKéMON you've seen and caught."], ["POKéMON", "ball", "party", "Your team: check them, switch them, pick who follows you."], ["BAG", "bag", "bag", "Your items, in four pockets."],
  ["MAP", "map", "map", "The TOWN MAP: where you are, and where everywhere else is."], ["CLOTHES", "cap", "clothes", "Change what you're wearing."], ["CARD", "card", "card", "Your TRAINER CARD and badges."],
  ["WAIT", "clock", "wait", "Let time pass, to morning, midday, evening or night."], ["SAVE", "save", "save", "Save your game."], ["OPTION", "cog", "options", "Text speed, music, nights and more."], ["EXIT", "close", "hud", "Close the menu."]];
screen("menu", "In the world", "The menu", {
  scene: "town", sceneOpts: () => ({}),
  what: "The menu over the blurred world: big round tiles, our own symbols, no Communicate and no Play-with tile.",
  mod: "MenuScreen: POKéDEX, POKéMON, BAG, MAP, CLOTHES, CARD, WAIT, SAVE, OPTION, EXIT, in a grid.",
  tags: ["New look"], try: ["Arrows to move, A to open, B to close."],
  make(arg) {
    blurOver(); let sel = arg && arg.sel || 0;
    box(0, 30, 1280, null, "", `<div class="g-title" style="text-align:center;font-size:30px;letter-spacing:8px;text-shadow:0 3px 8px rgba(0,0,0,.5)">MENU</div>`);
    const tiles = MENU.map(([n, ic], i) => { const t = h("div", "g-tile", `<div class="disc">${ICON[ic]}</div><div class="nm">${esc(n)}</div>`); t.style.left = (110 + (i % 5) * 220) + "px"; t.style.top = (130 + Math.floor(i / 5) * 230) + "px"; t.onmouseenter = () => { sel = i; draw(); }; t.onclick = () => { sel = i; open(); }; return add(t); });
    const line = box(240, 600, 800, null, "g-panel soft", ""); line.style.padding = "14px 26px"; line.style.textAlign = "center"; line.style.fontSize = "21px"; line.style.fontWeight = "800";
    const draw = () => { tiles.forEach((t, i) => t.classList.toggle("on", i === sel)); line.innerHTML = esc(MENU[sel][3]); };
    const open = () => go(MENU[sel][2], { from: "menu" });
    draw(); hints([["A", "Open"], ["B", "Close"]]);
    return { key: k => { if (k === "a") { open(); return true; } if (k === "b") { go("hud"); return true; } const n = sel; sel = gridMove(sel, MENU.length, 5, k); if (n !== sel) draw(); return true; } };
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
        return `<div class="g-item${i === sel ? " on" : ""}" style="min-height:54px"><span style="width:62px;opacity:.75;font-variant-numeric:tabular-nums">${String(x.n).padStart(3, "0")}</span><span style="width:22px;height:22px;display:inline-block">${got ? ICON.ball : ""}</span><span style="${seen ? "" : "opacity:.45"}">${seen ? esc(ourName(x.sid)) : "?????"}</span></div>`; }).join("");
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
    backdrop("linear-gradient(160deg,#5AC8E8,#2E8FC0 55%,#1E5F96)");
    SC.model.show(sid).then(() => { const e = $("#hgt"); if (e && SC.model.st) e.textContent = SC.model.st.m.size.h.toFixed(2) + " blocks"; });
    box(28, 28, 600, 640, "g-panel g-fadein", `<div style="padding:26px 30px;display:grid;gap:14px">
      <div class="g-label">No. ${String(d.dex).padStart(3, "0")}</div><div class="g-title" style="font-size:46px;line-height:1">${esc(ourName(sid))}</div>
      <div style="display:flex;gap:8px">${d.stats.types.map(typeChip).join("")}</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px"><div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:16px"><div class="g-label">Height</div><div style="font-size:22px;font-weight:900" id="hgt">...</div></div>
        <div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:16px"><div class="g-label">Stands in for</div><div style="font-size:22px;font-weight:900">${esc(d.pname)}</div></div></div>
      <div style="font-size:22px;font-weight:800;line-height:1.5;color:#E8F4EF">${esc(dexText(sid))}</div>
      <div class="g-label" style="margin-top:4px">Where to find it</div><div style="font-size:19px;font-weight:800">${CAUGHT.has(sid) ? "ROUTE 1, ROUTE 2, VIRIDIAN FOREST" : "Not found yet"}</div></div>`);
    hints([["V", "Read it out"], ["B", "Back"]]);
    return { key: k => { if (k === "b") { go("dex", { sid }); return true; } return true; } };
  }
});
function monCard(p, on, w = 560) {
  const f = p.hp / p.maxhp;
  return `<div class="g-panel" style="position:relative;width:${w}px;padding:14px 20px 14px 92px;border-radius:22px;${on ? "background:linear-gradient(180deg,#FFF8E6,#FFF0CC);color:#26304A;border-color:#fff;box-shadow:0 0 0 4px rgba(255,244,216,.4),0 14px 30px rgba(0,0,0,.35)" : ""}${p.status === "FNT" ? ";filter:saturate(.4)" : ""}">
    <div style="position:absolute;left:16px;top:50%;transform:translateY(-50%);width:62px;height:62px;border-radius:50%;background:${TYPECOL(sp(p.sid).stats.types[0])};display:grid;place-items:center;font-family:Fredoka;font-weight:700;font-size:26px;color:#fff;box-shadow:inset 0 -4px 0 rgba(0,0,0,.2)">${esc(ourName(p.sid)[0])}</div>
    <div style="display:flex;align-items:center;gap:10px"><span class="g-title" style="font-size:25px">${esc(ourName(p.sid))}</span>${gender(p.gender)}${statusChip(p.status)}<span style="margin-left:auto;font-weight:900;font-size:19px">Lv. ${p.lv}</span></div>
    <div style="display:flex;align-items:center;gap:12px;margin-top:6px"><span style="font-weight:900;font-size:14px;letter-spacing:1px;opacity:.75">HP</span>${hpBar(f, w - 260)}<span style="font-weight:900;font-size:18px;font-variant-numeric:tabular-nums">${p.hp}/${p.maxhp}</span></div></div>`;
}
screen("party", "Menus", "Your team", {
  what: "Your six as cards: name, level, HP and any status. A on one for SUMMARY, SWITCH, FOLLOW ME.",
  mod: "MenuScreen's POKéMON, with SUMMARY, SWITCH, FOLLOW ME and CANCEL.",
  make() {
    backdrop("linear-gradient(160deg,#9EE3C9,#4FA88E 60%,#2B6E61)"); const C = ctl();
    header(ICON.ball, "POKéMON");
    let sel = 0;
    const grid = box(60, 110, 1160, 520, "", "");
    const draw = () => { grid.innerHTML = `<div style="display:grid;grid-template-columns:1fr 1fr;gap:22px 30px">${G.party.map((p, i) => monCard(p, i === sel)).join("")}</div>`; [...grid.firstChild.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); menu(); }); };
    const menu = async () => {
      const i = await C.choose(["SUMMARY", "SWITCH", "FOLLOW ME", "CANCEL"], { x: 930, y: 380, w: 290 });
      if (i === 0) go("summary", { i: sel });
      else if (i === 2) { const tb = C.say([{ text: `**${ourName(G.party[sel].sid)}** will follow you now.` }]); }
    };
    draw(); hints([["A", "Choose"], ["B", "Back"]]);
    const nav = { key(k) { if (k === "a") { menu(); return true; } if (k === "b") { go("menu", { sel: 1 }); return true; } const n = sel; sel = gridMove(sel, 6, 2, k); if (n !== sel) draw(); return true; } };
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
    backdrop("linear-gradient(160deg,#FFE7B8,#F6B26B 55%,#D9733A)");
    const P = box(28, 28, 640, 660, "g-panel", ""); P.style.padding = "24px 28px";
    const draw = () => {
      const p = G.party[i], d = sp(p.sid); SC.model.show(p.sid);
      const tabs = ["INFO", "STATS", "MOVES"].map((t, j) => `<span class="g-chip${j === page ? " on" : ""}" style="font-size:15px">${t}</span>`).join("");
      let body = "";
      if (page === 0) body = `<div style="display:grid;grid-template-columns:auto 1fr;gap:10px 22px;font-size:21px;font-weight:800"><span class="g-label">No.</span><span>${String(d.dex).padStart(3, "0")}</span><span class="g-label">Types</span><span>${d.stats.types.map(typeChip).join(" ")}</span>
        <span class="g-label">Trainer</span><span>${esc(G.name)}</span><span class="g-label">ID No.</span><span>${G.id}</span><span class="g-label">Item</span><span>None</span><span class="g-label">Met</span><span>ROUTE 1, at Lv. ${Math.max(2, p.lv - 18)}</span></div>
        <div style="margin-top:16px"><div class="g-label">EXP to Lv. ${p.lv + 1}</div><div class="g-exp" style="margin-top:6px"><i style="width:${p.exp * 100}%"></i></div></div>`;
      else if (page === 1) { const st = p.stats, names = [["HP", "HP"], ["Attack", "ATTACK"], ["Defense", "DEFENSE"], ["Sp. Atk", "SPCL.ATK"], ["Sp. Def", "SPCL.DEF"], ["Speed", "SPEED"]];
        body = `<div style="display:grid;gap:12px">${names.map(([k, l]) => `<div style="display:grid;grid-template-columns:150px 70px 1fr;align-items:center;gap:12px;font-size:19px;font-weight:900"><span class="g-label">${l}</span><span style="text-align:right">${k === "HP" ? p.hp + "/" + st[k] : st[k]}</span><div class="g-hp" style="height:16px"><i style="width:${Math.min(100, st[k] / (p.lv * 2.6 + 30) * 100)}%;background:linear-gradient(90deg,#4FB896,#8FF5C0)"></i></div></div>`).join("")}</div>`; }
      else body = `<div style="display:grid;gap:10px">${p.moves.map(n => { const m = MOVE[n] || { type: "NORMAL", pp: 20, category: "Physical", power: 40 }; return `<div class="g-panel soft" style="position:relative;padding:10px 16px;border-radius:18px;display:flex;align-items:center;gap:14px">${typeChip(m.type)}<span style="font-size:21px;font-weight:900">${esc(n)}</span><span style="margin-left:auto;font-weight:900;font-variant-numeric:tabular-nums">PP ${m.pp}/${m.pp}</span></div>`; }).join("")}</div>`;
      P.innerHTML = `<div style="display:flex;align-items:center;gap:12px"><span class="g-title" style="font-size:36px">${esc(ourName(p.sid))}</span>${gender(p.gender)}${statusChip(p.status)}<span style="margin-left:auto;font-size:24px;font-weight:900">Lv. ${p.lv}</span></div>
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
    const bag = box(60, 130, 380, 470, "", ""); const L = box(480, 110, 740, 440, "g-panel", ""); L.style.padding = "14px";
    const desc = box(480, 570, 740, 110, "g-panel soft", ""); desc.style.padding = "18px 26px"; desc.style.fontSize = "21px"; desc.style.fontWeight = "800";
    const draw = () => {
      const col = G.look.bag || "#E9B43C", dk = darkerHex(col, 0.25), parts = { "ITEMS": "body", "BALLS": "front", "KEY ITEMS": "lid", "TM & HM": "side" }, on = parts[pockets[pk]];
      const glow = p => p === on ? `stroke="#FFF4D8" stroke-width="7"` : `stroke="${dk}" stroke-width="3"`;
      bag.innerHTML = `<svg viewBox="0 0 380 470" width="380" height="470"><path d="M120 90 a70 60 0 0 1 140 0" fill="none" stroke="${dk}" stroke-width="18"/>
        <rect x="60" y="80" width="260" height="360" rx="70" fill="${col}" ${glow("body")}/><path d="M70 160 Q190 80 310 160 L310 120 Q190 40 70 120 Z" fill="${dk}" ${glow("lid")}/>
        <rect x="105" y="250" width="170" height="140" rx="40" fill="${darkerHex(col, 0.12)}" ${glow("front")}/><rect x="300" y="200" width="60" height="170" rx="26" fill="${darkerHex(col, 0.18)}" ${glow("side")}/>
        <rect x="175" y="240" width="30" height="40" rx="10" fill="#26304A"/></svg>
        <div style="position:absolute;left:0;right:0;top:450px;display:flex;justify-content:center;gap:8px">${pockets.map((p, i) => `<span class="g-chip${i === pk ? " on" : ""}" style="font-size:14px;padding:4px 10px">${esc(p)}</span>`).join("")}</div>`;
      const items = G.bag[pockets[pk]];
      L.innerHTML = items.map(([n, c], i) => `<div class="g-item${i === sel ? " on" : ""}"><span style="width:34px;height:34px">${ITEM_ICON(n)}</span><span>${esc(n)}</span><span class="r">x${c}</span></div>`).join("") || `<div style="padding:20px;font-size:21px;font-weight:800;color:#A9D4C6">Empty</div>`;
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
const TOWNS = [["PALLET TOWN", 210, 520, "A quiet start to a long road. Your home, and PROF. OAK's lab.", ["PROF. OAK's LAB", "Your house", "Rival's house"]],
  ["VIRIDIAN CITY", 210, 400, "The city of green, where the grass is always fresh.", ["POKéMON CENTER", "POKé MART", "GYM"]], ["PEWTER CITY", 210, 170, "A stone-grey city between the rough mountains.", ["POKéMON CENTER", "POKé MART", "GYM", "MUSEUM"]],
  ["CERULEAN CITY", 610, 150, "A blue city by the water, and its cave of mysteries.", ["POKéMON CENTER", "POKé MART", "GYM", "BIKE SHOP"]], ["SAFFRON CITY", 610, 300, "The golden city of business, right in the middle of Kanto.", ["POKéMON CENTER", "POKé MART", "GYM", "SILPH CO."]],
  ["VERMILION CITY", 610, 460, "The port city, where the ships come in at sunset.", ["POKéMON CENTER", "POKé MART", "GYM", "FAN CLUB"]], ["LAVENDER TOWN", 860, 300, "A quiet town of purple light and old stories.", ["POKéMON CENTER", "POKé MART", "TOWER"]],
  ["CELADON CITY", 420, 300, "The city of rainbows, with its great department store.", ["POKéMON CENTER", "DEPT. STORE", "GYM", "GAME CORNER"]], ["FUCHSIA CITY", 520, 600, "A pink city by the safari, full of rare POKéMON.", ["POKéMON CENTER", "POKé MART", "GYM", "SAFARI ZONE"]],
  ["CINNABAR ISLAND", 210, 640, "A hot island with a volcano and a lab.", ["POKéMON CENTER", "POKé MART", "GYM", "LAB"]], ["INDIGO PLATEAU", 80, 120, "Where the best trainers in Kanto go to face the ELITE FOUR.", ["POKéMON CENTER", "POKéMON LEAGUE"]]];
screen("map", "Menus", "Town Map", {
  what: "Kanto, drawn our own way: towns and routes, where you are, and what's in the town you point at.",
  mod: "TownMapScreen: zoom with E and Q, C to centre on you, Tab for the next town.",
  try: ["Arrows to move between towns."],
  make() {
    backdrop("linear-gradient(180deg,#5BA3D9,#3F86C2)");
    let sel = 0;
    const roads = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [4, 6], [4, 7], [1, 7], [5, 8], [8, 9], [0, 9], [3, 6], [6, 8], [1, 10]];
    const M = box(20, 20, 900, 680, "", "");
    const info = box(940, 40, 320, 640, "g-panel", ""); info.style.padding = "22px 24px";
    const draw = () => {
      M.innerHTML = `<svg viewBox="0 0 900 680" width="900" height="680"><defs><pattern id="wv" width="40" height="20" patternUnits="userSpaceOnUse"><path d="M0 10 q10 -6 20 0 t20 0" stroke="rgba(255,255,255,.25)" fill="none" stroke-width="2"/></pattern></defs>
        <rect width="900" height="680" fill="url(#wv)"/>
        <path d="M60 60 L760 70 Q900 120 880 300 L860 420 Q820 560 660 640 L360 650 Q260 560 160 600 L60 560 Q20 300 60 60Z" fill="#7CC46A" stroke="#F4F0E6" stroke-width="6"/>
        <path d="M120 120 L330 120 L330 260 L180 260Z" fill="#9C8A6A" opacity=".55"/><path d="M700 120 L820 160 L800 260 L690 240Z" fill="#5E9C4E" opacity=".6"/><circle cx="470" cy="480" r="60" fill="#4E9A48" opacity=".5"/>
        <path d="M170 610 L250 610 L250 670 L170 670Z" fill="#7CC46A" stroke="#F4F0E6" stroke-width="5"/>
        ${roads.map(([a, b]) => `<line x1="${TOWNS[a][1]}" y1="${TOWNS[a][2]}" x2="${TOWNS[b][1]}" y2="${TOWNS[b][2]}" stroke="#F6E7B8" stroke-width="12" stroke-linecap="round"/>`).join("")}
        ${TOWNS.map(([n, x, y], i) => `<g data-i="${i}" style="cursor:pointer"><rect x="${x - 17}" y="${y - 17}" width="34" height="34" rx="9" fill="${i === sel ? "#FFF4D8" : "#E5525C"}" stroke="#fff" stroke-width="4"/>${i === 0 ? `<circle cx="${x}" cy="${y - 38}" r="11" fill="#FFD45A" stroke="#26304A" stroke-width="3"/>` : ""}</g>`).join("")}
        <circle cx="${TOWNS[sel][1]}" cy="${TOWNS[sel][2]}" r="30" fill="none" stroke="#FFF4D8" stroke-width="5" stroke-dasharray="8 6"><animateTransform attributeName="transform" type="rotate" from="0 ${TOWNS[sel][1]} ${TOWNS[sel][2]}" to="360 ${TOWNS[sel][1]} ${TOWNS[sel][2]}" dur="6s" repeatCount="indefinite"/></circle></svg>`;
      const [n, , , d, places] = TOWNS[sel];
      info.innerHTML = `<div class="g-label">${sel === 0 ? "You are here" : "Town"}</div><div class="g-title" style="font-size:32px;line-height:1.1;margin:6px 0 12px">${esc(n)}</div><div style="font-size:19px;font-weight:800;line-height:1.5;color:#E8F4EF">${esc(d)}</div>
        <div class="g-label" style="margin-top:20px">Here</div><div style="display:grid;gap:6px;margin-top:8px">${places.map(p => `<div style="display:flex;align-items:center;gap:10px;font-size:18px;font-weight:900"><span style="width:10px;height:10px;border-radius:3px;background:#8FF5C0"></span>${esc(p)}</div>`).join("")}</div>`;
      M.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { sel = +e.dataset.i; draw(); });
    };
    const near = k => { const [, x, y] = TOWNS[sel], dir = { left: [-1, 0], right: [1, 0], up: [0, -1], down: [0, 1] }[k]; let best = sel, bd = 1e9;
      TOWNS.forEach(([, tx, ty], i) => { if (i === sel) return; const dx = tx - x, dy = ty - y, along = dx * dir[0] + dy * dir[1]; if (along <= 0) return; const d = along + Math.abs(dx * dir[1] - dy * dir[0]) * 2; if (d < bd) { bd = d; best = i; } }); return best; };
    draw(); hints([["E", "Zoom in"], ["Q", "Zoom out"], ["C", "You"], ["B", "Back"]]);
    return { key: k => { if (k === "b") { go("menu", { sel: 3 }); return true; } if (k === "tab") { sel = (sel + 1) % TOWNS.length; draw(); return true; } if (["left", "right", "up", "down"].includes(k)) { sel = near(k); draw(); return true; } return true; } };
  }
});
const BADGES = [["BOULDERBADGE", "#A8A29A"], ["CASCADEBADGE", "#4D90D5"], ["THUNDERBADGE", "#E9C72E"], ["RAINBOWBADGE", "#5DB850"], ["SOULBADGE", "#E07AA8"], ["MARSHBADGE", "#E9B43C"], ["VOLCANOBADGE", "#E5525C"], ["EARTHBADGE", "#7CC46A"]];
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
      <div class="g-label" style="margin-top:6px">Badges</div><div style="display:grid;grid-template-columns:repeat(8,1fr);gap:10px">${BADGES.map(([n, c], i) => `<div title="${n}" style="aspect-ratio:1;border-radius:18px;background:rgba(0,0,0,.2);display:grid;place-items:center;${i < G.badges ? "" : "filter:grayscale(1) brightness(.5);opacity:.55"}"><span style="width:60px;height:60px">${ICON.badge(c)}</span></div>`).join("")}</div></div>`);
    hints([["B", "Back"]]);
    return { key: k => { if (k === "b") go("menu", { sel: 5 }); return true; } };
  }
});
screen("clothes", "Menus", "Clothes", {
  scene: "creator", sceneOpts: () => ({ side: -1.15 }),
  what: "Change what you wear: cap, top, bottoms, shoes and bag. B keeps what you chose, UNDO puts back what you came in with.",
  mod: "ClothesScreen: CAP STYLE, BACKWARDS, colours; B keeps, UNDO restores.",
  make() {
    backdrop("radial-gradient(ellipse at 70% 45%, #FFFFFF 0%, #FFF0E0 35%, #F6C99A 70%, #D98A52 100%)");
    const C = ctl(), before = JSON.stringify(G.look);
    maker(C, makerRows(false), { done: "UNDO", onDone: () => { G.look = JSON.parse(before); SC.creator.refresh(); go("clothes"); }, onBack: () => go("menu", { sel: 4 }) });
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
