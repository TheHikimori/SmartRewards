package me.azuredev.smartrewards.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.managers.Manager;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.impl.SqlStorage;
import me.azuredev.smartrewards.storage.impl.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StorageManager implements Manager {

    private final SmartRewardsPlugin plugin;
    private Storage storage;
    private YamlStorage yamlStorage;
    private Cache<UUID, PlayerData> cache;
    private int autoSaveTaskId = -1;

    public StorageManager(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();

        String typeName = Configs.database().getConfig().getString("type", "YAML");
        StorageType type;
        try {
            type = StorageType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = StorageType.YAML;
            plugin.getLogger().warning("Unknown storage type: " + typeName + ", using YAML");
        }

        if (type == StorageType.YAML) {
            yamlStorage = new YamlStorage(plugin);
            storage = yamlStorage;
        } else {
            storage = new SqlStorage(plugin, type);
        }

        storage.initialize();

        int interval = Configs.config().getConfig().getInt("settings.auto-save-interval", 300);
        if (interval > 0) {
            autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllCached, interval * 20L, interval * 20L).getTaskId();
        }

        plugin.getLogger().info("Storage initialized: " + type.name());
    }

    @Override
    public void unload() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        saveAllCached();
        if (storage != null) {
            storage.shutdown();
        }
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    public CompletableFuture<PlayerData> getOrLoad(UUID uuid) {
        PlayerData cached = cache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return storage.loadPlayer(uuid).thenApply(data -> {
            cache.put(uuid, data);
            return data;
        });
    }

    public PlayerData getCached(UUID uuid) {
        return cache.getIfPresent(uuid);
    }

    public void cache(PlayerData data) {
        cache.put(data.getUuid(), data);
    }

    public CompletableFuture<Void> save(PlayerData data) {
        cache.put(data.getUuid(), data);
        return storage.savePlayer(data);
    }

    public CompletableFuture<Void> delete(UUID uuid) {
        cache.invalidate(uuid);
        return storage.deletePlayer(uuid);
    }

    public void saveAllCached() {
        cache.asMap().values().forEach(data -> storage.savePlayer(data));
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.getIfPresent(uuid);
        if (data != null) {
            storage.savePlayer(data);
            cache.invalidate(uuid);
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public YamlStorage getYamlStorage() {
        return yamlStorage;
    }
}
