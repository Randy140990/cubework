// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The wind: plants sway and leaves rustle. Worked out from where a point is in the world, so a corner two
// blocks share moves the same in both (no gaps), and its shadow sways with it.

uniform float frameTimeCounter;
uniform vec3 cameraPosition;

vec3 windAt(vec3 wp, float amp) {
    float t = frameTimeCounter * WIND_SPEED;
    float a = sin(t * 1.6 + wp.x * 0.42 + wp.z * 0.31) * 0.6 + sin(t * 3.1 + wp.x * 0.9 - wp.z * 0.7) * 0.4;
    float b = sin(t * 1.3 + wp.z * 0.5 + wp.x * 0.2) * 0.6 + sin(t * 2.7 - wp.z * 1.1 + wp.x * 0.6) * 0.4;
    // Gusts: now and then a little stronger.
    float gust = 0.75 + 0.25 * sin(t * 0.37 + wp.x * 0.05);
    return vec3(a, 0.0, b) * amp * gust;
}

// How far a vertex moves (player space). id: the block's (block.properties); midBlock: at_midBlock (x64, from the
// vertex to the block's middle, so negative y at a block's top).
vec3 wave(vec3 playerPos, float id, vec4 midBlock) {
    if (id < 10000.5 || WAVING == 0) return vec3(0.0);
    vec3 wp = playerPos + cameraPosition;
    float top = midBlock.y < 0.0 ? 1.0 : 0.0;
    int k = int(id + 0.5);
    if (k == 10001) return windAt(wp, 0.10) * top;
    if (k == 10005) return windAt(wp, 0.10) * top * 0.5;
    if (k == 10006) return windAt(wp, 0.10) * (0.5 + 0.5 * top);
    if (k == 10002) return windAt(wp * 1.3, 0.035) + vec3(0.0, windAt(wp.zyx, 0.015).x, 0.0);
    return vec3(0.0);
}
