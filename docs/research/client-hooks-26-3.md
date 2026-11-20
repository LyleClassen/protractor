# Client-side hooks on MC 26.3: Fabric vs NeoForge

Research for issue #5. The question: what Fabric API and NeoForge on Minecraft 26.3 give a **client-only** mod for (1) translucent ghost blocks and outlines, including drawing through walls, (2) floating text and HUD text, (3) keybinds, (4) catching block clicks without the server having the mod, and (5) saving client data per server or world. It also lists every loader difference that common code has to hide.

Versions checked: Minecraft 26.3 (Mojang names, unobfuscated), Fabric API branch `26.3` (the project uses `0.166.2+26.3`), NeoForge branch `26.3.x`, and Mrbysco/Measurements branch `multi/26.3` (NeoForge `26.3.0.37-beta`, Fabric API `0.161.0+26.3`).

## Sources

| Tag | Source |
|---|---|
| [MC] | Vanilla 26.3 class signatures, read straight from the Loom cache jars `minecraft-clientonly-deobf-26.3.jar` and `minecraft-common-deobf-26.3.jar` |
| [FAPI] | `FabricMC/fabric-api` @ `26.3`: source and Javadoc |
| [FDOCS] | `FabricMC/fabric-docs` @ `main`: `develop/rendering/world.md`, `develop/key-mappings.md`, `develop/rendering/hud.md` and their `reference/latest` code |
| [NEO] | `neoforged/NeoForge` @ `26.3.x`: event source and `patches/` (the patches also show the surrounding vanilla code) |
| [MEAS] | `Mrbysco/Measurements` @ `multi/26.3`: a working mod that renders cuboids and text on both loaders |

---

## 1. Ghost blocks and outlines in the world

### How 26.x renders

Rendering is split into an **extraction** phase, where the game reads game state into immutable render state, and a **drawing** phase. World geometry goes through `SubmitNodeCollector`. Debug-style shapes go through the static **Gizmos** API. [FDOCS world.md] [FAPI `LevelRenderEvents` Javadoc]

**Gizmos** (`net.minecraft.gizmos.Gizmos`, common jar) [MC]:
- `Gizmos.cuboid(AABB, GizmoStyle[, boolean coloredCornerStroke])` and `cuboid(BlockPos, [float inflate,] GizmoStyle)`. Also `line`, `arrow`, `rect`, `point`, `circle`, `billboardText(String, Vec3, TextGizmo.Style)` and `billboardTextOverBlock`.
- `GizmoStyle.stroke(argb[, width])`, `fill(argb)` and `strokeAndFill(strokeArgb, width, fillArgb)`. A **filled translucent cuboid with an outline is one call.**
- Each call returns `GizmoProperties` with `setAlwaysOnTop()`, which **draws it through walls**, plus `persistForMillis(int)` and `fadeOut()`.
- Measurements passes `true` as the third argument of `cuboid`. That argument is `coloredCornerStroke`, not always-on-top. [MC `CuboidGizmo(AABB, GizmoStyle, boolean)`]
- Vanilla keeps three gizmo collectors. One is per tick (`Minecraft.collectPerTickGizmos` / `getPerTickGizmos`). One is per frame on the main thread, during extraction (`LevelExtractor.collectPerFrameMainThreadGizmos`). One is per frame on the render thread (`LevelRenderer.collectPerFrameRenderThreadGizmos`). `LevelRenderer.submitFeatures` calls `finalizeGizmoCollection()`, then submits the standard primitives and the always-on-top primitives separately. [MC names; NEO LevelRenderer patch context]

**SubmitNodeCollector / OrderedSubmitNodeCollector** [MC]: `submitText(PoseStack, x, y, FormattedCharSequence, shadow, Font.DisplayMode, light, color, bgColor, outlineColor)`, `submitNameTag`, `submitBlockModel(PoseStack, RenderType, List, int[], ...)`, `submitMovingBlock`, `submitShapeOutline(PoseStack, VoxelShape, RenderType, color, width, ...)`, `submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)` and `order(int)`.

**Render types that can see through walls or skip depth writes** [MC `RenderTypes` / `RenderPipelines`]: `textSeeThrough`, `textGrayscaleSeeThrough`, `linesTranslucentNoDepthWrite` (`LINES_TRANSLUCENT_NO_DEPTH_WRITE`) and `debugFilledBox`. `debugFilledBox` *does* depth-test. `Font.DisplayMode` has `NORMAL`, `SEE_THROUGH` and `POLYGON_OFFSET`. For a fully custom no-depth pipeline, the Fabric docs build one with `RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withDepthStencilState(Optional.empty())`. Drawing it needs a **mixin** into `LevelRenderer.executeClassicTransparency`, because "Fabric API does not provide the render pass" in 26.3. [FDOCS world.md, CustomRenderPipeline.java, LevelRendererMixin.java]

