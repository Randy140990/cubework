// ---------- 0.54.0: CELADON CITY's GAME CORNER, its PRIZE ROOM and the CELADON DEPT. STORE, as FireRed's ----------
// Every room is FireRed's own map (pret/pokefirered), a FireRed tile a block: tools/celadon.py reads the layouts,
// warps, signs and people into celadon_data.js. The DEPT. STORE: its lilac hall with the stairs and the elevator on
// every floor, the orange-tiled shop floors, the SERVICE COUNTER on 1F, the TRAINER'S MARKET's racks and glass cases,
// the TV GAME SHOP's counters, the WISEMAN GIFTS, the DRUG STORE's shelves of bottles, the ROOFTOP SQUARE's vending
// machines, and its elevator. The GAME CORNER: its gold carpet, the rows of slot machines and their stools, the
// counter, the violet stage at the back (its marquee of bulbs, as the roof outside), the poster; and the PRIZE ROOM.
(function () {
  const reg = (k, w, h, draw, m) => { TEXDEF[k] = [w, h, draw]; MATDEF[k] = Object.assign({ tex: k, per: 1 }, m || {}); };
  const DS = ["celadon_mart_1f", "celadon_mart_2f", "celadon_mart_3f", "celadon_mart_4f", "celadon_mart_5f", "celadon_mart_roof", "celadon_mart_elevator"];
  const GCR = ["game_corner", "game_corner_prize_room"];
  for (const id of DS) PLACES.push({ id, name: "Celadon Dept. Store", floor: CELADON[id].floor, group: "Celadon City" });
  for (const id of GCR) PLACES.push({ id, name: id === "game_corner" ? "Game Corner" : "Prize Room", floor: "", group: "Celadon City" });

  // ----- Materials: the DEPT. STORE (FireRed's lilac walls, orange tiles, white counters with green rims) -----
  reg("dsWall", 16, 16, (P, r) => {
    P.fill(hx("#A493B4"), 0.008, r);
    for (const x of [0, 8]) P.rect(x, 0, 1, 16, hx("#9483A4"));
    P.rect(0, 15, 16, 1, hx("#8C7B9C"));
  });
  reg("dsWallLight", 16, 16, (P, r) => {
    P.fill(hx("#DCD4EA"), 0.006, r);
    for (let y = 4; y < 16; y += 6) for (let x = (y % 12 === 4 ? 0 : 4); x < 16; x += 8) {
      P.rect(x + 1, y, 2, 1, hx("#CDACDE")); P.rect(x, y + 1, 1, 2, hx("#CDACDE")); P.rect(x + 3, y + 1, 1, 2, hx("#CDACDE")); P.rect(x + 1, y + 3, 2, 1, hx("#CDACDE"));
    }
  });
  reg("dsBand", 16, 16, (P, r) => { P.fill(hx("#F6FAFC"), 0.004, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#CFC6DD")); });
  reg("dsCap", 16, 16, (P, r) => { P.fill(hx("#5E5C78"), 0.006, r); P.rect(0, 0, 16, 1, hx("#7A7894")); P.rect(0, 15, 16, 1, hx("#4A485F")); });
  reg("dsSkirt", 16, 16, (P, r) => { P.fill(hx("#7E7894"), 0.008, r); P.rect(0, 0, 16, 2, hx("#9C94AE")); });
  reg("dsHall", 16, 16, (P, r) => {
    P.fill(hx("#D9B4E8"), 0.012, r);
    for (let y = 1; y < 16; y += 4) for (let x = (y % 8 === 1 ? 1 : 3); x < 16; x += 4) P.set(x, y, hx("#C29AD4"));
  });
  reg("dsTile", 16, 16, (P, r) => {
    const b = hx("#E8913E");
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      let k = (r() - 0.5) * 0.02;
      if (x === 0 || y === 0) k -= 0.12;
      if (x + y > 6 && x + y < 10) k += 0.05;
      P.set(x, y, shadec(b, k));
    }
  });
  reg("dsCarpet", 16, 16, (P, r) => {
    P.fill(hx("#EED654"), 0.012, r);
    for (let y = 2; y < 16; y += 4) for (let x = 2; x < 16; x += 4) P.rect(x, y, 1, 1, hx("#C9AE3E"));
  });
  reg("dsFloorEdge", 16, 16, (P, r) => { P.fill(hx("#B98A5A"), 0.01, r); P.rect(0, 0, 16, 1, hx("#D9A86C")); });
  reg("dsCounter", 16, 16, (P, r) => {
    P.fill(hx("#F6FAFC"), 0.004, r); P.rect(0, 0, 16, 1, hx("#FFFFFF"));
    P.rect(0, 12, 16, 3, hx("#83BD39")); P.rect(0, 12, 16, 1, hx("#A6D85E")); P.rect(0, 15, 16, 1, hx("#5E8F25"));
  });
  reg("dsCounterTop", 16, 16, (P, r) => { P.fill(hx("#FBFDFE"), 0.003, r); P.rect(0, 0, 16, 1, hx("#E1E6EE")); P.rect(0, 15, 16, 1, hx("#E1E6EE")); });
  reg("dsSofa", 16, 16, (P, r) => { P.fill(hx("#F4F2F8"), 0.01, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 13, 16, 3, hx("#CDACDE")); });
  reg("dsSofaArm", 16, 16, (P, r) => { P.fill(hx("#CDACDE"), 0.01, r); P.rect(0, 0, 16, 1, hx("#E2CBEC")); P.rect(0, 15, 16, 1, hx("#A98BB8")); });
  reg("dsLeg", 8, 8, (P, r) => P.fill(hx("#CD8341"), 0.02, r));
  reg("dsLiftDoor", 16, 32, (P, r) => {
    P.fill(hx("#EEAC52"), 0.01, r);
    for (let y = 0; y < 32; y++) { P.set(7, y, hx("#9E5A34")); P.set(8, y, hx("#F7C982")); }
    P.rect(0, 0, 16, 2, hx("#CD734A")); P.rect(0, 0, 1, 32, hx("#CD734A")); P.rect(15, 0, 1, 32, hx("#CD734A"));
    P.rect(3, 6, 2, 6, hx("#F7C982")); P.rect(11, 6, 2, 6, hx("#F7C982"));
  }, { fit: true });
  reg("dsLiftFrame", 16, 16, (P, r) => { P.fill(hx("#CD734A"), 0.01, r); P.rect(0, 0, 16, 1, hx("#E08B5E")); });
  reg("dsLiftLamp", 16, 8, (P, r) => {
    P.fill(hx("#2B2633"), 0.01, r);
    P.rect(3, 2, 1, 4, hx("#9BE36B")); P.rect(2, 3, 3, 1, hx("#9BE36B")); P.set(3, 1, hx("#9BE36B"));
    P.rect(11, 2, 1, 4, hx("#9BE36B")); P.rect(10, 4, 3, 1, hx("#9BE36B")); P.set(11, 6, hx("#9BE36B"));
  }, { fit: true, glow: 0.7 });
  reg("dsPlate", 16, 16, (P, r) => { P.fill(hx("#CD734A"), 0.01, r); P.rect(1, 1, 14, 14, hx("#E48A55")); P.rect(3, 6, 10, 4, hx("#F7D9B0")); });
  reg("dsRack", 16, 16, (P, r) => {
    P.fill(hx("#F7F8FA"), 0.004, r);
    for (const y of [0, 5, 10, 15]) P.rect(0, y, 16, 1, hx("#7B7B84"));
    const goods = ["#E2484A", "#4C8FD6", "#F2C230", "#5DBB63", "#F08A3C", "#9C6ADE", "#FFFFFF"];
    for (const y0 of [1, 6, 11]) for (let x = 1; x < 15; x += 3) { const c = hx(goods[Math.floor(r() * goods.length)]); P.rect(x, y0 + (r() < 0.4 ? 1 : 0), 2, 4 - (r() < 0.4 ? 1 : 0), c); }
  });
  reg("dsRackTop", 16, 16, (P, r) => {
    P.fill(hx("#E9EDF2"), 0.004, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#B9C0C9"));
    const goods = ["#E2484A", "#4C8FD6", "#F2C230", "#5DBB63", "#F08A3C", "#9C6ADE"];
    for (let y = 2; y < 14; y += 4) for (let x = 2; x < 14; x += 4) { P.rect(x, y, 3, 3, hx(goods[Math.floor(r() * goods.length)])); P.set(x, y, hx("#FFFFFF")); }
  });
  reg("dsRackSide", 16, 16, (P, r) => { P.fill(hx("#DEE3EA"), 0.006, r); P.rect(0, 0, 2, 16, hx("#FFFFFF")); P.rect(14, 0, 2, 16, hx("#B9C0C9")); });
  reg("dsBottles", 16, 16, (P, r) => {
    P.fill(hx("#F7F8FA"), 0.004, r);
    for (const y of [0, 7, 15]) P.rect(0, y, 16, 1, hx("#7B7B84"));
    const cs = ["#E2484A", "#F2C230", "#4C8FD6", "#5DBB63", "#C86ADE"];
    for (const y0 of [2, 9]) for (let x = 1; x < 15; x += 3) { const c = hx(cs[Math.floor(r() * cs.length)]); P.rect(x, y0 + 1, 2, 4, c); P.rect(x, y0, 2, 1, hx("#FFFFFF")); }
  });
  reg("dsGlass", 16, 16, (P, r) => {
    P.fill(hx("#C7E9FF"), 0.01, r);
    for (let i = 0; i < 16; i++) if (i % 5 < 2) P.line(i, 0, i - 6, 6, hx("#E9F6FF"));
    P.rect(0, 0, 16, 1, hx("#8BB4DE")); P.rect(0, 15, 16, 1, hx("#8BB4DE"));
  });
  reg("dsGlassSide", 16, 16, (P, r) => {
    P.fill(hx("#DDEEFA"), 0.008, r); P.rect(0, 0, 16, 2, hx("#8BB4DE")); P.rect(0, 12, 16, 4, hx("#F6FAFC"));
    for (let x = 2; x < 14; x += 4) P.rect(x, 5, 2, 4, hx(["#E2484A", "#F2C230", "#4C8FD6", "#5DBB63"][(x / 4) | 0]));
  });
  reg("dsTV", 16, 12, (P, r) => {
    P.fill(hx("#3A3A44"), 0, r); P.rect(1, 1, 14, 9, hx("#3F86C9"));
    P.rect(3, 6, 10, 3, hx("#5DBB63")); P.rect(9, 2, 3, 3, hx("#F2C230")); P.rect(4, 3, 2, 2, hx("#FFFFFF"));
  }, { fit: true, glow: 0.5 });
  reg("dsConsole", 16, 16, (P, r) => {
    P.fill(hx("#C9C9D3"), 0.006, r); P.rect(0, 0, 16, 1, hx("#E6E6EE"));
    P.rect(3, 4, 4, 2, hx("#6F5FAF")); P.rect(9, 4, 4, 2, hx("#9C8BCC")); P.rect(2, 10, 12, 2, hx("#8F8F9C"));
  });
  reg("dsMachine", 16, 16, (P, r) => {
    P.fill(hx("#D5CDE6"), 0.008, r); P.rect(0, 0, 16, 1, hx("#ECE6F4")); P.rect(0, 15, 16, 1, hx("#9C8BAC"));
    P.rect(3, 3, 10, 6, hx("#52526A")); P.rect(4, 4, 8, 4, hx("#8FD3A8")); P.rect(5, 11, 6, 2, hx("#9C8BAC"));
  });
  reg("dsMachineTop", 16, 16, (P, r) => { P.fill(hx("#CDC4E0"), 0.006, r); P.rect(1, 1, 14, 14, hx("#BFB4D6")); });
  reg("dsPoster", 16, 12, (P, r) => {
    P.fill(hx("#FFFFFF"), 0, r); P.rect(0, 0, 16, 1, hx("#9C8BAC")); P.rect(0, 11, 16, 1, hx("#9C8BAC"));
    P.rect(2, 2, 12, 5, hx("#F08A3C")); P.rect(5, 3, 6, 3, hx("#FFFFFF")); P.rect(7, 4, 2, 1, hx("#E2484A"));
    P.rect(3, 8, 10, 1, hx("#7B7B84")); P.rect(3, 10, 7, 1, hx("#7B7B84"));
  }, { fit: true });
  reg("dsSign", 16, 10, (P, r) => {
    P.fill(hx("#A8865A"), 0, r); P.rect(1, 1, 14, 8, hx("#FBF8F0"));
    for (const y of [3, 5, 7]) P.rect(3, y, 7 + (y % 3), 1, hx(y === 3 ? "#E2484A" : "#4C6E92"));
  }, { fit: true });
  reg("dsStep", 16, 16, (P, r) => { P.fill(hx("#B8A8C6"), 0.008, r); P.rect(0, 0, 16, 2, hx("#D9CDE4")); P.rect(0, 14, 16, 2, hx("#8C7B9C")); });
  reg("dsStepSide", 16, 16, (P, r) => { P.fill(hx("#9C8BAC"), 0.006, r); P.rect(0, 0, 16, 1, hx("#B8A8C6")); });
  reg("dsRail", 8, 8, (P, r) => { P.fill(hx("#E58A3C"), 0.01, r); P.rect(0, 0, 8, 1, hx("#F7B26A")); });
  reg("dsWell", 16, 16, (P, r) => { P.fill(hx("#4E4A5E"), 0.01, r); P.rect(0, 0, 16, 1, hx("#6C6780")); });
  // The ROOFTOP SQUARE.
  reg("roofTile", 16, 16, (P, r) => {
    P.fill(hx("#D7E0BF"), 0.01, r); P.rect(0, 0, 16, 1, hx("#B6BFA6")); P.rect(0, 0, 1, 16, hx("#B6BFA6")); P.rect(1, 1, 14, 1, hx("#E4EBD0"));
  });
  reg("roofRug", 16, 16, (P, r) => { P.fill(hx("#CFAEE0"), 0.012, r); P.rect(0, 0, 16, 1, hx("#B596C6")); });
  reg("roofFence", 16, 16, (P, r) => {
    P.fill(hx("#5ACD73"), 0.01, r); P.rect(0, 0, 16, 2, hx("#8BE39C")); P.rect(0, 14, 16, 2, hx("#299C62"));
    for (const x of [3, 11]) P.rect(x, 2, 2, 12, hx("#396A52"));
  });
  reg("roofFenceRail", 16, 16, (P, r) => { P.fill(hx("#3FA864"), 0.01, r); P.rect(0, 0, 16, 1, hx("#8BE39C")); });
  reg("roofHouse", 16, 16, (P, r) => { P.fill(hx("#E8F6EF"), 0.005, r); P.rect(0, 15, 16, 1, hx("#BDC5D5")); P.rect(0, 0, 16, 1, hx("#F7FCFA")); });
  reg("roofHouseCap", 16, 16, (P, r) => { P.fill(hx("#9CACAC"), 0.006, r); P.rect(0, 0, 16, 1, hx("#BDC9C9")); });
  reg("vendBody", 16, 16, (P, r) => { P.fill(hx("#CD734A"), 0.01, r); P.rect(0, 0, 16, 1, hx("#E58C62")); P.rect(0, 15, 16, 1, hx("#9E5232")); });
  reg("vendFace", 16, 32, (P, r) => {
    P.fill(hx("#CD734A"), 0.01, r); P.rect(1, 2, 14, 13, hx("#F6FFFF"));
    const cans = ["#4C8FD6", "#E2484A", "#F2C230"];
    for (let i = 0; i < 3; i++) for (let j = 0; j < 2; j++) { P.rect(2 + i * 5, 3 + j * 6, 3, 5, hx(cans[i])); P.rect(2 + i * 5, 3 + j * 6, 3, 1, hx("#FFFFFF")); }
    P.rect(2, 18, 9, 3, hx("#52526A")); P.rect(12, 18, 2, 5, hx("#F2C230")); P.rect(3, 25, 10, 4, hx("#3A3A44"));
  }, { fit: true, glow: 0.25 });
  reg("roofTable", 16, 16, (P, r) => { P.fill(hx("#EEF7F2"), 0.004, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(0, 15, 16, 1, hx("#BDC5D5")); });

  // ----- The GAME CORNER: gold carpet, sky-blue walls, the slot machines, the violet stage -----
  reg("gcCarpet", 16, 16, (P, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const d = (Math.abs(x - 7.5) + Math.abs(y - 7.5));
      const c = d < 3 ? "#E9C552" : d < 6 ? "#DFB22A" : d < 7.2 ? "#C08A1E" : "#D6A92C";
      P.set(x, y, shadec(hx(c), (r() - 0.5) * 0.02));
    }
  });
  reg("gcWall", 16, 16, (P, r) => {
    P.fill(hx("#ACCDEE"), 0.006, r);
    for (let x = 0; x < 16; x++) { const y = 2 + Math.round(Math.sin(x / 16 * Math.PI * 2) * 1.2); P.rect(x, 0, 1, y, hx("#FFFFFF")); P.set(x, y, hx("#D5E6EE")); }
  });
  reg("gcWallLow", 16, 16, (P, r) => {
    P.fill(hx("#ACCDEE"), 0.006, r); P.rect(0, 8, 16, 5, hx("#FFA473")); P.rect(0, 8, 16, 1, hx("#FFDEBD")); P.rect(0, 13, 16, 3, hx("#838383"));
  });
  reg("gcCap", 16, 16, (P, r) => { P.fill(hx("#5B6F86"), 0.006, r); P.rect(0, 0, 16, 1, hx("#7D93AB")); });
  reg("gcSlot", 16, 16, (P, r) => { P.fill(hx("#D57339"), 0.012, r); P.rect(0, 0, 16, 1, hx("#FFBD62")); P.rect(0, 15, 16, 1, hx("#A44A20")); P.rect(0, 0, 1, 16, hx("#E88A4C")); });
  reg("gcSlotFace", 16, 32, (P, r) => {
    P.fill(hx("#D57339"), 0.01, r); P.rect(0, 0, 16, 5, hx("#A44A20"));
    for (let x = 1; x < 15; x += 2) P.set(x, 2, hx(x % 4 === 1 ? "#FFE680" : "#FF7A5A"));
    P.rect(1, 7, 14, 9, hx("#3A2A20"));
    for (let i = 0; i < 3; i++) {
      P.rect(2 + i * 4, 8, 3, 7, hx("#FFFFFF"));
      const sym = ["#E2484A", "#F2C230", "#4C8FD6"][(i + Math.floor(r() * 3)) % 3];
      P.rect(3 + i * 4, 10, 1, 3, hx(sym)); P.set(2 + i * 4, 10, hx(sym)); P.set(4 + i * 4, 10, hx(sym));
    }
    P.rect(1, 18, 14, 4, hx("#A4FFB4")); P.rect(2, 19, 4, 2, hx("#5DBB63"));
    P.rect(11, 19, 2, 2, hx("#E2484A")); P.rect(2, 24, 12, 2, hx("#FFBD62")); P.rect(3, 28, 10, 3, hx("#A44A20"));
  }, { fit: true, glow: 0.45 });
  reg("gcSlotTop", 16, 16, (P, r) => { P.fill(hx("#E58A4C"), 0.01, r); P.rect(1, 1, 14, 14, hx("#D57339")); P.rect(6, 6, 4, 4, hx("#FFBD62")); });
  reg("gcStool", 16, 16, (P, r) => { P.fill(hx("#5A8FD6"), 0.02, r); P.rect(0, 0, 16, 2, hx("#8CB6EE")); P.rect(0, 14, 16, 2, hx("#3E6AAE")); });
  reg("gcChrome", 8, 8, (P, r) => { P.fill(hx("#C9D2DC"), 0.02, r); P.rect(0, 0, 8, 1, hx("#F0F4F8")); });
  reg("gcCounter", 16, 16, (P, r) => {
    P.fill(hx("#F6FFFF"), 0.004, r); P.rect(0, 0, 16, 4, hx("#A4FFB4")); P.rect(0, 0, 16, 1, hx("#D4FFDC")); P.rect(0, 13, 16, 3, hx("#838383"));
  });
  reg("gcCounterTop", 16, 16, (P, r) => { P.fill(hx("#B8FFC6"), 0.006, r); P.rect(0, 0, 16, 1, hx("#E4FFE9")); P.rect(0, 15, 16, 1, hx("#7FD892")); });
  reg("gcViolet", 16, 16, (P, r) => {
    P.fill(hx("#C98CF2"), 0.01, r); P.rect(0, 0, 16, 1, hx("#E2B8FF"));
    for (let y = 3; y < 16; y += 4) P.rect(0, y, 16, 1, hx("#B06EE0"));
  });
  reg("gcVioletTop", 16, 16, (P, r) => { P.fill(hx("#8B5AC5"), 0.01, r); P.rect(0, 0, 16, 1, hx("#A57AD8")); });
  reg("gcBulbs", 16, 4, (P, r) => {
    P.fill(hx("#6E44A0"), 0, r);
    for (let x = 1; x < 16; x += 3) { P.rect(x, 1, 2, 2, hx(x % 6 === 1 ? "#FFE680" : "#FFD24A")); }
  }, { fit: true, glow: 0.9 });
  reg("gcEmblem", 32, 16, (P, r) => {
    P.fill(hx("#ACCDEE"), 0, r);
    // An original emblem: a gold coin, a red seven on it, three stars round.
    for (let y = 0; y < 16; y++) for (let x = 0; x < 32; x++) {
      const d = Math.hypot((x - 15.5) / 1.0, (y - 7.5) / 1.0);
      if (d < 7.4) P.set(x, y, hx(d > 6.2 ? "#B46200" : d > 5.4 ? "#F2C230" : "#FFE680"));
    }
    P.rect(13, 4, 6, 1, hx("#E2484A")); P.rect(17, 5, 2, 1, hx("#E2484A")); P.rect(16, 6, 2, 1, hx("#E2484A")); P.rect(15, 7, 2, 4, hx("#E2484A"));
    for (const [x, y] of [[4, 4], [27, 4], [6, 12], [25, 12]]) { P.set(x, y, hx("#FFFFFF")); P.set(x - 1, y, hx("#FFE680")); P.set(x + 1, y, hx("#FFE680")); P.set(x, y - 1, hx("#FFE680")); P.set(x, y + 1, hx("#FFE680")); }
  }, { fit: true, glow: 0.3 });
  reg("gcPrizes", 16, 16, (P, r) => {
    P.fill(hx("#F6FFFF"), 0.004, r);
    for (const y of [0, 7, 15]) P.rect(0, y, 16, 1, hx("#838383"));
    const cs = ["#E2484A", "#F2C230", "#4C8FD6", "#5DBB63", "#F08A3C", "#C86ADE"];
    for (const y0 of [1, 8]) for (let x = 1; x < 15; x += 4) { const c = hx(cs[Math.floor(r() * cs.length)]); P.rect(x, y0 + 2, 3, 4, c); P.rect(x + 1, y0, 1, 2, c); }
  });
  reg("gcPoster", 16, 20, (P, r) => {
    P.fill(hx("#FBF8F0"), 0, r); P.rect(0, 0, 16, 1, hx("#C8BFA8")); P.rect(0, 19, 16, 1, hx("#C8BFA8"));
    P.rect(4, 3, 2, 13, hx("#E2484A")); P.rect(6, 3, 5, 2, hx("#E2484A")); P.rect(10, 4, 2, 5, hx("#E2484A")); P.rect(6, 8, 5, 2, hx("#E2484A"));
    P.rect(8, 10, 2, 3, hx("#E2484A")); P.rect(10, 13, 2, 3, hx("#E2484A"));
  }, { fit: true });
  reg("gcPrinter", 16, 16, (P, r) => { P.fill(hx("#E6E2CC"), 0.008, r); P.rect(0, 0, 16, 1, hx("#FFFFFF")); P.rect(3, 4, 10, 3, hx("#52526A")); P.rect(4, 10, 8, 2, hx("#B4B4A4")); });

  const FACEOF = { FACE_UP: "n", FACE_DOWN: "s", FACE_LEFT: "w", FACE_RIGHT: "e", FACE_LEFT_AND_RIGHT: "e", FACE_UP_AND_DOWN: "s" };
  const floorNo = id => { const f = CELADON[id].floor; if (f === "ROOF") return 6; const m = /^(\d+)F$/.exec(f); return m ? +m[1] : 0; };

  function pieces(F, codes) {
    const seen = new Set(), out = [];
    for (let z = 0; z < F.D; z++) for (let x = 0; x < F.W; x++) {
      const c = F.rows[z][x];
      if (!codes.includes(c) || seen.has(x + "," + z)) continue;
      const cells = [], st = [[x, z]]; seen.add(x + "," + z);
      while (st.length) {
        const [a, b] = st.pop(); cells.push([a, b]);
        for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
          const nx = a + dx, nz = b + dz, k = nx + "," + nz;
          if (nx < 0 || nz < 0 || nx >= F.W || nz >= F.D || seen.has(k) || F.rows[nz][nx] !== c) continue;
          seen.add(k); st.push([nx, nz]);
        }
      }
      const xs = cells.map(c => c[0]), zs = cells.map(c => c[1]);
      out.push({ c, cells, x0: Math.min(...xs), x1: Math.max(...xs) + 1, z0: Math.min(...zs), z1: Math.max(...zs) + 1 });
    }
    return out;
  }

  // A floor's staircase (FireRed's): the stair tiles beside its warp, and which way they go from it.
  function stairOf(F, wx, wz) {
    const pc = pieces(F, "u").find(p => p.cells.some(([a, b]) => a === wx && b === wz));
    if (!pc) return null;
    const others = pc.cells.filter(([a, b]) => !(a === wx && b === wz));
    const mx = others.reduce((s, c) => s + c[0], 0) / Math.max(1, others.length), mz = others.reduce((s, c) => s + c[1], 0) / Math.max(1, others.length);
    const horiz = Math.abs(mx - wx) >= Math.abs(mz - wz);
    const dir = horiz ? [Math.sign(mx - wx), 0] : [0, Math.sign(mz - wz)];
    return { pc, dir };
  }

  function build(id) {
    const F = CELADON[id], W = F.W, D = F.D;
    const gc = id.startsWith("game_corner"), roof = id === "celadon_mart_roof", lift = id === "celadon_mart_elevator";
    const R = new Room(id, { name: F.name, floor: F.floor, W, D, H: roof ? 4 : 3.2 });
    const G = R.geo;
    const at = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? "o" : F.rows[z][x];
    const idAt = (x, z) => x < 0 || z < 0 || x >= W || z >= D ? -1 : F.ids[z][x];
    const warpAt = {}; for (const w of F.warps) warpAt[w.x + "," + w.z] = w;
    const signAt = {}; for (const s of F.signs) signAt[s.x + "," + s.z] = s;
    const isWall = (x, z) => at(x, z) === "w" || (lift && at(x, z) === "m");
    const walkable = (x, z) => { const c = at(x, z); return c === "." || c === "X" || c === "e" || c === "o" && gc || (c === "u" && !!warpAt[x + "," + z]); };
    R._floors = []; R._boxes = []; R.balls = [];
    const TALL = roof ? 2.6 : 2.0, PART = gc ? 1.3 : 1.15;

    // The floor, its kind by FireRed's tile (the hall's lilac carpet, the shop's orange tiles, the yellow carpet, the
    // roof's tiles and rug, the GAME CORNER's gold carpet), and the stairwells going down.
    const FLOORMAT = { l: "dsHall", o: "dsTile", y: "dsCarpet", R: "roofTile", r: "roofRug", g: "gcCarpet", e: "silphLiftFloor" };
    const downCells = new Set(), stairs = [];
    for (const w of F.warps.filter(w => w.kind === "stairs")) {
      const s = stairOf(F, w.x, w.z); if (!s) continue;
      const down = floorNo(w.to) < floorNo(id);
      stairs.push({ w, s, down });
      if (down) for (const [a, b] of s.pc.cells) if (!(a === w.x && b === w.z)) downCells.add(a + "," + b);
    }
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (at(x, z) === "v" || (roof && at(x, z) === "F" && !fenceShown(x, z))) { x++; continue; }
        let x1 = x; while (x1 < W && at(x1, z) !== "v" && !(roof && at(x1, z) === "F" && !fenceShown(x1, z))) x1++;
        R._floors.push([x, z, x1, z + 1]);
        for (let a = x; a < x1; a++) if (!downCells.has(a + "," + z)) G.box(a, -1.2, z, a + 1, 0, z + 1, { top: FLOORMAT[F.fl[z][a]] || "dsTile", all: "dsFloorEdge", bottom: null });
        x = x1;
      }
      x = 0;
      while (x < W) {
        if (walkable(x, z)) { x++; continue; }
        let x1 = x; while (x1 < W && !walkable(x1, z)) x1++;
        R.solid(x, z, x1, z + 1); x = x1;
      }
    }
    R.solid(-1.5, -1, 0, D); R.solid(W, -1, W + 1.5, D); R.solid(-1.5, -1.5, W + 1.5, 0); R.solid(-1.5, D, W + 1.5, D + 1.5);
    // The roof's fence: a railing where it's beside the floor (its outer row, FireRed's perspective, not drawn).
    function fenceShown(x, z) {
      return [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dz]) => ".odubXeu".includes(at(x + dx, z + dz)));
    }
    if (roof) {
      for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
        if (at(x, z) !== "F" || !fenceShown(x, z)) continue;
        const ns = ".odub".includes(at(x, z + 1)) || ".odub".includes(at(x, z - 1));
        // Posts at the cell's ends, a rail along the top and one low down, green bars between.
        if (ns) {
          for (const px of [x, x + 0.88]) G.box(px, 0, z + 0.4, px + 0.12, 1.1, z + 0.6, "roofFenceRail");
          G.box(x, 1.0, z + 0.38, x + 1, 1.12, z + 0.62, "roofFenceRail"); G.box(x, 0.12, z + 0.44, x + 1, 0.95, z + 0.56, { all: "roofFence" });
        } else {
          for (const pz of [z, z + 0.88]) G.box(x + 0.4, 0, pz, x + 0.6, 1.1, pz + 0.12, "roofFenceRail");
          G.box(x + 0.38, 1.0, z, x + 0.62, 1.12, z + 1, "roofFenceRail"); G.box(x + 0.44, 0.12, z, x + 0.56, 0.95, z + 1, { all: "roofFence" });
        }
      }
      R.item("Fence", "The ROOFTOP SQUARE's green railing all round, as FireRed's.");
    }

    // The walls: the back wall tall (with its band), the partitions between the hall and the shop low.
    const wallMat = gc ? "gcWall" : roof ? "roofHouse" : lift ? "silphLiftWall" : "dsWall";
    for (let z = 0; z < D; z++) {
      let x = 0;
      while (x < W) {
        if (!isWall(x, z)) { x++; continue; }
        const part = !gc && !roof && !lift && z >= 3;
        let x1 = x; while (x1 < W && isWall(x1, z) && (!gc && !roof && !lift && z >= 3) === part) x1++;
        if (lift) G.box(x, 0, z, x1, TALL, z + 1, { all: wallMat, top: "silphCap", bottom: null });
        else if (part) {
          G.box(x, 0, z, x1, 0.15, z + 1, { all: "dsSkirt", top: null, bottom: null });
          G.box(x, 0.15, z, x1, PART, z + 1, { all: "dsWallLight", top: "dsCap", bottom: null });
        } else if (gc) {
          G.box(x, 0, z, x1, 0.9, z + 1, { all: "gcWallLow", top: null, bottom: null });
          G.box(x, 0.9, z, x1, TALL, z + 1, { all: "gcWall", top: "gcCap", bottom: null });
        } else if (roof) {
          G.box(x, 0, z, x1, TALL, z + 1, { all: "roofHouse", top: "roofHouseCap", bottom: null });
        } else {
          G.box(x, 0, z, x1, 0.18, z + 1, { all: "dsSkirt", top: null, bottom: null });
          G.box(x, 0.18, z, x1, 0.95, z + 1, { all: "dsWall", top: null, bottom: null });
          G.box(x, 0.95, z, x1, 1.1, z + 1, { all: "dsBand", top: null, bottom: null });
          G.box(x, 1.1, z, x1, TALL, z + 1, { all: "dsWall", top: "dsCap", bottom: null });
        }
        x = x1;
      }
    }
    const faceBelow = (x, z) => { let zz = z + 1; while (zz < D && isWall(x, zz)) zz++; return zz; };

    // The plants (FireRed's are two tiles: one pot for a pair).
    const used = new Set();
    for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
      if (at(x, z) !== "p" || used.has(x + "," + z)) continue;
      used.add(x + "," + z);
      if (at(x, z + 1) === "p" && !used.has(x + "," + (z + 1))) { used.add(x + "," + (z + 1)); plant(R, x + 0.5, z + 1.0, 1.15, { pot: "potWhite", kind: "leafy", lines: ["A tall leafy plant in a white pot."] }); }
      else plant(R, x + 0.5, z + 0.5, 1.0, { pot: "potWhite", kind: "leafy", lines: ["A leafy plant in a white pot."] });
    }

    // The furniture, piece by piece.
    for (const p of pieces(F, "kmshgnSqPVdbo")) {
      if (lift && p.c === "m") continue;
      const { x0, x1, z0, z1, cells } = p;
      const near = [x0 - 0.3, z0 - 0.3, x1 + 0.3, z1 + 0.3];
      if (p.c === "k") {
        for (const [a, b] of cells) {
          if (gc) G.box(a, 0, b, a + 1, 0.95, b + 1, { all: "gcCounter", top: "gcCounterTop" });
          else G.box(a, 0, b, a + 1, 0.92, b + 1, { all: "dsCounter", top: "dsCounterTop" });
          // The GAME CORNER's glass case in the counter's middle.
          if (gc && idAt(a, b) >= 0x2bc && idAt(a, b) <= 0x2be) G.box(a + 0.08, 0.95, b + 0.15, a + 0.92, 1.12, b + 0.85, { all: "dsGlass" });
        }
        R.act(near, { label: "Look", lines: [gc ? "The GAME CORNER's counter. A glass case of prize COINS sits in it." : "A white counter with a green trim."] });
      } else if (p.c === "m") {
        if (gc) {
          G.box(x0 + 0.1, 0, z0 + 0.1, x1 - 0.1, 1.2, z1 - 0.1, { all: "gcPrinter", top: "dsMachineTop" });
        } else for (const [a, b] of cells) {
          G.box(a + 0.06, 0, b + 0.06, a + 0.94, 1.55, b + 0.94, { all: "dsSkirt", s: "dsMachine", top: "dsMachineTop" });
        }
        if (!gc) R.act(near, { label: "Look", lines: ["A store terminal. Its screen shows today's special offers."] });
      } else if (p.c === "s") {
        // A sofa along the wall, its back to the wall it's against.
        const west = x0 === 0, east = x1 === W;
        const bx0 = west ? x0 + 0.06 : x0 + 0.62, bx1 = west ? x1 - 0.62 : x1 - 0.06;
        G.box(x0 + 0.1, 0, z0 + 0.1, x1 - 0.1, 0.1, z1 - 0.1, "dsLeg");
        G.box(x0 + 0.06, 0.1, z0 + 0.08, x1 - 0.06, 0.46, z1 - 0.08, { all: "dsSofa" });
        G.box(bx0, 0.46, z0 + 0.08, bx1, 0.98, z1 - 0.08, { all: "dsSofaArm" });
        G.box(x0 + 0.06, 0.46, z0 + 0.08, x1 - 0.06, 0.68, z0 + 0.3, { all: "dsSofaArm" });
        G.box(x0 + 0.06, 0.46, z1 - 0.3, x1 - 0.06, 0.68, z1 - 0.08, { all: "dsSofaArm" });
        R.act(near, { label: "Sit", lines: ["A soft white sofa for shoppers to rest on."] });
        if (!east && !west) R.item("Sofa", "A white sofa with lilac arms.");
      } else if (p.c === "h") {
        // Racks of goods (the TRAINER'S MARKET's, the WISEMAN GIFTS'), shelves of bottles (the DRUG STORE's).
        const bottles = id === "celadon_mart_5f" && idAt(x0, z0) >= 0x31f && idAt(x0, z0) <= 0x327;
        G.box(x0 + 0.06, 0, z0 + 0.06, x1 - 0.06, 1.7, z1 - 0.06, { all: bottles ? "dsBottles" : "dsRack", e: "dsRackSide", w: "dsRackSide", top: "dsRackTop" });
        R.act(near, { label: "Look", lines: [bottles ? "Shelves of POKéMON medicine, bottle after bottle." : "A rack of goods, every shelf full."] });
      } else if (p.c === "g") {
        G.box(x0 + 0.06, 0, z0 + 0.1, x1 - 0.06, 0.25, z1 - 0.1, { all: "dsCounter", top: null });
        G.box(x0 + 0.06, 0.25, z0 + 0.1, x1 - 0.06, 0.92, z1 - 0.1, { all: "dsGlassSide", top: "dsGlass" });
        R.act(near, { label: "Look", lines: ["A glass case. The goods inside are laid out neatly."] });
      } else if (p.c === "n") {
        // The TV GAME SHOP's counters, a TV and a games console on each: FireRed's TVs and SUPER NES.
        for (const [a, b] of cells) {
          G.box(a, 0, b, a + 1, 0.9, b + 1, { all: "dsCounter", top: "dsCounterTop" });
          const s = signAt[a + "," + b];
          if (s && s.kind && s.kind.startsWith("TV")) { G.box(a + 0.12, 0.9, b + 0.25, a + 0.88, 1.5, b + 0.8, { all: "dsSkirt", s: "dsTV", top: "dsCap" }); }
          else if (s && s.kind === "SuperNES") { G.box(a + 0.18, 0.9, b + 0.3, a + 0.82, 1.04, b + 0.75, { all: "dsConsole" }); G.box(a + 0.3, 0.9, b + 0.78, a + 0.5, 0.95, b + 0.92, "dsCap"); }
        }
      } else if (p.c === "S") {
        // A row of slot machines (FireRed's), each facing its stool.
        for (const [a, b] of cells) {
          const left = at(a - 1, b) === "o", right = at(a + 1, b) === "o";
          const face = left ? "w" : right ? "e" : "s";
          G.box(a + 0.06, 0, b + 0.04, a + 0.94, 1.75, b + 0.96, { all: "gcSlot", top: "gcSlotTop", [face]: "gcSlotFace" });
        }
        R.item("Slot machines", "FireRed's: rows of them, each facing its blue stool. Its reels: sevens, cherries, bars.");
      } else if (p.c === "o") {
        for (const [a, b] of cells) {
          const x = a + 0.5, z = b + 0.5;
          if (gc) {
            G.box(x - 0.2, 0, z - 0.2, x + 0.2, 0.04, z + 0.2, "gcChrome");
            G.box(x - 0.05, 0.04, z - 0.05, x + 0.05, 0.4, z + 0.05, "gcChrome");
            G.box(x - 0.26, 0.4, z - 0.26, x + 0.26, 0.52, z + 0.26, { all: "gcStool" });
          } else chair(R, x, z, at(a + 1, b) === "d" ? "e" : "w", { wood: "roofTable", seat: "roofTable" });
        }
      } else if (p.c === "q") {
        G.box(x0 + 0.04, 0, z0 + 0.1, x1 - 0.04, 1.6, z1 - 0.06, { all: "dsRackSide", s: "gcPrizes", top: "dsCounterTop" });
        R.act(near, { label: "Look", lines: ["Prizes on show: dolls, TMs, things you can win with COINS."] });
      } else if (p.c === "P") {
        // The violet stage at the back, its marquee of bulbs along the front (as the GAME CORNER's roof outside).
        G.box(x0, 0, z0, x1, PART, z1, { all: "gcViolet", top: "gcVioletTop" });
        G.box(x0, PART - 0.18, z1, x1, PART, z1 + 0.02, { s: "gcBulbs", all: null });
        R.item("The violet stage", "At the back: violet, with a row of lit bulbs along its front, as the roof outside.");
      } else if (p.c === "V") {
        // The ROOFTOP SQUARE's vending machines: each a column, its face to the square.
        for (let a = x0; a < x1; a++) G.box(a + 0.05, 0, z0 + 0.4, a + 0.95, 1.95, z1 - 0.05, { all: "vendBody", s: "vendFace", top: "dsCap" });
      } else if (p.c === "d") {
        table(R, x0 + 0.1, z0 + 0.1, x1 - 0.1, z1 - 0.1, { top: "roofTable", leg: "gcChrome", side: "roofTable", h: 0.78, name: "Table", note: "A round-cornered white table on the roof, its chairs either side." });
      } else if (p.c === "b") {
        bin(R, x0 + 0.5, z0 + 0.5);
      }
    }

    // The GAME CORNER's back wall: its emblem in the middle, prize shelves either side, the poster.
    if (gc) {
      const front = 2;
      const em = [];
      for (let x = 0; x < W; x++) if ([0x283, 0x284].includes(idAt(x, 0))) em.push(x);
      if (em.length) G.box(Math.min(...em) - 0.4, 1.1, front - 0.02, Math.max(...em) + 1.4, 1.95, front, { s: "gcEmblem", all: null });
      for (let x = 0; x < W; x++) if ([0x28d, 0x28e].includes(idAt(x, 1))) G.box(x + 0.04, 0.9, front - 0.3, x + 0.96, 1.7, front, { s: "gcPrizes", all: "dsRackSide", top: "dsCounterTop" });
    }
    // On the walls: the floor signs, the posters, the layout board.
    for (const s of F.signs) {
      if (s.use === "vending" || s.elevator) continue;
      const k = s.kind || "";
      if (isWall(s.x, s.z) || at(s.x, s.z) === "w") {
        const pz = faceBelow(s.x, s.z);
        if (k === "Poster" && gc) G.box(s.x + 0.12, 0.95, pz - 0.02, s.x + 0.88, 1.85, pz, { s: "gcPoster", all: null });
        else if (k === "Poster") G.box(s.x + 0.12, 0.55, pz, s.x + 0.88, 1.05, pz + 0.03, { s: "dsPoster", all: null });
        else G.box(s.x + 0.15, 1.2, pz, s.x + 0.85, 1.6, pz + 0.03, { s: "dsSign", all: null });
        R.act([s.x - 0.2, pz - 0.2, s.x + 1.2, pz + 1.0], { label: k === "Poster" && gc ? "Look" : "Read", lines: s.lines });
      } else {
        R.act([s.x - 0.25, s.z - 0.25, s.x + 1.25, s.z + 1.25], { label: k.startsWith("SlotMachine") || k.startsWith("Unusable") ? "Play" : "Look", lines: s.lines });
      }
    }
    // The vending machines: pick a drink (in the game, bought with your money).
    for (const s of F.signs.filter(s => s.use === "vending")) {
      R.act([s.x - 0.1, s.z + 0.2, s.x + 1.1, s.z + 1.6], { label: "Use", lines: s.lines, use: "vending", run: async say => {
        const pick = await say([{ text: "A vending machine! What does it have?", ask: ["FRESH WATER ¥200", "SODA POP ¥300", "LEMONADE ¥350", "CANCEL"] }]);
        if (pick != null && pick < 3) await say([{ text: "A can of " + ["FRESH WATER", "SODA POP", "LEMONADE"][pick] + " popped out!" }]);
      } });
    }
    if (roof) R.item("Vending machines", "The ROOFTOP SQUARE's: FRESH WATER, SODA POP and LEMONADE.");

    // The stairs between floors, up or down from their warp (FireRed's run sideways, by the hall's wall).
    for (const { w, s, down } of stairs) {
      const [dx, dz] = s.dir;
      for (const [a, b] of s.pc.cells) {
        if (a === w.x && b === w.z) continue;
        const n = Math.abs(dx ? a - w.x : b - w.z), step = Math.min(n * 0.4, 1.6);
        if (down) G.box(a, -1.2, b, a + 1, -step, b + 1, { top: "dsStep", all: "dsWell", bottom: null });
        else G.box(a, 0, b, a + 1, step, b + 1, { top: "dsStep", all: "dsStepSide", bottom: null });
      }
      // Its orange banister along the open side.
      if (dx && !roof) {
        const zs = s.pc.cells.map(c => c[1]), zb = Math.max(...zs) + 1, xs = s.pc.cells.filter(c => !(c[0] === w.x && c[1] === w.z)).map(c => c[0]);
        const xa = Math.min(...xs), xb = Math.max(...xs) + 1;
        G.box(xa, down ? 0 : 0.6, zb - 0.08, xb, down ? 0.85 : 1.6, zb, "dsRail");
      }
      // You come out beside the far floor's stairs, facing away from them.
      const T = CELADON[w.to], ts = stairOf(T, w.tx, w.tz);
      const [ox, oz] = ts ? [-ts.dir[0], -ts.dir[1]] : [0, 1];
      const yaw = ox > 0 ? FACE.e : ox < 0 ? FACE.w : oz > 0 ? FACE.s : FACE.n;
      const spawn = { x: w.tx + ox + 0.5, z: w.tz + oz + 0.5, yaw };
      const bx = dx > 0 ? [w.x + 0.35, w.x + 1] : dx < 0 ? [w.x, w.x + 0.65] : [w.x, w.x + 1];
      const bz = dz < 0 ? [w.z, w.z + 0.65] : dz > 0 ? [w.z + 0.35, w.z + 1] : [w.z, w.z + 1];
      R._boxes.push({ x0: bx[0], z0: bz[0], x1: bx[1], z1: bz[1], to: w.to, spawn });
      R.warps.push({ test: p => p.x > bx[0] && p.x < bx[1] && p.z > bz[0] && p.z < bz[1], to: w.to, spawn, fx: "fade" });
      R.item(down ? "Stairs down" : "Stairs up", "FireRed's stairs, by the hall's wall: walk onto them to go " + (down ? "down." : "up."));
    }

    // The elevator's doors (orange, its arrows lit over them): walk in and you're in the elevator.
    for (const w of F.warps.filter(w => w.kind === "elevator")) {
      const x = w.x, z = w.z;
      G.box(x, 0, z + 0.9, x + 1, 1.9, z + 0.96, { s: "dsLiftDoor", all: "dsLiftFrame" });
      G.box(x - 0.1, 0, z + 0.86, x, 2.0, z + 1, { all: "dsLiftFrame" }); G.box(x + 1, 0, z + 0.86, x + 1.1, 2.0, z + 1, { all: "dsLiftFrame" });
      G.box(x - 0.1, 1.9, z + 0.86, x + 1.1, 2.0, z + 1, { all: "dsLiftFrame" });
      G.box(x + 0.2, 1.62, z + 0.96, x + 0.8, 1.84, z + 0.98, { s: "dsLiftLamp", all: null });
      // The floor's plate in front of the doors.
      G.box(x - 1, 0, z + 1.05, x + 2, 0.03, z + 1.95, { top: "dsPlate", all: "dsLiftFrame", bottom: null });
      const spawn = { x: 2.5, z: 5.2, yaw: FACE.n };
      R._boxes.push({ x0: x + 0.1, z0: z, x1: x + 0.9, z1: z + 1.15, to: "celadon_mart_elevator", spawn });
      R.warps.push({ test: p => p.x > x + 0.1 && p.x < x + 0.9 && p.z < z + 1.15, to: "celadon_mart_elevator", spawn, fx: "fade" });
      const wt = R.warps[R.warps.length - 1], t0 = wt.test; wt.test = p => { const ok = t0(p); if (ok) window.CEL_LIFT.from = id; return ok; };
      R.item("Elevator", "Orange doors, the arrows lit over them, the floor's plate in front. Walk in and pick a floor.");
    }

    // The people, trainers and clerks, where FireRed has them (on the GAME CORNER's stools, sitting).
    for (const o of F.objs) {
      const x = o.x + 0.5, z = o.z + 0.5, f = FACEOF[o.face] || FACEOF["FACE_" + (o.dir || "").toUpperCase()] || "s";
      const seated = gc && at(o.x, o.z) === "o";
      if (o.kind === "trainer") R.npcs.push({ name: o.name, key: o.key, role: "trainer", x, z, yaw: FACE[f], look: { skin: o.skin }, talk: () => [{ text: "Hey! What are you doing here?" }] });
      else if (o.kind === "clerk") R.npcs.push({ name: "Clerk", key: o.key, role: "clerk", x, z, yaw: FACE[f], look: { skin: "clerk" }, talk: () => [{ text: "Hi there! May I help you?" }] });
      else R.npcs.push({ name: o.name, key: o.key, x, z, yaw: FACE[f], seated, look: { skin: o.skin }, talk: () => o.lines.map(text => ({ text })) });
    }

    if (lift) {
      // FireRed's elevator (SILPH CO.'s kind): its buttons by the doors; out onto the floor you picked.
      for (const s of F.signs.filter(s => s.elevator)) {
        const fx = s.x, fz = s.z;
        G.box(fx + 0.2, 0.95, fz + 1, fx + 0.8, 1.75, fz + 1.04, { s: "silphButtons", all: "silphSkirt", n: null });
        R.act([fx - 0.2, fz + 0.6, fx + 1.4, fz + 2.0], { label: "Use", lines: ["The elevator's floor buttons. Which floor?"], use: "lift", run: async say => {
          const floors = DS.slice(0, 5).reverse();
          const pick = await say([{ text: "Which floor?", ask: floors.map(f => CELADON[f].floor).concat(["CANCEL"]) }]);
          if (pick != null && pick < floors.length) { window.CEL_LIFT.to = floors[pick]; await say([{ text: "The elevator hums... It's stopped at " + CELADON[floors[pick]].floor + "." }]); }
        } });
      }
      G.box(1, 0, 6.6, 4, 2.0, 6.7, { n: "silphLiftDoor", all: null });
      R._boxes.push({ x0: 1, z0: 6.2, x1: 4, z1: 7.5, lift: true });
      const lw = { test: (p, input) => input.down && p.z > 6.2 && p.x > 1 && p.x < 4, fx: "fade" };
      Object.defineProperty(lw, "to", { get: () => window.CEL_LIFT.to || window.CEL_LIFT.from });
      Object.defineProperty(lw, "spawn", { get: () => {
        const f = window.CEL_LIFT.to || window.CEL_LIFT.from, e = CELADON[f].warps.find(w => w.kind === "elevator");
        window.CEL_LIFT.from = f; window.CEL_LIFT.to = null;
        return { x: e.x + 0.5, z: e.z + 1.5, yaw: FACE.s };
      } });
      R.warps.push(lw);
      R.spawn = { x: 2.5, z: 5.2, yaw: FACE.n };
      R.item("Elevator", "The DEPT. STORE's elevator: brushed steel. Press the buttons for a floor, then walk out.");
    } else if (F.warps.some(w => w.kind === "exit")) {
      // The way out: the mats by the front doors (two on the DEPT. STORE's 1F).
      const xs = F.warps.filter(w => w.kind === "exit").map(w => w.x).sort((a, b) => a - b);
      const runs = []; for (const x of xs) { const r = runs[runs.length - 1]; if (r && x <= r[1] + 1) r[1] = x; else runs.push([x, x]); }
      // (FireRed's exits are its doormat's row; the mat runs the width of the doorway below.)
      const mats = [];
      for (let x = 0; x < W; x++) if (at(x, D - 1) === "X") { const r = mats[mats.length - 1]; if (r && x === r[1] + 1) r[1] = x; else mats.push([x, x]); }
      for (const [a, b] of mats.length ? mats : runs) doormat(R, a, b + 1, { to: "town", mat: "matHome" });
      const m0 = (mats.length ? mats : runs)[0];
      R.spawn = { x: (m0[0] + m0[1] + 1) / 2, z: D - 1.6, yaw: FACE.n };
    } else {
      const st = F.warps.find(w => w.kind === "stairs"), ss = st && stairOf(F, st.x, st.z);
      R.spawn = ss ? { x: st.x - ss.dir[0] + 0.5, z: st.z - ss.dir[1] + 0.5, yaw: ss.dir[0] < 0 ? FACE.e : ss.dir[0] > 0 ? FACE.w : FACE.s } : { x: W / 2, z: D / 2, yaw: FACE.s };
    }
    R.item(gc ? "Game Corner" : "Celadon Dept. Store", gc ? "FireRed's, tile for tile: the gold carpet, sky-blue walls, the slot machines and stools, the violet stage."
      : "FireRed's floor, tile for tile: the lilac hall with the stairs and elevator, the orange-tiled shop floor, white counters with green trims.");
    return R;
  }
  window.CEL_LIFT = { from: "celadon_mart_1f", to: null };
  for (const id of DS.concat(GCR)) BUILD[id] = () => build(id);
})();
