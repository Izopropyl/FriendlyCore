package com.friendlysmp.core.features.tokens;

import com.friendlysmp.core.FriendlyCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

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
    
    public enum GiveResult {
        GIVEN_NOW,
        STORED_FOR_CLAIM,
        FAILED
    }

    public void handleMonthlyJoin(Player player) {
        plugin.getLogger().info("[TOKENS] Join check for " + player.getName());

        int rewardAmount = getHighestRewardAmount(player);
        plugin.getLogger().info("[TOKENS] Highest reward amount for " + player.getName() + " = " + rewardAmount);

        if (rewardAmount <= 0) {
            plugin.getLogger().info("[TOKENS] Stopping: no matching rank permission.");
            return;
        }

        LocalDate now = LocalDate.now();
        plugin.getLogger().info("[TOKENS] Today = " + now + ", day=" + now.getDayOfMonth() + ", claimUntil=" + getClaimUntilDay());

        if (now.getDayOfMonth() > getClaimUntilDay()) {
            plugin.getLogger().info("[TOKENS] Stopping: current day is past claim-until-day.");
            return;
        }

        try {
            boolean alreadyRewarded = dao.hasReceivedForPeriod(player.getUniqueId(), now.getYear(), now.getMonthValue());
            plugin.getLogger().info("[TOKENS] Already rewarded this period = " + alreadyRewarded);

            if (alreadyRewarded) {
                plugin.getLogger().info("[TOKENS] Stopping: already rewarded this month.");
                return;
            }

            boolean excluded = isExcludedWorld(player.getWorld());
            boolean canCarry = canCarry(player.getInventory(), rewardAmount, tokenDisplayName());

            plugin.getLogger().info("[TOKENS] Current world = " + player.getWorld().getName());
            plugin.getLogger().info("[TOKENS] Excluded world = " + excluded);
            plugin.getLogger().info("[TOKENS] Can carry = " + canCarry);

            boolean delivered = giveShopToken(player, rewardAmount, true);
            plugin.getLogger().info("[TOKENS] Delivered immediately = " + delivered);

            dao.setLastRewarded(player.getUniqueId(), now.getYear(), now.getMonthValue());
            plugin.getLogger().info("[TOKENS] Saved last rewarded = " + now.getYear() + "-" + now.getMonthValue());

            if (delivered) {
                player.sendMessage("§aYou have received your monthly token reward.");
            } else {
                player.sendMessage("§eYour monthly token reward was saved to claim later.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[TOKENS] Failed monthly token check for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
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

            boolean success = giveShopToken(player, pending, false);
            if (!success) {
                player.sendMessage("§cYou still can't receive your tokens right now. Make inventory space and leave excluded worlds.");
                return;
            }

            dao.setPendingTokens(player.getUniqueId(), 0);
            player.sendMessage("§aYou received " + pending + " shop token(s).");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed token claim for " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cSomething went wrong while claiming your tokens.");
        }
    }

    public GiveResult handleAdminGive(Player player, int amount) {
        try {
            boolean success = giveShopToken(player, amount, true);
            return success ? GiveResult.GIVEN_NOW : GiveResult.STORED_FOR_CLAIM;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give admin tokens to " + player.getName() + ": " + e.getMessage());
            return GiveResult.FAILED;
        }
    }

    public void setLastCollection(Player player, int year, int month) throws Exception {
        dao.setLastRewarded(player.getUniqueId(), year, month);
    }

    public int getClaimUntilDay() {
        int day = plugin.getConfig().getInt("tokens.claim-until-day", 7);
        return Math.max(1, Math.min(31, day));
    }

    public int getHighestRewardAmount(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tokens.ranks");
        if (section == null) {
            plugin.getLogger().info("[TOKENS] tokens.ranks section is NULL");
            return 0;
        }

        plugin.getLogger().info("[TOKENS] rank groups = " + section.getKeys(false));

        int highest = 0;

        for (String rankKey : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(rankKey);
            if (rankSection == null) {
                plugin.getLogger().info("[TOKENS] Rank section '" + rankKey + "' is NULL");
                continue;
            }

            String permission = rankSection.getString("permission", "");
            int amount = rankSection.getInt("amount", 0);
            boolean has = !permission.isBlank() && player.hasPermission(permission);

            plugin.getLogger().info("[TOKENS] Checking rank '" + rankKey + "' permission='" + permission + "' amount=" + amount + " has=" + has);

            if (has && amount > highest) {
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

    private boolean giveShopToken(Player player, int amount, boolean storeIfFailed) throws Exception {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        Component name = tokenDisplayName();

        if (meta != null) {
            meta.displayName(name);
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
            item.setItemMeta(meta);
        }

        if (!canCarry(player.getInventory(), amount, name) || isExcludedWorld(player.getWorld())) {
            if (storeIfFailed) {
                dao.addPendingTokens(player.getUniqueId(), amount);
            }
            return false;
        }

        player.getInventory().addItem(item);
        return true;
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
            if (item == null || item.getType() != Material.GOLD_NUGGET) {
                continue;
            }
            if (!item.hasItemMeta()) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            if (!Objects.equals(meta.displayName(), displayName)) {
                continue;
            }

            int spaceInStack = item.getMaxStackSize() - item.getAmount();
            remaining -= spaceInStack;
            if (remaining <= 0) {
                return true;
            }
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