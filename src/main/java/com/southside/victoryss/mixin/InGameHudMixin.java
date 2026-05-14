package com.southside.victoryss.mixin;

import com.southside.victoryss.VictoryDetector;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void victoryss$onSetTitle(Text title, CallbackInfo ci) {
        if (title != null && VictoryDetector.match(title.getString())) {
            VictoryDetector.triggerVictory();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void victoryss$onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message != null && VictoryDetector.match(message.getString())) {
            VictoryDetector.triggerVictory();
        }
    }
}
