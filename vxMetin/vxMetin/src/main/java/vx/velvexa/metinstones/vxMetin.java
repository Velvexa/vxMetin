package vx.velvexa.metinstones;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import vx.velvexa.metinstones.gui.AdminGUI;
import vx.velvexa.metinstones.hologram.HologramManager;
import vx.velvexa.metinstones.listeners.AddStoneGUIListener;
import vx.velvexa.metinstones.listeners.AdminGUIListener;
import vx.velvexa.metinstones.listeners.MetinBreakListener;
import vx.velvexa.metinstones.listeners.MetinStonePlaceListener;
import vx.velvexa.metinstones.managers.LangManager;
import vx.velvexa.metinstones.managers.LogManager;
import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.managers.StoneSpawnManager;
import vx.velvexa.metinstones.performance.PerformanceAnalyzer;
import vx.velvexa.metinstones.storage.DataStorage;
import vx.velvexa.metinstones.storage.StorageFactory;
import vx.velvexa.metinstones.webhook.WebhookManager;

public final class vxMetin extends JavaPlugin implements TabExecutor {

    private static vxMetin instance;

    private StoneManager stoneManager;
    private StoneSpawnManager spawnManager;
    private LangManager langManager;
    private LogManager logManager;
    private HologramManager hologramManager;
    private DataStorage storage;
    private WebhookManager webhookManager;

    private String activeLocale;
    private boolean debug;

    public static vxMetin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ensureDefaultFiles();
        saveResource("README_lang.md", false);

        this.activeLocale = getConfig().getString("language", "en_US");
        this.debug = getConfig().getBoolean("debug", false);

        getLogger().info("─────────────────────────────");
        getLogger().info(getLangMessage("console.plugin-starting"));

        langManager = new LangManager(this);
        stoneManager = new StoneManager(this);
        hologramManager = new HologramManager(this);
        logManager = new LogManager(this);

        storage = StorageFactory.create(this);
        getLogger().info(getLangMessage("console.storage-active")
                .replace("{type}", getConfig().getString("storage.type", "YAML")));

        spawnManager = new StoneSpawnManager(this, stoneManager);

        webhookManager = new WebhookManager(this, getConfig());
        if (webhookManager.isEnabled()) {
            getLogger().info("[vxMetin] Webhook system enabled.");
        } else {
            getLogger().info("[vxMetin] Webhook system disabled or invalid URL.");
        }

        if (getCommand("metin") != null) {
            getCommand("metin").setExecutor(this);
            getCommand("metin").setTabCompleter(this);
        }

        registerListeners();
        checkForUpdates();

