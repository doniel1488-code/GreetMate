package ru.governix.greet.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.governix.greet.config.GreetConfig;

public final class GreetMateClient implements ClientModInitializer {
    private static KeyBinding openSettingsKey;
    private static KeyBinding openWheelKey;

    @Override
    public void onInitializeClient() {
        GreetConfig.load();

        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.greetmate.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.greetmate"
        ));

        openWheelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.greetmate.open_wheel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.greetmate"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.wasPressed()) {
                client.setScreen(new WelcomeScreen(client.currentScreen));
            }
            while (openWheelKey.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.setScreen(new WheelScreen(client.currentScreen));
                }
            }
            WelcomeTracker.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WelcomeTracker.reset());
    }
}
