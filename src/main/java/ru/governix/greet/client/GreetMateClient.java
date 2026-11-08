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
    private static KeyBinding openScreenKey;

    @Override
    public void onInitializeClient() {
        GreetConfig.load();

        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.greetmate.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.greetmate"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKey.wasPressed()) {
                client.setScreen(new WelcomeScreen(client.currentScreen));
            }
            WelcomeTracker.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WelcomeTracker.reset());
    }
}
