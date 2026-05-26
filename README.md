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
<dependency>
    <groupId>com.mcplugin</groupId>
    <artifactId>bpb-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

**Gradle:**
```groovy
dependencies {
    compileOnly 'com.mcplugin:bpb-api:1.0.0'
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

## License

MIT License
