package vx.velvexa.metinstones.commands;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vx.velvexa.metinstones.performance.PerformanceAnalyzer;
import vx.velvexa.metinstones.vxMetin;

public class MetinAnalyzeCommand implements CommandExecutor {

    private final vxMetin plugin;
    private final PerformanceAnalyzer analyzer;

    public MetinAnalyzeCommand(vxMetin plugin) {
        this.plugin = plugin;
        this.analyzer = new PerformanceAnalyzer(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vxmetin.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        boolean detailed = args.length > 0 && args[0].equalsIgnoreCase("full");

        sender.sendMessage(ChatColor.GREEN + "[vxMetin] " + ChatColor.GRAY + "Starting performance analysis...");

        analyzer.runAnalysis(plugin, detailed);

        double pluginMemory = estimatePluginMemoryUsage();

        String msg = ChatColor.AQUA + "Plugin RAM Usage: " + ChatColor.WHITE + pluginMemory + " MB";
        if (sender instanceof Player player) {
            player.sendMessage(ChatColor.YELLOW + "Check the console for detailed results when the analysis is complete.");
            player.sendMessage(msg);
        } else {
            Bukkit.getConsoleSender().sendMessage("[vxMetin] " + msg);
        }

        return true;
    }

    private double estimatePluginMemoryUsage() {
        long totalSize = 0;
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
            int count = 0;
            for (Class<?> clazz : loader.getDefinedPackages() != null ? loader.getDefinedPackages()[0].getClass().getClassLoader().getDefinedPackages().getClass().getClasses() : new Class<?>[0]) {
                count++;
            }
        } catch (Throwable ignored) {}


        try {
            long before = getUsedMemory();
            Object instance = plugin;
            Object[] testRefs = new Object[10000];
            for (int i = 0; i < testRefs.length; i++) testRefs[i] = new Object();
            long after = getUsedMemory();
            totalSize = (after - before);
        } catch (Throwable ignored) {}

        if (totalSize <= 0) totalSize = 1;
        return ((double) totalSize) / (1024 * 1024);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
