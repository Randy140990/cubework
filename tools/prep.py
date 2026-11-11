#!/usr/bin/env python3
"""
Makes the Gradle project for one Minecraft version in build/v/<name>/: the Java sources with the
version-specific parts chosen, the build files, and the generated resources.

Version-specific source is marked with comment lines:

    //#if MC >= 1.21.5
    ... (kept for 1.21.5 and later)
    //#elif MC >= 1.21.2
    ...
    //#else
    ...
    //#endif

Conditions compare MC with a version (>=, >, <, <=, ==, !=) and can be joined with && and ||. Lines that
aren't kept (and the directive lines) become empty lines, so compiler errors keep their line numbers.
Names Mojang renamed between versions are swapped by SWAPS below.

Usage: prep.py <version name> [--fabric-api X] [--blocks blocks.txt]
"""
import json, os, re, shutil, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))


def ver(s):
    return tuple(int(p) for p in s.split("."))


# (applies when, regex, replacement): names that differ in older or newer versions.
SWAPS = [
    (lambda mc: mc < (1, 21, 11), r"\bIdentifier\b", "ResourceLocation"),
    (lambda mc: mc < (1, 21, 5), r"\bVegetationBlock\b", "BushBlock"),
    # 26.1 moved the model classes and renamed Fabric's creative tab and copper registries.
    (lambda mc: mc >= (26, 1), r"net\.minecraft\.client\.renderer\.block\.model\.BakedQuad\b", "net.minecraft.client.resources.model.geometry.BakedQuad"),
    (lambda mc: mc >= (26, 1), r"net\.minecraft\.client\.renderer\.block\.model\.BlockStateModel\b", "net.minecraft.client.renderer.block.dispatch.BlockStateModel"),
    (lambda mc: mc >= (26, 1), r"net\.minecraft\.client\.renderer\.block\.model\.BlockModelPart\b", "net.minecraft.client.renderer.block.dispatch.BlockModelPart"),
    (lambda mc: mc >= (26, 1), r"\bBlockModelPart\b", "BlockStateModelPart"),
    (lambda mc: mc >= (26, 1), r"net\.minecraft\.client\.resources\.model\.Material\b", "net.minecraft.client.resources.model.sprite.Material"),
    (lambda mc: mc >= (26, 1), r"net\.fabricmc\.fabric\.api\.itemgroup\.v1\.FabricItemGroup\b", "net.fabricmc.fabric.api.creativetab.v1.FabricItemGroup"),
    (lambda mc: mc >= (26, 1), r"\bFabricItemGroup\b", "FabricCreativeModeTab"),
    (lambda mc: mc >= (26, 1), r"\bregisterOxidizableBlockPair\b", "registerNextStage"),
    (lambda mc: mc >= (26, 1), r"\bregisterWaxableBlockPair\b", "registerWaxable"),
    # 26.3 renamed the push reactions.
    (lambda mc: mc >= (26, 3), r"\bPushReaction\.DESTROY\b", "PushReaction.POPPED"),
]


def condition(expr, mc):
    def one(term):
        m = re.fullmatch(r"\s*MC\s*(>=|<=|==|!=|>|<)\s*([0-9.]+)\s*", term)
        if not m:
            raise ValueError("bad condition: " + term)
        op, v = m.group(1), ver(m.group(2))
        return {">=": mc >= v, "<=": mc <= v, "==": mc == v, "!=": mc != v, ">": mc > v, "<": mc < v}[op]
    return any(all(one(t) for t in part.split("&&")) for part in expr.split("||"))


