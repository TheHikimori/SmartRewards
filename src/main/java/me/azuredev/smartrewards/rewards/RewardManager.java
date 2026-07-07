package me.azuredev.smartrewards.rewards;



import me.azuredev.smartrewards.SmartRewardsPlugin;

import me.azuredev.smartrewards.config.Configs;

import me.azuredev.smartrewards.logging.RewardLogger;

import me.azuredev.smartrewards.managers.Manager;

import me.azuredev.smartrewards.models.PlayerData;

import me.azuredev.smartrewards.models.RewardDefinition;

import me.azuredev.smartrewards.storage.StorageManager;

import me.azuredev.smartrewards.utils.MessageUtil;

import me.azuredev.smartrewards.utils.TimeUtil;

import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;



import java.time.LocalDate;

import java.util.HashMap;

import java.util.Map;



public class RewardManager implements Manager {



    private final SmartRewardsPlugin plugin;

    private final StorageManager storageManager;

    private final RewardLoader rewardLoader;

    private final StreakService streakService;

    private final RewardLogger logger;



    public RewardManager(SmartRewardsPlugin plugin, StorageManager storageManager,

                         RewardLoader rewardLoader, StreakService streakService, RewardLogger logger) {

        this.plugin = plugin;

        this.storageManager = storageManager;

        this.rewardLoader = rewardLoader;

        this.streakService = streakService;

        this.logger = logger;

    }



    @Override

    public void load() {

        rewardLoader.reload();

    }



    @Override

    public void unload() {

    }



    public void reload() {

        rewardLoader.reload();

    }



    public RewardLoader getRewardLoader() {

        return rewardLoader;

    }



    public void handleJoin(Player player, PlayerData data) {

        data.setName(player.getName());

        data.setTotalJoins(data.getTotalJoins() + 1);

        data.setSessionStart(System.currentTimeMillis());



        StreakResult streakResult = streakService.processLogin(data);

        handleStreakNotifications(player, data, streakResult);



        if (rewardLoader.isFirstJoinEnabled() && !data.isFirstJoinRewardClaimed()) {

            claimFirstJoin(player, data);

        }



        checkJoinMilestones(player, data);

        checkStreakMilestones(player, data);



        if (rewardLoader.isSpecialEnabled()) {

            checkSpecialRewards(player, data);

        }



        boolean autoClaim = Configs.config().getConfig().getBoolean("settings.auto-claim", false);

        if (autoClaim && rewardLoader.isDailyEnabled()) {

            ClaimResult result = claimDaily(player, data);

            if (result.isSuccess()) {

                MessageUtil.send(player, "auto-claim");

            }

        }



        storageManager.save(data);

    }



    private void handleStreakNotifications(Player player, PlayerData data, StreakResult streakResult) {

        if (streakResult.isReset()) {

            MessageUtil.send(player, "streak-reset", Map.of("streak", String.valueOf(streakResult.getOldStreak())));

            return;

        }

        if (streakResult.isContinued() && streakResult.getMissedDays() == 0 && data.getStreak() > 1) {

            plugin.getManagerLoader().getActionExecutor().playStreakAnimations(player, data.getStreak());

        }

    }



    public ClaimResult claimDaily(Player player, PlayerData data) {

        if (!rewardLoader.isDailyEnabled()) {

            return ClaimResult.DISABLED;

        }



        LocalDate today = TimeUtil.today();

        if (data.isClaimedToday() && today.equals(data.getLastClaimDate())) {

            return ClaimResult.ALREADY_CLAIMED;

        }



        int day = Math.max(1, data.getStreak());

        if (day > rewardLoader.getMaxDailyDay()) {

            day = ((day - 1) % rewardLoader.getMaxDailyDay()) + 1;

        }



        RewardDefinition reward = rewardLoader.getDailyReward(day);

        if (reward == null) {

            return ClaimResult.notAvailable("No reward for day " + day);

        }



        double multiplier = getVipMultiplier(player);

        executeReward(player, reward, multiplier);



        data.setLastClaimDate(today);

        data.setClaimedToday(true);

        data.claimDailyReward(day);

        data.setWeeklyProgress(data.getWeeklyProgress() + 1);

        data.setMonthlyProgress(data.getMonthlyProgress() + 1);



        logger.logClaim(player.getUniqueId(), player.getName(), "DAILY", String.valueOf(day));

        logger.logStreakContinue(player.getUniqueId(), player.getName(), data.getStreak());



        MessageUtil.send(player, "reward-claimed", Map.of("reward", reward.getName()));

        MessageUtil.send(player, "streak-continued", Map.of("streak", String.valueOf(data.getStreak())));



        checkStreakMilestones(player, data);

        checkWeeklyMonthly(player, data);

        storageManager.save(data);

        return ClaimResult.SUCCESS;

    }



