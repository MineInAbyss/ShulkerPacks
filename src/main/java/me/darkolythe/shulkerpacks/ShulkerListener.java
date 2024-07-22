package me.darkolythe.shulkerpacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShulkerListener implements Listener {

    public ShulkerPacks main;

    public ShulkerListener(ShulkerPacks plugin) {
        this.main = plugin; //set it equal to an instance of main
    }

    /*
    Saves the shulker on inventory drag if its open
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                if (!saveShulker(player, event.getView().title())) {
                    event.setCancelled(true);
                }
            }, 1);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        List<Player> closeInventories = new ArrayList<>();
        for (Player p : ShulkerPacks.openShulkers.keySet()) {
            if (ShulkerPacks.openShulkers.get(p).equals(event.getItem())) {
                closeInventories.add(p);
            }
        }
        for (Player p : closeInventories) {
            if (event.getInitiator().getLocation() != null && event.getInitiator().getLocation().getWorld() == p.getWorld()) {
                if (event.getInitiator().getLocation().distance(p.getLocation()) < 6) {
                    p.closeInventory();
                }
            }
        }
    }

    /*
    Opens the shulker if its not in a weird inventory, then saves it
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (ShulkerPacks.openShulkers.containsKey(player)) {
            if (ShulkerPacks.openShulkers.get(player).getType() == Material.AIR) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
        }

        if (checkIfOpen(event.getCurrentItem())) { //cancels the event if the player is trying to remove an open shulker
            if (event.getClick() != ClickType.RIGHT) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() != null) {
            if (event.getCurrentItem() != null && (ShulkerPacks.openShulkers.containsKey(player) && event.getCurrentItem().equals(ShulkerPacks.openShulkers.get(player)))) {
                event.setCancelled(true);
                return;
            }

            // prevent the player from opening it in a chest if they have no permission
            if (event.getClickedInventory() != null && (event.getClickedInventory().getType() == InventoryType.CHEST)) {
                if (!main.canOpenInChests || !player.hasPermission("shulkerpacks.open_in_chests")) {
                    return;
                }
            }

            // prevent the player from opening the shulkerbox in inventories without storage slots
            String typeStr = event.getClickedInventory().getType().toString();
            InventoryType type = event.getClickedInventory().getType();
            if (typeStr.equals("WORKBENCH") || typeStr.equals("ANVIL") || typeStr.equals("BEACON") || typeStr.equals("MERCHANT") || typeStr.equals("ENCHANTING") ||
                    typeStr.equals("GRINDSTONE") || typeStr.equals("CARTOGRAPHY") || typeStr.equals("LOOM") || typeStr.equals("STONECUTTER")) {
                return;
            }

            // prevent the player from opening it in the crafting slots of their inventory
            if (type == InventoryType.CRAFTING && event.getRawSlot() >= 1 && event.getRawSlot() <= 4) {
                return;
            }

            // prevent the player from opening it in the inventory if they have no permission
            if ((player.getInventory() == event.getClickedInventory())) {
                if (!main.canOpenInInventory || !player.hasPermission("shulkerpacks.open_in_inventory")) {
                    return;
                }
            }

            // prevent the player from opening the shulkerbox in the result slot of an inventory (this can be done with dyes)
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                return;
            }

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() != null && event.getClickedInventory().getHolder().getClass().toString().endsWith(".CraftBarrel") && !main.canOpenInBarrels) {
                return;
            }

            if (!main.canOpenInEnderChest && type == InventoryType.ENDER_CHEST) {
                return;
            }

            for (String str : main.blacklist) {
                PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
                LegacyComponentSerializer legacy = LegacyComponentSerializer.legacy('§');
                if (plainText.serialize(player.getOpenInventory().title()).equals(plainText.serialize(legacy.deserialize(str)))) {
                    return;
                }
            }

            if (!main.shiftClickToOpen || event.isShiftClick()) {
                boolean isCancelled = event.isCancelled();
                event.setCancelled(true);
                if (event.isRightClick() && openInventoryIfShulker(event.getCurrentItem(), player)) {
                    main.fromHand.remove(player);
                    return;
                }
                event.setCancelled(isCancelled);
            }

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                if (!saveShulker(player, event.getView().title())) {
                    event.setCancelled(true);
                }
            }, 1);
        }
    }

    // Deals with multiple people opening the same shulker
    private static boolean checkIfOpen(ItemStack shulker) {
        for (ItemStack i : ShulkerPacks.openShulkers.values())
            if (i.isSimilar(shulker)) return true;
        return false;
    }

    /*
    Saves the shulker if its open, then removes the current open shulker from the player data
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (saveShulker(player, player.getOpenInventory().title())) {
                if (main.openPreviousInv) openPreviousInventory(player);
            }
            ShulkerPacks.openShulkers.remove(player);
        }
    }


    private void openPreviousInventory(Player player) {
        InventoryType type = main.openContainer.get(player).getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.SHULKER_BOX) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                player.openInventory(main.openContainer.get(player));
                main.openContainer.remove(player);
            }, 1);
        }
    }


    /*
    Opens the shulker if the air was clicked with one
     */
    @EventHandler
    public void onClickAir(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!main.canOpenInAir || event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if ((main.shiftClickToOpen && !player.isSneaking())) return;
        if (!main.canOpenInAir || !player.hasPermission("shulkerpacks.open_in_air")) return;

        openInventoryIfShulker(event.getItem(), player);
        main.fromHand.put(player, true);
    }
    @EventHandler
    public void PlayerInteractEvent(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        ItemStack itemStack= event.getItem();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        if (block != null && Tag.SHULKER_BOXES.isTagged(block.getType()) && !main.canPlaceShulker) {
            event.setCancelled(true);
        } else if (itemStack != null && Tag.SHULKER_BOXES.isTagged(itemStack.getType()) && !main.canPlaceShulker) {
            event.setCancelled(true);
            openInventoryIfShulker(event.getItem() , event.getPlayer());
        }
    }

    @EventHandler
    public void onPlaceInteractShulker(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        ItemStack itemStack= event.getItem();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (block != null && Tag.SHULKER_BOXES.isTagged(block.getType()) && !main.canPlaceShulker)
            event.setCancelled(true);

        if (itemStack != null && Tag.SHULKER_BOXES.isTagged(itemStack.getType()) && !main.canPlaceShulker) {
            event.setCancelled(true);
            openInventoryIfShulker(itemStack, event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player) {
            main.setPvpTimer(damager);
            main.setPvpTimer(player);
        }
    }

    @EventHandler
    public void onPlayerShoot(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player hitEntity && event.getEntity().getShooter() instanceof Player shooter) {
            main.setPvpTimer(shooter);
            main.setPvpTimer(hitEntity);
        }
    }

    /*
    Saves the shulker data in the itemmeta
     */
    public boolean saveShulker(Player player, Component title) {
        try {
            if (ShulkerPacks.openShulkers.containsKey(player)) {
                if (title.equals(main.defaultName) || (ShulkerPacks.openShulkers.get(player).hasItemMeta() &&
                        ShulkerPacks.openShulkers.get(player).getItemMeta().hasDisplayName() &&
                        (Objects.equals(ShulkerPacks.openShulkers.get(player).getItemMeta().displayName(), title)))) {
                    ItemStack item = ShulkerPacks.openShulkers.get(player);
                    if (item != null) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                        shulker.getInventory().setContents(main.openInventories.get(player).getContents());
                        meta.setBlockState(shulker);
                        item.setItemMeta(meta);
                        ShulkerPacks.openShulkers.put(player, item);
                        updateAllInventories(ShulkerPacks.openShulkers.get(player));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            ShulkerPacks.openShulkers.remove(player);
            player.closeInventory();
            return false;
        }
        return false;
    }

    private void updateAllInventories(ItemStack item) {
        for (Player p : ShulkerPacks.openShulkers.keySet()) {
            if (ShulkerPacks.openShulkers.get(p).equals(item)) {
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                p.getOpenInventory().getTopInventory().setContents(shulker.getInventory().getContents());
                p.updateInventory();
            }
        }
    }

    /*
    Opens the shulker inventory with the contents of the shulker
     */
    public boolean openInventoryIfShulker(ItemStack item, Player player) {
        if (player.hasPermission("shulkerpacks.use")) {
            if (item != null) {
                if (item.getAmount() == 1 && item.getType().toString().contains("SHULKER")) {

                    if (main.getPvpTimer(player)) {
                        player.sendMessage(main.prefix + ChatColor.RED + "You cannot open shulkerboxes in combat!");
                        return false;
                    }

                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        if (meta != null && meta.getBlockState() instanceof ShulkerBox) {
                            ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                            Inventory inv;
                            if (meta.hasDisplayName()) {
                                inv = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, Objects.requireNonNull(meta.displayName()));
                            } else {
                                inv = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, main.defaultName);
                            }
                            inv.setContents(shulker.getInventory().getContents());

                            main.openContainer.put(player, player.getOpenInventory().getTopInventory());

                            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                                player.openInventory(inv);
                                player.playSound(player.getLocation(), main.shulkerOpenSound, main.volume, 1);
                                ShulkerPacks.openShulkers.put(player, item);
                                main.openInventories.put(player, player.getOpenInventory().getTopInventory());
                            }, 1);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void checkIfValid() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(main, () -> {
            for (Player p : ShulkerPacks.openShulkers.keySet()) {
                if (ShulkerPacks.openShulkers.get(p).getType() == Material.AIR) {
                    p.closeInventory();
                }
                if (main.openContainer.containsKey(p)) {
                    Location loc = main.openContainer.get(p).getLocation();
                    if (loc != null && loc.getWorld() == p.getWorld()) {
                        if (loc.distance(p.getLocation()) > 6) {
                            p.closeInventory();
                        }
                    }
                }
            }
        }, 1L, 1L);
    }
}
