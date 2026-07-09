package io.github.jeremyjenki.playerheight;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PotionFactory {

    private final NamespacedKey scaleDeltaKey;
    private final FeedbackManager feedback;
    private final PlayerHeight plugin;

    public PotionFactory(NamespacedKey scaleDeltaKey, FeedbackManager feedback, PlayerHeight plugin) {
        this.scaleDeltaKey = scaleDeltaKey;
        this.feedback = feedback;
        this.plugin = plugin;
    }

    private static String getTier(double delta) {
        double abs = Math.abs(delta);
        if (abs >= 1.0) return " IV";
        if (abs >= 0.5) return " III";
        if (abs >= 0.1) return " II";
        return "";
    }

    private int getStackSize(double delta) {
        String tier = getTier(delta);
        return switch (tier) {
            case ""     -> plugin.getConfig().getInt("potion_stack_sizes.tier_1", 16);
            case " II"  -> plugin.getConfig().getInt("potion_stack_sizes.tier_2", 4);
            case " III" -> plugin.getConfig().getInt("potion_stack_sizes.tier_3", 1);
            case " IV"  -> plugin.getConfig().getInt("potion_stack_sizes.tier_4", 1);
            default     -> 1;
        };
    }

    public ItemStack createPotion(double delta) {
        boolean shrinking = delta < 0;
        String sign      = delta > 0 ? "+" : "";
        String tier      = getTier(delta);
        String direction = plugin.getMessages().getRaw(
                shrinking ? "direction_shrink" : "direction_grow", Map.of());

        MessageManager msg = plugin.getMessages();
        Map<String, String> lorePlaceholders = MessageManager.of(
                "direction", direction, "tier", tier, "delta", sign + delta);

        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setBasePotionType(PotionType.WATER);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.setColor(shrinking ? feedback.getShrinkColor() : feedback.getGrowColor());
        meta.displayName(msg.resolve("potion_name", MessageManager.of("direction", direction)));

        List<Component> lore = new ArrayList<>();
        int i = 1;
        while (msg.hasKey("potion_lore_" + i)) {
            String raw = msg.getRaw("potion_lore_" + i, lorePlaceholders);
            lore.add(raw.isEmpty()
                    ? Component.empty().decoration(TextDecoration.ITALIC, false)
                    : msg.resolve("potion_lore_" + i, lorePlaceholders));
            i++;
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(scaleDeltaKey, PersistentDataType.DOUBLE, delta);
        meta.setMaxStackSize(getStackSize(delta));
        item.setItemMeta(meta);
        return item;
    }
}
