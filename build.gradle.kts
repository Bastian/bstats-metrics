import org.gradle.api.publish.PublishingExtension

apply(from = "generate-metrics.gradle.kts")
apply(from = "increment-version.gradle.kts")

plugins {
    `java-library`
    `maven-publish`
    signing
}

allprojects {
    group = "org.bstats"
}

configure<PublishingExtension> {
    subprojects {
        val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

        apply(plugin = "java-library")
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    groupId = "org.bstats"
                    afterEvaluate {
                        artifactId = "bstats-${project.name}"
                    }
                    from(components["java"])
                    pom {
                        name.set("bStats-Metrics")
                        description.set("The bStats Metrics class")
                        url.set("http://bStats.org")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/mit-license.php")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/Bastian/bStats-Metrics.git")
                            developerConnection.set("scm:git:git@github.com:Bastian/bStats-Metrics.git")
                            url.set("https://github.com/Bastian/bStats-Metrics")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "OSSRH"
                    url = if (isReleaseVersion) {
                        uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    } else {
                        uri("https://oss.sonatype.org/content/repositories/snapshots/")
                    }
                    credentials {
                        username = System.getenv("MAVEN_USERNAME")
                        password = System.getenv("MAVEN_PASSWORD")
                    }
                }
            }
        }

        signing {
            val signingKey = System.getenv("SIGNING_KEY")
            val signingPassword = System.getenv("SIGNING_PASSWORD")
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["mavenJava"])
        }
    }
}