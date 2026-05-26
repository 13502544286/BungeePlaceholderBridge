package com.mcplugin.bpb.bungee;

import com.mcplugin.bpb.api.BpbAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * BungeePlaceholderBridge BungeeCord proxy plugin.
 * Reads placeholder values from Redis (uploaded by backend Spigot servers)
 * and provides them as %bpb_xxx% placeholders for other Bungee plugins.
 */
public class BungeePlugin extends Plugin implements Listener {

    private BpbAPI api;
    private Configuration config;

    @Override
    public void onEnable() {
        // Load config manually (BungeeCord doesn't have built-in getConfig())
        loadConfig();

        String host = config.getString("Redis.Host", "localhost");
        int port = config.getInt("Redis.Port", 6379);
        String password = config.getString("Redis.Password", "");
        String prefix = config.getString("Redis.Prefix", "bpb");

        try {
            api = new BpbAPI(host, port, password, prefix);
            BpbAPI.setInstance(api);

            // Test connection
            try (redis.clients.jedis.Jedis j = api.getJedisPool().getResource()) {
                j.ping();
            }

            getLogger().info("Redis connected: " + host + ":" + port);
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Redis: " + e.getMessage());
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("========================================");
        getLogger().info("BungeePlaceholderBridge Bungee v" + getDescription().getVersion());
        getLogger().info("Redis: " + host + ":" + port);
        getLogger().info("Prefix: " + prefix);
        getLogger().info("Use %bpb_xxx% to access backend placeholders.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        BpbAPI.setInstance(null);
        if (api != null) {
            api.shutdown();
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (api != null) {
            api.deletePlayerValues(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Load configuration from config.yml (BungeeCord style).
     */
    private void loadConfig() {
        try {
            File folder = getDataFolder();
            if (!folder.exists()) {
                folder.mkdir();
            }
            File configFile = new File(folder, "config.yml");
            if (!configFile.exists()) {
                // Copy default config from jar
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile.toPath());
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            config = new Configuration(); // Empty fallback
        }
    }
}
