package rp.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Client side of the SVM Powers roleplay layer.
 *
 *  - "Open Ability Wheel" key (default G) asks the server for your spell list (trigger svm_rp.cast set 2000).
 *    The datapack answers with a hidden chat line that starts with U+E0FF "RPW|"; we swallow it and open the wheel.
 *  - "Cast Spell 1..9" keys (unbound by default) cast the Nth spell of your power (trigger svm_rp.cast set 1001..1009).
 */
public class RpKeybindsClient implements ClientModInitializer {
    private static final int SLOTS = 9;
    static final String PREFIX = "RPW|";
    static final String MANA_PREFIX = "RPM|";
    static final String COOLDOWN_PREFIX = "RPC|";
    static final String VESSEL_PREFIX = "RPV|";
    private final KeyMapping[] slotKeys = new KeyMapping[SLOTS];
    private KeyMapping wheelKey;
    private final boolean[] wasDown = new boolean[SLOTS];
    private final java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    private boolean wheelWasDown;
    private int heldIdx = -1, beat = 0, sinceSend = 99;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rpkeys", "magic"));
        wheelKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.rpkeys.wheel", InputConstants.KEY_G, cat));
        for (int i = 0; i < SLOTS; i++) {
            slotKeys[i] = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.rpkeys.slot" + (i + 1), InputConstants.UNKNOWN.getValue(), cat));
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.getConnection() == null) {
                queue.clear();
                heldIdx = -1;
                java.util.Arrays.fill(wasDown, false);
                wheelWasDown = false;
                return;
            }
            // Wheel key: open once per press (ignore the operating system's key-repeat).
            boolean wheelClicked = false;
            while (wheelKey.consumeClick()) wheelClicked = true;
            boolean wheelDown = wheelKey.isDown();
            if ((wheelDown || wheelClicked) && !wheelWasDown) queue.add(2000);
            wheelWasDown = wheelDown;

            // Spell keys: ONE cast when pressed, then a light "still held" signal while down, and a
            // release signal. Holding the key no longer re-casts every key-repeat.
            for (int i = 0; i < SLOTS; i++) {
                boolean clicked = false;
                while (slotKeys[i].consumeClick()) clicked = true;
                boolean down = slotKeys[i].isDown();
                if (!wasDown[i] && (down || clicked)) {
                    queue.add(1001 + i);
                    ManaHud.poke();
                    if (down) { heldIdx = i; beat = 0; }
                    else queue.add(3002);
                } else if (wasDown[i] && !down) {
                    if (heldIdx == i) { queue.add(3002); heldIdx = -1; }
                }
                wasDown[i] = down;
            }
            if (heldIdx >= 0 && ++beat >= 10) { beat = 0; queue.add(3003); }

            if (queue.size() > 6) queue.clear();
            sinceSend++;
            if (!queue.isEmpty() && sinceSend >= 2) {
                sendTrigger(mc, queue.poll());
                sinceSend = 0;
            }
        });

        // "On cooldown" notice with a live countdown.
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("rpkeys", "cooldown"),
            CooldownHud::extract);

        // On-screen mana bar next to the hotbar.
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("rpkeys", "mana"),
            ManaHud::extract);

        // On-screen vessel intro (small line, then big line below it, then fade).
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("rpkeys", "vessel"),
            VesselOverlay::extract);

        // Swallow the datapack's hidden spell-list message and open the wheel instead.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String s = message.getString();
            // Quiet the vanilla "You cannot trigger this objective yet" line if two key signals ever land in one tick.
            if (s.contains("cannot trigger this objective")) return false;
            if (s.startsWith(COOLDOWN_PREFIX)) {
                String cb = s.substring(COOLDOWN_PREFIX.length());
                Minecraft.getInstance().execute(() -> CooldownHud.handle(cb));
                return false;
            }
            if (s.startsWith(MANA_PREFIX)) {
                String mb = s.substring(MANA_PREFIX.length());
                Minecraft.getInstance().execute(() -> ManaHud.handle(mb));
                return false;
            }
            if (s.startsWith(VESSEL_PREFIX)) {
                String vb = s.substring(VESSEL_PREFIX.length());
                Minecraft.getInstance().execute(() -> VesselOverlay.handle(vb));
                return false;
            }
            if (!s.startsWith(PREFIX)) return true;
            Minecraft mc = Minecraft.getInstance();
            String body = s.substring(PREFIX.length());
            mc.execute(() -> openScreen(mc, WheelScreen.fromPayload(body)));
            return false;
        });
    }

    static void sendTrigger(Minecraft mc, int value) {
        if (mc.getConnection() != null) mc.getConnection().sendCommand("trigger svm_rp.cast set " + value);
    }

    static void openScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }
}
