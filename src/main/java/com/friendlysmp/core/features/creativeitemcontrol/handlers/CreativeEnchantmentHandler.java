package com.friendlysmp.core.features.creativeitemcontrol.handlers;

import com.friendlysmp.core.features.creativeitemcontrol.CreativeItemCheck;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.ItemCheckContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class CreativeEnchantmentHandler implements CreativeItemCheck {
    private final CreativeFeature plugin;

    public CreativeEnchantmentHandler(CreativeFeature plugin) {
        this.plugin = plugin;
    }

    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!plugin.enchantmentsEnabled) return;
        if (ctx.player.hasPermission("cic.bypass.enchantments")) return;

        Map<Enchantment, Integer> enchants = ctx.meta.getEnchants();
        if (enchants.isEmpty()) return;
        Set<Enchantment> seen = new HashSet<>();

        boolean found = false;

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();


            if (impossibleLevel(ctx, enchantment, level)) found = true;
            if (incompatibleItem(ctx, enchantment)) found = true;
            if (incompatibleEnchantment(ctx, enchantment, seen)) found = true;




            seen.add(enchantment);
        }
        if (found && plugin.playerAlerts) {
            ctx.player.sendMessage(Component.text("Items with impossible enchantments are not allowed here!", NamedTextColor.RED, TextDecoration.BOLD));
        }
    }

    public boolean incompatibleItem(ItemCheckContext ctx, Enchantment enchantment) {
        if (!enchantment.canEnchantItem(ctx.item)) {
            switch (plugin.enchantmentsAction) {
                case LOWER, REMOVE -> ctx.meta.removeEnchant(enchantment);
                case DELETE -> ctx.cancel();
                case null, default -> {}
            }
            return true;
        } else return false;
    }

    public boolean impossibleLevel(ItemCheckContext ctx, Enchantment enchantment, int level) {
        if (level > enchantment.getMaxLevel()) {
            switch (plugin.enchantmentsAction) {
                case LOWER -> ctx.meta.addEnchant(enchantment, enchantment.getMaxLevel(), true);
                case REMOVE -> ctx.meta.removeEnchant(enchantment);
                case DELETE -> ctx.cancel();
                case null, default -> {}
            }
            return true;
        } else return false;
    }

    public boolean incompatibleEnchantment(ItemCheckContext ctx, Enchantment enchantment, Set<Enchantment> seen) {
        if (plugin.enchantmentsAllowIncompatible) return false;
        for (Enchantment e : seen) {
            if (enchantment.conflictsWith(e)) {
                switch (plugin.enchantmentsAction) {
                    case LOWER, REMOVE -> ctx.meta.removeEnchant(enchantment);
                    case DELETE -> ctx.cancel();
                    case null, default -> {
                    }
                }
                return true;
            }
        }
        return false;
    }


}
