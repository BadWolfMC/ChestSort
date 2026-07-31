/*

	ChestSort - maintained by mfnalex / JEFF Media GbR ( www.jeff-media.de )
	
	THANK YOU for your interest in ChestSort :)
	
	ChestSort has been an open-source project from the day it started.
	Without the support of the community, many awesome features
	would be missing. A big THANK YOU to everyone who contributed to
	this project!
	
	If you have bug reports, feature requests etc. please message me at SpigotMC.org:
	https://www.spigotmc.org/members/mfnalex.175238/
	
	Please DO NOT post bug reports or feature requests in the review section at SpigotMC.org. Thank you.
	
	=============================================================================================
	
	TECHNICAL INFORMATION:
	
	If you want to know how the sorting works, have a look at the JeffChestSortOrganizer class.
	
	If you want to contribute, please note that messages sent to player must be made configurable in the config.yml.
	Please have a look at the JeffChestSortMessages class if you want to add a message.
	
*/

package de.jeff_media.chestsort;

import de.jeff_media.chestsort.commands.ChestSortCommand;
import de.jeff_media.chestsort.commands.InvSortCommand;
import de.jeff_media.chestsort.commands.TabCompleter;
import de.jeff_media.chestsort.config.Config;
import de.jeff_media.chestsort.config.ConfigUpdater;
import de.jeff_media.chestsort.config.Messages;
import de.jeff_media.chestsort.data.Category;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.gui.GUIListener;
import de.jeff_media.chestsort.gui.SettingsGUI;
import de.jeff_media.chestsort.gui.tracker.CustomGUITracker;
import de.jeff_media.chestsort.handlers.ChestSortOrganizer;
import de.jeff_media.chestsort.handlers.ChestSortPermissionsHandler;
import de.jeff_media.chestsort.handlers.Debugger;
import de.jeff_media.chestsort.handlers.Logger;
import de.jeff_media.chestsort.hooks.EnderContainersHook;
import de.jeff_media.chestsort.hooks.GenericGUIHook;
import de.jeff_media.chestsort.hooks.PlayerVaultsHook;
import de.jeff_media.chestsort.listeners.ChestSortListener;
import de.jeff_media.chestsort.placeholders.Placeholders;
import de.jeff_media.chestsort.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class ChestSortPlugin extends JavaPlugin {

    private static double updateCheckInterval = 4 * 60 * 60; // in seconds. We check on startup and every 4 hours
    private static ChestSortPlugin instance;
    public ChestSortOrganizer organizer; // Must be public for the API
    boolean hotkeyGUI = true;
    private EnderContainersHook enderContainersHook;
    private GenericGUIHook genericHook;
    private boolean hookCrackShot = false;
    private boolean hookInventoryPages = false;
    private boolean hookMinepacks = false;
    private boolean hookAdvancedChests = false;
    private PlayerVaultsHook playerVaultsHook;
    private boolean debug = false;
    private ArrayList<String> disabledWorlds;
    private HashMap<UUID, Long> hotkeyCooldown;
    private Logger lgr;
    private ChestSortListener chestSortListener;
    // 1.14.4 = 1_14_R1
    // 1.8.0  = 1_8_R1
    private int mcMinorVersion; // 14 for 1.14, 13 for 1.13, ...
    private String mcVersion;    // 1.13.2 = 1_13_R2
    private Messages messages;
    private Map<String, PlayerSetting> perPlayerSettings = new HashMap<>();
    private ChestSortPermissionsHandler permissionsHandler;
    private SettingsGUI settingsGUI;
    private String sortingMethod;
    private boolean usingMatchingConfig = true;
    private boolean verbose = true;
    private YamlConfiguration guiConfig = new YamlConfiguration();
    private int settingsFingerprint = 0;

    private static final String PDC_SORTING_ENABLED = "sorting_enabled";
    private static final String PDC_INV_SORTING_ENABLED = "inv_sorting_enabled";
    private static final String PDC_HAS_SEEN_MESSAGE = "has_seen_message";
    private static final String PDC_MIDDLE_CLICK = "middle_click";
    private static final String PDC_SHIFT_CLICK = "shift_click";
    private static final String PDC_DOUBLE_CLICK = "double_click";
    private static final String PDC_SHIFT_RIGHT_CLICK = "shift_right_click";
    private static final String PDC_LEFT_CLICK = "left_click";
    private static final String PDC_RIGHT_CLICK = "right_click";
    private static final String PDC_LEFT_CLICK_OUTSIDE = "left_click_outside";

    public List<Pattern> blacklistedInventoryHolderClassNames = new ArrayList<>();

    public static ChestSortPlugin getInstance() {
        return instance;
    }

    public YamlConfiguration getGuiConfig() { return guiConfig; }

    public static double getUpdateCheckInterval() {
        return updateCheckInterval;
    }

    public static void setUpdateCheckInterval(double updateCheckInterval) {
        ChestSortPlugin.updateCheckInterval = updateCheckInterval;
    }

    // Creates the default configuration file
    // Also checks the config-version of an already existing file. If the existing
    // config is too
    // old (generated prior to ChestSort 2.0.0), we rename it to config.old.yml so
    // that users
    // can start off with a new config file that includes all new options. However,
    // on most
    // updates, the file will not be touched, even if new config options were added.
    // You will instead
    // get a warning in the console that you should consider adding the options
    // manually. If you do
    // not add them, the default values will be used for any unset values.
    void createConfig() {

        // This saves the config.yml included in the .jar file, but it will not
        // overwrite an existing config.yml
        this.saveDefaultConfig();
        createGUIConfig();
        reloadConfig();

        // Load disabled-worlds. If it does not exist in the config, it returns null.
        // That's no problem
        setDisabledWorlds((ArrayList<String>) getConfig().getStringList(Config.DISABLED_WORLDS));

        ConfigUpdater.updateConfig();

        createDirectories();

        setDefaultConfigValues();

    }

    private void createGUIConfig() {
        File guiFile = new File(getDataFolder(), "gui.yml");
        if(!guiFile.exists()) {
            saveResource("gui.yml",false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    private void createDirectories() {
        createDirectory(new File(getDataFolder(), "playerdata"));
        createDirectory(new File(getDataFolder(), "categories"));
    }

    private void createDirectory(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            getLogger().warning("Could not create directory: " + directory.getAbsolutePath());
        }
    }

    public void debug(String t) {
        if (isDebug()) getLogger().warning("[DEBUG] " + t);
    }

    public void debug2(String t) {
        if (getConfig().getBoolean(Config.DEBUG2)) getLogger().warning("[DEBUG2] " + t);
    }

    // Dumps all Materials into a csv file with their current category
    void dump() {
        File file = new File(getDataFolder(), "dump.csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            for (Material material : Material.values()) {
                writer.write(material.name() + "," + getOrganizer().getCategoryLinePair(material.name()).getCategoryName());
                writer.newLine();
            }
        } catch (IOException exception) {
            getLogger().log(java.util.logging.Level.WARNING, "Could not write " + file.getAbsolutePath(), exception);
        }
    }

    private String getCategoryList() {
        StringBuilder list = new StringBuilder();
        Category[] categories = getOrganizer().categories.toArray(new Category[0]);
        Arrays.sort(categories);
        for (Category category : categories) {
            list.append(category.name).append(" (");
            list.append(category.typeMatches.length).append("), ");
        }
        if (list.isEmpty()) {
            return "(none)";
        }
        list.setLength(list.length() - 2);
        return list.toString();

    }

    public ArrayList<String> getDisabledWorlds() {
        return disabledWorlds == null ? new ArrayList<>() : disabledWorlds;
    }

    public void setDisabledWorlds(ArrayList<String> disabledWorlds) {
        this.disabledWorlds = new ArrayList<>();
        for (String world : disabledWorlds) {
            this.disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }
    }

    public EnderContainersHook getEnderContainersHook() {
        return enderContainersHook;
    }

    public void setEnderContainersHook(EnderContainersHook enderContainersHook) {
        this.enderContainersHook = enderContainersHook;
    }

    public GenericGUIHook getGenericHook() {
        return genericHook;
    }

    public void setGenericHook(GenericGUIHook genericHook) {
        this.genericHook = genericHook;
    }

    public HashMap<UUID, Long> getHotkeyCooldown() {
        return hotkeyCooldown;
    }

    public void setHotkeyCooldown(HashMap<UUID, Long> hotkeyCooldown) {
        this.hotkeyCooldown = hotkeyCooldown;
    }

    public Logger getLgr() {
        return lgr;
    }

    public void setLgr(Logger lgr) {
        if (this.lgr != null) {
            this.lgr.close();
        }
        this.lgr = lgr;
    }

    public ChestSortListener getListener() {
        return chestSortListener;
    }

    public void setListener(ChestSortListener chestSortListener) {
        this.chestSortListener = chestSortListener;
    }

    public Messages getMessages() {
        return messages;
    }

    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    public ChestSortOrganizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(ChestSortOrganizer organizer) {
        this.organizer = organizer;
    }

    public Map<String, PlayerSetting> getPerPlayerSettings() {
        return perPlayerSettings;
    }

    public void setPerPlayerSettings(Map<String, PlayerSetting> perPlayerSettings) {
        this.perPlayerSettings = perPlayerSettings;
    }

    public ChestSortPermissionsHandler getPermissionsHandler() {
        return permissionsHandler;
    }

    public void setPermissionsHandler(ChestSortPermissionsHandler permissionsHandler) {
        this.permissionsHandler = permissionsHandler;
    }

    public PlayerSetting getPlayerSetting(Player p) {
        registerPlayerIfNeeded(p);
        return getPerPlayerSettings().get(p.getUniqueId().toString());
    }

    public PlayerVaultsHook getPlayerVaultsHook() {
        return playerVaultsHook;
    }

    public void setPlayerVaultsHook(PlayerVaultsHook playerVaultsHook) {
        this.playerVaultsHook = playerVaultsHook;
    }

    public SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }

    public void setSettingsGUI(SettingsGUI settingsGUI) {
        this.settingsGUI = settingsGUI;
    }

    public String getSortingMethod() {
        return sortingMethod;
    }

    public void setSortingMethod(String sortingMethod) {
        this.sortingMethod = sortingMethod;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isHookCrackShot() {
        return hookCrackShot;
    }

    public void setHookCrackShot(boolean hookCrackShot) {
        this.hookCrackShot = hookCrackShot;
    }

    public boolean isHookInventoryPages() {
        return hookInventoryPages;
    }

    public void setHookInventoryPages(boolean hookInventoryPages) {
        this.hookInventoryPages = hookInventoryPages;
    }

    public boolean isHookMinepacks() {
        return hookMinepacks;
    }

    public void setHookMinepacks(boolean hookMinepacks) {
        this.hookMinepacks = hookMinepacks;
    }

    public boolean isHookAdvancedChests() {
        return hookAdvancedChests;
    }

    public void setHookAdvancedChests(boolean hookAdvancedChests) {
        this.hookAdvancedChests = hookAdvancedChests;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isHotkeyGUI() {
        // TODO: Remove, it's unused
        return hotkeyGUI;
    }

    public boolean isInHotkeyCooldown(UUID uuid) {
        double cooldown = getConfig().getDouble(Config.HOTKEY_COOLDOWN) * 1000;
        if (cooldown == 0) return false;
        long lastUsage = getHotkeyCooldown().containsKey(uuid) ? getHotkeyCooldown().get(uuid) : 0;
        long currentTime = System.currentTimeMillis();
        long difference = currentTime - lastUsage;
        getHotkeyCooldown().put(uuid, currentTime);
        debug("Difference: " + difference);
        return difference <= cooldown;
    }

    public boolean isSortingEnabled(Player p) {
        if (getPerPlayerSettings() == null) {
            setPerPlayerSettings(new HashMap<>());
        }
        registerPlayerIfNeeded(p);
        return getPerPlayerSettings().get(p.getUniqueId().toString()).sortingEnabled;
    }

    public boolean isUsingMatchingConfig() {
        return usingMatchingConfig;
    }

    public void setUsingMatchingConfig(boolean usingMatchingConfig) {
        this.usingMatchingConfig = usingMatchingConfig;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void load(boolean reload) {

        settingsFingerprint = 0;
        File fingerprintFile = new File(getDataFolder(), "settings.fingerprint");
        if(fingerprintFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fingerprintFile);
            settingsFingerprint = yaml.getInt("v",0);
        }

        if (reload) {
            unregisterAllPlayers();
            reloadConfig();
        }

        createConfig();
        setDebug(getConfig().getBoolean("debug"));

        HandlerList.unregisterAll(this);

        if (isDebug()) {
            Debugger debugger = new Debugger(this);
            getServer().getPluginManager().registerEvents(debugger, this);
        }

        setHookCrackShot(getConfig().getBoolean("hook-crackshot")
                && Bukkit.getPluginManager().getPlugin("CrackShot") != null);

        setHookInventoryPages(getConfig().getBoolean("hook-inventorypages")
                && Bukkit.getPluginManager().getPlugin("InventoryPages") != null);

        setHookMinepacks(getConfig().getBoolean("hook-minepacks")
                && Bukkit.getPluginManager().getPlugin("Minepacks") != null);

        setHookAdvancedChests(getConfig().getBoolean("hook-advancedchests")
                && Bukkit.getPluginManager().getPlugin("AdvancedChests") != null);

        setGenericHook(new GenericGUIHook(this, getConfig().getBoolean("hook-generic")));

        saveDefaultCategories();

        blacklistedInventoryHolderClassNames.clear();
        for(String line : getConfig().getStringList("blocked-inventory-holders-regex")) {
            try {
                Pattern pattern = Pattern.compile(line);
                blacklistedInventoryHolderClassNames.add(pattern);
            } catch (Exception e) {
                getLogger().warning("Invalid regex in blocked-inventory-holders-regex: " + line);
                continue;
            }
        }

        setVerbose(getConfig().getBoolean("verbose"));
        setLgr(new Logger(this, getConfig().getBoolean("log")));
        //noinspection InstantiationOfUtilityClass
        new Messages();
        setOrganizer(new ChestSortOrganizer(this));
        setSettingsGUI(new SettingsGUI(this));
        setListener(new ChestSortListener(this));
        setHotkeyCooldown(new HashMap<>());
        setPermissionsHandler(new ChestSortPermissionsHandler(this));
        setUpdateCheckInterval(getConfig().getDouble("check-interval"));
        setSortingMethod(getConfig().getString("sorting-method"));
        setPlayerVaultsHook(new PlayerVaultsHook(this));
        setEnderContainersHook(new EnderContainersHook(this));
        getServer().getPluginManager().registerEvents(getListener(), this);
        getServer().getPluginManager().registerEvents(getSettingsGUI(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        TabCompleter tabCompleter = new TabCompleter();
        PluginCommand sortCommand = Objects.requireNonNull(getCommand("sort"), "Missing sort command in plugin.yml");
        sortCommand.setExecutor(new ChestSortCommand(this));
        sortCommand.setTabCompleter(tabCompleter);

        PluginCommand invSortCommand = Objects.requireNonNull(getCommand("isort"), "Missing isort command in plugin.yml");
        invSortCommand.setExecutor(new InvSortCommand(this));
        invSortCommand.setTabCompleter(tabCompleter);
        //this.getCommand("chestsortadmin").setExecutor(new AdminCommand(this));

        if (isVerbose()) {
            getLogger().info("Use permissions: " + getConfig().getBoolean("use-permissions"));
            getLogger().info("Current sorting method: " + getSortingMethod());
            getLogger().info("Allow automatic chest sorting:" + getConfig().getBoolean("allow-automatic-sorting"));
            getLogger().info("  |- Chest sorting enabled by default: " + getConfig().getBoolean("sorting-enabled-by-default"));
            getLogger().info("  |- Sort time: " + getConfig().getString("sort-time"));
            getLogger().info("Allow automatic inventory sorting:" + getConfig().getBoolean("allow-automatic-inventory-sorting"));
            getLogger().info("  |- Inventory sorting enabled by default: " + getConfig().getBoolean("inv-sorting-enabled-by-default"));
            getLogger().info("Auto generate category files: " + getConfig().getBoolean("auto-generate-category-files"));
            getLogger().info("Allow hotkeys: " + getConfig().getBoolean("allow-sorting-hotkeys"));
            if (getConfig().getBoolean("allow-sorting-hotkeys")) {
                getLogger().info("Hotkeys enabled by default:");
                getLogger().info("  |- Middle-Click: " + getConfig().getBoolean("sorting-hotkeys.middle-click"));
                getLogger().info("  |- Shift-Click: " + getConfig().getBoolean("sorting-hotkeys.shift-click"));
                getLogger().info("  |- Double-Click: " + getConfig().getBoolean("sorting-hotkeys.double-click"));
                getLogger().info("  |- Shift-Right-Click: " + getConfig().getBoolean("sorting-hotkeys.shift-right-click"));
            }
            getLogger().info("Allow additional hotkeys: " + getConfig().getBoolean("allow-additional-hotkeys"));
            if (getConfig().getBoolean("allow-additional-hotkeys")) {
                getLogger().info("Additional hotkeys enabled by default:");
                getLogger().info("  |- Left-Click: " + getConfig().getBoolean("additional-hotkeys.left-click"));
                getLogger().info("  |- Right-Click: " + getConfig().getBoolean("additional-hotkeys.right-click"));
            }
            getLogger().info("Check for updates: " + getConfig().getString("check-for-updates"));
            if (getConfig().getString("check-for-updates").equalsIgnoreCase("true")) {
                getLogger().info("Check interval: " + getConfig().getString("check-interval") + " hours (" + getUpdateCheckInterval() + " seconds)");
            }
            getLogger().info("Categories: " + getCategoryList());
        }


        if (getConfig().getBoolean("dump")) {
            dump();
        }

        for (Player p : getServer().getOnlinePlayers()) {
            getPermissionsHandler().addPermissions(p);
        }

        // End Reload

    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (CustomGUITracker.getType(player.getOpenInventory()) != null) {
                player.closeInventory();
            }
            unregisterPlayer(player);
            if (getPermissionsHandler() != null) {
                getPermissionsHandler().removePermissions(player);
            }
        }
        CustomGUITracker.clear();
        if (lgr != null) {
            lgr.close();
        }
    }

    @Override
    public void onEnable() {

        instance = this;

        /*String tmpVersion = getServer().getClass().getPackage().getName();
        setMcVersion(tmpVersion.substring(tmpVersion.lastIndexOf('.') + 1));
        tmpVersion = getMcVersion().substring(getMcVersion().indexOf("_") + 1);
        setMcMinorVersion(Integer.parseInt(tmpVersion.substring(0, tmpVersion.indexOf("_"))));*/



        load(false);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
        }
    }

    public void incrementFingerprint() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("v",settingsFingerprint + 1);
        settingsFingerprint++;
        try {
            yaml.save(new File(getDataFolder(),"settings.fingerprint"));
            load(true);
        } catch (IOException exception) {
            getLogger().log(java.util.logging.Level.WARNING, "Could not update settings fingerprint", exception);
        }
    }

    public void registerPlayerIfNeeded(Player player) {
        String playerId = player.getUniqueId().toString();
        if (getPerPlayerSettings().containsKey(playerId)) {
            return;
        }

        File playerFile = new File(new File(getDataFolder(), "playerdata"), playerId + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        boolean sortingEnabled;
        boolean invSortingEnabled;
        boolean middleClick;
        boolean shiftClick;
        boolean doubleClick;
        boolean shiftRightClick;
        boolean leftClick;
        boolean rightClick;
        boolean leftClickOutside;
        boolean hasSeenMessage;

        if (playerFile.isFile()) {
            sortingEnabled = playerConfig.getBoolean("sortingEnabled", getConfig().getBoolean("sorting-enabled-by-default"));
            invSortingEnabled = playerConfig.getBoolean("invSortingEnabled", getConfig().getBoolean("inv-sorting-enabled-by-default"));
            middleClick = playerConfig.getBoolean("middleClick", getConfig().getBoolean("sorting-hotkeys.middle-click"));
            shiftClick = playerConfig.getBoolean("shiftClick", getConfig().getBoolean("sorting-hotkeys.shift-click"));
            doubleClick = playerConfig.getBoolean("doubleClick", getConfig().getBoolean("sorting-hotkeys.double-click"));
            shiftRightClick = playerConfig.getBoolean("shiftRightClick", getConfig().getBoolean("sorting-hotkeys.shift-right-click"));
            leftClick = playerConfig.getBoolean("leftClick", getConfig().getBoolean("additional-hotkeys.left-click"));
            rightClick = playerConfig.getBoolean("rightClick", getConfig().getBoolean("additional-hotkeys.right-click"));
            leftClickOutside = playerConfig.getBoolean("leftClickOutside", getConfig().getBoolean("left-click-to-sort-enabled-by-default"));
            hasSeenMessage = playerConfig.getBoolean("hasSeenMessage", false);

        } else {
            sortingEnabled = getStoredBoolean(player, PDC_SORTING_ENABLED, getConfig().getBoolean("sorting-enabled-by-default"));
            invSortingEnabled = getStoredBoolean(player, PDC_INV_SORTING_ENABLED, getConfig().getBoolean("inv-sorting-enabled-by-default"));
            middleClick = getStoredBoolean(player, PDC_MIDDLE_CLICK, getConfig().getBoolean("sorting-hotkeys.middle-click"));
            shiftClick = getStoredBoolean(player, PDC_SHIFT_CLICK, getConfig().getBoolean("sorting-hotkeys.shift-click"));
            doubleClick = getStoredBoolean(player, PDC_DOUBLE_CLICK, getConfig().getBoolean("sorting-hotkeys.double-click"));
            shiftRightClick = getStoredBoolean(player, PDC_SHIFT_RIGHT_CLICK, getConfig().getBoolean("sorting-hotkeys.shift-right-click"));
            leftClick = getStoredBoolean(player, PDC_LEFT_CLICK, getConfig().getBoolean("additional-hotkeys.left-click"));
            rightClick = getStoredBoolean(player, PDC_RIGHT_CLICK, getConfig().getBoolean("additional-hotkeys.right-click"));
            leftClickOutside = getStoredBoolean(player, PDC_LEFT_CLICK_OUTSIDE, getConfig().getBoolean("left-click-to-sort-enabled-by-default"));
            hasSeenMessage = getStoredBoolean(player, PDC_HAS_SEEN_MESSAGE, false);
        }

        if (getConfig().getBoolean("show-message-again-after-logout")) {
            hasSeenMessage = false;
        }

        PlayerSetting settings = new PlayerSetting(
                sortingEnabled, invSortingEnabled, middleClick, shiftClick, doubleClick,
                shiftRightClick, leftClick, rightClick, leftClickOutside, true, hasSeenMessage);
        if (playerFile.isFile()) {
            savePlayerSettings(player, settings);
            if (playerFile.delete()) {
                getLogger().info("Converted old .yml playerdata file to persistent player data for " + player.getName());
            } else {
                getLogger().warning("Could not remove old playerdata .yml file for " + player.getName());
            }
        }
        getPerPlayerSettings().put(playerId, settings);
    }

    private boolean getStoredBoolean(Player player, String key, boolean defaultValue) {
        Boolean value = player.getPersistentDataContainer().get(
                getPlayerSettingsKey(key), PersistentDataType.BOOLEAN);
        return value == null ? defaultValue : value;
    }

    private void setStoredBoolean(Player player, String key, boolean value) {
        player.getPersistentDataContainer().set(
                getPlayerSettingsKey(key), PersistentDataType.BOOLEAN, value);
    }

    private void savePlayerSettings(Player player, PlayerSetting setting) {
        setStoredBoolean(player, PDC_SORTING_ENABLED, setting.sortingEnabled);
        setStoredBoolean(player, PDC_INV_SORTING_ENABLED, setting.invSortingEnabled);
        setStoredBoolean(player, PDC_HAS_SEEN_MESSAGE, setting.hasSeenMessage);
        setStoredBoolean(player, PDC_MIDDLE_CLICK, setting.middleClick);
        setStoredBoolean(player, PDC_SHIFT_CLICK, setting.shiftClick);
        setStoredBoolean(player, PDC_DOUBLE_CLICK, setting.doubleClick);
        setStoredBoolean(player, PDC_SHIFT_RIGHT_CLICK, setting.shiftRightClick);
        setStoredBoolean(player, PDC_LEFT_CLICK, setting.leftClick);
        setStoredBoolean(player, PDC_RIGHT_CLICK, setting.rightClick);
        setStoredBoolean(player, PDC_LEFT_CLICK_OUTSIDE, setting.leftClickOutside);
    }

    public NamespacedKey getPlayerSettingsKey(String key) {
        return new NamespacedKey(this, key + getFingerprint());
    }

    private String getFingerprint() {
        String fingerprint = "";
        if(settingsFingerprint > 0) {
            fingerprint = "-" + settingsFingerprint;
        }
        return fingerprint;
    }

    // Saves default category files, when enabled in the config
    private void saveDefaultCategories() {

        // Abort when auto-generate-category-files is set to false in config.yml
        if (!getConfig().getBoolean("auto-generate-category-files", true)) {
            return;
        }

        // Isn't there a smarter way to find all the 9** files in the .jar?
        String[] defaultCategories = {"900-weapons", "905-common-tools", "907-other-tools", "909-food", "910-valuables", "920-armor-and-arrows", "930-brewing",
                "950-redstone", "960-wood", "970-stone", "980-plants", "981-corals", "_ReadMe - Category files"};

        // Delete obsolete generated category files.
        File categoriesDirectory = new File(getDataFolder(), "categories");
        File[] generatedCategoryFiles = categoriesDirectory.listFiles((directory, fileName) ->
                fileName.matches("(?i)9\\d\\d.*\\.txt$"));
        if (generatedCategoryFiles == null) {
            getLogger().warning("Could not list category directory: " + categoriesDirectory.getAbsolutePath());
            return;
        }
        for (File file : generatedCategoryFiles) {

            boolean delete = true;

            for (String name : defaultCategories) {
                name = name + ".txt";
                if (name.equalsIgnoreCase(file.getName())) {
                    delete = false;
                    break;
                }
            }
            if (delete) {
                if (file.delete()) {
                    getLogger().warning("Deleting deprecated default category file " + file.getName());
                } else {
                    getLogger().warning("Could not delete deprecated default category file " + file.getName());
                }
            }

        }

        for (String category : defaultCategories) {
            String resourcePath = "categories/" + category + ".default.txt";
            File targetFile = new File(categoriesDirectory, category + ".txt");
            try (InputStream input = getResource(resourcePath)) {
                if (input == null) {
                    getLogger().warning("Missing category resource: " + resourcePath);
                    continue;
                }
                Files.copy(input, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                getLogger().log(java.util.logging.Level.WARNING,
                        "Could not write default category file: " + targetFile.getAbsolutePath(), exception);
            }
        }
    }

    private void setDefaultConfigValues() {
        // If you use an old config file with missing options, the following default
        // values will be used instead
        // for every missing option.
        // By default, sorting is disabled. Every player has to run /chestsort once
        getConfig().addDefault("use-permissions", true);
        getConfig().addDefault("allow-automatic-sorting", true);
        getConfig().addDefault("allow-automatic-inventory-sorting", true);
        getConfig().addDefault("allow-left-click-to-sort", true);
        getConfig().addDefault("left-click-to-sort-enabled-by-default", false);
        getConfig().addDefault("sorting-enabled-by-default", false);
        getConfig().addDefault("inv-sorting-enabled-by-default", false);
        getConfig().addDefault("show-message-when-using-chest", true);
        getConfig().addDefault("show-message-when-using-chest-and-sorting-is-enabled", false);
        getConfig().addDefault("show-message-again-after-logout", true);
        getConfig().addDefault("sorting-method", "{category},{itemsFirst},{name},{color}");
        getConfig().addDefault("allow-player-inventory-sorting", false);
        getConfig().addDefault("check-for-updates", "true");
        getConfig().addDefault("check-interval", 4);
        getConfig().addDefault("auto-generate-category-files", true);
        getConfig().addDefault("sort-time", "close");
        getConfig().addDefault("allow-sorting-hotkeys", true);
        getConfig().addDefault("allow-additional-hotkeys", true);
        getConfig().addDefault("sorting-hotkeys.middle-click", true);
        getConfig().addDefault("sorting-hotkeys.shift-click", true);
        getConfig().addDefault("sorting-hotkeys.double-click", true);
        getConfig().addDefault("sorting-hotkeys.shift-right-click", true);
        getConfig().addDefault("additional-hotkeys.left-click", false);
        getConfig().addDefault("additional-hotkeys.right-click", false);
        getConfig().addDefault("dump", false);
        getConfig().addDefault("log", false);
        getConfig().addDefault("allow-commands", true);

        getConfig().addDefault("hook-crackshot", true);
        getConfig().addDefault("hook-crackshot-prefix", "crackshot_weapon");
        getConfig().addDefault("hook-inventorypages", true);
        getConfig().addDefault("hook-minepacks", true);
        getConfig().addDefault("hook-generic", true);
        getConfig().addDefault("prevent-sorting-null-inventories", false);

        getConfig().addDefault("mute-protection-plugins", false);

        getConfig().addDefault("verbose", true); // Prints some information in onEnable()
    }

    private void showOldConfigWarning() {
        getLogger().warning("==============================================");
        getLogger().warning("You were using an old config file. ChestSort");
        getLogger().warning("has updated the file to the newest version.");
        getLogger().warning("Your changes have been kept.");
        getLogger().warning("==============================================");
    }

    void unregisterAllPlayers() {
        if (getPerPlayerSettings() == null) {
            setPerPlayerSettings(new HashMap<>());
            return;
        }

        for (Player player : getServer().getOnlinePlayers()) {
            unregisterPlayer(player);
        }
        getPerPlayerSettings().clear();
    }

    public void unregisterPlayer(Player player) {
        String playerId = player.getUniqueId().toString();
        PlayerSetting setting = getPerPlayerSettings().remove(playerId);
        if (setting == null) {
            return;
        }

        savePlayerSettings(player, setting);
    }

}
