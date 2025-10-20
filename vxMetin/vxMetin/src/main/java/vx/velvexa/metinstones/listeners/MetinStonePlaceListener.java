package vx.velvexa.metinstones.listeners;

import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.vxMetin;

public class MetinStonePlaceListener implements Listener {

    private final vxMetin plugin;
    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_UID;

    public MetinStonePlaceListener(vxMetin plugin) {
        this.plugin = plugin;
        this.KEY_ID = new NamespacedKey(plugin, "stone_id");
        this.KEY_UID = new NamespacedKey(plugin, "stone_uid");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Block block = e.getBlockPlaced();

        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String stoneId = null;
        String uniqueId = null;
        try {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            stoneId = pdc.get(KEY_ID, PersistentDataType.STRING);
            uniqueId = pdc.get(KEY_UID, PersistentDataType.STRING);
        } catch (Throwable ignored) {}

        if (stoneId == null || uniqueId == null) {
            String ln = meta.getLocalizedName();
            if (ln != null && ln.startsWith("vxmetin_")) {
                int uidIndex = ln.lastIndexOf("_UID_");
                if (uidIndex != -1) {
                    stoneId = ln.substring("vxmetin_".length(), uidIndex);
                    uniqueId = ln.substring(uidIndex + "_UID_".length());
                }
            }
        }

        if (uniqueId == null || uniqueId.equalsIgnoreCase("template")) {
            uniqueId = UUID.randomUUID().toString().substring(0, 8);
        }

        uniqueId = stoneId + "_" + uniqueId;

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[vxMetin][PlaceDebug] type=" + item.getType()
                    + " | display=" + (meta.hasDisplayName() ? meta.getDisplayName() : "null")
                    + " | pdc.id=" + stoneId + " | pdc.uid=" + uniqueId);
        }

        if (stoneId == null) return;

        StoneManager.MetinStone stone = plugin.getStoneManager().getStone(stoneId);
        if (stone == null) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.invalid-stone-id")));
            return;
        }

        if (!player.hasPermission("vxmetin.place")) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.no-permission")));
            return;
        }

        Map<String, ?> saved = plugin.getStorage().safeLoadAll();
        if (saved.containsKey(uniqueId)) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.stone-already-placed")));
            return;
        }

        if (plugin.getSpawnManager().getStoneAt(block.getLocation()) != null) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.stone-already-exists")));
            return;
        }

        e.setCancelled(true);
        player.closeInventory();

        try {
            plugin.getSpawnManager().spawnStone(player, stone, block.getLocation(), uniqueId);
            plugin.getStorage().saveStone(uniqueId, stone.id, block.getLocation(), player.getName());

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.player-stone-placed")
                            .replace("{stone}", stone.coloredName)));

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logAction(
                        player.getName(),
                        uniqueId,
                        "PLACE",
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ()
                );
            }

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[vxMetin] Stone placed & saved → id=" + stone.id +
                        ", uid=" + uniqueId + ", by=" + player.getName());
            }

        } catch (Exception ex) {
            plugin.getLogger().severe("[vxMetin][PlaceError] " + ex.getMessage());
            ex.printStackTrace();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.place-error")));
        }
    }
}
