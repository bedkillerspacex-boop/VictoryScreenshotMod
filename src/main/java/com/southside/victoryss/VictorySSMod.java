package com.southside.victoryss;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class VictorySSMod implements ClientModInitializer {
    private boolean victoryDetected = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VictoryDetector.updateSession(client);
            if (client.world == null) {
                victoryDetected = false;
                return;
            }

            boolean isVictory = VictoryDetector.isVictory();
            if (isVictory && !victoryDetected) {
                ScreenshotManager.takeVictoryScreenshot(client);
                VictoryDetector.onVictoryScreenshotTaken();
                victoryDetected = true;
            } else if (!isVictory) {
                victoryDetected = false;
            }
        });
    }
}
