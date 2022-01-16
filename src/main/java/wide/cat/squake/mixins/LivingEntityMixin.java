package wide.cat.squake.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wide.cat.squake.config.ConfigHandler;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "handleFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public float onHandleFall(float fallDistance) {
        if (!(((LivingEntity) (Object) this) instanceof PlayerEntity)) return fallDistance;

        if (ConfigHandler.getConfigValues().increasedFallDistance != 0.0F) {
            return (fallDistance - ConfigHandler.getConfigValues().increasedFallDistance);
        }

        return fallDistance;
    }
}
