plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "neoforged"
        url = uri("https://maven.neoforged.net/releases/")
    }
}

dependencies {
    compileOnly("net.neoforged.fancymodloader:loader:4.0.44") {
        isTransitive = false
    }
    compileOnly("org.apache.maven:maven-artifact:3.9.9")
    compileOnly("org.apache.logging.log4j:log4j-api:2.22.1")
    api(project(":base")) {
        isTransitive = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}
