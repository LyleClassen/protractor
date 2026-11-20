package com.lyleclassen.protractor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Line between two Anchors. Its blocks are a thin 3D Bresenham line, computed from the Anchors
 * sorted by X, Y, Z so the block set doesn't depend on which Anchor was placed first. Everything
 * directional (deltas, bearing, Run order) still reads from Anchor 1 to Anchor 2.
 */
public record Line(Pos anchor1, Pos anchor2) {

	public Pos low() {
		return anchor1.compareTo(anchor2) <= 0 ? anchor1 : anchor2;
	}

	public Pos high() {
		return anchor1.compareTo(anchor2) <= 0 ? anchor2 : anchor1;
	}

	public int dx() { return anchor2.x() - anchor1.x(); }
	public int dy() { return anchor2.y() - anchor1.y(); }
	public int dz() { return anchor2.z() - anchor1.z(); }

	/** The Line's blocks, ordered from Anchor 1 to Anchor 2. */
	public List<Pos> blocks() {
		List<Pos> sorted = bresenham(low(), high());
		if (!low().equals(anchor1)) Collections.reverse(sorted);
		return sorted;
	}

	public int blockCount() {
		return Math.max(Math.abs(dx()), Math.max(Math.abs(dy()), Math.abs(dz()))) + 1;
	}

	static List<Pos> bresenham(Pos from, Pos to) {
		int[] p = {from.x(), from.y(), from.z()};
		int[] d = {to.x() - from.x(), to.y() - from.y(), to.z() - from.z()};
		int[] s = {Integer.signum(d[0]), Integer.signum(d[1]), Integer.signum(d[2])};
		int[] a = {Math.abs(d[0]), Math.abs(d[1]), Math.abs(d[2])};
		int major = Axis.longest(d[0], d[1], d[2]);
		int m1 = (major + 1) % 3, m2 = (major + 2) % 3;
		int n = a[major];
		List<Pos> out = new ArrayList<>(n + 1);
		out.add(new Pos(p[0], p[1], p[2]));
		int e1 = 2 * a[m1] - n, e2 = 2 * a[m2] - n;
		for (int i = 0; i < n; i++) {
			if (e1 > 0) { p[m1] += s[m1]; e1 -= 2 * n; }
			if (e2 > 0) { p[m2] += s[m2]; e2 -= 2 * n; }
			e1 += 2 * a[m1];
			e2 += 2 * a[m2];
			p[major] += s[major];
			out.add(new Pos(p[0], p[1], p[2]));
		}
		return out;
	}

	/** Colour index into a palette: a stable hash over the sorted Anchors' six ints. */
	public int colorIndex(int paletteSize) {
		Pos l = low(), h = high();
		int hash = 17;
		for (int v : new int[] {l.x(), l.y(), l.z(), h.x(), h.y(), h.z()}) {
			hash = hash * 31 + v;
			hash ^= hash >>> 15;
		}
		return Math.floorMod(hash, paletteSize);
	}
}
