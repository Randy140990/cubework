// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// After everything solid is drawn and before any water: a copy of the picture, so the water can show what's
// behind it (the bottom, bent by the waves and coloured by the depth) and mirror the world around it.

/*
const int colortex1Format = RGBA16F;
const bool colortex1Clear = false;
*/

uniform sampler2D colortex0;

#ifdef VSH
out vec2 texcoord;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 texcoord;
/* RENDERTARGETS: 1 */
layout(location = 0) out vec4 outBehind;
void main() {
    outBehind = vec4(texture(colortex0, texcoord).rgb, 1.0);
}
#endif
