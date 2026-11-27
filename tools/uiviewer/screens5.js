// ===================== (0.72) OPTION's admin pages: the PARTY PICKER, the EVENT PICKER, the PLACER; the GAME CORNER's slots =====================
// As the mod draws them (PartyPickerScreen, EventPickerScreen, PlacerScreen, SlotsScreen), with KANTO's own data
// (tools/uiviewer.py: the events sheet, FireRed's placeables, the campaign's points, the wild POKéMON and the reels).
const admBox = (x, y, w, hh, html, cls = "g-panel") => { const e = box(x, y, w, hh, cls, html); e.style.padding = "0"; return e; };
const admPill = (txt, bg, extra = "") => `<span style="display:inline-block;padding:5px 16px;border-radius:999px;background:${bg};color:#fff;font-weight:900;font-size:15px;letter-spacing:.5px;${extra}">${esc(txt)}</span>`;
const admTop = (title, line, now, nowCol) => admBox(40, 24, 1200, 80, `<div style="display:flex;align-items:center;height:100%;padding:0 24px;gap:20px">
  <div style="display:grid;gap:2px;min-width:0"><span class="g-title" style="font-size:30px;line-height:1">${esc(title)}</span><span style="font-size:17px;color:var(--g-muted);font-weight:700">${esc(line)}</span></div>
  <span style="margin-left:auto">${admPill(now, nowCol)}</span></div>`);
const PICK_STARTERS = ["bulbasaur", "charmander", "squirtle"];
const ADM_EVOS = { bulbasaur: [[16, "ivysaur"], [32, "venusaur"]], charmander: [[16, "charmeleon"], [36, "charizard"]], squirtle: [[16, "wartortle"], [36, "blastoise"]] };
const admLead = (s, lv) => { let id = s; for (const [l, e] of ADM_EVOS[s]) if (lv >= l) id = e; return id; };
const admName = id => (typeof ourName === "function" ? ourName(id) : id).toUpperCase();

