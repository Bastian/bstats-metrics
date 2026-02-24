package org.bstats.hytale;

import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
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
    private final HytaleLogger logger;
    private final boolean logErrors;
    private MetricsBase metricsBase;

    public Metrics(PluginBase pluginBase, int serviceId) {
        this.pluginBase = pluginBase;
        this.logger = HytaleLogger.getLogger();

        if (ServerAuthManager.getInstance().isSingleplayer()) {
            logErrors = false;
            return;
        }

        File configFile = new File("bstats.txt");
        MetricsConfig config;
        try {
            config = new MetricsConfig(configFile, true);
        } catch (IOException e) {
            logErrors = false;
            logger.atWarning().withCause(e).log("Failed to create bStats config");
            return;
        }

        logErrors = config.isLogErrorsEnabled();

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
                logErrors,
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled(),
                false
        );

        if (!config.didExistBefore()) {
            // Send an info message when the bStats config file gets created for the first time
            logger.atInfo().log("Some of your mods collect metrics and send them to bStats (https://bStats.org).");
            logger.atInfo().log("bStats collects some basic information for mod authors, like how many people use");
            logger.atInfo().log("their mod and their total player count. It's recommended to keep bStats enabled, but");
            logger.atInfo().log("if you're not comfortable with this, you can opt-out by editing the bstats.txt file in");
            logger.atInfo().log("the server root folder and setting enabled to false.");
        }
    }

    /**
     * Shuts down the underlying scheduler service.
     */
    public void shutdown() {
        if (metricsBase != null) {
            metricsBase.shutdown();
        }
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
        tryAppend(() -> builder.appendField("playerAmount", Universe.get().getPlayerCount()));
        tryAppend(() -> builder.appendField("authMode", Options.getOrDefault(Options.AUTH_MODE, Options.getOptionSet(), Options.AuthMode.AUTHENTICATED).name()));
        tryAppend(() -> builder.appendField("hytaleVersion", ManifestUtil.getImplementationVersion()));

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        tryAppend(() -> builder.appendField("pluginVersion", pluginBase.getManifest().getVersion().toString()));
    }

    /**
     * Hytale is still in early access, so we want to be extra careful when
     * using their APIs, as they might change at any time. This would be pretty
     * bad, as it would break all plugins using bStats.
     * <p>
     * To mitigate this risk, we wrap all optional API calls in a try-catch
     * block. This will not catch full renames (due to the imports), but
     * hopefully make us more resilient at least some API changes.
     */
    private void tryAppend(Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            if (logErrors) {
                logger.atWarning().withCause(e).log("Failed to append bStats platform data");
            }
        }
    }

}
