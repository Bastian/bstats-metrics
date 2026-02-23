plugins {
    id("com.gradleup.nmcp.settings").version("1.4.4")
}

rootProject.name = "bstats-metrics"
include("base", "bukkit", "bungeecord", "sponge", "velocity", "hytale")

nmcpSettings {
    centralPortal {
        username = System.getenv("MAVEN_USERNAME")
        password = System.getenv("MAVEN_PASSWORD")
    }
}
