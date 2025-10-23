package vx.velvexa.metinstones.hologram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class HologramManager {

    private final vxMetin plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageData = new ConcurrentHashMap<>();
    private final Set<Location> respawningLocations = ConcurrentHashMap.newKeySet();
    private final Map<String, BukkitRunnable> respawnTasks = new ConcurrentHashMap<>();
    private double baseOffset;

    public HologramManager(vxMetin plugin) {
        this.plugin = plugin;
        reloadOffset();
    }

    public void reloadOffset() {
        this.baseOffset = plugin.getConfig().getDouble("hologram.base-offset", 1.8);
        plugin.getLogger().info(plugin.getLang().get("debug.hologram-base-offset").replace("{offset}", String.valueOf(baseOffset)));
    }

    public void createHologram(String uniqueId, StoneManager.MetinStone stone, Location loc) {
        if (loc == null || loc.getWorld() == null || !stone.hologramEnabled) return;

        String holoId = "vxmetin_" + uniqueId;
        double centerX = loc.getBlockX() + 0.5;
        double centerY = loc.getBlockY() + stone.hologramOffset + baseOffset;
        double centerZ = loc.getBlockZ() + 0.5;
        Location holoLoc = new Location(loc.getWorld(), centerX, centerY, centerZ);

        removeHologram(holoId);

        List<String> parsedLines = new ArrayList<>();
        for (String line : stone.hologramLines) {
            parsedLines.addAll(expandLine(line, stone, null, stone.health, stone.health, null));
        }

        Hologram holo = DHAPI.createHologram(holoId, holoLoc, true, parsedLines);
        activeHolograms.put(holoId, holo);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info(plugin.getLang().get("debug.hologram-created")
                    .replace("{id}", holoId)
                    .replace("{offset}", String.valueOf(baseOffset))
                    .replace("{x}", String.valueOf(holoLoc.getBlockX()))
                    .replace("{y}", String.valueOf(holoLoc.getBlockY()))
                    .replace("{z}", String.valueOf(holoLoc.getBlockZ())));
        }
    }

    public void updateHologram(String uniqueId, StoneManager.MetinStone stone,
                               double currentHP, double maxHP, Player lastBreaker) {
        Hologram holo = activeHolograms.get("vxmetin_" + uniqueId);
        if (holo == null || stone == null) return;

        List<String> topList = buildTopDamagersList(stone.id);
        List<String> updated = new ArrayList<>();

        for (String line : stone.hologramLines) {
            updated.addAll(expandLine(line, stone, lastBreaker, currentHP, maxHP, topList));
        }

        DHAPI.setHologramLines(holo, updated);
    }

    public void startRespawnCountdown(String uniqueId, StoneManager.MetinStone stone, Location loc, int seconds) {
        final String holoId = "vxmetin_" + uniqueId;

        if (!activeHolograms.containsKey(holoId)) {
            createHologram(uniqueId, stone, loc);
        }

        final Hologram target = activeHolograms.get(holoId);
        if (target == null) return;

        if (loc != null && loc.getWorld() != null) {
            respawningLocations.add(loc.clone());
        }

        BukkitRunnable existing = respawnTasks.remove(uniqueId);
        if (existing != null) existing.cancel();

        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (!isHologramActive(target)) {
                    cancel();
                    respawnTasks.remove(uniqueId);
                    if (loc != null) {
                        respawningLocations.removeIf(l ->
                                l.getWorld().equals(loc.getWorld()) &&
                                l.getBlockX() == loc.getBlockX() &&
                                l.getBlockY() == loc.getBlockY() &&
                                l.getBlockZ() == loc.getBlockZ());
                    }
                    return;
                }

                if (timeLeft > 0) {
                    String stoneColored = stone.coloredName != null ? ChatColor.translateAlternateColorCodes('&', stone.coloredName) : stone.displayName;
                    List<String> countdown = List.of(
                            ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("hologram.respawn-line-name")
                                    .replace("{stone}", stoneColored)
                                    .replace("{name}", stoneColored)),
                            ChatColor.translateAlternateColorCodes('&', plugin.getLang().get("hologram.respawn-line-timer")
                                    .replace("{stone}", stoneColored)
                                    .replace("{name}", stoneColored)
                                    .replace("{time}", String.valueOf(timeLeft)))
                    );
                    DHAPI.setHologramLines(target, countdown);
                    timeLeft--;
                    return;
                }

                cancel();
                respawnTasks.remove(uniqueId);
                try {
                    if (loc != null && loc.getWorld() != null) {
                        Chunk ch = loc.getChunk();
                        if (!ch.isLoaded()) {
                            ch.load(true);
                            if (plugin.getConfig().getBoolean("debug", true)) {
                                plugin.getLogger().info(plugin.getLang().get("debug.chunk-loaded")
                                        .replace("{x}", String.valueOf(ch.getX()))
                                        .replace("{z}", String.valueOf(ch.getZ())));
                            }
                        }
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning(plugin.getLang().get("debug.chunk-load-fail") + ex.getMessage());
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    StoneSpawnManager spawnManager = plugin.getSpawnManager();
                    if (spawnManager == null) return;

                    spawnManager.respawnStone(uniqueId);

                    try {
                        removeHologram(holoId);
                    } catch (Exception ignored) {}

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        final Location known = spawnManager.getKnownLocation(uniqueId) != null
                                ? spawnManager.getKnownLocation(uniqueId)
                                : loc;

                        try {
                            createHologram(uniqueId, stone, known);
                            updateHologram(uniqueId, stone, stone.health, stone.health, null);
                        } catch (Exception ex) {
                            plugin.getLogger().warning(plugin.getLang().get("debug.respawn-update-fail") + ex.getMessage());
                        }

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            boolean inActive = spawnManager.getActiveStones().values().stream()
                                    .anyMatch(s -> s.uniqueId.equals(uniqueId));
                            if (!inActive) {
                                if (plugin.getConfig().getBoolean("debug", true)) {
                                    plugin.getLogger().warning(plugin.getLang().get("debug.respawn-retry")
                                            .replace("{id}", uniqueId));
                                }
                                spawnManager.respawnStone(uniqueId);
                                final Location k2 = spawnManager.getKnownLocation(uniqueId) != null
                                        ? spawnManager.getKnownLocation(uniqueId)
                                        : known;
                                removeHologram(holoId);
                                createHologram(uniqueId, stone, k2);
                                updateHologram(uniqueId, stone, stone.health, stone.health, null);
                            }

                            if (loc != null) {
                                respawningLocations.removeIf(l ->
                                        l.getWorld().equals(loc.getWorld()) &&
                                        l.getBlockX() == loc.getBlockX() &&
                                        l.getBlockY() == loc.getBlockY() &&
                                        l.getBlockZ() == loc.getBlockZ());
                                if (plugin.getConfig().getBoolean("debug", true)) {
                                    plugin.getLogger().info(plugin.getLang().get("debug.respawn-complete")
                                            .replace("{id}", uniqueId));
                                }
                            }

                        }, 10L);
                    }, 2L);
                });
            }
        };
        respawnTasks.put(uniqueId, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void addDamage(UUID stoneRuntimeUUID, UUID playerUUID, double amount) {
        damageData.computeIfAbsent(stoneRuntimeUUID, k -> new ConcurrentHashMap<>())
                .merge(playerUUID, amount, Double::sum);
    }

    private List<String> buildTopDamagersList(String stoneIdHint) {
        Map<UUID, Double> merged = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, Map<UUID, Double>> entry : damageData.entrySet()) {
            entry.getValue().forEach((p, dmg) -> merged.merge(p, dmg, Double::sum));
        }
        if (merged.isEmpty()) return new ArrayList<>();

        List<Map.Entry<UUID, Double>> sorted = merged.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            UUID uuid = sorted.get(i).getKey();
            double dmg = sorted.get(i).getValue();
            Player p = plugin.getServer().getPlayer(uuid);
            String name = (p != null) ? p.getName() :
                    (Bukkit.getOfflinePlayer(uuid) != null ? Bukkit.getOfflinePlayer(uuid).getName() : null);
            if (name == null) continue;
            lines.add(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("hologram.topdamager-line")
                            .replace("{rank}", String.valueOf(i + 1))
                            .replace("{player}", name)
                            .replace("{damage}", String.valueOf((int) dmg))));
        }
        return lines;
    }

    private boolean isHologramActive(Hologram holo) {
        try {
            for (String methodName : List.of("isActive", "isEnabled", "isSpawned")) {
                Method m = holo.getClass().getMethod(methodName);
                Object result = m.invoke(holo);
                if (result instanceof Boolean b) return b;
            }
        } catch (Exception ignored) {}
        return true;
    }

    public boolean isRespawningAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return respawningLocations.stream().anyMatch(l ->
                l.getWorld().equals(loc.getWorld()) &&
                l.getBlockX() == loc.getBlockX() &&
                l.getBlockY() == loc.getBlockY() &&
                l.getBlockZ() == loc.getBlockZ());
    }

    private String parsePlaceholders(String text, StoneManager.MetinStone stone, Player player,
                                     double current, double max, List<String> topDamagers) {
        if (text == null) return "";
        String stonePlain = stone.displayName != null ? stone.displayName : stone.id;
        String stoneColored = stone.coloredName != null ? ChatColor.translateAlternateColorCodes('&', stone.coloredName) : stonePlain;
        text = text.replace("{name}", stonePlain)
                .replace("{stone}", stoneColored)
                .replace("{current}", String.valueOf((int) current))
                .replace("{max}", String.valueOf((int) max));
        if (text.contains("{last-breaker}")) {
            if (player != null) text = text.replace("{last-breaker}", player.getName());
            else text = text.replace("{last-breaker}", plugin.getLang().get("hologram.no-damage-yet"));
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> expandLine(String line, StoneManager.MetinStone stone, Player player,
                                    double current, double max, List<String> topDamagers) {
        if (line == null) return List.of("");
        String pre = parsePlaceholders(line, stone, player, current, max, topDamagers);
        if (!pre.contains("{topdamagers}")) {
            return List.of(pre);
        }
        if (topDamagers != null && !topDamagers.isEmpty()) {
            List<String> out = new ArrayList<>();
            if (pre.trim().equals("{topdamagers}")) {
                out.addAll(topDamagers);
                return out;
            }
            out.add(pre.replace("{topdamagers}", topDamagers.get(0)));
            for (int i = 1; i < topDamagers.size(); i++) {
                out.add(topDamagers.get(i));
            }
            return out;
        } else {
            return List.of(pre.replace("{topdamagers}", plugin.getLang().get("hologram.no-damage-yet")));
        }
    }

    public void removeHologram(String id) {
        Hologram holo = activeHolograms.remove(id);
        if (holo != null) {
            holo.delete();
            if (plugin.getConfig().getBoolean("debug", true))
                plugin.getLogger().info(plugin.getLang().get("debug.hologram-removed").replace("{id}", id));
        }
        String uid = id != null && id.startsWith("vxmetin_") ? id.substring(8) : id;
        if (uid != null && !uid.isEmpty()) {
            BukkitRunnable task = respawnTasks.remove(uid);
            if (task != null) task.cancel();
            StoneSpawnManager sm = plugin.getSpawnManager();
            Location known = sm != null ? sm.getKnownLocation(uid) : null;
            if (known != null) {
                respawningLocations.removeIf(l ->
                        l.getWorld().equals(known.getWorld()) &&
                        l.getBlockX() == known.getBlockX() &&
                        l.getBlockY() == known.getBlockY() &&
                        l.getBlockZ() == known.getBlockZ());
            }
        }
    }

    public void removeAll() {
        activeHolograms.values().forEach(Hologram::delete);
        activeHolograms.clear();
        respawningLocations.clear();
        for (BukkitRunnable r : respawnTasks.values()) {
            try { r.cancel(); } catch (Exception ignored) {}
        }
        respawnTasks.clear();
        plugin.getLogger().info(plugin.getLang().get("debug.hologram-cleared"));
    }

    public Map<String, Hologram> getActiveHolograms() {
        return activeHolograms;
    }

    public void cancelRespawn(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty()) return;
        BukkitRunnable r = respawnTasks.remove(uniqueId);
        if (r != null) r.cancel();
        StoneSpawnManager sm = plugin.getSpawnManager();
        Location known = sm != null ? sm.getKnownLocation(uniqueId) : null;
        if (known != null) {
            respawningLocations.removeIf(l ->
                    l.getWorld().equals(known.getWorld()) &&
                    l.getBlockX() == known.getBlockX() &&
                    l.getBlockY() == known.getBlockY() &&
                    l.getBlockZ() == known.getBlockZ());
        }
        removeHologram("vxmetin_" + uniqueId);
    }
}
