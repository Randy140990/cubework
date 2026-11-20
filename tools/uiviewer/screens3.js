// ===================== Places =====================
const MART = [["POKé BALL", 200], ["GREAT BALL", 600], ["POTION", 300], ["SUPER POTION", 700], ["ANTIDOTE", 100], ["PARLYZ HEAL", 200], ["AWAKENING", 250], ["ESCAPE ROPE", 550], ["REPEL", 350]];
function bagCount(n) { for (const p of Object.values(G.bag)) for (const it of p) if (it[0] === n) return it[1]; return 0; }
function addToBag(n, c) { for (const p of Object.values(G.bag)) for (const it of p) if (it[0] === n) { it[1] += c; return; } (/BALL/.test(n) ? G.bag["BALLS"] : G.bag["ITEMS"]).push([n, c, ""]); }
function moneyBox() { const m = box(30, 30, 300, null, "g-panel", ""); m.style.padding = "12px 22px"; const draw = () => m.innerHTML = `<div class="g-label">Money</div><div style="font-size:30px;font-weight:900;font-variant-numeric:tabular-nums">${money(G.money)}</div>`; draw(); return draw; }
// The shop's shelf, as Let's Go's: a white page of tiles, each thing's picture in its colour, its name and price; the
// one you're on in sunny yellow with a red arrow. Resolves with the one picked, or -1.
function martPick(C, onMove) {
  return new Promise(res => {
    const P = box(600, 30, 650, null, "g-panel g-pop", ""); P.style.padding = "22px 22px 18px"; P.style.borderRadius = "30px";
    let sel = 0;
    const draw = () => {
      P.innerHTML = `<div class="g-title" style="font-size:22px;color:#E5525C;margin:0 0 12px 8px">${esc(T("BUY"))}</div><div style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px">${MART.map(([n, pr], i) => { const on = i === sel, kc = GOODS[ITEM_KIND(n)];
        return `<div data-i="${i}" style="position:relative;display:flex;align-items:center;gap:12px;height:74px;padding:0 16px 0 10px;border-radius:22px;cursor:pointer;background:${on ? "var(--g-sel)" : "#F5F8FD"};box-shadow:${on ? "0 0 0 3px #fff,0 0 0 6px #FFC93C,0 8px 16px rgba(232,150,30,.3)" : "inset 0 0 0 2px #E8EDF6"}">
          ${on ? `<span style="position:absolute;left:-18px;font-size:20px;color:#E5525C">&#9654;</span>` : ""}
          <span style="width:52px;height:52px;flex:none;border-radius:50%;background:#fff;display:grid;place-items:center;box-shadow:0 0 0 3px ${kc[1]}"><span style="width:34px;height:34px">${ITEM_ICON(n)}</span></span>
          <span style="font-weight:900;font-size:17px;color:#26304A;line-height:1.15">${esc(n)}</span><span style="margin-left:auto;font-weight:900;font-size:18px;color:#26304A">${money(pr)}</span></div>`; }).join("")}</div>`;
      P.querySelectorAll("[data-i]").forEach(e => { e.onmouseenter = () => { sel = +e.dataset.i; draw(); onMove(sel); }; e.onclick = () => { sel = +e.dataset.i; done(sel); }; });
    };
    const w = { key(k) { if (["left", "right", "up", "down"].includes(k)) { sel = gridMove(sel, MART.length, 2, k); draw(); onMove(sel); return true; } if (k === "a") { done(sel); return true; } if (k === "b") { done(-1); return true; } return true; } };
    const done = v => { C.pop(w); P.remove(); res(v); };
    C.push(w); draw();
  });
}
screen("mart", "Places", "Shop", {
  scene: "counter", sceneOpts: () => ({ kind: "mart" }),
  what: "Buying and selling, in the real Mart: you and the clerk face each other across his counter; his welcome, then the shelf as Let's Go's (a white page of tiles, each with its picture and price), how many, are you sure, thank you.",
  tags: ["New look: the shop's shelf"],
  mod: "MartScreen: BUY and SELL, MONEY, CANCEL.",
  try: ["BUY, pick an item, up and down for how many, A to buy."],
  make() {
    const C = ctl(), S = SC.counter;
    (async () => {
      let first = true;
      while (C.alive()) {
        S.talking = true;
        const v = await C.say([{ who: "CLERK", text: first ? "Hi there! Welcome to the POKé MART. How can I help?" : "Is there anything else I can do for you?", ask: ["BUY", "SELL", "SEE YOU"] }]);
        S.talking = false; first = false;
        if (v === 2 || v == null) { await C.say([{ who: "CLERK", text: "Thanks! Please come again!" }]); if (C.alive()) go("hud"); return; }
        const draw = moneyBox();
        if (v === 0) {
          const inBag = box(30, 600, 300, null, "g-panel soft", ""); inBag.style.padding = "12px 22px";
          const showBag = i => inBag.innerHTML = `<span class="g-label">In your bag</span>&nbsp;&nbsp;<span style="font-size:22px;font-weight:900">${bagCount(MART[i][0])}</span>`;
          showBag(0);
          while (C.alive()) {
            const i = await martPick(C, showBag);
            if (i < 0) break;
            const [n, p] = MART[i];
            let q = 1; const max = Math.max(1, Math.min(99, Math.floor(G.money / p)));
            const Q = box(440, 300, 300, null, "g-panel g-pop", ""); Q.style.padding = "16px 22px";
            const dq = () => Q.innerHTML = `<div class="g-label">How many?</div><div style="display:flex;align-items:center;gap:18px;margin-top:6px"><span style="font-size:38px;font-weight:900">x${String(q).padStart(2, "0")}</span><span style="margin-left:auto;font-size:26px;font-weight:900">${money(q * p)}</span></div><div style="font-size:14px;color:var(--g-muted);font-weight:800">Up and down to change</div>`;
            dq();
            const ok = await new Promise(res => { const w = { key(k) { if (k === "up" || k === "down") { q = Math.max(1, Math.min(max, q + (k === "up" ? 1 : -1))); dq(); return true; } if (k === "right" || k === "left") { q = Math.max(1, Math.min(max, q + (k === "right" ? 10 : -10))); dq(); return true; } if (k === "a") { C.pop(w); res(true); return true; } if (k === "b") { C.pop(w); res(false); return true; } return true; } }; C.push(w); });
            Q.remove(); if (!ok) continue;
            const yes = await C.say([{ who: "CLERK", text: `${n}, and you want ${q}. That'll be ${money(q * p).replace("&#8381;", "₽")}. OK?`, ask: ["YES", "NO"] }]);
            if (yes === 0) { if (G.money < q * p) { await C.say([{ who: "CLERK", text: "Sorry, you don't have enough money." }]); continue; } G.money -= q * p; addToBag(n, q); draw(); showBag(i); S.bow = true; await C.say([{ who: "CLERK", text: "Here you are! Thank you!" }]); S.bow = false;
              if (n === "POKé BALL" && q >= 10) { addToBag("PREMIER BALL", 1); await C.say([{ text: "You also got a **PREMIER BALL** as a thank-you." }]); } }
          }
          inBag.remove();
        } else {
          const items = G.bag["ITEMS"];
          const i = await C.choose(items.map(([n, c]) => ({ goods: n, right: "x" + c })).concat([{ label: "CANCEL" }]), { x: 760, y: 30, w: 480 });
          if (i >= 0 && i < items.length) { const price = (MART.find(m => m[0] === items[i][0]) || [0, 200])[1] / 2;
            const yes = await C.say([{ who: "CLERK", text: `I can pay ₽${price} for the ${items[i][0]}. Is that OK?`, ask: ["YES", "NO"] }]);
            if (yes === 0) { G.money += price; items[i][1]--; draw(); await C.say([{ text: `You handed over the **${items[i][0]}** and got ₽${price}.` }]); } }
        }
        uiRoot.querySelectorAll(".g-panel").forEach(e => { if (!e.classList.contains("g-text")) e.remove(); });
      }
    })();
    return { key: k => C.key(k) };
  }
});
screen("center", "Places", "Healing desk", {
  scene: "counter", sceneOpts: () => ({ kind: "center" }),
  what: "Healing, in the real Center: you and the nurse face each other across the counter; her welcome, yes or no, then your balls go onto her machine one at a time, it hums and they flash, and they come back to you good as new.",
  tags: ["New: the healing machine"], mod: "The nurse behind her counter in Viridian's Center.",
  make() {
    const C = ctl(), S = SC.counter;
    (async () => {
      S.talking = true;
      const v = await C.say([{ who: "NURSE", text: "Hello, and welcome to the POKéMON CENTER!" }, { who: "NURSE", text: "We heal tired POKéMON back to full health. Shall we heal yours?", ask: ["YES", "NO"] }]);
      if (v === 0) {
        await C.say([{ who: "NURSE", text: "OK. I'll take your POKéMON for a few seconds." }]);
        S.talking = false;
        // Your balls go onto her machine one at a time, it hums and they flash, then they come back to you.
        await new Promise(r => { const t0 = performance.now(); const f = () => { if (!C.alive()) return; S.heal = Math.min(1, (performance.now() - t0) / 5200); if (S.heal < 1) requestAnimationFrame(f); else r(); }; f(); });
        S.heal = 0; for (const p of G.party) { p.hp = p.maxhp; p.status = ""; }
        S.talking = true; S.bow = true;
        await C.say([{ who: "NURSE", text: "Thank you for waiting. Your POKéMON are fighting fit!" }, { who: "NURSE", text: "We hope to see you again!" }]);
        S.bow = false;
      } else await C.say([{ who: "NURSE", text: "We hope to see you again!" }]);
      S.talking = false; if (C.alive()) go("hud");
    })();
    return { key: k => C.key(k) };
  }
});
screen("trade", "Places", "Trading", {
  scene: "trade", sceneOpts: () => ({ a: "sandshrew", b: "farfetch_d" }),
  what: "A trade with someone in town: what they want, the two side by side, the balls crossing, take good care of it.",
  tags: ["New"],
  make() {
    backdrop("radial-gradient(ellipse at 50% 40%, #FFFFFF 0%, #E3F2FF 35%, #9CC2F2 70%, #4A6FC0 100%)");
    const C = ctl(), a = ourName("sandshrew"), b = ourName("farfetch_d");
    (async () => {
      const v = await C.say([{ who: "TRADER", text: `I'm looking for a **${a}**. Will you trade one for my **${b}**?`, ask: ["YES", "NO"] }]);
      if (v !== 0) { await C.say([{ who: "TRADER", text: "Aww. If you change your mind, I'll be here." }]); if (C.alive()) go("hud"); return; }
      const card = (who, sid, lv, ot) => `<div class="g-panel" style="position:relative;padding:16px 22px;width:400px"><div class="g-label">${who}</div><div style="display:flex;align-items:center;gap:10px;margin-top:4px"><span class="g-title" style="font-size:28px">${esc(ourName(sid))}</span><span style="margin-left:auto;font-weight:900;font-size:20px">Lv. ${lv}</span></div><div style="display:flex;gap:8px;margin-top:8px">${sp(sid).stats.types.map(typeChip).join("")}</div><div style="margin-top:8px;font-size:16px;font-weight:800;color:var(--g-muted)">Trainer: ${esc(ot)}</div></div>`;
      const offer = box(60, 40, 1160, null, "", `<div style="display:flex;justify-content:space-between">${card("You send", "sandshrew", 18, G.name)}${card("You get", "farfetch_d", 18, "TRADER")}</div>`);
      const go_ = await C.choose(["TRADE", "CANCEL"], { x: 490, y: 360, w: 300 });
      offer.remove(); if (go_ !== 0) { if (C.alive()) go("hud"); return; }
      await new Promise(r => SC.trade.cross(r));
      await C.say([{ text: `**${G.name}** sent **${a}** to TRADER.` }, { text: `TRADER sent **${b}** to **${G.name}**.` }, { who: "TRADER", text: "Thanks! Take good care of it!" }]);
      if (C.alive()) go("hud");
    })();
    return { key: k => C.key(k) };
  }
});
// The three starters: three balls on the professor's table; the one you're on rolls forward, opens, and its creature
// stands out big on its own backdrop (as the STATS page has it), its plate beside it. Then the professor asks.
const STARTERS = [["bulbasaur", "GRASS", "A leafy armadillo. It curls up in its bark-hard bands, and the sprout on its head drinks in the sun."],
  ["charmander", "FIRE", "A little black goat with a flame between its horns. The happier it is, the brighter the flame."],
  ["squirtle", "WATER", "A sea tapir. It sprays water from its soft snout, and the curl of wave on its head never dries."]];
