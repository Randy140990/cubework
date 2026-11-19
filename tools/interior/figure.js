// ---------- people: rounded blocks rather than square ones, at a player's proportions (32 pixels to 1.8 blocks) ----------
const PXK = 1.8 / 32;
function roundedBox(w, h, d, r) {
  const sx = Math.max(3, Math.round(w / (r * 0.5))), sy = Math.max(3, Math.round(h / (r * 0.5))), sz = Math.max(3, Math.round(d / (r * 0.5)));
  const g = new THREE.BoxGeometry(w, h, d, sx, sy, sz), p = g.attributes.position, nrm = g.attributes.normal;
  const hw = w / 2 - r, hh = h / 2 - r, hd = d / 2 - r, v = new THREE.Vector3(), c = new THREE.Vector3(), n = new THREE.Vector3();
  for (let i = 0; i < p.count; i++) {
    v.fromBufferAttribute(p, i);
    c.set(Math.max(-hw, Math.min(hw, v.x)), Math.max(-hh, Math.min(hh, v.y)), Math.max(-hd, Math.min(hd, v.z)));
    n.subVectors(v, c); if (n.lengthSq() < 1e-12) n.fromBufferAttribute(nrm, i); n.normalize();
    v.copy(c).addScaledVector(n, r); p.setXYZ(i, v.x, v.y, v.z); nrm.setXYZ(i, n.x, n.y, n.z);
  }
  return g;
}
const ghostMat = new THREE.MeshLambertMaterial({ color: 0xA6EBCF, emissive: 0x12382E, transparent: true, opacity: 0.62, depthWrite: false });
const ghostEye = new THREE.MeshBasicMaterial({ color: 0x163A33, transparent: true, opacity: 0.85 });
const lamCache = {};
function lam(c) { return lamCache[c] || (lamCache[c] = new THREE.MeshLambertMaterial({ color: new THREE.Color(c) })); }
function darkerHex(c, k) { const v = mixc(hx(c), [0, 0, 0], k); return "#" + v.map(x => Math.round(x).toString(16).padStart(2, "0")).join(""); }
const LOOK_DEFAULT = { body: "boy", skin: "#E9B48C", hair: "#4A3020", hairStyle: "short", eyes: "#3B2A1C", brows: "fine", cap: null,
  top: "#D9524E", legs: "#3F5F9E", shoes: "#2B2522", skirt: false, bag: null, coat: null };
