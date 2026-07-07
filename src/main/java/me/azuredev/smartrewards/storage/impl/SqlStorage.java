package me.azuredev.smartrewards.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.storage.PlayerDataSerializer;
import me.azuredev.smartrewards.storage.Storage;
import me.azuredev.smartrewards.storage.StorageType;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SqlStorage implements Storage {

    private final SmartRewardsPlugin plugin;
    private final StorageType type;
    private HikariDataSource dataSource;
    private String tableName;

    public SqlStorage(SmartRewardsPlugin plugin, StorageType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void initialize() {
        ConfigurationSection dbConfig = Configs.database().getConfig();
        String prefix = dbConfig.getString(type.name().toLowerCase() + ".table-prefix",
                dbConfig.getString("mysql.table-prefix", "sr_"));
        tableName = prefix + "players";

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(dbConfig.getInt("pool.maximum-pool-size", 10));
        config.setMinimumIdle(dbConfig.getInt("pool.minimum-idle", 2));
        config.setConnectionTimeout(dbConfig.getLong("pool.connection-timeout", 30000));
        config.setIdleTimeout(dbConfig.getLong("pool.idle-timeout", 600000));
        config.setMaxLifetime(dbConfig.getLong("pool.max-lifetime", 1800000));

        if (type == StorageType.SQLITE) {
            File dbFile = new File(plugin.getDataFolder(), dbConfig.getString("sqlite.file", "database.db"));
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        } else {
            ConfigurationSection section = dbConfig.getConfigurationSection(
                    type == StorageType.MARIADB ? "mariadb" : "mysql");
            if (section == null) {
                section = dbConfig.getConfigurationSection("mysql");
            }
            String host = section.getString("host", "localhost");
            int port = section.getInt("port", 3306);
            String database = section.getString("database", "smartrewards");
            String username = section.getString("username", "root");
            String password = section.getString("password", "password");
            boolean useSsl = section.getBoolean("use-ssl", false);

            if (type == StorageType.MARIADB) {
                config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl);
                config.setDriverClassName("org.mariadb.jdbc.Driver");
            } else {
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            config.setUsername(username);
            config.setPassword(password);
        }

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "data TEXT NOT NULL, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT data FROM " + tableName + " WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return PlayerDataSerializer.fromYamlString(uuid, rs.getString("data"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player " + uuid + ": " + e.getMessage());
            }
            return new PlayerData(uuid);
        });
    }

    @Override
    public CompletableFuture<Void> savePlayer(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String yaml = PlayerDataSerializer.toYamlString(playerData);
            String sql = type == StorageType.SQLITE
                    ? "INSERT OR REPLACE INTO " + tableName + " (uuid, name, data) VALUES (?, ?, ?)"
                    : "INSERT INTO " + tableName + " (uuid, name, data) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name), data = VALUES(data), updated_at = CURRENT_TIMESTAMP";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerData.getUuid().toString());
                ps.setString(2, playerData.getName());
                ps.setString(3, yaml);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player " + playerData.getUuid() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM " + tableName + " WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete player " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM " + tableName + " WHERE uuid = ? LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check player " + uuid + ": " + e.getMessage());
                return false;
            }
        });
    }
}
