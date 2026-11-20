// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The picture: the light's range brought into the screen's (gently, so nothing burns out), a touch of colour
// and a soft darkening at the corners. No blur, no glare, no changing brightness.

/*
const int colortex0Format = RGBA16F;
const bool colortex0Clear = true;
*/

#include "/lib/settings.glsl"

uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;

#ifdef VSH
out vec2 texcoord;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 texcoord;
layout(location = 0) out vec4 fragColor;

// As it is up to 0.8, then rolling off towards 1 (never quite reaching it), so bright sun doesn't flatten.
vec3 rolloff(vec3 x) {
    const float k = 0.8;
    vec3 over = k + (1.0 - k) * (1.0 - exp(-(x - k) / (1.0 - k)));
    return mix(x, over, step(vec3(k), x));
}

void main() {
    vec3 c = texture(colortex0, texcoord).rgb * EXPOSURE;
    c = rolloff(max(c, vec3(0.0)));
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = max(mix(vec3(l), c, SATURATION), vec3(0.0));
    vec2 d = texcoord - 0.5;
    c *= 1.0 - VIGNETTE * smoothstep(0.18, 0.75, dot(d, d) * 2.0);
    c = pow(c, vec3(1.0 / 2.2));
    // A whisper of noise, so the night sky's gradient doesn't band.
    float n = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    c += (n - 0.5) / 255.0;
    fragColor = vec4(c, 1.0);
}
#endif
