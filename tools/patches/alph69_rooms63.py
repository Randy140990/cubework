# (0.69) rooms63.js: the two Ruins of Alph gates' ruins sides lead into the RUINS OF ALPH.
p = "tools/interior/rooms63.js"
s = open(p).read()
old_a = """  const facing = { n: FACE.n, s: FACE.s, e: FACE.e, w: FACE.w };
  const GATES = JOHTO_GATES;"""
new_a = """  const facing = { n: FACE.n, s: FACE.s, e: FACE.e, w: FACE.w };
  const GATES = JOHTO_GATES;
  // (0.69, John's 26 Nov 15:58: "we've got the ruins of Alf to incorporate within the 2 gates ... you would go into
  // them. Those gates obviously need to be open.") The two gates' ruins sides go into the RUINS OF ALPH (tools/alph.py,
  // rooms71.js), not out into the gap John's left for it in the world: ROUTE 36's gate out of its south side, in
  // front of the ruins' north gatehouse facing south; ROUTE 32's out of its west side, in front of the east one
  // facing west.
  const INTO_RUINS = { jgate_alph: { s: { to: "ruins_of_alph", spawn: { x: 58.0, z: 43.4, yaw: FACE.s } } },
    jgate_r32: { w: { to: "ruins_of_alph", spawn: { x: 104.6, z: 110.0, yaw: FACE.w } } } };
  const way = (g, f, dflt) => (INTO_RUINS[g.id] && INTO_RUINS[g.id][f]) || dflt;"""
assert s.count(old_a) == 1
s = s.replace(old_a, new_a)
old_b = """lgGateSide(R, { west: g.sides.includes("w") ? {} : closed, east: g.sides.includes("e") ? {} : closed, view:"""
new_b = """lgGateSide(R, { west: g.sides.includes("w") ? way(g, "w", {}) : closed, east: g.sides.includes("e") ? way(g, "e", {}) : closed, view:"""
assert s.count(old_b) == 1
s = s.replace(old_b, new_b)
old_c = """lgGate(R, { south: g.sides.includes("s") ? { to: "town" } : closed, north: g.sides.includes("n") ? { to: "town" } : closed, view:"""
new_c = """lgGate(R, { south: g.sides.includes("s") ? way(g, "s", { to: "town" }) : closed, north: g.sides.includes("n") ? way(g, "n", { to: "town" }) : closed, view:"""
assert s.count(old_c) == 1
s = s.replace(old_c, new_c)
open(p, "w").write(s)

# The Route 32 gate is the Ruins of Alph's (its west side is the ruins, not Violet City): its name and what's outside.
import json, re
for p in ("tools/interior/johto_gates.json",):
    d = json.load(open(p))
    for g in d:
        if g["id"] == "jgate_r32":
            g.update({"name": "Ruins of Alph gate", "floor": "Route 32 to the Ruins of Alph", "a": "ROUTE 32", "b": "the RUINS OF ALPH"})
    txt = json.dumps(d, indent=1) if open(p).read().lstrip().startswith("[\n") else json.dumps(d)
    open(p, "w").write(txt + ("\n" if txt[-1] != "\n" else ""))
p = "tools/interior/johto_gates_data.js"
s = open(p).read()
old = '{"id":"jgate_r32","name":"Route 32 gate","floor":"Violet City to Route 32","place":"route_32","axis":"we","sides":["e","w"],"a":"ROUTE 32","b":"VIOLET CITY",'
new = '{"id":"jgate_r32","name":"Ruins of Alph gate","floor":"Route 32 to the Ruins of Alph","place":"route_32","axis":"we","sides":["e","w"],"a":"ROUTE 32","b":"the RUINS OF ALPH",'
assert s.count(old) == 1
s = s.replace(old, new)
open(p, "w").write(s)
print("ok")