// look: null for the see-through ghost, or { body, skin, hair, hairStyle, eyes, brows, cap, top, legs, shoes, skirt, bag, coat }.
function makeFigure(look) {
  const ghost = !look, k = PXK;
  const L = ghost ? null : Object.assign({}, LOOK_DEFAULT, look.shirt ? { top: look.shirt } : {}, look);
  const girl = L && L.body === "girl";
  const mat = c => ghost ? ghostMat : lam(c);
  const part = (w, h, d, r, m, x, y, z, parent, rot) => {
    const b = new THREE.Mesh(roundedBox(w * k, h * k, d * k, Math.min(r, w / 2 - 0.01, h / 2 - 0.01, d / 2 - 0.01) * k), m);
    b.position.set(x * k, y * k, z * k); if (rot) b.rotation.set(rot[0] || 0, rot[1] || 0, rot[2] || 0);
    b.castShadow = !ghost; parent.add(b); return b;
  };
  const root = new THREE.Group(), hips = new THREE.Group(); hips.position.y = 12 * k; root.add(hips);
  const tw = girl ? 7 : 8, aw = girl ? 3 : 4, ax = girl ? 5 : 6;
  part(tw, 12, 4, 1.2, mat(L ? L.top : 0), 0, 6, 0, hips);
  if (L && L.coat) { part(tw + 0.8, 11, 4.8, 1.2, lam(L.coat), 0, 6.2, 0.15, hips); part(tw - 3.2, 9, 0.4, 0.15, lam(L.top), 0, 7.4, -2.45, hips); }
  if (L && L.bag) { part(6, 7, 2.6, 1.0, lam(L.bag), 0, 6.5, 3.2, hips); part(1, 9, 0.6, 0.3, lam(darkerHex(L.bag, 0.3)), -2.2, 6.5, -2.2, hips); part(1, 9, 0.6, 0.3, lam(darkerHex(L.bag, 0.3)), 2.2, 6.5, -2.2, hips); }
  if (L && L.skirt) part(tw + 1.4, 4.6, 5.4, 1.4, lam(L.legs), 0, -1.6, 0, hips);
  const head = new THREE.Group(); head.position.y = 12 * k; hips.add(head);
  part(8, 8, 8, 1.6, mat(L ? L.skin : 0), 0, 4, 0, head);
  if (ghost) { part(1.2, 1.6, 0.5, 0.25, ghostEye, -1.7, 3.6, -4.05, head); part(1.2, 1.6, 0.5, 0.25, ghostEye, 1.7, 3.6, -4.05, head); }
  else {
    // Eyes: white, the iris in its own colour, a glint. A mouth, and brows in the hair's colour, a shade darker.
    for (const s of [-1, 1]) {
      part(1.5, 1.9, 0.5, 0.3, lam("#FFFFFF"), 1.8 * s, 3.4, -4.02, head);
      part(1.0, 1.4, 0.5, 0.28, lam(L.eyes), 1.65 * s, 3.3, -4.12, head);
      const g = part(0.45, 0.45, 0.3, 0.1, new THREE.MeshBasicMaterial({ color: 0xffffff }), 1.45 * s + 0.15, 3.75, -4.3, head);
      g.castShadow = false;
    }
    part(1.8, 0.45, 0.4, 0.15, lam(darkerHex(L.skin, 0.32)), 0, 1.6, -4.02, head);
    const bc = lam(darkerHex(L.hair, 0.3)), br = L.brows;
    for (const s of [-1, 1]) {
      if (br === "fine") part(1.8, 0.38, 0.4, 0.15, bc, 1.75 * s, 5.0, -4.05, head);
      else if (br === "thick") part(2.1, 0.75, 0.5, 0.25, bc, 1.75 * s, 5.05, -4.05, head);
      else if (br === "arched") part(1.9, 0.45, 0.45, 0.18, bc, 1.75 * s, 5.05, -4.05, head, [0, 0, -0.22 * s]);
      else if (br === "straight") part(2.2, 0.5, 0.45, 0.18, bc, 1.7 * s, 4.95, -4.05, head);
      else if (br === "bushy") part(2.3, 0.95, 0.6, 0.35, bc, 1.75 * s, 5.05, -4.06, head);
    }
    const hm = lam(L.hair), st = L.hairStyle;
    if (st === "buzz") { part(8.4, 1.4, 8.4, 0.6, hm, 0, 7.6, 0, head); part(8.4, 4.5, 1.0, 0.4, hm, 0, 5.6, 3.8, head); }
    else {
      if (!L.cap) part(8.8, 3.2, 8.8, 1.5, hm, 0, 7.2, 0.1, head);
      part(8.8, 6, 2.2, 1.0, hm, 0, 4.5, 3.5, head);
      for (const s of [-1, 1]) part(1.0, 3.2, 6, 0.45, hm, 4.3 * s, 5.4, 0.9, head);
      if (st === "swept") { part(6.2, 2.4, 1.3, 0.6, hm, -1.3, 6.0, -4.15, head, [0, 0, 0.28]); part(3, 1.6, 1.2, 0.5, hm, 2.6, 6.6, -4.1, head); }
      else if (!L.cap || st === "long") part(8.8, 2.0, 1.2, 0.5, hm, 0, 6.2, -4.1, head);
      if (st === "spiky" && !L.cap) for (const [x, z, rz, rx] of [[-2.5, -1.5, 0.4, -0.3], [0, -2, 0, -0.4], [2.5, -1.5, -0.4, -0.3], [-1.5, 1.5, 0.3, 0.3], [1.5, 1.5, -0.3, 0.3]]) part(2.2, 2.8, 2.2, 0.6, hm, x, 9.0, z, head, [rx, 0, rz]);
      if (st === "long") { part(8.8, 9.5, 2.4, 1.0, hm, 0, 2.2, 3.6, head); for (const s of [-1, 1]) part(1.3, 7.5, 2.6, 0.6, hm, 4.35 * s, 2.6, -1.2, head); }
      if (st === "bob") { part(8.8, 6.6, 2.6, 1.0, hm, 0, 3.4, 3.6, head); for (const s of [-1, 1]) part(1.5, 6.2, 7.2, 0.7, hm, 4.4 * s, 3.5, 0.2, head); }
      if (st === "pony") { part(1.8, 1.8, 1.8, 0.6, lam(L.top), 0, 5.6, 4.9, head); part(2.8, 7.5, 2.8, 1.2, hm, 0, 1.7, 5.6, head, [0.22, 0, 0]); }
      if (st === "bun") { part(4.2, 4.2, 4.2, 1.8, hm, 0, 8.9, 2.4, head); part(4.6, 0.8, 4.6, 0.35, lam(L.top), 0, 7.4, 2.4, head); }
    }
    if (L.cap) {
      const cm = lam(L.cap);
      part(9.0, 3.6, 9.0, 1.8, cm, 0, 7.6, 0.15, head); part(8.4, 0.7, 4.4, 0.35, cm, 0, 6.3, -5.7, head);
      part(2.4, 2.4, 0.35, 0.5, lam("#FFFFFF"), 0, 7.7, -4.6, head); part(1.2, 1.2, 0.4, 0.3, lam(darkerHex(L.cap, 0.2)), 0, 7.7, -4.72, head);
    }
  }
  const limb = (x, y, parent, upper, lower, uMat, lMat, w, foot) => {
    const j = new THREE.Group(); j.position.set(x * k, y * k, 0); parent.add(j);
    part(w, upper, 4, 1.0, uMat, 0, -upper / 2, 0, j);
    const knee = new THREE.Group(); knee.position.y = -upper * k; j.add(knee);
    if (foot) { part(w, lower - 2, 4, 1.0, lMat, 0, -(lower - 2) / 2, 0, knee); part(w + 0.5, 2.3, 5.2, 0.9, foot, 0, -lower + 1.15, -0.5, knee); }
    else part(w, lower, 4, 1.0, lMat, 0, -lower / 2, 0, knee);
    return { j, knee };
  };
  const top = mat(L ? L.top : 0), skin = mat(L ? L.skin : 0), sleeve = L && L.coat ? lam(L.coat) : top;
  const armL = limb(-ax, 11.5, hips, 6, 6, sleeve, L && L.coat ? sleeve : skin, aw), armR = limb(ax, 11.5, hips, 6, 6, sleeve, L && L.coat ? sleeve : skin, aw);
  const legM = L && L.skirt ? skin : mat(L ? L.legs : 0), shoe = ghost ? null : lam(L.shoes);
  const legL = limb(-2, 0, hips, 6, 6, legM, legM, 4, shoe), legR = limb(2, 0, hips, 6, 6, legM, legM, 4, shoe);
  const shadow = new THREE.Mesh(new THREE.PlaneGeometry(0.9, 0.9), matFor("shadowBlob")); shadow.rotation.x = -Math.PI / 2; shadow.position.y = 0.025; shadow.renderOrder = 3; root.add(shadow);
  return { root, hips, head, armL, armR, legL, legR, shadow, phase: 0 };
}
function poseFigure(f, o) {
  // o: { walk (0..1.3), seated, headYaw, talk, wave (0..1), point (0..1), cheer (0..1) }
  const s = Math.sin(f.phase), a = 0.62 * (o.walk || 0);
  if (o.seated) {
    f.hips.position.y = 0.56; f.hips.position.z = 0.08;
    f.legL.j.rotation.x = f.legR.j.rotation.x = Math.PI / 2; f.legL.knee.rotation.x = f.legR.knee.rotation.x = -Math.PI / 2 + 0.15;
    f.armL.j.rotation.x = f.armR.j.rotation.x = 0.55; f.armL.knee.rotation.x = f.armR.knee.rotation.x = 0.35;
    f.shadow.visible = false;
  } else {
    f.hips.position.y = 12 * PXK + Math.abs(Math.cos(f.phase)) * 0.035 * Math.min(1, o.walk || 0); f.hips.position.z = 0;
    f.legL.j.rotation.x = s * a; f.legR.j.rotation.x = -s * a;
    f.legL.knee.rotation.x = -Math.max(0, -Math.sin(f.phase + 0.9)) * a * 1.3; f.legR.knee.rotation.x = -Math.max(0, Math.sin(f.phase + 0.9)) * a * 1.3;
    f.armL.j.rotation.x = -s * a * 0.8; f.armR.j.rotation.x = s * a * 0.8;
    f.armL.knee.rotation.x = f.armR.knee.rotation.x = 0.25 + a * 0.3;
    f.armL.j.rotation.z = -0.06; f.armR.j.rotation.z = 0.06;
    f.shadow.visible = true;
  }
  const t = performance.now() / 1000;
  if (o.wave) { f.armR.j.rotation.z = 0.06 + 2.5 * o.wave; f.armR.j.rotation.x = 0; f.armR.knee.rotation.z = 0; f.armR.knee.rotation.x = 0.2 + Math.sin(t * 9) * 0.5 * o.wave; }
  if (o.point) { f.armR.j.rotation.x = 1.45 * o.point; f.armR.knee.rotation.x = 0.1; }
  if (o.cheer) { f.armL.j.rotation.z = -2.6 * o.cheer; f.armR.j.rotation.z = 2.6 * o.cheer; f.armL.j.rotation.x = f.armR.j.rotation.x = 0; f.hips.position.y += Math.abs(Math.sin(t * 7)) * 0.12 * o.cheer; }
  f.head.rotation.y = o.headYaw || 0;
  f.head.rotation.x = o.talk ? Math.sin(performance.now() / 120) * 0.05 : (o.nod || 0);
}
