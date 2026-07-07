package me.azuredev.smartrewards.listeners;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PlaytimeListener {

    private final SmartRewardsPlugin plugin;
    private final StorageManager storageManager;
    private BukkitTask task;

    public PlaytimeListener(SmartRewardsPlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    public void start() {
        int interval = plugin.getManagerLoader().getConfigManager().get(me.azuredev.smartrewards.config.ConfigType.CONFIG)
                .getConfig().getInt("playtime.check-interval", 60);
        if (interval <= 0) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = storageManager.getCached(player.getUniqueId());
                if (data == null || data.getSessionStart() <= 0) {
                    continue;
                }
                long sessionSeconds = (System.currentTimeMillis() - data.getSessionStart()) / 1000;
                data.setPlaytime(data.getPlaytime() + interval);
                plugin.getManagerLoader().getRewardManager().checkPlaytime(player, data);
                data.setSessionStart(System.currentTimeMillis());
                storageManager.save(data);
            }
        }, interval * 20L, interval * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }
}
