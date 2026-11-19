// ---------- start: the screen named in the link, or the opening ----------
resize3(); fitUI(); renderLook();
document.querySelectorAll("#lookSeg [data-look]").forEach(b => b.onclick = () => setLook(b.dataset.look));
skinsReady.then(() => { let id = ""; try { id = (location.hash || "").slice(1); } catch (e) {} go(BY[id] ? id : "splash"); requestAnimationFrame(loop3); });
window.addEventListener("hashchange", () => { let id = ""; try { id = location.hash.slice(1); } catch (e) {} if (BY[id] && (!curDef || curDef.id !== id)) go(id); });
