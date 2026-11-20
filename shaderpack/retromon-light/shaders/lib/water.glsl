// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// Water: small moving waves, the sky and the sun reflected in it (more at a low angle), clear in the shallows and
// deeper blue-green further down.

uniform sampler2D depthtex1;
uniform float viewWidth;
uniform float viewHeight;
uniform mat4 gbufferProjectionInverse;

float waveHeight(vec2 p, float t) {
    float h = sin(dot(p, vec2(0.92, 0.38)) * 1.1 + t * 1.3) * 0.30;
    h += sin(dot(p, vec2(-0.45, 0.89)) * 1.7 - t * 1.1) * 0.22;
    h += sin(dot(p, vec2(0.71, -0.70)) * 3.3 + t * 2.3) * 0.10;
    h += sin(dot(p, vec2(0.20, 0.98)) * 5.1 - t * 2.9) * 0.05;
    return h;
}

// The surface's normal (world space) at p (blocks across).
vec3 waterNormal(vec2 p, float t) {
    float e = 0.04, h = waveHeight(p, t);
    float dx = (waveHeight(p + vec2(e, 0.0), t) - h) / e, dz = (waveHeight(p + vec2(0.0, e), t) - h) / e;
    return normalize(vec3(-dx * 0.16 * WATER_WAVES, 1.0, -dz * 0.16 * WATER_WAVES));
}

#ifdef FSH
// How far below the surface the bottom is here (blocks).
float waterDepth(vec3 viewPos) {
    vec2 sc = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
    float d = texture(depthtex1, sc).r;
    vec4 p = gbufferProjectionInverse * vec4(vec3(sc, d) * 2.0 - 1.0, 1.0);
    return max(length(p.xyz / p.w) - length(viewPos), 0.0);
}
#endif
