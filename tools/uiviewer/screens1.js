// ---------- a screen's controls: whatever's on top gets the keys; text boxes and choices in turn ----------
let GEN = 0;
function ctl() {
  const stack = [], gen = ++GEN;
  const C = {
    alive: () => gen === GEN,
    push(w) { stack.push(w); return w; }, pop(w) { const i = stack.lastIndexOf(w); if (i >= 0) stack.splice(i, 1); },
    key(k) { const w = stack[stack.length - 1]; return w && w.key ? w.key(k) : false; },
    say(lines, o = {}) { const tb = textBox(lines, Object.assign({}, C.sayAt || {}, o)); C.push(tb); return tb.done.then(v => { C.pop(tb); return v; }); },
    choose(items, o = {}) { return new Promise(res => { const cb = choiceBox(items, Object.assign({}, o, { onPick: i => { C.pop(cb); cb.el.remove(); res(i); }, onBack: o.back === false ? null : () => { C.pop(cb); cb.el.remove(); res(-1); } })); C.push(cb); }); },
    wait: ms => new Promise(r => later(ms, r)),
  };
  return C;
}
function header(icon, text, right) {
  const e = box(28, 22, null, null, "g-panel soft", `<div style="display:flex;align-items:center;gap:12px;padding:8px 22px 8px 12px"><span style="width:42px;height:42px;display:grid;place-items:center">${icon || ""}</span><span class="g-title" style="font-size:28px">${esc(T(text))}</span>${right ? `<span style="margin-left:18px" class="g-label">${right}</span>` : ""}</div>`);
  e.style.borderRadius = "999px"; e.querySelector("svg") && (e.querySelector("svg").style.width = "40px");
  return e;
}
// Each screen on its own bright backdrop, as Let's Go's are: sky, lime, peach, with soft rays from a corner.
const SKY = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #E2F4FF 32%, #A6D8F7 66%, #6AAEE6 100%)";
const LIME = "radial-gradient(ellipse at 25% 15%, #FFFFF2 0%, #F4FAB8 35%, #CDEB6E 70%, #96C73A 100%)";
const PEACH = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #FFEDE0 35%, #FFC9A6 70%, #F2946A 100%)";
const MINT = "radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #E3FAEF 35%, #A9EBCB 70%, #5CC79A 100%)";
const DREAM = "radial-gradient(circle at 50% 42%, #FFFFFF 0%, #E3F8FA 20%, #9EDCEB 48%, #4C9CC7 78%, #2E5F96 100%)";
const NIGHT = "radial-gradient(ellipse at 50% 40%, #5FA8E8 0%, #2F64B0 50%, #1A3470 100%)";

