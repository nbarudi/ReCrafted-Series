package ca.bungo.snake.abstracted.v1_19_R3

import ca.bungo.snake.abstracted.AbstractedHandler
import ca.bungo.snake.abstracted.AbstractedLink
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin


class Abstractedv1_19_R3(helper: AbstractedLink) : AbstractedHandler(helper) {

    override fun printTesting() {
        helper.instance.logger.info("This is a test!")
    }

    override fun createMountableEntity(entityType: EntityType, world: World): Entity? {
        val nmsEnt = net.minecraft.world.entity.EntityType.byString(entityType.toString().lowercase()).get()
        return CustomEntity(nmsEnt as net.minecraft.world.entity.EntityType<out Sheep>, (world as CraftWorld).handle, null).bukkitEntity
    }

    override fun createFollowingEntity(entityType: EntityType?, world: World?, leader: Entity): Entity? {
        val nmsEnt = net.minecraft.world.entity.EntityType.byString(entityType.toString().lowercase()).get()
        if((leader as CraftEntity).handle !is Mob) return null;
        return CustomEntity(nmsEnt as net.minecraft.world.entity.EntityType<out Sheep>, (world as CraftWorld).handle, (leader.handle as Mob)).bukkitEntity
    }

    override fun moveEntityTo(entity: Entity?, player: org.bukkit.entity.Player) {
        val ent = (entity as CraftEntity).handle

        val yaw = player.location.yaw

        val vector = Vector()

        val rotX: Double = yaw.toDouble()

        vector.y = -sin(Math.toRadians(0.0))

        val xz = cos(Math.toRadians(0.0))

        vector.x = -xz * sin(Math.toRadians(rotX))
        vector.z = xz * cos(Math.toRadians(rotX))

        val dir = Vec3(vector.x * 0.7, 0.0, vector.z * 0.7)
        ent.move(MoverType.SELF, dir)
    }

    override fun moveEntityTo(self: Entity?, to: Vector) {
        val ent = (self as CraftEntity).handle
        val dir = Vec3(to.x * 0.7, 0.0, to.z * 0.7)
        ent.move(MoverType.SELF, dir)
    }


    override fun setEntityColor(entity: Entity, dyeColor: org.bukkit.DyeColor) {
        val nmsEntity = (entity as CraftEntity).handle
        val nmsDyeColor = DyeColor.valueOf(dyeColor.toString().uppercase())
        if(nmsEntity is Sheep){
            nmsEntity.color = nmsDyeColor
        }
    }

    class CustomEntity(type: net.minecraft.world.entity.EntityType<out Sheep>, world: Level, private val leader: Mob?) : Sheep(type, world), PlayerRideable {


        override fun getControllingPassenger(): LivingEntity? {
            if(leader != null){
                return leader
            }
            val pasEntity = this.firstPassenger;
            if(pasEntity is LivingEntity){
                return pasEntity
            }
            return null
        }

        private fun doPlayerRide(player: Player) {
            if (!level.isClientSide) {
                player.yRot = yRot
                player.xRot = xRot
                player.startRiding(this)
            }
        }

        private fun getRiddenRotation(controllingPassenger: LivingEntity): Vec2 {
            return Vec2(controllingPassenger.xRot * 0.5f, controllingPassenger.yRot)
        }

        override fun tickRidden(controllingPassenger: LivingEntity, movementInput: Vec3) {
            super.tickRidden(controllingPassenger, movementInput)
            val vec2f = getRiddenRotation(controllingPassenger)
            if(leader != null){
                this.lookAt(leader, 180.0f, 180.0f)
            }else {
                setRot(vec2f.y, vec2f.x)
                yHeadRot = yRot
                yBodyRot = yHeadRot
                yRotO = yBodyRot
            }
        }

        override fun interactAt(player: Player, hitPos: Vec3, hand: InteractionHand): InteractionResult {
            if (!this.isVehicle && !this.isBaby) {
                doPlayerRide(player)
                return InteractionResult.sidedSuccess(level.isClientSide)
            }
            return super.interactAt(player, hitPos, hand)
        }

        override fun isEffectiveAi(): Boolean {
            return !this.isVehicle
        }

        override fun getRiddenInput(controllingPassenger: LivingEntity, movementInput: Vec3): Vec3 {
            if (this.isVehicle) {
                val f = controllingPassenger.xxa * 0.5f
                var f1 = controllingPassenger.zza
                if (f1 <= 0.0f) {
                    f1 *= 0.25f
                }
                if (controllingPassenger !is Player) return super.getRiddenInput(controllingPassenger, movementInput)
                val res = Vec3(f.toDouble(), 0.0, f1.toDouble())
                return res
            }
            return Vec3.ZERO
        }

        override fun getRiddenSpeed(controllingPassenger: LivingEntity): Float {
            return 0.7f
        }

    }

}