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
#ifdef WAVING_PROGRAM
#include "/lib/wave.glsl"
#endif
#ifndef WAVING_PROGRAM
uniform float frameTimeCounter;
uniform vec3 cameraPosition;
#endif
#ifdef WATER_PROGRAM
#include "/lib/water.glsl"
#endif
#ifndef INSIDE
#include "/lib/clouds.glsl"
#endif

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

#if defined WAVING_PROGRAM || defined WATER_PROGRAM
in vec4 mc_Entity;
in vec4 at_midBlock;
#endif
out vec2 uv;
out vec2 lm;
out vec4 vcol;
out vec3 nView;
out vec3 viewPos;
flat out float blockId;
flat out float emit;

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
    blockId = 0.0;
    emit = 0.0;
    #if defined WAVING_PROGRAM || defined WATER_PROGRAM
    blockId = mc_Entity.x;
    emit = clamp(at_midBlock.w / 15.0, 0.0, 1.0);
    #endif
    #ifdef WAVING_PROGRAM
    vec3 pp = (gbufferModelViewInverse * vp).xyz;
    pp += wave(pp, mc_Entity.x, at_midBlock);
    vp = gbufferModelView * vec4(pp, 1.0);
    #endif
    #if defined WATER_PROGRAM && !defined INSIDE
    // The swell: the top of the water rises and falls, so its waves have real height.
    if (int(mc_Entity.x + 0.5) == 10003 && (mat3(gbufferModelViewInverse) * nView).y > 0.5) {
        vec3 pw = (gbufferModelViewInverse * vp).xyz;
        pw.y += waterSwell(pw.xz + cameraPosition.xz, waterClock());
        vp = gbufferModelView * vec4(pw, 1.0);
    }
    #endif
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
flat in float blockId;
flat in float emit;

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
    #ifndef CLOUDS
    // Under a passing cloud, less sun (its shadow drifting over the land).
    vec3 cloudAt = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;
    sunlit *= cloudShadow(cloudAt.xz, frameTimeCounter);
    #endif
    vec3 skyPart, lampPart;
    worldLight(n, lm, sunlit, skyPart, lampPart);
    // Night vision: Retromon lifts it a little as night falls (OPTION's NIGHTS says how much), so here it brings
    // in more of the night's blue light rather than turning everything grey. Rooms are lit as they are.
    skyPart += vec3(0.14, 0.22, 0.46) * nightVision * (0.35 + 0.65 * lm.y);
    // By moonlight colours fade towards blue-grey (as our eyes see them at night); lamplight keeps them warm.
    float nightK = nightAmount() * NIGHT_BLUE;
    vec3 moonAlbedo = mix(albedo, vec3(dot(albedo, vec3(0.2126, 0.7152, 0.0722))), clamp(nightK * 0.55, 0.0, 0.9));
    vec3 light = skyPart + lampPart;
    #endif

    #ifdef EMISSIVE
    light = max(light, vec3(1.0));
    #endif
    // Lamps, torches and other light blocks glow: their bright parts (the flame, the glass), not their frames.
    if (emit > 0.0) {
        float bright = smoothstep(0.30, 0.75, max(c.r, max(c.g, c.b)));
        light = max(light, vec3(2.4 * emit * bright * LAMP_BRIGHTNESS));
    }
    #ifdef INSIDE
    vec3 col = albedo * light;
    #else
    vec3 col = moonAlbedo * (light - lampPart) + albedo * lampPart;
    #endif
    float alpha = c.a;

    #if defined WATER_PROGRAM && !defined INSIDE
    if (int(blockId + 0.5) == 10003) {
        vec3 pPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz;
        vec3 wPos = pPos + cameraPosition;
        float wDist = length(pPos);
        bool below = isEyeInWater == 1;
        // The waves, on the top of the water (facing you, whichever side you see it from).
        vec3 nW = mat3(gbufferModelViewInverse) * n;
        if (abs(nW.y) > 0.5) {
            vec3 wn = waterNormal(wPos, wDist);
            nW = nW.y > 0.0 ? wn : -wn;
        }
        vec3 nV = mat3(gbufferModelView) * nW;
        vec3 v = normalize(viewPos);
        float NdotV = clamp(dot(nV, -v), 0.0, 1.0);
        vec3 direct, ambient;
        skyLight(direct, ambient);
        vec3 lW = mat3(gbufferModelViewInverse) * normalize(shadowLightPosition);
        // How much sun is on the water here (its shadows, a passing cloud's), 0 to 1.
        float sunVis = clamp(sunlit / max(NdotL, 0.05), 0.0, 1.0);

        // What's behind it: the bottom, a little bent by the waves (never onto anything in front of the water).
        vec2 sc = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
        float d1 = texture(depthtex1, sc).r;
        float depth = d1 >= 1.0 ? 64.0 : distance(viewPos, screenToView(sc, d1));
        vec2 rUV = sc;
        float refr = below ? 0.09 : 0.035 * clamp(depth / 3.0, 0.0, 1.0);
        vec2 tryUV = clamp(sc + nV.xy * refr / max(wDist * 0.06, 1.0), vec2(0.001), vec2(0.999));
        float dT = texture(depthtex1, tryUV).r;
        vec3 bedT = screenToView(tryUV, dT);
        if (dT >= 1.0 || bedT.z < viewPos.z) {
            rUV = tryUV;
            if (dT < 1.0) depth = mix(depth, distance(viewPos, bedT), 0.7);
            d1 = dT;
        }
        vec3 behind = texture(colortex1, rUV).rgb;

        vec3 body;
        if (!below) {
            // Caustics: the sunlight on the bottom gathered into a moving web of light, fading with depth.
            if (d1 < 1.0) {
                vec3 bedW = (gbufferModelViewInverse * vec4(screenToView(rUV, d1), 1.0)).xyz + cameraPosition;
                float day = smoothstep(0.0, 0.12, lW.y);
                behind *= mix(1.0, caustics(bedW), 0.5 * sunVis * day * (0.4 + 0.6 * exp(-depth / 14.0)) * smoothstep(0.0, 0.4, depth));
            }
            // The water takes the red out first, then the green: clear in the shallows, turquoise, then blue.
            vec3 absorb = waterAbsorption(), scat = waterScatter();
            vec3 T = exp(-(absorb + scat) * depth);
            // Light gets into the water from the sky and the sun, and some of it scatters back out towards you.
            float sky = lm.y;
            vec3 lightIn = ambient * sky * sky + direct * 0.32 * clamp(lW.y, 0.0, 1.0) * smoothstep(0.8, 0.97, sky) * sunVis;
            // Minecraft's colour for the water here nudges the light it scatters.
            vec3 biome = pow(vcol.rgb, vec3(2.2));
            biome = mix(vec3(1.0), biome / max(dot(biome, vec3(0.2126, 0.7152, 0.0722)), 0.05), 0.35);
            vec3 inscatter = lightIn * scat / (absorb + scat) * (1.0 - T) * 0.45 * biome;
            body = behind * T + inscatter;
        } else {
            // From under the water: the world above through the surface, tinted by the water you're in.
            body = behind * vec3(0.62, 0.86, 0.95);
        }

        // The sky and the world mirrored in it.
        vec3 r = reflect(v, nV);
        vec3 refl;
        if (!below) {
            vec3 rW = mat3(gbufferModelViewInverse) * r;
            if (rW.y < 0.02) rW = normalize(vec3(rW.x, 0.02, rW.z));
            refl = pow(skyAt(mat3(gbufferModelView) * rW), vec3(2.2)) / EXPOSURE * (0.12 + 0.88 * lm.y * lm.y);
            vec4 mirror = waterMirror(viewPos, r);
            refl = mix(refl, mirror.rgb, mirror.a);
        } else {
            refl = pow(fogColor, vec3(2.2)) * 0.6;
        }
        float F = 0.02 + 0.98 * pow(1.0 - NdotV, 5.0);
        if (below) {
            // Past the critical angle the surface above is a mirror.
            float sinT = 1.333 * sqrt(1.0 - NdotV * NdotV);
            F = sinT >= 1.0 ? 1.0 : mix(F, 1.0, smoothstep(0.95, 1.0, sinT));
        }
        col = mix(body, refl, F);

        if (!below) {
            // The sun (or the moon) on the water: its own image, a soft path round it, and close by small ripples
            // glittering where they catch it.
            vec3 vW = -normalize(pPos);
            float tg = waterClock() * 2.2;
            vec2 sl = vec2(0.0);
            for (int k = 0; k < 6; k++) {
                float ang = float(k) * 2.39996 + 0.7;
                vec2 dk = vec2(cos(ang), sin(ang));
                float kk = 9.0 * pow(1.38, float(k));
                float ph = dot(wPos.xz, dk) * kk - tg * sqrt(kk) * 1.6 + float(k) * 1.9;
                sl += dk * cos(ph) * (0.06 / (1.0 + 0.25 * float(k)));
            }
            float near = exp(-wDist / 40.0);
            vec3 nF = normalize(nW + vec3(sl.x, 0.0, sl.y) * near);
            float rl = max(dot(reflect(-vW, nW), lW), 0.0);
            float spark = smoothstep(0.9982, 0.99985, dot(reflect(-vW, nF), lW)) * near;
            float fF = 0.02 + 0.98 * pow(1.0 - clamp(dot(nF, vW), 0.0, 1.0), 5.0);
            float sunUp = smoothstep(-0.02, 0.12, lW.y);
            col += direct * sunVis * (pow(rl, 900.0) * 2.2 + pow(rl, 40.0) * 0.05 + spark * fF * 16.0 * sunUp);
        }
        alpha = 1.0;
    }
    #endif

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
    outColor = vec4(col, alpha);
}

#endif
