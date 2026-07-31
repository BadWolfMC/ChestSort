package de.jeff_media.chestsort.handlers;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.data.PlayerSetting;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Logger implements AutoCloseable {

    private final ChestSortPlugin plugin;
    private final boolean enabled;
    private final java.util.logging.Logger logger;
    private FileHandler fileHandler;

    public Logger(ChestSortPlugin plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.logger = java.util.logging.Logger.getLogger("ChestSortLogger");
        this.logger.setUseParentHandlers(false);

        if (!enabled) {
            return;
        }

        plugin.getLogger().info("=======================================");
        plugin.getLogger().info("     CHESTSORT LOGGER ACTIVATED!");
        plugin.getLogger().info("=======================================");

        try {
            fileHandler = new FileHandler(
                    new File(plugin.getDataFolder(), "ChestSort.log").getAbsolutePath(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not open ChestSort.log", exception);
        }
    }

    private String getPlayerSettings(Player player) {
        PlayerSetting setting = plugin.getPerPlayerSettings().get(player.getUniqueId().toString());
        if (setting == null) {
            return "null";
        }
        return String.format(
                "sorting: %s, invsorting: %s, middle-click: %s, shift-click: %s, double-click: %s, "
                        + "shift-right-click: %s, left-click: %s, right-click: %s, seen-msg: %s",
                setting.sortingEnabled,
                setting.invSortingEnabled,
                setting.middleClick,
                setting.shiftClick,
                setting.doubleClick,
                setting.shiftRightClick,
                setting.leftClick,
                setting.rightClick,
                setting.hasSeenMessage);
    }

    private void log(String message) {
        if (enabled && fileHandler != null) {
            logger.info(message);
        }
    }

    public void logSort(Player player, SortCause cause) {
        SortCause effectiveCause = cause == null ? SortCause.UNKNOWN : cause;
        log(String.format(
                "SORT: Player: %s, Cause: %s, Settings: {%s}",
                player.getName(), effectiveCause.name(), getPlayerSettings(player)));
    }

    public void logPlayerJoin(Player player) {
        log(String.format(
                "JOIN: Player: %s, Settings: {%s}",
                player.getName(), getPlayerSettings(player)));
    }

    @Override
    public void close() {
        if (fileHandler == null) {
            return;
        }
        fileHandler.flush();
        fileHandler.close();
        logger.removeHandler(fileHandler);
        fileHandler = null;
    }

    public enum SortCause {
        UNKNOWN, INV_CLOSE, CONT_CLOSE, CONT_OPEN, EC_OPEN, H_MIDDLE, H_SHIFT, H_DOUBLE,
        H_SHIFTRIGHT, H_LEFT, H_RIGHT, CMD_ISORT
    }
}
