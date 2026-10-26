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

import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.ListSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.lore
import com.odtheking.odin.utils.modMessage
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.odinclient.OdinClient
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.api.category.OdinClientCategory
import foo.starred.odinclient.utils.command
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items

object AutoSell : Module(
    name = "Auto Sell",
    description = "Automatically sell items in trades and cookie menus. (/odc sell)",
    category = OdinClientCategory.CHEATS
) {
    private val sellList by ListSetting("Sell list", mutableSetOf<String>())
    //~ if >= 26.2 '2, 10' -> '2..10'
    private val delay by NumberSetting("Delay", 6, 2, 10, 1, desc = "The delay between each sell action.", unit = " ticks")
    //~ if >= 26.2 '0, 5' -> '0..5'
    private val randomization by NumberSetting("Randomization", 1, 0, 5, 1, desc = "Random delay variance", unit = " ticks")
    //~ if >= 26.2 '"Shift", options = listOf("Shift", "Middle", "Left")' -> 'ClickType.Shift'
    private val clickType1 by SelectorSetting("Click Type", "Shift", options = listOf("Shift", "Middle", "Left"), desc = "The type of click to use when selling items.")
    private val addDefaults by ActionSetting("Add defaults", desc = "Add default dungeon items to the auto sell list.") {
        sellList.addAll(defaultItems)
        modMessage("§aAdded default items to auto sell list")
        OdinClient.config.save()
    }

    private var last = 0L
    private var next = 0L

    init {
        on<TickStartEvent> {
            if (sellList.isEmpty()) return@on
            val menu = MinecraftGui.container?.menu ?: return@on
            val now = System.currentTimeMillis()
            if (now - last < next) return@on

            val t0 = menu.slots.getOrNull(49)?.item
            val a = t0?.item == Items.HOPPER && t0.hoverName.stripped() == "Sell Item"
            val b = t0?.lore?.lastOrNull()?.stripped() == "Click to buyback!"
            if (!a && !b) return@on

            for (s in menu.slots) {
                if (s.container !is Inventory) continue

                val stack = s.item.takeIf { !it.isEmpty } ?: continue
                val name = stack.hoverName.string.stripped()

                if (!sellList.any { name.contains(it, true) }) continue
                if (blacklist.any { name.contains(it, true) }) continue

                MinecraftGui.click(menu.containerId, s.index, clickType = clickType1.get())
                last = now
                delay()

                break
            }
        }

        command {
            "sell".then {
                "add" {
                    val lowercase = client.player?.mainHandItem?.hoverName?.string?.stripped()?.lowercase() ?: return@invoke modMessage("Either hold an item or write an item name to be added to autosell.")
                    if (lowercase in sellList) return@invoke modMessage("$lowercase is already in the Auto sell list.")

                    modMessage("Added \"$lowercase\" to the Auto sell list.")
                    sellList.add(lowercase)
                    OdinClient.config.save()
                }

                "remove" {
                    val lowercase = client.player?.mainHandItem?.hoverName?.string?.stripped()?.lowercase() ?: return@invoke modMessage("Either hold an item or write an item name to be removed from autosell.")
                    if (lowercase !in sellList) return@invoke modMessage("$lowercase is not in the Auto sell list.")

                    modMessage("Removed \"$lowercase\" from the Auto sell list.")
                    sellList.remove(lowercase)
                    OdinClient.config.save()
                }

                "clear" {
                    modMessage("Auto sell list cleared.")
                    sellList.clear()
                    OdinClient.config.save()
                }

                "list" {
                    modMessage("Auto sell list (${sellList.size}):")
                    for (item in sellList) {
                        modMessage("  §b${item}")
                    }
                }
            }
        }
    }

    private fun delay() {
        next = ((delay + (0..randomization).random()) * 50).toLong()
    }

    private fun Int.get() = when (this) {
        0 -> ContainerInput.QUICK_MOVE
        1 -> ContainerInput.CLONE
        2 -> ContainerInput.PICKUP
        else -> ContainerInput.QUICK_MOVE
    }

    private val defaultItems = arrayOf(
        "enchanted ice", "superboom tnt", "rotten", "skeleton master", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )

    private val blacklist = listOf("skeleton master chestplate")

    //? if >= 26.2 {
    /*private enum class ClickType(val input: ContainerInput) {
        Shift(ContainerInput.QUICK_MOVE),
        Middle(ContainerInput.CLONE),
        Left(ContainerInput.PICKUP);

        fun get(): ContainerInput {
            return input
        }
    }
    *///? }
}
