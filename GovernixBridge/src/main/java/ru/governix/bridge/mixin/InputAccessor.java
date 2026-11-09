package ru.governix.bridge.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("movementVector")
    Vec2f governixbridge$getMovementVector();

    @Accessor("movementVector")
    void governixbridge$setMovementVector(Vec2f movementVector);
}
