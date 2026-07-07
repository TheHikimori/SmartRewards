package me.azuredev.smartrewards.config;

import me.azuredev.smartrewards.managers.Manager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

public class ConfigManager implements Manager {

    private final JavaPlugin plugin;

    private final Map<ConfigType, ConfigFile> configs = new EnumMap<>(ConfigType.class);

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {

        for (ConfigType type : ConfigType.values()) {

            configs.put(type, new ConfigFile(plugin, type));

        }

        plugin.getLogger().info("Loaded " + configs.size() + " configuration files.");

    }

    @Override
    public void unload() {

        configs.values().forEach(ConfigFile::save);

    }

    public void reloadAll() {

        configs.values().forEach(ConfigFile::reload);

    }

    public ConfigFile get(ConfigType type) {

        return configs.get(type);

    }

}