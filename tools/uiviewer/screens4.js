// ===================== Characters: every skin in the mod, side by side =====================
// [skin, name, who they are, where you meet them, their team (sids, as in the old games), their partner shown beside them]
const CAST = [
  ["home", "You, your rival and home", [
    [{ outfit: OUTFIT.defaults(false) }, "YOU (BOY)", "The boy as he starts, in the mod's default clothes.", "Your house, PALLET TOWN", [], "pikachu"],
    [{ outfit: OUTFIT.defaults(true) }, "YOU (GIRL)", "The girl as she starts, in the mod's default clothes.", "Your house, PALLET TOWN", [], "pikachu"],
    [{ skin: "red" }, "RED", "The old games' hero, for a cameo.", "MT. SILVER, later", ["pikachu", "venusaur", "charizard", "blastoise"], "pikachu"],
    [{ skin: "blue" }, "BLUE", "Your rival, PROF. OAK's grandson. You'll battle him again and again.", "Everywhere you go", ["pidgeot", "alakazam", "rhydon", "arcanine"], "pidgeot"],
    [{ skin: "oak" }, "PROF. OAK", "The POKéMON PROF. He gives you your first partner and the POKéDEX.", "His lab, PALLET TOWN", [], null],
    [{ skin: "middle_aged_woman" }, "MUM", "Your mum. Stand-in skin for now: MIDDLE-AGED WOMAN. Her own skin is still to make.", "Your house", [], null],
    [{ skin: "brunette_girl" }, "DAISY", "Your rival's sister. Stand-in skin for now: BRUNETTE GIRL. Her own is still to make.", "Rival's house", [], null],
  ]],
  ["leaders", "Gym leaders", [
    [{ skin: "leader_brock" }, "BROCK", "PEWTER CITY GYM. The rock-solid trainer. BOULDERBADGE.", "PEWTER CITY", ["geodude", "onix"], "onix"],
    [{ skin: "leader_misty" }, "MISTY", "CERULEAN CITY GYM. The tomboyish mermaid. CASCADEBADGE.", "CERULEAN CITY", ["staryu", "starmie"], "starmie"],
    [{ skin: "leader_lt_surge" }, "LT. SURGE", "VERMILION CITY GYM. The lightning American. THUNDERBADGE.", "VERMILION CITY", ["voltorb", "pikachu", "raichu"], "raichu"],
    [{ skin: "leader_erika" }, "ERIKA", "CELADON CITY GYM. The nature-loving princess. RAINBOWBADGE.", "CELADON CITY", ["victreebel", "tangela", "vileplume"], "vileplume"],
    [{ skin: "leader_koga" }, "KOGA", "FUCHSIA CITY GYM. The poisonous ninja master. SOULBADGE.", "FUCHSIA CITY", ["koffing", "muk", "koffing", "weezing"], "weezing"],
    [{ skin: "leader_sabrina" }, "SABRINA", "SAFFRON CITY GYM. The master of psychic POKéMON. MARSHBADGE.", "SAFFRON CITY", ["kadabra", "mr_mime", "venomoth", "alakazam"], "alakazam"],
    [{ skin: "leader_blaine" }, "BLAINE", "CINNABAR ISLAND GYM. The hot-headed quiz master. VOLCANOBADGE.", "CINNABAR ISLAND", ["growlithe", "ponyta", "rapidash", "arcanine"], "arcanine"],
    [{ skin: "leader_giovanni" }, "GIOVANNI", "VIRIDIAN CITY GYM, the last one. EARTHBADGE.", "VIRIDIAN CITY", ["rhyhorn", "dugtrio", "nidoqueen", "nidoking", "rhydon"], "rhydon"],
  ]],
  ["elite", "Elite Four and champion", [
    [{ skin: "lorelei" }, "LORELEI", "The first of the ELITE FOUR: ice.", "INDIGO PLATEAU", ["dewgong", "cloyster", "slowbro", "jynx", "lapras"], "cloyster"],
    [{ skin: "bruno" }, "BRUNO", "The second: fighting, with two ONIX.", "INDIGO PLATEAU", ["onix", "hitmonchan", "hitmonlee", "onix", "machamp"], "machamp"],
    [{ skin: "agatha" }, "AGATHA", "The third: ghosts. PROF. OAK's old rival.", "INDIGO PLATEAU", ["gengar", "golbat", "haunter", "arbok", "gengar"], "gengar"],
    [{ skin: "lance" }, "LANCE", "The last: the dragon master.", "INDIGO PLATEAU", ["gyarados", "dragonair", "dragonair", "aerodactyl", "dragonite"], null],
    [{ skin: "blue" }, "BLUE (CHAMPION)", "Your rival again, as the CHAMPION.", "INDIGO PLATEAU", ["pidgeot", "alakazam", "rhydon", "exeggutor", "arcanine"], "pidgeot"],
  ]],
  ["rocket", "Team Rocket", [
    [{ skin: "jessie" }, "JESSIE", "Half of the double trouble that follows you round Kanto.", "MT. MOON, the GAME CORNER, SILPH CO.", ["ekans", "arbok", "meowth"], "arbok"],
    [{ creature: "meowth" }, "MEOWTH", "Their talking partner, always with them.", "With JESSIE and JAMES", [], null],
    [{ skin: "james" }, "JAMES", "The other half.", "MT. MOON, the GAME CORNER, SILPH CO.", ["koffing", "weezing", "meowth"], "weezing"],
    [{ skin: "rocket" }, "GRUNT", "TEAM ROCKET's grunts, in black with the red R.", "MT. MOON, CELADON, LAVENDER, SAFFRON", ["rattata", "zubat", "ekans", "drowzee"], "zubat"],
    [{ skin: "giovanni" }, "GIOVANNI", "TEAM ROCKET's boss, out of his gym.", "The GAME CORNER hideout, SILPH CO.", ["onix", "rhyhorn", "persian"], "persian"],
  ]],
  ["helpers", "People who help", [
    [{ skin: "nurse" }, "NURSE JOY", "At every POKéMON CENTER, healing your team.", "Every POKéMON CENTER", [], "chansey"],
    [{ skin: "officer_jenny" }, "OFFICER JENNY", "Keeping the peace, with her GROWLITHE.", "Cities, the police box", [], "growlithe"],
    [{ skin: "clerk" }, "CLERK", "Behind the counter in every POKé MART.", "Every POKé MART", [], null],
    [{ skin: "gym_guide" }, "GYM GUIDE", "Waits at each gym's door with tips.", "Every gym", [], null],
    [{ skin: "link_receptionist" }, "LINK RECEPTIONIST", "Runs the trading and battle desks upstairs.", "POKéMON CENTERs, upstairs", [], null],
    [{ skin: "mr_fuji" }, "MR. FUJI", "The kind old man of LAVENDER TOWN.", "LAVENDER TOWN", [], null],
    [{ skin: "scientist" }, "SCIENTIST", "Researchers at the labs and SILPH CO.", "CINNABAR LAB, SILPH CO.", ["magnemite", "voltorb"], "magnemite"],
    [{ skin: "silph_president" }, "SILPH PRESIDENT", "Runs SILPH CO.", "SILPH CO., SAFFRON CITY", [], null],
    [{ skin: "silph_worker_m" }, "SILPH WORKER", "SILPH CO.'s staff.", "SILPH CO.", [], null],
    [{ skin: "silph_worker_f" }, "SILPH WORKER", "SILPH CO.'s staff.", "SILPH CO.", [], null],
    [{ skin: "guard" }, "GUARD", "Stands at the gates into SAFFRON.", "SAFFRON's gates", [], null],
    [{ skin: "warden" }, "WARDEN", "Of the SAFARI ZONE. He's lost his teeth.", "FUCHSIA CITY", [], null],
    [{ skin: "safari_zone_worker" }, "SAFARI WORKER", "At the SAFARI ZONE gate.", "SAFARI ZONE", [], null],
    [{ skin: "bike_shop_clerk" }, "BIKE SHOP", "Sells the BICYCLE (if you have the voucher).", "CERULEAN CITY", [], null],
    [{ skin: "captain" }, "CAPTAIN", "Of the S.S. ANNE. Seasick.", "S.S. ANNE", [], null],
    [{ skin: "cook" }, "COOK", "In the S.S. ANNE's kitchen.", "S.S. ANNE", [], null],
    [{ skin: "waiter" }, "WAITER", "On the S.S. ANNE.", "S.S. ANNE", [], null],
    [{ skin: "leader" }, "LEADER", "A generic gym leader, for gyms without their own.", "Spare", [], null],
  ]],
  ["trainers", "Trainers", [
    [{ skin: "youngster" }, "YOUNGSTER", "Shorts all year round.", "ROUTES 3, 6, 11, 24, 25", ["rattata", "ekans"], "rattata"],
    [{ skin: "cooltrainer_m" }, "COOLTRAINER", "A strong, all-round trainer.", "VICTORY ROAD, late routes", ["nidorino", "poliwhirl"], "nidorino"],
    [{ skin: "cooltrainer_f" }, "COOLTRAINER", "A strong, all-round trainer.", "VICTORY ROAD, late routes", ["clefairy", "ivysaur"], "clefairy"],
    [{ skin: "super_nerd" }, "SUPER NERD", "Loves machines and fossils.", "MT. MOON, the POWER PLANT", ["magnemite", "voltorb"], "voltorb"],
    [{ skin: "hiker" }, "HIKER", "Up every mountain.", "MT. MOON, ROCK TUNNEL", ["geodude", "onix", "machop"], "geodude"],
    [{ skin: "swimmer" }, "SWIMMER", "Out in the sea routes.", "ROUTES 19, 20, 21", ["tentacool", "shellder", "horsea"], "tentacool"],
    [{ skin: "gentleman" }, "GENTLEMAN", "Well-to-do, with well-bred POKéMON.", "S.S. ANNE", ["growlithe", "ponyta"], "growlithe"],
    [{ skin: "biker" }, "BIKER", "On CYCLING ROAD.", "ROUTES 16, 17", ["koffing", "grimer"], "koffing"],
    [{ skin: "channeler" }, "CHANNELER", "In the POKéMON TOWER.", "LAVENDER TOWN", ["gastly", "haunter"], "gastly"],
    [{ skin: "gambler" }, "GAMBLER", "Bets on everything.", "ROUTES 8, 10", ["poliwag", "growlithe"], "poliwag"],
    [{ skin: "fisher" }, "FISHER", "On every pier.", "ROUTES 12, 13", ["goldeen", "tentacool", "shellder"], "goldeen"],
    [{ skin: "sailor" }, "SAILOR", "On the S.S. ANNE.", "S.S. ANNE, VERMILION", ["machop", "shellder"], "machop"],
    [{ skin: "beauty" }, "BEAUTY", "Elegant, with pretty POKéMON.", "ROUTES 15, 20, CELADON GYM", ["oddish", "bellsprout"], "oddish"],
    [{ skin: "rocker" }, "ROCKER", "In the gyms and on the road.", "VERMILION GYM, ROUTE 12", ["voltorb", "magnemite"], "voltorb"],
    [{ skin: "gameboy_kid" }, "KID", "Plays games instead of training.", "Towns", [], null],
  ]],
  ["folk", "People in town", [
    [{ skin: "girl" }, "GIRL", "Out and about.", "Towns", [], null],
    [{ skin: "little_girl" }, "LITTLE GIRL", "Running about.", "Towns", [], null],
    [{ skin: "little_boy" }, "LITTLE BOY", "Running about.", "Towns", [], null],
    [{ skin: "brunette_girl" }, "LASS", "Stand-in for now.", "Towns", [], null],
    [{ skin: "middle_aged_man" }, "MAN", "Has a lot to say.", "Towns", [], null],
    [{ skin: "middle_aged_woman" }, "WOMAN", "Has a lot to say.", "Towns", [], null],
    [{ skin: "gramps" }, "GRAMPS", "Remembers the old days.", "Towns", [], null],
    [{ skin: "granny" }, "GRANNY", "Gives good advice.", "Towns", [], null],
    [{ skin: "balding_guy" }, "BALDING MAN", "Seen it all.", "Towns", [], null],
    [{ skin: "fishing_guru" }, "FISHING GURU", "Hands out rods.", "VERMILION, FUCHSIA, ROUTE 12", [], null],
  ]],
];
const STILL_TO_MAKE = ["MUM", "DAISY", "BILL", "COPYCAT", "BUG CATCHER", "LASS", "CAMPER", "PICNICKER", "BIRD KEEPER", "BLACK BELT", "BURGLAR", "ENGINEER", "JUGGLER", "TAMER", "PSYCHIC", "POKéMANIAC", "BLACKBELT MASTER", "ROCKET SCIENTIST"];
for (const [id, name, people] of CAST) {
  screen("cast-" + id, "Characters", name, {
    scene: "cast",
    what: `Everyone in this group as they look in the mod: Minecraft's player model in the mod's own skins (textures/entity/npc), corners just softened. Left and right to pick: they step forward and wave, with their partner beside them.`,
    tags: [`${people.length} people`],
    mod: "NpcRenderer gives each person their skin by name: the nurse, the clerk, trainers, gym leaders and townsfolk.",
    try: ["Left and right to pick, up and down for another page if there is one.", `Still to make: ${STILL_TO_MAKE.join(", ")}.`],
    make() {
      backdrop("radial-gradient(ellipse at 50% 35%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)");
      const PER = 8, pages = Math.ceil(people.length / PER);
      let sel = 0;
      header(ICON.card, name.toUpperCase(), `${people.length} PEOPLE`);
      const info = box(30, 520, 1220, 170, "g-panel", ""); info.style.padding = "16px 24px";
      const pager = box(1080, 30, 170, null, "g-panel soft", ""); pager.style.borderRadius = "999px"; pager.style.padding = "8px 16px"; pager.style.textAlign = "center"; pager.style.fontWeight = "900";
      const show = () => {
        const page = Math.floor(sel / PER), list = people.slice(page * PER, page * PER + PER);
        const specs = list.map(([spec, nm, , , , ace]) => ({ spec: spec.creature ? { skin: "__creature" } : spec, creature: spec.creature, ace }));
        SC.cast.show(specs, sel - page * PER);
        pager.innerHTML = pages > 1 ? `Page ${page + 1} of ${pages}` : `${people.length} here`;
        const [spec, nm, who, where, team, ace] = people[sel];
        info.innerHTML = `<div style="display:grid;grid-template-columns:1fr auto;gap:16px;align-items:start">
          <div style="display:grid;gap:6px"><div style="display:flex;align-items:center;gap:14px"><span class="g-title" style="font-size:36px">${esc(nm)}</span><span class="g-label">${esc(where)}</span></div>
            <div style="font-size:20px;font-weight:800;color:#E8F4EF">${esc(who)}</div>
            ${ace ? `<div style="font-size:15px;font-weight:800;color:#A9D4C6">With them: ${esc(ourName(ace))}<span id="acenote"></span></div>` : ""}
            <div class="g-label" style="margin-top:4px">${spec.creature ? "A POKéMON: Retromon's own model" : spec.outfit ? "Skin: made from your clothes" : spec.player ? "Skin: player/" + spec.player : "Skin: npc/" + esc(spec.skin)}</div></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;max-width:620px">${team.map(t => sp(t) ? `<div style="display:grid;justify-items:center;width:96px"><div style="width:76px;height:76px;border-radius:20px;background:rgba(255,255,255,.08);display:grid;place-items:center">${pic(t, 72)}</div><span style="font-size:12.5px;font-weight:900;text-align:center;line-height:1.15;margin-top:4px">${esc(ourName(t))}</span></div>`
            : `<div style="display:grid;justify-items:center;width:96px"><div style="width:76px;height:76px;border-radius:20px;background:rgba(255,255,255,.05);display:grid;place-items:center;color:#6B9A8E;font-size:30px;font-weight:900">?</div><span style="font-size:12px;font-weight:800;text-align:center;line-height:1.15;margin-top:4px;color:#A9D4C6">${esc(title(t))}<br>not built yet</span></div>`).join("")}</div></div>`;
      };
      show();
      timers.push(setInterval(() => { const e = $("#acenote"); if (e) e.textContent = SC.cast.ace && SC.cast.aceScale < 1 ? ` (shown smaller: really ${(SC.cast.ace.m.size.h / SC.cast.aceScale).toFixed(1)} blocks tall)` : ""; }, 300));
      hints([["◀ ▶", "Pick"], ["B", "Back"]]);
      return { key: k => {
        if (k === "left" || k === "right") { sel = (sel + (k === "right" ? 1 : people.length - 1)) % people.length; show(); return true; }
        if ((k === "up" || k === "down") && pages > 1) { sel = Math.max(0, Math.min(people.length - 1, sel + (k === "down" ? PER : -PER))); show(); return true; }
        return true; } };
    }
  });
}
