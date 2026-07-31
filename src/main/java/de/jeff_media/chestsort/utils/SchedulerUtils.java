package de.jeff_media.chestsort.utils;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.folia.FoliaRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Utility methods backed by Paper's region, global-region and async schedulers.
 * These scheduler implementations also work when the plugin is running on Paper.
 */
public final class SchedulerUtils {

    private static final long MILLIS_PER_TICK = 50L;

    private SchedulerUtils() {
    }

    public static void runTaskLater(@Nullable Location location, @NotNull Runnable task, long delayTicks) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        if (location == null) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), delayTicks);
        } else {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, location, ignored -> task.run(), delayTicks);
        }
    }

    public static void runTaskTimer(
            @Nullable Location location,
            @NotNull FoliaRunnable runnable,
            long delayTicks,
            long periodTicks) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        ScheduledTask scheduledTask;
        if (location == null) {
            scheduledTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                    plugin, ignored -> runnable.run(), delayTicks, periodTicks);
        } else {
            scheduledTask = plugin.getServer().getRegionScheduler().runAtFixedRate(
                    plugin, location, ignored -> runnable.run(), delayTicks, periodTicks);
        }
        runnable.setScheduledTask(scheduledTask);
    }

    public static void runTaskTimerAsynchronously(
            @NotNull FoliaRunnable runnable,
            long delayTicks,
            long periodTicks) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        ScheduledTask scheduledTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                ignored -> runnable.run(),
                ticksToMillis(delayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS);
        runnable.setScheduledTask(scheduledTask);
    }

    public static void runTaskAsynchronously(@NotNull Runnable task) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    public static void runTask(@Nullable Location location, @NotNull Runnable task) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        if (location == null) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
        } else {
            plugin.getServer().getRegionScheduler().execute(plugin, location, task);
        }
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(1L, ticks * MILLIS_PER_TICK);
    }
}
