// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The sky's colour in a direction (view space): Minecraft's own sky and horizon colours, warmed round the sun
// as it sets, and a deep blue at night rather than black.

uniform vec3 skyColor;
uniform vec3 fogColor;

#ifndef INSIDE
vec3 skyAt(vec3 dirView) {
    vec3 up = normalize(upPosition);
    float h = dot(normalize(sunPosition), up);
    float upDot = dot(dirView, up);
    float horizon = 0.25 / (max(upDot, 0.0) * max(upDot, 0.0) + 0.25);
    vec3 top = skyColor, low = fogColor;
    // Night: Minecraft's sky goes nearly black. Ours stays a deep, clear blue.
    float night = 1.0 - smoothstep(-0.25, 0.05, h);
    top = mix(top, max(top, vec3(0.045, 0.085, 0.215) * NIGHT_BRIGHTNESS), night);
    low = mix(low, max(low, vec3(0.090, 0.140, 0.290) * NIGHT_BRIGHTNESS), night);
    vec3 c = mix(top, low, horizon);
    // Round the low sun: a warm glow, strongest at the horizon.
    float dusk = smoothstep(-0.20, 0.02, h) * (1.0 - smoothstep(0.02, 0.40, h));
    float toward = max(dot(dirView, normalize(sunPosition)), 0.0);
    float glow = dusk * (horizon * (0.25 + 0.75 * toward * toward) + pow(toward, 8.0) * 0.5) * (1.0 - rainStrength * 0.8);
    vec3 warm = mix(vec3(1.0, 0.60, 0.30), vec3(0.92, 0.34, 0.22), smoothstep(0.12, -0.12, h));
    c = mix(c, c * vec3(1.0, 0.80, 0.80), dusk * horizon * 0.5);
    c = mix(c, warm, clamp(glow * 0.85, 0.0, 0.85));
    return c;
}
#endif
