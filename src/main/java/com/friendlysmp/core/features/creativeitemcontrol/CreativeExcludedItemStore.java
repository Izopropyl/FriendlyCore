package com.friendlysmp.core.features.creativeitemcontrol;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreativeExcludedItemStore {
    private final File file;
    private final YamlConfiguration yaml;


    public CreativeExcludedItemStore(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "cic_items.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, ItemStack> loadAll() {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            ItemStack item = yaml.getItemStack(key);
            if (item != null) result.put(key, item);
        }
        return result;
    }

    public void save(String id, ItemStack item) {
        yaml.set(id, item);
        flush();
    }

    public void remove(String id) {
        yaml.set(id, null);
        flush();
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save cic_items.yml", e);
        }
    }






}
