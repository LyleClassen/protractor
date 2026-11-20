# Angle Builder

A Fabric mod for Minecraft 26.3 that helps you build at angles.

Mods like [Measurements](https://github.com/Mrbysco/Measurements) show the
distance between two points along the N/S/E/W axes. Angle Builder goes
further: pick any two points and it shows you exactly which blocks make up
the line between them, at any angle in any direction, so you can build
diagonals, slopes and angled walls without guesswork.

## Planned features

- Mark a start and end point with a tool item.
- Preview the voxelised line (3D Bresenham) between them as ghost blocks.
- Show length, angle (horizontal and vertical) and block count.
- A per-step guide ("2 forward, 1 right, 1 up…") for placing the line.
- Snap to common angles (e.g. 15°, 22.5°, 30°, 45°).

## Development

Requires JDK 25.

```sh
./gradlew build        # build the mod jar into build/libs/
./gradlew runClient    # launch a dev Minecraft client with the mod loaded
```

## License

MIT
