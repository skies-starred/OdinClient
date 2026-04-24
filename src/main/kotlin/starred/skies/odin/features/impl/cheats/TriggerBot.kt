package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import com.odtheking.odin.utils.texture
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster/*? >= 1.21.11 {*/.zombie/*? }*/.Zombie
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.leftClick
import starred.skies.odin.utils.rightClick
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object TriggerBot : Module(
    name = "TriggerBot (!!!)",
    description = "Triggers bots - bots trigger, Untested.",
    category = Skit.CHEATS
) {
    @Suppress("unused")
    private val UAYOR by BooleanSetting("Use at your own risk", false, desc = "TriggerBot is untested, use at your own risk!")
    private val crystalDropdown by DropdownSetting("Crystal Dropdown", false)
    private val crystal by BooleanSetting("Crystal", false, desc = "Automatically takes and places crystals.").withDependency { crystalDropdown }
    private val take by BooleanSetting("Take", true, desc = "Takes crystals.").withDependency { crystal && crystalDropdown }
    private val place by BooleanSetting("Place", true, desc = "Places crystals.").withDependency { crystal && crystalDropdown }

    private val secretDropdown by DropdownSetting("Secret Dropdown", false)
    private val secret by BooleanSetting("Secret", false, desc = "Automatically clicks secrets.").withDependency { secretDropdown }
    private val delay by NumberSetting("Delay", 200L, 0, 1000, unit = "ms", desc = "The delay between each click.").withDependency { secret && secretDropdown }

    private val bloodDropdown by DropdownSetting("Blood Dropdown", false)
    private val blood by BooleanSetting("Blood Mobs", false, desc = "Left clicks once for each tracked blood mob at its predicted spawn time.").withDependency { bloodDropdown }
    private val bloodSpawnTick by NumberSetting("Blood Spawn Tick", 38, 35, 41, desc = "Blood mobs spawn randomly between 37 and 41 ticks; adjust this to match your instance.").withDependency { blood && bloodDropdown }
    private val bloodOffset by NumberSetting("Blood Offset", 40, -250, 250, unit = "ms", desc = "Millisecond offset applied to the predicted spawn click.").withDependency { blood && bloodDropdown }
    private val bloodRandomDelay by NumberSetting("Blood Random Delay", 0, 0, 150, unit = "ms", desc = "Optional extra random delay after the predicted spawn. Keep at 0 for the earliest click.").withDependency { blood && bloodDropdown }

    private var click = 0L
    private var click0 = 0L
    private val clicked = mutableMapOf<BlockPos, Long>()

    private var currentWatcherEntity: Zombie? = null
    private var firstBloodSpawns = true
    private val scheduledBloodMobs = ConcurrentHashMap.newKeySet<Int>()
    private val clickedBloodMobs = ConcurrentHashMap.newKeySet<Int>()
    private val bloodClickExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Odin Blood TriggerBot").apply { isDaemon = true }
    }

    init {
        on<TickEvent.Start> {
            if (!crystal) return@on
            if (!DungeonUtils.inBoss) return@on
            if (DungeonUtils.getF7Phase() != M7Phases.P1) return@on
            if (System.currentTimeMillis() - click < 500) return@on

            val hit = mc.hitResult ?: return@on
            if (hit.type != HitResult.Type.ENTITY) return@on

            val e = (hit as? EntityHitResult)?.entity ?: return@on
            val p = mc.player ?: return@on

            if (!take && !place) return@on
            if (take && e is EndCrystal) {
                rightClick()
                click = System.currentTimeMillis()
                return@on
            }

            if (!place) return@on
            if (e.name?.string?.noControlCodes != "Energy Crystal Missing") return@on
            if (p.mainHandItem?.displayName?.string?.noControlCodes != "Energy Crystal") return@on

            rightClick()
            click = System.currentTimeMillis()
        }

        on<TickEvent.Start> {
            if (!secret) return@on
            if (!DungeonUtils.inDungeons) return@on
            if (DungeonUtils.inBoss) return@on
            if (mc.screen != null) return@on
            if (System.currentTimeMillis() - click0 < delay) return@on
            if (DungeonUtils.currentRoomName.equalsOneOf("Water Board", "Three Weirdos")) return@on

            val hit = mc.hitResult ?: return@on
            if (hit.type != HitResult.Type.BLOCK) return@on

            val pos = (hit as? BlockHitResult)?.blockPos ?: return@on
            val state = world.getBlockState(pos) ?: return@on

            val n = System.currentTimeMillis()
            clicked.entries.removeIf { it.value + 1000L <= n }

            if (clicked.containsKey(pos)) return@on
            if (!DungeonUtils.isSecret(state, pos)) return@on

            rightClick()
            click0 = System.currentTimeMillis()
            clicked[pos] = n
        }

        onReceive<ClientboundSetEquipmentPacket> {
            if (!blood || currentWatcherEntity != null || !DungeonUtils.inClear) return@onReceive

            slots.forEach { slot ->
                if (slot.second.isEmpty) return@forEach
                val texture = slot.second.texture ?: return@forEach
                if (slot.first == EquipmentSlot.HEAD && texture in watcherSkulls) mc.execute {
                    currentWatcherEntity = mc.level?.getEntity(entity) as? Zombie
                }
            }
        }

        onReceive<ClientboundMoveEntityPacket> {
            if (!blood) return@onReceive
            if (xa == 0.toShort() && ya == 0.toShort() && za == 0.toShort()) return@onReceive
            if (!DungeonUtils.inClear) return@onReceive

            val level = mc.level ?: return@onReceive
            val entity = getEntity(level) as? ArmorStand ?: return@onReceive
            val watcher = currentWatcherEntity ?: return@onReceive
            if (watcher.distanceTo(entity) > 20) return@onReceive

            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            if (head.item != Items.PLAYER_HEAD || head.texture !in allowedMobSkulls) return@onReceive

            scheduleBloodClick(entity.id, firstBloodSpawns)
        }

        onReceive<ClientboundRemoveEntitiesPacket> {
            entityIds.forEach { id ->
                if (id == currentWatcherEntity?.id) currentWatcherEntity = null
                scheduledBloodMobs.remove(id)
                clickedBloodMobs.remove(id)
            }
        }

        on<ChatPacketEvent> {
            if (!DungeonUtils.inClear) return@on
            if (BLOOD_MOVE_REGEX.matches(value)) firstBloodSpawns = false
        }

        on<WorldEvent.Load> {
            clicked.clear()
            currentWatcherEntity = null
            firstBloodSpawns = true
            scheduledBloodMobs.clear()
            clickedBloodMobs.clear()
        }
    }

    private fun scheduleBloodClick(entityId: Int, firstSpawn: Boolean) {
        if (!scheduledBloodMobs.add(entityId) || entityId in clickedBloodMobs) return

        val randomDelay = if (bloodRandomDelay > 0) Random.nextLong(0, bloodRandomDelay.toLong() + 1L) else 0L
        val delayMs = ((if (firstSpawn) 2000 else 0) + (bloodSpawnTick * 50) + bloodOffset + randomDelay).coerceAtLeast(0L)

        bloodClickExecutor.schedule({
            mc.execute {
                if (!enabled || !blood || !DungeonUtils.inClear || mc.screen != null) return@execute
                if (!clickedBloodMobs.add(entityId)) return@execute

                leftClick()
            }
        }, delayMs, TimeUnit.MILLISECONDS)
    }

    @Suppress("UNCHECKED_CAST")
    private fun bloodCampSkullSet(field: String): Set<String> =
        runCatching {
            val clazz = Class.forName("com.odtheking.odin.features.impl.dungeon.BloodCamp")
            val declaredField = clazz.getDeclaredField(field)
            declaredField.isAccessible = true
            (declaredField.get(null) as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
        }.getOrDefault(emptySet())

    private val watcherSkulls by lazy { bloodCampSkullSet("watcherSkulls") }
    private val allowedMobSkulls by lazy { bloodCampSkullSet("allowedMobSkulls") }

    private val BLOOD_MOVE_REGEX = Regex("^\\[BOSS] The Watcher: Let's see how you can handle this\\.$")
}