// ---------- PARTY PICKER ----------
screen("party-picker", "OPTION for videos", "Party picker", {
  what: "For videos: a party from any point in KANTO (after BROCK, after MISTY ...), or your own pick. Your own party is kept aside. OPTION, in a game, for whoever runs it.",
  mod: "PartyPickerScreen: STARTER, BADGES (MATCH or KEEP MINE), the campaign's ten points, CUSTOM and MY OWN PARTY; the party at that point on the right.",
  tags: ["Admin", "In game only"],
  try: ["Up and down to pick a point: the party it makes shows on the right.", "Left and right on STARTER or BADGES to change them.", "The party here is an example: the game makes one from the POKéMON you could have caught by then."],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 });
    admTop("PARTY PICKER", "For your videos: a party from any point in KANTO, or your own pick. Your own party is kept aside.", "NOW: YOUR OWN PARTY", "var(--g-green)");
    const S = KANTO.stages, ROWS = 2 + S.length + 2;
    let row = 2, starter = 0, badges = true;
    const L = admBox(40, 118, 420, ROWS * 35 + 30, ""), R = admBox(476, 118, 764, ROWS * 35 + 30, "");
    const party = st => {
      const s = S[st], out = [{ id: admLead(PICK_STARTERS[starter], s.leadHi), lv: s.leadHi }];
      const pool = [];
      for (let i = 0; i <= st; i++) for (const p of S[i].places) for (const sp of (KANTO.wild[p] || [])) if (!pool.includes(sp)) pool.push(sp);
      for (const g of S.slice(0, st + 1).flatMap(x => x.gifts)) if (!pool.includes(g)) pool.push(g);
      let k = 7 * (st + 1);
      while (out.length < Math.max(s.min, Math.min(s.max, 6)) && pool.length) { const id = pool[(k = (k * 31 + 17) % 9973) % pool.length]; if (!out.some(o => o.id === id)) out.push({ id, lv: s.lo + (k % (s.hi - s.lo + 1)) }); else if (out.length >= pool.length) break; }
      return out;
    };
    const draw = () => {
      const label = r => r === 0 ? "STARTER" : r === 1 ? "BADGES" : r < 2 + S.length ? S[r - 2].title : r === 2 + S.length ? "CUSTOM" : "MY OWN PARTY";
      L.innerHTML = `<div style="padding:15px 14px">${Array.from({ length: ROWS }, (_, r) => {
        const right = r === 0 ? admName(PICK_STARTERS[starter]) : r === 1 ? (badges ? "MATCH" : "KEEP MINE") : null;
        return `<div class="g-item${r === row ? " on" : ""}${r === ROWS - 1 ? " off" : ""}" data-r="${r}" style="height:35px;min-height:0;font-size:19px;padding-top:0;padding-bottom:0;${r === row ? "" : "border-radius:0;border-bottom:1.5px solid #EEF1F8"}"><span>${esc(label(r))}</span>${right ? `<span class="r" style="display:flex;gap:10px;align-items:center;font-size:17px"><span style="color:var(--g-red)">&#9664;</span><span style="min-width:110px;text-align:center">${esc(right)}</span><span style="color:var(--g-red)">&#9654;</span></span>` : ""}</div>`;
      }).join("")}</div>`;
      L.querySelectorAll("[data-r]").forEach(e => e.onclick = () => { row = +e.dataset.r; draw(); });
      let head, line, team = [];
      if (row === ROWS - 1) { head = "MY OWN PARTY"; line = "You're using your own party."; }
      else if (row === ROWS - 2) { head = "CUSTOM"; line = "A to change each one: it starts from the party of " + S[S.length - 1].title + "."; }
      else if (row < 2) { head = row === 0 ? "YOUR STARTER" : "BADGES"; line = row === 0 ? "The POKéMON whose line leads every party." : "MATCH: the badges you'd have by then (FLY needs the THUNDERBADGE). KEEP MINE: yours."; }
      else { const s = S[row - 2]; head = s.title; line = s.line + " R for a slightly different party."; team = party(row - 2); }
      R.innerHTML = `<div style="padding:18px 24px"><div class="g-title" style="font-size:24px">${esc(head)}</div><div style="font-size:16px;color:var(--g-muted);font-weight:700;margin-top:4px">${esc(line)}</div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 14px;margin-top:16px">${Array.from({ length: 6 }, (_, i) => { const m = team[i];
          return m ? `<div style="height:${Math.floor((ROWS * 35 + 30 - 140) / 3)}px;border-radius:16px;background:#F1F6EE;display:flex;align-items:center;gap:12px;padding:0 14px">${pic(m.id, 76)}<div style="display:grid;gap:2px;min-width:0;flex:1"><div style="display:flex;justify-content:space-between;font-weight:900;font-size:19px"><span>${esc(admName(m.id))}</span><span style="font-size:17px">Lv ${m.lv}</span></div><div class="g-label">${i === 0 ? "Your starter's line" : "Caught by then"}</div></div></div>`
            : `<div style="height:${Math.floor((ROWS * 35 + 30 - 140) / 3)}px;border-radius:16px;background:#F4F6FB;display:grid;place-items:center;color:var(--g-dim);font-weight:800">-</div>`; }).join("")}</div></div>`;
    };
    draw();
    hints([["A", "Use"], ["R", "New party"], ["B", "Back"]]);
    return { key: k => {
      if (k === "up" || k === "down") { row = (row + (k === "down" ? 1 : ROWS - 1)) % ROWS; draw(); return true; }
      if ((k === "left" || k === "right") && row < 2) { if (row === 0) starter = (starter + (k === "right" ? 1 : 2)) % 3; else badges = !badges; draw(); return true; }
      if (k === "b") { go("options", arg); return true; }
      return k === "a";
    } };
  }
});

// ---------- EVENT PICKER ----------
screen("event-picker", "OPTION for videos", "Event picker", {
  what: "For videos: go to any step of KANTO's story, just before it or into it, with the badges, items and a party for that point. Your own game is kept aside until MY OWN GAME. OPTION, in a game, for whoever runs it.",
  mod: "EventPickerScreen: the 87 steps of the Kanto Campaign Events sheet (kanto_events.tsv) on the left; the one you're on, GO TO (BEFORE IT or IN IT), PARTY and STARTER on the right.",
  tags: ["New in 0.71", "Admin", "In game only"],
  try: ["Up and down (SHIFT: 10 at a time) for a step.", "Left and right: BEFORE IT or IN IT.", "P: that point's party or your own. S: the starter."],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 });
    admTop("EVENT PICKER", "Go to any step of KANTO's story: just before it, or into it. Your own game is kept aside.", "NOW: YOUR OWN GAME", "var(--g-green)");
    const E = KANTO.events, SHOWN = 13, rh = 38;
    let at = 0, top = 0, inIt = false, mine = false, starter = 0;
    const L = admBox(40, 118, 560, SHOWN * rh + 30, ""), R = admBox(616, 118, 624, SHOWN * rh + 30, "");
    const draw = () => {
      L.innerHTML = `<div style="padding:15px 14px">${E.slice(top, top + SHOWN).map((r, i) => { const idx = top + i, on = idx === at;
        return `<div class="g-item${on ? " on" : ""}" data-i="${idx}" style="height:${rh}px;min-height:0;padding:0 12px 0 10px;font-size:17px;${on ? "" : "border-radius:0;border-bottom:1.5px solid #EEF1F8"}"><span style="width:44px;height:24px;border-radius:999px;background:${r.main ? "var(--g-blue)" : "var(--g-dim)"};color:#fff;font-size:15px;display:grid;place-items:center;flex:none">${r.n}</span><span style="min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1">${esc(r.where)}</span><span class="g-label" style="font-size:12px">${esc(r.stageName.toUpperCase())}</span></div>`; }).join("")}</div>`;
      L.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { at = +e.dataset.i; draw(); });
      const r = E[at], st = KANTO.stages[Math.max(0, Math.min(KANTO.stages.length - 1, r.stage))];
      const opt = (l, v) => `<div style="display:flex;align-items:center;height:38px;border-radius:12px;background:#F4F6FB;padding:0 18px;font-weight:900;font-size:16px"><span>${esc(l)}</span><span style="margin-left:auto;display:flex;align-items:center;gap:12px"><span style="color:var(--g-red)">&#9664;</span><span style="min-width:260px;text-align:center">${esc(v)}</span><span style="color:var(--g-red)">&#9654;</span></span></div>`;
      R.innerHTML = `<div style="display:flex;flex-direction:column;height:100%;padding:20px 24px 24px">
        <div>${admPill("STEP " + r.n + "  " + r.stageName.toUpperCase() + (r.main ? "" : "  SIDE"), r.main ? "var(--g-blue)" : "var(--g-dim)", "font-size:14px")}</div>
        <div class="g-title" style="font-size:24px;margin-top:14px;line-height:1.2">${esc(r.where)}</div>
        <div style="font-size:16px;font-weight:700;margin-top:8px;line-height:1.4">${esc(r.what)}</div>
        ${r.need && r.need !== "-" ? `<div style="margin-top:10px;font-size:15px"><span class="g-label" style="display:inline-block;width:92px">YOU NEED</span>${esc(r.need)}</div>` : ""}
        ${r.get && r.get !== "-" ? `<div style="margin-top:4px;font-size:15px"><span class="g-label" style="display:inline-block;width:92px">YOU GET</span>${esc(r.get)}</div>` : ""}
        <div style="margin-top:auto;display:grid;gap:4px"><div style="font-size:14px;color:var(--g-muted);font-weight:700;margin-bottom:4px">${esc(mine ? "Your own party comes with you." : st.line + " Leads at Lv " + st.leadLo + " to " + st.leadHi + ".")}</div>
          ${opt("GO TO", inIt ? "IN IT" : "BEFORE IT")}${opt("PARTY", mine ? "MY OWN" : st.title)}${opt("STARTER", admName(PICK_STARTERS[starter]))}
          <div style="margin-top:10px">${admPill(inIt ? "GO INTO IT" : "GO TO JUST BEFORE", "var(--g-green)", "padding:10px 30px;font-size:16px")}</div></div></div>`;
    };
    const move = d => { at = Math.max(0, Math.min(E.length - 1, at + d)); if (at < top) top = at; if (at >= top + SHOWN) top = at - SHOWN + 1; draw(); };
    draw();
    hints([["A", "Go"], ["P", "Party"], ["S", "Starter"], ["B", "Back"]]);
    return {
      raw: e => { if (e.code === "KeyP") { mine = !mine; draw(); return true; } if (e.code === "KeyS") { starter = (starter + 1) % 3; draw(); return true; } return false; },
      key: k => {
        if (k === "up" || k === "down") { move(k === "down" ? 1 : -1); return true; }
        if (k === "left" || k === "right") { inIt = !inIt; draw(); return true; }
        if (k === "b") { go("options", arg); return true; }
        return k === "a";
      } };
  }
});

