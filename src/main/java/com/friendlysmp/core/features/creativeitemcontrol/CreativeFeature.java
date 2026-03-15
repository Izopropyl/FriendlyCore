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
    // Excluded items
    private Map<String, ItemStack> excludedItems;
    private CreativeExcludedItemStore excludedItemStore;

    private final Map<String, Long> giveCooldowns = new HashMap<>();
    public long giveCooldownSeconds;


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
        giveCooldownSeconds = plugin.getConfig().getLong("creativeitemcontrol.config.give-cooldown", 0);
    }

    @Override
    public String id() {return "creative-item-control";}

    @Override
    public void enable() {
    loadConfigCache();
    listener = new CreativeItemListener(this);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);

    var cicCmd = Objects.requireNonNull(plugin.getCommand("cic"));
    CreativeCommand cicExecutor = new CreativeCommand(plugin, this);
    cicCmd.setExecutor(cicExecutor);
    cicCmd.setTabCompleter(cicExecutor);


    excludedItemStore = new CreativeExcludedItemStore(plugin);
    excludedItems = excludedItemStore.loadAll();
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
        excludedItems = excludedItemStore.loadAll();
    }

    public ItemMeta getDefaultMeta(Material type) {
        return defaultMetaCache.computeIfAbsent(type, t -> new ItemStack(t, 1).getItemMeta());
    }

    public void storeExcludedItem(String id, ItemStack item) {
    excludedItems.put(id, item.clone());
    excludedItemStore.save(id, item);
    }

    public void removeExcludedItem(String id) {
        excludedItems.remove(id);
        excludedItemStore.remove(id);
    }

    public ItemStack getExcludedItem(String id) {
        return excludedItems.get(id);
    }

    public Map<String, ItemStack> getExcludedItems() {
        return Collections.unmodifiableMap(excludedItems);
    }

    public boolean isExcluded(ItemStack item) {
        return excludedItems.values().stream().anyMatch(e -> e.isSimilar(item));
    }

    public boolean isOnGiveCooldown(UUID targetId, String itemId) {
        String key = targetId + ":" + itemId;
        Long last = giveCooldowns.get(key);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < giveCooldownSeconds * 1000L;
    }
    public long getGiveCooldownRemaining(UUID targetId, String itemId) {
        String key = targetId + ":" + itemId;
        Long last = giveCooldowns.get(key);
        return giveCooldownSeconds - (System.currentTimeMillis() - last) / 1000L;
    }
    public void recordGive(UUID targetId, String itemId) {
        giveCooldowns.put(targetId + ":" + itemId, System.currentTimeMillis());
    }



}
