package xyz.meowing.krypt.api.dungeons.handlers

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.ZombieEntity
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.EntityEvent

/**
 * Tracks whether the Mimic miniboss has been killed in F6/F7.
 * Updates via chat messages or entity death detection.
 */
@Module
object MimicTrigger {
    val MIMIC_PATTERN = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    var mimicDead = false
        private set

    val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", $$"$skytils-dungeon-score-mimic$")

    init {
        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (DungeonAPI.floor?.floorNumber !in listOf(6, 7) || DungeonAPI.floor == null) return@registerIn

            val msg = event.message.stripped
            val match = MIMIC_PATTERN.matchEntire(msg) ?: return@registerIn

            if (mimicMessages.none { it == match.groupValues[1].lowercase() }) return@registerIn
            mimicDead = true
        }

        EventBus.registerIn<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (DungeonAPI.floor?.floorNumber !in listOf(6, 7) || mimicDead) return@registerIn
            val mcEntity = event.entity

            if (mcEntity !is ZombieEntity) return@registerIn
            if (
                !mcEntity.isBaby ||
                EquipmentSlot.entries
                    .filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }
                    .any { slot -> mcEntity.getEquippedStack(slot).isEmpty }
            ) return@registerIn

            mimicDead = true
        }
    }

    fun reset() { mimicDead = false }
}