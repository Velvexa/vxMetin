package vx.velvexa.metinstones.listeners;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.vxMetin;

public class AddStoneGUIListener implements Listener {

    private final vxMetin plugin;
    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_UID;
    private static final Pattern ID_PATTERN = Pattern.compile("(?i)\\bID\\s*:\\s*(\\S+)");

    public AddStoneGUIListener(vxMetin plugin) {
        this.plugin = plugin;
        this.KEY_ID = new NamespacedKey(plugin, "stone_id");
        this.KEY_UID = new NamespacedKey(plugin, "stone_uid");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null) return;

        String expectedTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("gui.title-add-stone"));
        String currentTitle = ChatColor.translateAlternateColorCodes('&', e.getView().getTitle());

        if (!ChatColor.stripColor(currentTitle)
                .equalsIgnoreCase(ChatColor.stripColor(expectedTitle))) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(KEY_ID, PersistentDataType.STRING);

        if (id == null || id.isEmpty()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line));
                    Matcher m = ID_PATTERN.matcher(plain);
                    if (m.find()) {
                        id = m.group(1);
                        break;
                    }
                }
            }
        }

        if (id == null || id.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.invalid-stone-data")));
            return;
        }

        StoneManager.MetinStone stone = plugin.getStoneManager().getStone(id);
        if (stone == null) {
            player.sendMessage(plugin.getLang().get("messages.invalid-stone-id").replace("{id}", id));
            debug(plugin.getLang().get("debug.invalid-stone-id").replace("{id}", id));
            return;
        }

        ItemStack stoneItem = new ItemStack(stone.material != null ? stone.material : Material.STONE);
        ItemMeta blockMeta = stoneItem.getItemMeta();

        if (blockMeta != null) {
            String coloredName = ChatColor.translateAlternateColorCodes('&', stone.displayName);
            blockMeta.setDisplayName(coloredName);
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);

            PersistentDataContainer npdc = blockMeta.getPersistentDataContainer();
            npdc.set(KEY_ID, PersistentDataType.STRING, id);
            npdc.set(KEY_UID, PersistentDataType.STRING, uniqueId);

            blockMeta.setLore(List.of(
                    ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("gui.stone-lore-title").replace("{stone}", stone.displayName)),
                    ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("gui.stone-lore-id").replace("{id}", id)),
                    ChatColor.translateAlternateColorCodes('&',
                            plugin.getLang().get("gui.stone-lore-uid").replace("{uid}", uniqueId))
            ));
            stoneItem.setItemMeta(blockMeta);
        }

        player.getInventory().addItem(stoneItem);
        player.closeInventory();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("messages.received-stone").replace("{stone}", stone.coloredName)));

        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logAction(
                    player.getName(),
                    id + "_" + blockMeta.getPersistentDataContainer().get(KEY_UID, PersistentDataType.STRING),
                    "GIVE",
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ());
        }

        debug(plugin.getLang().get("debug.stone-given")
                .replace("{id}", id)
                .replace("{uid}", blockMeta.getPersistentDataContainer().get(KEY_UID, PersistentDataType.STRING)));
    }

    private void debug(String msg) {
        if (plugin.getConfig().getBoolean("debug", false))
            plugin.getLogger().info("[DEBUG][AddStoneGUI] " + msg);
    }
}
