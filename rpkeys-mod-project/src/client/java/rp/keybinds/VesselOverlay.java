package rp.keybinds;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * The "You have been chosen to be the ... VOID VESSEL" intro.
 * Small line near the top first; after a moment the big line appears underneath it
 * (the line above stays); finally both fade out together.
 *
 * Driven by hidden chat lines from the datapack:
 *   RPV|1|small text           show the small line
 *   RPV|2|BIG TEXT|rrggbb      show the big line below it
 *   RPV|3                      fade both out
 */
final class VesselOverlay {
    private static final long FADE_IN = 800, FADE_OUT = 1600, HARD_LIMIT = 20000;

    private static String small = "", big = "";
    private static int bigRgb = 0xFFFFFF;
    private static long tSmall = -1, tBig = -1, tOut = -1;

    private VesselOverlay() { }

    static void handle(String body) {
        String[] f = body.split("\\|", -1);
        String kind = f.length > 0 ? f[0] : "";
        long now = System.currentTimeMillis();
        if (kind.equals("1") && f.length > 1) {
            small = f[1]; big = ""; bigRgb = 0xFFFFFF;
            tSmall = now; tBig = -1; tOut = -1;
        } else if (kind.equals("2") && f.length > 1) {
            big = f[1];
            if (f.length > 2) {
                try { bigRgb = Integer.parseInt(f[2].trim().replace("#", ""), 16) & 0xFFFFFF; }
                catch (NumberFormatException ignored) { bigRgb = 0xFFFFFF; }
            }
            if (tSmall < 0) { tSmall = now; }
            tBig = now; tOut = -1;
        } else if (kind.equals("3")) {
            if (tSmall >= 0 && tOut < 0) tOut = now;
        }
    }

    private static float fade(long start, long now, long dur) {
        if (start < 0) return 0f;
        return Math.max(0f, Math.min(1f, (now - start) / (float) dur));
    }

    static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (tSmall < 0) return;
        long now = System.currentTimeMillis();
        if (now - tSmall > HARD_LIMIT || (tOut >= 0 && now - tOut > FADE_OUT)) {
            tSmall = tBig = tOut = -1;
            return;
        }
        float out = tOut < 0 ? 1f : 1f - fade(tOut, now, FADE_OUT);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = sw / 2;

        // small line (near the top)
        float a1 = fade(tSmall, now, FADE_IN) * out;
        Component c1 = Component.literal(small);
        int w1 = Math.max(1, font.width(c1));
        float s1 = Math.min(1.7f, sw * 0.9f / w1);
        int y1 = Math.max(12, (int) (sh * 0.16f));
        draw(g, font, c1, cx, y1, s1, 0xE8E8E8, a1);

        // big line, below the small one
        if (tBig >= 0 && !big.isEmpty()) {
            float a2 = fade(tBig, now, FADE_IN) * out;
            Component c2 = Component.literal(big).setStyle(Style.EMPTY.withBold(true));
            int w2 = Math.max(1, font.width(c2));
            float s2 = Math.min(4f, sw * 0.9f / w2);
            int y2 = y1 + (int) (8 * s1) + 8 + (int) ((1f - fade(tBig, now, FADE_IN)) * 6);
            draw(g, font, c2, cx, y2, s2, bigRgb, a2);
        }
    }

    private static void draw(GuiGraphicsExtractor g, Font font, Component c, int cx, int y, float scale, int rgb, float alpha) {
        int a = Math.round(alpha * 255f);
        if (a < 6) return;                       // the game treats very low alpha as invisible/opaque
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        int w = font.width(c);
        if (Gfx.begin(g, cx, y, scale)) {
            g.text(font, c, -w / 2, 0, argb, true);
            Gfx.end(g);
        } else {
            g.text(font, c, cx - w / 2, y, argb, true);
        }
    }
}
