package me.azuredev.smartrewards.logging;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RewardLogger {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SmartRewardsPlugin plugin;
    private PrintWriter writer;
    private boolean enabled;
    private boolean console;

    public RewardLogger(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        enabled = Configs.config().getConfig().getBoolean("logging.enabled", true);
        console = Configs.config().getConfig().getBoolean("logging.console", true);
        if (!enabled) {
            return;
        }
        String filePath = Configs.config().getConfig().getString("logging.file", "logs/rewards.log");
        File file = new File(plugin.getDataFolder(), filePath);
        file.getParentFile().mkdirs();
        try {
            writer = new PrintWriter(new FileWriter(file, true));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to open log file: " + e.getMessage());
            enabled = false;
        }
    }

    public void shutdown() {
        if (writer != null) {
            writer.close();
        }
    }

    public void logClaim(UUID uuid, String playerName, String rewardType, String rewardId) {
        log("CLAIM", uuid, playerName, rewardType + "/" + rewardId);
    }

    public void logMiss(UUID uuid, String playerName, int missedDays) {
        log("MISS", uuid, playerName, "missed-days=" + missedDays);
    }

    public void logStreakReset(UUID uuid, String playerName, int oldStreak) {
        log("STREAK_RESET", uuid, playerName, "old-streak=" + oldStreak);
    }

    public void logStreakContinue(UUID uuid, String playerName, int streak) {
        log("STREAK", uuid, playerName, "streak=" + streak);
    }

    public void logError(String message) {
        log("ERROR", null, "SYSTEM", message);
    }

    public void logAdmin(String admin, String action, String details) {
        log("ADMIN", null, admin, action + ": " + details);
    }

    private void log(String type, UUID uuid, String player, String details) {
        if (!enabled && !console) {
            return;
        }
        String line = String.format("[%s] [%s] %s (%s) - %s",
                LocalDateTime.now().format(FORMAT), type, player,
                uuid != null ? uuid : "N/A", details);
        if (enabled && writer != null) {
            writer.println(line);
            writer.flush();
        }
        if (console) {
            plugin.getLogger().info(line);
        }
    }
}
