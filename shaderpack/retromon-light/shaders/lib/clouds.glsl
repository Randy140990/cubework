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

// How much sun gets through the clouds at a point (world x, z): 1 in the clear, less under a cloud.
float cloudShadow(vec2 xz, float t) {
    vec2 p = (xz + vec2(t * 2.4, t * 1.0)) / 64.0;
    float n = cloudNoise(p) * 0.62 + cloudNoise(p * 2.3 + vec2(7.1, 3.7)) * 0.28 + cloudNoise(p * 5.1 + vec2(1.3, 9.2)) * 0.10;
    return 1.0 - smoothstep(0.50, 0.70, n) * CLOUD_SHADOWS;
}
