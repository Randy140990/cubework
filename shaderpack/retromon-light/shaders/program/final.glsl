// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The picture: the light's range brought into the screen's (gently, so nothing burns out), a touch of colour
// and a soft darkening at the corners. No blur, no glare, no changing brightness. (1.2.9) Inside, everything's
// had these as it was drawn (lib/look.glsl), so the picture goes straight through.

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

#include "/lib/look.glsl"

void main() {
    #ifdef INSIDE
    fragColor = vec4(texture(colortex0, texcoord).rgb, 1.0);
    #else
    fragColor = vec4(retroLook(texture(colortex0, texcoord).rgb, texcoord, gl_FragCoord.xy), 1.0);
    #endif
}
#endif
