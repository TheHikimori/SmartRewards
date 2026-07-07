package me.azuredev.smartrewards.actions;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.hooks.VaultHook;
import me.azuredev.smartrewards.utils.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionExecutor {

    private final SmartRewardsPlugin plugin;
    private final VaultHook vaultHook;

    public ActionExecutor(SmartRewardsPlugin plugin, VaultHook vaultHook) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
    }

    public void executeAll(Player player, List<ConfigurationSection> actions, Map<String, String> placeholders) {
        executeAll(player, actions, placeholders, 1.0);
    }

    public void executeAll(Player player, List<ConfigurationSection> actions, Map<String, String> placeholders, double multiplier) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (ConfigurationSection action : actions) {
            int delay = action.getInt("delay", 0);
            if (delay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> execute(player, action, placeholders, multiplier), delay);
            } else {
                execute(player, action, placeholders, multiplier);
            }
        }
    }

    public void execute(Player player, ConfigurationSection action, Map<String, String> placeholders, double multiplier) {
        if (action == null) {
            return;
        }
        String type = action.getString("type", "").toUpperCase();
        switch (type) {
            case "COMMAND" -> executeCommand(player, action, placeholders);
            case "MONEY" -> executeMoney(player, action, multiplier);
            case "ITEM" -> executeItem(player, action, placeholders);
            case "XP" -> executeXp(player, action, multiplier);
            case "LEVEL" -> executeLevel(player, action, multiplier);
            case "EFFECT", "POTION" -> executeEffect(player, action);
            case "MESSAGE" -> executeMessage(player, action, placeholders);
            case "TITLE" -> executeTitle(player, action, placeholders);
            case "SUBTITLE" -> executeSubtitle(player, action, placeholders);
            case "ACTIONBAR" -> executeActionBar(player, action, placeholders);
            case "BOSSBAR" -> executeBossBar(player, action, placeholders);
            case "SOUND" -> executeSound(player, action);
            case "FIREWORK" -> executeFirework(player, action);
            case "PARTICLE" -> executeParticle(player, action);
            case "ITEMSADDER" -> executeItemsAdder(player, action);
            case "ORAXEN" -> executeOraxen(player, action);
            case "MMOITEMS" -> executeMmoItems(player, action);
            case "EXECUTABLEITEMS" -> executeExecutableItems(player, action);
            case "EXCELLENTCRATES", "CRATE" -> executeExcellentCrates(player, action, placeholders);
            default -> plugin.getLogger().warning("Unknown action type: " + type);
        }
    }

    private void executeCommand(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        String command = MessageUtil.replacePlaceholders(action.getString("command", ""), placeholders)
                .replace("{player}", player.getName());
        if (command.isEmpty()) {
            return;
        }
        String executor = action.getString("executor", "CONSOLE").toUpperCase();
        if (executor.equals("PLAYER")) {
            player.performCommand(command.startsWith("/") ? command.substring(1) : command);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void executeMoney(Player player, ConfigurationSection action, double multiplier) {
        if (!vaultHook.isEnabled()) {
            return;
        }
        double amount = action.getDouble("amount", 0) * multiplier;
        if (amount > 0) {
            vaultHook.getEconomy().depositPlayer(player, amount);
        }
    }

    private void executeItem(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        Material material = Material.matchMaterial(action.getString("material", "STONE"));
        if (material == null) {
            return;
        }
        int amount = action.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = action.getString("name");
            if (name != null) {
                meta.displayName(MessageUtil.parse(MessageUtil.replacePlaceholders(name, placeholders)));
            }
            if (action.contains("custom-model-data")) {
                meta.setCustomModelData(action.getInt("custom-model-data"));
            }
            item.setItemMeta(meta);
        }
        NbtHelper.applyNbt(item, action.getConfigurationSection("nbt"));
        giveItem(player, item);
    }

    private void giveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
    }

    private void executeXp(Player player, ConfigurationSection action, double multiplier) {
        int amount = (int) (action.getInt("amount", 0) * multiplier);
        player.giveExp(amount);
    }

    private void executeLevel(Player player, ConfigurationSection action, double multiplier) {
        int levels = (int) (action.getInt("amount", action.getInt("levels", 0)) * multiplier);
        player.giveExpLevels(levels);
    }

    private void executeEffect(Player player, ConfigurationSection action) {
        String effectName = action.getString("effect", action.getString("type-name", "SPEED"));
        PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
        if (effectType == null) {
            effectType = PotionEffectType.SPEED;
        }
        int duration = action.getInt("duration", 200);
        int amplifier = action.getInt("amplifier", action.getInt("level", 0));
        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
    }

    private void executeMessage(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        String text = action.getString("text", action.getString("message", ""));
        player.sendMessage(MessageUtil.parse(MessageUtil.replacePlaceholders(text, placeholders), placeholders));
    }

    private void executeTitle(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        String title = action.getString("title", "");
        String subtitle = action.getString("subtitle", "");
        int fadeIn = action.getInt("fade-in", 10);
        int stay = action.getInt("stay", 40);
        int fadeOut = action.getInt("fade-out", 10);
        player.showTitle(Title.title(
                MessageUtil.parse(MessageUtil.replacePlaceholders(title, placeholders), placeholders),
                MessageUtil.parse(MessageUtil.replacePlaceholders(subtitle, placeholders), placeholders),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));
    }

    private void executeSubtitle(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        executeTitle(player, action, placeholders);
    }

    private void executeActionBar(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        String text = action.getString("text", "");
        player.sendActionBar(MessageUtil.parse(MessageUtil.replacePlaceholders(text, placeholders), placeholders));
    }

    private void executeBossBar(Player player, ConfigurationSection action, Map<String, String> placeholders) {
        String text = action.getString("text", "");
        BossBar.Color color = BossBar.Color.valueOf(action.getString("color", "YELLOW").toUpperCase());
        BossBar.Overlay style = BossBar.Overlay.valueOf(action.getString("style", "SOLID").toUpperCase());
        int duration = action.getInt("duration", 60);
        BossBar bossBar = BossBar.bossBar(
                MessageUtil.parse(MessageUtil.replacePlaceholders(text, placeholders), placeholders),
                1.0f, color, style
        );
        player.showBossBar(bossBar);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.hideBossBar(bossBar), duration);
    }

    private void executeSound(Player player, ConfigurationSection action) {
        Sound sound = parseSound(action.getString("sound", "ENTITY_PLAYER_LEVELUP"));
        if (sound == null) {
            return;
        }
        float volume = (float) action.getDouble("volume", 1.0);
        float pitch = (float) action.getDouble("pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void executeFirework(Player player, ConfigurationSection action) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        meta.setPower(action.getInt("power", 1));
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 2L);
    }

    private void executeParticle(Player player, ConfigurationSection action) {
        Particle particle = parseParticle(action.getString("type", "TOTEM"));
        if (particle == null) {
            return;
        }
        int count = action.getInt("count", 20);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.1);
    }

    private void executeItemsAdder(Player player, ConfigurationSection action) {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return;
        }
        String itemId = action.getString("id", action.getString("item", ""));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "iagive " + player.getName() + " " + itemId + " " + action.getInt("amount", 1));
    }

    private void executeOraxen(Player player, ConfigurationSection action) {
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            return;
        }
        String itemId = action.getString("id", action.getString("item", ""));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "oraxen give " + player.getName() + " " + itemId + " " + action.getInt("amount", 1));
    }

    private void executeMmoItems(Player player, ConfigurationSection action) {
        if (Bukkit.getPluginManager().getPlugin("MMOItems") == null) {
            return;
        }
        String type = action.getString("item-type", "SWORD");
        String id = action.getString("id", action.getString("item", ""));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mi give " + type + " " + id + " " + player.getName() + " " + action.getInt("amount", 1));
    }

    private void executeExecutableItems(Player player, ConfigurationSection action) {
        if (Bukkit.getPluginManager().getPlugin("ExecutableItems") == null) {
            return;
        }
        String id = action.getString("id", action.getString("item", ""));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ei give " + player.getName() + " " + id + " " + action.getInt("amount", 1));
    }

    public void playClaimAnimations(Player player, String rewardName) {
        ConfigurationSection anim = Configs.animations().getConfig().getConfigurationSection("claim");
        if (anim == null) {
            return;
        }
        Map<String, String> ph = Map.of("reward", rewardName, "player", player.getName());

        ConfigurationSection title = anim.getConfigurationSection("title");
        if (title != null && title.getBoolean("enabled", true)) {
            executeTitle(player, title, ph);
        }
        ConfigurationSection actionbar = anim.getConfigurationSection("actionbar");
        if (actionbar != null && actionbar.getBoolean("enabled", true)) {
            executeActionBar(player, actionbar, ph);
        }
        ConfigurationSection bossbar = anim.getConfigurationSection("bossbar");
        if (bossbar != null && bossbar.getBoolean("enabled", false)) {
            executeBossBar(player, bossbar, ph);
        }
        if (anim.getConfigurationSection("firework") != null && anim.getConfigurationSection("firework").getBoolean("enabled", true)) {
            executeFirework(player, anim.getConfigurationSection("firework"));
        }
        if (anim.getConfigurationSection("particles") != null && anim.getConfigurationSection("particles").getBoolean("enabled", true)) {
            executeParticle(player, anim.getConfigurationSection("particles"));
        }
        if (anim.getBoolean("sound.enabled", true) && anim.getBoolean("sound.use-sounds-yml", true)) {
            ConfigurationSection sound = Configs.sounds().getConfig().getConfigurationSection("claim");
            if (sound != null && sound.getBoolean("enabled", true)) {
                executeSound(player, sound);
            }
        }
    }

    private Sound parseSound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception e) {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase().replace(".", "_"));
            return RegistrySoundHelper.getSound(key);
        }
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Particle.TOTEM_OF_UNDYING;
        }
    }
}
