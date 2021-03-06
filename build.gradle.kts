import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.30"
    id("org.jetbrains.intellij") version "0.7.2"
}

group = "ru.meanmail"
version = "${project.properties["version"]}-${project.properties["postfix"]}"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit:junit:4.13.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = project.properties["gradleVersion"].toString()
}

tasks.test {
    useJUnit()

    maxHeapSize = "1G"
}

intellij {
    pluginName = project.properties["pluginName"].toString()
    version = if (project.properties["eap"].toString() == "true") {
        "LATEST-EAP-SNAPSHOT"
    } else {
        project.properties["IdeVersion"].toString()
    }
    type = project.properties["ideType"].toString()
    when (type) {
        "PY" -> {
            setPlugins("python")
        }
        "PC" -> {
            setPlugins("PythonCore")
        }
        else -> {
            setPlugins(project.properties["pythonPluginVersion"].toString())
        }
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
            project.properties["repository"] +
            "/blob/master/ChangeNotes.md'>github</a>"
}

tasks.withType<PatchPluginXmlTask> {
    setPluginDescription(file("Description.html").readText())
    setChangeNotes(readChangeNotes("ChangeNotes.md"))
}
