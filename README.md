# Lightlens (working name)

Record video and take photos while you play Minecraft 1.21.11 (Fabric), at 1K, 2K or 4K, with your shaders
and everything on your screen. The video is encoded on your graphics card as you play, straight to the folder
you pick, so there is no export afterwards: stopping takes seconds.

By Insid3 Th3 Cub3, a LumenForge mod. Not an official Minecraft product.

## Keys
- **F10** start / stop recording
- **F12** take a photo
- Pause / resume recording and the settings screen have no key until you set one (Options, Controls, Lightlens).
- The settings screen is also the **Lightlens** button at the top left of the pause menu.

The keys also work with a menu open (inventory, Axiom, Litematica...), like F2.

## How it works
- **Video**: each finished frame (what you see: world, shaders, hand, HUD, menus, other mods' windows) is copied
  on the graphics card to the video size, read back without stalling, and piped with its time to FFmpeg, which
  encodes it with NVENC, AMF, Quick Sync or VideoToolbox into `.minecraft/lightlens/work`. On stop, the game's
  sound (rebuilt from the sounds that played, with your volume settings and where each sound was) and your mic
  are added, and the MP4 is saved in your folder. Video stream is copied, never encoded twice.
- **Sharper** (off by default): the game draws at the video size (2x on a 1080p screen for 4K) and the window
  shows it shrunk. Real 4K detail on a smaller screen, at the cost of frame rate. Mods that draw their own
  windows with their own sizing (ImGui ones) may look odd in this mode.
- **Photos**: up to your window size, the finished frame as you see it. Bigger than the window, the game draws
  a few frames at 2x or 3x (waiting for chunks via Flawless Frames and for shaders to settle), and the photo is
  taken from those and resized with an area filter.
- **FFmpeg**: an LGPL build, downloaded once into `.minecraft/lightlens/tools` (the Get FFmpeg button). If
  Lightreel already downloaded one, that's used.
- If the game closes mid-recording, the picture is saved as "(recovered)" on the next start.

## Build
Compiled on GitHub Actions (check branches). Mojang names, Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11,
Java 21.
