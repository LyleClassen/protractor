# v1 targets NeoForge on Minecraft 1.21.1, not 26.3 on Fabric and NeoForge

v1 is built for Minecraft 1.21.1 on NeoForge 21.1 with Java 21, as a single ModDevGradle project. The planning map aimed at Minecraft 26.3 on both Fabric and NeoForge in the MultiLoader layout. The mod is meant to be played in All The Mods 10, which runs Minecraft 1.21.1 on NeoForge, and a 26.3 jar can't load there. The client hooks the map researched for 26.3 (vanilla `Gizmos`, `SubmitCustomGeometryEvent`) don't exist in 1.21.1, so Lines are drawn at NeoForge's `RenderLevelStageEvent` `AFTER_LEVEL` stage and the Measurement is a GUI layer above the hotbar.

The maths stays in a pure `core` package with no Minecraft types, so a later 26.3 or Fabric port can reuse it unchanged.

## Considered Options

- **26.3 on Fabric and NeoForge, as planned.** It can't be tested in the pack the mod is for.
- **Both 1.21.1 and 26.3 in v1.** Nothing from the map would be dropped, but it's twice the loader work before anyone can play it.

## Consequences

- Fabric and Minecraft 26.3 are out of v1. Porting later means new render and input adapters around the same `core`.
- Server Lines are stored under `<gameDir>/protractor/servers/<address>.json` as decided, but characters that aren't safe in a file name (such as the `:` before a port, which Windows rejects) become `_`.
