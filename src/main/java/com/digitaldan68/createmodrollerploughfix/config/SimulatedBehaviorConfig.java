package com.digitaldan68.createmodrollerploughfix.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Server-synchronised settings for the behavior this addon adds to Sable
 * simulated contraptions. The ordinary Create roller compatibility fix does
 * not read this config and therefore remains active independently.
 */
public final class SimulatedBehaviorConfig {

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Server, ModConfigSpec> configured = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = configured.getLeft();
        SERVER_SPEC = configured.getRight();
    }

    private SimulatedBehaviorConfig() {
    }

    public static boolean isSimulatedBehaviorEnabled() {
        return SERVER_SPEC.isLoaded() && SERVER.enabled.getAsBoolean();
    }

    public static boolean isSimulatedPloughEnabled() {
        return isSimulatedBehaviorEnabled() && SERVER.ploughEnabled.getAsBoolean();
    }

    public static boolean isSimulatedRollerEnabled() {
        return isSimulatedBehaviorEnabled() && SERVER.rollerEnabled.getAsBoolean();
    }

    public static int simulatedRollerClearHeadroomBlocks() {
        return isSimulatedRollerEnabled() ? SERVER.clearHeadroomBlocks.get() : 0;
    }

    public static boolean simulatedRollerConsumesPavingMaterials() {
        return isSimulatedRollerEnabled() && SERVER.consumePavingMaterials.getAsBoolean();
    }

    public static float simulatedRollerReverseAnimationEasing() {
        return isSimulatedRollerEnabled() ? SERVER.reverseAnimationEasing.get().floatValue() : 0.25F;
    }

    public static final class Server {

        private final ModConfigSpec.BooleanValue enabled;
        private final ModConfigSpec.BooleanValue ploughEnabled;
        private final ModConfigSpec.BooleanValue rollerEnabled;
        private final ModConfigSpec.IntValue clearHeadroomBlocks;
        private final ModConfigSpec.BooleanValue consumePavingMaterials;
        private final ModConfigSpec.DoubleValue reverseAnimationEasing;

        private Server(ModConfigSpec.Builder builder) {
            builder.comment("Settings for functionality added to Sable simulated contraptions.")
                .push("simulated_behavior");

            enabled = builder.comment("Master switch for all simulated Plough and Roller behavior added by this mod.")
                .define("enabled", true);

            builder.push("mechanical_plough");
            ploughEnabled = builder.comment("Allow Mechanical Ploughs on simulated contraptions to act in the main world.")
                .define("enabled", true);
            builder.pop();

            builder.push("mechanical_roller");
            rollerEnabled = builder.comment("Allow Mechanical Rollers on simulated contraptions to pave and clear blocks in the main world.")
                .define("enabled", true);
            clearHeadroomBlocks = builder.comment(
                    "How many blocks above the paved surface a simulated roller clears.",
                    "0 keeps overhead blocks, 2 matches this mod's default simulated Roller behavior."
                )
                .defineInRange("clear_headroom_blocks", 2, 0, 2);
            consumePavingMaterials = builder.comment(
                    "Require the paving block to be available in an inventory carried by the same simulated contraption.",
                    "Set to false for unlimited simulated paving."
                )
                .define("consume_paving_materials", true);
            reverseAnimationEasing = builder.comment(
                    "How quickly a simulated roller's wheel eases to a stop after reversing.",
                    "1.0 stops immediately; smaller values coast longer."
                )
                .defineInRange("reverse_animation_easing", 0.25D, 0.01D, 1.0D);
            builder.pop();

            builder.pop();
        }
    }
}
