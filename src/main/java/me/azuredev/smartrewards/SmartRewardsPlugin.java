package me.azuredev.smartrewards;

import me.azuredev.smartrewards.managers.ManagerLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartRewardsPlugin extends JavaPlugin {

    private static SmartRewardsPlugin instance;

    private ManagerLoader managerLoader;

    @Override
    public void onEnable() {

        instance = this;

        this.managerLoader = new ManagerLoader(this);

        managerLoader.load();

        getLogger().info("SmartRewards enabled.");

    }

    @Override
    public void onDisable() {

        if (managerLoader != null)
            managerLoader.shutdown();

    }

    public static SmartRewardsPlugin getInstance() {
        return instance;
    }

    public ManagerLoader getManagerLoader() {
        return managerLoader;
    }

}