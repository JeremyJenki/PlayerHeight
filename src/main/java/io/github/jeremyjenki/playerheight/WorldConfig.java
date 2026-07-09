package io.github.jeremyjenki.playerheight;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WorldConfig {

    public record WorldClamp(double min, double max) {
        public double apply(double scale) {
            return Math.max(min, Math.min(max, scale));
        }
    }

    private final Map<String, WorldClamp> clamps = new HashMap<>();
    private final Logger logger;

    public WorldConfig(Logger logger) {
        this.logger = logger;
    }

    public void load(FileConfiguration config) {
        clamps.clear();
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String worldName : worlds.getKeys(false)) {
            ConfigurationSection section = worlds.getConfigurationSection(worldName);
            if (section == null) continue;

            double min = section.getDouble("min_scale", 0.1);
            double max = section.getDouble("max_scale", 2.0);

            if (min > max) {
                logger.warning("World '" + worldName + "' has min_scale > max_scale — skipping.");
                continue;
            }
            clamps.put(worldName, new WorldClamp(min, max));
        }

        if (!clamps.isEmpty()) {
            logger.info("Loaded world clamps for: " + String.join(", ", clamps.keySet()));
        }
    }

    public double effectiveScale(double storedScale, String worldName) {
        WorldClamp clamp = clamps.get(worldName);
        return clamp != null ? clamp.apply(storedScale) : storedScale;
    }
}
