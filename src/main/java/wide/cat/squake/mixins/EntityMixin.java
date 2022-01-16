package wide.cat.squake.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wide.cat.squake.SquakeClientPlayer;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    private void onUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (SquakeClientPlayer.moveRelativeBase((Entity) (Object) this, (float) movementInput.x, (float) movementInput.y, (float) movementInput.z, speed))
            ci.cancel();
    }
}
