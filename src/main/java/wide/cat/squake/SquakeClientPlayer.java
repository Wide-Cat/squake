package wide.cat.squake;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.ChunkStatus;
import wide.cat.squake.config.ConfigHandler;
import wide.cat.squake.config.ConfigValues;
import wide.cat.squake.mixins.LivingEntityAccessor;

import java.util.ArrayList;
import java.util.List;

public class SquakeClientPlayer {

    private static final List<float[]> baseVelocities = new ArrayList<>();
    private static final ConfigValues configValues = ConfigHandler.getConfigValues();

    public static boolean moveEntityWithHeading(PlayerEntity player, float sidemove, float upmove, float forwardmove) {
        if (!player.world.isClient())
            return false;

        if (!configValues.enabled)
            return false;

        boolean didQuakeMovement;
        double d0 = player.getX();
        double d1 = player.getY();
        double d2 = player.getZ();

        if ((player.getAbilities().flying || player.isFallFlying()) && player.getVehicle() == null)
            return false;
        else
            didQuakeMovement = quake_moveEntityWithHeading(player, sidemove, upmove, forwardmove);

        if (didQuakeMovement)
            player.increaseTravelMotionStats(player.getX() - d0, player.getY() - d1, player.getZ() - d2);

        return didQuakeMovement;
    }

    public static void beforeOnLivingUpdate(PlayerEntity player) {
        if (!player.world.isClient())
            return;

        if (!baseVelocities.isEmpty()) {
            baseVelocities.clear();
        }
    }

    public static boolean moveRelativeBase(Entity entity, float sidemove, float upmove, float forwardmove, float friction) {
        if(!(entity instanceof PlayerEntity))
            return false;

        return moveRelative((PlayerEntity) entity, sidemove, upmove, forwardmove, friction);
    }

    public static boolean moveRelative(PlayerEntity player, float sidemove, float upmove, float forwardmove, float friction) {
        if(!player.world.isClient())
            return false;

        if(!configValues.enabled)
            return false;

        if((player.getAbilities().flying && player.getVehicle() == null) || player.isTouchingWater() || player.isInLava() || player.isClimbing()) {
            return false;
        }

        // this is probably wrong, but it's what was there in 1.10.2
        float wishspeed = friction;
        wishspeed *= 2.15f;
        float[] wishdir = getMovementDirection(player, sidemove, forwardmove);
        float[] wishvel = new float[]{
                wishdir[0] * wishspeed,
                wishdir[1] * wishspeed
        };
        baseVelocities.add(wishvel);

        return true;
    }

    public static void afterJump(PlayerEntity player) {
        if(!player.world.isClient())
            return;

        if(!configValues.enabled)
            return;

        // undo this dumb thing
        if (player.isSprinting()) {
            float f = player.getYaw() * 0.017453292F;

            double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);

            motionX += MathHelper.sin(f) * 0.2F;
            motionZ -= MathHelper.cos(f) * 0.2F;

            Motions.setMotionHoriz(player, motionX, motionZ);
        }

