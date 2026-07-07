package me.azuredev.smartrewards.models;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RewardDefinition {

    private final String id;
    private final RewardCategory category;
    private final String name;
    private final ItemStack displayItem;
    private final ItemStack claimedDisplay;
    private final ItemStack lockedDisplay;
    private final List<ConfigurationSection> actions;
    private final ConfigurationSection rawSection;

    public RewardDefinition(String id, RewardCategory category, String name,
                            ItemStack displayItem, ItemStack claimedDisplay,
                            ItemStack lockedDisplay, List<ConfigurationSection> actions,
                            ConfigurationSection rawSection) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.displayItem = displayItem;
        this.claimedDisplay = claimedDisplay;
        this.lockedDisplay = lockedDisplay;
        this.actions = actions;
        this.rawSection = rawSection;
    }

    public String getId() {
        return id;
    }

    public RewardCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public ItemStack getClaimedDisplay() {
        return claimedDisplay;
    }

    public ItemStack getLockedDisplay() {
        return lockedDisplay;
    }

    public List<ConfigurationSection> getActions() {
        return actions;
    }

    public ConfigurationSection getRawSection() {
        return rawSection;
    }

    public static List<ConfigurationSection> parseActions(ConfigurationSection section) {
        return parseActionsFromKey(section, "actions");
    }

    public static List<ConfigurationSection> parseActionsFromKey(ConfigurationSection section, String key) {
        List<ConfigurationSection> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        List<?> list = section.getList(key);
        if (list == null) {
            return result;
        }
        for (int i = 0; i < list.size(); i++) {
            ConfigurationSection action = section.getConfigurationSection(key + "." + i);
            if (action != null) {
                result.add(action);
            }
        }
        return result;
    }
}
