#!/bin/bash
# Temporary: fetches and prepares the sky's planet and star textures (removed once they're in the repo).
set +e
mkdir -p out/fetched /tmp/raw
cd /tmp/raw
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
get() { # url file
  timeout 420 curl -fsSL --connect-timeout 20 --max-time 400 -A "$UA" -e "https://www.solarsystemscope.com/textures/" --retry 2 -o "$2" "$1" && echo "ok $1 $(stat -c %s "$2")" >> /tmp/raw/log.txt || echo "FAIL $1" >> /tmp/raw/log.txt
}
for n in 8k_earth_daymap.jpg 8k_earth_nightmap.jpg 8k_earth_clouds.jpg; do
  get "https://www.solarsystemscope.com/textures/download/$n" "$n"
done
# NASA fallbacks (public domain)
get "https://eoimages.gsfc.nasa.gov/images/imagerecords/74000/74393/world.topo.200412.3x5400x2700.jpg" nasa_day.jpg
get "https://eoimages.gsfc.nasa.gov/images/imagerecords/57000/57747/cloud_combined_2048.jpg" nasa_clouds.jpg
get "https://eoimages.gsfc.nasa.gov/images/imagerecords/144000/144898/BlackMarble_2016_3km.jpg" nasa_night.jpg
get "https://svs.gsfc.nasa.gov/vis/a000000/a004800/a004851/starmap_2020_8k.exr" starmap_8k.exr
ls -la >> /tmp/raw/log.txt
timeout 300 pip install --quiet opencv-python-headless numpy pillow >> /tmp/raw/log.txt 2>&1
OPENCV_IO_ENABLE_OPENEXR=1 timeout 900 python3 "$GITHUB_WORKSPACE/ci/prep.py" /tmp/raw "$GITHUB_WORKSPACE/out/fetched" >> /tmp/raw/log.txt 2>&1
cp /tmp/raw/log.txt "$GITHUB_WORKSPACE/out/fetched/log.txt"
exit 0
