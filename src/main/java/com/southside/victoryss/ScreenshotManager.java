package com.southside.victoryss;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScreenshotManager {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static void takeVictoryScreenshot(MinecraftClient client) {
        EXECUTOR.schedule(() -> client.execute(() -> {
            try {
                NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());

                EXECUTOR.execute(() -> {
                    try {
                        BufferedImage image = toBufferedImage(nativeImage);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(new ImageTransferable(image), null);

                        sendPlayerMessage(client, "\u00a7a[VSC] \u622a\u56fe\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f\uff01");
                    } catch (Exception e) {
                        sendPlayerMessage(client, "\u00a7c[VSC] \u5199\u5165\u526a\u8d34\u677f\u5931\u8d25\uff01");
                    } finally {
                        nativeImage.close();
                    }
                });
            } catch (Exception ignored) {
            }
        }), 500, TimeUnit.MILLISECONDS);
    }

    private static BufferedImage toBufferedImage(NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, nativeImage.getColorArgb(x, y));
            }
        }

        return bufferedImage;
    }

    private static void sendPlayerMessage(MinecraftClient client, String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(message), false);
            }
        });
    }

    private static final class ImageTransferable implements Transferable {
        private final Image image;

        private ImageTransferable(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
