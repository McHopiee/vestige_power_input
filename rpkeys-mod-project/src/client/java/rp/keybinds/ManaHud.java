package rp.keybinds;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Small mana bar to the left of the hotbar. It fades in whenever your mana changes
 * (casting, regenerating) and fades away a few seconds after it stops changing.
 * The datapack sends hidden lines:  RPM|current|max
 */
final class ManaHud {
    private static final long VISIBLE = 3500, FADE = 700, GHOST = 900;
    private static int cur = -1, max = 0, prev = 0;
    private static long tChange = 0, tDrop = 0;

    private ManaHud() { }

    static void handle(String body) {
        String[] f = body.split("\\|", -1);
        if (f.length < 2) return;
        try {
            int c = Integer.parseInt(f[0].trim());
            int m = Integer.parseInt(f[1].trim());
            long now = System.currentTimeMillis();
            if (cur >= 0 && c < cur) { prev = cur; tDrop = now; }
            if (c != cur || m != max) tChange = now;
            cur = c;
            max = m;
        } catch (NumberFormatException ignored) { }
    }

    /** Called when a spell key is pressed so the bar is visible while casting. */
    static void poke() {
        tChange = System.currentTimeMillis();
    }

    static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (max <= 0 || cur < 0) return;
        long now = System.currentTimeMillis();
        long age = now - tChange;
        float a = age < VISIBLE ? 1f : Math.max(0f, 1f - (age - VISIBLE) / (float) FADE);
        if (a <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int w = 88, h = 11;
        int x = sw / 2 - 91 - 34 - w;      // clear of the hotbar and the off-hand slot
        if (x < 4) x = 4;
        int y = sh - 17;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(a * 0.9f, 0x0A0A12));
        g.fill(x, y, x + w, y + h, argb(a * 0.75f, 0x16202C));
        int fw = (int) Math.round(w * Math.max(0, Math.min(cur, max)) / (double) max);
        long dropAge = now - tDrop;
        if (dropAge < GHOST && prev > cur) {
            int gw = (int) Math.round(w * Math.max(0, Math.min(prev, max)) / (double) max);
            float ga = a * (1f - dropAge / (float) GHOST);
            if (gw > fw) g.fill(x + fw, y, x + gw, y + h, argb(ga, 0xFF5A5A));
        }
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, argb(a, 0x2B7FE0));
            g.fill(x, y, x + fw, y + 2, argb(a, 0x66B6FF));
        }
        String label = "Mana " + cur + "/" + max;
        int tw = font.width(label);
        int ta = Math.round(a * 255f);
        if (ta >= 6) g.text(font, label, x + (w - tw) / 2, y + 2, (ta << 24) | 0xFFFFFF, true);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
