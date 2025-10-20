package vx.velvexa.metinstones.managers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import vx.velvexa.metinstones.vxMetin;

public class LogManager {

    private final vxMetin plugin;
    private final String storageType;
    private File logFile;
    private FileConfiguration logConfig;
    private Connection sqlConnection;
    private String tableName;

    public LogManager(vxMetin plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
        setup();
    }

    private void setup() {
        switch (storageType) {
            case "SQLITE" -> setupSQLite();
            case "MYSQL" -> setupMySQL();
            default -> setupYAML();
        }
    }

    private void setupYAML() {
        logFile = new File(plugin.getDataFolder(), "logs.yml");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                plugin.getLogger().info(plugin.getLang().get("messages.log-file-created"));
            } catch (IOException e) {
                plugin.getLogger().warning(plugin.getLang().get("messages.log-file-create-failed"));
            }
        }
        logConfig = YamlConfiguration.loadConfiguration(logFile);
        plugin.getLogger().info(plugin.getLang().get("messages.log-using-yaml"));
    }

    private void setupSQLite() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();

            String dbPath = plugin.getConfig().getString("storage.sqlite.file", "data/vxmetin.db");
            File dbFile = new File(plugin.getDataFolder(), dbPath);

            sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            tableName = "vxmetin_logs";
            createTable();

            plugin.getLogger().info(plugin.getLang().get("messages.log-sqlite-connected").replace("{file}", dbFile.getName()));
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getLang().get("messages.log-sqlite-failed"));
            e.printStackTrace();
        }
    }

    private void setupMySQL() {
        try {
            var cfg = plugin.getConfig().getConfigurationSection("storage.mysql");
            if (cfg == null) return;

            String host = cfg.getString("host", "localhost");
            int port = cfg.getInt("port", 3306);
            String db = cfg.getString("database", "vxmetin");
            String user = cfg.getString("username", "root");
            String pass = cfg.getString("password", "");
            boolean ssl = cfg.getBoolean("useSSL", false);
            tableName = cfg.getString("table", "vxmetin_logs");

            sqlConnection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl,
                    user,
                    pass
            );
            createTable();

            plugin.getLogger().info(plugin.getLang().get("messages.log-mysql-connected")
                    .replace("{host}", host).replace("{db}", db));
        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getLang().get("messages.log-mysql-failed"));
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        if (sqlConnection == null) return;

        String sql = """
            CREATE TABLE IF NOT EXISTS vxmetin_logs (
                id VARCHAR(36) PRIMARY KEY,
                timestamp TEXT,
                player TEXT,
                stone TEXT,
                action TEXT,
                world TEXT,
                x INTEGER,
                y INTEGER,
                z INTEGER
            )
        """;

        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void logAction(String player, String stone, String action, String world, int x, int y, int z) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        switch (storageType) {
            case "MYSQL", "SQLITE" -> logSQL(player, stone, action, world, x, y, z, timestamp);
            default -> logYAML(player, stone, action, world, x, y, z, timestamp);
        }
    }

    private void logSQL(String player, String stone, String action, String world, int x, int y, int z, String time) {
        if (sqlConnection == null) return;

        String sql = "INSERT INTO " + tableName +
                " (id, timestamp, player, stone, action, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, time);
            ps.setString(3, player);
            ps.setString(4, stone);
            ps.setString(5, action.toUpperCase());
            ps.setString(6, world);
            ps.setInt(7, x);
            ps.setInt(8, y);
            ps.setInt(9, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("messages.log-sql-failed").replace("{error}", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void logYAML(String player, String stone, String action, String world, int x, int y, int z, String time) {
        if (logConfig == null) return;

        String key = "logs." + action.toLowerCase();
        List<String> list = (List<String>) logConfig.getList(key);
        if (list == null) list = new java.util.ArrayList<>();

        String entry = String.format(
                "%s | %s | Player: %s | Stone: %s | World: %s | Loc: %d,%d,%d",
                time, action.toUpperCase(), player, stone, world, x, y, z
        );

        list.add(entry);
        logConfig.set(key, list);

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().warning(plugin.getLang().get("messages.log-save-failed"));
        }
    }

    public void close() {
        try {
            if (sqlConnection != null && !sqlConnection.isClosed()) {
                sqlConnection.close();
                plugin.getLogger().info("LogManager SQL connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing SQL connection: " + e.getMessage());
        }
    }

    public void reload() {
        close();
        setup();
        plugin.getLogger().info(plugin.getLang().get("messages.log-reloaded"));
    }
}