### Fabric: `LevelRenderEvents` / `LevelExtractionEvents` [FAPI]

| Event | Context | Where it fires (from the injection points in `LevelRendererMixin`) |
|---|---|---|
| `LevelExtractionEvents.END_EXTRACTION` | `LevelExtractionContext` (level, camera, deltaTracker) | At the end of `LevelExtractor.extract`. Use it to snapshot game state. |
| `LevelRenderEvents.COLLECT_SUBMITS` | `LevelRenderContext` (`submitNodeCollector()`, `poseStack()`, `levelState()`) | At the RETURN of `submitFeatures`, which is **after** `finalizeGizmoCollection` |
| `LevelRenderEvents.BEFORE_GIZMOS` | `LevelRenderContext` | Inside `submitFeatures`, just **before** `finalizeGizmoCollection()` |
| `AFTER_TRANSLUCENT_TERRAIN` (used by Measurements) | `LevelRenderContext` | Wraps the translucent `renderGroup` in `executeClassicTransparency` only. With improved transparency (OIT) the game takes `executeOit` instead, so this event may **not fire**. Inferred from the mixin targets; not tested. |
| `BEFORE_RENDER_PASS`, `AFTER_RENDER_PASS`, `END_MAIN`, `AFTER_SOLID_FEATURES`, `AFTER_TRANSLUCENT_FEATURES`, `BEFORE_BLOCK_OUTLINE` | various | Lower-level hooks for raw render passes or the block outline |

### NeoForge [NEO]

| Event (game bus) | Gives | Where it fires |
|---|---|---|
| `ExtractLevelRenderStateEvent` | `LevelRenderState` (with `setRenderData(ContextKey, T)`), level, camera, frustum, deltaTracker | After vanilla extraction |
| `SubmitCustomGeometryEvent` | `getSubmitNodeCollector()`, `getPoseStack()`, `getLevelRenderState()` | In `submitFeatures`, after the block outline and **just before `finalizeGizmoCollection()`**, at the same point as Fabric `BEFORE_GIZMOS` |
| `RenderLevelStageEvent.*` (`AfterSky`, `AfterOpaqueBlocks`, `AfterOpaqueFeatures`, `AfterTranslucentFeatures`, `AfterTranslucentBlocks`, `AfterTranslucentParticles`, `AfterWeather`, `OitTranslucent`, `AfterLevel`) | an active `RenderPass` (except AfterSky/AfterLevel) | For raw drawing. The Javadoc says to use `SubmitCustomGeometryEvent` when submitting nodes. |

NeoForge also patches always-on-top handling to cover **non-gizmo** always-on-top submits (`featureFrame.hasAnyAlwaysOnTop()`). That is a NeoForge-only extension; do not rely on it from common code.

### Recommendation

- **Hook:** submit from Fabric `BEFORE_GIZMOS` and NeoForge `SubmitCustomGeometryEvent`. Both get `(SubmitNodeCollector, PoseStack, LevelRenderState)`, both fire in `submitFeatures` before gizmos are finalized, and both fire under classic and OIT transparency alike. Common code can expose one `renderWorld(collector, poseStack, cameraPos)` behind a platform service.
- **Ghost blocks:** use `Gizmos.cuboid(new AABB(pos), GizmoStyle.strokeAndFill(edge, width, translucentFill))` once per Line block. Add `.setAlwaysOnTop()` for an X-ray mode. This needs no custom pipeline and no mixin. A fallback for textured ghost blocks is `submitBlockModel` with a translucent `RenderType`, but it cannot show through walls without a custom pipeline.
- **Open item to prototype:** check that gizmos emitted from these render-thread events land in the **same** frame. The events fire before `finalizeGizmoCollection`, so they should, and Measurements shows it works in practice. If they flicker or lag a frame, emit from extraction instead (Fabric `END_EXTRACTION` / NeoForge `ExtractLevelRenderStateEvent`). Measurements uses `AFTER_TRANSLUCENT_TERRAIN` on Fabric, which runs *after* finalization, so it may rely on a one-frame carry-over. Do not copy that choice.

## 2. Floating text and HUD text

**World text** has three options:
- `Gizmos.billboardText(String, Vec3, TextGizmo.Style.forColorAndCentered(argb).withScale(s))`. It always faces the camera and works with `setAlwaysOnTop()`, but takes a plain `String` only. [MC]
- `submitNodeCollector.submitText(poseStack, x, y, component.getVisualOrderText(), shadow, Font.DisplayMode.SEE_THROUGH | NORMAL, 0xF000F0 /*full bright*/, color, bg, outline)`. You build the billboard pose yourself with `camera.rotation()`, a 180° Y rotation, a negative scale and centring by `font.width`. Measurements does exactly this in `MeasurementBox.drawText`. [MEAS]
- `submitNameTag(PoseStack, Vec3, int, Component, boolean, int, CameraRenderState)` gives name-tag styling. [MC]

