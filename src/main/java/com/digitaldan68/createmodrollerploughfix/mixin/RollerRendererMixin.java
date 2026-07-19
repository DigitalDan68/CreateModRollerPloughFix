package com.digitaldan68.createmodrollerploughfix.mixin;

import com.digitaldan68.createmodrollerploughfix.SimulatedRollerClientHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.content.contraptions.actors.roller.RollerRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses the simulated roller's time-integrated phase instead of Create's time-speed product. */
@Mixin(RollerRenderer.class)
public abstract class RollerRendererMixin {

    @Redirect(
        method = "renderSafe(Lcom/simibubi/create/content/contraptions/actors/roller/RollerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/contraptions/actors/harvester/HarvesterRenderer;transform(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Direction;Lnet/createmod/catnip/render/SuperByteBuffer;FLnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void createModRollerPloughFix$renderEasedSimulatedRoller(Level level, Direction facing,
                                                                        SuperByteBuffer buffer, float speed,
                                                                        Vec3 rotationOffset,
                                                                        RollerBlockEntity roller, float partialTicks,
                                                                        PoseStack poseStack,
                                                                        MultiBufferSource bufferSource,
                                                                        int packedLight, int packedOverlay) {
        if (SimulatedRollerClientHandler.hasEasedAnimation(roller)) {
            SimulatedRollerClientHandler.transformEased(roller, facing, buffer, rotationOffset, partialTicks);
            return;
        }

        HarvesterRenderer.transform(level, facing, buffer, speed, rotationOffset);
    }
}
