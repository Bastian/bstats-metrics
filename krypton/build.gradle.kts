import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

repositories {
    mavenCentral()
    maven("https://repo.bristermitten.me/repository/maven-public/")
}

dependencies {
    api(project(":base")) {
        isTransitive = true
    }
    compileOnly(kotlin("stdlib"))
    compileOnly("org.kryptonmc:krypton-api:0.18.3")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform()
    }
}