screen("starter", "Places", "Choosing your first POKéMON", {
  scene: "model", sceneOpts: () => ({ sid: "charmander", ox: 0, stand: false, zoom: 0.92 }),
  what: "Choosing your first: three balls on the professor's table along the foot of the screen. The one you're on rolls forward and pops open; a white light builds up out of it over half a second and fills the screen, then fades to show its creature standing big in the middle on its own backdrop (embers for the fire one, leaves for the grass one, bubbles for the water one), with its name, type and a line about it on a plate. A to choose it, and the professor asks if you're sure.",
  mod: "StarterScreen.",
  tags: ["New: a better way to choose"], try: ["Left and right to look in each ball, A to choose, then YES or NO."],
  make() {
    const C = ctl(); C.sayAt = { film: true };
    let sel = 1, picking = true;
    add(h("style", "", `@keyframes stOpen{0%{transform:translateY(0) scale(1)}40%{transform:translateY(-22px) scale(1.08)}100%{transform:translateY(-14px) scale(1.04)}}
      @keyframes stBurst{from{transform:scale(.2);opacity:1}to{transform:scale(1.6);opacity:0}}@keyframes stWob{0%,100%{transform:rotate(0)}25%{transform:rotate(-8deg)}75%{transform:rotate(8deg)}}`));
    const balls = box(0, 486, 1280, 120, "", ""), plate = box(860, 150, 380, null, "", ""), flash = box(0, 0, 1280, 720, "", "");
    flash.style.pointerEvents = "none";
    const ball = (on, open, c) => `<svg viewBox="0 0 100 100" width="96" height="96" style="overflow:visible">
      <ellipse cx="50" cy="96" rx="34" ry="7" fill="rgba(0,0,0,.22)"/>
      <g transform="${open ? "translate(-6 -14) rotate(-42 8 50)" : ""}"><path d="M8 50a42 42 0 0 1 84 0z" fill="#E5525C" stroke="#26304A" stroke-width="5"/><path d="M22 30q10-12 24-14" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity=".55"/></g>
      <path d="M8 50a42 42 0 0 0 84 0z" fill="#F4F6F5" stroke="#26304A" stroke-width="5"/><rect x="8" y="46" width="84" height="8" fill="#26304A"/>
      <circle cx="50" cy="50" r="13" fill="#F4F6F5" stroke="#26304A" stroke-width="5"/><circle cx="50" cy="50" r="6" fill="${on ? c : "#F4F6F5"}" stroke="#26304A" stroke-width="2"/>
      ${open ? `<ellipse cx="50" cy="40" rx="30" ry="12" fill="#fff" opacity=".85" style="filter:blur(3px)"/>` : ""}</svg>`;
    // Looking in a ball: a white light builds up out of it for about a second, filling the screen; at its brightest the
    // creature and its backdrop are there behind it; the light fades off them; then its plate comes in.
    let run = 0;
    const reveal = () => {
      const me = ++run, [sid] = STARTERS[sel];
      plate.style.transition = "none"; plate.style.opacity = 0; SC.model.hide = true;
      flash.style.transition = "none"; flash.style.opacity = 1;
      flash.innerHTML = `<div id="stGlow" style="position:absolute;inset:0;background:radial-gradient(circle at ${[34, 50, 66][sel]}% 74%, #fff 0%, rgba(255,255,255,.95) 18%, rgba(255,255,255,0) 40%);opacity:0;transform-origin:${[34, 50, 66][sel]}% 74%;transform:scale(.3);transition:opacity .9s ease-in, transform .95s cubic-bezier(.6,0,.9,.6)"></div>
        <div id="stWhite" style="position:absolute;inset:0;background:#fff;opacity:0;transition:opacity .55s ease-in .45s"></div>`;
      requestAnimationFrame(() => requestAnimationFrame(() => { const g = $("#stGlow"), w = $("#stWhite"); if (g) { g.style.opacity = 1; g.style.transform = "scale(3)"; } if (w) w.style.opacity = .92; }));
      later(1000, () => { if (me !== run) return; bgArt.innerHTML = ""; monBackdrop(sid, { cx: 50, cy: 52 }); SC.model.show(sid); SC.model.hide = false; SC.model.glowOn = 1;
        flash.style.transition = "opacity .5s ease-out"; flash.style.opacity = 0;
        later(420, () => { if (me !== run) return; SC.model.glowOn = 0; plate.style.transition = "opacity .3s ease-out, transform .3s ease-out"; plate.style.opacity = 1; }); });
    };
    const draw = (burst) => {
      const [sid, kind, line] = STARTERS[sel], d = sp(sid), tc = typeHex(kind);
      if (!burst) { bgArt.innerHTML = ""; monBackdrop(sid, { cx: 50, cy: 52 }); SC.model.show(sid); }
      balls.innerHTML = `<div style="display:flex;justify-content:center;gap:150px">${STARTERS.map(([s2, k2], i) => { const on = i === sel;
        return `<div data-i="${i}" style="display:grid;justify-items:center;gap:6px;cursor:pointer;animation:${on ? "stOpen .45s ease-out forwards" : "none"}">
          <div style="width:96px;height:96px;${on ? "" : "animation:stWob 2.4s ease-in-out " + i * 0.4 + "s infinite"}">${ball(on, on, typeHex(k2))}</div>
          <span style="padding:2px 14px;border-radius:999px;font-weight:900;font-size:14px;letter-spacing:1px;background:${on ? "#fff" : "rgba(255,255,255,.4)"};color:${on ? typeHex(k2) : "#fff"}">${k2}</span></div>`; }).join("")}</div>`;
      balls.querySelectorAll("[data-i]").forEach(e => { e.onmouseenter = () => { if (picking && sel !== +e.dataset.i) { sel = +e.dataset.i; draw(true); } }; e.onclick = () => { if (picking) { sel = +e.dataset.i; draw(); pick(); } }; });
      plate.innerHTML = `<div class="g-fadein" style="position:relative;padding:18px 24px;border-radius:26px;background:rgba(255,255,255,.9);border:4px solid #fff;box-shadow:0 12px 28px rgba(30,40,84,.28)">
        <div style="font-weight:900;font-size:14px;letter-spacing:1.2px;color:${darkerHex(tc, 0.2)}">THE ${kind} POKéMON</div><div class="g-title" style="font-size:36px;line-height:1.1;color:#26304A">${esc(ourName(sid))}</div>
        <div style="display:flex;gap:8px;margin:8px 0 10px">${d.stats.types.map(typeChip).join("")}</div><div style="font-size:18px;font-weight:800;line-height:1.45;color:#26304A">${esc(line)}</div></div>`;
      if (burst) reveal();
    };
    const pick = async () => {
      picking = false; const [sid, kind] = STARTERS[sel], n = ourName(sid);
      const v = await C.say([{ who: "PROF. OAK", text: `So! You want the ${kind} POKéMON, **${n}**?`, ask: ["YES", "NO"] }]);
      if (!C.alive()) return;
      if (v !== 0) { picking = true; C.say([{ who: "PROF. OAK", text: "Take your time. Which one will you choose?" }], { keep: true }); return; }
      await C.say([{ text: `**{N}** received **${n}**!` }, { who: "PROF. OAK", text: "ROUTE 1 is just north of here. Good luck!" }]); if (C.alive()) go("hud");
    };
    draw(); C.say([{ who: "PROF. OAK", text: "Three balls, three POKéMON. Look in each one. Which will you choose?" }], { keep: true });
    hints([["A", "Choose"]]);
    return { key: k => {
      if (picking && (k === "left" || k === "right")) { sel = (sel + (k === "right" ? 1 : 2)) % 3; draw(true); return true; }
      if (picking && k === "a") { pick(); return true; }
      return C.key(k); } };
  }
});
screen("gift", "Places", "Being given a POKéMON", {
  scene: "model", sceneOpts: () => ({ sid: "hitmonlee", ox: 0 }),
  what: "Someone gives you a POKéMON: it's held up, then do you want to give it a nickname?",
  mod: "NicknameScreen: NEW NAME, up to 10 letters.",
  make() {
    backdrop("radial-gradient(ellipse at 50% 40%, #FFFFFF 0%, #FFF4D8 35%, #F6C46B 70%, #D9733A 100%)");
    const C = ctl(), n = ourName("hitmonlee");
    (async () => {
      await C.say([{ who: "KARATE KING", text: "You beat me fair and square. Here, take one of my fighters!" }, { text: `**${G.name}** received **${n}**!` }]);
      const v = await C.say([{ text: `Do you want to give a nickname to **${n}**?`, ask: ["YES", "NO"] }]);
      let nick = n;
      if (v === 0) { nick = await new Promise(res => { const w = nameEntry(C, { label: `${n}'S NICKNAME`, onDone: x => { C.pop(w); uiRoot.querySelectorAll(".g-panel.g-pop").forEach(e => e.remove()); res(x.toUpperCase()); } }); cur.raw = e => w.raw(e); }); }
      await C.say([{ text: `**${nick}** was added to your team.` }]); if (C.alive()) go("hud");
    })();
    return { key: k => C.key(k) };
  }
});

