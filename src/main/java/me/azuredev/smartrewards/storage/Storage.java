package me.azuredev.smartrewards.storage;

import me.azuredev.smartrewards.models.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    void initialize();

    void shutdown();

    CompletableFuture<PlayerData> loadPlayer(UUID uuid);

    CompletableFuture<Void> savePlayer(PlayerData data);

    CompletableFuture<Void> deletePlayer(UUID uuid);

    CompletableFuture<Boolean> exists(UUID uuid);

}
