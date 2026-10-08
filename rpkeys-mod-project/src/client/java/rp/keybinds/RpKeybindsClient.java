package rp.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Adds 9 "Cast Slot" keys + 1 "Ability Wheel" key to Options > Controls.
 * Each key just sends a /trigger command that the svm_rp datapack layer understands:
 *   trigger svm_rp.cast set 1001..1009  -> cast the ability bound to hotbar-slot 1..9
 *   trigger svm_rp.cast set 2000        -> open the ability wheel
 * NOTE: written against Minecraft 26.x Mojang names but NOT compiled/tested. If the compiler complains
 * about a class/method name, check the current Fabric example mod for 26.2 (KeyMapping category + helper names change often).
 */
public class RpKeybindsClient implements ClientModInitializer {
    private static final int SLOTS = 9;
    private final KeyMapping[] slotKeys = new KeyMapping[SLOTS];
    private KeyMapping wheelKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rpkeys", "magic"));
        for (int i = 0; i < SLOTS; i++) {
            slotKeys[i] = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.rpkeys.slot" + (i + 1), InputConstants.UNKNOWN.getValue(), cat));
        }
        wheelKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.rpkeys.wheel", InputConstants.UNKNOWN.getValue(), cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.getConnection() == null) return;
            for (int i = 0; i < SLOTS; i++) {
                while (slotKeys[i].consumeClick()) send(mc, 1001 + i);
            }
            while (wheelKey.consumeClick()) send(mc, 2000);
        });
    }

    private static void send(Minecraft mc, int value) {
        mc.getConnection().sendCommand("trigger svm_rp.cast set " + value);
    }
}
