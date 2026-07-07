package me.azuredev.smartrewards.config;

public enum ConfigType {

    CONFIG("config.yml"),
    MESSAGES("messages.yml"),
    MENUS("menus.yml"),
    DAILY("daily.yml"),
    WEEKLY("weekly.yml"),
    MONTHLY("monthly.yml"),
    SPECIAL("special.yml"),
    STREAK("streak.yml"),
    PLAYTIME("playtime.yml"),
    VIP("vip.yml"),
    DATABASE("database.yml"),
    SOUNDS("sounds.yml"),
    ANIMATIONS("animations.yml"),
    ITEMS("items.yml"),
    PERMISSIONS("permissions.yml"),
    PLACEHOLDERS("placeholders.yml");

    private final String fileName;

    ConfigType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

}