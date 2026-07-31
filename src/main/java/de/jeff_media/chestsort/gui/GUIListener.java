package de.jeff_media.chestsort.gui;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.gui.tracker.CustomGUITracker;
import de.jeff_media.chestsort.gui.tracker.CustomGUIType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GUIListener implements Listener {

    private final ChestSortPlugin plugin;
    private final NamespacedKey functionKey;
    private final NamespacedKey userCommandsKey;
    private final NamespacedKey adminCommandsKey;

    public GUIListener(ChestSortPlugin plugin) {
        this.plugin = plugin;
        this.functionKey = new NamespacedKey(plugin, "function");
        this.userCommandsKey = new NamespacedKey(plugin, "user-commands");
        this.adminCommandsKey = new NamespacedKey(plugin, "admin-commands");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (CustomGUITracker.getType(event.getView()) != CustomGUIType.NEW) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        String function = meta.getPersistentDataContainer().getOrDefault(
                functionKey, PersistentDataType.STRING, "");
        List<String> userCommands = meta.getPersistentDataContainer().getOrDefault(
                userCommandsKey, PersistentDataType.LIST.strings(), new ArrayList<>());
        List<String> adminCommands = meta.getPersistentDataContainer().getOrDefault(
                adminCommandsKey, PersistentDataType.LIST.strings(), new ArrayList<>());

        executeCommands(player, player, userCommands);
        executeCommands(player, Bukkit.getConsoleSender(), adminCommands);

        PlayerSetting setting = plugin.getPlayerSetting(player);
        switch (function) {
            case "leftclick" -> setting.toggleLeftClick();
            case "rightclick" -> setting.toggleRightClick();
            case "shiftclick" -> setting.toggleShiftClick();
            case "middleclick" -> setting.toggleMiddleClick();
            case "shiftrightclick" -> setting.toggleShiftRightClick();
            case "doubleclick" -> setting.toggleDoubleClick();
            case "outside" -> setting.toggleLeftClickOutside();
            case "autosorting" -> setting.toggleChestSorting();
            case "autoinvsorting" -> setting.toggleInvSorting();
            default -> {
                return;
            }
        }

        new NewUI(player).showGUI();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        CustomGUITracker.close(event.getView());
    }

    private void executeCommands(Player player, CommandSender sender, List<String> commands) {
        for (String command : commands) {
            plugin.getServer().dispatchCommand(sender, command.replace("{player}", player.getName()));
        }
    }
}
