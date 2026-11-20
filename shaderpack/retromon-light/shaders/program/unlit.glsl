// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// What isn't lit: outlines, glints, cracks, spider eyes, beacon beams, lightning. Drawn as Minecraft draws them.

#include "/lib/settings.glsl"

uniform sampler2D gtexture;
uniform float alphaTestRef;

#ifdef VSH
out vec2 uv;
out vec4 vcol;
void main() {
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    vcol = gl_Color;
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 uv;
in vec4 vcol;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    #ifdef NO_TEXTURE
    vec4 c = vcol;
    #else
    vec4 c = texture(gtexture, uv) * vcol;
    #endif
    if (c.a < alphaTestRef) discard;
    #ifdef GLOW
    c.rgb *= 1.6;
    #endif
    outColor = vec4(pow(c.rgb, vec3(2.2)) / EXPOSURE, c.a);
}
#endif
