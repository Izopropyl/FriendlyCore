package com.friendlysmp.core.features.creativeitemcontrol;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemCheckContext {
    public final Player player;
    public ItemStack item;
    public ItemMeta meta;
    public final int slot;
    private boolean cancelled = false;


    public ItemCheckContext(Player player, ItemStack item, ItemMeta meta, int slot) {
        this.player = player;
        this.item = item;
        this.meta = meta;
        this.slot = slot;
    }

    public void cancel() {this.cancelled = true;}
    public boolean isCancelled() {return cancelled;}
    public ItemMeta newItemMeta() {return meta;}

}
