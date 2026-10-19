plugins {
    id("net.minecraftforge.gradle") version "7.0.40"
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-forge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val mixinConfig = "${property("mod.id")}.mixins.json"

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()

    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${sc.current.version}-${property("deps.forge")}"))
}

minecraft {
    // 26.1+ ships unobfuscated, older versions need Mojang mappings applied
    if (sc.current.parsed < "26.1") mappings("official", sc.current.version)

    runs {
        configureEach {
            workingDir = rootProject.layout.projectDirectory.dir("run") // Shares the run directory between versions
            // Mixin picks the config up from the jar manifest in production, but not in dev
            args("--mixin.config=$mixinConfig")
        }

        register("client") {
            // `runClient -Pselftest` runs the in-game checks in SelfTest and quits
            if (providers.gradleProperty("selftest").isPresent) systemProperty("${project.property("mod.id")}.selftest", "true")
        }

        register("server") {
            args("--nogui")
        }
    }
}

// Forge loads each classpath directory as its own module, so in dev the mod's classes and resources
// have to share one, as in the Forge MDK; otherwise the classes load outside the game and crash it
sourceSets.configureEach {
    val dir = layout.buildDirectory.dir("sourceSets/$name")
    output.setResourcesDir(dir)
    java.destinationDirectory = dir
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("authors", sc.properties["mod.authors"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("forge", Regex("^\\d+").find(sc.properties.get<String>("deps.forge"))!!.value)
            // 1.21.9 replaced pack_format with a range of formats
            val format = sc.properties.get<String>("mod.resource_format").toInt()
            register("pack_format", if (format < 65) "\"pack_format\": $format" else "\"min_format\": $format, \"max_format\": $format")
        }

        filesMatching("META-INF/mods.toml") { expand(props) }
        // Forge only loads a mod's assets, such as its translations, when the mod has pack metadata
        filesMatching("pack.mcmeta") { expand(props) }

        // Forge ships Mixin 0.8.7, which knows no level past Java 21 and refuses the whole config otherwise
        val mixinJava = "JAVA_${minOf(requiredJava.majorVersion.toInt(), 21)}"
        inputs.property("mixinJava", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "*.ct", "*.classtweaker")
    }

    named<Jar>("jar") {
        // Forge has no mixin entry in mods.toml, Mixin finds configs through the manifest
        manifest.attributes["MixinConfigs"] = mixinConfig
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
