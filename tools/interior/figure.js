// ---------- people: Minecraft's player model wearing the mod's own skins, its corners only just softened ----------
// The skins are the mod's (textures/entity/npc, textures/entity/player, and your clothes made from textures/outfit
// as Outfit.compose makes them); the cap and the bag are the mod's GearLayer models. 32 pixels to 1.8 blocks.
const PXK = 1.8 / 32;
const SKIN_IMG = {};
const skinsReady = (typeof SKINDATA === "undefined" ? Promise.resolve() : Promise.all(Object.entries(SKINDATA).flatMap(([grp, m]) => Object.entries(m).map(([n, uri]) => new Promise(res => {
  const im = new Image(); im.onload = () => { SKIN_IMG[grp + "/" + n] = im; res(); }; im.onerror = () => res(); im.src = uri;
})))));
const OUTFIT = {
  SKINS: ["FAIR", "LIGHT", "TAN", "DEEP"], SKIN_RGB: [0xF2CBAE, 0xE6B48E, 0xC08A60, 0x86563A],
  // Proposed: four more skin tones in between and either side, for everyone to see themselves.
  SKINS_NEW: ["PALE", "MEDIUM", "BROWN", "DARK", "IVORY", "PEACH", "OLIVE", "ESPRESSO"], SKIN_RGB_NEW: [0xF8DCC8, 0xD69E72, 0xA2683F, 0x5C3824, 0xFCE7D9, 0xEDBF9C, 0xB58A5F, 0x4A2C1C],
  HAIRS: ["short", "spiky", "long", "ponytail", "pigtails", "bun"], HATS: ["none", "cap", "beanie", "sunhat"], TOPS: ["jacket", "tee", "hoodie", "vest"],
  BOTTOMS: ["jeans", "shorts", "skirt"], CAPS: ["pokeball", "greatball", "ultraball", "plain"], CAP_NAMES: ["POKé BALL", "GREAT BALL", "ULTRA BALL", "PLAIN"],
  BAGS: ["backpack", "satchel", "none"],
  // The first 14 for clothes; hair has its own natural shades as well, at the end.
  COLOUR_NAMES: ["RED", "BLUE", "GREEN", "YELLOW", "BLACK", "WHITE", "PINK", "ORANGE", "PURPLE", "BROWN", "TEAL", "GREY", "NAVY", "CREAM",
    "BLONDE", "AUBURN", "SILVER", "CHESTNUT"],
  COLOURS: [0xD8343C, 0x3A6FD0, 0x3FA34D, 0xF2C12E, 0x2A2A30, 0xF2F2F2, 0xF08FB6, 0xF07D2A, 0x8A52C8, 0x7A5030, 0x2AA5A0, 0x8C8F99, 0x283A6E, 0xF0E2BE,
    0xE8CF8A, 0xA8542A, 0xC9CED6, 0x5A3420], CLOTHES: 14,
  EYES: ["NAVY", "BROWN", "HAZEL", "GREEN", "BLUE", "GREY", "AMBER", "VIOLET"], EYE_RGB: [0x2D3250, 0x5A3A22, 0x7A6A34, 0x3A7A44, 0x3A6FD0, 0x6E7A88, 0xB87A22, 0x6A4AA8],
  defaults(girl) {
    return { girl: !!girl, skin: 0, hair: girl ? 4 : 0, hairc: girl ? 9 : 4, hat: girl ? 3 : 1, hatc: girl ? 5 : 0, top: girl ? 1 : 0, topc: girl ? 10 : 0,
      bot: girl ? 2 : 0, botc: girl ? 0 : 12, shoec: girl ? 6 : 0, capp: 0, capb: 0, bag: girl ? 1 : 0, bagc: 3, eyes: 0 };
  },
};
function skinRGB(i) { const all = OUTFIT.SKIN_RGB.concat(OUTFIT.SKIN_RGB_NEW); return all[((i % all.length) + all.length) % all.length]; }
const colourOf = i => OUTFIT.COLOURS[((i % OUTFIT.COLOURS.length) + OUTFIT.COLOURS.length) % OUTFIT.COLOURS.length];
// One layer stacked on, tinted (-1 for its own colours): as Outfit.layer does it, pixel for pixel.
function stack(out, img, rgb) {
  if (!img) return;
  const c = document.createElement("canvas"); c.width = img.width; c.height = img.height; const x = c.getContext("2d"); x.drawImage(img, 0, 0);
  const src = x.getImageData(0, 0, c.width, c.height).data, dst = out.data, W = out.width;
  const tr = rgb < 0 ? 255 : (rgb >> 16) & 255, tg = rgb < 0 ? 255 : (rgb >> 8) & 255, tb = rgb < 0 ? 255 : rgb & 255;
  for (let y = 0; y < Math.min(out.height, c.height); y++) for (let xx = 0; xx < Math.min(W, c.width); xx++) {
    const i = (y * c.width + xx) * 4, o = (y * W + xx) * 4, a = src[i + 3];
    if (!a) continue;
    const r = src[i] * tr / 255, g = src[i + 1] * tg / 255, b = src[i + 2] * tb / 255;
    if (a >= 250) { dst[o] = r; dst[o + 1] = g; dst[o + 2] = b; dst[o + 3] = 255; continue; }
    const oa = dst[o + 3], na = a + oa * (255 - a) / 255;
    dst[o] = (r * a + dst[o] * (255 - a)) / 255; dst[o + 1] = (g * a + dst[o + 1] * (255 - a)) / 255; dst[o + 2] = (b * a + dst[o + 2] * (255 - a)) / 255; dst[o + 3] = na;
  }
}
// Your skin from your clothes, as Outfit.compose makes it; your eyes recoloured where the face layer has its irises.
function composeOutfit(o) {
  const cv = document.createElement("canvas"); cv.width = 64; cv.height = 64; const ctx = cv.getContext("2d"), out = ctx.getImageData(0, 0, 64, 64);
  const L = n => SKIN_IMG["outfit/" + n];
  stack(out, L("body"), skinRGB(o.skin)); stack(out, L("face"), -1);
  const eye = OUTFIT.EYE_RGB[((o.eyes || 0) % 8 + 8) % 8], hairc = colourOf(o.hairc);
  for (let i = 0; i < out.data.length; i += 4) {
    const r = out.data[i], g = out.data[i + 1], b = out.data[i + 2];
    if (r === 45 && g === 50 && b === 80) { out.data[i] = eye >> 16 & 255; out.data[i + 1] = eye >> 8 & 255; out.data[i + 2] = eye & 255; }
    else if (r === 70 && g === 50 && b === 40) { out.data[i] = (hairc >> 16 & 255) * 0.62; out.data[i + 1] = (hairc >> 8 & 255) * 0.62; out.data[i + 2] = (hairc & 255) * 0.62; }
  }
  stack(out, L("bottom_" + OUTFIT.BOTTOMS[o.bot % 3]), colourOf(o.botc)); stack(out, L("shoes"), colourOf(o.shoec));
  const top = OUTFIT.TOPS[o.top % 4]; stack(out, L("top_" + top), colourOf(o.topc)); if (top === "jacket") stack(out, L("top_jacket_trim"), -1);
  stack(out, L("hair_" + OUTFIT.HAIRS[o.hair % 6]), colourOf(o.hairc));
  const hat = OUTFIT.HATS[o.hat % 4]; if (hat !== "none" && hat !== "cap") stack(out, L("hat_" + hat), colourOf(o.hatc));
  ctx.putImageData(out, 0, 0); return cv;
}
function gearCanvas(base, detail, rgb) {
  const cv = document.createElement("canvas"); cv.width = 64; cv.height = 32; const ctx = cv.getContext("2d"), out = ctx.getImageData(0, 0, 64, 32);
  stack(out, SKIN_IMG["gear/" + base], rgb); if (detail) stack(out, SKIN_IMG["gear/" + detail], -1); ctx.putImageData(out, 0, 0); return cv;
}
function texOf(canvas) { const t = new THREE.CanvasTexture(canvas); t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false; return t; }
function canvasOfImg(img) { const c = document.createElement("canvas"); c.width = img.width; c.height = img.height; c.getContext("2d").drawImage(img, 0, 0); return c; }

