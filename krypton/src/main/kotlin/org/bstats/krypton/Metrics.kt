package org.bstats.krypton

import org.bstats.MetricsBase
import org.bstats.charts.CustomChart
import org.bstats.config.MetricsConfig
import org.bstats.json.JsonObjectBuilder
import org.kryptonmc.krypton.api.plugin.Plugin
import org.kryptonmc.krypton.api.plugin.PluginLoadState
import java.nio.file.Path

class Metrics(private val plugin: Plugin, serviceId: Int) {

    private val server = plugin.context.server
    private val base: MetricsBase

    init {
        val configFile = Path.of("").resolve("plugins").resolve("bStats").resolve("config.txt").toFile()
        val config = MetricsConfig(configFile, true)

        base = MetricsBase(
            "krypton",
            config.serverUUID,
            serviceId,
            config.isEnabled,
            ::appendPlatformData,
            ::appendServiceData,
            { server.scheduler.run(plugin) { it.run() } },
            { plugin.loadState == PluginLoadState.INITIALIZED },
            plugin.context.logger::error,
            plugin.context.logger::info,
            config.isLogErrorsEnabled,
            config.isLogSentDataEnabled,
            config.isLogResponseStatusTextEnabled
        )
    }

    @JvmName("addCustomChart")
    operator fun plusAssign(chart: CustomChart) {
        base.addCustomChart(chart)
    }

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", server.players.size)
        builder.appendField("onlineMode", if (server.isOnline) 1 else 0)
        builder.appendField("kryptonVersion", server.info.version)
        builder.appendField("kryptonName", server.info.name)

        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.context.description.version)
    }
}
