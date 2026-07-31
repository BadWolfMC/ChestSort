package de.jeff_media.chestsort.gui;

import com.google.gson.JsonParser;
import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.enums.Hotkey;
import de.jeff_media.chestsort.gui.tracker.CustomGUITracker;
import de.jeff_media.chestsort.gui.tracker.CustomGUIType;
import de.jeff_media.chestsort.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class NewUI {

    private static final ChestSortPlugin main = ChestSortPlugin.getInstance();
    private final YamlConfiguration conf = main.getGuiConfig();
    private final Player player;

    public NewUI(Player player) {
        this.player = player;
    }

    private ItemStack getItem(int slot) {
        if(conf.isConfigurationSection("slots." + slot)) {
            return fromConfigurationSection(conf.getConfigurationSection("slots." + slot));
        }
        if(conf.isString("slots." + slot)) {
            String buttonName = conf.getString("slots." + slot);
            Hotkey key = Hotkey.fromPermission(buttonName);
            if (key != null && !key.hasPermission(player)) {
                buttonName = buttonName + "-nopermission";
            } else {
                boolean enabled = key == null || key.hasEnabled(player);
                if (key != null) buttonName = buttonName + (enabled ? "-enabled" : "-disabled");
            }
            main.debug("GUI button: " + buttonName);
            ItemStack button = fromConfigurationSection(conf.getConfigurationSection("items." + buttonName));
            if(button.hasItemMeta() && !buttonName.endsWith("-nopermission")) {
                ItemMeta meta = button.getItemMeta();
                if (meta == null) return button;
                int separator = buttonName.indexOf('-');
                String function = separator < 0 ? buttonName : buttonName.substring(0, separator);
                meta.getPersistentDataContainer().set(new NamespacedKey(main,"function"),PersistentDataType.STRING, function);
                List<String> userCommands = conf.getStringList("items." + buttonName + ".commands.player");
                List<String> adminCommands = conf.getStringList("items." + buttonName + ".commands.console");
                meta.getPersistentDataContainer().set(new NamespacedKey(main,"user-commands"), PersistentDataType.LIST.strings(), userCommands);
                meta.getPersistentDataContainer().set(new NamespacedKey(main,"admin-commands"), PersistentDataType.LIST.strings(), adminCommands);
                button.setItemMeta(meta);
            }
            return button;
        }
        return null;
    }

    public void showGUI() {

        int size = conf.getInt("size");
        String title = Utils.formatText(conf.getString("title"));

        Inventory inv = Bukkit.createInventory(null, size, title);

        for(int i = 0; i < size; i++) {
            ItemStack item = getItem(i);
            inv.setItem(i, item);
        }

        CustomGUITracker.open(player, inv, CustomGUIType.NEW);
    }

    private static ItemStack fromConfigurationSection(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);

        Material material;
        try {
            material = Material.valueOf(section.getString("material", "STONE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);

        // Apply base64 skull texture before touching ItemMeta for the first time
        String base64 = section.getString("base64");
        if (base64 != null && material == Material.PLAYER_HEAD) {
            try {
                String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                String url = JsonParser.parseString(decoded)
                        .getAsJsonObject()
                        .getAsJsonObject("textures")
                        .getAsJsonObject("SKIN")
                        .get("url").getAsString();
                SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
                if (skullMeta == null) return item;
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(URI.create(url).toURL());
                profile.setTextures(textures);
                skullMeta.setOwnerProfile(profile);
                item.setItemMeta(skullMeta);
            } catch (Exception ignored) {
                // Invalid base64 or malformed texture JSON — leave as plain head
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = section.getString("display-name");
        if (displayName != null) {
            meta.setDisplayName(Utils.formatText(displayName));
        }

        List<String> loreStrings = section.getStringList("lore");
        if (!loreStrings.isEmpty()) {
            meta.setLore(loreStrings.stream()
                    .map(Utils::formatText)
                    .collect(Collectors.toList()));
        }

        if (section.isInt("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        if (meta instanceof Damageable damageable && section.isInt("damage")) {
            damageable.setDamage(section.getInt("damage"));
        }

        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String key : enchantSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, enchantSection.getInt(key), true);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
