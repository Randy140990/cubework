R = "/home/claude/route-one/tools/interior/"


def sub(path, old, new):
    s = open(path).read()
    assert s.count(old) == 1, (path, old[:70])
    open(path, "w").write(s.replace(old, new))


# matFor: a material whose picture is an image (the game's own, as a PNG).
sub(R + "world.js", '''function matFor(key) {
  if (matCache[key]) return matCache[key];
  const d = MATDEF[key];
  if (!d) throw new Error("no material " + key);
  const map = d.dyn ? d.dyn.tex : pixTex(d.tex);''', '''// (0.67.1) A picture given as an image (the game's own PNG: the ships' looks), laid as the drawn ones are.
const imgCache = {};
function imgTex(src) {
  if (imgCache[src]) return imgCache[src];
  const im = new Image(), t = new THREE.Texture(im);
  im.onload = () => { t.needsUpdate = true; };
  im.src = src;
  t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false;
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  return (imgCache[src] = t);
}
function matFor(key) {
  if (matCache[key]) return matCache[key];
  const d = MATDEF[key];
  if (!d) throw new Error("no material " + key);
  const map = d.dyn ? d.dyn.tex : d.img ? imgTex(d.img) : pixTex(d.tex);''')

s = open(R + "rooms70.js").read()
s = s.replace('''// ---------- (0.67, John's 16:28) The S.S. ANNE from outside, in our looks: for review only ----------''',
              '''// ---------- (0.67, John's 16:28; 0.67.1, 17:13) The S.S. ANNE and the S.S. AQUA, in our looks, as the game has them ----------''')
s = s.replace('''// look at first. Drag to turn round it''', '''// look at first. (0.67.1, 17:13: "both ships obviously get the retouch") Both ships now, retextured in the game too,
// and drawn here in the game's very pictures (tools/shiplooks.py made them: ship_data.js carries them). Drag to turn round it''')
s = s.replace('''  if (typeof SHIP === "undefined") return;
  const D = SHIP;
  PLACES.push({ id: "ss_anne_outside", name: "S.S. ANNE (outside)", floor: "In our looks, for review: the game's ship unchanged", group: "Ships (for review)" });
''', '''  if (typeof SHIPS === "undefined") return;
  for (const [sid, D] of Object.entries(SHIPS.ships))
    PLACES.push({ id: "ship_" + sid, name: D.name + " (outside)", floor: "In our looks, as the game has it (" + D.moored + ")", group: "Ships" });
  // The game's pictures for the ships (tools/shiplooks.py), each a material laid as the game lays it.
  for (const [k, p] of Object.entries(SHIPS.pics)) {
    MATDEF["shp_" + k] = { img: "data:image/png;base64," + p.png, per: p.per, ...(p.cut ? { cut: true } : p.alpha ? { alpha: true } : {}) };
    TEXDEF["shp_" + k] = [16, 16, (P, r) => P.fill([200, 200, 200], 0, r)];
  }
''')
old = s[s.index('  const KEY = {};'):s.index('  reg("shipSea"')]
new = '''  // Anything the game leaves as it is (signs, banners, beds, heads, leaves, webs, fans): its own colour, as before.
  const KEY = {};
  for (const D of Object.values(SHIPS.ships)) for (const [b, kd] of Object.entries(D.kinds)) {
    if (KEY[b]) continue;
    const k = "ship_" + b, c = clean(kd.rgb);
    (STYLE[kd.fam] || STYLE.paint)(k, c, b.startsWith("black_") || b.startsWith("gray_") || b === "glass" || b === "tinted_glass");
    KEY[b] = k;
  }
'''
s = s.replace(old, new)
old = s[s.index('  // ===================================== the ship ====================================='):s.index('})();')]
new = '''  // ===================================== the ships =====================================
  for (const [sid, D] of Object.entries(SHIPS.ships)) BUILD["ship_" + sid] = () => {
    const R = new Room("ship_" + sid, { name: D.name + " (outside)", floor: "In our looks, as the game has it (" + D.moored + ")", W: D.W, D: D.D, H: D.H, outside: true });
    const big = Math.max(D.W, D.D);
    R.orbit = { x: D.W / 2, y: 18, z: D.D / 2, yaw: D.W >= D.D ? 0.55 : 0.55 + Math.PI / 2, pitch: 0.36, dist: big * 0.92 };
    R.spawn = { x: D.W / 2, z: D.D / 2, yaw: 0 };
    R.outsideSize = `${D.W} by ${D.D}, ${D.H} high (${D.count} blocks with a face showing; the rest are inside its solid walls)`;
    // Each block: the game's pictures for its top, bottom and sides (tools/shiplooks.py), or its own colour where the
    // game leaves it as it is.
    const specOf = b => { const f = D.faces[b]; if (f && f.side) return { top: "shp_" + (f.top || f.side), bottom: "shp_" + (f.bottom || f.side), side: "shp_" + f.side }; return null; };
    const G = R.geo, bytes = atob(D.cells), shapes = D.palette.map(shape);
    const looks = D.palette.map(s => { const b = parse(s)[0]; return specOf(b) || { all: KEY[b] || "paint" }; });
    for (let i = 0; i + 5 < bytes.length; i += 6) {
      const x = bytes.charCodeAt(i), y = bytes.charCodeAt(i + 1), z = bytes.charCodeAt(i + 2), p = bytes.charCodeAt(i + 3) | (bytes.charCodeAt(i + 4) << 8), m = bytes.charCodeAt(i + 5);
      const L = looks[p], s = shapes[p];
      const top = L.top || L.all, bottom = L.bottom || L.all, side = L.side || L.all;
      if (!s) {
        G.box(x, y, z, x + 1, y + 1, z + 1, { top: m & 1 ? top : null, bottom: m & 2 ? bottom : null, s: m & 4 ? side : null, n: m & 8 ? side : null, e: m & 16 ? side : null, w: m & 32 ? side : null });
        continue;
      }
      for (const b of s) G.box(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], { top, bottom, side });
    }
    // The sea round it.
    const M = 60;
    G.box(-M, D.sea - 0.14, -M, D.W + M, D.sea - 0.12, D.D + M, { top: "shipSea", all: null });
    R.item("Ship", "John's " + D.name + " as it stands on the map, every block of it: each kind of block in its own colour in our looks (painted steel clean and seamless, timber boards, brushed metal, tinted glass, lamps lit), in the very pictures the game now draws it in. The things the game draws apart from the blocks (signs, banners, beds, heads) are as John built them.");
    R.item("Looking round", "Drag to turn round it; right-drag, two fingers or Shift and drag to move across; the wheel or a pinch to come in and out; W A S D move across too.");
    return R;
  };
'''
s = s.replace(old, new)
open(R + "rooms70.js", "w").write(s)
print("ok")
