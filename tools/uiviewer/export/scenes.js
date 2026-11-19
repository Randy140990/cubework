// Exports the UI Viewer's backdrops and its title scenes for the mod (assets/retromon/textures/gui/v/):
//  - bg_<name>.png: each screen backdrop (its CSS gradients, rays, stars and moon), 480 by 270, drawn smooth in game;
//  - title_ours.png / title_theirs.png: the title's 3D scene (our creatures on the hill, or Charizard at night), 48
//    frames at 12 a second, each 256 by 144 (the viewer draws it as pixel art, 5 screen pixels to one), 8 across.
//   node scenes.js <url of ui-viewer.html> <three.min.js> <out dir>
const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
const BG = {
  sky: 'radial-gradient(ellipse at 30% 20%, #F6FFFB 0%, #BFEFE0 38%, #7FD3C2 70%, #3F9C8A 100%)',
  dream: 'radial-gradient(circle at 50% 42%, #FFFFFF 0%, #E3F8FA 20%, #9EDCEB 48%, #4C9CC7 78%, #2E5F96 100%)',
  night: 'radial-gradient(ellipse at 50% 40%, #1D5A52 0%, #123C38 45%, #071816 100%)',
  battle: 'linear-gradient(180deg,#7EC6EC 0%,#BFE6F7 55%,#E8F7FB 70%)',
  clothes: 'radial-gradient(ellipse at 70% 45%, #FFFFFF 0%, #FFF0E0 35%, #F6C99A 70%, #D98A52 100%)',
  card: 'linear-gradient(160deg,#BFD8F7,#4A8FE0 60%,#2F5D96)',
  maker: 'radial-gradient(ellipse at 70% 45%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)',
  name: 'radial-gradient(ellipse at 50% 45%, #FFFFFF 0%, #DFF6EE 35%, #9EDCC9 70%, #4FA88E 100%)',
};
const TITLE_DAY = `<div style="position:absolute;inset:0;background:linear-gradient(180deg,#5FB0E8 0%,#9ED6F5 50%,#D6F0FB 64%,#9ED68A 64.2%,#6DB85B 100%)"></div><div style="position:absolute;inset:0;background:repeating-conic-gradient(from 0deg at 50% 0%, rgba(255,255,255,.12) 0 5deg, transparent 5deg 15deg)"></div>`;
const TITLE_NIGHT = `<div style="position:absolute;inset:0;background:radial-gradient(ellipse at 50% 30%, #2A3F7A 0%, #142454 45%, #070D26 100%)"></div><div style="position:absolute;inset:0;background:radial-gradient(circle at 12% 18%,#fff 0 1.5px,transparent 2.5px),radial-gradient(circle at 78% 12%,#fff 0 2px,transparent 3px),radial-gradient(circle at 64% 34%,#cfe 0 1.5px,transparent 2.5px),radial-gradient(circle at 30% 42%,#fff 0 1px,transparent 2px),radial-gradient(circle at 88% 46%,#fff 0 1.5px,transparent 2.5px);background-size:320px 260px"></div><div style="position:absolute;right:120px;top:70px;width:90px;height:90px;border-radius:50%;background:radial-gradient(circle at 40% 40%,#FFF8DC,#E8DDB0);box-shadow:0 0 40px rgba(255,248,220,.5)"></div>`;
const PROF_SPARKS = `<div style="position:absolute;inset:0;background:radial-gradient(circle at 20% 30%,rgba(255,255,255,.6) 0 2px,transparent 3px),radial-gradient(circle at 80% 20%,rgba(255,255,255,.7) 0 2px,transparent 3px),radial-gradient(circle at 70% 70%,rgba(255,255,255,.5) 0 2px,transparent 3px),radial-gradient(circle at 30% 80%,rgba(255,255,255,.6) 0 2px,transparent 3px);background-size:240px 200px"></div>`;
(async () => {
  const [url, three, out] = process.argv.slice(2);
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch();
  const blur = n => fs.writeFileSync(path.join(out, n + '.png.mcmeta'), JSON.stringify({ texture: { blur: true, clamp: true } }));
  // Backdrops.
  if (!process.env.ONLY_TITLE) {
    const p = await b.newPage({ viewport: { width: 1280, height: 720 } });
    const shot = async (name, css, inner) => {
      await p.setContent(`<html><body style="margin:0"><div id="b" style="position:relative;width:1280px;height:720px;background:${css || '#000'}">${inner || ''}</div></body></html>`);
      const buf = await (await p.$('#b')).screenshot();
      const small = await p.evaluate(async d => { const im = new Image(); im.src = d; await im.decode(); const c = document.createElement('canvas'); c.width = 480; c.height = 270; const x = c.getContext('2d'); x.imageSmoothingQuality = 'high'; x.drawImage(im, 0, 0, 480, 270); return c.toDataURL('image/png'); }, 'data:image/png;base64,' + buf.toString('base64'));
      fs.writeFileSync(path.join(out, 'bg_' + name + '.png'), Buffer.from(small.split(',')[1], 'base64')); blur('bg_' + name);
    };
    for (const [n, css] of Object.entries(BG)) await shot(n, css);
    await shot('dream_sparks', BG.dream, PROF_SPARKS);
    await shot('title_day', null, TITLE_DAY);
    await shot('title_night', null, TITLE_NIGHT);
    await p.close();
  }
  // The title's scene, frame by frame on a stopped clock.
  for (const look of process.env.ONLY_BG ? [] : ['ours', 'theirs']) {
    const ctx = await b.newContext({ viewport: { width: 1600, height: 1000 } });
    const p = await ctx.newPage();
    await p.route('**/three@0.128.0/build/three.min.js', r => r.fulfill({ body: fs.readFileSync(three), contentType: 'application/javascript' }));
    await p.route('https://fonts.googleapis.com/**', r => r.fulfill({ body: '', contentType: 'text/css' }));
    await p.addInitScript(l => { try { localStorage.setItem('rm-ui-look', l); } catch (e) {} }, look);
    await p.clock.install();
    await p.goto(url + '#title');
    await p.evaluate(() => { const s = document.querySelector('#stage'); s.style.width = '1280px'; s.style.height = '720px'; s.style.maxWidth = 'none'; s.style.aspectRatio = 'auto'; });
    for (let i = 0; i < 40; i++) { await p.clock.runFor(100); await p.waitForTimeout(60); }
    await p.evaluate(() => { document.querySelector('#bg').style.visibility = 'hidden'; document.querySelector('#ui').style.visibility = 'hidden'; const st = document.querySelector('#stage'); document.body.appendChild(st); for (const c of [...document.body.children]) if (c !== st && c.tagName !== 'SCRIPT') c.style.display = 'none'; document.documentElement.style.background = 'transparent'; document.body.style.background = 'transparent'; st.style.position = 'fixed'; st.style.left = '0'; st.style.top = '0'; st.style.background = 'transparent'; st.style.borderRadius = '0'; st.style.boxShadow = 'none'; st.style.border = '0'; resize3(); });
    const N = 48, FW = 256, FH = 144, COLS = 8;
    const frames = [];
    for (let i = 0; i < N; i++) {
      await p.clock.runFor(1000 / 12); await p.waitForTimeout(40);
      frames.push((await (await p.$('#stage')).screenshot({ omitBackground: true })).toString('base64'));
    }
    const sheet = await p.evaluate(async ([frames, FW, FH, COLS]) => {
      const c = document.createElement('canvas'); c.width = FW * COLS; c.height = FH * Math.ceil(frames.length / COLS); const x = c.getContext('2d'); x.imageSmoothingEnabled = false;
      for (let i = 0; i < frames.length; i++) {
        const im = new Image(); im.src = 'data:image/png;base64,' + frames[i]; await im.decode();
        // The scene is drawn 5 to 1: take each block's middle pixel.
        const t = document.createElement('canvas'); t.width = im.width; t.height = im.height; const tx = t.getContext('2d'); tx.drawImage(im, 0, 0);
        const d = tx.getImageData(0, 0, im.width, im.height), sx = im.width / FW, sy = im.height / FH, o = x.createImageData(FW, FH);
        for (let y = 0; y < FH; y++) for (let xx = 0; xx < FW; xx++) { const si = (Math.floor((y + 0.5) * sy) * im.width + Math.floor((xx + 0.5) * sx)) * 4, di = (y * FW + xx) * 4; for (let k = 0; k < 4; k++) o.data[di + k] = d.data[si + k]; }
        x.putImageData(o, (i % COLS) * FW, Math.floor(i / COLS) * FH);
      }
      return c.toDataURL('image/png');
    }, [frames, FW, FH, COLS]);
    fs.writeFileSync(path.join(out, 'title_' + look + '.png'), Buffer.from(sheet.split(',')[1], 'base64'));
    await ctx.close();
  }
  console.log('done');
  await b.close();
})();
