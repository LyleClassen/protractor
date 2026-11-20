# Angle Builder

A client-only NeoForge mod for Minecraft 1.21.1 (All The Mods 10) that helps you build at angles.

Mods like [Measurements](https://github.com/Mrbysco/Measurements) show the
distance between two points along the N/S/E/W axes. Angle Builder goes
further: pick any two points and it shows you exactly which blocks make up
the line between them, at any angle in any direction, so you can build
diagonals, slopes and angled walls without guesswork.

## Features

- Aim at a block and press **Set Anchor** (`V`) twice to mark the two Anchors of a Line. A white preview follows the crosshair in between. **Cancel Anchor** (`B`) drops a pending first Anchor.
- The Line's blocks (a thin 3D Bresenham line) are drawn as outlines, and positions that are already built are hidden.
- The Measurement above the hotbar shows block count, blocks left, distance, deltas, compass bearing, elevation, the X:Y:Z ratio, and a ruler of Runs.
- Hold **Snap** (`Left Alt`) to move the second Anchor onto whole-block ratios (1:1, 2:1, 3:2, …).
- Lines are saved per world or server and per dimension. Look at a Line to make it the Target, then press **Delete** (`N`) to remove it. **Clear** and **Hide** are unbound by default.
- Settings live in `config/anglebuilder.json`.

## Development

Requires JDK 21.

```sh
./gradlew build        # test and build the mod jar into build/libs/
./gradlew runClient    # launch a dev Minecraft client with the mod loaded
```

## License

MIT
