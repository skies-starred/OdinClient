import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
    alias(libs.plugins.fletchingTable)
    `maven-publish`
}

val mc = stonecutter.current.version

version = "${property("mod.version")}+$mc"
base.archivesName = property("mod.id").toString()

repositories {
    @Suppress("UnstableApiUsage")
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://jitpack.io", "com.github.stivais", "com.github.odtheking", "com.github.sivthepolarfox")
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "odinClient.mixins.json") {
            env("CLIENT")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.officialMojangMappings())

    modRuntimeOnly(libs.devauth)

    modImplementation("fabric-api".mc(mc))
    modImplementation(fletchingTable.modrinth("odin", mc))
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.language.kotlin)
    shadow(libs.commodore)

    // needed by odin to work properly in dev env as we use modrinth which doesn't provide the transitive dependencies
    modImplementation(libs.okhttp)
    modImplementation(libs.okio)
    modImplementation(libs.lwjgl.nanovg)
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Dmixin.debug.export=true",
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-XX:+AllowEnhancedClassRedefinition"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to project.property("mod.id"),
                    "name" to project.property("mod.name"),
                    "version" to project.property("mod.version"),
                    "minecraft" to project.property("mod.mc_dep")
                )
            )
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()

fun DependencyHandler.shadow(dep: Any) {
    include(dep)
    modImplementation(dep)
}