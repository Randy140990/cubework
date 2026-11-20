// ===================== Characters: every person in the mod, side by side =====================
// The same people, in the same order and with the same numbers, as the Person Viewer: both are built from its roster
// (tools/personviewer/roster.js and more.js), so a person is never in one viewer and missing from the other.
// Beside them: their last POKéMON that's built (their ace, as the old games line them up), or PIKACHU for you.
const aceOf = p => p.skin.outfit ? "pikachu" : [...p.team].reverse().find(t => sp(t)) || null;
const CAST_IDS = { story: "home", leaders: "leaders", elite: "elite", rocket: "rocket", helpers: "helpers", trainers: "trainers", folk: "folk", crowd: "crowd" };
const CAST_ALL = ROSTER.PEOPLE.map((p, i) => {
  const [group, kind, skin, name, who, where, team] = p;
  const spec = typeof skin === "string" ? { skin } : skin.outfit ? { outfit: OUTFIT.defaults(skin.outfit === "girl") } : skin;
  return { id: "P-" + String(i + 1).padStart(3, "0"), group, kind, spec, name, who, where, team, skin };
});
for (const [gid, gname, gwhat] of ROSTER.GROUPS) {
  const people = CAST_ALL.filter(p => p.group === gid);
  if (!people.length) continue;
  const PER = 10;
  screen("cast-" + (CAST_IDS[gid] || gid), "Characters", gname, {
    scene: "cast",
    what: `${gwhat} Everyone here as they look in the mod: Minecraft's player model in the mod's own skins (textures/entity/npc), corners just softened. Left and right to pick: they step forward and wave, with their ace beside them. The numbers match the Person Viewer.`,
    tags: [`${people.length} people`],
    mod: "NpcRenderer gives each person their skin by name: the nurse, the clerk, trainers, gym leaders and townsfolk.",
    try: ["Left and right to pick, up and down for the next page."],
    make() {
      backdrop("radial-gradient(ellipse at 50% 35%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)");
      const pages = Math.ceil(people.length / PER);
      let sel = 0;
      header(ICON.card, gname.toUpperCase(), `${people.length} PEOPLE`);
      const info = box(30, 520, 1220, 170, "g-panel", ""); info.style.padding = "16px 24px";
      const pager = box(1040, 30, 210, null, "g-panel soft", ""); pager.style.borderRadius = "999px"; pager.style.padding = "8px 16px"; pager.style.textAlign = "center"; pager.style.fontWeight = "900";
      const show = () => {
        const page = Math.floor(sel / PER), list = people.slice(page * PER, page * PER + PER);
        SC.cast.show(list.map(p => ({ spec: p.spec, ace: aceOf(p) })), sel - page * PER);
        pager.innerHTML = pages > 1 ? `Page ${page + 1} of ${pages}` : `${people.length} here`;
        const p = people[sel], ace = aceOf(p);
        info.innerHTML = `<div style="display:grid;grid-template-columns:1fr auto;gap:16px;align-items:start">
          <div style="display:grid;gap:6px;min-width:0"><div style="display:flex;align-items:center;gap:14px;flex-wrap:wrap"><span style="font-size:14px;font-weight:900;letter-spacing:1px;padding:3px 10px;border-radius:999px;background:#26304A;color:#fff">${p.id}</span><span class="g-title" style="font-size:34px">${esc(p.name)}</span><span class="g-label">${esc(p.where)}</span></div>
            <div style="font-size:19px;font-weight:800;color:var(--g-text)">${esc(p.who)}</div>
            ${ace ? `<div style="font-size:15px;font-weight:800;color:var(--g-muted)">With them: ${esc(ourName(ace))}<span id="acenote"></span></div>` : ""}
            <div class="g-label" style="margin-top:4px">${p.spec.outfit ? "Skin: made from your clothes" : "Skin: npc/" + esc(p.spec.skin)} · ${p.kind === "U" ? "One of a kind" : "One of many"}</div></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;max-width:620px">${p.team.map(t => sp(t) ? `<div style="display:grid;justify-items:center;width:96px"><div style="width:76px;height:76px;border-radius:20px;background:#F1F4FB;display:grid;place-items:center">${pic(t, 72)}</div><span style="font-size:12.5px;font-weight:900;text-align:center;line-height:1.15;margin-top:4px">${esc(ourName(t))}</span></div>`
            : `<div style="display:grid;justify-items:center;width:96px"><div style="width:76px;height:76px;border-radius:20px;background:#F6F8FD;display:grid;place-items:center;color:var(--g-dim);font-size:30px;font-weight:900">?</div><span style="font-size:12px;font-weight:800;text-align:center;line-height:1.15;margin-top:4px;color:var(--g-muted)">${esc(title(t))}<br>not built yet</span></div>`).join("")}</div></div>`;
      };
      show();
      timers.push(setInterval(() => { const e = $("#acenote"); if (e) e.textContent = SC.cast.ace && SC.cast.aceScale < 1 ? ` (shown smaller: really ${(SC.cast.ace.m.size.h / SC.cast.aceScale).toFixed(1)} blocks tall)` : ""; }, 300));
      hints([["◀ ▶", "Pick"], ...(pages > 1 ? [["▲ ▼", "Page"]] : [])]);
      return { key: k => {
        if (k === "left" || k === "right") { sel = (sel + (k === "right" ? 1 : people.length - 1)) % people.length; show(); return true; }
        if ((k === "up" || k === "down") && pages > 1) { const pg = Math.floor(sel / PER), np = (pg + (k === "down" ? 1 : pages - 1)) % pages; sel = Math.min(people.length - 1, np * PER + (sel % PER)); show(); return true; }
        return true; } };
    }
  });
}
