// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The sun's (or moon's) view of the world, for the shadows. Only outside: buildings' insides have none.

#define SHADOW_PASS
#include "/lib/settings.glsl"
#include "/lib/shadow.glsl"

uniform sampler2D gtexture;
uniform float alphaTestRef;

#ifdef VSH
out vec2 uv;
out float alpha;
void main() {
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    alpha = gl_Color.a;
    vec4 p = ftransform();
    p.xy = distortShadow(p.xy);
    gl_Position = p;
}
#endif

#ifdef FSH
in vec2 uv;
in float alpha;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, uv);
    if (c.a * alpha < 0.1) discard;
    outColor = c;
}
#endif
