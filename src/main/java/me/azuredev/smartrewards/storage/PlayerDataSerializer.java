package me.azuredev.smartrewards.storage;

import me.azuredev.smartrewards.models.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerDataSerializer {

    private PlayerDataSerializer() {}

    public static void serialize(PlayerData data, ConfigurationSection section) {
        section.set("name", data.getName());
        section.set("last-claim-date", data.getLastClaimDate() != null ? data.getLastClaimDate().toString() : null);
        section.set("streak", data.getStreak());
        section.set("total-joins", data.getTotalJoins());
        section.set("missed-days", data.getMissedDays());
        section.set("playtime", data.getPlaytime());
        section.set("first-join-claimed", data.isFirstJoinRewardClaimed());
        section.set("weekly-claimed", data.isWeeklyRewardClaimed());
        section.set("monthly-claimed", data.isMonthlyRewardClaimed());
        section.set("weekly-progress", data.getWeeklyProgress());
        section.set("monthly-progress", data.getMonthlyProgress());
        section.set("claimed-today", data.isClaimedToday());
        section.set("claimed-daily", new ArrayList<>(data.getClaimedDailyRewards()));
        section.set("claimed-weekly", new ArrayList<>(data.getClaimedWeeklyRewards()));
        section.set("claimed-monthly", new ArrayList<>(data.getClaimedMonthlyRewards()));
        section.set("claimed-special", new ArrayList<>(data.getClaimedSpecialRewards()));
        section.set("claimed-playtime", new ArrayList<>(data.getClaimedPlaytimeRewards()));
        section.set("claimed-join-milestones", new ArrayList<>(data.getClaimedJoinMilestones()));
        section.set("claimed-streak", new ArrayList<>(data.getClaimedStreakRewards()));
    }

    public static PlayerData deserialize(UUID uuid, ConfigurationSection section) {
        PlayerData data = new PlayerData(uuid);
        if (section == null) {
            return data;
        }

        data.setName(section.getString("name"));
        String lastClaim = section.getString("last-claim-date");
        if (lastClaim != null && !lastClaim.isEmpty()) {
            data.setLastClaimDate(LocalDate.parse(lastClaim));
        }
        data.setStreak(section.getInt("streak", 0));
        data.setTotalJoins(section.getInt("total-joins", 0));
        data.setMissedDays(section.getInt("missed-days", 0));
        data.setPlaytime(section.getLong("playtime", 0));
        data.setFirstJoinRewardClaimed(section.getBoolean("first-join-claimed", false));
        data.setWeeklyRewardClaimed(section.getBoolean("weekly-claimed", false));
        data.setMonthlyRewardClaimed(section.getBoolean("monthly-claimed", false));
        data.setWeeklyProgress(section.getInt("weekly-progress", 0));
        data.setMonthlyProgress(section.getInt("monthly-progress", 0));
        data.setClaimedToday(section.getBoolean("claimed-today", false));

        data.getMutableClaimedDailyRewards().addAll(section.getIntegerList("claimed-daily"));
        data.getMutableClaimedWeeklyRewards().addAll(section.getIntegerList("claimed-weekly"));
        data.getMutableClaimedMonthlyRewards().addAll(section.getIntegerList("claimed-monthly"));
        data.getMutableClaimedSpecialRewards().addAll(section.getStringList("claimed-special"));
        data.getMutableClaimedPlaytimeRewards().addAll(section.getIntegerList("claimed-playtime"));
        data.getMutableClaimedJoinMilestones().addAll(section.getIntegerList("claimed-join-milestones"));
        data.getMutableClaimedStreakRewards().addAll(section.getIntegerList("claimed-streak"));

        return data;
    }

    public static String toYamlString(PlayerData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        serialize(data, yaml);
        return yaml.saveToString();
    }

    public static PlayerData fromYamlString(UUID uuid, String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            return new PlayerData(uuid);
        }
        return deserialize(uuid, config);
    }
}
