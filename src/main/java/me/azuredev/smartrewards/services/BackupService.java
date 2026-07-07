package me.azuredev.smartrewards.services;

import me.azuredev.smartrewards.SmartRewardsPlugin;
import me.azuredev.smartrewards.config.Configs;
import me.azuredev.smartrewards.storage.impl.YamlStorage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BackupService {

    private final SmartRewardsPlugin plugin;

    public BackupService(SmartRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public String createBackup() throws IOException {
        File backupDir = getBackupDir();
        backupDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFolder = new File(backupDir, "backup_" + timestamp);
        backupFolder.mkdirs();

        File dataFolder = plugin.getDataFolder();
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".yml") || file.getName().endsWith(".db"))) {
                    Files.copy(file.toPath(), new File(backupFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        cleanupOldBackups();
        return backupFolder.getName();
    }

    public void restoreBackup(String backupName) throws IOException {
        File backupFolder = new File(getBackupDir(), backupName);
        if (!backupFolder.exists()) {
            throw new IOException("Backup not found: " + backupName);
        }

        File[] files = backupFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                Files.copy(file.toPath(), new File(plugin.getDataFolder(), file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        plugin.getManagerLoader().reload();
    }

    public String getLatestBackup() {
        List<String> backups = listBackups();
        return backups.isEmpty() ? null : backups.get(0);
    }

    public List<String> listBackups() {
        File backupDir = getBackupDir();
        if (!backupDir.exists()) {
            return List.of();
        }
        File[] dirs = backupDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return List.of();
        }
        return Arrays.stream(dirs)
                .map(File::getName)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private File getBackupDir() {
        String folder = Configs.config().getConfig().getString("backup.folder", "backups");
        return new File(plugin.getDataFolder(), folder);
    }

    private void cleanupOldBackups() {
        int max = Configs.config().getConfig().getInt("backup.max-backups", 10);
        List<String> backups = new ArrayList<>(listBackups());
        for (int i = max; i < backups.size(); i++) {
            deleteBackup(backups.get(i));
        }
    }

    private void deleteBackup(String name) {
        File folder = new File(getBackupDir(), name);
        if (folder.exists()) {
            deleteRecursive(folder);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
