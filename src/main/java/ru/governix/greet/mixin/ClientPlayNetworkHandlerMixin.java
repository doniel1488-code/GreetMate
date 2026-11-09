package ru.governix.greet.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.governix.greet.client.WelcomeTracker;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void greetmate$onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        WelcomeTracker.handlePlayerList(
                MinecraftClient.getInstance(),
                (ClientPlayNetworkHandler) (Object) this,
                packet
        );
    }

    @Inject(method = "onPlayerRemove", at = @At("TAIL"))
    private void greetmate$onPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        WelcomeTracker.handlePlayerRemove(packet.profileIds());
    }
}
