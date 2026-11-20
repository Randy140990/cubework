// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// Everything that's lit: blocks, water, block entities, people and creatures, your hand, particles, rain, clouds.
// One pass, lit as it's drawn. Outside: the sun (or moon) moving with the day, its soft shadows, the sky's light
// and lamps. Inside a building: the room's own light, which never changes with the time of day.

#include "/lib/settings.glsl"
#include "/lib/light.glsl"
#ifdef SUN_SHADOWS
#include "/lib/shadow.glsl"
#endif
#include "/lib/sky.glsl"

uniform sampler2D gtexture;
uniform float alphaTestRef;
uniform float far;
uniform int isEyeInWater;
uniform float blindness;
uniform float darknessFactor;
uniform float nightVision;
#ifdef ENTITY
uniform vec4 entityColor;
#endif

#ifdef VSH

out vec2 uv;
out vec2 lm;
out vec4 vcol;
out vec3 nView;
out vec3 viewPos;

void main() {
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    #ifdef CLOUDS
    lm = vec2(0.0, 1.0);
    #else
    lm = levels((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
    #endif
    vcol = gl_Color;
    #ifdef FLAT
    // Particles, rain and clouds have no real facing: lit as if facing up.
    nView = normalize(mat3(gbufferModelView) * vec3(0.0, 1.0, 0.0));
    #else
    nView = normalize(gl_NormalMatrix * gl_Normal);
    #endif
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;
}

#endif

#ifdef FSH

in vec2 uv;
in vec2 lm;
in vec4 vcol;
in vec3 nView;
in vec3 viewPos;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 c = texture(gtexture, uv) * vcol;
    if (c.a < alphaTestRef) discard;
    #ifdef ENTITY
    c.rgb = mix(c.rgb, entityColor.rgb, entityColor.a);
    #endif
    vec3 albedo = pow(c.rgb, vec3(2.2));
    vec3 n = normalize(nView);
    #ifndef FLAT
    if (!gl_FrontFacing) n = -n;
    #endif

    #ifdef INSIDE
    vec3 light = roomLight(n, lm);
    #else
    vec3 l = normalize(shadowLightPosition);
    float NdotL = clamp(dot(n, l), 0.0, 1.0);
    #ifdef FLAT
    NdotL = 0.65;
    #endif
    #ifdef CLOUDS
    NdotL = dot(n, l) * 0.5 + 0.5;
    #endif
    #ifdef SUN_SHADOWS
    vec3 playerPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz;
    vec3 nPlayer = mat3(gbufferModelViewInverse) * n;
    float sunlit = sunlight(playerPos, nPlayer, NdotL);
    #else
    float sunlit = NdotL;
    #endif
    vec3 light = worldLight(n, lm, sunlit);
    // Night vision: Retromon lifts it a little as night falls (OPTION's NIGHTS says how much), so here it brings
    // in more of the night's blue light rather than turning everything grey. Rooms are lit as they are.
    light += vec3(0.16, 0.22, 0.42) * nightVision * (0.35 + 0.65 * lm.y);
    #endif

    #ifdef EMISSIVE
    light = max(light, vec3(1.0));
    #endif
    vec3 col = albedo * light;

    // Fog: into the sky at the edge of what's drawn (as Minecraft does), thick under water and in lava.
    float dist = length(viewPos);
    #ifdef INSIDE
    vec3 fogTo = vec3(0.0);
    float fog = 0.0;
    #else
    vec3 fogTo = pow(skyAt(normalize(viewPos)), vec3(2.2)) / EXPOSURE;
    float fog = smoothstep(far * 0.72, far, dist);
    #endif
    if (isEyeInWater == 1) {
        fogTo = pow(fogColor, vec3(2.2)) * 0.6;
        fog = 1.0 - exp(-dist * 0.045);
    } else if (isEyeInWater == 2) {
        fogTo = vec3(0.6, 0.12, 0.01);
        fog = smoothstep(0.0, 2.0, dist);
    } else if (isEyeInWater == 3) {
        fogTo = vec3(0.75, 0.8, 0.85);
        fog = smoothstep(0.0, 2.5, dist);
    }
    float dark = max(blindness, darknessFactor);
    if (dark > 0.0) {
        fog = max(fog, smoothstep(2.0, 8.0, dist) * dark);
        fogTo *= 1.0 - dark;
    }
    col = mix(col, fogTo, fog);
    outColor = vec4(col, c.a);
}

#endif
