package dev.coldpenguins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

final class BlackTippedArrow extends AbstractArrow {
    private static final double WATER_SPEED_MULTIPLIER = 2.0;
    private boolean underwaterStep;

    BlackTippedArrow(EntityType<? extends BlackTippedArrow> type, Level level) {
        super(type, level);
    }

    BlackTippedArrow(Level level, LivingEntity shooter, ItemStack arrow, ItemStack weapon) {
        super(ColdPenguins.BLACK_ARROW_PROJECTILE.get(), shooter, level, arrow, weapon);
    }

    BlackTippedArrow(Level level, Position position, ItemStack arrow) {
        super(ColdPenguins.BLACK_ARROW_PROJECTILE.get(), position.x(), position.y(), position.z(),
                level, arrow, null);
    }

    @Override
    public void tick() {
        BlockPos position = blockPosition();
        FluidState fluid = level().getFluidState(position);
        underwaterStep = !inGround && fluid.is(FluidTags.WATER)
                && getY() < position.getY() + fluid.getHeight(level(), position);
        if (!underwaterStep) {
            super.tick();
            return;
        }

        // Scale this movement step while retaining the vanilla velocity for later ticks and save data.
        setDeltaMovement(getDeltaMovement().scale(WATER_SPEED_MULTIPLIER));
        try {
            super.tick();
        } finally {
            setDeltaMovement(getDeltaMovement().scale(1.0 / WATER_SPEED_MULTIPLIER));
            underwaterStep = false;
        }
    }

    @Override
    protected float getWaterInertia() {
        // Two-times movement speed and 0.8 drag yield four-times vanilla's underwater horizontal range.
        return 0.8F;
    }

    @Override
    protected double getDefaultGravity() {
        return super.getDefaultGravity() * (underwaterStep ? WATER_SPEED_MULTIPLIER : 1.0);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ColdPenguins.BLACK_TIPPED_ARROW.get());
    }
}