        getLogger().info(getLangMessage("console.plugin-enabled")
                .replace("{locale}", activeLocale));
        getLogger().info("─────────────────────────────");
    }

    @Override
    public void onDisable() {
        getLogger().info("─────────────────────────────");
        getLogger().info(getLangMessage("console.plugin-stopping"));

        if (hologramManager != null) {
            try {
                hologramManager.removeAll();
                getLogger().info(getLangMessage("console.holograms-cleared"));
            } catch (Exception e) {
                getLogger().warning(getLangMessage("console.holograms-failed")
                        .replace("{error}", e.getMessage()));
            }
        }

        if (storage != null) {
            try {
                storage.close();
                getLogger().info(getLangMessage("console.storage-closed"));
            } catch (Exception e) {
                getLogger().warning(getLangMessage("console.storage-close-error")
                        .replace("{error}", e.getMessage()));
            }
        }

        if (logManager != null) {
            try {
                getLogger().info(getLangMessage("console.log-closing"));
            } catch (Exception e) {
                getLogger().warning(getLangMessage("console.log-close-error")
                        .replace("{error}", e.getMessage()));
            }
        }

        getLogger().info(langManager != null
                ? langManager.get("messages.plugin-disabled")
                : "vxMetin disabled.");
        getLogger().info("─────────────────────────────");
    }

    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(new AdminGUIListener(this), this);
            getServer().getPluginManager().registerEvents(new AddStoneGUIListener(this), this);
            getServer().getPluginManager().registerEvents(new MetinStonePlaceListener(this), this);
            getServer().getPluginManager().registerEvents(new MetinBreakListener(this), this);
            log(getLangMessage("console.listeners-registered"));
        } catch (Exception ex) {
            getLogger().severe(getLangMessage("console.listeners-failed")
                    .replace("{error}", ex.getMessage()));
            ex.printStackTrace();
        }
    }

    private void ensureDefaultFiles() {
        saveResourceIfMissing("stones.yml");
        saveResourceIfMissing("lang/en_US.yml");
        saveResourceIfMissing("lang/tr_TR.yml");
        saveResourceIfMissing("lang/de_DE.yml");
        saveResourceIfMissing("lang/es_ES.yml");
    }

    private void saveResourceIfMissing(String path) {
        File f = new File(getDataFolder(), path);
        if (!f.exists()) saveResource(path, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("metin")) return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("messages.console-only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(langManager.get("messages.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin" -> {
                if (!player.hasPermission("vxmetin.admin")) {
                    player.sendMessage(langManager.get("messages.no-permission"));
                    return true;
                }
                player.sendMessage(langManager.get("messages.admin-open"));
                AdminGUI.open(player);
                return true;
            }
            case "reload" -> handleReload(player);
            case "analyze" -> {
                if (!player.hasPermission("vxmetin.admin")) {
                    player.sendMessage(langManager.get("messages.no-permission"));
                    return true;
                }
                boolean detailed = args.length > 1 && args[1].equalsIgnoreCase("full");
                player.sendMessage(ChatColor.GREEN + "[vxMetin] " + ChatColor.GRAY + "Starting performance analysis...");
                PerformanceAnalyzer analyzer = new PerformanceAnalyzer(this);
                analyzer.runAnalysis(this, detailed);
                player.sendMessage(ChatColor.YELLOW + "Check the console for detailed results when the analysis is complete");
                return true;
            }
            default -> player.sendMessage(langManager.get("messages.unknown-subcommand"));
        }
        return true;
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("vxmetin.admin")) {
            player.sendMessage(langManager.get("messages.no-permission"));
            return;
        }

        reloadConfig();
        langManager.reload();
        stoneManager.loadStones();

        try {
            if (storage != null) storage.close();
            storage = StorageFactory.create(this);
        } catch (Exception e) {
            getLogger().warning(getLangMessage("console.reload-storage-failed")
                    .replace("{error}", e.getMessage()));
        }

        double newOffset = getConfig().getDouble("hologram.base-offset", 1.8);
        getLogger().info(getLangMessage("console.hologram-reloaded")
                .replace("{offset}", String.valueOf(newOffset)));

        player.sendMessage(langManager.get("messages.reload-success")
                .replace("{locale}", activeLocale));
        getLogger().info(langManager.get("messages.reload-log")
                .replace("{player}", player.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("metin")) return Collections.emptyList();
        if (args.length == 1) return List.of("admin", "reload", "analyze");
        if (args.length == 2 && args[0].equalsIgnoreCase("analyze")) return List.of("full");
        return Collections.emptyList();
    }

    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String currentVersion = getDescription().getVersion();
                URL url = new URL("https://raw.githubusercontent.com/Velvexa/vxMetin/main/vxMetin/vxMetin/version.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = in.readLine().trim();
                in.close();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    getLogger().warning("────────────────────────────────────");
                    getLogger().warning("vxMetin update available!");
                    getLogger().warning("Current version: " + currentVersion);
                    getLogger().warning("Latest version:  " + latestVersion);
                    getLogger().warning("Download: https://github.com/velvexa/vxMetin/releases");
                    getLogger().warning("────────────────────────────────────");

                    this.getServer().getPluginManager().registerEvents(new Listener() {
                        @EventHandler
                        public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                            Player p = e.getPlayer();
                            if (p.isOp()) {
                                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&6[&evxMetin&6] &cA new version is available! &7(Current: &f" + currentVersion + "&7 / Latest: &a" + latestVersion + "&7)"));
                                p.sendMessage(ChatColor.YELLOW + "Download: " + ChatColor.WHITE + "https://github.com/velvexa/vxMetin/releases");
                            }
                        }
                    }, this);
                } else {
                    getLogger().info("vxMetin is up to date (v" + currentVersion + ")");
                }

            } catch (Exception e) {
                getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    private void log(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }

    private String getLangMessage(String key) {
        if (langManager != null) return langManager.get(key);
        return key;
    }

    public StoneManager getStoneManager() { return stoneManager; }
    public StoneSpawnManager getSpawnManager() { return spawnManager; }
    public LangManager getLang() { return langManager; }
    public LogManager getLogManager() { return logManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public DataStorage getStorage() { return storage; }
    public WebhookManager getWebhookManager() { return webhookManager; }
    public String getActiveLocale() { return activeLocale; }
}
