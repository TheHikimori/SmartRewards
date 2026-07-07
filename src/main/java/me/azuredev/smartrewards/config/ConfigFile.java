package me.azuredev.smartrewards.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigFile {

    private final JavaPlugin plugin;

    private final ConfigType type;

    private File file;

    private YamlConfiguration configuration;

    public ConfigFile(JavaPlugin plugin, ConfigType type) {

        this.plugin = plugin;
        this.type = type;

        load();

    }

    public void load() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(plugin.getDataFolder(), type.getFileName());

        if (!file.exists()) {
            plugin.saveResource(type.getFileName(), false);
        }

        configuration = YamlConfiguration.loadConfiguration(file);

    }

    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {

        try {

            configuration.save(file);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public YamlConfiguration getConfig() {
        return configuration;
    }

}