@file:Suppress("ConstPropertyName")

package xyz.meowing.krypt.features.general

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.item.ItemEntity
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.events.core.DungeonEvent

@Module
object SecretSound : Feature(
    "secretSound",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val SECRET_DISTANCE = 10.0

    enum class SoundTypes(private val sound: SoundEvent, private val dropdownName: String) {
        BLAZE(SoundEvents.BLAZE_HURT, "Blaze"),
        CAT(SoundEvents.CAT_AMBIENT, "Cat"),
        ANVIL(SoundEvents.ANVIL_LAND, "Anvil"),
        XP(SoundEvents.EXPERIENCE_ORB_PICKUP, "Experience"),
        NOTE_BLOCK(SoundEvents.NOTE_BLOCK_PLING.value(), "Note Block");

        fun getName() = dropdownName
        fun getSound() = sound
    }

    private val dropdownValues = SoundTypes.entries.map { it.getName() }.toList()

    private val volume by ConfigDelegate<Double>("secretSound.volume")
    private val soundIndex by ConfigDelegate<Int>("secretSound.sound")
    private val pitch by ConfigDelegate<Double>("secretSound.pitch")

    private val selectedSound: SoundEvent
        get() = SoundTypes.entries[soundIndex].getSound()

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Secret sound",
                "Secret sound",
                "General",
                ConfigElement(
                    "secretSound",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Volume",
                ConfigElement(
                    "secretSound.volume",
                    ElementType.Slider(0.0, 100.0, 100.0, false)
                )
            )
            .addFeatureOption(
                "Pitch",
                ConfigElement(
                    "secretSound.pitch",
                    ElementType.Slider(0.0, 100.0, 100.0, false)
                )
            )
            .addFeatureOption(
                "Sound",
                ConfigElement(
                    "secretSound.sound",
                    ElementType.Dropdown(dropdownValues, 0)
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Secrets.Chest> { playSound() }
        register<DungeonEvent.Secrets.Misc> { playSound() }
        register<DungeonEvent.Secrets.Essence> { playSound() }

        register<DungeonEvent.Secrets.Item> { event ->
            val itemId = event.entityId

            val player = client.player ?: return@register
            val world = client.level
            val entity = world?.getEntity(itemId) as? ItemEntity ?: return@register

            if (player.distanceTo(entity) <= SECRET_DISTANCE) playSound()
        }

        register<DungeonEvent.Secrets.Bat> { event ->
            val player = client.player ?: return@register
            if (player.distanceTo(event.entity) <= SECRET_DISTANCE) playSound()
        }
    }

    private fun playSound() {
        player?.playSound(
            selectedSound,
            volume.toFloat() / 100,
            pitch.toFloat() / 100
        )
    }
}