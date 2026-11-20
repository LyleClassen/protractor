package com.lyleclassen.protractor.core;

import java.util.ArrayList;
import java.util.List;

/** The numbers describing a Line, read from Anchor 1 to Anchor 2. */
public record Measurement(
	int blockCount,
	double distance,
	int dx, int dy, int dz,
	int bearing,
	String cardinal,
	int elevation,
	String ratio,
	List<Run> runs,
	boolean labelledSteps
) {
	private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	public static Measurement of(Line line) {
		int dx = line.dx(), dy = line.dy(), dz = line.dz();
		double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
		int bearing = 0;
		String cardinal = "";
		if (dx != 0 || dz != 0) {
			// Minecraft: north is -Z, east is +X. Bearing is clockwise from north.
			double deg = Math.toDegrees(Math.atan2(dx, -dz));
			bearing = Math.floorMod((int) Math.round(deg), 360);
			cardinal = CARDINALS[Math.floorMod((int) Math.round(bearing / 45.0), 8)];
		}
		double horizontal = Math.sqrt((double) dx * dx + (double) dz * dz);
		int elevation = (int) Math.round(Math.toDegrees(Math.atan2(dy, horizontal)));
		List<Run> runs = runs(line);
		int movingMinorAxes = 0;
		int major = Axis.longest(dx, dy, dz);
		int[] d = {dx, dy, dz};
		for (int axis = 0; axis < 3; axis++) if (axis != major && d[axis] != 0) movingMinorAxes++;
		return new Measurement(line.blockCount(), distance, dx, dy, dz, bearing, cardinal, elevation,
			ratio(dx, dy, dz), runs, movingMinorAxes > 1);
	}

	/** Reduced |dx|:|dy|:|dz| with zero axes dropped, e.g. "8:3" or "4:1:2". */
	public static String ratio(int dx, int dy, int dz) {
		int[] parts = {Math.abs(dx), Math.abs(dy), Math.abs(dz)};
		int g = 0;
		for (int p : parts) g = gcd(g, p);
		if (g == 0) return "0";
		StringBuilder sb = new StringBuilder();
		for (int p : parts) {
			if (p == 0) continue;
			if (!sb.isEmpty()) sb.append(':');
			sb.append(p / g);
		}
		return sb.toString();
	}

	static int gcd(int a, int b) {
		while (b != 0) {
			int t = a % b;
			a = b;
			b = t;
		}
		return Math.abs(a);
	}

	/** Splits the Line into Runs along its longest axis, from Anchor 1 to Anchor 2. */
	public static List<Run> runs(Line line) {
		List<Pos> blocks = line.blocks();
		int major = Axis.longest(line.dx(), line.dy(), line.dz());
		List<Run> runs = new ArrayList<>();
		int length = 1;
		String step = "";
		for (int i = 1; i < blocks.size(); i++) {
			Pos prev = blocks.get(i - 1), cur = blocks.get(i);
			StringBuilder shift = new StringBuilder();
			for (int axis = 0; axis < 3; axis++) {
				if (axis == major) continue;
				int delta = cur.get(axis) - prev.get(axis);
				if (delta == 0) continue;
				shift.append(axisLabel(axis, delta));
			}
			if (shift.isEmpty()) {
				length++;
			} else {
				runs.add(new Run(length, step));
				step = shift.toString();
				length = 1;
			}
		}
		runs.add(new Run(length, step));
		return runs;
	}

	static String axisLabel(int axis, int delta) {
		return switch (axis) {
			case Axis.Y -> delta > 0 ? "↑" : "↓";
			case Axis.X -> "→x";
			default -> "→z";
		};
	}

	/** The step guide, e.g. "3 · 2 · 3" or "3 ↑ 2 →z 3". */
	public String guide() {
		StringBuilder sb = new StringBuilder();
		for (Run run : runs) {
			if (!sb.isEmpty()) sb.append(' ').append(labelledSteps ? run.stepBefore() : "·").append(' ');
			sb.append(run.length());
		}
		return sb.toString();
	}
}
