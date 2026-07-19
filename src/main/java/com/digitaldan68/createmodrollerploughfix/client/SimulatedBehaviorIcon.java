package com.digitaldan68.createmodrollerploughfix.client;

import com.digitaldan68.createmodrollerploughfix.CreateModRollerPloughFix;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * A Create-compatible icon backed by this addon's own texture, rather than an
 * override of Create's icon atlas.
 */
@OnlyIn(Dist.CLIENT)
public final class SimulatedBehaviorIcon extends AllIcons {

    public static final SimulatedBehaviorIcon INSTANCE = new SimulatedBehaviorIcon();

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        CreateModRollerPloughFix.MOD_ID, "textures/gui/simulated_behavior_icon_sheet.png"
    );
    private static final int ICON_SIZE = 16;
    private static final int TEXTURE_SIZE = 256;
    private static final int ICON_Y = 13 * ICON_SIZE;
    private static final float MIN_U = 0.0F;
    private static final float MAX_U = (float) ICON_SIZE / TEXTURE_SIZE;
    private static final float MIN_V = (float) ICON_Y / TEXTURE_SIZE;
    private static final float MAX_V = (float) (ICON_Y + ICON_SIZE) / TEXTURE_SIZE;

    private SimulatedBehaviorIcon() {
        super(0, 0);
    }

    @Override
    public void bind() {
        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x, y, 0, MIN_U * TEXTURE_SIZE, MIN_V * TEXTURE_SIZE,
            ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int color) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.text(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Color tint = new Color(color);

        vertex(consumer, pose, 0, 0, tint, MIN_U, MIN_V);
        vertex(consumer, pose, 0, 1, tint, MIN_U, MAX_V);
        vertex(consumer, pose, 1, 1, tint, MAX_U, MAX_V);
        vertex(consumer, pose, 1, 0, tint, MAX_U, MIN_V);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, Color tint,
                               float u, float v) {
        consumer.addVertex(pose, x, y, 0)
            .setColor(tint.getRed(), tint.getGreen(), tint.getBlue(), 255)
            .setUv(u, v)
            .setLight(LightTexture.FULL_BRIGHT);
    }
}
