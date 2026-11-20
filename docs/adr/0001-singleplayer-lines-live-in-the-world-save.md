# Singleplayer Lines live inside the world's save folder

In singleplayer, Lines are saved inside the world's save folder, at `saves/<world>/protractor/lines.json`. They are not kept beside the multiplayer files under the game directory, even though Protractor is a client-only mod and singleplayer and multiplayer storage therefore end up in two different places. Lines describe one particular world. Keeping them in its folder means copying a world copies its Lines, deleting a world removes them, and renaming a world has no effect, because renaming doesn't change the folder name. Multiplayer servers have no local save folder, so their Lines stay at `<gameDir>/protractor/servers/<address>.json`.

## Considered Options

- **Everything under the game directory**, at `protractor/worlds/<save-folder-name>.json`. This keeps one storage root, but a copied world starts with no Lines, a deleted world leaves an orphan file behind, and two game instances that share a folder name would overwrite each other's Lines.

## Consequences

- The mod writes a folder into the world save. Vanilla Minecraft and other tools ignore it.
- Moving this location later would mean migrating players' existing Lines, so don't "tidy up" storage into one root without reading this first.
