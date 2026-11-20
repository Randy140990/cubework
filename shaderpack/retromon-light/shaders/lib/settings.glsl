// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.
// Settings (the pack's own menu).

#define SUN_STRENGTH 1.0 // [0.6 0.8 1.0 1.2 1.4]
#define NIGHT_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.25 1.5]
#define SHADOW_SOFTNESS 1.0 // [0.5 1.0 1.5 2.0]
#define SHADOW_STRENGTH 0.85 // [0.6 0.7 0.85 1.0]
#define INSIDE_BRIGHTNESS 1.0 // [0.8 0.9 1.0 1.1 1.2]

const int shadowMapResolution = 1536; // [1024 1536 2048]
const float shadowDistance = 80.0; // [48.0 64.0 80.0 96.0 128.0]
const float shadowDistanceRenderMul = 1.0;
const float sunPathRotation = -25.0;
const bool shadowHardwareFiltering = true;
const float ambientOcclusionLevel = 1.0;

// How much the shadow map is squeezed towards you (more detail close by).
#define SHADOW_DISTORT 0.85

// The picture.
#define EXPOSURE 0.62 // [0.5 0.55 0.62 0.7 0.8]
#define SATURATION 1.08 // [0.9 1.0 1.08 1.15 1.25]
#define VIGNETTE 0.14 // [0.0 0.07 0.14 0.22]

#define INFO 0 // [0]
