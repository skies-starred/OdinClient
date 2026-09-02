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
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.ChatMessageEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.DungeonScan
import com.odtheking.odin.features.impl.dungeon.map.tile.DoorType
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.alert
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import foo.starred.odinclient.utils.Category
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB

object DoorHighlight : Module(
    name = "Door Highlight (C)",
    description = "Highlights wither and blood doors and keys in dungeons.",
    category = Category.CHEATS
) {
    private val announceKeySpawn by BooleanSetting("Announce Key Spawn", true, desc = "Announces when a key is spawned.")
    private val doorHighlightColor by ColorSetting("Door Highlight Color", Colors.MINECRAFT_RED.withAlpha(0.8f), true, desc = "Color for locked doors.")
    private val openableColor by ColorSetting("Openable Door Color", Colors.MINECRAFT_GREEN.withAlpha(0.8f), true, desc = "Color for doors that can be opened with a held key.")
    private val witherColor by ColorSetting("Wither Color", Colors.BLACK.withAlpha(0.8f), true, desc = "The color of the box.")
    private val bloodColor by ColorSetting("Blood Color", Colors.MINECRAFT_RED.withAlpha(0.8f), true, desc = "The color of the box.")
    private val depthCheck by BooleanSetting("Depth check", true, desc = "Disable depth check to enable ESP.")

    private var currentKey: KeyType? = null
    private var witherKeys = 0
    private var bloodKey = false
    private var bloodOpened = false

    private val witherKeyObtainRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Wither Key!?$")
    private val witherKeyPickedUpRegex = Regex("^A Wither Key was picked up!$")
    private val witherDoorOpenRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) opened a WITHER door!$")
    private val bloodKeyObtainRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Blood Key!$")
    private val bloodKeyPickedUpRegex = Regex("^A Blood Key was picked up!$")
    private val bloodDoorOpenRegex = Regex("^The BLOOD DOOR has been opened!$")

    val depth: Boolean
        get() = enabled && depthCheck

    init {
        on<ChatMessageEvent> {
            if (!DungeonUtils.inClear) return@on
            when {
                witherKeyObtainRegex.matches(value) || witherKeyPickedUpRegex.matches(value) -> witherKeys++
                witherDoorOpenRegex.matches(value) -> witherKeys = (witherKeys - 1).coerceAtLeast(0)
                bloodKeyObtainRegex.matches(value) || bloodKeyPickedUpRegex.matches(value) -> bloodKey = true
                bloodDoorOpenRegex.matches(value) -> { bloodKey = false; bloodOpened = true }
            }
        }

        onReceive<ClientboundSetEntityDataPacket> {
            if (!DungeonUtils.inClear) return@onReceive
            val entity = mc.level?.getEntity(id) as? ArmorStand ?: return@onReceive
            if (currentKey?.entity == entity) return@onReceive
            currentKey = KeyType.entries.find { it.displayName == entity.name.string } ?: return@onReceive
            currentKey?.entity = entity

            if (announceKeySpawn) alert("§${currentKey?.colorCode}${entity.name.string}§7 spawned!")
        }

        on<RenderEvent.Extract> {
            if (!DungeonUtils.inClear) return@on

            DungeonScan.doors.forEach { (_, door) ->
                if (!door.type.equalsOneOf(DoorType.Wither, DoorType.Blood)) return@forEach
                if (door.type == DoorType.Blood && bloodOpened) return@forEach

                val worldX = (door.position.x - 6) * 32 + 7 + door.rotation.offset.x * 16
                val worldZ = (door.position.z - 6) * 32 + 7 + door.rotation.offset.z * 16
                val box = AABB(worldX - 1.0, 69.0, worldZ - 1.0, worldX + 2.0, 73.0, worldZ + 2.0)

                val isOpenable = when (door.type) {
                    DoorType.Wither -> witherKeys > 0
                    DoorType.Blood -> bloodKey
                    else -> false
                }
                drawStyledBox(box, if (isOpenable) openableColor else doorHighlightColor, 2, false)
            }

            if (currentKey == null || currentKey?.entity == null) return@on
            currentKey?.let { keyType ->
                if (keyType.entity?.isAlive == false) {
                    currentKey = null
                    return@on
                }
                val position = keyType.entity?.position() ?: return@on
                drawStyledBox(AABB.unitCubeFromLowerCorner(position.add(-0.5, 1.0, -0.5)), keyType.color(), 2, depthCheck)
            }
        }

        on<LevelEvent.Load> {
            currentKey = null
            witherKeys = 0
            bloodKey = false
            bloodOpened = false
        }
    }

    private enum class KeyType(val displayName: String, val color: () -> Color, val colorCode: Char) {
        Wither("Wither Key", { witherColor }, '8'),
        Blood("Blood Key", { bloodColor }, 'c');

        var entity: Entity? = null
    }
}
