package vx.velvexa.metinstones.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import vx.velvexa.metinstones.storage.DataStorage;
import vx.velvexa.metinstones.vxMetin;

public class StoneManager {

    private final vxMetin plugin;
    private final Map<String, MetinStone> stones = new HashMap<>();
    private final File stonesFile;
    private final FileConfiguration cfg;
    private final DataStorage storage;

    public StoneManager(vxMetin plugin) {
        this.plugin = plugin;
        this.stonesFile = new File(plugin.getDataFolder(), "stones.yml");

        if (!stonesFile.exists()) {
            plugin.saveResource("stones.yml", false);
            plugin.getLogger().info(plugin.getLang().get("messages.stones-file-created"));
        }

        this.cfg = YamlConfiguration.loadConfiguration(stonesFile);
        this.storage = plugin.getStorage();
        loadStones();
    }

    public void loadStones() {
        stones.clear();
        ConfigurationSection section = cfg.getConfigurationSection("stones");
        if (section == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.no-stones-section"));
            return;
        }

        int loaded = 0;
        int skipped = 0;

        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) continue;

            try {
                String displayName = s.getString("display-name", id);
                String coloredName = ChatColor.translateAlternateColorCodes('&', displayName);

                Material material = Material.matchMaterial(s.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning(plugin.getLang().get("console.invalid-material").replace("{id}", id));
                    material = Material.STONE;
                }

                double health = s.getDouble("health", 100);
                int respawn = s.getInt("respawn-seconds", 1800);
                String worldName = s.getString("spawn-world", "world");

                List<String> rewards = s.getStringList("rewards");
                List<String> rewardsTop1 = s.getStringList("rewards-top1");
                List<String> rewardsTop2 = s.getStringList("rewards-top2");
                List<String> rewardsTop3 = s.getStringList("rewards-top3");
                List<String> effects = s.getStringList("effects");

                String msgSpawn = ChatColor.translateAlternateColorCodes('&', s.getString("messageSpawn", ""));
                String msgDestroy = ChatColor.translateAlternateColorCodes('&', s.getString("messageDestroy", ""));
                String msgRespawn = ChatColor.translateAlternateColorCodes('&', s.getString("messageRespawn", ""));

                boolean hologramEnabled = s.getBoolean("hologram-enabled", true);
                double hologramOffset = s.getDouble("hologram-offset", 1.5);
                List<String> hologramLines = new ArrayList<>();
                if (s.isList("hologram-lines")) {
                    for (String line : s.getStringList("hologram-lines")) {
                        hologramLines.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                }

                MetinStone stone = new MetinStone(
                        id, displayName, coloredName, material, health, respawn,
                        worldName, rewards, rewardsTop1, rewardsTop2, rewardsTop3,
                        effects, msgSpawn, msgDestroy, msgRespawn,
                        hologramEnabled, hologramOffset, hologramLines
                );

                stones.put(id, stone);
                loaded++;
            } catch (Exception ex) {
                skipped++;
                plugin.getLogger().warning(plugin.getLang().get("console.stone-load-failed")
                        .replace("{id}", id).replace("{error}", ex.getMessage()));
            }
        }

        plugin.getLogger().info(plugin.getLang().get("console.stones-loaded")
                .replace("{count}", String.valueOf(loaded))
                .replace("{skipped}", String.valueOf(skipped)));
    }

    public void savePlacedStone(String uniqueId, MetinStone base, Location loc) {
        try {
            storage.saveStone(uniqueId, base.id, loc, "SYSTEM");
            plugin.getLogger().info(plugin.getLang().get("console.stone-saved").replace("{uid}", uniqueId));
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getLang().get("console.stone-save-failed")
                    .replace("{uid}", uniqueId).replace("{error}", e.getMessage()));
        }
    }

    public void deleteStone(String uniqueId) {
        try {
            storage.deleteStone(uniqueId);
            plugin.getLogger().info(plugin.getLang().get("console.stone-deleted").replace("{uid}", uniqueId));
        } catch (Exception e) {
            plugin.getLogger().warning(plugin.getLang().get("console.stone-delete-failed")
                    .replace("{uid}", uniqueId).replace("{error}", e.getMessage()));
        }
    }

    public MetinStone getStone(String id) {
        return stones.get(id);
    }

    public Collection<MetinStone> getAllStones() {
        return stones.values();
    }

    public DataStorage getStorage() {
        return storage;
    }

    public static class MetinStone {
        public final String id;
        public final String displayName;
        public final String coloredName;
        public final Material material;
        public final double health;
        public final int respawnSeconds;
        public final String world;

        public final List<String> rewards;
        public final List<String> rewardsTop1;
        public final List<String> rewardsTop2;
        public final List<String> rewardsTop3;
        public final List<String> effects;

        public final String messageSpawn;
        public final String messageDestroy;
        public final String messageRespawn;

        public final boolean hologramEnabled;
        public final double hologramOffset;
        public final List<String> hologramLines;

        public MetinStone(String id,
                          String displayName,
                          String coloredName,
                          Material material,
                          double health,
                          int respawnSeconds,
                          String world,
                          List<String> rewards,
                          List<String> rewardsTop1,
                          List<String> rewardsTop2,
                          List<String> rewardsTop3,
                          List<String> effects,
                          String messageSpawn,
                          String messageDestroy,
                          String messageRespawn,
                          boolean hologramEnabled,
                          double hologramOffset,
                          List<String> hologramLines) {
            this.id = id;
            this.displayName = displayName;
            this.coloredName = coloredName;
            this.material = material;
            this.health = health;
            this.respawnSeconds = respawnSeconds;
            this.world = world;
            this.rewards = rewards != null ? rewards : new ArrayList<>();
            this.rewardsTop1 = rewardsTop1 != null ? rewardsTop1 : new ArrayList<>();
            this.rewardsTop2 = rewardsTop2 != null ? rewardsTop2 : new ArrayList<>();
            this.rewardsTop3 = rewardsTop3 != null ? rewardsTop3 : new ArrayList<>();
            this.effects = effects != null ? effects : new ArrayList<>();
            this.messageSpawn = messageSpawn;
            this.messageDestroy = messageDestroy;
            this.messageRespawn = messageRespawn;
            this.hologramEnabled = hologramEnabled;
            this.hologramOffset = hologramOffset;
            this.hologramLines = hologramLines != null ? hologramLines : new ArrayList<>();
        }
    }
}
