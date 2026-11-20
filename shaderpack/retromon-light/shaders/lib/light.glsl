// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The light: out in the world, the sun's (or the moon's) moving with the day, the sky's, and lamps'; inside a
// building, its own (lamp-lit fill, a fixed soft key from above). No sun moves across a room.

uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 shadowLightPosition;
uniform float rainStrength;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;

// The lightmap's two levels (block, sky) as 0 to 1.
vec2 levels(vec2 lmcoord) {
    return clamp((lmcoord - 1.0 / 32.0) * (16.0 / 15.0), 0.0, 1.0);
}

// Lamps, torches: warm, falling off quickly.
vec3 lampLight(float block) {
    return vec3(1.0, 0.80, 0.58) * (pow(block, 2.6) * 1.55);
}

#ifdef INSIDE

// Inside: lit as the room's lamps say (the rooms are full of hidden lights, so they're evenly lit; a room
// without them stays dark), with a soft fixed light from above and in front for shape.
vec3 roomLight(vec3 nView, vec2 lm) {
    float lit = mix(0.05, 1.0, smoothstep(0.0, 0.85, lm.x));
    vec3 key = normalize(mat3(gbufferModelView) * vec3(0.35, 1.0, 0.55));
    float k = max(dot(nView, key), 0.0);
    vec3 fill = vec3(1.0, 0.97, 0.92) * 1.25;
    return (fill * (0.72 + 0.40 * k) * lit + vec3(1.0, 0.84, 0.66) * 0.14 * lm.x) * INSIDE_BRIGHTNESS;
}

#else

// How high the sun is: 1 overhead, 0 on the horizon, -1 underfoot.
float sunHeight() {
    return dot(normalize(sunPosition), normalize(upPosition));
}

// The light now: from the sun by day (white at noon, warm low down), from the moon by night (cool blue), and
// the sky's all round (blue by day, rose at dusk, deep blue at night). Rain dims and greys it.
void skyLight(out vec3 direct, out vec3 ambient) {
    float h = sunHeight();
    float day = smoothstep(-0.08, 0.22, h);
    float dusk = smoothstep(-0.14, 0.04, h) * (1.0 - smoothstep(0.04, 0.38, h));
    vec3 sun = mix(vec3(1.00, 0.95, 0.86) * 1.10, vec3(1.00, 0.58, 0.30) * 0.95, dusk) * SUN_STRENGTH;
    vec3 moon = vec3(0.42, 0.56, 0.95) * 0.34 * NIGHT_BRIGHTNESS;
    // The sun hands over to the moon at the horizon: dim through the swap, so the shadows don't jump.
    direct = mix(moon, sun, day) * smoothstep(0.0, 0.09, abs(h));
    vec3 dayAmb = vec3(0.50, 0.60, 0.76) * 0.62;
    vec3 duskAmb = vec3(0.64, 0.48, 0.54) * 0.50;
    vec3 nightAmb = vec3(0.10, 0.15, 0.34) * 0.78 * NIGHT_BRIGHTNESS;
    ambient = mix(nightAmb, mix(dayAmb, duskAmb, dusk), day);
    direct *= 1.0 - 0.75 * rainStrength;
    ambient = mix(ambient, vec3(dot(ambient, vec3(0.333))) * 0.9, rainStrength * 0.6);
}

// All the light on a surface (view-space normal; lightmap levels; how lit by the sun or moon it is).
vec3 worldLight(vec3 nView, vec2 lm, float sunlit) {
    vec3 direct, ambient;
    skyLight(direct, ambient);
    float up = dot(nView, normalize(upPosition)) * 0.5 + 0.5;
    // Only where the sky reaches (caves and under roofs are dark, lamps aside).
    float sky = lm.y * lm.y;
    vec3 amb = ambient * (0.55 + 0.45 * up) * sky;
    vec3 dir = direct * sunlit * smoothstep(0.05, 0.6, lm.y);
    // Lamps matter less in daylight (as in Minecraft, where the brighter of the two wins).
    float day = smoothstep(-0.08, 0.22, sunHeight());
    return amb + dir + lampLight(lm.x) * (1.0 - 0.6 * lm.y * day) + vec3(0.012);
}

#endif
