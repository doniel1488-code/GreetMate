package ru.governix.bridge;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

/**
 * Adjusts only local movement input. It never teleports the player, edits blocks,
 * sends extra placement packets, or changes the selected item.
 */
public final class BridgeController {
    private static boolean laneLocked;
    private static double laneX;
    private static double laneZ;
    private static int inactiveTicks;

    private BridgeController() {}

    public static Vec2f adjust(Vec2f original) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (!BridgeConfig.laneAssist || player == null || client.world == null
                || client.currentScreen != null || player.hasVehicle()
                || player.isTouchingWater() || player.isInLava()) {
            resetLane();
            return original;
        }

        if (BridgeConfig.requireRightClick && !client.options.useKey.isPressed()) {
            inactiveTicks++;
            if (inactiveTicks > 8) resetLane();
            return original;
        }

        if (original.x * original.x + original.y * original.y < 0.02F || !player.isOnGround()) {
            inactiveTicks++;
            if (inactiveTicks > 8) resetLane();
            return original;
        }
        inactiveTicks = 0;

        double yaw = Math.toRadians(player.getYaw());
        // Minecraft's positive sideways input is left.
        double leftX = Math.cos(yaw);
        double leftZ = Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);

        double moveX = leftX * original.x + forwardX * original.y;
        double moveZ = leftZ * original.x + forwardZ * original.y;
        double moveLength = Math.hypot(moveX, moveZ);
        if (moveLength < 1.0E-4) return original;
        moveX /= moveLength;
        moveZ /= moveLength;

        if (!laneLocked) {
            laneX = moveX;
            laneZ = moveZ;
            laneLocked = true;
        }

        BlockPos support = findNearestSupport(client, player, laneX, laneZ);
        if (support == null) return original;

        // Keep the player's line centered across the bridge, without changing
        // the direction of travel along the bridge itself.
        double sideX = -laneZ;
        double sideZ = laneX;
        double error = (player.getX() - (support.getX() + 0.5)) * sideX
                + (player.getZ() - (support.getZ() + 0.5)) * sideZ;

        double correction = 0.0;
        double deadZone = 0.012;
        if (Math.abs(error) > deadZone) {
            double amount = Math.min(0.22, (Math.abs(error) - deadZone) * 1.35 * BridgeConfig.strength);
            correction = -Math.copySign(amount, error);
        }

        double correctionX = sideX * correction;
        double correctionZ = sideZ * correction;
        double adjustedX = original.x + correctionX * leftX + correctionZ * leftZ;
        double adjustedY = original.y + correctionX * forwardX + correctionZ * forwardZ;

        if (BridgeConfig.edgeGuard && !hasSupportAt(client, player,
                player.getX() + laneX * 0.40, player.getZ() + laneZ * 0.40)) {
            // Do not walk farther into unsupported space. Sideways correction remains available.
            double worldX = leftX * adjustedX + forwardX * adjustedY;
            double worldZ = leftZ * adjustedX + forwardZ * adjustedY;
            double along = worldX * laneX + worldZ * laneZ;
            if (along > 0.0) {
                worldX -= laneX * along;
                worldZ -= laneZ * along;
                adjustedX = worldX * leftX + worldZ * leftZ;
                adjustedY = worldX * forwardX + worldZ * forwardZ;
            }
        }

        // The assist may redirect input, but never increases its original magnitude.
        double oldLength = Math.hypot(original.x, original.y);
        double newLength = Math.hypot(adjustedX, adjustedY);
        if (newLength > oldLength && newLength > 1.0E-4) {
            double scale = oldLength / newLength;
            adjustedX *= scale;
            adjustedY *= scale;
        }

        return new Vec2f((float) adjustedX, (float) adjustedY);
    }

    private static BlockPos findNearestSupport(MinecraftClient client, ClientPlayerEntity player,
                                                 double directionX, double directionZ) {
        int supportY = BlockPos.ofFloored(player.getX(), player.getY() - 0.03, player.getZ()).getY();
        BlockPos origin = BlockPos.ofFloored(player.getX(), supportY, player.getZ());
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos candidate = origin.add(dx, 0, dz);
                if (!isSolidTop(client, candidate)) continue;

                double deltaX = player.getX() - (candidate.getX() + 0.5);
                double deltaZ = player.getZ() - (candidate.getZ() + 0.5);
                double across = deltaX * -directionZ + deltaZ * directionX;
                double along = deltaX * directionX + deltaZ * directionZ;
                double score = across * across * 5.0 + along * along;
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static boolean hasSupportAt(MinecraftClient client, ClientPlayerEntity player, double x, double z) {
        int supportY = BlockPos.ofFloored(player.getX(), player.getY() - 0.03, player.getZ()).getY();
        return isSolidTop(client, BlockPos.ofFloored(x, supportY, z));
    }

    private static boolean isSolidTop(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return state.isSideSolidFullSquare(client.world, pos, Direction.UP);
    }

    public static void resetLane() {
        laneLocked = false;
        laneX = 0.0;
        laneZ = 0.0;
        inactiveTicks = 0;
    }
}
