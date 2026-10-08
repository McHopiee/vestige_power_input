package rp.keybinds;

import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tiny helper that scales whatever is drawn between begin() and end().
 * It uses reflection on the GUI matrix stack, so if the game's API ever differs it simply
 * turns itself off (text is then drawn unscaled) instead of failing to compile or crashing.
 */
final class Gfx {
    private static boolean tried, ok;
    private static Method pose, push, pop, translate, scale;

    private Gfx() { }

    static boolean begin(GuiGraphicsExtractor g, float x, float y, float s) {
        if (!tried) {
            tried = true;
            try {
                pose = g.getClass().getMethod("pose");
                Object st = pose.invoke(g);
                Class<?> c = st.getClass();
                push = c.getMethod("pushMatrix");
                pop = c.getMethod("popMatrix");
                translate = c.getMethod("translate", float.class, float.class);
                scale = c.getMethod("scale", float.class, float.class);
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }
        }
        if (!ok) return false;
        try {
            Object st = pose.invoke(g);
            push.invoke(st);
            try {
                translate.invoke(st, x, y);
                scale.invoke(st, s, s);
            } catch (Throwable t) {
                pop.invoke(st);
                throw t;
            }
            return true;
        } catch (Throwable t) {
            ok = false;
            return false;
        }
    }

    static void end(GuiGraphicsExtractor g) {
        try {
            pop.invoke(pose.invoke(g));
        } catch (Throwable ignored) { }
    }
}
