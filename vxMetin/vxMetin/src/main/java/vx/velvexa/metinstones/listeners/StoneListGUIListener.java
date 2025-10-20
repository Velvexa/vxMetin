package vx.velvexa.metinstones.listeners;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vx.velvexa.metinstones.gui.ConfirmGUI;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class StoneListGUIListener implements Listener {

    private final vxMetin plugin;

    public StoneListGUIListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!(title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-remove"))) ||
              title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-teleport"))) ||
              title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-respawn"))))) return;

        e.setCancelled(true);

        String uid = ChatColor.stripColor(meta.getDisplayName());
        StoneSpawnManager spawnManager = plugin.getSpawnManager();
        Map<String, Location> stored = spawnManager.getStorage().loadAll();
        Location loc = stored.get(uid);
        if (loc == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.invalid-stone-selection")));
            return;
        }

        if (title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-remove")))) {
            player.closeInventory();
            ConfirmGUI.open(player, uid);
            debug("Admin " + player.getName() + " opened ConfirmGUI (" + uid + ")");

        } else if (title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-teleport")))) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(loc.clone().add(0.5, 1, 0.5));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getLang().get("messages.teleported-to-stone").replace("{uid}", uid)));
            });
            debug("Admin " + player.getName() + " teleported to stone: " + uid);

        } else if (title.contains(ChatColor.stripColor(plugin.getLang().get("gui.title-respawn")))) {
            player.closeInventory();
            spawnManager.forceRespawn(uid);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.stone-respawned").replace("{uid}", uid)));
            debug("Admin " + player.getName() + " respawned stone: " + uid);
        }
    }

    private void debug(String msg) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG][StoneListGUI] " + msg);
        }
    }
}
