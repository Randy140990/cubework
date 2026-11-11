#!/usr/bin/env python3
"""
For one target in tools/versions.json: downloads that Minecraft version's client jar from Mojang (for its
list of blocks and vanilla files to compare with) and finds the newest Fabric API built for it.
Writes <out>/client.jar, <out>/blocks.txt and <out>/fabric-api.txt. Run in CI.
"""
import json, os, re, sys, urllib.request, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))


def get(url):
    with urllib.request.urlopen(url, timeout=120) as r:
        return r.read()


def main():
    name, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    target = next(t for t in json.load(open(os.path.join(HERE, "..", "versions.json"))) if t["name"] == name)
    mc = target["mc"]
    manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    entry = next(v for v in manifest["versions"] if v["id"] == mc)
    info = json.loads(get(entry["url"]))
    jar = os.path.join(out, "client.jar")
    with open(jar, "wb") as f:
        f.write(get(info["downloads"]["client"]["url"]))
    with zipfile.ZipFile(jar) as z:
        blocks = sorted(re.match(r"assets/minecraft/blockstates/(.+)\.json$", n).group(1) for n in z.namelist()
                        if re.match(r"assets/minecraft/blockstates/.+\.json$", n))
    open(os.path.join(out, "blocks.txt"), "w").write("\n".join(blocks) + "\n")
    meta = get("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml").decode()
    versions = [v for v in re.findall(r"<version>([^<]+)</version>", meta) if v.endswith("+" + mc)]
    open(os.path.join(out, "fabric-api.txt"), "w").write(versions[-1] if versions else "")
    print(mc, len(blocks), "blocks; Fabric API", versions[-1] if versions else "none")


if __name__ == "__main__":
    main()
