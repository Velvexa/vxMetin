package vx.velvexa.metinstones.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class StoneListGUI {

    public enum Mode {
        REMOVE,
        TELEPORT,
        RESPAWN
    }

    public static void open(Player player, Mode mode) {
        vxMetin plugin = vxMetin.getInstance();
        StoneSpawnManager spawnManager = plugin.getSpawnManager();

        String title;
        switch (mode) {
            case REMOVE -> title = ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.title-stonelist-remove"));
            case TELEPORT -> title = ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.title-stonelist-teleport"));
            case RESPAWN -> title = ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.title-stonelist-respawn"));
            default -> title = ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.title-stonelist"));
        }

        Inventory inv = Bukkit.createInventory(null, 54, title);

        Map<String, Location> all = spawnManager.getStorage().loadAll();
        if (all == null || all.isEmpty()) {
            ItemStack empty = createItem(Material.BARRIER,
                    plugin.getLang().get("gui.no-stones-title"),
                    plugin.getLang().get("gui.no-stones-lore"));
            inv.setItem(22, empty);
            player.openInventory(inv);
            return;
        }

        for (Map.Entry<String, Location> entry : all.entrySet()) {
            String uid = entry.getKey();
            Location loc = entry.getValue();

            ItemStack item = new ItemStack(Material.MOSSY_COBBLESTONE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + uid));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("gui.stone-lore-world").replace("{world}", loc.getWorld().getName())));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("gui.stone-lore-coordinates")
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()))));
            lore.add("");
            switch (mode) {
                case REMOVE -> lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.action-remove")));
                case TELEPORT -> lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.action-teleport")));
                case RESPAWN -> lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("gui.action-respawn")));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(List.of(ChatColor.translateAlternateColorCodes('&', lore)));
        }
        item.setItemMeta(meta);
        return item;
    }
}
