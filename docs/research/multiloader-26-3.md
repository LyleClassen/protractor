# MultiLoader-Template on Minecraft 26.3, and config on Fabric + NeoForge

Research for issue #4. Gathered 2026-11-20. This covers the facts and the options. It doesn't make the decision, which belongs to #9.

## TL;DR

- **Layout.** jaredlll08/MultiLoader-Template has a `26.3` branch: `common/`, `fabric/`, `neoforge/`, plus an included build `build-logic/` holding two convention plugins. It uses **Fabric Loom `1.18.1`** (`net.fabricmc.fabric-loom`, which doesn't remap) and **ModDevGradle `2.0.147`** (`net.neoforged.moddev`) on Gradle **9.7.1** and Java **25**.
- **How common code reaches each loader.** `common` compiles against vanilla through MDG's `neoFormVersion` (`26.3-1`), so it can't see loader APIs. Each loader project then **compiles the common *sources* into its own jar**. No separate common jar ships. Loader-specific behaviour goes behind a `ServiceLoader` interface (`IPlatformHelper`).
- **Config.**
  - NeoForge's `ModConfigSpec` is built in, so it costs nothing on NeoForge. With Forge Config API Port (FCAP) on Fabric, the same spec works from `common`. This is the route Measurements takes.
  - Cloth Config and YACL both have 26.3 builds for both loaders, at about 1.1 MB per loader.
  - owo-lib has **no 26.3 build**.
  - A hand-rolled Gson file costs no dependencies but comes with no screen.

## 1. Project layout and Gradle plugins (26.3)

Source: the `26.3` branch of [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template/tree/26.3). Branches `26.1`, `26.1.1`, `26.1.2`, `26.2` and `26.3` exist.

| Item | Template `26.3` | Measurements `multi/26.3` | Latest on maven (2026-11-20) |
|---|---|---|---|
| Gradle wrapper | 9.7.1 | — | — |
| Fabric Loom plugin | `net.fabricmc.fabric-loom` **1.18.1** | `1.17-SNAPSHOT` | 1.19.4 ([maven](https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/maven-metadata.xml)); this repo pins `1.19-SNAPSHOT` |
| ModDevGradle | `net.neoforged.moddev` **2.0.147** | 2.0.147 | 2.0.148 ([maven](https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml)) |
| NeoForm (used by common) | `26.3-1` | `26.3-1` | — |
| NeoForge | `26.3.0.1-beta` | `26.3.0.37-beta` | 26.3.0.114 ([maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)) |
| Fabric Loader | 0.19.5 | 0.19.5 | 0.20.1 ([meta](https://meta.fabricmc.net/v2/versions/loader/26.3)) |
| Fabric API | `0.160.6+26.3` | `0.161.0+26.3` | this repo: `0.166.2+26.3` |
| NeoForge loader range | `[4,)` | `[4,)` | — |

**Loom: no remapping on 26.x.** Minecraft became unobfuscated in 26.1. From then on Fabric uses the `net.fabricmc.fabric-loom` plugin ID, which doesn't remap. Code uses `implementation`/`compileOnly`/`api` in place of `modImplementation` and friends, and `jar` in place of `remapJar`. No mappings are needed, and mixins targeting unobfuscated code don't need remapping. Sources: [Fabric 26.1 blog post](https://fabricmc.net/2026/03/14/261.html), [Fabric porting guide](https://docs.fabricmc.net/develop/porting/), [Loom docs](https://docs.fabricmc.net/develop/loom/). The current repo already uses this plugin ID.

**ModDevGradle vs NeoGradle.** Both the template and Measurements use **ModDevGradle** in two places: `common` in NeoForm-only "vanilla mode" (`neoForge { neoFormVersion = ... }`) and `neoforge` (`neoForge { version = ... }`). NeoGradle (`net.neoforged.gradle.userdev`) is still published (7.1.39, updated 2026-09-15, [maven](https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/maven-metadata.xml)), but no 26.x MultiLoader-Template branch uses it. Measurements' root `build.gradle` still has a stale comment, "Required for NeoGradle" (idea-ext), even though it uses MDG.

### Files in the template's `26.3` branch

- `settings.gradle`
  - `pluginManagement` includes the Fabric maven, restricted to `net.fabricmc` through `exclusiveContent`.
  - It applies the foojay toolchain resolver 1.0.0, then runs `includeBuild('build-logic')` and `include('common','fabric','neoforge')`.
- Root `build.gradle` only declares the two plugins, both with `apply false`.
- `build-logic/src/main/groovy/multiloader-common.gradle` is applied to all three projects. It sets up:
  - `java-library` and the Java toolchain from `java_version`
  - sources and javadoc jars, and `LICENSE` in the jar
  - `processResources`, which expands `gradle.properties` values into `fabric.mod.json`, `neoforge.mods.toml`, `pack.mcmeta` and `*.mixins.json`
  - maven-publish
- `build-logic/.../multiloader-loader.gradle` is applied to `fabric` and `neoforge`. It:
  - adds `compileOnly(project(':common'))` with the `io.github.mcgradleconventions.loader=common` attribute
  - resolves the `commonJava` and `commonResources` configurations that `common` exposes
  - adds them to `compileJava.source(...)`, `processResources.from(...)`, `javadoc` and `sourcesJar`
- `common/build.gradle`
  - It applies `net.neoforged.moddev` with `neoFormVersion`. An access transformer is picked up from `common/src/main/resources/META-INF/accesstransformer.cfg` if that file exists.
  - It adds `compileOnly` Sponge Mixin `0.17.3+mixin.0.8.7` and MixinExtras `0.5.3`. Both loaders bundle these.
  - It exposes `commonJava` and `commonResources` as consumable artifacts.
- `fabric/build.gradle`
  - It applies Loom and depends on `minecraft`, `fabric-loader` and `fabric-api`.
  - An access widener is read from `common/src/main/resources/<modid>.accesswidener` if it exists.
  - It defines client and server runs in `runs/client` and `runs/server`.
- `neoforge/build.gradle` applies MDG with `version = neoforge_version` and defines `client`, `server` and `data` (`clientData()`) runs. Generated resources go in `src/generated/resources`.

### How common code is shared

- `common` compiles **only against vanilla Minecraft** (NeoForm). NeoForm is the joined client and server, so client classes such as `Minecraft` and `GameRenderer` are visible there. Common code can't reference Fabric or NeoForge API.
- Each loader project re-compiles the common `.java` files together with its own sources and copies in the common resources. Both loaders then build a single self-contained jar.
- Measurements uses an older equivalent of the same idea:
  - `fabric`: `implementation project(":common")` plus `tasks.withType(JavaCompile) { source(project(":common").sourceSets.main.allSource) }`
  - `neoforge`: `compileOnly project(":common")` plus `compileJava.source(...)`
  - both: `processResources.from(project(":common").sourceSets.main.resources)`
- Calls that differ by loader go through `common/.../platform/services/IPlatformHelper` and `Services.load()`, which uses `java.util.ServiceLoader`. Each loader registers its implementation in `META-INF/services/<fqcn>`.
- Mixins:
  - A common mixin config (`<modid>.mixins.json`) lives in `common`.
  - Each loader has its own (`<modid>.fabric.mixins.json`, `<modid>.neoforge.mixins.json`).
  - Both `fabric.mod.json` (`"mixins"`) and `neoforge.mods.toml` (`[[mixins]]`) list them.

## 2. Things to change for a client-only mod

The template defaults to running on both sides. It sets `"environment": "*"`, has a `main` entrypoint, and registers dependencies with `side="BOTH"`. Measurements is also a both-sides mod, since it adds an item.

**Fabric**
- `fabric.mod.json` → set `"environment": "client"`. The accepted values are `client`, `server` and `*` ([Fabric docs: project structure](https://docs.fabricmc.net/develop/getting-started/project-structure)).
- Use the `client` entrypoint, which implements `ClientModInitializer`, and drop `main`.
- The template doesn't use Loom's `splitEnvironmentSourceSets()`, but the current repo does (`src/client`). Under MultiLoader the client/server split is enforced by convention in `common`, not by source sets, because NeoForm exposes the joined jar.

**NeoForge**
- Use `@Mod(value = MOD_ID, dist = Dist.CLIENT)` on the entry class so it loads only on the physical client ([NeoForge docs: mod files](https://docs.neoforged.net/docs/gettingstarted/modfiles), [sides](https://docs.neoforged.net/docs/concepts/sides)). Measurements does the same with a runtime check instead: `if (dist.isClient()) { registerConfig(CLIENT, ...); registerExtensionPoint(IConfigScreenFactory...) }`.
- `displayTest` in `mods.toml` appears in the NeoForge docs up to **1.20.6** (`MATCH_VERSION`/`IGNORE_SERVER_VERSION`/`IGNORE_ALL_VERSION`/`NONE`). It is **gone from the docs from 1.21.1 onward**, including the current 26.1 docs, so there's no direct equivalent to set. *Unverified:* I couldn't find an authoritative statement on how a client-only mod with no network payloads behaves against vanilla or other servers on 26.3. Test this in-game.
- A client-only mod can set `side="CLIENT"` on its own `[[dependencies]]` entries where that's relevant.

## 3. Config options for one client config on both loaders

Availability was checked with the Modrinth API (`/v2/project/<slug>/version?game_versions=["26.3"]`) and the libraries' own maven on 2026-11-20. Sizes are the primary jar.

| Option | 26.3 Fabric | 26.3 NeoForge | Runtime cost to users | Config screen | Code in `common`? |
|---|---|---|---|---|---|
| **NeoForge `ModConfigSpec` + Forge Config API Port** | FCAP `26.3.1` (595 KB; requires Fabric API; optional Mod Menu) | **built in**, no extra mod | Fabric: +1 mod. NeoForge: 0 | NeoForge's built-in `ConfigurationScreen`. FCAP also provides it on Fabric, wired up through Mod Menu | Yes: common compiles against `forgeconfigapiport-common-neoforgeapi` |
| **Cloth Config** | `26.3.159+fabric` (1.1 MB) | `26.3.159+neoforge` (1.1 MB) | +1 mod on each loader | Yes: screen builder, plus AutoConfig (annotated POJO, GSON/JSON5/TOML) | Probably, by compiling against one variant (not tested) |
| **YACL** (YetAnotherConfigLib v3) | `3.9.7+26.3-fabric` (1.1 MB; requires Fabric API) | `3.9.7+26.3-neoforge` (1.1 MB) | +1 mod on each loader | Yes, a polished screen. `ConfigClassHandler` + Gson serializer | Probably, same caveat |
| **owo-lib / owo-config** | **No 26.3 build.** Latest: `0.13.1+26.2` (Fabric) | Only for 1.21.1 | — | — | Not viable at the moment |
| **Hand-rolled JSON (Gson)** | Gson ships with Minecraft | same | **0** | You build one (vanilla `Screen`) or ship none (edit the file) | Yes, fully |

### Forge Config API Port (the Measurements pattern)

- Repo and licence: [Fuzss/forgeconfigapiport](https://github.com/Fuzss/forgeconfigapiport) (branch `26.3.x`), MPL-2.0. [Modrinth page](https://modrinth.com/mod/forge-config-api-port).
- Maven: `https://raw.githubusercontent.com/Fuzss/modresources/main/maven/`. Artifacts:
  - `forgeconfigapiport-common-neoforgeapi`
  - `-common-forgeapi`
  - `-common`
  - `-fabric`
  - `-forge`
  - `-neoforge`

  Version `26.3.1` exists for each.
- Measurements wires it up like this:
  - `common`: `api("fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi:26.3.1")`. Common code then builds `net.neoforged.neoforge.common.ModConfigSpec` with `Builder().configure(...)`, `defineEnum`, `defineInRange` and so on.
  - `fabric`: `api("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.3.1")`. The client initializer calls `fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, spec)`. `fabric.mod.json` declares `"forgeconfigapiport": ">=26.3"`.
  - `neoforge`: no extra dependency. Registers with `container.registerConfig(ModConfig.Type.CLIENT, spec)` and `container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new)`.
  - Optional Mod Menu integration on Fabric: `com.terraformersmc:modmenu:21.0.0` from `maven.terraformersmc.com`, with `exclude group: "eu.pb4"`. A `modmenu` entrypoint returns `new ConfigurationScreen(MOD_ID, parent)`.
- The FCAP Fabric POM pulls in NightConfig `core` and `toml` 3.8.4, and it's built against Fabric API `0.161.0+26.3` and Loader `0.19.5`.
- The `CLIENT` config type is "Loaded ONLY on the physical client" and is saved as `config/<modid>-client.toml` ([NeoForge docs: configuration](https://docs.neoforged.net/docs/misc/config)). It auto-reloads when the file changes (`ModConfigEvent.Reloading`).
- **Trade-off:** NeoForge users get native config with no extra dependency, and the TOML file has comments. Fabric users must install FCAP, which also requires Fabric API. The config API is NeoForge's, so changes to it in NeoForge flow through FCAP.

### Cloth Config / YACL

- Cloth Config: [repo](https://github.com/shedaniel/ClothConfig/), LGPL-3.0-only, maven `https://maven.shedaniel.me/`, `me.shedaniel.cloth:cloth-config-{fabric,neoforge}` (26.3.158 on maven, 26.3.159 on Modrinth).
- YACL: [repo](https://github.com/isXander/YetAnotherConfigLib), LGPL-3.0-or-later, maven `https://maven.isxander.dev/releases/`, `dev.isxander:yet-another-config-lib:3.9.7+26.3-{fabric,neoforge}`.
- **Trade-off:** you get a rich GUI screen for free, but every user on **both** loaders has to install an extra ~1.1 MB mod. Neither library has a published common/API-only artifact on maven. On unobfuscated 26.x, compiling `common` against the Fabric jar *should* work because the package names are the same. Nothing in this research confirmed that.

### Hand-rolled Gson

- Gson is on the Minecraft classpath on both loaders. A record or POJO serialised to `config/anglebuilder.json` from `common` needs no dependencies. The config directory comes from `IPlatformHelper` (Fabric `FabricLoader.getInstance().getConfigDir()`, NeoForge `FMLPaths.CONFIGDIR`).
- **Trade-off:** nobody needs to install anything else. You do have to write your own validation, defaults and migration, and file-watch reload if you want it. You get no screen unless you build one: a vanilla `Screen` registered through NeoForge's `IConfigScreenFactory` and, optionally, Mod Menu's `compileOnly` API on Fabric.

### Mod Menu (Fabric)

The config button in Fabric's mod list comes from Mod Menu. It is optional for users and has nothing to do with the config library you choose. Mod Menu `21.0.1` supports 26.3 and requires Fabric API and placeholder-api. NeoForge's mod list has a config button built in through `IConfigScreenFactory`.

## Sources

- jaredlll08/MultiLoader-Template, branch `26.3`: `settings.gradle`, `build.gradle`, `gradle.properties`, `build-logic/**`, `{common,fabric,neoforge}/build.gradle`, `fabric.mod.json`, `neoforge.mods.toml`, `gradle-wrapper.properties`
- Mrbysco/Measurements, branch `multi/26.3`:
  - `gradle.properties` and `*/build.gradle`
  - `common/.../config/MeasurementConfig.java`
  - `fabric/.../ClientMeasurementsFabric.java` and `MeasurementsModMenuIntegration.java`
  - `neoforge/.../MeasurementsNeoForge.java`
- NeoForge docs: [mod files](https://docs.neoforged.net/docs/gettingstarted/modfiles), [sides](https://docs.neoforged.net/docs/concepts/sides), [configuration](https://docs.neoforged.net/docs/misc/config). `displayTest` was checked in neoforged/Documentation `versioned_docs/version-1.20.4` and `version-1.20.6`.
- Fabric: [26.1 blog](https://fabricmc.net/2026/03/14/261.html), [porting](https://docs.fabricmc.net/develop/porting/), [Loom](https://docs.fabricmc.net/develop/loom/), [project structure](https://docs.fabricmc.net/develop/getting-started/project-structure)
- Maven metadata: Fabric (Loom, loader), NeoForged (MDG, NeoGradle, NeoForge), Fuzss modresources (FCAP), shedaniel (Cloth), isxander (YACL)
- Modrinth API v2: `forge-config-api-port`, `cloth-config`, `yacl`, `owo-lib`, `modmenu`
