// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The sun's (or moon's) shadow map: squeezed towards you for detail close by, read softly.

vec2 distortShadow(vec2 p) {
    return p / (length(p) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT));
}

#ifndef SHADOW_PASS
uniform sampler2DShadow shadowtex0;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

// Twelve taps round each point, each one filtered by the hardware too: soft edges, no grain.
const vec2 TAPS[12] = vec2[12](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893),
    vec2(0.507, 0.064), vec2(0.896, 0.412), vec2(-0.322, -0.933), vec2(-0.792, -0.598));

// How much of the sun (or moon) reaches a point in the world (player space), 0 to 1.
float sunlight(vec3 playerPos, vec3 nPlayer, float NdotL) {
    if (NdotL <= 0.0) return 0.0;
    // Pushed out along the surface a little, more where the light grazes it, so faces don't shade themselves.
    vec3 p = playerPos + nPlayer * (0.04 + 0.08 * (1.0 - NdotL));
    vec4 sp = shadowProjection * (shadowModelView * vec4(p, 1.0));
    float d = length(sp.xy) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
    sp.xy /= d;
    vec3 sc = sp.xyz * 0.5 + 0.5;
    if (sc.x <= 0.0 || sc.y <= 0.0 || sc.x >= 1.0 || sc.y >= 1.0 || sc.z >= 1.0) return NdotL;
    sc.z -= 0.0004;
    float spread = SHADOW_SOFTNESS * 2.2 / float(shadowMapResolution);
    float lit = 0.0;
    for (int i = 0; i < 12; i++) lit += texture(shadowtex0, vec3(sc.xy + TAPS[i] * spread, sc.z));
    lit /= 12.0;
    return NdotL * mix(1.0 - SHADOW_STRENGTH, 1.0, lit);
}
#endif
