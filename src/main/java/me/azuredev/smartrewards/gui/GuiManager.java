package me.azuredev.smartrewards.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.models.RewardDefinition;
import me.azuredev.smartrewards.rewards.ClaimResult;
import me.azuredev.smartrewards.rewards.RewardLoader;
import me.azuredev.smartrewards.rewards.RewardManager;
import me.azuredev.smartrewards.storage.StorageManager;
import me.azuredev.smartrewards.utils.MessageUtil;
import me.azuredev.smartrewards.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiManager {

    private final SmartRewardsPlugin plugin;
    private final RewardManager rewardManager;
    private final StorageManager storageManager;

    public GuiManager(SmartRewardsPlugin plugin, RewardManager rewardManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
        this.storageManager = storageManager;
    }

    public void openMainMenu(Player player) {
        PlayerData data = storageManager.getCached(player.getUniqueId());
        if (data == null) {
            return;
        }

        ConfigurationSection menu = Configs.menus().getConfig().getConfigurationSection("main-menu");
        if (menu == null) {
            return;
        }

        Map<String, String> ph = buildPlaceholders(player, data);
        Component title = MessageUtil.parse(menu.getString("title", "SmartRewards"), ph);
        int rows = menu.getInt("rows", 6);

        Gui gui = Gui.gui().title(title).rows(rows).disableAllInteractions().create();

        fillFiller(gui, menu, rows);

        ConfigurationSection items = menu.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int slot = itemSection.getInt("slot", -1);
                if (slot < 0) {
                    continue;
                }
                ItemStack item = me.azuredev.smartrewards.utils.ItemBuilder.fromConfig(itemSection, ph);
                String action = itemSection.getString("action", "NONE");
                gui.setItem(slot, new GuiItem(item, event -> {
                    event.setCancelled(true);
                    handleAction(player, action, data);
                }));
            }
        }

        playMenuSound(player);
        gui.open(player);
    }

    public void openCalendar(Player player) {
        PlayerData data = storageManager.getCached(player.getUniqueId());
        if (data == null) {
            return;
        }

        ConfigurationSection menu = Configs.menus().getConfig().getConfigurationSection("calendar");
        if (menu == null) {
            return;
        }

        Map<String, String> ph = buildPlaceholders(player, data);
        Component title = MessageUtil.parse(menu.getString("title", "Calendar"), ph);
        int rows = menu.getInt("rows", 6);

        Gui gui = Gui.gui().title(title).rows(rows).disableAllInteractions().create();
        fillFiller(gui, menu, rows);

        RewardLoader loader = rewardManager.getRewardLoader();
        List<Integer> daySlots = menu.getIntegerList("day-slots");
        int slotIndex = 0;

        for (Map.Entry<Integer, RewardDefinition> entry : loader.getDailyRewards().entrySet()) {
            if (slotIndex >= daySlots.size()) {
                break;
            }
            int day = entry.getKey();
            RewardDefinition reward = entry.getValue();
            int slot = daySlots.get(slotIndex++);

            ItemStack display = resolveDayItem(reward, day, data, menu);
            gui.setItem(slot, new GuiItem(display, event -> {
                event.setCancelled(true);
                if (canClaimDay(day, data)) {
                    ClaimResult result = rewardManager.claimDaily(player, data);
                    handleClaimResult(player, result);
                    openCalendar(player);
                }
            }));
        }

        ConfigurationSection nav = menu.getConfigurationSection("navigation");
        if (nav != null) {
            for (String key : nav.getKeys(false)) {
                ConfigurationSection itemSection = nav.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int slot = itemSection.getInt("slot", -1);
                if (slot < 0) {
                    continue;
                }
                ItemStack item = me.azuredev.smartrewards.utils.ItemBuilder.fromConfig(itemSection, ph);
                String action = itemSection.getString("action", "NONE");
                gui.setItem(slot, new GuiItem(item, event -> {
                    event.setCancelled(true);
                    handleAction(player, action, data);
                }));
            }
        }

        playMenuSound(player);
        gui.open(player);
    }

    public void openEditor(Player player) {
        if (!Configs.config().getConfig().getBoolean("editor.enabled", true)) {
            MessageUtil.send(player, "editor-disabled");
            return;
        }
        ConfigurationSection menu = Configs.menus().getConfig().getConfigurationSection("editor");
        if (menu == null) {
            return;
        }
        Component title = MessageUtil.parse(menu.getString("title", "Editor"));
        Gui gui = Gui.gui().title(title).rows(menu.getInt("rows", 3)).disableAllInteractions().create();
        gui.setItem(4, dev.triumphteam.gui.builder.item.ItemBuilder.from(org.bukkit.Material.BOOK)
                .name(MessageUtil.parse("<yellow>Редактор наград</yellow>"))
                .lore(MessageUtil.parse("<gray>Настройте награды в конфигурационных файлах</gray>"))
                .asGuiItem(event -> event.setCancelled(true)));
        gui.open(player);
    }

    private ItemStack resolveDayItem(RewardDefinition reward, int day, PlayerData data, ConfigurationSection menu) {
        int currentDay = Math.max(1, data.getStreak());
        boolean claimed = data.getClaimedDailyRewards().contains(day)
                || (day == currentDay && data.isClaimedToday());
        boolean available = day == currentDay && !data.isClaimedToday();
        boolean missed = day < currentDay && !data.getClaimedDailyRewards().contains(day);

        if (claimed && reward.getClaimedDisplay() != null) {
            return reward.getClaimedDisplay().clone();
        }
        if (available && reward.getDisplayItem() != null) {
            return reward.getDisplayItem().clone();
        }
        if (missed) {
            ConfigurationSection missedSection = menu.getConfigurationSection("states.missed");
            if (missedSection != null) {
                return me.azuredev.smartrewards.utils.ItemBuilder.fromConfig(missedSection);
            }
        }
        if (reward.getLockedDisplay() != null) {
            return reward.getLockedDisplay().clone();
        }
        return reward.getDisplayItem() != null ? reward.getDisplayItem().clone() : new ItemStack(org.bukkit.Material.BARRIER);
    }

    private boolean canClaimDay(int day, PlayerData data) {
        int currentDay = Math.max(1, data.getStreak());
        return day == currentDay && !data.isClaimedToday();
    }

    private void handleAction(Player player, String action, PlayerData data) {
        switch (action.toUpperCase()) {
            case "OPEN_CALENDAR" -> openCalendar(player);
            case "CLAIM" -> {
                ClaimResult result = rewardManager.claimDaily(player, data);
                handleClaimResult(player, result);
            }
            case "BACK" -> openMainMenu(player);
            case "CLOSE" -> player.closeInventory();
            default -> {}
        }
    }

    private void handleClaimResult(Player player, ClaimResult result) {
        switch (result) {
            case SUCCESS -> {}
            case ALREADY_CLAIMED -> MessageUtil.send(player, "reward-already-claimed");
            case NOT_AVAILABLE -> MessageUtil.send(player, "reward-not-available");
            case DISABLED -> MessageUtil.send(player, "reward-not-available");
            case ERROR -> MessageUtil.send(player, "give-failed", Map.of("error", result.getMessage() != null ? result.getMessage() : "error"));
        }
    }

    private void fillFiller(Gui gui, ConfigurationSection menu, int rows) {
        String template = menu.getString("filler-template");
        if (template == null) {
            return;
        }
        ConfigurationSection filler = Configs.items().getConfig().getConfigurationSection("templates." + template);
        if (filler == null) {
            return;
        }
        ItemStack fillerItem = me.azuredev.smartrewards.utils.ItemBuilder.fromConfig(filler);
        GuiItem guiItem = new GuiItem(fillerItem, event -> event.setCancelled(true));
        for (int i = 0; i < rows * 9; i++) {
            gui.setItem(i, guiItem);
        }
    }

    private Map<String, String> buildPlaceholders(Player player, PlayerData data) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", player.getName());
        ph.put("streak", String.valueOf(data.getStreak()));
        ph.put("next", rewardManager.getNextRewardName(player));
        ph.put("remaining", TimeUtil.formatDuration(TimeUtil.secondsUntilMidnight()));
        ph.put("time", TimeUtil.formatPlaytime(data.getPlaytime()));
        return ph;
    }

    private void playMenuSound(Player player) {
        ConfigurationSection sound = Configs.sounds().getConfig().getConfigurationSection("menu-open");
        if (sound != null && sound.getBoolean("enabled", true)) {
            plugin.getManagerLoader().getActionExecutor().execute(player, sound, Map.of(), 1.0);
        }
    }
}
