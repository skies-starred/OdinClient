/**
 * Taken from OdinFabric
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

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import foo.starred.odinclient.utils.Category
import foo.starred.odinclient.utils.drawTracer
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object LividSolver : Module(
    name = "Livid Solver (C)",
    description = "Provides a visual cue for the correct Livid's location in the boss fight.",
    category = Category.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val tracer by BooleanSetting("Tracer", true, desc = "Displays a tracer to the livid") // mi0 c:
    private val woolLocation = BlockPos(5, 108, 43)
    private var currentLivid = Livid.HOCKEY

    private val hud by HUD("Invulnerability Timer", "Shows time remaining on Livid's invulnerability.") { example ->
        if (!example && (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5) || invulnTime <= 0)) return@HUD 0 to 0
        val time = if (example) 390 else invulnTime
        val color = when {
            time > 260 -> "§a"
            time > 130 -> "§e"
            else -> "§c"
        }
        textDim("${color}Livid: ${time}t ", 0, 0)
    }

    private var invulnTime = 0
    private val lividStartRegex = Regex("^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$")

    init {
        on<ChatMessageEvent> {
            if (!DungeonUtils.inDungeons || !DungeonUtils.isFloor(5)) return@on
            if (value.matches(lividStartRegex)) invulnTime = 390
        }

        on<BlockUpdateEvent> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5) || pos != woolLocation) return@on
            currentLivid = Livid.entries.find { livid -> livid.wool.defaultBlockState() == updated.block.defaultBlockState() } ?: return@on
            modMessage("Found Livid: §${currentLivid.colorCode}${currentLivid.entityName}")
        }

        onReceive<ClientboundSetEntityDataPacket> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5)) return@onReceive
            schedule(2) {
                currentLivid.entity =
                    (mc.level?.getEntity(id) as? Player)?.takeIf { it.name.string == "${currentLivid.entityName} Livid" }
                        ?: return@schedule
            }
        }

        on<RenderEvent.Extract> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5)) return@on
            currentLivid.entity?.let {
                drawWireFrameBox(it.boundingBox, currentLivid.color, 8f, depthCheck)
                if (tracer) drawTracer(it.position().addVec(y = it.eyeHeight), currentLivid.color, depth = depthCheck)
            }
        }

        on<TickEvent.Server> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5)) return@on
            if (invulnTime > 0) invulnTime--
        }

        on<LevelEvent.Load> {
            currentLivid = Livid.HOCKEY
            currentLivid.entity = null
            invulnTime = 0
        }
    }

    private enum class Livid(val entityName: String, val colorCode: Char, val color: Color, val wool: Block) {
        VENDETTA("Vendetta", 'f', Colors.WHITE, Blocks.WHITE_WOOL),
        CROSSED("Crossed", 'd', Colors.MINECRAFT_DARK_PURPLE, Blocks.MAGENTA_WOOL),
        ARCADE("Arcade", 'e', Colors.MINECRAFT_YELLOW, Blocks.YELLOW_WOOL),
        SMILE("Smile", 'a', Colors.MINECRAFT_GREEN, Blocks.LIME_WOOL),
        DOCTOR("Doctor", '7', Colors.MINECRAFT_GRAY, Blocks.GRAY_WOOL),
        PURPLE("Purple", '5', Colors.MINECRAFT_DARK_PURPLE, Blocks.PURPLE_WOOL),
        SCREAM("Scream", '9', Colors.MINECRAFT_BLUE, Blocks.BLUE_WOOL),
        FROG("Frog", '2', Colors.MINECRAFT_DARK_GREEN, Blocks.GREEN_WOOL),
        HOCKEY("Hockey", 'c', Colors.MINECRAFT_RED, Blocks.RED_WOOL);

        var entity: Player? = null
    }
}
