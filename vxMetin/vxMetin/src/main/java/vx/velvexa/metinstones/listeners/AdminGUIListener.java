package vx.velvexa.metinstones.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import vx.velvexa.metinstones.gui.AddStoneGUI;
import vx.velvexa.metinstones.gui.AdminGUI;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class AdminGUIListener implements Listener {

    private final vxMetin plugin;

    public AdminGUIListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        Inventory top = e.getView().getTopInventory();
        if (!e.getClickedInventory().equals(top)) return;
        e.setCancelled(true);
        if (e.getAction() == InventoryAction.NOTHING) return;

        final String rawTitle = e.getView().getTitle();
        final String title = strip(rawTitle);

        final String adminTitle = strip(plugin.getLang().get("gui.title-admin"));
        final String listTitle = strip(plugin.getLang().get("gui.list-title"));
        final String confirmTitle = strip(plugin.getLang().get("gui.title-confirm"));

        boolean isAdminMenu = title.equalsIgnoreCase(adminTitle);
        boolean isListMenu = title.equalsIgnoreCase(listTitle)
                || title.toLowerCase().contains("remove")
                || title.toLowerCase().contains("respawn")
                || title.toLowerCase().contains("sil")
                || title.toLowerCase().contains("yeniden");
        boolean isConfirmMenu = title.equalsIgnoreCase(confirmTitle)
                || title.toLowerCase().contains("onay");

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String itemName = strip(item.getItemMeta().getDisplayName());
        StoneSpawnManager spawnManager = plugin.getSpawnManager();

        if (isAdminMenu) {
            final String addButton = strip(plugin.getLang().get("gui.button-add"));
            final String removeButton = strip(plugin.getLang().get("gui.button-remove"));
            final String respawnButton = strip(plugin.getLang().get("gui.button-respawn"));

            if (itemName.equalsIgnoreCase(addButton)) {
                player.closeInventory();
                AddStoneGUI.open(player, plugin.getStoneManager());
                debug("Opened AddStoneGUI for " + player.getName());
                return;
            }

            if (itemName.equalsIgnoreCase(removeButton)) {
                player.closeInventory();
                AdminGUI.openStoneList(player, "remove");
                debug("Opened Remove list GUI for " + player.getName());
                return;
            }

            if (itemName.equalsIgnoreCase(respawnButton)) {
                player.closeInventory();
                AdminGUI.openStoneList(player, "respawn");
                debug("Opened Respawn list GUI for " + player.getName());
                return;
            }
        }

        if (isListMenu) {
            String mode = "none";
            String lower = rawTitle.toLowerCase();
            if (lower.contains("remove") || lower.contains("sil")) mode = "remove";
            else if (lower.contains("respawn") || lower.contains("yeniden")) mode = "respawn";

            String uid = extractUniqueId(item);
            if (uid == null || uid.isEmpty()) {
                debug("No UID found for clicked item: " + itemName);
                return;
            }

            if (mode.equals("remove")) {
                player.closeInventory();
                AdminGUI.openConfirmGUI(player, uid);
                debug("Opened confirm GUI for removal: " + uid);
            } else if (mode.equals("respawn")) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    spawnManager.forceRespawn(uid);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("messages.stone-respawned").replace("{uid}", uid)));
                });
                debug("Forced respawn for stone: " + uid);
            }
        }

        if (isConfirmMenu) {
            String uid = extractUniqueId(item);
            if (uid == null || uid.isEmpty()) return;

            final String confirm = strip(plugin.getLang().get("gui.confirm-yes"));
            final String cancel = strip(plugin.getLang().get("gui.confirm-no"));

            if (itemName.equalsIgnoreCase(confirm)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    spawnManager.removeStoneByUniqueId(uid);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("messages.stone-deleted").replace("{uid}", uid)));
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> AdminGUI.open(player), 10L);
                });
                debug("Confirmed removal for " + uid);
            } else if (itemName.equalsIgnoreCase(cancel)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("messages.action-cancelled")));
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> AdminGUI.open(player), 10L);
                debug("Cancelled removal for " + uid);
            }
        }
    }

private String extractUniqueId(ItemStack item) {
    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
    List<String> lore = item.getItemMeta().getLore();
    if (lore == null) return null;

    for (String line : lore) {
        String clean = ChatColor.stripColor(line).toLowerCase().trim();
        if (clean.startsWith("uid:")) {
            return clean.substring(4).trim();
        }
        if (clean.contains("uid:")) {
            return clean.split("uid:")[1].trim();
        }
        if (clean.contains("unique id")) {
            return clean.replace("unique id", "").trim();
        }
        if (clean.contains("benzersiz kimlik")) {
            return clean.replace("benzersiz kimlik", "").trim();
        }
    }
    return null;
}

    private String strip(String s) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s == null ? "" : s));
    }

    private void debug(String msg) {
        if (plugin.getConfig().getBoolean("debug", true))
            plugin.getLogger().info("[DEBUG][AdminGUI] " + msg);
    }
}
