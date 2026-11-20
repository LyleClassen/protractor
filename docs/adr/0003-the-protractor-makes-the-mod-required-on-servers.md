# The Protractor makes the mod required on servers

Anchors are set with an item, the Protractor, rather than keybinds: right-click sets an Anchor, sneak + right-click cancels a pending Anchor or deletes the Target, and a sneak + double right-click clears the dimension's Lines. A registered item has to exist on the server too, so the mod is no longer client-only. On multiplayer it must be installed on both sides; singleplayer is unaffected.

Only the item is registered on both sides. Its behaviour is client-side: the right-click is intercepted and cancelled before vanilla handles it, so nothing reaches the server, and Lines are still stored on the client as ADR 0001 describes.

## Considered Options

- **Keep the keybinds.** Stays client-only, but the player asked for a tool like the Measurements tape measure.
- **Reuse a vanilla item as the tool.** Stays client-only, but steals that item's right-click and can't have its own recipe.
