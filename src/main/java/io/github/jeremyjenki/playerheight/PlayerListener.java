package io.github.jeremyjenki.playerheight;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    private final PlayerHeight plugin;
    private final NamespacedKey scaleDeltaKey;

    public PlayerListener(PlayerHeight plugin) {
        this.plugin = plugin;
        this.scaleDeltaKey = new NamespacedKey(plugin, "scale_delta");
    }

    public NamespacedKey getScaleDeltaKey() {
        return scaleDeltaKey;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        plugin.getDatabase().loadScale(player.getUniqueId()).ifPresentOrElse(stored -> {
            double rounded = plugin.getAttributeConfig().round(stored);
            if (rounded != stored) {
                plugin.getDatabase().saveScale(player.getUniqueId(), rounded);
            }
            plugin.getApplier().applyProfile(player,
                    plugin.getWorldConfig().effectiveScale(rounded, worldName));
        }, () -> {
            plugin.getApplier().applyProfile(player,
                    plugin.getWorldConfig().effectiveScale(1.0, worldName));
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        double stored = plugin.getDatabase().loadScale(player.getUniqueId()).orElse(1.0);
        plugin.getApplier().applyProfile(player,
                plugin.getWorldConfig().effectiveScale(stored, player.getWorld().getName()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(scaleDeltaKey, PersistentDataType.DOUBLE)) return;

        double delta = pdc.get(scaleDeltaKey, PersistentDataType.DOUBLE);
        double currentScale = plugin.getDatabase().loadScale(player.getUniqueId()).orElse(1.0);
        double newScale = applyDeltaAndClamp(currentScale, delta);

        plugin.getDatabase().saveScale(player.getUniqueId(), newScale);

        // already saved above, this is just for display
        double effective = plugin.getWorldConfig().effectiveScale(newScale, player.getWorld().getName());
        plugin.getApplier().applyProfile(player, effective);
        plugin.getFeedback().sendFeedback(player, effective,
                effective - plugin.getWorldConfig().effectiveScale(currentScale, player.getWorld().getName()),
                plugin.getMessages());
    }

    // Builds the full placeholder map for scale-related messages.
    public java.util.Map<String, String> scalePlaceholders(Player player, double scale) {
        double cm = scale * 182.88;
        return MessageManager.of(
                "player",          player.getName(),
                "scale",           String.valueOf(scale),
                "height",          plugin.getFeedback().formatHeight(scale),
                "height_cm",       (int) Math.round(cm) + "cm",
                "height_imperial", FeedbackManager.formatImperial(cm));
    }

    public double applyDeltaAndClamp(double current, double delta) {
        double result = current + delta;
        result = Math.max(plugin.getAttributeConfig().getScaleMin(),
                 Math.min(plugin.getAttributeConfig().getScaleMax(), result));
        return plugin.getAttributeConfig().round(result);
    }

    public double clampAndRound(double scale) {
        scale = Math.max(plugin.getAttributeConfig().getScaleMin(),
                Math.min(plugin.getAttributeConfig().getScaleMax(), scale));
        return plugin.getAttributeConfig().round(scale);
    }

    public double applyScale(Player target, String valueArg, boolean silent) {
        boolean isDelta = valueArg.startsWith("+") || valueArg.startsWith("-");
        double oldScale = plugin.getDatabase().loadScale(target.getUniqueId()).orElse(1.0);
        double newScale;
        try {
            double parsed = Double.parseDouble(valueArg);
            newScale = isDelta ? applyDeltaAndClamp(oldScale, parsed) : clampAndRound(parsed);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
        plugin.getDatabase().saveScale(target.getUniqueId(), newScale);
        double effective = plugin.getWorldConfig().effectiveScale(newScale, target.getWorld().getName());
        plugin.getApplier().applyProfile(target, effective);
        if (!silent) plugin.getFeedback().sendFeedback(target, effective,
                effective - plugin.getWorldConfig().effectiveScale(oldScale, target.getWorld().getName()),
                plugin.getMessages());
        return newScale;
    }
}