// ===================== Start =====================
screen("splash", "Start", "Opening", {
  what: "The first thing you see: just the studio's name, then the title.",
  mod: "IntroScreen: 'INSID3 TH3 CUB3, presents', no star.",
  make() {
    backdrop("#000");
    const e = box(0, 0, 1280, 720, "", `<div style="position:absolute;inset:0;display:grid;place-content:center;text-align:center;gap:10px">
      <div class="g-title" style="font-size:64px;letter-spacing:10px;color:#F4F6FF">INSID3 TH3 CUB3</div><div style="font-size:24px;letter-spacing:6px;color:var(--g-muted);font-weight:700">presents</div></div>`);
    e.style.opacity = 0; e.style.transition = "opacity .9s ease";
    later(80, () => e.style.opacity = 1); later(2300, () => e.style.opacity = 0); later(3300, () => go("title"));
    return { key: k => { if (k === "a" || k === "b") go("title"); return true; } };
  }
});
// The title's painting: soft and smooth (an SVG drawn once), with the grass swaying, clouds drifting, motes of light.
function titleArt() {
  let seed = 11; const r = () => (seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296;
  const blobs = (cx, cy, w, hgt, n, fill, rMin, rMax) => { let o = ""; for (let i = 0; i < n; i++) { const x = cx + (r() - 0.5) * w, y = cy + (r() - 0.5) * hgt, rr = rMin + r() * (rMax - rMin); o += `<circle cx="${x.toFixed(0)}" cy="${y.toFixed(0)}" r="${rr.toFixed(0)}" fill="${fill}"/>`; } return o; };
  // A canopy: dark underneath, mid, then sunlit clumps up top where the light falls.
  const canopy = (cx, cy, w, hgt, k = 1) => blobs(cx, cy + hgt * 0.12, w, hgt * 0.8, Math.round(22 * k), "url(#cDk)", 40 * k, 90 * k) + blobs(cx, cy, w * 0.92, hgt * 0.7, Math.round(20 * k), "url(#cMd)", 34 * k, 78 * k)
    + blobs(cx + w * 0.12, cy - hgt * 0.16, w * 0.7, hgt * 0.45, Math.round(14 * k), "url(#cLt)", 22 * k, 52 * k);
  // Long grass: curved blades, short at the back, long at the front.
  let grass = ""; for (let i = 0; i < 300; i++) { const x = r() * 1280, base = 480 + Math.pow(r(), 0.55) * 250, hgt = 14 + r() * (base > 650 ? 80 : base > 560 ? 44 : 24), lean = (r() - 0.4) * 26, g = ["#4E9E3A", "#5DB345", "#79C653", "#3F8A34", "#8ED65E"][Math.floor(r() * 5)];
    grass += `<path d="M${(x - 3).toFixed(0)} ${base.toFixed(0)} Q${(x + lean * 0.4).toFixed(0)} ${(base - hgt * 0.6).toFixed(0)} ${(x + lean).toFixed(0)} ${(base - hgt).toFixed(0)} Q${(x + lean * 0.3 + 3).toFixed(0)} ${(base - hgt * 0.5).toFixed(0)} ${(x + 4).toFixed(0)} ${base.toFixed(0)}Z" fill="${g}"/>`; }
  let flowers = ""; for (let i = 0; i < 46; i++) { const x = r() * 1280, y = 540 + r() * 170, c = ["#FFFFFF", "#FFF3A6", "#FF8FA8", "#FFD45A", "#E84A4A"][Math.floor(r() * 5)], s = 3 + r() * 5 + (y > 640 ? 3 : 0);
    flowers += `<g transform="translate(${x.toFixed(0)} ${y.toFixed(0)})">${[0, 72, 144, 216, 288].map(a => `<circle cx="${(Math.cos(a * Math.PI / 180) * s * 0.9).toFixed(1)}" cy="${(Math.sin(a * Math.PI / 180) * s * 0.9).toFixed(1)}" r="${(s * 0.62).toFixed(1)}" fill="${c}"/>`).join("")}<circle r="${(s * 0.42).toFixed(1)}" fill="#F2B01E"/></g>`; }
  let motes = ""; for (let i = 0; i < 18; i++) motes += `<circle cx="${(r() * 1280).toFixed(0)}" cy="${(120 + r() * 480).toFixed(0)}" r="${(2 + r() * 3).toFixed(1)}" fill="#FFFDE8" style="animation:tmote ${(4 + r() * 4).toFixed(1)}s ease-in-out ${(-r() * 4).toFixed(1)}s infinite"/>`;
  return `<svg viewBox="0 0 1280 720" width="1280" height="720" style="position:absolute;inset:0;display:block"><defs>
    <radialGradient id="tSun" cx="0.9" cy="0.02" r="0.6"><stop offset="0" stop-color="#FFFFF4" stop-opacity="1"/><stop offset=".12" stop-color="#FFF8D6" stop-opacity=".85"/><stop offset=".35" stop-color="#FFF4CC" stop-opacity=".25"/><stop offset="1" stop-color="#FFF4CC" stop-opacity="0"/></radialGradient>
    <linearGradient id="cDk" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2F7A3A"/><stop offset="1" stop-color="#1F5A30"/></linearGradient>
    <linearGradient id="cMd" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5EB64C"/><stop offset="1" stop-color="#3B8E3E"/></linearGradient>
    <linearGradient id="cLt" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#C4EE7A"/><stop offset="1" stop-color="#7CCB56"/></linearGradient>
    <linearGradient id="tFar" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#A6DDB6"/><stop offset="1" stop-color="#7FC497"/></linearGradient>
    <linearGradient id="tMeadow" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#B3E27A"/><stop offset=".35" stop-color="#86CB55"/><stop offset="1" stop-color="#54A83E"/></linearGradient>
    <linearGradient id="tPath" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F4E2B0"/><stop offset="1" stop-color="#DCC08A"/></linearGradient>
    <linearGradient id="tRock" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#EDE7DB"/><stop offset=".5" stop-color="#BDB4A6"/><stop offset="1" stop-color="#8E8576"/></linearGradient>
    <linearGradient id="tTrunk" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#6A4A30"/><stop offset=".55" stop-color="#8A6440"/><stop offset="1" stop-color="#5A3E28"/></linearGradient>
    <filter id="tb1" x="-10%" y="-10%" width="120%" height="120%"><feGaussianBlur stdDeviation="1.4"/></filter><filter id="tb3" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="4"/></filter>
    <filter id="tb8" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="9"/></filter><filter id="tb16" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="18"/></filter></defs>
    <style>@keyframes tsway{from{transform:skewX(-1.5deg)}to{transform:skewX(2deg)}}@keyframes tcloud{from{transform:translateX(0)}to{transform:translateX(70px)}}@keyframes tmote{0%,100%{opacity:.1;transform:translateY(0)}50%{opacity:.9;transform:translateY(-14px)}}
      .tsw{transform-box:fill-box;transform-origin:50% 100%;animation:tsway 3.6s ease-in-out infinite alternate}</style>
    <rect width="1280" height="720" fill="url(#tSun)"/>
    <g filter="url(#tb8)" opacity=".92" style="animation:tcloud 38s ease-in-out infinite alternate">${blobs(420, 220, 300, 34, 8, "#FFFFFF", 24, 46)}${blobs(820, 170, 320, 36, 9, "#FFFFFF", 22, 44)}</g>
    <g filter="url(#tb8)">${blobs(640, 395, 1500, 80, 44, "url(#tFar)", 44, 100)}</g>
    <g filter="url(#tb3)" opacity=".95">${blobs(600, 432, 1000, 46, 20, "#6DB86A", 30, 60)}</g>
    <path d="M0 470 C 260 432 520 452 700 462 S 1100 442 1280 452 V720 H0Z" fill="url(#tMeadow)"/>
    <path d="M0 470 C 260 432 520 452 700 462 S 1100 442 1280 452 V506 C 900 490 500 504 0 524Z" fill="#E6F7B0" opacity=".45" filter="url(#tb3)"/>
    <path d="M612 468 C 642 520 566 592 470 720 H770 C 724 600 694 520 664 466Z" fill="url(#tPath)" opacity=".85"/>
    <g filter="url(#tb1)"><rect x="268" y="210" width="56" height="290" rx="18" fill="url(#tTrunk)"/><path d="M292 320 Q 250 270 206 256" stroke="#6A4A30" stroke-width="16" fill="none" stroke-linecap="round"/>${canopy(250, 150, 560, 280)}</g>
    <g filter="url(#tb1)"><rect x="1168" y="250" width="48" height="240" rx="14" fill="url(#tTrunk)" opacity=".95"/>${canopy(1170, 140, 420, 300, 1.05)}</g>
    <g filter="url(#tb1)">${blobs(880, 468, 280, 28, 10, "#3F8E3E", 24, 46)}${blobs(900, 452, 240, 18, 8, "#6DBB55", 18, 34)}${blobs(120, 486, 320, 28, 10, "#3F8E3E", 26, 50)}</g>
    <path d="M44 644 C 32 566 112 514 212 524 C 302 532 334 604 304 654 C 244 694 94 694 44 644Z" fill="url(#tRock)"/><path d="M94 566 C 134 534 204 532 244 554" stroke="#F8F4EC" stroke-width="10" fill="none" stroke-linecap="round" opacity=".7"/>
    <g class="tsw">${grass}</g>${flowers}
    <g filter="url(#tb16)" opacity=".92">${blobs(70, 706, 260, 60, 8, "#2E7A34", 50, 92)}${blobs(1250, 706, 240, 60, 7, "#3A8A3A", 50, 92)}</g>
    <rect width="1280" height="720" fill="url(#tSun)" opacity=".5"/>${motes}</svg>`;
}
screen("title", "Start", "Title screen", {
  scene: "title", pixel: () => LOOK === "theirs" ? 5 : 0,
  what: "The title, as Let's Go's, smooth (no pixels): RETROMON big at the top left with its own tag, A FAN-MADE ADVENTURE, over a painted meadow: blue sky with the sun's glare top right, hazy trees far off, a big tree on the left and leafy canopy on the right, a path, a rock, long grass swaying and flowers, blurred leaves right at the front. Your partner stands big on the right, looking out at you. Press SPACE, in white words. COBBLEMON look: the old title, the pixel Charizard against the night sky, reading POKéMON.",
  tags: ["New look: Let's Go's title"], mod: "IntroScreen: the pixel Charizard against the night sky, RETROMON, PRESS START. To come: this painted title.",
  make() {
    const night = LOOK === "theirs";
    if (night) {
      backdrop("radial-gradient(ellipse at 50% 30%, #2A3F7A 0%, #142454 45%, #070D26 100%)");
      add(h("div", "", `<div style="position:absolute;inset:0;background:radial-gradient(circle at 12% 18%,#fff 0 1.5px,transparent 2.5px),radial-gradient(circle at 78% 12%,#fff 0 2px,transparent 3px),radial-gradient(circle at 64% 34%,#cfe 0 1.5px,transparent 2.5px),radial-gradient(circle at 30% 42%,#fff 0 1px,transparent 2px),radial-gradient(circle at 88% 46%,#fff 0 1.5px,transparent 2.5px);background-size:320px 260px"></div><div style="position:absolute;right:120px;top:70px;width:90px;height:90px;border-radius:50%;background:radial-gradient(circle at 40% 40%,#FFF8DC,#E8DDB0);box-shadow:0 0 40px rgba(255,248,220,.5)"></div>`));
    } else {
      // As Let's Go's title: a painted meadow in soft light (no pixels): sky and the sun's glare top right, hazy trees far
      // off, a big tree on the left and leafy canopy behind on the right, a sunlit meadow with a path and a rock, long
      // grass and flowers, and blurred leaves right at the front, out of focus. Your partner stands big on the right.
      backdrop("linear-gradient(180deg,#4AAEEC 0%,#8ED3F7 34%,#DDF4FF 56%,#B9E39A 60%,#7CC456 100%)");
      bgAdd(titleArt());
    }
    if (night) box(0, 34, 1280, null, "", `<div style="text-align:center"><img alt="POKéMON" src="${wordmark("POKéMON", { top: [255, 236, 120], bottom: [240, 170, 40], side: [40, 60, 140], line: [12, 18, 48] })}" style="max-width:1080px;image-rendering:pixelated;filter:drop-shadow(0 10px 0 rgba(0,0,0,.18))"></div>`);
    // RETROMON in round, heavy letters on a gentle arch (tools/wordmark.py), with its own edge and shadow.
    // RETROMON big, upper left, as Let's Go's logo sits, with its own tag under it: A FAN-MADE ADVENTURE.
    else box(70, 54, 760, null, "", `<style>@keyframes tlogo{from{transform:translateY(-14px) scale(.96);opacity:0}to{transform:none;opacity:1}}</style><div style="position:relative;animation:tlogo .8s cubic-bezier(.2,.9,.3,1.2) both">
      <img alt="RETROMON" src="${wordmark("RETROMON")}" style="width:760px;height:138px;display:block;filter:drop-shadow(0 8px 0 rgba(255,255,255,.65)) drop-shadow(0 14px 18px rgba(20,60,90,.28))">
      <div style="position:absolute;left:300px;top:128px;transform:rotate(-4deg);display:inline-flex;align-items:center;gap:12px;padding:8px 26px 8px 12px;border-radius:999px;background:#fff;box-shadow:0 0 0 4px #26304A,0 8px 0 rgba(38,48,74,.35),0 12px 22px rgba(20,60,90,.25)">
        <svg viewBox="0 0 24 24" width="34" height="34"><path d="M12 2l2.6 6.6L21.5 9l-5.2 4.6L18 21l-6-3.6L6 21l1.7-7.4L2.5 9l6.9-.4z" fill="#FFC93C" stroke="#E5583E" stroke-width="1.6" stroke-linejoin="round"/></svg>
        <span style="font-family:Fredoka,system-ui,sans-serif;font-weight:700;font-size:30px;letter-spacing:1.5px;color:#26304A;white-space:nowrap">${esc(T("A FAN-MADE ADVENTURE"))}</span></div></div>`);
    // Which key, said plainly (Let's Go's "Press the A Button"): white words with a soft edge, SPACE in its own key cap.
    if (night) box(0, 612, 1280, null, "", `<div style="text-align:center"><span class="g-title g-blink" style="display:inline-block;padding:8px 34px;border-radius:999px;background:#fff;color:#26304A;font-size:30px;letter-spacing:6px">${esc(T("PRESS START"))}</span></div>`);
    else box(330, 626, 560, null, "", `<style>@keyframes tpulse{0%,100%{opacity:1}50%{opacity:.55}}</style><div style="text-align:center;animation:tpulse 2s ease-in-out infinite;font-family:Fredoka,system-ui,sans-serif;font-weight:700;font-size:36px;letter-spacing:1px;color:#fff;-webkit-text-stroke:7px rgba(38,48,74,.55);paint-order:stroke fill;text-shadow:0 4px 12px rgba(20,40,60,.35)">${T("Press {0}").split("{0}").map(esc).join(`<span style="display:inline-grid;place-items:center;vertical-align:middle;height:44px;margin:0 8px;padding:0 20px;border-radius:12px;background:linear-gradient(180deg,#fff,#E6ECF6);color:#26304A;-webkit-text-stroke:0;font-size:20px;letter-spacing:3px;box-shadow:0 0 0 3px rgba(38,48,74,.55),inset 0 -4px 0 rgba(38,48,74,.15)">SPACE</span>`)}</div>`);
    box(28, 680, 400, null, "", `<div style="font-weight:800;font-size:15px;color:${night ? "#DDE6FF" : "#123C38"};opacity:.8">INSID3 TH3 CUB3</div>`);
    box(1080, 680, 180, null, "", `<div style="text-align:right;font-weight:800;font-size:15px;color:${night ? "#DDE6FF" : "#123C38"};opacity:.8">Version 0.28</div>`);
    return { key: k => { if (k === "a") go("saves"); return true; } };
  }
});
const LANGS = [["English", "English"], ["Español", "Spanish"], ["Français", "French"], ["Deutsch", "German"], ["Italiano", "Italian"], ["日本語", "Japanese"], ["한국어", "Korean"], ["简体中文", "Chinese, simplified"], ["繁體中文", "Chinese, traditional"]];
screen("saves", "Start", "Continue or new game", {
  scene: "creator", sceneOpts: () => ({ side: -1.6 }),
  what: "Your saves as cards: who, where, how far. Pick one to carry on, or an empty slot to start again.",
  mod: "IntroScreen's save slots: rounded cards with a picture, a SLOT pill and DELETE.",
  try: ["Up and down between slots, A to load or start, B for the title."],
  make() {
    swirlBackdrop("sky", { lx: 85, ly: 10 }); const C = ctl();
    const slots = [{ used: true }, {}, {}];
    let sel = 0;
    const wrap = box(70, 90, 640, null, "", "");
    const draw = () => {
      wrap.innerHTML = slots.map((s, i) => s.used ? `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:18px 22px;display:grid;grid-template-columns:150px 1fr;gap:20px;${i === sel ? "box-shadow:0 0 0 5px #FFC93C,0 14px 36px rgba(30,40,84,.3)" : ""}">
          <div style="border-radius:16px;background:linear-gradient(180deg,#9ED8F2,#BFEFE0 55%,#6DB257 56%,#5FA24C);position:relative;overflow:hidden">
            <div style="position:absolute;left:18px;bottom:22px;width:56px;height:40px;background:#F2E8D2;border-radius:4px"></div><div style="position:absolute;left:12px;bottom:58px;width:68px;height:22px;background:#C9493F;border-radius:6px 6px 0 0"></div>
            <div style="position:absolute;right:18px;bottom:16px;width:22px;height:44px;border-radius:4px;background:${hex(colourOf(G.outfit.topc))}"></div><div style="position:absolute;right:20px;bottom:58px;width:18px;height:18px;border-radius:3px;background:${hex(skinRGB(G.outfit.skin))}"></div></div>
          <div style="display:grid;gap:6px"><div style="display:flex;align-items:center;gap:12px"><span style="padding:2px 12px;border-radius:999px;background:linear-gradient(180deg,#FF9473,#E5525C);color:#fff;font-weight:900;font-size:14px;letter-spacing:1px">${esc(T("SLOT {0}", i + 1))}</span><span class="g-title" style="font-size:30px">${esc(G.name)}</span></div>
            <div style="display:grid;grid-template-columns:auto 1fr;gap:2px 18px;font-size:19px;font-weight:800"><span class="g-label">${esc(T("Place"))}</span><span>${esc(G.place)}</span><span class="g-label">${esc(T("Pokédex"))}</span><span>${G.caught}</span><span class="g-label">${esc(T("Badges"))}</span><span>${G.badges}</span><span class="g-label">${esc(T("Play time"))}</span><span>${G.time}</span></div></div>
          <span style="position:absolute;right:16px;top:14px;padding:2px 12px;border-radius:999px;border:2px solid #F3A6AC;color:#D9404A;font-weight:900;font-size:13px">${esc(T("DELETE"))}</span></div>` : `
        <div class="g-panel" style="position:relative;margin-bottom:18px;padding:22px 24px;display:flex;align-items:center;gap:16px;${i === sel ? "box-shadow:0 0 0 5px #FFC93C,0 14px 36px rgba(30,40,84,.3)" : "opacity:.92"}">
          <span style="padding:2px 12px;border-radius:999px;background:#EEF2FB;color:var(--g-muted);font-weight:900;font-size:14px;letter-spacing:1px">${esc(T("SLOT {0}", i + 1))}</span><span class="g-title" style="font-size:28px">${esc(T("NEW GAME"))}</span><span style="margin-left:auto;font-size:17px;color:var(--g-muted);font-weight:800">${esc(T("Empty"))}</span></div>`).join("") +
        `<div class="g-item${sel === 3 ? " on" : ""}" style="display:inline-flex;margin-top:4px;background:${sel === 3 ? "" : "rgba(255,255,255,.85)"}">${ICON.cog.replace("<svg", '<svg style="width:26px;height:26px"')}<span>${esc(T("OPTIONS"))}</span></div>`;
      [...wrap.children].forEach((c, i) => c.onclick = () => { sel = i; draw(); pick(); });
    };
    const pick = () => { if (sel === 0) go("loading"); else if (sel === 3) go("options"); else go("prof"); };
    draw();
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
    return { key: k => { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : 3)) % 4; draw(); return true; } if (k === "a") { pick(); return true; } if (k === "b") { go("title"); return true; } return false; } };
  }
});
const OPTS = [
  ["TEXT SPEED", "textSpeed", ["SLOW", "MID", "FAST"], "How quickly words appear in text boxes."],
  ["MUSIC", "music", ["OFF", "LOW", "MID", "HIGH", "MAX"], "How loud the music plays."],
  ["SOUND", "sound", ["OFF", "LOW", "MID", "HIGH", "MAX"], "How loud cries, steps and menu sounds are."],
  ["POKéDEX VOICE", "voice", ["OFF", "ON"], "Whether the POKéDEX reads its entries out loud."],
  ["NIGHTS", "nights", ["DARK", "SOFT", "BRIGHT"], "How dark nights get outside."],
  ["WILD POKéMON", "wild", ["HIDDEN", "SEEN"], "Whether wild POKéMON show in the grass, or jump out at you."],
  ["BATTLE EFFECTS", "effects", ["ON", "OFF"], "Whether moves play their animations in battle."],
  ["BATTLE STYLE", "style", ["SWITCH", "SET"], "SWITCH asks if you want to change POKéMON when a trainer's sends out the next one."],
];
screen("options", "Start", "Options", {
  what: "Settings, a row each: left and right to change. What the row does is written at the top.",
  mod: "MenuScreen's OPTION: TEXT SPEED, MUSIC, POKéDEX VOICE, NIGHTS, WILD POKéMON.",
  tags: ["New: sound, battle effects, battle style, controls"],
  make(arg) {
    swirlBackdrop("lime", { lx: 15, ly: 0 }); let sel = 0;
    const desc = box(170, 40, 940, 96, "g-panel", ""); desc.style.padding = "22px 30px"; desc.style.fontSize = "22px"; desc.style.fontWeight = "800";
    const P = box(170, 160, 940, 470, "g-panel", ""); P.style.padding = "18px";
    const rows = OPTS.concat([["CONTROLS", null, null, "See every key: what it does in game, and what's turned off."]]);
    const draw = () => {
      desc.innerHTML = esc(T(rows[sel][3]));
      P.innerHTML = rows.map(([l, k, vals], i) => {
        const v = k ? T(vals[G.opts[k]]) : "";
        return `<div class="g-item${i === sel ? " on" : ""}" style="min-height:44px;padding-top:5px;padding-bottom:5px;${i === sel ? "" : "border-bottom:1.5px solid #EEF1F8;border-radius:0"}"><span style="width:340px">${esc(T(l))}</span><span class="r" style="display:flex;align-items:center;gap:14px">${k ? `<span style="color:var(--g-red)">&#9664;</span><span style="min-width:120px;text-align:center">${esc(v)}</span><span style="color:var(--g-red)">&#9654;</span>` : `<span style="color:var(--g-red)">&#9654;</span>`}</span></div>`;
      }).join("");
      [...P.children].forEach((c, i) => c.onclick = () => { sel = i; act("a"); });
    };
    const act = k => {
      const [l, key, vals] = rows[sel];
      if (key && (k === "left" || k === "right" || k === "a")) { G.opts[key] = (G.opts[key] + (k === "left" ? vals.length - 1 : 1)) % vals.length; draw(); return true; }
      if (l === "CONTROLS" && k === "a") { go("keymap", arg); return true; }
      return false;
    };
    draw(); hints([["A", "Change"], ["B", "Back"]]);
    const back = () => go(arg && arg.from === "menu" ? "menu" : "saves", { sel: 8 });
    return { key: k => { if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : rows.length - 1)) % rows.length; draw(); return true; } if (k === "b") { back(); return true; } return act(k); } };
  }
});
screen("keys", "Start", "Keys check", {
  what: "Before the first game: shows the keys you'll use, with a way to see the full key map. E opens the menu, as Minecraft's inventory key does, and every other mod's keys are turned off while you play.",
  tags: ["New"],
  make() {
    swirlBackdrop("peach", { lx: 80, ly: 0 }); const C = ctl();
    box(180, 100, 640, 470, "g-panel g-fadein", `<div style="padding:34px 40px;display:grid;gap:20px"><div class="g-title" style="font-size:36px">These are your keys</div>
      <div style="display:grid;grid-template-columns:auto 1fr;gap:14px 22px;align-items:center;font-size:22px;font-weight:800">
        <span><span class="g-key">W</span> <span class="g-key">A</span> <span class="g-key">S</span> <span class="g-key">D</span></span><span>Walk</span>
        <span><span class="g-key" style="min-width:120px">SPACE</span></span><span>Talk, look, choose</span>
        <span><span class="g-key">E</span></span><span>Open the menu (as Minecraft's inventory)</span>
        <span><span class="g-key" style="min-width:64px">TAB</span></span><span>Speed up</span>
        <span><span class="g-key">Q</span> <span class="g-key">X</span></span><span>Back</span></div>
      <div style="font-size:16px;font-weight:800;color:var(--g-muted);line-height:1.4">Other mods' keys are turned off while you play, so nothing else opens on these (a map mod on M, say).</div></div>`);
    C.choose(["Use these keys", "Show the key map", "Back"], { x: 860, y: 300, w: 300 }).then(i => go(i === 1 ? "keymap" : i === 2 || i < 0 ? "saves" : "prof"));
    return { key: k => C.key(k) };
  }
});

