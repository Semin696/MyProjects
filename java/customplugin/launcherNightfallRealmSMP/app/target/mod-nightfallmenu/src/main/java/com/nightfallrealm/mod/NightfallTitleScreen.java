package com.nightfallrealm.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NightfallTitleScreen extends Screen {

    private static final String SERVER_IP = "f1.rustix.me";
    private static final int SERVER_PORT = 25283;
    private static final int STAR_COUNT = 180;

    private final Random random = new Random();
    private final List<Star> stars = new ArrayList<>();
    private long startTime;

    private static class Star {
        double x, y, size, baseBrightness, twinkleSpeed, phase;
        double driftX, driftY;
        int colorMode;
        boolean twinkling;
    }

    public NightfallTitleScreen() {
        super(Text.literal("Nightfall Realm SMP"));
        startTime = System.currentTimeMillis();
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            Star star = new Star();
            star.x = random.nextDouble() * Math.max(width, 1);
            star.y = random.nextDouble() * Math.max(height, 1);
            star.size = 0.5 + random.nextDouble() * 2.5;
            star.baseBrightness = 0.3 + random.nextDouble() * 0.7;
            star.twinkleSpeed = 0.5 + random.nextDouble() * 2.0;
            star.phase = random.nextDouble() * Math.PI * 2;
            star.driftX = (random.nextDouble() - 0.5) * 0.01;
            star.driftY = (random.nextDouble() - 0.5) * 0.01;
            star.twinkling = random.nextDouble() > 0.15;
            star.colorMode = random.nextInt(4);
            stars.add(star);
        }
    }

    @Override
    protected void init() {
        if (stars.isEmpty()) initStars();

        int cx = width / 2;
        int by = height / 2 + 40;
        int bw = 220;
        int bh = 28;
        int gap = 6;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("✦ Подключиться к серверу"),
                btn -> connectToServer()
        ).dimensions(cx - bw / 2, by, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Зайти на SMP"),
                btn -> client.setScreen(new MultiplayerScreen(this))
        ).dimensions(cx - bw / 2, by + bh + gap, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Настройки"),
                btn -> client.setScreen(new OptionsScreen(this, client.options))
        ).dimensions(cx - bw / 2, by + (bh + gap) * 2, bw, bh).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Выйти из игры"),
                btn -> client.scheduleStop()
        ).dimensions(cx - bw / 2, by + (bh + gap) * 3, bw, bh).build());
    }

    private void connectToServer() {
        String addressStr = SERVER_IP + ":" + SERVER_PORT;
        ServerAddress address = ServerAddress.parse(addressStr);
        ServerInfo serverInfo = new ServerInfo("Nightfall Realm SMP", addressStr, false);
        ConnectScreen.connect(this, MinecraftClient.getInstance(), address, serverInfo, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderStars(context, delta);
        renderNebula(context);
        renderTitle(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height,
                0xFF070720, 0xFF0E0E3A);
        context.fillGradient(0, height / 2, width, height,
                0xFF0E0E3A, 0xFF151550);
        context.fillGradient(0, (int)(height * 0.7), width, height,
                0xFF151550, 0xFF1A1A5E);
    }

    private void renderNebula(DrawContext context) {
        renderNebulaBlob(context, (int)(width * 0.15), (int)(height * 0.2), 120, 0x204B0082);
        renderNebulaBlob(context, (int)(width * 0.85), (int)(height * 0.75), 140, 0x18191970);
        renderNebulaBlob(context, (int)(width * 0.5), (int)(height * 0.5), 100, 0x102E1A47);
    }

    private void renderNebulaBlob(DrawContext context, int cx, int cy, int radius, int argb) {
        int alpha = (argb >> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        for (int r = radius; r > 0; r -= 4) {
            int a = alpha * (radius - r) / radius;
            int color = (a << 24) | rgb;
            int half = r / 2;
            context.fill(cx - half, cy - half, cx + half, cy + half, color);
        }
    }

    private void renderStars(DrawContext context, float delta) {
        long now = System.currentTimeMillis();
        double elapsed = (now - startTime) / 1000.0;

        for (Star star : stars) {
            star.x += star.driftX;
            star.y += star.driftY;

            if (star.x < -5) star.x = width + 5;
            if (star.x > width + 5) star.x = -5;
            if (star.y < -5) star.y = height + 5;
            if (star.y > height + 5) star.y = -5;

            double brightness = star.baseBrightness;
            if (star.twinkling) {
                double twinkle = 0.5 + 0.5 * Math.sin(elapsed * star.twinkleSpeed + star.phase);
                brightness *= Math.max(0.1, twinkle);
            }
            brightness = Math.min(1.0, Math.max(0.05, brightness));

            int starColor = getStarColor(star.colorMode, brightness);
            int size = (int) Math.max(1, star.size);

            context.fill((int) star.x, (int) star.y,
                    (int) star.x + size, (int) star.y + size, starColor);

            if (star.size > 2.0 && brightness > 0.5) {
                int glowSize = size * 3;
                int glowColor = getStarColor(star.colorMode, brightness * 0.12);
                context.fill((int) star.x - glowSize / 2 + size / 2,
                        (int) star.y - glowSize / 2 + size / 2,
                        (int) star.x + glowSize / 2 + size / 2,
                        (int) star.y + glowSize / 2 + size / 2, glowColor);
            }
        }
    }

    private int getStarColor(int mode, double brightness) {
        int a = (int) (brightness * 255);
        a = Math.min(255, Math.max(5, a));
        return switch (mode) {
            case 0 -> (a << 24) | 0xE0E0FF;
            case 1 -> (a << 24) | 0xC8D0FF;
            case 2 -> (a << 24) | 0xFFFACD;
            case 3 -> (a << 24) | 0xFFC8D0;
            default -> (a << 24) | 0xFFFFFF;
        };
    }

    private void renderTitle(DrawContext context) {
        int cx = width / 2;
        String title = "Nightfall Realm SMP";
        String version = "Minecraft 1.21.11 | Fabric";
        String serverInfo = "Сервер: " + SERVER_IP + ":" + SERVER_PORT;

        int titleW = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, cx - titleW / 2, height / 4 - 30,
                0xFFE0E0FF, false);

        int verW = textRenderer.getWidth(version);
        context.drawText(textRenderer, version, cx - verW / 2, height / 4 - 8,
                0xFF7777AA, false);

        int srvW = textRenderer.getWidth(serverInfo);
        context.drawText(textRenderer, serverInfo, cx - srvW / 2, height / 4 + 6,
                0xFF5555AA, false);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        client.setScreen(null);
    }
}
