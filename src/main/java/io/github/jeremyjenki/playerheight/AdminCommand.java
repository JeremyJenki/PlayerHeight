package io.github.jeremyjenki.playerheight;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final PlayerHeight plugin;
    private ScaleCommand scaleCommand;

    public AdminCommand(PlayerHeight plugin) {
        this.plugin = plugin;
    }

    public void setScaleCommand(ScaleCommand scaleCommand) {
        this.scaleCommand = scaleCommand;
    }

    private ScaleCommand getScaleCommand() {
        return scaleCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                if (!sender.hasPermission("playerheight.admin")) {
                    plugin.getMessages().send(sender, "no_permission");
                    return true;
                }
                plugin.saveDefaultConfig();
                plugin.reloadConfig();
                plugin.getAttributeConfig().load(
                    new java.io.File(plugin.getDataFolder(), "config.yml"),
                    plugin.getConfig());
                plugin.getFeedback().load(plugin.getConfig());
                plugin.getWorldConfig().load(plugin.getConfig());
                plugin.getMessages().load(plugin.getConfig().getString("language", "en_US"));

                int reapplied = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    double stored = plugin.getDatabase().loadScale(p.getUniqueId()).orElse(1.0);
                    double effective = plugin.getWorldConfig().effectiveScale(stored, p.getWorld().getName());
                    plugin.getApplier().applyProfile(p, effective);
                    reapplied++;
                }
                plugin.getMessages().send(sender, "reload",
                        MessageManager.of("count", String.valueOf(reapplied)));
            }

            case "scale" -> {
                getScaleCommand().onCommand(sender, command, label,
                        Arrays.copyOfRange(args, 1, args.length));
            }

            case "setscale" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                if (!player.hasPermission("playerheight.self") && !player.hasPermission("playerheight.admin")) {
                    plugin.getMessages().send(sender, "no_permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getMessages().send(sender, "usage_setscale");
                    return true;
                }

                double newScale = plugin.getPlayerListener().applyScale(player, args[1], false);

                if (Double.isNaN(newScale)) {
                    plugin.getMessages().send(sender, "invalid_value",
                            MessageManager.of("value", args[1]));
                    return true;
                }

                plugin.getMessages().send(player, "scale_set_self",
                        plugin.getPlayerListener().scalePlaceholders(player, newScale));
            }

            case "give" -> {
                if (!sender.hasPermission("playerheight.admin")) {
                    plugin.getMessages().send(sender, "no_permission");
                    return true;
                }
                if (args.length < 3) {
                    plugin.getMessages().send(sender, "usage_give");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessages().send(sender, "player_not_found",
                            MessageManager.of("player", args[1]));
                    return true;
                }
                double delta;
                try {
                    delta = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    plugin.getMessages().send(sender, "invalid_delta",
                            MessageManager.of("value", args[2]));
                    return true;
                }
                if (delta == 0) {
                    plugin.getMessages().send(sender, "delta_zero");
                    return true;
                }

                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                        if (amount < 1) {
                            plugin.getMessages().send(sender, "invalid_amount",
                                    MessageManager.of("value", args[3]));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        plugin.getMessages().send(sender, "invalid_amount",
                                MessageManager.of("value", args[3]));
                        return true;
                    }
                }

                ItemStack potion = plugin.getPotionFactory().createPotion(delta);
                potion.setAmount(amount);
                target.getInventory().addItem(potion);

                String sign = delta > 0 ? "+" : "";
                String deltaStr = sign + delta;
                plugin.getMessages().send(sender, "give_sender", MessageManager.of(
                        "player", target.getName(),
                        "amount", String.valueOf(amount),
                        "delta",  deltaStr));
                if (!sender.equals(target)) {
                    plugin.getMessages().send(target, "give_receiver", MessageManager.of(
                            "amount", String.valueOf(amount),
                            "delta",  deltaStr));
                }
            }

            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        plugin.getMessages().send(sender, "help_header",
                MessageManager.of("version", plugin.getDescription().getVersion()));

        boolean admin = sender.hasPermission("playerheight.admin");

        if (admin || sender.hasPermission("playerheight.check")) {
            plugin.getMessages().send(sender, "help_scale_get");
        }
        if (admin) {
            plugin.getMessages().send(sender, "help_scale_set");
        }
        if (admin || sender.hasPermission("playerheight.self")) {
            plugin.getMessages().send(sender, "help_setscale");
        }
        if (admin) {
            plugin.getMessages().send(sender, "help_give");
            plugin.getMessages().send(sender, "help_reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("playerheight.check") || sender.hasPermission("playerheight.admin")) subs.add("scale");
            if (sender.hasPermission("playerheight.self") || sender.hasPermission("playerheight.admin")) subs.add("setscale");
            if (sender.hasPermission("playerheight.admin")) subs.addAll(List.of("give", "reload"));
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("setscale")) {
            if (!sender.hasPermission("playerheight.self") && !sender.hasPermission("playerheight.admin")) {
                return Collections.emptyList();
            }
            if (args.length == 2) return Arrays.asList("-0.5", "-0.1", "-0.01", "+0.01", "+0.1", "+0.5", "1.0");
            return Collections.emptyList();
        }

        if (!sender.hasPermission("playerheight.admin")) return Collections.emptyList();

        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();

        if (args.length == 2 && !sub.equals("reload")) return playerNames;

        if (args.length == 3) {
            return switch (sub) {
                case "scale" -> Arrays.asList("-0.5", "-0.1", "-0.01", "+0.01", "+0.1", "+0.5", "1.0");
                case "give"  -> Arrays.asList("-0.50", "-0.10", "-0.01", "0.01", "0.10", "0.50");
                default -> Collections.emptyList();
            };
        }

        if (args.length == 4 && sub.equals("scale")) return List.of("silent");

        return Collections.emptyList();
    }
}
