package ru.governix.greet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class GreetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("greetmate.json");
    private static final Pattern VALID_NICK = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final int MAX_GREETINGS_PER_PLAYER = 32;
    private static final int MAX_GREETING_LENGTH = 256;

    // "enabled" from 1.x configs is still read and written for backward compatibility.
    private static boolean autoGreetingEnabled = true;
    private static final Set<String> targets = new LinkedHashSet<>();
    private static final Map<String, List<String>> greetings = new LinkedHashMap<>();

    private GreetConfig() {}

    public static synchronized void load() {
        targets.clear();
        greetings.clear();
        autoGreetingEnabled = true;
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();

            if (root.has("autoGreetingEnabled")) {
                autoGreetingEnabled = root.get("autoGreetingEnabled").getAsBoolean();
            } else if (root.has("enabled")) {
                autoGreetingEnabled = root.get("enabled").getAsBoolean();
            }

            if (root.has("targets") && root.get("targets").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("targets")) {
                    if (element.isJsonPrimitive()) addTargetInternal(element.getAsString());
                }
            }

            if (root.has("greetings") && root.get("greetings").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("greetings").entrySet()) {
                    String nick = entry.getKey();
                    if (!VALID_NICK.matcher(nick).matches() || !entry.getValue().isJsonArray()) continue;
                    addTargetInternal(nick);
                    List<String> values = new ArrayList<>();
                    for (JsonElement value : entry.getValue().getAsJsonArray()) {
                        if (!value.isJsonPrimitive()) continue;
                        String message = value.getAsString();
                        if (!message.isBlank() && message.length() <= MAX_GREETING_LENGTH
                                && !values.contains(message)) values.add(message);
                        if (values.size() >= MAX_GREETINGS_PER_PLAYER) break;
                    }
                    greetings.put(normalize(nick), values);
                }
            }
            for (String nick : targets) greetings.putIfAbsent(normalize(nick), new ArrayList<>());
        } catch (Exception exception) {
            System.err.println("[GreetMate] Config read failed: " + exception.getMessage());
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("autoGreetingEnabled", autoGreetingEnabled);
        root.addProperty("enabled", autoGreetingEnabled); // Legacy key for 1.x compatibility.

        JsonArray targetArray = new JsonArray();
        targets.forEach(targetArray::add);
        root.add("targets", targetArray);

        JsonObject greetingObject = new JsonObject();
        for (String nick : targets) {
            JsonArray messages = new JsonArray();
            greetings.getOrDefault(normalize(nick), List.of()).forEach(messages::add);
            greetingObject.add(nick, messages);
        }
        root.add("greetings", greetingObject);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[GreetMate] Config write failed: " + exception.getMessage());
        }
    }

    private static void addTargetInternal(String nick) {
        String value = nick == null ? "" : nick.trim();
        if (!VALID_NICK.matcher(value).matches() || containsTargetInternal(value)) return;
        targets.add(value);
        greetings.putIfAbsent(normalize(value), new ArrayList<>());
    }

    public static synchronized boolean addTarget(String nick) {
        String value = nick == null ? "" : nick.trim();
        if (!VALID_NICK.matcher(value).matches() || containsTargetInternal(value)) return false;
        targets.add(value);
        greetings.put(normalize(value), new ArrayList<>());
        save();
        return true;
    }

    public static synchronized boolean removeTarget(String nick) {
        if (nick == null) return false;
        boolean removed = targets.removeIf(value -> value.equalsIgnoreCase(nick));
        if (removed) {
            greetings.remove(normalize(nick));
            save();
        }
        return removed;
    }

    public static synchronized boolean containsTarget(String nick) {
        return nick != null && containsTargetInternal(nick);
    }

    private static boolean containsTargetInternal(String nick) {
        return targets.stream().anyMatch(value -> value.equalsIgnoreCase(nick));
    }

    public static synchronized List<String> getTargets() {
        return new ArrayList<>(targets);
    }

    public static synchronized boolean addGreeting(String nick, String message) {
        if (nick == null || !containsTargetInternal(nick)) return false;
        String value = message == null ? "" : message.trim();
        if (value.isEmpty() || value.length() > MAX_GREETING_LENGTH) return false;
        List<String> values = greetings.computeIfAbsent(normalize(nick), key -> new ArrayList<>());
        if (values.size() >= MAX_GREETINGS_PER_PLAYER || values.contains(value)) return false;
        values.add(value);
        save();
        return true;
    }

    public static synchronized boolean removeGreeting(String nick, int index) {
        if (nick == null) return false;
        List<String> values = greetings.get(normalize(nick));
        if (values == null || index < 0 || index >= values.size()) return false;
        values.remove(index);
        save();
        return true;
    }

    public static synchronized List<String> getGreetings(String nick) {
        if (nick == null) return List.of();
        return new ArrayList<>(greetings.getOrDefault(normalize(nick), List.of()));
    }

    public static synchronized boolean isAutoGreetingEnabled() {
        return autoGreetingEnabled;
    }

    public static synchronized void setAutoGreetingEnabled(boolean value) {
        autoGreetingEnabled = value;
        save();
    }

    // Compatibility aliases used by older UI code.
    public static synchronized boolean isEnabled() {
        return isAutoGreetingEnabled();
    }

    public static synchronized void setEnabled(boolean value) {
        setAutoGreetingEnabled(value);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
