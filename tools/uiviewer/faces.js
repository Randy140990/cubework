// ---------- 2D faces of our creatures, for the top of the menu (where Let's Go has its Pikachu): flat, bold outlines,
// soft gradients. The four we start with have their own; anyone else is their picture as a sticker. ----------
const FACE_INK = "#2A2130";
const FACES = {
  // ZAPGLIDER: a glider squirrel. Yellow, big round ears pink inside, a dark mask, blue sparks on its cheeks.
  pikachu: `<svg viewBox="0 0 200 180"><defs><linearGradient id="fzY" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFE57A"/><stop offset="1" stop-color="#EBA417"/></linearGradient>
    <radialGradient id="fzS" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset=".5" stop-color="#fff" stop-opacity="0"/></radialGradient></defs>
    <g stroke="#7A4A10" stroke-width="6" stroke-linejoin="round">
    <path d="M44 86C18 52 22 14 50 8c26-4 40 30 36 66z" fill="url(#fzY)"/><path d="M156 86c26-34 22-72-6-78-26-4-40 30-36 66z" fill="url(#fzY)"/></g>
    <path d="M52 70c-12-20-10-42 2-46 14-2 22 18 20 40z" fill="#FFB7A6"/><path d="M148 70c12-20 10-42-2-46-14-2-22 18-20 40z" fill="#FFB7A6"/>
    <ellipse cx="100" cy="114" rx="76" ry="60" fill="url(#fzY)" stroke="#7A4A10" stroke-width="6"/><ellipse cx="100" cy="114" rx="76" ry="60" fill="url(#fzS)"/>
    <path d="M38 104c14-22 44-24 58-6-6 20-40 28-58 6zM162 104c-14-22-44-24-58-6 6 20 40 28 58 6z" fill="${FACE_INK}"/>
    <circle cx="70" cy="102" r="12" fill="#fff"/><circle cx="130" cy="102" r="12" fill="#fff"/><circle cx="72" cy="104" r="7" fill="${FACE_INK}"/><circle cx="128" cy="104" r="7" fill="${FACE_INK}"/><circle cx="75" cy="100" r="2.6" fill="#fff"/><circle cx="131" cy="100" r="2.6" fill="#fff"/>
    <ellipse cx="100" cy="142" rx="30" ry="20" fill="#FFF6E0"/><ellipse cx="100" cy="131" rx="7" ry="5" fill="#3A2A20"/><path d="M88 144q12 10 24 0" stroke="#3A2A20" stroke-width="4" fill="none" stroke-linecap="round"/>
    <path d="M30 136l14-7-5 10 15-7M170 136l-14-7 5 10-15-7" stroke="#3E95EE" stroke-width="5" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  // CINDERKID: a little black goat. Charcoal fur, cream horns, ears out to the side, a flame on top, amber eyes.
  charmander: `<svg viewBox="0 0 200 180"><defs><linearGradient id="fcB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5A5464"/><stop offset="1" stop-color="#26222E"/></linearGradient>
    <linearGradient id="fcF" x1="0" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#E8501E"/><stop offset=".45" stop-color="#FF9A2E"/><stop offset="1" stop-color="#FFE85A"/></linearGradient>
    <radialGradient id="fcS" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#fff" stop-opacity=".3"/><stop offset=".5" stop-color="#fff" stop-opacity="0"/></radialGradient></defs>
    <path d="M100 8c-20 26-24 44-12 60 2-10 6-16 12-20 6 6 10 12 10 22 18-14 14-38-10-62z" fill="url(#fcF)" stroke="#B8401E" stroke-width="5" stroke-linejoin="round"/>
    <path d="M72 70c-6-16-2-30 10-38-2 14 2 24 10 32zM128 70c6-16 2-30-10-38 2 14-2 24-10 32z" fill="#F4E8CC" stroke="#8A7550" stroke-width="5" stroke-linejoin="round"/>
    <ellipse cx="30" cy="104" rx="30" ry="13" transform="rotate(-14 30 104)" fill="url(#fcB)" stroke="#17141C" stroke-width="5"/><ellipse cx="170" cy="104" rx="30" ry="13" transform="rotate(14 170 104)" fill="url(#fcB)" stroke="#17141C" stroke-width="5"/>
    <ellipse cx="26" cy="104" rx="16" ry="5" transform="rotate(-14 26 104)" fill="#E9A0A8"/><ellipse cx="174" cy="104" rx="16" ry="5" transform="rotate(14 174 104)" fill="#E9A0A8"/>
    <ellipse cx="100" cy="114" rx="66" ry="58" fill="url(#fcB)" stroke="#17141C" stroke-width="6"/><ellipse cx="100" cy="114" rx="66" ry="58" fill="url(#fcS)"/>
    <ellipse cx="74" cy="104" rx="15" ry="17" fill="#fff"/><ellipse cx="126" cy="104" rx="15" ry="17" fill="#fff"/><circle cx="76" cy="107" r="9" fill="#F7A928"/><circle cx="124" cy="107" r="9" fill="#F7A928"/><circle cx="76" cy="108" r="5" fill="${FACE_INK}"/><circle cx="124" cy="108" r="5" fill="${FACE_INK}"/><circle cx="79" cy="102" r="3" fill="#fff"/><circle cx="127" cy="102" r="3" fill="#fff"/>
    <ellipse cx="100" cy="146" rx="30" ry="20" fill="#9A93A4"/><ellipse cx="91" cy="140" rx="4" ry="3" fill="#2A2632"/><ellipse cx="109" cy="140" rx="4" ry="3" fill="#2A2632"/><path d="M90 152q10 8 20 0" stroke="#2A2632" stroke-width="4" fill="none" stroke-linecap="round"/></svg>`,
  // FERNADILLO: a leafy armadillo. Green, darker bands of armour over its head, a sprout on top, a tan snout.
  bulbasaur: `<svg viewBox="0 0 200 180"><defs><linearGradient id="ffG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9BE06E"/><stop offset="1" stop-color="#3E9A3E"/></linearGradient>
    <linearGradient id="ffL" x1="0" y1="1" x2="1" y2="0"><stop offset="0" stop-color="#2F8A3A"/><stop offset="1" stop-color="#A8F07A"/></linearGradient></defs>
    <path d="M100 52c-4-24 8-40 34-44-2 26-14 40-34 44z" fill="url(#ffL)" stroke="#226B2C" stroke-width="5" stroke-linejoin="round"/><path d="M100 54c-2-18-14-28-34-30 2 18 14 28 34 30z" fill="url(#ffL)" stroke="#226B2C" stroke-width="5" stroke-linejoin="round"/>
    <circle cx="40" cy="80" r="16" fill="url(#ffG)" stroke="#226B2C" stroke-width="5"/><circle cx="160" cy="80" r="16" fill="url(#ffG)" stroke="#226B2C" stroke-width="5"/>
    <ellipse cx="100" cy="112" rx="70" ry="60" fill="url(#ffG)" stroke="#226B2C" stroke-width="6"/>
    <path d="M42 92q58-48 116 0" stroke="#2F7F36" stroke-width="7" fill="none"/><path d="M50 74q50-36 100 0" stroke="#2F7F36" stroke-width="6" fill="none"/>
    <circle cx="72" cy="112" r="12" fill="#fff"/><circle cx="128" cy="112" r="12" fill="#fff"/><circle cx="74" cy="114" r="7" fill="${FACE_INK}"/><circle cx="126" cy="114" r="7" fill="${FACE_INK}"/><circle cx="77" cy="110" r="2.6" fill="#fff"/><circle cx="129" cy="110" r="2.6" fill="#fff"/>
    <ellipse cx="100" cy="146" rx="32" ry="20" fill="#EAD9A6"/><ellipse cx="100" cy="138" rx="8" ry="5" fill="#5A4630"/><path d="M88 152q12 8 24 0" stroke="#5A4630" stroke-width="4" fill="none" stroke-linecap="round"/></svg>`,
  // TIDAPIR: a little tapir of the sea. Blue, a long soft snout, a curl of wave on top, round ears.
  squirtle: `<svg viewBox="0 0 200 180"><defs><linearGradient id="ftB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8ACBFA"/><stop offset="1" stop-color="#2F6FC0"/></linearGradient>
    <linearGradient id="ftW" x1="0" y1="1" x2="1" y2="0"><stop offset="0" stop-color="#5FC8F2"/><stop offset="1" stop-color="#FFFFFF"/></linearGradient></defs>
    <path d="M70 60c4-30 34-48 62-36-18 2-26 12-24 24 10-8 22-6 28 4-20-4-34 4-40 18z" fill="url(#ftW)" stroke="#2C79B8" stroke-width="5" stroke-linejoin="round"/>
    <circle cx="44" cy="74" r="16" fill="url(#ftB)" stroke="#1F4F8E" stroke-width="5"/><circle cx="156" cy="74" r="16" fill="url(#ftB)" stroke="#1F4F8E" stroke-width="5"/><circle cx="44" cy="74" r="7" fill="#F2A7B6"/><circle cx="156" cy="74" r="7" fill="#F2A7B6"/>
    <ellipse cx="100" cy="106" rx="70" ry="56" fill="url(#ftB)" stroke="#1F4F8E" stroke-width="6"/>
    <path d="M70 130q30-10 60 0v18q-4 20-30 22-26-2-30-22z" fill="#BFE3FB" stroke="#1F4F8E" stroke-width="5" stroke-linejoin="round"/>
    <circle cx="72" cy="100" r="12" fill="#fff"/><circle cx="128" cy="100" r="12" fill="#fff"/><circle cx="74" cy="102" r="7" fill="${FACE_INK}"/><circle cx="126" cy="102" r="7" fill="${FACE_INK}"/><circle cx="77" cy="98" r="2.6" fill="#fff"/><circle cx="129" cy="98" r="2.6" fill="#fff"/>
    <ellipse cx="92" cy="160" rx="3.5" ry="2.5" fill="#1F4F8E"/><ellipse cx="108" cy="160" rx="3.5" ry="2.5" fill="#1F4F8E"/></svg>`,
};
// The face (or a sticker of the picture) for the top of the menu, w wide.
function faceOf(sid, w) {
  if (FACES[sid]) return `<div style="width:${w}px;height:${Math.round(w * 0.9)}px;filter:drop-shadow(0 4px 0 #fff) drop-shadow(0 -4px 0 #fff) drop-shadow(4px 0 0 #fff) drop-shadow(-4px 0 0 #fff) drop-shadow(0 10px 14px rgba(30,40,84,.28))">${FACES[sid].replace("<svg", '<svg style="width:100%;height:100%"')}</div>`;
  return pic(sid, w, "filter:drop-shadow(0 4px 0 #fff) drop-shadow(0 -4px 0 #fff) drop-shadow(4px 0 0 #fff) drop-shadow(-4px 0 0 #fff) drop-shadow(0 10px 14px rgba(30,40,84,.28))");
}
// The menu's five pictures, white, each with its tile's colour cut through it.
const PICTO = {
  dex: c => `<svg viewBox="0 0 64 64"><rect x="14" y="7" width="36" height="50" rx="9" fill="#fff"/><rect x="20" y="14" width="24" height="17" rx="4" fill="${c}"/><circle cx="25.5" cy="43" r="4.5" fill="${c}"/><rect x="33" y="41" width="11" height="4.5" rx="2.2" fill="${c}"/></svg>`,
  ball: c => `<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="23" fill="#fff"/><path d="M9 32h46" stroke="${c}" stroke-width="6"/><circle cx="32" cy="32" r="8.5" fill="#fff" stroke="${c}" stroke-width="5.5"/></svg>`,
  bag: c => `<svg viewBox="0 0 64 64"><path d="M23 19v-4a9 9 0 0 1 18 0v4" stroke="#fff" stroke-width="5.5" fill="none"/><rect x="13" y="18" width="38" height="39" rx="11" fill="#fff"/><rect x="19" y="34" width="26" height="13" rx="4.5" fill="${c}"/><rect x="29.5" y="31" width="5" height="8" rx="2" fill="#fff"/></svg>`,
  map: c => `<svg viewBox="0 0 64 64"><path d="M7 15l17-7 16 7 17-7v42l-17 7-16-7-17 7z" fill="#fff"/><path d="M24 8v42M40 15v42" stroke="${c}" stroke-width="2.5" opacity=".45"/><path d="M44 17c-5.5 0-9 4-9 8.5 0 6.5 9 14 9 14s9-7.5 9-14c0-4.5-3.5-8.5-9-8.5z" fill="${c}"/><circle cx="44" cy="25.5" r="3.2" fill="#fff"/></svg>`,
  save: c => `<svg viewBox="0 0 64 64"><rect x="11" y="9" width="42" height="46" rx="9" fill="#fff"/><rect x="20" y="9" width="23" height="15" rx="2.5" fill="${c}"/><rect x="34" y="12" width="5.5" height="9" rx="1.6" fill="#fff"/><rect x="18" y="32" width="28" height="15" rx="4" fill="${c}" opacity=".35"/></svg>`,
};

// ---------- the partner's emblem for the top of the menu, as Let's Go's Pikachu is drawn there: just the shape of its
// head, in one colour (a soft watercolour wash of its own colour, darker round the edge), no eyes or features. ----------
const EMBLEM_SHAPES = {
  pikachu: [["#FFE98A", "#F6B42A", "#D08410"], `<path d="M44 86C18 52 22 14 50 8c26-4 40 30 36 66z"/><path d="M156 86c26-34 22-72-6-78-26-4-40 30-36 66z"/><ellipse cx="100" cy="114" rx="76" ry="60"/>`],
  charmander: [["#FFC27A", "#F2803A", "#C2461E"], `<path d="M100 8c-20 26-24 44-12 60 2-10 6-16 12-20 6 6 10 12 10 22 18-14 14-38-10-62z"/><path d="M72 70c-6-16-2-30 10-38-2 14 2 24 10 32zM128 70c6-16 2-30-10-38 2 14-2 24-10 32z"/><ellipse cx="30" cy="104" rx="30" ry="13" transform="rotate(-14 30 104)"/><ellipse cx="170" cy="104" rx="30" ry="13" transform="rotate(14 170 104)"/><ellipse cx="100" cy="114" rx="66" ry="58"/><ellipse cx="100" cy="146" rx="30" ry="22"/>`],
  bulbasaur: [["#C4F08E", "#6CC55A", "#2F8A3A"], `<path d="M100 52c-4-24 8-40 34-44-2 26-14 40-34 44z"/><path d="M100 54c-2-18-14-28-34-30 2 18 14 28 34 30z"/><circle cx="40" cy="80" r="16"/><circle cx="160" cy="80" r="16"/><ellipse cx="100" cy="112" rx="70" ry="60"/>`],
  squirtle: [["#B8E4FF", "#5AA6EE", "#2A62B8"], `<path d="M70 60c4-30 34-48 62-36-18 2-26 12-24 24 10-8 22-6 28 4-20-4-34 4-40 18z"/><circle cx="44" cy="74" r="16"/><circle cx="156" cy="74" r="16"/><ellipse cx="100" cy="106" rx="70" ry="56"/><path d="M70 130q30-10 60 0v18q-4 20-30 22-26-2-30-22z"/>`],
};
let EMB_N = 0;
function emblemOf(sid, w) {
  const e = EMBLEM_SHAPES[sid], u = "em" + (EMB_N++);
  if (e) {
    const [[lt, mid, dk], shapes] = e;
    return `<svg viewBox="-10 -6 220 196" style="width:${w}px;height:${Math.round(w * 0.9)}px;overflow:visible;filter:drop-shadow(0 10px 14px rgba(30,40,84,.25))"><defs>
      <radialGradient id="${u}g" cx=".38" cy=".3" r=".85"><stop offset="0" stop-color="${lt}"/><stop offset=".55" stop-color="${mid}"/><stop offset="1" stop-color="${dk}"/></radialGradient>
      <radialGradient id="${u}w" cx=".3" cy=".25" r=".6"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
      <filter id="${u}t" x="-10%" y="-10%" width="120%" height="120%"><feTurbulence type="fractalNoise" baseFrequency=".035" numOctaves="3" seed="7"/><feColorMatrix values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 0 0 .22 0"/><feComposite in2="SourceGraphic" operator="in"/></filter>
      <clipPath id="${u}c">${shapes}</clipPath></defs>
      <g fill="#fff" stroke="#fff" stroke-width="20" stroke-linejoin="round">${shapes}</g>
      <g fill="${dk}" stroke="${dk}" stroke-width="9" stroke-linejoin="round">${shapes}</g>
      <g fill="url(#${u}g)">${shapes}</g>
      <g clip-path="url(#${u}c)"><rect x="-10" y="-6" width="220" height="196" fill="url(#${u}w)"/><rect x="-10" y="-6" width="220" height="196" fill="${lt}" filter="url(#${u}t)"/></g></svg>`;
  }
  // Anyone else: the shape of their picture, washed in their type's colour.
  const col = typeof typeHex === "function" ? typeHex(sp(sid).stats.types[0]) : "#F6B42A", id = "emi" + (EMB_N++);
  portrait(sid).then(url => { const el = document.getElementById(id); if (el) { el.style.webkitMaskImage = el.style.maskImage = `url(${url})`; } });
  return `<div style="width:${w}px;height:${w}px;filter:drop-shadow(0 4px 0 #fff) drop-shadow(0 -4px 0 #fff) drop-shadow(4px 0 0 #fff) drop-shadow(-4px 0 0 #fff) drop-shadow(0 10px 14px rgba(30,40,84,.25))"><div id="${id}" style="width:100%;height:100%;background:radial-gradient(circle at 38% 30%, ${lighterHex(col, 0.5)}, ${col} 55%, ${darkerHex(col, 0.3)});-webkit-mask-size:contain;mask-size:contain;-webkit-mask-repeat:no-repeat;mask-repeat:no-repeat;-webkit-mask-position:center;mask-position:center"></div></div>`;
}
