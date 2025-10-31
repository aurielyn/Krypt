import dev.deftu.gradle.utils.version.MinecraftVersions

plugins {
	java
	kotlin("jvm")
	id("dev.deftu.gradle.multiversion")
	id("dev.deftu.gradle.tools")
	id("dev.deftu.gradle.tools.resources")
	id("dev.deftu.gradle.tools.bloom")
	id("dev.deftu.gradle.tools.shadow")
	id("dev.deftu.gradle.tools.minecraft.loom")
	id("dev.deftu.gradle.tools.minecraft.releases")
}

toolkitMultiversion {
	moveBuildsToRootProject.set(true)
}

toolkitLoomHelper {
	useMixinRefMap(modData.id)
	useDevAuth("1.2.1")
}

dependencies {
	modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
	modImplementation(include("xyz.meowing:vexel-${mcData}:116")!!)

	when (mcData.version) {
		MinecraftVersions.VERSION_1_21_9 -> modImplementation("com.terraformersmc:modmenu:16.0.0-rc.1")
		MinecraftVersions.VERSION_1_21_7 -> modImplementation("com.terraformersmc:modmenu:15.0.0")
		MinecraftVersions.VERSION_1_21_5 -> modImplementation("com.terraformersmc:modmenu:14.0.0-rc.2")
		else -> {}
	}
}