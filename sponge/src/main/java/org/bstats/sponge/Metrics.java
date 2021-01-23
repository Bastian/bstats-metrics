package org.bstats.sponge;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;

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
         *                  <p>Not to be confused with Sponge's {@link PluginContainer#getId()} method!
         * @return A Metrics2 instance that can be used to register custom charts.
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

        Sponge.getEventManager().registerListeners(plugin, this);
    }

    @Listener
    public void startup(GamePreInitializationEvent event) {
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
                Sponge.getMetricsConfigManager().areMetricsEnabled(plugin),
                this::appendPlatformData,
                this::appendServiceData,
                task -> {
                    Scheduler scheduler = Sponge.getScheduler();
                    Task.Builder taskBuilder = scheduler.createTaskBuilder();
                    taskBuilder.execute(task).submit(plugin);
                },
                () -> true,
                logger::warn,
                logger::info,
                logErrors,
                logSentData,
                logResponseStatusText
        );

        StringBuilder builder = new StringBuilder().append(System.lineSeparator());
        builder.append("Plugin ").append(plugin.getName()).append(" is using bStats Metrics ");
        if (Sponge.getMetricsConfigManager().areMetricsEnabled(plugin)) {
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
        HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().setFile(configFile).build();
        CommentedConfigurationNode node;
        if (!configFile.exists()) {
            configFile.createNewFile();
            node = configurationLoader.load();

            node.getNode("serverUuid").setValue(UUID.randomUUID().toString());
            node.getNode("logFailedRequests").setValue(false);
            node.getNode("logSentData").setValue(false);
            node.getNode("logResponseStatusText").setValue(false);

            node.getNode("serverUuid").setComment(
                    "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                    "To control whether this is enabled or disabled, see the Sponge configuration file.\n" +
                    "Check out https://bStats.org/ to learn more :)"
            );
            node.getNode("configVersion").setValue(2);

            configurationLoader.save(node);
        } else {
            node = configurationLoader.load();

            if (!node.getNode("configVersion").isVirtual()) {

                node.getNode("configVersion").setValue(2);

                node.getNode("enabled").setComment(
                        "Enabling bStats in this file is deprecated. At least one of your plugins now uses the\n" +
                        "Sponge config to control bStats. Leave this value as you want it to be for outdated plugins,\n" +
                        "but look there for further control");

                node.getNode("serverUuid").setComment(
                        "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                        "To control whether this is enabled or disabled, see the Sponge configuration file.\n" +
                        "Check out https://bStats.org/ to learn more :)"
                );

                configurationLoader.save(node);
            }
        }

        // Load configuration
        serverUUID = node.getNode("serverUuid").getString();
        logErrors = node.getNode("logFailedRequests").getBoolean(false);
        logSentData = node.getNode("logSentData").getBoolean(false);
        logResponseStatusText = node.getNode("logResponseStatusText").getBoolean(false);
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
        builder.appendField("playerAmount",  Sponge.getServer().getOnlinePlayers().size());
        builder.appendField("onlineMode", Sponge.getServer().getOnlineMode() ? 1 : 0);
        builder.appendField("minecraftVersion", Sponge.getGame().getPlatform().getMinecraftVersion().getName());
        builder.appendField("spongeImplementation", Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName());

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", plugin.getVersion().orElse("unknown"));
    }

}