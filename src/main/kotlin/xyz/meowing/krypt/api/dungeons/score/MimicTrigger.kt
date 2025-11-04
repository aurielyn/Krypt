package xyz.meowing.krypt.api.dungeons.score

import xyz.meowing.krypt.api.dungeons.Dungeon
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.utils.StringUtils.removeFormatting

/**
 * Tracks whether the Mimic miniboss has been killed in F6/F7.
 * Updates via chat messages or entity death detection.
 */
object MimicTrigger {
    val MIMIC_PATTERN = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    var mimicDead = false

    val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", "\$skytils-dungeon-score-mimic$")

    /* lowkey dont know if this is even needed in this day and age
    val updater = EventBus.register<EntityEvent.Death>({ event ->
        val mcEntity = event.entity
        if (Dungeon.floorNumber !in listOf(6, 7) || mimicDead) return@register

        if (mcEntity !is ZombieEntity) return@register
        if (
            !mcEntity.isBaby ||
            EquipmentSlot.entries
                .filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }
                .any { slot -> mcEntity.getEquippedStack(slot).isEmpty }
        ) return@register

        mimicDead = true
    }, false)
    */

    fun init() {
        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floor?.floorNumber !in listOf(6, 7) || Dungeon.floor == null || !Dungeon.inDungeon) return@registerIn

            val msg = event.message.string.removeFormatting()
            val match = MIMIC_PATTERN.matchEntire(msg) ?: return@registerIn

            if (mimicMessages.none { it == match.groupValues[1].lowercase() }) return@registerIn
            mimicDead = true
        }
    }

    fun reset() { mimicDead = false }
}