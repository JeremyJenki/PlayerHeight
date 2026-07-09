package io.github.jeremyjenki.playerheight;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AttributeApplier {

    private final AttributeConfig config;
    private final Logger logger;

    private static final String ATTR_SCALE = "scale";

    public AttributeApplier(AttributeConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void applyProfile(Player player, double scale) {
        scale = config.round(scale);
        setAttributeBase(player, ATTR_SCALE, scale);

        if (config.hasAttribute("walk_speed")) {
            float walkSpeed = (float) Math.max(0.0, Math.min(1.0, config.resolve("walk_speed", scale)));
            player.setWalkSpeed(walkSpeed);
        }

        for (String attr : config.getAttributeNames()) {
            if (attr.equals("walk_speed")) continue;
            double value = config.resolve(attr, scale);
            setAttributeBase(player, attr, value);
            if (attr.equals("max_health") && player.getHealth() > value) {
                player.setHealth(Math.max(1.0, value));
            }
        }
    }

    private void setAttributeBase(Player player, String attributeId, double value) {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attributeId));
        if (attribute == null) {
            logger.warning("Unknown attribute: minecraft:" + attributeId);
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            logger.warning("Player " + player.getName() + " does not have attribute: " + attributeId);
            return;
        }
        try {
            instance.setBaseValue(value);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set " + attributeId + " to " + value + " for " + player.getName(), e);
        }
    }
}
