package com.lyleclassen.anglebuilder.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lyleclassen.anglebuilder.AngleBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;

/** The hand-edited {@code config/anglebuilder.json}. Read at start and on every world join. */
public final class Settings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<String> DEFAULT_PALETTE = List.of(
		"#00FFFF", "#FF00FF", "#FF8000", "#80FF00", "#3F7FFF", "#FF3F3F", "#9F5FFF", "#00BF9F");

	public static int reach = 64;
	public static int drawDistance = 64;
	public static int[] linePalette = parsePalette(DEFAULT_PALETTE);
	public static int previewColor = 0xFFFFFF;
	public static int targetColor = 0xFFFF00;
	public static int snapMaxPart = 4;

	private Settings() {}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve("anglebuilder.json");
	}

	public static void load() {
		Path file = file();
		JsonObject json = new JsonObject();
		boolean rewrite = false;
		if (Files.exists(file)) {
			try {
				JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
				if (parsed.isJsonObject()) {
					json = parsed.getAsJsonObject();
				} else {
					throw new IllegalStateException("not a JSON object");
				}
			} catch (Exception e) {
				AngleBuilder.LOGGER.warn("Invalid {}, backing it up and using defaults: {}", file, e.getMessage());
				try {
					Files.copy(file, file.resolveSibling("anglebuilder.json.bak"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException io) {
					AngleBuilder.LOGGER.warn("Could not back up {}", file, io);
				}
				json = new JsonObject();
			}
		}

		for (String key : List.of("reach", "drawDistance", "linePalette", "previewColor", "targetColor", "snapMaxPart")) {
			if (!json.has(key)) rewrite = true;
		}

		reach = readInt(json, "reach", 64, 8, 128);
		drawDistance = readInt(json, "drawDistance", 64, 16, 256);
		snapMaxPart = readInt(json, "snapMaxPart", 4, 1, 8);
		previewColor = readColor(json, "previewColor", 0xFFFFFF);
		targetColor = readColor(json, "targetColor", 0xFFFF00);
		linePalette = readPalette(json);

		if (rewrite || json.keySet().size() != 6) write();
	}

	private static int readInt(JsonObject json, String key, int def, int min, int max) {
		try {
			if (!json.has(key)) return def;
			int v = json.get(key).getAsInt();
			if (v < min || v > max) {
				AngleBuilder.LOGGER.warn("anglebuilder.json: {} = {} is out of range {}-{}, clamping", key, v, min, max);
			}
			return Math.max(min, Math.min(max, v));
		} catch (Exception e) {
			AngleBuilder.LOGGER.warn("anglebuilder.json: {} is not a whole number, using {}", key, def);
			return def;
		}
	}

	private static int readColor(JsonObject json, String key, int def) {
		if (!json.has(key)) return def;
		try {
			return parseColor(json.get(key).getAsString());
		} catch (Exception e) {
			AngleBuilder.LOGGER.warn("anglebuilder.json: {} is not a \"#RRGGBB\" colour, using default", key);
			return def;
		}
	}

	private static int[] readPalette(JsonObject json) {
		if (!json.has("linePalette")) return parsePalette(DEFAULT_PALETTE);
		try {
			JsonArray arr = json.getAsJsonArray("linePalette");
			List<String> colors = new ArrayList<>();
			arr.forEach(e -> colors.add(e.getAsString()));
			if (colors.isEmpty()) {
				AngleBuilder.LOGGER.warn("anglebuilder.json: linePalette is empty, using the default palette");
				return parsePalette(DEFAULT_PALETTE);
			}
			return parsePalette(colors);
		} catch (Exception e) {
			AngleBuilder.LOGGER.warn("anglebuilder.json: linePalette must be a list of \"#RRGGBB\" colours, using default");
			return parsePalette(DEFAULT_PALETTE);
		}
	}

	private static int[] parsePalette(List<String> colors) {
		int[] out = new int[colors.size()];
		for (int i = 0; i < out.length; i++) out[i] = parseColor(colors.get(i));
		return out;
	}

	static int parseColor(String s) {
		if (!s.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException(s);
		return Integer.parseInt(s.substring(1), 16);
	}

	private static String hex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	private static void write() {
		JsonObject out = new JsonObject();
		out.addProperty("reach", reach);
		out.addProperty("drawDistance", drawDistance);
		JsonArray palette = new JsonArray();
		for (int c : linePalette) palette.add(hex(c));
		out.add("linePalette", palette);
		out.addProperty("previewColor", hex(previewColor));
		out.addProperty("targetColor", hex(targetColor));
		out.addProperty("snapMaxPart", snapMaxPart);
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(out), StandardCharsets.UTF_8);
		} catch (IOException e) {
			AngleBuilder.LOGGER.warn("Could not write {}", file(), e);
		}
	}
}
