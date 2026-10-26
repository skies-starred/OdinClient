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

package foo.starred.odinclient.features.impl.general

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.features.Module
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.client.KeyMapping

object FarmKeys : Module(
    name = "Farm keys",
    description = "Temporarily changes your minecraft keybind configuration for farming in Skyblock.",
    category = OdinClientCategory.CHEATS
) {
    private var prev: InputConstants.Key? = null
    private var prev0: InputConstants.Key? = null

    private val attackKey by KeybindSetting("Block breaking", InputConstants.UNKNOWN.value, "Changes the keybind for breaking blocks.")
    private val jumpKey by KeybindSetting("Jump", InputConstants.UNKNOWN.value, "Changes the keybind for jumping.")
    private val lockCamera by BooleanSetting("Lock camera", true, desc = "Locks your camera.")

    @JvmStatic
    val lock: Boolean
        get() = enabled && lockCamera

    override fun onEnable() {
        super.onEnable()

        prev = (client.options?.keyAttack as? KeyMappingAccessor)?.boundKey
        prev0 = (client.options?.keyJump as? KeyMappingAccessor)?.boundKey

        bind(attackKey, jumpKey)
    }

    override fun onDisable() {
        bind(prev ?: client.options.keyAttack.defaultKey, prev0 ?: client.options.keyJump.defaultKey)
        super.onDisable()
    }

    private fun bind(attack: InputConstants.Key, jump: InputConstants.Key) {
        val options = client.options ?: return

        if (attackKey != InputConstants.UNKNOWN) options.keyAttack.setKey(attack)
        if (jumpKey != InputConstants.UNKNOWN) options.keyJump.setKey(jump)

        options.save()
        KeyMapping.resetMapping()
    }
}