// ===================== Battle =====================
const FOE_TRAINER = "YOUNGSTER JOEY";
function plate(side, p, o = {}) {
  const f = p.hp / p.maxhp, ours = side === "ours";
  // Yours sits level with the text box beside it (574 to 694, its 120 tall), top and bottom.
  const e = box(ours ? 36 : 820, ours ? 574 : 36, ours ? 470 : 420, ours ? 120 : null, "g-panel", "");
  e.style.padding = ours ? "12px 22px 20px" : "12px 22px"; e.style.borderRadius = "22px"; e.style.boxSizing = "border-box";
  const balls = o.balls ? `<div style="position:absolute;${ours ? "top:-30px;left:18px" : "top:-30px;right:18px"};display:flex;gap:4px">${o.balls.map(s => `<span style="width:22px;height:22px;display:inline-block;${s ? "" : "filter:grayscale(1);opacity:.45"}">${ICON.ball}</span>`).join("")}</div>` : "";
  const draw = () => { const f = p.hp / p.maxhp; e.innerHTML = `${balls}<div style="display:flex;align-items:center;gap:10px"><span class="g-title" style="font-size:27px">${esc(ourName(p.sid))}</span>${gender(p.gender)}${statusChip(p.status)}<span style="margin-left:auto;font-weight:900;font-size:21px">Lv. ${p.lv}</span></div>
    <div style="display:flex;align-items:center;gap:10px;margin-top:${ours ? 4 : 8}px"><span style="font-weight:900;font-size:14px;letter-spacing:1px;color:var(--g-gold)">HP</span>${hpBar(f, ours ? 260 : 320)}${ours ? `<span style="font-weight:900;font-size:20px;font-variant-numeric:tabular-nums">${Math.ceil(p.hp)}/${p.maxhp}</span>` : ""}</div>
    ${ours ? `<div style="display:flex;align-items:center;gap:10px;margin-top:4px"><span style="font-weight:900;font-size:12px;letter-spacing:1px;color:#3C8DE4">EXP</span><div class="g-exp" style="flex:1"><i style="width:${(p.exp || 0) * 100}%"></i></div></div>` : ""}`; };
  draw(); return { el: e, draw };
}
const CHART = { ELECTRIC: { FLYING: 2, WATER: 2, GROUND: 0, GRASS: 0.5, ELECTRIC: 0.5 }, NORMAL: { ROCK: 0.5, GHOST: 0 }, FLYING: { GRASS: 2, BUG: 2, FIGHTING: 2, ELECTRIC: 0.5, ROCK: 0.5 } };
function eff(mt, sid) { let m = 1; for (const t of sp(sid).stats.types) m *= (CHART[mt] || {})[t] ?? 1; return m; }
function drain(p, to, pl, ms = 700) { return new Promise(r => { const from = p.hp, t0 = performance.now(); const f = () => { const k = Math.min(1, (performance.now() - t0) / ms); p.hp = from + (to - from) * k; pl.draw(); if (k < 1) requestAnimationFrame(f); else r(); }; f(); }); }
function flash(col) { const f = add(h("div", "", "")); Object.assign(f.style, { position: "absolute", inset: "0", background: col, opacity: ".0", transition: "opacity .12s" }); requestAnimationFrame(() => { f.style.opacity = ".45"; }); later(160, () => f.style.opacity = "0"); later(400, () => f.remove()); }
function statArrow(x, y, up) { const e = box(x, y, 200, 120, "", `<div style="font-size:64px;font-weight:900;color:${up ? "#FF8A5C" : "#6FC3FF"};text-shadow:0 0 18px currentColor;animation:${up ? "aup" : "adn"} 1s ease both">${up ? "&#9650;&#9650;" : "&#9660;&#9660;"}</div><style>@keyframes adn{from{transform:translateY(-30px);opacity:0}40%{opacity:1}to{transform:translateY(30px);opacity:0}}@keyframes aup{from{transform:translateY(30px);opacity:0}40%{opacity:1}to{transform:translateY(-30px);opacity:0}}</style>`); later(1100, () => e.remove()); }
// The four commands: a stack of wide buttons bottom right, each in its colour with a sheen, its own picture, and an arrow
// that points at the one you're on. The question sits above them, clear of everything.
const CMD_ICON = {
  FIGHT: `<svg viewBox="0 0 64 64"><path d="M32 6l6 14 15-4-8 13 12 9-15 3 1 15-11-9-11 9 1-15-15-3 12-9-8-13 15 4z" fill="#FFF4D8"/><path d="M32 20l3 8 8-2-4 7 6 5-8 1v8l-5-5-5 5v-8l-8-1 6-5-4-7 8 2z" fill="#FFD45A"/></svg>`,
  "POKéMON": `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" fill="#FFF4D8"/><path d="M8 32a24 24 0 0 1 48 0z" fill="#E5525C"/><rect x="8" y="29.5" width="48" height="5" fill="#26304A"/><circle cx="32" cy="32" r="8.5" fill="#26304A"/><circle cx="32" cy="32" r="5" fill="#FFF4D8"/></svg>`,
  BAG: `<svg viewBox="0 0 64 64"><path d="M22 18q0-10 10-10t10 10" fill="none" stroke="#FFF4D8" stroke-width="5"/><rect x="10" y="18" width="44" height="38" rx="10" fill="#FFF4D8"/><rect x="10" y="30" width="44" height="6" fill="#E0B868"/><rect x="27" y="27" width="10" height="12" rx="3" fill="#8A5A2A"/></svg>`,
  RUN: `<svg viewBox="0 0 64 64"><path d="M10 24h24V12l22 20-22 20V40H10z" fill="#FFF4D8"/><path d="M6 24h3v16H6zM0 26h3v12H0z" fill="#FFF4D8" opacity=".6"/></svg>`,
};
function commandButtons(onPick, o = {}) {
  const B = [["FIGHT", "#E5525C", 404, 80], ["POKéMON", "#36B07A", 494, 62], ["BAG", "#E9A830", 566, 62], ["RUN", "#4A8FE0", 638, 62]];
  const X = 948, W = 300;
  let sel = 0;
  const els = B.map(([n, c, y, hh], i) => { const big = i === 0;
    const e = box(X, y, W, hh, "", `<div style="position:relative;width:100%;height:100%;border-radius:${hh / 2}px;overflow:hidden;display:flex;align-items:center;gap:14px;padding:0 22px 0 ${big ? 12 : 10}px;
      background:linear-gradient(180deg, ${lighterHex(c, 0.18)} 0%, ${c} 48%, ${darkerHex(c, 0.22)} 100%);border:3px solid rgba(255,255,255,.9);box-shadow:0 6px 0 ${darkerHex(c, 0.45)},0 12px 22px rgba(0,0,0,.3);transition:transform .15s, box-shadow .15s">
      <span style="position:absolute;left:0;right:0;top:0;height:46%;background:linear-gradient(180deg,rgba(255,255,255,.35),rgba(255,255,255,0))"></span>
      <span style="position:absolute;right:-30px;top:-40px;width:${hh * 2}px;height:${hh * 2}px;border-radius:50%;background:#F1F4FB"></span>
      <span style="position:relative;width:${big ? 56 : 44}px;height:${big ? 56 : 44}px;flex:none;border-radius:50%;background:${darkerHex(c, 0.3)};display:grid;place-items:center;box-shadow:inset 0 3px 6px rgba(0,0,0,.25)"><span style="width:${big ? 40 : 30}px;height:${big ? 40 : 30}px">${CMD_ICON[n]}</span></span>
      <span style="position:relative;font-family:Fredoka,Nunito,sans-serif;font-weight:700;font-size:${big ? 32 : 24}px;letter-spacing:1.5px;color:#fff;text-shadow:0 2px 0 ${darkerHex(c, 0.45)}">${n}</span></div>`);
    e.style.cursor = "pointer"; e.onclick = () => { sel = i; paint(); onPick(i); }; e.onmouseenter = () => { sel = i; paint(); }; return e; });
  const arrow = box(X - 46, 0, 36, 36, "", `<style>@keyframes cmdbob{50%{transform:translateX(-7px)}}</style><svg viewBox="0 0 36 36" style="animation:cmdbob .7s ease-in-out infinite;filter:drop-shadow(0 3px 0 rgba(0,0,0,.3))"><path d="M6 4l26 14L6 32l6-14z" fill="#FFF4D8" stroke="#26304A" stroke-width="3" stroke-linejoin="round"/></svg>`);
  const pill = box(null, 330, null, null, "g-panel soft", `<div style="padding:11px 22px;font-size:19px;font-weight:900;white-space:nowrap">What will <span style="color:var(--g-gold)">${esc(o.who)}</span> do?</div>`); pill.style.borderRadius = "999px"; pill.style.left = "auto"; pill.style.right = (1280 - X - W) + "px";
  const paint = () => { els.forEach((e, i) => { const d = e.firstChild, on = i === sel; d.style.transform = on ? "translateX(-14px) scale(1.03)" : "none"; d.style.borderColor = on ? "#FFF4D8" : "rgba(255,255,255,.75)"; d.style.filter = on ? "none" : "saturate(.8) brightness(.92)"; });
    const [, , y, hh] = B[sel]; arrow.style.top = (y + hh / 2 - 18) + "px"; arrow.style.left = (X - 54) + "px"; };
  paint();
  return { key(k) { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : 3)) % 4; paint(); return true; } if (k === "left" || k === "right") return true; if (k === "a") { onPick(sel); return true; } return false; }, remove() { els.forEach(e => e.remove()); pill.remove(); arrow.remove(); } };
}
// The bag in battle, as BattleScreen.drawBag: a panel over the dimmed stage, its two pockets as big coloured cards down
// the left (POKé BALLS red, MEDICINE green, how many kinds in each), what's in the picked one as coloured bars on the
// right, what it does underneath. Resolves with the item's name, or null for back.
function battleBag(C, o = {}) {
  return new Promise(res => {
    const pockets = [["POKé BALLS", GOODS.ball, "ball", () => G.bag["BALLS"]], ["MEDICINE", GOODS.med, "potion", () => G.bag["ITEMS"].filter(([n]) => ITEM_KIND(n) === "med")]];
    let pk = o.pocket || 0, sel = 0;
    const dim = add(h("div", "", "", { position: "absolute", inset: "0", background: "rgba(6,26,30,.55)" }));
    const P = box(190, 70, 900, 540, "g-panel", ""); P.style.padding = "0"; P.style.boxSizing = "border-box";
    const mute = (c, k) => `color-mix(in srgb, ${c} ${Math.round((1 - k) * 100)}%, #123C38)`;
    const draw = () => {
      const items = pockets[pk][3](); sel = Math.max(0, Math.min(sel, items.length - 1));
      P.innerHTML = `<div style="position:absolute;left:30px;top:20px;width:44px;height:44px">${ICON.bag}</div><div class="g-title" style="position:absolute;left:86px;top:24px;font-size:30px">BAG</div>
        ${pockets.map(([n, c, ic, get], i) => { const on = i === pk, k = get().length, cols = on ? c : c.map(x => mute(x, 0.55));
          return `<div data-p="${i}" style="position:absolute;left:30px;top:${90 + i * 128}px;width:300px;height:108px;border-radius:26px;cursor:pointer;background:linear-gradient(180deg, ${cols[0]}, ${cols[1]} 50%, ${cols[2]});box-shadow:${on ? "0 0 0 3px #FFF4D8, 0 0 0 8px rgba(255,244,216,.45), " : ""}0 5px 0 rgba(0,0,0,.25)">
            <div style="position:absolute;left:18px;top:18px;width:72px;height:72px;border-radius:50%;background:${on ? "#fff" : "rgba(255,255,255,.7)"};display:grid;place-items:center"><span style="width:44px;height:44px">${ICON[ic]}</span></div>
            <div style="position:absolute;left:104px;top:22px;font-size:24px;font-weight:900;color:${on ? "#fff" : "#CFEDE3"}">${n}</div>
            <div style="position:absolute;left:104px;top:58px;font-size:19px;font-weight:800;color:${on ? "rgba(255,255,255,.9)" : "rgba(207,237,227,.7)"}">${k ? k + (k === 1 ? " kind" : " kinds") : "Empty"}</div></div>`; }).join("")}
        <div style="position:absolute;left:360px;top:90px;width:510px">${items.map(([n, c], i) => [n, c, i]).slice(Math.max(0, Math.min(sel - 2, items.length - 5)), Math.max(0, Math.min(sel - 2, items.length - 5)) + 5).map(([n, c, i]) => `<div data-i="${i}" style="margin-bottom:8px">${goodsRow(n, "x" + c, i === sel, 54)}</div>`).join("") || `<div style="font-size:21px;font-weight:800;color:var(--g-muted);padding:14px 10px">${pk === 0 ? "No POKé BALLS yet. A POKé MART sells them." : "No medicine to use in battle."}</div>`}</div>
        <div class="g-panel soft" style="position:absolute;left:360px;top:418px;width:510px;height:92px;box-sizing:border-box;padding:18px 24px;font-size:19px;font-weight:800;color:var(--g-muted)">${items[sel] ? esc(items[sel][2]) : ""}</div>`;
      P.querySelectorAll("[data-p]").forEach(e => e.onclick = () => { pk = +e.dataset.p; sel = 0; draw(); });
      P.querySelectorAll("[data-i]").forEach(e => { e.onmouseenter = () => { sel = +e.dataset.i; draw(); }; e.onclick = () => { sel = +e.dataset.i; done(items[sel][0]); }; });
    };
    const hs = hints([["A", "Use"], ["B", "Back"], ["TAB", "Pocket"]]);
    const w = { key(k) {
      const items = pockets[pk][3]();
      if (k === "left" || k === "right" || k === "tab") { pk = 1 - pk; sel = 0; draw(); return true; }
      if ((k === "up" || k === "down") && items.length) { sel = (sel + (k === "down" ? 1 : items.length - 1)) % items.length; draw(); return true; }
      if (k === "a" && items[sel]) { done(items[sel][0]); return true; }
      if (k === "b") { done(null); return true; }
      return true; } };
    const done = v => { C.pop(w); P.remove(); dim.remove(); if (hs && hs.remove) hs.remove(); else uiRoot.querySelectorAll(".g-hints").forEach(e => e.remove()); res(v); };
    C.push(w); draw();
  });
}
function movePanel(p, foeSid, onPick, onBack) {
  let sel = 0;
  const info = box(830, 230, 410, null, "g-panel", ""); info.style.padding = "14px 20px";
  const L = box(830, 420, 410, null, "", "");
  const paint = () => {
    L.innerHTML = p.moves.map((n, i) => { const m = MOVE[n] || {}; return `<div style="margin-bottom:10px;display:flex;align-items:center;gap:12px;padding:8px 18px 8px 10px;border-radius:999px;background:${i === sel ? "#FFF4D8" : `linear-gradient(90deg, ${TYPECOL(m.type)}, rgba(18,60,56,.95) 60%)`};color:${i === sel ? "#26304A" : "#fff"};border:3px solid ${i === sel ? "#fff" : "rgba(143,245,192,.4)"};font-weight:900;font-size:21px;cursor:pointer" data-i="${i}">
      <span class="g-type" style="background:${TYPECOL(m.type)};min-width:44px;padding:2px 8px;font-size:11px">${esc((m.type || "").slice(0, 3))}</span><span>${esc(n)}</span><span style="margin-left:auto;font-variant-numeric:tabular-nums">${m.pp}/${m.pp}</span></div>`; }).join("");
    const m = MOVE[p.moves[sel]] || {}, e = eff(m.type, foeSid);
    info.innerHTML = `<div style="display:flex;align-items:center;gap:10px">${typeChip(m.type)}<span class="g-label">${esc(m.category || "")}</span><span style="margin-left:auto;font-weight:900;font-size:15px;color:${e > 1 ? "#8FF5C0" : e < 1 ? "#FFB3B8" : "#A9D4C6"}">${e > 1 ? "Super effective" : e === 0 ? "No effect" : e < 1 ? "Not very effective" : "Effective"}</span></div>
      <div style="display:flex;gap:22px;margin-top:8px;font-size:18px;font-weight:900"><span><span class="g-label">Power</span> ${m.power > 1 ? m.power : "—"}</span><span><span class="g-label">Accuracy</span> ${m.accuracy || "—"}</span></div><div style="margin-top:6px;font-size:17px;font-weight:800;color:var(--g-text)">${esc(m.desc || "")}</div>`;
    L.querySelectorAll("[data-i]").forEach(el => el.onclick = () => { sel = +el.dataset.i; paint(); onPick(sel); });
  };
  paint();
  return { key(k) { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : 3)) % 4; paint(); return true; } if (k === "a") { onPick(sel); return true; } if (k === "b") { onBack(); return true; } return true; }, remove() { info.remove(); L.remove(); } };
}
// The camera gliding from one shot to another (eye and the point it looks at), eased.
function camGlide(p0, t0, p1, t1, ms) {
  return new Promise(res => { const st = performance.now(), A = V(...p0), B = V(...p1), TA = V(...t0), TB = V(...t1), P = V(0, 0, 0), T = V(0, 0, 0);
    const f = () => { const k = Math.min(1, (performance.now() - st) / ms), e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2; P.lerpVectors(A, B, e); T.lerpVectors(TA, TB, e); look3(P.x, P.y, P.z, T.x, T.y, T.z); if (k < 1) requestAnimationFrame(f); else res(); }; f(); });
}
// A ball thrown out: its arc to the spot, a burst of light, and the creature growing out of it.
function sendOut(S, from, to, who, grow) {
  return new Promise(res => S.ballPath([{ from: V(...from), to: V(...to), dur: 0.75, arc: 1.6, spin: 14, at: () => { S.ball.visible = false; S.burst(V(to[0], 0.7, to[2]), 2.6); if (S[who]) playOnce(S[who], "cry");
    const t0 = performance.now(), f = () => { const k = Math.min(1, (performance.now() - t0) / 420); grow(1 - k * k * (3 - 2 * k)); if (k < 1) requestAnimationFrame(f); else res(); }; f(); } }]));
}
screen("challenge", "Battle", "A trainer challenges you", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "spearow", trainer: true }),
  what: "A trainer challenges you, as Let's Go's do: a rush of light sweeps across, then you're looking over your own shoulder at them as the words come up. The camera swings to them as they throw: the ball arcs out, bursts in light and their POKéMON grows out of it. Then it pulls back wide and you throw yours. No name card: just the scene and the words.",
  tags: ["New: Let's Go's way in"], mod: "BattleScreen: trainers throw overarm; your rival's battle. To come: the shoulder shot, the swing to them, the throws.",
  make() {
    const C = ctl(), S = SC.battle; C.sayAt = { x: 120, w: 1040 };
    S.holdCam = true; S.foeHidden = 1; S.oursHidden = 1;
    // Over your shoulder: you big on the left, your back to us; them across the way on the right.
    const SHOULDER = [[-2.1, 2.15, 10.3], [-0.2, 1.2, -1.5]], AT_THEM = [[-0.6, 1.95, 4.4], [0.9, 0.5, -1.0]];
    look3(...SHOULDER[0], ...SHOULDER[1]);
    // The rush: streaks of white and mint light sweeping across, then gone before anything's said.
    const rush = box(0, 0, 1280, 720, "", `<style>@keyframes csrush{from{transform:translateX(-60%)}to{transform:translateX(60%)}}@keyframes csfade{0%{opacity:0}15%{opacity:1}70%{opacity:1}100%{opacity:0}}</style>
      <div style="position:absolute;inset:0;overflow:hidden;animation:csfade 1.15s ease-out forwards">
        <div style="position:absolute;inset:0;background:radial-gradient(ellipse 70% 50% at 50% 55%, rgba(255,255,240,.85), rgba(220,255,210,.35) 55%, transparent 80%)"></div>
        ${Array.from({ length: 16 }, (_, i) => `<div style="position:absolute;left:-40%;width:180%;top:${4 + i * 6}%;height:${2 + (i * 7) % 5}px;border-radius:999px;background:linear-gradient(90deg,transparent,${i % 3 ? "rgba(255,255,255,.9)" : "rgba(190,255,190,.9)"},transparent);animation:csrush ${0.45 + (i % 4) * 0.08}s linear ${-(i * 0.13) % 0.5}s infinite;filter:blur(${i % 2}px)"></div>`).join("")}</div>`);
    later(1200, () => rush.remove());
    (async () => {
      await C.wait(900);
      await C.say([{ text: `You are challenged by **${FOE_TRAINER}**!` }]);
      if (!C.alive()) return;
      // To them: they wind up and throw.
      await camGlide(...SHOULDER, ...AT_THEM, 900);
      S.foePoint = 1; await C.wait(250);
      await sendOut(S, [2.0, 1.7, -2.2], [0, 0, 0], "foe", h => S.foeHidden = h);
      S.foePoint = 0;
      await C.say([{ text: `**${FOE_TRAINER}** sent out **${ourName("spearow")}**!` }]);
      if (!C.alive()) return;
      // Wide, from behind your side, as you throw yours.
      S.holdCam = false; const fh = S.foe ? Math.max(0.8, S.foe.m.size.h) : 1, oh = S.ours ? Math.max(0.8, S.ours.m.size.h) : 1, k = Math.max(1, Math.max(fh, oh) / 1.2);
      await camGlide(...AT_THEM, [1.2 * k, 1.6 + 0.9 * k, 5.2 + 5.6 * k], [-1.0, 0.55 + fh * 0.3, 1.4], 900);
      S.point = 1; await C.wait(200);
      await sendOut(S, [-2.6, 1.6, 7.0], [-1.2, 0, 5.0], "ours", h => S.oursHidden = h);
      S.point = 0;
      await C.say([{ text: `Go! **${ourName("pikachu")}**!` }]);
      if (C.alive()) go("battle");
    })();
    return { key: k => C.key(k), leave() { S.holdCam = false; S.foeHidden = 0; S.oursHidden = 0; S.foePoint = 0; S.point = 0; } };
  }
});
screen("battle", "Battle", "Battle: commands and moves", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "spearow", trainer: true }),
  what: "The battle screen: their plate top right, yours low beside FIGHT, the round buttons, the moves with their types, PP and how well they'll work, and the battle's words in the text box.",
  mod: "BattleScreen: the plates, FIGHT, POKéMON, BAG and RUN, the move card, the bag with POKé BALLS and MEDICINE.",
  try: ["FIGHT, then THUNDERSHOCK: it's super effective on a flying POKéMON.", "GROWL lowers their Attack; RUN says you can't run from a trainer."],
  make() {
    const C = ctl(), S = SC.battle; C.sayAt = { x: 520, w: 724 };
    const ours = Object.assign({}, G.party[0]), foe = { sid: "spearow", lv: 17, gender: "M", status: "" }; foe.maxhp = statsAt("spearow", 17).HP; foe.hp = foe.maxhp;
    const P1 = plate("ours", ours, { balls: G.party.map(p => p.status !== "FNT") }), P2 = plate("foe", foe, { balls: [1, 1] });
    const turn = async mi => {
      const n = ours.moves[mi], m = MOVE[n] || {}, e = eff(m.type, foe.sid);
      await C.say([{ text: `**${ourName(ours.sid)}** used **${n}**!` }], { keep: true });
      if (S.ours) playOnce(S.ours, m.category === "Status" ? "cry" : m.category === "Special" ? "special" : "attack");
      await C.wait(450); if (m.category !== "Status") { flash(TYPECOL(m.type)); if (S.foe) playOnce(S.foe, "recoil"); }
      uiRoot.querySelectorAll(".g-text").forEach(x => x.remove());
      if (m.category === "Status") { statArrow(960, 110, false); await C.say([{ text: `The opposing **${ourName(foe.sid)}**'s Attack fell!` }]); }
      else {
        const dmg = Math.max(1, Math.round(((2 * ours.lv / 5 + 2) * (m.power || 40) * (ours.stats["Sp. Atk"] / 30) / 50 + 2) * e * (0.85 + Math.random() * 0.15)));
        await drain(foe, Math.max(0, foe.hp - dmg), P2);
        if (e > 1) await C.say([{ text: "It's super effective!" }]); else if (e > 0 && e < 1) await C.say([{ text: "It's not very effective..." }]); else if (e === 0) await C.say([{ text: `It doesn't affect the opposing **${ourName(foe.sid)}**...` }]);
      }
      if (foe.hp <= 0) { S.foeHidden = 0; const t0 = performance.now(); if (S.foe) playOnce(S.foe, "faint"); await C.wait(500); await new Promise(r => { const f = () => { S.foeHidden = Math.min(1, (performance.now() - t0 - 500) / 500); if (S.foeHidden < 1) requestAnimationFrame(f); else r(); }; f(); });
        await C.say([{ text: `The opposing **${ourName(foe.sid)}** fainted!` }]); if (C.alive()) go("levelup"); return; }
      await C.say([{ text: `The opposing **${ourName(foe.sid)}** used **PECK**!` }], { keep: true });
      if (S.foe) playOnce(S.foe, "attack"); await C.wait(450); flash("#fff"); if (S.ours) playOnce(S.ours, "recoil");
      uiRoot.querySelectorAll(".g-text").forEach(x => x.remove());
      await drain(ours, Math.max(1, ours.hp - Math.round(ours.maxhp * 0.18)), P1);
      commands();
    };
    const commands = () => {
      const cb = commandButtons(async i => {
        C.pop(cb); cb.remove();
        if (i === 0) { const mp = movePanel(ours, foe.sid, j => { C.pop(mp); mp.remove(); turn(j); }, () => { C.pop(mp); mp.remove(); commands(); }); C.push(mp); }
        else if (i === 1) { const dim = add(h("div", "", "", { position: "absolute", inset: "0", background: "rgba(6,26,30,.55)" })); const j = await C.choose(G.party.map(p => ({ pic: p.sid, label: ourName(p.sid), right: `Lv. ${p.lv}`, off: p.status === "FNT" })).concat(["CANCEL"]), { x: 800, y: null, bottom: 24, w: 450 }); dim.remove(); if (j > 0 && j < 6 && G.party[j].status !== "FNT") await C.say([{ text: `Come back, **${ourName(ours.sid)}**!` }, { text: "(In game, the other one goes out here.)" }]); commands(); }
        else if (i === 2) { const it = await battleBag(C), j = it == null ? -1 : /BALL/.test(it) ? 0 : 1; if (j === 0) await C.say([{ text: "The trainer blocked the ball! Don't be a thief!" }]); else if (j === 1) { await C.say([{ text: `**${G.name}** used a **POTION**.` }]); await drain(ours, Math.min(ours.maxhp, ours.hp + 20), P1); await C.say([{ text: `**${ourName(ours.sid)}**'s HP was restored.` }]); } commands(); }
        else { await C.say([{ text: "No! There's no running from a trainer battle!" }]); commands(); }
      }, { who: ourName(ours.sid) });
      C.push(cb);
    };
    commands();
    return { key: k => C.key(k) };
  }
});
screen("catch", "Battle", "Catching", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "pidgey", trainer: false }),
  what: "A wild POKéMON: throw a ball from the BAG (the list opens bottom right, where the buttons were). A ring closes in round it; press SPACE (or click) when it's small for Nice!, Great! or Excellent! Then, slowly: the ball arcs out and hits it, bursts open, it turns to red light and is drawn in, the ball hangs, drops with a little bounce, wobbles three times (the ball itself shows them; no counter), then clicks shut with a burst of gold sparkles, and Gotcha!",
  tags: ["New: throw timing and call-outs"], mod: "BattleScreen's BAG, POKé BALLS.",
  try: ["BAG, POKé BALLS, POKé BALL, then press SPACE (or click) as the ring shrinks."],
  make() {
    const C = ctl(), S = SC.battle, foeN = ourName("pidgey"); C.sayAt = { x: 520, w: 724 };
    const ours = Object.assign({}, G.party[0]), foe = { sid: "pidgey", lv: 4, gender: "F", status: "" }; foe.maxhp = statsAt("pidgey", 4).HP; foe.hp = Math.round(foe.maxhp * 0.4);
    let P1, P2;
    (async () => {
      await C.say([{ text: `A wild **${foeN}** appeared!` }]);
      P1 = plate("ours", ours, { balls: G.party.map(p => p.status !== "FNT") }); P2 = plate("foe", foe);
      while (C.alive()) {
        const pick = await new Promise(res => { const cb = commandButtons(i => { C.pop(cb); cb.remove(); res(i); }, { who: ourName(ours.sid) }); C.push(cb); });
        if (pick === 3) { await C.say([{ text: "Got away safely!" }]); if (C.alive()) go("hud"); return; }
        if (pick !== 2) { await C.say([{ text: "Try the BAG this time: it's for catching." }]); continue; }
        const it = await battleBag(C);
        if (it == null) continue;
        if (!/BALL/.test(it)) { await C.say([{ text: `**${G.name}** used a **${it}**.` }]); continue; }
        // The ring: smaller is better.
        const p = V(0, 0.4, 0).project(cam3), cx = (p.x + 1) / 2 * 1280, cy = (1 - p.y) / 2 * 720;
        const ring = box(cx - 150, cy - 150, 300, 300, "", `<svg viewBox="0 0 300 300" width="300" height="300"><circle cx="150" cy="150" r="60" fill="none" stroke="rgba(255,255,255,.7)" stroke-width="6"/><circle id="rg" cx="150" cy="150" r="140" fill="none" stroke="#8FF5C0" stroke-width="8"/></svg>`);
        const tip = box(400, Math.max(96, cy - 230), 480, null, "g-panel g-pop", `<div style="padding:12px 18px;text-align:center;font-size:22px;font-weight:900">Press <span class="g-key">SPACE</span> or click to throw!</div>`); tip.style.borderRadius = "999px";
        const hit = box(0, 0, 1280, 720, "", ""); hit.style.cursor = "crosshair";
        let r = 140, dir = -1, live = true;
        const spin = () => { if (!live || !C.alive()) return; r += dir * 2.4; if (r < 40) dir = 1; if (r > 140) dir = -1; const e = $("#rg"); if (e) { e.setAttribute("r", r); e.setAttribute("stroke", r < 70 ? "#FFD45A" : r < 100 ? "#8FF5C0" : "#fff"); } requestAnimationFrame(spin); };
        spin();
        await new Promise(res => { const w = { key(k) { if (k === "a") { C.pop(w); res(); } return true; } }; C.push(w); hit.onclick = e => { e.stopPropagation(); C.pop(w); res(); }; });
        live = false; ring.remove(); tip.remove(); hit.remove();
        const call = r < 62 ? "Excellent!" : r < 90 ? "Great!" : r < 120 ? "Nice!" : null;
        await C.say([{ text: `**${G.name}** threw a **${it}**!` }], { keep: true });
        // Slowly, so you can see it: the ball arcs out and hits it; bursts open; it turns to red light and is drawn
        // in; the ball hangs a moment, drops to the ground with a little bounce, and lies still before it wobbles.
        S.point = 1;
        const fh = S.foe ? S.foe.m.size.h : 0.8, HIT = V(0, Math.max(0.35, fh * 0.55), 0.18), UP = V(0, HIT.y + 0.55, 0.3), DOWN = V(0, 0.17, 0.52), REST = V(0, 0.17, 0.66);
        await new Promise(res => S.ballPath([
          { from: V(-2.8, 1.6, 6.2), to: HIT, dur: 1.2, arc: 2.0, spin: 9, at: () => {
            S.point = 0; S.burst(HIT, 1.6);
            if (call) { const c = add(h("div", "g-callout", call)); c.style.left = cx + "px"; c.style.top = (cy - 80) + "px"; later(1600, () => c.remove()); }
            const t0 = performance.now(), f = () => { const k = Math.min(1, (performance.now() - t0) / 800); S.foeRed = Math.min(1, k * 2.2); S.foeHidden = k * k; if (k < 1) requestAnimationFrame(f); }; f(); } },
          { from: HIT, to: UP, dur: 0.9, arc: 0, spin: 0, ease: "out" },
          { from: UP, to: DOWN, dur: 0.45, arc: 0, spin: 6 },
          { from: DOWN, to: REST, dur: 0.32, arc: 0.28, spin: 4 },
        ], res));
        S.foeRed = 0; S.restX = 0;
        await C.wait(500);
        uiRoot.querySelectorAll(".g-text").forEach(x => x.remove());
        // The ball shakes three times on its own, a dot for each, then clicks shut.
        S.shakeT = 0;
        // The ball itself shows the wobbles; then, caught, it clicks shut with a burst of gold sparkles off it.
        await C.wait(4500); S.shakeT = null;
        const bp = S.ball.position.clone().project(cam3), bx = (bp.x + 1) / 2 * 1280, by = (1 - bp.y) / 2 * 720;
        const st = box(bx - 120, by - 120, 240, 240, "", `<style>@keyframes ctSpark{0%{transform:translate(0,0) scale(.3);opacity:0}25%{opacity:1}100%{transform:translate(var(--dx),var(--dy)) scale(1);opacity:0}}</style>
          ${Array.from({ length: 9 }, (_, i) => { const a = -Math.PI / 2 + (i - 4) * 0.36, r = 70 + (i % 3) * 18; return `<span style="position:absolute;left:108px;top:108px;width:24px;height:24px;background:#FFE07A;clip-path:polygon(50% 0,62% 38%,100% 50%,62% 62%,50% 100%,38% 62%,0 50%,38% 38%);filter:drop-shadow(0 0 6px #FFC93C);--dx:${(Math.cos(a) * r).toFixed(0)}px;--dy:${(Math.sin(a) * r).toFixed(0)}px;animation:ctSpark .9s ease-out ${i * 0.03}s both"></span>`; }).join("")}`);
        st.style.pointerEvents = "none";
        const cg = add(h("div", "g-callout", "Gotcha!")); cg.style.left = "640px"; cg.style.top = "150px";
        await C.say([{ text: `Gotcha! **${foeN}** was caught!` }]); st.remove();
        await C.say([{ text: `**${foeN}**'s data was added to the **POKéDEX**.` }]);
        CAUGHT.add("pidgey");
        if (C.alive()) go("levelup"); return;
      }
    })();
    return { key: k => C.key(k) };
  }
});
screen("levelup", "Battle", "EXP and levelling up", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "spearow", trainer: true }),
  what: "After winning: EXP fills the bar, a new level with what each stat went up by, then a new move, and choosing one to forget.",
  mod: "BattleScreen's EXP bar; LearnScreen for new moves.",
  make() {
    const C = ctl(), S = SC.battle; C.sayAt = { x: 520, w: 724 }; S.foeHidden = 1;
    const p = Object.assign({}, G.party[0], { moves: G.party[0].moves.slice() }), n = ourName(p.sid);
    const P1 = plate("ours", p, { balls: G.party.map(x => x.status !== "FNT") });
    (async () => {
      await C.say([{ text: `**${n}** gained **214** EXP. Points!` }], { keep: true });
      await new Promise(r => { const a = p.exp, t0 = performance.now(); const f = () => { const k = Math.min(1, (performance.now() - t0) / 1200); p.exp = a + (1 - a) * k; P1.draw(); if (k < 1) requestAnimationFrame(f); else r(); }; f(); });
      uiRoot.querySelectorAll(".g-text").forEach(x => x.remove());
      p.lv++; p.exp = 0.08; const before = p.stats, after = statsAt(p.sid, p.lv); p.stats = after; p.maxhp = after.HP; P1.draw();
      const names = [["HP", "HP"], ["Attack", "ATTACK"], ["Defense", "DEFENSE"], ["Sp. Atk", "SPCL.ATK"], ["Sp. Def", "SPCL.DEF"], ["Speed", "SPEED"]];
      const T = box(860, 120, 380, null, "g-panel g-pop", `<div style="padding:16px 22px;display:grid;gap:8px">${names.map(([k, l]) => `<div style="display:flex;align-items:center;font-size:20px;font-weight:900"><span class="g-label" style="width:140px">${l}</span><span style="flex:1;text-align:right;font-variant-numeric:tabular-nums">${after[k]}</span><span style="width:70px;text-align:right;color:#2FA86C">+${Math.max(1, after[k] - before[k])}</span></div>`).join("")}</div>`);
      await C.say([{ text: `**${n}** grew to Lv. **${p.lv}**!` }]); T.remove();
      const v = await C.say([{ text: `**${n}** wants to learn the move **SWIFT**.` }, { text: `But **${n}** already knows four moves. Forget one to make room for **SWIFT**?`, ask: ["YES", "NO"] }]);
      if (v === 0) {
        const j = await C.choose(p.moves.map(m => ({ label: m, right: typeChip((MOVE[m] || {}).type || "NORMAL") })).concat([{ label: "SWIFT", right: typeChip("NORMAL") }]), { x: 760, y: 120, w: 480, back: false });
        if (j < 4) { const old = p.moves[j]; p.moves[j] = "SWIFT"; await C.say([{ text: "1, 2 and... Poof!" }, { text: `**${n}** forgot **${old}**...` }, { text: `...and learned **SWIFT**!` }]); }
        else await C.say([{ text: `**${n}** didn't learn **SWIFT**.` }]);
      } else await C.say([{ text: `**${n}** didn't learn **SWIFT**.` }]);
      if (C.alive()) go("evolve");
    })();
    return { key: k => C.key(k) };
  }
});
screen("evolve", "Battle", "Evolving", {
  scene: "model", sceneOpts: () => ({ sid: "bulbasaur", ox: 0 }),
  what: "Evolution: 'What? It's evolving!'. The creature turns to white light; then it and its new form swap back and forth, each growing as the other shrinks, slowly at first and then quicker and quicker, until the new one stays and its colours come back through the white. B (or ESC) while it's going stops it.",
  mod: "EvolutionScreen.",
  try: ["B while it glows stops it, as the old games let you."],
  make() {
    backdrop("radial-gradient(circle at 50% 45%, #FFFFFF 0%, #C9F2FF 25%, #6FB8E8 55%, #2E5F96 100%)");
    const C = ctl(), S = SC.model, a = ourName("bulbasaur"), b = ourName("ivysaur");
    let stopped = false;
    (async () => {
      await C.say([{ text: `What? **${a}** is evolving!` }]);
      S.turn = false; S.glowOn = 1;
      const tip = box(440, 300, 400, null, "g-panel g-pop", `<div style="padding:12px 18px;text-align:center;font-size:20px;font-weight:900">Press <span class="g-key round">B</span> to stop it</div>`); tip.style.borderRadius = "999px";
      const stop = { key(k) { if (k === "b") stopped = true; return true; } }; C.push(stop);
      const how = await S.morph("ivysaur", () => stopped);
      C.pop(stop); tip.remove(); S.glowOn = 0; S.turn = true;
      if (how === "stopped") { await C.say([{ text: `Huh? **${a}** stopped evolving!` }]); if (C.alive()) go("victory"); return; }
      // A burst of light as it settles.
      const fb = box(0, 0, 1280, 720, "", ""); fb.style.background = "radial-gradient(circle at 50% 50%, #fff 0, rgba(255,255,255,.7) 25%, transparent 60%)"; fb.style.pointerEvents = "none"; fb.style.transition = "opacity .8s"; later(30, () => fb.style.opacity = 0); later(900, () => fb.remove());
      add(h("div", "g-callout", "Congratulations!")).style.cssText += "left:640px;top:120px";
      await C.say([{ text: `**${a}** evolved into **${b}**!` }]);
      if (C.alive()) go("victory");
    })();
    return { key: k => C.key(k), leave() { S.glowOn = 0; S.hide = false; S.turn = true; } };
  }
});
screen("victory", "Battle", "Winning", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "spearow", trainer: true }),
  what: "You've won: the trainer has their say, you get prize money, counting up, then the celebration holds with your EXP until you press Continue.",
  mod: "BattleScreen: after a win you celebrate, a different way each time.",
  make() {
    const C = ctl(), S = SC.battle; C.sayAt = { x: 520, w: 724 }; S.foeHidden = 1;
    look3(0.6, 1.5, 3.4, 2.2, 1.05, -2.4);
    (async () => {
      await C.say([{ text: `You defeated **${FOE_TRAINER}**!` }, { who: FOE_TRAINER, text: "Aww, my birds! I'll train harder, you'll see!" }]);
      S.frame(); S.cheer = 1;
      const draw = moneyBox(); const start = G.money; G.money += 240;
      await new Promise(r => { const t0 = performance.now(); const f = () => { const k = Math.min(1, (performance.now() - t0) / 900); const m = Math.round(start + 240 * k); const keep = G.money; G.money = m; draw(); G.money = keep; if (k < 1) requestAnimationFrame(f); else { draw(); r(); } }; f(); });
      await C.say([{ text: `**${G.name}** got **₽240** for winning!` }]);
      // The celebration holds (EXP shown) until you choose to carry on.
      uiRoot.querySelectorAll(".g-text").forEach(e => e.remove());
      const lead = G.party[0];
      box(40, 520, 460, null, "g-panel g-pop", `<div style="padding:16px 22px;display:grid;gap:8px"><div style="display:flex;align-items:center;gap:10px"><span class="g-title" style="font-size:26px">${esc(ourName(lead.sid))}</span><span style="margin-left:auto;font-weight:900;font-size:20px">Lv. ${lead.lv}</span></div>
        <div style="display:flex;align-items:center;gap:10px"><span style="font-weight:900;font-size:13px;color:#3C8DE4;letter-spacing:1px">EXP</span><div class="g-exp" style="flex:1;height:10px;background:#DDE3F0"><i id="vx" style="width:${lead.exp * 100}%;background:linear-gradient(90deg,#7CC2FA,#3C8DE4)"></i></div><span style="font-weight:900;color:#3C8DE4">+214</span></div></div>`);
      later(200, () => { const e = $("#vx"); if (e) e.style.width = Math.min(100, lead.exp * 100 + 25) + "%"; });
      actions([{ key: "a", label: "Continue" }], { right: true });
      await new Promise(r => C.push({ key(k) { if (k === "a") { r(); return true; } return true; } }));
      S.cheer = 0; if (C.alive()) go("whiteout");
    })();
    return { key: k => C.key(k), leave() { S.cheer = 0; } };
  }
});
screen("whiteout", "Battle", "Losing", {
  scene: "battle", sceneOpts: () => ({ ours: "pikachu", foe: "spearow", trainer: true }),
  what: "When your whole team faints: the winner celebrates first (the camera swings round to them: their POKéMON jumps for joy and calls out, the trainer cheers with both arms up), then you drop some money, everything fades, and you wake at the last POKéMON CENTER.",
  make() {
    const C = ctl(), S = SC.battle; C.sayAt = { x: 520, w: 724 }; S.oursHidden = 1;
    (async () => {
      // The winner celebrates: their creature jumps for joy and calls out, the trainer cheers with both arms up.
      S.holdCam = true; await camGlide([1.2, 2.4, 9.0], [-1.0, 0.9, 1.4], [-0.4, 1.5, 4.2], [1.0, 0.95, -1.2], 900);
      S.foeCheer = 1; let hop = true; const t0 = performance.now();
      const jump = () => { if (!hop || !S.foe) return; const t = (performance.now() - t0) / 1000; S.foe.m.holder.position.y = Math.abs(Math.sin(t * 5.2)) * 0.32; requestAnimationFrame(jump); }; jump();
      if (S.foe) playOnce(S.foe, "cry");
      await C.say([{ who: FOE_TRAINER, text: "Yes! We did it! Great job, buddy!" }]);
      hop = false; if (S.foe) S.foe.m.holder.position.y = 0; S.foeCheer = 0;
      await C.say([{ text: `**${G.name}** has no POKéMON left that can fight!` }, { text: `**${G.name}** panicked and dropped **₽120**...` }]);
      const d = box(0, 0, 1280, 720, "", `<div id="wo" style="position:absolute;inset:0;background:#fff;opacity:0;transition:opacity 1.2s"></div>`); later(30, () => $("#wo").style.opacity = 1);
      await C.wait(1300);
      await C.say([{ text: "... ... ..." }, { text: `**${G.name}** whited out!` }]);
      S.oursHidden = 0; if (C.alive()) go("center");
    })();
    return { key: k => C.key(k), leave() { S.oursHidden = 0; S.foeCheer = 0; S.holdCam = false; if (S.foe) S.foe.m.holder.position.y = 0; } };
  }
});

