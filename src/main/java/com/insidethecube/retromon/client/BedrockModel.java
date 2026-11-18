package com.insidethecube.retromon.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * A Pokémon's model, read from its Bedrock geometry and animation files (the Cobblemon models), built as a
 * Minecraft model and posed with its idle animation each frame. Each Pokémon on the field has its own, so
 * each poses on its own.
 */
public final class BedrockModel {
	/** What's read once for a species: the files, worked into a form quick to build and pose. */
	public static final class Def {
		final String species;
		final JsonArray bones;
		final int texW, texH;
		public final float scale, height;
		public final Identifier texture;
		public final List<Layer> layers = new ArrayList<>();
		final Anim idle;
		/** One-off animations by name (cry, recoil, faint...), as Cobblemon plays them over the idle. */
		final Map<String, Anim> actions = new HashMap<>();
		/** How its parts stand in its pose: shown or hidden (Pidgey's open wings are hidden while it stands), moved, turned. */
		final List<JsonObject> poseParts = new ArrayList<>();
		/** Its other poses out in the world (walk, fly, hover): each an animation and how its parts stand. */
		final Map<String, PoseDef> poses = new HashMap<>();

		Def(String species, JsonObject geo, JsonObject info, JsonObject anims) {
			this.species = species;
			JsonObject g = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
			JsonObject d = g.getAsJsonObject("description");
			texW = d.has("texture_width") ? d.get("texture_width").getAsInt() : 64;
			texH = d.has("texture_height") ? d.get("texture_height").getAsInt() : 64;
			bones = g.has("bones") ? g.getAsJsonArray("bones") : new JsonArray();
			scale = info.has("scale") ? info.get("scale").getAsFloat() : 1F;
			height = info.has("height") ? info.get("height").getAsFloat() : 1F;
			texture = Identifier.parse(info.has("texture") ? info.get("texture").getAsString() : Retromon.ID + ":textures/pokemon/" + species + ".png");
			if (info.has("layers")) {
				for (JsonElement e : info.getAsJsonArray("layers")) {
					JsonObject l = e.getAsJsonObject();
					List<Identifier> frames = new ArrayList<>();
					for (JsonElement f : l.getAsJsonArray("frames")) frames.add(Identifier.parse(f.getAsString()));
					if (!frames.isEmpty()) layers.add(new Layer(frames, l.has("fps") ? l.get("fps").getAsFloat() : 10F));
				}
			}
			Anim a = null;
			JsonObject all = anims != null && anims.has("animations") ? anims.getAsJsonObject("animations") : new JsonObject();
			for (String key : new String[] {"idle", "ground"}) {
				if (a != null || !info.has(key) || info.get(key).isJsonNull()) continue;
				String name = info.get(key).getAsString();
				if (all.has(name)) a = Anim.read(all.getAsJsonObject(name));
			}
			idle = a;
			if (info.has("actions") && info.get("actions").isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : info.getAsJsonObject("actions").entrySet()) {
					String name = e.getValue().getAsString();
					if (all.has(name)) actions.put(e.getKey(), Anim.read(all.getAsJsonObject(name)));
				}
			}
			if (info.has("parts") && info.get("parts").isJsonArray()) {
				for (JsonElement e : info.getAsJsonArray("parts")) if (e.isJsonObject()) poseParts.add(e.getAsJsonObject());
			}
			if (info.has("poses") && info.get("poses").isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : info.getAsJsonObject("poses").entrySet()) {
					JsonObject o = e.getValue().getAsJsonObject();
					String anim = o.has("anim") ? o.get("anim").getAsString() : "";
					if (!all.has(anim)) continue;
					List<JsonObject> parts = new ArrayList<>();
					if (o.has("parts")) for (JsonElement pe : o.getAsJsonArray("parts")) if (pe.isJsonObject()) parts.add(pe.getAsJsonObject());
					poses.put(e.getKey(), new PoseDef(Anim.read(all.getAsJsonObject(anim)), parts));
				}
			}
		}

		/** Whether it has a pose by this name (walk, fly, hover). */
		public boolean hasPose(String name) {
			return poses.containsKey(name);
		}

		/** Whether it has a one-off animation by this name. */
		public boolean has(String action) {
			return actions.containsKey(action);
		}

		/** How long a one-off animation lasts (seconds), or 0. */
		public double length(String action) {
			Anim a = actions.get(action);
			return a == null ? 0 : a.length;
		}
	}

	/** A pose: its looping animation and how its parts stand. */
	record PoseDef(Anim anim, List<JsonObject> parts) {}

	/** A glowing layer drawn over the model (Charmander's flame), cycling through its frames. */
	public record Layer(List<Identifier> frames, float fps) {
		public Identifier frame(float seconds) {
			return frames.get(Math.floorMod((int) (seconds * fps), frames.size()));
		}
	}

	private static final Map<String, Optional<Def>> DEFS = new HashMap<>();

	public static Def def(String species) {
		return DEFS.computeIfAbsent(species, BedrockModel::load).orElse(null);
	}

	public static void clearCache() {
		DEFS.clear();
	}

	private static Optional<Def> load(String species) {
		try {
			JsonObject geo = json("pokemon/" + species + "/model.geo.json");
			JsonObject info = json("pokemon/" + species + "/info.json");
			if (geo == null) return Optional.empty();
			JsonObject anims = json("pokemon/" + species + "/anim.json");
			return Optional.of(new Def(species, geo, info == null ? new JsonObject() : info, anims));
		} catch (RuntimeException e) {
			Retromon.LOG.warn("Couldn't read the model for {}", species, e);
			return Optional.empty();
		}
	}

	private static JsonObject json(String path) {
		Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(Retromon.id(path));
		if (r.isEmpty()) return null;
		try (Reader in = r.get().openAsReader()) {
			return JsonParser.parseReader(in).getAsJsonObject();
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read {}", path, e);
			return null;
		}
	}

	// ---------- one model ----------

	public final Def def;
	public final ModelPart root;
	private final Map<String, ModelPart> parts = new LinkedHashMap<>();

	private BedrockModel(Def def) {
		this.def = def;
		MeshDefinition mesh = new MeshDefinition();
		Map<String, JsonObject> byName = new LinkedHashMap<>();
		Map<String, List<JsonObject>> children = new HashMap<>();
		List<JsonObject> roots = new ArrayList<>();
		for (JsonElement e : def.bones) {
			JsonObject b = e.getAsJsonObject();
			byName.put(b.get("name").getAsString(), b);
		}
		for (JsonObject b : byName.values()) {
			String parent = b.has("parent") ? b.get("parent").getAsString() : null;
			if (parent != null && byName.containsKey(parent)) children.computeIfAbsent(parent, k -> new ArrayList<>()).add(b);
			else roots.add(b);
		}
		for (JsonObject b : roots) build(mesh.getRoot(), b, null, children);
		this.root = LayerDefinition.create(mesh, def.texW, def.texH).bakeRoot();
		for (JsonObject b : roots) collect(root, b, children);
		for (JsonObject t : def.poseParts) {
			ModelPart p = parts.get(t.has("part") ? t.get("part").getAsString() : "");
			if (p != null && t.has("visible")) p.visible = t.get("visible").getAsBoolean();
		}
	}

	public static BedrockModel create(String species) {
		Def d = def(species);
		return d == null ? null : new BedrockModel(d);
	}

	private static float[] vec(JsonObject o, String key) {
		float[] v = new float[3];
		if (o.has(key) && o.get(key).isJsonArray()) {
			JsonArray a = o.getAsJsonArray(key);
			for (int i = 0; i < 3 && i < a.size(); i++) v[i] = a.get(i).getAsFloat();
		}
		return v;
	}

	private static float rad(float deg) {
		return (float) Math.toRadians(deg);
	}

	/** One bone: its cubes (turned ones as parts of their own, turned about their pivots) and its bones. */
	private static void build(PartDefinition parentDef, JsonObject bone, float[] parentPivot, Map<String, List<JsonObject>> children) {
		String name = bone.get("name").getAsString();
		float[] pv = vec(bone, "pivot"), rot = vec(bone, "rotation");
		CubeListBuilder cubes = CubeListBuilder.create();
		List<JsonObject> turned = new ArrayList<>();
		if (bone.has("cubes")) {
			for (JsonElement e : bone.getAsJsonArray("cubes")) {
				JsonObject c = e.getAsJsonObject();
				float[] r = vec(c, "rotation");
				if (r[0] != 0 || r[1] != 0 || r[2] != 0) {
					turned.add(c);
					continue;
				}
				addBox(cubes, c, pv);
			}
		}
		PartPose pose = parentPivot == null
				? PartPose.offsetAndRotation(-pv[0], 24 - pv[1], pv[2], -rad(rot[0]), -rad(rot[1]), rad(rot[2]))
				: PartPose.offsetAndRotation(-(pv[0] - parentPivot[0]), -(pv[1] - parentPivot[1]), pv[2] - parentPivot[2], -rad(rot[0]), -rad(rot[1]),
						rad(rot[2]));
		PartDefinition def = parentDef.addOrReplaceChild(name, cubes, pose);
		for (int i = 0; i < turned.size(); i++) {
			JsonObject c = turned.get(i);
			float[] cp = c.has("pivot") ? vec(c, "pivot") : pv, r = vec(c, "rotation");
			CubeListBuilder one = CubeListBuilder.create();
			addBox(one, c, cp);
			def.addOrReplaceChild(name + "_r" + i, one,
					PartPose.offsetAndRotation(-(cp[0] - pv[0]), -(cp[1] - pv[1]), cp[2] - pv[2], -rad(r[0]), -rad(r[1]), rad(r[2])));
		}
		for (JsonObject child : children.getOrDefault(name, List.of())) build(def, child, pv, children);
	}

	private static void addBox(CubeListBuilder b, JsonObject c, float[] pivot) {
		float[] o = vec(c, "origin"), s = vec(c, "size");
		int u = 0, v = 0;
		if (c.has("uv") && c.get("uv").isJsonArray()) {
			JsonArray uv = c.getAsJsonArray("uv");
			u = (int) Math.floor(uv.get(0).getAsFloat());
			v = (int) Math.floor(uv.get(1).getAsFloat());
		}
		float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0F;
		boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
		b.texOffs(u, v).mirror(mirror).addBox(pivot[0] - (o[0] + s[0]), pivot[1] - (o[1] + s[1]), o[2] - pivot[2], s[0], s[1], s[2], new CubeDeformation(inflate));
	}

	private void collect(ModelPart parent, JsonObject bone, Map<String, List<JsonObject>> children) {
		String name = bone.get("name").getAsString();
		ModelPart part;
		try {
			part = parent.getChild(name);
		} catch (RuntimeException e) {
			return;
		}
		parts.put(name, part);
		// Cobblemon's extra faces for when Ditto turns into it.
		if (name.contains("ditto")) part.visible = false;
		for (JsonObject child : children.getOrDefault(name, List.of())) collect(part, child, children);
	}

	/** Poses it for this moment of its idle animation. */
	public void pose(double seconds) {
		pose(seconds, null, 0);
	}

	/**
	 * Poses it for this moment of its idle, with a one-off animation (cry, recoil...) played over it, action
	 * seconds in (if it has that one and it hasn't finished).
	 */
	public void pose(double seconds, String action, double actionSeconds) {
		pose(seconds, null, action, actionSeconds);
	}

	/**
	 * Poses it in one of its poses out in the world (walk, fly, hover; null for its idle), with a one-off
	 * animation over it. The pose decides which parts show (Pidgey's wings open to fly, folded to walk).
	 */
	public void pose(double seconds, String poseName, String action, double actionSeconds) {
		for (ModelPart p : parts.values()) p.resetPose();
		PoseDef pd = poseName == null ? null : def.poses.get(poseName);
		visibility(def.poseParts);
		List<JsonObject> standing = def.poseParts;
		if (pd != null) {
			visibility(pd.parts());
			standing = pd.parts();
		}
		// Its pose's own moves and turns (from Cobblemon's poser).
		for (JsonObject t : standing) {
			ModelPart p = parts.get(t.has("part") ? t.get("part").getAsString() : "");
			if (p == null) continue;
			if (t.has("position") && t.get("position").isJsonArray()) {
				JsonArray v = t.getAsJsonArray("position");
				p.x += -v.get(0).getAsFloat();
				p.y += -v.get(1).getAsFloat();
				p.z += v.get(2).getAsFloat();
			}
			if (t.has("rotation") && t.get("rotation").isJsonArray()) {
				JsonArray v = t.getAsJsonArray("rotation");
				p.xRot += (float) -Math.toRadians(v.get(0).getAsFloat());
				p.yRot += (float) -Math.toRadians(v.get(1).getAsFloat());
				p.zRot += (float) Math.toRadians(v.get(2).getAsFloat());
			}
		}
		apply(pd != null ? pd.anim() : def.idle, seconds, true);
		Anim one = action == null ? null : def.actions.get(action);
		if (one != null && actionSeconds >= 0 && actionSeconds <= one.length) apply(one, actionSeconds, false);
	}

	private void visibility(List<JsonObject> list) {
		for (JsonObject t : list) {
			ModelPart p = parts.get(t.has("part") ? t.get("part").getAsString() : "");
			if (p != null && t.has("visible")) p.visible = t.get("visible").getAsBoolean();
		}
	}

	private void apply(Anim a, double seconds, boolean loop) {
		if (a == null) return;
		double t = a.length > 0 ? (loop && a.loop ? seconds % a.length : Math.min(seconds, a.length)) : seconds;
		double[] v = new double[3];
		for (Track tr : a.tracks) {
			ModelPart p = parts.get(tr.bone);
			if (p == null) continue;
			tr.eval(t, v);
			switch (tr.kind) {
				case Track.ROTATION -> {
					p.xRot += (float) -Math.toRadians(v[0]);
					p.yRot += (float) -Math.toRadians(v[1]);
					p.zRot += (float) Math.toRadians(v[2]);
				}
				case Track.POSITION -> {
					p.x += (float) -v[0];
					p.y += (float) -v[1];
					p.z += (float) v[2];
				}
				default -> {
					p.xScale *= (float) v[0];
					p.yScale *= (float) v[1];
					p.zScale *= (float) v[2];
				}
			}
		}
	}

	// ---------- animations ----------

	static final class Anim {
		double length;
		boolean loop;
		final List<Track> tracks = new ArrayList<>();

		static Anim read(JsonObject o) {
			Anim a = new Anim();
			a.loop = o.has("loop") && o.get("loop").isJsonPrimitive() && (o.get("loop").getAsJsonPrimitive().isBoolean() ? o.get("loop").getAsBoolean()
					: o.get("loop").getAsString().equals("true"));
			a.length = o.has("animation_length") ? o.get("animation_length").getAsDouble() : 0;
			double last = 0;
			if (o.has("bones")) {
				for (Map.Entry<String, JsonElement> b : o.getAsJsonObject("bones").entrySet()) {
					JsonObject ch = b.getValue().getAsJsonObject();
					for (String kind : new String[] {"rotation", "position", "scale"}) {
						if (!ch.has(kind)) continue;
						Track t = Track.read(b.getKey(), kind.equals("rotation") ? Track.ROTATION : kind.equals("position") ? Track.POSITION : Track.SCALE, ch.get(kind));
						if (t == null) continue;
						a.tracks.add(t);
						if (t.keys != null) last = Math.max(last, t.keys[t.keys.length - 1].time);
					}
				}
			}
			if (a.length <= 0) a.length = last;
			return a;
		}
	}

	static final class Key {
		final double time;
		final Molang.Expr[] pre, post;

		Key(double time, Molang.Expr[] pre, Molang.Expr[] post) {
			this.time = time;
			this.pre = pre;
			this.post = post;
		}
	}

	static final class Track {
		static final int ROTATION = 0, POSITION = 1, SCALE = 2;
		final String bone;
		final int kind;
		Molang.Expr[] constant;
		Key[] keys;

		Track(String bone, int kind) {
			this.bone = bone;
			this.kind = kind;
		}

		static Track read(String bone, int kind, JsonElement e) {
			Track t = new Track(bone, kind);
			if (e.isJsonObject() && !isPrePost(e.getAsJsonObject())) {
				List<Key> keys = new ArrayList<>();
				for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
					double time;
					try {
						time = Double.parseDouble(k.getKey());
					} catch (NumberFormatException ex) {
						continue;
					}
					JsonElement v = k.getValue();
					Molang.Expr[] pre, post;
					if (v.isJsonObject()) {
						JsonObject pp = v.getAsJsonObject();
						post = pp.has("post") ? values(pp.get("post"), kind) : pp.has("pre") ? values(pp.get("pre"), kind) : values(null, kind);
						pre = pp.has("pre") ? values(pp.get("pre"), kind) : post;
					} else {
						pre = post = values(v, kind);
					}
					keys.add(new Key(time, pre, post));
				}
				if (keys.isEmpty()) return null;
				keys.sort((a, b) -> Double.compare(a.time, b.time));
				t.keys = keys.toArray(new Key[0]);
			} else if (e.isJsonObject()) {
				JsonObject pp = e.getAsJsonObject();
				t.constant = values(pp.has("post") ? pp.get("post") : pp.get("pre"), kind);
			} else {
				t.constant = values(e, kind);
			}
			return t;
		}

		private static boolean isPrePost(JsonObject o) {
			return o.has("pre") || o.has("post");
		}

		private static Molang.Expr[] values(JsonElement e, int kind) {
			Molang.Expr zero = Molang.constant(kind == SCALE ? 1 : 0);
			Molang.Expr[] out = {zero, zero, zero};
			if (e == null || e.isJsonNull()) return out;
			if (e.isJsonArray()) {
				JsonArray a = e.getAsJsonArray();
				for (int i = 0; i < 3 && i < a.size(); i++) out[i] = expr(a.get(i));
			} else {
				Molang.Expr one = expr(e);
				out[0] = out[1] = out[2] = one;
			}
			return out;
		}

		private static Molang.Expr expr(JsonElement e) {
			if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return Molang.constant(e.getAsDouble());
			if (e.isJsonPrimitive()) return Molang.parse(e.getAsString());
			return Molang.constant(0);
		}

		void eval(double t, double[] out) {
			if (constant != null) {
				for (int i = 0; i < 3; i++) out[i] = constant[i].eval(t);
				return;
			}
			Key first = keys[0], last = keys[keys.length - 1];
			if (t <= first.time) {
				for (int i = 0; i < 3; i++) out[i] = first.pre[i].eval(t);
				return;
			}
			if (t >= last.time) {
				for (int i = 0; i < 3; i++) out[i] = last.post[i].eval(t);
				return;
			}
			for (int k = 0; k < keys.length - 1; k++) {
				Key a = keys[k], b = keys[k + 1];
				if (t < a.time || t > b.time) continue;
				double f = b.time > a.time ? (t - a.time) / (b.time - a.time) : 0;
				for (int i = 0; i < 3; i++) {
					double va = a.post[i].eval(t), vb = b.pre[i].eval(t);
					out[i] = va + (vb - va) * f;
				}
				return;
			}
		}
	}
}
