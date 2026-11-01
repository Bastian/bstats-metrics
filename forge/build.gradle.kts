plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "minecraftforge"
        url = uri("https://maven.minecraftforge.net/")
    }
}

dependencies {
    compileOnly("net.minecraftforge:fmlcore:1.20.1-47.4.23") {
        isTransitive = false
    }
    compileOnly("net.minecraftforge:fmlloader:1.20.1-47.4.23") {
        isTransitive = false
    }
    compileOnly("net.minecraftforge:forgespi:7.1.0") {
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}
