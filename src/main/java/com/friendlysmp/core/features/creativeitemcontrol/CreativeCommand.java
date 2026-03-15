package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.FriendlyCorePlugin;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CreativeCommand implements CommandExecutor, TabCompleter {
    private final CreativeFeature cic;
    private final FriendlyCorePlugin plugin;

    public CreativeCommand(FriendlyCorePlugin plugin, CreativeFeature cic) {
        this.plugin = plugin;
        this.cic = cic;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        // No perms at all
        boolean isAdmin = sender.hasPermission("friendlycore.cic.admin");
        if (!isAdmin && !sender.hasPermission("friendlycore.cic.give")) {
            sender.sendMessage(Component.text("You do not have permission", NamedTextColor.RED));
            return true;
        }

        // Give
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("give")) {
            ItemStack item = cic.getExcludedItem(args[1]);

            if (item == null) {
                sender.sendMessage(Component.text("No excluded item found with id \"" + args[1] + "\".", NamedTextColor.RED));
                return true;
            }
            Player target;
            if (args.length == 3) {
                if (!isAdmin) {
                    sender.sendMessage(Component.text("You do not have permission to give to other players!", NamedTextColor.RED));
                    return true;
                }
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player \"" + args[2] + "\" not found.", NamedTextColor.RED));
                    return true;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Specify a player: /cic give <id> <player>", NamedTextColor.RED));
                return true;
            }
            boolean inList = cic.worlds.contains(target.getWorld().getName());

            if (!isAdmin && cic.worldsBlacklist == inList) {
                sender.sendMessage(Component.text("You cannot use this in this world!", NamedTextColor.RED));
                return true;
            }
            if (!isAdmin && cic.giveCooldownSeconds > 0 && cic.isOnGiveCooldown(target.getUniqueId(), args[1])) {
                long remaining = cic.getGiveCooldownRemaining(target.getUniqueId(), args[1]);
                sender.sendMessage(Component.text("You must wait " + remaining + "s before receiving this item again.", NamedTextColor.RED));
                return true;
            }


            target.getInventory().addItem(item.clone());
            cic.recordGive(target.getUniqueId(), args[1]);
            sender.sendMessage(Component.text("Gave \"" + args[1] + "\" to " + target.getName() + ".", NamedTextColor.GREEN));
            return true;
        }


        if (!isAdmin) {
            if (args.length == 1 && args[0].equalsIgnoreCase("give")) {
                sender.sendMessage(Component.text("You must select an item! Available Items: " + cic.getExcludedItems().keySet()));
                return true;
            }

            sender.sendMessage(Component.text("You do not have permission!", NamedTextColor.RED));
            return true;
        }


        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            cic.loadConfigCache();
            sender.sendMessage(Component.text("CreativeItemControl config reloaded!", NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            Map<String, ItemStack> items = cic.getExcludedItems();
            if (items.isEmpty()) {
                sender.sendMessage(Component.text("No excluded items stored.", NamedTextColor.YELLOW));

            } else {
                sender.sendMessage(Component.text("Excluded items:", NamedTextColor.GOLD));
                items.forEach((id, item) ->
                        sender.sendMessage(Component.text("  " + id + " - " + item.getType(), NamedTextColor.WHITE))
                );
                return true;
            }
            return true;
        }

        // Exclude
        if (args.length == 2 && args[0].equalsIgnoreCase("exclude")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return true;
            }
            ItemStack held = p.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                sender.sendMessage(Component.text("You must be holding an item!", NamedTextColor.RED));
                return true;
            }
            cic.storeExcludedItem(args[1], held);
            sender.sendMessage(Component.text("Stored " + held.getType() + " as \"" + args[1] + "\".", NamedTextColor.GREEN));
            return true;
        }

        // Remove
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            if (cic.getExcludedItem(args[1]) == null) {
                sender.sendMessage(Component.text("No excluded item found with id \"" + args[1] + "\".", NamedTextColor.RED));
                return true;
            }
            cic.removeExcludedItem(args[1]);
            sender.sendMessage(Component.text("Removed excluded item \"" + args[1] + "\".", NamedTextColor.GREEN));
            return true;
        }



        return true;
    }




    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("friendlycore.cic.admin") && !sender.hasPermission("friendlycore.cic.give")) return List.of();

        if (!sender.hasPermission("friendlycore.cic.admin")) {
            if (args.length == 1) return List.of("give");
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) return List.copyOf(cic.getExcludedItems().keySet());
            return List.of();
        }


        if (args.length == 1) {
            return List.of("reload", "list", "exclude", "remove", "give");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("give"))) {
            return List.copyOf(cic.getExcludedItems().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
