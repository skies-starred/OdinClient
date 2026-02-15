package starred.skies.odin.commands

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.utils.modMessage

@JvmField
var streamMode: Boolean = false

val streamCommand = Commodore("od") {
    literal("stream").literal("toggle").runs {
        streamMode = !streamMode
        modMessage("Stream mode is now: $streamMode.")
    }
}