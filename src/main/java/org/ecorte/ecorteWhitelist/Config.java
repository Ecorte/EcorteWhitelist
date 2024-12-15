package org.ecorte.ecorteWhitelist;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class Config {

    private final JavaPlugin plugin;
    public String dbUrl;

    public String username;

    public String password;

    public static Config instance = null;

    public boolean requireWhitelist = false;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        this.dbUrl = config.getString("database.dbUrl", "jdbc:mysql://localhost:3306/minecraft");
        this.username = config.getString("database.username", "root");
        this.password = config.getString("database.password", "");
        this.requireWhitelist = config.getBoolean("config.requireWhitelist", true);
    }
    public static Config getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new Config(plugin);
        }
        return instance;
    }
}
