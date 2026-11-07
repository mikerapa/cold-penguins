package dev.coldpenguins;

import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class PenguinHornItem extends InstrumentItem {
    private static final double CALL_RANGE = 100.0;

    PenguinHornItem(Properties properties, TagKey<Instrument> instruments) {
        super(properties, instruments);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && result.getResult().consumesAction()) {
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
