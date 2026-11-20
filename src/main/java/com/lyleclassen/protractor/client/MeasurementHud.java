package com.lyleclassen.protractor.client;

import com.lyleclassen.protractor.core.Measurement;
import com.lyleclassen.protractor.core.Run;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The Measurement strip above the hotbar: one line of numbers with a colour swatch, and above it
 * a ruler of Run boxes. Shown for the preview while placing, otherwise for the Target.
 */
final class MeasurementHud {
	private static final int BOX_HEIGHT = 11;
	private static final int MAX_PX_PER_BLOCK = 8;

	private MeasurementHud() {}

	static void render(GuiGraphics gfx, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.level == null) return;
		SavedLine shown;
		int color;
		if (ProtractorClient.preview != null) {
			shown = ProtractorClient.preview;
			color = Settings.previewColor;
		} else if (ProtractorClient.target != null && !ProtractorClient.hidden) {
			shown = ProtractorClient.target;
			color = shown.color();
		} else {
			return;
		}

		Font font = mc.font;
		Measurement m = shown.measurement;
		String text = text(m, left(mc.level, shown));
		int screenW = gfx.guiWidth(), screenH = gfx.guiHeight();

		// Text line sits above the vanilla action-bar message (which is drawn at height - 68).
		int textY = screenH - 82;
		int swatch = 7;
		int textW = swatch + 4 + font.width(text);
		int x = (screenW - textW) / 2;
		gfx.fill(x - 3, textY - 2, x + textW + 3, textY + 10, 0x90000000);
		gfx.fill(x, textY, x + swatch, textY + swatch + 1, 0xFF000000 | color);
		gfx.drawString(font, text, x + swatch + 4, textY, 0xFFFFFFFF, true);

		ruler(gfx, font, m, screenW, textY - 4);
	}

	private static int left(Level level, SavedLine line) {
		int left = 0;
		for (BlockPos p : line.blocks) if (!ProtractorClient.isFilled(level, p)) left++;
		return left;
	}

	static String text(Measurement m, int left) {
		StringBuilder sb = new StringBuilder();
		sb.append(m.blockCount()).append(m.blockCount() == 1 ? " block" : " blocks");
		if (left == 0) sb.append(" ✓");
		else if (left < m.blockCount()) sb.append(" (").append(left).append(" left)");
		sb.append(" · ").append(String.format("%.1f", m.distance()));
		sb.append(" · Δ ").append(signed(m.dx())).append(' ').append(signed(m.dy())).append(' ').append(signed(m.dz()));
		if (!m.cardinal().isEmpty()) sb.append(" · ").append(m.bearing()).append("° ").append(m.cardinal());
		sb.append(" · ").append(m.elevation()).append('°');
		sb.append(" · ").append(m.ratio());
		return sb.toString();
	}

	private static String signed(int v) {
		return v > 0 ? "+" + v : v < 0 ? "−" + (-v) : "0";
	}

	/** Run boxes as wide as their Runs, scaled to fit; Step labels above the gaps on 3D Lines. */
	private static void ruler(GuiGraphics gfx, Font font, Measurement m, int screenW, int bottom) {
		int runs = m.runs().size();
		int maxW = (int) (screenW * 0.9);
		int gaps = runs - 1;
		double px = Math.min(MAX_PX_PER_BLOCK, (maxW - gaps) / (double) m.blockCount());
		if (px < 1) {
			// Too many blocks to draw a box per block: show the guide as text instead.
			String guide = font.plainSubstrByWidth(m.guide(), maxW);
			gfx.drawString(font, guide, (screenW - font.width(guide)) / 2, bottom - 9, 0xFFFFFFFF, true);
			return;
		}
		int total = 0;
		int[] widths = new int[runs];
		for (int i = 0; i < runs; i++) {
			widths[i] = Math.max(1, (int) Math.round(m.runs().get(i).length() * px));
			total += widths[i];
		}
		total += gaps;
		int x = (screenW - total) / 2;
		int top = bottom - BOX_HEIGHT;
		int lastLabelEnd = Integer.MIN_VALUE;
		for (int i = 0; i < runs; i++) {
			Run run = m.runs().get(i);
			int w = widths[i];
			if (i > 0 && m.labelledSteps()) {
				String step = run.stepBefore();
				int sw = font.width(step);
				int sx = x - 1 - sw / 2;
				if (sx > lastLabelEnd + 2) {
					gfx.drawString(font, step, sx, top - 10, 0xFFFFFF55, true);
					lastLabelEnd = sx + sw;
				}
			}
			gfx.fill(x, top, x + w, bottom, 0xA0FFFFFF);
			gfx.fill(x + 1, top + 1, x + w - 1, bottom - 1, 0xC0303030);
			String label = Integer.toString(run.length());
			if (font.width(label) + 2 <= w) {
				gfx.drawString(font, label, x + (w - font.width(label)) / 2 + 1, top + 2, 0xFFFFFFFF, false);
			}
			x += w + 1;
		}
	}
}
