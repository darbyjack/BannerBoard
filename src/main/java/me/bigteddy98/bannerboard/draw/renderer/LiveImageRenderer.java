package me.bigteddy98.bannerboard.draw.renderer;

import me.bigteddy98.bannerboard.BannerBoardPlugin;
import me.bigteddy98.bannerboard.api.BannerBoardRenderer;
import me.bigteddy98.bannerboard.api.DisableBannerBoardException;
import me.bigteddy98.bannerboard.api.Setting;
import me.bigteddy98.bannerboard.util.AtomicString;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveImageRenderer extends BannerBoardRenderer<BufferedImage> {

    public LiveImageRenderer(List<Setting> parameters, int allowedWidth, int allowedHeight) {
        super(parameters, allowedWidth, allowedHeight);

        if (!this.hasSetting("url")) {
            throw new DisableBannerBoardException("Renderer LIVEIMG did not have a valid URL parameter, disabling...");
        }
    }

    @Override
    public BufferedImage asyncRenderPrepare(final Player p) throws Exception {
        // do place holders
        final AtomicString pngURL = new AtomicString(this.getSetting("url").getValue());

        final Object lock = new Object();
        final AtomicBoolean finished = new AtomicBoolean(false);
        // switch to main bukkit thread
        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    pngURL.set(BannerBoardPlugin.getInstance().applyPlaceholders(pngURL.get(), p));
                } finally {
                    synchronized (lock) {
                        finished.set(true);
                        lock.notifyAll();
                    }
                }
            }
        }.runTask(BannerBoardPlugin.getInstance());

        synchronized (lock) {
            while (!finished.get()) {
                lock.wait();
            }
        }

        String link = pngURL.get();
        return BannerBoardPlugin.getInstance().fetchImage(link);
    }

    @Override
    public void render(Player p, BufferedImage image, Graphics2D g, BufferedImage preparation) {
        if (preparation == null) {
            return;
        }
        BufferedImage img = (BufferedImage) preparation;

        Integer xOffset = null;
        Integer yOffset = null;

        if (this.hasSetting("xOffset")) {
            xOffset = Integer.parseInt(this.getSetting("xOffset").getValue());
        }
        if (this.hasSetting("yOffset")) {
            yOffset = Integer.parseInt(this.getSetting("yOffset").getValue());
        }

        int width = img.getWidth();
        int height = img.getHeight();
        if (this.hasSetting("width")) {
            width = Integer.parseInt(this.getSetting("width").getValue());
        }
        if (this.hasSetting("height")) {
            height = Integer.parseInt(this.getSetting("height").getValue());
        }

        // fix the possible yOffset and xOffset null
        if (xOffset == null) {
            xOffset = (image.getWidth() / 2 - (width / 2));
        }
        if (yOffset == null) {
            yOffset = (image.getHeight() / 2 - (height / 2));
        }

        g.drawImage(img, xOffset, yOffset, width, height, null);
    }
}
