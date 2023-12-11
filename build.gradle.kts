
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.github.ikorennoy"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3")
    type.set("IC")
}

dependencies {
    implementation("com.hierynomus:sshj:0.37.0")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        version.set(properties("version"))
        pluginDescription.set(file("parts/pluginDescription.html").readText())
        changeNotes.set(file("parts/pluginChanges.html").readText())
    }

    publishPlugin {
        token.set(System.getenv("IJ_REPO_TOKEN"))
    }
}
