package com.friendlysmp.core.features.commandmaker;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.bukkit.Bukkit.getName;

public class CommandFeature implements Feature {
    private final FriendlyCorePlugin plugin;

    public CommandFeature(FriendlyCorePlugin plugin) {this.plugin = plugin;}

    private final List<CommandCreator> registeredCommands = new ArrayList<>();

    @Override
    public String id() {
        return "command-maker";
    }

    @Override
    public void enable() {
        plugin.saveDefaultConfig();
        registerCommands();
        Objects.requireNonNull(plugin.getCommand("commandmaker")).setExecutor(new AdminCommand(this));
    }

    @Override
    public void disable() {
        unregisterCommands();
    }

    @Override
    public void reload() {
        unregisterCommands();
        plugin.reloadConfig();
        registerCommands();
    }

    private void registerCommands() {
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        for (String cmdName : plugin.getConfig().getStringList("commandmaker.enabled-commands")) {
            List<String> aliases = plugin.getConfig().getStringList("commandmaker.commands." + cmdName + ".aliases");
            List<String> messages = plugin.getConfig().getStringList("commandmaker.commands." + cmdName + ".message");
            CommandCreator cmd = new CommandCreator(cmdName, aliases, messages);
            commandMap.register(getName(), cmd);
            registeredCommands.add(cmd);
        }
    }

    private void unregisterCommands() {
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        for (CommandCreator cmd : registeredCommands) {
            cmd.unregister(commandMap);
            knownCommands.remove(cmd.getName());
            knownCommands.remove(getName() + ":" + cmd.getName());
            for (String alias : cmd.getAliases()) {
                knownCommands.remove(alias);
                knownCommands.remove(getName() + ":" + alias);
            }
        }
        registeredCommands.clear();
    }




}
