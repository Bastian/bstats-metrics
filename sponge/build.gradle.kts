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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}
