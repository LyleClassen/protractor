package com.lyleclassen.protractor.core;

public final class Axis {
	public static final int X = 0, Y = 1, Z = 2;

	private Axis() {}

	/** The longest axis of a delta; ties go to X, then Y, then Z. */
	public static int longest(int dx, int dy, int dz) {
		int ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
		if (ax >= ay && ax >= az) return X;
		if (ay >= az) return Y;
		return Z;
	}
}
