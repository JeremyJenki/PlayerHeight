package io.github.jeremyjenki.playerheight;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// Reads via SnakeYAML -- Bukkit config mangles numeric keys
public class AttributeConfig {

    private double scaleMin;
    private double scaleMax;
    private int precision;
    private double precisionFactor;

    private final Map<String, TreeMap<Double, Double>> keyframes = new LinkedHashMap<>();
    private final Logger logger;

    public AttributeConfig(Logger logger) {
        this.logger = logger;
    }

    private static final Map<String, Double> VANILLA_DEFAULTS = Map.ofEntries(
        Map.entry("walk_speed",                0.2),
        Map.entry("max_health",               20.0),
        Map.entry("attack_damage",             1.0),
        Map.entry("knockback_resistance",      0.0),
        Map.entry("block_break_speed",         1.0),
        Map.entry("jump_strength",             0.42),
        Map.entry("step_height",               0.6),
        Map.entry("safe_fall_distance",        3.0),
        Map.entry("entity_interaction_range",  3.0),
        Map.entry("block_interaction_range",   4.5),
        Map.entry("attack_knockback",          0.0)
    );

    public void load(File configFile, FileConfiguration bukkitConfig) {
        keyframes.clear();

        scaleMin           = bukkitConfig.getDouble("scale.min", 0.1);
        scaleMax           = bukkitConfig.getDouble("scale.max", 2.0);
        precision          = Math.max(1, Math.min(6, bukkitConfig.getInt("scale.precision", 2)));
        precisionFactor    = Math.pow(10, precision);

        Map<String, Object> root;
        try (FileInputStream in = new FileInputStream(configFile)) {
            root = new Yaml().load(in);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read config.yml", e);
            return;
        }

        if (root == null) { logger.warning("config.yml is empty."); return; }

        Object attrsObj = root.get("attributes");
        if (!(attrsObj instanceof Map<?, ?> attrsMap)) {
            logger.warning("No 'attributes' section found in config.yml.");
            return;
        }

        for (Map.Entry<?, ?> attrEntry : attrsMap.entrySet()) {
            String attrName = String.valueOf(attrEntry.getKey());
            if (!(attrEntry.getValue() instanceof Map<?, ?> attrSection)) {
                logger.warning("Attribute '" + attrName + "' is not a section — skipping.");
                continue;
            }
            if (!(attrSection.get("keyframes") instanceof Map<?, ?> kfMap)) {
                logger.warning("Attribute '" + attrName + "' has no valid keyframes — skipping.");
                continue;
            }

            TreeMap<Double, Double> kf = new TreeMap<>();
            for (Map.Entry<?, ?> kfEntry : kfMap.entrySet()) {
                try {
                    kf.put(toDouble(kfEntry.getKey()), toDouble(kfEntry.getValue()));
                } catch (NumberFormatException e) {
                    logger.warning("Invalid keyframe '" + kfEntry.getKey() + ": " + kfEntry.getValue()
                            + "' in '" + attrName + "' — skipping.");
                }
            }

            if (kf.size() < 2) {
                logger.warning("Attribute '" + attrName + "' has fewer than 2 keyframes — skipping.");
                continue;
            }
            keyframes.put(attrName, kf);
        }

        logger.info("Loaded " + keyframes.size() + " keyframe attributes.");
    }

    private static double toDouble(Object obj) throws NumberFormatException {
        if (obj == null) throw new NumberFormatException("null");
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(obj.toString());
    }

    public double resolve(String attrName, double scale) {
        TreeMap<Double, Double> kf = keyframes.get(attrName);
        if (kf == null) {
            double fallback = VANILLA_DEFAULTS.getOrDefault(attrName, 1.0);
            logger.warning("Attribute '" + attrName + "' not in config — using vanilla default " + fallback);
            return fallback;
        }
        if (kf.containsKey(scale)) return kf.get(scale);
        if (scale <= kf.firstKey()) return kf.firstEntry().getValue();
        if (scale >= kf.lastKey())  return kf.lastEntry().getValue();

        Map.Entry<Double, Double> lower = kf.floorEntry(scale);
        Map.Entry<Double, Double> upper = kf.ceilingEntry(scale);
        double t = (scale - lower.getKey()) / (upper.getKey() - lower.getKey());
        return lower.getValue() + t * (upper.getValue() - lower.getValue());
    }

    public boolean hasAttribute(String attrName)  { return keyframes.containsKey(attrName); }
    public Set<String> getAttributeNames()         { return keyframes.keySet(); }
    public double round(double scale)              { return Math.round(scale * precisionFactor) / precisionFactor; }
    public double getScaleMin()                    { return scaleMin; }
    public double getScaleMax()                    { return scaleMax; }
}
