package com.lyleclassen.protractor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.lyleclassen.protractor.core.Pos;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws Lines as block outlines. Ordinary Lines and the preview are hidden by terrain and skip
 * Filled positions; the Target is drawn in full, with a faint fill, on top of terrain.
 */
final class LineRenderer {
	private static final double GROW = 0.002;
	private static final float TARGET_FILL_ALPHA = 0.18f;

	private LineRenderer() {}

	static void render(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) return;
		SavedLine preview = ProtractorClient.preview;
		SavedLine target = ProtractorClient.target;
		boolean showSaved = !ProtractorClient.hidden;
		if (!showSaved && preview == null) return;

		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		PoseStack pose = new PoseStack();
		pose.mulPose(event.getModelViewMatrix());
		pose.translate(-cam.x, -cam.y, -cam.z);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		RenderSystem.lineWidth(2.0f);

		// Depth-tested outlines: saved Lines (not the Target) and the preview.
		BufferBuilder lines = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		boolean any = false;
		double maxDistSq = (double) Settings.drawDistance * Settings.drawDistance;
		if (showSaved) {
			for (SavedLine line : ProtractorClient.currentLines()) {
				if (line == target || !withinDistance(line, cam, maxDistSq)) continue;
				any |= outlineUnfilled(pose, lines, level, line, line.color());
			}
		}
		if (preview != null) any |= outlineUnfilled(pose, lines, level, preview, Settings.previewColor);
		draw(lines, any);

		// The Target: on top of terrain, every position, outline plus faint fill.
		if (showSaved && target != null) {
			RenderSystem.disableDepthTest();
			int c = Settings.targetColor;
			float r = r(c), g = g(c), b = b(c);
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			BufferBuilder fill = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
			for (BlockPos p : target.blocks) {
				LevelRenderer.addChainedFilledBoxVertices(pose, fill,
					p.getX() - GROW, p.getY() - GROW, p.getZ() - GROW,
					p.getX() + 1 + GROW, p.getY() + 1 + GROW, p.getZ() + 1 + GROW,
					r, g, b, TARGET_FILL_ALPHA);
			}
			draw(fill, !target.blocks.isEmpty());

			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			BufferBuilder outline = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			for (BlockPos p : target.blocks) box(pose, outline, p, c);
			draw(outline, !target.blocks.isEmpty());
			RenderSystem.enableDepthTest();
		}

		RenderSystem.lineWidth(1.0f);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();

		// Anchor tags.
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		if (preview != null) {
			tag(pose, camera, buffers, mc.font, preview.line.anchor1(), "1", Settings.previewColor);
			if (!preview.line.anchor1().equals(preview.line.anchor2())) {
				tag(pose, camera, buffers, mc.font, preview.line.anchor2(), "2", Settings.previewColor);
			}
		}
		if (showSaved) {
			for (SavedLine line : ProtractorClient.currentLines()) {
				if (line != target && !withinDistance(line, cam, maxDistSq)) continue;
				int color = line == target ? Settings.targetColor : line.color();
				tag(pose, camera, buffers, mc.font, line.line.anchor1(), "1", color);
				tag(pose, camera, buffers, mc.font, line.line.anchor2(), "2", color);
			}
		}
		buffers.endBatch();
	}

	private static boolean outlineUnfilled(PoseStack pose, BufferBuilder buf, Level level, SavedLine line, int color) {
		boolean any = false;
		for (BlockPos p : line.blocks) {
			if (ProtractorClient.isFilled(level, p)) continue;
			box(pose, buf, p, color);
			any = true;
		}
		return any;
	}

	private static void box(PoseStack pose, BufferBuilder buf, BlockPos p, int color) {
		LevelRenderer.renderLineBox(pose, buf,
			p.getX() - GROW, p.getY() - GROW, p.getZ() - GROW,
			p.getX() + 1 + GROW, p.getY() + 1 + GROW, p.getZ() + 1 + GROW,
			r(color), g(color), b(color), 1.0f);
	}

	private static void draw(BufferBuilder buf, boolean any) {
		MeshData mesh = buf.build();
		if (mesh != null && any) BufferUploader.drawWithShader(mesh);
		else if (mesh != null) mesh.close();
	}

	/** A Line is drawn only while the camera is within the draw distance of its nearest block. */
	private static boolean withinDistance(SavedLine line, Vec3 cam, double maxDistSq) {
		for (BlockPos p : line.blocks) {
			double dx = p.getX() + 0.5 - cam.x, dy = p.getY() + 0.5 - cam.y, dz = p.getZ() + 0.5 - cam.z;
			if (dx * dx + dy * dy + dz * dz <= maxDistSq) return true;
		}
		return false;
	}

	private static void tag(PoseStack pose, Camera camera, MultiBufferSource buffers, Font font, Pos at, String text, int color) {
		pose.pushPose();
		pose.translate(at.x() + 0.5, at.y() + 1.35, at.z() + 0.5);
		pose.mulPose(camera.rotation());
		pose.scale(0.04f, -0.04f, 0.04f);
		float x = -font.width(text) / 2.0f;
		font.drawInBatch(text, x, 0, 0xFF000000 | color, true, pose.last().pose(), buffers,
			Font.DisplayMode.NORMAL, 0x40000000, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	private static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
	private static float g(int c) { return ((c >> 8) & 0xFF) / 255f; }
	private static float b(int c) { return (c & 0xFF) / 255f; }
}
