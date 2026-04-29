package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal/*? >= 1.21.11 {*//*.chicken*//*? }*/.Chicken
import net.minecraft.world.entity.animal/*? >= 1.21.11 {*//*.cow*//*? }*/.Cow
import net.minecraft.world.entity.animal/*? >= 1.21.11 {*//*.pig*//*? }*/.Pig
import net.minecraft.world.entity.animal/*? >= 1.21.11 {*//*.rabbit*//*? }*/.Rabbit
import net.minecraft.world.entity.animal/*? >= 1.21.11 {*//*.equine*//*? } else {*/.horse/*? }*/.Horse
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.ai.attributes.Attributes
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.drawTracer
import kotlin.math.abs

object TrapperESP : Module(
    name = "Trapper ESP",
    description = "Highlights Trevor trapper animals by rarity HP profile.",
    category = Skit.CHEATS
) {
    private val enabledEsp by BooleanSetting("Enable ESP", true, desc = "Render ESP box for trapper mobs.")
    private val tracer by BooleanSetting("Show Tracer", true, desc = "Draw a tracer to trapper mobs.")
    private val derpyMode by BooleanSetting("Derpy Mode", false, desc = "Use doubled trapper HP thresholds while Derpy is mayor.")
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to render through walls.").withDependency { enabledEsp || tracer }
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the ESP box.").withDependency { enabledEsp }
    private val trackableColor by ColorSetting("Trackable Color", Colors.WHITE, true, desc = "Color for Trackable animals.").withDependency { enabledEsp || tracer }
    private val untrackableColor by ColorSetting("Untrackable Color", Colors.MINECRAFT_GREEN, true, desc = "Color for Untrackable animals.").withDependency { enabledEsp || tracer }
    private val undetectedColor by ColorSetting("Undetected Color", Colors.MINECRAFT_AQUA, true, desc = "Color for Undetected animals.").withDependency { enabledEsp || tracer }
    private val endangeredColor by ColorSetting("Endangered Color", Colors.MINECRAFT_DARK_PURPLE, true, desc = "Color for Endangered animals.").withDependency { enabledEsp || tracer }
    private val elusiveColor by ColorSetting("Elusive Color", Colors.MINECRAFT_YELLOW, true, desc = "Color for Elusive animals.").withDependency { enabledEsp || tracer }

    private val startRegex = Regex("^\\[NPC] Trevor: You can find your (\\w+) animal near the .*$")
    private var activeRarity: Rarity? = null

    init {
        on<ChatPacketEvent> {
            val msg = value.noControlCodes
            val rarityName = startRegex.matchEntire(msg)?.groupValues?.getOrNull(1) ?: return@on
            activeRarity = Rarity.fromLabel(rarityName)
        }

        on<RenderEvent.Extract> {
            val world = mc.level ?: return@on
            if (!enabledEsp && !tracer) return@on
            val rarity = activeRarity ?: return@on
            val targetHp = rarity.expectedHp(derpyMode)
            val drawColor = rarity.color()

            for (entity in world.entitiesForRendering()) {
                val living = entity as? LivingEntity ?: continue
                if (!isSupportedAnimal(living)) continue
                if (!matchesRarityHp(living, targetHp)) continue

                if (enabledEsp) drawStyledBox(living.renderBoundingBox, drawColor, renderStyle, depthCheck)
                if (tracer) drawTracer(living.renderPos, drawColor, depth = depthCheck)
            }
        }

        on<WorldEvent.Load> {
            activeRarity = null
        }
    }

    private fun isSupportedAnimal(entity: LivingEntity): Boolean {
        return entity is Cow || entity is Pig || entity is Sheep || entity is Chicken || entity is Rabbit || entity is Horse
    }

    private fun matchesRarityHp(entity: LivingEntity, expectedHp: Float): Boolean {
        val base = entity.getAttributeBaseValue(Attributes.MAX_HEALTH).toFloat()
        val normalized = if (entity is Horse) base / 2f else base
        return abs(normalized - expectedHp) <= 0.5f
    }

    private enum class Rarity(private val normalHp: Float, private val derpyHp: Float) {
        Trackable(100f, 200f),
        Untrackable(500f, 1000f),
        Undetected(1000f, 2000f),
        Endangered(5000f, 10000f),
        Elusive(10000f, 20000f);

        fun expectedHp(derpy: Boolean): Float = if (derpy) derpyHp else normalHp

        fun color(): Color = when (this) {
            Trackable -> trackableColor
            Untrackable -> untrackableColor
            Undetected -> undetectedColor
            Endangered -> endangeredColor
            Elusive -> elusiveColor
        }

        companion object {
            fun fromLabel(label: String): Rarity? = entries.firstOrNull { it.name.equals(label, ignoreCase = true) }
        }
    }
}
