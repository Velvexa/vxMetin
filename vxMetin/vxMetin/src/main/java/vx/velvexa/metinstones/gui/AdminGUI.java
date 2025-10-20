package vx.velvexa.metinstones.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.managers.StoneSpawnManager.ActiveStone;
import vx.velvexa.metinstones.vxMetin;

public class AdminGUI {

    private static final int GUI_SIZE = 27;

    public static void open(Player player) {
        vxMetin plugin = vxMetin.getInstance();
        String guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.title-admin"));
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, guiTitle);

        inv.setItem(10, createItem(Material.EMERALD_BLOCK,
                plugin.getLang().get("gui.button-add"),
                plugin.getLang().get("gui.button-lore-add")));

        inv.setItem(12, createItem(Material.BARRIER,
                plugin.getLang().get("gui.button-remove"),
                plugin.getLang().get("gui.button-lore-remove")));

        inv.setItem(14, createItem(Material.RESPAWN_ANCHOR,
                plugin.getLang().get("gui.button-respawn"),
                plugin.getLang().get("gui.button-lore-respawn")));

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    public static void openStoneList(Player player, String mode) {
        vxMetin plugin = vxMetin.getInstance();
        StoneSpawnManager spawnManager = plugin.getSpawnManager();
        Map<UUID, ActiveStone> active = spawnManager.getActiveStones();

        String titleKey = switch (mode.toLowerCase()) {
            case "remove" -> plugin.getLang().get("gui.title-stonelist-remove");
            case "respawn" -> plugin.getLang().get("gui.title-stonelist-respawn");
            default -> plugin.getLang().get("gui.title-stonelist");
        };

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', titleKey));

        Map<String, Location> stored = plugin.getStorage().safeLoadAll();

        if ((active == null || active.isEmpty()) && (stored == null || stored.isEmpty())) {
            inv.setItem(22, createItem(Material.BARRIER,
                    plugin.getLang().get("gui.no-stones-title"),
                    plugin.getLang().get("gui.no-stones-lore")));
        } else {
            int slot = 0;

            if (active != null && !active.isEmpty()) {
                for (ActiveStone stone : active.values()) {
                    if (slot >= 54) break;
                    inv.setItem(slot++, createStoneItem(stone.uniqueId, stone.location,
                            stone.data.displayName, stone.data.material));
                }
            }

            if (stored != null && !stored.isEmpty()) {
                for (Map.Entry<String, Location> entry : stored.entrySet()) {
                    String uid = entry.getKey();
                    boolean alreadyListed = active != null && active.values().stream()
                            .anyMatch(a -> a.uniqueId.equalsIgnoreCase(uid));
                    if (alreadyListed) continue;

                    if (slot >= 54) break;
                    Location loc = entry.getValue();
                    inv.setItem(slot++, createStoneItem(uid, loc,
                            plugin.getLang().get("gui.registered-stone-name"),
                            Material.STONE));
                }
            }
        }

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    public static void openConfirmGUI(Player player, String uniqueId) {
        vxMetin plugin = vxMetin.getInstance();
        Inventory inv = Bukkit.createInventory(null, 9,
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("gui.title-confirm-delete")));

        List<String> yesLore = new ArrayList<>();
        yesLore.add(ChatColor.translateAlternateColorCodes('&', "&7UID: &f" + uniqueId));
        yesLore.add(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-yes-lore")));

        List<String> noLore = new ArrayList<>();
        noLore.add(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.confirm-no-lore")));

        inv.setItem(3, createItem(Material.LIME_WOOL,
                plugin.getLang().get("gui.confirm-yes"), yesLore));

        inv.setItem(5, createItem(Material.RED_WOOL,
                plugin.getLang().get("gui.confirm-no"), noLore));

        fillEmptySlots(inv, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    private static ItemStack createStoneItem(String uid, Location loc, String displayName, Material mat) {
        vxMetin plugin = vxMetin.getInstance();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.stone-lore-id").replace("{id}", uid)));

        if (loc != null && loc.getWorld() != null) {
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("gui.stone-lore-location")
                            .replace("{world}", loc.getWorld().getName())
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()))));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("gui.stone-lore-location-unknown")));
        }

        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.stone-lore-click")));

        return createItem(mat != null ? mat : Material.STONE,
                ChatColor.translateAlternateColorCodes('&', displayName),
                lore);
    }

    private static ItemStack createItem(Material mat, String name, String lore) {
        return createItem(mat, name, lore == null ? Collections.emptyList() : Collections.singletonList(lore));
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillEmptySlots(Inventory inv, Material fillerMat) {
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }
}
