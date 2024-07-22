package me.darkolythe.shulkerpacks;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class ConfigHandler {

    static void loadConfig(ShulkerPacks main) {
        main.reloadConfig();
        FileConfiguration config = main.getConfig();

        main.saveDefaultConfig();
        main.canOpenInChests = config.getBoolean("canOpenInChests");
        main.canOpenInEnderChest = config.getBoolean("canOpenInEnderChest", true);
        main.canOpenInBarrels = config.getBoolean("canOpenInBarrels", true);
        main.canOpenInInventory = config.getBoolean("canOpenInInventory", true);
        main.canPlaceShulker = config.getBoolean("canPlaceShulker", true);
        main.blacklist = config.getStringList("blacklistedInventories");
        main.canOpenInAir = config.getBoolean("canOpenInAir", true);
        main.allowMultiplePlayers = config.getBoolean("allowMultiplePlayers", true);
        main.openPreviousInv = config.getBoolean("openPreviousInventory", false);
        main.volume = (float) config.getDouble("shulkerVolume", 1.0);
        main.shulkerOpenSound = config.getString("shulkerOpenSound", "minecraft:entity.shulker.open");
        main.shulkerCloseSound = config.getString("shulkerCloseSound", "minecraft:entity.shulker.close");
        main.pvpTimerEnabled = config.getBoolean("disableInCombat", true);
        if (config.getString("defaultName") != null) {
            main.defaultName = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(config.getString("defaultName")));
        }
        main.shiftClickToOpen = config.getBoolean("shiftClickToOpen");
    }
}
