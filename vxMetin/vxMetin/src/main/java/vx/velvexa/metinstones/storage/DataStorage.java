package vx.velvexa.metinstones.storage;

import java.util.Collections;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import vx.velvexa.metinstones.vxMetin;

public abstract class DataStorage {

    protected final vxMetin plugin;

    public DataStorage(vxMetin plugin) {
        this.plugin = plugin;
    }

    public abstract void init();

    public abstract void saveStone(String uniqueId, String stoneId, Location location, String placer);

    public abstract void deleteStone(String uniqueId);

    public abstract Map<String, Location> loadAll();

    public abstract void close();

    protected void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[vxMetin][Storage][DEBUG] " + message);
        }
    }

    public Map<String, Location> safeLoadAll() {
        try {
            Map<String, Location> map = loadAll();
            if (map == null || map.isEmpty()) {
                plugin.getLogger().info(plugin.getLang().get("console.storage-no-stones"));
                return Collections.emptyMap();
            }
            plugin.getLogger().info(plugin.getLang().get("console.storage-loaded")
                    .replace("{count}", String.valueOf(map.size())));
            return map;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[vxMetin] " 
                + plugin.getLang().get("console.storage-load-error")
                .replace("{error}", e.getMessage()));
            return Collections.emptyMap();
        }
    }

    public Location getStoneLocation(String uniqueId) {
        try {
            Map<String, Location> all = safeLoadAll();
            if (all.containsKey(uniqueId)) {
                return all.get(uniqueId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[vxMetin] " 
                + plugin.getLang().get("console.storage-get-location-error")
                .replace("{error}", e.getMessage()));
        }
        return null;
    }
}
