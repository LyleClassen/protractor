# Contributing to Protractor

Thanks for helping out. Bug reports, ideas and pull requests are all welcome.

## Reporting bugs and asking for features

Open an [issue](https://github.com/LyleClassen/protractor/issues/new/choose) using the template that fits. For bugs, include your Minecraft, NeoForge and Protractor versions, any modpack you're using, and a log if something crashed.

## Development setup

You need JDK 21.

```sh
./gradlew build        # compile, run the unit tests, and build the jar into build/libs/
./gradlew runClient    # launch a dev client with the mod loaded
```

The code is in two packages:

- `core` holds the pure logic: Lines, Snap and Measurements. It doesn't depend on Minecraft and is unit-tested under `src/test`.
- `client` holds input handling, rendering, the HUD and storage.

## Words and decisions

- [GLOSSARY.md](GLOSSARY.md) defines the project's terms (Anchor, Line, Target, Run, …). Please use them in code, docs and issues, and avoid the listed alternatives.
- [docs/adr/](docs/adr/) records the main design decisions. If your change goes against one, say so in the PR, or add a new ADR.

## Pull requests

- Keep each PR focused on one change.
- Make sure `./gradlew build` passes.
- Add or update tests in `core` when you change its logic.
- Add an entry under a new `## [Unreleased]` heading in [CHANGELOG.md](CHANGELOG.md) if players will notice the change.

By contributing, you agree that your work is licensed under the [MIT License](LICENSE).
