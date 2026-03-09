package com.friendlysmp.core.features.withersound;

import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.placeholder.FriendlyCoreExpansion;
import com.friendlysmp.core.placeholder.PlaceholderProvider;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitherSoundFeature implements Feature, PlaceholderProvider {

    private final JavaPlugin plugin;
    private final PlayerSettingsStore store;

    private PacketListenerAbstract registered; // only one listener now

    public WitherSoundFeature(JavaPlugin plugin, PlayerSettingsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override public String id() { return "wither-sound"; }

    @Override
    public void enable() {
        PluginCommand cmd = plugin.getCommand("withersound");
        if (cmd != null) {
            WitherSoundCommand exec = new WitherSoundCommand(store);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }
        org.bukkit.Bukkit.getPluginManager().registerEvents(new WitherSoundJoinListener(store), plugin);
        boolean debug = plugin.getConfig().getBoolean("features.wither-sound.debug", false);

        registered = new WitherEffectPacketListener(store, debug);
        PacketEvents.getAPI().getEventManager().registerListener(registered);
    }

    @Override
    public void registerPlaceholders(FriendlyCoreExpansion expansion) {
        expansion.registerHandler("withersound", (player, args) -> {
            boolean muted = store.isWitherDeathMuted(player.getUniqueId());
            if (args.length == 0) return muted ? "OFF" : "ON";
            if (args[0].equalsIgnoreCase("colored")) return muted ? "§cOFF" : "§aON";
            return muted ? "OFF" : "ON";
        });
    }

    @Override
    public void disable() {
        if (registered != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(registered);
            registered = null;
        }
    }

    @Override
    public void reload() { }
}