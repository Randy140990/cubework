#!/bin/bash
# Temporary: fetches and prepares the sky's planet and star textures (removed once they're in the repo).
set +e
mkdir -p out/fetched /tmp/raw
cd /tmp/raw
UA="OrbitArenaBuild/1.0 (https://github.com/Randy140990; texture fetch for a Minecraft mod)"
get() { # url file
  code=$(timeout 420 curl -sSL --connect-timeout 20 --max-time 400 -A "$UA" --retry 2 -o "$2" -w "%{http_code}" "$1")
  echo "$code $1 $(stat -c %s "$2" 2>/dev/null)" >> /tmp/raw/log.txt
}
# Solar System Scope's Earth (CC BY 4.0), as kept on Wikimedia Commons.
timeout 60 curl -sSL -A "$UA" "https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=Solarsystemscope%20texture&srnamespace=6&srlimit=100&format=json" -o commons.json
python3 - <<'PY' >> /tmp/raw/log.txt
import json
d = json.load(open("/tmp/raw/commons.json"))
names = [h["title"][5:] for h in d.get("query", {}).get("search", [])]
print("commons:", names)
want = {"day": ["8k earth daymap", "4k earth daymap", "2k earth daymap"], "night": ["8k earth nightmap", "4k earth nightmap", "2k earth nightmap"],
        "clouds": ["8k earth clouds", "4k earth clouds", "2k earth clouds"]}
with open("/tmp/raw/picks.txt", "w") as f:
    for k, prefs in want.items():
        for p in prefs:
            hit = [n for n in names if p in n.lower().replace("_", " ")]
            if hit:
                f.write(k + "\t" + hit[0] + "\n")
                break
print(open("/tmp/raw/picks.txt").read())
PY
while IFS=$'\t' read -r kind name; do
  get "https://commons.wikimedia.org/wiki/Special:FilePath/$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1].replace(" ","_")))' "$name")" "sss_$kind.jpg"
done < /tmp/raw/picks.txt
# The star map (NASA SVS Deep Star Maps 2020).
get "https://svs.gsfc.nasa.gov/vis/a000000/a004800/a004851/starmap_2020_8k.exr" starmap_8k.exr
ls -la >> /tmp/raw/log.txt
timeout 400 pip install --quiet OpenEXR numpy pillow >> /tmp/raw/log.txt 2>&1
timeout 900 python3 "$GITHUB_WORKSPACE/ci/prep.py" /tmp/raw "$GITHUB_WORKSPACE/out/fetched" >> /tmp/raw/log.txt 2>&1
cp /tmp/raw/log.txt "$GITHUB_WORKSPACE/out/fetched/log.txt"
exit 0
