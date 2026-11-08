package dev.coldpenguins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class PenguinNestBlock extends BaseEntityBlock {
    static final IntegerProperty EGGS = IntegerProperty.create("eggs", 0, 3);
    private static final VoxelShape OUTLINE = Block.box(1, 0, 1, 15, 5, 15);

    PenguinNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EGGS, 0));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGGS);
    }

    static boolean isAvailable(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        boolean usable = state.isAir() || state.is(Blocks.SNOW)
                || (state.is(ColdPenguins.NEST.get()) && state.getValue(EGGS) == 0);
        BlockPos below = pos.below();
        return usable && level.getFluidState(pos).isEmpty() && level.getFluidState(below).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Nest(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level instanceof ServerLevel server && state.getValue(EGGS) > 0) {
            return createTickerHelper(type, ColdPenguins.NEST_ENTITY.get(),
                    (world, pos, block, nest) -> nest.incubate(server, pos, block));
        }
        return null;
    }

    static final class Nest extends BlockEntity {
        private static final long INCUBATION_TICKS = 3 * 24_000L;
        private PenguinSpecies species = PenguinSpecies.ORIGINAL;
        private long legacyHatchAt;
        private long remainingTicks = INCUBATION_TICKS;
        private long lastGameTime = -1;
        private long lastDayTime;

        Nest(BlockPos pos, BlockState state) {
            super(ColdPenguins.NEST_ENTITY.get(), pos, state);
        }

        void setSpecies(PenguinSpecies species) {
            this.species = species;
            setChanged();
        }

        private void incubate(ServerLevel level, BlockPos pos, BlockState state) {
            long gameTime = level.getGameTime();
            long dayTime = level.getDefaultClockTime();
            if (lastGameTime < 0) {
                if (legacyHatchAt > 0) {
                    remainingTicks = Math.clamp(legacyHatchAt - gameTime, 0, INCUBATION_TICKS);
                    legacyHatchAt = 0;
                }
            } else {
                // Skipped nights count, while moving the clock backward cannot extend incubation.
                long elapsed = Math.max(Math.max(0, gameTime - lastGameTime),
                        Math.max(0, dayTime - lastDayTime));
                remainingTicks = Math.max(0, remainingTicks - elapsed);
            }
            lastGameTime = gameTime;
            lastDayTime = dayTime;
            setChanged();
            if (remainingTicks > 0) {
                return;
            }

            int eggs = state.getValue(EGGS);
            int hatched = 0;
            for (int i = 0; i < eggs; i++) {
                Penguin chick = Penguin.createChick(level, species);
                if (chick != null) {
                    chick.snapTo(pos.getX() + 0.28 + 0.22 * i, pos.getY() + 0.04, pos.getZ() + 0.5,
                            level.getRandom().nextFloat() * 360.0F, 0.0F);
                    if (level.addFreshEntity(chick)) {
                        hatched++;
                    }
                }
            }
            if (hatched > 0) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
                                ColdPenguins.PENGUIN_EGG.get()),
                        pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        6 * hatched, 0.22, 0.12, 0.22, 0.06);
            }
            int remaining = eggs - hatched;
            remainingTicks = remaining == 0 ? INCUBATION_TICKS : 20;
            if (remaining == 0) {
                lastGameTime = -1;
            }
            setChanged();
            level.setBlock(pos, state.setValue(EGGS, remaining), Block.UPDATE_ALL);
        }

        @Override
        protected void saveAdditional(ValueOutput tag) {
            super.saveAdditional(tag);
            tag.putString("Species", species.id);
            if (legacyHatchAt > 0) {
                tag.putLong("HatchAt", legacyHatchAt);
            } else {
                tag.putLong("IncubationRemaining", remainingTicks);
                tag.putLong("LastIncubationGameTime", lastGameTime);
                tag.putLong("LastIncubationDayTime", lastDayTime);
            }
        }

        @Override
        protected void loadAdditional(ValueInput tag) {
            super.loadAdditional(tag);
            species = PenguinSpecies.fromName(tag.getStringOr("Species", "original"));
            if (tag.getLong("IncubationRemaining").isPresent()) {
                remainingTicks = Math.clamp(tag.getLongOr("IncubationRemaining", 0), 0, INCUBATION_TICKS);
                lastGameTime = tag.getLongOr("LastIncubationGameTime", 0);
                lastDayTime = tag.getLongOr("LastIncubationDayTime", 0);
                legacyHatchAt = 0;
            } else {
                legacyHatchAt = tag.getLongOr("HatchAt", 0);
                remainingTicks = INCUBATION_TICKS;
                lastGameTime = -1;
            }
        }
    }
}
