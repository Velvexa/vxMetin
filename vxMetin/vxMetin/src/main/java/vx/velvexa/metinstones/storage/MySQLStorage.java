package vx.velvexa.metinstones.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import vx.velvexa.metinstones.vxMetin;

public class MySQLStorage extends DataStorage {

    private HikariDataSource hikari;
    private String table;

    public MySQLStorage(vxMetin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String db = plugin.getConfig().getString("storage.mysql.database", "vxmetin");
        String user = plugin.getConfig().getString("storage.mysql.username", "root");
        String pass = plugin.getConfig().getString("storage.mysql.password", "");
        table = plugin.getConfig().getString("storage.mysql.table", "vxmetin_stones");

        boolean hikariEnabled = plugin.getConfig().getBoolean("storage.mysql.hikari.enabled", true);
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true" +
                "&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true";

        try {
            if (hikariEnabled) {
                HikariConfig cfg = new HikariConfig();
                cfg.setJdbcUrl(jdbcUrl);
                cfg.setUsername(user);
                cfg.setPassword(pass);
                cfg.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.hikari.maximum-pool-size", 10));
                cfg.setMinimumIdle(plugin.getConfig().getInt("storage.mysql.hikari.minimum-idle", 2));
                cfg.setConnectionTimeout(10000);
                cfg.setIdleTimeout(60000);
                cfg.setMaxLifetime(600000);
                cfg.addDataSourceProperty("cachePrepStmts", "true");
                cfg.addDataSourceProperty("prepStmtCacheSize", "250");
                cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                cfg.setConnectionTestQuery("SELECT 1");

                hikari = new HikariDataSource(cfg);
                plugin.getLogger().info(plugin.getLang().get("console.hikari-started"));
            } else {
                plugin.getLogger().info(plugin.getLang().get("console.hikari-disabled"));
                hikari = null;
            }

            try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                        "`unique_id` VARCHAR(64) PRIMARY KEY," +
                        "`stone_id` VARCHAR(64)," +
                        "`world` VARCHAR(64)," +
                        "`x` DOUBLE," +
                        "`y` DOUBLE," +
                        "`z` DOUBLE," +
                        "`placer` VARCHAR(64)," +
                        "`timestamp` BIGINT" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
            }

            plugin.getLogger().info(plugin.getLang().get("console.mysql-connected").replace("{table}", table));
            debug(plugin.getLang().get("console.mysql-debug").replace("{url}", jdbcUrl));

        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getLang().get("console.mysql-connection-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public void saveStone(String uniqueId, String stoneId, Location loc, String placer) {
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.mysql-save-invalid-location")
                    .replace("{uid}", uniqueId));
            return;
        }

        String sql = "INSERT INTO `" + table + "` (unique_id, stone_id, world, x, y, z, placer, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE stone_id=VALUES(stone_id), world=VALUES(world), " +
                "x=VALUES(x), y=VALUES(y), z=VALUES(z), placer=VALUES(placer), timestamp=VALUES(timestamp)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            ps.setString(2, stoneId);
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getBlockX() + 0.5);
            ps.setDouble(5, loc.getBlockY());
            ps.setDouble(6, loc.getBlockZ() + 0.5);
            ps.setString(7, placer);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();

            debug(plugin.getLang().get("console.mysql-saved")
                    .replace("{uid}", uniqueId)
                    .replace("{world}", loc.getWorld().getName()));
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.mysql-save-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public void deleteStone(String uniqueId) {
        String sql = "DELETE FROM `" + table + "` WHERE unique_id=?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            ps.executeUpdate();
            debug(plugin.getLang().get("console.mysql-deleted").replace("{uid}", uniqueId));
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.mysql-delete-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public Map<String, Location> loadAll() {
        Map<String, Location> map = new HashMap<>();
        String sql = "SELECT unique_id, world, x, y, z FROM `" + table + "`";

        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("unique_id");
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning(plugin.getLang().get("console.world-not-found")
                            .replace("{world}", worldName)
                            .replace("{uid}", id));
                    continue;
                }

                map.put(id, new Location(world, x, y, z));
            }

            debug(plugin.getLang().get("console.mysql-loaded").replace("{count}", String.valueOf(map.size())));
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.mysql-read-error")
                    .replace("{error}", e.getMessage()));
        }

        return map;
    }

    @Override
    public void close() {
        if (hikari != null && !hikari.isClosed()) {
            hikari.close();
            debug(plugin.getLang().get("console.hikari-closed"));
        }
    }

    private Connection getConnection() throws SQLException {
        if (hikari != null) return hikari.getConnection();

        String host = plugin.getConfig().getString("storage.mysql.host");
        int port = plugin.getConfig().getInt("storage.mysql.port");
        String db = plugin.getConfig().getString("storage.mysql.database");
        String user = plugin.getConfig().getString("storage.mysql.username");
        String pass = plugin.getConfig().getString("storage.mysql.password");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true" +
                "&characterEncoding=UTF-8&serverTimezone=UTC";

        return DriverManager.getConnection(url, user, pass);
    }
}
