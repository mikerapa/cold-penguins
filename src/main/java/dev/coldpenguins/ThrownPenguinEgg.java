package dev.coldpenguins;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class ThrownPenguinEgg extends ThrowableItemProjectile {
    ThrownPenguinEgg(EntityType<? extends ThrownPenguinEgg> type, Level level) {
        super(type, level);
    }

    ThrownPenguinEgg(Level level, LivingEntity owner) {
        super(ColdPenguins.THROWN_EGG.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ColdPenguins.PENGUIN_EGG.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            Vec3 impact = hit.getLocation();
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()),
                    impact.x, impact.y, impact.z, 8, 0.1, 0.1, 0.1, 0.05);
            if (random.nextInt(10) < 3) {
                Penguin chick = Penguin.createChick(server);
                if (chick != null) {
                    Vec3 hatchPosition = impact;
                    if (hit instanceof BlockHitResult blockHit) {
                        Direction face = blockHit.getDirection();
                        double offset = face == Direction.DOWN ? 0.51 : 0.20;
                        hatchPosition = impact.relative(face, offset);
                    }
                    chick.moveTo(hatchPosition.x, hatchPosition.y, hatchPosition.z, getYRot(), 0.0F);
                    server.addFreshEntity(chick);
                }
            }
            discard();
        }
    }
}
