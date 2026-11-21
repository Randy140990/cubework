// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The clouds' shadows: big soft patches drifting with the wind across the land, dimming the sun where they pass
// (as Tempered Light has them). Worked out from where a point is in the world, so they slide over everything alike.

float cloudHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float cloudNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(cloudHash(i), cloudHash(i + vec2(1.0, 0.0)), f.x), mix(cloudHash(i + vec2(0.0, 1.0)), cloudHash(i + vec2(1.0, 1.0)), f.x), f.y);
}

// How much of a cloud is overhead at a point (world x, z): 0 in the clear, up to 1 under a thick one. Patches a
// few dozen blocks across with soft edges, so from up above you always see a few drifting by.
float cloudCover(vec2 xz, float t) {
    vec2 p = (xz + vec2(t * 3.2, t * 1.3)) / 38.0;
    float n = cloudNoise(p) * 0.60 + cloudNoise(p * 2.1 + vec2(7.1, 3.7)) * 0.28 + cloudNoise(p * 4.7 + vec2(1.3, 9.2)) * 0.12;
    return smoothstep(0.44, 0.66, n);
}

// How much sun (or moon) gets through the clouds at a point: 1 in the clear, less under a cloud.
float cloudShadow(vec2 xz, float t) {
    return 1.0 - cloudCover(xz, t) * CLOUD_SHADOWS;
}
