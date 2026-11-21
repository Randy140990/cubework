# Lightlens 0.1.0 (working name): in-game recording and photos (21 Nov 2026)

John's ask: a mod to record video and take photos while playing, unlike Flashback, Replay Mod or Lightreel (no replay or export step). It should:
- capture what he sees, including shaders (Tempered Light), menus and other mods' UIs (Axiom, Litematica);
- offer 1K, 2K and 4K output;
- write to a folder chosen before recording, processing as it records;
- take photos at a chosen size, unlike F2.

Built for 1.21.11 Fabric, alongside Iris, Sodium and the rest of his mod list.

Jar: `Lightlens-0.1.0-for-MC1.21.11.jar` (125 KB). Mod id `lightlens`, package `com.lumenforge.lens`, Mojang names. It needs Fabric API only; Iris and Sodium are optional.

## Where it lives
- Code: branch `check/lightlens-0.1` on `Randy140990/cubework`, the scratch build repo that Retromon also uses. It's an orphan branch with its own tree.
- CI: the compile and jar results go to `ci-check-lightlens`. When a commit message contains `[ingame]`, an in-game test also runs, and its results go to `ci-test-lightlens-vanilla` and `ci-test-lightlens-shaders`.
- No repo of its own yet. One is only needed for public releases.

## How it works
- **Video (default)**
  - At `Window.updateDisplay` HEAD, the back buffer holds the finished frame: world, Iris output, hand, HUD, menus, and any mod that draws straight to the window.
  - It's blitted on the GPU to the output size (16:9 centre crop) and flipped, then read back through a ring of 4 PBOs with fences, so nothing stalls.
  - Frames are piped to FFmpeg as Matroska `V_UNCOMPRESSED` RGBA, each with its capture time (`core/MkvWriter`). FFmpeg runs `-fps_mode cfr`, so slow game frames repeat and audio stays in sync.
  - Frame slots are phase-locked to the first frame, with 30% tolerance.
  - Encoding happens live: NVENC takes RGBA directly; AMF, QSV, MF and VideoToolbox get NV12 converted in FFmpeg. The output goes to `.minecraft/lightlens/work/<take>/video.mkv`, which stays readable after a crash.
- **Stop**
  - The game sound is rebuilt (below), the mic WAV is added, and the result is muxed with `-c:v copy` and AAC. It's written to `<folder>/Lightlens <date> at <time>.mp4.part`, then renamed.
  - A silent track is added if nothing played. Then the work folder is deleted.
- **Game sound**
  - A mixin on `SoundEngine.play` RETURN, `stop` and `stopAll`.
  - Every client tick, each live voice gets a keyframe: volume (with `getFinalSoundSourceVolume`), linear attenuation, pan from `ListenerTransform`, pitch, and whether it's paused (only MUSIC and UI keep playing in the pause menu).
  - `core/SoundMix` mixes the voices into a WAV one second at a time. Clips are decoded with STBVorbis, using an LRU cache of 48M samples. Stereo clips aren't spatialised, as in OpenAL.
- **Sharper (off by default)**
  - `RenderScale` multiplies `Window.getWidth`, `getHeight` and `getGuiScale` by a whole number k (2x for 4K on a 1080p screen), then calls `resizeDisplay`. GUI-scaled sizes stay the same, so menus and the mouse are unchanged.
  - Frames are taken from the main target at `RenderTarget.blitToScreen` HEAD. The window gets our own scaled blit, and the vanilla 1:1 present is cancelled.
  - ImGui-style mods may misplace their windows in this mode.
- **Photos**
  - Up to window size: the back buffer, as you see it.
  - Bigger: k = ceil(target / window), capped at 4. Flawless Frames is on for 14 warm-up frames (at least 300 ms), then the main target is read at k×.
  - In both cases the picture is resized on the CPU with an area filter and saved as PNG or JPG (0.95) to `<folder>/Photos`. The size is restored after the frame is shown.
- **The red dot and clock**
  - Drawn with scissored clears onto the window after the capture, so they're never in the video. The window title also shows ● REC.
  - A photo taken while recording flashes the dot white, with no toast.
- **Keys**
  - F10 record and F12 photo. Pause and the settings screen are unbound.
  - The keys also work with a screen open, through a `KeyboardHandler.keyPress` mixin (function keys only).
  - The pause menu has a "Lightlens" button at the top left.
- **FFmpeg**
  - A BtbN LGPL build, downloaded to `.minecraft/lightlens/tools`. Lightreel's copy in `lightreel/tools/ffmpeg` is used if it's there.
  - Encoders are checked at start-up in the background: h264 `nvenc > amf > qsv > videotoolbox > mf > libx264 > libopenh264 > mpeg4`, and hevc hardware only.