// ---------- PLACER ----------
screen("placer", "OPTION for videos", "Placer", {
  what: "Everyone and everything FireRed has outside and in the caves (470: trainers, people, item balls, hidden items, STRENGTH's boulders, switches and rocks), place by place. Stand where one goes, face its way, and place it. Saved as you go to config/retromon_placed.tsv. OPTION, in a game, for whoever runs it.",
  mod: "PlacerScreen and its WALK AND PLACE card (placeables.tsv from FireRed's maps; Placer and Boulders in the game).",
  tags: ["New in 0.72", "Admin", "In game only"],
  try: ["Left and right for the place, up and down for the one in it.", "A: PLACE HERE. W: WALK AND PLACE (the card at the top, out on the map). X: TAKE AWAY. U: PUT BACK.", "Here, A marks it PLACED, as the game would once you'd put it down."],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 });
    const P = KANTO.placeables, places = [...new Set(P.map(t => t.place))];
    const order = KANTO.placerOrder;
    places.sort((a, b) => (order.indexOf(a) < 0 ? 999 : order.indexOf(a)) - (order.indexOf(b) < 0 ? 999 : order.indexOf(b)));
    const state = {};
    for (const t of P) if (t.baked) state[t.key] = "mod";
    const KC = { trainer: "var(--g-red)", person: "var(--g-blue)", item: "var(--g-amber)", hidden: "#B07CE0" }, kc = k => KC[k] || "#8C93A8";
    const SW = { mine: ["PLACED", "var(--g-green)"], mod: ["IN THE MOD", "var(--g-blue)"], gone: ["TAKEN AWAY", "var(--g-red)"], "": ["NOT PLACED", "var(--g-dim)"] };
    let pi = 0, at = 0, top = 0;
    const SHOWN = 12, rh = 36;
    const head = admBox(40, 24, 1200, 80, ""), PL = admBox(40, 118, 560, 56, ""), L = admBox(40, 184, 560, SHOWN * rh + 30, ""), R = admBox(616, 118, 624, 66 + SHOWN * rh + 30, "");
    const list = () => P.filter(t => t.place === places[pi]);
    const placeName = id => (KANTO.placeNames[id] || id.replace(/_/g, " ")).toUpperCase();
    const draw = () => {
      const down = P.filter(t => state[t.key] && state[t.key] !== "gone").length, l = list(), ld = l.filter(t => state[t.key] && state[t.key] !== "gone").length;
      head.innerHTML = `<div style="display:flex;align-items:center;height:100%;padding:0 24px;gap:20px"><div style="display:grid;gap:2px;min-width:0"><span class="g-title" style="font-size:30px;line-height:1">PLACER</span><span style="font-size:16px;color:var(--g-muted);font-weight:700">Everyone and everything FireRed has outside and in the caves. Stand where one goes, face its way, and place it.</span></div><span style="margin-left:auto">${admPill(down + " OF " + P.length + " DOWN", "var(--g-green)")}</span></div>`;
      PL.innerHTML = `<div style="display:flex;align-items:center;height:100%;padding:0 22px"><span style="color:var(--g-red);font-size:22px">&#9664;</span><div style="flex:1;text-align:center;display:grid"><span style="font-weight:900;font-size:21px">${esc(placeName(places[pi]))}</span><span class="g-label" style="font-size:12px">${pi + 1} OF ${places.length} · ${ld} OF ${l.length} DOWN</span></div><span style="color:var(--g-red);font-size:22px">&#9654;</span></div>`;
      L.innerHTML = `<div style="padding:15px 14px">${l.slice(top, top + SHOWN).map((t, i) => { const idx = top + i, on = idx === at, [sw, sc] = SW[state[t.key] || ""];
        return `<div class="g-item${on ? " on" : ""}" data-i="${idx}" style="height:${rh}px;min-height:0;padding:0 12px 0 8px;font-size:16px;${on ? "" : "border-radius:0;border-bottom:1.5px solid #EEF1F8"}"><span style="width:86px;height:22px;border-radius:999px;background:${kc(t.kind)};color:#fff;font-size:12px;display:grid;place-items:center;flex:none">${esc(t.kind.toUpperCase())}</span><span style="min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1">${esc(t.label)}</span><span style="font-size:12px;font-weight:900;color:${sc}">${sw}</span></div>`; }).join("")}</div>`;
      L.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { at = +e.dataset.i; draw(); });
      const t = l[at]; if (!t) { R.innerHTML = ""; return; }
      const [sw, sc] = SW[state[t.key] || ""];
      const what = t.kind === "trainer" ? "TEAM: " + t.detail : t.kind === "item" ? "In a POKé BALL: " + t.detail.replace("item=", "").replace(/_/g, " ").toUpperCase() : t.kind === "hidden" ? "Hidden (found with A): " + t.detail.replace("item=", "").replace(/_/g, " ").toUpperCase() : t.detail === "-" ? "" : t.detail;
      const how = { trainer: "Face the way they look: they see straight ahead, a step (two blocks) wide.", boulder: "Stand on the boulder's spot (it's two blocks across): STRENGTH pushes it a step at a time.", switch: "Stand on the switch: a boulder pushed onto it opens its rocks.", barrier: "Stand on the rocks' spot: they crumble when a boulder's on their switch.", hidden: "Stand on the spot: A facing it (or the ITEMFINDER) finds it." }[t.kind] || "Stand on the spot where it goes.";
      const spot = "step " + t.gx + ", " + t.gy + (t.facing && t.facing !== "none" ? "  ·  facing " + t.facing.replace(/_/g, " ") : "") + (t.sight > 0 ? "  ·  sees " + t.sight + (t.sight === 1 ? " step" : " steps") : "");
      R.innerHTML = `<div style="display:flex;flex-direction:column;height:100%;padding:20px 24px 24px">
        <div style="display:flex;justify-content:space-between">${admPill(t.kind.toUpperCase() + "  " + placeName(t.place), kc(t.kind), "font-size:14px")}${admPill(sw, sc, "font-size:14px")}</div>
        <div class="g-title" style="font-size:24px;margin-top:14px">${esc(t.label)}</div>
        <div style="font-size:16px;font-weight:700;margin-top:8px;line-height:1.4">${esc(what)}</div>
        <div style="margin-top:12px;font-size:15px;display:flex"><span class="g-label" style="width:146px;flex:none">FIRERED'S SPOT</span><span>${esc(spot)}</span></div>
        <div style="margin-top:8px;font-size:15px;display:flex"><span class="g-label" style="width:146px;flex:none">HOW</span><span>${esc(how)}</span></div>
        <div style="margin-top:auto;font-size:13px;color:var(--g-muted);font-weight:700">Saved as you go to config/retromon_placed.tsv. Send it to Claude to build into the mod.</div>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-top:12px">${[["PLACE HERE", "var(--g-green)"], ["WALK AND PLACE", "var(--g-blue)"], ["TAKE AWAY", "var(--g-red)"], ["PUT BACK", "var(--g-amber)"]].map(([l2, c], i) => `<span data-b="${i}" style="cursor:pointer;height:40px;border-radius:999px;background:${c};color:#fff;font-weight:900;font-size:14px;display:grid;place-items:center">${l2}</span>`).join("")}</div></div>`;
      R.querySelectorAll("[data-b]").forEach(e => e.onclick = () => act(["a", "w", "x", "u"][+e.dataset.b]));
    };
    const act = k => {
      const t = list()[at]; if (!t) return;
      if (k === "a") state[t.key] = "mine";
      else if (k === "x") state[t.key] = "gone";
      else if (k === "u") { if (t.baked) state[t.key] = "mod"; else delete state[t.key]; }
      else if (k === "w") { go("placer-walk", { thing: t, from: arg }); return; }
      draw();
    };
    const move = d => { const n = list().length; at = Math.max(0, Math.min(n - 1, at + d)); if (at < top) top = at; if (at >= top + SHOWN) top = at - SHOWN + 1; draw(); };
    draw();
    hints([["A", "Place here"], ["W", "Walk and place"], ["X", "Take away"], ["U", "Put back"], ["B", "Back"]]);
    return {
      raw: e => { if (e.code === "KeyW") { act("w"); return true; } if (e.code === "KeyX") { act("x"); return true; } if (e.code === "KeyU") { act("u"); return true; } return false; },
      key: k => {
        if (k === "up" || k === "down") { move(k === "down" ? 1 : -1); return true; }
        if (k === "left" || k === "right") { pi = (pi + (k === "right" ? 1 : places.length - 1)) % places.length; at = 0; top = 0; draw(); return true; }
        if (k === "a") { act("a"); return true; }
        if (k === "b") { go("options", arg && arg.from ? arg.from : arg); return true; }
        return false;
      } };
  }
});
screen("placer-walk", "OPTION for videos", "Placer: walk and place", {
  scene: "town", sceneOpts: () => ({}),
  what: "WALK AND PLACE: back out on the map with a card at the top saying what you're putting down. P places it where you stand (facing your way) and moves on to the next one there not placed yet; N skips; U undoes; X stops.",
  mod: "PlacerScreen.hud and PlacerScreen.fieldKey.",
  tags: ["New in 0.72", "Admin"],
  make(arg) {
    const P = KANTO.placeables, t = arg && arg.thing || P.find(x => x.kind === "trainer");
    const KC = { trainer: "var(--g-red)", person: "var(--g-blue)", item: "var(--g-amber)", hidden: "#B07CE0" };
    const name = (KANTO.placeNames[t.place] || t.place.replace(/_/g, " ")).toUpperCase();
    const card = box(640 - 330, 96, 660, 92, "g-panel", `<div style="position:absolute;left:0;top:0;bottom:0;width:8px;border-radius:22px 0 0 22px;background:${KC[t.kind] || "#8C93A8"}"></div>
      <div style="padding:12px 24px 0 28px;display:grid;gap:4px"><div style="font-weight:900;font-size:18px">PLACING  ${esc(t.kind.toUpperCase())}  ${esc(t.label)}</div>
      <div style="font-size:14px;color:var(--g-muted);font-weight:700">${esc(name)}  ·  FireRed's step ${t.gx}, ${t.gy}${t.facing && t.facing !== "none" ? "  ·  facing " + esc(t.facing.replace(/_/g, " ")) : ""}</div>
      <div style="font-size:14px;color:var(--g-blue);font-weight:900">P  Place here     N  Next     U  Undo     X  Stop</div></div>`);
    card.style.padding = "0";
    if (typeof placePill === "function") placePill();
    hints([["A", "Talk"], ["B", "Back to the PLACER"]]);
    return { key: k => { if (k === "b") { go("placer", arg && arg.from); return true; } return false; } };
  }
});

