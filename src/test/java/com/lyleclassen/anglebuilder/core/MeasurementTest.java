package com.lyleclassen.anglebuilder.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MeasurementTest {
	private static Measurement m(int x1, int y1, int z1, int x2, int y2, int z2) {
		return Measurement.of(new Line(new Pos(x1, y1, z1), new Pos(x2, y2, z2)));
	}

	@Test
	void countDistanceAndDeltas() {
		var m = m(0, 0, 0, 24, 0, -10);
		assertEquals(25, m.blockCount());
		assertEquals(26.0, m.distance(), 1e-9);
		assertEquals(24, m.dx());
		assertEquals(-10, m.dz());
	}

	@Test
	void bearingIsCompassClockwiseFromNorth() {
		assertEquals(0, m(0, 0, 0, 0, 0, -5).bearing());
		assertEquals("N", m(0, 0, 0, 0, 0, -5).cardinal());
		assertEquals(90, m(0, 0, 0, 5, 0, 0).bearing());
		assertEquals(180, m(0, 0, 0, 0, 0, 5).bearing());
		assertEquals(270, m(0, 0, 0, -5, 0, 0).bearing());
		assertEquals(67, m(0, 0, 0, 24, 0, -10).bearing());
		assertEquals("NE", m(0, 0, 0, 24, 0, -10).cardinal());
	}

	@Test
	void elevationIsPositiveWhenAnchor2IsHigher() {
		assertEquals(45, m(0, 0, 0, 3, 3, 0).elevation());
		assertEquals(-45, m(0, 3, 0, 3, 0, 0).elevation());
		assertEquals(90, m(0, 0, 0, 0, 7, 0).elevation());
	}

	@Test
	void ratioIsReducedWithZeroAxesDropped() {
		assertEquals("12:5", Measurement.ratio(24, 0, -10));
		assertEquals("4:1:2", Measurement.ratio(8, 2, 4));
		assertEquals("17:13", Measurement.ratio(17, 13, 0));
		assertEquals("1", Measurement.ratio(0, 9, 0));
	}

	@Test
	void runsCoverEveryBlockWithOneStepBetweenRuns() {
		var m = m(0, 0, 0, 8, 3, 0);
		assertEquals(4, m.runs().size());
		assertEquals(9, m.runs().stream().mapToInt(Run::length).sum());
		assertFalse(m.labelledSteps());
		assertTrue(m.guide().contains("·"));
	}

	@Test
	void threeDimensionalStepsNameTheirAxis() {
		var m = m(0, 0, 0, 10, 3, 4);
		assertTrue(m.labelledSteps());
		assertTrue(m.guide().contains("↑"));
		assertTrue(m.guide().contains("→z"));
	}

	@Test
	void runsReadFromAnchor1() {
		var forward = m(0, 0, 0, 8, 3, 0).runs();
		var backward = m(8, 3, 0, 0, 0, 0).runs();
		assertEquals(forward.stream().map(Run::length).toList(),
			backward.stream().map(Run::length).toList().reversed());
	}
}
