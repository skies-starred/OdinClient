/**
 * Taken from OdinClient 1.8.9
 * odtheking's BSD-3 Clause License applies to this file.
 *
 * BSD 3-Clause License
 *
 * Copyright (c) 2023-2025, odtheking
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package foo.starred.odinclient.features.impl.dungeons

import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object CloseChest : Module(
    name = "Close Chest",
    description = "Allows you to instantly close chests with any key or automatically.",
    category = OdinClientCategory.CHEATS
) {
    private enum class Mode {
        AUTO, ANY_KEY
    }

    //~ if >= 26.2 '"Auto", arrayListOf("Auto", "Any Key")' -> 'Mode.AUTO'
    private val mode by SelectorSetting("Mode", "Auto", arrayListOf("Auto", "Any Key"), desc = "The mode to use.")

    init {
        onReceive<ClientboundOpenScreenPacket> {
            //~ if >= 26.2 'mode != 0' -> 'mode != Mode.AUTO'
            if (mode != 0) return@onReceive
            if (!DungeonUtils.inDungeons) return@onReceive
            if (!title.string.noControlCodes.equalsOneOf("Chest", "Large Chest")) return@onReceive

            client.connection?.send(ServerboundContainerClosePacket(containerId))
            it.cancel()
        }

        on<ScreenEvent.KeyPress> {
            if (!DungeonUtils.inDungeons) return@on
            if (client.options.keyInventory.matches(input)) return@on

            handleInput(screen)
        }

        on<ScreenEvent.MouseClick> {
            if (!DungeonUtils.inDungeons) return@on

            handleInput(screen)
        }
    }

    private fun handleInput(screen: Screen?) {
        //~ if >= 26.2 'mode != 1' -> 'mode != Mode.ANY_KEY'
        if (mode != 1) return
        val screen = screen as? ContainerScreen? ?: return
        if (screen.title.string.noControlCodes.equalsOneOf("Chest", "Large Chest")) client.player?.closeContainer()
    }
}
