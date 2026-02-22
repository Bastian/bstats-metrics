package org.bstats.hytale;

import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.Universe;

import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.config.MetricsConfig;
import org.bstats.json.JsonObjectBuilder;

import java.io.File;
import java.io.IOException;

public class Metrics {

    private final PluginBase pluginBase;
    private MetricsBase metricsBase;

    public Metrics(PluginBase pluginBase, int serviceId) {
        this.pluginBase = pluginBase;
        HytaleLogger logger = HytaleLogger.getLogger();

        File configFile = new File("bStats.txt");
        MetricsConfig config;
        try {
            config = new MetricsConfig(configFile, true);
        } catch (IOException e) {
            logger.atWarning().withCause(e).log("Failed to create bStats config");
            return;
        }

        metricsBase = new MetricsBase(
                "hytale",
                config.getServerUUID(),
                serviceId,
                config.isEnabled(),
                this::appendPlatformData,
                this::appendServiceData,
                null,
                () -> true,
                (msg, throwable) -> logger.atWarning().withCause(throwable).log(msg),
                msg -> logger.atInfo().log(msg),
                config.isLogErrorsEnabled(),
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled(),
                false
        );

        if (!config.didExistBefore()) {
            // Send an info message when the bStats config file gets created for the first time
            logger.atInfo().log("Some of your mods collect metrics and send them to bStats (https://bStats.org).");
            logger.atInfo().log("bStats collects some basic information for mod authors, like how many people use");
            logger.atInfo().log("their mod and their total player count. It's recommend to keep bStats enabled, but");
            logger.atInfo().log("if you're not comfortable with this, you can opt-out by editing the bStats.txt file in");
            logger.atInfo().log("the server root folder and setting enabled to false.");
        }
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
        if (metricsBase != null) {
            metricsBase.addCustomChart(chart);
        }
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", Universe.get().getPlayerCount());
        builder.appendField("authMode", Options.getOrDefault(Options.AUTH_MODE, Options.getOptionSet(), Options.AuthMode.AUTHENTICATED).name());
        builder.appendField("hytaleVersion", ManifestUtil.getImplementationVersion());

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", pluginBase.getManifest().getVersion().toString());
    }

}
