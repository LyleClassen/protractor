package com.lyleclassen.anglebuilder.core;

/** A block position. Pure: no Minecraft types. */
public record Pos(int x, int y, int z) implements Comparable<Pos> {
	@Override
	public int compareTo(Pos o) {
		if (x != o.x) return Integer.compare(x, o.x);
		if (y != o.y) return Integer.compare(y, o.y);
		return Integer.compare(z, o.z);
	}

	public int get(int axis) {
		return switch (axis) {
			case 0 -> x;
			case 1 -> y;
			default -> z;
		};
	}

	public Pos plus(int dx, int dy, int dz) {
		return new Pos(x + dx, y + dy, z + dz);
	}
}
