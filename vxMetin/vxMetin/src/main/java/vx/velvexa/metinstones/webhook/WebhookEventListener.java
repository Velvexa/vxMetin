package vx.velvexa.metinstones.webhook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vx.velvexa.metinstones.managers.StoneManager;
import vx.velvexa.metinstones.vxMetin;

public class WebhookEventListener {

    private final vxMetin plugin;

    public WebhookEventListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    public void onStoneSpawn(StoneManager.MetinStone stone, Location loc) {
        if (plugin.getWebhookManager() == null || !plugin.getWebhookManager().isEnabled()) return;
        plugin.getWebhookManager().sendStoneSpawn(
                stone.coloredName,
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    public void onStoneDestroyed(StoneManager.MetinStone stone, Player breaker, Location loc) {
        if (plugin.getWebhookManager() == null || !plugin.getWebhookManager().isEnabled()) return;
        plugin.getWebhookManager().sendStoneDestroyed(
                stone.coloredName,
                breaker != null ? breaker.getName() : "Unknown",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }
}
