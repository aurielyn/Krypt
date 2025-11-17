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
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.events.core.DungeonEvent
import kotlin.math.abs

@Module
object SecretSound : Feature(
    "secretSound",
    island = SkyBlockIsland.THE_CATACOMBS
){
    enum class SoundTypes(private val sound: SoundEvent, private val dropdownName: String) {
        BLAZE(SoundEvents.BLAZE_HURT, "Blaze"),
        CAT(SoundEvents.CAT_AMBIENT, "Cat"),
        ANVIL(SoundEvents.ANVIL_LAND, "Anvil"),
        XP(SoundEvents.EXPERIENCE_ORB_PICKUP, "Experience");

        fun getName() = dropdownName
        fun getSound() = sound
    }

    private const val secretDistance = 10.0
    private const val redSkull = "eyJ0aW1lc3RhbXAiOjE1NzA5MTUxODU0ODUsInByb2ZpbGVJZCI6IjVkZTZlMTg0YWY4ZDQ5OGFiYmRlMDU1ZTUwNjUzMzE2IiwicHJvZmlsZU5hbWUiOiJBc3Nhc2luSmlhbmVyMjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyMjNlMzZhYzEzZjBmNzFhYmNmYmYwYzk2ZmRjMjAxMGNjM2UxMWZmMmIwZDgxMTJkMGU2M2Y0YjRhYWEwZGUifX19"
    private const val witherEssence = "ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ==ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ=="

    private val dropdownValues = SoundTypes.entries.map { it.getName() }.toList()

    private val volume by ConfigDelegate<Double>("secretSound.volume")
    private val soundIndex by ConfigDelegate<Int>("secretSound.sound")
    private val pitch by ConfigDelegate<Double>("secretSound.pitch")

    private val selectedSound: SoundEvent
        get() = SoundTypes.entries[soundIndex].getSound()

    private val secretTypes = listOf(
        "Architect's First Draft",
        "Candycomb",
        "Decoy",
        "Defuse Kit",
        "Dungeon Chest Key",
        "Healing VIII Splash Potion",
        "Inflatable Jerry",
        "Revive Stone",
        "Secret Dye",
        "Spirit Leap",
        "Training Weights",
        "Trap",
        "Treasure Talisman"
    ).sorted()

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

    private val volume by ConfigDelegate<Double>("secretSound.volume")
    private val soundIndex by ConfigDelegate<Int>("secretSound.sound")
    private val pitch by ConfigDelegate<Double>("secretSound.pitch")

    private val selectedSound: SoundEvent
        get() = SoundTypes.entries[soundIndex].getSound()

    private const val SECRET_DISTANCE = 10.0

    override fun initialize() {
        register<DungeonEvent.Secrets.Chest> { playSound() }
        register<DungeonEvent.Secrets.Misc> { playSound() }
        register<DungeonEvent.Secrets.Item> { event ->
            val itemId = event.entityId

            val world = KnitClient.world
            val entity = world?.getEntity(itemId) as? ItemEntity ?: return@register

            if(abs(player!!.position().distanceTo(entity.position())) <= SECRET_DISTANCE)
                playSound()
            }
        }
        register<DungeonEvent.Secrets.Bat> { event ->
            if(abs(player!!.position().distanceTo(event.entity.position())) <= SECRET_DISTANCE)
                playSound()
            }
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

enum class SoundTypes(private val sound: SoundEvent, private val dropdownName: String) {
    BLAZE(SoundEvents.BLAZE_HURT, "Blaze"),
    CAT(SoundEvents.CAT_AMBIENT, "Cat"),
    ANVIL(SoundEvents.ANVIL_LAND, "Anvil"),
    XP(SoundEvents.EXPERIENCE_ORB_PICKUP, "Experience"),
    NOTE_BLOCK(SoundEvents.NOTE_BLOCK_PLING.value(), "Note Block")
    ;

    fun getName() = dropdownName
    fun getSound() = sound
}