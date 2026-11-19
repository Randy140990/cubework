// Exports the UI Viewer's pictures for the mod: its icons, the battle command pictures, the eight badges and the two
// wordmarks, as PNGs in assets/retromon/textures/gui/v/. Run against the built viewer page:
//   node icons.js <url of ui-viewer.html> <three.min.js> <out dir>
const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
(async () => {
  const [url, three, out] = process.argv.slice(2);
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 800, height: 800 }, deviceScaleFactor: 1 });
  await p.route('**/three@0.128.0/build/three.min.js', r => r.fulfill({ body: fs.readFileSync(three), contentType: 'application/javascript' }));
  await p.route('https://fonts.googleapis.com/**', r => r.fulfill({ body: '', contentType: 'text/css' }));
  await p.goto(url); await p.waitForTimeout(1500);
  const svgs = await p.evaluate(() => {
    const o = {};
    for (const [k, v] of Object.entries(ICON)) if (typeof v === 'string') o['icon_' + k] = v;
    for (const [k, v] of Object.entries(CMD_ICON)) o['cmd_' + (k.startsWith('POK') ? 'pokemon' : k.toLowerCase())] = v;
    for (let i = 0; i < 8; i++) o['badge_' + i] = badgeSvg(i);
    return o;
  });
  for (const [name, svg] of Object.entries(svgs)) {
    const png = await p.evaluate(async ([svg, size]) => {
      const s = svg.replace('<svg ', `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" `);
      const img = new Image(); img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(s); await img.decode();
      const c = document.createElement('canvas'); c.width = size; c.height = size; c.getContext('2d').drawImage(img, 0, 0, size, size);
      return c.toDataURL('image/png');
    }, [svg, 128]);
    fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(png.split(',')[1], 'base64'));
  }
  // The wordmarks: RETROMON in our colours, POKéMON in the night look's gold and blue.
  const marks = await p.evaluate(() => ({
    wordmark_ours: wordmark('RETROMON', {}),
    wordmark_theirs: wordmark('POKéMON', { top: [255, 236, 120], bottom: [240, 170, 40], side: [40, 60, 140], line: [12, 18, 48] }),
  }));
  for (const [name, d] of Object.entries(marks)) fs.writeFileSync(path.join(out, name + '.png'), Buffer.from(d.split(',')[1], 'base64'));
  console.log(Object.keys(svgs).length + 2, 'pictures');
  await b.close();
})();
