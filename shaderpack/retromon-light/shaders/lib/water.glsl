// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// Water, made the way Tempered Light makes it: waves that follow the wind (sharp crests and broad troughs, calm and
// choppy patches drifting across), a slow swell lifting the surface itself, the bottom seen clearly through it in
// the shallows (bent by the waves, with caustics moving over it), the water's own clear blue-green deepening with
// depth, the world and the sky mirrored in it (more at a low angle) and the sun glittering on it.
// What's behind the water comes from the deferred pass (program/copy.glsl), which keeps the picture as it is before
// any water is drawn.

#ifndef RL_WATER
#define RL_WATER
#ifndef INSIDE

float wHash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float wNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(wHash(i), wHash(i + vec2(1.0, 0.0)), u.x), mix(wHash(i + vec2(0.0, 1.0)), wHash(i + vec2(1.0, 1.0)), u.x), u.y);
}

mat2 wRot(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}

// The wind over the water: from the south-west, wandering slowly; stronger in rain.
vec2 waterWind() {
    float a = radians(45.0) + 0.35 * sin(frameTimeCounter * 0.0021) + 0.15 * sin(frameTimeCounter * 0.0137);
    return vec2(cos(a), sin(a));
}

float waterWindStrength() {
    return 0.55 + 0.6 * rainStrength;
}

// The water's own clock: a calm, natural pace.
float waterClock() {
    return frameTimeCounter * 0.4 * WIND_SPEED;
}

// The long, slow swell (blocks): the surface itself is lifted and lowered by it, so the waves have real height.
float waterSwell(vec2 p, float t) {
    vec2 wd = waterWind();
    vec2 wd2 = wRot(0.6) * wd;
    float ws = 0.6 + 0.5 * waterWindStrength();
    float s = sin(dot(p, wd) * 0.34 - t * 1.05) + 0.6 * sin(dot(p, wd2) * 0.53 - t * 1.37 + 1.7)
            + 0.3 * sin(dot(p, wRot(-0.9) * wd) * 0.81 - t * 1.8 + 4.1);
    return s * 0.045 * WATER_WAVES * ws;
}

// The height of the surface (blocks) at xz: waves of shrinking size running at angles to the wind, each warped a
// little so no line is straight, sharp at the crest (exp of a sine), with calm and choppy patches drifting across.
float waterHeight(vec2 p, float t, int octaves) {
    vec2 wd = waterWind();
    float ws = waterWindStrength();
    float calm = smoothstep(0.25, 0.8, wNoise(p * 0.018 + wd * t * 0.03));
    float amp = mix(0.4, 1.0, calm) * WATER_WAVES * (0.65 + 0.45 * ws);
    float h = 0.0;
    float a = 0.055, k = 0.45, ang = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        vec2 d = wRot(ang) * wd;
        float warp = wNoise(p * k * 0.4 + float(i) * 3.17) * 2.4;
        float ph = dot(p, d) * k - t * sqrt(9.81 * k) * 0.55 + warp;
        h += a * exp(sin(ph) - 1.0);
        a *= 0.6;
        k *= 1.72;
        ang += (i % 2 == 0) ? 1.05 : -1.75;
    }
    return h * amp + waterSwell(p, t);
}

// The surface's normal (world space) at a point in the world, dist blocks from you.
vec3 waterNormal(vec3 worldPos, float dist) {
    float t = waterClock();
    float e = 0.05 + dist * 0.0012;
    int oct = dist > 96.0 ? 4 : 6;
    vec2 p = worldPos.xz;
    float hx = waterHeight(p + vec2(e, 0.0), t, oct) - waterHeight(p - vec2(e, 0.0), t, oct);
    float hz = waterHeight(p + vec2(0.0, e), t, oct) - waterHeight(p - vec2(0.0, e), t, oct);
    vec3 n = normalize(vec3(-hx / (2.0 * e), 1.0, -hz / (2.0 * e)));
    // Eased off far away (no shimmer), but still readable from high up.
    float keep = exp(-dist / 260.0);
    return normalize(mix(vec3(0.0, 1.0, 0.0), n, 0.5 + 0.5 * keep));
}

// How much of each colour the water takes out, and scatters back, per block: clear, bright, tropical blue-green
// (red goes first, so deep water turns turquoise and then blue).
vec3 waterAbsorption() {
    return vec3(0.30, 0.060, 0.050);
}

vec3 waterScatter() {
    return vec3(0.010, 0.060, 0.062);
}

// The bright web of focused sunlight moving over the bottom.
float causticLayer(vec2 p, float t) {
    vec2 q = p + vec2(wNoise(p * 0.8 + t * 0.35), wNoise(p * 0.8 - t * 0.3 + 4.7)) * 1.7;
    float v = wNoise(q * 1.6 + t * 0.15);
    return pow(1.0 - abs(v * 2.0 - 1.0), 6.0);
}

float caustics(vec3 worldPos) {
    float t = frameTimeCounter * 0.5 * WIND_SPEED;
    vec2 p = worldPos.xz + waterWind() * t * 0.2;
    float c = causticLayer(p * 0.9, t) + causticLayer(p * 1.31 + 11.0, t * 1.17);
    return 0.35 + c * 1.6;
}

#ifdef FSH
uniform sampler2D depthtex1;
uniform sampler2D colortex1;
uniform float viewWidth;
uniform float viewHeight;
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;

// A point on the screen (0 to 1, and its depth) as a point in view space, and back.
vec3 screenToView(vec2 sc, float d) {
    vec4 p = gbufferProjectionInverse * vec4(vec3(sc, d) * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec3 viewToScreen(vec3 v) {
    vec4 c = gbufferProjection * vec4(v, 1.0);
    return c.xyz / c.w * 0.5 + 0.5;
}

float viewZFromDepth(float d) {
    float z = d * 2.0 - 1.0;
    return (gbufferProjectionInverse[2][2] * z + gbufferProjectionInverse[3][2]) /
           (gbufferProjectionInverse[2][3] * z + gbufferProjectionInverse[3][3]);
}

// The world mirrored in the water: follow the reflected ray across the picture until it passes behind something
// (then home in on where). rgb the colour there, a how sure (0: use the sky). Fixed steps, so it holds still.
vec4 waterMirror(vec3 viewPos, vec3 dir) {
    if (dir.z > 0.0 && -viewPos.z < 1.0) return vec4(0.0);
    float stepLen = 0.35 + 0.03 * length(viewPos);
    vec3 p = viewPos + dir * stepLen * 0.7;
    vec3 prev = viewPos;
    for (int i = 0; i < 28; i++) {
        vec3 sp = viewToScreen(p);
        if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0 || sp.z > 1.0) break;
        float d = texture(depthtex1, sp.xy).r;
        float diff = viewZFromDepth(d) - p.z;
        if (d < 1.0 && diff > 0.0 && diff < stepLen * 2.2 + 0.3) {
            vec3 a = prev, b = p;
            for (int k = 0; k < 5; k++) {
                vec3 m = (a + b) * 0.5;
                if (viewZFromDepth(texture(depthtex1, viewToScreen(m).xy).r) - m.z > 0.0) b = m; else a = m;
            }
            vec3 hs = viewToScreen(b);
            vec2 e = min(hs.xy, 1.0 - hs.xy);
            float fade = clamp(min(e.x, e.y) * 9.0, 0.0, 1.0) * clamp(float(28 - i) / 4.0, 0.0, 1.0);
            return vec4(texture(colortex1, hs.xy).rgb, fade);
        }
        prev = p;
        p += dir * stepLen;
        stepLen *= 1.22;
    }
    return vec4(0.0);
}
#endif

#endif
#endif
