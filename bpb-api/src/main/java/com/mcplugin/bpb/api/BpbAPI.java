package com.mcplugin.bpb.api;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

/**
 * BungeePlaceholderBridge Public API.
 * Provides access to Redis-stored placeholder values on the BungeeCord proxy.
 *
 * Usage:
 *   String value = BpbAPI.getInstance().getValue(player, "player_name");
 *   // Returns the last uploaded value from the backend Spigot server
 */
public class BpbAPI {

    private static BpbAPI instance;

    private JedisPool jedisPool;
    private String redisPrefix = "bpb";

    public BpbAPI(String host, int port, String password, String prefix) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(config, host, port, 5000, password);
        } else {
            this.jedisPool = new JedisPool(config, host, port, 5000);
        }
        if (prefix != null && !prefix.isEmpty()) {
            this.redisPrefix = prefix;
        }
    }

    public static BpbAPI getInstance() {
        return instance;
    }

    public static void setInstance(BpbAPI instance) {
        BpbAPI.instance = instance;
    }

    /**
     * Get a placeholder value for a player from Redis.
     *
     * @param player      The player
     * @param placeholder The placeholder name WITHOUT % and WITHOUT bpb_ prefix (e.g., "player_name")
     * @return The placeholder value, or null if not found
     */
    public String getValue(ProxiedPlayer player, String placeholder) {
        return getValue(player.getUniqueId(), placeholder);
    }

    /**
     * Get a placeholder value for a player UUID from Redis.
     *
     * @param playerUUID  The player's UUID
     * @param placeholder The placeholder name WITHOUT % and WITHOUT bpb_ prefix (e.g., "player_name")
     * @return The placeholder value, or null if not found
     */
    public String getValue(UUID playerUUID, String placeholder) {
        String key = buildKey(playerUUID, placeholder);
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a placeholder value for a player UUID from Redis (string UUID).
     */
    public String getValue(String playerUUID, String placeholder) {
        return getValue(UUID.fromString(playerUUID), placeholder);
    }

    /**
     * Set a placeholder value in Redis.
     */
    public void setValue(UUID playerUUID, String placeholder, String value, int expireSeconds) {
        String key = buildKey(playerUUID, placeholder);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        } catch (Exception e) {
            // Silently ignore Redis errors
        }
    }

    /**
     * Delete all placeholder values for a player.
     */
    public void deletePlayerValues(UUID playerUUID) {
        String pattern = redisPrefix + ":player:" + playerUUID.toString() + ":*";
        try (Jedis jedis = jedisPool.getResource()) {
            for (String key : jedis.keys(pattern)) {
                jedis.del(key);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Build the Redis key for a player+placeholder.
     * Format: <prefix>:player:<uuid>:<placeholder>
     * e.g., bpb:player:550e8400-e29b-41d4-a716-446655440000:player_name
     */
    public String buildKey(UUID playerUUID, String placeholder) {
        return redisPrefix + ":player:" + playerUUID.toString() + ":" + placeholder;
    }

    /**
     * Parse a %bpb_xxx% style placeholder and return the value.
     * Handles the full placeholder with % prefix/suffix.
     *
     * @param player         The player
     * @param fullPlaceholder The full placeholder like "%bpb_player_name%"
     * @return The resolved value, or the original placeholder if not found
     */
    public String resolveBpbPlaceholder(ProxiedPlayer player, String fullPlaceholder) {
        if (fullPlaceholder == null || !fullPlaceholder.startsWith("%bpb_") || !fullPlaceholder.endsWith("%")) {
            return fullPlaceholder;
        }
        String inner = fullPlaceholder.substring(5, fullPlaceholder.length() - 1); // strip %bpb_ and %
        String value = getValue(player, inner);
        return value != null ? value : fullPlaceholder;
    }

    /**
     * Replace all %bpb_xxx% placeholders in a text.
     */
    public String replaceBpbPlaceholders(ProxiedPlayer player, String text) {
        if (text == null || !text.contains("%bpb_")) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf("%bpb_", i);
            if (start == -1) {
                result.append(text.substring(i));
                break;
            }
            result.append(text.substring(i, start));
            int end = text.indexOf('%', start + 5);
            if (end == -1) {
                result.append(text.substring(start));
                break;
            }
            String fullPlaceholder = text.substring(start, end + 1);
            String inner = text.substring(start + 5, end);
            String value = getValue(player, inner);
            result.append(value != null ? value : fullPlaceholder);
            i = end + 1;
        }
        return result.toString();
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
