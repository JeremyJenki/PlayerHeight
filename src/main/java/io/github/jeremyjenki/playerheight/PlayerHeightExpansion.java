package io.github.jeremyjenki.playerheight;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

// Placeholders: %playerheight_scale% | %playerheight_cm% | %playerheight_imperial%
public class PlayerHeightExpansion extends PlaceholderExpansion {

    private final PlayerHeight plugin;

    public PlayerHeightExpansion(PlayerHeight plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "playerheight"; }
    @Override public @NotNull String getAuthor()     { return "JeremyJenki"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        double scale = plugin.getDatabase().loadScale(player.getUniqueId()).orElse(1.0);
        double cm = scale * 182.88;
        return switch (params.toLowerCase()) {
            case "scale"    -> String.valueOf(scale);
            case "cm"       -> String.valueOf((int) Math.round(cm));
            case "imperial" -> FeedbackManager.formatImperial(cm);
            default         -> null;
        };
    }
}