    private void claimFirstJoin(Player player, PlayerData data) {

        RewardDefinition reward = rewardLoader.getFirstJoinReward();

        if (reward == null) {

            return;

        }

        executeReward(player, reward, getVipMultiplier(player));

        data.setFirstJoinRewardClaimed(true);

        logger.logClaim(player.getUniqueId(), player.getName(), "FIRST_JOIN", "first-join");

        MessageUtil.send(player, "first-join");

    }



    private void checkJoinMilestones(Player player, PlayerData data) {

        for (Map.Entry<Integer, RewardDefinition> entry : rewardLoader.getJoinMilestones().entrySet()) {

            int required = entry.getKey();

            if (data.getTotalJoins() >= required && !data.getClaimedJoinMilestones().contains(required)) {

                executeReward(player, entry.getValue(), getVipMultiplier(player));

                data.claimJoinMilestone(required);

                logger.logClaim(player.getUniqueId(), player.getName(), "JOIN_MILESTONE", String.valueOf(required));

            }

        }

    }



    private void checkStreakMilestones(Player player, PlayerData data) {

        if (!rewardLoader.isStreakRewardsEnabled()) {

            return;

        }

        int streak = data.getStreak();

        for (Map.Entry<Integer, RewardDefinition> entry : rewardLoader.getStreakRewards().entrySet()) {

            int required = entry.getKey();

            if (streak >= required && !data.getClaimedStreakRewards().contains(required)) {

                executeReward(player, entry.getValue(), getVipMultiplier(player));

                data.claimStreakReward(required);

                logger.logClaim(player.getUniqueId(), player.getName(), "STREAK", String.valueOf(required));

                MessageUtil.send(player, "streak-milestone", Map.of(

                        "reward", entry.getValue().getName(),

                        "streak", String.valueOf(required)

                ));

            }

        }

    }



    private void checkWeeklyMonthly(Player player, PlayerData data) {

        int weeklyRequired = Configs.config().getConfig().getInt("weekly.required-days", 7);

        int monthlyRequired = Configs.config().getConfig().getInt("monthly.required-days", 30);



        if (rewardLoader.isWeeklyEnabled() && data.getWeeklyProgress() >= weeklyRequired) {

            RewardDefinition weekly = rewardLoader.getWeeklyReward();

            if (weekly != null) {

                executeReward(player, weekly, getVipMultiplier(player));

                data.setWeeklyProgress(data.getWeeklyProgress() - weeklyRequired);

                data.setWeeklyRewardClaimed(true);

                logger.logClaim(player.getUniqueId(), player.getName(), "WEEKLY", "weekly");

                MessageUtil.send(player, "weekly-unlocked");

            }

        }



        if (rewardLoader.isMonthlyEnabled() && data.getMonthlyProgress() >= monthlyRequired) {

            RewardDefinition monthly = rewardLoader.getMonthlyReward();

            if (monthly != null) {

                executeReward(player, monthly, getVipMultiplier(player));

                data.setMonthlyProgress(data.getMonthlyProgress() - monthlyRequired);

                data.setMonthlyRewardClaimed(true);

                logger.logClaim(player.getUniqueId(), player.getName(), "MONTHLY", "monthly");

                MessageUtil.send(player, "monthly-unlocked");

            }

        }

    }



    public void checkPlaytime(Player player, PlayerData data) {

        if (!rewardLoader.isPlaytimeEnabled()) {

            return;

        }

        long playtimeMinutes = data.getPlaytime() / 60;

        for (Map.Entry<Integer, RewardDefinition> entry : rewardLoader.getPlaytimeRewards().entrySet()) {

            int required = entry.getKey();

            if (playtimeMinutes >= required && !data.getClaimedPlaytimeRewards().contains(required)) {

                executeReward(player, entry.getValue(), getVipMultiplier(player));

                data.claimPlaytimeReward(required);

                logger.logClaim(player.getUniqueId(), player.getName(), "PLAYTIME", String.valueOf(required));

                MessageUtil.send(player, "playtime-reward", Map.of("reward", entry.getValue().getName()));

            }

        }

    }



    private void checkSpecialRewards(Player player, PlayerData data) {

        LocalDate today = TimeUtil.today();

        for (Map.Entry<String, RewardDefinition> entry : rewardLoader.getSpecialRewards().entrySet()) {

            String id = entry.getKey();

            if (data.getClaimedSpecialRewards().contains(id)) {

                continue;

            }

            RewardDefinition reward = entry.getValue();

            ConfigurationSection section = reward.getRawSection();

            if (isSpecialAvailable(section, today)) {

                executeReward(player, reward, getVipMultiplier(player));

                data.claimSpecialReward(id);

                logger.logClaim(player.getUniqueId(), player.getName(), "SPECIAL", id);

                MessageUtil.send(player, "special-reward", Map.of("reward", reward.getName()));

            }

        }

    }



