package com.digitaldan68.createmodrollerploughfix;

import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Server-side equivalent of Create's roller actor for a Sable sub-level.
 * Sable retains the roller block entity, so its filter and rolling mode stay
 * editable normally; this class supplies the missing world-motion callback.
 */
public final class SimulatedRollerHandler {

    /**
     * Create Aeronautics' fourth Roller Mode: "Replace Tracks". On a Sable
     * contraption we repurpose it as the opt-in for world-projected paving.
     */
    public static final int SIMULATED_PAVING_MODE = 3;

    private SimulatedRollerHandler() {
    }

    public static BlockPos tick(RollerBlockEntity roller, ServerSubLevel subLevel, BlockPos previousWorldPos) {
        ServerLevel level = (ServerLevel) roller.getLevel();
        if (level == null || roller.filtering == null || roller.mode == null) {
            return previousWorldPos;
        }

        BlockPos localPos = roller.getBlockPos();
        // This is the exact active-area offset used by Create's
        // RollerMovementBehaviour: forward by 0.45 blocks and down two.
        // The first version used the roller's own block position, which put
        // paving one block above Create's normal target surface.
        BlockPos worldPos = projectedActivePosition(level, localPos, roller.getBlockState());
        if (worldPos.equals(previousWorldPos)) {
            return previousWorldPos;
        }

        // The existing Replace Tracks option is the per-roller opt-in. Keep
        // tracking its position while disabled so toggling it on does not
        // process every block the contraption travelled across while off.
        if (roller.mode.getValue() != SIMULATED_PAVING_MODE) {
            return worldPos;
        }

        Vec3 previousCenter = previousWorldPos.getCenter();
        Vec3 movement = worldPos.getCenter().subtract(previousCenter);
        Direction facing = projectedFacing(level, localPos, roller.getBlockState());
        if (movement.dot(Vec3.atLowerCornerOf(facing.getNormal())) > 0) {
            roll(level, subLevel, roller, worldPos, projectedUp(level, localPos));
        }

        return worldPos;
    }

    private static Direction projectedFacing(ServerLevel level, BlockPos localPos, BlockState state) {
        Direction localFacing = state.getValue(RollerBlock.FACING);
        Vec3 center = Sable.HELPER.projectOutOfSubLevel(level, localPos.getCenter());
        Vec3 projectedFront = Sable.HELPER.projectOutOfSubLevel(level,
            localPos.getCenter().add(Vec3.atLowerCornerOf(localFacing.getNormal())));
        Vec3 delta = projectedFront.subtract(center);
        return Direction.getNearest(delta.x, delta.y, delta.z);
    }

    private static BlockPos projectedActivePosition(ServerLevel level, BlockPos localPos, BlockState state) {
        Direction facing = state.getValue(RollerBlock.FACING);
        Vec3 activeOffset = Vec3.atLowerCornerOf(facing.getNormal()).scale(0.45).subtract(0, 2, 0);
        return BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, localPos.getCenter().add(activeOffset)));
    }

    /** Returns the contraption's local-up direction after its Sable pose is applied. */
    private static Direction projectedUp(ServerLevel level, BlockPos localPos) {
        Vec3 center = Sable.HELPER.projectOutOfSubLevel(level, localPos.getCenter());
        Vec3 projectedUp = Sable.HELPER.projectOutOfSubLevel(level, localPos.getCenter().add(0, 1, 0));
        Vec3 delta = projectedUp.subtract(center);
        return Direction.getNearest(delta.x, delta.y, delta.z);
    }

    private static void roll(ServerLevel level, ServerSubLevel subLevel, RollerBlockEntity roller, BlockPos position,
                             Direction up) {
        ItemStack filter = roller.filtering.getFilter();
        BlockState pavingState = RollerMovementBehaviour.getStateToPaveWith(filter);

        // The simulated Replace Tracks mode behaves as Clear Blocks and Pave:
        // replace the surface and clear only its two blocks of headroom.
        for (int y = 1; y <= 2; y++) {
            BlockPos target = position.relative(up, y);
            if (canRollThrough(level, target)) {
                level.destroyBlock(target, true);
            }
        }

        if (pavingState.isAir()) {
            return;
        }
        tryPave(level, subLevel, filter, pavingState, position);
    }

    private static boolean canRollThrough(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return BlockBreakingKineticBlockEntity.isBreakable(state, state.getDestroySpeed(level, pos))
            && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static void tryPave(ServerLevel level, ServerSubLevel subLevel, ItemStack filter, BlockState toPlace,
                                 BlockPos target) {
        BlockState existing = level.getBlockState(target);
        if (existing.is(toPlace.getBlock())) {
            return;
        }
        // A roller paves by replacing its path surface. It must not, however,
        // overwrite unbreakable blocks such as bedrock.
        if (!existing.isAir() && !(existing.getBlock() instanceof LeavesBlock) && !existing.canBeReplaced()
            && !BlockBreakingKineticBlockEntity.isBreakable(existing, existing.getDestroySpeed(level, target))) {
            return;
        }
        if (!consumeMaterial(level, subLevel, filter)) {
            return;
        }
        level.setBlockAndUpdate(target, toPlace);
    }

    /** Uses inventories carried by the same simulated contraption as the roller. */
    private static boolean consumeMaterial(ServerLevel level, ServerSubLevel subLevel, ItemStack filter) {
        if (filter.isEmpty()) {
            return false;
        }
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        for (BlockPos localPos : BlockPos.betweenClosed(
            bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()
        )) {
            IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, localPos, null);
            if (inventory == null) {
                continue;
            }
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stored = inventory.getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(stored, filter)) {
                    return !inventory.extractItem(slot, 1, false).isEmpty();
                }
            }
        }
        return false;
    }
}
