package vx.velvexa.metinstones.webhook;

import java.awt.Color;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WebhookManager {

    private final Plugin plugin;
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final String footerText;
    private final String footerIcon;
    private final boolean includeTimestamp;
    private final String authorName;
    private final String authorIcon;
    private final String thumbnailUrl;

    private final String spawnTitle;
    private final String spawnDesc;
    private final int spawnColor;

    private final String destroyTitle;
    private final String destroyDesc;
    private final int destroyColor;

    public WebhookManager(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;

        this.enabled = config.getBoolean("webhook.enabled", false);
        this.webhookUrl = config.getString("webhook.url", "");
        this.username = config.getString("webhook.username", "vxMetin Logger");
        this.avatarUrl = config.getString("webhook.avatar_url", "");
        this.footerText = config.getString("webhook.footer.text", "vxMetin • Velvexa Technologies");
        this.footerIcon = config.getString("webhook.footer.icon_url", "");
        this.includeTimestamp = config.getBoolean("webhook.footer.include_timestamp", true);
        this.authorName = config.getString("webhook.author.name", "vxMetin Monitoring System");
        this.authorIcon = config.getString("webhook.author.icon_url", "");
        this.thumbnailUrl = config.getString("webhook.thumbnail_url", "");

        this.spawnTitle = config.getString("webhook.messages.spawn.title", "🪨 Metin Stone Spawned");
        this.spawnDesc = config.getString("webhook.messages.spawn.description", "**Stone:** {stone}\n**World:** {world}\n**Location:** {x}, {y}, {z}");
        this.spawnColor = parseColor(config.getString("webhook.messages.spawn.color", "#3BA55D"));

        this.destroyTitle = config.getString("webhook.messages.destroy.title", "💥 Metin Stone Destroyed");
        this.destroyDesc = config.getString("webhook.messages.destroy.description", "**Stone:** {stone}\n**Player:** {player}\n**Location:** {x}, {y}, {z}");
        this.destroyColor = parseColor(config.getString("webhook.messages.destroy.color", "#ED4245"));
    }

    private int parseColor(String hex) {
        try {
            return Color.decode(hex).getRGB() & 0xFFFFFF;
        } catch (Exception e) {
            return 0x2F3136;
        }
    }

    public boolean isEnabled() {
        return enabled && webhookUrl != null && webhookUrl.startsWith("https://discord.com/api/webhooks/");
    }

    public void sendStoneSpawn(String stoneName, String world, int x, int y, int z) {
        if (!isEnabled()) return;
        String desc = spawnDesc
                .replace("{stone}", stoneName)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));
        sendEmbed(spawnTitle, desc, spawnColor);
    }

    public void sendStoneDestroyed(String stoneName, String player, String world, int x, int y, int z) {
        if (!isEnabled()) return;
        String desc = destroyDesc
                .replace("{stone}", stoneName)
                .replace("{player}", player)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));
        sendEmbed(destroyTitle, desc, destroyColor);
    }

    private void sendEmbed(String title, String description, int color) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "vxMetin-WebHook");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                JSONObject embed = new JSONObject();
                embed.put("title", title);
                embed.put("description", description);
                embed.put("color", color);

                if (authorName != null && !authorName.isEmpty()) {
                    JSONObject author = new JSONObject();
                    author.put("name", authorName);
                    if (authorIcon != null && !authorIcon.isEmpty())
                        author.put("icon_url", authorIcon);
                    embed.put("author", author);
                }

                if (thumbnailUrl != null && !thumbnailUrl.isEmpty())
                    embed.put("thumbnail", new JSONObject() {{ put("url", thumbnailUrl); }});

                if (footerText != null && !footerText.isEmpty()) {
                    JSONObject footer = new JSONObject();
                    footer.put("text", footerText);
                    if (footerIcon != null && !footerIcon.isEmpty())
                        footer.put("icon_url", footerIcon);
                    embed.put("footer", footer);
                }

                if (includeTimestamp)
                    embed.put("timestamp", Instant.now().toString());

                JSONArray embeds = new JSONArray();
                embeds.add(embed);

                JSONObject payload = new JSONObject();
                payload.put("username", username);
                if (avatarUrl != null && !avatarUrl.isEmpty())
                    payload.put("avatar_url", avatarUrl);
                payload.put("embeds", embeds);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.toJSONString().getBytes(StandardCharsets.UTF_8));
                }

                connection.getInputStream().close();
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        ChatColor.RED + "[vxMetin] Webhook send failed: " + e.getMessage());
            }
        });
    }
}
