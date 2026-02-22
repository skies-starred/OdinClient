package starred.skies.odin.events

import com.odtheking.odin.events.core.CancellableEvent
import com.odtheking.odin.events.core.Event
import net.minecraft.client.input.MouseButtonInfo

sealed class InputEvent {
    sealed class Mouse {
        data class Press(
            val buttonInfo: MouseButtonInfo
        ) : CancellableEvent()

        data class Release(
            val buttonInfo: MouseButtonInfo
        ) : Event
    }
}