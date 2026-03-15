package com.friendlysmp.core.features.creativeitemcontrol;

import com.friendlysmp.core.features.creativeitemcontrol.handlers.CreativeAttributeHandler;
import com.friendlysmp.core.features.creativeitemcontrol.handlers.CreativeEnchantmentHandler;
import com.friendlysmp.core.features.creativeitemcontrol.handlers.CreativePotionHandler;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CreativeItemListener implements Listener {

    private final CreativeFeature plugin;
    private final CreativeAttributeHandler attributeHandler;
    private final CreativeEnchantmentHandler enchantmentHandler;
    private final CreativePotionHandler potionHandler;


    public CreativeItemListener(CreativeFeature plugin) {
        this.plugin = plugin;
        this.attributeHandler = new CreativeAttributeHandler(plugin);
        this.enchantmentHandler = new CreativeEnchantmentHandler(plugin);
        this.potionHandler = new CreativePotionHandler(plugin);
    }

    @EventHandler
    public void onCreativeInventory (InventoryCreativeEvent e) {
        boolean inList = plugin.worlds.contains(e.getWhoClicked().getWorld().getName());
        if (plugin.worldsBlacklist == inList) return;

        if (e.getWhoClicked().hasPermission("friendlycore.cic.bypass")) return;


        // Setup Item Information
        boolean isDrop = e.getSlot() < 0;

        ItemStack item = e.getCursor();
        if (item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        ItemMeta originalMeta = meta.clone();



        Player p = (Player) e.getWhoClicked();


        if (meta.equals(plugin.getDefaultMeta(item.getType()))) return;

        if (plugin.isExcluded(item)) return;

        ItemCheckContext ctx = new ItemCheckContext(p, item, meta, e.getSlot());

        attributeHandler.check(ctx);
        potionHandler.check(ctx);
        enchantmentHandler.check(ctx);

        boolean wasModified = !ctx.meta.equals(originalMeta);

        if (ctx.isCancelled()) {
            e.setCancelled(true);
        } else if (isDrop && wasModified) {
            e.setCancelled(true);
        } else {
            item.setItemMeta(ctx.newItemMeta());
            if (!isDrop && e.getSlot() < p.getInventory().getSize()) {
                p.getInventory().setItem(e.getSlot(), item);
            }
            if (wasModified) p.updateInventory();
        }
    }

    @EventHandler
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent e) {
        if (e.getSlot() < 0) return;
        if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;

        boolean inList = plugin.worlds.contains(e.getPlayer().getWorld().getName());
        if (plugin.worldsBlacklist == inList) return;

        if (e.getPlayer().hasPermission("friendlycore.cic.bypass")) return;


        ItemStack item = e.getNewItemStack();
        if (item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Player p = e.getPlayer();



        if (meta.equals(plugin.getDefaultMeta(item.getType()))) return;

        if (plugin.isExcluded(item)) return;

        ItemCheckContext ctx = new ItemCheckContext(p, item, meta, e.getSlot());

        attributeHandler.check(ctx);
        potionHandler.check(ctx);
        enchantmentHandler.check(ctx);

        if (ctx.isCancelled()) {
            p.getInventory().setItem(e.getSlot(), null);
        } else {
            item.setItemMeta(ctx.newItemMeta());
            p.getInventory().setItem(e.getSlot(), item);
        }

    }
}
