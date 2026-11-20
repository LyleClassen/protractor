package com.lyleclassen.anglebuilder.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Keybinds, rebindable in vanilla Controls under "Angle Builder". */
public final class Keys {
	private static final String CATEGORY = "key.categories.anglebuilder";

	public static final KeyMapping ANCHOR = key("anchor", GLFW.GLFW_KEY_V);
	public static final KeyMapping CANCEL = key("cancel", GLFW.GLFW_KEY_B);
	public static final KeyMapping SNAP = key("snap", GLFW.GLFW_KEY_LEFT_ALT);
	public static final KeyMapping DELETE = key("delete", GLFW.GLFW_KEY_N);
	public static final KeyMapping CLEAR = key("clear", InputConstants.UNKNOWN.getValue());
	public static final KeyMapping HIDE = key("hide", InputConstants.UNKNOWN.getValue());

	private Keys() {}

	private static KeyMapping key(String name, int code) {
		return new KeyMapping("key.anglebuilder." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
	}

	static void register(RegisterKeyMappingsEvent event) {
		event.register(ANCHOR);
		event.register(CANCEL);
		event.register(SNAP);
		event.register(DELETE);
		event.register(CLEAR);
		event.register(HIDE);
	}
}
