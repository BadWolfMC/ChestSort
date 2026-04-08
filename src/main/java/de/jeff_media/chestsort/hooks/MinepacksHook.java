package de.jeff_media.chestsort.hooks;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class MinepacksHook {

    final ChestSortPlugin plugin;
    Plugin minepacks = null;
    Method isBackpackItemMethod = null;
    Class<?> backpackClass = null;

    public MinepacksHook(ChestSortPlugin plugin) {
        this.plugin = plugin;
        Plugin bukkitPlugin = Bukkit.getPluginManager().getPlugin("Minepacks");
        if (plugin.isHookMinepacks() && bukkitPlugin != null) {
            try {
                Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.MinepacksPlugin");
                backpackClass = Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack");
                minepacks = bukkitPlugin;
                try {
                    isBackpackItemMethod = minepacks.getClass().getMethod("isBackpackItem", ItemStack.class);
                } catch (NoSuchMethodException e) {
                    plugin.getLogger().warning("Minepacks version too old; hook disabled.");
                    plugin.setHookMinepacks(false);
                }
                if (minepacks != null) {
                    plugin.getLogger().info("Successfully hooked into Minepacks");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("Minepacks API classes not found; hook disabled.");
                plugin.setHookMinepacks(false);
            }
        }
    }

    public boolean isMinepacksBackpack(ItemStack item) {
        if (minepacks == null || isBackpackItemMethod == null) {
            return false;
        }
        try {
            return (boolean) isBackpackItemMethod.invoke(minepacks, item);
        } catch (Exception e) {
            plugin.getLogger().warning("Minepacks hook error; disabling.");
            minepacks = null;
            plugin.setHookMinepacks(false);
            return false;
        }
    }

    public boolean isMinepacksBackpack(Inventory inv, InventoryHolder holder) {
        if (minepacks == null || backpackClass == null || holder == null) {
            return false;
        }
        return backpackClass.isInstance(holder);
    }

}
