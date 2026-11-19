// ---------- creatures: fetched from mons/<sid>.json when first wanted, and measured as they stand ----------
function loadSpecies(sid) {
  const d = DATA.species[sid];
  if (d.ours.geo) return Promise.resolve();
  if (!d._loading) d._loading = fetch(`mons/${sid}.json`).then(r => { if (!r.ok) throw new Error(String(r.status)); return r.json(); })
    .then(j => { Object.assign(d.ours, j.ours); Object.assign(d.theirs, j.theirs); })
    .catch(e => { d._loading = null; throw e; });
  return d._loading;
}
function sizeOf(model) {
  setParts(model, null);
  pose(model, [[groundIdle(model), 0]]);
  model.holder.position.set(0, 0, 0); model.holder.rotation.set(0, 0, 0);
  model.holder.updateMatrixWorld(true);
  const box = new THREE.Box3(), tmp = new THREE.Box3(), ws = new THREE.Vector3(), hs = (model.info.scale || 1) / 16;
  const shown = o => { for (let x = o; x; x = x.parent) if (!x.visible) return false; return true; };
  model.holder.traverse(o => {
    if (!o.isMesh || !shown(o)) return;
    o.getWorldScale(ws); if (Math.min(Math.abs(ws.x), Math.abs(ws.y), Math.abs(ws.z)) < hs * 0.02) return;
    o.geometry.computeBoundingBox(); tmp.copy(o.geometry.boundingBox).applyMatrix4(o.matrixWorld); box.union(tmp);
  });
  const s = new THREE.Vector3(); box.getSize(s);
  return { h: box.max.y, w: s.x, l: s.z, cx: (box.min.x + box.max.x) / 2, cz: (box.min.z + box.max.z) / 2, bottom: box.min.y };
}
function groundIdle(model) { const i = model.info; return model.anims[i.ground] || model.anims[i.idle]; }
function moveAnim(model) { const p = model.info.poses || {}; for (const k of ["walk", "fly", "hover"]) if (p[k] && model.anims[p[k].anim]) return { an: model.anims[p[k].anim], key: k }; return null; }

