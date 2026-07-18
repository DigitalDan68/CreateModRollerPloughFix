package com.digitaldan68.createmodrollerploughfix.mixin;

import com.digitaldan68.createmodrollerploughfix.CreateModRollerPloughFix;
import com.digitaldan68.createmodrollerploughfix.SimulatedRollerHandler;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter.ScrollOptionSettingsFormatter;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.ryanhcode.sable.Sable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewords Aeronautics' fourth roller option only while its block entity lives
 * on a Sable sub-level. The original option name remains unchanged elsewhere.
 */
@Mixin(value = ScrollOptionBehaviour.class, remap = false)
public abstract class ScrollOptionBehaviourMixin {

    @Inject(method = "createBoard", at = @At("RETURN"), cancellable = true, remap = false)
    private void createModRollerPloughFix$renameSimulatedMode(Player player, BlockHitResult hitResult,
                                                               CallbackInfoReturnable<ValueSettingsBoard> cir) {
        ScrollOptionBehaviour<?> behaviour = (ScrollOptionBehaviour<?>) (Object) this;
        if (!(behaviour.blockEntity instanceof RollerBlockEntity)
            || Sable.HELPER.getContaining(behaviour.blockEntity) == null) {
            return;
        }

        ValueSettingsBoard board = cir.getReturnValue();
        if (!(board.formatter() instanceof ScrollOptionSettingsFormatter formatter) || board.maxValue() < 3) {
            return;
        }

        cir.setReturnValue(new ValueSettingsBoard(board.title(), board.maxValue(), board.milestoneInterval(),
            board.rows(), new SimulatedModeFormatter(formatter)));
    }

    private static final class SimulatedModeFormatter extends ScrollOptionSettingsFormatter {

        // User-supplied glyph at the first cell of the new bottom atlas row.
        private static final AllIcons SIMULATED_BEHAVIOR_ICON = new AllIcons(0, 13);

        private final ScrollOptionSettingsFormatter original;

        private SimulatedModeFormatter(ScrollOptionSettingsFormatter original) {
            super(new INamedIconOptions[0]);
            this.original = original;
        }

        @Override
        public MutableComponent format(ValueSettings setting) {
            if (setting.value() == SimulatedRollerHandler.SIMULATED_PAVING_MODE) {
                return Component.translatable(CreateModRollerPloughFix.MOD_ID + ".roller_mode.simulated_behavior");
            }
            return original.format(setting);
        }

        @Override
        public AllIcons getIcon(ValueSettings setting) {
            if (setting.value() == SimulatedRollerHandler.SIMULATED_PAVING_MODE) {
                return SIMULATED_BEHAVIOR_ICON;
            }
            return original.getIcon(setting);
        }
    }
}
