import com.google.cloud.tools.jib.gradle.JibExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    idea
    id("com.diffplug.spotless") version "5.12.5"
    id("com.google.cloud.tools.jib") version "1.0.2" apply false
    id("com.palantir.consistent-versions") version "1.28.0"
    id("net.ltgt.errorprone") version "2.0.1" apply false
    id("org.inferred.processors") version "3.3.0" apply false
}

version = "git describe --tags".runCommand().trim()

allprojects {
    group = "io.github.markelliot.md"
    version = rootProject.version
}

allprojects {
    apply(plugin = "idea")
    apply(plugin = "com.diffplug.spotless")

    // lives in allprojects because of consistent-versions
    repositories {
        jcenter()
    }

    plugins.withType<ApplicationPlugin> {
        apply(plugin = "com.google.cloud.tools.jib")

        val projectId = System.getenv("GOOGLE_PROJECT_ID")
        val imageName = "gcr.io/$projectId/${project.name}:${project.version}"

        // https://circleci.com/docs/2.0/google-auth/
        // https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#using-specific-credentials
        configure<JibExtension> {
            to {
                image = imageName
                auth {
                    username = "_json_key"
                    password = System.getenv("GCLOUD_SERVICE_KEY")
                }
            }
        }

        tasks.register("publish").get().dependsOn("jib")
        tasks.register("publishLocal").get().dependsOn("jibBuildTar")

        task("deploy") {
            doLast {
                "kubectl set image deployment/${project.name} ${project.name}=$imageName".runCommand()
            }
        }.dependsOn("publish")

        // Only enable publishing on CI
        // https://circleci.com/docs/2.0/env-vars/#built-in-environment-variables
        tasks["publish"].enabled = booleanEnv("CIRCLECI") ?: false
    }

    plugins.withType<JavaLibraryPlugin> {
        apply(plugin = "net.ltgt.errorprone")
        apply(plugin = "org.inferred.processors")

        dependencies {
            "errorprone"("com.google.errorprone:error_prone_core")
            "errorprone"("com.jakewharton.nopen:nopen-checker")
            "compileOnly"("com.jakewharton.nopen:nopen-annotations")
        }

        spotless {
            java {
                googleJavaFormat("1.10.0").aosp()
            }
        }

        tasks.withType<JavaCompile> {
            options.errorprone.disable("UnusedVariable")
        }

        the<JavaPluginExtension>().sourceCompatibility = JavaVersion.VERSION_16
    }

    spotless {
        kotlinGradle {
            ktlint()
        }
    }

    tasks["check"].dependsOn("spotlessCheck")
    tasks.register("format").get().dependsOn("spotlessApply")
}

fun booleanEnv(envVar: String): Boolean? {
    return System.getenv(envVar)?.toBoolean()
}

fun String.runCommand(): String {
    val proc = ProcessBuilder(*split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
    proc.waitFor(10, TimeUnit.SECONDS)
    return proc.inputStream.bufferedReader().readText()
}
