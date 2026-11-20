package com.lyleclassen.anglebuilder.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class LineTest {
	@Test
	void blocksRunFromAnchor1ToAnchor2() {
		List<Pos> blocks = new Line(new Pos(0, 0, 0), new Pos(8, 3, 0)).blocks();
		assertEquals(9, blocks.size());
		assertEquals(new Pos(0, 0, 0), blocks.getFirst());
		assertEquals(new Pos(8, 3, 0), blocks.getLast());
	}

	@Test
	void blockSetDoesNotDependOnAnchorOrder() {
		Pos a = new Pos(3, -7, 12), b = new Pos(-20, 4, 1);
		var forward = new Line(a, b).blocks();
		var backward = new Line(b, a).blocks();
		assertEquals(new HashSet<>(forward), new HashSet<>(backward));
		assertEquals(forward, backward.reversed());
	}

	@Test
	void lineIsThinOneBlockPerStepAlongLongestAxis() {
		var line = new Line(new Pos(0, 0, 0), new Pos(5, 17, -9));
		var blocks = line.blocks();
		assertEquals(18, blocks.size());
		assertEquals(18, line.blockCount());
		for (int i = 1; i < blocks.size(); i++) {
			Pos p = blocks.get(i - 1), q = blocks.get(i);
			assertEquals(1, Math.abs(q.y() - p.y()));
			assertTrue(Math.abs(q.x() - p.x()) <= 1);
			assertTrue(Math.abs(q.z() - p.z()) <= 1);
		}
	}

	@Test
	void singleBlockLine() {
		var line = new Line(new Pos(1, 2, 3), new Pos(1, 2, 3));
		assertEquals(List.of(new Pos(1, 2, 3)), line.blocks());
	}

	@Test
	void colourIndexIsStableAndOrderIndependent() {
		var a = new Line(new Pos(1, 2, 3), new Pos(10, 20, 30));
		var b = new Line(new Pos(10, 20, 30), new Pos(1, 2, 3));
		assertEquals(a.colorIndex(8), b.colorIndex(8));
		int i = a.colorIndex(8);
		assertTrue(i >= 0 && i < 8);
	}
}