// ---------- the GAME CORNER's slots ----------
screen("slots", "Places", "Slot machine", {
  what: "The GAME CORNER's slot machines (the ones that work): bet 1 to 3 coins, A spins, then A stops each reel in turn. Our own symbols. Three in a line pay per coin on that line.",
  mod: "SlotsScreen (KantoStory's REELS and PAYS: SEVEN 300, BAR 100, BELL, STAR and MOON 15, CHERRY 8).",
  tags: ["New in 0.71"],
  try: ["Up and down: the bet.", "A spins; A again stops the next reel."],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 });
    const REELS = KANTO.reels, PAYS = KANTO.pays;
    const SYM = (s, r) => {
      const R2 = r * 2;
      if (s === 0) return `<svg width="${R2}" height="${R2}" viewBox="-1 -1 2 2"><circle r="1" fill="#FFD34E"/><text y=".55" text-anchor="middle" font-size="1.6" font-weight="900" fill="#D62F3C" font-family="Nunito Sans,sans-serif">7</text></svg>`;
      if (s === 1) return `<svg width="${R2 * 1.25}" height="${R2}" viewBox="-1.25 -1 2.5 2"><rect x="-1.25" y="-.55" width="2.5" height="1.1" rx=".25" fill="#22242C"/><text y=".27" text-anchor="middle" font-size=".75" font-weight="900" fill="#fff" font-family="Nunito Sans,sans-serif">BAR</text></svg>`;
      if (s === 2) return `<svg width="${R2}" height="${R2}" viewBox="-1 -1 2 2"><path d="M-.05 -1 V-.1 M-.05 -1 H.6" stroke="#3E9E4E" stroke-width=".16" stroke-linecap="round" fill="none"/><circle cx="-.47" cy=".37" r=".47" fill="#D62F3C"/><circle cx=".52" cy=".52" r=".47" fill="#E5525C"/></svg>`;
      if (s === 3) return `<svg width="${R2}" height="${R2}" viewBox="-1 -1 2 2"><circle cy="-.4" r=".55" fill="#F0B43C"/><rect x="-.8" y="-.35" width="1.6" height=".95" rx=".25" fill="#F0B43C"/><circle cy=".68" r=".18" fill="#B27A1C"/></svg>`;
      if (s === 4) return `<svg width="${R2}" height="${R2}" viewBox="-1 -1 2 2"><path d="M0 -1 L.85 .45 H-.85 Z M0 1 L.85 -.4 H-.85 Z" fill="#FFC93C"/></svg>`;
      return `<svg width="${R2}" height="${R2}" viewBox="-1 -1 2 2"><circle r="1" fill="#FFE7A1"/><circle cx=".55" cy="-.1" r="1" fill="#FFFBF0"/></svg>`;
    };
    let bet = 1, coins = 120, pos = [0, 0, 0], spinning = false, stopped = 0, said = "Bet some coins and press A!";
    const M = box(380, 70, 520, 560, "", ""); M.style.cssText += "border-radius:28px;background:linear-gradient(180deg,#E5525C,#B8323E);box-shadow:0 10px 24px rgba(0,0,0,.35)";
    const side = box(40, 150, 310, 230, "g-panel", `<div style="padding:18px 20px"><div class="g-title" style="font-size:20px">HOW TO PLAY</div>${["UP/DOWN: bet 1 to 3 coins.", "1 coin: the middle line.", "2 coins: the top and bottom too.", "3 coins: the diagonals too.", "A spins, then A stops", "each reel in turn."].map(s => `<div style="font-size:15px;margin-top:6px;font-weight:700">${esc(s)}</div>`).join("")}</div>`);
    const pay = box(950, 150, 290, 330, "g-panel", `<div style="padding:18px 20px"><div class="g-title" style="font-size:20px">PAYOUTS</div>${[0, 1, 3, 4, 5, 2].map(s => `<div style="display:flex;align-items:center;height:40px"><span style="display:flex;gap:4px">${SYM(s, 12).repeat(3)}</span><span style="margin-left:auto;font-weight:900;font-size:18px">${PAYS[s]}</span></div>`).join("")}<div style="font-size:13px;color:var(--g-muted);margin-top:2px">per coin on each line</div></div>`);
    const draw = () => {
      const sym = (i, k) => REELS[i][((Math.floor(pos[i]) + k) % REELS[i].length + REELS[i].length) % REELS[i].length];
      const reel = i => `<div style="width:132px;height:252px;border-radius:10px;background:#FFFBF0;display:grid;grid-template-rows:repeat(3,84px);place-items:center">${[-1, 0, 1].map(k => SYM(sym(i, k), 30)).join("")}</div>`;
      const lit = c => c ? "rgba(255,211,78,.8)" : "rgba(255,255,255,.2)";
      M.innerHTML = `<div style="position:absolute;left:20px;right:20px;top:18px;height:74px;border-radius:18px;background:#2B1B2E;display:grid;place-items:center"><span class="g-title" style="font-size:34px;color:var(--g-cream)">SLOTS</span></div>
        <div style="position:absolute;left:26px;top:98px;padding:14px;border-radius:18px;background:#2B1B2E;display:flex;gap:16px">${[0, 1, 2].map(reel).join("")}
          <div style="position:absolute;left:8px;right:8px;top:${14 + 126 - 2}px;height:4px;border-radius:2px;background:${lit(true)}"></div>
          <div style="position:absolute;left:8px;right:8px;top:${14 + 42 - 2}px;height:4px;border-radius:2px;background:${lit(bet >= 2)}"></div>
          <div style="position:absolute;left:8px;right:8px;top:${14 + 210 - 2}px;height:4px;border-radius:2px;background:${lit(bet >= 2)}"></div>
          <svg style="position:absolute;left:8px;top:14px" width="${3 * 132 + 32 + 12}" height="252"><line x1="0" y1="42" x2="${3 * 132 + 44}" y2="210" stroke="${lit(bet >= 3)}" stroke-width="4" stroke-dasharray="4 6"/><line x1="0" y1="210" x2="${3 * 132 + 44}" y2="42" stroke="${lit(bet >= 3)}" stroke-width="4" stroke-dasharray="4 6"/></svg></div>
        <div style="position:absolute;left:30px;top:${560 - 150}px;width:210px;height:56px;border-radius:14px;background:#2B1B2E;padding:8px 16px"><div style="font-size:13px;font-weight:900;color:var(--g-dim)">COINS</div><div style="font-size:22px;font-weight:900;color:var(--g-cream)">${coins}</div></div>
        <div style="position:absolute;right:30px;top:${560 - 150}px;width:210px;height:56px;border-radius:14px;background:#2B1B2E;padding:8px 16px"><div style="font-size:13px;font-weight:900;color:var(--g-dim)">BET</div><div style="font-size:22px;font-weight:900;color:var(--g-cream)">${bet} COIN${bet === 1 ? "" : "S"}</div></div>
        <div style="position:absolute;left:30px;right:30px;top:${560 - 78}px;height:44px;border-radius:999px;background:#fff;display:grid;place-items:center;font-weight:900;font-size:18px">${esc(said)}</div>`;
    };
    const tick = () => { if (!spinning) return; for (let i = stopped; i < 3; i++) pos[i] += 0.42; draw(); };
    timers.push(setInterval(tick, 50));
    const result = () => {
      const lines = [[0, 0, 0]].concat(bet >= 2 ? [[-1, -1, -1], [1, 1, 1]] : [], bet >= 3 ? [[-1, 0, 1], [1, 0, -1]] : []);
      let win = 0;
      for (const ln of lines) { const s = ln.map((k, i) => REELS[i][((Math.round(pos[i]) + k) % REELS[i].length + REELS[i].length) % REELS[i].length]); if (s[0] === s[1] && s[1] === s[2]) win += PAYS[s[0]]; else if (s[0] === 2) win += s[1] === 2 ? 4 : 2; }
      coins += win; said = win ? "You won " + win + " coins!" : "Not this time!";
    };
    draw();
    hints([["A", "Spin"], ["B", "Leave"]]);
    return { key: k => {
      if ((k === "up" || k === "down") && !spinning) { bet = Math.max(1, Math.min(3, bet + (k === "up" ? 1 : -1))); draw(); return true; }
      if (k === "a") {
        if (!spinning) { if (coins < bet) { said = "You don't have enough coins!"; draw(); return true; } coins -= bet; spinning = true; stopped = 0; said = "A stops the reels!"; }
        else { pos[stopped] = Math.round(pos[stopped]); stopped++; if (stopped >= 3) { spinning = false; result(); } }
        draw(); return true;
      }
      if (k === "b") { go("menu"); return true; }
      return false;
    } };
  }
});
