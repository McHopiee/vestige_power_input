package rp.keybinds;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A ring of buttons, one per spell, around a centre button. Locked spells are blank and disabled.
 * Click a spell to cast it. Esc closes. More than 8 spells are split over pages (centre button turns the page).
 */
public class WheelScreen extends Screen {
    record Entry(int id, String name, boolean unlocked) {}

    private static final int PER_PAGE = 8;
    private final String power;
    private final List<Entry> entries;
    private int page = 0;

    public WheelScreen(String power, List<Entry> entries) {
        super(Component.literal(power));
        this.power = power;
        this.entries = entries;
    }

    /** Payload looks like: Power|1:Name:1;2:Name:0;... */
    static WheelScreen fromPayload(String payload) {
        String[] top = payload.split("\\|", 2);
        String power = top[0];
        List<Entry> list = new ArrayList<>();
        if (top.length > 1) {
            for (String part : top[1].split(";")) {
                String[] f = part.split(":");
                if (f.length < 3) continue;
                try {
                    list.add(new Entry(Integer.parseInt(f[0].trim()), f[1], f[2].trim().equals("1")));
                } catch (NumberFormatException ignored) { }
            }
        }
        return new WheelScreen(power, list);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = 0;
        int start = page * PER_PAGE;
        int count = Math.min(PER_PAGE, entries.size() - start);

        int bw = 104, bh = 20;
        int rx = Math.min(130, Math.max(70, this.width / 2 - bw / 2 - 10));
        int ry = Math.min(80, Math.max(40, this.height / 2 - bh - 10));

        for (int i = 0; i < count; i++) {
            Entry e = entries.get(start + i);
            double ang = -Math.PI / 2 + (2 * Math.PI * i) / Math.max(1, count);
            int x = cx + (int) Math.round(Math.cos(ang) * rx) - bw / 2;
            int y = cy + (int) Math.round(Math.sin(ang) * ry) - bh / 2;
            Component label = e.unlocked() ? Component.literal(e.name()) : Component.literal(" ");
            Button b = Button.builder(label, btn -> {
                RpKeybindsClient.sendTrigger(Minecraft.getInstance(), e.id());
                this.onClose();
            }).bounds(x, y, bw, bh).build();
            b.active = e.unlocked();
            this.addRenderableWidget(b);
        }

        Component centre = pages > 1
            ? Component.literal(power + "  " + (page + 1) + "/" + pages)
            : Component.literal(power);
        Button mid = Button.builder(centre, btn -> {
            page = (page + 1) % pages;
            this.rebuildWidgets();
        }).bounds(cx - 50, cy - 10, 100, 20).build();
        mid.active = pages > 1;
        this.addRenderableWidget(mid);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
