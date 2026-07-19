package com.digitaldan68.createmodrollerploughfix.mixin;

import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.ryanhcode.sable.Sable;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Keeps Sable's distance guard for rollers that truly belong to a sub-level,
 * while restoring Create's original breaker and paving tick for ordinary
 * contraptions. Sable's generic guard sees a roller's normal paving target as
 * more than two blocks away, which prevents its break-then-pave handoff.
 */
@Mixin(RollerMovementBehaviour.class)
public abstract class RollerMovementBehaviourMixin extends BlockBreakingMovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        if (createModRollerPloughFix$isOnSableSubLevel(context)) {
            super.tick(context);
            return;
        }

        // Adapted from Create 6.0.10's BlockBreakingMovementBehaviour ticker.
        // See THIRD_PARTY_NOTICES.md for the Create MIT attribution.
        // Defining it on RollerMovementBehaviour prevents Sable's generic
        // block-breaker mixin from cancelling ordinary roller paving.
        tickBreaker(context);

        CompoundTag data = context.data;
        if (!data.contains("WaitingTicks")) {
            return;
        }

        int waitingTicks = data.getInt("WaitingTicks");
        if (waitingTicks-- > 0) {
            data.putInt("WaitingTicks", waitingTicks);
            context.stall = true;
            return;
        }

        BlockPos position = NBTHelper.readBlockPos(data, "LastPos");
        data.remove("WaitingTicks");
        data.remove("LastPos");
        context.stall = false;
        visitNewPosition(context, position);
    }

    @Unique
    private static boolean createModRollerPloughFix$isOnSableSubLevel(MovementContext context) {
        return context.world != null
            && context.contraption != null
            && context.contraption.anchor != null
            && Sable.HELPER.getContaining(context.world, context.contraption.anchor) != null;
    }
}
