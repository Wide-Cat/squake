package wide.cat.squake;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
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

    public static boolean moveEntityWithHeading(PlayerEntity player, float sideMove, float upMove, float forwardMove) {
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
            didQuakeMovement = quake_moveEntityWithHeading(player, sideMove, upMove, forwardMove);

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

    public static boolean moveRelativeBase(Entity entity, float sideMove, float upMove, float forwardMove, float friction) {
        if (!(entity instanceof PlayerEntity))
            return false;

        return moveRelative((PlayerEntity) entity, sideMove, upMove, forwardMove, friction);
    }

    public static boolean moveRelative(PlayerEntity player, float sideMove, float upMove, float forwardMove, float friction) {
        if (!player.world.isClient())
            return false;

        if (!configValues.enabled)
            return false;

        if ((player.getAbilities().flying && player.getVehicle() == null) || player.isTouchingWater() || player.isInLava() || player.isClimbing()) {
            return false;
        }

        // this is probably wrong, but it's what was there in 1.10.2
        float wishspeed = friction;
        wishspeed *= 2.15f;
        float[] wishDir = getMovementDirection(player, sideMove, forwardMove);
        float[] wishVel = new float[]{
                wishDir[0] * wishspeed,
                wishDir[1] * wishspeed
        };
        baseVelocities.add(wishVel);

        return true;
    }

    public static void afterJump(PlayerEntity player) {
        if (!player.world.isClient())
            return;

        if (!configValues.enabled)
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

        if (player.isOnGround()) {
            BlockPos groundPos = new BlockPos(MathHelper.floor(player.getX()), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(player.getZ()));
            f2 = 1.0F - Motions.getSlipperiness(player, groundPos);
        }

        return f2;
    }

    private static float getSlipperiness(PlayerEntity player) {
        float f2 = 0.91F;
        if (player.isOnGround()) {
            BlockPos groundPos = new BlockPos(MathHelper.floor(player.getX()), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(player.getZ()));
            f2 = Motions.getSlipperiness(player, groundPos) * 0.91F;
        }
        return f2;
    }

    private static float minecraft_getMoveSpeed(PlayerEntity player) {
        float f2 = getSlipperiness(player);

        float f3 = 0.16277136F / (f2 * f2 * f2);

        return player.getMovementSpeed() * f3;
    }

    private static float[] getMovementDirection(PlayerEntity player, float sideMove, float forwardMove) {
        float f3 = sideMove * sideMove + forwardMove * forwardMove;
        float[] dir = {0.0F, 0.0F};

        if (f3 >= 1.0E-4F) {
            f3 = MathHelper.sqrt(f3);

            if (f3 < 1.0F) {
                f3 = 1.0F;
            }

            f3 = 1.0F / f3;
            sideMove *= f3;
            forwardMove *= f3;
            float f4 = MathHelper.sin(player.getYaw() * (float) Math.PI / 180.0F);
            float f5 = MathHelper.cos(player.getYaw() * (float) Math.PI / 180.0F);
            dir[0] = (sideMove * f5 - forwardMove * f4);
            dir[1] = (forwardMove * f5 + sideMove * f4);
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

        if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
            for (int iParticle = 0; iParticle < numParticles; iParticle++) {
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
            if (player.getY() > player.world.getBottomY()) {
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
        if (f6 > 1.0F) f6 = 1.0F;
        player.limbDistance += (f6 - player.limbDistance) * 0.4F;
        player.limbAngle += player.limbDistance;
    }

    private static void minecraft_WaterMove(PlayerEntity player, float sideMove, float upMove, float forwardMove) {
        double d0 = player.getY();
        player.updateVelocity(0.04F, new Vec3d(sideMove, upMove, forwardMove));

        double motionX = Motions.getMotionX(player), motionY = Motions.getMotionY(player), motionZ = Motions.getMotionZ(player);

        player.move(MovementType.SELF, player.getVelocity());

        motionX *= 0.800000011920929D;
        motionY *= 0.800000011920929D;
        motionZ *= 0.800000011920929D;
        motionY -= 0.02D;

        player.setVelocity(motionX, motionY, motionZ);

        if (player.horizontalCollision && player.doesNotCollide(Motions.getMotionX(player), Motions.getMotionY(player) + 0.6000000238418579D - player.getY() + d0, Motions.getMotionZ(player))) {
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
    public static boolean quake_moveEntityWithHeading(PlayerEntity player, float sideMove, float upMove, float forwardMove) {
        // take care of ladder movement using default code
        if (player.isClimbing()) {
            return false;
        }
        // take care of lava movement using default code
        else if (player.isInLava() && !player.getAbilities().flying) {
            return false;
        } else if (player.isTouchingWater() && !player.getAbilities().flying) {
            if (configValues.sharkingEnabled) {
                quake_WaterMove(player, sideMove, upMove, forwardMove);
            }
            else return false;

        } else {
            // get all relevant movement values
            float wishSpeed = (sideMove != 0.0F || forwardMove != 0.0F) ? quake_getMoveSpeed(player) : 0.0F;
            float[] wishDir = getMovementDirection(player, sideMove, forwardMove);
            boolean isOnGroundForReal = player.isOnGround() && !isJumping(player);
            float momentumRetention = getSlipperiness(player);

            //info("wishSpeed: " + wishSpeed + ", wishDir: " + Arrays.toString(wishDir) + ", onground: " + isOnGroundForReal + ", momentumretention: " + momentumRetention);

            // ground movement
            if (isOnGroundForReal) {
                // apply friction before acceleration, so we can accelerate back up to maxspeed afterwards
                //quake_Friction(); // buggy because material-based friction uses a totally different format
                minecraft_ApplyFriction(player, momentumRetention);

                double sv_accelerate = configValues.accelerate;

                if (wishSpeed != 0.0F) {
                    // alter based on the surface friction
                    sv_accelerate *= minecraft_getMoveSpeed(player) * 2.15F / wishSpeed;
                    //info("sv: " + sv_accelerate);

                    quake_Accelerate(player, wishSpeed, wishDir[0], wishDir[1], sv_accelerate);
                }

                //info("basevelocities: " + baseVelocities);
                if (!baseVelocities.isEmpty()) {
                    float speedMod = wishSpeed / quake_getMaxMoveSpeed(player);

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
                double sv_airAccelerate = configValues.airAccelerate;
                quake_AirAccelerate(player, wishSpeed, wishDir[0], wishDir[1], sv_airAccelerate);

                if (configValues.sharkingEnabled && configValues.sharkingSurfTension > 0.0D && isJumping(player) && Motions.getMotionY(player) < 0.0F) {
                    Box offsetBox = player.getBoundingBox().offset(player.getVelocity());
                    boolean isFallingIntoWater = player.world.containsFluid(offsetBox);

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

        if (!didTrimp) {
            quake_ApplyHardCap(player, quake_getMaxMoveSpeed(player));
        }
    }

    private static boolean quake_DoTrimp(PlayerEntity player) {
        if (configValues.trimpingEnabled && player.isSneaking()) {

            double curSpeed = getSpeed(player);
            float moveSpeed = quake_getMaxMoveSpeed(player);

            if (curSpeed > moveSpeed) {
                double speedbonus = curSpeed / moveSpeed * 0.5F;
                if (speedbonus > 1.0F)
                    speedbonus = 1.0F;

                Motions.setMotionY(player, Motions.getMotionY(player) + speedbonus * curSpeed * configValues.trimpMult);

                if (configValues.trimpMult > 0) {
                    float multiplier = (float) (1.0f / configValues.trimpMult);
                    double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
                    motionX *= multiplier;
                    motionZ *= multiplier;
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
    private static void quake_WaterAccelerate(PlayerEntity player, float wishSpeed, float speed, double wishX, double wishZ, double accel) {
        float addSpeed = wishSpeed - speed;
        if (addSpeed > 0) {
            float accelspeed = (float) (accel * wishSpeed * 0.05F);
            if (accelspeed > addSpeed) {
                accelspeed = addSpeed;
            }
            double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
            motionX += accelspeed * wishX;
            motionZ += accelspeed * wishZ;
            Motions.setMotionHoriz(player, motionX, motionZ);
        }
    }

    private static void quake_WaterMove(PlayerEntity player, float sideMove, float upMove, float forwardMove) {
        double lastPosY = player.getY();

        // get all relevant movement values
        float wishSpeed = (sideMove != 0.0F || forwardMove != 0.0F) ? quake_getMaxMoveSpeed(player) : 0.0F;
        float[] wishDir = getMovementDirection(player, sideMove, forwardMove);
        boolean isSharking = isJumping(player) && player.doesNotCollide(0, 1, 0); // doesNotCollide == isOffsetPositionInLiquid
        double curSpeed = getSpeed(player);

        if (!isSharking || curSpeed < 0.078F || player.isSwimming()) {
            minecraft_WaterMove(player, sideMove, upMove, forwardMove);
        } else {
            if (curSpeed > 0.09) {
                quake_ApplyWaterFriction(player, configValues.sharkingWaterFriction);
            }

            if (curSpeed > 0.098) {
                quake_AirAccelerate(player, wishSpeed, wishDir[0], wishDir[1], configValues.accelerate);
            }
            else {
                quake_Accelerate(player, .0980F, wishDir[0], wishDir[1], configValues.accelerate);
            }

            player.move(MovementType.SELF, player.getVelocity());

            Motions.setMotionY(player, 0);
        }

        // jumping out of water
        if (player.horizontalCollision && player.doesNotCollide(player.getVelocity().x, player.getVelocity().y + 0.6000000238418579D - player.getY() + lastPosY, player.getVelocity().z)) {
            Motions.setMotionY(player, 0.30000001192092896D);
        }

        if (!baseVelocities.isEmpty()) {
            float speedMod = wishSpeed / quake_getMaxMoveSpeed(player);
            // add in base velocities

            for (float[] baseVel : baseVelocities) {
                double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);

                motionX += baseVel[0] * speedMod;
                motionZ += baseVel[1] * speedMod;

                Motions.setMotionHoriz(player, motionX, motionZ);
            }
        }
    }

    private static void quake_Accelerate(PlayerEntity player, float wishSpeed, double wishX, double wishZ, double accel) {
        double addSpeed, accelSpeed, currentSpeed;

        // Determine veer amount
        // this is a dot product
        currentSpeed = Motions.getMotionX(player) * wishX + Motions.getMotionZ(player) * wishZ;
        //info("current speed: " + currentSpeed);

        // See how much to add
        addSpeed = wishSpeed - currentSpeed;

        // If not adding any, done.
        if (addSpeed <= 0)
            return;

        // Determine acceleration speed after acceleration
        accelSpeed = accel * wishSpeed / getSlipperiness(player) * 0.05F;
        //info("accelSpeed: " + accelSpeed);

        // Cap it
        if (accelSpeed > addSpeed)
            accelSpeed = addSpeed;

        // Adjust pmove vel.
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        motionX += accelSpeed * wishX;
        motionZ += accelSpeed * wishZ;
        //info("x: " + motionX + ", z: " + motionZ);
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    private static void quake_AirAccelerate(PlayerEntity player, float wishSpeed, double wishX, double wishZ, double accel) {
        double addSpeed, accelspeed, currentSpeed;

        float wishSpd = wishSpeed;
        float maxAirAcceleration = configValues.maxAirAccelerationPerTick;

        if (wishSpd > maxAirAcceleration)
            wishSpd = maxAirAcceleration;

        // Determine veer amount
        // this is a dot product
        currentSpeed = Motions.getMotionX(player) * wishX + Motions.getMotionZ(player) * wishZ;

        // See how much to add
        addSpeed = wishSpd - currentSpeed;

        // If not adding any, done.
        if (addSpeed <= 0)
            return;

        // Determine acceleration speed after acceleration
        accelspeed = accel * wishSpeed * 0.05F;

        // Cap it
        if (accelspeed > addSpeed)
            accelspeed = addSpeed;

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
        if (speed <= 0.0F)
            return;


        drop = 0.0F;

        // convars
        float sv_friction = 1.0F;
        float sv_stopSpeed = 0.005F;

        float surfaceFriction = getSurfaceFriction(player);
        friction = sv_friction * surfaceFriction;

        // Bleed off some speed, but if we have less than the bleed
        //  threshold, bleed the threshold amount.
        control = (speed < sv_stopSpeed) ? sv_stopSpeed : speed;

        // Add the amount to the drop amount.
        drop += control * friction * 0.05F;

        // scale the velocity
        newspeed = speed - drop;
        if (newspeed < 0.0F)
            newspeed = 0.0F;
        double motionX = Motions.getMotionX(player), motionZ = Motions.getMotionZ(player);
        if (newspeed != speed) {
            // Determine proportion of old speed we are using.
            newspeed /= speed;
            // Adjust velocity according to proportion.
            motionX *= newspeed;
            motionZ *= newspeed;
        }
        Motions.setMotionHoriz(player, motionX, motionZ);
    }

    private static void quake_ApplySoftCap(PlayerEntity player, float moveSpeed) {
        float softCapPercent = configValues.softCap;
        float softCapDegen = configValues.softCapDegen;

        if (configValues.uncappedBunnyhopEnabled) {
            softCapPercent = 1.0F;
            softCapDegen = 1.0F;
        }

        float speed = (float) (getSpeed(player));
        float softCap = moveSpeed * softCapPercent;

        // apply soft cap first; if soft -> hard is not done, then you can continually trigger only the hard cap and stay at the hard cap
        if (speed > softCap) {
            if (softCapDegen != 1.0F) {
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

    private static void quake_ApplyHardCap(PlayerEntity player, float moveSpeed) {
        if (configValues.uncappedBunnyhopEnabled)
            return;

        float hardCapPercent = configValues.hardCap;

        float speed = (float) (getSpeed(player));
        float hardCap = moveSpeed * hardCapPercent;

        if (speed > hardCap && hardCap != 0.0F) {
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
}
