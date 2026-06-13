package dev.stw.survivaltweaks.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.Event;
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

        KeyBindingHelper.registerKeyBinding(SurvivalTweaks.OPEN_GUI_KEY);
        KeyBindingHelper.registerKeyBinding(SurvivalTweaks.TOGGLE_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(this::clientTickEvent);
        ClientLifecycleEvents.CLIENT_STARTED.register((mc) -> ClientController.onSetup());
        registerWorldRenderEvent();

        HudElementRegistry.addLast(HUD_ELEMENT_ID, (guiGraphics, tickCounter) -> HudOverlay.renderGameOverlayEvent(guiGraphics));
    }

    private void renderOverlay(Object worldRenderContext) {
        try {
            OutlineRender.renderBlocks((com.mojang.blaze3d.vertex.PoseStack) worldRenderContext.getClass().getMethod("matrices").invoke(worldRenderContext));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerWorldRenderEvent() {
        try {
            for (var field : WorldRenderEvents.class.getFields()) {
                if (Event.class.isAssignableFrom(field.getType()) && field.getName().contains("TRANSLUCENT")) {
                    var event = (Event) field.get(null);
                    var register = event.getClass().getMethods()[0];
                    var callbackType = register.getParameterTypes()[0];
                    var callback = java.lang.reflect.Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType}, (proxy, method, args) -> {
                        if (method.getName().equals("invoke")) {
                            renderOverlay(args[0]);
                        }
                        return null;
                    });
                    event.getClass().getMethod("register", callbackType).invoke(event, callback);
                    return;
                }
            }
            throw new IllegalStateException("No translucent world render event found");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }


    private void clientTickEvent(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }

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
