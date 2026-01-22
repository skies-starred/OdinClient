package starred.skies.odin.features

import com.google.gson.JsonArray
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.network.WebUtils
import com.odtheking.odin.utils.setTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import starred.skies.odin.OdinClient
import java.net.URI

object UpdateNotifier {
    private const val GITHUB_API = "https://api.github.com/repos/skies-starred/OdinClient/releases"
    private val versionRegex = Regex("""(\d+)\.(\d+)\.(\d+)(?:-r(\d+))?""") // https://regex101.com/r/An6dOq/1
    private var times: Int = 0
    private var latestVersion: Version? = null

    private data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val revision: Int = 0,
        val tag: String
    ) : Comparable<Version> {
        override fun compareTo(other: Version): Int =
            compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }, { it.revision })

        fun display() = if (revision > 0) "$major.$minor.$patch-r$revision" else "$major.$minor.$patch"
    }

    init {
        on</*? >= 1.21.10 {*/WorldEvent.Load/*? } else { *//*WorldLoadEvent*//*? } */> {
            if (times++ >= 3) return@on

            if (times == 1) {
                scope.launch {
                    delay(5000)
                    latestVersion = latest()
                    run()
                }

                return@on
            }

            run()
        }
    }

    private fun run() {
        val current = OdinClient.MOD_VERSION.parse() ?: return
        val latest = latestVersion?.takeIf { it > current } ?: return

        mc.execute {
            setTitle("§6Update Available: §b${latest.display()}")

            val message =
                Component.literal("§eNew update available for OdinClient: §c${current.display()} §7-> §b${latest.display()}").withStyle {
                    it.withClickEvent(ClickEvent.OpenUrl(URI("https://github.com/skies-starred/OdinClient/releases/tag/${latest.tag}")))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("§6Click to view release")))
                }

            modMessage(message)
        }
    }

    private fun String.parse(): Version? {
        val match = versionRegex.find(this) ?: return null
        return Version(
            match.groupValues[1].toInt(),
            match.groupValues[2].toInt(),
            match.groupValues[3].toInt(),
            match.groupValues[4].toIntOrNull() ?: 0,
            this
        )
    }

    private suspend fun latest(): Version? {
        return runCatching {
            WebUtils.fetchJson<JsonArray>(GITHUB_API).getOrNull()
                ?.mapNotNull { it.asJsonObject.get("tag_name")?.asString?.parse() }
                ?.maxOrNull()
        }.getOrNull()
    }
}