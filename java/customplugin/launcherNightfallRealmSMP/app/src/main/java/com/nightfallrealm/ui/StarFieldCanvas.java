package com.nightfallrealm.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class StarFieldCanvas extends Canvas {

    private static final int STAR_COUNT = 280;
    private static final long SHOOTING_STAR_MIN_INTERVAL = 4_000_000_000L;
    private static final long SHOOTING_STAR_RANDOM_EXTRA = 8_000_000_000L;
    private static final int MAX_SHOOTING_STARS = 5;

    private final Random random = new Random();
    private final List<Star> stars = new ArrayList<>();
    private final List<ShootingStar> shootingStars = new ArrayList<>();
    private final AnimationTimer timer;
    private long lastShootingStar = 0;
    private long startTime = 0;
    private boolean running = true;

    private static class Star {
        double x, y, size, baseBrightness, twinkleSpeed, phase;
        double driftX, driftY;
        Color color;
        boolean twinkling;
    }

    private static class ShootingStar {
        double x, y, vx, vy, length, opacity;
        boolean active;
    }

    public StarFieldCanvas(double width, double height) {
        super(width, height);
        initStars();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running) return;
                if (startTime == 0) startTime = now;
                update(now);
                render();
            }
        };
        timer.start();
    }

    private void initStars() {
        stars.clear();
        double w = getWidth();
        double h = getHeight();
        for (int i = 0; i < STAR_COUNT; i++) {
            Star star = new Star();
            star.x = random.nextDouble() * w;
            star.y = random.nextDouble() * h;
            star.size = 0.3 + random.nextDouble() * 2.8;
            star.baseBrightness = 0.25 + random.nextDouble() * 0.75;
            star.twinkleSpeed = 0.4 + random.nextDouble() * 2.5;
            star.phase = random.nextDouble() * Math.PI * 2;
            star.driftX = (random.nextDouble() - 0.5) * 0.015;
            star.driftY = (random.nextDouble() - 0.5) * 0.015;
            star.twinkling = random.nextDouble() > 0.2;

            float colorChoice = random.nextFloat();
            if (colorChoice < 0.55f) star.color = Color.WHITE;
            else if (colorChoice < 0.75f) star.color = Color.web("#C8D0FF");
            else if (colorChoice < 0.9f) star.color = Color.web("#FFFACD");
            else star.color = Color.web("#FFC8D0");

            stars.add(star);
        }
    }

    private void update(long now) {
        double elapsed = (now - startTime) / 1e9;
        double w = getWidth();
        double h = getHeight();

        for (Star star : stars) {
            star.x += star.driftX;
            star.y += star.driftY;
            if (star.x < -5) star.x = w + 5;
            if (star.x > w + 5) star.x = -5;
            if (star.y < -5) star.y = h + 5;
            if (star.y > h + 5) star.y = -5;
        }

        if (now - lastShootingStar > SHOOTING_STAR_MIN_INTERVAL
                + random.nextLong(SHOOTING_STAR_RANDOM_EXTRA)) {
            spawnShootingStar();
            lastShootingStar = now;
        }

        for (ShootingStar s : shootingStars) {
            if (s.active) {
                s.x += s.vx;
                s.y += s.vy;
                s.opacity -= 0.015;
                if (s.opacity <= 0.02
                        || s.x < -50 || s.x > w + 50
                        || s.y < -50 || s.y > h + 50) {
                    s.active = false;
                }
            }
        }
        shootingStars.removeIf(s -> !s.active);
    }

    private void spawnShootingStar() {
        ShootingStar s = new ShootingStar();
        s.x = random.nextDouble() * getWidth() * 0.8;
        s.y = -20;
        double angle = Math.PI / 5 + random.nextDouble() * Math.PI / 5;
        double speed = 10 + random.nextDouble() * 18;
        s.vx = Math.cos(angle) * speed;
        s.vy = Math.sin(angle) * speed;
        s.length = 40 + random.nextDouble() * 60;
        s.opacity = 1.0;
        s.active = true;
        shootingStars.add(s);
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        Paint bgGradient = new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#070720")),
                new Stop(0.4, Color.web("#0E0E3A")),
                new Stop(0.7, Color.web("#151550")),
                new Stop(1, Color.web("#1A1A5E")));
        gc.setFill(bgGradient);
        gc.fillRect(0, 0, w, h);

        drawNebula(gc, w * 0.15, h * 0.25, 180, Color.web("#4B0082", 0.12));
        drawNebula(gc, w * 0.85, h * 0.75, 220, Color.web("#191970", 0.10));
        drawNebula(gc, w * 0.5, h * 0.5, 150, Color.web("#2E1A47", 0.08));

        double time = startTime / 1e9;
        for (Star star : stars) {
            double brightness = star.baseBrightness;
            if (star.twinkling) {
                double twinkle = 0.5 + 0.5 * Math.sin(time * star.twinkleSpeed + star.phase);
                brightness *= Math.max(0.1, twinkle);
            }
            brightness = Math.min(1.0, Math.max(0.05, brightness));
            Color starColor = star.color.deriveColor(0, 1, 1, brightness);

            if (star.size > 1.8) {
                gc.setEffect(new Glow(0.25));
            }
            gc.setFill(starColor);
            double s = star.size;
            gc.fillOval(star.x - s / 2, star.y - s / 2, s, s);
            gc.setEffect(null);

            if (star.size > 2.0 && brightness > 0.6) {
                gc.setGlobalAlpha(brightness * 0.15);
                gc.setFill(starColor);
                gc.fillOval(star.x - s, star.y - s, s * 2, s * 2);
                gc.setGlobalAlpha(1.0);
            }
        }

        for (ShootingStar s : shootingStars) {
            if (!s.active) continue;
            double speed = Math.sqrt(s.vx * s.vx + s.vy * s.vy);
            double nx = s.vx / speed;
            double ny = s.vy / speed;

            gc.setEffect(new Glow(0.6));
            gc.setGlobalAlpha(s.opacity);

            LinearGradient trail = new LinearGradient(
                    s.x, s.y,
                    s.x - nx * s.length, s.y - ny * s.length,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#E0E0FF", 0.0)),
                    new Stop(0.3, Color.web("#C0C0FF", s.opacity * 0.3)),
                    new Stop(0.7, Color.web("#A0A0FF", s.opacity * 0.6)),
                    new Stop(1, Color.web("#FFFFFF", s.opacity * 0.9)));
            gc.setStroke(trail);
            gc.setLineWidth(2.5);
            gc.strokeLine(s.x, s.y, s.x - nx * s.length, s.y - ny * s.length);

            gc.setFill(Color.web("#FFFFFF", s.opacity));
            gc.fillOval(s.x - 2, s.y - 2, 4, 4);

            gc.setEffect(null);
            gc.setGlobalAlpha(1.0);
        }
    }

    private void drawNebula(GraphicsContext gc, double cx, double cy, double radius, Color color) {
        RadialGradient gradient = new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, color),
                new Stop(0.5, Color.web(color.toString().substring(0, 8), color.getOpacity() * 0.3)),
                new Stop(1, Color.TRANSPARENT));
        gc.setFill(gradient);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    public void stop() {
        running = false;
        timer.stop();
    }
}
