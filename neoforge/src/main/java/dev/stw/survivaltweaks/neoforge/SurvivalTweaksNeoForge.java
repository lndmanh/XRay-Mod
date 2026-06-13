package dev.stw.survivaltweaks.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.stw.survivaltweaks.ClientController;
import dev.stw.survivaltweaks.SurvivalTweaks;
import dev.stw.survivaltweaks.screens.HudOverlay;
import dev.stw.survivaltweaks.core.ScanController;
import dev.stw.survivaltweaks.core.OutlineRender;

@Mod(SurvivalTweaks.MOD_ID)
public class SurvivalTweaksNeoForge {
	private static final Identifier GUI_LAYER_ID = SurvivalTweaks.id("st_overlay");

	public static final Logger LOGGER = LogManager.getLogger();

	public SurvivalTweaksNeoForge(IEventBus eventBus) {
		if (!FMLEnvironment.getDist().isClient()) {
			return;
		}

		SurvivalTweaks.INSTANCE.init();

		eventBus.addListener(this::registerKeyBinding);
		eventBus.addListener(this::registerPipeline);
		NeoForge.EVENT_BUS.addListener(this::eventInput);
		NeoForge.EVENT_BUS.addListener(this::tickEnd);
		eventBus.addListener(this::onClientSetup);

		NeoForge.EVENT_BUS.addListener(this::onWorldRenderLast);
		eventBus.addListener(this::registerGuiLayer);
	}

	private void registerGuiLayer(RegisterGuiLayersEvent event) {
		event.registerAboveAll(GUI_LAYER_ID, (guiGraphics, tickCounter) -> HudOverlay.renderGameOverlayEvent(guiGraphics));
	}

	private void onWorldRenderLast(RenderLevelStageEvent.AfterWeather event) {
		OutlineRender.renderBlocks(event.getPoseStack());
	}

	public void onClientSetup(FMLClientSetupEvent event) {
		ClientController.onSetup();
	}

	public void registerKeyBinding(RegisterKeyMappingsEvent event) {
		event.register(SurvivalTweaks.TOGGLE_KEY);
		event.register(SurvivalTweaks.OPEN_GUI_KEY);
	}

	public void registerPipeline(RegisterRenderPipelinesEvent event) {
	}

	public void eventInput(InputEvent.Key event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || Minecraft.getInstance().screen != null || Minecraft.getInstance().level == null)
			return;

		if (SurvivalTweaks.TOGGLE_KEY.consumeClick()) {
			SurvivalTweaks.INSTANCE.onToggleKeyPressed();
		}

		if (SurvivalTweaks.OPEN_GUI_KEY.consumeClick()) {
			SurvivalTweaks.INSTANCE.onOpenGuiKeyPressed();
		}
	}

	public void tickEnd(ClientTickEvent.Post event) {
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			ScanController.INSTANCE.clearProfileActivationContext();
			return;
		}

		ScanController.INSTANCE.updateProfileForCurrentConnection();

		if (mc.screen != null) {
			return;
		}

		ScanController.INSTANCE.requestBlockFinder(false);
	}
}
