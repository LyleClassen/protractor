package com.lyleclassen.protractor.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SnapTest {
	private static final Pos O = new Pos(0, 0, 0);

	private static String ratioAfterSnap(int x, int y, int z, int max) {
		Pos p = Snap.anchor2(O, new Pos(x, y, z), max);
		return Measurement.ratio(p.x(), p.y(), p.z());
	}

	@Test
	void nearlyDiagonalSnapsTo45() {
		Pos p = Snap.anchor2(O, new Pos(10, 0, 9), 4);
		assertEquals(p.x(), p.z());
		assertEquals(0, p.y());
	}

	@Test
	void keepsDirectionSigns() {
		Pos p = Snap.anchor2(O, new Pos(-10, 0, 21), 4);
		assertTrue(p.x() < 0 && p.z() > 0);
		assertEquals("1:2", ratioAfterSnap(-10, 0, 21, 4));
	}

	@Test
	void nearlyAxisAlignedSnapsOntoTheAxis() {
		Pos p = Snap.anchor2(O, new Pos(30, 0, 1), 4);
		assertEquals(0, p.z());
		assertEquals(30, p.x());
	}

	@Test
	void slopeSnapsAgainstLongestHorizontalDelta() {
		Pos p = Snap.anchor2(O, new Pos(12, 5, 0), 4);
		// 5/12 ≈ 0.42 → nearest rise:run is 2:5? not allowed with max 4 → 1:2 (0.5) or 1:3 (0.33): 1:2 by angle
		assertEquals(0, p.z());
		assertEquals(p.x(), 2 * p.y());
	}

	@Test
	void verticalStaysVertical() {
		assertEquals(new Pos(0, 9, 0), Snap.anchor2(O, new Pos(0, 9, 0), 4));
		assertEquals(new Pos(0, 9, 0), Snap.anchor2(O, new Pos(1, 9, 0), 4));
	}

	@Test
	void maxPartOneAllowsOnly45AndAxes() {
		assertEquals("1:1", ratioAfterSnap(10, 0, 6, 1));
		assertEquals("1", ratioAfterSnap(10, 0, 2, 1));
	}

	@Test
	void resultIsAlwaysWholeRatioInMaxPart() {
		for (int x = -15; x <= 15; x += 3)
			for (int y = -9; y <= 9; y += 3)
				for (int z = -15; z <= 15; z += 4) {
					Pos p = Snap.anchor2(O, new Pos(x, y, z), 4);
					for (String part : Measurement.ratio(p.x(), p.y(), p.z()).split(":")) {
						// Horizontal pair and vertical against longest horizontal both use parts ≤ 4,
						// so the combined parts stay small.
						assertTrue(Integer.parseInt(part) <= 16, () -> "" + p);
					}
				}
	}
}
