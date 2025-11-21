package xyz.meowing.krypt.config

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.hud.HudEditor
import xyz.meowing.krypt.managers.config.ConfigManager.configUI
import xyz.meowing.krypt.managers.config.ConfigManager.openConfig
import xyz.meowing.krypt.utils.modMessage
import java.lang.Exception

@Command
object ConfigCommand : Commodore("krypt") {
    init {
        literal("updateConfig") {
            runs { configName: String, newValue: String, silent: Boolean ->
                try {
                    val value = parseValue(newValue)
                    configUI.updateConfig(configName, value)
                    if (!silent) KnitChat.modMessage("§fUpdated config §b$configName §fto §b$value§f.")
                    Krypt.LOGGER.info("Updated config $configName to value $value [${value.javaClass}]")
                } catch (e: Exception) {
                    Krypt.LOGGER.error("Caught exception in command \"/krypt updateConfig\": $e")
                }
            }
        }

        literal("hud") {
            runs {
                TickScheduler.Client.post {
                    client.execute { client.setScreen(HudEditor()) }
                }
            }
        }

        literal("test") {
            runs {
                val currentRoom = DungeonAPI.currentRoom ?: return@runs
                val test = ScanUtils.getRoomCenter(currentRoom)

                KnitChat.fakeMessage("$prefix §fRoom centers for room §c${currentRoom.name}")
                KnitChat.fakeMessage("§d| §fCenter §c${test}")
                KnitPlayer.player?.let { p ->
                    KnitChat.fakeMessage("§d| §fCenter2 §c${WorldScanUtils.getRoomCenter(p.x.toInt(), p.z.toInt())}")
                }
                KnitChat.fakeMessage("§d| §fRoom Center §c${currentRoom.center}")
                KnitChat.fakeMessage("§d| §fComp Centers:")
                currentRoom.componentCenters.forEach {
                    KnitChat.fakeMessage("§d| §f- §c${it}")
                }
            }
        }


        runs {
            openConfig()
        }
    }

    private fun parseValue(input: String): Any {
        return when {
            input.toBooleanStrictOrNull() != null -> input.toBoolean()
            input.toIntOrNull() != null -> input.toInt()
            input.toDoubleOrNull() != null -> input.toDouble()
            input.toFloatOrNull() != null -> input.toFloat()
            else -> input
        }
    }
}