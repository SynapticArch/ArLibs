plugins {
    kotlin("jvm") version "2.2.0-RC"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.arteam"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Database dependencies
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.slf4j:slf4j-api:2.0.12")

    constraints {
        implementation("com.google.guava:guava:33.4.8-jre") {
            because("CVE-2023-2976: Force Guava version to address vulnerability")
        }
    }
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.20")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
