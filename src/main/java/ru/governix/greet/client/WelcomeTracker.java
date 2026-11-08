package ru.governix.greet.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import ru.governix.greet.config.GreetConfig;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class WelcomeTracker {
    private static final Set<UUID> ONLINE_IDS = new HashSet<>();
    private static final Map<UUID, String> ONLINE_NAMES = new HashMap<>();
    private static final Map<String, String> LAST_GREETING = new HashMap<>();
    private static final Queue<String> OUTGOING = new ArrayDeque<>();

    private static boolean initialized;
    private static int sendCooldown;

    private WelcomeTracker() {}

    public static void reset() {
        ONLINE_IDS.clear();
        ONLINE_NAMES.clear();
        LAST_GREETING.clear();
        OUTGOING.clear();
        initialized = false;
        sendCooldown = 0;
    }

    public static void handlePlayerList(MinecraftClient client, ClientPlayNetworkHandler handler,
                                        PlayerListS2CPacket packet) {
        if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) return;

        // The first player-list update establishes the initial TAB snapshot.
        // Existing players are remembered, not greeted.
        if (!initialized) {
            for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
                UUID id = entry.getProfile().getId();
                String name = entry.getProfile().getName();
                ONLINE_IDS.add(id);
                ONLINE_NAMES.put(id, name);
            }
            initialized = true;
            return;
        }

        for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
            if (!entry.listed() || entry.profile() == null) continue;

            UUID id = entry.profileId();
            String name = entry.profile().getName();
            if (name == null || name.isBlank()) continue;

            boolean newToTab = ONLINE_IDS.add(id);
            ONLINE_NAMES.put(id, name);
            if (!newToTab || client.player == null) continue;
            if (!GreetConfig.isEnabled() || !GreetConfig.containsTarget(name)) continue;

            String chosen = chooseGreeting(name);
            if (chosen != null) {
                OUTGOING.add(chosen.replace("{player}", name).replace("{name}", name));
            }
        }
    }

    public static void handlePlayerRemove(List<UUID> removedIds) {
        for (UUID id : removedIds) {
            ONLINE_IDS.remove(id);
            ONLINE_NAMES.remove(id);
        }
    }

    private static String chooseGreeting(String playerName) {
        List<String> messages = GreetConfig.getGreetings(playerName);
        if (messages.isEmpty()) return null;

        String key = GreetConfig.normalize(playerName);
        String previous = LAST_GREETING.get(key);
        List<String> choices = messages;
        if (messages.size() > 1 && previous != null) {
            choices = messages.stream().filter(message -> !message.equals(previous)).toList();
        }

        String selected = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        LAST_GREETING.put(key, selected);
        return selected;
    }

    public static void tick(MinecraftClient client) {
        if (sendCooldown > 0) sendCooldown--;
        if (sendCooldown > 0 || OUTGOING.isEmpty()) return;
        if (client.player == null || client.getNetworkHandler() == null) return;

        String message = OUTGOING.poll();
        if (message != null && !message.isBlank()) {
            client.getNetworkHandler().sendChatMessage(message);
            sendCooldown = 35;
        }
    }

    public static boolean isOnline(String name) {
        return ONLINE_NAMES.values().stream().anyMatch(value -> value.equalsIgnoreCase(name));
    }
}
