package dev.ironcraft.minecart.command;

import dev.ironcraft.minecart.MinecartPlus;
import dev.ironcraft.minecart.config.RailSettings;
import dev.ironcraft.minecart.item.CustomRailItemFactory;
import dev.ironcraft.minecart.rail.CustomRailType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class IronCraftRailCommand implements CommandExecutor, TabCompleter {
    private static final List<String> IDS = java.util.Arrays.stream(CustomRailType.values())
            .map(CustomRailType::logicalId)
            .toList();

    private final MinecartPlus plugin;
    private final CustomRailItemFactory items;

    public IronCraftRailCommand(MinecartPlus plugin, CustomRailItemFactory items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("ironcraft.command")) {
            message(sender, "You do not have permission to use this command.", NamedTextColor.RED);
            return true;
        }

        if (args.length == 0) {
            showInfo(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "reload" -> reload(sender);
            default -> {
                message(sender, "Usage: /ironcraft [give <target> <rail-id> [amount]|reload]", NamedTextColor.RED);
                yield true;
            }
        };
    }

    private void showInfo(CommandSender sender) {
        RailSettings settings = plugin.settings();
        message(sender, "MinecartPlus v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD);
        message(sender, "Normal powered rail: " + blocksPerSecond(settings.poweredRailSpeed()) + " blocks/s", NamedTextColor.YELLOW);
        for (CustomRailType type : CustomRailType.values()) {
            message(sender,
                    type.displayName() + ": " + blocksPerSecond(settings.speed(type)) + " blocks/s (" + type.logicalId() + ')',
                    NamedTextColor.GRAY);
        }
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ironcraft.admin")) {
            message(sender, "You do not have permission to give custom rails.", NamedTextColor.RED);
            return true;
        }
        if (args.length < 3 || args.length > 4) {
            message(sender, "Usage: /ironcraft give <target> <rail-id> [amount]", NamedTextColor.RED);
            return true;
        }

        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }
        CustomRailType type = CustomRailType.fromLogicalId(args[2].toLowerCase(Locale.ROOT));
        if (type == null) {
            message(sender, "Unknown rail ID. Use one of: " + String.join(", ", IDS), NamedTextColor.RED);
            return true;
        }

        int amount = args.length == 4 ? parseAmount(sender, args[3]) : 1;
        if (amount < 0) {
            return true;
        }

        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(items.create(type, stackAmount));
            for (ItemStack stack : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
            }
            remaining -= stackAmount;
        }
        message(sender, "Gave " + amount + ' ' + type.displayName() + "(s) to " + target.getName() + '.', NamedTextColor.GREEN);
        return true;
    }

    private static Player resolveTarget(CommandSender sender, String value) {
        if (value.equalsIgnoreCase("@s")) {
            if (sender instanceof Player player) {
                return player;
            }
            message(sender, "@s can only be used by a player; use an exact player name from console.", NamedTextColor.RED);
            return null;
        }
        Player target = Bukkit.getPlayerExact(value);
        if (target == null) {
            message(sender, "Player not found: " + value, NamedTextColor.RED);
        }
        return target;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("ironcraft.admin")) {
            message(sender, "You do not have permission to reload this plugin.", NamedTextColor.RED);
            return true;
        }
        try {
            plugin.reloadAddon();
            message(sender, "MinecartPlus reloaded.", NamedTextColor.GREEN);
        } catch (IllegalArgumentException exception) {
            message(sender, "Reload failed: " + exception.getMessage(), NamedTextColor.RED);
        }
        return true;
    }

    private static int parseAmount(CommandSender sender, String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount < 1 || amount > 2304) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException ignored) {
            message(sender, "Amount must be a whole number from 1 to 2304.", NamedTextColor.RED);
            return -1;
        }
    }

    private static String blocksPerSecond(double blocksPerTick) {
        return String.format(Locale.ROOT, "%.1f", blocksPerTick * 20.0);
    }

    private static void message(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return prefix(List.of("give", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> targets = new ArrayList<>();
            if (sender instanceof Player) {
                targets.add("@s");
            }
            targets.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return prefix(targets, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return prefix(IDS, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return prefix(List.of("1", "6", "64"), args[3]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
