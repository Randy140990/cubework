// Every item its own picture, in the icons' style (64 by 64, flat colours, a white shine): medicine bottles by colour
// (POTION purple, SUPER POTION orange, HYPER POTION pink, MAX POTION blue, FULL RESTORE gold), sprays for the status
// cures (ANTIDOTE green, PARLYZ HEAL yellow, AWAKENING blue, BURN HEAL red, ICE HEAL ice, FULL HEAL gold), REVIVE's
// gold diamond, a coiled ESCAPE ROPE, REPEL's spray can, the evolution stones, candy, vitamins, X items, drinks,
// berries, rods and the rest. ITEM_ART[slug] (slug: the name in lower case, other letters as _) is the SVG; the mod
// bakes each to textures/gui/v/item_<slug>.png.
const ITEM_ART = (() => {
  const sv = b => `<svg viewBox="0 0 64 64">${b}</svg>`;
  const shine = (x, y, w, h) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${Math.min(w, h) / 2}" fill="#fff" opacity=".7"/>`;
  // A medicine bottle (the POTION's shape): cap, body in its colour, darker below, a shine.
  const bottle = (lt, dk, cap = "#7D8990") => sv(`<rect x="24" y="6" width="16" height="10" rx="3" fill="${cap}"/><path d="M20 16h24l4 10v24a8 8 0 0 1-8 8H24a8 8 0 0 1-8-8V26z" fill="${lt}"/><path d="M18 34h28v14a6 6 0 0 1-6 6H24a6 6 0 0 1-6-6z" fill="${dk}"/>${shine(24, 22, 5, 10)}`);
  // A spray for a status cure: a trigger head, a nozzle, the bottle in its colour with a white label.
  const spray = (lt, dk) => sv(`<rect x="27" y="4" width="14" height="9" rx="2.5" fill="#7D8990"/><rect x="38" y="6" width="10" height="4" rx="2" fill="#7D8990"/><rect x="25" y="12" width="18" height="6" rx="2" fill="#AEB8BE"/><rect x="17" y="18" width="34" height="40" rx="9" fill="${lt}"/><rect x="17" y="40" width="34" height="18" rx="9" fill="${dk}"/><rect x="22" y="27" width="24" height="10" rx="3" fill="#fff" opacity=".85"/>${shine(21, 21, 4, 14)}`);
  // REPEL's can: a round top, a nozzle, the can in its colour with a band.
  const can = (lt, dk) => sv(`<rect x="27" y="4" width="10" height="7" rx="2" fill="#7D8990"/><path d="M22 14a10 6 0 0 1 20 0v2H22z" fill="#AEB8BE"/><rect x="18" y="15" width="28" height="43" rx="7" fill="${lt}"/><rect x="18" y="30" width="28" height="12" fill="${dk}"/><circle cx="32" cy="36" r="4" fill="#fff" opacity=".9"/>${shine(22, 19, 4, 9)}`);
  // A small drink or ETHER bottle: a narrow neck.
  const flask = (lt, dk, cap = "#E5525C") => sv(`<rect x="27" y="5" width="10" height="7" rx="2" fill="${cap}"/><path d="M27 12h10v8l7 8v24a6 6 0 0 1-6 6H26a6 6 0 0 1-6-6V28l7-8z" fill="${lt}"/><path d="M20 38h24v14a6 6 0 0 1-6 6H26a6 6 0 0 1-6-6z" fill="${dk}"/>${shine(24, 28, 4, 9)}`);
  // An evolution stone: a cut gem in its colour.
  const gem = (lt, md, dk) => sv(`<path d="M32 6l20 14-6 30-14 8-14-8-6-30z" fill="${md}"/><path d="M32 6l20 14-20 8-20-8z" fill="${lt}"/><path d="M32 28l14 22-14 8z" fill="${dk}"/><path d="M32 28L18 50l14 8z" fill="${md}" opacity=".8"/><path d="M24 14l8-5" stroke="#fff" stroke-width="3" stroke-linecap="round" opacity=".7"/>`);
  // A vitamin jar: a lid, the jar with a label in its colour.
  const jar = (lt, dk) => sv(`<rect x="18" y="8" width="28" height="10" rx="3" fill="${dk}"/><rect x="16" y="16" width="32" height="42" rx="8" fill="#F3F6FD"/><rect x="16" y="28" width="32" height="18" fill="${lt}"/><path d="M26 37h12M32 31v12" stroke="#fff" stroke-width="3.5" stroke-linecap="round"/>${shine(20, 20, 4, 6)}`);
  // An X item: a navy bottle with the X.
  const xitem = (lt) => sv(`<rect x="24" y="6" width="16" height="9" rx="3" fill="#7D8990"/><path d="M20 15h24l4 10v25a8 8 0 0 1-8 8H24a8 8 0 0 1-8-8V25z" fill="#3E4F86"/><path d="M24 30l16 16M40 30L24 46" stroke="${lt}" stroke-width="6" stroke-linecap="round"/>${shine(23, 20, 4, 7)}`);
  // A berry: round, its leaf on top.
  const berry = (lt, dk) => sv(`<circle cx="32" cy="38" r="19" fill="${lt}"/><path d="M15 40a17 17 0 0 0 34 0" fill="${dk}" opacity=".6"/><path d="M32 20c-2-6 2-12 10-13-1 7-5 11-10 13z" fill="#4FB04A"/><path d="M32 21v-4" stroke="#6B4A2A" stroke-width="3" stroke-linecap="round"/>${shine(22, 28, 5, 8)}`);
  // An apricorn: a ball-like fruit.
  const apricorn = (lt, dk) => sv(`<circle cx="32" cy="36" r="20" fill="${lt}"/><path d="M12 36a20 20 0 0 0 40 0z" fill="${dk}"/><path d="M32 16c-1-5 2-9 7-10" stroke="#6B4A2A" stroke-width="3" fill="none" stroke-linecap="round"/>${shine(22, 24, 5, 8)}`);
  const mail = (lt) => sv(`<rect x="8" y="16" width="48" height="34" rx="5" fill="#fff"/><path d="M8 18l24 18 24-18" fill="none" stroke="${lt}" stroke-width="4"/><rect x="8" y="16" width="48" height="34" rx="5" fill="none" stroke="${lt}" stroke-width="3"/>`);
  const holder = (body) => sv(body);
  const A = {
    potion: bottle("#B86AE0", "#8E44C0"), super_potion: bottle("#FF9A6A", "#E5604A"), hyper_potion: bottle("#FF8AC4", "#D9408A"),
    max_potion: bottle("#7CC2FA", "#3C7FE0"), full_restore: bottle("#FFE07A", "#F0B030", "#D9A520"), secretpotion: bottle("#9FE6C8", "#3DB88A"),
    berry_juice: bottle("#FFB07A", "#E07A3A", "#9E6A3A"), moomoo_milk: flask("#FFFFFF", "#E3EAF5", "#4A8FE0"),
    antidote: spray("#86E08E", "#3DB860"), parlyz_heal: spray("#FFE36A", "#E8B820"), awakening: spray("#7ADCF0", "#2FA0C8"),
    burn_heal: spray("#FF8A7A", "#E5484F"), ice_heal: spray("#D6F2FF", "#8ACCF0"), full_heal: spray("#FFF3C4", "#F0C850"),
    revive: holder(`<path d="M32 6l18 26-18 26-18-26z" fill="#F7C744"/><path d="M32 6l18 26H14z" fill="#FFE38A"/><path d="M32 18l9 14-9 14-9-14z" fill="#fff" opacity=".55"/>`),
    max_revive: holder(`<circle cx="32" cy="32" r="26" fill="none" stroke="#FFE38A" stroke-width="4"/><path d="M32 8l17 24-17 24-17-24z" fill="#F0B030"/><path d="M32 8l17 24H15z" fill="#FFE38A"/><path d="M32 20l8 12-8 12-8-12z" fill="#fff" opacity=".6"/>`),
    sacred_ash: holder(`<path d="M14 40h36l-4 18H18z" fill="#E9B43C"/><path d="M20 40c0-10 6-18 12-26 6 8 12 16 12 26z" fill="#FF8A5C"/><path d="M26 40c0-6 3-10 6-14 3 4 6 8 6 14z" fill="#FFE07A"/>`),
    escape_rope: holder(`<circle cx="32" cy="34" r="20" fill="none" stroke="#C89350" stroke-width="7"/><circle cx="32" cy="34" r="10" fill="none" stroke="#E0B070" stroke-width="6"/><path d="M48 22c6-6 10-10 10-16" stroke="#C89350" stroke-width="6" fill="none" stroke-linecap="round"/><path d="M18 26a16 16 0 0 1 10-8" stroke="#fff" stroke-width="2.5" fill="none" opacity=".6"/>`),
    repel: can("#B48AF0", "#7A4CD0"), super_repel: can("#FFD86A", "#E0A020"), max_repel: can("#7FE0A6", "#2FA86C"),
    ether: flask("#8EC4F6", "#4A8FE0", "#7D8990"), max_ether: flask("#8EC4F6", "#2F64B0", "#F0B43C"),
    elixer: flask("#FFA8D2", "#E060A0", "#7D8990"), max_elixer: flask("#FFA8D2", "#C03A80", "#F0B43C"),
    fresh_water: flask("#BFE6FF", "#6FB8F0", "#4A8FE0"), soda_pop: flask("#FF9AAE", "#E5485C", "#E5525C"), lemonade: flask("#FFF08A", "#F0C83C", "#41BF84"),
    fire_stone: gem("#FFC27A", "#F2803A", "#C2461E"), water_stone: gem("#9FD8FF", "#4A8FE0", "#2F64B0"), thunderstone: gem("#FFF3A0", "#F2C744", "#C89A1E"),
    leaf_stone: gem("#B8F08A", "#5DB850", "#2F8A3A"), moon_stone: gem("#D8D2F0", "#8A84B8", "#5C567E"), sun_stone: gem("#FFE08A", "#FF9A3A", "#D8601E"),
    everstone: gem("#E6E6E6", "#A8A8A8", "#7A7A7A"), hard_stone: gem("#D8C8A8", "#A89070", "#7A6448"),
    rare_candy: holder(`<path d="M6 32l10-8v16z" fill="#4A8FE0"/><path d="M58 32l-10-8v16z" fill="#4A8FE0"/><ellipse cx="32" cy="32" rx="17" ry="13" fill="#7CC2FA"/><path d="M22 26l20 12M22 38l20-12" stroke="#fff" stroke-width="3.5" opacity=".8"/>`),
    hp_up: jar("#E5525C", "#B92F3A"), protein: jar("#F59A2A", "#C8761A"), iron: jar("#8A93A6", "#5C6478"), carbos: jar("#41BF84", "#228A52"),
    calcium: jar("#4A8FE0", "#2F64B0"), pp_up: jar("#B48AF0", "#7A4CD0"),
    x_attack: xitem("#FF8A7A"), x_defend: xitem("#7CC2FA"), x_speed: xitem("#FFE07A"), x_special: xitem("#B48AF0"), x_accuracy: xitem("#86E08E"),
    dire_hit: xitem("#FFB86A"), guard_spec: xitem("#FFFFFF"),
    nugget: holder(`<path d="M12 40c0-12 10-22 22-22 10 0 18 8 18 18 0 10-10 16-22 16-10 0-18-4-18-12z" fill="#F0B030"/><path d="M18 34c2-8 10-12 18-12" stroke="#FFE38A" stroke-width="4" fill="none" stroke-linecap="round"/>`),
    pearl: holder(`<circle cx="32" cy="34" r="16" fill="#F3F0FA"/><circle cx="26" cy="28" r="5" fill="#fff"/><circle cx="32" cy="34" r="16" fill="none" stroke="#D8D0E8" stroke-width="2"/>`),
    big_pearl: holder(`<circle cx="32" cy="32" r="22" fill="#F3F0FA"/><circle cx="24" cy="24" r="7" fill="#fff"/><circle cx="32" cy="32" r="22" fill="none" stroke="#D8D0E8" stroke-width="2.5"/>`),
    stardust: holder(`<path d="M32 10l5 14 14 2-11 9 4 15-12-8-12 8 4-15-11-9 14-2z" fill="#FFD45A"/><circle cx="14" cy="16" r="3" fill="#FFE38A"/><circle cx="50" cy="48" r="3" fill="#FFE38A"/>`),
    star_piece: holder(`<path d="M32 6l6 18 18 2-14 11 5 18-15-10-15 10 5-18-14-11 18-2z" fill="#F06A6A"/><path d="M32 18l3 9 9 1-7 5 3 9-8-5-8 5 3-9-7-5 9-1z" fill="#FFD45A"/>`),
    old_rod: holder(`<path d="M10 56L50 8" stroke="#A8763A" stroke-width="5" stroke-linecap="round"/><path d="M50 8v34" stroke="#26304A" stroke-width="2"/><circle cx="50" cy="44" r="4" fill="#E5525C"/>`),
    good_rod: holder(`<path d="M10 56L50 8" stroke="#4A8FE0" stroke-width="5" stroke-linecap="round"/><path d="M50 8v34" stroke="#26304A" stroke-width="2"/><circle cx="50" cy="44" r="4" fill="#E5525C"/><circle cx="20" cy="44" r="5" fill="#26304A"/>`),
    super_rod: holder(`<path d="M10 56L50 8" stroke="#8F60DE" stroke-width="5" stroke-linecap="round"/><path d="M50 8v34" stroke="#26304A" stroke-width="2"/><circle cx="50" cy="44" r="4" fill="#FFD45A"/><circle cx="20" cy="44" r="5" fill="#26304A"/>`),
    bicycle: holder(`<circle cx="16" cy="42" r="11" fill="none" stroke="#26304A" stroke-width="4"/><circle cx="48" cy="42" r="11" fill="none" stroke="#26304A" stroke-width="4"/><path d="M16 42l10-18h16l6 18M26 24l8 18h-18M38 18h8" stroke="#E5525C" stroke-width="4" fill="none" stroke-linejoin="round" stroke-linecap="round"/>`),
    oaks_parcel: null,
  };
  const B = { berry: ["#E5525C", "#B92F3A"], gold_berry: ["#FFD45A", "#E0A020"], mint_berry: ["#86E0C8", "#3DB89A"], ice_berry: ["#BFE6FF", "#6FB8F0"],
    burnt_berry: ["#FF8A5C", "#C2461E"], bitter_berry: ["#B8D86A", "#7A9A2A"], miracleberry: ["#B48AF0", "#7A4CD0"], mysteryberry: ["#FF8AC4", "#D9408A"],
    psncureberry: ["#C68AE0", "#8E44C0"], przcureberry: ["#FFE36A", "#E8B820"] };
  for (const [k, [l, d]] of Object.entries(B)) A[k] = berry(l, d);
  const C = { red_apricorn: ["#F27682", "#D94452"], blu_apricorn: ["#7CC2FA", "#3C7FE0"], ylw_apricorn: ["#FFE07A", "#F0B030"], grn_apricorn: ["#86E08E", "#3DB860"],
    wht_apricorn: ["#FFFFFF", "#D8DEE8"], blk_apricorn: ["#5C6478", "#26304A"], pnk_apricorn: ["#FFA8D2", "#E060A0"] };
  for (const [k, [l, d]] of Object.entries(C)) A[k] = apricorn(l, d);
  for (const [k, c] of [["flower_mail", "#FF8AC4"], ["surf_mail", "#4A8FE0"], ["litebluemail", "#7CC2FA"], ["portraitmail", "#F0B43C"], ["lovely_mail", "#E5525C"],
    ["eon_mail", "#8F60DE"], ["morph_mail", "#41BF84"], ["bluesky_mail", "#3C8DE4"], ["music_mail", "#F59A2A"], ["mirage_mail", "#B48AF0"]]) A[k] = mail(c);
  delete A.oaks_parcel;
  return A;
})();
const itemSlug = n => String(n).toLowerCase().replace(/é/g, "e").replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");
