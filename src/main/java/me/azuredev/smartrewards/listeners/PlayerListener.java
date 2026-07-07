package me.azuredev.smartrewards.listeners;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.StorageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final SmartRewardsPlugin plugin;
    private final StorageManager storageManager;

    public PlayerListener(SmartRewardsPlugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        storageManager.getOrLoad(event.getPlayer().getUniqueId()).thenAccept(data -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getManagerLoader().getRewardManager().handleJoin(event.getPlayer(), data);
            });
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = storageManager.getCached(event.getPlayer().getUniqueId());
        if (data != null && data.getSessionStart() > 0) {
            long sessionSeconds = (System.currentTimeMillis() - data.getSessionStart()) / 1000;
            data.setPlaytime(data.getPlaytime() + sessionSeconds);
            plugin.getManagerLoader().getRewardManager().checkPlaytime(event.getPlayer(), data);
            storageManager.save(data);
        }
        storageManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
