package com.lyleclassen.protractor.client;

import com.lyleclassen.protractor.ProtractorMod;
import com.lyleclassen.protractor.core.Line;
import com.lyleclassen.protractor.core.Pos;
import com.lyleclassen.protractor.core.Snap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * All client state: pending Anchor, preview, Target and Hide toggle. The Protractor is driven
 * here: right-click sets an Anchor, sneak + right-click cancels a pending Anchor or deletes the
 * Target, and a quick second sneak + right-click clears every Line in the dimension.
 */
public final class ProtractorClient {
	private static final long DOUBLE_CLICK_MS = 400;

	static final LineStore STORE = new LineStore();
	@Nullable static Pos anchor1;
	@Nullable static SavedLine preview;
	@Nullable static SavedLine target;
	static boolean hidden;
	private static long lastSneakClickAt;
	/** Whether the use key was down at the end of the last tick, to ignore held-button repeats. */
	private static boolean useHeldLastTick;
	@Nullable private static String dimension;

	private ProtractorClient() {}

	public static void init(IEventBus modBus) {
		Settings.load();
		modBus.addListener(Keys::register);
		modBus.addListener(ProtractorClient::registerGui);
		NeoForge.EVENT_BUS.addListener(ProtractorClient::onTick);
		NeoForge.EVENT_BUS.addListener(ProtractorClient::onLogin);
		NeoForge.EVENT_BUS.addListener(ProtractorClient::onLogout);
		NeoForge.EVENT_BUS.addListener(ProtractorClient::onRenderLevel);
		NeoForge.EVENT_BUS.addListener(ProtractorClient::onUse);
	}

	private static void registerGui(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(ProtractorMod.MOD_ID, "measurement"), MeasurementHud::render);
	}

	private static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) LineRenderer.render(event);
	}

	private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		Settings.load();
		STORE.load(Minecraft.getInstance());
		STORE.rebuildCaches();
		resetTransient();
	}

	private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		STORE.unload();
		resetTransient();
	}

	private static void resetTransient() {
		anchor1 = null;
		preview = null;
		target = null;
		hidden = false;
		lastSneakClickAt = 0;
		dimension = null;
	}

	static String dimension() {
		return dimension == null ? "" : dimension;
	}

	static List<SavedLine> currentLines() {
		return STORE.lines(dimension());
	}

	private static void onTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		String dim = mc.level.dimension().location().toString();
		if (!dim.equals(dimension)) {
			dimension = dim;
			anchor1 = null;
			preview = null;
			target = null;
		}

		while (Keys.HIDE.consumeClick()) {
			hidden = !hidden;
			mc.gui.setOverlayMessage(Component.translatable(hidden ? "protractor.hidden" : "protractor.shown"), false);
		}

		updatePreview(mc);
		updateTarget(mc);
		useHeldLastTick = mc.options.keyUse.isDown();
	}

	/**
	 * Right-click with the Protractor. Cancelled before vanilla sees it, so the click never opens a
	 * chest or reaches the server; holding the button down doesn't repeat.
	 */
	private static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isUseItem()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (!mc.player.getItemInHand(event.getHand()).is(ProtractorMod.PROTRACTOR.get())) return;
		event.setCanceled(true);
		event.setSwingHand(false);
		if (useHeldLastTick) return;
		if (mc.player.isShiftKeyDown()) onSneakUse(mc);
		else onAnchorUse(mc);
	}

	private static void onSneakUse(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (lastSneakClickAt != 0 && now - lastSneakClickAt <= DOUBLE_CLICK_MS) {
			lastSneakClickAt = 0;
			anchor1 = null;
			preview = null;
			int removed = STORE.clear(dimension());
			target = null;
			mc.gui.setOverlayMessage(Component.translatable("protractor.cleared", removed), false);
			return;
		}
		lastSneakClickAt = now;
		if (anchor1 != null) {
			anchor1 = null;
			preview = null;
			return;
		}
		updateTarget(mc);
		if (target != null) {
			STORE.remove(dimension(), target);
			target = null;
			updateTarget(mc);
		}
	}

	private static void onAnchorUse(Minecraft mc) {
		hidden = false;
		BlockPos aimed = aimedAnchor(mc);
		if (aimed == null) return;
		Pos pos = SavedLine.toPos(aimed);
		if (anchor1 == null) {
			anchor1 = pos;
			updatePreview(mc);
			return;
		}
		Pos second = preview != null ? preview.line.anchor2() : pos;
		if (second.equals(anchor1)) return;
		STORE.add(dimension(), new Line(anchor1, second));
		anchor1 = null;
		preview = null;
	}

	private static void updatePreview(Minecraft mc) {
		if (anchor1 == null) {
			preview = null;
			return;
		}
		BlockPos aimed = aimedAnchor(mc);
		if (aimed == null) {
			// Keep the last preview so the Measurement doesn't flicker when looking at the sky.
			if (preview == null) preview = new SavedLine(new Line(anchor1, anchor1));
			return;
		}
		Pos second = SavedLine.toPos(aimed);
		if (Keys.SNAP.isDown() && !second.equals(anchor1)) second = Snap.anchor2(anchor1, second, Settings.snapMaxPart);
		if (preview == null || !preview.line.anchor2().equals(second)) preview = new SavedLine(new Line(anchor1, second));
	}

	/** The space next to the face the crosshair hits, within the far reach. */
	@Nullable
	private static BlockPos aimedAnchor(Minecraft mc) {
		var player = mc.player;
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(Settings.reach));
		BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) return null;
		return hit.getBlockPos().relative(hit.getDirection());
	}

	/** The nearest Line block the view ray passes through, ignoring terrain. Newest Line wins ties. */
	private static void updateTarget(Minecraft mc) {
		target = null;
		if (hidden || anchor1 != null) return;
		var player = mc.player;
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 dir = player.getViewVector(1.0f);
		double best = Settings.reach;
		List<SavedLine> lines = currentLines();
		for (int i = lines.size() - 1; i >= 0; i--) {
			SavedLine line = lines.get(i);
			for (BlockPos p : line.blocks) {
				double t = rayBox(eye, dir, p);
				if (t >= 0 && t < best) {
					best = t;
					target = line;
				}
			}
		}
	}

	/** Distance along the ray to a full block, or -1 if it misses. */
	private static double rayBox(Vec3 o, Vec3 d, BlockPos p) {
		double tMin = 0, tMax = Double.MAX_VALUE;
		double[] origin = {o.x, o.y, o.z};
		double[] dir = {d.x, d.y, d.z};
		int[] lo = {p.getX(), p.getY(), p.getZ()};
		for (int a = 0; a < 3; a++) {
			if (Math.abs(dir[a]) < 1e-9) {
				if (origin[a] < lo[a] || origin[a] > lo[a] + 1) return -1;
				continue;
			}
			double t1 = (lo[a] - origin[a]) / dir[a];
			double t2 = (lo[a] + 1 - origin[a]) / dir[a];
			if (t1 > t2) {
				double tmp = t1;
				t1 = t2;
				t2 = tmp;
			}
			tMin = Math.max(tMin, t1);
			tMax = Math.min(tMax, t2);
			if (tMin > tMax) return -1;
		}
		return tMin;
	}

	/**
	 * A position is Filled when it holds a block you'd have to break before building there. Air,
	 * plants, snow layers and fluids leave it unfilled; so do unloaded chunks.
	 */
	static boolean isFilled(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) return false;
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.canBeReplaced()) return false;
		var block = state.getBlock();
		return !(block instanceof BushBlock || block instanceof SnowLayerBlock || block instanceof LiquidBlock);
	}
}
