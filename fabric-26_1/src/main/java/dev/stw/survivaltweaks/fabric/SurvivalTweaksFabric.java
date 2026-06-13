package dev.stw.survivaltweaks.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import dev.stw.survivaltweaks.ClientController;
import dev.stw.survivaltweaks.SurvivalTweaks;
import dev.stw.survivaltweaks.core.OutlineRender;
import dev.stw.survivaltweaks.screens.HudOverlay;
import dev.stw.survivaltweaks.core.ScanController;

public class SurvivalTweaksFabric implements ClientModInitializer {
    private static final Identifier HUD_ELEMENT_ID = SurvivalTweaks.id("st_overlay");

    @Override
    public void onInitializeClient() {
        SurvivalTweaks.INSTANCE.init();

        KeyMappingHelper.registerKeyMapping(SurvivalTweaks.OPEN_GUI_KEY);
        KeyMappingHelper.registerKeyMapping(SurvivalTweaks.TOGGLE_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(this::clientTickEvent);
        ClientLifecycleEvents.CLIENT_STARTED.register((mc) -> ClientController.onSetup());
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::renderOverlay);

        HudElementRegistry.addLast(HUD_ELEMENT_ID, (guiGraphics, tickCounter) -> HudOverlay.renderGameOverlayEvent(guiGraphics));
    }

    private void renderOverlay(LevelRenderContext levelRenderContext) {
        OutlineRender.renderBlocks(levelRenderContext.poseStack());
    }


    private void clientTickEvent(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            ScanController.INSTANCE.clearProfileActivationContext();
            return;
        }

        ScanController.INSTANCE.updateProfileForCurrentConnection();

        if (mc.screen != null) {
            return;
        }

        ScanController.INSTANCE.requestBlockFinder(false);

        while (SurvivalTweaks.OPEN_GUI_KEY.consumeClick()) {
            SurvivalTweaks.INSTANCE.onOpenGuiKeyPressed();
        }

        while (SurvivalTweaks.TOGGLE_KEY.consumeClick()) {
            SurvivalTweaks.INSTANCE.onToggleKeyPressed();
        }
    }
}
