package ru.governix.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BridgeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("governixbridge.json");

    public static boolean laneAssist = true;
    public static boolean edgeGuard = false;
    public static boolean requireRightClick = true;
    public static float strength = 0.85F;

    private BridgeConfig() {}

    public static synchronized void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            laneAssist = readBoolean(root, "laneAssist", true);
            edgeGuard = readBoolean(root, "edgeGuard", false);
            requireRightClick = readBoolean(root, "requireRightClick", true);
            strength = Math.max(0.25F, Math.min(1.50F, readFloat(root, "strength", 0.85F)));
        } catch (Exception exception) {
            System.err.println("[GovernixBridge] Failed to read config: " + exception.getMessage());
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("laneAssist", laneAssist);
        root.addProperty("edgeGuard", edgeGuard);
        root.addProperty("requireRightClick", requireRightClick);
        root.addProperty("strength", strength);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[GovernixBridge] Failed to save config: " + exception.getMessage());
        }
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float readFloat(JsonObject root, String key, float fallback) {
        try {
            return root.has(key) ? root.get(key).getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
