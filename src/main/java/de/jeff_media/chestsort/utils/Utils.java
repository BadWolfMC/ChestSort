package de.jeff_media.chestsort.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Utils {

    private static final Pattern HEX_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>");

    /**
     * Converts a string with legacy &amp; colour codes and &lt;#rrggbb&gt; hex colour
     * tags into a Bukkit-formatted string (§ codes).
     */
    public static String formatText(String text) {
        if (text == null) return "";
        // Strip gradient-end tags like <#/85c1e9>
        text = text.replaceAll("<#/[0-9a-fA-F]{6}>", "");
        // Convert <#rrggbb> to Spigot §x§r§r§g§g§b§b hex format
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder spigotHex = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                spigotHex.append('§').append(c);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(spigotHex.toString()));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

	public static ItemStack[] getStorageContents(Inventory inv) {
		return inv.getStorageContents();
	}
	
	// We need this to write the category files inside the .jar to the disk
	// Maybe there is a smarter way, i don't know.
	public static byte[] getBytes(InputStream is) throws IOException {

	    int len;
	    int size = 1024;
	    byte[] buf;

	    if (is instanceof ByteArrayInputStream) {
	      size = is.available();
	      buf = new byte[size];
	      len = is.read(buf, 0, size);
	    } else {
	      ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      buf = new byte[size];
	      while ((len = is.read(buf, 0, size)) != -1)
	        bos.write(buf, 0, len);
	      buf = bos.toByteArray();
	    }
	    return buf;
	  }
	
	public static String shortToStringWithLeadingZeroes(short number) {
		return String.format("%05d", number);
	}
	
	public static void renameFileInPluginDir(ChestSortPlugin plugin,String oldName, String newName) {
		File oldFile = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + oldName);
		File newFile = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + newName);
		oldFile.getAbsoluteFile().renameTo(newFile.getAbsoluteFile());
	}
}
