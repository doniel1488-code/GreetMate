package ru.governix.greet.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import ru.governix.greet.config.GreetConfig;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class WelcomeTracker {
    private static final Set<UUID> LISTED_IDS = new HashSet<>();
    // Once a player has been greeted, TAB hide/show updates must not greet them again.
    // This set is cleared only by an actual PlayerRemove packet or disconnect.
    private static final Set<UUID> GREETED_THIS_SESSION = new HashSet<>();
    private static final Queue<String> OUTGOING = new ArrayDeque<>();

    private static boolean initialized;
    private static int sendCooldown;

    private WelcomeTracker() {}

    public static void reset() {
        LISTED_IDS.clear();
        GREETED_THIS_SESSION.clear();
        OUTGOING.clear();
        initialized = false;
        sendCooldown = 0;
    }

    public static void handlePlayerList(MinecraftClient client, ClientPlayNetworkHandler handler,
                                        PlayerListS2CPacket packet) {
        boolean addsPlayers = packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER);
        boolean updatesListed = packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_LISTED);

        if (!addsPlayers && !updatesListed) return;

        // Cache everybody currently visible on first packet, so joining a server with
        // an already populated TAB never sends a burst of old greetings.
        if (!initialized) {
            for (PlayerListEntry playerEntry : handler.getListedPlayerListEntries()) {
                UUID id = playerEntry.getProfile().getId();
                LISTED_IDS.add(id);
                GREETED_THIS_SESSION.add(id);
            }
            initialized = true;
            return;
        }

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            UUID id = entry.profileId();

            if (updatesListed && !entry.listed()) {
                // Player is hidden from TAB, but is still connected. Keep the session cache.
                LISTED_IDS.remove(id);
                continue;
            }

            if (!entry.listed()) continue;

            PlayerListEntry currentEntry = handler.getPlayerListEntry(id);
            String name = currentEntry != null
                    ? currentEntry.getProfile().getName()
                    : entry.profile() != null ? entry.profile().getName() : null;
            if (name == null || name.isBlank()) continue;

            if (!addsPlayers && !updatesListed) continue;

            boolean newlyListed = LISTED_IDS.add(id);
            if (!newlyListed || client.player == null) continue;

            // add() returning false means this player already received an automatic greeting
            // during this connection, even if they disappeared/reappeared in TAB.
            if (!GREETED_THIS_SESSION.add(id)) continue;
            if (!GreetConfig.isAutoGreetingEnabled()) continue;

            String chosen = chooseGreeting(name);
            if (chosen != null) queueMessage(chosen, name);
        }
    }

    public static void handlePlayerRemove(List<UUID> removedIds) {
        // This packet represents a real player-list removal, unlike UPDATE_LISTED=false.
        for (UUID id : removedIds) {
            LISTED_IDS.remove(id);
            GREETED_THIS_SESSION.remove(id);
        }
    }

    private static String chooseGreeting(String playerName) {
        if (!GreetConfig.containsTarget(playerName)) return null;
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

    private static final java.util.Map<String, String> LAST_GREETING = new java.util.HashMap<>();

    private static void queueMessage(String template, String playerName) {
        String message = template.replace("{player}", playerName).replace("{name}", playerName);
        if (!message.isBlank()) OUTGOING.add(message);
    }

    public static boolean sendManualGreeting(String playerName, String template) {
        if (playerName == null || template == null || template.isBlank()) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (client.player == null || handler == null || !isOnline(playerName)) return false;

        PlayerListEntry entry = handler.getPlayerList().stream()
                .filter(value -> value.getProfile().getName().equalsIgnoreCase(playerName))
                .findFirst().orElse(null);
        if (entry != null) GREETED_THIS_SESSION.add(entry.getProfile().getId());

        queueMessage(template, playerName);
        return true;
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
        if (name == null || name.isBlank()) return false;
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return false;

        for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
            String listedName = entry.getProfile().getName();
            if (listedName != null && listedName.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static List<String> getOnlineTargets() {
        return GreetConfig.getTargets().stream().filter(WelcomeTracker::isOnline).toList();
    }
}
