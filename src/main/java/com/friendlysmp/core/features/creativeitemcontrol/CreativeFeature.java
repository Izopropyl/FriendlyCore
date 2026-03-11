package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.feature.Feature;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CreativeFeature implements Feature {
    private final FriendlyCorePlugin plugin;
    // Config options
    public boolean attributesEnabled;
    public boolean enchantmentsEnabled;
    public boolean potionsEnabled;
    public boolean enchantmentsAllowIncompatible;
    public AttributeAction attributesAction;
    public EnchantAction enchantmentsAction;
    public Set<String> worlds;
    public boolean worldsBlacklist;
    public boolean playerAlerts;

    private final Map<Material, ItemMeta> defaultMetaCache = new EnumMap<>(Material.class);
    private CreativeItemListener listener;


    public CreativeFeature(FriendlyCorePlugin plugin) {
        this.plugin = plugin;
    }


    public void loadConfigCache() {
        attributesEnabled = plugin.getConfig().getBoolean("creativeitemcontrol.attributes.enabled");
        enchantmentsEnabled = plugin.getConfig().getBoolean("creativeitemcontrol.enchantments.enabled");
        potionsEnabled = plugin.getConfig().getBoolean("creativeitemcontrol.potions.enabled");
        attributesAction = AttributeAction.valueOf(plugin.getConfig().getString("creativeitemcontrol.attributes.action", "REMOVE"));
        enchantmentsAction = EnchantAction.valueOf(plugin.getConfig().getString("creativeitemcontrol.enchantments.action", "REMOVE"));
        enchantmentsAllowIncompatible = plugin.getConfig().getBoolean("creativeitemcontrol.enchantments.allow-incompatible");
        worlds = new HashSet<>(plugin.getConfig().getStringList("creativeitemcontrol.config.worlds"));
        worldsBlacklist = plugin.getConfig().getBoolean("creativeitemcontrol.config.blacklist");
        playerAlerts = plugin.getConfig().getBoolean("creativeitemcontrol.config.playeralerts");
    }

    @Override
    public String id() {return "creative-item-control";}

    @Override
    public void enable() {
    loadConfigCache();
    listener = new CreativeItemListener(this);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    Objects.requireNonNull(plugin.getCommand("cic")).setExecutor(new CreativeCommand(plugin, this));

    }

    @Override
    public void disable() {
        if (listener != null) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
            listener = null;
        }
        Objects.requireNonNull(plugin.getCommand("cic")).setExecutor(null);
        defaultMetaCache.clear();
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        loadConfigCache();
    }

    public ItemMeta getDefaultMeta(Material type) {
        return defaultMetaCache.computeIfAbsent(type, t -> new ItemStack(t, 1).getItemMeta());
    }

}
