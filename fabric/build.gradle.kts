plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "fabricmc"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    compileOnly("net.fabricmc:fabric-loader:0.19.5")
    compileOnly("org.slf4j:slf4j-api:2.0.16")
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
