package dev.stw.survivaltweaks;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import dev.stw.survivaltweaks.screens.ScanManageScreen;
import dev.stw.survivaltweaks.utils.XPlatShim;
import dev.stw.survivaltweaks.core.ScanController;

import java.util.ServiceLoader;

public enum SurvivalTweaks {
	INSTANCE;

	public static final String MOD_ID = "survivaltweaks";
	private static final Logger LOGGER = LogManager.getLogger();

	public static final XPlatShim XPLAT = ServiceLoader.load(XPlatShim.class).findFirst().orElseThrow();

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SurvivalTweaks.id("category"));
	public static final KeyMapping TOGGLE_KEY = new KeyMapping(I18n.get("survivaltweaks.config.toggle"), GLFW.GLFW_KEY_BACKSLASH, CATEGORY);
	public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(I18n.get("survivaltweaks.config.open"), GLFW.GLFW_KEY_G, CATEGORY);

	public void init() {
	}

	public void onToggleKeyPressed() {
		if (minecraftNotReady()) {
			LOGGER.warn("Cannot toggle scan, Minecraft is not ready.");
			return;
		}

		ScanController.INSTANCE.toggleActive();
	}

	public void onOpenGuiKeyPressed() {
		if (minecraftNotReady()) {
			LOGGER.warn("Cannot open GUI, Minecraft is not ready.");
			return;
		}

		Minecraft.getInstance().setScreen(new ScanManageScreen());
	}

	private boolean minecraftNotReady() {
		Minecraft mc = Minecraft.getInstance();

		return mc.player == null || Minecraft.getInstance().screen != null || Minecraft.getInstance().level == null;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static Identifier assetLocation(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, "textures/" + path);
	}

	public static Configuration config() {
		return Configuration.INSTANCE;
	}
}
