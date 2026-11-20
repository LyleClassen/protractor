package com.lyleclassen.protractor.client;

import com.lyleclassen.protractor.core.Line;
import com.lyleclassen.protractor.core.Measurement;
import com.lyleclassen.protractor.core.Pos;
import java.util.List;
import net.minecraft.core.BlockPos;

/** A Line plus its cached blocks and Measurement. */
public final class SavedLine {
	public final Line line;
	public final List<BlockPos> blocks;
	public final Measurement measurement;

	public SavedLine(Line line) {
		this.line = line;
		this.blocks = line.blocks().stream().map(SavedLine::toBlockPos).toList();
		this.measurement = Measurement.of(line);
	}

	public int color() {
		return Settings.linePalette[line.colorIndex(Settings.linePalette.length)];
	}

	public static BlockPos toBlockPos(Pos p) {
		return new BlockPos(p.x(), p.y(), p.z());
	}

	public static Pos toPos(BlockPos p) {
		return new Pos(p.getX(), p.getY(), p.getZ());
	}
}
