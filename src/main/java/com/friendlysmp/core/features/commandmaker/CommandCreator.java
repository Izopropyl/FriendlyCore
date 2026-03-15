package com.friendlysmp.core.features.commandmaker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandCreator extends Command {
    private final List<String> messages;

    public CommandCreator(String name, List<String> aliases, List<String> messages) {
        super(name);
        setAliases(aliases);
        this.messages = messages;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        for (String message: messages) {
            Component component = MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(message));
            sender.sendMessage(component);
        }
        return true;
    }

    private static String convertLegacyToMiniMessage(String input) {
        return input
                .replace("&0", "<black>").replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>")
                .replace("&8", "<dark_gray>").replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>")
                .replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>").replace("&r", "<reset>");
    }



}
