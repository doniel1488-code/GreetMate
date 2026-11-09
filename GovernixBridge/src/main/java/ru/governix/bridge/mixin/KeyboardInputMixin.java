package ru.governix.bridge.mixin;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.governix.bridge.BridgeController;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void governixbridge$adjustMovement(CallbackInfo ci) {
        InputAccessor accessor = (InputAccessor) this;
        accessor.governixbridge$setMovementVector(
                BridgeController.adjust(accessor.governixbridge$getMovementVector()));
    }
}
