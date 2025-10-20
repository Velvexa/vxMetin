package vx.velvexa.metinstones.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class ConfirmGUIListener implements Listener {

    private final vxMetin plugin;

    public ConfirmGUIListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        final String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.equalsIgnoreCase(ChatColor.stripColor(plugin.getLang().get("gui.title-confirm-delete")))) return;

        final ItemStack current = e.getCurrentItem();
        if (current == null) return;
        final ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        e.setCancelled(true);
        final String name = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
        if (name.isEmpty()) return;

        if (name.toLowerCase().contains(strip(plugin.getLang().get("gui.confirm-yes")))) {
            String parsedId = "";
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                String first = ChatColor.stripColor(lore.get(0)).trim();
                if (first.startsWith("ID: ")) {
                    parsedId = first.substring("ID: ".length()).trim();
                } else if (first.toLowerCase().contains(strip(plugin.getLang().get("gui.confirm-delete-lore-prefix")).toLowerCase())) {
                    parsedId = first.replace(strip(plugin.getLang().get("gui.confirm-delete-lore-prefix")), "").trim();
                }
            }

            if (parsedId.isEmpty()) {
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("messages.no-stone-id")));
                return;
            }

            final String uid = parsedId;
            final StoneSpawnManager spawn = plugin.getSpawnManager();

            if (spawn == null) {
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("messages.spawn-manager-error")));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    StoneSpawnManager.ActiveStone active = spawn.getActiveStones().values().stream()
                            .filter(a -> a != null && uid.equalsIgnoreCase(a.uniqueId))
                            .findFirst()
                            .orElse(null);

                    if (active != null) {
                        spawn.removeStone(active.uuid);
                    } else {
                        spawn.getStorage().deleteStone(uid);
                        try {
                            plugin.getHologramManager().removeHologram("vxmetin_" + uid);
                        } catch (Exception ignored) {}
                    }

                    player.closeInventory();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("messages.stone-deleted").replace("{uid}", uid)));
                    plugin.getLogger().info("[vxMetin] Admin " + player.getName() + " deleted stone: " + uid);
                } catch (Exception ex) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("messages.delete-error").replace("{error}", ex.getMessage())));
                    plugin.getLogger().warning("[vxMetin] Delete error (" + uid + "): " + ex.getMessage());
                }
            });

        } else if (name.toLowerCase().contains(strip(plugin.getLang().get("gui.confirm-no")))) {
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.delete-cancelled")));
        }
    }

    private String strip(String s) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
    }
}
