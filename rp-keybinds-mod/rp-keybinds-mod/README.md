# RP Ability Keybinds (client-only Fabric mod) - SOURCE ONLY, UNTESTED
Build: copy the Fabric example mod / template generator output for Minecraft 26.2 (https://fabricmc.net/develop/template/),
then drop `src/main/java/rp/keybinds/RpKeybindsClient.java` and the two resource files into it, set the mod id to `rpkeys`, run `gradlew build`.
The jar in build/libs goes in players' mods folder (client only, the server does not need it).
If a class name does not compile, adjust it to the current 26.2 Fabric API (only KeyMapping category/registration names tend to change).
