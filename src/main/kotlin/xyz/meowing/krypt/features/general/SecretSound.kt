package xyz.meowing.krypt.features.general

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SkullBlockEntity
import tech.thatgravyboat.skyblockapi.platform.properties
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.utils.Utils.removeFormatting
import kotlin.math.abs

@Module
object SecretSound : Feature(
    "secretSound",
    island = SkyBlockIsland.THE_CATACOMBS
){
    private const val NAME = "Secret Sound"
    private val dropdownValues = SoundTypes.entries.map { it.getName() }.toList()

    override fun addConfig() {
        ConfigManager.addFeature(
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

    private const val redSkull = "eyJ0aW1lc3RhbXAiOjE1NzA5MTUxODU0ODUsInByb2ZpbGVJZCI6IjVkZTZlMTg0YWY4ZDQ5OGFiYmRlMDU1ZTUwNjUzMzE2IiwicHJvZmlsZU5hbWUiOiJBc3Nhc2luSmlhbmVyMjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyMjNlMzZhYzEzZjBmNzFhYmNmYmYwYzk2ZmRjMjAxMGNjM2UxMWZmMmIwZDgxMTJkMGU2M2Y0YjRhYWEwZGUifX19"
    private const val witherEssence = "ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ==ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ=="

    private val selectedSound: SoundEvent
        get() = SoundTypes.entries[soundIndex].getSound()

    private val secretTypes = listOf("Architect's First Draft", "Candycomb", "Decoy", "Defuse Kit", "Dungeon Chest Key",
        "Healing VIII Splash Potion", "Inflatable Jerry", "Revive Stone", "Secret Dye", "Spirit Leap", "Training Weights", "Trap", "Treasure Talisman").sorted()

    private const val secretDistance = 10.0

    override fun initialize() {
        register<PacketEvent.Sent> { event ->
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register

            val pos = packet.hitResult.blockPos
            val world = KnitClient.world ?: return@register
            val blockState = world.getBlockState(pos)

            if(blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST || blockState.block == Blocks.LEVER || checkSkull(world, pos))
                playSound()
        }
        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundTakeItemEntityPacket ?: return@register

            val itemId = packet.itemId
            val world = KnitClient.world

            val entity = world?.getEntity(itemId) as? ItemEntity ?: return@register
            val name = entity.item.displayName.string.removeFormatting()
            val sanitizedName = name.drop(1).dropLast(1)

            if(secretTypes.binarySearch(sanitizedName) >= 0
                && abs(player!!.position().distanceTo(entity.position())) <= secretDistance)
                playSound()
        }
        register<EntityEvent.Death> { event ->
            if(event.entity.type == EntityType.BAT
                && abs(player!!.position().distanceTo(event.entity.position())) <= secretDistance)
                playSound()
        }
    }

    private fun checkSkull(world: ClientLevel, pos: BlockPos): Boolean {
        val blockEntity = world.getBlockEntity(pos)

        if(blockEntity is SkullBlockEntity) {
            val profile = blockEntity.ownerProfile ?: return false
            val texture = profile.properties.get("textures")

            if(texture != null && texture.isNotEmpty()) {
                return texture.first().value == witherEssence || texture.first().value == redSkull
            }

            return false
        }

        return false
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
    XP(SoundEvents.EXPERIENCE_ORB_PICKUP, "Experience");

    fun getName() = dropdownName
    fun getSound() = sound
}