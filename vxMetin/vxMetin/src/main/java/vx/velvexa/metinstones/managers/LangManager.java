package vx.velvexa.metinstones.managers;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import vx.velvexa.metinstones.vxMetin;

public class LangManager {

    private final vxMetin plugin;
    private FileConfiguration langConfig;
    private FileConfiguration defaultLang;
    private String activeLocale;
    private final Map<String, String> cache = new HashMap<>();

    private static final String[] SUPPORTED_LANGS = {"en_US", "tr_TR", "de_DE", "es_ES"};

    public LangManager(vxMetin plugin) {
        this.plugin = plugin;


        for (String lang : SUPPORTED_LANGS) {
            File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + lang + ".yml", false);
                plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                        "&aLanguage file created: &e" + lang + ".yml"));
            }
        }

  
        String initialLocale = plugin.getConfig().getString("language", "en_US");
        loadLanguage(initialLocale);
    }

 
    public void loadLanguage(String locale) {
        this.activeLocale = locale;
        cache.clear();

        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
            plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                    "&aLanguage file created: &e" + locale + ".yml"));
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);


        InputStream defaultStream = plugin.getResource("lang/en_US.yml");
        if (defaultStream != null) {
            defaultLang = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultLang);
        }

        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                "&aLoaded language file: &e" + locale));
    }


    public String get(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        String text = null;

        if (langConfig != null) {
            text = langConfig.getString(path);
        }

        if ((text == null || text.isEmpty()) && defaultLang != null) {
            text = defaultLang.getString(path);
        }

        if (text == null) {
            text = "&cMissing lang key: &f" + path;
            plugin.getLogger().warning("[LangManager] Missing key: " + path);
        }

        text = ChatColor.translateAlternateColorCodes('&', text);
        cache.put(path, text);
        return text;
    }

    private String getOrDefault(String path, String fallback) {
        String value = null;

        if (langConfig != null) value = langConfig.getString(path);
        if ((value == null || value.isEmpty()) && defaultLang != null)
            value = defaultLang.getString(path);
        if (value == null || value.isEmpty()) value = fallback;

        return ChatColor.translateAlternateColorCodes('&', value);
    }


    public void reload() {
        plugin.reloadConfig(); 
        String newLocale = plugin.getConfig().getString("language", activeLocale);

        if (!newLocale.equalsIgnoreCase(activeLocale)) {
            plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                    "&eDetected language change in config: &f" + activeLocale + " &7→ &a" + newLocale));
        }

        loadLanguage(newLocale);

        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                "&eLanguage reloaded: &f" + newLocale));
    }

    public String getActiveLocale() {
        return activeLocale;
    }
}
