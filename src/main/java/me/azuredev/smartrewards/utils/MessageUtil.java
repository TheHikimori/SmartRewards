package me.azuredev.smartrewards.utils;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {}

    public static Component parse(String text) {
        return parse(text, null);
    }

    public static Component parse(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String prefix = Configs.config().getConfig().getString("settings.prefix", "");
        text = text.replace("{prefix}", prefix);
        text = applyPlaceholderApi(text, player);
        if (text.contains("<") && text.contains(">")) {
            return MINI.deserialize(text);
        }
        if (text.contains("&")) {
            return LEGACY.deserialize(text);
        }
        return Component.text(text);
    }

    public static Component parse(String text, Map<String, String> placeholders) {
        return parse(text, placeholders, null);
    }

    public static Component parse(String text, Map<String, String> placeholders, Player player) {
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return parse(text, player);
    }

    public static void send(Player player, String key) {
        String message = Configs.messages().getConfig().getString(key);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(parse(message, player));
        }
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        String message = Configs.messages().getConfig().getString(key);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(parse(message, placeholders, player));
        }
    }

    public static void sendList(Player player, String key) {
        List<String> messages = Configs.messages().getConfig().getStringList(key);
        for (String line : messages) {
            player.sendMessage(parse(line, player));
        }
    }

    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    public static String applyPlaceholderApi(String text, Player player) {
        if (text == null || player == null || !text.contains("%")) {
            return text;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    public static ZoneId getTimezone() {
        String tz = Configs.config().getConfig().getString("settings.timezone", "UTC");
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            SmartRewardsPlugin.getInstance().getLogger().warning("Invalid timezone: " + tz + ", using UTC");
            return ZoneId.of("UTC");
        }
    }

    public static void broadcastAdmin(String message) {
        Component component = parse(message);
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(getNode("admin")))
                .forEach(p -> p.sendMessage(component));
    }

    public static String getNode(String key) {
        return Configs.permissions().getConfig().getString("nodes." + key, "smartrewards." + key);
    }
}
