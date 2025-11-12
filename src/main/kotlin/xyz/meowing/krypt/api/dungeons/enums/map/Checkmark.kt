package xyz.meowing.krypt.api.dungeons.enums.map

import net.minecraft.resources.ResourceLocation
import xyz.meowing.krypt.Krypt

enum class Checkmark {
    NONE,
    WHITE,
    GREEN,
    FAILED,
    UNEXPLORED,
    UNDISCOVERED
    ;

    val image: ResourceLocation?
        get() = when (this) {
            GREEN -> greenCheck
            WHITE -> whiteCheck
            FAILED -> failedRoom
            UNEXPLORED -> questionMark
            else -> null
        }

    val textColor: String
        get() = when (this) {
            WHITE -> "§f"
            GREEN -> "§a"
            FAILED -> "§c"
            else -> "§7"
        }

    companion object {
        val greenCheck: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/clear/green_check")
        val whiteCheck: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/clear/white_check")
        val failedRoom: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/clear/failed_room")
        val questionMark: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/clear/question_mark")
    }
}