def preprocess(text, mc, where):
    out = []
    # Each frame: [this branch is kept, some branch already kept, parent kept]
    stack = []
    for n, line in enumerate(text.split("\n"), 1):
        s = line.strip()
        if s.startswith("//#if "):
            parent = all(f[0] for f in stack)
            keep = parent and condition(s[6:], mc)
            stack.append([keep, keep, parent])
            out.append("")
        elif s.startswith("//#elif "):
            if not stack:
                raise ValueError("%s:%d: #elif without #if" % (where, n))
            f = stack[-1]
            keep = f[2] and not f[1] and condition(s[8:], mc)
            f[0] = keep
            f[1] = f[1] or keep
            out.append("")
        elif s == "//#else":
            if not stack:
                raise ValueError("%s:%d: #else without #if" % (where, n))
            f = stack[-1]
            f[0] = f[2] and not f[1]
            f[1] = True
            out.append("")
        elif s == "//#endif":
            if not stack:
                raise ValueError("%s:%d: #endif without #if" % (where, n))
            stack.pop()
            out.append("")
        else:
            out.append(line if all(f[0] for f in stack) else "")
    if stack:
        raise ValueError("%s: unclosed #if" % where)
    text = "\n".join(out)
    for applies, pattern, repl in SWAPS:
        if applies(mc):
            text = re.sub(pattern, repl, text)
    return text


def main():
    name = sys.argv[1]
    args = dict(zip(sys.argv[2::2], sys.argv[3::2]))
    target = next(t for t in json.load(open(os.path.join(HERE, "versions.json"))) if t["name"] == name)
    mc = ver(target["mc"])
    out = os.path.join(ROOT, "build", "v", name)
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)

    # Java sources.
    src = os.path.join(ROOT, "src", "main", "java")
    for dirpath, _, files in os.walk(src):
        for f in files:
            path = os.path.join(dirpath, f)
            rel = os.path.relpath(path, src)
            dest = os.path.join(out, "src", "main", "java", rel)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            text = open(path, encoding="utf-8").read()
            with open(dest, "w", encoding="utf-8") as fh:
                fh.write(preprocess(text, mc, rel) if f.endswith(".java") else text)

    # Hand-made resources, then generated ones.
    res = os.path.join(out, "src", "main", "resources")
    shutil.copytree(os.path.join(ROOT, "src", "main", "resources"), res)
    gen = [sys.executable, os.path.join(HERE, "gen.py"), "--out", res, "--mc", target["mc"]]
    if "--blocks" in args:
        gen += ["--blocks", args["--blocks"]]
    subprocess.run(gen, check=True)
    mod = json.load(open(os.path.join(res, "fabric.mod.json")))
    mod["depends"]["minecraft"] = target["range"]
    mod["depends"]["java"] = ">=%d" % target["java"]
    json.dump(mod, open(os.path.join(res, "fabric.mod.json"), "w"), indent=2)

    # Build files.
    props = open(os.path.join(ROOT, "gradle.properties")).read()
    props = re.sub(r"(?m)^minecraft_version=.*$", "minecraft_version=" + target["mc"], props)
    props = re.sub(r"(?m)^fabric_api_version=.*$", "fabric_api_version=" + args.get("--fabric-api", target.get("fabric_api", "")), props)
    props += "\nmc_target=%s\njava_release=%d\nloom_plugin=%s\n" % (name, target["java"], target["plugin"])
    open(os.path.join(out, "gradle.properties"), "w").write(props)
    build = open(os.path.join(HERE, "build", "build.gradle")).read()
    build = build.replace("@PLUGIN@", target["plugin"])
    remap = target["plugin"].endswith("-remap")
    build = build.replace("@MAPPINGS@", "\tmappings loom.officialMojangMappings()\n" if remap else "")
    build = build.replace("@MOD@", "modImplementation" if remap else "implementation")
    open(os.path.join(out, "build.gradle"), "w").write(build)
    shutil.copy(os.path.join(ROOT, "settings.gradle"), os.path.join(out, "settings.gradle"))
    shutil.copy(os.path.join(ROOT, "LICENSE"), os.path.join(out, "LICENSE"))
    print("prepared", name, "in", out)


if __name__ == "__main__":
    main()
