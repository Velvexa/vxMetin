package vx.velvexa.metinstones.storage;

import vx.velvexa.metinstones.vxMetin;

public final class StorageFactory {

    private StorageFactory() {}

    public static DataStorage create(vxMetin plugin) {
        String type = "YAML";
        try {
            type = plugin.getConfig().getString("storage.type", "yaml").trim().toUpperCase();
        } catch (Exception ignored) {}

        DataStorage storage = null;

        try {
            switch (type) {
                case "SQLITE" -> {
                    plugin.getLogger().info(plugin.getLang().get("console.storage-selected-sqlite"));
                    storage = new SQLiteStorage(plugin);
                }
                case "MYSQL" -> {
                    plugin.getLogger().info(plugin.getLang().get("console.storage-selected-mysql"));
                    storage = new MySQLStorage(plugin);
                }
                case "YAML" -> {
                    plugin.getLogger().info(plugin.getLang().get("console.storage-selected-yaml"));
                    storage = new YamlStorage(plugin);
                }
                default -> {
                    plugin.getLogger().warning(plugin.getLang().get("console.storage-invalid-type")
                            .replace("{type}", type));
                    storage = new YamlStorage(plugin);
                }
            }

            if (storage != null) {
                storage.init();
                plugin.getLogger().info(plugin.getLang().get("console.storage-initialized")
                        .replace("{type}", type));
            }
            return storage;

        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getLang().get("console.storage-init-error")
                    .replace("{type}", type)
                    .replace("{error}", e.getMessage()));
            e.printStackTrace();

            plugin.getLogger().warning(plugin.getLang().get("console.storage-fallback"));
            DataStorage fallback = new YamlStorage(plugin);
            try {
                fallback.init();
                plugin.getLogger().info(plugin.getLang().get("console.storage-fallback-success"));
            } catch (Exception ex) {
                plugin.getLogger().severe(plugin.getLang().get("console.storage-fallback-failed")
                        .replace("{error}", ex.getMessage()));
            }
            return fallback;
        }
    }
}
