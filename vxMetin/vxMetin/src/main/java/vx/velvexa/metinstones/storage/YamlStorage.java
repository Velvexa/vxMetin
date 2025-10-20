package vx.velvexa.metinstones.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import vx.velvexa.metinstones.vxMetin;

public class YamlStorage extends DataStorage {

    private File file;
    private YamlConfiguration yaml;

    public YamlStorage(vxMetin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        try {
            String path = plugin.getConfig().getString("storage.yaml.file", "data/database.yml");
            file = new File(plugin.getDataFolder(), path);

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                if (file.createNewFile()) {
                    plugin.getLogger().info(plugin.getLang().get("console.yaml-created").replace("{path}", path));
                }
            }

            yaml = YamlConfiguration.loadConfiguration(file);
            debug(plugin.getLang().get("console.yaml-loaded"));

        } catch (IOException e) {
            plugin.getLogger().severe(plugin.getLang().get("console.yaml-create-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public void saveStone(String uniqueId, String stoneId, Location loc, String placer) {
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.yaml-invalid-location")
                    .replace("{uid}", uniqueId));
            return;
        }

        String path = "stones." + uniqueId;
        yaml.set(path + ".id", stoneId);
        yaml.set(path + ".world", loc.getWorld().getName());
        yaml.set(path + ".x", loc.getBlockX());
        yaml.set(path + ".y", loc.getBlockY());
        yaml.set(path + ".z", loc.getBlockZ());
        yaml.set(path + ".placer", placer);
        yaml.set(path + ".timestamp", System.currentTimeMillis());

        saveFileSync();

        debug(plugin.getLang().get("console.yaml-saved")
                .replace("{uid}", uniqueId)
                .replace("{world}", loc.getWorld().getName())
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ())));
    }

    @Override
    public void deleteStone(String uniqueId) {
        if (yaml == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.yaml-not-loaded")
                    .replace("{uid}", uniqueId));
            return;
        }

        yaml.set("stones." + uniqueId, null);
        saveFileSync();
        debug(plugin.getLang().get("console.yaml-deleted").replace("{uid}", uniqueId));
    }

    @Override
    public Map<String, Location> loadAll() {
        Map<String, Location> map = new HashMap<>();

        if (yaml == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.yaml-null"));
            return map;
        }

        if (yaml.getConfigurationSection("stones") == null) {
            debug(plugin.getLang().get("console.yaml-no-stones"));
            return map;
        }

        int count = 0;
        for (String key : yaml.getConfigurationSection("stones").getKeys(false)) {
            try {
                String worldName = yaml.getString("stones." + key + ".world");
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    plugin.getLogger().warning(plugin.getLang().get("console.world-not-found")
                            .replace("{world}", worldName)
                            .replace("{uid}", key));
                    continue;
                }

                int x = yaml.getInt("stones." + key + ".x");
                int y = yaml.getInt("stones." + key + ".y");
                int z = yaml.getInt("stones." + key + ".z");

                Location loc = new Location(world, x + 0.5, y, z + 0.5);
                map.put(key, loc);
                count++;

            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getLang().get("console.yaml-load-error")
                        .replace("{uid}", key)
                        .replace("{error}", e.getMessage()));
            }
        }

        debug(plugin.getLang().get("console.yaml-loaded-count")
                .replace("{count}", String.valueOf(count)));
        return map;
    }

    @Override
    public void close() {
        saveFileSync();
        debug(plugin.getLang().get("console.yaml-closed"));
    }

    private synchronized void saveFileSync() {
        try {
            if (yaml != null && file != null) {
                yaml.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.yaml-save-error")
                    .replace("{error}", e.getMessage()));
        }
    }
}
