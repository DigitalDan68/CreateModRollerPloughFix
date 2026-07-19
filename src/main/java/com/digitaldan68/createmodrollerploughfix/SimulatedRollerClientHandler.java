package com.digitaldan68.createmodrollerploughfix;

import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Supplies simulated rollers with a Sable-projected, time-integrated wheel animation. */
@EventBusSubscriber(modid = CreateModRollerPloughFix.MOD_ID, value = Dist.CLIENT)
public final class SimulatedRollerClientHandler {

    private static final Map<RollerBlockEntity, RollerAnimation> ANIMATIONS = new WeakHashMap<>();
    private static final float SPEED_EASING = 0.25F;
    private static final float STOP_THRESHOLD = 0.25F;
    // Create's HarvesterRenderer advances by animatedSpeed / 20 degrees each tick.
    private static final float DEGREES_PER_TICK_PER_SPEED = 1.0F / 20.0F;

    private SimulatedRollerClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        for (ClientSubLevel subLevel : container.getAllSubLevels()) {
            for (BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
                if (!(actor instanceof RollerBlockEntity roller)) {
                    continue;
                }
                Vec3 velocity = Sable.HELPER.getVelocity(level, subLevel, roller.getBlockPos().getCenter());
                Vec3 facing = projectedFacing(level, roller);
                float targetSpeed = roller.mode != null
                    && roller.mode.getValue() == SimulatedRollerHandler.SIMULATED_PAVING_MODE
                    && velocity.dot(facing) > 0
                    ? animationSpeed(velocity.length())
                    : 0;
                updateAnimation(roller, targetSpeed);
            }
        }
    }

    /**
     * Advances a time-integrated animation phase. Create's stock renderer
     * derives the angle from render time and speed directly, which resets the
     * wheel to its default angle when the simulated contraption reverses.
     */
    private static void updateAnimation(RollerBlockEntity roller, float targetSpeed) {
        RollerAnimation animation = ANIMATIONS.get(roller);
        if (animation == null && targetSpeed == 0) {
            roller.setAnimatedSpeed(0);
            return;
        }

        if (animation == null) {
            animation = new RollerAnimation();
            ANIMATIONS.put(roller, animation);
        }

        animation.tick(targetSpeed);
        roller.setAnimatedSpeed(animation.speed);
    }

    public static boolean hasEasedAnimation(RollerBlockEntity roller) {
        return ANIMATIONS.containsKey(roller);
    }

    public static void transformEased(RollerBlockEntity roller, Direction facing, SuperByteBuffer buffer,
                                      Vec3 rotationOffset, float partialTicks) {
        RollerAnimation animation = ANIMATIONS.get(roller);
        if (animation == null) {
            return;
        }

        Vec3 pivotOffset = new Vec3(0, rotationOffset.y * 0.0625F, rotationOffset.z * 0.0625F);
        float angle = animation.angle(partialTicks);
        buffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing)), Direction.UP)
            .translate(pivotOffset)
            .rotate(AngleHelper.rad(angle), Direction.WEST)
            .translate(pivotOffset.scale(-1));
    }

    private static Vec3 projectedFacing(ClientLevel level, RollerBlockEntity roller) {
        BlockState state = roller.getBlockState();
        Direction localFacing = state.getValue(RollerBlock.FACING);
        Vec3 center = Sable.HELPER.projectOutOfSubLevel(level, roller.getBlockPos().getCenter());
        Vec3 front = Sable.HELPER.projectOutOfSubLevel(level,
            roller.getBlockPos().getCenter().add(Vec3.atLowerCornerOf(localFacing.getNormal())));
        return front.subtract(center).normalize();
    }

    private static float animationSpeed(double blocksPerSecond) {
        double blocksPerTick = blocksPerSecond / 20.0;
        if (blocksPerTick < 1.0 / 512.0) {
            return 0;
        }
        // Match MovementContext#getAnimationSpeed() for a moving Create actor.
        return -((int) (blocksPerTick * 1000.0 + 100.0) / 100) * 100.0F;
    }

    private static final class RollerAnimation {

        private float previousAngle;
        private float speed;

        private void tick(float targetSpeed) {
            previousAngle = wrapAngle(previousAngle + speed * DEGREES_PER_TICK_PER_SPEED);
            if (targetSpeed == 0) {
                speed += (targetSpeed - speed) * SPEED_EASING;
                if (Math.abs(speed) < STOP_THRESHOLD) {
                    speed = 0;
                }
            } else {
                // Preserve Create's normal full-speed forward animation.
                speed = targetSpeed;
            }
        }

        private float angle(float partialTicks) {
            return previousAngle + speed * DEGREES_PER_TICK_PER_SPEED * partialTicks;
        }

        private static float wrapAngle(float angle) {
            float wrapped = angle % 360.0F;
            return wrapped < 0 ? wrapped + 360.0F : wrapped;
        }
    }
}
