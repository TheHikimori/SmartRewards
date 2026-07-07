package me.azuredev.smartrewards.commands;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.models.PlayerData;
import me.azuredev.smartrewards.rewards.ClaimResult;
import me.azuredev.smartrewards.services.BackupService;
import me.azuredev.smartrewards.storage.StorageManager;
import me.azuredev.smartrewards.utils.MessageUtil;
import me.azuredev.smartrewards.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RewardsCommand implements CommandExecutor, TabCompleter {

    private final SmartRewardsPlugin plugin;

    public RewardsCommand(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("player-only")));
            return true;
        }
            if (!player.hasPermission(MessageUtil.getNode("use"))) {
                sendNoPermission(sender);
                return true;
            }
            plugin.getManagerLoader().getGuiManager().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "claim" -> handleClaim(sender);
            case "streak" -> handleStreak(sender);
            case "next" -> handleNext(sender);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "give" -> handleGive(sender, args);
            case "editor" -> handleEditor(sender);
            case "info" -> handleInfo(sender, args);
            case "backup" -> handleBackup(sender);
            case "restore" -> handleRestore(sender, args);
            default -> {
                if (sender instanceof Player player) {
                    MessageUtil.sendList(player, "help");
                }
                yield true;
            }
        };
    }

    private boolean handleClaim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (!player.hasPermission(MessageUtil.getNode("use"))) {
            sendNoPermission(sender);
            return true;
        }
        PlayerData data = plugin.getManagerLoader().getStorageManager().getCached(player.getUniqueId());
        if (data == null) {
            MessageUtil.send(player, "reward-not-available");
            return true;
        }
        ClaimResult result = plugin.getManagerLoader().getRewardManager().claimDaily(player, data);
        if (result == ClaimResult.SUCCESS) {
            MessageUtil.send(player, "reward-claimed", Map.of("reward", plugin.getManagerLoader().getRewardManager().getNextRewardName(player)));
        } else if (result == ClaimResult.ALREADY_CLAIMED) {
            MessageUtil.send(player, "reward-already-claimed");
        } else {
            MessageUtil.send(player, "reward-not-available");
        }
        return true;
    }

    private boolean handleStreak(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (!player.hasPermission(MessageUtil.getNode("use"))) {
            sendNoPermission(sender);
            return true;
        }
        PlayerData data = plugin.getManagerLoader().getStorageManager().getCached(player.getUniqueId());
        int streak = data != null ? data.getStreak() : 0;
        MessageUtil.send(player, "streak-info", Map.of("streak", String.valueOf(streak)));
        return true;
    }

    private boolean handleNext(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (!player.hasPermission(MessageUtil.getNode("use"))) {
            sendNoPermission(sender);
            return true;
        }
        PlayerData data = plugin.getManagerLoader().getStorageManager().getCached(player.getUniqueId());
        String next = plugin.getManagerLoader().getRewardManager().getNextRewardName(player);
        int day = data != null ? data.getStreak() : 1;
        MessageUtil.send(player, "next-reward", Map.of("reward", next, "day", String.valueOf(day)));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(MessageUtil.getNode("reload")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        try {
            plugin.getManagerLoader().reload();
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("reload-success")));
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("reload-failed").replace("{error}", e.getMessage())));
        }
        return true;
    }

    private String ConfigsMessage(String key) {
        return me.azuredev.smartrewards.config.Configs.messages().getConfig().getString(key, key);
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MessageUtil.getNode("reset")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /rewards reset <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        StorageManager storage = plugin.getManagerLoader().getStorageManager();
        storage.getOrLoad(target.getUniqueId()).thenAccept(data -> {
            plugin.getManagerLoader().getRewardManager().resetPlayer(data);
            storage.save(data);
            plugin.getManagerLoader().getLogger().logAdmin(sender.getName(), "RESET", target.getName());
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MessageUtil.parse(ConfigsMessage("reset-success").replace("{player}", target.getName()))));
        });
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MessageUtil.getNode("give")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /rewards give <player> <type> [id]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("player-not-found").replace("{player}", args[1])));
            return true;
        }
        String type = args[2];
        String id = args.length > 3 ? args[3] : "1";
        try {
            ClaimResult result = plugin.getManagerLoader().getRewardManager().giveReward(target, type, id);
            if (result.isSuccess()) {
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("give-success")
                        .replace("{player}", target.getName()).replace("{type}", type)));
            } else {
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("give-failed").replace("{error}", "Unknown reward")));
            }
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("give-failed").replace("{error}", e.getMessage())));
        }
        return true;
    }

    private boolean handleEditor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (!player.hasPermission(MessageUtil.getNode("editor")) && !player.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        plugin.getManagerLoader().getGuiManager().openEditor(player);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MessageUtil.getNode("info")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        String targetName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        if (targetName == null) {
            sender.sendMessage("Usage: /rewards info <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        plugin.getManagerLoader().getStorageManager().getOrLoad(target.getUniqueId()).thenAccept(data -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-header").replace("{player}", targetName)));
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-streak").replace("{streak}", String.valueOf(data.getStreak()))));
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-total-joins").replace("{joins}", String.valueOf(data.getTotalJoins()))));
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-playtime").replace("{playtime}", TimeUtil.formatPlaytime(data.getPlaytime()))));
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-last-claim").replace("{date}", TimeUtil.formatDate(data.getLastClaimDate()))));
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("info-missed").replace("{missed}", String.valueOf(data.getMissedDays()))));
            });
        });
        return true;
    }

    private boolean handleBackup(CommandSender sender) {
        if (!sender.hasPermission(MessageUtil.getNode("backup")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        BackupService backup = plugin.getManagerLoader().getBackupService();
        try {
            String file = backup.createBackup();
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("backup-success").replace("{file}", file)));
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("backup-failed").replace("{error}", e.getMessage())));
        }
        return true;
    }

    private boolean handleRestore(CommandSender sender, String[] args) {
        if (!sender.hasPermission(MessageUtil.getNode("backup")) && !sender.hasPermission(MessageUtil.getNode("admin"))) {
            sendNoPermission(sender);
            return true;
        }
        BackupService backup = plugin.getManagerLoader().getBackupService();
        try {
            String file = args.length > 1 ? args[1] : backup.getLatestBackup();
            if (file == null) {
                sender.sendMessage(MessageUtil.parse(ConfigsMessage("restore-no-backups")));
                return true;
            }
            backup.restoreBackup(file);
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("restore-success").replace("{file}", file)));
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.parse(ConfigsMessage("restore-failed").replace("{error}", e.getMessage())));
        }
        return true;
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(MessageUtil.parse(ConfigsMessage("no-permission")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("claim", "streak", "next", "reload", "reset", "give", "editor", "info", "backup", "restore"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "reset", "give", "info" -> null;
                case "restore" -> plugin.getManagerLoader().getBackupService().listBackups();
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("daily", "weekly", "monthly", "special", "playtime", "first-join"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
