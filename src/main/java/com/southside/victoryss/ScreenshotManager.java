package com.southside.victoryss;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScreenshotManager {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static void takeVictoryScreenshot(MinecraftClient client) {
        EXECUTOR.schedule(() -> {
            client.execute(() -> {
                try {
                    NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
                    
                    EXECUTOR.execute(() -> {
                        File tempFile = null;
                        try {
                            tempFile = Files.createTempFile("victory_ss_", ".png").toFile();
                            nativeImage.writeTo(tempFile);
                            nativeImage.close();
                            
                            String absolutePath = tempFile.getAbsolutePath();
                            String escapedPath = absolutePath.replace("'", "''");
                            String command = String.format(
                                "powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -STA -Command \"Add-Type -AssemblyName System.Windows.Forms, System.Drawing; [System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile('%s'))\"",
                                escapedPath
                            );
                            
                            Process process = Runtime.getRuntime().exec(command);
                            
                            if (process.waitFor(8, TimeUnit.SECONDS) && process.exitValue() == 0) {
                                client.execute(() -> {
                                    if (client.player != null) {
                                        // "§a[VSC] 截图成功！"
                                        // Unicode: \u00a7a[VSC] \u622a\u56fe\u6210\u529f\uff01
                                        client.player.sendMessage(Text.literal("\u00a7a[VSC] \u622a\u56fe\u6210\u529f\uff01"), false);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            if (nativeImage != null) nativeImage.close();
                        } finally {
                            if (tempFile != null && tempFile.exists()) {
                                File finalTempFile = tempFile;
                                EXECUTOR.schedule(() -> {
                                    try { finalTempFile.delete(); } catch (Exception ignored) {}
                                }, 5, TimeUnit.SECONDS);
                            }
                        }
                    });
                } catch (Exception ignored) {}
            });
        }, 500, TimeUnit.MILLISECONDS);
    }
}
