package io.github.jeremyjenki.playerheight;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScaleCommand implements CommandExecutor, TabCompleter {

    private final PlayerHeight plugin;

    public ScaleCommand(PlayerHeight plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            if (!player.hasPermission("playerheight.check")) {
                plugin.getMessages().send(sender, "no_permission");
                return true;
            }
            double scale = plugin.getDatabase().loadScale(player.getUniqueId()).orElse(1.0);
            plugin.getMessages().send(player, "scale_query_self",
                    plugin.getPlayerListener().scalePlaceholders(player, scale));
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission("playerheight.admin")) {
                plugin.getMessages().send(sender, "no_permission");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessages().send(sender, "player_not_found",
                        MessageManager.of("player", args[0]));
                return true;
            }
            double current = plugin.getDatabase().loadScale(target.getUniqueId()).orElse(1.0);
            plugin.getMessages().send(sender, "scale_query_other",
                    plugin.getPlayerListener().scalePlaceholders(target, current));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessages().send(sender, "player_not_found",
                    MessageManager.of("player", args[0]));
            return true;
        }

        boolean isSelf = sender instanceof Player p && p.equals(target);
        boolean canAdmin = sender.hasPermission("playerheight.admin");
        boolean canSelfSet = isSelf && sender.hasPermission("playerheight.self");

        if (!canAdmin && !canSelfSet) {
            plugin.getMessages().send(sender, "no_permission");
            return true;
        }

        boolean silent = args.length >= 3 && args[2].equalsIgnoreCase("silent");
        double newScale = plugin.getPlayerListener().applyScale(target, args[1], silent);

        if (Double.isNaN(newScale)) {
            plugin.getMessages().send(sender, "invalid_value",
                    MessageManager.of("value", args[1]));
            return true;
        }

        if (isSelf) {
            plugin.getMessages().send(sender, "scale_set_self",
                    plugin.getPlayerListener().scalePlaceholders(target, newScale));
            return true;
        }

        plugin.getMessages().send(sender, "scale_set_other",
                plugin.getPlayerListener().scalePlaceholders(target, newScale));
        if (!silent) {
            plugin.getMessages().send(target, "scale_set_self",
                    plugin.getPlayerListener().scalePlaceholders(target, newScale));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (!sender.hasPermission("playerheight.admin") && !sender.hasPermission("playerheight.self")) {
            return Collections.emptyList();
        }
        if (args.length == 2) return Arrays.asList("-0.5", "-0.1", "-0.01", "+0.01", "+0.1", "+0.5", "1.0");
        if (args.length == 3 && sender.hasPermission("playerheight.admin")) return List.of("silent");
        return Collections.emptyList();
    }
}
