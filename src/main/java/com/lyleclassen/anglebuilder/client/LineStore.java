package com.lyleclassen.anglebuilder.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lyleclassen.anglebuilder.AngleBuilder;
import com.lyleclassen.anglebuilder.core.Line;
import com.lyleclassen.anglebuilder.core.Pos;
import com.lyleclassen.anglebuilder.core.ServerKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Saved Lines for the world or server the player is in, grouped by dimension id. Singleplayer
 * Lines live in {@code saves/<world>/anglebuilder/lines.json} (see ADR 0001); servers use
 * {@code <gameDir>/anglebuilder/servers/<address>.json}. Written atomically on every change.
 */
public final class LineStore {
	private static final int VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Map<String, List<SavedLine>> byDimension = new HashMap<>();
	@Nullable private Path file;
	private boolean readOnly;

	/** Lines in creation order (newest last). Never null. */
	public List<SavedLine> lines(String dimension) {
		return byDimension.computeIfAbsent(dimension, d -> new ArrayList<>());
	}

	public void add(String dimension, Line line) {
		lines(dimension).add(new SavedLine(line));
		save();
	}

	public void remove(String dimension, SavedLine line) {
		if (lines(dimension).remove(line)) save();
	}

	public int clear(String dimension) {
		int n = lines(dimension).size();
		lines(dimension).clear();
		if (n > 0) save();
		return n;
	}

	public void rebuildCaches() {
		byDimension.replaceAll((dim, list) -> new ArrayList<>(list.stream().map(s -> new SavedLine(s.line)).toList()));
	}

	public void unload() {
		byDimension.clear();
		file = null;
		readOnly = false;
	}

	public void load(Minecraft mc) {
		unload();
		file = locate(mc);
		if (file == null || !Files.exists(file)) return;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			int version = root.has("version") ? root.get("version").getAsInt() : VERSION;
			if (version > VERSION) {
				AngleBuilder.LOGGER.warn("{} was written by a newer Angle Builder (version {}); leaving it alone", file, version);
				readOnly = true;
				return;
			}
			JsonObject dims = root.getAsJsonObject("dimensions");
			for (Map.Entry<String, JsonElement> e : dims.entrySet()) {
				List<SavedLine> list = lines(e.getKey());
				for (JsonElement el : e.getValue().getAsJsonArray()) {
					JsonObject o = el.getAsJsonObject();
					list.add(new SavedLine(new Line(readPos(o.getAsJsonArray("anchor1")), readPos(o.getAsJsonArray("anchor2")))));
				}
			}
		} catch (Exception e) {
			AngleBuilder.LOGGER.warn("Corrupt {}, backing it up and starting empty: {}", file, e.toString());
			byDimension.clear();
			try {
				Files.move(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException io) {
				AngleBuilder.LOGGER.warn("Could not back up {}", file, io);
				readOnly = true;
			}
		}
	}

	private void save() {
		if (file == null || readOnly) return;
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		JsonObject dims = new JsonObject();
		Map<String, List<SavedLine>> sorted = new LinkedHashMap<>();
		byDimension.keySet().stream().sorted().forEach(k -> sorted.put(k, byDimension.get(k)));
		sorted.forEach((dim, list) -> {
			if (list.isEmpty()) return;
			JsonArray arr = new JsonArray();
			for (SavedLine s : list) {
				JsonObject o = new JsonObject();
				o.add("anchor1", writePos(s.line.anchor1()));
				o.add("anchor2", writePos(s.line.anchor2()));
				arr.add(o);
			}
			dims.add(dim, arr);
		});
		root.add("dimensions", dims);
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			AngleBuilder.LOGGER.warn("Could not save Lines to {}", file, e);
		}
	}

	@Nullable
	private static Path locate(Minecraft mc) {
		if (mc.getSingleplayerServer() != null) {
			return mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("anglebuilder").resolve("lines.json");
		}
		String address = null;
		ServerData server = mc.getCurrentServer();
		if (server != null) address = server.ip;
		if ((address == null || address.isBlank()) && mc.getConnection() != null) {
			address = String.valueOf(mc.getConnection().getConnection().getRemoteAddress());
		}
		if (address == null || address.isBlank()) return null;
		return FMLPaths.GAMEDIR.get().resolve("anglebuilder").resolve("servers").resolve(ServerKey.fileName(address));
	}

	private static Pos readPos(JsonArray a) {
		return new Pos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}

	private static JsonArray writePos(Pos p) {
		JsonArray a = new JsonArray();
		a.add(p.x());
		a.add(p.y());
		a.add(p.z());
		return a;
	}
}
