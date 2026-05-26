# BungeePlaceholderBridge

A bridge plugin that allows **BungeeCord plugins** to use **PlaceholderAPI** placeholders from backend Spigot/Paper servers.

Inspired by [PAPIProxyBridge](https://github.com/WiIIiam278/PAPIProxyBridge), this plugin provides a simple and efficient way for proxy-side plugins to resolve placeholders that only exist on backend servers.

---

## How It Works

```
+----------------------------------+         Plugin Messaging          +-------------------------------+
|         BungeeCord Proxy         |        Channel (bpb:*)           |      Spigot/Paper Backend     |
|                                  | <------------------------------> |                               |
|  +---------------------------+   |                                   |  +-------------------------+  |
|  |  BungeePlaceholderBridge  |   |   1. Request: %player_name%      |  |  BungeePlaceholderBridge |  |
|  |  (Provides API)           |   | --------------------------------> |  |  (Bukkit side)          |  |
|  |                           |   |                                   |  |                         |  |
|  |  PlaceholderBridge.       |   |   2. Calls PlaceholderAPI        |  |  PlaceholderAPI.set     |  |
|  |    format("%player_       |   |      .setPlaceholders()          |  |    Placeholders()       |  |
|  |     name%", player)       |   |                                   |  |                         |  |
|  |       |                   |   |   3. Response: "Steve"           |  |                         |  |
|  |       v                   |   | <-------------------------------- |  |                         |  |
|  |  CompletableFuture        |   |                                   |  |                         |  |
|  |    .thenAccept(result)    |   |                                   |  |                         |  |
|  +---------------------------+   |                                   |  +-------------------------+  |
|                                  |                                   |                               |
|  +---------------------------+   |                                   |  Requires: PlaceholderAPI    |
|  | Your Bungee Plugin        |   |                                   |       (clip)                 |
|  | Uses the API              |   |                                   |                               |
|  +---------------------------+   |                                   |                               |
+----------------------------------+                                   +-------------------------------+
```

---

## Installation

### For Server Owners

1. **BungeeCord Proxy**: Put `BungeePlaceholderBridge-Bungee-1.0.0.jar` into your `plugins/` folder
2. **Each Spigot Backend Server**: 
   - Put `BungeePlaceholderBridge-Bukkit-1.0.0.jar` into your `plugins/` folder
   - Ensure [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is also installed
3. Restart all servers

### For Developers (Using the API)

Add the API dependency to your BungeeCord plugin:

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.13502544286</groupId>
        <artifactId>BungeePlaceholderBridge</artifactId>
        <version>v2.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Gradle:**
```groovy
dependencies {
    compileOnly 'com.github.13502544286.BungeePlaceholderBridge:BungeePlaceholderBridge:2.0.0'
}
```

### API Usage Example

```java
import com.mcplugin.bpb.api.PlaceholderBridge;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MyPlugin extends Plugin {
    
    public void onPlayerJoin(ProxiedPlayer player) {
        PlaceholderBridge bridge = PlaceholderBridge.getInstance();
        
        // Format a string with placeholders
        bridge.format("Welcome %player_name%! Your rank is %vault_rank%.", player)
            .thenAccept(formatted -> {
                player.sendMessage(new TextComponent(formatted));
            });
        
        // Format by UUID (async-safe)
        bridge.format("Hello %player_name%!", player.getUniqueId())
            .thenAccept(formatted -> {
                // Do something with the formatted text
            });
            
        // Check if text has placeholders before formatting
        if (PlaceholderBridge.hasPlaceholders(message)) {
            bridge.format(message, player).thenAccept(formatted -> {
                // Use formatted message
            });
        }
    }
}
```

**Important:** The `format()` method returns a `CompletableFuture<String>`. Never call `.join()` on it from the main thread - this will deadlock the proxy!

---

## Commands

### `/bpb` (BungeeCord only)

| Command | Description | Permission |
|---------|-------------|------------|
| `/bpb status` | Show plugin status (pending requests, cache size) | `bpb.admin` |
| `/bpb cache` | Show cache information | `bpb.admin` |
| `/bpb cache clear` | Clear the placeholder cache | `bpb.admin` |
| `/bpb test <text>` | Test placeholder resolution | `bpb.admin` |
| `/bpb help` | Show help message | `bpb.admin` |

---

## Configuration

Currently, the plugin works out of the box with sensible defaults:

- **Cache expiry**: 30 seconds (placeholder results are cached per-player to reduce network traffic)
- **Request timeout**: 5 seconds (requests that don't receive a response in time will return the original text)

These settings may be made configurable in future versions.

---

## Features

- **Async by design** - Uses `CompletableFuture` to never block the proxy thread
- **Smart caching** - Results are cached per-player to minimize Plugin Messaging channel traffic
- **Timeout handling** - Requests that time out gracefully return the original unformatted text
- **Zero-config** - Works immediately after installation, no configuration files needed
- **Lightweight** - Minimal overhead, only processes messages that actually contain placeholders
- **PlaceholderAPI compatible** - Works with all PlaceholderAPI expansions installed on backend servers

---

## Protocol Details

The plugin uses two Plugin Messaging channels:

| Channel | Direction | Purpose |
|---------|-----------|---------|
| `bpb:request` | Bungee -> Spigot | Send placeholder resolution requests |
| `bpb:response` | Spigot -> Bungee | Return resolved placeholder results |

**Request packet format:**
```
[int]     protocol version (1)
[long]    request ID MSB
[long]    request ID LSB  
[long]    player UUID MSB
[long]    player UUID LSB
[String]  text with placeholders
```

**Response packet format:**
```
[int]     protocol version (1)
[long]    request ID MSB
[long]    request ID LSB
[String]  resolved text
```

---

## Building from Source

```bash
git clone <repo>
cd BungeePlaceholderBridge
mvn clean install
```

Output JARs:
- `bpb-api/target/bpb-api-1.0.0.jar` - API library (for developers)
- `bpb-bukkit/target/BungeePlaceholderBridge-Bukkit-1.0.0.jar` - Spigot/Paper plugin
- `bpb-bungee/target/BungeePlaceholderBridge-Bungee-1.0.0.jar` - BungeeCord plugin

---

# API Reference

## `BpbAPI.getInstance()`

Returns the singleton BpbAPI instance (set during BPB-Bungee startup).

---

## `replaceBpbPlaceholders(ProxiedPlayer player, String text)`

Batch-replaces all `%bpb_xxx%` placeholders in a string.

| Parameter | Type | Description |
|-----------|------|-------------|
| `player` | `ProxiedPlayer` | Player to read placeholder values for |
| `text` | `String` | Text containing `%bpb_xxx%` |

**Returns:** `String` — Text with placeholders replaced. Unresolved placeholders remain as-is.

```java
String text = "Welcome %bpb_player_name%, Rank: %bpb_vault_rank%!";
String result = BpbAPI.getInstance().replaceBpbPlaceholders(player, text);
// "Welcome Steve, Rank: VIP!"
```

---

## `getValue(ProxiedPlayer player, String placeholder)`

Get a single placeholder value for a player.

| Parameter | Type | Description |
|-----------|------|-------------|
| `player` | `ProxiedPlayer` | The player |
| `placeholder` | `String` | Placeholder name without `%` or `bpb_` prefix |

**Returns:** `String` — The value, or `null` if not found.

```java
String rank = BpbAPI.getInstance().getValue(player, "vault_rank");
// "VIP"
```

---

## `getValue(UUID playerUUID, String placeholder)`

Get a placeholder value by UUID (useful in async contexts).

```java
String name = BpbAPI.getInstance().getValue(player.getUniqueId(), "player_name");
```

---

## `getValue(String playerUUID, String placeholder)`

Get a placeholder value by string UUID.

```java
String health = BpbAPI.getInstance().getValue(player.getUniqueId().toString(), "player_health");
```

---

## `resolveBpbPlaceholder(ProxiedPlayer player, String fullPlaceholder)`

Resolve a single full `%bpb_xxx%` placeholder.

| Parameter | Type | Description |
|-----------|------|-------------|
| `player` | `ProxiedPlayer` | The player |
| `fullPlaceholder` | `String` | Full placeholder like `"%bpb_player_name%"` |

**Returns:** `String` — Resolved value, or the original placeholder if not found.

```java
String value = BpbAPI.getInstance().resolveBpbPlaceholder(player, "%bpb_player_name%");
// "Steve"
```

---

## `setValue(UUID playerUUID, String placeholder, String value, int expireSeconds)`

Manually write a placeholder value to Redis (for plugin extensions).

```java
BpbAPI.getInstance().setValue(player.getUniqueId(), "custom_data", "hello", 60);
```

---

## `deletePlayerValues(UUID playerUUID)`

Delete all placeholder data for a player from Redis (called on disconnect).

---

## Full Example

```java
import com.mcplugin.bpb.api.BpbAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class MyPlugin extends Plugin {

    @Override
    public void onEnable() {
        if (getProxy().getPluginManager().getPlugin("BungeePlaceholderBridge") == null) {
            getLogger().warning("BPB not found!");
            return;
        }
    }

    public void sendWelcome(ProxiedPlayer player) {
        String template = "&7[&aWelcome&7] &b%bpb_player_name% &7| Rank: &e%bpb_vault_rank%";
        String msg = BpbAPI.getInstance().replaceBpbPlaceholders(player, template);
        player.sendMessage(msg);
        // [Welcome] Steve | Rank: VIP
    }

    public void showStats(ProxiedPlayer player) {
        String health = BpbAPI.getInstance().getValue(player, "player_health");
        String max = BpbAPI.getInstance().getValue(player, "player_max_health");
        player.sendMessage("Health: " + health + "/" + max);
        // Health: 20/20
    }
}
```

## Tab List Example

```java
String header = "&7Online: &b%bpb_server_online%&7/&b%bpb_server_online_max%";
String formatted = BpbAPI.getInstance().replaceBpbPlaceholders(player, header);
// Online: 42/100
```

## License

MIT License
