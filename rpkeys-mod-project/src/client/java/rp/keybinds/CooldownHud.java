package rp.keybinds;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * "Spell on cooldown" notice: shows the spell name and the time left (counting down live) with a small
 * bar, for a few moments, then fades. The datapack sends hidden lines:  RPC|spell name|ticks left
 */
final class CooldownHud {
    private static final long SHOW_MAX = 3200, FADE = 500;

    private static String name = "";
    private static long startMs = -1, remainMs = 0, totalMs = 1;

    private CooldownHud() { }

    static void handle(String body) {
        int bar = body.lastIndexOf('|');
        if (bar < 0) return;
        String n = body.substring(0, bar).trim();
        long ticks;
        try { ticks = Long.parseLong(body.substring(bar + 1).trim()); }
        catch (NumberFormatException e) { return; }
        if (ticks <= 0) return;
        long now = System.currentTimeMillis();
        long ms = ticks * 50L;
        boolean same = n.equals(name) && startMs >= 0 && now - startMs < SHOW_MAX + FADE + 6000;
        totalMs = same ? Math.max(totalMs, ms) : ms;   // bar is relative to the longest time seen for this spell
        name = n;
        remainMs = ms;
        startMs = now;
    }

    static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (startMs < 0) return;
        long now = System.currentTimeMillis();
        long age = now - startMs;
        long visible = Math.min(SHOW_MAX, remainMs);          // stop early if the cooldown ends sooner
        if (age > visible + FADE) { startMs = -1; return; }
        float a = age <= visible ? 1f : 1f - (age - visible) / (float) FADE;
        if (age < 120) a *= age / 120f;                        // quick fade-in
        long left = Math.max(0, remainMs - age);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        String time = String.format(java.util.Locale.ROOT, "%.1fs", left / 1000.0);
        String line = name + "  -  " + time;
        int tw = font.width(line);
        int w = Math.max(110, tw + 16), h = 26;
        int x = (sw - w) / 2, y = (int) (sh * 0.62f);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(a * 0.85f, 0x0A0A12));
        g.fill(x, y, x + w, y + h, argb(a * 0.70f, 0x1A1216));
        int ta = Math.round(a * 255f);
        if (ta >= 6) {
            g.text(font, line, x + (w - tw) / 2, y + 4, (ta << 24) | 0xFFDDDD, true);
        }
        int bx = x + 6, bw = w - 12, by = y + h - 9;
        g.fill(bx, by, bx + bw, by + 4, argb(a * 0.8f, 0x2A2A32));
        int fw = (int) Math.round(bw * Math.min(1.0, left / (double) Math.max(1, totalMs)));
        if (fw > 0) g.fill(bx, by, bx + fw, by + 4, argb(a, 0xE05A5A));
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
