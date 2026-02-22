package starred.skies.odin.events

import com.odtheking.odin.events.core.Event
import com.odtheking.odin.utils.skyblock.dungeon.terminals.terminalhandler.TerminalHandler

data class TerminalUpdateEvent(val terminal: TerminalHandler) : Event