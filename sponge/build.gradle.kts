plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }
}

dependencies {
    compileOnly("org.spongepowered:spongeapi:8.0.0")
    api(project(":base")) {
        isTransitive = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}
