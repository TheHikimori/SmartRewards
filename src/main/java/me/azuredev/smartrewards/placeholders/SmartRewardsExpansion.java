package me.azuredev.smartrewards.placeholders;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.StorageManager;
import me.azuredev.smartrewards.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmartRewardsExpansion extends PlaceholderExpansion {

    private final SmartRewardsPlugin plugin;
    private final StorageManager storageManager;

    public SmartRewardsExpansion(SmartRewardsPlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "smartrewards";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AzureDev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return getDefault("no-data");
        }

        PlayerData data = storageManager.getCached(player.getUniqueId());
        if (data == null) {
            return getDefault("no-data");
        }

        String value = switch (params.toLowerCase()) {
            case "streak" -> String.valueOf(data.getStreak());
            case "next" -> {
                int day = Math.max(1, data.getStreak());
                var reward = plugin.getManagerLoader().getRewardManager().getRewardLoader().getDailyReward(day);
                yield reward != null ? reward.getName() : getDefault("not-available");
            }
            case "remaining" -> TimeUtil.formatDuration(TimeUtil.secondsUntilMidnight());
            case "claimed" -> data.isClaimedToday() ? "yes" : "no";
            case "time", "playtime" -> TimeUtil.formatPlaytime(data.getPlaytime());
            case "month" -> String.valueOf(data.getMonthlyProgress());
            case "week" -> String.valueOf(data.getWeeklyProgress());
            case "available" -> data.isClaimedToday() ? "no" : "yes";
            case "joins" -> String.valueOf(data.getTotalJoins());
            case "missed" -> String.valueOf(data.getMissedDays());
            default -> null;
        };

        if (value == null) {
            return null;
        }

        String format = Configs.placeholders().getConfig().getString("formats." + params.toLowerCase(), "{value}");
        return format.replace("{value}", value);
    }

    private String getDefault(String key) {
        return Configs.placeholders().getConfig().getString("defaults." + key, "0");
    }
}
