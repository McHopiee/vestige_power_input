package rp.keybinds;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Radial spell wheel: a ring of pie slices, one per spell, around a centre disc.
 * Locked spells are blank. Hover highlights a slice, left click casts it. Esc closes.
 * More than 8 spells are split over pages; click the centre disc to turn the page.
 */
public class WheelScreen extends Screen {
    record Entry(int id, String name, boolean unlocked) {}

    private static final int PER_PAGE = 8;
    private static final int CENTER = -2;
    private static final int NONE = -1;

    private final String power;
    private final List<Entry> entries;
    private int page = 0;
    private int pages = 1;

    private int cx, cy, rOuter, rInner, rCenter;
    private int n, start;
    private final List<List<int[]>> runs = new ArrayList<>();
    private final List<int[]> centerRuns = new ArrayList<>();

    public WheelScreen(String power, List<Entry> entries) {
        super(Component.literal(power));
        this.power = power;
        this.entries = entries;
    }

    /** Payload looks like: Power|1:Name:1;2:Name:0;...  (parsed leniently: stray quotes/separators are ignored) */
    static WheelScreen fromPayload(String payload) {
        int bar = payload.indexOf('|');
        String power = (bar < 0 ? payload : payload.substring(0, bar)).replace("\"", "").trim();
        String rest = bar < 0 ? "" : payload.substring(bar + 1);
        List<Entry> list = new ArrayList<>();
        Matcher m = ENTRY.matcher(rest);
        while (m.find()) {
            try {
                list.add(new Entry(Integer.parseInt(m.group(1)), m.group(2).trim(), m.group(3).equals("1")));
            } catch (NumberFormatException ignored) { }
        }
        return new WheelScreen(power, list);
    }

    private static final Pattern ENTRY = Pattern.compile("(\\d+):([^:;|]+):([01])");

    @Override
    protected void init() {
        cx = this.width / 2;
        cy = this.height / 2;
        rOuter = Math.max(50, Math.min(76, (int) (Math.min(this.width, this.height) * 0.28)));
        rInner = (int) (rOuter * 0.48);
        rCenter = rInner - 3;

        pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = 0;
        start = page * PER_PAGE;
        n = Math.max(1, Math.min(PER_PAGE, entries.size() - start));
        if (entries.isEmpty()) n = 1;

        // Pre-compute horizontal pixel runs for every slice so drawing is just a few fills per row.
        runs.clear();
        centerRuns.clear();
        for (int i = 0; i < n; i++) runs.add(new ArrayList<>());
        for (int y = -rOuter; y <= rOuter; y++) {
            int runStart = 0;
            int runClass = -3; // "no run yet"
            for (int x = -rOuter; x <= rOuter + 1; x++) {
                int c = (x > rOuter) ? NONE : classify(x, y);
                if (c != runClass) {
                    if (runClass >= 0 || runClass == CENTER) addRun(runClass, y, runStart, x);
                    runStart = x;
                    runClass = c;
                }
            }
        }
    }

    private void addRun(int cls, int y, int x0, int x1) {
        int[] run = {y, x0, x1};
        if (cls == CENTER) centerRuns.add(run);
        else if (cls >= 0 && cls < runs.size()) runs.get(cls).add(run);
    }

    /** Which part of the wheel a pixel offset from the centre belongs to. */
    private int classify(int dx, int dy) {
        double px = dx + 0.5, py = dy + 0.5;
        double r = Math.sqrt(px * px + py * py);
        if (r <= rCenter) return CENTER;
        if (r < rInner || r > rOuter) return NONE;
        double sec = 2 * Math.PI / n;
        double rel = Math.atan2(py, px) + Math.PI / 2 + sec / 2;
        rel = ((rel % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        int idx = Math.min(n - 1, (int) (rel / sec));
        double within = rel - idx * sec;
        if (Math.min(within, sec - within) * r < 2.0) return NONE; // gap between slices
        return idx;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int hover = classify(mouseX - cx, mouseY - cy);

        for (int i = 0; i < n; i++) {
            Entry e = slot(i);
            int color;
            if (e == null || !e.unlocked()) color = 0x90101010;
            else if (hover == i) color = 0xFF1E78FF;
            else color = 0xE0404040;
            for (int[] r : runs.get(i)) g.fill(cx + r[1], cy + r[0], cx + r[2], cy + r[0] + 1, color);
        }
        int centerColor = (hover == CENTER && pages > 1) ? 0xFF1E78FF : 0xD0161616;
        for (int[] r : centerRuns) g.fill(cx + r[1], cy + r[0], cx + r[2], cy + r[0] + 1, centerColor);

        // Slice labels: only unlocked spells show their name.
        double sec = 2 * Math.PI / n;
        double rm = (rInner + rOuter) / 2.0;
        int labelW = (int) Math.max(30, Math.min(70, 2 * rm * Math.sin(Math.PI / Math.max(2, n)) * 0.9));
        for (int i = 0; i < n; i++) {
            Entry e = slot(i);
            if (e == null || !e.unlocked()) continue;
            double a = -Math.PI / 2 + i * sec;
            drawFit(g, e.name(), cx + (int) Math.round(Math.cos(a) * rm), cy + (int) Math.round(Math.sin(a) * rm), labelW);
        }

        // Centre disc: power name (and page number when there are several pages).
        int centreW = Math.max(30, rCenter * 2 - 8);
        drawFit(g, power, cx, pages > 1 ? cy - 6 : cy, centreW);
        if (pages > 1) drawFit(g, (page + 1) + " / " + pages, cx, cy + 9, centreW);
    }

    private Entry slot(int i) {
        int k = start + i;
        return (k >= 0 && k < entries.size()) ? entries.get(k) : null;
    }

    /** Draws text centred on (x, y): wraps onto up to three lines and shrinks to fit maxW. */
    private void drawFit(GuiGraphicsExtractor g, String text, int x, int y, int maxW) {
        List<String> lines = wrap(text, maxW);
        int widest = 1;
        for (String l : lines) widest = Math.max(widest, this.font.width(l));
        float scale = widest > maxW ? Math.max(0.5f, maxW / (float) widest) : 1f;
        int lh = this.font.lineHeight;
        float top = y - (lines.size() * lh * scale) / 2f;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            int w = this.font.width(l);
            float ly = top + i * lh * scale;
            if (Gfx.begin(g, x, ly, scale)) {
                g.text(this.font, l, -w / 2, 0, 0xFFFFFFFF, true);
                Gfx.end(g);
            } else {
                g.text(this.font, l, x - w / 2, Math.round(ly), 0xFFFFFFFF, true);
            }
        }
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        String cur = "";
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            String t = cur.isEmpty() ? word : cur + " " + word;
            if (cur.isEmpty() || this.font.width(t) <= maxW) cur = t;
            else { out.add(cur); cur = word; }
        }
        if (!cur.isEmpty()) out.add(cur);
        while (out.size() > 3) {                     // merge overflow into the last line
            String last = out.remove(out.size() - 1);
            out.set(out.size() - 1, out.get(out.size() - 1) + " " + last);
        }
        if (out.isEmpty()) out.add(text);
        return out;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int h = classify((int) Math.floor(event.x()) - cx, (int) Math.floor(event.y()) - cy);
            if (h >= 0 && h < n) {
                Entry e = slot(h);
                if (e != null && e.unlocked()) {
                    RpKeybindsClient.sendTrigger(Minecraft.getInstance(), e.id());
                    this.onClose();
                }
                return true;
            }
            if (h == CENTER && pages > 1) {
                page = (page + 1) % pages;
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
