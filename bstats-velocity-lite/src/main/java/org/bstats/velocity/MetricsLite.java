package org.bstats.velocity;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import com.sun.javafx.font.Metrics;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

/**
 * bStats collects some data for plugin authors.
 * <p>
 * Check out https://bStats.org/ to learn more about bStats!
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MetricsLite {

    static {
        // You can use the property to disable the check in your test environment
        if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
            // Maven's Relocate is clever and changes strings, too. So we have to use this little "trick" ... :D
            String defaultPackage = new String(
                    new byte[] {'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's', '.', 'v', 'e', 'l', 'o', 'c', 'i', 't', 'y'});
            String examplePackage = new String(new byte[] {'y', 'o', 'u', 'r', '.', 'p', 'a', 'c', 'k', 'a', 'g', 'e'});
            // We want to make sure nobody just copy & pastes the example and use the wrong package names
            if (Metrics.class.getPackage().getName().equals(defaultPackage) || Metrics.class.getPackage().getName().equals(examplePackage)) {
                throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
            }
        }
    }

    // The version of this bStats class
    public static final int B_STATS_VERSION = 1;

    // The url to which the data is sent
    private static final String URL = "https://bStats.org/submitData/velocity";

    // The plugin's data
    private final PluginData pluginData;

    // Is bStats enabled on this server?
    private boolean enabled;

    // The uuid of the server
    private String serverUUID;

    // Should failed requests be logged?
    private boolean logFailedRequests = false;

    // Should the sent data be logged?
    private static boolean logSentData;

    // Should the response text be logged?
    private static boolean logResponseStatusText;

    // A list with all known metrics class objects including this one
    private static final List<Object> knownMetricsInstances = new ArrayList<>();

    public MetricsLite(PluginData pluginData) {
        this.pluginData = pluginData;

        try {
            loadConfig();
        } catch (IOException e) {
            // Failed to load configuration
            pluginData.getLogger().warn("Failed to load bStats config!", e);
            return;
        }

        // We are not allowed to send data about this server :(
        if (!enabled) {
            return;
        }

        Class<?> usedMetricsClass = getFirstBStatsClass();
        if (usedMetricsClass == null) {
            // Failed to get first metrics class
            return;
        }
        if (usedMetricsClass == getClass()) {
            // We are the first! :)
            linkMetrics(this);
            startSubmitting();
        } else {
            // We aren't the first so we link to the first metrics class
            try {
                usedMetricsClass.getMethod("linkMetrics", Object.class).invoke(null, this);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                if (logFailedRequests) {
                    pluginData.getLogger().warn("Failed to link to first metrics class " + usedMetricsClass.getName() + "!", e);
                }
            }
        }
    }

    /**
     * Checks if bStats is enabled.
     *
     * @return Whether bStats is enabled or not.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Links an other metrics class with this class.
     * This method is called using Reflection.
     *
     * @param metrics An object of the metrics class to link.
     */
    public static void linkMetrics(Object metrics) {
        knownMetricsInstances.add(metrics);
    }

    /**
     * Gets the plugin specific data.
     * This method is called using Reflection.
     *
     * @return The plugin specific data.
     */
    public JsonObject getPluginData() {
        return pluginData.getSent();
    }

    private void startSubmitting() {
        // The data collection is async, as well as sending the data
        // Velocity does not have a main thread, everything is async
        pluginData.getServer().getScheduler().buildTask(pluginData.getPluginInstance(), this::submitData)
                .delay(2, TimeUnit.MINUTES).repeat(30, TimeUnit.MILLISECONDS).schedule();
        // Submit the data every 30 minutes, first time after 2 minutes to give other plugins enough time to start
        // WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
        // WARNING: Just don't do it!
    }

    /**
     * Gets the server specific data.
     *
     * @return The server specific data.
     */
    private JsonObject getServerData() {
        // Minecraft specific data
        int playerAmount = pluginData.getServer().getPlayerCount();
        playerAmount = playerAmount > 500 ? 500 : playerAmount;
        int onlineMode = pluginData.getServer().getConfiguration().isOnlineMode() ? 1 : 0;
        String velocityVersion = pluginData.getServer().getVersion().getVersion();
        int managedServers = pluginData.getServer().getAllServers().size();

        // OS/Java specific data
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        JsonObject data = new JsonObject();

        data.addProperty("serverUUID", serverUUID);

        data.addProperty("playerAmount", playerAmount);
        data.addProperty("managedServers", managedServers);
        data.addProperty("onlineMode", onlineMode);
        data.addProperty("velocityVersion", velocityVersion);

        data.addProperty("javaVersion", javaVersion);
        data.addProperty("osName", osName);
        data.addProperty("osArch", osArch);
        data.addProperty("osVersion", osVersion);
        data.addProperty("coreCount", coreCount);

        return data;
    }

    /**
     * Collects the data and sends it afterwards.
     */
    private void submitData() {
        JsonObject data = getServerData();

        JsonArray pluginData = new JsonArray();
        // Search for all other bStats Metrics classes to get their plugin data
        for (Object metrics : knownMetricsInstances) {
            try {
                Object plugin = metrics.getClass().getMethod("getPluginData").invoke(metrics);
                if (plugin instanceof JsonObject) {
                    pluginData.add((JsonObject) plugin);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        data.add("plugins", pluginData);

        try {
            // Send the data
            sendData(this.pluginData, data);
        } catch (Exception e) {
            // Something went wrong! :(
            if (logFailedRequests) {
                this.pluginData.getLogger().warn("Could not submit plugin stats!", e);
            }
        }
    }

    private void loadConfig() throws IOException {
        Path configPath = pluginData.getDataFolder().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File configFile = new File(configPath.toFile(), "config.toml");
        if (!configFile.exists()) {
            writeFile(configFile,
                    "#bStats collects some data for plugin authors like how many servers are using their plugins.",
                    "#To honor their work, you should not disable it.",
                    "#This has nearly no effect on the server performance!",
                    "#Check out https://bStats.org/ to learn more :)",
                    "enabled = true",
                    "serverUuid = \"" + UUID.randomUUID().toString() + "\"",
                    "logFailedRequests = false",
                    "logSentData = false",
                    "logResponseStatusText = false");
        }

        Toml configuration = new Toml().read(configFile);

        // Load configuration
        enabled = configuration.getBoolean("enabled", true);
        serverUUID = configuration.getString("serverUuid");
        logFailedRequests = configuration.getBoolean("logFailedRequests", false);
        logSentData = configuration.getBoolean("logSentData", false);
        logResponseStatusText = configuration.getBoolean("logResponseStatusText", false);
    }


    /**
     * Gets the first bStat Metrics class.
     *
     * @return The first bStats metrics class.
     */
    private Class<?> getFirstBStatsClass() {
        Path configPath = pluginData.getDataFolder().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File tempFile = new File(configPath.toFile(), "temp.txt");

        try {
            String className = readFile(tempFile);
            if (className != null) {
                try {
                    // Let's check if a class with the given name exists.
                    return Class.forName(className);
                } catch (ClassNotFoundException ignored) {
                }
            }
            writeFile(tempFile, getClass().getName());
            return getClass();
        } catch (IOException e) {
            if (logFailedRequests) {
                pluginData.getLogger().warn("Failed to get first bStats class!", e);
            }
            return null;
        }
    }

    /**
     * Reads the first line of the file.
     *
     * @param file The file to read. Cannot be null.
     * @return The first line of the file or <code>null</code> if the file does not exist or is empty.
     * @throws IOException If something did not work :(
     */
    private String readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            return bufferedReader.readLine();
        }
    }

    /**
     * Writes a String to a file. It also adds a note for the user,
     *
     * @param file  The file to write to. Cannot be null.
     * @param lines The lines to write.
     * @throws IOException If something did not work :(
     */
    private void writeFile(File file, String... lines) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)
        ) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    /**
     * Sends the data to the bStats server.
     *
     * @param plugin Any plugin. It's just used to get a logger instance.
     * @param data   The data to send.
     * @throws Exception If the request failed.
     */
    private void sendData(PluginData plugin, JsonObject data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (logSentData) {
            plugin.getLogger().info("Sending data to bStats: " + data.toString());
        }

        HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();

        // Compress the data to save bandwidth
        byte[] compressedData = compress(data.toString());

        // Add headers
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
        connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

        // Send data
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);

        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line);
        }
        bufferedReader.close();
        if (logResponseStatusText) {
            plugin.getLogger().info("Sent data to bStats and received response: " + builder.toString());
        }
    }

    /**
     * Gzips the given String.
     *
     * @param str The string to gzip.
     * @return The gzipped String.
     * @throws IOException If the compression failed.
     */
    private static byte[] compress(String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
        gzip.write(str.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        return outputStream.toByteArray();
    }

    /**
     * Represents a class for taking data about a plugin
     */
    public static class PluginData {

        private final PluginContainer plugin;
        private final ProxyServer server;
        private final Logger logger;
        private final Path dataFolder;
        private final Object pluginInstance;

        /**
         * Creates a plugin data
         *
         * @param id     id, as the bStats page of the plugin
         * @param server server instance
         * @param logger plugin logger
         */
        public PluginData(Object pluginInstance, String id, ProxyServer server, Logger logger) {
            plugin = server.getPluginManager().getPlugin(id).get();
            this.server = server;
            this.logger = logger;
            dataFolder = plugin.getDescription().getSource().get();
            this.pluginInstance = pluginInstance;
        }

        public JsonObject getSent() {
            PluginDescription description = plugin.getDescription();
            JsonObject data = new JsonObject();
            data.addProperty("pluginName", description.getId());
            if (description.getVersion().isPresent()) {
                data.addProperty("pluginVersion", description.getVersion().get());
            } else {
                data.addProperty("pluginVersion", "unknown");
            }
            data.add("customCharts", new JsonArray());
            return data;
        }

        public ProxyServer getServer() {
            return server;
        }

        public Logger getLogger() {
            return logger;
        }

        public Object getPluginInstance() {
            return pluginInstance;
        }

        public Path getDataFolder() {
            return dataFolder;
        }
    }
}
