package com.lyleclassen.protractor.core;

/**
 * Moves Anchor 2 so the Line lies on a whole-block ratio. The horizontal pair dx:dz snaps first
 * to a:b with parts from 1 to {@code maxPart} (or along an axis), then dy snaps against the longest
 * horizontal delta the same way. Never snaps to degrees.
 */
public final class Snap {
	private Snap() {}

	public static Pos anchor2(Pos anchor1, Pos aimed, int maxPart) {
		int dx = aimed.x() - anchor1.x(), dy = aimed.y() - anchor1.y(), dz = aimed.z() - anchor1.z();
		if (dx == 0 && dz == 0) return aimed; // straight up or down is already on an axis

		// Horizontal: pick the a:b direction closest in angle, scaled to the nearest length.
		int[] h = nearestRatio(Math.abs(dx), Math.abs(dz), maxPart);
		int a = h[0], b = h[1];
		double scale = projection(Math.abs(dx), Math.abs(dz), a, b);
		int hMajor = Math.max(a, b);

		// Vertical: dy against the longest horizontal delta, with the same ratio set plus flat/vertical.
		double horizontalLongest = Math.max(1, scale * hMajor);
		int[] v = nearestVerticalRatio(Math.abs(dy), horizontalLongest, maxPart);
		int c = v[0], e = v[1]; // dy : longest horizontal = c : e
		if (e == 0) {
			// Vertical snap: keep the aimed height, drop the horizontal offset.
			return new Pos(anchor1.x(), aimed.y(), anchor1.z());
		}
		// Horizontal multiplier n must make n*hMajor divisible by e so dy is a whole number.
		int unit = e / Measurement.gcd(e, hMajor);
		int n = (int) Math.max(1, Math.round(scale / unit)) * unit;
		int hx = Integer.signum(dx) * n * a;
		int hz = Integer.signum(dz) * n * b;
		int vy = Integer.signum(dy) * (n * hMajor / e) * c;
		return anchor1.plus(hx, vy, hz);
	}

	/** Nearest a:b (both 0..maxPart, not both 0, reduced) to the direction (u, w) with u, w >= 0. */
	static int[] nearestRatio(int u, int w, int maxPart) {
		double target = Math.atan2(w, u);
		int[] best = {1, 0};
		double bestDiff = Double.MAX_VALUE;
		for (int a = 0; a <= maxPart; a++) {
			for (int b = 0; b <= maxPart; b++) {
				if ((a == 0 && b == 0) || Measurement.gcd(a, b) != 1) continue;
				double diff = Math.abs(Math.atan2(b, a) - target);
				if (diff < bestDiff - 1e-12) {
					bestDiff = diff;
					best = new int[] {a, b};
				}
			}
		}
		return best;
	}

	/** Nearest c:e for rise c over run e, both 0..maxPart, against rise/run = vertical/horizontal. */
	static int[] nearestVerticalRatio(int vertical, double horizontal, int maxPart) {
		double target = Math.atan2(vertical, horizontal);
		int[] best = {0, 1};
		double bestDiff = Double.MAX_VALUE;
		for (int c = 0; c <= maxPart; c++) {
			for (int e = 0; e <= maxPart; e++) {
				if ((c == 0 && e == 0) || Measurement.gcd(c, e) != 1) continue;
				double diff = Math.abs(Math.atan2(c, e) - target);
				if (diff < bestDiff - 1e-12) {
					bestDiff = diff;
					best = new int[] {c, e};
				}
			}
		}
		return best;
	}

	/** How many (a, b) units the vector (u, w) projects onto, at least 1. */
	static double projection(int u, int w, int a, int b) {
		double t = (u * (double) a + w * (double) b) / (a * (double) a + b * (double) b);
		return Math.max(1, t);
	}
}
