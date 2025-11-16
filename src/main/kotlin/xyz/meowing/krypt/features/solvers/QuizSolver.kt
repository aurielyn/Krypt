package xyz.meowing.krypt.features.solvers

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils.getRealCoord
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object QuizSolver : Feature(
    "quizSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class TriviaAnswer(var blockPos: BlockPos?, var isCorrect: Boolean)

    private val quizSolutions = mutableMapOf<String, List<String>>()
    private var triviaAnswers: List<String>? = null
    private val triviaOptions = MutableList(3) { TriviaAnswer(null, false) }

    private var inQuiz = false
    private var roomCenter: BlockPos? = null
    private var rotation: Int? = null

    private val boxColor by ConfigDelegate<Color>("quizSolver.boxColor")
    private val showBeam by ConfigDelegate<Boolean>("quizSolver.beam")

    init {
        NetworkUtils.fetchJson<Map<String, List<String>>>(
            url = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/solvers/QuizSolver.json",
            onSuccess = {
                quizSolutions.putAll(it)
                Krypt.LOGGER.info("Loaded Quiz solutions.")
            },
            onError = { error ->
                Krypt.LOGGER.error("Caught error while trying to load Quiz solutions: $error")
            }
        )
    }

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Quiz solver",
                "Highlights correct trivia answers",
                "Solvers",
                ConfigElement(
                    "quizSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Box color",
                ConfigElement(
                    "quizSolver.boxColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 127))
                )
            )
            .addFeatureOption(
                "Show beam",
                ConfigElement(
                    "quizSolver.beam",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Phase through walls",
                ConfigElement(
                    "quizSolver.phase",
                    ElementType.Switch(true)
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Quiz") return@register

            inQuiz = true
            roomCenter = ScanUtils.getRoomCenter(event.new)
            rotation = 360 - event.new.rotation.degrees

            triviaOptions[0].blockPos = getRealCoord(BlockPos(5, 70, -9), roomCenter!!, rotation!!)
            triviaOptions[1].blockPos = getRealCoord(BlockPos(0, 70, -6), roomCenter!!, rotation!!)
            triviaOptions[2].blockPos = getRealCoord(BlockPos(-5, 70, -9), roomCenter!!, rotation!!)
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inQuiz && event.new.name != "Quiz") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<ChatEvent.Receive> { event ->
            if (!inQuiz || event.isActionBar) return@register
            val message = event.message.stripped
            val trimmed = message.trim()

            when {
                message.startsWith("[STATUE] Oruo the Omniscient: ") && message.endsWith("correctly!") -> {
                    if (message.contains("answered the final question")) {
                        reset()
                    } else if (message.contains("answered Question #")) {
                        triviaOptions.forEach { it.isCorrect = false }
                    }
                }

                trimmed.startsWith("ⓐ") || trimmed.startsWith("ⓑ") || trimmed.startsWith("ⓒ") -> {
                    triviaAnswers?.firstOrNull { message.endsWith(it) }?.let {
                        when (trimmed[0]) {
                            'ⓐ' -> triviaOptions[0].isCorrect = true
                            'ⓑ' -> triviaOptions[1].isCorrect = true
                            'ⓒ' -> triviaOptions[2].isCorrect = true
                        }
                    }
                }

                else -> {
                    val newAnswers = when {
                        trimmed == "What SkyBlock year is it?" -> {
                            val year = (((System.currentTimeMillis() / 1000) - 1560276000) / 446400).toInt() + 1
                            listOf("Year $year")
                        }
                        else -> quizSolutions.entries.find { message.contains(it.key) }?.value
                    }
                    newAnswers?.let { triviaAnswers = it }
                }
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (!inQuiz || triviaAnswers == null) return@register

            triviaOptions.forEach { answer ->
                if (!answer.isCorrect) return@forEach
                val pos = answer.blockPos ?: return@forEach

                Render3D.drawSpecialBB(
                    AABB(pos),
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    false
                )

                if (showBeam) {
                    Render3D.renderBeam(
                        event.context,
                        pos.x.toDouble(),
                        pos.y.toDouble(),
                        pos.z.toDouble(),
                        boxColor,
                        false
                    )
                }
            }
        }
    }

    private fun reset() {
        inQuiz = false
        roomCenter = null
        rotation = null
        triviaOptions.forEach {
            it.blockPos = null
            it.isCorrect = false
        }
        triviaAnswers = null
    }
}