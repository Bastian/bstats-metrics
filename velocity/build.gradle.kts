plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:1.1.3")
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