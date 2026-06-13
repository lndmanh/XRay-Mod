package dev.stw.survivaltweaks.screens;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import dev.stw.survivaltweaks.SurvivalTweaks;
import dev.stw.survivaltweaks.core.ScanController;

public class HudOverlay {
    private static final Identifier CIRCLE = SurvivalTweaks.assetLocation("gui/circle.png");

    public static void renderGameOverlayEvent(GuiGraphicsExtractor graphics) {
        // Draw Indicator
        if(!ScanController.INSTANCE.isActive() || !SurvivalTweaks.config().showOverlay.get())
            return;

        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        boolean renderDebug = gpuDevice != null && gpuDevice.isDebuggingEnabled();

        int x = 5, y = 5;
        if (renderDebug) {
            x = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 10;
            y = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 10;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, CIRCLE, x, y, 0f, 0f, 5, 5, 5, 5, 0xFF00FF00);

        int width = Minecraft.getInstance().font.width(I18n.get("survivaltweaks.overlay"));
        graphics.text(Minecraft.getInstance().font, I18n.get("survivaltweaks.overlay"), x + (!renderDebug ? 10 : -width - 5), y - (!renderDebug ? 1 : 2), 0xff00ff00);
    }
}
