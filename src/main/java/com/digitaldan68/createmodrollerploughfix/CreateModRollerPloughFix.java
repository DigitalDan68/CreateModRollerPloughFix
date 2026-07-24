package com.digitaldan68.createmodrollerploughfix;

import com.digitaldan68.createmodrollerploughfix.config.SimulatedBehaviorConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CreateModRollerPloughFix.MOD_ID)
public final class CreateModRollerPloughFix {

    public static final String MOD_ID = "create_mod_roller_plough_fix";

    public CreateModRollerPloughFix(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SimulatedBehaviorConfig.SERVER_SPEC);
    }
}