All three come from vanilla, so the code is identical on both loaders. Only the hook that hands you the collector differs, as in §1.

**HUD text** is registration only; the drawing code is the same on both loaders:
- Fabric: `HudElementRegistry.addLast/addFirst/attachElementBefore/After(Identifier, HudElement)`, where `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`. Vanilla anchors are in `VanillaHudElements`. Register in `onInitializeClient`. [FAPI, FDOCS hud.md]
- NeoForge: `RegisterGuiLayersEvent` on the **mod bus**, with `registerAboveAll/registerBelow/registerAbove(...)(Identifier, GuiLayer)`, where `GuiLayer.render(GuiGraphicsExtractor, DeltaTracker)`. Anchors are in `VanillaGuiLayers`. [NEO]
- Both lambdas have the shape `(GuiGraphicsExtractor, DeltaTracker) -> void`, so one common callback can serve both. Text is drawn with `graphics.text(font, Component|String, x, y, color[, shadow])` [MC `GuiGraphicsExtractor`].

## 3. Keybinds

Vanilla 26.3 [MC]: `new KeyMapping(String translationKey, InputConstants.Type, int keyCode, KeyMapping.Category)` and `KeyMapping.Category.register(Identifier)`. The category's translation key is `key.category.<ns>.<path>`. Poll it with `consumeClick()` or `isDown()`. Key codes now follow **SDL3** scancodes/keycodes (`InputConstants.KEY_*`), not GLFW. [NEO `InputEvent.Key` docs]

- Fabric: `KeyMappingHelper.registerKeyMapping(KeyMapping)` in the client initializer. It also adds the category. Poll inside `ClientTickEvents.END_CLIENT_TICK`. [FAPI, FDOCS key-mappings.md]
- NeoForge: `RegisterKeyMappingsEvent` on the **mod bus**: `event.register(key)` and `event.registerCategory(category)`. Poll in `ClientTickEvent.Post` on the game bus. NeoForge adds `IKeyConflictContext`/`KeyModifier` overloads, which are NeoForge-only. [NEO]
- For common code, build the `KeyMapping` objects in common code and let each loader register the list.

## 4. Catching block clicks with no server support

The mod runs against a vanilla server, so cancelling has to happen **before** the client sends `ServerboundPlayerActionPacket` (attack) or `ServerboundUseItemOnPacket` (use). Otherwise the server still breaks or uses the block. In both cases, read the clicked block from `Minecraft.getInstance().hitResult` (a `BlockHitResult`), which is limited to normal reach. For Anchors beyond reach, ray-cast yourself with `player.pick(distance, partialTick, false)`.

**Fabric** [FAPI `MultiPlayerGameModeMixin`, Javadoc]:
- **Left click:** `ClientPreAttackCallback` (client-only) fires every tick while attack is held and passes `clickCount`. Returning `true` cancels **all** vanilla attack handling: no breaking, no swing, no packet. `clickCount != 0` means a fresh click. **This is the right hook.** `AttackBlockCallback` returning `FAIL` also skips the packet, but it is a *common* event that also fires server-side in singleplayer.
- **Right click:** `UseBlockCallback` returning `FAIL` cancels before `startPrediction`, so no packet is sent. It is a common event: check `level.isClientSide()`, because in singleplayer the integrated server also fires it, and note it fires "before the spectator check". Returning `SUCCESS` would send the use packet to the server.

**NeoForge** [NEO `Minecraft.java.patch`, `MultiPlayerGameMode.java.patch`]:
- **Use `InputEvent.InteractionKeyMappingTriggered`** (game bus, client-only). It fires in `startAttack`, `continueAttack`, `startUseItem` and `pickBlockOrEntity`, before any game-mode call. `setCanceled(true)` returns early with no packet, and `setSwingHand(false)` suppresses the swing. The swing still sends `ServerboundPunchPacket` from `continueAttack`, which is harmless. Use `isAttack()`, `isUseItem()` and `getHand()`. The event carries no target; read `mc.hitResult`.
- **Do not use `PlayerInteractEvent.LeftClickBlock/RightClickBlock` to block the server.** NeoForge fires them *inside* the `startPrediction` lambda, which still **returns and sends the packet** even when the event is cancelled (`RightClickBlock` in `performUseItemOn`; creative and survival `START_DESTROY_BLOCK` paths). Cancelling stops only the client-side prediction, so a vanilla server would still act. These events are also common (both sides).
- For common code, define `onAttackPressed(isFreshClick) -> boolean cancel` and `onUsePressed(hand) -> boolean cancel`. Fabric wires them to `ClientPreAttackCallback` + `UseBlockCallback` (FAIL, client side only). NeoForge wires both to `InteractionKeyMappingTriggered`. The Fabric use path fires only when a block is targeted. The NeoForge one fires for any right click, so filter on `hitResult.getType() == BLOCK`.

