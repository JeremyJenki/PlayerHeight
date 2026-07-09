package io.github.jeremyjenki.playerheight;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private File dataFile;
    private YamlConfiguration data;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        dataFile = new File(plugin.getDataFolder(), "playerscales.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create playerscales.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        plugin.getLogger().info("Loaded player scale data from playerscales.yml");
    }

    public void disconnect() {
        save();
        plugin.getLogger().info("Saved player scale data.");
    }

    // Empty = Never touched this player
    public Optional<Double> loadScale(UUID uuid) {
        String key = uuid.toString();
        if (!data.contains(key)) return Optional.empty();
        return Optional.of(data.getDouble(key));
    }

    public void saveScale(UUID uuid, double scale) {
        data.set(uuid.toString(), scale);
        save();
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save playerscales.yml", e);
        }
    }
}
