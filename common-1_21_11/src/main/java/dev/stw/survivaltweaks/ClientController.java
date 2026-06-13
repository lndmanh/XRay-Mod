package dev.stw.survivaltweaks;

import net.minecraft.client.resources.language.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.stw.survivaltweaks.core.ScanController;

public class ClientController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientController.class);

    public static void onSetup() {
        LOGGER.debug(I18n.get("survivaltweaks.debug.init"));
        SurvivalTweaks.config().load();

        ScanController.INSTANCE.init();
    }
}
