// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The picture's last touches (final's): the light's range brought into the screen's (gently, so nothing burns out), a
// touch of colour, a soft darkening at the corners, the darks lifted a little. (1.2.9) Inside, each thing drawn gets
// these as it's drawn (final passes the picture straight through), so a room's own lit blocks, coloured just as the
// Interior Viewer shows them, can be put on the screen as they are.

// As it is up to 0.8, then rolling off towards 1 (never quite reaching it), so bright sun doesn't flatten.
vec3 rolloff(vec3 x) {
    const float k = 0.8;
    vec3 over = k + (1.0 - k) * (1.0 - exp(-(x - k) / (1.0 - k)));
    return mix(x, over, step(vec3(k), x));
}

// c: the light's colour as it's drawn (before EXPOSURE); texcoord: where on the screen (0 to 1); fragCoord: the pixel.
vec3 retroLook(vec3 c, vec2 texcoord, vec2 fragCoord) {
    c *= EXPOSURE;
    c = rolloff(max(c, vec3(0.0)));
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = max(mix(vec3(l), c, SATURATION), vec3(0.0));
    vec2 d = texcoord - 0.5;
    c *= 1.0 - VIGNETTE * smoothstep(0.18, 0.75, dot(d, d) * 2.0);
    c = pow(c, vec3(1.0 / 2.2));
    // The darks lifted a touch (shade reads soft, not black), the brights left alone.
    float ls = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c += LIFT * (1.0 - smoothstep(0.0, 0.6, ls)) * (vec3(0.92, 0.96, 1.0) - c * 0.5);
    // A whisper of noise, so the night sky's gradient doesn't band.
    float n = fract(52.9829189 * fract(dot(fragCoord, vec2(0.06711056, 0.00583715))));
    c += (n - 0.5) / 255.0;
    return c;
}
