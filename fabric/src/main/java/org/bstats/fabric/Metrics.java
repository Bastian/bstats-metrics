package org.bstats.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.config.MetricsConfig;
import org.bstats.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Metrics {

    public interface ServerInfo {

        int getPlayerAmount();

        boolean isOnlineMode();
    }

    private final FabricLoader loader;
    private final ModContainer modContainer;
    private final ServerInfo serverInfo;
    private MetricsBase metricsBase;

    public Metrics(String modId, int serviceId) {
        this(modId, serviceId, null);
    }

    public Metrics(String modId, int serviceId, ServerInfo serverInfo) {
        this(modId, serviceId, serverInfo, LoggerFactory.getLogger("bStats"));
    }

    public Metrics(String modId, int serviceId, ServerInfo serverInfo, Logger logger) {
        this.loader = FabricLoader.getInstance();
        this.modContainer = loader.getModContainer(modId)
                .orElseThrow(() -> new IllegalArgumentException("No mod with the id '" + modId + "' is loaded"));
        this.serverInfo = serverInfo;

        File configFile = loader.getConfigDir().resolve("bStats").resolve("config.txt").toFile();
        MetricsConfig config;
        try {
            config = new MetricsConfig(configFile, true);
        } catch (IOException e) {
            logger.error("Failed to create bStats config", e);
            return;
        }

        metricsBase = new MetricsBase(
                "fabric",
                config.getServerUUID(),
                serviceId,
                config.isEnabled(),
                this::appendPlatformData,
                this::appendServiceData,
                null,
                () -> true,
                logger::warn,
                logger::info,
                config.isLogErrorsEnabled(),
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled(),
                false
        );

        if (!config.didExistBefore()) {
            // Send an info message when the bStats config file gets created for the first time
            logger.info("Some of the mods on this server collect metrics and send them to bStats (https://bStats.org).");
            logger.info("bStats collects some basic information for mod authors, like how many people use");
            logger.info("their mod and their total player count. It's recommended to keep bStats enabled, but");
            logger.info("if you're not comfortable with this, you can opt-out by editing the config.txt file in");
            logger.info("the '/config/bStats/' folder and setting enabled to false.");
        }
    }

    public void shutdown() {
        if (metricsBase != null) {
            metricsBase.shutdown();
        }
    }

    public void addCustomChart(CustomChart chart) {
        if (metricsBase != null) {
            metricsBase.addCustomChart(chart);
        }
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        if (serverInfo != null) {
            builder.appendField("playerAmount", serverInfo.getPlayerAmount());
            builder.appendField("onlineMode", serverInfo.isOnlineMode() ? 1 : 0);
        }
        builder.appendField("minecraftVersion", loader.getRawGameVersion());
        builder.appendField("fabricLoaderVersion", getLoaderVersion());
        builder.appendField("modCount", loader.getAllMods().size());

        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("modVersion", modContainer.getMetadata().getVersion().getFriendlyString());
    }

    private String getLoaderVersion() {
        return loader.getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

}
