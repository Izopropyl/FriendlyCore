package com.friendlysmp.core.features.creativeitemcontrol.handlers;

import com.friendlysmp.core.features.creativeitemcontrol.AttributeAction;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeItemCheck;
import com.friendlysmp.core.features.creativeitemcontrol.CreativeFeature;
import com.friendlysmp.core.features.creativeitemcontrol.ItemCheckContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CreativeAttributeHandler implements CreativeItemCheck {
    private final CreativeFeature plugin;

    public CreativeAttributeHandler(CreativeFeature plugin) {
        this.plugin = plugin;
    }
    @Override
    public void check(ItemCheckContext ctx) {
        if (ctx.isCancelled()) return;
        if (!plugin.attributesEnabled) return;
        if (ctx.player.hasPermission("cic.bypass.attributes")) return;

        boolean attributeIssue = ctx.meta.getAttributeModifiers() != null;
        if (attributeIssue) {
            if (plugin.attributesAction.equals(AttributeAction.REMOVE)) {
                ctx.meta.setAttributeModifiers(null);
            } else {
                ctx.cancel();
            }

            if (plugin.playerAlerts) {
                ctx.player.sendMessage(Component.text("Items with attribute modifiers are not allowed here!", NamedTextColor.RED, TextDecoration.BOLD));
            }

        }

    }
}