    private boolean isSpecialAvailable(ConfigurationSection section, LocalDate today) {

        ConfigurationSection range = section.getConfigurationSection("date-range");

        if (range != null) {

            return TimeUtil.isInDateRange(range.getString("start"), range.getString("end"), today);

        }

        if (section.contains("months")) {

            int month = today.getMonthValue();

            return section.getIntegerList("months").contains(month);

        }

        return false;

    }



    public ClaimResult giveReward(Player target, String type, String id) {

        RewardDefinition reward = resolveReward(type, id);

        if (reward == null) {

            return ClaimResult.notAvailable("Unknown reward");

        }

        executeReward(target, reward, getVipMultiplier(target));

        logger.logAdmin("CONSOLE", "GIVE", target.getName() + " " + type + "/" + id);

        return ClaimResult.SUCCESS;

    }



    private RewardDefinition resolveReward(String type, String id) {

        return switch (type.toLowerCase()) {

            case "daily" -> rewardLoader.getDailyReward(Integer.parseInt(id));

            case "weekly" -> rewardLoader.getWeeklyReward();

            case "monthly" -> rewardLoader.getMonthlyReward();

            case "special" -> rewardLoader.getSpecialRewards().get(id);

            case "streak" -> rewardLoader.getStreakReward(Integer.parseInt(id));

            case "playtime" -> rewardLoader.getPlaytimeRewards().get(Integer.parseInt(id));

            case "first-join", "firstjoin" -> rewardLoader.getFirstJoinReward();

            default -> null;

        };

    }



    public void executeReward(Player player, RewardDefinition reward, double multiplier) {

        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("player", player.getName());

        placeholders.put("reward", reward.getName());

        placeholders.put("streak", String.valueOf(getPlayerStreak(player)));



        plugin.getManagerLoader().getActionExecutor().executeAll(player, reward.getActions(), placeholders, multiplier);



        if (isVip(player) && Configs.config().getConfig().getBoolean("features.vip", true)) {

            ConfigurationSection vip = Configs.vip().getConfig();

            if (vip.getBoolean("enabled", true)) {

                MessageUtil.send(player, "vip-bonus", Map.of("multiplier", String.valueOf(multiplier)));

                plugin.getManagerLoader().getActionExecutor().executeAll(

                        player, RewardDefinition.parseActionsFromKey(vip, "bonus-actions"), placeholders, 1.0);

            }

        }



        plugin.getManagerLoader().getActionExecutor().playClaimAnimations(player, reward.getName());

    }



    public double getVipMultiplier(Player player) {

        if (!isVip(player)) {

            return 1.0;

        }

        return Configs.vip().getConfig().getDouble("multiplier", 1.5);

    }



    public boolean isVip(Player player) {

        return player.hasPermission(MessageUtil.getNode("vip"));

    }



    public int getPlayerStreak(Player player) {

        PlayerData data = storageManager.getCached(player.getUniqueId());

        return data != null ? data.getStreak() : 0;

    }



    public String getNextRewardName(Player player) {

        PlayerData data = player != null ? storageManager.getCached(player.getUniqueId()) : null;

        int day = data != null ? Math.max(1, data.getStreak()) : 1;

        RewardDefinition reward = rewardLoader.getDailyReward(day);

        return reward != null ? reward.getName() : "N/A";

    }



    public void resetPlayer(PlayerData data) {

        data.setStreak(0);

        data.setMissedDays(0);

        data.setLastClaimDate(null);

        data.setClaimedToday(false);

        data.setFirstJoinRewardClaimed(false);

        data.setWeeklyRewardClaimed(false);

        data.setMonthlyRewardClaimed(false);

        data.setWeeklyProgress(0);

        data.setMonthlyProgress(0);

        data.getMutableClaimedDailyRewards().clear();

        data.getMutableClaimedWeeklyRewards().clear();

        data.getMutableClaimedMonthlyRewards().clear();

        data.getMutableClaimedSpecialRewards().clear();

        data.getMutableClaimedPlaytimeRewards().clear();

        data.getMutableClaimedJoinMilestones().clear();

        data.getMutableClaimedStreakRewards().clear();

    }



    public ClaimResult claimSpecial(Player player, PlayerData data, String id) {

        if (!rewardLoader.isSpecialEnabled()) {

            return ClaimResult.DISABLED;

        }

        if (data.getClaimedSpecialRewards().contains(id)) {

            return ClaimResult.ALREADY_CLAIMED;

        }

        RewardDefinition reward = rewardLoader.getSpecialRewards().get(id);

        if (reward == null || !isSpecialAvailable(reward.getRawSection(), TimeUtil.today())) {

            return ClaimResult.NOT_AVAILABLE;

        }

        executeReward(player, reward, getVipMultiplier(player));

        data.claimSpecialReward(id);

        storageManager.save(data);

        return ClaimResult.SUCCESS;

    }

}

