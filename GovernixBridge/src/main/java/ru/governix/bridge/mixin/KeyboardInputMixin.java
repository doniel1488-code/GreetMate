package ru.governix.bridge.mixin;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.governix.bridge.BridgeController;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Shadow
    protected Vec2f movementVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void governixbridge$adjustMovement(CallbackInfo ci) {
        movementVector = BridgeController.adjust(movementVector);
    }
}
