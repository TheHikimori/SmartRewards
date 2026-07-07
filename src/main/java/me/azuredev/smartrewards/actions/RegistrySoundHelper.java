package me.azuredev.smartrewards.actions;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

final class RegistrySoundHelper {

    private RegistrySoundHelper() {}

    static Sound getSound(NamespacedKey key) {
        Sound sound = Registry.SOUNDS.get(key);
        return sound != null ? sound : Sound.ENTITY_PLAYER_LEVELUP;
    }
}
