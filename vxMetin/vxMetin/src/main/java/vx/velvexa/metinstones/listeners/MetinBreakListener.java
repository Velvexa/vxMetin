package vx.velvexa.metinstones.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import vx.velvexa.metinstones.hologram.HologramManager;
import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class MetinBreakListener implements Listener {

    private final vxMetin plugin;
    private final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();
    private final Set<Location> safetyLock = Collections.synchronizedSet(new HashSet<>());

    public MetinBreakListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        Location loc = block.getLocation();

        StoneSpawnManager spawnManager = plugin.getSpawnManager();
        HologramManager holo = plugin.getHologramManager();

        boolean isRespawning = !spawnManager.isActiveStone(loc)
                && holo != null
                && holo.isRespawningAt(loc);

        if (isRespawning) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.stone-respawning")));
            debug("Break prevented (respawning): " + loc);
            return;
        }

        synchronized (safetyLock) {
            if (safetyLock.contains(loc)) {
                e.setCancelled(true);
                return;
            }
            safetyLock.add(loc);
        }

        StoneSpawnManager.ActiveStone active = spawnManager.getStoneAt(loc);
        if (active == null) {
            safetyLock.remove(loc);
            return;
        }

        e.setCancelled(true);
        StoneManager.MetinStone stone = active.data;
        if (stone == null) {
            safetyLock.remove(loc);
            return;
        }

        boolean combatEnabled = plugin.getConfig().getBoolean("combat.enabled", true);
        double damageAmount = plugin.getConfig().getDouble("combat.base-damage", 1.0);

        if (!combatEnabled) {
            spawnManager.handleStoneBroken(active, player);
            safetyLock.remove(loc);
            return;
        }

        active.damage(damageAmount);
        damageMap.computeIfAbsent(active.uuid, k -> new HashMap<>())
                .merge(player.getUniqueId(), damageAmount, Double::sum);

        if (holo != null) {
            holo.addDamage(active.uuid, player.getUniqueId(), damageAmount);
        }

        if (holo != null && stone.hologramEnabled) {
            holo.updateHologram(active.uniqueId, stone, active.currentHP, stone.health, player);
        }

        if (!active.isDead()) {
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
            playEffects(loc, stone.effects);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.stone-damage")
                            .replace("{player}", player.getName())
                            .replace("%player%", player.getName())
                            .replace("{stone}", stone.coloredName)
                            .replace("%stone%", stone.coloredName)
                            .replace("{damage}", String.valueOf((int) damageAmount))
                            .replace("%damage%", String.valueOf((int) damageAmount))
                            .replace("{current}", String.valueOf((int) active.currentHP))
                            .replace("%current%", String.valueOf((int) active.currentHP))
                            .replace("{max}", String.valueOf((int) stone.health))
                            .replace("%max%", String.valueOf((int) stone.health))));
            safetyLock.remove(loc);
            return;
        }

        block.setType(Material.BEDROCK);

        playEffects(loc, stone.effects);

        UUID activeId = active.uuid;
        String uniqueId = active.uniqueId;

        Map<UUID, Double> damageData = damageMap.getOrDefault(activeId, new HashMap<>());
        List<Map.Entry<UUID, Double>> sortedList = damageData.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < sortedList.size(); i++) {
            UUID pid = sortedList.get(i).getKey();
            double dmg = sortedList.get(i).getValue();
            Player top = Bukkit.getPlayer(pid);
            if (top == null) continue;

            List<String> rewards;
            switch (i) {
                case 0 -> rewards = stone.rewardsTop1;
                case 1 -> rewards = stone.rewardsTop2;
                case 2 -> rewards = stone.rewardsTop3;
                default -> rewards = stone.rewards;
            }

            if (rewards == null || rewards.isEmpty()) continue;

            for (String cmd : rewards) {
                String formatted = cmd
                        .replace("%player%", top.getName())
                        .replace("{player}", top.getName())
                        .replace("%rank%", String.valueOf(i + 1))
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("%damage%", String.valueOf((int) dmg))
                        .replace("{damage}", String.valueOf((int) dmg))
                        .replace("{stone}", ChatColor.stripColor(stone.displayName))
                        .replace("%stone%", ChatColor.stripColor(stone.displayName));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
            }

            top.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLang().get("messages.top-damager")
                            .replace("{player}", top.getName())
                            .replace("%player%", top.getName())
                            .replace("{rank}", String.valueOf(i + 1))
                            .replace("%rank%", String.valueOf(i + 1))
                            .replace("{damage}", String.valueOf((int) dmg))
                            .replace("%damage%", String.valueOf((int) dmg))));
        }

        String broadcast = (stone.messageDestroy != null && !stone.messageDestroy.isEmpty())
                ? ChatColor.translateAlternateColorCodes('&',
                stone.messageDestroy
                        .replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%stone%", stone.coloredName)
                        .replace("{stone}", stone.coloredName))
                : ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("messages.broadcast-stone-destroyed")
                        .replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%stone%", stone.coloredName)
                        .replace("{stone}", stone.coloredName));

        Bukkit.broadcastMessage(broadcast);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().get("messages.player-stone-destroyed")
                        .replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%stone%", stone.coloredName)
                        .replace("{stone}", stone.coloredName)));

        if (holo != null && stone.hologramEnabled) {
            try {
                holo.removeHologram("vxmetin_" + uniqueId);
            } catch (Exception ignored) {}
            holo.startRespawnCountdown(uniqueId, stone, loc, stone.respawnSeconds);
        }

        spawnManager.getActiveStones().remove(activeId);
        damageMap.remove(activeId);
        safetyLock.remove(loc);

        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logAction(
                    player.getName(),
                    uniqueId,
                    "BREAK",
                    block.getWorld().getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );
        }

        debug("Stone destroyed → ID: " + uniqueId + " | Player: " + player.getName());
    }

    private void playEffects(Location loc, List<String> effects) {
        if (effects == null || effects.isEmpty()) return;
        for (String eff : effects) {
            try {
                Particle particle = Particle.valueOf(eff.toUpperCase());
                loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1, 0.5), 10, 0.3, 0.5, 0.3, 0.01);
            } catch (IllegalArgumentException ignored) {
                try {
                    Effect effect = Effect.valueOf(eff.toUpperCase());
                    loc.getWorld().playEffect(loc, effect, 0);
                } catch (IllegalArgumentException ignored2) {}
            }
        }
    }

    private void debug(String msg) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }
}
