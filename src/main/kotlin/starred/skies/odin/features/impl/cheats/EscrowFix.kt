package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand

object EscrowFix : Module(
    name = "Escrow Fix",
    description = "Automatically reopens the ah/bz when it gets closed by escrow."
) {
    private val messages = mapOf(
        "There was an error with the auction house! (AUCTION_EXPIRED_OR_NOT_FOUND)" to "ah",
        "There was an error with the auction house! (INVALID_BID)" to "ah",
        "Claiming BIN auction..." to "ah",
        "Visit the Auction House to collect your item!" to "ah"
    )

    private val regex = Regex("Escrow refunded (\\d+) coins for Bazaar Instant Buy Submit!")

    init {
        on<ChatPacketEvent> {
            val command = messages[value] ?: if (value.matches(regex)) "bz" else null
            command?.let { sendCommand(it) }
        }
    }
}