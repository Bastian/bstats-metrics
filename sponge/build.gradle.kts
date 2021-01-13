plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("http://repo.spongepowered.org/maven")
    }
}

dependencies {
    compileOnly("org.spongepowered:spongeapi:7.1.0")
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
