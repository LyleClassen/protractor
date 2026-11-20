package com.lyleclassen.protractor.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds, rebindable in vanilla Controls under "Protractor". Setting Anchors, deleting and
 * clearing are done with the Protractor instead of keys.
 */
public final class Keys {
	private static final String CATEGORY = "key.categories.protractor";

	public static final KeyMapping SNAP = key("snap", GLFW.GLFW_KEY_LEFT_ALT);
	public static final KeyMapping HIDE = key("hide", InputConstants.UNKNOWN.getValue());

	private Keys() {}

	private static KeyMapping key(String name, int code) {
		return new KeyMapping("key.protractor." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
	}

	static void register(RegisterKeyMappingsEvent event) {
		event.register(SNAP);
		event.register(HIDE);
	}
}
