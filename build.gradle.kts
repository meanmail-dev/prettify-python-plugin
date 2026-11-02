import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun config(name: String) = project.findProperty(name).toString()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.kotlinSerialization)
}

group = config("group")
version = config("version")

dependencies {
    implementation(libs.kotlinxSerialization)
    testImplementation(libs.junit)

    intellijPlatform {
        create(config("platformType"), config("platformVersion")) {
            useInstaller = false
        }
        jetbrainsRuntime()
        val plugins = providers.gradleProperty("plugins").map { it.split(',') }
        if (plugins.isPresent && plugins.get().isNotEmpty()) {
            plugins(plugins)
        }
        val platformBundledPlugins = providers.gradleProperty("platformBundledPlugins").map { it.split(',') }
        if (platformBundledPlugins.isPresent && platformBundledPlugins.get().isNotEmpty()) {
            bundledPlugins(platformBundledPlugins)
        }

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set(config("pluginName"))
        version.set(project.version.toString())
        description.set(file("description.html").readText())
        changeNotes.set(readChangeNotes("CHANGES.md"))
        ideaVersion {
            sinceBuild.set(config("platformSinceBuild"))
        }
    }

    buildSearchableOptions = false

    pluginVerification.ides {
        create("PC", "2024.3.6.1") {
            useInstaller = false
        }
        recommended()
    }

    publishing {
        try {
            token.set(file("token.txt").readLines()[0])
        } catch (e: Exception) {
            println("No token.txt found")
        }
        channels.set(listOf(config("publishChannel")))
    }
}

fun readChangeNotes(pathname: String): String {
    val lines = file(pathname).readLines()

    val notes: MutableList<MutableList<String>> = mutableListOf()

    var note: MutableList<String>? = null

    for (line in lines) {
        if (line.startsWith('#')) {
            if (notes.size == 3) {
                break
            }
            note = mutableListOf()
            notes.add(note)
            val header = line.trimStart('#')
            note.add("<b>$header</b>")
        } else if (line.isNotBlank()) {
            note?.add(line)
        }
    }

    return notes.joinToString(
        "</p><br><p>",
        prefix = "<p>",
        postfix = "</p><br>"
    ) {
        it.joinToString("<br>")
    } +
            "See the full change notes on the <a href='" +
            config("repository") +
            "/blob/master/CHANGES.md'>github</a>"
}

tasks {
    config("jvmVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(it))
        }
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = config("gradleVersion")
    }

    processResources {
        val resourcePluginVersion = project.version.toString()
        inputs.property("pluginVersion", resourcePluginVersion)
        filesMatching("prettify-python-version.txt") {
            expand("pluginVersion" to resourcePluginVersion)
        }
    }

    test {
        useJUnit()
        systemProperty("java.awt.headless", "false")
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

        maxHeapSize = "1G"
    }
}
