package pom.xml;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;


public class Main extends JavaPlugin implements Listener {

    private Set<String> allowedCountries;
    private Set<Long> excludedIPs = new HashSet<>();
    private final List<IPRange> ipRanges = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int config = 2; // Increment this if you change the config structure

        // Ensure ip2location folder exists inside plugin data folder
        File ipFolder = new File(getDataFolder(), "ip2location");
        if (!ipFolder.exists()) ipFolder.mkdirs();

        // Extract all required files if missing, with .txt and .CSV as specified
        saveResourceToFolder("ip2location/attribution.txt", ipFolder);
        saveResourceToFolder("ip2location/LICENSE-CC-BY-SA-4.0.txt", ipFolder);
        saveResourceToFolder("ip2location/README_LITE.txt", ipFolder);
        saveResourceToFolder("ip2location/IP2LOCATION-LITE-DB1.CSV", ipFolder);
        
        // Check if the plugin is enabled in the config
        if (getConfig().getBoolean("disable", false)) {
            getLogger().info("IPGuard was disabled in config. Shutting down silently :(");
        return;
    }
        
        // Load config data and IP ranges
        loadAllowedCountries();
        loadExcludedIPs();
        loadIPRanges(ipFolder);
        
        // Register the event listener
        Bukkit.getPluginManager().registerEvents(this, this);
        // Check if the config version is up to date
        int configVersion = getConfig().getInt("config-version", 0);
        if (configVersion < config) {
            getLogger().warning("Outdated config detected. Please update or delete config.yml to regenerate.");
        }
        getLogger().info("IPBanPlugin enabled");
    }

    @Override
    public void onDisable() {
    }
    

    private void loadAllowedCountries() {
        allowedCountries = new HashSet<>(getConfig().getStringList("allowed-countries"));
        if (allowedCountries.isEmpty()) {
            getLogger().warning("No allowed countries configured, defaulting to empty set.");
        } else {
            String logLevel = getConfig().getString("log-level", "INFO");
            if (logLevel.equals("INFO")) {
                getLogger().info("Allowed countries loaded: " + allowedCountries);
            }
        }
    }
    


    private void loadExcludedIPs() {
        List<String> excludedIpStrings = getConfig().getStringList("excluded-ips");
        for (String ip : excludedIpStrings) {
            long ipNum = ipToLong(ip);
            if (ipNum != -1) {
                excludedIPs.add(ipNum);
            } else {
                getLogger().warning("Invalid IP in excluded-ips: " + ip);
            }
        }
        String logLevel = getConfig().getString("log-level", "INFO");
        if (logLevel.equals("INFO")) {
            getLogger().info("Excluded IPs loaded: " + excludedIPs.size());
        }    
    }

    private void loadIPRanges(File ipFolder) {
        File ipFile = new File(ipFolder, "IP2LOCATION-LITE-DB1.CSV");
        if (!ipFile.exists()) {
            getLogger().severe("IP ranges file not found at " + ipFile.getAbsolutePath());
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(ipFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                long start = Long.parseLong(parts[0].replace("\"", "").trim());
                long end = Long.parseLong(parts[1].replace("\"", "").trim());
                String countryCode = parts[2].replace("\"", "").trim();

                ipRanges.add(new IPRange(start, end, countryCode));
            }
            String logLevel = getConfig().getString("log-level", "INFO");
            if (logLevel.equals("INFO")) {
                getLogger().info("Loaded IP ranges: " + ipRanges.size());
            }  
        } catch (IOException e) {
            getLogger().severe("Failed to load IP ranges: " + e.getMessage());
        }
    }

    //private long ipToLong(String ip) {
    //    String[] parts = ip.split("\\.");
    //    if (parts.length != 4) return -1;
    //    long ipNum = 0;
    //    for (int i = 0; i < 4; i++) {
    //        try {
    //            long part = Long.parseLong(parts[i]);
    //            ipNum += part * Math.pow(256, 3 - i);
    //        } catch (NumberFormatException e) {
    //            return -1;
    //        }
    //    }
    //    return ipNum;
    //}
    
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return -1;

        long ipNum = 0;
        for (int i = 0; i < 4; i++) {
            try {
                int part = Integer.parseInt(parts[i]);
                if (part < 0 || part > 255) return -1;
                ipNum = (ipNum << 8) | part;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return ipNum;
    }

    private boolean isAllowedCountry(long ipNum) {
        for (IPRange range : ipRanges) {
            if (range.contains(ipNum)) {
                return allowedCountries.contains(range.countryCode);
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        long ipNum = ipToLong(ip);

        if (excludedIPs.contains(ipNum)) {
            // IP is excluded from checks, allow immediately
            return;
        }

        if (ipNum == -1 || !isAllowedCountry(ipNum)) {

            //Kick-message: "Access denied. Your IP address is not allowed to access this service."
            String kickmessage = getConfig().getString("Kick-message", "Access denied. Your IP address is not allowed to access this service.");
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.kickMessage(net.kyori.adventure.text.Component.text(kickmessage));
            String logLevel = getConfig().getString("log-level", "INFO");
            if (logLevel.equals("INFO")) {
                getLogger().info("Kicked player " + event.getName() + " for disallowed IP: " + ip);
            }
        }
    }

    private void saveResourceToFolder(String resourcePath, File folder) {
        File outFile = new File(folder, new File(resourcePath).getName()); // just filename
        if (!outFile.exists()) {
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    getLogger().severe("Resource " + resourcePath + " not found in jar");
                    return;
                }
                Files.copy(in, outFile.toPath());
                getLogger().info("Extracted " + resourcePath + " to " + outFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().severe("Failed to extract " + resourcePath + ": " + e.getMessage());
            }
        }
    }

    private static class IPRange {
        long start, end;
        String countryCode;

        IPRange(long start, long end, String countryCode) {
            this.start = start;
            this.end = end;
            this.countryCode = countryCode;
        }

        boolean contains(long ip) {
            return ip >= start && ip <= end;
        }
    }
}
