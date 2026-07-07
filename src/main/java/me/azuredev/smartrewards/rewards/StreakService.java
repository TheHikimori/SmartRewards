package me.azuredev.smartrewards.rewards;

import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.logging.RewardLogger;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.utils.TimeUtil;

import java.time.LocalDate;

public class StreakService {

    private final RewardLogger logger;

    public StreakService(RewardLogger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return Configs.config().getConfig().getBoolean("features.streak", true)
                && Configs.config().getConfig().getBoolean("streak.enabled", true);
    }

    public StreakResult processLogin(PlayerData data) {
        if (!isEnabled()) {
            return new StreakResult(false, data.getStreak(), 0, false);
        }

        LocalDate today = TimeUtil.today();
        LocalDate lastClaim = data.getLastClaimDate();

        if (lastClaim == null) {
            if (data.getStreak() == 0) {
                data.setStreak(1);
            }
            resetDailyClaimFlag(data, today);
            return new StreakResult(false, 0, 0, true);
        }

        long daysBetween = TimeUtil.daysBetween(lastClaim, today);

        if (daysBetween == 0) {
            return new StreakResult(false, data.getStreak(), 0, false);
        }

        if (daysBetween == 1) {
            data.setStreak(data.getStreak() + 1);
            data.setMissedDays(0);
            resetDailyClaimFlag(data, today);
            return new StreakResult(false, data.getStreak() - 1, 0, true);
        }

        long missed = daysBetween - 1;
        boolean allowMiss = Configs.config().getConfig().getBoolean("streak.allow-miss-days", true);
        int maxMissed = Configs.config().getConfig().getInt("streak.max-missed-days", 1);
        boolean preserve = Configs.config().getConfig().getBoolean("streak.preserve-streak", false);
        boolean resetOnMiss = Configs.config().getConfig().getBoolean("streak.reset-on-miss", true);

        data.setMissedDays(data.getMissedDays() + (int) missed);
        logger.logMiss(data.getUuid(), data.getName(), (int) missed);

        if (allowMiss && missed <= maxMissed && !resetOnMiss) {
            data.setStreak(data.getStreak() + 1);
            resetDailyClaimFlag(data, today);
            return new StreakResult(false, data.getStreak() - 1, (int) missed, true);
        }

        if (preserve && missed <= maxMissed) {
            resetDailyClaimFlag(data, today);
            return new StreakResult(false, data.getStreak(), (int) missed, false);
        }

        int oldStreak = data.getStreak();
        data.setStreak(1);
        data.setMissedDays(0);
        resetDailyClaimFlag(data, today);
        logger.logStreakReset(data.getUuid(), data.getName(), oldStreak);
        return new StreakResult(true, oldStreak, (int) missed, false);
    }

    private void resetDailyClaimFlag(PlayerData data, LocalDate today) {
        if (data.getLastClaimDate() == null || !today.equals(data.getLastClaimDate())) {
            data.setClaimedToday(false);
        }
    }
}
