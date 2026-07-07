package me.azuredev.smartrewards.rewards;

public final class StreakResult {

    private final boolean reset;
    private final int oldStreak;
    private final int missedDays;
    private final boolean continued;

    public StreakResult(boolean reset, int oldStreak, int missedDays, boolean continued) {
        this.reset = reset;
        this.oldStreak = oldStreak;
        this.missedDays = missedDays;
        this.continued = continued;
    }

    public boolean isReset() {
        return reset;
    }

    public int getOldStreak() {
        return oldStreak;
    }

    public int getMissedDays() {
        return missedDays;
    }

    public boolean isContinued() {
        return continued;
    }
}
