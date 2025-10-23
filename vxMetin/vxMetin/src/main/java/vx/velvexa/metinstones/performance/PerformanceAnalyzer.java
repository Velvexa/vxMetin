package vx.velvexa.metinstones.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import vx.velvexa.metinstones.hologram.HologramManager;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.vxMetin;

public class PerformanceAnalyzer {

    private final vxMetin plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");

    private long baselineMemory = -1;

    public PerformanceAnalyzer(vxMetin plugin) {
        this.plugin = plugin;
    }

    public String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long max = runtime.maxMemory() / (1024 * 1024);
        return used + " MB / " + max + " MB";
    }

    public long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    public String getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double load = 0.0;
        try {
            load = (double) osBean.getSystemLoadAverage();
        } catch (Exception ignored) {}
        return (load >= 0 ? df.format(load * 100 / osBean.getAvailableProcessors()) : "N/A") + "%";
    }

    public String getTPS() {
        double[] tps = getPaperTPS();
        return df.format(tps[0]);
    }

    private double[] getPaperTPS() {
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method method = server.getClass().getMethod("getTPS");
            return (double[]) method.invoke(server);
        } catch (Exception e) {
            return new double[]{20.0};
        }
    }

    public int getActiveStoneCount() {
        StoneSpawnManager manager = plugin.getSpawnManager();
        return (manager != null) ? manager.getActiveStones().size() : 0;
    }

    public int getActiveHologramCount() {
        HologramManager hm = plugin.getHologramManager();
        try {
            java.lang.reflect.Field field = hm.getClass().getDeclaredField("activeHolograms");
            field.setAccessible(true);
            Object map = field.get(hm);
            if (map instanceof java.util.Map<?, ?> m) {
                return m.size();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public int getTaskCount() {
        return Bukkit.getScheduler().getActiveWorkers().size()
                + Bukkit.getScheduler().getPendingTasks().size();
    }


    public double getPluginMemoryUsageMB() {
        long before = getUsedMemory();
        try {

            Object[] tmp = new Object[50000];
            for (int i = 0; i < tmp.length; i++) tmp[i] = new Object();
        } catch (Throwable ignored) {}
        long after = getUsedMemory();
        long diff = after - before;
        if (diff < 0) diff = 0;
        return ((double) diff) / (1024 * 1024);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public void startMemoryBenchmark(Plugin requester) {
        baselineMemory = getUsedMemoryMB();
        if (requester instanceof org.bukkit.entity.Player player)
            player.sendMessage("§a[vxMetin] §7Memory benchmark started. Baseline: §f" + baselineMemory + " MB");
        else
            Bukkit.getConsoleSender().sendMessage("[vxMetin] Memory benchmark started. Baseline: " + baselineMemory + " MB");
    }

    public void stopMemoryBenchmark(Plugin requester) {
        if (baselineMemory == -1) {
            if (requester instanceof org.bukkit.entity.Player player)
                player.sendMessage("§c[vxMetin] No baseline found. Use /metin memstart first.");
            else
                Bukkit.getConsoleSender().sendMessage("[vxMetin] No baseline found. Use /metin memstart first.");
            return;
        }

        long current = getUsedMemoryMB();
        long diff = current - baselineMemory;
        String status = diff >= 0 ? "+" + diff : String.valueOf(diff);
        baselineMemory = -1;

        if (requester instanceof org.bukkit.entity.Player player)
            player.sendMessage("§a[vxMetin] §7Average plugin memory usage since last check: §f" + status + " MB");
        else
            Bukkit.getConsoleSender().sendMessage("[vxMetin] Average plugin memory usage since last check: " + status + " MB");
    }

    public void runAnalysis(Plugin requester, boolean detailed) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String ram = getMemoryUsage();
                String cpu = getCpuUsage();
                String tps = getTPS();
                int stones = getActiveStoneCount();
                int holograms = getActiveHologramCount();
                int tasks = getTaskCount();
                double pluginRAM = getPluginMemoryUsageMB();

                Bukkit.getConsoleSender().sendMessage("§a[vxMetin] §fPerformance Analysis Report");
                Bukkit.getConsoleSender().sendMessage("§7──────────────────────────────");
                Bukkit.getConsoleSender().sendMessage("§eActive Stones: §f" + stones);
                Bukkit.getConsoleSender().sendMessage("§eActive Holograms: §f" + holograms);
                Bukkit.getConsoleSender().sendMessage("§eScheduler Tasks: §f" + tasks);
                Bukkit.getConsoleSender().sendMessage("§ePlugin RAM Usage: §f" + df.format(pluginRAM) + " MB");
                Bukkit.getConsoleSender().sendMessage("§eServer RAM Usage: §f" + ram);
                Bukkit.getConsoleSender().sendMessage("§eCPU Load: §f" + cpu);
                Bukkit.getConsoleSender().sendMessage("§eTPS: §f" + tps);
                Bukkit.getConsoleSender().sendMessage("§7──────────────────────────────");

                if (detailed) {
                    Bukkit.getConsoleSender().sendMessage("§8[Detailed Mode Enabled]");
                    Bukkit.getConsoleSender().sendMessage("§8Plugin Version: " + plugin.getDescription().getVersion());
                    Bukkit.getConsoleSender().sendMessage("§8Total Worlds: " + Bukkit.getWorlds().size());
                    Bukkit.getConsoleSender().sendMessage("§8Online Players: " + Bukkit.getOnlinePlayers().size());
                }

                if (requester instanceof org.bukkit.entity.Player player) {
                    player.sendMessage("§a[vxMetin] §7Performance Snapshot:");
                    player.sendMessage("§ePlugin RAM Usage: §f" + df.format(pluginRAM) + " MB");
                    player.sendMessage("§eServer RAM: §f" + ram);
                    player.sendMessage("§eCPU: §f" + cpu);
                    player.sendMessage("§eTPS: §f" + tps);
                    player.sendMessage("§eActive Stones: §f" + stones);
                    player.sendMessage("§eTasks: §f" + tasks);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
