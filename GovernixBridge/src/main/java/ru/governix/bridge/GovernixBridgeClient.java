package ru.governix.bridge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class GovernixBridgeClient implements ClientModInitializer {
    private static KeyBinding toggleAssistKey;
    private static KeyBinding toggleGuardKey;

    @Override
    public void onInitializeClient() {
        BridgeConfig.load();

        toggleAssistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.governixbridge.toggle_assist",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.governixbridge"
        ));

        toggleGuardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.governixbridge.toggle_guard",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.governixbridge"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleAssistKey.wasPressed()) {
                BridgeConfig.laneAssist = !BridgeConfig.laneAssist;
                BridgeConfig.save();
                BridgeController.resetLane();
                notify(client, "Lane Assist", BridgeConfig.laneAssist);
            }

            while (toggleGuardKey.wasPressed()) {
                BridgeConfig.edgeGuard = !BridgeConfig.edgeGuard;
                BridgeConfig.save();
                notify(client, "Edge Guard", BridgeConfig.edgeGuard);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BridgeController.resetLane());
    }

    private static void notify(MinecraftClient client, String feature, boolean enabled) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(
                    "§8[§5GovernixBridge§8] §f" + feature + ": " + (enabled ? "§aON" : "§cOFF")));
        }
    }
}
