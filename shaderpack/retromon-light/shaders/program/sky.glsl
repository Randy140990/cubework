// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// The sky: Minecraft's own sky colours with a warm glow round the low sun and a deep blue night; the sun, moon
// and stars as Minecraft draws them. Inside a building there's no sky: all round the room is black.

#include "/lib/settings.glsl"
#include "/lib/light.glsl"
#include "/lib/sky.glsl"

uniform sampler2D gtexture;
uniform int renderStage;
uniform float viewWidth;
uniform float viewHeight;
uniform mat4 gbufferProjectionInverse;

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
    #ifdef INSIDE
    #ifdef SKY_TEXTURED
    discard;
    #else
    outColor = vec4(0.0, 0.0, 0.0, 1.0);
    #endif
    #else
    #ifdef SKY_TEXTURED
    // The sun and the moon (and anything a resource pack draws in the sky).
    vec4 c = texture(gtexture, uv) * vcol;
    if (c.a < 0.01) discard;
    float boost = renderStage == MC_RENDER_STAGE_SUN ? 2.2 : 1.3;
    outColor = vec4(pow(c.rgb, vec3(2.2)) * boost, c.a);
    #else
    if (renderStage == MC_RENDER_STAGE_STARS) {
        outColor = vec4(pow(vcol.rgb, vec3(2.2)) * 1.4 / EXPOSURE, vcol.a);
        return;
    }
    if (renderStage == MC_RENDER_STAGE_SUNSET) {
        // Minecraft's sunset band: our own glow does this, so it's left out.
        discard;
    }
    vec4 p = gbufferProjectionInverse * vec4(gl_FragCoord.xy / vec2(viewWidth, viewHeight) * 2.0 - 1.0, 1.0, 1.0);
    vec3 dir = normalize(p.xyz / p.w);
    outColor = vec4(pow(skyAt(dir), vec3(2.2)) / EXPOSURE, 1.0);
    #endif
    #endif
}
#endif
