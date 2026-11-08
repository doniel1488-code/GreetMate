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
    // Tracks transitions into/out of the server's listed TAB state for greeting events.
    private static final Set<UUID> LISTED_IDS = new HashSet<>();
    private static final Map<String, String> LAST_GREETING = new HashMap<>();
    private static final Queue<String> OUTGOING = new ArrayDeque<>();

    private static boolean initialized;
    private static int sendCooldown;

    private WelcomeTracker() {}

    public static void reset() {
        LISTED_IDS.clear();
        LAST_GREETING.clear();
        OUTGOING.clear();
        initialized = false;
        sendCooldown = 0;
    }

    public static void handlePlayerList(MinecraftClient client, ClientPlayNetworkHandler handler,
                                        PlayerListS2CPacket packet) {
        boolean addsPlayers = packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER);
        boolean updatesListed = packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_LISTED);

        if (!addsPlayers && !updatesListed) return;

        // Snapshot the initial TAB roster once. Do not greet people who were already online
        // when this client finished connecting.
        if (!initialized) {
            for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
                LISTED_IDS.add(entry.getProfile().getId());
            }
            initialized = true;
            return;
        }

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            UUID id = entry.profileId();

            // UPDATE_LISTED=false means the server explicitly removed this player from TAB.
            // If they are listed again later, that transition can be detected as a new entry.
            if (updatesListed && !entry.listed()) {
                LISTED_IDS.remove(id);
                continue;
            }

            if (!entry.listed()) continue;

            PlayerListEntry currentEntry = handler.getPlayerListEntry(id);
            String name = currentEntry != null
                    ? currentEntry.getProfile().getName()
                    : entry.profile() != null ? entry.profile().getName() : null;

            if (name == null || name.isBlank()) continue;

            // Only ADD_PLAYER and UPDATE_LISTED can represent a new appearance in TAB.
            if (!addsPlayers && !updatesListed) continue;

            boolean newlyListed = LISTED_IDS.add(id);
            if (!newlyListed || client.player == null) continue;
            if (!GreetConfig.isEnabled() || !GreetConfig.containsTarget(name)) continue;

            String chosen = chooseGreeting(name);
            if (chosen != null) {
                OUTGOING.add(chosen.replace("{player}", name).replace("{name}", name));
            }
        }
    }

    public static void handlePlayerRemove(List<UUID> removedIds) {
        LISTED_IDS.removeAll(removedIds);
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

    /**
     * Read the live list maintained by Minecraft itself instead of a second,
     * potentially stale mod-side cache. This is the authoritative TAB-listed state.
     */
    public static boolean isOnline(String name) {
        if (name == null || name.isBlank()) return false;

        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return false;

        for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
            String listedName = entry.getProfile().getName();
            if (listedName != null && listedName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