// ===================== A new game =====================
screen("prof", "A new game", "The professor", {
  scene: "prof", sceneOpts: () => ({ mode: "intro" }),
  what: "The professor welcomes you, with our creatures about him: one wanders round, the bird hovers flapping, and when he says some keep them as friends, the little yellow one jumps up onto his shoulder.",
  tags: ["Retromon creatures"], mod: "IntroScreen: Prof. Oak talks the whole time he's speaking.",
  try: ["A to go on."],
  make() {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 });
    add(h("div", "", `<div style="position:absolute;inset:0;background:radial-gradient(circle at 20% 30%,rgba(255,255,255,.6) 0 2px,transparent 3px),radial-gradient(circle at 80% 20%,rgba(255,255,255,.7) 0 2px,transparent 3px),radial-gradient(circle at 70% 70%,rgba(255,255,255,.5) 0 2px,transparent 3px),radial-gradient(circle at 30% 80%,rgba(255,255,255,.6) 0 2px,transparent 3px);background-size:240px 200px;animation:gbob 4s ease-in-out infinite"></div>`));
    const C = ctl(), S = SC.prof;
    // The professor's words on a dark band at the foot of the screen, no box (as Let's Go's opening).
    C.sayAt = { film: true };
    S.talking = true;
    C.say([
      { who: "PROF. OAK", text: "Ah, there you are! Welcome. I'm **OAK**. Most people round here call me the **POKéMON PROF**." },
      { who: "PROF. OAK", text: "This world is full of creatures we call **POKéMON**. You'll find them in the grass, the sea, the caves, even in town." },
      { who: "PROF. OAK", text: "Some people keep them as friends. Some raise them for battles. Me? I study them.", on: () => S.hop && S.hop() },
      { who: "PROF. OAK", text: "But that's enough about me. First, tell me a little about yourself." },
    ]).then(() => { S.talking = false; if (C.alive()) go("creator"); });
    return { key: k => C.key(k) };
  }
});
// The maker's rows: what the mod's CLOTHES already has (Outfit), and what's new here (more skin tones, eye colour).
function makerRows(tab) {
  const O = () => G.outfit, sw = (label, key, list, isNew) => ({ label, key, swatches: list.map(hex), isNew, get: () => O()[key], set: v => O()[key] = v });
  const ch = (label, key, names, off) => ({ label, key, chips: names.map((n, i) => [i, title(n)]), off, get: () => O()[key], set: v => O()[key] = v });
  if (tab === "you") return [
    { label: "BODY", chips: [[0, "Boy"], [1, "Girl"]], get: () => O().girl ? 1 : 0, set: v => { const keep = { skin: O().skin, eyes: O().eyes }; G.outfit = Object.assign(OUTFIT.defaults(!!v), keep); } },
    Object.assign(sw("SKIN", "skin", OUTFIT.SKIN_RGB.concat(OUTFIT.SKIN_RGB_NEW)), { newFrom: 4 }), Object.assign(sw("EYES", "eyes", OUTFIT.EYE_RGB), { newFrom: 0 }),
    ch("HAIR", "hair", OUTFIT.HAIRS), sw("HAIR COLOUR", "hairc", OUTFIT.COLOURS),
  ];
  return [ch("HAT", "hat", OUTFIT.HATS), ch("CAP", "capp", OUTFIT.CAP_NAMES, () => O().hat !== 1), ch("CAP WAY", "capb", ["Forwards", "Backwards"], () => O().hat !== 1), sw("HAT COLOUR", "hatc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)),
    ch("TOP", "top", OUTFIT.TOPS), sw("TOP COLOUR", "topc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)), ch("BOTTOMS", "bot", OUTFIT.BOTTOMS), sw("BOTTOMS COLOUR", "botc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)),
    sw("SHOES", "shoec", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES)), ch("BAG", "bag", OUTFIT.BAGS), sw("BAG COLOUR", "bagc", OUTFIT.COLOURS.slice(0, OUTFIT.CLOTHES))];
}
function maker(C, tabs, o) {
  let tab = 0, sel = 0, rows = makerRows(tabs[0]);
  const P = box(24, 20, 870, null, "g-panel", ""); P.style.padding = "12px 14px"; P.style.maxHeight = "650px"; P.style.overflow = "auto";
  const opts = r => r.swatches ? r.swatches.map((c, i) => [i, c]) : r.chips;
  const idxOf = r => Math.max(0, opts(r).findIndex(([v]) => v === r.get()));
  const draw = () => {
    rows = makerRows(tabs[tab]);
    const tabBar = tabs.length > 1 ? `<div style="display:flex;gap:8px;margin:2px 0 8px 10px">${tabs.map((t, i) => `<span class="g-chip${i === tab ? " on" : ""}" data-t="${i}" style="font-size:14px;padding:3px 14px">${t === "you" ? "YOU" : "CLOTHES"}</span>`).join("")}<span style="margin-left:auto;font-size:12px;font-weight:900;color:var(--g-muted);align-self:center">TAB switches</span></div>` : "";
    // Rows go in cards by what they're for (hat, top, bottoms...), with room to breathe: big swatches and chips.
    // YOU is one card: body, skin, eyes, then hair style and hair colour under them.
    const GROUP = { "BODY": "YOU", "SKIN": "YOU", "EYES": "YOU", "HAIR": "YOU", "HAIR COLOUR": "YOU", "HAT": "HAT", "CAP": "HAT", "CAP WAY": "HAT", "HAT COLOUR": "HAT",
      "TOP": "TOP", "TOP COLOUR": "TOP", "BOTTOMS": "BOTTOMS", "BOTTOMS COLOUR": "BOTTOMS", "SHOES": "SHOES", "BAG": "BAG", "BAG COLOUR": "BAG" };
    const short = (r, g) => r.label === "HAIR" && g === "YOU" ? "HAIR STYLE" : r.label === g ? (g === "SHOES" ? "COLOUR" : "STYLE") : r.label.startsWith(g + " ") ? r.label.slice(g.length + 1) : r.label;
    const one = tabs[tab] === "you";
    const rowHtml = (r, i, g) => { const dim = r.off && r.off(), on = i === sel;
      return `<div style="display:grid;grid-template-columns:${one ? 112 : 70}px 1fr;align-items:center;gap:10px;padding:${one ? "9px 12px" : "4px 8px"};border-radius:14px;transition:background .15s;${dim ? "opacity:.4;" : ""}${on ? "background:#FFF6CF;box-shadow:inset 0 0 0 2.5px #FFC93C" : ""}">
      <span class="g-label" style="font-size:12.5px;${on ? "color:var(--g-red)" : ""}">${on ? "&#9656; " : ""}${short(r, g)}</span>
      <span style="display:flex;flex-wrap:wrap;gap:${one ? 6 : 7}px;align-items:center">${opts(r).map(([v, l], j) => r.swatches
        ? `<span class="g-swatch${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="position:relative;width:${one ? 29 : 30}px;height:${one ? 29 : 30}px;border-width:3px;border-radius:9px;background:linear-gradient(160deg,rgba(255,255,255,.28),rgba(0,0,0,.12)),${l}">${r.newFrom != null && j >= r.newFrom ? `<i style="position:absolute;right:-4px;top:-4px;width:10px;height:10px;border-radius:50%;background:#FFD45A;box-shadow:0 0 0 2px #fff"></i>` : ""}</span>`
        : `<span class="g-chip${v === r.get() ? " on" : ""}" data-r="${i}" data-j="${j}" style="font-size:14px;padding:3px 11px">${esc(l)}</span>`).join("")}</span></div>`; };
    const groups = []; rows.forEach((r, i) => { const g = GROUP[r.label] || r.label; let G2 = groups[groups.length - 1]; if (!G2 || G2.g !== g) groups.push(G2 = { g, rows: [] }); G2.rows.push([r, i]); });
    // Rows sit apart (a clear gap under the one you're on), the YOU card full width, CLOTHES in two columns.
    P.innerHTML = tabBar + `<div style="${one ? "" : "columns:2;column-gap:10px"}">${groups.map(G2 => `<div style="border-radius:18px;background:#F3F6FC;padding:${one ? "12px 12px 14px" : "8px 6px"};display:grid;gap:${one ? 20 : 8}px;break-inside:avoid;margin-bottom:12px">
      <div class="g-title" style="font-size:17px;letter-spacing:2px;padding:2px 12px 2px;color:var(--g-red)">${G2.g}</div>${G2.rows.map(([r, i]) => rowHtml(r, i, G2.g)).join("")}</div>`).join("")}</div>` +
      `<div style="display:flex;gap:12px;margin-top:0;padding:0 10px;align-items:center"><span class="g-item${sel === rows.length ? " on" : ""}" data-done="1">${esc(T(o.done || "DONE"))}</span><span class="g-item${sel === rows.length + 1 ? " on" : ""}" data-rand="1">${esc(T("MIX IT UP"))}</span>
        ${tabs[tab] === "you" ? `<span style="margin-left:auto;font-size:12.5px;font-weight:800;color:var(--g-muted)"><i style="display:inline-block;width:9px;height:9px;border-radius:50%;background:#FFD45A;margin-right:5px"></i>New: four more skin tones, eye colour</span>` : ""}</div>`;
    P.querySelectorAll("[data-r]").forEach(e => e.onclick = () => { const r = rows[+e.dataset.r]; sel = +e.dataset.r; r.set(opts(r)[+e.dataset.j][0]); changed(); });
    P.querySelectorAll("[data-t]").forEach(e => e.onclick = () => { tab = +e.dataset.t; sel = 0; draw(); });
    P.querySelector("[data-done]").onclick = () => o.onDone(); P.querySelector("[data-rand]").onclick = () => mix();
  };
  const changed = () => { draw(); if (SC.creator) SC.creator.refresh(); };
  const mix = () => { for (const t of tabs) for (const r of makerRows(t)) { if (r.label === "BODY") continue; const os = opts(r); r.set(os[Math.floor(Math.random() * os.length)][0]); } changed(); };
  draw();
  const w = { key(k) {
    const n = rows.length + 2;
    if (k === "tab" && tabs.length > 1) { tab = (tab + 1) % tabs.length; sel = 0; draw(); return true; }
    if (k === "up" || k === "down") { sel = (sel + (k === "down" ? 1 : n - 1)) % n; draw(); return true; }
    if (sel >= rows.length) { if (k === "a") { if (sel === rows.length) o.onDone(); else mix(); return true; } if (k === "left" || k === "right") { sel = sel === rows.length ? rows.length + 1 : rows.length; draw(); return true; } }
    if (k === "a") { o.onDone(); return true; }
    const r = rows[sel];
    if (r && (k === "left" || k === "right")) { const os = opts(r); r.set(os[(idxOf(r) + (k === "right" ? 1 : os.length - 1)) % os.length][0]); changed(); return true; }
    if (k === "b" && o.onBack) { o.onBack(); return true; }
    return false;
  } };
  C.push(w);
  return w;
}
screen("creator", "A new game", "Character maker", {
  scene: "creator", sceneOpts: () => ({ side: -1.95 }),
  what: "Make yourself on Minecraft's player model, its corners just softened. YOU is one card: body, skin tone, eyes, then hair style and hair colour under them, each row with room round it. CLOTHES is the second tab. It's the mod's own wardrobe (the same layers and colours CLOTHES uses), with four more skin tones and eye colour added.",
  tags: ["The mod's wardrobe", "New: 4 skin tones, eye colour"], mod: "Outfit: FAIR, LIGHT, TAN, DEEP skin; 6 hair styles; cap, beanie or sun hat; jacket, tee, hoodie or vest; jeans, shorts or skirt; shoes; backpack or satchel; 14 colours.",
  try: ["Up and down between rows, left and right to change, A on DONE.", "MIX IT UP picks at random."],
  make() {
    swirlBackdrop("peach", { lx: 80, ly: 10, cx: 78, cy: 62 });
    const C = ctl();
    box(900, 34, 360, null, "", `<div class="g-title" style="text-align:center;font-size:26px;color:#26304A">${esc(T("What do you look like?"))}</div>`);
    maker(C, ["you", "clothes"], { done: "THAT'S ME", onDone: () => go("name", { who: "you" }), onBack: () => go("prof") });
    // Plain to see: Back and Continue, bottom right (left and right change a row; A carries on from anywhere).
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
    return { key: k => C.key(k) };
  }
});
const KEYS_AZ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").concat(["-", "'", ".", " ", "0", "1", "2", "3", "4", "5"]);
function nameEntry(C, o) {
  let name = o.start || "", sel = 0, presetsOn = !!(o.presets && o.presets.length);
  const P = box(240, 150, 800, 430, "g-panel g-pop", ""); P.style.padding = "24px 30px";
  const all = KEYS_AZ.length + 2;
  const draw = () => {
    P.innerHTML = `<div style="display:flex;align-items:center;gap:18px;margin-bottom:18px"><span class="g-label">${esc(T(o.label))}</span>
      <span style="flex:1;display:flex;gap:8px">${Array.from({ length: 7 }, (_, i) => `<span style="flex:1;max-width:56px;height:56px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:34px;background:${i < name.length ? "#FFF6CF" : "#EEF2FB"};color:#26304A;${i === name.length ? "box-shadow:inset 0 0 0 3px #E5525C" : ""}">${esc(name[i] || "")}</span>`).join("")}</span></div>
      <div style="display:grid;grid-template-columns:repeat(9,1fr);gap:8px">${KEYS_AZ.map((c, i) => `<span data-i="${i}" style="height:50px;border-radius:14px;display:grid;place-items:center;font-family:Fredoka;font-weight:600;font-size:26px;cursor:pointer;${i === sel ? "background:var(--g-sel);color:#26304A;box-shadow:0 0 0 2px #fff,0 4px 10px rgba(232,150,30,.35)" : "background:#EEF2FB"}">${c === " " ? "&#9251;" : esc(c)}</span>`).join("")}
      <span data-i="${KEYS_AZ.length}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length ? "background:var(--g-sel);color:#26304A" : "background:#EEF2FB"}">${esc(T("DELETE"))}</span>
      <span data-i="${KEYS_AZ.length + 1}" style="grid-column:span 2;height:50px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:18px;cursor:pointer;${sel === KEYS_AZ.length + 1 ? "background:linear-gradient(180deg,#6BE0A6,#2FA86C);color:#fff" : "background:#D9F5E6;color:#1F8A55"}">OK</span></div>`;
    P.querySelectorAll("[data-i]").forEach(e => e.onclick = () => { sel = +e.dataset.i; press(); });
  };
  const press = () => { if (sel < KEYS_AZ.length) { if (name.length < 7) name += KEYS_AZ[sel]; } else if (sel === KEYS_AZ.length) name = name.slice(0, -1); else if (name.trim()) { o.onDone(name.trim()); return; } draw(); };
  draw();
  const w = {
    key(k) { if (k === "a") { press(); return true; } if (k === "b") { if (name) { name = name.slice(0, -1); draw(); } else if (o.onBack) o.onBack(); return true; }
      const n = sel; sel = gridMove(sel, all, 9, k); if (sel !== n) draw(); return true; },
    raw(e) { if (/^Key[A-Z]$/.test(e.code) && !["KeyW", "KeyA", "KeyS", "KeyD", "KeyE", "KeyQ", "KeyX", "KeyZ"].includes(e.code) || (e.shiftKey && /^Key[A-Z]$/.test(e.code))) { if (name.length < 7) name += e.code.slice(3); draw(); return true; }
      if (e.code === "Backspace") { name = name.slice(0, -1); draw(); return true; } if (e.code === "Enter" && name.trim()) { o.onDone(name.trim()); return true; } return false; }
  };
  C.push(w);
  return w;
}
screen("name", "A new game", "Your name", {
  scene: "creator", sceneOpts: () => ({ side: 0 }),
  what: "Type your name, up to 7 letters, or pick one of the ready names.",
  mod: "IntroScreen: YOUR NAME, with ready names and NEW NAME.",
  try: ["Type on your keyboard (Shift for W, A, S, D, E, Q, X and Z), or move round the letters and press A.", "OK when you're done."],
  make(arg) {
    swirlBackdrop("peach", { lx: 80, ly: 10, cx: 78, cy: 62 });
    const C = ctl(), girl = !!G.outfit.girl;
    const presets = girl ? ["LEAF", "LYRA", "KRIS"] : ["RED", "KRIS", "ETHAN"];
    C.choose(["NEW NAME"].concat(presets), { x: 980, y: 190, w: 250, back: false }).then(i => {
      if (i > 0) { G.name = presets[i - 1]; go("rival"); return; }
      const w = nameEntry(C, { label: "YOUR NAME", start: "", onDone: n => { G.name = n.toUpperCase(); go("rival"); }, onBack: () => go("creator") });
      cur.raw = e => w.raw(e);
    });
    box(0, 70, 1280, null, "", `<div class="g-title" style="text-align:center;font-size:30px;color:#26304A">${esc(T("And what's your name?"))}</div>`);
    actions([{ key: "b", label: "Back" }, { key: "a", label: "Continue" }], { right: true });
    return { key: k => C.key(k) || (k === "b" ? (go("creator"), true) : false) };
  }
});
screen("rival", "A new game", "Your rival's name", {
  scene: "prof", sceneOpts: () => ({ mode: "rival" }),
  what: "The professor's grandson, your rival since you were small. Pick or type his name.",
  mod: "IntroScreen: YOUR RIVAL'S NAME.",
  make() {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 }); const C = ctl(), S = SC.prof; C.sayAt = { film: true };
    (async () => {
      S.talking = true;
      await C.say([{ who: "PROF. OAK", text: "So you're **{N}**! A fine name." }, { who: "PROF. OAK", text: "And this is my grandson. You two have been racing each other since you could walk." }, { who: "PROF. OAK", text: "Now, what was his name again...?" }], { keep: true });
      S.talking = false;
      const names = ["BLUE", "GARY", "JOHN"];
      const i = await C.choose(["NEW NAME"].concat(names), { x: 980, y: 140, w: 250, back: false });
      if (!C.alive()) return;
      const done = async n => { G.rival = n; uiRoot.querySelectorAll(".g-text").forEach(e => e.remove()); S.talking = true; await C.say([{ who: "PROF. OAK", text: "That's right! I remember now. His name is **{R}**!" }]); S.talking = false; if (C.alive()) go("ready"); };
      if (i > 0) done(names[i - 1]);
      else { uiRoot.querySelectorAll(".g-text").forEach(e => e.remove()); const w = nameEntry(C, { label: "YOUR RIVAL'S NAME", onDone: n => { C.pop(w); uiRoot.querySelectorAll(".g-panel.g-pop").forEach(e => e.remove()); done(n.toUpperCase()); } }); cur.raw = e => w.raw(e); }
    })();
    return { key: k => C.key(k) };
  }
});
screen("ready", "A new game", "Off you go", {
  scene: "prof", sceneOpts: () => ({ mode: "you" }),
  what: "You, as you made yourself, beside the professor, before the game loads.",
  make() {
    swirlBackdrop("dream", { lx: 50, ly: 0, cx: 50, cy: 58 }); const C = ctl(), S = SC.prof; C.sayAt = { film: true };
    // You look up (so your eyes show under the cap), then at "Off you go" you do the floss while he waves.
    S.talking = true; S.youWave = 0; S.youFloss = 0; S.youLookUp = 0.38; S.wave = 0;
    C.say([{ who: "PROF. OAK", text: "**{N}**, your own adventure starts today." }, { who: "PROF. OAK", text: "There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab." },
      { who: "PROF. OAK", text: "Off you go, then!", on: () => { S.youFloss = 1; S.wave = 1; } }])
      .then(() => { S.talking = false; return C.wait(1600); }).then(() => { S.youFloss = 0; S.wave = 0; if (C.alive()) go("loading"); });
    return { key: k => C.key(k) };
  }
});
const TIPS = ["Press TAB to speed the game up. Press it again to go back.", "Creatures 1 block tall or less can ride on your shoulder.", "Talk to people with SPACE. Some have things to give you.",
  "Your POKéMON follow you around. Talk to them too.", "In the bag, the KEY ITEMS pocket is its lid.", "Nights are darker outside. Change it under OPTIONS, NIGHTS."];
