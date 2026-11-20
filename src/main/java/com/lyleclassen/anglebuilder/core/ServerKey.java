package com.lyleclassen.anglebuilder.core;

import java.util.Locale;

/** Turns a server address into the file name its Lines are stored under. */
public final class ServerKey {
	private ServerKey() {}

	/**
	 * Lowercased, with the default port stripped. Characters that aren't safe in a file name on
	 * every OS (notably ':' on Windows) become '_'.
	 */
	public static String fileName(String address) {
		String key = address.trim().toLowerCase(Locale.ROOT);
		if (key.endsWith(":25565")) key = key.substring(0, key.length() - ":25565".length());
		key = key.replaceAll("[^a-z0-9._\\-\\[\\]]", "_");
		if (key.isEmpty() || key.chars().allMatch(ch -> ch == '.')) key = "_";
		return key + ".json";
	}
}