- **Safety**
  - Free space is checked every 3 s, and recording stops below 1 GB.
  - If the writer fails, recording stops and the take is kept.
  - On quitting, the game waits up to 2 minutes for the MP4 to finish.
  - A take left over from a crash is saved as "(recovered)" on the next start.
- **Lightreel**: while Lightreel is exporting, Lightlens waits, because both mods resize the game's frame.

## Sizes (High, H.264, per 10 min)
| | 30 fps | 60 fps |
| --- | --- | --- |
| 1080p | 915 MB | about 1.4 GB |
| 4K | about 3.2 GB | 4.8 GB |

H.265 is about 60% of these. Good is 0.6× and Max is 1.6×.

## Checked
- Compiled in CI. Mixin targets were checked against 1.21.11 sources, and their intermediary remap was checked in the jar.
- Core pieces tested locally against FFmpeg 6.1:
  - The timed Matroska stream: 286 uneven frames with a 1 s stall gave exactly 360 frames over 12.000 s, and 4K frames worked.
  - The mixer, the MP4 mux and the silent track.
  - The area resize.
- In-game CI test on a real 1.21.11 client (Mesa llvmpipe, libx264, PulseAudio null sink), run plain and with Sodium 0.8.15 and Iris:
  - Video 1 was 1920x1080 at 30 fps, 24 s, H.264 and AAC, with real game sound (peak -1.9 dB). The creative inventory opened mid-recording is in the video.
  - The 4K photo taken while recording was 3840x2160, as was the 4K photo afterwards; the Screen photo was 1280x720.
  - Sharper from 720p rendered at 2x and was visibly crisper. The window size was restored afterwards.
  - No frames were dropped.
- Shaders: Tempered Light 2.4.4 (Lightreel's CI copy) doesn't compile on the Mesa core profile, so the CI shader run uses Complementary Reimagined.
  - Its 3D clouds and lighting show in the recorded frames and in the 4K photo.
  - The settings screen reads "Shaders: Complementary, in your video".
  - Even at under 1 fps on llvmpipe, recording stayed in sync (173 s, 110 frames captured, 0 dropped).

## For John's test
- Pick a folder; if needed, use Get FFmpeg; then press F10 and F12.
- Check that the encoder shows NVENC or AMF, and the shaders line shows Tempered Light.
- Try 4K at 60 with Sharper off and on, Axiom's UI while recording, and the size and time to finish a 10-minute clip.

## 0.1.1 (21 Nov, after John's first test)

John's report:
- F10 gave no sign of recording; only something flashing at the top saying "0", probably the "●" in the window title.
- He wanted the keys to work only in game.
- He asked for a 10-second "shutter" photo.
- He wasn't sure the photos kept detail, and asked whether a 988 KB JPG is 4K.

Cause: FFmpeg 8 (the BtbN master build that Get FFmpeg downloads, and Lightreel's copy too) refuses `-thread_queue_size` on an input. FFmpeg exited at once, so every recording stopped as it started. The CI had used Ubuntu's FFmpeg 6.1, which accepts it.

Fixes:
- The option is removed. The live command and the encoder check now share `Ffmpeg.pipeIn`.
- The encoder check sends the real stream (timed Matroska RGBA through the pipe) with the real encoder settings. If the tuned settings fail, it falls back to plain ones (`-c:v enc -b:v -g`) for that encoder.
- CI's plain run now uses the BtbN master linux64 LGPL build (env `LIGHTLENS_FFMPEG`).
- Each key press does one thing, with repeats within 1 s ignored. Before this, holding F10 started and then stopped a recording.
- Keys only work in a world. Leaving the world stops and saves.
- Failures are loud: a toast, a red chat line, a "Last time" line on the screen, and `.minecraft/lightlens/last-error.txt` with FFmpeg's log.
- Cue sounds: a high pling at start (before game sound is noted, so it isn't recorded), a lower one at stop, and a click for a photo when not recording.
- The REC box is bigger (u = height/216) and has a red underline.
- Photo Wait setting: None, 3 s or 10 s. For a big photo the game draws at full size for the whole wait, so the shaders settle. The countdown box sits in the top right and is never in the photo. Pressing F12 again takes the photo straight away.
- File names include the size: "Lightlens <date> at <time> (3840x2160).png" and "(1920x1080 60fps).mp4".

Photos checked against John's Print Screen: the 2K photo is sharper and less grainy. The chat resizes uploads (to about 2576 wide), so 4K has to be checked from the file's Properties.

CI (61baa90) with BtbN FFmpeg master:
- Video 1 is 14 s with sound; Sharper video is 5 s; no frames were dropped.
- The 4K photo with the 3 s wait and countdown was 3840x2160, and the Screen photo was 1280x720.
- The new names show in chat.

Jar: `Lightlens-0.1.1-for-MC1.21.11.jar`.
