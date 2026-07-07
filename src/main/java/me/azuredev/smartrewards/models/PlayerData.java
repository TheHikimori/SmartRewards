package me.azuredev.smartrewards.models;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private LocalDate lastClaimDate;
    private int streak;
    private int totalJoins;
    private int missedDays;
    private long playtime;
    private long sessionStart;
    private boolean firstJoinRewardClaimed;
    private boolean weeklyRewardClaimed;
    private boolean monthlyRewardClaimed;
    private int weeklyProgress;
    private int monthlyProgress;
    private final Set<Integer> claimedDailyRewards;
    private final Set<Integer> claimedWeeklyRewards;
    private final Set<Integer> claimedMonthlyRewards;
    private final Set<String> claimedSpecialRewards;
    private final Set<Integer> claimedPlaytimeRewards;
    private final Set<Integer> claimedJoinMilestones;
    private final Set<Integer> claimedStreakRewards;
    private boolean claimedToday;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.claimedDailyRewards = new HashSet<>();
        this.claimedWeeklyRewards = new HashSet<>();
        this.claimedMonthlyRewards = new HashSet<>();
        this.claimedSpecialRewards = new HashSet<>();
        this.claimedPlaytimeRewards = new HashSet<>();
        this.claimedJoinMilestones = new HashSet<>();
        this.claimedStreakRewards = new HashSet<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getLastClaimDate() {
        return lastClaimDate;
    }

    public void setLastClaimDate(LocalDate lastClaimDate) {
        this.lastClaimDate = lastClaimDate;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public int getTotalJoins() {
        return totalJoins;
    }

    public void setTotalJoins(int totalJoins) {
        this.totalJoins = totalJoins;
    }

    public int getMissedDays() {
        return missedDays;
    }

    public void setMissedDays(int missedDays) {
        this.missedDays = missedDays;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        this.sessionStart = sessionStart;
    }

    public boolean isFirstJoinRewardClaimed() {
        return firstJoinRewardClaimed;
    }

    public void setFirstJoinRewardClaimed(boolean firstJoinRewardClaimed) {
        this.firstJoinRewardClaimed = firstJoinRewardClaimed;
    }

    public boolean isWeeklyRewardClaimed() {
        return weeklyRewardClaimed;
    }

    public void setWeeklyRewardClaimed(boolean weeklyRewardClaimed) {
        this.weeklyRewardClaimed = weeklyRewardClaimed;
    }

    public boolean isMonthlyRewardClaimed() {
        return monthlyRewardClaimed;
    }

    public void setMonthlyRewardClaimed(boolean monthlyRewardClaimed) {
        this.monthlyRewardClaimed = monthlyRewardClaimed;
    }

    public int getWeeklyProgress() {
        return weeklyProgress;
    }

    public void setWeeklyProgress(int weeklyProgress) {
        this.weeklyProgress = weeklyProgress;
    }

    public int getMonthlyProgress() {
        return monthlyProgress;
    }

    public void setMonthlyProgress(int monthlyProgress) {
        this.monthlyProgress = monthlyProgress;
    }

    public Set<Integer> getClaimedDailyRewards() {
        return Collections.unmodifiableSet(claimedDailyRewards);
    }

    public void claimDailyReward(int day) {
        claimedDailyRewards.add(day);
    }

    public Set<Integer> getClaimedWeeklyRewards() {
        return Collections.unmodifiableSet(claimedWeeklyRewards);
    }

    public void claimWeeklyReward(int id) {
        claimedWeeklyRewards.add(id);
    }

    public Set<Integer> getClaimedMonthlyRewards() {
        return Collections.unmodifiableSet(claimedMonthlyRewards);
    }

    public void claimMonthlyReward(int id) {
        claimedMonthlyRewards.add(id);
    }

    public Set<String> getClaimedSpecialRewards() {
        return Collections.unmodifiableSet(claimedSpecialRewards);
    }

    public void claimSpecialReward(String id) {
        claimedSpecialRewards.add(id);
    }

    public Set<Integer> getClaimedPlaytimeRewards() {
        return Collections.unmodifiableSet(claimedPlaytimeRewards);
    }

    public void claimPlaytimeReward(int minutes) {
        claimedPlaytimeRewards.add(minutes);
    }

    public Set<Integer> getClaimedJoinMilestones() {
        return Collections.unmodifiableSet(claimedJoinMilestones);
    }

    public void claimJoinMilestone(int joins) {
        claimedJoinMilestones.add(joins);
    }

    public Set<Integer> getClaimedStreakRewards() {
        return Collections.unmodifiableSet(claimedStreakRewards);
    }

    public void claimStreakReward(int days) {
        claimedStreakRewards.add(days);
    }

    public boolean isClaimedToday() {
        return claimedToday;
    }

    public void setClaimedToday(boolean claimedToday) {
        this.claimedToday = claimedToday;
    }

    public Set<Integer> getMutableClaimedDailyRewards() {
        return claimedDailyRewards;
    }

    public Set<Integer> getMutableClaimedWeeklyRewards() {
        return claimedWeeklyRewards;
    }

    public Set<Integer> getMutableClaimedMonthlyRewards() {
        return claimedMonthlyRewards;
    }

    public Set<String> getMutableClaimedSpecialRewards() {
        return claimedSpecialRewards;
    }

    public Set<Integer> getMutableClaimedPlaytimeRewards() {
        return claimedPlaytimeRewards;
    }

    public Set<Integer> getMutableClaimedJoinMilestones() {
        return claimedJoinMilestones;
    }

    public Set<Integer> getMutableClaimedStreakRewards() {
        return claimedStreakRewards;
    }
}
