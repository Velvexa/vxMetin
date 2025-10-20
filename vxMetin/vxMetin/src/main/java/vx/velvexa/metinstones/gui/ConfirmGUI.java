package vx.velvexa.metinstones.gui;

import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vx.velvexa.metinstones.vxMetin;

public class ConfirmGUI {

    public static void open(Player player, String uniqueId) {
        vxMetin plugin = vxMetin.getInstance();

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.title-confirm-delete"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack yes = new ItemStack(Material.RED_CONCRETE);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-yes")));
        yesMeta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-yes-lore").replace("{id}", uniqueId))));
        yes.setItemMeta(yesMeta);

        ItemStack no = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta noMeta = no.getItemMeta();
        noMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-no")));
        noMeta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-no-lore"))));
        no.setItemMeta(noMeta);

        Material fillerMat;
        try {
            fillerMat = Material.valueOf(plugin.getLang().get("gui.filler-item").toUpperCase());
        } catch (IllegalArgumentException e) {
            fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        inv.setItem(11, yes);
        inv.setItem(15, no);

        player.openInventory(inv);
    }
}