screen("loading", "A new game", "Loading", {
  what: "While the world loads: a ball spinning, the place you're going, a tip.",
  tags: ["New"],
  make() {
    backdrop(NIGHT);
    const tip = TIPS[Math.floor(Math.random() * TIPS.length)];
    box(0, 0, 1280, 720, "", `<div style="position:absolute;left:50%;top:44%;transform:translate(-50%,-50%);display:grid;justify-items:center;gap:22px">
      <div style="width:110px;height:110px;animation:spin 1.1s linear infinite">${ICON.ball}</div><div class="g-title" style="font-size:40px;letter-spacing:4px">${esc(G.place)}</div>
      <div class="g-hp" style="width:520px;height:18px"><i id="ld" style="width:0%;background:linear-gradient(90deg,#4FB896,#8FF5C0);transition:width .2s"></i></div></div>
      <div class="g-panel soft" style="position:absolute;left:50%;bottom:60px;transform:translateX(-50%);padding:16px 30px;font-size:20px;font-weight:800;white-space:nowrap"><span class="g-label" style="margin-right:12px">Tip</span>${esc(tip)}</div>
      <style>@keyframes spin{to{transform:rotate(360deg)}}</style>`);
    let p = 0; const t = setInterval(() => { p = Math.min(100, p + 7 + Math.random() * 9); const e = $("#ld"); if (e) e.style.width = p + "%"; if (p >= 100) { clearInterval(t); later(350, () => go("hud")); } }, 160); timers.push(t);
    return { key: k => true };
  }
});
