package com.digitaldan68.createmodrollerploughfix.mixin;

import com.digitaldan68.createmodrollerploughfix.SimulatedRollerHandler;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RollerBlockEntity.class)
public abstract class RollerBlockEntityMixin extends SmartBlockEntity implements BlockEntitySubLevelActor {

    @Unique
    private BlockPos sable$previousWorldPos = BlockPos.ZERO;

    public RollerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (this.level == null || Sable.HELPER.getContaining(this) != subLevel) {
            return;
        }
        sable$previousWorldPos = SimulatedRollerHandler.tick(
            (RollerBlockEntity) (Object) this, subLevel, sable$previousWorldPos
        );
    }
}
