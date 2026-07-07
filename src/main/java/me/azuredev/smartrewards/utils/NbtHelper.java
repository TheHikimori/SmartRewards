package me.azuredev.smartrewards.utils;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class NbtHelper {

    private NbtHelper() {}

    public static void applyNbt(ItemStack item, ConfigurationSection section) {
        if (item == null || section == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        SmartRewardsPlugin plugin = SmartRewardsPlugin.getInstance();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key.toLowerCase());
            if (value instanceof Number number) {
                container.set(namespacedKey, PersistentDataType.DOUBLE, number.doubleValue());
            } else if (value instanceof Boolean bool) {
                container.set(namespacedKey, PersistentDataType.BYTE, (byte) (bool ? 1 : 0));
            } else {
                container.set(namespacedKey, PersistentDataType.STRING, String.valueOf(value));
            }
        }
        item.setItemMeta(meta);
    }
}
