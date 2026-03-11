package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CreativeCommand implements CommandExecutor {
    private final CreativeFeature cic;
    private final FriendlyCorePlugin plugin;

    public CreativeCommand(FriendlyCorePlugin plugin, CreativeFeature cic) {
        this.plugin = plugin;
        this.cic = cic;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (commandSender.hasPermission("friendlycore.cic.admin")) {
                commandSender.sendMessage(Component.text("CreativeItemControl config reloaded!", NamedTextColor.GREEN));
                plugin.reloadConfig();
                cic.loadConfigCache();
                return true;
            } else {
                commandSender.sendMessage(Component.text("You do not have permission!", NamedTextColor.RED));
            }
        }

        return true;
    }
}
