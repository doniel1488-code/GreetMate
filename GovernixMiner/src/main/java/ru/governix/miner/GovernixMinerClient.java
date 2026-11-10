package ru.governix.miner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class GovernixMinerClient {
    private static KeyBinding toggleKey;
    private static boolean enabled;
    private static boolean controllingAttackKey;
    private static int originalSlot = -1;

    private GovernixMinerClient() {}

    public static final class Entrypoint implements ClientModInitializer {
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
                restoreOriginalSlot(client);
            });
        }
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            if (client.player == null || client.world == null) {
                enabled = false;
                releaseAttackKey(client);
                restoreOriginalSlot(client);
                notify(client, "Сначала зайди в мир");
                continue;
            }

            if (!enabled) {
                originalSlot = client.player.getInventory().getSelectedSlot();
                enabled = true;
                notify(client, "Автодобыча включена");
            } else {
                enabled = false;
                releaseAttackKey(client);
                restoreOriginalSlot(client);
                notify(client, "Автодобыча выключена");
            }
        }

        if (shouldMine(client)) {
            BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());

            selectBestTool(client, state);

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

    /**
     * Selects the fastest suitable tool in the hotbar. If no stack is suitable,
     * it falls back to the stack with the highest raw mining speed multiplier.
     * Efficiency is included in the score when the base multiplier is above 1.
     */
    private static void selectBestTool(MinecraftClient client, BlockState state) {
        if (client.player == null || client.world == null) {
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        int currentSlot = inventory.getSelectedSlot();

        int bestSuitableSlot = -1;
        float bestSuitableSpeed = -1.0F;
        int bestAnySlot = currentSlot;
        float bestAnySpeed = miningSpeed(client, inventory.getStack(currentSlot), state);

        for (int slot = 0; slot < PlayerInventory.HOTBAR_SIZE; slot++) {
            ItemStack stack = inventory.getStack(slot);
            float speed = miningSpeed(client, stack, state);

            // The selected slot wins ties, preventing needless slot flicker.
            if (speed > bestAnySpeed + 0.0001F
                    || (slot == currentSlot && speed >= bestAnySpeed - 0.0001F)) {
                bestAnySpeed = speed;
                bestAnySlot = slot;
            }

            if (!stack.isEmpty() && stack.isSuitableFor(state)) {
                if (bestSuitableSlot < 0 || speed > bestSuitableSpeed + 0.0001F
                        || (slot == currentSlot && speed >= bestSuitableSpeed - 0.0001F)) {
                    bestSuitableSpeed = speed;
                    bestSuitableSlot = slot;
                }
            }
        }

        int chosenSlot = bestSuitableSlot >= 0 ? bestSuitableSlot : bestAnySlot;
        if (chosenSlot != currentSlot) {
            inventory.setSelectedSlot(chosenSlot);
        }
    }

    private static float miningSpeed(MinecraftClient client, ItemStack stack, BlockState state) {
        if (stack.isEmpty()) {
            return 1.0F;
        }

        float speed = stack.getMiningSpeedMultiplier(state);
        if (speed <= 1.0F) {
            return speed;
        }

        try {
            int efficiency = EnchantmentHelper.getLevel(
                    client.world.getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.EFFICIENCY),
                    stack
            );

            if (efficiency > 0) {
                speed += efficiency * efficiency + 1.0F;
            }
        } catch (RuntimeException ignored) {
            // If a modded registry does not expose Efficiency, still use the
            // item's normal mining speed instead of interrupting auto-mining.
        }

        return speed;
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

    private static void restoreOriginalSlot(MinecraftClient client) {
        if (originalSlot < 0 || originalSlot >= PlayerInventory.HOTBAR_SIZE || client.player == null) {
            originalSlot = -1;
            return;
        }

        client.player.getInventory().setSelectedSlot(originalSlot);
        originalSlot = -1;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(
                    "§8[§5GovernixMiner§8] §f" + message
            ));
        }
    }
}
