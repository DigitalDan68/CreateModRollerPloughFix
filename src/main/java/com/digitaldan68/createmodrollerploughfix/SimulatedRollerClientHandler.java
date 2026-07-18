package com.digitaldan68.createmodrollerploughfix;

import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Supplies Create's normal roller renderer with Sable's interpolated velocity. */
@EventBusSubscriber(modid = CreateModRollerPloughFix.MOD_ID, value = Dist.CLIENT)
public final class SimulatedRollerClientHandler {

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
                roller.setAnimatedSpeed(roller.mode != null
                    && roller.mode.getValue() == SimulatedRollerHandler.SIMULATED_PAVING_MODE
                    && velocity.dot(facing) > 0
                    ? animationSpeed(velocity.length())
                    : 0);
            }
        }
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
}
