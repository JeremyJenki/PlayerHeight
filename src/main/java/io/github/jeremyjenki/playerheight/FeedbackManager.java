package io.github.jeremyjenki.playerheight;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

public class FeedbackManager {

    private static final double BLOCKS_TO_CM = 182.88;
    private static final double CM_TO_INCHES  = 0.393701;

    private boolean actionbarEnabled;
    private boolean particlesEnabled;
    private boolean imperialUnit;

    private Color shrinkColor;
    private Color growColor;
    private Color neutralColor;

    public void load(FileConfiguration config) {
        actionbarEnabled = config.getBoolean("feedback.actionbar", true);
        particlesEnabled = config.getBoolean("feedback.particles", true);
        imperialUnit = config.getString("feedback.height_unit", "cm").equalsIgnoreCase("imperial");

        shrinkColor  = parseHex(config.getString("feedback.shrink_color",  "76E3FF"));
        growColor    = parseHex(config.getString("feedback.grow_color",    "FFA262"));
        neutralColor = parseHex(config.getString("feedback.neutral_color", "C8C8C8"));
    }

    private static Color parseHex(String hex) {
        if (hex == null) return Color.WHITE;
        hex = hex.replace("#", "").trim();
        try {
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    public void sendFeedback(Player player, double newScale, double delta, MessageManager messages) {
        if (actionbarEnabled) sendActionbar(player, newScale, delta, messages);
        if (particlesEnabled) spawnParticles(player, delta);
    }

    private void sendActionbar(Player player, double newScale, double delta, MessageManager messages) {
        String key = delta > 0 ? "actionbar_grow" : delta < 0 ? "actionbar_shrink" : "actionbar_neutral";
        double cm = newScale * 182.88;
        Map<String, String> placeholders = Map.of(
                "height",          formatHeight(newScale),
                "height_cm",       (int) Math.round(cm) + "cm",
                "height_imperial", formatImperial(cm),
                "scale",           String.valueOf(newScale));
        player.sendActionBar(messages.resolve(key, placeholders));
    }

    private void spawnParticles(Player player, double delta) {
        Color colour = delta > 0 ? growColor : delta < 0 ? shrinkColor : neutralColor;
        Particle.DustOptions dust = new Particle.DustOptions(colour, 1.0f);
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.DUST, loc, 20, 0.5, 1.0, 0.5, 0, dust);
    }

    public String formatHeight(double scale) {
        double cm = scale * BLOCKS_TO_CM;
        return imperialUnit ? formatImperial(cm) : (int) Math.round(cm) + "cm";
    }

    public static String formatImperial(double cm) {
        double totalInches = cm * CM_TO_INCHES;
        double rounded = Math.round(totalInches * 2.0) / 2.0;
        int feet = (int) (rounded / 12);
        double inches = rounded % 12;
        String inchStr = (inches == Math.floor(inches))
                ? String.valueOf((int) inches)
                : String.valueOf(inches);
        return feet + "'" + inchStr + "\"";
    }

    public Color getShrinkColor()  { return shrinkColor; }
    public Color getGrowColor()    { return growColor; }
    public Color getNeutralColor() { return neutralColor; }
}
