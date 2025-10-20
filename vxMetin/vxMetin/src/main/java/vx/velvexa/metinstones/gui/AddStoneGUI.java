package vx.velvexa.metinstones.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.vxMetin;

public class AddStoneGUI {

    public static void open(Player player, StoneManager stoneManager) {
        vxMetin plugin = vxMetin.getInstance();

        String guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.title-add-stone"));

        int total = stoneManager.getAllStones().size();
        int rows = Math.min(((total - 1) / 9) + 1, 6);
        if (rows <= 0) rows = 1;
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size, guiTitle);

        if (total == 0) {
            ItemStack info = new ItemStack(Material.BARRIER);
            ItemMeta meta = info.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.no-stones-title")));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.no-stones-lore")));
                meta.setLore(lore);
                info.setItemMeta(meta);
            }
            inv.setItem(size / 2, info);
            player.openInventory(inv);
            return;
        }

        NamespacedKey KEY_ID = new NamespacedKey(plugin, "stone_id");
        NamespacedKey KEY_UID = new NamespacedKey(plugin, "stone_uid");

        for (StoneManager.MetinStone stone : stoneManager.getAllStones()) {
            Material mat = (stone.material != null ? stone.material : Material.STONE);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                String display = ChatColor.translateAlternateColorCodes('&', stone.displayName);
                meta.setDisplayName(display);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.stone-lore-id").replace("{id}", stone.id)));
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.stone-lore-health").replace("{health}", String.valueOf((int) stone.health))));
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.stone-lore-respawn").replace("{respawn}", String.valueOf(stone.respawnSeconds))));
                lore.add(" ");
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.stone-lore-click")));
                meta.setLore(lore);

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(KEY_ID, PersistentDataType.STRING, stone.id);
                pdc.set(KEY_UID, PersistentDataType.STRING, "template");

                item.setItemMeta(meta);
            }

            inv.addItem(item);
        }

        player.openInventory(inv);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG][AddStoneGUI] " + player.getName() +
                    " opened the Add Stone GUI (" + total + " stones listed).");
        }
    }
}
