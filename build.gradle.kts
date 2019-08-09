import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.3.11"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.1")
    }
}

plugins {
    java
    application
}

apply {
    plugin("com.github.johnrengelman.shadow")
    plugin("kotlin")
}

group = "Thesaurus"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        // will disable warning about usage of experimental Kotlin features
        freeCompilerArgs = freeCompilerArgs + listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
                "-Xuse-experimental=kotlin.Experimental")
    }
}

val kotlinVersion: String by extra

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))

    val arrow = "0.8.2"

    compile("info.bliki.wiki:bliki-core:3.1.0")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    compile("com.google.code.gson:gson:2.8.5")
    compile("org.deeplearning4j:deeplearning4j-core:1.0.0-beta3")
    compile("io.arrow-kt:arrow-data:$arrow")
    compile("io.arrow-kt:arrow-core:$arrow")
    compile("io.arrow-kt:arrow-effects:$arrow")
    compile("io.arrow-kt:arrow-effects-kotlinx-coroutines:$arrow")
    compile("io.arrow-kt:arrow-instances-core:$arrow")

    compile("org.deeplearning4j:deeplearning4j-nlp:1.0.0-beta3")

    val arrowVersion = "0.9.0"

    compile("io.arrow-kt:arrow-core-extensions:$arrowVersion")
    compile("io.arrow-kt:arrow-syntax:$arrowVersion")
    compile("io.arrow-kt:arrow-typeclasses:$arrowVersion")
    compile("io.arrow-kt:arrow-extras-data:$arrowVersion")
    compile("io.arrow-kt:arrow-extras-extensions:$arrowVersion")
    compile("io.arrow-kt:arrow-effects-data:$arrowVersion")
    compile("io.arrow-kt:arrow-effects-extensions:$arrowVersion")
    compile("io.arrow-kt:arrow-effects-io-extensions:$arrowVersion")

    compile("com.github.ajalt:clikt:2.0.0")
    
    compile("org.nd4j:nd4j-native-platform:1.0.0-beta3")
    //compile("org.nd4j:nd4j-cuda-9.0-platform:1.0.0-beta3")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "runner.RunnerKt"
}