        quake_Jump(player);
    }

    /* =================================================
     * START HELPERS
     * =================================================
     */

    private static double getSpeed(PlayerEntity player) {
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        return MathHelper.sqrt((float) (motionX * motionX + motionZ * motionZ));
    }

    private static float getSurfaceFriction(PlayerEntity player) {
        float f2 = 1.0F;

        if(player.isOnGround()) {
            BlockPos groundPos = new BlockPos(MathHelper.floor(player.getX()), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(player.getZ()));
            f2 = 1.0F - Motions.getSlipperiness(player, groundPos);
        }

        return f2;
    }

    private static float getSlipperiness(PlayerEntity player) {
        float f2 = 0.91F;
        if(player.isOnGround()) {
            BlockPos groundPos = new BlockPos(MathHelper.floor(player.getX()), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(player.getZ()));
            f2 = Motions.getSlipperiness(player, groundPos) * 0.91F;
        }
        return f2;
    }

    private static float minecraft_getMoveSpeed(PlayerEntity player) {
        float f2 = getSlipperiness(player);

        float f3 = 0.16277136F / (f2 * f2 * f2);

        return player.speed * f3;
    }

    private static float[] getMovementDirection(PlayerEntity player, float sidemove, float forwardmove) {
        float f3 = sidemove * sidemove + forwardmove * forwardmove;
        float[] dir = {
                0.0F,
                0.0F
        };

        if(f3 >= 1.0E-4F) {
            f3 = MathHelper.sqrt(f3);

            if(f3 < 1.0F) {
                f3 = 1.0F;
            }

            f3 = 1.0F / f3;
            sidemove *= f3;
            forwardmove *= f3;
            float f4 = MathHelper.sin(player.getYaw() * (float) Math.PI / 180.0F);
            float f5 = MathHelper.cos(player.getYaw() * (float) Math.PI / 180.0F);
            dir[0] = (sidemove * f5 - forwardmove * f4);
            dir[1] = (forwardmove * f5 + sidemove * f4);
        }

        return dir;
    }

    private static float quake_getMoveSpeed(PlayerEntity player) {
        float baseSpeed = player.getMovementSpeed();
        return !player.isSneaking() ? baseSpeed * 2.15F : baseSpeed * 1.11F;
    }

    private static float quake_getMaxMoveSpeed(PlayerEntity player) {
        float baseSpeed = player.getMovementSpeed();
        return baseSpeed * 2.15F;
    }

    private static void spawnBunnyhopParticles(PlayerEntity player, int numParticles) {
        // taken from sprint
        int j = MathHelper.floor(player.getX());
        int i = MathHelper.floor(player.getY() - 0.20000000298023224D - player.getMountedHeightOffset());
        int k = MathHelper.floor(player.getZ());
        BlockState blockState = player.world.getBlockState(new BlockPos(j, i, k));

        Vec3d motion = player.getVelocity();
        Random random = player.getRandom();

        if(blockState.getRenderType() != BlockRenderType.INVISIBLE) {
            for(int iParticle = 0; iParticle < numParticles; iParticle++) {
                player.world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), player.getX() + (random.nextFloat() - 0.5D) * player.getWidth(), player.getBoundingBox().minY + 0.1D, player.getZ() + (random.nextFloat() - 0.5D) * player.getWidth(), -motion.x * 4.0D, 1.5D, -motion.z * 4.0D);
            }
        }
    }

    private static boolean isJumping(PlayerEntity player) {
        return ((LivingEntityAccessor)player).getJumping();
    }

    /* =================================================
     * END HELPERS
     * =================================================
     */

    /* =================================================
     * START MINECRAFT PHYSICS
     * =================================================
     */

    private static void minecraft_ApplyGravity(PlayerEntity player) {
        double motionY = Motions.getMotionY(player);
        BlockPos blockPos = new BlockPos(player.getPos().x, player.getPos().y - 0.5000001D, player.getPos().z);

        if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
            motionY += (0.05D * (double)(player.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - motionY) * 0.2D;
        } else if (player.world.isClient && (!player.world.isChunkLoaded(blockPos) || player.world.getChunk(blockPos).getStatus() != ChunkStatus.FULL)) {
            if(player.getY() > player.world.getBottomY()) {
                motionY = -0.1D;
            } else {
                motionY = 0.0D;
            }
        } else if (!player.hasNoGravity()) {
            // gravity
            motionY -= 0.08D;
        }

        // air resistance
        if (!player.hasNoDrag()) motionY *= 0.9800000190734863D;

        Motions.setMotionY(player, motionY);
    }

    private static void minecraft_ApplyFriction(PlayerEntity player, float momentumRetention) {
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        motionX *= momentumRetention;
        motionZ *= momentumRetention;
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    private static void minecraft_SwingLimbsBasedOnMovement(PlayerEntity player) {
        player.lastLimbDistance = player.limbDistance;
        double d0 = player.getX() - player.prevX;
        double d1 = player.getZ() - player.prevZ;
        float f6 = MathHelper.sqrt((float) (d0 * d0 + d1 * d1)) * 4.0F;
        if(f6 > 1.0F) f6 = 1.0F;
        player.limbDistance += (f6 - player.limbDistance) * 0.4F;
        player.limbAngle += player.limbDistance;
    }

    private static void minecraft_WaterMove(PlayerEntity player, float sidemove, float upmove, float forwardmove) {
        double d0 = player.getY();
        player.updateVelocity(0.04F, new Vec3d(sidemove, upmove, forwardmove));

        double motionX = Motions.getMotionX(player), motionY = Motions.getMotionY(player), motionZ = Motions.getMotionZ(player);

        player.move(MovementType.PLAYER, player.getVelocity());

        motionX *= 0.800000011920929D;
        motionY *= 0.800000011920929D;
        motionZ *= 0.800000011920929D;
        motionY -= 0.02D;

        player.setVelocity(motionX, motionY, motionZ);

        if(player.horizontalCollision && Motions.isOffsetPositionInLiquid(player, Motions.getMotionX(player), Motions.getMotionY(player) + 0.6000000238418579D - player.getY() + d0, Motions.getMotionZ(player))) {
            Motions.setMotionY(player, 0.30000001192092896D);
        }
    }

    /* =================================================
     * END MINECRAFT PHYSICS
     * =================================================
     */

    /* =================================================
     * START QUAKE PHYSICS
     * =================================================
     */

    /**
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
    public static boolean quake_moveEntityWithHeading(PlayerEntity player, float sidemove, float upmove, float forwardmove) {
        // take care of ladder movement using default code
        if (player.isClimbing()) {
            return false;
        }
        // take care of lava movement using default code
        else if (player.isInLava() && !player.getAbilities().flying) {
            return false;
        } else if (player.isTouchingWater() && !player.getAbilities().flying) {
            if (configValues.sharkingEnabled) {
                quake_WaterMove(player, sidemove, upmove, forwardmove);
            }
            else {
                return false;
            }
        } else {
            // get all relevant movement values
            float wishspeed = (sidemove != 0.0F || forwardmove != 0.0F) ? quake_getMoveSpeed(player) : 0.0F;
            float[] wishdir = getMovementDirection(player, sidemove, forwardmove);
            boolean isOnGroundForReal = player.isOnGround() && !isJumping(player);
            float momentumRetention = getSlipperiness(player);

            // ground movement
            if (isOnGroundForReal) {
                // apply friction before acceleration so we can accelerate back up to maxspeed afterwards
                //quake_Friction(); // buggy because material-based friction uses a totally different format
                minecraft_ApplyFriction(player, momentumRetention);

                double sv_accelerate = configValues.accelerate;

                if (wishspeed != 0.0F) {
                    // alter based on the surface friction
                    sv_accelerate *= minecraft_getMoveSpeed(player) * 2.15F / wishspeed;

                    quake_Accelerate(player, wishspeed, wishdir[0], wishdir[1], sv_accelerate);
                }

                if (!baseVelocities.isEmpty()) {
                    float speedMod = wishspeed / quake_getMaxMoveSpeed(player);

                    double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);

                    // add in base velocities
                    for (float[] baseVel : baseVelocities) {
                        motionX += baseVel[0] * speedMod;
                        motionZ += baseVel[1] * speedMod;
                    }

                    Motions.setMotionHoriz(player, motionX, motionZ);
                }
            }
            // air movement
            else {
                double sv_airaccelerate = configValues.airAccelerate;
                quake_AirAccelerate(player, wishspeed, wishdir[0], wishdir[1], sv_airaccelerate);

                if (configValues.sharkingEnabled && configValues.sharkingSurfTension > 0.0D && isJumping(player) && Motions.getMotionY(player) < 0.0F) {
                    Box axisalignedbb = player.getBoundingBox().offset(player.getVelocity());
                    boolean isFallingIntoWater = player.world.containsFluid(axisalignedbb);

                    if (isFallingIntoWater)
                        Motions.setMotionY(player, Motions.getMotionY(player) * configValues.sharkingSurfTension);
                }
            }

            // apply velocity
            player.move(MovementType.SELF, player.getVelocity());

            // HL2 code applies half gravity before acceleration and half after acceleration, but this seems to work fine
            minecraft_ApplyGravity(player);
        }

        // swing them arms
        minecraft_SwingLimbsBasedOnMovement(player);

        return true;
    }

    private static void quake_Jump(PlayerEntity player) {
        quake_ApplySoftCap(player, quake_getMaxMoveSpeed(player));

        boolean didTrimp = quake_DoTrimp(player);

        if(!didTrimp) {
            quake_ApplyHardCap(player, quake_getMaxMoveSpeed(player));
        }
    }

    private static boolean quake_DoTrimp(PlayerEntity player) {
        if(configValues.trimpingEnabled && player.isSneaking()) {

            double curspeed = getSpeed(player);
            float movespeed = quake_getMaxMoveSpeed(player);

            if(curspeed > movespeed) {
                double speedbonus = curspeed / movespeed * 0.5F;
                if(speedbonus > 1.0F)
                    speedbonus = 1.0F;

                Motions.setMotionY(player, Motions.getMotionY(player) + speedbonus * curspeed * configValues.trimpMult);

                if(configValues.trimpMult > 0) {
                    float mult = (float) (1.0f / configValues.trimpMult);
                    double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
                    motionX *= mult;
                    motionZ *= mult;
                    Motions.setMotionHoriz(player, motionX, motionZ);
                }

                spawnBunnyhopParticles(player, 30);

                return true;
            }
        }

        return false;
    }

    private static void quake_ApplyWaterFriction(PlayerEntity player, double friction) {
        player.setVelocity(player.getVelocity().multiply(friction));
    }

    @SuppressWarnings("unused")
    private static void quake_WaterAccelerate(PlayerEntity player, float wishspeed, float speed, double wishX, double wishZ, double accel) {
        float addspeed = wishspeed - speed;
        if(addspeed > 0) {
            float accelspeed = (float) (accel * wishspeed * 0.05F);
            if(accelspeed > addspeed) {
                accelspeed = addspeed;
            }
            double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
            motionX += accelspeed * wishX;
            motionZ += accelspeed * wishZ;
            Motions.setMotionHoriz(player, motionX, motionZ);
        }
    }

    private static void quake_WaterMove(PlayerEntity player, float sidemove, float upmove, float forwardmove) {
        double lastPosY = player.getY();

        // get all relevant movement values
        float wishspeed = (sidemove != 0.0F || forwardmove != 0.0F) ? quake_getMaxMoveSpeed(player) : 0.0F;
        float[] wishdir = getMovementDirection(player, sidemove, forwardmove);
        boolean isSharking = isJumping(player) && player.doesNotCollide(0, 1, 0);
        info("sharking: " + isSharking);
        double curspeed = getSpeed(player);

        if (!isSharking || curspeed < 0.078F || player.isSwimming()) {
            minecraft_WaterMove(player, sidemove, upmove, forwardmove);
            info("minecraft water moving");
        } else {
            if (curspeed > 0.09) {
                quake_ApplyWaterFriction(player, configValues.sharkingWaterFriction);
                info("applying friction");
            }

            if (curspeed > 0.098) {
                quake_AirAccelerate(player, wishspeed, wishdir[0], wishdir[1], configValues.accelerate);
                info("aa wishspeed");
            }
            else {
                quake_Accelerate(player, .0980F, wishdir[0], wishdir[1], configValues.accelerate);
                info("aa .098");
            }

            player.move(MovementType.PLAYER, player.getVelocity());

            Motions.setMotionY(player, 0);
        }

        // water jump
        if (player.horizontalCollision && player.doesNotCollide(player.getVelocity().x, player.getVelocity().y + 0.6000000238418579D - player.getY() + lastPosY, player.getVelocity().z)) {
            Motions.setMotionY(player, 0.30000001192092896D);
            info("set motion y");
            //info(String.valueOf(player.doesNotCollide(player.getVelocity().x, player.getVelocity().y + 0.6000000238418579D - player.getY() + lastPosY, player.getVelocity().z)));
        }

        if (!baseVelocities.isEmpty()) {
            float speedMod = wishspeed / quake_getMaxMoveSpeed(player);
            // add in base velocities

            double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
            for (float[] baseVel : baseVelocities) {
                motionX += baseVel[0] * speedMod;
                motionZ += baseVel[1] * speedMod;

                Motions.setMotionHoriz(player, motionX, motionZ);
            }
        }
    }

    private static void quake_Accelerate(PlayerEntity player, float wishspeed, double wishX, double wishZ, double accel) {
        double addspeed, accelspeed, currentspeed;

        // Determine veer amount
        // this is a dot product
        currentspeed = Motions.getMotionX(player) * wishX + Motions.getMotionZ(player) * wishZ;

        // See how much to add
        addspeed = wishspeed - currentspeed;

        // If not adding any, done.
        if(addspeed <= 0)
            return;

        // Determine acceleration speed after acceleration
        accelspeed = accel * wishspeed / getSlipperiness(player) * 0.05F;

        // Cap it
        if(accelspeed > addspeed)
            accelspeed = addspeed;

        // Adjust pmove vel.
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        motionX += accelspeed * wishX;
        motionZ += accelspeed * wishZ;
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    private static void quake_AirAccelerate(PlayerEntity player, float wishspeed, double wishX, double wishZ, double accel) {
        double addspeed, accelspeed, currentspeed;

        float wishspd = wishspeed;
        float maxAirAcceleration = configValues.maxAirAccelerationPerTick;

        if(wishspd > maxAirAcceleration)
            wishspd = maxAirAcceleration;

        // Determine veer amount
        // this is a dot product
        currentspeed = Motions.getMotionX(player) * wishX + Motions.getMotionZ(player) * wishZ;

        // See how much to add
        addspeed = wishspd - currentspeed;

        // If not adding any, done.
        if(addspeed <= 0)
            return;

        // Determine acceleration speed after acceleration
        accelspeed = accel * wishspeed * 0.05F;

        // Cap it
        if(accelspeed > addspeed)
            accelspeed = addspeed;

        // Adjust pmove vel.
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        motionX += accelspeed * wishX;
        motionZ += accelspeed * wishZ;
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    @SuppressWarnings("unused")
    private static void quake_Friction(PlayerEntity player) {
        double speed, newspeed, control;
        float friction;
        float drop;

        // Calculate speed
        speed = getSpeed(player);

        // If too slow, return
        if(speed <= 0.0F)
            return;


        drop = 0.0F;

        // convars
        float sv_friction = 1.0F;
        float sv_stopspeed = 0.005F;

        float surfaceFriction = getSurfaceFriction(player);
        friction = sv_friction * surfaceFriction;

        // Bleed off some speed, but if we have less than the bleed
        //  threshold, bleed the threshold amount.
        control = (speed < sv_stopspeed) ? sv_stopspeed : speed;

        // Add the amount to the drop amount.
        drop += control * friction * 0.05F;

        // scale the velocity
        newspeed = speed - drop;
        if(newspeed < 0.0F)
            newspeed = 0.0F;
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        if(newspeed != speed) {
            // Determine proportion of old speed we are using.
            newspeed /= speed;
            // Adjust velocity according to proportion.
            motionX *= newspeed;
            motionZ *= newspeed;
        }
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    private static void quake_ApplySoftCap(PlayerEntity player, float movespeed) {
        float softCapPercent = configValues.softCap;
        float softCapDegen = configValues.softCapDegen;

        if(configValues.uncappedBunnyhopEnabled) {
            softCapPercent = 1.0F;
            softCapDegen = 1.0F;
        }

        float speed = (float) (getSpeed(player));
        float softCap = movespeed * softCapPercent;

        // apply soft cap first; if soft -> hard is not done, then you can continually trigger only the hard cap and stay at the hard cap
        if(speed > softCap) {
            if(softCapDegen != 1.0F) {
                float applied_cap = (speed - softCap) * softCapDegen + softCap;
                float multi = applied_cap / speed;
                double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
                motionX *= multi;
                motionZ *= multi;
                Motions.setMotionHoriz(player, motionX, motionZ);
            }

            spawnBunnyhopParticles(player, 10);
        }
    }

    private static void quake_ApplyHardCap(PlayerEntity player, float movespeed) {
        if(configValues.uncappedBunnyhopEnabled)
            return;

        float hardCapPercent = configValues.hardCap;

        float speed = (float) (getSpeed(player));
        float hardCap = movespeed * hardCapPercent;

        if(speed > hardCap && hardCap != 0.0F) {
            float multi = hardCap / speed;

            double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
            motionX *= multi;
            motionZ *= multi;
            Motions.setMotionHoriz(player, motionX, motionZ);

            spawnBunnyhopParticles(player, 30);
        }
    }

    @SuppressWarnings("unused")
    private static void quake_OnLivingUpdate()
    {
    }

    /* =================================================
     * END QUAKE PHYSICS
     * =================================================
     */

    private static void info(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
