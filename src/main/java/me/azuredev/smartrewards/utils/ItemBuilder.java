package me.azuredev.smartrewards.utils;

import me.azuredev.smartrewards.config.Configs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemBuilder {

    private ItemBuilder() {}

    public static ItemStack fromConfig(ConfigurationSection section) {
        return fromConfig(section, Map.of());
    }

    public static ItemStack fromConfig(ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        String template = section.getString("template");
        if (template != null && !template.equals("none")) {
            ConfigurationSection templateSection = Configs.items().getConfig()
                    .getConfigurationSection("templates." + template);
            if (templateSection != null) {
                section = mergeSection(templateSection, section);
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String name = section.getString("name");
        if (name != null) {
            meta.displayName(MessageUtil.parse(MessageUtil.replacePlaceholders(name, placeholders)));
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(MessageUtil.parse(MessageUtil.replacePlaceholders(line, placeholders)));
            }
            meta.lore(loreComponents);
        }

        if (section.contains("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        if (section.getBoolean("glow", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        NbtHelper.applyNbt(item, section.getConfigurationSection("nbt"));
        return item;
    }

    private static ConfigurationSection mergeSection(ConfigurationSection base, ConfigurationSection override) {
        org.bukkit.configuration.file.YamlConfiguration merged = new org.bukkit.configuration.file.YamlConfiguration();
        copySection(base, merged, "");
        copySection(override, merged, "");
        return merged;
    }

    private static void copySection(ConfigurationSection from, org.bukkit.configuration.file.YamlConfiguration to, String prefix) {
        for (String key : from.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (from.isConfigurationSection(key)) {
                copySection(from.getConfigurationSection(key), to, path);
            } else {
                to.set(path, from.get(key));
            }
        }
    }
}