// ===================== Controls =====================
const KB = [
  [["Esc", 1, "Esc"], ["F1", 1, "F1"], ["F2", 1, "F2"], ["F3", 1, "F3"], ["F4", 1, "F4"], ["F5", 1, "F5"], ["F6", 1, "F6"], ["F7", 1, "F7"], ["F8", 1, "F8"], ["F9", 1, "F9"], ["F10", 1, "F10"], ["F11", 1, "F11"], ["F12", 1, "F12"]],
  [["`", 1, "Backquote"], ["1", 1, "Digit1"], ["2", 1, "Digit2"], ["3", 1, "Digit3"], ["4", 1, "Digit4"], ["5", 1, "Digit5"], ["6", 1, "Digit6"], ["7", 1, "Digit7"], ["8", 1, "Digit8"], ["9", 1, "Digit9"], ["0", 1, "Digit0"], ["-", 1, "Minus"], ["=", 1, "Equal"], ["Back", 2, "Backspace"]],
  [["Tab", 1.5, "Tab"], ["Q", 1, "KeyQ"], ["W", 1, "KeyW"], ["E", 1, "KeyE"], ["R", 1, "KeyR"], ["T", 1, "KeyT"], ["Y", 1, "KeyY"], ["U", 1, "KeyU"], ["I", 1, "KeyI"], ["O", 1, "KeyO"], ["P", 1, "KeyP"], ["[", 1, "BracketLeft"], ["]", 1, "BracketRight"], ["Enter", 1.5, "Enter"]],
  [["Caps", 1.75, "CapsLock"], ["A", 1, "KeyA"], ["S", 1, "KeyS"], ["D", 1, "KeyD"], ["F", 1, "KeyF"], ["G", 1, "KeyG"], ["H", 1, "KeyH"], ["J", 1, "KeyJ"], ["K", 1, "KeyK"], ["L", 1, "KeyL"], [";", 1, "Semicolon"], ["'", 1, "Quote"], ["#", 1, "Backslash"], ["", 1.25, "x1"]],
  [["Shift", 2.25, "ShiftLeft"], ["Z", 1, "KeyZ"], ["X", 1, "KeyX"], ["C", 1, "KeyC"], ["V", 1, "KeyV"], ["B", 1, "KeyB"], ["N", 1, "KeyN"], ["M", 1, "KeyM"], [",", 1, "Comma"], [".", 1, "Period"], ["/", 1, "Slash"], ["Shift", 2.75, "ShiftRight"]],
  [["Ctrl", 1.5, "ControlLeft"], ["Alt", 1.5, "AltLeft"], ["Space", 7, "Space"], ["Alt Gr", 1.5, "AltRight"], ["Ctrl", 1.5, "ControlRight"], ["", 0.5, "x2"], ["&#8592;", 1, "ArrowLeft"], ["&#8593;", 1, "ArrowUp"], ["&#8595;", 1, "ArrowDown"], ["&#8594;", 1, "ArrowRight"]],
];
const ROLE = {
  KeyW: ["game", "Walk north", "In menus: up."], KeyA: ["game", "Walk west", "In menus: left."], KeyS: ["game", "Walk south", "In menus: down."], KeyD: ["game", "Walk east", "In menus: right."],
  Space: ["game", "Talk, look at things, choose", "In menus: A."], Enter: ["menu", "Choose, in menus (A)", "In the world it opens the menu too, as E does."], Tab: ["game", "Speed up: x1, x2, x3", "In menus: the next tab or pocket. (Moved off R, which reloads shaders.)"],
  Escape: ["game", "Pause", "In menus: back."], KeyE: ["game", "Open the menu (START)", "Where Minecraft opens its inventory, E opens ours. Press it again in the menu to close it."],
  KeyQ: ["menu", "Back, in menus (B)", "In the world it would drop what you hold: turned off. On the Town Map, zoom out."], KeyZ: ["menu", "Choose, in menus (A)", ""], KeyX: ["menu", "Back, in menus (B)", "On a name, delete a letter."],
  Backspace: ["menu", "Back, in menus (B)", "On a name, delete a letter."], KeyC: ["menu", "Town Map: back to where you are", ""], KeyV: ["menu", "Pokédex: read the entry out", ""],
  ArrowUp: ["menu", "Move, in menus", "In the world, W A S D walk."], ArrowDown: ["menu", "Move, in menus", ""], ArrowLeft: ["menu", "Move, in menus", ""], ArrowRight: ["menu", "Move, in menus", ""],
  F7: ["dev", "Retromon on or off", "For building and testing only. Off in the finished game."], F2: ["free", "Screenshot", "Minecraft's own, kept."], F11: ["free", "Full screen", "Minecraft's own, kept."], F1: ["free", "Hide the HUD", "Kept, for screenshots."],
  F3: ["off", "Debug screen", "Shows coordinates and more: off in the Retromon game."], F5: ["off", "Camera view", "The camera is fixed: off."],
  KeyR: ["off", "Reload shaders", "Iris's key. Off: the game's look can't be changed."], KeyK: ["off", "Shaders on or off", "Iris's key. Off: the shaders stay on."], KeyO: ["off", "Shader packs", "Iris's menu. Off."],
  KeyM: ["off", "Other mods' keys", "Every other mod's key is turned off while you play (a map mod on M, for one). The Town Map is in the menu."], KeyJ: ["off", "Map mods", "Off."], KeyL: ["off", "Advancements", "Off."], KeyP: ["off", "Social interactions", "Off."],
  KeyT: ["decide", "Chat", "Off in single player. On servers: to decide."], Slash: ["off", "Commands", "Off, except for whoever runs the server."], KeyF: ["off", "Swap hands", "Off."],
  ShiftLeft: ["decide", "Crouch", "To decide: off, or run."], ShiftRight: ["decide", "Crouch", "To decide."], ControlLeft: ["decide", "Sprint", "To decide: run, or off."],
  Digit1: ["off", "Hotbar", "1 to 9: off. There's no hotbar."], Digit2: ["off", "Hotbar", ""], Digit3: ["off", "Hotbar", ""], Digit4: ["off", "Hotbar", ""], Digit5: ["off", "Hotbar", ""], Digit6: ["off", "Hotbar", ""], Digit7: ["off", "Hotbar", ""], Digit8: ["off", "Hotbar", ""], Digit9: ["off", "Hotbar", ""],
};
const ROLECOL = { game: ["#41BF84", "#fff", "In game"], menu: ["#FFD34E", "#26304A", "In menus"], dev: ["#8E6BC8", "#fff", "For building"], free: ["#7C8A99", "#fff", "Minecraft's, kept"], off: ["#E5525C", "#fff", "Turned off"], decide: ["#F0B43C", "#26304A", "To decide"] };
screen("keymap", "Controls", "Key map", {
  what: "Every key: what it does in the world, what it does in menus, and what Retromon turns off (Axiom, shaders on and off, map mods, the hotbar, the inventory, the camera view).",
  tags: ["New"], mod: "Adventure: SPACE talks, ENTER opens the menu, TAB speeds up, F7 turns Retromon on and off. To come: E opens the menu, and other mods' keys are turned off.",
  try: ["Point at a key, or move round them with the arrows."],
  make(arg) {
    swirlBackdrop("sky", { lx: 85, ly: 0 });
    let ri = 2, ci = 2;
    const U = 62, X0 = 40, Y0 = 60;
    const kb = box(0, 0, 1280, 720, "", "");
    const info = box(40, 470, 760, 220, "g-panel", ""); info.style.padding = "18px 24px";
    const side = box(830, 470, 410, 220, "g-panel", ""); side.style.padding = "16px 22px";
    side.innerHTML = `<div class="g-label">Also turned off</div><div style="display:grid;gap:6px;margin-top:8px;font-size:17px;font-weight:800">
      <div><span style="color:#E5525C">&#9632;</span> Axiom: its editor and every key it adds</div><div><span style="color:#E5525C">&#9632;</span> Mouse: breaking, placing, picking blocks, the hotbar wheel</div>
      <div><span style="color:#E5525C">&#9632;</span> Minecraft's inventory, recipes and creative menus</div><div><span style="color:#E5525C">&#9632;</span> Turning the shaders off, or swapping them</div></div>`;
    const draw = () => {
      let html = "";
      KB.forEach((row, r) => { let x = X0; row.forEach(([l, w, code], c) => { const role = ROLE[code], col = role ? ROLECOL[role[0]] : null, on = r === ri && c === ci;
        if (l !== "" || code.startsWith("x") === false) html += `<div data-r="${r}" data-c="${c}" style="position:absolute;left:${x}px;top:${Y0 + r * (U + 6)}px;width:${w * U - 6}px;height:${U - 6}px;border-radius:12px;display:grid;place-items:center;font-weight:900;font-size:${l.length > 3 ? 15 : 19}px;cursor:pointer;
          background:${col ? col[0] : "#fff"};color:${col ? col[1] : "var(--g-dim)"};${role && role[0] === "off" ? "background-image:repeating-linear-gradient(45deg,rgba(0,0,0,.15) 0 6px,transparent 6px 12px);" : ""}
          box-shadow:${on ? "0 0 0 3px #fff, 0 0 0 6px #E5525C, 0 8px 18px rgba(30,40,84,.3)" : "0 3px 0 rgba(30,40,84,.18)"};transform:${on ? "translateY(-3px)" : "none"}">${code.startsWith("x") ? "" : l}</div>`;
        x += w * U; }); });
      html += `<div style="position:absolute;left:${X0}px;top:20px;display:flex;gap:16px;font-size:15px;font-weight:900;color:#26304A">${Object.values(ROLECOL).map(([b, f, n]) => `<span style="display:inline-flex;align-items:center;gap:6px"><span style="width:16px;height:16px;border-radius:5px;background:${b}"></span>${n}</span>`).join("")}</div>`;
      kb.innerHTML = html;
      const [l, , code] = KB[ri][ci], role = ROLE[code];
      info.innerHTML = role ? `<div style="display:flex;align-items:center;gap:14px"><span class="g-key" style="min-width:56px;height:40px;font-size:18px">${l}</span><span class="g-title" style="font-size:28px">${esc(role[1])}</span><span style="margin-left:auto;padding:3px 12px;border-radius:999px;background:${ROLECOL[role[0]][0]};color:${ROLECOL[role[0]][1]};font-weight:900;font-size:14px">${ROLECOL[role[0]][2]}</span></div><div style="margin-top:12px;font-size:20px;font-weight:800;color:var(--g-text);line-height:1.5">${esc(role[2])}</div>`
        : `<div style="display:flex;align-items:center;gap:14px"><span class="g-key" style="min-width:56px;height:40px;font-size:18px">${l || "&nbsp;"}</span><span class="g-title" style="font-size:26px;color:var(--g-muted)">Not used</span></div><div style="margin-top:12px;font-size:19px;font-weight:800;color:var(--g-muted)">Free for now.</div>`;
      kb.querySelectorAll("[data-r]").forEach(e => { e.onmouseenter = () => { ri = +e.dataset.r; ci = +e.dataset.c; draw(); }; });
    };
    draw();
    hints([["B", "Back"]]);
    return { key: k => {
      if (k === "b") { go(arg && arg.from === "menu" ? "menu" : "options"); return true; }
      if (k === "left" || k === "right") { ci = Math.max(0, Math.min(KB[ri].length - 1, ci + (k === "right" ? 1 : -1))); draw(); return true; }
      if (k === "up" || k === "down") { const nr = Math.max(0, Math.min(KB.length - 1, ri + (k === "down" ? 1 : -1))); const x = KB[ri].slice(0, ci).reduce((s, q) => s + q[1], 0) + KB[ri][ci][1] / 2; let acc = 0, best = 0; KB[nr].forEach((q, i) => { if (acc <= x) best = i; acc += q[1]; }); ri = nr; ci = best; draw(); return true; }
      return true; } };
  }
});
