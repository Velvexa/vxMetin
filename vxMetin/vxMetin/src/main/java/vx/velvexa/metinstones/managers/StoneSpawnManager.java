package vx.velvexa.metinstones.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import vx.velvexa.metinstones.hologram.HologramManager;
import vx.velvexa.metinstones.storage.DataStorage;
import vx.velvexa.metinstones.vxMetin;

public class StoneSpawnManager {

    private final vxMetin plugin;
    private final StoneManager stoneManager;
    private final DataStorage storage;

    private final Map<UUID, ActiveStone> activeStones = new HashMap<>();
    private final Map<String, UUID> activeByUniqueId = new HashMap<>();
    private final Map<String, Location> lastKnownLocationById = new HashMap<>();

    public StoneSpawnManager(vxMetin plugin, StoneManager stoneManager) {
        this.plugin = plugin;
        this.stoneManager = stoneManager;
        this.storage = plugin.getStorage();

        boolean auto = plugin.getConfig().getBoolean("autospawn.enabled", true);
        int delay = plugin.getConfig().getInt("autospawn.delay-seconds", 20);

        if (auto) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    spawnAll();
                } catch (Exception e) {
                    plugin.getLogger().warning("[vxMetin] "
                            + plugin.getLang().get("console.spawnall-failed").replace("{error}", e.getMessage()));
                    e.printStackTrace();
                }
            }, delay * 20L);

            plugin.getLogger().info("[vxMetin] "
                    + plugin.getLang().get("console.auto-spawn-enabled").replace("{delay}", String.valueOf(delay)));
        }
    }

    public void spawnAll() {
        Map<String, Location> saved = storage.loadAll();
        if (saved == null || saved.isEmpty()) {
            plugin.getLogger().info("[vxMetin] " + plugin.getLang().get("console.spawnall-no-saved-stones"));
            return;
        }

        plugin.getLogger().info("[vxMetin] "
                + plugin.getLang().get("console.spawnall-loading-count").replace("{count}",
                        String.valueOf(saved.size())));

        saved.forEach((uniqueId, loc) -> {
            if (activeByUniqueId.containsKey(uniqueId)) {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[vxMetin] "
                            + plugin.getLang().get("console.spawnall-skip-already-active").replace("{uid}", uniqueId));
                }
                lastKnownLocationById.put(uniqueId, loc.clone());
                return;
            }

            String stoneId = extractStoneIdFromUID(uniqueId);
            StoneManager.MetinStone stone = stoneManager.getStone(stoneId);
            if (stone == null || loc.getWorld() == null) {
                plugin.getLogger().warning("[vxMetin] "
                        + plugin.getLang().get("console.spawnall-load-skipped").replace("{id}", stoneId)
                                .replace("{uid}", uniqueId));
                return;
            }

            boolean usedModel = false;
            if (stone.modelEnabled && plugin.getModelEngineManager() != null && plugin.getModelEngineManager().isAvailable() && stone.modelId != null && !stone.modelId.isEmpty()) {
                usedModel = plugin.getModelEngineManager().spawnModel(stone.modelId, loc, uniqueId);
            }
            if (!usedModel) {
                Block block = loc.getBlock();
                block.setType(stone.material != null ? stone.material : Material.STONE, false);
            }

            ActiveStone active = new ActiveStone(stone, loc.clone(), stone.health, uniqueId);
            activeStones.put(active.uuid, active);
            activeByUniqueId.put(uniqueId, active.uuid);
            lastKnownLocationById.put(uniqueId, loc.clone());

            HologramManager hm = plugin.getHologramManager();
            if (hm != null && stone.hologramEnabled) {
                hm.createHologram(uniqueId, stone, loc.clone());
                hm.updateHologram(uniqueId, stone, stone.health, stone.health, null);
            }

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[vxMetin] "
                        + plugin.getLang().get("console.spawnall-loaded").replace("{stone}", stoneId)
                                .replace("{location}", loc.toString()));
            }
        });
    }

    public void spawnStone(Player placer, StoneManager.MetinStone stone, Location location, String uniqueId) {
        if (stone == null || location == null || location.getWorld() == null)
            return;

        final String finalUniqueId = (uniqueId == null || uniqueId.isEmpty())
                ? stone.id + "_" + UUID.randomUUID().toString().substring(0, 8)
                : uniqueId;

        if (activeByUniqueId.containsKey(finalUniqueId)) {
            removeStone(finalUniqueId);
        }

        final Player finalPlacer = placer;
        final Location finalLoc = location.clone();
        final StoneManager.MetinStone finalStone = stone;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                boolean usedModel = false;
                if (finalStone.modelEnabled && plugin.getModelEngineManager() != null && plugin.getModelEngineManager().isAvailable() && finalStone.modelId != null && !finalStone.modelId.isEmpty()) {
                    usedModel = plugin.getModelEngineManager().spawnModel(finalStone.modelId, finalLoc, finalUniqueId);
                }
                if (!usedModel) {
                    Block block = finalLoc.getBlock();
                    block.setType(finalStone.material != null ? finalStone.material : Material.STONE, false);
                }

                ActiveStone active = new ActiveStone(finalStone, finalLoc.clone(), finalStone.health, finalUniqueId);
                activeStones.put(active.uuid, active);
                activeByUniqueId.put(finalUniqueId, active.uuid);
                lastKnownLocationById.put(finalUniqueId, finalLoc.clone());

                storage.saveStone(finalUniqueId, finalStone.id, finalLoc,
                        finalPlacer != null ? finalPlacer.getName() : "SYSTEM");

                HologramManager hm = plugin.getHologramManager();
                if (hm != null && finalStone.hologramEnabled) {
                    hm.createHologram(finalUniqueId, finalStone, finalLoc.clone());
                    hm.updateHologram(finalUniqueId, finalStone, finalStone.health, finalStone.health, null);
                }

                if (plugin.getLogManager() != null) {
                    plugin.getLogManager().logAction(
                            finalPlacer != null ? finalPlacer.getName() : "SYSTEM",
                            finalUniqueId, "SPAWN",
                            finalLoc.getWorld().getName(),
                            finalLoc.getBlockX(), finalLoc.getBlockY(), finalLoc.getBlockZ());
                }

                String msg = (finalStone.messageSpawn != null && !finalStone.messageSpawn.isEmpty())
                        ? finalStone.messageSpawn
                        : plugin.getLang().get("messages.broadcast-stone-spawned");
                msg = ChatColor.translateAlternateColorCodes('&', msg.replace("{stone}", finalStone.coloredName));
                Bukkit.broadcastMessage(msg);

                if (finalPlacer != null) {
                    ItemStack hand = finalPlacer.getInventory().getItemInMainHand();
                    if (hand != null && hand.getAmount() > 0) {
                        hand.setAmount(hand.getAmount() - 1);
                        finalPlacer.updateInventory();
                    }
                }

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[vxMetin] "
                            + plugin.getLang().get("console.spawnstone-created").replace("{uid}", finalUniqueId)
                                    .replace("{location}", finalLoc.toString()));
                }

                new vx.velvexa.metinstones.webhook.WebhookEventListener(plugin).onStoneSpawn(finalStone, finalLoc);

            } catch (Exception ex) {
                plugin.getLogger().warning("[vxMetin] "
                        + plugin.getLang().get("console.spawnstone-error").replace("{error}", ex.getMessage()));
                ex.printStackTrace();
            }
        });
    }

    public void forceRespawn(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty())
            return;
        Location lastLoc = getKnownLocation(uniqueId);
        if (lastLoc == null || lastLoc.getWorld() == null) {
            plugin.getLogger().warning("[vxMetin] "
                    + plugin.getLang().get("console.force-respawn-location-not-found").replace("{uid}", uniqueId));
            return;
        }

        StoneManager.MetinStone stone = stoneManager.getStone(extractStoneIdFromUID(uniqueId));
        if (stone == null) {
            plugin.getLogger().warning("[vxMetin] "
                    + plugin.getLang().get("console.force-respawn-id-not-found").replace("{uid}", uniqueId));
            return;
        }

        removeStone(uniqueId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnStone(null, stone, lastLoc, uniqueId), 10L);
        plugin.getLogger().info("[vxMetin] "
                + plugin.getLang().get("console.force-respawn-complete").replace("{uid}", uniqueId));
    }

    public void handleStoneBroken(ActiveStone stone, Player breaker) {
        if (stone == null)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getModelEngineManager() != null && plugin.getModelEngineManager().isAvailable()) {
                plugin.getModelEngineManager().removeModel(stone.uniqueId);
            }

            Block block = stone.location.getBlock();
            block.setType(Material.AIR);

            lastKnownLocationById.put(stone.uniqueId, stone.location.clone());

            HologramManager hm = plugin.getHologramManager();
            if (hm != null) {
                hm.removeHologram("vxmetin_" + stone.uniqueId);
                hm.startRespawnCountdown(stone.uniqueId, stone.data, stone.location, stone.data.respawnSeconds);
            }

            String msg = plugin.getLang().get("messages.broadcast-stone-broken")
                    .replace("{stone}", stone.data.coloredName)
                    .replace("{player}",
                            breaker != null ? breaker.getName()
                                    : plugin.getLang().get("messages.unknown-player"));
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logAction(
                        breaker != null ? breaker.getName() : "SYSTEM",
                        stone.uniqueId, "BREAK",
                        stone.location.getWorld().getName(),
                        stone.location.getBlockX(), stone.location.getBlockY(), stone.location.getBlockZ());
            }
        });
    }

    public ActiveStone getStoneAt(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;
        return activeStones.values().stream()
                .filter(a -> a.location.getWorld().equals(loc.getWorld())
                        && a.location.getBlockX() == loc.getBlockX()
                        && a.location.getBlockY() == loc.getBlockY()
                        && a.location.getBlockZ() == loc.getBlockZ())
                .findFirst().orElse(null);
    }

    public boolean isActiveStone(Location loc) {
        return getStoneAt(loc) != null;
    }

    public void respawnStone(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty())
            return;

        removeStone(uniqueId);

        Location lastLoc = getKnownLocation(uniqueId);
        if (lastLoc == null || lastLoc.getWorld() == null) {
            plugin.getLogger().warning("[vxMetin] "
                    + plugin.getLang().get("console.respawn-location-not-found").replace("{uid}", uniqueId));
            return;
        }

        StoneManager.MetinStone stone = stoneManager.getStone(extractStoneIdFromUID(uniqueId));
        if (stone == null) {
            plugin.getLogger().warning("[vxMetin] "
                    + plugin.getLang().get("console.respawn-failed-id-not-found").replace("{uid}", uniqueId));
            return;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[vxMetin] "
                    + plugin.getLang().get("console.respawn-start").replace("{uid}", uniqueId));
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Chunk chunk = lastLoc.getChunk();
                if (!chunk.isLoaded())
                    chunk.load(true);
                boolean usedModel = false;
                if (stone.modelEnabled && plugin.getModelEngineManager() != null && plugin.getModelEngineManager().isAvailable() && stone.modelId != null && !stone.modelId.isEmpty()) {
                    usedModel = plugin.getModelEngineManager().spawnModel(stone.modelId, lastLoc, uniqueId);
                }
                if (!usedModel) {
                    Block block = lastLoc.getBlock();
                    block.setType(stone.material != null ? stone.material : Material.STONE, false);
                }

                spawnStone(null, stone, lastLoc, uniqueId);
                plugin.getLogger().info("[vxMetin] "
                        + plugin.getLang().get("console.respawn-updated-active-list").replace("{uid}", uniqueId));
            } catch (Exception ex) {
                plugin.getLogger().warning("[vxMetin] "
                        + plugin.getLang().get("console.respawn-fix-error").replace("{error}", ex.getMessage()));
            }
        });
    }

    public void removeStone(UUID id) {
        ActiveStone active = activeStones.remove(id);
        if (active == null)
            return;

        activeByUniqueId.values().removeIf(u -> u.equals(id));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getModelEngineManager() != null && plugin.getModelEngineManager().isAvailable()) {
                plugin.getModelEngineManager().removeModel(active.uniqueId);
            }

            Block block = active.location.getBlock();
            block.setType(Material.AIR);
            storage.deleteStone(active.uniqueId);

            HologramManager hm = plugin.getHologramManager();
            if (hm != null) {
                try {
                    hm.removeHologram("vxmetin_" + active.uniqueId);
                } catch (Exception ex) {
                    plugin.getLogger().warning("[vxMetin] "
                            + plugin.getLang().get("console.hologram-remove-failed").replace("{error}",
                                    ex.getMessage()));
                }
            }

            String msg = plugin.getLang().get("messages.broadcast-stone-removed")
                    .replace("{stone}", active.data.coloredName);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));

            plugin.getLogger().info("[vxMetin] "
                    + plugin.getLang().get("console.stone-removed").replace("{uid}", active.uniqueId));
        });
    }

    public void removeStone(String uniqueId) {
        UUID rid = activeByUniqueId.remove(uniqueId);
        if (rid == null)
            return;
        ActiveStone target = activeStones.get(rid);
        if (target == null)
            return;
        removeStone(target.uuid);
    }

    public void removeStoneByUniqueId(String uniqueId) {
        removeStone(uniqueId);
    }

    public ActiveStone getActiveByUniqueId(String uniqueId) {
        if (uniqueId == null)
            return null;
        UUID rid = activeByUniqueId.get(uniqueId);
        return (rid != null) ? activeStones.get(rid) : null;
    }

    public Location getKnownLocation(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty())
            return null;

        ActiveStone active = getActiveByUniqueId(uniqueId);
        if (active != null && active.location != null && active.location.getWorld() != null)
            return active.location.clone();

        Location cached = lastKnownLocationById.get(uniqueId);
        if (cached != null && cached.getWorld() != null)
            return cached.clone();

        Map<String, Location> all = storage.safeLoadAll();
        Location fromStorage = all.get(uniqueId);
        if (fromStorage != null) {
            if (fromStorage.getWorld() == null) {
                World w = (fromStorage.getWorld() != null) ? fromStorage.getWorld() : null;
                if (w == null)
                    return null;
            }
            lastKnownLocationById.put(uniqueId, fromStorage.clone());
            return fromStorage.clone();
        }
        return null;
    }

    public boolean isKnownUniqueId(String uniqueId) {
        return getKnownLocation(uniqueId) != null;
    }

    public Map<String, Location> getCombinedKnownStones() {
        Map<String, Location> combined = new HashMap<>();
        for (ActiveStone a : activeStones.values()) {
            combined.put(a.uniqueId, a.location.clone());
        }
        combined.putAll(lastKnownLocationById);
        return combined;
    }

    public boolean teleportPlayerToUniqueId(Player player, String uniqueId) {
        Location target = getKnownLocation(uniqueId);
        if (player == null || target == null)
            return false;
        if (target.getWorld() == null)
            return false;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                player.closeInventory();
            } catch (Exception ignored) {
            }
            player.teleport(target);
        });
        return true;
    }

    private String extractStoneIdFromUID(String uid) {
        if (uid == null)
            return null;
        int idx = uid.lastIndexOf('_');
        if (idx < 0)
            return uid;
        String suffix = uid.substring(idx + 1);
        if (suffix.length() <= 12) {
            return uid.substring(0, idx);
        }
        return uid;
    }

    public static class ActiveStone {
        public final UUID uuid;
        public final StoneManager.MetinStone data;
        public final Location location;
        public final String uniqueId;
        public double currentHP;

        public ActiveStone(StoneManager.MetinStone data, Location location, double maxHP, String uniqueId) {
            this.uuid = UUID.randomUUID();
            this.data = data;
            this.location = location;
            this.currentHP = maxHP;
            this.uniqueId = uniqueId;
        }

        public void damage(double amount) {
            this.currentHP = Math.max(0, this.currentHP - amount);
        }

        public boolean isDead() {
            return currentHP <= 0;
        }
    }

    public Map<UUID, ActiveStone> getActiveStones() {
        return activeStones;
    }

    public DataStorage getStorage() {
        return storage;
    }
}
