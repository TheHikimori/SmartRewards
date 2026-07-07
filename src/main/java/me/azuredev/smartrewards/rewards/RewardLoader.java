package me.azuredev.smartrewards.rewards;

import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.models.RewardCategory;
import me.azuredev.smartrewards.models.RewardDefinition;
import me.azuredev.smartrewards.utils.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class RewardLoader {

    private Map<Integer, RewardDefinition> dailyRewards = new TreeMap<>();
    private RewardDefinition weeklyReward;
    private RewardDefinition monthlyReward;
    private Map<String, RewardDefinition> specialRewards = new LinkedHashMap<>();
    private Map<Integer, RewardDefinition> streakRewards = new TreeMap<>();
    private Map<Integer, RewardDefinition> playtimeRewards = new TreeMap<>();
    private RewardDefinition firstJoinReward;
    private Map<Integer, RewardDefinition> joinMilestones = new TreeMap<>();

    public void reload() {
        loadDaily();
        loadWeekly();
        loadMonthly();
        loadSpecial();
        loadStreak();
        loadPlaytime();
    }

    private void loadDaily() {
        dailyRewards = new TreeMap<>();
        ConfigurationSection section = Configs.daily().getConfig().getConfigurationSection("rewards");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int day = Integer.parseInt(key);
                ConfigurationSection rewardSection = section.getConfigurationSection(key);
                dailyRewards.put(day, createDefinition(key, RewardCategory.DAILY, rewardSection));
            } catch (NumberFormatException ignored) {
            }
        }

        ConfigurationSection firstJoin = Configs.daily().getConfig().getConfigurationSection("first-join");
        if (firstJoin != null && firstJoin.getBoolean("enabled", true)) {
            firstJoinReward = createDefinition("first-join", RewardCategory.FIRST_JOIN, firstJoin);
        } else {
            firstJoinReward = null;
        }

        joinMilestones = new TreeMap<>();
        ConfigurationSection milestones = Configs.daily().getConfig().getConfigurationSection("join-milestones");
        if (milestones != null) {
            for (String key : milestones.getKeys(false)) {
                try {
                    int joins = Integer.parseInt(key);
                    joinMilestones.put(joins, createDefinition(key, RewardCategory.JOIN_MILESTONE, milestones.getConfigurationSection(key)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void loadWeekly() {
        ConfigurationSection section = Configs.weekly().getConfig().getConfigurationSection("reward");
        if (section != null && Configs.weekly().getConfig().getBoolean("enabled", true)) {
            weeklyReward = createDefinition("weekly", RewardCategory.WEEKLY, section);
        } else {
            weeklyReward = null;
        }
    }

    private void loadMonthly() {
        ConfigurationSection section = Configs.monthly().getConfig().getConfigurationSection("reward");
        if (section != null && Configs.monthly().getConfig().getBoolean("enabled", true)) {
            monthlyReward = createDefinition("monthly", RewardCategory.MONTHLY, section);
        } else {
            monthlyReward = null;
        }
    }

    private void loadSpecial() {
        specialRewards = new LinkedHashMap<>();
        ConfigurationSection section = Configs.special().getConfig().getConfigurationSection("rewards");
        if (section == null || !Configs.special().getConfig().getBoolean("enabled", true)) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(key);
            if (rewardSection == null || !isSpecialTypeEnabled(rewardSection)) {
                continue;
            }
            specialRewards.put(key, createDefinition(key, RewardCategory.SPECIAL, rewardSection));
        }
    }

    private boolean isSpecialTypeEnabled(ConfigurationSection section) {
        if (section.contains("season")) {
            return Configs.config().getConfig().getBoolean("features.seasonal", true);
        }
        if (section.contains("event")) {
            return Configs.config().getConfig().getBoolean("features.event", true);
        }
        return Configs.config().getConfig().getBoolean("features.special", true);
    }

    private void loadStreak() {
        streakRewards = new TreeMap<>();
        ConfigurationSection section = Configs.streak().getConfig().getConfigurationSection("rewards");
        if (section == null || !Configs.streak().getConfig().getBoolean("enabled", true)) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int days = Integer.parseInt(key);
                streakRewards.put(days, createDefinition(key, RewardCategory.STREAK, section.getConfigurationSection(key)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void loadPlaytime() {
        playtimeRewards = new TreeMap<>();
        ConfigurationSection section = Configs.playtime().getConfig().getConfigurationSection("intervals");
        if (section == null || !Configs.playtime().getConfig().getBoolean("enabled", true)) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int minutes = Integer.parseInt(key);
                playtimeRewards.put(minutes, createDefinition(key, RewardCategory.PLAYTIME, section.getConfigurationSection(key)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private RewardDefinition createDefinition(String id, RewardCategory category, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String name = section.getString("name", id);
        return new RewardDefinition(
                id,
                category,
                name,
                ItemBuilder.fromConfig(section.getConfigurationSection("display")),
                ItemBuilder.fromConfig(section.getConfigurationSection("claimed-display")),
                ItemBuilder.fromConfig(section.getConfigurationSection("locked-display")),
                RewardDefinition.parseActions(section),
                section
        );
    }

    public Map<Integer, RewardDefinition> getDailyRewards() {
        return Collections.unmodifiableMap(dailyRewards);
    }

    public RewardDefinition getDailyReward(int day) {
        return dailyRewards.get(day);
    }

    public int getMaxDailyDay() {
        return dailyRewards.isEmpty() ? 0 : dailyRewards.lastKey();
    }

    public RewardDefinition getWeeklyReward() {
        return weeklyReward;
    }

    public RewardDefinition getMonthlyReward() {
        return monthlyReward;
    }

    public Map<String, RewardDefinition> getSpecialRewards() {
        return Collections.unmodifiableMap(specialRewards);
    }

    public Map<Integer, RewardDefinition> getStreakRewards() {
        return Collections.unmodifiableMap(streakRewards);
    }

    public RewardDefinition getStreakReward(int days) {
        return streakRewards.get(days);
    }

    public Map<Integer, RewardDefinition> getPlaytimeRewards() {
        return Collections.unmodifiableMap(playtimeRewards);
    }

    public RewardDefinition getFirstJoinReward() {
        return firstJoinReward;
    }

    public Map<Integer, RewardDefinition> getJoinMilestones() {
        return Collections.unmodifiableMap(joinMilestones);
    }

    public boolean isDailyEnabled() {
        return Configs.config().getConfig().getBoolean("features.daily", true)
                && Configs.daily().getConfig().getBoolean("enabled", true);
    }

    public boolean isWeeklyEnabled() {
        return Configs.config().getConfig().getBoolean("features.weekly", true)
                && Configs.weekly().getConfig().getBoolean("enabled", true);
    }

    public boolean isMonthlyEnabled() {
        return Configs.config().getConfig().getBoolean("features.monthly", true)
                && Configs.monthly().getConfig().getBoolean("enabled", true);
    }

    public boolean isSpecialEnabled() {
        return Configs.config().getConfig().getBoolean("features.special", true)
                && Configs.special().getConfig().getBoolean("enabled", true);
    }

    public boolean isStreakRewardsEnabled() {
        return Configs.config().getConfig().getBoolean("features.streak", true)
                && Configs.streak().getConfig().getBoolean("enabled", true);
    }

    public boolean isPlaytimeEnabled() {
        return Configs.config().getConfig().getBoolean("features.playtime", true)
                && Configs.playtime().getConfig().getBoolean("enabled", true);
    }

    public boolean isFirstJoinEnabled() {
        return Configs.config().getConfig().getBoolean("features.first-join", true);
    }

    public boolean isStreakEnabled() {
        return Configs.config().getConfig().getBoolean("features.streak", true)
                && Configs.config().getConfig().getBoolean("streak.enabled", true);
    }
}
