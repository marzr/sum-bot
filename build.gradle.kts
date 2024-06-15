import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val jvmTargetVersion = JavaVersion.VERSION_11

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    id ("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example.blank"
version = "0.0.1"
application {
    mainClass.set("com.github.marzr.sum.bot.TgBotApplicationKt")
}
val exposedVersion = "0.48.0"
val postgresqlVersion = "42.7.2"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.tg.bot)
    ksp(libs.tg.ksp)

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    implementation("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks {
    compileJava {
        targetCompatibility = jvmTargetVersion.majorVersion
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = jvmTargetVersion.majorVersion
            javaParameters = true
        }
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("bot.jar")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
