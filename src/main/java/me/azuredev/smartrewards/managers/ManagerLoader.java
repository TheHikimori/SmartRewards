package me.azuredev.smartrewards.managers;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.actions.ActionExecutor;
import me.azuredev.smartrewards.commands.RewardsCommand;
import me.azuredev.smartrewards.config.ConfigManager;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.gui.GuiManager;
import me.azuredev.smartrewards.hooks.VaultHook;
import me.azuredev.smartrewards.listeners.PlaytimeListener;
import me.azuredev.smartrewards.listeners.PlayerListener;
import me.azuredev.smartrewards.logging.RewardLogger;
import me.azuredev.smartrewards.placeholders.SmartRewardsExpansion;
import me.azuredev.smartrewards.rewards.RewardLoader;
import me.azuredev.smartrewards.rewards.RewardManager;
import me.azuredev.smartrewards.rewards.StreakService;
import me.azuredev.smartrewards.services.BackupService;
import me.azuredev.smartrewards.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class ManagerLoader {

    private final SmartRewardsPlugin plugin;
    private final List<Manager> managers = new ArrayList<>();

    private ConfigManager configManager;
    private StorageManager storageManager;
    private RewardLoader rewardLoader;
    private StreakService streakService;
    private RewardManager rewardManager;
    private RewardLogger rewardLogger;
    private VaultHook vaultHook;
    private ActionExecutor actionExecutor;
    private GuiManager guiManager;
    private BackupService backupService;
    private PlaytimeListener playtimeListener;
    private SmartRewardsExpansion placeholderExpansion;

    public ManagerLoader(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configManager = new ConfigManager(plugin);
        managers.add(configManager);
        configManager.load();
        Configs.init(configManager);

        rewardLogger = new RewardLogger(plugin);
        rewardLogger.initialize();

        vaultHook = new VaultHook(plugin);
        if (!vaultHook.setup()) {
            plugin.getLogger().warning("Vault economy not available. Money rewards disabled.");
        }

        actionExecutor = new ActionExecutor(plugin, vaultHook);

        storageManager = new StorageManager(plugin);
        managers.add(storageManager);
        storageManager.load();

        rewardLoader = new RewardLoader();
        streakService = new StreakService(rewardLogger);

        rewardManager = new RewardManager(plugin, storageManager, rewardLoader, streakService, rewardLogger);
        managers.add(rewardManager);
        rewardManager.load();

        guiManager = new GuiManager(plugin, rewardManager, storageManager);
        backupService = new BackupService(plugin);

        registerCommands();
        registerListeners();
        registerPlaceholders();

        playtimeListener = new PlaytimeListener(plugin, storageManager);
        playtimeListener.start();
    }

    public void reload() {
        configManager.reloadAll();
        rewardLoader.reload();
        rewardManager.reload();
        rewardLogger.initialize();
        plugin.getLogger().info("SmartRewards configuration reloaded.");
    }

    public void shutdown() {
        if (playtimeListener != null) {
            playtimeListener.stop();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        managers.forEach(Manager::unload);
    }

    private void registerCommands() {
        var rewardsCommand = plugin.getCommand("rewards");
        if (rewardsCommand == null) {
            plugin.getLogger().severe("Command 'rewards' not found in plugin.yml!");
            return;
        }
        RewardsCommand command = new RewardsCommand(plugin);
        rewardsCommand.setExecutor(command);
        rewardsCommand.setTabCompleter(command);
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(plugin, storageManager), plugin);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new SmartRewardsExpansion(plugin, storageManager);
            placeholderExpansion.register();
            plugin.getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public RewardLogger getLogger() {
        return rewardLogger;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public BackupService getBackupService() {
        return backupService;
    }
}
