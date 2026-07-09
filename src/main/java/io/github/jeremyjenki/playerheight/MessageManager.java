package io.github.jeremyjenki.playerheight;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageManager {

    private final PlayerHeight plugin;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private YamlConfiguration lang;
    private String prefix;

    public MessageManager(PlayerHeight plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    private static final String[] BUNDLED_LOCALES = { "en_US", "de_DE", "pl_PL", "ru_RU" };

    public void load(String locale) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        langDir.mkdirs();

        for (String bundledLocale : BUNDLED_LOCALES) {
            File f = new File(langDir, bundledLocale + ".yml");
            if (!f.exists()) {
                InputStream in = plugin.getResource("lang/" + bundledLocale + ".yml");
                if (in != null) {
                    try (OutputStream out = new FileOutputStream(f)) {
                        in.transferTo(out);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Could not extract lang/" + bundledLocale + ".yml", e);
                    }
                }
            }
        }

        File langFile = new File(langDir, locale + ".yml");
        if (langFile.exists()) {
            lang = YamlConfiguration.loadConfiguration(langFile);
            prefix = get("prefix");
            logger.info("Loaded language: " + locale);
            return;
        }

        logger.warning("Language file '" + locale + ".yml' not found — falling back to en_US.");
        File fallbackFile = new File(langDir, "en_US.yml");
        if (fallbackFile.exists()) {
            lang = YamlConfiguration.loadConfiguration(fallbackFile);
            prefix = get("prefix");
            return;
        }

        InputStream fallback = plugin.getResource("lang/en_US.yml");
        if (fallback != null) {
            lang = YamlConfiguration.loadConfiguration(new InputStreamReader(fallback, StandardCharsets.UTF_8));
            prefix = get("prefix");
            return;
        }

        logger.severe("Bundled en_US.yml missing — messages will be empty.");
        lang = new YamlConfiguration();
        prefix = "";
    }

    private String get(String key) {
        return lang.getString(key, key);
    }

    public boolean hasKey(String key) {
        return lang.contains(key);
    }

    // Substitutes placeholders in a raw string without deserializing to a Component.
    private String substitute(String key, Map<String, String> placeholders) {
        String raw = get(key).replace("{prefix}", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }

    public String getRaw(String key, Map<String, String> placeholders) {
        return substitute(key, placeholders);
    }

    public Component resolve(String key, Map<String, String> placeholders) {
        return mm.deserialize(substitute(key, placeholders))
                .decoration(TextDecoration.ITALIC, false);
    }

    public Component resolve(String key) {
        return resolve(key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(resolve(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public static Map<String, String> of(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
