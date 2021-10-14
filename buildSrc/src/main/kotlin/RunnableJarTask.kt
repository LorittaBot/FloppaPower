import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.task
import java.io.File

val DEFAULT_SHADED_WITHIN_JAR_LIBRARIES = arrayOf(
    "common-",
    "loritta-serializable-commons-",
    "parallax-code-api-",
    "db-tables-"
)

/**
 * Creates a Runnable JAR with a few dependencies inside (`loritta-api`, `loritta-serializable-commons` and `parallax-code-api`)
 *
 * A manifest file is created with the application class path and sets the main class attribute to [mainClass].
 *
 * Custom manifest attributes can be set, or overriden, with the [customAttributes] map
 *
 * So this is just like a "poor mans" Gradle Shadow Plugin :P, however there are a few advantages of doing this!
 *
 * https://product.hubspot.com/blog/the-fault-in-our-jars-why-we-stopped-building-fat-jars
 *
 * @param mainClass        the main class of the application
 * @param customAttributes custom attributes to be set in the manifest
 */
fun Project.runnableJarTask(
    addToFinalJarSourceProjectsPrefixes: Array<String> = DEFAULT_SHADED_WITHIN_JAR_LIBRARIES,
    runtimeClasspath: Configuration,
    taskProvider: Jar,
    mainClass: String,
    customAttributes: Map<String, String>
): Task {
    return task("runnableJar", type = Jar::class) {
        doFirst {
            archiveBaseName.set("${project.name}-runnable")

            manifest {
                fun addIfAvailable(name: String, attrName: String) {
                    attributes[attrName] = System.getProperty(name) ?: "Unknown"
                }

                addIfAvailable("build.number", "Build-Number")
                addIfAvailable("commit.hash", "Commit-Hash")
                addIfAvailable("git.branch", "Git-Branch")
                addIfAvailable("compiled.at", "Compiled-At")
                addIfAvailable("github.build.id", "Github-Build-Id")
                attributes["Main-Class"] = mainClass
                attributes["Class-Path"] = runtimeClasspath
                    .filterNot { addToFinalJarSourceProjectsPrefixes.any { sourceName -> it.name.startsWith(sourceName) } }
                    .filter { it.extension == "jar" }
                    .distinctBy { it.name }
                    .joinToString(" ", transform = { "libs/" + it.name })
                attributes.putAll(customAttributes)
            }

            // Add any required dependencies inside the JAR
            // This NEEDS to be within the "doFirst", not in "doLast"!
            // In the "doLast", the JARs were never added to the JAR (I wonder why?)
            from(runtimeClasspath.mapNotNull {
                if (addToFinalJarSourceProjectsPrefixes.any { sourceName -> it.name.startsWith(sourceName) }) {
                    zipTree(it)
                } else null
            })
        }

        // Only copy the libs in a "doLast"
        // doLast means that this won't be executed when loading the build.gradle.kts
        // (Yes, by default Gradle will run everything in this task block, even if you are compiling a unrelated project)
        // Very strange...
        doLast {
            println("Copying dependencies JARs for ${project.name}...")

            val libs = File(project.projectDir, "build/libs/libs")
            libs.mkdirs()
            // And the rest we will store outside of the JAR
            from(runtimeClasspath.mapNotNull {
                if (!addToFinalJarSourceProjectsPrefixes.any { sourceName -> it.name.startsWith(sourceName) }) {
                    val output = File(libs, it.name)

                    if (it.exists() && !output.exists() && it.extension == "jar")
                        it.copyTo(output, true)
                }
                null
            })
        }

        with(taskProvider as CopySpec)
    }
}