// A box part in the skin's layout (w, h, d pixels; uv at its corner in a texW by texH picture), its edges rounded by r.
const lamCache = {};
function lam(c) { return lamCache[c] || (lamCache[c] = new THREE.MeshLambertMaterial({ color: new THREE.Color(c) })); }
function mcBox(w, h, d, uvx, uvy, r, texW = 64, texH = 64, inflate = 0, dims) {
  const [tw, th, td] = dims || [w, h, d];
  const W = w + 2 * inflate, H = h + 2 * inflate, D = d + 2 * inflate, hw = W / 2, hh = H / 2, hd = D / 2;
  r = Math.min(r, hw * 0.45, hh * 0.45, hd * 0.45);
  const g = new THREE.BoxGeometry(W, H, D, 3, 3, 3), P = g.attributes.position, UV = g.attributes.uv, N = g.attributes.normal, idx = g.index;
  const rem = (v, half) => (Math.abs(v) > 1e-6 && Math.abs(Math.abs(v) - half) > 1e-6) ? Math.sign(v) * (half - r) : v;
  for (let i = 0; i < P.count; i++) P.setXYZ(i, rem(P.getX(i), hw), rem(P.getY(i), hh), rem(P.getZ(i), hd));
  const R = { px: [uvx, uvy + td, td, th], nz: [uvx + td, uvy + td, tw, th], nx: [uvx + td + tw, uvy + td, td, th], pz: [uvx + 2 * td + tw, uvy + td, tw, th],
    py: [uvx + td, uvy, tw, td], ny: [uvx + td + tw, uvy, tw, td] };
  const faces = ["px", "nx", "py", "ny", "pz", "nz"];
  g.groups.forEach((grp, fi) => {
    const f = faces[fi], [rx, ry, rw, rh] = R[f];
    for (let k = grp.start; k < grp.start + grp.count; k++) {
      const v = idx.getX(k), x = P.getX(v), y = P.getY(v), z = P.getZ(v);
      let u, vr;
      if (f === "px") { u = (hd - z) / D; vr = (hh - y) / H; } else if (f === "nx") { u = (z + hd) / D; vr = (hh - y) / H; }
      else if (f === "pz") { u = (x + hw) / W; vr = (hh - y) / H; } else if (f === "nz") { u = (hw - x) / W; vr = (hh - y) / H; }
      else if (f === "py") { u = (hw - x) / W; vr = (z + hd) / D; } else { u = (hw - x) / W; vr = (z + hd) / D; }
      u = Math.min(1, Math.max(0, u)); vr = Math.min(1, Math.max(0, vr));
      UV.setXY(v, (rx + u * rw * 0.999 + 0.0005) / texW, 1 - (ry + vr * rh * 0.999 + 0.0005) / texH);
    }
  });
  const v3 = new THREE.Vector3(), c = new THREE.Vector3(), n = new THREE.Vector3(), iw = hw - r, ih = hh - r, id = hd - r;
  for (let i = 0; i < P.count; i++) {
    v3.fromBufferAttribute(P, i); c.set(Math.max(-iw, Math.min(iw, v3.x)), Math.max(-ih, Math.min(ih, v3.y)), Math.max(-id, Math.min(id, v3.z)));
    n.subVectors(v3, c); if (n.lengthSq() < 1e-12) n.fromBufferAttribute(N, i); n.normalize();
    v3.copy(c).addScaledVector(n, r); P.setXYZ(i, v3.x, v3.y, v3.z); N.setXYZ(i, n.x, n.y, n.z);
  }
  g.scale(PXK, PXK, PXK);
  return g;
}
const ghostMat = new THREE.MeshLambertMaterial({ color: 0xA6EBCF, emissive: 0x12382E, transparent: true, opacity: 0.62, depthWrite: false });
const ghostEye = new THREE.MeshBasicMaterial({ color: 0x163A33, transparent: true, opacity: 0.85 });
// spec: null for the see-through ghost; { skin: "oak" } for one of the mod's people; { player: "boy" } for the boy
// or the girl as they come; { outfit: {...} } for you in your clothes (with the cap and bag as the mod adds them).
function makeFigure(spec) {
  const ghost = !spec, k = PXK, R = 0.45;
  let canvas = null, outfit = null;
  if (!ghost) {
    if (spec.outfit) { outfit = spec.outfit; canvas = composeOutfit(outfit); }
    else { const im = SKIN_IMG[(spec.player ? "player/" : "npc/") + (spec.player || spec.skin)] || SKIN_IMG["npc/youngster"]; canvas = im ? canvasOfImg(im) : null; }
  }
  const tex = canvas ? texOf(canvas) : null;
  const base = ghost ? ghostMat : new THREE.MeshLambertMaterial({ map: tex });
  const over = ghost ? null : new THREE.MeshLambertMaterial({ map: tex, transparent: true, alphaTest: 0.1, depthWrite: true, side: THREE.DoubleSide });
  const part = (parent, w, h, d, uvx, uvy, ox, oy, oz, ovx, ovy, inf = 0.5) => {
    const m = new THREE.Mesh(mcBox(w, h, d, uvx, uvy, R), base); m.position.set(ox * k, oy * k, oz * k); m.castShadow = !ghost || (typeof FIT !== "undefined" && FIT); parent.add(m);
    if (over && ovx != null) { const o = new THREE.Mesh(mcBox(w, h, d, ovx, ovy, R, 64, 64, inf), over); o.position.copy(m.position); o.castShadow = true; parent.add(o); }
    return m;
  };
  const root = new THREE.Group(), hips = new THREE.Group(); hips.position.y = 12 * k; root.add(hips);
  part(hips, 8, 12, 4, 16, 16, 0, 6, 0, 16, 32, 0.25);
  const head = new THREE.Group(); head.position.y = 12 * k; hips.add(head);
  part(head, 8, 8, 8, 0, 0, 0, 4, 0, 32, 0, 0.5);
  if (ghost) { const e = (x) => { const m = new THREE.Mesh(new THREE.BoxGeometry(1.2 * k, 1.6 * k, 0.4 * k), ghostEye); m.position.set(x * k, 3.6 * k, -4.05 * k); head.add(m); }; e(-1.7); e(1.7); }
  const limb = (x, y, parent, uvx, uvy, ovx, ovy) => {
    const j = new THREE.Group(); j.position.set(x * k, y * k, 0); parent.add(j);
    const knee = new THREE.Group(); j.add(knee);
    part(knee, 4, 12, 4, uvx, uvy, 0, y === 0 ? -6 : -4, 0, ovx, ovy, 0.25);
    return { j, knee };
  };
  // The character's right is +x here (they face -z): the right arm and leg are the skin's first arm and leg.
  const armR = limb(6, 10, hips, 40, 16, 40, 32), armL = limb(-6, 10, hips, 32, 48, 48, 48);
  const legR = limb(2, 0, hips, 0, 16, 0, 32), legL = limb(-2, 0, hips, 16, 48, 0, 48);
  const F = { root, hips, head, armL, armR, legL, legR, phase: 0 };
  if (outfit) addGear(F, outfit);
  const shadow = new THREE.Mesh(new THREE.PlaneGeometry(0.9, 0.9), matFor("shadowBlob")); shadow.rotation.x = -Math.PI / 2; shadow.position.y = 0.025; shadow.renderOrder = 3; root.add(shadow);
  F.shadow = shadow;
  return F;
}
// The cap and the bag, as GearLayer draws them (Minecraft's y runs down there and its x the other way: turned round here).
function addGear(F, o) {
  const k = PXK, R = 0.3;
  if (OUTFIT.HATS[o.hat % 4] === "cap") {
    const pat = OUTFIT.CAPS[(o.capp || 0) % OUTFIT.CAPS.length], tint = pat === "plain" ? colourOf(o.hatc) : -1;
    const mat = new THREE.MeshLambertMaterial({ map: texOf(gearCanvas("cap_" + pat, null, tint)), alphaTest: 0.1, side: THREE.DoubleSide });
    const cap = new THREE.Group(); F.head.add(cap); if (o.capb === 1) cap.rotation.y = Math.PI;
    const add = (w, h, d, u, v, x, y, z, inf = 0, parent = cap) => { const m = new THREE.Mesh(mcBox(w, h, d, u, v, R, 64, 32, inf), mat); m.position.set(x * k, y * k, z * k); m.castShadow = true; parent.add(m); return m; };
    add(9, 4, 9, 0, 0, 0, 6.5, 0, 0.12); add(7, 1, 7, 36, 0, 0, 8.95, 0); add(1, 1, 1, 28, 14, 0, 9.7, 0, -0.1);
    const brim = new THREE.Group(); brim.position.set(0, 4.9 * k, -4.6 * k); brim.rotation.x = -0.14; cap.add(brim); add(9, 1, 5, 0, 14, 0, 0, -2.5, 0, brim);
  }
  const bag = OUTFIT.BAGS[(o.bag || 0) % 3];
  if (bag !== "none") {
    const mat = new THREE.MeshLambertMaterial({ map: texOf(gearCanvas("bag_base", "bag_detail", colourOf(o.bagc))), alphaTest: 0.1 });
    const B = (w, h, d, u, v, x0, y0, z0, inf = 0) => { const m = new THREE.Mesh(mcBox(w, h, d, u, v, R, 64, 32, inf), mat); m.position.set(-(x0 + w / 2) * k, (12 - (y0 + h / 2)) * k, (z0 + d / 2) * k); m.castShadow = true; F.hips.add(m); return m; };
    if (bag === "backpack") {
      B(7, 9, 4, 0, 0, -3.5, 0.6, 2.2); B(7, 4, 1, 0, 13, -3.5, 0.3, 6.0, 0.08); B(1, 4, 3, 22, 0, 3.5, 5, 2.7); B(1, 4, 3, 22, 0, -4.5, 5, 2.7); B(5, 3, 1, 16, 13, -2.5, 5.6, 6.05);
      B(1, 1, 5, 36, 0, 2.2, -0.45, -2.6); B(1, 1, 5, 36, 0, -3.2, -0.45, -2.6); B(1, 8, 1, 32, 0, 2.2, 0, -2.6); B(1, 8, 1, 32, 0, -3.2, 0, -2.6);
    } else {
      B(5, 5, 2, 0, 20, 0.6, 7.6, 2.15); B(1, 1, 5, 36, 0, -3.7, -0.45, -2.6);
      const s = (len, z, rz) => { const m = new THREE.Mesh(mcBox(1, len, 1, 48, 0, R, 64, 32), mat); const g = new THREE.Group(); g.position.set(3.2 * k, 11.8 * k, z * k); g.rotation.z = -rz; m.position.y = -len / 2 * k; g.add(m); F.hips.add(g); };
      s(14, -2.35, 0.62); s(12, 2.35, 0.5);
    }
  }
}
function poseFigure(f, o) {
  // Minecraft's own swing: arms and legs straight, swinging from the shoulder and hip. o: { walk, seated, headYaw, talk, wave, point, cheer, nod }.
  const s = Math.sin(f.phase), a = 0.75 * Math.min(1.2, o.walk || 0);
  f.armL.j.rotation.set(0, 0, 0); f.armR.j.rotation.set(0, 0, 0); f.legL.j.rotation.set(0, 0, 0); f.legR.j.rotation.set(0, 0, 0);
  if (o.seated) {
    f.hips.position.y = 0.56; f.hips.position.z = 0.04;
    f.legL.j.rotation.x = f.legR.j.rotation.x = Math.PI / 2 - 0.08; f.legL.j.rotation.y = 0.08; f.legR.j.rotation.y = -0.08;
    f.armL.j.rotation.x = f.armR.j.rotation.x = 0.5;
    f.shadow.visible = false;
  } else {
    f.hips.position.y = 12 * PXK; f.hips.position.z = 0;
    f.legL.j.rotation.x = s * a; f.legR.j.rotation.x = -s * a;
    f.armL.j.rotation.x = -s * a * 0.85; f.armR.j.rotation.x = s * a * 0.85;
    const t0 = performance.now() / 1000;
    f.armL.j.rotation.z = -0.04 - Math.sin(t0 * 1.3) * 0.02; f.armR.j.rotation.z = 0.04 + Math.sin(t0 * 1.3) * 0.02;
    f.shadow.visible = true;
  }
  const t = performance.now() / 1000;
  if (o.wave) { f.armR.j.rotation.z = 2.7 * o.wave + Math.sin(t * 9) * 0.25 * o.wave; f.armR.j.rotation.x = 0; }
  if (o.point) { f.armR.j.rotation.x = 1.5 * o.point; f.armR.j.rotation.z = 0.1; }
  if (o.cheer) { f.armL.j.rotation.z = -2.7 * o.cheer; f.armR.j.rotation.z = 2.7 * o.cheer; f.armL.j.rotation.x = f.armR.j.rotation.x = 0; f.hips.position.y += Math.abs(Math.sin(t * 7)) * 0.12 * o.cheer; }
  // The floss: both arms swung straight to one side and the other, one in front and one behind (swapping each
  // swing), the hips swaying the other way.
  if (o.floss) {
    // Both arms straight and out to one side together; as they swing across they swap, so the arm on the far side is
    // always the one in front of the body and the other behind. The hips swing the other way, twice as quick.
    const k = o.floss, w = t * 6.4, p = Math.sin(w), hp = Math.sin(w * 2);
    f.armL.j.rotation.z = f.armR.j.rotation.z = 0.55 * p * k;
    f.armR.j.rotation.x = -0.55 * p * k; f.armL.j.rotation.x = 0.55 * p * k;
    f.hips.position.x = -0.06 * hp * k; f.hips.rotation.z = 0.08 * hp * k;
  } else { f.hips.position.x = 0; f.hips.rotation.z = 0; }
  f.head.rotation.y = o.headYaw || 0;
  f.head.rotation.x = o.talk ? Math.sin(performance.now() / 120) * 0.05 : (o.nod || 0) - (o.lookUp || 0);
}