Measurements is no help here: it uses a mod **item** (`TapeItem.useOn`), which needs the item registered on the server.

## 5. Client-side storage per server or world

All of this is vanilla, so common code needs no loader abstraction [MC]:
- Base directory: `Minecraft.getInstance().gameDirectory` (a `File`), e.g. `<gameDir>/anglebuilder/<key>.json`. The loader equivalents are Fabric `FabricLoader.getInstance().getGameDir()/getConfigDir()` and NeoForge `FMLPaths.GAMEDIR/CONFIGDIR`, but the vanilla field is enough.
- **Multiplayer:** `mc.getCurrentServer()` returns a `ServerData` with `ip`, `name`, `type()` and `isRealm()`. Key on a sanitised `ip`, because names are user-editable.
- **Singleplayer:** `mc.hasSingleplayerServer()` / `mc.getSingleplayerServer()` returns an `IntegratedServer`. `getWorldPath(LevelResource.ROOT)` gives the save folder, whose name is unique per save. The data could also live **inside** the save folder, so it travels with the world. `getWorldData().getLevelName()` returns the display name, which is not unique.
- Add `level.dimension()` (a `ResourceKey<Level>`) to the key, since Anchors are per dimension; Measurements already stores the dimension per box.
- **Load and save lifecycle:**
  - Fabric: `ClientPlayConnectionEvents.JOIN` and `DISCONNECT` [FAPI]. Measurements uses mixins into `ClientPacketListener.handleLogin` and `Minecraft.disconnect`, but the Fabric API events cover this.
  - NeoForge: `ClientPlayerNetworkEvent.LoggingIn` and `LoggingOut`. `LoggingOut` "may also fire when a new integrated server is being created", so its fields are nullable. [NEO]

## Loader differences common code must hide

| Concern | Fabric | NeoForge |
|---|---|---|
| Init / registration | `ClientModInitializer.onInitializeClient` + static `Event.register` | `@Mod` constructor; **mod bus** for `RegisterKeyMappingsEvent`, `RegisterGuiLayersEvent`; **game bus** for everything else |
| World submit hook | `LevelRenderEvents.BEFORE_GIZMOS` (`ctx.submitNodeCollector()`, `ctx.poseStack()`, `ctx.levelState()`) | `SubmitCustomGeometryEvent` (`getSubmitNodeCollector()`, `getPoseStack()`, `getLevelRenderState()`) |
| Extraction hook | `LevelExtractionEvents.END_EXTRACTION` | `ExtractLevelRenderStateEvent` |
| HUD | `HudElementRegistry` + `HudElement.extractRenderState` | `RegisterGuiLayersEvent` + `GuiLayer.render` |
| Keybind registration | `KeyMappingHelper.registerKeyMapping` | `RegisterKeyMappingsEvent.register/registerCategory` |
| Client tick | `ClientTickEvents.END_CLIENT_TICK` | `ClientTickEvent.Post` |
| Attack intercept | `ClientPreAttackCallback` → `true` | `InteractionKeyMappingTriggered` (`isAttack`) → cancel + `setSwingHand(false)` |
| Use intercept | `UseBlockCallback` → `FAIL` (filter `isClientSide`) | `InteractionKeyMappingTriggered` (`isUseItem`) → cancel |
| Join / leave | `ClientPlayConnectionEvents.JOIN/DISCONNECT` | `ClientPlayerNetworkEvent.LoggingIn/LoggingOut` |
| Paths | vanilla `gameDirectory` (same on both) | same |

Gizmos, `SubmitNodeCollector`, `KeyMapping`, `GuiGraphicsExtractor`, `ServerData` and `IntegratedServer` are all vanilla, so the actual drawing and storage logic can live entirely in common code. Measurements uses the same split: common `ClientClass`/`MeasurementBox`, with thin `ClientMeasurementsFabric` and `ClientHandler` (NeoForge) adapters.

## Open questions / not verified

- Same-frame gizmo visibility from the `BEFORE_GIZMOS` / `SubmitCustomGeometryEvent` hooks needs a prototype check. No JDK was available here to disassemble `LevelRenderer`, so collector activation was inferred from member names and the NeoForge patch context.
- Whether Fabric `AFTER_TRANSLUCENT_TERRAIN` fires when improved transparency is on was inferred from mixin targets, not tested.
- Gizmo rendering cost with hundreds of cuboids (long Lines) is not measured. If it is too slow, a custom `submitCustomGeometry` with `debugFilledBox`/`linesTranslucentNoDepthWrite`, or a custom pipeline, is the fallback.
