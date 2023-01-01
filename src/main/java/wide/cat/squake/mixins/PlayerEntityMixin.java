package wide.cat.squake.mixins;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wide.cat.squake.SquakeClientPlayer;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    public PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {super(type, world);}

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void moveEntityWithHeading(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity asPlayer = (PlayerEntity) (LivingEntity) this;
        if (SquakeClientPlayer.moveEntityWithHeading(asPlayer, (float) movementInput.x, (float) movementInput.y, (float) movementInput.z))
            ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void beforeOnLivingUpdate(CallbackInfo ci) {
        PlayerEntity asPlayer = (PlayerEntity) (LivingEntity) this;
        SquakeClientPlayer.beforeOnLivingUpdate(asPlayer);
    }

    @Inject(method = "jump", at = @At("TAIL"))
    public void afterJump(CallbackInfo ci) {
        PlayerEntity asPlayer = (PlayerEntity) (LivingEntity) this;
        SquakeClientPlayer.afterJump(asPlayer);
    }

    private boolean wasVelocityChangedBeforeFall = false;

    @Inject(method = "handleFallDamage", at = @At("HEAD"))
    public void beforeFall(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) return;
        wasVelocityChangedBeforeFall = velocityDirty;
    }

    @Inject(method = "handleFallDamage", at = @At("RETURN"), slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;increaseStat(Lnet/minecraft/util/Identifier;I)V"),
            to = @At("TAIL")))
    public void afterFall(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) return;
        velocityDirty = wasVelocityChangedBeforeFall;
    }
}
