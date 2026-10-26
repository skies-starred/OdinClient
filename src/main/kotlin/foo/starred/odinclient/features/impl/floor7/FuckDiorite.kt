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

package foo.starred.odinclient.features.impl.floor7

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object FuckDiorite : Module(
    name = "Fuck Diorite",
    description = "Replaces the pillars in the storm fight with glass.",
    category = OdinClientCategory.CHEATS
) {
    private val GLASS_STATE = Blocks.GLASS.defaultBlockState()
    private val STAINED_GLASS_BLOCKS = arrayOf(
        //~ if >= 26.2 'WHITE_STAINED_GLASS' -> 'STAINED_GLASS.white()'
        Blocks.WHITE_STAINED_GLASS,
        //~ if >= 26.2 'ORANGE_STAINED_GLASS' -> 'STAINED_GLASS.orange()'
        Blocks.ORANGE_STAINED_GLASS,
        //~ if >= 26.2 'MAGENTA_STAINED_GLASS' -> 'STAINED_GLASS.magenta()'
        Blocks.MAGENTA_STAINED_GLASS,
        //~ if >= 26.2 'LIGHT_BLUE_STAINED_GLASS' -> 'STAINED_GLASS.lightBlue()'
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        //~ if >= 26.2 'YELLOW_STAINED_GLASS' -> 'STAINED_GLASS.yellow()'
        Blocks.YELLOW_STAINED_GLASS,
        //~ if >= 26.2 'LIME_STAINED_GLASS' -> 'STAINED_GLASS.lime()'
        Blocks.LIME_STAINED_GLASS,
        //~ if >= 26.2 'PINK_STAINED_GLASS' -> 'STAINED_GLASS.pink()'
        Blocks.PINK_STAINED_GLASS,
        //~ if >= 26.2 'GRAY_STAINED_GLASS' -> 'STAINED_GLASS.gray()'
        Blocks.GRAY_STAINED_GLASS,
        //~ if >= 26.2 'LIGHT_GRAY_STAINED_GLASS' -> 'STAINED_GLASS.lightGray()'
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        //~ if >= 26.2 'CYAN_STAINED_GLASS' -> 'STAINED_GLASS.cyan()'
        Blocks.CYAN_STAINED_GLASS,
        //~ if >= 26.2 'PURPLE_STAINED_GLASS' -> 'STAINED_GLASS.purple()'
        Blocks.PURPLE_STAINED_GLASS,
        //~ if >= 26.2 'BLUE_STAINED_GLASS' -> 'STAINED_GLASS.blue()'
        Blocks.BLUE_STAINED_GLASS,
        //~ if >= 26.2 'BROWN_STAINED_GLASS' -> 'STAINED_GLASS.brown()'
        Blocks.BROWN_STAINED_GLASS,
        //~ if >= 26.2 'GREEN_STAINED_GLASS' -> 'STAINED_GLASS.green()'
        Blocks.GREEN_STAINED_GLASS,
        //~ if >= 26.2 'RED_STAINED_GLASS' -> 'STAINED_GLASS.red()'
        Blocks.RED_STAINED_GLASS,
        //~ if >= 26.2 'BLACK_STAINED_GLASS' -> 'STAINED_GLASS.black()'
        Blocks.BLACK_STAINED_GLASS
    )

    private val pillarBasedColor by BooleanSetting("Pillar Based", true, desc = "Swaps the diorite in the pillar to a corresponding color.").withDependency { !schitzo }
    //~ if >= 26.2 '"None", arrayListOf("NONE", "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK")' -> 'GlassColor.NONE'
    private val colorIndex by SelectorSetting("Color", "None", arrayListOf("NONE", "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"), desc = "Color for the stained glass.").withDependency { !pillarBasedColor && !schitzo }

    private val schitzo by BooleanSetting("Schitzo mode", false, desc = "Schtizoing.")

    private val pillars = arrayOf(BlockPos(46, 169, 41), BlockPos(46, 169, 65), BlockPos(100, 169, 65), BlockPos(100, 169, 41))
    private val pillarColors = intArrayOf(5, 4, 10, 14)

    private val coordinates: Array<Set<BlockPos>> = Array(4) { pillarIndex ->
        val pillar = pillars[pillarIndex]
        buildSet {
            for (dx in (pillar.x - 3)..(pillar.x + 3))
                for (dy in pillar.y..(pillar.y + 37))
                    for (dz in (pillar.z - 3)..(pillar.z + 3))
                        add(BlockPos(dx, dy, dz))
        }
    }

    init {
        on<TickEvent.End> {
            if (DungeonUtils.getF7Phase() == M7Phases.P2) replaceDiorite()
        }
    }

    private fun replaceDiorite() {
        val level = client.level ?: return

        for ((index, coordinateSet) in coordinates.withIndex()) {
            for (pos in coordinateSet) {
                val state = level.getBlockState(pos)

                if (state.block.equalsOneOf(Blocks.DIORITE, Blocks.POLISHED_DIORITE)) {
                    setGlass(pos, index)
                }
            }
        }
    }

    private fun setGlass(pos: BlockPos, pillarIndex: Int) {
        val newState = when {
            schitzo -> STAINED_GLASS_BLOCKS.random().defaultBlockState()
            pillarBasedColor -> STAINED_GLASS_BLOCKS[pillarColors[pillarIndex]].defaultBlockState()
            //~ if >= 26.2 'colorIndex != 0 -> STAINED_GLASS_BLOCKS[colorIndex - 1]' -> 'colorIndex != GlassColor.NONE -> STAINED_GLASS_BLOCKS[colorIndex.ordinal - 1]'
            colorIndex != 0 -> STAINED_GLASS_BLOCKS[colorIndex - 1].defaultBlockState()
            else -> GLASS_STATE
        }

        client.level?.setBlock(pos, newState, 3)
    }

    @Suppress("Unused")
    private enum class GlassColor {
        NONE, WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK,
        GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK
    }
}
