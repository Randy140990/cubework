// ---------- start: the screen named in the link, or the opening ----------
resize3(); fitUI(); renderLook();
document.querySelectorAll("#lookSeg [data-look]").forEach(b => b.onclick = () => setLook(b.dataset.look));
// Minecraft's language (the game follows it; there's no language screen of its own): pick one to see the words.
{ const sel = $("#langSel"); for (const [n, e] of LANGS) { const o = document.createElement("option"); o.value = n; o.textContent = n === e ? n : `${n} (${e})`; sel.appendChild(o); }
  try { const v = localStorage.getItem("rm-ui-lang"); if (v && LANGCODE[v]) G.lang = v; } catch (e) {}
  sel.value = G.lang; sel.onchange = () => { G.lang = sel.value; try { localStorage.setItem("rm-ui-lang", G.lang); } catch (e) {} sel.blur(); if (curDef) go(curDef.id, curArg); stageEl.focus({ preventScroll: true }); stageActive = true; }; }
// The creatures the start screens show, fetched straight away so they're there when you get to them.
for (const sid of ["charmander", "pikachu", "bulbasaur", "squirtle", "cubone", "oddish", "clefairy", "pidgey"]) loadSpecies(sid).catch(() => {});
skinsReady.then(() => { let id = ""; try { id = (location.hash || "").slice(1); } catch (e) {} go(BY[id] ? id : "splash"); requestAnimationFrame(loop3); });
window.addEventListener("hashchange", () => { let id = ""; try { id = location.hash.slice(1); } catch (e) {} if (BY[id] && (!curDef || curDef.id !== id)) go(id); });
