package vx.velvexa.metinstones.storage;

import java.io.File;
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

import vx.velvexa.metinstones.vxMetin;

public class SQLiteStorage extends DataStorage {

    private Connection conn;

    public SQLiteStorage(vxMetin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        try {
            String rawPath = plugin.getConfig().getString("storage.sqlite.file", "data/vxmetin.db");
            File dbFile = new File(plugin.getDataFolder(), rawPath);
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            conn.setAutoCommit(true);

            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS stones (
                        unique_id TEXT PRIMARY KEY,
                        stone_id TEXT,
                        world TEXT,
                        x REAL,
                        y REAL,
                        z REAL,
                        placer TEXT,
                        timestamp INTEGER
                    )
                    """);
            }

            plugin.getLogger().info(plugin.getLang().get("console.sqlite-loaded")
                    .replace("{file}", dbFile.getName()));
            debug(plugin.getLang().get("console.sqlite-connection")
                    .replace("{path}", dbFile.getAbsolutePath()));

        } catch (SQLException e) {
            plugin.getLogger().severe(plugin.getLang().get("console.sqlite-connection-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public void saveStone(String uniqueId, String stoneId, Location loc, String placer) {
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning(plugin.getLang().get("console.sqlite-invalid-location")
                    .replace("{uid}", uniqueId));
            return;
        }
        if (!isConnectionValid()) reconnect();

        String sql = "REPLACE INTO stones (unique_id, stone_id, world, x, y, z, placer, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            ps.setString(2, stoneId);
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getBlockX() + 0.5);
            ps.setDouble(5, loc.getBlockY());
            ps.setDouble(6, loc.getBlockZ() + 0.5);
            ps.setString(7, placer);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();

            debug(plugin.getLang().get("console.sqlite-saved")
                    .replace("{uid}", uniqueId)
                    .replace("{world}", loc.getWorld().getName())
                    .replace("{x}", String.valueOf(loc.getBlockX() + 0.5))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ() + 0.5)));

        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.sqlite-save-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public void deleteStone(String uniqueId) {
        if (!isConnectionValid()) reconnect();

        String sql = "DELETE FROM stones WHERE unique_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uniqueId);
            ps.executeUpdate();
            debug(plugin.getLang().get("console.sqlite-deleted").replace("{uid}", uniqueId));
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.sqlite-delete-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    @Override
    public Map<String, Location> loadAll() {
        Map<String, Location> map = new HashMap<>();
        if (!isConnectionValid()) reconnect();

        String sql = "SELECT unique_id, world, x, y, z FROM stones";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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

                Location loc = new Location(world, x, y, z);
                map.put(id, loc);
            }

            debug(plugin.getLang().get("console.sqlite-loaded-stones")
                    .replace("{count}", String.valueOf(map.size())));

        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.sqlite-read-error")
                    .replace("{error}", e.getMessage()));
        }

        return map;
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                debug(plugin.getLang().get("console.sqlite-closed"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(plugin.getLang().get("console.sqlite-close-error")
                    .replace("{error}", e.getMessage()));
        }
    }

    private boolean isConnectionValid() {
        try {
            return (conn != null && conn.isValid(1));
        } catch (SQLException e) {
            return false;
        }
    }

    private void reconnect() {
        try {
            plugin.getLogger().info(plugin.getLang().get("console.sqlite-reconnecting"));
            close();
            init();
        } catch (Exception ex) {
            plugin.getLogger().severe(plugin.getLang().get("console.sqlite-reconnect-error")
                    .replace("{error}", ex.getMessage()));
        }
    }
}
