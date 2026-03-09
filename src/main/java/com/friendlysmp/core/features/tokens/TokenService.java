package com.friendlysmp.core.features.tokens;

import com.friendlysmp.core.FriendlyCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class TokenService {
    private final FriendlyCorePlugin plugin;
    private final TokenDao dao;

    public TokenService(FriendlyCorePlugin plugin, TokenDao dao) {
        this.plugin = plugin;
        this.dao = dao;
    }

    public void handleMonthlyJoin(Player player) {
        int rewardAmount = getHighestRewardAmount(player);
        if (rewardAmount <= 0) return;

        LocalDate now = LocalDate.now();
        if (now.getDayOfMonth() > getClaimUntilDay()) return;

        try {
            if (dao.hasReceivedForPeriod(player.getUniqueId(), now.getYear(), now.getMonthValue())) {
                return;
            }

            boolean delivered = tryDeliverMonthlyReward(player, rewardAmount);
            dao.setLastRewarded(player.getUniqueId(), now.getYear(), now.getMonthValue());

            if (delivered) {
                player.sendMessage("§aYou have received your monthly token reward.");
            } else {
                player.sendMessage("§eYour monthly token reward was saved to claim later.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed monthly token check for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void handleClaim(Player player) {
        try {
            int pending = dao.getPendingTokens(player.getUniqueId());
            if (pending <= 0) {
                player.sendMessage("§cYou don't have any tokens to claim.");
                return;
            }

            if (LocalDate.now().getDayOfMonth() > getClaimUntilDay()) {
                dao.setPendingTokens(player.getUniqueId(), 0);
                player.sendMessage("§cYou can only claim your stored tokens before day " + getClaimUntilDay() + " of the month.");
                return;
            }

            if (!canReceiveNow(player, pending)) {
                player.sendMessage("§cYou still can't receive your tokens right now. Make inventory space and leave excluded worlds.");
                return;
            }

            giveExactTokenCommand(player, pending);
            dao.setPendingTokens(player.getUniqueId(), 0);
            player.sendMessage("§aYou received " + pending + " shop token(s).");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed token claim for " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cSomething went wrong while claiming your tokens.");
        }
    }

    public boolean handleAdminGive(Player player, int amount) {
        if (!canReceiveNow(player, amount)) {
            return false;
        }
        return giveExactTokenCommand(player, amount);
    }

    public void setLastCollection(Player player, int year, int month) throws Exception {
        dao.setLastRewarded(player.getUniqueId(), year, month);
    }

    private boolean tryDeliverMonthlyReward(Player player, int amount) throws Exception {
        if (!canReceiveNow(player, amount)) {
            dao.addPendingTokens(player.getUniqueId(), amount);
            return false;
        }

        return giveExactTokenCommand(player, amount);
    }

    public int getClaimUntilDay() {
        return Math.max(1, plugin.getConfig().getInt("tokens.claim-until-day", 7));
    }

    public int getHighestRewardAmount(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tokens.ranks");
        if (section == null) return 0;

        int highest = 0;
        for (String permission : section.getKeys(false)) {
            int amount = section.getInt(permission, 0);
            if (amount > highest && player.hasPermission(permission)) {
                highest = amount;
            }
        }
        return highest;
    }

    public boolean canReceiveNow(Player player, int amount) {
        if (isExcludedWorld(player.getWorld())) {
            return false;
        }
        return canCarry(player.getInventory(), amount, tokenDisplayName());
    }

    private boolean isExcludedWorld(World world) {
        List<String> excluded = plugin.getConfig().getStringList("tokens.excluded-worlds");
        for (String name : excluded) {
            if (name.equalsIgnoreCase(world.getName())) {
                return true;
            }
        }
        return false;
    }

    private Component tokenDisplayName() {
        return Component.text("Shop Token").decoration(TextDecoration.ITALIC, false);
    }

    private boolean canCarry(Inventory inventory, int amount, Component displayName) {
        int remaining = amount;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() != Material.GOLD_NUGGET) continue;
            if (!item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (!Objects.equals(meta.displayName(), displayName)) continue;

            int free = item.getMaxStackSize() - item.getAmount();
            remaining -= free;
            if (remaining <= 0) return true;
        }

        int fullStacksNeeded = (int) Math.ceil(remaining / 64.0);
        int emptySlots = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }

        return emptySlots >= fullStacksNeeded;
    }

    private boolean giveExactTokenCommand(Player player, int amount) {
        String giveCommand =
                "minecraft:give " + player.getName() +
                " minecraft:gold_nugget[custom_name='[{\\\"text\\\":\\\"Shop Token\\\",\\\"italic\\\":false}]',enchantments={levels:{loyalty:1}}] " +
                amount;

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCommand);
    }

    public static ItemStack createTokenPreviewItem(int amount) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Shop Token").decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}