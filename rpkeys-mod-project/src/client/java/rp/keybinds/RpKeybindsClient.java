package rp.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client side of the SVM Powers roleplay layer.
 *
 *  - "Open Ability Wheel" key (default B) asks the server for your spell list (trigger svm_rp.cast set 2000).
 *    The datapack answers with a hidden chat line that starts with U+E0FF "RPW|"; we swallow it and open the wheel.
 *  - "Cast Spell 1..9" keys (unbound by default) cast the Nth spell of your power (trigger svm_rp.cast set 1001..1009).
 */
public class RpKeybindsClient implements ClientModInitializer {
    private static final int SLOTS = 9;
    static final String PREFIX = "RPW|";
    private final KeyMapping[] slotKeys = new KeyMapping[SLOTS];
    private KeyMapping wheelKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rpkeys", "magic"));
        wheelKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.rpkeys.wheel", InputConstants.KEY_B, cat));
        for (int i = 0; i < SLOTS; i++) {
            slotKeys[i] = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.rpkeys.slot" + (i + 1), InputConstants.UNKNOWN.getValue(), cat));
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.getConnection() == null) return;
            while (wheelKey.consumeClick()) sendTrigger(mc, 2000);
            for (int i = 0; i < SLOTS; i++) {
                while (slotKeys[i].consumeClick()) sendTrigger(mc, 1001 + i);
            }
        });

        // Swallow the datapack's hidden spell-list message and open the wheel instead.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String s = message.getString();
            if (!s.startsWith(PREFIX)) return true;
            Minecraft mc = Minecraft.getInstance();
            String body = s.substring(PREFIX.length());
            mc.execute(() -> mc.setScreen(WheelScreen.fromPayload(body)));
            return false;
        });
    }

    static void sendTrigger(Minecraft mc, int value) {
        if (mc.getConnection() != null) mc.getConnection().sendCommand("trigger svm_rp.cast set " + value);
    }
}
