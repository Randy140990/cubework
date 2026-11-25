// ---------- (0.64, John's 07:10) Battles on the spot: in John's own build, cleared, from the battle's camera ----------
// "take a portion of the actual schematic and kind of build that into the battle arena ... it could be that it's
// always looks like my schematic, but you've maybe cleared the grass area ... Obviously camera angles not being
// obscured by stuff ... a distant horizon where you just clear it down a bit."
// Outside, in the caves, the forests, the SAFARI ZONE and MT. SILVER, a battle is fought where you are, in John's
// build: the field and the camera's view cleared of grass, flowers, leaves and fences (and the like) on your screen
// only, nothing solid ever cut (the battle moves to the nearest open spot, or the camera comes in), everyone else near
// it out of the picture, the world beyond as it is. Each preview here is a real place out of John's world or his
// block rooms, laid out as the game lays it (tools/spots.py, tools/battlespot.py; client/BattleSpot.java): his
// blocks, with the mod's own looks where the mod has them (its caves', its pond edges'...), the Retromon Look pack's
// pictures where it has the block, and a plain picture in the block's colours otherwise. (In the game it's the
// world itself, all the way to the horizon; here the blocks round the battle, fading into the distance.)
(function () {
  if (typeof SPOTS === "undefined") return;
  // The custom stages these replace: gone from the menu (and from the game).
  const GONE = new Set(["arena_route", "arena_rocky", "arena_river", "arena_bridge", "arena_cycling", "arena_cycling_sea", "arena_water", "arena_town"]);
  for (let i = PLACES.length - 1; i >= 0; i--) if (GONE.has(PLACES[i].id)) PLACES.splice(i, 1);
  // The pictures the viewer hasn't already (the look pack's, the plain ones).
  for (const k of Object.keys(SPOTTEX)) {
    AREATEX[k] = SPOTTEX[k];
    TEXDEF[k] = [SPOTTEX[k].w, SPOTTEX[k].h, P => P.area(k)];
    const leafy = /_leaves/.test(k), plant = /^lk_(short_grass|tall_grass|fern|large_fern|dandelion|poppy|allium|azure_bluet|blue_orchid|cornflower|lily_of_the_valley|oxeye_daisy|red_tulip|orange_tulip|pink_tulip|white_tulip|torchflower|lilac|peony|rose_bush|sunflower|short_dry_grass|tall_dry_grass)/.test(k);
    const glass = /glass/.test(k);
    MATDEF[k] = plant ? { tex: k, fit: true, cut: true } : leafy ? { tex: k, per: 1, cut: true } : glass ? { tex: k, per: 1, alpha: 0.5 } : { tex: k, per: 1 };
  }
  // A look's picture laid over more than a block (its @4): its own material.
  const mat = m => {
    if (!m) return null;
    const at = m.indexOf("@");
    if (at < 0) return MATDEF[m] ? m : (m.startsWith("area_") && MATDEF[m.replace(/@.*/, "")] ? m : m);
    const base = m.slice(0, at), per = +m.slice(at + 1);
    if (!MATDEF[m] && MATDEF[base]) MATDEF[m] = Object.assign({}, MATDEF[base], { per });
    return MATDEF[m] ? m : base;
  };
  const FACE = ["top", "bottom", "n", "s", "w", "e"];
  function spotRoom(S) {
    const R = new Room(S.id, { name: S.name, floor: "Battle on the spot", W: S.W, D: S.D, H: 8, outside: true });
    R.haze = 0xcfe4f2;
    const G = R.geo, raw = atob(S.cells), y0 = S.look[1] - 0.8;   // (the field's ground at height 0)
    const ground = S.hero[1];
    for (let i = 0; i < S.n; i++) {
      const o = i * 6, x = raw.charCodeAt(o), yy = raw.charCodeAt(o + 1), z = raw.charCodeAt(o + 2), pi = raw.charCodeAt(o + 3) | (raw.charCodeAt(o + 4) << 8), mask = raw.charCodeAt(o + 5);
      const p = S.pal[pi], y = yy - ground;
      const t = mat(p.t), d = mat(p.d), b = mat(p.b);
      const faces = {}; for (let k = 0; k < 6; k++) faces[FACE[k]] = mask & (1 << k) ? (k === 0 ? t : k === 1 ? b : d) : null;
      const box = (x0, y0b, z0, x1, y1, z1, f) => G.box(x + x0, y + y0b, z + z0, x + x1, y + y1, z + z1, f);
      const all = { top: t, bottom: b, all: d };
      switch (p.s) {
        case "cube": case "leaves": case "slab_double": box(0, 0, 0, 1, 1, 1, faces); break;
        case "slab_bottom": box(0, 0, 0, 1, 0.5, 1, Object.assign({}, faces, { top: t })); break;
        case "slab_top": box(0, 0.5, 0, 1, 1, 1, Object.assign({}, faces, { bottom: b })); break;
        case "stairs": {
          const top = p.p.half === "top";
          box(0, top ? 0.5 : 0, 0, 1, top ? 1 : 0.5, 1, all);
          const f = p.p.facing || "north", yA = top ? 0 : 0.5, yB = top ? 0.5 : 1;
          if (f === "north") box(0, yA, 0, 1, yB, 0.5, all); else if (f === "south") box(0, yA, 0.5, 1, yB, 1, all);
          else if (f === "west") box(0, yA, 0, 0.5, yB, 1, all); else box(0.5, yA, 0, 1, yB, 1, all);
          break;
        }
        case "post": { const w = /wall/.test(p.n) ? 0.25 : 0.375; box(w, 0, w, 1 - w, 1, 1 - w, all); break; }
        case "gate": box(0, 0.35, 0.44, 1, 0.9, 0.56, all); break;
        case "pane": box(0, 0, 0.47, 1, 1, 0.53, all); box(0.47, 0, 0, 0.53, 1, 1, all); break;
        case "trapdoor": box(0, p.p.half === "top" ? 0.8125 : 0, 0, 1, p.p.half === "top" ? 1 : 0.1875, 1, all); break;
        case "door": { const f = p.p.facing || "north"; if (f === "north" || f === "south") box(0, 0, f === "north" ? 0.8125 : 0, 1, 1, f === "north" ? 1 : 0.1875, all); else box(f === "west" ? 0.8125 : 0, 0, 0, f === "west" ? 1 : 0.1875, 1, 1, all); break; }
        case "small": box(0.32, 0, 0.32, 0.68, 0.6, 0.68, all); break;
        case "carpet": box(0, 0, 0, 1, 0.0625, 1, { top: t, all: null }); break;
        case "cross": case "tall": {
          G.box(x + 0.12, y, z + 0.5, x + 0.88, y + 0.95, z + 0.5, { s: d, n: d, all: null });
          G.box(x + 0.5, y, z + 0.12, x + 0.5, y + 0.95, z + 0.88, { e: d, w: d, all: null });
          break;
        }
        case "water": G.box(x, y, z, x + 1, y + 0.875, z + 1, { top: "water", all: null }); break;
        case "lava": G.box(x, y, z, x + 1, y + 0.875, z + 1, { top: t, all: null }); break;
        default: box(0, 0, 0, 1, 1, 1, faces);
      }
    }
    // You, them, the two POKéMON (the game's are the creatures; here a trainer and a POKéMON stand at each spot).
    const at = v => ({ x: v[0], y: v[1] - ground, z: v[2] });
    const hero = at(S.hero), foeAt = S.foe ? at(S.foe) : null, mine = at(S.mine), enemy = at(S.enemy);
    const yawTo = (a, b) => Math.atan2(-(b.x - a.x), -(b.z - a.z));
    R.spawn = { x: hero.x, z: hero.z, yaw: yawTo(hero, enemy) };
    R.ramps.push({ rect: [hero.x - 0.6, hero.z - 0.6, hero.x + 0.6, hero.z + 0.6], h: () => hero.y });
    if (foeAt) R.npcs.push({ name: "Their trainer", key: S.id + "_foe", x: foeAt.x, z: foeAt.z, y: foeAt.y, yaw: yawTo(foeAt, mine), look: { skin: S.foeSkin || "youngster" }, role: "trainer",
      talk: () => [{ text: "(The trainer you're battling, behind their POKéMON.)" }] });
    R.pets = [{ sid: "pikachu", x: mine.x, z: mine.z, y: mine.y + (S.wet.mine ? -0.1 : 0), yaw: 180 - yawTo(mine, enemy) / D2R },
              { sid: S.id.includes("cave") || S.id.includes("moon") || S.id.includes("tunnel") ? "geodude" : S.id.includes("forest") ? "caterpie" : "pidgey", x: enemy.x, z: enemy.z, y: enemy.y + (S.wet.enemy ? -0.1 : 0), yaw: 180 - yawTo(enemy, mine) / D2R }];
    R.battleCam = { pos: at(S.cam), look: at(S.look) };
    const moved = Math.hypot(S.moved[0], S.moved[1]);
    R.item("Where", S.note + (moved ? " The battle's moved " + Math.round(moved) + " block" + (moved >= 1.5 ? "s" : "") + " from where you'd stand, to the nearest spot that's open." : " Fought right where you stand."));
    const kinds = Object.entries(S.clearedKinds).map(([k, n]) => k.replace(/_/g, " ") + " (" + n + ")").join(", ");
    R.item("Cleared for the battle", S.cleared ? "On your screen only, put back after: " + kinds + "." : "Nothing in the way here.");
    R.item("The blocks", "John's own, " + (S.inter ? "out of the block room" : "out of his world") + ": the mod's looks where it has them, the Retromon Look pack's pictures where it has the block, otherwise a plain picture in the block's colours.");
    return R;
  }
  for (const S of SPOTS) {
    PLACES.push({ id: S.id, name: S.name, floor: "Battle on the spot", group: "Battle arenas", also: "on the spot outside " + S.name + " " + S.note });
    BUILD[S.id] = () => spotRoom(S);
  }
})();
