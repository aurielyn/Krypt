pluginManagement {
	repositories {
		maven("https://maven.deftu.dev/releases")
		maven("https://maven.deftu.dev/snapshots")

		maven("https://jitpack.io/")
		maven("https://maven.fabricmc.net")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net")
		maven("https://repo.essential.gg/repository/maven-public")
		maven("https://server.bbkr.space/artifactory/libs-release/")

		maven("https://maven.terraformersmc.com/") // ModMenu
		maven("https://maven.teamresourceful.com/repository/maven-private/") // Blossom 1.3.2

		mavenLocal()
		mavenCentral()

		gradlePluginPortal()
	}

	plugins {
		kotlin("jvm") version("2.0.0")
		id("dev.deftu.gradle.multiversion-root") version("2.57.0")
	}
}

val projectName: String = extra["mod.name"].toString()

rootProject.name = projectName
rootProject.buildFileName = "root.gradle.kts"

listOf(
	"1.21.5-fabric",
	"1.21.7-fabric",
	"1.21.9-fabric"
).forEach { version ->
	include(":$version")
	project(":$version").apply {
		projectDir = file("versions/$version")
		buildFileName = "../../build.gradle.kts"
	}
}