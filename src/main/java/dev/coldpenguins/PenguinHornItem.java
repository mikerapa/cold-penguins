package dev.coldpenguins;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class PenguinHornItem extends InstrumentItem {
    private static final double CALL_RANGE = 100.0;

    PenguinHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = super.use(level, player, hand);
        if (!level.isClientSide() && result.consumesAction()) {
            Vec3 origin = player.position();
            for (Penguin penguin : level.getEntitiesOfClass(Penguin.class,
                    player.getBoundingBox().inflate(CALL_RANGE))) {
                if (penguin.isAlive() && penguin.distanceToSqr(origin) <= CALL_RANGE * CALL_RANGE) {
                    penguin.hearPenguinHorn(origin);
                }
            }
        }
        return result;
    }
}
