package me.azuredev.smartrewards.storage.impl;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.PlayerDataSerializer;
import me.azuredev.smartrewards.storage.Storage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YamlStorage implements Storage {

    private final SmartRewardsPlugin plugin;
    private File file;
    private YamlConfiguration data;

    public YamlStorage(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create players.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        if (!data.isConfigurationSection("players")) {
            data.createSection("players");
            saveFile();
        }
    }

    @Override
    public void shutdown() {
        saveFile();
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String path = "players." + uuid;
            if (!data.isConfigurationSection(path)) {
                return new PlayerData(uuid);
            }
            return PlayerDataSerializer.deserialize(uuid, data.getConfigurationSection(path));
        });
    }

    @Override
    public CompletableFuture<Void> savePlayer(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String path = "players." + playerData.getUuid();
            data.createSection(path);
            PlayerDataSerializer.serialize(playerData, data.getConfigurationSection(path));
            saveFile();
        });
    }

    @Override
    public CompletableFuture<Void> deletePlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            data.set("players." + uuid, null);
            saveFile();
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> data.isConfigurationSection("players." + uuid));
    }

    public YamlConfiguration getData() {
        return data;
    }

    public void saveFile() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save players.yml: " + e.getMessage());
        }
    }
}
