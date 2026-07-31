package de.jeff_media.chestsort.hooks;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.gui.page.ChestPage;
import us.lynuxcraft.deadsilenceiv.advancedchests.utils.inventory.InteractiveInventory;

public class AdvancedChestsHook {

    private final ChestSortPlugin plugin;

    public AdvancedChestsHook(ChestSortPlugin plugin) {
        this.plugin = plugin;
        if (plugin.isHookAdvancedChests()
                && plugin.getServer().getPluginManager().isPluginEnabled("AdvancedChests")) {
            plugin.getLogger().info("Successfully hooked into AdvancedChests");
        } else {
            plugin.setHookAdvancedChests(false);
        }
    }

    public boolean isAnAdvancedChest(Inventory inventory) {
        try {
            return plugin.isHookAdvancedChests()
                    && inventory != null
                    && AdvancedChestsAPI.getInventoryManager().getAdvancedChest(inventory) != null;
        } catch (Exception | LinkageError ignored) {
            return false;
        }
    }

    public boolean handleAChestSortingIfPresent(Inventory inventory) {
        if (!plugin.isHookAdvancedChests()) return false;
        try {
            InteractiveInventory interactiveInventory =
                    AdvancedChestsAPI.getInventoryManager().getInteractiveByBukkit(inventory);
            if (interactiveInventory == null) {
                return false;
            }
            if (interactiveInventory instanceof ChestPage) {
                plugin.getOrganizer().sortInventory(inventory, 0, inventory.getSize() - 10);
            }
            return true;
        } catch (Exception | LinkageError ignored) {
            return false;
        }
    }

    public boolean handleAChestSortingIfPresent(Location location) {
        if (!plugin.isHookAdvancedChests()) return false;
        try {
            AdvancedChest<?, ?> chest = AdvancedChestsAPI.getChestManager().getAdvancedChest(location);
            if (chest == null) {
                return false;
            }
            for (ChestPage<?> page : chest.getPages().values()) {
                Inventory inventory = page.getBukkitInventory();
                plugin.getOrganizer().sortInventory(inventory, 0, inventory.getSize() - 10);
            }
            return true;
        } catch (Exception | LinkageError ignored) {
            return false;
        }
    }
}
