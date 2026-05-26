#!/bin/bash
# Build script for BungeePlaceholderBridge
# Requires: Maven 3.6+, Java 8+

echo "=========================================="
echo "Building BungeePlaceholderBridge"
echo "=========================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven 3.6 or higher."
    echo "Visit: https://maven.apache.org/install.html"
    exit 1
fi

# Check Java version
java -version

echo ""
echo "Starting build..."
mvn clean install

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Build successful!"
    echo "=========================================="
    echo ""
    echo "Output files:"
    echo "  API (for developers):  bpb-api/target/bpb-api-*.jar"
    echo "  Bukkit plugin:         bpb-bukkit/target/BungeePlaceholderBridge-Bukkit-*.jar"
    echo "  Bungee plugin:         bpb-bungee/target/BungeePlaceholderBridge-Bungee-*.jar"
    echo ""
    echo "Installation:"
    echo "  1. Put BungeePlaceholderBridge-Bungee-*.jar in your BungeeCord plugins/ folder"
    echo "  2. Put BungeePlaceholderBridge-Bukkit-*.jar in each Spigot backend's plugins/ folder"
    echo "  3. Ensure PlaceholderAPI is installed on each backend server"
    echo "  4. Restart all servers"
else
    echo "Build failed! Check the error messages above."
    exit 1
fi
