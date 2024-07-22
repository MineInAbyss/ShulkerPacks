package me.darkolythe.shulkerpacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public final class ShulkerPacks extends JavaPlugin {

    ShulkerListener shulkerlistener;

    private static ShulkerPacks plugin;
    LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacy('§');
    PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    String prefix = ChatColor.WHITE + ChatColor.BOLD.toString() + "[" + ChatColor.BLUE + "ShulkerPacks" + ChatColor.WHITE + ChatColor.BOLD + "] ";

    static Map<Player, ItemStack> openShulkers = new HashMap<>();
    Map<Player, Boolean> fromHand = new HashMap<>();
    Map<Player, Inventory> openInventories = new HashMap<>();
    Map<Player, Inventory> openContainer = new HashMap<>();
    private final Map<Player, Long> pvpTimer = new HashMap<>();
    boolean canOpenInChests = true;
    boolean openPreviousInv = false;
    List<String> blacklist = new ArrayList<>();
    Component defaultName = MiniMessage.miniMessage().deserialize("<blue>Shulker Pack");
    boolean pvpTimerEnabled = false;
    boolean shiftClickToOpen = false;
    boolean canOpenInEnderChest, canOpenInBarrels, canPlaceShulker, canOpenInInventory, canOpenInAir, allowMultiplePlayers;
    float volume;
    String shulkerOpenSound = "minecraft:entity.shulker.open";
    String shulkerCloseSound = "minecraft:entity.shulker.close";

    /*
    Sets up the plugin
     */
    @Override
    public void onEnable() {

        plugin = this;
        shulkerlistener = new ShulkerListener(this);

        getServer().getPluginManager().registerEvents(shulkerlistener, this);

        Objects.requireNonNull(getCommand("shulkerpacks")).setExecutor(new CommandReload());

        ConfigHandler.loadConfig(this);

        shulkerlistener.checkIfValid();

        getLogger().log(Level.INFO, plainSerializer.serialize(legacySerializer.deserialize(prefix + ChatColor.GREEN + "ShulkerPacks has been enabled!")));
    }

    /*
    Doesn't do much. Just says a message
     */
    @Override
    public void onDisable() {
        for (Player player : this.openInventories.keySet()) {
            player.closeInventory();
        }
        getLogger().log(Level.INFO, plainSerializer.serialize(legacySerializer.deserialize(prefix + ChatColor.RED + "ShulkerPacks has been disabled!")));
    }


    public static ShulkerPacks getInstance() {
        return plugin;
    }


    public boolean getPvpTimer(Player player) {
        if (pvpTimer.containsKey(player)) {
            return System.currentTimeMillis() - pvpTimer.get(player) < 7000;
        }
        return false;
    }

    public void setPvpTimer(Player player) {
        if (pvpTimerEnabled) {
            pvpTimer.put(player, System.currentTimeMillis());
        }
    }
}
