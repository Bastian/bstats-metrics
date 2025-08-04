package org.bstats.sponge;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.json.JsonObjectBuilder;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class Metrics {

    /**
     * A factory to create new Metrics classes.
     */
    public static class Factory {

        private final PluginContainer plugin;
        private final Logger logger;
        private final Path configDir;

        // The constructor is not meant to be called by the user.
        // The instance is created using Dependency Injection (https://docs.spongepowered.org/master/en/plugin/injection.html)
        @Inject
        private Factory(PluginContainer plugin, Logger logger, @ConfigDir(sharedRoot = true) Path configDir) {
            this.plugin = plugin;
            this.logger = logger;
            this.configDir = configDir;
        }

        /**
         * Creates a new Metrics class.
         *
         * @param serviceId The id of the service.
         *                  It can be found at <a href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
         *                  <p>Not to be confused with Sponge's {@link org.spongepowered.plugin.metadata.PluginMetadata#id()} method!
         * @return A Metrics instance that can be used to register custom charts.
         * <p>The return value can be ignored, when you do not want to register custom charts.
         */
        public Metrics make(int serviceId) {
            return new Metrics(plugin, logger, configDir, serviceId);
        }
    }

    private final PluginContainer plugin;
    private final Logger logger;
    private final Path configDir;
    private final int serviceId;

    private MetricsBase metricsBase;

    private String serverUUID;
    private boolean logErrors = false;
    private boolean logSentData;
    private boolean logResponseStatusText;

    private Metrics(PluginContainer plugin, Logger logger, Path configDir, int serviceId) {
        this.plugin = plugin;
        this.logger = logger;
        this.configDir = configDir;
        this.serviceId = serviceId;

        Sponge.eventManager().registerListeners(plugin, this);
    }

    @Listener
    public void startup(ConstructPluginEvent event) {
        try {
            loadConfig();
        } catch (IOException e) {
            // Failed to load configuration
            logger.warn("Failed to load bStats config!", e);
            return;
        }

        metricsBase = new MetricsBase(
                "sponge",
                serverUUID,
                serviceId,
                Sponge.metricsConfigManager().effectiveCollectionState(plugin).asBoolean(),
                this::appendPlatformData,
                this::appendServiceData,
                task -> {
                    Scheduler scheduler = Sponge.asyncScheduler();
                    Task.Builder taskBuilder = Task.builder();
                    scheduler.submit(taskBuilder.execute(task).plugin(plugin).build());
                },
                () -> true,
                logger::warn,
                logger::info,
                logErrors,
                logSentData,
                logResponseStatusText,
                false
        );

        StringBuilder builder = new StringBuilder().append(System.lineSeparator());
        builder.append("Plugin ").append(plugin.metadata().name().orElse(plugin.metadata().id())).append(" is using bStats Metrics ");
        if (Sponge.metricsConfigManager().effectiveCollectionState(plugin).asBoolean()) {
            builder.append(" and is allowed to send data.");
        } else {
            builder.append(" but currently has data sending disabled.").append(System.lineSeparator());
            builder.append("To change the enabled/disabled state of any bStats use in a plugin, visit the Sponge config!");
        }
        logger.info(builder.toString());
    }

    /**
     * Loads the bStats configuration.
     */
    private void loadConfig() throws IOException {
        File configPath = configDir.resolve("bStats").toFile();
        configPath.mkdirs();
        File configFile = new File(configPath, "config.conf");
        HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().file(configFile).build();
        CommentedConfigurationNode node;

        String serverUuidComment =
            "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n" +
            "many people use their plugin and their total player count. It's recommended to keep bStats\n" +
            "enabled, but if you're not comfortable with this, you can disable data collection in the\n" +
            "Sponge configuration file. There is no performance penalty associated with having metrics\n" +
            "enabled, and data sent to bStats is fully anonymous.";

        if (!configFile.exists()) {
            configFile.createNewFile();
            node = configurationLoader.load();

            node.node("serverUuid").set(UUID.randomUUID().toString());
            node.node("logFailedRequests").set(false);
            node.node("logSentData").set(false);
            node.node("logResponseStatusText").set(false);
            node.node("configVersion").set(2);

            configurationLoader.save(node);
        } else {
            node = configurationLoader.load();

            if (!node.node("configVersion").virtual()) {

                node.node("configVersion").set(2);

                node.node("enabled").comment(
                        "Enabling bStats in this file is deprecated. At least one of your plugins now uses the\n" +
                        "Sponge config to control bStats. Leave this value as you want it to be for outdated plugins,\n" +
                        "but look there for further control");

                node.node("serverUuid").comment(serverUuidComment);
                configurationLoader.save(node);
            }
        }

        // Load configuration
        serverUUID = node.node("serverUuid").getString();
        logErrors = node.node("logFailedRequests").getBoolean(false);
        logSentData = node.node("logSentData").getBoolean(false);
        logResponseStatusText = node.node("logResponseStatusText").getBoolean(false);
    }

    /**
     * Shuts down the underlying scheduler service.
     */
    public void shutdown() {
        metricsBase.shutdown();
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
        builder.appendField("playerAmount",  Sponge.server().onlinePlayers().size());
        builder.appendField("onlineMode", Sponge.server().isOnlineModeEnabled() ? 1 : 0);
        builder.appendField("minecraftVersion", Sponge.game().platform().minecraftVersion().name());
        builder.appendField("spongeImplementation", Sponge.platform().container(Platform.Component.IMPLEMENTATION).metadata().id());

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", plugin.metadata().version().toString());
    }

}