package vx.velvexa.metinstones.modelengine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import vx.velvexa.metinstones.vxMetin;

public class ModelEngineManager {

    private final vxMetin plugin;
    private final boolean available;
    private final Map<String, Object> activeModels = new ConcurrentHashMap<>();
    private Object apiInstance = null;
    private boolean usingV4 = false;

    public ModelEngineManager(vxMetin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("ModelEngine");

        if (available) {
            try {
                Class<?> apiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                apiInstance = apiClass.getMethod("getAPI").invoke(null);
                usingV4 = true;
                plugin.getLogger().info("[vxMetin] ModelEngine v4 API detected and linked successfully.");
            } catch (Exception e1) {
                try {
                    Class<?> legacyApi = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                    apiInstance = legacyApi.getMethod("getInstance").invoke(null);
                    usingV4 = false;
                    plugin.getLogger().info("[vxMetin] ModelEngine v3 API detected and linked successfully.");
                } catch (Exception e2) {
                    plugin.getLogger().warning("[vxMetin] ModelEngine detected but API could not be initialized.");
                    apiInstance = null;
                }
            }
        } else {
            plugin.getLogger().info("[vxMetin] ModelEngine not found. Model support disabled.");
        }
    }

    public boolean isAvailable() {
        return available && apiInstance != null;
    }

    public boolean spawnModel(String modelId, Location location, String uniqueId) {
        if (!isAvailable() || modelId == null || modelId.isEmpty() || location == null || location.getWorld() == null)
            return false;

        try {
            if (usingV4) {
                Class<?> apiClass = apiInstance.getClass();
                Object activeModel = apiClass.getMethod("createActiveModel", String.class).invoke(apiInstance, modelId);
                Object modelEntity = apiClass.getMethod("createModelEntity", Location.class, Object.class)
                        .invoke(apiInstance, location, activeModel);
                activeModels.put(uniqueId, modelEntity);
                plugin.getLogger().info("[vxMetin] Spawned ModelEngine v4 model '" + modelId + "' at " + formatLoc(location));
                return true;
            } else {
                Class<?> apiClass = apiInstance.getClass();
                Object activeModel = apiClass.getMethod("createActiveModel", String.class).invoke(apiInstance, modelId);
                Object modelEntity = apiClass.getMethod("createModelEntity", Location.class, Object.class)
                        .invoke(apiInstance, location, activeModel);
                activeModels.put(uniqueId, modelEntity);
                plugin.getLogger().info("[vxMetin] Spawned ModelEngine v3 model '" + modelId + "' at " + formatLoc(location));
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[vxMetin] Failed to spawn ModelEngine model: " + e.getMessage());
            return false;
        }
    }

    public void removeModel(String uniqueId) {
        if (!isAvailable() || !activeModels.containsKey(uniqueId)) return;
        Object modelEntity = activeModels.remove(uniqueId);
        if (modelEntity == null) return;

        try {
            modelEntity.getClass().getMethod("remove").invoke(modelEntity);
            plugin.getLogger().info("[vxMetin] Removed ModelEngine model for " + uniqueId);
        } catch (Exception e) {
            plugin.getLogger().warning("[vxMetin] Failed to remove ModelEngine model: " + e.getMessage());
        }
    }

    public void removeAllModels() {
        if (!isAvailable()) return;
        for (String id : activeModels.keySet()) {
            removeModel(id);
        }
        activeModels.clear();
    }

    public boolean hasModel(String uniqueId) {
        return activeModels.containsKey(uniqueId);
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + " [" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]";
    }
}
