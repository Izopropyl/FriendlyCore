package com.friendlysmp.core.features.tokens;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import com.friendlysmp.core.storage.PlayerSettingsStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TokenFeature implements Feature, Listener {
    private final FriendlyCorePlugin plugin;
    private final PlayerSettingsStore playerSettings;

    private TokenDao dao;
    private TokenService service;
    private TokenCommand command;

    public TokenFeature(FriendlyCorePlugin plugin, PlayerSettingsStore playerSettings) {
        this.plugin = plugin;
        this.playerSettings = playerSettings;
    }

    @Override
    public String id() {
        return "tokens";
    }

    @Override
    public void enable() {
        try {
            this.dao = new TokenDao(playerSettings.dataSource());
            this.dao.init();
            this.service = new TokenService(plugin, dao);
            this.command = new TokenCommand(plugin, service);

            Bukkit.getPluginManager().registerEvents(this, plugin);

            PluginCommand token = plugin.getCommand("token");
            if (token != null) {
                token.setExecutor(command);
                token.setTabCompleter(command);
            }

            plugin.getLogger().info("Token feature enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable token feature: " + e.getMessage());
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        // config is re-read live through plugin.getConfig()
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("[TOKENS] TokenFeature saw join for " + player.getName());
        service.handleMonthlyJoin(player);
    }
}