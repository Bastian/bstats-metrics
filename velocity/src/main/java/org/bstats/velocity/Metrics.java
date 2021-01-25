package org.bstats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.json.JsonObjectBuilder;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Metrics {

    /**
     * A factory to create new Metrics classes.
     */
    public static class Factory {

        private final ProxyServer server;
        private final Logger logger;
        private final Path dataDirectory;

        // The constructor is not meant to be called by the user.
        // The instance is created using Dependency Injection
        @Inject
        private Factory(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
            this.server = server;
            this.logger = logger;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Creates a new Metrics class.
         *
         * @param serviceId The id of the service.
         *                  It can be found at <a href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
         *                  <p>Not to be confused with Velocity's {@link PluginDescription#getId()} method!
         * @return A Metrics instance that can be used to register custom charts.
         * <p>The return value can be ignored, when you do not want to register custom charts.
         */
        public Metrics make(Object plugin, int serviceId) {
            return new Metrics(plugin, server, logger, dataDirectory, serviceId);
        }
    }

    private final PluginContainer pluginContainer;
    private final ProxyServer server;
    private final MetricsBase metricsBase;
    private final Logger logger;
    private final Path dataDirectory;

    private String serverUUID;
    private boolean enabled;
    private boolean logErrors;
    private boolean logSentData;
    private boolean logResponseStatusText;

    private Metrics(Object plugin, ProxyServer server, Logger logger, Path dataDirectory, int serviceId) {
        pluginContainer = server.getPluginManager().fromInstance(plugin)
                .orElseThrow(() -> new IllegalArgumentException("The provided instance is not a plugin"));
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        try {
            setupConfig(true);
        } catch (IOException e) {
            logger.error("Failed to create bStats config", e);
        }

        metricsBase = new MetricsBase(
                "velocity",
                serverUUID,
                serviceId,
                enabled,
                this::appendPlatformData,
                this::appendServiceData,
                task -> server.getScheduler().buildTask(plugin, task).schedule(),
                () -> true,
                logger::warn,
                logger::info,
                logErrors,
                logSentData,
                logResponseStatusText
        );
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        metricsBase.addCustomChart(chart);
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", server.getPlayerCount());
        builder.appendField("managedServers", server.getAllServers().size());
        builder.appendField("onlineMode", server.getConfiguration().isOnlineMode() ? 1 : 0);
        builder.appendField("velocityVersionVersion", server.getVersion().getVersion());
        builder.appendField("velocityVersionName", server.getVersion().getName());
        builder.appendField("velocityVersionVendor", server.getVersion().getVendor());

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", pluginContainer.getDescription().getVersion().orElse("unknown"));
    }

    /**
     * Setups the bStats configuration.
     *
     * @param recreateWhenMalformed Whether the method should recreate the config file when it's malformed.
     */
    public void setupConfig(boolean recreateWhenMalformed) throws IOException {
        File configFolder = dataDirectory.getParent().resolve("bStats").toFile();
        configFolder.mkdirs();
        File configFile = new File(configFolder, "config.txt");
        if (!configFile.exists()) {
            writeConfig(configFile);
        }

        List<String> lines = readFile(configFile);
        if (lines == null) {
            throw new AssertionError("Content of newly created file is null");
        }

        enabled = getConfigValue("enabled", lines).map("true"::equals).orElse(true);
        serverUUID = getConfigValue("server-uuid", lines).orElse(null);
        logErrors = getConfigValue("log-errors", lines).map("true"::equals).orElse(false);
        logSentData =  getConfigValue("log-sent-data", lines).map("true"::equals).orElse(false);
        logResponseStatusText =  getConfigValue("log-response-status-text", lines).map("true"::equals).orElse(false);

        if (serverUUID == null) {
            if (recreateWhenMalformed) {
                logger.info("Found malformed bStats config file. Re-creating it...");
                configFile.delete();
                setupConfig(false);
            } else {
                throw new AssertionError("Failed to re-create malformed bStats config file");
            }
        }
    }

    /**
     * Creates a simple bStats configuration.
     *
     * @param file The config file.
     */
    private void writeConfig(File file) throws IOException {
        List<String> configContent = new ArrayList<>();
        configContent.add("# bStats collects some basic information for plugin authors, like how many people use");
        configContent.add("# their plugin and their total player count. It's recommend to keep bStats enabled, but");
        configContent.add("# if you're not comfortable with this, you can turn this setting off. There is no");
        configContent.add("# performance penalty associated with having metrics enabled, and data sent to bStats");
        configContent.add("# can't identify your server.");
        configContent.add("enabled=true");
        configContent.add("server-uuid=" + UUID.randomUUID().toString());
        configContent.add("log-errors=false");
        configContent.add("log-sent-data=false");
        configContent.add("log-response-status-text=false");
        writeFile(file, configContent);
    }

    /**
     * Gets a config setting from the given list of lines of the file.
     *
     * @param key The key for the setting.
     * @param lines The lines of the file.
     * @return The value of the setting.
     */
    private Optional<String> getConfigValue(String key, List<String> lines) {
        return lines.stream()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.replaceFirst(Pattern.quote(key + "="), ""))
                .findFirst();
    }

    /**
     * Reads the text content of the given file.
     *
     * @param file The file to read.
     * @return The lines of the given file.
     */
    private List<String> readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            return bufferedReader.lines().collect(Collectors.toList());
        }
    }

    /**
     * Writes the given lines to the given file.
     *
     * @param file The file to write to.
     * @param lines The lines to write.
     */
    private void writeFile(File file, List<String> lines) throws IOException {
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

}