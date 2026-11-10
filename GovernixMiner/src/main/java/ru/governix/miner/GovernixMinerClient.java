package ru.governix.miner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class GovernixMinerClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static boolean enabled;
    private static boolean controllingAttackKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.governixminer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.governixminer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(GovernixMinerClient::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            enabled = false;
            releaseAttackKey(client);
        });
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            if (client.player == null || client.world == null) {
                enabled = false;
                releaseAttackKey(client);
                notify(client, "Сначала зайди в мир");
                continue;
            }

            enabled = !enabled;
            if (!enabled) {
                releaseAttackKey(client);
            }
            notify(client, enabled ? "Автодобыча включена" : "Автодобыча выключена");
        }

        if (shouldMine(client)) {
            // Use Minecraft's ordinary attack key path. Vanilla retains block
            // reach, mining speed, tool checks and server-side validation.
            client.options.attackKey.setPressed(true);
            controllingAttackKey = true;
        } else {
            releaseAttackKey(client);
        }
    }

    private static boolean shouldMine(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null) {
            return false;
        }

        if (!(client.crosshairTarget instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockState state = client.world.getBlockState(blockHit.getBlockPos());
        return !state.isAir();
    }

    private static void releaseAttackKey(MinecraftClient client) {
        if (!controllingAttackKey) {
            return;
        }

        if (client.options != null) {
            client.options.attackKey.setPressed(false);
        }

        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            client.interactionManager.cancelBlockBreaking();
        }

        controllingAttackKey = false;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(
                    "§8[§5GovernixMiner§8] §f" + message
            ));
        }
    }
}
