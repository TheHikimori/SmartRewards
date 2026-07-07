package me.azuredev.smartrewards.config;

public final class Configs {

    private static ConfigManager manager;

    private Configs() {}

    public static void init(ConfigManager configManager) {
        manager = configManager;
    }

    public static ConfigFile config() {
        return manager.get(ConfigType.CONFIG);
    }

    public static ConfigFile messages() {
        return manager.get(ConfigType.MESSAGES);
    }

    public static ConfigFile menus() {
        return manager.get(ConfigType.MENUS);
    }

    public static ConfigFile daily() {
        return manager.get(ConfigType.DAILY);
    }

    public static ConfigFile weekly() {
        return manager.get(ConfigType.WEEKLY);
    }

    public static ConfigFile monthly() {
        return manager.get(ConfigType.MONTHLY);
    }

    public static ConfigFile special() {
        return manager.get(ConfigType.SPECIAL);
    }

    public static ConfigFile streak() {
        return manager.get(ConfigType.STREAK);
    }

    public static ConfigFile playtime() {
        return manager.get(ConfigType.PLAYTIME);
    }

    public static ConfigFile vip() {
        return manager.get(ConfigType.VIP);
    }

    public static ConfigFile database() {
        return manager.get(ConfigType.DATABASE);
    }

    public static ConfigFile sounds() {
        return manager.get(ConfigType.SOUNDS);
    }

    public static ConfigFile animations() {
        return manager.get(ConfigType.ANIMATIONS);
    }

    public static ConfigFile items() {
        return manager.get(ConfigType.ITEMS);
    }

    public static ConfigFile permissions() {
        return manager.get(ConfigType.PERMISSIONS);
    }

    public static ConfigFile placeholders() {
        return manager.get(ConfigType.PLACEHOLDERS);
    }
}