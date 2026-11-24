// ---------- 0.59.7 (John, 15:33): every gate the same, Kanto's and Johto's ----------
// "All the gateways, just double check that they all work in Kanto and in Johto ... I did try a couple in Johto and the
// interior design really did suck ... make sure that all of them are working and the way you go in and come out is
// honoured." Every Johto gatehouse on John's map now has its inside (it had none: its dark doorway went nowhere),
// built from the one gatehouse design all Kanto's gates use, in at one side and out at the other (johto_gates.json,
// from a scan of the map: each one's dark doorway, its two faces, which way it runs). The NATIONAL PARK's two (Crystal's
// layouts) are rebuilt the same way, their doors kept. "I'm not a big fan of the floor ... changed across the board":
// every gate's floor is now pale stone flags, not the yellow check.
(function () {
  // The gates' floor: big pale stone flags, a block each, a fine grout between, each flag a hair different.
  TEXDEF.gateCheck = [32, 32, (P, r) => {
    const flags = [hx("#E9E5DC"), hx("#E4DFD5"), hx("#EDE9E1"), hx("#E6E1D8")];
    for (let ty = 0; ty < 2; ty++) for (let tx = 0; tx < 2; tx++) {
      const base = flags[(tx * 2 + ty * 3 + Math.floor(r() * 4)) % 4];
      for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
        let k = (r() - 0.5) * 0.02;
        if (x === 0 || y === 0) k -= 0.1;
        else if (x === 1 || y === 1) k += 0.05;
        else if (x === 15 || y === 15) k -= 0.05;
        // A faint soft sheen across each flag.
        k += 0.02 * Math.cos((x + y - 15) / 10);
        P.set(tx * 16 + x, ty * 16 + y, shadec(base, k));
      }
    }
  }];

  const facing = { n: FACE.n, s: FACE.s, e: FACE.e, w: FACE.w };
  const GATES = JOHTO_GATES;
  for (const g of GATES) PLACES.push({ id: g.id, name: g.name, floor: g.floor, group: "Johto's gates" });
  for (const g of GATES) {
    BUILD[g.id] = function () {
      const closed = { none: true, lines: ["The way on isn't open yet."] };
      if (g.axis === "we") {
        const R = new Room(g.id, { name: g.name, floor: g.floor, W: 16, D: 10, outsideSize: "16 by 12" });
        lgGateSide(R, { west: g.sides.includes("w") ? {} : closed, east: g.sides.includes("e") ? {} : closed, view: "Outside: " + g.a + " one way, " + g.b + " the other.",
          leaflet: "A glass table. There's a leaflet: JOHTO's ROUTES, A WALKER'S GUIDE." });
        R.npcs.push({ name: "Officer", key: g.id + "_p0", x: 7.0, z: 0.8, yaw: facing.s, look: { skin: "guard" }, talk: () => [{ text: "Welcome! Mind how you go." }] });
        R.spawn = g.sides.includes("e") ? { x: 14.4, z: 5.0, yaw: FACE.w } : { x: 1.6, z: 5.0, yaw: FACE.e };
        return R;
      }
      const R = new Room(g.id, { name: g.name, floor: g.floor, W: 14, D: 14, outsideSize: "12 by 16" });
      lgGate(R, { south: g.sides.includes("s") ? { to: "town" } : closed, north: g.sides.includes("n") ? { to: "town" } : closed, view: "Outside: " + g.a + " to the north, " + g.b + " to the south.",
        leaflet: "A glass table. There's a leaflet: JOHTO's ROUTES, A WALKER'S GUIDE.", map: "A glass table. Someone's left a map of JOHTO on it." });
      R.npcs.push({ name: "Officer", key: g.id + "_p0", x: 12.6, z: 6.0, yaw: facing.w, look: { skin: "guard" }, talk: () => [{ text: "Welcome! Mind how you go." }] });
      R.spawn = LG_GATE_IN;
      return R;
    };
  }

  // The NATIONAL PARK's gates, as every other gate (their doors in the world as before): ROUTE 35's in from the route
  // at its south, out at its north to the PARK; ROUTE 36's in from the route at its east, out at its west.
  BUILD.route_35_national_park_gate = function () {
    const R = new Room("route_35_national_park_gate", { name: "National Park gate", floor: "Route 35", W: 14, D: 14, outsideSize: "12 by 16" });
    lgGate(R, { south: { to: "town" }, north: { to: "town" }, view: "Outside: the NATIONAL PARK's trees to the north, ROUTE 35 to the south.",
      leaflet: "A glass table. There's a leaflet: the BUG-CATCHING CONTEST, TUESDAYS, THURSDAYS AND SATURDAYS!" });
    R.npcs.push({ name: "Officer", key: "route_35_national_park_gate_p0", x: 12.6, z: 6.0, yaw: FACE.w, look: { skin: "guard" },
      talk: () => [{ text: "The NATIONAL PARK is just through here. Enjoy your walk!" }] });
    R.spawn = LG_GATE_IN;
    return R;
  };
  BUILD.route_36_national_park_gate = function () {
    const R = new Room("route_36_national_park_gate", { name: "National Park gate", floor: "Route 36", W: 16, D: 10, outsideSize: "16 by 12" });
    lgGateSide(R, { view: "Outside: the NATIONAL PARK one way, ROUTE 36 the other.",
      leaflet: "A glass table. There's a leaflet: the BUG-CATCHING CONTEST, TUESDAYS, THURSDAYS AND SATURDAYS!" });
    R.npcs.push({ name: "Officer", key: "route_36_national_park_gate_p0", x: 7.0, z: 0.8, yaw: FACE.s, look: { skin: "guard" },
      talk: () => [{ text: "The NATIONAL PARK is just through here. Enjoy your walk!" }] });
    R.spawn = { x: 14.4, z: 5.0, yaw: FACE.w };
    return R;
  };
})();
