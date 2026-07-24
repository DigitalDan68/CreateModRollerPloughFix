package com.digitaldan68.createmodrollerploughfix;

import com.digitaldan68.createmodrollerploughfix.config.SimulatedBehaviorConfig;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.contraptions.actors.plough.PloughBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Supplies the movement callback that Create normally receives from a piston
 * or a standard Create contraption. Sable moves block positions through a
 * physics pose, so those callbacks have to be reconstructed from the
 * projected position once per server tick.
 */
@EventBusSubscriber(modid = CreateModRollerPloughFix.MOD_ID)
public final class SimulatedPloughHandler {

    private static final GameProfile PLOUGH_PROFILE = new GameProfile(
        UUID.fromString("9e2faded-eeee-4ec2-c314-dad129ae971d"), "Plough"
    );

    private static final Map<ServerLevel, Map<BlockPos, BlockPos>> LAST_WORLD_POSITIONS = new HashMap<>();

    private SimulatedPloughHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SimulatedBehaviorConfig.isSimulatedPloughEnabled()) {
            LAST_WORLD_POSITIONS.clear();
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }

            Set<BlockPos> activePloughs = new HashSet<>();
            Map<BlockPos, BlockPos> previousPositions = LAST_WORLD_POSITIONS.computeIfAbsent(level, ignored -> new HashMap<>());
            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                tickPloughs(level, subLevel, activePloughs, previousPositions);
            }
            previousPositions.keySet().retainAll(activePloughs);
        }
    }

    private static void tickPloughs(ServerLevel level, ServerSubLevel subLevel, Set<BlockPos> activePloughs,
                                    Map<BlockPos, BlockPos> previousPositions) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds.volume() <= 0) {
            return;
        }

        for (BlockPos localPos : BlockPos.betweenClosed(
            bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()
        )) {
            if (!(level.getBlockState(localPos).getBlock() instanceof PloughBlock)) {
                continue;
            }

            BlockPos immutableLocalPos = localPos.immutable();
            activePloughs.add(immutableLocalPos);
            BlockPos worldPos = BlockPos.containing(
                Sable.HELPER.projectOutOfSubLevel(level, immutableLocalPos.getCenter())
            );

            if (worldPos.equals(previousPositions.put(immutableLocalPos, worldPos))) {
                continue;
            }

            ploughWorldPosition(level, worldPos);
        }
    }

    private static void ploughWorldPosition(ServerLevel level, BlockPos worldPos) {
        BlockPos below = worldPos.below();
        if (!level.isLoaded(below)) {
            return;
        }

        FakePlayer player = FakePlayerFactory.get(level, PLOUGH_PROFILE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_HOE));

        Vec3 center = Vec3.atCenterOf(worldPos);
        BlockHitResult hit = level.clip(new ClipContext(
            center, center.add(0, -1, 0), Block.OUTLINE, Fluid.NONE, player
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            new ItemStack(Items.DIAMOND_HOE).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }
    }
}
