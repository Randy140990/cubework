# Retromon Light

Retromon's own shader pack (for Iris). The source is in `retromon-light/shaders`; the mod carries it zipped as
`src/main/resources/retromon/shaders/Retromon-Light-1.2.2.zip` and puts it in the shaderpacks folder when you play.

Rebuild the zip after changing the pack (and give it a new version number in the name, Shaders.java and
`shaders.properties` / `lang` when it's a release):

    cd shaderpack/retromon-light && rm -f ../../src/main/resources/retromon/shaders/Retromon-Light-1.2.2.zip && \
      zip -qr -X ../../src/main/resources/retromon/shaders/Retromon-Light-1.2.2.zip shaders

What it does:
- One pass, lit as it's drawn (no deferred passes, no blur, no changing exposure), so it's quick.
- Outside (`world0`): the sun and moon move with the day; soft shadows; warm sunsets; blue, clear nights;
  lamps warm; rain dims; fog into the sky at the edge of view.
- Inside (`interior`, for `retromon:interiors`): no sun and no shadow pass. Rooms are lit by their own light
  (their lamps), the same at every time of day. All round the room is black.
