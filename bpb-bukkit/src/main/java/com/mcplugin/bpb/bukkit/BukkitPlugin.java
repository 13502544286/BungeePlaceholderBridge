package com.mcplugin.bpb.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Bukkit backend for BungeePlaceholderBridge v2.
 * Periodically resolves PlaceholderAPI placeholders and uploads values to Redis.
 */
public class BukkitPlugin extends JavaPlugin {

    private static BukkitPlugin instance;
    private JedisPool jedisPool;
    private List<String> placeholders = new ArrayList<>();
    private int uploadInterval = 5; // seconds
    private int expireSeconds = 30; // seconds
    private String redisPrefix = "bpb";
    private UploadTask uploadTask;
    private long uploadsTotal = 0;

    public static BukkitPlugin getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfiguration();

        String host = getConfig().getString("Redis.Host", "localhost");
        int port = getConfig().getInt("Redis.Port", 6379);
        String password = getConfig().getString("Redis.Password", "");
        boolean useSsl = getConfig().getBoolean("Redis.SSL", false);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);

        try {
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 5000, password, useSsl);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 5000, useSsl);
            }
            // Test connection
            try (Jedis j = jedisPool.getResource()) {
                j.ping();
            }
            getLogger().info("Redis connected: " + host + ":" + port);
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Redis: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Start upload task
        uploadTask = new UploadTask();
        uploadTask.runTaskTimerAsynchronously(this, 20L * 2, 20L * uploadInterval);

        getLogger().info("========================================");
        getLogger().info("BungeePlaceholderBridge Bukkit v" + getDescription().getVersion());
        getLogger().info("Upload interval: " + uploadInterval + "s");
        getLogger().info("Expire: " + expireSeconds + "s");
        getLogger().info("Placeholders: " + placeholders);
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (uploadTask != null) uploadTask.cancel();
        if (jedisPool != null && !jedisPool.isClosed()) jedisPool.close();
        instance = null;
    }

    void reloadConfiguration() {
        reloadConfig();
        placeholders = getConfig().getStringList("Placeholders");
        if (placeholders == null) placeholders = new ArrayList<>();
        // Normalize: remove % if present
        List<String> normalized = new ArrayList<>();
        for (String ph : placeholders) {
            ph = ph.trim();
            if (ph.startsWith("%") && ph.endsWith("%")) ph = ph.substring(1, ph.length() - 1);
            if (!ph.isEmpty()) normalized.add(ph);
        }
        placeholders = normalized;
        uploadInterval = getConfig().getInt("UploadInterval", 5);
        if (uploadInterval < 1) uploadInterval = 1;
        expireSeconds = getConfig().getInt("ExpireSeconds", 30);
        if (expireSeconds < uploadInterval + 5) expireSeconds = uploadInterval + 5;
        redisPrefix = getConfig().getString("Redis.Prefix", "bpb");
    }

    /**
     * Periodic task that uploads placeholder values to Redis.
     */
    class UploadTask extends BukkitRunnable {
        @Override
        public void run() {
            if (jedisPool == null || jedisPool.isClosed()) return;
            if (placeholders.isEmpty()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    UUID uuid = player.getUniqueId();
                    for (String ph : placeholders) {
                        try {
                            String value = PlaceholderAPI.setPlaceholders(player, "%" + ph + "%");
                            String key = redisPrefix + ":player:" + uuid.toString() + ":" + ph;
                            jedis.setex(key, expireSeconds, value);
                            uploadsTotal++;
                        } catch (Exception e) {
                            getLogger().log(Level.FINE, "Failed to resolve %" + ph + "% for " + player.getName(), e);
                        }
                    }
                } catch (Exception e) {
                    getLogger().log(Level.FINE, "Redis upload error for " + player.getName(), e);
                }
            }
        }
    }

    public JedisPool getJedisPool() { return jedisPool; }
    public List<String> getPlaceholders() { return new ArrayList<>(placeholders); }
    public int getUploadInterval() { return uploadInterval; }
    public long getUploadsTotal() { return uploadsTotal; }
    public String getRedisPrefix() { return redisPrefix; }
}
